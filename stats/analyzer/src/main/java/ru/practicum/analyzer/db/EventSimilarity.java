package ru.practicum.analyzer.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_similarity")
@IdClass(EventSimilarityId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSimilarity {

    @Id
    @Column(name = "event_a")
    private Long eventA;

    @Id
    @Column(name = "event_b")
    private Long eventB;

    @Column(nullable = false)
    private double score;

    @Column(name = "updated_ts", nullable = false)
    private LocalDateTime updatedTs;
}
