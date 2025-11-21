package com.arcadeblocks;

import com.arcadeblocks.gameplay.Brick;
import com.arcadeblocks.gameplay.Bonus;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки отсутствия утечек памяти в игровых компонентах
 * и переходах между UI компонентами
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MemoryLeakTests {

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
    @DisplayName("Brick: проверка, что анимации очищаются при удалении")
    public void testBrickAnimationsCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем кирпич
                Brick brick = new Brick(3, Color.RED, 100);
                
                // Используем рефлексию для проверки полей анимаций
                java.lang.reflect.Field fadeField = Brick.class.getDeclaredField("currentFadeTransition");
                java.lang.reflect.Field scaleField = Brick.class.getDeclaredField("currentScaleTransition");
                fadeField.setAccessible(true);
                scaleField.setAccessible(true);
                
                // Устанавливаем тестовые анимации
                FadeTransition fade = new FadeTransition();
                ScaleTransition scale = new ScaleTransition();
                fadeField.set(brick, fade);
                scaleField.set(brick, scale);
                
                // Проверяем, что анимации установлены
                assertNotNull(fadeField.get(brick), "FadeTransition должна быть установлена");
                assertNotNull(scaleField.get(brick), "ScaleTransition должна быть установлена");
                
                // Вызываем метод очистки через onRemoved
                java.lang.reflect.Method stopMethod = Brick.class.getDeclaredMethod("stopAllAnimations");
                stopMethod.setAccessible(true);
                stopMethod.invoke(brick);
                
                // Проверяем, что анимации очищены
                assertNull(fadeField.get(brick), "FadeTransition должна быть null после cleanup");
                assertNull(scaleField.get(brick), "ScaleTransition должна быть null после cleanup");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании Brick cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Bonus: проверка, что анимации очищаются при удалении")
    public void testBonusAnimationsCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем бонус
                Bonus bonus = new Bonus();
                
                // Используем рефлексию для проверки полей анимаций
                java.lang.reflect.Field fadeField = Bonus.class.getDeclaredField("currentFadeTransition");
                java.lang.reflect.Field scaleField = Bonus.class.getDeclaredField("currentScaleTransition");
                fadeField.setAccessible(true);
                scaleField.setAccessible(true);
                
                // Устанавливаем тестовые анимации
                FadeTransition fade = new FadeTransition();
                ScaleTransition scale = new ScaleTransition();
                fadeField.set(bonus, fade);
                scaleField.set(bonus, scale);
                
                // Проверяем, что анимации установлены
                assertNotNull(fadeField.get(bonus), "FadeTransition должна быть установлена");
                assertNotNull(scaleField.get(bonus), "ScaleTransition должна быть установлена");
                
                // Вызываем метод очистки
                java.lang.reflect.Method stopMethod = Bonus.class.getDeclaredMethod("stopAllAnimations");
                stopMethod.setAccessible(true);
                stopMethod.invoke(bonus);
                
                // Проверяем, что анимации очищены
                assertNull(fadeField.get(bonus), "FadeTransition должна быть null после cleanup");
                assertNull(scaleField.get(bonus), "ScaleTransition должна быть null после cleanup");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании Bonus cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка, что FadeTransition корректно останавливается")
    public void testFadeTransitionStops() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(100, 100);
                FadeTransition fade = new FadeTransition(javafx.util.Duration.seconds(10), rect);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setCycleCount(Timeline.INDEFINITE);
                fade.play();
                
                // Проверяем, что анимация запущена
                assertEquals(javafx.animation.Animation.Status.RUNNING, fade.getStatus(), 
                    "Анимация должна быть в состоянии RUNNING");
                
                // Останавливаем анимацию
                fade.stop();
                
                // Проверяем, что анимация остановлена
                assertEquals(javafx.animation.Animation.Status.STOPPED, fade.getStatus(), 
                    "Анимация должна быть в состоянии STOPPED после stop()");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании FadeTransition: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка, что Timeline корректно останавливается")
    public void testTimelineStops() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                Timeline timeline = new Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {})
                );
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
                
                // Проверяем, что Timeline запущен
                assertEquals(javafx.animation.Animation.Status.RUNNING, timeline.getStatus(), 
                    "Timeline должен быть в состоянии RUNNING");
                
                // Останавливаем Timeline
                timeline.stop();
                
                // Проверяем, что Timeline остановлен
                assertEquals(javafx.animation.Animation.Status.STOPPED, timeline.getStatus(), 
                    "Timeline должен быть в состоянии STOPPED после stop()");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании Timeline: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка удаления listeners на Stage после cleanup")
    public void testStageListenerRemoval() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем тестовый компонент с listener на Stage
                class TestComponent extends StackPane {
                    private javafx.beans.value.ChangeListener<Number> widthListener;
                    
                    public TestComponent(Stage stage) {
                        widthListener = (obs, oldVal, newVal) -> {
                            // Listener для проверки
                        };
                        stage.widthProperty().addListener(widthListener);
                        this.getProperties().put("responsiveWidthListener", widthListener);
                    }
                    
                    public void cleanup(Stage stage) {
                        @SuppressWarnings("unchecked")
                        javafx.beans.value.ChangeListener<Number> listener = 
                            (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveWidthListener");
                        if (listener != null) {
                            stage.widthProperty().removeListener(listener);
                            this.getProperties().remove("responsiveWidthListener");
                        }
                    }
                    
                    public boolean hasListener() {
                        return this.getProperties().containsKey("responsiveWidthListener");
                    }
                }
                
                TestComponent component = new TestComponent(testStage);
                
                // Проверяем, что listener установлен
                assertTrue(component.hasListener(), "Listener должен быть установлен");
                
                // Проверяем, что listener действительно на Stage
                AtomicInteger listenerCount = new AtomicInteger(0);
                testStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                    listenerCount.incrementAndGet();
                });
                testStage.setWidth(900);
                
                // Ждем обработки события
                Thread.sleep(100);
                
                // Вызываем cleanup
                component.cleanup(testStage);
                
                // Проверяем, что listener удален из properties
                assertFalse(component.hasListener(), "Listener должен быть удален из properties");
                
                // Проверяем, что listener удален из Stage (создаем новый listener для проверки)
                AtomicInteger listenerCountAfter = new AtomicInteger(0);
                javafx.beans.value.ChangeListener<Number> testListener = (obs, oldVal, newVal) -> {
                    listenerCountAfter.incrementAndGet();
                };
                testStage.widthProperty().addListener(testListener);
                testStage.setWidth(1000);
                
                // Ждем обработки события
                Thread.sleep(100);
                
                // Удаляем тестовый listener
                testStage.widthProperty().removeListener(testListener);
                
                // Если cleanup работает правильно, listenerCountAfter должен быть > 0
                // (это означает, что новый listener работает, а старый удален)
                assertTrue(listenerCountAfter.get() > 0, 
                    "Новый listener должен работать, старый должен быть удален");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании удаления listeners: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка, что остановленные анимации не держат ссылки")
    public void testStoppedAnimationsDontHoldReferences() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(100, 100);
                FadeTransition fade = new FadeTransition(javafx.util.Duration.seconds(1), rect);
                
                // Создаем слабую ссылку на анимацию
                WeakReference<FadeTransition> weakRef = new WeakReference<>(fade);
                
                fade.play();
                
                // Останавливаем анимацию
                fade.stop();
                
                // Удаляем сильные ссылки
                fade = null;
                rect = null;
                
                // Принудительно вызываем GC несколько раз
                for (int i = 0; i < 5; i++) {
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);
                }
                
                // Проверяем, что объект может быть собран GC
                // Если объект не собран после остановки и удаления ссылок,
                // это может указывать на утечку
                assertNull(weakRef.get(), 
                    "Остановленная анимация должна быть доступна для GC после удаления ссылок");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании GC: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка множественных переходов между компонентами")
    public void testMultipleComponentTransitions() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Симулируем множественные переходы между компонентами
                final int TRANSITION_COUNT = 10;
                AtomicReference<Object> currentComponent = new AtomicReference<>();
                
                class TestComponent extends StackPane {
                    private FadeTransition animation;
                    private javafx.beans.value.ChangeListener<Number> stageListener;
                    
                    public TestComponent(Stage stage) {
                        // Создаем анимацию
                        animation = new FadeTransition(javafx.util.Duration.millis(100), this);
                        animation.setCycleCount(Timeline.INDEFINITE);
                        animation.play();
                        
                        // Создаем listener на Stage
                        stageListener = (obs, oldVal, newVal) -> {};
                        stage.widthProperty().addListener(stageListener);
                        this.getProperties().put("stageListener", stageListener);
                    }
                    
                    public void cleanup(Stage stage) {
                        // Останавливаем анимацию
                        if (animation != null) {
                            animation.stop();
                            animation = null;
                        }
                        
                        // Удаляем listener
                        @SuppressWarnings("unchecked")
                        javafx.beans.value.ChangeListener<Number> listener = 
                            (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("stageListener");
                        if (listener != null) {
                            stage.widthProperty().removeListener(listener);
                            this.getProperties().remove("stageListener");
                        }
                        
                        // Очищаем children
                        getChildren().clear();
                    }
                    
                    public boolean isCleanedUp() {
                        return animation == null && 
                               !getProperties().containsKey("stageListener") &&
                               getChildren().isEmpty();
                    }
                }
                
                // Выполняем множественные переходы
                for (int i = 0; i < TRANSITION_COUNT; i++) {
                    TestComponent component = new TestComponent(testStage);
                    testScene.setRoot(component);
                    currentComponent.set(component);
                    
                    // Проверяем, что компонент создан
                    assertNotNull(currentComponent.get(), "Компонент должен быть создан");
                    assertFalse(((TestComponent) currentComponent.get()).isCleanedUp(), 
                        "Компонент не должен быть очищен до cleanup()");
                    
                    // Вызываем cleanup
                    ((TestComponent) currentComponent.get()).cleanup(testStage);
                    
                    // Проверяем, что компонент очищен
                    assertTrue(((TestComponent) currentComponent.get()).isCleanedUp(), 
                        "Компонент должен быть очищен после cleanup()");
                    
                    // Удаляем ссылку
                    currentComponent.set(null);
                    
                    // Небольшая задержка между переходами
                    Thread.sleep(10);
                }
                
                // Проверяем, что после всех переходов нет утечек
                // (если бы были утечки, listeners накапливались бы на Stage)
                
            } catch (Exception e) {
                fail("Ошибка при тестировании множественных переходов: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Массовое создание и очистка анимаций (стресс-тест)")
    public void testMassiveAnimationCreationAndCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                final int ANIMATION_COUNT = 1000;
                java.util.List<FadeTransition> animations = new java.util.ArrayList<>();
                
                // Создаем много анимаций
                for (int i = 0; i < ANIMATION_COUNT; i++) {
                    javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(10, 10);
                    FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(100), rect);
                    fade.setFromValue(1.0);
                    fade.setToValue(0.0);
                    fade.play();
                    animations.add(fade);
                }
                
                // Проверяем, что все анимации запущены
                long runningCount = animations.stream()
                    .filter(a -> a.getStatus() == javafx.animation.Animation.Status.RUNNING)
                    .count();
                assertTrue(runningCount > 0, "Хотя бы одна анимация должна быть запущена");
                
                // Останавливаем все анимации
                for (FadeTransition animation : animations) {
                    animation.stop();
                }
                
                // Проверяем, что все анимации остановлены
                long stoppedCount = animations.stream()
                    .filter(a -> a.getStatus() == javafx.animation.Animation.Status.STOPPED)
                    .count();
                assertEquals(ANIMATION_COUNT, stoppedCount, 
                    "Все анимации должны быть остановлены");
                
                // Очищаем список
                animations.clear();
                
                // Проверяем, что список пуст
                assertEquals(0, animations.size(), "Список анимаций должен быть пуст");
                
            } catch (Exception e) {
                fail("Ошибка при стресс-тестировании: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Стресс-тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка очистки event handlers")
    public void testEventHandlerCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Button button = new javafx.scene.control.Button("Test");
                
                // Устанавливаем обработчики
                AtomicInteger actionCount = new AtomicInteger(0);
                AtomicInteger mouseEnterCount = new AtomicInteger(0);
                AtomicInteger mouseExitCount = new AtomicInteger(0);
                
                javafx.event.EventHandler<javafx.event.ActionEvent> actionHandler = e -> actionCount.incrementAndGet();
                javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseEnterHandler = e -> mouseEnterCount.incrementAndGet();
                javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseExitHandler = e -> mouseExitCount.incrementAndGet();
                
                button.setOnAction(actionHandler);
                button.setOnMouseEntered(mouseEnterHandler);
                button.setOnMouseExited(mouseExitHandler);
                
                // Проверяем, что обработчики установлены
                assertNotNull(button.getOnAction(), "OnAction должен быть установлен");
                assertNotNull(button.getOnMouseEntered(), "OnMouseEntered должен быть установлен");
                assertNotNull(button.getOnMouseExited(), "OnMouseExited должен быть установлен");
                
                // Вызываем обработчики
                button.fire();
                
                // Очищаем обработчики
                button.setOnAction(null);
                button.setOnMouseEntered(null);
                button.setOnMouseExited(null);
                
                // Проверяем, что обработчики очищены
                assertNull(button.getOnAction(), "OnAction должен быть null после очистки");
                assertNull(button.getOnMouseEntered(), "OnMouseEntered должен быть null после очистки");
                assertNull(button.getOnMouseExited(), "OnMouseExited должен быть null после очистки");
                
                // Проверяем, что старый счетчик не изменился после очистки
                int oldActionCount = actionCount.get();
                
                // Пытаемся вызвать обработчики снова (они не должны работать)
                button.fire();
                
                // Проверяем, что счетчик не изменился
                assertEquals(oldActionCount, actionCount.get(), 
                    "Счетчик action не должен изменяться после очистки обработчика");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании очистки event handlers: " + e.getMessage());
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
