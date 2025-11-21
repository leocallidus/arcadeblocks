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

import java.util.Map;

/**
 * Диалог подтверждения для несохраненных изменений настроек
 */
public class UnsavedChangesDialog extends VBox {
    
    private final Runnable onSave;
    private final Runnable onDiscard;
    private final Runnable onCancel;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    
    public UnsavedChangesDialog(Runnable onSave, Runnable onDiscard, Runnable onCancel) {
        this.onSave = onSave;
        this.onDiscard = onDiscard;
        this.onCancel = onCancel;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30));
        setPrefSize(500, 400);
        
        // Центрируем диалог по экрану
        setLayoutX((GameConfig.GAME_WIDTH - 500) / 2.0);
        setLayoutY((GameConfig.GAME_HEIGHT - 400) / 2.0);
        
        // Стилизация диалога
        setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 15;
            -fx-border-color: %s;
            -fx-border-width: 3;
            -fx-border-radius: 15;
        """, 
        GameConfig.DARK_BACKGROUND, GameConfig.NEON_CYAN));
        
        // Эффект тени
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(GameConfig.NEON_CYAN));
        shadow.setRadius(20);
        shadow.setSpread(0.3);
        setEffect(shadow);
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "unsaved.title");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_PINK));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.web(GameConfig.NEON_PINK));
        titleShadow.setRadius(10);
        titleShadow.setSpread(0.5);
        titleLabel.setEffect(titleShadow);
        
        // Сообщение
        Label messageLabel = new Label();
        localizationManager.bind(messageLabel, "unsaved.message");
        messageLabel.setFont(Font.font("Arial", 16));
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setStyle("-fx-line-spacing: 5;");
        
        // Список изменений
        VBox changesBox = new VBox();
        changesBox.setSpacing(5);
        changesBox.setAlignment(Pos.CENTER_LEFT);
        
        Label changesTitle = new Label();
        localizationManager.bind(changesTitle, "unsaved.changes.title");
        changesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        changesTitle.setTextFill(Color.web(GameConfig.NEON_GREEN));
        changesBox.getChildren().add(changesTitle);
        
        // Добавляем изменения (будет заполнено при показе диалога)
        changesBox.setId("changes-container");
        
        // Кнопки
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        
        Button saveButton = createStyledButton("unsaved.button.save", GameConfig.NEON_GREEN, () -> {
            playMenuSelectSound();
            hide();
            if (onSave != null) onSave.run();
        });
        
        Button discardButton = createStyledButton("unsaved.button.discard", GameConfig.NEON_YELLOW, () -> {
            playMenuSelectSound();
            hide();
            if (onDiscard != null) onDiscard.run();
        });
        
        Button cancelButton = createStyledButton("unsaved.button.cancel", GameConfig.NEON_PINK, () -> {
            playMenuBackSound();
            hide();
            if (onCancel != null) onCancel.run();
        });
        
        buttonsBox.getChildren().addAll(saveButton, discardButton, cancelButton);
        
        getChildren().addAll(titleLabel, messageLabel, changesBox, buttonsBox);
        
        // Обработчик ESC
        setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                playMenuBackSound();
                hide();
                if (onCancel != null) onCancel.run();
            }
        });
    }
    
    private Button createStyledButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setPrefSize(150, 40);
        
        button.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: %s;
            -fx-background-radius: 10;
            -fx-border-color: %s;
            -fx-border-width: 2;
            -fx-border-radius: 10;
        """, 
        color, 
        Color.BLACK, 
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
     * Показать диалог с информацией об изменениях
     */
    public static void show(Map<String, String> changes, Runnable onSave, Runnable onDiscard, Runnable onCancel) {
        UnsavedChangesDialog dialog = new UnsavedChangesDialog(onSave, onDiscard, onCancel);
        LocalizationManager localizationManager = LocalizationManager.getInstance();
        
        // Заполняем список изменений
        VBox changesContainer = (VBox) dialog.lookup("#changes-container");
        if (changesContainer != null) {
            for (Map.Entry<String, String> change : changes.entrySet()) {
                String displayKey = localizationManager.getOrDefault(
                    "unsaved.change." + change.getKey(),
                    change.getKey()
                );
                String itemText = localizationManager.format("unsaved.change.item", displayKey, change.getValue());
                Label changeLabel = new Label(itemText);
                changeLabel.setFont(Font.font("Arial", 12));
                changeLabel.setTextFill(Color.web("#CCCCCC"));
                changesContainer.getChildren().add(changeLabel);
            }
        }
        
        // Добавляем в игровую сцену
        com.almasb.fxgl.dsl.FXGL.getGameScene().addUINode(dialog);
        
        // Дополнительное центрирование после добавления на сцену
        javafx.application.Platform.runLater(() -> {
            // Получаем размеры игровой сцены
            double sceneWidth = com.almasb.fxgl.dsl.FXGL.getGameScene().getWidth();
            double sceneHeight = com.almasb.fxgl.dsl.FXGL.getGameScene().getHeight();
            
            // Центрируем диалог
            dialog.setLayoutX((sceneWidth - 500) / 2.0);
            dialog.setLayoutY((sceneHeight - 400) / 2.0);
            
            // Устанавливаем фокус для обработки клавиш
            dialog.requestFocus();
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
}
