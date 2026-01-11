package ru.practicum.event.dto;

import lombok.Value;

@Value
public class FindCompilationParams {
    Boolean pinned;
    Integer from;
    Integer size;
}
