package ru.practicum.collector.service;

import stats.service.collector.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto event);
}
