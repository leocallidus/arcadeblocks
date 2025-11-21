package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.audio.SDL2AudioManager;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.story.ChapterStoryData;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.ui.util.UINodeCleanup;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
 * Fullscreen narrative overlay displayed before the first level of a chapter.
 */
public class ChapterStoryView extends StackPane implements SupportsCleanup {

    private static final double BACKGROUND_OPACITY = 0.35;
    private static final Duration INTRO_DURATION = Duration.millis(600);
    private static final Duration PARAGRAPH_FADE_DURATION = Duration.millis(1500);
    private static final Duration MUSIC_FADE_OUT = Duration.millis(320);
    private static final Duration MUSIC_FADE_IN = Duration.millis(650);

    private final ChapterStoryData storyData;
    private final Runnable onDismissed;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();

    private SDL2AudioManager audioManager;
    private double initialRuntimeMusicMultiplier = 1.0;
    private boolean storyMusicStarted = false;
    private boolean audioRestored = false;

    private Node backgroundNode;
    private Rectangle overlayRect;
    private HBox contentContainer;
    private Rectangle imageBackdrop;
    private ImageView storyImageView;
    private Text hintText;
    private final List<Text> paragraphTexts = new ArrayList<>();

    private FadeTransition fadeInTransition;
    private TranslateTransition introSlideTransition;
    private SequentialTransition paragraphsSequence;
    private FadeTransition fadeOutTransition;
    private ScaleTransition hintPulseAnimation;
    private Timeline musicFadeTimeline;
    private DoubleProperty musicFadeProgress;
    private javafx.beans.value.ChangeListener<Number> musicFadeListener;

    private boolean paragraphsFullyVisible = false;
    private boolean closing = false;
    private boolean cleanedUp = false;

    public ChapterStoryView(ArcadeBlocksApp app, ChapterStoryData storyData, Runnable onDismissed) {
        this.storyData = storyData;
        this.onDismissed = onDismissed;
        this.audioManager = app != null ? app.getAudioManager() : null;
        if (audioManager != null) {
            initialRuntimeMusicMultiplier = audioManager.getRuntimeMusicVolumeMultiplier();
        }

        setAlignment(Pos.CENTER);
        setPickOnBounds(true);
        setFocusTraversable(true);

        buildUI();
        setupInputHandlers();
        playIntro();
        startStoryMusic();

        ResponsiveLayoutHelper.bindToStage(this, this::adjustLayoutForResolution);
        setUserData("fullScreenOverlay");

        Platform.runLater(this::requestFocus);
    }

    private void buildUI() {
        if (storyData != null) {
            backgroundNode = LevelIntroView.createBackgroundNode(storyData.firstLevelNumber(), BACKGROUND_OPACITY);
        }

        overlayRect = new Rectangle();
        overlayRect.setFill(Color.rgb(0, 0, 0, 0.45));

        contentContainer = new HBox(56);
        contentContainer.setAlignment(Pos.CENTER);
        contentContainer.setPadding(new Insets(42, 64, 48, 64));
        contentContainer.setStyle(
            "-fx-background-color: rgba(10, 10, 25, 0.55);" +
            "-fx-background-radius: 30;" +
            "-fx-border-color: rgba(93, 242, 255, 0.35);" +
            "-fx-border-radius: 28;" +
            "-fx-border-width: 1.5;"
        );

        StackPane imageWrapper = new StackPane();
        imageWrapper.setAlignment(Pos.CENTER);
        imageWrapper.setPadding(new Insets(20));

        imageBackdrop = new Rectangle(420, 520);
        imageBackdrop.setArcWidth(30);
        imageBackdrop.setArcHeight(30);
        imageBackdrop.setFill(Color.rgb(18, 26, 42, 0.62));
        imageBackdrop.setStroke(Color.rgb(93, 242, 255, 0.45));
        imageBackdrop.setStrokeWidth(1.4);

        storyImageView = new ImageView();
        storyImageView.setPreserveRatio(true);
        storyImageView.setSmooth(true);
        storyImageView.setCache(true);
        storyImageView.setFitWidth(360);

        if (storyData != null) {
            String imagePath = storyData.imagePath();
            if (imagePath != null && !imagePath.isBlank()) {
                try {
                    Image image = ImageCache.get(imagePath);
                    if (image != null) {
                        storyImageView.setImage(image);
                    } else {
                        storyImageView.setOpacity(0.0);
                    }
                } catch (Exception ignored) {
                    storyImageView.setOpacity(0.0);
                }
            } else {
                storyImageView.setOpacity(0.0);
            }
        }

        imageWrapper.getChildren().addAll(imageBackdrop, storyImageView);

        VBox textColumn = new VBox(18);
        textColumn.setAlignment(Pos.TOP_LEFT);
        textColumn.setFillWidth(true);

        String prefix = storyData != null ? storyData.localizationPrefix() : "";

        // Получаем цвет главы из LevelConfig
        Color chapterColor = Color.web("#5DF2FF"); // Цвет по умолчанию
        if (storyData != null) {
            com.arcadeblocks.config.LevelConfig.LevelChapter chapter = 
                com.arcadeblocks.config.LevelConfig.getChapter(storyData.firstLevelNumber());
            if (chapter != null) {
                chapterColor = Color.web(chapter.getAccentColorHex());
            }
        }

        Text titleText = new Text(localizationManager.get(prefix + ".title"));
        titleText.setFont(Font.font("Orbitron", FontWeight.BOLD, 44));
        titleText.setFill(chapterColor);
        titleText.setTextAlignment(TextAlignment.LEFT);
        titleText.setOpacity(0.94);
        titleText.setEffect(new javafx.scene.effect.DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            chapterColor,
            24,
            0.45,
            0,
            0
        ));

        Text subtitleText = new Text(localizationManager.get(prefix + ".subtitle"));
        subtitleText.setFont(Font.font("Orbitron", FontWeight.SEMI_BOLD, 22));
        subtitleText.setFill(Color.rgb(200, 232, 255, 0.85));
        subtitleText.setTextAlignment(TextAlignment.LEFT);

        textColumn.getChildren().addAll(titleText, subtitleText);

        for (int index = 1; index <= 8; index++) {
            String key = prefix + ".paragraph" + index;
            String value = localizationManager.get(key);
            if (value == null || value.equals(key)) {
                break;
            }
            Text paragraph = new Text(value);
            paragraph.setFont(Font.font("Orbitron", FontWeight.NORMAL, 20));
            paragraph.setFill(Color.rgb(220, 236, 255, 0.88));
            paragraph.setTextAlignment(TextAlignment.LEFT);
            paragraph.setOpacity(0.0);
            paragraph.setWrappingWidth(540);
            paragraphTexts.add(paragraph);
            textColumn.getChildren().add(paragraph);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        textColumn.getChildren().add(spacer);

        hintText = new Text(localizationManager.get(prefix + ".cta"));
        hintText.setFont(Font.font("Orbitron", FontWeight.BOLD, 19));
        hintText.setFill(Color.web("#F2E86D"));
        hintText.setTextAlignment(TextAlignment.CENTER);
        hintText.setOpacity(0.0);
        hintText.setWrappingWidth(540);
        textColumn.getChildren().add(hintText);

        contentContainer.getChildren().addAll(imageWrapper, textColumn);

        if (backgroundNode != null) {
            getChildren().add(backgroundNode);
        }
        getChildren().addAll(overlayRect, contentContainer);

        setOpacity(0.0);

        var res = GameConfig.getCurrentResolution();
        adjustLayoutForResolution(res.getWidth(), res.getHeight());
    }

    private void setupInputHandlers() {
        setOnKeyPressed(this::handleKeyPressed);
        setOnMouseClicked(event -> {
            event.consume();
            handleAdvance(false);
        });
    }

    private void playIntro() {
        contentContainer.setTranslateY(40);

        fadeInTransition = new FadeTransition(INTRO_DURATION, this);
        fadeInTransition.setFromValue(0.0);
        fadeInTransition.setToValue(1.0);

        introSlideTransition = new TranslateTransition(INTRO_DURATION, contentContainer);
        introSlideTransition.setFromY(40);
        introSlideTransition.setToY(0);
        introSlideTransition.setInterpolator(Interpolator.EASE_OUT);

        fadeInTransition.setOnFinished(event -> startParagraphSequence());

        fadeInTransition.play();
        introSlideTransition.play();
    }

    private void startParagraphSequence() {
        if (paragraphTexts.isEmpty()) {
            paragraphsFullyVisible = true;
            if (hintText != null) {
                hintText.setOpacity(1.0);
                startHintPulse();
            }
            return;
        }

        paragraphsSequence = new SequentialTransition();
        for (Text paragraph : paragraphTexts) {
            FadeTransition fade = new FadeTransition(PARAGRAPH_FADE_DURATION, paragraph);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);
            paragraphsSequence.getChildren().add(fade);
        }

        if (hintText != null) {
            FadeTransition hintFade = new FadeTransition(Duration.millis(320), hintText);
            hintFade.setFromValue(0.0);
            hintFade.setToValue(1.0);
            hintFade.setInterpolator(Interpolator.EASE_OUT);
            paragraphsSequence.getChildren().add(hintFade);
        }

        paragraphsSequence.setOnFinished(event -> {
            paragraphsFullyVisible = true;
            startHintPulse();
        });
        paragraphsSequence.play();
    }

    private void startHintPulse() {
        if (hintText == null || hintPulseAnimation != null) {
            return;
        }
        hintPulseAnimation = new ScaleTransition(Duration.millis(1500), hintText);
        hintPulseAnimation.setFromX(1.0);
        hintPulseAnimation.setToX(1.045);
        hintPulseAnimation.setFromY(1.0);
        hintPulseAnimation.setToY(1.045);
        hintPulseAnimation.setAutoReverse(true);
        hintPulseAnimation.setCycleCount(Animation.INDEFINITE);
        hintPulseAnimation.play();
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.SPACE || code == KeyCode.ENTER) {
            event.consume();
            handleAdvance(false);
        } else if (code == KeyCode.ESCAPE) {
            event.consume();
            handleAdvance(true);
        }
    }

    private void handleAdvance(boolean forceClose) {
        if (closing) {
            return;
        }

        if (!paragraphsFullyVisible) {
            fastForwardParagraphs();
            if (!forceClose) {
                return;
            }
        }

        startOutro(forceClose);
    }

    private void fastForwardParagraphs() {
        if (paragraphsSequence != null) {
            paragraphsSequence.stop();
            paragraphsSequence = null;
        }
        for (Text paragraph : paragraphTexts) {
            paragraph.setOpacity(1.0);
        }
        if (hintText != null) {
            hintText.setOpacity(1.0);
        }
        paragraphsFullyVisible = true;
        startHintPulse();
    }

    private void startOutro(boolean immediate) {
        if (closing) {
            return;
        }
        closing = true;

        if (paragraphsSequence != null) {
            paragraphsSequence.stop();
            paragraphsSequence = null;
        }
        if (hintPulseAnimation != null) {
            try {
                hintPulseAnimation.stop();
            } catch (Exception ignored) {
            }
            hintPulseAnimation = null;
        }

        restoreAudioContext();

        fadeOutTransition = new FadeTransition(immediate ? Duration.millis(160) : Duration.millis(360), this);
        fadeOutTransition.setToValue(0.0);
        fadeOutTransition.setInterpolator(Interpolator.EASE_IN);
        fadeOutTransition.setOnFinished(event -> {
            cleanup();
            if (onDismissed != null) {
                onDismissed.run();
            }
        });
        fadeOutTransition.play();
    }

    private void startStoryMusic() {
        if (audioManager == null || !audioManager.isInitialized() || storyData == null) {
            return;
        }
        String musicPath = storyData.musicPath();
        if (musicPath == null || musicPath.isBlank()) {
            return;
        }

        double currentMultiplier = audioManager.getRuntimeMusicVolumeMultiplier();
        initialRuntimeMusicMultiplier = currentMultiplier;

        runMusicFade(currentMultiplier, 0.0, MUSIC_FADE_OUT, () -> {
            if (audioRestored) {
                return;
            }
            audioManager.saveCurrentMusic();
            audioManager.playMusic(musicPath, true);
            storyMusicStarted = true;
            audioManager.setRuntimeMusicVolumeMultiplier(0.0);
            runMusicFade(0.0, 1.0, MUSIC_FADE_IN, null);
        });
    }

    private void restoreAudioContext() {
        if (audioRestored || audioManager == null || !audioManager.isInitialized()) {
            return;
        }

        double startMultiplier = audioManager.getRuntimeMusicVolumeMultiplier();
        runMusicFade(startMultiplier, 0.0, MUSIC_FADE_OUT, () -> {
            boolean restored = false;
            if (storyMusicStarted) {
                audioManager.stopMusic();
                storyMusicStarted = false;
                restored = audioManager.restorePreviousMusic();
            } else {
                restored = audioManager.restorePreviousMusic();
            }
            double current = audioManager.getRuntimeMusicVolumeMultiplier();
            double target = initialRuntimeMusicMultiplier <= 0.0 ? 1.0 : initialRuntimeMusicMultiplier;
            runMusicFade(current, target, Duration.millis(320), () -> {
                audioManager.setRuntimeMusicVolumeMultiplier(target);
                audioRestored = true;
            });
            if (!restored && !audioManager.isMusicPlaying()) {
                audioManager.setRuntimeMusicVolumeMultiplier(target);
                audioRestored = true;
            }
        });
    }

    @SuppressWarnings("unused")
    private void restoreAudioImmediately() {
        if (audioRestored) {
            return;
        }
        if (musicFadeTimeline != null) {
            musicFadeTimeline.stop();
            musicFadeTimeline = null;
        }
        if (audioManager != null && audioManager.isInitialized()) {
            if (storyMusicStarted) {
                audioManager.stopMusic();
                storyMusicStarted = false;
                audioManager.restorePreviousMusic();
            } else {
                audioManager.restorePreviousMusic();
            }
            // КРИТИЧНО: Всегда сбрасываем обратно к 1.0, так как следующий экран (LevelIntroView)
            // сам управляет громкостью и ожидает, что multiplier будет на 1.0
            double targetMultiplier = initialRuntimeMusicMultiplier <= 0.0 ? 1.0 : initialRuntimeMusicMultiplier;
            audioManager.setRuntimeMusicVolumeMultiplier(targetMultiplier);
        }
        audioRestored = true;
    }

    private void runMusicFade(double from, double to, Duration duration, Runnable onFinished) {
        final SDL2AudioManager manager = this.audioManager;
        if (manager == null || !manager.isInitialized()) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        if (musicFadeTimeline != null) {
            musicFadeTimeline.stop();
            musicFadeTimeline = null;
        }
        
        // КРИТИЧНО: Удаляем старый listener перед созданием нового
        if (musicFadeProgress != null && musicFadeListener != null) {
            try {
                musicFadeProgress.removeListener(musicFadeListener);
            } catch (Exception ignored) {
            }
        }

        musicFadeProgress = new SimpleDoubleProperty(from);
        // КРИТИЧНО: Сохраняем ссылку на listener для его удаления в cleanup()
        musicFadeListener = (obs, oldVal, newVal) -> {
            if (!audioRestored && manager != null) {
                manager.setRuntimeMusicVolumeMultiplier(newVal.doubleValue());
            }
        };
        musicFadeProgress.addListener(musicFadeListener);
        manager.setRuntimeMusicVolumeMultiplier(from);

        musicFadeTimeline = new Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(musicFadeProgress, from)),
            new javafx.animation.KeyFrame(duration, new javafx.animation.KeyValue(musicFadeProgress, to, Interpolator.EASE_BOTH))
        );
        musicFadeTimeline.setOnFinished(event -> {
            if (!audioRestored && manager != null) {
                manager.setRuntimeMusicVolumeMultiplier(to);
            }
            musicFadeTimeline = null;
            // КРИТИЧНО: Удаляем listener перед обнулением property
            if (musicFadeProgress != null && musicFadeListener != null) {
                try {
                    musicFadeProgress.removeListener(musicFadeListener);
                } catch (Exception ignored) {
                }
            }
            musicFadeProgress = null;
            musicFadeListener = null;
            if (onFinished != null && !audioRestored) {
                onFinished.run();
            }
        });
        musicFadeTimeline.play();
    }

    private void adjustLayoutForResolution(double width, double height) {
        if (overlayRect != null) {
            overlayRect.setWidth(width);
            overlayRect.setHeight(height);
        }

        if (contentContainer != null) {
            double targetWidth = Math.min(1180, Math.max(780, width * 0.78));
            contentContainer.setMaxWidth(targetWidth);
            contentContainer.setPrefWidth(targetWidth);
            contentContainer.setSpacing(Math.min(64, targetWidth * 0.06));
        }

        if (storyImageView != null) {
            double imageWidth = Math.min(420, Math.max(320, width * 0.28));
            storyImageView.setFitWidth(imageWidth);
            if (imageBackdrop != null) {
                imageBackdrop.setWidth(imageWidth + 120);
                imageBackdrop.setHeight(Math.min(540, Math.max(420, height * 0.65)));
            }
        }

        double wrapWidth = Math.min(560, Math.max(420, width * 0.42));
        for (Text paragraph : paragraphTexts) {
            paragraph.setWrappingWidth(wrapWidth);
        }
        if (hintText != null) {
            hintText.setWrappingWidth(wrapWidth);
        }
    }

    @Override
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;

        // КРИТИЧНО: Устанавливаем audioRestored = true КАК МОЖНО РАНЬШЕ,
        // чтобы остановить все асинхронные fade операции
        audioRestored = true;

        if (fadeInTransition != null) {
            try {
                fadeInTransition.stop();
            } catch (Exception ignored) {
            }
            fadeInTransition = null;
        }
        if (introSlideTransition != null) {
            try {
                introSlideTransition.stop();
            } catch (Exception ignored) {
            }
            introSlideTransition = null;
        }
        if (paragraphsSequence != null) {
            try {
                paragraphsSequence.stop();
            } catch (Exception ignored) {
            }
            paragraphsSequence = null;
        }
        if (fadeOutTransition != null) {
            try {
                fadeOutTransition.stop();
            } catch (Exception ignored) {
            }
            fadeOutTransition = null;
        }
        if (hintPulseAnimation != null) {
            try {
                hintPulseAnimation.stop();
            } catch (Exception ignored) {
            }
            hintPulseAnimation = null;
        }
        if (musicFadeTimeline != null) {
            try {
                musicFadeTimeline.stop();
            } catch (Exception ignored) {
            }
            musicFadeTimeline = null;
        }
        // КРИТИЧНО: Удаляем listener перед обнулением property
        if (musicFadeProgress != null && musicFadeListener != null) {
            try {
                musicFadeProgress.removeListener(musicFadeListener);
            } catch (Exception ignored) {
            }
        }
        musicFadeProgress = null;
        musicFadeListener = null;

        // КРИТИЧНО: Останавливаем музыку сюжета и принудительно сбрасываем multiplier
        // Это гарантирует, что следующий экран получит правильную громкость музыки
        if (audioManager != null && audioManager.isInitialized()) {
            if (storyMusicStarted) {
                audioManager.stopMusic();
                storyMusicStarted = false;
            }
            // КРИТИЧНО: Принудительно сбрасываем multiplier на случай, если fade не завершился
            audioManager.resetRuntimeMusicVolumeMultiplier();
            // Очищаем сохранённую музыку, так как переходим к новому экрану
            audioManager.clearSavedMusic();
        }

        setOnKeyPressed(null);
        setOnMouseClicked(null);

        ResponsiveLayoutHelper.unbind(this);

        // КРИТИЧНО: Очищаем все эффекты (DropShadow) перед удалением children
        clearAllEffects(this);

        UINodeCleanup.releaseImages(this);

        backgroundNode = null;
        storyImageView = null;
        imageBackdrop = null;
        hintText = null;
        paragraphTexts.clear();

        getChildren().clear();
        audioManager = null;
    }
    
    /**
     * Рекурсивно очищает все эффекты (DropShadow и др.) в дереве компонентов
     */
    private void clearAllEffects(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        
        // Очищаем эффекты у всех узлов
        node.setEffect(null);
        
        // Рекурсивно обрабатываем дочерние элементы
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                clearAllEffects(child);
            }
        }
    }
}


