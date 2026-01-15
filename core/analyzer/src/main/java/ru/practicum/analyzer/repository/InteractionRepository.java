package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.analyzer.db.UserEventInteraction;
import ru.practicum.analyzer.db.UserEventInteractionId;

import java.util.List;

public interface InteractionRepository extends JpaRepository<UserEventInteraction, UserEventInteractionId> {

    List<UserEventInteraction> findTop10ByUserIdOrderByLastTsDesc(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    List<UserEventInteraction> findByUserId(Long userId);

    @Query("select i.eventId, sum(i.weight) from UserEventInteraction i where i.eventId in ?1 group by i.eventId")
    List<Object[]> sumWeightsByEventIds(List<Long> eventIds);
}
