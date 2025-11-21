package com.arcadeblocks.bosses;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.gameplay.Boss;
import com.arcadeblocks.gameplay.Projectile;

/**
 * Босс "FIREWALL-7"
 * Простое горизонтальное движение и одиночные снаряды.
 */
public class Firewall7 extends Boss {

    private static final double MOVE_SPEED = 150.0;
    private static final double LEFT_MARGIN = 100.0;
    private static final double RIGHT_MARGIN = 100.0;

    private double direction = 1.0;
    private double shootCooldown = 0.0;
    private boolean abilityEnabled = false;
    private double leftBound;
    private double rightBound;

    public Firewall7() {
        super("FIREWALL-7", GameConfig.FIREWALL_7_HP);
        setSpawnDurationSeconds(5.0);
        setFadeOutDurationSeconds(2.5);
        setDefeatDelaySeconds(10.0);
        setNegativeBonusChance(0.3);

        // Boss sounds removed
    }

    @Override
    protected void onSpawnFinished() {
        if (entity != null) {
            leftBound = LEFT_MARGIN;
            rightBound = GameConfig.GAME_WIDTH - entity.getWidth() - RIGHT_MARGIN;
            rightBound = Math.max(leftBound, rightBound);
        }
        shootCooldown = GameConfig.BOSS_SHOOT_INTERVAL;
        abilityEnabled = true;
    }

    @Override
    protected void onBossUpdate(double tpf) {
        if (!abilityEnabled || entity == null) {
            return;
        }

        updateMovement(tpf);
        updateShooting(tpf);
    }

    @Override
    protected void onDefeated() {
        abilityEnabled = false;
    }

    private void updateMovement(double tpf) {
        double dx = direction * MOVE_SPEED * tpf;
        entity.translateX(dx);

        double currentX = entity.getX();
        if (currentX <= leftBound) {
            entity.setX(leftBound);
            direction = 1.0;
        } else if (currentX >= rightBound) {
            entity.setX(rightBound);
            direction = -1.0;
        }
    }

    private void updateShooting(double tpf) {
        shootCooldown -= tpf;
        if (shootCooldown <= 0.0) {
            fireProjectile();
            shootCooldown = GameConfig.BOSS_SHOOT_INTERVAL;
        }
    }

    private void fireProjectile() {
        double shotX = entity.getX() + entity.getWidth() / 2.0 - 2;
        double shotY = entity.getY() + entity.getHeight() - 4;

        SpawnData data = new SpawnData(shotX, shotY);
        data.put("owner", "boss");
        Entity projectile = FXGL.spawn("projectile", data);
        Projectile projectileComponent = projectile.getComponent(Projectile.class);
        if (projectileComponent != null) {
            projectileComponent.setVelocity(0, GameConfig.BOSS_PROJECTILE_SPEED);
        }

        ArcadeBlocksApp app = (ArcadeBlocksApp) FXGL.getApp();
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().playSFX("sounds/laser_shot.wav");
        }
    }
}
