package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.localization.LocalizationManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Экран поэмы после уровня 100.
 */
public class PoemView extends StackPane {

    private static final String PLAYER_NAME_PLACEHOLDER = "{PLAYER_NAME}";

    private final ArcadeBlocksApp app;
    private final Text poemText;
    private Timeline scrollTimeline;
    private VBox container;
    private boolean isPaused = false;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private final boolean fromDebugMenu; // Флаг: запущено из debug меню или после уровня 100
    
    // Навигация по кнопкам
    private Button[] controlButtons;
    private int currentButtonIndex = 0;
    // КРИТИЧНО: Сохраняем ссылки на анимации кнопок для их остановки
    private java.util.Map<Button, ScaleTransition> buttonScaleTransitions = new java.util.HashMap<>();

    public PoemView(ArcadeBlocksApp app) {
        this(app, false); // По умолчанию - после уровня 100
    }
    
    public PoemView(ArcadeBlocksApp app, boolean fromDebugMenu) {
        this.app = app;
        this.fromDebugMenu = fromDebugMenu;
        setStyle("-fx-background-color: white;");

        // Получаем имя игрока из SaveManager
        String defaultPlayerName = localizationManager.get("poem.player.default");
        String playerName = defaultPlayerName;
        if (app.getSaveManager() != null) {
            String savedName = app.getSaveManager().getPlayerName();
            if (savedName != null && !savedName.isEmpty()) {
                playerName = savedName;
            }
        }
        if (playerName == null || playerName.isBlank()) {
            playerName = defaultPlayerName;
        }
        
        // Получаем содержимое поэмы из системы локализации
        String poemTemplate = localizationManager.get("poem.content");
        String poemContent = poemTemplate.replace(PLAYER_NAME_PLACEHOLDER, playerName);

        container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(32));
        container.setMaxWidth(900);
        container.setStyle(
            "-fx-background-color: rgba(10, 10, 30, 0.08);" +
            "-fx-background-radius: 18px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 20, 0.4, 0, 0);"
        );

        Label title = new Label(localizationManager.get("poem.title"));
        title.setFont(Font.font("Orbitron", FontWeight.BOLD, 34));
        title.setTextFill(Color.web("#222222"));
        title.setAlignment(Pos.CENTER);

        StackPane poemViewport = new StackPane();
        poemViewport.setPrefSize(container.getMaxWidth() - 40, 520);
        poemViewport.setMinSize(poemViewport.getPrefWidth(), poemViewport.getPrefHeight());
        poemViewport.setMaxSize(poemViewport.getPrefWidth(), poemViewport.getPrefHeight());
        poemViewport.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 12px;");

        Rectangle clip = new Rectangle(poemViewport.getPrefWidth(), poemViewport.getPrefHeight());
        poemViewport.setClip(clip);

        poemText = new Text(poemContent);
        poemText.setFont(Font.font("Orbitron", FontWeight.NORMAL, 20));
        poemText.setFill(Color.web("#111111"));
        poemText.setTextAlignment(TextAlignment.CENTER);
        
        // Устанавливаем ширину для переноса текста
        poemText.setWrappingWidth(poemViewport.getPrefWidth() - 40);
        
        // Добавляем Text напрямую в viewport (как в титрах)
        poemViewport.getChildren().add(poemText);
        
        // ВАЖНО: Устанавливаем выравнивание по верху для корректной анимации
        StackPane.setAlignment(poemText, Pos.TOP_CENTER);

        // Кнопки управления
        HBox buttonBox = createButtonBox();
        
        container.getChildren().addAll(title, poemViewport, buttonBox);
        getChildren().add(container);

        setAlignment(Pos.CENTER);

        // Устанавливаем обработчик клавиатуры для навигации по кнопкам
        setupKeyboardNavigation();
        
        Platform.runLater(this::requestFocus);
        startScroll(poemViewport);

        // Сохраняем текущую музыку перед запуском музыки поэмы
        if (app.getAudioManager() != null) {
            app.getAudioManager().saveCurrentMusic();
            app.getAudioManager().stopMusic();
            app.getAudioManager().playMusic("music/poem.mp3", true);
        }

        ResponsiveLayoutHelper.bindToStage(this, this::adjustLayoutForResolution);
        setUserData("fullScreenOverlay");
    }

    private void startScroll(StackPane viewport) {
        Platform.runLater(() -> {
            if (scrollTimeline != null) {
                scrollTimeline.stop();
            }

            // Форсируем layout для корректного расчета размеров
            viewport.layout();
            
            // Двойной Platform.runLater для гарантии завершения layout
            Platform.runLater(() -> {
                double viewportHeight = viewport.getHeight();
                if (viewportHeight <= 0) {
                    viewportHeight = viewport.getPrefHeight();
                }
                
                // Получаем реальную высоту Text (он автоматически расширяется до нужного размера)
                double textHeight = poemText.getLayoutBounds().getHeight();
                //System.out.println("DEBUG: textHeight from layoutBounds = " + textHeight);
                
                // Используем реальную высоту с небольшим запасом
                double contentHeight = Math.max(textHeight * 1.2, 5000);
                
                // Стартовая позиция - текст начинается с нижнего края viewport (текст невидим)
                // Так как у нас clip, стартуем с позиции = высоте viewport
                double startY = viewportHeight;
                
                // Конечная позиция - текст полностью прокручивается ВВЕРХ (уходит за верхний край)
                // Должен уйти на всю высоту контента + высоту viewport
                double endY = -(contentHeight + viewportHeight);
                
                //System.out.println("DEBUG: viewportHeight = " + viewportHeight);
                //System.out.println("DEBUG: contentHeight = " + contentHeight);
                ////System.out.println("DEBUG: startY = " + startY);
                //System.out.println("DEBUG: endY = " + endY);
                //System.out.println("DEBUG: travelDistance = " + (startY - endY));

                // Устанавливаем начальную позицию (текст за нижним краем)
                poemText.setTranslateY(startY);

                // Замедленная прокрутка для комфортного чтения (200 секунд = 3.33 минуты)
                scrollTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(poemText.translateYProperty(), startY)),
                    new KeyFrame(Duration.seconds(200), new KeyValue(poemText.translateYProperty(), endY))
                );
                // Прокручиваем поэму один раз (не циклично, как титры)
                scrollTimeline.setCycleCount(1);
                scrollTimeline.play();
            });
        });
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(30);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Кнопка паузы
        final Button[] pauseButtonRef = new Button[1];
        pauseButtonRef[0] = createControlButton("poem.controls.pause", "#f0f0f0", () -> {
            if (scrollTimeline != null) {
                if (!isPaused) {
                    pausePoem();
                    applyControlButtonTranslation(pauseButtonRef[0], "poem.controls.resume");
                } else {
                    resumePoem();
                    applyControlButtonTranslation(pauseButtonRef[0], "poem.controls.pause");
                }
                app.getAudioManager().playSFXByName("menu_select");
            }
        });
        Button pauseButton = pauseButtonRef[0];
        
        // Кнопка "Продолжить"
        Button backButton = createControlButton("poem.controls.proceed", "#f0f0f0", () -> {
            app.getAudioManager().playSFXByName("menu_back");
            stopPoem();
            proceedToNext();
        });
        
        buttonBox.getChildren().addAll(pauseButton, backButton);
        
        // Сохраняем кнопки для навигации
        controlButtons = new Button[]{pauseButton, backButton};
        
        return buttonBox;
    }
    
    private Button createControlButton(String translationKey, String bgColor, Runnable action) {
        Button button = new Button();
        applyControlButtonTranslation(button, translationKey);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        button.setPrefWidth(200);
        button.setTextFill(Color.web("#111111"));
        
        String normalStyle = String.format(
            "-fx-background-color: %s;" +
            "-fx-border-color: #222222;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 10px;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;",
            bgColor
        );
        
        String hoverStyle = String.format(
            "-fx-background-color: #ffffff;" +
            "-fx-border-color: #000000;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 10px;" +
            "-fx-background-radius: 10px;" +
            "-fx-cursor: hand;"
        );
        
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> {
            if (controlButtons != null && button == controlButtons[currentButtonIndex]) {
                button.setStyle(hoverStyle); // Сохраняем hover стиль для выбранной кнопки
            } else {
                button.setStyle(normalStyle);
            }
        });
        button.setOnAction(e -> action.run());
        
        return button;
    }

    private void applyControlButtonTranslation(Button button, String translationKey) {
        button.getProperties().put("translationKey", translationKey);
        button.setText(localizationManager.get(translationKey));
    }
    
    private void setupKeyboardNavigation() {
        this.setFocusTraversable(true);
        
        this.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:
                case UP:
                    moveLeft();
                    event.consume();
                    break;
                case RIGHT:
                case DOWN:
                    moveRight();
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    selectCurrentButton();
                    event.consume();
                    break;
                case ESCAPE:
                    app.getAudioManager().playSFXByName("menu_back");
                    stopPoem();
                    proceedToNext();
                    event.consume();
                    break;
                default:
                    break;
            }
        });
        
        // Устанавливаем hover стиль на первую кнопку
        Platform.runLater(() -> {
            if (controlButtons != null && controlButtons.length > 0) {
                updateButtonHighlight();
            }
        });
    }
    
    private void moveLeft() {
        if (controlButtons != null && currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    private void moveRight() {
        if (controlButtons != null && currentButtonIndex < controlButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    private void selectCurrentButton() {
        if (controlButtons != null && currentButtonIndex >= 0 && currentButtonIndex < controlButtons.length) {
            controlButtons[currentButtonIndex].fire();
        }
    }
    
    private void updateButtonHighlight() {
        if (controlButtons == null) return;
        
        for (int i = 0; i < controlButtons.length; i++) {
            Button button = controlButtons[i];
            if (i == currentButtonIndex) {
                // Выбранная кнопка
                button.setStyle(
                    "-fx-background-color: #ffffff;" +
                    "-fx-border-color: #000000;" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-radius: 10px;" +
                    "-fx-background-radius: 10px;" +
                    "-fx-cursor: hand;"
                );
            } else {
                // Обычная кнопка
                button.setStyle(
                    "-fx-background-color: #f0f0f0;" +
                    "-fx-border-color: #222222;" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-radius: 10px;" +
                    "-fx-background-radius: 10px;" +
                    "-fx-cursor: hand;"
                );
            }
        }
    }
    
    private void pausePoem() {
        if (scrollTimeline != null) {
            scrollTimeline.pause();
            isPaused = true;
        }
    }
    
    private void resumePoem() {
        if (scrollTimeline != null) {
            scrollTimeline.play();
            isPaused = false;
        }
    }
    
    private void stopPoem() {
        if (scrollTimeline != null) {
            scrollTimeline.stop();
        }
    }

    private void proceedToNext() {
        // КРИТИЧНО: Вызываем cleanup() ПЕРЕД переходом
        // Это гарантирует остановку всех анимаций и освобождение ресурсов
        cleanup();
        
        // Восстанавливаем музыку главного меню (если она была сохранена)
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().restorePreviousMusic();
        }
        
        com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(this);
        
        if (fromDebugMenu) {
            // Если запущено из debug меню - возвращаемся в главное меню
            app.showMainMenu();
        } else {
            // Если после уровня 100 - переходим на уровень 101
            // Сюжетное окно главы XII будет показано автоматически при загрузке уровня 101
            int currentLevel = com.almasb.fxgl.dsl.FXGL.geti("level");
            int currentScore = com.almasb.fxgl.dsl.FXGL.geti("score");
            int currentLives = com.almasb.fxgl.dsl.FXGL.geti("lives");
            
            // Обновляем SaveManager перед переходом на следующий уровень
            if (app.getSaveManager() != null && !app.isDebugMode()) {
                app.getSaveManager().setScore(currentScore);
                app.getSaveManager().setLives(currentLives);
                app.getSaveManager().setLevelCompleted(currentLevel, true);
                app.getSaveManager().setTotalLevelsCompleted(app.getSaveManager().getTotalLevelsCompleted() + 1);
                app.getSaveManager().setCurrentLevel(currentLevel + 1);
                app.getSaveManager().autoSaveToActiveSlot();
                app.getSaveManager().awaitPendingWrites();
            }
            
            // Переходим на следующий уровень (101)
            app.startLevel(currentLevel + 1, false);
        }
    }

    private void adjustLayoutForResolution(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());

        if (container != null) {
            StackPane.setAlignment(container, Pos.CENTER);
            StackPane.setMargin(container, new Insets(offsetY, offsetX, offsetY, offsetX));
        }
    }
    
    /**
     * Полная очистка компонента для предотвращения утечек памяти
     */
    public void cleanup() {
        // Останавливаем музыку поэмы
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().stopMusic();
        }
        
        // Останавливаем Timeline прокрутки
        if (scrollTimeline != null) {
            scrollTimeline.stop();
            scrollTimeline = null;
        }
        
        // КРИТИЧНО: Останавливаем все активные анимации кнопок
        if (buttonScaleTransitions != null) {
            for (ScaleTransition transition : buttonScaleTransitions.values()) {
                if (transition != null) {
                    try {
                        transition.stop();
                    } catch (Exception ignored) {}
                }
            }
            buttonScaleTransitions.clear();
            buttonScaleTransitions = null;
        }
        
        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        
        // КРИТИЧНО: Сначала очищаем все обработчики кнопок перед удалением из children
        if (controlButtons != null) {
            for (Button button : controlButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button
                    button.textProperty().unbind();
                }
            }
            controlButtons = null;
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Дополнительная прямая очистка ResponsiveLayoutHelper listeners
        try {
            javafx.stage.Stage stage = com.almasb.fxgl.dsl.FXGL.getPrimaryStage();
            if (stage != null) {
                @SuppressWarnings("unchecked")
                javafx.beans.value.ChangeListener<Number> widthListener = 
                    (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveWidthListener");
                @SuppressWarnings("unchecked")
                javafx.beans.value.ChangeListener<Number> heightListener = 
                    (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveHeightListener");
                
                if (widthListener != null) {
                    stage.widthProperty().removeListener(widthListener);
                    this.getProperties().remove("responsiveWidthListener");
                }
                if (heightListener != null) {
                    stage.heightProperty().removeListener(heightListener);
                    this.getProperties().remove("responsiveHeightListener");
                }
            }
            
            @SuppressWarnings("unchecked")
            javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener = 
                (javafx.beans.value.ChangeListener<javafx.scene.Scene>) this.getProperties().get("responsiveSceneListener");
            if (sceneListener != null) {
                this.sceneProperty().removeListener(sceneListener);
                this.getProperties().remove("responsiveSceneListener");
            }
            
            this.getProperties().remove("responsiveLastWidth");
            this.getProperties().remove("responsiveLastHeight");
        } catch (Exception ignored) {}
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
    }
    
    /**
     * Рекурсивно отвязывает все textProperty() bindings от LocalizationManager
     */
    private void unbindAllTextProperties(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        
        // Отвязываем textProperty() у Labeled компонентов
        if (node instanceof javafx.scene.control.Labeled) {
            ((javafx.scene.control.Labeled) node).textProperty().unbind();
        }
        
        // Рекурсивно обрабатываем дочерние элементы
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                unbindAllTextProperties(child);
            }
        }
    }
}
