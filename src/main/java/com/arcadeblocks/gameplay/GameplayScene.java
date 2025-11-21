package com.arcadeblocks.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.scene.SubScene;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.levels.LevelManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Игровая сцена
 */
public class GameplayScene extends SubScene {
    
    private ArcadeBlocksApp app;
    private LevelManager levelManager;
    
    private Label scoreLabel;
    private Label levelLabel;
    private Label livesLabel;
    
    private int score = 0;
    private int lives = GameConfig.INITIAL_LIVES;
    private int currentLevel = 1;
    
    public GameplayScene(ArcadeBlocksApp app) {
        this.app = app;
        this.levelManager = new LevelManager();
        
        initializeUI();
        startGame();
    }
    
    private void initializeUI() {
        // HUD элементы
        VBox hudBox = new VBox(10);
        hudBox.setAlignment(Pos.TOP_RIGHT);
        hudBox.setTranslateX(GameConfig.GAME_WIDTH - 300);
        hudBox.setTranslateY(20);
        
        // Информация об уровне
        levelLabel = new Label("Уровень: 1");
        levelLabel.setFont(Font.font("Orbitron", 16));
        levelLabel.setTextFill(Color.WHITE);
        
        // Счет
        scoreLabel = new Label("Очки: 0");
        scoreLabel.setFont(Font.font("Orbitron", 16));
        scoreLabel.setTextFill(Color.WHITE);
        
        // Жизни
        livesLabel = new Label("Жизни: " + lives);
        livesLabel.setFont(Font.font("Orbitron", 16));
        livesLabel.setTextFill(Color.WHITE);
        
        hudBox.getChildren().addAll(levelLabel, scoreLabel, livesLabel);
        getContentRoot().getChildren().add(hudBox);
    }
    
    private void startGame() {
        // TODO: Создание ракетки и мяча
        // TODO: Загрузка первого уровня
        loadLevel(1);
    }
    
    private void loadLevel(int levelNumber) {
        currentLevel = levelNumber;
        // TODO: Загрузка уровня через LevelManager
        levelLabel.setText("Уровень: " + levelNumber);
    }
}
