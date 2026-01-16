package ru.practicum.analyzer.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.infra.model.UserAction;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByUserId(Long userId);

    List<UserAction> findAllByEventId(Long eventId);
}
