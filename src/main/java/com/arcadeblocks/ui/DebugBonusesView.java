package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.BonusConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.utils.ImageCache;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug-меню для управления бонусами
 */
public class DebugBonusesView extends VBox {
    
    private ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private Map<String, CheckBox> bonusCheckBoxes;
    private Map<String, Boolean> originalStates;
    private Map<String, javafx.beans.value.ChangeListener<Boolean>> checkboxListeners; // КРИТИЧНО: Сохраняем ссылки на listeners
    private Runnable onCloseCallback;
    private Button[] menuButtons;
    private int currentButtonIndex = 0;
    private long lastMenuActionTime = 0L;
    private static final long ACTION_COOLDOWN_NANOS = 250_000_000L; // ~250ms
    private boolean isLoadingStates = false; // Флаг для предотвращения звука при загрузке состояний
    
    // Анимации появления и закрытия
    private javafx.animation.FadeTransition appearFadeTransition;
    private javafx.animation.TranslateTransition appearSlideTransition;
    private javafx.animation.FadeTransition closeFadeTransition;
    private javafx.animation.ScaleTransition closeScaleTransition;
    private boolean isClosing = false;
    
    public DebugBonusesView(ArcadeBlocksApp app) {
        this.app = app;
        this.bonusCheckBoxes = new HashMap<>();
        this.originalStates = new HashMap<>();
        this.checkboxListeners = new HashMap<>(); // КРИТИЧНО: Инициализируем Map для listeners
        
        initializeUI();
        loadBonusStates();
        setupKeyHandler();
        playAppearAnimation();
    }
    
    /**
     * Очистка ресурсов для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимации появления
        if (appearFadeTransition != null) {
            try {
                appearFadeTransition.stop();
            } catch (Exception ignored) {}
            appearFadeTransition = null;
        }
        if (appearSlideTransition != null) {
            try {
                appearSlideTransition.stop();
            } catch (Exception ignored) {}
            appearSlideTransition = null;
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
        
        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        
        // КРИТИЧНО: Сначала очищаем все обработчики checkboxes перед удалением из children
        // Удаляем listeners, используя сохраненные ссылки
        if (bonusCheckBoxes != null && checkboxListeners != null) {
            for (Map.Entry<String, CheckBox> entry : bonusCheckBoxes.entrySet()) {
                CheckBox checkBox = entry.getValue();
                if (checkBox != null) {
                    String fieldName = entry.getKey();
                    javafx.beans.value.ChangeListener<Boolean> listener = checkboxListeners.get(fieldName);
                    if (listener != null) {
                        checkBox.selectedProperty().removeListener(listener);
                    }
                    checkBox.setOnAction(null);
                    // КРИТИЧНО: Отвязываем textProperty() у CheckBox если он был привязан
                    if (checkBox instanceof javafx.scene.control.Labeled) {
                        ((javafx.scene.control.Labeled) checkBox).textProperty().unbind();
                    }
                }
            }
        }
        
        // Очищаем обработчики кнопок меню
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
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        // Это удаляет listeners на localeProperty, которые создаются через localizationManager.bind()
        // Делаем это ПОСЛЕ очистки обработчиков, но ПЕРЕД удалением children
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Отвязываем listeners ResponsiveLayoutHelper ПЕРЕД удалением children
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        // Делаем это ПОСЛЕ очистки всех listeners и обработчиков
        getChildren().clear();
        
        setBackground(null);
        
        // Очищаем коллекции ПОСЛЕ удаления children
        if (originalStates != null) {
            originalStates.clear();
            originalStates = null;
        }
        if (bonusCheckBoxes != null) {
            bonusCheckBoxes.clear();
            bonusCheckBoxes = null;
        }
        if (checkboxListeners != null) {
            checkboxListeners.clear();
            checkboxListeners = null;
        }
        
        // КРИТИЧНО: Обнуляем ссылки для предотвращения утечек памяти
        menuButtons = null;
        app = null;
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
        setSpacing(15);
        // Применяем CSS стили
        getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
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
        } catch (Exception ex) {
            setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
        }
        setPadding(new Insets(20));
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "debug.bonuses.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        
        // Информация о клавишах
        Label infoLabel = new Label();
        localizationManager.bind(infoLabel, "debug.bonuses.info");
        infoLabel.setFont(Font.font("Orbitron", 14));
        infoLabel.setTextFill(Color.web("#CCCCCC"));
        infoLabel.setAlignment(Pos.CENTER);
        
        // Основной контейнер с прокруткой
        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.getStyleClass().add("scroll-pane");
        
        // Контейнер для бонусов
        VBox bonusesContainer = new VBox(10);
        bonusesContainer.setAlignment(Pos.CENTER);
        bonusesContainer.setPadding(new Insets(10));
        
        // Создаем секции для разных типов бонусов
        VBox positiveSection = createBonusSection("debug.bonuses.section.positive", GameConfig.NEON_GREEN);
        VBox negativeSection = createBonusSection("debug.bonuses.section.negative", GameConfig.NEON_PINK);
        VBox settingsSection = createBonusSection("debug.bonuses.section.testing", GameConfig.NEON_PURPLE);
        
        bonusesContainer.getChildren().addAll(positiveSection, negativeSection, settingsSection);
        
        scrollPane.setContent(bonusesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Кнопки управления + Назад
        HBox buttonBox = createButtonBox();
        Button backButton = createBackButton();
        buttonBox.getChildren().add(0, backButton);
        
        // Добавляем кнопку back в массив menuButtons для навигации
        if (menuButtons != null) {
            Button[] allButtons = new Button[menuButtons.length + 1];
            allButtons[0] = backButton;
            System.arraycopy(menuButtons, 0, allButtons, 1, menuButtons.length);
            menuButtons = allButtons;
        } else {
            menuButtons = new Button[]{backButton};
        }
        
        // Обновляем выделение с учетом добавленной кнопки back
        updateButtonHighlight();
        
        getChildren().addAll(titleLabel, infoLabel, scrollPane, buttonBox);
        // Центрируем сам view
        // app.centerUINode(this);

        ResponsiveLayoutHelper.bindToStage(this, (width, height) -> {
            double adjustedWidth = Math.max(width - 40, 600);
            double adjustedHeight = Math.max(height - 200, 400);
            scrollPane.setPrefViewportWidth(adjustedWidth);
            scrollPane.setPrefViewportHeight(adjustedHeight);
            scrollPane.setPrefSize(adjustedWidth, adjustedHeight);
        });
        setUserData("fullScreenOverlay");
    }

    private Button createBackButton() {
        Button button = new Button();
        localizationManager.bind(button, "debug.bonuses.button.back");
        button.setPrefSize(GameConfig.MENU_BUTTON_WIDTH, GameConfig.MENU_BUTTON_HEIGHT);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        String normal = String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE, GameConfig.NEON_PURPLE
        );
        String hover = String.format(
            "-fx-background-color: %s40; -fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, %s, 8, 0.4, 0, 0);",
            GameConfig.NEON_CYAN, GameConfig.NEON_CYAN, GameConfig.NEON_CYAN
        );
        button.setStyle(normal);
        button.setOnMouseEntered(e -> {
            // Воспроизводим звук только если кнопка не активна
            if (menuButtons != null && button != menuButtons[currentButtonIndex]) {
                app.getAudioManager().playSFXByName("menu_hover");
            }
            
            // Временное визуальное выделение при наведении мыши
            // Сохраняем текущий стиль для восстановления
            button.setUserData(button.getStyle());
            button.setStyle(hover);
            
            // Анимация увеличения
            javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.setOnFinished(e2 -> scaleUp.stop());
            scaleUp.play();
        });
        button.setOnMouseExited(e -> {
            // Восстанавливаем исходный стиль только если кнопка не выделена клавиатурой
            if (menuButtons != null && button != menuButtons[currentButtonIndex]) {
                if (button.getUserData() != null) {
                    button.setStyle((String) button.getUserData());
                } else {
                    button.setStyle(normal);
                }
            }
            
            // Анимация уменьшения
            javafx.animation.ScaleTransition scaleDown = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setOnFinished(e2 -> scaleDown.stop());
            scaleDown.play();
        });
        button.setOnAction(e -> {
            app.getAudioManager().playSFXByName("menu_back");
            // Запускаем анимацию закрытия перед возвратом
            playCloseAnimation(() -> {
                if (onCloseCallback != null) onCloseCallback.run();
                // Удаляем текущий экран
                FXGL.getGameScene().removeUINode(this);
            });
        });
        return button;
    }
    
    private VBox createBonusSection(String titleKey, String color) {
        VBox section = new VBox(8);
        section.setAlignment(Pos.CENTER);
        
        Label sectionTitle = new Label();
        localizationManager.bind(sectionTitle, titleKey);
        sectionTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web(color));
        sectionTitle.setAlignment(Pos.CENTER);
        
        VBox checkBoxesContainer = new VBox(5);
        checkBoxesContainer.setAlignment(Pos.CENTER);
        checkBoxesContainer.setPadding(new Insets(15));
        
        // Добавляем чекбоксы в зависимости от секции
        switch (titleKey) {
            case "debug.bonuses.section.positive":
                addPositiveBonuses(checkBoxesContainer);
                break;
            case "debug.bonuses.section.negative":
                addNegativeBonuses(checkBoxesContainer);
                break;
            case "debug.bonuses.section.testing":
                addTestSettings(checkBoxesContainer);
                break;
        }
        
        // Полупрозрачный скругленный контейнер для секции
        VBox sectionContainer = new VBox(10);
        sectionContainer.setAlignment(Pos.CENTER);
        sectionContainer.setPadding(new Insets(20));
        sectionContainer.setMaxWidth(500);
        sectionContainer.setStyle(
            "-fx-background-color: rgba(15, 15, 28, 0.39);" +
            "-fx-background-radius: 16px;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 18, 0.35, 0, 0);"
        );
        
        sectionContainer.getChildren().addAll(sectionTitle, checkBoxesContainer);
        section.getChildren().add(sectionContainer);
        
        return section;
    }
    
    private void addPositiveBonuses(VBox container) {
        String[][] positiveBonuses = {
            {"INCREASE_PADDLE_ENABLED", "debug.bonuses.bonus.bigger_paddle"},
            {"STICKY_PADDLE_ENABLED", "debug.bonuses.bonus.sticky_paddle"},
            {"SLOW_BALLS_ENABLED", "debug.bonuses.bonus.slow_balls"},
            {"ENERGY_BALLS_ENABLED", "debug.bonuses.bonus.energy_balls"},
            {"EXPLOSION_BALLS_ENABLED", "debug.bonuses.bonus.explosive_balls"},
            {"BONUS_BALL_ENABLED", "debug.bonuses.bonus.extra_ball"},
            {"BONUS_SCORE_ENABLED", "debug.bonuses.bonus.extra_points"},
            {"BONUS_SCORE_200_ENABLED", "debug.bonuses.bonus.extra_points_200"},
            {"BONUS_SCORE_500_ENABLED", "debug.bonuses.bonus.extra_points_500"},
            {"ADD_FIVE_SECONDS_ENABLED", "debug.bonuses.bonus.add_five_seconds"},
            {"CALL_BALL_ENABLED", "debug.bonuses.bonus.call_ball"},
            {"EXTRA_LIFE_ENABLED", "debug.bonuses.bonus.extra_life"},
            {"BONUS_WALL_ENABLED", "debug.bonuses.bonus.shield_barrier"},
            {"BONUS_MAGNET_ENABLED", "debug.bonuses.bonus.bonus_magnet"},
            {"PLASMA_WEAPON_ENABLED", "debug.bonuses.bonus.plasma_weapon"},
            {"LEVEL_PASS_ENABLED", "debug.bonuses.bonus.level_pass"},
            {"SCORE_RAIN_ENABLED", "debug.bonuses.bonus.score_rain"},
            {"TRICKSTER_ENABLED", "debug.bonuses.bonus.trickster"},
            {"RANDOM_BONUS_ENABLED", "debug.bonuses.bonus.random_bonus"}
        };
        
        for (String[] bonus : positiveBonuses) {
            container.getChildren().add(createBonusCheckBox(bonus[0], bonus[1]));
        }
    }
    
    private void addNegativeBonuses(VBox container) {
        String[][] negativeBonuses = {
            {"CHAOTIC_BALLS_ENABLED", "debug.bonuses.bonus.chaotic_balls"},
            {"FROZEN_PADDLE_ENABLED", "debug.bonuses.bonus.frozen_paddle"},
            {"DECREASE_PADDLE_ENABLED", "debug.bonuses.bonus.smaller_paddle"},
            {"FAST_BALLS_ENABLED", "debug.bonuses.bonus.fast_balls"},
            {"WEAK_BALLS_ENABLED", "debug.bonuses.bonus.weak_balls"},
            {"INVISIBLE_PADDLE_ENABLED", "debug.bonuses.bonus.ghost_paddle"},
            {"DARKNESS_ENABLED", "debug.bonuses.bonus.darkness"},
            {"BAD_LUCK_ENABLED", "debug.bonuses.bonus.bad_luck"},
            {"RESET_ENABLED", "debug.bonuses.bonus.reset_bonuses"},
            {"PENALTIES_MAGNET_ENABLED", "debug.bonuses.bonus.penalty_magnet"}
        };
        
        for (String[] bonus : negativeBonuses) {
            container.getChildren().add(createBonusCheckBox(bonus[0], bonus[1]));
        }
    }
    
    private void addTestSettings(VBox container) {
        String[][] testSettings = {
            {"TESTING_MODE", "debug.bonuses.bonus.testing_mode"},
            {"POSITIVE_BONUSES_ONLY", "debug.bonuses.bonus.positive_only"},
            {"NEGATIVE_BONUSES_ONLY", "debug.bonuses.bonus.negative_only"},
            {"DISABLE_ALL_BONUSES", "debug.bonuses.bonus.disable_all"}
        };
        
        for (String[] setting : testSettings) {
            container.getChildren().add(createBonusCheckBox(setting[0], setting[1]));
        }
    }
    
    private HBox createBonusCheckBox(String fieldName, String localizationKey) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        
        Label label = new Label();
        localizationManager.bind(label, localizationKey);
        label.setFont(Font.font("Orbitron", 14));
        label.setTextFill(Color.web("#E0E0E0"));
        label.setPrefWidth(250);
        
        CheckBox checkBox = new CheckBox();
        checkBox.setPrefSize(24, 24);
        checkBox.getStyleClass().add("check-box");
        
        // КРИТИЧНО: Сохраняем ссылку на listener для правильного удаления в cleanup()
        javafx.beans.value.ChangeListener<Boolean> listener = (obs, oldVal, newVal) -> {
            updateBonusConfig(fieldName, newVal);
        };
        checkBox.selectedProperty().addListener(listener);
        
        box.getChildren().addAll(label, checkBox);
        
        // Сохраняем ссылку на чекбокс и listener
        bonusCheckBoxes.put(fieldName, checkBox);
        checkboxListeners.put(fieldName, listener);
        
        return box;
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button applyButton = createButton("debug.bonuses.button.apply", GameConfig.NEON_GREEN, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_select");
            // Изменения уже применены через обработчики чекбоксов
            closeDebugMenu();
        });
        
        Button resetButton = createButton("debug.bonuses.button.reset", GameConfig.NEON_ORANGE, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("settings_change");
            resetToDefaults();
        });

        Button clearButton = createButton("debug.bonuses.button.clear", GameConfig.NEON_PINK, () -> {
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("settings_change");
            clearAllBonusesSelections();
        });
        
        Button closeButton = createButton("debug.bonuses.button.close", GameConfig.NEON_CYAN, () -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_back");
            // Запускаем анимацию закрытия перед возвратом в главное меню
            playCloseAnimation(() -> {
                // Закрываем все debug меню и возвращаемся в главное меню без перезагрузки музыки и фона
                returnToMainMenuWithoutReload();
            });
        });
        
        buttonBox.getChildren().addAll(applyButton, resetButton, clearButton, closeButton);
        
        // Сохраняем ссылки на кнопки для навигации
        menuButtons = new Button[]{applyButton, resetButton, clearButton, closeButton};
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();
        
        return buttonBox;
    }
    
    private Button createButton(String localizationKey, String color, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, localizationKey);
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
            
            // Анимация увеличения
            javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.setOnFinished(e2 -> scaleUp.stop());
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // Восстанавливаем исходный стиль только если кнопка не выделена клавиатурой
            if (menuButtons != null && button != menuButtons[currentButtonIndex]) {
                if (button.getUserData() != null) {
                    button.setStyle((String) button.getUserData());
                }
            }
            
            // Анимация уменьшения
            javafx.animation.ScaleTransition scaleDown = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setOnFinished(e2 -> scaleDown.stop());
            scaleDown.play();
        });
        
        // Клик по кнопке
        button.setOnAction(e -> handleMenuAction(action));
        
        return button;
    }
    
    private void setupKeyHandler() {
        this.setFocusTraversable(true);
        
        // Пытаемся получить фокус через Platform.runLater
        Platform.runLater(() -> {
            this.requestFocus();
        });
        
        this.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F1:
                case ESCAPE:
                    app.getAudioManager().playSFXByName("menu_back");
                    // Проверяем, есть ли callback - если есть, возвращаемся к debug меню, иначе в главное меню
                    if (onCloseCallback != null) {
                        closeDebugMenu();
                    } else {
                        returnToMainMenuWithoutReload();
                    }
                    event.consume();
                    break;
                case UP:
                case LEFT:
                    moveUp();
                    event.consume();
                    break;
                case DOWN:
                case RIGHT:
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
    
    private void moveUp() {
        if (currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    private void moveDown() {
        if (currentButtonIndex < menuButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
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
    
    private void updateButtonHighlight() {
        if (menuButtons == null) return;
        
        for (int i = 0; i < menuButtons.length; i++) {
            Button button = menuButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка - яркий цвет
                button.setStyle(
                    "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 10, 0.5, 0, 0);"
                );
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
            }
        }
    }
    
    private void loadBonusStates() {
        // КРИТИЧНО: Проверяем app на null перед использованием
        if (app == null) {
            return;
        }
        
        // Загружаем настройки из базы данных в BonusConfig
        if (app.getSaveManager() != null) {
            app.getSaveManager().loadDebugSettingsToBonusConfig();
        }
        
        // Загружаем текущие состояния бонусов
        originalStates.put("INCREASE_PADDLE_ENABLED", BonusConfig.getBonusEnabled("INCREASE_PADDLE_ENABLED"));
        originalStates.put("DECREASE_PADDLE_ENABLED", BonusConfig.getBonusEnabled("DECREASE_PADDLE_ENABLED"));
        originalStates.put("STICKY_PADDLE_ENABLED", BonusConfig.getBonusEnabled("STICKY_PADDLE_ENABLED"));
        originalStates.put("FAST_BALLS_ENABLED", BonusConfig.getBonusEnabled("FAST_BALLS_ENABLED"));
        originalStates.put("SLOW_BALLS_ENABLED", BonusConfig.getBonusEnabled("SLOW_BALLS_ENABLED"));
        originalStates.put("ENERGY_BALLS_ENABLED", BonusConfig.getBonusEnabled("ENERGY_BALLS_ENABLED"));
        originalStates.put("EXPLOSION_BALLS_ENABLED", BonusConfig.getBonusEnabled("EXPLOSION_BALLS_ENABLED"));
        originalStates.put("WEAK_BALLS_ENABLED", BonusConfig.getBonusEnabled("WEAK_BALLS_ENABLED"));
        originalStates.put("BONUS_BALL_ENABLED", BonusConfig.getBonusEnabled("BONUS_BALL_ENABLED"));
        originalStates.put("BONUS_SCORE_ENABLED", BonusConfig.getBonusEnabled("BONUS_SCORE_ENABLED"));
        originalStates.put("BONUS_SCORE_200_ENABLED", BonusConfig.getBonusEnabled("BONUS_SCORE_200_ENABLED"));
        originalStates.put("BONUS_SCORE_500_ENABLED", BonusConfig.getBonusEnabled("BONUS_SCORE_500_ENABLED"));
        originalStates.put("ADD_FIVE_SECONDS_ENABLED", BonusConfig.getBonusEnabled("ADD_FIVE_SECONDS_ENABLED"));
        originalStates.put("CALL_BALL_ENABLED", BonusConfig.getBonusEnabled("CALL_BALL_ENABLED"));
        originalStates.put("EXTRA_LIFE_ENABLED", BonusConfig.getBonusEnabled("EXTRA_LIFE_ENABLED"));
        originalStates.put("BONUS_WALL_ENABLED", BonusConfig.getBonusEnabled("BONUS_WALL_ENABLED"));
        originalStates.put("BONUS_MAGNET_ENABLED", BonusConfig.getBonusEnabled("BONUS_MAGNET_ENABLED"));
        originalStates.put("PLASMA_WEAPON_ENABLED", BonusConfig.getBonusEnabled("PLASMA_WEAPON_ENABLED"));
        originalStates.put("LEVEL_PASS_ENABLED", BonusConfig.getBonusEnabled("LEVEL_PASS_ENABLED"));
        originalStates.put("SCORE_RAIN_ENABLED", BonusConfig.getBonusEnabled("SCORE_RAIN_ENABLED"));
        originalStates.put("BAD_LUCK_ENABLED", BonusConfig.getBonusEnabled("BAD_LUCK_ENABLED"));
        originalStates.put("TRICKSTER_ENABLED", BonusConfig.getBonusEnabled("TRICKSTER_ENABLED"));
        originalStates.put("CHAOTIC_BALLS_ENABLED", BonusConfig.getBonusEnabled("CHAOTIC_BALLS_ENABLED"));
        originalStates.put("FROZEN_PADDLE_ENABLED", BonusConfig.getBonusEnabled("FROZEN_PADDLE_ENABLED"));
        originalStates.put("INVISIBLE_PADDLE_ENABLED", BonusConfig.getBonusEnabled("INVISIBLE_PADDLE_ENABLED"));
        originalStates.put("DARKNESS_ENABLED", BonusConfig.getBonusEnabled("DARKNESS_ENABLED"));
        originalStates.put("RESET_ENABLED", BonusConfig.getBonusEnabled("RESET_ENABLED"));
        originalStates.put("PENALTIES_MAGNET_ENABLED", BonusConfig.getBonusEnabled("PENALTIES_MAGNET_ENABLED"));
        originalStates.put("RANDOM_BONUS_ENABLED", BonusConfig.getBonusEnabled("RANDOM_BONUS_ENABLED"));
        originalStates.put("TESTING_MODE", BonusConfig.getBonusEnabled("TESTING_MODE"));
        originalStates.put("POSITIVE_BONUSES_ONLY", BonusConfig.getBonusEnabled("POSITIVE_BONUSES_ONLY"));
        originalStates.put("NEGATIVE_BONUSES_ONLY", BonusConfig.getBonusEnabled("NEGATIVE_BONUSES_ONLY"));
        originalStates.put("DISABLE_ALL_BONUSES", BonusConfig.getBonusEnabled("DISABLE_ALL_BONUSES"));
        
        // Устанавливаем состояния чекбоксов без проигрывания звука
        isLoadingStates = true;
        for (Map.Entry<String, CheckBox> entry : bonusCheckBoxes.entrySet()) {
            String fieldName = entry.getKey();
            CheckBox checkBox = entry.getValue();
            Boolean state = originalStates.get(fieldName);
            if (state != null) {
                checkBox.setSelected(state);
            }
        }
        isLoadingStates = false;
    }
    
    private void updateBonusConfig(String fieldName, boolean value) {
        // КРИТИЧНО: Проверяем app на null перед использованием
        if (app == null) {
            return;
        }
        
        // Обновляем состояние бонуса в BonusConfig
        BonusConfig.setBonusEnabled(fieldName, value);
        
        // Сохраняем изменение в базу данных
        if (app.getSaveManager() != null) {
            app.getSaveManager().saveDebugSettingsFromBonusConfig();
        }
        
        // System.out.println("DEBUG: " + fieldName + " = " + value);
        
        // Воспроизводим звук изменения только если это не загрузка состояний
        if (!isLoadingStates) {
            app.getAudioManager().playSFXByName("settings_change");
        }
    }
    
    private void resetToDefaults() {
        // КРИТИЧНО: Проверяем на null для предотвращения утечек памяти
        if (bonusCheckBoxes == null || app == null) {
            return;
        }
        
        // Включаем все чекбоксы бонусов (устанавливаем в true)
        // ИСКЛЮЧАЯ тестовые настройки (testing bonuses section)
        for (Map.Entry<String, CheckBox> entry : bonusCheckBoxes.entrySet()) {
            String fieldName = entry.getKey();
            CheckBox checkBox = entry.getValue();
            
            // Пропускаем 4 тестовые настройки
            if (fieldName.equals("TESTING_MODE") || 
                fieldName.equals("POSITIVE_BONUSES_ONLY") || 
                fieldName.equals("NEGATIVE_BONUSES_ONLY") || 
                fieldName.equals("DISABLE_ALL_BONUSES")) {
                continue;
            }
            
            // КРИТИЧНО: Проверяем checkBox на null
            if (checkBox != null) {
                checkBox.setSelected(true);
                // Также обновляем BonusConfig
                BonusConfig.setBonusEnabled(fieldName, true);
            }
        }
        
        // Сохраняем изменения в базу данных
        if (app.getSaveManager() != null) {
            app.getSaveManager().saveDebugSettingsFromBonusConfig();
        }
    }

    private void clearAllBonusesSelections() {
        if (bonusCheckBoxes == null || app == null) {
            return;
        }

        isLoadingStates = true;
        for (Map.Entry<String, CheckBox> entry : bonusCheckBoxes.entrySet()) {
            String fieldName = entry.getKey();
            CheckBox checkBox = entry.getValue();

            // Пропускаем тестовые переключатели
            if (fieldName.equals("TESTING_MODE") ||
                fieldName.equals("POSITIVE_BONUSES_ONLY") ||
                fieldName.equals("NEGATIVE_BONUSES_ONLY") ||
                fieldName.equals("DISABLE_ALL_BONUSES")) {
                continue;
            }

            if (checkBox != null) {
                checkBox.setSelected(false);
                BonusConfig.setBonusEnabled(fieldName, false);
            }
        }
        isLoadingStates = false;

        if (app.getSaveManager() != null) {
            app.getSaveManager().saveDebugSettingsFromBonusConfig();
        }
    }
    
    private void closeDebugMenu() {
        // КРИТИЧНО: Сохраняем callback перед cleanup
        Runnable callbackRef = onCloseCallback;
        
        // КРИТИЧНО: СНАЧАЛА удаляем из scene, ПОТОМ cleanup
        // Это предотвращает race condition, когда нода еще в scene, но уже "мертвая"
        FXGL.getGameScene().removeUINode(this);
        
        // Теперь безопасно вызываем cleanup, так как нода уже не в scene
        cleanup();
        
        // Уведомляем родительский компонент о закрытии
        if (callbackRef != null) {
            callbackRef.run();
        }
    }
    
    private void returnToMainMenuWithoutReload() {
        // КРИТИЧНО: НЕ создаем новый MainMenuView!
        // Просто удаляем DebugBonusesView и DebugMenuView, возвращаемся к MainMenuView
        
        var scene = FXGL.getGameScene();
        
        // КРИТИЧНО: Удаляем DebugMenuView, если он скрыт (setVisible(false))
        // Это важно, чтобы можно было открыть debug меню снова
        var debugMenu = scene.getUINodes().stream()
            .filter(node -> node instanceof com.arcadeblocks.ui.DebugMenuView)
            .findFirst();
        if (debugMenu.isPresent()) {
            com.arcadeblocks.ui.util.UINodeCleanup.cleanupNode(debugMenu.get());
            scene.removeUINode(debugMenu.get());
        }
        
        // Удаляем этот view
        com.arcadeblocks.ui.util.UINodeCleanup.cleanupNode(this);
        scene.removeUINode(this);
        
        // MainMenuView уже на экране, просто возвращаем фокус
        javafx.application.Platform.runLater(() -> {
            var mainMenu = scene.getUINodes().stream()
                .filter(node -> node instanceof com.arcadeblocks.ui.MainMenuView)
                .findFirst();
            if (mainMenu.isPresent()) {
                mainMenu.get().requestFocus();
            }
        });
    }
    
    /**
     * Анимация появления окна
     */
    private void playAppearAnimation() {
        this.setOpacity(0);
        this.setTranslateY(20);
        
        appearFadeTransition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), this);
        appearFadeTransition.setFromValue(0.0);
        appearFadeTransition.setToValue(1.0);
        
        appearSlideTransition = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(400), this);
        appearSlideTransition.setFromY(20);
        appearSlideTransition.setToY(0);
        
        appearFadeTransition.setOnFinished(e -> {
            if (appearFadeTransition != null) {
                appearFadeTransition.stop();
                appearFadeTransition = null;
            }
            if (appearSlideTransition != null) {
                appearSlideTransition.stop();
                appearSlideTransition = null;
            }
        });
        
        appearFadeTransition.play();
        appearSlideTransition.play();
    }
    
    /**
     * Анимация закрытия окна
     */
    private void playCloseAnimation(Runnable onFinished) {
        if (isClosing) {
            return;
        }
        isClosing = true;
        
        closeFadeTransition = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), this);
        closeFadeTransition.setFromValue(1.0);
        closeFadeTransition.setToValue(0.0);
        
        closeScaleTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), this);
        closeScaleTransition.setFromX(1.0);
        closeScaleTransition.setFromY(1.0);
        closeScaleTransition.setToX(0.9);
        closeScaleTransition.setToY(0.9);
        
        closeFadeTransition.setOnFinished(e -> {
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
        
        closeFadeTransition.play();
        closeScaleTransition.play();
    }
}
