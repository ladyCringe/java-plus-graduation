package ru.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@ConfigurationProperties("analyzer.kafka")
public class KafkaConfiguration {

    private final Map<String, ConsumerConfig> consumers;

    public KafkaConfiguration(Map<String, String> commonProperties, List<ConsumerConfig> consumers) {
        this.consumers = consumers.stream()
                .peek(cfg -> cfg.setProperties(mergeProperties(commonProperties, cfg.getProperties())))
                .collect(Collectors.toMap(ConsumerConfig::getType, Function.identity()));
    }

    private Properties mergeProperties(Map<String, String> commonProperties, Properties specificProperties) {
        Properties merged = new Properties();
        merged.putAll(commonProperties);
        merged.putAll(specificProperties);
        return merged;
    }

    @Setter
    @Getter
    public static class ConsumerConfig {
        private String type;
        private List<String> topics;
        private Duration pollTimeout;
        private Properties properties;

        public ConsumerConfig(String type,
                              List<String> topics,
                              Duration pollTimeout,
                              Map<String, String> properties) {
            this.type = type;
            this.topics = topics;
            this.pollTimeout = pollTimeout;

            this.properties = new Properties(properties.size());
            this.properties.putAll(properties);
        }
    }
}
