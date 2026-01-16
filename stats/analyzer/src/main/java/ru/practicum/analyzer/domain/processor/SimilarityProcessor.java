package ru.practicum.analyzer.domain.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.config.KafkaConfiguration;
import ru.practicum.analyzer.domain.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SimilarityProcessor {

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, EventSimilarityAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final SimilarityService similarityService;

    public SimilarityProcessor(KafkaConfiguration config, SimilarityService similarityService) {
        KafkaConfiguration.ConsumerConfig consumerConfig = config.getConsumers().get(this.getClass().getSimpleName());

        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.similarityService = similarityService;

        registerShutdownHook();
    }

    public void start() {
        try {
            subscribe();

            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = poll();
                processRecords(records);
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            log.info("Получен сигнал завершения работы. WakeupException. Analyzer. SnapshotProcessor");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от хабов", e);
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
        log.trace("Подписываемся на топики {}", topics);
        consumer.subscribe(topics);
    }

    private ConsumerRecords<String, EventSimilarityAvro> poll() {
        return consumer.poll(pollTimeout);
    }

    private void processRecords(ConsumerRecords<String, EventSimilarityAvro> records) {
        int count = 0;

        for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
            log.trace("Обработка сообщения от хаба {} из партиции {} с офсетом {}.",
                    record.key(), record.partition(), record.offset());

            similarityService.save(record.value());
            manageOffsets(record, count, consumer);

            count++;
        }
    }

    private void closeSafely() {
        try {
            consumer.commitSync(currentOffsets);
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
        }
    }

    private static void manageOffsets(ConsumerRecord<String, EventSimilarityAvro> record, int count,
                                      KafkaConsumer<String, EventSimilarityAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 100 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }
}
