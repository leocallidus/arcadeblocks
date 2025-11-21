package com.arcadeblocks;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * A simple JavaFX application for testing functionality
 */
public class SimpleApp extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Arcade Blocks - The project has been successfully assembled!");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: #00ff00;");
        
        StackPane root = new StackPane();
        root.getChildren().add(label);
        
        Scene scene = new Scene(root, 600, 400);
        scene.setFill(javafx.scene.paint.Color.BLACK);
        
        primaryStage.setTitle("Arcade Blocks");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
