package com.arcadeblocks.config;

/**
 * Конфигурационный класс для управления бонусами
 * Позволяет легко включать/выключать бонусы для тестирования
 */
public class BonusConfig {
    
    // === ПОЗИТИВНЫЕ БОНУСЫ ===
    
    /** Увеличение размера ракетки */
    public static boolean INCREASE_PADDLE_ENABLED = true;
    
    /** Уменьшение размера ракетки */
    public static boolean DECREASE_PADDLE_ENABLED = true;
    
    /** Липкая ракетка (мяч прилипает к ракетке) */
    public static boolean STICKY_PADDLE_ENABLED = true;
    
    /** Быстрые мячи */
    public static boolean FAST_BALLS_ENABLED = true;
    
    /** Медленные мячи */
    public static boolean SLOW_BALLS_ENABLED = true;
    
    /** Энергетические мячи (проходят сквозь кирпичи) */
    public static boolean ENERGY_BALLS_ENABLED = true;
    
    /** Взрывные мячи */
    public static boolean EXPLOSION_BALLS_ENABLED = true;
    
    /** Слабые мячи */
    public static boolean WEAK_BALLS_ENABLED = true;
    
    /** Дополнительный мяч */
    public static boolean BONUS_BALL_ENABLED = true;
    
    /** Дополнительные очки */
    public static boolean BONUS_SCORE_ENABLED = true;
    
    /** Дополнительная жизнь */
    public static boolean EXTRA_LIFE_ENABLED = true;
    
    /** Защитный барьер */
    public static boolean BONUS_WALL_ENABLED = true;
    
    /** Магнит для бонусов */
    public static boolean BONUS_MAGNET_ENABLED = true;
    
    /** Плазменное оружие */
    public static boolean PLASMA_WEAPON_ENABLED = true;
    
    /** Проход уровня */
    public static boolean LEVEL_PASS_ENABLED = true;

    /** Дождь очков */
    public static boolean SCORE_RAIN_ENABLED = true;

// === НЕГАТИВНЫЕ БОНУСЫ (ШТРАФЫ) ===
    
    /** Хаотичные мячи */
    public static boolean CHAOTIC_BALLS_ENABLED = true;
    
    /** Замороженная ракетка */
    public static boolean FROZEN_PADDLE_ENABLED = true;
    
    /** Призрачная ракетка */
    public static boolean INVISIBLE_PADDLE_ENABLED = true;

    /** Темнота */
    public static boolean DARKNESS_ENABLED = true;

    /** Сброс всех бонусов */
    public static boolean RESET_ENABLED = true;
    
/** Шулер (активация всех позитивных бонусов) */
public static boolean TRICKSTER_ENABLED = true;

/** Невезуха (активация всех негативных бонусов) */
public static boolean BAD_LUCK_ENABLED = true;
    
    /** Магнит для штрафов */
    public static boolean PENALTIES_MAGNET_ENABLED = true;
    
    /** Случайный бонус */
    public static boolean RANDOM_BONUS_ENABLED = true;
    
    // === НАСТРОЙКИ ТЕСТИРОВАНИЯ ===
    
    /** Режим тестирования - все бонусы активны */
    public static boolean TESTING_MODE = false;
    
    /** Только позитивные бонусы для тестирования */
    public static boolean POSITIVE_BONUSES_ONLY = false;
    
    /** Только негативные бонусы для тестирования */
    public static boolean NEGATIVE_BONUSES_ONLY = false;
    
    /** Отключить все бонусы */
    public static boolean DISABLE_ALL_BONUSES = false;
    
    // === МЕТОДЫ ПРОВЕРКИ ===
    
    /**
     * Проверить, доступен ли бонус для выпадения
     * @param bonusName название бонуса
     * @return true если бонус доступен
     */
    public static boolean isBonusEnabled(String bonusName) {
        // Если отключены все бонусы
        if (DISABLE_ALL_BONUSES) {
            return false;
        }
        
        // Режим тестирования - все бонусы активны
        if (TESTING_MODE) {
            return true;
        }
        
        // Только позитивные бонусы
        if (POSITIVE_BONUSES_ONLY) {
            return isPositiveBonus(bonusName);
        }
        
        // Только негативные бонусы
        if (NEGATIVE_BONUSES_ONLY) {
            return isNegativeBonus(bonusName);
        }
        
        // Проверка конкретного бонуса
        switch (bonusName.toLowerCase()) {
            // Позитивные бонусы
            case "bonus_score":
                return BONUS_SCORE_ENABLED;
            case "extra_life":
                return EXTRA_LIFE_ENABLED;
            case "increase_paddle":
                return INCREASE_PADDLE_ENABLED;
            case "sticky_paddle":
                return STICKY_PADDLE_ENABLED;
            case "slow_balls":
                return SLOW_BALLS_ENABLED;
            case "energy_balls":
                return ENERGY_BALLS_ENABLED;
            case "bonus_wall":
                return BONUS_WALL_ENABLED;
            case "bonus_magnet":
                return BONUS_MAGNET_ENABLED;
            case "bonus_ball":
                return BONUS_BALL_ENABLED;
            case "plasma_weapon":
                return PLASMA_WEAPON_ENABLED;
            case "explosion_balls":
                return EXPLOSION_BALLS_ENABLED;
            case "level_pass":
                return LEVEL_PASS_ENABLED;
            case "score_rain":
                return SCORE_RAIN_ENABLED;
            case "trickster":
                return TRICKSTER_ENABLED;
                
            // Негативные бонусы
            case "chaotic_balls":
                return CHAOTIC_BALLS_ENABLED;
            case "frozen_paddle":
                return FROZEN_PADDLE_ENABLED;
            case "decrease_paddle":
                return DECREASE_PADDLE_ENABLED;
            case "fast_balls":
                return FAST_BALLS_ENABLED;
            case "penalties_magnet":
                return PENALTIES_MAGNET_ENABLED;
            case "weak_balls":
                return WEAK_BALLS_ENABLED;
            case "invisible_paddle":
                return INVISIBLE_PADDLE_ENABLED;
            case "darkness":
                return DARKNESS_ENABLED;
            case "reset":
                return RESET_ENABLED;
            case "bad_luck":
                return BAD_LUCK_ENABLED;
            case "random_bonus":
                return RANDOM_BONUS_ENABLED;
                
            default:
                return true; // По умолчанию бонус доступен
        }
    }
    
    /**
     * Проверить, является ли бонус позитивным
     * @param bonusName название бонуса
     * @return true если бонус позитивный
     */
    private static boolean isPositiveBonus(String bonusName) {
        switch (bonusName.toLowerCase()) {
            case "bonus_score":
            case "extra_life":
            case "increase_paddle":
            case "sticky_paddle":
            case "slow_balls":
            case "energy_balls":
            case "bonus_wall":
            case "bonus_magnet":
            case "bonus_ball":
            case "plasma_weapon":
            case "explosion_balls":
            case "level_pass":
            case "trickster":
            case "score_rain":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Проверить, является ли бонус негативным
     * @param bonusName название бонуса
     * @return true если бонус негативный
     */
    private static boolean isNegativeBonus(String bonusName) {
        switch (bonusName.toLowerCase()) {
            case "chaotic_balls":
            case "frozen_paddle":
            case "decrease_paddle":
            case "fast_balls":
            case "penalties_magnet":
            case "weak_balls":
            case "invisible_paddle":
            case "darkness":
            case "reset":
            case "bad_luck":
            case "random":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Получить статистику включенных бонусов
     * @return строка со статистикой
     */
    public static String getBonusStatistics() {
        if (DISABLE_ALL_BONUSES) {
            return "Все бонусы отключены";
        }
        
        if (TESTING_MODE) {
            return "Режим тестирования: все бонусы активны";
        }
        
        if (POSITIVE_BONUSES_ONLY) {
            return "Только позитивные бонусы активны";
        }
        
        if (NEGATIVE_BONUSES_ONLY) {
            return "Только негативные бонусы активны";
        }
        
        // Подсчет активных бонусов
        int positiveCount = 0;
        int negativeCount = 0;
        
        if (INCREASE_PADDLE_ENABLED) positiveCount++;
        if (DECREASE_PADDLE_ENABLED) positiveCount++;
        if (STICKY_PADDLE_ENABLED) positiveCount++;
        if (FAST_BALLS_ENABLED) positiveCount++;
        if (SLOW_BALLS_ENABLED) positiveCount++;
        if (ENERGY_BALLS_ENABLED) positiveCount++;
        if (EXPLOSION_BALLS_ENABLED) positiveCount++;
        if (WEAK_BALLS_ENABLED) positiveCount++;
        if (BONUS_BALL_ENABLED) positiveCount++;
        if (BONUS_SCORE_ENABLED) positiveCount++;
        if (EXTRA_LIFE_ENABLED) positiveCount++;
        if (BONUS_WALL_ENABLED) positiveCount++;
        if (BONUS_MAGNET_ENABLED) positiveCount++;
        if (PLASMA_WEAPON_ENABLED) positiveCount++;
        if (LEVEL_PASS_ENABLED) positiveCount++;
        if (TRICKSTER_ENABLED) positiveCount++;
        if (SCORE_RAIN_ENABLED) positiveCount++;
        
        if (CHAOTIC_BALLS_ENABLED) negativeCount++;
        if (FROZEN_PADDLE_ENABLED) negativeCount++;
        if (INVISIBLE_PADDLE_ENABLED) negativeCount++;
        if (PENALTIES_MAGNET_ENABLED) negativeCount++;
        if (DARKNESS_ENABLED) negativeCount++;
        if (RESET_ENABLED) negativeCount++;
        if (BAD_LUCK_ENABLED) negativeCount++;
        if (RANDOM_BONUS_ENABLED) positiveCount++;
        
        return String.format("Активных бонусов: %d позитивных, %d негативных", positiveCount, negativeCount);
    }
    
    /**
     * Быстрое включение всех бонусов для тестирования
     */
    public static void enableAllBonuses() {
        // Этот метод можно использовать для программного включения всех бонусов
        // В реальной реализации нужно будет перекомпилировать код
        // System.out.println("Для включения всех бонусов установите TESTING_MODE = true");
    }
    
    /**
     * Быстрое отключение всех бонусов
     */
    public static void disableAllBonuses() {
        // Этот метод можно использовать для программного отключения всех бонусов
        // System.out.println("Для отключения всех бонусов установите DISABLE_ALL_BONUSES = true");
    }
    
    // === МЕТОДЫ ДЛЯ DEBUG МЕНЮ ===
    
    /**
     * Установить состояние бонуса
     * @param fieldName название поля
     * @param enabled включен ли бонус
     */
    public static void setBonusEnabled(String fieldName, boolean enabled) {
        switch (fieldName) {
            case "INCREASE_PADDLE_ENABLED":
                INCREASE_PADDLE_ENABLED = enabled;
                break;
            case "DECREASE_PADDLE_ENABLED":
                DECREASE_PADDLE_ENABLED = enabled;
                break;
            case "STICKY_PADDLE_ENABLED":
                STICKY_PADDLE_ENABLED = enabled;
                break;
            case "FAST_BALLS_ENABLED":
                FAST_BALLS_ENABLED = enabled;
                break;
            case "SLOW_BALLS_ENABLED":
                SLOW_BALLS_ENABLED = enabled;
                break;
            case "ENERGY_BALLS_ENABLED":
                ENERGY_BALLS_ENABLED = enabled;
                break;
            case "EXPLOSION_BALLS_ENABLED":
                EXPLOSION_BALLS_ENABLED = enabled;
                break;
            case "WEAK_BALLS_ENABLED":
                WEAK_BALLS_ENABLED = enabled;
                break;
            case "BONUS_BALL_ENABLED":
                BONUS_BALL_ENABLED = enabled;
                break;
            case "BONUS_SCORE_ENABLED":
                BONUS_SCORE_ENABLED = enabled;
                break;
            case "EXTRA_LIFE_ENABLED":
                EXTRA_LIFE_ENABLED = enabled;
                break;
            case "BONUS_WALL_ENABLED":
                BONUS_WALL_ENABLED = enabled;
                break;
            case "BONUS_MAGNET_ENABLED":
                BONUS_MAGNET_ENABLED = enabled;
                break;
            case "PLASMA_WEAPON_ENABLED":
                PLASMA_WEAPON_ENABLED = enabled;
                break;
            case "LEVEL_PASS_ENABLED":
                LEVEL_PASS_ENABLED = enabled;
                break;
            case "SCORE_RAIN_ENABLED":
                SCORE_RAIN_ENABLED = enabled;
                break;
            case "TRICKSTER_ENABLED":
                TRICKSTER_ENABLED = enabled;
                break;
            case "CHAOTIC_BALLS_ENABLED":
                CHAOTIC_BALLS_ENABLED = enabled;
                break;
            case "FROZEN_PADDLE_ENABLED":
                FROZEN_PADDLE_ENABLED = enabled;
                break;
            case "INVISIBLE_PADDLE_ENABLED":
                INVISIBLE_PADDLE_ENABLED = enabled;
                break;
            case "PENALTIES_MAGNET_ENABLED":
                PENALTIES_MAGNET_ENABLED = enabled;
                break;
            case "DARKNESS_ENABLED":
                DARKNESS_ENABLED = enabled;
                break;
            case "RESET_ENABLED":
                RESET_ENABLED = enabled;
                break;
            case "BAD_LUCK_ENABLED":
                BAD_LUCK_ENABLED = enabled;
                break;
            case "RANDOM_BONUS_ENABLED":
                RANDOM_BONUS_ENABLED = enabled;
                break;
            case "TESTING_MODE":
                TESTING_MODE = enabled;
                break;
            case "POSITIVE_BONUSES_ONLY":
                POSITIVE_BONUSES_ONLY = enabled;
                break;
            case "NEGATIVE_BONUSES_ONLY":
                NEGATIVE_BONUSES_ONLY = enabled;
                break;
            case "DISABLE_ALL_BONUSES":
                DISABLE_ALL_BONUSES = enabled;
                break;
            default:
        // System.out.println("Неизвестное поле: " + fieldName);
                break;
        }
    }
    
    /**
     * Получить состояние бонуса
     * @param fieldName название поля
     * @return состояние бонуса
     */
    public static boolean getBonusEnabled(String fieldName) {
        switch (fieldName) {
            case "INCREASE_PADDLE_ENABLED":
                return INCREASE_PADDLE_ENABLED;
            case "DECREASE_PADDLE_ENABLED":
                return DECREASE_PADDLE_ENABLED;
            case "STICKY_PADDLE_ENABLED":
                return STICKY_PADDLE_ENABLED;
            case "FAST_BALLS_ENABLED":
                return FAST_BALLS_ENABLED;
            case "SLOW_BALLS_ENABLED":
                return SLOW_BALLS_ENABLED;
            case "ENERGY_BALLS_ENABLED":
                return ENERGY_BALLS_ENABLED;
            case "EXPLOSION_BALLS_ENABLED":
                return EXPLOSION_BALLS_ENABLED;
            case "WEAK_BALLS_ENABLED":
                return WEAK_BALLS_ENABLED;
            case "BONUS_BALL_ENABLED":
                return BONUS_BALL_ENABLED;
            case "BONUS_SCORE_ENABLED":
                return BONUS_SCORE_ENABLED;
            case "EXTRA_LIFE_ENABLED":
                return EXTRA_LIFE_ENABLED;
            case "BONUS_WALL_ENABLED":
                return BONUS_WALL_ENABLED;
            case "BONUS_MAGNET_ENABLED":
                return BONUS_MAGNET_ENABLED;
            case "PLASMA_WEAPON_ENABLED":
                return PLASMA_WEAPON_ENABLED;
            case "LEVEL_PASS_ENABLED":
                return LEVEL_PASS_ENABLED;
            case "SCORE_RAIN_ENABLED":
                return SCORE_RAIN_ENABLED;
            case "TRICKSTER_ENABLED":
                return TRICKSTER_ENABLED;
            case "CHAOTIC_BALLS_ENABLED":
                return CHAOTIC_BALLS_ENABLED;
            case "FROZEN_PADDLE_ENABLED":
                return FROZEN_PADDLE_ENABLED;
            case "INVISIBLE_PADDLE_ENABLED":
                return INVISIBLE_PADDLE_ENABLED;
            case "PENALTIES_MAGNET_ENABLED":
                return PENALTIES_MAGNET_ENABLED;
            case "DARKNESS_ENABLED":
                return DARKNESS_ENABLED;
            case "RESET_ENABLED":
                return RESET_ENABLED;
            case "BAD_LUCK_ENABLED":
                return BAD_LUCK_ENABLED;
            case "RANDOM_BONUS_ENABLED":
                return RANDOM_BONUS_ENABLED;
            case "TESTING_MODE":
                return TESTING_MODE;
            case "POSITIVE_BONUSES_ONLY":
                return POSITIVE_BONUSES_ONLY;
            case "NEGATIVE_BONUSES_ONLY":
                return NEGATIVE_BONUSES_ONLY;
            case "DISABLE_ALL_BONUSES":
                return DISABLE_ALL_BONUSES;
            default:
        // System.out.println("Неизвестное поле: " + fieldName);
                return false;
        }
    }
}
