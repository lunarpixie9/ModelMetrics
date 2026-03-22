ModelMetrics

A Java desktop application that evaluates and compares the performance of multiple Large Language Models (LLMs) in real time. Built with JavaFX for visualization and Java Streams for data analysis.

## What It Does

- Sends a user-entered prompt to GPT-4o, Gemini 1.5 Pro, and Claude Sonnet simultaneously
- Measures and compares response time and word count across models
- Displays results in a live dashboard with bar charts and a data table
- Identifies the fastest model after each evaluation run

## Tech Stack

- **Language:** Java 21
- **UI:** JavaFX 21
- **HTTP:** Java HttpClient (built-in)
- **JSON Parsing:** Gson
- **Build Tool:** Maven
- **Testing:** JUnit 5

## Project Structure
```
src/
└── main/
    ├── java/com/modelmetrics/
    │   ├── MainApp.java              # Entry point
    │   ├── api/
    │   │   └── LLMClient.java        # API calls to all 3 LLMs
    │   ├── evaluation/
    │   │   └── EvaluationService.java # Streams-based analysis
    │   ├── model/
    │   │   └── LLMResponse.java      # Data model
    │   └── ui/
    │       └── DashboardController.java # JavaFX controller
    └── resources/com/modelmetrics/
        └── dashboard.fxml            # UI layout
```

## Prerequisites
Make sure you have the following installed:
- [Java 21](https://adoptium.net/) (Temurin recommended)
- [Maven](https://maven.apache.org/install.html)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) (recommended)

## Setup & Running

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/ModelMetrics.git
cd ModelMetrics 
```

### 2. Add your API keys
Open `src/main/java/com/modelmetrics/ui/DashboardController.java` and replace:
```java
private final String openAiKey = "YOUR_OPENAI_KEY";
private final String geminiKey = "YOUR_GEMINI_KEY";
private final String anthropicKey = "YOUR_ANTHROPIC_KEY";
```
with your actual API keys.

> **Get API keys here:**
> - GPT-4o → [platform.openai.com](https://platform.openai.com)
> - Gemini → [aistudio.google.com](https://aistudio.google.com) (free)
> - Claude Sonnet → [console.anthropic.com](https://console.anthropic.com)

### 3. Run the app
```
mvn clean javafx:run
```

The ModelMetrics dashboard will open automatically.

## How to Use

1. Type any prompt in the input field (e.g. *"Explain recursion in simple terms"*)
2. Click 'Run Evaluation'
3. Wait for all 3 models to respond
4. View the comparison charts and response table
