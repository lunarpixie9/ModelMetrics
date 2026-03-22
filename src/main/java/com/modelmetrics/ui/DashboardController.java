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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DashboardController {

    @FXML private TextField promptField;
    @FXML private Button runButton;
    @FXML private Label statusLabel;
    @FXML private BarChart<String, Number> responseTimeChart;
    @FXML private BarChart<String, Number> wordCountChart;
    @FXML private TableView<LLMResponse> resultsTable;
    @FXML private TableColumn<LLMResponse, String> modelCol;
    @FXML private TableColumn<LLMResponse, Long> timeCol;
    @FXML private TableColumn<LLMResponse, Integer> wordsCol;
    @FXML private TableColumn<LLMResponse, String> responseCol;

    private final LLMClient llmClient = new LLMClient();
    private final EvaluationService evaluationService = new EvaluationService();

    // --- Replace with your real API keys ---
    private final String openAiKey = "YOUR_OPENAI_KEY";
    private final String geminiKey = "YOUR_GEMINI_KEY";
    private final String anthropicKey = "YOUR_ANTHROPIC_KEY";

    @FXML
    public void initialize() {
        // Set up table columns
        modelCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getModelName()));
        timeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getResponseTimeMs()));
        wordsCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getWordCount()));
        responseCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getResponseText().length() > 100
                                ? data.getValue().getResponseText().substring(0, 100) + "..."
                                : data.getValue().getResponseText()
                ));
    }

    @FXML
    private void handleRunEvaluation() {
        String prompt = promptField.getText().trim();
        if (prompt.isEmpty()) {
            statusLabel.setText("Please enter a prompt first.");
            return;
        }

        runButton.setDisable(true);
        statusLabel.setText("Running evaluation...");
        responseTimeChart.getData().clear();
        wordCountChart.getData().clear();

        // Call all 3 APIs concurrently
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
                        Platform.runLater(() -> updateDashboard(responses));
                    } catch (Exception e) {
                        Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
                    }
                })
                .whenComplete((v, e) -> Platform.runLater(() -> runButton.setDisable(false)));
    }

    private void updateDashboard(List<LLMResponse> responses) {
        // Update table
        resultsTable.setItems(FXCollections.observableArrayList(responses));

        // Response time chart
        XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
        timeSeries.setName("Response Time");
        responses.forEach(r -> timeSeries.getData().add(
                new XYChart.Data<>(r.getModelName(), r.getResponseTimeMs())));
        responseTimeChart.getData().add(timeSeries);

        // Word count chart
        XYChart.Series<String, Number> wordSeries = new XYChart.Series<>();
        wordSeries.setName("Word Count");
        responses.forEach(r -> wordSeries.getData().add(
                new XYChart.Data<>(r.getModelName(), r.getWordCount())));
        wordCountChart.getData().add(wordSeries);

        // Status summary
        LLMResponse fastest = evaluationService.getFastest(responses);
        statusLabel.setText("✅ Done! Fastest model: " + fastest.getModelName()
                + " (" + fastest.getResponseTimeMs() + "ms)");
    }
}