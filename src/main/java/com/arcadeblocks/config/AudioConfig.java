package com.arcadeblocks.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация аудио системы
 */
public class AudioConfig {
    
    public static final String DEFAULT_LEVEL_LOADING_SOUND = "sounds/level_loading.wav";
    public static final String DEFAULT_BRICK_HIT_SOUND = "sounds/brick_break.wav";
    public static final String DEFAULT_LEVEL_COMPLETE_SOUND = "sounds/level_complete.wav";
    private static final List<String> DEFAULT_BRICK_HIT_SOUNDS = List.of(
        "sounds/brick_break.wav",
        "sounds/brick_break2.wav",
        "sounds/brick_break3.wav",
        "sounds/brick_break4.wav",
        "sounds/brick_break.wav"
    );
    private static final List<String> DEFAULT_LEVEL_COMPLETION_SOUNDS = List.of(
        "sounds/level_complete.wav",
        "sounds/level_complete2.wav",
        "sounds/level_complete3.wav",
        "sounds/level_complete4.wav",
        "sounds/level_complete5.wav"
    );
    
    
    // Фоновая музыка для меню
    // ВАЖНО: Не использовать напрямую! Используйте getRandomMainMenuMusic(gameCompleted)
    // После завершения игры эта музыка блокируется и используется только EXTRA музыка
    public static final String MAIN_MENU_MUSIC = "music/main_menu.mp3";
    public static final String MAIN_MENU_MUSIC_2 = "music/main_menu2.mp3";
    public static final String MAIN_MENU_MUSIC_3 = "music/main_menu3.mp3";
    public static final String MAIN_MENU_MUSIC_4 = "music/main_menu_4.mp3";
    public static final String PAUSE_MUSIC = "music/pause.mp3";
    public static final String VICTORY_MUSIC = "music/victory.mp3";
    public static final String GAME_OVER_MUSIC = "music/game_over.mp3";
    public static final String CREDITS_MUSIC = "music/credits.mp3";
    public static final String WELCOME_SOUND = "sounds/sfx/welcomesound.mp3";
    public static final String WELCOME_SOUND_2 = "sounds/welcomesound2.wav";
    public static final String WELCOME_SOUND_3 = "sounds/welcomesound3.wav";
    public static final String WELCOME_SOUND_AFTER_LEVEL100_1 = "sounds/welcomesound1_after_level100.wav";
    public static final String WELCOME_SOUND_AFTER_LEVEL100_2 = "sounds/welcomesound2_after_level100.wav";
    public static final String WELCOME_SOUND_COMPLETED_1 = "sounds/welcomesound1_completed_game.wav";
    public static final String WELCOME_SOUND_COMPLETED_2 = "sounds/welcomesound2_completed_game.wav";
    public static final String LOADING_SOUND = "sounds/loading_sound.wav";
    
    // Фоновые изображения для главного меню
    // ВАЖНО: Не использовать напрямую! Используйте getRandomMainMenuBackground(gameCompleted)
    // После завершения игры эти фоны блокируются и используются только EXTRA фоны
    public static final String MAIN_MENU_BACKGROUND = "background.png";
    public static final String MAIN_MENU_BACKGROUND_2 = "background2.png";
    public static final String MAIN_MENU_BACKGROUND_3 = "background3.png";
    
    // Фоновые изображения для экрана загрузки
    public static final String LOADING_BACKGROUND = "loading_background.png";
    public static final String LOADING_BACKGROUND_2 = "loading_background2.png";
    public static final String LOADING_BACKGROUND_3 = "loading_background3.png";
    
    // Музыка для уровней
    public static final String LEVEL0_MUSIC = "music/level0.mp3";
    public static final String LEVEL1_MUSIC = "music/level1.mp3";
    
    // Звуковые эффекты
    public static final Map<String, String> SFX = new HashMap<>() {{
        // Основные звуки
        put("paddle_hit", "sounds/sfx/paddle_hit.wav");
        put("brick_break", "sounds/sfx/brick_break.wav");
        put("wall_bounce", "sounds/sfx/wall_bounce.wav");
        put("level_complete", "sounds/sfx/level_complete.wav");
        put("game_over", "sounds/sfx/game_over.wav");
        put("life_lost", "sounds/sfx/life_lost.wav");
        put("ball_call", "sounds/call_ball_paddle.wav");
        put("call_to_paddle_block", "sounds/call_to_paddle_block.wav");
        
        
        // Бонусы - старые звуки (закомментированы)
        // put("powerup_good", "sounds/sfx/powerup_good.wav");
        // put("powerup_bad", "sounds/sfx/powerup_bad.wav");
        // put("powerup_pickup", "sounds/sfx/powerup_pickup.wav");
        
        // Новые звуки бонусов
        put("bonus_score", "sounds/bonus_sounds/extra_score.wav");
        put("extra_life", "sounds/bonus_sounds/extra_life.wav");
        put("increase_paddle", "sounds/bonus_sounds/increase_paddle.wav");
        put("sticky_paddle", "sounds/bonus_sounds/sticky_paddle.wav");
        put("slow_balls", "sounds/bonus_sounds/slow_balls.wav");
        put("energy_balls", "sounds/bonus_sounds/energy_balls.wav");
        put("bonus_wall", "sounds/bonus_sounds/bonus_wall.wav");
        put("bonus_magnet", "sounds/bonus_sounds/magnet_bonuses.wav");
        put("bonus_ball", "sounds/bonus_sounds/extra_ball.wav");
        put("plasma_weapon", "sounds/bonus_sounds/plasma_weapon.wav");
        put("explosion_balls", "sounds/bonus_sounds/explosion_ball.wav");
        put("plasma_weapon_recharge", "sounds/recharge_plasma_weapon.wav");
        put("reset", "sounds/reset.wav");
        put("trickster", "sounds/trickster.wav");
        put("bad_luck", "sounds/bad_luck.wav");
        put("score_rain", "sounds/score_rain.wav");
        
        // Негативные бонусы
        put("chaotic_balls", "sounds/bonus_sounds/chaotic_balls.wav");
        put("frozen_paddle", "sounds/bonus_sounds/frozen_paddle.wav");
        put("decrease_paddle", "sounds/bonus_sounds/decrease_paddle.wav");
        put("fast_balls", "sounds/bonus_sounds/fast_balls.wav");
        put("penalties_magnet", "sounds/bonus_sounds/penalty_magnet.wav");
        put("weak_balls", "sounds/bonus_sounds/weak_balls.wav");
        put("invisible_paddle", "sounds/bonus_sounds/ghost_paddle.wav");
        put("darkness", "sounds/bonus_sounds/darkness.wav");
        put("random_bonus", "sounds/bonus_sounds/random.wav");
        
        // Особые эффекты
        put("explosion", "sounds/sfx/explosion.wav");
        put("laser_shot", "sounds/sfx/laser_shot.wav");
        put("ball_split", "sounds/sfx/ball_split.wav");
        put("sticky_paddle", "sounds/sfx/sticky_paddle.wav");
        
        // UI звуки
        put("menu_select", "sounds/sfx/menu_select.wav");
        put("menu_hover", "sounds/sfx/menu_hover.wav");
        put("menu_back", "sounds/sfx/menu_back.wav");
        put("settings_change", "sounds/sfx/settings_change.wav");
        put("welcome_sound", "sounds/sfx/welcomesound.mp3");
        put("welcome_sound_2", "sounds/welcomesound2.wav");
        put("welcome_sound_3", "sounds/welcomesound3.wav");
        put("welcome_sound_after_level100_1", "sounds/welcomesound1_after_level100.wav");
        put("welcome_sound_after_level100_2", "sounds/welcomesound2_after_level100.wav");
        put("welcome_sound_completed_1", "sounds/welcomesound1_completed_game.wav");
        put("welcome_sound_completed_2", "sounds/welcomesound2_completed_game.wav");
        put("loading_sound", "sounds/loading_sound.wav");
        put("level_loading", "sounds/level_loading.wav");
        
        // Специальные звуки
        put("magnet_active", "sounds/sfx/magnet_active.wav");
    }};
    
    // Специальные звуки для уровней
    public static final Map<Integer, String> LEVEL_LOADING_SOUNDS = new HashMap<>() {{
        put(32, "sounds/level_loading32.wav");
        put(33, "sounds/level_loading33.wav");
        put(34, "sounds/level_loading34.wav");
        put(35, "sounds/level_loading35.wav");
        put(36, "sounds/level_loading36.wav");
        put(37, "sounds/level_loading37.wav");
        put(38, "sounds/level_loading38.wav");
        put(39, "sounds/level_loading39.wav");
        put(41, "sounds/level_loading41.wav");
        put(42, "sounds/level_loading42.wav");
        put(43, "sounds/level_loading43.wav");
        put(44, "sounds/level_loading44.wav");
        put(45, "sounds/level_loading45.wav");
        put(46, "sounds/level_loading46.wav");
        put(47, "sounds/level_loading47.wav");
        put(48, "sounds/level_loading48.wav");
        put(49, "sounds/level_loading49.wav");
        put(51, "sounds/level_loading51.wav");
        put(52, "sounds/level_loading52.wav");
        put(53, "sounds/level_loading53.wav");
        put(54, "sounds/level_loading54.wav");
        put(55, "sounds/level_loading55.wav");
        put(56, "sounds/level_loading56.wav");
        put(57, "sounds/level_loading57.wav");
        put(58, "sounds/level_loading58.wav");
        put(59, "sounds/level_loading59.wav");
        put(61, "sounds/level_loading61.wav");
        put(62, "sounds/level_loading62.wav");
        put(63, "sounds/level_loading63.wav");
        put(64, "sounds/level_loading64.wav");
        put(65, "sounds/level_loading65.wav");
        put(66, "sounds/level_loading66.wav");
        put(67, "sounds/level_loading67.wav");
        put(68, "sounds/level_loading68.wav");
        put(69, "sounds/level_loading69.wav");
        put(71, "sounds/level_loading71.wav");
        put(72, "sounds/level_loading72.wav");
        put(73, "sounds/level_loading73.wav");
        put(74, "sounds/level_loading74.wav");
        put(75, "sounds/level_loading75.wav");
        put(76, "sounds/level_loading76.wav");
        put(77, "sounds/level_loading77.wav");
        put(78, "sounds/level_loading78.wav");
        put(79, "sounds/level_loading79.wav");
        put(81, "sounds/level_loading81.wav");
        put(82, "sounds/level_loading82.wav");
        put(83, "sounds/level_loading83.wav");
        put(84, "sounds/level_loading84.wav");
        put(85, "sounds/level_loading85.wav");
        put(86, "sounds/level_loading86.wav");
        put(87, "sounds/level_loading87.wav");
        put(88, "sounds/level_loading88.wav");
        put(89, "sounds/level_loading89.wav");
        put(91, "sounds/level_loading91.wav");
        put(92, "sounds/level_loading92.wav");
        put(93, "sounds/level_loading93.wav");
        put(94, "sounds/level_loading94.wav");
        put(95, "sounds/level_loading95.wav");
        put(96, "sounds/level_loading96.wav");
        put(97, "sounds/level_loading97.wav");
        put(98, "sounds/level_loading98.wav");
        put(99, "sounds/level_loading99.wav");
        // Глава XII: Восстановление Системы (уровни 101-116)
        put(102, "sounds/level_loading102.wav");
        put(103, "sounds/level_loading103.wav");
        put(104, "sounds/level_loading104.wav");
        put(105, "sounds/level_loading105.wav");
        put(106, "sounds/level_loading106.wav");
        put(107, "sounds/level_loading107.wav");
        put(108, "sounds/level_loading108.wav");
        put(109, "sounds/level_loading109.wav");
        put(110, "sounds/level_loading110.wav");
        put(111, "sounds/level_loading111.wav");
        put(112, "sounds/level_loading112.wav");
        put(113, "sounds/level_loading113.wav");
        put(116, "sounds/level_loading116.wav");
    }};
    
    public static final Map<Integer, List<String>> LEVEL_BRICK_HIT_SOUNDS = new HashMap<>() {{
        // Особые уровни Главы XII (вместо боссов)
        put(114, List.of(
            "sounds/boss11_hit1.wav",
            "sounds/boss11_hit2.wav",
            "sounds/boss11_hit3.wav",
            "sounds/boss11_hit4.wav"
        ));
        put(115, List.of(
            "sounds/boss12_hit1.wav",
            "sounds/boss12_hit2.wav",
            "sounds/boss12_hit3.wav",
            "sounds/boss12_hit4.wav"
        ));
    }};
    
    public static final Map<Integer, List<String>> LEVEL_COMPLETION_SOUNDS = new HashMap<>() {{
        // Boss completion sounds removed
    }};
    
    // Фоновые изображения для уровней
    public static final Map<Integer, String> LEVEL_BACKGROUNDS = new HashMap<>() {{
        put(0, "easter_egg.png");
        put(1, "level1.jpg");
        put(2, "images/bg_level2.png");
        put(3, "images/bg_level3.png");
        put(4, "images/bg_level4.png");
        put(5, "images/bg_level5.png");
        put(6, "images/bg_level6.png");
        put(7, "images/bg_level7.png");
        put(8, "images/bg_level8.png");
        put(9, "images/bg_level9.png");
        put(10, "images/bg_boss1.png");
        put(11, "images/bg_level11.png");
        put(12, "images/bg_level12.png");
        put(13, "images/bg_level13.png");
        put(14, "images/bg_level14.png");
        put(15, "images/bg_level15.png");
        put(16, "images/bg_level16.png");
        put(17, "images/bg_level17.png");
        put(18, "images/bg_level18.png");
        put(19, "images/bg_level19.png");
        put(20, "images/bg_boss2.png");
        put(21, "images/bg_level21.png");
        put(22, "images/bg_level22.png");
        put(23, "images/bg_level23.png");
        put(24, "images/bg_level24.png");
        put(25, "images/bg_level25.png");
        put(26, "images/bg_level26.png");
        put(27, "images/bg_level27.png");
        put(28, "images/bg_level28.png");
        put(29, "images/bg_level29.png");
        put(30, "images/bg_boss3.png");
        put(31, "easter_egg.png");
        put(32, "level32.png");
        put(33, "level33.png");
        put(34, "level34.png");
        put(35, "level35.png");
        put(36, "level36.png");
        put(37, "level37.png");
        put(38, "level38.png");
        put(39, "level39.png");
        put(40, "boss_background4.png");
        put(41, "level41.png");
        put(42, "level42.png");
        put(43, "level43.png");
        put(44, "level44.png");
        put(45, "level45.png");
        put(46, "level46.png");
        put(47, "level47.png");
        put(48, "level48.png");
        put(49, "level49.png");
        put(50, "boss_background5.png");
        put(51, "level51.png");
        put(52, "level52.png");
        put(53, "level53.png");
        put(54, "level54.png");
        put(55, "level55.png");
        put(56, "level56.png");
        put(57, "level57.png");
        put(58, "level58.png");
        put(59, "level59.png");
        put(60, "boss_background6.png");
        put(61, "level61.png");
        put(62, "level62.png");
        put(63, "level63.png");
        put(64, "level64.png");
        put(65, "level65.png");
        put(66, "level66.png");
        put(67, "level67.png");
        put(68, "level68.png");
        put(69, "level69.png");
        put(70, "boss_background7.png");
        put(71, "level71.png");
        put(72, "level72.png");
        put(73, "level73.png");
        put(74, "level74.png");
        put(75, "level75.png");
        put(76, "level76.png");
        put(77, "level77.png");
        put(78, "level78.png");
        put(79, "level79.png");
        put(80, "boss_background8.png");
        put(81, "level81.png");
        put(82, "level82.png");
        put(83, "level83.png");
        put(84, "level84.png");
        put(85, "level85.png");
        put(86, "level86.png");
        put(87, "level87.png");
        put(88, "level88.png");
        put(89, "level89.png");
        put(90, "boss_background9.png");
        put(91, "level91.png");
        put(92, "level92.png");
        put(93, "level93.png");
        put(94, "level94.png");
        put(95, "level95.png");
        put(96, "level96.png");
        put(97, "level97.png");
        put(98, "level98.png");
        put(99, "level99.png");
        put(100, "boss_background10.png");
        // Глава XII: Восстановление Системы (уровни 101-116)
        put(101, "level101.png");
        put(102, "level102.png");
        put(103, "level103.png");
        put(104, "level104.png");
        put(105, "level105.png");
        put(106, "level106.png");
        put(107, "level107.png");
        put(108, "level108.png");
        put(109, "level109.png");
        put(110, "level110.png");
        put(111, "level111.png");
        put(112, "level112.png");
        put(113, "level113.png");
        put(114, "boss_background11.png");
        put(115, "boss_background12.png");
        put(116, "level116.png");
    }};
    
    // Изображения боссов
    public static final Map<Integer, String> BOSS_IMAGES = new HashMap<>() {{
        put(10, "images/firewall7.png");
        put(20, "images/neon_architect.png");
        put(30, "images/monolith_exe.png");
    }};
    
    
    /**
     * Получить случайную музыку главного меню (25% шанс на каждую)
     */
    public static String getRandomMainMenuMusic() {
        double random = Math.random();
        if (random < 0.25) {
            return MAIN_MENU_MUSIC;
        } else if (random < 0.5) {
            return MAIN_MENU_MUSIC_2;
        } else if (random < 0.75) {
            return MAIN_MENU_MUSIC_3;
        } else {
            return MAIN_MENU_MUSIC_4;
        }
    }
    
    /**
     * Получить случайный приветственный звук в зависимости от прогресса игры
     * @param progressState состояние прогресса игры
     * @return имя звука для использования в SFX map
     */
    public static String getRandomWelcomeSound(GameProgressState progressState) {
        switch (progressState) {
            case COMPLETED:
                // Для пройденной игры используем специальные звуки (50/50)
                return Math.random() < 0.5 ? "welcome_sound_completed_1" : "welcome_sound_completed_2";
            case AFTER_LEVEL_100:
                // После уровня 100 используем промежуточные звуки (50/50)
                return Math.random() < 0.5 ? "welcome_sound_after_level100_1" : "welcome_sound_after_level100_2";
            case NORMAL:
            default:
                // Для обычной игры используем старые звуки (33% шанс на каждый)
                double random = Math.random();
                if (random < 0.33) {
                    return "welcome_sound";
                } else if (random < 0.66) {
                    return "welcome_sound_2";
                } else {
                    return "welcome_sound_3";
                }
        }
    }
    
    /**
     * Получить случайный приветственный звук (устаревший метод)
     * @param gameCompleted true если игра завершена (пройден уровень 116)
     * @return имя звука для использования в SFX map
     * @deprecated Используйте getRandomWelcomeSound(GameProgressState)
     */
    @Deprecated
    public static String getRandomWelcomeSound(boolean gameCompleted) {
        return getRandomWelcomeSound(gameCompleted ? GameProgressState.COMPLETED : GameProgressState.NORMAL);
    }
    
    /**
     * Получить случайный приветственный звук (устаревший метод, использует getRandomWelcomeSound(false))
     * @deprecated Используйте getRandomWelcomeSound(GameProgressState)
     */
    @Deprecated
    public static String getRandomWelcomeSound() {
        return getRandomWelcomeSound(GameProgressState.NORMAL);
    }
    
    /**
     * Получить случайный фон главного меню (равновероятно из 3 вариантов)
     */
    public static String getRandomMainMenuBackground() {
        double random = Math.random();
        if (random < 0.3333) {
            return MAIN_MENU_BACKGROUND;
        } else if (random < 0.6666) {
            return MAIN_MENU_BACKGROUND_2;
        } else {
            return MAIN_MENU_BACKGROUND_3;
        }
    }
    
    /**
     * Получить случайный фон экрана загрузки (25% шанс на каждый)
     */
    public static String getRandomLoadingBackground() {
        double random = Math.random();
        if (random < 0.25) {
            return LOADING_BACKGROUND;
        } else if (random < 0.5) {
            return LOADING_BACKGROUND_2;
        } else if (random < 0.75) {
            return LOADING_BACKGROUND_3;
        } else {
            // Для оставшихся 25% возвращаем первый фон как fallback
            return LOADING_BACKGROUND;
        }
    }
    
    /**
     * Получить случайный звук потери жизни (25% шанс на каждый)
     */
    public static String getRandomLifeLostSound() {
        double random = Math.random();
        if (random < 0.25) {
            return "sounds/life_lost.wav";
        } else if (random < 0.5) {
            return "sounds/life_lost2.wav";
        } else if (random < 0.75) {
            return "sounds/life_lost3.wav";
        } else {
            return "sounds/life_lost4.wav";
        }
    }
    
    /**
     * Получить звуковой эффект
     */
    public static String getSFX(String effectName) {
        return SFX.get(effectName);
    }
    
    /**
     * Получить фоновое изображение для уровня
     */
    public static String getLevelBackground(int levelNumber) {
        return LEVEL_BACKGROUNDS.get(levelNumber);
    }
    
    /**
     * Получить звук загрузки уровня
     */
    public static String getLevelLoadingSound(int levelNumber) {
        if (LEVEL_LOADING_SOUNDS.containsKey(levelNumber)) {
            return LEVEL_LOADING_SOUNDS.get(levelNumber);
        }

        String candidate = String.format("sounds/level%d_loading.wav", levelNumber);
        if (AudioConfig.class.getResource("/assets/" + candidate) != null) {
            return candidate;
        }

        return DEFAULT_LEVEL_LOADING_SOUND;
    }
    
    /**
     * Получить звуки попадания по кирпичам
     */
    public static List<String> getBrickHitSounds(int levelNumber) {
        return LEVEL_BRICK_HIT_SOUNDS.getOrDefault(levelNumber, DEFAULT_BRICK_HIT_SOUNDS);
    }
    
    /**
     * Получить звук завершения уровня
     */
    public static List<String> getLevelCompletionSounds(int levelNumber) {
        return LEVEL_COMPLETION_SOUNDS.getOrDefault(levelNumber, DEFAULT_LEVEL_COMPLETION_SOUNDS);
    }
    
    /**
     * Получить дефолтные звуки попаданий по кирпичам
     */
    public static List<String> getDefaultBrickHitSounds() {
        return DEFAULT_BRICK_HIT_SOUNDS;
    }

    public static List<String> getDefaultLevelCompletionSounds() {
        return DEFAULT_LEVEL_COMPLETION_SOUNDS;
    }
    
    /**
     * Получить изображение босса
     */
    public static String getBossImage(int levelNumber) {
        return BOSS_IMAGES.get(levelNumber);
    }
    
    // Настройки громкости по умолчанию
    public static final double DEFAULT_MASTER_VOLUME = 0.7;
    public static final double DEFAULT_MUSIC_VOLUME = 0.6;
    public static final double DEFAULT_SFX_VOLUME = 0.8;
    
    // Настройки воспроизведения
    public static final boolean DEFAULT_LOOP_MUSIC = true;
    public static final boolean DEFAULT_LOOP_SFX = false;
    
    // Настройки качества аудио (320 kbps эквивалент)
    public static final int HIGH_QUALITY_FREQUENCY = 48000;  // 48kHz для высокого качества
    public static final int HIGH_QUALITY_CHANNELS = 2;       // Стерео
    public static final int HIGH_QUALITY_BUFFER_SIZE = 2048;  // Размер буфера для стабильности
    public static final int HIGH_QUALITY_MAX_CHANNELS = 256;  // Максимальное количество каналов (увеличено для избежания "No free channels")
    
    // Специальные фоны главного меню после уровня 100 (но до завершения игры)
    public static final String MAIN_MENU_AFTER_LEVEL100_BACKGROUND_1 = "textures/main_menu1_background_after_level100.png";
    public static final String MAIN_MENU_AFTER_LEVEL100_BACKGROUND_2 = "textures/main_menu2_background_after_level100.png";
    
    // Специальные фоны главного меню после завершения игры
    public static final String MAIN_MENU_EXTRA_BACKGROUND_1 = "main_menu_extra_background1.png";
    public static final String MAIN_MENU_EXTRA_BACKGROUND_2 = "main_menu_extra_background2.png";
    
    // Специальная музыка главного меню после уровня 100 (но до завершения игры)
    public static final String MAIN_MENU_AFTER_LEVEL100_MUSIC_1 = "music/main_menu1_after_level100.mp3";
    public static final String MAIN_MENU_AFTER_LEVEL100_MUSIC_2 = "music/main_menu2_after_level100.mp3";
    
    // Специальная музыка главного меню после завершения игры
    public static final String MAIN_MENU_EXTRA_MUSIC_1 = "music/main_menu_extra1.mp3";
    public static final String MAIN_MENU_EXTRA_MUSIC_2 = "music/main_menu_extra2.mp3";
    
    // Список обычных фонов главного меню
    private static final List<String> NORMAL_MAIN_MENU_BACKGROUNDS = List.of(
        MAIN_MENU_BACKGROUND,
        MAIN_MENU_BACKGROUND_2,
        MAIN_MENU_BACKGROUND_3
    );
    
    // Список фонов после уровня 100 (но до завершения игры)
    private static final List<String> AFTER_LEVEL100_MAIN_MENU_BACKGROUNDS = List.of(
        MAIN_MENU_AFTER_LEVEL100_BACKGROUND_1,
        MAIN_MENU_AFTER_LEVEL100_BACKGROUND_2
    );
    
    // Список специальных фонов после завершения игры
    private static final List<String> EXTRA_MAIN_MENU_BACKGROUNDS = List.of(
        MAIN_MENU_EXTRA_BACKGROUND_1,
        MAIN_MENU_EXTRA_BACKGROUND_2
    );
    
    // Список обычной музыки главного меню
    private static final List<String> NORMAL_MAIN_MENU_MUSIC = List.of(
        MAIN_MENU_MUSIC,
        MAIN_MENU_MUSIC_2,
        MAIN_MENU_MUSIC_3,
        MAIN_MENU_MUSIC_4
    );
    
    // Список музыки после уровня 100 (но до завершения игры)
    private static final List<String> AFTER_LEVEL100_MAIN_MENU_MUSIC = List.of(
        MAIN_MENU_AFTER_LEVEL100_MUSIC_1,
        MAIN_MENU_AFTER_LEVEL100_MUSIC_2
    );
    
    // Список специальной музыки после завершения игры
    private static final List<String> EXTRA_MAIN_MENU_MUSIC = List.of(
        MAIN_MENU_EXTRA_MUSIC_1,
        MAIN_MENU_EXTRA_MUSIC_2
    );
    
    private static final java.util.Random random = new java.util.Random();
    
    /**
     * Состояние прогресса игры для выбора фонов и музыки
     */
    public enum GameProgressState {
        NORMAL,           // Обычная игра (до уровня 101)
        AFTER_LEVEL_100,  // После прохождения уровня 100 (уровень 101+, но игра не завершена)
        COMPLETED         // Игра полностью завершена (пройден уровень 116)
    }
    
    /**
     * Получить случайный фон главного меню в зависимости от прогресса игры
     * @param progressState состояние прогресса игры
     * @return путь к файлу фона
     */
    public static String getRandomMainMenuBackground(GameProgressState progressState) {
        List<String> backgrounds;
        switch (progressState) {
            case COMPLETED:
                // После завершения игры используются только специальные фоны
                backgrounds = EXTRA_MAIN_MENU_BACKGROUNDS;
                break;
            case AFTER_LEVEL_100:
                // После уровня 100 используются промежуточные фоны
                backgrounds = AFTER_LEVEL100_MAIN_MENU_BACKGROUNDS;
                break;
            case NORMAL:
            default:
                // Обычные фоны для начала игры
                backgrounds = NORMAL_MAIN_MENU_BACKGROUNDS;
                break;
        }
        return backgrounds.get(random.nextInt(backgrounds.size()));
    }
    
    /**
     * Получить случайный фон главного меню (устаревший метод)
     * @param gameCompleted true если игра завершена (пройден уровень 116)
     * @return путь к файлу фона
     * @deprecated Используйте getRandomMainMenuBackground(GameProgressState)
     */
    @Deprecated
    public static String getRandomMainMenuBackground(boolean gameCompleted) {
        return getRandomMainMenuBackground(gameCompleted ? GameProgressState.COMPLETED : GameProgressState.NORMAL);
    }
    
    /**
     * Получить случайную музыку главного меню в зависимости от прогресса игры
     * @param progressState состояние прогресса игры
     * @return путь к файлу музыки
     */
    public static String getRandomMainMenuMusic(GameProgressState progressState) {
        List<String> musicList;
        switch (progressState) {
            case COMPLETED:
                // После завершения игры используется только специальная музыка
                musicList = EXTRA_MAIN_MENU_MUSIC;
                break;
            case AFTER_LEVEL_100:
                // После уровня 100 используется промежуточная музыка
                musicList = AFTER_LEVEL100_MAIN_MENU_MUSIC;
                break;
            case NORMAL:
            default:
                // Обычная музыка для начала игры
                musicList = NORMAL_MAIN_MENU_MUSIC;
                break;
        }
        return musicList.get(random.nextInt(musicList.size()));
    }
    
    /**
     * Получить случайную музыку главного меню (устаревший метод)
     * @param gameCompleted true если игра завершена (пройден уровень 116)
     * @return путь к файлу музыки
     * @deprecated Используйте getRandomMainMenuMusic(GameProgressState)
     */
    @Deprecated
    public static String getRandomMainMenuMusic(boolean gameCompleted) {
        return getRandomMainMenuMusic(gameCompleted ? GameProgressState.COMPLETED : GameProgressState.NORMAL);
    }
    
    /**
     * Проверить, является ли фон старым (заблокированным после завершения игры)
     * @param backgroundPath путь к фону
     * @return true если это старый фон
     */
    public static boolean isOldMainMenuBackground(String backgroundPath) {
        return NORMAL_MAIN_MENU_BACKGROUNDS.contains(backgroundPath);
    }
    
    /**
     * Проверить, является ли музыка старой (заблокированной после завершения игры)
     * @param musicPath путь к музыке
     * @return true если это старая музыка
     */
    public static boolean isOldMainMenuMusic(String musicPath) {
        return NORMAL_MAIN_MENU_MUSIC.contains(musicPath);
    }
    
    // Форматы аудио для высокого качества
    public static final String AUDIO_FORMAT_16BIT = "16-bit";
    public static final String AUDIO_FORMAT_32BIT = "32-bit";
    public static final String AUDIO_FORMAT_FLOAT = "32-bit float";
}
