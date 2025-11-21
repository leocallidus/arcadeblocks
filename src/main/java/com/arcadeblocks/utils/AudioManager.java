package com.arcadeblocks.utils;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.config.AudioConfig;

/**
 * Менеджер аудио для управления музыкой и звуковыми эффектами
 */
public class AudioManager {
    
    private double masterVolume = 1.0; // Максимальная громкость для тестирования
    private double musicVolume = 1.0;  // Максимальная громкость для тестирования
    private double sfxVolume = 1.0;    // Максимальная громкость для тестирования
    
    private boolean soundEnabled = true; // Отключено - звуковые файлы отсутствуют
    private boolean musicEnabled = true; // Отключено - музыка отключена
    
    private String currentMusicFile;
    
    /**
     * Воспроизвести музыку
     */
    public void playMusic(String musicFile, boolean loop) {
        if (!musicEnabled || masterVolume == 0 || musicVolume == 0) {
            return;
        }
        
        try {
            // Остановить текущую музыку
            stopMusic();
            
            // Музыка отключена из-за проблем с JavaFX Media в Linux
            // В будущем можно добавить альтернативные решения
            currentMusicFile = musicFile;
            
        } catch (Exception e) {
            System.err.println("Ошибка воспроизведения музыки: " + musicFile + " - " + e.getMessage());
        }
    }
    
    /**
     * Воспроизвести звуковой эффект
     */
    public void playSFX(String sfxFile) {
        if (!soundEnabled || masterVolume == 0 || sfxVolume == 0) {
            return;
        }
        
        try {
            // Обходное решение: используем системный звук
            // JavaFX Media имеет проблемы в Linux, поэтому используем системный звук
            java.awt.Toolkit.getDefaultToolkit().beep();
            
            // Попытка воспроизведения через FXGL (может не работать)
            String correctedPath = sfxFile.startsWith("sounds/") ? sfxFile.substring(7) : sfxFile;
            if (isValidAudioFile(correctedPath, "sounds")) {
                try {
                    var sound = FXGL.getAssetLoader().loadSound(correctedPath);
                    FXGL.getAudioPlayer().playSound(sound);
                } catch (Exception e) {
                    // FXGL не работает, но системный звук уже воспроизведен
                }
            }
            
        } catch (Exception e) {
            // Игнорируем ошибки, системный звук уже воспроизведен
        }
    }
    
    /**
     * Воспроизвести звуковой эффект по имени из конфига
     */
    public void playSFXByName(String effectName) {
        String sfxFile = AudioConfig.getSFX(effectName);
        if (sfxFile != null) {
            playSFX(sfxFile);
        }
    }
    
    /**
     * Остановить музыку
     */
    public void stopMusic() {
        if (currentMusicFile != null) {
            try {
                String correctedPath = currentMusicFile.startsWith("music/") ? currentMusicFile.substring(6) : currentMusicFile;
                FXGL.getAudioPlayer().stopMusic(FXGL.getAssetLoader().loadMusic(correctedPath));
            } catch (Exception e) {
                // Игнорируем ошибки при остановке
            }
            currentMusicFile = null;
        }
    }
    
    /**
     * Остановить все аудио
     */
    public void stopAll() {
        stopMusic();
        FXGL.getAudioPlayer().stopAllSounds();
    }
    
    /**
     * Остановить все звуковые эффекты (SFX) без остановки музыки
     */
    public void stopAllSFX() {
        FXGL.getAudioPlayer().stopAllSounds();
    }
    
    /**
     * Пауза музыки
     */
    public void pauseMusic() {
        if (currentMusicFile != null) {
            try {
                String correctedPath = currentMusicFile.startsWith("music/") ? currentMusicFile.substring(6) : currentMusicFile;
                FXGL.getAudioPlayer().pauseMusic(FXGL.getAssetLoader().loadMusic(correctedPath));
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
    }
    
    /**
     * Возобновить музыку
     */
    public void resumeMusic() {
        if (currentMusicFile != null) {
            try {
                String correctedPath = currentMusicFile.startsWith("music/") ? currentMusicFile.substring(6) : currentMusicFile;
                FXGL.getAudioPlayer().resumeMusic(FXGL.getAssetLoader().loadMusic(correctedPath));
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
    }
    
    /**
     * Установить общую громкость
     */
    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, volume));
        updateMusicVolume();
        updateSfxVolume();
    }
    
    /**
     * Установить громкость музыки
     */
    public void setMusicVolume(double volume) {
        this.musicVolume = Math.max(0.0, Math.min(1.0, volume));
        updateMusicVolume();
    }
    
    /**
     * Установить громкость звуковых эффектов
     */
    public void setSfxVolume(double volume) {
        this.sfxVolume = Math.max(0.0, Math.min(1.0, volume));
        updateSfxVolume();
    }
    
    /**
     * Включить/выключить звук
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            stopAll();
        }
    }
    
    /**
     * Включить/выключить музыку
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
    }
    
    /**
     * Обновить громкость текущей музыки
     */
    private void updateMusicVolume() {
        FXGL.getSettings().setGlobalMusicVolume(masterVolume * musicVolume);
    }
    
    /**
     * Обновить громкость звуковых эффектов
     */
    private void updateSfxVolume() {
        FXGL.getSettings().setGlobalSoundVolume(masterVolume * sfxVolume);
    }
    
    /**
     * Получить текущую общую громкость
     */
    public double getMasterVolume() {
        return masterVolume;
    }
    
    /**
     * Получить текущую громкость музыки
     */
    public double getMusicVolume() {
        return musicVolume;
    }
    
    /**
     * Получить текущую громкость звуковых эффектов
     */
    public double getSfxVolume() {
        return sfxVolume;
    }
    
    /**
     * Проверить, включен ли звук
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Проверить, включена ли музыка
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    /**
     * Проверить, играет ли музыка
     */
    public boolean isMusicPlaying() {
        return currentMusicFile != null;
    }
    
    /**
     * Обновление (вызывается каждый кадр)
     */
    public void update(double tpf) {
        // Здесь можно добавить логику обновления аудио
    }
    
    /**
     * Предзагрузка музыки для уровня
     */
    public void preloadLevelMusic(int levelNumber) {
        // В FXGL 17.3 предзагрузка происходит автоматически
    }
    
    /**
     * Очистить кэш музыки
     */
    public void clearMusicCache() {
        // В FXGL 17.3 управляется автоматически
    }
    
    /**
     * Проверяет, существует ли аудиофайл и не является ли он пустым
     */
    private boolean isValidAudioFile(String fileName, String folder) {
        try {
            // Создаем путь к файлу в ресурсах
            String resourcePath = "assets/" + folder + "/" + fileName;
            
            // Получаем URL ресурса
            java.net.URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
            if (resourceUrl == null) {
                return false; // Файл не найден
            }
            
            // Проверяем размер файла
            java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                return false;
            }
            
            int byteCount = inputStream.available();
            inputStream.close();
            
            // Файл должен быть больше 0 байт
            return byteCount > 0;
            
        } catch (Exception e) {
            // Если произошла ошибка при проверке, считаем файл невалидным
            return false;
        }
    }
}
