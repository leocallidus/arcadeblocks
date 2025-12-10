package com.arcadeblocks.utils;

import com.arcadeblocks.config.BonusConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.GameLine;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.persistence.GameSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * ╨Ь╨╡╨╜╨╡╨┤╨╢╨╡╤А ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╣ ╨┤╨╗╤П ╨╜╨░╤Б╤В╤А╨╛╨╡╨║ ╨╕ ╨┐╤А╨╛╨│╤А╨╡╤Б╤Б╨░ ╨╕╨│╤А╤Л (╤Б ╨╕╤Б╨┐╨╛╨╗╤М╨╖╨╛╨▓╨░╨╜╨╕╨╡╨╝ SQLite)
 */
public class SaveManager {
    
    private static final String FALLBACK_SAVE_NAME_PREFIX = "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435 "; // "Сохранение "
    private static final String LEGACY_CORRUPTED_SAVE_NAME_PREFIX = "╨б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╡ ";
    private static final String FALLBACK_UNKNOWN_TIME = "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e"; // "Неизвестно"
    private static final String LEGACY_CORRUPTED_UNKNOWN_TIME = "╨Э╨╡╨╕╨╖╨▓╨╡╤Б╤В╨╜╨╛";
    
    private static final String LEGACY_FPS_LIMIT_KEY = "fps_limit_enabled";
    private static final String VSYNC_ENABLED_KEY = "vsync_enabled";
    
    private DatabaseManager databaseManager;
    private PlayerStats playerStats;
    private Map<String, Object> gameData;
    private final ExecutorService dbExecutor;
    private final Object writeChainLock = new Object();
    private CompletableFuture<Void> pendingWrites = CompletableFuture.completedFuture(null);
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper();

    private static final String CURRENT_PLAYER_NAME_KEY = "current_player_name";

    private GameLine currentGameLine = GameLine.ARCADE_BLOCKS;

    public void setCurrentGameLine(GameLine line) {
        if (line != null) {
            currentGameLine = line;
        }
    }

    public GameLine getCurrentGameLine() {
        return currentGameLine != null ? currentGameLine : GameLine.ARCADE_BLOCKS;
    }

    private GameLine resolveCurrentGameLine() {
        return currentGameLine != null ? currentGameLine : GameLine.ARCADE_BLOCKS;
    }
    
    private static String getDefaultPlayerName() {
        try {
            return LocalizationManager.getInstance().get("player.default");
        } catch (Exception e) {
            // ╨Х╤Б╨╗╨╕ ╨╗╨╛╨║╨░╨╗╨╕╨╖╨░╤Ж╨╕╤П ╨╡╤Й╤С ╨╜╨╡ ╨╖╨░╨│╤А╤Г╨╢╨╡╨╜╨░, ╨▓╨╛╨╖╨▓╤А╨░╤Й╨░╨╡╨╝ ╤Е╨░╤А╨┤╨║╨╛╨┤
            return "Player";
        }
    }

    public SaveManager() {
        this.databaseManager = new DatabaseManager();
        this.gameData = new HashMap<>();
        
        this.dbExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "SaveManager-DB");
                thread.setDaemon(true);
                return thread;
            }
        });
        
        // ╨Ш╨╜╨╕╤Ж╨╕╨░╨╗╨╕╨╖╨╕╤А╤Г╨╡╨╝ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е
        databaseManager.initialize();
        
        // ╨Ш╨╜╨╕╤Ж╨╕╨░╨╗╨╕╨╖╨╕╤А╤Г╨╡╨╝ ╤Б╤В╨░╤В╨╕╤Б╤В╨╕╨║╤Г ╨╕╨│╤А╨╛╨║╨╛╨▓
        this.playerStats = new PlayerStats(databaseManager);
        
        loadDefaultSettings();
    }
    
    /**
     * ╨Ч╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О
     */
    private void loadDefaultSettings() {
        // ╨г╤Б╤В╨░╨╜╨░╨▓╨╗╨╕╨▓╨░╨╡╨╝ ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О ╤В╨╛╨╗╤М╨║╨╛ ╨╡╤Б╨╗╨╕ ╨╛╨╜╨╕ ╨╜╨╡ ╤Б╤Г╤Й╨╡╤Б╤В╨▓╤Г╤О╤В ╨▓ ╨С╨Ф
        setDefaultIfNotExists("master_volume", String.valueOf(GameConfig.DEFAULT_VOLUME));
        setDefaultIfNotExists("music_volume", String.valueOf(GameConfig.DEFAULT_VOLUME));
        setDefaultIfNotExists("sfx_volume", String.valueOf(GameConfig.DEFAULT_VOLUME));
        setDefaultIfNotExists("sound_enabled", String.valueOf(GameConfig.DEFAULT_SOUND_ENABLED));
        
        // ╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╤Г╨┐╤А╨░╨▓╨╗╨╡╨╜╨╕╤П
        for (Map.Entry<String, String> entry : GameConfig.DEFAULT_CONTROLS.entrySet()) {
            setDefaultIfNotExists("control_" + entry.getKey().toLowerCase(), entry.getValue());
        }
        
        // ╨Ш╨│╤А╨╛╨▓╤Л╨╡ ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕
        setDefaultIfNotExists("player_name", getDefaultPlayerName());
        setDefaultIfNotExists("paddle_speed", String.valueOf(GameConfig.PADDLE_SPEED));
        setDefaultIfNotExists("fullscreen", "true");
        setDefaultIfNotExists("windowed_mode", "false");
        migrateLegacyFpsSetting();
        setDefaultIfNotExists(VSYNC_ENABLED_KEY, "true");
        // ╨а╨░╨╖╤А╨╡╤И╨╡╨╜╨╕╨╡ ╨▒╨╛╨╗╨╡╨╡ ╨╜╨╡ ╤Б╨╛╤Е╤А╨░╨╜╤П╨╡╨╝
        setDefaultIfNotExists("difficulty", "NORMAL");
        // ╨п╨╖╤Л╨║ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О
        setDefaultIfNotExists("language", "en");
        
        // Показываем фон уровня по умолчанию (опция отключения фона выключена)
        setDefaultIfNotExists("level_background_enabled", "true");
        // Скорость турбо-режима (множитель) по умолчанию
        // По умолчанию 2.5x (2.5 * 100 = 250 в слайдере)
        setDefaultIfNotExists("turbo_mode_speed", "2.5");
        
        // ╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О (╨▒╨╛╨╜╤Г╤Б╤Л)
        loadDefaultDebugSettings();
        
        // ╨Т╨║╨╗╤О╤З╨░╨╡╨╝ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╡ ╨╛╤З╨║╨╛╨▓ (╤Г╤Б╤В╨░╨╜╨░╨▓╨╗╨╕╨▓╨░╨╡╨╝ ╤Д╨╗╨░╨│ ╨▓ false)
        setDefaultIfNotExists("debug_bonus_score_enabled", "false");
        
        // ╨Ш╨╜╨╕╤Ж╨╕╨░╨╗╨╕╨╖╨░╤Ж╨╕╤П ╨╕╨│╤А╨╛╨▓╤Л╤Е ╨┤╨░╨╜╨╜╤Л╤Е
        setDefaultGameDataIfNotExists("current_level", 1);
        setDefaultGameDataIfNotExists("lives", GameConfig.INITIAL_LIVES);
        setDefaultGameDataIfNotExists("score", 0);
        setDefaultGameDataIfNotExists("high_score", 0);
        setDefaultGameDataIfNotExists("total_levels_completed", 0);
        setDefaultGameDataIfNotExists("game_difficulty", getDifficulty().name());
    }

    private void migrateLegacyFpsSetting() {
        String vsyncValue = databaseManager.getSetting(VSYNC_ENABLED_KEY);
        if (vsyncValue != null) {
            return;
        }
        String legacyValue = databaseManager.getSetting(LEGACY_FPS_LIMIT_KEY);
        if (legacyValue != null) {
            databaseManager.setSetting(VSYNC_ENABLED_KEY, legacyValue);
        }
    }
    
    private void submitDbTask(Runnable task) {
        synchronized (writeChainLock) {
            pendingWrites = pendingWrites.handle((unused, ex) -> null)
                .thenRunAsync(() -> {
                    try {
                        task.run();
                    } catch (Exception e) {
                        System.err.println("╨Ю╤И╨╕╨▒╨║╨░ ╤Д╨╛╨╜╨╛╨▓╨╛╨╣ ╨╖╨░╨┐╨╕╤Б╨╕ SaveManager: " + e.getMessage());
                    }
                }, dbExecutor);
        }
    }
    
    private void setSettingAsync(String key, String value) {
        submitDbTask(() -> databaseManager.setSetting(key, value));
    }
    
    private void setGameDataAsync(String key, String value) {
        submitDbTask(() -> databaseManager.setGameData(key, value));
    }
    
    private void removeGameDataAsync(String key) {
        submitDbTask(() -> databaseManager.removeGameData(key));
    }
    
    public void awaitPendingWrites() {
        CompletableFuture<Void> pending;
        synchronized (writeChainLock) {
            pending = pendingWrites;
        }
        try {
            pending.join();
        } catch (Exception e) {
            System.err.println("╨Ю╤И╨╕╨▒╨║╨░ ╨╛╨╢╨╕╨┤╨░╨╜╨╕╤П ╤Д╨╛╨╜╨╛╨▓╤Л╤Е ╨╖╨░╨┐╨╕╤Б╨╡╨╣ SaveManager: " + e.getMessage());
        }
    }

    public void runAfterPendingWrites(Runnable task) {
        CompletableFuture<Void> pending;
        synchronized (writeChainLock) {
            pending = pendingWrites;
        }
        pending.thenRun(() -> javafx.application.Platform.runLater(task));
    }

    /**
     * Forces a cleanup after all pending writes are done.
     */
    public void forceMemoryCleanup() {
        awaitPendingWrites();
        System.gc();
    }

    public void saveGameSnapshot(GameSnapshot snapshot) {
        if (snapshot == null) {
            clearGameSnapshot();
            return;
        }

        try {
            String json = SNAPSHOT_MAPPER.writeValueAsString(snapshot);
            setGameDataAsync("resume_snapshot", json);
            setGameDataAsync("resume_snapshot_pending", "true");
        } catch (Exception e) {
            System.err.println("╨Ю╤И╨╕╨▒╨║╨░ ╤Б╨╡╤А╨╕╨░╨╗╨╕╨╖╨░╤Ж╨╕╨╕ snapshot: " + e.getMessage());
        }
    }

    public GameSnapshot loadGameSnapshot() {
        awaitPendingWrites();
        String json = databaseManager.getGameData("resume_snapshot");
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            return SNAPSHOT_MAPPER.readValue(json, GameSnapshot.class);
        } catch (Exception e) {
            System.err.println("╨Ю╤И╨╕╨▒╨║╨░ ╨┤╨╡╤Б╨╡╤А╨╕╨░╨╗╨╕╨╖╨░╤Ж╨╕╨╕ snapshot: " + e.getMessage());
            return null;
        }
    }

    public boolean isResumeSnapshotPending() {
        String value = databaseManager.getGameData("resume_snapshot_pending");
        return value != null && Boolean.parseBoolean(value);
    }

    public void markResumeSnapshotConsumed() {
        setGameDataAsync("resume_snapshot_pending", "false");
    }

    public void clearGameSnapshot() {
        removeGameDataAsync("resume_snapshot");
        setGameDataAsync("resume_snapshot_pending", "false");
    }
    
    /**
     * ╨г╤Б╤В╨░╨╜╨╛╨▓╨╕╤В╤М ╨╖╨╜╨░╤З╨╡╨╜╨╕╨╡ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О ╨╡╤Б╨╗╨╕ ╨╛╨╜╨╛ ╨╜╨╡ ╤Б╤Г╤Й╨╡╤Б╤В╨▓╤Г╨╡╤В
     */
    private void setDefaultIfNotExists(String key, String value) {
        if (databaseManager.getSetting(key) == null) {
            databaseManager.setSetting(key, value);
        }
    }
    
    /**
     * ╨г╤Б╤В╨░╨╜╨╛╨▓╨╕╤В╤М ╨╕╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О ╨╡╤Б╨╗╨╕ ╨╛╨╜╨╕ ╨╜╨╡ ╤Б╤Г╤Й╨╡╤Б╤В╨▓╤Г╤О╤В
     */
    private void setDefaultGameDataIfNotExists(String key, Object value) {
        if (databaseManager.getGameData(key) == null) {
            databaseManager.setGameData(key, String.valueOf(value));
        }
        gameData.put(key, value);
    }
    
    /**
     * ╨Ч╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨╕╨╖ ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void loadSettings() {
        // ╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╤Г╨╢╨╡ ╨╖╨░╨│╤А╤Г╨╢╨╡╨╜╤Л ╨┐╤А╨╕ ╨╕╨╜╨╕╤Ж╨╕╨░╨╗╨╕╨╖╨░╤Ж╨╕╨╕ ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е
        // System.out.println("╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨╖╨░╨│╤А╤Г╨╢╨╡╨╜╤Л ╨╕╨╖ ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е SQLite");
    }
    
    /**
     * ╨б╨╛╤Е╤А╨░╨╜╨╕╤В╤М ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨▓ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void saveSettings() {
        // ╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╤Б╨╛╤Е╤А╨░╨╜╤П╤О╤В╤Б╤П ╨░╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╕ ╨┐╤А╨╕ ╨║╨░╨╢╨┤╨╛╨╝ ╨╕╨╖╨╝╨╡╨╜╨╡╨╜╨╕╨╕
        // System.out.println("╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╤Л ╨▓ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е SQLite");
    }

    // ╨Ь╨╡╤В╨╛╨┤╤Л ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╤П ╤А╨░╨╖╤А╨╡╤И╨╡╨╜╨╕╤П ╤Г╨┤╨░╨╗╨╡╨╜╤Л
    
    /**
     * ╨Ч╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨╕╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╨╕╨╖ ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void loadGameData() {
        // ╨Ш╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╨╖╨░╨│╤А╤Г╨╢╨░╤О╤В╤Б╤П ╨░╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╕ ╨┐╤А╨╕ ╨╛╨▒╤А╨░╤Й╨╡╨╜╨╕╨╕ ╨║ ╨╝╨╡╤В╨╛╨┤╨░╨╝ get*()
        // System.out.println("╨Ш╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╨╖╨░╨│╤А╤Г╨╢╨╡╨╜╤Л ╨╕╨╖ ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е SQLite");
    }
    
    /**
     * ╨б╨╛╤Е╤А╨░╨╜╨╕╤В╤М ╨╕╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╨▓ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void saveGameData() {
        // ╨Ш╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╤Б╨╛╤Е╤А╨░╨╜╤П╤О╤В╤Б╤П ╨░╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╕ ╨┐╤А╨╕ ╨▓╤Л╨╖╨╛╨▓╨╡ ╨╝╨╡╤В╨╛╨┤╨╛╨▓ set*()
        // System.out.println("╨Ш╨│╤А╨╛╨▓╤Л╨╡ ╨┤╨░╨╜╨╜╤Л╨╡ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╤Л ╨▓ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е SQLite");
    }
    
    // ╨Ь╨╡╤В╨╛╨┤╤Л ╨┤╨╗╤П ╤А╨░╨▒╨╛╤В╤Л ╤Б ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨░╨╝╨╕ ╨░╤Г╨┤╨╕╨╛
    public double getMasterVolume() {
        String value = databaseManager.getSetting("master_volume");
        return value != null ? Double.parseDouble(value) : GameConfig.DEFAULT_VOLUME;
    }
    
    public void setMasterVolume(double volume) {
        setSettingAsync("master_volume", String.valueOf(volume));
    }
    
    public double getMusicVolume() {
        String value = databaseManager.getSetting("music_volume");
        return value != null ? Double.parseDouble(value) : GameConfig.DEFAULT_VOLUME;
    }
    
    public void setMusicVolume(double volume) {
        setSettingAsync("music_volume", String.valueOf(volume));
    }
    
    public double getSfxVolume() {
        String value = databaseManager.getSetting("sfx_volume");
        return value != null ? Double.parseDouble(value) : GameConfig.DEFAULT_VOLUME;
    }
    
    public void setSfxVolume(double volume) {
        setSettingAsync("sfx_volume", String.valueOf(volume));
    }
    
    public boolean isSoundEnabled() {
        String value = databaseManager.getSetting("sound_enabled");
        return value != null ? Boolean.parseBoolean(value) : GameConfig.DEFAULT_SOUND_ENABLED;
    }
    
    public void setSoundEnabled(boolean enabled) {
        setSettingAsync("sound_enabled", String.valueOf(enabled));
    }
    
    public double getTurboModeSpeed() {
        String value = databaseManager.getSetting("turbo_mode_speed");
        return value != null ? Double.parseDouble(value) : 1.5;
    }

    public void setTurboModeSpeed(double speed) {
        setSettingAsync("turbo_mode_speed", String.valueOf(speed));
    }
    
    
    // Метод удален, так как настройка убрана из интерфейса
    public boolean isCallBallSoundEnabled() {
        return true; // По умолчанию включено, если код все еще где-то вызывает
    }
    
    public void setCallBallSoundEnabled(boolean enabled) {
        // Ничего не делаем
    }

    
    // ╨Ь╨╡╤В╨╛╨┤╤Л ╨┤╨╗╤П ╤А╨░╨▒╨╛╤В╤Л ╤Б ╤П╨╖╤Л╨║╨╛╨╝
    public String getLanguage() {
        String value = databaseManager.getSetting("language");
        return value != null ? value : "en"; // ╨Я╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О ╨░╨╜╨│╨╗╨╕╨╣╤Б╨║╨╕╨╣
    }
    
    public void setLanguage(String languageCode) {
        setSettingAsync("language", languageCode);
    }
    
    
    // ╨Ь╨╡╤В╨╛╨┤╤Л ╨┤╨╗╤П ╤А╨░╨▒╨╛╤В╤Л ╤Б ╤Г╨┐╤А╨░╨▓╨╗╨╡╨╜╨╕╨╡╨╝
    public String getControlKey(String action) {
        String value = databaseManager.getSetting("control_" + action.toLowerCase());
        return value != null ? value : GameConfig.DEFAULT_CONTROLS.get(action);
    }
    
    public void setControlKey(String action, String key) {
        setSettingAsync("control_" + action.toLowerCase(), key);
    }
    
    public boolean isLevelBackgroundEnabled() {
        String value = databaseManager.getSetting("level_background_enabled");
        return value != null ? Boolean.parseBoolean(value) : true;
    }

    public void setLevelBackgroundEnabled(boolean enabled) {
        setSettingAsync("level_background_enabled", String.valueOf(enabled));
    }
    
    // ╨Ь╨╡╤В╨╛╨┤╤Л ╨┤╨╗╤П ╤А╨░╨▒╨╛╤В╤Л ╤Б ╨╕╨│╤А╨╛╨▓╤Л╨╝╨╕ ╨┤╨░╨╜╨╜╤Л╨╝╨╕
    public int getCurrentLevel() {
        String key = getGameLinePrefix(resolveCurrentGameLine()) + "current_level";
        String value = databaseManager.getGameData(key);
        if (value == null && resolveCurrentGameLine() == GameLine.ARCADE_BLOCKS) {
            value = databaseManager.getGameData("current_level"); // legacy key
        }
        return value != null ? Integer.parseInt(value) : (Integer) gameData.getOrDefault(key, 1);
    }
    
    public void setCurrentLevel(int level) {
        String key = getGameLinePrefix(resolveCurrentGameLine()) + "current_level";
        gameData.put(key, level);
        setGameDataAsync(key, String.valueOf(level));
        if (resolveCurrentGameLine() == GameLine.ARCADE_BLOCKS) {
            // maintain legacy compatibility
            setGameDataAsync("current_level", String.valueOf(level));
        }
    }

    public static final class LineProgress {
        public final int maxLevel;
        public final boolean completed;

        public LineProgress(int maxLevel, boolean completed) {
            this.maxLevel = maxLevel;
            this.completed = completed;
        }
    }

    /**
     * Получить прогресс по указанной игровой линии (макс уровень и флаг завершения)
     */
    public LineProgress getLineProgress(GameLine gameLine) {
        GameLine line = gameLine != null ? gameLine : GameLine.ARCADE_BLOCKS;
        int maxLevel = 0;
        boolean completed = false;
        for (int slot = 1; slot <= 4; slot++) {
            SaveInfo info = getSaveInfo(line, slot);
            if (info == null) {
                continue;
            }
            if (info.level > maxLevel) {
                maxLevel = info.level;
            }
            if (info.gameCompleted || info.level >= line.getEndLevel()) {
                completed = true;
            }
        }
        return new LineProgress(maxLevel, completed);
    }

    /**
     * Прогресс основной кампании (Arcade Blocks) независимо от выбранной линии.
     */
    public LineProgress getMainCampaignProgress() {
        GameLine line = GameLine.ARCADE_BLOCKS;
        LineProgress progress = getLineProgress(line);

        // Учитываем легаси-ключ current_level (без слотов), чтобы не терять прогресс
        int legacyLevel = 0;
        try {
            String legacy = databaseManager.getGameData("current_level");
            if (legacy != null && !legacy.isBlank()) {
                legacyLevel = Integer.parseInt(legacy.trim());
            }
        } catch (Exception ignored) {}

        int maxLevel = Math.max(progress.maxLevel, legacyLevel);
        boolean completed = progress.completed || legacyLevel >= line.getEndLevel();

        return new LineProgress(maxLevel, completed);
    }

    /**
     * Возвращает true, если в любом слоте (любой линии) установлен флаг завершения игры.
     */
    public boolean isAnyGameCompleted() {
        for (GameLine line : GameLine.values()) {
            for (int slot = 1; slot <= 4; slot++) {
                if (isGameCompletedInSlot(line, slot)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Определить состояние прогресса для главного меню:
     * COMPLETED — если любая линия/слот завершены или достигнут финальный уровень линии;
     * AFTER_LEVEL_100 — если основная линия имеет уровень 101+ и не завершена;
     * NORMAL — по умолчанию.
     */
    public com.arcadeblocks.config.AudioConfig.GameProgressState getMenuProgressState() {
        boolean completed = false;
        boolean afterLevel100 = false;
        boolean globalCompletedFlag = "true".equalsIgnoreCase(databaseManager.getGameData("main_campaign_completed"));

        for (GameLine line : GameLine.values()) {
            for (int slot = 1; slot <= 4; slot++) {
                SaveInfo info = getSaveInfo(line, slot);
                if (info == null) {
                    continue;
                }
                int level = info.level;
                int endLevel = line.getEndLevel();
                if (isGameCompletedInSlot(line, slot) || level >= endLevel) {
                    completed = true;
                }
                if (line == GameLine.ARCADE_BLOCKS && level >= 101) {
                    afterLevel100 = true;
                }
            }
        }

        completed = completed || globalCompletedFlag;

        System.out.println("[SaveManager] getMenuProgressState: completedAny=" + completed
            + ", afterLevel100=" + afterLevel100
            + ", legacyLevel=" + getCurrentLevel()
            + ", globalCompletedFlag=" + globalCompletedFlag);

        if (completed) {
            return com.arcadeblocks.config.AudioConfig.GameProgressState.COMPLETED;
        }
        if (afterLevel100) {
            return com.arcadeblocks.config.AudioConfig.GameProgressState.AFTER_LEVEL_100;
        }
        return com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
    }
    
    public int getLives() {
        Object cached = gameData.get("lives");
        if (cached instanceof Number) {
            return ((Number) cached).intValue();
        }
        String value = databaseManager.getGameData("lives");
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                gameData.put("lives", parsed);
                return parsed;
            } catch (NumberFormatException ignored) {}
        }
        int fallback = GameConfig.INITIAL_LIVES;
        gameData.put("lives", fallback);
        return fallback;
    }
    
    public void setLives(int lives) {
        gameData.put("lives", lives);
        setGameDataAsync("lives", String.valueOf(lives));
    }
    
    public int getScore() {
        Object cached = gameData.get("score");
        if (cached instanceof Number) {
            return ((Number) cached).intValue();
        }

        String value = databaseManager.getGameData("score");
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                gameData.put("score", parsed);
                return parsed;
            } catch (NumberFormatException ignored) {}
        }
        return (Integer) gameData.getOrDefault("score", 0);
    }
    
    public void setScore(int score) {
        gameData.put("score", score);
        setGameDataAsync("score", String.valueOf(score));
    }
    
    public int getHighScore() {
        String value = databaseManager.getGameData("high_score");
        return value != null ? Integer.parseInt(value) : (Integer) gameData.getOrDefault("high_score", 0);
    }
    
    public void setHighScore(int highScore) {
        gameData.put("high_score", highScore);
        setGameDataAsync("high_score", String.valueOf(highScore));
    }
    
    public String getPlayerName() {
        Object cached = gameData.get(CURRENT_PLAYER_NAME_KEY);
        if (cached instanceof String && !((String) cached).isBlank()) {
            return (String) cached;
        }

        GameLine currentGameLine = resolveCurrentGameLine();
        int activeSlot = getActiveSaveSlot(currentGameLine);
        String slotName = getPlayerNameForSlot(currentGameLine, activeSlot);
        if (slotName == null || slotName.isBlank()) {
            slotName = getStoredGlobalPlayerName();
        }
        gameData.put(CURRENT_PLAYER_NAME_KEY, slotName);
        return slotName;
    }

    public void setPlayerName(String name) {
        String sanitized = sanitizePlayerName(name);
        setSettingAsync("player_name", sanitized);
        gameData.put(CURRENT_PLAYER_NAME_KEY, sanitized);
    }

    public String getPlayerNameForSlot(int slotNumber) {
        return getPlayerNameForSlot(resolveCurrentGameLine(), slotNumber);
    }

    private String getPlayerNameForSlot(GameLine gameLine, int slotNumber) {
        if (slotNumber < 1) {
            return getStoredGlobalPlayerName();
        }
        if (slotNumber == getActiveSaveSlot(gameLine)) {
            Object cached = gameData.get(CURRENT_PLAYER_NAME_KEY);
            if (cached instanceof String && !((String) cached).isBlank()) {
                return (String) cached;
            }
        }
        String value = databaseManager.getGameData(getSlotKey(gameLine, slotNumber) + "_player_name");
        if (value == null || value.isBlank()) {
            return getStoredGlobalPlayerName();
        }
        return value;
    }

    public void setPlayerNameForSlot(int slotNumber, String name) {
        GameLine currentGameLine = resolveCurrentGameLine();
        if (slotNumber < 1) {
            return;
        }
        String sanitized = sanitizePlayerName(name);
        submitDbTask(() -> databaseManager.setGameData(getSlotKey(currentGameLine, slotNumber) + "_player_name", sanitized));
        if (slotNumber == getActiveSaveSlot(currentGameLine)) {
            gameData.put(CURRENT_PLAYER_NAME_KEY, sanitized);
        }
    }

    private String getStoredGlobalPlayerName() {
        String value = databaseManager.getSetting("player_name");
        if (value == null || value.isBlank()) {
            return getDefaultPlayerName();
        }
        return value;
    }

    private String sanitizePlayerName(String name) {
        if (name == null) {
            return getDefaultPlayerName();
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? getDefaultPlayerName() : trimmed;
    }

    public boolean isVSyncEnabled() {
        String value = databaseManager.getSetting(VSYNC_ENABLED_KEY);
        if (value == null) {
            value = databaseManager.getSetting(LEGACY_FPS_LIMIT_KEY);
        }
        return value != null ? Boolean.parseBoolean(value) : true;
    }

    public void setVSyncEnabled(boolean enabled) {
        setSettingAsync(VSYNC_ENABLED_KEY, String.valueOf(enabled));
    }
    
    public double getPaddleSpeed() {
        String value = databaseManager.getSetting("paddle_speed");
        return value != null ? Double.parseDouble(value) : GameConfig.PADDLE_SPEED;
    }
    
    public void setPaddleSpeed(double speed) {
        setSettingAsync("paddle_speed", String.valueOf(speed));
    }
    
    
    public boolean isFullscreen() {
        String value = databaseManager.getSetting("fullscreen");
        return value != null ? Boolean.parseBoolean(value) : true;
    }
    
    public void setFullscreen(boolean fullscreen) {
        setSettingAsync("fullscreen", String.valueOf(fullscreen));
    }
    
    public boolean isWindowedMode() {
        String value = databaseManager.getSetting("windowed_mode");
        return value != null ? Boolean.parseBoolean(value) : false;
    }
    
    public void setWindowedMode(boolean windowed) {
        setSettingAsync("windowed_mode", String.valueOf(windowed));
    }
    
    // ╨Ь╨╡╤В╨╛╨┤╤Л ╨┤╨╗╤П ╤А╨░╨▒╨╛╤В╤Л ╤Б ╤А╨░╨╖╤А╨╡╤И╨╡╨╜╨╕╨╡╨╝
    public com.arcadeblocks.config.Resolution getResolution() {
        String value = databaseManager.getSetting("resolution");
        if (value == null || value.isEmpty()) {
            return com.arcadeblocks.config.GameConfig.DEFAULT_RESOLUTION;
        }
        return com.arcadeblocks.config.Resolution.fromString(value);
    }
    
    public void setResolution(com.arcadeblocks.config.Resolution resolution) {
        if (resolution != null) {
            setSettingAsync("resolution", resolution.toString());
        }
    }
    
    public boolean isLevelCompleted(int levelNumber) {
        String value = databaseManager.getGameData("level_" + levelNumber + "_completed");
        return value != null ? Boolean.parseBoolean(value) : false;
    }
    
    public void setLevelCompleted(int levelNumber, boolean completed) {
        if (completed) {
            gameData.put("level_" + levelNumber + "_completed", true);
            setGameDataAsync("level_" + levelNumber + "_completed", "true");
        } else {
            gameData.remove("level_" + levelNumber + "_completed");
            removeGameDataAsync("level_" + levelNumber + "_completed");
        }
    }

    public boolean isLevelStatsRecorded(int levelNumber) {
        Object cached = gameData.get("level_" + levelNumber + "_stats_recorded");
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        String value = databaseManager.getGameData("level_" + levelNumber + "_stats_recorded");
        if (value != null) {
            boolean parsed = Boolean.parseBoolean(value);
            gameData.put("level_" + levelNumber + "_stats_recorded", parsed);
            return parsed;
        }
        return false;
    }

    public void markLevelStatsRecorded(int levelNumber) {
        gameData.put("level_" + levelNumber + "_stats_recorded", true);
        setGameDataAsync("level_" + levelNumber + "_stats_recorded", "true");
    }

    public void setLevelStars(int levelNumber, int stars) {
        int clamped = Math.max(1, Math.min(5, stars));
        gameData.put("level_" + levelNumber + "_stars", clamped);
        setGameDataAsync("level_" + levelNumber + "_stars", String.valueOf(clamped));
    }

    public int getLevelStars(int levelNumber) {
        Object cached = gameData.get("level_" + levelNumber + "_stars");
        if (cached instanceof Number) {
            return ((Number) cached).intValue();
        }
        String value = databaseManager.getGameData("level_" + levelNumber + "_stars");
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                gameData.put("level_" + levelNumber + "_stars", parsed);
                return parsed;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public double getAverageStarsForLine(com.arcadeblocks.config.GameLine gameLine) {
        if (gameLine == null) {
            return -1.0;
        }
        int start = gameLine.getStartLevel();
        int end = gameLine.getEndLevel();
        int total = 0;
        int count = 0;
        for (int level = start; level <= end; level++) {
            int stars = getLevelStars(level);
            if (stars > 0) {
                total += stars;
                count++;
            }
        }
        if (count == 0) {
            return -1.0;
        }
        return (double) total / (double) count;
    }

    private String buildLineKey(com.arcadeblocks.config.GameLine line, String suffix) {
        String id = line != null ? line.getId() : "unknown";
        return "line_" + id + "_" + suffix;
    }

    public void addLineTimeSeconds(com.arcadeblocks.config.GameLine line, double seconds) {
        if (line == null || seconds <= 0) return;
        String key = buildLineKey(line, "time_seconds");
        double current = getLineTimeSeconds(line);
        double next = current < 0 ? seconds : current + seconds;
        gameData.put(key, next);
        setGameDataAsync(key, String.valueOf(next));
    }

    public double getLineTimeSeconds(com.arcadeblocks.config.GameLine line) {
        if (line == null) return -1;
        String key = buildLineKey(line, "time_seconds");
        Object cached = gameData.get(key);
        if (cached instanceof Number) {
            return ((Number) cached).doubleValue();
        }
        String value = databaseManager.getGameData(key);
        if (value != null) {
            try {
                double parsed = Double.parseDouble(value);
                gameData.put(key, parsed);
                return parsed;
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    public void addLineBonuses(com.arcadeblocks.config.GameLine line, int positive, int negative) {
        if (line == null) return;
        if (positive > 0) {
            addLineBonusesInternal(line, "positive_bonuses", positive);
        }
        if (negative > 0) {
            addLineBonusesInternal(line, "negative_bonuses", negative);
        }
    }

    private void addLineBonusesInternal(com.arcadeblocks.config.GameLine line, String suffix, int delta) {
        String key = buildLineKey(line, suffix);
        int current = getLineBonuses(line, suffix);
        int next = Math.max(0, current) + delta;
        gameData.put(key, next);
        setGameDataAsync(key, String.valueOf(next));
    }

    private int getLineBonuses(com.arcadeblocks.config.GameLine line, String suffix) {
        String key = buildLineKey(line, suffix);
        Object cached = gameData.get(key);
        if (cached instanceof Number) {
            return ((Number) cached).intValue();
        }
        String value = databaseManager.getGameData(key);
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                gameData.put(key, parsed);
                return parsed;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public int getLinePositiveBonuses(com.arcadeblocks.config.GameLine line) {
        if (line == null) return 0;
        return getLineBonuses(line, "positive_bonuses");
    }

    public int getLineNegativeBonuses(com.arcadeblocks.config.GameLine line) {
        if (line == null) return 0;
        return getLineBonuses(line, "negative_bonuses");
    }
    
    public int getTotalLevelsCompleted() {
        String value = databaseManager.getGameData("total_levels_completed");
        return value != null ? Integer.parseInt(value) : (Integer) gameData.getOrDefault("total_levels_completed", 0);
    }
    
    public void setTotalLevelsCompleted(int count) {
        gameData.put("total_levels_completed", count);
        setGameDataAsync("total_levels_completed", String.valueOf(count));
    }
    
    // ╨Ь╨╡╤В╨╛╨┤╤Л ╨┤╨╗╤П ╤А╨░╨▒╨╛╤В╤Л ╤Б ╤Г╤А╨╛╨▓╨╜╨╡╨╝ ╤Б╨╗╨╛╨╢╨╜╨╛╤Б╤В╨╕
    public com.arcadeblocks.config.DifficultyLevel getDifficulty() {
        String value = databaseManager.getSetting("difficulty");
        if (value != null) {
            try {
                return com.arcadeblocks.config.DifficultyLevel.valueOf(value);
            } catch (IllegalArgumentException e) {
                // ╨Х╤Б╨╗╨╕ ╨╖╨╜╨░╤З╨╡╨╜╨╕╨╡ ╨╜╨╡╨║╨╛╤А╤А╨╡╨║╤В╨╜╨╛╨╡, ╨▓╨╛╨╖╨▓╤А╨░╤Й╨░╨╡╨╝ ╨╜╨╛╤А╨╝╨░╨╗╤М╨╜╤Г╤О ╤Б╨╗╨╛╨╢╨╜╨╛╤Б╤В╤М
                return com.arcadeblocks.config.DifficultyLevel.NORMAL;
            }
        }
        return com.arcadeblocks.config.DifficultyLevel.NORMAL;
    }
    
    public void setDifficulty(com.arcadeblocks.config.DifficultyLevel difficulty) {
        setSettingAsync("difficulty", difficulty.name());
    }

    public com.arcadeblocks.config.DifficultyLevel getGameDifficulty() {
        String value = databaseManager.getGameData("game_difficulty");
        if (value == null) {
            Object cached = gameData.get("game_difficulty");
            if (cached != null) {
                value = String.valueOf(cached);
            }
        }

        if (value != null) {
            try {
                com.arcadeblocks.config.DifficultyLevel difficulty = com.arcadeblocks.config.DifficultyLevel.valueOf(value);
                gameData.put("game_difficulty", difficulty.name());
                return difficulty;
            } catch (IllegalArgumentException e) {
                // ignore and fallback
            }
        }

        return getDifficulty();
    }

    public void setGameDifficulty(com.arcadeblocks.config.DifficultyLevel difficulty) {
        if (difficulty == null) {
            difficulty = getDifficulty();
        }
        String value = difficulty.name();
        gameData.put("game_difficulty", value);
        setGameDataAsync("game_difficulty", value);
    }
    
    /**
     * ╨б╨▒╤А╨╛╤Б╨╕╤В╤М ╨┐╤А╨╛╨│╤А╨╡╤Б╤Б ╨╕╨│╤А╤Л
     */
    public void resetGameProgress() {
        gameData.clear();
        databaseManager.clearAllGameData();
        clearGameSnapshot();
        
        // ╨г╤Б╤В╨░╨╜╨░╨▓╨╗╨╕╨▓╨░╨╡╨╝ ╨╖╨╜╨░╤З╨╡╨╜╨╕╤П ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О
        setCurrentLevel(1);
        setLives(GameConfig.INITIAL_LIVES);
        setScore(0);
        setTotalLevelsCompleted(0);
        setGameDifficulty(getDifficulty());
    }

    /**
     * ╨б╨▒╤А╨╛╤Б╨╕╤В╤М ╨┐╤А╨╛╨│╤А╨╡╤Б╤Б ╤В╨╡╨║╤Г╤Й╨╡╨╣ ╨╕╨│╤А╤Л, ╤Б╨╛╤Е╤А╨░╨╜╨╕╨▓ ╨┤╨░╨╜╨╜╤Л╨╡ ╨┤╤А╤Г╨│╨╕╤Е ╤Б╨╗╨╛╤В╨╛╨▓.
     * ╨Ш╤Б╨┐╨╛╨╗╤М╨╖╤Г╨╡╤В╤Б╤П ╨┐╤А╨╕ ╤Б╨╛╨╖╨┤╨░╨╜╨╕╨╕ ╨╜╨╛╨▓╨╛╨│╨╛ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╤П, ╤З╤В╨╛╨▒╤Л ╨╜╨╡ ╤Б╤В╨╕╤А╨░╤В╤М ╤Б╤Г╤Й╨╡╤Б╤В╨▓╤Г╤О╤Й╨╕╨╡ ╤Б╨╗╨╛╤В╤Л.
     */
    public void resetProgressPreservingSlots() {
        clearGameSnapshot();

        // ╨Ш╤Б╨┐╨╛╨╗╤М╨╖╤Г╨╡╨╝ ╤Б╨╗╨╛╨╢╨╜╨╛╤Б╤В╤М ╤В╨╡╨║╤Г╤Й╨╡╨│╨╛ ╨░╨║╤В╨╕╨▓╨╜╨╛╨│╨╛ ╤Б╨╗╨╛╤В╨░ (╨╕╨╗╨╕ ╨╛╨▒╤Й╤Г╤О ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╤Г, ╨╡╤Б╨╗╨╕ ╨╛╨╜╨░ ╨╡╤Й╤С ╨╜╨╡ ╨╖╨░╨┤╨░╨╜╨░)
        com.arcadeblocks.config.DifficultyLevel baseDifficulty = getGameDifficulty();
        if (baseDifficulty == null) {
            baseDifficulty = getDifficulty();
            setGameDifficulty(baseDifficulty);
        }

        setCurrentLevel(1);
        setLives(baseDifficulty.getLives());
        setScore(0);
        setTotalLevelsCompleted(0);
    }
    
    /**
     * ╨б╨▒╤А╨╛╤Б╨╕╤В╤М ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨║ ╨╖╨╜╨░╤З╨╡╨╜╨╕╤П╨╝ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О
     */
    public void resetSettings() {
        databaseManager.clearAllSettings();
        loadDefaultSettings();
    }
    
    /**
     * ╨Я╤А╨╛╨▓╨╡╤А╨╕╤В╤М, ╤Б╤Г╤Й╨╡╤Б╤В╨▓╤Г╨╡╤В ╨╗╨╕ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╡
     */
    public boolean hasSaveFile() {
        return databaseManager.isConnected() && getCurrentLevel() > 1;
    }
    
    /**
     * ╨г╨┤╨░╨╗╨╕╤В╤М ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╡ ╨░╨║╤В╨╕╨▓╨╜╨╛╨│╨╛ ╤Б╨╗╨╛╤В╨░.
     */
    public void deleteSaveFile() {
        deleteSaveFile(resolveCurrentGameLine());
    }

    public void deleteSaveFile(GameLine gameLine) {
        deleteSaveFileForSlot(gameLine, getActiveSaveSlot(gameLine));
    }

    /**
     * ╨г╨┤╨░╨╗╨╕╤В╤М ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╡ ╤Г╨║╨░╨╖╨░╨╜╨╜╨╛╨│╨╛ ╤Б╨╗╨╛╤В╨░, ╤Б╨╛╤Е╤А╨░╨╜╨╕╨▓ ╨╛╤Б╤В╨░╨╗╤М╨╜╤Л╨╡.
     *
     * @param slotNumber ╨╜╨╛╨╝╨╡╤А ╤Б╨╗╨╛╤В╨░, ╨║╨╛╤В╨╛╤А╤Л╨╣ ╨╜╤Г╨╢╨╜╨╛ ╨╛╤З╨╕╤Б╤В╨╕╤В╤М
     */
    public void deleteSaveFileForSlot(int slotNumber) {
        deleteSaveFileForSlot(resolveCurrentGameLine(), slotNumber);
    }

    public void deleteSaveFileForSlot(GameLine gameLine, int slotNumber) {
        if (slotNumber < 1) {
            slotNumber = 1;
        }

        setActiveSaveSlot(gameLine, slotNumber);
        clearGameSnapshot();
        clearSaveSlot(gameLine, slotNumber);

        com.arcadeblocks.config.DifficultyLevel difficulty = getGameDifficulty();
        if (difficulty == null) {
            difficulty = getDifficulty();
        }

        int resetLevel = (gameLine != null) ? gameLine.getStartLevel() : 1;
        setCurrentLevel(resetLevel);
        setLives(difficulty.getLives());
        setScore(0);
        setTotalLevelsCompleted(0);
        setGameDifficulty(difficulty);
    }
    
    /**
     * ╨Ч╨░╨║╤А╤Л╤В╤М ╤Б╨╛╨╡╨┤╨╕╨╜╨╡╨╜╨╕╨╡ ╤Б ╨▒╨░╨╖╨╛╨╣ ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void close() {
        awaitPendingWrites();
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╤Б╤В╨░╤В╨╕╤Б╤В╨╕╨║╤Г ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void printDatabaseStats() {
        if (databaseManager != null) {
            databaseManager.printDatabaseStats();
        }
    }
    
    /**
     * ╨Ю╨▒╨╜╨╛╨▓╨╕╤В╤М ╤Б╤В╨░╤В╨╕╤Б╤В╨╕╨║╤Г ╨╕╨│╤А╨╛╨║╨░
     */
    public void updatePlayerStats(String playerName, int level, int score, int gamesPlayed) {
        if (playerStats != null) {
            playerStats.updatePlayerStats(playerName, level, score, gamesPlayed);
        }
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╤Б╤В╨░╤В╨╕╤Б╤В╨╕╨║╤Г ╨╕╨│╤А╨╛╨║╨░
     */
    public PlayerStats.PlayerStatsRecord getPlayerStats(String playerName, int level) {
        return playerStats != null ? playerStats.getPlayerStats(playerName, level) : null;
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╤В╨╛╨┐ ╨╕╨│╤А╨╛╨║╨╛╨▓
     */
    public List<PlayerStats.PlayerStatsRecord> getTopPlayers(int level, int limit) {
        return playerStats != null ? playerStats.getTopPlayers(level, limit) : new ArrayList<>();
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╨▓╤Б╤О ╤Б╤В╨░╤В╨╕╤Б╤В╨╕╨║╤Г ╨╕╨│╤А╨╛╨║╨╛╨▓
     */
    public List<PlayerStats.PlayerStatsRecord> getAllPlayerStats() {
        return playerStats != null ? playerStats.getAllPlayerStats() : new ArrayList<>();
    }
    
    // ========== ╨Ь╨Х╨в╨Ю╨Ф╨л ╨Ф╨Ы╨п ╨а╨Р╨С╨Ю╨в╨л ╨б ╨Ф╨Х╨С╨Р╨У ╨Ь╨Х╨Э╨о ==========
    
    /**
     * ╨Ч╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О
     */
    private void loadDefaultDebugSettings() {
        // ╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨▒╨╛╨╜╤Г╤Б╨╛╨▓ ╨┐╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О
        setDefaultIfNotExists("debug_bonus_score_enabled", "true");
        setDefaultIfNotExists("debug_extra_life_enabled", "true");
        setDefaultIfNotExists("debug_increase_paddle_enabled", "true");
        setDefaultIfNotExists("debug_decrease_paddle_enabled", "true");
        setDefaultIfNotExists("debug_sticky_paddle_enabled", "true");
        setDefaultIfNotExists("debug_fast_balls_enabled", "true");
        setDefaultIfNotExists("debug_slow_balls_enabled", "true");
        setDefaultIfNotExists("debug_energy_balls_enabled", "true");
        setDefaultIfNotExists("debug_explosion_balls_enabled", "true");
        setDefaultIfNotExists("debug_weak_balls_enabled", "true");
        setDefaultIfNotExists("debug_bonus_ball_enabled", "true");
        setDefaultIfNotExists("debug_bonus_wall_enabled", "true");
        setDefaultIfNotExists("debug_bonus_magnet_enabled", "true");
        setDefaultIfNotExists("debug_plasma_weapon_enabled", "true");
        setDefaultIfNotExists("debug_full_hp_enabled", "true");
        setDefaultIfNotExists("debug_level_pass_enabled", "true");
        setDefaultIfNotExists("debug_score_rain_enabled", "true");
        setDefaultIfNotExists("debug_bonus_score200_enabled", "true");
        setDefaultIfNotExists("debug_bonus_score500_enabled", "true");
        setDefaultIfNotExists("debug_add_five_seconds_enabled", "true");
        setDefaultIfNotExists("debug_call_ball_enabled", "true");
        setDefaultIfNotExists("debug_darkness_enabled", "true");
        setDefaultIfNotExists("debug_reset_enabled", "true");
        setDefaultIfNotExists("debug_trickster_enabled", "true");
        setDefaultIfNotExists("debug_bad_luck_enabled", "true");
        setDefaultIfNotExists("debug_chaotic_balls_enabled", "true");
        setDefaultIfNotExists("debug_frozen_paddle_enabled", "true");
        setDefaultIfNotExists("debug_invisible_paddle_enabled", "true");
        setDefaultIfNotExists("debug_penalties_magnet_enabled", "true");
        setDefaultIfNotExists("debug_random_bonus_enabled", "true");
        setDefaultIfNotExists("debug_testing_mode", "false");
        setDefaultIfNotExists("debug_positive_bonuses_only", "false");
        setDefaultIfNotExists("debug_negative_bonuses_only", "false");
        setDefaultIfNotExists("debug_disable_all_bonuses", "false");
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╤Б╨╛╤Б╤В╨╛╤П╨╜╨╕╨╡ ╨▒╨╛╨╜╤Г╤Б╨░ ╨╕╨╖ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О
     */
    public boolean getDebugBonusEnabled(String fieldName) {
        String value = databaseManager.getSetting("debug_" + fieldName.toLowerCase());
        return value != null ? Boolean.parseBoolean(value) : false;
    }
    
    /**
     * ╨г╤Б╤В╨░╨╜╨╛╨▓╨╕╤В╤М ╤Б╨╛╤Б╤В╨╛╤П╨╜╨╕╨╡ ╨▒╨╛╨╜╤Г╤Б╨░ ╨▓ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О
     */
    public void setDebugBonusEnabled(String fieldName, boolean enabled) {
        setSettingAsync("debug_" + fieldName.toLowerCase(), String.valueOf(enabled));
    }
    
    /**
     * ╨Ч╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨▓╤Б╨╡ ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О ╨▓ BonusConfig
     */
    public void loadDebugSettingsToBonusConfig() {
        // ╨Ч╨░╨│╤А╤Г╨╢╨░╨╡╨╝ ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨▒╨╛╨╜╤Г╤Б╨╛╨▓
        BonusConfig.setBonusEnabled("BONUS_SCORE_ENABLED", getDebugBonusEnabled("bonus_score_enabled"));
        BonusConfig.setBonusEnabled("BONUS_SCORE_200_ENABLED", getDebugBonusEnabled("bonus_score200_enabled"));
        BonusConfig.setBonusEnabled("BONUS_SCORE_500_ENABLED", getDebugBonusEnabled("bonus_score500_enabled"));
        BonusConfig.setBonusEnabled("ADD_FIVE_SECONDS_ENABLED", getDebugBonusEnabled("add_five_seconds_enabled"));
        BonusConfig.setBonusEnabled("CALL_BALL_ENABLED", getDebugBonusEnabled("call_ball_enabled"));
        BonusConfig.setBonusEnabled("EXTRA_LIFE_ENABLED", getDebugBonusEnabled("extra_life_enabled"));
        BonusConfig.setBonusEnabled("INCREASE_PADDLE_ENABLED", getDebugBonusEnabled("increase_paddle_enabled"));
        BonusConfig.setBonusEnabled("DECREASE_PADDLE_ENABLED", getDebugBonusEnabled("decrease_paddle_enabled"));
        BonusConfig.setBonusEnabled("STICKY_PADDLE_ENABLED", getDebugBonusEnabled("sticky_paddle_enabled"));
        BonusConfig.setBonusEnabled("FAST_BALLS_ENABLED", getDebugBonusEnabled("fast_balls_enabled"));
        BonusConfig.setBonusEnabled("SLOW_BALLS_ENABLED", getDebugBonusEnabled("slow_balls_enabled"));
        BonusConfig.setBonusEnabled("ENERGY_BALLS_ENABLED", getDebugBonusEnabled("energy_balls_enabled"));
        BonusConfig.setBonusEnabled("EXPLOSION_BALLS_ENABLED", getDebugBonusEnabled("explosion_balls_enabled"));
        BonusConfig.setBonusEnabled("WEAK_BALLS_ENABLED", getDebugBonusEnabled("weak_balls_enabled"));
        BonusConfig.setBonusEnabled("BONUS_BALL_ENABLED", getDebugBonusEnabled("bonus_ball_enabled"));
        BonusConfig.setBonusEnabled("BONUS_WALL_ENABLED", getDebugBonusEnabled("bonus_wall_enabled"));
        BonusConfig.setBonusEnabled("BONUS_MAGNET_ENABLED", getDebugBonusEnabled("bonus_magnet_enabled"));
        BonusConfig.setBonusEnabled("PLASMA_WEAPON_ENABLED", getDebugBonusEnabled("plasma_weapon_enabled"));
        BonusConfig.setBonusEnabled("FULL_HP_ENABLED", getDebugBonusEnabled("full_hp_enabled"));
        BonusConfig.setBonusEnabled("LEVEL_PASS_ENABLED", getDebugBonusEnabled("level_pass_enabled"));
        BonusConfig.setBonusEnabled("SCORE_RAIN_ENABLED", getDebugBonusEnabled("score_rain_enabled"));
        BonusConfig.setBonusEnabled("CHAOTIC_BALLS_ENABLED", getDebugBonusEnabled("chaotic_balls_enabled"));
        BonusConfig.setBonusEnabled("FROZEN_PADDLE_ENABLED", getDebugBonusEnabled("frozen_paddle_enabled"));
        BonusConfig.setBonusEnabled("INVISIBLE_PADDLE_ENABLED", getDebugBonusEnabled("invisible_paddle_enabled"));
        BonusConfig.setBonusEnabled("PENALTIES_MAGNET_ENABLED", getDebugBonusEnabled("penalties_magnet_enabled"));
        BonusConfig.setBonusEnabled("DARKNESS_ENABLED", getDebugBonusEnabled("darkness_enabled"));
        BonusConfig.setBonusEnabled("RESET_ENABLED", getDebugBonusEnabled("reset_enabled"));
        BonusConfig.setBonusEnabled("BAD_LUCK_ENABLED", getDebugBonusEnabled("bad_luck_enabled"));
        BonusConfig.setBonusEnabled("TRICKSTER_ENABLED", getDebugBonusEnabled("trickster_enabled"));
        BonusConfig.setBonusEnabled("RANDOM_BONUS_ENABLED", getDebugBonusEnabled("random_bonus_enabled"));
        BonusConfig.setBonusEnabled("TESTING_MODE", getDebugBonusEnabled("testing_mode"));
        BonusConfig.setBonusEnabled("POSITIVE_BONUSES_ONLY", getDebugBonusEnabled("positive_bonuses_only"));
        BonusConfig.setBonusEnabled("NEGATIVE_BONUSES_ONLY", getDebugBonusEnabled("negative_bonuses_only"));
        BonusConfig.setBonusEnabled("DISABLE_ALL_BONUSES", getDebugBonusEnabled("disable_all_bonuses"));
        
        // System.out.println("╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О ╨╖╨░╨│╤А╤Г╨╢╨╡╨╜╤Л ╨╕╨╖ ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е");
    }
    
    /**
     * ╨б╨╛╤Е╤А╨░╨╜╨╕╤В╤М ╤В╨╡╨║╤Г╤Й╨╕╨╡ ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ BonusConfig ╨▓ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public void saveDebugSettingsFromBonusConfig() {
        // ╨б╨╛╤Е╤А╨░╨╜╤П╨╡╨╝ ╨╜╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨▒╨╛╨╜╤Г╤Б╨╛╨▓
        setDebugBonusEnabled("bonus_score_enabled", BonusConfig.getBonusEnabled("BONUS_SCORE_ENABLED"));
        setDebugBonusEnabled("bonus_score200_enabled", BonusConfig.getBonusEnabled("BONUS_SCORE_200_ENABLED"));
        setDebugBonusEnabled("bonus_score500_enabled", BonusConfig.getBonusEnabled("BONUS_SCORE_500_ENABLED"));
        setDebugBonusEnabled("add_five_seconds_enabled", BonusConfig.getBonusEnabled("ADD_FIVE_SECONDS_ENABLED"));
        setDebugBonusEnabled("call_ball_enabled", BonusConfig.getBonusEnabled("CALL_BALL_ENABLED"));
        setDebugBonusEnabled("extra_life_enabled", BonusConfig.getBonusEnabled("EXTRA_LIFE_ENABLED"));
        setDebugBonusEnabled("increase_paddle_enabled", BonusConfig.getBonusEnabled("INCREASE_PADDLE_ENABLED"));
        setDebugBonusEnabled("decrease_paddle_enabled", BonusConfig.getBonusEnabled("DECREASE_PADDLE_ENABLED"));
        setDebugBonusEnabled("sticky_paddle_enabled", BonusConfig.getBonusEnabled("STICKY_PADDLE_ENABLED"));
        setDebugBonusEnabled("fast_balls_enabled", BonusConfig.getBonusEnabled("FAST_BALLS_ENABLED"));
        setDebugBonusEnabled("slow_balls_enabled", BonusConfig.getBonusEnabled("SLOW_BALLS_ENABLED"));
        setDebugBonusEnabled("energy_balls_enabled", BonusConfig.getBonusEnabled("ENERGY_BALLS_ENABLED"));
        setDebugBonusEnabled("explosion_balls_enabled", BonusConfig.getBonusEnabled("EXPLOSION_BALLS_ENABLED"));
        setDebugBonusEnabled("weak_balls_enabled", BonusConfig.getBonusEnabled("WEAK_BALLS_ENABLED"));
        setDebugBonusEnabled("bonus_ball_enabled", BonusConfig.getBonusEnabled("BONUS_BALL_ENABLED"));
        setDebugBonusEnabled("bonus_wall_enabled", BonusConfig.getBonusEnabled("BONUS_WALL_ENABLED"));
        setDebugBonusEnabled("bonus_magnet_enabled", BonusConfig.getBonusEnabled("BONUS_MAGNET_ENABLED"));
        setDebugBonusEnabled("plasma_weapon_enabled", BonusConfig.getBonusEnabled("PLASMA_WEAPON_ENABLED"));
        setDebugBonusEnabled("full_hp_enabled", BonusConfig.getBonusEnabled("FULL_HP_ENABLED"));
        setDebugBonusEnabled("level_pass_enabled", BonusConfig.getBonusEnabled("LEVEL_PASS_ENABLED"));
        setDebugBonusEnabled("score_rain_enabled", BonusConfig.getBonusEnabled("SCORE_RAIN_ENABLED"));
        setDebugBonusEnabled("chaotic_balls_enabled", BonusConfig.getBonusEnabled("CHAOTIC_BALLS_ENABLED"));
        setDebugBonusEnabled("frozen_paddle_enabled", BonusConfig.getBonusEnabled("FROZEN_PADDLE_ENABLED"));
        setDebugBonusEnabled("invisible_paddle_enabled", BonusConfig.getBonusEnabled("INVISIBLE_PADDLE_ENABLED"));
        setDebugBonusEnabled("penalties_magnet_enabled", BonusConfig.getBonusEnabled("PENALTIES_MAGNET_ENABLED"));
        setDebugBonusEnabled("darkness_enabled", BonusConfig.getBonusEnabled("DARKNESS_ENABLED"));
        setDebugBonusEnabled("reset_enabled", BonusConfig.getBonusEnabled("RESET_ENABLED"));
        setDebugBonusEnabled("bad_luck_enabled", BonusConfig.getBonusEnabled("BAD_LUCK_ENABLED"));
        setDebugBonusEnabled("trickster_enabled", BonusConfig.getBonusEnabled("TRICKSTER_ENABLED"));
        setDebugBonusEnabled("random_bonus_enabled", BonusConfig.getBonusEnabled("RANDOM_BONUS_ENABLED"));
        setDebugBonusEnabled("testing_mode", BonusConfig.getBonusEnabled("TESTING_MODE"));
        setDebugBonusEnabled("positive_bonuses_only", BonusConfig.getBonusEnabled("POSITIVE_BONUSES_ONLY"));
        setDebugBonusEnabled("negative_bonuses_only", BonusConfig.getBonusEnabled("NEGATIVE_BONUSES_ONLY"));
        setDebugBonusEnabled("disable_all_bonuses", BonusConfig.getBonusEnabled("DISABLE_ALL_BONUSES"));
        
        // System.out.println("╨Э╨░╤Б╤В╤А╨╛╨╣╨║╨╕ ╨┤╨╡╨▒╨░╨│ ╨╝╨╡╨╜╤О ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╤Л ╨▓ ╨▒╨░╨╖╤Г ╨┤╨░╨╜╨╜╤Л╤Е");
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╨╝╨╡╨╜╨╡╨┤╨╢╨╡╤А ╨▒╨░╨╖╤Л ╨┤╨░╨╜╨╜╤Л╤Е
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * ╨Р╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╕ ╤Б╨╛╤Е╤А╨░╨╜╨╕╤В╤М ╤В╨╡╨║╤Г╤Й╨╕╨╣ ╨┐╤А╨╛╨│╤А╨╡╤Б╤Б ╨▓ ╤Г╨║╨░╨╖╨░╨╜╨╜╤Л╨╣ ╤Б╨╗╨╛╤В
     */
    public void autoSaveToSlot(int slotNumber) {
        autoSaveToSlot(resolveCurrentGameLine(), slotNumber);
    }

    /**
     * Автосохранение данных в слот с учетом игровой линии
     */
    public void autoSaveToSlot(GameLine gameLine, int slotNumber) {
        GameLine effectiveLine = (gameLine != null) ? gameLine : GameLine.ARCADE_BLOCKS;
        String saveKey = getSlotKey(effectiveLine, slotNumber);
        String currentTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        int level = getCurrentLevel();
        int lives = getLives();
        int score = getScore();
        String difficulty = getGameDifficulty().name();
        String playerName = getPlayerNameForSlot(effectiveLine, slotNumber);
        
        String saveName = computeDefaultSaveName(slotNumber);
        
        final String finalSaveName = saveName;
        submitDbTask(() -> {
            databaseManager.setGameData(saveKey, "true");
            databaseManager.setGameData(saveKey + "_name", finalSaveName);
            databaseManager.setGameData(saveKey + "_time", currentTime);
            databaseManager.setGameData(saveKey + "_level", String.valueOf(level));
            databaseManager.setGameData(saveKey + "_lives", String.valueOf(lives));
            databaseManager.setGameData(saveKey + "_score", String.valueOf(score));
            databaseManager.setGameData(saveKey + "_difficulty", difficulty);
            databaseManager.setGameData(saveKey + "_player_name", playerName);
        });
        
    }

    public void clearSaveSlot(int slotNumber) {
        String saveKey = "save_slot_" + slotNumber;
        submitDbTask(() -> {
            databaseManager.removeGameData(saveKey);
            databaseManager.removeGameData(saveKey + "_name");
            databaseManager.removeGameData(saveKey + "_time");
            databaseManager.removeGameData(saveKey + "_level");
            databaseManager.removeGameData(saveKey + "_lives");
            databaseManager.removeGameData(saveKey + "_score");
            databaseManager.removeGameData(saveKey + "_difficulty");
            databaseManager.removeGameData(saveKey + "_player_name");
            // КРИТИЧНО: Удаляем флаг завершения игры при очистке слота
            databaseManager.removeGameData(saveKey + "_game_completed");
        });
    }

    public void clearAllSaveSlots() {
        for (int i = 1; i <= 4; i++) {
            clearSaveSlot(i);
        }
    }

    /**
     * ╨Ч╨░╨│╤А╤Г╨╖╨╕╤В╤М ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╡ ╨╕╨╖ ╤Г╨║╨░╨╖╨░╨╜╨╜╨╛╨│╨╛ ╤Б╨╗╨╛╤В╨░
     */
    public boolean loadFromSlot(int slotNumber) {
        String saveKey = "save_slot_" + slotNumber;
        String saveExists = databaseManager.getGameData(saveKey);
        
        if (saveExists != null && saveExists.equals("true")) {
            String level = databaseManager.getGameData(saveKey + "_level");
            String lives = databaseManager.getGameData(saveKey + "_lives");
            String score = databaseManager.getGameData(saveKey + "_score");
            String difficultyValue = databaseManager.getGameData(saveKey + "_difficulty");
            String playerName = databaseManager.getGameData(saveKey + "_player_name");
            
            if (difficultyValue != null) {
                try {
                    setGameDifficulty(com.arcadeblocks.config.DifficultyLevel.valueOf(difficultyValue));
                    setGameDataAsync(saveKey + "_difficulty", difficultyValue);
                } catch (IllegalArgumentException e) {
                    com.arcadeblocks.config.DifficultyLevel fallback = getDifficulty();
                    setGameDifficulty(fallback);
                    setGameDataAsync(saveKey + "_difficulty", fallback.name());
                }
            } else {
                com.arcadeblocks.config.DifficultyLevel fallback = getDifficulty();
                setGameDifficulty(fallback);
                setGameDataAsync(saveKey + "_difficulty", fallback.name());
            }
            
            if (level != null) setCurrentLevel(Integer.parseInt(level));
            if (lives != null) setLives(Integer.parseInt(lives));
            if (score != null) setScore(Integer.parseInt(score));
            if (playerName != null && !playerName.isBlank()) {
                gameData.put(CURRENT_PLAYER_NAME_KEY, playerName.trim());
            } else {
                gameData.put(CURRENT_PLAYER_NAME_KEY, getStoredGlobalPlayerName());
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╨╕╨╜╤Д╨╛╤А╨╝╨░╤Ж╨╕╤О ╨╛ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╕ ╨▓ ╤Б╨╗╨╛╤В╨╡
     */
    public SaveInfo getSaveInfo(int slotNumber) {
        // Legacy main-line compatibility: delegate to line-aware API
        return getSaveInfo(GameLine.ARCADE_BLOCKS, slotNumber);
    }

    public SaveInfo getSaveInfo(GameLine gameLine, int slotNumber) {
        GameLine line = gameLine != null ? gameLine : GameLine.ARCADE_BLOCKS;
        String saveKey = getSlotKey(line, slotNumber);
        String saveExists = databaseManager.getGameData(saveKey);
        
        if (saveExists != null && saveExists.equals("true")) {
            String name = databaseManager.getGameData(saveKey + "_name");
            String lastPlayTime = databaseManager.getGameData(saveKey + "_time");
            String level = databaseManager.getGameData(saveKey + "_level");
            String lives = databaseManager.getGameData(saveKey + "_lives");
            String score = databaseManager.getGameData(saveKey + "_score");
            String difficultyValue = databaseManager.getGameData(saveKey + "_difficulty");
            String playerName = databaseManager.getGameData(saveKey + "_player_name");
            boolean gameCompleted = isGameCompletedInSlot(line, slotNumber);
            
            com.arcadeblocks.config.DifficultyLevel difficulty = null;
            if (difficultyValue != null) {
                try {
                    difficulty = com.arcadeblocks.config.DifficultyLevel.valueOf(difficultyValue);
                } catch (IllegalArgumentException e) {
                    difficulty = null;
                }
            }
            if (difficulty == null) {
                difficulty = getDifficulty();
            }

            String defaultName = computeDefaultSaveName(slotNumber);
            String defaultTime = computeUnknownSaveTime();
            String resolvedTime = resolveSaveSlotTime(lastPlayTime, defaultTime);
            
            int levelValue = level != null ? Integer.parseInt(level) : line.getStartLevel();
            int livesValue = lives != null ? Integer.parseInt(lives) : difficulty.getLives();
            int scoreValue = score != null ? Integer.parseInt(score) : 0;
            String resolvedPlayerName = (playerName != null && !playerName.isBlank())
                ? playerName.trim()
                : getStoredGlobalPlayerName();

            return new SaveInfo(
                resolveSaveSlotName(name, slotNumber, defaultName),
                resolvedTime,
                levelValue,
                livesValue,
                scoreValue,
                difficulty,
                resolvedPlayerName,
                gameCompleted
            );
        }

        return null;
    }
    
    /**
     * ╨Р╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╕ ╤Б╨╛╤Е╤А╨░╨╜╨╕╤В╤М ╤В╨╡╨║╤Г╤Й╨╕╨╣ ╨┐╤А╨╛╨│╤А╨╡╤Б╤Б ╨▓ ╨░╨║╤В╨╕╨▓╨╜╤Л╨╣ ╤Б╨╗╨╛╤В
     */
    public void autoSaveToActiveSlot() {
        GameLine currentGameLine = resolveCurrentGameLine();
        autoSaveToActiveSlot(currentGameLine);
    }

    public void autoSaveToActiveSlot(GameLine gameLine) {
        int slot = getActiveSaveSlot(gameLine);
        autoSaveToSlot(gameLine, slot);
    }
    
    private String computeDefaultSaveName(int slotNumber) {
        try {
            String localized = LocalizationManager.getInstance().format("savegame.default_name", slotNumber);
            if (localized != null && !localized.isBlank() && !localized.startsWith("savegame.")) {
                return localized;
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_SAVE_NAME_PREFIX + slotNumber;
    }

    private String computeUnknownSaveTime() {
        try {
            String localized = LocalizationManager.getInstance().get("savegame.unknown_time");
            if (localized != null && !localized.isBlank() && !localized.startsWith("savegame.")) {
                return localized;
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_UNKNOWN_TIME;
    }

    private String resolveSaveSlotName(String storedName, int slotNumber, String defaultName) {
        String effectiveDefault = (defaultName != null && !defaultName.isBlank())
            ? defaultName
            : FALLBACK_SAVE_NAME_PREFIX + slotNumber;

        if (storedName == null || storedName.isBlank()) {
            return effectiveDefault;
        }

        String trimmed = storedName.trim();
        if (trimmed.equalsIgnoreCase(effectiveDefault)
            || trimmed.equalsIgnoreCase(FALLBACK_SAVE_NAME_PREFIX + slotNumber)) {
            return effectiveDefault;
        }

        String legacyRu = LEGACY_CORRUPTED_SAVE_NAME_PREFIX + slotNumber;
        String legacyEn = "Save " + slotNumber;
        if (trimmed.equalsIgnoreCase(legacyRu) || trimmed.equalsIgnoreCase(legacyEn)) {
            return effectiveDefault;
        }

        return trimmed;
    }

    private String resolveSaveSlotTime(String storedTime, String defaultTime) {
        if (storedTime == null || storedTime.isBlank()) {
            return defaultTime;
        }
        String trimmed = storedTime.trim();
        if (trimmed.equalsIgnoreCase(defaultTime)
            || trimmed.equalsIgnoreCase(FALLBACK_UNKNOWN_TIME)
            || trimmed.equalsIgnoreCase(LEGACY_CORRUPTED_UNKNOWN_TIME)) {
            return defaultTime;
        }
        return trimmed;
    }

    /**
     * ╨г╤Б╤В╨░╨╜╨╛╨▓╨╕╤В╤М ╨░╨║╤В╨╕╨▓╨╜╤Л╨╣ ╤Б╨╗╨╛╤В ╨┤╨╗╤П ╨░╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╛╨│╨╛ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╤П
     */
    public void setActiveSaveSlot(int slotNumber) {
        setGameDataAsync("active_save_slot", String.valueOf(slotNumber));
    }
    
    /**
     * ╨Я╨╛╨╗╤Г╤З╨╕╤В╤М ╨░╨║╤В╨╕╨▓╨╜╤Л╨╣ ╤Б╨╗╨╛╤В ╨┤╨╗╤П ╨░╨▓╤В╨╛╨╝╨░╤В╨╕╤З╨╡╤Б╨║╨╛╨│╨╛ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╤П
     */
    public int getActiveSaveSlot() {
        String value = databaseManager.getGameData("active_save_slot");
        return value != null ? Integer.parseInt(value) : 1; // ╨Я╨╛ ╤Г╨╝╨╛╨╗╤З╨░╨╜╨╕╤О ╤Б╨╗╨╛╤В 1
    }
    
    /**
     * ╨Ъ╨╗╨░╤Б╤Б ╨┤╨╗╤П ╤Е╤А╨░╨╜╨╡╨╜╨╕╤П ╨╕╨╜╤Д╨╛╤А╨╝╨░╤Ж╨╕╨╕ ╨╛ ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╕╨╕
     */
    public static class SaveInfo {
        public final String name;
        public final String lastPlayTime;
        public final int level;
        public final int lives;
        public final int score;
        public final com.arcadeblocks.config.DifficultyLevel difficulty;
        public final String playerName;
        public final boolean gameCompleted; // Флаг завершения игры (прохождение уровня 116)

        public SaveInfo(String name, String lastPlayTime, int level, int lives, int score, com.arcadeblocks.config.DifficultyLevel difficulty, String playerName, boolean gameCompleted) {
            this.name = name;
            this.lastPlayTime = lastPlayTime;
            this.level = level;
            this.lives = lives;
            this.score = score;
            this.difficulty = difficulty;
            this.playerName = playerName;
            this.gameCompleted = gameCompleted;
        }
        
        // Конструктор для обратной совместимости
        public SaveInfo(String name, String lastPlayTime, int level, int lives, int score, com.arcadeblocks.config.DifficultyLevel difficulty, String playerName) {
            this(name, lastPlayTime, level, lives, score, difficulty, playerName, false);
        }
    }
    
    /**
     * Установить флаг завершения игры для слота
     * @param slotNumber номер слота
     */
    public void setGameCompletedForSlot(int slotNumber) {
        setGameCompletedForSlot(resolveCurrentGameLine(), slotNumber);
    }

    public void setGameCompletedForSlot(GameLine gameLine, int slotNumber) {
        if (slotNumber < 1) {
            return;
        }
        String key = getSlotKey(gameLine, slotNumber) + "_game_completed";
        setGameDataAsync(key, "true");
        if (gameLine == GameLine.ARCADE_BLOCKS) {
            setGameDataAsync("main_campaign_completed", "true");
        }
    }
    
    /**
     * Проверить, завершена ли игра в слоте
     * @param slotNumber номер слота
     * @return true если игра завершена
     */
    public boolean isGameCompletedInSlot(int slotNumber) {
        return isGameCompletedInSlot(resolveCurrentGameLine(), slotNumber);
    }

    public boolean isGameCompletedInSlot(GameLine gameLine, int slotNumber) {
        if (slotNumber < 1) {
            return false;
        }
        String key = getSlotKey(gameLine, slotNumber) + "_game_completed";
        String value = databaseManager.getGameData(key);
        return "true".equalsIgnoreCase(value);
    }
    
    /**
     * Установить флаг завершения игры для текущего активного слота
     */
    public void setGameCompletedForActiveSlot() {
        GameLine currentGameLine = resolveCurrentGameLine();
        setGameCompletedForSlot(currentGameLine, getActiveSaveSlot(currentGameLine));
    }

    // ========== Методы для поддержки разных игровых линий ==========

    /**
     * Получить префикс ключа для игровой линии
     */
    private String getGameLinePrefix(GameLine gameLine) {
        if (gameLine == null || gameLine == GameLine.ARCADE_BLOCKS) {
            return ""; // Основная кампания без префикса для обратной совместимости
        }
        return gameLine.getId() + "_";
    }

    /**
     * Получить ключ слота для игровой линии
     */
    private String getSlotKey(GameLine gameLine, int slotNumber) {
        return getGameLinePrefix(gameLine) + "save_slot_" + slotNumber;
    }

    /**
     * Сохранить в слот для конкретной игровой линии
     */
    public void saveToSlot(GameLine gameLine, int slotNumber, int level, int lives, int score, String playerName) {
        if (gameLine == null || gameLine == GameLine.ARCADE_BLOCKS) {
            autoSaveToSlot(GameLine.ARCADE_BLOCKS, slotNumber);
            return;
        }

        String saveKey = getSlotKey(gameLine, slotNumber);
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        submitDbTask(() -> {
            databaseManager.setGameData(saveKey, "true");
            databaseManager.setGameData(saveKey + "_name", computeDefaultSaveName(slotNumber));
            databaseManager.setGameData(saveKey + "_time", timestamp);
            databaseManager.setGameData(saveKey + "_level", String.valueOf(level));
            databaseManager.setGameData(saveKey + "_lives", String.valueOf(lives));
            databaseManager.setGameData(saveKey + "_score", String.valueOf(score));
            databaseManager.setGameData(saveKey + "_difficulty", getDifficulty().name());
            if (playerName != null && !playerName.isBlank()) {
                databaseManager.setGameData(saveKey + "_player_name", playerName.trim());
            }
        });
    }

    /**
     * Загрузить из слота для конкретной игровой линии
     */
    public boolean loadFromSlot(GameLine gameLine, int slotNumber) {
        if (gameLine == null || gameLine == GameLine.ARCADE_BLOCKS) {
            return loadFromSlot(slotNumber);
        }

        String saveKey = getSlotKey(gameLine, slotNumber);
        String saveExists = databaseManager.getGameData(saveKey);

        if (saveExists != null && saveExists.equals("true")) {
            String level = databaseManager.getGameData(saveKey + "_level");
            String lives = databaseManager.getGameData(saveKey + "_lives");
            String score = databaseManager.getGameData(saveKey + "_score");
            String difficultyValue = databaseManager.getGameData(saveKey + "_difficulty");
            String playerName = databaseManager.getGameData(saveKey + "_player_name");

            if (difficultyValue != null) {
                try {
                    setGameDifficulty(com.arcadeblocks.config.DifficultyLevel.valueOf(difficultyValue));
                } catch (IllegalArgumentException ignored) {}
            }

            if (level != null) setCurrentLevel(Integer.parseInt(level));
            if (lives != null) setLives(Integer.parseInt(lives));
            if (score != null) setScore(Integer.parseInt(score));
            if (playerName != null && !playerName.isBlank()) {
                gameData.put(CURRENT_PLAYER_NAME_KEY, playerName.trim());
            }

            return true;
        }

        return false;
    }

    /**
     * Очистить слот для конкретной игровой линии
     */
    public void clearSaveSlot(GameLine gameLine, int slotNumber) {
        if (gameLine == null || gameLine == GameLine.ARCADE_BLOCKS) {
            clearSaveSlot(slotNumber);
            return;
        }

        String saveKey = getSlotKey(gameLine, slotNumber);
        submitDbTask(() -> {
            databaseManager.removeGameData(saveKey);
            databaseManager.removeGameData(saveKey + "_name");
            databaseManager.removeGameData(saveKey + "_time");
            databaseManager.removeGameData(saveKey + "_level");
            databaseManager.removeGameData(saveKey + "_lives");
            databaseManager.removeGameData(saveKey + "_score");
            databaseManager.removeGameData(saveKey + "_difficulty");
            databaseManager.removeGameData(saveKey + "_player_name");
            databaseManager.removeGameData(saveKey + "_game_completed");
        });
    }

    /**
     * Установить активный слот для игровой линии
     */
    public void setActiveSaveSlot(GameLine gameLine, int slotNumber) {
        String key = getGameLinePrefix(gameLine) + "active_save_slot";
        setGameDataAsync(key, String.valueOf(slotNumber));
    }

    /**
     * Получить активный слот для игровой линии
     */
    public int getActiveSaveSlot(GameLine gameLine) {
        if (gameLine == null || gameLine == GameLine.ARCADE_BLOCKS) {
            return getActiveSaveSlot();
        }
        String key = getGameLinePrefix(gameLine) + "active_save_slot";
        String value = databaseManager.getGameData(key);
        return value != null ? Integer.parseInt(value) : 1;
    }
}
