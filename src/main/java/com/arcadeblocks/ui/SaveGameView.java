package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.GameLine;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.MainMenuView;
import com.arcadeblocks.utils.SaveManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно сохранений игры
 */
public class SaveGameView extends StackPane {
    
    private ArcadeBlocksApp app;
    private SaveManager saveManager;
    private final LocalizationManager localizationManager;
    private GameLine gameLine;
    private VBox contentBox;
    private List<SaveSlot> saveSlots;
    private Button backButton;
    private VBox saveSlotsContainer;
    private int currentSlotIndex = 0;
    private ScaleTransition titlePulseAnimation;
    private FadeTransition fadeIn;
    
    // Анимации закрытия
    private FadeTransition closeFadeTransition;
    private ScaleTransition closeScaleTransition;
    private volatile boolean isClosing = false;
    
    public SaveGameView(ArcadeBlocksApp app) {
        this(app, GameLine.ARCADE_BLOCKS);
    }
    
    public SaveGameView(ArcadeBlocksApp app, GameLine gameLine) {
        this.app = app;
        this.gameLine = gameLine;
        this.saveManager = app.getSaveManager();
        this.localizationManager = LocalizationManager.getInstance();
        this.saveSlots = new ArrayList<>();
        
        initializeUI();
        setupKeyHandler();
    }
    
    private void initializeUI() {
        // КРИТИЧНО: НЕ создаем свой собственный фон!
        // SaveGameView должен быть прозрачным и показываться поверх существующего MainMenuView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Создание контентного контейнера
        contentBox = new VBox(30);
        contentBox.setAlignment(Pos.CENTER);
        
        // Полупрозрачный темный фон, MainMenuView виден под ним
        contentBox.setStyle("-fx-background-color: rgba(15, 15, 28, 0.85);");
        
        // Заголовок
        Label titleLabel = createTitleLabel();
        
        // Контейнеры сохранений и управление
        saveSlotsContainer = createSaveSlotsContainer();
        HBox slotsRow = new HBox(30);
        slotsRow.setAlignment(Pos.CENTER);
        slotsRow.getChildren().add(saveSlotsContainer);
        
        // Кнопка "Назад в главное меню"
        backButton = createBackButton();
        
        contentBox.getChildren().addAll(titleLabel, slotsRow, backButton);
        
        // КРИТИЧНО: Добавляем только contentBox, без backgroundImageView
        getChildren().add(contentBox);
        
        // Анимация появления
        fadeIn = new FadeTransition(Duration.seconds(1.0), contentBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            if (fadeIn != null) {
                fadeIn.stop();
                fadeIn = null;
            }
        });
        fadeIn.play();

        ResponsiveLayoutHelper.bindToStage(this, (width, height) -> {
            if (contentBox != null) {
                contentBox.setPrefSize(width, height);
                contentBox.setMinSize(width, height);
                contentBox.setMaxSize(width, height);
            }
        });
        setUserData("fullScreenOverlay");
    }
    
    private ImageView createBackgroundImage() {
        try {
            // Используем текущий фон главного меню из приложения, чтобы фон не менялся
            String backgroundPath = app != null && app.getCurrentMainMenuBackground() != null
                ? app.getCurrentMainMenuBackground()
                : com.arcadeblocks.config.AudioConfig.MAIN_MENU_BACKGROUND;
            
            Image backgroundImage = ImageCache.get(backgroundPath);
            ImageView backgroundImageView = new ImageView(backgroundImage);
            
            // Настройка размеров и масштабирования
            backgroundImageView.setFitWidth(GameConfig.GAME_WIDTH);
            backgroundImageView.setFitHeight(GameConfig.GAME_HEIGHT);
            backgroundImageView.setPreserveRatio(false);
            backgroundImageView.setSmooth(false);
            backgroundImageView.setCache(false);
            
            return backgroundImageView;
        } catch (Exception e) {
            System.err.println("Не удалось загрузить фоновое изображение: " + e.getMessage());
            // Создаем пустое изображение как fallback
            ImageView fallbackImageView = new ImageView();
            fallbackImageView.setFitWidth(GameConfig.GAME_WIDTH);
            fallbackImageView.setFitHeight(GameConfig.GAME_HEIGHT);
            fallbackImageView.setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
            return fallbackImageView;
        }
    }
    
    private Label createTitleLabel() {
        Label titleLabel = new Label();
        // Используем название игровой линии в заголовке
        String titleKey = gameLine == GameLine.ARCADE_BLOCKS ? "savegame.title" : gameLine.getNameKey();
        localizationManager.bind(titleLabel, titleKey);
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 10, 0.5, 0, 0);");
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Анимация пульсации
        titlePulseAnimation = new ScaleTransition(Duration.seconds(2.0), titleLabel);
        titlePulseAnimation.setFromX(1.0);
        titlePulseAnimation.setFromY(1.0);
        titlePulseAnimation.setToX(1.05);
        titlePulseAnimation.setToY(1.05);
        titlePulseAnimation.setAutoReverse(true);
        titlePulseAnimation.setCycleCount(ScaleTransition.INDEFINITE);
        titlePulseAnimation.play();
        
        return titleLabel;
    }
    
    private VBox createSaveSlotsContainer() {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPrefWidth(1000);
        refreshSaveSlots(container);
        return container;
    }

    private void refreshSaveSlots() {
        if (saveSlotsContainer != null) {
            refreshSaveSlots(saveSlotsContainer);
        }
    }

    private void refreshSaveSlots(VBox container) {
        saveManager.awaitPendingWrites();
        container.getChildren().clear();
        saveSlots.clear();
        for (int i = 0; i < 4; i++) {
            SaveSlot saveSlot = new SaveSlot(i + 1);
            saveSlots.add(saveSlot);
            container.getChildren().add(saveSlot.getContainer());
        }
        currentSlotIndex = saveSlots.isEmpty() ? 0 : Math.min(currentSlotIndex, saveSlots.size() - 1);
        if (currentSlotIndex < 0) {
            currentSlotIndex = 0;
        }
        updateSlotHighlight();
    }
    
    private Button createBackButton() {
        Button button = new Button();
        localizationManager.bind(button, "savegame.button.back");
        button.setPrefSize(GameConfig.MENU_BUTTON_WIDTH * 2, GameConfig.MENU_BUTTON_HEIGHT); // Увеличиваем ширину в 2 раза
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
        String buttonStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
        );
        
        button.setStyle(buttonStyle);
        
        // Hover эффекты
        button.setOnMouseEntered(e -> {
            app.getAudioManager().playSFXByName("menu_hover");
            
            String hoverStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
            );
            
            button.setUserData(button.getStyle());
            button.setStyle(hoverStyle);
            
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.setOnFinished(e2 -> scaleUp.stop());
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            if (button.getUserData() != null) {
                button.setStyle((String) button.getUserData());
            }
            
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setOnFinished(e2 -> scaleDown.stop());
            scaleDown.play();
        });
        
        button.setOnAction(e -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            returnToMainMenu();
        });
        
        return button;
    }
    
    private void setupKeyHandler() {
        setFocusTraversable(true);
        requestFocus();
        
        setOnKeyPressed(event -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                event.consume();
                return;
            }
            
            switch (event.getCode()) {
                case UP:
                    moveUp();
                    event.consume();
                    break;
                case DOWN:
                    moveDown();
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    selectCurrentSlot();
                    event.consume();
                    break;
                case ESCAPE:
                    returnToMainMenu();
                    event.consume();
                    break;
                default:
                    break;
            }
        });
    }
    
    private void moveUp() {
        if (currentSlotIndex > 0) {
            // КРИТИЧНО: Проверяем app на null после cleanup()
            if (app != null) {
                app.getAudioManager().playSFXByName("menu_hover");
            }
            currentSlotIndex--;
            updateSlotHighlight();
        }
    }
    
    private void moveDown() {
        if (currentSlotIndex < saveSlots.size() - 1) {
            // КРИТИЧНО: Проверяем app на null после cleanup()
            if (app != null) {
                app.getAudioManager().playSFXByName("menu_hover");
            }
            currentSlotIndex++;
            updateSlotHighlight();
        }
    }
    
    private void selectCurrentSlot() {
        if (currentSlotIndex >= 0 && currentSlotIndex < saveSlots.size()) {
            // КРИТИЧНО: Проверяем app на null после cleanup()
            if (app != null) {
                app.getAudioManager().playSFXByName("menu_select");
                saveSlots.get(currentSlotIndex).loadSave();
            }
        }
    }
    
    private void updateSlotHighlight() {
        for (int i = 0; i < saveSlots.size(); i++) {
            saveSlots.get(i).setHighlighted(i == currentSlotIndex);
        }
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимацию пульсации заголовка
        if (titlePulseAnimation != null) {
            titlePulseAnimation.stop();
            titlePulseAnimation = null;
        }
        
        // КРИТИЧНО: Останавливаем анимацию появления
        if (fadeIn != null) {
            fadeIn.stop();
            fadeIn = null;
        }
        
        // КРИТИЧНО: Останавливаем анимации закрытия
        if (closeFadeTransition != null) {
            try {
                closeFadeTransition.stop();
            } catch (Exception ignored) {}
            closeFadeTransition = null;
        }
        if (closeScaleTransition != null) {
            try {
                closeScaleTransition.stop();
            } catch (Exception ignored) {}
            closeScaleTransition = null;
        }
        
        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        
        // КРИТИЧНО: Сначала очищаем обработчики и отвязываем textProperty() биндинги
        // ПЕРЕД обнулением кнопок, чтобы избежать утечек памяти от StringBinding
        if (backButton != null) {
            backButton.setOnAction(null);
            backButton.setOnMouseEntered(null);
            backButton.setOnMouseExited(null);
            // КРИТИЧНО: Отвязываем textProperty() у Button
            backButton.textProperty().unbind();
            backButton = null;
        }
        
        // Очищаем слоты сохранения
        if (saveSlots != null) {
            // КРИТИЧНО: Отвязываем textProperty() у всех кнопок слотов перед очисткой
            for (SaveSlot slot : saveSlots) {
                if (slot != null) {
                    if (slot.loadButton != null) {
                        slot.loadButton.textProperty().unbind();
                    }
                    if (slot.deleteButton != null) {
                        slot.deleteButton.textProperty().unbind();
                    }
                }
            }
            saveSlots.clear();
            saveSlots = null;
        }
        
        // КРИТИЧНО: Отвязываем textProperty() у всех Label компонентов перед удалением children
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Освобождаем изображения перед удалением children
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // Отвязываем слушателей ResponsiveLayoutHelper
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
        
        // Очищаем ссылки
        app = null;
        saveManager = null;
        contentBox = null;
        saveSlotsContainer = null;
    }
    
    /**
     * Рекурсивно отвязывает все textProperty() bindings в дереве компонентов
     */
    private void unbindAllTextProperties(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        
        // Отвязываем textProperty() у Labeled компонентов
        if (node instanceof javafx.scene.control.Labeled) {
            ((javafx.scene.control.Labeled) node).textProperty().unbind();
        }
        
        // Рекурсивно обрабатываем дочерние элементы
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                unbindAllTextProperties(child);
            }
        }
    }
    
    private void playCloseAnimation(Runnable onFinished) {
        // КРИТИЧНО: Проверяем, не закрываемся ли мы уже
        if (isClosing) {
            return;
        }
        isClosing = true;
        
        // КРИТИЧНО: Останавливаем старые анимации закрытия, если они еще активны
        if (closeFadeTransition != null) {
            try {
                closeFadeTransition.stop();
            } catch (Exception ignored) {}
            closeFadeTransition = null;
        }
        if (closeScaleTransition != null) {
            try {
                closeScaleTransition.stop();
            } catch (Exception ignored) {}
            closeScaleTransition = null;
        }
        
        // Анимация затухания
        closeFadeTransition = new FadeTransition(Duration.millis(300), this);
        closeFadeTransition.setFromValue(1.0);
        closeFadeTransition.setToValue(0.0);
        
        // Анимация уменьшения
        closeScaleTransition = new ScaleTransition(Duration.millis(300), this);
        closeScaleTransition.setFromX(1.0);
        closeScaleTransition.setFromY(1.0);
        closeScaleTransition.setToX(0.9);
        closeScaleTransition.setToY(0.9);
        
        // КРИТИЧНО: Очищаем ссылки на анимации после их завершения
        closeFadeTransition.setOnFinished(e -> {
            // Останавливаем обе анимации
            if (closeFadeTransition != null) {
                closeFadeTransition.stop();
                closeFadeTransition = null;
            }
            if (closeScaleTransition != null) {
                closeScaleTransition.stop();
                closeScaleTransition = null;
            }
            
            if (onFinished != null) {
                onFinished.run();
            }
        });
        closeScaleTransition.setOnFinished(e -> {
            // Ничего не делаем, основная логика в closeFadeTransition.onFinished
        });
        
        closeFadeTransition.play();
        closeScaleTransition.play();
    }
    
    private void returnToMainMenu() {
        // КРИТИЧНО: Запускаем анимацию закрытия перед возвратом в главное меню
        playCloseAnimation(() -> {
            // КРИТИЧНО: НЕ создаем новый MainMenuView!
            // MainMenuView уже существует под SaveGameView (overlay pattern)
            // Просто удаляем SaveGameView, чтобы показать существующий MainMenuView
            
            cleanup();
            FXGL.getGameScene().removeUINode(this);
            
            // КРИТИЧНО: Восстанавливаем фокус на MainMenuView для работы навигации по клавиатуре
            for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
                if (node instanceof MainMenuView) {
                    ((MainMenuView) node).restoreFocus();
                    break;
                }
            }
        });
    }

    private void showDeleteConfirmation(int slotNumber) {
        StackPane overlay = new StackPane();
        overlay.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(5,8,18,0.6);");
        overlay.setPickOnBounds(true);

        VBox dialog = new VBox(12);
        dialog.setAlignment(Pos.CENTER);
        dialog.setFillWidth(false);
        dialog.setSpacing(12);
        dialog.setPadding(new javafx.geometry.Insets(18, 22, 20, 22));
        dialog.setStyle(String.format(
            "-fx-background-color: rgba(12, 18, 34, 0.82); " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 16px; " +
            "-fx-background-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 16, 0.4, 0, 4);",
            GameConfig.NEON_PINK
        ));
        dialog.setPrefWidth(280);
        dialog.setMinWidth(260);
        dialog.setMaxWidth(280);
        dialog.setMinHeight(Region.USE_PREF_SIZE);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label();
        localizationManager.bind(title, "savegame.delete.title", () -> new Object[]{slotNumber});
        title.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(GameConfig.NEON_PINK));
        title.setWrapText(true);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setMaxWidth(240);

        Label description = new Label();
        localizationManager.bind(description, "savegame.delete.message");
        description.setFont(Font.font("Orbitron", 14));
        description.setTextFill(Color.web("#B8D7FF"));
        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.CENTER);
        description.setMaxWidth(240);

        HBox buttons = new HBox(14);
        buttons.setAlignment(Pos.CENTER);

        Button confirm = new Button();
        localizationManager.bind(confirm, "savegame.delete.yes");
        confirm.setPrefSize(108, 38);
        confirm.setFont(Font.font("Orbitron", FontWeight.BOLD, 15));
        confirm.setTextFill(Color.WHITE);
        confirm.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px; " +
            "-fx-effect: dropshadow(gaussian, %s, 10, 0.45, 0, 0);",
            GameConfig.NEON_PINK,
            GameConfig.NEON_PINK,
            GameConfig.NEON_PINK
        ));

        Button cancel = new Button();
        localizationManager.bind(cancel, "savegame.delete.no");
        cancel.setPrefSize(108, 38);
        cancel.setFont(Font.font("Orbitron", FontWeight.BOLD, 15));
        cancel.setTextFill(Color.WHITE);
        cancel.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px; " +
            "-fx-effect: dropshadow(gaussian, %s, 10, 0.45, 0, 0);",
            "rgba(20, 24, 36, 0.88)",
            GameConfig.NEON_CYAN,
            GameConfig.NEON_CYAN
        ));

        confirm.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_select");
            int activeSlot = saveManager.getActiveSaveSlot(gameLine);
            saveManager.clearSaveSlot(gameLine, slotNumber);
            if (activeSlot == slotNumber) {
                saveManager.awaitPendingWrites();
                int fallbackSlot = -1;
                for (int i = 1; i <= 4; i++) {
                    if (saveManager.getSaveInfo(gameLine, i) != null) {
                        fallbackSlot = i;
                        break;
                    }
                }
                if (fallbackSlot == -1) {
                    saveManager.setActiveSaveSlot(gameLine, 1);
                } else {
                    saveManager.setActiveSaveSlot(gameLine, fallbackSlot);
                }
                saveManager.clearGameSnapshot();
            } else {
                saveManager.awaitPendingWrites();
            }
            refreshSaveSlots();
            getChildren().remove(overlay);
        });

        cancel.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_select");
            getChildren().remove(overlay);
        });

        buttons.getChildren().addAll(confirm, cancel);
        dialog.getChildren().addAll(title, description, buttons);
        StackPane.setAlignment(dialog, Pos.CENTER);
        overlay.getChildren().add(dialog);
        getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        overlay.setFocusTraversable(true);
        overlay.requestFocus();
        overlay.setOnKeyPressed(evt -> {
            switch (evt.getCode()) {
                case ESCAPE:
                case BACK_SPACE:
                    app.getAudioManager().playSFXByName("menu_select");
                    getChildren().remove(overlay);
                    evt.consume();
                    break;
                case ENTER:
                case SPACE:
                    confirm.fire();
                    evt.consume();
                    break;
                default:
                    evt.consume();
                    break;
            }
        });
    }
    
    /**
     * Класс для представления слота сохранения
     */
    private class SaveSlot {
        private int slotNumber;
        private HBox container;
        private Button loadButton;
        private Button deleteButton;
        private Label infoLabel;
        private boolean hasSave;
        private SaveData saveData;
        
        public SaveSlot(int slotNumber) {
            this.slotNumber = slotNumber;
            this.hasSave = loadSaveData();
            createUI();
        }
        
        private boolean loadSaveData() {
            // Используем новый метод из SaveManager
            com.arcadeblocks.utils.SaveManager.SaveInfo saveInfo = saveManager.getSaveInfo(gameLine, slotNumber);
            
            if (saveInfo != null) {
                // Если слот дошел до конечного уровня линии, считаем его завершенным (failsafe)
                boolean completed = saveInfo.gameCompleted
                    || (gameLine != null && saveInfo.level >= gameLine.getEndLevel());
                if (completed && !saveInfo.gameCompleted) {
                    // Persist completion flag to keep UI consistent across sessions
                    saveManager.setGameCompletedForSlot(gameLine, slotNumber);
                }
                this.saveData = new SaveData(
                    saveInfo.name,
                    saveInfo.lastPlayTime,
                    saveInfo.level,
                    saveInfo.lives,
                    saveInfo.score,
                    saveInfo.difficulty,
                    saveInfo.playerName,
                    completed  // Добавляем флаг завершения игры
                );
                return true;
            }
            
            return false;
        }
        
        private void createUI() {
            container = new HBox(20);
            container.setPrefSize(1200, Region.USE_COMPUTED_SIZE);
            container.setMinWidth(1100);
            container.setMaxWidth(1200);
            container.setMinHeight(100);

			// Клип, чтобы содержимое (в т.ч. свечение/скейл лейбла) не выходило за границы контейнера
			Rectangle clip = new Rectangle();
			clip.widthProperty().bind(container.widthProperty());
			clip.heightProperty().bind(container.heightProperty());
			clip.setArcWidth(10);
			clip.setArcHeight(10);
			container.setClip(clip);

            deleteButton = createDeleteButton();

            // Проверяем, прошел ли игрок игру (используем флаг из SaveData)
            boolean gameCompleted = hasSave && saveData != null && saveData.gameCompleted;

            if (hasSave) {
                container.setAlignment(Pos.CENTER_LEFT);
                
                if (gameCompleted) {
                    // Игра пройдена - показываем специальное сообщение
                    infoLabel = createGameCompletedLabel();
                    infoLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(infoLabel, javafx.scene.layout.Priority.ALWAYS);
                    var textWidth = container.widthProperty().subtract(360);
                    infoLabel.maxWidthProperty().bind(textWidth);
                    infoLabel.prefWidthProperty().bind(textWidth);
                    
                    // Кнопки Play и Delete становятся неактивными
                    loadButton = createLoadButton();
                    loadButton.setDisable(true);
                    loadButton.setOpacity(0.5);
                    deleteButton.setDisable(true);
                    deleteButton.setOpacity(0.5);
                    
                    container.getChildren().addAll(loadButton, infoLabel, deleteButton);
                } else {
                    // Обычное сохранение
                    loadButton = createLoadButton();
                    infoLabel = createSaveInfoLabel();
                    infoLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(infoLabel, javafx.scene.layout.Priority.ALWAYS);
                    var textWidth = container.widthProperty().subtract(360);
                    infoLabel.maxWidthProperty().bind(textWidth);
                    infoLabel.prefWidthProperty().bind(textWidth);
                    container.getChildren().addAll(loadButton, infoLabel, deleteButton);
                }
            } else {
                container.setAlignment(Pos.CENTER);
                Label emptyLabel = createEmptySlotLabel();
                emptyLabel.setAlignment(Pos.CENTER);
                emptyLabel.setTextAlignment(TextAlignment.CENTER);
                
                // Используем Region для центрирования: Region слева, label по центру, кнопка справа
                Region leftSpacer = new Region();
                Region rightSpacer = new Region();
                HBox.setHgrow(leftSpacer, javafx.scene.layout.Priority.ALWAYS);
                HBox.setHgrow(rightSpacer, javafx.scene.layout.Priority.ALWAYS);
                
                container.getChildren().addAll(leftSpacer, emptyLabel, rightSpacer, deleteButton);
            }

            String containerStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
                GameConfig.DARK_BACKGROUND + "80", GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
            );
            container.setStyle(containerStyle);
        }
        
        private Button createLoadButton() {
            Button button = new Button();
            localizationManager.bind(button, "savegame.button.play");
            button.setPrefSize(150, 50);
            button.setMinWidth(140);
            button.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
            button.setTextFill(Color.WHITE);
            
            String buttonStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, %s, 3, 0.2, 0, 0);",
                GameConfig.NEON_GREEN, GameConfig.NEON_GREEN, GameConfig.NEON_GREEN
            );
            
            button.setStyle(buttonStyle);
            
            // Добавляем hover эффект
            button.setOnMouseEntered(e -> {
                app.getAudioManager().playSFXByName("menu_hover");
                
                String hoverStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
                    GameConfig.NEON_GREEN + "80", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
                );
                
                button.setUserData(button.getStyle());
                button.setStyle(hoverStyle);
                
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
                scaleUp.setToX(1.05);
                scaleUp.setToY(1.05);
                scaleUp.play();
            });
            
            button.setOnMouseExited(e -> {
                if (button.getUserData() != null) {
                    button.setStyle((String) button.getUserData());
                }
                
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                scaleDown.play();
            });
            
            button.setOnAction(e -> {
                app.getAudioManager().playSFXByName("menu_select");
                loadSave();
            });
            
            return button;
        }
        
        private Label createSaveInfoLabel() {
            Label label = new Label();
            label.setFont(Font.font("Orbitron", 14));
            label.setTextFill(Color.web(GameConfig.NEON_CYAN));
            label.setTextAlignment(TextAlignment.LEFT);
            
            // Создаем динамический биндинг для обновления при смене языка
            SaveData data = saveData;
            label.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                // Строим локализованную строку с использованием format для параметров
                StringBuilder info = new StringBuilder();
                info.append(data.name).append("\n");
                info.append(localizationManager.get("savegame.info.last_play")).append("\n").append(data.lastPlayTime).append("\n");
                info.append(localizationManager.get("savegame.info.level")).append(data.level).append(" | ");
                info.append(localizationManager.get("savegame.info.lives")).append(data.lives).append(" | ");
                info.append(localizationManager.get("savegame.info.score")).append(data.score);
                if (data.difficulty != null) {
                    info.append(" | ").append(localizationManager.get("savegame.info.difficulty")).append(data.difficulty.getDisplayName());
                }
                String playerDisplayName = (data.playerName != null && !data.playerName.isBlank())
                    ? data.playerName
                    : localizationManager.get("player.default");
                info.append("\n").append(localizationManager.get("savegame.info.player")).append(playerDisplayName);
                
                return info.toString();
            }, localizationManager.localeProperty()));
            
            label.setWrapText(true);
            label.setMaxWidth(1040);
            label.setPrefWidth(1020);
            // Клип по границам лейбла, чтобы свечение/масштаб не выходили за контейнер
            Rectangle labelClip = new Rectangle();
            labelClip.widthProperty().bind(label.widthProperty());
            labelClip.heightProperty().bind(label.heightProperty());
            label.setClip(labelClip);
            
            return label;
        }
        
        private Label createGameCompletedLabel() {
            Label label = new Label();
            label.setFont(Font.font("Orbitron", FontWeight.NORMAL, 14)); // Обычный шрифт вместо жирного
            label.setTextFill(Color.web(GameConfig.NEON_GREEN));
            label.setTextAlignment(TextAlignment.LEFT);
            
            // Создаем динамический биндинг для обновления при смене языка
            SaveData data = saveData;
            label.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                boolean isBonusLine = gameLine != null && gameLine != GameLine.ARCADE_BLOCKS;
                StringBuilder info = new StringBuilder();
                info.append(data.name).append("\n");
                if (isBonusLine) {
                    info.append(localizationManager.get("savegame.game_line_completed"));
                } else {
                    info.append(localizationManager.get("savegame.game_completed"));
                }
                info.append("\n\n");
                info.append(localizationManager.get("savegame.info.last_play")).append("\n").append(data.lastPlayTime).append("\n");
                info.append(localizationManager.get("savegame.info.score")).append(data.score).append("\n");
                if (data.difficulty != null) {
                    info.append(localizationManager.get("savegame.info.difficulty")).append(data.difficulty.getDisplayName()).append("\n");
                }
                String playerDisplayName = (data.playerName != null && !data.playerName.isBlank())
                    ? data.playerName
                    : localizationManager.get("player.default");
                info.append("\n").append(localizationManager.get("savegame.info.player")).append(playerDisplayName);
                
                return info.toString();
            }, localizationManager.localeProperty()));
            
            label.setWrapText(true);
            label.setMaxWidth(1040);
            label.setPrefWidth(1020);
            // Минимальный glow эффект для читаемости
            label.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_GREEN + ", 2, 0.15, 0, 0);");
            
            // Клип по границам лейбла
            Rectangle labelClip = new Rectangle();
            labelClip.widthProperty().bind(label.widthProperty());
            labelClip.heightProperty().bind(label.heightProperty());
            label.setClip(labelClip);
            
            return label;
        }
        
        private Label createEmptySlotLabel() {
            Label label = new Label();
            localizationManager.bind(label, "savegame.empty_slot", () -> new Object[]{slotNumber});
            label.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
            label.setTextFill(Color.web(GameConfig.NEON_YELLOW + "80"));
            label.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_YELLOW + ", 3, 0.3, 0, 0);");
            label.setWrapText(true);
            label.setMaxWidth(500);
            label.setCursor(javafx.scene.Cursor.HAND);
            // Клип по границам лейбла, чтобы свечение/масштаб не выходили за контейнер
            Rectangle labelClip = new Rectangle();
            labelClip.widthProperty().bind(label.widthProperty());
            labelClip.heightProperty().bind(label.heightProperty());
            label.setClip(labelClip);
            
            // Добавляем обработчик клика для пустого слота
            label.setOnMouseClicked(e -> {
                app.getAudioManager().playSFXByName("menu_select");
                createNewSave();
            });
            
            // Добавляем hover эффект для пустого слота
            label.setOnMouseEntered(e -> {
                app.getAudioManager().playSFXByName("menu_hover");
                label.setTextFill(Color.web(GameConfig.NEON_YELLOW));
                label.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_YELLOW + ", 6, 0.45, 0, 0);");
                
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), label);
                scaleUp.setToX(1.02);
                scaleUp.setToY(1.02);
                scaleUp.play();
            });
            
            label.setOnMouseExited(e -> {
                label.setTextFill(Color.web(GameConfig.NEON_YELLOW + "80"));
                label.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_YELLOW + ", 3, 0.3, 0, 0);");
                
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), label);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                scaleDown.play();
            });
            
            return label;
        }

        private Button createDeleteButton() {
            Button button = new Button();
            localizationManager.bind(button, "savegame.delete", () -> new Object[]{slotNumber});
            button.setPrefSize(180, 50);
            button.setMinWidth(170);
            button.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
            button.setTextFill(Color.WHITE);

            String baseStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, %s, 8, 0.45, 0, 0);",
                GameConfig.DARK_BACKGROUND,
                GameConfig.NEON_PINK,
                GameConfig.NEON_PINK
            );
            button.setStyle(baseStyle);

            if (!hasSave) {
                button.setDisable(true);
                button.setOpacity(0.55);
                button.setCursor(javafx.scene.Cursor.DEFAULT);
                return button;
            }

            button.setCursor(javafx.scene.Cursor.HAND);

            button.setOnMouseEntered(e -> {
                app.getAudioManager().playSFXByName("menu_hover");
                String hoverStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 12, 0.55, 0, 0);",
                    GameConfig.NEON_PINK + "55",
                    GameConfig.NEON_CYAN,
                    GameConfig.NEON_CYAN
                );
                button.setUserData(button.getStyle());
                button.setStyle(hoverStyle);
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(140), button);
                scaleUp.setToX(1.05);
                scaleUp.setToY(1.05);
                scaleUp.play();
            });

            button.setOnMouseExited(e -> {
                Object previous = button.getUserData();
                button.setStyle(previous instanceof String ? (String) previous : baseStyle);
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(140), button);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                scaleDown.play();
            });

            button.setOnAction(e -> {
                app.getAudioManager().playSFXByName("menu_select");
                showDeleteConfirmation(slotNumber);
            });

            return button;
        }
        
        public void setHighlighted(boolean highlighted) {
            if (highlighted) {
                String highlightStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 3px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-effect: dropshadow(gaussian, %s, 10, 0.5, 0, 0);",
                    GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
                );
                container.setStyle(highlightStyle);
            } else {
                String normalStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
                    GameConfig.DARK_BACKGROUND + "80", GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
                );
                container.setStyle(normalStyle);
            }
        }
        
        public void loadSave() {
            if (hasSave && saveData != null) {
                // Устанавливаем этот слот как активный для автоматического сохранения
                saveManager.setCurrentGameLine(gameLine);
                saveManager.setActiveSaveSlot(gameLine, slotNumber);
                
                // Запоминаем изначально выбранный слот для создания нового сохранения после Game Over
                app.setOriginalSaveSlot(slotNumber);
                
                // Используем новый метод загрузки из SaveManager
                if (saveManager.loadFromSlot(gameLine, slotNumber)) {
                    // КРИТИЧНО: Сохраняем ссылку на app ПЕРЕД cleanup(), так как cleanup() обнуляет app
                    ArcadeBlocksApp appRef = app;
                    int levelToStart = saveData.level;
                    
                    // КРИТИЧНО: Очищаем ресурсы ПЕРЕД переходом к игре
                    cleanup();
                    // Переходим к окну загрузки уровня
                    FXGL.getGameScene().removeUINode(SaveGameView.this);
                    if (appRef != null) {
                        appRef.clearUINodesSafely();
                        // Используем сохраненную ссылку, так как app уже null после cleanup()
                        appRef.startLevel(levelToStart, false);
                    }
                }
            } else {
                // Создаем новое сохранение
                createNewSave();
            }
        }
        
        private void createNewSave() {
            saveManager.setCurrentGameLine(gameLine);
            // Применяем выбранную пользователем сложность к новому слоту
            com.arcadeblocks.config.DifficultyLevel selectedDifficulty = saveManager.getDifficulty();
            saveManager.setGameDifficulty(selectedDifficulty);

            // Полностью очищаем данные слота перед созданием нового сохранения
            saveManager.clearSaveSlot(gameLine, slotNumber);
            saveManager.awaitPendingWrites();

            // Сбрасываем прогресс и начинаем новую игру
            saveManager.resetProgressPreservingSlots();
            saveManager.awaitPendingWrites();

            // Устанавливаем этот слот как активный для автоматического сохранения
            saveManager.setActiveSaveSlot(gameLine, slotNumber);

            // Обновляем текущие базовые значения для выбранной линии
            saveManager.setCurrentLevel(gameLine.getStartLevel());
            saveManager.setLives(selectedDifficulty.getLives());
            saveManager.setScore(0);
            
            // Запоминаем изначально выбранный слот для создания нового сохранения после Game Over
            app.setOriginalSaveSlot(slotNumber);
            
            // Автоматически сохраняем в этот слот
            saveManager.autoSaveToSlot(gameLine, slotNumber);
            saveManager.awaitPendingWrites();
            
            // КРИТИЧНО: Сохраняем ссылку на app ПЕРЕД cleanup(), так как cleanup() обнуляет app
            ArcadeBlocksApp appRef = app;
            
            // КРИТИЧНО: Очищаем ресурсы ПЕРЕД переходом к игре
            cleanup();
            FXGL.getGameScene().removeUINode(SaveGameView.this);
            if (appRef != null) {
                appRef.clearUINodesSafely();
                // Используем сохраненную ссылку, так как app уже null после cleanup()
                // Запускаем первый уровень выбранной игровой линии
                appRef.startLevel(gameLine.getStartLevel(), true);
            }
        }
        
        public HBox getContainer() {
            return container;
        }
    }
    
    /**
     * Класс для хранения данных сохранения
     */
    private static class SaveData {
        public final String name;
        public final String lastPlayTime;
        public final int level;
        public final int lives;
        public final int score;
        public final com.arcadeblocks.config.DifficultyLevel difficulty;
        public final String playerName;
        public final boolean gameCompleted;
        
        public SaveData(String name, String lastPlayTime, int level, int lives, int score, com.arcadeblocks.config.DifficultyLevel difficulty, String playerName, boolean gameCompleted) {
            this.name = name;
            this.lastPlayTime = lastPlayTime;
            this.level = level;
            this.lives = lives;
            this.score = score;
            this.difficulty = difficulty;
            this.playerName = playerName;
            this.gameCompleted = gameCompleted;
        }
        
        // Конструктор для обратной совместимости
        public SaveData(String name, String lastPlayTime, int level, int lives, int score, com.arcadeblocks.config.DifficultyLevel difficulty, String playerName) {
            this(name, lastPlayTime, level, lives, score, difficulty, playerName, false);
        }
    }
}
