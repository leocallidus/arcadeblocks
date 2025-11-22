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
     * Извлекает встроенные VLC библиотеки во временную директорию
     * @return путь к директории с библиотеками или null если не удалось
     */
    private String extractEmbeddedVlcLibraries() {
        try {
            // Определяем платформу
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            
            String platformDir;
            if (os.contains("win")) {
                platformDir = arch.contains("aarch64") || arch.contains("arm") 
                    ? "windows-aarch64" : "windows-x64";
            } else if (os.contains("mac")) {
                platformDir = arch.contains("aarch64") || arch.contains("arm") 
                    ? "macos-aarch64" : "macos-x64";
            } else if (os.contains("nux")) {
                platformDir = arch.contains("aarch64") || arch.contains("arm") 
                    ? "linux-aarch64" : "linux-x64";
            } else {
                // System.out.println("Неподдерживаемая платформа: " + os + " " + arch);
                return null;
            }
            
            // Проверяем, есть ли встроенные библиотеки для этой платформы
            String resourcePath = "/natives/" + platformDir + "/";
            
            // Проверяем наличие libvlc.dll/libvlc.so
            String vlcLibName = os.contains("win") ? "libvlc.dll" : "libvlc.so";
            java.io.InputStream testStream = getClass().getResourceAsStream(resourcePath + vlcLibName);
            if (testStream == null) {
                // System.out.println("Встроенные VLC библиотеки не найдены для платформы: " + platformDir);
                return null;
            }
            testStream.close();
            
            // Создаем временную директорию для библиотек
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "arcadeblocks-vlc-" + platformDir);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Список библиотек для извлечения (зависит от платформы)
            List<String> libraries = new ArrayList<>();
            if (os.contains("win")) {
                libraries.add("libvlc.dll");
                libraries.add("libvlccore.dll");
                libraries.add("axvlc.dll");
                libraries.add("npvlc.dll");
                // Дополнительные зависимости
                libraries.add("libgme.dll");
                libraries.add("libogg-0.dll");
                libraries.add("libopus-0.dll");
                libraries.add("libopusfile-0.dll");
                libraries.add("libwavpack-1.dll");
                libraries.add("libxmp.dll");
            } else if (os.contains("nux")) {
                libraries.add("libvlc.so");
                libraries.add("libvlccore.so");
                // На Linux могут быть дополнительные .so файлы
            } else if (os.contains("mac")) {
                libraries.add("libvlc.dylib");
                libraries.add("libvlccore.dylib");
            }
            
            // Извлекаем библиотеки
            int extractedCount = 0;
            for (String libName : libraries) {
                File targetFile = new File(tempDir, libName);
                
                // Пропускаем если файл уже существует и не пустой
                if (targetFile.exists() && targetFile.length() > 0) {
                    extractedCount++;
                    continue;
                }
                
                try (java.io.InputStream in = getClass().getResourceAsStream(resourcePath + libName)) {
                    if (in != null) {
                        try (java.io.FileOutputStream out = new java.io.FileOutputStream(targetFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                        extractedCount++;
                        // System.out.println("Извлечено: " + libName);
                    }
                } catch (Exception e) {
                    // System.out.println("Не удалось извлечь " + libName + ": " + e.getMessage());
                }
            }
            
            if (extractedCount > 0) {
                // System.out.println("Извлечено " + extractedCount + " библиотек в " + tempDir.getAbsolutePath());
                return tempDir.getAbsolutePath();
            } else {
                // System.out.println("Не удалось извлечь ни одной библиотеки");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при извлечении встроенных VLC библиотек: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Initialize VLC with automatic native library discovery
     * @return true if initialization succeeded, false otherwise
     */
    public synchronized boolean initialize() {
        return initialize(null, true);
    }
    
    /**
     * Initialize VLC with automatic native library discovery
     * @param showWarning whether to show warning dialog if VLC is not found
     * @return true if initialization succeeded, false otherwise
     */
    public synchronized boolean initialize(boolean showWarning) {
        return initialize(null, showWarning);
    }
    
    /**
     * Initialize VLC with custom path to libVLC
     * @param vlcPath custom path to VLC installation (null for automatic discovery)
     * @return true if initialization succeeded, false otherwise
     */
    public synchronized boolean initialize(String vlcPath) {
        return initialize(vlcPath, true);
    }
    
    /**
     * Initialize VLC with custom path to libVLC
     * @param vlcPath custom path to VLC installation (null for automatic discovery)
     * @param showWarning whether to show warning dialog if VLC is not found
     * @return true if initialization succeeded, false otherwise
     */
    public synchronized boolean initialize(String vlcPath, boolean showWarning) {
        if (initialized) {
            return true;
        }
        
        // System.out.println("🎬 Инициализация VLCJ...");
        
        try {
            // Try to discover VLC native libraries
            boolean discovered = false;
            String embeddedLibPath = null;
            
            // КРИТИЧНО: Сначала проверяем, загружены ли библиотеки через NativeLibraryLoader
            String jnaLibraryPath = System.getProperty("jna.library.path");
            if (jnaLibraryPath != null && !jnaLibraryPath.isEmpty()) {
                // System.out.println("  Использование библиотек из jna.library.path: " + jnaLibraryPath);
                
                // Проверяем наличие libvlc в указанной директории
                File vlcLib = new File(jnaLibraryPath, "libvlc.dll");
                if (vlcLib.exists()) {
                    // System.out.println("  ✅ Найден libvlc.dll в " + vlcLib.getAbsolutePath());
                    
                    // Пробуем загрузить библиотеку напрямую через JNA
                    try {
                        com.sun.jna.NativeLibrary.addSearchPath("vlc", jnaLibraryPath);
                        com.sun.jna.NativeLibrary.addSearchPath("libvlc", jnaLibraryPath);
                        com.sun.jna.NativeLibrary.addSearchPath("libvlccore", jnaLibraryPath);
                        
                        // Пробуем загрузить библиотеку
                        com.sun.jna.NativeLibrary lib = com.sun.jna.NativeLibrary.getInstance("libvlc");
                        if (lib != null) {
                            // System.out.println("  ✅ libvlc успешно загружена через JNA");
                            discovered = true;
                        }
                    } catch (Exception e) {
                        // System.err.println("  ⚠️  Ошибка загрузки через JNA: " + e.getMessage());
                        // e.printStackTrace();
                    }
                }
                
                // Если не удалось загрузить напрямую, пробуем через NativeDiscovery
                if (!discovered) {
                    discovered = new NativeDiscovery().discover();
                }
            }
            
            // ЗАКОММЕНТИРОВАНО: Отключена загрузка встроенных VLC библиотек
            // Игра будет использовать только системный VLC
            /*
            // Если не найдено через NativeLibraryLoader, пытаемся использовать встроенные библиотеки
            if (!discovered) {
                embeddedLibPath = extractEmbeddedVlcLibraries();
                if (embeddedLibPath != null) {
                    // System.out.println("  Использование встроенных VLC библиотек: " + embeddedLibPath);
                    System.setProperty("jna.library.path", embeddedLibPath);
                    discovered = new NativeDiscovery().discover();
                    if (discovered) {
                        // System.out.println("  ✅ Встроенные VLC библиотеки успешно загружены");
                    }
                }
            }
            */
            
            // Если встроенные библиотеки не найдены, пробуем custom path
            if (!discovered && vlcPath != null && !vlcPath.isEmpty()) {
                // Use custom path if provided
                // System.out.println("  Попытка использовать custom VLC path: " + vlcPath);
                File vlcDir = new File(vlcPath);
                if (vlcDir.exists() && vlcDir.isDirectory()) {
                    System.setProperty("jna.library.path", vlcPath);
                    discovered = new NativeDiscovery().discover();
                }
            }
            
            // Если ничего не найдено, пробуем системный VLC
            if (!discovered) {
                // Automatic discovery
                // System.out.println("  Автоматический поиск системных VLC библиотек...");
                
                // Пробуем стандартные пути установки VLC на Windows
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    String[] possiblePaths = {
                        "C:\\Program Files\\VideoLAN\\VLC",
                        "C:\\Program Files (x86)\\VideoLAN\\VLC",
                        System.getenv("ProgramFiles") + "\\VideoLAN\\VLC",
                        System.getenv("ProgramFiles(x86)") + "\\VideoLAN\\VLC"
                    };
                    
                    for (String path : possiblePaths) {
                        if (path != null) {
                            File vlcDir = new File(path);
                            if (vlcDir.exists() && new File(vlcDir, "libvlc.dll").exists()) {
                                // System.out.println("  ✅ Найден VLC в: " + path);
                                System.setProperty("jna.library.path", path);
                                com.sun.jna.NativeLibrary.addSearchPath("vlc", path);
                                com.sun.jna.NativeLibrary.addSearchPath("libvlc", path);
                                com.sun.jna.NativeLibrary.addSearchPath("libvlccore", path);
                                break;
                            }
                        }
                    }
                }
                
                discovered = new NativeDiscovery().discover();
                
                // if (discovered) {
                //     System.out.println("  ✅ Системный VLC успешно найден");
                // }
            }
            
            if (!discovered) {
                errorMessage = "VLC native libraries not found. Please install VLC media player or provide custom path.";
                System.err.println("❌ " + errorMessage);
                
                // Show polite warning dialog if requested
                if (showWarning) {
                    showVlcNotFoundDialog();
                }
                
                return false;
            }
            
            // Create MediaPlayerFactory with minimal options
            List<String> options = new ArrayList<>();
            
            // ЗАКОММЕНТИРОВАНО: Отключена загрузка плагинов из встроенных библиотек
            // Системный VLC использует свои плагины автоматически
            /*
            // КРИТИЧНО: Если используем встроенные библиотеки, указываем путь к плагинам
            if (embeddedLibPath != null) {
                // Проверяем наличие папки plugins
                File pluginsDir = new File(embeddedLibPath, "plugins");
                if (pluginsDir.exists() && pluginsDir.isDirectory()) {
                    options.add("--plugin-path=" + pluginsDir.getAbsolutePath());
                    // System.out.println("  Используем плагины из: " + pluginsDir.getAbsolutePath());
                }
            }
            
            // Проверяем путь к библиотекам из NativeLibraryLoader
            if (jnaLibraryPath != null && !jnaLibraryPath.isEmpty()) {
                File pluginsDir = new File(jnaLibraryPath, "plugins");
                if (pluginsDir.exists() && pluginsDir.isDirectory()) {
                    options.add("--plugin-path=" + pluginsDir.getAbsolutePath());
                    // System.out.println("  Используем плагины из: " + pluginsDir.getAbsolutePath());
                }
            }
            */
            
            // Minimal performance options (без аппаратного ускорения, которое может не поддерживаться)
            options.add("--no-video-title-show"); // Don't show video title on video
            options.add("--no-snapshot-preview"); // Disable snapshot preview
            options.add("--quiet"); // Suppress console output
            
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
     * Show a polite dialog informing the user that VLC is not installed
     */
    private void showVlcNotFoundDialog() {
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION
                );
                alert.setTitle("VLC Media Player Not Found");
                alert.setHeaderText("VLC Media Player is recommended for the best experience");
                
                String message = "VLC Media Player was not detected on your system.\n\n" +
                                "To enjoy video cutscenes and enhanced media playback, " +
                                "please install VLC Media Player:\n\n" +
                                "• Windows: https://www.videolan.org/vlc/download-windows.html\n" +
                                "• macOS: https://www.videolan.org/vlc/download-macosx.html\n" +
                                "• Linux: https://www.videolan.org/vlc/download-linux.html\n\n" +
                                "The game will continue without video support.";
                
                alert.setContentText(message);
                
                // Пытаемся привязать к главному окну игры, если оно доступно
                try {
                    javafx.stage.Window owner = javafx.stage.Window.getWindows().stream()
                        .filter(w -> w instanceof javafx.stage.Stage)
                        .findFirst()
                        .orElse(null);
                    if (owner != null) {
                        alert.initOwner(owner);
                    }
                } catch (Exception ownerEx) {
                    // Игнорируем ошибки привязки к owner
                }
                
                // КРИТИЧНО: Устанавливаем диалог поверх всех окон после его отображения
                alert.getDialogPane().getScene().getWindow().setOnShown(event -> {
                    javafx.stage.Stage alertStage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
                    alertStage.setAlwaysOnTop(true);
                    alertStage.toFront();
                    alertStage.requestFocus();
                });
                
                alert.showAndWait();
            } catch (Exception e) {
                System.err.println("Failed to show VLC warning dialog: " + e.getMessage());
            }
        });
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

