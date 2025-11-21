package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.gameplay.BonusType;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.utils.ImageCache;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BonusTimerView extends VBox {
    private Map<BonusType, BonusTimerItem> activeBonuses;
    private static final double CONTAINER_WIDTH = 200;
    private static final double CONTAINER_HEIGHT = 300;
    private static final double BONUS_ITEM_HEIGHT = 50;
    private static final double MARGIN = 10;
    private static final LocalizationManager LOCALIZATION = LocalizationManager.getInstance();
    
    public BonusTimerView() {
        activeBonuses = new HashMap<>();
        setupUI();
    }
    
    private void setupUI() {
        setAlignment(Pos.TOP_LEFT);
        setPrefWidth(CONTAINER_WIDTH);
        setPrefHeight(CONTAINER_HEIGHT);
        setPadding(new Insets(MARGIN));
        setSpacing(MARGIN);
        setMouseTransparent(true);
        setPickOnBounds(false);
        setManaged(false);
    }
    
    public void addBonus(BonusType bonusType, int durationSeconds) {
        if (activeBonuses.containsKey(bonusType)) {
            activeBonuses.get(bonusType).updateTime(durationSeconds);
            return;
        }
        
        BonusTimerItem item = new BonusTimerItem(bonusType, durationSeconds);
        activeBonuses.put(bonusType, item);
        getChildren().add(item);
        
        item.setOpacity(0);
        item.setScaleX(0.5);
        item.setScaleY(0.5);
        
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(300), item);
        fadeIn.setToValue(1.0);
        
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(
            javafx.util.Duration.millis(300), item);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        FXGL.runOnce(() -> {
            fadeIn.play();
            scaleIn.play();
        }, javafx.util.Duration.millis(50));
    }
    
    public void removeBonus(BonusType bonusType) {
        BonusTimerItem item = activeBonuses.remove(bonusType);
        if (item != null) {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(200), item);
            fadeOut.setToValue(0.0);
            
            javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), item);
            scaleOut.setToX(0.5);
            scaleOut.setToY(0.5);
            
            scaleOut.setOnFinished(e -> {
                // КРИТИЧНО: Освобождаем иконку бонуса перед удалением
                if (item.bonusIcon != null) {
                    com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(item.bonusIcon);
                }
                getChildren().remove(item);
            });
            
            fadeOut.play();
            scaleOut.play();
        }
    }
    
    public void updateBonusTime(BonusType bonusType, int remainingSeconds) {
        BonusTimerItem item = activeBonuses.get(bonusType);
        if (item != null) {
            item.updateTime(remainingSeconds);
        }
    }
    
    public void clearAllBonuses() {
        // КРИТИЧНО: Освобождаем иконки бонусов перед очисткой
        for (BonusTimerItem item : activeBonuses.values()) {
            if (item != null && item.bonusIcon != null) {
                com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(item.bonusIcon);
            }
        }
        activeBonuses.clear();
        getChildren().clear();
    }
    
    /**
     * Очистка ресурсов для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Освобождаем иконки бонусов
        clearAllBonuses();
    }

    public void updatePlasmaWeaponKey() {
        BonusTimerItem item = activeBonuses.get(BonusType.PLASMA_WEAPON);
        if (item != null) {
            item.updateHint();
        }
    }
    
    private static class BonusTimerItem extends HBox {
        private BonusType bonusType;
        private ImageView bonusIcon;
        private Label timeLabel;
        private Label hintLabel;
        private AtomicInteger remainingTime;
        
        public BonusTimerItem(BonusType bonusType, int initialTime) {
            this.bonusType = bonusType;
            this.remainingTime = new AtomicInteger(initialTime);
            
            setupUI();
            updateTime(initialTime);
        }
        
        private void setupUI() {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(8);
            setPrefHeight(BONUS_ITEM_HEIGHT);
            setMaxHeight(BONUS_ITEM_HEIGHT);
            
            try {
                Image bonusImage = ImageCache.get(bonusType.getTextureName());
                bonusIcon = new ImageView(bonusImage);
                bonusIcon.setFitHeight(30);
                bonusIcon.setFitWidth(30);
                bonusIcon.setPreserveRatio(true);
            } catch (Exception e) {
                bonusIcon = new ImageView();
                bonusIcon.setFitHeight(30);
                bonusIcon.setFitWidth(30);
                bonusIcon.setStyle("-fx-background-color: rgba(0,255,100,0.3); -fx-background-radius: 15;");
            }
            
            timeLabel = new Label();
            timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            timeLabel.setTextFill(Color.WHITE);
            
            DropShadow neonShadow = new DropShadow();
            neonShadow.setColor(Color.rgb(100, 255, 150, 0.8));
            neonShadow.setRadius(10);
            neonShadow.setSpread(0.3);
            
            Glow neonGlow = new Glow();
            neonGlow.setLevel(0.5);
            
            neonShadow.setInput(neonGlow);
            timeLabel.setEffect(neonShadow);
            
            hintLabel = new Label();
            hintLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            hintLabel.setTextFill(Color.rgb(200, 200, 200));
            
            DropShadow hintShadow = new DropShadow();
            hintShadow.setRadius(5);
            hintShadow.setSpread(0.2);
            hintLabel.setEffect(hintShadow);
            updateHint();
            LOCALIZATION.localeProperty().addListener((obs, old, locale) -> updateHint());
            
            setStyle(
                "-fx-background-color: rgba(0, 50, 25, 0.7);" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(100, 255, 150, 0.6);" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 8;"
            );
            
            VBox textContainer = new VBox();
            textContainer.setSpacing(2);
            textContainer.setAlignment(Pos.CENTER_LEFT);
            textContainer.getChildren().addAll(timeLabel, hintLabel);
            
            getChildren().addAll(bonusIcon, textContainer);
        }
        
        public void updateTime(int newTime) {
            remainingTime.set(newTime);
            
            if (newTime <= 0) {
                timeLabel.setText("0");
                timeLabel.setTextFill(Color.rgb(255, 100, 100)); // Красный для истечения
            } else {
                timeLabel.setText(String.valueOf(newTime));
                
                // Цвет в зависимости от оставшегося времени
                if (newTime <= 5) {
                    timeLabel.setTextFill(Color.rgb(255, 200, 100)); // Оранжевый
                } else if (newTime <= 10) {
                    timeLabel.setTextFill(Color.rgb(255, 255, 100)); // Желтый
                } else {
                    timeLabel.setTextFill(Color.rgb(100, 255, 150)); // Неон-зеленый
                }
            }
        }

        public void updateHint() {
            if (bonusType == BonusType.PLASMA_WEAPON) {
                ArcadeBlocksApp app = (ArcadeBlocksApp) FXGL.getApp();
                String keyName = app.getSaveManager().getControlKey("PLASMA_WEAPON");
                hintLabel.setVisible(true);
                hintLabel.setTextFill(Color.rgb(100, 255, 200));
                hintLabel.setEffect(new DropShadow(5, Color.rgb(100, 255, 200, 0.6)));
                hintLabel.setText(LOCALIZATION.format("bonus.timer.plasma.hint", keyName));
            } else if (bonusType == BonusType.STICKY_PADDLE) {
                hintLabel.setVisible(true);
                hintLabel.setTextFill(Color.rgb(255, 200, 100));
                hintLabel.setEffect(new DropShadow(5, Color.rgb(255, 200, 100, 0.6)));
                hintLabel.setText(LOCALIZATION.get("bonus.timer.sticky.hint"));
            } else {
                hintLabel.setVisible(false);
            }
        }
        
        public BonusType getBonusType() {
            return bonusType;
        }
    }
    
    public void updatePosition(double offsetX, double offsetY) {
        double x = offsetX + MARGIN;
        double y = offsetY + MARGIN + 100; // 100px ниже верхней панели
        setTranslateX(x);
        setTranslateY(y);
    }
}
