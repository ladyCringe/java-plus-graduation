package ru.practicum.analyzer.domain.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionService {
    void save(UserActionAvro userActionAvro);
}
