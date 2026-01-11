package ru.practicum.main.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.main.event.dto.EventFullDto;

import java.util.Optional;

@FeignClient(name = "event-service", path = "/api/v1/events")
public interface EventClient {
    @GetMapping("/{id}")
    Optional<EventFullDto> getEvent(@PathVariable Long id);
}
