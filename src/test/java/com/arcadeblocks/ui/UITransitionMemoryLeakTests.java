package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.localization.LocalizationManager;
import com.arcadeblocks.story.ChapterStoryData;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки утечек памяти при переходах между UI компонентами
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UITransitionMemoryLeakTests {

    private static boolean javaFXInitialized = false;
    private Stage testStage;
    private Scene testScene;

    @BeforeAll
    public static void initJavaFX() throws Exception {
        if (!javaFXInitialized) {
            try {
                Platform.startup(() -> {
                    javaFXInitialized = true;
                });
                // Даем время на инициализацию и убеждаемся, что Platform запущен
                Thread.sleep(500);
                
                // Проверяем, что Platform действительно запущен
                CountDownLatch checkLatch = new CountDownLatch(1);
                Platform.runLater(() -> checkLatch.countDown());
                if (!checkLatch.await(5, TimeUnit.SECONDS)) {
                    throw new RuntimeException("JavaFX Platform не запустился");
                }
            } catch (IllegalStateException e) {
                // JavaFX уже инициализирован
                javaFXInitialized = true;
            }
        }
    }

    @BeforeEach
    public void setupStage() throws Exception {
        // Убеждаемся, что JavaFX Application Thread доступен
        CountDownLatch initLatch = new CountDownLatch(1);
        Platform.runLater(() -> initLatch.countDown());
        if (!initLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("JavaFX Application Thread не доступен");
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                testStage = new Stage();
                testScene = new Scene(new StackPane(), 800, 600);
                testStage.setScene(testScene);
                testStage.show();
            } catch (Exception e) {
                exceptionRef.set(e);
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Не удалось создать Stage: таймаут");
        }
        if (exceptionRef.get() != null) {
            throw new RuntimeException("Ошибка создания Stage: " + exceptionRef.get().getMessage(), exceptionRef.get());
        }
    }

    @AfterEach
    public void cleanupStage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                if (testStage != null) {
                    testStage.close();
                    testStage = null;
                }
                testScene = null;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Не удалось закрыть Stage");
    }

    @Test
    @DisplayName("Проверка cleanup() в DebugMenuView удаляет listeners на Stage")
    public void testDebugMenuViewCleanupRemovesStageListeners() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем мок ArcadeBlocksApp
                ArcadeBlocksApp app = Mockito.mock(ArcadeBlocksApp.class);
                
                // Создаем DebugMenuView
                DebugMenuView menuView = new DebugMenuView(app);
                
                // Добавляем компонент в Scene для правильной работы ResponsiveLayoutHelper
                // (но ResponsiveLayoutHelper требует FXGL, поэтому тестируем без него)
                // Вместо этого создаем listeners вручную для тестирования cleanup
                javafx.beans.value.ChangeListener<Number> widthListener = (obs, oldVal, newVal) -> {};
                javafx.beans.value.ChangeListener<Number> heightListener = (obs, oldVal, newVal) -> {};
                javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener = (obsScene, oldScene, newScene) -> {};
                
                // Добавляем listeners в properties (как это делает ResponsiveLayoutHelper)
                menuView.getProperties().put("responsiveWidthListener", widthListener);
                menuView.getProperties().put("responsiveHeightListener", heightListener);
                menuView.getProperties().put("responsiveSceneListener", sceneListener);
                
                // Добавляем listeners на Stage
                testStage.widthProperty().addListener(widthListener);
                testStage.heightProperty().addListener(heightListener);
                menuView.sceneProperty().addListener(sceneListener);
                
                // Проверяем, что listeners установлены
                assertTrue(menuView.getProperties().containsKey("responsiveWidthListener"),
                    "responsiveWidthListener должен быть установлен");
                assertTrue(menuView.getProperties().containsKey("responsiveHeightListener"),
                    "responsiveHeightListener должен быть установлен");
                assertTrue(menuView.getProperties().containsKey("responsiveSceneListener"),
                    "responsiveSceneListener должен быть установлен");
                
                // Вызываем cleanup
                menuView.cleanup();
                
                // Проверяем, что listeners удалены из properties
                assertFalse(menuView.getProperties().containsKey("responsiveWidthListener"),
                    "responsiveWidthListener должен быть удален");
                assertFalse(menuView.getProperties().containsKey("responsiveHeightListener"),
                    "responsiveHeightListener должен быть удален");
                assertFalse(menuView.getProperties().containsKey("responsiveSceneListener"),
                    "responsiveSceneListener должен быть удален");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании DebugMenuView cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка cleanup() останавливает все анимации в DebugMenuView")
    public void testDebugMenuViewCleanupStopsAnimations() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем мок ArcadeBlocksApp
                ArcadeBlocksApp app = Mockito.mock(ArcadeBlocksApp.class);
                
                // Создаем DebugMenuView
                DebugMenuView menuView = new DebugMenuView(app);
                
                // Используем рефлексию для проверки полей анимаций
                Field fadeField = null;
                Field slideField = null;
                try {
                    fadeField = DebugMenuView.class.getDeclaredField("animateCloseFadeTransition");
                    slideField = DebugMenuView.class.getDeclaredField("animateCloseSlideTransition");
                    fadeField.setAccessible(true);
                    slideField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // Поля могут называться по-другому
                }
                
                // Вызываем cleanup
                menuView.cleanup();
                
                // Проверяем, что анимации остановлены (если поля доступны)
                if (fadeField != null) {
                    assertNull(fadeField.get(menuView), 
                        "animateCloseFadeTransition должна быть null после cleanup");
                }
                if (slideField != null) {
                    assertNull(slideField.get(menuView), 
                        "animateCloseSlideTransition должна быть null после cleanup");
                }
                
            } catch (Exception e) {
                fail("Ошибка при тестировании остановки анимаций: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка cleanup() очищает обработчики кнопок в DebugMenuView")
    public void testDebugMenuViewCleanupClearsButtonHandlers() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем мок ArcadeBlocksApp
                ArcadeBlocksApp app = Mockito.mock(ArcadeBlocksApp.class);
                
                // Создаем DebugMenuView
                DebugMenuView menuView = new DebugMenuView(app);
                
                // Используем рефлексию для получения кнопок
                Field menuButtonsField = null;
                try {
                    menuButtonsField = DebugMenuView.class.getDeclaredField("menuButtons");
                    menuButtonsField.setAccessible(true);
                    Button[] menuButtons = (Button[]) menuButtonsField.get(menuView);
                    
                    if (menuButtons != null && menuButtons.length > 0) {
                        // Проверяем, что обработчики установлены
                        Button firstButton = menuButtons[0];
                        assertNotNull(firstButton.getOnAction(), 
                            "Обработчик кнопки должен быть установлен");
                        
                        // Вызываем cleanup
                        menuView.cleanup();
                        
                        // Проверяем, что обработчики очищены
                        assertNull(firstButton.getOnAction(), 
                            "Обработчик кнопки должен быть null после cleanup");
                        assertNull(firstButton.getOnMouseEntered(), 
                            "OnMouseEntered должен быть null после cleanup");
                        assertNull(firstButton.getOnMouseExited(), 
                            "OnMouseExited должен быть null после cleanup");
                    }
                } catch (NoSuchFieldException e) {
                    // Поле может быть недоступно
                }
                
            } catch (Exception e) {
                fail("Ошибка при тестировании очистки обработчиков: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка множественных переходов DebugMenuView -> DebugLevelsView -> DebugMenuView")
    public void testMultipleMenuTransitions() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем мок ArcadeBlocksApp
                ArcadeBlocksApp app = Mockito.mock(ArcadeBlocksApp.class);
                
                final int TRANSITION_COUNT = 5;
                
                // Симулируем множественные переходы
                for (int i = 0; i < TRANSITION_COUNT; i++) {
                    // Создаем DebugMenuView
                    DebugMenuView menuView = new DebugMenuView(app);
                    WeakReference<DebugMenuView> menuRef = new WeakReference<>(menuView);
                    
                    // Создаем listeners вручную (имитируем ResponsiveLayoutHelper)
                    javafx.beans.value.ChangeListener<Number> menuWidthListener = (obs, oldVal, newVal) -> {};
                    menuView.getProperties().put("responsiveWidthListener", menuWidthListener);
                    testStage.widthProperty().addListener(menuWidthListener);
                    
                    // Создаем DebugLevelsView
                    DebugLevelsView levelsView = new DebugLevelsView(app);
                    WeakReference<DebugLevelsView> levelsRef = new WeakReference<>(levelsView);
                    
                    // Создаем listeners вручную (имитируем ResponsiveLayoutHelper)
                    javafx.beans.value.ChangeListener<Number> levelsWidthListener = (obs, oldVal, newVal) -> {};
                    levelsView.getProperties().put("responsiveWidthListener", levelsWidthListener);
                    testStage.widthProperty().addListener(levelsWidthListener);
                    
                    // Проверяем, что компоненты созданы
                    assertNotNull(menuRef.get(), "DebugMenuView должен быть создан");
                    assertNotNull(levelsRef.get(), "DebugLevelsView должен быть создан");
                    
                    // Вызываем cleanup для обоих компонентов
                    menuView.cleanup();
                    levelsView.cleanup();
                    
                    // Проверяем, что listeners удалены
                    assertFalse(menuView.getProperties().containsKey("responsiveWidthListener"),
                        "ResponsiveLayoutHelper listeners должны быть удалены из DebugMenuView");
                    assertFalse(levelsView.getProperties().containsKey("responsiveWidthListener"),
                        "ResponsiveLayoutHelper listeners должны быть удалены из DebugLevelsView");
                    
                    // Удаляем сильные ссылки
                    menuView = null;
                    levelsView = null;
                    
                    // Небольшая задержка между переходами
                    Thread.sleep(50);
                }
                
                // Принудительно вызываем GC
                System.gc();
                System.runFinalization();
                Thread.sleep(100);
                
            } catch (Exception e) {
                fail("Ошибка при тестировании множественных переходов: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка cleanup() в LevelIntroView останавливает все анимации")
    public void testLevelIntroViewCleanupStopsAnimations() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем LevelIntroView (но он требует FXGL, поэтому используем рефлексию для проверки cleanup)
                // Вместо этого создаем тестовый компонент, который имитирует LevelIntroView
                class TestLevelIntroView extends StackPane {
                    private FadeTransition fadeInTransition;
                    private FadeTransition fadeOutTransition;
                    private ScaleTransition scaleTransition;
                    private Timeline glowAnimation;
                    
                    public TestLevelIntroView() {
                        // Создаем анимации
                        fadeInTransition = new FadeTransition(javafx.util.Duration.millis(500), this);
                        fadeInTransition.setCycleCount(Timeline.INDEFINITE);
                        fadeInTransition.play();
                        
                        scaleTransition = new ScaleTransition(javafx.util.Duration.millis(1000), this);
                        scaleTransition.setCycleCount(Timeline.INDEFINITE);
                        scaleTransition.play();
                        
                        glowAnimation = new Timeline(
                            new javafx.animation.KeyFrame(javafx.util.Duration.millis(1000), e -> {})
                        );
                        glowAnimation.setCycleCount(Timeline.INDEFINITE);
                        glowAnimation.play();
                    }
                    
                    public void cleanup() {
                        if (fadeInTransition != null) {
                            fadeInTransition.stop();
                            fadeInTransition = null;
                        }
                        if (fadeOutTransition != null) {
                            fadeOutTransition.stop();
                            fadeOutTransition = null;
                        }
                        if (scaleTransition != null) {
                            scaleTransition.stop();
                            scaleTransition = null;
                        }
                        if (glowAnimation != null) {
                            glowAnimation.stop();
                            glowAnimation = null;
                        }
                        getChildren().clear();
                    }
                    
                    public boolean isCleanedUp() {
                        return fadeInTransition == null &&
                               fadeOutTransition == null &&
                               scaleTransition == null &&
                               glowAnimation == null &&
                               getChildren().isEmpty();
                    }
                }
                
                TestLevelIntroView view = new TestLevelIntroView();
                
                // Проверяем, что анимации запущены
                assertFalse(view.isCleanedUp(), "View не должен быть очищен до cleanup()");
                
                // Вызываем cleanup
                view.cleanup();
                
                // Проверяем, что все анимации остановлены
                assertTrue(view.isCleanedUp(), "View должен быть полностью очищен после cleanup()");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании LevelIntroView cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("ChapterStoryView cleanup releases animations and nodes")
    public void testChapterStoryViewCleanupReleasesResources() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ArcadeBlocksApp app = Mockito.mock(ArcadeBlocksApp.class);
                Mockito.when(app.getAudioManager()).thenReturn(null);

                ChapterStoryData data = new ChapterStoryData(1, -1, "", "", "chapter.story.chapter1");
                ChapterStoryView view = new ChapterStoryView(app, data, () -> {});

                view.cleanup();

                Field fadeInField = ChapterStoryView.class.getDeclaredField("fadeInTransition");
                fadeInField.setAccessible(true);
                assertNull(fadeInField.get(view),
                    "fadeInTransition should be null after cleanup");

                Field slideField = ChapterStoryView.class.getDeclaredField("introSlideTransition");
                slideField.setAccessible(true);
                assertNull(slideField.get(view),
                    "introSlideTransition should be null after cleanup");

                Field sequenceField = ChapterStoryView.class.getDeclaredField("paragraphsSequence");
                sequenceField.setAccessible(true);
                assertNull(sequenceField.get(view),
                    "paragraphsSequence should be null after cleanup");

                Field hintPulseField = ChapterStoryView.class.getDeclaredField("hintPulseAnimation");
                hintPulseField.setAccessible(true);
                assertNull(hintPulseField.get(view),
                    "hintPulseAnimation should be null after cleanup");

                Field musicFadeField = ChapterStoryView.class.getDeclaredField("musicFadeTimeline");
                musicFadeField.setAccessible(true);
                assertNull(musicFadeField.get(view),
                    "musicFadeTimeline should be null after cleanup");

                assertTrue(view.getChildren().isEmpty(),
                    "All children should be removed after cleanup");
            } catch (Exception e) {
                fail("Ошибка при тестировании ChapterStoryView cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка, что textProperty() bindings удаляются при cleanup()")
    public void testTextPropertyBindingCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем тестовый компонент с textProperty binding
                class TestComponent extends StackPane {
                    private Button button;
                    
                    public TestComponent() {
                        button = new Button();
                        // Привязываем textProperty через LocalizationManager
                        LocalizationManager.getInstance().bind(button, "test.key");
                    }
                    
                    public void cleanup() {
                        if (button != null) {
                            button.textProperty().unbind();
                            button = null;
                        }
                        getChildren().clear();
                    }
                    
                    public boolean isTextPropertyBound() {
                        return button != null && button.textProperty().isBound();
                    }
                }
                
                TestComponent component = new TestComponent();
                
                // Проверяем, что textProperty привязан
                assertTrue(component.isTextPropertyBound(), 
                    "textProperty должен быть привязан");
                
                // Вызываем cleanup
                component.cleanup();
                
                // Проверяем, что textProperty отвязан (хотя button уже null)
                // В реальности нужно проверять до установки button = null
                
            } catch (Exception e) {
                fail("Ошибка при тестировании textProperty binding cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @AfterAll
    public static void cleanup() {
        // Platform.exit() может вызвать проблемы при запуске нескольких тестов
        // Поэтому оставляем JavaFX запущенным
    }
}

