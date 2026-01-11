package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.request.entity.RequestStatus;
import ru.practicum.request.service.RequestService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/requests")
public class InternalParticipationRequestController {

    private final RequestService requestService;

    @GetMapping("/{eventId}/count")
    public long countByEventIdAndStatus(@PathVariable Long eventId,
                                        @RequestParam RequestStatus status) {
        return requestService.countEventsInStatus(eventId, status);
    }
}
