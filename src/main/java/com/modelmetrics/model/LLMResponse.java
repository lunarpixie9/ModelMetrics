package com.modelmetrics.model;

public class LLMResponse {
    private String modelName;
    private String responseText;
    private long responseTimeMs;
    private int wordCount;
    private int charCount;
    private int sentenceCount;
    private double avgWordLength;
    private double readabilityScore;
    private boolean containsCodeBlock;
    private String lengthCategory;

    public LLMResponse(String modelName, String responseText, long responseTimeMs) {
        this.modelName = modelName;
        this.responseText = responseText;
        this.responseTimeMs = responseTimeMs;
        this.wordCount = calculateWordCount(responseText);
        this.charCount = responseText.length();
        this.sentenceCount = calculateSentenceCount(responseText);
        this.avgWordLength = calculateAvgWordLength(responseText);
        this.readabilityScore = calculateReadability();
        this.containsCodeBlock = responseText.contains("```");
        this.lengthCategory = calculateLengthCategory();
    }

    // --- Calculation Methods ---

    private int calculateWordCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    private int calculateSentenceCount(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] sentences = text.split("[.!?]+");
        return sentences.length;
    }

    private double calculateAvgWordLength(String text) {
        if (wordCount == 0) return 0;
        String[] words = text.trim().split("\\s+");
        double totalLength = 0;
        for (String word : words) {
            // Strip punctuation before measuring
            totalLength += word.replaceAll("[^a-zA-Z]", "").length();
        }
        return Math.round((totalLength / wordCount) * 100.0) / 100.0;
    }

    private double calculateReadability() {
        // Flesch-Kincaid Reading Ease formula
        // Score 90-100: Very easy, 60-70: Standard, 0-30: Very difficult
        if (wordCount == 0 || sentenceCount == 0) return 0;
        double wordsPerSentence = (double) wordCount / sentenceCount;
        double charsPerWord = avgWordLength;
        double score = 206.835 - (1.015 * wordsPerSentence) - (84.6 * (charsPerWord / 5.0));
        return Math.round(score * 100.0) / 100.0;
    }

    private String calculateLengthCategory() {
        if (wordCount < 50) return "Short";
        if (wordCount < 150) return "Medium";
        return "Long";
    }

    // --- Getters ---

    public String getModelName() { return modelName; }
    public String getResponseText() { return responseText; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public int getWordCount() { return wordCount; }
    public int getCharCount() { return charCount; }
    public int getSentenceCount() { return sentenceCount; }
    public double getAvgWordLength() { return avgWordLength; }
    public double getReadabilityScore() { return readabilityScore; }
    public boolean isContainsCodeBlock() { return containsCodeBlock; }
    public String getLengthCategory() { return lengthCategory; }

    @Override
    public String toString() {
        return String.format("[%s] time=%dms words=%d readability=%.1f category=%s",
                modelName, responseTimeMs, wordCount, readabilityScore, lengthCategory);
    }
}