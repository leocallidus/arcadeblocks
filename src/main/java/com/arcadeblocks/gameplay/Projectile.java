package com.arcadeblocks.gameplay;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.arcadeblocks.config.GameConfig;

/**
 * Компонент снаряда
 */
public class Projectile extends Component {
    
    private PhysicsComponent physics;
    private double speed = 800; // Увеличена скорость для лучшей дальности
    private int damage = 1;
    private Double pendingVelocityX;
    private Double pendingVelocityY;
    private String owner = "player";
	private long spawnTimeMs = 0L;
	private static final long IGNORE_PADDLE_COLLISION_MS = 150; // игнорируем столкновение с ракеткой первые миллисекунды
    
    public Projectile() {
        // Фиксируем время создания для юнит‑тестов, где компонент не добавляется к сущности
        this.spawnTimeMs = System.currentTimeMillis();
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
		spawnTimeMs = System.currentTimeMillis();
        if (physics != null) {
            // Используем setOnPhysicsInitialized для безопасной установки скорости
            physics.setOnPhysicsInitialized(() -> {
                applyInitialVelocity();
            });
        }
    }
    
    @Override
    public void onUpdate(double tpf) {
        // Удалить снаряд, если он достиг верхней панели игрового мира или вышел за пределы экрана снизу
        double topBoundary = GameConfig.TOP_UI_HEIGHT;
        if (entity.getY() < topBoundary || entity.getY() > GameConfig.GAME_HEIGHT + 50) {
            entity.removeFromWorld();
        }
        
        // Удалить снаряд, если он вышел за пределы экрана по бокам
        if (entity.getX() < -50 || entity.getX() > GameConfig.GAME_WIDTH + 50) {
            entity.removeFromWorld();
        }
    }
    
    public void onPaddleHit(Entity paddle) {
        // Снаряд не должен попадать в платформу, но на всякий случай
        entity.removeFromWorld();
    }
    
    public void onBrickHit(Entity brick) {
        entity.removeFromWorld();
    }
    
    public int getDamage() {
        return damage;
    }

    public void setOwner(String owner) {
        this.owner = owner != null ? owner : "player";
    }

    public String getOwner() {
        return owner;
    }

    public boolean isBossProjectile() {
        return "boss".equalsIgnoreCase(owner);
    }

	/**
	 * Следует ли игнорировать столкновение с ракеткой (сразу после спавна)?
	 */
	public boolean shouldIgnorePaddleCollision() {
		return System.currentTimeMillis() - spawnTimeMs < IGNORE_PADDLE_COLLISION_MS;
	}

    /**
     * Установить стартовую скорость снаряда.
     */
    public void setVelocity(double velocityX, double velocityY) {
        pendingVelocityX = velocityX;
        pendingVelocityY = velocityY;
        if (physics != null) {
            physics.setLinearVelocity(velocityX, velocityY);
        }
    }

    private void applyInitialVelocity() {
        double vx = pendingVelocityX != null ? pendingVelocityX : 0.0;
        double vy = pendingVelocityY != null ? pendingVelocityY : -speed;
        if (physics != null) {
            physics.setLinearVelocity(vx, vy);
        }
    }
}
