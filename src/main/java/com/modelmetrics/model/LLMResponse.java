package com.modelmetrics.model;

public class LLMResponse {
    private String modelName;
    private String responseText;
    private long responseTimeMs;
    private int wordCount;
    private int charCount;

    public LLMResponse(String modelName, String responseText, long responseTimeMs) {
        this.modelName = modelName;
        this.responseText = responseText;
        this.responseTimeMs = responseTimeMs;
        this.wordCount = responseText.split("\\s+").length;
        this.charCount = responseText.length();
    }

    public String getModelName() { return modelName; }
    public String getResponseText() { return responseText; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public int getWordCount() { return wordCount; }
    public int getCharCount() { return charCount; }

    @Override
    public String toString() {
        return String.format("[%s] time=%dms words=%d", modelName, responseTimeMs, wordCount);
    }
}