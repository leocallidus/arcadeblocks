package com.arcadeblocks.video;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * VLC Bootstrap Layer
 * Handles VLCJ initialization, native library discovery, and MediaPlayerFactory management.
 */
public class VlcContext {
    
    private static VlcContext instance;
    private MediaPlayerFactory factory;
    private boolean initialized = false;
    private String errorMessage;
    
    // КРИТИЧНО: Отслеживаем все созданные players для правильной очистки
    private final java.util.Set<uk.co.caprica.vlcj.player.base.MediaPlayer> activePlayers = 
        java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    
    private VlcContext() {
    }
    
    public static synchronized VlcContext getInstance() {
        if (instance == null) {
            instance = new VlcContext();
        }
        return instance;
    }
    
    /**
     * Initialize VLC with automatic native library discovery
     * @return true if initialization succeeded, false otherwise
     */
    public synchronized boolean initialize() {
        return initialize(null);
    }
    
    /**
     * Initialize VLC with custom path to libVLC
     * @param vlcPath custom path to VLC installation (null for automatic discovery)
     * @return true if initialization succeeded, false otherwise
     */
    public synchronized boolean initialize(String vlcPath) {
        if (initialized) {
            return true;
        }
        
        // System.out.println("🎬 Инициализация VLCJ...");
        
        try {
            // Try to discover VLC native libraries
            boolean discovered = false;
            
            if (vlcPath != null && !vlcPath.isEmpty()) {
                // Use custom path if provided
                // System.out.println("  Попытка использовать custom VLC path: " + vlcPath);
                File vlcDir = new File(vlcPath);
                if (vlcDir.exists() && vlcDir.isDirectory()) {
                    System.setProperty("jna.library.path", vlcPath);
                    discovered = new NativeDiscovery().discover();
                }
            } else {
                // Automatic discovery
                // System.out.println("  Автоматический поиск VLC библиотек...");
                discovered = new NativeDiscovery().discover();
            }
            
            if (!discovered) {
                errorMessage = "VLC native libraries not found. Please install VLC media player or provide custom path.";
                System.err.println("❌ " + errorMessage);
                return false;
            }
            
            // Create MediaPlayerFactory with hardware acceleration options
            List<String> options = new ArrayList<>();
            
            // Enable hardware acceleration (platform-specific)
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                options.add("--avcodec-hw=dxva2"); // Windows DirectX Video Acceleration
            } else if (os.contains("mac")) {
                options.add("--avcodec-hw=videotoolbox"); // macOS VideoToolbox
            } else if (os.contains("nux")) {
                options.add("--avcodec-hw=vaapi"); // Linux VA-API
            }
            
            // Other performance options
            options.add("--no-video-title-show"); // Don't show video title on video
            options.add("--no-snapshot-preview"); // Disable snapshot preview
            
            factory = new MediaPlayerFactory(options.toArray(new String[0]));
            initialized = true;
            
            // System.out.println("✅ VLCJ успешно инициализирован");
            // System.out.println("  libVLC версия: " + factory.application().version());
            
            return true;
            
        } catch (UnsatisfiedLinkError e) {
            errorMessage = "Failed to load VLC native libraries: " + e.getMessage();
            System.err.println("❌ " + errorMessage);
            return false;
        } catch (Exception e) {
            errorMessage = "Failed to initialize VLC: " + e.getMessage();
            System.err.println("❌ " + errorMessage);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get the MediaPlayerFactory instance
     * @return MediaPlayerFactory or null if not initialized
     */
    public MediaPlayerFactory getFactory() {
        return factory;
    }
    
    /**
     * Register a MediaPlayer for tracking (called by VlcjMediaBackend)
     * КРИТИЧНО: Для правильной очистки всех players перед dispose()
     */
    void registerPlayer(uk.co.caprica.vlcj.player.base.MediaPlayer player) {
        if (player != null) {
            activePlayers.add(player);
        }
    }
    
    /**
     * Unregister a MediaPlayer (called during cleanup)
     */
    void unregisterPlayer(uk.co.caprica.vlcj.player.base.MediaPlayer player) {
        if (player != null) {
            activePlayers.remove(player);
        }
    }
    
    /**
     * Check if VLC is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get error message if initialization failed
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Dispose VLC resources on application exit
     * CRITICAL: Must be called to prevent memory leaks
     */
    public synchronized void dispose() {
        // System.out.println("🔄 Очистка VLCJ ресурсов...");
        
        // КРИТИЧНО: Сначала освобождаем все активные players ПЕРЕД release() factory
        synchronized (activePlayers) {
            if (!activePlayers.isEmpty()) {
                // System.out.println("  Освобождение " + activePlayers.size() + " активных MediaPlayer...");
                for (uk.co.caprica.vlcj.player.base.MediaPlayer player : new java.util.HashSet<>(activePlayers)) {
                    try {
                        if (player != null) {
                            // Останавливаем воспроизведение
                            if (player instanceof uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer) {
                                ((uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer) player).controls().stop();
                            }
                            // Освобождаем player
                            player.release();
                        }
                    } catch (Exception e) {
                        System.err.println("  Ошибка при освобождении MediaPlayer: " + e.getMessage());
                    }
                }
                activePlayers.clear();
            }
        }
        
        // КРИТИЧНО: Теперь можно освободить factory
        if (factory != null) {
            try {
                factory.release();
                factory = null;
                initialized = false;
                // System.out.println("✅ VLCJ ресурсы очищены");
            } catch (Exception e) {
                System.err.println("❌ Ошибка при очистке VLCJ factory: " + e.getMessage());
            }
        }
    }
}

