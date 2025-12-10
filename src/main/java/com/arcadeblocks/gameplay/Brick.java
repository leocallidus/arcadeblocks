package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.arcadeblocks.config.GameConfig;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Компонент кирпича
 */
public class Brick extends Component {

    private static final AtomicInteger ACTIVE_BRICKS = new AtomicInteger();
    private static final boolean DEBUG_LOGS = Boolean.getBoolean("arcadeblocks.debug.brick");

    private int health;
    private int maxHealth;
    private Color color;
    private int scoreValue;
    private final boolean countTowardsCompletion;
    private boolean isDestroyed = false; // Флаг для предотвращения повторного вызова destroy()
    private boolean removalHandled = false;
    private boolean shouldCheckCompletionOnRemoval = false;
    private Node brickNode;
    private boolean isExplosive = false; // Флаг для взрывных кирпичей
    
    // Анимации для остановки при удалении
    private FadeTransition currentFadeTransition;
    private ScaleTransition currentScaleTransition;
    
    public Brick(int health, Color color, int scoreValue) {
        this(health, color, scoreValue, false, true);
    }
    
    public Brick(int health, Color color, int scoreValue, boolean isExplosive) {
        this(health, color, scoreValue, isExplosive, true);
    }

    public Brick(int health, Color color, int scoreValue, boolean isExplosive, boolean countTowardsCompletion) {
        this.health = health;
        this.maxHealth = health;
        this.color = color;
        this.scoreValue = scoreValue;
        this.isExplosive = isExplosive;
        this.countTowardsCompletion = countTowardsCompletion;
    }
    
    @Override
    public void onAdded() {
        if (countTowardsCompletion) {
            ACTIVE_BRICKS.incrementAndGet();
        }
        brickNode = resolveBrickNode();
        updateColor();
    }

    @Override
    public void onRemoved() {
        // Останавливаем все анимации для предотвращения утечек памяти
        stopAllAnimations();
        handleBrickRemoval();
    }
    
    /**
     * Останавливает все активные анимации кирпича
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
    
    public void takeDamage(int damage) {
        takeDamage(damage, true);
    }
    
    public void takeDamage(int damage, boolean playSound) {
        if (health <= 0 || isDestroyed) return; // Уже разрушен или уничтожен
        
        health -= damage;
        
        if (health <= 0) {
            // Если это взрывной кирпич, взрываем соседние кирпичи
            if (isExplosive) {
                explodeNearbyBricks();
            }
            destroy(playSound);
        } else {
            // Анимация получения урона
            playDamageAnimation();
            updateColor();
            if (playSound) {
                try {
                    com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
                    if (app != null && app.getAudioManager() != null) {
                        // Случайный выбор между двумя звуками с 50% вероятностью
                        String soundFile = Math.random() < 0.5 
                            ? "sounds/no_dest_brick1.wav" 
                            : "sounds/no_dest_brick2.wav";
                        app.getAudioManager().playSFX(soundFile);
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки при проигрывании звука
                }
            }
        }
    }
    
    private void playDamageAnimation() {
        // Быстрая анимация "вспышки" при получении урона
        Node viewNode = resolveBrickNode();
        if (viewNode == null) {
            return;
        }

        // Останавливаем предыдущие анимации перед запуском новых
        stopAllAnimations();
        
        currentFadeTransition = new FadeTransition(Duration.millis(100), viewNode);
        currentFadeTransition.setFromValue(1.0);
        currentFadeTransition.setToValue(0.3);
        currentFadeTransition.setAutoReverse(true);
        currentFadeTransition.setCycleCount(2);
        
        // Легкое увеличение размера при ударе
        currentScaleTransition = new ScaleTransition(Duration.millis(100), viewNode);
        currentScaleTransition.setFromX(1.0);
        currentScaleTransition.setFromY(1.0);
        currentScaleTransition.setToX(1.05);
        currentScaleTransition.setToY(1.05);
        currentScaleTransition.setAutoReverse(true);
        currentScaleTransition.setCycleCount(2);
        
        currentFadeTransition.play();
        currentScaleTransition.play();
    }
    
    public void destroy() {
        destroy(true);
    }
    
    public void destroy(boolean playSound) {
        if (isDestroyed) return; // Предотвращаем повторный вызов
        isDestroyed = true;
        shouldCheckCompletionOnRemoval = countTowardsCompletion;
        
        if (playSound) {
            try {
                com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
                if (app != null) {
                    app.playBrickHitSound();
                }
            } catch (Exception e) {
                // Игнорируем ошибки при проигрывании звука
            }
        }
        
        // Добавляем очки за разрушение кирпича
        addScoreForBrick();
        
        // Попытка выпадения бонуса (10% шанс)
        trySpawnBonus();
        
        // Создаем анимацию плавного исчезновения
        playDestroyAnimation();
        
        // Проверяем завершение уровня после завершения анимации (в callback)
    }
    
    /**
     * Тихое разрушение кирпича без звука и проверки завершения уровня
     * Используется для отладочных команд
     */
    public void destroySilently() {
        if (isDestroyed) return; // Предотвращаем повторный вызов
        isDestroyed = true;
        shouldCheckCompletionOnRemoval = false;
        
        // Добавляем очки за разрушение кирпича
        addScoreForBrick();
        
        // Попытка выпадения бонуса (10% шанс)
        trySpawnBonus();
        
        // Создаем анимацию плавного исчезновения
        playDestroyAnimation();
        
        // НЕ проверяем завершение уровня - это будет сделано отдельно в destroyAllBricks()
    }
    
    private void addScoreForBrick() {
        // Добавляем очки за разрушение кирпича
        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
        if (app != null && app.getScoreManager() != null) {
            app.getScoreManager().addScore(scoreValue);
            if (app.getBonusEffectManager() != null && app.getBonusEffectManager().isScoreRainActive()) {
                app.getScoreManager().addScore(com.arcadeblocks.gameplay.BonusEffectManager.SCORE_RAIN_BONUS_POINTS);
            }
        // System.out.println("+ " + scoreValue + " очков за кирпич!");
        }
    }
    
    private void trySpawnBonus() {
        // КРИТИЧНО: На легкой сложности позитивные бонусы падают в 2 раза чаще
        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
        double dropChance = GameConfig.POWERUP_DROP_CHANCE;
        
        // Увеличиваем шанс выпадения позитивных бонусов на легкой сложности
        boolean isEasy = app != null && 
            app.getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.EASY;
        
        // Используем шанс выпадения бонуса из конфига
        if (Math.random() <= dropChance) {
            // Получаем центр кирпича для спавна бонуса
            double brickCenterX = entity.getX() + entity.getWidth() / 2.0;
            double brickCenterY = entity.getY() + entity.getHeight() / 2.0;
            
            // Определяем тип бонуса: на босс-уровнях 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 — только негативные
            BonusType bonusType;
            try {
                // Используем FXGL runtime-переменную уровня, т.к. она всегда актуальна даже в debug
                int currentLevel = com.almasb.fxgl.dsl.FXGL.geti("level");
                if (currentLevel == 10
                    || currentLevel == 20
                    || currentLevel == 30
                    || currentLevel == 40
                    || currentLevel == 50
                    || currentLevel == 60
                    || currentLevel == 70
                    || currentLevel == 80
                    || currentLevel == 90
                    || currentLevel == 100) {
                    bonusType = BonusType.getRandomNegativeBonus();
                } else {
                    // КРИТИЧНО: На легкой сложности позитивные бонусы падают в 2 раза чаще
                    if (isEasy) {
                        // Легкая сложность: 80% позитивные, 20% негативные
                        bonusType = BonusType.getWeightedRandomBonusForEasy();
                    } else {
                        // КРИТИЧНО: На хардкорной сложности негативные бонусы падают в 2 раза чаще
                        boolean isHardcore = app != null && 
                            app.getEffectiveDifficulty() == com.arcadeblocks.config.DifficultyLevel.HARDCORE;
                        
                        if (isHardcore) {
                            // Хардкорная сложность: 66.67% негативные, 33.33% позитивные
                            bonusType = BonusType.getWeightedRandomBonusForHardcore();
                        } else {
                            // Обычная логика: выбираем случайный тип бонуса с учетом весовых коэффициентов
                            // Энергетические и взрывные мячи имеют 3% шанс каждый
                            bonusType = BonusType.getWeightedRandomBonus();
                        }
                    }
                }
            } catch (Exception ex) {
                // На случай любых сбоев — fallback на обычную логику
                bonusType = BonusType.getWeightedRandomBonus();
            }
            
            // Создаем данные для спавна
            com.almasb.fxgl.entity.SpawnData spawnData = new com.almasb.fxgl.entity.SpawnData(
                brickCenterX - 31, // Центрируем бонус (62x32)
                brickCenterY - 16
            );
            spawnData.put("bonusType", bonusType);
            
            // Создаем сущность бонуса
            com.almasb.fxgl.entity.Entity bonus = FXGL.spawn("bonus", spawnData);
            
            // Добавляем компонент бонуса
            Bonus bonusComponent = new Bonus();
            bonusComponent.setBonusType(bonusType);
            bonus.addComponent(bonusComponent);
            
        // System.out.println("Выпал бонус: " + bonusType.getDescription() + 
        //                      " в позиции (" + brickCenterX + ", " + brickCenterY + ")");
        }
    }
    
    private void playDestroyAnimation() {
        Node viewNode = resolveBrickNode();
        if (entity == null) {
            handleBrickRemoval();
            return;
        }

        if (viewNode == null) {
            entity.removeFromWorld();
            handleBrickRemoval();
            return;
        }

        // Останавливаем предыдущие анимации перед запуском новых
        stopAllAnimations();
        
        // Анимация исчезновения с уменьшением размера
        currentFadeTransition = new FadeTransition(Duration.millis(300), viewNode);
        currentFadeTransition.setFromValue(1.0);
        currentFadeTransition.setToValue(0.0);
        
        // Анимация уменьшения размера
        currentScaleTransition = new ScaleTransition(Duration.millis(300), viewNode);
        currentScaleTransition.setFromX(1.0);
        currentScaleTransition.setFromY(1.0);
        currentScaleTransition.setToX(0.1);
        currentScaleTransition.setToY(0.1);
        
        // Запускаем анимации параллельно
        currentFadeTransition.play();
        currentScaleTransition.play();
        
        // Удаляем entity из мира после завершения анимации
        currentFadeTransition.setOnFinished(e -> {
            if (entity != null) {
                entity.removeFromWorld();
            }
            handleBrickRemoval();
        });
    }
    
    private void updateColor() {
        // Изменение цвета в зависимости от здоровья
        double healthPercent = (double) health / maxHealth;
        Color newColor = color.deriveColor(0, 1.0, healthPercent, 1.0);

        Node node = resolveBrickNode();
        if (node instanceof Rectangle) {
            ((Rectangle) node).setFill(newColor);
        }
    }
    
    public int getHealth() {
        return health;
    }
    
    public int getScoreValue() {
        return scoreValue;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public Color getBaseColor() {
        return color;
    }

    public void restoreState(int health) {
        int clamped = Math.max(0, Math.min(health, maxHealth));
        this.health = clamped;
        this.isDestroyed = false;
        this.removalHandled = false;
        this.shouldCheckCompletionOnRemoval = false;
        updateColor();
    }
    
    /**
     * Проверка завершения уровня
     */
    private void checkLevelCompletion() {
        if (!shouldCheckCompletionOnRemoval) {
            return;
        }

        int remaining = ACTIVE_BRICKS.get();
        if (DEBUG_LOGS) {
            // System.out.println("[Brick] checking level completion, remaining bricks: " + remaining);
        }

        if (remaining > 0) {
            return;
        }

        com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp();
        if (app != null && !app.isLevelCompleted() && !app.isTransitioning()) {
            com.almasb.fxgl.dsl.FXGL.runOnce(app::checkLevelCompletion, javafx.util.Duration.millis(100));
        }
    }

    private void handleBrickRemoval() {
        if (removalHandled) {
            return;
        }
        removalHandled = true;

        if (!countTowardsCompletion) {
            return;
        }

        int remaining = ACTIVE_BRICKS.decrementAndGet();
        if (remaining < 0) {
            ACTIVE_BRICKS.set(0);
            remaining = 0;
        }

        if (DEBUG_LOGS) {
            // System.out.println("[Brick] removed. Remaining bricks: " + remaining + ", shouldCheck: " + shouldCheckCompletionOnRemoval);
        }

        // КРИТИЧНО: Сохраняем ссылку на app для fallback проверки завершения уровня
        com.arcadeblocks.ArcadeBlocksApp capturedApp = null;
        try {
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null) {
                capturedApp = app;
                app.onBrickCountChanged(remaining);
            }
        } catch (Exception ignored) {}

        checkLevelCompletion();
        
        // КРИТИЧНО: Fallback проверка завершения уровня, если счётчик стал ≤ 0
        // Это гарантирует переход к завершению уровня даже если shouldCheckCompletionOnRemoval был сброшен
        if (remaining <= 0 && capturedApp != null) {
            final com.arcadeblocks.ArcadeBlocksApp appRef = capturedApp;
            FXGL.runOnce(() -> {
                if (appRef != null) {
                    appRef.checkLevelCompletion();
                }
            }, javafx.util.Duration.ZERO);
        }
    }

    private Node resolveBrickNode() {
        if (brickNode == null && entity != null && !entity.getViewComponent().getChildren().isEmpty()) {
            brickNode = entity.getViewComponent().getChildren().get(0);
        }
        return brickNode;
    }

    public static void resetBrickCounter() {
        ACTIVE_BRICKS.set(0);
    }

    public static int getActiveBrickCount() {
        return ACTIVE_BRICKS.get();
    }

    /**
     * Взрыв соседних кирпичей при разрушении взрывного кирпича
     */
    public void explodeNearbyBricks() {
        // Воспроизводим звук взрыва
        try {
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null) {
                app.getAudioManager().playSFX("sounds/explosion.wav");
            }
        } catch (Exception e) {
            // Игнорируем ошибки при проигрывании звука
        }
        
        // Взрываем 3 кирпича в радиусе
        double explosionRadius = GameConfig.BRICK_WIDTH * 2.0; // Увеличиваем радиус для лучшего эффекта
        double brickCenterX = entity.getCenter().getX();
        double brickCenterY = entity.getCenter().getY();
        
        int explodedBricks = 0;
        var allBricks = FXGL.getGameWorld().getEntitiesByType(com.arcadeblocks.EntityType.BRICK);
        
        // Сортируем кирпичи по расстоянию для взрыва ближайших
        java.util.List<Entity> nearbyBricks = new java.util.ArrayList<>();
        for (Entity brick : allBricks) {
            if (brick == entity) continue; // Не взрываем сам взрывной кирпич
            
            double distance = Math.sqrt(
                Math.pow(brick.getCenter().getX() - brickCenterX, 2) + 
                Math.pow(brick.getCenter().getY() - brickCenterY, 2)
            );
            
            if (distance <= explosionRadius) {
                nearbyBricks.add(brick);
            }
        }
        
        // Взрываем до 3 ближайших кирпичей
        nearbyBricks.sort((b1, b2) -> {
            double d1 = Math.sqrt(
                Math.pow(b1.getCenter().getX() - brickCenterX, 2) + 
                Math.pow(b1.getCenter().getY() - brickCenterY, 2)
            );
            double d2 = Math.sqrt(
                Math.pow(b2.getCenter().getX() - brickCenterX, 2) + 
                Math.pow(b2.getCenter().getY() - brickCenterY, 2)
            );
            return Double.compare(d1, d2);
        });
        
        for (int i = 0; i < Math.min(3, nearbyBricks.size()); i++) {
            Entity brick = nearbyBricks.get(i);
            Brick brickComponent = brick.getComponent(Brick.class);
            if (brickComponent != null && brickComponent.getHealth() > 0) {
                brickComponent.takeDamage(1, false); // Наносим урон 1, без звука
                explodedBricks++;
            }
        }
    }
    
    /**
     * Проверка, является ли кирпич взрывным
     */
    public boolean isExplosive() {
        return isExplosive;
    }
    
    public boolean countsForCompletion() {
        return countTowardsCompletion;
    }
    
    /**
     * Разрушение кирпича плазменным снарядом с особой анимацией и звуком
     */
    public void destroyByPlasma() {
        if (isDestroyed) return;
        isDestroyed = true;
        shouldCheckCompletionOnRemoval = countTowardsCompletion;
        
        // Воспроизводим случайный звук разрушения плазмой
        try {
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null && app.getAudioManager() != null) {
                String soundFile = Math.random() < 0.5 
                    ? "sounds/plasma_brick_hit1.wav" 
                    : "sounds/plasma_brick_hit2.wav";
                app.getAudioManager().playSFX(soundFile);
            }
        } catch (Exception e) {
            // Игнорируем ошибки при проигрывании звука
        }
        
        addScoreForBrick();
        trySpawnBonus();
        playPlasmaDestroyAnimation();
    }
    
    /**
     * Анимация разрушения плазмой - яркая вспышка с быстрым исчезновением
     */
    private void playPlasmaDestroyAnimation() {
        Node viewNode = resolveBrickNode();
        if (entity == null) {
            handleBrickRemoval();
            return;
        }

        if (viewNode == null) {
            entity.removeFromWorld();
            handleBrickRemoval();
            return;
        }

        stopAllAnimations();
        
        // Яркая вспышка перед исчезновением
        viewNode.setOpacity(1.5);
        viewNode.setScaleX(1.2);
        viewNode.setScaleY(1.2);
        
        // Быстрое исчезновение с расширением (эффект "выжигания")
        currentFadeTransition = new FadeTransition(Duration.millis(150), viewNode);
        currentFadeTransition.setFromValue(1.0);
        currentFadeTransition.setToValue(0.0);
        
        currentScaleTransition = new ScaleTransition(Duration.millis(150), viewNode);
        currentScaleTransition.setFromX(1.2);
        currentScaleTransition.setFromY(1.2);
        currentScaleTransition.setToX(1.5);
        currentScaleTransition.setToY(0.3);
        
        currentFadeTransition.play();
        currentScaleTransition.play();
        
        currentFadeTransition.setOnFinished(e -> {
            if (entity != null) {
                entity.removeFromWorld();
            }
            handleBrickRemoval();
        });
    }
    
    /**
     * Разрушение кирпича энергетическим мячом с особой анимацией и звуком
     */
    public void destroyByEnergyBall() {
        if (isDestroyed) return;
        isDestroyed = true;
        shouldCheckCompletionOnRemoval = true;
        
        // Воспроизводим звук разрушения энергетическим мячом
        try {
            com.arcadeblocks.ArcadeBlocksApp app = (com.arcadeblocks.ArcadeBlocksApp) FXGL.getApp();
            if (app != null && app.getAudioManager() != null) {
                app.getAudioManager().playSFX("sounds/energy_ball_brick_hit.wav");
            }
        } catch (Exception e) {
            // Игнорируем ошибки при проигрывании звука
        }
        
        addScoreForBrick();
        trySpawnBonus();
        playEnergyDestroyAnimation();
    }
    
    /**
     * Анимация разрушения энергетическим мячом - электрический эффект растворения
     */
    private void playEnergyDestroyAnimation() {
        Node viewNode = resolveBrickNode();
        if (entity == null) {
            handleBrickRemoval();
            return;
        }

        if (viewNode == null) {
            entity.removeFromWorld();
            handleBrickRemoval();
            return;
        }

        stopAllAnimations();
        
        // Быстрое мерцание и растворение (эффект "электрического разряда")
        currentFadeTransition = new FadeTransition(Duration.millis(200), viewNode);
        currentFadeTransition.setFromValue(1.0);
        currentFadeTransition.setToValue(0.0);
        currentFadeTransition.setCycleCount(1);
        
        // Вибрация по горизонтали при растворении
        currentScaleTransition = new ScaleTransition(Duration.millis(200), viewNode);
        currentScaleTransition.setFromX(1.0);
        currentScaleTransition.setFromY(1.0);
        currentScaleTransition.setToX(0.0);
        currentScaleTransition.setToY(1.3);
        
        currentFadeTransition.play();
        currentScaleTransition.play();
        
        currentFadeTransition.setOnFinished(e -> {
            if (entity != null) {
                entity.removeFromWorld();
            }
            handleBrickRemoval();
        });
    }
}
