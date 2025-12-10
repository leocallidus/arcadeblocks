package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.utils.ImageCache;
import com.arcadeblocks.utils.SettingsChangeTracker;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.ui.util.UINodeCleanup;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.config.GameConfig;
import com.almasb.fxgl.dsl.FXGL;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Labeled;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Меню настроек
 */
public class SettingsView extends VBox implements SupportsCleanup {
    
    private static final Logger LOG = Logger.getLogger(SettingsView.class.getName());
    private static final AtomicInteger ACTIVE_INSTANCES = new AtomicInteger();
    private static final AtomicInteger TOTAL_CREATED = new AtomicInteger();
    private static final AtomicInteger TOTAL_CLEANED = new AtomicInteger();

    private ArcadeBlocksApp app;
    private SettingsChangeTracker changeTracker;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();
    // КРИТИЧНО: Флаг для предотвращения выполнения асинхронных операций после cleanup
    private volatile boolean isCleanedUp = false;
    
    // КРИТИЧНО: Сохраняем ссылку на event filter для его удаления при cleanup
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyEventFilter;
    
    // Аудио настройки
    private Slider masterVolumeSlider;
    private Slider musicVolumeSlider;
    private Slider sfxVolumeSlider;
    
    // Настройки управления - теперь кнопки вместо текстовых полей
    private Button moveLeftButton;
    private Button moveRightButton;
    private Button launchButton;
    private Button callBallButton;
    private Button turboPaddleButton;
    private Button turboBallButton;
    private Button pauseButton;
    private Button plasmaWeaponButton;
    
    // Текущая кнопка, ожидающая ввода клавиши
    private Button waitingForKeyButton = null;
    private boolean isLoading = false; // Флаг для предотвращения сохранения во время загрузки
    
    // Игровые настройки
    private TextField playerNameField;
    private Slider paddleSpeedSlider;
    private Slider turboSpeedSlider;
    private ComboBox<LanguageOption> languageComboBox;
    private ComboBox<WindowModeOption> windowModeComboBox;
    private ComboBox<com.arcadeblocks.config.Resolution> resolutionComboBox;
    private ComboBox<com.arcadeblocks.config.DifficultyLevel> difficultyComboBox;
    private CheckBox levelBackgroundCheckBox;
    private CheckBox vsyncCheckBox;
    
    // Listeners для очистки (предотвращение утечек памяти)
    private javafx.beans.value.ChangeListener<LanguageOption> languageListener;
    private javafx.beans.value.ChangeListener<Boolean> levelBackgroundListener;
    private javafx.beans.value.ChangeListener<Boolean> vsyncListener;
    private javafx.beans.value.ChangeListener<String> playerNameListener;
    private javafx.beans.value.ChangeListener<Number> paddleSpeedListener;
    private javafx.beans.value.ChangeListener<Number> turboSpeedListener;
    private javafx.beans.value.ChangeListener<WindowModeOption> windowModeListener;
    private javafx.beans.value.ChangeListener<WindowModeOption> windowModeListener2; // второй listener
    private javafx.beans.value.ChangeListener<com.arcadeblocks.config.Resolution> resolutionListener;
    
    // Throttling для слайдеров (предотвращение частых сохранений)
    private javafx.animation.PauseTransition volumeThrottle;
    private javafx.animation.PauseTransition paddleSpeedThrottle;
    private javafx.animation.PauseTransition turboSpeedThrottle;
    private javafx.beans.value.ChangeListener<com.arcadeblocks.config.DifficultyLevel> difficultyListener;
    // Listeners для слайдеров громкости (критично для предотвращения утечек)
    private javafx.beans.value.ChangeListener<Number> masterVolumeListener;
    private javafx.beans.value.ChangeListener<Number> musicVolumeListener;
    private javafx.beans.value.ChangeListener<Number> sfxVolumeListener;
    // Listeners для tooltip (критично для предотвращения утечек)
    private java.util.List<javafx.beans.value.ChangeListener<java.util.Locale>> tooltipListeners = new java.util.ArrayList<>();
    // КРИТИЧНО: Сохраняем ссылки на все созданные bindings для их dispose
    private java.util.List<javafx.beans.binding.StringBinding> activeBindings = new java.util.ArrayList<>();
    // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки
    private FadeTransition appearFadeTransition;
    private TranslateTransition appearSlideTransition;
    private java.util.Map<Button, ScaleTransition> buttonScaleTransitions = new java.util.HashMap<>();
    // КРИТИЧНО: Анимации закрытия для предотвращения утечек памяти
    private FadeTransition closeFadeTransition;
    private ScaleTransition closeScaleTransition;
    // КРИТИЧНО: Флаг для предотвращения повторного закрытия
    private volatile boolean isClosing = false;
    
    public SettingsView(ArcadeBlocksApp app) {
        this.app = app;
        this.changeTracker = new SettingsChangeTracker();
        final int created = TOTAL_CREATED.incrementAndGet();
        final int active = ACTIVE_INSTANCES.incrementAndGet();
        // System.out.println("[SettingsDiagnostics] SettingsView created (total=" + created + ", active=" + active + ")");
        
        // Инициализируем throttling для слайдеров (задержка 100мс)
        volumeThrottle = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        paddleSpeedThrottle = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        turboSpeedThrottle = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        
        initializeUI();
        
        // Инициализируем трекер изменений после загрузки настроек
        changeTracker.initialize(app.getSaveManager());
    }
    
    /**
     * Очистка ресурсов и обработчиков событий для предотвращения утечек памяти
     */
    @Override
    public void cleanup() {
        // КРИТИЧНО: Защита от повторного вызова cleanup()
        if (isCleanedUp) {
            return; // Уже очищен
        }
        
        // КРИТИЧНО: Устанавливаем флаг очистки ПЕРВЫМ, чтобы предотвратить выполнение асинхронных операций
        isCleanedUp = true;
        final int active = ACTIVE_INSTANCES.updateAndGet(prev -> prev > 0 ? prev - 1 : 0);
        final int cleaned = TOTAL_CLEANED.incrementAndGet();
        // System.out.println("[SettingsDiagnostics] SettingsView cleanup (cleaned=" + cleaned + ", active=" + active + ")");
        
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
        
        // КРИТИЧНО: Останавливаем throttling таймеры
        if (volumeThrottle != null) {
            volumeThrottle.stop();
            volumeThrottle = null;
        }
        if (paddleSpeedThrottle != null) {
            paddleSpeedThrottle.stop();
            paddleSpeedThrottle = null;
        }
        if (turboSpeedThrottle != null) {
            turboSpeedThrottle.stop();
            turboSpeedThrottle = null;
        }
        
        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        if (keyEventFilter != null) {
            this.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyEventFilter);
            keyEventFilter = null;
        }
        
        // Очищаем обработчики кнопок управления
        if (moveLeftButton != null) moveLeftButton.setOnAction(null);
        if (moveRightButton != null) moveRightButton.setOnAction(null);
        if (launchButton != null) launchButton.setOnAction(null);
        if (callBallButton != null) callBallButton.setOnAction(null);
        if (turboPaddleButton != null) turboPaddleButton.setOnAction(null);
        if (turboBallButton != null) turboBallButton.setOnAction(null);
        if (pauseButton != null) pauseButton.setOnAction(null);
        if (plasmaWeaponButton != null) plasmaWeaponButton.setOnAction(null);
        
        // КРИТИЧНО: Удаляем listeners с CheckBox'ов
        // НЕ вызываем unbind() - dispose() на bindings уже освободил все ресурсы
        if (levelBackgroundCheckBox != null) {
            // КРИТИЧНО: Очищаем tooltip перед удалением listener'а
            levelBackgroundCheckBox.setTooltip(null);
            // КРИТИЧНО: Удаляем listener с selectedProperty
            if (levelBackgroundListener != null) {
                levelBackgroundCheckBox.selectedProperty().removeListener(levelBackgroundListener);
                levelBackgroundListener = null;
            }
        }
        if (vsyncCheckBox != null) {
            // КРИТИЧНО: Очищаем tooltip перед удалением listener'а
            vsyncCheckBox.setTooltip(null);
            // КРИТИЧНО: Удаляем listener с selectedProperty
            if (vsyncListener != null) {
                vsyncCheckBox.selectedProperty().removeListener(vsyncListener);
                vsyncListener = null;
            }
        }
        
        // КРИТИЧНО: Удаляем listeners с TextField
        if (playerNameField != null && playerNameListener != null) {
            playerNameField.textProperty().removeListener(playerNameListener);
            playerNameListener = null;
        }
        
        // КРИТИЧНО: Удаляем listeners со Slider'ов
        if (paddleSpeedSlider != null) {
            // Удаляем основной listener
            if (paddleSpeedListener != null) {
                paddleSpeedSlider.valueProperty().removeListener(paddleSpeedListener);
                paddleSpeedListener = null;
            }
            // КРИТИЧНО: Удаляем listener для обновления label, если он был сохранен в userData
            Object labelListener = paddleSpeedSlider.getUserData();
            if (labelListener instanceof javafx.beans.value.ChangeListener) {
                paddleSpeedSlider.valueProperty().removeListener((javafx.beans.value.ChangeListener<Number>) labelListener);
                paddleSpeedSlider.setUserData(null);
            }
        }
        if (turboSpeedSlider != null) {
            // Удаляем основной listener
            if (turboSpeedListener != null) {
                turboSpeedSlider.valueProperty().removeListener(turboSpeedListener);
                turboSpeedListener = null;
            }
            // КРИТИЧНО: Удаляем listener для обновления label
            Object labelListener = turboSpeedSlider.getUserData();
            if (labelListener instanceof javafx.beans.value.ChangeListener) {
                turboSpeedSlider.valueProperty().removeListener((javafx.beans.value.ChangeListener<Number>) labelListener);
                turboSpeedSlider.setUserData(null);
            }
        }
        // КРИТИЧНО: Удаляем listeners со слайдеров громкости
        if (masterVolumeSlider != null && masterVolumeListener != null) {
            masterVolumeSlider.valueProperty().removeListener(masterVolumeListener);
            masterVolumeListener = null;
            masterVolumeSlider.setUserData(null);
        }
        if (musicVolumeSlider != null && musicVolumeListener != null) {
            musicVolumeSlider.valueProperty().removeListener(musicVolumeListener);
            musicVolumeListener = null;
            musicVolumeSlider.setUserData(null);
        }
        if (sfxVolumeSlider != null && sfxVolumeListener != null) {
            sfxVolumeSlider.valueProperty().removeListener(sfxVolumeListener);
            sfxVolumeListener = null;
            sfxVolumeSlider.setUserData(null);
        }
        
        // КРИТИЧНО: Удаляем listeners с ComboBox'ов
        if (languageComboBox != null) {
            if (languageListener != null) {
                languageComboBox.valueProperty().removeListener(languageListener);
                languageListener = null;
            }
            languageComboBox.setCellFactory(null);
            languageComboBox.setButtonCell(null);
            languageComboBox.getItems().clear();
        }

        if (windowModeComboBox != null) {
            if (windowModeListener != null) {
                windowModeComboBox.valueProperty().removeListener(windowModeListener);
                windowModeListener = null;
            }
            if (windowModeListener2 != null) {
                windowModeComboBox.valueProperty().removeListener(windowModeListener2);
                windowModeListener2 = null;
            }
            // КРИТИЧНО: Очищаем items и cell factory для освобождения bindings в ячейках
            windowModeComboBox.setCellFactory(null);
            windowModeComboBox.setButtonCell(null);
            windowModeComboBox.getItems().clear();
        }
        
        if (resolutionComboBox != null) {
            if (resolutionListener != null) {
                resolutionComboBox.valueProperty().removeListener(resolutionListener);
                resolutionListener = null;
            }
            // КРИТИЧНО: Очищаем items для освобождения памяти
            resolutionComboBox.getItems().clear();
        }
        
        if (difficultyComboBox != null) {
            if (difficultyListener != null) {
                difficultyComboBox.valueProperty().removeListener(difficultyListener);
                difficultyListener = null;
            }
            // КРИТИЧНО: Очищаем items и cell factory для освобождения bindings в ячейках
            // Это критично, так как каждая ячейка создает binding на localizationManager.localeProperty()
            difficultyComboBox.setCellFactory(null);
            difficultyComboBox.setButtonCell(null);
            difficultyComboBox.getItems().clear();
        }
        
        // Останавливаем анимации
        
        // КРИТИЧНО: Удаляем listeners для tooltip
        synchronized (tooltipListeners) {
            for (javafx.beans.value.ChangeListener<java.util.Locale> listener : tooltipListeners) {
                try {
                    localizationManager.localeProperty().removeListener(listener);
                } catch (Exception ignored) {}
            }
            tooltipListeners.clear();
        }
        
        // КРИТИЧНО: Dispose всех созданных bindings для полного освобождения памяти
        // После dispose() вызов unbind() не нужен - dispose() уже освобождает все ресурсы
        for (javafx.beans.binding.StringBinding binding : activeBindings) {
            if (binding != null) {
                try {
                    binding.dispose();
                } catch (Exception e) {
                    // Игнорируем ошибки при dispose
                }
            }
        }
        activeBindings.clear();
        
        // КРИТИЧНО: Освобождаем изображения СИНХРОННО перед очисткой children
        UINodeCleanup.releaseImages(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper СИНХРОННО
        ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Очищаем все дочерние элементы ПОСЛЕ освобождения изображений
        getChildren().clear();
        
        setBackground(null);
        
        // КРИТИЧНО: Обнуляем ссылки для предотвращения утечек памяти
        app = null;
        changeTracker = null;
        
        // Обнуляем все UI компоненты
        masterVolumeSlider = null;
        musicVolumeSlider = null;
        sfxVolumeSlider = null;
        moveLeftButton = null;
        moveRightButton = null;
        launchButton = null;
        callBallButton = null;
        turboPaddleButton = null;
        turboBallButton = null;
        pauseButton = null;
        plasmaWeaponButton = null;
        waitingForKeyButton = null;
        playerNameField = null;
        paddleSpeedSlider = null;
        windowModeComboBox = null;
        resolutionComboBox = null;
        difficultyComboBox = null;
        /* callBallSoundCheckBox = null; */
        vsyncCheckBox = null;
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(10);  // Уменьшили с 30 до 10
        
        // КРИТИЧНО: НЕ создаем свой собственный фон!
        // SettingsView должен быть прозрачным и показываться поверх существующего MainMenuView
        // Это предотвращает создание дубликатов фоновых изображений
        setBackground(null);
        
        if (app.isInPauseSettings()) {
            // В паузе затемняем фон уровня
            setStyle("-fx-background-color: rgba(15, 15, 28, 0.72);");
        } else {
            // В главном меню используем полупрозрачный темный фон
            // MainMenuView остается видимым под SettingsView
            setStyle("-fx-background-color: rgba(15, 15, 28, 0.85);");
        }
        
        setPadding(new Insets(15));  // Уменьшили с 50 до 15
        
        // Применяем CSS стили
        getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        
        // Заголовок
        Label titleLabel = new Label();
        bindText(titleLabel, "settings.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 32));  // Немного уменьшили шрифт
        titleLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxHeight(40);
        
        // Основной контейнер настроек - горизонтальное расположение секций
        HBox settingsContainer = new HBox(30);  // Горизонтальное расположение с отступом
        settingsContainer.setAlignment(Pos.CENTER);
        
        // Аудио настройки
        VBox audioSection = createAudioSection();
        
        // Настройки управления
        VBox controlsSection = createControlsSection();
        
        // Устанавливаем флаг загрузки перед созданием секций, чтобы предотвратить звуки при инициализации
        isLoading = true;
        
        // Игровые настройки
        VBox gameSection = createGameSection();
        
        // Кнопки
        HBox buttonBox = createButtonBox();
        buttonBox.setMaxHeight(50);
        
        settingsContainer.getChildren().addAll(audioSection, controlsSection, gameSection);
        
        // Полупрозрачный скругленный контейнер поверх фонового изображения
        VBox contentContainer = new VBox(20);
        contentContainer.setAlignment(Pos.CENTER);
        contentContainer.setPadding(new Insets(24));
        contentContainer.setMaxWidth(860);
        contentContainer.setStyle(
            "-fx-background-color: rgba(15, 15, 28, 0.39);" +
            "-fx-background-radius: 16px;" +
            "-fx-border-color: " + GameConfig.NEON_PURPLE + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 18, 0.35, 0, 0);"
        );

        contentContainer.getChildren().addAll(titleLabel, settingsContainer, buttonBox);

        getChildren().add(contentContainer);

        // Плавная анимация появления контейнера настроек
        playAppearAnimation(contentContainer);
        
        // Настройка обработчика клавиш для назначения кнопок
        setupKeyBindingHandler();

        ResponsiveLayoutHelper.bindToStage(this);
        setUserData("fullScreenOverlay");
        
        // Загружаем настройки (флаг isLoading уже установлен)
        loadSettings();
        
        // Сбрасываем флаг загрузки после загрузки всех настроек
        isLoading = false;
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
    
    private VBox createAudioSection() {
        VBox section = new VBox(10);  // Отступ между элементами
        section.setAlignment(Pos.CENTER);
        section.setPrefWidth(300);  // Фиксированная ширина секции
        section.setMinWidth(300);
        section.setMaxWidth(300);
        
        Label sectionTitle = new Label();
        bindText(sectionTitle, "settings.section.audio");
        sectionTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));  // Немного уменьшили шрифт
        sectionTitle.setTextFill(Color.web(GameConfig.NEON_PINK));
        
        
        // Общая громкость
        VBox masterVolumeBox = createAudioSliderBox("settings.audio.master", 0, 100, 70, "master_volume", true);
        masterVolumeSlider = (Slider) masterVolumeBox.getChildren().get(1);
        
        // Громкость музыки
        VBox musicVolumeBox = createAudioSliderBox("settings.audio.music", 0, 100, 60, "music_volume", false);
        musicVolumeSlider = (Slider) musicVolumeBox.getChildren().get(1);
        
        // Громкость звуковых эффектов
        VBox sfxVolumeBox = createAudioSliderBox("settings.audio.sfx", 0, 100, 80, "sfx_volume", false);
        sfxVolumeSlider = (Slider) sfxVolumeBox.getChildren().get(1);
        
        section.getChildren().addAll(sectionTitle, masterVolumeBox, 
                                   musicVolumeBox, sfxVolumeBox);
        
        return section;
    }
    
    private VBox createControlsSection() {
        VBox section = new VBox(10);  // Отступ между элементами
        section.setAlignment(Pos.CENTER);
        section.setPrefWidth(300);  // Фиксированная ширина секции
        section.setMinWidth(300);
        section.setMaxWidth(300);
        
        Label sectionTitle = new Label();
        bindText(sectionTitle, "settings.section.controls");
        sectionTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));  // Немного уменьшили шрифт
        sectionTitle.setTextFill(Color.web(GameConfig.NEON_PURPLE));
        
        // Движение влево
        VBox moveLeftBox = createKeyBindingBox("settings.controls.left");
        moveLeftButton = (Button) moveLeftBox.getChildren().get(1);
        
        // Движение вправо
        VBox moveRightBox = createKeyBindingBox("settings.controls.right");
        moveRightButton = (Button) moveRightBox.getChildren().get(1);
        
        // Запуск
        VBox launchBox = createKeyBindingBox("settings.controls.launch");
        launchButton = (Button) launchBox.getChildren().get(1);

        // Притягивание мяча
        VBox callBallBox = createKeyBindingBox("settings.controls.call_ball");
        callBallButton = (Button) callBallBox.getChildren().get(1);
        
        // Турбо-ракетка
        VBox turboPaddleBox = createKeyBindingBox("settings.controls.turbo");
        turboPaddleButton = (Button) turboPaddleBox.getChildren().get(1);

        // Турбо-мяч
        VBox turboBallBox = createKeyBindingBox("settings.controls.turbo_ball");
        turboBallButton = (Button) turboBallBox.getChildren().get(1);
        
        // Плазменное оружие
        VBox plasmaBox = createKeyBindingBox("settings.controls.plasma");
        plasmaWeaponButton = (Button) plasmaBox.getChildren().get(1);
        
        // Кнопка паузы
        VBox pauseBox = createKeyBindingBox("settings.controls.pause");
        pauseButton = (Button) pauseBox.getChildren().get(1);
        
        section.getChildren().addAll(sectionTitle, moveLeftBox, moveRightBox, 
                           launchBox, callBallBox, turboPaddleBox, turboBallBox, plasmaBox, pauseBox);
        
        return section;
    }
    
    private VBox createGameSection() {
        VBox section = new VBox(10);  // Отступ между элементами
        section.setAlignment(Pos.CENTER);
        section.setPrefWidth(300);  // Фиксированная ширина секции
        section.setMinWidth(300);
        section.setMaxWidth(300);
        
        Label sectionTitle = new Label();
        bindText(sectionTitle, "settings.section.game");
        sectionTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));  // Немного уменьшили шрифт
        sectionTitle.setTextFill(Color.web(GameConfig.NEON_GREEN));
        
        // Язык игры
        VBox languageBox = new VBox(8);
        languageBox.setAlignment(Pos.CENTER);
        Label languageLabel = createLabel("menu.language");
        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll(LanguageOption.values());
        languageComboBox.setValue(LanguageOption.ENGLISH); // Default
        languageComboBox.setPrefWidth(180);
        languageComboBox.setMinWidth(180);
        languageComboBox.setMaxWidth(180);
        languageComboBox.getStyleClass().add("combo-box");
        languageComboBox.setCellFactory(listView -> createLanguageCell());
        languageComboBox.setButtonCell(createLanguageCell());
        
        languageListener = (obs, oldVal, newVal) -> {
            if (isCleanedUp || app == null || changeTracker == null) return;
            if (newVal != null && !isLoading) {
                playSettingsToggleSound();
                updateChangeTracker("language", newVal.getCode());
                // Применяем язык немедленно
                app.getSaveManager().setLanguage(newVal.getCode());
                localizationManager.setLanguage(newVal.getCode());
                saveSettings();
            }
        };
        languageComboBox.valueProperty().addListener(languageListener);
        languageBox.getChildren().addAll(languageLabel, languageComboBox);
        
        // Имя игрока
        VBox playerNameBox = createGameTextFieldBox("settings.game.player_name", "player.default");
        playerNameField = (TextField) playerNameBox.getChildren().get(1);
        if (!canEditPlayerName()) {
            playerNameField.setDisable(true);
            playerNameField.setOpacity(0.7);
            bindPromptText(playerNameField, "settings.game.player_name.restricted");
        }
        // Добавляем обработчик сохранения для имени игрока
        // Сохраняем listener для последующей очистки
        playerNameListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            if (!canEditPlayerName()) {
                return;
            }
            updateChangeTracker("player_name", newVal);
            if (!isLoading) {
                saveSettings();
            }
        };
        playerNameField.textProperty().addListener(playerNameListener);
        
        // Скорость ракетки
        VBox paddleSpeedBox = createGameSliderBox("settings.game.paddle_speed", 100, 1000, 400);
        paddleSpeedSlider = (Slider) paddleSpeedBox.getChildren().get(1);
        // Добавляем обработчик сохранения для скорости ракетки
        // Сохраняем listener для последующей очистки
        paddleSpeedListener = (obs, oldVal, newVal) -> {
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            updateChangeTracker("paddle_speed", newVal.doubleValue());
            if (!isLoading) {
                app.updatePaddleSpeed();
                if (paddleSpeedThrottle != null) {
                    paddleSpeedThrottle.stop();
                    paddleSpeedThrottle.setOnFinished(evt -> {
                        if (!isCleanedUp && !isLoading) {
                            saveSettings();
                        }
                    });
                    paddleSpeedThrottle.playFromStart();
                } else {
                    saveSettings();
                }
            }
        };
        paddleSpeedSlider.valueProperty().addListener(paddleSpeedListener);
        
        // Скорость турбо-режима
        VBox turboSpeedBox = createGameSliderBox("settings.game.turbo_speed", 100, 300, 250); // 1.0x - 3.0x, default 2.5x
        turboSpeedSlider = (Slider) turboSpeedBox.getChildren().get(1);
        // Добавляем обработчик сохранения для скорости турбо
        turboSpeedListener = (obs, oldVal, newVal) -> {
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            double speed = newVal.doubleValue() / 100.0;
            updateChangeTracker("turbo_mode_speed", speed);
            if (!isLoading) {
                app.getSaveManager().setTurboModeSpeed(speed);
                app.updateTurboSpeed();
                if (turboSpeedThrottle != null) {
                    turboSpeedThrottle.stop();
                    turboSpeedThrottle.setOnFinished(evt -> {
                        if (!isCleanedUp && !isLoading) {
                            saveSettings();
                        }
                    });
                    turboSpeedThrottle.playFromStart();
                } else {
                    saveSettings();
                }
            }
        };
        turboSpeedSlider.valueProperty().addListener(turboSpeedListener);
        
        // Режим окна
        VBox windowModeBox = new VBox(8);  // Вертикальное расположение
        windowModeBox.setAlignment(Pos.CENTER);
        Label windowModeLabel = createLabel("settings.game.window_mode");
        windowModeComboBox = new ComboBox<>();
        windowModeComboBox.getItems().addAll(WindowModeOption.values());
        windowModeComboBox.setValue(WindowModeOption.FULLSCREEN); // Значение по умолчанию
        windowModeComboBox.setPrefWidth(180); // Уменьшили для горизонтального расположения
        windowModeComboBox.setMinWidth(180);  // Минимальная ширина
        windowModeComboBox.setMaxWidth(180);  // Максимальная ширина
        windowModeComboBox.getStyleClass().add("combo-box"); // Применяем CSS стиль
        // Сохраняем listener для последующей очистки
        windowModeListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            if (newVal != null && !isLoading) {
                playSettingsToggleSound();
                // Обновляем настройки
                app.getSaveManager().setFullscreen(newVal.isFullscreen());
                app.getSaveManager().setWindowedMode(newVal.isWindowed());
                // Дожидаемся завершения записи, чтобы applyWindowSettings увидел актуальные значения
                app.getSaveManager().awaitPendingWrites();
                
                updateChangeTracker("window_mode", newVal.name());
                saveSettings();
                // Применяем изменения немедленно
                app.applyWindowSettings();
            }
        };
        windowModeComboBox.valueProperty().addListener(windowModeListener);
        windowModeComboBox.setCellFactory(listView -> createWindowModeCell());
        windowModeComboBox.setButtonCell(createWindowModeCell());
        windowModeBox.getChildren().addAll(windowModeLabel, windowModeComboBox);

        // Разрешение игры
        VBox resolutionBox = new VBox(8);
        resolutionBox.setAlignment(Pos.CENTER);
        Label resolutionLabel = createLabel("settings.game.resolution");
        resolutionComboBox = new ComboBox<>();
        updateResolutionComboBox(); // Обновляем список разрешений в зависимости от режима окна
        resolutionComboBox.setValue(GameConfig.DEFAULT_RESOLUTION);
        resolutionComboBox.setPrefWidth(180);
        resolutionComboBox.setMinWidth(180);
        resolutionComboBox.setMaxWidth(180);
        resolutionComboBox.getStyleClass().add("combo-box");
        // Сохраняем listener для последующей очистки
        resolutionListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            if (newVal != null && !isLoading) {
                playSettingsToggleSound();
                // Сохраняем новое разрешение
                app.getSaveManager().setResolution(newVal);
                app.getSaveManager().awaitPendingWrites();
                
                updateChangeTracker("resolution", newVal.toString());
                saveSettings();
                
                // Применяем изменения разрешения
                app.applyResolutionSettings(newVal);
            }
        };
        resolutionComboBox.valueProperty().addListener(resolutionListener);
        
        // Обновляем список разрешений при изменении режима окна
        // Сохраняем второй listener для последующей очистки
        windowModeListener2 = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            if (newVal != null && !isLoading) {
                // Сохраняем текущее разрешение перед обновлением списка
                com.arcadeblocks.config.Resolution currentRes = resolutionComboBox.getValue();
                updateResolutionComboBox();
                // Если переключились на fullscreen и было 1600x900, автоматически выбираем 1920x1080
                if (newVal.isFullscreen() && currentRes != null && currentRes.equals(GameConfig.RESOLUTION_1600x900)) {
                    resolutionComboBox.setValue(GameConfig.RESOLUTION_1920x1080);
                    // Сохраняем новое разрешение
                    app.getSaveManager().setResolution(GameConfig.RESOLUTION_1920x1080);
                    saveSettings();
                }
            }
        };
        windowModeComboBox.valueProperty().addListener(windowModeListener2);
        
        resolutionBox.getChildren().addAll(resolutionLabel, resolutionComboBox);
        
        // Сложность игры
        VBox difficultyBox = new VBox(8);
        difficultyBox.setAlignment(Pos.CENTER);
        Label difficultyLabel = createLabel("settings.game.difficulty");
        difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().addAll(com.arcadeblocks.config.DifficultyLevel.values());
        difficultyComboBox.setValue(com.arcadeblocks.config.DifficultyLevel.NORMAL);
        difficultyComboBox.setPrefWidth(180);
        difficultyComboBox.setMinWidth(180);
        difficultyComboBox.setMaxWidth(180);
        difficultyComboBox.getStyleClass().add("combo-box");
        difficultyComboBox.setCellFactory(listView -> createDifficultyCell());
        difficultyComboBox.setButtonCell(createDifficultyCell());
        // Сохраняем listener для последующей очистки
        difficultyListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            if (newVal != null && !isLoading) {
                playSettingsToggleSound();
                updateChangeTracker("difficulty", newVal.name());
                saveSettings();
            }
        };
        difficultyComboBox.valueProperty().addListener(difficultyListener);
        
        // Блокируем комбобокс сложности, если мы в настройках паузы
        if (app.isInPauseSettings()) {
            difficultyComboBox.setDisable(true);
            difficultyComboBox.setStyle("-fx-opacity: 0.5;");
        }
        difficultyBox.getChildren().addAll(difficultyLabel, difficultyComboBox);

        levelBackgroundCheckBox = new CheckBox();
        localizationManager.bind(levelBackgroundCheckBox, "settings.game.level_background");
        levelBackgroundCheckBox.getStyleClass().add("debug-checkbox");
        levelBackgroundCheckBox.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
        levelBackgroundCheckBox.setTextFill(Color.web("#E0E0E0"));
        levelBackgroundCheckBox.setAlignment(Pos.CENTER);
        // Сохраняем listener для последующей очистки
        levelBackgroundListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null || isLoading) {
                return;
            }
            // КРИТИЧНО: Игнорируем если значение не изменилось
            if (Objects.equals(oldVal, newVal)) {
                return;
            }
            try {
                updateChangeTracker("level_background_enabled", newVal);
                // Звук проигрываем асинхронно
                javafx.application.Platform.runLater(() -> {
                    if (!isCleanedUp && app != null) {
                        playSettingsToggleSound();
                    }
                });
                
                // Сохраняем настройку, чтобы она применилась немедленно (если поддерживается app)
                app.getSaveManager().setLevelBackgroundEnabled(newVal);
                app.refreshLevelBackground();
                
                // Если мы в игре, возможно, нужно обновить фон (зависит от реализации GameplayUIView)
                // Но пока просто сохраняем
            } catch (Exception e) {
                // Игнорируем ошибки, если SettingsView уже очищен
            }
        };
        levelBackgroundCheckBox.selectedProperty().addListener(levelBackgroundListener);
        levelBackgroundCheckBox.setTooltip(createLocalizedTooltip("settings.game.level_background.tooltip"));

        vsyncCheckBox = new CheckBox();
        localizationManager.bind(vsyncCheckBox, "settings.game.vsync");
        vsyncCheckBox.getStyleClass().add("debug-checkbox");
        vsyncCheckBox.setFont(Font.font("Orbitron", FontWeight.BOLD, 14));
        vsyncCheckBox.setTextFill(Color.web("#E0E0E0"));
        vsyncCheckBox.setAlignment(Pos.CENTER);
        // Сохраняем listener для последующей очистки
        vsyncListener = (obs, oldVal, newVal) -> {
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен
            if (isCleanedUp || app == null || changeTracker == null || isLoading) {
                return;
            }
            // КРИТИЧНО: Игнорируем если значение не изменилось
            if (Objects.equals(oldVal, newVal)) {
                return;
            }
            try {
                // System.out.println("[VSYNC] vsyncListener: " + oldVal + " -> " + newVal);
                updateChangeTracker("vsync_enabled", newVal);
                // Звук проигрываем асинхронно
                javafx.application.Platform.runLater(() -> {
                    if (!isCleanedUp && app != null) {
                        playSettingsToggleSound();
                    }
                });
                // КРИТИЧНО: НЕ применяем FPS лимит немедленно - только сохраняем
                // Настройка будет применена после перезапуска игры
                app.applyVSyncSetting(newVal);
                
                // В паузе не сохраняем сразу - сохраним все при выходе
            } catch (Exception e) {
                // Игнорируем ошибки, если SettingsView уже очищен
            }
        };
        vsyncCheckBox.selectedProperty().addListener(vsyncListener);
        vsyncCheckBox.setTooltip(createLocalizedTooltip("settings.game.vsync.tooltip"));
        
        section.getChildren().addAll(sectionTitle, languageBox, playerNameBox, paddleSpeedBox, turboSpeedBox, 
                                   windowModeBox, resolutionBox, difficultyBox, levelBackgroundCheckBox, vsyncCheckBox);
        
        return section;
    }

    // Удалены все методы управления разрешением/letterbox из SettingsView
    
    @SuppressWarnings("unused")
    private HBox createSliderBox(String labelText, double min, double max, double defaultValue) {
        HBox box = new HBox(15);  // Уменьшили с 20 до 15
        box.setAlignment(Pos.CENTER);
        
        Label label = createLabel(labelText);
        Slider slider = new Slider(min, max, defaultValue);
        slider.setPrefWidth(180);  // Уменьшили с 200 до 180
        
        Label valueLabel = new Label(String.valueOf((int) defaultValue));
        valueLabel.setFont(Font.font("Orbitron", 12));  // Уменьшили с 14 до 12
        valueLabel.setTextFill(Color.web("#E0E0E0")); // Светло-серый текст
        valueLabel.setMinWidth(35);  // Фиксированная минимальная ширина
        
        // Обновление значения при изменении слайдера
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.valueOf((int) newVal.doubleValue()));
            // Обновляем трекер изменений
            updateChangeTracker("volume", newVal.doubleValue() / 100.0);
            // Автоматическое сохранение
            saveSettings();
        });
        
        box.getChildren().addAll(label, slider, valueLabel);
        
        return box;
    }
    
    private VBox createGameSliderBox(String labelText, double min, double max, double defaultValue) {
        VBox box = new VBox(8);  // Вертикальное расположение
        box.setAlignment(Pos.CENTER);
        
        Label label = createLabel(labelText);
        Slider slider = new Slider(min, max, defaultValue);
        slider.setPrefWidth(180);  // Уменьшили для горизонтального расположения
        slider.setMinWidth(180);
        slider.setMaxWidth(180);
        slider.getStyleClass().add("slider"); // Применяем CSS стиль
        
        Label valueLabel = new Label(String.valueOf((int) defaultValue));
        if ("settings.game.turbo_speed".equals(labelText)) {
            valueLabel.setText(String.format("%.1fx", defaultValue / 100.0));
        }
        valueLabel.setFont(Font.font("Orbitron", 12));
        valueLabel.setTextFill(Color.web("#E0E0E0"));
        valueLabel.setAlignment(Pos.CENTER);
        
        // Обновление значения при изменении слайдера (без автоматического сохранения)
        // КРИТИЧНО: Сохраняем listener в userData слайдера для последующей очистки
        javafx.beans.value.ChangeListener<Number> labelUpdateListener = (obs, oldVal, newVal) -> {
            if ("settings.game.turbo_speed".equals(labelText)) {
                valueLabel.setText(String.format("%.1fx", newVal.doubleValue() / 100.0));
            } else {
                valueLabel.setText(String.valueOf((int) newVal.doubleValue()));
            }
        };
        slider.valueProperty().addListener(labelUpdateListener);
        // Сохраняем listener в userData для последующей очистки
        slider.setUserData(labelUpdateListener);
        
        box.getChildren().addAll(label, slider, valueLabel);
        
        return box;
    }

    private VBox createAudioSliderBox(String labelText, double min, double max, double defaultValue, String settingKey, boolean isMasterVolume) {
        VBox box = new VBox(8);  // ������������ ������������
        box.setAlignment(Pos.CENTER);

        Label label = createLabel(labelText);
        Slider slider = new Slider(min, max, defaultValue);
        slider.setPrefWidth(180);
        slider.setMinWidth(180);
        slider.setMaxWidth(180);
        slider.getStyleClass().add("slider");

        Label valueLabel = new Label(String.valueOf((int) defaultValue));
        valueLabel.setFont(Font.font("Orbitron", 12));
        valueLabel.setTextFill(Color.web("#E0E0E0"));
        valueLabel.setAlignment(Pos.CENTER);

        javafx.beans.value.ChangeListener<Number> listener = (obs, oldVal, newVal) -> {
            if (isCleanedUp || app == null || changeTracker == null) {
                return;
            }
            valueLabel.setText(String.valueOf((int) newVal.doubleValue()));
            double volume = newVal.doubleValue() / 100.0;

            switch (settingKey) {
                case "master_volume":
                    app.getAudioManager().setMasterVolume(volume);
                    break;
                case "music_volume":
                    app.getAudioManager().setMusicVolume(volume);
                    break;
                case "sfx_volume":
                    app.getAudioManager().setSfxVolume(volume);
                    break;
            }

            updateChangeTracker(settingKey, volume);

            if (!isLoading) {
                if (volumeThrottle != null) {
                    volumeThrottle.stop();
                    volumeThrottle.setOnFinished(evt -> {
                        if (!isCleanedUp && !isLoading) {
                            saveSettings();
                        }
                    });
                    volumeThrottle.playFromStart();
                } else {
                    saveSettings();
                }
            }
        };

        if (isMasterVolume) {
            masterVolumeListener = listener;
        } else if ("music_volume".equals(settingKey)) {
            musicVolumeListener = listener;
        } else if ("sfx_volume".equals(settingKey)) {
            sfxVolumeListener = listener;
        }

        slider.valueProperty().addListener(listener);
        slider.setUserData(listener);

        box.getChildren().addAll(label, slider, valueLabel);

        return box;
    }

    @SuppressWarnings("unused")
    private VBox createTextFieldBox(String labelText, String defaultValue) {
        VBox box = new VBox(8);  // Вертикальное расположение
        box.setAlignment(Pos.CENTER);
        
        Label label = createLabel(labelText);
        TextField textField = new TextField(defaultValue);
        textField.setPrefWidth(180);  // Уменьшили для горизонтального расположения
        textField.setMinWidth(180);
        textField.setMaxWidth(180);
        textField.setFont(Font.font("Orbitron", 12));  // Уменьшили с 14 до 12
        textField.getStyleClass().add("text-field"); // Применяем CSS стиль
        
        // Добавляем обработчик изменений
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateChangeTracker("text_field", newVal);
            // Автоматическое сохранение
            saveSettings();
        });
        
        box.getChildren().addAll(label, textField);
        
        return box;
    }
    
    private VBox createGameTextFieldBox(String labelKey, String defaultValueKey) {
        VBox box = new VBox(8);  // Вертикальное расположение
        box.setAlignment(Pos.CENTER);
        
        Label label = createLabel(labelKey);
        TextField textField = new TextField(defaultValueKey != null ? localize(defaultValueKey) : "");
        textField.setPrefWidth(180);  // Уменьшили для горизонтального расположения
        textField.setMinWidth(180);
        textField.setMaxWidth(180);
        textField.setFont(Font.font("Orbitron", 12));
        textField.getStyleClass().add("text-field"); // Применяем CSS стиль
        
        // Добавляем обработчик изменений (без автоматического сохранения)
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateChangeTracker("text_field", newVal);
        });
        
        box.getChildren().addAll(label, textField);
        
        return box;
    }
    
    private VBox createKeyBindingBox(String labelKey) {
        VBox box = new VBox(8);  // Вертикальное расположение
        box.setAlignment(Pos.CENTER);
        
        Label label = createLabel(labelKey);
        Button keyButton = new Button("...");
        keyButton.setPrefWidth(180);  // Уменьшили для горизонтального расположения
        keyButton.setMinWidth(180);
        keyButton.setMaxWidth(180);
        keyButton.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));
        keyButton.setTextFill(Color.WHITE);
        
        String normalStyle = String.format(
            "-fx-background-color: transparent; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 3px; " +
            "-fx-background-radius: 3px; " +
            "-fx-cursor: hand;",
            GameConfig.NEON_PURPLE
        );
        
        String waitingStyle = String.format(
            "-fx-background-color: %s60; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 3px; " +
            "-fx-background-radius: 3px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 10, 0.7, 0, 0); " +
            "-fx-text-fill: #000000;",  // Черный текст для контраста
            GameConfig.NEON_YELLOW, GameConfig.NEON_YELLOW, GameConfig.NEON_YELLOW
        );
        
        keyButton.setStyle(normalStyle);
        keyButton.setUserData(normalStyle); // Сохраняем нормальный стиль
        
        // Обработчик нажатия на кнопку
        keyButton.setOnAction(e -> {
            // Если уже какая-то кнопка ждет ввода, сбрасываем её
            if (waitingForKeyButton != null && waitingForKeyButton != keyButton) {
                waitingForKeyButton.setStyle((String) waitingForKeyButton.getUserData());
            }
            
            // Устанавливаем эту кнопку в режим ожидания
            waitingForKeyButton = keyButton;
            keyButton.setText(localize("settings.controls.wait_for_key"));
            keyButton.setStyle(waitingStyle);
            keyButton.setTextFill(Color.BLACK);  // Чёрный текст для видимости
            app.getAudioManager().playSFXByName("menu_select");
            
            // Фокусируем главный VBox для перехвата клавиш
            this.requestFocus();
        });
        
        // Обработчик правой кнопки мыши для отмены назначения
        keyButton.setOnContextMenuRequested(e -> {
            if (waitingForKeyButton == keyButton) {
                // Отменяем назначение клавиши
                waitingForKeyButton.setText("...");
                waitingForKeyButton.setStyle((String) waitingForKeyButton.getUserData());
                waitingForKeyButton.setTextFill(Color.WHITE);
                waitingForKeyButton = null;
                app.getAudioManager().playSFXByName("menu_back");
            }
        });
        
        box.getChildren().addAll(label, keyButton);
        
        return box;
    }
    
    private Label createLabel(String translationKey) {
        Label label = new Label();
        bindText(label, translationKey);
        label.setFont(Font.font("Orbitron", 14));  // Уменьшили с 16 до 14
        label.setTextFill(Color.web("#E0E0E0")); // Светло-серый текст
        label.setAlignment(Pos.CENTER); // Выравнивание по центру
        return label;
    }
    
    private void bindText(Labeled labeled, String key) {
        if (labeled == null || key == null || key.isBlank()) {
            return;
        }
        labeled.textProperty().unbind();
        javafx.beans.binding.StringBinding binding = localizationManager.bind(labeled, key);
        // КРИТИЧНО: Сохраняем binding для последующего dispose
        if (binding != null) {
            activeBindings.add(binding);
            // System.out.println("[BINDINGS] Total active bindings: " + activeBindings.size());
        }
    }
    


    private String localize(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        return localizationManager.getOrDefault(key, key);
    }

    private void bindPromptText(TextInputControl input, String key) {
        if (input == null || key == null || key.isBlank()) {
            return;
        }
        input.promptTextProperty().unbind();
        javafx.beans.binding.StringBinding binding = Bindings.createStringBinding(
            () -> localize(key),
            localizationManager.localeProperty()
        );
        input.promptTextProperty().bind(binding);
        // КРИТИЧНО: Сохраняем binding для последующего dispose
        activeBindings.add(binding);
    }

    private Tooltip createLocalizedTooltip(String key) {
        Tooltip tooltip = new Tooltip();
        updateTooltipText(tooltip, key);
        // КРИТИЧНО: Сохраняем listener для последующей очистки
        javafx.beans.value.ChangeListener<java.util.Locale> listener = (obs, oldLocale, newLocale) -> updateTooltipText(tooltip, key);
        localizationManager.localeProperty().addListener(listener);
        synchronized (tooltipListeners) {
            tooltipListeners.add(listener);
        }
        return tooltip;
    }

    private void updateTooltipText(Tooltip tooltip, String key) {
        if (tooltip != null) {
            tooltip.setText(localize(key));
        }
    }

    private ListCell<WindowModeOption> createWindowModeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(WindowModeOption item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();
                if (empty || item == null) {
                    setText(null);
                } else {
                    bindText(this, item.translationKey());
                }
            }
        };
    }

    private ListCell<LanguageOption> createLanguageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(LanguageOption item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();
                if (empty || item == null) {
                    setText(null);
                } else {
                    bindText(this, item.getTranslationKey());
                }
            }
        };
    }
    
    private ListCell<com.arcadeblocks.config.DifficultyLevel> createDifficultyCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(com.arcadeblocks.config.DifficultyLevel item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Используем getDisplayName(), который уже использует локализацию
                    textProperty().bind(Bindings.createStringBinding(
                        () -> item.getDisplayName(),
                        localizationManager.localeProperty()
                    ));
                }
            }
        };
    }
    
    /**
     * Обновить список доступных разрешений в зависимости от режима окна
     */
    private void updateResolutionComboBox() {
        if (resolutionComboBox == null || windowModeComboBox == null) {
            return;
        }
        
        WindowModeOption currentMode = windowModeComboBox.getValue();
        if (currentMode == null) {
            currentMode = WindowModeOption.FULLSCREEN; // По умолчанию fullscreen
        }
        
        boolean isFullscreen = currentMode.isFullscreen();
        com.arcadeblocks.config.Resolution currentResolution = resolutionComboBox.getValue();
        
        // Получаем доступные разрешения для текущего режима
        var availableResolutions = GameConfig.getAvailableResolutions(isFullscreen);
        
        // Сохраняем текущее выбранное разрешение
        com.arcadeblocks.config.Resolution selectedResolution = currentResolution != null ? currentResolution : GameConfig.DEFAULT_RESOLUTION;
        
        // Если текущее разрешение недоступно для нового режима, выбираем разрешение по умолчанию
        boolean isCurrentResolutionAvailable = false;
        for (com.arcadeblocks.config.Resolution res : availableResolutions) {
            if (res.equals(selectedResolution)) {
                isCurrentResolutionAvailable = true;
                break;
            }
        }
        
        if (!isCurrentResolutionAvailable) {
            // Если текущее разрешение недоступно (например, 1600x900 в fullscreen), выбираем 1920x1080
            selectedResolution = GameConfig.RESOLUTION_1920x1080;
        }
        
        // Обновляем список разрешений
        boolean previousLoading = isLoading;
        isLoading = true; // Предотвращаем сохранение при обновлении списка
        resolutionComboBox.getItems().clear();
        resolutionComboBox.getItems().addAll(availableResolutions);
        resolutionComboBox.setValue(selectedResolution);
        isLoading = previousLoading;
    }

    private enum LanguageOption {
        ENGLISH("en", "language.button.english"),
        RUSSIAN("ru", "language.button.russian");

        private final String code;
        private final String translationKey;

        LanguageOption(String code, String translationKey) {
            this.code = code;
            this.translationKey = translationKey;
        }

        public String getCode() {
            return code;
        }

        public String getTranslationKey() {
            return translationKey;
        }
        
        public static LanguageOption fromCode(String code) {
            for (LanguageOption option : values()) {
                if (option.code.equalsIgnoreCase(code)) {
                    return option;
                }
            }
            return ENGLISH; // Default
        }
    }

    private enum WindowModeOption {
        FULLSCREEN("settings.window.fullscreen", true, false),
        WINDOWED("settings.window.windowed", false, true);

        private final String translationKey;
        private final boolean fullscreen;
        private final boolean windowed;

        WindowModeOption(String translationKey, boolean fullscreen, boolean windowed) {
            this.translationKey = translationKey;
            this.fullscreen = fullscreen;
            this.windowed = windowed;
        }

        public String translationKey() {
            return translationKey;
        }

        public boolean isFullscreen() {
            return fullscreen;
        }

        public boolean isWindowed() {
            return windowed;
        }

        public static WindowModeOption fromSettings(boolean fullscreen, boolean windowed) {
            if (windowed) {
                return WINDOWED;
            }
            return FULLSCREEN;
        }
    }

    private void setupKeyBindingHandler() {
        this.setFocusTraversable(true);
        
        keyEventFilter = event -> {
            if (waitingForKeyButton != null) {
                // Если ожидаем назначение клавиши, обрабатываем как обычно
                // ESC теперь можно назначить как обычную клавишу
                
                // Проверяем, не является ли это отменой назначения
                if (event.getCode() == KeyCode.ENTER) {
                    // ENTER отменяет назначение клавиши
                    waitingForKeyButton.setText("...");
                    waitingForKeyButton.setStyle((String) waitingForKeyButton.getUserData());
                    waitingForKeyButton.setTextFill(Color.WHITE);
                    waitingForKeyButton = null;
                    app.getAudioManager().playSFXByName("menu_back");
                    event.consume();
                    return;
                }
                
                // Получаем название клавиши
                String keyName = event.getCode().toString();
                
                // Проверка на конфликты с другими кнопками
                Button conflictButton = findConflictingButton(keyName);
                if (conflictButton != null && conflictButton != waitingForKeyButton) {
                    // Если найден конфликт, сбрасываем конфликтную кнопку
                    conflictButton.setText("...");
                }
                
                // Устанавливаем клавишу в кнопку
                waitingForKeyButton.setText(keyName);
                waitingForKeyButton.setStyle((String) waitingForKeyButton.getUserData());
                waitingForKeyButton.setTextFill(Color.WHITE);  // Возвращаем белый цвет текста
                
                // Обновляем трекер изменений
                updateChangeTracker("control_key", keyName);
                
                // Автоматическое сохранение настроек управления (только если не идет загрузка)
                if (!isLoading) {
                    // Сохраняем клавишу напрямую
                    String action = getControlKeyAction(waitingForKeyButton);
                    if (action != null) {
                        app.getSaveManager().setControlKey(action, keyName);
                        // Сохраняем все настройки (включая эту клавишу)
                        saveSettings();
                        // Ждём завершения всех асинхронных операций и перезагружаем привязки
                        // КРИТИЧНО: Сохраняем ссылку на app для проверки в callback
                        WeakReference<SettingsView> viewRef = new WeakReference<>(this);
                        WeakReference<ArcadeBlocksApp> appRef = new WeakReference<>(app);
                        // КРИТИЧНО: захватываем строковые значения локально, чтобы не удерживать кнопки
                        final String actionCaptured = action;
                        final String keyNameCaptured = keyName;
                        app.getSaveManager().runAfterPendingWrites(() -> {
                            SettingsView view = viewRef.get();
                            ArcadeBlocksApp appStrong = appRef.get();
                            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен и приложение доступно
                            if (view == null || view.isCleanedUp || appStrong == null) {
                                return;
                            }
                            // Дополнительно ждём завершения операций для гарантии
                            appStrong.getSaveManager().awaitPendingWrites();
                            // Перезагружаем привязки, передавая новую клавишу напрямую для немедленного применения
                            appStrong.reloadInputBindings(actionCaptured, keyNameCaptured);
                        });
                    }
                }
                
                // Воспроизводим звук
                app.getAudioManager().playSFXByName("settings_change");
                
                // Сбрасываем режим ожидания
                waitingForKeyButton = null;
                
                // Потребляем событие, чтобы оно не обрабатывалось дальше
                event.consume();
            } else {
                // Если не ожидаем назначение клавиши, обрабатываем навигацию
                KeyCode leftKey = getBoundKeyCode("MOVE_LEFT");
                KeyCode rightKey = getBoundKeyCode("MOVE_RIGHT");
                if (leftKey != null && event.getCode() == leftKey) {
                    navigateControlButtons(-1);
                    event.consume();
                    return;
                } else if (rightKey != null && event.getCode() == rightKey) {
                    navigateControlButtons(1);
                    event.consume();
                    return;
                }

                switch (event.getCode()) {
                    case ESCAPE:
                        // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
                        if (isClosing) {
                            event.consume();
                            break;
                        }
                        // Выход из настроек
                        app.getAudioManager().playSFXByName("menu_back");
                        if (app.isInPauseSettings()) {
                            // Возвращаемся в паузу
                            handleBackFromPauseSettings();
                        }
                        else {
                            // Возвращаемся в главное меню
                            handleBackFromMainMenuSettings();
                        }
                        event.consume();
                        break;
                    default:
                        // Блокируем все остальные клавиши, чтобы они не активировали главное меню
                        event.consume();
                        break;
                }
            }
        };
        this.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyEventFilter);
    }
    
    private Button findConflictingButton(String keyName) {
        // Проверяем все кнопки управления на конфликт
        Button[] allButtons = {
            moveLeftButton, moveRightButton, launchButton, callBallButton, turboPaddleButton, turboBallButton, plasmaWeaponButton, pauseButton
        };
        
        for (Button button : allButtons) {
            if (button != null && button.getText().equals(keyName)) {
                return button;
            }
        }
        
        return null;
    }

    private KeyCode getBoundKeyCode(String action) {
        if (app == null || app.getSaveManager() == null || action == null) {
            return null;
        }
        String keyName = app.getSaveManager().getControlKey(action);
        if (keyName == null || keyName.isBlank()) {
            return null;
        }
        try {
            return KeyCode.valueOf(keyName.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void navigateControlButtons(int delta) {
        if (delta == 0) {
            return;
        }
        java.util.List<Button> controls = new java.util.ArrayList<>();
        if (moveLeftButton != null) controls.add(moveLeftButton);
        if (moveRightButton != null) controls.add(moveRightButton);
        if (launchButton != null) controls.add(launchButton);
        if (callBallButton != null) controls.add(callBallButton);
        if (turboPaddleButton != null) controls.add(turboPaddleButton);
        if (turboBallButton != null) controls.add(turboBallButton);
        if (plasmaWeaponButton != null) controls.add(plasmaWeaponButton);
        if (pauseButton != null) controls.add(pauseButton);
        if (controls.isEmpty()) {
            return;
        }

        int currentIndex = 0;
        if (this.getScene() != null && this.getScene().getFocusOwner() != null) {
            currentIndex = controls.indexOf(this.getScene().getFocusOwner());
            if (currentIndex < 0) {
                currentIndex = 0;
            }
        }

        int nextIndex = Math.min(controls.size() - 1, Math.max(0, currentIndex + delta));
        Button target = controls.get(nextIndex);
        target.requestFocus();
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().playSFX("sounds/menu_hover.wav");
        }
    }
    
    /**
     * Определить действие управления по кнопке
     */
    private String getControlKeyAction(Button button) {
        if (button == moveLeftButton) {
            return "MOVE_LEFT";
        } else if (button == moveRightButton) {
            return "MOVE_RIGHT";
        } else if (button == launchButton) {
            return "LAUNCH";
        } else if (button == callBallButton) {
            return "CALL_BALL";
        } else if (button == turboPaddleButton) {
            return "TURBO_PADDLE";
        } else if (button == turboBallButton) {
            return "TURBO_BALL";
        } else if (button == plasmaWeaponButton) {
            return "PLASMA_WEAPON";
        } else if (button == pauseButton) {
            return "PAUSE";
        }
        return null;
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button resetButton = createButton("settings.button.reset", GameConfig.NEON_ORANGE, () -> {
            app.getAudioManager().playSFXByName("settings_change");
            showResetConfirmation();
        });
        
        // Блокируем кнопку сброса, если мы в настройках паузы
        if (app.isInPauseSettings()) {
            resetButton.setDisable(true);
            resetButton.setOpacity(0.5);
        }
        
        Button backButton = createButton("settings.button.back", GameConfig.NEON_CYAN, () -> {
            // КРИТИЧНО: Предотвращаем повторные нажатия во время анимации закрытия
            if (isClosing) {
                return;
            }
            app.getAudioManager().playSFXByName("menu_back");
            if (app.isInPauseSettings()) {
                // Возвращаемся в паузу
                handleBackFromPauseSettings();
            } else {
                // Возвращаемся в главное меню
                handleBackFromMainMenuSettings();
            }
        });
        
        buttonBox.getChildren().addAll(resetButton, backButton);
        
        return buttonBox;
    }
    
    private Button createButton(String translationKey, String color, Runnable action) {
        Button button = new Button();
        bindText(button, translationKey);
        button.setPrefSize(120, 40);
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
            // КРИТИЧНО: Проверяем что view не очищен
            if (buttonScaleTransitions == null || app == null) {
                return;
            }
            
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
                scaleUp.stop();
                if (buttonScaleTransitions != null) {
                    buttonScaleTransitions.remove(button);
                }
            });
            buttonScaleTransitions.put(button, scaleUp);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // КРИТИЧНО: Проверяем что view не очищен (cleanup() может обнулить buttonScaleTransitions)
            if (buttonScaleTransitions == null) {
                return;
            }
            
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
                scaleDown.stop();
                if (buttonScaleTransitions != null) {
                    buttonScaleTransitions.remove(button);
                }
            });
            buttonScaleTransitions.put(button, scaleDown);
            scaleDown.play();
        });
        button.setOnAction(e -> action.run());
        
        return button;
    }

    private void playSettingsToggleSound() {
        // System.out.println("[SettingsView] playSettingsToggleSound called");
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().playSFXByName("settings_change");
        }
    }
    
    /**
     * Обновить подписи всех слайдеров
     */
    private void updateSliderLabels() {
        // Обновляем подписи аудио слайдеров
        updateAudioSliderLabel(masterVolumeSlider);
        updateAudioSliderLabel(musicVolumeSlider);
        updateAudioSliderLabel(sfxVolumeSlider);
        
        // Обновляем подпись слайдера скорости ракетки
        updateSliderLabel(paddleSpeedSlider);
        updateSliderLabel(turboSpeedSlider);
    }
    
    /**
     * Обновить подпись конкретного аудио слайдера
     */
    private void updateAudioSliderLabel(Slider slider) {
        VBox parent = (VBox) slider.getParent();
        Label valueLabel = (Label) parent.getChildren().get(2); // Третий элемент - это подпись
        valueLabel.setText(String.valueOf((int) slider.getValue()));
    }
    
    /**
     * Обновить подпись обычного слайдера
     */
    private void updateSliderLabel(Slider slider) {
        VBox parent = (VBox) slider.getParent();
        Label valueLabel = (Label) parent.getChildren().get(2); // Третий элемент - это подпись
        if (slider == turboSpeedSlider) {
            valueLabel.setText(String.format("%.1fx", slider.getValue() / 100.0));
        } else {
            valueLabel.setText(String.valueOf((int) slider.getValue()));
        }
    }

    private boolean canEditPlayerName() {
        return !app.isInPauseSettings() && !app.isDebugMode();
    }
    
    private void loadSettings() {
        isLoading = true; // Устанавливаем флаг загрузки
        
        // Загрузка настроек аудио
        double masterVolume = app.getSaveManager().getMasterVolume() * 100;
        double musicVolume = app.getSaveManager().getMusicVolume() * 100;
        double sfxVolume = app.getSaveManager().getSfxVolume() * 100;
        
        masterVolumeSlider.setValue(masterVolume);
        musicVolumeSlider.setValue(musicVolume);
        sfxVolumeSlider.setValue(sfxVolume);
        
        // Обновляем подписи слайдеров
        updateSliderLabels();
        
        
        // Загрузка настроек управления (теперь в кнопки)
        moveLeftButton.setText(app.getSaveManager().getControlKey("MOVE_LEFT"));
        moveRightButton.setText(app.getSaveManager().getControlKey("MOVE_RIGHT"));
        launchButton.setText(app.getSaveManager().getControlKey("LAUNCH"));
        callBallButton.setText(app.getSaveManager().getControlKey("CALL_BALL"));
        turboPaddleButton.setText(app.getSaveManager().getControlKey("TURBO_PADDLE"));
        turboBallButton.setText(app.getSaveManager().getControlKey("TURBO_BALL"));
        plasmaWeaponButton.setText(app.getSaveManager().getControlKey("PLASMA_WEAPON"));
        pauseButton.setText(app.getSaveManager().getControlKey("PAUSE"));
        
        // Загрузка игровых настроек
        String currentLang = app.getSaveManager().getLanguage();
        languageComboBox.setValue(LanguageOption.fromCode(currentLang));
        
        playerNameField.setText(app.getSaveManager().getPlayerName());
        paddleSpeedSlider.setValue(app.getSaveManager().getPaddleSpeed());
        turboSpeedSlider.setValue(app.getSaveManager().getTurboModeSpeed() * 100);
        
        // Загрузка режима окна
        boolean isFullscreen = app.getSaveManager().isFullscreen();
        boolean isWindowed = app.getSaveManager().isWindowedMode();
        windowModeComboBox.setValue(WindowModeOption.fromSettings(isFullscreen, isWindowed));
        
        // Обновляем список разрешений в зависимости от режима окна
        updateResolutionComboBox();
        
        // Загрузка разрешения
        com.arcadeblocks.config.Resolution savedResolution = app.getSaveManager().getResolution();
        if (savedResolution != null) {
            // Проверяем, доступно ли сохраненное разрешение для текущего режима окна
            boolean isResolutionAvailable = false;
            for (com.arcadeblocks.config.Resolution res : resolutionComboBox.getItems()) {
                if (res.equals(savedResolution)) {
                    isResolutionAvailable = true;
                    break;
                }
            }
            if (isResolutionAvailable) {
                resolutionComboBox.setValue(savedResolution);
            } else {
                // Если сохраненное разрешение недоступно (например, 1600x900 в fullscreen), используем по умолчанию
                resolutionComboBox.setValue(GameConfig.DEFAULT_RESOLUTION);
            }
        } else {
            resolutionComboBox.setValue(GameConfig.DEFAULT_RESOLUTION);
        }
        
        // Загрузка сложности: в паузе отображаем сложность активного сохранения, иначе - глобальную
        if (app.isInPauseSettings()) {
            com.arcadeblocks.config.DifficultyLevel slotDifficulty = app.getEffectiveDifficulty();
            if (slotDifficulty == null) {
                slotDifficulty = app.getSaveManager().getDifficulty();
            }
            difficultyComboBox.setValue(slotDifficulty);
        } else {
            difficultyComboBox.setValue(app.getSaveManager().getDifficulty());
        }

        levelBackgroundCheckBox.setSelected(app.getSaveManager().isLevelBackgroundEnabled());
        vsyncCheckBox.setSelected(app.getSaveManager().isVSyncEnabled());
        
        // Флаг isLoading сбрасывается в initializeUI() после завершения загрузки
    }
    
    private void saveSettings() {
        // Сохранение настроек аудио
        app.getSaveManager().setMasterVolume(masterVolumeSlider.getValue() / 100.0);
        app.getSaveManager().setMusicVolume(musicVolumeSlider.getValue() / 100.0);
        app.getSaveManager().setSfxVolume(sfxVolumeSlider.getValue() / 100.0);
        
        // Сохранение настроек управления (теперь из кнопок)
        app.getSaveManager().setControlKey("MOVE_LEFT", moveLeftButton.getText());
        app.getSaveManager().setControlKey("MOVE_RIGHT", moveRightButton.getText());
        app.getSaveManager().setControlKey("LAUNCH", launchButton.getText());
        app.getSaveManager().setControlKey("CALL_BALL", callBallButton.getText());
        app.getSaveManager().setControlKey("TURBO_PADDLE", turboPaddleButton.getText());
        app.getSaveManager().setControlKey("TURBO_BALL", turboBallButton.getText());
        app.getSaveManager().setControlKey("PLASMA_WEAPON", plasmaWeaponButton.getText());
        app.getSaveManager().setControlKey("PAUSE", pauseButton.getText());
        
        // Сохранение игровых настроек
        if (languageComboBox.getValue() != null) {
            app.getSaveManager().setLanguage(languageComboBox.getValue().getCode());
        }

        if (canEditPlayerName()) {
            app.getSaveManager().setPlayerName(playerNameField.getText());
        }
        app.getSaveManager().setPaddleSpeed(paddleSpeedSlider.getValue());
        app.getSaveManager().setTurboModeSpeed(turboSpeedSlider.getValue() / 100.0);
        
        // КРИТИЧНО: Сохраняем сложность только если не в паузе (в паузе сложность нельзя менять)
        if (!app.isInPauseSettings() && difficultyComboBox.getValue() != null) {
            app.getSaveManager().setDifficulty(difficultyComboBox.getValue());
        }
        
        app.getSaveManager().setLevelBackgroundEnabled(levelBackgroundCheckBox.isSelected());
        app.getSaveManager().setVSyncEnabled(vsyncCheckBox.isSelected());
        updateChangeTracker("level_background_enabled", levelBackgroundCheckBox.isSelected());
        updateChangeTracker("vsync_enabled", vsyncCheckBox.isSelected());

        // Режим окна сохраняется автоматически при изменении ComboBox

        // Применение настроек аудио
        app.getAudioManager().setMasterVolume(masterVolumeSlider.getValue() / 100.0);
        app.getAudioManager().setMusicVolume(musicVolumeSlider.getValue() / 100.0);
        app.getAudioManager().setSfxVolume(sfxVolumeSlider.getValue() / 100.0);

        // КРИТИЧНО: НЕ применяем FPS лимит во время работы игры - вызывает фризы!
        // Настройка будет применена после перезапуска
        
        // Сохранение в файл
        app.getSaveManager().saveSettings();

        markChangesSaved();
    }
    
    /**
     * Сохранить все настройки при выходе из паузы (вызывается один раз)
     */
    public void saveAllPauseSettings() {
        if (app == null || isCleanedUp) {
            return;
        }
        
        // Сохраняем все настройки в базу данных одним пакетом
        app.getSaveManager().setMasterVolume(masterVolumeSlider.getValue() / 100.0);
        app.getSaveManager().setMusicVolume(musicVolumeSlider.getValue() / 100.0);
        app.getSaveManager().setSfxVolume(sfxVolumeSlider.getValue() / 100.0);
        app.getSaveManager().setPaddleSpeed(paddleSpeedSlider.getValue());
        app.getSaveManager().setTurboModeSpeed(turboSpeedSlider.getValue() / 100.0);
        app.getSaveManager().setLevelBackgroundEnabled(levelBackgroundCheckBox.isSelected());
        app.getSaveManager().setVSyncEnabled(vsyncCheckBox.isSelected());
        
        // Сохранение настроек управления
        app.getSaveManager().setControlKey("MOVE_LEFT", moveLeftButton.getText());
        app.getSaveManager().setControlKey("MOVE_RIGHT", moveRightButton.getText());
        app.getSaveManager().setControlKey("LAUNCH", launchButton.getText());
        app.getSaveManager().setControlKey("CALL_BALL", callBallButton.getText());
        app.getSaveManager().setControlKey("TURBO_PADDLE", turboPaddleButton.getText());
        app.getSaveManager().setControlKey("TURBO_BALL", turboBallButton.getText());
        app.getSaveManager().setControlKey("PLASMA_WEAPON", plasmaWeaponButton.getText());
        app.getSaveManager().setControlKey("PAUSE", pauseButton.getText());
        
        // Сохранение в файл
        app.getSaveManager().saveSettings();

        markChangesSaved();
    }

    private void markChangesSaved() {
        if (changeTracker != null) {
            changeTracker.resetChanges();
        }
    }
    
    private void resetToDefaults() {
        // Сброс к настройкам по умолчанию
        masterVolumeSlider.setValue(GameConfig.DEFAULT_VOLUME * 100);
        musicVolumeSlider.setValue(GameConfig.DEFAULT_VOLUME * 100);
        sfxVolumeSlider.setValue(GameConfig.DEFAULT_VOLUME * 100);
        
        // Обновляем подписи слайдеров
        updateSliderLabels();
        
        
        // Сброс кнопок управления к значениям по умолчанию
        isLoading = true; // Устанавливаем флаг загрузки, чтобы не вызывать saveSettings() при каждом setText()
        moveLeftButton.setText(GameConfig.DEFAULT_CONTROLS.get("MOVE_LEFT"));
        moveRightButton.setText(GameConfig.DEFAULT_CONTROLS.get("MOVE_RIGHT"));
        launchButton.setText(GameConfig.DEFAULT_CONTROLS.get("LAUNCH"));
        callBallButton.setText(GameConfig.DEFAULT_CONTROLS.get("CALL_BALL"));
        turboPaddleButton.setText(GameConfig.DEFAULT_CONTROLS.get("TURBO_PADDLE"));
        turboBallButton.setText(GameConfig.DEFAULT_CONTROLS.get("TURBO_BALL"));
        plasmaWeaponButton.setText(GameConfig.DEFAULT_CONTROLS.get("PLASMA_WEAPON"));
        pauseButton.setText(GameConfig.DEFAULT_CONTROLS.get("PAUSE"));
        levelBackgroundCheckBox.setSelected(true);
        isLoading = false; // Сбрасываем флаг загрузки

        // Сохраняем настройки управления в базу данных
        app.getSaveManager().setControlKey("MOVE_LEFT", GameConfig.DEFAULT_CONTROLS.get("MOVE_LEFT"));
        app.getSaveManager().setControlKey("MOVE_RIGHT", GameConfig.DEFAULT_CONTROLS.get("MOVE_RIGHT"));
        app.getSaveManager().setControlKey("LAUNCH", GameConfig.DEFAULT_CONTROLS.get("LAUNCH"));
        app.getSaveManager().setControlKey("CALL_BALL", GameConfig.DEFAULT_CONTROLS.get("CALL_BALL"));
        app.getSaveManager().setControlKey("TURBO_PADDLE", GameConfig.DEFAULT_CONTROLS.get("TURBO_PADDLE"));
        app.getSaveManager().setControlKey("TURBO_BALL", GameConfig.DEFAULT_CONTROLS.get("TURBO_BALL"));
        app.getSaveManager().setControlKey("PLASMA_WEAPON", GameConfig.DEFAULT_CONTROLS.get("PLASMA_WEAPON"));
        app.getSaveManager().setControlKey("PAUSE", GameConfig.DEFAULT_CONTROLS.get("PAUSE"));

        if (canEditPlayerName()) {
            playerNameField.setText(localize("player.default"));
        }
        paddleSpeedSlider.setValue(GameConfig.PADDLE_SPEED);
        turboSpeedSlider.setValue(150); // 1.5x default
        windowModeComboBox.setValue(WindowModeOption.FULLSCREEN);
        
        // Обновляем список разрешений после установки режима окна
        updateResolutionComboBox();
        
        // Сброс разрешения и сложности (для fullscreen всегда 1920x1080)
        resolutionComboBox.setValue(GameConfig.DEFAULT_RESOLUTION);
        
        // Сохраняем все настройки в файл
        saveSettings();
        
        // Ждём завершения записи и перезагружаем привязки клавиш
        // КРИТИЧНО: Сохраняем ссылку на app для проверки в callback
        WeakReference<SettingsView> viewRef = new WeakReference<>(this);
        WeakReference<ArcadeBlocksApp> appRef = new WeakReference<>(app);
        app.getSaveManager().runAfterPendingWrites(() -> {
            SettingsView view = viewRef.get();
            ArcadeBlocksApp appStrong = appRef.get();
            // КРИТИЧНО: Проверяем, что SettingsView еще не очищен и приложение доступно
            if (view == null || view.isCleanedUp || appStrong == null) {
                return;
            }
            appStrong.getSaveManager().awaitPendingWrites();
            // Перезагружаем привязки клавиш в игре после сохранения
            appStrong.reloadInputBindings();
        });
        
        // Обновляем скорость ракетки в игре
        app.updatePaddleSpeed();
        app.updateTurboSpeed();
        
        app.getAudioManager().playSFXByName("menu_select");
    }
    
    
    /**
     * Обновить трекер изменений при изменении настройки
     */
    private void updateChangeTracker(String key, Object value) {
        // System.out.println("[SettingsView] updateChangeTracker: " + key + " = " + value);
        // System.out.println("[SettingsView] Active bindings: " + activeBindings.size());
        // System.out.println("[SettingsView] Children count: " + getChildren().size());
        changeTracker.updateSetting(key, value);
    }
    
    /**
     * Показать диалог подтверждения сброса настроек
     */
    private void showResetConfirmation() {
        ResetSettingsDialog.show(
            () -> {
                // Подтвержден сброс настроек
                resetToDefaults();
                // КРИТИЧНО: НЕ сбрасываем трекер изменений!
                // Сброс настроек - это тоже изменение, требующее перезапуска игры
                // changeTracker.resetChanges(); - УДАЛЕНО
                
                // Отмечаем, что были изменения (сброс настроек)
                changeTracker.updateSetting("settings_reset", true);
            },
            () -> {
                // Отменен сброс настроек - остаемся в настройках
            }
        );
    }
    
    /**
     * Плавная анимация закрытия окна настроек
     */
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
            
            if (onFinished != null && !isCleanedUp) {
                onFinished.run();
            }
        });
        closeScaleTransition.setOnFinished(e -> {
            // Ничего не делаем, основная логика в closeFadeTransition.onFinished
        });
        
        closeFadeTransition.play();
        closeScaleTransition.play();
    }
    
    /**
     * Обработка возврата из настроек в главное меню
     */
    private void handleBackFromMainMenuSettings() {
        // КРИТИЧНО: Сохраняем ссылку на app ПЕРЕД cleanup(), так как cleanup() обнулит app
        final ArcadeBlocksApp appRef = this.app;
        
        // Проверяем, были ли изменения
        if (changeTracker.hasUnsavedChanges()) {
            // Сохраняем настройки
            saveSettings();
            
            // ВРЕМЕННО ОТКЛЮЧЕНО: Диалог закрытия игры
            // RestartGameDialog.show(() -> {
            //     if (appRef != null) {
            //         appRef.exitGame();
            //     }
            // });
        }
        
        // КРИТИЧНО: Запускаем анимацию закрытия перед возвратом в главное меню
        playCloseAnimation(() -> {
            // Возвращаемся в главное меню после завершения анимации
            // КРИТИЧНО: НЕ вызываем cleanup() и removeUINode() вручную!
            // Пусть appRef.showMainMenu() сам вызовет clearUINodesSafely()
            if (appRef != null) {
                appRef.showMainMenu();
            }
        });
    }
    
    /**
     * Обработка возврата из настроек в паузу
     */
    private void handleBackFromPauseSettings() {
        final ArcadeBlocksApp appRef = this.app;

        if (changeTracker.hasUnsavedChanges()) {
            saveAllPauseSettings();
        }

        // КРИТИЧНО: Запускаем анимацию закрытия перед возвратом в паузу
        playCloseAnimation(() -> {
            if (appRef != null) {
                appRef.setInPauseSettings(false);
                FXGL.getGameScene().removeUINode(this);
                cleanup();
                appRef.showPauseScreen();
            }
        });
    }
}
