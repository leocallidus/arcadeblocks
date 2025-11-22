package com.arcadeblocks.config;

import javafx.geometry.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Основные константы и настройки игры
 */
public class GameConfig {
    
    // Базовые размеры игрового мира (всегда 1600x900)
    public static final int GAME_WORLD_WIDTH = 1600;
    public static final int GAME_WORLD_HEIGHT = 900;
    
    // Размеры окна/экрана (могут меняться через настройки)
    public static final int GAME_WIDTH = 1600;
    public static final int GAME_HEIGHT = 900;
    
    // Доступные разрешения
    public static final Resolution RESOLUTION_1600x900 = new Resolution(1600, 900);
    public static final Resolution RESOLUTION_1920x1080 = new Resolution(1920, 1080);
    public static final Resolution DEFAULT_RESOLUTION = RESOLUTION_1920x1080;
    
    // Текущее разрешение (устанавливается при запуске)
    private static Resolution currentResolution = DEFAULT_RESOLUTION;
    
    // Размеры игровых объектов
    public static final int PADDLE_WIDTH = 170;
    public static final int PADDLE_HEIGHT = 28;
    public static final int BALL_RADIUS = 11;
    public static final int BRICK_WIDTH = 85;
    public static final int BRICK_HEIGHT = 32;
    public static final double PADDLE_MAX_SIZE_MULTIPLIER = 6.0;
    
    // Отступы игрового поля
    public static final double TOP_UI_HEIGHT = 110.0;
    
    // Скорости
    public static final double PADDLE_SPEED = 400.0;
    public static final double BALL_SPEED = 650.0;  // Увеличили с 500 до 750 (1.5x) для оптимальной скорости
    public static final double BOSS_PROJECTILE_SPEED = 200.0;
    
    // Игровые константы
    public static final int INITIAL_LIVES = 6;
    public static final int BALL_DAMAGE_TO_BRICK = 1;
    public static final double BOSS_DAMAGE_FROM_STANDARD_BALL = 0.4;
    public static final double BOSS_DAMAGE_FROM_WEAK_BALL = 0.2;
    public static final int TOTAL_LEVELS = 116;
    
    // Бонусы
    public static final double POWERUP_DROP_CHANCE = 0.15;
    public static final double GOOD_POWERUP_CHANCE = 0.5;
    
    // Тайминги (в секундах)
    public static final double POWERUP_DURATION = 20.0;
    public static final double BOSS_SHOOT_INTERVAL = 2.0;
    public static final double FROZEN_PADDLE_DURATION = 5.0;
    public static final double GHOST_PADDLE_DURATION = 5.0;
    public static final double CHAOTIC_BALLS_DURATION = 15.0;
    
    // Цветовая палитра (пастельно-неоновая)
    public static final String NEON_PINK = "#FF6EC7";
    public static final String NEON_CYAN = "#7EE8FA";
    public static final String NEON_PURPLE = "#B388FF";
    public static final String NEON_GREEN = "#7FFF7F";
    public static final String NEON_ORANGE = "#FFB347";
    public static final String NEON_YELLOW = "#FFFF7F";
    public static final String DARK_BACKGROUND = "#1A1A2E";
    public static final String LIGHT_BACKGROUND = "#16213E";
    
    // Позиции игровых объектов (для полного экрана)
    public static final Point2D PADDLE_START_POS = new Point2D(
        GAME_WIDTH / 2.0 - PADDLE_WIDTH / 2.0,
        GAME_HEIGHT - 50
    );
    
    public static final Point2D BALL_START_POS = new Point2D(
        GAME_WIDTH / 2.0,
        GAME_HEIGHT - 80
    );
    
    // Размеры UI
    public static final int HUD_HEIGHT = 80;
    public static final int MENU_BUTTON_WIDTH = 200;
    public static final int MENU_BUTTON_HEIGHT = 40;
    
    // Настройки сохранения
    public static final String SAVE_FILE = "arcade_blocks_save.dat";
    public static final String SETTINGS_FILE = "arcade_blocks_settings.dat";
    
    // Управление по умолчанию
    public static final Map<String, String> DEFAULT_CONTROLS = new HashMap<>() {{
        put("MOVE_LEFT", "LEFT");
        put("MOVE_RIGHT", "RIGHT");
        put("LAUNCH", "SPACE");
        put("CALL_BALL", "V");
        put("TURBO_PADDLE", "X");
        put("PAUSE", "ESCAPE");
        put("PLASMA_WEAPON", "Z");
    }};
    
    // Звуковые настройки по умолчанию
    public static final boolean DEFAULT_SOUND_ENABLED = true;
    public static final double DEFAULT_VOLUME = 0.7;
    
    // Анимации
    public static final double ANIMATION_DURATION = 0.3;
    public static final double PARTICLE_LIFETIME = 2.0;
    
    // Боссы
    public static final int FIREWALL_7_HP = 10;
    public static final int NEON_ARCHITECT_HP = 20;
    public static final int MONOLITH_EXE_HP = 30;
    public static final int SINGULARITY_CORE_HP = 36;
    public static final int MONOLITH_ASCENT_HP = 40;
    public static final int CERBERUS_SENTINEL_HP = 45;
    public static final int LEVIATHAN_AI_HP = 52;
    public static final int GOLIATH_DEFENDER_HP = 58;
    public static final int DEMIURGE_ARCHITECT_HP = 64;
    public static final int DIGITAL_IMMORTALITY_HP = 72;
    
    // Методы для работы с разрешением
    public static Resolution getCurrentResolution() {
        return currentResolution;
    }
    
    public static void setCurrentResolution(Resolution resolution) {
        if (resolution != null) {
            currentResolution = resolution;
        }
    }
    
    /**
     * Получить доступные разрешения
     */
    public static Resolution[] getAvailableResolutions() {
        return new Resolution[] {
            RESOLUTION_1600x900,
            RESOLUTION_1920x1080
        };
    }
    
    /**
     * Получить доступные разрешения для указанного режима окна
     * @param isFullscreen true если полноэкранный режим, false если оконный
     */
    public static Resolution[] getAvailableResolutions(boolean isFullscreen) {
        if (isFullscreen) {
            // В полноэкранном режиме только 1920x1080
            return new Resolution[] {
                RESOLUTION_1920x1080
            };
        } else {
            // В оконном режиме доступны оба разрешения
            return new Resolution[] {
                RESOLUTION_1600x900,
                RESOLUTION_1920x1080
            };
        }
    }
    
    /**
     * Получить смещение letterbox для центрирования игрового мира
     */
    public static double getLetterboxOffsetX() {
        return (currentResolution.getWidth() - GAME_WORLD_WIDTH) / 2.0;
    }
    
    public static double getLetterboxOffsetY() {
        return (currentResolution.getHeight() - GAME_WORLD_HEIGHT) / 2.0;
    }
}
