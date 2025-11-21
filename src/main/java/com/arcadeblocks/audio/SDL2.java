package com.arcadeblocks.audio;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA interface для основной библиотеки SDL2
 * Предоставляет доступ к базовым функциям SDL2
 */
public interface SDL2 extends Library {
    
    // Загружаем библиотеку SDL2 динамически
    static SDL2 getInstance() {
        try {
            // Сначала пытаемся загрузить из временной директории (приоритет)
            return Native.load("SDL2", SDL2.class);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Не удалось загрузить SDL2 из временной директории: " + e.getMessage());
            
            // Если не получилось, пробуем загрузить из системных путей
            try {
                return SystemSDL2Loader.loadSDL2();
            } catch (Exception e2) {
                System.err.println("Не удалось загрузить SDL2 из системных путей: " + e2.getMessage());
                throw new RuntimeException("Не удалось загрузить библиотеку SDL2. Проверьте установку SDL2 в системе.", e2);
            }
        }
    }
    
    /**
     * Получение строки ошибки SDL2
     * @return строка с описанием последней ошибки
     */
    String SDL_GetError();

    /**
     * Очистка состояния последней ошибки SDL2
     */
    void SDL_ClearError();
}
