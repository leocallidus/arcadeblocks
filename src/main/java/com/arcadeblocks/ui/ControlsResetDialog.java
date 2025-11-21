package com.arcadeblocks.ui;

import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Диалог об ошибке настроек управления и их сбросе
 */
public class ControlsResetDialog extends VBox {
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();

    public ControlsResetDialog() {
        initializeUI();
        show();
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30));
        setPrefSize(500, 350);
        
        // Центрируем диалог по экрану
        setLayoutX((GameConfig.GAME_WIDTH - 500) / 2.0);
        setLayoutY((GameConfig.GAME_HEIGHT - 350) / 2.0);
        
        // Стилизация диалога
        setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 15;
            -fx-border-color: %s;
            -fx-border-width: 3;
            -fx-border-radius: 15;
        """, 
        GameConfig.DARK_BACKGROUND, GameConfig.NEON_YELLOW));
        
        // Эффект тени
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(GameConfig.NEON_YELLOW));
        shadow.setRadius(20);
        shadow.setSpread(0.3);
        setEffect(shadow);
        
        // Иконка предупреждения
        Label iconLabel = new Label("⚠️");
        iconLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        iconLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "controls.reset.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_YELLOW));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(440);
        
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.web(GameConfig.NEON_YELLOW));
        titleShadow.setRadius(10);
        titleShadow.setSpread(0.5);
        titleLabel.setEffect(titleShadow);
        
        // Сообщение
        Label messageLabel = new Label();
        localizationManager.bind(messageLabel, "controls.reset.message");
        messageLabel.setFont(Font.font("Arial", 14));
        messageLabel.setTextFill(Color.web("#E0E0E0"));
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setStyle("-fx-line-spacing: 3;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(440);
        
        // Кнопка OK
        Button okButton = createStyledButton("controls.reset.ok", GameConfig.NEON_GREEN, this::hide);
        
        getChildren().addAll(iconLabel, titleLabel, messageLabel, okButton);
        
        // Обработчик ESC и Enter
        setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE || 
                event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                hide();
            }
        });
        
        // Установка фокуса
        setFocusTraversable(true);
        requestFocus();
    }
    
    private Button createStyledButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        button.setPrefSize(180, 45);
        
        button.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: %s;
            -fx-background-radius: 10;
            -fx-border-color: %s;
            -fx-border-width: 2;
            -fx-border-radius: 10;
        """, 
        color, 
        "#000000", 
        color));
        
        // Эффект тени
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setColor(Color.web(color));
        buttonShadow.setRadius(8);
        buttonShadow.setSpread(0.3);
        button.setEffect(buttonShadow);
        
        // Анимация наведения
        button.setOnMouseEntered(e -> {
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), button);
            scaleIn.setToX(1.05);
            scaleIn.setToY(1.05);
            scaleIn.play();
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
    
    private void show() {
        setOpacity(0);
        setScaleX(0.8);
        setScaleY(0.8);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setToValue(1.0);
        
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), this);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }
    
    private void hide() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), this);
        fadeOut.setToValue(0);
        
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), this);
        scaleOut.setToX(0.8);
        scaleOut.setToY(0.8);
        
        fadeOut.setOnFinished(e -> {
            if (getParent() != null) {
                javafx.scene.Parent parent = getParent();
                if (parent instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane) parent).getChildren().remove(this);
                } else if (parent instanceof javafx.scene.Group) {
                    ((javafx.scene.Group) parent).getChildren().remove(this);
                }
            }
        });
        
        fadeOut.play();
        scaleOut.play();
    }
}
