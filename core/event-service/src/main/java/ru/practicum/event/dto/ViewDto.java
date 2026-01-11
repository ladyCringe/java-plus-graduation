package ru.practicum.event.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViewDto {
    private Long eventId;
    private Long views;
}
