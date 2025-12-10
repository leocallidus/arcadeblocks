package com.arcadeblocks.config;

import com.arcadeblocks.localization.LocalizationManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация бонусных уровней (Arcade Blocks Bonus)
 */
public class BonusLevelConfig {

    // Неоновый Малиновый для бонусной главы
    public static final String BONUS_CHAPTER_COLOR = "#DC143C";

    public static class BonusChapter {
        private final int chapterNumber;
        private final String romanNumeral;
        private final String titleKey;
        private final String defaultTitle;
        private final int startLevel;
        private final int endLevel;
        private final String accentColorHex;

        public BonusChapter(int chapterNumber, String romanNumeral, String titleKey, String defaultTitle,
                           int startLevel, int endLevel, String accentColorHex) {
            this.chapterNumber = chapterNumber;
            this.romanNumeral = romanNumeral;
            this.titleKey = titleKey;
            this.defaultTitle = defaultTitle;
            this.startLevel = startLevel;
            this.endLevel = endLevel;
            this.accentColorHex = accentColorHex;
        }

        public int getChapterNumber() { return chapterNumber; }
        public String getRomanNumeral() { return romanNumeral; }
        public String getTitle() { return LocalizationManager.getInstance().getOrDefault(titleKey, defaultTitle); }
        public int getStartLevel() { return startLevel; }
        public int getEndLevel() { return endLevel; }
        public String getAccentColorHex() { return accentColorHex; }
        public boolean containsLevel(int levelNumber) { return levelNumber >= startLevel && levelNumber <= endLevel; }
    }

    private static final List<BonusChapter> BONUS_CHAPTERS = List.of(
        new BonusChapter(1, "I", "bonus.chapter.title.1", "Симфония Хаоса", 1001, 1021, BONUS_CHAPTER_COLOR)
    );

    public static List<BonusChapter> getChapters() { return BONUS_CHAPTERS; }

    public static BonusChapter getChapter(int levelNumber) {
        return BONUS_CHAPTERS.stream()
            .filter(ch -> ch.containsLevel(levelNumber))
            .findFirst()
            .orElse(null);
    }

    public static class BonusLevelData {
        private final int levelNumber;
        private final String nameKey;
        private final String defaultName;
        private final String backgroundImage;
        private final String musicFile;
        private final String initSound;
        private final String initVideo;
        private final boolean bossLevel;
        private final String completionVideo;

        public BonusLevelData(int levelNumber, String nameKey, String defaultName, String backgroundImage,
                             String musicFile, String initSound, String initVideo, boolean bossLevel, String completionVideo) {
            this.levelNumber = levelNumber;
            this.nameKey = nameKey;
            this.defaultName = defaultName;
            this.backgroundImage = backgroundImage;
            this.musicFile = musicFile;
            this.initSound = initSound;
            this.initVideo = initVideo;
            this.bossLevel = bossLevel;
            this.completionVideo = completionVideo;
        }

        public int getLevelNumber() { return levelNumber; }
        public String getName() { return LocalizationManager.getInstance().getOrDefault(nameKey, defaultName); }
        public String getNameKey() { return nameKey; }
        public String getBackgroundImage() { return backgroundImage; }
        public String getMusicFile() {
            if (musicFile == null || musicFile.isBlank()) {
                return null;
            }
            if (musicFile.contains("/")) {
                return musicFile;
            }
            return "music/" + musicFile;
        }
        public String getInitSound() { return initSound; }
        public String getInitVideo() { return initVideo; }
        public boolean isBossLevel() { return bossLevel; }
        public String getCompletionVideo() { return completionVideo; }
    }

    private static final Map<Integer, BonusLevelData> BONUS_LEVELS = new HashMap<>();

    static {
        // Бонусная глава I: Симфония Хаоса (уровни 1001-1021)
        BONUS_LEVELS.put(1001, new BonusLevelData(1001, "bonus.level.name.1", "Кибер-Колизей",
            "level_backgroundbonus1.jpeg", "level_bonus1.mp3", null, "level_bonus_init1.mp4", false, null));
        BONUS_LEVELS.put(1002, new BonusLevelData(1002, "bonus.level.name.2", "Погоня на эстакаде",
            "level_backgroundbonus2.jpeg", "level_bonus2.mp3", "level_bonus_init2.wav", null, false, null));
        BONUS_LEVELS.put(1003, new BonusLevelData(1003, "bonus.level.name.3", "Река Кошмаров",
            "level_backgroundbonus3.jpeg", "level_bonus3.mp3", "level_bonus_init3.wav", null, false, null));
        BONUS_LEVELS.put(1004, new BonusLevelData(1004, "bonus.level.name.4", "Инферно на Районе",
            "level_backgroundbonus4.jpeg", "level_bonus4.mp3", "level_bonus_init4.wav", null, false, null));
        BONUS_LEVELS.put(1005, new BonusLevelData(1005, "bonus.level.name.5", "Эпицентр Тьмы",
            "level_backgroundbonus5.jpeg", "level_bonus5.mp3", "level_bonus_init5.wav", null, false, null));
        BONUS_LEVELS.put(1006, new BonusLevelData(1006, "bonus.level.name.6", "Царство Хаоса",
            "level_backgroundbonus6.jpeg", "level_bonus6.mp3", "level_bonus_init6.wav", null, false, null));
        BONUS_LEVELS.put(1007, new BonusLevelData(1007, "bonus.level.name.7", "Огненная Вечеринка",
            "level_backgroundbonus7.jpeg", "level_bonus7.mp3", "level_bonus_init7.wav", null, false, null));
        BONUS_LEVELS.put(1008, new BonusLevelData(1008, "bonus.level.name.8", "Индустриальный Рейв",
            "level_backgroundbonus8.jpeg", "level_bonus8.mp3", "level_bonus_init8.wav", null, false, null));
        BONUS_LEVELS.put(1009, new BonusLevelData(1009, "bonus.level.name.9", "Тишина Мегаполиса",
            "level_backgroundbonus9.jpeg", "level_bonus9.mp3", "level_bonus_init9.wav", null, false, null));
        // Босс: Железный Каратель
        BONUS_LEVELS.put(1010, new BonusLevelData(1010, "bonus.level.name.10", "Железный Каратель",
            "boss_background13.jpeg", "boss13_music.mp3", null, "boss_init13.mp4", true, "boss_completed13.mp4"));
        BONUS_LEVELS.put(1011, new BonusLevelData(1011, "bonus.level.name.11", "Импульс Города",
            "level_backgroundbonus11.jpeg", "level_bonus11.mp3", "level_bonus_init11.wav", null, false, null));
        BONUS_LEVELS.put(1012, new BonusLevelData(1012, "bonus.level.name.12", "Импульсный Проспект",
            "level_backgroundbonus12.jpeg", "level_bonus12.mp3", "level_bonus_init12.wav", null, false, null));
        BONUS_LEVELS.put(1013, new BonusLevelData(1013, "bonus.level.name.13", "Синтетическая Любовь",
            "level_backgroundbonus13.jpeg", "level_bonus13.mp3", "level_bonus_init13.wav", null, false, null));
        BONUS_LEVELS.put(1014, new BonusLevelData(1014, "bonus.level.name.14", "Короткое Замыкание",
            "level_backgroundbonus14.jpeg", "level_bonus14.mp3", "level_bonus_init14.wav", null, false, null));
        BONUS_LEVELS.put(1015, new BonusLevelData(1015, "bonus.level.name.15", "Ритм Бунта",
            "level_backgroundbonus15.jpeg", "level_bonus15.mp3", "level_bonus_init15.wav", null, false, null));
        BONUS_LEVELS.put(1016, new BonusLevelData(1016, "bonus.level.name.16", "Фатальная Ошибка",
            "level_backgroundbonus16.jpeg", "level_bonus16.mp3", "level_bonus_init16.wav", null, false, null));
        BONUS_LEVELS.put(1017, new BonusLevelData(1017, "bonus.level.name.17", "Министерство Цензуры",
            "level_backgroundbonus17.jpeg", "level_bonus17.mp3", "level_bonus_init17.wav", null, false, null));
        BONUS_LEVELS.put(1018, new BonusLevelData(1018, "bonus.level.name.18", "Столкновение Титанов",
            "level_backgroundbonus18.jpeg", "level_bonus18.mp3", "level_bonus_init18.wav", null, false, null));
        BONUS_LEVELS.put(1019, new BonusLevelData(1019, "bonus.level.name.19", "Долина Монолитов",
            "level_backgroundbonus19.jpeg", "level_bonus19.mp3", "level_bonus_init19.wav", null, false, null));
        // Босс: Сайлас
        BONUS_LEVELS.put(1020, new BonusLevelData(1020, "bonus.level.name.20", "Сайлас (Архитектор Монолитов)",
            "boss_background14.jpeg", "boss14_music.mp3", null, "boss_init14.mp4", true, "boss_completed14.mp4"));
        // Босс: Ледяной Суверен
        BONUS_LEVELS.put(1021, new BonusLevelData(1021, "bonus.level.name.21", "Ледяной Суверен",
            "boss_background15.jpeg", "boss15_music.mp3", null, "boss_init15.mp4", true, "boss_completed15.mp4"));
    }

    public static BonusLevelData getLevelData(int levelNumber) {
        return BONUS_LEVELS.get(levelNumber);
    }

    public static boolean isBonusLevel(int levelNumber) {
        return levelNumber >= 1001 && levelNumber <= 1021;
    }

    public static int getTotalLevels() {
        return 21;
    }
}
