package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.db.EventSimilarity;
import ru.practicum.analyzer.db.EventSimilarityId;

import java.util.List;

public interface SimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {
    List<EventSimilarity> findByEventAOrEventB(Long a, Long b);
}
