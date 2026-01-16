package ru.practicum.analyzer.domain.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface SimilarityService {
    void save(EventSimilarityAvro eventSimilarityAvro);
}
