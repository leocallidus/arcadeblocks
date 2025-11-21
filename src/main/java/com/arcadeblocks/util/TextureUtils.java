package com.arcadeblocks.util;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.image.Image;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for texture pooling to prevent VRAM leaks on Windows d3d11.
 * Keeps one decoded Image per sprite and hands out lightweight Texture views
 * instead of calling FXGL.texture() on every spawn.
 */
public final class TextureUtils {

    private TextureUtils() {
    }

    // КРИТИЧНО: Кэш декодированных изображений для переиспользования текстур
    private static final Map<String, SoftReference<Image>> gameplayImageCache = new ConcurrentHashMap<>();

    /**
     * Load a texture and scale it to the requested size. This allows using high-resolution
     * assets (e.g. 1024x1024) while keeping expected gameplay dimensions.
     * 
     * КРИТИЧНО: Использует texture pooling для предотвращения утечек VRAM на Windows d3d11.
     * Переиспользует одно декодированное Image на спрайт и создает легковесные Texture views.
     *
     * @param textureName  name of the texture in the asset folder
     * @param targetWidth  required width in the game world
     * @param targetHeight required height in the game world
     * @return scaled texture instance
     */
    public static Texture loadScaledTexture(String textureName, double targetWidth, double targetHeight) {
        // КРИТИЧНО: Получаем или создаем декодированное изображение из кэша
        Image cachedImage = null;
        SoftReference<Image> reference = gameplayImageCache.get(textureName);
        if (reference != null) {
            cachedImage = reference.get();
            if (cachedImage == null) {
                gameplayImageCache.remove(textureName, reference);
            }
        }

        if (cachedImage == null) {
            cachedImage = loadImageForCache(textureName);
            if (cachedImage != null) {
                gameplayImageCache.put(textureName, new SoftReference<>(cachedImage));
            }
        }

        Texture texture;
        if (cachedImage != null) {
            // КРИТИЧНО: Создаем легковесный Texture view из кэшированного изображения
            texture = new Texture(cachedImage);
        } else {
            // Fallback: используем старый путь если кэш не сработал
            Texture baseTexture = FXGL.texture(textureName);
            texture = baseTexture.copy();
        }

        texture.setFitWidth(targetWidth);
        texture.setFitHeight(targetHeight);

        return texture;
    }

    /**
     * Convenience overload for square textures.
     *
     * @param textureName name of the texture in the asset folder
     * @param targetSize  required size (width and height) in the game world
     * @return scaled texture instance
     */
    public static Texture loadScaledTexture(String textureName, double targetSize) {
        return loadScaledTexture(textureName, targetSize, targetSize);
    }

    /**
     * КРИТИЧНО: Очистка кэша текстур геймплея для освобождения VRAM.
     * Используется при переключении разрешения или для тестов утечек.
     */
    public static void clearGameplayTextureCache() {
        gameplayImageCache.clear();
    }

    private static Image loadImageForCache(String textureName) {
        try {
            Image image = com.arcadeblocks.utils.ImageCache.get(textureName);
            if (image != null) {
                return image;
            }
            Texture baseTexture = FXGL.texture(textureName);
            return baseTexture.getImage();
        } catch (Exception e) {
            return null;
        }
    }
}
