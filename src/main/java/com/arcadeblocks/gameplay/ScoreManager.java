package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.ui.BonusIndicator;
import com.arcadeblocks.config.GameConfig;
import javafx.util.Duration;

/**
 * Менеджер системы очков
 */
public class ScoreManager {
    
    private ArcadeBlocksApp app;
    private int currentScore;
    private BonusIndicator bonusIndicator;
    private String lastBossHealthText = "";
    private int cachedHighScore;
    private boolean highScoreDirty;
    private boolean scoreSaveScheduled;
    private boolean autosaveScheduled;
    private double levelTimerSeconds;
    private int lastDisplayedTimerSeconds = -1;
    private boolean levelTimerRunning;
    private boolean persistenceEnabled = true;
    private static final Duration SCORE_SAVE_DELAY = Duration.millis(250);
    private static final Duration AUTOSAVE_DELAY = Duration.seconds(2);
    
    private void mirrorScoreForDebug() {
        if (app != null && app.isDebugMode()) {
            app.setDebugScoreOverride(currentScore);
        }
    }
    
    public ScoreManager(ArcadeBlocksApp app) {
        this.app = app;
        this.currentScore = 0;
        this.cachedHighScore = 0;
        createScoreUI();
    }
    
    private void createScoreUI() {
        bonusIndicator = new BonusIndicator();
        positionBonusIndicator();
        FXGL.getGameScene().addUINode(bonusIndicator);
        
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateScore(currentScore);
            app.getGameplayUIView().updateLives(FXGL.geti("lives"));
            app.getGameplayUIView().updateLevel(FXGL.geti("level"));
            pushTimeToUI(0);
            lastDisplayedTimerSeconds = 0;
        }
    }
    
    /**
     * Позиционировать индикатор бонусов с учетом разрешения
     */
    private void positionBonusIndicator() {
        if (bonusIndicator == null) {
            return;
        }
        
        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());

        double horizontalMargin = 20;
        double verticalMargin = GameConfig.TOP_UI_HEIGHT + 10 + 100;  // +100 пикселей ниже
        
        // Позиционируем в правом верхнем углу игрового мира
        // Увеличили ширину индикатора с 250 до 300 пикселей для полного отображения текста
        double indicatorWidth = 300;
        double x = offsetX + GameConfig.GAME_WORLD_WIDTH - indicatorWidth - horizontalMargin;
        double y = offsetY + verticalMargin;

        bonusIndicator.relocate(x, y);
        bonusIndicator.setTranslateX(0);
        bonusIndicator.setTranslateY(0);
    }
    
    /**
     * Добавить очки
     */
    public void addScore(int points) {
        currentScore += points;
        updateScoreDisplay();
        mirrorScoreForDebug();

        if (currentScore > cachedHighScore) {
            cachedHighScore = currentScore;
            highScoreDirty = true;
        }

        schedulePersistence();
    }
    
    private void schedulePersistence() {
        if (!persistenceEnabled) {
            return;
        }
        if (app.isDebugMode()) {
            return;
        }
        if (!scoreSaveScheduled) {
            scoreSaveScheduled = true;
            FXGL.runOnce(() -> {
                scoreSaveScheduled = false;
                if (!persistenceEnabled) {
                    return;
                }
                flushScoreToStorage();
            }, SCORE_SAVE_DELAY);
        }

        if (!autosaveScheduled) {
            autosaveScheduled = true;
            FXGL.runOnce(() -> {
                autosaveScheduled = false;
                if (!persistenceEnabled) {
                    return;
                }
                performAutoSave();
            }, AUTOSAVE_DELAY);
        }
    }

    private void flushScoreToStorage() {
        if (app.getSaveManager() == null || app.isDebugMode()) {
            return;
        }

        app.getSaveManager().setScore(currentScore);

        if (highScoreDirty) {
            app.getSaveManager().setHighScore(cachedHighScore);
            highScoreDirty = false;
        }
    }

    private void performAutoSave() {
        // Сохраняем очки в базе данных только если не в debug режиме
        if (app.getSaveManager() != null && !app.isDebugMode()) {
            app.getSaveManager().autoSaveToActiveSlot();
        }
    }
    
    /**
     * Получить текущий счет
     */
    public int getCurrentScore() {
        return currentScore;
    }
    
    /**
     * Установить счет
     */
    public void setScore(int score) {
        this.currentScore = score;
        updateScoreDisplay();
        mirrorScoreForDebug();

        if (score > cachedHighScore) {
            cachedHighScore = score;
            highScoreDirty = true;
        }

        schedulePersistence();
    }

    /**
     * Принудительно записать текущие данные в базу и выполнить автосохранение, если оно ожидало.
     */
    public void flushPendingOperations() {
        scoreSaveScheduled = false;
        if (!app.isDebugMode()) {
            flushScoreToStorage();
        }

        if (autosaveScheduled && !app.isDebugMode()) {
            autosaveScheduled = false;
            performAutoSave();
        }

        if (app.getSaveManager() != null && !app.isDebugMode()) {
            app.getSaveManager().awaitPendingWrites();
        }
    }
    
    /**
     * Сбросить счет
     */
    public void resetScore() {
        this.currentScore = 0;
        updateScoreDisplay();
        mirrorScoreForDebug();
        
        if (app.getSaveManager() != null && !app.isDebugMode()) {
            app.getSaveManager().setScore(0);
        }
    }

    public void setPersistenceEnabled(boolean enabled) {
        this.persistenceEnabled = enabled;
        if (!enabled) {
            scoreSaveScheduled = false;
            autosaveScheduled = false;
        }
    }
    
    public void update(double tpf) {
        if (!levelTimerRunning) {
            return;
        }

        levelTimerSeconds += tpf;
        int seconds = (int) Math.floor(levelTimerSeconds);
        if (seconds != lastDisplayedTimerSeconds) {
            lastDisplayedTimerSeconds = seconds;
            pushTimeToUI(seconds);
        }
    }
    
    public void restartLevelTimer() {
        levelTimerSeconds = 0;
        levelTimerRunning = true;
        pushTimeToUI(0);
        lastDisplayedTimerSeconds = 0;
    }
    
    public void pauseLevelTimer() {
        levelTimerRunning = false;
    }
    
    public void resumeLevelTimer() {
        levelTimerRunning = true;
    }
    
    public void stopLevelTimer() {
        levelTimerRunning = false;
    }
    
    public void setLevelTimerSeconds(double seconds) {
        levelTimerSeconds = Math.max(0, seconds);
        lastDisplayedTimerSeconds = (int) Math.floor(levelTimerSeconds);
        pushTimeToUI(lastDisplayedTimerSeconds);
    }
    
    public double getLevelTimerSeconds() {
        return levelTimerSeconds;
    }
    
    /**
     * Обновить отображение счета
     */
    private void updateScoreDisplay() {
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateScore(currentScore);
        }
    }
    
    private void pushTimeToUI(int totalSeconds) {
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateTime(formatLevelTime(totalSeconds));
        }
    }
    
    private String formatLevelTime(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Обновить отображение уровня
     */
    public void updateLevel(int currentLevel) {
        updateLevel(currentLevel, null);
    }
    
    /**
     * Обновить отображение уровня с названием
     */
    public void updateLevel(int currentLevel, String levelName) {
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateLevel(currentLevel, levelName);
        }
    }

    /**
     * Показать индикатор здоровья босса
     */
    public void showBossHealth(double health, double maxHealth) {
        lastBossHealthText = formatBossHealth(health, maxHealth);
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().showBossHealth(lastBossHealthText);
        }
    }

    /**
     * Обновить здоровье босса
     */
    public void updateBossHealth(double health, double maxHealth) {
        lastBossHealthText = formatBossHealth(health, maxHealth);
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().updateBossHealthText(lastBossHealthText);
        }
    }

    /**
     * Скрыть индикатор здоровья босса
     */
    public void hideBossHealth() {
        if (app.getGameplayUIView() != null) {
            app.getGameplayUIView().hideBossHealth();
        }
    }

    private String formatBossHealth(double health, double maxHealth) {
        double clampedHealth = Math.max(0.0, Math.min(health, maxHealth));
        if (Math.abs(clampedHealth - Math.rint(clampedHealth)) < 0.0001) {
            return "Здоровье босса: " + (int) Math.rint(clampedHealth);
        }
        return String.format("Здоровье босса: %.1f", clampedHealth);
    }
    
    /**
     * Загрузить счет из сохранения
     */
    public void loadFromSave() {
        if (app.isDebugMode()) {
            Integer override = app.getDebugScoreOverride();
            currentScore = override != null ? override : 0;
            cachedHighScore = Math.max(cachedHighScore, currentScore);
            highScoreDirty = false;
            scoreSaveScheduled = false;
            autosaveScheduled = false;
            persistenceEnabled = true;
            updateScoreDisplay();
            setLevelTimerSeconds(0);
            mirrorScoreForDebug();
            return;
        }

        if (app.getSaveManager() != null && !app.isDebugMode()) {
            currentScore = app.getSaveManager().getScore();
            cachedHighScore = app.getSaveManager().getHighScore();
            highScoreDirty = false;
            scoreSaveScheduled = false;
            autosaveScheduled = false;
            persistenceEnabled = true;
            updateScoreDisplay();
            setLevelTimerSeconds(0);
        } else {
            currentScore = 0;
            cachedHighScore = 0;
            highScoreDirty = false;
            scoreSaveScheduled = false;
            autosaveScheduled = false;
            persistenceEnabled = true;
            updateScoreDisplay();
            setLevelTimerSeconds(0);
        }
        mirrorScoreForDebug();
    }
    
    /**
     * Показать индикатор бонуса
     */
    public void showBonus(BonusType bonusType, int durationSeconds) {
        // System.out.println("📊 ScoreManager.showBonus: " + bonusType + " на " + durationSeconds + " секунд");
        if (bonusIndicator != null) {
        // System.out.println("✅ BonusIndicator найден, вызываем showBonus");
            bonusIndicator.showBonus(bonusType, durationSeconds);
        } else {
        // System.out.println("❌ BonusIndicator = null!");
        }
    }
    
    /**
     * Обновить таймер бонуса
     */
    public void updateBonusTimer(BonusType bonusType, int remainingSeconds) {
        if (bonusIndicator != null) {
            bonusIndicator.updateBonusTimer(bonusType, remainingSeconds);
        }
    }
    
    /**
     * Скрыть бонус
     */
    public void hideBonus(BonusType bonusType) {
        if (bonusIndicator != null) {
            bonusIndicator.hideBonus(bonusType);
        }
    }
    
    /**
     * Скрыть все бонусы
     */
    public void hideAllBonuses() {
        if (bonusIndicator != null) {
            bonusIndicator.hideAllBonuses();
        }
    }
    
    
    /**
     * Получить индикатор бонуса
     */
    public BonusIndicator getBonusIndicator() {
        return bonusIndicator;
    }
    
    /**
     * Обновить позицию индикатора бонусов (вызывается при изменении разрешения)
     */
    public void updateBonusIndicatorPosition() {
        positionBonusIndicator();
    }
    
    /**
     * Удалить UI с экрана
     */
    public void removeFromScene() {
        if (bonusIndicator != null && bonusIndicator.getParent() != null) {
            FXGL.getGameScene().removeUINode(bonusIndicator);
        }
    }
}
