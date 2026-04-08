package com.modelmetrics.ui;

import com.modelmetrics.api.LLMClient;
import com.modelmetrics.evaluation.EvaluationService;
import com.modelmetrics.evaluation.SemanticProcessor;
import com.modelmetrics.model.LLMResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DashboardController {

    @FXML private TextField promptField;
    @FXML private Button runButton;
    @FXML private Button clearButton;
    @FXML private Button exportButton;
    @FXML private Label statusLabel;

    @FXML private Label fastestLabel;
    @FXML private Label fastestTimeLabel;
    @FXML private Label readableLabel;
    @FXML private Label readableScoreLabel;
    @FXML private Label conciseLabel;
    @FXML private Label conciseWordsLabel;
    @FXML private Label promptCountLabel;

    @FXML private BarChart<String, Number> responseTimeChart;
    @FXML private BarChart<String, Number> wordCountChart;

    @FXML private TableView<LLMResponse> resultsTable;
    @FXML private TableColumn<LLMResponse, String> modelCol;
    @FXML private TableColumn<LLMResponse, Long> timeCol;
    @FXML private TableColumn<LLMResponse, Integer> wordsCol;
    @FXML private TableColumn<LLMResponse, Double> readabilityCol;
    @FXML private TableColumn<LLMResponse, String> lengthCategoryCol;
    @FXML private TableColumn<LLMResponse, String> codeBlockCol;
    @FXML private TableColumn<LLMResponse, String> responseCol;
    @FXML private TableColumn<LLMResponse, Double> qualityScoreCol; // ✅ moved here

    private final LLMClient llmClient = new LLMClient();
    private final EvaluationService evaluationService = new EvaluationService();
    private final List<LLMResponse> allResponses = new ArrayList<>();
    private int promptCount = 0;

    private String openAiKey;
    private String geminiKey;
    private String anthropicKey;
    private SemanticProcessor semanticProcessor; // ✅ moved here, not @FXML

    private void loadApiKeys() {
        try (java.io.InputStream input = getClass()
                .getResourceAsStream("/config.properties")) {
            if (input == null) {
                statusLabel.setText("❌ config.properties not found.");
                return;
            }
            java.util.Properties props = new java.util.Properties();
            props.load(input);
            openAiKey = props.getProperty("openai.api.key");
            geminiKey = props.getProperty("gemini.api.key");
            anthropicKey = props.getProperty("anthropic.api.key");
            semanticProcessor = new SemanticProcessor(geminiKey);
        } catch (java.io.IOException e) {
            statusLabel.setText("❌ Failed to load API keys: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        loadApiKeys();

        // ✅ qualityScoreCol wired here, not inside loadApiKeys
        qualityScoreCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getQualityScore()));

        resultsTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<LLMResponse> row =
                    new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    showResponseDetail(row.getItem());
                }
            });
            return row;
        });

        modelCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getModelName()));
        timeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getResponseTimeMs()));
        wordsCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getWordCount()));
        readabilityCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        data.getValue().getReadabilityScore()));
        lengthCategoryCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getLengthCategory()));
        codeBlockCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().isContainsCodeBlock() ? "✓ Yes" : "No"));
        responseCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getResponseText().length() > 100
                                ? data.getValue().getResponseText()
                                .substring(0, 100) + "..."
                                : data.getValue().getResponseText()));
    }

    @FXML
    private void handleRunEvaluation() {
        String rawPrompt = promptField.getText().trim();
        if (rawPrompt.isEmpty()) {
            statusLabel.setText("⚠ Please enter a prompt first.");
            return;
        }

        runButton.setDisable(true);
        statusLabel.setText("🔍 Analyzing and enhancing your prompt...");
        responseTimeChart.getData().clear();
        wordCountChart.getData().clear();

        CompletableFuture<String> cleanedPromptFuture =
                semanticProcessor.cleanAndEnhancePrompt(rawPrompt);
        CompletableFuture<String> promptTypeFuture =
                semanticProcessor.detectPromptType(rawPrompt);

        CompletableFuture.allOf(cleanedPromptFuture, promptTypeFuture)
                .thenAccept(v -> {
                    try {
                        String cleanedPrompt = cleanedPromptFuture.get();
                        String promptType = promptTypeFuture.get();

                        Platform.runLater(() -> {
                            promptField.setText(cleanedPrompt);
                            statusLabel.setText("✨ Prompt enhanced (" + promptType
                                    + ") — sending to all 3 models...");
                        });

                        CompletableFuture<LLMResponse> gptFuture =
                                llmClient.callGPT4o(cleanedPrompt, openAiKey);
                        CompletableFuture<LLMResponse> geminiFuture =
                                llmClient.callGemini(cleanedPrompt, geminiKey);
                        CompletableFuture<LLMResponse> claudeFuture =
                                llmClient.callClaudeSonnet(cleanedPrompt, anthropicKey);

                        CompletableFuture.allOf(gptFuture, geminiFuture, claudeFuture)
                                .thenAccept(v2 -> {
                                    try {
                                        List<LLMResponse> responses = List.of(
                                                gptFuture.get(),
                                                geminiFuture.get(),
                                                claudeFuture.get()
                                        );

                                        responses.forEach(r -> r.setPromptType(promptType));

                                        Platform.runLater(() -> statusLabel.setText(
                                                "⭐ Scoring response quality..."));

                                        CompletableFuture<Double> gptScore =
                                                semanticProcessor.scoreResponseQuality(
                                                        cleanedPrompt,
                                                        responses.get(0).getResponseText());
                                        CompletableFuture<Double> geminiScore =
                                                semanticProcessor.scoreResponseQuality(
                                                        cleanedPrompt,
                                                        responses.get(1).getResponseText());
                                        CompletableFuture<Double> claudeScore =
                                                semanticProcessor.scoreResponseQuality(
                                                        cleanedPrompt,
                                                        responses.get(2).getResponseText());

                                        CompletableFuture.allOf(
                                                        gptScore, geminiScore, claudeScore)
                                                .thenAccept(v3 -> {
                                                    try {
                                                        responses.get(0).setQualityScore(
                                                                gptScore.get());
                                                        responses.get(1).setQualityScore(
                                                                geminiScore.get());
                                                        responses.get(2).setQualityScore(
                                                                claudeScore.get());

                                                        LLMResponse fastest =
                                                                evaluationService
                                                                        .getFastest(responses);
                                                        LLMResponse readable =
                                                                evaluationService
                                                                        .getMostReadable(responses);
                                                        LLMResponse quality =
                                                                evaluationService
                                                                        .getHighestQuality(responses);

                                                        semanticProcessor
                                                                .generateRecommendation(
                                                                        cleanedPrompt,
                                                                        promptType,
                                                                        fastest.getModelName(),
                                                                        readable.getModelName(),
                                                                        quality.getModelName())
                                                                .thenAccept(recommendation -> {
                                                                    allResponses.addAll(responses);
                                                                    promptCount++;
                                                                    Platform.runLater(() ->
                                                                            updateDashboard(
                                                                                    responses,
                                                                                    recommendation,
                                                                                    promptType));
                                                                });

                                                    } catch (Exception e) {
                                                        Platform.runLater(() ->
                                                                statusLabel.setText(
                                                                        "❌ Scoring error: "
                                                                                + e.getMessage()));
                                                    }
                                                });

                                    } catch (Exception e) {
                                        Platform.runLater(() ->
                                                statusLabel.setText(
                                                        "❌ Error: " + e.getMessage()));
                                    }
                                })
                                .whenComplete((v2, e) ->
                                        Platform.runLater(() ->
                                                runButton.setDisable(false)));

                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            statusLabel.setText("❌ Error: " + e.getMessage());
                            runButton.setDisable(false);
                        });
                    }
                });
    }

    @FXML
    private void handleClear() {
        responseTimeChart.getData().clear();
        wordCountChart.getData().clear();
        resultsTable.getItems().clear();
        promptField.clear();
        statusLabel.setText("Dashboard cleared. Enter a new prompt to begin.");
        fastestLabel.setText("—");
        fastestTimeLabel.setText("awaiting results");
        readableLabel.setText("—");
        readableScoreLabel.setText("awaiting results");
        conciseLabel.setText("—");
        conciseWordsLabel.setText("awaiting results");
    }

    @FXML
    private void handleExportCSV() {
        if (allResponses.isEmpty()) {
            statusLabel.setText("⚠ No results to export yet. Run an evaluation first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Results as CSV");
        fileChooser.setInitialFileName("modelmetrics_results.csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(
                exportButton.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Model,Response Time (ms),Word Count,Char Count," +
                        "Sentence Count,Avg Word Length,Readability Score," +
                        "Quality Score,Length Category,Has Code Block\n");
                for (LLMResponse r : allResponses) {
                    writer.write(String.format("%s,%d,%d,%d,%d,%.2f,%.2f,%.1f,%s,%s\n",
                            r.getModelName(),
                            r.getResponseTimeMs(),
                            r.getWordCount(),
                            r.getCharCount(),
                            r.getSentenceCount(),
                            r.getAvgWordLength(),
                            r.getReadabilityScore(),
                            r.getQualityScore(),
                            r.getLengthCategory(),
                            r.isContainsCodeBlock() ? "Yes" : "No"
                    ));
                }
                statusLabel.setText("✅ Results exported to " + file.getName());
            } catch (IOException e) {
                statusLabel.setText("❌ Export failed: " + e.getMessage());
            }
        }
    }

    private void updateDashboard(List<LLMResponse> responses,
                                 String recommendation,
                                 String promptType) {
        responseTimeChart.getData().clear();
        wordCountChart.getData().clear();

        resultsTable.setItems(FXCollections.observableArrayList(responses));

        XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
        timeSeries.setName("Response Time (ms)");
        responses.forEach(r -> timeSeries.getData().add(
                new XYChart.Data<>(r.getModelName(), r.getResponseTimeMs())));
        responseTimeChart.getData().add(timeSeries);

        XYChart.Series<String, Number> wordSeries = new XYChart.Series<>();
        wordSeries.setName("Word Count");
        responses.forEach(r -> wordSeries.getData().add(
                new XYChart.Data<>(r.getModelName(), r.getWordCount())));
        wordCountChart.getData().add(wordSeries);

        LLMResponse fastest = evaluationService.getFastest(responses);
        LLMResponse mostReadable = evaluationService.getMostReadable(responses);
        LLMResponse mostConcise = evaluationService.getMostConcise(responses);

        fastestLabel.setText(fastest.getModelName());
        fastestTimeLabel.setText(fastest.getResponseTimeMs() + "ms");
        readableLabel.setText(mostReadable.getModelName());
        readableScoreLabel.setText("Score: " + mostReadable.getReadabilityScore());
        conciseLabel.setText(mostConcise.getModelName());
        conciseWordsLabel.setText(mostConcise.getWordCount() + " words");
        promptCountLabel.setText(String.valueOf(promptCount));
        statusLabel.setText("💡 " + recommendation);
    }

    private void showResponseDetail(LLMResponse response) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Full Response — " + response.getModelName());
        alert.setHeaderText(response.getModelName()
                + " | " + response.getResponseTimeMs() + "ms"
                + " | " + response.getWordCount() + " words"
                + " | Readability: " + response.getReadabilityScore()
                + " | Quality: " + response.getQualityScore() + "/10");

        TextArea textArea = new TextArea(response.getResponseText());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(300);
        textArea.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 13px;");

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(650);
        alert.showAndWait();
    }
}