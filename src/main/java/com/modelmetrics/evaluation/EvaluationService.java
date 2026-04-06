package com.modelmetrics.evaluation;

import com.modelmetrics.model.LLMResponse;
import java.util.*;
import java.util.stream.Collectors;

public class EvaluationService {

    // Fastest model
    public LLMResponse getFastest(List<LLMResponse> responses) {
        return responses.stream()
                .min(Comparator.comparingLong(LLMResponse::getResponseTimeMs))
                .orElseThrow();
    }

    // Slowest model
    public LLMResponse getSlowest(List<LLMResponse> responses) {
        return responses.stream()
                .max(Comparator.comparingLong(LLMResponse::getResponseTimeMs))
                .orElseThrow();
    }

    // Most readable model
    public LLMResponse getMostReadable(List<LLMResponse> responses) {
        return responses.stream()
                .max(Comparator.comparingDouble(LLMResponse::getReadabilityScore))
                .orElseThrow();
    }

    // Most concise model
    public LLMResponse getMostConcise(List<LLMResponse> responses) {
        return responses.stream()
                .min(Comparator.comparingInt(LLMResponse::getWordCount))
                .orElseThrow();
    }

    // Average response time per model
    public Map<String, Double> getAverageResponseTimes(List<LLMResponse> responses) {
        return responses.stream()
                .collect(Collectors.groupingBy(
                        LLMResponse::getModelName,
                        Collectors.averagingLong(LLMResponse::getResponseTimeMs)
                ));
    }

    // Average word count per model
    public Map<String, Double> getAverageWordCounts(List<LLMResponse> responses) {
        return responses.stream()
                .collect(Collectors.groupingBy(
                        LLMResponse::getModelName,
                        Collectors.averagingInt(LLMResponse::getWordCount)
                ));
    }

    // Average readability per model
    public Map<String, Double> getAverageReadability(List<LLMResponse> responses) {
        return responses.stream()
                .collect(Collectors.groupingBy(
                        LLMResponse::getModelName,
                        Collectors.averagingDouble(LLMResponse::getReadabilityScore)
                ));
    }

    // Filter by length category
    public List<LLMResponse> filterByLengthCategory(List<LLMResponse> responses, String category) {
        return responses.stream()
                .filter(r -> r.getLengthCategory().equals(category))
                .collect(Collectors.toList());
    }

    // Filter models that contain code blocks
    public List<LLMResponse> filterCodeResponses(List<LLMResponse> responses) {
        return responses.stream()
                .filter(LLMResponse::isContainsCodeBlock)
                .collect(Collectors.toList());
    }

    // Sort by readability score descending
    public List<LLMResponse> sortByReadability(List<LLMResponse> responses) {
        return responses.stream()
                .sorted(Comparator.comparingDouble(LLMResponse::getReadabilityScore).reversed())
                .collect(Collectors.toList());
    }

    // Sort by response time ascending
    public List<LLMResponse> sortByResponseTime(List<LLMResponse> responses) {
        return responses.stream()
                .sorted(Comparator.comparingLong(LLMResponse::getResponseTimeMs))
                .collect(Collectors.toList());
    }

    // Generate summary string for status label
    public String generateSummary(List<LLMResponse> responses) {
        LLMResponse fastest = getFastest(responses);
        LLMResponse mostReadable = getMostReadable(responses);
        LLMResponse mostConcise = getMostConcise(responses);

        return String.format(
                "Done! Fastest: %s (%dms) | Most Readable: %s (%.1f) | Most Concise: %s (%d words)",
                fastest.getModelName(), fastest.getResponseTimeMs(),
                mostReadable.getModelName(), mostReadable.getReadabilityScore(),
                mostConcise.getModelName(), mostConcise.getWordCount()
        );
    }
}