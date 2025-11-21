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
 * Диалог подтверждения закрытия игры после изменения настроек
 */
public class RestartGameDialog extends VBox implements SupportsCleanup {
    
    private final Runnable onClose;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private Button okButton;
    
    public RestartGameDialog(Runnable onClose) {
        this.onClose = onClose;
        initializeUI();
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(25);
        setPadding(new Insets(40));
        setPrefSize(900, 320);
        
        // Центрируем диалог по экрану
        setLayoutX((GameConfig.GAME_WIDTH - 900) / 2.0);
        setLayoutY((GameConfig.GAME_HEIGHT - 320) / 2.0);
        
        // КРИТИЧНО: Блокируем все события мыши и клавиатуры, чтобы пользователь не мог взаимодействовать с элементами под диалогом
        setPickOnBounds(true);
        setMouseTransparent(false);
        
        // Блокируем все события мыши
        setOnMousePressed(e -> e.consume());
        setOnMouseReleased(e -> e.consume());
        setOnMouseClicked(e -> e.consume());
        setOnMouseDragged(e -> e.consume());
        setOnMouseMoved(e -> e.consume());
        
        // Стилизация диалога
        setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 15;
            -fx-border-color: %s;
            -fx-border-width: 3;
            -fx-border-radius: 15;
        """, 
        GameConfig.DARK_BACKGROUND, GameConfig.NEON_PINK));
        
        // Эффект тени
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(GameConfig.NEON_PINK));
        shadow.setRadius(20);
        shadow.setSpread(0.3);
        setEffect(shadow);
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "restart.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_PINK));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.web(GameConfig.NEON_PINK));
        titleShadow.setRadius(12);
        titleShadow.setSpread(0.5);
        titleLabel.setEffect(titleShadow);
        
        // Сообщение
        Label messageLabel = new Label();
        localizationManager.bind(messageLabel, "restart.message");
        messageLabel.setFont(Font.font("Orbitron", 16));
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(800);
        messageLabel.setStyle("-fx-line-spacing: 5;");
        
        // Кнопка OK
        HBox buttonsBox = new HBox();
        buttonsBox.setAlignment(Pos.CENTER);
        
        okButton = createStyledButton("restart.button.ok", GameConfig.NEON_GREEN, () -> {
            playMenuSelectSound();
            cleanup();
            hide();
            if (onClose != null) onClose.run();
        });
        
        buttonsBox.getChildren().add(okButton);
        
        getChildren().addAll(titleLabel, messageLabel, buttonsBox);
        
        // Обработчик ENTER и ESC
        setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER || 
                event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                playMenuSelectSound();
                cleanup();
                hide();
                if (onClose != null) onClose.run();
                event.consume();
            }
        });
        
        // Делаем диалог фокусируемым
        setFocusTraversable(true);
    }
    
    private Button createStyledButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        button.setPrefSize(180, 50);
        
        button.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: #000000;
            -fx-background-radius: 10;
            -fx-border-color: %s;
            -fx-border-width: 2;
            -fx-border-radius: 10;
        """, 
        color, 
        color));
        
        // Эффект тени
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setColor(Color.web(color));
        buttonShadow.setRadius(10);
        buttonShadow.setSpread(0.4);
        button.setEffect(buttonShadow);
        
        // Анимация наведения
        button.setOnMouseEntered(e -> {
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), button);
            scaleIn.setToX(1.1);
            scaleIn.setToY(1.1);
            scaleIn.play();
            playMenuHoverSound();
        });
        
        button.setOnMouseExited(e -> {
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(100), button);
            scaleOut.setToX(1.0);
            scaleOut.setToY(1.0);
            scaleOut.play();
        });
        
        button.setOnAction(e -> action.run());
        
        return button;
    }
    
    /**
     * Показать диалог
     */
    public static void show(Runnable onClose) {
        // КРИТИЧНО: Создаем полупрозрачный фон, который блокирует все события
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        overlay.setPickOnBounds(true);
        overlay.setMouseTransparent(false);
        
        // Блокируем все события мыши на overlay
        overlay.setOnMousePressed(e -> e.consume());
        overlay.setOnMouseReleased(e -> e.consume());
        overlay.setOnMouseClicked(e -> e.consume());
        overlay.setOnMouseDragged(e -> e.consume());
        overlay.setOnMouseMoved(e -> e.consume());
        
        // Блокируем все события клавиатуры на overlay (кроме ENTER и ESC для диалога)
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() != javafx.scene.input.KeyCode.ENTER && 
                e.getCode() != javafx.scene.input.KeyCode.ESCAPE) {
                e.consume();
            }
        });
        overlay.setOnKeyReleased(e -> e.consume());
        overlay.setOnKeyTyped(e -> e.consume());
        
        RestartGameDialog dialog = new RestartGameDialog(onClose);
        
        // Добавляем диалог в overlay
        overlay.getChildren().add(dialog);
        
        // Добавляем overlay в игровую сцену
        com.almasb.fxgl.dsl.FXGL.getGameScene().addUINode(overlay);
        
        // Дополнительное центрирование после добавления на сцену
        javafx.application.Platform.runLater(() -> {
            // Получаем размеры игровой сцены
            double sceneWidth = com.almasb.fxgl.dsl.FXGL.getGameScene().getWidth();
            double sceneHeight = com.almasb.fxgl.dsl.FXGL.getGameScene().getHeight();
            
            // Центрируем диалог
            dialog.setLayoutX((sceneWidth - 900) / 2.0);
            dialog.setLayoutY((sceneHeight - 320) / 2.0);
            
            // Устанавливаем фокус для обработки клавиш
            overlay.setFocusTraversable(true);
            overlay.requestFocus();
        });
    }
    
    /**
     * Скрыть диалог
     */
    public void hide() {
        // КРИТИЧНО: Удаляем overlay (родительский элемент), а не сам диалог
        javafx.scene.Parent parent = getParent();
        if (parent != null) {
            com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(parent);
        } else {
            // Fallback: если родителя нет, удаляем сам диалог
            com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(this);
        }
    }
    
    /**
     * Очистка ресурсов
     */
    @Override
    public void cleanup() {
        // System.out.println("[RestartGameDialog] cleanup called");
        
        // Очищаем обработчики
        this.setOnKeyPressed(null);
        
        if (okButton != null) {
            okButton.setOnAction(null);
            okButton.setOnMouseEntered(null);
            okButton.setOnMouseExited(null);
            okButton.textProperty().unbind();
        }
        
        // Очищаем children
        getChildren().clear();
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
     * Воспроизвести звук наведения в меню
     */
    private void playMenuHoverSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_hover");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
}
