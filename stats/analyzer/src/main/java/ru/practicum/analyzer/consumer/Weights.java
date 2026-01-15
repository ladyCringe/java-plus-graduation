package ru.practicum.analyzer.consumer;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Component
public class Weights {

    public double weight(ActionTypeAvro type) {
        if (type == null) return 0.0;

        return switch (type) {
            case VIEW -> 1.0;
            case REGISTER -> 3.0;
            case LIKE -> 5.0;
        };
    }
}
