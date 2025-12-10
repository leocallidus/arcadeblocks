package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Экран паузы с управлением клавиатурой
 */
public class PauseView extends VBox implements SupportsCleanup {
    
    private final ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    
    // Навигация по меню
    private Button[] menuButtons;
    private int currentButtonIndex = 0;
    private FadeTransition fadeIn;
    private ScaleTransition scaleIn;
    // КРИТИЧНО: Флаг для предотвращения повторных нажатий кнопки "Главное меню"
    private volatile boolean isReturningToMainMenu = false;
    
    public PauseView(ArcadeBlocksApp app) {
        this.app = app;
        initializeUI();
        setupKeyHandler();
        ResponsiveLayoutHelper.bindToStage(this);
        setUserData("fullScreenOverlay");
        // Курсор показывается из логики приложения при входе в паузу
        
        // Устанавливаем фокус с задержкой, чтобы гарантировать, что окно добавлено в сцену
        javafx.application.Platform.runLater(() -> {
            this.requestFocus();
        });
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимации появления
        if (fadeIn != null) {
            fadeIn.stop();
            fadeIn = null;
        }
        if (scaleIn != null) {
            scaleIn.stop();
            scaleIn = null;
        }
        
        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        
        // КРИТИЧНО: Сначала очищаем обработчики и отвязываем textProperty() биндинги
        // ПЕРЕД обнулением menuButtons, чтобы избежать утечек памяти от StringBinding
        if (menuButtons != null) {
            for (Button button : menuButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button перед обнулением массива
                    // Это предотвращает утечки памяти от StringBinding, которые создаются через localizationManager.bind()
                    button.textProperty().unbind();
                }
            }
            // Только после отвязки всех биндингов обнуляем массив
            menuButtons = null;
        }
        
        // КРИТИЧНО: Отвязываем textProperty() у всех Label компонентов перед удалением children
        // Это предотвращает утечки памяти от StringBinding, если они были привязаны
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Освобождаем изображения перед удалением children
        // Это предотвращает утечки памяти от com.sun.prism.image.* буферов
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // Отвязываем слушателей ResponsiveLayoutHelper
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Дополнительная прямая очистка ResponsiveLayoutHelper listeners
        try {
            javafx.stage.Stage stage = FXGL.getPrimaryStage();
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
     * Рекурсивно отвязывает все textProperty() bindings в дереве компонентов
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
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                unbindAllTextProperties(child);
            }
        }
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setStyle("-fx-background-color: rgba(26, 26, 46, 0.9);");
        setPadding(new Insets(50));
        
        // Заголовок
        Label titleLabel = createTitleLabel();
        
        // Меню кнопок
        VBox menuBox = createMenuBox();
        
        getChildren().addAll(titleLabel, menuBox);
        
        // Анимация появления
        playAppearAnimation();
    }
    
    private Label createTitleLabel() {
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "pause.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        
        // Эффект свечения
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 20, 0.5, 0, 0);");
        
        return titleLabel;
    }
    
    private VBox createMenuBox() {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        
        // Кнопка "Продолжить" (первая)
        Button resumeButton = createMenuButton("pause.button.resume", GameConfig.NEON_GREEN, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            resumeGame();
        });
        
        // Кнопка "Настройки" - ОТКЛЮЧЕНА
        Button settingsButton = createMenuButton("pause.button.settings", GameConfig.NEON_PURPLE, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            showSettings();
        });
        // ВРЕМЕННО ВКЛЮЧЕНО: Кнопка настроек доступна
        // settingsButton.setDisable(true);
        // settingsButton.setOpacity(0.5);
        
        // Кнопка "Справка"
        Button helpButton = createMenuButton("pause.button.help", GameConfig.NEON_ORANGE, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            showHelp();
        });
        
        // Кнопка "Главное меню" (последняя)
        Button mainMenuButton = createMenuButton("pause.button.main_menu", GameConfig.NEON_YELLOW, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            returnToMainMenu();
        });
        // КРИТИЧНО: Сохраняем ссылку на кнопку для последующего отключения
        final Button mainMenuButtonRef = mainMenuButton;
        // КРИТИЧНО: Переопределяем обработчик, чтобы отключить кнопку после нажатия
        mainMenuButton.setOnAction(e -> {
            // КРИТИЧНО: Отключаем кнопку сразу после нажатия, чтобы предотвратить повторные нажатия
            mainMenuButtonRef.setDisable(true);
            app.getAudioManager().playSFXByName("menu_select");
            returnToMainMenu();
        });
        
        // Кнопка перезапуска уровня
        Button restartButton = createMenuButton("pause.button.restart", GameConfig.NEON_GREEN, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            showRestartConfirm();
        });

        menuBox.getChildren().addAll(resumeButton, settingsButton, helpButton, restartButton, mainMenuButton);
        
        // Сохраняем ссылки на кнопки для навигации (продолжить первая)
        // Исключаем отключенную кнопку настроек из навигации
        menuButtons = new Button[]{resumeButton, helpButton, restartButton, mainMenuButton};
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();
        
        return menuBox;
    }
    
    private Button createMenuButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setPrefSize(GameConfig.MENU_BUTTON_WIDTH, GameConfig.MENU_BUTTON_HEIGHT);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
        // Единый стиль кнопки (как в главном меню)
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
        
        // Hover эффекты для мыши
        button.setOnMouseEntered(e -> {
            // Воспроизводим звук только если кнопка не активна
            if (menuButtons != null && button != menuButtons[currentButtonIndex]) {
                app.getAudioManager().playSFXByName("menu_hover");
            }
            
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
            
            // Анимация увеличения
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // Восстанавливаем стиль
            if (button.getUserData() != null) {
                button.setStyle((String) button.getUserData());
            }
            
            // Анимация уменьшения
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
        
        button.setOnAction(e -> action.run());
        
        return button;
    }
    
    private void setupKeyHandler() {
        // Делаем VBox фокусируемым
        this.setFocusTraversable(true);
        
        // Обработчик нажатия клавиш
        this.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    if (app.consumePauseResumeLock()) {
                        event.consume();
                        break;
                    }
                    // Возобновляем игру
                    app.getAudioManager().playSFXByName("menu_back");
                    resumeGame();
                    event.consume();
                    break;
                case ENTER:
                    // Активируем выбранную кнопку
                    if (menuButtons != null && currentButtonIndex < menuButtons.length) {
                        app.getAudioManager().playSFXByName("menu_select");
                        menuButtons[currentButtonIndex].fire();
                    }
                    event.consume();
                    break;
                case UP:
                    // Переход к предыдущей кнопке
                    if (menuButtons != null) {
                        currentButtonIndex = (currentButtonIndex - 1 + menuButtons.length) % menuButtons.length;
                        updateButtonHighlight();
                        app.getAudioManager().playSFXByName("menu_hover");
                    }
                    event.consume();
                    break;
                case DOWN:
                    // Переход к следующей кнопке
                    if (menuButtons != null) {
                        currentButtonIndex = (currentButtonIndex + 1) % menuButtons.length;
                        updateButtonHighlight();
                        app.getAudioManager().playSFXByName("menu_hover");
                    }
                    event.consume();
                    break;
                default:
                    break;
            }
        });
    }
    
    private void updateButtonHighlight() {
        if (menuButtons == null) return;
        
        for (int i = 0; i < menuButtons.length; i++) {
            Button button = menuButtons[i];
            if (i == currentButtonIndex) {
                // Выделяем активную кнопку
                String activeStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                    GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
                );
                button.setStyle(activeStyle);
            } else {
                // Обычный стиль для неактивных кнопок
                String normalStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
                    GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
                );
                button.setStyle(normalStyle);
            }
        }
    }
    
    private void playAppearAnimation() {
        // Анимация появления
        fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // Анимация масштабирования
        scaleIn = new ScaleTransition(Duration.millis(300), this);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }
    
    private void resumeGame() {
        // Очищаем обработчики перед удалением
        cleanup();
        
        // Скрываем экран паузы
        FXGL.getGameScene().removeUINode(this);
        
        // Возобновляем игру
        app.resumeGame();
    }
    
    private void showSettings() {
        // Очищаем обработчики перед удалением
        cleanup();
        
        // Скрываем экран паузы
        FXGL.getGameScene().removeUINode(this);
        
        // Показываем настройки с возвратом в паузу
        showSettingsWithPauseReturn();
    }
    
    private void showSettingsWithPauseReturn() {
        // КРИТИЧНО: Сохраняем snapshot ДО установки флага inPauseSettings
        // Это предотвращает утечки памяти при изменении настроек
        app.saveSnapshotBeforePauseSettings();
        
        // Устанавливаем флаг ПЕРЕД созданием SettingsView
        // Это позволит SettingsView знать, что нужно блокировать сложность
        app.setInPauseSettings(true);
        
        // Создаем настройки с заблокированной сложностью
        SettingsView settingsView = new SettingsView(app);
        
        FXGL.getGameScene().addUINode(settingsView);
    }
    
    private void showHelp() {
        // Очищаем обработчики перед удалением
        cleanup();
        
        // Скрываем экран паузы
        FXGL.getGameScene().removeUINode(this);
        
        // Показываем справку с возвратом в паузу
        showHelpWithPauseReturn();
    }
    
    private void showHelpWithPauseReturn() {
        // Создаем справку с возвратом в паузу
        HelpView helpView = new HelpView(app, true); // true = из паузы
        
        FXGL.getGameScene().addUINode(helpView);
    }
    
    
    private void returnToMainMenu() {
        // КРИТИЧНО: Проверяем, не выполняется ли уже переход в главное меню
        if (isReturningToMainMenu) {
            return; // Уже выполняется переход, игнорируем повторное нажатие
        }
        
        // Устанавливаем флаг, что мы выполняем переход
        isReturningToMainMenu = true;
        
        // КРИТИЧНО: Сохраняем ссылку на app перед cleanup(), так как cleanup() обнулит app
        ArcadeBlocksApp appRef = app;
        
        // КРИТИЧНО: Сбрасываем флаг паузы ПЕРЕД cleanup(), чтобы предотвратить создание нового PauseView
        if (appRef != null) {
            // Устанавливаем флаг, что мы выходим из паузы в главное меню
            appRef.setInPauseSettings(false);
            // КРИТИЧНО: Сбрасываем флаг паузы через публичный метод, если он существует
            // Это предотвратит создание нового PauseView в returnToMainMenu()
            appRef.resetPauseState();
        }
        
        // Очищаем обработчики перед удалением
        cleanup();
        
        // Скрываем экран паузы
        FXGL.getGameScene().removeUINode(this);
        
        // Возвращаемся в главное меню
        if (appRef != null) {
            appRef.returnToMainMenu();
        }
    }

    private void showRestartConfirm() {
        if (app == null) {
            return;
        }
        VBox dialog = new VBox(12);
        dialog.setAlignment(Pos.CENTER);
        dialog.setPadding(new Insets(18));
        dialog.setStyle("-fx-background-color: rgba(10,12,18,0.96); -fx-background-radius: 14; " +
            "-fx-border-radius: 14; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1;");

        Label title = new Label(localize("pause.restart.title"));
        title.setFont(Font.font("Orbitron", FontWeight.EXTRA_BOLD, 18));
        title.setTextFill(Color.web(GameConfig.NEON_GREEN));

        Label subtitle = new Label(localize("pause.restart.subtitle"));
        subtitle.setFont(Font.font("Orbitron", FontWeight.NORMAL, 13));
        subtitle.setTextFill(Color.web("#cfd7e3"));
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subtitle.setMaxWidth(360);

        Button cancel = new Button(localize("pause.restart.cancel"));
        cancel.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
        cancel.setTextFill(Color.web("#f5f7ff"));
        cancel.setCursor(javafx.scene.Cursor.HAND);
        cancel.setStyle("-fx-background-color: linear-gradient(to right, #3b4258, #202534);" +
            "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-width: 1; -fx-padding: 8 18 8 18;");
        ScaleTransition cancelScale = new ScaleTransition(Duration.millis(140), cancel);
        cancelScale.setFromX(1.0);
        cancelScale.setFromY(1.0);
        cancelScale.setToX(1.05);
        cancelScale.setToY(1.05);

        Button confirm = new Button(localize("pause.restart.confirm"));
        confirm.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
        confirm.setTextFill(Color.web("#0c0c0f"));
        confirm.setCursor(javafx.scene.Cursor.HAND);
        confirm.setStyle("-fx-background-color: linear-gradient(to right, #7CFF72, #4DE964);" +
            "-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: rgba(0,0,0,0.3);" +
            "-fx-border-width: 1; -fx-padding: 8 18 8 18;");
        ScaleTransition confirmScale = new ScaleTransition(Duration.millis(140), confirm);
        confirmScale.setFromX(1.0);
        confirmScale.setFromY(1.0);
        confirmScale.setToX(1.05);
        confirmScale.setToY(1.05);

        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(title, subtitle, buttons);

        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        overlay.getChildren().add(dialog);
        overlay.setUserData("restartConfirm");
        overlay.setOpacity(0);

        FXGL.getGameScene().addUINode(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), overlay);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        cancel.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_back");
            FadeTransition out = new FadeTransition(Duration.millis(160), overlay);
            out.setToValue(0.0);
            out.setOnFinished(ev -> FXGL.getGameScene().removeUINode(overlay));
            out.play();
        });

        confirm.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_select");
            FXGL.getGameScene().removeUINode(overlay);
            restartLevelImmediately();
        });

        cancel.setOnMouseEntered(e -> {
            if (app != null && app.getAudioManager() != null) {
                app.getAudioManager().playSFX("sounds/menu_hover.wav");
            }
            cancelScale.playFromStart();
        });
        cancel.setOnMouseExited(e -> {
            cancelScale.stop();
            cancel.setScaleX(1.0);
            cancel.setScaleY(1.0);
        });

        confirm.setOnMouseEntered(e -> {
            if (app != null && app.getAudioManager() != null) {
                app.getAudioManager().playSFX("sounds/menu_hover.wav");
            }
            confirmScale.playFromStart();
        });
        confirm.setOnMouseExited(e -> {
            confirmScale.stop();
            confirm.setScaleX(1.0);
            confirm.setScaleY(1.0);
        });
    }
    
    private String localize(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        return localizationManager.getOrDefault(key, key);
    }

    private void restartLevelImmediately() {
        if (app == null) {
            return;
        }
        app.resetPauseState();
        cleanup();
        FXGL.getGameScene().removeUINode(this);
        app.restartCurrentLevelFromPause();
    }
}
