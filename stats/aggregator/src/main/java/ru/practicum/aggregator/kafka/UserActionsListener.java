package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.logic.SimilarityStore;
import ru.practicum.aggregator.logic.WeightsProperties;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserActionsListener {

    private final SimilarityStore store;
    private final WeightsProperties weights;
    private final KafkaTemplate<Long, EventSimilarityAvro> producer;

    @Value("${topics.similarity:stats.events-similarity.v1}")
    private String similarityTopic;

    @KafkaListener(
            topics = "${topics.user-actions:stats.user-actions.v1}",
            containerFactory = "userActionListenerFactory"
    )
    public void onAction(UserActionAvro msg) {
        long userId = msg.getUserId();
        long eventId = msg.getEventId();
        double w = weights.weight(msg.getActionType());

        double oldW = store.getUserWeight(eventId, userId);
        if (w <= oldW) {
            return;
        }

        store.putUserWeight(eventId, userId, w);

        store.addToSum(eventId, (w - oldW));

        Set<Long> events = store.allEventsSnapshot();

        for (long otherEvent : events) {
            if (otherEvent == eventId) continue;

            double otherW = store.getUserWeight(otherEvent, userId);
            if (otherW > 0.0) {
                double oldMin = Math.min(oldW, otherW);
                double newMin = Math.min(w, otherW);
                double deltaMin = newMin - oldMin;
                if (deltaMin != 0.0) {
                    store.addToMinSum(eventId, otherEvent, deltaMin);
                }
            }

            long a = Math.min(eventId, otherEvent);
            long b = Math.max(eventId, otherEvent);

            double sMin = store.getMinSum(a, b);
            double sA = store.sum(a);
            double sB = store.sum(b);

            double score = (sA == 0.0 || sB == 0.0) ? 0.0 : (sMin / (Math.sqrt(sA) * Math.sqrt(sB)));

            EventSimilarityAvro out = EventSimilarityAvro.newBuilder()
                    .setEventA(a)
                    .setEventB(b)
                    .setScore(score)
                    .setTimestamp(msg.getTimestamp())
                    .build();

            producer.send(similarityTopic, out.getEventA(), out);
        }
    }
}
