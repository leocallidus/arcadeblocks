package com.arcadeblocks.gameplay;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.util.TextureUtils;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

/**
 * Компонент платформы
 */
public class Paddle extends Component {
    
    private PhysicsComponent physics;
    private double speed = GameConfig.PADDLE_SPEED;
    
    // Управление движением извне
    private boolean moveLeft = false;
    private boolean moveRight = false;
    
    // Флаг блокировки движения (для сброса позиции)
    private boolean movementBlocked = false;
    
    // Эффекты изменения размера ракетки
    private double sizeMultiplier = 1.0;
    private double originalWidth = GameConfig.PADDLE_WIDTH;
    private boolean isInvisible = false;
    private static final String DEFAULT_TEXTURE = "paddle.png";
    private String currentTextureName = DEFAULT_TEXTURE;
    
    // Турбо-режим ракетки
    private boolean turboMode = false;
    private double originalSpeed = GameConfig.PADDLE_SPEED;
    
    // Убрано управление мышью
    
    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }
    
    @Override
    public void onUpdate(double tpf) {
        double velocityX = 0;
        
        // Проверяем границы ПЕРЕД движением
        double currentX = entity.getX();
        double entityWidth = entity.getWidth();
        
        // ДИАГНОСТИКА: выводим состояние каждую секунду
        // if (System.currentTimeMillis() % 1000 < 16) {
        //     System.out.println("[Paddle.onUpdate] moveLeft=" + moveLeft + ", moveRight=" + moveRight + ", blocked=" + movementBlocked + ", speed=" + speed);
        // }
        
        // Только клавиатурное управление (если движение не заблокировано)
        if (!movementBlocked) {
            if (moveLeft) {
                if (currentX > 5) { // Небольшой отступ от края
                    velocityX = -speed;
                }
            }
            
            if (moveRight) {
                double rightBoundary = GameConfig.GAME_WIDTH - entityWidth - 5;
                if (currentX < rightBoundary) {
                    velocityX = speed;
                }
            }
        } else {
            // Движение заблокировано - принудительно останавливаем ракетку
            velocityX = 0;
        }
        
        // Устанавливаем скорость через физику
        if (physics != null) {
            physics.setLinearVelocity(velocityX, 0);
            // ДИАГНОСТИКА: выводим когда устанавливаем velocity
            // if (velocityX != 0) {
            //     System.out.println("[Paddle] Setting velocityX=" + velocityX);
            // }
        } else {
            // System.out.println("[Paddle] ERROR: physics is null!");
        }
        
        // Жесткое ограничение позиции (на случай, если физика вынесла за пределы)
        if (currentX <= 0) {
            entity.setX(1);
        } else if (currentX >= GameConfig.GAME_WIDTH - entityWidth) {
            entity.setX(GameConfig.GAME_WIDTH - entityWidth - 1);
        }
    }
    
    public void setMoveLeft(boolean move) {
        this.moveLeft = move;
    }
    
    public void setMoveRight(boolean move) {
        this.moveRight = move;
    }
    
    public void setMovementBlocked(boolean blocked) {
        this.movementBlocked = blocked;
        if (blocked) {
            // Останавливаем ракетку при блокировке
            if (physics != null) {
                physics.setLinearVelocity(0, 0);
            }
        }
    }
    
    public boolean isMovementBlocked() {
        return movementBlocked;
    }
    
    /**
     * Обновить скорость ракетки
     */
    public void setSpeed(double newSpeed) {
        this.speed = newSpeed;
    }
    
    /**
     * Получить текущую скорость ракетки
     */
    public double getSpeed() {
        return speed;
    }
    
    // ========== МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ЭФФЕКТАМИ ==========
    
    /**
     * Установить множитель размера ракетки
     */
    public void setSizeMultiplier(double multiplier) {
        this.sizeMultiplier = Math.max(0.3, Math.min(GameConfig.PADDLE_MAX_SIZE_MULTIPLIER, multiplier)); // Ограничиваем от 30% до 600%
        updatePaddleSize();
        // System.out.println("Размер ракетки изменен: " + (sizeMultiplier * 100) + "%");
    }
    
    /**
     * Получить текущий множитель размера
     */
    public double getSizeMultiplier() {
        return sizeMultiplier;
    }
    
    /**
     * Сбросить размер ракетки к оригинальному
     */
    public void resetSize() {
        this.sizeMultiplier = 1.0;
        updatePaddleSize();
        // System.out.println("Размер ракетки сброшен");
    }
    
    /**
     * Обновить размер ракетки в зависимости от множителя
     */
    private void updatePaddleSize() {
        if (entity != null) {
            double newWidth = originalWidth * sizeMultiplier;
            double newHeight = GameConfig.PADDLE_HEIGHT;
            
            // Обновляем размеры сущности
            entity.setScaleX(sizeMultiplier);
            entity.setScaleY(1.0); // Высота не меняем
            
            // Обновляем bounding box
            entity.getBoundingBoxComponent().clearHitBoxes();
            entity.getBoundingBoxComponent().addHitBox(new com.almasb.fxgl.physics.HitBox(
                com.almasb.fxgl.physics.BoundingShape.box(newWidth, newHeight)
            ));
            
            // Корректируем позицию, чтобы ракетка не выходила за границы
            double currentX = entity.getX();
            double maxX = GameConfig.GAME_WIDTH - newWidth;
            if (currentX > maxX) {
                entity.setX(maxX);
            }
        }
    }

    /**
     * Установить временную текстуру для ракетки.
     */
    public void setTexture(String textureName) {
        if (textureName == null || textureName.isBlank()) {
            textureName = DEFAULT_TEXTURE;
        }
        currentTextureName = textureName;
        if (entity == null) {
            return;
        }
        var texture = TextureUtils.loadScaledTexture(textureName, GameConfig.PADDLE_WIDTH, GameConfig.PADDLE_HEIGHT);
        entity.getViewComponent().clearChildren();
        entity.getViewComponent().addChild(texture);
        // Сохраняем текущий масштаб/opacity эффектов
        entity.setScaleX(sizeMultiplier);
        entity.setScaleY(1.0);
        entity.getViewComponent().setOpacity(isInvisible ? 0.0 : 1.0);
        updatePaddleSize();
    }

    /**
     * Сбросить текстуру к стандартной.
     */
    public void resetTexture() {
        if (!DEFAULT_TEXTURE.equals(currentTextureName)) {
            setTexture(DEFAULT_TEXTURE);
        } else if (entity != null && entity.getViewComponent().getChildren().isEmpty()) {
            setTexture(DEFAULT_TEXTURE);
        }
    }

    /**
     * Получить имя текущей текстуры.
     */
    public String getCurrentTextureName() {
        return currentTextureName;
    }

    public boolean isUsingDefaultTexture() {
        return DEFAULT_TEXTURE.equals(currentTextureName);
    }
    
    /**
     * Включить/выключить невидимость ракетки
     */
    public void setInvisible(boolean invisible) {
        this.isInvisible = invisible;
        if (entity != null) {
            entity.getViewComponent().setOpacity(invisible ? 0.0 : 1.0);
        // System.out.println("Ракетка " + (invisible ? "стала призрачной" : "стала видимой"));
        }
    }
    
    /**
     * Проверить, невидима ли ракетка
     */
    public boolean isInvisible() {
        return isInvisible;
    }
    
    /**
     * Сбросить все эффекты ракетки
     */
    public void resetEffects() {
        setSizeMultiplier(1.0);
        setInvisible(false);
        setMovementBlocked(false);
        setTurboMode(false);
        resetTexture();
        // System.out.println("Все эффекты ракетки сброшены");
    }
    
    // ========== МЕТОДЫ ДЛЯ ТУРБО-РЕЖИМА ==========
    
    /**
     * Включить/выключить турбо-режим ракетки
     */
    public void setTurboMode(boolean turbo) {
        // Изменяем режим только если он действительно изменился
        if (this.turboMode != turbo) {
            this.turboMode = turbo;
            if (turbo) {
                this.speed = originalSpeed * 2.0; // Ускоряем в 2 раза
            } else {
                this.speed = originalSpeed; // Возвращаем обычную скорость
            }
            // System.out.println("Турбо-режим ракетки: " + (turbo ? "включен" : "выключен"));
        }
    }
    
    /**
     * Проверить, включен ли турбо-режим
     */
    public boolean isTurboMode() {
        return turboMode;
    }
    
    /**
     * Переключить турбо-режим
     */
    public void toggleTurboMode() {
        setTurboMode(!turboMode);
    }
    
    // Убраны методы управления мышью
}
