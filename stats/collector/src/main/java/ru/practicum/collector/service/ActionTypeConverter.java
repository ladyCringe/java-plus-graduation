package ru.practicum.collector.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import stats.service.collector.ActionTypeProto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionTypeConverter {

    public static ActionTypeAvro convert(ActionTypeProto proto) {
        if (proto == null) {
            return null;
        }

        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown ActionTypeProto: " + proto);
        };
    }
}
