package com.arcadeblocks.ui.util;

import com.almasb.fxgl.dsl.FXGL;
import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Utility for binding UI containers to the current FXGL stage size so that
 * menus, dialogs, and overlays automatically adapt to different resolutions.
 */
public final class ResponsiveLayoutHelper {

    private static final String WIDTH_LISTENER_KEY = "responsiveWidthListener";
    private static final String HEIGHT_LISTENER_KEY = "responsiveHeightListener";
    private static final String SCENE_LISTENER_KEY = "responsiveSceneListener";
    private static final String LAST_WIDTH_KEY = "responsiveLastWidth";
    private static final String LAST_HEIGHT_KEY = "responsiveLastHeight";

    private ResponsiveLayoutHelper() {
    }

    public static void bindToStage(Region region) {
        bindToStage(region, null);
    }

    public static void bindToStage(Region region, BiConsumer<Double, Double> onSizeChanged) {
        if (region == null) {
            return;
        }

        Runnable adjust = () -> adjustRegion(region, onSizeChanged);
        adjust.run();

        Platform.runLater(() -> {
            Stage stage = FXGL.getPrimaryStage();
            if (stage == null) {
                adjust.run();
                return;
            }

            ChangeListener<Number> widthListener = (obs, oldVal, newVal) -> adjust.run();
            ChangeListener<Number> heightListener = (obs, oldVal, newVal) -> adjust.run();

            stage.widthProperty().addListener(widthListener);
            stage.heightProperty().addListener(heightListener);

            region.getProperties().put(WIDTH_LISTENER_KEY, widthListener);
            region.getProperties().put(HEIGHT_LISTENER_KEY, heightListener);

            ChangeListener<javafx.scene.Scene> sceneListener = (obsScene, oldScene, newScene) -> {
                if (newScene == null) {
                    unbind(region);
                }
            };
            
            region.sceneProperty().addListener(sceneListener);
            region.getProperties().put(SCENE_LISTENER_KEY, sceneListener);

            adjust.run();
        });
    }
    
    /**
     * Явная очистка всех слушателей для предотвращения утечек памяти
     * КРИТИЧНО: Делает синхронную очистку, если уже в FX thread, иначе использует Platform.runLater()
     */
    @SuppressWarnings("unchecked")
    public static void unbind(Region region) {
        if (region == null) {
            return;
        }
        
        // КРИТИЧНО: Создаем Runnable с логикой очистки
        Runnable cleanupTask = () -> {
            try {
                Stage stage = FXGL.getPrimaryStage();
                if (stage != null) {
                    ChangeListener<Number> widthListener = 
                        (ChangeListener<Number>) region.getProperties().get(WIDTH_LISTENER_KEY);
                    ChangeListener<Number> heightListener = 
                        (ChangeListener<Number>) region.getProperties().get(HEIGHT_LISTENER_KEY);
                    
                    if (widthListener != null) {
                        stage.widthProperty().removeListener(widthListener);
                    }
                    if (heightListener != null) {
                        stage.heightProperty().removeListener(heightListener);
                    }
                }
                
                ChangeListener<javafx.scene.Scene> sceneListener = 
                    (ChangeListener<javafx.scene.Scene>) region.getProperties().get(SCENE_LISTENER_KEY);
                if (sceneListener != null) {
                    region.sceneProperty().removeListener(sceneListener);
                }
                
                region.getProperties().remove(WIDTH_LISTENER_KEY);
                region.getProperties().remove(HEIGHT_LISTENER_KEY);
                region.getProperties().remove(SCENE_LISTENER_KEY);
                region.getProperties().remove(LAST_WIDTH_KEY);
                region.getProperties().remove(LAST_HEIGHT_KEY);
            } catch (Exception ignored) {
                // Игнорируем ошибки очистки
            }
        };
        
        // КРИТИЧНО: Если мы уже в FX Application Thread, выполняем очистку синхронно
        // Это предотвращает утечки памяти, когда view удаляется до выполнения Platform.runLater()
        if (Platform.isFxApplicationThread()) {
            cleanupTask.run();
        } else {
            // Если не в FX thread, используем Platform.runLater()
            Platform.runLater(cleanupTask);
        }
    }

    private static void adjustRegion(Region region, BiConsumer<Double, Double> onSizeChanged) {
        ArcadeBlocksApp app = (ArcadeBlocksApp) FXGL.getApp();
        double width = Math.max(app.getEffectiveUIWidth(), GameConfig.GAME_WIDTH);
        double height = Math.max(app.getEffectiveUIHeight(), GameConfig.GAME_HEIGHT);
        
        com.arcadeblocks.config.Resolution currentResolution = GameConfig.getCurrentResolution();
        if (currentResolution != null) {
            width = Math.max(width, currentResolution.getWidth());
            height = Math.max(height, currentResolution.getHeight());
        }
        
        width = Math.round(width);
        height = Math.round(height);

        Double lastWidth = (Double) region.getProperties().get(LAST_WIDTH_KEY);
        Double lastHeight = (Double) region.getProperties().get(LAST_HEIGHT_KEY);
        if (lastWidth != null && lastWidth.doubleValue() == width
            && lastHeight != null && lastHeight.doubleValue() == height) {
            if (onSizeChanged != null) {
                onSizeChanged.accept(width, height);
            }
            return;
        }

        region.setPrefSize(width, height);
        region.setMinSize(width, height);
        region.setMaxSize(width, height);
        region.getProperties().put(LAST_WIDTH_KEY, width);
        region.getProperties().put(LAST_HEIGHT_KEY, height);

        if (onSizeChanged != null) {
            onSizeChanged.accept(width, height);
        }
    }
}
