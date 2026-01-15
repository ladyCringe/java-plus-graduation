package ru.practicum.analyzer.grpc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reco")
public class RecoProperties {
    private int recentLimit = 10;
    private int neighborsK = 5;
}
