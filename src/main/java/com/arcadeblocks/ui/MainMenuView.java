package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.AuthorsConfig;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.event.EventHandler;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Главное меню игры
 */
public class MainMenuView extends StackPane {
    
    private ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private VBox contentBox;
    private DebugMenuView debugMenu;
    private boolean isDebugMenuOpen = false;
    // КРИТИЧНО: Метод для обновления ссылки на debugMenu извне (для showDebugMenuAnimated)
    public void setDebugMenu(DebugMenuView menu) {
        // КРИТИЧНО: Если старое меню еще существует, сначала полностью очищаем его
        if (this.debugMenu != null && this.debugMenu != menu) {
            try {
                if (this.debugMenu instanceof DebugMenuView) {
                    ((DebugMenuView) this.debugMenu).cleanup();
                }
                FXGL.getGameScene().removeUINode(this.debugMenu);
            } catch (Exception ignored) {}
        }
        this.debugMenu = menu;
        if (menu != null) {
            isDebugMenuOpen = true;
            // Устанавливаем callback для нового меню
            menu.setOnCloseCallback(this::closeDebugMenu);
        } else {
            isDebugMenuOpen = false;
        }
    }
    // КРИТИЧНО: Сохраняем ссылки на анимации открытия debug меню для их остановки
    private javafx.animation.FadeTransition debugMenuFadeIn;
    private javafx.animation.TranslateTransition debugMenuSlideIn;
    private ImageView backgroundImageView;
    private ImageView leftFloatingLogo;
    private ImageView floatingLogo;
    
    // Навигация по меню
    private Button[] menuButtons;
    private int currentButtonIndex = 0;
    private long lastMenuActionTime = 0L;
    private static final long ACTION_COOLDOWN_NANOS = 250_000_000L; // ~250ms
    
    private boolean f1KeyPressed = false; // Защита от зажатия клавиши
    private boolean cleanedUp = false;
    private FadeTransition fadeIn;
    private EventHandler<KeyEvent> sceneKeyHandler;
    private EventHandler<KeyEvent> releaseHandler;
    private javafx.beans.value.ChangeListener<Scene> sceneListener;
    private final java.util.List<javafx.beans.binding.StringBinding> activeBindings = new java.util.ArrayList<>();
    
    public MainMenuView(ArcadeBlocksApp app) {
        this.app = app;
        initializeUI(true); // По умолчанию фон меняется
    }
    
    public MainMenuView(ArcadeBlocksApp app, boolean shouldChangeBackground) {
        this.app = app;
        initializeUI(shouldChangeBackground);
    }
    
    public MainMenuView(ArcadeBlocksApp app, boolean shouldChangeBackground, String specificBackground) {
        this.app = app;
        initializeUI(shouldChangeBackground, specificBackground);
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;

        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        this.setOnKeyReleased(null);
        
        // Удаляем listener со sceneProperty
        if (sceneListener != null) {
            this.sceneProperty().removeListener(sceneListener);
            sceneListener = null;
        }
        
        // Удаляем обработчики со сцены
        Scene scene = this.getScene();
        if (scene != null) {
            if (sceneKeyHandler != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, sceneKeyHandler);
            }
            if (releaseHandler != null) {
                scene.removeEventFilter(KeyEvent.KEY_RELEASED, releaseHandler);
            }
        }
        sceneKeyHandler = null;
        releaseHandler = null;

        // КРИТИЧНО: Сначала очищаем обработчики и отвязываем textProperty() биндинги
        // ПЕРЕД обнулением menuButtons, чтобы избежать утечек памяти от StringBinding
        if (menuButtons != null) {
            for (Button button : menuButtons) {
                if (button != null) {
                    // Очищаем обработчики событий
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button перед обнулением массива
                    button.textProperty().unbind();
                }
            }
            // Только после отвязки всех биндингов обнуляем массив
            menuButtons = null;
        }
        
        // КРИТИЧНО: Dispose всех StringBinding для полного освобождения памяти
        for (javafx.beans.binding.StringBinding binding : activeBindings) {
            if (binding != null) {
                try {
                    binding.dispose();
                } catch (Exception ignored) {}
            }
        }
        activeBindings.clear();

        // КРИТИЧНО: Останавливаем анимации открытия debug меню
        if (debugMenuFadeIn != null) {
            try {
                debugMenuFadeIn.stop();
            } catch (Exception ignored) {}
            debugMenuFadeIn = null;
        }
        if (debugMenuSlideIn != null) {
            try {
                debugMenuSlideIn.stop();
            } catch (Exception ignored) {}
            debugMenuSlideIn = null;
        }
        
        // Закрываем debug меню
        if (debugMenu != null) {
            debugMenu = null;
        }

        // Освобождаем ссылки на изображения, чтобы GC смог их выгрузить
        if (backgroundImageView != null) {
            backgroundImageView.setImage(null);
            backgroundImageView = null;
        }
        if (leftFloatingLogo != null) {
            leftFloatingLogo.setImage(null);
            leftFloatingLogo = null;
        }
        if (floatingLogo != null) {
            floatingLogo.setImage(null);
            floatingLogo = null;
        }

        // КРИТИЧНО: Останавливаем анимацию появления ПЕРЕД очисткой children
        if (fadeIn != null) {
            fadeIn.stop();
            fadeIn = null;
        }

        // КРИТИЧНО: textProperty() биндинги уже отвязаны выше при очистке menuButtons
        // Дополнительная проверка не требуется, так как menuButtons уже обнулен
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Дополнительная прямая синхронная очистка ResponsiveLayoutHelper listeners
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
        
        // КРИТИЧНО: Освобождаем изображения перед удалением children
        // Это предотвращает утечки памяти от com.sun.prism.image.* буферов
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();

        // Очищаем ссылки
        app = null;
        contentBox = null;
    }

    /**
     * Перестраивает фон в соответствии с текущей игровой линией и прогрессом.
     */
    public void refreshBackground() {
        try {
            ImageView newBg = createBackgroundImage(true, null);
            if (newBg == null) {
                return;
            }
            int idx = getChildren().indexOf(backgroundImageView);
            if (idx >= 0) {
                getChildren().set(idx, newBg);
            } else {
                getChildren().add(0, newBg);
            }
            if (backgroundImageView != null) {
                backgroundImageView.setImage(null);
            }
            backgroundImageView = newBg;
        } catch (Exception e) {
            System.err.println("Не удалось обновить фон главного меню: " + e.getMessage());
        }
    }

    private void initializeUI(boolean shouldChangeBackground) {
        initializeUI(shouldChangeBackground, null);
    }
    
    private void initializeUI(boolean shouldChangeBackground, String specificBackground) {
        // Создание фонового изображения
        backgroundImageView = createBackgroundImage(shouldChangeBackground, specificBackground);
        
        // Создание контентного контейнера
        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        
        // Добавление полупрозрачного фона для лучшей читаемости
        contentBox.setStyle("-fx-background-color: rgba(26, 26, 46, 0.6);");
        
        // Логотип игры
        Label titleLabel = createTitleLabel();
        
        // Меню кнопок
        VBox menuBox = createMenuBox();
        
        contentBox.getChildren().addAll(titleLabel, menuBox);
        
        // Левитирующий логотип GamesHopes в левом нижнем углу
        leftFloatingLogo = createLeftFloatingLogo();
        setAlignment(leftFloatingLogo, Pos.BOTTOM_LEFT);
        
        // Левитирующий логотип игры в правом нижнем углу
        floatingLogo = createFloatingLogo();
        setAlignment(floatingLogo, Pos.BOTTOM_RIGHT);
        
        // Добавление элементов в StackPane (фон снизу, контент сверху)
        getChildren().addAll(backgroundImageView, contentBox, leftFloatingLogo, floatingLogo);
        
        // Анимация появления
        fadeIn = new FadeTransition(Duration.seconds(1.0), contentBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
        // Настройка обработки клавиш для debug меню и навигации
        setupKeyHandler();

        ResponsiveLayoutHelper.bindToStage(this, (width, height) -> {
            if (backgroundImageView != null) {
                backgroundImageView.setFitWidth(width);
                backgroundImageView.setFitHeight(height);
            }

            if (contentBox != null) {
                contentBox.setPrefSize(width, height);
                contentBox.setMinSize(width, height);
                contentBox.setMaxSize(width, height);
            }
        });
    }
    
    /**
     * Проверить, завершена ли игра хотя бы в одном слоте
     */
    private boolean isGameCompletedInAnySlot() {
        if (app == null || app.getSaveManager() == null) {
            // System.out.println("DEBUG: app or saveManager is null");
            return false;
        }
        
        // Проверяем все 4 слота
        for (int slot = 1; slot <= 4; slot++) {
            if (app.getSaveManager().isGameCompletedInSlot(slot)) {
                // System.out.println("DEBUG: Game completed in slot " + slot);
                return true;
            }
        }
        
        // System.out.println("DEBUG: Game not completed in any slot");
        return false;
    }
    
    private ImageView createBackgroundImage(boolean shouldChangeBackground) {
        return createBackgroundImage(shouldChangeBackground, null);
    }
    
    private ImageView createBackgroundImage(boolean shouldChangeBackground, String specificBackground) {
        try {
            String backgroundPath = specificBackground;

            if (backgroundPath == null) {
                // Определяем состояние прогресса игры
                com.arcadeblocks.config.AudioConfig.GameProgressState progressState =
                    app.getSaveManager() != null
                        ? app.getSaveManager().getMenuProgressState()
                        : com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
                System.out.println("[MainMenuView] progressState=" + progressState);
                
                // Проверяем, нужно ли использовать специальные фоны
                boolean useSpecialBackgrounds = progressState != com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
                
                if (shouldChangeBackground) {
                    String current = app.getCurrentMainMenuBackground();
                    // БЛОКИРОВКА: Если нужны специальные фоны и текущий фон старый - принудительно меняем
                    if (current != null && !current.isEmpty() && 
                        !(useSpecialBackgrounds && com.arcadeblocks.config.AudioConfig.isOldMainMenuBackground(current))) {
                        backgroundPath = current;
                    } else {
                        // Используем фоны в зависимости от прогресса игры
                        backgroundPath = com.arcadeblocks.config.AudioConfig.getRandomMainMenuBackground(progressState);
                    }
                } else {
                    String current = app.getCurrentMainMenuBackground();
                    // БЛОКИРОВКА: Если нужны специальные фоны и текущий фон старый - принудительно меняем
                    if (current != null && !current.isEmpty() && 
                        !(useSpecialBackgrounds && com.arcadeblocks.config.AudioConfig.isOldMainMenuBackground(current))) {
                        backgroundPath = current;
                    } else {
                        // Всегда используем метод с проверкой прогресса игры
                        // Это гарантирует, что старые фоны не будут использоваться после прохождения
                        backgroundPath = com.arcadeblocks.config.AudioConfig.getRandomMainMenuBackground(progressState);
                    }
                }
            }

            if (backgroundPath != null) {
                app.setCurrentMainMenuBackground(backgroundPath);
            }
            
            // Загрузка фонового изображения
            Image backgroundImage = ImageCache.get(backgroundPath);
            ImageView backgroundImageView = new ImageView(backgroundImage);
            
            // Настройка размеров и масштабирования
            backgroundImageView.setFitWidth(GameConfig.GAME_WIDTH);
            backgroundImageView.setFitHeight(GameConfig.GAME_HEIGHT);
            backgroundImageView.setPreserveRatio(false);
            backgroundImageView.setSmooth(false);
            backgroundImageView.setCache(false);
            
            return backgroundImageView;
        } catch (Exception e) {
            System.err.println("Не удалось загрузить фоновое изображение: " + e.getMessage());
            // Создаем пустое изображение как fallback
            ImageView fallbackImageView = new ImageView();
            fallbackImageView.setFitWidth(GameConfig.GAME_WIDTH);
            fallbackImageView.setFitHeight(GameConfig.GAME_HEIGHT);
            fallbackImageView.setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
            return fallbackImageView;
        }
    }
    
    private Label createTitleLabel() {
        // Создаём разноцветный текст: ARCADE (неоново-розовый) + BLOCKS (как раньше)
        Text arcadeText = new Text("ARCADE ");
        arcadeText.setFont(Font.font("Orbitron", FontWeight.BOLD, 48));
        arcadeText.setFill(Color.web(GameConfig.NEON_PINK));
        // Свечение для ARCADE
        DropShadow arcadeGlow = new DropShadow();
        arcadeGlow.setColor(Color.web(GameConfig.NEON_PINK));
        arcadeGlow.setRadius(12);
        arcadeGlow.setSpread(0.5);
        Glow arcadeInnerGlow = new Glow(0.6);
        arcadeGlow.setInput(arcadeInnerGlow);
        arcadeText.setEffect(arcadeGlow);

        Text blocksText = new Text("BLOCKS");
        blocksText.setFont(Font.font("Orbitron", FontWeight.BOLD, 48));
        blocksText.setFill(Color.web(GameConfig.NEON_CYAN));
        // Лёгкое свечение как раньше
        DropShadow blocksGlow = new DropShadow();
        blocksGlow.setColor(Color.web(GameConfig.NEON_CYAN));
        blocksGlow.setRadius(10);
        blocksGlow.setSpread(0.4);
        blocksText.setEffect(blocksGlow);

        TextFlow textFlow = new TextFlow(arcadeText, blocksText);
        textFlow.setTextAlignment(TextAlignment.CENTER);

        Label titleLabel = new Label();
        titleLabel.setGraphic(textFlow);
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        // Анимация пульсации
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.0), titleLabel);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(ScaleTransition.INDEFINITE);
        pulse.play();

        // Левитация (плавное вертикальное движение)
        TranslateTransition levitation = new TranslateTransition(Duration.seconds(3.0), titleLabel);
        levitation.setFromY(0);
        levitation.setToY(-12);
        levitation.setAutoReverse(true);
        levitation.setCycleCount(TranslateTransition.INDEFINITE);
        levitation.play();

        return titleLabel;
    }
    
    private VBox createMenuBox() {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        
        // Играть
        Button newGameButton = createMenuButton("menu.play", GameConfig.NEON_PINK, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            // Переход к окну сохранений
            showSaveGameWindow();
        }, 0);
        // Настройки
        Button settingsButton = createMenuButton("menu.settings", GameConfig.NEON_PURPLE, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            app.showSettings();
        }, 1);
        
        // Справка
        Button helpButton = createMenuButton("menu.help", GameConfig.NEON_GREEN, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            app.showHelp();
        }, 2);
        
        // Выход
        Button exitButton = createMenuButton("menu.exit", GameConfig.NEON_YELLOW, () -> {
            app.getAudioManager().playSFXByName("menu_select");
            exitGame();
        }, 3);
        
        menuBox.getChildren().addAll(newGameButton, settingsButton, helpButton, exitButton);
        
        // Сохраняем ссылки на кнопки для навигации
        menuButtons = new Button[]{newGameButton, settingsButton, helpButton, exitButton};
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();
        
        return menuBox;
    }
    
    private Button createMenuButton(String translationKey, String color, Runnable action, int buttonIndex) {
        Button button = new Button();
        javafx.beans.binding.StringBinding binding = localizationManager.bind(button, translationKey);
        activeBindings.add(binding);
        button.setPrefSize(GameConfig.MENU_BUTTON_WIDTH, GameConfig.MENU_BUTTON_HEIGHT);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
        // Устанавливаем курсор-руку
        button.setCursor(javafx.scene.Cursor.HAND);
        
        // Единый стиль кнопки (как при клавиатурной навигации)
        String buttonStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
        );
        
        button.setStyle(buttonStyle);
        
        // Hover sync with keyboard navigation
        button.setOnMouseEntered(e -> {
            if (menuButtons != null) {
                boolean changedSelection = currentButtonIndex != buttonIndex;
                currentButtonIndex = buttonIndex;
                if (changedSelection) {
                    app.getAudioManager().playSFXByName("menu_hover");
                }
                updateButtonHighlight();
            }

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            updateButtonHighlight();

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
        
        // Клик по кнопке
        button.setOnAction(e -> handleMenuAction(action));
        
        return button;
    }
    
    private ImageView createFloatingLogo() {
        try {
            // Загрузка логотипа игры
            Image logoImage = ImageCache.get("arcadeblocks_logo_vertical.png");
            ImageView logoImageView = new ImageView(logoImage);
            
            // Настройка размеров логотипа
            logoImageView.setFitWidth(160);
            logoImageView.setFitHeight(192);
            logoImageView.setPreserveRatio(true);
            logoImageView.setSmooth(true);
            
            // Позиционирование в правом нижнем углу с отступами
            logoImageView.setTranslateX(-30); // Отступ от правого края
            logoImageView.setTranslateY(-30); // Отступ от нижнего края
            
            // Добавляем неоновый эффект
            logoImageView.setStyle(
                "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 0.6, 0, 0);"
            );
            
            // Анимация появления с задержкой
            logoImageView.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), logoImageView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(0.8);
            fadeIn.setDelay(Duration.seconds(0.5)); // Задержка после появления основного контента
            fadeIn.play();
            
            // Анимация левитации (плавное движение вверх-вниз)
            TranslateTransition levitation = new TranslateTransition(Duration.seconds(3.0), logoImageView);
            levitation.setFromY(0);
            levitation.setToY(-15); // Поднимаем на 15 пикселей
            levitation.setAutoReverse(true);
            levitation.setCycleCount(TranslateTransition.INDEFINITE);
            levitation.setDelay(Duration.seconds(1.0)); // Начинаем левитацию после появления
            levitation.play();
            
            // Анимация легкого вращения
            ScaleTransition rotation = new ScaleTransition(Duration.seconds(4.0), logoImageView);
            rotation.setFromX(1.0);
            rotation.setFromY(1.0);
            rotation.setToX(1.05);
            rotation.setToY(1.05);
            rotation.setAutoReverse(true);
            rotation.setCycleCount(ScaleTransition.INDEFINITE);
            rotation.setDelay(Duration.seconds(1.0));
            rotation.play();
            
            return logoImageView;
            
        } catch (Exception e) {
            System.err.println("Не удалось загрузить логотип игры: " + e.getMessage());
            
            // Создаем заглушку в случае ошибки
            ImageView fallbackLogo = new ImageView();
            fallbackLogo.setFitWidth(160);
            fallbackLogo.setFitHeight(192);
            fallbackLogo.setTranslateX(-30);
            fallbackLogo.setTranslateY(-30);
            fallbackLogo.setStyle(
                "-fx-background-color: rgba(100, 255, 255, 0.3); " +
                "-fx-background-radius: 80; " +
                "-fx-border-color: " + GameConfig.NEON_CYAN + "; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 80; " +
                "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 0.6, 0, 0);"
            );
            
            // Анимация появления для заглушки
            fallbackLogo.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), fallbackLogo);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(0.8);
            fadeIn.setDelay(Duration.seconds(0.5));
            fadeIn.play();
            
            return fallbackLogo;
        }
    }
    
    /**
     * Создание левитирующего логотипа GamesHopes в левом нижнем углу
     */
    private ImageView createLeftFloatingLogo() {
        try {
            // Загрузка логотипа GamesHopes
            Image logoImage = ImageCache.get("Leocallidus_games_logo.png");
            ImageView logoImageView = new ImageView(logoImage);
            
            // Настройка размеров логотипа
            logoImageView.setFitWidth(160);
            logoImageView.setFitHeight(192);
            logoImageView.setPreserveRatio(true);
            logoImageView.setSmooth(true);
            
            // Позиционирование в левом нижнем углу с отступами
            logoImageView.setTranslateX(30); // Отступ от левого края
            logoImageView.setTranslateY(-30); // Отступ от нижнего края
            
            // Добавляем неоновый эффект
            logoImageView.setStyle(
                "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 0.6, 0, 0);"
            );
            
            // Анимация появления с задержкой
            logoImageView.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), logoImageView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(0.8);
            fadeIn.setDelay(Duration.seconds(0.5)); // Задержка после появления основного контента
            fadeIn.play();
            
            // Анимация левитации (плавное движение вверх-вниз)
            TranslateTransition levitation = new TranslateTransition(Duration.seconds(3.0), logoImageView);
            levitation.setFromY(0);
            levitation.setToY(-15); // Поднимаем на 15 пикселей
            levitation.setAutoReverse(true);
            levitation.setCycleCount(TranslateTransition.INDEFINITE);
            levitation.setDelay(Duration.seconds(1.0)); // Начинаем левитацию после появления
            levitation.play();
            
            // Анимация легкого вращения
            ScaleTransition rotation = new ScaleTransition(Duration.seconds(4.0), logoImageView);
            rotation.setFromX(1.0);
            rotation.setFromY(1.0);
            rotation.setToX(1.05);
            rotation.setToY(1.05);
            rotation.setAutoReverse(true);
            rotation.setCycleCount(ScaleTransition.INDEFINITE);
            rotation.setDelay(Duration.seconds(1.0));
            rotation.play();
            
            return logoImageView;
            
        } catch (Exception e) {
            System.err.println("Не удалось загрузить левый логотип LCGames: " + e.getMessage());
            
            // Создаем заглушку в случае ошибки
            ImageView fallbackLogo = new ImageView();
            fallbackLogo.setFitWidth(160);
            fallbackLogo.setFitHeight(192);
            fallbackLogo.setTranslateX(30);
            fallbackLogo.setTranslateY(-30);
            fallbackLogo.setStyle(
                "-fx-background-color: rgba(100, 255, 255, 0.3); " +
                "-fx-background-radius: 80; " +
                "-fx-border-color: " + GameConfig.NEON_CYAN + "; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 80; " +
                "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 0.6, 0, 0);"
            );
            
            // Анимация появления для заглушки
            fallbackLogo.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), fallbackLogo);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(0.8);
            fadeIn.setDelay(Duration.seconds(0.5));
            fadeIn.play();
            
            return fallbackLogo;
        }
    }
    
    
    private void showSaveGameWindow() {
        // Показываем окно выбора игровой линии
        FXGL.getGameScene().addUINode(new GameLineSelectionView(app));
    }
    
    private void startNewGame() {
        // Сброс прогресса и начало новой игры
        app.getSaveManager().resetGameProgress();
        app.startLevel(1, true); // Сбрасываем счет при новой игре
    }
    
    private void exitGame() {
        // Стилизованное подтверждение выхода
        com.arcadeblocks.ui.GameExitConfirmView.show(
            // Действие при выходе
            () -> {
                app.exitGame();
            },
            // Действие при отмене (ничего не делаем, просто закрываем диалог)
            () -> {
                // Диалог закроется автоматически
            }
        );
    }
    
    /**
     * Настройка обработчика клавиш для debug меню
     */
    private void setupKeyHandler() {
        this.setFocusTraversable(true);
        
        // Обработчик клавиш - работает всегда
        sceneKeyHandler = event -> {
            // Игнорируем если есть оверлеи (кроме debug меню)
            if (app.hasOverlayWindows() && !isDebugMenuOpen) {
                return;
            }
            
            if (event.getCode() == KeyCode.F1) {
                if (!f1KeyPressed && !isDebugMenuOpen) {
                    f1KeyPressed = true;
                    app.getAudioManager().playSFXByName("menu_select");
                    showDebugMenu();
                }
                event.consume();
                return;
            }
            
            if (event.getCode() == KeyCode.ESCAPE && isDebugMenuOpen && debugMenu != null) {
                if (debugMenu instanceof DebugMenuView) {
                    ((DebugMenuView) debugMenu).closeDebugMenu();
                }
                event.consume();
                return;
            }
            
            if (!isDebugMenuOpen) {
                handleMenuNavigation(event);
            }
        };
        
        // Обработчик отпускания клавиш
        releaseHandler = event -> {
            if (event.getCode() == KeyCode.F1) {
                f1KeyPressed = false;
            }
        };
        
        // Устанавливаем обработчики на элемент
        this.setOnKeyPressed(sceneKeyHandler);
        this.setOnKeyReleased(releaseHandler);
        
        // Добавляем обработчик на сцену для гарантии работы
        sceneListener = (obs, oldScene, newScene) -> {
            if (oldScene != null && sceneKeyHandler != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, sceneKeyHandler);
                oldScene.removeEventFilter(KeyEvent.KEY_RELEASED, releaseHandler);
            }
            if (newScene != null && sceneKeyHandler != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, sceneKeyHandler);
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, releaseHandler);
                Platform.runLater(this::requestFocus);
            }
        };
        this.sceneProperty().addListener(sceneListener);
        
        // Если сцена уже есть, добавляем обработчик сразу
        Scene scene = this.getScene();
        if (scene != null) {
            scene.addEventFilter(KeyEvent.KEY_PRESSED, sceneKeyHandler);
            scene.addEventFilter(KeyEvent.KEY_RELEASED, releaseHandler);
        }

        // Запрашиваем фокус
        Platform.runLater(this::requestFocus);
    }


    private void showDebugMenu() {
        // КРИТИЧНО: Останавливаем старые анимации открытия, если они еще активны
        if (debugMenuFadeIn != null) {
            try {
                debugMenuFadeIn.stop();
            } catch (Exception ignored) {}
            debugMenuFadeIn = null;
        }
        if (debugMenuSlideIn != null) {
            try {
                debugMenuSlideIn.stop();
            } catch (Exception ignored) {}
            debugMenuSlideIn = null;
        }
        
        // КРИТИЧНО: Если старое меню еще существует, сначала полностью очищаем его
        if (debugMenu != null) {
            try {
                if (debugMenu instanceof DebugMenuView) {
                    ((DebugMenuView) debugMenu).cleanup();
                }
                FXGL.getGameScene().removeUINode(debugMenu);
            } catch (Exception ignored) {}
            debugMenu = null;
        }
        
        // Сбрасываем флаг перед созданием нового меню
        isDebugMenuOpen = false;
        
        // Добавляем debug меню поверх главного меню с плавной анимацией
        debugMenu = new DebugMenuView(app);
        debugMenu.setOnCloseCallback(this::closeDebugMenu);
        debugMenu.setOpacity(0);
        FXGL.getGameScene().addUINode(debugMenu);
        isDebugMenuOpen = true;
        
        // Устанавливаем начальную позицию для анимации
        debugMenu.setTranslateY(20);

        // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки при необходимости
        debugMenuFadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), debugMenu);
        debugMenuFadeIn.setFromValue(0.0);
        debugMenuFadeIn.setToValue(1.0);
        debugMenuSlideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(350), debugMenu);
        debugMenuSlideIn.setFromY(20);
        debugMenuSlideIn.setToY(0);
        
        // КРИТИЧНО: Очищаем ссылки на анимации после их завершения
        debugMenuFadeIn.setOnFinished(e -> {
            debugMenuFadeIn = null;
        });
        debugMenuSlideIn.setOnFinished(e -> {
            debugMenuSlideIn = null;
        });
        
        debugMenuFadeIn.play();
        debugMenuSlideIn.play();
        
        // КРИТИЧНО: Даем фокус debug меню, чтобы оно могло обрабатывать клавиши (ESC и т.д.)
        // Но также MainMenuView будет обрабатывать ESC как резервный вариант, если debug меню не получит фокус
        Platform.runLater(() -> {
            if (debugMenu != null) {
                debugMenu.requestFocus();
            }
        });
    }
    
    /**
     * Закрыть debug меню
     * КРИТИЧНО: Этот метод вызывается из callback DebugMenuView.closeDebugMenu()
     * DebugMenuView уже сам удаляет себя из сцены и вызывает cleanup(), поэтому
     * здесь мы просто обнуляем ссылки и сбрасываем флаги
     */
    private void closeDebugMenu() {
        // КРИТИЧНО: Проверяем, не закрыто ли уже меню (защита от двойного вызова)
        if (!isDebugMenuOpen) {
            return;
        }
        
        // КРИТИЧНО: Сбрасываем флаг сразу, чтобы можно было открыть меню снова
        isDebugMenuOpen = false;
        // КРИТИЧНО: Сбрасываем флаг нажатия F1, чтобы можно было открыть меню сразу после закрытия
        f1KeyPressed = false;
        
        // КРИТИЧНО: Просто обнуляем ссылку - DebugMenuView уже сам удалил себя из сцены и вызвал cleanup()
        // Не нужно вызывать cleanup() или removeUINode() здесь, так как это уже сделано в DebugMenuView.closeDebugMenu()
        debugMenu = null;
        
        // КРИТИЧНО: Возвращаем фокус главному меню после закрытия debug меню
        // Это нужно для правильной обработки клавиш (особенно F1)
        Platform.runLater(() -> {
            try {
                this.requestFocus();
            } catch (Exception ignored) {}
        });
    }
    
    /**
     * Возвращает фокус на главное меню (используется после закрытия оверлеев)
     */
    public void restoreFocus() {
        this.setFocusTraversable(true);
        Platform.runLater(this::requestFocus);
    }
    
    
    /**
     * Обработка навигации по меню
     */
    private void handleMenuNavigation(javafx.scene.input.KeyEvent event) {
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
                selectCurrentButton();
                event.consume();
                break;
            case SPACE:
                selectCurrentButton();
                event.consume();
                break;
            default:
                // Игнорируем остальные клавиши
                break;
        }
    }
    
    /**
     * Перемещение вверх по меню
     */
    private void moveUp() {
        if (currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    /**
     * Перемещение вниз по меню
     */
    private void moveDown() {
        if (currentButtonIndex < menuButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    /**
     * Выбор текущей кнопки
     */
    private void selectCurrentButton() {
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
    
    /**
     * Обновление визуального выделения кнопок
     */
    private void updateButtonHighlight() {
        if (menuButtons == null) return;
        
        for (int i = 0; i < menuButtons.length; i++) {
            Button button = menuButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка - яркий цвет и увеличенный размер
                button.setStyle(
                    "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 10, 0.5, 0, 0);"
                );
                // Анимация увеличения
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), button);
                scaleUp.setToX(1.1);
                scaleUp.setToY(1.1);
                scaleUp.play();
            } else {
                // Обычная кнопка - стандартный стиль
                button.setStyle(
                    "-fx-background-color: " + GameConfig.DARK_BACKGROUND + "; " +
                    "-fx-border-color: " + GameConfig.NEON_PURPLE + "; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_PURPLE + ", 5, 0.3, 0, 0);"
                );
                // Анимация уменьшения
                ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), button);
                scaleDown.setToX(1.0);
                scaleDown.setToY(1.0);
                scaleDown.play();
            }
        }
    }
}
