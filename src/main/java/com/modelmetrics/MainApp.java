package com.modelmetrics;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/modelmetrics/dashboard.fxml")
        );
        Scene scene = new Scene(loader.load(), 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("/com/modelmetrics/styles.css").toExternalForm()
        );
        stage.setTitle("ModelMetrics");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}