package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.event.model.EventView;

public interface EventViewRepository extends JpaRepository<EventView, Long> {
    boolean existsByIpAndEventId(String ip, Long eventId);

    Long countByEventId(Long eventId);
}

