package com.modelmetrics.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.modelmetrics.model.LLMResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class LLMClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // OpenAI - GPT-4o
    public CompletableFuture<LLMResponse> callGPT4o(String prompt, String apiKey) {
        String body = """
                {
                    "model": "gpt-4o",
                    "messages": [{"role": "user", "content": "%s"}],
                    "max_tokens": 300
                }
                """.formatted(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long start = System.currentTimeMillis();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long time = System.currentTimeMillis() - start;
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String text = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    return new LLMResponse("GPT-4o", text, time);
                });
    }

    // Google - Gemini 1.5 Pro
    public CompletableFuture<LLMResponse> callGemini(String prompt, String apiKey) {
        String body = """
                {
                    "contents": [{"parts": [{"text": "%s"}]}]
                }
                """.formatted(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long start = System.currentTimeMillis();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long time = System.currentTimeMillis() - start;
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String text = json.getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();
                    return new LLMResponse("Gemini 1.5 Pro", text, time);
                });
    }

    // Anthropic - Claude Sonnet
    public CompletableFuture<LLMResponse> callClaudeSonnet(String prompt, String apiKey) {
        String body = """
                {
                    "model": "claude-sonnet-4-20250514",
                    "max_tokens": 300,
                    "messages": [{"role": "user", "content": "%s"}]
                }
                """.formatted(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long start = System.currentTimeMillis();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long time = System.currentTimeMillis() - start;
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String text = json.getAsJsonArray("content")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();
                    return new LLMResponse("Claude Sonnet", text, time);
                });
    }
}