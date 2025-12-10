<<<<<<< HEAD
package com.arcadeblocks;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.logging.ConsoleOutput;
import com.almasb.fxgl.logging.FileOutput;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.logging.LoggerLevel;
import com.arcadeblocks.config.AudioConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.GameLine;
import com.arcadeblocks.config.BonusConfig;
import com.arcadeblocks.config.BonusLevelConfig;
import com.arcadeblocks.persistence.GameSnapshot;
import com.arcadeblocks.gameplay.Ball;
import com.arcadeblocks.gameplay.Brick;
import com.arcadeblocks.gameplay.Projectile;
import com.arcadeblocks.gameplay.BonusType;
import com.arcadeblocks.gameplay.Paddle;
import com.arcadeblocks.gameplay.Bonus;
import com.arcadeblocks.gameplay.Boss;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.arcadeblocks.ui.MainMenuView;
import com.arcadeblocks.ui.LCGamesJingleView;
import com.arcadeblocks.ui.BonusTimerView;
import com.arcadeblocks.ui.GameplayUIView;
import com.arcadeblocks.ui.PauseView;
import com.arcadeblocks.ui.CountdownTimerView;
import com.arcadeblocks.ui.SettingsView;
import com.arcadeblocks.ui.ChapterStoryView;
import com.arcadeblocks.ui.SupportsCleanup;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.audio.SDL2AudioManager;
import com.arcadeblocks.story.ChapterStoryData;
import com.arcadeblocks.story.StoryConfig;
import com.arcadeblocks.utils.AppDataManager;
import com.arcadeblocks.utils.SaveManager;
import com.arcadeblocks.utils.ImageCache;
import com.arcadeblocks.ui.util.UINodeCleanup;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import java.io.File;
import java.util.Arrays;
import javafx.geometry.Point2D;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import javafx.scene.robot.Robot;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Main class for Arcade Blocks game
 */
public class ArcadeBlocksApp extends GameApplication {
    
    private SDL2AudioManager audioManager;
    private SaveManager saveManager;
    private com.arcadeblocks.video.VideoBackendFactory videoBackendFactory;
    private com.arcadeblocks.gameplay.Paddle paddleComponent;
    private boolean mouseClicksBlocked = false;
    private com.arcadeblocks.gameplay.LivesManager livesManager;
    private com.arcadeblocks.gameplay.ScoreManager scoreManager;
    private com.arcadeblocks.gameplay.BonusEffectManager bonusEffectManager;
    private BonusTimerView bonusTimerView;
    private GameplayUIView gameplayUIView;
    private ChapterStoryView activeChapterStoryView;
    private Group darknessOverlayGroup;
    private Rectangle darknessOverlayDimLayer;
    private ImageView darknessOverlayCapsule;
    private boolean darknessOverlayActive = false;
    private final List<Node> levelBackgroundNodes = new ArrayList<>();
    private final Set<Integer> shownChapterStoryChapters = new HashSet<>();
    private boolean alwaysShowChapterStory = false; // Debug flag: always show chapter windows
    private ApplicationMode applicationMode = ApplicationMode.RELEASE;
    
    // Key press flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean turboPressed = false;
    private int callBallActiveSources = 0;
    
    // Loading state flag
    private boolean isLoading = false;
    private boolean isLevelIntroActive = false;
    
    // Periodic cleanup for long gameplay sessions
    private double gameplayTimeAccumulator = 0.0;
    private static final double PERIODIC_CLEANUP_INTERVAL = 300.0; // 5 minutes
    private boolean isStoryOverlayActive = false;
    private boolean isVictorySequenceActive = false;
    private static final Duration LEVEL_FADE_DURATION = Duration.millis(450);
    private boolean launchLocked = false;
    private long launchLockToken = 0L;
    private boolean fadeOutActive = false;
    private boolean fadeOutCompleted = false;
    private final List<Runnable> fadeOutCallbacks = new ArrayList<>();
    // Список активных FadeTransition для очистки при переходе между уровнями
    private final List<FadeTransition> activeFadeTransitions = new ArrayList<>();
    private javafx.util.Duration autoLaunchDelay = Duration.seconds(5); // Задержка автозапуска мяча после смерти
    private javafx.util.Duration postFadeDelay = Duration.millis(150);
    private boolean autoLaunchScheduled = false;
    private final AtomicBoolean shutdownTriggered = new AtomicBoolean(false);

    public ArcadeBlocksApp() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> performShutdownIfNeeded(), "ArcadeBlocksShutdownHook"));
    }

    public Duration getLevelFadeDuration() {
        return LEVEL_FADE_DURATION;
    }

    public Duration getAutoLaunchDelay() {
        return autoLaunchDelay;
    }

    public Duration getPostFadeDelay() {
        return postFadeDelay;
    }

    private void scheduleAutoLaunch() {
        if (autoLaunchScheduled) {
            return;
        }
        autoLaunchScheduled = true;

        FXGL.runOnce(() -> {
            autoLaunchScheduled = false;
            if (!isLaunchLocked()) {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null && ballComponent.isAttachedToPaddle()) {
                        ballComponent.launchBall();
                    }
                }
            }
        }, autoLaunchDelay);
    }
    
    private boolean isTransitioning = false;
    private boolean isLevelCompleted = false;
    private boolean isGameOver = false;
    private int continueCount = 0;
    private int levelStartScore = 0;
    private int levelStartLives = 0;
    private int levelStartLevel = -1;
    private int livesLostThisLevel = 0;
    private int positiveBonusesCollectedThisLevel = 0;
    private int negativeBonusesCollectedThisLevel = 0;
    private double cachedTurboSpeed = GameConfig.DEFAULT_TURBO_SPEED;

    public double getTurboSpeed() {
        return cachedTurboSpeed;
    }

    public void updateTurboSpeed() {
        if (saveManager != null) {
            cachedTurboSpeed = saveManager.getTurboModeSpeed();
            if (paddleComponent != null) {
                paddleComponent.setTurboMultiplier(cachedTurboSpeed);
            }
        }
    }

    public int getContinueCount() {
        return continueCount;
    }

    public void incrementContinueCount() {
        this.continueCount++;
    }

    public void resetContinueCount() {
        this.continueCount = 0;
    }

    private void resetLevelRuntimeStats() {
        livesLostThisLevel = 0;
        positiveBonusesCollectedThisLevel = 0;
        negativeBonusesCollectedThisLevel = 0;
    }

    private void recordLevelCompletionStats(int levelNumber, double levelTimeSeconds) {
        if (saveManager == null || isDebugMode) {
            return;
        }
        if (saveManager.isLevelStatsRecorded(levelNumber)) {
            return;
        }
        GameLine line = GameLine.fromLevel(levelNumber);
        saveManager.addLineTimeSeconds(line, Math.max(0, levelTimeSeconds));
        saveManager.addLineBonuses(line, positiveBonusesCollectedThisLevel, negativeBonusesCollectedThisLevel);
        saveManager.markLevelStatsRecorded(levelNumber);
    }

    private void ensureLevelStarsPersisted(int levelNumber) {
        if (saveManager == null || isDebugMode) {
            return;
        }
        int safeLivesLost = Math.max(0, livesLostThisLevel);
        int filledStars = Math.min(5, Math.max(1, 5 - safeLivesLost));
        saveManager.setLevelStars(levelNumber, filledStars);
    }

    private void recordLevelStartStats(int levelNumber) {
        levelStartLevel = levelNumber;
        levelStartScore = Math.max(0, FXGL.geti("score"));
        levelStartLives = Math.max(1, FXGL.geti("lives"));
    }

    public void onLifeLost() {
        livesLostThisLevel++;
    }

    public void onBonusCollected(BonusType bonusType) {
        if (bonusType == null) {
            return;
        }
        if (bonusType.isPositive()) {
            positiveBonusesCollectedThisLevel++;
        } else {
            negativeBonusesCollectedThisLevel++;
        }
    }

    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    public void setSuppressLevelCompletionChecks(boolean suppress) {
        this.suppressLevelCompletionChecks = suppress;
    }
    
    // Setter for the GameOverView flag to optimize onUpdate
    public void setGameOverViewVisible(boolean visible) {
        this.isGameOverViewVisible = visible;
    }

    public void captureLastMusicState() {
        if (audioManager != null) {
            this.restoredMusicFile = audioManager.getCurrentMusicFile();
            this.restoredMusicTime = audioManager.getCurrentMusicPosition();
        }
    }

    public void resetBallAndPaddle() {
        boolean skipFade = bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive();

        Runnable resetTask = () -> {
            var allBalls = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.BALL);
            for (Entity ball : allBalls) {
                ball.removeFromWorld();
            }

            Entity paddle = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.PADDLE).stream()
                .findFirst().orElse(null);

            if (paddle != null) {
                double currentPaddleX = paddle.getX();
                double currentPaddleY = paddle.getY();

                com.arcadeblocks.gameplay.Paddle paddleComponent = paddle.getComponent(com.arcadeblocks.gameplay.Paddle.class);
                if (paddleComponent != null) {
                    paddleComponent.setMoveLeft(false);
                    paddleComponent.setMoveRight(false);
                    paddleComponent.setMovementBlocked(true);
                }

                resetPaddleInputFlags();

                com.almasb.fxgl.physics.PhysicsComponent paddlePhysics = paddle.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                if (paddlePhysics != null) {
                    paddlePhysics.setLinearVelocity(0, 0);
                    paddlePhysics.setBodyType(com.almasb.fxgl.physics.box2d.dynamics.BodyType.KINEMATIC);
                }

                double basePaddleWidth = GameConfig.PADDLE_WIDTH;
                if (paddleComponent != null) {
                    basePaddleWidth *= paddleComponent.getSizeMultiplier();
                }

                double defaultOffsetX = basePaddleWidth * 0.18;
                double defaultOffsetY = -GameConfig.BALL_RADIUS * 2 - 5;
                double ballX = currentPaddleX + basePaddleWidth / 2.0 + defaultOffsetX - GameConfig.BALL_RADIUS;
                double ballY = currentPaddleY + defaultOffsetY;

                Entity newBall = FXGL.spawn("ball", ballX, ballY);
                Ball ballComponent = new Ball();
                newBall.addComponent(ballComponent);

                if (bonusEffectManager != null && bonusEffectManager.isChaoticBallsActive()) {
                    ballComponent.setChaoticBall(true);
                }

                if (bonusEffectManager != null && bonusEffectManager.isWeakBallsActive()) {
                    ballComponent.setWeakBall(true);
                }

                if (bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive()) {
                ballComponent.setStickyEnabled(true);
            }

                ballComponent.setAttachedOffset(defaultOffsetX, defaultOffsetY);
                ballComponent.attachToPaddle(paddle);

                // Unblock paddle control after 1 second delay
                FXGL.runOnce(() -> {
                    if (paddleComponent != null) {
                        paddleComponent.setMovementBlocked(false);
                    }
                    if (paddlePhysics != null) {
                        paddlePhysics.setBodyType(com.almasb.fxgl.physics.box2d.dynamics.BodyType.KINEMATIC);
                        paddlePhysics.setLinearVelocity(0, 0);
                    }
                }, Duration.seconds(1));
                
                // Play fade in animation immediately
                playPaddleBallFadeIn(skipFade);
                scheduleAutoLaunch();
            }
        };

        fadeOutPaddleAndBalls(skipFade, resetTask);
    }

    public void continueFromGameOver() {
        // Stop Game Over music
        if (audioManager != null) {
            audioManager.stopMusic();
        }

        // Reset Game Over flags and click blocking to restore control
        setGameOver(false);
        // Reset the GameOverView flag to optimize onUpdate
        isGameOverViewVisible = false;
        mouseClicksBlocked = false;

        // Handle score and continue count
        int cost = (getContinueCount() + 1) * 10000;
        if (scoreManager != null) {
            scoreManager.setPersistenceEnabled(true);
            scoreManager.addScore(-cost);
            FXGL.set("score", Math.max(0, scoreManager.getCurrentScore()));
            scoreManager.flushPendingOperations();
        }
        incrementContinueCount();

        // Reset lives
        if (livesManager != null) {
            livesManager.resetLives();
        }

        // Reset UI and cursor
        clearUINodesSafely();
        gameplayUIView = new GameplayUIView(this);
        FXGL.getGameScene().addUINode(gameplayUIView);
        if (scoreManager != null && scoreManager.getBonusIndicator() != null) {
            FXGL.getGameScene().addUINode(scoreManager.getBonusIndicator());
        }
        setHiddenCursor();

        int currentLevel = FXGL.geti("level");
        com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(currentLevel);
        String levelName = levelData != null ? levelData.getName() : null;
        if (gameplayUIView != null) {
            gameplayUIView.updateLevel(currentLevel, levelName);
            if (scoreManager != null) {
                gameplayUIView.updateScore(scoreManager.getCurrentScore());
            }
            if (livesManager != null) {
                gameplayUIView.updateLives(Math.max(0, livesManager.getCurrentLives()));
            }
        }
        if (scoreManager != null) {
            scoreManager.updateLevel(currentLevel, levelName);
        }

        // Reset game world state
        resetBallAndPaddle();
        refreshLevelBackground();

        // Play countdown sound
        if (audioManager != null) {
            audioManager.playSFX("sounds/menu_select.wav");
        }

        // Start countdown (which will handle resuming the music)
        showCountdownTimerForLevelStart(FXGL.geti("level"));
    }
    
    // Pause state flag
    private boolean isPaused = false;
    
    // Flag for settings from pause menu
    private boolean inPauseSettings = false;
    
    // Flag for countdown timer
    private boolean isCountdownActive = false;
    
    // Initially selected save slot (for creating new save after Game Over)
    private int originalSaveSlot = 1;
    
    // Current main menu background (for preserving during navigation)
    private String currentMainMenuBackground = null;
    
    // Flag to prevent repeated level completion messages
    private boolean levelCompletedMessageShown = false;

    // Flag to prevent repeated calls to proceedToNextLevel
    private boolean proceedToNextLevelCalled = false;
    private Integer pendingLevelWarpTarget = null;
    private boolean isDebugMode = false; // Debug mode flag
    // Flag to optimize GameOverView checking in onUpdate (instead of stream)
    private volatile boolean isGameOverViewVisible = false;
    private com.arcadeblocks.config.DifficultyLevel debugDifficultyOverride = null;
    private Integer debugLivesOverride = null;
    private Integer debugScoreOverride = null;

    // Level completion bonus
    private boolean levelPassBonusSpawned = false;
    private Entity levelPassBonusEntity;
    
    // Cursor status and forced timer
    private boolean cursorHidden = false;
    private javafx.animation.Timeline cursorEnforcer;
    
    // Video player tracking (now handled by VlcContext)
    
    // Tracking active PauseTransitions for videos (critical for preventing leaks)
    private final java.util.List<javafx.animation.PauseTransition> activeVideoPauseTransitions = new java.util.ArrayList<>();
    
    // Token to prevent race conditions when quickly restarting levels via the debug menu.
    // Each time video resources are cleaned up, the token is incremented, allowing old callbacks to understand
    // that they belong to an outdated video session and should not perform final cleanup.
    private volatile long videoSessionToken = 0;
    
    /**
     * Wrapper for tracking video overlay along with its backend
     * need to store a reference to the backend for proper cleanup when removing the overlay.
     */
    private static class VideoOverlayWrapper {
        final javafx.scene.Node overlay;
        final com.arcadeblocks.video.VideoPlayerBackend backend;
        
        VideoOverlayWrapper(javafx.scene.Node overlay, com.arcadeblocks.video.VideoPlayerBackend backend) {
            this.overlay = overlay;
            this.backend = backend;
        }
    }
    
    // Tracking active video overlays for cleaning
    private final java.util.List<VideoOverlayWrapper> activeVideoOverlays = new java.util.ArrayList<>();
    
    // Mouse control for paddle
    private boolean mouseHandlersInstalled = false;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseMoveHandler;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseDragHandler;
    // Jerk suppression: ignore first mouse movement after keyboard input
    private boolean suppressMouseUntilMove = false;
    
    // Cursor confinement within game window
    private boolean confineCursorEnabled = false;
    private boolean confineFocusListenerInstalled = false;
    private boolean isWarpingMouse = false;
    private java.awt.Robot awtRobot;
    private javafx.animation.Timeline confineTicker;
    private javafx.animation.Timeline mouseFollowTicker;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseExitHandler;
    private AWTEventListener globalMouseConfineListener;
    private final AtomicBoolean confineRequestScheduled = new AtomicBoolean(false);
    private Robot fxRobot;
    private boolean pauseResumeLockFromFocusLoss = false;
    private boolean suppressLevelCompletionChecks = false;
    private boolean vsyncEnabled = true;
    private boolean vsyncConfigured = false;

    // Input Actions
    private UserAction paddleLeftAction;
    private UserAction paddleRightAction;
    private UserAction launchBallAction;
    private UserAction turboPaddleAction;
    private UserAction callBallAction;
    private UserAction mouseCallBallAction;
    private UserAction turboBallAction;
    private UserAction turboBallMouseAction;
    private UserAction plasmaWeaponAction;
    private UserAction pauseAction;
    private UserAction mouseLaunchBallAction;
    private UserAction mousePlasmaWeaponAction;
    private UserAction destroyAllBricksAction;
    private UserAction skipToLevel100Action;
    
    // Current level audio settings
    private String currentLevelLoadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
    private List<String> currentBrickHitSounds = new ArrayList<>(AudioConfig.getDefaultBrickHitSounds());
    private List<String> currentLevelCompletionSounds = new ArrayList<>(AudioConfig.getDefaultLevelCompletionSounds());
    private final Random levelAudioRandom = new Random();

    // State for restoring music
    private String restoredMusicFile = null;
    private double restoredMusicTime = 0.0;
    
    @Override
    protected void onPreInit() {
        configureRuntimeStorage();
    }
    
    private void configureRuntimeStorage() {
        try {
            // Disable file logging to avoid permission issues in Program Files
            // Only use console output for logging
            Logger.removeAllOutputs();
            var consoleLevel = applicationMode != null ? applicationMode.getLoggerLevel() : LoggerLevel.INFO;
            Logger.addOutput(new ConsoleOutput(), consoleLevel);
            
            // Ensure data directories exist
            AppDataManager.getLogsDirectory();
            AppDataManager.getFxglDirectory();
        } catch (Exception ex) {
            System.err.println("Unable to reconfigure logging: " + ex.getMessage());
        }
    }
    
    @Override
    protected void initSettings(GameSettings settings) {
        // Disable file logging to avoid permission issues
        settings.setFileSystemWriteAllowed(false);
        
        // Load the saved resolution from the database
        com.arcadeblocks.config.Resolution savedResolution = loadResolutionFromDatabase();
        GameConfig.setCurrentResolution(savedResolution);
        
        // Set the window size according to the saved resolution.
        settings.setWidth(savedResolution.getWidth());
        settings.setHeight(savedResolution.getHeight());
        settings.setTitle("Arcade Blocks");
        settings.setVersion("1.20");
        settings.setMainMenuEnabled(false);
        
        // Setting up the app icon
        settings.setAppIcon("favicon-32.png");
        settings.setGameMenuEnabled(false);
        settings.setIntroEnabled(false);
        settings.setProfilingEnabled(false);
        settings.setCloseConfirmation(false); // Disable standard confirmation, handle manually
        
        // Fullscreen mode - load settings from database
        settings.setFullScreenAllowed(true);
        
        // Load window mode settings from database
        boolean startInFullscreen = loadWindowModeFromDatabase();
        settings.setFullScreenFromStart(startInFullscreen);
        
        // Performance settings
        settings.setPreserveResizeRatio(true);   // Preserve aspect ratio
        settings.setManualResizeEnabled(false);  // BLOCK manual resizing
        settings.setScaleAffectedOnResize(false); // DO NOT scale content
        vsyncEnabled = loadVSyncFromDatabase();
        applyVSyncFlag(vsyncEnabled);
        settings.setTicksPerSecond(-1); // Use JavaFX pulse (vsync) by default
        
        // Window centering settings (if supported)
        // settings.setWindowCenteredOnStartup(true); // Not supported in this FXGL version
        
        // Set window style for borderless mode switching capability
        // settings.setWindowStyle(WindowStyle.UNDECORATED); // Not supported in this FXGL version
        
        // Custom FXGL cursor disabled - system cursor will be used
        
        applicationMode = settings.getApplicationMode();
    }
    
    /**
     * Load resolution settings from database
     * @return saved resolution or default resolution
     */
    private com.arcadeblocks.config.Resolution loadResolutionFromDatabase() {
        com.arcadeblocks.utils.DatabaseManager tempDb = null;
        try {
            // Создаем временный DatabaseManager для чтения настроек
            tempDb = new com.arcadeblocks.utils.DatabaseManager();
            tempDb.initialize();
            
            // Load resolution settings
            String resolutionValue = tempDb.getSetting("resolution");
            
            if (resolutionValue != null && !resolutionValue.isEmpty()) {
                com.arcadeblocks.config.Resolution resolution = com.arcadeblocks.config.Resolution.fromString(resolutionValue);
                // System.out.println("Loaded resolution from database: " + resolution);
                return resolution;
            } else {
                // System.out.println("Resolution not found in database, using default: " + GameConfig.DEFAULT_RESOLUTION);
                return GameConfig.DEFAULT_RESOLUTION;
            }
        } catch (Exception e) {
            System.err.println("Error loading resolution from database: " + e.getMessage());
            return GameConfig.DEFAULT_RESOLUTION;
        } finally {
            // ВАЖНО: закрываем соединение с БД
            if (tempDb != null) {
                tempDb.close();
            }
        }
    }

    private boolean loadVSyncFromDatabase() {
        com.arcadeblocks.utils.DatabaseManager tempDb = null;
        try {
            tempDb = new com.arcadeblocks.utils.DatabaseManager();
            tempDb.initialize();
            String vsyncValue = tempDb.getSetting("vsync_enabled");
            if (vsyncValue == null) {
                vsyncValue = tempDb.getSetting("fps_limit_enabled");
            }
            if (vsyncValue != null) {
                return Boolean.parseBoolean(vsyncValue);
            }
        } catch (Exception e) {
            System.err.println("Error loading vsync setting from database: " + e.getMessage());
        } finally {
            // ВАЖНО: закрываем соединение с БД
            if (tempDb != null) {
                tempDb.close();
            }
        }
        return true;
    }
    
    /**
     * Load window mode settings from database
     * @return true if should start in fullscreen mode
     */
    private boolean loadWindowModeFromDatabase() {
        com.arcadeblocks.utils.DatabaseManager tempDb = null;
        try {
            // Создаем временный DatabaseManager для чтения настроек
            tempDb = new com.arcadeblocks.utils.DatabaseManager();
            tempDb.initialize();
            
            // Load window mode settings
            String windowedModeValue = tempDb.getSetting("windowed_mode");
            String fullscreenValue = tempDb.getSetting("fullscreen");
            
            boolean isWindowed = windowedModeValue != null ? Boolean.parseBoolean(windowedModeValue) : false;
            boolean isFullscreen = fullscreenValue != null ? Boolean.parseBoolean(fullscreenValue) : true;
            
            // If windowed mode is enabled, start in windowed mode
            if (isWindowed) {
        // System.out.println("Settings loaded: starting in windowed mode");
                return false;
            } else if (isFullscreen) {
        // System.out.println("Settings loaded: starting in fullscreen mode");
                return true;
            } else {
                // Default to fullscreen mode
        // System.out.println("Settings loaded: starting in fullscreen mode (default)");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error loading window mode settings: " + e.getMessage());
            // In case of error, use fullscreen mode by default
        // System.out.println("Using default fullscreen mode due to error");
            return true;
        } finally {
            // ВАЖНО: закрываем соединение с БД
            if (tempDb != null) {
                tempDb.close();
            }
        }
    }
    
    @Override
    protected void initGame() {
        // Register entity factory
        FXGL.getGameWorld().addEntityFactory(new ArcadeBlocksFactory());
        
        // Initialize game properties
        FXGL.set("score", 0);
        FXGL.set("level", 1);
        
        // Initialize managers
        audioManager = new SDL2AudioManager();
        saveManager = new SaveManager();
        
        // Initialize video backend factory
        videoBackendFactory = new com.arcadeblocks.video.VideoBackendFactory();
        // System.out.println(videoBackendFactory.getBackendInfo());

        // Initialize input actions and load bindings now that saveManager is available.
        initInputActions();
        loadInputBindings();
        
        // Initialize audio system
        // System.out.println("Initializing audio system...");
        
        // Load settings
        saveManager.loadSettings();
        // Apply cached turbo speed from settings before gameplay objects are created
        updateTurboSpeed();

        // Apply saved language
        LocalizationManager.getInstance().setLanguage(saveManager.getLanguage());
        
        // Initialize control keys after saveManager initialization
        loadInputBindings();
        
        // Load debug menu settings
        saveManager.loadDebugSettingsToBonusConfig();
        
        // Configure audio
        audioManager.setMasterVolume(saveManager.getMasterVolume());
        audioManager.setMusicVolume(saveManager.getMusicVolume());
        audioManager.setSfxVolume(saveManager.getSfxVolume());
        audioManager.setSoundEnabled(saveManager.isSoundEnabled());

        javafx.application.Platform.runLater(this::applySavedVSync);
        
        // Preload frequently used sounds
        if (audioManager.isInitialized()) {
            audioManager.preloadCommonSounds();
        }
        
        // Force set system cursor instead of blue FXGL cursor
        setSystemCursor();

        // Monitor UI layer changes to automatically center new elements
        FXGL.getGameScene().getUINodes().addListener((ListChangeListener<javafx.scene.Node>) change -> centerAllUINodes());
        centerAllUINodes();
        
        // Bonus statistics output
        // System.out.println("=== BONUS SETTINGS ===");
        // System.out.println(BonusConfig.getBonusStatistics());
        // System.out.println("Included bonuses: " + BonusType.getEnabledBonusesCount());
        // System.out.println("Positive: " + BonusType.getEnabledPositiveBonusesCount());
        // System.out.println("Negative: " + BonusType.getEnabledNegativeBonusesCount());
        // System.out.println("========================");
        
        // Start initial loading sequence (jingle + loading screen)
        isLoading = true; // Set loading flag
        startInitialLoadingSequence();
        
        // Apply fullscreen mode settings and window close handler (after UI initialization)
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                if (stage != null) {
                    // Apply window settings
                    applyWindowSettings();
                    
                    // Window close handler
                    stage.setOnCloseRequest(event -> {
        // System.out.println("The window close handler has been called!"); // Debugging
                        event.consume(); // Cancel standard close
                        
                        // Block exit during loading
                        if (!isLoading) {
                            handleWindowClose();
                        }
                    });
                    
                    // Additional handler for reliability
                    stage.setOnHiding(event -> {
        // System.out.println("The window is hiding!"); // Debugging
                        event.consume();
                        
                        // Block exit during loading
                        if (!isLoading) {
                            handleWindowClose();
                        }
                    });
                }
            } catch (Exception e) {
                // System.err.println("Failed to apply window settings: " + e.getMessage());
            }
        });
        
        // Additional handler setup after loading completion
        javafx.animation.Timeline setupHandlerTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5.5), e -> {
                // Set window close handler after loading completion
                javafx.application.Platform.runLater(() -> {
                    try {
                        Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                        if (stage != null) {
                            // Update window close handler
                            stage.setOnCloseRequest(event -> {
        // System.out.println("Window close handler called (after loading)!"); // Debugging
                                event.consume(); // Cancel standard close
                                handleWindowClose();
                            });
                            
                            // Update additional handler
                            stage.setOnHiding(event -> {
        // System.out.println("The window disappears (after loading)!"); // Debugging
                                event.consume();
                                handleWindowClose();
                            });
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to update window handler: " + ex.getMessage());
                    }
                });
            })
        );
        setupHandlerTimeline.play();
    }
    
    private void startInitialLoadingSequence() {
        try {
            LCGamesJingleView jingleView = new LCGamesJingleView(() ->
                javafx.application.Platform.runLater(this::showLoadingViewAfterJingle)
            );
            FXGL.getGameScene().addUINode(jingleView);
        } catch (Exception e) {
            System.err.println("Failed to start LCGames jingle: " + e.getMessage());
            javafx.application.Platform.runLater(this::showLoadingViewAfterJingle);
        }
    }
    
    private void showLoadingViewAfterJingle() {
        // LCGamesJingleView should already be removed via its callback in playAnimation()
        // But just in case, check and remove if it still exists
        var scene = FXGL.getGameScene();
        var existingNodes = scene.getUINodes();
        for (javafx.scene.Node node : existingNodes) {
            if (node instanceof com.arcadeblocks.ui.LCGamesJingleView) {
                try {
                    ((com.arcadeblocks.ui.LCGamesJingleView) node).cleanup();
                    scene.removeUINode(node);
                } catch (Exception ignored) {
                    // Ignore errors, just try to remove
                    try {
                        scene.removeUINode(node);
                    } catch (Exception ignored2) {}
                }
                break; // Remove only the first one found
            }
        }
        
        com.arcadeblocks.ui.LoadingView loadingView = new com.arcadeblocks.ui.LoadingView(this);
        loadingView.setOpacity(0.0);
        FXGL.getGameScene().addUINode(loadingView);
        
        // We save the reference to FadeTransition in LoadingView to stop it during cleanup().
        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(400), loadingView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            // Clear reference after animation completion
            fadeIn.setOnFinished(null);
        });
        loadingView.setExternalFadeIn(fadeIn);
        fadeIn.play();
        
        // Save the reference to loadingView for use in callbacks.
        // This is necessary to avoid capturing the reference in lambda, which may hold the object.
        final com.arcadeblocks.ui.LoadingView loadingViewRef = loadingView;
        loadingView.setOnSkipCallback(() -> {
            // КРИТИЧНО: Проверяем, что loadingView еще существует в сцене
            if (FXGL.getGameScene().getUINodes().contains(loadingViewRef)) {
                completeLoading(loadingViewRef);
            }
        });
        
        loadingView.setOnCompleteCallback(() -> {
            // Check that loadingView still exists in the scene
            if (FXGL.getGameScene().getUINodes().contains(loadingViewRef)) {
                // Do not pass loadingView to startMainMenuMusic to avoid memory leaks.
                startMainMenuMusic();
                transitionToMainMenu(loadingViewRef);
            }
        });
        
        // Automatic loading completion after 4 seconds if user didn't skip the screen
        // Save the link to Timeline in LoadingView to stop it during cleanup()
        javafx.animation.Timeline loadingTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(4.0), e -> {
                // Check that LoadingView still exists in the scene before calling completeLoading()
                // This prevents memory leaks and errors when LoadingView is removed prematurely
                if (FXGL.getGameScene().getUINodes().contains(loadingView) && isLoading) {
                    isLoading = false;
                    loadingView.completeLoading();
                }
                // If LoadingView is already removed, Timeline will just finish without calling completeLoading()
            })
        );
        loadingView.setExternalTimeline(loadingTimeline);
        loadingTimeline.play();
    }
    
    @Override
    protected void initUI() {
        // Load CSS styles for dark background
        try {
            String cssPath = "/dark-theme.css";
            FXGL.getGameScene().getRoot().getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Failed to load CSS styles: " + e.getMessage());
        }
        
        // Set dark background for window (remove white bars)
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.Scene scene = FXGL.getGameScene().getRoot().getScene();
                if (scene != null) {
                    // Set dark background matching game design
                    scene.setFill(javafx.scene.paint.Color.web(GameConfig.DARK_BACKGROUND));
                    
                    // Also set style for entire window
                    scene.getRoot().setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
                    
                    // Set background for game window itself
                    FXGL.getGameScene().getRoot().setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
                    
                    // Set background for FXGL scene as well
                    FXGL.getGameScene().setBackgroundColor(javafx.scene.paint.Color.web(GameConfig.DARK_BACKGROUND));
                    
                    // Additionally set style for Stage
                    Stage stage = (Stage) scene.getWindow();
                    if (stage != null) {
                        stage.getScene().getRoot().setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
                        
                        // Icon is set via FXGL settings.setAppIcon()
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to set background color: " + e.getMessage());
            }
        });
    }
    
    @Override
    protected void initPhysics() {
        // Disable gravity for weightlessness effect
        FXGL.getPhysicsWorld().setGravity(0, 0);
        
        // FXGL physics engine (Box2D) works with improved accuracy
        // thanks to CCD (Continuous Collision Detection) enabled for the ball
        // and increased number of checks in handleContinuousBrickCollisions()
        // System.out.println("Physics engine initialized with CCD to prevent tunneling");
        
        // Configure collisions
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity paddle) {
                // Handle ball collision with paddle
                Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    ballComponent.onPaddleHit(paddle);
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.BRICK) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity brick) {
                // Handle ball collision with brick
                Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    // If ball is energy ball, it passes through the brick
                    if (ballComponent.isEnergyBall()) {
                        ballComponent.onBrickHit(brick);
                        return; // Don't process physical collision
                    }
                    ballComponent.onBrickHit(brick);
                }
            }
        });
        
        // Ball-ball collision handler (disable for attached balls)
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.BALL) {
            @Override
            protected void onCollisionBegin(Entity ball1, Entity ball2) {
                Ball ball1Component = ball1.getComponent(Ball.class);
                Ball ball2Component = ball2.getComponent(Ball.class);
                
                if (ball1Component != null && ball2Component != null) {
                    // If one of the balls is attached to paddle, ignore collision
                    if (ball1Component.isAttachedToPaddle() || ball2Component.isAttachedToPaddle()) {
                        return; // Ignore collision
                    }
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.POWERUP, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity powerup, Entity paddle) {
                // Handle powerup pickup
                // Logic will be implemented via components
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BONUS, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity bonus, Entity paddle) {
                // Handle bonus pickup
                com.arcadeblocks.gameplay.Bonus bonusComponent = bonus.getComponent(com.arcadeblocks.gameplay.Bonus.class);
                if (bonusComponent != null) {
                    bonusComponent.collect();
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PROJECTILE, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity projectile, Entity paddle) {
                Projectile projectileComponent = projectile.getComponent(Projectile.class);
                if (projectileComponent != null && projectileComponent.isBossProjectile()) {
                    handleBossProjectileHit(projectileComponent, projectile);
                } else {
                    // Ignore instant collision of player with their own paddle (first milliseconds)
                    if (projectileComponent != null && projectileComponent.shouldIgnorePaddleCollision()) {
                        return;
                    }
                    projectile.removeFromWorld();
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PROJECTILE, EntityType.BRICK) {
            @Override
            protected void onCollisionBegin(Entity projectile, Entity brick) {
                // Handle projectile collision with brick
                Projectile projectileComponent = projectile.getComponent(Projectile.class);
                if (projectileComponent != null) {
                    // Destroy brick
                    Brick brickComponent = brick.getComponent(Brick.class);
                    if (brickComponent != null) {
                        // Check if brick is explosive
                        if (brickComponent.isExplosive()) {
                            // If brick is explosive, trigger explosion before destruction
                            brickComponent.explodeNearbyBricks();
                        }
                        boolean isPlayerPlasma = !projectileComponent.isBossProjectile()
                            && "player".equalsIgnoreCase(projectileComponent.getOwner());
                        // Use special plasma destroy for player plasma shots
                        if (isPlayerPlasma) {
                            brickComponent.destroyByPlasma();
                        } else {
                            brickComponent.destroy();
                        }
                    }
                    
                    // Remove projectile
                    projectile.removeFromWorld();
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PROJECTILE, EntityType.BALL) {
            @Override
            protected void onCollisionBegin(Entity projectile, Entity ball) {
                Projectile projectileComponent = projectile.getComponent(Projectile.class);
                Ball ballComponent = ball.getComponent(Ball.class);
                
                if (projectileComponent != null && ballComponent != null) {
                    // Check if projectile is player's plasma weapon (not boss)
                    boolean isPlasmaWeapon = !projectileComponent.isBossProjectile() && 
                                             "player".equalsIgnoreCase(projectileComponent.getOwner());
                    
                    if (isPlasmaWeapon) {
                        // If this is plasma weapon and hit ball - count life loss
                        // Check if ball is not an extra ball
                        if (!ballComponent.isExtraBall()) {
                            // Check if life loss was already in progress BEFORE calling loseLife()
                            boolean wasLifeLossInProgress = getLivesManager() != null && 
                                getLivesManager().isLifeLossInProgress();
                            
                            // Trigger life loss via LivesManager
                            if (getLivesManager() != null) {
                                getLivesManager().loseLife();
                            }
                            
                            // If life loss was already in progress (lifeLossInProgress was true),
                            // then loseLife() was ignored, and we need to clear bonuses directly.
                            // This ensures bonuses will be cleared even if loseLife() didn't execute.
                            if (wasLifeLossInProgress) {
                                // Life loss already in progress - clear bonuses immediately,
                                // so they are guaranteed to disappear from screen
                                fadeOutBonuses(false, () -> {
                                    if (getBonusEffectManager() != null) {
                                        getBonusEffectManager().clearAllBonuses();
                                        getBonusEffectManager().resetAllEffects();
                                    }
                                    // Also remove all falling bonuses from world
                                    var bonusEntities = new java.util.ArrayList<>(
                                        FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
                                    for (Entity bonusEntity : bonusEntities) {
                                        bonusEntity.removeFromWorld();
                                    }
                                });
                            }
                        } else {
                            // Remove extra ball with smooth fade out animation
                            fadeOutBall(ball);
                        }
                        // Remove projectile
                        projectile.removeFromWorld();
                        return;
                    }
                }
                
                // For all other projectiles (e.g., boss projectiles) simply remove without consequences
                projectile.removeFromWorld();
            }
        });
        
        // Temporarily disable FXGL CollisionHandler for debugging
        // FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.BOSS) {
        //     @Override
        //     protected void onCollisionBegin(Entity ball, Entity boss) {
        //         System.out.println("FXGL CollisionHandler: the ball collided with the boss");
        //         Ball ballComponent = ball.getComponentOptional(Ball.class).orElse(null);
        //         Boss bossComponent = boss.getComponentOptional(Boss.class).orElse(null);
        //         if (ballComponent == null || bossComponent == null) {
        //             System.out.println("FXGL CollisionHandler: components not found");
        //             return;
        //         }
        //         System.out.println("FXGL CollisionHandler: deal damage to the boss: " + ballComponent.getBossDamage());
        //         bossComponent.takeDamage(ballComponent.getBossDamage());
        //     }
        // });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity wall) {
                // Handle ball collision with wall
                Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    // Check if this is a protective wall
                    try {
                        Boolean isProtectiveWall = wall.getBoolean("isProtectiveWall");
                        if (isProtectiveWall != null && isProtectiveWall) {
                            // Reflect ball from protective wall
                            ballComponent.onWallHit(wall);
                        }
                    } catch (Exception e) {
                        // If property doesn't exist, this is a regular wall - ignore
                        // Regular walls are already handled by physics engine
                    }
                }
            }
        });
    }
    
    @Override
    protected void initInput() {
        // Actions are initialized in initGame() after SaveManager is created.
    }

    private void initInputActions() {
        paddleLeftAction = new UserAction("Paddle Left") {
            @Override
            protected void onActionBegin() {
                leftPressed = true;
                // System.out.println("[Input] LEFT pressed = true");
            }
            @Override
            protected void onActionEnd() {
                leftPressed = false;
                // System.out.println("[Input] LEFT pressed = false");
            }
        };

        paddleRightAction = new UserAction("Paddle Right") {
            @Override
            protected void onActionBegin() {
                rightPressed = true;
                // System.out.println("[Input] RIGHT pressed = true");
            }
            @Override
            protected void onActionEnd() {
                rightPressed = false;
                // System.out.println("[Input] RIGHT pressed = false");
            }
        };

        launchBallAction = new UserAction("Launch Ball") {
            @Override
            protected void onActionBegin() {
                if (!canProcessLaunchInput()) return;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null && ballComponent.isAttachedToPaddle()) {
                        ballComponent.launchBall();
                    }
                }
            }
        };

        callBallAction = new UserAction("Call Ball") {
            @Override
            protected void onActionBegin() {
                activateCallBall();
            }

            @Override
            protected void onActionEnd() {
                deactivateCallBall();
            }
        };

        turboPaddleAction = new UserAction("Turbo Paddle") {
            @Override
            protected void onActionBegin() {
                turboPressed = true;
            }
            @Override
            protected void onActionEnd() {
                turboPressed = false;
            }
        };

        plasmaWeaponAction = new UserAction("Plasma Weapon") {
            @Override
            protected void onActionBegin() {
                if (bonusEffectManager != null && bonusEffectManager.isPlasmaWeaponActive()) {
                    bonusEffectManager.firePlasmaShot();
                }
            }
        };

        pauseAction = new UserAction("Pause") {
            @Override
            protected void onActionBegin() {
                if (!isGameplayState() || isLoading || isLevelIntroActive || isStoryOverlayActive || isCountdownActive || isLevelCompleted || isVictorySequenceActive) return;
                if (FXGL.getGameScene().getUINodes().stream().anyMatch(node -> node instanceof com.arcadeblocks.ui.GameOverView)) return;
                // Check for the presence of PauseView in the scene before switching the pause
                var scene = FXGL.getGameScene();
                boolean hasPauseView = scene.getUINodes().stream()
                    .anyMatch(node -> node instanceof com.arcadeblocks.ui.PauseView);
                if (hasPauseView || isPaused) {
                    resumeGame();
                } else {
                    pauseGame();
                }
            }
        };

        mouseLaunchBallAction = new UserAction("Mouse Launch") {
            @Override
            protected void onActionBegin() {
                if (!canProcessLaunchInput()) return;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null && ballComponent.isAttachedToPaddle()) {
                        ballComponent.launchBall();
                    }
                }
            }
        };

        mouseCallBallAction = new UserAction("Mouse Call Ball") {
            @Override
            protected void onActionBegin() {
                if (mouseClicksBlocked) return;
                activateCallBall();
            }

            @Override
            protected void onActionEnd() {
                deactivateCallBall();
            }
        };

        mousePlasmaWeaponAction = new UserAction("Mouse Plasma Weapon") {
            @Override
            protected void onActionBegin() {
                if (mouseClicksBlocked) return;
                if (bonusEffectManager != null && bonusEffectManager.isPlasmaWeaponActive()) {
                    bonusEffectManager.firePlasmaShot();
                }
            }
        };

        turboBallAction = new UserAction("Turbo Ball") {
            @Override
            protected void onActionBegin() {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setTurboMode(true);
                    }
                }
            }

            @Override
            protected void onActionEnd() {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setTurboMode(false);
                    }
                }
            }
        };

        turboBallMouseAction = new UserAction("Turbo Ball Mouse") {
            @Override
            protected void onActionBegin() {
                if (mouseClicksBlocked) return;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setTurboMode(true);
                    }
                }
            }

            @Override
            protected void onActionEnd() {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setTurboMode(false);
                    }
                }
            }
        };

         destroyAllBricksAction = new UserAction("Debug Destroy Bricks") {
             @Override
             protected void onActionBegin() {
                 if (!canTriggerDebugHotkey()) {
                     return;
                 }
                 triggerDestroyAllBricksCheat();
             }
         };

         skipToLevel100Action = new UserAction("Debug Warp To Level 100") {
             @Override
             protected void onActionBegin() {
                 if (!canTriggerDebugHotkey()) {
                     return;
                 }
                 triggerWarpToLevel100Cheat();
             }
         };

        // Add all actions to the input system once with a dummy key
        // This registers the actions so they can be rebound later.
        FXGL.getInput().addAction(paddleLeftAction, KeyCode.F13);
        FXGL.getInput().addAction(paddleRightAction, KeyCode.F14);
        FXGL.getInput().addAction(launchBallAction, KeyCode.F15);
        FXGL.getInput().addAction(turboPaddleAction, KeyCode.F16);
        FXGL.getInput().addAction(plasmaWeaponAction, KeyCode.F17);
        FXGL.getInput().addAction(pauseAction, KeyCode.F18);
        FXGL.getInput().addAction(callBallAction, KeyCode.F19);
        FXGL.getInput().addAction(mouseLaunchBallAction, MouseButton.PRIMARY);
        FXGL.getInput().addAction(turboBallMouseAction, MouseButton.MIDDLE);
        FXGL.getInput().addAction(mousePlasmaWeaponAction, MouseButton.SECONDARY);
        FXGL.getInput().addAction(turboBallAction, KeyCode.F20);
        //FXGL.getInput().addAction(destroyAllBricksAction, KeyCode.P);
        //FXGL.getInput().addAction(skipToLevel100Action, KeyCode.L);
    }
    
    /**
     * Load key bindings from settings
     */
    private void loadInputBindings() {
        try {
            rebindAction("MOVE_LEFT", paddleLeftAction);
            rebindAction("MOVE_RIGHT", paddleRightAction);
            rebindAction("LAUNCH", launchBallAction);
            rebindAction("CALL_BALL", callBallAction);
            rebindAction("TURBO_PADDLE", turboPaddleAction);
            rebindAction("PLASMA_WEAPON", plasmaWeaponAction);
            rebindAction("PAUSE", pauseAction);
            rebindAction("TURBO_BALL", turboBallAction);

        } catch (Exception e) {
            System.err.println("Error loading/rebinding control settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void rebindAction(String actionName, UserAction action) {
        String key = saveManager.getControlKey(actionName);
        if (isValidKey(key)) {
            try {
                FXGL.getInput().rebind(action, KeyCode.valueOf(key));
            }
            catch (IllegalArgumentException e) {
                System.err.println("Failed to rebind action " + actionName + " to key " + key + ": " + e.getMessage());
            }
        } else {
            System.err.println("[Input] Invalid key for " + actionName + ": " + key);
        }
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isEmpty() && !key.equals("...");
    }

    private boolean canTriggerDebugHotkey() {
        return isGameplayState()
            && !isPaused
            && !isLoading
            && !isLevelIntroActive
            && !isStoryOverlayActive
            && !isCountdownActive
            && !isLevelCompleted
            && !isTransitioning
            && !isVictorySequenceActive
            && !isGameOver;
    }

     private void triggerDestroyAllBricksCheat() {
         destroyAllBricks();
     }

     private void triggerWarpToLevel100Cheat() {
         if (isDebugMode) {
             destroyAllBricks();
             return;
         }
         pendingLevelWarpTarget = 100;
         destroyAllBricks();
     }

    private void activateCallBall() {
        if (!isGameplayState()) {
            return;
        }

        if (bonusEffectManager == null || !bonusEffectManager.isCallBallBonusActive()) {
            return;
        }
        
        // КРИТИЧНО: Проверяем условия блокировки притягивания мяча
        boolean isBlocked = false;
        boolean bonusOverrides = bonusEffectManager != null && bonusEffectManager.isCallBallBonusActive();
        
        // Блокировка на хардкорной сложности
        if (!bonusOverrides && getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE) {
            isBlocked = true;
        }
        
        // Блокировка на boss уровнях (кроме легкой сложности)
        if (!bonusOverrides &&
            getEffectiveDifficulty() != com.arcadeblocks.config.DifficultyLevel.EASY && 
            com.arcadeblocks.config.LevelConfig.isBossLevel(FXGL.geti("level"))) {
            isBlocked = true;
        }
        
        callBallActiveSources++;
        
        if (isBlocked && audioManager != null && (saveManager == null || saveManager.isCallBallSoundEnabled())) {
            // Воспроизводим звук блокировки притягивания только если заблокировано
            audioManager.playSFXByName("call_to_paddle_block");
        }
    }

    private void deactivateCallBall() {
        if (callBallActiveSources > 0) {
            callBallActiveSources--;
        }
    }

    private boolean isCallBallActive() {
        return callBallActiveSources > 0;
    }

    private void resetCallBallState() {
        callBallActiveSources = 0;
    }
    
    /**
     * Reload key bindings (called when settings are changed)
     * @param overrideAction Optional action to rebind (e.g., "PLASMA_WEAPON")
     * @param overrideKey Optional new key for action (e.g., "X")
     */
    public void reloadInputBindings(String overrideAction, String overrideKey) {
        if (overrideAction != null && overrideKey != null) {
            // Force rebind specific action with new key
            try {
                switch (overrideAction) {
                    case "MOVE_LEFT":
                        rebindActionWithKey("MOVE_LEFT", paddleLeftAction, overrideKey);
                        break;
                    case "MOVE_RIGHT":
                        rebindActionWithKey("MOVE_RIGHT", paddleRightAction, overrideKey);
                        break;
                    case "LAUNCH":
                        rebindActionWithKey("LAUNCH", launchBallAction, overrideKey);
                        break;
                    case "CALL_BALL":
                        rebindActionWithKey("CALL_BALL", callBallAction, overrideKey);
                        break;
                    case "TURBO_PADDLE":
                        rebindActionWithKey("TURBO_PADDLE", turboPaddleAction, overrideKey);
                        break;
                    case "TURBO_BALL":
                        rebindActionWithKey("TURBO_BALL", turboBallAction, overrideKey);
                        break;
                    case "PLASMA_WEAPON":
                        rebindActionWithKey("PLASMA_WEAPON", plasmaWeaponAction, overrideKey);
                        break;
                    case "PAUSE":
                        rebindActionWithKey("PAUSE", pauseAction, overrideKey);
                        break;
                }
            } catch (Exception e) {
                System.err.println("An error occurred during forced re-binding " + overrideAction + " to the key " + overrideKey + ": " + e.getMessage());
            }
        }
        
        // Reload all other bindings
        loadInputBindings();

        // After reloading the bindings, we update the UI to display the correct keys.
        if (getScoreManager() != null && getScoreManager().getBonusIndicator() != null) {
            getScoreManager().getBonusIndicator().updatePlasmaWeaponKey();
        }
    }
    
    /**
     * Reload key bindings (called when settings are changed)
     */
    public void reloadInputBindings() {
        reloadInputBindings(null, null);
    }
    
    private void rebindActionWithKey(String actionName, UserAction action, String key) {
        if (isValidKey(key)) {
            try {
                FXGL.getInput().rebind(action, KeyCode.valueOf(key));
            }
            catch (IllegalArgumentException e) {
                System.err.println("Failed to rebind action " + actionName + " to key " + key + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Reset control settings to default values
     */
    private void resetControlsToDefault() {
        // System.out.println("Control settings are reset to default values.");
        for (java.util.Map.Entry<String, String> entry : com.arcadeblocks.config.GameConfig.DEFAULT_CONTROLS.entrySet()) {
            saveManager.setControlKey(entry.getKey(), entry.getValue());
        }
        saveManager.awaitPendingWrites(); // Wait for saving all default values
    }
    
    /**
     * Show control settings error dialog
     */
    private void showControlsResetDialog() {
        javafx.application.Platform.runLater(() -> {
            com.arcadeblocks.ui.ControlsResetDialog dialog = new com.arcadeblocks.ui.ControlsResetDialog();
            FXGL.getGameScene().addUINode(dialog);
        });
    }
    
    /**
     * Update paddle speed (called when settings are changed)
     */
    public void updatePaddleSpeed() {
        double newSpeed = saveManager.getPaddleSpeed();
        
        // Update speed of all paddles in game
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        for (Entity paddleEntity : paddles) {
            Paddle paddleComponent = paddleEntity.getComponent(Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setSpeed(newSpeed);
            }
        }
        
        // Update global variable for new paddles
        if (paddleComponent != null) {
            paddleComponent.setSpeed(newSpeed);
        }
    }
    
    // Removed mouse control to restore ball stickiness
    
    // Removed cursor control
    
    // Removed cursor confinement
    
    /**
     * Force set system cursor instead of blue FXGL cursor
     */
    private void setSystemCursor() {
        cursorHidden = false;
        applyCursorStateAndEnforcer();
    }

    /**
     * Hide cursor during gameplay.
     */
    private void setHiddenCursor() {
        cursorHidden = true;
        applyCursorStateAndEnforcer();
    }

    private void applyCursorStateAndEnforcer() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene != null) {
                var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                scene.setCursor(targetCursor);
                scene.getRoot().setCursor(targetCursor);
            }
        });
        
        // Restart unified enforcer timer so cursor won't be changed by FXGL
        if (cursorEnforcer != null) {
            cursorEnforcer.stop();
        }
        cursorEnforcer = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.1), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.5), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
            }),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
            })
        );
        cursorEnforcer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cursorEnforcer.play();
    }

    private void enableCursorConfine(boolean enable) {
        // On Wayland cursor confinement is unavailable - disable
        if (enable && isLinuxWayland()) {
            confineCursorEnabled = false;
            return;
        }
        confineCursorEnabled = enable;
        if (enable && awtRobot == null) {
            try {
                awtRobot = new java.awt.Robot();
            } catch (Exception ignored) {}
        }
        if (enable && awtRobot == null && fxRobot == null) {
            try {
                fxRobot = new Robot();
            } catch (Exception ignored) {}
        }
        if (enable) {
            installGlobalMouseConfineListener();
        } else {
            uninstallGlobalMouseConfineListener();
        }
        restartConfineTicker();
        if (enable) {
            ensureConfineFocusListener();
        }
    }

    private void restartConfineTicker() {
        stopConfineTicker();
        if (!confineCursorEnabled || !isStageReadyForConfine()) {
            return;
        }
        if (awtRobot == null && fxRobot == null) {
            return;
        }
        installGlobalMouseConfineListener();
        confineTicker = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(16), e -> confineMouseToWindow())
        );
        confineTicker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        confineTicker.play();
        // Instantly return cursor to window if it already exited
        confineMouseToWindow();
    }

    private void stopConfineTicker() {
        if (confineTicker != null) {
            confineTicker.stop();
            confineTicker = null;
        }
    }

    private void installGlobalMouseConfineListener() {
        if (globalMouseConfineListener != null) {
            return;
        }
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            globalMouseConfineListener = event -> {
                if (!confineCursorEnabled || awtRobot == null) {
                    return;
                }
                if (!(event instanceof java.awt.event.MouseEvent mouseEvent)) {
                    return;
                }
                int id = mouseEvent.getID();
                if (id != java.awt.event.MouseEvent.MOUSE_MOVED &&
                    id != java.awt.event.MouseEvent.MOUSE_DRAGGED &&
                    id != java.awt.event.MouseEvent.MOUSE_EXITED &&
                    id != java.awt.event.MouseEvent.MOUSE_ENTERED) {
                    return;
                }
                if (!isStageReadyForConfine()) {
                    return;
                }
                if (confineRequestScheduled.compareAndSet(false, true)) {
                    javafx.application.Platform.runLater(() -> {
                        confineRequestScheduled.set(false);
                        if (confineCursorEnabled && isStageReadyForConfine()) {
                            confineMouseToWindow();
                        }
                    });
                }
            };
            long mask = AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK;
            toolkit.addAWTEventListener(globalMouseConfineListener, mask);
        } catch (Exception e) {
            globalMouseConfineListener = null;
        }
    }

    private void uninstallGlobalMouseConfineListener() {
        if (globalMouseConfineListener == null) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseConfineListener);
        } catch (Exception ignored) {}
        globalMouseConfineListener = null;
        confineRequestScheduled.set(false);
    }

    private void ensureConfineFocusListener() {
        if (confineFocusListenerInstalled) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            if (confineFocusListenerInstalled) {
                return;
            }
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage == null) {
                return;
            }
            stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    suppressLevelCompletionChecks = true;
                    FXGL.runOnce(() -> suppressLevelCompletionChecks = false, Duration.millis(150));
                    if (confineCursorEnabled) {
                        restartConfineTicker();
                    }
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                } else {
                    suppressLevelCompletionChecks = true;
                    if (isPaused) {
                        pauseResumeLockFromFocusLoss = true;
                    }
                    stopConfineTicker();
                    uninstallGlobalMouseConfineListener();
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                }
            });
            stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
                if (!isIconified) {
                    suppressLevelCompletionChecks = true;
                    FXGL.runOnce(() -> suppressLevelCompletionChecks = false, Duration.millis(150));
                    if (confineCursorEnabled) {
                        restartConfineTicker();
                    }
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                } else {
                    suppressLevelCompletionChecks = true;
                    if (isPaused && isIconified) {
                        pauseResumeLockFromFocusLoss = true;
                    }
                    stopConfineTicker();
                    uninstallGlobalMouseConfineListener();
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                }
            });
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && confineCursorEnabled) {
                    restartConfineTicker();
                } else {
                    stopConfineTicker();
                    uninstallGlobalMouseConfineListener();
                }
            });
            confineFocusListenerInstalled = true;
        });
    }

    private boolean isStageReadyForConfine() {
        javafx.stage.Stage stage = FXGL.getPrimaryStage();
        return stage != null && stage.isShowing() && stage.isFocused() && !stage.isIconified();
    }
    private void confineMouseToWindow() {
        javafx.stage.Stage stage = FXGL.getPrimaryStage();
        if (stage == null || stage.getScene() == null) return;
        if (!isStageReadyForConfine()) return;
        javafx.scene.Scene scene = stage.getScene();
        // Get cursor coordinates in screen coordinates
        double cursorScreenX;
        double cursorScreenY;
        java.awt.PointerInfo info = null;
        try {
            info = java.awt.MouseInfo.getPointerInfo();
        } catch (Throwable ignored) {}
        if (info != null) {
            java.awt.Point p = info.getLocation();
            cursorScreenX = p.getX();
            cursorScreenY = p.getY();
        } else if (fxRobot != null) {
            cursorScreenX = fxRobot.getMouseX();
            cursorScreenY = fxRobot.getMouseY();
        } else {
            return;
        }

        // Screen bounds of scene content (without window borders), accounting for HiDPI
        javafx.geometry.Bounds screenBounds = null;
        try {
            screenBounds = scene.getRoot().localToScreen(scene.getRoot().getBoundsInLocal());
        } catch (Exception ignored) {}
        if (screenBounds == null) {
            javafx.stage.Window window = scene.getWindow();
            if (window == null) {
                return;
            }
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            if (sceneWidth <= 0 || sceneHeight <= 0) {
                return;
            }
            double offsetX = window.getX() + scene.getX();
            double offsetY = window.getY() + scene.getY();
            screenBounds = new javafx.geometry.BoundingBox(offsetX, offsetY, sceneWidth, sceneHeight);
        }

        // Safe padding from content edges to avoid window title/borders
        double minX = screenBounds.getMinX() + 4;
        double minY = screenBounds.getMinY() + 10; // increase top padding
        double maxX = screenBounds.getMaxX() - 4;
        double maxY = screenBounds.getMaxY() - 6;

        double clampedX = Math.max(minX, Math.min(maxX, cursorScreenX));
        double clampedY = Math.max(minY, Math.min(maxY, cursorScreenY));

        if (Math.abs(cursorScreenX - clampedX) > 0.01 || Math.abs(cursorScreenY - clampedY) > 0.01) {
            isWarpingMouse = true;
            try {
                if (awtRobot != null) {
                    awtRobot.mouseMove((int) Math.round(clampedX), (int) Math.round(clampedY));
                } else if (fxRobot != null) {
                    fxRobot.mouseMove(clampedX, clampedY);
                }
                try {
                    // Return focus to scene after pointer warp
                    scene.getRoot().requestFocus();
                } catch (Exception ignored) {}
            } finally {
                // Short delay to avoid immediate recursive call
                javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(5));
                pt.setOnFinished(e -> isWarpingMouse = false);
                pt.play();
            }
        }

        // Update paddle position even when pointer exits to title area, using screen -> scene coordinate transformation
        if (!isWarpingMouse && !isPaused && !isCountdownActive && paddleComponent != null && !isLevelCompleted && !isTransitioning) {
            try {
                // Use actual X coordinate (clamped), Y taken from inside scene
                double effScreenX = Math.max(minX, Math.min(maxX, cursorScreenX));
                double effScreenY = Math.max(minY, Math.min(maxY, cursorScreenY));
                javafx.geometry.Point2D local = scene.getRoot().screenToLocal(effScreenX, effScreenY);
                if (local != null) {
                    double sceneX = local.getX();
                    // IMPORTANT: don't call handleMouseMove() here to avoid re-entrancy
                    movePaddleForSceneX(sceneX);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean isLinuxWayland() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) return false;
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType != null && sessionType.equalsIgnoreCase("wayland")) return true;
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        return waylandDisplay != null && !waylandDisplay.isEmpty();
    }

    /**
     * Install mouse handlers for paddle control during gameplay.
     */
    public void installMousePaddleControlHandlers() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene == null) return;
            // Remove old filters if they were bound to another scene
            if (mouseHandlersInstalled) {
                try {
                    if (mouseMoveHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
                    }
                    if (mouseDragHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
                    }
                } catch (Exception ignored) {}
                mouseHandlersInstalled = false;
            }
            // Create and attach new handlers to current scene
            mouseMoveHandler = e -> handleMouseMove(e.getSceneX());
            mouseDragHandler = e -> handleMouseMove(e.getSceneX());
            mouseExitHandler = e -> {
                if (confineCursorEnabled) {
                    confineMouseToWindow();
                }
            };
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, mouseExitHandler);
            mouseHandlersInstalled = true;
            enableCursorConfine(true);
            // Ensure window focus for correct event delivery
            try {
                scene.getRoot().requestFocus();
            } catch (Exception ignored) {}
            enableMouseFollowTicker(true);
        });
    }

    /**
     * Remove mouse paddle control handlers (for menu/pause).
     */
    public void uninstallMousePaddleControlHandlers() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene != null) {
                try {
                    if (mouseMoveHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
                    }
                    if (mouseDragHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
                    }
                    if (mouseExitHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, mouseExitHandler);
                    }
                    mouseExitHandler = null;
                } catch (Exception ignored) {}
            }
            mouseHandlersInstalled = false;
            enableCursorConfine(false);
            enableMouseFollowTicker(false);
        });
    }

    /**
     * Paddle movement logic based on mouse position.
     */
    private void handleMouseMove(double mouseSceneX) {
        // Active only during gameplay: paddle exists, not paused, not Game Over screen, not transitioning
        if (paddleComponent == null || isPaused || isCountdownActive || isTransitioning || isLevelCompleted) return;
        boolean gameOverVisible = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> node instanceof com.arcadeblocks.ui.GameOverView);
        if (gameOverVisible) return;
        // Check if paddle movement is blocked (e.g., frozen paddle bonus)
        if (paddleComponent.isMovementBlocked()) return;
        // Keyboard priority: if left/right keys are pressed - ignore mouse
        if (isKeyboardDriving()) return;
        
        if (isWarpingMouse) return; // avoid recursion during forced movement
        // If keyboard was just used - ignore first mouse movement to avoid jerk
        if (suppressMouseUntilMove) {
            suppressMouseUntilMove = false;
            return;
        }

        if (pauseResumeLockFromFocusLoss) {
            pauseResumeLockFromFocusLoss = false;
            return;
        }

        movePaddleForSceneX(mouseSceneX);
    }

    private boolean canProcessLaunchInput() {
        if (isLoading || isPaused || isCountdownActive || isTransitioning || isLevelCompleted) {
            return false;
        }
        if (mouseClicksBlocked) {
            return false;
        }
        boolean gameOverVisible = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> node instanceof com.arcadeblocks.ui.GameOverView);
        if (gameOverVisible) {
            return false;
        }
        return true;
    }

    public boolean consumePauseResumeLock() {
        if (pauseResumeLockFromFocusLoss) {
            pauseResumeLockFromFocusLoss = false;
            return true;
        }
        return false;
    }

    private void movePaddleForSceneX(double sceneX) {
        // Legacy behavior: mouse respects countdown/movement blocks and yields to keyboard input
        if (isCountdownActive || paddleComponent == null || paddleComponent.isMovementBlocked()) {
            return;
        }
        if (suppressMouseUntilMove) {
            return;
        }
        if (isKeyboardDriving()) {
            return;
        }

        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null) return;

        double halfWidth = paddleEntity.getWidth() / 2.0;
        double desiredX = sceneX - halfWidth;
        double minX = 1;
        double maxX = GameConfig.GAME_WIDTH - paddleEntity.getWidth() - 1;
        if (desiredX < minX) desiredX = minX;
        if (desiredX > maxX) desiredX = maxX;

        paddleEntity.setX(desiredX);
        try {
            var physics = paddleEntity.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
            if (physics != null) {
                physics.overwritePosition(new javafx.geometry.Point2D(desiredX, paddleEntity.getY()));
                physics.setLinearVelocity(0, 0);
            }
        } catch (Exception ignored) {}

        // NOTE: do not reset keyboard flags here so keyboard control stays authoritative until released
    }

    /**
     * Returns true if control is currently being performed by keyboard.
     */
    private boolean isKeyboardDriving() {
        return leftPressed || rightPressed || turboPressed;
    }
    
    /**
     * Complete loading
     */
    private void completeLoading(com.arcadeblocks.ui.LoadingView loadingView) {
        // Check that we are still in loading process
        if (!isLoading) {
            return; // If loading is already completed, do nothing
        }
        
        // Complete loading
        isLoading = false;
        
        // Don't pass loadingView to startMainMenuMusic to avoid memory leaks
        startMainMenuMusic();
        
        // Transition to main menu without greeting sound
        transitionToMainMenu(loadingView);
    }
    

    
    /**
     * Transition to main menu
     */
    private void transitionToMainMenu(com.arcadeblocks.ui.LoadingView loadingView) {
        // Clean LoadingView before removing from scene
        // This ensures stopping all animations and removing all listeners
        if (loadingView != null) {
            removeUINodeSafely(loadingView);
        }

        dismissActiveChapterStoryView();
        disableDarknessOverlay();
        
        // Show main menu (background should change when transitioning from loading)
        MainMenuView mainMenuView = new MainMenuView(this, true);
        FXGL.getGameScene().addUINode(mainMenuView);
        
        // Remove mouse handlers when entering menu
        uninstallMousePaddleControlHandlers();
        // Unblock mouse clicks in menu
        unblockMouseClicks();
        // Set system cursor in menu
        setSystemCursor();
        
        // Ensure MainMenuView received focus for keyboard navigation
        javafx.application.Platform.runLater(() -> {
            mainMenuView.restoreFocus();
        });
        
        // Music already started in startMainMenuMusic()
        // System.out.println("Transition to main menu (music already playing)");
    }
    
    
    private void showExitConfirmation() {
        com.arcadeblocks.ui.ExitConfirmView.show(
            // Action on exit
            () -> {
                // Return to main menu
                paddleComponent = null;
                // CRITICAL: Clean LivesManager before nullifying
                if (livesManager != null) {
                    livesManager.cleanup();
                }
                livesManager = null;
                scoreManager = null;
                bonusEffectManager = null;
                leftPressed = false;
                rightPressed = false;
                turboPressed = false;
                resetCallBallState();
                
                // Safe removal of all entities (copy list)
                var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
                entities.forEach(this::removeEntitySafely);
                
                // Cursor remains standard system cursor
                
                returnToMainMenu();
            },
            // Action on cancel - resume game
            () -> {
                // Resume gameplay
                FXGL.getGameController().resumeEngine();
            }
        );
    }
    
    /**
     * Window close handler (X button)
     */
    private void handleWindowClose() {
        // System.out.println("handleWindowClose called! paddleComponent = " + paddleComponent); // Debug

        if (isLoading || isLevelIntroActive || isStoryOverlayActive || isCountdownActive || isLevelCompleted || isTransitioning) {
            return;
        }
        
        // Check if we are in gameplay
        if (paddleComponent != null) {
        // System.out.println("Show pause screen on window close during gameplay"); // Debug
            // During gameplay - show pause screen instead of exit dialog
            if (!isPaused) {
                pauseGame();
            }
            // If already paused, do nothing - pause screen is already shown
        } else {
        // System.out.println("Show game exit dialog"); // Debug
            // In main menu - show game exit dialog
            showGameExitConfirmation();
        }
    }
    
    /**
     * Show game exit dialog (for main menu)
     */
    private void showGameExitConfirmation() {
        com.arcadeblocks.ui.GameExitConfirmView.show(
            // Action on exit
            () -> {
                exitGame();
            },
            // Action on cancel (do nothing, just close dialog)
            () -> {
                // Dialog will close automatically
            }
        );
    }
    
    private long lastFpsLogTime = 0;
    private int frameCount = 0;
    private long lastFrameTime = 0;
    
    @Override
    protected void onUpdate(double tpf) {
        long startTime = System.nanoTime();
        
        // Детектируем микрофризы (если кадр занял больше 33мс при 60 FPS)
        if (lastFrameTime > 0) {
            long frameDuration = startTime - lastFrameTime;
            if (frameDuration > 33_000_000) { // Больше 33мс = микрофриз
                // System.out.println(String.format("[Performance] MICROFREEZE detected! Frame took %.2fms", frameDuration / 1_000_000.0));
            }
        }
        lastFrameTime = startTime;
        
        // Логируем FPS каждую секунду
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsLogTime >= 1000) {
            // Получаем информацию о памяти
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            // System.out.println(String.format("[Performance] FPS: %d, TPF: %.2fms, Memory: %dMB/%dMB", 
            //     frameCount, tpf * 1000, usedMemory, maxMemory));
            frameCount = 0;
            lastFpsLogTime = currentTime;
        }
        
        super.onUpdate(tpf);
        if (!vsyncConfigured) {
            applyVSyncInternal(vsyncEnabled);
        }
        long superTime = System.nanoTime() - startTime;
        
        // КРИТИЧНО: Периодическая очистка ресурсов для предотвращения лагов при долгой игре
        if (isGameplayState()) {
            gameplayTimeAccumulator += tpf;
            if (gameplayTimeAccumulator >= PERIODIC_CLEANUP_INTERVAL) {
                performPeriodicGameplayCleanup();
                gameplayTimeAccumulator = 0.0;
            }
        } else {
            // Сбрасываем аккумулятор когда не в игровом состоянии
            gameplayTimeAccumulator = 0.0;
        }
        
        if (audioManager != null) {
            audioManager.update(tpf);
        }
        if (scoreManager != null) {
            scoreManager.update(tpf);
        }
        if (bonusEffectManager != null) {
            bonusEffectManager.update(tpf);
        }
        if (isLevelCompleted) {
            if (!levelCompletedMessageShown) {
                levelCompletedMessageShown = true;
            }
            return;
        } else {
            levelCompletedMessageShown = false;
        }
        if (paddleComponent != null) {
            // ДИАГНОСТИКА: выводим состояние флагов перед установкой
            // if (leftPressed || rightPressed) {
            //     System.out.println("[App.onUpdate] leftPressed=" + leftPressed + ", rightPressed=" + rightPressed + ", isGameOverViewVisible=" + isGameOverViewVisible);
            // }
            
            // Use flag instead of stream check to eliminate micro-freezes
            if (!isGameOverViewVisible) {
                paddleComponent.setMoveLeft(leftPressed);
                paddleComponent.setMoveRight(rightPressed);
                paddleComponent.setTurboMode(turboPressed);
                if (leftPressed || rightPressed || turboPressed) {
                    suppressMouseUntilMove = true;
                }
            } else {
                paddleComponent.setMoveLeft(false);
                paddleComponent.setMoveRight(false);
                paddleComponent.setTurboMode(false);
                paddleComponent.setMovementBlocked(true);
            }
        } else {
            // ДИАГНОСТИКА: paddleComponent is null
            // if (leftPressed || rightPressed) {
            //     System.out.println("[App] WARNING: paddleComponent is NULL but input detected!");
            // }
        }

        long beforeCallBall = System.nanoTime();
        applyCallBallAttraction(tpf);
        long callBallTime = System.nanoTime() - beforeCallBall;
        
        long totalTime = System.nanoTime() - startTime;
        
        // Логируем только если onUpdate занял больше 2мс (заметная задержка при 60 FPS)
        // if (totalTime > 2_000_000) {
        //     System.out.println(String.format("[Performance] onUpdate took %.2fms (super: %.2fms, callBall: %.2fms)", 
        //         totalTime / 1_000_000.0, superTime / 1_000_000.0, callBallTime / 1_000_000.0));
        // }
    }

    private void applyCallBallAttraction(double tpf) {
        if (bonusEffectManager == null || !bonusEffectManager.isCallBallBonusActive()) {
            return;
        }
        if (!isCallBallActive() || paddleComponent == null) {
            return;
        }
        if (!isGameplayState()) {
            return;
        }
        
        // КРИТИЧНО: Отключаем притяжение мячей на хардкорной сложности
        // Проверяем один раз здесь вместо проверки для каждого мяча - оптимизация
        if (getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE) {
            return;
        }
        
        // Блокировка на boss уровнях (кроме легкой сложности)
        if (getEffectiveDifficulty() != com.arcadeblocks.config.DifficultyLevel.EASY && 
            com.arcadeblocks.config.LevelConfig.isBossLevel(FXGL.geti("level"))) {
            return;
        }

        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null || !paddleEntity.isActive()) {
            return;
        }

        Point2D paddleCenter = paddleEntity.getCenter();
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ballEntity : balls) {
            if (ballEntity == null || !ballEntity.isActive()) {
                continue;
            }
            Ball ballComponent;
            try {
                ballComponent = ballEntity.getComponent(Ball.class);
            } catch (Exception ex) {
                continue;
            }
            if (ballComponent != null) {
                ballComponent.applyPaddleAttraction(paddleCenter, tpf);
            }
        }
    }

    /**
     * Периодическая очистка ресурсов во время долгой игры для предотвращения лагов.
     * Вызывается каждые 5 минут игрового времени.
     */
    private void performPeriodicGameplayCleanup() {
        try {
            // КРИТИЧНО: Принудительно запускаем сборку мусора для освобождения памяти
            // Это помогает предотвратить накопление мусора во время долгой игры
            System.gc();
            
            // КРИТИЧНО: Очищаем неактивные анимации и переходы
            synchronized (activeFadeTransitions) {
                activeFadeTransitions.removeIf(transition -> {
                    if (transition.getStatus() == Animation.Status.STOPPED) {
                        try {
                            transition.stop();
                        } catch (Exception ignored) {
                        }
                        return true;
                    }
                    return false;
                });
            }
            
            // КРИТИЧНО: Очищаем неактивные видео переходы
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.removeIf(transition -> {
                    if (transition.getStatus() == Animation.Status.STOPPED) {
                        try {
                            transition.stop();
                        } catch (Exception ignored) {
                        }
                        return true;
                    }
                    return false;
                });
            }
            
            // КРИТИЧНО: Очищаем неактивные сущности из игрового мира
            var gameWorld = FXGL.getGameWorld();
            if (gameWorld != null) {
                var entities = gameWorld.getEntities();
                int beforeCount = entities.size();
                
                // Удаляем неактивные сущности
                entities.removeIf(entity -> !entity.isActive());
                
                int afterCount = gameWorld.getEntities().size();
                // if (beforeCount != afterCount) {
                //     System.out.println("Cleaned up " + (beforeCount - afterCount) + " inactive entities");
                // }
            }
            
            // КРИТИЧНО: Очищаем неактивные UI ноды (включая orphaned SettingsView)
            try {
                clearInactiveUINodes();
            } catch (Exception e) {
                System.err.println("Error during UI cleanup in periodic cleanup: " + e.getMessage());
            }
            
            // System.out.println("Periodic gameplay cleanup performed after " + PERIODIC_CLEANUP_INTERVAL + " seconds");
            
        } catch (Exception e) {
            System.err.println("Error during periodic gameplay cleanup: " + e.getMessage());
        }
    }

    /**
     * Очищает неактивные UI ноды для предотвращения утечек памяти.
     * Используется при переходах между экранами настроек и паузы.
     */
    private void clearInactiveUINodes() {
        try {
            var scene = FXGL.getGameScene();
            var uiNodes = new ArrayList<Node>(scene.getUINodes());
            
            for (Node node : uiNodes) {
                // Проверяем, является ли нод неактивным или "мертвым"
                boolean shouldRemove = false;
                
                // Стандартные проверки неактивности
                if (!node.isVisible() || node.getOpacity() == 0.0 || node.getParent() == null) {
                    shouldRemove = true;
                }
                
                // КРИТИЧНО: Специальная проверка для SettingsView и связанных диалогов
                // Если мы не в режиме настроек паузы, но эти элементы все еще в сцене - удаляем их
                if (!inPauseSettings && (node instanceof SettingsView 
                    || node instanceof com.arcadeblocks.ui.ResetSettingsDialog
                    || node instanceof com.arcadeblocks.ui.ControlsResetDialog
                    || node instanceof com.arcadeblocks.ui.UnsavedChangesDialog)) {
                    shouldRemove = true;
                    // System.out.println("Force removing orphaned settings UI: " + node.getClass().getSimpleName());
                }
                
                if (shouldRemove) {
                    // Пытаемся очистить через UINodeCleanup
                    try {
                        UINodeCleanup.cleanupNode(node);
                        scene.removeUINode(node);
                    } catch (Exception e) {
                        System.err.println("Error cleaning inactive UI node: " + e.getMessage());
                    }
                }
            }
            
            // КРИТИЧНО: НЕ вызываем System.gc() здесь, так как это блокирующая операция
            // которая вызывает заметные лаги в UI потоке.
            // Сборка мусора произойдет автоматически, когда это будет необходимо.
            
        } catch (Exception e) {
            System.err.println("Error in clearInactiveUINodes: " + e.getMessage());
        }
    }
    
    private void performShutdownIfNeeded() {
        if (shutdownTriggered.compareAndSet(false, true)) {
            shutdownInternal();
        }
    }

    private void shutdownInternal() {
        // Stop all active video resources FIRST
        cleanupActiveVideoResources();
        
        // Clean all UI components
        cleanupAllUINodes();
        
        // Clean gameplay state before cleaning managers
        cleanupGameplayState();
        
        // Save settings on exit
        if (saveManager != null) {
            saveManager.saveSettings();
            try {
                saveManager.close();
            } catch (Exception ignored) {
            }
            saveManager = null;
        }
        
        // Stop all animations and timers
        cleanupAllTimelines();
        
        // Remove all event listeners
        cleanupAllEventListeners();
        
        // Stop and clean audio
        if (audioManager != null) {
            audioManager.stopAll();
            audioManager.cleanup();
            audioManager = null;
        }
        
        // Clean VLC resources
        try {
            com.arcadeblocks.video.VlcContext.getInstance().dispose();
        } catch (Exception e) {
            System.err.println("Error cleaning VLC: " + e.getMessage());
        }

        try {
            FXGL.getAssetLoader().clearCache();
        } catch (Exception ignored) {
        }

        ImageCache.clear();
        
        try {
            com.arcadeblocks.util.TextureUtils.clearGameplayTextureCache();
        } catch (Exception ignored) {
        }
        
        // Clean all manager references
        if (scoreManager != null) {
            try {
                scoreManager.stopLevelTimer();
                scoreManager.hideBossHealth();
            } catch (Exception ignored) {
            }
            scoreManager = null;
        }
        bonusEffectManager = null;
        // Clean LivesManager before nullifying
        if (livesManager != null) {
            livesManager.cleanup();
        }
        livesManager = null;
        paddleComponent = null;
        // Clean BonusTimerView before nullifying
        if (bonusTimerView != null) {
            bonusTimerView.cleanup();
        }
        bonusTimerView = null;
        gameplayUIView = null;
        
        // Clean all UI component references
        try {
            var scene = FXGL.getGameScene();
            if (scene != null) {
                // Remove all remaining UI nodes
                var remainingNodes = new java.util.ArrayList<>(scene.getUINodes());
                for (var node : remainingNodes) {
                    try {
                        scene.removeUINode(node);
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        // Clean all background image references
        try {
            levelBackgroundNodes.clear();
        } catch (Exception ignored) {
        }
        
        // Clean all darkness overlay references
        darknessOverlayCapsule = null;
        darknessOverlayGroup = null;
        darknessOverlayDimLayer = null;
        darknessOverlayActive = false;
    }
    
    /**
     * Shutdown all ExecutorService for complete process termination
     */
    private void shutdownAllExecutors() {
        // Shutdown ExecutorService in AudioManager
        if (audioManager != null && audioManager instanceof com.arcadeblocks.audio.SDL2AudioManager) {
            com.arcadeblocks.audio.SDL2AudioManager sdlAudioManager = (com.arcadeblocks.audio.SDL2AudioManager) audioManager;
            try {
                java.util.concurrent.ExecutorService executor = sdlAudioManager.getAsyncExecutor();
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                    try {
                        if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                            executor.shutdownNow();
                            if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                                System.err.println("AudioManager executor did not terminate within 2 seconds");
                            }
                        }
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                // Ignore errors during executor termination
            }
        }
        
        // SaveManager already closes in shutdownInternal(), but ensure executor is terminated
        if (saveManager != null) {
            try {
                // SaveManager.close() already calls shutdown on dbExecutor
                // No additional check required
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }
    
    /**
     * Safe entity removal with resource cleanup to prevent memory leaks
     * Cleans cached textures, physical fixtures and custom components before removal.
     * Centralizes cleanup behavior and protects from exceptions during cleanup/removal.
     */
    private void removeEntitySafely(Entity entity) {
        if (entity == null || !entity.isActive()) {
            return;
        }
        
        try {
            // Clean entity components before removal
            // Clean ViewComponent to release textures
            if (entity.getViewComponent() != null) {
                var viewChildren = entity.getViewComponent().getChildren();
                if (viewChildren != null) {
                    for (var node : new java.util.ArrayList<>(viewChildren)) {
                        try {
                            if (node instanceof javafx.scene.image.ImageView imageView) {
                                var image = imageView.getImage();
                                if (image != null) {
                                    ImageCache.forget(image);
                                }
                                imageView.setImage(null);
                            }
                        } catch (Exception e) {
                            // Ignore errors when cleaning individual nodes
                        }
                    }
                }
            }
            
            // Clean PhysicsComponent to release fixtures
            try {
                var physics = entity.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                if (physics != null) {
                    // Physics fixtures will be automatically cleaned when entity is removed
                    // But we can explicitly stop physics
                    physics.setLinearVelocity(0, 0);
                }
            } catch (Exception e) {
                // Ignore errors when component not found or cleaning physics
            }
            
            // Clean custom components that may have resources
            // Ball, Brick, Bonus, Paddle etc. may have cached resources
            try {
                var ballComponent = entity.getComponent(com.arcadeblocks.gameplay.Ball.class);
                if (ballComponent != null) {
                    // Ball component may have cached textures
                    // They will be cleaned when entity is removed
                }
            } catch (Exception e) {
                // Ignore errors when component not found
            }
            
            try {
                var brickComponent = entity.getComponent(com.arcadeblocks.gameplay.Brick.class);
                if (brickComponent != null) {
                    // Brick component may have cached textures
                    // They will be cleaned when entity is removed
                }
            } catch (Exception e) {
                // Ignore errors when component not found
            }
            
            try {
                var bonusComponent = entity.getComponent(com.arcadeblocks.gameplay.Bonus.class);
                if (bonusComponent != null) {
                    // Bonus component may have cached textures
                    // They will be cleaned when entity is removed
                }
            } catch (Exception e) {
                // Ignore errors when component not found
            }
            
        } catch (Exception e) {
            // Log error but continue removal (suppress expected component not found errors)
            // System.err.println("Error cleaning entity resources before removal: " + e.getMessage());
        }
        
        // Remove entity from world
        try {
            entity.removeFromWorld();
        } catch (Exception e) {
            // Log error but don't interrupt removal flow
            System.err.println("Error removing entity from world: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup all UI components for complete process termination
     */
    private void cleanupAllUINodes() {
        try {
            var scene = FXGL.getGameScene();
            if (scene != null) {
                // Clean all UI nodes with cleanup() call if they support it
                var uiNodes = new java.util.ArrayList<>(scene.getUINodes());
                for (var node : uiNodes) {
                    try {
                        // First try via SupportsCleanup interface
                        if (node instanceof com.arcadeblocks.ui.SupportsCleanup) {
                            ((com.arcadeblocks.ui.SupportsCleanup) node).cleanup();
                        } else {
                            // Fallback: try via reflection for legacy widgets
                            try {
                                java.lang.reflect.Method cleanupMethod = node.getClass().getMethod("cleanup");
                                if (cleanupMethod != null) {
                                    cleanupMethod.invoke(node);
                                }
                            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
                                // cleanup() method not found or unavailable - this is normal
                            }
                        }
                        removeUINodeSafely(node);
                    } catch (Exception e) {
                        // Ignore errors when cleaning individual nodes
                    }
                }
            }
            
            // Clean all entities from game world
            try {
                var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
                for (var entity : entities) {
                    try {
                        removeEntitySafely(entity);
                    } catch (Exception e) {
                        // Ignore errors when removing entities
                    }
                }
            } catch (Exception e) {
                // Ignore errors when cleaning game world
            }
        } catch (Exception e) {
            // Ignore errors when cleaning UI
        }
    }
    
    /**
     * Stop all Timeline animations to prevent memory leaks
     */
    private void cleanupAllTimelines() {
        // Stop cursorEnforcer
        if (cursorEnforcer != null) {
            cursorEnforcer.stop();
            cursorEnforcer = null;
        }
        
        confineCursorEnabled = false;
        // Stop confineTicker
        if (confineTicker != null) {
            confineTicker.stop();
            confineTicker = null;
        }
        
        // Stop mouseFollowTicker
        if (mouseFollowTicker != null) {
            mouseFollowTicker.stop();
            mouseFollowTicker = null;
        }
        
        uninstallGlobalMouseConfineListener();
        awtRobot = null;
        fxRobot = null;

        // Stop all VLC players to prevent memory leaks
        try {
            com.arcadeblocks.video.VlcContext.getInstance().dispose();
        } catch (Exception e) {
            System.err.println("Error cleaning up VLC resources: " + e.getMessage());
        }
    }
    
    /**
     * Remove all event listeners to prevent memory leaks
     */
    private void cleanupAllEventListeners() {
        javafx.application.Platform.runLater(() -> {
            // Cleanup GameOverView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.GameOverView)
                .forEach(node -> {
                    com.arcadeblocks.ui.GameOverView gameOverView = (com.arcadeblocks.ui.GameOverView) node;
                    gameOverView.cleanup();
                });
            // Reset GameOverView flag after cleanup
            isGameOverViewVisible = false;
            
            // Cleanup LevelIntroView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.LevelIntroView)
                .forEach(node -> {
                    com.arcadeblocks.ui.LevelIntroView levelIntroView = (com.arcadeblocks.ui.LevelIntroView) node;
                    levelIntroView.cleanup();
                });
            
            // Cleanup LoadingView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.LoadingView)
                .forEach(node -> {
                    com.arcadeblocks.ui.LoadingView loadingView = (com.arcadeblocks.ui.LoadingView) node;
                    loadingView.cleanup();
                });
            
            // Cleanup CountdownTimerView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.CountdownTimerView)
                .forEach(node -> {
                    com.arcadeblocks.ui.CountdownTimerView countdownView = (com.arcadeblocks.ui.CountdownTimerView) node;
                    countdownView.cleanup();
                });
            
            // Cleanup PoemView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.PoemView)
                .forEach(node -> {
                    com.arcadeblocks.ui.PoemView poemView = (com.arcadeblocks.ui.PoemView) node;
                    poemView.cleanup();
                });
            
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene != null) {
                try {
                    if (mouseMoveHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
                        mouseMoveHandler = null;
                    }
                    if (mouseDragHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
                        mouseDragHandler = null;
                    }
                    if (mouseExitHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, mouseExitHandler);
                        mouseExitHandler = null;
                    }
                } catch (Exception ignored) {}
            }
            mouseHandlersInstalled = false;
        });
        
        // Disable all tickers
        stopConfineTicker();
        enableMouseFollowTicker(false);
    }
    
    /**
     * Get audio manager
     */
    public SDL2AudioManager getAudioManager() {
        return audioManager;
    }
    
    
    public String getCurrentLevelLoadingSound() {
        return currentLevelLoadingSound != null
            ? currentLevelLoadingSound
            : AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
    }
    
    public String getCurrentLevelCompleteSound() {
        int currentLevel = FXGL.geti("level");
        if (com.arcadeblocks.config.LevelConfig.isBossLevel(currentLevel)) {
            return null; // Для боссов не проигрываем стандартный звук завершения
        }
        if (currentLevelCompletionSounds == null || currentLevelCompletionSounds.isEmpty()) {
            return AudioConfig.DEFAULT_LEVEL_COMPLETE_SOUND;
        }
        if (currentLevelCompletionSounds.size() == 1) {
            return currentLevelCompletionSounds.get(0);
        }
        int index = levelAudioRandom.nextInt(currentLevelCompletionSounds.size());
        return currentLevelCompletionSounds.get(index);
    }
    
    private String pickBrickHitSound() {
        if (currentBrickHitSounds == null || currentBrickHitSounds.isEmpty()) {
            return AudioConfig.DEFAULT_BRICK_HIT_SOUND;
        }
        if (currentBrickHitSounds.size() == 1) {
            return currentBrickHitSounds.get(0);
        }
        int index = levelAudioRandom.nextInt(currentBrickHitSounds.size());
        return currentBrickHitSounds.get(index);
    }
    
    public void playBrickHitSound() {
        if (audioManager == null) {
            return;
        }
        String sound = pickBrickHitSound();
        if (sound != null && !sound.isBlank()) {
            audioManager.playSFX(sound);
        }
    }
    
    /**
     * Get save manager
     */
    public SaveManager getSaveManager() {
        return saveManager;
    }
    
    /**
     * Get lives manager
     */
    public com.arcadeblocks.gameplay.LivesManager getLivesManager() {
        return livesManager;
    }
    
    /**
     * Get bonus effect manager
     */
    public com.arcadeblocks.gameplay.BonusEffectManager getBonusEffectManager() {
        return bonusEffectManager;
    }
    
    /**
     * Get bonus timer indicator
     */
    public BonusTimerView getBonusTimerView() {
        return bonusTimerView;
    }
    
    /**
     * Get gameplay UI
     */
    public GameplayUIView getGameplayUIView() {
        return gameplayUIView;
    }
    
    /**
     * Get score manager
     */
    public com.arcadeblocks.gameplay.ScoreManager getScoreManager() {
        return scoreManager;
    }

    public UserAction getPlasmaWeaponAction() {
        return plasmaWeaponAction;
    }
    
    /**
     * Reset paddle key press flags
     */
    public void resetPaddleInputFlags() {
        leftPressed = false;
        rightPressed = false;
        turboPressed = false;
        resetCallBallState();
    }

    public void applyVSyncSetting(boolean enabled) {
        if (saveManager != null) {
            boolean stored = saveManager.isVSyncEnabled();
            if (stored != enabled) {
                saveManager.setVSyncEnabled(enabled);
            }
        }
        // КРИТИЧНО: НЕ сбрасываем vsyncConfigured здесь!
        // Иначе первоначальная настройка не успевала сработать, пока запускалась игра,
        // и потом кустарно приходилось в onUpdate() вызывать applyVSyncInternal(),
        // что приводило к постоянным дерганиям настроек частоты при загрузке
        applyVSyncInternal(enabled);
    }

    public void applySavedVSync() {
        boolean enabled = saveManager == null || saveManager.isVSyncEnabled();
        vsyncConfigured = false;
        applyVSyncInternal(enabled);
    }

    private void applyVSyncInternal(boolean enabled) {
        vsyncEnabled = enabled;
        applyVSyncFlag(enabled);
        vsyncConfigured = true;
    }

    private void applyVSyncFlag(boolean enabled) {
        System.setProperty("prism.vsync", enabled ? "true" : "false");
    }

    private void enforceVSyncAfterResume() {
        applyVSyncInternal(vsyncEnabled);
    }

    /**
     * Go to game
     */
    public void startGame() {
        // Reset loading flag when starting game
        isLoading = false;

        // Reset pause flag when starting game
        isPaused = false;
        
        // Reset paddle input flags for proper control operation
        resetPaddleInputFlags();
        if (levelPassBonusEntity != null && levelPassBonusEntity.isActive()) {
            levelPassBonusEntity.removeFromWorld();
        }
        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
        
        // Stop any current music when starting game
        if (audioManager != null) {
            audioManager.stopMusic();
        // System.out.println("All music stopped - gameplay begins");
        }
        
        clearUINodesSafely();

        resetLevelRuntimeStats();
        
        // Hide cursor during gameplay
        setHiddenCursor();
        // Safety: reinstall mouse handlers
        uninstallMousePaddleControlHandlers();
        
        // Reload key bindings to ensure proper control operation after restart
        reloadInputBindings();
        
        // Ensure window/scene focus immediately
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.stage.Stage st = FXGL.getPrimaryStage();
                if (st != null) {
                    st.requestFocus();
                    if (st.getScene() != null && st.getScene().getRoot() != null) {
                        st.getScene().getRoot().setFocusTraversable(true);
                        st.getScene().getRoot().requestFocus();
                    }
                }
            } catch (Exception ignored) {}
        });
        
        // Set background color
        FXGL.getGameScene().setBackgroundColor(Color.web(GameConfig.DARK_BACKGROUND));
        
        // Create new gameplay UI
        gameplayUIView = new GameplayUIView(this);
        FXGL.getGameScene().addUINode(gameplayUIView);
        
        // Center game world when starting game and update letterbox overlay
        javafx.application.Platform.runLater(() -> {
            centerGameWorld();
            updateLetterboxOverlay();
        });
        
        // Create lives manager (add to game area)
        livesManager = new com.arcadeblocks.gameplay.LivesManager(this);

        // Set lives count in FXGL based on difficulty
        FXGL.set("lives", livesManager.getCurrentLives());

        if (saveManager != null && !isDebugMode) {
            int savedLives = saveManager.getLives();
            livesManager.setCurrentLivesFromSnapshot(savedLives);
            FXGL.set("lives", savedLives);
            if (gameplayUIView != null) {
                gameplayUIView.updateLives(savedLives);
            }
        }
        
        // Create score manager
        scoreManager = new com.arcadeblocks.gameplay.ScoreManager(this);
        scoreManager.loadFromSave();
        
        // Reset score to FXGL value (for new levels)
        int fxglScore = FXGL.geti("score");
        if (fxglScore == 0) {
            scoreManager.resetScore();
        }
        
        // Update UI with initial data
        if (gameplayUIView != null) {
            int currentLevel = FXGL.geti("level");
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(currentLevel);
            String levelName = levelData != null ? levelData.getName() : null;
            gameplayUIView.updateLevel(currentLevel, levelName);
            gameplayUIView.updateScore(FXGL.geti("score"));
            gameplayUIView.updateLives(FXGL.geti("lives"));
        }
        
        // Update level display in ScoreManager
        if (scoreManager != null) {
            int currentLevel = FXGL.geti("level");
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(currentLevel);
            String levelName = levelData != null ? levelData.getName() : null;
            scoreManager.updateLevel(currentLevel, levelName);
        }
        
        if (scoreManager != null) {
            scoreManager.restartLevelTimer();
        }
        
        // Create bonus effect manager
        bonusEffectManager = new com.arcadeblocks.gameplay.BonusEffectManager(this);
        
        // Create bonus timer indicator (disabled - using BonusIndicator)
        // if (bonusTimerView != null) {
        //     bonusTimerView.clearAllBonuses();
        // }
        // bonusTimerView = new BonusTimerView();
        // FXGL.getGameScene().addUINode(bonusTimerView);
        // javafx.application.Platform.runLater(() -> {
        //     if (bonusTimerView != null) {
        //         bonusTimerView.updatePosition(0, 0);
        //     }
        // });
        
        // Create walls for fullscreen
        FXGL.entityBuilder()
            .type(EntityType.WALL)
            .at(0, 0)
            .bbox(new com.almasb.fxgl.physics.HitBox(com.almasb.fxgl.physics.BoundingShape.box(10, GameConfig.GAME_HEIGHT)))
            .collidable()
            .buildAndAttach();
            
        FXGL.entityBuilder()
            .type(EntityType.WALL)
            .at(GameConfig.GAME_WIDTH - 10, 0)
            .bbox(new com.almasb.fxgl.physics.HitBox(com.almasb.fxgl.physics.BoundingShape.box(10, GameConfig.GAME_HEIGHT)))
            .collidable()
            .buildAndAttach();
            
        FXGL.entityBuilder()
            .type(EntityType.WALL)
            .at(0, GameConfig.TOP_UI_HEIGHT)
            .bbox(new com.almasb.fxgl.physics.HitBox(com.almasb.fxgl.physics.BoundingShape.box(GameConfig.GAME_WIDTH, 10)))
            .collidable()
            .buildAndAttach();
        
        // Create paddle
        com.almasb.fxgl.entity.Entity paddle = FXGL.spawn("paddle", GameConfig.PADDLE_START_POS);
        paddleComponent = new com.arcadeblocks.gameplay.Paddle();
        paddle.addComponent(paddleComponent);
        
        // Set paddle speed from settings
        paddleComponent.setSpeed(saveManager.getPaddleSpeed());
        paddleComponent.setTurboMultiplier(cachedTurboSpeed);
        
        // Ensure paddle movement is unblocked (especially important after Game Over restart)
        paddleComponent.setMovementBlocked(false);
        
        // Enable mouse control for paddle
        installMousePaddleControlHandlers();
        // Additionally: reinstall after a short interval to be the last event consumer
        FXGL.runOnce(() -> installMousePaddleControlHandlers(), javafx.util.Duration.seconds(0.05));
        
        // Unblock mouse clicks for new level
        unblockMouseClicks();
        
        // Removed cursor setup
        
        double paddleWidth = paddle.getWidth();
        double defaultOffsetX = paddleWidth * 0.18;
        double defaultOffsetY = -GameConfig.BALL_RADIUS * 2 - 5;
        double spawnBallX = paddle.getX() + paddleWidth / 2.0 + defaultOffsetX - GameConfig.BALL_RADIUS;
        double spawnBallY = paddle.getY() + defaultOffsetY;
        com.almasb.fxgl.entity.Entity ball = FXGL.spawn("ball", spawnBallX, spawnBallY);
        Ball ballComponent = new com.arcadeblocks.gameplay.Ball();
        ball.addComponent(ballComponent);
        
        // Check if chaotic balls bonus is active (for initial ball)
        if (bonusEffectManager != null && bonusEffectManager.isChaoticBallsActive()) {
            ballComponent.setChaoticBall(true);
        // System.out.println("Initial ball became chaotic (chaotic balls bonus active)");
        }
        
        // Check if sticky paddle is active (for initial ball)
        if (bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive()) {
            ballComponent.setStickyEnabled(true);
            // System.out.println("Initial ball got stickiness (sticky paddle active)");
        }
        
        ballComponent.setAttachedOffset(defaultOffsetX, defaultOffsetY);
        ballComponent.attachToPaddle(paddle);

        // Add initial ball to attached list
        com.arcadeblocks.gameplay.Ball.addAttachedBall(ball);

        scheduleAutoLaunch();

        // Debug information
        // System.out.println("Ball created and attached to paddle:");
        // System.out.println("  Paddle position: " + paddle.getX() + ", " + paddle.getY());
        // System.out.println("  Ball position: " + ball.getX() + ", " + ball.getY());
        // System.out.println("  Attached: " + ballComponent.isAttachedToPaddle());
        
        // Stop menu music and start game music
        // audioManager.playMusic("music/level1.mp3", true); // Level music disabled
        
        // Ensure game is not paused
        FXGL.getGameController().resumeEngine();

        // После старта уровня ссылка на звук загрузки больше не нужна — сбрасываем
        releaseLevelLoadingSoundReference();
    }
    
    /**
     * Go to settings
     */
    public void showSettings() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под SettingsView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем SettingsView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.SettingsView(this));
    }
    
    /**
     * Go to help
     */
    public void showHelp() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под HelpView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем HelpView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.HelpView(this));
    }
    
    /**
     * Go to language selection window
     */
    public void showLanguageWindow() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под LanguageView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем LanguageView поверх MainMenuView
        // TODO: LanguageView removed - FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.LanguageView(this));
    }
    
    /**
     * Set game language
     */
    public void setLanguage(String languageCode) {
        if (saveManager != null) {
            saveManager.setLanguage(languageCode);
            saveManager.awaitPendingWrites();
            // Can add logic here to reload interface with new language
            // For now just save the language choice
        }
        LocalizationManager.getInstance().setLanguage(languageCode);
    }
    
    /**
     * Go to credits
     */
    public void showCredits() {
        showCredits(false, false);
    }
    
    public void showCredits(boolean fromSaveSystem) {
        showCredits(fromSaveSystem, false);
    }

    public void showCredits(boolean fromSaveSystem, boolean bonusEnding) {
        // Cancel pending music since credits will have its own music
        if (audioManager != null) {
            audioManager.cancelPendingMusic();
        }
        
        // Remove mouse handlers when transitioning to credits
        uninstallMousePaddleControlHandlers();
        // Unblock mouse clicks in credits
        unblockMouseClicks();
        // Set system cursor in credits
        setSystemCursor();

        cleanupGameplayState();
        
        clearUINodesSafely();
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.CreditsView(this, fromSaveSystem, bonusEnding));
    }
    
    private void showCreditsForLBreakout1() {
        if (audioManager != null) {
            audioManager.cancelPendingMusic();
            audioManager.stopMusic();
        }

        uninstallMousePaddleControlHandlers();
        unblockMouseClicks();
        setSystemCursor();

        cleanupGameplayState();
        clearUINodesSafely();
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.CreditsView(
            this,
            false,
            false,
            "textures/back4.jpg",
            true
        ));
    }
    
    /**
     * Воспроизведение видео после титров и возврат в главное меню
     */
    public void playAfterCreditsVideo() {
        playVideoOverlay("after_credits_video.mp4", 22.0, remover -> {
            // После видео возвращаемся в главное меню
            javafx.application.Platform.runLater(() -> {
                // Удаляем оверлей видео
                if (remover != null) {
                    remover.run();
                }
                
                // Запускаем музыку главного меню с учетом завершения игры
                startMainMenuMusic();
                
                // Возвращаемся в главное меню
                returnToMainMenuFromSettings();
            });
        });
    }

    /**
     * Воспроизведение альтернативного видео после титров и возврат в главное меню
     */
    public void playAfterCreditsVideo(String videoPath, double fallbackDurationSeconds) {
        playVideoOverlay(videoPath, fallbackDurationSeconds, remover -> {
            javafx.application.Platform.runLater(() -> {
                if (remover != null) {
                    remover.run();
                }
                startMainMenuMusic();
                returnToMainMenuFromSettings();
            });
        });
    }
    
    /**
     * Show save game window
     */
    public void showSaveGameWindow() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под SaveGameView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем SaveGameView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.SaveGameView(this));
    }
    
    /**
     * Start level 0 (debug level)
     */
    public void startLevel0() {
        startLevel(0, true); // Reset score for debug level
    }
    
    /**
     * Start level in debug mode (completely decoupled from save system)
     */
    public void startDebugLevel(int levelNumber) {
        startDebugLevel(levelNumber, null, null);
    }

    private void startDebugLevel(int levelNumber, Integer livesCarryOver, Integer scoreCarryOver) {
        startDebugLevel(levelNumber, livesCarryOver, scoreCarryOver, null);
    }
    
    /**
     * КРИТИЧНО: Перегруженная версия для бесшовного перехода от видео к сюжетному окну в debug режиме
     */
    private void startDebugLevel(int levelNumber, Integer livesCarryOver, Integer scoreCarryOver, Runnable overlayRemover) {
        // System.out.println("DEBUG: Starting level " + levelNumber + " in debug mode (no save)");

        isDebugMode = true;
        com.arcadeblocks.config.DifficultyLevel debugDifficulty = com.arcadeblocks.config.DifficultyLevel.NORMAL;
        if (saveManager != null) {
            try {
                com.arcadeblocks.config.DifficultyLevel configuredDifficulty = saveManager.getDifficulty();
                if (configuredDifficulty != null) {
                    debugDifficulty = configuredDifficulty;
                }
            } catch (Exception ignored) {}
        }
        debugDifficultyOverride = debugDifficulty;

        int initialLives = livesCarryOver != null ? Math.max(1, livesCarryOver) : debugDifficulty.getLives();
        int initialScore = scoreCarryOver != null ? Math.max(0, scoreCarryOver) : 0;
        setDebugLivesOverride(initialLives);
        setDebugScoreOverride(initialScore);

        FXGL.set("score", initialScore);
        FXGL.set("lives", initialLives);
        resetFadeState();
        if (livesManager != null) {
            livesManager.setLives(initialLives);
        }
        if (scoreManager != null) {
            scoreManager.setScore(initialScore);
        }
        recordLevelStartStats(levelNumber);

        // Reset level completion and transition flags
        isLevelCompleted = false;
        isTransitioning = false;
        levelCompletedMessageShown = false;
        proceedToNextLevelCalled = false;
        Runnable levelReloadGuard = beginLevelReloadGuard();
        com.arcadeblocks.gameplay.Ball.clearAttachedBalls();

        com.arcadeblocks.config.LevelConfig.LevelData levelData = getLevelDataUnified(levelNumber);
        if (levelData == null) {
            System.err.println("Level " + levelNumber + " not found in configuration!");
            return;
        }

        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;

        prepareLevelAudio(levelNumber);

        clearLevelBackground();

        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(this::removeEntitySafely);

        clearUINodesSafely();
        
        // КРИТИЧНО: Если есть overlayRemover, НЕ вызываем cleanupActiveVideoResources()
        // Оверлей от видео босса должен остаться как черный фон под сюжетным окном
        if (overlayRemover == null) {
            // CRITICAL: Clean all active video resources before starting new level
            // This prevents memory leaks when transitioning from debug menu to special level
            cleanupActiveVideoResources();
        }

        FXGL.set("level", levelNumber);

        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);

        Runnable startLevelTask = () -> {
            try {
                startGame();
                setLevelBackground(levelNumber, levelData.getBackgroundImage());

                if (debugLivesOverride != null && livesManager != null) {
                    livesManager.setLives(debugLivesOverride);
                    FXGL.set("lives", debugLivesOverride);
                }
                if (debugScoreOverride != null && scoreManager != null) {
                    scoreManager.setScore(debugScoreOverride);
                    FXGL.set("score", debugScoreOverride);
                }

                com.arcadeblocks.gameplay.Brick.resetBrickCounter();
                com.arcadeblocks.levels.LevelLoader.loadLevel(levelNumber, levelData);

                playLevelMusic(levelNumber);
                playPaddleBallFadeIn(false);
                FXGL.runOnce(levelReloadGuard, LEVEL_FADE_DURATION);

            } catch (Exception e) {
                levelReloadGuard.run();
                System.err.println("Error starting debug level " + levelNumber + ": " + e.getMessage());
                e.printStackTrace();
            }
        };
        ChapterStoryData chapterStory = StoryConfig.findForLevel(levelNumber)
            .filter(data -> alwaysShowChapterStory || (StoryConfig.shouldShowForLevel(levelNumber, new StoryConfig.GameProgress(shownChapterStoryChapters, true)) && shouldShowLevel1Story(levelNumber)))
            .orElse(null);

        if (chapterStory != null) {
            showChapterStory(chapterStory, levelNumber, levelData.getName(), startLevelTask, overlayRemover);
            return;
        }
        
        runOverlayRemover(overlayRemover);

        if (metadata != null && metadata.getIntroVideoPath() != null && shouldShowLevel1StoryAndVideo(levelNumber)) {
            playLevelIntroVideo(levelNumber, metadata, startLevelTask);
        } else {
            // levelName passed for compatibility, but LevelIntroView uses getLocalizedLevelName
            // (DEBUG) label is added in LevelIntroView based on isDebugMode()
            showLevelIntro(levelNumber, levelData.getName(), startLevelTask);
        }
    }
    
    
    /**
     * Check if we are in debug mode
     */
    public boolean isDebugMode() {
        return isDebugMode;
    }
    
    public boolean isAlwaysShowChapterStory() {
        return alwaysShowChapterStory;
    }
    
    public void setAlwaysShowChapterStory(boolean alwaysShowChapterStory) {
        this.alwaysShowChapterStory = alwaysShowChapterStory;
    }
    
    public com.arcadeblocks.config.DifficultyLevel getDebugDifficultyOverride() {
        return debugDifficultyOverride;
    }
    
    public Integer getDebugLivesOverride() {
        return debugLivesOverride;
    }
    
    public void setDebugLivesOverride(Integer lives) {
        if (lives == null) {
            debugLivesOverride = null;
        } else {
            debugLivesOverride = Math.max(1, lives);
        }
    }
    
    public Integer getDebugScoreOverride() {
        return debugScoreOverride;
    }
    
    public void setDebugScoreOverride(Integer score) {
        if (score == null) {
            debugScoreOverride = null;
        } else {
            debugScoreOverride = Math.max(0, score);
        }
    }
    
    public com.arcadeblocks.config.DifficultyLevel getEffectiveDifficulty() {
        if (isDebugMode && debugDifficultyOverride != null) {
            return debugDifficultyOverride;
        }
        if (saveManager != null) {
            try {
                com.arcadeblocks.config.DifficultyLevel difficulty = saveManager.getGameDifficulty();
                if (difficulty != null) {
                    return difficulty;
                }
            } catch (Exception ignored) {}
            try {
                com.arcadeblocks.config.DifficultyLevel fallback = saveManager.getDifficulty();
                if (fallback != null) {
                    return fallback;
                }
            } catch (Exception ignored) {}
        }
        return com.arcadeblocks.config.DifficultyLevel.NORMAL;
    }
    
    /**
     * Reset debug mode
     */
    public void resetDebugMode() {
        isDebugMode = false;
        debugDifficultyOverride = null;
        debugLivesOverride = null;
        debugScoreOverride = null;
    }
    
    private void prepareLevelAudio(int levelNumber) {
        currentLevelLoadingSound = AudioConfig.getLevelLoadingSound(levelNumber);
        List<String> levelHitSounds = AudioConfig.getBrickHitSounds(levelNumber);
        if (levelHitSounds == null || levelHitSounds.isEmpty()) {
            currentBrickHitSounds = new ArrayList<>(AudioConfig.getDefaultBrickHitSounds());
        } else {
            currentBrickHitSounds = new ArrayList<>(levelHitSounds);
        }
        List<String> completionSounds = AudioConfig.getLevelCompletionSounds(levelNumber);
        if (completionSounds == null || completionSounds.isEmpty()) {
            currentLevelCompletionSounds = new ArrayList<>(AudioConfig.getDefaultLevelCompletionSounds());
        } else {
            currentLevelCompletionSounds = new ArrayList<>(completionSounds);
        }
    }
    
    private double getLevelCompletionPauseSeconds(int levelNumber) {
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        if (metadata != null && metadata.getCompletionPauseSeconds() != null) {
            return metadata.getCompletionPauseSeconds();
        }
        return com.arcadeblocks.config.LevelConfig.isBossLevel(levelNumber) ? 12.0 : 4.0;
    }

    private void showLevelCompletionMessage(int levelNumber, int currentScore, int currentLives, Runnable onContinue) {
        boolean alreadyShowing = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> "levelCompletionMessage".equals(node.getUserData()));
        if (alreadyShowing) {
            return;
        }

        com.arcadeblocks.localization.LocalizationManager localizationManager = 
            com.arcadeblocks.localization.LocalizationManager.getInstance();
        unblockMouseClicks();
        setSystemCursor();
        String playerName = saveManager != null ? saveManager.getPlayerName() : null;
        if (playerName == null || playerName.isBlank()) {
            playerName = localizationManager.get("player.default");
        }
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        String message = metadata != null ? metadata.formatCompletionMessage(playerName) : null;
        if (message == null || message.isBlank()) {
            // Use localized message
            message = localizationManager.format("level.completion.message", playerName, levelNumber);
        }

        // Get chapter color for the level
        String chapterColor = GameConfig.NEON_PINK;
        com.arcadeblocks.config.LevelConfig.LevelChapter chapter =
            com.arcadeblocks.config.LevelConfig.getChapter(levelNumber);
        if (chapter != null) {
            chapterColor = chapter.getAccentColorHex();
        } else if (com.arcadeblocks.config.BonusLevelConfig.isBonusLevel(levelNumber)) {
            var bonusChapter = com.arcadeblocks.config.BonusLevelConfig.getChapter(levelNumber);
            if (bonusChapter != null && bonusChapter.getAccentColorHex() != null) {
                chapterColor = bonusChapter.getAccentColorHex();
            }
        }

        Label label = new Label(message);
        label.setFont(Font.font("Orbitron", FontWeight.BOLD, 32));
        label.setTextFill(Color.web(chapterColor));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(GameConfig.GAME_WIDTH * 0.7);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(chapterColor, 0.55));
        glow.setRadius(12);
        glow.setSpread(0.28);
        label.setEffect(glow);

        StackPane overlay = new StackPane();
        overlay.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(3,5,12,0.75);");
        overlay.setAlignment(Pos.TOP_CENTER);
        overlay.setUserData("levelCompletionMessage");
        overlay.setMouseTransparent(false);
        overlay.setOpacity(0.0);

        HBox starsRow = new HBox(12);
        starsRow.setAlignment(Pos.CENTER);
        List<Polygon> starShapes = new ArrayList<>(5);
        Color filledStarColor = Color.web("#ffd447");
        Color emptyStarColor = Color.web("#5c4c25");
        Color starStroke = Color.web("#f6e8a6", 0.7);
        for (int i = 0; i < 5; i++) {
            Polygon star = createStarShape(22, 10);
            star.setFill(emptyStarColor);
            star.setStroke(starStroke);
            star.setStrokeWidth(1.6);
            star.setEffect(new DropShadow(10, Color.web(chapterColor, 0.4)));
            star.setScaleX(0.9);
            star.setScaleY(0.9);
            starShapes.add(star);
            starsRow.getChildren().add(star);
        }

        int safeLivesLost = Math.max(0, livesLostThisLevel);
        int filledStars = Math.min(5, Math.max(1, 5 - safeLivesLost));
        if (saveManager != null && !isDebugMode) {
            saveManager.setLevelStars(levelNumber, filledStars);
        }

        double levelTimeSeconds = scoreManager != null ? scoreManager.getLevelTimerSeconds() : 0.0;

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(18);
        statsGrid.setVgap(10);
        statsGrid.setAlignment(Pos.CENTER);
        LocalizationManager lm = LocalizationManager.getInstance();
        addStatRow(statsGrid, 0, lm.get("level.completion.stats.score"), formatScoreForSummary(currentScore));
        addStatRow(statsGrid, 1, lm.get("level.completion.stats.time"), formatTimeForSummary(levelTimeSeconds));
        addStatRow(statsGrid, 2, lm.get("level.completion.stats.lives_lost"), String.valueOf(safeLivesLost));
        addStatRow(statsGrid, 3, lm.get("level.completion.stats.positive_bonuses"), String.valueOf(positiveBonusesCollectedThisLevel));
        addStatRow(statsGrid, 4, lm.get("level.completion.stats.negative_bonuses"), String.valueOf(negativeBonusesCollectedThisLevel));

        Button continueButton = new Button(lm.get("level.completion.button.continue"));
        continueButton.setCursor(Cursor.HAND);
        continueButton.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        continueButton.setTextFill(Color.web("#0c0c0f"));
        continueButton.setStyle("-fx-background-color: linear-gradient(to right, #ffd447, #ffb347);" +
            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: rgba(0,0,0,0.35);" +
            "-fx-border-width: 1; -fx-padding: 10 20 10 20;");
        continueButton.setOnMouseEntered(e -> {
            continueButton.setScaleX(1.03);
            continueButton.setScaleY(1.03);
            if (audioManager != null) {
                audioManager.playSFX("sounds/menu_hover.wav");
            }
        });
        continueButton.setOnMouseExited(e -> {
            continueButton.setScaleX(1.0);
            continueButton.setScaleY(1.0);
        });

        Button restartButton = new Button(lm.get("level.completion.button.restart"));
        restartButton.setCursor(Cursor.HAND);
        restartButton.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        restartButton.setTextFill(Color.web("#f5f7ff"));
        restartButton.setStyle("-fx-background-color: linear-gradient(to right, #343b55, #1f2436);" +
            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-width: 1; -fx-padding: 10 20 10 20;");
        restartButton.setOnMouseEntered(e -> {
            restartButton.setScaleX(1.03);
            restartButton.setScaleY(1.03);
            if (audioManager != null) {
                audioManager.playSFX("sounds/menu_hover.wav");
            }
        });
        restartButton.setOnMouseExited(e -> {
            restartButton.setScaleX(1.0);
            restartButton.setScaleY(1.0);
        });

        HBox buttonsRow = new HBox(12, restartButton, continueButton);
        buttonsRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, label, starsRow, statsGrid, buttonsRow);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 28, 26, 28));
        content.setMaxWidth(GameConfig.GAME_WIDTH * 0.7);
        content.setStyle("-fx-background-color: rgba(18,20,30,0.95); -fx-background-radius: 18;" +
            "-fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;");
        DropShadow cardShadow = new DropShadow();
        cardShadow.setColor(Color.web(chapterColor, 0.35));
        cardShadow.setRadius(16);
        cardShadow.setSpread(0.12);
        content.setEffect(cardShadow);
        StackPane.setMargin(content, new Insets(32, 0, 0, 0));

        overlay.getChildren().add(content);
        FXGL.getGameScene().addUINode(overlay);

        SequentialTransition starAnimation = buildStarFillAnimation(starShapes, filledStars, filledStarColor, emptyStarColor);
        ScaleTransition pop = new ScaleTransition(Duration.seconds(1.4), content);
        pop.setFromX(0.98);
        pop.setToX(1.02);
        pop.setFromY(0.98);
        pop.setToY(1.02);
        pop.setAutoReverse(true);
        pop.setCycleCount(Animation.INDEFINITE);

        java.util.concurrent.atomic.AtomicBoolean actionTaken = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable triggerContinue = () -> {
            if (actionTaken.getAndSet(true)) {
                return;
            }
            starAnimation.stop();
            pop.stop();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), overlay);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> {
                removeUINodeSafely(overlay);
                recordLevelCompletionStats(levelNumber, levelTimeSeconds);
                if (onContinue != null) {
                    onContinue.run();
                }
            });
            fadeOut.play();
        };
        Runnable triggerRestart = () -> {
            if (actionTaken.getAndSet(true)) {
                return;
            }
            starAnimation.stop();
            pop.stop();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), overlay);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> {
                removeUINodeSafely(overlay);
                restartLevelFromCompletion(levelNumber, currentScore, currentLives);
            });
            fadeOut.play();
        };

        continueButton.setOnAction(e -> {
            if (audioManager != null) {
                audioManager.playSFX("sounds/menu_select.wav");
            }
            triggerContinue.run();
        });
        restartButton.setOnAction(e -> {
            if (audioManager != null) {
                audioManager.playSFX("sounds/menu_back.wav");
            }
            triggerRestart.run();
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(380), overlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> starAnimation.play());
        fadeIn.play();

        pop.play();

    }

    private void addStatRow(GridPane grid, int rowIndex, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Orbitron", FontWeight.NORMAL, 14));
        label.setTextFill(Color.web("#9fb2c8"));
        Label value = new Label(valueText);
        value.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        value.setTextFill(Color.web("#f3f6ff"));
        grid.add(label, 0, rowIndex);
        grid.add(value, 1, rowIndex);
    }

    public void restartCurrentLevelFromPause() {
        int levelNumber = FXGL.geti("level");
        int score = scoreManager != null ? scoreManager.getCurrentScore() : FXGL.geti("score");
        int lives = FXGL.geti("lives");
        restartLevelFromCompletion(levelNumber, score, lives);
    }

    private void restartLevelFromCompletion(int levelNumber, int score, int lives) {
        javafx.application.Platform.runLater(() -> {
            isLevelCompleted = false;
            isTransitioning = false;
            levelCompletedMessageShown = false;
            proceedToNextLevelCalled = false;
            pendingLevelWarpTarget = null;
            int restartScore = levelStartScore;
            int restartLives = levelStartLives;
            if (levelStartLevel != levelNumber || restartLives <= 0) {
                restartScore = score;
                restartLives = lives;
            }
            restartLives = Math.max(1, restartLives);
            restartScore = Math.max(0, restartScore);
            if (saveManager != null && !isDebugMode) {
                saveManager.clearGameSnapshot();
                saveManager.setLevelCompleted(levelNumber, false);
                saveManager.setCurrentLevel(levelNumber);
                saveManager.setScore(restartScore);
                saveManager.setLives(restartLives);
            }
            FXGL.set("score", restartScore);
            FXGL.set("lives", restartLives);
            if (scoreManager != null) {
                scoreManager.setScore(restartScore);
                scoreManager.setLevelTimerSeconds(0);
            }
            if (livesManager != null) {
                livesManager.setLives(restartLives);
            }
            if (gameplayUIView != null) {
                gameplayUIView.updateScore(restartScore);
                gameplayUIView.updateLives(restartLives);
            }
            startLevel(levelNumber, false);
        });
    }

    private String formatTimeForSummary(double seconds) {
        int totalSeconds = (int) Math.max(0, Math.round(seconds));
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private String formatScoreForSummary(int score) {
        return String.format("%,d", Math.max(0, score));
    }

    private Polygon createStarShape(double outerRadius, double innerRadius) {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double angleDeg = -90 + i * 36;
            double angleRad = Math.toRadians(angleDeg);
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double x = radius * Math.cos(angleRad);
            double y = radius * Math.sin(angleRad);
            star.getPoints().addAll(x, y);
        }
        return star;
    }

    private SequentialTransition buildStarFillAnimation(List<Polygon> stars, int filledStars, Color filledColor, Color emptyColor) {
        SequentialTransition sequence = new SequentialTransition();
        int cappedFilled = Math.max(0, Math.min(filledStars, stars.size()));
        for (int i = 0; i < cappedFilled; i++) {
            Polygon star = stars.get(i);
            FillTransition fill = new FillTransition(Duration.millis(240), star, emptyColor, filledColor);
            ScaleTransition pop = new ScaleTransition(Duration.millis(260), star);
            pop.setFromX(0.75);
            pop.setFromY(0.75);
            pop.setToX(1.1);
            pop.setToY(1.1);
            pop.setAutoReverse(true);
            pop.setCycleCount(2);
            PauseTransition sound = new PauseTransition(Duration.ZERO);
            sound.setOnFinished(evt -> {
                if (audioManager != null) {
                    audioManager.playSFX("sounds/extra_score.wav");
                }
            });
            ParallelTransition step = new ParallelTransition(fill, pop);
            sequence.getChildren().add(new SequentialTransition(sound, step, new PauseTransition(Duration.millis(80))));
        }
        return sequence;
    }

    private void showGameLineCompletionOverlay(GameLine gameLine, Runnable onContinue) {
        boolean alreadyShowing = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> "gameLineCompletionMessage".equals(node.getUserData()));
        if (alreadyShowing) {
            return;
        }
        unblockMouseClicks();
        setSystemCursor();

        GameLine line = gameLine != null ? gameLine : GameLine.fromLevel(FXGL.geti("level"));
        LocalizationManager lm = LocalizationManager.getInstance();
        String lineName = lm.get(line.getNameKey());
        String playerName = saveManager != null ? saveManager.getPlayerName() : null;
        if (playerName == null || playerName.isBlank()) {
            playerName = lm.get("player.default");
        }

        String headlineText = String.format("%s %s %s", playerName,
            lm.get("level.completion.final.congrats"), lineName);

        Label headline = new Label(headlineText);
        headline.setFont(Font.font("Orbitron", FontWeight.EXTRA_BOLD, 26));
        headline.setTextFill(Color.web("#7CFF72"));
        headline.setWrapText(true);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(GameConfig.GAME_WIDTH * 0.8);
        DropShadow headShadow = new DropShadow();
        headShadow.setColor(Color.web("#7CFF72", 0.7));
        headShadow.setRadius(14);
        headShadow.setSpread(0.18);
        headline.setEffect(headShadow);

        int totalScore = saveManager != null ? saveManager.getScore() : FXGL.geti("score");
        int totalLevels = line.getEndLevel() - line.getStartLevel() + 1;
        double totalTimeSeconds = saveManager != null ? saveManager.getLineTimeSeconds(line) : -1.0;
        int totalPos = saveManager != null ? saveManager.getLinePositiveBonuses(line) : 0;
        int totalNeg = saveManager != null ? saveManager.getLineNegativeBonuses(line) : 0;
        double avgStars = saveManager != null ? saveManager.getAverageStarsForLine(line) : -1.0;

        GridPane stats = new GridPane();
        stats.setHgap(18);
        stats.setVgap(10);
        stats.setAlignment(Pos.CENTER);
        addStatRow(stats, 0, lm.get("level.completion.stats.score"), formatScoreForSummary(totalScore));
        addStatRow(stats, 1, lm.get("level.completion.stats.levels_total"), String.valueOf(totalLevels));
        addStatRow(stats, 2, lm.get("level.completion.stats.time_total"), formatTimeForSummary(totalTimeSeconds));
        addStatRow(stats, 3, lm.get("level.completion.stats.positive_bonuses_total"), String.valueOf(totalPos));
        addStatRow(stats, 4, lm.get("level.completion.stats.negative_bonuses_total"), String.valueOf(totalNeg));

        Label avgLabel = new Label(lm.get("level.completion.stats.avg_stars"));
        avgLabel.setFont(Font.font("Orbitron", FontWeight.NORMAL, 14));
        avgLabel.setTextFill(Color.web("#9fb2c8"));
        Label avgValue = new Label(avgStars > 0 ? String.format("%.2f ★", avgStars) : "—");
        avgValue.setFont(Font.font("Orbitron", FontWeight.EXTRA_BOLD, 18));
        avgValue.setTextFill(Color.web("#ffd447"));
        DropShadow avgGlow = new DropShadow();
        avgGlow.setColor(Color.web("#ffd447", 0.7));
        avgGlow.setRadius(12);
        avgGlow.setSpread(0.22);
        avgValue.setEffect(avgGlow);
        stats.add(avgLabel, 0, 5);
        stats.add(avgValue, 1, 5);

        ScaleTransition avgPulse = new ScaleTransition(Duration.seconds(1.2), avgValue);
        avgPulse.setFromX(0.96);
        avgPulse.setToX(1.08);
        avgPulse.setFromY(0.96);
        avgPulse.setToY(1.08);
        avgPulse.setCycleCount(Animation.INDEFINITE);
        avgPulse.setAutoReverse(true);

        Button continueButton = new Button(lm.get("level.completion.button.continue"));
        continueButton.setCursor(Cursor.HAND);
        continueButton.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        continueButton.setTextFill(Color.web("#0c0c0f"));
        continueButton.setStyle("-fx-background-color: linear-gradient(to right, #ffd447, #ffb347);" +
            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: rgba(0,0,0,0.35);" +
            "-fx-border-width: 1; -fx-padding: 10 26 10 26;");
        continueButton.setOnMouseEntered(e -> {
            continueButton.setScaleX(1.03);
            continueButton.setScaleY(1.03);
            if (audioManager != null) {
                audioManager.playSFX("sounds/menu_hover.wav");
            }
        });
        continueButton.setOnMouseExited(e -> {
            continueButton.setScaleX(1.0);
            continueButton.setScaleY(1.0);
        });

        StackPane overlay = new StackPane();
        overlay.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(3,5,12,0.82);");
        overlay.setAlignment(Pos.TOP_CENTER);
        overlay.setUserData("gameLineCompletionMessage");
        overlay.setOpacity(0.0);

        VBox content = new VBox(18, headline, stats, continueButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26, 28, 30, 28));
        content.setMaxWidth(GameConfig.GAME_WIDTH * 0.78);
        content.setStyle("-fx-background-color: rgba(14,16,24,0.98); -fx-background-radius: 18;" +
            "-fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;");
        DropShadow cardShadow = new DropShadow();
        cardShadow.setColor(Color.web("#7CFF72", 0.25));
        cardShadow.setRadius(18);
        cardShadow.setSpread(0.14);
        content.setEffect(cardShadow);
        StackPane.setMargin(content, new Insets(40, 0, 0, 0));

        overlay.getChildren().add(content);
        FXGL.getGameScene().addUINode(overlay);

        Runnable triggerContinue = () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), overlay);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> {
                avgPulse.stop();
                removeUINodeSafely(overlay);
                if (onContinue != null) {
                    onContinue.run();
                }
            });
            fadeOut.play();
        };

        continueButton.setOnAction(e -> {
            if (audioManager != null) {
                audioManager.playSFX("sounds/menu_select.wav");
            }
            triggerContinue.run();
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(380), overlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> avgPulse.play());
        fadeIn.play();
    }
    
    /**
     * Start specified level
     */
    public void startLevel(int levelNumber) {
        startLevel(levelNumber, false);
    }
    /**
     * Получить данные уровня (обычного или бонусного)
     */
    private com.arcadeblocks.config.LevelConfig.LevelData getLevelDataUnified(int levelNumber) {
        if (BonusLevelConfig.isBonusLevel(levelNumber)) {
            BonusLevelConfig.BonusLevelData bonusData = BonusLevelConfig.getLevelData(levelNumber);
            if (bonusData != null) {
                return new com.arcadeblocks.config.LevelConfig.LevelData(
                    bonusData.getNameKey(),
                    bonusData.getName(),
                    "level" + levelNumber + ".json",
                    bonusData.getBackgroundImage(),
                    bonusData.getMusicFile()
                );
            }
        }
        return com.arcadeblocks.config.LevelConfig.getLevel(levelNumber);
    }

    
    /**
     * Start specified level with option to reset score
     */
    public void startLevel(int levelNumber, boolean resetScore) {
        startLevel(levelNumber, resetScore, null);
    }
    
    /**
     * КРИТИЧНО: Перегруженная версия для бесшовного перехода от видео к сюжетному окну
     * overlayRemover удаляется только когда сюжетное окно уже показано
     */
    private void startLevel(int levelNumber, boolean resetScore, Runnable overlayRemover) {
        // Reset level completion and transition flags - resume gameplay
        isLevelCompleted = false;
        isTransitioning = false;
        isGameOver = false;
        levelCompletedMessageShown = false;
        proceedToNextLevelCalled = false;

        // Удаляем ссылки на фон главного меню, чтобы его текстуры не висели в памяти в игровом мире
        releaseMainMenuBackgroundAssets();
        
        // Reset paddle input flags for proper control operation after restart
        resetPaddleInputFlags();
        // System.out.println("Flags isLevelCompleted, isTransitioning, levelCompletedMessageShown and proceedToNextLevelCalled reset");
        Runnable levelReloadGuard = beginLevelReloadGuard();
        
        // Clear attached balls list when starting new level
        com.arcadeblocks.gameplay.Ball.clearAttachedBalls();
        
        // Reset pause flag when starting new level
        isPaused = false;
        
        // Get level data
        com.arcadeblocks.config.LevelConfig.LevelData levelData = getLevelDataUnified(levelNumber);
        if (levelData == null) {
            System.err.println("Level " + levelNumber + " not found in configuration!");
            return;
        }
        // System.out.println("Level data obtained: " + levelData.getName());

        if (scoreManager != null) {
            scoreManager.hideBossHealth();
        }
        
        prepareLevelAudio(levelNumber);
        
        // Clear background images before showing loading screen
        clearLevelBackground();
        
        // Clear all entities (create copy of list for safe removal)
        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(this::removeEntitySafely);
        
        // Clear current scene after removing entities
        clearUINodesSafely();
        
        // КРИТИЧНО: Если есть overlayRemover, НЕ вызываем cleanupActiveVideoResources()
        // Оверлей от видео босса должен остаться как черный фон под сюжетным окном
        // Он будет удален через overlayRemover когда сюжетное окно закроется
        if (overlayRemover == null) {
            // Clean all active video resources before starting new level
            // This prevents memory leaks when transitioning between levels
            cleanupActiveVideoResources();
        }
        
        // Set level
        FXGL.set("level", levelNumber);
        if (saveManager != null) {
            saveManager.setCurrentGameLine(com.arcadeblocks.config.GameLine.fromLevel(levelNumber));
            saveManager.setCurrentLevel(levelNumber);
        }
        // System.out.println("Level set in FXGL: " + levelNumber);
        
        // Reset score only if explicitly requested
        if (resetScore) {
            FXGL.set("score", 0);
            resetContinueCount();
            // Set lives count based on difficulty
            com.arcadeblocks.config.DifficultyLevel difficulty = getEffectiveDifficulty();
            FXGL.set("lives", difficulty.getLives());
            if (saveManager != null) {
                saveManager.setLives(difficulty.getLives());
            }
        } else {
            // Load saved data from SaveManager
            FXGL.set("score", saveManager.getScore());
            int savedLives = saveManager.getLives();
            
            // Protection from starting level with 0 lives
            if (savedLives <= 0) {
                // System.out.println("Detected save with 0 lives - deleting save and starting new game");
                saveManager.deleteSaveFileForSlot(getOriginalSaveSlot());
                // Set lives count based on difficulty
                com.arcadeblocks.config.DifficultyLevel difficulty = getEffectiveDifficulty();
                savedLives = difficulty.getLives();
                saveManager.setLives(savedLives);
            }
            
            FXGL.set("lives", savedLives);
        }
        resetFadeState();
        recordLevelStartStats(levelNumber);
        

        Runnable initializeLevelTask = () -> {
            try {
        // System.out.println("Starting level initialization " + levelNumber);
                
                startGame();
        // System.out.println("Game scene initialized");
                
                setLevelBackground(levelNumber, levelData.getBackgroundImage());
        // System.out.println("Level background set");

                boolean restored = false;
                if (saveManager != null && saveManager.isResumeSnapshotPending()) {
                    GameSnapshot snapshot = saveManager.loadGameSnapshot();
                    if (snapshot != null && snapshot.level == levelNumber) {
                        restored = restoreGameSnapshot(snapshot);
                        if (restored) {
                            recordLevelStartStats(levelNumber);
                            saveManager.markResumeSnapshotConsumed();
                        }
                        // Очищаем snapshot после использования
                        snapshot.clear();
                    } else if (snapshot != null) {
                        // Очищаем snapshot если он не подошел
                        snapshot.clear();
                    }
                }

                if (!restored) {
                    com.arcadeblocks.gameplay.Brick.resetBrickCounter();
                    com.arcadeblocks.levels.LevelLoader.loadLevel(levelNumber, levelData);
        // System.out.println("Level " + levelNumber + " loaded from JSON: " + levelData.getName());
                }

                // Show countdown timer only if game was restored from save
                if (restored) {
                    showCountdownTimerForLevelStart(levelNumber);
                    playPaddleBallFadeIn(true);
                } else {
                    // If game was not restored, start music immediately
                    playLevelMusic(levelNumber);
                    playPaddleBallFadeIn(false);
        // System.out.println("Level music started");
                }
                
                javafx.util.Duration guardDelay = restored ? javafx.util.Duration.millis(240) : LEVEL_FADE_DURATION;
                FXGL.runOnce(levelReloadGuard, guardDelay);

        // System.out.println("Level " + levelNumber + " fully initialized!");
            } catch (Exception e) {
                levelReloadGuard.run();
                System.err.println("Error initializing level " + levelNumber + ": " + e.getMessage());
                e.printStackTrace();
            }
        };

        ChapterStoryData chapterStory = StoryConfig.findForLevel(levelNumber)
            .filter(data -> alwaysShowChapterStory || (StoryConfig.shouldShowForLevel(levelNumber, new StoryConfig.GameProgress(shownChapterStoryChapters, true)) && shouldShowLevel1Story(levelNumber)))
            .orElse(null);

        if (chapterStory != null) {
            showChapterStory(chapterStory, levelNumber, levelData.getName(), initializeLevelTask, overlayRemover);
            return;
        }
        
        runOverlayRemover(overlayRemover);

        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);

        if (metadata != null && metadata.getIntroVideoPath() != null && shouldShowLevel1StoryAndVideo(levelNumber)) {
            setLevelIntroActive(true);
            playLevelIntroVideo(levelNumber, metadata, initializeLevelTask);
        } else {
            showLevelIntro(levelNumber, levelData.getName(), initializeLevelTask);
        }
    }

    private void showChapterStory(ChapterStoryData storyData, int levelNumber, String levelName, Runnable onComplete, Runnable overlayRemover) {
        clearLetterboxOverlay();

        // КРИТИЧНО: НЕ удаляем видео оверлей здесь!
        // Видео оверлей черный (black), а сюжетное окно полупрозрачное
        // Если удалить видео оверлей, игровой мир будет виден через полупрозрачный фон
        // Поэтому оставляем видео оверлей как черный фон под сюжетным окном
        // И удаляем его только когда закрывается сюжетное окно
        
        ChapterStoryView storyView = new ChapterStoryView(this, storyData, () -> {
            // Удаляем видео оверлей ПОСЛЕ закрытия сюжетного окна
            runOverlayRemover(overlayRemover);
            handleChapterStoryClosed(storyData, levelNumber, levelName, onComplete);
        });
        activeChapterStoryView = storyView;
        setStoryOverlayActive(true);
        FXGL.getGameScene().addUINode(storyView);
    }

    private void handleChapterStoryClosed(ChapterStoryData storyData, int levelNumber, String levelName, Runnable onComplete) {
        if (storyData != null) {
            shownChapterStoryChapters.add(storyData.chapterNumber());
        }
        dismissActiveChapterStoryView();
        setStoryOverlayActive(false);
        
        // КРИТИЧНО: Принудительно очищаем все активные ресурсы после долгого нахождения на сюжетном окне
        // Это предотвращает race condition между cleanup ChapterStoryView и началом видео
        cleanupActiveVideoResources();
        clearUINodesSafely();
        
        // Check if level has intro video after story window closes
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        
        if (metadata != null && metadata.getIntroVideoPath() != null && shouldShowLevel1StoryAndVideo(levelNumber)) {
            // КРИТИЧНО: Добавляем небольшую задержку для завершения cleanup операций
            // Это предотвращает race condition при переходе от сюжета к видео
            Platform.runLater(() -> {
                setLevelIntroActive(true);
                playLevelIntroVideo(levelNumber, metadata, onComplete);
            });
        } else {
            showLevelIntro(levelNumber, levelName, onComplete);
        }
    }
    
    /**
     * Show level loading screen
     */
    private void showLevelIntro(int levelNumber, String levelName, Runnable onComplete) {
        // Remove game world borders during Level Intro display
        clearLetterboxOverlay();
        
        cleanupLevelIntroViews();

        com.arcadeblocks.ui.LevelIntroView levelIntro = new com.arcadeblocks.ui.LevelIntroView(
            levelNumber, levelName, () -> {
                setLevelIntroActive(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        );
        
        // Add a loading screen on top of everything
        FXGL.getGameScene().addUINode(levelIntro);
    }
    
    /**
     * Setting the background image for the level
     */
    private void setLevelBackground(int levelNumber, String backgroundImage) {
        if (saveManager != null && !saveManager.isLevelBackgroundEnabled()) {
            return; // Фон отключен настройкой
        }
        if (backgroundImage == null || backgroundImage.isEmpty()) {
            return;
        }
        
        try {
            // Loading background image
            Image image = ImageCache.get(backgroundImage);
            if (image == null) {
                return;
            }
            
            // Create an ImageView for the background
            javafx.scene.image.ImageView backgroundImageView = new javafx.scene.image.ImageView(image);
            
            // Use the current resolution for the background (as in the main menu and Level Intro)
            // so that the background fills the entire screen, including the letterbox areas
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            backgroundImageView.setFitWidth(currentRes.getWidth());
            backgroundImageView.setFitHeight(currentRes.getHeight());
            backgroundImageView.setPreserveRatio(false);
            backgroundImageView.setSmooth(false);
            backgroundImageView.setCache(false);

            // We do not use letterbox offset - the background should cover the entire screen
            backgroundImageView.setTranslateX(0);
            backgroundImageView.setTranslateY(0);
            backgroundImageView.setLayoutX(0);
            backgroundImageView.setLayoutY(0);
            
            // Add the background as a UI element at the very beginning (so that it is behind all other elements)
            FXGL.getGameScene().getContentRoot().getChildren().add(0, backgroundImageView);
            levelBackgroundNodes.add(backgroundImageView);
            if (darknessOverlayActive) {
                backgroundImageView.setVisible(false);
                backgroundImageView.setOpacity(0.0);
            }
            
        } catch (Exception e) {
            System.err.println("An error occurred while loading the background for the level. " + levelNumber + " (" + backgroundImage + "): " + e.getMessage());
        }
    }
    
    /**
     * Cleans up background images by freeing up VRAM.
     * Goes through all tracked nodes, removes them from ContentRoot, and,
     * most importantly, calls ImageCache.forget() + setImage(null) for each ImageView.
     * This ensures that JavaFX has no live Image references left and the corresponding
     * d3d11 textures are actually freed.
     */
    private void clearLevelBackground() {
        try {
            // Go through all previously added nodes
            for (javafx.scene.Node node : new java.util.ArrayList<>(levelBackgroundNodes)) {
                if (node instanceof javafx.scene.image.ImageView imageView) {
                    // Clear the image from the cache before deleting it.
                    javafx.scene.image.Image image = imageView.getImage();
                    if (image != null) {
                        ImageCache.forget(image);
                    }
                    imageView.setImage(null);
                }
            }
            
            // Remove all ImageView elements from ContentRoot
            var children = FXGL.getGameScene().getContentRoot().getChildren();
            children.removeAll(levelBackgroundNodes);
            levelBackgroundNodes.clear();
            
        // System.out.println("All background images have been cleared.");
        } catch (Exception e) {
            System.err.println("Error clearing background images: " + e.getMessage());
        }
    }

    /**
     * Refresh the level background based on current settings.
     */
    public void refreshLevelBackground() {
        clearLevelBackground();
        if (saveManager != null && !saveManager.isLevelBackgroundEnabled()) {
            // Пользователь отключил фон уровня
            return;
        }
        int currentLevel = FXGL.geti("level");
        com.arcadeblocks.config.LevelConfig.LevelData levelData = getLevelDataUnified(currentLevel);
        if (levelData != null && levelData.getBackgroundImage() != null) {
            setLevelBackground(currentLevel, levelData.getBackgroundImage());
        }
    }

    /**
     * Complete clearing of the active game state to transition to non-game screens
     */
    private void cleanupGameplayState() {
        try {
            stopAllBonuses();
        } catch (Exception ignored) {}

        try {
            stopAllBalls();
        } catch (Exception ignored) {}

        uninstallMousePaddleControlHandlers();
        unblockMouseClicks();

        clearLevelBackground();
        dismissActiveChapterStoryView();
        
        // Clean up active video resources to prevent memory leaks
        cleanupActiveVideoResources();
        
        // Clear darkness overlay to free up VRAM
        if (darknessOverlayCapsule != null && darknessOverlayCapsule.getImage() != null) {
            ImageCache.forget(darknessOverlayCapsule.getImage());
            darknessOverlayCapsule.setImage(null);
        }
        darknessOverlayCapsule = null;
        darknessOverlayGroup = null;
        darknessOverlayDimLayer = null;

        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(this::removeEntitySafely);

        Brick.resetBrickCounter();

        if (gameplayUIView != null) {
            gameplayUIView.cleanup();
            FXGL.getGameScene().removeUINode(gameplayUIView);
        }

        if (bonusTimerView != null) {
            // Clear BonusTimerView before deletion
            bonusTimerView.cleanup();
            FXGL.getGameScene().removeUINode(bonusTimerView);
        }

        if (scoreManager != null) {
            scoreManager.hideBossHealth();
            scoreManager.stopLevelTimer();
        }

        paddleComponent = null;
        // Clearing LivesManager before resetting
        if (livesManager != null) {
            livesManager.cleanup();
        }
        livesManager = null;
        bonusEffectManager = null;
        bonusTimerView = null;
        gameplayUIView = null;
        levelPassBonusEntity = null;
        levelPassBonusSpawned = false;
        mouseClicksBlocked = false;
        isLevelCompleted = false;
        isTransitioning = false;
        levelCompletedMessageShown = false;
        isGameOver = false;
        isLevelIntroActive = false;
        isStoryOverlayActive = false;
        isVictorySequenceActive = false;
        proceedToNextLevelCalled = false;
    }
    
    /**
     * Clearing up active video resources (PauseTransition, VLC players, overlay) to prevent memory leaks
     */
    private void cleanupActiveVideoResources() {
        // Increment the video session token to prevent race conditions.
        // This allows old callbacks to understand that they are outdated and should not perform final cleanup.
        videoSessionToken++;
        
        // Stop and clear all active PauseTransitions
        synchronized (activeVideoPauseTransitions) {
            for (javafx.animation.PauseTransition transition : activeVideoPauseTransitions) {
                try {
                    if (transition != null) {
                        transition.stop();
                    }
                } catch (Exception ignored) {}
            }
            activeVideoPauseTransitions.clear();
        }
        
        // Clear all active video overlays
        synchronized (activeVideoOverlays) {
            for (VideoOverlayWrapper wrapper : activeVideoOverlays) {
                try {
                    if (wrapper != null) {
                        // First, clear the backend before clearing UI elements.
                        // This prevents rendering attempts on already deleted ImageView elements.
                        if (wrapper.backend != null) {
                            try {
                                // System.out.println("Clearing the video backend during cleanupActiveVideoResources...");
                                wrapper.backend.cleanup();
                            } catch (Exception e) {
                                System.err.println("Error while clearing video backend: " + e.getMessage());
                            }
                        }
                        
                        // Now we clean the UI elements
                        javafx.scene.Node overlay = wrapper.overlay;
                        if (overlay != null) {
                            // If the overlay contains ImageView, clear them.
                            if (overlay instanceof javafx.scene.layout.Pane) {
                                javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) overlay;
                                for (javafx.scene.Node child : pane.getChildren()) {
                                    if (child instanceof javafx.scene.image.ImageView) {
                                        javafx.scene.image.ImageView imageView = (javafx.scene.image.ImageView) child;
                                        // Clear the image to free memory
                                        imageView.setImage(null);
                                    }
                                }
                            }
                            
                            // Unbind ResponsiveLayoutHelper listeners
                            if (overlay instanceof javafx.scene.layout.Region) {
                                try {
                                    com.arcadeblocks.ui.util.ResponsiveLayoutHelper.unbind((javafx.scene.layout.Region) overlay);
                                } catch (Exception e) {
                                    System.err.println("Error when unlinking ResponsiveLayoutHelper:" + e.getMessage());
                                }
                            }
                            
                            // Remove from the scene
                            try {
                                FXGL.getGameScene().removeUINode(overlay);
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error during clearing video overlay wrapper: " + e.getMessage());
                }
            }
            activeVideoOverlays.clear();
        }
        
        // Stop and clear all active VLC players
        try {
            com.arcadeblocks.video.VlcContext.getInstance().dispose();
        } catch (Exception e) {
            System.err.println("Error cleaning up VLC resources in cleanupActiveVideoResources: " + e.getMessage());
        }
    }

    private boolean isGameplayState() {
        if (paddleComponent == null || gameplayUIView == null) {
            return false;
        }

        if (!FXGL.getGameScene().getUINodes().contains(gameplayUIView)) {
            return false;
        }

        return !isLoading
            && !isLevelIntroActive
            && !isStoryOverlayActive
            && !isCountdownActive
            && !isLevelCompleted
            && !isTransitioning
            && !isGameOver
            && !isVictorySequenceActive;
    }

    public void enableDarknessOverlay() {
        if (darknessOverlayActive) {
            return;
        }

        ensureDarknessOverlayInitialized();
        updateDarknessOverlayLayout();

        var contentRoot = FXGL.getGameScene().getContentRoot();
        if (!contentRoot.getChildren().contains(darknessOverlayGroup)) {
            contentRoot.getChildren().add(darknessOverlayGroup);
        }

        darknessOverlayGroup.setVisible(true);
        darknessOverlayGroup.toFront();
        darknessOverlayActive = true;
        setLevelBackgroundsVisible(false);
    }

    public void disableDarknessOverlay() {
        var contentRoot = FXGL.getGameScene().getContentRoot();
        boolean removed = contentRoot.getChildren().removeIf(node -> "darknessOverlay".equals(node.getUserData()));

        if (darknessOverlayGroup != null) {
            contentRoot.getChildren().remove(darknessOverlayGroup);
            darknessOverlayGroup.setVisible(false);
            removed = true;
        }

        darknessOverlayActive = false;

        setLevelBackgroundsVisible(true);

        // Clear the image from ImageCache before resetting
        if (darknessOverlayCapsule != null && darknessOverlayCapsule.getImage() != null) {
            ImageCache.forget(darknessOverlayCapsule.getImage());
            darknessOverlayCapsule.setImage(null);
        }
        
        // Clear children from group before resetting
        if (darknessOverlayGroup != null) {
            darknessOverlayGroup.getChildren().clear();
        }
        
        darknessOverlayGroup = null;
        darknessOverlayDimLayer = null;
        darknessOverlayCapsule = null;

        if (removed) {
            FXGL.getGameScene().getRoot().requestLayout();
        }
    }

    private void setLevelBackgroundsVisible(boolean visible) {
        var contentRoot = FXGL.getGameScene().getContentRoot().getChildren();
        levelBackgroundNodes.removeIf(node -> node == null || !contentRoot.contains(node));

        for (Node node : levelBackgroundNodes) {
            node.setVisible(visible);
            if (visible) {
                node.setOpacity(1.0);
            } else {
                node.setOpacity(0.0);
            }
        }
    }

    private void ensureDarknessOverlayInitialized() {
        if (darknessOverlayGroup == null) {
            darknessOverlayDimLayer = new Rectangle();
            darknessOverlayDimLayer.setManaged(false);
            darknessOverlayDimLayer.setMouseTransparent(true);
            darknessOverlayDimLayer.setFill(Color.BLACK);
            darknessOverlayDimLayer.setOpacity(0.72);

            darknessOverlayCapsule = new ImageView();
            darknessOverlayCapsule.setManaged(false);
            darknessOverlayCapsule.setMouseTransparent(true);
            darknessOverlayCapsule.setPreserveRatio(true);
            // Disable anti-aliasing for clear display
            darknessOverlayCapsule.setSmooth(false);
            try {
                Image capsuleImage = ImageCache.get("darkness.png");
                darknessOverlayCapsule.setImage(capsuleImage);
            } catch (Exception e) {
                darknessOverlayCapsule.setImage(null);
            }

            darknessOverlayGroup = new Group(darknessOverlayDimLayer, darknessOverlayCapsule);
            darknessOverlayGroup.setManaged(false);
            darknessOverlayGroup.setMouseTransparent(true);
            darknessOverlayGroup.setUserData("darknessOverlay");
        }

        // Use the current resolution to adapt to 1920x1080
        com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
        darknessOverlayDimLayer.setWidth(currentRes.getWidth());
        darknessOverlayDimLayer.setHeight(currentRes.getHeight());
        darknessOverlayDimLayer.setOpacity(0.72);
    }

    private void updateDarknessOverlayLayout() {
        if (darknessOverlayCapsule != null && darknessOverlayCapsule.getImage() != null) {
            // Use the current resolution to adapt to 1920x1080
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            
            double baseWidth = darknessOverlayCapsule.getImage().getWidth();
            double baseHeight = darknessOverlayCapsule.getImage().getHeight();
            double maxWidth = currentRes.getWidth() * 0.85;
            double scale = Math.min(1.0, maxWidth / baseWidth);
            double finalWidth = baseWidth * scale;
            double finalHeight = baseHeight * scale;

            darknessOverlayCapsule.setFitWidth(finalWidth);
            darknessOverlayCapsule.setFitHeight(finalHeight);
            darknessOverlayCapsule.setLayoutX((currentRes.getWidth() - finalWidth) / 2.0);
            darknessOverlayCapsule.setLayoutY((currentRes.getHeight() - finalHeight) / 2.0);
        }
    }
    
    /**
     * Play music for level
     */
    private void playLevelMusic(int levelNumber) {
        try {
            if (audioManager == null || !audioManager.isInitialized()) {
                System.err.println("Audio manager not initialized, skipping music for level " + levelNumber);
                return;
            }
            
            // Get music from level configuration
            com.arcadeblocks.config.LevelConfig.LevelData levelData = getLevelDataUnified(levelNumber);
            String musicFile = null;
            
            if (levelData != null && levelData.getMusicFile() != null && !levelData.getMusicFile().isEmpty()) {
                musicFile = levelData.getMusicFile();
            } else {
                // Fallback to old system for levels without music in LevelConfig
                switch (levelNumber) {
                    case 0:
                        musicFile = com.arcadeblocks.config.AudioConfig.LEVEL0_MUSIC;
                        break;
                    case 1:
                        musicFile = com.arcadeblocks.config.AudioConfig.LEVEL1_MUSIC;
                        break;
                    default:
        // System.out.println("Music for level " + levelNumber + " not configured");
                        return;
                }
            }
            
            if (musicFile != null) {
        // System.out.println("Starting music for level " + levelNumber + ": " + musicFile);
                audioManager.playMusic(musicFile, true); // true = looped
            }
        } catch (Exception e) {
            System.err.println("Error playing music for level " + levelNumber + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    
    
    /**
     * Apply window settings (fullscreen/windowed mode)
     */
    public void applyWindowSettings() {
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                if (stage != null) {
                    boolean isFullscreen = saveManager.isFullscreen();
                    boolean isWindowed = saveManager.isWindowedMode();
                    // Use current resolution
                    com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
                    int targetW = currentRes.getWidth();
                    int targetH = currentRes.getHeight();
                    
                    // If windowed mode enabled, exit fullscreen
                    if (isWindowed) {
                        stage.setFullScreen(false);
                        stage.setMaximized(false);
                        stage.setResizable(false); // BLOCK manual resizing
                        // Window style remains unchanged (cannot change after showing)
                        // Set selected window resolution
                        stage.setWidth(targetW);
                        stage.setHeight(targetH);
                        stage.centerOnScreen();
                        // Center game world
                        centerGameWorld();
                        // Update letterbox bars only for gameplay
                        updateLetterboxOverlay();
        // System.out.println("Switched to windowed mode");
                    } else if (isFullscreen) {
                        // Fullscreen mode
                        stage.setFullScreen(true);
                        // Center game world
                        centerGameWorld();
                        updateLetterboxOverlay();
        // System.out.println("Switched to fullscreen mode");
                    } else {
                        // If both modes are off, use fullscreen mode by default
                        stage.setFullScreen(true);
                        // Center game world
                        centerGameWorld();
                        updateLetterboxOverlay();
        // System.out.println("Set fullscreen mode by default");
                    }
                }
            } catch (Exception e) {
                // System.err.println("Failed to apply window settings: " + e.getMessage());
            }
        });
    }
    
    /**
     * Apply screen resolution settings
     */
    public void applyResolutionSettings(com.arcadeblocks.config.Resolution newResolution) {
        if (newResolution == null) {
            return;
        }
        
        // Save the new resolution
        GameConfig.setCurrentResolution(newResolution);
        
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                if (stage != null) {
                    // Get the current window mode
                    boolean isFullscreen = stage.isFullScreen();
                    
                    // Set a new resolution
                    stage.setWidth(newResolution.getWidth());
                    stage.setHeight(newResolution.getHeight());
                    
                    // Center the window if it is in windowed mode
                    if (!isFullscreen) {
                        stage.centerOnScreen();
                    }
                    
                    // Center the game world on the screen
                    centerGameWorld();
                    
                    // Update letterbox for new resolution
                    updateLetterboxOverlay();
                    
                    // System.out.println("New resolution applied: " + newResolution);
                }
            } catch (Exception e) {
                System.err.println("Error occured while applying resolution settings: " + e.getMessage());
            }
        });
    }
    
    /**
     * Centers the game world on the screen when changing resolution
     */
    private void centerGameWorld() {
        try {
            // Calculate the offset for centering
            double offsetX = GameConfig.getLetterboxOffsetX();
            double offsetY = GameConfig.getLetterboxOffsetY();
            
            // Use viewport for offset instead of translate (which is bound in FXGL)
            // Negative viewport offset shifts the visible area to the left/up,
            // which visually centers the content
            FXGL.getGameScene().getViewport().setX(-offsetX);
            FXGL.getGameScene().getViewport().setY(-offsetY);
            
            // System.out.println("The game world is centered through the viewport: offsetX=" + offsetX + ", offsetY=" + offsetY);
            
            centerAllUINodes();
            
             // Update the position of the bonus indicator
            if (scoreManager != null) {
                scoreManager.updateBonusIndicatorPosition();
            }
        } catch (Exception e) {
            System.err.println("Game world centering error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh/clear dark areas around the game world (only during gameplay).
     */
    public void updateLetterboxOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                var root = FXGL.getGameScene().getRoot();
                // Removing old overlays
                root.getChildren().removeIf(n -> "letterboxOverlay".equals(n.getUserData()));
                root.getChildren().removeIf(n -> "gameWorldFrame".equals(n.getUserData()));
                
                javafx.stage.Stage stage = FXGL.getPrimaryStage();
                if (stage == null) return;
                
                // Get the current window resolution
                com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
                int windowW = currentRes.getWidth();
                int windowH = currentRes.getHeight();
                
                // The game world is always 1600x900
                int gameWorldW = GameConfig.GAME_WORLD_WIDTH;
                int gameWorldH = GameConfig.GAME_WORLD_HEIGHT;
                
                // If the window resolution is larger than the game world, add semi-transparent fields.
                boolean hasLetterbox = windowW > gameWorldW || windowH > gameWorldH;
                if (hasLetterbox) {
                    // Vertical fields (pillarbox)
                    if (windowW > gameWorldW) {
                        double sideWidth = (windowW - gameWorldW) / 2.0;
                        var leftBox = createLetterboxRegion(sideWidth, windowH);
                        leftBox.setLayoutX(0);
                        leftBox.setLayoutY(0);
                        var rightBox = createLetterboxRegion(sideWidth, windowH);
                        rightBox.setLayoutX(windowW - sideWidth);
                        rightBox.setLayoutY(0);
                        root.getChildren().addAll(leftBox, rightBox);
                    }
                    // Horizontal fields (letterbox)
                    if (windowH > gameWorldH) {
                        double topBottomHeight = (windowH - gameWorldH) / 2.0;
                        var topBox = createLetterboxRegion(windowW, topBottomHeight);
                        topBox.setLayoutX(0);
                        topBox.setLayoutY(0);
                        var bottomBox = createLetterboxRegion(windowW, topBottomHeight);
                        bottomBox.setLayoutX(0);
                        bottomBox.setLayoutY(windowH - topBottomHeight);
                        root.getChildren().addAll(topBox, bottomBox);
                    }
                    
                }

                // Update the position of the bonus indicator
                if (scoreManager != null && scoreManager.getBonusIndicator() != null) {
                    scoreManager.updateBonusIndicatorPosition();
                }
                
                // The background always fills the entire screen, the position does not change
            } catch (Exception ignored) {}
        });
    }

    private javafx.scene.layout.Region createLetterboxRegion(double w, double h) {
        var r = new javafx.scene.layout.Region();
        r.setPrefSize(w, h);
        r.setUserData("letterboxOverlay");
        r.setMouseTransparent(true);
        // Style as in the main menu - semi-transparent dark blue container (80% transparent)
        r.setStyle("-fx-background-color: rgba(26, 26, 46, 0.15);");
        return r;
    }
    
    /**
     * Clear letterbox overlay (frames and panels around the game world)
     */
    public void clearLetterboxOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                var root = FXGL.getGameScene().getRoot();
                root.getChildren().removeIf(n -> "letterboxOverlay".equals(n.getUserData()));
                root.getChildren().removeIf(n -> "gameWorldFrame".equals(n.getUserData()));
            } catch (Exception ignored) {}
        });
    }

    public void clearUINodesSafely() {
        // КРИТИЧНО: освобождаем фоновые слои до удаления UI, чтобы предотвратить накопление ImageView
        clearLevelBackground();

        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
        for (javafx.scene.Node node : uiNodes) {
            removeUINodeSafely(node);
        }
    }

    private void dismissActiveChapterStoryView() {
        if (activeChapterStoryView != null) {
            ChapterStoryView view = activeChapterStoryView;
            activeChapterStoryView = null;
            
            // FIRST remove from scene, THEN cleanup
            // removeUINodeSafely already does the correct order: removeUINode → cleanup
            removeUINodeSafely(view);
        }
        isStoryOverlayActive = false;
    }

    private void removeUINodeSafely(javafx.scene.Node node) {
        if (node == null) {
            return;
        }

        if (node == activeChapterStoryView) {
            activeChapterStoryView = null;
            isStoryOverlayActive = false;
        }

        // CRITICAL: FIRST remove from scene, THEN call cleanup
        // This prevents a race condition where the node is still in the scene but already “dead”
        try {
            FXGL.getGameScene().removeUINode(node);
        } catch (Exception e) {
            System.err.println("Failed to remove UI node " + node.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Now we can safely call cleanup via UINodeCleanup
        try {
            UINodeCleanup.cleanupNode(node);
        } catch (Exception e) {
            System.err.println("Failed to cleanup UI node " + node.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void cleanupLevelIntroViews() {
        try {
            var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
            for (javafx.scene.Node node : uiNodes) {
                if (node instanceof com.arcadeblocks.ui.LevelIntroView) {
                    removeUINodeSafely(node);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup LevelIntroView: " + e.getMessage());
        } finally {
            setLevelIntroActive(false);
        }
    }

    private void runOverlayRemover(Runnable overlayRemover) {
        if (overlayRemover == null) {
            cleanupActiveVideoResources();
            return;
        }

        javafx.application.Platform.runLater(() -> {
            try {
                overlayRemover.run();
            } catch (Exception e) {
                System.err.println("Error executing overlay remover: " + e.getMessage());
            } finally {
                cleanupActiveVideoResources();
            }
        });
    }

    // UI helpers for adaptive resolution (menus, dialogs)
    public double getEffectiveUIWidth() {
        try {
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage != null) {
                double w = stage.getWidth();
                if (w > 0) return Math.max(w, com.arcadeblocks.config.GameConfig.GAME_WIDTH);
            }
        } catch (Exception ignored) {}
        return com.arcadeblocks.config.GameConfig.GAME_WIDTH;
    }

    public double getEffectiveUIHeight() {
        try {
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage != null) {
                double h = stage.getHeight();
                if (h > 0) return Math.max(h, com.arcadeblocks.config.GameConfig.GAME_HEIGHT);
            }
        } catch (Exception ignored) {}
        return com.arcadeblocks.config.GameConfig.GAME_HEIGHT;
    }

    public void centerUINode(javafx.scene.Node node) {
        if (node == null) {
            return;
        }

        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());

        javafx.application.Platform.runLater(() -> centerUINodeInternal(node, offsetX, offsetY));
    }

    private void centerUINodeInternal(javafx.scene.Node node, double offsetX, double offsetY) {
        if (node == null) {
            return;
        }
        Object marker = node.getUserData();
        if ("letterboxOverlay".equals(marker)) {
            return;
        }
        
        // BonusIndicator is positioned manually via ScoreManager
        if ("bonusIndicator".equals(marker)) {
            return;
        }

        if (node instanceof MainMenuView || "fullScreenOverlay".equals(marker)) {
            node.setTranslateX(0);
            node.setTranslateY(0);
            node.setLayoutX(0);
            node.setLayoutY(0);
            return;
        }

        node.setTranslateX(0);
        node.setTranslateY(0);
        double baseWidth = GameConfig.GAME_WORLD_WIDTH;
        double baseHeight = GameConfig.GAME_WORLD_HEIGHT;

        double nodeWidth = calculateNodeWidth(node);
        double nodeHeight = calculateNodeHeight(node);

        double layoutX = offsetX + Math.max(0, (baseWidth - nodeWidth) / 2.0);
        double layoutY = offsetY + Math.max(0, (baseHeight - nodeHeight) / 2.0);

        node.setLayoutX(layoutX);
        node.setLayoutY(layoutY);
    }

    private double calculateNodeWidth(javafx.scene.Node node) {
        double width = 0;

        if (node instanceof Region region) {
            width = region.prefWidth(-1);
            if (Double.isNaN(width) || width <= 0) {
                width = region.getWidth();
            }
            if (width <= 0) {
                width = region.getLayoutBounds().getWidth();
            }
        } else {
            Bounds bounds = node.getLayoutBounds();
            width = bounds.getWidth();
            if (width <= 0) {
                bounds = node.getBoundsInParent();
                width = bounds.getWidth();
            }
        }

        return width > 0 ? width : GameConfig.GAME_WORLD_WIDTH;
    }

    private double calculateNodeHeight(javafx.scene.Node node) {
        double height = 0;

        if (node instanceof Region region) {
            height = region.prefHeight(-1);
            if (Double.isNaN(height) || height <= 0) {
                height = region.getHeight();
            }
            if (height <= 0) {
                height = region.getLayoutBounds().getHeight();
            }
        } else {
            Bounds bounds = node.getLayoutBounds();
            height = bounds.getHeight();
            if (height <= 0) {
                bounds = node.getBoundsInParent();
                height = bounds.getHeight();
            }
        }

        return height > 0 ? height : GameConfig.GAME_WORLD_HEIGHT;
    }

    public void centerAllUINodes() {
        javafx.application.Platform.runLater(() -> {
            try {
                double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
                double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());
                var nodes = FXGL.getGameScene().getUINodes();
                for (javafx.scene.Node n : nodes) {
                    centerUINodeInternal(n, offsetX, offsetY);
                }
            } catch (Exception ignored) {}
        });
    }
    
    /**
     * Exit the game
     */
    public void exitGame() {
        // Set completion flag to prevent new operations
        shutdownTriggered.set(true);
        
        // First, we clear all resources
        performShutdownIfNeeded();
        
        // Clear all UI components
        cleanupAllUINodes();
        
        // We complete all threads before exiting
        shutdownAllExecutors();
        
        // Stop all active animations and timers
        cleanupAllTimelines();
        
        // Remove all event listeners
        cleanupAllEventListeners();
        
        // Clear all active video resources
        cleanupActiveVideoResources();
        
        // Clear all caches
        try {
            FXGL.getAssetLoader().clearCache();
        } catch (Exception ignored) {
        }
        
        try {
            ImageCache.clear();
        } catch (Exception ignored) {
        }
        
        try {
            com.arcadeblocks.util.TextureUtils.clearGameplayTextureCache();
        } catch (Exception ignored) {
        }
        
        // Terminating the JavaFX Application Thread
        try {
            javafx.application.Platform.exit();
        } catch (Exception ignored) {
        }
        
        // Allow time for JavaFX to finish before forcing exit
        try {
            Thread.sleep(100); // 100ms to complete JavaFX
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Forcefully terminate the JVM process immediately.
        // Do not wait, as all resources have already been cleared.
        System.exit(0);
    }
    
    /**
     * Return to main menu (with background change - to return from the game)
     */
    public void returnToMainMenu() {
        boolean wasDebugSession = isDebugMode;

        dismissActiveChapterStoryView();

        if (bonusEffectManager != null) {
            bonusEffectManager.forceDisableDarkness();
        } else {
            disableDarknessOverlay();
        }
        
        // Check if we are in pause mode in the settings
        if (inPauseSettings) {
            // Return to pause instead of main menu
            inPauseSettings = false;
            // CRITICAL: Check if PauseView already exists in the scene
            var scene = FXGL.getGameScene();
            var existingPauseView = scene.getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.PauseView)
                .findFirst();
            
            if (!existingPauseView.isPresent()) {
                // PauseView does not exist, create a new one
                clearUINodesSafely();
                FXGL.getGameScene().addUINode(new PauseView(this));
            } else {
                // PauseView already exists, just clear other UI nodes
                clearUINodesSafely();
            }
            return;
        }
        
        // Check if PauseView already exists in the scene before going to the main menu.
        // This prevents a new PauseView from being created if the old one has not yet been deleted.
        var scene = FXGL.getGameScene();
        var existingPauseView = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.PauseView)
            .findFirst();
        
        if (existingPauseView.isPresent()) {
            // PauseView still exists, delete it before going to the main menu
            ((com.arcadeblocks.ui.PauseView) existingPauseView.get()).cleanup();
            removeUINodeSafely(existingPauseView.get());
        }
        
        // Reset the pause flag before going to the main menu
        isPaused = false;

        if (saveManager != null) {
            if (!wasDebugSession && paddleComponent != null) {
                saveCurrentGameSnapshot();
                saveManager.autoSaveToActiveSlot();
                saveManager.awaitPendingWrites();
            } else {
                saveManager.clearGameSnapshot();
            }
        }

        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
        
        // Reset the level completion and transition flags
        isLevelCompleted = false;
        isTransitioning = false;
        
        if (scoreManager != null) {
            if (wasDebugSession) {
                scoreManager.setPersistenceEnabled(false);
            }
            scoreManager.stopLevelTimer();
            if (!wasDebugSession) {
                scoreManager.flushPendingOperations();
            }
        }
        
        paddleComponent = null;
        // Clear LivesManager before resetting
        if (livesManager != null) {
            livesManager.cleanup();
        }
        livesManager = null;
        bonusEffectManager = null;
        bonusTimerView = null;
        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
        
        // Cleaning all entities (safe)
        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(Entity::removeFromWorld);
        
        // CRITICAL: Free up background images before rebuilding the menu
        releaseLevelBackground();
        
        // Keep the current background of the main menu (do not change it when returning from the game)
        String currentBackground = getCurrentMainMenuBackground();
        setCurrentMainMenuBackground(currentBackground);
        
        // Clear the letterbox overlay before displaying the main menu
        clearLetterboxOverlay();
        
        // Clearing the UI and displaying the main menu (the background should NOT change when returning from the game)
        clearUINodesSafely();
        MainMenuView mainMenuView = new MainMenuView(this, false, currentBackground);
        FXGL.getGameScene().addUINode(mainMenuView);
        
        // КРИТИЧНО: Запрашиваем фокус для MainMenuView
        javafx.application.Platform.runLater(() -> {
            mainMenuView.restoreFocus();
        });
        
        // Set the system cursor
        setSystemCursor();
        
        if (wasDebugSession) {
            resetDebugMode();
        }
        
        // Play music from the main menu or restore the previous one
        if (audioManager != null) {
            // First, we try to restore the previous music (if it was saved)
            boolean restored = audioManager.restorePreviousMusic();
            
            // If the previous music was not restored, play random music from the main menu
            if (!restored || !audioManager.isMusicPlaying()) {
                // Stop any currently playing music (including background music)
                audioManager.stopMusic();
                // System.out.println("Music stopped when returning to the main menu");
                
                // Определяем состояние прогресса игры для выбора правильной музыки
                com.arcadeblocks.config.AudioConfig.GameProgressState progressState = 
                    com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
                
                if (saveManager != null) {
                    int maxLevel = 0;
                    boolean gameCompleted = false;
                    
                    // Проверяем все слоты сохранения
                    for (int slot = 1; slot <= 4; slot++) {
                        com.arcadeblocks.utils.SaveManager.SaveInfo saveInfo = saveManager.getSaveInfo(slot);
                        if (saveInfo != null) {
                            if (saveInfo.level > maxLevel) {
                                maxLevel = saveInfo.level;
                            }
                            if (saveManager.isGameCompletedInSlot(slot)) {
                                gameCompleted = true;
                            }
                        }
                    }
                    
                    // Определяем состояние прогресса
                    if (gameCompleted) {
                        progressState = com.arcadeblocks.config.AudioConfig.GameProgressState.COMPLETED;
                    } else if (maxLevel >= 101) {
                        progressState = com.arcadeblocks.config.AudioConfig.GameProgressState.AFTER_LEVEL_100;
                    }
                }
                
                String randomMainMenuMusic = com.arcadeblocks.config.AudioConfig.getRandomMainMenuMusic(progressState);
                audioManager.playMusic(randomMainMenuMusic, true);
                // System.out.println("Playing random music from the main menu: " + randomMainMenuMusic + " (back to menu));
            } else {
                // System.out.println("Previous main menu music restored);
            }
        }
        
        // Display database statistics at startup
        if (saveManager != null) {
            saveManager.printDatabaseStats();
        }
    }
    
    // Custom method for processing output (called from dialogs)
    public void handleGameExit() {
        // System.out.println("handleGameExit called!"); // Debugging

        if (isLoading || isLevelIntroActive || isStoryOverlayActive || isCountdownActive || isLevelCompleted || isTransitioning) {
            return;
        }
        
        // CRITICAL: Set completion flag to prevent new operations
        shutdownTriggered.set(true);
        
        // CRITICAL: Stop all active operations
        try {
            if (scoreManager != null) {
                scoreManager.stopLevelTimer();
                scoreManager.flushPendingOperations();
            }
        } catch (Exception ignored) {
        }
        
        // Close the connection to the database
        if (saveManager != null) {
            try {
                saveCurrentGameSnapshot();
            } catch (Exception ignored) {
            }
            try {
                saveManager.close();
            } catch (Exception ignored) {
            }
            saveManager = null;
        }
        
        // First, we clear all resources
        performShutdownIfNeeded();
        
        // Clear all UI components
        cleanupAllUINodes();
        
        // Complete all threads before exiting
        shutdownAllExecutors();
        
        // Stop all active animations and timers
        cleanupAllTimelines();
        
        // Remove all event listeners
        cleanupAllEventListeners();
        
        // Clear all active video resources
        cleanupActiveVideoResources();
        
        // Clear all caches
        try {
            FXGL.getAssetLoader().clearCache();
        } catch (Exception ignored) {
        }
        
        try {
            ImageCache.clear();
        } catch (Exception ignored) {
        }
        
        try {
            com.arcadeblocks.util.TextureUtils.clearGameplayTextureCache();
        } catch (Exception ignored) {
        }
        
        // Terminate the JavaFX Application Thread
        try {
            javafx.application.Platform.exit();
        } catch (Exception ignored) {
        }
        
        // Allow time for JavaFX to finish before forcing exit
        try {
            Thread.sleep(100); // 100ms to complete JavaFX
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Forcefully terminate the JVM process immediately.
        // Do not wait, as all resources have already been cleared.
        System.exit(0);
    }
    
    private Runnable beginLevelReloadGuard() {
        suppressLevelCompletionChecks = true;
        AtomicBoolean released = new AtomicBoolean(false);
        return () -> {
            if (released.compareAndSet(false, true)) {
                suppressLevelCompletionChecks = false;
                boolean noBricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).isEmpty()
                    && Brick.getActiveBrickCount() == 0;
                if (noBricks && !isTransitioning && !isLevelCompleted) {
                    FXGL.runOnce(this::checkLevelCompletion, javafx.util.Duration.millis(100));
                }
            }
        };
    }

    /**
     * Level completion check
     */
    public void checkLevelCompletion() {
        if (isLevelCompleted || isTransitioning || isGameOver) {
            return;
        }

        var remainingBricks = FXGL.getGameWorld()
            .getEntitiesByType(EntityType.BRICK)
            .stream()
            .filter(entity -> {
                Brick brick = entity.getComponentOptional(Brick.class).orElse(null);
                return brick == null || brick.countsForCompletion();
            })
            .toList();

        if (remainingBricks.isEmpty() && Brick.getActiveBrickCount() == 0) {
            int currentLevel = FXGL.geti("level");
            int currentScore = scoreManager != null ? scoreManager.getCurrentScore() : 0;
            int currentLives = FXGL.geti("lives");
            double pauseSeconds = getLevelCompletionPauseSeconds(currentLevel);

            // We check again after a short delay to eliminate window collapse artifacts.
            FXGL.runOnce(() -> confirmLevelCompletion(currentLevel, currentScore, currentLives, pauseSeconds),
                javafx.util.Duration.millis(120));
        } else {
            // System.out.println("There are still bricks left in ArcadeBlocksApp: " + remainingBricks.size());
        }
    }

    private void confirmLevelCompletion(int currentLevel, int currentScore, int currentLives, double pauseSeconds) {
        if (suppressLevelCompletionChecks || isLevelCompleted || isTransitioning) {
            return;
        }

        boolean hasRemainingBricks = FXGL.getGameWorld()
            .getEntitiesByType(EntityType.BRICK)
            .stream()
            .anyMatch(entity -> {
                Brick brick = entity.getComponentOptional(Brick.class).orElse(null);
                return brick == null || brick.countsForCompletion();
            });

        if (hasRemainingBricks || Brick.getActiveBrickCount() > 0) {
            return;
        }

        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(currentLevel);

        if (saveManager != null) {
            saveManager.clearGameSnapshot();
        }

        isLevelCompleted = true;
        isTransitioning = true;

        if (scoreManager != null) {
            scoreManager.stopLevelTimer();
        }

        blockPaddleControl();
        blockMouseClicks();
        uninstallMousePaddleControlHandlers();
        stopAllBonusesExceptStickyPaddle();
        stopAllBalls();
        playPaddleBallFadeOut(null);

        boolean finalVictory = isFinalVictoryLevel(currentLevel);
        
        // Проверяем, есть ли видео завершения
        boolean hasCompletionVideo = metadata != null
            && metadata.getCompletionVideoPath() != null
            && !metadata.getCompletionVideoPath().isBlank();
        
        // Проверяем, нужно ли показывать финальное видео с поэмой (уровень 100)
        boolean shouldPlayFinalVictorySequence = finalVictory
            && hasCompletionVideo
            && metadata.shouldShowPoemAfterVictory();
        
        // Проверяем, нужно ли показывать финальное видео без поэмы (уровень 116)
        boolean shouldPlayFinalVictoryVideo = finalVictory
            && hasCompletionVideo
            && !metadata.shouldShowPoemAfterVictory();
        
        // Обычное видео босса (не финальная победа)
        boolean shouldPlayCompletionVideo = !finalVictory && hasCompletionVideo;

        String completionSound = getCurrentLevelCompleteSound();

        if (audioManager != null) {
            audioManager.stopMusic();
            if (!shouldPlayCompletionVideo && !shouldPlayFinalVictorySequence && !shouldPlayFinalVictoryVideo && completionSound != null) {
                audioManager.playSFX(completionSound);
            }
        }

        if (shouldPlayFinalVictorySequence) {
            // Уровень с поэмой (например, уровень 100) - показываем финальное видео, затем поэму
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    playFinalVictorySequence(metadata);
                }
            });
        } else if (shouldPlayFinalVictoryVideo) {
            // Финальная победа с видео но без поэмы (например, уровень 116) - показываем видео, затем титры
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    playBossCompletionVideo(metadata, completionSound,
                        remover -> proceedToNextLevel(currentLevel, currentScore, currentLives, remover));
                }
            });
        } else if (finalVictory) {
            // Финальная победа без видео и поэмы - просто переходим к следующему экрану
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    proceedToNextLevel(currentLevel, currentScore, currentLives);
                }
            });
        } else if (shouldPlayCompletionVideo) {
            // Обычное видео босса
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    playBossCompletionVideo(metadata, completionSound, remover -> {
                        showLevelCompletionMessage(currentLevel, currentScore, currentLives,
                            () -> proceedToNextLevel(currentLevel, currentScore, currentLives, remover));
                    });
                }
            });
        } else {
            // Обычный уровень без видео
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    showLevelCompletionMessage(currentLevel, currentScore, currentLives,
                        () -> proceedToNextLevel(currentLevel, currentScore, currentLives));
                }
            });
        }
    }

    public void onBrickCountChanged(int remaining) {
        if (levelPassBonusSpawned || isLevelCompleted || isTransitioning) {
            return;
        }

        int currentLevel = FXGL.geti("level");
        if (isSpecialLevel(currentLevel)) {
            return;
        }

        if (!BonusConfig.isBonusEnabled("level_pass")) {
            return;
        }

        int trackedRemaining = Brick.getActiveBrickCount();

        if (trackedRemaining <= 0) {
            FXGL.runOnce(this::checkLevelCompletion, Duration.millis(60));
            return;
        }

        // Сопоставляем с фактическим числом разрушаемых кирпичей в мире, чтобы не спавнить бонус преждевременно
        int worldBreakableRemaining = (int) FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).stream()
            .map(e -> e.getComponentOptional(Brick.class).orElse(null))
            .filter(b -> b != null && b.getHealth() > 0)
            .count();

        if (worldBreakableRemaining != trackedRemaining) {
            return;
        }

        // Спавним только когда осталось ровно 3 разрушаемых кирпича
        if (trackedRemaining != 3) {
            return;
        }

        spawnLevelPassBonus();
    }

    public void onBossDefeatSequenceStarted(Boss boss) {
        if (isTransitioning) {
            return;
        }
        isLevelCompleted = true;
        isTransitioning = true;
        levelCompletedMessageShown = false;
        proceedToNextLevelCalled = false;
        
        if (scoreManager != null) {
            scoreManager.stopLevelTimer();
        }

        if (saveManager != null) {
            saveManager.clearGameSnapshot();
        }

        blockPaddleControl();
        stopAllBonusesExceptStickyPaddle();
        stopAllBalls();

        if (audioManager != null) {
            audioManager.stopMusic();
        }
    }

    private boolean isSpecialLevel(int levelNumber) {
        return com.arcadeblocks.config.LevelConfig.isBossLevel(levelNumber);
    }

    private boolean isFinalVictoryLevel(int levelNumber) {
        // Проверяем метаданные уровня на флаг showPoemAfterVictory
        // Это позволяет уровню 100 быть финальной победой с поэмой,
        // даже если это не последний уровень в игре
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        if (metadata != null && metadata.shouldShowPoemAfterVictory()) {
            return true;
        }
        // Также считаем финальной победой последний уровень текущей игровой линии
        GameLine gameLine = GameLine.fromLevel(levelNumber);
        return levelNumber >= gameLine.getEndLevel();
    }

    private void spawnLevelPassBonus() {
        levelPassBonusSpawned = true;

        if (levelPassBonusEntity != null && levelPassBonusEntity.isActive()) {
            levelPassBonusEntity.removeFromWorld();
        }

        double bonusWidth = 81;
        double spawnX = (GameConfig.GAME_WIDTH - bonusWidth) / 2.0;
        double spawnY = GameConfig.TOP_UI_HEIGHT + 20;

        SpawnData data = new SpawnData(spawnX, spawnY);
        data.put("bonusType", BonusType.LEVEL_PASS);

        levelPassBonusEntity = FXGL.spawn("bonus", data);
        if (levelPassBonusEntity != null) {
            Bonus bonusComponent = new Bonus();
            bonusComponent.setBonusType(BonusType.LEVEL_PASS);
            bonusComponent.setFallSpeed(72.0); // ~1.2px per frame @60fps normalized
            levelPassBonusEntity.addComponent(bonusComponent);
        }
    }

    public void completeLevelViaBonus() {
        if (isLevelCompleted || isTransitioning) {
            return;
        }

        levelPassBonusSpawned = true;
        
        // КРИТИЧНО: НЕ удаляем levelPassBonusEntity здесь - пусть анимация collect() завершится естественно
        // Это предотвращает утечку памяти от незавершенной анимации бонуса
        levelPassBonusEntity = null; // Обнуляем ссылку, но entity удалится через collect() анимацию

        // КРИТИЧНО: Сначала запускаем плавное исчезновение ракетки и мяча
        // как при обычном завершении уровня
        playPaddleBallFadeOut(() -> {
            // КРИТИЧНО: Удаляем кирпичи ПОСЛЕ завершения fade анимации
            // Это синхронизирует визуальное завершение уровня
            var bricks = new java.util.ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK));
            for (Entity brick : bricks) {
                brick.removeFromWorld();
            }
            Brick.resetBrickCounter();

            // Проверяем завершение уровня после удаления кирпичей
            FXGL.runOnce(this::checkLevelCompletion, Duration.millis(50));
        });
    }

    public void onBossDefeated(Boss boss) {
        if (proceedToNextLevelCalled) {
            return;
        }

        int currentLevel = FXGL.geti("level");
        int currentScore = scoreManager != null ? scoreManager.getCurrentScore() : 0;
        int currentLives = FXGL.geti("lives");

        if (scoreManager != null) {
            scoreManager.stopLevelTimer();
            scoreManager.hideBossHealth();
        }

        boolean finalVictory = isFinalVictoryLevel(currentLevel);
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(currentLevel);
        boolean shouldPlayCompletionVideo = !finalVictory
            && metadata != null
            && metadata.getCompletionVideoPath() != null
            && !metadata.getCompletionVideoPath().isBlank();
        boolean shouldShowCompletionMessage = !finalVictory && !shouldPlayCompletionVideo;

        String completionSound = getCurrentLevelCompleteSound();
        if (audioManager != null) {
            audioManager.stopMusic();
            if (!shouldPlayCompletionVideo && completionSound != null) {
                audioManager.playSFX(completionSound);
            }
        }

        Runnable continueFlow = () -> {
            if (shouldShowCompletionMessage) {
                showLevelCompletionMessage(currentLevel, currentScore, currentLives,
                    () -> proceedToNextLevel(currentLevel, currentScore, currentLives));
            } else {
                proceedToNextLevel(currentLevel, currentScore, currentLives);
            }
        };

        if (shouldPlayCompletionVideo) {
            playBossCompletionVideo(metadata, completionSound, remover -> {
                showLevelCompletionMessage(currentLevel, currentScore, currentLives,
                    () -> proceedToNextLevel(currentLevel, currentScore, currentLives, remover));
            });
        } else {
            continueFlow.run();
        }
    }
    
    private void handleBossProjectileHit(Projectile projectileComponent, Entity projectileEntity) {
        projectileEntity.removeFromWorld();

        if (livesManager != null) {
            livesManager.loseLife();
        } else {
            onLifeLost();
            int remainingLives = Math.max(0, FXGL.geti("lives") - 1);
            FXGL.set("lives", remainingLives);
        }

        var bosses = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS);
        for (Entity bossEntity : bosses) {
            Boss boss = bossEntity.getComponentOptional(Boss.class).orElse(null);
            if (boss != null) {
                boss.restoreFullHealth();
            }
        }
    }
    
    /**
     * Public method for safely freeing the background layer.
     * Used by GameOverView and other UI components to free VRAM
     * before switching scenes.
     */
    public void releaseLevelBackground() {
        javafx.application.Platform.runLater(() -> {
            clearLevelBackground();
        });
    }
    
    /**
     * Stop all balls without removing them
     */
    private void stopAllBalls() {
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.pauseForCountdown();
            }
        }
        // System.out.println("Balls stopped at the end of the level: " + balls.size() + " balls");
    }
    
    private void resumeAllBalls() {
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.resumeAfterCountdown();
            }
        }
    }

    public boolean isLaunchLocked() {
        return launchLocked;
    }

    private long lockBallLaunchInternal() {
        launchLockToken++;
        launchLocked = true;
        return launchLockToken;
    }

    private void unlockBallLaunchInternal(long token) {
        if (launchLockToken == token) {
            launchLocked = false;
        }
    }

    private void forceUnlockBallLaunch() {
        launchLockToken++;
        launchLocked = false;
    }

    private List<Node> collectEntityViewNodes(Entity entity) {
        List<Node> nodes = new ArrayList<>();
        if (entity == null || entity.getViewComponent() == null) {
            return nodes;
        }
        entity.getViewComponent().getChildren().forEach(child -> {
            if (child != null) {
                nodes.add(child);
            }
        });
        return nodes;
    }

    private void resetFadeState() {
        // КРИТИЧНО: Останавливаем все активные FadeTransition перед сбросом состояния
        synchronized (activeFadeTransitions) {
            for (FadeTransition fade : activeFadeTransitions) {
                try {
                    if (fade != null) {
                        fade.stop();
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки остановки
                }
            }
            activeFadeTransitions.clear();
        }
        
        fadeOutActive = false;
        fadeOutCompleted = false;
        fadeOutCallbacks.clear();
    }

    private void playPaddleBallFadeOut(Runnable onFinished) {
        if (fadeOutCompleted) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        if (onFinished != null) {
            fadeOutCallbacks.add(onFinished);
        }
        if (fadeOutActive) {
            return;
        }
        fadeOutActive = true;

        javafx.application.Platform.runLater(() -> {
            List<Node> nodes = new ArrayList<>();
            Entity paddle = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE).stream().findFirst().orElse(null);
            nodes.addAll(collectEntityViewNodes(paddle));
            FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)
                .forEach(ball -> nodes.addAll(collectEntityViewNodes(ball)));

            if (nodes.isEmpty()) {
                fadeOutActive = false;
                fadeOutCompleted = true;
                runFadeOutCallbacks();
                return;
            }

            AtomicInteger remaining = new AtomicInteger(nodes.size());
            for (Node node : nodes) {
                FadeTransition fade = new FadeTransition(LEVEL_FADE_DURATION, node);
                fade.setToValue(0.0);
                
                // КРИТИЧНО: Добавляем анимацию в список для отслеживания
                synchronized (activeFadeTransitions) {
                    activeFadeTransitions.add(fade);
                }
                
                fade.setOnFinished(evt -> {
                    // КРИТИЧНО: Удаляем анимацию из списка после завершения
                    synchronized (activeFadeTransitions) {
                        activeFadeTransitions.remove(fade);
                    }
                    
                    if (remaining.decrementAndGet() == 0) {
                        fadeOutActive = false;
                        fadeOutCompleted = true;
                        runFadeOutCallbacks();
                    }
                });
                fade.play();
            }
        });
    }

    public void fadeOutPaddleAndBalls(boolean skipFade, Runnable onFinished) {
        if (skipFade) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        resetFadeState();
        playPaddleBallFadeOut(onFinished);
    }

    private void runFadeOutCallbacks() {
        List<Runnable> callbacks = new ArrayList<>(fadeOutCallbacks);
        fadeOutCallbacks.clear();
        callbacks.forEach(Runnable::run);
    }

    /**
     * Deferred execution after fadeOutBonuses to prevent “Physics world is locked” errors
     * All exit paths from fadeOutBonuses() now use this method, which schedules execution
     * via FXGL.runOnce(..., Duration.ZERO), ensuring that the code runs on the next frame,
     * safely outside of the active physics step.
     */
    private void runAfterBonusesFade(Runnable after) {
        if (after != null) {
            FXGL.runOnce(after, javafx.util.Duration.ZERO);
        }
    }
    
    public void fadeOutBonuses(boolean skipFade, Runnable after) {
        if (skipFade) {
            runAfterBonusesFade(after);
            return;
        }

        List<Entity> bonusEntities = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
        if (bonusEntities.isEmpty()) {
            runAfterBonusesFade(after);
            return;
        }

        List<Node> bonusNodes = new ArrayList<>();
        for (Entity bonus : bonusEntities) {
            bonusNodes.addAll(collectEntityViewNodes(bonus));
        }

        if (bonusNodes.isEmpty()) {
            runAfterBonusesFade(after);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(bonusNodes.size());
        javafx.application.Platform.runLater(() -> {
            for (Node node : bonusNodes) {
                FadeTransition fade = new FadeTransition(LEVEL_FADE_DURATION, node);
                fade.setToValue(0.0);
                fade.setOnFinished(evt -> {
                    if (remaining.decrementAndGet() == 0) {
                        runAfterBonusesFade(after);
                    }
                });
                fade.play();
            }
        });
    }
    
    /**
     * Smooth removal of the ball with fade out animation
     */
    private void fadeOutBall(Entity ball) {
        if (ball == null || !ball.isActive()) {
            return;
        }
        
        // We check whether the removal animation for this ball has already been started.
        if (ball.getProperties().exists("isFadingOut")) {
            Boolean isFadingOut = ball.getBoolean("isFadingOut");
            if (Boolean.TRUE.equals(isFadingOut)) {
                return; // The animation has already started.
            }
        }
        
        ball.setProperty("isFadingOut", true);
        
        List<Node> ballNodes = collectEntityViewNodes(ball);
        if (ballNodes.isEmpty()) {
            // If there are no nodes for animation, delete them immediately.
            ball.removeFromWorld();
            return;
        }
        
        // Stop the ball's physics so that it does not move during animation
        com.almasb.fxgl.physics.PhysicsComponent physics = ball.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
        if (physics != null) {
            physics.setLinearVelocity(0, 0);
            physics.setBodyType(com.almasb.fxgl.physics.box2d.dynamics.BodyType.STATIC);
        }
        
        javafx.application.Platform.runLater(() -> {
            AtomicInteger remaining = new AtomicInteger(ballNodes.size());
            for (Node node : ballNodes) {
                FadeTransition fade = new FadeTransition(Duration.millis(300), node);
                fade.setFromValue(node.getOpacity());
                fade.setToValue(0.0);
                fade.setOnFinished(evt -> {
                    if (remaining.decrementAndGet() == 0) {
                        // Remove the ball from the world after the animation ends
                        if (ball.isActive()) {
                            ball.removeFromWorld();
                        }
                    }
                });
                fade.play();
            }
        });
    }

    private void playPaddleBallFadeIn(boolean skipFade) {
        boolean skipLock = bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive();
        if (skipFade) {
            // КРИТИЧНО: При пропуске анимации сразу разблокируем запуск и клики мыши
            forceUnlockBallLaunch();
            unblockMouseClicks();
            Entity paddle = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE).stream().findFirst().orElse(null);
            collectEntityViewNodes(paddle).forEach(node -> node.setOpacity(1.0));
            FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)
                .forEach(ball -> collectEntityViewNodes(ball).forEach(node -> node.setOpacity(1.0)));
            return;
        }

        Entity paddle = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE).stream().findFirst().orElse(null);
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(collectEntityViewNodes(paddle));
        FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)
            .forEach(ball -> nodes.addAll(collectEntityViewNodes(ball)));

        if (nodes.isEmpty()) {
            // КРИТИЧНО: Всегда разблокируем запуск и клики мыши, даже если нет нод для анимации
            forceUnlockBallLaunch();
            unblockMouseClicks();
            if (paddleComponent != null) {
                paddleComponent.setMovementBlocked(false);
            }
            return;
        }

        long lockToken = skipLock ? -1L : lockBallLaunchInternal();
        if (paddleComponent != null) {
            paddleComponent.setMovementBlocked(true);
        }

        AtomicInteger remaining = new AtomicInteger(nodes.size());
        javafx.application.Platform.runLater(() -> {
            for (Node node : nodes) {
                node.setOpacity(0.0);
                FadeTransition fade = new FadeTransition(LEVEL_FADE_DURATION, node);
                fade.setToValue(1.0);
                fade.setOnFinished(evt -> {
                    if (remaining.decrementAndGet() == 0) {
                        // КРИТИЧНО: Всегда разблокируем движение ракетки после анимации
                        // Не восстанавливаем предыдущее состояние, так как оно может быть заблокированным
                        if (paddleComponent != null) {
                            paddleComponent.setMovementBlocked(false);
                        }
                        // КРИТИЧНО: Добавляем задержку 500мс перед разблокировкой запуска мяча вручную
                        // Это дает игроку время подготовиться после возрождения
                        // КРИТИЧНО: Используем forceUnlockBallLaunch для гарантированной разблокировки
                        // Это предотвращает баг, когда мяч нельзя запустить вручную после возрождения
                        FXGL.runOnce(() -> {
                            forceUnlockBallLaunch();
                            // КРИТИЧНО: Разблокируем клики мыши для возможности запуска мяча
                            // Без этого canProcessLaunchInput() всегда возвращает false
                            unblockMouseClicks();
                        }, Duration.millis(500));
                    }
                });
                fade.play();
            }
        });
    }

    private void enforcePauseFreeze() {
        stopAllBalls();
        FXGL.getGameController().pauseEngine();
        if (scoreManager != null) {
            scoreManager.pauseLevelTimer();
        }
        if (bonusEffectManager != null) {
            bonusEffectManager.pauseAllBonusTimers();
        }
    }
    
    /**
     * Paddle control lock
     */
    private void blockPaddleControl() {
        if (paddleComponent != null) {
            paddleComponent.setMoveLeft(false);
            paddleComponent.setMoveRight(false);
            paddleComponent.setTurboMode(false);
            paddleComponent.setMovementBlocked(true);
            // System.out.println("Paddle control is blocked");
        }
    }
    
    /**
     * Blocking mouse clicks
     */
    public void blockMouseClicks() {
        mouseClicksBlocked = true;
        // System.out.println("Mouse clicks are blocked");
    }
    
    /**
     * Unlocking mouse clicks
     */
    public void unblockMouseClicks() {
        mouseClicksBlocked = false;
        // System.out.println("Mouse clicks are unlocked");
    }
    
    /**
     * All bonuses and effects are stopped
     */
    private void stopAllBonuses() {
        if (bonusEffectManager != null) {
            bonusEffectManager.resetAllEffects();
        }

        fadeOutBonuses(false, () -> {
            var bonuses = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
            for (Entity bonusEntity : bonuses) {
                bonusEntity.removeFromWorld();
            }

            if (!bonuses.isEmpty()) {
                // System.out.println("Deleted " + bonuses.size() + " falling bonuses");
            }

            levelPassBonusEntity = null;
        });
        
        // System.out.println("All bonuses and effects are stopped");
    }
    
    /**
     * All bonuses and effects are stopped, except for racket stickiness.
     */
    private void stopAllBonusesExceptStickyPaddle() {
        if (bonusEffectManager != null) {
            bonusEffectManager.resetAllEffectsExceptStickyPaddle();
        }

        fadeOutBonuses(false, () -> {
            var bonuses = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
            for (Entity bonusEntity : bonuses) {
                bonusEntity.removeFromWorld();
            }

            if (!bonuses.isEmpty()) {
                // System.out.println("Deleted " + bonuses.size() + " falling bonuses");
            }

            // System.out.println("All bonuses and effects are disabled (except for racket stickiness).");

            levelPassBonusEntity = null;
            levelPassBonusSpawned = false;
        });
    }

    public void onAllBonusesCleared() {
        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
    }
    
    public void setInPauseSettings(boolean inPauseSettings) {
        this.inPauseSettings = inPauseSettings;
    }
    
    /**
     * Check whether we are in pause settings
     */
    public boolean isInPauseSettings() {
        return inPauseSettings;
    }
    
    /**
     * Сохранить snapshot перед входом в настройки паузы
     * Это предотвращает утечки памяти при изменении настроек
     */
    public void saveSnapshotBeforePauseSettings() {
        if (saveManager != null && !inPauseSettings) {
            saveCurrentGameSnapshot();
        }
    }
    
    /**
     * Сохранить все настройки при выходе из настроек паузы
     * Вызывается один раз вместо сохранения при каждом изменении
     */
    private void saveAllPauseSettings() {
        // Метод вызывается из returnToPauseFromSettings
        // Настройки уже применены через listeners, просто ждем завершения записи
        if (saveManager != null) {
            saveManager.awaitPendingWrites();
        }
    }
    
    /**
     * Return to pause from settings
     */
    public void returnToPauseFromSettings() {
        var scene = FXGL.getGameScene();
        
        // КРИТИЧНО: Сначала находим SettingsView, сохраняем настройки
        // ДО того, как сбросим флаг inPauseSettings и удалим узел
        SettingsView settingsView = null;
        for (Node node : new ArrayList<Node>(scene.getUINodes())) {
            if (node instanceof SettingsView) {
                settingsView = (SettingsView) node;
                break;
            }
        }
        
        // Сохраняем настройки синхронно, чтобы гарантировать их сохранение
        if (settingsView != null) {
            settingsView.saveAllPauseSettings();
        }
        
        // КРИТИЧНО: Сбрасываем флаг inPauseSettings
        inPauseSettings = false;
        
        // КРИТИЧНО: Удаляем ВСЕ экземпляры SettingsView и диалоги из сцены
        var nodesToRemove = new ArrayList<Node>();
        for (Node node : scene.getUINodes()) {
            if (node instanceof SettingsView
                || node instanceof com.arcadeblocks.ui.ResetSettingsDialog
                || node instanceof com.arcadeblocks.ui.ControlsResetDialog
                || node instanceof com.arcadeblocks.ui.UnsavedChangesDialog) {
                nodesToRemove.add(node);
            }
        }
        
        // Удаляем все найденные узлы БЕЗ вызова cleanup (cleanup будет вызван асинхронно)
        for (Node node : nodesToRemove) {
            try {
                scene.removeUINode(node);
            } catch (Exception e) {
                System.err.println("Error removing UI node: " + e.getMessage());
            }
        }
        
        // КРИТИЧНО: Показываем экран паузы СРАЗУ для мгновенного отклика
        showPauseScreen();
        
        // КРИТИЧНО: Вызываем cleanup() с БОЛЬШОЙ задержкой, чтобы дать время на рендеринг PauseView
        // Это позволяет пользователю увидеть экран паузы ДО того, как начнется GC
        final SettingsView viewToCleanup = settingsView;
        new Thread(() -> {
            try {
                // Задержка 500мс - даем время на рендеринг PauseView
                Thread.sleep(500);
                
                // Вызываем cleanup в JavaFX потоке
                Platform.runLater(() -> {
                    if (viewToCleanup != null) {
                        viewToCleanup.cleanup();
                    }
                    // Очищаем другие узлы
                    for (Node node : nodesToRemove) {
                        if (node instanceof SettingsView && node != viewToCleanup) {
                            ((SettingsView) node).cleanup();
                        }
                    }
                    nodesToRemove.clear();
                });
            } catch (InterruptedException ignored) {}
        }, "Delayed-Cleanup-Thread").start();
        
        // КРИТИЧНО: НЕ вызываем System.gc() здесь, так как это может вызывать лаги
        // Сборка мусора произойдет автоматически, когда это необходимо
        
        // КРИТИЧНО: Тяжелые операции выполняем асинхронно в фоне
        Platform.runLater(() -> {
            // Запускаем очистку памяти асинхронно
            if (saveManager != null) {
                saveManager.runAfterPendingWrites(() -> {
                    if (saveManager != null) {
                        saveManager.forceMemoryCleanup();
                    }
                });
            }
            
            // Очистка неактивных UI нодов
            try {
                clearInactiveUINodes();
            } catch (Exception e) {
                System.err.println("Error clearing inactive UI nodes: " + e.getMessage());
            }
        });
    }
    
    /**
     * Return to pause from help
     */
    public void returnToPauseFromHelp() {
        // Return to pause from help
        var scene = FXGL.getGameScene();
        var uiNodes = new ArrayList<Node>(scene.getUINodes());
        for (Node node : uiNodes) {
            if (node instanceof com.arcadeblocks.ui.HelpView
                || node instanceof com.arcadeblocks.ui.PauseView) {
                removeUINodeSafely(node);
            }
        }

        showPauseScreen();
    }
    
    /**
     * Return to the main menu without changing the background (to return from settings/help/credits)
     */
    public void returnToMainMenuFromSettings() {
        // КРИТИЧНО: Сбрасываем флаг inPauseSettings
        inPauseSettings = false;

        // Проверяем, нужно ли установить флаг завершения игры
        if (!isDebugMode && saveManager != null) {
            int currentLevel = FXGL.geti("level");
            GameLine currentGameLine = GameLine.fromLevel(currentLevel);
            if (currentLevel >= currentGameLine.getEndLevel()) {
                // Игрок прошел уровень 116 - устанавливаем флаг завершения
                saveManager.setGameCompletedForActiveSlot();
            }
        }

        // Запускаем музыку главного меню с учетом завершения игры
        startMainMenuMusic();

        boolean mainMenuVisible = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> node instanceof com.arcadeblocks.ui.MainMenuView);

        // Проверяем, завершена ли игра хотя бы в одном слоте
        boolean gameCompleted = false;
        if (saveManager != null) {
            for (int slot = 1; slot <= 4; slot++) {
                if (saveManager.isGameCompletedInSlot(slot)) {
                    gameCompleted = true;
                    break;
                }
            }
        }

        if (mainMenuVisible && !gameCompleted) {
            // MainMenuView уже на экране и игра не завершена - просто удаляем overlay
            showMainMenu();
        } else {
            // Игра завершена или MainMenuView отсутствует - пересоздаем меню с новым фоном
            clearUINodesSafely();
            MainMenuView mainMenuView = new MainMenuView(this, true); // true = изменить фон
            FXGL.getGameScene().addUINode(mainMenuView);
            
            javafx.application.Platform.runLater(() -> {
                mainMenuView.requestFocus();
            });
            
            uninstallMousePaddleControlHandlers();
            mouseClicksBlocked = false;
            setSystemCursor();
        }
    }
    
    /**
     * Reset pause state (used when exiting pause to main menu)
     */
    public void resetPauseState() {
        isPaused = false;
        inPauseSettings = false;
    }
    
    /**
     * Proceed to the next level
     */
    private void proceedToNextLevel(int currentLevel, int currentScore, int currentLives) {
        proceedToNextLevel(currentLevel, currentScore, currentLives, null);
    }

    private void proceedToNextLevel(int currentLevel, int currentScore, int currentLives, Runnable overlayRemover) {
        if (proceedToNextLevelCalled) {
            return;
        }
        proceedToNextLevelCalled = true;

        fadeOutPaddleAndBalls(false,
            () -> continueToNextLevel(currentLevel, currentScore, currentLives, overlayRemover));
    }

    private void continueToNextLevel(int currentLevel, int currentScore, int currentLives, Runnable overlayRemover) {
        isLevelCompleted = false;
        isTransitioning = false;
        levelCompletedMessageShown = false;

        if (saveManager != null && !isDebugMode) {
            saveManager.clearGameSnapshot();
        }

        try {
            if (scoreManager != null) {
                scoreManager.flushPendingOperations();
            }
            if (saveManager != null && !isDebugMode) {
                saveManager.setScore(currentScore);
                saveManager.setLives(currentLives);
                saveManager.setLevelCompleted(currentLevel, true);
                saveManager.setTotalLevelsCompleted(saveManager.getTotalLevelsCompleted() + 1);
            }

            int nextLevel = pendingLevelWarpTarget != null ? pendingLevelWarpTarget : currentLevel + 1;
            pendingLevelWarpTarget = null;

            GameLine currentGameLine = GameLine.fromLevel(currentLevel);
            if (nextLevel > currentGameLine.getEndLevel()) {
                Runnable videoCleanup = overlayRemover != null
                    ? () -> javafx.application.Platform.runLater(overlayRemover)
                    : null;
                showVictoryScreen(currentLevel, videoCleanup);
            } else {
                if (saveManager != null && !isDebugMode) {
                    saveManager.setCurrentLevel(nextLevel);
                    saveManager.autoSaveToActiveSlot();
                    saveManager.awaitPendingWrites();
                }

                if (isDebugMode) {
                    startDebugLevel(nextLevel, currentLives, currentScore, overlayRemover);
                } else {
                    startLevel(nextLevel, false, overlayRemover);
                }
            }
        } catch (Exception e) {
            // КРИТИЧНО: При ошибке обязательно удаляем оверлей чтобы не было утечки памяти
            if (overlayRemover != null) {
                javafx.application.Platform.runLater(overlayRemover);
            }
            System.err.println("Error when moving to the next level: " + e.getMessage());
            e.printStackTrace();
            returnToMainMenu();
        }
    }

     public void destroyAllBricks() {
         var bricks = new java.util.ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK));
         if (bricks.isEmpty()) {
             return;
         }

         for (Entity brickEntity : bricks) {
             Brick brickComponent = brickEntity.getComponent(Brick.class);
             if (brickComponent != null) {
                 brickComponent.destroySilently();
             } else {
                 brickEntity.removeFromWorld();
             }
         }

         FXGL.runOnce(this::checkLevelCompletion, Duration.millis(120));
     }
    
    /**
     * Show victory screen
     */
    private void showVictoryScreen(int completedLevel) {
        showVictoryScreen(completedLevel, null);
    }

    private void showVictoryScreen(int completedLevel, Runnable videoCleanup) {
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(completedLevel);
        if (metadata != null && metadata.getCompletionVideoPath() != null && metadata.shouldShowPoemAfterVictory()) {
            playFinalVictorySequence(metadata);
            return;
        }
        boolean bonusEnding = false;
        com.arcadeblocks.config.BonusLevelConfig.BonusChapter bonusChapter =
            com.arcadeblocks.config.BonusLevelConfig.getChapter(completedLevel);
        if (bonusChapter != null && completedLevel >= bonusChapter.getEndLevel()) {
            bonusEnding = true;
        }
        GameLine line = GameLine.fromLevel(completedLevel);
        boolean isArcadeLineEnd = (line == GameLine.ARCADE_BLOCKS || line == GameLine.ARCADE_BLOCKS_BONUS)
            && completedLevel >= line.getEndLevel();
        boolean isLBreakoutEnd = (line == GameLine.LBREAKOUT1) && completedLevel >= line.getEndLevel();
        if (isArcadeLineEnd) {
            double levelTimeSeconds = scoreManager != null ? scoreManager.getLevelTimerSeconds() : 0.0;
            ensureLevelStarsPersisted(completedLevel);
            recordLevelCompletionStats(completedLevel, levelTimeSeconds);
            final boolean bonusEndingFinal = bonusEnding;
            showGameLineCompletionOverlay(line, () -> {
                if (videoCleanup != null) {
                    videoCleanup.run();
                }
                showCredits(true, bonusEndingFinal);
            });
        } else if (isLBreakoutEnd) {
            double levelTimeSeconds = scoreManager != null ? scoreManager.getLevelTimerSeconds() : 0.0;
            ensureLevelStarsPersisted(completedLevel);
            recordLevelCompletionStats(completedLevel, levelTimeSeconds);
            showGameLineCompletionOverlay(line, () -> {
                if (videoCleanup != null) {
                    videoCleanup.run();
                }
                showCreditsForLBreakout1();
            });
        } else {
            // Передаем true, так как это титры после завершения игры (основная кампания или бонусная глава)
            if (videoCleanup != null) {
                videoCleanup.run();
            }
            showCredits(true, bonusEnding);
        }
    }

    private void playFinalVictorySequence(com.arcadeblocks.config.LevelConfig.LevelMetadata metadata) {
        unblockMouseClicks();
        setSystemCursor();

        isVictorySequenceActive = true;
        
        // Clear all old video resources BEFORE creating the new final victory video.
        // This prevents memory leaks when playing the final victory video.
        cleanupActiveVideoResources();
        
        // Capture the token of the current video session to verify the relevance of the callback
        final long currentVideoToken = videoSessionToken;

        if (audioManager != null) {
            audioManager.cancelPendingMusic();
            audioManager.stopMusic();
            audioManager.playSFX(getCurrentLevelCompleteSound());
        }

        double duration = metadata.getCompletionVideoDurationSeconds() != null
            ? metadata.getCompletionVideoDurationSeconds()
            : 30.0;

        playVideoOverlay(metadata.getCompletionVideoPath(), duration, remover -> {
            // CRITICAL: Performing cleanup in the UI thread
            javafx.application.Platform.runLater(() -> {
                // Keep video overlay visible until after completion window
                final Runnable videoCleanup = () -> {
                    if (remover != null) {
                        try {
                            remover.run();
                        } catch (Exception e) {
                            System.err.println("Error when removing the overlay from the final video: " + e.getMessage());
                        }
                    }
                    if (currentVideoToken == videoSessionToken) {
                        cleanupActiveVideoResources();
                    }
                };

                isVictorySequenceActive = false;

                int finalLevel = FXGL.geti("level");
                double finalLevelTime = scoreManager != null ? scoreManager.getLevelTimerSeconds() : 0.0;
                ensureLevelStarsPersisted(finalLevel);
                recordLevelCompletionStats(finalLevel, finalLevelTime);

                GameLine line = GameLine.fromLevel(finalLevel);
                boolean showLineOverlay = (line == GameLine.ARCADE_BLOCKS || line == GameLine.ARCADE_BLOCKS_BONUS)
                    && finalLevel >= line.getEndLevel();

                Runnable nextStep = metadata.shouldShowPoemAfterVictory() ? this::showPoemScreen : this::showCredits;
                int finalScore = scoreManager != null ? scoreManager.getCurrentScore() : FXGL.geti("score");
                int finalLives = FXGL.geti("lives");

                Runnable proceed = () -> {
                    if (showLineOverlay) {
                        showGameLineCompletionOverlay(line, () -> {
                            videoCleanup.run();
                            nextStep.run();
                        });
                    } else {
                        videoCleanup.run();
                        nextStep.run();
                    }
                };

                showLevelCompletionMessage(finalLevel, finalScore, finalLives, proceed);
            });
        });
    }

    private void playBossCompletionVideo(
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata,
        String completionSound,
        Consumer<Runnable> onComplete
    ) {
        boolean hasVideo = metadata != null
            && metadata.getCompletionVideoPath() != null
            && !metadata.getCompletionVideoPath().isBlank();

        if (!hasVideo) {
            if (audioManager != null && completionSound != null && !completionSound.isBlank()) {
                audioManager.playSFX(completionSound);
            }
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }

        isVictorySequenceActive = true;
        
        // CRITICAL: Clear all old video resources BEFORE creating a new boss completion video.
        // This prevents memory leaks when playing boss videos.
        cleanupActiveVideoResources();
        
        // CRITICAL: Capture the token of the current video session to verify the relevance of the callback.
        // If the token has changed when finish is triggered, it means that a new video has been created,
        // and the old callback should not perform the final cleanup.
        final long currentVideoToken = videoSessionToken;

        final boolean[] audioFinished = {completionSound == null || completionSound.isBlank()};
        final boolean[] videoFinished = {false};
        final javafx.animation.PauseTransition[] audioGuardRef = {null};
        final AtomicReference<Runnable> overlayRemoverRef = new AtomicReference<>();
        final AtomicBoolean completionHandled = new AtomicBoolean(false);

        Runnable finish = () -> {
            if (completionHandled.compareAndSet(false, true)) {
                // CRITICAL: First, clear PauseTransition if it is still active.
                if (audioGuardRef[0] != null) {
                    try {
                        synchronized (activeVideoPauseTransitions) {
                            activeVideoPauseTransitions.remove(audioGuardRef[0]);
                        }
                        audioGuardRef[0].stop();
                        audioGuardRef[0] = null;
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
                
                isVictorySequenceActive = false;
                // КРИТИЧНО: НЕ удаляем overlay здесь - передаем его в onComplete
                // Это предотвращает мерцание игрового мира между видео и сюжетным окном
                Runnable remover = overlayRemoverRef.getAndSet(null);
                
                // Performing cleanup in the UI thread
                javafx.application.Platform.runLater(() -> {
                    // КРИТИЧНО: НЕ вызываем remover.run() здесь!
                    // Оверлей должен остаться как черный фон под сюжетным окном
                    // Он будет удален только когда сюжетное окно закроется
                    
                    // Check the video session token before final cleanup
                    // If the token has changed, it means a new video has been created and the old callback should not clean up the new video
                    if (currentVideoToken == videoSessionToken) {
                        // КРИТИЧНО: НЕ вызываем cleanupActiveVideoResources() здесь
                        // Это удалит видео оверлей и покажет игровой мир
                        // Очистка произойдет вместе с удалением оверлея позже
                    } else {
                        // System.out.println("Final boss video cleanup skipped - token expired (race condition prevented)");
                    }
                    
                    // КРИТИЧНО: Передаем remover в onComplete вместо null
                    // onComplete передаст его в proceedToNextLevel → startLevel → showChapterStory
                    // Где оверлей будет удален после показа сюжетного окна
                    if (onComplete != null) {
                        try {
                            onComplete.accept(remover); // Передаем remover для бесшовного перехода
                        } catch (Exception e) {
                            System.err.println("Error in onComplete: " + e.getMessage());
                            e.printStackTrace();
                            // КРИТИЧНО: При ошибке удаляем оверлей чтобы не было утечки
                            if (remover != null) {
                                try {
                                    remover.run();
                                } catch (Exception ex) {
                                    System.err.println("Error removing overlay after onComplete error: " + ex.getMessage());
                                }
                            }
                        }
                    } else {
                        // КРИТИЧНО: Если нет onComplete, удаляем оверлей чтобы не было утечки
                        if (remover != null) {
                            try {
                                remover.run();
                            } catch (Exception e) {
                                System.err.println("Error removing overlay when no onComplete: " + e.getMessage());
                            }
                        }
                    }
                });
            }
        };

        Runnable tryFinish = () -> {
            if (audioFinished[0] && videoFinished[0]) {
                finish.run();
            }
        };

        double videoDuration = metadata.getCompletionVideoDurationSeconds() != null
            ? metadata.getCompletionVideoDurationSeconds()
            : 8.0;
        double soundDuration = (completionSound != null && !completionSound.isBlank())
            ? estimateSoundDurationSeconds(completionSound)
            : 0.0;
        double guardDuration = Math.max(videoDuration, soundDuration);

        if (audioManager != null && completionSound != null && !completionSound.isBlank()) {
            audioManager.playSFX(completionSound, () -> javafx.application.Platform.runLater(() -> {
                if (audioGuardRef[0] != null) {
                    // Remove from tracking list before stopping
                    synchronized (activeVideoPauseTransitions) {
                        activeVideoPauseTransitions.remove(audioGuardRef[0]);
                    }
                    audioGuardRef[0].stop();
                    audioGuardRef[0] = null;
                }
                audioFinished[0] = true;
                tryFinish.run();
            }));

            double guardSeconds = soundDuration > 0.0 ? soundDuration + 0.2 : guardDuration;
            javafx.animation.PauseTransition audioGuard = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(guardSeconds));
            audioGuard.setOnFinished(evt -> {
                // Remove from tracking list before resetting
                synchronized (activeVideoPauseTransitions) {
                    activeVideoPauseTransitions.remove(audioGuard);
                }
                audioGuardRef[0] = null;
                audioFinished[0] = true;
                tryFinish.run();
            });
            audioGuardRef[0] = audioGuard;
            // Add to the tracking list for cleaning when changing levels
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.add(audioGuard);
            }
            audioGuard.play();
        } else {
            audioFinished[0] = true;
        }

        playVideoOverlay(metadata.getCompletionVideoPath(), guardDuration, remover -> {
            overlayRemoverRef.set(remover);
            videoFinished[0] = true;
            tryFinish.run();
        });
    }

    private void playLevelIntroVideo(int levelNumber, com.arcadeblocks.config.LevelConfig.LevelMetadata metadata, Runnable onComplete) {
        setLevelIntroActive(true);
        
        // КРИТИЧНО: Сбрасываем runtime volume multiplier обратно к 1.0
        // Это необходимо, так как ChapterStoryView мог оставить его на низком значении
        if (audioManager != null) {
            audioManager.resetRuntimeMusicVolumeMultiplier();
        }
        
        // Clear all old video resources BEFORE creating a new intro video.
        // This prevents a race condition where tryFinish from the old video could clear the new video
        // when restarting the level via the debug menu.
        cleanupActiveVideoResources();
        
        // Capture the token of the current video session to check the relevance of the callback.
        // If the token has changed when tryFinish is triggered, it means that a new video has been created,
        // and the old callback should not perform the final cleanup.
        final long currentVideoToken = videoSessionToken;

        final boolean[] audioFinished = {false};
        final boolean[] videoFinished = {false};
        final javafx.animation.PauseTransition[] audioGuardRef = {null};
        final AtomicReference<Runnable> overlayRemoverRef = new AtomicReference<>();

        String loadingSound = getCurrentLevelLoadingSound();
        // Для всех уровней с видео отключаем звук при показе видео, для остальных уровней проверяем null
        boolean soundDisabled = shouldDisableLoadingSoundDuringVideo(levelNumber) || (loadingSound == null);
        if (loadingSound == null || loadingSound.isBlank()) {
            loadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
        }

        double videoDuration = metadata.getIntroVideoDurationSeconds() != null
            ? metadata.getIntroVideoDurationSeconds()
            : 8.0;
        double soundDuration = estimateSoundDurationSeconds(loadingSound);
        double guardDuration = Math.max(videoDuration, soundDuration);

        Runnable tryFinish = () -> {
            if (audioFinished[0] && videoFinished[0]) {
                // First clean up PauseTransition if it's still active
                if (audioGuardRef[0] != null) {
                    try {
                        synchronized (activeVideoPauseTransitions) {
                            activeVideoPauseTransitions.remove(audioGuardRef[0]);
                        }
                        audioGuardRef[0].stop();
                        audioGuardRef[0] = null;
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
                
                // Ensure overlay is removed even on errors
                Runnable remover = overlayRemoverRef.getAndSet(null);
                
                setLevelIntroActive(false);
                
                // Execute cleanup in UI thread
                // remover itself uses Platform.runLater(), so we wrap everything in one runLater
                // to guarantee correct execution order
                javafx.application.Platform.runLater(() -> {
                    // Call remover which will clean up overlay and MediaPlayer
                    if (remover != null) {
                        try {
                            remover.run();
                        } catch (Exception e) {
                            System.err.println("Error removing overlay: " + e.getMessage());
                            // Continue execution even on error
                        }
                    }
                    
                    // Check video session token before final cleanup
                    // If token has changed, it means a new video was created (e.g., during quick
                    // level restart via debug menu), and the old callback should not clean up the new video
                    if (currentVideoToken == videoSessionToken) {
                        // Final cleanup of all active video resources after intro video completes
                        // This ensures that ALL resources (PauseTransition, MediaPlayer, overlay) 
                        // are fully cleaned up and don't remain in memory
                        // remover has already removed overlay from lists, but we do final cleanup in case
                        // something remained (e.g., PauseTransition that wasn't removed)
                        cleanupActiveVideoResources();
                    } else {
                        // System.out.println("Skipped final video cleanup - token is stale (race condition prevented)");
                    }
                    
                    // Call onComplete after full resource cleanup
                    if (onComplete != null) {
                        try {
                            onComplete.run();
                        } catch (Exception e) {
                            System.err.println("Error in onComplete: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        javafx.animation.PauseTransition audioGuard = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(soundDuration + 0.5));
        audioGuard.setOnFinished(evt -> {
            // Remove from tracking list before nulling
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.remove(audioGuard);
            }
            audioGuardRef[0] = null;
            audioFinished[0] = true;
            tryFinish.run();
        });

        // КРИТИЧНО: Всегда останавливаем музыку при показе видео, независимо от звука загрузки
        if (audioManager != null) {
            audioManager.stopMusic();
        }
        
        if (audioManager != null && !soundDisabled) {
            String soundToPlay = loadingSound;
            audioManager.playSFX(soundToPlay, () -> javafx.application.Platform.runLater(() -> {
                if (audioGuardRef[0] != null) {
                    // CRITICAL: Remove from tracking list before stopping
                    synchronized (activeVideoPauseTransitions) {
                        activeVideoPauseTransitions.remove(audioGuardRef[0]);
                    }
                    audioGuardRef[0].stop();
                    audioGuardRef[0] = null;
                }
                audioFinished[0] = true;
                tryFinish.run();
            }));
            audioGuardRef[0] = audioGuard;
            // CRITICAL: Add to tracking list for cleanup on level change
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.add(audioGuard);
            }
            audioGuard.play();
        } else {
            audioFinished[0] = true;
        }

        playVideoOverlay(metadata.getIntroVideoPath(), guardDuration, remover -> {
            overlayRemoverRef.set(remover);
            videoFinished[0] = true;
            tryFinish.run();
        });
    }

    private void playVideoOverlay(String assetPath, double fallbackDurationSeconds, Consumer<Runnable> onFinished) {
        if (assetPath == null || assetPath.isBlank()) {
            if (onFinished != null) {
                onFinished.accept(() -> {});
            }
            return;
        }

        // Remove letterbox borders for video playback
        clearLetterboxOverlay();

        javafx.application.Platform.runLater(() -> {
            try {

            // Use current resolution instead of fixed sizes (like in main menu and Level Intro)
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: black;");
            
            // Mark as fullScreenOverlay for proper positioning
            overlay.setUserData("fullScreenOverlay");

            // CRITICAL: Create video backend via factory
            com.arcadeblocks.video.VideoPlayerBackend backend;
            try {
                backend = videoBackendFactory.createBackend();
                // System.out.println("Using backend: " + backend.getBackendName() + " for video: " + assetPath);
            } catch (Exception e) {
                System.err.println("Failed to create video backend: " + e.getMessage());
                e.printStackTrace();
                if (onFinished != null) {
                    onFinished.accept(() -> {});
                }
                return;
            }
            
            // Prepare video
            javafx.scene.Node videoNode;
            try {
                videoNode = backend.prepareVideo(assetPath, currentRes.getWidth(), currentRes.getHeight());
            } catch (Exception e) {
                System.err.println("Failed to prepare video with " + backend.getBackendName() + ": " + e.getMessage());

                // CRITICAL: Fallback to JavaFX Media backend if VLC couldn't load video
                if (!(backend instanceof com.arcadeblocks.video.JavaFxMediaBackend)) {
                    try {
                        backend.cleanup();
                        backend = new com.arcadeblocks.video.JavaFxMediaBackend();
                        videoNode = backend.prepareVideo(assetPath, currentRes.getWidth(), currentRes.getHeight());
                    } catch (Exception fallbackException) {
                        System.err.println("JavaFX Media fallback failed: " + fallbackException.getMessage());
                        fallbackException.printStackTrace();
                        backend.cleanup();
                        if (onFinished != null) {
                            onFinished.accept(() -> {});
                        }
                        return;
                    }
                } else {
                    backend.cleanup();
                    if (onFinished != null) {
                        onFinished.accept(() -> {});
                    }
                    return;
                }
            }
            
            // Save final references for use in lambda
            final javafx.scene.Node finalVideoNode = videoNode;
            final com.arcadeblocks.video.VideoPlayerBackend backendRef = backend;
            
            overlay.getChildren().add(finalVideoNode);

            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicBoolean callbackInvoked = new AtomicBoolean(false);
            
            // CRITICAL: Create wrapper to track overlay and backend together
            final VideoOverlayWrapper videoWrapper = new VideoOverlayWrapper(overlay, backendRef);

            Runnable removeOverlay = () -> {
                if (finished.compareAndSet(false, true)) {
                    // CRITICAL: Unbind ResponsiveLayoutHelper
                    try {
                        com.arcadeblocks.ui.util.ResponsiveLayoutHelper.unbind(overlay);
                    } catch (Exception e) {
                        System.err.println("Error cleaning up ResponsiveLayoutHelper: " + e.getMessage());
                    }
                    
                    // CRITICAL: Clean up video backend
                    try {
                        backendRef.cleanup();
                    } catch (Exception e) {
                        System.err.println("Error cleaning up video backend: " + e.getMessage());
                    }
                    
                    // CRITICAL: Remove wrapper from active overlays list
                    // Now remove wrapper instead of just overlay
                    synchronized (activeVideoOverlays) {
                        activeVideoOverlays.removeIf(w -> w.overlay == overlay);
                    }
                    
                    try {
                        // CRITICAL: Clear overlay from scene
                        FXGL.getGameScene().removeUINode(overlay);
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            };

            Runnable cleanup = () -> {
                if (callbackInvoked.compareAndSet(false, true)) {
                    if (onFinished != null) {
                        onFinished.accept(removeOverlay);
                    } else {
                        removeOverlay.run();
                    }
                }
            };
            
            // CRITICAL: Add wrapper to list BEFORE setting callback to prevent race condition
            // If video completes instantly (e.g., error), wrapper must already be in the list
            synchronized (activeVideoOverlays) {
                activeVideoOverlays.add(videoWrapper);
            }

            backendRef.setOnFinished(cleanup);
            backendRef.setOnError(cleanup);

            FXGL.getGameScene().addUINode(overlay);
            
            // Use ResponsiveLayoutHelper for proper positioning (like other fullscreen overlays)
            com.arcadeblocks.ui.util.ResponsiveLayoutHelper.bindToStage(overlay, (width, height) -> {
                overlay.setTranslateX(0);
                overlay.setTranslateY(0);
                overlay.setLayoutX(0);
                overlay.setLayoutY(0);
                // Update video node dimensions
                if (finalVideoNode instanceof javafx.scene.image.ImageView) {
                    javafx.scene.image.ImageView iv = (javafx.scene.image.ImageView) finalVideoNode;
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                } else if (finalVideoNode instanceof javafx.scene.media.MediaView) {
                    javafx.scene.media.MediaView mv = (javafx.scene.media.MediaView) finalVideoNode;
                    mv.setFitWidth(width);
                    mv.setFitHeight(height);
                } else if (finalVideoNode instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) finalVideoNode;
                    pane.setPrefWidth(width);
                    pane.setPrefHeight(height);
                }
            });
            
            // Start playback
            backendRef.play();

            // Fallback timer in case video doesn't send onFinished
            // AtomicBoolean finished prevents double cleanup if video completes before timer
            // Увеличен запас с 0.1 до 3.0 секунд, чтобы VLC успел отправить событие finished
            if (fallbackDurationSeconds > 0.0) {
                FXGL.runOnce(cleanup, Duration.seconds(fallbackDurationSeconds + 3.0));
            }
            
            } catch (Exception e) {
                System.err.println("Failed to play video " + assetPath + ": " + e.getMessage());
                e.printStackTrace();
                if (onFinished != null) {
                    onFinished.accept(() -> {});
                }
            }
        });
    }

    public void showPoemScreen() {
        cleanupGameplayState();
        unblockMouseClicks();
        setSystemCursor();

        if (audioManager != null) {
            audioManager.cancelPendingMusic();
            audioManager.playMusic(AudioConfig.CREDITS_MUSIC, true);
        }

        clearUINodesSafely();
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.PoemView(this));
    }

    private double estimateSoundDurationSeconds(String soundPath) {
        if (soundPath == null || soundPath.isBlank()) {
            return 2.0;
        }

        try {
            String normalized = soundPath.startsWith("/") ? soundPath.substring(1) : soundPath;
            if (!normalized.startsWith("sounds/")) {
                normalized = "sounds/" + normalized;
            }
            var url = getClass().getResource("/assets/" + normalized);
            if (url != null) {
                try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(url)) {
                    AudioFormat format = inputStream.getFormat();
                    long frames = inputStream.getFrameLength();
                    if (frames > 0 && format.getFrameRate() > 0) {
                        return frames / format.getFrameRate();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return fallbackSoundDuration(soundPath);
    }

    private double fallbackSoundDuration(String soundPath) {
        if (soundPath == null) {
            return 2.0;
        }
        if (soundPath.contains("loading_sound")) {
            return 10.0;
        }
        return 2.5;
    }

    private void saveCurrentGameSnapshot() {
        if (saveManager == null) {
            return;
        }
        
        // Не создаем snapshot если мы в настройках паузы - это предотвращает утечки памяти
        // при частом изменении настроек (например, чекбоксов)
        if (inPauseSettings) {
            return;
        }
        
        if (paddleComponent == null || isLevelCompleted || isTransitioning) {
            saveManager.clearGameSnapshot();
            return;
        }
        
        GameSnapshot snapshot = captureCurrentGameSnapshot();
        if (snapshot != null) {
            saveManager.saveGameSnapshot(snapshot);
        } else {
            saveManager.clearGameSnapshot();
        }
    }

    private GameSnapshot captureCurrentGameSnapshot() {
        if (paddleComponent == null) {
            return null;
        }
        
        var bricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK);
        if (bricks.isEmpty()) {
            return null;
        }
        
        GameSnapshot snapshot = new GameSnapshot();
        // Предварительно выделяем память для списков, чтобы избежать многократных реаллокаций
        snapshot.bricks = new ArrayList<>(bricks.size());
        var ballsCount = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL).size();
        snapshot.balls = new ArrayList<>(Math.max(ballsCount, 5));
        var bonusesCount = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS).size();
        snapshot.bonuses = new ArrayList<>(Math.max(bonusesCount, 10));
        var projectilesCount = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE).size();
        snapshot.projectiles = new ArrayList<>(Math.max(projectilesCount, 5));
        var bossesCount = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS).size();
        snapshot.bosses = new ArrayList<>(Math.max(bossesCount, 2));
        
        snapshot.level = FXGL.geti("level");
        snapshot.score = scoreManager != null ? scoreManager.getCurrentScore() : FXGL.geti("score");
        snapshot.lives = livesManager != null ? livesManager.getCurrentLives() : FXGL.geti("lives");
        snapshot.levelTimeSeconds = scoreManager != null ? scoreManager.getLevelTimerSeconds() : 0.0;

        if (audioManager != null) {
            snapshot.currentMusicFile = audioManager.getCurrentMusicFile();
            snapshot.currentMusicTime = audioManager.getCurrentMusicPosition();
        }
        
        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null) {
            return null;
        }
        
        GameSnapshot.PaddleState paddleState = new GameSnapshot.PaddleState();
        paddleState.x = paddleEntity.getX();
        paddleState.y = paddleEntity.getY();
        paddleState.sizeMultiplier = paddleComponent.getSizeMultiplier();
        paddleState.turboMode = paddleComponent.isTurboMode();
        paddleState.invisible = paddleComponent.isInvisible();
        paddleState.movementBlocked = paddleComponent.isMovementBlocked();
        paddleState.speed = paddleComponent.getSpeed();
        snapshot.paddle = paddleState;
        
        for (Entity brickEntity : bricks) {
            Brick brickComponent = brickEntity.getComponent(Brick.class);
            if (brickComponent == null) {
                continue;
            }
            GameSnapshot.BrickState state = new GameSnapshot.BrickState();
            state.x = brickEntity.getX();
            state.y = brickEntity.getY();
            state.health = brickComponent.getHealth();
            state.maxHealth = brickComponent.getMaxHealth();
            state.scoreValue = brickComponent.getScoreValue();
            state.colorHex = colorToHex(brickComponent.getBaseColor());
            try {
                state.colorName = brickEntity.getString("color");
            } catch (Exception ignored) {
            }
            snapshot.bricks.add(state);
        }
        
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ballEntity : balls) {
            Ball ballComponent = ballEntity.getComponent(Ball.class);
            if (ballComponent == null) {
                continue;
            }
            GameSnapshot.BallState state = new GameSnapshot.BallState();
            state.x = ballEntity.getX();
            state.y = ballEntity.getY();

            if (ballComponent.isPausedForCountdown()) {
                state.velocityX = ballComponent.getPausedVelocityX();
                state.velocityY = ballComponent.getPausedVelocityY();
                state.pausedForCountdown = true;
                state.pausedVelocityX = ballComponent.getPausedVelocityX();
                state.pausedVelocityY = ballComponent.getPausedVelocityY();
            } else {
                PhysicsComponent physics = ballEntity.getComponent(PhysicsComponent.class);
                Point2D velocity = physics != null ? physics.getLinearVelocity() : Point2D.ZERO;
                state.velocityX = velocity.getX();
            state.velocityY = velocity.getY();
            state.pausedForCountdown = false;
        }

        double[] offset = ballComponent.getAttachedOffset();
        state.attachedOffsetX = offset != null ? offset[0] : 0.0;
        state.attachedOffsetY = offset != null ? offset[1] : -GameConfig.BALL_RADIUS * 2 - 5;

        state.attachedToPaddle = ballComponent.isAttachedToPaddle();
        state.stickyEnabled = ballComponent.isStickyEnabled();
            state.speedMultiplier = ballComponent.getSpeedMultiplier();
            state.energyBall = ballComponent.isEnergyBall();
            state.explosionBall = ballComponent.isExplosionBall();
            state.weakBall = ballComponent.isWeakBall();
            state.chaoticBall = ballComponent.isChaoticBall();
            state.extraBall = ballComponent.isExtraBall();
            state.maintainConstantSpeed = ballComponent.isMaintainConstantSpeed();
            state.targetSpeed = ballComponent.getTargetSpeed();
            snapshot.balls.add(state);
        }
        
        var bonusEntities = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS);
        for (Entity bonusEntity : bonusEntities) {
            Bonus bonusComponent = bonusEntity.getComponent(Bonus.class);
            if (bonusComponent == null || bonusComponent.isCollected()) {
                continue;
            }
            GameSnapshot.BonusEntityState state = new GameSnapshot.BonusEntityState();
            state.x = bonusEntity.getX();
            state.y = bonusEntity.getY();
            BonusType bonusType = bonusComponent.getBonusType();
            if (bonusType != null) {
                state.bonusType = bonusType.name();
            }
            state.fallSpeed = bonusComponent.getFallSpeed();
            snapshot.bonuses.add(state);
        }
        
        var projectiles = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE);
        for (Entity projectileEntity : projectiles) {
            GameSnapshot.ProjectileState state = new GameSnapshot.ProjectileState();
            state.x = projectileEntity.getX();
            state.y = projectileEntity.getY();
            snapshot.projectiles.add(state);
        }
        
        var bosses = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS);
        for (Entity bossEntity : bosses) {
            Boss bossComponent = bossEntity.getComponentOptional(Boss.class).orElse(null);
            if (bossComponent == null) {
                continue;
            }
            GameSnapshot.BossState state = new GameSnapshot.BossState();
            state.bossId = bossComponent.getBossId();
            state.x = bossEntity.getX();
            state.y = bossEntity.getY();
            state.health = bossComponent.getHealth();
            state.maxHealth = bossComponent.getMaxHealth();
            state.spawnCompleted = bossComponent.isSpawnCompleted();
            snapshot.bosses.add(state);
        }
        
        if (bonusEffectManager != null) {
            snapshot.bonusEffects = bonusEffectManager.createSnapshot();
        }
        
        return snapshot;
    }

    private boolean restoreGameSnapshot(GameSnapshot snapshot) {
        if (snapshot == null || paddleComponent == null) {
            return false;
        }
        if (snapshot.bricks == null || snapshot.bricks.isEmpty()) {
            return false;
        }
        
        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null) {
            return false;
        }
        
        FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.BALL).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS).forEach(Entity::removeFromWorld);
        
        // Clear list of attached balls before restore
        Ball.clearAttachedBalls();
        
        Brick.resetBrickCounter();
        
        FXGL.set("score", snapshot.score);
        FXGL.set("lives", snapshot.lives);
        
        if (scoreManager != null) {
            scoreManager.setScore(snapshot.score);
            scoreManager.setLevelTimerSeconds(snapshot.levelTimeSeconds);
            scoreManager.resumeLevelTimer();
        }
        if (livesManager != null) {
            livesManager.setCurrentLivesFromSnapshot(snapshot.lives);
        }
        
        if (gameplayUIView != null) {
            gameplayUIView.updateScore(snapshot.score);
            gameplayUIView.updateLives(snapshot.lives);
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(snapshot.level);
            String levelName = levelData != null ? levelData.getName() : null;
            gameplayUIView.updateLevel(snapshot.level, levelName);
        }

        // Store restored music state to be used after countdown
        this.restoredMusicFile = snapshot.currentMusicFile;
        this.restoredMusicTime = snapshot.currentMusicTime;

        leftPressed = false;
        rightPressed = false;
        turboPressed = false;
        resetCallBallState();
        paddleComponent.setMoveLeft(false);
        paddleComponent.setMoveRight(false);
        paddleComponent.setTurboMode(false);
        
        if (saveManager != null && !isDebugMode) {
            saveManager.setScore(snapshot.score);
            saveManager.setLives(snapshot.lives);
        }
        
        paddleEntity.setPosition(snapshot.paddle.x, snapshot.paddle.y);
        paddleComponent.setSizeMultiplier(snapshot.paddle.sizeMultiplier != 0 ? snapshot.paddle.sizeMultiplier : 1.0);
        paddleComponent.setInvisible(snapshot.paddle.invisible);
        paddleComponent.setMovementBlocked(snapshot.paddle.movementBlocked);
        if (snapshot.paddle.speed > 0) {
            paddleComponent.setSpeed(snapshot.paddle.speed);
        }
        
        for (GameSnapshot.BrickState brickState : snapshot.bricks) {
            SpawnData spawnData = new SpawnData(brickState.x, brickState.y);
            if (brickState.colorName != null) {
                spawnData.put("color", brickState.colorName);
            }
            Entity brickEntity = FXGL.spawn("brick", spawnData);
            Color baseColor = colorFromHex(brickState.colorHex, Color.web(GameConfig.NEON_PURPLE));
            boolean isExplosive = "explosive".equals(brickState.colorName);
            Brick brickComponent = new Brick(Math.max(brickState.maxHealth, 1), baseColor, brickState.scoreValue, isExplosive);
            brickEntity.addComponent(brickComponent);
            brickComponent.restoreState(brickState.health);
        }
        
        for (GameSnapshot.BallState ballState : snapshot.balls) {
            SpawnData ballData = new SpawnData(ballState.x, ballState.y);
            Entity ballEntity = FXGL.spawn("ball", ballData);
            Ball ballComponent = new Ball();
            ballEntity.addComponent(ballComponent);
            
            ballComponent.setSpeedMultiplier(ballState.speedMultiplier != 0 ? ballState.speedMultiplier : 1.0);
        ballComponent.setStickyEnabled(ballState.stickyEnabled);
        ballComponent.setEnergyBall(ballState.energyBall);
        ballComponent.setExplosionBall(ballState.explosionBall);
        ballComponent.setWeakBall(ballState.weakBall);
        ballComponent.setChaoticBall(ballState.chaoticBall);
        ballComponent.setExtraBall(ballState.extraBall);
        ballComponent.setMaintainConstantSpeed(ballState.maintainConstantSpeed);
        if (ballState.targetSpeed > 0) {
            ballComponent.setTargetSpeed(ballState.targetSpeed);
        }

        double offsetX = ballState.attachedOffsetX;
        double offsetY = ballState.attachedOffsetY;
        ballComponent.setAttachedOffset(offsetX, offsetY);

            ballComponent.restorePauseState(ballState.pausedForCountdown, ballState.pausedVelocityX, ballState.pausedVelocityY);
            
            PhysicsComponent physics = ballEntity.getComponent(PhysicsComponent.class);
            if (ballState.attachedToPaddle) {
                ballComponent.attachToPaddle(paddleEntity);
                // Add ball to attached list for proper launch system operation
                Ball.addAttachedBall(ballEntity);
            } else if (physics != null) {
                if (!ballState.pausedForCountdown) {
                    physics.setBodyType(BodyType.DYNAMIC);
                    physics.setLinearVelocity(ballState.velocityX, ballState.velocityY);
                }
            }
        }
        
        if (snapshot.balls == null || snapshot.balls.isEmpty()) {
            SpawnData ballData = new SpawnData(
                paddleEntity.getX() + paddleEntity.getWidth() / 2.0 - GameConfig.BALL_RADIUS,
                paddleEntity.getY() - GameConfig.BALL_RADIUS * 2 - 5
            );
            Entity ballEntity = FXGL.spawn("ball", ballData);
            Ball ballComponent = new Ball();
            ballEntity.addComponent(ballComponent);
            ballComponent.attachToPaddle(paddleEntity);
            // Add the ball to the list of attached items for the launch system to work correctly.
            Ball.addAttachedBall(ballEntity);
        }
        
        if (bonusEffectManager != null) {
            bonusEffectManager.restoreFromSnapshot(snapshot.bonusEffects);
        }
        
        for (GameSnapshot.BonusEntityState bonusState : snapshot.bonuses) {
            if (bonusState.bonusType == null) {
                continue;
            }
            try {
                BonusType bonusType = BonusType.valueOf(bonusState.bonusType);
                SpawnData bonusData = new SpawnData(bonusState.x, bonusState.y);
                bonusData.put("bonusType", bonusType);
                Entity bonusEntity = FXGL.spawn("bonus", bonusData);
                Bonus bonusComponent = new Bonus();
                bonusComponent.setBonusType(bonusType);
                bonusComponent.setFallSpeed(bonusState.fallSpeed);
                bonusEntity.addComponent(bonusComponent);
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        for (GameSnapshot.ProjectileState projectileState : snapshot.projectiles) {
            FXGL.spawn("projectile", projectileState.x, projectileState.y);
        }
        
        for (GameSnapshot.BossState bossState : snapshot.bosses) {
            SpawnData bossData = new SpawnData(bossState.x, bossState.y);
            if (bossState.bossId != null) {
                bossData.put("bossId", bossState.bossId);
            }
            bossData.put("skipSpawnAnimation", true);
            Entity bossEntity = FXGL.spawn("boss", bossData);
            Boss bossComponent = bossEntity.getComponentOptional(Boss.class).orElse(null);
            if (bossComponent != null) {
                bossComponent.restoreState(
                    bossState.health,
                    bossState.spawnCompleted
                );
            }
        }
        
        // After restore - ensure mouse control is active
        installMousePaddleControlHandlers();
        
        // Очищаем snapshot для освобождения памяти
        snapshot.clear();
        
        return true;
    }

    private static String colorToHex(Color color) {
        if (color == null) {
            return null;
        }
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static Color colorFromHex(String hex, Color fallback) {
        if (hex == null || hex.isEmpty()) {
            return fallback;
        }
        try {
            return Color.web(hex);
        } catch (Exception e) {
            return fallback;
        }
    }
    
    /**
     * Toggle pause
     */
    public void togglePause() {
        if (isPaused) {
            if (consumePauseResumeLock()) {
                return;
            }
            // Resume game
            resumeGame();
        } else {
            // Pause game
            pauseGame();
        }
    }
    
    /**
     * Pause the game
     */
    public void pauseGame() {
        if (isPaused) return; // Already paused
        
        isPaused = true;
        pauseResumeLockFromFocusLoss = false;
        enforcePauseFreeze();
        
        // Stop music
        if (audioManager != null) {
            audioManager.pauseMusic();
        }

        // Show pause screen
        showPauseScreen();

        // Cursor visible during pause
        setSystemCursor();
    }
    
    /**
     * Resume the game
     */
    public void resumeGame() {
        if (!isPaused) return; // Not paused
        pauseResumeLockFromFocusLoss = false;
        
        // Hide pause screen
        hidePauseScreen();
        
        // Show countdown timer
        showCountdownTimer();
    }
    
    /**
     * Show countdown timer
     */
    public void showCountdownTimer() {
        if (isCountdownActive) return;
        
        // Check if CountdownTimerView already exists in scene
        var scene = FXGL.getGameScene();
        var existingCountdown = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.CountdownTimerView)
            .findFirst();
        
        if (existingCountdown.isPresent()) {
            // CountdownTimerView already exists, don't create new one
            return;
        }
        
        isCountdownActive = true;
        enforcePauseFreeze();
        
        CountdownTimerView countdownView = new CountdownTimerView(this, () -> {
            // Callback called after timer completes
            isCountdownActive = false;
            actuallyResumeGame();
        });
        
        FXGL.getGameScene().addUINode(countdownView);
    }
    
    /**
     * Show countdown timer for level start
     */
    public void showCountdownTimerForLevelStart(int levelNumber) {
        if (isCountdownActive) return;
        
        isCountdownActive = true;
        enforcePauseFreeze();
        
        CountdownTimerView countdownView = new CountdownTimerView(this, () -> {
            // Callback called after timer completes
            isCountdownActive = false;
            enforceVSyncAfterResume();
            
            // Play music from restored state or start new
            if (restoredMusicFile != null && !restoredMusicFile.isEmpty()) {
                audioManager.playMusic(restoredMusicFile, true, restoredMusicTime);
                restoredMusicFile = null;
                restoredMusicTime = 0.0;
            } else {
                playLevelMusic(levelNumber);
            }

            // Resume all balls movement
            resumeAllBalls();
        // System.out.println("Level music starts after the timer");
            
            // Resume game process
            FXGL.getGameController().resumeEngine();
            
            // Resume level timer
            if (scoreManager != null && !isLevelCompleted && !isTransitioning) {
                scoreManager.resumeLevelTimer();
            }
            
            // Resume all bonus timers
            if (bonusEffectManager != null) {
                bonusEffectManager.resumeAllBonusTimers();
            }
        });
        
        FXGL.getGameScene().addUINode(countdownView);
    }
    
    /**
     * Actually resume the game (called after timer)
     */
    public void actuallyResumeGame() {
        isPaused = false;
        enforceVSyncAfterResume();

        // Resume game process
        resumeAllBalls();
        FXGL.getGameController().resumeEngine();
        
        if (scoreManager != null && !isLevelCompleted && !isTransitioning) {
            scoreManager.resumeLevelTimer();
        }

        if (bonusEffectManager != null) {
            bonusEffectManager.resumeAllBonusTimers();
        }
        
        // Resume music
        if (audioManager != null) {
            audioManager.resumeMusic();
        }

        // Restore letterbox overlay after returning from pause
        updateLetterboxOverlay();

        // Cursor hidden during gameplay
        setHiddenCursor();
    }
    
    public void setLevelIntroActive(boolean active) {
        isLevelIntroActive = active;
    }

    public void setStoryOverlayActive(boolean active) {
        isStoryOverlayActive = active;
    }
    
    /**
     * Set current main menu background
     */
    public void setCurrentMainMenuBackground(String background) {
        this.currentMainMenuBackground = background;
    }
    
    /**
     * Get current main menu background
     */
    public String getCurrentMainMenuBackground() {
        return currentMainMenuBackground;
    }
    
    /**
     * Start main menu music with randomization based on game completion status
     */
    public void startMainMenuMusic() {
        if (audioManager == null) {
            // System.out.println("DEBUG: audioManager is null, cannot start music");
            return;
        }
        
        // Определяем состояние прогресса игры
        com.arcadeblocks.config.AudioConfig.GameProgressState progressState = 
            com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
        
        if (saveManager != null) {
            progressState = saveManager.getMenuProgressState();
        }
        
        // Получаем случайную музыку в зависимости от прогресса игры
        String musicFile = com.arcadeblocks.config.AudioConfig.getRandomMainMenuMusic(progressState);

        System.out.println("[ArcadeBlocksApp] startMainMenuMusic state=" + progressState + ", file=" + musicFile);
        
        // System.out.println("DEBUG: Starting main menu music: " + musicFile + " (state=" + progressState + ")");
        
        // Запускаем музыку
        audioManager.stopMusic();
        audioManager.playMusic(musicFile, true);
    }


    /**
     * Release cached main-menu visuals when they are no longer needed.
     * This drops the reference string and forgets the cached Image so prism textures can be collected.
     */
    private void releaseMainMenuBackgroundAssets() {
        if (currentMainMenuBackground != null) {
            try {
                javafx.scene.image.Image img = ImageCache.get(currentMainMenuBackground);
                if (img != null) {
                    ImageCache.forget(img);
                }
            } catch (Exception ignored) {
                // If the image is not cached, just drop the reference name.
            }
            currentMainMenuBackground = null;
        }
    }

    /**
     * Reset loading sound reference to allow the audio cache to evict it after use.
     */
    private void releaseLevelLoadingSoundReference() {
        currentLevelLoadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
    }
    
    /**
     * Set originally selected save slot
     */
    public void setOriginalSaveSlot(int slotNumber) {
        this.originalSaveSlot = slotNumber;
    }
    
    /**
     * Get originally selected save slot
     */
    public int getOriginalSaveSlot() {
        return originalSaveSlot;
    }
    
    /**
     * Check if level is completed
     */
    public boolean isLevelCompleted() {
        return isLevelCompleted;
    }
    
    /**
     * Check if transitioning between levels
     */
    public boolean isTransitioning() {
        return isTransitioning;
    }
    
    /**
     * Show pause screen
     */
    public void showPauseScreen() {
        // Check if PauseView already exists in scene
        var scene = FXGL.getGameScene();
        var existingPauseView = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.PauseView)
            .findFirst();
        
        if (existingPauseView.isPresent()) {
            // PauseView already exists, don't create new one
            return;
        }
        
        // Clear letterbox overlay before showing pause
        clearLetterboxOverlay();
        
        // Create pause screen
        com.arcadeblocks.ui.PauseView pauseView = new com.arcadeblocks.ui.PauseView(this);
        FXGL.getGameScene().addUINode(pauseView);
        // Remove mouse handlers during pause so paddle doesn't move under UI
        uninstallMousePaddleControlHandlers();
    }
    
    /**
     * Hide pause screen
     */
    public void hidePauseScreen() {
        // Remove all pause screens (safely)
        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
        for (var node : uiNodes) {
            if (node instanceof com.arcadeblocks.ui.PauseView) {
                // CRITICAL: Call cleanup() before removal to free all resources
                ((com.arcadeblocks.ui.PauseView) node).cleanup();
                // Clear handlers before removal
                FXGL.getGameScene().removeUINode(node);
            }
        }
        // Restore handlers after closing pause
        installMousePaddleControlHandlers();
    }

    public void enableMouseFollowTicker(boolean enable) {
        if (mouseFollowTicker != null) {
            mouseFollowTicker.stop();
            mouseFollowTicker = null;
        }
        if (!enable) return;
        // Avoid MouseInfo on Wayland
        if (isLinuxWayland()) return;
        mouseFollowTicker = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(16), e -> {
                try {
                    javafx.stage.Stage stage = FXGL.getPrimaryStage();
                    if (stage == null || stage.getScene() == null) return;
                    if (isCountdownActive) return;
                    java.awt.PointerInfo pi = java.awt.MouseInfo.getPointerInfo();
                    if (pi == null) return;
                    java.awt.Point pt = pi.getLocation();
                    javafx.geometry.Point2D local = stage.getScene().getRoot().screenToLocal(pt.x, pt.y);
                    if (local != null) {
                        movePaddleForSceneX(local.getX());
                    }
                } catch (Throwable ignored) {}
            })
        );
        mouseFollowTicker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        mouseFollowTicker.play();
    }

    public static void main(String[] args) {
        // Initialize AppDataManager and set working directory BEFORE anything else
        String dataDir = AppDataManager.getDataDirectory().toString();
        
        // Pre-create logs directory with proper permissions
        try {
            java.nio.file.Path logsPath = java.nio.file.Paths.get(dataDir, "logs");
            java.nio.file.Files.createDirectories(logsPath);
        } catch (Exception e) {
            System.err.println("Failed to create logs directory: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Pre-create system directory
        try {
            java.nio.file.Path systemPath = java.nio.file.Paths.get(dataDir, "system");
            java.nio.file.Files.createDirectories(systemPath);
        } catch (Exception e) {
            System.err.println("Failed to create system directory: " + e.getMessage());
        }
        
        // Save original working directory for resource loading
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("app.install.dir", originalUserDir);
        
        // Set working directory to user data directory so FXGL creates logs/ and system/ there
        System.setProperty("user.dir", dataDir);
        
        // Disable DPI scaling for Windows - force 100% scaling
        System.setProperty("glass.win.uiScale", "100%");
        System.setProperty("glass.gtk.uiScale", "100%");
        System.setProperty("prism.allowhidpi", "false");
        
        // Setup console encoding for correct UTF-8 display (especially important for Windows)
        com.arcadeblocks.utils.ConsoleUtils.setupConsoleEncoding();
        
        // Attempt to launch via XWayland (X11) on Linux Wayland
        if (shouldRelaunchUnderX11(args)) {
            if (tryRelaunchUnderX11(args)) {
                return; // Terminate current process, control is with child
            }
            // If relaunch failed, continue with normal launch
        }
        launch(args);
    }

    public static boolean shouldRelaunchUnderX11(String[] args) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) return false;
        // Don't loop
        boolean alreadyForced = Arrays.stream(args).anyMatch(a -> "--x11-launched".equals(a));
        if (alreadyForced) return false;
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        boolean isWayland = (sessionType != null && sessionType.equalsIgnoreCase("wayland"))
            || (waylandDisplay != null && !waylandDisplay.isEmpty());
        if (!isWayland) return false;
        String gdkBackend = System.getenv("GDK_BACKEND");
        // If already X11 - do nothing
        if (gdkBackend != null && gdkBackend.toLowerCase().contains("x11")) return false;
        return true;
    }

    public static boolean tryRelaunchUnderX11(String[] args) {
        try {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            String classPath = System.getProperty("java.class.path");
            String mainClass = "com.arcadeblocks.ArcadeBlocksApp";
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(javaBin);
            cmd.add("-cp");
            cmd.add(classPath);
            cmd.add(mainClass);
            cmd.add("--x11-launched");
            cmd.addAll(java.util.Arrays.asList(args));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            // Set environment for X11
            java.util.Map<String, String> env = pb.environment();
            env.put("GDK_BACKEND", "x11");
            // Helps on some systems for SDL (we have audio via SDL, but no video output)
            env.putIfAbsent("SDL_VIDEODRIVER", "x11");
            pb.inheritIO();
            Process p = pb.start();
            // Don't wait - let child process take over terminal, terminate current
            return true;
        } catch (Exception e) {
            System.err.println("Failed to relaunch application under X11: " + e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, нужно ли показывать сюжетное окно для первого уровня.
     * Для уровня 1: показывать только при первом прохождении или в отладочном режиме.
     * Для остальных уровней: всегда показывать (если есть сюжетное окно).
     */
    private boolean shouldShowLevel1Story(int levelNumber) {
        if (levelNumber != 1) {
            return true; // Для всех уровней кроме первого - показывать как обычно
        }
        
        // В отладочном режиме с принудительным показом сюжетных окон - всегда показывать
        if (alwaysShowChapterStory) {
            return true;
        }
        
        // Для первого уровня проверяем очки в сохранении
        if (saveManager != null) {
            int savedScore = saveManager.getScore();
            return savedScore == 0; // Показывать только если очки равны 0 (первое прохождение)
        }
        
        return true; // Если нет saveManager, показывать по умолчанию
    }

    /**
     * Проверяет, нужно ли отключать звук загрузки во время видео.
     * Для уровней с видео: звук загрузки отключается во время воспроизведения видео.
     */
    private boolean shouldDisableLoadingSoundDuringVideo(int levelNumber) {
        // Для уровней 1, 11, 21, 31, 32, 41, 51, 61, 71, 81 и 91 отключаем звук загрузки во время видео
        return levelNumber == 1 || levelNumber == 11 || levelNumber == 21 || levelNumber == 31 || levelNumber == 32 || levelNumber == 41 || levelNumber == 51 || levelNumber == 61 || levelNumber == 71 || levelNumber == 81 || levelNumber == 91;
    }

    /**
     * Проверяет, нужно ли показывать видео для уровней с особой логикой.
     * Для уровней 1, 11, 21, 31, 32, 41, 51, 61, 71, 81 и 91: видео всегда показывается.
     * Для остальных уровней: всегда показывать (если есть видео).
     */
    private boolean shouldShowLevel1StoryAndVideo(int levelNumber) {
        // Для уровней 1, 11, 21, 31, 32, 41, 51, 61, 71, 81 и 91 всегда показываем видео, независимо от прогресса
        if (levelNumber == 1 || levelNumber == 11 || levelNumber == 21 || levelNumber == 31 || levelNumber == 32 || levelNumber == 41 || levelNumber == 51 || levelNumber == 61 || levelNumber == 71 || levelNumber == 81 || levelNumber == 91) {
            return true;
        }
        
        // Для остальных уровней - показывать как обычно
        return true;
    }
    
    /**
     * Показать главное меню
     */
    public void showMainMenu() {
        // КРИТИЧНО: Удаляем только overlay views, MainMenuView уже на экране!
        // Ищем и удаляем только SettingsView, HelpView, LanguageView, SaveGameView
        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
        for (javafx.scene.Node node : uiNodes) {
            if (node instanceof com.arcadeblocks.ui.SettingsView || 
                node instanceof com.arcadeblocks.ui.HelpView ||
                // node instanceof com.arcadeblocks.ui.LanguageView ||
                node instanceof com.arcadeblocks.ui.SaveGameView) {
                removeUINodeSafely(node);
            }
        }
        
        // КРИТИЧНО: Восстанавливаем фокус на MainMenuView после удаления overlay-окон
        for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
            if (node instanceof com.arcadeblocks.ui.MainMenuView) {
                ((com.arcadeblocks.ui.MainMenuView) node).restoreFocus();
                break;
            }
        }
        
        // Показываем курсор
        setSystemCursor();
    }
    
    /**
     * Проверить, открыты ли overlay-окна (настройки, справка, языки, сохранения)
     */
    public boolean hasOverlayWindows() {
        for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
            if (node instanceof com.arcadeblocks.ui.SettingsView || 
                node instanceof com.arcadeblocks.ui.HelpView ||
                // node instanceof com.arcadeblocks.ui.LanguageView ||
                node instanceof com.arcadeblocks.ui.SaveGameView) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Перезапустить игру (полный перезапуск процесса)
     */
    public void restartGame() {
        // Запускаем в отдельном потоке, чтобы не блокировать UI
        new Thread(() -> {
            try {
                // System.out.println("Начинаем перезапуск игры...");
                
                // КРИТИЧНО: Сначала останавливаем все, что может держать ресурсы
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Останавливаем игровой движок
                        FXGL.getGameController().pauseEngine();
                        
                        // Останавливаем и очищаем аудио
                        if (audioManager != null) {
                            audioManager.stopMusic();
                            audioManager.cleanup();
                        }
                        
                        // КРИТИЧНО: Принудительно очищаем все UI компоненты с cleanup
                        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
                        for (var node : uiNodes) {
                            if (node instanceof SupportsCleanup) {
                                try {
                                    ((SupportsCleanup) node).cleanup();
                                } catch (Exception ignored) {}
                            }
                        }
                        
                        // Очищаем UI
                        clearUINodesSafely();
                        
                        // Очищаем игровой мир
                        try {
                            FXGL.getGameWorld().getEntities().forEach(entity -> {
                                try {
                                    entity.removeFromWorld();
                                } catch (Exception ignored) {}
                            });
                        } catch (Exception ignored) {}
                        
                        // КРИТИЧНО: Очищаем кэш изображений
                        try {
                            ImageCache.clear();
                        } catch (Exception ignored) {}
                        
                        // Принудительный сбор мусора
                        System.gc();
                        
                    } catch (Exception e) {
                        System.err.println("Ошибка при очистке перед перезапуском: " + e.getMessage());
                    }
                });
                
                // Даем время на очистку
                Thread.sleep(500);
                
                // Получаем путь к Java
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
                
                // Получаем classpath
                String classpath = System.getProperty("java.class.path");
                
                // Получаем главный класс
                String mainClass = ArcadeBlocksApp.class.getName();
                
                // Создаем команду для запуска
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(javaBin);
                
                // Добавляем JVM аргументы если они были
                java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                java.util.List<String> jvmArgs = runtimeMxBean.getInputArguments();
                for (String arg : jvmArgs) {
                    // Пропускаем агенты отладки
                    if (!arg.contains("-agentlib") && !arg.contains("-javaagent")) {
                        command.add(arg);
                    }
                }
                
                command.add("-cp");
                command.add(classpath);
                command.add(mainClass);
                
                // Создаем процесс
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.inheritIO();
                
                // System.out.println("Запускаем новый процесс...");
                
                // Запускаем новый процесс
                Process newProcess = builder.start();
                
                // Даем больше времени новому процессу для инициализации
                Thread.sleep(2000);
                
                // Проверяем, что новый процесс запустился
                if (newProcess.isAlive()) {
                    // System.out.println("Новый процесс запущен успешно. Завершаем старый процесс...");
                } else {
                    // System.err.println("ВНИМАНИЕ: Новый процесс не запустился!");
                }
                
                // Закрываем JavaFX
                javafx.application.Platform.runLater(() -> {
                    javafx.application.Platform.exit();
                });
                
                // Даем время JavaFX завершиться
                Thread.sleep(1000);
                
                // System.out.println("Принудительное завершение старого процесса...");
                
                // Принудительное завершение
                Runtime.getRuntime().halt(0);
                
            } catch (Exception e) {
                System.err.println("Критическая ошибка при перезапуске игры: " + e.getMessage());
                e.printStackTrace();
                
                // В случае ошибки принудительно завершаем процесс
                Runtime.getRuntime().halt(1);
            }
        }, "GameRestartThread").start();
    }
    
    /**
     * Get video backend factory for video playback
     */
    public com.arcadeblocks.video.VideoBackendFactory getVideoBackendFactory() {
        return videoBackendFactory;
    }
}
=======
package com.arcadeblocks;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.logging.ConsoleOutput;
import com.almasb.fxgl.logging.FileOutput;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.logging.LoggerLevel;
import com.arcadeblocks.config.AudioConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.BonusConfig;
import com.arcadeblocks.persistence.GameSnapshot;
import com.arcadeblocks.gameplay.Ball;
import com.arcadeblocks.gameplay.Brick;
import com.arcadeblocks.gameplay.Projectile;
import com.arcadeblocks.gameplay.BonusType;
import com.arcadeblocks.gameplay.Paddle;
import com.arcadeblocks.gameplay.Bonus;
import com.arcadeblocks.gameplay.Boss;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.arcadeblocks.ui.MainMenuView;
import com.arcadeblocks.ui.LCGamesJingleView;
import com.arcadeblocks.ui.BonusTimerView;
import com.arcadeblocks.ui.GameplayUIView;
import com.arcadeblocks.ui.PauseView;
import com.arcadeblocks.ui.CountdownTimerView;
import com.arcadeblocks.ui.SettingsView;
import com.arcadeblocks.ui.ChapterStoryView;
import com.arcadeblocks.ui.SupportsCleanup;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.audio.SDL2AudioManager;
import com.arcadeblocks.story.ChapterStoryData;
import com.arcadeblocks.story.StoryConfig;
import com.arcadeblocks.utils.AppDataManager;
import com.arcadeblocks.utils.SaveManager;
import com.arcadeblocks.utils.ImageCache;
import com.arcadeblocks.ui.util.UINodeCleanup;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import java.io.File;
import java.util.Arrays;
import javafx.geometry.Point2D;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.text.TextAlignment;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import javafx.scene.robot.Robot;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Main class for Arcade Blocks game
 */
public class ArcadeBlocksApp extends GameApplication {
    
    private SDL2AudioManager audioManager;
    private SaveManager saveManager;
    private com.arcadeblocks.video.VideoBackendFactory videoBackendFactory;
    private com.arcadeblocks.gameplay.Paddle paddleComponent;
    private boolean mouseClicksBlocked = false;
    private com.arcadeblocks.gameplay.LivesManager livesManager;
    private com.arcadeblocks.gameplay.ScoreManager scoreManager;
    private com.arcadeblocks.gameplay.BonusEffectManager bonusEffectManager;
    private BonusTimerView bonusTimerView;
    private GameplayUIView gameplayUIView;
    private ChapterStoryView activeChapterStoryView;
    private Group darknessOverlayGroup;
    private Rectangle darknessOverlayDimLayer;
    private ImageView darknessOverlayCapsule;
    private boolean darknessOverlayActive = false;
    private final List<Node> levelBackgroundNodes = new ArrayList<>();
    private final Set<Integer> shownChapterStoryChapters = new HashSet<>();
    private boolean alwaysShowChapterStory = false; // Debug flag: always show chapter windows
    private ApplicationMode applicationMode = ApplicationMode.RELEASE;
    
    // Key press flags
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean turboPressed = false;
    private int callBallActiveSources = 0;
    
    // Loading state flag
    private boolean isLoading = false;
    private boolean isLevelIntroActive = false;
    
    // Periodic cleanup for long gameplay sessions
    private double gameplayTimeAccumulator = 0.0;
    private static final double PERIODIC_CLEANUP_INTERVAL = 300.0; // 5 minutes
    private boolean isStoryOverlayActive = false;
    private boolean isVictorySequenceActive = false;
    private static final Duration LEVEL_FADE_DURATION = Duration.millis(450);
    private boolean launchLocked = false;
    private long launchLockToken = 0L;
    private boolean fadeOutActive = false;
    private boolean fadeOutCompleted = false;
    private final List<Runnable> fadeOutCallbacks = new ArrayList<>();
    // Список активных FadeTransition для очистки при переходе между уровнями
    private final List<FadeTransition> activeFadeTransitions = new ArrayList<>();
    private javafx.util.Duration autoLaunchDelay = Duration.seconds(5); // Задержка автозапуска мяча после смерти
    private javafx.util.Duration postFadeDelay = Duration.millis(150);
    private boolean autoLaunchScheduled = false;
    private final AtomicBoolean shutdownTriggered = new AtomicBoolean(false);

    public ArcadeBlocksApp() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> performShutdownIfNeeded(), "ArcadeBlocksShutdownHook"));
    }

    public Duration getLevelFadeDuration() {
        return LEVEL_FADE_DURATION;
    }

    public Duration getAutoLaunchDelay() {
        return autoLaunchDelay;
    }

    public Duration getPostFadeDelay() {
        return postFadeDelay;
    }

    private void scheduleAutoLaunch() {
        if (autoLaunchScheduled) {
            return;
        }
        autoLaunchScheduled = true;

        FXGL.runOnce(() -> {
            autoLaunchScheduled = false;
            if (!isLaunchLocked()) {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null && ballComponent.isAttachedToPaddle()) {
                        ballComponent.launchBall();
                    }
                }
            }
        }, autoLaunchDelay);
    }
    
    private boolean isTransitioning = false;
    private boolean isLevelCompleted = false;
    private boolean isGameOver = false;
    private int continueCount = 0;

    public int getContinueCount() {
        return continueCount;
    }

    public void incrementContinueCount() {
        this.continueCount++;
    }

    public void resetContinueCount() {
        this.continueCount = 0;
    }

    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    public void setSuppressLevelCompletionChecks(boolean suppress) {
        this.suppressLevelCompletionChecks = suppress;
    }
    
    // Setter for the GameOverView flag to optimize onUpdate
    public void setGameOverViewVisible(boolean visible) {
        this.isGameOverViewVisible = visible;
    }

    public void captureLastMusicState() {
        if (audioManager != null) {
            this.restoredMusicFile = audioManager.getCurrentMusicFile();
            this.restoredMusicTime = audioManager.getCurrentMusicPosition();
        }
    }

    public void resetBallAndPaddle() {
        boolean skipFade = bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive();

        Runnable resetTask = () -> {
            var allBalls = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.BALL);
            for (Entity ball : allBalls) {
                ball.removeFromWorld();
            }

            Entity paddle = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.PADDLE).stream()
                .findFirst().orElse(null);

            if (paddle != null) {
                double currentPaddleX = paddle.getX();
                double currentPaddleY = paddle.getY();

                com.arcadeblocks.gameplay.Paddle paddleComponent = paddle.getComponent(com.arcadeblocks.gameplay.Paddle.class);
                if (paddleComponent != null) {
                    paddleComponent.setMoveLeft(false);
                    paddleComponent.setMoveRight(false);
                    paddleComponent.setMovementBlocked(true);
                }

                resetPaddleInputFlags();

                com.almasb.fxgl.physics.PhysicsComponent paddlePhysics = paddle.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                if (paddlePhysics != null) {
                    paddlePhysics.setLinearVelocity(0, 0);
                    paddlePhysics.setBodyType(com.almasb.fxgl.physics.box2d.dynamics.BodyType.KINEMATIC);
                }

                double basePaddleWidth = GameConfig.PADDLE_WIDTH;
                if (paddleComponent != null) {
                    basePaddleWidth *= paddleComponent.getSizeMultiplier();
                }

                double defaultOffsetX = basePaddleWidth * 0.18;
                double defaultOffsetY = -GameConfig.BALL_RADIUS * 2 - 5;
                double ballX = currentPaddleX + basePaddleWidth / 2.0 + defaultOffsetX - GameConfig.BALL_RADIUS;
                double ballY = currentPaddleY + defaultOffsetY;

                Entity newBall = FXGL.spawn("ball", ballX, ballY);
                Ball ballComponent = new Ball();
                newBall.addComponent(ballComponent);

                if (bonusEffectManager != null && bonusEffectManager.isChaoticBallsActive()) {
                    ballComponent.setChaoticBall(true);
                }

                if (bonusEffectManager != null && bonusEffectManager.isWeakBallsActive()) {
                    ballComponent.setWeakBall(true);
                }

                if (bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive()) {
                ballComponent.setStickyEnabled(true);
            }

                ballComponent.setAttachedOffset(defaultOffsetX, defaultOffsetY);
                ballComponent.attachToPaddle(paddle);

                // Unblock paddle control after 1 second delay
                FXGL.runOnce(() -> {
                    if (paddleComponent != null) {
                        paddleComponent.setMovementBlocked(false);
                    }
                    if (paddlePhysics != null) {
                        paddlePhysics.setBodyType(com.almasb.fxgl.physics.box2d.dynamics.BodyType.KINEMATIC);
                        paddlePhysics.setLinearVelocity(0, 0);
                    }
                }, Duration.seconds(1));
                
                // Play fade in animation immediately
                playPaddleBallFadeIn(skipFade);
                scheduleAutoLaunch();
            }
        };

        fadeOutPaddleAndBalls(skipFade, resetTask);
    }

    public void continueFromGameOver() {
        // Stop Game Over music
        if (audioManager != null) {
            audioManager.stopMusic();
        }

        // Reset Game Over flags and click blocking to restore control
        setGameOver(false);
        // Reset the GameOverView flag to optimize onUpdate
        isGameOverViewVisible = false;
        mouseClicksBlocked = false;

        // Handle score and continue count
        int cost = (getContinueCount() + 1) * 10000;
        if (scoreManager != null) {
            scoreManager.setPersistenceEnabled(true);
            scoreManager.addScore(-cost);
            FXGL.set("score", Math.max(0, scoreManager.getCurrentScore()));
            scoreManager.flushPendingOperations();
        }
        incrementContinueCount();

        // Reset lives
        if (livesManager != null) {
            livesManager.resetLives();
        }

        // Reset UI and cursor
        clearUINodesSafely();
        gameplayUIView = new GameplayUIView(this);
        FXGL.getGameScene().addUINode(gameplayUIView);
        if (scoreManager != null && scoreManager.getBonusIndicator() != null) {
            FXGL.getGameScene().addUINode(scoreManager.getBonusIndicator());
        }
        setHiddenCursor();

        int currentLevel = FXGL.geti("level");
        com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(currentLevel);
        String levelName = levelData != null ? levelData.getName() : null;
        if (gameplayUIView != null) {
            gameplayUIView.updateLevel(currentLevel, levelName);
            if (scoreManager != null) {
                gameplayUIView.updateScore(scoreManager.getCurrentScore());
            }
            if (livesManager != null) {
                gameplayUIView.updateLives(Math.max(0, livesManager.getCurrentLives()));
            }
        }
        if (scoreManager != null) {
            scoreManager.updateLevel(currentLevel, levelName);
        }

        // Reset game world state
        resetBallAndPaddle();

        // Play countdown sound
        if (audioManager != null) {
            audioManager.playSFX("sounds/menu_select.wav");
        }

        // Start countdown (which will handle resuming the music)
        showCountdownTimerForLevelStart(FXGL.geti("level"));
    }
    
    // Pause state flag
    private boolean isPaused = false;
    
    // Flag for settings from pause menu
    private boolean inPauseSettings = false;
    
    // Flag for countdown timer
    private boolean isCountdownActive = false;
    
    // Initially selected save slot (for creating new save after Game Over)
    private int originalSaveSlot = 1;
    
    // Current main menu background (for preserving during navigation)
    private String currentMainMenuBackground = null;
    
    // Flag to prevent repeated level completion messages
    private boolean levelCompletedMessageShown = false;

    // Flag to prevent repeated calls to proceedToNextLevel
    private boolean proceedToNextLevelCalled = false;
    private Integer pendingLevelWarpTarget = null;
    private boolean isDebugMode = false; // Debug mode flag
    // Flag to optimize GameOverView checking in onUpdate (instead of stream)
    private volatile boolean isGameOverViewVisible = false;
    private com.arcadeblocks.config.DifficultyLevel debugDifficultyOverride = null;
    private Integer debugLivesOverride = null;
    private Integer debugScoreOverride = null;

    // Level completion bonus
    private boolean levelPassBonusSpawned = false;
    private Entity levelPassBonusEntity;
    
    // Cursor status and forced timer
    private boolean cursorHidden = false;
    private javafx.animation.Timeline cursorEnforcer;
    
    // Video player tracking (now handled by VlcContext)
    
    // Tracking active PauseTransitions for videos (critical for preventing leaks)
    private final java.util.List<javafx.animation.PauseTransition> activeVideoPauseTransitions = new java.util.ArrayList<>();
    
    // Token to prevent race conditions when quickly restarting levels via the debug menu.
    // Each time video resources are cleaned up, the token is incremented, allowing old callbacks to understand
    // that they belong to an outdated video session and should not perform final cleanup.
    private volatile long videoSessionToken = 0;
    
    /**
     * Wrapper for tracking video overlay along with its backend
     * need to store a reference to the backend for proper cleanup when removing the overlay.
     */
    private static class VideoOverlayWrapper {
        final javafx.scene.Node overlay;
        final com.arcadeblocks.video.VideoPlayerBackend backend;
        
        VideoOverlayWrapper(javafx.scene.Node overlay, com.arcadeblocks.video.VideoPlayerBackend backend) {
            this.overlay = overlay;
            this.backend = backend;
        }
    }
    
    // Tracking active video overlays for cleaning
    private final java.util.List<VideoOverlayWrapper> activeVideoOverlays = new java.util.ArrayList<>();
    
    // Mouse control for paddle
    private boolean mouseHandlersInstalled = false;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseMoveHandler;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseDragHandler;
    // Jerk suppression: ignore first mouse movement after keyboard input
    private boolean suppressMouseUntilMove = false;
    
    // Cursor confinement within game window
    private boolean confineCursorEnabled = false;
    private boolean confineFocusListenerInstalled = false;
    private boolean isWarpingMouse = false;
    private java.awt.Robot awtRobot;
    private javafx.animation.Timeline confineTicker;
    private javafx.animation.Timeline mouseFollowTicker;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseExitHandler;
    private AWTEventListener globalMouseConfineListener;
    private final AtomicBoolean confineRequestScheduled = new AtomicBoolean(false);
    private Robot fxRobot;
    private boolean pauseResumeLockFromFocusLoss = false;
    private boolean suppressLevelCompletionChecks = false;
    private boolean vsyncEnabled = true;
    private boolean vsyncConfigured = false;

    // Input Actions
    private UserAction paddleLeftAction;
    private UserAction paddleRightAction;
    private UserAction launchBallAction;
    private UserAction turboPaddleAction;
    private UserAction callBallAction;
    private UserAction mouseCallBallAction;
    private UserAction plasmaWeaponAction;
    private UserAction pauseAction;
    private UserAction mouseLaunchBallAction;
    private UserAction mousePlasmaWeaponAction;
    // private UserAction destroyAllBricksAction;
    // private UserAction skipToLevel100Action;
    
    // Current level audio settings
    private String currentLevelLoadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
    private List<String> currentBrickHitSounds = new ArrayList<>(AudioConfig.getDefaultBrickHitSounds());
    private List<String> currentLevelCompletionSounds = new ArrayList<>(AudioConfig.getDefaultLevelCompletionSounds());
    private final Random levelAudioRandom = new Random();

    // State for restoring music
    private String restoredMusicFile = null;
    private double restoredMusicTime = 0.0;
    
    @Override
    protected void onPreInit() {
        configureRuntimeStorage();
    }
    
    private void configureRuntimeStorage() {
        try {
            // Disable file logging to avoid permission issues in Program Files
            // Only use console output for logging
            Logger.removeAllOutputs();
            var consoleLevel = applicationMode != null ? applicationMode.getLoggerLevel() : LoggerLevel.INFO;
            Logger.addOutput(new ConsoleOutput(), consoleLevel);
            
            // Ensure data directories exist
            AppDataManager.getLogsDirectory();
            AppDataManager.getFxglDirectory();
        } catch (Exception ex) {
            System.err.println("Unable to reconfigure logging: " + ex.getMessage());
        }
    }
    
    @Override
    protected void initSettings(GameSettings settings) {
        // Disable file logging to avoid permission issues
        settings.setFileSystemWriteAllowed(false);
        
        // Load the saved resolution from the database
        com.arcadeblocks.config.Resolution savedResolution = loadResolutionFromDatabase();
        GameConfig.setCurrentResolution(savedResolution);
        
        // Set the window size according to the saved resolution.
        settings.setWidth(savedResolution.getWidth());
        settings.setHeight(savedResolution.getHeight());
        settings.setTitle("Arcade Blocks");
        settings.setVersion("1.19.1");
        settings.setMainMenuEnabled(false);
        
        // Setting up the app icon
        settings.setAppIcon("favicon-32.png");
        settings.setGameMenuEnabled(false);
        settings.setIntroEnabled(false);
        settings.setProfilingEnabled(false);
        settings.setCloseConfirmation(false); // Disable standard confirmation, handle manually
        
        // Fullscreen mode - load settings from database
        settings.setFullScreenAllowed(true);
        
        // Load window mode settings from database
        boolean startInFullscreen = loadWindowModeFromDatabase();
        settings.setFullScreenFromStart(startInFullscreen);
        
        // Performance settings
        settings.setPreserveResizeRatio(true);   // Preserve aspect ratio
        settings.setManualResizeEnabled(false);  // BLOCK manual resizing
        settings.setScaleAffectedOnResize(false); // DO NOT scale content
        vsyncEnabled = loadVSyncFromDatabase();
        applyVSyncFlag(vsyncEnabled);
        settings.setTicksPerSecond(-1); // Use JavaFX pulse (vsync) by default
        
        // Window centering settings (if supported)
        // settings.setWindowCenteredOnStartup(true); // Not supported in this FXGL version
        
        // Set window style for borderless mode switching capability
        // settings.setWindowStyle(WindowStyle.UNDECORATED); // Not supported in this FXGL version
        
        // Custom FXGL cursor disabled - system cursor will be used
        
        applicationMode = settings.getApplicationMode();
    }
    
    /**
     * Load resolution settings from database
     * @return saved resolution or default resolution
     */
    private com.arcadeblocks.config.Resolution loadResolutionFromDatabase() {
        com.arcadeblocks.utils.DatabaseManager tempDb = null;
        try {
            // Создаем временный DatabaseManager для чтения настроек
            tempDb = new com.arcadeblocks.utils.DatabaseManager();
            tempDb.initialize();
            
            // Load resolution settings
            String resolutionValue = tempDb.getSetting("resolution");
            
            if (resolutionValue != null && !resolutionValue.isEmpty()) {
                com.arcadeblocks.config.Resolution resolution = com.arcadeblocks.config.Resolution.fromString(resolutionValue);
                // System.out.println("Loaded resolution from database: " + resolution);
                return resolution;
            } else {
                // System.out.println("Resolution not found in database, using default: " + GameConfig.DEFAULT_RESOLUTION);
                return GameConfig.DEFAULT_RESOLUTION;
            }
        } catch (Exception e) {
            System.err.println("Error loading resolution from database: " + e.getMessage());
            return GameConfig.DEFAULT_RESOLUTION;
        } finally {
            // ВАЖНО: закрываем соединение с БД
            if (tempDb != null) {
                tempDb.close();
            }
        }
    }

    private boolean loadVSyncFromDatabase() {
        com.arcadeblocks.utils.DatabaseManager tempDb = null;
        try {
            tempDb = new com.arcadeblocks.utils.DatabaseManager();
            tempDb.initialize();
            String vsyncValue = tempDb.getSetting("vsync_enabled");
            if (vsyncValue == null) {
                vsyncValue = tempDb.getSetting("fps_limit_enabled");
            }
            if (vsyncValue != null) {
                return Boolean.parseBoolean(vsyncValue);
            }
        } catch (Exception e) {
            System.err.println("Error loading vsync setting from database: " + e.getMessage());
        } finally {
            // ВАЖНО: закрываем соединение с БД
            if (tempDb != null) {
                tempDb.close();
            }
        }
        return true;
    }
    
    /**
     * Load window mode settings from database
     * @return true if should start in fullscreen mode
     */
    private boolean loadWindowModeFromDatabase() {
        com.arcadeblocks.utils.DatabaseManager tempDb = null;
        try {
            // Создаем временный DatabaseManager для чтения настроек
            tempDb = new com.arcadeblocks.utils.DatabaseManager();
            tempDb.initialize();
            
            // Load window mode settings
            String windowedModeValue = tempDb.getSetting("windowed_mode");
            String fullscreenValue = tempDb.getSetting("fullscreen");
            
            boolean isWindowed = windowedModeValue != null ? Boolean.parseBoolean(windowedModeValue) : false;
            boolean isFullscreen = fullscreenValue != null ? Boolean.parseBoolean(fullscreenValue) : true;
            
            // If windowed mode is enabled, start in windowed mode
            if (isWindowed) {
        // System.out.println("Settings loaded: starting in windowed mode");
                return false;
            } else if (isFullscreen) {
        // System.out.println("Settings loaded: starting in fullscreen mode");
                return true;
            } else {
                // Default to fullscreen mode
        // System.out.println("Settings loaded: starting in fullscreen mode (default)");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error loading window mode settings: " + e.getMessage());
            // In case of error, use fullscreen mode by default
        // System.out.println("Using default fullscreen mode due to error");
            return true;
        } finally {
            // ВАЖНО: закрываем соединение с БД
            if (tempDb != null) {
                tempDb.close();
            }
        }
    }
    
    @Override
    protected void initGame() {
        // Register entity factory
        FXGL.getGameWorld().addEntityFactory(new ArcadeBlocksFactory());
        
        // Initialize game properties
        FXGL.set("score", 0);
        FXGL.set("level", 1);
        
        // Initialize managers
        audioManager = new SDL2AudioManager();
        saveManager = new SaveManager();
        
        // Initialize video backend factory
        videoBackendFactory = new com.arcadeblocks.video.VideoBackendFactory();
        // System.out.println(videoBackendFactory.getBackendInfo());

        // Initialize input actions and load bindings now that saveManager is available.
        initInputActions();
        loadInputBindings();
        
        // Initialize audio system
        // System.out.println("Initializing audio system...");
        
        // Load settings
        saveManager.loadSettings();

        // Apply saved language
        LocalizationManager.getInstance().setLanguage(saveManager.getLanguage());
        
        // Initialize control keys after saveManager initialization
        loadInputBindings();
        
        // Load debug menu settings
        saveManager.loadDebugSettingsToBonusConfig();
        
        // Configure audio
        audioManager.setMasterVolume(saveManager.getMasterVolume());
        audioManager.setMusicVolume(saveManager.getMusicVolume());
        audioManager.setSfxVolume(saveManager.getSfxVolume());
        audioManager.setSoundEnabled(saveManager.isSoundEnabled());

        javafx.application.Platform.runLater(this::applySavedVSync);
        
        // Preload frequently used sounds
        if (audioManager.isInitialized()) {
            audioManager.preloadCommonSounds();
        }
        
        // Force set system cursor instead of blue FXGL cursor
        setSystemCursor();

        // Monitor UI layer changes to automatically center new elements
        FXGL.getGameScene().getUINodes().addListener((ListChangeListener<javafx.scene.Node>) change -> centerAllUINodes());
        centerAllUINodes();
        
        // Bonus statistics output
        // System.out.println("=== BONUS SETTINGS ===");
        // System.out.println(BonusConfig.getBonusStatistics());
        // System.out.println("Included bonuses: " + BonusType.getEnabledBonusesCount());
        // System.out.println("Positive: " + BonusType.getEnabledPositiveBonusesCount());
        // System.out.println("Negative: " + BonusType.getEnabledNegativeBonusesCount());
        // System.out.println("========================");
        
        // Start initial loading sequence (jingle + loading screen)
        isLoading = true; // Set loading flag
        startInitialLoadingSequence();
        
        // Apply fullscreen mode settings and window close handler (after UI initialization)
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                if (stage != null) {
                    // Apply window settings
                    applyWindowSettings();
                    
                    // Window close handler
                    stage.setOnCloseRequest(event -> {
        // System.out.println("The window close handler has been called!"); // Debugging
                        event.consume(); // Cancel standard close
                        
                        // Block exit during loading
                        if (!isLoading) {
                            handleWindowClose();
                        }
                    });
                    
                    // Additional handler for reliability
                    stage.setOnHiding(event -> {
        // System.out.println("The window is hiding!"); // Debugging
                        event.consume();
                        
                        // Block exit during loading
                        if (!isLoading) {
                            handleWindowClose();
                        }
                    });
                }
            } catch (Exception e) {
                // System.err.println("Failed to apply window settings: " + e.getMessage());
            }
        });
        
        // Additional handler setup after loading completion
        javafx.animation.Timeline setupHandlerTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5.5), e -> {
                // Set window close handler after loading completion
                javafx.application.Platform.runLater(() -> {
                    try {
                        Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                        if (stage != null) {
                            // Update window close handler
                            stage.setOnCloseRequest(event -> {
        // System.out.println("Window close handler called (after loading)!"); // Debugging
                                event.consume(); // Cancel standard close
                                handleWindowClose();
                            });
                            
                            // Update additional handler
                            stage.setOnHiding(event -> {
        // System.out.println("The window disappears (after loading)!"); // Debugging
                                event.consume();
                                handleWindowClose();
                            });
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to update window handler: " + ex.getMessage());
                    }
                });
            })
        );
        setupHandlerTimeline.play();
    }
    
    private void startInitialLoadingSequence() {
        try {
            LCGamesJingleView jingleView = new LCGamesJingleView(() ->
                javafx.application.Platform.runLater(this::showLoadingViewAfterJingle)
            );
            FXGL.getGameScene().addUINode(jingleView);
        } catch (Exception e) {
            System.err.println("Failed to start LCGames jingle: " + e.getMessage());
            javafx.application.Platform.runLater(this::showLoadingViewAfterJingle);
        }
    }
    
    private void showLoadingViewAfterJingle() {
        // LCGamesJingleView should already be removed via its callback in playAnimation()
        // But just in case, check and remove if it still exists
        var scene = FXGL.getGameScene();
        var existingNodes = scene.getUINodes();
        for (javafx.scene.Node node : existingNodes) {
            if (node instanceof com.arcadeblocks.ui.LCGamesJingleView) {
                try {
                    ((com.arcadeblocks.ui.LCGamesJingleView) node).cleanup();
                    scene.removeUINode(node);
                } catch (Exception ignored) {
                    // Ignore errors, just try to remove
                    try {
                        scene.removeUINode(node);
                    } catch (Exception ignored2) {}
                }
                break; // Remove only the first one found
            }
        }
        
        com.arcadeblocks.ui.LoadingView loadingView = new com.arcadeblocks.ui.LoadingView(this);
        loadingView.setOpacity(0.0);
        FXGL.getGameScene().addUINode(loadingView);
        
        // We save the reference to FadeTransition in LoadingView to stop it during cleanup().
        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(400), loadingView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            // Clear reference after animation completion
            fadeIn.setOnFinished(null);
        });
        loadingView.setExternalFadeIn(fadeIn);
        fadeIn.play();
        
        // Save the reference to loadingView for use in callbacks.
        // This is necessary to avoid capturing the reference in lambda, which may hold the object.
        final com.arcadeblocks.ui.LoadingView loadingViewRef = loadingView;
        loadingView.setOnSkipCallback(() -> {
            // КРИТИЧНО: Проверяем, что loadingView еще существует в сцене
            if (FXGL.getGameScene().getUINodes().contains(loadingViewRef)) {
                completeLoading(loadingViewRef);
            }
        });
        
        loadingView.setOnCompleteCallback(() -> {
            // Check that loadingView still exists in the scene
            if (FXGL.getGameScene().getUINodes().contains(loadingViewRef)) {
                // Do not pass loadingView to startMainMenuMusic to avoid memory leaks.
                startMainMenuMusic();
                transitionToMainMenu(loadingViewRef);
            }
        });
        
        // Automatic loading completion after 4 seconds if user didn't skip the screen
        // Save the link to Timeline in LoadingView to stop it during cleanup()
        javafx.animation.Timeline loadingTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(4.0), e -> {
                // Check that LoadingView still exists in the scene before calling completeLoading()
                // This prevents memory leaks and errors when LoadingView is removed prematurely
                if (FXGL.getGameScene().getUINodes().contains(loadingView) && isLoading) {
                    isLoading = false;
                    loadingView.completeLoading();
                }
                // If LoadingView is already removed, Timeline will just finish without calling completeLoading()
            })
        );
        loadingView.setExternalTimeline(loadingTimeline);
        loadingTimeline.play();
    }
    
    @Override
    protected void initUI() {
        // Load CSS styles for dark background
        try {
            String cssPath = "/dark-theme.css";
            FXGL.getGameScene().getRoot().getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Failed to load CSS styles: " + e.getMessage());
        }
        
        // Set dark background for window (remove white bars)
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.Scene scene = FXGL.getGameScene().getRoot().getScene();
                if (scene != null) {
                    // Set dark background matching game design
                    scene.setFill(javafx.scene.paint.Color.web(GameConfig.DARK_BACKGROUND));
                    
                    // Also set style for entire window
                    scene.getRoot().setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
                    
                    // Set background for game window itself
                    FXGL.getGameScene().getRoot().setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
                    
                    // Set background for FXGL scene as well
                    FXGL.getGameScene().setBackgroundColor(javafx.scene.paint.Color.web(GameConfig.DARK_BACKGROUND));
                    
                    // Additionally set style for Stage
                    Stage stage = (Stage) scene.getWindow();
                    if (stage != null) {
                        stage.getScene().getRoot().setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
                        
                        // Icon is set via FXGL settings.setAppIcon()
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to set background color: " + e.getMessage());
            }
        });
    }
    
    @Override
    protected void initPhysics() {
        // Disable gravity for weightlessness effect
        FXGL.getPhysicsWorld().setGravity(0, 0);
        
        // FXGL physics engine (Box2D) works with improved accuracy
        // thanks to CCD (Continuous Collision Detection) enabled for the ball
        // and increased number of checks in handleContinuousBrickCollisions()
        // System.out.println("Physics engine initialized with CCD to prevent tunneling");
        
        // Configure collisions
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity paddle) {
                // Handle ball collision with paddle
                Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    ballComponent.onPaddleHit(paddle);
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.BRICK) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity brick) {
                // Handle ball collision with brick
                Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    // If ball is energy ball, it passes through the brick
                    if (ballComponent.isEnergyBall()) {
                        ballComponent.onBrickHit(brick);
                        return; // Don't process physical collision
                    }
                    ballComponent.onBrickHit(brick);
                }
            }
        });
        
        // Ball-ball collision handler (disable for attached balls)
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.BALL) {
            @Override
            protected void onCollisionBegin(Entity ball1, Entity ball2) {
                Ball ball1Component = ball1.getComponent(Ball.class);
                Ball ball2Component = ball2.getComponent(Ball.class);
                
                if (ball1Component != null && ball2Component != null) {
                    // If one of the balls is attached to paddle, ignore collision
                    if (ball1Component.isAttachedToPaddle() || ball2Component.isAttachedToPaddle()) {
                        return; // Ignore collision
                    }
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.POWERUP, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity powerup, Entity paddle) {
                // Handle powerup pickup
                // Logic will be implemented via components
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BONUS, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity bonus, Entity paddle) {
                // Handle bonus pickup
                com.arcadeblocks.gameplay.Bonus bonusComponent = bonus.getComponent(com.arcadeblocks.gameplay.Bonus.class);
                if (bonusComponent != null) {
                    bonusComponent.collect();
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PROJECTILE, EntityType.PADDLE) {
            @Override
            protected void onCollisionBegin(Entity projectile, Entity paddle) {
                Projectile projectileComponent = projectile.getComponent(Projectile.class);
                if (projectileComponent != null && projectileComponent.isBossProjectile()) {
                    handleBossProjectileHit(projectileComponent, projectile);
                } else {
                    // Ignore instant collision of player with their own paddle (first milliseconds)
                    if (projectileComponent != null && projectileComponent.shouldIgnorePaddleCollision()) {
                        return;
                    }
                    projectile.removeFromWorld();
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PROJECTILE, EntityType.BRICK) {
            @Override
            protected void onCollisionBegin(Entity projectile, Entity brick) {
                // Handle projectile collision with brick
                Projectile projectileComponent = projectile.getComponent(Projectile.class);
                if (projectileComponent != null) {
                    // Destroy brick
                    Brick brickComponent = brick.getComponent(Brick.class);
                    if (brickComponent != null) {
                        // Check if brick is explosive
                        if (brickComponent.isExplosive()) {
                            // If brick is explosive, trigger explosion before destruction
                            brickComponent.explodeNearbyBricks();
                        }
                        // Use destroy() method from Brick for proper destruction animation
                        brickComponent.destroy();
                    }
                    
                    // Remove projectile
                    projectile.removeFromWorld();
                }
            }
        });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PROJECTILE, EntityType.BALL) {
            @Override
            protected void onCollisionBegin(Entity projectile, Entity ball) {
                Projectile projectileComponent = projectile.getComponent(Projectile.class);
                Ball ballComponent = ball.getComponent(Ball.class);
                
                if (projectileComponent != null && ballComponent != null) {
                    // Check if projectile is player's plasma weapon (not boss)
                    boolean isPlasmaWeapon = !projectileComponent.isBossProjectile() && 
                                             "player".equalsIgnoreCase(projectileComponent.getOwner());
                    
                    if (isPlasmaWeapon) {
                        // If this is plasma weapon and hit ball - count life loss
                        // Check if ball is not an extra ball
                        if (!ballComponent.isExtraBall()) {
                            // Check if life loss was already in progress BEFORE calling loseLife()
                            boolean wasLifeLossInProgress = getLivesManager() != null && 
                                getLivesManager().isLifeLossInProgress();
                            
                            // Trigger life loss via LivesManager
                            if (getLivesManager() != null) {
                                getLivesManager().loseLife();
                            }
                            
                            // If life loss was already in progress (lifeLossInProgress was true),
                            // then loseLife() was ignored, and we need to clear bonuses directly.
                            // This ensures bonuses will be cleared even if loseLife() didn't execute.
                            if (wasLifeLossInProgress) {
                                // Life loss already in progress - clear bonuses immediately,
                                // so they are guaranteed to disappear from screen
                                fadeOutBonuses(false, () -> {
                                    if (getBonusEffectManager() != null) {
                                        getBonusEffectManager().clearAllBonuses();
                                        getBonusEffectManager().resetAllEffects();
                                    }
                                    // Also remove all falling bonuses from world
                                    var bonusEntities = new java.util.ArrayList<>(
                                        FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
                                    for (Entity bonusEntity : bonusEntities) {
                                        bonusEntity.removeFromWorld();
                                    }
                                });
                            }
                        } else {
                            // Remove extra ball with smooth fade out animation
                            fadeOutBall(ball);
                        }
                        // Remove projectile
                        projectile.removeFromWorld();
                        return;
                    }
                }
                
                // For all other projectiles (e.g., boss projectiles) simply remove without consequences
                projectile.removeFromWorld();
            }
        });
        
        // Temporarily disable FXGL CollisionHandler for debugging
        // FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.BOSS) {
        //     @Override
        //     protected void onCollisionBegin(Entity ball, Entity boss) {
        //         System.out.println("FXGL CollisionHandler: the ball collided with the boss");
        //         Ball ballComponent = ball.getComponentOptional(Ball.class).orElse(null);
        //         Boss bossComponent = boss.getComponentOptional(Boss.class).orElse(null);
        //         if (ballComponent == null || bossComponent == null) {
        //             System.out.println("FXGL CollisionHandler: components not found");
        //             return;
        //         }
        //         System.out.println("FXGL CollisionHandler: deal damage to the boss: " + ballComponent.getBossDamage());
        //         bossComponent.takeDamage(ballComponent.getBossDamage());
        //     }
        // });
        
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL) {
            @Override
            protected void onCollisionBegin(Entity ball, Entity wall) {
                // Handle ball collision with wall
                Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    // Check if this is a protective wall
                    try {
                        Boolean isProtectiveWall = wall.getBoolean("isProtectiveWall");
                        if (isProtectiveWall != null && isProtectiveWall) {
                            // Reflect ball from protective wall
                            ballComponent.onWallHit(wall);
                        }
                    } catch (Exception e) {
                        // If property doesn't exist, this is a regular wall - ignore
                        // Regular walls are already handled by physics engine
                    }
                }
            }
        });
    }
    
    @Override
    protected void initInput() {
        // Actions are initialized in initGame() after SaveManager is created.
    }

    private void initInputActions() {
        paddleLeftAction = new UserAction("Paddle Left") {
            @Override
            protected void onActionBegin() {
                leftPressed = true;
                // System.out.println("[Input] LEFT pressed = true");
            }
            @Override
            protected void onActionEnd() {
                leftPressed = false;
                // System.out.println("[Input] LEFT pressed = false");
            }
        };

        paddleRightAction = new UserAction("Paddle Right") {
            @Override
            protected void onActionBegin() {
                rightPressed = true;
                // System.out.println("[Input] RIGHT pressed = true");
            }
            @Override
            protected void onActionEnd() {
                rightPressed = false;
                // System.out.println("[Input] RIGHT pressed = false");
            }
        };

        launchBallAction = new UserAction("Launch Ball") {
            @Override
            protected void onActionBegin() {
                if (!canProcessLaunchInput()) return;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null && ballComponent.isAttachedToPaddle()) {
                        ballComponent.launchBall();
                    }
                }
            }
        };

        callBallAction = new UserAction("Call Ball") {
            @Override
            protected void onActionBegin() {
                activateCallBall();
            }

            @Override
            protected void onActionEnd() {
                deactivateCallBall();
            }
        };

        turboPaddleAction = new UserAction("Turbo Paddle") {
            @Override
            protected void onActionBegin() {
                turboPressed = true;
            }
            @Override
            protected void onActionEnd() {
                turboPressed = false;
            }
        };

        plasmaWeaponAction = new UserAction("Plasma Weapon") {
            @Override
            protected void onActionBegin() {
                if (bonusEffectManager != null && bonusEffectManager.isPlasmaWeaponActive()) {
                    bonusEffectManager.firePlasmaShot();
                }
            }
        };

        pauseAction = new UserAction("Pause") {
            @Override
            protected void onActionBegin() {
                if (!isGameplayState() || isLoading || isLevelIntroActive || isStoryOverlayActive || isCountdownActive || isLevelCompleted || isVictorySequenceActive) return;
                if (FXGL.getGameScene().getUINodes().stream().anyMatch(node -> node instanceof com.arcadeblocks.ui.GameOverView)) return;
                // Check for the presence of PauseView in the scene before switching the pause
                var scene = FXGL.getGameScene();
                boolean hasPauseView = scene.getUINodes().stream()
                    .anyMatch(node -> node instanceof com.arcadeblocks.ui.PauseView);
                if (hasPauseView || isPaused) {
                    resumeGame();
                } else {
                    pauseGame();
                }
            }
        };

        mouseLaunchBallAction = new UserAction("Mouse Launch") {
            @Override
            protected void onActionBegin() {
                if (!canProcessLaunchInput()) return;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : balls) {
                    Ball ballComponent = ballEntity.getComponent(Ball.class);
                    if (ballComponent != null && ballComponent.isAttachedToPaddle()) {
                        ballComponent.launchBall();
                    }
                }
            }
        };

        mouseCallBallAction = new UserAction("Mouse Call Ball") {
            @Override
            protected void onActionBegin() {
                if (mouseClicksBlocked) return;
                activateCallBall();
            }

            @Override
            protected void onActionEnd() {
                deactivateCallBall();
            }
        };

        mousePlasmaWeaponAction = new UserAction("Mouse Plasma Weapon") {
            @Override
            protected void onActionBegin() {
                if (mouseClicksBlocked) return;
                if (bonusEffectManager != null && bonusEffectManager.isPlasmaWeaponActive()) {
                    bonusEffectManager.firePlasmaShot();
                }
            }
        };

        // destroyAllBricksAction = new UserAction("Debug Destroy Bricks") {
        //     @Override
        //     protected void onActionBegin() {
        //         if (!canTriggerDebugHotkey()) {
        //             return;
        //         }
        //         triggerDestroyAllBricksCheat();
        //     }
        // };

        // skipToLevel100Action = new UserAction("Debug Warp To Level 100") {
        //     @Override
        //     protected void onActionBegin() {
        //         if (!canTriggerDebugHotkey()) {
        //             return;
        //         }
        //         triggerWarpToLevel100Cheat();
        //     }
        // };

        // Add all actions to the input system once with a dummy key
        // This registers the actions so they can be rebound later.
        FXGL.getInput().addAction(paddleLeftAction, KeyCode.F13);
        FXGL.getInput().addAction(paddleRightAction, KeyCode.F14);
        FXGL.getInput().addAction(launchBallAction, KeyCode.F15);
        FXGL.getInput().addAction(turboPaddleAction, KeyCode.F16);
        FXGL.getInput().addAction(plasmaWeaponAction, KeyCode.F17);
        FXGL.getInput().addAction(pauseAction, KeyCode.F18);
        FXGL.getInput().addAction(callBallAction, KeyCode.F19);
        FXGL.getInput().addAction(mouseLaunchBallAction, MouseButton.PRIMARY);
        FXGL.getInput().addAction(mouseCallBallAction, MouseButton.MIDDLE);
        FXGL.getInput().addAction(mousePlasmaWeaponAction, MouseButton.SECONDARY);
        // FXGL.getInput().addAction(destroyAllBricksAction, KeyCode.P);
        // FXGL.getInput().addAction(skipToLevel100Action, KeyCode.L);
    }
    
    /**
     * Load key bindings from settings
     */
    private void loadInputBindings() {
        try {
            rebindAction("MOVE_LEFT", paddleLeftAction);
            rebindAction("MOVE_RIGHT", paddleRightAction);
            rebindAction("LAUNCH", launchBallAction);
            rebindAction("CALL_BALL", callBallAction);
            rebindAction("TURBO_PADDLE", turboPaddleAction);
            rebindAction("PLASMA_WEAPON", plasmaWeaponAction);
            rebindAction("PAUSE", pauseAction);

        } catch (Exception e) {
            System.err.println("Error loading/rebinding control settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void rebindAction(String actionName, UserAction action) {
        String key = saveManager.getControlKey(actionName);
        if (isValidKey(key)) {
            try {
                FXGL.getInput().rebind(action, KeyCode.valueOf(key));
            }
            catch (IllegalArgumentException e) {
                System.err.println("Failed to rebind action " + actionName + " to key " + key + ": " + e.getMessage());
            }
        } else {
            System.err.println("[Input] Invalid key for " + actionName + ": " + key);
        }
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isEmpty() && !key.equals("...");
    }

    private boolean canTriggerDebugHotkey() {
        return isGameplayState()
            && !isPaused
            && !isLoading
            && !isLevelIntroActive
            && !isStoryOverlayActive
            && !isCountdownActive
            && !isLevelCompleted
            && !isTransitioning
            && !isVictorySequenceActive
            && !isGameOver;
    }

    // private void triggerDestroyAllBricksCheat() {
    //     destroyAllBricks();
    // }

    // private void triggerWarpToLevel100Cheat() {
    //     if (isDebugMode) {
    //         destroyAllBricks();
    //         return;
    //     }
    //     pendingLevelWarpTarget = 100;
    //     destroyAllBricks();
    // }

    private void activateCallBall() {
        if (!isGameplayState()) {
            return;
        }
        
        // КРИТИЧНО: Проверяем условия блокировки притягивания мяча
        boolean isBlocked = false;
        
        // Блокировка на хардкорной сложности
        if (getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE) {
            isBlocked = true;
        }
        
        // Блокировка на boss уровнях (кроме легкой сложности)
        if (getEffectiveDifficulty() != com.arcadeblocks.config.DifficultyLevel.EASY && 
            com.arcadeblocks.config.LevelConfig.isBossLevel(FXGL.geti("level"))) {
            isBlocked = true;
        }
        
        callBallActiveSources++;
        
        if (audioManager != null && (saveManager == null || saveManager.isCallBallSoundEnabled())) {
            if (isBlocked) {
                // Воспроизводим звук блокировки притягивания
                audioManager.playSFXByName("call_to_paddle_block");
            } else {
                // Воспроизводим обычный звук притягивания
                audioManager.playSFXByName("ball_call");
            }
        }
    }

    private void deactivateCallBall() {
        if (callBallActiveSources > 0) {
            callBallActiveSources--;
        }
    }

    private boolean isCallBallActive() {
        return callBallActiveSources > 0;
    }

    private void resetCallBallState() {
        callBallActiveSources = 0;
    }
    
    /**
     * Reload key bindings (called when settings are changed)
     * @param overrideAction Optional action to rebind (e.g., "PLASMA_WEAPON")
     * @param overrideKey Optional new key for action (e.g., "X")
     */
    public void reloadInputBindings(String overrideAction, String overrideKey) {
        if (overrideAction != null && overrideKey != null) {
            // Force rebind specific action with new key
            try {
                switch (overrideAction) {
                    case "MOVE_LEFT":
                        rebindActionWithKey("MOVE_LEFT", paddleLeftAction, overrideKey);
                        break;
                    case "MOVE_RIGHT":
                        rebindActionWithKey("MOVE_RIGHT", paddleRightAction, overrideKey);
                        break;
                    case "LAUNCH":
                        rebindActionWithKey("LAUNCH", launchBallAction, overrideKey);
                        break;
                    case "CALL_BALL":
                        rebindActionWithKey("CALL_BALL", callBallAction, overrideKey);
                        break;
                    case "TURBO_PADDLE":
                        rebindActionWithKey("TURBO_PADDLE", turboPaddleAction, overrideKey);
                        break;
                    case "PLASMA_WEAPON":
                        rebindActionWithKey("PLASMA_WEAPON", plasmaWeaponAction, overrideKey);
                        break;
                    case "PAUSE":
                        rebindActionWithKey("PAUSE", pauseAction, overrideKey);
                        break;
                }
            } catch (Exception e) {
                System.err.println("An error occurred during forced re-binding " + overrideAction + " to the key " + overrideKey + ": " + e.getMessage());
            }
        }
        
        // Reload all other bindings
        loadInputBindings();

        // After reloading the bindings, we update the UI to display the correct keys.
        if (getScoreManager() != null && getScoreManager().getBonusIndicator() != null) {
            getScoreManager().getBonusIndicator().updatePlasmaWeaponKey();
        }
    }
    
    /**
     * Reload key bindings (called when settings are changed)
     */
    public void reloadInputBindings() {
        reloadInputBindings(null, null);
    }
    
    private void rebindActionWithKey(String actionName, UserAction action, String key) {
        if (isValidKey(key)) {
            try {
                FXGL.getInput().rebind(action, KeyCode.valueOf(key));
            }
            catch (IllegalArgumentException e) {
                System.err.println("Failed to rebind action " + actionName + " to key " + key + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Reset control settings to default values
     */
    private void resetControlsToDefault() {
        // System.out.println("Control settings are reset to default values.");
        for (java.util.Map.Entry<String, String> entry : com.arcadeblocks.config.GameConfig.DEFAULT_CONTROLS.entrySet()) {
            saveManager.setControlKey(entry.getKey(), entry.getValue());
        }
        saveManager.awaitPendingWrites(); // Wait for saving all default values
    }
    
    /**
     * Show control settings error dialog
     */
    private void showControlsResetDialog() {
        javafx.application.Platform.runLater(() -> {
            com.arcadeblocks.ui.ControlsResetDialog dialog = new com.arcadeblocks.ui.ControlsResetDialog();
            FXGL.getGameScene().addUINode(dialog);
        });
    }
    
    /**
     * Update paddle speed (called when settings are changed)
     */
    public void updatePaddleSpeed() {
        double newSpeed = saveManager.getPaddleSpeed();
        
        // Update speed of all paddles in game
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        for (Entity paddleEntity : paddles) {
            Paddle paddleComponent = paddleEntity.getComponent(Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setSpeed(newSpeed);
            }
        }
        
        // Update global variable for new paddles
        if (paddleComponent != null) {
            paddleComponent.setSpeed(newSpeed);
        }
    }
    
    // Removed mouse control to restore ball stickiness
    
    // Removed cursor control
    
    // Removed cursor confinement
    
    /**
     * Force set system cursor instead of blue FXGL cursor
     */
    private void setSystemCursor() {
        cursorHidden = false;
        applyCursorStateAndEnforcer();
    }

    /**
     * Hide cursor during gameplay.
     */
    private void setHiddenCursor() {
        cursorHidden = true;
        applyCursorStateAndEnforcer();
    }

    private void applyCursorStateAndEnforcer() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene != null) {
                var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                scene.setCursor(targetCursor);
                scene.getRoot().setCursor(targetCursor);
            }
        });
        
        // Restart unified enforcer timer so cursor won't be changed by FXGL
        if (cursorEnforcer != null) {
            cursorEnforcer.stop();
        }
        cursorEnforcer = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.1), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.5), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
                    }),
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
            }),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5.0), e -> {
                javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
                if (scene != null) {
                    var targetCursor = cursorHidden ? javafx.scene.Cursor.NONE : javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(targetCursor);
                    scene.getRoot().setCursor(targetCursor);
                }
            })
        );
        cursorEnforcer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cursorEnforcer.play();
    }

    private void enableCursorConfine(boolean enable) {
        // On Wayland cursor confinement is unavailable - disable
        if (enable && isLinuxWayland()) {
            confineCursorEnabled = false;
            return;
        }
        confineCursorEnabled = enable;
        if (enable && awtRobot == null) {
            try {
                awtRobot = new java.awt.Robot();
            } catch (Exception ignored) {}
        }
        if (enable && awtRobot == null && fxRobot == null) {
            try {
                fxRobot = new Robot();
            } catch (Exception ignored) {}
        }
        if (enable) {
            installGlobalMouseConfineListener();
        } else {
            uninstallGlobalMouseConfineListener();
        }
        restartConfineTicker();
        if (enable) {
            ensureConfineFocusListener();
        }
    }

    private void restartConfineTicker() {
        stopConfineTicker();
        if (!confineCursorEnabled || !isStageReadyForConfine()) {
            return;
        }
        if (awtRobot == null && fxRobot == null) {
            return;
        }
        installGlobalMouseConfineListener();
        confineTicker = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(16), e -> confineMouseToWindow())
        );
        confineTicker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        confineTicker.play();
        // Instantly return cursor to window if it already exited
        confineMouseToWindow();
    }

    private void stopConfineTicker() {
        if (confineTicker != null) {
            confineTicker.stop();
            confineTicker = null;
        }
    }

    private void installGlobalMouseConfineListener() {
        if (globalMouseConfineListener != null) {
            return;
        }
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            globalMouseConfineListener = event -> {
                if (!confineCursorEnabled || awtRobot == null) {
                    return;
                }
                if (!(event instanceof java.awt.event.MouseEvent mouseEvent)) {
                    return;
                }
                int id = mouseEvent.getID();
                if (id != java.awt.event.MouseEvent.MOUSE_MOVED &&
                    id != java.awt.event.MouseEvent.MOUSE_DRAGGED &&
                    id != java.awt.event.MouseEvent.MOUSE_EXITED &&
                    id != java.awt.event.MouseEvent.MOUSE_ENTERED) {
                    return;
                }
                if (!isStageReadyForConfine()) {
                    return;
                }
                if (confineRequestScheduled.compareAndSet(false, true)) {
                    javafx.application.Platform.runLater(() -> {
                        confineRequestScheduled.set(false);
                        if (confineCursorEnabled && isStageReadyForConfine()) {
                            confineMouseToWindow();
                        }
                    });
                }
            };
            long mask = AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK;
            toolkit.addAWTEventListener(globalMouseConfineListener, mask);
        } catch (Exception e) {
            globalMouseConfineListener = null;
        }
    }

    private void uninstallGlobalMouseConfineListener() {
        if (globalMouseConfineListener == null) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseConfineListener);
        } catch (Exception ignored) {}
        globalMouseConfineListener = null;
        confineRequestScheduled.set(false);
    }

    private void ensureConfineFocusListener() {
        if (confineFocusListenerInstalled) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            if (confineFocusListenerInstalled) {
                return;
            }
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage == null) {
                return;
            }
            stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    suppressLevelCompletionChecks = true;
                    FXGL.runOnce(() -> suppressLevelCompletionChecks = false, Duration.millis(150));
                    if (confineCursorEnabled) {
                        restartConfineTicker();
                    }
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                } else {
                    suppressLevelCompletionChecks = true;
                    if (isPaused) {
                        pauseResumeLockFromFocusLoss = true;
                    }
                    stopConfineTicker();
                    uninstallGlobalMouseConfineListener();
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                }
            });
            stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
                if (!isIconified) {
                    suppressLevelCompletionChecks = true;
                    FXGL.runOnce(() -> suppressLevelCompletionChecks = false, Duration.millis(150));
                    if (confineCursorEnabled) {
                        restartConfineTicker();
                    }
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                } else {
                    suppressLevelCompletionChecks = true;
                    if (isPaused && isIconified) {
                        pauseResumeLockFromFocusLoss = true;
                    }
                    stopConfineTicker();
                    uninstallGlobalMouseConfineListener();
                    if (isPaused || isCountdownActive) {
                        enforcePauseFreeze();
                    }
                }
            });
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && confineCursorEnabled) {
                    restartConfineTicker();
                } else {
                    stopConfineTicker();
                    uninstallGlobalMouseConfineListener();
                }
            });
            confineFocusListenerInstalled = true;
        });
    }

    private boolean isStageReadyForConfine() {
        javafx.stage.Stage stage = FXGL.getPrimaryStage();
        return stage != null && stage.isShowing() && stage.isFocused() && !stage.isIconified();
    }
    private void confineMouseToWindow() {
        javafx.stage.Stage stage = FXGL.getPrimaryStage();
        if (stage == null || stage.getScene() == null) return;
        if (!isStageReadyForConfine()) return;
        javafx.scene.Scene scene = stage.getScene();
        // Get cursor coordinates in screen coordinates
        double cursorScreenX;
        double cursorScreenY;
        java.awt.PointerInfo info = null;
        try {
            info = java.awt.MouseInfo.getPointerInfo();
        } catch (Throwable ignored) {}
        if (info != null) {
            java.awt.Point p = info.getLocation();
            cursorScreenX = p.getX();
            cursorScreenY = p.getY();
        } else if (fxRobot != null) {
            cursorScreenX = fxRobot.getMouseX();
            cursorScreenY = fxRobot.getMouseY();
        } else {
            return;
        }

        // Screen bounds of scene content (without window borders), accounting for HiDPI
        javafx.geometry.Bounds screenBounds = null;
        try {
            screenBounds = scene.getRoot().localToScreen(scene.getRoot().getBoundsInLocal());
        } catch (Exception ignored) {}
        if (screenBounds == null) {
            javafx.stage.Window window = scene.getWindow();
            if (window == null) {
                return;
            }
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            if (sceneWidth <= 0 || sceneHeight <= 0) {
                return;
            }
            double offsetX = window.getX() + scene.getX();
            double offsetY = window.getY() + scene.getY();
            screenBounds = new javafx.geometry.BoundingBox(offsetX, offsetY, sceneWidth, sceneHeight);
        }

        // Safe padding from content edges to avoid window title/borders
        double minX = screenBounds.getMinX() + 4;
        double minY = screenBounds.getMinY() + 10; // increase top padding
        double maxX = screenBounds.getMaxX() - 4;
        double maxY = screenBounds.getMaxY() - 6;

        double clampedX = Math.max(minX, Math.min(maxX, cursorScreenX));
        double clampedY = Math.max(minY, Math.min(maxY, cursorScreenY));

        if (Math.abs(cursorScreenX - clampedX) > 0.01 || Math.abs(cursorScreenY - clampedY) > 0.01) {
            isWarpingMouse = true;
            try {
                if (awtRobot != null) {
                    awtRobot.mouseMove((int) Math.round(clampedX), (int) Math.round(clampedY));
                } else if (fxRobot != null) {
                    fxRobot.mouseMove(clampedX, clampedY);
                }
                try {
                    // Return focus to scene after pointer warp
                    scene.getRoot().requestFocus();
                } catch (Exception ignored) {}
            } finally {
                // Short delay to avoid immediate recursive call
                javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(5));
                pt.setOnFinished(e -> isWarpingMouse = false);
                pt.play();
            }
        }

        // Update paddle position even when pointer exits to title area, using screen -> scene coordinate transformation
        if (!isWarpingMouse && !isPaused && !isCountdownActive && paddleComponent != null && !isLevelCompleted && !isTransitioning) {
            try {
                // Use actual X coordinate (clamped), Y taken from inside scene
                double effScreenX = Math.max(minX, Math.min(maxX, cursorScreenX));
                double effScreenY = Math.max(minY, Math.min(maxY, cursorScreenY));
                javafx.geometry.Point2D local = scene.getRoot().screenToLocal(effScreenX, effScreenY);
                if (local != null) {
                    double sceneX = local.getX();
                    // IMPORTANT: don't call handleMouseMove() here to avoid re-entrancy
                    movePaddleForSceneX(sceneX);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean isLinuxWayland() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) return false;
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType != null && sessionType.equalsIgnoreCase("wayland")) return true;
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        return waylandDisplay != null && !waylandDisplay.isEmpty();
    }

    /**
     * Install mouse handlers for paddle control during gameplay.
     */
    public void installMousePaddleControlHandlers() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene == null) return;
            // Remove old filters if they were bound to another scene
            if (mouseHandlersInstalled) {
                try {
                    if (mouseMoveHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
                    }
                    if (mouseDragHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
                    }
                } catch (Exception ignored) {}
                mouseHandlersInstalled = false;
            }
            // Create and attach new handlers to current scene
            mouseMoveHandler = e -> handleMouseMove(e.getSceneX());
            mouseDragHandler = e -> handleMouseMove(e.getSceneX());
            mouseExitHandler = e -> {
                if (confineCursorEnabled) {
                    confineMouseToWindow();
                }
            };
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, mouseExitHandler);
            mouseHandlersInstalled = true;
            enableCursorConfine(true);
            // Ensure window focus for correct event delivery
            try {
                scene.getRoot().requestFocus();
            } catch (Exception ignored) {}
            enableMouseFollowTicker(true);
        });
    }

    /**
     * Remove mouse paddle control handlers (for menu/pause).
     */
    public void uninstallMousePaddleControlHandlers() {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene != null) {
                try {
                    if (mouseMoveHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
                    }
                    if (mouseDragHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
                    }
                    if (mouseExitHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, mouseExitHandler);
                    }
                    mouseExitHandler = null;
                } catch (Exception ignored) {}
            }
            mouseHandlersInstalled = false;
            enableCursorConfine(false);
            enableMouseFollowTicker(false);
        });
    }

    /**
     * Paddle movement logic based on mouse position.
     */
    private void handleMouseMove(double mouseSceneX) {
        // Active only during gameplay: paddle exists, not paused, not Game Over screen, not transitioning
        if (paddleComponent == null || isPaused || isCountdownActive || isTransitioning || isLevelCompleted) return;
        boolean gameOverVisible = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> node instanceof com.arcadeblocks.ui.GameOverView);
        if (gameOverVisible) return;
        // Check if paddle movement is blocked (e.g., frozen paddle bonus)
        if (paddleComponent.isMovementBlocked()) return;
        // Keyboard priority: if left/right keys are pressed - ignore mouse
        if (isKeyboardDriving()) return;
        
        if (isWarpingMouse) return; // avoid recursion during forced movement
        // If keyboard was just used - ignore first mouse movement to avoid jerk
        if (suppressMouseUntilMove) {
            suppressMouseUntilMove = false;
            return;
        }

        if (pauseResumeLockFromFocusLoss) {
            pauseResumeLockFromFocusLoss = false;
            return;
        }

        movePaddleForSceneX(mouseSceneX);
    }

    private boolean canProcessLaunchInput() {
        if (isLoading || isPaused || isCountdownActive || isTransitioning || isLevelCompleted) {
            return false;
        }
        if (mouseClicksBlocked) {
            return false;
        }
        boolean gameOverVisible = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> node instanceof com.arcadeblocks.ui.GameOverView);
        if (gameOverVisible) {
            return false;
        }
        return true;
    }

    public boolean consumePauseResumeLock() {
        if (pauseResumeLockFromFocusLoss) {
            pauseResumeLockFromFocusLoss = false;
            return true;
        }
        return false;
    }

    private void movePaddleForSceneX(double sceneX) {
        // Legacy behavior: mouse respects countdown/movement blocks and yields to keyboard input
        if (isCountdownActive || paddleComponent == null || paddleComponent.isMovementBlocked()) {
            return;
        }
        if (suppressMouseUntilMove) {
            return;
        }
        if (isKeyboardDriving()) {
            return;
        }

        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null) return;

        double halfWidth = paddleEntity.getWidth() / 2.0;
        double desiredX = sceneX - halfWidth;
        double minX = 1;
        double maxX = GameConfig.GAME_WIDTH - paddleEntity.getWidth() - 1;
        if (desiredX < minX) desiredX = minX;
        if (desiredX > maxX) desiredX = maxX;

        paddleEntity.setX(desiredX);
        try {
            var physics = paddleEntity.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
            if (physics != null) {
                physics.overwritePosition(new javafx.geometry.Point2D(desiredX, paddleEntity.getY()));
                physics.setLinearVelocity(0, 0);
            }
        } catch (Exception ignored) {}

        // NOTE: do not reset keyboard flags here so keyboard control stays authoritative until released
    }

    /**
     * Returns true if control is currently being performed by keyboard.
     */
    private boolean isKeyboardDriving() {
        return leftPressed || rightPressed || turboPressed;
    }
    
    /**
     * Complete loading
     */
    private void completeLoading(com.arcadeblocks.ui.LoadingView loadingView) {
        // Check that we are still in loading process
        if (!isLoading) {
            return; // If loading is already completed, do nothing
        }
        
        // Complete loading
        isLoading = false;
        
        // Don't pass loadingView to startMainMenuMusic to avoid memory leaks
        startMainMenuMusic();
        
        // Transition to main menu without greeting sound
        transitionToMainMenu(loadingView);
    }
    

    
    /**
     * Transition to main menu
     */
    private void transitionToMainMenu(com.arcadeblocks.ui.LoadingView loadingView) {
        // Clean LoadingView before removing from scene
        // This ensures stopping all animations and removing all listeners
        if (loadingView != null) {
            removeUINodeSafely(loadingView);
        }

        dismissActiveChapterStoryView();
        disableDarknessOverlay();
        
        // Show main menu (background should change when transitioning from loading)
        MainMenuView mainMenuView = new MainMenuView(this, true);
        FXGL.getGameScene().addUINode(mainMenuView);
        
        // Remove mouse handlers when entering menu
        uninstallMousePaddleControlHandlers();
        // Unblock mouse clicks in menu
        unblockMouseClicks();
        // Set system cursor in menu
        setSystemCursor();
        
        // Ensure MainMenuView received focus for keyboard navigation
        javafx.application.Platform.runLater(() -> {
            mainMenuView.restoreFocus();
        });
        
        // Music already started in startMainMenuMusic()
        // System.out.println("Transition to main menu (music already playing)");
    }
    
    
    private void showExitConfirmation() {
        com.arcadeblocks.ui.ExitConfirmView.show(
            // Action on exit
            () -> {
                // Return to main menu
                paddleComponent = null;
                // CRITICAL: Clean LivesManager before nullifying
                if (livesManager != null) {
                    livesManager.cleanup();
                }
                livesManager = null;
                scoreManager = null;
                bonusEffectManager = null;
                leftPressed = false;
                rightPressed = false;
                turboPressed = false;
                resetCallBallState();
                
                // Safe removal of all entities (copy list)
                var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
                entities.forEach(this::removeEntitySafely);
                
                // Cursor remains standard system cursor
                
                returnToMainMenu();
            },
            // Action on cancel - resume game
            () -> {
                // Resume gameplay
                FXGL.getGameController().resumeEngine();
            }
        );
    }
    
    /**
     * Window close handler (X button)
     */
    private void handleWindowClose() {
        // System.out.println("handleWindowClose called! paddleComponent = " + paddleComponent); // Debug

        if (isLoading || isLevelIntroActive || isStoryOverlayActive || isCountdownActive || isLevelCompleted || isTransitioning) {
            return;
        }
        
        // Check if we are in gameplay
        if (paddleComponent != null) {
        // System.out.println("Show pause screen on window close during gameplay"); // Debug
            // During gameplay - show pause screen instead of exit dialog
            if (!isPaused) {
                pauseGame();
            }
            // If already paused, do nothing - pause screen is already shown
        } else {
        // System.out.println("Show game exit dialog"); // Debug
            // In main menu - show game exit dialog
            showGameExitConfirmation();
        }
    }
    
    /**
     * Show game exit dialog (for main menu)
     */
    private void showGameExitConfirmation() {
        com.arcadeblocks.ui.GameExitConfirmView.show(
            // Action on exit
            () -> {
                exitGame();
            },
            // Action on cancel (do nothing, just close dialog)
            () -> {
                // Dialog will close automatically
            }
        );
    }
    
    private long lastFpsLogTime = 0;
    private int frameCount = 0;
    private long lastFrameTime = 0;
    
    @Override
    protected void onUpdate(double tpf) {
        long startTime = System.nanoTime();
        
        // Детектируем микрофризы (если кадр занял больше 33мс при 60 FPS)
        if (lastFrameTime > 0) {
            long frameDuration = startTime - lastFrameTime;
            if (frameDuration > 33_000_000) { // Больше 33мс = микрофриз
                // System.out.println(String.format("[Performance] MICROFREEZE detected! Frame took %.2fms", frameDuration / 1_000_000.0));
            }
        }
        lastFrameTime = startTime;
        
        // Логируем FPS каждую секунду
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsLogTime >= 1000) {
            // Получаем информацию о памяти
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            // System.out.println(String.format("[Performance] FPS: %d, TPF: %.2fms, Memory: %dMB/%dMB", 
            //     frameCount, tpf * 1000, usedMemory, maxMemory));
            frameCount = 0;
            lastFpsLogTime = currentTime;
        }
        
        super.onUpdate(tpf);
        if (!vsyncConfigured) {
            applyVSyncInternal(vsyncEnabled);
        }
        long superTime = System.nanoTime() - startTime;
        
        // КРИТИЧНО: Периодическая очистка ресурсов для предотвращения лагов при долгой игре
        if (isGameplayState()) {
            gameplayTimeAccumulator += tpf;
            if (gameplayTimeAccumulator >= PERIODIC_CLEANUP_INTERVAL) {
                performPeriodicGameplayCleanup();
                gameplayTimeAccumulator = 0.0;
            }
        } else {
            // Сбрасываем аккумулятор когда не в игровом состоянии
            gameplayTimeAccumulator = 0.0;
        }
        
        if (audioManager != null) {
            audioManager.update(tpf);
        }
        if (scoreManager != null) {
            scoreManager.update(tpf);
        }
        if (bonusEffectManager != null) {
            bonusEffectManager.update(tpf);
        }
        if (isLevelCompleted) {
            if (!levelCompletedMessageShown) {
                levelCompletedMessageShown = true;
            }
            return;
        } else {
            levelCompletedMessageShown = false;
        }
        if (paddleComponent != null) {
            // ДИАГНОСТИКА: выводим состояние флагов перед установкой
            // if (leftPressed || rightPressed) {
            //     System.out.println("[App.onUpdate] leftPressed=" + leftPressed + ", rightPressed=" + rightPressed + ", isGameOverViewVisible=" + isGameOverViewVisible);
            // }
            
            // Use flag instead of stream check to eliminate micro-freezes
            if (!isGameOverViewVisible) {
                paddleComponent.setMoveLeft(leftPressed);
                paddleComponent.setMoveRight(rightPressed);
                paddleComponent.setTurboMode(turboPressed);
                if (leftPressed || rightPressed || turboPressed) {
                    suppressMouseUntilMove = true;
                }
            } else {
                paddleComponent.setMoveLeft(false);
                paddleComponent.setMoveRight(false);
                paddleComponent.setTurboMode(false);
                paddleComponent.setMovementBlocked(true);
            }
        } else {
            // ДИАГНОСТИКА: paddleComponent is null
            // if (leftPressed || rightPressed) {
            //     System.out.println("[App] WARNING: paddleComponent is NULL but input detected!");
            // }
        }

        long beforeCallBall = System.nanoTime();
        applyCallBallAttraction(tpf);
        long callBallTime = System.nanoTime() - beforeCallBall;
        
        long totalTime = System.nanoTime() - startTime;
        
        // Логируем только если onUpdate занял больше 2мс (заметная задержка при 60 FPS)
        // if (totalTime > 2_000_000) {
        //     System.out.println(String.format("[Performance] onUpdate took %.2fms (super: %.2fms, callBall: %.2fms)", 
        //         totalTime / 1_000_000.0, superTime / 1_000_000.0, callBallTime / 1_000_000.0));
        // }
    }

    private void applyCallBallAttraction(double tpf) {
        if (!isCallBallActive() || paddleComponent == null) {
            return;
        }
        if (!isGameplayState()) {
            return;
        }
        
        // КРИТИЧНО: Отключаем притяжение мячей на хардкорной сложности
        // Проверяем один раз здесь вместо проверки для каждого мяча - оптимизация
        if (getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE) {
            return;
        }
        
        // Блокировка на boss уровнях (кроме легкой сложности)
        if (getEffectiveDifficulty() != com.arcadeblocks.config.DifficultyLevel.EASY && 
            com.arcadeblocks.config.LevelConfig.isBossLevel(FXGL.geti("level"))) {
            return;
        }

        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null || !paddleEntity.isActive()) {
            return;
        }

        Point2D paddleCenter = paddleEntity.getCenter();
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ballEntity : balls) {
            if (ballEntity == null || !ballEntity.isActive()) {
                continue;
            }
            Ball ballComponent;
            try {
                ballComponent = ballEntity.getComponent(Ball.class);
            } catch (Exception ex) {
                continue;
            }
            if (ballComponent != null) {
                ballComponent.applyPaddleAttraction(paddleCenter, tpf);
            }
        }
    }

    /**
     * Периодическая очистка ресурсов во время долгой игры для предотвращения лагов.
     * Вызывается каждые 5 минут игрового времени.
     */
    private void performPeriodicGameplayCleanup() {
        try {
            // КРИТИЧНО: Принудительно запускаем сборку мусора для освобождения памяти
            // Это помогает предотвратить накопление мусора во время долгой игры
            System.gc();
            
            // КРИТИЧНО: Очищаем неактивные анимации и переходы
            synchronized (activeFadeTransitions) {
                activeFadeTransitions.removeIf(transition -> {
                    if (transition.getStatus() == Animation.Status.STOPPED) {
                        try {
                            transition.stop();
                        } catch (Exception ignored) {
                        }
                        return true;
                    }
                    return false;
                });
            }
            
            // КРИТИЧНО: Очищаем неактивные видео переходы
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.removeIf(transition -> {
                    if (transition.getStatus() == Animation.Status.STOPPED) {
                        try {
                            transition.stop();
                        } catch (Exception ignored) {
                        }
                        return true;
                    }
                    return false;
                });
            }
            
            // КРИТИЧНО: Очищаем неактивные сущности из игрового мира
            var gameWorld = FXGL.getGameWorld();
            if (gameWorld != null) {
                var entities = gameWorld.getEntities();
                int beforeCount = entities.size();
                
                // Удаляем неактивные сущности
                entities.removeIf(entity -> !entity.isActive());
                
                int afterCount = gameWorld.getEntities().size();
                // if (beforeCount != afterCount) {
                //     System.out.println("Cleaned up " + (beforeCount - afterCount) + " inactive entities");
                // }
            }
            
            // КРИТИЧНО: Очищаем неактивные UI ноды (включая orphaned SettingsView)
            try {
                clearInactiveUINodes();
            } catch (Exception e) {
                System.err.println("Error during UI cleanup in periodic cleanup: " + e.getMessage());
            }
            
            // System.out.println("Periodic gameplay cleanup performed after " + PERIODIC_CLEANUP_INTERVAL + " seconds");
            
        } catch (Exception e) {
            System.err.println("Error during periodic gameplay cleanup: " + e.getMessage());
        }
    }

    /**
     * Очищает неактивные UI ноды для предотвращения утечек памяти.
     * Используется при переходах между экранами настроек и паузы.
     */
    private void clearInactiveUINodes() {
        try {
            var scene = FXGL.getGameScene();
            var uiNodes = new ArrayList<Node>(scene.getUINodes());
            
            for (Node node : uiNodes) {
                // Проверяем, является ли нод неактивным или "мертвым"
                boolean shouldRemove = false;
                
                // Стандартные проверки неактивности
                if (!node.isVisible() || node.getOpacity() == 0.0 || node.getParent() == null) {
                    shouldRemove = true;
                }
                
                // КРИТИЧНО: Специальная проверка для SettingsView и связанных диалогов
                // Если мы не в режиме настроек паузы, но эти элементы все еще в сцене - удаляем их
                if (!inPauseSettings && (node instanceof SettingsView 
                    || node instanceof com.arcadeblocks.ui.ResetSettingsDialog
                    || node instanceof com.arcadeblocks.ui.ControlsResetDialog
                    || node instanceof com.arcadeblocks.ui.UnsavedChangesDialog)) {
                    shouldRemove = true;
                    // System.out.println("Force removing orphaned settings UI: " + node.getClass().getSimpleName());
                }
                
                if (shouldRemove) {
                    // Пытаемся очистить через UINodeCleanup
                    try {
                        UINodeCleanup.cleanupNode(node);
                        scene.removeUINode(node);
                    } catch (Exception e) {
                        System.err.println("Error cleaning inactive UI node: " + e.getMessage());
                    }
                }
            }
            
            // КРИТИЧНО: НЕ вызываем System.gc() здесь, так как это блокирующая операция
            // которая вызывает заметные лаги в UI потоке.
            // Сборка мусора произойдет автоматически, когда это будет необходимо.
            
        } catch (Exception e) {
            System.err.println("Error in clearInactiveUINodes: " + e.getMessage());
        }
    }
    
    private void performShutdownIfNeeded() {
        if (shutdownTriggered.compareAndSet(false, true)) {
            shutdownInternal();
        }
    }

    private void shutdownInternal() {
        // Stop all active video resources FIRST
        cleanupActiveVideoResources();
        
        // Clean all UI components
        cleanupAllUINodes();
        
        // Clean gameplay state before cleaning managers
        cleanupGameplayState();
        
        // Save settings on exit
        if (saveManager != null) {
            saveManager.saveSettings();
            try {
                saveManager.close();
            } catch (Exception ignored) {
            }
            saveManager = null;
        }
        
        // Stop all animations and timers
        cleanupAllTimelines();
        
        // Remove all event listeners
        cleanupAllEventListeners();
        
        // Stop and clean audio
        if (audioManager != null) {
            audioManager.stopAll();
            audioManager.cleanup();
            audioManager = null;
        }
        
        // Clean VLC resources
        try {
            com.arcadeblocks.video.VlcContext.getInstance().dispose();
        } catch (Exception e) {
            System.err.println("Error cleaning VLC: " + e.getMessage());
        }

        try {
            FXGL.getAssetLoader().clearCache();
        } catch (Exception ignored) {
        }

        ImageCache.clear();
        
        try {
            com.arcadeblocks.util.TextureUtils.clearGameplayTextureCache();
        } catch (Exception ignored) {
        }
        
        // Clean all manager references
        if (scoreManager != null) {
            try {
                scoreManager.stopLevelTimer();
                scoreManager.hideBossHealth();
            } catch (Exception ignored) {
            }
            scoreManager = null;
        }
        bonusEffectManager = null;
        // Clean LivesManager before nullifying
        if (livesManager != null) {
            livesManager.cleanup();
        }
        livesManager = null;
        paddleComponent = null;
        // Clean BonusTimerView before nullifying
        if (bonusTimerView != null) {
            bonusTimerView.cleanup();
        }
        bonusTimerView = null;
        gameplayUIView = null;
        
        // Clean all UI component references
        try {
            var scene = FXGL.getGameScene();
            if (scene != null) {
                // Remove all remaining UI nodes
                var remainingNodes = new java.util.ArrayList<>(scene.getUINodes());
                for (var node : remainingNodes) {
                    try {
                        scene.removeUINode(node);
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        // Clean all background image references
        try {
            levelBackgroundNodes.clear();
        } catch (Exception ignored) {
        }
        
        // Clean all darkness overlay references
        darknessOverlayCapsule = null;
        darknessOverlayGroup = null;
        darknessOverlayDimLayer = null;
        darknessOverlayActive = false;
    }
    
    /**
     * Shutdown all ExecutorService for complete process termination
     */
    private void shutdownAllExecutors() {
        // Shutdown ExecutorService in AudioManager
        if (audioManager != null && audioManager instanceof com.arcadeblocks.audio.SDL2AudioManager) {
            com.arcadeblocks.audio.SDL2AudioManager sdlAudioManager = (com.arcadeblocks.audio.SDL2AudioManager) audioManager;
            try {
                java.util.concurrent.ExecutorService executor = sdlAudioManager.getAsyncExecutor();
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                    try {
                        if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                            executor.shutdownNow();
                            if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                                System.err.println("AudioManager executor did not terminate within 2 seconds");
                            }
                        }
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                // Ignore errors during executor termination
            }
        }
        
        // SaveManager already closes in shutdownInternal(), but ensure executor is terminated
        if (saveManager != null) {
            try {
                // SaveManager.close() already calls shutdown on dbExecutor
                // No additional check required
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }
    
    /**
     * Safe entity removal with resource cleanup to prevent memory leaks
     * Cleans cached textures, physical fixtures and custom components before removal.
     * Centralizes cleanup behavior and protects from exceptions during cleanup/removal.
     */
    private void removeEntitySafely(Entity entity) {
        if (entity == null || !entity.isActive()) {
            return;
        }
        
        try {
            // Clean entity components before removal
            // Clean ViewComponent to release textures
            if (entity.getViewComponent() != null) {
                var viewChildren = entity.getViewComponent().getChildren();
                if (viewChildren != null) {
                    for (var node : new java.util.ArrayList<>(viewChildren)) {
                        try {
                            if (node instanceof javafx.scene.image.ImageView imageView) {
                                var image = imageView.getImage();
                                if (image != null) {
                                    ImageCache.forget(image);
                                }
                                imageView.setImage(null);
                            }
                        } catch (Exception e) {
                            // Ignore errors when cleaning individual nodes
                        }
                    }
                }
            }
            
            // Clean PhysicsComponent to release fixtures
            try {
                var physics = entity.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                if (physics != null) {
                    // Physics fixtures will be automatically cleaned when entity is removed
                    // But we can explicitly stop physics
                    physics.setLinearVelocity(0, 0);
                }
            } catch (Exception e) {
                // Ignore errors when component not found or cleaning physics
            }
            
            // Clean custom components that may have resources
            // Ball, Brick, Bonus, Paddle etc. may have cached resources
            try {
                var ballComponent = entity.getComponent(com.arcadeblocks.gameplay.Ball.class);
                if (ballComponent != null) {
                    // Ball component may have cached textures
                    // They will be cleaned when entity is removed
                }
            } catch (Exception e) {
                // Ignore errors when component not found
            }
            
            try {
                var brickComponent = entity.getComponent(com.arcadeblocks.gameplay.Brick.class);
                if (brickComponent != null) {
                    // Brick component may have cached textures
                    // They will be cleaned when entity is removed
                }
            } catch (Exception e) {
                // Ignore errors when component not found
            }
            
            try {
                var bonusComponent = entity.getComponent(com.arcadeblocks.gameplay.Bonus.class);
                if (bonusComponent != null) {
                    // Bonus component may have cached textures
                    // They will be cleaned when entity is removed
                }
            } catch (Exception e) {
                // Ignore errors when component not found
            }
            
        } catch (Exception e) {
            // Log error but continue removal (suppress expected component not found errors)
            // System.err.println("Error cleaning entity resources before removal: " + e.getMessage());
        }
        
        // Remove entity from world
        try {
            entity.removeFromWorld();
        } catch (Exception e) {
            // Log error but don't interrupt removal flow
            System.err.println("Error removing entity from world: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup all UI components for complete process termination
     */
    private void cleanupAllUINodes() {
        try {
            var scene = FXGL.getGameScene();
            if (scene != null) {
                // Clean all UI nodes with cleanup() call if they support it
                var uiNodes = new java.util.ArrayList<>(scene.getUINodes());
                for (var node : uiNodes) {
                    try {
                        // First try via SupportsCleanup interface
                        if (node instanceof com.arcadeblocks.ui.SupportsCleanup) {
                            ((com.arcadeblocks.ui.SupportsCleanup) node).cleanup();
                        } else {
                            // Fallback: try via reflection for legacy widgets
                            try {
                                java.lang.reflect.Method cleanupMethod = node.getClass().getMethod("cleanup");
                                if (cleanupMethod != null) {
                                    cleanupMethod.invoke(node);
                                }
                            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
                                // cleanup() method not found or unavailable - this is normal
                            }
                        }
                        removeUINodeSafely(node);
                    } catch (Exception e) {
                        // Ignore errors when cleaning individual nodes
                    }
                }
            }
            
            // Clean all entities from game world
            try {
                var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
                for (var entity : entities) {
                    try {
                        removeEntitySafely(entity);
                    } catch (Exception e) {
                        // Ignore errors when removing entities
                    }
                }
            } catch (Exception e) {
                // Ignore errors when cleaning game world
            }
        } catch (Exception e) {
            // Ignore errors when cleaning UI
        }
    }
    
    /**
     * Stop all Timeline animations to prevent memory leaks
     */
    private void cleanupAllTimelines() {
        // Stop cursorEnforcer
        if (cursorEnforcer != null) {
            cursorEnforcer.stop();
            cursorEnforcer = null;
        }
        
        confineCursorEnabled = false;
        // Stop confineTicker
        if (confineTicker != null) {
            confineTicker.stop();
            confineTicker = null;
        }
        
        // Stop mouseFollowTicker
        if (mouseFollowTicker != null) {
            mouseFollowTicker.stop();
            mouseFollowTicker = null;
        }
        
        uninstallGlobalMouseConfineListener();
        awtRobot = null;
        fxRobot = null;

        // Stop all VLC players to prevent memory leaks
        try {
            com.arcadeblocks.video.VlcContext.getInstance().dispose();
        } catch (Exception e) {
            System.err.println("Error cleaning up VLC resources: " + e.getMessage());
        }
    }
    
    /**
     * Remove all event listeners to prevent memory leaks
     */
    private void cleanupAllEventListeners() {
        javafx.application.Platform.runLater(() -> {
            // Cleanup GameOverView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.GameOverView)
                .forEach(node -> {
                    com.arcadeblocks.ui.GameOverView gameOverView = (com.arcadeblocks.ui.GameOverView) node;
                    gameOverView.cleanup();
                });
            // Reset GameOverView flag after cleanup
            isGameOverViewVisible = false;
            
            // Cleanup LevelIntroView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.LevelIntroView)
                .forEach(node -> {
                    com.arcadeblocks.ui.LevelIntroView levelIntroView = (com.arcadeblocks.ui.LevelIntroView) node;
                    levelIntroView.cleanup();
                });
            
            // Cleanup LoadingView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.LoadingView)
                .forEach(node -> {
                    com.arcadeblocks.ui.LoadingView loadingView = (com.arcadeblocks.ui.LoadingView) node;
                    loadingView.cleanup();
                });
            
            // Cleanup CountdownTimerView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.CountdownTimerView)
                .forEach(node -> {
                    com.arcadeblocks.ui.CountdownTimerView countdownView = (com.arcadeblocks.ui.CountdownTimerView) node;
                    countdownView.cleanup();
                });
            
            // Cleanup PoemView if active
            FXGL.getGameScene().getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.PoemView)
                .forEach(node -> {
                    com.arcadeblocks.ui.PoemView poemView = (com.arcadeblocks.ui.PoemView) node;
                    poemView.cleanup();
                });
            
            javafx.scene.Scene scene = FXGL.getPrimaryStage().getScene();
            if (scene != null) {
                try {
                    if (mouseMoveHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, mouseMoveHandler);
                        mouseMoveHandler = null;
                    }
                    if (mouseDragHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, mouseDragHandler);
                        mouseDragHandler = null;
                    }
                    if (mouseExitHandler != null) {
                        scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_EXITED, mouseExitHandler);
                        mouseExitHandler = null;
                    }
                } catch (Exception ignored) {}
            }
            mouseHandlersInstalled = false;
        });
        
        // Disable all tickers
        stopConfineTicker();
        enableMouseFollowTicker(false);
    }
    
    /**
     * Get audio manager
     */
    public SDL2AudioManager getAudioManager() {
        return audioManager;
    }
    
    
    public String getCurrentLevelLoadingSound() {
        return currentLevelLoadingSound != null
            ? currentLevelLoadingSound
            : AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
    }
    
    public String getCurrentLevelCompleteSound() {
        if (currentLevelCompletionSounds == null || currentLevelCompletionSounds.isEmpty()) {
            return AudioConfig.DEFAULT_LEVEL_COMPLETE_SOUND;
        }
        if (currentLevelCompletionSounds.size() == 1) {
            return currentLevelCompletionSounds.get(0);
        }
        int index = levelAudioRandom.nextInt(currentLevelCompletionSounds.size());
        return currentLevelCompletionSounds.get(index);
    }
    
    private String pickBrickHitSound() {
        if (currentBrickHitSounds == null || currentBrickHitSounds.isEmpty()) {
            return AudioConfig.DEFAULT_BRICK_HIT_SOUND;
        }
        if (currentBrickHitSounds.size() == 1) {
            return currentBrickHitSounds.get(0);
        }
        int index = levelAudioRandom.nextInt(currentBrickHitSounds.size());
        return currentBrickHitSounds.get(index);
    }
    
    public void playBrickHitSound() {
        if (audioManager == null) {
            return;
        }
        String sound = pickBrickHitSound();
        if (sound != null && !sound.isBlank()) {
            audioManager.playSFX(sound);
        }
    }
    
    /**
     * Get save manager
     */
    public SaveManager getSaveManager() {
        return saveManager;
    }
    
    /**
     * Get lives manager
     */
    public com.arcadeblocks.gameplay.LivesManager getLivesManager() {
        return livesManager;
    }
    
    /**
     * Get bonus effect manager
     */
    public com.arcadeblocks.gameplay.BonusEffectManager getBonusEffectManager() {
        return bonusEffectManager;
    }
    
    /**
     * Get bonus timer indicator
     */
    public BonusTimerView getBonusTimerView() {
        return bonusTimerView;
    }
    
    /**
     * Get gameplay UI
     */
    public GameplayUIView getGameplayUIView() {
        return gameplayUIView;
    }
    
    /**
     * Get score manager
     */
    public com.arcadeblocks.gameplay.ScoreManager getScoreManager() {
        return scoreManager;
    }

    public UserAction getPlasmaWeaponAction() {
        return plasmaWeaponAction;
    }
    
    /**
     * Reset paddle key press flags
     */
    public void resetPaddleInputFlags() {
        leftPressed = false;
        rightPressed = false;
        turboPressed = false;
        resetCallBallState();
    }

    public void applyVSyncSetting(boolean enabled) {
        if (saveManager != null) {
            boolean stored = saveManager.isVSyncEnabled();
            if (stored != enabled) {
                saveManager.setVSyncEnabled(enabled);
            }
        }
        // КРИТИЧНО: НЕ сбрасываем vsyncConfigured здесь!
        // Иначе первоначальная настройка не успевала сработать, пока запускалась игра,
        // и потом кустарно приходилось в onUpdate() вызывать applyVSyncInternal(),
        // что приводило к постоянным дерганиям настроек частоты при загрузке
        applyVSyncInternal(enabled);
    }

    public void applySavedVSync() {
        boolean enabled = saveManager == null || saveManager.isVSyncEnabled();
        vsyncConfigured = false;
        applyVSyncInternal(enabled);
    }

    private void applyVSyncInternal(boolean enabled) {
        vsyncEnabled = enabled;
        applyVSyncFlag(enabled);
        vsyncConfigured = true;
    }

    private void applyVSyncFlag(boolean enabled) {
        System.setProperty("prism.vsync", enabled ? "true" : "false");
    }

    private void enforceVSyncAfterResume() {
        applyVSyncInternal(vsyncEnabled);
    }

    /**
     * Go to game
     */
    public void startGame() {
        // Reset loading flag when starting game
        isLoading = false;

        // Reset pause flag when starting game
        isPaused = false;
        
        // Reset paddle input flags for proper control operation
        resetPaddleInputFlags();
        if (levelPassBonusEntity != null && levelPassBonusEntity.isActive()) {
            levelPassBonusEntity.removeFromWorld();
        }
        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
        
        // Stop any current music when starting game
        if (audioManager != null) {
            audioManager.stopMusic();
        // System.out.println("All music stopped - gameplay begins");
        }
        
        clearUINodesSafely();
        
        // Hide cursor during gameplay
        setHiddenCursor();
        // Safety: reinstall mouse handlers
        uninstallMousePaddleControlHandlers();
        
        // Reload key bindings to ensure proper control operation after restart
        reloadInputBindings();
        
        // Ensure window/scene focus immediately
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.stage.Stage st = FXGL.getPrimaryStage();
                if (st != null) {
                    st.requestFocus();
                    if (st.getScene() != null && st.getScene().getRoot() != null) {
                        st.getScene().getRoot().setFocusTraversable(true);
                        st.getScene().getRoot().requestFocus();
                    }
                }
            } catch (Exception ignored) {}
        });
        
        // Set background color
        FXGL.getGameScene().setBackgroundColor(Color.web(GameConfig.DARK_BACKGROUND));
        
        // Create new gameplay UI
        gameplayUIView = new GameplayUIView(this);
        FXGL.getGameScene().addUINode(gameplayUIView);
        
        // Center game world when starting game and update letterbox overlay
        javafx.application.Platform.runLater(() -> {
            centerGameWorld();
            updateLetterboxOverlay();
        });
        
        // Create lives manager (add to game area)
        livesManager = new com.arcadeblocks.gameplay.LivesManager(this);

        // Set lives count in FXGL based on difficulty
        FXGL.set("lives", livesManager.getCurrentLives());

        if (saveManager != null && !isDebugMode) {
            int savedLives = saveManager.getLives();
            livesManager.setCurrentLivesFromSnapshot(savedLives);
            FXGL.set("lives", savedLives);
            if (gameplayUIView != null) {
                gameplayUIView.updateLives(savedLives);
            }
        }
        
        // Create score manager
        scoreManager = new com.arcadeblocks.gameplay.ScoreManager(this);
        scoreManager.loadFromSave();
        
        // Reset score to FXGL value (for new levels)
        int fxglScore = FXGL.geti("score");
        if (fxglScore == 0) {
            scoreManager.resetScore();
        }
        
        // Update UI with initial data
        if (gameplayUIView != null) {
            int currentLevel = FXGL.geti("level");
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(currentLevel);
            String levelName = levelData != null ? levelData.getName() : null;
            gameplayUIView.updateLevel(currentLevel, levelName);
            gameplayUIView.updateScore(FXGL.geti("score"));
            gameplayUIView.updateLives(FXGL.geti("lives"));
        }
        
        // Update level display in ScoreManager
        if (scoreManager != null) {
            int currentLevel = FXGL.geti("level");
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(currentLevel);
            String levelName = levelData != null ? levelData.getName() : null;
            scoreManager.updateLevel(currentLevel, levelName);
        }
        
        if (scoreManager != null) {
            scoreManager.restartLevelTimer();
        }
        
        // Create bonus effect manager
        bonusEffectManager = new com.arcadeblocks.gameplay.BonusEffectManager(this);
        
        // Create bonus timer indicator (disabled - using BonusIndicator)
        // if (bonusTimerView != null) {
        //     bonusTimerView.clearAllBonuses();
        // }
        // bonusTimerView = new BonusTimerView();
        // FXGL.getGameScene().addUINode(bonusTimerView);
        // javafx.application.Platform.runLater(() -> {
        //     if (bonusTimerView != null) {
        //         bonusTimerView.updatePosition(0, 0);
        //     }
        // });
        
        // Create walls for fullscreen
        FXGL.entityBuilder()
            .type(EntityType.WALL)
            .at(0, 0)
            .bbox(new com.almasb.fxgl.physics.HitBox(com.almasb.fxgl.physics.BoundingShape.box(10, GameConfig.GAME_HEIGHT)))
            .collidable()
            .buildAndAttach();
            
        FXGL.entityBuilder()
            .type(EntityType.WALL)
            .at(GameConfig.GAME_WIDTH - 10, 0)
            .bbox(new com.almasb.fxgl.physics.HitBox(com.almasb.fxgl.physics.BoundingShape.box(10, GameConfig.GAME_HEIGHT)))
            .collidable()
            .buildAndAttach();
            
        FXGL.entityBuilder()
            .type(EntityType.WALL)
            .at(0, GameConfig.TOP_UI_HEIGHT)
            .bbox(new com.almasb.fxgl.physics.HitBox(com.almasb.fxgl.physics.BoundingShape.box(GameConfig.GAME_WIDTH, 10)))
            .collidable()
            .buildAndAttach();
        
        // Create paddle
        com.almasb.fxgl.entity.Entity paddle = FXGL.spawn("paddle", GameConfig.PADDLE_START_POS);
        paddleComponent = new com.arcadeblocks.gameplay.Paddle();
        paddle.addComponent(paddleComponent);
        
        // Set paddle speed from settings
        paddleComponent.setSpeed(saveManager.getPaddleSpeed());
        
        // Ensure paddle movement is unblocked (especially important after Game Over restart)
        paddleComponent.setMovementBlocked(false);
        
        // Enable mouse control for paddle
        installMousePaddleControlHandlers();
        // Additionally: reinstall after a short interval to be the last event consumer
        FXGL.runOnce(() -> installMousePaddleControlHandlers(), javafx.util.Duration.seconds(0.05));
        
        // Unblock mouse clicks for new level
        unblockMouseClicks();
        
        // Removed cursor setup
        
        double paddleWidth = paddle.getWidth();
        double defaultOffsetX = paddleWidth * 0.18;
        double defaultOffsetY = -GameConfig.BALL_RADIUS * 2 - 5;
        double spawnBallX = paddle.getX() + paddleWidth / 2.0 + defaultOffsetX - GameConfig.BALL_RADIUS;
        double spawnBallY = paddle.getY() + defaultOffsetY;
        com.almasb.fxgl.entity.Entity ball = FXGL.spawn("ball", spawnBallX, spawnBallY);
        Ball ballComponent = new com.arcadeblocks.gameplay.Ball();
        ball.addComponent(ballComponent);
        
        // Check if chaotic balls bonus is active (for initial ball)
        if (bonusEffectManager != null && bonusEffectManager.isChaoticBallsActive()) {
            ballComponent.setChaoticBall(true);
        // System.out.println("Initial ball became chaotic (chaotic balls bonus active)");
        }
        
        // Check if sticky paddle is active (for initial ball)
        if (bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive()) {
            ballComponent.setStickyEnabled(true);
            // System.out.println("Initial ball got stickiness (sticky paddle active)");
        }
        
        ballComponent.setAttachedOffset(defaultOffsetX, defaultOffsetY);
        ballComponent.attachToPaddle(paddle);

        // Add initial ball to attached list
        com.arcadeblocks.gameplay.Ball.addAttachedBall(ball);

        scheduleAutoLaunch();

        // Debug information
        // System.out.println("Ball created and attached to paddle:");
        // System.out.println("  Paddle position: " + paddle.getX() + ", " + paddle.getY());
        // System.out.println("  Ball position: " + ball.getX() + ", " + ball.getY());
        // System.out.println("  Attached: " + ballComponent.isAttachedToPaddle());
        
        // Stop menu music and start game music
        // audioManager.playMusic("music/level1.mp3", true); // Level music disabled
        
        // Ensure game is not paused
        FXGL.getGameController().resumeEngine();

        // После старта уровня ссылка на звук загрузки больше не нужна — сбрасываем
        releaseLevelLoadingSoundReference();
    }
    
    /**
     * Go to settings
     */
    public void showSettings() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под SettingsView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем SettingsView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.SettingsView(this));
    }
    
    /**
     * Go to help
     */
    public void showHelp() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под HelpView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем HelpView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.HelpView(this));
    }
    
    /**
     * Go to language selection window
     */
    public void showLanguageWindow() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под LanguageView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем LanguageView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.LanguageView(this));
    }
    
    /**
     * Set game language
     */
    public void setLanguage(String languageCode) {
        if (saveManager != null) {
            saveManager.setLanguage(languageCode);
            saveManager.awaitPendingWrites();
            // Can add logic here to reload interface with new language
            // For now just save the language choice
        }
        LocalizationManager.getInstance().setLanguage(languageCode);
    }
    
    /**
     * Go to credits
     */
    public void showCredits() {
        showCredits(false);
    }
    
    public void showCredits(boolean fromSaveSystem) {
        // Cancel pending music since credits will have its own music
        if (audioManager != null) {
            audioManager.cancelPendingMusic();
        }
        
        // Remove mouse handlers when transitioning to credits
        uninstallMousePaddleControlHandlers();
        // Unblock mouse clicks in credits
        unblockMouseClicks();
        // Set system cursor in credits
        setSystemCursor();

        cleanupGameplayState();
        
        clearUINodesSafely();
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.CreditsView(this, fromSaveSystem));
    }
    
    /**
     * Воспроизведение видео после титров и возврат в главное меню
     */
    public void playAfterCreditsVideo() {
        playVideoOverlay("after_credits_video.mp4", 22.0, remover -> {
            // После видео возвращаемся в главное меню
            javafx.application.Platform.runLater(() -> {
                // Удаляем оверлей видео
                if (remover != null) {
                    remover.run();
                }
                
                // Запускаем музыку главного меню с учетом завершения игры
                startMainMenuMusic();
                
                // Возвращаемся в главное меню
                returnToMainMenuFromSettings();
            });
        });
    }
    
    /**
     * Show save game window
     */
    public void showSaveGameWindow() {
        // Check pending music before transition
        if (audioManager != null) {
            audioManager.checkPendingMusic();
            // Save current music for restoration on return
            audioManager.saveCurrentMusic();
        }
        
        // КРИТИЧНО: НЕ вызываем clearUINodesSafely()!
        // MainMenuView должен остаться на экране под SaveGameView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Просто добавляем SaveGameView поверх MainMenuView
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.SaveGameView(this));
    }
    
    /**
     * Start level 0 (debug level)
     */
    public void startLevel0() {
        startLevel(0, true); // Reset score for debug level
    }
    
    /**
     * Start level in debug mode (completely decoupled from save system)
     */
    public void startDebugLevel(int levelNumber) {
        startDebugLevel(levelNumber, null, null);
    }

    private void startDebugLevel(int levelNumber, Integer livesCarryOver, Integer scoreCarryOver) {
        startDebugLevel(levelNumber, livesCarryOver, scoreCarryOver, null);
    }
    
    /**
     * КРИТИЧНО: Перегруженная версия для бесшовного перехода от видео к сюжетному окну в debug режиме
     */
    private void startDebugLevel(int levelNumber, Integer livesCarryOver, Integer scoreCarryOver, Runnable overlayRemover) {
        // System.out.println("DEBUG: Starting level " + levelNumber + " in debug mode (no save)");

        isDebugMode = true;
        com.arcadeblocks.config.DifficultyLevel debugDifficulty = com.arcadeblocks.config.DifficultyLevel.NORMAL;
        if (saveManager != null) {
            try {
                com.arcadeblocks.config.DifficultyLevel configuredDifficulty = saveManager.getDifficulty();
                if (configuredDifficulty != null) {
                    debugDifficulty = configuredDifficulty;
                }
            } catch (Exception ignored) {}
        }
        debugDifficultyOverride = debugDifficulty;

        int initialLives = livesCarryOver != null ? Math.max(1, livesCarryOver) : debugDifficulty.getLives();
        int initialScore = scoreCarryOver != null ? Math.max(0, scoreCarryOver) : 0;
        setDebugLivesOverride(initialLives);
        setDebugScoreOverride(initialScore);

        FXGL.set("score", initialScore);
        FXGL.set("lives", initialLives);
        resetFadeState();
        if (livesManager != null) {
            livesManager.setLives(initialLives);
        }
        if (scoreManager != null) {
            scoreManager.setScore(initialScore);
        }

        // Reset level completion and transition flags
        isLevelCompleted = false;
        isTransitioning = false;
        levelCompletedMessageShown = false;
        proceedToNextLevelCalled = false;
        Runnable levelReloadGuard = beginLevelReloadGuard();
        com.arcadeblocks.gameplay.Ball.clearAttachedBalls();

        com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(levelNumber);
        if (levelData == null) {
            System.err.println("Level " + levelNumber + " not found in configuration!");
            return;
        }

        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;

        prepareLevelAudio(levelNumber);

        clearLevelBackground();

        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(this::removeEntitySafely);

        clearUINodesSafely();
        
        // КРИТИЧНО: Если есть overlayRemover, НЕ вызываем cleanupActiveVideoResources()
        // Оверлей от видео босса должен остаться как черный фон под сюжетным окном
        if (overlayRemover == null) {
            // CRITICAL: Clean all active video resources before starting new level
            // This prevents memory leaks when transitioning from debug menu to special level
            cleanupActiveVideoResources();
        }

        FXGL.set("level", levelNumber);

        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);

        Runnable startLevelTask = () -> {
            try {
                startGame();
                setLevelBackground(levelNumber, levelData.getBackgroundImage());

                if (debugLivesOverride != null && livesManager != null) {
                    livesManager.setLives(debugLivesOverride);
                    FXGL.set("lives", debugLivesOverride);
                }
                if (debugScoreOverride != null && scoreManager != null) {
                    scoreManager.setScore(debugScoreOverride);
                    FXGL.set("score", debugScoreOverride);
                }

                com.arcadeblocks.gameplay.Brick.resetBrickCounter();
                com.arcadeblocks.levels.LevelLoader.loadLevelFromJson(levelData.getLevelFile());

                playLevelMusic(levelNumber);
                playPaddleBallFadeIn(false);
                FXGL.runOnce(levelReloadGuard, LEVEL_FADE_DURATION);

            } catch (Exception e) {
                levelReloadGuard.run();
                System.err.println("Error starting debug level " + levelNumber + ": " + e.getMessage());
                e.printStackTrace();
            }
        };
        ChapterStoryData chapterStory = StoryConfig.findForLevel(levelNumber)
            .filter(data -> alwaysShowChapterStory || (StoryConfig.shouldShowForLevel(levelNumber, new StoryConfig.GameProgress(shownChapterStoryChapters, true)) && shouldShowLevel1Story(levelNumber)))
            .orElse(null);

        if (chapterStory != null) {
            showChapterStory(chapterStory, levelNumber, levelData.getName(), startLevelTask, overlayRemover);
            return;
        }
        
        runOverlayRemover(overlayRemover);

        if (metadata != null && metadata.getIntroVideoPath() != null && shouldShowLevel1StoryAndVideo(levelNumber)) {
            playLevelIntroVideo(levelNumber, metadata, startLevelTask);
        } else {
            // levelName passed for compatibility, but LevelIntroView uses getLocalizedLevelName
            // (DEBUG) label is added in LevelIntroView based on isDebugMode()
            showLevelIntro(levelNumber, levelData.getName(), startLevelTask);
        }
    }
    
    
    /**
     * Check if we are in debug mode
     */
    public boolean isDebugMode() {
        return isDebugMode;
    }
    
    public boolean isAlwaysShowChapterStory() {
        return alwaysShowChapterStory;
    }
    
    public void setAlwaysShowChapterStory(boolean alwaysShowChapterStory) {
        this.alwaysShowChapterStory = alwaysShowChapterStory;
    }
    
    public com.arcadeblocks.config.DifficultyLevel getDebugDifficultyOverride() {
        return debugDifficultyOverride;
    }
    
    public Integer getDebugLivesOverride() {
        return debugLivesOverride;
    }
    
    public void setDebugLivesOverride(Integer lives) {
        if (lives == null) {
            debugLivesOverride = null;
        } else {
            debugLivesOverride = Math.max(1, lives);
        }
    }
    
    public Integer getDebugScoreOverride() {
        return debugScoreOverride;
    }
    
    public void setDebugScoreOverride(Integer score) {
        if (score == null) {
            debugScoreOverride = null;
        } else {
            debugScoreOverride = Math.max(0, score);
        }
    }
    
    public com.arcadeblocks.config.DifficultyLevel getEffectiveDifficulty() {
        if (isDebugMode && debugDifficultyOverride != null) {
            return debugDifficultyOverride;
        }
        if (saveManager != null) {
            try {
                com.arcadeblocks.config.DifficultyLevel difficulty = saveManager.getGameDifficulty();
                if (difficulty != null) {
                    return difficulty;
                }
            } catch (Exception ignored) {}
            try {
                com.arcadeblocks.config.DifficultyLevel fallback = saveManager.getDifficulty();
                if (fallback != null) {
                    return fallback;
                }
            } catch (Exception ignored) {}
        }
        return com.arcadeblocks.config.DifficultyLevel.NORMAL;
    }
    
    /**
     * Reset debug mode
     */
    public void resetDebugMode() {
        isDebugMode = false;
        debugDifficultyOverride = null;
        debugLivesOverride = null;
        debugScoreOverride = null;
    }
    
    private void prepareLevelAudio(int levelNumber) {
        currentLevelLoadingSound = AudioConfig.getLevelLoadingSound(levelNumber);
        List<String> levelHitSounds = AudioConfig.getBrickHitSounds(levelNumber);
        if (levelHitSounds == null || levelHitSounds.isEmpty()) {
            currentBrickHitSounds = new ArrayList<>(AudioConfig.getDefaultBrickHitSounds());
        } else {
            currentBrickHitSounds = new ArrayList<>(levelHitSounds);
        }
        List<String> completionSounds = AudioConfig.getLevelCompletionSounds(levelNumber);
        if (completionSounds == null || completionSounds.isEmpty()) {
            currentLevelCompletionSounds = new ArrayList<>(AudioConfig.getDefaultLevelCompletionSounds());
        } else {
            currentLevelCompletionSounds = new ArrayList<>(completionSounds);
        }
    }
    
    private double getLevelCompletionPauseSeconds(int levelNumber) {
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        if (metadata != null && metadata.getCompletionPauseSeconds() != null) {
            return metadata.getCompletionPauseSeconds();
        }
        return com.arcadeblocks.config.LevelConfig.isBossLevel(levelNumber) ? 12.0 : 4.0;
    }

    private void showLevelCompletionMessage(int levelNumber) {
        boolean alreadyShowing = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> "levelCompletionMessage".equals(node.getUserData()));
        if (alreadyShowing) {
            return;
        }

        com.arcadeblocks.localization.LocalizationManager localizationManager = 
            com.arcadeblocks.localization.LocalizationManager.getInstance();
        String playerName = saveManager != null ? saveManager.getPlayerName() : null;
        if (playerName == null || playerName.isBlank()) {
            playerName = localizationManager.get("player.default");
        }
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        String message = metadata != null ? metadata.formatCompletionMessage(playerName) : null;
        if (message == null || message.isBlank()) {
            // Use localized message
            message = localizationManager.format("level.completion.message", playerName, levelNumber);
        }

        // Get chapter color for the level
        com.arcadeblocks.config.LevelConfig.LevelChapter chapter = 
            com.arcadeblocks.config.LevelConfig.getChapter(levelNumber);
        String chapterColor = (chapter != null) ? chapter.getAccentColorHex() : GameConfig.NEON_PINK;

        Label label = new Label(message);
        label.setFont(javafx.scene.text.Font.font("Orbitron", javafx.scene.text.FontWeight.BOLD, 34));
        label.setTextFill(Color.web(chapterColor));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(GameConfig.GAME_WIDTH * 0.75);

        // We soften the glow
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(chapterColor, 0.6));
        glow.setRadius(10);
        glow.setSpread(0.25);
        label.setEffect(glow);

        // Dark translucent container for text
        javafx.scene.layout.StackPane backdrop = new javafx.scene.layout.StackPane();
        backdrop.setStyle("-fx-background-color: rgba(15,15,28,0.45); -fx-background-radius: 14px;");
        backdrop.setPadding(new Insets(18, 24, 18, 24));
        javafx.scene.layout.StackPane content = new javafx.scene.layout.StackPane(label);
        content.setPadding(Insets.EMPTY);
        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(backdrop, content);
        wrapper.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMouseTransparent(true);
        wrapper.setOpacity(0);
        wrapper.setUserData("levelCompletionMessage");

        FXGL.getGameScene().addUINode(wrapper);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(450), wrapper);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(1.6), label);
        scaleTransition.setFromX(1.0);
        scaleTransition.setToX(1.05);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToY(1.05);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(Animation.INDEFINITE);

        TranslateTransition levitation = new TranslateTransition(Duration.seconds(2.2), label);
        levitation.setFromY(0);
        levitation.setToY(-10);
        levitation.setAutoReverse(true);
        levitation.setCycleCount(Animation.INDEFINITE);

        double displaySeconds = Math.max(4.0, getLevelCompletionPauseSeconds(levelNumber));

        PauseTransition hold = new PauseTransition(Duration.seconds(displaySeconds));
        hold.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(450), wrapper);
            fadeOut.setFromValue(wrapper.getOpacity());
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> {
                scaleTransition.stop();
                levitation.stop();
                removeUINodeSafely(wrapper);
            });
            fadeOut.play();
        });

        fadeIn.setOnFinished(e -> {
            scaleTransition.play();
            levitation.play();
            hold.play();
        });

        fadeIn.play();
    }
    
    /**
     * Start specified level
     */
    public void startLevel(int levelNumber) {
        startLevel(levelNumber, false);
    }
    
    /**
     * Start specified level with option to reset score
     */
    public void startLevel(int levelNumber, boolean resetScore) {
        startLevel(levelNumber, resetScore, null);
    }
    
    /**
     * КРИТИЧНО: Перегруженная версия для бесшовного перехода от видео к сюжетному окну
     * overlayRemover удаляется только когда сюжетное окно уже показано
     */
    private void startLevel(int levelNumber, boolean resetScore, Runnable overlayRemover) {
        // Reset level completion and transition flags - resume gameplay
        isLevelCompleted = false;
        isTransitioning = false;
        isGameOver = false;
        levelCompletedMessageShown = false;
        proceedToNextLevelCalled = false;

        // Удаляем ссылки на фон главного меню, чтобы его текстуры не висели в памяти в игровом мире
        releaseMainMenuBackgroundAssets();
        
        // Reset paddle input flags for proper control operation after restart
        resetPaddleInputFlags();
        // System.out.println("Flags isLevelCompleted, isTransitioning, levelCompletedMessageShown and proceedToNextLevelCalled reset");
        Runnable levelReloadGuard = beginLevelReloadGuard();
        
        // Clear attached balls list when starting new level
        com.arcadeblocks.gameplay.Ball.clearAttachedBalls();
        
        // Reset pause flag when starting new level
        isPaused = false;
        
        // Get level data
        com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(levelNumber);
        if (levelData == null) {
            System.err.println("Level " + levelNumber + " not found in configuration!");
            return;
        }
        // System.out.println("Level data obtained: " + levelData.getName());

        if (scoreManager != null) {
            scoreManager.hideBossHealth();
        }
        
        prepareLevelAudio(levelNumber);
        
        // Clear background images before showing loading screen
        clearLevelBackground();
        
        // Clear all entities (create copy of list for safe removal)
        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(this::removeEntitySafely);
        
        // Clear current scene after removing entities
        clearUINodesSafely();
        
        // КРИТИЧНО: Если есть overlayRemover, НЕ вызываем cleanupActiveVideoResources()
        // Оверлей от видео босса должен остаться как черный фон под сюжетным окном
        // Он будет удален через overlayRemover когда сюжетное окно закроется
        if (overlayRemover == null) {
            // Clean all active video resources before starting new level
            // This prevents memory leaks when transitioning between levels
            cleanupActiveVideoResources();
        }
        
        // Set level
        FXGL.set("level", levelNumber);
        if (saveManager != null) {
            saveManager.setCurrentLevel(levelNumber);
        }
        // System.out.println("Level set in FXGL: " + levelNumber);
        
        // Reset score only if explicitly requested
        if (resetScore) {
            FXGL.set("score", 0);
            resetContinueCount();
            // Set lives count based on difficulty
            com.arcadeblocks.config.DifficultyLevel difficulty = getEffectiveDifficulty();
            FXGL.set("lives", difficulty.getLives());
            if (saveManager != null) {
                saveManager.setLives(difficulty.getLives());
            }
        } else {
            // Load saved data from SaveManager
            FXGL.set("score", saveManager.getScore());
            int savedLives = saveManager.getLives();
            
            // Protection from starting level with 0 lives
            if (savedLives <= 0) {
                // System.out.println("Detected save with 0 lives - deleting save and starting new game");
                saveManager.deleteSaveFileForSlot(getOriginalSaveSlot());
                // Set lives count based on difficulty
                com.arcadeblocks.config.DifficultyLevel difficulty = getEffectiveDifficulty();
                savedLives = difficulty.getLives();
                saveManager.setLives(savedLives);
            }
            
            FXGL.set("lives", savedLives);
        }
        resetFadeState();
        

        Runnable initializeLevelTask = () -> {
            try {
        // System.out.println("Starting level initialization " + levelNumber);
                
                startGame();
        // System.out.println("Game scene initialized");
                
                setLevelBackground(levelNumber, levelData.getBackgroundImage());
        // System.out.println("Level background set");

                boolean restored = false;
                if (saveManager != null && saveManager.isResumeSnapshotPending()) {
                    GameSnapshot snapshot = saveManager.loadGameSnapshot();
                    if (snapshot != null && snapshot.level == levelNumber) {
                        restored = restoreGameSnapshot(snapshot);
                        if (restored) {
                            saveManager.markResumeSnapshotConsumed();
                        }
                        // Очищаем snapshot после использования
                        snapshot.clear();
                    } else if (snapshot != null) {
                        // Очищаем snapshot если он не подошел
                        snapshot.clear();
                    }
                }

                if (!restored) {
                    com.arcadeblocks.gameplay.Brick.resetBrickCounter();
                    com.arcadeblocks.levels.LevelLoader.loadLevelFromJson(levelData.getLevelFile());
        // System.out.println("Level " + levelNumber + " loaded from JSON: " + levelData.getName());
                }

                // Show countdown timer only if game was restored from save
                if (restored) {
                    showCountdownTimerForLevelStart(levelNumber);
                    playPaddleBallFadeIn(true);
                } else {
                    // If game was not restored, start music immediately
                    playLevelMusic(levelNumber);
                    playPaddleBallFadeIn(false);
        // System.out.println("Level music started");
                }
                
                javafx.util.Duration guardDelay = restored ? javafx.util.Duration.millis(240) : LEVEL_FADE_DURATION;
                FXGL.runOnce(levelReloadGuard, guardDelay);

        // System.out.println("Level " + levelNumber + " fully initialized!");
            } catch (Exception e) {
                levelReloadGuard.run();
                System.err.println("Error initializing level " + levelNumber + ": " + e.getMessage());
                e.printStackTrace();
            }
        };

        ChapterStoryData chapterStory = StoryConfig.findForLevel(levelNumber)
            .filter(data -> alwaysShowChapterStory || (StoryConfig.shouldShowForLevel(levelNumber, new StoryConfig.GameProgress(shownChapterStoryChapters, true)) && shouldShowLevel1Story(levelNumber)))
            .orElse(null);

        if (chapterStory != null) {
            showChapterStory(chapterStory, levelNumber, levelData.getName(), initializeLevelTask, overlayRemover);
            return;
        }
        
        runOverlayRemover(overlayRemover);

        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);

        if (metadata != null && metadata.getIntroVideoPath() != null && shouldShowLevel1StoryAndVideo(levelNumber)) {
            setLevelIntroActive(true);
            playLevelIntroVideo(levelNumber, metadata, initializeLevelTask);
        } else {
            showLevelIntro(levelNumber, levelData.getName(), initializeLevelTask);
        }
    }

    private void showChapterStory(ChapterStoryData storyData, int levelNumber, String levelName, Runnable onComplete, Runnable overlayRemover) {
        clearLetterboxOverlay();

        // КРИТИЧНО: НЕ удаляем видео оверлей здесь!
        // Видео оверлей черный (black), а сюжетное окно полупрозрачное
        // Если удалить видео оверлей, игровой мир будет виден через полупрозрачный фон
        // Поэтому оставляем видео оверлей как черный фон под сюжетным окном
        // И удаляем его только когда закрывается сюжетное окно
        
        ChapterStoryView storyView = new ChapterStoryView(this, storyData, () -> {
            // Удаляем видео оверлей ПОСЛЕ закрытия сюжетного окна
            runOverlayRemover(overlayRemover);
            handleChapterStoryClosed(storyData, levelNumber, levelName, onComplete);
        });
        activeChapterStoryView = storyView;
        setStoryOverlayActive(true);
        FXGL.getGameScene().addUINode(storyView);
    }

    private void handleChapterStoryClosed(ChapterStoryData storyData, int levelNumber, String levelName, Runnable onComplete) {
        if (storyData != null) {
            shownChapterStoryChapters.add(storyData.chapterNumber());
        }
        dismissActiveChapterStoryView();
        setStoryOverlayActive(false);
        
        // КРИТИЧНО: Принудительно очищаем все активные ресурсы после долгого нахождения на сюжетном окне
        // Это предотвращает race condition между cleanup ChapterStoryView и началом видео
        cleanupActiveVideoResources();
        clearUINodesSafely();
        
        // Check if level has intro video after story window closes
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        
        if (metadata != null && metadata.getIntroVideoPath() != null && shouldShowLevel1StoryAndVideo(levelNumber)) {
            // КРИТИЧНО: Добавляем небольшую задержку для завершения cleanup операций
            // Это предотвращает race condition при переходе от сюжета к видео
            Platform.runLater(() -> {
                setLevelIntroActive(true);
                playLevelIntroVideo(levelNumber, metadata, onComplete);
            });
        } else {
            showLevelIntro(levelNumber, levelName, onComplete);
        }
    }
    
    /**
     * Show level loading screen
     */
    private void showLevelIntro(int levelNumber, String levelName, Runnable onComplete) {
        // Remove game world borders during Level Intro display
        clearLetterboxOverlay();
        
        cleanupLevelIntroViews();

        com.arcadeblocks.ui.LevelIntroView levelIntro = new com.arcadeblocks.ui.LevelIntroView(
            levelNumber, levelName, () -> {
                setLevelIntroActive(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        );
        
        // Add a loading screen on top of everything
        FXGL.getGameScene().addUINode(levelIntro);
    }
    
    /**
     * Setting the background image for the level
     */
    private void setLevelBackground(int levelNumber, String backgroundImage) {
        if (backgroundImage == null || backgroundImage.isEmpty()) {
            return;
        }
        
        try {
            // Loading background image
            Image image = ImageCache.get(backgroundImage);
            if (image == null) {
                return;
            }
            
            // Create an ImageView for the background
            javafx.scene.image.ImageView backgroundImageView = new javafx.scene.image.ImageView(image);
            
            // Use the current resolution for the background (as in the main menu and Level Intro)
            // so that the background fills the entire screen, including the letterbox areas
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            backgroundImageView.setFitWidth(currentRes.getWidth());
            backgroundImageView.setFitHeight(currentRes.getHeight());
            backgroundImageView.setPreserveRatio(false);
            backgroundImageView.setSmooth(false);
            backgroundImageView.setCache(false);

            // We do not use letterbox offset - the background should cover the entire screen
            backgroundImageView.setTranslateX(0);
            backgroundImageView.setTranslateY(0);
            backgroundImageView.setLayoutX(0);
            backgroundImageView.setLayoutY(0);
            
            // Add the background as a UI element at the very beginning (so that it is behind all other elements)
            FXGL.getGameScene().getContentRoot().getChildren().add(0, backgroundImageView);
            levelBackgroundNodes.add(backgroundImageView);
            if (darknessOverlayActive) {
                backgroundImageView.setVisible(false);
                backgroundImageView.setOpacity(0.0);
            }
            
        } catch (Exception e) {
            System.err.println("An error occurred while loading the background for the level. " + levelNumber + " (" + backgroundImage + "): " + e.getMessage());
        }
    }
    
    /**
     * Cleans up background images by freeing up VRAM.
     * Goes through all tracked nodes, removes them from ContentRoot, and,
     * most importantly, calls ImageCache.forget() + setImage(null) for each ImageView.
     * This ensures that JavaFX has no live Image references left and the corresponding
     * d3d11 textures are actually freed.
     */
    private void clearLevelBackground() {
        try {
            // Go through all previously added nodes
            for (javafx.scene.Node node : new java.util.ArrayList<>(levelBackgroundNodes)) {
                if (node instanceof javafx.scene.image.ImageView imageView) {
                    // Clear the image from the cache before deleting it.
                    javafx.scene.image.Image image = imageView.getImage();
                    if (image != null) {
                        ImageCache.forget(image);
                    }
                    imageView.setImage(null);
                }
            }
            
            // Remove all ImageView elements from ContentRoot
            var children = FXGL.getGameScene().getContentRoot().getChildren();
            children.removeAll(levelBackgroundNodes);
            levelBackgroundNodes.clear();
            
        // System.out.println("All background images have been cleared.");
        } catch (Exception e) {
            System.err.println("Error clearing background images: " + e.getMessage());
        }
    }

    /**
     * Complete clearing of the active game state to transition to non-game screens
     */
    private void cleanupGameplayState() {
        try {
            stopAllBonuses();
        } catch (Exception ignored) {}

        try {
            stopAllBalls();
        } catch (Exception ignored) {}

        uninstallMousePaddleControlHandlers();
        unblockMouseClicks();

        clearLevelBackground();
        dismissActiveChapterStoryView();
        
        // Clean up active video resources to prevent memory leaks
        cleanupActiveVideoResources();
        
        // Clear darkness overlay to free up VRAM
        if (darknessOverlayCapsule != null && darknessOverlayCapsule.getImage() != null) {
            ImageCache.forget(darknessOverlayCapsule.getImage());
            darknessOverlayCapsule.setImage(null);
        }
        darknessOverlayCapsule = null;
        darknessOverlayGroup = null;
        darknessOverlayDimLayer = null;

        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(this::removeEntitySafely);

        Brick.resetBrickCounter();

        if (gameplayUIView != null) {
            gameplayUIView.cleanup();
            FXGL.getGameScene().removeUINode(gameplayUIView);
        }

        if (bonusTimerView != null) {
            // Clear BonusTimerView before deletion
            bonusTimerView.cleanup();
            FXGL.getGameScene().removeUINode(bonusTimerView);
        }

        if (scoreManager != null) {
            scoreManager.hideBossHealth();
            scoreManager.stopLevelTimer();
        }

        paddleComponent = null;
        // Clearing LivesManager before resetting
        if (livesManager != null) {
            livesManager.cleanup();
        }
        livesManager = null;
        bonusEffectManager = null;
        bonusTimerView = null;
        gameplayUIView = null;
        levelPassBonusEntity = null;
        levelPassBonusSpawned = false;
        mouseClicksBlocked = false;
        isLevelCompleted = false;
        isTransitioning = false;
        levelCompletedMessageShown = false;
        isGameOver = false;
        isLevelIntroActive = false;
        isStoryOverlayActive = false;
        isVictorySequenceActive = false;
        proceedToNextLevelCalled = false;
    }
    
    /**
     * Clearing up active video resources (PauseTransition, VLC players, overlay) to prevent memory leaks
     */
    private void cleanupActiveVideoResources() {
        // Increment the video session token to prevent race conditions.
        // This allows old callbacks to understand that they are outdated and should not perform final cleanup.
        videoSessionToken++;
        
        // Stop and clear all active PauseTransitions
        synchronized (activeVideoPauseTransitions) {
            for (javafx.animation.PauseTransition transition : activeVideoPauseTransitions) {
                try {
                    if (transition != null) {
                        transition.stop();
                    }
                } catch (Exception ignored) {}
            }
            activeVideoPauseTransitions.clear();
        }
        
        // Clear all active video overlays
        synchronized (activeVideoOverlays) {
            for (VideoOverlayWrapper wrapper : activeVideoOverlays) {
                try {
                    if (wrapper != null) {
                        // First, clear the backend before clearing UI elements.
                        // This prevents rendering attempts on already deleted ImageView elements.
                        if (wrapper.backend != null) {
                            try {
                                // System.out.println("Clearing the video backend during cleanupActiveVideoResources...");
                                wrapper.backend.cleanup();
                            } catch (Exception e) {
                                System.err.println("Error while clearing video backend: " + e.getMessage());
                            }
                        }
                        
                        // Now we clean the UI elements
                        javafx.scene.Node overlay = wrapper.overlay;
                        if (overlay != null) {
                            // If the overlay contains ImageView, clear them.
                            if (overlay instanceof javafx.scene.layout.Pane) {
                                javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) overlay;
                                for (javafx.scene.Node child : pane.getChildren()) {
                                    if (child instanceof javafx.scene.image.ImageView) {
                                        javafx.scene.image.ImageView imageView = (javafx.scene.image.ImageView) child;
                                        // Clear the image to free memory
                                        imageView.setImage(null);
                                    }
                                }
                            }
                            
                            // Unbind ResponsiveLayoutHelper listeners
                            if (overlay instanceof javafx.scene.layout.Region) {
                                try {
                                    com.arcadeblocks.ui.util.ResponsiveLayoutHelper.unbind((javafx.scene.layout.Region) overlay);
                                } catch (Exception e) {
                                    System.err.println("Error when unlinking ResponsiveLayoutHelper:" + e.getMessage());
                                }
                            }
                            
                            // Remove from the scene
                            try {
                                FXGL.getGameScene().removeUINode(overlay);
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error during clearing video overlay wrapper: " + e.getMessage());
                }
            }
            activeVideoOverlays.clear();
        }
        
        // Stop and clear all active VLC players
        try {
            com.arcadeblocks.video.VlcContext.getInstance().dispose();
        } catch (Exception e) {
            System.err.println("Error cleaning up VLC resources in cleanupActiveVideoResources: " + e.getMessage());
        }
    }

    private boolean isGameplayState() {
        if (paddleComponent == null || gameplayUIView == null) {
            return false;
        }

        if (!FXGL.getGameScene().getUINodes().contains(gameplayUIView)) {
            return false;
        }

        return !isLoading
            && !isLevelIntroActive
            && !isStoryOverlayActive
            && !isCountdownActive
            && !isLevelCompleted
            && !isTransitioning
            && !isGameOver
            && !isVictorySequenceActive;
    }

    public void enableDarknessOverlay() {
        if (darknessOverlayActive) {
            return;
        }

        ensureDarknessOverlayInitialized();
        updateDarknessOverlayLayout();

        var contentRoot = FXGL.getGameScene().getContentRoot();
        if (!contentRoot.getChildren().contains(darknessOverlayGroup)) {
            contentRoot.getChildren().add(darknessOverlayGroup);
        }

        darknessOverlayGroup.setVisible(true);
        darknessOverlayGroup.toFront();
        darknessOverlayActive = true;
        setLevelBackgroundsVisible(false);
    }

    public void disableDarknessOverlay() {
        var contentRoot = FXGL.getGameScene().getContentRoot();
        boolean removed = contentRoot.getChildren().removeIf(node -> "darknessOverlay".equals(node.getUserData()));

        if (darknessOverlayGroup != null) {
            contentRoot.getChildren().remove(darknessOverlayGroup);
            darknessOverlayGroup.setVisible(false);
            removed = true;
        }

        darknessOverlayActive = false;

        setLevelBackgroundsVisible(true);

        // Clear the image from ImageCache before resetting
        if (darknessOverlayCapsule != null && darknessOverlayCapsule.getImage() != null) {
            ImageCache.forget(darknessOverlayCapsule.getImage());
            darknessOverlayCapsule.setImage(null);
        }
        
        // Clear children from group before resetting
        if (darknessOverlayGroup != null) {
            darknessOverlayGroup.getChildren().clear();
        }
        
        darknessOverlayGroup = null;
        darknessOverlayDimLayer = null;
        darknessOverlayCapsule = null;

        if (removed) {
            FXGL.getGameScene().getRoot().requestLayout();
        }
    }

    private void setLevelBackgroundsVisible(boolean visible) {
        var contentRoot = FXGL.getGameScene().getContentRoot().getChildren();
        levelBackgroundNodes.removeIf(node -> node == null || !contentRoot.contains(node));

        for (Node node : levelBackgroundNodes) {
            node.setVisible(visible);
            if (visible) {
                node.setOpacity(1.0);
            } else {
                node.setOpacity(0.0);
            }
        }
    }

    private void ensureDarknessOverlayInitialized() {
        if (darknessOverlayGroup == null) {
            darknessOverlayDimLayer = new Rectangle();
            darknessOverlayDimLayer.setManaged(false);
            darknessOverlayDimLayer.setMouseTransparent(true);
            darknessOverlayDimLayer.setFill(Color.BLACK);
            darknessOverlayDimLayer.setOpacity(0.72);

            darknessOverlayCapsule = new ImageView();
            darknessOverlayCapsule.setManaged(false);
            darknessOverlayCapsule.setMouseTransparent(true);
            darknessOverlayCapsule.setPreserveRatio(true);
            // Disable anti-aliasing for clear display
            darknessOverlayCapsule.setSmooth(false);
            try {
                Image capsuleImage = ImageCache.get("darkness.png");
                darknessOverlayCapsule.setImage(capsuleImage);
            } catch (Exception e) {
                darknessOverlayCapsule.setImage(null);
            }

            darknessOverlayGroup = new Group(darknessOverlayDimLayer, darknessOverlayCapsule);
            darknessOverlayGroup.setManaged(false);
            darknessOverlayGroup.setMouseTransparent(true);
            darknessOverlayGroup.setUserData("darknessOverlay");
        }

        // Use the current resolution to adapt to 1920x1080
        com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
        darknessOverlayDimLayer.setWidth(currentRes.getWidth());
        darknessOverlayDimLayer.setHeight(currentRes.getHeight());
        darknessOverlayDimLayer.setOpacity(0.72);
    }

    private void updateDarknessOverlayLayout() {
        if (darknessOverlayCapsule != null && darknessOverlayCapsule.getImage() != null) {
            // Use the current resolution to adapt to 1920x1080
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            
            double baseWidth = darknessOverlayCapsule.getImage().getWidth();
            double baseHeight = darknessOverlayCapsule.getImage().getHeight();
            double maxWidth = currentRes.getWidth() * 0.85;
            double scale = Math.min(1.0, maxWidth / baseWidth);
            double finalWidth = baseWidth * scale;
            double finalHeight = baseHeight * scale;

            darknessOverlayCapsule.setFitWidth(finalWidth);
            darknessOverlayCapsule.setFitHeight(finalHeight);
            darknessOverlayCapsule.setLayoutX((currentRes.getWidth() - finalWidth) / 2.0);
            darknessOverlayCapsule.setLayoutY((currentRes.getHeight() - finalHeight) / 2.0);
        }
    }
    
    /**
     * Play music for level
     */
    private void playLevelMusic(int levelNumber) {
        try {
            if (audioManager == null || !audioManager.isInitialized()) {
                System.err.println("Audio manager not initialized, skipping music for level " + levelNumber);
                return;
            }
            
            // Get music from level configuration
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(levelNumber);
            String musicFile = null;
            
            if (levelData != null && levelData.getMusicFile() != null && !levelData.getMusicFile().isEmpty()) {
                musicFile = levelData.getMusicFile();
            } else {
                // Fallback to old system for levels without music in LevelConfig
                switch (levelNumber) {
                    case 0:
                        musicFile = com.arcadeblocks.config.AudioConfig.LEVEL0_MUSIC;
                        break;
                    case 1:
                        musicFile = com.arcadeblocks.config.AudioConfig.LEVEL1_MUSIC;
                        break;
                    default:
        // System.out.println("Music for level " + levelNumber + " not configured");
                        return;
                }
            }
            
            if (musicFile != null) {
        // System.out.println("Starting music for level " + levelNumber + ": " + musicFile);
                audioManager.playMusic(musicFile, true); // true = looped
            }
        } catch (Exception e) {
            System.err.println("Error playing music for level " + levelNumber + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    
    
    /**
     * Apply window settings (fullscreen/windowed mode)
     */
    public void applyWindowSettings() {
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                if (stage != null) {
                    boolean isFullscreen = saveManager.isFullscreen();
                    boolean isWindowed = saveManager.isWindowedMode();
                    // Use current resolution
                    com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
                    int targetW = currentRes.getWidth();
                    int targetH = currentRes.getHeight();
                    
                    // If windowed mode enabled, exit fullscreen
                    if (isWindowed) {
                        stage.setFullScreen(false);
                        stage.setMaximized(false);
                        stage.setResizable(false); // BLOCK manual resizing
                        // Window style remains unchanged (cannot change after showing)
                        // Set selected window resolution
                        stage.setWidth(targetW);
                        stage.setHeight(targetH);
                        stage.centerOnScreen();
                        // Center game world
                        centerGameWorld();
                        // Update letterbox bars only for gameplay
                        updateLetterboxOverlay();
        // System.out.println("Switched to windowed mode");
                    } else if (isFullscreen) {
                        // Fullscreen mode
                        stage.setFullScreen(true);
                        // Center game world
                        centerGameWorld();
                        updateLetterboxOverlay();
        // System.out.println("Switched to fullscreen mode");
                    } else {
                        // If both modes are off, use fullscreen mode by default
                        stage.setFullScreen(true);
                        // Center game world
                        centerGameWorld();
                        updateLetterboxOverlay();
        // System.out.println("Set fullscreen mode by default");
                    }
                }
            } catch (Exception e) {
                // System.err.println("Failed to apply window settings: " + e.getMessage());
            }
        });
    }
    
    /**
     * Apply screen resolution settings
     */
    public void applyResolutionSettings(com.arcadeblocks.config.Resolution newResolution) {
        if (newResolution == null) {
            return;
        }
        
        // Save the new resolution
        GameConfig.setCurrentResolution(newResolution);
        
        javafx.application.Platform.runLater(() -> {
            try {
                Stage stage = (Stage) FXGL.getGameScene().getRoot().getScene().getWindow();
                if (stage != null) {
                    // Get the current window mode
                    boolean isFullscreen = stage.isFullScreen();
                    
                    // Set a new resolution
                    stage.setWidth(newResolution.getWidth());
                    stage.setHeight(newResolution.getHeight());
                    
                    // Center the window if it is in windowed mode
                    if (!isFullscreen) {
                        stage.centerOnScreen();
                    }
                    
                    // Center the game world on the screen
                    centerGameWorld();
                    
                    // Update letterbox for new resolution
                    updateLetterboxOverlay();
                    
                    // System.out.println("New resolution applied: " + newResolution);
                }
            } catch (Exception e) {
                System.err.println("Error occured while applying resolution settings: " + e.getMessage());
            }
        });
    }
    
    /**
     * Centers the game world on the screen when changing resolution
     */
    private void centerGameWorld() {
        try {
            // Calculate the offset for centering
            double offsetX = GameConfig.getLetterboxOffsetX();
            double offsetY = GameConfig.getLetterboxOffsetY();
            
            // Use viewport for offset instead of translate (which is bound in FXGL)
            // Negative viewport offset shifts the visible area to the left/up,
            // which visually centers the content
            FXGL.getGameScene().getViewport().setX(-offsetX);
            FXGL.getGameScene().getViewport().setY(-offsetY);
            
            // System.out.println("The game world is centered through the viewport: offsetX=" + offsetX + ", offsetY=" + offsetY);
            
            centerAllUINodes();
            
             // Update the position of the bonus indicator
            if (scoreManager != null) {
                scoreManager.updateBonusIndicatorPosition();
            }
        } catch (Exception e) {
            System.err.println("Game world centering error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh/clear dark areas around the game world (only during gameplay).
     */
    public void updateLetterboxOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                var root = FXGL.getGameScene().getRoot();
                // Removing old overlays
                root.getChildren().removeIf(n -> "letterboxOverlay".equals(n.getUserData()));
                root.getChildren().removeIf(n -> "gameWorldFrame".equals(n.getUserData()));
                
                javafx.stage.Stage stage = FXGL.getPrimaryStage();
                if (stage == null) return;
                
                // Get the current window resolution
                com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
                int windowW = currentRes.getWidth();
                int windowH = currentRes.getHeight();
                
                // The game world is always 1600x900
                int gameWorldW = GameConfig.GAME_WORLD_WIDTH;
                int gameWorldH = GameConfig.GAME_WORLD_HEIGHT;
                
                // If the window resolution is larger than the game world, add semi-transparent fields.
                boolean hasLetterbox = windowW > gameWorldW || windowH > gameWorldH;
                if (hasLetterbox) {
                    // Vertical fields (pillarbox)
                    if (windowW > gameWorldW) {
                        double sideWidth = (windowW - gameWorldW) / 2.0;
                        var leftBox = createLetterboxRegion(sideWidth, windowH);
                        leftBox.setLayoutX(0);
                        leftBox.setLayoutY(0);
                        var rightBox = createLetterboxRegion(sideWidth, windowH);
                        rightBox.setLayoutX(windowW - sideWidth);
                        rightBox.setLayoutY(0);
                        root.getChildren().addAll(leftBox, rightBox);
                    }
                    // Horizontal fields (letterbox)
                    if (windowH > gameWorldH) {
                        double topBottomHeight = (windowH - gameWorldH) / 2.0;
                        var topBox = createLetterboxRegion(windowW, topBottomHeight);
                        topBox.setLayoutX(0);
                        topBox.setLayoutY(0);
                        var bottomBox = createLetterboxRegion(windowW, topBottomHeight);
                        bottomBox.setLayoutX(0);
                        bottomBox.setLayoutY(windowH - topBottomHeight);
                        root.getChildren().addAll(topBox, bottomBox);
                    }
                    
                }

                // Update the position of the bonus indicator
                if (scoreManager != null && scoreManager.getBonusIndicator() != null) {
                    scoreManager.updateBonusIndicatorPosition();
                }
                
                // The background always fills the entire screen, the position does not change
            } catch (Exception ignored) {}
        });
    }

    private javafx.scene.layout.Region createLetterboxRegion(double w, double h) {
        var r = new javafx.scene.layout.Region();
        r.setPrefSize(w, h);
        r.setUserData("letterboxOverlay");
        r.setMouseTransparent(true);
        // Style as in the main menu - semi-transparent dark blue container (80% transparent)
        r.setStyle("-fx-background-color: rgba(26, 26, 46, 0.15);");
        return r;
    }
    
    /**
     * Clear letterbox overlay (frames and panels around the game world)
     */
    public void clearLetterboxOverlay() {
        javafx.application.Platform.runLater(() -> {
            try {
                var root = FXGL.getGameScene().getRoot();
                root.getChildren().removeIf(n -> "letterboxOverlay".equals(n.getUserData()));
                root.getChildren().removeIf(n -> "gameWorldFrame".equals(n.getUserData()));
            } catch (Exception ignored) {}
        });
    }

    public void clearUINodesSafely() {
        // КРИТИЧНО: освобождаем фоновые слои до удаления UI, чтобы предотвратить накопление ImageView
        clearLevelBackground();

        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
        for (javafx.scene.Node node : uiNodes) {
            removeUINodeSafely(node);
        }
    }

    private void dismissActiveChapterStoryView() {
        if (activeChapterStoryView != null) {
            ChapterStoryView view = activeChapterStoryView;
            activeChapterStoryView = null;
            
            // FIRST remove from scene, THEN cleanup
            // removeUINodeSafely already does the correct order: removeUINode → cleanup
            removeUINodeSafely(view);
        }
        isStoryOverlayActive = false;
    }

    private void removeUINodeSafely(javafx.scene.Node node) {
        if (node == null) {
            return;
        }

        if (node == activeChapterStoryView) {
            activeChapterStoryView = null;
            isStoryOverlayActive = false;
        }

        // CRITICAL: FIRST remove from scene, THEN call cleanup
        // This prevents a race condition where the node is still in the scene but already “dead”
        try {
            FXGL.getGameScene().removeUINode(node);
        } catch (Exception e) {
            System.err.println("Failed to remove UI node " + node.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Now we can safely call cleanup via UINodeCleanup
        try {
            UINodeCleanup.cleanupNode(node);
        } catch (Exception e) {
            System.err.println("Failed to cleanup UI node " + node.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void cleanupLevelIntroViews() {
        try {
            var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
            for (javafx.scene.Node node : uiNodes) {
                if (node instanceof com.arcadeblocks.ui.LevelIntroView) {
                    removeUINodeSafely(node);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup LevelIntroView: " + e.getMessage());
        } finally {
            setLevelIntroActive(false);
        }
    }

    private void runOverlayRemover(Runnable overlayRemover) {
        if (overlayRemover == null) {
            cleanupActiveVideoResources();
            return;
        }

        javafx.application.Platform.runLater(() -> {
            try {
                overlayRemover.run();
            } catch (Exception e) {
                System.err.println("Error executing overlay remover: " + e.getMessage());
            } finally {
                cleanupActiveVideoResources();
            }
        });
    }

    // UI helpers for adaptive resolution (menus, dialogs)
    public double getEffectiveUIWidth() {
        try {
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage != null) {
                double w = stage.getWidth();
                if (w > 0) return Math.max(w, com.arcadeblocks.config.GameConfig.GAME_WIDTH);
            }
        } catch (Exception ignored) {}
        return com.arcadeblocks.config.GameConfig.GAME_WIDTH;
    }

    public double getEffectiveUIHeight() {
        try {
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
            if (stage != null) {
                double h = stage.getHeight();
                if (h > 0) return Math.max(h, com.arcadeblocks.config.GameConfig.GAME_HEIGHT);
            }
        } catch (Exception ignored) {}
        return com.arcadeblocks.config.GameConfig.GAME_HEIGHT;
    }

    public void centerUINode(javafx.scene.Node node) {
        if (node == null) {
            return;
        }

        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());

        javafx.application.Platform.runLater(() -> centerUINodeInternal(node, offsetX, offsetY));
    }

    private void centerUINodeInternal(javafx.scene.Node node, double offsetX, double offsetY) {
        if (node == null) {
            return;
        }
        Object marker = node.getUserData();
        if ("letterboxOverlay".equals(marker)) {
            return;
        }
        
        // BonusIndicator is positioned manually via ScoreManager
        if ("bonusIndicator".equals(marker)) {
            return;
        }

        if (node instanceof MainMenuView || "fullScreenOverlay".equals(marker)) {
            node.setTranslateX(0);
            node.setTranslateY(0);
            node.setLayoutX(0);
            node.setLayoutY(0);
            return;
        }

        node.setTranslateX(0);
        node.setTranslateY(0);
        double baseWidth = GameConfig.GAME_WORLD_WIDTH;
        double baseHeight = GameConfig.GAME_WORLD_HEIGHT;

        double nodeWidth = calculateNodeWidth(node);
        double nodeHeight = calculateNodeHeight(node);

        double layoutX = offsetX + Math.max(0, (baseWidth - nodeWidth) / 2.0);
        double layoutY = offsetY + Math.max(0, (baseHeight - nodeHeight) / 2.0);

        node.setLayoutX(layoutX);
        node.setLayoutY(layoutY);
    }

    private double calculateNodeWidth(javafx.scene.Node node) {
        double width = 0;

        if (node instanceof Region region) {
            width = region.prefWidth(-1);
            if (Double.isNaN(width) || width <= 0) {
                width = region.getWidth();
            }
            if (width <= 0) {
                width = region.getLayoutBounds().getWidth();
            }
        } else {
            Bounds bounds = node.getLayoutBounds();
            width = bounds.getWidth();
            if (width <= 0) {
                bounds = node.getBoundsInParent();
                width = bounds.getWidth();
            }
        }

        return width > 0 ? width : GameConfig.GAME_WORLD_WIDTH;
    }

    private double calculateNodeHeight(javafx.scene.Node node) {
        double height = 0;

        if (node instanceof Region region) {
            height = region.prefHeight(-1);
            if (Double.isNaN(height) || height <= 0) {
                height = region.getHeight();
            }
            if (height <= 0) {
                height = region.getLayoutBounds().getHeight();
            }
        } else {
            Bounds bounds = node.getLayoutBounds();
            height = bounds.getHeight();
            if (height <= 0) {
                bounds = node.getBoundsInParent();
                height = bounds.getHeight();
            }
        }

        return height > 0 ? height : GameConfig.GAME_WORLD_HEIGHT;
    }

    public void centerAllUINodes() {
        javafx.application.Platform.runLater(() -> {
            try {
                double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
                double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());
                var nodes = FXGL.getGameScene().getUINodes();
                for (javafx.scene.Node n : nodes) {
                    centerUINodeInternal(n, offsetX, offsetY);
                }
            } catch (Exception ignored) {}
        });
    }
    
    /**
     * Exit the game
     */
    public void exitGame() {
        // Set completion flag to prevent new operations
        shutdownTriggered.set(true);
        
        // First, we clear all resources
        performShutdownIfNeeded();
        
        // Clear all UI components
        cleanupAllUINodes();
        
        // We complete all threads before exiting
        shutdownAllExecutors();
        
        // Stop all active animations and timers
        cleanupAllTimelines();
        
        // Remove all event listeners
        cleanupAllEventListeners();
        
        // Clear all active video resources
        cleanupActiveVideoResources();
        
        // Clear all caches
        try {
            FXGL.getAssetLoader().clearCache();
        } catch (Exception ignored) {
        }
        
        try {
            ImageCache.clear();
        } catch (Exception ignored) {
        }
        
        try {
            com.arcadeblocks.util.TextureUtils.clearGameplayTextureCache();
        } catch (Exception ignored) {
        }
        
        // Terminating the JavaFX Application Thread
        try {
            javafx.application.Platform.exit();
        } catch (Exception ignored) {
        }
        
        // Allow time for JavaFX to finish before forcing exit
        try {
            Thread.sleep(100); // 100ms to complete JavaFX
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Forcefully terminate the JVM process immediately.
        // Do not wait, as all resources have already been cleared.
        System.exit(0);
    }
    
    /**
     * Return to main menu (with background change - to return from the game)
     */
    public void returnToMainMenu() {
        boolean wasDebugSession = isDebugMode;

        dismissActiveChapterStoryView();

        if (bonusEffectManager != null) {
            bonusEffectManager.forceDisableDarkness();
        } else {
            disableDarknessOverlay();
        }
        
        // Check if we are in pause mode in the settings
        if (inPauseSettings) {
            // Return to pause instead of main menu
            inPauseSettings = false;
            // CRITICAL: Check if PauseView already exists in the scene
            var scene = FXGL.getGameScene();
            var existingPauseView = scene.getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.PauseView)
                .findFirst();
            
            if (!existingPauseView.isPresent()) {
                // PauseView does not exist, create a new one
                clearUINodesSafely();
                FXGL.getGameScene().addUINode(new PauseView(this));
            } else {
                // PauseView already exists, just clear other UI nodes
                clearUINodesSafely();
            }
            return;
        }
        
        // Check if PauseView already exists in the scene before going to the main menu.
        // This prevents a new PauseView from being created if the old one has not yet been deleted.
        var scene = FXGL.getGameScene();
        var existingPauseView = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.PauseView)
            .findFirst();
        
        if (existingPauseView.isPresent()) {
            // PauseView still exists, delete it before going to the main menu
            ((com.arcadeblocks.ui.PauseView) existingPauseView.get()).cleanup();
            removeUINodeSafely(existingPauseView.get());
        }
        
        // Reset the pause flag before going to the main menu
        isPaused = false;

        if (saveManager != null) {
            if (!wasDebugSession && paddleComponent != null) {
                saveCurrentGameSnapshot();
                saveManager.autoSaveToActiveSlot();
                saveManager.awaitPendingWrites();
            } else {
                saveManager.clearGameSnapshot();
            }
        }

        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
        
        // Reset the level completion and transition flags
        isLevelCompleted = false;
        isTransitioning = false;
        
        if (scoreManager != null) {
            if (wasDebugSession) {
                scoreManager.setPersistenceEnabled(false);
            }
            scoreManager.stopLevelTimer();
            if (!wasDebugSession) {
                scoreManager.flushPendingOperations();
            }
        }
        
        paddleComponent = null;
        // Clear LivesManager before resetting
        if (livesManager != null) {
            livesManager.cleanup();
        }
        livesManager = null;
        bonusEffectManager = null;
        bonusTimerView = null;
        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
        
        // Cleaning all entities (safe)
        var entities = new java.util.ArrayList<>(FXGL.getGameWorld().getEntities());
        entities.forEach(Entity::removeFromWorld);
        
        // CRITICAL: Free up background images before rebuilding the menu
        releaseLevelBackground();
        
        // Keep the current background of the main menu (do not change it when returning from the game)
        String currentBackground = getCurrentMainMenuBackground();
        setCurrentMainMenuBackground(currentBackground);
        
        // Clear the letterbox overlay before displaying the main menu
        clearLetterboxOverlay();
        
        // Clearing the UI and displaying the main menu (the background should NOT change when returning from the game)
        clearUINodesSafely();
        MainMenuView mainMenuView = new MainMenuView(this, false, currentBackground);
        FXGL.getGameScene().addUINode(mainMenuView);
        
        // КРИТИЧНО: Запрашиваем фокус для MainMenuView
        javafx.application.Platform.runLater(() -> {
            mainMenuView.restoreFocus();
        });
        
        // Set the system cursor
        setSystemCursor();
        
        if (wasDebugSession) {
            resetDebugMode();
        }
        
        // Play music from the main menu or restore the previous one
        if (audioManager != null) {
            // First, we try to restore the previous music (if it was saved)
            boolean restored = audioManager.restorePreviousMusic();
            
            // If the previous music was not restored, play random music from the main menu
            if (!restored || !audioManager.isMusicPlaying()) {
                // Stop any currently playing music (including background music)
                audioManager.stopMusic();
                // System.out.println("Music stopped when returning to the main menu");
                
                // Определяем состояние прогресса игры для выбора правильной музыки
                com.arcadeblocks.config.AudioConfig.GameProgressState progressState = 
                    com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
                
                if (saveManager != null) {
                    int maxLevel = 0;
                    boolean gameCompleted = false;
                    
                    // Проверяем все слоты сохранения
                    for (int slot = 1; slot <= 4; slot++) {
                        com.arcadeblocks.utils.SaveManager.SaveInfo saveInfo = saveManager.getSaveInfo(slot);
                        if (saveInfo != null) {
                            if (saveInfo.level > maxLevel) {
                                maxLevel = saveInfo.level;
                            }
                            if (saveManager.isGameCompletedInSlot(slot)) {
                                gameCompleted = true;
                            }
                        }
                    }
                    
                    // Определяем состояние прогресса
                    if (gameCompleted) {
                        progressState = com.arcadeblocks.config.AudioConfig.GameProgressState.COMPLETED;
                    } else if (maxLevel >= 101) {
                        progressState = com.arcadeblocks.config.AudioConfig.GameProgressState.AFTER_LEVEL_100;
                    }
                }
                
                String randomMainMenuMusic = com.arcadeblocks.config.AudioConfig.getRandomMainMenuMusic(progressState);
                audioManager.playMusic(randomMainMenuMusic, true);
                // System.out.println("Playing random music from the main menu: " + randomMainMenuMusic + " (back to menu));
            } else {
                // System.out.println("Previous main menu music restored);
            }
        }
        
        // Display database statistics at startup
        if (saveManager != null) {
            saveManager.printDatabaseStats();
        }
    }
    
    // Custom method for processing output (called from dialogs)
    public void handleGameExit() {
        // System.out.println("handleGameExit called!"); // Debugging

        if (isLoading || isLevelIntroActive || isStoryOverlayActive || isCountdownActive || isLevelCompleted || isTransitioning) {
            return;
        }
        
        // CRITICAL: Set completion flag to prevent new operations
        shutdownTriggered.set(true);
        
        // CRITICAL: Stop all active operations
        try {
            if (scoreManager != null) {
                scoreManager.stopLevelTimer();
                scoreManager.flushPendingOperations();
            }
        } catch (Exception ignored) {
        }
        
        // Close the connection to the database
        if (saveManager != null) {
            try {
                saveCurrentGameSnapshot();
            } catch (Exception ignored) {
            }
            try {
                saveManager.close();
            } catch (Exception ignored) {
            }
            saveManager = null;
        }
        
        // First, we clear all resources
        performShutdownIfNeeded();
        
        // Clear all UI components
        cleanupAllUINodes();
        
        // Complete all threads before exiting
        shutdownAllExecutors();
        
        // Stop all active animations and timers
        cleanupAllTimelines();
        
        // Remove all event listeners
        cleanupAllEventListeners();
        
        // Clear all active video resources
        cleanupActiveVideoResources();
        
        // Clear all caches
        try {
            FXGL.getAssetLoader().clearCache();
        } catch (Exception ignored) {
        }
        
        try {
            ImageCache.clear();
        } catch (Exception ignored) {
        }
        
        try {
            com.arcadeblocks.util.TextureUtils.clearGameplayTextureCache();
        } catch (Exception ignored) {
        }
        
        // Terminate the JavaFX Application Thread
        try {
            javafx.application.Platform.exit();
        } catch (Exception ignored) {
        }
        
        // Allow time for JavaFX to finish before forcing exit
        try {
            Thread.sleep(100); // 100ms to complete JavaFX
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Forcefully terminate the JVM process immediately.
        // Do not wait, as all resources have already been cleared.
        System.exit(0);
    }
    
    private Runnable beginLevelReloadGuard() {
        suppressLevelCompletionChecks = true;
        AtomicBoolean released = new AtomicBoolean(false);
        return () -> {
            if (released.compareAndSet(false, true)) {
                suppressLevelCompletionChecks = false;
                boolean noBricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).isEmpty()
                    && Brick.getActiveBrickCount() == 0;
                if (noBricks && !isTransitioning && !isLevelCompleted) {
                    FXGL.runOnce(this::checkLevelCompletion, javafx.util.Duration.millis(100));
                }
            }
        };
    }

    /**
     * Level completion check
     */
    public void checkLevelCompletion() {
        if (isLevelCompleted || isTransitioning || isGameOver) {
            return;
        }

        var remainingBricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK);
        if (remainingBricks.isEmpty() && Brick.getActiveBrickCount() == 0) {
            int currentLevel = FXGL.geti("level");
            int currentScore = scoreManager != null ? scoreManager.getCurrentScore() : 0;
            int currentLives = FXGL.geti("lives");
            double pauseSeconds = getLevelCompletionPauseSeconds(currentLevel);

            // We check again after a short delay to eliminate window collapse artifacts.
            FXGL.runOnce(() -> confirmLevelCompletion(currentLevel, currentScore, currentLives, pauseSeconds),
                javafx.util.Duration.millis(120));
        } else {
            // System.out.println("There are still bricks left in ArcadeBlocksApp: " + remainingBricks.size());
        }
    }

    private void confirmLevelCompletion(int currentLevel, int currentScore, int currentLives, double pauseSeconds) {
        if (suppressLevelCompletionChecks || isLevelCompleted || isTransitioning) {
            return;
        }

        if (!FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).isEmpty()
            || Brick.getActiveBrickCount() > 0) {
            return;
        }

        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(currentLevel);

        if (saveManager != null) {
            saveManager.clearGameSnapshot();
        }

        isLevelCompleted = true;
        isTransitioning = true;

        if (scoreManager != null) {
            scoreManager.stopLevelTimer();
        }

        blockPaddleControl();
        blockMouseClicks();
        uninstallMousePaddleControlHandlers();
        stopAllBonusesExceptStickyPaddle();
        stopAllBalls();
        playPaddleBallFadeOut(null);

        boolean finalVictory = isFinalVictoryLevel(currentLevel);
        
        // Проверяем, есть ли видео завершения
        boolean hasCompletionVideo = metadata != null
            && metadata.getCompletionVideoPath() != null
            && !metadata.getCompletionVideoPath().isBlank();
        
        // Проверяем, нужно ли показывать финальное видео с поэмой (уровень 100)
        boolean shouldPlayFinalVictorySequence = finalVictory
            && hasCompletionVideo
            && metadata.shouldShowPoemAfterVictory();
        
        // Проверяем, нужно ли показывать финальное видео без поэмы (уровень 116)
        boolean shouldPlayFinalVictoryVideo = finalVictory
            && hasCompletionVideo
            && !metadata.shouldShowPoemAfterVictory();
        
        // Обычное видео босса (не финальная победа)
        boolean shouldPlayCompletionVideo = !finalVictory && hasCompletionVideo;

        String completionSound = getCurrentLevelCompleteSound();

        if (audioManager != null) {
            audioManager.stopMusic();
            if (!shouldPlayCompletionVideo && !shouldPlayFinalVictorySequence && !shouldPlayFinalVictoryVideo && completionSound != null) {
                audioManager.playSFX(completionSound);
            }
        }

        if (!finalVictory && !shouldPlayCompletionVideo) {
            showLevelCompletionMessage(currentLevel);
        }

        if (shouldPlayFinalVictorySequence) {
            // Уровень с поэмой (например, уровень 100) - показываем финальное видео, затем поэму
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    playFinalVictorySequence(metadata);
                }
            });
        } else if (shouldPlayFinalVictoryVideo) {
            // Финальная победа с видео но без поэмы (например, уровень 116) - показываем видео, затем титры
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    playBossCompletionVideo(metadata, completionSound,
                        remover -> proceedToNextLevel(currentLevel, currentScore, currentLives, remover));
                }
            });
        } else if (finalVictory) {
            // Финальная победа без видео и поэмы - просто переходим к следующему экрану
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    proceedToNextLevel(currentLevel, currentScore, currentLives);
                }
            });
        } else if (shouldPlayCompletionVideo) {
            // Обычное видео босса
            javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    playBossCompletionVideo(metadata, completionSound,
                        remover -> proceedToNextLevel(currentLevel, currentScore, currentLives, remover));
                }
            });
        } else {
            // Обычный уровень без видео
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(pauseSeconds));
            pause.setOnFinished(event -> javafx.application.Platform.runLater(() -> {
                if (!proceedToNextLevelCalled) {
                    proceedToNextLevel(currentLevel, currentScore, currentLives);
                }
            }));
            pause.play();
        }
    }

    public void onBrickCountChanged(int remaining) {
        if (levelPassBonusSpawned || isLevelCompleted || isTransitioning) {
            return;
        }

        int currentLevel = FXGL.geti("level");
        if (isSpecialLevel(currentLevel)) {
            return;
        }

        if (!BonusConfig.isBonusEnabled("level_pass")) {
            return;
        }

        int actualRemaining = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).size();
        int trackedRemaining = Brick.getActiveBrickCount();

        if (trackedRemaining <= 0) {
            FXGL.runOnce(this::checkLevelCompletion, Duration.millis(60));
            return;
        }

        if (actualRemaining != 3 || trackedRemaining != 3) {
            return;
        }

        spawnLevelPassBonus();
    }

    public void onBossDefeatSequenceStarted(Boss boss) {
        if (isTransitioning) {
            return;
        }
        isLevelCompleted = true;
        isTransitioning = true;
        levelCompletedMessageShown = false;
        proceedToNextLevelCalled = false;
        
        if (scoreManager != null) {
            scoreManager.stopLevelTimer();
        }

        if (saveManager != null) {
            saveManager.clearGameSnapshot();
        }

        blockPaddleControl();
        stopAllBonusesExceptStickyPaddle();
        stopAllBalls();

        if (audioManager != null) {
            audioManager.stopMusic();
        }
    }

    private boolean isSpecialLevel(int levelNumber) {
        return com.arcadeblocks.config.LevelConfig.isBossLevel(levelNumber);
    }

    private boolean isFinalVictoryLevel(int levelNumber) {
        // Проверяем метаданные уровня на флаг showPoemAfterVictory
        // Это позволяет уровню 100 быть финальной победой с поэмой,
        // даже если это не последний уровень в игре
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(levelNumber);
        if (metadata != null && metadata.shouldShowPoemAfterVictory()) {
            return true;
        }
        // Также считаем финальной победой последний уровень игры
        return levelNumber >= GameConfig.TOTAL_LEVELS;
    }

    private void spawnLevelPassBonus() {
        levelPassBonusSpawned = true;

        if (levelPassBonusEntity != null && levelPassBonusEntity.isActive()) {
            levelPassBonusEntity.removeFromWorld();
        }

        double bonusWidth = 81;
        double spawnX = (GameConfig.GAME_WIDTH - bonusWidth) / 2.0;
        double spawnY = GameConfig.TOP_UI_HEIGHT + 20;

        SpawnData data = new SpawnData(spawnX, spawnY);
        data.put("bonusType", BonusType.LEVEL_PASS);

        levelPassBonusEntity = FXGL.spawn("bonus", data);
        if (levelPassBonusEntity != null) {
            Bonus bonusComponent = new Bonus();
            bonusComponent.setBonusType(BonusType.LEVEL_PASS);
            bonusComponent.setFallSpeed(72.0); // ~1.2px per frame @60fps normalized
            levelPassBonusEntity.addComponent(bonusComponent);
        }
    }

    public void completeLevelViaBonus() {
        if (isLevelCompleted || isTransitioning) {
            return;
        }

        levelPassBonusSpawned = true;
        
        // КРИТИЧНО: НЕ удаляем levelPassBonusEntity здесь - пусть анимация collect() завершится естественно
        // Это предотвращает утечку памяти от незавершенной анимации бонуса
        levelPassBonusEntity = null; // Обнуляем ссылку, но entity удалится через collect() анимацию

        // КРИТИЧНО: Сначала запускаем плавное исчезновение ракетки и мяча
        // как при обычном завершении уровня
        playPaddleBallFadeOut(() -> {
            // КРИТИЧНО: Удаляем кирпичи ПОСЛЕ завершения fade анимации
            // Это синхронизирует визуальное завершение уровня
            var bricks = new java.util.ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK));
            for (Entity brick : bricks) {
                brick.removeFromWorld();
            }
            Brick.resetBrickCounter();

            // Проверяем завершение уровня после удаления кирпичей
            FXGL.runOnce(this::checkLevelCompletion, Duration.millis(50));
        });
    }

    public void onBossDefeated(Boss boss) {
        if (proceedToNextLevelCalled) {
            return;
        }

        int currentLevel = FXGL.geti("level");
        int currentScore = scoreManager != null ? scoreManager.getCurrentScore() : 0;
        int currentLives = FXGL.geti("lives");

        if (scoreManager != null) {
            scoreManager.stopLevelTimer();
            scoreManager.hideBossHealth();
        }

        boolean finalVictory = isFinalVictoryLevel(currentLevel);
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(currentLevel);
        boolean shouldPlayCompletionVideo = !finalVictory
            && metadata != null
            && metadata.getCompletionVideoPath() != null
            && !metadata.getCompletionVideoPath().isBlank();
        boolean shouldShowCompletionMessage = !finalVictory && !shouldPlayCompletionVideo;

        String completionSound = getCurrentLevelCompleteSound();
        if (audioManager != null) {
            audioManager.stopMusic();
            if (!shouldPlayCompletionVideo && completionSound != null) {
                audioManager.playSFX(completionSound);
            }
        }

        Runnable continueFlow = () -> {
            if (shouldShowCompletionMessage) {
                showLevelCompletionMessage(currentLevel);
            }
            proceedToNextLevel(currentLevel, currentScore, currentLives);
        };

        if (shouldPlayCompletionVideo) {
            playBossCompletionVideo(metadata, completionSound,
                remover -> proceedToNextLevel(currentLevel, currentScore, currentLives, remover));
        } else {
            continueFlow.run();
        }
    }
    
    private void handleBossProjectileHit(Projectile projectileComponent, Entity projectileEntity) {
        projectileEntity.removeFromWorld();

        if (livesManager != null) {
            livesManager.loseLife();
        } else {
            int remainingLives = Math.max(0, FXGL.geti("lives") - 1);
            FXGL.set("lives", remainingLives);
        }

        var bosses = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS);
        for (Entity bossEntity : bosses) {
            Boss boss = bossEntity.getComponentOptional(Boss.class).orElse(null);
            if (boss != null) {
                boss.restoreFullHealth();
            }
        }
    }
    
    /**
     * Public method for safely freeing the background layer.
     * Used by GameOverView and other UI components to free VRAM
     * before switching scenes.
     */
    public void releaseLevelBackground() {
        javafx.application.Platform.runLater(() -> {
            clearLevelBackground();
        });
    }
    
    /**
     * Stop all balls without removing them
     */
    private void stopAllBalls() {
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.pauseForCountdown();
            }
        }
        // System.out.println("Balls stopped at the end of the level: " + balls.size() + " balls");
    }
    
    private void resumeAllBalls() {
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.resumeAfterCountdown();
            }
        }
    }

    public boolean isLaunchLocked() {
        return launchLocked;
    }

    private long lockBallLaunchInternal() {
        launchLockToken++;
        launchLocked = true;
        return launchLockToken;
    }

    private void unlockBallLaunchInternal(long token) {
        if (launchLockToken == token) {
            launchLocked = false;
        }
    }

    private void forceUnlockBallLaunch() {
        launchLockToken++;
        launchLocked = false;
    }

    private List<Node> collectEntityViewNodes(Entity entity) {
        List<Node> nodes = new ArrayList<>();
        if (entity == null || entity.getViewComponent() == null) {
            return nodes;
        }
        entity.getViewComponent().getChildren().forEach(child -> {
            if (child != null) {
                nodes.add(child);
            }
        });
        return nodes;
    }

    private void resetFadeState() {
        // КРИТИЧНО: Останавливаем все активные FadeTransition перед сбросом состояния
        synchronized (activeFadeTransitions) {
            for (FadeTransition fade : activeFadeTransitions) {
                try {
                    if (fade != null) {
                        fade.stop();
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки остановки
                }
            }
            activeFadeTransitions.clear();
        }
        
        fadeOutActive = false;
        fadeOutCompleted = false;
        fadeOutCallbacks.clear();
    }

    private void playPaddleBallFadeOut(Runnable onFinished) {
        if (fadeOutCompleted) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        if (onFinished != null) {
            fadeOutCallbacks.add(onFinished);
        }
        if (fadeOutActive) {
            return;
        }
        fadeOutActive = true;

        javafx.application.Platform.runLater(() -> {
            List<Node> nodes = new ArrayList<>();
            Entity paddle = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE).stream().findFirst().orElse(null);
            nodes.addAll(collectEntityViewNodes(paddle));
            FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)
                .forEach(ball -> nodes.addAll(collectEntityViewNodes(ball)));

            if (nodes.isEmpty()) {
                fadeOutActive = false;
                fadeOutCompleted = true;
                runFadeOutCallbacks();
                return;
            }

            AtomicInteger remaining = new AtomicInteger(nodes.size());
            for (Node node : nodes) {
                FadeTransition fade = new FadeTransition(LEVEL_FADE_DURATION, node);
                fade.setToValue(0.0);
                
                // КРИТИЧНО: Добавляем анимацию в список для отслеживания
                synchronized (activeFadeTransitions) {
                    activeFadeTransitions.add(fade);
                }
                
                fade.setOnFinished(evt -> {
                    // КРИТИЧНО: Удаляем анимацию из списка после завершения
                    synchronized (activeFadeTransitions) {
                        activeFadeTransitions.remove(fade);
                    }
                    
                    if (remaining.decrementAndGet() == 0) {
                        fadeOutActive = false;
                        fadeOutCompleted = true;
                        runFadeOutCallbacks();
                    }
                });
                fade.play();
            }
        });
    }

    public void fadeOutPaddleAndBalls(boolean skipFade, Runnable onFinished) {
        if (skipFade) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }
        resetFadeState();
        playPaddleBallFadeOut(onFinished);
    }

    private void runFadeOutCallbacks() {
        List<Runnable> callbacks = new ArrayList<>(fadeOutCallbacks);
        fadeOutCallbacks.clear();
        callbacks.forEach(Runnable::run);
    }

    /**
     * Deferred execution after fadeOutBonuses to prevent “Physics world is locked” errors
     * All exit paths from fadeOutBonuses() now use this method, which schedules execution
     * via FXGL.runOnce(..., Duration.ZERO), ensuring that the code runs on the next frame,
     * safely outside of the active physics step.
     */
    private void runAfterBonusesFade(Runnable after) {
        if (after != null) {
            FXGL.runOnce(after, javafx.util.Duration.ZERO);
        }
    }
    
    public void fadeOutBonuses(boolean skipFade, Runnable after) {
        if (skipFade) {
            runAfterBonusesFade(after);
            return;
        }

        List<Entity> bonusEntities = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
        if (bonusEntities.isEmpty()) {
            runAfterBonusesFade(after);
            return;
        }

        List<Node> bonusNodes = new ArrayList<>();
        for (Entity bonus : bonusEntities) {
            bonusNodes.addAll(collectEntityViewNodes(bonus));
        }

        if (bonusNodes.isEmpty()) {
            runAfterBonusesFade(after);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(bonusNodes.size());
        javafx.application.Platform.runLater(() -> {
            for (Node node : bonusNodes) {
                FadeTransition fade = new FadeTransition(LEVEL_FADE_DURATION, node);
                fade.setToValue(0.0);
                fade.setOnFinished(evt -> {
                    if (remaining.decrementAndGet() == 0) {
                        runAfterBonusesFade(after);
                    }
                });
                fade.play();
            }
        });
    }
    
    /**
     * Smooth removal of the ball with fade out animation
     */
    private void fadeOutBall(Entity ball) {
        if (ball == null || !ball.isActive()) {
            return;
        }
        
        // We check whether the removal animation for this ball has already been started.
        if (ball.getProperties().exists("isFadingOut")) {
            Boolean isFadingOut = ball.getBoolean("isFadingOut");
            if (Boolean.TRUE.equals(isFadingOut)) {
                return; // The animation has already started.
            }
        }
        
        ball.setProperty("isFadingOut", true);
        
        List<Node> ballNodes = collectEntityViewNodes(ball);
        if (ballNodes.isEmpty()) {
            // If there are no nodes for animation, delete them immediately.
            ball.removeFromWorld();
            return;
        }
        
        // Stop the ball's physics so that it does not move during animation
        com.almasb.fxgl.physics.PhysicsComponent physics = ball.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
        if (physics != null) {
            physics.setLinearVelocity(0, 0);
            physics.setBodyType(com.almasb.fxgl.physics.box2d.dynamics.BodyType.STATIC);
        }
        
        javafx.application.Platform.runLater(() -> {
            AtomicInteger remaining = new AtomicInteger(ballNodes.size());
            for (Node node : ballNodes) {
                FadeTransition fade = new FadeTransition(Duration.millis(300), node);
                fade.setFromValue(node.getOpacity());
                fade.setToValue(0.0);
                fade.setOnFinished(evt -> {
                    if (remaining.decrementAndGet() == 0) {
                        // Remove the ball from the world after the animation ends
                        if (ball.isActive()) {
                            ball.removeFromWorld();
                        }
                    }
                });
                fade.play();
            }
        });
    }

    private void playPaddleBallFadeIn(boolean skipFade) {
        boolean skipLock = bonusEffectManager != null && bonusEffectManager.isStickyPaddleActive();
        if (skipFade) {
            // КРИТИЧНО: При пропуске анимации сразу разблокируем запуск и клики мыши
            forceUnlockBallLaunch();
            unblockMouseClicks();
            Entity paddle = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE).stream().findFirst().orElse(null);
            collectEntityViewNodes(paddle).forEach(node -> node.setOpacity(1.0));
            FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)
                .forEach(ball -> collectEntityViewNodes(ball).forEach(node -> node.setOpacity(1.0)));
            return;
        }

        Entity paddle = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE).stream().findFirst().orElse(null);
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(collectEntityViewNodes(paddle));
        FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)
            .forEach(ball -> nodes.addAll(collectEntityViewNodes(ball)));

        if (nodes.isEmpty()) {
            // КРИТИЧНО: Всегда разблокируем запуск и клики мыши, даже если нет нод для анимации
            forceUnlockBallLaunch();
            unblockMouseClicks();
            if (paddleComponent != null) {
                paddleComponent.setMovementBlocked(false);
            }
            return;
        }

        long lockToken = skipLock ? -1L : lockBallLaunchInternal();
        if (paddleComponent != null) {
            paddleComponent.setMovementBlocked(true);
        }

        AtomicInteger remaining = new AtomicInteger(nodes.size());
        javafx.application.Platform.runLater(() -> {
            for (Node node : nodes) {
                node.setOpacity(0.0);
                FadeTransition fade = new FadeTransition(LEVEL_FADE_DURATION, node);
                fade.setToValue(1.0);
                fade.setOnFinished(evt -> {
                    if (remaining.decrementAndGet() == 0) {
                        // КРИТИЧНО: Всегда разблокируем движение ракетки после анимации
                        // Не восстанавливаем предыдущее состояние, так как оно может быть заблокированным
                        if (paddleComponent != null) {
                            paddleComponent.setMovementBlocked(false);
                        }
                        // КРИТИЧНО: Добавляем задержку 500мс перед разблокировкой запуска мяча вручную
                        // Это дает игроку время подготовиться после возрождения
                        // КРИТИЧНО: Используем forceUnlockBallLaunch для гарантированной разблокировки
                        // Это предотвращает баг, когда мяч нельзя запустить вручную после возрождения
                        FXGL.runOnce(() -> {
                            forceUnlockBallLaunch();
                            // КРИТИЧНО: Разблокируем клики мыши для возможности запуска мяча
                            // Без этого canProcessLaunchInput() всегда возвращает false
                            unblockMouseClicks();
                        }, Duration.millis(500));
                    }
                });
                fade.play();
            }
        });
    }

    private void enforcePauseFreeze() {
        stopAllBalls();
        FXGL.getGameController().pauseEngine();
        if (scoreManager != null) {
            scoreManager.pauseLevelTimer();
        }
        if (bonusEffectManager != null) {
            bonusEffectManager.pauseAllBonusTimers();
        }
    }
    
    /**
     * Paddle control lock
     */
    private void blockPaddleControl() {
        if (paddleComponent != null) {
            paddleComponent.setMoveLeft(false);
            paddleComponent.setMoveRight(false);
            paddleComponent.setTurboMode(false);
            paddleComponent.setMovementBlocked(true);
            // System.out.println("Paddle control is blocked");
        }
    }
    
    /**
     * Blocking mouse clicks
     */
    public void blockMouseClicks() {
        mouseClicksBlocked = true;
        // System.out.println("Mouse clicks are blocked");
    }
    
    /**
     * Unlocking mouse clicks
     */
    public void unblockMouseClicks() {
        mouseClicksBlocked = false;
        // System.out.println("Mouse clicks are unlocked");
    }
    
    /**
     * All bonuses and effects are stopped
     */
    private void stopAllBonuses() {
        if (bonusEffectManager != null) {
            bonusEffectManager.resetAllEffects();
        }

        fadeOutBonuses(false, () -> {
            var bonuses = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
            for (Entity bonusEntity : bonuses) {
                bonusEntity.removeFromWorld();
            }

            if (!bonuses.isEmpty()) {
                // System.out.println("Deleted " + bonuses.size() + " falling bonuses");
            }

            levelPassBonusEntity = null;
        });
        
        // System.out.println("All bonuses and effects are stopped");
    }
    
    /**
     * All bonuses and effects are stopped, except for racket stickiness.
     */
    private void stopAllBonusesExceptStickyPaddle() {
        if (bonusEffectManager != null) {
            bonusEffectManager.resetAllEffectsExceptStickyPaddle();
        }

        fadeOutBonuses(false, () -> {
            var bonuses = new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS));
            for (Entity bonusEntity : bonuses) {
                bonusEntity.removeFromWorld();
            }

            if (!bonuses.isEmpty()) {
                // System.out.println("Deleted " + bonuses.size() + " falling bonuses");
            }

            // System.out.println("All bonuses and effects are disabled (except for racket stickiness).");

            levelPassBonusEntity = null;
            levelPassBonusSpawned = false;
        });
    }

    public void onAllBonusesCleared() {
        levelPassBonusSpawned = false;
        levelPassBonusEntity = null;
    }
    
    public void setInPauseSettings(boolean inPauseSettings) {
        this.inPauseSettings = inPauseSettings;
    }
    
    /**
     * Check whether we are in pause settings
     */
    public boolean isInPauseSettings() {
        return inPauseSettings;
    }
    
    /**
     * Сохранить snapshot перед входом в настройки паузы
     * Это предотвращает утечки памяти при изменении настроек
     */
    public void saveSnapshotBeforePauseSettings() {
        if (saveManager != null && !inPauseSettings) {
            saveCurrentGameSnapshot();
        }
    }
    
    /**
     * Сохранить все настройки при выходе из настроек паузы
     * Вызывается один раз вместо сохранения при каждом изменении
     */
    private void saveAllPauseSettings() {
        // Метод вызывается из returnToPauseFromSettings
        // Настройки уже применены через listeners, просто ждем завершения записи
        if (saveManager != null) {
            saveManager.awaitPendingWrites();
        }
    }
    
    /**
     * Return to pause from settings
     */
    public void returnToPauseFromSettings() {
        var scene = FXGL.getGameScene();
        
        // КРИТИЧНО: Сначала находим SettingsView, сохраняем настройки
        // ДО того, как сбросим флаг inPauseSettings и удалим узел
        SettingsView settingsView = null;
        for (Node node : new ArrayList<Node>(scene.getUINodes())) {
            if (node instanceof SettingsView) {
                settingsView = (SettingsView) node;
                break;
            }
        }
        
        // Сохраняем настройки синхронно, чтобы гарантировать их сохранение
        if (settingsView != null) {
            settingsView.saveAllPauseSettings();
        }
        
        // КРИТИЧНО: Сбрасываем флаг inPauseSettings
        inPauseSettings = false;
        
        // КРИТИЧНО: Удаляем ВСЕ экземпляры SettingsView и диалоги из сцены
        var nodesToRemove = new ArrayList<Node>();
        for (Node node : scene.getUINodes()) {
            if (node instanceof SettingsView
                || node instanceof com.arcadeblocks.ui.ResetSettingsDialog
                || node instanceof com.arcadeblocks.ui.ControlsResetDialog
                || node instanceof com.arcadeblocks.ui.UnsavedChangesDialog) {
                nodesToRemove.add(node);
            }
        }
        
        // Удаляем все найденные узлы БЕЗ вызова cleanup (cleanup будет вызван асинхронно)
        for (Node node : nodesToRemove) {
            try {
                scene.removeUINode(node);
            } catch (Exception e) {
                System.err.println("Error removing UI node: " + e.getMessage());
            }
        }
        
        // КРИТИЧНО: Показываем экран паузы СРАЗУ для мгновенного отклика
        showPauseScreen();
        
        // КРИТИЧНО: Вызываем cleanup() с БОЛЬШОЙ задержкой, чтобы дать время на рендеринг PauseView
        // Это позволяет пользователю увидеть экран паузы ДО того, как начнется GC
        final SettingsView viewToCleanup = settingsView;
        new Thread(() -> {
            try {
                // Задержка 500мс - даем время на рендеринг PauseView
                Thread.sleep(500);
                
                // Вызываем cleanup в JavaFX потоке
                Platform.runLater(() -> {
                    if (viewToCleanup != null) {
                        viewToCleanup.cleanup();
                    }
                    // Очищаем другие узлы
                    for (Node node : nodesToRemove) {
                        if (node instanceof SettingsView && node != viewToCleanup) {
                            ((SettingsView) node).cleanup();
                        }
                    }
                    nodesToRemove.clear();
                });
            } catch (InterruptedException ignored) {}
        }, "Delayed-Cleanup-Thread").start();
        
        // КРИТИЧНО: НЕ вызываем System.gc() здесь, так как это может вызывать лаги
        // Сборка мусора произойдет автоматически, когда это необходимо
        
        // КРИТИЧНО: Тяжелые операции выполняем асинхронно в фоне
        Platform.runLater(() -> {
            // Запускаем очистку памяти асинхронно
            if (saveManager != null) {
                saveManager.runAfterPendingWrites(() -> {
                    if (saveManager != null) {
                        saveManager.forceMemoryCleanup();
                    }
                });
            }
            
            // Очистка неактивных UI нодов
            try {
                clearInactiveUINodes();
            } catch (Exception e) {
                System.err.println("Error clearing inactive UI nodes: " + e.getMessage());
            }
        });
    }
    
    /**
     * Return to pause from help
     */
    public void returnToPauseFromHelp() {
        // Return to pause from help
        var scene = FXGL.getGameScene();
        var uiNodes = new ArrayList<Node>(scene.getUINodes());
        for (Node node : uiNodes) {
            if (node instanceof com.arcadeblocks.ui.HelpView
                || node instanceof com.arcadeblocks.ui.PauseView) {
                removeUINodeSafely(node);
            }
        }

        showPauseScreen();
    }
    
    /**
     * Return to the main menu without changing the background (to return from settings/help/credits)
     */
    public void returnToMainMenuFromSettings() {
        // КРИТИЧНО: Сбрасываем флаг inPauseSettings
        inPauseSettings = false;

        // Проверяем, нужно ли установить флаг завершения игры
        if (saveManager != null) {
            int currentLevel = FXGL.geti("level");
            if (currentLevel >= 116) {
                // Игрок прошел уровень 116 - устанавливаем флаг завершения
                saveManager.setGameCompletedForActiveSlot();
            }
        }

        // Запускаем музыку главного меню с учетом завершения игры
        startMainMenuMusic();

        boolean mainMenuVisible = FXGL.getGameScene().getUINodes().stream()
            .anyMatch(node -> node instanceof com.arcadeblocks.ui.MainMenuView);

        // Проверяем, завершена ли игра хотя бы в одном слоте
        boolean gameCompleted = false;
        if (saveManager != null) {
            for (int slot = 1; slot <= 4; slot++) {
                if (saveManager.isGameCompletedInSlot(slot)) {
                    gameCompleted = true;
                    break;
                }
            }
        }

        if (mainMenuVisible && !gameCompleted) {
            // MainMenuView уже на экране и игра не завершена - просто удаляем overlay
            showMainMenu();
        } else {
            // Игра завершена или MainMenuView отсутствует - пересоздаем меню с новым фоном
            clearUINodesSafely();
            MainMenuView mainMenuView = new MainMenuView(this, true); // true = изменить фон
            FXGL.getGameScene().addUINode(mainMenuView);
            
            javafx.application.Platform.runLater(() -> {
                mainMenuView.requestFocus();
            });
            
            uninstallMousePaddleControlHandlers();
            mouseClicksBlocked = false;
            setSystemCursor();
        }
    }
    
    /**
     * Reset pause state (used when exiting pause to main menu)
     */
    public void resetPauseState() {
        isPaused = false;
        inPauseSettings = false;
    }
    
    /**
     * Proceed to the next level
     */
    private void proceedToNextLevel(int currentLevel, int currentScore, int currentLives) {
        proceedToNextLevel(currentLevel, currentScore, currentLives, null);
    }

    private void proceedToNextLevel(int currentLevel, int currentScore, int currentLives, Runnable overlayRemover) {
        if (proceedToNextLevelCalled) {
            return;
        }
        proceedToNextLevelCalled = true;

        fadeOutPaddleAndBalls(false,
            () -> continueToNextLevel(currentLevel, currentScore, currentLives, overlayRemover));
    }

    private void continueToNextLevel(int currentLevel, int currentScore, int currentLives, Runnable overlayRemover) {
        isLevelCompleted = false;
        isTransitioning = false;
        levelCompletedMessageShown = false;

        if (saveManager != null && !isDebugMode) {
            saveManager.clearGameSnapshot();
        }

        try {
            if (scoreManager != null) {
                scoreManager.flushPendingOperations();
            }
            if (saveManager != null && !isDebugMode) {
                saveManager.setScore(currentScore);
                saveManager.setLives(currentLives);
                saveManager.setLevelCompleted(currentLevel, true);
                saveManager.setTotalLevelsCompleted(saveManager.getTotalLevelsCompleted() + 1);
            }

            int nextLevel = pendingLevelWarpTarget != null ? pendingLevelWarpTarget : currentLevel + 1;
            pendingLevelWarpTarget = null;

            if (nextLevel > GameConfig.TOTAL_LEVELS) {
                if (overlayRemover != null) {
                    javafx.application.Platform.runLater(overlayRemover);
                }
                showVictoryScreen(currentLevel);
            } else {
                if (saveManager != null && !isDebugMode) {
                    saveManager.setCurrentLevel(nextLevel);
                    saveManager.autoSaveToActiveSlot();
                    saveManager.awaitPendingWrites();
                }

                if (isDebugMode) {
                    startDebugLevel(nextLevel, currentLives, currentScore, overlayRemover);
                } else {
                    startLevel(nextLevel, false, overlayRemover);
                }
            }
        } catch (Exception e) {
            // КРИТИЧНО: При ошибке обязательно удаляем оверлей чтобы не было утечки памяти
            if (overlayRemover != null) {
                javafx.application.Platform.runLater(overlayRemover);
            }
            System.err.println("Error when moving to the next level: " + e.getMessage());
            e.printStackTrace();
            returnToMainMenu();
        }
    }

    // public void destroyAllBricks() {
    //     var bricks = new java.util.ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK));
    //     if (bricks.isEmpty()) {
    //         return;
    //     }

    //     for (Entity brickEntity : bricks) {
    //         Brick brickComponent = brickEntity.getComponent(Brick.class);
    //         if (brickComponent != null) {
    //             brickComponent.destroySilently();
    //         } else {
    //             brickEntity.removeFromWorld();
    //         }
    //     }

    //     FXGL.runOnce(this::checkLevelCompletion, Duration.millis(120));
    // }
    
    /**
     * Show victory screen
     */
    private void showVictoryScreen(int completedLevel) {
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata =
            com.arcadeblocks.config.LevelConfig.getMetadata(completedLevel);
        if (metadata != null && metadata.getCompletionVideoPath() != null && metadata.shouldShowPoemAfterVictory()) {
            playFinalVictorySequence(metadata);
            return;
        }
        // Передаем true, так как это титры после завершения игры (уровень 116)
        showCredits(true);
    }

    private void playFinalVictorySequence(com.arcadeblocks.config.LevelConfig.LevelMetadata metadata) {
        unblockMouseClicks();
        setSystemCursor();

        isVictorySequenceActive = true;
        
        // Clear all old video resources BEFORE creating the new final victory video.
        // This prevents memory leaks when playing the final victory video.
        cleanupActiveVideoResources();
        
        // Capture the token of the current video session to verify the relevance of the callback
        final long currentVideoToken = videoSessionToken;

        if (audioManager != null) {
            audioManager.cancelPendingMusic();
            audioManager.stopMusic();
            audioManager.playSFX(getCurrentLevelCompleteSound());
        }

        double duration = metadata.getCompletionVideoDurationSeconds() != null
            ? metadata.getCompletionVideoDurationSeconds()
            : 30.0;

        playVideoOverlay(metadata.getCompletionVideoPath(), duration, remover -> {
            // CRITICAL: Performing cleanup in the UI thread
            javafx.application.Platform.runLater(() -> {
                // Call the remover, which will clear the overlay and MediaPlayer
                if (remover != null) {
                    try {
                        remover.run();
                    } catch (Exception e) {
                        System.err.println("Error when removing the overlay from the final video: " + e.getMessage());
                    }
                }
                
                isVictorySequenceActive = false;
                
                // Checking the video session token before final cleanup
                if (currentVideoToken == videoSessionToken) {
                    // Final cleanup of all active video resources after completion of final victory video
                    cleanupActiveVideoResources();
                } else {
                    //System.out.println("Final cleaning of the final video skipped - token expired (race condition prevented)");
                }
                
                // Display the next screen after complete clearing
                if (metadata.shouldShowPoemAfterVictory()) {
                    showPoemScreen();
                } else {
                    showCredits();
                }
            });
        });
    }

    private void playBossCompletionVideo(
        com.arcadeblocks.config.LevelConfig.LevelMetadata metadata,
        String completionSound,
        Consumer<Runnable> onComplete
    ) {
        boolean hasVideo = metadata != null
            && metadata.getCompletionVideoPath() != null
            && !metadata.getCompletionVideoPath().isBlank();

        if (!hasVideo) {
            if (audioManager != null && completionSound != null && !completionSound.isBlank()) {
                audioManager.playSFX(completionSound);
            }
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }

        isVictorySequenceActive = true;
        
        // CRITICAL: Clear all old video resources BEFORE creating a new boss completion video.
        // This prevents memory leaks when playing boss videos.
        cleanupActiveVideoResources();
        
        // CRITICAL: Capture the token of the current video session to verify the relevance of the callback.
        // If the token has changed when finish is triggered, it means that a new video has been created,
        // and the old callback should not perform the final cleanup.
        final long currentVideoToken = videoSessionToken;

        final boolean[] audioFinished = {completionSound == null || completionSound.isBlank()};
        final boolean[] videoFinished = {false};
        final javafx.animation.PauseTransition[] audioGuardRef = {null};
        final AtomicReference<Runnable> overlayRemoverRef = new AtomicReference<>();
        final AtomicBoolean completionHandled = new AtomicBoolean(false);

        Runnable finish = () -> {
            if (completionHandled.compareAndSet(false, true)) {
                // CRITICAL: First, clear PauseTransition if it is still active.
                if (audioGuardRef[0] != null) {
                    try {
                        synchronized (activeVideoPauseTransitions) {
                            activeVideoPauseTransitions.remove(audioGuardRef[0]);
                        }
                        audioGuardRef[0].stop();
                        audioGuardRef[0] = null;
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
                
                isVictorySequenceActive = false;
                // КРИТИЧНО: НЕ удаляем overlay здесь - передаем его в onComplete
                // Это предотвращает мерцание игрового мира между видео и сюжетным окном
                Runnable remover = overlayRemoverRef.getAndSet(null);
                
                // Performing cleanup in the UI thread
                javafx.application.Platform.runLater(() -> {
                    // КРИТИЧНО: НЕ вызываем remover.run() здесь!
                    // Оверлей должен остаться как черный фон под сюжетным окном
                    // Он будет удален только когда сюжетное окно закроется
                    
                    // Check the video session token before final cleanup
                    // If the token has changed, it means a new video has been created and the old callback should not clean up the new video
                    if (currentVideoToken == videoSessionToken) {
                        // КРИТИЧНО: НЕ вызываем cleanupActiveVideoResources() здесь
                        // Это удалит видео оверлей и покажет игровой мир
                        // Очистка произойдет вместе с удалением оверлея позже
                    } else {
                        // System.out.println("Final boss video cleanup skipped - token expired (race condition prevented)");
                    }
                    
                    // КРИТИЧНО: Передаем remover в onComplete вместо null
                    // onComplete передаст его в proceedToNextLevel → startLevel → showChapterStory
                    // Где оверлей будет удален после показа сюжетного окна
                    if (onComplete != null) {
                        try {
                            onComplete.accept(remover); // Передаем remover для бесшовного перехода
                        } catch (Exception e) {
                            System.err.println("Error in onComplete: " + e.getMessage());
                            e.printStackTrace();
                            // КРИТИЧНО: При ошибке удаляем оверлей чтобы не было утечки
                            if (remover != null) {
                                try {
                                    remover.run();
                                } catch (Exception ex) {
                                    System.err.println("Error removing overlay after onComplete error: " + ex.getMessage());
                                }
                            }
                        }
                    } else {
                        // КРИТИЧНО: Если нет onComplete, удаляем оверлей чтобы не было утечки
                        if (remover != null) {
                            try {
                                remover.run();
                            } catch (Exception e) {
                                System.err.println("Error removing overlay when no onComplete: " + e.getMessage());
                            }
                        }
                    }
                });
            }
        };

        Runnable tryFinish = () -> {
            if (audioFinished[0] && videoFinished[0]) {
                finish.run();
            }
        };

        double videoDuration = metadata.getCompletionVideoDurationSeconds() != null
            ? metadata.getCompletionVideoDurationSeconds()
            : 8.0;
        double soundDuration = (completionSound != null && !completionSound.isBlank())
            ? estimateSoundDurationSeconds(completionSound)
            : 0.0;
        double guardDuration = Math.max(videoDuration, soundDuration);

        if (audioManager != null && completionSound != null && !completionSound.isBlank()) {
            audioManager.playSFX(completionSound, () -> javafx.application.Platform.runLater(() -> {
                if (audioGuardRef[0] != null) {
                    // Remove from tracking list before stopping
                    synchronized (activeVideoPauseTransitions) {
                        activeVideoPauseTransitions.remove(audioGuardRef[0]);
                    }
                    audioGuardRef[0].stop();
                    audioGuardRef[0] = null;
                }
                audioFinished[0] = true;
                tryFinish.run();
            }));

            double guardSeconds = soundDuration > 0.0 ? soundDuration + 0.2 : guardDuration;
            javafx.animation.PauseTransition audioGuard = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(guardSeconds));
            audioGuard.setOnFinished(evt -> {
                // Remove from tracking list before resetting
                synchronized (activeVideoPauseTransitions) {
                    activeVideoPauseTransitions.remove(audioGuard);
                }
                audioGuardRef[0] = null;
                audioFinished[0] = true;
                tryFinish.run();
            });
            audioGuardRef[0] = audioGuard;
            // Add to the tracking list for cleaning when changing levels
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.add(audioGuard);
            }
            audioGuard.play();
        } else {
            audioFinished[0] = true;
        }

        playVideoOverlay(metadata.getCompletionVideoPath(), guardDuration, remover -> {
            overlayRemoverRef.set(remover);
            videoFinished[0] = true;
            tryFinish.run();
        });
    }

    private void playLevelIntroVideo(int levelNumber, com.arcadeblocks.config.LevelConfig.LevelMetadata metadata, Runnable onComplete) {
        setLevelIntroActive(true);
        
        // КРИТИЧНО: Сбрасываем runtime volume multiplier обратно к 1.0
        // Это необходимо, так как ChapterStoryView мог оставить его на низком значении
        if (audioManager != null) {
            audioManager.resetRuntimeMusicVolumeMultiplier();
        }
        
        // Clear all old video resources BEFORE creating a new intro video.
        // This prevents a race condition where tryFinish from the old video could clear the new video
        // when restarting the level via the debug menu.
        cleanupActiveVideoResources();
        
        // Capture the token of the current video session to check the relevance of the callback.
        // If the token has changed when tryFinish is triggered, it means that a new video has been created,
        // and the old callback should not perform the final cleanup.
        final long currentVideoToken = videoSessionToken;

        final boolean[] audioFinished = {false};
        final boolean[] videoFinished = {false};
        final javafx.animation.PauseTransition[] audioGuardRef = {null};
        final AtomicReference<Runnable> overlayRemoverRef = new AtomicReference<>();

        String loadingSound = getCurrentLevelLoadingSound();
        // Для всех уровней с видео отключаем звук при показе видео, для остальных уровней проверяем null
        boolean soundDisabled = shouldDisableLoadingSoundDuringVideo(levelNumber) || (loadingSound == null);
        if (loadingSound == null || loadingSound.isBlank()) {
            loadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
        }

        double videoDuration = metadata.getIntroVideoDurationSeconds() != null
            ? metadata.getIntroVideoDurationSeconds()
            : 8.0;
        double soundDuration = estimateSoundDurationSeconds(loadingSound);
        double guardDuration = Math.max(videoDuration, soundDuration);

        Runnable tryFinish = () -> {
            if (audioFinished[0] && videoFinished[0]) {
                // First clean up PauseTransition if it's still active
                if (audioGuardRef[0] != null) {
                    try {
                        synchronized (activeVideoPauseTransitions) {
                            activeVideoPauseTransitions.remove(audioGuardRef[0]);
                        }
                        audioGuardRef[0].stop();
                        audioGuardRef[0] = null;
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
                
                // Ensure overlay is removed even on errors
                Runnable remover = overlayRemoverRef.getAndSet(null);
                
                setLevelIntroActive(false);
                
                // Execute cleanup in UI thread
                // remover itself uses Platform.runLater(), so we wrap everything in one runLater
                // to guarantee correct execution order
                javafx.application.Platform.runLater(() -> {
                    // Call remover which will clean up overlay and MediaPlayer
                    if (remover != null) {
                        try {
                            remover.run();
                        } catch (Exception e) {
                            System.err.println("Error removing overlay: " + e.getMessage());
                            // Continue execution even on error
                        }
                    }
                    
                    // Check video session token before final cleanup
                    // If token has changed, it means a new video was created (e.g., during quick
                    // level restart via debug menu), and the old callback should not clean up the new video
                    if (currentVideoToken == videoSessionToken) {
                        // Final cleanup of all active video resources after intro video completes
                        // This ensures that ALL resources (PauseTransition, MediaPlayer, overlay) 
                        // are fully cleaned up and don't remain in memory
                        // remover has already removed overlay from lists, but we do final cleanup in case
                        // something remained (e.g., PauseTransition that wasn't removed)
                        cleanupActiveVideoResources();
                    } else {
                        // System.out.println("Skipped final video cleanup - token is stale (race condition prevented)");
                    }
                    
                    // Call onComplete after full resource cleanup
                    if (onComplete != null) {
                        try {
                            onComplete.run();
                        } catch (Exception e) {
                            System.err.println("Error in onComplete: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        javafx.animation.PauseTransition audioGuard = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(soundDuration + 0.5));
        audioGuard.setOnFinished(evt -> {
            // Remove from tracking list before nulling
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.remove(audioGuard);
            }
            audioGuardRef[0] = null;
            audioFinished[0] = true;
            tryFinish.run();
        });

        // КРИТИЧНО: Всегда останавливаем музыку при показе видео, независимо от звука загрузки
        if (audioManager != null) {
            audioManager.stopMusic();
        }
        
        if (audioManager != null && !soundDisabled) {
            String soundToPlay = loadingSound;
            audioManager.playSFX(soundToPlay, () -> javafx.application.Platform.runLater(() -> {
                if (audioGuardRef[0] != null) {
                    // CRITICAL: Remove from tracking list before stopping
                    synchronized (activeVideoPauseTransitions) {
                        activeVideoPauseTransitions.remove(audioGuardRef[0]);
                    }
                    audioGuardRef[0].stop();
                    audioGuardRef[0] = null;
                }
                audioFinished[0] = true;
                tryFinish.run();
            }));
            audioGuardRef[0] = audioGuard;
            // CRITICAL: Add to tracking list for cleanup on level change
            synchronized (activeVideoPauseTransitions) {
                activeVideoPauseTransitions.add(audioGuard);
            }
            audioGuard.play();
        } else {
            audioFinished[0] = true;
        }

        playVideoOverlay(metadata.getIntroVideoPath(), guardDuration, remover -> {
            overlayRemoverRef.set(remover);
            videoFinished[0] = true;
            tryFinish.run();
        });
    }

    private void playVideoOverlay(String assetPath, double fallbackDurationSeconds, Consumer<Runnable> onFinished) {
        if (assetPath == null || assetPath.isBlank()) {
            if (onFinished != null) {
                onFinished.accept(() -> {});
            }
            return;
        }

        // Remove letterbox borders for video playback
        clearLetterboxOverlay();

        javafx.application.Platform.runLater(() -> {
            try {

            // Use current resolution instead of fixed sizes (like in main menu and Level Intro)
            com.arcadeblocks.config.Resolution currentRes = GameConfig.getCurrentResolution();
            
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: black;");
            
            // Mark as fullScreenOverlay for proper positioning
            overlay.setUserData("fullScreenOverlay");

            // CRITICAL: Create video backend via factory
            com.arcadeblocks.video.VideoPlayerBackend backend;
            try {
                backend = videoBackendFactory.createBackend();
                // System.out.println("Using backend: " + backend.getBackendName() + " for video: " + assetPath);
            } catch (Exception e) {
                System.err.println("Failed to create video backend: " + e.getMessage());
                e.printStackTrace();
                if (onFinished != null) {
                    onFinished.accept(() -> {});
                }
                return;
            }
            
            // Prepare video
            javafx.scene.Node videoNode;
            try {
                videoNode = backend.prepareVideo(assetPath, currentRes.getWidth(), currentRes.getHeight());
            } catch (Exception e) {
                System.err.println("Failed to prepare video with " + backend.getBackendName() + ": " + e.getMessage());
                
                // CRITICAL: Fallback to Stub backend if VLC couldn't load video
                if (backend instanceof com.arcadeblocks.video.VlcjMediaBackend) {
                    System.err.println("Trying fallback to Stub backend...");
                    try {
                        backend.cleanup();
                        backend = new com.arcadeblocks.video.StubVideoBackend();
                        videoNode = backend.prepareVideo(assetPath, currentRes.getWidth(), currentRes.getHeight());
                        // System.out.println("Successfully loaded via Stub backend fallback");
                    } catch (Exception fallbackException) {
                        System.err.println("Stub fallback also failed: " + fallbackException.getMessage());
                        fallbackException.printStackTrace();
                        backend.cleanup();
                        if (onFinished != null) {
                            onFinished.accept(() -> {});
                        }
                        return;
                    }
                } else {
                    // If already Stub backend, just exit
                    backend.cleanup();
                    if (onFinished != null) {
                        onFinished.accept(() -> {});
                    }
                    return;
                }
            }
            
            // Save final references for use in lambda
            final javafx.scene.Node finalVideoNode = videoNode;
            final com.arcadeblocks.video.VideoPlayerBackend backendRef = backend;
            
            overlay.getChildren().add(finalVideoNode);

            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicBoolean callbackInvoked = new AtomicBoolean(false);
            
            // CRITICAL: Create wrapper to track overlay and backend together
            final VideoOverlayWrapper videoWrapper = new VideoOverlayWrapper(overlay, backendRef);

            Runnable removeOverlay = () -> {
                if (finished.compareAndSet(false, true)) {
                    // CRITICAL: Unbind ResponsiveLayoutHelper
                    try {
                        com.arcadeblocks.ui.util.ResponsiveLayoutHelper.unbind(overlay);
                    } catch (Exception e) {
                        System.err.println("Error cleaning up ResponsiveLayoutHelper: " + e.getMessage());
                    }
                    
                    // CRITICAL: Clean up video backend
                    try {
                        backendRef.cleanup();
                    } catch (Exception e) {
                        System.err.println("Error cleaning up video backend: " + e.getMessage());
                    }
                    
                    // CRITICAL: Remove wrapper from active overlays list
                    // Now remove wrapper instead of just overlay
                    synchronized (activeVideoOverlays) {
                        activeVideoOverlays.removeIf(w -> w.overlay == overlay);
                    }
                    
                    try {
                        // CRITICAL: Clear overlay from scene
                        FXGL.getGameScene().removeUINode(overlay);
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            };

            Runnable cleanup = () -> {
                if (callbackInvoked.compareAndSet(false, true)) {
                    if (onFinished != null) {
                        onFinished.accept(removeOverlay);
                    } else {
                        removeOverlay.run();
                    }
                }
            };
            
            // CRITICAL: Add wrapper to list BEFORE setting callback to prevent race condition
            // If video completes instantly (e.g., error), wrapper must already be in the list
            synchronized (activeVideoOverlays) {
                activeVideoOverlays.add(videoWrapper);
            }

            backendRef.setOnFinished(cleanup);
            backendRef.setOnError(cleanup);

            FXGL.getGameScene().addUINode(overlay);
            
            // Use ResponsiveLayoutHelper for proper positioning (like other fullscreen overlays)
            com.arcadeblocks.ui.util.ResponsiveLayoutHelper.bindToStage(overlay, (width, height) -> {
                overlay.setTranslateX(0);
                overlay.setTranslateY(0);
                overlay.setLayoutX(0);
                overlay.setLayoutY(0);
                // Update video node dimensions
                if (finalVideoNode instanceof javafx.scene.image.ImageView) {
                    javafx.scene.image.ImageView iv = (javafx.scene.image.ImageView) finalVideoNode;
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                } else if (finalVideoNode instanceof javafx.scene.layout.Pane) {
                    // For StubVideoBackend Pane nodes
                    javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) finalVideoNode;
                    pane.setPrefWidth(width);
                    pane.setPrefHeight(height);
                }
            });
            
            // Start playback
            backendRef.play();

            // Fallback timer in case video doesn't send onFinished
            // AtomicBoolean finished prevents double cleanup if video completes before timer
            // Увеличен запас с 0.1 до 3.0 секунд, чтобы VLC успел отправить событие finished
            if (fallbackDurationSeconds > 0.0) {
                FXGL.runOnce(cleanup, Duration.seconds(fallbackDurationSeconds + 3.0));
            }
            
            } catch (Exception e) {
                System.err.println("Failed to play video " + assetPath + ": " + e.getMessage());
                e.printStackTrace();
                if (onFinished != null) {
                    onFinished.accept(() -> {});
                }
            }
        });
    }

    public void showPoemScreen() {
        cleanupGameplayState();
        unblockMouseClicks();
        setSystemCursor();

        if (audioManager != null) {
            audioManager.cancelPendingMusic();
            audioManager.playMusic(AudioConfig.CREDITS_MUSIC, true);
        }

        clearUINodesSafely();
        FXGL.getGameScene().addUINode(new com.arcadeblocks.ui.PoemView(this));
    }

    private double estimateSoundDurationSeconds(String soundPath) {
        if (soundPath == null || soundPath.isBlank()) {
            return 2.0;
        }

        try {
            String normalized = soundPath.startsWith("/") ? soundPath.substring(1) : soundPath;
            if (!normalized.startsWith("sounds/")) {
                normalized = "sounds/" + normalized;
            }
            var url = getClass().getResource("/assets/" + normalized);
            if (url != null) {
                try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(url)) {
                    AudioFormat format = inputStream.getFormat();
                    long frames = inputStream.getFrameLength();
                    if (frames > 0 && format.getFrameRate() > 0) {
                        return frames / format.getFrameRate();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return fallbackSoundDuration(soundPath);
    }

    private double fallbackSoundDuration(String soundPath) {
        if (soundPath == null) {
            return 2.0;
        }
        if (soundPath.contains("loading_sound")) {
            return 10.0;
        }
        return 2.5;
    }

    private void saveCurrentGameSnapshot() {
        if (saveManager == null) {
            return;
        }
        
        // Не создаем snapshot если мы в настройках паузы - это предотвращает утечки памяти
        // при частом изменении настроек (например, чекбоксов)
        if (inPauseSettings) {
            return;
        }
        
        if (paddleComponent == null || isLevelCompleted || isTransitioning) {
            saveManager.clearGameSnapshot();
            return;
        }
        
        GameSnapshot snapshot = captureCurrentGameSnapshot();
        if (snapshot != null) {
            saveManager.saveGameSnapshot(snapshot);
        } else {
            saveManager.clearGameSnapshot();
        }
    }

    private GameSnapshot captureCurrentGameSnapshot() {
        if (paddleComponent == null) {
            return null;
        }
        
        var bricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK);
        if (bricks.isEmpty()) {
            return null;
        }
        
        GameSnapshot snapshot = new GameSnapshot();
        // Предварительно выделяем память для списков, чтобы избежать многократных реаллокаций
        snapshot.bricks = new ArrayList<>(bricks.size());
        var ballsCount = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL).size();
        snapshot.balls = new ArrayList<>(Math.max(ballsCount, 5));
        var bonusesCount = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS).size();
        snapshot.bonuses = new ArrayList<>(Math.max(bonusesCount, 10));
        var projectilesCount = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE).size();
        snapshot.projectiles = new ArrayList<>(Math.max(projectilesCount, 5));
        var bossesCount = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS).size();
        snapshot.bosses = new ArrayList<>(Math.max(bossesCount, 2));
        
        snapshot.level = FXGL.geti("level");
        snapshot.score = scoreManager != null ? scoreManager.getCurrentScore() : FXGL.geti("score");
        snapshot.lives = livesManager != null ? livesManager.getCurrentLives() : FXGL.geti("lives");
        snapshot.levelTimeSeconds = scoreManager != null ? scoreManager.getLevelTimerSeconds() : 0.0;

        if (audioManager != null) {
            snapshot.currentMusicFile = audioManager.getCurrentMusicFile();
            snapshot.currentMusicTime = audioManager.getCurrentMusicPosition();
        }
        
        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null) {
            return null;
        }
        
        GameSnapshot.PaddleState paddleState = new GameSnapshot.PaddleState();
        paddleState.x = paddleEntity.getX();
        paddleState.y = paddleEntity.getY();
        paddleState.sizeMultiplier = paddleComponent.getSizeMultiplier();
        paddleState.turboMode = paddleComponent.isTurboMode();
        paddleState.invisible = paddleComponent.isInvisible();
        paddleState.movementBlocked = paddleComponent.isMovementBlocked();
        paddleState.speed = paddleComponent.getSpeed();
        snapshot.paddle = paddleState;
        
        for (Entity brickEntity : bricks) {
            Brick brickComponent = brickEntity.getComponent(Brick.class);
            if (brickComponent == null) {
                continue;
            }
            GameSnapshot.BrickState state = new GameSnapshot.BrickState();
            state.x = brickEntity.getX();
            state.y = brickEntity.getY();
            state.health = brickComponent.getHealth();
            state.maxHealth = brickComponent.getMaxHealth();
            state.scoreValue = brickComponent.getScoreValue();
            state.colorHex = colorToHex(brickComponent.getBaseColor());
            try {
                state.colorName = brickEntity.getString("color");
            } catch (Exception ignored) {
            }
            snapshot.bricks.add(state);
        }
        
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ballEntity : balls) {
            Ball ballComponent = ballEntity.getComponent(Ball.class);
            if (ballComponent == null) {
                continue;
            }
            GameSnapshot.BallState state = new GameSnapshot.BallState();
            state.x = ballEntity.getX();
            state.y = ballEntity.getY();

            if (ballComponent.isPausedForCountdown()) {
                state.velocityX = ballComponent.getPausedVelocityX();
                state.velocityY = ballComponent.getPausedVelocityY();
                state.pausedForCountdown = true;
                state.pausedVelocityX = ballComponent.getPausedVelocityX();
                state.pausedVelocityY = ballComponent.getPausedVelocityY();
            } else {
                PhysicsComponent physics = ballEntity.getComponent(PhysicsComponent.class);
                Point2D velocity = physics != null ? physics.getLinearVelocity() : Point2D.ZERO;
                state.velocityX = velocity.getX();
            state.velocityY = velocity.getY();
            state.pausedForCountdown = false;
        }

        double[] offset = ballComponent.getAttachedOffset();
        state.attachedOffsetX = offset != null ? offset[0] : 0.0;
        state.attachedOffsetY = offset != null ? offset[1] : -GameConfig.BALL_RADIUS * 2 - 5;

        state.attachedToPaddle = ballComponent.isAttachedToPaddle();
        state.stickyEnabled = ballComponent.isStickyEnabled();
            state.speedMultiplier = ballComponent.getSpeedMultiplier();
            state.energyBall = ballComponent.isEnergyBall();
            state.explosionBall = ballComponent.isExplosionBall();
            state.weakBall = ballComponent.isWeakBall();
            state.chaoticBall = ballComponent.isChaoticBall();
            state.extraBall = ballComponent.isExtraBall();
            state.maintainConstantSpeed = ballComponent.isMaintainConstantSpeed();
            state.targetSpeed = ballComponent.getTargetSpeed();
            snapshot.balls.add(state);
        }
        
        var bonusEntities = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS);
        for (Entity bonusEntity : bonusEntities) {
            Bonus bonusComponent = bonusEntity.getComponent(Bonus.class);
            if (bonusComponent == null || bonusComponent.isCollected()) {
                continue;
            }
            GameSnapshot.BonusEntityState state = new GameSnapshot.BonusEntityState();
            state.x = bonusEntity.getX();
            state.y = bonusEntity.getY();
            BonusType bonusType = bonusComponent.getBonusType();
            if (bonusType != null) {
                state.bonusType = bonusType.name();
            }
            state.fallSpeed = bonusComponent.getFallSpeed();
            snapshot.bonuses.add(state);
        }
        
        var projectiles = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE);
        for (Entity projectileEntity : projectiles) {
            GameSnapshot.ProjectileState state = new GameSnapshot.ProjectileState();
            state.x = projectileEntity.getX();
            state.y = projectileEntity.getY();
            snapshot.projectiles.add(state);
        }
        
        var bosses = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS);
        for (Entity bossEntity : bosses) {
            Boss bossComponent = bossEntity.getComponentOptional(Boss.class).orElse(null);
            if (bossComponent == null) {
                continue;
            }
            GameSnapshot.BossState state = new GameSnapshot.BossState();
            state.bossId = bossComponent.getBossId();
            state.x = bossEntity.getX();
            state.y = bossEntity.getY();
            state.health = bossComponent.getHealth();
            state.maxHealth = bossComponent.getMaxHealth();
            state.spawnCompleted = bossComponent.isSpawnCompleted();
            snapshot.bosses.add(state);
        }
        
        if (bonusEffectManager != null) {
            snapshot.bonusEffects = bonusEffectManager.createSnapshot();
        }
        
        return snapshot;
    }

    private boolean restoreGameSnapshot(GameSnapshot snapshot) {
        if (snapshot == null || paddleComponent == null) {
            return false;
        }
        if (snapshot.bricks == null || snapshot.bricks.isEmpty()) {
            return false;
        }
        
        Entity paddleEntity = paddleComponent.getEntity();
        if (paddleEntity == null) {
            return false;
        }
        
        FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.BALL).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE).forEach(Entity::removeFromWorld);
        FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS).forEach(Entity::removeFromWorld);
        
        // Clear list of attached balls before restore
        Ball.clearAttachedBalls();
        
        Brick.resetBrickCounter();
        
        FXGL.set("score", snapshot.score);
        FXGL.set("lives", snapshot.lives);
        
        if (scoreManager != null) {
            scoreManager.setScore(snapshot.score);
            scoreManager.setLevelTimerSeconds(snapshot.levelTimeSeconds);
            scoreManager.resumeLevelTimer();
        }
        if (livesManager != null) {
            livesManager.setCurrentLivesFromSnapshot(snapshot.lives);
        }
        
        if (gameplayUIView != null) {
            gameplayUIView.updateScore(snapshot.score);
            gameplayUIView.updateLives(snapshot.lives);
            com.arcadeblocks.config.LevelConfig.LevelData levelData = com.arcadeblocks.config.LevelConfig.getLevel(snapshot.level);
            String levelName = levelData != null ? levelData.getName() : null;
            gameplayUIView.updateLevel(snapshot.level, levelName);
        }

        // Store restored music state to be used after countdown
        this.restoredMusicFile = snapshot.currentMusicFile;
        this.restoredMusicTime = snapshot.currentMusicTime;

        leftPressed = false;
        rightPressed = false;
        turboPressed = false;
        resetCallBallState();
        paddleComponent.setMoveLeft(false);
        paddleComponent.setMoveRight(false);
        paddleComponent.setTurboMode(false);
        
        if (saveManager != null && !isDebugMode) {
            saveManager.setScore(snapshot.score);
            saveManager.setLives(snapshot.lives);
        }
        
        paddleEntity.setPosition(snapshot.paddle.x, snapshot.paddle.y);
        paddleComponent.setSizeMultiplier(snapshot.paddle.sizeMultiplier != 0 ? snapshot.paddle.sizeMultiplier : 1.0);
        paddleComponent.setInvisible(snapshot.paddle.invisible);
        paddleComponent.setMovementBlocked(snapshot.paddle.movementBlocked);
        if (snapshot.paddle.speed > 0) {
            paddleComponent.setSpeed(snapshot.paddle.speed);
        }
        
        for (GameSnapshot.BrickState brickState : snapshot.bricks) {
            SpawnData spawnData = new SpawnData(brickState.x, brickState.y);
            if (brickState.colorName != null) {
                spawnData.put("color", brickState.colorName);
            }
            Entity brickEntity = FXGL.spawn("brick", spawnData);
            Color baseColor = colorFromHex(brickState.colorHex, Color.web(GameConfig.NEON_PURPLE));
            boolean isExplosive = "explosive".equals(brickState.colorName);
            Brick brickComponent = new Brick(Math.max(brickState.maxHealth, 1), baseColor, brickState.scoreValue, isExplosive);
            brickEntity.addComponent(brickComponent);
            brickComponent.restoreState(brickState.health);
        }
        
        for (GameSnapshot.BallState ballState : snapshot.balls) {
            SpawnData ballData = new SpawnData(ballState.x, ballState.y);
            Entity ballEntity = FXGL.spawn("ball", ballData);
            Ball ballComponent = new Ball();
            ballEntity.addComponent(ballComponent);
            
            ballComponent.setSpeedMultiplier(ballState.speedMultiplier != 0 ? ballState.speedMultiplier : 1.0);
        ballComponent.setStickyEnabled(ballState.stickyEnabled);
        ballComponent.setEnergyBall(ballState.energyBall);
        ballComponent.setExplosionBall(ballState.explosionBall);
        ballComponent.setWeakBall(ballState.weakBall);
        ballComponent.setChaoticBall(ballState.chaoticBall);
        ballComponent.setExtraBall(ballState.extraBall);
        ballComponent.setMaintainConstantSpeed(ballState.maintainConstantSpeed);
        if (ballState.targetSpeed > 0) {
            ballComponent.setTargetSpeed(ballState.targetSpeed);
        }

        double offsetX = ballState.attachedOffsetX;
        double offsetY = ballState.attachedOffsetY;
        ballComponent.setAttachedOffset(offsetX, offsetY);

            ballComponent.restorePauseState(ballState.pausedForCountdown, ballState.pausedVelocityX, ballState.pausedVelocityY);
            
            PhysicsComponent physics = ballEntity.getComponent(PhysicsComponent.class);
            if (ballState.attachedToPaddle) {
                ballComponent.attachToPaddle(paddleEntity);
                // Add ball to attached list for proper launch system operation
                Ball.addAttachedBall(ballEntity);
            } else if (physics != null) {
                if (!ballState.pausedForCountdown) {
                    physics.setBodyType(BodyType.DYNAMIC);
                    physics.setLinearVelocity(ballState.velocityX, ballState.velocityY);
                }
            }
        }
        
        if (snapshot.balls == null || snapshot.balls.isEmpty()) {
            SpawnData ballData = new SpawnData(
                paddleEntity.getX() + paddleEntity.getWidth() / 2.0 - GameConfig.BALL_RADIUS,
                paddleEntity.getY() - GameConfig.BALL_RADIUS * 2 - 5
            );
            Entity ballEntity = FXGL.spawn("ball", ballData);
            Ball ballComponent = new Ball();
            ballEntity.addComponent(ballComponent);
            ballComponent.attachToPaddle(paddleEntity);
            // Add the ball to the list of attached items for the launch system to work correctly.
            Ball.addAttachedBall(ballEntity);
        }
        
        if (bonusEffectManager != null) {
            bonusEffectManager.restoreFromSnapshot(snapshot.bonusEffects);
        }
        
        for (GameSnapshot.BonusEntityState bonusState : snapshot.bonuses) {
            if (bonusState.bonusType == null) {
                continue;
            }
            try {
                BonusType bonusType = BonusType.valueOf(bonusState.bonusType);
                SpawnData bonusData = new SpawnData(bonusState.x, bonusState.y);
                bonusData.put("bonusType", bonusType);
                Entity bonusEntity = FXGL.spawn("bonus", bonusData);
                Bonus bonusComponent = new Bonus();
                bonusComponent.setBonusType(bonusType);
                bonusComponent.setFallSpeed(bonusState.fallSpeed);
                bonusEntity.addComponent(bonusComponent);
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        for (GameSnapshot.ProjectileState projectileState : snapshot.projectiles) {
            FXGL.spawn("projectile", projectileState.x, projectileState.y);
        }
        
        for (GameSnapshot.BossState bossState : snapshot.bosses) {
            SpawnData bossData = new SpawnData(bossState.x, bossState.y);
            if (bossState.bossId != null) {
                bossData.put("bossId", bossState.bossId);
            }
            bossData.put("skipSpawnAnimation", true);
            Entity bossEntity = FXGL.spawn("boss", bossData);
            Boss bossComponent = bossEntity.getComponentOptional(Boss.class).orElse(null);
            if (bossComponent != null) {
                bossComponent.restoreState(
                    bossState.health,
                    bossState.spawnCompleted
                );
            }
        }
        
        // After restore - ensure mouse control is active
        installMousePaddleControlHandlers();
        
        // Очищаем snapshot для освобождения памяти
        snapshot.clear();
        
        return true;
    }

    private static String colorToHex(Color color) {
        if (color == null) {
            return null;
        }
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static Color colorFromHex(String hex, Color fallback) {
        if (hex == null || hex.isEmpty()) {
            return fallback;
        }
        try {
            return Color.web(hex);
        } catch (Exception e) {
            return fallback;
        }
    }
    
    /**
     * Toggle pause
     */
    public void togglePause() {
        if (isPaused) {
            if (consumePauseResumeLock()) {
                return;
            }
            // Resume game
            resumeGame();
        } else {
            // Pause game
            pauseGame();
        }
    }
    
    /**
     * Pause the game
     */
    public void pauseGame() {
        if (isPaused) return; // Already paused
        
        isPaused = true;
        pauseResumeLockFromFocusLoss = false;
        enforcePauseFreeze();
        
        // Stop music
        if (audioManager != null) {
            audioManager.pauseMusic();
        }

        // Show pause screen
        showPauseScreen();

        // Cursor visible during pause
        setSystemCursor();
    }
    
    /**
     * Resume the game
     */
    public void resumeGame() {
        if (!isPaused) return; // Not paused
        pauseResumeLockFromFocusLoss = false;
        
        // Hide pause screen
        hidePauseScreen();
        
        // Show countdown timer
        showCountdownTimer();
    }
    
    /**
     * Show countdown timer
     */
    public void showCountdownTimer() {
        if (isCountdownActive) return;
        
        // Check if CountdownTimerView already exists in scene
        var scene = FXGL.getGameScene();
        var existingCountdown = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.CountdownTimerView)
            .findFirst();
        
        if (existingCountdown.isPresent()) {
            // CountdownTimerView already exists, don't create new one
            return;
        }
        
        isCountdownActive = true;
        enforcePauseFreeze();
        
        CountdownTimerView countdownView = new CountdownTimerView(this, () -> {
            // Callback called after timer completes
            isCountdownActive = false;
            actuallyResumeGame();
        });
        
        FXGL.getGameScene().addUINode(countdownView);
    }
    
    /**
     * Show countdown timer for level start
     */
    public void showCountdownTimerForLevelStart(int levelNumber) {
        if (isCountdownActive) return;
        
        isCountdownActive = true;
        enforcePauseFreeze();
        
        CountdownTimerView countdownView = new CountdownTimerView(this, () -> {
            // Callback called after timer completes
            isCountdownActive = false;
            enforceVSyncAfterResume();
            
            // Play music from restored state or start new
            if (restoredMusicFile != null && !restoredMusicFile.isEmpty()) {
                audioManager.playMusic(restoredMusicFile, true, restoredMusicTime);
                restoredMusicFile = null;
                restoredMusicTime = 0.0;
            } else {
                playLevelMusic(levelNumber);
            }

            // Resume all balls movement
            resumeAllBalls();
        // System.out.println("Level music starts after the timer");
            
            // Resume game process
            FXGL.getGameController().resumeEngine();
            
            // Resume level timer
            if (scoreManager != null && !isLevelCompleted && !isTransitioning) {
                scoreManager.resumeLevelTimer();
            }
            
            // Resume all bonus timers
            if (bonusEffectManager != null) {
                bonusEffectManager.resumeAllBonusTimers();
            }
        });
        
        FXGL.getGameScene().addUINode(countdownView);
    }
    
    /**
     * Actually resume the game (called after timer)
     */
    public void actuallyResumeGame() {
        isPaused = false;
        enforceVSyncAfterResume();

        // Resume game process
        resumeAllBalls();
        FXGL.getGameController().resumeEngine();
        
        if (scoreManager != null && !isLevelCompleted && !isTransitioning) {
            scoreManager.resumeLevelTimer();
        }

        if (bonusEffectManager != null) {
            bonusEffectManager.resumeAllBonusTimers();
        }
        
        // Resume music
        if (audioManager != null) {
            audioManager.resumeMusic();
        }

        // Restore letterbox overlay after returning from pause
        updateLetterboxOverlay();

        // Cursor hidden during gameplay
        setHiddenCursor();
    }
    
    public void setLevelIntroActive(boolean active) {
        isLevelIntroActive = active;
    }

    public void setStoryOverlayActive(boolean active) {
        isStoryOverlayActive = active;
    }
    
    /**
     * Set current main menu background
     */
    public void setCurrentMainMenuBackground(String background) {
        this.currentMainMenuBackground = background;
    }
    
    /**
     * Get current main menu background
     */
    public String getCurrentMainMenuBackground() {
        return currentMainMenuBackground;
    }
    
    /**
     * Start main menu music with randomization based on game completion status
     */
    public void startMainMenuMusic() {
        if (audioManager == null) {
            // System.out.println("DEBUG: audioManager is null, cannot start music");
            return;
        }
        
        // Определяем состояние прогресса игры
        com.arcadeblocks.config.AudioConfig.GameProgressState progressState = 
            com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
        
        if (saveManager != null) {
            int maxLevel = 0;
            boolean gameCompleted = false;
            
            // Проверяем все слоты сохранения
            for (int slot = 1; slot <= 4; slot++) {
                com.arcadeblocks.utils.SaveManager.SaveInfo saveInfo = saveManager.getSaveInfo(slot);
                if (saveInfo != null) {
                    // Находим максимальный достигнутый уровень
                    if (saveInfo.level > maxLevel) {
                        maxLevel = saveInfo.level;
                    }
                    // Проверяем, завершена ли игра
                    if (saveManager.isGameCompletedInSlot(slot)) {
                        gameCompleted = true;
                    }
                }
            }
            
            // Определяем состояние прогресса
            if (gameCompleted) {
                progressState = com.arcadeblocks.config.AudioConfig.GameProgressState.COMPLETED;
                // System.out.println("DEBUG: Game completed, using COMPLETED music");
            } else if (maxLevel >= 101) {
                progressState = com.arcadeblocks.config.AudioConfig.GameProgressState.AFTER_LEVEL_100;
                // System.out.println("DEBUG: Max level " + maxLevel + ", using AFTER_LEVEL_100 music");
            } else {
                // System.out.println("DEBUG: Max level " + maxLevel + ", using NORMAL music");
            }
        }
        
        // Получаем случайную музыку в зависимости от прогресса игры
        String musicFile = com.arcadeblocks.config.AudioConfig.getRandomMainMenuMusic(progressState);
        
        // System.out.println("DEBUG: Starting main menu music: " + musicFile + " (state=" + progressState + ")");
        
        // Запускаем музыку
        audioManager.stopMusic();
        audioManager.playMusic(musicFile, true);
    }


    /**
     * Release cached main-menu visuals when they are no longer needed.
     * This drops the reference string and forgets the cached Image so prism textures can be collected.
     */
    private void releaseMainMenuBackgroundAssets() {
        if (currentMainMenuBackground != null) {
            try {
                javafx.scene.image.Image img = ImageCache.get(currentMainMenuBackground);
                if (img != null) {
                    ImageCache.forget(img);
                }
            } catch (Exception ignored) {
                // If the image is not cached, just drop the reference name.
            }
            currentMainMenuBackground = null;
        }
    }

    /**
     * Reset loading sound reference to allow the audio cache to evict it after use.
     */
    private void releaseLevelLoadingSoundReference() {
        currentLevelLoadingSound = AudioConfig.DEFAULT_LEVEL_LOADING_SOUND;
    }
    
    /**
     * Set originally selected save slot
     */
    public void setOriginalSaveSlot(int slotNumber) {
        this.originalSaveSlot = slotNumber;
    }
    
    /**
     * Get originally selected save slot
     */
    public int getOriginalSaveSlot() {
        return originalSaveSlot;
    }
    
    /**
     * Check if level is completed
     */
    public boolean isLevelCompleted() {
        return isLevelCompleted;
    }
    
    /**
     * Check if transitioning between levels
     */
    public boolean isTransitioning() {
        return isTransitioning;
    }
    
    /**
     * Show pause screen
     */
    public void showPauseScreen() {
        // Check if PauseView already exists in scene
        var scene = FXGL.getGameScene();
        var existingPauseView = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.PauseView)
            .findFirst();
        
        if (existingPauseView.isPresent()) {
            // PauseView already exists, don't create new one
            return;
        }
        
        // Clear letterbox overlay before showing pause
        clearLetterboxOverlay();
        
        // Create pause screen
        com.arcadeblocks.ui.PauseView pauseView = new com.arcadeblocks.ui.PauseView(this);
        FXGL.getGameScene().addUINode(pauseView);
        // Remove mouse handlers during pause so paddle doesn't move under UI
        uninstallMousePaddleControlHandlers();
    }
    
    /**
     * Hide pause screen
     */
    public void hidePauseScreen() {
        // Remove all pause screens (safely)
        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
        for (var node : uiNodes) {
            if (node instanceof com.arcadeblocks.ui.PauseView) {
                // CRITICAL: Call cleanup() before removal to free all resources
                ((com.arcadeblocks.ui.PauseView) node).cleanup();
                // Clear handlers before removal
                FXGL.getGameScene().removeUINode(node);
            }
        }
        // Restore handlers after closing pause
        installMousePaddleControlHandlers();
    }

    public void enableMouseFollowTicker(boolean enable) {
        if (mouseFollowTicker != null) {
            mouseFollowTicker.stop();
            mouseFollowTicker = null;
        }
        if (!enable) return;
        // Avoid MouseInfo on Wayland
        if (isLinuxWayland()) return;
        mouseFollowTicker = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(16), e -> {
                try {
                    javafx.stage.Stage stage = FXGL.getPrimaryStage();
                    if (stage == null || stage.getScene() == null) return;
                    if (isCountdownActive) return;
                    java.awt.PointerInfo pi = java.awt.MouseInfo.getPointerInfo();
                    if (pi == null) return;
                    java.awt.Point pt = pi.getLocation();
                    javafx.geometry.Point2D local = stage.getScene().getRoot().screenToLocal(pt.x, pt.y);
                    if (local != null) {
                        movePaddleForSceneX(local.getX());
                    }
                } catch (Throwable ignored) {}
            })
        );
        mouseFollowTicker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        mouseFollowTicker.play();
    }

    public static void main(String[] args) {
        // Initialize AppDataManager and set working directory BEFORE anything else
        String dataDir = AppDataManager.getDataDirectory().toString();
        
        // Pre-create logs directory with proper permissions
        try {
            java.nio.file.Path logsPath = java.nio.file.Paths.get(dataDir, "logs");
            java.nio.file.Files.createDirectories(logsPath);
        } catch (Exception e) {
            System.err.println("Failed to create logs directory: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Pre-create system directory
        try {
            java.nio.file.Path systemPath = java.nio.file.Paths.get(dataDir, "system");
            java.nio.file.Files.createDirectories(systemPath);
        } catch (Exception e) {
            System.err.println("Failed to create system directory: " + e.getMessage());
        }
        
        // Save original working directory for resource loading
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("app.install.dir", originalUserDir);
        
        // Set working directory to user data directory so FXGL creates logs/ and system/ there
        System.setProperty("user.dir", dataDir);
        
        // Disable DPI scaling for Windows - force 100% scaling
        System.setProperty("glass.win.uiScale", "100%");
        System.setProperty("glass.gtk.uiScale", "100%");
        System.setProperty("prism.allowhidpi", "false");
        
        // Setup console encoding for correct UTF-8 display (especially important for Windows)
        com.arcadeblocks.utils.ConsoleUtils.setupConsoleEncoding();
        
        // Attempt to launch via XWayland (X11) on Linux Wayland
        if (shouldRelaunchUnderX11(args)) {
            if (tryRelaunchUnderX11(args)) {
                return; // Terminate current process, control is with child
            }
            // If relaunch failed, continue with normal launch
        }
        launch(args);
    }

    public static boolean shouldRelaunchUnderX11(String[] args) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) return false;
        // Don't loop
        boolean alreadyForced = Arrays.stream(args).anyMatch(a -> "--x11-launched".equals(a));
        if (alreadyForced) return false;
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        boolean isWayland = (sessionType != null && sessionType.equalsIgnoreCase("wayland"))
            || (waylandDisplay != null && !waylandDisplay.isEmpty());
        if (!isWayland) return false;
        String gdkBackend = System.getenv("GDK_BACKEND");
        // If already X11 - do nothing
        if (gdkBackend != null && gdkBackend.toLowerCase().contains("x11")) return false;
        return true;
    }

    public static boolean tryRelaunchUnderX11(String[] args) {
        try {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            String classPath = System.getProperty("java.class.path");
            String mainClass = "com.arcadeblocks.ArcadeBlocksApp";
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(javaBin);
            cmd.add("-cp");
            cmd.add(classPath);
            cmd.add(mainClass);
            cmd.add("--x11-launched");
            cmd.addAll(java.util.Arrays.asList(args));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            // Set environment for X11
            java.util.Map<String, String> env = pb.environment();
            env.put("GDK_BACKEND", "x11");
            // Helps on some systems for SDL (we have audio via SDL, but no video output)
            env.putIfAbsent("SDL_VIDEODRIVER", "x11");
            pb.inheritIO();
            Process p = pb.start();
            // Don't wait - let child process take over terminal, terminate current
            return true;
        } catch (Exception e) {
            System.err.println("Failed to relaunch application under X11: " + e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, нужно ли показывать сюжетное окно для первого уровня.
     * Для уровня 1: показывать только при первом прохождении или в отладочном режиме.
     * Для остальных уровней: всегда показывать (если есть сюжетное окно).
     */
    private boolean shouldShowLevel1Story(int levelNumber) {
        if (levelNumber != 1) {
            return true; // Для всех уровней кроме первого - показывать как обычно
        }
        
        // В отладочном режиме с принудительным показом сюжетных окон - всегда показывать
        if (alwaysShowChapterStory) {
            return true;
        }
        
        // Для первого уровня проверяем очки в сохранении
        if (saveManager != null) {
            int savedScore = saveManager.getScore();
            return savedScore == 0; // Показывать только если очки равны 0 (первое прохождение)
        }
        
        return true; // Если нет saveManager, показывать по умолчанию
    }

    /**
     * Проверяет, нужно ли отключать звук загрузки во время видео.
     * Для уровней с видео: звук загрузки отключается во время воспроизведения видео.
     */
    private boolean shouldDisableLoadingSoundDuringVideo(int levelNumber) {
        // Для уровней 1, 11, 21, 31, 32, 41, 51, 61, 71, 81 и 91 отключаем звук загрузки во время видео
        return levelNumber == 1 || levelNumber == 11 || levelNumber == 21 || levelNumber == 31 || levelNumber == 32 || levelNumber == 41 || levelNumber == 51 || levelNumber == 61 || levelNumber == 71 || levelNumber == 81 || levelNumber == 91;
    }

    /**
     * Проверяет, нужно ли показывать видео для уровней с особой логикой.
     * Для уровней 1, 11, 21, 31, 32, 41, 51, 61, 71, 81 и 91: видео всегда показывается.
     * Для остальных уровней: всегда показывать (если есть видео).
     */
    private boolean shouldShowLevel1StoryAndVideo(int levelNumber) {
        // Для уровней 1, 11, 21, 31, 32, 41, 51, 61, 71, 81 и 91 всегда показываем видео, независимо от прогресса
        if (levelNumber == 1 || levelNumber == 11 || levelNumber == 21 || levelNumber == 31 || levelNumber == 32 || levelNumber == 41 || levelNumber == 51 || levelNumber == 61 || levelNumber == 71 || levelNumber == 81 || levelNumber == 91) {
            return true;
        }
        
        // Для остальных уровней - показывать как обычно
        return true;
    }
    
    /**
     * Показать главное меню
     */
    public void showMainMenu() {
        // КРИТИЧНО: Удаляем только overlay views, MainMenuView уже на экране!
        // Ищем и удаляем только SettingsView, HelpView, LanguageView, SaveGameView
        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
        for (javafx.scene.Node node : uiNodes) {
            if (node instanceof com.arcadeblocks.ui.SettingsView || 
                node instanceof com.arcadeblocks.ui.HelpView ||
                node instanceof com.arcadeblocks.ui.LanguageView ||
                node instanceof com.arcadeblocks.ui.SaveGameView) {
                removeUINodeSafely(node);
            }
        }
        
        // КРИТИЧНО: Восстанавливаем фокус на MainMenuView после удаления overlay-окон
        for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
            if (node instanceof com.arcadeblocks.ui.MainMenuView) {
                ((com.arcadeblocks.ui.MainMenuView) node).restoreFocus();
                break;
            }
        }
        
        // Показываем курсор
        setSystemCursor();
    }
    
    /**
     * Проверить, открыты ли overlay-окна (настройки, справка, языки, сохранения)
     */
    public boolean hasOverlayWindows() {
        for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
            if (node instanceof com.arcadeblocks.ui.SettingsView || 
                node instanceof com.arcadeblocks.ui.HelpView ||
                node instanceof com.arcadeblocks.ui.LanguageView ||
                node instanceof com.arcadeblocks.ui.SaveGameView) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Перезапустить игру (полный перезапуск процесса)
     */
    public void restartGame() {
        // Запускаем в отдельном потоке, чтобы не блокировать UI
        new Thread(() -> {
            try {
                // System.out.println("Начинаем перезапуск игры...");
                
                // КРИТИЧНО: Сначала останавливаем все, что может держать ресурсы
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Останавливаем игровой движок
                        FXGL.getGameController().pauseEngine();
                        
                        // Останавливаем и очищаем аудио
                        if (audioManager != null) {
                            audioManager.stopMusic();
                            audioManager.cleanup();
                        }
                        
                        // КРИТИЧНО: Принудительно очищаем все UI компоненты с cleanup
                        var uiNodes = new java.util.ArrayList<>(FXGL.getGameScene().getUINodes());
                        for (var node : uiNodes) {
                            if (node instanceof SupportsCleanup) {
                                try {
                                    ((SupportsCleanup) node).cleanup();
                                } catch (Exception ignored) {}
                            }
                        }
                        
                        // Очищаем UI
                        clearUINodesSafely();
                        
                        // Очищаем игровой мир
                        try {
                            FXGL.getGameWorld().getEntities().forEach(entity -> {
                                try {
                                    entity.removeFromWorld();
                                } catch (Exception ignored) {}
                            });
                        } catch (Exception ignored) {}
                        
                        // КРИТИЧНО: Очищаем кэш изображений
                        try {
                            ImageCache.clear();
                        } catch (Exception ignored) {}
                        
                        // Принудительный сбор мусора
                        System.gc();
                        
                    } catch (Exception e) {
                        System.err.println("Ошибка при очистке перед перезапуском: " + e.getMessage());
                    }
                });
                
                // Даем время на очистку
                Thread.sleep(500);
                
                // Получаем путь к Java
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
                
                // Получаем classpath
                String classpath = System.getProperty("java.class.path");
                
                // Получаем главный класс
                String mainClass = ArcadeBlocksApp.class.getName();
                
                // Создаем команду для запуска
                java.util.List<String> command = new java.util.ArrayList<>();
                command.add(javaBin);
                
                // Добавляем JVM аргументы если они были
                java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                java.util.List<String> jvmArgs = runtimeMxBean.getInputArguments();
                for (String arg : jvmArgs) {
                    // Пропускаем агенты отладки
                    if (!arg.contains("-agentlib") && !arg.contains("-javaagent")) {
                        command.add(arg);
                    }
                }
                
                command.add("-cp");
                command.add(classpath);
                command.add(mainClass);
                
                // Создаем процесс
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.inheritIO();
                
                // System.out.println("Запускаем новый процесс...");
                
                // Запускаем новый процесс
                Process newProcess = builder.start();
                
                // Даем больше времени новому процессу для инициализации
                Thread.sleep(2000);
                
                // Проверяем, что новый процесс запустился
                if (newProcess.isAlive()) {
                    // System.out.println("Новый процесс запущен успешно. Завершаем старый процесс...");
                } else {
                    // System.err.println("ВНИМАНИЕ: Новый процесс не запустился!");
                }
                
                // Закрываем JavaFX
                javafx.application.Platform.runLater(() -> {
                    javafx.application.Platform.exit();
                });
                
                // Даем время JavaFX завершиться
                Thread.sleep(1000);
                
                // System.out.println("Принудительное завершение старого процесса...");
                
                // Принудительное завершение
                Runtime.getRuntime().halt(0);
                
            } catch (Exception e) {
                System.err.println("Критическая ошибка при перезапуске игры: " + e.getMessage());
                e.printStackTrace();
                
                // В случае ошибки принудительно завершаем процесс
                Runtime.getRuntime().halt(1);
            }
        }, "GameRestartThread").start();
    }
    
    /**
     * Get video backend factory for video playback
     */
    public com.arcadeblocks.video.VideoBackendFactory getVideoBackendFactory() {
        return videoBackendFactory;
    }
}
>>>>>>> origin/main
