package com.modelmetrics.evaluation;

import com.modelmetrics.model.LLMResponse;
import java.util.*;
import java.util.stream.Collectors;

public class EvaluationService {

    // Get the fastest model
    public LLMResponse getFastest(List<LLMResponse> responses) {
        return responses.stream()
                .min(Comparator.comparingLong(LLMResponse::getResponseTimeMs))
                .orElseThrow();
    }

    // Get the slowest model
    public LLMResponse getSlowest(List<LLMResponse> responses) {
        return responses.stream()
                .max(Comparator.comparingLong(LLMResponse::getResponseTimeMs))
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

    // Filter responses by model name
    public List<LLMResponse> filterByModel(List<LLMResponse> responses, String modelName) {
        return responses.stream()
                .filter(r -> r.getModelName().equals(modelName))
                .collect(Collectors.toList());
    }

    // Sort by response time ascending
    public List<LLMResponse> sortByResponseTime(List<LLMResponse> responses) {
        return responses.stream()
                .sorted(Comparator.comparingLong(LLMResponse::getResponseTimeMs))
                .collect(Collectors.toList());
    }
}