package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.service.RecommendationsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicRecommendationsController {

    private final RecommendationsService recommendationsService;

    @GetMapping("/recommendations")
    public List<EventShortDto> get(@RequestHeader("X-EWM-USER-ID") long userId,
                                   @RequestParam(defaultValue = "10") int size) {
        return recommendationsService.getRecommendations(userId, size);
    }
}
