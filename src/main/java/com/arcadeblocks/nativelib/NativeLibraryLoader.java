package com.arcadeblocks.nativelib;

import com.sun.jna.Platform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Загрузчик нативных библиотек SDL2 и SDL2_mixer
 * Автоматически определяет платформу и загружает соответствующие библиотеки
 */
public class NativeLibraryLoader {
    
    private static final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());
    private static final String NATIVES_DIR = "natives";
    private static boolean librariesLoaded = false;
    private static Path tempDirectory = null;
    
    /**
     * Загружает нативные библиотеки для текущей платформы
     */
    public static synchronized void loadLibraries() {
        if (librariesLoaded) {
            return;
        }
        
        try {
            String platform = getPlatformString();
            logger.info("Загрузка нативных библиотек для платформы: " + platform);
            
            // Создаем временную директорию для библиотек
            Path tempDir = createTempDirectory();
            tempDirectory = tempDir;
            
            // Регистрируем shutdown hook для гарантированной очистки
            registerShutdownHook();
            
            // Загружаем библиотеки для текущей платформы
            loadPlatformLibraries(platform, tempDir);
            
            // Устанавливаем путь к библиотекам
            setLibraryPath(tempDir.toString());
            
            librariesLoaded = true;
            logger.info("Нативные библиотеки успешно загружены");
            
        } catch (Exception e) {
            logger.severe("Ошибка загрузки нативных библиотек: " + e.getMessage());
            throw new RuntimeException("Не удалось загрузить нативные библиотеки", e);
        }
    }
    
    /**
     * Определяет строку платформы для загрузки библиотек
     */
    private static String getPlatformString() {
        if (Platform.isWindows()) {
            return Platform.is64Bit() ? "windows-x64" : "windows-x86";
        } else if (Platform.isLinux()) {
            return Platform.is64Bit() ? "linux-x64" : "linux-x86";
        } else if (Platform.isMac()) {
            // Проверяем архитектуру для macOS
            String arch = System.getProperty("os.arch").toLowerCase();
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                return "macos-aarch64";
            } else {
                return "macos-x64";
            }
        } else {
            throw new UnsupportedOperationException("Неподдерживаемая платформа: " + Platform.getOSType());
        }
    }
    
    /**
     * Создает временную директорию для библиотек
     */
    private static Path createTempDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("arcade-blocks-natives");
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }
    
    /**
     * Регистрирует shutdown hook для гарантированной очистки временных файлов
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (tempDirectory != null && Files.exists(tempDirectory)) {
                try {
                    deleteDirectoryRecursively(tempDirectory);
                    logger.info("Временные нативные библиотеки успешно удалены");
                } catch (IOException e) {
                    logger.warning("Ошибка удаления временной директории: " + e.getMessage());
                }
            }
        }, "NativeLibraryCleanupHook"));
    }
    
    /**
     * Рекурсивно удаляет директорию и все её содержимое
     */
    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        if (Files.isDirectory(directory)) {
            // Удаляем все файлы и поддиректории
            try (var stream = Files.list(directory)) {
                stream.forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            deleteDirectoryRecursively(path);
                        } else {
                            Files.delete(path);
                        }
                    } catch (IOException e) {
                        logger.warning("Не удалось удалить: " + path + " - " + e.getMessage());
                    }
                });
            }
        }
        
        // Удаляем саму директорию
        try {
            Files.delete(directory);
        } catch (IOException e) {
            logger.warning("Не удалось удалить директорию: " + directory + " - " + e.getMessage());
        }
    }
    
    /**
     * Загружает библиотеки для конкретной платформы
     */
    private static void loadPlatformLibraries(String platform, Path tempDir) throws IOException {
        String[] libraries = getLibrariesForPlatform(platform);
        
        for (String library : libraries) {
            // Пробуем разные варианты путей
            String[] possiblePaths = {
                NATIVES_DIR + "/" + platform + "/" + library,
                NATIVES_DIR + "/" + platform + "/" + library + getLibraryExtension(),
                "/" + NATIVES_DIR + "/" + platform + "/" + library,
                "/" + NATIVES_DIR + "/" + platform + "/" + library + getLibraryExtension()
            };
            
            InputStream inputStream = null;
            String foundPath = null;
            
            for (String resourcePath : possiblePaths) {
                inputStream = NativeLibraryLoader.class.getClassLoader().getResourceAsStream(resourcePath);
                if (inputStream != null) {
                    foundPath = resourcePath;
                    break;
                }
            }
            
            if (inputStream == null) {
                logger.warning("Библиотека не найдена в ресурсах. Проверенные пути: " + String.join(", ", possiblePaths));
                continue;
            }
            
            // Определяем расширение файла
            String extension = getLibraryExtension();
            String fileName = library + extension;
            Path libraryPath = tempDir.resolve(fileName);
            
            // Копируем библиотеку во временную директорию
            try (FileOutputStream outputStream = new FileOutputStream(libraryPath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            // Устанавливаем права на выполнение для Unix-систем
            if (!Platform.isWindows()) {
                libraryPath.toFile().setExecutable(true);
            }
            
            logger.info("Загружена библиотека: " + fileName + " из " + foundPath);
        }
    }
    
    /**
     * Возвращает список библиотек для конкретной платформы
     */
    private static String[] getLibrariesForPlatform(String platform) {
        if (platform.startsWith("windows")) {
            return new String[]{"SDL2", "SDL2_mixer"};
        } else if (platform.startsWith("linux")) {
            return new String[]{"libSDL2", "libSDL2_mixer"};
        } else if (platform.startsWith("macos")) {
            return new String[]{"libSDL2", "libSDL2_mixer"};
        } else {
            throw new UnsupportedOperationException("Неподдерживаемая платформа: " + platform);
        }
    }
    
    /**
     * Возвращает расширение файла для библиотек
     */
    private static String getLibraryExtension() {
        if (Platform.isWindows()) {
            return ".dll";
        } else if (Platform.isLinux()) {
            return ".so";
        } else if (Platform.isMac()) {
            return ".dylib";
        } else {
            return "";
        }
    }
    
    /**
     * Устанавливает путь к библиотекам для JNA
     */
    private static void setLibraryPath(String libraryPath) {
        String currentPath = System.getProperty("jna.library.path", "");
        if (currentPath.isEmpty()) {
            System.setProperty("jna.library.path", libraryPath);
        } else {
            System.setProperty("jna.library.path", currentPath + File.pathSeparator + libraryPath);
        }
        
        // Также устанавливаем LD_LIBRARY_PATH для Linux
        if (Platform.isLinux()) {
            String ldPath = System.getenv("LD_LIBRARY_PATH");
            if (ldPath == null || ldPath.isEmpty()) {
                System.setProperty("java.library.path", libraryPath);
            } else {
                System.setProperty("java.library.path", ldPath + File.pathSeparator + libraryPath);
            }
        }
        
        // Принудительно устанавливаем путь к библиотекам в начало списка поиска
        System.setProperty("jna.library.path", libraryPath);
    }
    
    /**
     * Проверяет, загружены ли библиотеки
     */
    public static boolean areLibrariesLoaded() {
        return librariesLoaded;
    }
}
