package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.AuthorsConfig;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.utils.ImageCache;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Меню титров
 */
public class CreditsView extends StackPane {
    
    private final ArcadeBlocksApp app;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    private Text creditsText;
    private TranslateTransition scrollTransition;
    private javafx.animation.Timeline scrollTimeline; // Timeline для прокрутки
    private boolean isScrolling = false;
    private boolean isPaused = false; // Флаг паузы
    private final boolean fromSaveSystem; // Флаг: запущено из системы сохранений (после прохождения уровня 116)
    
    // Синхронизация с музыкой
    private String creditsMusicFile = "music/credits.mp3";
    private boolean musicFinished = false;
    private double scrollSpeed = 0.3; // Скорость прокрутки (пикселей в секунду) - замедлена для комфортного чтения
    private double currentScrollPosition = 0.0;
    
    // Навигация по кнопкам
    private Button[] controlButtons;
    private int currentButtonIndex = 0;
    private VBox contentContainer;
    private VBox creditsContainer;
    // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки
    private FadeTransition appearFadeTransition;
    private TranslateTransition appearSlideTransition;
    private java.util.Map<Button, ScaleTransition> buttonScaleTransitions = new java.util.HashMap<>();
    
    public CreditsView(ArcadeBlocksApp app) {
        this(app, false); // По умолчанию - не из системы сохранений
    }
    
    public CreditsView(ArcadeBlocksApp app, boolean fromSaveSystem) {
        this.app = app;
        this.fromSaveSystem = fromSaveSystem;
        initializeUI();
        
        // Запускаем музыку титров
        if (app.getAudioManager() != null) {
            app.getAudioManager().stopMusic();
            app.getAudioManager().playMusic(creditsMusicFile, true);
        }
        
        startCredits();
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    public void cleanup() {
        // Останавливаем музыку титров только если она еще играет
        if (app != null && app.getAudioManager() != null) {
            // Проверяем, играет ли музыка титров
            if (app.getAudioManager().isMusicPlaying(creditsMusicFile)) {
                app.getAudioManager().stopMusic();
            }
        }
        
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
        
        // Останавливаем анимации
        if (scrollTransition != null) {
            scrollTransition.stop();
            scrollTransition = null;
        }
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
        this.setOnMouseClicked(null);
        
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
        
        // КРИТИЧНО: Удаляем listener на localeProperty
        if (creditsText != null) {
            @SuppressWarnings("unchecked")
            javafx.beans.value.ChangeListener<java.util.Locale> localeListener = 
                (javafx.beans.value.ChangeListener<java.util.Locale>) creditsText.getProperties().get("localeListener");
            if (localeListener != null) {
                localizationManager.localeProperty().removeListener(localeListener);
                creditsText.getProperties().remove("localeListener");
            }
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Освобождаем изображения перед удалением children
        com.arcadeblocks.ui.util.UINodeCleanup.releaseImages(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
        
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
        // Фон титров — изображение credits_background.png
        try {
            // Загружаем из каталога assets/textures
            javafx.scene.image.Image bgImg = ImageCache.get("credits_background.png");
            if (bgImg != null) {
                javafx.scene.layout.BackgroundSize bs = new javafx.scene.layout.BackgroundSize(
                    100, 100, true, true, false, true
                );
                javafx.scene.layout.BackgroundImage bi = new javafx.scene.layout.BackgroundImage(
                    bgImg,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundPosition.CENTER,
                    bs
                );
                setBackground(new javafx.scene.layout.Background(bi));
            } else {
                setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
            }
        } catch (Exception ex) {
            setStyle("-fx-background-color: " + GameConfig.DARK_BACKGROUND + ";");
        }
        setPadding(new Insets(10));
        
        // Заголовок
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "credits.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxHeight(50);
        
        // Контейнер для титров
        // Расчет: padding (20px) + title (50px) + spacing (10px) + buttons (50px) + spacing (10px) = 140px
        // Контейнер: 768 - 140 = 628px по высоте
        // По ширине: 1024 - 60 = 964px
        creditsContainer = new VBox();
        creditsContainer.setAlignment(Pos.TOP_CENTER);
        
        double containerHeight = Math.min(620, GameConfig.GAME_WORLD_HEIGHT - 160);
        double containerWidth = Math.min(960, GameConfig.GAME_WORLD_WIDTH - 64);
        
        creditsContainer.setPrefSize(containerWidth, containerHeight);
        creditsContainer.setMinSize(containerWidth, containerHeight);
        creditsContainer.setMaxSize(containerWidth, containerHeight);
        creditsContainer.setPadding(new Insets(5));
        
        // ВАЖНО: включаем обрезку только внутри контейнера
        creditsContainer.setClip(new javafx.scene.shape.Rectangle(
            0, 0,
            containerWidth, 
            containerHeight
        ));
        
        // Текст титров (используем Text вместо Label для лучшей работы с большим объемом текста)
        creditsText = new Text();
        updateCreditsText();
        creditsText.setFont(Font.font("Orbitron", 18));
        creditsText.setFill(Color.WHITE);
        creditsText.setTextAlignment(TextAlignment.CENTER);
        
        // Устанавливаем ширину для переноса текста
        creditsText.setWrappingWidth(containerWidth - 10);
        
        // КРИТИЧНО: Слушаем изменения языка и обновляем текст титров
        // Сохраняем ссылку на listener для удаления в cleanup
        javafx.beans.value.ChangeListener<java.util.Locale> localeListener = (obs, oldLocale, newLocale) -> {
            updateCreditsText();
        };
        localizationManager.localeProperty().addListener(localeListener);
        // КРИТИЧНО: Сохраняем listener в properties для удаления в cleanup
        creditsText.getProperties().put("localeListener", localeListener);
        
        creditsContainer.getChildren().add(creditsText);
        
        // Кнопки управления
        HBox buttonBox = createButtonBox();
        buttonBox.setMaxHeight(40);
        
        // Полупрозрачный скругленный контейнер поверх фонового изображения
        contentContainer = new VBox(20);
        contentContainer.setAlignment(Pos.CENTER);
        contentContainer.setPadding(new Insets(24));
        contentContainer.setMaxWidth(900);
        contentContainer.setStyle(
            "-fx-background-color: rgba(15, 15, 28, 0.39);" +
            "-fx-background-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 18, 0.35, 0, 0);"
        );

        contentContainer.getChildren().addAll(titleLabel, creditsContainer, buttonBox);

        getChildren().add(contentContainer);
        // app.centerUINode(this);

        // Плавная анимация появления контейнера титров
        playAppearAnimation(contentContainer);
        
        // Настройка обработки клавиш
        setupKeyHandlers();

        ResponsiveLayoutHelper.bindToStage(this, this::adjustLayoutForResolution);
        setUserData("fullScreenOverlay");
    }
    
    private void updateCreditsText() {
        if (creditsText != null) {
            String credits = AuthorsConfig.getCredits();
            creditsText.setText(credits);
            // Перезапускаем прокрутку при смене языка, если она уже запущена
            if (isScrolling && !isPaused) {
                double currentY = creditsText.getTranslateY();
                // Сохраняем текущую позицию прокрутки при смене языка
                // Можно просто продолжить с текущей позиции
            }
        }
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
            appearFadeTransition = null;
        });
        appearSlideTransition.setOnFinished(e -> {
            appearSlideTransition = null;
        });
        
        appearFadeTransition.play();
        appearSlideTransition.play();
    }
    
    private void startCredits() {
        if (isScrolling) return;
        
        isScrolling = true;
        musicFinished = false;
        
        // Высота контейнера (620px как в initializeUI)
        double containerHeight = 620;
        
        // Стартовая позиция - текст начинается ЗА НИЖНИМ краем контейнера (не виден)
        double startY = containerHeight;
        
        // Конечная позиция - текст полностью прокручивается ВВЕРХ (уходит за верхний край)
        // Нужно учесть высоту всего текста (примерно 1800px для всего контента)
        // double endY = -1800; // Весь текст прокрутился вверх
        
        // Устанавливаем начальную позицию
        creditsText.setTranslateY(startY);
        
        // Инициализируем переменные для синхронизации
        currentScrollPosition = startY;
        
        // Воспроизведение музыки титров
        app.getAudioManager().playMusic(creditsMusicFile, false); // Не зацикливаем музыку
        
        // Убеждаемся, что фокус установлен правильно
        javafx.application.Platform.runLater(() -> {
            this.requestFocus();
        });
        
        // Запускаем непрерывную прокрутку, синхронизированную с музыкой
        startMusicSynchronizedScrolling();
    }
    
    /**
     * Запуск синхронизированной с музыкой прокрутки
     */
    private void startMusicSynchronizedScrolling() {
        // Создаем Timeline для непрерывной прокрутки
        scrollTimeline = new javafx.animation.Timeline();
        scrollTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        
        // Обновляем позицию каждые 16ms (60 FPS)
        scrollTimeline.getKeyFrames().add(new javafx.animation.KeyFrame(
            Duration.millis(16), 
            e -> updateScrollingPosition()
        ));
        
        scrollTimeline.play();
        isPaused = false;
    }
    
    /**
     * Обновление позиции прокрутки в зависимости от состояния музыки
     */
    private void updateScrollingPosition() {
        if (!isScrolling || isPaused) return; // Не обновляем позицию если на паузе
        
        // Проверяем, играет ли еще музыка
        boolean musicStillPlaying = app.getAudioManager().isMusicPlaying(creditsMusicFile);
        
        if (!musicStillPlaying && !musicFinished) {
            // Музыка закончилась
            musicFinished = true;
            finishCredits();
            return;
        }
        
        if (musicStillPlaying) {
            // Продолжаем прокрутку
            double endY = -2500; // Позиция, когда весь текст ушел за верхний край (увеличено для полного исчезновения)
            
            // Обновляем позицию с учетом скорости прокрутки
            currentScrollPosition -= scrollSpeed;
            creditsText.setTranslateY(currentScrollPosition);
            
            // Продолжаем прокрутку даже после окончания текста, чтобы обеспечить полное исчезновение
            // и дать время для прочтения последних строк
            if (currentScrollPosition <= endY) {
                // Текст полностью прокрутился, но продолжаем движение для полного исчезновения
                creditsText.setTranslateY(currentScrollPosition);
            }
        }
    }
    
    /**
     * Завершение титров
     */
    private void finishCredits() {
        isScrolling = false;
        musicFinished = true;
        
        // После окончания возвращаемся в меню
        javafx.application.Platform.runLater(() -> {
            // Очищаем сохраненную музыку при возврате из титров
            app.getAudioManager().clearSavedMusic();
            // returnToMainMenuFromSettings() сам вызовет cleanup через removeUINodeSafely
            app.returnToMainMenuFromSettings();
        });
    }
    
    private void stopCredits() {
        if (scrollTimeline != null) {
            scrollTimeline.stop();
            isScrolling = false;
        }
        if (scrollTransition != null) {
            scrollTransition.stop();
        }
    }
    
    private void pauseCredits() {
        if (scrollTimeline != null) {
            scrollTimeline.pause();
            isPaused = true;
        }
    }
    
    private void resumeCredits() {
        if (scrollTimeline != null) {
            scrollTimeline.play();
            isPaused = false;
        }
    }
    
    /**
     * Воспроизведение видео после титров и возврат в главное меню
     * (только если титры были запущены из системы сохранений после прохождения уровня 116)
     */
    private void playAfterCreditsVideoAndReturnToMenu() {
        cleanup();
        com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(this);
        
        // Устанавливаем флаг завершения игры для текущего слота
        if (app.getSaveManager() != null) {
            app.getSaveManager().setGameCompletedForActiveSlot();
        }
        
        // Воспроизводим видео after_credits_video.mp4 через метод приложения
        app.playAfterCreditsVideo();
    }
    
    /**
     * Воспроизведение видео с заданной длительностью
     * @param videoFileName имя файла видео (относительно assets/textures/)
     * @param durationSeconds длительность видео в секундах
     * @param onFinished callback после завершения видео
     */
    private void playVideoWithDuration(String videoFileName, double durationSeconds, Runnable onFinished) {
        try {
            // Получаем фабрику видео из приложения
            com.arcadeblocks.video.VideoBackendFactory videoFactory = app.getVideoBackendFactory();
            if (videoFactory == null) {
                System.err.println("VideoBackendFactory не доступна, пропускаем видео");
                if (onFinished != null) {
                    onFinished.run();
                }
                return;
            }
            
            // Создаем backend для воспроизведения видео
            com.arcadeblocks.video.VideoPlayerBackend videoBackend = videoFactory.createBackend();
            
            // Получаем размеры экрана
            double width = com.arcadeblocks.config.GameConfig.GAME_WORLD_WIDTH;
            double height = com.arcadeblocks.config.GameConfig.GAME_WORLD_HEIGHT;
            
            // Подготавливаем видео
            javafx.scene.Node videoNode = videoBackend.prepareVideo(videoFileName, width, height);
            
            // Создаем контейнер для видео
            javafx.scene.layout.StackPane videoContainer = new javafx.scene.layout.StackPane();
            videoContainer.setStyle("-fx-background-color: black;");
            videoContainer.setPrefSize(width, height);
            videoContainer.setMinSize(width, height);
            videoContainer.setMaxSize(width, height);
            videoContainer.getChildren().add(videoNode);
            
            // Добавляем видео на экран
            com.almasb.fxgl.dsl.FXGL.getGameScene().addUINode(videoContainer);
            
            // Устанавливаем callback на завершение видео
            videoBackend.setOnFinished(() -> {
                javafx.application.Platform.runLater(() -> {
                    // Останавливаем и очищаем видео
                    videoBackend.stop();
                    videoBackend.cleanup();
                    
                    // Удаляем видео с экрана
                    com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(videoContainer);
                    
                    // Вызываем callback
                    if (onFinished != null) {
                        onFinished.run();
                    }
                });
            });
            
            // Устанавливаем callback на ошибку
            videoBackend.setOnError(() -> {
                System.err.println("Ошибка воспроизведения видео: " + videoFileName);
                javafx.application.Platform.runLater(() -> {
                    videoBackend.cleanup();
                    com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(videoContainer);
                    if (onFinished != null) {
                        onFinished.run();
                    }
                });
            });
            
            // Запускаем воспроизведение
            videoBackend.play();
            
            // Устанавливаем таймер на случай, если видео не завершится автоматически
            javafx.animation.PauseTransition pauseTransition = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(durationSeconds + 1.0) // +1 секунда запас
            );
            pauseTransition.setOnFinished(e -> {
                if (videoBackend.isPlaying()) {
                    // System.out.println("Принудительная остановка видео по таймеру");
                    videoBackend.stop();
                    videoBackend.cleanup();
                    com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(videoContainer);
                    if (onFinished != null) {
                        onFinished.run();
                    }
                }
            });
            pauseTransition.play();
            
        } catch (Exception e) {
            System.err.println("Ошибка при воспроизведении видео: " + e.getMessage());
            e.printStackTrace();
            // В случае ошибки просто вызываем callback
            if (onFinished != null) {
                onFinished.run();
            }
        }
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(30);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Используем массив чтобы можно было ссылаться на кнопку внутри лямбды
        final Button[] pauseButtonRef = new Button[1];
        pauseButtonRef[0] = createControlButton("credits.button.pause", GameConfig.NEON_YELLOW, () -> {
            if (isScrolling && scrollTimeline != null) {
                if (!isPaused) {
                    pauseCredits();
                    applyButtonTranslation(pauseButtonRef[0], "credits.button.resume");
                } else {
                    resumeCredits();
                    applyButtonTranslation(pauseButtonRef[0], "credits.button.pause");
                }
                app.getAudioManager().playSFXByName("menu_select");
            }
        });
        Button pauseButton = pauseButtonRef[0];
        
        Button backButton = createControlButton("credits.button.back", GameConfig.NEON_CYAN, () -> {
            app.getAudioManager().playSFXByName("menu_back");
            stopCredits();
            
            if (fromSaveSystem) {
                // Если титры запущены из системы сохранений (после прохождения уровня 116)
                // Воспроизводим специальное видео перед возвратом в главное меню
                playAfterCreditsVideoAndReturnToMenu();
            } else {
                // Обычный возврат в главное меню
                // ВАЖНО: Сначала запускаем музыку главного меню, потом cleanup
                // Это предотвращает остановку музыки в cleanup после её запуска
                app.startMainMenuMusic();
                
                cleanup();
                
                // Возвращаемся в главное меню
                app.returnToMainMenuFromSettings();
            }
        });
        
        buttonBox.getChildren().addAll(pauseButton, backButton);
        
        // Сохраняем ссылки на кнопки для навигации
        controlButtons = new Button[]{pauseButton, backButton};
        
        // Устанавливаем визуальное выделение первой кнопки
        updateButtonHighlight();
        
        return buttonBox;
    }
    
    private Button createControlButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        applyButtonTranslation(button, translationKey);
        button.setPrefSize(150, 40); // Увеличена ширина для надписи "ПРОДОЛЖИТЬ"
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
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
            scaleUp.setOnFinished(e2 -> {
                buttonScaleTransitions.remove(button);
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
            scaleDown.setOnFinished(e2 -> {
                buttonScaleTransitions.remove(button);
            });
            buttonScaleTransitions.put(button, scaleDown);
            scaleDown.play();
        });
        button.setOnAction(e -> action.run());
        
        return button;
    }

    private void applyButtonTranslation(Button button, String translationKey) {
        button.textProperty().unbind();
        localizationManager.bind(button, translationKey);
        button.getProperties().put("i18n.key", translationKey);
    }
    
    private void setupKeyHandlers() {
        // Делаем VBox фокусируемым
        this.setFocusTraversable(true);

        // Устанавливаем фокус с задержкой, чтобы убедиться, что UI полностью инициализирован
        javafx.application.Platform.runLater(() -> {
            this.requestFocus();
        });
        
        // Обработчик нажатия клавиш
        this.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT:
                    moveLeft();
                    event.consume();
                    break;
                case RIGHT:
                    moveRight();
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    selectCurrentButton();
                    event.consume();
                    break;
                case ESCAPE:
                    // Выход в главное меню
                    app.getAudioManager().playSFXByName("menu_back");
                    stopCredits();
                    
                    if (fromSaveSystem) {
                        // Если титры запущены из системы сохранений (после прохождения уровня 116)
                        // Воспроизводим специальное видео перед возвратом в главное меню
                        playAfterCreditsVideoAndReturnToMenu();
                    } else {
                        // Обычный возврат в главное меню
                        cleanup();
                        
                        // Запускаем музыку главного меню с учетом завершения игры
                        app.startMainMenuMusic();
                        
                        // Возвращаемся в главное меню
                        app.returnToMainMenuFromSettings();
                    }
                    event.consume();
                    break;
                default:
                    // Игнорируем остальные клавиши
                    break;
            }
        });
        
        // Обработчик клика мыши для установки фокуса
        this.setOnMouseClicked(event -> {
            this.requestFocus();
        });
    }
    
    /**
     * Перемещение влево по кнопкам
     */
    private void moveLeft() {
        if (currentButtonIndex > 0) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex--;
            updateButtonHighlight();
        }
    }
    
    /**
     * Перемещение вправо по кнопкам
     */
    private void moveRight() {
        if (currentButtonIndex < controlButtons.length - 1) {
            app.getAudioManager().playSFXByName("menu_hover");
            currentButtonIndex++;
            updateButtonHighlight();
        }
    }
    
    /**
     * Выбор текущей кнопки
     */
    private void selectCurrentButton() {
        if (controlButtons != null && currentButtonIndex >= 0 && currentButtonIndex < controlButtons.length) {
            app.getAudioManager().playSFXByName("menu_select");
            controlButtons[currentButtonIndex].fire();
        }
    }
    
    /**
     * Обновление визуального выделения кнопок
     */
    private void updateButtonHighlight() {
        if (controlButtons == null) return;
        
        for (int i = 0; i < controlButtons.length; i++) {
            Button button = controlButtons[i];
            if (i == currentButtonIndex) {
                // Выделенная кнопка - яркий цвет и увеличенный размер
                button.setStyle(
                    "-fx-background-color: " + GameConfig.NEON_CYAN + "; " +
                    "-fx-border-color: #FFFFFF; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-effect: dropshadow(gaussian, " + GameConfig.NEON_CYAN + ", 10, 0.5, 0, 0);"
                );
                button.setScaleX(1.1);
                button.setScaleY(1.1);
            } else {
                // Обычная кнопка - стандартный стиль
                button.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: " + GameConfig.NEON_PURPLE + "; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px;"
                );
                button.setScaleX(1.0);
                button.setScaleY(1.0);
            }
        }
    }

    private void adjustLayoutForResolution(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());

        if (contentContainer != null) {
            StackPane.setAlignment(contentContainer, Pos.CENTER);
            StackPane.setMargin(contentContainer, new Insets(offsetY, offsetX, offsetY, offsetX));
        }

        if (creditsContainer != null) {
            double containerWidth = Math.min(960, GameConfig.GAME_WORLD_WIDTH - 64);
            double containerHeight = Math.min(620, GameConfig.GAME_WORLD_HEIGHT - 160);
            creditsContainer.setPrefWidth(containerWidth);
            creditsContainer.setMaxWidth(containerWidth);
            creditsContainer.setPrefHeight(containerHeight);
            creditsContainer.setMaxHeight(containerHeight);
            if (creditsContainer.getClip() instanceof javafx.scene.shape.Rectangle clip) {
                clip.setWidth(containerWidth);
                clip.setHeight(containerHeight);
            }
            if (creditsText != null) {
                creditsText.setWrappingWidth(containerWidth - 10);
            }
        }
    }
}
