package com.arcadeblocks.video;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Stub/No-op video backend for when VLC is unavailable
 * Returns empty nodes and does nothing for playback operations
 */
public class StubVideoBackend implements VideoPlayerBackend {
    
    private Runnable onFinished;
    private Runnable onError;
    private boolean ready = true; // Always ready since it does nothing
    
    @Override
    public Node prepareVideo(String videoPath, double width, double height) throws Exception {
        // Return empty pane with specified dimensions
        Pane emptyPane = new Pane();
        emptyPane.setPrefWidth(width);
        emptyPane.setPrefHeight(height);
        emptyPane.setStyle("-fx-background-color: black;");
        
        // Simulate immediate completion
        if (onFinished != null) {
            javafx.application.Platform.runLater(() -> {
                if (onFinished != null) {
                    onFinished.run();
                }
            });
        }
        
        return emptyPane;
    }
    
    @Override
    public void play() {
        // No-op
    }
    
    @Override
    public void pause() {
        // No-op
    }
    
    @Override
    public void resume() {
        // No-op
    }
    
    @Override
    public void stop() {
        // No-op
    }
    
    @Override
    public void setOnFinished(Runnable callback) {
        this.onFinished = callback;
    }
    
    @Override
    public void setOnError(Runnable callback) {
        this.onError = callback;
    }
    
    @Override
    public boolean isPlaying() {
        return false; // Never playing
    }
    
    @Override
    public boolean isReady() {
        return ready;
    }
    
    @Override
    public void setVolume(double volume) {
        // No-op
    }
    
    @Override
    public double getVolume() {
        return 0.0;
    }
    
    @Override
    public void cleanup() {
        onFinished = null;
        onError = null;
    }
    
    @Override
    public String getBackendName() {
        return "Stub (No Video)";
    }
}
