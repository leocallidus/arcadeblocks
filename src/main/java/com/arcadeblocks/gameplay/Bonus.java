package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.arcadeblocks.config.GameConfig;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Компонент бонуса
 */
public class Bonus extends Component {
    
    private BonusType bonusType;
    // Скорость падения в пикселях в секунду (нормализована по времени кадра)
    private double fallSpeedPerSecond = 150.0; // 2.5 px * 60 fps
    private boolean isCollected = false;
    private boolean isFadingOut = false;
    private boolean forceInvisibleCapsule = false;
    
    // Магнитные свойства
    private double magneticRadius = 0;
    private double magneticForce = 0;
    
    // Анимации для остановки при удалении
    private FadeTransition currentFadeTransition;
    private ScaleTransition currentScaleTransition;
    
    @Override
    public void onAdded() {
        boolean invisibleFlag = false;
        if (entity != null && entity.getProperties().exists("forceInvisibleCapsule")) {
            invisibleFlag = Boolean.TRUE.equals(entity.getBoolean("forceInvisibleCapsule"));
        }
        forceInvisibleCapsule = invisibleFlag;

        // Инициализируем магнитные свойства в зависимости от типа бонуса
        initializeMagneticProperties();
        if (forceInvisibleCapsule) {
            Node node = getViewNode();
            if (node != null) {
                node.setOpacity(0.0);
                node.setVisible(false);
            }
        } else {
            playSpawnAnimation();
        }
    }
    
    @Override
    public void onRemoved() {
        // Останавливаем все анимации для предотвращения утечек памяти
        stopAllAnimations();
    }
    
    /**
     * Останавливает все активные анимации бонуса
     */
    private void stopAllAnimations() {
        if (currentFadeTransition != null) {
            currentFadeTransition.stop();
            currentFadeTransition = null;
        }
        if (currentScaleTransition != null) {
            currentScaleTransition.stop();
            currentScaleTransition = null;
        }
    }
    
    @Override
    public void onUpdate(double tpf) {
        if (isCollected || isFadingOut) {
            return;
        }
        
        // Проверяем, не упал ли бонус за экран
        if (entity.getY() > GameConfig.GAME_HEIGHT) {
            playOutOfBoundsAnimation();
            return;
        }
        
        // Движение бонуса
        updateMovement(tpf);
        
        // Применяем магнитное поведение
        applyMagneticBehavior(tpf);
    }
    
    private void initializeMagneticProperties() {
        if (bonusType == null) {
            // Если тип еще не установлен, используем значения по умолчанию
            magneticRadius = 70.0;
            magneticForce = 1.5;
            return;
        }
        
        if (bonusType.isPositive()) {
            // Позитивные бонусы отталкиваются от ракетки
            magneticRadius = 70.0; // 50-80 пикселей
            magneticForce = 1.5;   // 1-2 пикселя
        } else {
            // Негативные бонусы притягиваются к ракетке
            magneticRadius = 80.0; // 60-100 пикселей
            magneticForce = 2.5;   // 2-3 пикселя
        }
    }
    
    private void updateMovement(double tpf) {
        // Простое падение вниз
        double currentY = entity.getY();
        entity.setY(currentY + fallSpeedPerSecond * tpf);
    }
    
    private void applyMagneticBehavior(double tpf) {
        // Получаем менеджер эффектов для проверки магнитных состояний
        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
        if (app == null || app.getBonusEffectManager() == null) {
            return;
        }
        
        com.arcadeblocks.gameplay.BonusEffectManager effectManager = app.getBonusEffectManager();
        
        // Проверяем, должен ли этот бонус притягиваться магнитом
        if (!effectManager.shouldAttractBonus(bonusType)) {
            return;
        }
        
        // Находим ракетку
        var paddles = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.PADDLE);
        if (paddles.isEmpty()) {
            return;
        }
        
        Entity paddle = paddles.get(0);
        Point2D paddleCenter = paddle.getCenter();
        Point2D bonusCenter = entity.getCenter();
        
        // Вычисляем расстояние до ракетки
        double distance = paddleCenter.distance(bonusCenter);
        double magnetRadius = effectManager.getMagnetRadius();
        
        // Если бонус в радиусе действия магнита
        if (distance <= magnetRadius) {
            // Вычисляем направление к ракетке
            double deltaX = paddleCenter.getX() - bonusCenter.getX();
            double deltaY = paddleCenter.getY() - bonusCenter.getY();
            
            // Нормализуем направление
            double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (length > 0) {
                deltaX /= length;
                deltaY /= length;
            }
            
            // Применяем магнитную силу
            double magnetForce = effectManager.getMagnetForce();
            double currentX = entity.getX();
            double currentY = entity.getY();
            double newX = currentX + deltaX * magnetForce * tpf * 60; // 60 FPS normalization
            double newY = currentY + deltaY * magnetForce * tpf * 60;
            
            // Ограничиваем движение в пределах экрана
            newX = Math.max(0, Math.min(GameConfig.GAME_WIDTH - entity.getWidth(), newX));
            
            entity.setPosition(newX, newY);
        }
    }
    
    /**
     * Установить тип бонуса
     */
    public void setBonusType(BonusType bonusType) {
        this.bonusType = bonusType;
        initializeMagneticProperties();
    }
    
    /**
     * Получить тип бонуса
     */
    public BonusType getBonusType() {
        return bonusType;
    }
    
    /**
     * Подобрать бонус
     */
    public void collect() {
        if (isCollected) {
            return;
        }
        
        isCollected = true;
        fallSpeedPerSecond = 0;
        
        // Воспроизводим звук подбора бонуса - старые звуки закомментированы
        /*
        try {
            if (bonusType.isPositive()) {
                ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/powerup_good.wav");
            } else {
                // Негативные бонусы
                ((com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp()).getAudioManager().playSFX("sounds/sfx/powerup_bad.wav");
            }
        } catch (Exception e) {
            // Если звук не найден, игнорируем
        }
        */
        
        // Применяем эффект бонуса
        applyBonusEffect();
        
        if (forceInvisibleCapsule) {
            if (entity != null) {
                entity.removeFromWorld();
            }
            return;
        }
        
        // Запускаем анимацию исчезновения и удаляем бонус после неё
        playCollectAnimation();
    }
    
    private void applyBonusEffect() {
        // Получаем менеджер эффектов из основного приложения
        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
        if (app != null) {
            // Add score based on bonus type
            com.arcadeblocks.gameplay.ScoreManager scoreManager = app.getScoreManager();
            if (scoreManager != null) {
                if (bonusType.isPositive()) {
                    scoreManager.addScore(1000);
                } else {
                    scoreManager.addScore(-1000);
                }
            }

            BonusEffectManager effectManager = app.getBonusEffectManager();
            if (effectManager != null) {
                effectManager.applyBonusEffect(bonusType);
            }
        }
    }

    private void playSpawnAnimation() {
        Node node = getViewNode();
        if (node == null) {
            FXGL.runOnce(this::playSpawnAnimation, Duration.millis(16));
            return;
        }

        // Останавливаем предыдущие анимации
        stopAllAnimations();

        node.setOpacity(0.0);
        node.setScaleX(0.65);
        node.setScaleY(0.65);

        currentFadeTransition = new FadeTransition(Duration.millis(220), node);
        currentFadeTransition.setFromValue(0.0);
        currentFadeTransition.setToValue(1.0);
        currentFadeTransition.setInterpolator(Interpolator.EASE_OUT);

        currentScaleTransition = new ScaleTransition(Duration.millis(220), node);
        currentScaleTransition.setFromX(0.65);
        currentScaleTransition.setFromY(0.65);
        currentScaleTransition.setToX(1.0);
        currentScaleTransition.setToY(1.0);
        currentScaleTransition.setInterpolator(Interpolator.EASE_OUT);

        currentFadeTransition.play();
        currentScaleTransition.play();
    }

    private void playCollectAnimation() {
        Node node = getViewNode();
        if (node == null) {
            entity.removeFromWorld();
            return;
        }

        // Останавливаем предыдущие анимации
        stopAllAnimations();

        currentFadeTransition = new FadeTransition(Duration.millis(200), node);
        currentFadeTransition.setFromValue(node.getOpacity());
        currentFadeTransition.setToValue(0.0);
        currentFadeTransition.setInterpolator(Interpolator.EASE_IN);

        currentScaleTransition = new ScaleTransition(Duration.millis(200), node);
        currentScaleTransition.setFromX(node.getScaleX());
        currentScaleTransition.setFromY(node.getScaleY());
        currentScaleTransition.setToX(node.getScaleX() * 1.2);
        currentScaleTransition.setToY(node.getScaleY() * 1.2);
        currentScaleTransition.setInterpolator(Interpolator.EASE_IN);

        currentFadeTransition.setOnFinished(event -> {
            if (entity != null) {
                entity.removeFromWorld();
            }
        });

        currentFadeTransition.play();
        currentScaleTransition.play();
    }

    private void playOutOfBoundsAnimation() {
        if (isFadingOut) {
            return;
        }

        isFadingOut = true;
        fallSpeedPerSecond = 0;

        Node node = getViewNode();
        if (node == null) {
            if (entity != null) {
                entity.removeFromWorld();
            }
            return;
        }

        // Останавливаем предыдущие анимации
        stopAllAnimations();

        currentFadeTransition = new FadeTransition(Duration.millis(250), node);
        currentFadeTransition.setFromValue(node.getOpacity());
        currentFadeTransition.setToValue(0.0);
        currentFadeTransition.setInterpolator(Interpolator.EASE_IN);
        currentFadeTransition.setOnFinished(event -> {
            if (entity != null) {
                entity.removeFromWorld();
            }
        });

        currentFadeTransition.play();
    }

    private Node getViewNode() {
        if (entity == null || entity.getViewComponent().getChildren().isEmpty()) {
            return null;
        }
        return entity.getViewComponent().getChildren().get(0);
    }
    
    /**
     * Проверить, подобран ли бонус
     */
    public boolean isCollected() {
        return isCollected;
    }
    
    /**
     * Установить скорость падения
     */
    public void setFallSpeed(double fallSpeed) {
        // Поддержка старого формата (передавали пиксели за кадр). Если число маленькое, считаем его кадром и нормализуем.
        if (fallSpeed > 0 && fallSpeed <= 10) {
            fallSpeedPerSecond = fallSpeed * 60.0;
        } else {
            fallSpeedPerSecond = fallSpeed;
        }
    }
    
    /**
     * Получить скорость падения
     */
    public double getFallSpeed() {
        return fallSpeedPerSecond;
    }
    
    /**
     * Проверить, находится ли бонус в пределах экрана
     */
    public boolean isOnScreen() {
        return entity.getY() >= 0 && entity.getY() <= GameConfig.GAME_HEIGHT &&
               entity.getX() >= 0 && entity.getX() <= GameConfig.GAME_WIDTH;
    }
}
