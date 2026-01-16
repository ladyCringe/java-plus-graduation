package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.entity.Event;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.main.event.entity.EventState;
import ru.practicum.stat.client.grpc.AnalyzerGrpcClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecommendationsServiceImpl implements RecommendationsService {

    private final AnalyzerGrpcClient analyzerGrpcClient;
    private final EventRepository eventRepository;

    @Override
    public List<EventShortDto> getRecommendations(long userId, int max) {
        int safeMax = Math.max(0, max);

        var recs = analyzerGrpcClient.recommendationsForUser(userId, safeMax).toList();
        if (recs.isEmpty()) return List.of();

        List<Long> ids = recs.stream().map(r -> r.getEventId()).toList();

        Map<Long, Event> byId = eventRepository.findAllById(ids).stream()
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return recs.stream()
                .filter(r -> byId.containsKey(r.getEventId()))
                .map(r -> {
                    Event e = byId.get(r.getEventId());
                    e.setRating(r.getScore());
                    EventShortDto dto = EventMapper.toShortDto(e);
                    dto.setRating(r.getScore());
                    return dto;
                })
                .toList();
    }
}
