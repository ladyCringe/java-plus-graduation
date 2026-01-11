package ru.practicum.main.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.main.request.entity.RequestStatus;

@FeignClient(name = "request-service", path = "/api/v1/requests")
public interface ParticipationRequestClient {

    @GetMapping("/{eventId}/count")
    long countByEventIdAndStatus(@PathVariable Long eventId, @RequestParam RequestStatus status);
}
