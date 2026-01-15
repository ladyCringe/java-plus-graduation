package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${collector.topic.user-actions:stats.user-actions.v1}")
    private String topic;

    public void send(UserActionAvro msg) {
        kafkaTemplate.send(topic, msg.getEventId(), msg);
    }
}
