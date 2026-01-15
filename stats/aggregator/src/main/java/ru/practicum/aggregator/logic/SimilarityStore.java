package ru.practicum.aggregator.logic;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SimilarityStore {

    private final Map<Long, Map<Long, Double>> maxWeights = new HashMap<>();

    private final Map<Long, Double> sums = new HashMap<>();

    private final Map<Long, Map<Long, Double>> minSums = new HashMap<>();

    public double getUserWeight(long eventId, long userId) {
        return maxWeights.getOrDefault(eventId, Map.of()).getOrDefault(userId, 0.0);
    }

    public Set<Long> allEventsSnapshot() {
        return new HashSet<>(maxWeights.keySet());
    }

    public void putUserWeight(long eventId, long userId, double newWeight) {
        maxWeights.computeIfAbsent(eventId, e -> new HashMap<>()).put(userId, newWeight);
        sums.putIfAbsent(eventId, sums.getOrDefault(eventId, 0.0));
    }

    public double sum(long eventId) {
        return sums.getOrDefault(eventId, 0.0);
    }

    public void addToSum(long eventId, double delta) {
        sums.put(eventId, sum(eventId) + delta);
    }

    public double getMinSum(long a, long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        return minSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }

    public void addToMinSum(long a, long b, double delta) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        minSums.computeIfAbsent(first, x -> new HashMap<>());
        minSums.get(first).put(second, getMinSum(first, second) + delta);
    }
}
