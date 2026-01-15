package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.service.LikeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicLikeController {

    private final LikeService likeService;

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void like(@PathVariable long eventId,
                     @RequestHeader("X-EWM-USER-ID") long userId) {
        likeService.like(userId, eventId);
    }
}
