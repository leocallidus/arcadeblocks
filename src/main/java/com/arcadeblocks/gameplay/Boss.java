package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.arcadeblocks.ArcadeBlocksApp;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Базовый компонент босса с поддержкой здоровья, анимаций и звуков.
 */
public abstract class Boss extends Component {

    private final String bossId;
    private final double maxHealth;
    private double health;

    private boolean skipSpawnAnimation;
    private boolean spawnCompleted;
    private boolean defeated;
    private boolean defeatNotified;
    private boolean active;

    private double negativeBonusChance = 0.25;
    private Duration spawnDuration = Duration.seconds(5);
    private Duration fadeOutDuration = Duration.seconds(2.5);
    private Duration defeatDelay = Duration.seconds(10);

    private final List<String> hitSounds = new ArrayList<>();
    private String spawnSound;
    private String defeatSound;

    private final Random random = new Random();
    
    // Анимации для остановки при удалении
    private FadeTransition currentFadeTransition;
    private PauseTransition currentPauseTransition;

    protected Boss(String bossId, double maxHealth) {
        this.bossId = bossId;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    @Override
    public void onAdded() {
        showBossHealth();

        if (skipSpawnAnimation || spawnDuration.lessThanOrEqualTo(Duration.ZERO)) {
            spawnCompleted = true;
            active = !defeated;
            onSpawnStarted();
            onSpawnFinished();
            return;
        }

        Node view = resolveBossView();
        if (view != null) {
            view.setOpacity(0.0);
        }

        onSpawnStarted();
        playSound(spawnSound);

        if (view == null) {
            spawnCompleted = true;
            active = !defeated;
            onSpawnFinished();
            return;
        }

        // Останавливаем предыдущие анимации
        stopAllAnimations();
        
        currentFadeTransition = new FadeTransition(spawnDuration, view);
        currentFadeTransition.setFromValue(0.0);
        currentFadeTransition.setToValue(1.0);
        currentFadeTransition.setOnFinished(event -> {
            spawnCompleted = true;
            active = !defeated;
            onSpawnFinished();
        });
        currentFadeTransition.play();
    }

    @Override
    public void onRemoved() {
        // Останавливаем все анимации для предотвращения утечек памяти
        stopAllAnimations();
        hideBossHealth();
    }
    
    /**
     * Останавливает все активные анимации босса
     */
    private void stopAllAnimations() {
        if (currentFadeTransition != null) {
            currentFadeTransition.stop();
            currentFadeTransition = null;
        }
        if (currentPauseTransition != null) {
            currentPauseTransition.stop();
            currentPauseTransition = null;
        }
    }

    @Override
    public void onUpdate(double tpf) {
        if (!active || defeated) {
            return;
        }
        onBossUpdate(tpf);
    }

    /**
     * Нанести урон боссу.
     */
    public void takeDamage(double damage) {
        if (defeated || damage <= 0 || !spawnCompleted) {
            return;
        }

        double previousHealth = health;
        health = Math.max(0.0, Math.min(maxHealth, health - damage));

        updateBossHealthUI();
        onDamageTaken(damage, previousHealth, health);

        playRandomHitSound();
        maybeDropNegativeBonus();

        if (health <= 0.0) {
            triggerDefeat();
        }
    }

    /**
     * Восстановить здоровье для загрузки сохранения.
     */
    public void restoreState(double healthValue, boolean spawnFinished) {
        health = Math.max(0.0, Math.min(healthValue, maxHealth));
        defeated = health <= 0.0;
        spawnCompleted = spawnFinished || skipSpawnAnimation;
        active = spawnCompleted && !defeated;

        Node view = resolveBossView();
        if (view != null) {
            view.setOpacity(defeated ? 0.0 : (spawnCompleted ? 1.0 : view.getOpacity()));
        }

        updateBossHealthUI();
    }

    /**
     * Пропустить анимацию появления (используется при восстановлении из сохранения).
     */
    public void setSkipSpawnAnimation(boolean skip) {
        this.skipSpawnAnimation = skip;
    }

    protected void setSpawnDurationSeconds(double seconds) {
        spawnDuration = Duration.seconds(Math.max(0.0, seconds));
    }

    protected void setFadeOutDurationSeconds(double seconds) {
        fadeOutDuration = Duration.seconds(Math.max(0.0, seconds));
    }

    protected void setDefeatDelaySeconds(double seconds) {
        defeatDelay = Duration.seconds(Math.max(0.0, seconds));
    }

    protected void setNegativeBonusChance(double chance) {
        negativeBonusChance = Math.max(0.0, Math.min(1.0, chance));
    }

    protected void setSpawnSound(String soundPath) {
        this.spawnSound = soundPath;
    }

    protected void setDefeatSound(String soundPath) {
        this.defeatSound = soundPath;
    }

    protected void addHitSound(String soundPath) {
        if (soundPath != null && !soundPath.isBlank()) {
            hitSounds.add(soundPath);
        }
    }

    public String getBossId() {
        return bossId;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void restoreFullHealth() {
        if (defeated) {
            return;
        }
        health = maxHealth;
        updateBossHealthUI();
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean isSpawnCompleted() {
        return spawnCompleted;
    }

    protected boolean isActive() {
        return active && !defeated;
    }

    protected void onSpawnStarted() {
        // Для переопределения в подклассах
    }

    protected void onSpawnFinished() {
        // Для переопределения в подклассах
    }

    protected void onBossUpdate(double tpf) {
        // Для переопределения в подклассах
    }

    protected void onDamageTaken(double damage, double previousHealth, double currentHealth) {
        // Для переопределения
    }

    protected void onDefeated() {
        // Для переопределения
    }

    private void triggerDefeat() {
        if (defeated) {
            return;
        }

        defeated = true;
        active = false;
        updateBossHealthUI();
        playSound(defeatSound);
        onDefeated();

        ArcadeBlocksApp app = getApp();
        if (app != null) {
            app.onBossDefeatSequenceStarted(this);
        }

        // Останавливаем предыдущие анимации
        stopAllAnimations();
        
        Node view = resolveBossView();
        if (view != null) {
            currentFadeTransition = new FadeTransition(fadeOutDuration, view);
            currentFadeTransition.setFromValue(view.getOpacity());
            currentFadeTransition.setToValue(0.0);
            currentFadeTransition.setOnFinished(event -> {
                if (entity != null && entity.isActive()) {
                    entity.removeFromWorld();
                }
            });
            currentFadeTransition.play();
        } else if (entity != null) {
            entity.removeFromWorld();
        }

        currentPauseTransition = new PauseTransition(defeatDelay);
        currentPauseTransition.setOnFinished(event -> notifyBossDefeated());
        currentPauseTransition.play();
    }

    private void notifyBossDefeated() {
        if (defeatNotified) {
            return;
        }
        defeatNotified = true;

        ArcadeBlocksApp app = getApp();
        if (app != null) {
            app.onBossDefeated(this);
        }
    }

    private void showBossHealth() {
        var scoreManager = getScoreManager();
        if (scoreManager != null) {
            scoreManager.showBossHealth(health, maxHealth);
        }
    }

    private void updateBossHealthUI() {
        var scoreManager = getScoreManager();
        if (scoreManager != null) {
            scoreManager.updateBossHealth(health, maxHealth);
        }
    }

    private void hideBossHealth() {
        var scoreManager = getScoreManager();
        if (scoreManager != null) {
            scoreManager.hideBossHealth();
        }
    }

    private void maybeDropNegativeBonus() {
        if (negativeBonusChance <= 0 || entity == null) {
            return;
        }

        if (BonusType.getEnabledNegativeBonusesCount() <= 0) {
            return;
        }

        if (random.nextDouble() > negativeBonusChance) {
            return;
        }

        double spawnX = entity.getCenter().getX() - 31; // центрируем бонус (62x32)
        double spawnY = entity.getY() + entity.getHeight();

        var spawnData = new com.almasb.fxgl.entity.SpawnData(spawnX, spawnY);
        BonusType bonusType = BonusType.getRandomNegativeBonus();
        spawnData.put("bonusType", bonusType);

        var bonusEntity = FXGL.spawn("bonus", spawnData);
        Bonus bonusComponent = new Bonus();
        bonusComponent.setBonusType(bonusType);
        bonusEntity.addComponent(bonusComponent);
    }

    private void playRandomHitSound() {
        if (hitSounds.isEmpty()) {
            return;
        }
        String sound = hitSounds.get(random.nextInt(hitSounds.size()));
        playSound(sound);
    }

    private void playSound(String soundPath) {
        if (soundPath == null || soundPath.isBlank()) {
            return;
        }
        ArcadeBlocksApp app = getApp();
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().playSFX(soundPath);
        }
    }

    private ArcadeBlocksApp getApp() {
        return (ArcadeBlocksApp) FXGL.getApp();
    }

    private ScoreManager getScoreManager() {
        ArcadeBlocksApp app = getApp();
        return app != null ? app.getScoreManager() : null;
    }

    protected Node resolveBossView() {
        if (entity == null || entity.getViewComponent().getChildren().isEmpty()) {
            return null;
        }
        return entity.getViewComponent().getChildren().get(0);
    }
}
