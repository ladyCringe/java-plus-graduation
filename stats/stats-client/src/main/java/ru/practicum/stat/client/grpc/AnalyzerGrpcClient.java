package ru.practicum.stat.client.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import stats.service.dashboard.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class AnalyzerGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> recommendationsForUser(long userId, int max) {
        max = Math.max(0, max);
        var req = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(max)
                .build();
        return asStream(client.getRecommendationsForUser(req));
    }

    public Stream<RecommendedEventProto> similarEvents(long eventId, long userId, int max) {
        max = Math.max(0, max);
        var req = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(max)
                .build();
        return asStream(client.getSimilarEvents(req));
    }

    public Stream<RecommendedEventProto> interactionsCount(Iterable<Long> eventIds) {
        var b = InteractionsCountRequestProto.newBuilder();
        eventIds.forEach(b::addEventId);
        return asStream(client.getInteractionsCount(b.build()));
    }

    public boolean hasInteraction(long userId, long eventId) {
        var req = UserEventRequestProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .build();
        return client.checkUserInteracted(req).getValue();
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> it) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
    }
}
