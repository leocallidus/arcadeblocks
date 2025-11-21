package com.arcadeblocks.ui;

import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.effect.DropShadow;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

/**
 * Диалог подтверждения сброса настроек к значениям по умолчанию
 */
public class ResetSettingsDialog extends VBox {
    
    private Runnable onReset;
    private Runnable onCancel;
    
    // Навигация по кнопкам
    private Button[] confirmButtons;
    private int currentButtonIndex = 0;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    
    public ResetSettingsDialog(Runnable onReset, Runnable onCancel) {
        this.onReset = onReset;
        this.onCancel = onCancel;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30));
        setPrefSize(450, 350);
        
        // Центрируем диалог по экрану
        setLayoutX((GameConfig.GAME_WIDTH - 450) / 2.0);
        setLayoutY((GameConfig.GAME_HEIGHT - 350) / 2.0);
        
        // Стилизация диалога
        setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 15;
            -fx-border-color: %s;
            -fx-border-width: 3;
            -fx-border-radius: 15;
        """, 
        GameConfig.DARK_BACKGROUND, GameConfig.NEON_ORANGE));
        
        // Эффект тени
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(GameConfig.NEON_ORANGE));
        shadow.setRadius(20);
        shadow.setSpread(0.3);
        setEffect(shadow);
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "reset.dialog.title");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_ORANGE));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.web(GameConfig.NEON_ORANGE));
        titleShadow.setRadius(10);
        titleShadow.setSpread(0.5);
        titleLabel.setEffect(titleShadow);
        
        // Предупреждающий текст
        Label warningLabel = new Label();
        localizationManager.bind(warningLabel, "reset.dialog.warning");
        warningLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        warningLabel.setTextFill(Color.web(GameConfig.NEON_YELLOW));
        warningLabel.setTextAlignment(TextAlignment.CENTER);
        
        DropShadow warningShadow = new DropShadow();
        warningShadow.setColor(Color.web(GameConfig.NEON_YELLOW));
        warningShadow.setRadius(8);
        warningShadow.setSpread(0.4);
        warningLabel.setEffect(warningShadow);
        
        // Основное сообщение
        Label messageLabel = new Label();
        localizationManager.bind(messageLabel, "reset.dialog.message");
        messageLabel.setFont(Font.font("Arial", 16));
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setStyle("-fx-line-spacing: 5;");
        
        // Список того, что будет сброшено
        VBox resetListBox = new VBox(8);
        resetListBox.setAlignment(Pos.CENTER_LEFT);
        
        Label listTitle = new Label();
        localizationManager.bind(listTitle, "reset.dialog.list.title");
        listTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        listTitle.setTextFill(Color.web(GameConfig.NEON_CYAN));
        resetListBox.getChildren().add(listTitle);
        
        String[] resetItemKeys = {
            "reset.dialog.list.item.audio",
            "reset.dialog.list.item.controls",
            "reset.dialog.list.item.gameplay",
            "reset.dialog.list.item.display"
        };
        
        for (String key : resetItemKeys) {
            Label itemLabel = new Label();
            localizationManager.bind(itemLabel, key);
            itemLabel.setFont(Font.font("Arial", 12));
            itemLabel.setTextFill(Color.web("#CCCCCC"));
            resetListBox.getChildren().add(itemLabel);
        }
        
        // Кнопки
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        
        Button resetButton = createStyledButton("reset.dialog.button.confirm", GameConfig.NEON_ORANGE, () -> {
            playMenuSelectSound();
            hide();
            if (onReset != null) onReset.run();
        });
        
        Button cancelButton = createStyledButton("reset.dialog.button.cancel", GameConfig.NEON_CYAN, () -> {
            playMenuBackSound();
            hide();
            if (onCancel != null) onCancel.run();
        });
        
        buttonsBox.getChildren().addAll(resetButton, cancelButton);
        
        // Сохраняем ссылки на кнопки для навигации
        confirmButtons = new Button[]{resetButton, cancelButton};
        
        getChildren().addAll(titleLabel, warningLabel, messageLabel, resetListBox, buttonsBox);
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();
        
        // Обработчик клавиш
        setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:
                    moveLeft();
                    event.consume();
                    break;
                case RIGHT:
                    moveRight();
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    selectCurrentButton();
                    event.consume();
                    break;
                case ESCAPE:
                    playMenuBackSound();
                    hide();
                    if (onCancel != null) onCancel.run();
                    event.consume();
                    break;
                default:
                    // Игнорируем остальные клавиши
                    break;
            }
        });
        
        // Делаем диалог фокусируемым
        setFocusTraversable(true);
        requestFocus();
    }
    
    private Button createStyledButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setPrefSize(130, 40);
        
        // Единый стиль кнопки (как при клавиатурной навигации)
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
        
        // Hover эффекты для мыши (совместимо с клавиатурной навигацией)
        button.setOnMouseEntered(e -> {
            playMenuHoverSound();
            
            // Временное визуальное выделение при наведении мыши
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
            
            // Сохраняем текущий стиль для восстановления
            button.setUserData(button.getStyle());
            button.setStyle(hoverStyle);
            
            // Анимация увеличения
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // Восстанавливаем исходный стиль
            if (button.getUserData() != null) {
                button.setStyle((String) button.getUserData());
            }
            
            // Анимация уменьшения
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
        
        button.setOnAction(e -> action.run());
        
        return button;
    }
    
    /**
     * Показать диалог сброса настроек
     */
    public static void show(Runnable onReset, Runnable onCancel) {
        ResetSettingsDialog dialog = new ResetSettingsDialog(onReset, onCancel);
        
        // Добавляем в игровую сцену
        com.almasb.fxgl.dsl.FXGL.getGameScene().addUINode(dialog);
        
        // Дополнительное центрирование после добавления на сцену
        javafx.application.Platform.runLater(() -> {
            // Получаем размеры игровой сцены
            double sceneWidth = com.almasb.fxgl.dsl.FXGL.getGameScene().getWidth();
            double sceneHeight = com.almasb.fxgl.dsl.FXGL.getGameScene().getHeight();
            
            // Центрируем диалог
            dialog.setLayoutX((sceneWidth - 450) / 2.0);
            dialog.setLayoutY((sceneHeight - 350) / 2.0);
            
            // Устанавливаем фокус для обработки клавиш
            dialog.requestFocus();
            dialog.setFocusTraversable(true);
        });
    }
    
    /**
     * Скрыть диалог
     */
    public void hide() {
        com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(this);
    }
    
    /**
     * Воспроизвести звук выбора в меню
     */
    private void playMenuSelectSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_select");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Воспроизвести звук возврата/отмены в меню
     */
    private void playMenuBackSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_back");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Воспроизвести звук наведения в меню
     */
    private void playMenuHoverSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_hover");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Перемещение влево по кнопкам
     */
    private void moveLeft() {
        if (currentButtonIndex > 0) {
            playMenuHoverSound();
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    /**
     * Перемещение вправо по кнопкам
     */
    private void moveRight() {
        if (currentButtonIndex < confirmButtons.length - 1) {
            playMenuHoverSound();
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    /**
     * Выбор текущей кнопки
     */
    private void selectCurrentButton() {
        if (confirmButtons != null && currentButtonIndex >= 0 && currentButtonIndex < confirmButtons.length) {
            playMenuSelectSound();
            confirmButtons[currentButtonIndex].fire();
        }
    }
    
    /**
     * Обновление визуального выделения кнопок
     */
    private void updateButtonHighlight() {
        if (confirmButtons == null) return;
        
        for (int i = 0; i < confirmButtons.length; i++) {
            Button button = confirmButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка - яркий цвет и увеличенный размер
                button.setStyle(
                    "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                    "-fx-text-fill: BLACK; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-width: 3px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 1.0, 0, 0);"
                );
                button.setScaleX(1.1);
                button.setScaleY(1.1);
            } else {
                // Обычная кнопка - стандартный стиль
                if (i == 0) { // Кнопка "Сбросить"
                    button.setStyle(
                        "-fx-background-color: " + GameConfig.NEON_ORANGE + "; " +
                        "-fx-text-fill: BLACK; " +
                        "-fx-border-color: " + GameConfig.NEON_ORANGE + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_ORANGE + ", 8, 0.3, 0, 0);"
                    );
                } else { // Кнопка "Отмена"
                    button.setStyle(
                        "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                        "-fx-text-fill: BLACK; " +
                        "-fx-border-color: " + GameConfig.NEON_CYAN + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 8, 0.3, 0, 0);"
                    );
                }
                button.setScaleX(1.0);
                button.setScaleY(1.0);
            }
        }
    }
}
