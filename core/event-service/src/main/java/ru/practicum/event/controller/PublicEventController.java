package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getAllPublicEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @Pattern(regexp = "EVENT_DATE|RATING", message = "sort must be EVENT_DATE or RATING")
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request
    ) {
        log.info("PUBLIC /events from={} size={} ip={} uri={}", from, size, request.getRemoteAddr(), request.getRequestURI());
        List<EventShortDto> newList = eventService.getAllPublicEvents(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);
        log.debug("SUCCESS: PUBLIC /events newList={}", newList);
        return newList;
    }

    @GetMapping("/{id}")
    public EventFullDto getPublishedEventById(@PathVariable Long id,
                                              @RequestHeader("X-EWM-USER-ID") long userId,
                                              HttpServletRequest request) {
        log.info("PUBLIC /events/{} user={}", id, userId);
        EventFullDto dto = eventService.getPublishedEventById(id, userId);
        log.debug("SUCCESS: PUBLIC /events/{} dto={} ip={} uri={}", id, dto, request.getRemoteAddr(), request.getRequestURI());
        return dto;
    }

}

