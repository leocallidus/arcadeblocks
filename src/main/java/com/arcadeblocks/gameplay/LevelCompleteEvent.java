package com.arcadeblocks.gameplay;

/**
 * Событие завершения уровня
 */
public class LevelCompleteEvent {
    private final int levelNumber;
    private final int score;
    private final int lives;
    
    public LevelCompleteEvent(int levelNumber, int score, int lives) {
        this.levelNumber = levelNumber;
        this.score = score;
        this.lives = lives;
    }
    
    public int getLevelNumber() {
        return levelNumber;
    }
    
    public int getScore() {
        return score;
    }
    
    public int getLives() {
        return lives;
    }
}
