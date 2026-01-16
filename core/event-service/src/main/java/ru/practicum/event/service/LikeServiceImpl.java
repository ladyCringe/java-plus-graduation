package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.stat.client.grpc.AnalyzerGrpcClient;
import ru.practicum.stat.client.grpc.CollectorGrpcClient;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeServiceImpl implements LikeService {

    private final AnalyzerGrpcClient analyzerGrpcClient;
    private final CollectorGrpcClient collectorGrpcClient;

    @Override
    public void like(long userId, long eventId) {
        boolean visited = analyzerGrpcClient.hasInteraction(userId, eventId);
        if (!visited) {
            throw new BadRequestException("Пользователь может лайкать только посещённые мероприятия");
        }
        collectorGrpcClient.sendLike(userId, eventId);
    }
}
