package ru.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.config.KafkaConfiguration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AggregationEntryPoint {

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final EventsSimilarityService eventsSimilarityService;

    private final KafkaConfiguration.ConsumerConfig consumerConfig;
    private final KafkaConsumer<String, UserActionAvro> consumer;

    private final KafkaConfiguration.ProducerConfig producerConfig;
    private final KafkaProducer<String, EventSimilarityAvro> producer;

    @Autowired
    public AggregationEntryPoint(EventsSimilarityService eventsSimilarityService, KafkaConfiguration kafkaConfig) {
        this.eventsSimilarityService = eventsSimilarityService;

        this.consumerConfig = kafkaConfig.getConsumer();
        this.producerConfig = kafkaConfig.getProducer();

        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.producer = new KafkaProducer<>(producerConfig.getProperties());

        registerShutdownHook();
    }

    public void start() {
        log.info("Starting Aggregator service...");
        try {
            subscribe();

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = poll();
                processRecords(records);
                flushProducer();
                commitConsumerAsync();
            }
        } catch (WakeupException ignores) {
            log.info("Получен сигнал завершения работы. WakeupException. Aggregator. AggregationStarter");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            closeSafely();
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал хук на завершение JVM. Прерываю работу консьюмера.");
            consumer.wakeup();
        }));
    }

    private void subscribe() {
        consumer.subscribe(List.of(consumerConfig.getTopic()));
    }

    private ConsumerRecords<String, UserActionAvro> poll() {
        return consumer.poll(consumerConfig.getPollTimeout());
    }

    private void processRecords(ConsumerRecords<String, UserActionAvro> records) {
        int count = 0;

        for (ConsumerRecord<String, UserActionAvro> record : records) {
            log.trace("Обработка сообщения от хаба {} из партиции {} с офсетом {}.",
                    record.key(), record.partition(), record.offset());

            handleRecord(record.value());
            manageOffsets(record, count, consumer);

            count++;
        }
    }

    private void flushProducer() {
        producer.flush();
    }

    private void commitConsumerAsync() {
        consumer.commitAsync();
    }

    private void closeSafely() {
        try {
            producer.flush();
            consumer.commitSync(currentOffsets);
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
            log.info("Закрываем продюсер");
            producer.close();
        }
    }

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                      KafkaConsumer<String, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 100 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации офсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(UserActionAvro userActionAvro) {
        List<EventSimilarityAvro> similarities = eventsSimilarityService.countSimilarity(userActionAvro);

        for (EventSimilarityAvro similarity : similarities) {
            try {
                log.info("Начинаю отправку сообщений {} в топик {}", similarity, producerConfig.getTopic());

                String key = buildKey(similarity);
                ProducerRecord<String, EventSimilarityAvro> record = buildProducerRecord(similarity, key);

                log.info("Отправляю сходство {} -> ключ: {}", similarity, key);

                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Ошибка отправки сообщения в топик {}", producerConfig.getTopic(), exception);
                    } else {
                        log.info("Сообщение отправлено в топик {} partition {} offset {}",
                                producerConfig.getTopic(), metadata.partition(), metadata.offset());
                    }
                });
            } catch (Exception e) {
                log.error("Ошибка обработки события", e);
            }
        }
    }

    private String buildKey(EventSimilarityAvro similarity) {
        return similarity.getEventA() + "_" + similarity.getEventB();
    }

    private ProducerRecord<String, EventSimilarityAvro> buildProducerRecord(EventSimilarityAvro similarity, String key) {
        return new ProducerRecord<>(
                producerConfig.getTopic(),
                null,
                similarity.getTimestamp().toEpochMilli(),
                key,
                similarity
        );
    }
}
