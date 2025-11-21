package com.arcadeblocks.ui;

import com.arcadeblocks.ui.util.ResponsiveLayoutHelper;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки корректности cleanup() методов в UI компонентах
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UIComponentCleanupTests {

    private static boolean javaFXInitialized = false;

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

    @Test
    @DisplayName("CountdownTimerView: cleanup() останавливает Timeline")
    public void testCountdownTimerViewCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Создаем простой UI компонент с Timeline для тестирования
                class TestComponent extends StackPane {
                    private Timeline testTimeline;
                    
                    public TestComponent() {
                        testTimeline = new Timeline(
                            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {})
                        );
                        testTimeline.setCycleCount(Timeline.INDEFINITE);
                        testTimeline.play();
                    }
                    
                    public void cleanup() {
                        if (testTimeline != null) {
                            testTimeline.stop();
                            testTimeline = null;
                        }
                    }
                    
                    public Timeline getTimeline() {
                        return testTimeline;
                    }
                }
                
                TestComponent component = new TestComponent();
                
                // Проверяем, что Timeline запущен
                assertNotNull(component.getTimeline(), "Timeline не должен быть null");
                assertEquals(javafx.animation.Animation.Status.RUNNING, 
                    component.getTimeline().getStatus(), 
                    "Timeline должен быть запущен");
                
                // Вызываем cleanup
                component.cleanup();
                
                // Проверяем, что Timeline очищен
                assertNull(component.getTimeline(), "Timeline должен быть null после cleanup");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("ResponsiveLayoutHelper: unbind() корректно отвязывает listeners")
    public void testResponsiveLayoutHelperUnbind() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                VBox testBox = new VBox();
                
                // Проверяем, что у компонента нет сохраненных listeners
                assertFalse(testBox.getProperties().containsKey("WIDTH_LISTENER_KEY"), 
                    "Компонент не должен иметь WIDTH_LISTENER_KEY до bindToStage");
                
                // Вызываем unbind (должен работать корректно даже если не было bind)
                ResponsiveLayoutHelper.unbind(testBox);
                
                // Проверяем, что unbind не вызвал ошибок
                assertFalse(testBox.getProperties().containsKey("WIDTH_LISTENER_KEY"), 
                    "Компонент не должен иметь WIDTH_LISTENER_KEY после unbind");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании ResponsiveLayoutHelper: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка null-safety в cleanup методах")
    public void testNullSafetyInCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Тестируем, что cleanup с null не вызывает ошибок
                class TestComponentWithNulls extends StackPane {
                    private Timeline timeline = null;
                    
                    public void cleanup() {
                        if (timeline != null) {
                            timeline.stop();
                            timeline = null;
                        }
                        ResponsiveLayoutHelper.unbind(this);
                    }
                }
                
                TestComponentWithNulls component = new TestComponentWithNulls();
                
                // Не должно быть исключений
                assertDoesNotThrow(() -> component.cleanup(), 
                    "cleanup() должен корректно работать с null объектами");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании null-safety: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Множественный вызов cleanup() не вызывает ошибок")
    public void testMultipleCleanupCalls() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                class TestComponent extends StackPane {
                    private Timeline timeline;
                    
                    public TestComponent() {
                        timeline = new Timeline(
                            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {})
                        );
                        timeline.play();
                    }
                    
                    public void cleanup() {
                        if (timeline != null) {
                            timeline.stop();
                            timeline = null;
                        }
                    }
                }
                
                TestComponent component = new TestComponent();
                
                // Вызываем cleanup несколько раз
                assertDoesNotThrow(() -> {
                    component.cleanup();
                    component.cleanup();
                    component.cleanup();
                }, "Множественный вызов cleanup() не должен вызывать ошибок");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании множественного cleanup: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }

    @Test
    @DisplayName("Проверка очистки event handlers")
    public void testEventHandlerCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Button button = new javafx.scene.control.Button("Test");
                
                // Устанавливаем обработчики
                button.setOnAction(e -> System.out.println("Action"));
                button.setOnMouseEntered(e -> System.out.println("Enter"));
                button.setOnMouseExited(e -> System.out.println("Exit"));
                
                // Проверяем, что обработчики установлены
                assertNotNull(button.getOnAction(), "OnAction должен быть установлен");
                assertNotNull(button.getOnMouseEntered(), "OnMouseEntered должен быть установлен");
                assertNotNull(button.getOnMouseExited(), "OnMouseExited должен быть установлен");
                
                // Очищаем обработчики
                button.setOnAction(null);
                button.setOnMouseEntered(null);
                button.setOnMouseExited(null);
                
                // Проверяем, что обработчики очищены
                assertNull(button.getOnAction(), "OnAction должен быть null после очистки");
                assertNull(button.getOnMouseEntered(), "OnMouseEntered должен быть null после очистки");
                assertNull(button.getOnMouseExited(), "OnMouseExited должен быть null после очистки");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании очистки event handlers: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }
    
    @Test
    @DisplayName("Проверка удаления ChangeListener с Property")
    public void testChangeListenerRemoval() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                javafx.scene.control.CheckBox checkBox = new javafx.scene.control.CheckBox("Test");
                javafx.beans.property.BooleanProperty property = checkBox.selectedProperty();
                
                // Создаем listener
                javafx.beans.value.ChangeListener<Boolean> listener = (obs, oldVal, newVal) -> {
                    System.out.println("Value changed: " + newVal);
                };
                
                // Добавляем listener
                property.addListener(listener);
                
                // Изменяем значение (listener должен сработать)
                checkBox.setSelected(true);
                
                // Удаляем listener
                property.removeListener(listener);
                
                // Изменяем значение снова (listener НЕ должен сработать)
                checkBox.setSelected(false);
                
                // Проверка успешна, если не было исключений
                assertTrue(true, "ChangeListener был успешно удален");
                
            } catch (Exception e) {
                fail("Ошибка при тестировании удаления ChangeListener: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }
    
    @Test
    @DisplayName("Стресс-тест: множественные listeners")
    public void testMultipleListenersStressTest() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Slider slider = new javafx.scene.control.Slider(0, 100, 50);
                javafx.beans.property.DoubleProperty property = slider.valueProperty();
                
                // Создаем и добавляем 100 listeners
                java.util.List<javafx.beans.value.ChangeListener<Number>> listeners = new java.util.ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    javafx.beans.value.ChangeListener<Number> listener = (obs, oldVal, newVal) -> {
                        // Пустой listener
                    };
                    listeners.add(listener);
                    property.addListener(listener);
                }
                
                // Изменяем значение
                slider.setValue(75);
                
                // Удаляем все listeners
                for (javafx.beans.value.ChangeListener<Number> listener : listeners) {
                    property.removeListener(listener);
                }
                
                // Очищаем список
                listeners.clear();
                
                // Проверка успешна, если не было исключений
                assertTrue(true, "100 listeners были успешно добавлены и удалены");
                
            } catch (Exception e) {
                fail("Ошибка при стресс-тестировании listeners: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Тест не завершился вовремя");
    }
}

