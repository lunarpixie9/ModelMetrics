package com.modelmetrics.evaluation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SemanticProcessor {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String geminiApiKey;

    public SemanticProcessor(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    // ─── 1. Fix grammar and clarify prompt ───
    public CompletableFuture<String> cleanAndEnhancePrompt(String rawPrompt) {
        String systemInstruction =
                "You are a prompt enhancer. The user will give you a prompt that may contain " +
                        "grammar mistakes, spelling errors, or unclear phrasing. " +
                        "Fix any grammar or spelling mistakes, make it clear and well-structured, " +
                        "but do NOT change the meaning or add extra context. " +
                        "Return ONLY the cleaned prompt text. No explanation, no preamble.";

        // FIX: The user's raw prompt is passed as separate data — never embedded
        // inside the instruction string via .formatted(). This prevents the user's
        // text from being interpreted as part of the instruction.
        return callGemini(systemInstruction, rawPrompt).thenApply(response -> {
            System.out.println("CLEANED PROMPT: " + response);
            return response.isEmpty() ? rawPrompt : response.trim();
        });
    }

    // ─── 2. Detect prompt type ───
    public CompletableFuture<String> detectPromptType(String prompt) {
        String systemInstruction =
                "Classify the user's prompt into exactly one of these categories: " +
                        "MATH, CODING, FACTUAL, CREATIVE, ANALYTICAL, GENERAL. " +
                        "Return ONLY the category name. Nothing else — no punctuation, no explanation.";

        return callGemini(systemInstruction, prompt).thenApply(response -> {
            String type = response.trim().toUpperCase();
            if (!type.matches("MATH|CODING|FACTUAL|CREATIVE|ANALYTICAL|GENERAL")) {
                return "GENERAL";
            }
            return type;
        });
    }

    // ─── 3. Score semantic quality of responses ───
    public CompletableFuture<Double> scoreResponseQuality(String prompt, String response) {
        String systemInstruction =
                "You are an expert LLM evaluator. The user will give you a prompt and a " +
                        "model's response separated by '===RESPONSE==='. " +
                        "Score the response on a scale of 0.0 to 10.0, considering: " +
                        "accuracy, completeness, clarity, and relevance. " +
                        "Return ONLY a decimal number between 0.0 and 10.0. Nothing else.";

        // FIX: Prompt and response are joined with a clear separator so Gemini
        // cannot mistake the evaluated response text for a new instruction.
        String truncatedResponse = response.length() > 500
                ? response.substring(0, 500) + "..." : response;
        String userContent = "Prompt: " + prompt + "\n===RESPONSE===\n" + truncatedResponse;

        return callGemini(systemInstruction, userContent).thenApply(result -> {
            try {
                double score = Double.parseDouble(result.trim());
                return Math.max(0.0, Math.min(10.0, score)); // clamp to valid range
            } catch (NumberFormatException e) {
                System.err.println("SemanticProcessor: could not parse score '" + result + "'");
                return 5.0;
            }
        });
    }

    // ─── 4. Generate AI recommendation ───
    public CompletableFuture<String> generateRecommendation(
            String prompt,
            String promptType,
            String fastestModel,
            String mostReadableModel,
            String highestQualityModel) {

        String systemInstruction =
                "You are an AI model advisor. The user will give you evaluation results. " +
                        "Write ONE concise sentence recommending the best model for this type of task. " +
                        "Be specific and helpful. No bullet points, no explanation — one sentence only.";

        String userContent = String.format(
                "Prompt type: %s%n" +
                        "Fastest model: %s%n" +
                        "Most readable model: %s%n" +
                        "Highest quality model: %s",
                promptType, fastestModel, mostReadableModel, highestQualityModel);

        return callGemini(systemInstruction, userContent)
                .thenApply(r -> r.isEmpty()
                        ? "Use " + highestQualityModel + " for best quality on " + promptType + " tasks."
                        : r.trim());
    }

    // ─── Internal Gemini call ───
    // Takes a system instruction and user content as separate arguments.
    // Both are passed to Gson's addProperty() which handles all JSON escaping —
    // no manual .replace() calls needed.
    private CompletableFuture<String> callGemini(String systemInstruction, String userContent) {

        // Build: { "system_instruction": { "parts": [{ "text": "..." }] },
        //          "contents": [{ "role": "user", "parts": [{ "text": "..." }] }] }

        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemInstruction);
        JsonArray sysParts = new JsonArray();
        sysParts.add(sysPart);
        JsonObject sysInstruction = new JsonObject();
        sysInstruction.add("parts", sysParts);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userContent);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        JsonObject userTurn = new JsonObject();
        userTurn.addProperty("role", "user");
        userTurn.add("parts", userParts);
        JsonArray contents = new JsonArray();
        contents.add(userTurn);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
        generationConfig.addProperty("maxOutputTokens", 512);

        JsonObject body = new JsonObject();
        body.add("system_instruction", sysInstruction);
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://generativelanguage.googleapis.com/v1beta/models/" +
                                "gemini-2.0-flash:generateContent?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response.body())
                                .getAsJsonObject();

                        if (!json.has("candidates") || json.get("candidates").isJsonNull()) {
                            System.err.println("SemanticProcessor: no 'candidates'. Body: "
                                    + response.body());
                            return "";
                        }

                        JsonArray candidates = json.getAsJsonArray("candidates");
                        if (candidates.isEmpty()) {
                            System.err.println("SemanticProcessor: 'candidates' is empty.");
                            return "";
                        }

                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        if (!candidate.has("content") || candidate.get("content").isJsonNull()) {
                            System.err.println("SemanticProcessor: no 'content' in candidate.");
                            return "";
                        }

                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        if (!contentObj.has("parts") || contentObj.get("parts").isJsonNull()) {
                            System.err.println("SemanticProcessor: no 'parts' in content.");
                            return "";
                        }

                        JsonArray parts = contentObj.getAsJsonArray("parts");
                        if (parts.isEmpty()) {
                            System.err.println("SemanticProcessor: 'parts' is empty.");
                            return "";
                        }

                        JsonElement textEl = parts.get(0).getAsJsonObject().get("text");
                        if (textEl == null || textEl.isJsonNull()) {
                            System.err.println("SemanticProcessor: no 'text' in first part.");
                            return "";
                        }

                        return textEl.getAsString();

                    } catch (Exception e) {
                        System.err.println("SemanticProcessor error: " + e.getMessage());
                        System.err.println("Raw body: " + response.body());
                        return "";
                    }
                });
    }
}
