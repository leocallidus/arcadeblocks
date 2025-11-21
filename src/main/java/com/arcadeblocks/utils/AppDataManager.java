package com.arcadeblocks.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralised access point for all runtime storage directories.
 * Ensures every game-generated file (database, logs, FXGL artifacts)
 * is stored under the {@code arcade_blocks_data} root directory.
 */
public final class AppDataManager {

    private static final String DATA_DIR_PROPERTY = "arcade.blocks.data.dir";
    private static final String DATA_DIR_ENV = "ARCADE_BLOCKS_DATA_DIR";
    private static final String DEFAULT_DIR_NAME = "arcade_blocks_data";

    private static final Path DATA_DIRECTORY;
    private static final Path LOGS_DIRECTORY;
    private static final Path FXGL_DIRECTORY;

    static {
        DATA_DIRECTORY = initDataDirectory();
        LOGS_DIRECTORY = ensureDirectory(DATA_DIRECTORY.resolve("logs"));
        FXGL_DIRECTORY = ensureDirectory(DATA_DIRECTORY.resolve("fxgl"));
    }

    private AppDataManager() {
    }

    /**
     * Returns the absolute path to the root data directory.
     */
    public static Path getDataDirectory() {
        return DATA_DIRECTORY;
    }

    /**
     * Returns the absolute path to the SQLite database file.
     */
    public static Path getDatabasePath() {
        return DATA_DIRECTORY.resolve("arcade_blocks.db");
    }

    /**
     * Returns the absolute path to the directory used for FXGL log files.
     */
    public static Path getLogsDirectory() {
        return LOGS_DIRECTORY;
    }

    /**
     * Returns the absolute path to the directory reserved for FXGL runtime data.
     * (Created for potential future use by FXGL subsystems.)
     */
    public static Path getFxglDirectory() {
        return FXGL_DIRECTORY;
    }

    private static Path initDataDirectory() {
        String configuredPath = System.getProperty(DATA_DIR_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv(DATA_DIR_ENV);
        }

        Path basePath;
        if (configuredPath != null && !configuredPath.isBlank()) {
            basePath = Paths.get(configuredPath).toAbsolutePath();
        } else {
            basePath = resolveDefaultBasePath();
        }

        return ensureDirectory(basePath);
    }

    private static Path resolveDefaultBasePath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        Path userHome = Paths.get(System.getProperty("user.home"));

        if (osName.contains("linux")) {
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            Path configHome;
            if (xdgConfig != null && !xdgConfig.isBlank()) {
                configHome = Paths.get(xdgConfig);
            } else {
                configHome = userHome.resolve(".config");
            }
            return configHome.resolve(DEFAULT_DIR_NAME);
        }

        if (osName.contains("windows")) {
            // На Windows используем %APPDATA%\ArcadeBlocks\arcade_blocks_data
            String appData = System.getenv("APPDATA");
            Path appDataPath;
            if (appData != null && !appData.isBlank()) {
                appDataPath = Paths.get(appData);
            } else {
                // Fallback: если APPDATA не задан, используем стандартный путь
                appDataPath = userHome.resolve("AppData").resolve("Roaming");
            }
            return appDataPath.resolve("ArcadeBlocks").resolve(DEFAULT_DIR_NAME);
        }

        // Для других ОС используем домашнюю директорию
        return userHome.resolve(DEFAULT_DIR_NAME);
    }

    private static Path ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать директорию данных: " + path, e);
        }
        return path;
    }
}
