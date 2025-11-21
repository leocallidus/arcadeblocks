package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.LevelConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.ui.util.UINodeCleanup;
import com.arcadeblocks.utils.ImageCache;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;

/**
 * Отладочный экран "Уровни" для быстрого перехода к любому уровню
 * Вызывается по клавише F3 в главном меню
 */
public class DebugLevelsView extends VBox {
    
    private ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private Runnable onCloseCallback;
    private List<Button> levelButtons;
    private int currentButtonIndex = 0;
    private Button[] menuButtons;
    private int currentMenuButtonIndex = 0;
    private boolean navigatingMenuButtons = false; // Флаг режима навигации
    private long lastMenuActionTime = 0L;
    private static final long ACTION_COOLDOWN_NANOS = 250_000_000L; // ~250ms
    private boolean isCleanedUp = false; // КРИТИЧНО: Флаг для предотвращения повторных вызовов cleanup
    
    // КРИТИЧНО: Чекбокс и listener для постоянного показа сюжетных окон
    private CheckBox alwaysShowStoryCheckBox;
    private javafx.beans.value.ChangeListener<Boolean> alwaysShowStoryListener;
    
    // Анимации появления и закрытия
    private javafx.animation.FadeTransition appearFadeTransition;
    private javafx.animation.TranslateTransition appearSlideTransition;
    private javafx.animation.FadeTransition closeFadeTransition;
    private javafx.animation.ScaleTransition closeScaleTransition;
    private boolean isClosing = false;
    
    // Константы для сетки кнопок
    private static final int BUTTONS_PER_ROW = 10;
    private static final int BUTTON_SIZE = 80;
    private static final int BUTTON_SPACING = 10;
    
    public DebugLevelsView(ArcadeBlocksApp app) {
        this.app = app;
        this.levelButtons = new ArrayList<>();
        
        initializeUI();
        setupKeyHandler();
        playAppearAnimation();
    }

    /**
     * Releases listeners and handlers so the view can be garbage collected.
     */
    public void cleanup() {
        // КРИТИЧНО: Предотвращаем повторные вызовы cleanup при race condition
        if (isCleanedUp) {
            return;
        }
        isCleanedUp = true;
        
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
        
        setOnKeyPressed(null);
        setOnKeyReleased(null);
        
        // КРИТИЧНО: Сначала очищаем все обработчики кнопок перед удалением из children
        if (levelButtons != null) {
            for (Button button : levelButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    button.setOnMouseEntered(null);
                    button.setOnMouseExited(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button
                    button.textProperty().unbind();
                }
            }
            levelButtons.clear();
            levelButtons = null;
        }

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
        
        // КРИТИЧНО: Очищаем чекбокс для постоянного показа сюжета
        if (alwaysShowStoryCheckBox != null) {
            // КРИТИЧНО: Сначала отвязываем textProperty() binding от LocalizationManager
            alwaysShowStoryCheckBox.textProperty().unbind();
            // КРИТИЧНО: Очищаем tooltip перед удалением listener'а
            alwaysShowStoryCheckBox.setTooltip(null);
            // КРИТИЧНО: Удаляем listener с selectedProperty
            if (alwaysShowStoryListener != null) {
                alwaysShowStoryCheckBox.selectedProperty().removeListener(alwaysShowStoryListener);
                alwaysShowStoryListener = null;
            }
            alwaysShowStoryCheckBox = null;
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        // Это удаляет listeners на localeProperty, которые создаются через localizationManager.bind()
        // Делаем это ПОСЛЕ очистки обработчиков кнопок, но ПЕРЕД удалением children
        unbindAllTextProperties(this);

        // КРИТИЧНО: Освобождаем изображения через UINodeCleanup
        UINodeCleanup.releaseImages(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        // ResponsiveLayoutHelper.unbind() теперь делает синхронную очистку, если мы в FX thread
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        // Делаем это ПОСЛЕ очистки всех listeners и обработчиков
        getChildren().clear();

        setBackground(null);
        
        // КРИТИЧНО: Обнуляем ссылки для предотвращения утечек памяти
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
    
    private Button createBackButton() {
        Button button = new Button();
        localizationManager.bind(button, "debug.levels.button.back");
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
            if (menuButtons != null && menuButtons.length > 0) {
                if (button != menuButtons[currentMenuButtonIndex]) {
                    app.getAudioManager().playSFXByName("menu_hover");
                }
            } else {
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
            if (menuButtons != null && menuButtons.length > 0) {
                if (button != menuButtons[currentMenuButtonIndex]) {
                    if (button.getUserData() != null) {
                        button.setStyle((String) button.getUserData());
                    }
                }
            } else {
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
        
        button.setOnAction(e -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_back");
            // Запускаем анимацию закрытия перед возвратом
            playCloseAnimation(() -> {
                // КРИТИЧНО: Используем closeDebugLevelsView() для полной очистки цепочки
                // Это гарантирует вызов cleanup(), UINodeCleanup.releaseImages() и ImageCache.forget()
                closeDebugLevelsView(true);
            });
        });
        return button;
    }
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
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
        localizationManager.bind(titleLabel, "debug.levels.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 15, 0.5, 0, 0);");
        
        // Информация о клавишах
        Label infoLabel = new Label();
        localizationManager.bind(infoLabel, "debug.levels.info");
        infoLabel.setFont(Font.font("Orbitron", 14));
        infoLabel.setTextFill(Color.web("#CCCCCC"));
        infoLabel.setAlignment(Pos.CENTER);
        
        // Основной контейнер с прокруткой
        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Контейнер для кнопок уровней
        VBox levelsContainer = createLevelsContainer();
        scrollPane.setContent(levelsContainer);
        scrollPane.setFitToWidth(true);
        
        // Кнопки управления + Назад
        HBox buttonBox = createButtonBox();
        Button backButton = createBackButton();
        buttonBox.getChildren().add(0, backButton);
        
        // Чекбокс для постоянного показа сюжетных окон
        HBox storyCheckBoxContainer = createStoryCheckBoxContainer();
        
        // Добавляем кнопку back в массив menuButtons для навигации
        if (menuButtons != null) {
            Button[] allButtons = new Button[menuButtons.length + 1];
            allButtons[0] = backButton;
            System.arraycopy(menuButtons, 0, allButtons, 1, menuButtons.length);
            menuButtons = allButtons;
        } else {
            menuButtons = new Button[]{backButton};
        }
        
        getChildren().addAll(titleLabel, infoLabel, storyCheckBoxContainer, scrollPane, buttonBox);
        // Центрируем сам view
        // app.centerUINode(this);
        
        // Устанавливаем визуальное выделение первой кнопки уровней и первой кнопки меню
        updateButtonHighlight();
        updateMenuButtonHighlight();

        ResponsiveLayoutHelper.bindToStage(this, (width, height) -> {
            double adjustedWidth = Math.max(width - 40, 600);
            double adjustedHeight = Math.max(height - 200, 400);
            scrollPane.setPrefViewportWidth(adjustedWidth);
            scrollPane.setPrefViewportHeight(adjustedHeight);
            scrollPane.setPrefSize(adjustedWidth, adjustedHeight);
        });
        setUserData("fullScreenOverlay");
    }
    
    private VBox createLevelsContainer() {
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));
        
        // Создаем секции для разных групп уровней
        for (LevelConfig.LevelChapter chapter : LevelConfig.getChapters()) {
            String rangeText = chapter.getStartLevel() == chapter.getEndLevel()
                ? String.valueOf(chapter.getStartLevel())
                : chapter.getStartLevel() + "-" + chapter.getEndLevel();
            String sectionTitle = localizationManager.format(
                "debug.levels.section.format",
                chapter.getRomanNumeral(),
                chapter.getTitle(),
                rangeText
            );
            VBox section = createLevelSection(
                sectionTitle,
                chapter.getAccentColorHex(),
                chapter.getStartLevel(),
                chapter.getEndLevel()
            );
            container.getChildren().add(section);
        }

        return container;
    }
    
    private VBox createLevelSection(String title, String color, int startLevel, int endLevel) {
        VBox section = new VBox(10);
        section.setAlignment(Pos.CENTER);
        
        // Заголовок секции
        Label sectionTitle = new Label(title);
        sectionTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        
        // Для темных цветов используем более светлый вариант для лучшей видимости
        Color accentColor = Color.web(color);
        double brightness = accentColor.getBrightness();
        
        if (brightness < 0.3) {
            // Очень темный цвет - используем осветленную версию
            // Увеличиваем яркость в 3 раза, но не более 0.9
            double newBrightness = Math.min(brightness * 3.5, 0.9);
            accentColor = Color.hsb(
                accentColor.getHue(),
                accentColor.getSaturation() * 0.8, // Немного уменьшаем насыщенность
                newBrightness
            );
        }
        
        sectionTitle.setTextFill(accentColor);
        sectionTitle.setAlignment(Pos.CENTER);
        
        // Используем осветленный цвет для эффекта свечения
        String glowColor = String.format("#%02X%02X%02X",
            (int)(accentColor.getRed() * 255),
            (int)(accentColor.getGreen() * 255),
            (int)(accentColor.getBlue() * 255)
        );
        sectionTitle.setStyle("-fx-effect: dropshadow(gaussian, " + glowColor + ", 8, 0.4, 0, 0);");
        
        // Контейнер для кнопок в сетке
        VBox gridContainer = new VBox(BUTTON_SPACING);
        gridContainer.setAlignment(Pos.CENTER);
        
        // Создаем строки кнопок
        for (int level = startLevel; level <= endLevel; level += BUTTONS_PER_ROW) {
            HBox row = new HBox(BUTTON_SPACING);
            row.setAlignment(Pos.CENTER);
            
            // Создаем кнопки для текущей строки
            for (int i = 0; i < BUTTONS_PER_ROW && (level + i) <= endLevel; i++) {
                int currentLevel = level + i;
                Button levelButton = createLevelButton(currentLevel, color);
                row.getChildren().add(levelButton);
                levelButtons.add(levelButton);
            }
            
            gridContainer.getChildren().add(row);
        }
        
        section.getChildren().addAll(sectionTitle, gridContainer);
        return section;
    }
    
    private Button createLevelButton(int levelNumber, String sectionColor) {
        Button button = new Button(String.valueOf(levelNumber));
        button.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);
        
        // Определяем цвет в зависимости от типа уровня
        String buttonColor = sectionColor;
        
        // Стиль кнопки
        String normalStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 5, 0.3, 0, 0);",
            GameConfig.DARK_BACKGROUND, buttonColor, buttonColor
        );
        
        String hoverStyle = String.format(
            "-fx-background-color: %s40; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 10, 0.5, 0, 0);",
            buttonColor, buttonColor, buttonColor
        );
        
        String selectedStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: #FFFFFF; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, #FFFFFF, 15, 0.7, 0, 0);",
            buttonColor
        );
        
        button.setStyle(normalStyle);
        
        // Сохраняем стили в свойствах кнопки
        ButtonStyles styles = new ButtonStyles(normalStyle, hoverStyle, selectedStyle, levelNumber);
        button.setUserData(styles);
        
        // Hover эффекты
        button.setOnMouseEntered(e -> {
            // КРИТИЧНО: Проверяем app на null перед использованием
            if (app == null) {
                return;
            }
            if (levelButtons.indexOf(button) != currentButtonIndex) {
                button.setStyle(styles.hoverStyle);
                app.getAudioManager().playSFXByName("menu_hover");
            }
        });
        
        button.setOnMouseExited(e -> {
            if (levelButtons.indexOf(button) != currentButtonIndex) {
                button.setStyle(styles.normalStyle);
            }
        });
        
        // Клик по кнопке
        button.setOnAction(e -> selectLevel(levelNumber));
        
        return button;
    }
    
    private HBox createStoryCheckBoxContainer() {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(5, 0, 5, 0));
        
        // Создаем чекбокс с тем же стилем, что и в SettingsView
        alwaysShowStoryCheckBox = new CheckBox();
        localizationManager.bind(alwaysShowStoryCheckBox, "debug.levels.always_show_story");
        alwaysShowStoryCheckBox.getStyleClass().add("debug-checkbox");
        alwaysShowStoryCheckBox.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
        alwaysShowStoryCheckBox.setTextFill(Color.web("#E0E0E0"));
        alwaysShowStoryCheckBox.setAlignment(Pos.CENTER);
        
        // Устанавливаем начальное состояние из app
        if (app != null) {
            alwaysShowStoryCheckBox.setSelected(app.isAlwaysShowChapterStory());
        }
        
        // Добавляем tooltip для пояснения
        Tooltip tooltip = new Tooltip(localizationManager.get("debug.levels.always_show_story.tooltip"));
        tooltip.setFont(Font.font("Orbitron", 12));
        alwaysShowStoryCheckBox.setTooltip(tooltip);
        
        // Сохраняем listener для последующей очистки
        alwaysShowStoryListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что DebugLevelsView еще не очищен
            if (app == null) {
                return;
            }
            try {
                // Устанавливаем флаг в app
                app.setAlwaysShowChapterStory(newVal);
                // Воспроизводим звук переключения
                app.getAudioManager().playSFXByName("menu_hover");
            } catch (Exception e) {
                // Игнорируем ошибки, если DebugLevelsView уже очищен
            }
        };
        alwaysShowStoryCheckBox.selectedProperty().addListener(alwaysShowStoryListener);
        
        container.getChildren().add(alwaysShowStoryCheckBox);
        
        return container;
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button closeButton = createControlButton("debug.levels.button.close", GameConfig.NEON_CYAN, () -> {
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
        
        Button selectButton = createControlButton("debug.levels.button.select", GameConfig.NEON_GREEN, () -> {
            if (currentButtonIndex >= 0 && currentButtonIndex < levelButtons.size()) {
                Button selectedButton = levelButtons.get(currentButtonIndex);
                ButtonStyles styles = (ButtonStyles) selectedButton.getUserData();
                selectLevel(styles.levelNumber);
            }
        });
        
        buttonBox.getChildren().addAll(closeButton, selectButton);
        
        // Сохраняем ссылки на кнопки для навигации (без кнопки back, она отдельно)
        menuButtons = new Button[]{closeButton, selectButton};
        
        return buttonBox;
    }
    
    private Button createControlButton(String localizationKey, String color, Runnable action) {
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
            if (menuButtons != null && button != menuButtons[currentMenuButtonIndex]) {
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
            if (menuButtons != null && button != menuButtons[currentMenuButtonIndex]) {
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
    
    private void handleMenuAction(Runnable action) {
        long now = System.nanoTime();
        if (now - lastMenuActionTime < ACTION_COOLDOWN_NANOS) {
            return;
        }

        lastMenuActionTime = now;
        action.run();
    }
    
    private void setupKeyHandler() {
        this.setFocusTraversable(true);
        
        // Пытаемся получить фокус через Platform.runLater
        Platform.runLater(() -> {
            this.requestFocus();
        });
        
        this.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F3:
                case ESCAPE:
                    app.getAudioManager().playSFXByName("menu_back");
                    // Проверяем, есть ли callback - если есть, возвращаемся к debug меню, иначе в главное меню
                    if (onCloseCallback != null) {
                        closeDebugLevelsView();
                    } else {
                        returnToMainMenuWithoutReload();
                    }
                    event.consume();
                    break;
                case TAB:
                    // Переключение между навигацией по уровням и кнопкам меню
                    navigatingMenuButtons = !navigatingMenuButtons;
                    if (navigatingMenuButtons) {
                        app.getAudioManager().playSFXByName("menu_hover");
                        updateMenuButtonHighlight();
                    } else {
                        updateButtonHighlight();
                    }
                    event.consume();
                    break;
                case UP:
                    if (navigatingMenuButtons) {
                        moveMenuButtonUp();
                    } else {
                        moveUp();
                    }
                    event.consume();
                    break;
                case DOWN:
                    if (navigatingMenuButtons) {
                        moveMenuButtonDown();
                    } else {
                        moveDown();
                    }
                    event.consume();
                    break;
                case LEFT:
                    if (navigatingMenuButtons) {
                        moveMenuButtonUp();
                    } else {
                        moveLeft();
                    }
                    event.consume();
                    break;
                case RIGHT:
                    if (navigatingMenuButtons) {
                        moveMenuButtonDown();
                    } else {
                        moveRight();
                    }
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    if (navigatingMenuButtons) {
                        selectCurrentMenuButton();
                    } else {
                        if (currentButtonIndex >= 0 && currentButtonIndex < levelButtons.size()) {
                            Button selectedButton = levelButtons.get(currentButtonIndex);
                            ButtonStyles styles = (ButtonStyles) selectedButton.getUserData();
                            selectLevel(styles.levelNumber);
                        }
                    }
                    event.consume();
                    break;
                default:
                    // Игнорируем остальные клавиши
                    break;
            }
        });
    }
    
    private void moveUp() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentButtonIndex >= BUTTONS_PER_ROW) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex -= BUTTONS_PER_ROW;
            if (currentButtonIndex < 0) currentButtonIndex = 0;
            updateButtonHighlight();
        }
    }
    
    private void moveDown() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentButtonIndex + BUTTONS_PER_ROW < levelButtons.size()) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex += BUTTONS_PER_ROW;
            if (currentButtonIndex >= levelButtons.size()) {
                currentButtonIndex = levelButtons.size() - 1;
            }
            updateButtonHighlight();
        }
    }
    
    private void moveLeft() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    private void moveRight() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentButtonIndex < levelButtons.size() - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    private void updateButtonHighlight() {
        for (int i = 0; i < levelButtons.size(); i++) {
            Button button = levelButtons.get(i);
            ButtonStyles styles = (ButtonStyles) button.getUserData();
            
            if (i == currentButtonIndex) {
                button.setStyle(styles.selectedStyle);
            } else {
                button.setStyle(styles.normalStyle);
            }
        }
    }
    
    private void updateMenuButtonHighlight() {
        if (menuButtons == null) return;
        
        for (int i = 0; i < menuButtons.length; i++) {
            Button button = menuButtons[i];
            if (i == currentMenuButtonIndex) {
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
    
    private void moveMenuButtonUp() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentMenuButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentMenuButtonIndex--;
            updateMenuButtonHighlight();
        }
    }
    
    private void moveMenuButtonDown() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (currentMenuButtonIndex < menuButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentMenuButtonIndex++;
            updateMenuButtonHighlight();
        }
    }
    
    private void selectCurrentMenuButton() {
        if (app == null) {
            return; // Если app уже null, значит view уже очищен
        }
        if (menuButtons != null && currentMenuButtonIndex >= 0 && currentMenuButtonIndex < menuButtons.length) {
            app.getAudioManager().playSFXByName("menu_select");
            menuButtons[currentMenuButtonIndex].fire();
        }
    }
    
    private void selectLevel(int levelNumber) {
        // КРИТИЧНО: Сохраняем ссылку на app перед cleanup(), чтобы можно было использовать после закрытия view
        ArcadeBlocksApp appRef = app;
        if (appRef == null) {
            return; // Если app уже null, значит view уже очищен
        }
        
        appRef.getAudioManager().playSFXByName("menu_select");
        
        LevelConfig.LevelData levelData = LevelConfig.getLevel(levelNumber);
        if (levelData != null) {
        // System.out.println("DEBUG: Выбран уровень " + levelNumber + " - " + levelData.getName());
            
            // Закрываем экран выбора уровней без уведомления callback (это обнулит app в cleanup())
            // Это предотвращает повторное открытие debug меню при выборе уровня
            closeDebugLevelsView(false);
            
            // КРИТИЧНО: Запускаем уровень через Platform.runLater(), чтобы дать время на полную очистку
            // DebugLevelsView перед созданием новых UI элементов. Это предотвращает утечки памяти
            // при переходе из debug меню в уровень, так как гарантирует правильный порядок:
            // 1. cleanup() и removeUINode() выполняются синхронно
            // 2. JavaFX обрабатывает удаление ноды
            // 3. Только после этого запускается новый уровень
            final int levelToStart = levelNumber;
            Platform.runLater(() -> {
                // КРИТИЧНО: Принудительно очищаем все активные видео ресурсы перед запуском уровня
                // Это предотвращает утечки памяти от видео оверлеев и MediaPlayer'ов
                try {
                    java.lang.reflect.Method cleanupMethod = appRef.getClass().getDeclaredMethod("cleanupActiveVideoResources");
                    cleanupMethod.setAccessible(true);
                    cleanupMethod.invoke(appRef);
                } catch (Exception e) {
                    System.err.println("Warning: Could not cleanup video resources before debug level start: " + e.getMessage());
                }
                
                // КРИТИЧНО: Принудительно очищаем все UI ноды для предотвращения утечек памяти
                try {
                    java.lang.reflect.Method clearUIMethod = appRef.getClass().getDeclaredMethod("clearUINodesSafely");
                    clearUIMethod.setAccessible(true);
                    clearUIMethod.invoke(appRef);
                } catch (Exception e) {
                    System.err.println("Warning: Could not cleanup UI nodes before debug level start: " + e.getMessage());
                }
                
                // Используем сохраненную ссылку, так как app уже обнулен в cleanup()
                appRef.startDebugLevel(levelToStart);
            });
        } else {
            String errorMessage = localizationManager.format("debug.levels.error.not_found", levelNumber);
            System.err.println(errorMessage);
        }
    }
    
    private void closeDebugLevelsView() {
        closeDebugLevelsView(true);
    }
    
    private void closeDebugLevelsView(boolean notifyCallback) {
        // КРИТИЧНО: Сохраняем callback перед cleanup, так как он обнуляется
        Runnable callbackRef = onCloseCallback;
        
        // КРИТИЧНО: СНАЧАЛА удаляем из scene, ПОТОМ cleanup
        // Это предотвращает race condition, когда нода еще в scene, но уже "мертвая"
        FXGL.getGameScene().removeUINode(this);
        
        // Теперь безопасно вызываем cleanup, так как нода уже не в scene
        cleanup();
        
        // Уведомляем родительский компонент о закрытии только если требуется
        if (notifyCallback && callbackRef != null) {
            callbackRef.run();
        }
    }
    
    private void returnToMainMenuWithoutReload() {
        // КРИТИЧНО: НЕ создаем новый MainMenuView!
        // Просто удаляем DebugLevelsView и DebugMenuView, возвращаемся к MainMenuView
        
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
     * Класс для хранения стилей кнопки
     */
    private static class ButtonStyles {
        final String normalStyle;
        final String hoverStyle;
        final String selectedStyle;
        final int levelNumber;
        
        ButtonStyles(String normalStyle, String hoverStyle, String selectedStyle, int levelNumber) {
            this.normalStyle = normalStyle;
            this.hoverStyle = hoverStyle;
            this.selectedStyle = selectedStyle;
            this.levelNumber = levelNumber;
        }
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
            
            if (onFinished != null && !isCleanedUp) {
                onFinished.run();
            }
        });
        
        closeFadeTransition.play();
        closeScaleTransition.play();
    }
}
