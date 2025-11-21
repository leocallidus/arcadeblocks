package com.arcadeblocks.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO для сохранения состояния игрового процесса.
 * Поля сделаны публичными и с конструкторами по умолчанию для совместимости с Jackson.
 */
public class GameSnapshot {

    public int level;
    public int score;
    public int lives;
    public double levelTimeSeconds;
    public String currentMusicFile;
    public double currentMusicTime;
    public PaddleState paddle;
    public List<BrickState> bricks = new ArrayList<>();
    public List<BallState> balls = new ArrayList<>();
    public List<BonusEntityState> bonuses = new ArrayList<>();
    public List<ProjectileState> projectiles = new ArrayList<>();
    public List<BossState> bosses = new ArrayList<>();
    public BonusEffectsState bonusEffects;

    public GameSnapshot() {
        // required by Jackson
    }
    
    /**
     * Очистить все списки для освобождения памяти
     */
    public void clear() {
        if (bricks != null) bricks.clear();
        if (balls != null) balls.clear();
        if (bonuses != null) bonuses.clear();
        if (projectiles != null) projectiles.clear();
        if (bosses != null) bosses.clear();
        paddle = null;
        bonusEffects = null;
        currentMusicFile = null;
    }

    public static class PaddleState {
        public double x;
        public double y;
        public double sizeMultiplier;
        public boolean turboMode;
        public boolean invisible;
        public boolean movementBlocked;
        public double speed;

        public PaddleState() {
        }
    }

    public static class BrickState {
        public double x;
        public double y;
        public int health;
        public int maxHealth;
        public int scoreValue;
        public String colorHex;
        public String colorName;

        public BrickState() {
        }
    }

    public static class BallState {
        public double x;
        public double y;
        public double velocityX;
        public double velocityY;
        public double attachedOffsetX;
        public double attachedOffsetY;
        public boolean attachedToPaddle;
        public boolean stickyEnabled;
        public double speedMultiplier;
        public boolean energyBall;
        public boolean explosionBall;
        public boolean weakBall;
        public boolean chaoticBall;
        public boolean extraBall;
        public boolean maintainConstantSpeed;
        public double targetSpeed;

        // Fields for saving pause state
        public boolean pausedForCountdown;
        public double pausedVelocityX;
        public double pausedVelocityY;

        public BallState() {
        }
    }

    public static class BonusEntityState {
        public double x;
        public double y;
        public String bonusType;
        public double fallSpeed;

        public BonusEntityState() {
        }
    }

    public static class ProjectileState {
        public double x;
        public double y;

        public ProjectileState() {
        }
    }

    public static class BossState {
        public String bossId;
        public double x;
        public double y;
        public double health;
        public double maxHealth;
        public boolean spawnCompleted;

        public BossState() {
        }
    }

    public static class BonusEffectsState {
        public boolean bonusMagnetActive;
        public boolean penaltyMagnetActive;
        public double magnetRadius;
        public double magnetForce;
        public boolean plasmaWeaponActive;
        public int plasmaShotsRemaining;
        public boolean chaoticBallsActive;
        public boolean frozenPaddleTextureActive;
        public boolean stickyPaddleTextureActive;
        public int increasePaddleStacks;
        public boolean darknessActive;
        public Map<String, Integer> timers = new HashMap<>();

        public BonusEffectsState() {
        }
    }
}
