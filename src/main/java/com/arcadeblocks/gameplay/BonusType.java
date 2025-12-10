package com.arcadeblocks.gameplay;

import com.arcadeblocks.config.BonusConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Перечисление типов бонусов в игре
 */
public enum BonusType {
    // Позитивные бонусы
    BONUS_SCORE("bonus_score.png", "Дополнительные очки", true),
    BONUS_SCORE_200("bonus_score200.png", "+200 очков", true),
    BONUS_SCORE_500("bonus_score500.png", "+500 очков", true),
    ADD_FIVE_SECONDS("bonus_add_five_second.png", "+5 секунд ко всем активным бонусам", true),
    CALL_BALL("call_ball_to_paddle_bonus.png", "Притянуть мяч к ракетке", true),
    EXTRA_LIFE("extra_life.png", "Дополнительная жизнь", true),
    INCREASE_PADDLE("increase_paddle.png", "Увеличение ракетки", true),
    STICKY_PADDLE("sticky_paddle.png", "Липкая ракетка", true),
    SLOW_BALLS("slow_balls.png", "Медленные мячи", true),
    ENERGY_BALLS("energy_balls.png", "Энергетические мячи", true),
    BONUS_WALL("bonus_wall.png", "Защитный барьер", true),
    BONUS_MAGNET("bonus_magnet.png", "Магнит бонусов", true),
    BONUS_BALL("bonus_ball.png", "Дополнительный мяч", true),
    PLASMA_WEAPON("plasma_weapon.png", "Плазменное оружие", true),
    EXPLOSION_BALLS("explosion_balls.png", "Взрывные мячи", true),
    TRICKSTER("trickster.png", "Шулер", true),
    SCORE_RAIN("score_rain.png", "Дождь очков", true),
    LEVEL_PASS("level_complete_bonus.png", "Проход уровня", true),

    // Негативные бонусы
    CHAOTIC_BALLS("chaotic_balls.png", "Хаотичные мячи", false),
    FROZEN_PADDLE("frozen_paddle.png", "Замороженная ракетка", false),
    DECREASE_PADDLE("decrease_paddle.png", "Уменьшение ракетки", false),
    FAST_BALLS("fast_balls.png", "Быстрые мячи", false),
    PENALTIES_MAGNET("penalties_magnet.png", "Магнит штрафов", false),
    WEAK_BALLS("weak_balls.png", "Слабые мячи", false),
    INVISIBLE_PADDLE("invisible_paddle.png", "Призрачная ракетка", false),
    DARKNESS("darkness.png", "Темнота", false),
    BAD_LUCK("bad_luck.png", "Невезуха", false),
    RESET("reset.png", "Сброс бонусов", false),
    RANDOM_BONUS("random.png", "Случайный бонус", true);
    
    private final String textureName;
    private final String description;
    private final boolean isPositive;
    
    BonusType(String textureName, String description, boolean isPositive) {
        this.textureName = textureName;
        this.description = description;
        this.isPositive = isPositive;
    }
    
    public String getTextureName() {
        return textureName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isPositive() {
        return isPositive;
    }
    
    public boolean isNegative() {
        return !isPositive;
    }
    
    /**
     * Получить случайный бонус (только включенные в конфигурации)
     */
    public static BonusType getRandomBonus() {
        List<BonusType> enabledBonuses = getEnabledBonuses();
        
        if (enabledBonuses.isEmpty()) {
            return BONUS_SCORE; // Fallback на случай если все бонусы отключены
        }
        
        return enabledBonuses.get((int) (Math.random() * enabledBonuses.size()));
    }
    
    /**
     * Получить случайный бонус с учетом весовых коэффициентов
     * Энергетические и взрывные мячи имеют 3% шанс, остальные бонусы - равные шансы
     */
    public static BonusType getWeightedRandomBonus() {
        List<BonusType> enabledBonuses = getEnabledBonuses();

        if (enabledBonuses.isEmpty()) {
            return BONUS_SCORE; // Fallback на случай если все бонусы отключены
        }

        enabledBonuses.remove(LEVEL_PASS);
        enabledBonuses.remove(TRICKSTER);
        enabledBonuses.remove(BAD_LUCK);

        double random = Math.random();
        double threshold = 0.0;

        if (BonusConfig.isBonusEnabled("trickster")) {
            threshold += 0.01;
            if (random < threshold) {
                return TRICKSTER;
            }
        }

        if (BonusConfig.isBonusEnabled("bad_luck")) {
            threshold += 0.01;
            if (random < threshold) {
                return BAD_LUCK;
            }
        }

        if (BonusConfig.isBonusEnabled("energy_balls")) {
            threshold += 0.03;
            if (random < threshold) {
                return ENERGY_BALLS;
            }
        }

        if (BonusConfig.isBonusEnabled("explosion_balls")) {
            threshold += 0.03;
            if (random < threshold) {
                return EXPLOSION_BALLS;
            }
        }
        
        // Если специальные бонусы не выпали или отключены, выбираем обычным способом
        // Исключаем ENERGY_BALLS и EXPLOSION_BALLS из обычного выбора
        List<BonusType> regularBonuses = new ArrayList<>();
        for (BonusType bonus : enabledBonuses) {
            if (bonus != ENERGY_BALLS && bonus != EXPLOSION_BALLS && bonus != LEVEL_PASS && bonus != TRICKSTER && bonus != BAD_LUCK) {
                regularBonuses.add(bonus);
            }
        }
        
        if (regularBonuses.isEmpty()) {
            // Если нет обычных бонусов, возвращаем любой доступный
            return enabledBonuses.get((int) (Math.random() * enabledBonuses.size()));
        }
        
        return regularBonuses.get((int) (Math.random() * regularBonuses.size()));
    }
    
    /**
     * Получить случайный бонус для активации (исключая RANDOM_BONUS)
     */
    public static BonusType getRandomBonusForActivation() {
        List<BonusType> enabledBonuses = getEnabledBonuses();

        // Исключаем RANDOM_BONUS из случайного выбора для активации
        enabledBonuses.remove(RANDOM_BONUS);
        enabledBonuses.remove(LEVEL_PASS);
        enabledBonuses.remove(TRICKSTER);
        enabledBonuses.remove(BAD_LUCK);

        if (enabledBonuses.isEmpty()) {
            // Если нет доступных бонусов, возвращаем базовые бонусы
            List<BonusType> fallbackBonuses = new ArrayList<>();
            fallbackBonuses.add(BONUS_SCORE);
            fallbackBonuses.add(EXTRA_LIFE);
            fallbackBonuses.add(BONUS_BALL);
            fallbackBonuses.add(STICKY_PADDLE);
            fallbackBonuses.add(ENERGY_BALLS);
            fallbackBonuses.add(PLASMA_WEAPON);
            
            return fallbackBonuses.get((int) (Math.random() * fallbackBonuses.size()));
        }
        
        return enabledBonuses.get((int) (Math.random() * enabledBonuses.size()));
    }
    
    /**
     * Получить случайный позитивный бонус (только включенные, исключая RANDOM_BONUS)
     */
    public static BonusType getRandomPositiveBonus() {
        List<BonusType> enabledPositiveBonuses = getEnabledPositiveBonuses();
        // Исключаем RANDOM_BONUS из случайного выбора
        enabledPositiveBonuses.remove(RANDOM_BONUS);
        enabledPositiveBonuses.remove(LEVEL_PASS);
        enabledPositiveBonuses.remove(TRICKSTER);
        if (enabledPositiveBonuses.isEmpty()) {
            return BONUS_SCORE; // Fallback
        }
        return enabledPositiveBonuses.get((int) (Math.random() * enabledPositiveBonuses.size()));
    }
    
    /**
     * Получить случайный негативный бонус (только включенные)
     */
    public static BonusType getRandomNegativeBonus() {
        List<BonusType> enabledNegativeBonuses = getEnabledNegativeBonuses();
        if (enabledNegativeBonuses.isEmpty()) {
            return CHAOTIC_BALLS; // Fallback
        }
        return enabledNegativeBonuses.get((int) (Math.random() * enabledNegativeBonuses.size()));
    }
    
    /**
     * КРИТИЧНО: Получить бонус для хардкорной сложности
     * Негативные бонусы падают в 2 раза чаще чем позитивные (66.67% vs 33.33%)
     * Метод не создает долгоживущих объектов - предотвращает утечки памяти
     */
    public static BonusType getWeightedRandomBonusForHardcore() {
        // КРИТИЧНО: Все списки локальные, будут удалены GC после выхода из метода
        List<BonusType> enabledPositiveBonuses = getEnabledPositiveBonuses();
        List<BonusType> enabledNegativeBonuses = getEnabledNegativeBonuses();
        
        // Исключаем специальные бонусы из обычного выбора
        enabledPositiveBonuses.remove(RANDOM_BONUS);
        enabledPositiveBonuses.remove(LEVEL_PASS);
        enabledPositiveBonuses.remove(TRICKSTER);
        enabledPositiveBonuses.remove(BAD_LUCK);
        
        // Fallback на случай если бонусы отключены
        if (enabledNegativeBonuses.isEmpty() && enabledPositiveBonuses.isEmpty()) {
            return BONUS_SCORE;
        }
        if (enabledNegativeBonuses.isEmpty()) {
            return getWeightedRandomBonus(); // Если нет негативных, используем обычную логику
        }
        if (enabledPositiveBonuses.isEmpty()) {
            return enabledNegativeBonuses.get((int) (Math.random() * enabledNegativeBonuses.size()));
        }
        
        // 66.67% шанс на негативный бонус, 33.33% на позитивный
        double random = Math.random();
        if (random < 0.6667) {
            // Негативный бонус (2/3 шанса)
            return enabledNegativeBonuses.get((int) (Math.random() * enabledNegativeBonuses.size()));
        } else {
            // Позитивный бонус (1/3 шанса)
            return enabledPositiveBonuses.get((int) (Math.random() * enabledPositiveBonuses.size()));
        }
    }
    
    /**
     * Получить случайный бонус с весовыми коэффициентами для легкой сложности
     * На легкой сложности позитивные бонусы падают в 2 раза чаще
     * 80% шанс на позитивный бонус, 20% на негативный
     */
    public static BonusType getWeightedRandomBonusForEasy() {
        // КРИТИЧНО: Все списки локальные, будут удалены GC после выхода из метода
        List<BonusType> enabledPositiveBonuses = getEnabledPositiveBonuses();
        List<BonusType> enabledNegativeBonuses = getEnabledNegativeBonuses();
        
        // Исключаем специальные бонусы из обычного выбора
        enabledPositiveBonuses.remove(RANDOM_BONUS);
        enabledPositiveBonuses.remove(LEVEL_PASS);
        enabledPositiveBonuses.remove(TRICKSTER);
        enabledPositiveBonuses.remove(BAD_LUCK);
        
        // Fallback на случай если бонусы отключены
        if (enabledPositiveBonuses.isEmpty() && enabledNegativeBonuses.isEmpty()) {
            return BONUS_SCORE;
        }
        if (enabledPositiveBonuses.isEmpty()) {
            return getWeightedRandomBonus(); // Если нет позитивных, используем обычную логику
        }
        if (enabledNegativeBonuses.isEmpty()) {
            return enabledPositiveBonuses.get((int) (Math.random() * enabledPositiveBonuses.size()));
        }
        
        // 80% шанс на позитивный бонус, 20% на негативный
        double random = Math.random();
        if (random < 0.8) {
            // Позитивный бонус (4/5 шанса)
            return enabledPositiveBonuses.get((int) (Math.random() * enabledPositiveBonuses.size()));
        } else {
            // Негативный бонус (1/5 шанса)
            return enabledNegativeBonuses.get((int) (Math.random() * enabledNegativeBonuses.size()));
        }
    }
    
    /**
     * Получить все включенные бонусы согласно BonusConfig
     */
    public static List<BonusType> getEnabledBonuses() {
        List<BonusType> enabledBonuses = new ArrayList<>();
        for (BonusType bonus : values()) {
            String key = bonus.name().toLowerCase();
            boolean isEnabled = BonusConfig.isBonusEnabled(key);
            if (isEnabled) {
                enabledBonuses.add(bonus);
            }
        }
        return enabledBonuses;
    }
    
    /**
     * Получить все включенные позитивные бонусы
     */
    public static List<BonusType> getEnabledPositiveBonuses() {
        List<BonusType> enabledBonuses = new ArrayList<>();
        for (BonusType bonus : values()) {
            String key = bonus.name().toLowerCase();
            // Приводим новые бонусы очков к основному флагу bonus_score
            if (key.equals("bonus_score_200") || key.equals("bonus_score_500")) {
                key = "bonus_score";
            }
            if (bonus.isPositive() && BonusConfig.isBonusEnabled(key)) {
                enabledBonuses.add(bonus);
            }
        }
        return enabledBonuses;
    }
    
    /**
     * Получить все включенные негативные бонусы
     */
    public static List<BonusType> getEnabledNegativeBonuses() {
        List<BonusType> enabledBonuses = new ArrayList<>();
        for (BonusType bonus : values()) {
            if (bonus.isNegative() && BonusConfig.isBonusEnabled(bonus.name().toLowerCase())) {
                enabledBonuses.add(bonus);
            }
        }
        return enabledBonuses;
    }
    
    /**
     * Получить количество включенных бонусов
     */
    public static int getEnabledBonusesCount() {
        return getEnabledBonuses().size();
    }
    
    /**
     * Получить количество включенных позитивных бонусов
     */
    public static int getEnabledPositiveBonusesCount() {
        return getEnabledPositiveBonuses().size();
    }
    
    /**
     * Получить количество включенных негативных бонусов
     */
    public static int getEnabledNegativeBonusesCount() {
        return getEnabledNegativeBonuses().size();
    }
}
