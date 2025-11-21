package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.arcadeblocks.EntityType;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.util.TextureUtils;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Компонент мяча
 */
public class Ball extends Component {
    
    private PhysicsComponent physics;
    private int damage = GameConfig.BALL_DAMAGE_TO_BRICK;
    
    // Состояние прикрепления к ракетке
    private boolean attachedToPaddle = true;
    private Entity paddleEntity = null;
    
    // Эффекты
    private boolean isStickyEnabled = false; // Липкая ракетка
    private double speedMultiplier = 1.0; // Множитель скорости
    private boolean isEnergyBall = false; // Энергетический мяч
    private boolean isExplosionBall = false; // Взрывной мяч
    private boolean isWeakBall = false; // Слабый мяч
    private boolean isChaoticBall = false; // Хаотичный мяч
    private boolean isExtraBall = false; // Дополнительный мяч (не тратит жизнь при падении)
    private double sizeMultiplier = 1.0; // Множитель размера мяча
    
    // Защита от ложного срабатывания при запуске
    private long launchTime = 0; // Время последнего запуска мяча
    private static final long LAUNCH_PROTECTION_MS = 500; // 500мс защиты после запуска
    private static final long BOSS_HIT_COOLDOWN_MS = 120;
    
    // Параметры синхронизации прикрепленного мяча с ракеткой
    private double attachedOffsetX = 0.0;
    private double attachedOffsetY = -GameConfig.BALL_RADIUS * 2 - 5;
    
    // Статический список для отслеживания прикрепленных мячей к ракетке
    private static final java.util.List<Entity> attachedBalls = new java.util.ArrayList<>();
    private static final int MAX_ATTACHED_BALLS = 4;
    private double lastCenterX;
    private double lastCenterY;
    private Entity lastProcessedBrick;
    
    // Невесомость - поддержание постоянной скорости
    private double targetSpeed = GameConfig.BALL_SPEED; // Целевая скорость мяча
    private boolean maintainConstantSpeed = true; // Флаг для поддержания постоянной скорости
    private long lastBossHitTime = 0;
    private long lastAttractionBlockedSoundTime = 0; // Время последнего проигрывания звука блокировки притяжения
    private static final long ATTRACTION_BLOCKED_SOUND_COOLDOWN_MS = 1000; // 1 секунда между звуками
    
    // Состояние при паузе обратного отсчета
    private boolean pausedForCountdown = false;
    private double pausedVelocityX = 0.0;
    private double pausedVelocityY = 0.0;
    private BodyType pausedBodyType = BodyType.DYNAMIC;
    private boolean pausedMaintainConstantSpeed = true;
    
    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
        // Мяч остается прикрепленным к ракетке до запуска
        setPhysicsState(0, 0, BodyType.KINEMATIC);
        lastCenterX = entity.getCenter().getX();
        lastCenterY = entity.getCenter().getY();
    }
    
    @Override
    public void onRemoved() {
        // КРИТИЧНО: Очищаем ссылки для предотвращения утечек памяти
        paddleEntity = null;
        lastProcessedBrick = null;
        
        // КРИТИЧНО: Удаляем мяч из статического списка attachedBalls
        // synchronized для thread-safety
        synchronized (attachedBalls) {
            attachedBalls.remove(entity);
        }
        
        // КРИТИЧНО: Обнуляем ссылку на PhysicsComponent
        physics = null;
    }
    
    @Override
    public void onUpdate(double tpf) {
        if (attachedToPaddle) {
            if (paddleEntity == null || !paddleEntity.isActive()) {
                // КРИТИЧНО: Если ракетка неактивна, ищем новую активную ракетку
                List<Entity> paddles = FXGL.getGameWorld().getEntitiesByType(EntityType.PADDLE);
                if (!paddles.isEmpty()) {
                    Entity newPaddle = paddles.get(0);
                    if (newPaddle.isActive()) {
                        // Прикрепляем к новой ракетке
                        paddleEntity = newPaddle;
                        synchronizeAttachedBall(tpf);
                        return;
                    }
                }
                // Если новой ракетки нет, отсоединяем мяч
                attachedToPaddle = false;
            } else {
                synchronizeAttachedBall(tpf);
                return;
            }
        }

        if (!attachedToPaddle) {
            // Проверка границ экрана только если мяч не прикреплен
            checkBounds();
            checkBossCollision();
            handleContinuousBrickCollisions();
            
            // Поддерживаем постоянную скорость в невесомости
            if (maintainConstantSpeed) {
                maintainConstantSpeed(tpf);
            }
            
            // Применяем хаотичное поведение
            if (isChaoticBall) {
                applyChaoticBehavior(tpf);
            }
        }
        
        lastCenterX = entity.getCenter().getX();
        lastCenterY = entity.getCenter().getY();
    }
    
    private void synchronizeAttachedBall(double tpf) {
        if (paddleEntity == null) {
            return;
        }

        double paddleCenterX = paddleEntity.getCenter().getX();
        double paddleY = paddleEntity.getY();
        
        // Вычисляем смещение мяча относительно ракетки
        double desiredCenterX = paddleCenterX + attachedOffsetX;
        double desiredX = desiredCenterX - GameConfig.BALL_RADIUS;
        double desiredY = paddleY + attachedOffsetY;
        
        if (Math.abs(entity.getX() - desiredX) > 0.01 || Math.abs(entity.getY() - desiredY) > 0.01) {
            entity.setPosition(desiredX, desiredY);
            
            if (physics != null) {
                Point2D targetPoint = new Point2D(desiredX, desiredY);
                try {
                    physics.overwritePosition(targetPoint);
                } catch (IllegalStateException e) {
                    physics.setOnPhysicsInitialized(() -> physics.overwritePosition(targetPoint));
                }
            }
        }

        lastCenterX = entity.getCenter().getX();
        lastCenterY = entity.getCenter().getY();

        setPhysicsState(0, 0, BodyType.KINEMATIC);
    }
    
    public void launchBall() {
        if (attachedToPaddle && physics != null && paddleEntity != null) {
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null && app.isLaunchLocked() && !isStickyEnabled) {
                return;
            }
        // System.out.println("ЗАПУСК МЯЧА:");
        // System.out.println("  Позиция мяча ДО запуска: (" + entity.getX() + ", " + entity.getY() + ")");
        // System.out.println("  Позиция ракетки ДО запуска: (" + paddleEntity.getX() + ", " + paddleEntity.getY() + ")");
        // System.out.println("  Центр мяча ДО запуска: (" + entity.getCenter().getX() + ", " + entity.getCenter().getY() + ")");
        // System.out.println("  Центр ракетки ДО запуска: (" + paddleEntity.getCenter().getX() + ", " + paddleEntity.getCenter().getY() + ")");
            
            // Получаем актуальную позицию ракетки
            double currentPaddleY = paddleEntity.getY();
            double currentPaddleCenterX = paddleEntity.getCenter().getX();

            double launchBallX = currentPaddleCenterX + attachedOffsetX - GameConfig.BALL_RADIUS;
            double launchBallY = currentPaddleY + attachedOffsetY;

            entity.setPosition(launchBallX, launchBallY);
            lastCenterX = entity.getCenter().getX();
            lastCenterY = entity.getCenter().getY();
            
        // System.out.println("  Точная позиция мяча для запуска: (" + launchBallX + ", " + launchBallY + ")");
        // System.out.println("  Актуальная позиция ракетки: (" + currentPaddleX + ", " + currentPaddleY + ")");
            
            attachedToPaddle = false;
            launchTime = System.currentTimeMillis(); // Запоминаем время запуска
            
            // Удаляем мяч из списка прикрепленных при запуске
            removeAttachedBall(entity);
            
            // Используем задержку для правильной смены типа тела
            FXGL.runOnce(() -> {
                if (physics != null && entity != null && entity.isActive()) {
                    setPhysicsBodyType(BodyType.DYNAMIC);
                    
                    // Принудительно устанавливаем позицию мяча несколько раз
                    entity.setPosition(launchBallX, launchBallY);
                    
                    // Дополнительная задержка для стабилизации позиции
                    FXGL.runOnce(() -> {
                        if (physics != null && entity != null && entity.isActive()) {
                            // Еще раз устанавливаем позицию мяча
                            entity.setPosition(launchBallX, launchBallY);
                            lastCenterX = entity.getCenter().getX();
                            lastCenterY = entity.getCenter().getY();
                            
                    // Запуск мяча с учетом позиции относительно ракетки
                    double launchSpeed = GameConfig.BALL_SPEED * 1.3 * speedMultiplier; // Используем конфигурацию
                    
                    double paddleWidth = Math.max(1.0, paddleEntity.getWidth());
                    double halfWidth = paddleWidth / 2.0;
                    double ratio = halfWidth > 0 ? attachedOffsetX / halfWidth : 0.0;
                    ratio = Math.max(-1.0, Math.min(1.0, ratio));

                    // Более точный расчет угла запуска
                    double maxAngle = Math.PI / 3; // 60 градусов максимум
                    double angle = ratio * maxAngle;

                    double velocityX = Math.sin(angle) * launchSpeed;
                    double velocityY = -Math.abs(Math.cos(angle) * launchSpeed); // Всегда вверх

                    // Учитываем скорость движения ракетки для более реалистичной физики
                    PhysicsComponent paddlePhysics = paddleEntity.getComponent(PhysicsComponent.class);
                    if (paddlePhysics != null) {
                        double paddleVelX = 0.0;
                        try {
                            paddleVelX = paddlePhysics.getLinearVelocity().getX();
                        } catch (IllegalStateException ignored) {}
                        // Добавляем 40% скорости ракетки к горизонтальной скорости мяча
                        velocityX += paddleVelX * 0.4;
                        
                        // Ограничиваем максимальную скорость, чтобы мяч не улетал слишком быстро
                        double maxVelocityX = launchSpeed * 0.8;
                        velocityX = Math.max(-maxVelocityX, Math.min(maxVelocityX, velocityX));
                    }

                    setPhysicsVelocity(velocityX, velocityY);
                    maintainConstantSpeed = true;
                    targetSpeed = GameConfig.BALL_SPEED;
                    attachedOffsetX = 0.0;
        // System.out.println("  Мяч запущен: attachedToPaddle=false, скорость=" + launchSpeed + ", X=" + velocityX + ", Y=" + velocityY);
        // System.out.println("  Offset: " + offset + ", ratio: " + ratio + ", angle: " + Math.toDegrees(angle) + "°");
        // System.out.println("  Позиция мяча ПОСЛЕ запуска: (" + entity.getX() + ", " + entity.getY() + ")");
        // System.out.println("  Позиция ракетки ПОСЛЕ запуска: (" + paddleEntity.getX() + ", " + paddleEntity.getY() + ")");
        // System.out.println("  Сохраненная позиция мяча: (" + launchBallX + ", " + launchBallY + ")");
                            
                            // Финальная проверка и коррекция позиции
                            if (Math.abs(entity.getX() - launchBallX) > 10 || Math.abs(entity.getY() - launchBallY) > 10) {
        // System.out.println("  КОРРЕКЦИЯ: мяч телепортировался, восстанавливаем позицию");
                                entity.setPosition(launchBallX, launchBallY);
                            }
                        }
                    }, Duration.millis(16)); // Один кадр для стабилизации
                }
            }, Duration.millis(8)); // Половина кадра для смены типа тела
        }
    }
    
    public void attachToPaddle(Entity paddle) {
        this.paddleEntity = paddle;
        this.attachedToPaddle = true;
        // System.out.println("Мяч прикреплен к ракетке: attachedToPaddle=" + attachedToPaddle + ", paddleEntity=" + (paddleEntity != null));
        
        setPhysicsState(0, 0, BodyType.KINEMATIC);
        
        // Немедленно синхронизируем позицию мяча с ракеткой
        if (paddle != null) {
            // Принудительная синхронизация для гарантии точного позиционирования
            double paddleCenterX = paddle.getCenter().getX();
            double desiredCenterX = paddleCenterX + attachedOffsetX;
            double desiredX = desiredCenterX - GameConfig.BALL_RADIUS;
            double desiredY = paddle.getY() + attachedOffsetY;
            entity.setPosition(desiredX, desiredY);
            
            if (physics != null) {
                Point2D targetPoint = new Point2D(desiredX, desiredY);
                try {
                    physics.overwritePosition(targetPoint);
                } catch (IllegalStateException e) {
                    physics.setOnPhysicsInitialized(() -> physics.overwritePosition(targetPoint));
                }
            }
            lastCenterX = entity.getCenter().getX();
            lastCenterY = entity.getCenter().getY();
        // System.out.println("Позиция мяча синхронизирована с ракеткой: (" + entity.getX() + ", " + entity.getY() + ")");
        // System.out.println("Позиция ракетки: (" + paddle.getX() + ", " + paddle.getY() + ")");
        }
    }
    
    public boolean isAttachedToPaddle() {
        return attachedToPaddle;
    }
    
    /**
     * Устанавливает смещение мяча относительно ракетки
     */
    public void setAttachedOffset(double offsetX, double offsetY) {
        this.attachedOffsetX = offsetX;
        this.attachedOffsetY = offsetY;
    }
    
    /**
     * Получает текущее смещение мяча относительно ракетки
     */
    public double[] getAttachedOffset() {
        return new double[]{attachedOffsetX, attachedOffsetY};
    }
    
    /**
     * Получить количество прикрепленных мячей
     */
    public static int getAttachedBallsCount() {
        synchronized (attachedBalls) {
            return attachedBalls.size();
        }
    }
    
    /**
     * Очистить список прикрепленных мячей
     */
    public static void clearAttachedBalls() {
        synchronized (attachedBalls) {
            attachedBalls.clear();
        }
    }
    
    /**
     * Удалить мяч из списка прикрепленных
     */
    public static void removeAttachedBall(Entity ball) {
        synchronized (attachedBalls) {
            attachedBalls.remove(ball);
        }
    }
    
    /**
     * Добавить мяч в список прикрепленных
     */
    public static void addAttachedBall(Entity ball) {
        synchronized (attachedBalls) {
            if (!attachedBalls.contains(ball)) {
                attachedBalls.add(ball);
            }
        }
    }
    
    /**
     * Получить следующее доступное смещение для прикрепления мяча
     */
    public static double[] getNextAttachedOffset() {
        synchronized (attachedBalls) {
            int count = attachedBalls.size();
            if (count >= MAX_ATTACHED_BALLS) {
                return null; // Максимум достигнут
            }
        
            // Распределяем мячи по ширине ракетки с учетом их радиуса
            double ballDiameter = GameConfig.BALL_RADIUS * 2;
            double spacing = ballDiameter + 10.0; // Расстояние между центрами мячей
            double totalWidth = (MAX_ATTACHED_BALLS - 1) * spacing;
            double startOffset = -totalWidth / 2.0;
            
            // Находим свободную позицию
            double offsetX = startOffset + (count * spacing);
            double offsetY = -GameConfig.BALL_RADIUS * 2 - 5;
            
            return new double[]{offsetX, offsetY};
        }
    }
    
    private void checkBounds() {
        if (physics == null) {
            return;
        }
        double x = entity.getX();
        double y = entity.getY();
        double velocityX;
        double velocityY;
        try {
            velocityX = physics.getLinearVelocity().getX();
            velocityY = physics.getLinearVelocity().getY();
        } catch (IllegalStateException e) {
            return;
        }
        
        // Проверка левой границы
        if (x <= 0) {
            entity.setX(2); // Небольшой отступ от границы
            // Добавляем небольшую случайность для более интересной игры
            double randomFactor = 0.95 + Math.random() * 0.1; // От 95% до 105% скорости
            setPhysicsVelocity(Math.abs(velocityX) * randomFactor, velocityY);
            try {
                ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/wall_bounce.wav");
            } catch (Exception e) {}
        }
        
        // Проверка правой границы
        if (x >= GameConfig.GAME_WIDTH - GameConfig.BALL_RADIUS * 2) {
            entity.setX(GameConfig.GAME_WIDTH - GameConfig.BALL_RADIUS * 2 - 2);
            // Добавляем небольшую случайность для более интересной игры
            double randomFactor = 0.95 + Math.random() * 0.1; // От 95% до 105% скорости
            setPhysicsVelocity(-Math.abs(velocityX) * randomFactor, velocityY);
            try {
                ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/wall_bounce.wav");
            } catch (Exception e) {}
        }
        
        // Проверка верхней границы
        // Увеличиваем хитбокс верхней границы, учитывая радиус мяча и добавляя запас
        double topBoundary = GameConfig.TOP_UI_HEIGHT;
        double extraHitbox = 5; // Дополнительный запас для хитбокса (5 пикселей)
        double effectiveTopBoundary = topBoundary + extraHitbox; // Граница с увеличенным хитбоксом
        
        // Проверяем верхнюю точку мяча (центр - радиус, т.к. Y увеличивается вниз)
        // Если верхняя точка мяча достигла или прошла границу (с учетом хитбокса)
        if (y - GameConfig.BALL_RADIUS <= effectiveTopBoundary) {
            // Устанавливаем позицию так, чтобы верхняя точка мяча была чуть ниже границы
            entity.setY(topBoundary + GameConfig.BALL_RADIUS + 2); // Центр мяча на границе + радиус + отступ
            // Добавляем небольшую случайность для более интересной игры
            double randomFactor = 0.95 + Math.random() * 0.1; // От 95% до 105% скорости
            setPhysicsVelocity(velocityX, Math.abs(velocityY) * randomFactor);
            try {
                ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/wall_bounce.wav");
            } catch (Exception e) {}
        }
        
        // Проверка нижней границы (потеря мяча)
        if (y > GameConfig.GAME_HEIGHT) {
            // Защита от ложного срабатывания сразу после запуска мяча
            long currentTime = System.currentTimeMillis();
            if (currentTime - launchTime < LAUNCH_PROTECTION_MS) {
        // System.out.println("Защита от ложного срабатывания: мяч только что был запущен, игнорируем падение");
                return;
            }
            onBallLost();
        }
    }
    
    public void onPaddleHit(Entity paddle) {
        // Проверяем, не призрачная ли ракетка
        Paddle paddleComponent = paddle.getComponent(Paddle.class);
        if (paddleComponent != null && paddleComponent.isInvisible()) {
            // Мяч проходит сквозь призрачную ракетку
            return;
        }
        
        // Проверяем, что мяч действительно ударился о верхнюю часть ракетки
        double paddleTop = paddle.getY();
        double ballCenterY = entity.getCenter().getY();
        
        // Если мяч находится слишком низко относительно ракетки, игнорируем столкновение
        if (ballCenterY > paddleTop + paddle.getHeight() * 0.3) {
            return;
        }
        
        // Если включена липкая ракетка, прикрепляем мяч
        if (isStickyEnabled) {
            // Дополнительная проверка: убеждаемся, что липкая ракетка активна глобально
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null && app.getBonusEffectManager() != null && app.getBonusEffectManager().isStickyPaddleActive()) {
                // Проверяем, не прикреплен ли уже этот мяч
                boolean alreadyAttached;
                synchronized (attachedBalls) {
                    alreadyAttached = attachedBalls.contains(entity);
                }
                if (alreadyAttached) {
                    // System.out.println("🖱️ Мяч уже прикреплен к ракетке, игнорируем");
                    try {
                        app.getAudioManager().playSFX("sounds/sfx/paddle_hit.wav");
                    } catch (Exception e) {}
                    return;
                }
                
                // Проверяем, не достигнут ли максимум прикрепленных мячей
                if (getAttachedBallsCount() >= MAX_ATTACHED_BALLS) {
                    // Если максимум достигнут, мяч отскакивает как обычно
                    // System.out.println("🖱️ Максимум прикрепленных мячей достигнут (" + MAX_ATTACHED_BALLS + "), мяч отскакивает");
                } else {
                    // Получаем следующее доступное смещение
                    double[] offset = getNextAttachedOffset();
                    if (offset != null) {
                        // Устанавливаем смещение и прикрепляем мяч
                        setAttachedOffset(offset[0], offset[1]);
                        attachToPaddle(paddle);
                        addAttachedBall(entity);
                        // System.out.println("🖱️ Мяч прикреплен к липкой ракетке (позиция " + (getAttachedBallsCount()) + "/" + MAX_ATTACHED_BALLS + ")");
                    }
                }
                
                try {
                    app.getAudioManager().playSFX("sounds/sfx/paddle_hit.wav");
                } catch (Exception e) {}
                return;
            } else {
                // Липкая ракетка не активна глобально, отключаем липкость у мяча
                setStickyEnabled(false);
                // System.out.println("🖱️ Липкость мяча отключена (липкая ракетка не активна)");
            }
        }
        
        // Расчет отскока от платформы с улучшенной физикой
        double ballCenterX = entity.getCenter().getX();
        double paddleCenterX = paddle.getCenter().getX();
        double offset = ballCenterX - paddleCenterX;
        double maxOffset = paddle.getWidth() / 2;
        double ratio = Math.max(-1.0, Math.min(1.0, offset / maxOffset)); // Ограничиваем ratio от -1 до 1
        
        // Более точный расчет угла отскока
        double maxAngle = Math.PI / 4; // 45 градусов максимум для лучшего контроля
        double angle = ratio * maxAngle;
        
        // Базовая скорость отскока с учетом множителя
        double bounceSpeed = GameConfig.BALL_SPEED * 1.1 * speedMultiplier;
        
        // Вычисляем скорости
        double velocityX = Math.sin(angle) * bounceSpeed;
        double velocityY = -Math.abs(Math.cos(angle) * bounceSpeed); // Всегда вверх
        
        // Учитываем скорость движения ракетки при отскоке
        PhysicsComponent paddlePhysics = paddle.getComponent(PhysicsComponent.class);
        if (paddlePhysics != null) {
            double paddleVelX = 0.0;
            try {
                paddleVelX = paddlePhysics.getLinearVelocity().getX();
            } catch (IllegalStateException ignored) {}
            // Добавляем 30% скорости ракетки для более реалистичной физики
            velocityX += paddleVelX * 0.3;
            
            // Ограничиваем максимальную скорость
            double maxVelocityX = bounceSpeed * 0.9;
            velocityX = Math.max(-maxVelocityX, Math.min(maxVelocityX, velocityX));
        }
        
        // System.out.println("СТОЛКНОВЕНИЕ С РАКЕТКОЙ:");
        // System.out.println("  Позиция мяча: (" + entity.getX() + ", " + entity.getY() + ")");
        // System.out.println("  Позиция ракетки: (" + paddle.getX() + ", " + paddle.getY() + ")");
        // System.out.println("  Центр мяча: " + ballCenterX + ", центр ракетки: " + paddleCenterX);
        // System.out.println("  Offset: " + offset + ", ratio: " + ratio);
        // System.out.println("  Новые скорости: X=" + velocityX + ", Y=" + velocityY);
        
        setPhysicsVelocity(velocityX, velocityY);
        
        // Коррекция позиции мяча для предотвращения застревания
        double ballY = entity.getY();
        double paddleY = paddle.getY();
        
        // Если мяч находится слишком близко к ракетке, отталкиваем его
        if (ballY + entity.getHeight() > paddleY - 2) {
            entity.setY(paddleY - entity.getHeight() - 2);
        }
        
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/paddle_hit.wav");
        } catch (Exception e) {}
    }
    
    public void onWallHit(Entity wall) {
        // Обработка столкновения с защитной стеной
        // System.out.println("СТОЛКНОВЕНИЕ С ЗАЩИТНОЙ СТЕНОЙ");
        
        if (physics != null) {
            double velocityX;
            double velocityY;
            try {
                velocityX = physics.getLinearVelocity().getX();
                velocityY = physics.getLinearVelocity().getY();
            } catch (IllegalStateException e) {
                return;
            }
            
            // Отражаем вертикальную скорость (мяч отскакивает вверх)
            if (velocityY > 0) {
                velocityY = -Math.abs(velocityY);
        // System.out.println("  Мяч отражен от защитной стены вверх: Y=" + velocityY);
            }
            
            setPhysicsVelocity(velocityX, velocityY);
        }
        
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/wall_bounce.wav");
        } catch (Exception e) {}
    }
    
    public void onBrickHit(Entity brick) {
        // Если мяч энергетический, разрушаем кирпич но не меняем траекторию мяча
        if (isEnergyBall) {
            destroyBrick(brick);
            return;
        }
        
        // Наносим урон кирпичу
        Brick brickComponent = brick.getComponent(Brick.class);
        if (brickComponent != null) {
            int actualDamage = isWeakBall ? 1 : 2; // Слабый мяч наносит урон 1, обычный мяч - урон 2
            brickComponent.takeDamage(actualDamage);
            
            // Если мяч взрывной, взрываем соседние кирпичи
            if (isExplosionBall) {
                explodeNearbyBricks(brick, actualDamage);
            }
        }
        
        // Улучшенная физика отскока от кирпича
        if (physics != null) {
            // Получаем позиции и размеры
            double ballCenterX = entity.getCenter().getX();
            double ballCenterY = entity.getCenter().getY();
            double brickCenterX = brick.getCenter().getX();
            double brickCenterY = brick.getCenter().getY();
            
            double ballRadius = getCurrentBallRadius();
            double brickWidth = brick.getWidth();
            double brickHeight = brick.getHeight();
            
            // Вычисляем расстояние от центра мяча до краев кирпича
            double deltaX = ballCenterX - brickCenterX;
            double deltaY = ballCenterY - brickCenterY;
            
            // Определяем, с какой стороны произошло столкновение
            double overlapX = (brickWidth / 2 + ballRadius) - Math.abs(deltaX);
            double overlapY = (brickHeight / 2 + ballRadius) - Math.abs(deltaY);
            
            double currentVelX;
            double currentVelY;
            try {
                currentVelX = physics.getLinearVelocity().getX();
                currentVelY = physics.getLinearVelocity().getY();
            } catch (IllegalStateException e) {
                return;
            }
            
            // Если пересечение по X больше, чем по Y - столкновение с боковой стороной
            if (overlapX < overlapY) {
                // Столкновение с левой или правой стороной
                // Добавляем небольшую случайность для более интересной игры
                double randomFactor = 0.98 + Math.random() * 0.04; // От 98% до 102% скорости
                setPhysicsVelocity(-currentVelX * randomFactor, currentVelY);
                
                // Коррекция позиции, чтобы мяч не застрял
                if (deltaX > 0) {
                    // Мяч справа от кирпича - отталкиваем вправо
                    entity.setX(brickCenterX + brickWidth / 2 + ballRadius + 2);
                } else {
                    // Мяч слева от кирпича - отталкиваем влево
                    entity.setX(brickCenterX - brickWidth / 2 - ballRadius - 2);
                }
            } else {
                // Столкновение с верхней или нижней стороной
                // Добавляем небольшую случайность для более интересной игры
                double randomFactor = 0.98 + Math.random() * 0.04; // От 98% до 102% скорости
                setPhysicsVelocity(currentVelX, -currentVelY * randomFactor);
                
                // Коррекция позиции, чтобы мяч не застрял
                if (deltaY > 0) {
                    // Мяч снизу от кирпича - отталкиваем вниз
                    entity.setY(brickCenterY + brickHeight / 2 + ballRadius + 2);
                } else {
                    // Мяч сверху от кирпича - отталкиваем вверх
                    entity.setY(brickCenterY - brickHeight / 2 - ballRadius - 2);
                }
            }
        }
        
        lastProcessedBrick = brick;
        
        // Звук воспроизводится в Brick.java при получении урона
        // try {
        //     ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/brick_break.wav");
        // } catch (Exception e) {}
    }
    
    public int getDamage() {
        return damage;
    }
    
    public double getBossDamage() {
        return isWeakBall ? GameConfig.BOSS_DAMAGE_FROM_WEAK_BALL : GameConfig.BOSS_DAMAGE_FROM_STANDARD_BALL;
    }

    private void setPhysicsVelocity(double velocityX, double velocityY) {
        if (physics == null) {
            return;
        }
        try {
            physics.setLinearVelocity(velocityX, velocityY);
        } catch (IllegalStateException e) {
            physics.setOnPhysicsInitialized(() -> physics.setLinearVelocity(velocityX, velocityY));
        }
    }

    private void setPhysicsBodyType(BodyType bodyType) {
        if (physics == null) {
            return;
        }
        try {
            physics.setBodyType(bodyType);
        } catch (IllegalStateException e) {
            physics.setOnPhysicsInitialized(() -> physics.setBodyType(bodyType));
        }
    }

    private void setPhysicsState(double velocityX, double velocityY, BodyType bodyType) {
        if (physics == null) {
            return;
        }
        try {
            physics.setLinearVelocity(velocityX, velocityY);
            physics.setBodyType(bodyType);
        } catch (IllegalStateException e) {
            physics.setOnPhysicsInitialized(() -> {
                physics.setLinearVelocity(velocityX, velocityY);
                physics.setBodyType(bodyType);
            });
        }
    }

    public void applyPaddleAttraction(Point2D paddleCenter, double tpf) {
        if (physics == null || entity == null || paddleCenter == null || attachedToPaddle) {
            return;
        }
        if (!entity.isActive()) {
            return;
        }

        // Проверяем, не активны ли эффекты, которые должны отключать притяжение
        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
        if (app != null) {
            // КРИТИЧНО: На легкой сложности притягивание работает ВСЕГДА, без исключений
            boolean isEasy = app.getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.EASY;
            
            if (!isEasy) {
                // КРИТИЧНО: Отключаем притяжение на хардкорной сложности
                if (app.getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE) {
                    return;
                }
                
                if (app.getBonusEffectManager() != null) {
                    com.arcadeblocks.gameplay.BonusEffectManager bonusManager = app.getBonusEffectManager();
                    // Отключаем притяжение, если активны: темнота, замороженная ракетка, призрачная ракетка или хаотичные мячи
                    if (bonusManager.isDarknessActive() || 
                        bonusManager.isFrozenPaddleActive() || 
                        bonusManager.isInvisiblePaddleActive() ||
                        bonusManager.isChaoticBallsActive()) {
                        
                        // Проигрываем звук блокировки только для темноты и только раз в секунду
                        if (bonusManager.isDarknessActive()) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastAttractionBlockedSoundTime >= ATTRACTION_BLOCKED_SOUND_COOLDOWN_MS) {
                                lastAttractionBlockedSoundTime = currentTime;
                                try {
                                    app.getAudioManager().playSFX("sounds/call_to_paddle_block.wav");
                                } catch (Exception e) {
                                    // Игнорируем ошибки воспроизведения звука
                                }
                            }
                        }
                        
                        return;
                    }
                }
            }
        }

        Point2D ballCenter = entity.getCenter();
        double distance = paddleCenter.distance(ballCenter);
        if (distance < 5.0) {
            return;
        }

        Point2D toPaddle = paddleCenter.subtract(ballCenter);
        double magnitude = toPaddle.magnitude();
        if (magnitude < 0.0001) {
            return;
        }

        // Анализируем препятствия и корректируем направление
        Point2D direction = adjustDirectionToAvoidBricks(ballCenter, toPaddle.normalize(), paddleCenter);
        
        Point2D currentVelocity;
        try {
            currentVelocity = physics.getLinearVelocity();
        } catch (IllegalStateException e) {
            return;
        }

        double currentSpeed = currentVelocity.magnitude();
        double baseTargetSpeed = Math.max(targetSpeed * speedMultiplier, GameConfig.BALL_SPEED);
        if (currentSpeed < 20.0) {
            currentSpeed = baseTargetSpeed;
        }

        double normalizedTpf = Math.min(tpf * 60.0, 2.0);
        double distanceFactor = Math.min(distance / 400.0, 1.0);
        double influence = (0.08 + 0.22 * distanceFactor) * normalizedTpf;
        influence = Math.max(0.02, Math.min(influence, 0.65));

        Point2D desiredVelocity = direction.multiply(Math.max(currentSpeed, baseTargetSpeed * 0.9));
        Point2D adjustment = desiredVelocity.subtract(currentVelocity).multiply(influence);
        Point2D newVelocity = currentVelocity.add(adjustment);
        double newSpeed = newVelocity.magnitude();
        double minSpeed = Math.max(baseTargetSpeed * 0.75, GameConfig.BALL_SPEED * 0.6);
        double maxSpeed = Math.max(baseTargetSpeed * 1.6, GameConfig.BALL_SPEED * 1.6);

        if (newSpeed < 0.0001) {
            newVelocity = direction.multiply(minSpeed);
            newSpeed = minSpeed;
        } else if (newSpeed < minSpeed) {
            newVelocity = newVelocity.normalize().multiply(minSpeed);
            newSpeed = minSpeed;
        } else if (newSpeed > maxSpeed) {
            newVelocity = newVelocity.normalize().multiply(maxSpeed);
            newSpeed = maxSpeed;
        }

        setPhysicsVelocity(newVelocity.getX(), newVelocity.getY());
    }
    
    /**
     * Корректирует направление движения, чтобы избежать кирпичей на пути к ракетке
     */
    private Point2D adjustDirectionToAvoidBricks(Point2D ballCenter, Point2D baseDirection, Point2D paddleCenter) {
        List<Entity> brickEntities = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK);
        if (brickEntities == null || brickEntities.isEmpty()) {
            return baseDirection;
        }

        // 1. Анализируем поле кирпичей
        double maxBrickY = 0;
        double minBrickY = Double.MAX_VALUE;
        boolean pathBlocked = false;
        double ballRadius = getCurrentBallRadius();
        double safetyMargin = ballRadius * 3.0; // Увеличенный запас безопасности
        
        // Собираем активные кирпичи
        List<Entity> activeBricks = new java.util.ArrayList<>();
        
        for (Entity brick : brickEntities) {
            if (brick == null || !brick.isActive()) continue;
            
            Brick brickComponent = brick.getComponent(Brick.class);
            if (brickComponent == null || brickComponent.getHealth() <= 0) continue;
            
            activeBricks.add(brick);

            double brickBottom = brick.getY() + brick.getHeight();
            double brickTop = brick.getY();
            
            if (brickBottom > maxBrickY) {
                maxBrickY = brickBottom;
            }
            if (brickTop < minBrickY) {
                minBrickY = brickTop;
            }
            
            // Проверяем, блокирует ли этот кирпич прямой путь вниз
            if (brick.getY() > ballCenter.getY()) {
                double brickLeft = brick.getX();
                double brickRight = brick.getX() + brick.getWidth();
                
                // Если мяч вертикально над кирпичом (с запасом)
                if (ballCenter.getX() >= brickLeft - ballRadius - 10 && 
                    ballCenter.getX() <= brickRight + ballRadius + 10) {
                    pathBlocked = true;
                }
            }
        }

        // 2. Если мяч значительно ниже всех кирпичей, идем напрямую
        if (ballCenter.getY() > maxBrickY + 20 || activeBricks.isEmpty()) {
            return baseDirection;
        }

        // 3. Если мяч выше всех кирпичей и путь не заблокирован, идем напрямую
        if (ballCenter.getY() < minBrickY - safetyMargin && !pathBlocked) {
            return baseDirection;
        }

        // 4. Логика обхода препятствий через боковые каналы
        double gameWidth = GameConfig.GAME_WIDTH;
        
        // Определяем безопасные зоны у стен (каналы для прохода)
        double channelWidth = 40.0; // Ширина безопасного канала у стены
        double channelOffset = 120.0; // Отступ от края экрана (увеличен для меньшего горизонтального смещения)
        double leftChannelX = channelOffset;
        double rightChannelX = gameWidth - channelOffset;
        
        // Выбираем ближайший канал
        double distToLeft = Math.abs(ballCenter.getX() - leftChannelX);
        double distToRight = Math.abs(ballCenter.getX() - rightChannelX);
        
        double targetChannelX;
        boolean isLeftChannel;
        
        if (distToLeft < distToRight) {
            targetChannelX = leftChannelX;
            isLeftChannel = true;
        } else {
            targetChannelX = rightChannelX;
            isLeftChannel = false;
        }
        
        // Проверяем, находимся ли мы уже в безопасном канале
        boolean inChannel = Math.abs(ballCenter.getX() - targetChannelX) < channelWidth * 0.6;
        
        if (inChannel && ballCenter.getY() > maxBrickY + 10) {
            // Мы в канале и ниже всех кирпичей - можем идти напрямую к ракетке
            return baseDirection;
        }
        
        if (inChannel) {
            // Мы в канале, но еще на уровне кирпичей или выше
            // Проверяем, свободен ли путь вниз в канале
            boolean channelClear = true;
            
            for (Entity brick : activeBricks) {
                // Проверяем кирпичи, которые ниже мяча
                if (brick.getY() > ballCenter.getY()) {
                    double brickLeft = brick.getX();
                    double brickRight = brick.getX() + brick.getWidth();
                    
                    // Если кирпич пересекает наш канал
                    if ((brickLeft < targetChannelX + channelWidth / 2.0) && 
                        (brickRight > targetChannelX - channelWidth / 2.0)) {
                        channelClear = false;
                        break;
                    }
                }
            }
            
            if (channelClear) {
                // Канал свободен - движемся вниз к ракетке, оставаясь в канале
                // Небольшая коррекция по X, чтобы оставаться по центру канала
                double correctionX = (targetChannelX - ballCenter.getX()) * 0.2;
                double targetY = paddleCenter.getY() - ballCenter.getY();
                
                return new Point2D(correctionX, targetY).normalize();
            } else {
                // В канале есть препятствия - прижимаемся к стене
                double wallX = isLeftChannel ? channelOffset : gameWidth - channelOffset;
                return new Point2D(wallX - ballCenter.getX(), 3.0).normalize();
            }
        } else {
            // Мы еще не в канале - нужно туда попасть
            
            // Проверяем, есть ли кирпичи на пути к каналу
            double minX = Math.min(ballCenter.getX(), targetChannelX);
            double maxX = Math.max(ballCenter.getX(), targetChannelX);
            
            // Находим самый высокий кирпич на пути к каналу
            double highestBrickTop = Double.MAX_VALUE;
            boolean bricksInHorizontalPath = false;
            
            for (Entity brick : activeBricks) {
                double brickLeft = brick.getX();
                double brickRight = brick.getX() + brick.getWidth();
                double brickTop = brick.getY();
                
                // Если кирпич пересекает горизонтальный путь к каналу
                if ((brickLeft < maxX + ballRadius * 2) && (brickRight > minX - ballRadius * 2)) {
                    bricksInHorizontalPath = true;
                    if (brickTop < highestBrickTop) {
                        highestBrickTop = brickTop;
                    }
                }
            }
            
            if (bricksInHorizontalPath) {
                // Есть кирпичи на горизонтальном пути
                double clearanceNeeded = highestBrickTop - safetyMargin;
                
                if (ballCenter.getY() > clearanceNeeded) {
                    // Мы слишком низко - нужно подняться
                    // Движемся почти вертикально вверх с небольшим смещением к каналу
                    double horizontalComponent = (targetChannelX - ballCenter.getX()) * 0.15;
                    return new Point2D(horizontalComponent, -1.0).normalize();
                } else {
                    // Мы достаточно высоко - движемся горизонтально к каналу
                    // Держимся чуть выше, чтобы не задевать кирпичи
                    return new Point2D(targetChannelX - ballCenter.getX(), -0.1).normalize();
                }
            } else {
                // Путь к каналу свободен - движемся горизонтально
                return new Point2D(targetChannelX - ballCenter.getX(), 0.0).normalize();
            }
        }
    }
    
    /**
     * Остановить мяч (для завершения уровня)
     */
    public void stopBall() {
        setPhysicsVelocity(0, 0);
        // System.out.println("Мяч остановлен для завершения уровня");
    }
    
    public void pauseForCountdown() {
        if (attachedToPaddle || pausedForCountdown || physics == null) {
            return;
        }
        pausedForCountdown = true;
        pausedMaintainConstantSpeed = maintainConstantSpeed;
        maintainConstantSpeed = false;

        pausedBodyType = attachedToPaddle ? BodyType.KINEMATIC : BodyType.DYNAMIC;

        try {
            pausedVelocityX = physics.getLinearVelocity().getX();
            pausedVelocityY = physics.getLinearVelocity().getY();
        } catch (IllegalStateException e) {
            pausedVelocityX = 0.0;
            pausedVelocityY = 0.0;
        }

        setPhysicsState(0, 0, BodyType.KINEMATIC);
    }

    public void resumeAfterCountdown() {
        if (!pausedForCountdown || physics == null) {
            return;
        }

        maintainConstantSpeed = pausedMaintainConstantSpeed;

        if (attachedToPaddle) {
            setPhysicsState(0, 0, BodyType.KINEMATIC);
        } else {
            setPhysicsState(pausedVelocityX, pausedVelocityY, pausedBodyType != null ? pausedBodyType : BodyType.DYNAMIC);
        }

        pausedForCountdown = false;
    }

    public boolean isPausedForCountdown() {
        return pausedForCountdown;
    }

    public double getPausedVelocityX() {
        return pausedVelocityX;
    }

    public double getPausedVelocityY() {
        return pausedVelocityY;
    }

    public void restorePauseState(boolean paused, double pVelX, double pVelY) {
        this.pausedForCountdown = paused;
        this.pausedVelocityX = pVelX;
        this.pausedVelocityY = pVelY;
        if (paused) {
            // If we are restoring to a paused state, make sure the ball is physically frozen.
            setPhysicsState(0, 0, BodyType.KINEMATIC);
        }
    }
    
    private void onBallLost() {
        // Удаляем мяч из списка прикрепленных при потере
        removeAttachedBall(entity);
        
        // Если это дополнительный мяч, просто удаляем его без потери жизни
        if (isExtraBall) {
        // System.out.println("Дополнительный мяч упал - удаляем без потери жизни");
            entity.removeFromWorld();
            return;
        }
        
        // Уведомляем LivesManager о потере основного мяча
        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
        if (app != null && app.getLivesManager() != null) {
            app.getLivesManager().loseLife();
            // LivesManager сам удалит мяч и создаст новый
        }
    }
    
    private void explodeNearbyBricks(Entity hitBrick, int explosionDamage) {
        // Воспроизводим звук взрыва
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/explosion.wav");
        } catch (Exception e) {
        // System.out.println("Не удалось воспроизвести звук взрыва: " + e.getMessage());
        }
        
        // Взрываем кирпичи в радиусе 1 клетки
        double explosionRadius = GameConfig.BRICK_WIDTH * 1.5;
        double brickCenterX = hitBrick.getCenter().getX();
        double brickCenterY = hitBrick.getCenter().getY();
        
        var allBricks = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK);
        for (Entity brick : allBricks) {
            if (brick == hitBrick) continue;
            
            double distance = Math.sqrt(
                Math.pow(brick.getCenter().getX() - brickCenterX, 2) + 
                Math.pow(brick.getCenter().getY() - brickCenterY, 2)
            );
            
            if (distance <= explosionRadius) {
                Brick brickComponent = brick.getComponent(Brick.class);
                if (brickComponent != null) {
                    brickComponent.takeDamage(explosionDamage, false); // Не воспроизводим звук для взорванных кирпичей
                }
            }
        }
    }
    
    // ========== МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ЭФФЕКТАМИ ==========
    
    /**
     * Включить/выключить липкую ракетку
     */
    public void setStickyEnabled(boolean enabled) {
        this.isStickyEnabled = enabled;
        // System.out.println("Липкая ракетка: " + (enabled ? "включена" : "выключена"));
    }
    
    /**
     * Установить множитель скорости
     */
    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = multiplier;
        // System.out.println("Множитель скорости установлен: " + multiplier);
    }
    
    /**
     * Включить/выключить энергетический мяч
     */
    public void setEnergyBall(boolean enabled) {
        this.isEnergyBall = enabled;
        updateBallTexture();
        // System.out.println("Энергетический мяч: " + (enabled ? "включен" : "выключен"));
    }
    
    /**
     * Разрушить кирпич без изменения траектории мяча (для энергетических мячей)
     */
    private void destroyBrick(Entity brick) {
        Brick brickComponent = brick.getComponent(Brick.class);
        if (brickComponent != null) {
            // Начисляем очки за кирпич
            int points = 100;
            FXGL.inc("score", points);
        // System.out.println("+ " + points + " очков за кирпич!");
            
            // Воспроизводим звук разрушения кирпича
            try {
                ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).playBrickHitSound();
            } catch (Exception e) {}
            
            // Используем метод destroy() из Brick для анимации разрушения
            brickComponent.destroy();
            
        // System.out.println("Энергетический мяч разрушил кирпич!");
        }
    }
    
    /**
     * Включить/выключить взрывной мяч
     */
    public void setExplosionBall(boolean enabled) {
        this.isExplosionBall = enabled;
        updateBallTexture();
        // System.out.println("Взрывной мяч: " + (enabled ? "включен" : "выключен"));
    }
    
    /**
     * Включить/выключить слабый мяч
     */
    public void setWeakBall(boolean enabled) {
        this.isWeakBall = enabled;
        
        // Изменяем размер мяча
        if (enabled) {
            sizeMultiplier = 0.7; // Уменьшаем мяч до 70% от обычного размера
            // System.out.println("Слабый мяч: включен, размер уменьшен до 70%");
        } else {
            sizeMultiplier = 1.0; // Возвращаем обычный размер
            // System.out.println("Слабый мяч: выключен, размер восстановлен");
        }
        
        // Обновляем текстуру и размер мяча
        updateBallSize();
    }
    
    /**
     * Включить/выключить хаотичный мяч
     */
    public void setChaoticBall(boolean enabled) {
        this.isChaoticBall = enabled;
        // System.out.println("Хаотичный мяч: " + (enabled ? "включен" : "выключен"));
    }
    
    /**
     * Сбросить все эффекты мяча
     */
    public void resetEffects() {
        isStickyEnabled = false;
        speedMultiplier = 1.0;
        isEnergyBall = false;
        isExplosionBall = false;
        isWeakBall = false;
        isChaoticBall = false;
        sizeMultiplier = 1.0; // Восстанавливаем обычный размер
        // НЕ сбрасываем isExtraBall - дополнительный мяч должен оставаться дополнительным
        launchTime = 0; // Сбрасываем время запуска
        maintainConstantSpeed = true; // Включаем невесомость по умолчанию
        targetSpeed = GameConfig.BALL_SPEED; // Сбрасываем целевую скорость
        
        // Обновляем размер и текстуру мяча
        updateBallSize();
        // System.out.println("Все эффекты мяча сброшены, невесомость включена");
    }
    
    /**
     * Установить флаг дополнительного мяча
     */
    public void setExtraBall(boolean isExtraBall) {
        this.isExtraBall = isExtraBall;
        if (isExtraBall) {
        // System.out.println("Мяч отмечен как дополнительный - не будет тратить жизнь при падении");
            // Обновляем текстуру для дополнительного мяча
            updateBallTexture();
        }
    }
    
    /**
     * Проверить, является ли мяч дополнительным
     */
    public boolean isExtraBall() {
        return isExtraBall;
    }
    
    /**
     * Применить хаотичное поведение к мячу
     */
    private void applyChaoticBehavior(double tpf) {
        if (physics == null) {
            return;
        }
        
        double currentVelX;
        double currentVelY;
        try {
            currentVelX = physics.getLinearVelocity().getX();
            currentVelY = physics.getLinearVelocity().getY();
        } catch (IllegalStateException e) {
            return;
        }
        double currentSpeed = Math.sqrt(currentVelX * currentVelX + currentVelY * currentVelY);
        
        // Постоянные случайные импульсы для хаотичного движения
        double chaosForce = 50.0; // Сила хаотичного воздействия
        
        // Добавляем случайные силы в разных направлениях
        double randomForceX = (Math.random() - 0.5) * 2.0 * chaosForce;
        double randomForceY = (Math.random() - 0.5) * 2.0 * chaosForce;
        
        // Применяем силы к скорости
        double newVelX = currentVelX + randomForceX * tpf;
        double newVelY = currentVelY + randomForceY * tpf;
        
        // Ограничиваем максимальную скорость, чтобы мяч не улетал слишком быстро
        double maxChaosSpeed = currentSpeed * 1.5; // Максимум в 1.5 раза быстрее обычной скорости
        double newSpeed = Math.sqrt(newVelX * newVelX + newVelY * newVelY);
        
        if (newSpeed > maxChaosSpeed) {
            double scale = maxChaosSpeed / newSpeed;
            newVelX *= scale;
            newVelY *= scale;
        }
        
        // Иногда резко меняем направление (10% шанс каждые 0.5 секунды)
        if (Math.random() < 0.01 * tpf * 60) { // 1% шанс каждый кадр при 60 FPS
            // Полностью случайное направление
            double randomAngle = Math.random() * 2 * Math.PI;
            double targetSpeed = Math.max(currentSpeed * 0.7, GameConfig.BALL_SPEED * 0.8); // Минимальная скорость
            newVelX = Math.cos(randomAngle) * targetSpeed;
            newVelY = Math.sin(randomAngle) * targetSpeed;
            
        // System.out.println("🌀 Хаотичный мяч резко сменил направление!");
        }
        
        // Периодические "кружения" - добавляем вращательную силу
        if (Math.random() < 0.05) { // 5% шанс каждый кадр
            double centerX = GameConfig.GAME_WIDTH / 2.0;
            double centerY = GameConfig.GAME_HEIGHT / 2.0;
            double ballX = entity.getX() + entity.getWidth() / 2;
            double ballY = entity.getY() + entity.getHeight() / 2;
            
            // Вектор от центра к мячу
            double toCenterX = centerX - ballX;
            double toCenterY = centerY - ballY;
            double distance = Math.sqrt(toCenterX * toCenterX + toCenterY * toCenterY);
            
            if (distance > 100) { // Если мяч не слишком близко к центру
                // Перпендикулярная сила для создания кругового движения
                double tangentX = -toCenterY / distance;
                double tangentY = toCenterX / distance;
                double spiralForce = 30.0;
                
                newVelX += tangentX * spiralForce * tpf;
                newVelY += tangentY * spiralForce * tpf;
            }
        }
        
        // Применяем новую скорость
        setPhysicsVelocity(newVelX, newVelY);
    }
    
    /**
     * Поддерживает постоянную скорость мяча в невесомости
     */
    private void maintainConstantSpeed(double tpf) {
        if (physics == null) {
            return;
        }
        
        double currentVelX;
        double currentVelY;
        try {
            currentVelX = physics.getLinearVelocity().getX();
            currentVelY = physics.getLinearVelocity().getY();
        } catch (IllegalStateException e) {
            return;
        }
        
        // Вычисляем текущую скорость
        double currentSpeed = Math.sqrt(currentVelX * currentVelX + currentVelY * currentVelY);
        
        // Если мяч движется (скорость больше минимального порога)
        if (currentSpeed > 10.0) {
            // Вычисляем коэффициент для достижения целевой скорости
            double targetSpeedWithMultiplier = targetSpeed * speedMultiplier;
            double speedRatio = targetSpeedWithMultiplier / currentSpeed;
            
            // Плавная корректировка скорости для избежания резких изменений
            double smoothingFactor = 0.95; // Плавность корректировки
            speedRatio = 1.0 + (speedRatio - 1.0) * (1.0 - smoothingFactor);
            
            // Применяем корректировку скорости, сохраняя направление
            double newVelX = currentVelX * speedRatio;
            double newVelY = currentVelY * speedRatio;
            
            // Проверяем, что скорость не стала слишком малой
            double newSpeed = Math.sqrt(newVelX * newVelX + newVelY * newVelY);
            if (newSpeed > targetSpeedWithMultiplier * 0.8) {
                setPhysicsVelocity(newVelX, newVelY);
            }
        }
    }

    private void handleContinuousBrickCollisions() {
        if (physics == null) {
            return;
        }

        List<Entity> brickEntities = FXGL.getGameWorld().getEntitiesByType(EntityType.BRICK);
        if (brickEntities == null || brickEntities.isEmpty()) {
            return;
        }

        double startX = lastCenterX;
        double startY = lastCenterY;
        double endX = entity.getCenter().getX();
        double endY = entity.getCenter().getY();

        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double distance = Math.hypot(deltaX, deltaY);

        double currentRadius = getCurrentBallRadius();

        // Проверяем, вышел ли мяч из последнего обработанного кирпича
        if (lastProcessedBrick != null && lastProcessedBrick.isActive()) {
            if (!circleIntersectsBrick(endX, endY, currentRadius, lastProcessedBrick)) {
                // Мяч вышел из кирпича - сбрасываем флаг
                lastProcessedBrick = null;
            }
        } else {
            // Кирпич был уничтожен или неактивен - сбрасываем флаг
            lastProcessedBrick = null;
        }

        // Уменьшаем шаг проверки для более точного обнаружения столкновений
        // Используем минимум между текущим радиусом мяча * 0.2 и минимальным размером кирпича / 5
        // Уменьшаем коэффициент для более частых проверок с многоударными кирпичами
        double samplingStep = Math.min(
            currentRadius * 0.2,
            Math.min(GameConfig.BRICK_WIDTH, GameConfig.BRICK_HEIGHT) * 0.2
        );
        samplingStep = Math.max(0.5, samplingStep); // Минимум 0.5 пикселя между проверками
        
        // Всегда делаем хотя бы одну проверку, даже при микроскопическом перемещении
        // Это предотвращает "проспать" контакт, когда мяч почти не сдвинулся
        int samples;
        if (distance < samplingStep * 0.5) {
            // При очень малом перемещении делаем одну проверку в конечной точке
            samples = 1;
        } else {
            // Увеличиваем количество проверок для быстрых мячей
            samples = Math.max(2, (int) Math.ceil(distance / samplingStep));
            // Ограничиваем максимальное количество проверок для производительности
            samples = Math.min(samples, 60); // Увеличиваем лимит до 60 для лучшей точности
        }
        
        double stepX = distance > 0.0001 ? deltaX / samples : 0.0;
        double stepY = distance > 0.0001 ? deltaY / samples : 0.0;

        Set<Entity> alreadyHit = new HashSet<>();
        List<Entity> bricksSnapshot = new java.util.ArrayList<>(brickEntities);

        // Проверяем каждую точку траектории в порядке от начала к концу
        for (int i = 1; i <= samples; i++) {
            double sampleX = startX + stepX * i;
            double sampleY = startY + stepY * i;

            // Проверяем все кирпичи на этой точке траектории
            for (Entity brick : bricksSnapshot) {
                if (brick == null || alreadyHit.contains(brick) || !brick.isActive()) {
                    continue;
                }
                
                // Улучшенная логика для предотвращения двойного урона:
                // Пропускаем только если это последний обработанный кирпич И мяч движется ОТ него
                if (lastProcessedBrick == brick) {
                    // Проверяем направление движения относительно центра кирпича
                    double brickCenterX = brick.getCenter().getX();
                    double brickCenterY = brick.getCenter().getY();
                    
                    // Вектор от центра кирпича к текущей позиции мяча
                    double toBallX = endX - brickCenterX;
                    double toBallY = endY - brickCenterY;
                    
                    // Скалярное произведение с направлением движения
                    // Если положительное - мяч движется от кирпича, если отрицательное - к кирпичу
                    double dotProduct = toBallX * deltaX + toBallY * deltaY;
                    
                    if (dotProduct > 0) {
                        // Мяч движется от кирпича - пропускаем чтобы избежать двойного урона
                        continue;
                    }
                    // Мяч движется к кирпичу или параллельно - проверяем столкновение
                }
                
                Brick brickComponent = brick.getComponent(Brick.class);
                if (brickComponent == null || brickComponent.getHealth() <= 0) {
                    continue;
                }

                if (circleIntersectsBrick(sampleX, sampleY, currentRadius, brick)) {
                    // Сохраняем точку столкновения для правильного расчета отскока
                    double collisionX = sampleX;
                    double collisionY = sampleY;
                    
                    if (!isEnergyBall) {
                        // Находим безопасную позицию ПЕРЕД точкой столкновения
                        double safeFactor = Math.max(0.0, ((double) i - 1.5) / samples);
                        double safeX = startX + deltaX * safeFactor;
                        double safeY = startY + deltaY * safeFactor;

                        int attempts = 0;
                        int maxAttempts = Math.max(samples, 30); // Увеличено до 30 попыток
                        while (circleIntersectsBrick(safeX, safeY, currentRadius, brick) && attempts < maxAttempts) {
                            safeFactor = Math.max(0.0, safeFactor - 1.0 / maxAttempts);
                            safeX = startX + deltaX * safeFactor;
                            safeY = startY + deltaY * safeFactor;
                            attempts++;
                        }

                        if (circleIntersectsBrick(safeX, safeY, currentRadius, brick)) {
                            // Если все еще пересекаемся, используем более агрессивный откат
                            double moveLength = Math.hypot(deltaX, deltaY);
                            if (moveLength > 0.0001) {
                                double unitX = deltaX / moveLength;
                                double unitY = deltaY / moveLength;
                                // Отодвигаем мяч назад вдоль траектории на 2.5 радиуса для гарантии
                                safeX = collisionX - unitX * currentRadius * 2.5;
                                safeY = collisionY - unitY * currentRadius * 2.5;
                            } else {
                                safeX = startX;
                                safeY = startY;
                            }
                        }

                        // Позиционируем мяч на безопасной позиции
                        positionBallAtCenter(safeX, safeY);
                        
                        // Вычисляем и применяем отскок СРАЗУ после позиционирования
                        applyBrickBounce(brick, collisionX, collisionY, deltaX, deltaY);
                    }

                    alreadyHit.add(brick);
                    
                    // Наносим урон кирпичу (используем уже полученный brickComponent)
                    int actualDamage = isWeakBall ? 1 : 2;
                    brickComponent.takeDamage(actualDamage);
                    
                    if (isExplosionBall) {
                        explodeNearbyBricks(brick, actualDamage);
                    }
                    
                    lastProcessedBrick = brick;

                    // КРИТИЧНО: Только энергетические мячи могут проходить сквозь кирпичи
                    // Обычные и слабые мячи должны останавливаться после первого столкновения
                    // Это предотвращает прохождение мяча сквозь колонны многоударных кирпичей
                    if (!isEnergyBall) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Применяет отскок мяча от кирпича используя точку столкновения
     */
    private void applyBrickBounce(Entity brick, double collisionX, double collisionY, double moveX, double moveY) {
        if (physics == null) {
            return;
        }
        
        double brickCenterX = brick.getCenter().getX();
        double brickCenterY = brick.getCenter().getY();
        double ballRadius = getCurrentBallRadius();
        double brickWidth = brick.getWidth();
        double brickHeight = brick.getHeight();
        
        // Используем точку столкновения для определения стороны удара
        double deltaX = collisionX - brickCenterX;
        double deltaY = collisionY - brickCenterY;
        
        // Определяем, с какой стороны произошло столкновение
        double overlapX = (brickWidth / 2 + ballRadius) - Math.abs(deltaX);
        double overlapY = (brickHeight / 2 + ballRadius) - Math.abs(deltaY);
        
        double currentVelX;
        double currentVelY;
        try {
            currentVelX = physics.getLinearVelocity().getX();
            currentVelY = physics.getLinearVelocity().getY();
        } catch (IllegalStateException e) {
            return;
        }
        
        // Если пересечение по X меньше, чем по Y - столкновение с боковой стороной
        if (overlapX < overlapY) {
            // Столкновение с левой или правой стороной - отражаем горизонтальную скорость
            double randomFactor = 0.98 + Math.random() * 0.04;
            setPhysicsVelocity(-currentVelX * randomFactor, currentVelY);
        } else {
            // Столкновение с верхней или нижней стороной - отражаем вертикальную скорость
            double randomFactor = 0.98 + Math.random() * 0.04;
            setPhysicsVelocity(currentVelX, -currentVelY * randomFactor);
        }
    }

    private boolean circleIntersectsBrick(double cx, double cy, double radius, Entity brick) {
        double left = brick.getX();
        double top = brick.getY();
        double right = left + brick.getWidth();
        double bottom = top + brick.getHeight();

        double closestX = clamp(cx, left, right);
        double closestY = clamp(cy, top, bottom);

        double diffX = cx - closestX;
        double diffY = cy - closestY;

        return diffX * diffX + diffY * diffY <= radius * radius;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private void positionBallAtCenter(double centerX, double centerY) {
        if (entity == null) {
            return;
        }

        double radius = getCurrentBallRadius();
        double newX = centerX - radius;
        double newY = centerY - radius;

        entity.setPosition(newX, newY);
        lastCenterX = centerX;
        lastCenterY = centerY;

        if (physics != null) {
            Point2D targetPoint = new Point2D(newX, newY);
            try {
                physics.overwritePosition(targetPoint);
            } catch (IllegalStateException e) {
                physics.setOnPhysicsInitialized(() -> physics.overwritePosition(targetPoint));
            }
        }
    }

    private double getCurrentBallRadius() {
        double multiplier = Math.max(0.1, sizeMultiplier);
        return GameConfig.BALL_RADIUS * multiplier;
    }

    private void checkBossCollision() {
        var bosses = FXGL.getGameWorld().getEntitiesByType(EntityType.BOSS);
        if (bosses.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastBossHitTime < BOSS_HIT_COOLDOWN_MS) {
            return;
        }

        var ballBB = entity.getBoundingBoxComponent();
        for (Entity boss : bosses) {
            if (!boss.isActive()) {
                continue;
            }
            if (!ballBB.isCollidingWith(boss.getBoundingBoxComponent())) {
                continue;
            }

            Boss bossComponent = boss.getComponentOptional(Boss.class).orElse(null);
            if (bossComponent == null || bossComponent.isDefeated()) {
                continue;
            }

            bossComponent.takeDamage(getBossDamage());
            reflectFromBoss(boss);
            lastBossHitTime = now;
            break;
        }
    }

    private void reflectFromBoss(Entity boss) {
        if (physics != null) {
            double velocityX;
            double velocityY;
            try {
                velocityX = physics.getLinearVelocity().getX();
                velocityY = physics.getLinearVelocity().getY();
            } catch (IllegalStateException e) {
                return;
            }

            double ballCenterX = entity.getCenter().getX();
            double ballCenterY = entity.getCenter().getY();
            double bossCenterX = boss.getCenter().getX();
            double bossCenterY = boss.getCenter().getY();

            double diffX = ballCenterX - bossCenterX;
            double diffY = ballCenterY - bossCenterY;

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // боковой удар
                double newVelX = -Math.copySign(Math.max(Math.abs(velocityX), GameConfig.BALL_SPEED * 0.6), diffX);
                setPhysicsVelocity(newVelX, velocityY);
                if (diffX > 0) {
                    entity.setX(boss.getX() + boss.getWidth() + 2);
                } else {
                    entity.setX(boss.getX() - entity.getWidth() - 2);
                }
            } else {
                double newVelY = -Math.copySign(Math.max(Math.abs(velocityY), GameConfig.BALL_SPEED * 0.6), diffY);
                setPhysicsVelocity(velocityX, newVelY);
                if (diffY > 0) {
                    entity.setY(boss.getY() + boss.getHeight() + 2);
                } else {
                    entity.setY(boss.getY() - entity.getHeight() - 2);
                }
            }
        }
    }

    /**
     * Включить/выключить поддержание постоянной скорости (невесомость)
     */
    public void setMaintainConstantSpeed(boolean enabled) {
        this.maintainConstantSpeed = enabled;
        // System.out.println("Поддержание постоянной скорости (невесомость): " + (enabled ? "включено" : "выключено"));
    }
    
    /**
     * Установить целевую скорость для невесомости
     */
    public void setTargetSpeed(double speed) {
        this.targetSpeed = speed;
        // System.out.println("Целевая скорость установлена: " + speed);
    }
    
    /**
     * Обновить текстуру мяча в зависимости от активных эффектов
     */
    private void updateBallTexture() {
        String textureName;
        
        // Приоритет эффектов: энергетический > взрывной > дополнительный > обычный
        if (isEnergyBall) {
            textureName = "energy_ball.png";
        } else if (isExplosionBall) {
            textureName = "explosion_ball.png";
        } else if (isExtraBall) {
            textureName = "extra_ball.png"; // Текстура дополнительного мяча
        } else {
            textureName = "ball.png"; // Обычная текстура
        }
        
        // Обновляем текстуру мяча с учетом множителя размера
        try {
            int ballSize = (int) (GameConfig.BALL_RADIUS * 2 * sizeMultiplier);
            var newTexture = TextureUtils.loadScaledTexture(textureName, ballSize);
            entity.getViewComponent().clearChildren();
            entity.getViewComponent().addChild(newTexture);
        // System.out.println("Текстура мяча изменена на: " + textureName + " (размер: " + ballSize + ")");
        } catch (Exception e) {
        // System.out.println("Не удалось загрузить текстуру мяча: " + textureName + " - " + e.getMessage());
            // В случае ошибки возвращаемся к обычной текстуре
            try {
                int ballSize = (int) (GameConfig.BALL_RADIUS * 2 * sizeMultiplier);
                var defaultTexture = TextureUtils.loadScaledTexture("ball.png", ballSize);
                entity.getViewComponent().clearChildren();
                entity.getViewComponent().addChild(defaultTexture);
            } catch (Exception e2) {
        // System.out.println("Критическая ошибка: не удалось загрузить даже стандартную текстуру мяча!");
            }
        }
    }
    
    /**
     * Обновить размер мяча (используется при активации/деактивации слабого мяча)
     */
    private void updateBallSize() {
        // Обновляем текстуру с новым размером
        updateBallTexture();
        
        // Обновляем физический размер мяча (bounding box)
        if (entity != null && entity.getBoundingBoxComponent() != null) {
            // Используем transform scale для изменения физического размера
            entity.setScaleX(sizeMultiplier);
            entity.setScaleY(sizeMultiplier);
        // System.out.println("Физический размер мяча обновлен: scale=" + sizeMultiplier);
        }
    }
    
    // Геттеры для проверки состояния эффектов
    public boolean isStickyEnabled() { return isStickyEnabled; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public boolean isEnergyBall() { return isEnergyBall; }
    public boolean isExplosionBall() { return isExplosionBall; }
    public boolean isWeakBall() { return isWeakBall; }
    public boolean isChaoticBall() { return isChaoticBall; }
    public boolean isMaintainConstantSpeed() { return maintainConstantSpeed; }
    public double getTargetSpeed() { return targetSpeed; }
}
