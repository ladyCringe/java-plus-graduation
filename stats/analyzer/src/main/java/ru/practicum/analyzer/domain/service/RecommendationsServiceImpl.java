package ru.practicum.analyzer.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.infra.mapper.RecommendationsMapper;
import ru.practicum.analyzer.infra.model.EventSimilarity;
import ru.practicum.analyzer.infra.model.UserAction;
import ru.practicum.analyzer.infra.repository.SimilarityRepository;
import ru.practicum.analyzer.infra.repository.UserInteractionRepository;
import stats.service.dashboard.InteractionsCountRequestProto;
import stats.service.dashboard.RecommendedEventProto;
import stats.service.dashboard.SimilarEventsRequestProto;
import stats.service.dashboard.UserPredictionsRequestProto;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RecommendationsServiceImpl implements RecommendationsService {

    private static final int K_NEIGHBORS = 10;

    private final UserInteractionRepository userInteractionRepository;
    private final SimilarityRepository similarityRepository;

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Long userId = request.getUserId();
        int maxResults = (int) request.getMaxResults();

        log.info("Получение рекомендаций для пользователя {} (max_results: {})", userId, maxResults);

        if (!isValidMaxResults(maxResults)) {
            log.warn("Запрошено недопустимое количество результатов: {}", maxResults);
            return Stream.empty();
        }

        List<UserAction> userInteractions = userInteractionRepository.findAllByUserId(userId);
        if (userInteractions.isEmpty()) {
            log.info("Пользователь {} не имеет взаимодействий, рекомендации невозможны", userId);
            return Stream.empty();
        }

        List<UserAction> sortedInteractions = RecommendationsMapper.sortByTimestampDesc(userInteractions);
        Set<Long> alreadyInteractedEventIds = toEventIdSet(sortedInteractions);

        Map<Long, Double> candidateEvents = collectCandidateEvents(sortedInteractions, alreadyInteractedEventIds);
        if (candidateEvents.isEmpty()) {
            log.info("Для пользователя {} не найдено кандидатов для рекомендаций", userId);
            return Stream.empty();
        }

        return candidateEvents.entrySet().stream()
                .parallel()
                .map(entry -> toRecommendationOrNull(userId, entry.getKey(), alreadyInteractedEventIds))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(maxResults);
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int maxResults = (int) request.getMaxResults();

        log.info("Поиск похожих событий для события {} (user_id: {}, max_results: {})",
                eventId, userId, maxResults);

        if (!isValidMaxResults(maxResults)) {
            log.warn("Запрошено недопустимое количество результатов: {}", maxResults);
            return Stream.empty();
        }

        List<EventSimilarity> similarities = similarityRepository.findAllByEventId(eventId);
        if (similarities.isEmpty()) {
            log.info("Для события {} не найдено похожих событий", eventId);
            return Stream.empty();
        }

        if (userId == 0) {
            return mapSimilarities(similarities.stream(), eventId, maxResults);
        }

        Set<Long> userInteractedEventIds = userInteractionRepository.findAllByUserId(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        Stream<EventSimilarity> filtered = similarities.stream()
                .filter(similarity -> !userInteractedEventIds.contains(otherEventId(similarity, eventId)));

        return mapSimilarities(filtered, eventId, maxResults);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("Получение суммы взаимодействий для {} событий", request.getEventIdCount());

        return request.getEventIdList().stream()
                .distinct()
                .map(this::toInteractionsSumRecommendation)
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed());
    }

    private boolean isValidMaxResults(int maxResults) {
        return maxResults > 0;
    }

    private Set<Long> toEventIdSet(List<UserAction> interactions) {
        return interactions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());
    }

    private Map<Long, Double> collectCandidateEvents(List<UserAction> sortedInteractions,
                                                     Set<Long> alreadyInteractedEventIds) {
        Map<Long, Double> candidateEvents = new HashMap<>();

        for (UserAction interaction : sortedInteractions) {
            Long eventId = interaction.getEventId();
            List<EventSimilarity> similarities = similarityRepository.findAllByEventId(eventId);

            for (EventSimilarity similarity : similarities) {
                Long candidateEventId = otherEventId(similarity, eventId);

                if (alreadyInteractedEventIds.contains(candidateEventId)) {
                    continue;
                }

                double currentScore = candidateEvents.getOrDefault(candidateEventId, 0.0);
                candidateEvents.put(candidateEventId, Math.max(currentScore, similarity.getSimilarity()));
            }
        }

        return candidateEvents;
    }

    private RecommendedEventProto toRecommendationOrNull(Long userId,
                                                         Long candidateEventId,
                                                         Set<Long> alreadyInteractedEventIds) {
        Double predictedScore = calculatePredictedScore(userId, candidateEventId, alreadyInteractedEventIds);
        if (predictedScore == null) {
            return null;
        }
        return RecommendationsMapper.toRecommendedEventProto(candidateEventId, predictedScore);
    }

    private Stream<RecommendedEventProto> mapSimilarities(Stream<EventSimilarity> similarities,
                                                          Long targetEventId,
                                                          int maxResults) {
        return similarities
                .sorted(Comparator.comparing(EventSimilarity::getSimilarity).reversed())
                .limit(maxResults)
                .map(similarity -> RecommendationsMapper.toRecommendedEventProto(similarity, targetEventId))
                .filter(Objects::nonNull);
    }

    private RecommendedEventProto toInteractionsSumRecommendation(Long eventId) {
        List<UserAction> interactions = userInteractionRepository.findAllByEventId(eventId);

        if (interactions.isEmpty()) {
            return RecommendationsMapper.toRecommendedEventProto(eventId, 0.0);
        }

        double totalScore = interactions.stream()
                .collect(Collectors.groupingBy(
                        UserAction::getUserId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(UserAction::getRating)),
                                opt -> opt.map(UserAction::getRating).orElse(0.0)
                        )
                ))
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        return RecommendationsMapper.toRecommendedEventProto(eventId, totalScore);
    }

    private Double calculatePredictedScore(Long userId,
                                           Long candidateEventId,
                                           Set<Long> userInteractedEventIds) {
        List<EventSimilarity> allSimilarities = similarityRepository.findAllByEventId(candidateEventId);

        List<EventSimilarity> neighborSimilarities = allSimilarities.stream()
                .filter(similarity -> userInteractedEventIds.contains(otherEventId(similarity, candidateEventId)))
                .sorted(Comparator.comparing(EventSimilarity::getSimilarity).reversed())
                .limit(K_NEIGHBORS)
                .toList();

        if (neighborSimilarities.isEmpty()) {
            return null;
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity similarity : neighborSimilarities) {
            Long similarEventId = otherEventId(similarity, candidateEventId);

            Optional<UserAction> userAction = userInteractionRepository.findByUserIdAndEventId(userId, similarEventId);
            if (userAction.isPresent()) {
                double rating = userAction.get().getRating();
                double similarityScore = similarity.getSimilarity();

                weightedSum += rating * similarityScore;
                similaritySum += similarityScore;
            }
        }

        if (similaritySum == 0) {
            return 0.0;
        }

        return weightedSum / similaritySum;
    }

    private Long otherEventId(EventSimilarity similarity, Long referenceEventId) {
        return similarity.getEvent1().equals(referenceEventId)
                ? similarity.getEvent2()
                : similarity.getEvent1();
    }
}
