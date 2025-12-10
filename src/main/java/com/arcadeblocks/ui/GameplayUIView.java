package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ContentDisplay;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Неоновый интерфейс игрового поля с подсвеченной рамкой и верхней панелью статистики.
 */
public class GameplayUIView extends VBox {
    
    private final ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    
    private Label levelValueLabel;
    private Label levelNameLabel;
    private Label scoreValueLabel;
    private Label livesValueLabel;
    private Label timeValueLabel;
    private Label bossHealthLabel;
    private VBox topBarContainer;
    private Rectangle gameOverOverlay;
    private boolean gameOverVisualsApplied = false;
    private StackPane rootLayer;
    private StackPane worldWrapper;
    private StackPane playfieldLayer;
    private FadeTransition fadeTop;
    private Timeline overlayTimeline;
    
    public GameplayUIView(ArcadeBlocksApp app) {
        this.app = app;
        initializeUI();
        syncWithSaveState();
    }
    
    private void initializeUI() {
        setAlignment(Pos.TOP_LEFT);
        setPickOnBounds(false);
        // Не делаем слой полностью mouseTransparent, чтобы сцена корректно получала фокус
        setMouseTransparent(false);
        setSpacing(0);
        setPadding(Insets.EMPTY);
        
        rootLayer = new StackPane();
        rootLayer.setPickOnBounds(false);
        rootLayer.setMouseTransparent(true);
        rootLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        topBarContainer = createTopBar();
        StackPane.setAlignment(topBarContainer, Pos.TOP_CENTER);

        playfieldLayer = createPlayfieldDecorLayer();

        worldWrapper = new StackPane(playfieldLayer);
        worldWrapper.setPickOnBounds(false);
        worldWrapper.setMouseTransparent(true);
        worldWrapper.setPrefSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        worldWrapper.setMinSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        worldWrapper.setMaxSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        StackPane.setAlignment(worldWrapper, Pos.CENTER);

        rootLayer.getChildren().addAll(worldWrapper, topBarContainer);
        getChildren().setAll(rootLayer);
        VBox.setVgrow(rootLayer, Priority.ALWAYS);

        ResponsiveLayoutHelper.bindToStage(this, this::adjustLayoutForResolution);
        setUserData("fullScreenOverlay");
    }

    private void syncWithSaveState() {
        if (app == null || app.getSaveManager() == null) {
            return;
        }
        if (app.isDebugMode()) {
            updateLives(FXGL.geti("lives"));
            int level = FXGL.geti("level");
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(level);
            updateLevel(level, levelData != null ? levelData.getName() : null);
            updateScore(FXGL.geti("score"));
            return;
        }

        int lives = app.getSaveManager().getLives();
        if (lives <= 0) {
            com.arcadeblocks.config.DifficultyLevel difficulty = app.getEffectiveDifficulty();
            lives = difficulty != null ? difficulty.getLives() : GameConfig.INITIAL_LIVES;
            app.getSaveManager().setLives(lives);
        }
        updateLives(lives);

        int level = app.getSaveManager().getCurrentLevel();
        updateLevel(level, null);

        int score = app.getSaveManager().getScore();
        updateScore(score);
    }
    
    private VBox createTopBar() {
        VBox wrapper = new VBox();
        wrapper.setPickOnBounds(false);
        wrapper.setMouseTransparent(true);
        wrapper.setPadding(new Insets(24, 32, 0, 32));
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(GameConfig.GAME_WORLD_WIDTH);
        wrapper.setPrefWidth(GameConfig.GAME_WORLD_WIDTH);
        
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(28);
        bar.setPadding(new Insets(14, 28, 14, 28));
        bar.setBackground(new Background(new BackgroundFill(
            Color.web("#14142D", 0.9),
            new CornerRadii(18),
            Insets.EMPTY
        )));
        bar.setBorder(new Border(new BorderStroke(
            Color.web("#7EE8FA", 0.5),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.6)
        )));
        
        DropShadow glow = new DropShadow();
        glow.setRadius(20);
        glow.setSpread(0.32);
        glow.setColor(Color.web("#7EE8FA", 0.28));
        bar.setEffect(glow);
        
        Text arcadeText = new Text("ARCADE ");
        arcadeText.setFont(Font.font("Orbitron", FontWeight.BOLD, 19));
        arcadeText.setFill(Color.web(GameConfig.NEON_PINK));

        Text blocksText = new Text("BLOCKS");
        blocksText.setFont(Font.font("Orbitron", FontWeight.BOLD, 19));
        blocksText.setFill(Color.web(GameConfig.NEON_CYAN));

        TextFlow titleFlow = new TextFlow(arcadeText, blocksText);
        titleFlow.setTextAlignment(TextAlignment.LEFT);

        Label titleLabel = new Label();
        titleLabel.setGraphic(titleFlow);
        titleLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        levelValueLabel = new Label("1");
        levelNameLabel = new Label("");
        scoreValueLabel = new Label("0");
        livesValueLabel = new Label(String.valueOf(GameConfig.INITIAL_LIVES));
        timeValueLabel = new Label("00:00");
        
        HBox statsRow = new HBox(24);
        statsRow.setAlignment(Pos.CENTER_RIGHT);
        statsRow.getChildren().addAll(
            createLevelStatBlock("gameplay.stat.level", levelValueLabel, levelNameLabel, Color.web("#7EE8FA")),
            createDivider(),
            createStatBlock("gameplay.stat.score", scoreValueLabel, Color.web("#FF6EC7")),
            createDivider(),
            createStatBlock("gameplay.stat.lives", livesValueLabel, Color.web("#7FFF7F")),
            createDivider(),
            createStatBlock("gameplay.stat.time", timeValueLabel, Color.web("#FFB347"))
        );
        
        bossHealthLabel = new Label();
        bossHealthLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));
        bossHealthLabel.setTextFill(Color.web("#FFB347"));
        bossHealthLabel.setVisible(false);
        bossHealthLabel.setManaged(false);
        bossHealthLabel.setOpacity(0.9);
        
        VBox statsColumn = new VBox(4);
        statsColumn.setAlignment(Pos.CENTER_RIGHT);
        statsColumn.getChildren().addAll(statsRow, bossHealthLabel);
        
        bar.getChildren().addAll(titleLabel, spacer, statsColumn);
        wrapper.getChildren().add(bar);
        return wrapper;
    }
    
    private VBox createLevelStatBlock(String captionKey, Label valueLabel, Label nameLabel, Color accentColor) {
        Label captionLabel = new Label();
        localizationManager.bind(captionLabel, captionKey);
        captionLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 10));
        captionLabel.setTextFill(accentColor);
        captionLabel.setOpacity(0.9);
        
        valueLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        valueLabel.setTextFill(Color.web("#F8F8FF"));
        
        nameLabel.setFont(Font.font("Orbitron", FontWeight.NORMAL, 9));
        nameLabel.setTextFill(Color.web("#B8C5D6"));
        nameLabel.setOpacity(0.85);
        nameLabel.setMaxWidth(140);
        nameLabel.setWrapText(false);
        
        VBox block = new VBox(1, captionLabel, valueLabel, nameLabel);
        block.setAlignment(Pos.CENTER_LEFT);
        return block;
    }
    
    private VBox createStatBlock(String captionKey, Label valueLabel, Color accentColor) {
        Label captionLabel = new Label();
        localizationManager.bind(captionLabel, captionKey);
        captionLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 10));
        captionLabel.setTextFill(accentColor);
        captionLabel.setOpacity(0.9);
        
        valueLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        valueLabel.setTextFill(Color.web("#F8F8FF"));
        
        VBox block = new VBox(2, captionLabel, valueLabel);
        block.setAlignment(Pos.CENTER_LEFT);
        return block;
    }
    
    private Rectangle createDivider() {
        Rectangle divider = new Rectangle(1.6, 34, Color.web("#2F3B66", 0.7));
        divider.setArcHeight(6);
        divider.setArcWidth(6);
        return divider;
    }
    
    private StackPane createPlayfieldDecorLayer() {
        StackPane wrapper = new StackPane();
        playfieldLayer = wrapper;
        wrapper.setPickOnBounds(false);
        wrapper.setMouseTransparent(true);
        wrapper.setPadding(Insets.EMPTY);
        wrapper.setPrefSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        wrapper.setMinSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        wrapper.setMaxSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        gameOverOverlay = new Rectangle(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        gameOverOverlay.setFill(Color.web("#050814"));
        gameOverOverlay.setOpacity(0.0);
        gameOverOverlay.setVisible(false);
        gameOverOverlay.widthProperty().bind(wrapper.widthProperty());
        gameOverOverlay.heightProperty().bind(wrapper.heightProperty());
        wrapper.getChildren().add(gameOverOverlay);
        return wrapper;
    }

    private void adjustLayoutForResolution(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        if (rootLayer != null) {
            rootLayer.setPrefSize(width, height);
            rootLayer.setMinSize(width, height);
            rootLayer.setMaxSize(width, height);
        }

        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());

        if (worldWrapper != null) {
            StackPane.setAlignment(worldWrapper, Pos.CENTER);
            StackPane.setMargin(worldWrapper, new Insets(offsetY, offsetX, offsetY, offsetX));
        }

        if (topBarContainer != null) {
            StackPane.setAlignment(topBarContainer, Pos.TOP_CENTER);
            StackPane.setMargin(topBarContainer, new Insets(offsetY + 24, offsetX, 0, offsetX));
            topBarContainer.setMaxWidth(GameConfig.GAME_WORLD_WIDTH);
            topBarContainer.setPrefWidth(GameConfig.GAME_WORLD_WIDTH);
        }
    }
    
    public void updateLevel(int currentLevel) {
        updateLevel(currentLevel, null);
    }
    
    public void updateLevel(int currentLevel, String levelName) {
        if (levelValueLabel != null) {
            levelValueLabel.setText(String.valueOf(currentLevel));
        }
        if (levelNameLabel != null) {
            // Получаем локализованное название уровня (обычного или бонусного)
            String displayName = getLocalizedLevelName(currentLevel);
            // Добавляем приписку (DEBUG) / (ОТЛАДКА) если уровень запущен в debug режиме
            if (app != null && app.isDebugMode()) {
                String debugSuffix = localizationManager.get("debug.levels.suffix");
                displayName = displayName + " " + debugSuffix;
            }
            levelNameLabel.setText(displayName);
        }
    }
    
    /**
     * Получить локализованное название уровня (только название, без префикса "Уровень X:" или "Level X:")
     */
    private String getLocalizedLevelName(int levelNumber) {
        String fullName;
        com.arcadeblocks.config.LevelConfig.LevelData levelData = null;
        if (com.arcadeblocks.config.BonusLevelConfig.isBonusLevel(levelNumber)) {
            var bonusData = com.arcadeblocks.config.BonusLevelConfig.getLevelData(levelNumber);
            if (bonusData == null) {
                return "";
            }
            fullName = com.arcadeblocks.localization.LocalizationManager.getInstance()
                .getOrDefault(bonusData.getNameKey(), bonusData.getName());
        } else {
            levelData = com.arcadeblocks.config.LevelConfig.getLevel(levelNumber);
            if (levelData == null) {
                return "";
            }
            fullName = levelData.getName();
        }
        
        // Извлекаем часть после "Уровень X:" или "Level X:" для русской и английской локализации
        // Пробуем найти двоеточие после номера уровня
        String levelPrefixPattern = "(?i)(Уровень|Level)\\s+\\d+\\s*:?\\s*";
        String displayName = fullName.replaceFirst(levelPrefixPattern, "").trim();
        
        // Если паттерн не сработал, пытаемся найти двоеточие вручную
        if (displayName.equals(fullName)) {
            int colonIndex = fullName.indexOf(":");
            if (colonIndex != -1 && colonIndex + 1 < fullName.length()) {
                displayName = fullName.substring(colonIndex + 1).trim();
            } else {
                displayName = fullName;
            }
        }

        boolean isLBreakout = levelData != null 
            && levelData.getLevelFormat() == com.arcadeblocks.config.LevelConfig.LevelFormat.LBREAKOUT;
        boolean missingTitle = displayName.isBlank() || (fullName != null && fullName.trim().endsWith(":"));
        if (isLBreakout) {
            String fallbackTitle = com.arcadeblocks.config.LevelConfig.getLBreakoutHdTitle(levelNumber);
            if (fallbackTitle != null && !fallbackTitle.isBlank()) {
                // HUD всегда показывает эталонное название LBreakoutHD, чтобы не зависеть от локализации/ключей
                displayName = fallbackTitle;
            }
        }

        return displayName.isBlank() ? fullName : displayName;
    }
    
    public void refreshHighScores() {
        // Рекорды можно выводить в отдельный экран — оставлено для совместимости
    }
    
    public void updateScore(int score) {
        if (scoreValueLabel != null) {
            scoreValueLabel.setText(String.format("%,d", Math.max(0, score)).replace(',', ' '));
        }
    }

    public void playGameOverVisuals() {
        if (gameOverVisualsApplied) {
            return;
        }
        gameOverVisualsApplied = true;

        if (topBarContainer != null) {
            fadeTop = new FadeTransition(Duration.millis(450), topBarContainer);
            fadeTop.setToValue(0.0);
            fadeTop.setInterpolator(Interpolator.EASE_BOTH);
            fadeTop.setOnFinished(e -> {
                topBarContainer.setVisible(false);
                topBarContainer.setManaged(false);
            });
            fadeTop.play();
        }

        if (gameOverOverlay != null) {
            gameOverOverlay.setVisible(true);
            overlayTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(gameOverOverlay.opacityProperty(), gameOverOverlay.getOpacity(), Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(350),
                    new KeyValue(gameOverOverlay.opacityProperty(), 0.5, Interpolator.EASE_BOTH)
                )
            );
            overlayTimeline.play();
        }
    }
    
    public void updateLives(int lives) {
        if (livesValueLabel != null) {
            livesValueLabel.setText(String.valueOf(Math.max(0, lives)));
        }
    }
    
    public void updateTime(String time) {
        if (timeValueLabel != null) {
            timeValueLabel.setText(time);
        }
    }
    
    public void showBossHealth(String text) {
        if (bossHealthLabel != null) {
            bossHealthLabel.setText(text);
            bossHealthLabel.setVisible(true);
            bossHealthLabel.setManaged(true);
        }
    }
    
    public void updateBossHealthText(String text) {
        if (bossHealthLabel != null && bossHealthLabel.isVisible()) {
            bossHealthLabel.setText(text);
        }
    }
    
    public void hideBossHealth() {
        if (bossHealthLabel != null) {
            bossHealthLabel.setVisible(false);
            bossHealthLabel.setManaged(false);
        }
    }
    
    public VBox getGameArea() {
        return null;
    }
    
    public VBox getGameContainer() {
        return null;
    }
    
    public VBox getInfoPanel() {
        return null;
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимации
        if (fadeTop != null) {
            fadeTop.stop();
            fadeTop = null;
        }
        if (overlayTimeline != null) {
            overlayTimeline.stop();
            overlayTimeline = null;
        }
        
        // Отвязываем слушателей ResponsiveLayoutHelper
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Освобождаем изображения перед удалением children
        // Это предотвращает утечки памяти от com.sun.prism.image.* буферов
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // Очищаем children
        getChildren().clear();
    }
}
