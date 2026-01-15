package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.repository.InteractionRepository;
import ru.practicum.analyzer.repository.SimilarityRepository;
import stats.service.dashboard.*;

import java.util.*;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final InteractionRepository interactionRepo;
    private final SimilarityRepository similarityRepo;
    private final RecoProperties props;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {

        long eventId = request.getEventId();
        long userId = request.getUserId();
        int max = Math.max(0, request.getMaxResults());

        Set<Long> interacted = interactionRepo.findByUserId(userId).stream()
                .map(i -> i.getEventId())
                .collect(Collectors.toSet());

        var sims = similarityRepo.findByEventAOrEventB(eventId, eventId);

        sims.stream()
                .map(s -> {
                    long other = Objects.equals(s.getEventA(), eventId) ? s.getEventB() : s.getEventA();
                    return Map.entry(other, s.getScore());
                })
                .filter(e -> !interacted.contains(e.getKey()))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(max)
                .forEach(e -> responseObserver.onNext(
                        RecommendedEventProto.newBuilder()
                                .setEventId(e.getKey())
                                .setScore(e.getValue())
                                .build()
                ));

        responseObserver.onCompleted();
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {

        long userId = request.getUserId();
        int max = Math.max(0, request.getMaxResults());

        var recent = interactionRepo.findTop10ByUserIdOrderByLastTsDesc(userId);
        if (recent.isEmpty()) {
            responseObserver.onCompleted();
            return;
        }

        Map<Long, Double> userWeights = recent.stream()
                .collect(Collectors.toMap(r -> r.getEventId(), r -> r.getWeight(), (a, b) -> a));

        Set<Long> interactedAll = interactionRepo.findByUserId(userId).stream()
                .map(i -> i.getEventId())
                .collect(Collectors.toSet());

        Map<Long, Double> bestCandidateScore = new HashMap<>();

        for (Long viewedEvent : userWeights.keySet()) {
            var sims = similarityRepo.findByEventAOrEventB(viewedEvent, viewedEvent);
            for (var s : sims) {
                long other = Objects.equals(s.getEventA(), viewedEvent) ? s.getEventB() : s.getEventA();
                if (interactedAll.contains(other)) continue;

                bestCandidateScore.merge(other, s.getScore(), Math::max);
            }
        }

        int neighborsK = props.getNeighborsK();

        List<Map.Entry<Long, Double>> predicted = new ArrayList<>();

        for (Long candidate : bestCandidateScore.keySet()) {
            var sims = similarityRepo.findByEventAOrEventB(candidate, candidate);

            Map<Long, Double> simToViewed = new HashMap<>();
            for (var s : sims) {
                long other = Objects.equals(s.getEventA(), candidate) ? s.getEventB() : s.getEventA();
                if (userWeights.containsKey(other)) {
                    simToViewed.put(other, s.getScore());
                }
            }
            if (simToViewed.isEmpty()) continue;

            var topK = simToViewed.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(neighborsK)
                    .toList();

            double num = 0.0;
            double den = 0.0;

            for (var e : topK) {
                double sim = e.getValue();
                double w = userWeights.get(e.getKey());
                num += sim * w;
                den += sim;
            }

            if (den > 0.0) {
                predicted.add(Map.entry(candidate, num / den));
            }
        }

        predicted.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(max)
                .forEach(e -> responseObserver.onNext(
                        RecommendedEventProto.newBuilder()
                                .setEventId(e.getKey())
                                .setScore(e.getValue())
                                .build()
                ));

        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {

        List<Long> ids = request.getEventIdList();
        if (ids.isEmpty()) {
            responseObserver.onCompleted();
            return;
        }

        var rows = interactionRepo.sumWeightsByEventIds(ids);

        Map<Long, Double> sums = new HashMap<>();
        for (Object[] row : rows) {
            Long eventId = (Long) row[0];
            Double sum = ((Number) row[1]).doubleValue();
            sums.put(eventId, sum);
        }

        for (Long eventId : ids) {
            double sum = sums.getOrDefault(eventId, 0.0);
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(sum)
                    .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void checkUserInteracted(UserEventRequestProto request,
                                    StreamObserver<BoolResponseProto> responseObserver) {

        boolean exists = interactionRepo.existsByUserIdAndEventId(request.getUserId(), request.getEventId());

        responseObserver.onNext(BoolResponseProto.newBuilder().setValue(exists).build());
        responseObserver.onCompleted();
    }

}
