package com.arcadeblocks.audio;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

/**
 * Альтернативный загрузчик SDL2 библиотек из системных путей
 * Используется как fallback, если NativeLibraryLoader не работает
 */
public class SystemSDL2Loader {
    
    private static final String[] SDL2_PATHS = {
        "/usr/lib/libSDL2.so",
        "/usr/lib/libSDL2-2.0.so.0.3200.56",
        "/usr/lib/x86_64-linux-gnu/libSDL2.so",
        "/usr/local/lib/libSDL2.so"
    };
    
    private static final String[] SDL2_MIXER_PATHS = {
        "/usr/lib/libSDL2_mixer.so",
        "/usr/lib/libSDL2_mixer-2.0.so.0.800.1",
        "/usr/lib/x86_64-linux-gnu/libSDL2_mixer.so",
        "/usr/local/lib/libSDL2_mixer.so"
    };
    
    /**
     * Загружает SDL2 библиотеку из системных путей
     */
    public static SDL2 loadSDL2() {
        if (!Platform.isLinux()) {
            throw new UnsupportedOperationException("SystemSDL2Loader поддерживает только Linux");
        }
        
        for (String path : SDL2_PATHS) {
            File lib = new File(path);
            if (lib.exists()) {
                try {
                    // System.out.println("Загружаем SDL2 из: " + path);
                    return Native.load(path, SDL2.class);
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("Не удалось загрузить " + path + ": " + e.getMessage());
                }
            }
        }
        
        throw new RuntimeException("Не удалось найти библиотеку SDL2 в системных путях");
    }
    
    /**
     * Загружает SDL2_mixer библиотеку из системных путей
     */
    public static SDL2Mixer loadSDL2Mixer() {
        if (!Platform.isLinux()) {
            throw new UnsupportedOperationException("SystemSDL2Loader поддерживает только Linux");
        }
        
        for (String path : SDL2_MIXER_PATHS) {
            File lib = new File(path);
            if (lib.exists()) {
                try {
                    // System.out.println("Загружаем SDL2_mixer из: " + path);
                    return Native.load(path, SDL2Mixer.class);
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("Не удалось загрузить " + path + ": " + e.getMessage());
                }
            }
        }
        
        throw new RuntimeException("Не удалось найти библиотеку SDL2_mixer в системных путях");
    }
    
    /**
     * Проверяет доступность библиотек в системе
     */
    public static boolean areSystemLibrariesAvailable() {
        boolean sdl2Available = false;
        boolean mixerAvailable = false;
        
        for (String path : SDL2_PATHS) {
            if (new File(path).exists()) {
                sdl2Available = true;
                break;
            }
        }
        
        for (String path : SDL2_MIXER_PATHS) {
            if (new File(path).exists()) {
                mixerAvailable = true;
                break;
            }
        }
        
        return sdl2Available && mixerAvailable;
    }
}
