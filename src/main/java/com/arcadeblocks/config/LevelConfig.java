package com.arcadeblocks.config;

import com.arcadeblocks.localization.LocalizationManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Конфигурация уровней игры
 */
public class LevelConfig {
    
    /**
     * Описание главы уровня
     */
    public static class LevelChapter {
        private final int chapterNumber;
        private final String romanNumeral;
        private final String titleKey;
        private final String defaultTitle;
        private final int startLevel;
        private final int endLevel;
        private final String accentColorHex;

        public LevelChapter(int chapterNumber, String romanNumeral, String titleKey, String defaultTitle,
                            int startLevel, int endLevel, String accentColorHex) {
            this.chapterNumber = chapterNumber;
            this.romanNumeral = romanNumeral;
            this.titleKey = titleKey;
            this.defaultTitle = defaultTitle;
            this.startLevel = startLevel;
            this.endLevel = endLevel;
            this.accentColorHex = accentColorHex;
        }

        public int getChapterNumber() {
            return chapterNumber;
        }

        public String getRomanNumeral() {
            return romanNumeral;
        }

        public String getTitle() {
            return LocalizationManager.getInstance().getOrDefault(titleKey, defaultTitle);
        }

        public int getStartLevel() {
            return startLevel;
        }

        public int getEndLevel() {
            return endLevel;
        }

        public String getAccentColorHex() {
            return accentColorHex;
        }

        public boolean containsLevel(int levelNumber) {
            return levelNumber >= startLevel && levelNumber <= endLevel;
        }
    }

    private static final List<LevelChapter> CHAPTERS = List.of(
        new LevelChapter(1, "I", "chapter.title.1", "Сектор \"Генезис\"", 1, 10, "#FF00FF"),  // Neon Violet (Magenta)
        new LevelChapter(2, "II", "chapter.title.2", "Шпили Мегакорпа", 11, 20, "#00FFFF"),  // Neon Cyan (Aqua)
        new LevelChapter(3, "III", "chapter.title.3", "Нексус Даркнета", 21, 30, "#BF00FF"),  // Neon Purple
        new LevelChapter(4, "IV", "chapter.title.4", "Скрытый Сектор", 31, 31, "#00FF00"),  // Neon Lime (Green)
        new LevelChapter(5, "V", "chapter.title.5", "Лабиринты Памяти", 32, 40, "#E040FB"),  // Neon Orchid
        new LevelChapter(6, "VI", "chapter.title.6", "Сердце Сети", 41, 50, "#00D9FF"),  // Neon Blue
        new LevelChapter(7, "VII", "chapter.title.7", "Запретные Зоны", 51, 60, "#FF8C00"),  // Neon Orange (вместо коричневого)
        new LevelChapter(8, "VIII", "chapter.title.8", "Паника Ядра", 61, 70, "#00E5FF"),  // Neon Sky Blue
        new LevelChapter(9, "IX", "chapter.title.9", "Проект \"Тень\"", 71, 80, "#F0F0F0"),  // Neon Silver
        new LevelChapter(10, "X", "chapter.title.10", "Фрактальная Архитектура", 81, 90, "#9D00FF"),  // Neon Indigo
        new LevelChapter(11, "XI", "chapter.title.11", "Точка Невозврата", 91, 100, "#FFFFFF"),  // Neon White
        new LevelChapter(12, "XII", "chapter.title.12", "Восстановление Системы", 101, 116, "#DD00FF")  // Neon Plum
    );

    public static List<LevelChapter> getChapters() {
        return CHAPTERS;
    }

    public static LevelChapter getChapter(int levelNumber) {
        return CHAPTERS.stream()
            .filter(chapter -> chapter.containsLevel(levelNumber))
            .findFirst()
            .orElse(null);
    }

    /**
     * Данные уровня
     */
    public static class LevelData {
        private final String nameKey;
        private final String defaultName;
        private final String levelFile;
        private final String backgroundImage;
        private final String musicFile;
        public LevelData(String nameKey, String defaultName, String levelFile, String backgroundImage, String musicFile) {
            this.nameKey = nameKey;
            this.defaultName = defaultName;
            this.levelFile = levelFile;
            this.backgroundImage = backgroundImage;
            this.musicFile = musicFile;
        }
        
        // Getters
        public String getName() { return LocalizationManager.getInstance().getOrDefault(nameKey, defaultName); }
        public String getNameKey() { return nameKey; }
        public String getLevelFile() { return levelFile; }
        public String getBackgroundImage() { return backgroundImage; }
        public String getMusicFile() { return musicFile; }
    }

    /**
     * Дополнительная метаинформация уровня
     */
    public static class LevelMetadata {
        private final boolean bossLevel;
        private final String completionMessageKey;
        private final String completionMessageDefault;
        private final Double completionPauseSeconds;
        private final String introVideoPath;
        private final Double introVideoDurationSeconds;
        private final String completionVideoPath;
        private final Double completionVideoDurationSeconds;
        private final boolean showPoemAfterVictory;

        private LevelMetadata(Builder builder) {
            this.bossLevel = builder.bossLevel;
            this.completionMessageKey = builder.completionMessageKey;
            this.completionMessageDefault = builder.completionMessageDefault;
            this.completionPauseSeconds = builder.completionPauseSeconds;
            this.introVideoPath = builder.introVideoPath;
            this.introVideoDurationSeconds = builder.introVideoDurationSeconds;
            this.completionVideoPath = builder.completionVideoPath;
            this.completionVideoDurationSeconds = builder.completionVideoDurationSeconds;
            this.showPoemAfterVictory = builder.showPoemAfterVictory;
        }

        public boolean isBossLevel() {
            return bossLevel;
        }

        public String formatCompletionMessage(String playerName) {
            if (completionMessageKey == null && (completionMessageDefault == null || completionMessageDefault.isBlank())) {
                return null;
            }
            String safeName = playerName;
            if (safeName == null || safeName.isBlank()) {
                safeName = LocalizationManager.getInstance().getOrDefault("player.defaultName", "Игрок");
            }
            return LocalizationManager.getInstance().formatOrDefault(
                completionMessageKey != null ? completionMessageKey : "",
                completionMessageDefault != null ? completionMessageDefault : "{0}",
                safeName
            );
        }

        public Double getCompletionPauseSeconds() {
            return completionPauseSeconds;
        }

        public String getIntroVideoPath() {
            return introVideoPath;
        }

        public Double getIntroVideoDurationSeconds() {
            return introVideoDurationSeconds;
        }

        public String getCompletionVideoPath() {
            return completionVideoPath;
        }

        public Double getCompletionVideoDurationSeconds() {
            return completionVideoDurationSeconds;
        }

        public boolean shouldShowPoemAfterVictory() {
            return showPoemAfterVictory;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean bossLevel;
            private String completionMessageKey;
            private String completionMessageDefault;
            private Double completionPauseSeconds;
            private String introVideoPath;
            private Double introVideoDurationSeconds;
            private String completionVideoPath;
            private Double completionVideoDurationSeconds;
            private boolean showPoemAfterVictory;

            public Builder boss(boolean value) {
                this.bossLevel = value;
                return this;
            }

            public Builder completionMessage(String key, String defaultPattern) {
                this.completionMessageKey = key;
                this.completionMessageDefault = defaultPattern;
                return this;
            }

            public Builder completionPauseSeconds(Double seconds) {
                this.completionPauseSeconds = seconds;
                return this;
            }

            public Builder introVideo(String path, Double durationSeconds) {
                this.introVideoPath = path;
                this.introVideoDurationSeconds = durationSeconds;
                return this;
            }

            public Builder completionVideo(String path, Double durationSeconds) {
                this.completionVideoPath = path;
                this.completionVideoDurationSeconds = durationSeconds;
                return this;
            }

            public Builder showPoemAfterVictory(boolean value) {
                this.showPoemAfterVictory = value;
                return this;
            }

            public LevelMetadata build() {
                return new LevelMetadata(this);
            }
        }
    }
    
    // Конфигурация всех 100 уровней
    public static final Map<Integer, LevelData> LEVELS = new HashMap<>() {{
        
        // Обычные уровни 1-9
        put(1, new LevelData("level.name.1", "Уровень 1: Неоновое Пробуждение", "level1.json", "level1.png", "music/level1.mp3"));
        put(2, new LevelData("level.name.2", "Уровень 2: Поток Данных", "level2.json", "level2.png", "music/level2.mp3"));
        put(3, new LevelData("level.name.3", "Уровень 3: Хромированный Переулок", "level3.json", "level3.png", "music/level3.mp3"));
        put(4, new LevelData("level.name.4", "Уровень 4: Пиксельный Синдикат", "level4.json", "level4.png", "music/level4.mp3"));
        put(5, new LevelData("level.name.5", "Уровень 5: Всплеск Напряжения", "level5.json", "level5.png", "music/level5.mp3"));
        put(6, new LevelData("level.name.6", "Уровень 6: Кибер-Трущобы", "level6.json", "level6.png", "music/level6.mp3"));
        put(7, new LevelData("level.name.7", "Уровень 7: Голограммный Квартал", "level7.json", "level7.png", "music/level7.mp3"));
        put(8, new LevelData("level.name.8", "Уровень 8: Бинарный Хаос", "level8.json", "level8.png", "music/level8.mp3"));
        put(9, new LevelData("level.name.9", "Уровень 9: Нейронные Врата", "level9.json", "level9.png", "music/level9.mp3"));
        
        // Особый уровень 10
        put(10, new LevelData("level.name.10", "Уровень 10: FIREWALL-7", "level10.json", "boss1_background.png", "music/boss_music1.mp3"));
        
        // Обычные уровни 11-19
        put(11, new LevelData("level.name.11", "Уровень 11: Башня Мегакорпа", "level11.json", "level11.png", "music/level11.mp3"));
        put(12, new LevelData("level.name.12", "Уровень 12: Чёрный Лёд", "level12.json", "level12.png", "music/level12.mp3"));
        put(13, new LevelData("level.name.13", "Уровень 13: Синтвейв", "level13.json", "level13.png", "music/level13.mp3"));
        put(14, new LevelData("level.name.14", "Уровень 14: Протокол Мятежного ИИ", "level14.json", "level14.png", "music/level14.mp3"));
        put(15, new LevelData("level.name.15", "Уровень 15: Неоновое Подбрюшье", "level15.json", "level15.png", "music/level15.mp3"));
        put(16, new LevelData("level.name.16", "Уровень 16: Квантовый Взлом", "level16.json", "level16.png", "music/level16.mp3"));
        put(17, new LevelData("level.name.17", "Уровень 17: Бегущий по Лезвию", "level17.json", "level17.png", "music/level17.mp3"));
        put(18, new LevelData("level.name.18", "Уровень 18: Зашифрованное Ядро", "level18.json", "level18.png", "music/level18.mp3"));
        put(19, new LevelData("level.name.19", "Уровень 19: Призрак в Сети", "level19.json", "level19.png", "music/level19.mp3"));
        
        // Особый уровень 20
        put(20, new LevelData("level.name.20", "Уровень 20: Архитектор Города", "level20.json", "boss_background2.png", "music/boss_music2.mp3"));
        
        // Обычные уровни 21-29
        put(21, new LevelData("level.name.21", "Уровень 21: Техно-Якудза", "level21.json", "level21.png", "music/level21.mp3"));
        put(22, new LevelData("level.name.22", "Уровень 22: Виртуальная Пустошь", "level22.json", "level22.png", "music/level22.mp3"));
        put(23, new LevelData("level.name.23", "Уровень 23: Хромированный Мятеж", "level23.json", "level23.png", "music/level23.mp3"));
        put(24, new LevelData("level.name.24", "Уровень 24: Нексус Даркнета", "level24.json", "level24.png", "music/level24.mp3"));
        put(25, new LevelData("level.name.25", "Уровень 25: Плазменный Собор", "level25.json", "level25.png", "music/level25.mp3"));
        put(26, new LevelData("level.name.26", "Уровень 26: Сны Андроидов", "level26.json", "level26.png", "music/level26.mp3"));
        put(27, new LevelData("level.name.27", "Уровень 27: Город Сбоев", "level27.json", "level27.png", "music/level27.mp3"));
        put(28, new LevelData("level.name.28", "Уровень 28: Кибер-Самурай", "level28.json", "level28.png", "music/level28.mp3"));
        put(29, new LevelData("level.name.29", "Уровень 29: Перегрузка Датапотока", "level29.json", "level29.png", "music/level29.mp3"));
        
        // Финальный уровень 30
        put(30, new LevelData("level.name.30", "Уровень 30: MONOLITH.exe", "level30.json", "boss_background3.png", "music/boss_music3.mp3"));
        
        // Дополнительный уровень 31 (бывший уровень 0)
        put(31, new LevelData("level.name.31", "Уровень 31: Отладка", "level31.json", "easter_egg.png", "music/level31.mp3"));
        
        // Новая арка 32-39
        put(32, new LevelData("level.name.32", "Уровень 32: Электрический Лабиринт", "level32.json", "level32.png", "music/level32.mp3"));
        put(33, new LevelData("level.name.33", "Уровень 33: Туман Неоновой Памяти", "level33.json", "level33.png", "music/level33.mp3"));
        put(34, new LevelData("level.name.34", "Уровень 34: Вирусный Код", "level34.json", "level34.png", "music/level34.mp3"));
        put(35, new LevelData("level.name.35", "Уровень 35: Протокол Омега", "level35.json", "level35.png", "music/level35.mp3"));
        put(36, new LevelData("level.name.36", "Уровень 36: Пульс Мегаполиса", "level36.json", "level36.png", "music/level36.mp3"));
        put(37, new LevelData("level.name.37", "Уровень 37: Астральный Сервер", "level37.json", "level37.png", "music/level37.mp3"));
        put(38, new LevelData("level.name.38", "Уровень 38: Зеркало Системы", "level38.json", "level38.png", "music/level38.mp3"));
        put(39, new LevelData("level.name.39", "Уровень 39: Транзисторные Сны", "level39.json", "level39.png", "music/level39.mp3"));
        
        // Особый уровень 40
        put(40, new LevelData("level.name.40", "Уровень 40: Точка Сингулярности", "level40.json", "boss_background4.png", "music/boss_music4.mp3"));
        
        // Уровни 41-49
        put(41, new LevelData("level.name.41", "Уровень 41: Код Памяти", "level41.json", "level41.png", "music/level41.mp3"));
        put(42, new LevelData("level.name.42", "Уровень 42: Эхо Подсознания", "level42.json", "level42.png", "music/level42.mp3"));
        put(43, new LevelData("level.name.43", "Уровень 43: Перекрёсток Реальностей", "level43.json", "level43.png", "music/level43.mp3"));
        put(44, new LevelData("level.name.44", "Уровень 44: Алгоритм Судьбы", "level44.json", "level44.png", "music/level44.mp3"));
        put(45, new LevelData("level.name.45", "Уровень 45: Кристалл Данных", "level45.json", "level45.png", "music/level45.mp3"));
        put(46, new LevelData("level.name.46", "Уровень 46: Кибер-Оазис", "level46.json", "level46.png", "music/level46.mp3"));
        put(47, new LevelData("level.name.47", "Уровень 47: Сердце Сети", "level47.json", "level47.png", "music/level47.mp3"));
        put(48, new LevelData("level.name.48", "Уровень 48: Падение Нейросферы", "level48.json", "level48.png", "music/level48.mp3"));
        put(49, new LevelData("level.name.49", "Уровень 49: Тьма за Экранами", "level49.json", "level49.png", "music/level49.mp3"));

        // Финальный особый уровень 50
        put(50, new LevelData("level.name.50", "Уровень 50: Восход Монолита", "level50.json", "boss_background5.png", "music/boss_music5.mp3"));

        // Уровни 51-59
        put(51, new LevelData("level.name.51", "Уровень 51: Сектор \"Зеро\"", "level51.json", "level51.png", "music/level51.mp3"));
        put(52, new LevelData("level.name.52", "Уровень 52: Глитч-Каскад", "level52.json", "level52.png", "music/level52.mp3"));
        put(53, new LevelData("level.name.53", "Уровень 53: Ржавый Район", "level53.json", "level53.png", "music/level53.mp3"));
        put(54, new LevelData("level.name.54", "Уровень 54: Чёрный Рынок \"Зет\"", "level54.json", "level54.png", "music/level54.mp3"));
        put(55, new LevelData("level.name.55", "Уровень 55: Протокол \"Химера\"", "level55.json", "level55.png", "music/level55.mp3"));
        put(56, new LevelData("level.name.56", "Уровень 56: Канал Контрабандистов", "level56.json", "level56.png", "music/level56.mp3"));
        put(57, new LevelData("level.name.57", "Уровень 57: Фантомный Сигнал", "level57.json", "level57.png", "music/level57.mp3"));
        put(58, new LevelData("level.name.58", "Уровень 58: Шпиль \"ОмниКорп\"", "level58.json", "level58.png", "music/level58.mp3"));
        put(59, new LevelData("level.name.59", "Уровень 59: Запретная Зона", "level59.json", "level59.png", "music/level59.mp3"));

        // Босс-уровень 60
        put(60, new LevelData("level.name.60", "Уровень 60: (Босс) Страж \"Цербер\"", "level60.json", "boss_background6.png", "music/boss_music6.mp3"));

        // Уровни 61-69
        put(61, new LevelData("level.name.61", "Уровень 61: Световые Магистрали", "level61.json", "level61.png", "music/level61.mp3"));
        put(62, new LevelData("level.name.62", "Уровень 62: Архив \"Коллектор\"", "level62.json", "level62.png", "music/level62.mp3"));
        put(63, new LevelData("level.name.63", "Уровень 63: Паника Ядра", "level63.json", "level63.png", "music/level63.mp3"));
        put(64, new LevelData("level.name.64", "Уровень 64: Стеклянный Симулякр", "level64.json", "level64.png", "music/level64.mp3"));
        put(65, new LevelData("level.name.65", "Уровень 65: Искажённая Реальность", "level65.json", "level65.png", "music/level65.mp3"));
        put(66, new LevelData("level.name.66", "Уровень 66: Руткит-Туннель", "level66.json", "level66.png", "music/level66.mp3"));
        put(67, new LevelData("level.name.67", "Уровень 67: Цифровой Дождь", "level67.json", "level67.png", "music/level67.mp3"));
        put(68, new LevelData("level.name.68", "Уровень 68: Лаборатория \"Экзодус\"", "level68.json", "level68.png", "music/level68.mp3"));
        put(69, new LevelData("level.name.69", "Уровень 69: Последний Бэкап", "level69.json", "level69.png", "music/level69.mp3"));

        // Босс-уровень 70
        put(70, new LevelData("level.name.70", "Уровень 70: (Босс) ИИ \"Левиафан\"", "level70.json", "boss_background7.png", "music/boss_music7.mp3"));

        // Уровни 71-79
        put(71, new LevelData("level.name.71", "Уровень 71: Штаб-квартира \"БиоТех\"", "level71.json", "level71.png", "music/level71.mp3"));
        put(72, new LevelData("level.name.72", "Уровень 72: Улей Дронов", "level72.json", "level72.png", "music/level72.mp3"));
        put(73, new LevelData("level.name.73", "Уровень 73: Логическая Бомба", "level73.json", "level73.png", "music/level73.mp3"));
        put(74, new LevelData("level.name.74", "Уровень 74: Секретный Проект \"Тень\"", "level74.json", "level74.png", "music/level74.mp3"));
        put(75, new LevelData("level.name.75", "Уровень 75: Нулевой Пациент", "level75.json", "level75.png", "music/level75.mp3"));
        put(76, new LevelData("level.name.76", "Уровень 76: Кислотные Небеса", "level76.json", "level76.png", "music/level76.mp3"));
        put(77, new LevelData("level.name.77", "Уровень 77: Завод \"Атлас\"", "level77.json", "level77.png", "music/level77.mp3"));
        put(78, new LevelData("level.name.78", "Уровень 78: Красный Уровень Тревоги", "level78.json", "level78.png", "music/level78.mp3"));
        put(79, new LevelData("level.name.79", "Уровень 79: Прорыв Периметра", "level79.json", "level79.png", "music/level79.mp3"));

        // Босс-уровень 80
        put(80, new LevelData("level.name.80", "Уровень 80: (Босс) \"Goliath\" (Мех-Защитник)", "level80.json", "boss_background8.png", "music/boss_music8.mp3"));

        // Уровни 81-89
        put(81, new LevelData("level.name.81", "Уровень 81: Рекурсивная Петля", "level81.json", "level81.png", "music/level81.mp3"));
        put(82, new LevelData("level.name.82", "Уровень 82: Поток Сознания", "level82.json", "level82.png", "music/level82.mp3"));
        put(83, new LevelData("level.name.83", "Уровень 83: Фрактальная Архитектура", "level83.json", "level83.png", "music/level83.mp3"));
        put(84, new LevelData("level.name.84", "Уровень 84: Забытый Сервер", "level84.json", "level84.png", "music/level84.mp3"));
        put(85, new LevelData("level.name.85", "Уровень 85: Синтетический Рассвет", "level85.json", "level85.png", "music/level85.mp3"));
        put(86, new LevelData("level.name.86", "Уровень 86: Бесконечный Коридор", "level86.json", "level86.png", "music/level86.mp3"));
        put(87, new LevelData("level.name.87", "Уровень 87: Зона Молчания", "level87.json", "level87.png", "music/level87.mp3"));
        put(88, new LevelData("level.name.88", "Уровень 88: Вектор Атаки", "level88.json", "level88.png", "music/level88.mp3"));
        put(89, new LevelData("level.name.89", "Уровень 89: Эфирный Шум", "level89.json", "level89.png", "music/level89.mp3"));

        // Босс-уровень 90
        put(90, new LevelData("level.name.90", "Уровень 90: (Босс) \"Демиург\" (Архитектор Сети)", "level90.json", "boss_background9.png", "music/boss_music9.mp3"));

        // Уровни 91-99
        put(91, new LevelData("level.name.91", "Уровень 91: За Гранью \"Чёрного Льда\"", "level91.json", "level91.png", "music/level91.mp3"));
        put(92, new LevelData("level.name.92", "Уровень 92: Разрыв Континуума", "level92.json", "level92.png", "music/level92.mp3"));
        put(93, new LevelData("level.name.93", "Уровень 93: Квантовая Запутанность", "level93.json", "level93.png", "music/level93.mp3"));
        put(94, new LevelData("level.name.94", "Уровень 94: Цитадель \"KOSMOS\"", "level94.json", "level94.png", "music/level94.mp3"));
        put(95, new LevelData("level.name.95", "Уровень 95: Точка Невозврата", "level95.json", "level95.png", "music/level95.mp3"));
        put(96, new LevelData("level.name.96", "Уровень 96: Аномалия \"Нуль\"", "level96.json", "level96.png", "music/level96.mp3"));
        put(97, new LevelData("level.name.97", "Уровень 97: Чистый Код", "level97.json", "level97.png", "music/level97.mp3"));
        put(98, new LevelData("level.name.98", "Уровень 98: Рождение Сверхновой", "level98.json", "level98.png", "music/level98.mp3"));
        put(99, new LevelData("level.name.99", "Уровень 99: Преддверие Вечности", "level99.json", "level99.png", "music/level99.mp3"));

        // Финальный босс-уровень 100
        put(100, new LevelData("level.name.100", "Уровень 100: (Финал) Цифровое Бессмертие", "level100.json", "boss_background10.png", "music/boss_music10.mp3"));

        // Глава XII: Восстановление Системы (101-116)
        put(101, new LevelData("level.name.101", "Уровень 101: Архивы Хранителя", "level101.json", "level101.png", "music/level101.mp3"));
        put(102, new LevelData("level.name.102", "Уровень 102: Зона \"Абсолют\"", "level102.json", "level102.png", "music/level102.mp3"));
        put(103, new LevelData("level.name.103", "Уровень 103: Эхо \"Генезиса\"", "level103.json", "level103.png", "music/level103.mp3"));
        put(104, new LevelData("level.name.104", "Уровень 104: Шёпот за Гранью", "level104.json", "level104.png", "music/level104.mp3"));
        put(105, new LevelData("level.name.105", "Уровень 105: Раскол Монолита", "level105.json", "level105.png", "music/level105.mp3"));
        put(106, new LevelData("level.name.106", "Уровень 106: Карантин KOSMOS", "level106.json", "level106.png", "music/level106.mp3"));
        put(107, new LevelData("level.name.107", "Уровень 107: Ледяная Аномалия", "level107.json", "level107.png", "music/level107.mp3"));
        put(108, new LevelData("level.name.108", "Уровень 108: Призрачный Руткит", "level108.json", "level108.png", "music/level108.mp3"));
        put(109, new LevelData("level.name.109", "Уровень 109: Нескомпилированный Страх", "level109.json", "level109.png", "music/level109.mp3"));
        put(110, new LevelData("level.name.110", "Уровень 110: Нулевой Протокол", "level110.json", "level110.png", "music/level110.mp3"));
        put(111, new LevelData("level.name.111", "Уровень 111: Оскверненный Трон", "level111.json", "level111.png", "music/level111.mp3"));
        put(112, new LevelData("level.name.112", "Уровень 112: Царство Монолитов", "level112.json", "level112.png", "music/level112.mp3"));
        put(113, new LevelData("level.name.113", "Уровень 113: Ледяной Бастион", "level113.json", "level113.png", "music/level113.mp3"));
        
        // Особые уровни 114 и 115 (вместо боссов)
        put(114, new LevelData("level.name.114", "Уровень 114: Древний Монолит", "level114.json", "boss_background11.png", "music/boss_music11.mp3"));
        put(115, new LevelData("level.name.115", "Уровень 115: Орион (Архитектор Цитадели)", "level115.json", "boss_background12.png", "music/boss_music12.mp3"));
        
        // Финальный уровень 116
        put(116, new LevelData("level.name.116", "Уровень 116: Снежные Холмы", "level116.json", "level116.png", "music/level116.mp3"));
    }};

    /**
     * Мета данные по уровням
     */
    public static final Map<Integer, LevelMetadata> LEVEL_METADATA = new HashMap<>() {{
        put(1, LevelMetadata.builder()
            .introVideo("level1_init.mp4", 8.0)
            .build());
        put(11, LevelMetadata.builder()
            .introVideo("level11_init.mp4", 8.0)
            .build());
        put(10, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.10", "{0} вы победили Firewall-7!")
            .completionPauseSeconds(12.0)
            .introVideo("boss1_init.mp4", 8.0)
            .completionVideo("boss_completed1_video.mp4", 8.0)
            .build());
        put(20, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.20", "{0} вы победили Архитектора Города!")
            .completionPauseSeconds(12.0)
            .introVideo("boss2_init.mp4", 8.0)
            .completionVideo("boss_completed2_video.mp4", 8.0)
            .build());
        put(21, LevelMetadata.builder()
            .introVideo("level21_init.mp4", 8.0)
            .build());
        put(30, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.30", "{0} вы победили MONOLITH.EXE")
            .completionPauseSeconds(17.0)
            .introVideo("boss3_init.mp4", 8.0)
            .completionVideo("boss_completed3_video.mp4", 8.0)
            .build());
        put(31, LevelMetadata.builder()
            .introVideo("level31_init.mp4", 8.0)
            .build());
        put(32, LevelMetadata.builder()
            .introVideo("level32_init.mp4", 8.0)
            .build());
        put(40, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.40", "{0} вы победили Точку Сингулярности!")
            .completionPauseSeconds(15.0)
            .introVideo("boss4_init.mp4", 8.0)
            .completionVideo("boss_completed4_video.mp4", 8.0)
            .build());
        put(41, LevelMetadata.builder()
            .introVideo("level41_init.mp4", 8.0)
            .build());
        put(50, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.50", "{0} вы победили Монолита!")
            .completionPauseSeconds(20.0)
            .introVideo("boss5_init.mp4", 8.0)
            .completionVideo("boss_completed5_video.mp4", 8.0)
            .build());
        put(51, LevelMetadata.builder()
            .introVideo("level51_init.mp4", 8.0)
            .build());
        put(60, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.60", "{0} вы победили Стража \"Цербера\"!")
            .completionPauseSeconds(10.0)
            .introVideo("boss6_init.mp4", 8.0)
            .completionVideo("boss_completed6_video.mp4", 8.0)
            .build());
        put(61, LevelMetadata.builder()
            .introVideo("level61_init.mp4", 8.0)
            .build());
        put(70, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.70", "{0} вы победили ИИ \"Левиафан\".")
            .completionPauseSeconds(10.0)
            .introVideo("boss7_init.mp4", 8.0)
            .completionVideo("boss_completed7_video.mp4", 8.0)
            .build());
        put(71, LevelMetadata.builder()
            .introVideo("level71_init.mp4", 8.0)
            .build());
        put(80, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.80", "{0} вы победили \"Goliath\" (Мех-Защитник)!")
            .completionPauseSeconds(10.0)
            .introVideo("boss8_init.mp4", 8.0)
            .completionVideo("boss_completed8_video.mp4", 8.0)
            .build());
        put(81, LevelMetadata.builder()
            .introVideo("level81_init.mp4", 8.0)
            .build());
        put(90, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.90", "{0} вы победили \"Демиург\" (Архитектор Сети)!")
            .completionPauseSeconds(10.0)
            .introVideo("boss9_init.mp4", 8.0)
            .completionVideo("boss_completed9_video.mp4", 8.0)
            .build());
        put(91, LevelMetadata.builder()
            .introVideo("level91_init.mp4", 8.0)
            .build());
        put(100, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.100", "{0} достиг цифрового бессмертия!")
            .completionPauseSeconds(33.0)
            .introVideo("boss10_init.mp4", 10.0)
            .completionVideo("boss_completed10_video.mp4", 33.0)
            .showPoemAfterVictory(true)
            .build());
        
        // Глава XII метаданные
        put(101, LevelMetadata.builder()
            .introVideo("level101_init.mp4", 8.0)
            .build());
        put(102, LevelMetadata.builder()
            .build());
        put(103, LevelMetadata.builder()
            .build());
        put(104, LevelMetadata.builder()
            .build());
        put(105, LevelMetadata.builder()
            .build());
        put(106, LevelMetadata.builder()
            .build());
        put(107, LevelMetadata.builder()
            .build());
        put(108, LevelMetadata.builder()
            .build());
        put(109, LevelMetadata.builder()
            .build());
        put(110, LevelMetadata.builder()
            .build());
        put(111, LevelMetadata.builder()
            .build());
        put(112, LevelMetadata.builder()
            .build());
        put(113, LevelMetadata.builder()
            .build());
        
        // Особые уровни 114 и 115 (вместо боссов)
        put(114, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.114", "{0} победил Древний Монолит!")
            .completionPauseSeconds(15.0)
            .introVideo("boss11_init.mp4", 8.0)
            .completionVideo("boss_completed11_video.mp4", 24.0)
            .build());
        put(115, LevelMetadata.builder()
            .boss(true)
            .completionMessage("level.completion.115", "{0} победил Орион (Архитектор Цитадели)!")
            .completionPauseSeconds(44.0)  // Увеличена пауза для перехода на уровень 116
            .introVideo("boss12_init.mp4", 8.0)
            .completionVideo("boss_completed12_video.mp4", 44.0)
            .build());
        
        // Финальный уровень 116
        put(116, LevelMetadata.builder()
            .completionMessage("level.completion.116", "Поздравляем! Вы прошли игру!")
            .completionPauseSeconds(58.0)  // Пауза после видео перед переходом в главное меню
            .completionVideo("level116_completed_video.mp4", 58.0)  // Длительность видео
            .build());
    }};

    private static final Set<Integer> BOSS_LEVELS = Set.of(
        10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 114, 115
    );
    
    /**
     * Получить данные уровня по номеру
     */
    public static LevelData getLevel(int levelNumber) {
        return LEVELS.get(levelNumber);
    }

    public static LevelMetadata getMetadata(int levelNumber) {
        return LEVEL_METADATA.get(levelNumber);
    }

    public static boolean isBossLevel(int levelNumber) {
        return BOSS_LEVELS.contains(levelNumber);
    }
    
    /**
     * Применить специальные настройки бонусов для конкретных уровней
     * @param levelNumber номер уровня
     */
    public static void applyLevelBonusSettings(int levelNumber) {
        // Сбрасываем все специальные режимы перед применением новых
        BonusConfig.POSITIVE_BONUSES_ONLY = false;
        BonusConfig.NEGATIVE_BONUSES_ONLY = false;
        
        // Уровни 80, 114, 115 - только негативные бонусы
        if (levelNumber == 80 || levelNumber == 114 || levelNumber == 115) {
            BonusConfig.NEGATIVE_BONUSES_ONLY = true;
        }
        
        // Уровень 116 - только позитивные бонусы (финал)
        if (levelNumber == 116) {
            BonusConfig.POSITIVE_BONUSES_ONLY = true;
        }
    }
}
