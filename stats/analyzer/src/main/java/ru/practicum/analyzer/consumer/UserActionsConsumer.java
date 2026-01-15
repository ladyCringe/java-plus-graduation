package ru.practicum.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.db.UserEventInteraction;
import ru.practicum.analyzer.db.UserEventInteractionId;
import ru.practicum.analyzer.repository.InteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class UserActionsConsumer {

    private final InteractionRepository repo;
    private final Weights weights;

    @KafkaListener(
            topics = "${topics.user-actions:stats.user-actions.v1}",
            containerFactory = "userActionListenerFactory"
    )
    @Transactional
    public void on(UserActionAvro msg) {
        long userId = msg.getUserId();
        long eventId = msg.getEventId();
        double w = weights.weight(msg.getActionType());

        LocalDateTime ts = LocalDateTime.ofInstant(
                msg.getTimestamp(),
                ZoneOffset.UTC
        );

        UserEventInteractionId id = new UserEventInteractionId(userId, eventId);
        UserEventInteraction old = repo.findById(id).orElse(null);

        if (old == null) {
            repo.save(UserEventInteraction.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .weight(w)
                    .lastTs(ts)
                    .build());
            return;
        }

        if (w > old.getWeight()) {
            old.setWeight(w);
            old.setLastTs(ts);
            repo.save(old);
        }
    }
}
