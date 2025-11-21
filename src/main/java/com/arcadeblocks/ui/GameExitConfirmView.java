package com.arcadeblocks.ui;

import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Стилизованное меню подтверждения выхода из игры
 */
public class GameExitConfirmView extends VBox {
    
    private Runnable onExit;
    private Runnable onCancel;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    // КРИТИЧНО: Сохраняем ссылки на Label с эффектами для их очистки
    private Label titleLabel;
    private Label warningLabel;
    
    // Навигация по кнопкам
    private Button[] confirmButtons;
    private int currentButtonIndex = 0;
    // КРИТИЧНО: Сохраняем ссылки на анимации кнопок для их остановки
    private java.util.Map<Button, ScaleTransition> buttonScaleTransitions = new java.util.HashMap<>();
    
    public GameExitConfirmView(Runnable onExit, Runnable onCancel) {
        this.onExit = onExit;
        this.onCancel = onCancel;
        initializeUI();
        ResponsiveLayoutHelper.bindToStage(this);
        setUserData("fullScreenOverlay");
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Обнуляем callbacks ПЕРВЫМИ, чтобы предотвратить их вызов после cleanup
        onExit = null;
        onCancel = null;
        
        // КРИТИЧНО: Останавливаем все активные анимации кнопок и очищаем их handlers
        if (buttonScaleTransitions != null) {
            for (java.util.Map.Entry<Button, ScaleTransition> entry : buttonScaleTransitions.entrySet()) {
                ScaleTransition transition = entry.getValue();
                if (transition != null) {
                    try {
                        // КРИТИЧНО: Очищаем setOnFinished handler перед остановкой, чтобы убрать ссылки на кнопки
                        transition.setOnFinished(null);
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
        if (confirmButtons != null) {
            for (Button button : confirmButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    button.setOnMousePressed(null);
                    button.setOnMouseReleased(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button
                    button.textProperty().unbind();
                }
            }
            confirmButtons = null;
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
        
        // КРИТИЧНО: Очищаем эффекты (DropShadow) перед удалением children
        if (titleLabel != null) {
            titleLabel.setEffect(null);
            titleLabel = null;
        }
        if (warningLabel != null) {
            warningLabel.setEffect(null);
            warningLabel = null;
        }
        
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
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(25);
        setStyle("-fx-background-color: rgba(26, 26, 46, 0.95);"); // Более темный фон для выхода из игры
        setPadding(new Insets(60));
        
        // Заголовок
        titleLabel = new Label();
        localizationManager.bind(titleLabel, "game_exit.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_PINK));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Добавляем эффект свечения к заголовку
        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.web(GameConfig.NEON_PINK));
        glowEffect.setRadius(25);
        glowEffect.setSpread(0.6);
        titleLabel.setEffect(glowEffect);
        
        // Основной текст
        Label messageLabel = new Label();
        localizationManager.bind(messageLabel, "game_exit.message");
        messageLabel.setFont(Font.font("Orbitron", 20));
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setLineSpacing(8);
        
        // Дополнительный текст
        warningLabel = new Label();
        localizationManager.bind(warningLabel, "game_exit.warning");
        warningLabel.setFont(Font.font("Orbitron", 14));
        warningLabel.setTextFill(Color.web(GameConfig.NEON_YELLOW));
        warningLabel.setAlignment(Pos.CENTER);
        warningLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Добавляем эффект предупреждения
        DropShadow warningEffect = new DropShadow();
        warningEffect.setColor(Color.web(GameConfig.NEON_YELLOW));
        warningEffect.setRadius(10);
        warningEffect.setSpread(0.4);
        warningLabel.setEffect(warningEffect);
        
        // Кнопки
        VBox buttonContainer = new VBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        
        // Кнопка "Выйти из игры"
        // КРИТИЧНО: Сохраняем ссылку на callback перед созданием lambda, чтобы избежать утечек
        Runnable exitCallback = onExit;
        Button exitButton = createStyledButton("game_exit.exit", GameConfig.NEON_PINK, () -> {
            playMenuSelectSound();
            // КРИТИЧНО: Вызываем callback перед hide(), чтобы он выполнился до cleanup
            if (exitCallback != null) {
                exitCallback.run();
            }
            hide();
        });
        
        // Кнопка "Отмена"
        // КРИТИЧНО: Сохраняем ссылку на callback перед созданием lambda, чтобы избежать утечек
        Runnable cancelCallback = onCancel;
        Button cancelButton = createStyledButton("game_exit.cancel", GameConfig.NEON_GREEN, () -> {
            playMenuBackSound();
            // КРИТИЧНО: Вызываем callback перед hide(), чтобы он выполнился до cleanup
            if (cancelCallback != null) {
                cancelCallback.run();
            }
            hide();
        });
        
        buttonContainer.getChildren().addAll(exitButton, cancelButton);
        
        // Сохраняем ссылки на кнопки для навигации
        confirmButtons = new Button[]{exitButton, cancelButton};
        
        // Подсказка
        Label hintLabel = new Label();
        localizationManager.bind(hintLabel, "game_exit.hint");
        hintLabel.setFont(Font.font("Orbitron", 12));
        hintLabel.setTextFill(Color.web("#888888"));
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setTextAlignment(TextAlignment.CENTER);
        
        getChildren().addAll(titleLabel, messageLabel, warningLabel, buttonContainer, hintLabel);
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();
        
        // Обработка клавиш
        setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP:
                    moveUp();
                    event.consume();
                    break;
                case DOWN:
                    moveDown();
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    selectCurrentButton();
                    event.consume();
                    break;
                case ESCAPE:
                    playMenuBackSound();
                    // КРИТИЧНО: Сохраняем ссылку на callback перед hide(), чтобы он выполнился до cleanup
                    Runnable cancelCallbackForEsc = onCancel;
                    if (cancelCallbackForEsc != null) {
                        cancelCallbackForEsc.run();
                    }
                    hide();
                    event.consume();
                    break;
                default:
                    // Игнорируем остальные клавиши
                    break;
            }
        });
        setFocusTraversable(true);
    }
    
    private Button createStyledButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setPrefWidth(250);
        button.setPrefHeight(55);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
        // Единый стиль кнопки (как при клавиатурной навигации)
        String buttonStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
        );
        
        button.setStyle(buttonStyle);
        
        // Hover эффекты для мыши (совместимо с клавиатурной навигацией)
        button.setOnMouseEntered(e -> {
            playMenuHoverSound();
            
            // Временное визуальное выделение при наведении мыши
            String hoverStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
            );
            
            // Сохраняем текущий стиль для восстановления
            button.setUserData(button.getStyle());
            button.setStyle(hoverStyle);
            
            // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
            ScaleTransition oldTransition = buttonScaleTransitions.get(button);
            if (oldTransition != null) {
                try {
                    oldTransition.stop();
                } catch (Exception ignored) {}
            }
            
            // Анимация увеличения
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            // КРИТИЧНО: Используем слабую ссылку через final переменную, чтобы избежать утечек
            final Button buttonRef = button;
            scaleUp.setOnFinished(e2 -> {
                if (buttonScaleTransitions != null) {
                    buttonScaleTransitions.remove(buttonRef);
                }
            });
            buttonScaleTransitions.put(button, scaleUp);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
            ScaleTransition oldTransition = buttonScaleTransitions.get(button);
            if (oldTransition != null) {
                try {
                    oldTransition.stop();
                } catch (Exception ignored) {}
                buttonScaleTransitions.remove(button);
            }
            
            // Восстанавливаем исходный стиль
            if (button.getUserData() != null) {
                button.setStyle((String) button.getUserData());
            }
            
            // Анимация уменьшения
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            // КРИТИЧНО: Используем слабую ссылку через final переменную, чтобы избежать утечек
            final Button buttonRef = button;
            scaleDown.setOnFinished(e2 -> {
                if (buttonScaleTransitions != null) {
                    buttonScaleTransitions.remove(buttonRef);
                }
            });
            buttonScaleTransitions.put(button, scaleDown);
            scaleDown.play();
        });
        
        button.setOnMousePressed(e -> {
            button.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                GameConfig.NEON_CYAN + "60", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
            ));
        });
        
        button.setOnMouseReleased(e -> {
            // При отпускании восстанавливаем hover стиль
            if (button.getUserData() != null) {
                button.setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                    GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
                ));
            }
        });
        
        // Обработчик клика
        button.setOnAction(e -> action.run());
        
        return button;
    }
    
    /**
     * Показать меню выхода из игры
     */
    public static void show(Runnable onExit, Runnable onCancel) {
        GameExitConfirmView confirmView = new GameExitConfirmView(onExit, onCancel);
        
        // Добавляем в игровую сцену
        com.almasb.fxgl.dsl.FXGL.getGameScene().addUINode(confirmView);
        
        // Устанавливаем фокус для обработки клавиш
        javafx.application.Platform.runLater(() -> {
            confirmView.requestFocus();
        });
    }
    
    /**
     * Скрыть меню выхода из игры
     */
    public void hide() {
        // Очищаем обработчики перед удалением
        cleanup();
        
        // Удаляем из игровой сцены
        com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(this);
        
        // КРИТИЧНО: Callbacks уже были вызваны в кнопках или в обработчике ESC перед вызовом hide()
    }
    
    /**
     * Воспроизвести звук выбора в меню
     */
    private void playMenuSelectSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_select");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Воспроизвести звук возврата/отмены в меню
     */
    private void playMenuBackSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_back");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Воспроизвести звук наведения в меню
     */
    private void playMenuHoverSound() {
        try {
            ((com.arcadeblocks.ArcadeBlocksApp) com.almasb.fxgl.dsl.FXGL.getApp()).getAudioManager().playSFXByName("menu_hover");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Перемещение вверх по кнопкам
     */
    private void moveUp() {
        if (currentButtonIndex > 0) {
            playMenuHoverSound();
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    /**
     * Перемещение вниз по кнопкам
     */
    private void moveDown() {
        if (currentButtonIndex < confirmButtons.length - 1) {
            playMenuHoverSound();
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    /**
     * Выбор текущей кнопки
     */
    private void selectCurrentButton() {
        if (confirmButtons != null && currentButtonIndex >= 0 && currentButtonIndex < confirmButtons.length) {
            playMenuSelectSound();
            confirmButtons[currentButtonIndex].fire();
        }
    }
    
    /**
     * Обновление визуального выделения кнопок
     */
    private void updateButtonHighlight() {
        if (confirmButtons == null) return;
        
        for (int i = 0; i < confirmButtons.length; i++) {
            Button button = confirmButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка - яркий цвет и увеличенный размер
                button.setStyle(
                    "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-width: 3px; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 18, 1.2, 0, 0);"
                );
                button.setScaleX(1.1);
                button.setScaleY(1.1);
            } else {
                // Обычная кнопка - стандартный стиль
                button.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: " + GameConfig.NEON_PURPLE + "; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_PURPLE + ", 6, 0.4, 0, 0);"
                );
                button.setScaleX(1.0);
                button.setScaleY(1.0);
            }
        }
    }
}
