package com.arcadeblocks.config;

import com.arcadeblocks.localization.LocalizationManager;

/**
 * Конфигурация информации об авторах игры
 */
public final class AuthorsConfig {
    
    private static final String[] AUTHOR_KEYS = {
        "credits.author.lead_dev",
        "credits.author.game_design",
        "credits.author.music",
        "credits.author.designer",
        "credits.author.junior_dev",
        "credits.author.qa"
    };
    
    // Информация о версии
    public static final String VERSION = "1.19.1";
    public static final String BUILD_DATE = "2025";
    public static final String COMPANY = "Leocallidus Games";
    
    private AuthorsConfig() {
    }
    
    /**
     * Получить полные титры
     */
    public static String getCredits() {
        return LocalizationManager.getInstance().get("credits.full");
    }
    
    /**
     * Получить краткие титры
     */
    public static String getShortCredits() {
        return LocalizationManager.getInstance().get("credits.short");
    }
    
    /**
     * Получить информацию об авторах
     */
    public static String[] getAuthors() {
        LocalizationManager i18n = LocalizationManager.getInstance();
        String[] authors = new String[AUTHOR_KEYS.length];
        for (int i = 0; i < AUTHOR_KEYS.length; i++) {
            authors[i] = i18n.get(AUTHOR_KEYS[i]);
        }
        return authors;
    }
    
    /**
     * Получить версию игры
     */
    public static String getVersion() {
        return VERSION;
    }
}
