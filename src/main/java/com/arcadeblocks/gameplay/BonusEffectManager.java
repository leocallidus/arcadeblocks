package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.EntityType;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.AudioConfig;
import com.arcadeblocks.config.BonusConfig;
import com.arcadeblocks.persistence.GameSnapshot;
import javafx.util.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер эффектов бонусов
 */
public class BonusEffectManager {
    
    private ArcadeBlocksApp app;
    
    // Состояние магнитных эффектов
    private boolean bonusMagnetActive = false;
    private boolean penaltyMagnetActive = false;
    private double magnetRadius = 150.0; // Радиус действия магнита
    private double magnetForce = 3.0; // Сила притяжения
    
    // Состояние плазменного оружия
    private int plasmaShotsRemaining = 0;
    private boolean plasmaWeaponActive = false;
    private boolean frozenPaddleTextureActive = false;
    private boolean stickyPaddleTextureActive = false;
    private static final String STICKY_PLASMA_PADDLE_TEXTURE = "slime_pw_paddle.png";
    private boolean stickyPaddleActive = false;
    private int increasePaddleStacks = 0;
    private static final String FROZEN_PADDLE_TEXTURE = "freeze_paddle.png";
    private static final String STICKY_PADDLE_TEXTURE = "slime_paddle.png";
    private static final String PLASMA_PADDLE_TEXTURE = "pw_paddle.png";
    private static final int INCREASE_PADDLE_BASE_DURATION = 30;
    private static final int INCREASE_PADDLE_MAX_STACKS = 6;
    private static final double INCREASE_PADDLE_FIRST_STACK_MULTIPLIER = 1.5;
    private static final double INCREASE_PADDLE_MAX_MULTIPLIER = GameConfig.PADDLE_MAX_SIZE_MULTIPLIER;
    private static final double INCREASE_PADDLE_STACK_INCREMENT =
        INCREASE_PADDLE_MAX_STACKS > 1
            ? (INCREASE_PADDLE_MAX_MULTIPLIER - INCREASE_PADDLE_FIRST_STACK_MULTIPLIER) / (INCREASE_PADDLE_MAX_STACKS - 1)
            : 0.0;
    private static final int STICKY_PADDLE_BASE_DURATION = 20;
    private static final int BONUS_WALL_BASE_DURATION = 10;
    private static final int SCORE_RAIN_BASE_DURATION = 20;
    private static final int DARKNESS_BASE_DURATION = 15;
    private static final int CHAOTIC_BALLS_BASE_DURATION = 15;
    private static final int FROZEN_PADDLE_BASE_DURATION = 3;
    private static final int DECREASE_PADDLE_BASE_DURATION = 20;
    private static final int FAST_BALLS_BASE_DURATION = 15;
    private static final int PENALTIES_MAGNET_BASE_DURATION = 20;
    private static final int WEAK_BALLS_BASE_DURATION = 15;
    private static final int INVISIBLE_PADDLE_BASE_DURATION = 5;
    
    // Защита от спама плазменных выстрелов
    private long lastPlasmaShotTime = 0;
    private static final long PLASMA_SHOT_COOLDOWN_MS = 200; // 200мс между выстрелами
    
    // Состояние хаотичных мячей
    private boolean chaoticBallsActive = false;
    
    // Состояние слабых мячей
    private boolean weakBallsActive = false;

    // Состояние дождя очков
    private boolean scoreRainActive = false;
    public static final int SCORE_RAIN_BONUS_POINTS = 1000;

    // Состояние бонуса темноты
    private boolean darknessActive = false;
    private static final EnumSet<EntityType> DARKNESS_VISIBLE_TYPES = EnumSet.of(EntityType.PADDLE, EntityType.BALL, EntityType.BONUS);
    
    // Состояние замороженной ракетки (не только текстура, но и сам эффект)
    private boolean frozenPaddleActive = false;
    
    // Состояние призрачной ракетки
    private boolean invisiblePaddleActive = false;
    
    // Точная система отслеживания таймеров бонусов (в миллисекундах)
    private ConcurrentHashMap<BonusType, Long> bonusEndTimes = new ConcurrentHashMap<>();
    
    // Кэш последнего отображаемого времени для оптимизации обновлений UI
    private ConcurrentHashMap<BonusType, Integer> lastDisplayedSeconds = new ConcurrentHashMap<>();
    
    // Приоритет отображения бонусов (для ScoreManager)
    private ConcurrentHashMap<BonusType, Integer> bonusPriorities = new ConcurrentHashMap<>();
    
    // Система паузы таймеров
    private ConcurrentHashMap<BonusType, Long> pausedBonusTimes = new ConcurrentHashMap<>();
    private boolean timersPaused = false;
    
    public BonusEffectManager(ArcadeBlocksApp app) {
        this.app = app;
    }
    
    /**
     * Обновление таймеров бонусов - вызывается каждый кадр для максимальной точности
     */
    public void update(double tpf) {
        long now = System.currentTimeMillis();

        if (darknessActive) {
            enforceDarknessVisibility();
        }
        
        // Создаём копию ключей для безопасной итерации
        bonusEndTimes.keySet().forEach(type -> {
            Long endTime = bonusEndTimes.get(type);
            if (endTime == null) {
                return; // Бонус был удалён в другом потоке
            }
            
            long remainingMillis = endTime - now;
            
            // Если время истекло
            if (remainingMillis <= 0) {
                deactivateBonus(type);
                removeBonusTimer(type);
            } else {
                // Вычисляем оставшиеся секунды с округлением вверх
                int remainingSeconds = (int) Math.ceil(remainingMillis / 1000.0);
                
                // Обновляем UI только если изменилось количество секунд
                Integer lastDisplayed = lastDisplayedSeconds.get(type);
                if (lastDisplayed == null || remainingSeconds != lastDisplayed) {
                    lastDisplayedSeconds.put(type, remainingSeconds);
                    updateBonusUI(type, remainingSeconds);
                }
            }
        });
    }
    
    /**
     * Пересчитать размер ракетки с учетом текущего количества стаков бонуса увеличения
     */
    private void applyIncreasePaddleSize(Paddle paddleComponent) {
        if (paddleComponent == null) {
            return;
        }

        if (increasePaddleStacks <= 0) {
            paddleComponent.setSizeMultiplier(1.0);
            return;
        }

        double multiplier = INCREASE_PADDLE_FIRST_STACK_MULTIPLIER;
        if (INCREASE_PADDLE_MAX_STACKS > 1) {
            double extraStacks = Math.max(0, increasePaddleStacks - 1);
            multiplier = INCREASE_PADDLE_FIRST_STACK_MULTIPLIER + INCREASE_PADDLE_STACK_INCREMENT * extraStacks;
        }

        paddleComponent.setSizeMultiplier(Math.min(INCREASE_PADDLE_MAX_MULTIPLIER, multiplier));
    }
    
    /**
     * Обновление UI для отображения таймера бонуса
     */
    private void updateBonusUI(BonusType bonusType, int remainingSeconds) {
        // Обновляем индикатор под счетом
        if (app.getScoreManager() != null) {
            app.getScoreManager().updateBonusTimer(bonusType, remainingSeconds);
        }
    }
    
    /**
     * Запустить таймер для бонуса с точным отсчетом времени
     */
    private void startBonusTimer(BonusType bonusType, int durationSeconds) {
        // System.out.println("⏰ startBonusTimer: " + bonusType + " на " + durationSeconds + " секунд");
        
        // Вычисляем точное время окончания в миллисекундах
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        
        // Если бонус уже активен, обновляем время окончания
        Long existingEndTime = bonusEndTimes.get(bonusType);
        if (existingEndTime != null) {
            // System.out.println("🔄 Продление существующего таймера для " + bonusType);
            // Продлеваем время: берём максимум из текущего оставшегося времени и нового
            long newEndTime = Math.max(endTime, existingEndTime);
            bonusEndTimes.put(bonusType, newEndTime);
        } else {
            // Новый бонус
            bonusEndTimes.put(bonusType, endTime);
        }
        
        // Инициализируем отображение
        lastDisplayedSeconds.put(bonusType, durationSeconds);
        
        // Показываем индикатор бонуса под счетом
        if (app.getScoreManager() != null) {
            app.getScoreManager().showBonus(bonusType, durationSeconds);
        }
    }
    
    /**
     * Удалить таймер бонуса
     */
    private void removeBonusTimer(BonusType bonusType) {
        bonusEndTimes.remove(bonusType);
        lastDisplayedSeconds.remove(bonusType);
        bonusPriorities.remove(bonusType);
        
        if (app.getScoreManager() != null) {
            app.getScoreManager().hideBonus(bonusType);
        }
    }

    private void deactivateBonus(BonusType bonusType) {
        switch (bonusType) {
            case INCREASE_PADDLE:
                increasePaddleStacks = 0;
                var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setSizeMultiplier(1.0);
                    }
                }
                break;
            case STICKY_PADDLE:
                stickyPaddleActive = false;
                var allBalls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ballEntity : allBalls) {
                    Ball ballComp = ballEntity.getComponent(Ball.class);
                    if (ballComp != null) {
                        ballComp.setStickyEnabled(false);
                        if (ballComp.isAttachedToPaddle()) {
                            ballComp.launchBall();
                        }
                    }
                }
                com.arcadeblocks.gameplay.Ball.clearAttachedBalls();
                stickyPaddleTextureActive = false;
                updatePaddleTexture();
                break;
            case SLOW_BALLS:
                var ballsAfter = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : ballsAfter) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setSpeedMultiplier(1.0);
                    }
                }
                break;
            case ENERGY_BALLS:
                ballsAfter = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : ballsAfter) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setEnergyBall(false);
                    }
                }
                break;
            case BONUS_WALL:
                var walls = FXGL.getGameWorld().getEntitiesByType(EntityType.WALL);
                for (Entity wall : walls) {
                    // Проверяем, существует ли свойство перед чтением
                    if (wall.getProperties().exists("isProtectiveWall") && wall.getBoolean("isProtectiveWall")) {
                        wall.removeFromWorld();
                    }
                }
                break;
            case BONUS_MAGNET:
                bonusMagnetActive = false;
                break;
            case PENALTIES_MAGNET:
                penaltyMagnetActive = false;
                break;
            case EXPLOSION_BALLS:
                ballsAfter = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : ballsAfter) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setExplosionBall(false);
                    }
                }
                break;
            case CHAOTIC_BALLS:
                chaoticBallsActive = false;
                ballsAfter = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : ballsAfter) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setChaoticBall(false);
                    }
                }
                break;
            case FROZEN_PADDLE:
                paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setMovementBlocked(false);
                        frozenPaddleActive = false;
                        frozenPaddleTextureActive = false;
                        updatePaddleTexture();
                        // Разблокируем управление мышью при деактивации бонуса
                        app.unblockMouseClicks();
                    }
                }
                break;
            case DECREASE_PADDLE:
                paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setSizeMultiplier(1.0);
                    }
                }
                break;
            case FAST_BALLS:
                ballsAfter = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : ballsAfter) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setSpeedMultiplier(1.0);
                    }
                }
                break;
            case WEAK_BALLS:
                weakBallsActive = false;
                ballsAfter = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : ballsAfter) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setWeakBall(false);
                    }
                }
                break;
            case INVISIBLE_PADDLE:
                paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setInvisible(false);
                        invisiblePaddleActive = false;
                    }
                }
                break;
            case DARKNESS:
                setDarknessActive(false);
                break;
            case SCORE_RAIN:
                scoreRainActive = false;
                break;
            default:
                break;
        }
    }

    private void updatePaddleTexture() {
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (paddles.isEmpty()) {
            return;
        }
        Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
        if (paddleComponent == null) {
            return;
        }

        String currentTexture = paddleComponent.getCurrentTextureName();
        String desiredTexture = null;
        boolean stickyActive = stickyPaddleTextureActive || stickyPaddleActive;

        if (frozenPaddleTextureActive) {
            desiredTexture = FROZEN_PADDLE_TEXTURE;
        } else if (plasmaWeaponActive && (stickyPaddleTextureActive || stickyPaddleActive)) {
            desiredTexture = STICKY_PLASMA_PADDLE_TEXTURE;
        } else if (plasmaWeaponActive) {
            desiredTexture = PLASMA_PADDLE_TEXTURE;
        } else if (stickyPaddleTextureActive || stickyPaddleActive) {
            desiredTexture = STICKY_PADDLE_TEXTURE;
        }

        if (desiredTexture == null) {
            if (!paddleComponent.isUsingDefaultTexture()) {
                paddleComponent.resetTexture();
            }
        } else if (!desiredTexture.equals(currentTexture)) {
            paddleComponent.setTexture(desiredTexture);
        }
    }

    public void setPlasmaWeaponActive(boolean active) {
        plasmaWeaponActive = active;
        updatePaddleTexture();
    }

    public void setStickyPaddleTextureActive(boolean active) {
        stickyPaddleTextureActive = active;
        stickyPaddleActive = active;
        updatePaddleTexture();
    }

    private void setDarknessActive(boolean active) {
        if (darknessActive == active) {
            if (active) {
                enforceDarknessVisibility();
            }
            return;
        }

        darknessActive = active;

        if (active) {
            if (app != null) {
                app.enableDarknessOverlay();
            }
            enforceDarknessVisibility();
        } else {
            restoreDarknessVisibility();
            if (app != null) {
                app.disableDarknessOverlay();
            }
        }
    }

    private void enforceDarknessVisibility() {
        var entities = FXGL.getGameWorld().getEntities();
        for (Entity entity : entities) {
            Object rawType = entity.getType();
            if (!(rawType instanceof EntityType entityType)) {
                continue;
            }
            boolean shouldStayVisible = isEntityTypeVisibleInDarkness(entityType);

            var viewComponent = entity.getViewComponent();
            if (viewComponent == null) {
                continue;
            }

            boolean alreadyHidden = false;
            if (entity.getProperties().exists("darknessHidden")) {
                try {
                    alreadyHidden = entity.getBoolean("darknessHidden");
                } catch (IllegalArgumentException ignored) {
                    alreadyHidden = false;
                }
            }

            if (shouldStayVisible) {
                if (alreadyHidden) {
                    viewComponent.setVisible(true);
                    entity.setProperty("darknessHidden", false);
                }
            } else if (!alreadyHidden) {
                viewComponent.setVisible(false);
                entity.setProperty("darknessHidden", true);
            }
        }
    }

    private void restoreDarknessVisibility() {
        var entities = FXGL.getGameWorld().getEntities();
        for (Entity entity : entities) {
            if (!entity.getProperties().exists("darknessHidden")) {
                continue;
            }

            boolean wasHidden = false;
            try {
                wasHidden = entity.getBoolean("darknessHidden");
            } catch (IllegalArgumentException ignored) {
                // Свойство существует, но не boolean — игнорируем
            }

            if (wasHidden) {
                var viewComponent = entity.getViewComponent();
                if (viewComponent != null) {
                    viewComponent.setVisible(true);
                }
            }

            entity.setProperty("darknessHidden", false);
        }
    }

    private boolean isEntityTypeVisibleInDarkness(EntityType type) {
        return DARKNESS_VISIBLE_TYPES.contains(type);
    }

    /**
     * Полностью отключить эффект темноты, даже если таймер ещё не истёк.
     * Используется при переходах между разделами игры.
     */
    public void forceDisableDarkness() {
        darknessActive = false;

        restoreDarknessVisibility();
        if (app != null) {
            app.disableDarknessOverlay();
        }

        bonusEndTimes.remove(BonusType.DARKNESS);
        lastDisplayedSeconds.remove(BonusType.DARKNESS);
        bonusPriorities.remove(BonusType.DARKNESS);
        pausedBonusTimes.remove(BonusType.DARKNESS);

        if (app != null) {
            if (app.getBonusTimerView() != null) {
                app.getBonusTimerView().removeBonus(BonusType.DARKNESS);
            }
            if (app.getScoreManager() != null) {
                app.getScoreManager().hideBonus(BonusType.DARKNESS);
            }
        }

        // Если бонус был активен, мы уже восстановили видимость и убрали оверлей выше.
    }
    
    /**
     * Получить оставшееся время бонуса в секундах
     */
    public int getBonusRemainingTime(BonusType bonusType) {
        Long endTime = bonusEndTimes.get(bonusType);
        if (endTime == null) {
            return 0;
        }
        
        long remainingMillis = endTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return 0;
        }
        
        return (int) Math.ceil(remainingMillis / 1000.0);
    }
    
    /**
     * Получить точное оставшееся время бонуса в миллисекундах
     */
    public long getBonusRemainingMillis(BonusType bonusType) {
        Long endTime = bonusEndTimes.get(bonusType);
        if (endTime == null) {
            return 0;
        }
        
        long remainingMillis = endTime - System.currentTimeMillis();
        return Math.max(0, remainingMillis);
    }

    public GameSnapshot.BonusEffectsState createSnapshot() {
        GameSnapshot.BonusEffectsState state = new GameSnapshot.BonusEffectsState();
        state.bonusMagnetActive = bonusMagnetActive;
        state.penaltyMagnetActive = penaltyMagnetActive;
        state.magnetRadius = magnetRadius;
        state.magnetForce = magnetForce;
        state.plasmaWeaponActive = plasmaWeaponActive;
        state.plasmaShotsRemaining = plasmaShotsRemaining;
        state.chaoticBallsActive = chaoticBallsActive;
        state.frozenPaddleTextureActive = frozenPaddleTextureActive;
        state.stickyPaddleTextureActive = stickyPaddleTextureActive;
        state.darknessActive = darknessActive;
        state.increasePaddleStacks = increasePaddleStacks;
        
        // Сохраняем оставшееся время для каждого активного бонуса
        bonusEndTimes.forEach((type, endTime) -> {
            int remainingSeconds = getBonusRemainingTime(type);
            if (remainingSeconds > 0) {
                state.timers.put(type.name(), remainingSeconds);
            }
        });
        
        return state;
    }

    public void restoreFromSnapshot(GameSnapshot.BonusEffectsState snapshot) {
        resetAllEffects();

        if (snapshot == null) {
            return;
        }

        increasePaddleStacks = Math.min(
            Math.max(snapshot.increasePaddleStacks, 0),
            INCREASE_PADDLE_MAX_STACKS
        );

        if (snapshot.magnetRadius > 0) {
            magnetRadius = snapshot.magnetRadius;
        }
        if (snapshot.magnetForce > 0) {
            magnetForce = snapshot.magnetForce;
        }

        plasmaWeaponActive = snapshot.plasmaWeaponActive && snapshot.plasmaShotsRemaining > 0;
        plasmaShotsRemaining = snapshot.plasmaShotsRemaining;
        if (plasmaWeaponActive && app.getScoreManager() != null) {
            app.getScoreManager().showBonus(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
            app.getScoreManager().updateBonusTimer(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
        }
        frozenPaddleTextureActive = snapshot.frozenPaddleTextureActive;
        // Если текстура замороженной ракетки активна, значит и сам эффект активен
        frozenPaddleActive = snapshot.frozenPaddleTextureActive;
        stickyPaddleTextureActive = snapshot.stickyPaddleTextureActive;
        updatePaddleTexture();

        if (snapshot.timers != null) {
            for (Map.Entry<String, Integer> entry : snapshot.timers.entrySet()) {
                int remaining = entry.getValue() != null ? entry.getValue() : 0;
                if (remaining <= 0) {
                    continue;
                }
                try {
                    BonusType type = BonusType.valueOf(entry.getKey());
                    reactivateBonus(type, remaining);
                } catch (IllegalArgumentException ignored) {
                    // неизвестный тип бонуса - игнорируем
                }
            }
        }

        // Чаотичные мячи могли быть активны без таймера (на всякий случай)
        chaoticBallsActive = snapshot.chaoticBallsActive;
        if (chaoticBallsActive) {
            var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
            for (Entity ball : balls) {
                Ball comp = ball.getComponent(Ball.class);
                if (comp != null) {
                    comp.setChaoticBall(true);
                }
            }
        }

        if (snapshot.darknessActive && !darknessActive) {
            int remaining = 0;
            if (snapshot.timers != null) {
                Integer saved = snapshot.timers.get(BonusType.DARKNESS.name());
                if (saved != null) {
                    remaining = saved;
                }
            }
            setDarknessActive(true);
            if (remaining > 0) {
                startBonusTimer(BonusType.DARKNESS, remaining);
            }
        }
    }

    private void reactivateBonus(BonusType bonusType, int remainingSeconds) {
        int timerValue = Math.max(remainingSeconds, 1);

        switch (bonusType) {
            case INCREASE_PADDLE -> {
                var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        if (increasePaddleStacks <= 0) {
                            increasePaddleStacks = 1;
                        }
                        applyIncreasePaddleSize(paddleComponent);
                        startBonusTimer(bonusType, timerValue + 1);
                    }
                }
            }
            case STICKY_PADDLE -> {
                stickyPaddleActive = true;
                stickyPaddleTextureActive = true;
                updatePaddleTexture();

                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity entityBall : balls) {
                    Ball ballComponent = entityBall.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setStickyEnabled(true);
                    }
                }

                startBonusTimer(bonusType, timerValue);

                FXGL.runOnce(() -> {
                    for (Entity stickyBall : FXGL.getGameWorld().getEntitiesByType(EntityType.BALL)) {
                        Ball component = stickyBall.getComponent(Ball.class);
                        if (component != null) {
                            component.setStickyEnabled(false);
                        }
                    }
                    stickyPaddleActive = false;
                    stickyPaddleTextureActive = false;
                    updatePaddleTexture();
                }, Duration.seconds(timerValue));
            }
            case SLOW_BALLS -> {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setSpeedMultiplier(0.7);
                    }
                }
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case ENERGY_BALLS -> {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setEnergyBall(true);
                    }
                }
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case BONUS_WALL -> {
                double wallX = 0;
                double wallY = GameConfig.GAME_HEIGHT - 10;
                double wallWidth = GameConfig.GAME_WIDTH;
                double wallHeight = 20;

                SpawnData wallData = new SpawnData(wallX, wallY);
                wallData.put("width", wallWidth);
                wallData.put("height", wallHeight);
                wallData.put("isProtectiveWall", true);

                Entity wall = FXGL.spawn("wall", wallData);
                wall.setProperty("isProtectiveWall", true);
                wall.setProperty("lifetime", (double) timerValue);

                startBonusTimer(bonusType, timerValue + 1);
                FXGL.runOnce(() -> {
                    if (wall != null && wall.isActive()) {
                        wall.removeFromWorld();
                    }
                }, Duration.seconds(timerValue));
            }
            case BONUS_MAGNET -> {
                bonusMagnetActive = true;
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case PENALTIES_MAGNET -> {
                penaltyMagnetActive = true;
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case BONUS_BALL -> {
                // Дополнительный мяч уже сохранен в snapshot в виде отдельного BallState
            }
            case PLASMA_WEAPON -> {
                plasmaWeaponActive = true;
                plasmaShotsRemaining = Math.max(plasmaShotsRemaining, timerValue);
                if (app.getScoreManager() != null) {
                    app.getScoreManager().showBonus(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
                    app.getScoreManager().updateBonusTimer(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
                }
                updatePaddleTexture();
            }
            case EXPLOSION_BALLS -> {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setExplosionBall(true);
                    }
                }
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case SCORE_RAIN -> {
                scoreRainActive = true;
                startBonusTimer(bonusType, timerValue);
            }
            case CHAOTIC_BALLS -> {
                chaoticBallsActive = true;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setChaoticBall(true);
                    }
                }
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case FROZEN_PADDLE -> {
                var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setMovementBlocked(true);
                        frozenPaddleActive = true;
                        startBonusTimer(bonusType, timerValue + 1);
                        frozenPaddleTextureActive = true;
                        updatePaddleTexture();
                        FXGL.runOnce(() -> {
                            paddleComponent.setMovementBlocked(false);
                            frozenPaddleActive = false;
                            frozenPaddleTextureActive = false;
                            updatePaddleTexture();
                        }, Duration.seconds(timerValue));
                    }
                }
            }
            case DECREASE_PADDLE -> {
                var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setSizeMultiplier(0.6);
                        startBonusTimer(bonusType, timerValue + 1);
                        FXGL.runOnce(() -> paddleComponent.setSizeMultiplier(1.0), Duration.seconds(timerValue));
                    }
                }
            }
            case FAST_BALLS -> {
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setSpeedMultiplier(1.5);
                    }
                }
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case WEAK_BALLS -> {
                weakBallsActive = true;
                var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
                for (Entity ball : balls) {
                    Ball ballComponent = ball.getComponent(Ball.class);
                    if (ballComponent != null) {
                        ballComponent.setWeakBall(true);
                    }
                }
                startBonusTimer(bonusType, timerValue);
                // Деактивация через update() -> deactivateBonus()
            }
            case INVISIBLE_PADDLE -> {
                var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Paddle paddleComponent = paddles.get(0).getComponent(Paddle.class);
                    if (paddleComponent != null) {
                        paddleComponent.setInvisible(true);
                        invisiblePaddleActive = true;
                        startBonusTimer(bonusType, timerValue + 1);
                        FXGL.runOnce(() -> {
                            paddleComponent.setInvisible(false);
                            invisiblePaddleActive = false;
                        }, Duration.seconds(timerValue));
                    }
                }
            }
            case DARKNESS -> {
                setDarknessActive(true);
                startBonusTimer(bonusType, timerValue);
            }
            default -> {
                // Остальные бонусы либо мгновенные, либо обрабатываются отдельно
            }
        }
    }
    
    /**
     * Воспроизвести звук активации бонуса
     */
    private void playBonusSound(BonusType bonusType) {
        try {
            String soundKey = bonusType.name().toLowerCase();
            String soundPath = com.arcadeblocks.config.AudioConfig.getSFX(soundKey);
            if (soundPath != null) {
                app.getAudioManager().playSFX(soundPath);
        // System.out.println("🔊 Воспроизведен звук бонуса: " + soundKey);
            }
        } catch (Exception e) {
        // System.out.println("❌ Ошибка воспроизведения звука бонуса " + bonusType + ": " + e.getMessage());
        }
    }
    
    
    /**
     * Применить эффект бонуса
     */
    public void applyBonusEffect(BonusType bonusType) {
        // System.out.println("Применяется эффект бонуса: " + bonusType.getDescription());
        
        switch (bonusType) {
            case BONUS_SCORE:
                applyBonusScore();
                break;
            case EXTRA_LIFE:
                applyExtraLife();
                break;
            case INCREASE_PADDLE:
                applyIncreasePaddle();
                break;
            case STICKY_PADDLE:
                applyStickyPaddle();
                break;
            case SLOW_BALLS:
                applySlowBalls();
                break;
            case ENERGY_BALLS:
                applyEnergyBalls();
                break;
            case BONUS_WALL:
                applyBonusWall();
                break;
            case BONUS_MAGNET:
                applyBonusMagnet();
                break;
            case BONUS_BALL:
                applyBonusBall();
                break;
            case PLASMA_WEAPON:
                applyPlasmaWeapon();
                break;
            case EXPLOSION_BALLS:
                applyExplosionBalls();
                break;
            case LEVEL_PASS:
                applyLevelPassBonus();
                break;
            case SCORE_RAIN:
                applyScoreRain();
                break;
                
            // Негативные эффекты
            case CHAOTIC_BALLS:
                applyChaoticBalls();
                break;
            case FROZEN_PADDLE:
                applyFrozenPaddle();
                break;
            case DECREASE_PADDLE:
                applyDecreasePaddle();
                break;
            case FAST_BALLS:
                applyFastBalls();
                break;
            case PENALTIES_MAGNET:
                applyPenaltiesMagnet();
                break;
            case WEAK_BALLS:
                applyWeakBalls();
                break;
            case INVISIBLE_PADDLE:
                applyInvisiblePaddle();
                break;
            case DARKNESS:
                applyDarkness();
                break;
            case RESET:
                applyResetBonus();
                break;
            case BAD_LUCK:
                applyBadLuck();
                break;
            case TRICKSTER:
                applyTrickster();
                break;
            case RANDOM_BONUS:
                applyRandomBonus();
                break;
        }
    }
    
    // ========== ПОЗИТИВНЫЕ ЭФФЕКТЫ ==========
    
    private void applyBonusScore() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.BONUS_SCORE);
        
        // Добавляем 1000 очков
        if (app.getScoreManager() != null) {
            app.getScoreManager().addScore(1000);
        // System.out.println("+1000 очков!");
        }
    }
    
    private void applyExtraLife() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.EXTRA_LIFE);

        // В режиме хардкор вместо фактического добавления жизни запускаем "Дождь очков"
        if (app != null && app.getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE) {
            applyBonusEffect(BonusType.SCORE_RAIN);
            return;
        }

        // На остальных сложностях добавляем дополнительную жизнь
        if (app.getLivesManager() != null) {
            app.getLivesManager().addLife();
        // System.out.println("Дополнительная жизнь!");
        }
    }
    
    private void applyIncreasePaddle() {
        applyIncreasePaddle(INCREASE_PADDLE_BASE_DURATION);
    }

    private void applyIncreasePaddle(int durationSeconds) {
        playBonusSound(BonusType.INCREASE_PADDLE);
        
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (!paddles.isEmpty()) {
            Entity paddle = paddles.get(0);
            Paddle paddleComponent = paddle.getComponent(Paddle.class);
            if (paddleComponent != null) {
                if (increasePaddleStacks < INCREASE_PADDLE_MAX_STACKS) {
                    increasePaddleStacks++;
                }
                applyIncreasePaddleSize(paddleComponent);
                startBonusTimer(BonusType.INCREASE_PADDLE, durationSeconds);
            }
        }
    }
    
    private void applyStickyPaddle() {
        applyStickyPaddle(STICKY_PADDLE_BASE_DURATION);
    }

    private void applyStickyPaddle(int durationSeconds) {
        // Воспроизводим звук активации (всегда)
        playBonusSound(BonusType.STICKY_PADDLE);
        
        // Проверяем, не активна ли уже липкая ракетка
        if (stickyPaddleActive) {
            startBonusTimer(BonusType.STICKY_PADDLE, durationSeconds);
            return;
        }
        
        stickyPaddleActive = true;
        stickyPaddleTextureActive = true;
        updatePaddleTexture();
        
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setStickyEnabled(true);
            }
        }
        
        startBonusTimer(BonusType.STICKY_PADDLE, durationSeconds);
    }
    
    private void applySlowBalls() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.SLOW_BALLS);
        
        // Замедляем ВСЕ мячи на 30%
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setSpeedMultiplier(0.7); // Замедляем на 30%
            }
        }
        
        // Запускаем таймер (только один раз, вне цикла)
        startBonusTimer(BonusType.SLOW_BALLS, 20);
        // System.out.println("Мячи замедлены на 30% на 20 секунд!");
        
        // Деактивация произойдет автоматически через update() -> deactivateBonus()
    }
    
    private void applyEnergyBalls() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.ENERGY_BALLS);
        
        // Делаем ВСЕ мячи энергетическими (проходят сквозь кирпичи)
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        // System.out.println("🎯 Применяется эффект энергетических мячей. Найдено мячей: " + balls.size());
        
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setEnergyBall(true);
            }
        }
        
        // Запускаем таймер (только один раз, вне цикла)
        startBonusTimer(BonusType.ENERGY_BALLS, 5);
        // System.out.println("🕐 Мячи стали энергетическими на 5 секунд!");
        
        // Деактивация произойдет автоматически через update() -> deactivateBonus()
    }
    
    private void applyBonusWall() {
        applyBonusWall(BONUS_WALL_BASE_DURATION);
    }

    private void applyBonusWall(int durationSeconds) {
        // Воспроизводим звук активации
        playBonusSound(BonusType.BONUS_WALL);

        // Проверяем, существует ли уже защитный барьер
        boolean wallExists = !FXGL.getGameWorld().getEntitiesByType(EntityType.WALL)
                .stream()
                .filter(e -> e.getProperties().exists("isProtectiveWall") && e.getBoolean("isProtectiveWall"))
                .toList().isEmpty();

        if (!wallExists) {
            // Создаем защитный барьер у нижней границы поля, только если его еще нет
            double wallX = 0; // От левого края поля
            double wallY = GameConfig.GAME_HEIGHT - 10; // У нижней границы
            double wallWidth = GameConfig.GAME_WIDTH; // На всю ширину поля
            double wallHeight = 20; // Толщина стены

            com.almasb.fxgl.entity.SpawnData wallData = new com.almasb.fxgl.entity.SpawnData(wallX, wallY);
            wallData.put("width", wallWidth);
            wallData.put("height", wallHeight);
            wallData.put("isProtectiveWall", true); // Передаем информацию о том, что это дополнительная стена

            Entity wall = FXGL.spawn("wall", wallData);
            wall.setProperty("isProtectiveWall", true);
            wall.setProperty("lifetime", (double) durationSeconds);
        }

        // Всегда запускаем или продлеваем таймер
        startBonusTimer(BonusType.BONUS_WALL, durationSeconds);
    }
    
    private void applyBonusMagnet() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.BONUS_MAGNET);
        
        // Включаем магнит для позитивных бонусов
        bonusMagnetActive = true;
        
        // Запускаем таймер (только один раз)
        startBonusTimer(BonusType.BONUS_MAGNET, 20);
        // System.out.println("Магнит для бонусов включен на 20 секунд!");
        
        // Деактивация произойдет автоматически через update() -> deactivateBonus()
    }
    
    private void applyBonusBall() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.BONUS_BALL);
        
        // Создаем дополнительный мяч
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (!paddles.isEmpty()) {
            Entity paddle = paddles.get(0);
            double ballX = paddle.getX() + paddle.getWidth() / 2.0 - GameConfig.BALL_RADIUS;
            double ballY = paddle.getY() - GameConfig.BALL_RADIUS * 2 - 5;
            
            Entity ball = FXGL.spawn("ball", ballX, ballY);
            Ball ballComponent = new Ball();
            ball.addComponent(ballComponent);
            
            // Проверяем, активен ли бонус хаотичных мячей
            if (chaoticBallsActive) {
                ballComponent.setChaoticBall(true);
        // System.out.println("🌀 Дополнительный мяч стал хаотичным (активен бонус хаотичных мячей)");
            }
            
            // Проверяем, активен ли бонус слабых мячей
            if (weakBallsActive) {
                ballComponent.setWeakBall(true);
        // System.out.println("💪 Дополнительный мяч стал слабым (активен бонус слабых мячей)");
            }
            
            // Отмечаем мяч как дополнительный - он не будет тратить жизнь при падении
            ballComponent.setExtraBall(true);
            
            // Проверяем, активна ли липкая ракетка
            if (stickyPaddleActive) {
                ballComponent.setStickyEnabled(true);
                // System.out.println("🖱️ Дополнительный мяч получил липкость (активна липкая ракетка)");
                
                // Получаем следующее доступное смещение для прикрепления
                double[] offset = com.arcadeblocks.gameplay.Ball.getNextAttachedOffset();
                if (offset != null) {
                    // Устанавливаем смещение и прикрепляем мяч
                    ballComponent.setAttachedOffset(offset[0], offset[1]);
                    ballComponent.attachToPaddle(paddle);
                    
                    // Добавляем в список прикрепленных мячей
                    com.arcadeblocks.gameplay.Ball.addAttachedBall(ball);
                    // System.out.println("🖱️ Дополнительный мяч прикреплен к липкой ракетке (позиция " + com.arcadeblocks.gameplay.Ball.getAttachedBallsCount() + "/4)");
                } else {
                    // Если максимум достигнут, мяч отскакивает как обычно
                    // System.out.println("🖱️ Максимум прикрепленных мячей достигнут, дополнительный мяч запускается");
                    ballComponent.attachToPaddle(paddle);
                    ballComponent.launchBall();
                }
            } else {
                // Если липкая ракетка не активна, просто прикрепляем и сразу запускаем
                ballComponent.attachToPaddle(paddle);
                ballComponent.launchBall();
            }
            
        // System.out.println("Создан дополнительный мяч!");
        }
    }
    
    private void applyPlasmaWeapon() {
        // Если плазменное оружие уже активно, просто добавляем выстрелы
        if (plasmaWeaponActive) {
            boolean hadRemainingShots = plasmaShotsRemaining > 0;
            plasmaShotsRemaining += 10;
// System.out.println("Плазменное оружие пополнено! Осталось выстрелов: " + plasmaShotsRemaining);

            if (hadRemainingShots) {
                playPlasmaRechargeSound();
            }
            
            // Обновляем индикатор
            if (app.getScoreManager() != null) {
                app.getScoreManager().updateBonusTimer(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
            }
            updatePaddleTexture();
            return;
        }
        
        // Воспроизводим звук активации
        playBonusSound(BonusType.PLASMA_WEAPON);
        
        // Активируем плазменное оружие (10 выстрелов)
        plasmaShotsRemaining = 10;
        plasmaWeaponActive = true;
// System.out.println("Плазменное оружие активировано! Нажмите Z для выстрела. Осталось выстрелов: " + plasmaShotsRemaining);
        
        // Показываем индикатор плазменного оружия (показываем количество выстрелов как "время")
        if (app.getScoreManager() != null) {
            app.getScoreManager().showBonus(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
        }
        updatePaddleTexture();
        
        // Клавиши Z, X, C уже привязаны в ArcadeBlocksApp.initInput()
        // Здесь мы только активируем оружие
        // System.out.println("🔫 Плазменное оружие активировано! Используйте клавиши Z, X или C для выстрела.");
        // System.out.println("📊 Осталось выстрелов: " + plasmaShotsRemaining);
    }

    private void playPlasmaRechargeSound() {
        if (app == null || app.getAudioManager() == null) {
            return;
        }
        String sfx = AudioConfig.getSFX("plasma_weapon_recharge");
        if (sfx == null || sfx.isEmpty()) {
            return;
        }
        try {
            app.getAudioManager().playSFX(sfx);
        } catch (Exception ignored) {
            // Если не удалось воспроизвести звук, просто игнорируем ошибку
        }
    }
    
    /**
     * Выстрелить плазменным зарядом
     */
    public void firePlasmaShot() {
        if (!plasmaWeaponActive || plasmaShotsRemaining <= 0) {
            return;
        }
        
        // Запрещаем стрельбу при активной призрачной ракетке
        if (invisiblePaddleActive) {
            return;
        }
        
        // Проверяем кулдаун между выстрелами
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlasmaShotTime < PLASMA_SHOT_COOLDOWN_MS) {
            return; // Слишком быстро, игнорируем выстрел
        }
        lastPlasmaShotTime = currentTime;
        
        // Проверяем, нет ли уже слишком много снарядов (максимум 10)
        var existingProjectiles = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE);
        if (existingProjectiles.size() >= 10) {
        // System.out.println("Слишком много снарядов! Максимум 10. Ждите...");
            return;
        }
        
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (paddles.isEmpty()) {
            return;
        }
        
        Entity paddle = paddles.get(0);
        
        // Создаем плазменный заряд над ракеткой
        double shotX = paddle.getX() + paddle.getWidth() / 2.0 - 2; // Центрируем
        double shotY = paddle.getY() - 10; // Над ракеткой
        
        // Создаем данные для спавна
        com.almasb.fxgl.entity.SpawnData shotData = new com.almasb.fxgl.entity.SpawnData(shotX, shotY);
        shotData.put("isPlasma", true);
        
        // Создаем плазменный заряд
        Entity plasmaShot = FXGL.spawn("projectile", shotData);
        
        // КРИТИЧНО: Сохраняем только UUID для предотвращения утечки памяти
        // Прямая ссылка на entity в lambda может держать entity в памяти 15 секунд
        final int plasmaShotId = plasmaShot.hashCode();
        
        // Настраиваем плазменный заряд (скорость будет установлена в Projectile.onAdded())
        // Используем setOnPhysicsInitialized для безопасности
        com.almasb.fxgl.physics.PhysicsComponent shotPhysics = plasmaShot.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
        if (shotPhysics != null) {
            shotPhysics.setOnPhysicsInitialized(() -> {
                // КРИТИЧНО: Проверяем, что компонент еще существует перед использованием
                try {
                    shotPhysics.setLinearVelocity(0, -800); // Летит вверх со скоростью 800
                } catch (Exception e) {
                    // Игнорируем ошибки если физика уже очищена
                }
            });
        }
        
        // Удаляем заряд через 15 секунд (увеличено время жизни для большей дальности)
        // КРИТИЧНО: Ищем entity по hashCode вместо хранения прямой ссылки
        FXGL.runOnce(() -> {
            try {
                var projectiles = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE);
                for (Entity proj : projectiles) {
                    if (proj.hashCode() == plasmaShotId && proj.isActive()) {
                        proj.removeFromWorld();
                        break;
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибки при очистке
            }
        }, Duration.seconds(15));
        
        plasmaShotsRemaining--;
        // System.out.println("Плазменный выстрел! Осталось: " + plasmaShotsRemaining);
        
        // Обновляем индикатор
        if (app.getScoreManager() != null) {
            app.getScoreManager().updateBonusTimer(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
        }
        
        // Если выстрелы закончились, деактивируем оружие
        if (plasmaShotsRemaining <= 0) {
            plasmaWeaponActive = false;
// System.out.println("Плазменное оружие разряжено!");
            
            // Скрываем индикатор
            if (app.getScoreManager() != null) {
                app.getScoreManager().hideBonus(BonusType.PLASMA_WEAPON);
            }
            updatePaddleTexture();
        }
        
        // Воспроизводим звук выстрела
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/plasma_shot.wav");
        } catch (Exception e) {
            // Если звук не найден, игнорируем
        }
    }
    
    private void applyExplosionBalls() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.EXPLOSION_BALLS);
        
        // Делаем ВСЕ мячи взрывными
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setExplosionBall(true);
            }
        }
        
        // Запускаем таймер (только один раз, вне цикла)
        startBonusTimer(BonusType.EXPLOSION_BALLS, 5);
        // System.out.println("Мячи стали взрывными на 5 секунд!");
        
        // Деактивация произойдет автоматически через update() -> deactivateBonus()
    }
    
    // ========== НЕГАТИВНЫЕ ЭФФЕКТЫ ==========
    
    private void applyChaoticBalls() {
        applyChaoticBalls(CHAOTIC_BALLS_BASE_DURATION);
    }

    private void applyChaoticBalls(int durationSeconds) {
        playBonusSound(BonusType.CHAOTIC_BALLS);

        chaoticBallsActive = true;
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setChaoticBall(true);
            }
        }

        startBonusTimer(BonusType.CHAOTIC_BALLS, durationSeconds);
    }

    private void applyFrozenPaddle() {
        applyFrozenPaddle(FROZEN_PADDLE_BASE_DURATION);
    }

    private void applyFrozenPaddle(int durationSeconds) {
        playBonusSound(BonusType.FROZEN_PADDLE);

        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (!paddles.isEmpty()) {
            Entity paddle = paddles.get(0);
            Paddle paddleComponent = paddle.getComponent(Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setMovementBlocked(true);
                // Блокируем управление мышью при активации бонуса
                app.blockMouseClicks();

                startBonusTimer(BonusType.FROZEN_PADDLE, durationSeconds);
                frozenPaddleActive = true;
                frozenPaddleTextureActive = true;
                updatePaddleTexture();

                FXGL.runOnce(() -> {
                    paddleComponent.setMovementBlocked(false);
                    frozenPaddleActive = false;
                    frozenPaddleTextureActive = false;
                    updatePaddleTexture();
                    // Разблокируем управление мышью при деактивации бонуса
                    app.unblockMouseClicks();
                }, Duration.seconds(durationSeconds));
            }
        }
    }

    private void applyDecreasePaddle() {
        applyDecreasePaddle(DECREASE_PADDLE_BASE_DURATION);
    }

    private void applyDecreasePaddle(int durationSeconds) {
        playBonusSound(BonusType.DECREASE_PADDLE);

        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (!paddles.isEmpty()) {
            Entity paddle = paddles.get(0);
            Paddle paddleComponent = paddle.getComponent(Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setSizeMultiplier(0.6);

                startBonusTimer(BonusType.DECREASE_PADDLE, durationSeconds);

                FXGL.runOnce(() -> paddleComponent.setSizeMultiplier(1.0), Duration.seconds(durationSeconds));
            }
        }
    }

    private void applyFastBalls() {
        applyFastBalls(FAST_BALLS_BASE_DURATION);
    }

    private void applyFastBalls(int durationSeconds) {
        playBonusSound(BonusType.FAST_BALLS);

        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setSpeedMultiplier(1.5);
            }
        }

        startBonusTimer(BonusType.FAST_BALLS, durationSeconds);
    }

    private void applyPenaltiesMagnet() {
        applyPenaltiesMagnet(PENALTIES_MAGNET_BASE_DURATION);
    }

    private void applyPenaltiesMagnet(int durationSeconds) {
        playBonusSound(BonusType.PENALTIES_MAGNET);

        penaltyMagnetActive = true;

        startBonusTimer(BonusType.PENALTIES_MAGNET, durationSeconds);
    }

    private void applyWeakBalls() {
        applyWeakBalls(WEAK_BALLS_BASE_DURATION);
    }

    private void applyWeakBalls(int durationSeconds) {
        playBonusSound(BonusType.WEAK_BALLS);

        weakBallsActive = true;

        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
            if (ballComponent != null) {
                ballComponent.setWeakBall(true);
            }
        }

        startBonusTimer(BonusType.WEAK_BALLS, durationSeconds);
    }

    private void applyInvisiblePaddle() {
        applyInvisiblePaddle(INVISIBLE_PADDLE_BASE_DURATION);
    }

    private void applyInvisiblePaddle(int durationSeconds) {
        playBonusSound(BonusType.INVISIBLE_PADDLE);

        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        if (!paddles.isEmpty()) {
            Entity paddle = paddles.get(0);
            Paddle paddleComponent = paddle.getComponent(Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.setInvisible(true);
                invisiblePaddleActive = true;

                startBonusTimer(BonusType.INVISIBLE_PADDLE, durationSeconds);

                FXGL.runOnce(() -> {
                    paddleComponent.setInvisible(false);
                    invisiblePaddleActive = false;
                }, Duration.seconds(durationSeconds));
            }
        }
    }

    private void applyDarkness() {
        applyDarkness(DARKNESS_BASE_DURATION);
    }

    private void applyDarkness(int durationSeconds) {
        playBonusSound(BonusType.DARKNESS);
        setDarknessActive(true);
        startBonusTimer(BonusType.DARKNESS, durationSeconds);
    }

    private void applyScoreRain() {
        applyScoreRain(SCORE_RAIN_BASE_DURATION);
    }

    private void applyScoreRain(int durationSeconds) {
        playBonusSound(BonusType.SCORE_RAIN);
        scoreRainActive = true;
        startBonusTimer(BonusType.SCORE_RAIN, durationSeconds);
    }

    private void applyLevelPassBonus() {
        playBonusSound(BonusType.LEVEL_PASS);
        if (app != null) {
            app.completeLevelViaBonus();
        }
    }

    private void applyBadLuck() {
        playBonusSound(BonusType.BAD_LUCK);

        var activeBonuses = new java.util.ArrayList<>(bonusEndTimes.keySet());
        for (BonusType activeType : activeBonuses) {
            if (activeType.isPositive()) {
                deactivateBonus(activeType);
                removeBonusTimer(activeType);
            }
        }

        var fallingBonuses = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS);
        for (Entity entity : fallingBonuses) {
            Bonus bonusComponent = entity.getComponent(Bonus.class);
            if (bonusComponent != null) {
                BonusType type = bonusComponent.getBonusType();
                if (type != null && type.isPositive()) {
                    entity.removeFromWorld();
                }
            }
        }

        for (BonusType type : BonusType.values()) {
            if (!type.isNegative() || type == BonusType.BAD_LUCK || type == BonusType.RESET) {
                continue;
            }

            if (!BonusConfig.isBonusEnabled(type.name().toLowerCase())) {
                continue;
            }

            activateNegativeBonusFromBadLuck(type);
        }
    }

    private void applyTrickster() {
        playBonusSound(BonusType.TRICKSTER);

        var activeTypes = new java.util.ArrayList<>(bonusEndTimes.keySet());
        for (BonusType activeType : activeTypes) {
            if (!activeType.isPositive()) {
                deactivateBonus(activeType);
                removeBonusTimer(activeType);
            }
        }

        var fallingBonuses = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS);
        for (Entity entity : fallingBonuses) {
            Bonus bonusComponent = entity.getComponent(Bonus.class);
            if (bonusComponent != null) {
                BonusType type = bonusComponent.getBonusType();
                if (type != null && !type.isPositive()) {
                    entity.removeFromWorld();
                }
            }
        }

        for (BonusType type : BonusType.values()) {
            if (!type.isPositive() || type == BonusType.TRICKSTER || type == BonusType.RANDOM_BONUS || type == BonusType.LEVEL_PASS) {
                continue;
            }

            if (!BonusConfig.isBonusEnabled(type.name().toLowerCase())) {
                continue;
            }

            if (bonusEndTimes.containsKey(type)) {
                doubleBonusDuration(type);
                if (type == BonusType.PLASMA_WEAPON) {
                    plasmaShotsRemaining *= 2;
                    if (app.getScoreManager() != null) {
                        app.getScoreManager().updateBonusTimer(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
                    }
                }
            } else {
                activatePositiveBonusFromTrickster(type);
            }
        }
    }

    private void activatePositiveBonusFromTrickster(BonusType type) {
        boolean durationHandled = false;

        switch (type) {
            case BONUS_SCORE -> applyBonusScore();
            case EXTRA_LIFE -> applyExtraLife();
            case INCREASE_PADDLE -> {
                applyIncreasePaddle(INCREASE_PADDLE_BASE_DURATION * 2);
                durationHandled = true;
            }
            case STICKY_PADDLE -> {
                applyStickyPaddle(STICKY_PADDLE_BASE_DURATION * 2);
                durationHandled = true;
            }
            case SLOW_BALLS -> applySlowBalls();
            case ENERGY_BALLS -> applyEnergyBalls();
            case BONUS_WALL -> {
                applyBonusWall(BONUS_WALL_BASE_DURATION * 2);
                durationHandled = true;
            }
            case BONUS_MAGNET -> applyBonusMagnet();
            case BONUS_BALL -> applyBonusBall();
            case PLASMA_WEAPON -> {
                applyPlasmaWeapon();
                plasmaShotsRemaining *= 2;
                if (app.getScoreManager() != null) {
                    app.getScoreManager().updateBonusTimer(BonusType.PLASMA_WEAPON, plasmaShotsRemaining);
                }
                durationHandled = true;
            }
            case EXPLOSION_BALLS -> applyExplosionBalls();
            case LEVEL_PASS -> {
                // Не активируем проход уровня через шулера
                durationHandled = true;
            }
            case SCORE_RAIN -> {
                applyScoreRain(SCORE_RAIN_BASE_DURATION * 2);
                durationHandled = true;
            }
            default -> {
                // other positive bonuses handled above or do nothing
            }
        }

        if (!durationHandled) {
            doubleBonusDuration(type);
        }
    }

    private void applyResetBonus() {
        playBonusSound(BonusType.RESET);
        resetAllEffects();
    }

    private void activateNegativeBonusFromBadLuck(BonusType type) {
        int baseDuration = getNegativeBaseDuration(type);
        int durationSeconds = baseDuration;

        if (bonusEndTimes.containsKey(type)) {
            durationSeconds = Math.max(getBonusRemainingTime(type) * 2, baseDuration);
        }

        switch (type) {
            case CHAOTIC_BALLS -> applyChaoticBalls(durationSeconds);
            case FROZEN_PADDLE -> applyFrozenPaddle(durationSeconds);
            case DECREASE_PADDLE -> applyDecreasePaddle(durationSeconds);
            case FAST_BALLS -> applyFastBalls(durationSeconds);
            case PENALTIES_MAGNET -> applyPenaltiesMagnet(durationSeconds);
            case WEAK_BALLS -> applyWeakBalls(durationSeconds);
            case INVISIBLE_PADDLE -> applyInvisiblePaddle(durationSeconds);
            case DARKNESS -> applyDarkness(durationSeconds);
            default -> {
                // Бонусы без длительности (RESET, BAD_LUCK) обрабатываются отдельно
            }
        }
    }

    private int getNegativeBaseDuration(BonusType type) {
        return switch (type) {
            case CHAOTIC_BALLS -> CHAOTIC_BALLS_BASE_DURATION;
            case FROZEN_PADDLE -> FROZEN_PADDLE_BASE_DURATION;
            case DECREASE_PADDLE -> DECREASE_PADDLE_BASE_DURATION;
            case FAST_BALLS -> FAST_BALLS_BASE_DURATION;
            case PENALTIES_MAGNET -> PENALTIES_MAGNET_BASE_DURATION;
            case WEAK_BALLS -> WEAK_BALLS_BASE_DURATION;
            case INVISIBLE_PADDLE -> INVISIBLE_PADDLE_BASE_DURATION;
            case DARKNESS -> DARKNESS_BASE_DURATION;
            default -> 0;
        };
    }

    private void doubleBonusDuration(BonusType bonusType) {
        doubleBonusDuration(bonusType, 2.0);
    }

    private void doubleBonusDuration(BonusType bonusType, double multiplier) {
        Long endTime = bonusEndTimes.get(bonusType);
        if (endTime == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long remainingMillis = Math.max(0, endTime - now);
        long newRemaining = (long) Math.ceil(remainingMillis * multiplier);
        long newEndTime = now + newRemaining;

        bonusEndTimes.put(bonusType, newEndTime);

        int remainingSeconds = (int) Math.ceil(newRemaining / 1000.0);
        lastDisplayedSeconds.put(bonusType, remainingSeconds);
        updateBonusUI(bonusType, remainingSeconds);
    }
    
    /**
     * Удалить все активные бонусы с экрана
     */
    public void clearAllBonuses() {
        var bonuses = FXGL.getGameWorld().getEntitiesByType(EntityType.BONUS);
        for (Entity bonus : bonuses) {
            bonus.removeFromWorld();
        }
        // System.out.println("Удалено " + bonuses.size() + " активных бонусов");

        try {
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null) {
                app.onAllBonusesCleared();
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Сбросить все активные эффекты бонусов
     */
    public void resetAllEffects() {
        setDarknessActive(false);

        // Сбрасываем все состояния эффектов
        bonusMagnetActive = false;
        penaltyMagnetActive = false;
        scoreRainActive = false;
        increasePaddleStacks = 0;
        
        // Удаляем все защитные стены
        var walls = FXGL.getGameWorld().getEntitiesByType(EntityType.WALL);
        for (Entity wall : walls) {
            try {
                Boolean isProtectiveWall = wall.getBoolean("isProtectiveWall");
                if (isProtectiveWall != null && isProtectiveWall) {
                    wall.removeFromWorld();
        // System.out.println("Защитная стена удалена при сбросе эффектов");
                }
            } catch (IllegalArgumentException e) {
                // Обычная стена без свойства isProtectiveWall - игнорируем
            }
        }
        
        // Очищаем все таймеры бонусов
        bonusEndTimes.clear();
        lastDisplayedSeconds.clear();
        bonusPriorities.clear();
        pausedBonusTimes.clear();
        timersPaused = false;
        
        if (app.getBonusTimerView() != null) {
            app.getBonusTimerView().clearAllBonuses();
        }
        
        // Скрываем все бонусы в индикаторе под счетом
        if (app.getScoreManager() != null) {
            app.getScoreManager().hideAllBonuses();
        }
        
        // Отключаем плазменное оружие и удаляем все снаряды
        if (plasmaWeaponActive) {
// System.out.println("Плазменное оружие деактивировано при сбросе эффектов");
        }
        plasmaShotsRemaining = 0;
        plasmaWeaponActive = false;
        chaoticBallsActive = false;
        weakBallsActive = false;
        frozenPaddleActive = false;
        frozenPaddleTextureActive = false;
        invisiblePaddleActive = false;
        stickyPaddleTextureActive = false;
        stickyPaddleActive = false;
        updatePaddleTexture();
        
        // Удаляем все снаряды плазменного оружия
        var projectiles = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE);
        for (Entity projectile : projectiles) {
            projectile.removeFromWorld();
        }
        if (!projectiles.isEmpty()) {
        // System.out.println("Удалено " + projectiles.size() + " снарядов плазменного оружия");
        }
        
        // Сбрасываем эффекты мячей и запускаем прикрепленные мячи
        var balls = FXGL.getGameWorld().getEntitiesByType(EntityType.BALL);
        for (Entity ball : balls) {
            Ball ballComponent = ball.getComponent(Ball.class);
                if (ballComponent != null) {
                    ballComponent.resetEffects();
                }
            }

            // Очищаем список прикрепленных мячей
            com.arcadeblocks.gameplay.Ball.clearAttachedBalls();
        
        // Сбрасываем эффекты ракетки
        var paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
        for (Entity paddle : paddles) {
            Paddle paddleComponent = paddle.getComponent(Paddle.class);
            if (paddleComponent != null) {
                paddleComponent.resetEffects();
            }
        }
        
        // System.out.println("Все эффекты бонусов сброшены");
    }
    
    /**
     * Сбросить все активные эффекты бонусов, кроме липкости ракетки
     */
    public void resetAllEffectsExceptStickyPaddle() {
        // Сбрасываем все состояния эффектов, кроме липкости ракетки
        bonusMagnetActive = false;
        penaltyMagnetActive = false;
        scoreRainActive = false;
        increasePaddleStacks = 0;
        
        // Удаляем все защитные стены
        var walls = FXGL.getGameWorld().getEntitiesByType(EntityType.WALL);
        for (Entity wall : walls) {
            try {
                Boolean isProtectiveWall = wall.getBoolean("isProtectiveWall");
                if (isProtectiveWall != null && isProtectiveWall) {
                    wall.removeFromWorld();
        // System.out.println("Защитная стена удалена при сбросе эффектов");
                }
            } catch (IllegalArgumentException e) {
                // Обычная стена без свойства isProtectiveWall - игнорируем
            }
        }
        
        // Очищаем все таймеры бонусов
        bonusEndTimes.clear();
        lastDisplayedSeconds.clear();
        bonusPriorities.clear();
        pausedBonusTimes.clear();
        timersPaused = false;
        
        if (app.getBonusTimerView() != null) {
            app.getBonusTimerView().clearAllBonuses();
        }
        
        // Скрываем все бонусы в индикаторе под счетом
        if (app.getScoreManager() != null) {
            app.getScoreManager().hideAllBonuses();
        }
        
        // Отключаем плазменное оружие и удаляем все снаряды
        if (plasmaWeaponActive) {
// System.out.println("Плазменное оружие деактивировано при сбросе эффектов");
        }
        plasmaShotsRemaining = 0;
        plasmaWeaponActive = false;
        chaoticBallsActive = false;
        weakBallsActive = false;
        frozenPaddleActive = false;
        frozenPaddleTextureActive = false;
        invisiblePaddleActive = false;
        // НЕ сбрасываем stickyPaddleTextureActive и stickyPaddleActive
        updatePaddleTexture();
        setDarknessActive(false);
        
        // Удаляем все снаряды плазменного оружия
        var projectiles = FXGL.getGameWorld().getEntitiesByType(EntityType.PROJECTILE);
        for (Entity projectile : projectiles) {
            projectile.removeFromWorld();
        }
        if (!projectiles.isEmpty()) {
        // System.out.println("Удалено " + projectiles.size() + " снарядов плазменного оружия");
        }
        
        // НЕ запускаем прикрепленные мячи и НЕ сбрасываем их эффекты
        // НЕ очищаем список прикрепленных мячей
        // НЕ сбрасываем эффекты ракетки
        
        // System.out.println("Все эффекты бонусов сброшены (кроме липкости ракетки)");
    }
    
    /**
     * Проверить, должен ли бонус притягиваться магнитом
     */
    public boolean shouldAttractBonus(BonusType bonusType) {
        if (bonusType == BonusType.LEVEL_PASS) {
            return false;
        }
        if (bonusMagnetActive && bonusType.isPositive()) {
            return true;
        }
        if (penaltyMagnetActive && !bonusType.isPositive()) {
            return true;
        }
        return false;
    }
    
    /**
     * Получить силу магнитного притяжения
     */
    public double getMagnetForce() {
        return magnetForce;
    }
    
    /**
     * Получить радиус действия магнита
     */
    public double getMagnetRadius() {
        return magnetRadius;
    }
    
    /**
     * Проверить, активен ли магнит для бонусов
     */
    public boolean isBonusMagnetActive() {
        return bonusMagnetActive;
    }
    
    /**
     * Проверить, активен ли магнит для штрафов
     */
    public boolean isPenaltyMagnetActive() {
        return penaltyMagnetActive;
    }
    
    /**
     * Проверить, активно ли плазменное оружие
     */
    public boolean isPlasmaWeaponActive() {
        return plasmaWeaponActive;
    }
    
    public boolean isStickyPaddleActive() {
        return stickyPaddleActive;
    }
    
    /**
     * Получить количество оставшихся плазменных выстрелов
     */
    public int getPlasmaShotsRemaining() {
        return plasmaShotsRemaining;
    }
    
    /**
     * Проверить, активны ли хаотичные мячи
     */
    public boolean isChaoticBallsActive() {
        return chaoticBallsActive;
    }
    
    /**
     * Проверить, активны ли слабые мячи
     */
    public boolean isWeakBallsActive() {
        return weakBallsActive;
    }

    public boolean isScoreRainActive() {
        return scoreRainActive;
    }
    
    /**
     * Проверяет, активен ли эффект темноты
     */
    public boolean isDarknessActive() {
        return darknessActive;
    }
    
    /**
     * Проверяет, активен ли эффект замороженной ракетки
     */
    public boolean isFrozenPaddleActive() {
        return frozenPaddleActive;
    }
    
    /**
     * Проверяет, активен ли эффект призрачной ракетки
     */
    public boolean isInvisiblePaddleActive() {
        return invisiblePaddleActive;
    }
    
    // ========== СЛУЧАЙНЫЙ БОНУС ==========
    
    private void applyRandomBonus() {
        // Воспроизводим звук активации
        playBonusSound(BonusType.RANDOM_BONUS);
        
        // Получаем случайный бонус (исключая RANDOM_BONUS)
        BonusType randomBonus = BonusType.getRandomBonusForActivation();
        
        // Дополнительная защита от рекурсии
        if (randomBonus == BonusType.RANDOM_BONUS) {
        // System.out.println("⚠️ Защита от рекурсии: случайный бонус выбрал сам себя, используем BONUS_SCORE");
            randomBonus = BonusType.BONUS_SCORE;
        }
        
        // System.out.println("🎲 Случайный бонус активирован! Выбран: " + randomBonus.getDescription());
        
        // Применяем случайно выбранный бонус
        applyBonusEffect(randomBonus);
    }
    
    /**
     * Приостановить все таймеры бонусов
     * Сохраняет текущее оставшееся время для последующего возобновления
     */
    public void pauseAllBonusTimers() {
        if (timersPaused) {
            return; // Уже на паузе
        }
        
        long now = System.currentTimeMillis();
        pausedBonusTimes.clear();
        
        // Сохраняем оставшееся время для каждого активного бонуса
        bonusEndTimes.forEach((type, endTime) -> {
            long remainingMillis = endTime - now;
            if (remainingMillis > 0) {
                pausedBonusTimes.put(type, remainingMillis);
            }
        });
        
        timersPaused = true;
        // System.out.println("⏸️ Все таймеры бонусов приостановлены (" + pausedBonusTimes.size() + " активных)");
    }
    
    /**
     * Возобновить все таймеры бонусов
     * Восстанавливает оставшееся время, которое было до паузы
     */
    public void resumeAllBonusTimers() {
        if (!timersPaused) {
            return; // Не на паузе
        }
        
        long now = System.currentTimeMillis();
        
        // Восстанавливаем таймеры с оставшимся временем
        pausedBonusTimes.forEach((type, remainingMillis) -> {
            long newEndTime = now + remainingMillis;
            bonusEndTimes.put(type, newEndTime);
            
            int remainingSeconds = (int) Math.ceil(remainingMillis / 1000.0);
            lastDisplayedSeconds.put(type, remainingSeconds);
            updateBonusUI(type, remainingSeconds);
        });
        
        pausedBonusTimes.clear();
        timersPaused = false;
        // System.out.println("▶️ Все таймеры бонусов возобновлены");
    }
}
