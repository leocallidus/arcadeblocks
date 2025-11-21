package com.arcadeblocks.ui.util;

import com.arcadeblocks.ui.SupportsCleanup;
import com.arcadeblocks.utils.ImageCache;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;

/**
 * Utility helpers for releasing JavaFX resources (images, backgrounds) when cleaning up custom UI nodes.
 * This helps avoid native prism image buffers piling up when views get recreated frequently.
 */
public final class UINodeCleanup {

    private UINodeCleanup() {
    }

    /**
     * Invokes cleanup for nodes that support it and releases Image-backed resources such as ImageView hierarchies.
     *
     * @param node target node
     */
    public static void cleanupNode(Node node) {
        if (node == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            cleanupNodeInternal(node);
        } else {
            Platform.runLater(() -> cleanupNodeInternal(node));
        }
    }

    private static void cleanupNodeInternal(Node node) {
        // Сначала пробуем через интерфейс SupportsCleanup
        if (node instanceof SupportsCleanup supportsCleanup) {
            try {
                supportsCleanup.cleanup();
            } catch (Exception ex) {
                System.err.println("Failed to cleanup UI node " + node.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        } else {
            // Fallback: пробуем через reflection для legacy виджетов
            try {
                java.lang.reflect.Method cleanupMethod = node.getClass().getMethod("cleanup");
                if (cleanupMethod != null) {
                    cleanupMethod.invoke(node);
                }
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
                // Метод cleanup() не найден или недоступен - это нормально
            } catch (Exception ex) {
                System.err.println("Failed to cleanup UI node via reflection " + node.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        releaseImagesInternal(node);
    }

    /**
     * Recursively traverses the specified node and clears image-related state such as ImageView content
     * and Background images on Region instances.
     *
     * @param node root node to clean
     */
    public static void releaseImages(Node node) {
        if (node == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            releaseImagesInternal(node);
        } else {
            Platform.runLater(() -> releaseImagesInternal(node));
        }
    }

    private static void releaseImagesInternal(Node node) {
        if (node == null) {
            return;
        }

        if (node instanceof ImageView imageView) {
            Image image = imageView.getImage();
            if (image != null) {
                ImageCache.forget(image);
            }
            try {
                imageView.setImage(null);
            } catch (Exception ignored) {
                // failing to clear an image should not break cleanup
            }
        }

        if (node instanceof Region region) {
            Background background = region.getBackground();
            if (background != null && background.getImages() != null && !background.getImages().isEmpty()) {
                background.getImages().forEach(bgImage -> {
                    Image image = bgImage.getImage();
                    if (image != null) {
                        ImageCache.forget(image);
                    }
                });
            }
            try {
                region.setBackground(null);
            } catch (Exception ignored) {
                // some custom regions may reject background changes during cleanup
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                releaseImagesInternal(child);
            }
        }
    }
}

