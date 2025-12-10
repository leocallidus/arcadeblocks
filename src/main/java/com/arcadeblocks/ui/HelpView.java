package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.ui.util.UINodeCleanup;
import com.arcadeblocks.gameplay.BonusType;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Меню справки
 */
public class HelpView extends VBox {
    
    private final ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private ScrollPane scrollPane;
    private Button backButton;
    private boolean fromPause;
    // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки
    private FadeTransition appearFadeTransition;
    private TranslateTransition appearSlideTransition;
    private ScaleTransition buttonScaleTransition;
    // КРИТИЧНО: Сохраняем ссылки на иконки бонусов для освобождения памяти
    private java.util.List<javafx.scene.image.ImageView> bonusIconViews = new java.util.ArrayList<>();
    // КРИТИЧНО: Сохраняем ссылку на event filter для его удаления при cleanup
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyEventFilter;
    
    // Анимации закрытия
    private FadeTransition closeFadeTransition;
    private ScaleTransition closeScaleTransition;
    private volatile boolean isClosing = false;
    
    public HelpView(ArcadeBlocksApp app) {
        this.app = app;
        this.fromPause = false;
        initializeUI();
    }
    
    public HelpView(ArcadeBlocksApp app, boolean fromPause) {
        this.app = app;
        this.fromPause = fromPause;
        initializeUI();
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
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
        
        // КРИТИЧНО: Останавливаем анимацию кнопки
        if (buttonScaleTransition != null) {
            try {
                buttonScaleTransition.stop();
            } catch (Exception ignored) {}
            buttonScaleTransition = null;
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
        
        // КРИТИЧНО: Сначала очищаем все обработчики кнопки перед удалением из children
        if (backButton != null) {
            backButton.setOnAction(null);
            backButton.setOnMouseEntered(null);
            backButton.setOnMouseExited(null);
            // КРИТИЧНО: Отвязываем textProperty() у Button
            backButton.textProperty().unbind();
            backButton = null;
        }
        
        // КРИТИЧНО: Обнуляем ссылку на scrollPane для предотвращения утечек
        if (scrollPane != null) {
            scrollPane.setContent(null);
            scrollPane = null;
        }
        
        // КРИТИЧНО: Освобождаем изображения иконок бонусов
        synchronized (bonusIconViews) {
            for (javafx.scene.image.ImageView iconView : bonusIconViews) {
                if (iconView != null) {
                    UINodeCleanup.releaseImages(iconView);
                    iconView.setImage(null);
                }
            }
            bonusIconViews.clear();
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Освобождаем изображения через UINodeCleanup
        UINodeCleanup.releaseImages(this);
        
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
        
        setBackground(null);
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
        setSpacing(20);
        // Применяем CSS стили
        getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        // КРИТИЧНО: НЕ создаем свой собственный фон!
        // HelpView должен быть прозрачным и показываться поверх существующего MainMenuView
        // Это предотвращает создание дубликатов фоновых изображений
        setBackground(null);
        
        if (fromPause) {
            // В паузе затемняем фон уровня
            setStyle("-fx-background-color: rgba(15, 15, 28, 0.72);");
        } else {
            // В главном меню используем полупрозрачный темный фон
            // MainMenuView остается видимым под HelpView
            setStyle("-fx-background-color: rgba(15, 15, 28, 0.85);");
        }
        
        setPadding(new Insets(20));
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "help.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        
        // Создание содержимого справки
        VBox contentBox = createHelpContent();
        
        // Создание ScrollPane
        scrollPane = new ScrollPane(contentBox);
        scrollPane.setPrefSize(GameConfig.GAME_WIDTH - 100, GameConfig.GAME_HEIGHT - 200);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Кнопка "Назад"
        backButton = createBackButton();

        // Полупрозрачный скругленный контейнер поверх фонового изображения
        VBox contentContainer = new VBox(20);
        contentContainer.setAlignment(Pos.CENTER);
        contentContainer.setPadding(new Insets(24));
        contentContainer.setMaxWidth(1400); // Увеличили с 1100 до 1400 для русского текста
        contentContainer.setStyle(
            "-fx-background-color: rgba(15, 15, 28, 0.39);" +
            "-fx-background-radius: 16px;" +
            "-fx-border-color: " + GameConfig.NEON_PURPLE + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 18, 0.35, 0, 0);"
        );

        contentContainer.getChildren().addAll(titleLabel, scrollPane, backButton);

        getChildren().add(contentContainer);
        
        // Плавная анимация появления (только контента, фон остаётся видимым)
        playAppearAnimation(contentContainer);
        
        // Настройка обработки клавиш
        setupKeyHandler();

        ResponsiveLayoutHelper.bindToStage(this);
        setUserData("fullScreenOverlay");
    }

    private void playAppearAnimation(javafx.scene.Node node) {
        // КРИТИЧНО: Останавливаем старые анимации, если они еще активны
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
        
        node.setOpacity(0);
        appearFadeTransition = new FadeTransition(Duration.millis(400), node);
        appearFadeTransition.setFromValue(0.0);
        appearFadeTransition.setToValue(1.0);
        appearSlideTransition = new TranslateTransition(Duration.millis(400), node);
        appearSlideTransition.setFromY(18);
        appearSlideTransition.setToY(0);
        
        // КРИТИЧНО: Очищаем ссылки на анимации после их завершения
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
        appearSlideTransition.setOnFinished(e -> {
            // Ничего не делаем, основная логика в appearFadeTransition.onFinished
        });
        
        appearFadeTransition.play();
        appearSlideTransition.play();
    }
    
    private VBox createHelpContent() {
        VBox contentBox = new VBox(30);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setPadding(new Insets(30));
        contentBox.setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
        
        // Создаем двухколоночный layout
        javafx.scene.layout.HBox twoColumnLayout = new javafx.scene.layout.HBox(30);
        twoColumnLayout.setAlignment(Pos.TOP_LEFT);
        
        // Левая колонка: общая информация
        VBox leftColumn = new VBox(30);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        leftColumn.setPrefWidth(640); // Увеличили с 500 до 640 для русского текста
        
        // Управление
        VBox controlsSection = createControlsSection();
        
        // Типы кирпичей
        VBox bricksSection = createBricksSection();
        
        // Цель игры
        VBox objectiveSection = createObjectiveSection();
        
        leftColumn.getChildren().addAll(controlsSection, bricksSection, objectiveSection);
        
        // Правая колонка: бонусы
        VBox rightColumn = createBonusShowcase();
        rightColumn.setPrefWidth(640); // Увеличили с 500 до 640 для русского текста
        
        twoColumnLayout.getChildren().addAll(leftColumn, rightColumn);
        
        contentBox.getChildren().add(twoColumnLayout);
        
        return contentBox;
    }
    
    
    private VBox createControlsSection() {
        VBox section = new VBox(15);
        
        Label sectionTitle = createSectionTitle("help.controls.title", Color.web(GameConfig.NEON_PINK));
        
        Label controlsLabel = createTextLabel("help.controls.content");
        
        section.getChildren().addAll(sectionTitle, controlsLabel);
        return section;
    }
    
    private VBox createBricksSection() {
        VBox section = new VBox(15);
        
        Label sectionTitle = createSectionTitle("help.bricks.title", Color.web(GameConfig.NEON_PURPLE));
        
        Label bricksLabel = createTextLabel("help.bricks.content");
        
        section.getChildren().addAll(sectionTitle, bricksLabel);
        return section;
    }
    
    private VBox createBonusShowcase() {
        VBox showcase = new VBox(20);
        showcase.setAlignment(Pos.TOP_LEFT);
        
        // Описание панели бонусов
        Label descriptionLabel = createTextLabel("help.powerups.description");
        descriptionLabel.setMaxWidth(640); // Увеличили с 500 до 640 для русского текста
        showcase.getChildren().add(descriptionLabel);
        
        // Положительные бонусы
        Label positiveTitle = createSectionTitle("help.powerups.positive", Color.web(GameConfig.NEON_GREEN));
        VBox positiveBonuses = createBonusGrid(BonusType.getEnabledPositiveBonuses());
        
        // Негативные бонусы
        Label negativeTitle = createSectionTitle("help.powerups.negative", Color.web("#FF4444"));
        VBox negativeBonuses = createBonusGrid(BonusType.getEnabledNegativeBonuses());
        
        showcase.getChildren().addAll(positiveTitle, positiveBonuses, negativeTitle, negativeBonuses);
        
        return showcase;
    }
    
    private VBox createBonusGrid(java.util.List<BonusType> bonuses) {
        VBox grid = new VBox(10);
        grid.setAlignment(Pos.TOP_LEFT);
        
        for (BonusType bonus : bonuses) {
            javafx.scene.layout.HBox bonusRow = createBonusRow(bonus);
            grid.getChildren().add(bonusRow);
        }
        
        return grid;
    }
    
    private javafx.scene.layout.HBox createBonusRow(BonusType bonus) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(15); // Увеличили с 10 до 15 для больших иконок
        row.setAlignment(Pos.CENTER_LEFT);
        
        // Иконка бонуса
        javafx.scene.image.ImageView iconView = createBonusIconNode(bonus);
        
        // КРИТИЧНО: Синхронизируем доступ к bonusIconViews для предотвращения race condition
        synchronized (bonusIconViews) {
            bonusIconViews.add(iconView);
        }
        
        // Название бонуса
        Label nameLabel = new Label();
        String localizationKey = getBonusLocalizationKey(bonus);
        localizationManager.bind(nameLabel, localizationKey);
        nameLabel.setFont(Font.font("Orbitron", 16)); // Увеличили с 14 до 16 для лучшей читаемости
        nameLabel.setTextFill(Color.web("#E0E0E0"));
        
        row.getChildren().addAll(iconView, nameLabel);
        
        return row;
    }
    
    private javafx.scene.image.ImageView createBonusIconNode(BonusType bonus) {
        javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView();
        try {
            javafx.scene.image.Image iconImage = ImageCache.get(bonus.getTextureName());
            iconView.setImage(iconImage);
            iconView.setFitWidth(64); // Увеличили в 2 раза: с 32 до 64
            iconView.setFitHeight(64); // Увеличили в 2 раза: с 32 до 64
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true);
        } catch (Exception e) {
            // Если иконка не найдена, создаем пустой ImageView
            iconView.setFitWidth(64); // Увеличили в 2 раза: с 32 до 64
            iconView.setFitHeight(64); // Увеличили в 2 раза: с 32 до 64
        }
        return iconView;
    }
    
    private String getBonusLocalizationKey(BonusType bonusType) {
        switch (bonusType) {
            case BONUS_SCORE:
                return "debug.bonuses.bonus.extra_points";
            case BONUS_SCORE_200:
                return "debug.bonuses.bonus.extra_points_200";
            case BONUS_SCORE_500:
                return "debug.bonuses.bonus.extra_points_500";
            case EXTRA_LIFE:
                return "debug.bonuses.bonus.extra_life";
            case INCREASE_PADDLE:
                return "debug.bonuses.bonus.bigger_paddle";
            case DECREASE_PADDLE:
                return "debug.bonuses.bonus.smaller_paddle";
            case STICKY_PADDLE:
                return "debug.bonuses.bonus.sticky_paddle";
            case SLOW_BALLS:
                return "debug.bonuses.bonus.slow_balls";
            case FAST_BALLS:
                return "debug.bonuses.bonus.fast_balls";
            case ENERGY_BALLS:
                return "debug.bonuses.bonus.energy_balls";
            case EXPLOSION_BALLS:
                return "debug.bonuses.bonus.explosive_balls";
            case WEAK_BALLS:
                return "debug.bonuses.bonus.weak_balls";
            case BONUS_BALL:
                return "debug.bonuses.bonus.extra_ball";
            case BONUS_WALL:
                return "debug.bonuses.bonus.shield_barrier";
            case BONUS_MAGNET:
                return "debug.bonuses.bonus.bonus_magnet";
            case PLASMA_WEAPON:
                return "debug.bonuses.bonus.plasma_weapon";
            case LEVEL_PASS:
                return "debug.bonuses.bonus.level_pass";
            case SCORE_RAIN:
                return "debug.bonuses.bonus.score_rain";
            case ADD_FIVE_SECONDS:
                return "debug.bonuses.bonus.add_five_seconds";
            case CALL_BALL:
                return "debug.bonuses.bonus.call_ball";
            case TRICKSTER:
                return "debug.bonuses.bonus.trickster";
            case RANDOM_BONUS:
                return "debug.bonuses.bonus.random_bonus";
            case CHAOTIC_BALLS:
                return "debug.bonuses.bonus.chaotic_balls";
            case FROZEN_PADDLE:
                return "debug.bonuses.bonus.frozen_paddle";
            case PENALTIES_MAGNET:
                return "debug.bonuses.bonus.penalty_magnet";
            case INVISIBLE_PADDLE:
                return "debug.bonuses.bonus.ghost_paddle";
            case DARKNESS:
                return "debug.bonuses.bonus.darkness";
            case RESET:
                return "debug.bonuses.bonus.reset_bonuses";
            case BAD_LUCK:
                return "debug.bonuses.bonus.bad_luck";
            default:
                return "debug.bonuses.bonus.random_bonus";
        }
    }
    
    private VBox createObjectiveSection() {
        VBox section = new VBox(15);
        
        Label sectionTitle = createSectionTitle("help.objective.title", Color.web(GameConfig.NEON_YELLOW));
        
        Label objectiveLabel = createTextLabel("help.objective.content");
        
        section.getChildren().addAll(sectionTitle, objectiveLabel);
        return section;
    }
    
    private Label createSectionTitle(String translationKey, Color color) {
        Label label = new Label();
        localizationManager.bind(label, translationKey);
        label.setFont(Font.font("Orbitron", FontWeight.BOLD, 24));
        label.setTextFill(color);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }
    
    private Label createTextLabel(String translationKey) {
        Label label = new Label();
        localizationManager.bind(label, translationKey);
        label.setFont(Font.font("Orbitron", 14));
        label.setTextFill(Color.web("#E0E0E0"));
        label.setWrapText(true);
        label.setMaxWidth(GameConfig.GAME_WIDTH - 150);
        label.setAlignment(Pos.TOP_LEFT);
        return label;
    }
    
    
    private Button createBackButton() {
        Button backButton = new Button();
        localizationManager.bind(backButton, "help.back");
        backButton.setPrefSize(150, 40);
        backButton.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        backButton.setTextFill(Color.WHITE);
        
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
        
        backButton.setStyle(buttonStyle);
        
        // Hover эффекты для мыши (совместимо с клавиатурной навигацией)
        backButton.setOnMouseEntered(e -> {
            app.getAudioManager().playSFXByName("menu_hover");
            
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
            backButton.setUserData(backButton.getStyle());
            backButton.setStyle(hoverStyle);
            
            // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
            if (buttonScaleTransition != null) {
                try {
                    buttonScaleTransition.stop();
                } catch (Exception ignored) {}
            }
            
            // Анимация увеличения
            buttonScaleTransition = new ScaleTransition(Duration.millis(150), backButton);
            buttonScaleTransition.setToX(1.05);
            buttonScaleTransition.setToY(1.05);
            buttonScaleTransition.setOnFinished(e2 -> {
                if (buttonScaleTransition != null) {
                    buttonScaleTransition.stop();
                    buttonScaleTransition = null;
                }
            });
            buttonScaleTransition.play();
        });
        
        backButton.setOnMouseExited(e -> {
            // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
            if (buttonScaleTransition != null) {
                try {
                    buttonScaleTransition.stop();
                } catch (Exception ignored) {}
                buttonScaleTransition = null;
            }
            
            // Восстанавливаем исходный стиль
            if (backButton.getUserData() != null) {
                backButton.setStyle((String) backButton.getUserData());
            }
            
            // Анимация уменьшения
            buttonScaleTransition = new ScaleTransition(Duration.millis(150), backButton);
            buttonScaleTransition.setToX(1.0);
            buttonScaleTransition.setToY(1.0);
            buttonScaleTransition.setOnFinished(e2 -> {
                if (buttonScaleTransition != null) {
                    buttonScaleTransition.stop();
                    buttonScaleTransition = null;
                }
            });
            buttonScaleTransition.play();
        });
        
        backButton.setOnAction(e -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_back");
            if (fromPause) {
                returnToPause();
            } else {
                returnToMainMenu();
            }
        });
        
        return backButton;
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
            // MainMenuView уже существует под HelpView (overlay pattern)
            // Просто удаляем HelpView, чтобы показать существующий MainMenuView
            
            cleanup();
            FXGL.getGameScene().removeUINode(this);
            
            // КРИТИЧНО: Восстанавливаем фокус на MainMenuView для работы навигации по клавиатуре
            for (javafx.scene.Node node : FXGL.getGameScene().getUINodes()) {
                if (node instanceof MainMenuView) {
                    ((MainMenuView) node).restoreFocus();
                    break;
                }
            }
        });
    }
    
    private void returnToPause() {
        // КРИТИЧНО: Запускаем анимацию закрытия перед возвратом в паузу
        playCloseAnimation(() -> {
            // КРИТИЧНО: НЕ вызываем cleanup() и removeUINode() вручную!
            // Пусть app.returnToPauseFromHelp() сам вызовет правильную очистку
            
            // Используем централизованный метод для возврата в паузу
            app.returnToPauseFromHelp();
        });
    }
    
    /**
     * Настройка обработчика клавиш
     */
    private void setupKeyHandler() {
        this.setFocusTraversable(true);
        this.requestFocus();
        
        keyEventFilter = event -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                event.consume();
                return;
            }
            
            switch (event.getCode()) {
                case ESCAPE:
                case ENTER:
                case SPACE:
                    app.getAudioManager().playSFXByName("menu_back");
                    if (fromPause) {
                        returnToPause();
                    } else {
                        returnToMainMenu();
                    }
                    event.consume();
                    break;
                case UP:
                    scrollPane.setVvalue(Math.max(0, scrollPane.getVvalue() - 0.1));
                    event.consume();
                    break;
                case DOWN:
                    scrollPane.setVvalue(Math.min(1, scrollPane.getVvalue() + 0.1));
                    event.consume();
                    break;
                case PAGE_UP:
                    scrollPane.setVvalue(Math.max(0, scrollPane.getVvalue() - 0.3));
                    event.consume();
                    break;
                case PAGE_DOWN:
                    scrollPane.setVvalue(Math.min(1, scrollPane.getVvalue() + 0.3));
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
}
