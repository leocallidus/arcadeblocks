package com.arcadeblocks.video;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

/**
 * JavaFX Media backend used as a fallback when VLC is unavailable.
 */
public class JavaFxMediaBackend implements VideoPlayerBackend {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Runnable onFinished;
    private Runnable onError;

    @Override
    public Node prepareVideo(String videoPath, double width, double height) throws Exception {
        String normalized = videoPath.startsWith("/") ? videoPath.substring(1) : videoPath;
        String mediaUri = null;

        java.net.URL resourceUrl = getClass().getResource("/assets/textures/" + normalized);
        if (resourceUrl != null) {
            if ("jar".equals(resourceUrl.getProtocol())) {
                String extractedPath = VideoResourceExtractor.getInstance().extractVideo(normalized);
                mediaUri = new java.io.File(extractedPath).toURI().toString();
            } else {
                mediaUri = resourceUrl.toExternalForm();
            }
        }

        if (mediaUri == null) {
            throw new IllegalStateException("Media resource not found: " + videoPath);
        }

        Media media = new Media(mediaUri);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnEndOfMedia(() -> {
            if (onFinished != null) {
                Platform.runLater(onFinished);
            }
        });
        mediaPlayer.setOnError(() -> {
            if (onError != null) {
                Platform.runLater(onError);
            }
        });

        mediaView = new MediaView(mediaPlayer);
        mediaView.setPreserveRatio(false);
        mediaView.setFitWidth(width);
        mediaView.setFitHeight(height);

        return mediaView;
    }

    @Override
    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
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
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    @Override
    public boolean isReady() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.READY;
    }

    @Override
    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    @Override
    public double getVolume() {
        return mediaPlayer != null ? mediaPlayer.getVolume() : 0.0;
    }

    @Override
    public void cleanup() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {}
            mediaPlayer.dispose();
        }
        mediaPlayer = null;
        mediaView = null;
        onFinished = null;
        onError = null;
    }

    @Override
    public String getBackendName() {
        return "JavaFX Media";
    }
}
