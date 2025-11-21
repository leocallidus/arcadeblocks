package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.utils.ImageCache;
import com.arcadeblocks.ui.MainMenuView;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Главное Debug-меню с выбором между бонусами и уровнями
 * Вызывается по клавише F1 в главном меню
 */
public class DebugMenuView extends VBox {
    
    private final ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private Runnable onCloseCallback;
    private Button[] menuButtons;
    private int currentButtonIndex = 0;
    private long lastMenuActionTime = 0L;
    private static final long ACTION_COOLDOWN_NANOS = 250_000_000L; // ~250ms
    private boolean cleanedUp = false;
    // КРИТИЧНО: Сохраняем ссылки на анимации кнопок для их остановки
    private java.util.Map<Button, javafx.animation.ScaleTransition> buttonScaleTransitions = new java.util.HashMap<>();
    // КРИТИЧНО: Сохраняем ссылки на анимации закрытия для их остановки
    private javafx.animation.FadeTransition closeFadeTransition;
    private javafx.animation.TranslateTransition closeSlideTransition;
    // КРИТИЧНО: Сохраняем ссылки на анимации animateClose для их остановки
    private javafx.animation.FadeTransition animateCloseFadeTransition;
    private javafx.animation.TranslateTransition animateCloseSlideTransition;

    public DebugMenuView(ArcadeBlocksApp app) {
        this.app = app;
        
        initializeUI();
        setupKeyHandler();
    }

    /**
     * Снимает обработчики и отвязывает вспомогательные биндинги, чтобы избежать утечек.
     */
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;

        setOnKeyPressed(null);
        
        // КРИТИЧНО: Сначала останавливаем анимации закрытия, если они еще активны
        if (closeFadeTransition != null) {
            try {
                closeFadeTransition.stop();
            } catch (Exception ignored) {}
            closeFadeTransition = null;
        }
        if (closeSlideTransition != null) {
            try {
                closeSlideTransition.stop();
            } catch (Exception ignored) {}
            closeSlideTransition = null;
        }
        
        // КРИТИЧНО: Останавливаем анимации animateClose, если они еще активны
        if (animateCloseFadeTransition != null) {
            try {
                animateCloseFadeTransition.stop();
            } catch (Exception ignored) {}
            animateCloseFadeTransition = null;
        }
        if (animateCloseSlideTransition != null) {
            try {
                animateCloseSlideTransition.stop();
            } catch (Exception ignored) {}
            animateCloseSlideTransition = null;
        }
        
        // КРИТИЧНО: Сначала останавливаем все активные анимации кнопок
        if (buttonScaleTransitions != null) {
            for (javafx.animation.ScaleTransition transition : buttonScaleTransitions.values()) {
                if (transition != null) {
                    try {
                        transition.stop();
                    } catch (Exception ignored) {}
                }
            }
            buttonScaleTransitions.clear();
            buttonScaleTransitions = null;
        }
        
        // КРИТИЧНО: Сначала очищаем все обработчики кнопок перед удалением из children
        if (menuButtons != null) {
            for (Button button : menuButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button
                    button.textProperty().unbind();
                }
            }
            menuButtons = null;
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        // Это удаляет listeners на localeProperty, которые создаются через localizationManager.bind()
        // Делаем это ПОСЛЕ очистки обработчиков кнопок, но ПЕРЕД удалением children
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        // ResponsiveLayoutHelper.unbind() теперь делает синхронную очистку, если мы в FX thread
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        // Делаем это ПОСЛЕ очистки всех listeners и обработчиков
        getChildren().clear();
        
        setBackground(null);
        
        // КРИТИЧНО: Обнуляем ссылки для предотвращения утечек памяти
        // app объявлен как final, поэтому его нельзя обнулить
        onCloseCallback = null;
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
        if (node instanceof Parent) {
            for (javafx.scene.Node child : ((Parent) node).getChildrenUnmodifiable()) {
                unbindAllTextProperties(child);
            }
        }
    }
    
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        // Фон пасхалки
        try {
            javafx.scene.image.Image img = ImageCache.get("easter_egg.png");
            // Масштабируем фон на весь контейнер (cover)
            javafx.scene.layout.BackgroundSize bs = new javafx.scene.layout.BackgroundSize(100, 100, true, true, false, true);
            javafx.scene.layout.BackgroundImage bi = new javafx.scene.layout.BackgroundImage(
                img,
                javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                javafx.scene.layout.BackgroundPosition.CENTER,
                bs
            );
            setBackground(new javafx.scene.layout.Background(bi));
            // КРИТИЧНО: Не сохраняем ссылку на Image, так как ImageCache управляет кэшированием
            // Но нужно убедиться, что BackgroundImage и BackgroundSize не создают утечек
        } catch (Exception ex) {
            setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
        }
        setPadding(new Insets(40));
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "debug.menu.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 0.5, 0, 0);");
        
        // Подзаголовок
        Label subtitleLabel = new Label();
        localizationManager.bind(subtitleLabel, "debug.menu.subtitle");
        subtitleLabel.setFont(Font.font("Orbitron", 20));
        subtitleLabel.setTextFill(Color.web("#CCCCCC"));
        subtitleLabel.setAlignment(Pos.CENTER);
        
        // Информация о клавишах
        Label infoLabel = new Label();
        localizationManager.bind(infoLabel, "debug.menu.info");
        infoLabel.setFont(Font.font("Orbitron", 14));
        infoLabel.setTextFill(Color.web("#999999"));
        infoLabel.setAlignment(Pos.CENTER);
        
        // Меню кнопок
        VBox menuBox = createMenuBox();
        
        getChildren().addAll(titleLabel, subtitleLabel, infoLabel, menuBox);
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();

        ResponsiveLayoutHelper.bindToStage(this);
        setUserData("fullScreenOverlay");
    }
    
    private VBox createMenuBox() {
        VBox menuBox = new VBox(25);
        menuBox.setAlignment(Pos.CENTER);
        
        // Кнопка "Бонусы"
        Button bonusesButton = createMenuButton("debug.menu.bonuses", GameConfig.NEON_GREEN, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            showBonusesMenu();
        });
        
        // Кнопка "Уровни"
        Button levelsButton = createMenuButton("debug.menu.levels", GameConfig.NEON_PURPLE, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            showLevelsMenu();
        });
        
        // Кнопка "Поэма"
        Button poemButton = createMenuButton("debug.menu.poem", GameConfig.NEON_YELLOW, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            showPoem();
        });
        
        // Кнопка "Титры"
        Button creditsButton = createMenuButton("debug.menu.credits", GameConfig.NEON_PINK, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            showCredits();
        });
        
        menuBox.getChildren().addAll(bonusesButton, levelsButton, poemButton, creditsButton);
        
        // Сохраняем ссылки на кнопки для навигации
        menuButtons = new Button[]{bonusesButton, levelsButton, poemButton, creditsButton};
        
        return menuBox;
    }
    
    private Button createMenuButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setPrefSize(GameConfig.MENU_BUTTON_WIDTH, GameConfig.MENU_BUTTON_HEIGHT);
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
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
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
            
            // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
            javafx.animation.ScaleTransition oldTransition = buttonScaleTransitions.get(button);
            if (oldTransition != null) {
                try {
                    oldTransition.stop();
                } catch (Exception ignored) {}
            }
            
            // Анимация увеличения
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.setOnFinished(e2 -> {
                buttonScaleTransitions.remove(button);
            });
            buttonScaleTransitions.put(button, scaleUp);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
            javafx.animation.ScaleTransition oldTransition = buttonScaleTransitions.get(button);
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
            scaleDown.setOnFinished(e2 -> {
                buttonScaleTransitions.remove(button);
            });
            buttonScaleTransitions.put(button, scaleDown);
            scaleDown.play();
        });
        
        // Клик по кнопке
        button.setOnAction(e -> handleMenuAction(action));
        
        return button;
    }
    
    private void showBonusesMenu() {
        // КРИТИЧНО: НЕ удаляем DebugMenuView, просто скрываем его
        // DebugBonusesView будет показан поверх
        this.setVisible(false);
        
        // Открываем меню бонусов
        DebugBonusesView bonusesView = new DebugBonusesView(app);
        // КРИТИЧНО: При закрытии просто удаляем DebugBonusesView и показываем DebugMenuView
        bonusesView.setOnCloseCallback(() -> {
            // Удаляем DebugBonusesView
            com.arcadeblocks.ui.util.UINodeCleanup.cleanupNode(bonusesView);
            FXGL.getGameScene().removeUINode(bonusesView);
            // Показываем DebugMenuView обратно
            this.setVisible(true);
        });
        FXGL.getGameScene().addUINode(bonusesView);
        playAppearAnimation(bonusesView);
    }
    
    private void showLevelsMenu() {
        // КРИТИЧНО: НЕ удаляем DebugMenuView, просто скрываем его
        // DebugLevelsView будет показан поверх
        this.setVisible(false);
        
        // Открываем меню уровней
        DebugLevelsView levelsView = new DebugLevelsView(app);
        // КРИТИЧНО: При закрытии просто удаляем DebugLevelsView и показываем DebugMenuView
        levelsView.setOnCloseCallback(() -> {
            // Удаляем DebugLevelsView
            com.arcadeblocks.ui.util.UINodeCleanup.cleanupNode(levelsView);
            FXGL.getGameScene().removeUINode(levelsView);
            // Показываем DebugMenuView обратно
            this.setVisible(true);
        });
        FXGL.getGameScene().addUINode(levelsView);
        playAppearAnimation(levelsView);
    }
    
    private void showPoem() {
        // Закрываем главное debug меню
        closeDebugMenu();
        
        // Открываем экран поэмы с флагом fromDebugMenu = true
        PoemView poemView = new PoemView(app, true);
        FXGL.getGameScene().addUINode(poemView);
    }
    
    private void showCredits() {
        // Закрываем главное debug меню
        closeDebugMenu();
        
        // Открываем экран титров
        app.showCredits();
    }
    
    private void updateButtonHighlight() {
        if (menuButtons == null) return;
        
        for (int i = 0; i < menuButtons.length; i++) {
            Button button = menuButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка - яркий цвет (как в главном меню)
                button.setStyle(
                    "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 10, 0.5, 0, 0);"
                );
            } else {
                // Обычная кнопка - стандартный стиль (как в главном меню)
                button.setStyle(
                    "-fx-background-color: " + GameConfig.DARK_BACKGROUND + "; " +
                    "-fx-border-color: " + GameConfig.NEON_PURPLE + "; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_PURPLE + ", 5, 0.3, 0, 0);"
                );
            }
        }
    }
    
    private void moveUp() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    private void moveDown() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentButtonIndex < menuButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    private void selectCurrentButton() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (menuButtons != null && currentButtonIndex >= 0 && currentButtonIndex < menuButtons.length) {
            app.getAudioManager().playSFXByName("menu_select");
            menuButtons[currentButtonIndex].fire();
        }
    }

    private void handleMenuAction(Runnable action) {
        long now = System.nanoTime();
        if (now - lastMenuActionTime < ACTION_COOLDOWN_NANOS) {
            return;
        }

        lastMenuActionTime = now;
        action.run();
    }
    
    // КРИТИЧНО: Делаем метод package-private, чтобы MainMenuView мог вызвать его напрямую
    void closeDebugMenu() {
        // КРИТИЧНО: Проверяем, не был ли уже вызван cleanup (защита от двойного вызова)
        if (cleanedUp) {
            return;
        }
        
        // КРИТИЧНО: Сохраняем callback перед cleanup, так как cleanup() обнулит onCloseCallback
        Runnable callback = onCloseCallback;
        onCloseCallback = null; // Обнуляем сразу, чтобы избежать повторных вызовов
        
        // КРИТИЧНО: Останавливаем старые анимации закрытия, если они еще активны
        if (closeFadeTransition != null) {
            try {
                closeFadeTransition.stop();
            } catch (Exception ignored) {}
            closeFadeTransition = null;
        }
        if (closeSlideTransition != null) {
            try {
                closeSlideTransition.stop();
            } catch (Exception ignored) {}
            closeSlideTransition = null;
        }
        
        // КРИТИЧНО: Анимация закрытия перед удалением из сцены
        closeFadeTransition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), this);
        closeFadeTransition.setFromValue(this.getOpacity());
        closeFadeTransition.setToValue(0.0);
        closeSlideTransition = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), this);
        closeSlideTransition.setFromY(0);
        closeSlideTransition.setToY(10);
        
        closeFadeTransition.setOnFinished(e -> {
            try {
                // КРИТИЧНО: Очищаем ссылки на анимации
                closeFadeTransition = null;
                closeSlideTransition = null;
                
                // КРИТИЧНО: СНАЧАЛА удаляем из scene, ПОТОМ cleanup
                // Это предотвращает race condition, когда нода еще в scene, но уже "мертвая"
                FXGL.getGameScene().removeUINode(this);
                
                // Теперь безопасно вызываем cleanup, так как нода уже не в scene
                // cleanup() сам установит cleanedUp = true
                cleanup();
                
                // КРИТИЧНО: Уведомляем родительский компонент о закрытии ПОСЛЕ cleanup и удаления из сцены
                // Это предотвращает повторные вызовы closeDebugMenu
                if (callback != null) {
                    callback.run();
                }
            } catch (Exception ignored) {}
        });
        
        closeFadeTransition.play();
        closeSlideTransition.play();
    }
    
    
    private void setupKeyHandler() {
        this.setFocusTraversable(true);
        
        // Пытаемся получить фокус через Platform.runLater
        Platform.runLater(() -> {
            this.requestFocus();
        });
        
        this.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    // КРИТИЧНО: Не проигрываем звук здесь, так как он будет проигран в MainMenuView
                    // или при прямом вызове closeDebugMenu() из MainMenuView
                    closeDebugMenu();
                    event.consume();
                    break;
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
                default:
                    // Игнорируем остальные клавиши
                    break;
            }
        });
    }

    private void playAppearAnimation(javafx.scene.Node node) {
        node.setOpacity(0);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), node);
        slide.setFromY(20);
        slide.setToY(0);
        fade.play();
        slide.play();
    }

    private void animateClose(Runnable onFinished) {
        // КРИТИЧНО: Останавливаем старые анимации animateClose, если они еще активны
        if (animateCloseFadeTransition != null) {
            try {
                animateCloseFadeTransition.stop();
            } catch (Exception ignored) {}
            animateCloseFadeTransition = null;
        }
        if (animateCloseSlideTransition != null) {
            try {
                animateCloseSlideTransition.stop();
            } catch (Exception ignored) {}
            animateCloseSlideTransition = null;
        }
        
        animateCloseFadeTransition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), this);
        animateCloseFadeTransition.setFromValue(getOpacity());
        animateCloseFadeTransition.setToValue(0.0);
        animateCloseSlideTransition = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), this);
        animateCloseSlideTransition.setFromY(0);
        animateCloseSlideTransition.setToY(10);
        animateCloseFadeTransition.setOnFinished(e -> {
            try {
                // КРИТИЧНО: Очищаем ссылки на анимации
                animateCloseFadeTransition = null;
                animateCloseSlideTransition = null;
                
                // КРИТИЧНО: СНАЧАЛА удаляем из scene, ПОТОМ cleanup
                FXGL.getGameScene().removeUINode(this);
                cleanup();
                if (onFinished != null) onFinished.run();
            } catch (Exception ignored) {}
        });
        animateCloseFadeTransition.play();
        animateCloseSlideTransition.play();
    }

    @SuppressWarnings("unused")
    private void showDebugMenuAnimated() {
        // КРИТИЧНО: Используем сохраненную ссылку на app, так как this.app может быть null после cleanup()
        if (app == null) {
            return;
        }
        showDebugMenuAnimated(app);
    }
    
    /**
     * Создает и показывает новый DebugMenuView с анимацией
     * @param appRef ссылка на ArcadeBlocksApp (не использует this.app для избежания утечек памяти)
     */
    private static void showDebugMenuAnimated(ArcadeBlocksApp appRef) {
        // КРИТИЧНО: Этот метод вызывается из callback после закрытия DebugBonusesView или DebugLevelsView
        // Текущий DebugMenuView уже был удален из сцены в animateClose(), но нужно убедиться,
        // что новый DebugMenuView правильно создается и добавляется в MainMenuView
        
        // КРИТИЧНО: Проверяем, что appRef не null
        if (appRef == null) {
            return;
        }
        
        // Переоткрыть этот экран с анимацией (после возврата из раздела)
        DebugMenuView view = new DebugMenuView(appRef);
        FXGL.getGameScene().addUINode(view);
        
        // КРИТИЧНО: Находим MainMenuView в сцене и обновляем его ссылку на новое debug меню
        // Это предотвращает утечки памяти от старых ссылок
        try {
            var uiNodes = FXGL.getGameScene().getUINodes();
            for (var node : uiNodes) {
                if (node instanceof MainMenuView) {
                    ((MainMenuView) node).setDebugMenu(view);
                    break;
                }
            }
        } catch (Exception ignored) {}
        
        // Вызываем playAppearAnimation через экземпляр view
        view.playAppearAnimation(view);
    }
}
