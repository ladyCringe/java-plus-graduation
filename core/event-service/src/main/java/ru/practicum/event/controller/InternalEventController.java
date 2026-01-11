package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.service.EventService;
import ru.practicum.main.event.dto.EventFullDto;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class InternalEventController {
    private final EventService eventService;

    @GetMapping("/{id}")
    public Optional<EventFullDto> getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }
}