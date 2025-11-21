package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.ui.LivesIndicator;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Менеджер системы жизней
 */
public class LivesManager {
    
    private ArcadeBlocksApp app;
    private int currentLives;
    private HBox livesContainer;
    private ImageView[] lifeIcons;
    private LivesIndicator livesIndicator; // Новый индикатор жизней
    private boolean lifeLossInProgress = false;
    private FadeTransition activeLifeLossFadeTransition; // КРИТИЧНО: Ссылка на активную анимацию для cleanup
    
    // Автосохранение: задержка для коалесцирования запросов и флаг для контроля
    private static final Duration LIVES_AUTOSAVE_DELAY = Duration.millis(500);
    private boolean autosaveScheduled = false;
    
    public LivesManager(ArcadeBlocksApp app) {
        this.app = app;
        com.arcadeblocks.utils.SaveManager saveManager = app.getSaveManager();
        if (app.isDebugMode()) {
            Integer overrideLives = app.getDebugLivesOverride();
            if (overrideLives != null) {
                this.currentLives = Math.max(1, overrideLives);
            } else {
                com.arcadeblocks.config.DifficultyLevel difficulty = app.getDebugDifficultyOverride();
                if (difficulty == null && saveManager != null) {
                    try {
                        difficulty = saveManager.getDifficulty();
                    } catch (Exception ignored) {}
                }
                if (difficulty == null) {
                    difficulty = com.arcadeblocks.config.DifficultyLevel.NORMAL;
                }
                this.currentLives = Math.max(1, difficulty.getLives());
                app.setDebugLivesOverride(this.currentLives);
            }
        } else if (saveManager != null) {
            int storedLives = saveManager.getLives();
            if (storedLives > 0) {
                this.currentLives = storedLives;
            } else {
                com.arcadeblocks.config.DifficultyLevel difficulty = app.getEffectiveDifficulty();
                this.currentLives = difficulty.getLives();
                saveManager.setLives(this.currentLives);
            }
        } else {
            this.currentLives = GameConfig.INITIAL_LIVES;
        }
        createLivesUI();

        // Синхронизируем глобальное состояние и верхнюю панель сразу после инициализации
        FXGL.set("lives", currentLives);
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateLives(currentLives);
        } else {
            lifeLossInProgress = false;
        }
    }
    
    private void createLivesUI() {
        // Новый HUD показывает жизни через GameplayUIView (цифрой), поэтому старые иконки больше не нужны.
        // Чтобы не тянуть неиспользуемую текстуру life.png в память, полностью отключаем создание иконок.
        livesIndicator = null;
        lifeIcons = new ImageView[0];
        livesContainer = null;
        // Legacy UI (border/old container) intentionally not created.
    }
    
    public void loseLife() {
        if (lifeLossInProgress) {
            return;
        }
        lifeLossInProgress = true;
        
        // КРИТИЧНО: Блокируем управление ракеткой СРАЗУ после установки флага
        // Это предотвращает движение как мышью, так и клавиатурой во время анимации потери жизни
        freezeActiveObjects();
        app.blockMouseClicks();

        if (currentLives > 0) {
            currentLives--;
            
            // Останавливаем все звуки бонусов ПЕРЕД воспроизведением звука потери жизни
            if (app.getAudioManager() != null) {
                app.getAudioManager().stopAllSFX();
            }
            
			// Воспроизводим звук потери жизни или специальный звук при потере предпоследней жизни
			try {
				if (currentLives == 1) {
					// Потеря предпоследней жизни
					app.getAudioManager().playSFX("sounds/sfx/powerup_bad.wav");
				} else if (currentLives > 1) {
					// Обычная потеря жизни (но не последней) - выбираем случайный звук
					String randomLifeLostSound = com.arcadeblocks.config.AudioConfig.getRandomLifeLostSound();
					app.getAudioManager().playSFX(randomLifeLostSound);
				}
			} catch (Exception e) {
				// Если звук не найден, игнорируем
				// System.out.println("Не удалось воспроизвести звук life_lost.wav: " + e.getMessage());
			}
            
            // Обновляем новый индикатор жизней
            if (livesIndicator != null) {
                livesIndicator.loseLife();
            }
            
            persistLivesChange(true);

            if (currentLives > 0) {
                app.fadeOutBonuses(false, () -> {
                    if (app.getBonusEffectManager() != null) {
                        app.getBonusEffectManager().clearAllBonuses();
                        app.getBonusEffectManager().resetAllEffects();
                    }
                });
            }
            
            // Анимация потери жизни (старый код для совместимости)
            if (currentLives < lifeIcons.length) {
                ImageView lostLife = lifeIcons[currentLives];
                
                // КРИТИЧНО: Останавливаем предыдущую анимацию перед созданием новой
                if (activeLifeLossFadeTransition != null) {
                    try {
                        activeLifeLossFadeTransition.stop();
                    } catch (Exception e) {
                        // Игнорируем ошибки остановки
                    }
                }
                
                activeLifeLossFadeTransition = new FadeTransition(Duration.millis(300), lostLife);
                activeLifeLossFadeTransition.setFromValue(1.0);
                activeLifeLossFadeTransition.setToValue(0.3); // Полупрозрачность при потере жизни
                
                // КРИТИЧНО: Очищаем ссылку после завершения анимации
                activeLifeLossFadeTransition.setOnFinished(e -> activeLifeLossFadeTransition = null);
                
                activeLifeLossFadeTransition.play();
            }
            
            // Сбрасываем позиции мяча и ракетки только если есть жизни
            if (currentLives > 0) {
                app.resetBallAndPaddle();
                
                // Показываем countdown после возрождения
                FXGL.runOnce(() -> {
                    lifeLossInProgress = false;
                }, app.getLevelFadeDuration().add(Duration.millis(150)));
            } else {
                // Если жизней не осталось, воспроизводим звук Game Over и показываем экран
                try {
                    app.getAudioManager().playSFX("sounds/game_over.wav");
                } catch (Exception e) {
                    // Если звук не найден, игнорируем
                    System.err.println("Не удалось воспроизвести звук game_over.wav: " + e.getMessage());
                }
                
                app.captureLastMusicState();
                freezeActiveObjects();
                showGameOver();
            }
        }
    }

    private void freezeActiveObjects() {
        var balls = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.pauseForCountdown();
            }
        }

        Entity paddleEntity = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.PADDLE).stream()
            .findFirst().orElse(null);
        if (paddleEntity != null) {
            com.arcadeblocks.gameplay.Paddle paddleComponent = paddleEntity.getComponent(com.arcadeblocks.gameplay.Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setMoveLeft(false);
                paddleComponent.setMoveRight(false);
                paddleComponent.setMovementBlocked(true);
            }
            com.almasb.fxgl.physics.PhysicsComponent physics = paddleEntity.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
            if (physics != null) {
                physics.setLinearVelocity(0, 0);
            }
        }
        
    }
    

    
    private void showGameOver() {
        app.setGameOver(true);
        // System.out.println("Game Over! Все жизни израсходованы.");
        
        if (app.getScoreManager() != null) {
            app.getScoreManager().flushPendingOperations();
            app.getScoreManager().setPersistenceEnabled(false);
        }

        if (app.getSaveManager() != null) {
            // int slotToClear = app.getOriginalSaveSlot();
            // app.getSaveManager().deleteSaveFileForSlot(slotToClear);
            // System.out.println("🗑️ Сохранение активного слота очищено при Game Over");
        }

        // Сбрасываем флаги нажатия клавиш для ракетки и блокируем движение
        app.resetPaddleInputFlags();
        Entity paddle = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.PADDLE).stream()
            .findFirst().orElse(null);
        if (paddle != null) {
            com.arcadeblocks.gameplay.Paddle paddleComponent = paddle.getComponent(com.arcadeblocks.gameplay.Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setMoveLeft(false);
                paddleComponent.setMoveRight(false);
                paddleComponent.setMovementBlocked(true);
            }
        }

        java.util.concurrent.atomic.AtomicInteger pendingFades = new java.util.concurrent.atomic.AtomicInteger(2);

        Runnable finalizeGameOver = () -> {
            if (pendingFades.decrementAndGet() != 0) {
                return;
            }

            var balls = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.BALL);
            for (Entity ball : balls) {
                ball.removeFromWorld();
            }

            if (app.getGameplayUIView() != null) {
                app.getGameplayUIView().playGameOverVisuals();
            }

            int continueCost = (app.getContinueCount() + 1) * 10000;
            boolean isHardcore = app.getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE;
            int currentScore = app.getScoreManager() != null ? app.getScoreManager().getCurrentScore() : 0;
            boolean canContinue = !isHardcore && currentScore >= continueCost;

            com.arcadeblocks.ui.GameOverView gameOverView = new com.arcadeblocks.ui.GameOverView(app, canContinue, continueCost);
            FXGL.getGameScene().addUINode(gameOverView);
            // КРИТИЧНО: Устанавливаем флаг для оптимизации onUpdate
            app.setGameOverViewVisible(true);
            try {
                java.lang.reflect.Method uninstall = com.arcadeblocks.ArcadeBlocksApp.class.getDeclaredMethod("uninstallMousePaddleControlHandlers");
                uninstall.setAccessible(true);
                uninstall.invoke(app);
                java.lang.reflect.Method showCursor = com.arcadeblocks.ArcadeBlocksApp.class.getDeclaredMethod("setSystemCursor");
                showCursor.setAccessible(true);
                showCursor.invoke(app);
            } catch (Exception ignored) {}

            if (app.getAudioManager() != null) {
                app.getAudioManager().stopMusic();
                app.getAudioManager().playMusic("music/game_over.mp3", false);
            }
            lifeLossInProgress = false;
        };

        app.fadeOutBonuses(false, () -> {
            if (app.getBonusEffectManager() != null) {
                app.getBonusEffectManager().clearAllBonuses();
                app.getBonusEffectManager().resetAllEffects();
            }
            finalizeGameOver.run();
        });

        app.fadeOutPaddleAndBalls(false, finalizeGameOver);
    }
    
    public int getCurrentLives() {
        return currentLives;
    }
    
    public boolean hasLivesLeft() {
        return currentLives > 0;
    }
    
    public boolean isLifeLossInProgress() {
        return lifeLossInProgress;
    }
    
    public void resetLives() {
        // Получаем количество жизней из настроек сложности
        com.arcadeblocks.config.DifficultyLevel difficulty = app.getEffectiveDifficulty();
        currentLives = difficulty.getLives();
        
        // Обновляем новый индикатор жизней
        if (livesIndicator != null) {
            livesIndicator.resetLives();
        }
        
        // Восстанавливаем все иконки жизней (старый код для совместимости)
        for (ImageView lifeIcon : lifeIcons) {
            lifeIcon.setOpacity(1.0);
        }

        persistLivesChange(true);
    }

    public void setCurrentLivesFromSnapshot(int lives) {
        // Защита от установки 0 жизней - это может привести к багам
        if (lives <= 0) {
            // System.out.println("⚠️ Попытка установить 0 жизней - используем значение по умолчанию");
            com.arcadeblocks.config.DifficultyLevel difficulty = app.getEffectiveDifficulty();
            lives = difficulty.getLives();
        }
        
        currentLives = Math.max(1, lives); // Минимум 1 жизнь, но без ограничения максимума

        if (livesIndicator != null) {
            livesIndicator.setLives(currentLives);
        }

        // Обновляем старые иконки жизней (для совместимости)
        for (int i = 0; i < lifeIcons.length; i++) {
            ImageView lifeIcon = lifeIcons[i];
            if (lifeIcon != null) {
                lifeIcon.setOpacity(i < currentLives ? 1.0 : 0.3);
            }
        }

        persistLivesChange(false);
    }

    public void addLife() {
        // Убираем ограничение на максимальное количество жизней
        // Бонус дополнительной жизни может давать жизни сверх лимита сложности
        currentLives++;
        
        // Обновляем новый индикатор жизней
        if (livesIndicator != null) {
            livesIndicator.addLife();
        }
        
        // Анимация добавления жизни (старый код для совместимости)
        // Проверяем, есть ли иконка для этой жизни
        if (currentLives <= lifeIcons.length) {
            ImageView newLife = lifeIcons[currentLives - 1];
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newLife);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
        
        // System.out.println("Добавлена жизнь! Текущее количество: " + currentLives);

        persistLivesChange(true);
    }
    
    /**
     * Установить количество жизней (для debug режима)
     */
    public void setLives(int lives) {
        currentLives = Math.max(1, lives);
        
        // Обновляем новый индикатор жизней
        if (livesIndicator != null) {
            livesIndicator.setLives(currentLives);
        }
        
        // Обновляем старые иконки жизней (для совместимости)
        for (int i = 0; i < lifeIcons.length; i++) {
            ImageView lifeIcon = lifeIcons[i];
            if (lifeIcon != null) {
                lifeIcon.setOpacity(i < currentLives ? 1.0 : 0.3);
            }
        }
        
        persistLivesChange(true);
    }
    
    public void removeFromScene() {
        // Удаляем новый индикатор жизней
        if (livesIndicator != null && livesIndicator.getParent() != null) {
            FXGL.getGameScene().removeUINode(livesIndicator);
        }
        
        // Удаляем старый индикатор жизней
        if (livesContainer != null && livesContainer.getParent() != null) {
            FXGL.getGameScene().removeUINode(livesContainer);
        }
    }

    private void persistLivesChange(boolean saveToSlot) {
        if (app.getSaveManager() != null && !app.isDebugMode()) {
            // Жизни записываются в БД асинхронно через setLives()
            app.getSaveManager().setLives(currentLives);
            
            // Автосохранение слотов коалесцируется через FXGL.runOnce и никогда не блокирует UI поток
            if (saveToSlot && !autosaveScheduled) {
                autosaveScheduled = true;
                FXGL.runOnce(() -> {
                    if (app.getSaveManager() != null) {
                        app.getSaveManager().autoSaveToActiveSlot();
                    }
                    autosaveScheduled = false;
                }, LIVES_AUTOSAVE_DELAY);
            }
        }

        if (app.isDebugMode()) {
            app.setDebugLivesOverride(currentLives);
        }

        FXGL.set("lives", currentLives);
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateLives(currentLives);
        }
    }
    
    /**
     * Получить новый индикатор жизней
     */
    public LivesIndicator getLivesIndicator() {
        return livesIndicator;
    }
    
    /**
     * Очистка ресурсов для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем активную FadeTransition перед очисткой
        if (activeLifeLossFadeTransition != null) {
            try {
                activeLifeLossFadeTransition.stop();
            } catch (Exception e) {
                // Игнорируем ошибки остановки
            }
            activeLifeLossFadeTransition = null;
        }
        
        // КРИТИЧНО: Очищаем LivesIndicator перед обнулением ссылки
        if (livesIndicator != null) {
            livesIndicator.cleanup();
            livesIndicator = null;
        }
        
        // КРИТИЧНО: Очищаем ссылку на app для предотвращения циклических зависимостей
        app = null;
    }
}
