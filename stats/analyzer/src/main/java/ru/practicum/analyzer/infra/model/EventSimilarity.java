package ru.practicum.analyzer.infra.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "similarities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event1", "event2"})
)
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event1", nullable = false)
    private Long event1;

    @Column(name = "event2", nullable = false)
    private Long event2;

    @Column(name = "similarity", nullable = false)
    private Double similarity;

    @Column(name = "ts", nullable = false)
    private LocalDateTime timestamp;
}
