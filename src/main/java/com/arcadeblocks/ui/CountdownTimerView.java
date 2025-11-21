package com.arcadeblocks.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import com.arcadeblocks.ui.util.UINodeCleanup;
import javafx.animation.*;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Компонент таймера обратного отсчета для возобновления игры
 */
public class CountdownTimerView extends StackPane implements SupportsCleanup {
    
    private ArcadeBlocksApp app;
    private Label countdownLabel;
    private Rectangle container;
    private int currentCount = 5;
    private Timeline countdownTimeline;
    private Runnable onCompleteCallback;
    // КРИТИЧНО: Сохраняем ссылки на анимации для их остановки
    private FadeTransition fadeInTransition;
    private FadeTransition fadeOutTransition;
    private SequentialTransition numberAnimationSequence;
    
    public CountdownTimerView(ArcadeBlocksApp app, Runnable onCompleteCallback) {
        this.app = app;
        this.onCompleteCallback = onCompleteCallback;
        
        // Отключаем управление мышью во время таймера
        if (app != null) {
            app.uninstallMousePaddleControlHandlers();
        }
        
        initializeUI();
        startCountdown();
    }
    
    private void initializeUI() {
        // КРИТИЧНО: Используем размеры игрового мира, а не окна
        setPrefSize(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        setAlignment(Pos.CENTER);
        setMouseTransparent(false); // Блокируем мышь для предотвращения управления ракеткой
        // Отключаем ввод с клавиатуры, чтобы ESC/PROBEEL и др. не срабатывали во время таймера
        addEventFilter(javafx.scene.input.InputEvent.ANY, Event::consume);
        
        // Полупрозрачный оверлей
        Rectangle overlay = new Rectangle(GameConfig.GAME_WORLD_WIDTH, GameConfig.GAME_WORLD_HEIGHT);
        overlay.setFill(Color.rgb(0, 0, 0, 0.3));
        
        // Стилизованный полупрозрачный контейнер (как в Level Complete)
        container = new Rectangle(300, 150);
        container.setFill(Color.rgb(15, 15, 28, 0.45));
        container.setStroke(Color.web(GameConfig.NEON_CYAN));
        container.setStrokeWidth(2);
        container.setArcWidth(14);
        container.setArcHeight(14);
        
        // Эффект свечения для контейнера
        DropShadow containerGlow = new DropShadow();
        containerGlow.setColor(Color.web(GameConfig.NEON_CYAN, 0.6));
        containerGlow.setRadius(10);
        containerGlow.setSpread(0.25);
        container.setEffect(containerGlow);
        
        // Лейбл с таймером
        countdownLabel = new Label(String.valueOf(currentCount));
        countdownLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 72));
        countdownLabel.setTextFill(Color.web(GameConfig.NEON_CYAN));
        countdownLabel.setAlignment(Pos.CENTER);
        countdownLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Эффект свечения для текста
        DropShadow textGlow = new DropShadow();
        textGlow.setColor(Color.web(GameConfig.NEON_CYAN, 0.8));
        textGlow.setRadius(15);
        textGlow.setSpread(0.3);
        countdownLabel.setEffect(textGlow);
        
        // Создаем контент контейнера
        StackPane content = new StackPane(countdownLabel);
        content.setPadding(new Insets(20));
        
        // Добавляем все элементы
        getChildren().addAll(overlay, container, content);
        
        // КРИТИЧНО: Привязываем ResponsiveLayoutHelper с кастомным callback для правильного позиционирования
        // относительно игрового мира с учетом letterbox
        ResponsiveLayoutHelper.bindToStage(this, this::adjustLayoutForResolution);
        
        // Начальная анимация появления
        setOpacity(0);
        fadeInTransition = new FadeTransition(Duration.millis(300), this);
        fadeInTransition.setFromValue(0.0);
        fadeInTransition.setToValue(1.0);
        fadeInTransition.setOnFinished(e -> {
            fadeInTransition = null;
        });
        fadeInTransition.play();
    }
    
    /**
     * КРИТИЧНО: Корректировка позиционирования относительно игрового мира с учетом letterbox
     */
    private void adjustLayoutForResolution(double width, double height) {
        // Используем размеры игрового мира
        double gameWorldWidth = GameConfig.GAME_WORLD_WIDTH;
        double gameWorldHeight = GameConfig.GAME_WORLD_HEIGHT;
        
        // Получаем смещения для letterbox
        double offsetX = Math.max(0, GameConfig.getLetterboxOffsetX());
        double offsetY = Math.max(0, GameConfig.getLetterboxOffsetY());
        
        // Устанавливаем размеры на основе игрового мира
        setPrefSize(gameWorldWidth, gameWorldHeight);
        setMinSize(gameWorldWidth, gameWorldHeight);
        setMaxSize(gameWorldWidth, gameWorldHeight);
        
        // КРИТИЧНО: Позиционируем относительно игрового мира с учетом letterbox
        setLayoutX(offsetX);
        setLayoutY(offsetY);
        setTranslateX(0);
        setTranslateY(0);
    }
    
    private void startCountdown() {
        countdownTimeline = new Timeline();
        
        // Создаем ключевые кадры для каждого числа
        for (int i = 5; i >= 1; i--) {
            final int count = i;
            
            // Анимация появления числа
            KeyFrame showFrame = new KeyFrame(
                Duration.seconds(5 - count),
                e -> {
                    currentCount = count;
                    countdownLabel.setText(String.valueOf(count));
                    playNumberAnimation();
                }
            );
            
            countdownTimeline.getKeyFrames().add(showFrame);
        }
        
        // Завершение таймера
        KeyFrame completeFrame = new KeyFrame(
            Duration.seconds(5),
            e -> {
                // КРИТИЧНО: Останавливаем старую анимацию исчезновения, если она еще активна
                if (fadeOutTransition != null) {
                    try {
                        fadeOutTransition.stop();
                    } catch (Exception ignored) {}
                    fadeOutTransition = null;
                }
                
                // Анимация исчезновения
                fadeOutTransition = new FadeTransition(Duration.millis(300), this);
                fadeOutTransition.setFromValue(1.0);
                fadeOutTransition.setToValue(0.0);
                fadeOutTransition.setOnFinished(evt -> {
                    try {
                        // КРИТИЧНО: Сохраняем ссылки перед cleanup(), так как cleanup() обнулит их
                        Runnable callback = onCompleteCallback;
                        ArcadeBlocksApp appRef = app;
                        
                        // КРИТИЧНО: Останавливаем и очищаем анимацию перед cleanup()
                        if (fadeOutTransition != null) {
                            fadeOutTransition.setOnFinished(null);
                            fadeOutTransition.stop();
                            fadeOutTransition = null;
                        }
                        
                        // КРИТИЧНО: Вызываем cleanup() перед удалением для освобождения ресурсов
                        cleanup();
                        
                        // Включаем управление мышью обратно
                        if (appRef != null) {
                            appRef.installMousePaddleControlHandlers();
                        }
                        
                        // КРИТИЧНО: Удаляем компонент из сцены перед вызовом callback
                        FXGL.getGameScene().removeUINode(this);
                        
                        // Вызываем callback ПОСЛЕ удаления из сцены
                        if (callback != null) {
                            callback.run();
                        }
                    } catch (Exception ignored) {}
                });
                fadeOutTransition.play();
            }
        );
        
        countdownTimeline.getKeyFrames().add(completeFrame);
        countdownTimeline.play();
    }
    
    private void playNumberAnimation() {
        // КРИТИЧНО: Останавливаем старую анимацию, если она еще активна
        if (numberAnimationSequence != null) {
            try {
                numberAnimationSequence.stop();
            } catch (Exception ignored) {}
            numberAnimationSequence = null;
        }
        
        // Анимация масштабирования для каждого числа
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), countdownLabel);
        scaleIn.setFromX(0.5);
        scaleIn.setToX(1.2);
        scaleIn.setFromY(0.5);
        scaleIn.setToY(1.2);
        
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), countdownLabel);
        scaleOut.setFromX(1.2);
        scaleOut.setToX(1.0);
        scaleOut.setFromY(1.2);
        scaleOut.setToY(1.0);
        
        // Последовательное воспроизведение анимаций
        numberAnimationSequence = new SequentialTransition(scaleIn, scaleOut);
        numberAnimationSequence.setOnFinished(e -> {
            numberAnimationSequence = null;
        });
        numberAnimationSequence.play();
        
        // Воспроизводим звук (если доступен)
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().playSFXByName("menu_select");
        }
    }
    
    /**
     * Остановить таймер
     */
    public void stop() {
        // КРИТИЧНО: Вызываем cleanup() перед остановкой для освобождения ресурсов
        cleanup();
        
        // Включаем управление мышью обратно при принудительной остановке
        if (app != null) {
            app.installMousePaddleControlHandlers();
        }
    }
    
    /**
     * Полная очистка компонента для предотвращения утечек памяти
     */
    @Override
    public void cleanup() {
        // КРИТИЧНО: Обнуляем callbacks ПЕРВЫМИ, чтобы предотвратить их вызов после cleanup
        onCompleteCallback = null;
        
        // КРИТИЧНО: Останавливаем анимации и очищаем их handlers
        if (fadeInTransition != null) {
            try {
                fadeInTransition.setOnFinished(null);
                fadeInTransition.stop();
            } catch (Exception ignored) {}
            fadeInTransition = null;
        }
        if (fadeOutTransition != null) {
            try {
                fadeOutTransition.setOnFinished(null);
                fadeOutTransition.stop();
            } catch (Exception ignored) {}
            fadeOutTransition = null;
        }
        if (numberAnimationSequence != null) {
            try {
                numberAnimationSequence.setOnFinished(null);
                numberAnimationSequence.stop();
            } catch (Exception ignored) {}
            numberAnimationSequence = null;
        }
        
        if (countdownTimeline != null) {
            try {
                countdownTimeline.stop();
            } catch (Exception ignored) {}
            countdownTimeline = null;
        }
        
        // КРИТИЧНО: Очищаем обработчики событий
        // Примечание: обработчики событий будут автоматически удалены при очистке children
        setOnKeyPressed(null);
        
        // КРИТИЧНО: Освобождаем изображения через UINodeCleanup
        UINodeCleanup.releaseImages(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
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
        
        // КРИТИЧНО: Обнуляем ссылки для предотвращения утечек памяти
        app = null;
        countdownLabel = null;
        container = null;
    }
    
    /**
     * Проверить, активен ли таймер
     */
    public boolean isActive() {
        return countdownTimeline != null && countdownTimeline.getStatus() == Animation.Status.RUNNING;
    }
}
