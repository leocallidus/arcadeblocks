package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.GameLine;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.ui.util.UINodeCleanup;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно выбора игровой линии перед окном сохранений
 */
public class GameLineSelectionView extends StackPane implements SupportsCleanup {

    private final ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();

    private VBox contentBox;
    private VBox linesListBox;
    private ImageView lineImageView;
    private Text descriptionText;
    private Button selectButton;
    private Button backButton;

    private final List<GameLineInfo> gameLines = new ArrayList<>();
    private int currentLineIndex = 0;
    private Button[] lineButtons;

    private FadeTransition fadeIn;
    private FadeTransition closeFadeTransition;
    private ScaleTransition closeScaleTransition;
    private boolean cleanedUp = false;
    private volatile boolean isClosing = false;

    public GameLineSelectionView(ArcadeBlocksApp app) {
        this.app = app;
        initGameLines();
        initializeUI();
        setupKeyHandler();
        ResponsiveLayoutHelper.bindToStage(this);
        setUserData("fullScreenOverlay");
    }

    private void initGameLines() {
        gameLines.add(new GameLineInfo(
            "arcade_blocks",
            "gameline.arcade_blocks.name",
            "gameline.arcade_blocks.description",
            "assets/textures/arcadeblocks_logo_vertical.png",
            true
        ));
        gameLines.add(new GameLineInfo(
            "arcade_blocks_bonus",
            "gameline.arcade_blocks_bonus.name",
            "gameline.arcade_blocks_bonus.description",
            "assets/textures/arcadebonusline.png",
            true  // Теперь доступен
        ));
        gameLines.add(new GameLineInfo(
            "lbreakout1",
            "gameline.lbreakout1.name",
            "gameline.lbreakout1.description",
            "assets/textures/chapter_lbreakout1.png",
            true
        ));
        gameLines.add(new GameLineInfo(
            "lbreakout2",
            "gameline.lbreakout2.name",
            "gameline.lbreakout2.description",
            "assets/textures/lbreakouthd256.png",
            false
        ));
        gameLines.add(new GameLineInfo(
            "lbreakout2_community",
            "gameline.lbreakout2_community.name",
            "gameline.lbreakout2_community.description",
            "assets/textures/lbreakouthd256.png",
            false
        ));
    }

    private void initializeUI() {
        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setStyle("-fx-background-color: rgba(15, 15, 28, 0.92);");

        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "gameline.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 42));
        titleLabel.setTextFill(Color.web("#5DF2FF"));
        titleLabel.setEffect(new javafx.scene.effect.DropShadow(20, Color.web("#5DF2FF")));

        // Основной контейнер: слева список, справа картинка и описание
        HBox mainContainer = new HBox(40);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(30, 60, 30, 60));

        // Левая часть - список линий
        linesListBox = new VBox(12);
        linesListBox.setAlignment(Pos.TOP_LEFT);
        linesListBox.setPadding(new Insets(20));
        linesListBox.setStyle(
            "-fx-background-color: rgba(10, 10, 25, 0.6);" +
            "-fx-background-radius: 15;" +
            "-fx-border-color: rgba(93, 242, 255, 0.3);" +
            "-fx-border-radius: 15;" +
            "-fx-border-width: 1;"
        );
        linesListBox.setMinWidth(320);

        lineButtons = new Button[gameLines.size()];
        for (int i = 0; i < gameLines.size(); i++) {
            GameLineInfo line = gameLines.get(i);
            Button btn = createLineButton(line, i);
            lineButtons[i] = btn;
            linesListBox.getChildren().add(btn);
        }

        // Правая часть - картинка и описание
        VBox rightPanel = new VBox(20);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle(
            "-fx-background-color: rgba(10, 10, 25, 0.6);" +
            "-fx-background-radius: 15;" +
            "-fx-border-color: rgba(93, 242, 255, 0.3);" +
            "-fx-border-radius: 15;" +
            "-fx-border-width: 1;"
        );
        rightPanel.setMinWidth(450);
        rightPanel.setMaxWidth(500);

        // Картинка линии
        Rectangle imageBackdrop = new Rectangle(280, 280);
        imageBackdrop.setArcWidth(20);
        imageBackdrop.setArcHeight(20);
        imageBackdrop.setFill(Color.rgb(18, 26, 42, 0.7));
        imageBackdrop.setStroke(Color.rgb(93, 242, 255, 0.4));
        imageBackdrop.setStrokeWidth(1.2);

        lineImageView = new ImageView();
        lineImageView.setPreserveRatio(true);
        lineImageView.setSmooth(true);
        lineImageView.setFitWidth(260);
        lineImageView.setFitHeight(260);

        StackPane imageWrapper = new StackPane(imageBackdrop, lineImageView);
        imageWrapper.setAlignment(Pos.CENTER);

        // Описание
        descriptionText = new Text();
        descriptionText.setFont(Font.font("Orbitron", FontWeight.NORMAL, 16));
        descriptionText.setFill(Color.rgb(220, 236, 255, 0.9));
        descriptionText.setTextAlignment(TextAlignment.CENTER);
        descriptionText.setWrappingWidth(420);

        rightPanel.getChildren().addAll(imageWrapper, descriptionText);

        mainContainer.getChildren().addAll(linesListBox, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // Кнопки внизу
        HBox buttonsBox = new HBox(30);
        buttonsBox.setAlignment(Pos.CENTER);

        selectButton = createActionButton("gameline.select", GameConfig.NEON_GREEN, this::selectCurrentLine);
        backButton = createActionButton("gameline.back", GameConfig.NEON_YELLOW, this::goBack);
        // Переопределяем обработчик для backButton с звуком menu_back
        backButton.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_back");
            goBack();
        });

        buttonsBox.getChildren().addAll(selectButton, backButton);

        contentBox.getChildren().addAll(titleLabel, mainContainer, buttonsBox);
        getChildren().add(contentBox);

        // Начальное обновление
        updateSelection();

        // Анимация появления
        fadeIn = new FadeTransition(Duration.millis(400), contentBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private Button createLineButton(GameLineInfo line, int index) {
        Button btn = new Button();
        localizationManager.bind(btn, line.nameKey);
        btn.setPrefWidth(280);
        btn.setPrefHeight(45);
        btn.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        btn.setTextFill(Color.WHITE);
        btn.setCursor(javafx.scene.Cursor.HAND);

        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px;",
            GameConfig.DARK_BACKGROUND, line.available ? GameConfig.NEON_CYAN : "#555555"
        );
        btn.setStyle(baseStyle);

        if (!line.available) {
            btn.setOpacity(0.5);
        }

        btn.setOnMouseEntered(e -> {
            if (currentLineIndex != index) {
                currentLineIndex = index;
                app.getAudioManager().playSFXByName("menu_hover");
                updateSelection();
            }
            if (line.available) {
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
                st.setToX(1.03);
                st.setToY(1.03);
                st.play();
            }
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        btn.setOnAction(e -> {
            currentLineIndex = index;
            updateSelection();
            if (line.available) {
                app.getAudioManager().playSFXByName("menu_select");
                selectCurrentLine();
            } else {
                app.getAudioManager().playSFXByName("menu_error");
            }
        });

        return btn;
    }

    private Button createActionButton(String key, String color, Runnable action) {
        Button btn = new Button();
        localizationManager.bind(btn, key);
        btn.setPrefSize(GameConfig.MENU_BUTTON_WIDTH, GameConfig.MENU_BUTTON_HEIGHT);
        btn.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        btn.setTextFill(Color.WHITE);
        btn.setCursor(javafx.scene.Cursor.HAND);

        String style = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND, color, color
        );
        btn.setStyle(style);

        btn.setOnMouseEntered(e -> {
            app.getAudioManager().playSFXByName("menu_hover");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        btn.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_select");
            action.run();
        });

        return btn;
    }

    private void updateSelection() {
        GameLineInfo selected = gameLines.get(currentLineIndex);

        // Обновляем стили кнопок
        for (int i = 0; i < lineButtons.length; i++) {
            GameLineInfo line = gameLines.get(i);
            boolean isSelected = (i == currentLineIndex);
            String borderColor = line.available ? (isSelected ? GameConfig.NEON_PINK : GameConfig.NEON_CYAN) : "#555555";
            String bgColor = isSelected ? "rgba(93, 242, 255, 0.15)" : GameConfig.DARK_BACKGROUND;

            lineButtons[i].setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: %s; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;" +
                (isSelected ? "-fx-effect: dropshadow(gaussian, " + borderColor + ", 10, 0.5, 0, 0);" : ""),
                bgColor, borderColor, isSelected ? "2px" : "1px"
            ));
        }

        // Обновляем картинку
        try {
            Image img = ImageCache.get(selected.imagePath);
            lineImageView.setImage(img);
        } catch (Exception e) {
            lineImageView.setImage(null);
        }

        // Обновляем описание
        String desc = localizationManager.get(selected.descriptionKey);
        if (!selected.available) {
            desc += "\n\n" + localizationManager.get("gameline.unavailable");
        }
        descriptionText.setText(desc);

        // Обновляем кнопку выбора
        selectButton.setDisable(!selected.available);
        selectButton.setOpacity(selected.available ? 1.0 : 0.5);
    }

    private void setupKeyHandler() {
        setOnKeyPressed(this::handleKeyPressed);
        setFocusTraversable(true);
        requestFocus();
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        switch (code) {
            case UP -> {
                event.consume();
                navigateUp();
            }
            case DOWN -> {
                event.consume();
                navigateDown();
            }
            case ENTER, SPACE -> {
                event.consume();
                if (gameLines.get(currentLineIndex).available) {
                    app.getAudioManager().playSFXByName("menu_select");
                    selectCurrentLine();
                } else {
                    app.getAudioManager().playSFXByName("menu_error");
                }
            }
            case ESCAPE -> {
                event.consume();
                app.getAudioManager().playSFXByName("menu_back");
                goBack();
            }
        }
    }

    private void navigateUp() {
        if (currentLineIndex > 0) {
            currentLineIndex--;
            app.getAudioManager().playSFXByName("menu_hover");
            updateSelection();
        }
    }

    private void navigateDown() {
        if (currentLineIndex < gameLines.size() - 1) {
            currentLineIndex++;
            app.getAudioManager().playSFXByName("menu_hover");
            updateSelection();
        }
    }

    private void selectCurrentLine() {
        GameLineInfo selected = gameLines.get(currentLineIndex);
        if (!selected.available) return;

        // Определяем enum GameLine по id
        GameLine gameLine = GameLine.fromId(selected.id);

        // Закрываем это окно и открываем SaveGameView с выбранной линией
        cleanup();
        FXGL.getGameScene().removeUINode(this);
        FXGL.getGameScene().addUINode(new SaveGameView(app, gameLine));
    }

    private void goBack() {
        if (isClosing) return;
        isClosing = true;

        closeFadeTransition = new FadeTransition(Duration.millis(300), this);
        closeFadeTransition.setFromValue(1.0);
        closeFadeTransition.setToValue(0.0);

        closeScaleTransition = new ScaleTransition(Duration.millis(300), this);
        closeScaleTransition.setFromX(1.0);
        closeScaleTransition.setFromY(1.0);
        closeScaleTransition.setToX(0.9);
        closeScaleTransition.setToY(0.9);

        closeFadeTransition.setOnFinished(e -> {
            if (closeFadeTransition != null) {
                closeFadeTransition.stop();
                closeFadeTransition = null;
            }
            if (closeScaleTransition != null) {
                closeScaleTransition.stop();
                closeScaleTransition = null;
            }
            cleanup();
            FXGL.getGameScene().removeUINode(this);
        });

        closeFadeTransition.play();
        closeScaleTransition.play();
    }

    @Override
    public void cleanup() {
        if (cleanedUp) return;
        cleanedUp = true;

        setOnKeyPressed(null);
        if (fadeIn != null) {
            fadeIn.stop();
            fadeIn = null;
        }
        if (closeFadeTransition != null) {
            closeFadeTransition.stop();
            closeFadeTransition = null;
        }
        if (closeScaleTransition != null) {
            closeScaleTransition.stop();
            closeScaleTransition = null;
        }
        if (lineButtons != null) {
            for (Button btn : lineButtons) {
                if (btn != null) {
                    btn.setOnAction(null);
                    btn.setOnMouseEntered(null);
                    btn.setOnMouseExited(null);
                    btn.textProperty().unbind();
                }
            }
        }
        if (selectButton != null) {
            selectButton.setOnAction(null);
            selectButton.setOnMouseEntered(null);
            selectButton.setOnMouseExited(null);
            selectButton.textProperty().unbind();
        }
        if (backButton != null) {
            backButton.setOnAction(null);
            backButton.setOnMouseEntered(null);
            backButton.setOnMouseExited(null);
            backButton.textProperty().unbind();
        }
        UINodeCleanup.cleanupNode(this);
    }

    private static class GameLineInfo {
        final String id;
        final String nameKey;
        final String descriptionKey;
        final String imagePath;
        final boolean available;

        GameLineInfo(String id, String nameKey, String descriptionKey, String imagePath, boolean available) {
            this.id = id;
            this.nameKey = nameKey;
            this.descriptionKey = descriptionKey;
            this.imagePath = imagePath;
            this.available = available;
        }
    }
}
