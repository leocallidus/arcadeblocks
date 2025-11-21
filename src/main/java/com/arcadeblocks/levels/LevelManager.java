package com.arcadeblocks.levels;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.config.LevelConfig;
import com.arcadeblocks.config.GameConfig;
import javafx.scene.paint.Color;

/**
 * Менеджер уровней
 */
public class LevelManager {
    
    private int currentLevelNumber;
    private LevelConfig.LevelData currentLevelData;
    
    public LevelManager() {
        this.currentLevelNumber = 0;
    }
    
    /**
     * Загрузить уровень
     */
    public void loadLevel(int levelNumber) {
        currentLevelNumber = levelNumber;
        currentLevelData = LevelConfig.getLevel(levelNumber);
        
        if (currentLevelData == null) {
            System.err.println("Уровень " + levelNumber + " не найден!");
            return;
        }
        
        // TODO: Создание кирпичей и боссов
        // TODO: Установка фона и музыки
    }
    
    /**
     * Очистить уровень
     */
    public void clearLevel() {
        // TODO: Удалить все сущности уровня
    }
    
    /**
     * Проверка завершения уровня
     */
    public boolean isLevelCompleted() {
        // TODO: Проверка, что все кирпичи уничтожены
        return false;
    }
    
    /**
     * Получить данные текущего уровня
     */
    public LevelConfig.LevelData getCurrentLevelData() {
        return currentLevelData;
    }
    
    /**
     * Получить номер текущего уровня
     */
    public int getCurrentLevelNumber() {
        return currentLevelNumber;
    }
}
