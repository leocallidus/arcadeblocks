package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Короткий экран с логотипом Leocallidus Games (LCGames) перед загрузкой игры.
 */
public class LCGamesJingleView extends StackPane {

    private static final Duration FADE_IN_DURATION = Duration.millis(350);
    private static final Duration DISPLAY_DURATION = Duration.millis(300);
    private static final Duration FADE_OUT_DURATION = Duration.millis(350);
    private static final Duration LOGO_FADE_DURATION = Duration.millis(350);
    private static final double FRAME_PADDING = 30.0;
    private static final String LOGO_TEXTURE = "Leocallidus_games_logo.png";
    private static final String JINGLE_SOUND = "sounds/lcgames_jingle.wav";
    private final ImageView logoView = new ImageView();
    private final Rectangle frame = new Rectangle();
    private StackPane logoContainer;
    private SequentialTransition animationSequence;
    private javafx.beans.value.ChangeListener<? super javafx.geometry.Bounds> boundsListener;
    // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки
    private FadeTransition fadeInView;
    private FadeTransition fadeInLogo;
    private PauseTransition pause;
    private FadeTransition fadeOutView;
    private FadeTransition fadeOutLogo;

    public LCGamesJingleView(Runnable onFinished) {
        setAlignment(Pos.CENTER);

        Rectangle background = new Rectangle(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT, Color.web("#06060f"));
        background.setOpacity(0.92);

        logoContainer = createLogoContainer();

        getChildren().addAll(background, logoContainer);

        ResponsiveLayoutHelper.bindToStage(this, (width, height) -> {
            background.setWidth(width);
            background.setHeight(height);
            adjustLogoSizing(width, height);
            double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
            double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());
            StackPane.setMargin(logoContainer, new Insets(offsetY, offsetX, offsetY, offsetX));
        });
        setUserData("fullScreenOverlay");

        playJingleSound();
        playAnimation(logoContainer, onFinished);
    }

    private StackPane createLogoContainer() {
        logoView.setPreserveRatio(true);
        logoView.setFitWidth(420);
        try {
            Image logo = ImageCache.get(LOGO_TEXTURE);
            logoView.setImage(logo);
        } catch (Exception e) {
            System.err.println("Не удалось загрузить логотип Leocallidus Games (LCGames): " + e.getMessage());
        }
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(Color.web("#00f7ff"));
        frame.setStrokeWidth(3.5);
        frame.setArcWidth(28);
        frame.setArcHeight(28);
        
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00f7ff"));
        glow.setRadius(45);
        glow.setSpread(0.75);
        frame.setEffect(glow);

        StackPane container = new StackPane(frame, logoView);
        container.setAlignment(Pos.CENTER);
        container.setOpacity(0.0);

        boundsListener = (obs, oldBounds, newBounds) -> {
            frame.setWidth(newBounds.getWidth() + FRAME_PADDING);
            frame.setHeight(newBounds.getHeight() + FRAME_PADDING);
        };
        logoView.boundsInParentProperty().addListener(boundsListener);

        return container;
    }

    private void adjustLogoSizing(double width, double height) {
        double targetWidth = Math.max(360, width * 0.32);
        double targetHeight = Math.max(240, height * 0.32);

        if (logoView.getImage() != null) {
            double scaleX = targetWidth / logoView.getImage().getWidth();
            double scaleY = targetHeight / logoView.getImage().getHeight();
            double scale = Math.min(Math.max(scaleX, scaleY), 1.0);
            logoView.setFitWidth(logoView.getImage().getWidth() * scale);
        } else {
            logoView.setFitWidth(targetWidth);
        }
    }

    private void playJingleSound() {
        try {
            ArcadeBlocksApp app = (ArcadeBlocksApp) FXGL.getApp();
            if (app != null && app.getAudioManager() != null) {
                // System.out.println("🔊 Попытка воспроизведения джингла: " + JINGLE_SOUND);
                app.getAudioManager().playSFX(JINGLE_SOUND);
                // System.out.println("✅ Команда на воспроизведение джингла отправлена");
            } else {
                System.err.println("❌ AudioManager недоступен: app=" + (app != null) + ", audioManager=" + (app != null ? app.getAudioManager() : "N/A"));
            }
        } catch (Exception e) {
            System.err.println("❌ Не удалось воспроизвести джингл Leocallidus Games (LCGames): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playAnimation(Node logoNode, Runnable onFinished) {
        setOpacity(0.0);

        // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки в cleanup()
        fadeInView = new FadeTransition(FADE_IN_DURATION, this);
        fadeInView.setFromValue(0.0);
        fadeInView.setToValue(1.0);

        fadeInLogo = new FadeTransition(LOGO_FADE_DURATION, logoNode);
        fadeInLogo.setFromValue(0.0);
        fadeInLogo.setToValue(1.0);

        pause = new PauseTransition(DISPLAY_DURATION);

        fadeOutView = new FadeTransition(FADE_OUT_DURATION, this);
        fadeOutView.setFromValue(1.0);
        fadeOutView.setToValue(0.0);

        fadeOutLogo = new FadeTransition(FADE_OUT_DURATION, logoNode);
        fadeOutLogo.setFromValue(1.0);
        fadeOutLogo.setToValue(0.0);

        ParallelTransition fadeInSequence = new ParallelTransition(fadeInView, fadeInLogo);
        ParallelTransition fadeOutSequence = new ParallelTransition(fadeOutView, fadeOutLogo);

        animationSequence = new SequentialTransition(fadeInSequence, pause, fadeOutSequence);
        animationSequence.setOnFinished(event -> {
            try {
                // КРИТИЧНО: Вызываем cleanup() перед удалением из сцены
                cleanup();
                
                // КРИТИЧНО: Используем правильный метод удаления UI ноды из FXGL сцены
                FXGL.getGameScene().removeUINode(this);
                
                // КРИТИЧНО: Вызываем callback ПОСЛЕ удаления из сцены
                if (onFinished != null) {
                    onFinished.run();
                }
            } catch (Exception e) {
                System.err.println("Ошибка при завершении LCGamesJingleView: " + e.getMessage());
                // В случае ошибки все равно вызываем callback
                if (onFinished != null) {
                    try {
                        onFinished.run();
                    } catch (Exception ignored) {}
                }
            }
        });
        animationSequence.play();
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем animationSequence ПЕРВЫМ
        // Это автоматически остановит все встроенные в него анимации (fadeInView, fadeInLogo, pause, fadeOutView, fadeOutLogo)
        // Попытка остановить отдельные анимации, которые встроены в SequentialTransition, вызовет ошибку:
        // "Cannot stop when embedded in another animation"
        if (animationSequence != null) {
            try {
                animationSequence.stop();
            } catch (Exception e) {
                // Игнорируем ошибки при остановке (например, если анимация уже завершена)
            }
            animationSequence = null;
        }
        
        // КРИТИЧНО: НЕ останавливаем отдельные анимации, если они встроены в animationSequence
        // animationSequence.stop() уже остановил все встроенные анимации
        // Просто обнуляем ссылки для сборки мусора
        fadeInView = null;
        fadeInLogo = null;
        pause = null;
        fadeOutView = null;
        fadeOutLogo = null;
        
        // КРИТИЧНО: Удаляем listener на boundsInParentProperty
        if (logoView != null && boundsListener != null) {
            logoView.boundsInParentProperty().removeListener(boundsListener);
            boundsListener = null;
        }
        
        // КРИТИЧНО: Очищаем эффекты перед удалением нод (эффекты могут держать ссылки)
        if (frame != null) {
            frame.setEffect(null);
            frame.setFill(null);
            frame.setStroke(null);
        }
        
        // КРИТИЧНО: Очищаем ImageView полностью
        if (logoView != null) {
            logoView.setImage(null);
            logoView.setEffect(null);
        }
        
        // КРИТИЧНО: Очищаем контейнер логотипа
        if (logoContainer != null) {
            logoContainer.setEffect(null);
            logoContainer.getChildren().clear();
        }
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Дополнительная прямая синхронная очистка ResponsiveLayoutHelper listeners
        try {
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage != null) {
                @SuppressWarnings("unchecked")
                javafx.beans.value.ChangeListener<Number> widthListener = 
                    (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveWidthListener");
                @SuppressWarnings("unchecked")
                javafx.beans.value.ChangeListener<Number> heightListener = 
                    (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveHeightListener");
                
                if (widthListener != null) {
                    stage.widthProperty().removeListener(widthListener);
                    this.getProperties().remove("responsiveWidthListener");
                }
                if (heightListener != null) {
                    stage.heightProperty().removeListener(heightListener);
                    this.getProperties().remove("responsiveHeightListener");
                }
            }
            
            @SuppressWarnings("unchecked")
            javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener = 
                (javafx.beans.value.ChangeListener<javafx.scene.Scene>) this.getProperties().get("responsiveSceneListener");
            if (sceneListener != null) {
                this.sceneProperty().removeListener(sceneListener);
                this.getProperties().remove("responsiveSceneListener");
            }
            
            this.getProperties().remove("responsiveLastWidth");
            this.getProperties().remove("responsiveLastHeight");
        } catch (Exception ignored) {}
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
        
        // КРИТИЧНО: Очищаем собственный эффект
        setEffect(null);
        
        // КРИТИЧНО: Обнуляем ссылки для предотвращения утечек памяти
        logoContainer = null;
    }
}
