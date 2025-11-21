package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Индикатор жизней в левом нижнем углу игрового поля
 */
public class LivesIndicator extends VBox {
    
    private int currentLives;
    private int initialLives;
    private List<ImageView> lifeIcons;
    private VBox container;
    
    public LivesIndicator() {
        this.currentLives = GameConfig.INITIAL_LIVES;
        this.initialLives = GameConfig.INITIAL_LIVES;
        initializeUI();
    }
    
    public LivesIndicator(int lives) {
        this.currentLives = lives;
        this.initialLives = lives;
        initializeUI();
    }
    
    private void initializeUI() {
        setAlignment(Pos.TOP_LEFT);
        setPrefSize(300, 200); // Устанавливаем размер для видимости
        
        // Убираем отладочную рамку
        // setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(
        //     Color.web("#FF0000"), 
        //     javafx.scene.layout.BorderStrokeStyle.SOLID, 
        //     null, 
        //     new javafx.scene.layout.BorderWidths(2)
        // )));
        
        // Создаем контейнер для иконок жизней
        container = new VBox(8); // Промежуток 8 пикселей между иконками
        container.setAlignment(Pos.TOP_LEFT);
        
        // Позиционируем в левом верхнем углу игрового поля
        container.setTranslateX(20); // Отступ от левого края игрового поля
        container.setTranslateY(-20); // Поднято еще на 20 пикселей (было 0, стало -20)
        
        // Создаем иконки жизней используя PNG текстуру
        lifeIcons = new ArrayList<>();
        
        for (int i = 0; i < currentLives; i++) {
            try {
                // Загружаем текстуру жизни
                Image lifeImage = ImageCache.get("life.png");
                ImageView lifeIcon = new ImageView(lifeImage);
                
                // Устанавливаем размер иконки (уменьшаем оригинальную текстуру 600x499 в 1.5 раза)
                lifeIcon.setFitWidth(27); // 40 / 1.5 ≈ 27
                lifeIcon.setFitHeight(22); // 33 / 1.5 ≈ 22
                lifeIcon.setPreserveRatio(true);
                lifeIcon.setSmooth(true);
                
                // Добавляем эффект свечения для неонового вида
                javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                glow.setColor(Color.web("#FF1493", 0.8)); // Неон-розовое свечение
                glow.setRadius(8);
                glow.setSpread(0.3);
                
                javafx.scene.effect.Glow neonGlow = new javafx.scene.effect.Glow();
                neonGlow.setLevel(0.6);
                
                // Комбинируем эффекты
                glow.setInput(neonGlow);
                lifeIcon.setEffect(glow);
                
                lifeIcons.add(lifeIcon);
                container.getChildren().add(lifeIcon);
                
            } catch (Exception e) {
                // Fallback: создаем простую иконку если текстура не найдена
                ImageView fallbackIcon = new ImageView();
                fallbackIcon.setFitWidth(27); // Уменьшено в 1.5 раза
                fallbackIcon.setFitHeight(22); // Уменьшено в 1.5 раза
                fallbackIcon.setStyle("-fx-background-color: rgba(255, 20, 147, 0.8); -fx-background-radius: 20;");
                
                lifeIcons.add(fallbackIcon);
                container.getChildren().add(fallbackIcon);
                
                System.err.println("Не удалось загрузить текстуру жизни: " + e.getMessage());
            }
        }
        
        getChildren().add(container);
    }
    
    /**
     * Потеря жизни
     */
    public void loseLife() {
        if (currentLives > 0) {
            currentLives--;
            
            // Анимация потери жизни - делаем полупрозрачной
            if (currentLives < lifeIcons.size() && currentLives >= 0) {
                ImageView lostLife = lifeIcons.get(currentLives);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), lostLife);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.3); // Полупрозрачность при потере жизни
                fadeOut.play();
            }
        }
    }
    
    /**
     * Создать новую иконку жизни
     */
    private ImageView createLifeIcon() {
        try {
            // Загружаем текстуру жизни
            Image lifeImage = ImageCache.get("life.png");
            ImageView lifeIcon = new ImageView(lifeImage);
            
            // Устанавливаем размер иконки
            lifeIcon.setFitWidth(27);
            lifeIcon.setFitHeight(22);
            lifeIcon.setPreserveRatio(true);
            lifeIcon.setSmooth(true);
            
            // Добавляем эффект свечения для неонового вида
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#FF1493", 0.8)); // Неон-розовое свечение
            glow.setRadius(8);
            glow.setSpread(0.3);
            
            javafx.scene.effect.Glow neonGlow = new javafx.scene.effect.Glow();
            neonGlow.setLevel(0.6);
            
            // Комбинируем эффекты
            glow.setInput(neonGlow);
            lifeIcon.setEffect(glow);
            
            return lifeIcon;
            
        } catch (Exception e) {
            // Fallback: создаем простую иконку если текстура не найдена
            ImageView fallbackIcon = new ImageView();
            fallbackIcon.setFitWidth(27);
            fallbackIcon.setFitHeight(22);
            fallbackIcon.setStyle("-fx-background-color: rgba(255, 20, 147, 0.8); -fx-background-radius: 20;");
            
            System.err.println("Не удалось загрузить текстуру жизни: " + e.getMessage());
            return fallbackIcon;
        }
    }
    
    /**
     * Добавление жизни
     */
    public void addLife() {
        currentLives++;
        
        // Если нужно больше иконок чем есть, создаем новые
        while (lifeIcons.size() < currentLives) {
            ImageView newLifeIcon = createLifeIcon();
            lifeIcons.add(newLifeIcon);
            container.getChildren().add(newLifeIcon);
        }
        
        // Анимация добавления жизни - восстанавливаем полную непрозрачность
        if (currentLives > 0 && currentLives <= lifeIcons.size()) {
            ImageView newLife = lifeIcons.get(currentLives - 1);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newLife);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }
    
    /**
     * Сброс всех жизней
     */
    public void resetLives() {
        currentLives = initialLives;
        
        // Восстанавливаем все иконки жизней
        for (ImageView lifeIcon : lifeIcons) {
            lifeIcon.setOpacity(1.0);
        }
    }
    
    /**
     * Получить текущее количество жизней
     */
    public int getCurrentLives() {
        return currentLives;
    }
    
    /**
     * Проверить, остались ли жизни
     */
    public boolean hasLivesLeft() {
        return currentLives > 0;
    }
    
    /**
     * Установить количество жизней
     */
    public void setLives(int lives) {
        if (lives >= 0) {
            // Если нужно больше иконок чем есть, создаем новые
            while (lifeIcons.size() < lives) {
                ImageView newLifeIcon = createLifeIcon();
                lifeIcons.add(newLifeIcon);
                container.getChildren().add(newLifeIcon);
            }
            
            // Сбрасываем все иконки - делаем их полупрозрачными
            for (ImageView lifeIcon : lifeIcons) {
                lifeIcon.setOpacity(0.3);
            }
            
            // Восстанавливаем нужное количество - делаем их полностью видимыми
            for (int i = 0; i < lives && i < lifeIcons.size(); i++) {
                lifeIcons.get(i).setOpacity(1.0);
            }
            
            currentLives = lives;
        }
    }
    
    /**
     * Анимация появления индикатора
     */
    public void playShowAnimation() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
    
    /**
     * Анимация скрытия индикатора
     */
    public void playHideAnimation() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.play();
    }
    
    /**
     * Очистка ресурсов для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Освобождаем иконки жизней
        if (lifeIcons != null) {
            for (ImageView lifeIcon : lifeIcons) {
                if (lifeIcon != null) {
                    // Освобождаем изображение
                    com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(lifeIcon);
                    // Удаляем эффекты
                    lifeIcon.setEffect(null);
                }
            }
            lifeIcons.clear();
            lifeIcons = null;
        }
        
        // Очищаем контейнер
        if (container != null) {
            container.getChildren().clear();
            container = null;
        }
        
        // Очищаем children
        getChildren().clear();
    }
}
