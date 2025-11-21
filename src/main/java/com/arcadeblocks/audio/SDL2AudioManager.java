package com.arcadeblocks.audio;

import com.arcadeblocks.config.AudioConfig;
import com.arcadeblocks.nativelib.NativeLibraryLoader;
import com.sun.jna.Pointer;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер аудио с использованием SDL2_mixer через JNA
 * Заменяет JavaFX Media для решения проблем совместимости в Linux
 */
public class SDL2AudioManager {
    
    private final SDL2Mixer mixer;
    private final SDL2 sdl;
    private boolean initialized = false;
    
    // Настройки громкости
    private double masterVolume = 1.0;
    private double musicVolume = 1.0;
    private double sfxVolume = 1.0;
    private double runtimeMusicVolumeMultiplier = 1.0;
    
    // Состояние аудио
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    
    // Текущая музыка
    private Pointer currentMusic = null;
    private String currentMusicFile = null;
    private double currentMusicPlaybackTime = 0.0;
    
    // Кэш загруженных звуков
    private final Map<String, Pointer> soundCache = new ConcurrentHashMap<>();
    private final Map<String, Pointer> musicCache = new ConcurrentHashMap<>();
    
    // Кэш временных файлов
    private final Map<String, String> tempFiles = new HashMap<>();
    
    // Очередь воспроизведения для последовательного воспроизведения
    private final java.util.Queue<Runnable> playbackQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private boolean isPlayingSequence = false;
    
    // Callback для отслеживания окончания звуков
    private Runnable onSoundFinished = null;
    private String currentSequentialSound = null;
    private int currentSequentialChannel = -1;
    
    // Система сохранения предыдущей музыки
    private String previousMusicFile = null;
    private boolean previousMusicLoop = false;
    
    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SDL2AudioManager-Async");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean isCleaningUp = false;
    
    public SDL2AudioManager() {
        // Загружаем нативные библиотеки перед инициализацией
        NativeLibraryLoader.loadLibraries();
        
        this.mixer = SDL2Mixer.getInstance();
        this.sdl = SDL2.getInstance();
        initialize();
        
        // Регистрируем shutdown hook для гарантированной очистки временных файлов
        registerShutdownHook();
    }
    
    /**
     * Инициализация SDL2_mixer
     */
    private void initialize() {
        try {
            // Инициализация SDL2_mixer
        // System.out.println("🎮 Инициализация аудио системы для Arcade Blocks");
            int initResult = mixer.Mix_Init(0);
            if (initResult < 0) {
                System.err.println("Ошибка инициализации SDL2_mixer: " + sdl.SDL_GetError());
                return;
            }
            
            // Открытие аудио устройства с высоким качеством (320 kbps эквивалент)
            int openResult = mixer.Mix_OpenAudio(
                AudioConfig.HIGH_QUALITY_FREQUENCY,  // 48kHz частота дискретизации для высокого качества
                SDL2Mixer.AUDIO_S16LSB,  // 16-bit signed little-endian для лучшего качества
                AudioConfig.HIGH_QUALITY_CHANNELS,  // Стерео
                AudioConfig.HIGH_QUALITY_BUFFER_SIZE  // Увеличенный размер буфера для стабильности
            );
            
            if (openResult < 0) {
                System.err.println("Ошибка открытия аудио устройства: " + sdl.SDL_GetError());
                return;
            }
            
            // Выделение каналов для звуковых эффектов (увеличиваем для высокого качества)
            mixer.Mix_AllocateChannels(AudioConfig.HIGH_QUALITY_MAX_CHANNELS);  // Больше каналов для лучшего качества звука
            
            // Получение информации об аудио устройстве
            com.sun.jna.ptr.IntByReference freq = new com.sun.jna.ptr.IntByReference();
            com.sun.jna.ptr.IntByReference format = new com.sun.jna.ptr.IntByReference();
            com.sun.jna.ptr.IntByReference channels = new com.sun.jna.ptr.IntByReference();
            
            int queryResult = mixer.Mix_QuerySpec(freq, format, channels);
            if (queryResult == 1) {
        // System.out.println("🎵 SDL2_mixer инициализирован:");
        // System.out.println("   Частота: " + freq.getValue() + " Hz");
        // System.out.println("   Формат: " + format.getValue());
        // System.out.println("   Каналы: " + channels.getValue());
        // System.out.println("   Звуковой поток 'Arcade Blocks' создан в системе");
            }
            
            initialized = true;
        // System.out.println("✅ SDL2_mixer готов к работе");
            
        } catch (Exception e) {
            System.err.println("Критическая ошибка инициализации SDL2_mixer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Регистрирует shutdown hook для гарантированной очистки временных файлов
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Удаляем все временные аудио файлы
            for (String tempPath : tempFiles.values()) {
                try {
                    File tempFile = new File(tempPath);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки удаления при shutdown
                }
            }
        }, "SDL2AudioCleanupHook"));
    }
    
    /**
     * Воспроизведение музыки
     */
    public void playMusic(String musicFile, boolean loop) {
        playMusic(musicFile, loop, 0.0);
    }

    public void playMusic(String musicFile, boolean loop, double startTime) {
        if (!initialized || !musicEnabled || masterVolume == 0 || musicVolume == 0) {
            return;
        }
        
        try {
            String normalizedMusicFile = musicFile;
            if (normalizedMusicFile.startsWith("music/")) {
                normalizedMusicFile = normalizedMusicFile.substring(6);
            }
            
            if (currentMusicFile != null && currentMusicFile.equals(musicFile) && isMusicPlaying()) {
                return;
            }
            
            if (currentMusicFile != null && isMusicPlaying() && !currentMusicFile.equals(musicFile)) {
                saveCurrentMusic();
            }
            
            cancelPendingMusic();
            
            stopMusic();
            
            String filePath = getResourceFilePath(normalizedMusicFile, "music");
            if (filePath == null) {
                System.err.println("❌ Файл музыки не найден: " + musicFile);
                return;
            }
            
            Pointer music = musicCache.get(filePath);
            if (music == null) {
                music = mixer.Mix_LoadMUS(filePath);
                if (music != null) {
                    musicCache.put(filePath, music);
                }
            }
            
            if (music == null) {
                System.err.println("❌ Ошибка загрузки музыки: " + filePath + " - " + sdl.SDL_GetError());
                return;
            }
            
            int loops = loop ? -1 : 0;
            int result = mixer.Mix_PlayMusic(music, loops);
            
            if (result == 0) {
                currentMusic = music;
                currentMusicFile = musicFile;

                if (startTime > 0) {
                    mixer.Mix_SetMusicPosition(startTime);
                }
                
                updateMusicVolume();
            } else {
                System.err.println("❌ Ошибка воспроизведения музыки: " + sdl.SDL_GetError());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка воспроизведения музыки: " + musicFile + " - " + e.getMessage());
        }
    }
    
    /**
     * Воспроизведение звукового эффекта
     */
    public void playSFX(String sfxFile) {
        playSFX(sfxFile, null);
    }
    
    /**
     * Воспроизведение звукового эффекта с callback при завершении
     */
    public void playSFX(String sfxFile, Runnable onFinished) {
        if (!initialized || !soundEnabled || masterVolume == 0 || sfxVolume == 0) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        
        try {
            // Определяем папку для поиска звука
            String folder = "sounds";
            if (sfxFile.contains("/")) {
                // Если путь уже содержит папку, используем его как есть
                folder = sfxFile.substring(0, sfxFile.lastIndexOf("/"));
                sfxFile = sfxFile.substring(sfxFile.lastIndexOf("/") + 1);
            }
            
            // Получить путь к файлу
            String filePath = getResourceFilePath(sfxFile, folder);
            if (filePath == null) {
                // Пробуем поискать в папке sfx
                filePath = getResourceFilePath(sfxFile, "sounds/sfx");
                if (filePath == null) {
                    System.err.println("❌ Файл звукового эффекта не найден: " + sfxFile);
                    if (onFinished != null) {
                        onFinished.run();
                    }
                    return;
                }
            }
            
            // Загрузить звук (используем кэш)
            Pointer sound = soundCache.get(filePath);
            if (sound == null) {
                sound = mixer.Mix_LoadWAV(filePath);
                if (sound != null) {
                    soundCache.put(filePath, sound);
                    // System.out.println("✅ Звук загружен в кэш: " + sfxFile);
                }
            }
            
            if (sound == null) {
                System.err.println("❌ Ошибка загрузки звука: " + filePath + " - " + sdl.SDL_GetError());
                if (onFinished != null) {
                    onFinished.run();
                }
                return;
            }
            
            // Освобождаем завершенные каналы перед воспроизведением
            freeFinishedChannels();
            
            // Воспроизведение на свободном канале
            int channel = mixer.Mix_PlayChannel(-1, sound, 0);
            
            if (channel >= 0) {
                // Установка громкости канала
                int volume = (int)(masterVolume * sfxVolume * 128);
                mixer.Mix_Volume(channel, volume);
                // System.out.println("🔊 Воспроизведение звука: " + sfxFile + " на канале " + channel + " (громкость: " + volume + ")");
                
                // Если есть callback, устанавливаем отслеживание завершения
                if (onFinished != null) {
                    onSoundFinished = onFinished;
                    currentSequentialSound = sfxFile;
                    currentSequentialChannel = channel;
        // System.out.println("🎵 Установлен callback для звука: " + sfxFile);
                }
            } else {
                System.err.println("❌ Ошибка воспроизведения звука: " + sdl.SDL_GetError());
                // Если звук не может воспроизвестись, все равно вызываем callback
                // чтобы логика игры не зависла
                if (onFinished != null) {
                    // Небольшая задержка для имитации воспроизведения звука
                    asyncExecutor.execute(() -> {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        onFinished.run();
                    });
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка воспроизведения звука: " + sfxFile + " - " + e.getMessage());
            // Если произошла ошибка, все равно вызываем callback
            // чтобы логика игры не зависла
            if (onFinished != null) {
                // Небольшая задержка для имитации воспроизведения звука
                asyncExecutor.execute(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    onFinished.run();
                });
            }
        }
    }
    
    /**
     * Воспроизведение звукового эффекта по имени из конфига
     */
    public void playSFXByName(String effectName) {
        String sfxFile = AudioConfig.getSFX(effectName);
        if (sfxFile != null) {
            playSFX(sfxFile);
        }
    }
    
    /**
     * Последовательное воспроизведение: сначала звук, потом музыка
     */
    public void playSoundThenMusic(String soundFile, String musicFile, boolean loop) {
        if (!initialized) {
            return;
        }
        
        // System.out.println("🎵 Запуск последовательного воспроизведения:");
        // System.out.println("   1. Звук: " + soundFile);
        // System.out.println("   2. Музыка: " + musicFile + (loop ? " (зациклено)" : ""));
        
        // Останавливаем текущую музыку
        stopMusic();
        
        // Воспроизводим звук с callback
        playSFX(soundFile, () -> {
            // Этот callback вызывается когда звук полностью закончился
            // Выполняем в отдельном потоке, чтобы не зависеть от UI состояния
            asyncExecutor.execute(() -> {
                try {
                    Thread.sleep(100);
                    javafx.application.Platform.runLater(() -> {
                        playMusic(musicFile, loop);
                        isPlayingSequence = false;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        
        isPlayingSequence = true;
    }
    
    /**
     * Обработка очереди воспроизведения
     */
    private void processPlaybackQueue() {
        Runnable next = playbackQueue.poll();
        if (next != null) {
            next.run();
        }
    }
    
    /**
     * Остановка музыки
     */
    public void stopMusic() {
        if (initialized && currentMusic != null) {
            mixer.Mix_HaltMusic();
            currentMusic = null;
            currentMusicFile = null;
        }
    }
    
    /**
     * Пауза музыки
     */
    public void pauseMusic() {
        if (initialized && isMusicPlaying()) {
            currentMusicPlaybackTime = getCurrentMusicPosition();
            mixer.Mix_PauseMusic();
        }
    }
    
    /**
     * Возобновление музыки
     */
    public void resumeMusic() {
        if (initialized && isMusicPaused()) {
            mixer.Mix_ResumeMusic();
        }
    }

    public double getCurrentMusicPosition() {
        if (initialized && currentMusic != null && isMusicPlaying()) {
            return mixer.Mix_GetMusicPosition(currentMusic);
        }
        return 0.0;
    }

    public String getCurrentMusicFile() {
        return currentMusicFile;
    }
    
    /**
     * Остановка всех звуков
     */
    public void stopAll() {
        if (initialized) {
            stopMusic();
            mixer.Mix_HaltChannel(-1); // Остановить все каналы
        }
    }
    
    /**
     * Остановка всех звуковых эффектов (SFX) без остановки музыки
     */
    public void stopAllSFX() {
        if (initialized) {
            mixer.Mix_HaltChannel(-1); // Остановить все каналы звуковых эффектов
        // System.out.println("🔇 Остановлены все звуковые эффекты");
        }
    }
    
    /**
     * Получение пути к ресурсу и создание временного файла
     */
    private String getResourceFilePath(String resourcePath, String folder) {
        try {
            // Проверяем кэш временных файлов
            String tempPath = tempFiles.get(resourcePath);
            if (tempPath != null && new File(tempPath).exists()) {
                return tempPath;
            }
            
            // Нормализуем путь к ресурсу
            String normalizedPath = resourcePath;
            if (normalizedPath.startsWith(folder + "/")) {
                normalizedPath = normalizedPath.substring(folder.length() + 1);
            }
            
            // Создаем полный путь к ресурсу
            String fullResourcePath = "assets/" + folder + "/" + normalizedPath;
            // System.out.println("🔍 Попытка загрузить ресурс: " + fullResourcePath);
            
            // Загружаем ресурс
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fullResourcePath);
            if (inputStream == null) {
                System.err.println("❌ Ресурс не найден: " + fullResourcePath);
                return null;
            }
            
            // Создаем временный файл
            File tempFile = File.createTempFile("arcade_blocks_", "_" + new File(normalizedPath).getName());
            tempFile.deleteOnExit();
            
            // Копируем данные
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            
            String absolutePath = tempFile.getAbsolutePath();
            tempFiles.put(resourcePath, absolutePath);
            
            // System.out.println("📁 Загружен аудио файл: " + normalizedPath + " -> " + absolutePath);
            
            return absolutePath;
            
        } catch (Exception e) {
            System.err.println("Ошибка создания временного файла для " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Обновление громкости музыки
     */
    private void updateMusicVolume() {
        if (initialized) {
            int volume = (int)(masterVolume * musicVolume * runtimeMusicVolumeMultiplier * 128);
            mixer.Mix_VolumeMusic(volume);
        }
    }
    
    /**
     * Освобождает завершенные каналы для предотвращения "No free channels available"
     */
    private void freeFinishedChannels() {
        if (!initialized) {
            return;
        }
        
        try {
            // Проверяем все каналы и останавливаем завершенные
            for (int i = 0; i < AudioConfig.HIGH_QUALITY_MAX_CHANNELS; i++) {
                // Mix_Playing возвращает 0 если канал не воспроизводится
                if (mixer.Mix_Playing(i) == 0) {
                    // Останавливаем канал для полной очистки
                    mixer.Mix_HaltChannel(i);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки очистки каналов
        }
    }
    
    /**
     * Обновление громкости звуковых эффектов
     */
    private void updateSfxVolume() {
        if (initialized) {
            int volume = (int)(masterVolume * sfxVolume * 128);
            // Устанавливаем громкость для всех каналов (обновлено для высокого качества)
            for (int i = 0; i < AudioConfig.HIGH_QUALITY_MAX_CHANNELS; i++) {
                mixer.Mix_Volume(i, volume);
            }
        }
    }
    
    // Геттеры и сеттеры
    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, volume));
        updateMusicVolume();
        updateSfxVolume();
    }
    
    public void setMusicVolume(double volume) {
        this.musicVolume = Math.max(0.0, Math.min(1.0, volume));
        updateMusicVolume();
    }

    public void setRuntimeMusicVolumeMultiplier(double multiplier) {
        this.runtimeMusicVolumeMultiplier = Math.max(0.0, Math.min(1.0, multiplier));
        updateMusicVolume();
    }

    public double getRuntimeMusicVolumeMultiplier() {
        return runtimeMusicVolumeMultiplier;
    }

    public void resetRuntimeMusicVolumeMultiplier() {
        setRuntimeMusicVolumeMultiplier(1.0);
    }
    
    public void setSfxVolume(double volume) {
        this.sfxVolume = Math.max(0.0, Math.min(1.0, volume));
        updateSfxVolume();
    }
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            stopAll();
        }
    }
    
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
    }
    
    public double getMasterVolume() { return masterVolume; }
    public double getMusicVolume() { return musicVolume; }
    public double getSfxVolume() { return sfxVolume; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public boolean isMusicEnabled() { return musicEnabled; }
    
    public boolean isMusicPlaying() {
        return initialized && mixer.Mix_PlayingMusic() == 1;
    }
    
    public boolean isMusicPaused() {
        return initialized && mixer.Mix_PausedMusic() == 1;
    }
    
    /**
     * Проверить, играет ли определенная музыка
     */
    public boolean isMusicPlaying(String musicFile) {
        return isMusicPlaying() && currentMusicFile != null && currentMusicFile.equals(musicFile);
    }
    
    /**
     * Сохранить текущую музыку для последующего восстановления
     */
    public void saveCurrentMusic() {
        if (currentMusicFile != null && isMusicPlaying()) {
            previousMusicFile = currentMusicFile;
            previousMusicLoop = true; // Предполагаем, что музыка главного меню зациклена
        // System.out.println("💾 Сохранена текущая музыка: " + currentMusicFile);
        }
    }
    
    /**
     * Восстановить предыдущую музыку
     * @return true если музыка была восстановлена, false если не было сохраненной музыки
     */
    public boolean restorePreviousMusic() {
        if (previousMusicFile != null) {
        // System.out.println("🔄 Восстановление предыдущей музыки: " + previousMusicFile);
            playMusic(previousMusicFile, previousMusicLoop);
            previousMusicFile = null;
            previousMusicLoop = false;
            return true;
        }
        return false;
    }
    
    /**
     * Очистить сохраненную музыку
     */
    public void clearSavedMusic() {
        previousMusicFile = null;
        previousMusicLoop = false;
        // System.out.println("🗑️ Очищена сохраненная музыка");
    }
    
    /**
     * Предзагрузка часто используемых звуков
     */
    public void preloadCommonSounds() {
        if (!initialized) {
            return;
        }
        
        // System.out.println("🔄 Предзагрузка часто используемых звуков...");
        
        String[] commonSounds = {
            "paddle_hit.wav",
            "brick_break.wav", 
            "wall_bounce.wav",
            "menu_select.wav",
            "menu_hover.wav",
            "menu_back.wav"
        };
        
        for (String soundFile : commonSounds) {
            try {
                String filePath = getResourceFilePath(soundFile, "sounds/sfx");
                if (filePath != null) {
                    Pointer sound = mixer.Mix_LoadWAV(filePath);
                    if (sound != null) {
                        soundCache.put(filePath, sound);
        // System.out.println("✅ Предзагружен: " + soundFile);
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Ошибка предзагрузки " + soundFile + ": " + e.getMessage());
            }
        }
        
        // System.out.println("✅ Предзагрузка завершена");
    }
    
    /**
     * Отмена отложенной музыки (callback)
     */
    public void cancelPendingMusic() {
        if (onSoundFinished != null && currentSequentialSound != null) {
            // Не отменяем callback для звука завершения уровня
            if (currentSequentialSound.contains("level_complete") || currentSequentialSound.contains("sounds/sfx/level_complete")) {
        // System.out.println("🎵 Звук завершения уровня имеет приоритет, не отменяем callback");
                return;
            }
        // System.out.println("🚫 Отменен callback для звука: " + currentSequentialSound);
            onSoundFinished = null;
            currentSequentialSound = null;
            currentSequentialChannel = -1;
            isPlayingSequence = false;
        }
    }
    
    /**
     * Принудительная проверка и запуск отложенной музыки
     * Вызывается при переходе между разделами
     */
    public void checkPendingMusic() {
        if (onSoundFinished != null && currentSequentialSound != null) {
            // Проверяем, играет ли еще звук на каналах
            boolean stillPlaying = false;
            if (currentSequentialChannel >= 0) {
                stillPlaying = mixer.Mix_Playing(currentSequentialChannel) == 1;
            } else {
                for (int i = 0; i < AudioConfig.HIGH_QUALITY_MAX_CHANNELS; i++) {
                    if (mixer.Mix_Playing(i) == 1) {
                        stillPlaying = true;
                        break;
                    }
                }
            }
            
            // Если канал больше не играет, звук завершился
            if (!stillPlaying) {
        // System.out.println("🔊 Принудительная проверка: звук завершен: " + currentSequentialSound);
                Runnable callback = onSoundFinished;
                onSoundFinished = null;
                currentSequentialSound = null;
                currentSequentialChannel = -1;
                
                // Выполняем callback немедленно
                asyncExecutor.execute(() -> {
                try {
                    Thread.sleep(50);
                    callback.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            }
        }
    }
    
    /**
     * Обновление (вызывается каждый кадр)
     */
    public void update(double tpf) {
        // Проверяем завершение звуков для callback
        if (onSoundFinished != null && currentSequentialSound != null) {
            // Проверяем, играет ли еще звук на каналах
            boolean stillPlaying = false;
            if (currentSequentialChannel >= 0) {
                stillPlaying = mixer.Mix_Playing(currentSequentialChannel) == 1;
            } else {
                for (int i = 0; i < AudioConfig.HIGH_QUALITY_MAX_CHANNELS; i++) {
                    if (mixer.Mix_Playing(i) == 1) {
                        stillPlaying = true;
                        break;
                    }
                }
            }
            
            // Если канал больше не играет, звук закончился
            if (!stillPlaying) {
        // System.out.println("🔊 Звук завершен: " + currentSequentialSound);
                Runnable callback = onSoundFinished;
                onSoundFinished = null;
                currentSequentialSound = null;
                currentSequentialChannel = -1;
                
                // Выполняем callback в отдельном потоке для надежности
                asyncExecutor.execute(() -> {
                try {
                    Thread.sleep(50);
                    callback.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            }
        }
    }
    
    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        if (initialized) {
            isCleaningUp = true;
            stopAll();
            asyncExecutor.shutdownNow();
            
            // Освобождение загруженных звуков
            for (Pointer sound : soundCache.values()) {
                if (sound != null) {
                    mixer.Mix_FreeChunk(sound);
                }
            }
            soundCache.clear();
            
            // Освобождение загруженной музыки
            for (Pointer music : musicCache.values()) {
                if (music != null) {
                    mixer.Mix_FreeMusic(music);
                }
            }
            musicCache.clear();
            
            // Закрытие аудио
            mixer.Mix_CloseAudio();
            mixer.Mix_Quit();
            
            // Удаление временных файлов
            for (String tempPath : tempFiles.values()) {
                try {
                    new File(tempPath).delete();
                } catch (Exception e) {
                    // Игнорируем ошибки удаления временных файлов
                }
            }
            tempFiles.clear();
            
            initialized = false;
        // System.out.println("🎵 SDL2_mixer очищен");
        }
    }
    
    /**
     * Проверка, инициализирован ли аудио менеджер
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Получить ExecutorService для правильного завершения
     */
    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }
}
