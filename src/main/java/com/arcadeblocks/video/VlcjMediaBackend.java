package com.arcadeblocks.video;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;

/**
 * VLCJ-based video player backend with JavaFX rendering
 */
public class VlcjMediaBackend implements VideoPlayerBackend {
    
    private EmbeddedMediaPlayer player;
    private ImageView imageView;
    private WritableImage image;
    private PixelWriter pixelWriter;
    private java.lang.ref.WeakReference<Runnable> onFinishedRef;
    private java.lang.ref.WeakReference<Runnable> onErrorRef;
    private boolean ready = false;
    private int videoWidth;
    private int videoHeight;
    private final VlcContext vlcContext;
    
    // КРИТИЧНО: Сохраняем ссылки для правильной очистки
    private uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter eventListener;
    private CallbackVideoSurface videoSurface;
    private BufferFormatCallback bufferFormatCallback;
    private RenderCallback renderCallback;
    
    // КРИТИЧНО: Для предотвращения переполнения JavaFX event queue
    // Lock-free подход: AtomicReference для thread-safe доступа без synchronized
    private final java.util.concurrent.atomic.AtomicReference<ByteBuffer> frameToRender = 
        new java.util.concurrent.atomic.AtomicReference<>(null);
    private volatile boolean renderingActive = false;
    private javafx.animation.AnimationTimer renderTimer;
    
    public VlcjMediaBackend() {
        this.vlcContext = VlcContext.getInstance();
        if (!vlcContext.isInitialized()) {
            throw new IllegalStateException("VlcContext must be initialized before creating VlcjMediaBackend");
        }
    }
    
    @Override
    public Node prepareVideo(String videoPath, double width, double height) throws Exception {
        videoWidth = (int) width;
        videoHeight = (int) height;
        
        // Create writable image for rendering
        image = new WritableImage(videoWidth, videoHeight);
        pixelWriter = image.getPixelWriter();
        
        // Create image view
        imageView = new ImageView(image);
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        
        // Get factory and create embedded media player
        MediaPlayerFactory factory = vlcContext.getFactory();
        player = factory.mediaPlayers().newEmbeddedMediaPlayer();
        
        // КРИТИЧНО: Регистрируем player в VlcContext для отслеживания
        vlcContext.registerPlayer(player);
        
        // Setup video surface with callback
        // КРИТИЧНО: Сохраняем ссылки для правильной очистки
        bufferFormatCallback = new BufferFormatCallback() {
            @Override
            public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                return new RV32BufferFormat(videoWidth, videoHeight);
            }
            
            @Override
            public void allocatedBuffers(ByteBuffer[] buffers) {
                // Called when buffers are allocated, can be used for initialization
            }
        };
        
        // КРИТИЧНО: Используем final переменные для lambda, чтобы избежать утечек
        final int finalVideoWidth = videoWidth;
        final int finalVideoHeight = videoHeight;
        
        renderCallback = new RenderCallback() {
            @Override
            public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
                // КРИТИЧНО: Lock-free подход - используем AtomicReference.set() без synchronized
                // VLC может вызывать это 30-60 раз в секунду, поэтому важна максимальная производительность
                if (nativeBuffers != null && nativeBuffers.length > 0 && renderingActive) {
                    frameToRender.set(nativeBuffers[0]); // Thread-safe, lock-free операция
                }
            }
        };
        
        // КРИТИЧНО: AnimationTimer рендерит последний доступный кадр синхронно с JavaFX pulse (60fps)
        // Lock-free: getAndSet(null) атомарно получает кадр и сбрасывает в null за одну операцию
        // Если кадра нет (null) - пропускаем рендеринг, экономя CPU
        renderTimer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                // Атомарно получаем кадр и сбрасываем в null - lock-free операция
                ByteBuffer frame = frameToRender.getAndSet(null);
                if (frame != null && pixelWriter != null) {
                    try {
                        // Рендерим кадр напрямую из VLC буфера без копирования
                        pixelWriter.setPixels(0, 0, finalVideoWidth, finalVideoHeight,
                            PixelFormat.getByteBgraPreInstance(), frame, finalVideoWidth * 4);
                    } catch (Exception e) {
                        // Ignore rendering errors during cleanup
                    }
                }
            }
        };
        
        videoSurface = new CallbackVideoSurface(
            bufferFormatCallback,
            renderCallback,
            true,
            VideoSurfaceAdapters.getVideoSurfaceAdapter()
        );
        
        player.videoSurface().set(videoSurface);
        
        // Setup event listeners
        // КРИТИЧНО: Сохраняем ссылку на listener для удаления
        eventListener = new uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                ready = true;
                // System.out.println("🎬 VLC: Video started playing");
                
                // Получаем длительность после начала воспроизведения
                // long duration = mediaPlayer.media().info().duration();
                // if (duration > 0) {
                //     System.out.println("🎬 VLC: Video duration: " + (duration / 1000.0) + " seconds");
                // }
            }
            
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                // System.out.println("🎬 VLC: Video finished event received");
                Platform.runLater(() -> {
                    Runnable callback = onFinishedRef != null ? onFinishedRef.get() : null;
                    if (callback != null) {
                        // System.out.println("🎬 VLC: Calling onFinished callback");
                        callback.run();
                    }
                });
            }
            
            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                // if (newLength > 0) {
                //     System.out.println("🎬 VLC: Length changed - Video duration: " + (newLength / 1000.0) + " seconds");
                // }
            }
            
            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.err.println("VLC Media error occurred");
                Platform.runLater(() -> {
                    Runnable callback = onErrorRef != null ? onErrorRef.get() : null;
                    if (callback != null) {
                        callback.run();
                    }
                });
            }
        };
        
        // КРИТИЧНО: Регистрируем event listener
        player.events().addMediaPlayerEventListener(eventListener);
        
        // Load video from resources
        try {
            java.net.URL resourceUrl = getClass().getResource("/assets/textures/" + videoPath);
            if (resourceUrl == null) {
                throw new Exception("Video resource not found: " + videoPath);
            }
            
            // Convert URL to file path for VLC
            String fullPath;
            if (resourceUrl.getProtocol().equals("jar")) {
                // Video is in JAR - extract it to temp directory
                System.out.println("🎬 VLC: Extracting video from JAR: " + videoPath);
                VideoResourceExtractor extractor = VideoResourceExtractor.getInstance();
                fullPath = extractor.extractVideo(videoPath);
            } else if (resourceUrl.getProtocol().equals("file")) {
                // Video is a file - convert URL to file path
                try {
                    java.net.URI uri = resourceUrl.toURI();
                    java.io.File file = new java.io.File(uri);
                    fullPath = file.getAbsolutePath();
                    
                    // Verify file exists
                    if (!file.exists()) {
                        throw new Exception("Video file does not exist: " + fullPath);
                    }
                } catch (java.net.URISyntaxException e) {
                    throw new Exception("Invalid file URI: " + resourceUrl, e);
                }
            } else {
                throw new Exception("Unsupported resource protocol: " + resourceUrl.getProtocol());
            }
            
            System.out.println("🎬 VLC loading video from: " + fullPath);
            player.media().play(fullPath);
            player.controls().pause(); // Start paused, will be played when play() is called
            
        } catch (Exception e) {
            cleanup();
            throw new Exception("Failed to load video with VLC: " + e.getMessage(), e);
        }
        
        return imageView;
    }
    
    @Override
    public void play() {
        if (player != null) {
            renderingActive = true;
            // КРИТИЧНО: Запускаем AnimationTimer для рендеринга кадров
            if (renderTimer != null) {
                renderTimer.start();
            }
            player.controls().play();
        }
    }
    
    @Override
    public void pause() {
        if (player != null) {
            player.controls().pause();
            renderingActive = false;
            // КРИТИЧНО: Останавливаем AnimationTimer при паузе
            if (renderTimer != null) {
                renderTimer.stop();
            }
        }
    }
    
    @Override
    public void resume() {
        if (player != null) {
            player.controls().play();
        }
    }
    
    @Override
    public void stop() {
        if (player != null) {
            player.controls().stop();
        }
    }
    
    @Override
    public void setOnFinished(Runnable callback) {
        this.onFinishedRef = callback != null ? new java.lang.ref.WeakReference<>(callback) : null;
    }
    
    @Override
    public void setOnError(Runnable callback) {
        this.onErrorRef = callback != null ? new java.lang.ref.WeakReference<>(callback) : null;
    }
    
    @Override
    public boolean isPlaying() {
        return player != null && player.status().isPlaying();
    }
    
    @Override
    public boolean isReady() {
        return ready;
    }
    
    @Override
    public void setVolume(double volume) {
        if (player != null) {
            // Convert 0.0-1.0 to 0-100 for VLC
            int vlcVolume = (int) (Math.max(0.0, Math.min(1.0, volume)) * 100);
            player.audio().setVolume(vlcVolume);
        }
    }
    
    @Override
    public double getVolume() {
        if (player != null) {
            // Convert VLC 0-100 to 0.0-1.0
            return player.audio().volume() / 100.0;
        }
        return 0.0;
    }
    
    @Override
    public void cleanup() {
        // КРИТИЧНО: Защита от двойной очистки
        if (player == null) {
            // System.out.println("⚠️  VLC media player уже очищен, пропускаем cleanup");
            return;
        }
        
        // System.out.println("🔄 Очистка VLC media player...");
        
        // КРИТИЧНО: Останавливаем рендеринг и AnimationTimer
        renderingActive = false;
        if (renderTimer != null) {
            try {
                renderTimer.stop();
            } catch (Exception e) {
                System.err.println("Error stopping render timer: " + e.getMessage());
            }
            renderTimer = null;
        }
        
        // КРИТИЧНО: Очищаем ссылку на буфер кадра (lock-free)
        frameToRender.set(null);
        
        // КРИТИЧНО: Сначала обнуляем callbacks, чтобы они не вызывались во время cleanup
        onFinishedRef = null;
        onErrorRef = null;
        
        if (player != null) {
            try {
                // КРИТИЧНО: Останавливаем воспроизведение перед очисткой
                player.controls().stop();
                
                // КРИТИЧНО: Удаляем event listener ПЕРЕД release
                if (eventListener != null) {
                    try {
                        player.events().removeMediaPlayerEventListener(eventListener);
                    } catch (Exception e) {
                        System.err.println("Error removing VLC event listener: " + e.getMessage());
                    }
                    eventListener = null;
                }
                
                // КРИТИЧНО: Освобождаем video surface ПЕРЕД release
                if (videoSurface != null) {
                    try {
                        player.videoSurface().set(null);
                    } catch (Exception e) {
                        System.err.println("Error clearing VLC video surface: " + e.getMessage());
                    }
                    videoSurface = null;
                }
                
                // КРИТИЧНО: Освобождаем callbacks
                bufferFormatCallback = null;
                renderCallback = null;
                
                // КРИТИЧНО: Удаляем player из отслеживания в VlcContext
                vlcContext.unregisterPlayer(player);
                
                // КРИТИЧНО: Теперь можно освободить player
                player.release();
            } catch (Exception e) {
                System.err.println("Error cleaning up VLC player: " + e.getMessage());
                // Все равно пытаемся удалить из отслеживания
                try {
                    vlcContext.unregisterPlayer(player);
                } catch (Exception ignored) {}
            }
            player = null;
        }
        
        // КРИТИЧНО: Очищаем JavaFX resources ПОСЛЕ освобождения VLC player
        // Это предотвращает попытки рендеринга после cleanup
        pixelWriter = null;
        
        if (imageView != null) {
            imageView.setImage(null);
            imageView = null;
        }
        
        image = null;
        
        // System.out.println("✅ VLC media player очищен");
    }
    
    @Override
    public String getBackendName() {
        return "VLCJ (" + vlcContext.getFactory().application().version() + ")";
    }
}

