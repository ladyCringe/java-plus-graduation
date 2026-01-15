package ru.practicum.aggregator.logic;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Component
@Getter
public class WeightsProperties {
    @Value("${weights.view:1.0}") private double view;
    @Value("${weights.register:3.0}") private double register;
    @Value("${weights.like:5.0}") private double like;

    public double weight(ActionTypeAvro t) {
        return switch (t) {
            case VIEW -> view;
            case REGISTER -> register;
            case LIKE -> like;
        };
    }
}
