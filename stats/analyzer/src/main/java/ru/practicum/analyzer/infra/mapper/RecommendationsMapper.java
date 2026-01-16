package ru.practicum.analyzer.infra.mapper;

import ru.practicum.analyzer.infra.model.EventSimilarity;
import ru.practicum.analyzer.infra.model.UserAction;
import stats.service.dashboard.RecommendedEventProto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecommendationsMapper {

    private RecommendationsMapper() {
    }

    public static RecommendedEventProto toRecommendedEventProto(EventSimilarity similarity, Long targetEventId) {
        if (similarity == null) {
            return null;
        }

        Long recommendedEventId = similarity.getEvent1().equals(targetEventId)
                ? similarity.getEvent2()
                : similarity.getEvent1();

        return RecommendedEventProto.newBuilder()
                .setEventId(recommendedEventId)
                .setScore(similarity.getSimilarity())
                .build();
    }

    public static RecommendedEventProto toRecommendedEventProto(Long eventId, Double predictedScoreOrInteractionsSum) {
        if (eventId == null || predictedScoreOrInteractionsSum == null) {
            return null;
        }

        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(predictedScoreOrInteractionsSum)
                .build();
    }

    public static Map<Long, List<UserAction>> groupByUser(List<UserAction> userActions) {
        return userActions.stream()
                .collect(Collectors.groupingBy(UserAction::getUserId));
    }

    public static List<UserAction> sortByTimestampDesc(List<UserAction> userActions) {
        return userActions.stream()
                .sorted(Comparator.comparing(UserAction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
}
