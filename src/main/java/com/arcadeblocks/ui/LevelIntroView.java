package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.AudioConfig;
import com.arcadeblocks.config.LevelConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.BonusLevelConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Экран загрузки уровня с анимацией
 */
public class LevelIntroView extends StackPane implements SupportsCleanup {
    
    private Runnable onCompleteCallback;
    private ArcadeBlocksApp app;
    private Label levelNumberLabel;
    private Color accentColor;
    private LevelConfig.LevelChapter chapterInfo;
    private BonusLevelConfig.BonusChapter bonusChapterInfo;
    private BonusLevelConfig.BonusLevelData bonusLevelData;
    private boolean isBonusLevel;
    private String backgroundImagePath;
    private boolean loadingSoundFinished = false;
    private boolean minDisplayFinished = false;
    private boolean completionTriggered = false;
    private PauseTransition loadingSoundGuard;
    private Rectangle overlayRect;
    private StackPane introContainer;
    private javafx.scene.Node backgroundNode;
    private VBox contentBox;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    
    // Анимации для очистки
    private javafx.animation.ScaleTransition scaleTransition;
    private javafx.animation.TranslateTransition levitationTransition;
    private Timeline glowAnimation;
    private ScaleTransition hintPulseAnimation; // Анимация подсказки для первого уровня
    // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки
    private FadeTransition fadeInTransition;
    private FadeTransition fadeOutTransition;
    private javafx.animation.PauseTransition minimumDisplayPause;

    public LevelIntroView(int levelNumber, String levelName, Runnable onCompleteCallback) {
        this.onCompleteCallback = onCompleteCallback;
        this.app = (ArcadeBlocksApp) FXGL.getApp();
        if (this.app != null) {
            this.app.setLevelIntroActive(true);
        }
        
        initializeUI(levelNumber, levelName);
        startAnimation();
    }
    
    private void initializeUI(int levelNumber, String levelName) {
        setAlignment(Pos.CENTER);

        // Определяем цвет и данные главы в зависимости от уровня
        this.isBonusLevel = BonusLevelConfig.isBonusLevel(levelNumber);
        if (isBonusLevel) {
            this.bonusLevelData = BonusLevelConfig.getLevelData(levelNumber);
            this.bonusChapterInfo = BonusLevelConfig.getChapter(levelNumber);
            if (bonusChapterInfo != null && bonusChapterInfo.getAccentColorHex() != null) {
                this.accentColor = Color.web(bonusChapterInfo.getAccentColorHex());
            } else {
                this.accentColor = Color.web(GameConfig.NEON_CYAN);
            }
            if (bonusLevelData != null) {
                this.backgroundImagePath = bonusLevelData.getBackgroundImage();
            }
        } else {
            this.chapterInfo = LevelConfig.getChapter(levelNumber);
            this.accentColor = resolveAccentColor(this.chapterInfo);
        }

        // Создаем фоновый узел (будет масштабироваться на весь экран)
        backgroundNode = createBackgroundNode(levelNumber, backgroundImagePath, 0.7);

        // Полупрозрачный оверлей для читаемости текста (будет масштабироваться на весь экран)
        overlayRect = new Rectangle();
        overlayRect.setFill(Color.rgb(0, 0, 0, 0.45));
        
        // Стилизованный полупрозрачный контейнер
        Rectangle container = new Rectangle(600, 200);
        
        // КРИТИЧНО: Для темных цветов используем светлый фон, для светлых - темный
        // Это обеспечивает читаемость текста для всех глав
        double brightness = accentColor.getBrightness();
        if (brightness < 0.5) {
            // Темный цвет (SaddleBrown, Indigo, Purple) - используем светлый фон
            container.setFill(Color.rgb(240, 240, 240, 0.85));
        } else {
            // Светлый цвет - используем темный фон как обычно
            container.setFill(Color.rgb(0, 0, 0, 0.6));
        }
        
        container.setStroke(accentColor);
        container.setStrokeWidth(2);
        container.setArcWidth(20);
        container.setArcHeight(20);
        
        // Создаем эффект свечения для контейнера
        // Для темных цветов увеличиваем свечение для лучшей видимости
        double glowRadius = brightness < 0.5 ? 15 : 10;
        double glowSpread = brightness < 0.5 ? 0.4 : 0.25;
        container.setEffect(new javafx.scene.effect.DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            accentColor,
            glowRadius,
            glowSpread,
            0,
            0
        ));
        
        // Создаем контент
        contentBox = createContent(levelNumber, levelName, accentColor);

        introContainer = new StackPane();
        introContainer.setAlignment(Pos.CENTER);
        introContainer.getChildren().addAll(container, contentBox);

        // Добавляем все слои (фон растягивается на весь экран, контент центрирован)
        if (backgroundNode != null) {
            getChildren().add(backgroundNode);
        }
        getChildren().addAll(overlayRect, introContainer);

        ResponsiveLayoutHelper.bindToStage(this, this::adjustLayoutForResolution);
        setUserData("fullScreenOverlay");
    }

    public static javafx.scene.Node createBackgroundNode(int levelNumber, double opacity) {
        return createBackgroundNode(levelNumber, null, opacity);
    }

    public static javafx.scene.Node createBackgroundNode(int levelNumber, String explicitBackground, double opacity) {
        String backgroundImage = explicitBackground;
        if (backgroundImage == null || backgroundImage.isBlank()) {
            if (BonusLevelConfig.isBonusLevel(levelNumber)) {
                BonusLevelConfig.BonusLevelData bonusData = BonusLevelConfig.getLevelData(levelNumber);
                if (bonusData != null) {
                    backgroundImage = bonusData.getBackgroundImage();
                }
            } else {
                LevelConfig.LevelData levelData = LevelConfig.getLevel(levelNumber);
                if (levelData != null) {
                    backgroundImage = levelData.getBackgroundImage();
                }
            }
        }

        if (backgroundImage == null || backgroundImage.isBlank()) {
            return null;
        }
        
        try {
            // Используем текущее разрешение для фона (как в главном меню)
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            javafx.scene.image.Image bgImage = ImageCache.get(backgroundImage);
            if (bgImage == null) {
                return null;
            }
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(bgImage);
            imageView.setFitWidth(currentRes.getWidth());
            imageView.setFitHeight(currentRes.getHeight());
            imageView.setPreserveRatio(false);
            imageView.setSmooth(false);
            imageView.setCache(false);
            imageView.setOpacity(Math.max(0.0, Math.min(1.0, opacity)));
            return imageView;
        } catch (Exception e) {
            System.err.println("⚠️ Не удалось загрузить фон загрузочного экрана для уровня " + levelNumber + ": " + e.getMessage());
            return null;
        }
    }
    
    private VBox createContent(int levelNumber, String levelName, Color accentColor) {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPrefSize(600, 200);
        
        // Надпись "Уровень X"
        levelNumberLabel = new Label(localizationManager.format("level.intro.level_number", levelNumber));
        levelNumberLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 48));
        levelNumberLabel.setTextFill(accentColor);
        levelNumberLabel.setTextAlignment(TextAlignment.CENTER);
        levelNumberLabel.setAlignment(Pos.CENTER);
        
        // Эффект свечения для номера уровня (уменьшен наполовину)
        levelNumberLabel.setEffect(new javafx.scene.effect.DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            accentColor,
            7.5, // Уменьшили с 15 до 7.5
            0.4,  // Уменьшили с 0.8 до 0.4
            0,
            0
        ));
        
        // Информация о главе
        Label chapterLabel;
        if (isBonusLevel) {
            String roman = bonusChapterInfo != null ? bonusChapterInfo.getRomanNumeral() : "I";
            String title = bonusChapterInfo != null ? bonusChapterInfo.getTitle() : localizationManager.get("bonus.chapter.title.1");
            String chapterText = localizationManager.format("level.intro.bonus.chapter.named", roman, title);
            chapterLabel = new Label(chapterText);
        } else {
            if (chapterInfo != null) {
                String chapterText = localizationManager.format(
                    "level.intro.chapter.named",
                    chapterInfo.getRomanNumeral(),
                    chapterInfo.getTitle()
                );
                chapterLabel = new Label(chapterText);
            } else {
                chapterLabel = new Label(localizationManager.format("level.intro.chapter.number", (levelNumber + 9) / 10));
            }
        }
        chapterLabel.setFont(Font.font("Orbitron", FontWeight.SEMI_BOLD, 20));
        chapterLabel.setTextFill(accentColor);
        chapterLabel.setTextAlignment(TextAlignment.CENTER);
        chapterLabel.setAlignment(Pos.CENTER);
        chapterLabel.setOpacity(0.85);

        // Название уровня - получаем локализованное название напрямую из LevelConfig
        String displayName = getLocalizedLevelName(levelNumber);
        // Добавляем приписку (DEBUG) / (ОТЛАДКА) если уровень запущен в debug режиме
        if (app != null && app.isDebugMode()) {
            String debugSuffix = localizationManager.get("debug.levels.suffix");
            displayName = displayName + " " + debugSuffix;
        }
        Label levelNameLabel = new Label(displayName);
        levelNameLabel.setFont(Font.font("Orbitron", FontWeight.NORMAL, 24));
        levelNameLabel.setTextFill(accentColor);
        levelNameLabel.setTextAlignment(TextAlignment.CENTER);
        levelNameLabel.setAlignment(Pos.CENTER);
        levelNameLabel.setOpacity(0.9);
        
        // Специальная подсказка для первого уровня
        if (levelNumber == 1) {
            Label hintLabel = new Label(localizationManager.get("level.intro.hint"));
            hintLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
            hintLabel.setTextFill(Color.web("#FFD700")); // Золотой цвет
            hintLabel.setTextAlignment(TextAlignment.CENTER);
            hintLabel.setAlignment(Pos.CENTER);
            hintLabel.setOpacity(0.95);
            
            // Эффект свечения для подсказки
            DropShadow hintGlow = new DropShadow(
                javafx.scene.effect.BlurType.GAUSSIAN,
                Color.web("#FFD700"),
                8,
                0.6,
                0,
                0
            );
            hintLabel.setEffect(hintGlow);
            
            // Анимация пульсации для подсказки
            hintPulseAnimation = new ScaleTransition(Duration.millis(1500), hintLabel);
            hintPulseAnimation.setFromX(1.0);
            hintPulseAnimation.setToX(1.05);
            hintPulseAnimation.setFromY(1.0);
            hintPulseAnimation.setToY(1.05);
            hintPulseAnimation.setAutoReverse(true);
            hintPulseAnimation.setCycleCount(Animation.INDEFINITE);
            hintPulseAnimation.play();
            
            contentBox.getChildren().addAll(levelNumberLabel, chapterLabel, levelNameLabel, hintLabel);
        } else {
            contentBox.getChildren().addAll(levelNumberLabel, chapterLabel, levelNameLabel); 
        }
        
        return contentBox;
    }
    
    private void startAnimation() {
        // Останавливаем музыку главного меню
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().stopMusic();
            // КРИТИЧНО: Сбрасываем runtime volume multiplier обратно к 1.0
            // Это необходимо, так как ChapterStoryView мог оставить его на низком значении
            app.getAudioManager().resetRuntimeMusicVolumeMultiplier();
        // System.out.println("🔇 Музыка главного меню остановлена при загрузке уровня");
        }
        
        // Воспроизводим звук загрузки уровня
        if (app != null && app.getAudioManager() != null) {
            try {
                String loadingSound = app.getCurrentLevelLoadingSound();
                if (loadingSound == null || loadingSound.isBlank()) {
                    loadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
                }
                String loadingSoundFinal = loadingSound;
                final double expectedDuration = estimateSoundDurationSeconds(loadingSoundFinal);
                // КРИТИЧНО: Сохраняем ссылку на app для проверки в callback
                final ArcadeBlocksApp appRef = app;
                app.getAudioManager().playSFX(loadingSoundFinal, () -> javafx.application.Platform.runLater(() -> {
                    // КРИТИЧНО: Проверяем, что LevelIntroView еще не очищен
                    // Используем appRef вместо app, так как app может быть обнулен в cleanup()
                    if (appRef == null) {
                        return;
                    }
                    loadingSoundFinished = true;
                    if (loadingSoundGuard != null) {
                        loadingSoundGuard.stop();
                        loadingSoundGuard = null;
                    }
                    attemptCompletion();
                }));
                loadingSoundGuard = new PauseTransition(Duration.seconds(expectedDuration + 0.5));
                // КРИТИЧНО: Сохраняем ссылку на app для проверки в callback
                final ArcadeBlocksApp appRefForGuard = app;
                loadingSoundGuard.setOnFinished(evt -> {
                    // КРИТИЧНО: Проверяем, что LevelIntroView еще не очищен
                    // Используем appRefForGuard вместо app, так как app может быть обнулен в cleanup()
                    if (appRefForGuard == null) {
                        return;
                    }
                    loadingSoundGuard = null;
                    if (!loadingSoundFinished) {
                        loadingSoundFinished = true;
                        attemptCompletion();
                    }
                });
                loadingSoundGuard.play();
        // System.out.println("🔊 Воспроизводится звук загрузки уровня");
            } catch (Exception e) {
                System.err.println("Ошибка воспроизведения звука загрузки уровня: " + e.getMessage());
                loadingSoundFinished = true;
                attemptCompletion();
            }
        } else {
            loadingSoundFinished = true;
        }

        // Начальная прозрачность
        setOpacity(0);

        // КРИТИЧНО: Останавливаем старую анимацию появления, если она еще активна
        if (fadeInTransition != null) {
            try {
                fadeInTransition.stop();
            } catch (Exception ignored) {}
            fadeInTransition = null;
        }
        
        // Анимация появления
        // КРИТИЧНО: Сохраняем ссылку на анимацию для её остановки в cleanup()
        fadeInTransition = new FadeTransition(Duration.millis(500), this);
        fadeInTransition.setFromValue(0.0);
        fadeInTransition.setToValue(1.0);
        
        // Анимация левитации для номера уровня
        if (levelNumberLabel == null) {
            return;
        }
        
        // Создаем анимацию пульсации и левитации
        scaleTransition = new ScaleTransition(Duration.millis(1000), levelNumberLabel);
        scaleTransition.setFromX(1.0);
        scaleTransition.setToX(1.05);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToY(1.05);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(Animation.INDEFINITE);
        
        // Анимация движения вверх-вниз (левитация)
        levitationTransition = new TranslateTransition(Duration.millis(2000), levelNumberLabel);
        levitationTransition.setFromY(0);
        levitationTransition.setToY(-5);
        levitationTransition.setAutoReverse(true);
        levitationTransition.setCycleCount(Animation.INDEFINITE);
        
        // Анимация свечения (уменьшен наполовину)
        DropShadow glowEffect = new DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            accentColor,
            7.5, // Уменьшили с 15 до 7.5
            0.4,  // Уменьшили с 0.8 до 0.4
            0,
            0
        );
        
        
        glowAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glowEffect.radiusProperty(), 7.5)), // Уменьшили с 15 до 7.5
            new KeyFrame(Duration.millis(1000), new KeyValue(glowEffect.radiusProperty(), 12.5)), // Уменьшили с 25 до 12.5
            new KeyFrame(Duration.millis(2000), new KeyValue(glowEffect.radiusProperty(), 7.5)) // Уменьшили с 15 до 7.5
        );
        glowAnimation.setCycleCount(Animation.INDEFINITE);
        glowAnimation.setAutoReverse(true);
        
        // Применяем эффект свечения
        levelNumberLabel.setEffect(glowEffect);
        
        // Запускаем анимации
        fadeInTransition.play();
        scaleTransition.play();
        levitationTransition.play();
        glowAnimation.play();
        
        // Минимальное время отображения (2 секунды)
        // КРИТИЧНО: Останавливаем старую анимацию, если она еще активна
        if (minimumDisplayPause != null) {
            try {
                minimumDisplayPause.stop();
            } catch (Exception ignored) {}
            minimumDisplayPause = null;
        }
        
        minimumDisplayPause = new javafx.animation.PauseTransition(Duration.seconds(2));
        // КРИТИЧНО: Сохраняем ссылку на app для проверки в callback
        final ArcadeBlocksApp appRefForPause = app;
        minimumDisplayPause.setOnFinished(event -> {
            // КРИТИЧНО: Проверяем, что LevelIntroView еще не очищен
            // Используем appRefForPause вместо app, так как app может быть обнулен в cleanup()
            if (appRefForPause == null) {
                return;
            }
            minDisplayFinished = true;
            attemptCompletion();
        });

        // Запускаем паузу после завершения появления
        // КРИТИЧНО: Сохраняем ссылку на app для проверки в callback
        final ArcadeBlocksApp appRefForFadeIn = app;
        fadeInTransition.setOnFinished(event -> {
            // КРИТИЧНО: Проверяем, что LevelIntroView еще не очищен
            // Используем appRefForFadeIn вместо app, так как app может быть обнулен в cleanup()
            if (appRefForFadeIn == null || minimumDisplayPause == null) {
                return;
            }
            minimumDisplayPause.play();
        });
    }

    private Color resolveAccentColor(LevelConfig.LevelChapter chapter) {
        if (chapter != null) {
            return Color.web(chapter.getAccentColorHex());
        }
        return Color.web(GameConfig.NEON_CYAN);
    }

    private void attemptCompletion() {
        // КРИТИЧНО: Проверяем, что LevelIntroView еще не очищен
        // Используем проверку completionTriggered вместо app == null, так как app может быть обнулен в cleanup()
        if (completionTriggered) {
            return;
        }
        if (!loadingSoundFinished || !minDisplayFinished) {
            return;
        }
        completionTriggered = true;
        // КРИТИЧНО: Сохраняем ссылку на this для использования в Platform.runLater
        final LevelIntroView self = this;
        javafx.application.Platform.runLater(() -> {
            // КРИТИЧНО: Проверяем, что LevelIntroView еще не очищен
            // Используем проверку completionTriggered вместо app == null
            if (self.completionTriggered && self.app == null) {
                return;
            }
            self.playFadeOut();
        });
    }
    
    /**
     * Очистка всех анимаций для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимации появления/исчезновения
        if (fadeInTransition != null) {
            try {
                // КРИТИЧНО: Очищаем callback перед остановкой для предотвращения утечек памяти
                fadeInTransition.setOnFinished(null);
                fadeInTransition.stop();
            } catch (Exception ignored) {}
            fadeInTransition = null;
        }
        if (fadeOutTransition != null) {
            try {
                // КРИТИЧНО: Очищаем callback перед остановкой для предотвращения утечек памяти
                fadeOutTransition.setOnFinished(null);
                fadeOutTransition.stop();
            } catch (Exception ignored) {}
            fadeOutTransition = null;
        }
        if (minimumDisplayPause != null) {
            try {
                // КРИТИЧНО: Очищаем callback перед остановкой для предотвращения утечек памяти
                minimumDisplayPause.setOnFinished(null);
                minimumDisplayPause.stop();
            } catch (Exception ignored) {}
            minimumDisplayPause = null;
        }
        
        // Останавливаем все анимации
        if (loadingSoundGuard != null) {
            try {
                // КРИТИЧНО: Очищаем callback перед остановкой для предотвращения утечек памяти
                loadingSoundGuard.setOnFinished(null);
                loadingSoundGuard.stop();
            } catch (Exception ignored) {}
            loadingSoundGuard = null;
        }
        if (scaleTransition != null) {
            scaleTransition.stop();
            scaleTransition = null;
        }
        if (levitationTransition != null) {
            levitationTransition.stop();
            levitationTransition = null;
        }
        if (glowAnimation != null) {
            glowAnimation.stop();
            glowAnimation = null;
        }
        if (hintPulseAnimation != null) {
            hintPulseAnimation.stop();
            hintPulseAnimation = null;
        }
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Дополнительная прямая очистка ResponsiveLayoutHelper listeners
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
        
        // КРИТИЧНО: Очищаем все эффекты (DropShadow) перед удалением children
        // Рекурсивно очищаем все эффекты во всех дочерних элементах
        clearAllEffects(this);
        
        // КРИТИЧНО: Отвязываем textProperty() у всех Label компонентов перед удалением children
        // Это предотвращает утечки памяти от StringBinding, если они были привязаны
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Освобождаем фоновое изображение через ImageCache перед удалением children
        if (backgroundNode instanceof javafx.scene.image.ImageView imageView) {
            javafx.scene.image.Image bgImage = imageView.getImage();
            if (bgImage != null) {
                // КРИТИЧНО: Освобождаем изображение из кэша для предотвращения утечек VRAM
                ImageCache.forget(bgImage);
                imageView.setImage(null);
            }
        }
        
        // КРИТИЧНО: Освобождаем изображения (включая фоновое изображение intro) перед удалением children
        // Это предотвращает утечки памяти от com.sun.prism.image.* буферов
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
        
        // КРИТИЧНО: Обнуляем все ссылки для предотвращения утечек памяти
        onCompleteCallback = null;
        app = null;
        levelNumberLabel = null;
        overlayRect = null;
        introContainer = null;
        backgroundNode = null;
        contentBox = null;
        chapterInfo = null;
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
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                unbindAllTextProperties(child);
            }
        }
    }
    
    /**
     * Рекурсивно очищает все эффекты (DropShadow и др.) в дереве компонентов
     */
    private void clearAllEffects(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        
        // Очищаем эффекты у всех узлов
        if (node instanceof javafx.scene.control.Labeled) {
            ((javafx.scene.control.Labeled) node).setEffect(null);
        } else if (node instanceof javafx.scene.shape.Shape) {
            ((javafx.scene.shape.Shape) node).setEffect(null);
        } else if (node instanceof javafx.scene.layout.Region) {
            ((javafx.scene.layout.Region) node).setEffect(null);
        }
        
        // Рекурсивно обрабатываем дочерние элементы
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                clearAllEffects(child);
            }
        }
    }

    private void playFadeOut() {
        // КРИТИЧНО: Сохраняем ссылки перед cleanup(), чтобы использовать их после очистки
        ArcadeBlocksApp appRef = app;
        Runnable callbackRef = onCompleteCallback;
        
        // КРИТИЧНО: Останавливаем старую анимацию исчезновения, если она еще активна
        if (fadeOutTransition != null) {
            try {
                fadeOutTransition.setOnFinished(null);
                fadeOutTransition.stop();
            } catch (Exception ignored) {}
            fadeOutTransition = null;
        }
        
        // КРИТИЧНО: НЕ вызываем cleanup() здесь, так как он обнулит app и callback не сможет выполниться
        // cleanup() будет вызван в fadeOutTransition.setOnFinished после удаления из сцены
        
        fadeOutTransition = new FadeTransition(Duration.millis(500), this);
        fadeOutTransition.setFromValue(getOpacity());
        fadeOutTransition.setToValue(0.0);
        // КРИТИЧНО: Сохраняем ссылку на this для использования в callback
        final LevelIntroView self = this;
        fadeOutTransition.setOnFinished(e -> {
            try {
                // КРИТИЧНО: Останавливаем и очищаем анимацию перед удалением
                if (fadeOutTransition != null) {
                    fadeOutTransition.setOnFinished(null);
                    fadeOutTransition.stop();
                    fadeOutTransition = null;
                }
                
                // КРИТИЧНО: Используем правильный метод удаления UI ноды из FXGL сцены
                // Это гарантирует полное удаление из всех структур FXGL
                FXGL.getGameScene().removeUINode(self);
                
                // Используем сохраненную ссылку, так как app уже обнулен в cleanup()
                if (appRef != null) {
                    appRef.setLevelIntroActive(false);
                }
                
                // КРИТИЧНО: Вызываем callback ПОСЛЕ удаления из сцены, чтобы гарантировать,
                // что LevelIntroView полностью очищен перед созданием новых UI элементов
                // Используем сохраненную ссылку, так как onCompleteCallback уже обнулен в cleanup()
                if (callbackRef != null) {
                    callbackRef.run();
                }
                
                // КРИТИЧНО: Вызываем cleanup() ПОСЛЕ удаления из сцены и вызова callback
                // Это гарантирует, что все ресурсы освобождаются после завершения работы
                self.cleanup();
            } catch (Exception ignored) {}
        });
        fadeOutTransition.play();
    }

    private void adjustLayoutForResolution(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        // Масштабируем оверлей на весь экран (как в главном меню)
        if (overlayRect != null) {
            overlayRect.setWidth(width);
            overlayRect.setHeight(height);
        }

        // Масштабируем фон на весь экран (как в главном меню)
        if (backgroundNode instanceof javafx.scene.image.ImageView imageView) {
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
        }

        // Контейнер с контентом также растягиваем на весь экран для правильного центрирования
        if (introContainer != null) {
            introContainer.setPrefSize(width, height);
            introContainer.setMinSize(width, height);
            introContainer.setMaxSize(width, height);
        }
    }

    private double estimateSoundDurationSeconds(String soundPath) {
        if (soundPath == null || soundPath.isBlank()) {
            return 2.0;
        }

        try {
            String normalized = soundPath.startsWith("/") ? soundPath.substring(1) : soundPath;
            var url = LevelIntroView.class.getResource("/assets/" + normalized);
            if (url == null) {
                return fallbackDuration(soundPath);
            }
            try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(url)) {
                AudioFormat format = inputStream.getFormat();
                long frames = inputStream.getFrameLength();
                if (frames > 0 && format.getFrameRate() > 0) {
                    return frames / format.getFrameRate();
                }
            }
        } catch (Exception ignored) {
        }
        return fallbackDuration(soundPath);
    }

    private double fallbackDuration(String soundPath) {
        if (soundPath.contains("boss_loading10") || soundPath.contains("loading_sound")) {
            return 10.0;
        }
        if (soundPath.contains("boss_loading") || soundPath.contains("boss_completed")) {
            return 5.0;
        }
        return 2.5;
    }
    
    /**
     * Получить локализованное название уровня (только название, без префикса "Уровень X:" или "Level X:")
     */
    private String getLocalizedLevelName(int levelNumber) {
        String fullName;
        LevelConfig.LevelData levelData = null;
        if (BonusLevelConfig.isBonusLevel(levelNumber)) {
            BonusLevelConfig.BonusLevelData bonusData = BonusLevelConfig.getLevelData(levelNumber);
            if (bonusData == null) {
                return "";
            }
            fullName = LocalizationManager.getInstance().getOrDefault(bonusData.getNameKey(), bonusData.getName());
        } else {
            levelData = LevelConfig.getLevel(levelNumber);
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

        boolean isLBreakout = levelData != null && levelData.getLevelFormat() == LevelConfig.LevelFormat.LBREAKOUT;
        boolean missingTitle = displayName.isBlank() || (fullName != null && fullName.trim().endsWith(":"));
        if (isLBreakout) {
            String fallbackTitle = LevelConfig.getLBreakoutHdTitle(levelNumber);
            if (fallbackTitle != null && !fallbackTitle.isBlank()) {
                // Для LBreakoutHD всегда используем эталонное название из списка, чтобы исключить пустые/урезанные строки
                displayName = fallbackTitle;
            }
        }

        return displayName.isBlank() ? fullName : displayName;
    }
}
