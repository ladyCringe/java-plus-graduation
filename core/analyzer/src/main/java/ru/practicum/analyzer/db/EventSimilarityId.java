package ru.practicum.analyzer.db;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarityId implements Serializable {
    private Long eventA;
    private Long eventB;
}
