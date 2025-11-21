package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.gameplay.BonusType;
import com.arcadeblocks.localization.LocalizationManager;
import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class BonusIndicator extends VBox {
    
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private Map<BonusType, HBox> activeBonusContainers = new HashMap<>();
    private VBox mainContainer;
    private boolean isVisible = false;
    
    public BonusIndicator() {
        setupUI();
        setVisible(false);
        // Маркер для исключения из автоматического центрирования
        setUserData("bonusIndicator");
        setManaged(false);
        setMouseTransparent(true);
        setPickOnBounds(false);
        setFocusTraversable(false);
    }
    
    private void setupUI() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);  // Увеличили с 5 до 8
        setPadding(new Insets(8, 12, 8, 12));  // Увеличили с (5, 10, 5, 10) до (8, 12, 8, 12)
        
        // Устанавливаем минимальную и предпочтительную ширину для предотвращения обрезания текста
        setMinWidth(280);
        setPrefWidth(280);
        setMaxWidth(300);
        
        mainContainer = new VBox(5);  // Увеличили с 3 до 5
        mainContainer.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(mainContainer);
        
        setStyle(
            "-fx-background-color: rgba(26, 26, 46, 0.95);" +  // Увеличили непрозрачность с 0.8 до 0.95
            "-fx-border-color: #7FFF7F;" +
            "-fx-border-width: 2;" +  // Увеличили с 1.5 до 2
            "-fx-border-radius: 10;"  // Увеличили с 8 до 10
        );
    }
    
    public void showBonus(BonusType bonusType, int durationSeconds) {
        if (activeBonusContainers.containsKey(bonusType)) {
            updateBonusTimer(bonusType, durationSeconds);
            return;
        }
        
        HBox bonusContainer = createBonusContainer(bonusType, durationSeconds);
        activeBonusContainers.put(bonusType, bonusContainer);
        mainContainer.getChildren().add(bonusContainer);
        
        // Если это первый бонус, плавно показываем индикатор
        if (!isVisible) {
            setVisible(true);
            isVisible = true;
            fadeIn();
        }
    }
    
    /**
     * Плавное появление индикатора
     */
    private void fadeIn() {
        setOpacity(0.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
    
    private HBox createBonusContainer(BonusType bonusType, int durationSeconds) {
        HBox container = new HBox(10);  // Увеличили с 8 до 10
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMinWidth(260);  // Минимальная ширина контейнера
        container.setPrefWidth(270); // Предпочтительная ширина контейнера
        
        VBox infoContainer = new VBox(3);  // Увеличили с 2 до 3
        infoContainer.setAlignment(Pos.CENTER_LEFT);
        infoContainer.setMinWidth(230);  // Минимальная ширина для текстового контейнера
        infoContainer.setPrefWidth(230); // Предпочтительная ширина для текстового контейнера
        
        ImageView icon = new ImageView();
        icon.setFitHeight(28);  // Увеличили с 20 до 28
        icon.setFitWidth(28);   // Увеличили с 20 до 28
        icon.setPreserveRatio(true);
        
        try {
            Image iconImage = ImageCache.get(bonusType.getTextureName());
            icon.setImage(iconImage);
        } catch (Exception e) {
            // icon will be empty
        }
        
        Label nameLabel = new Label();
        String localizationKey = getBonusLocalizationKey(bonusType);
        localizationManager.bind(nameLabel, localizationKey);
        nameLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 13));  // Увеличили с 10 до 13
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setWrapText(true);  // Перенос текста при необходимости
        nameLabel.setMaxWidth(230);   // Максимальная ширина для текста
        nameLabel.setMinWidth(230);   // Минимальная ширина для текста
        nameLabel.setPrefWidth(230);  // Предпочтительная ширина для текста
        nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);  // Отключаем эллипсис
        
        Label timerLabel = new Label();
        timerLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 15));  // Увеличили с 12 до 15
        timerLabel.setTextFill(getTimerColor(durationSeconds));
        timerLabel.setMaxWidth(230);   // Максимальная ширина для текста
        timerLabel.setMinWidth(230);   // Минимальная ширина для текста
        timerLabel.setPrefWidth(230);  // Предпочтительная ширина для текста
        timerLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);  // Отключаем эллипсис
        
        DropShadow neonShadow = new DropShadow();
        neonShadow.setColor(Color.rgb(127, 255, 127, 0.8));
        neonShadow.setRadius(6);
        neonShadow.setSpread(0.3);
        
        Glow neonGlow = new Glow();
        neonGlow.setLevel(0.4);
        
        neonShadow.setInput(neonGlow);
        timerLabel.setEffect(neonShadow);
        
        updateTimerText(timerLabel, bonusType, durationSeconds);
        
        Label hintLabel = null;
        if (bonusType == BonusType.PLASMA_WEAPON) {
            ArcadeBlocksApp app = (ArcadeBlocksApp) FXGL.getApp();
            final String keyName = app.getSaveManager().getControlKey("PLASMA_WEAPON");
            final Label hintLabelFinal = new Label();
            updateHintLabel(hintLabelFinal, keyName);
            // Слушаем изменения языка и обновляем подсказку
            localizationManager.localeProperty().addListener((obs, oldLocale, newLocale) -> {
                updateHintLabel(hintLabelFinal, keyName);
            });
            hintLabelFinal.setFont(Font.font("Orbitron", FontWeight.NORMAL, 11));  // Увеличили с 9 до 11
            hintLabelFinal.setTextFill(Color.rgb(100, 255, 200));
            hintLabelFinal.setMaxWidth(230);   // Максимальная ширина для текста
            hintLabelFinal.setMinWidth(230);   // Минимальная ширина для текста
            hintLabelFinal.setPrefWidth(230);  // Предпочтительная ширина для текста
            hintLabelFinal.setWrapText(true);  // Перенос текста при необходимости
            hintLabelFinal.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);  // Отключаем эллипсис
            
            DropShadow hintShadow = new DropShadow();
            hintShadow.setColor(Color.rgb(100, 255, 200, 0.6));
            hintShadow.setRadius(4);
            hintShadow.setSpread(0.2);
            hintLabelFinal.setEffect(hintShadow);
            hintLabel = hintLabelFinal;
        }
        
        if (hintLabel != null) {
            infoContainer.getChildren().addAll(nameLabel, timerLabel, hintLabel);
        } else {
            infoContainer.getChildren().addAll(nameLabel, timerLabel);
        }
        container.getChildren().addAll(icon, infoContainer);
        
        container.setUserData(new BonusContainerData(nameLabel, timerLabel, hintLabel));
        
        return container;
    }

    private String getBonusLocalizationKey(BonusType bonusType) {
        switch (bonusType) {
            case BONUS_SCORE:
                return "debug.bonuses.bonus.extra_points";
            case EXTRA_LIFE:
                return "debug.bonuses.bonus.extra_life";
            case INCREASE_PADDLE:
                return "debug.bonuses.bonus.bigger_paddle";
            case DECREASE_PADDLE:
                return "debug.bonuses.bonus.smaller_paddle";
            case STICKY_PADDLE:
                return "debug.bonuses.bonus.sticky_paddle";
            case SLOW_BALLS:
                return "debug.bonuses.bonus.slow_balls";
            case FAST_BALLS:
                return "debug.bonuses.bonus.fast_balls";
            case ENERGY_BALLS:
                return "debug.bonuses.bonus.energy_balls";
            case EXPLOSION_BALLS:
                return "debug.bonuses.bonus.explosive_balls";
            case WEAK_BALLS:
                return "debug.bonuses.bonus.weak_balls";
            case BONUS_BALL:
                return "debug.bonuses.bonus.extra_ball";
            case BONUS_WALL:
                return "debug.bonuses.bonus.shield_barrier";
            case BONUS_MAGNET:
                return "debug.bonuses.bonus.bonus_magnet";
            case PLASMA_WEAPON:
                return "debug.bonuses.bonus.plasma_weapon";
            case LEVEL_PASS:
                return "debug.bonuses.bonus.level_pass";
            case SCORE_RAIN:
                return "debug.bonuses.bonus.score_rain";
            case TRICKSTER:
                return "debug.bonuses.bonus.trickster";
            case RANDOM_BONUS:
                return "debug.bonuses.bonus.random_bonus";
            case CHAOTIC_BALLS:
                return "debug.bonuses.bonus.chaotic_balls";
            case FROZEN_PADDLE:
                return "debug.bonuses.bonus.frozen_paddle";
            case PENALTIES_MAGNET:
                return "debug.bonuses.bonus.penalty_magnet";
            case INVISIBLE_PADDLE:
                return "debug.bonuses.bonus.ghost_paddle";
            case DARKNESS:
                return "debug.bonuses.bonus.darkness";
            case RESET:
                return "debug.bonuses.bonus.reset_bonuses";
            case BAD_LUCK:
                return "debug.bonuses.bonus.bad_luck";
            default:
                return "debug.bonuses.bonus.random_bonus";
        }
    }
    
    private void updateHintLabel(Label hintLabel, String keyName) {
        String hintText = localizationManager.get("bonus.indicator.shot_hint");
        hintLabel.setText(keyName + " " + hintText);
    }
    
    public void updatePlasmaWeaponKey() {
        HBox container = activeBonusContainers.get(BonusType.PLASMA_WEAPON);
        if (container != null) {
            BonusContainerData data = (BonusContainerData) container.getUserData();
            if (data != null && data.hintLabel != null) {
                ArcadeBlocksApp app = (ArcadeBlocksApp) FXGL.getApp();
                String keyName = app.getSaveManager().getControlKey("PLASMA_WEAPON");
                updateHintLabel(data.hintLabel, keyName);
            }
        }
    }
    
    public void updateBonusTimer(BonusType bonusType, int remainingSeconds) {
        HBox container = activeBonusContainers.get(bonusType);
        if (container != null) {
            BonusContainerData data = (BonusContainerData) container.getUserData();
            if (data != null) {
                updateTimerText(data.timerLabel, bonusType, remainingSeconds);
                data.timerLabel.setTextFill(getTimerColor(remainingSeconds));
            }
        }
    }
    
    private void updateTimerText(Label timerLabel, BonusType bonusType, int seconds) {
        if (bonusType == BonusType.PLASMA_WEAPON) {
            timerLabel.setText(localizationManager.format("bonus.indicator.shots.format", seconds));
        } else {
            timerLabel.setText(localizationManager.format("bonus.indicator.seconds.format", seconds));
        }
    }
    
    private Color getTimerColor(int seconds) {
        if (seconds <= 3) {
            return Color.web("#FF4444");
        } else if (seconds <= 10) {
            return Color.web("#FFAA00");
        } else {
            return Color.web("#7FFF7F");
        }
    }
    
    public void hideBonus(BonusType bonusType) {
        // Если это последний бонус, запускаем полную анимацию исчезновения
        if (activeBonusContainers.size() == 1 && activeBonusContainers.containsKey(bonusType)) {
            // Удаляем бонус из карты, но не из UI - это сделает fadeOutAndHide
            activeBonusContainers.remove(bonusType);
            fadeOutAndHide();
        } else {
            // Если это не последний бонус, удаляем его сразу
            HBox container = activeBonusContainers.remove(bonusType);
            if (container != null) {
                mainContainer.getChildren().remove(container);
            }
        }
    }
    
    public void hideAllBonuses() {
        if (activeBonusContainers.isEmpty() && !isVisible) {
            return; // Уже скрыто
        }
        
        // Сначала запускаем анимацию исчезновения
        fadeOutAndHide();
        
        // Очищаем контейнеры ПОСЛЕ завершения анимации (в callback fadeOutAndHide)
    }
    
    /**
     * Плавное исчезновение индикатора
     */
    private void fadeOutAndHide() {
        if (!isVisible) {
            return; // Уже скрыт
        }
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), this);
        fadeOut.setFromValue(getOpacity());
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            // Очищаем контейнеры ПОСЛЕ завершения анимации
            activeBonusContainers.clear();
            mainContainer.getChildren().clear();
            
            setVisible(false);
            isVisible = false;
            setOpacity(1.0); // Восстанавливаем opacity для следующего показа
        });
        fadeOut.play();
    }
    
    public boolean isBonusActive(BonusType bonusType) {
        return activeBonusContainers.containsKey(bonusType);
    }
    
    public void positionUnderScore(double scoreX, double scoreY, double scoreHeight) {
        setTranslateX(scoreX);
        setTranslateY(scoreY + scoreHeight + 10);
    }
    
    private static class BonusContainerData {
        Label nameLabel;
        Label timerLabel;
        Label hintLabel;
        
        BonusContainerData(Label nameLabel, Label timerLabel, Label hintLabel) {
            this.nameLabel = nameLabel;
            this.timerLabel = timerLabel;
            this.hintLabel = hintLabel;
        }
    }
}
