package ru.practicum.event.service;

import ru.practicum.event.dto.EventShortDto;

import java.util.List;

public interface RecommendationsService {
    List<EventShortDto> getRecommendations(long userId, int max);
}
