package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Окно выбора языка
 */
public class LanguageView extends StackPane {
    
    private ArcadeBlocksApp app;
    private final LocalizationManager localizationManager;
    private VBox contentBox;
    private Button russianButton;
    private Button englishButton;
    private Button backButton;
    private Button[] languageButtons;
    private int currentButtonIndex = 0;
    private ScaleTransition titlePulseAnimation;
    private FadeTransition fadeIn;
    // КРИТИЧНО: Сохраняем ссылку на event filter для его удаления при cleanup
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyEventFilter;
    
    // Анимации закрытия
    private FadeTransition closeFadeTransition;
    private ScaleTransition closeScaleTransition;
    private volatile boolean isClosing = false;
    
    public LanguageView(ArcadeBlocksApp app) {
        this.app = app;
        this.localizationManager = LocalizationManager.getInstance();
        
        initializeUI();
        setupKeyHandler();
    }
    
    private void initializeUI() {
        // КРИТИЧНО: НЕ создаем свой собственный фон!
        // LanguageView должен быть прозрачным и показываться поверх существующего MainMenuView
        // Это предотвращает создание дубликатов фоновых изображений
        
        // Создание контентного контейнера
        contentBox = new VBox(30);
        contentBox.setAlignment(Pos.CENTER);
        
        // Полупрозрачный темный фон, MainMenuView виден под ним
        contentBox.setStyle("-fx-background-color: rgba(15, 15, 28, 0.85);");
        
        // Заголовок
        Label titleLabel = createTitleLabel();
        
        // Контейнер кнопок языков
        VBox languageButtonsBox = createLanguageButtonsBox();
        
        // Кнопка "Назад в главное меню"
        backButton = createBackButton();
        
        contentBox.getChildren().addAll(titleLabel, languageButtonsBox, backButton);
        
        applyInitialSelection();
        updateButtonHighlight();
        
        // КРИТИЧНО: Добавляем только contentBox, без backgroundImageView
        getChildren().add(contentBox);
        
        // Анимация появления
        fadeIn = new FadeTransition(Duration.seconds(1.0), contentBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            if (fadeIn != null) {
                fadeIn.stop();
                fadeIn = null;
            }
        });
        fadeIn.play();

        ResponsiveLayoutHelper.bindToStage(this, (width, height) -> {
            if (contentBox != null) {
                contentBox.setPrefSize(width, height);
                contentBox.setMinSize(width, height);
                contentBox.setMaxSize(width, height);
            }
        });
        setUserData("fullScreenOverlay");
    }
    

    
    private Label createTitleLabel() {
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "language.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 10, 0.5, 0, 0);");
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Анимация пульсации
        titlePulseAnimation = new ScaleTransition(Duration.seconds(2.0), titleLabel);
        titlePulseAnimation.setFromX(1.0);
        titlePulseAnimation.setFromY(1.0);
        titlePulseAnimation.setToX(1.05);
        titlePulseAnimation.setToY(1.05);
        titlePulseAnimation.setAutoReverse(true);
        titlePulseAnimation.setCycleCount(ScaleTransition.INDEFINITE);
        titlePulseAnimation.play();
        
        return titleLabel;
    }
    
    private VBox createLanguageButtonsBox() {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        
        // Кнопка "Русский язык"
        russianButton = createLanguageButton("language.button.russian", GameConfig.NEON_GREEN, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            setLanguage("ru");
        });
        
        // Кнопка "English"
        englishButton = createLanguageButton("language.button.english", GameConfig.NEON_PURPLE, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            setLanguage("en");
        });
        
        // Сохраняем ссылки на кнопки для навигации
        languageButtons = new Button[]{russianButton, englishButton};
        
        container.getChildren().addAll(russianButton, englishButton);
        
        return container;
    }
    
    private Button createLanguageButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setPrefSize(400, 80);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 24));
        button.setTextFill(Color.WHITE);
        
        String buttonStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND + "80", color, color
        );
        
        button.setStyle(buttonStyle);
        
        // Hover эффекты
        button.setOnMouseEntered(e -> {
            app.getAudioManager().playSFXByName("menu_hover");
            
            String hoverStyle = String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 3px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                color + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
            );
            
            button.setUserData(button.getStyle());
            button.setStyle(hoverStyle);
            
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.setOnFinished(e2 -> scaleUp.stop());
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            if (button.getUserData() != null) {
                button.setStyle((String) button.getUserData());
            }
            
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setOnFinished(e2 -> scaleDown.stop());
            scaleDown.play();
        });
        
        button.setOnAction(e -> action.run());
        
        return button;
    }
    
    private Button createBackButton() {
        Button button = new Button();
        localizationManager.bind(button, "language.back");
        button.setPrefSize(GameConfig.MENU_BUTTON_WIDTH * 2, GameConfig.MENU_BUTTON_HEIGHT);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
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
        
        // Hover эффекты
        button.setOnMouseEntered(e -> {
            app.getAudioManager().playSFXByName("menu_hover");
            
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
            
            button.setUserData(button.getStyle());
            button.setStyle(hoverStyle);
            
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.setOnFinished(e2 -> scaleUp.stop());
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            if (button.getUserData() != null) {
                button.setStyle((String) button.getUserData());
            }
            
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setOnFinished(e2 -> scaleDown.stop());
            scaleDown.play();
        });
        
        button.setOnAction(e -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            returnToMainMenu();
        });
        
        return button;
    }
    
    private void setupKeyHandler() {
        setFocusTraversable(true);
        requestFocus();
        
        keyEventFilter = event -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                event.consume();
                return;
            }
            
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
                    returnToMainMenu();
                    event.consume();
                    break;
                default:
                    // Блокируем все остальные клавиши, чтобы они не активировали главное меню
                    event.consume();
                    break;
            }
        };
        this.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyEventFilter);
    }
    
    private void moveUp() {
        if (currentButtonIndex == -1) {
            // Если выбрана кнопка "Назад", переходим к последней языковой кнопке
            currentButtonIndex = languageButtons.length - 1;
            updateButtonHighlight();
        } else if (currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        } else if (currentButtonIndex == 0) {
            // Переходим к кнопке "Назад"
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex = -1;
            updateButtonHighlight();
        }
    }
    
    private void moveDown() {
        if (currentButtonIndex == -1) {
            // Если выбрана кнопка "Назад", переходим к первой языковой кнопке
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex = 0;
            updateButtonHighlight();
        } else if (currentButtonIndex < languageButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        } else if (currentButtonIndex == languageButtons.length - 1) {
            // Переходим к кнопке "Назад"
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex = -1;
            updateButtonHighlight();
        }
    }
    
    private void selectCurrentButton() {
        if (currentButtonIndex == -1) {
            backButton.fire();
        } else if (currentButtonIndex >= 0 && currentButtonIndex < languageButtons.length) {
            app.getAudioManager().playSFXByName("menu_select");
            languageButtons[currentButtonIndex].fire();
        }
    }
    
    private void updateButtonHighlight() {
        if (languageButtons == null) return;
        
        for (int i = 0; i < languageButtons.length; i++) {
            Button button = languageButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка
                String highlightStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 3px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 10, 0.5, 0, 0);",
                    GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
                );
                button.setStyle(highlightStyle);
            } else {
                // Обычная кнопка
                String normalStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
                    GameConfig.DARK_BACKGROUND + "80",
                    i == 0 ? GameConfig.NEON_GREEN : GameConfig.NEON_PURPLE,
                    i == 0 ? GameConfig.NEON_GREEN : GameConfig.NEON_PURPLE
                );
                button.setStyle(normalStyle);
            }
        }
        
        // Обновляем стиль кнопки "Назад" (только если она уже создана)
        if (backButton != null) {
            if (currentButtonIndex == -1) {
                String backHighlightStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
                    GameConfig.NEON_CYAN + "40", GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
                );
                backButton.setStyle(backHighlightStyle);
            } else {
                String backNormalStyle = String.format(
                    "-fx-background-color: %s; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
                    GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
                );
                backButton.setStyle(backNormalStyle);
            }
        }
    }
    
    private void setLanguage(String languageCode) {
        // Устанавливаем язык через менеджер локализации
        app.setLanguage(languageCode);
        currentButtonIndex = "en".equalsIgnoreCase(languageCode) ? 1 : 0;
        updateButtonHighlight();
        
        // Закрываем окно и возвращаемся в главное меню
        returnToMainMenu();
    }

    private void applyInitialSelection() {
        String selectedLanguage = localizationManager.getLocale().getLanguage();
        if (app != null && app.getSaveManager() != null) {
            selectedLanguage = app.getSaveManager().getLanguage();
        }
        if ("en".equalsIgnoreCase(selectedLanguage)) {
            currentButtonIndex = 1;
        } else {
            currentButtonIndex = 0;
        }
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимацию пульсации заголовка
        if (titlePulseAnimation != null) {
            titlePulseAnimation.stop();
            titlePulseAnimation = null;
        }
        
        // КРИТИЧНО: Останавливаем анимацию появления
        if (fadeIn != null) {
            fadeIn.stop();
            fadeIn = null;
        }
        
        // КРИТИЧНО: Останавливаем анимации закрытия
        if (closeFadeTransition != null) {
            try {
                closeFadeTransition.stop();
            } catch (Exception ignored) {}
            closeFadeTransition = null;
        }
        if (closeScaleTransition != null) {
            try {
                closeScaleTransition.stop();
            } catch (Exception ignored) {}
            closeScaleTransition = null;
        }
        
        this.setOnKeyPressed(null);
        if (keyEventFilter != null) {
            this.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyEventFilter);
            keyEventFilter = null;
        }
        
        // КРИТИЧНО: Сначала очищаем обработчики и отвязываем textProperty() биндинги
        // ПЕРЕД обнулением кнопок, чтобы избежать утечек памяти от StringBinding
        if (languageButtons != null) {
            for (Button button : languageButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button перед обнулением массива
                    button.textProperty().unbind();
                }
            }
            languageButtons = null;
        }
        
        if (backButton != null) {
            backButton.setOnAction(null);
            backButton.setOnMouseEntered(null);
            backButton.setOnMouseExited(null);
            // КРИТИЧНО: Отвязываем textProperty() у Button
            backButton.textProperty().unbind();
            backButton = null;
        }
        
        // КРИТИЧНО: Отвязываем textProperty() у всех Label компонентов перед удалением children
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Освобождаем изображения перед удалением children
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // Отвязываем слушателей ResponsiveLayoutHelper
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
        
        // Очищаем ссылки
        app = null;
        contentBox = null;
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
    
    private void playCloseAnimation(Runnable onFinished) {
        // КРИТИЧНО: Проверяем, не закрываемся ли мы уже
        if (isClosing) {
            return;
        }
        isClosing = true;
        
        // КРИТИЧНО: Останавливаем старые анимации закрытия, если они еще активны
        if (closeFadeTransition != null) {
            try {
                closeFadeTransition.stop();
            } catch (Exception ignored) {}
            closeFadeTransition = null;
        }
        if (closeScaleTransition != null) {
            try {
                closeScaleTransition.stop();
            } catch (Exception ignored) {}
            closeScaleTransition = null;
        }
        
        // Анимация затухания
        closeFadeTransition = new FadeTransition(Duration.millis(300), this);
        closeFadeTransition.setFromValue(1.0);
        closeFadeTransition.setToValue(0.0);
        
        // Анимация уменьшения
        closeScaleTransition = new ScaleTransition(Duration.millis(300), this);
        closeScaleTransition.setFromX(1.0);
        closeScaleTransition.setFromY(1.0);
        closeScaleTransition.setToX(0.9);
        closeScaleTransition.setToY(0.9);
        
        // КРИТИЧНО: Очищаем ссылки на анимации после их завершения
        closeFadeTransition.setOnFinished(e -> {
            // Останавливаем обе анимации
            if (closeFadeTransition != null) {
                closeFadeTransition.stop();
                closeFadeTransition = null;
            }
            if (closeScaleTransition != null) {
                closeScaleTransition.stop();
                closeScaleTransition = null;
            }
            
            if (onFinished != null) {
                onFinished.run();
            }
        });
        closeScaleTransition.setOnFinished(e -> {
            // Ничего не делаем, основная логика в closeFadeTransition.onFinished
        });
        
        closeFadeTransition.play();
        closeScaleTransition.play();
    }
    
    private void returnToMainMenu() {
        // КРИТИЧНО: Запускаем анимацию закрытия перед возвратом в главное меню
        playCloseAnimation(() -> {
            // КРИТИЧНО: НЕ создаем новый MainMenuView!
            // MainMenuView уже существует под LanguageView (overlay pattern)
            // Просто удаляем LanguageView, чтобы показать существующий MainMenuView
            
            cleanup();
            FXGL.getGameScene().removeUINode(this);
            
            // КРИТИЧНО: Восстанавливаем фокус на MainMenuView
            for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
                if (node instanceof MainMenuView) {
                    ((MainMenuView) node).restoreFocus();
                    break;
                }
            }
        });
    }
}
