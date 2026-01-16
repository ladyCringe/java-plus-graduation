package ru.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EventsSimilarityServiceImpl implements EventsSimilarityService {

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventTotalWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> countSimilarity(UserActionAvro userAction) {
        long userId = userAction.getUserId();
        long eventId = userAction.getEventId();
        double weight = resolveWeight(userAction);
        Instant timestamp = userAction.getTimestamp();

        log.debug("Processing action: userId={}, eventId={}, weight={}", userId, eventId, weight);

        Map<Long, Double> userWeightsForEvent = getOrCreateUserWeightsForEvent(eventId);
        Double currentMaxWeight = userWeightsForEvent.get(userId);

        if (isWeightNotIncreased(currentMaxWeight, weight)) {
            log.debug("Weight not changed or decreased for userId={}, eventId={}", userId, eventId);
            return Collections.emptyList();
        }

        double previousWeight = currentMaxWeight != null ? currentMaxWeight : 0.0;
        userWeightsForEvent.put(userId, weight);

        updateEventTotalWeight(eventId, weight, previousWeight);

        return updateAndCalculateSimilarities(eventId, userId, weight, previousWeight, timestamp);
    }

    private double resolveWeight(UserActionAvro userAction) {
        return ACTION_WEIGHTS.get(userAction.getActionType());
    }

    private Map<Long, Double> getOrCreateUserWeightsForEvent(long eventId) {
        return eventUserWeights.computeIfAbsent(eventId, k -> new HashMap<>());
    }

    private boolean isWeightNotIncreased(Double currentMaxWeight, double newWeight) {
        return currentMaxWeight != null && newWeight <= currentMaxWeight;
    }

    private void updateEventTotalWeight(long eventId, double newWeight, double previousWeight) {
        double currentTotal = eventTotalWeights.getOrDefault(eventId, 0.0);
        double updatedTotal = currentTotal - previousWeight + newWeight;

        eventTotalWeights.put(eventId, updatedTotal);
        log.debug("Updated total weight for event {}: {} -> {}", eventId, currentTotal, updatedTotal);
    }

    private List<EventSimilarityAvro> updateAndCalculateSimilarities(
            long updatedEventId,
            long userId,
            double newWeight,
            double previousWeight,
            Instant timestamp
    ) {
        List<EventSimilarityAvro> similarities = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long otherEventId = entry.getKey();

            if (otherEventId == updatedEventId) {
                continue;
            }

            Double otherWeight = entry.getValue().get(userId);
            if (otherWeight == null) {
                continue;
            }

            double minWeightDelta = calculateMinWeightDelta(newWeight, previousWeight, otherWeight);
            if (minWeightDelta != 0) {
                updateMinWeightSum(updatedEventId, otherEventId, minWeightDelta);
            }

            double similarity = calculateSimilarity(updatedEventId, otherEventId);
            if (similarity > 0) {
                similarities.add(createEventSimilarity(updatedEventId, otherEventId, similarity, timestamp));
                log.debug("Added similarity for pair ({}, {}): {}", updatedEventId, otherEventId, similarity);
            }
        }

        log.info("Generated {} similarity updates for event {}", similarities.size(), updatedEventId);
        return similarities;
    }

    private double calculateMinWeightDelta(double newWeight, double previousWeight, double otherWeight) {
        double previousMin = Math.min(previousWeight, otherWeight);
        double newMin = Math.min(newWeight, otherWeight);
        return newMin - previousMin;
    }

    private void updateMinWeightSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        double currentSum = minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .getOrDefault(second, 0.0);

        minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .put(second, currentSum + delta);

        log.debug("Updated min weight sum for pair ({}, {}): {} -> {}", first, second, currentSum, currentSum + delta);
    }

    private double calculateSimilarity(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        Double sMin = minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .get(second);

        if (sMin == null || sMin == 0) {
            return 0.0;
        }

        Double sA = eventTotalWeights.get(eventA);
        Double sB = eventTotalWeights.get(eventB);

        if (sA == null || sB == null || sA == 0 || sB == 0) {
            return 0.0;
        }

        double similarity = sMin / (Math.sqrt(sA) * Math.sqrt(sB));
        log.debug("Calculated similarity for ({}, {}): sMin={}, sA={}, sB={}, similarity={}",
                eventA, eventB, sMin, sA, sB, similarity);

        return similarity;
    }

    private EventSimilarityAvro createEventSimilarity(long eventA, long eventB, double similarity, Instant timestamp) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();
    }
}
