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
import ru.practicum.analyzer.domain.service.UserActionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserActionProcessor implements Runnable {

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final UserActionService userActionService;

    public UserActionProcessor(KafkaConfiguration config, UserActionService userActionService) {
        KafkaConfiguration.ConsumerConfig consumerConfig = config.getConsumers().get(this.getClass().getSimpleName());

        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.userActionService = userActionService;

        registerShutdownHook();
    }

    @Override
    public void run() {
        try {
            subscribe();

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = poll();
                processRecords(records);
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            log.info("Получен сигнал завершения работы. WakeupException. Analyzer. HubEventProcessor");
        } catch (Exception e) {
            log.error("Ошибка во время обработки сценариев от хабов", e);
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

    private ConsumerRecords<String, UserActionAvro> poll() {
        return consumer.poll(pollTimeout);
    }

    private void processRecords(ConsumerRecords<String, UserActionAvro> records) {
        int count = 0;

        for (ConsumerRecord<String, UserActionAvro> record : records) {
            log.trace("Обработка сообщения от хаба {} из партиции {} с офсетом {}.",
                    record.key(), record.partition(), record.offset());

            userActionService.save(record.value());
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

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                      KafkaConsumer<String, UserActionAvro> consumer) {
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
