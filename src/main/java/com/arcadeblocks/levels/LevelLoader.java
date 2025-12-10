package com.arcadeblocks.levels;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.arcadeblocks.config.BonusLevelConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.LevelConfig;
import com.arcadeblocks.config.LevelConfig.LevelFormat;
import com.arcadeblocks.gameplay.Brick;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Загрузчик уровней из JSON файлов
 */
public class LevelLoader {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern LEVEL_NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final int LBREAKOUT_WIDTH = 14;
    private static final int LBREAKOUT_HEIGHT = 18;
    private static final int LBREAKOUT_BRICK_SPACING = 4;
    private static final int LBREAKOUT_START_Y = 30;
    private static final Map<Character, BrickTemplate> LBREAKOUT_BRICK_MAP = buildLBreakoutBrickMap();
    
    /**
     * Загрузить уровень в зависимости от формата LevelData
     */
    public static void loadLevel(int levelNumber, LevelConfig.LevelData levelData) {
        if (levelData == null) {
            System.err.println("Уровень " + levelNumber + " не найден в конфигурации");
            return;
        }

        // Применяем специальные настройки бонусов для этого уровня
        if (levelNumber > 0) {
            if (BonusLevelConfig.isBonusLevel(levelNumber)) {
                applyBonusOverridesForBonusLevels(levelNumber);
            } else {
                com.arcadeblocks.config.LevelConfig.applyLevelBonusSettings(levelNumber);
            }
        }

        LevelLayout layout = switch (levelData.getLevelFormat()) {
            case LBREAKOUT -> loadLBreakoutLayout(levelData);
            case JSON -> loadJsonLayout(levelNumber, levelData.getLevelFile());
            default -> loadJsonLayout(levelNumber, levelData.getLevelFile());
        };

        if (layout == null && levelNumber > 0) {
            layout = ProceduralLevelGenerator.generate(levelNumber);
        }

        if (layout == null) {
            System.err.println("Не удалось загрузить или сгенерировать уровень " + levelNumber);
            return;
        }

        spawnLayout(layout);
    }

    /**
     * Совместимость для старого вызова загрузки JSON
     */
    public static void loadLevelFromJson(String levelFileName) {
        int levelNumber = extractLevelNumber(levelFileName);
        LevelConfig.LevelData temp = new LevelConfig.LevelData(
            "level.name." + levelNumber,
            levelFileName,
            levelFileName,
            "",
            "",
            LevelFormat.JSON,
            -1
        );
        loadLevel(levelNumber, temp);
    }

    /**
     * Загрузить уровень по номеру из стандартного JSON
     */
    public static void loadLevel(int levelNumber) {
        String fileName = "level" + levelNumber + ".json";
        loadLevelFromJson(fileName);
    }

    private static LevelLayout loadJsonLayout(int levelNumber, String levelFileName) {
        String levelPath = getLevelPath(levelNumber, levelFileName);
        try (InputStream inputStream = LevelLoader.class.getResourceAsStream(levelPath)) {
            if (inputStream != null) {
                return parseLayout(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки уровня из файла " + levelPath + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Получить путь к файлу уровня
     */
    private static String getLevelPath(int levelNumber, String levelFileName) {
        if (BonusLevelConfig.isBonusLevel(levelNumber)) {
            return "/assets/levels/bonus/" + levelFileName;
        }
        return "/assets/levels/" + levelFileName;
    }

    private static LevelLayout loadLBreakoutLayout(LevelConfig.LevelData levelData) {
        Path levelSetPath = resolveLevelSetPath(levelData.getLevelFile());
        int targetIndex = Math.max(0, levelData.getExternalLevelIndex());
        if (levelSetPath != null && Files.exists(levelSetPath)) {
            try (BufferedReader reader = Files.newBufferedReader(levelSetPath, StandardCharsets.ISO_8859_1)) {
                return parseLBreakout(reader, targetIndex);
            } catch (IOException e) {
                System.err.println("Ошибка чтения уровня LBreakout из файла " + levelSetPath + ": " + e.getMessage());
            }
        }

        try (InputStream resourceStream = LevelLoader.class.getResourceAsStream("/" + levelData.getLevelFile())) {
            if (resourceStream != null) {
                try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(resourceStream, StandardCharsets.ISO_8859_1))) {
                    return parseLBreakout(reader, targetIndex);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки ресурса уровня LBreakout: " + e.getMessage());
        }

        System.err.println("Не удалось найти файл уровня LBreakout: " + levelData.getLevelFile());
        return null;
    }

    private static Path resolveLevelSetPath(String levelFile) {
        if (levelFile == null || levelFile.isBlank()) {
            return null;
        }
        Path direct = Path.of(levelFile);
        if (Files.exists(direct)) {
            return direct;
        }
        Path relativeToWorkdir = Path.of(System.getProperty("user.dir")).resolve(levelFile);
        if (Files.exists(relativeToWorkdir)) {
            return relativeToWorkdir;
        }
        return null;
    }

    private static LevelLayout parseLBreakout(BufferedReader reader, int targetIndex) throws IOException {
        String line = reader.readLine();
        if (line == null || !line.startsWith("Version:")) {
            return null;
        }

        int currentIndex = 0;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            if (!"Level:".equals(line)) {
                continue;
            }
            // author and title (kept for future use)
            String author = reader.readLine();
            String title = reader.readLine();
            String bricksHeader = reader.readLine();
            if (author == null || title == null || bricksHeader == null || !"Bricks:".equals(bricksHeader)) {
                break;
            }
            List<String> brickRows = new ArrayList<>();
            for (int j = 0; j < LBREAKOUT_HEIGHT; j++) {
                String row = reader.readLine();
                if (row == null) {
                    break;
                }
                brickRows.add(padRight(row, LBREAKOUT_WIDTH));
            }
            String bonusHeader = reader.readLine();
            if (bonusHeader == null || !"Bonus:".equals(bonusHeader)) {
                break;
            }
            // Skip bonus rows for now
            for (int j = 0; j < LBREAKOUT_HEIGHT; j++) {
                reader.readLine();
            }

            if (currentIndex == targetIndex) {
                return convertLBreakoutRows(brickRows);
            }
            currentIndex++;
        }
        return null;
    }

    private static LevelLayout convertLBreakoutRows(List<String> brickRows) {
        List<BrickDefinition> bricks = new ArrayList<>();
        for (int row = 0; row < Math.min(brickRows.size(), LBREAKOUT_HEIGHT); row++) {
            String line = brickRows.get(row);
            for (int col = 0; col < Math.min(line.length(), LBREAKOUT_WIDTH); col++) {
                char c = line.charAt(col);
                BrickTemplate template = getTemplateForChar(c);
                if (template == null) {
                    continue;
                }
                bricks.add(new BrickDefinition(
                    row,
                    col,
                    template.color(),
                    template.health(),
                    template.points(),
                    template.explosive(),
                    template.countTowardsCompletion()
                ));
            }
        }

        return new LevelLayout(
            LBREAKOUT_WIDTH,
            GameConfig.BRICK_WIDTH,
            GameConfig.BRICK_HEIGHT,
            LBREAKOUT_BRICK_SPACING,
            LBREAKOUT_START_Y,
            bricks
        );
    }

    private static BrickTemplate getTemplateForChar(char rawChar) {
        BrickTemplate template = LBREAKOUT_BRICK_MAP.get(rawChar);
        if (template != null) {
            return template;
        }
        // fallback: grown bricks are stored upper-case, use lower-case stats
        char lowered = Character.toLowerCase(rawChar);
        return LBREAKOUT_BRICK_MAP.get(lowered);
    }

    private static Map<Character, BrickTemplate> buildLBreakoutBrickMap() {
        Map<Character, BrickTemplate> map = new HashMap<>();
        map.put('E', new BrickTemplate("light_gray", Integer.MAX_VALUE, 0, false, false));
        // В оригинале '#' часто использовался как стальной блок; делаем его разрушаемым, но прочным
        map.put('#', new BrickTemplate("dark_blue", 5, 0, false, true));
        map.put('@', new BrickTemplate("dark_blue", 3, 240, false, true));
        map.put('a', new BrickTemplate("green", 1, 140, false, true));
        map.put('b', new BrickTemplate("blue", 2, 180, false, true));
        map.put('c', new BrickTemplate("orange", 3, 220, false, true));
        map.put('v', new BrickTemplate("red", 4, 260, false, true));
        map.put('x', new BrickTemplate("yellow", 1, 140, false, true));
        map.put('y', new BrickTemplate("yellow", 2, 200, false, true));
        map.put('z', new BrickTemplate("yellow", 3, 260, false, true));
        map.put('*', new BrickTemplate("explosive", 1, 220, true, true));
        map.put('!', new BrickTemplate("light_gray", 1, 180, false, true));

        // Variety bricks (single hit)
        char[] variety = new char[]{'d', 'e', 'f', 'g', 'h', 'i', 'j', 'k'};
        String[] colors = new String[]{"pink", "purple", "orange", "blue", "green", "red", "light_gray", "dark_blue"};
        for (int idx = 0; idx < variety.length; idx++) {
            map.put(variety[idx], new BrickTemplate(colors[idx % colors.length], 1, 160, false, true));
        }
        return map;
    }

    private static String padRight(String value, int targetLength) {
        if (value.length() >= targetLength) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < targetLength) {
            sb.append('.');
        }
        return sb.toString();
    }

    private static LevelLayout parseLayout(InputStream inputStream) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputStream);
        if (rootNode == null) {
            return null;
        }

        JsonNode layoutNode = rootNode.get("layout");
        if (layoutNode == null) {
            return null;
        }

        int brickColumns = layoutNode.get("brickColumns").asInt();
        int brickWidth = layoutNode.get("brickWidth").asInt();
        int brickHeight = layoutNode.get("brickHeight").asInt();
        int brickSpacing = layoutNode.get("brickSpacing").asInt();
        int startY = layoutNode.get("startY").asInt();

        List<BrickDefinition> bricks = new ArrayList<>();
        JsonNode bricksNode = rootNode.get("bricks");
        if (bricksNode != null && bricksNode.isArray()) {
            for (JsonNode brickNode : bricksNode) {
                int row = brickNode.get("row").asInt();
                int col = brickNode.get("col").asInt();
                String colorName = brickNode.get("color").asText();
                int health = brickNode.get("health").asInt();
                int points = brickNode.get("points").asInt();
                boolean isExplosive = "explosive".equalsIgnoreCase(colorName);
                bricks.add(new BrickDefinition(row, col, colorName, health, points, isExplosive, true));
            }
        }

        return new LevelLayout(brickColumns, brickWidth, brickHeight, brickSpacing, startY, bricks);
    }

    private static void spawnLayout(LevelLayout layout) {
        double totalBrickWidth = layout.brickColumns() * (layout.brickWidth() + layout.brickSpacing()) - layout.brickSpacing();
        double startX = (GameConfig.GAME_WIDTH - totalBrickWidth) / 2.0;

        for (BrickDefinition brickDef : layout.bricks()) {
            double x = startX + brickDef.col() * (layout.brickWidth() + layout.brickSpacing());
            double y = GameConfig.TOP_UI_HEIGHT + layout.startY() + brickDef.row() * (layout.brickHeight() + layout.brickSpacing());

            SpawnData spawnData = new SpawnData(x, y);
            spawnData.put("color", brickDef.color());

            Entity brickEntity = FXGL.spawn("brick", spawnData);
            Color brickColor = getColorFromName(brickDef.color());
            brickEntity.addComponent(
                new Brick(
                    brickDef.health(),
                    brickColor,
                    brickDef.points(),
                    brickDef.explosive(),
                    brickDef.countTowardsCompletion()
                )
            );
        }
    }

    private static int extractLevelNumber(String levelFileName) {
        Matcher matcher = LEVEL_NUMBER_PATTERN.matcher(levelFileName);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private record BrickTemplate(String color, int health, int points, boolean explosive,
                                 boolean countTowardsCompletion) {}

    public record BrickDefinition(int row, int col, String color, int health, int points,
                                  boolean explosive, boolean countTowardsCompletion) {}

    public record LevelLayout(int brickColumns, int brickWidth, int brickHeight, int brickSpacing, int startY,
                              List<BrickDefinition> bricks) {}
    
    /**
     * Преобразовать имя цвета в Color объект
     */
    private static Color getColorFromName(String colorName) {
        switch (colorName.toLowerCase()) {
            case "blue":
                return Color.web("#4A90E2");
            case "green":
                return Color.web("#7FFF7F");
            case "pink":
                return Color.web("#FF6EC7");
            case "purple":
                return Color.web(GameConfig.NEON_PURPLE);
            case "yellow":
                return Color.web("#FFFF7F");
            case "orange":
                return Color.web("#FF9F40");
            case "red":
                return Color.web("#FF5757");
            case "light_gray":
                return Color.web("#B0B0B0");
            case "explosive":
                return Color.web("#FF4444");
            case "dark_blue":
                return Color.web("#1E3A8A");
            default:
                return Color.web(GameConfig.NEON_PURPLE);
        }
    }

    private static void applyBonusOverridesForBonusLevels(int levelNumber) {
        // Сбрасываем ограничители на бонусы по умолчанию
        com.arcadeblocks.config.BonusConfig.POSITIVE_BONUSES_ONLY = false;
        com.arcadeblocks.config.BonusConfig.NEGATIVE_BONUSES_ONLY = false;
        com.arcadeblocks.config.BonusConfig.DISABLE_ALL_BONUSES = false;

        boolean negativeOnly = levelNumber == 1010 || levelNumber == 1020 || levelNumber == 1021;
        if (negativeOnly) {
            com.arcadeblocks.config.BonusConfig.POSITIVE_BONUSES_ONLY = false;
            com.arcadeblocks.config.BonusConfig.NEGATIVE_BONUSES_ONLY = true;
        }
    }
}
