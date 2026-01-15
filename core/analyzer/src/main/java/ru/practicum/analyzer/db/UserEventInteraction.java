package ru.practicum.analyzer.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_event_interactions")
@IdClass(UserEventInteractionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventInteraction {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private double weight;

    @Column(name = "last_ts", nullable = false)
    private LocalDateTime lastTs;
}
