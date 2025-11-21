package com.arcadeblocks.utils;

import com.almasb.fxgl.dsl.FXGL;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.image.Image;

/**
 * Lightweight cache for JavaFX {@link Image} instances that are used throughout the UI.
 * The cache keeps images via {@link SoftReference} so that the JVM can reclaim native
 * textures when memory becomes constrained. Images are loaded through FXGL's asset
 * loader streams to avoid the global asset cache retaining strong references and
 * preventing old resolutions from being garbage-collected.
 */
public final class ImageCache {

    private static final Map<String, SoftReference<Image>> CACHE = new ConcurrentHashMap<>();

    private ImageCache() {
    }

    /**
     * Returns a cached {@link Image} for the given asset path, loading it on demand.
     *
     * @param assetPath relative resource path (e.g. {@code background.png})
     * @return cached image instance or {@code null} when the path is blank or loading fails
     */
    public static Image get(String assetPath) {
        if (assetPath == null) {
            return null;
        }

        String key = assetPath.trim();
        if (key.isEmpty()) {
            return null;
        }

        SoftReference<Image> reference = CACHE.get(key);
        Image image = reference != null ? reference.get() : null;

        if (image == null) {
            if (reference != null) {
                CACHE.remove(key, reference);
            }

            image = loadImage(key);
            if (image != null) {
                CACHE.put(key, new SoftReference<>(image));
            }
        }

        return image;
    }

    /**
     * Clears the cache. Intended for tests; the next {@link #get(String)} call will reload the image.
     */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * Marks an image for potential garbage collection by removing its cache entry if found.
     * Since the cache uses SoftReference, images will be automatically reclaimed when memory
     * is constrained. This method provides a way to explicitly hint that an image is no longer needed.
     *
     * @param image the image to forget (may be null)
     */
    public static void forget(Image image) {
        if (image == null) {
            return;
        }

        // Since we cache by path string, we need to find the entry by value.
        // This is a linear scan, but it's acceptable for cleanup operations.
        CACHE.entrySet().removeIf(entry -> {
            Image cached = entry.getValue().get();
            return cached == null || cached == image;
        });
    }

    private static Image loadImage(String assetPath) {
        String resource = normalisePath(assetPath);

        try (InputStream stream = FXGL.getAssetLoader().getStream(resource)) {
            Image image = new Image(stream);
            if (image.isError()) {
                throw image.getException();
            }
            return image;
        } catch (Exception ex) {
            System.err.println("Failed to load image \"" + assetPath + "\": " + ex.getMessage());
            return null;
        }
    }

    private static String normalisePath(String assetPath) {
        String path = assetPath.replace('\\', '/').trim();

        if (path.startsWith("/")) {
            return path;
        }

        if (path.startsWith("assets/")) {
            return "/" + path;
        }

        if (path.startsWith("textures/")) {
            return "/assets/" + path;
        }

        return "/assets/textures/" + path;
    }
}
