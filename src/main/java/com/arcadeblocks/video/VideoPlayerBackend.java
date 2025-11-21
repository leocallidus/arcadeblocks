package com.arcadeblocks.video;

import javafx.scene.Node;

/**
 * Abstract interface for video playback backends
 * Allows switching between JavaFX Media and VLCJ
 */
public interface VideoPlayerBackend {
    
    /**
     * Prepare the video for playback
     * @param videoPath path to video file (relative to assets/textures/)
     * @param width target display width
     * @param height target display height
     * @return JavaFX Node for displaying video (ImageView or Pane)
     */
    Node prepareVideo(String videoPath, double width, double height) throws Exception;
    
    /**
     * Start video playback
     */
    void play();
    
    /**
     * Pause video playback
     */
    void pause();
    
    /**
     * Resume video playback
     */
    void resume();
    
    /**
     * Stop video playback and cleanup
     */
    void stop();
    
    /**
     * Set callback when video finishes
     */
    void setOnFinished(Runnable callback);
    
    /**
     * Set callback when video encounters an error
     */
    void setOnError(Runnable callback);
    
    /**
     * Check if video is currently playing
     */
    boolean isPlaying();
    
    /**
     * Check if video is ready to play
     */
    boolean isReady();
    
    /**
     * Set volume (0.0 to 1.0)
     */
    void setVolume(double volume);
    
    /**
     * Get volume (0.0 to 1.0)
     */
    double getVolume();
    
    /**
     * Cleanup resources
     * CRITICAL: Must be called to prevent memory leaks
     */
    void cleanup();
    
    /**
     * Get backend name for debugging
     */
    String getBackendName();
}

