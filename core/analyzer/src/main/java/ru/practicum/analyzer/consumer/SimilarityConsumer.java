package ru.practicum.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.db.EventSimilarity;
import ru.practicum.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class SimilarityConsumer {

    private final SimilarityRepository repo;

    @KafkaListener(
            topics = "${topics.similarity:stats.events-similarity.v1}",
            containerFactory = "similarityListenerFactory"
    )
    @Transactional
    public void on(EventSimilarityAvro msg) {
        LocalDateTime ts = LocalDateTime.ofInstant(
                msg.getTimestamp(),
                ZoneOffset.UTC
        );

        repo.save(EventSimilarity.builder()
                .eventA(msg.getEventA())
                .eventB(msg.getEventB())
                .score(msg.getScore())
                .updatedTs(ts)
                .build());
    }
}
