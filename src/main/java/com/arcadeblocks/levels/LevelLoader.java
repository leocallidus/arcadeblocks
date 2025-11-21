package com.arcadeblocks.levels;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.gameplay.Brick;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Загрузчик уровней из JSON файлов
 */
public class LevelLoader {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern LEVEL_NUMBER_PATTERN = Pattern.compile("(\\d+)");
    
    /**
     * Загрузить уровень из JSON файла
     */
    public static void loadLevelFromJson(String levelFileName) {
        // Извлекаем номер уровня из имени файла
        int levelNumber = extractLevelNumber(levelFileName);
        
        // Применяем специальные настройки бонусов для этого уровня
        if (levelNumber > 0) {
            com.arcadeblocks.config.LevelConfig.applyLevelBonusSettings(levelNumber);
        }
        
        LevelLayout layout = null;

        try (InputStream inputStream = LevelLoader.class.getResourceAsStream("/assets/levels/" + levelFileName)) {
            if (inputStream != null) {
                layout = parseLayout(inputStream);
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки уровня из файла " + levelFileName + ": " + e.getMessage());
            e.printStackTrace();
        }

        if (layout == null) {
            if (levelNumber > 0) {
                layout = ProceduralLevelGenerator.generate(levelNumber);
            }
        }

        if (layout == null) {
            System.err.println("Не удалось загрузить или сгенерировать уровень для файла: " + levelFileName);
            return;
        }

        spawnLayout(layout);
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
                bricks.add(new BrickDefinition(row, col, colorName, health, points));
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
            boolean isExplosive = "explosive".equals(brickDef.color());

            brickEntity.addComponent(new Brick(brickDef.health(), brickColor, brickDef.points(), isExplosive));
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

    public record BrickDefinition(int row, int col, String color, int health, int points) {}

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
}
