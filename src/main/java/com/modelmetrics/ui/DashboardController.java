package com.modelmetrics.ui;

import com.modelmetrics.api.LLMClient;
import com.modelmetrics.evaluation.EvaluationService;
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

    private final LLMClient llmClient = new LLMClient();
    private final EvaluationService evaluationService = new EvaluationService();
    private final List<LLMResponse> allResponses = new ArrayList<>();
    private int promptCount = 0;

    // --- Replace with your real API keys ---
    private final String openAiKey = "YOUR_OPENAI_KEY";
    private final String geminiKey = "YOUR_GEMINI_KEY";
    private final String anthropicKey = "YOUR_ANTHROPIC_KEY";

    @FXML
    public void initialize() {
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
                                ? data.getValue().getResponseText().substring(0, 100) + "..."
                                : data.getValue().getResponseText()));
    }

    @FXML
    private void handleRunEvaluation() {
        String prompt = promptField.getText().trim();
        if (prompt.isEmpty()) {
            statusLabel.setText("⚠ Please enter a prompt first.");
            return;
        }

        runButton.setDisable(true);
        statusLabel.setText("⏳ Sending prompt to all 3 models...");
        responseTimeChart.getData().clear();
        wordCountChart.getData().clear();

        CompletableFuture<LLMResponse> gptFuture = llmClient.callGPT4o(prompt, openAiKey);
        CompletableFuture<LLMResponse> geminiFuture = llmClient.callGemini(prompt, geminiKey);
        CompletableFuture<LLMResponse> claudeFuture = llmClient.callClaudeSonnet(prompt, anthropicKey);

        CompletableFuture.allOf(gptFuture, geminiFuture, claudeFuture)
                .thenAccept(v -> {
                    try {
                        List<LLMResponse> responses = List.of(
                                gptFuture.get(),
                                geminiFuture.get(),
                                claudeFuture.get()
                        );
                        allResponses.addAll(responses);
                        promptCount++;
                        Platform.runLater(() -> updateDashboard(responses));
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                statusLabel.setText("❌ Error: " + e.getMessage()));
                    }
                })
                .whenComplete((v, e) ->
                        Platform.runLater(() -> runButton.setDisable(false)));
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

        java.io.File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Model,Response Time (ms),Word Count,Char Count,Sentence Count,Avg Word Length,Readability Score,Length Category,Has Code Block\n");
                for (LLMResponse r : allResponses) {
                    writer.write(String.format("%s,%d,%d,%d,%d,%.2f,%.2f,%s,%s\n",
                            r.getModelName(),
                            r.getResponseTimeMs(),
                            r.getWordCount(),
                            r.getCharCount(),
                            r.getSentenceCount(),
                            r.getAvgWordLength(),
                            r.getReadabilityScore(),
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

    private void updateDashboard(List<LLMResponse> responses) {
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

        statusLabel.setText(evaluationService.generateSummary(responses));
    }
}