package com.arcadeblocks.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Трекер изменений настроек для отслеживания несохраненных данных
 */
public class SettingsChangeTracker {
    
    private Map<String, Object> originalSettings;
    private Map<String, Object> currentSettings;
    private boolean hasUnsavedChanges;
    
    public SettingsChangeTracker() {
        this.originalSettings = new HashMap<>();
        this.currentSettings = new HashMap<>();
        this.hasUnsavedChanges = false;
    }
    
    /**
     * Инициализация с текущими настройками
     */
    public void initialize(SaveManager saveManager) {
        // Сохраняем текущие настройки как исходные
        originalSettings.clear();
        currentSettings.clear();
        
        // Аудио настройки
        originalSettings.put("master_volume", saveManager.getMasterVolume());
        originalSettings.put("music_volume", saveManager.getMusicVolume());
        originalSettings.put("sfx_volume", saveManager.getSfxVolume());
        originalSettings.put("sound_enabled", saveManager.isSoundEnabled());
        
        // Настройки управления
        originalSettings.put("move_left", saveManager.getControlKey("MOVE_LEFT"));
        originalSettings.put("move_right", saveManager.getControlKey("MOVE_RIGHT"));
        originalSettings.put("launch_left", saveManager.getControlKey("LAUNCH_LEFT"));
        originalSettings.put("launch_right", saveManager.getControlKey("LAUNCH_RIGHT"));
        originalSettings.put("turbo_paddle", saveManager.getControlKey("TURBO_PADDLE"));
        originalSettings.put("call_ball", saveManager.getControlKey("CALL_BALL"));
        
        // Игровые настройки
        originalSettings.put("player_name", saveManager.getPlayerName());
        originalSettings.put("paddle_speed", saveManager.getPaddleSpeed());
        originalSettings.put("fullscreen", saveManager.isFullscreen());
        originalSettings.put("vsync_enabled", saveManager.isVSyncEnabled());
        originalSettings.put("call_ball_sound_enabled", saveManager.isCallBallSoundEnabled());
        
        // Копируем в текущие настройки
        currentSettings.putAll(originalSettings);
        hasUnsavedChanges = false;
    }
    
    /**
     * Обновить текущие настройки
     */
    public void updateSetting(String key, Object value) {
        currentSettings.put(key, value);
        // КРИТИЧНО: Оптимизация - проверяем только измененную настройку, а не все
        checkForChangesOptimized(key, value);
    }
    
    /**
     * Оптимизированная проверка изменений - проверяет только одну настройку
     */
    private void checkForChangesOptimized(String key, Object value) {
        Object originalValue = originalSettings.get(key);
        
        // Если хотя бы одна настройка изменилась - устанавливаем флаг
        if (!Objects.equals(value, originalValue)) {
            hasUnsavedChanges = true;
        } else {
            // Если текущая настройка вернулась к исходному значению,
            // нужно проверить все остальные настройки
            checkForChanges();
        }
    }
    
    /**
     * Проверить наличие изменений (полная проверка всех настроек)
     */
    private void checkForChanges() {
        // Сравниваем размеры и содержимое карт
        if (originalSettings.size() != currentSettings.size()) {
            hasUnsavedChanges = true;
            return;
        }
        
        // Проверяем каждую пару ключ-значение
        for (Map.Entry<String, Object> entry : currentSettings.entrySet()) {
            String key = entry.getKey();
            Object currentValue = entry.getValue();
            Object originalValue = originalSettings.get(key);
            
            if (!Objects.equals(currentValue, originalValue)) {
                hasUnsavedChanges = true;
                return;
            }
        }
        
        hasUnsavedChanges = false;
    }
    
    /**
     * Есть ли несохраненные изменения
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    /**
     * Сбросить изменения (применить текущие настройки как исходные)
     */
    public void resetChanges() {
        originalSettings.clear();
        originalSettings.putAll(currentSettings);
        hasUnsavedChanges = false;
    }
    
    /**
     * Отменить изменения (вернуть к исходным настройкам)
     */
    public void revertChanges(SaveManager saveManager) {
        // Восстанавливаем исходные настройки
        if (originalSettings.containsKey("master_volume")) {
            saveManager.setMasterVolume((Double) originalSettings.get("master_volume"));
        }
        if (originalSettings.containsKey("music_volume")) {
            saveManager.setMusicVolume((Double) originalSettings.get("music_volume"));
        }
        if (originalSettings.containsKey("sfx_volume")) {
            saveManager.setSfxVolume((Double) originalSettings.get("sfx_volume"));
        }
        if (originalSettings.containsKey("sound_enabled")) {
            saveManager.setSoundEnabled((Boolean) originalSettings.get("sound_enabled"));
        }
        if (originalSettings.containsKey("move_left")) {
            saveManager.setControlKey("MOVE_LEFT", (String) originalSettings.get("move_left"));
        }
        if (originalSettings.containsKey("move_right")) {
            saveManager.setControlKey("MOVE_RIGHT", (String) originalSettings.get("move_right"));
        }
        if (originalSettings.containsKey("launch_left")) {
            saveManager.setControlKey("LAUNCH_LEFT", (String) originalSettings.get("launch_left"));
        }
        if (originalSettings.containsKey("launch_right")) {
            saveManager.setControlKey("LAUNCH_RIGHT", (String) originalSettings.get("launch_right"));
        }
        if (originalSettings.containsKey("turbo_paddle")) {
            saveManager.setControlKey("TURBO_PADDLE", (String) originalSettings.get("turbo_paddle"));
        }
        if (originalSettings.containsKey("call_ball")) {
            saveManager.setControlKey("CALL_BALL", (String) originalSettings.get("call_ball"));
        }
        if (originalSettings.containsKey("player_name")) {
            saveManager.setPlayerName((String) originalSettings.get("player_name"));
        }
        if (originalSettings.containsKey("paddle_speed")) {
            saveManager.setPaddleSpeed((Double) originalSettings.get("paddle_speed"));
        }
        if (originalSettings.containsKey("fullscreen")) {
            saveManager.setFullscreen((Boolean) originalSettings.get("fullscreen"));
        }
        if (originalSettings.containsKey("vsync_enabled")) {
            saveManager.setVSyncEnabled((Boolean) originalSettings.get("vsync_enabled"));
        }
        if (originalSettings.containsKey("call_ball_sound_enabled")) {
            saveManager.setCallBallSoundEnabled((Boolean) originalSettings.get("call_ball_sound_enabled"));
        }
        
        // Обновляем текущие настройки
        currentSettings.clear();
        currentSettings.putAll(originalSettings);
        hasUnsavedChanges = false;
    }
    
    /**
     * Получить список измененных настроек
     */
    public Map<String, String> getChangedSettings() {
        Map<String, String> changes = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : currentSettings.entrySet()) {
            String key = entry.getKey();
            Object currentValue = entry.getValue();
            Object originalValue = originalSettings.get(key);
            
            // Проверяем на null и сравниваем значения
            if (!Objects.equals(currentValue, originalValue)) {
                String originalStr = originalValue != null ? originalValue.toString() : "null";
                String currentStr = currentValue != null ? currentValue.toString() : "null";
                changes.put(key, String.format("%s → %s", originalStr, currentStr));
            }
        }
        
        return changes;
    }
    
    /**
     * Получить количество изменений
     */
    public int getChangeCount() {
        return getChangedSettings().size();
    }
}
