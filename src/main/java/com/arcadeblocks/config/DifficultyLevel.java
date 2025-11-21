package com.arcadeblocks.config;

/**
 * Уровни сложности игры
 */
import com.arcadeblocks.localization.LocalizationManager;

public enum DifficultyLevel {
    EASY("difficulty.easy", 6),
    NORMAL("difficulty.normal", 4),
    HARD("difficulty.hard", 2),
    HARDCORE("difficulty.hardcore", 1);
    
    private final String displayNameKey;
    private final int lives;
    
    DifficultyLevel(String displayNameKey, int lives) {
        this.displayNameKey = displayNameKey;
        this.lives = lives;
    }
    
    public String getDisplayName() {
        return LocalizationManager.getInstance().getOrDefault(displayNameKey, displayNameKey);
    }
    
    public int getLives() {
        return lives;
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
}
