package ru.practicum.analyzer.domain.service;

import stats.service.dashboard.InteractionsCountRequestProto;
import stats.service.dashboard.RecommendedEventProto;
import stats.service.dashboard.SimilarEventsRequestProto;
import stats.service.dashboard.UserPredictionsRequestProto;

import java.util.stream.Stream;

public interface RecommendationsService {

    Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);

    Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);
}
