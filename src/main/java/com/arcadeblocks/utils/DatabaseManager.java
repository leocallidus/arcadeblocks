package com.arcadeblocks.utils;

import java.sql.*;
import java.util.Properties;

/**
 * Менеджер базы данных SQLite для сохранения настроек игры
 */
public class DatabaseManager {
    
    private static final String DB_NAME = "arcade_blocks.db";
    private final String connectionUrl;
    
    private Connection connection;
    private final Object dbLock = new Object();

    public DatabaseManager() {
        this.connectionUrl = "jdbc:sqlite:" + AppDataManager.getDatabasePath().toString();
    }
    
    /**
     * Инициализация базы данных
     */
    public void initialize() {
        try {
            // Создаем подключение к базе данных
            connection = DriverManager.getConnection(connectionUrl);
            
            // Создаем таблицы, если они не существуют
            createTables();
            
            // Создаем таблицу для проверки целостности
            DatabaseIntegrity.createIntegrityTable(connection);
            
            // Инициализируем подписи для всех таблиц (если это первый запуск)
            DatabaseIntegrity.initializeSignatures(connection);
            
            // Проверяем целостность существующих данных
            if (!DatabaseIntegrity.verifyAllTables(connection)) {
                System.err.println("⚠️ Обнаружена манипуляция с базой данных. Поврежденные данные были сброшены.");
            }
            
        // System.out.println("База данных SQLite инициализирована: " + DB_NAME);
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создание таблиц базы данных
     */
    private void createTables() throws SQLException {
        // Таблица настроек игры
        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS game_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key VARCHAR(255) UNIQUE NOT NULL,
                value TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Таблица игровых данных
        String createGameDataTable = """
            CREATE TABLE IF NOT EXISTS game_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key VARCHAR(255) UNIQUE NOT NULL,
                value TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Таблица статистики игрока
        String createPlayerStatsTable = """
            CREATE TABLE IF NOT EXISTS player_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_name VARCHAR(255) NOT NULL,
                level INTEGER NOT NULL,
                score INTEGER NOT NULL,
                games_played INTEGER DEFAULT 0,
                best_score INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSettingsTable);
            stmt.execute(createGameDataTable);
            stmt.execute(createPlayerStatsTable);
            
        // System.out.println("Таблицы базы данных созданы успешно");
        }
    }
    
    /**
     * Получить значение настройки
     */
    public String getSetting(String key) {
        return getValue("game_settings", key);
    }
    
    /**
     * Установить значение настройки
     */
    public void setSetting(String key, String value) {
        setValue("game_settings", key, value);
    }
    
    /**
     * Получить игровые данные
     */
    public String getGameData(String key) {
        return getValue("game_data", key);
    }
    
    /**
     * Установить игровые данные
     */
    public void setGameData(String key, String value) {
        setValue("game_data", key, value);
    }
    
    /**
     * Получить значение из указанной таблицы
     */
    private String getValue(String tableName, String key) {
        synchronized (dbLock) {
            String sql = "SELECT value FROM " + tableName + " WHERE key = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, key);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getString("value");
                }
            } catch (SQLException e) {
                System.err.println("Ошибка получения значения из " + tableName + ": " + e.getMessage());
            }
            
            return null;
        }
    }
    
    /**
     * Установить значение в указанную таблицу
     */
    private void setValue(String tableName, String key, String value) {
        synchronized (dbLock) {
            String sql = """
                INSERT OR REPLACE INTO %s (key, value, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """.formatted(tableName);
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.setString(2, value);
                pstmt.executeUpdate();
                
                // Обновляем подпись после изменения данных
                DatabaseIntegrity.updateSignature(connection, tableName);
            } catch (SQLException e) {
                System.err.println("Ошибка установки значения в " + tableName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Удалить настройку
     */
    public void removeSetting(String key) {
        removeValue("game_settings", key);
    }
    
    /**
     * Удалить игровые данные
     */
    public void removeGameData(String key) {
        removeValue("game_data", key);
    }
    
    /**
     * Удалить значение из указанной таблицы
     */
    private void removeValue(String tableName, String key) {
        synchronized (dbLock) {
            String sql = "DELETE FROM " + tableName + " WHERE key = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.executeUpdate();
                
                // Обновляем подпись после удаления данных
                DatabaseIntegrity.updateSignature(connection, tableName);
            } catch (SQLException e) {
                System.err.println("Ошибка удаления значения из " + tableName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Получить все настройки в виде Properties
     */
    public Properties getAllSettings() {
        synchronized (dbLock) {
            Properties props = new Properties();
            String sql = "SELECT key, value FROM game_settings";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    props.setProperty(rs.getString("key"), rs.getString("value"));
                }
            } catch (SQLException e) {
                System.err.println("Ошибка получения всех настроек: " + e.getMessage());
            }
            
            return props;
        }
    }
    
    /**
     * Очистить все настройки
     */
    public void clearAllSettings() {
        synchronized (dbLock) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM game_settings");
                
                // Обновляем подпись после очистки
                DatabaseIntegrity.updateSignature(connection, "game_settings");
        // System.out.println("Все настройки очищены");
            } catch (SQLException e) {
                System.err.println("Ошибка очистки настроек: " + e.getMessage());
            }
        }
    }
    
    /**
     * Очистить все игровые данные
     */
    public void clearAllGameData() {
        synchronized (dbLock) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM game_data");
                
                // Обновляем подпись после очистки
                DatabaseIntegrity.updateSignature(connection, "game_data");
        // System.out.println("Все игровые данные очищены");
            } catch (SQLException e) {
                System.err.println("Ошибка очистки игровых данных: " + e.getMessage());
            }
        }
    }
    
    /**
     * Получить статистику базы данных
     */
    public void printDatabaseStats() {
        synchronized (dbLock) {
            try (Statement stmt = connection.createStatement()) {
        // System.out.println("=== Статистика базы данных ===");
                
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM game_settings");
                if (rs.next()) {
        // System.out.println("Настроек: " + rs.getInt("count"));
                }
                
                rs = stmt.executeQuery("SELECT COUNT(*) as count FROM game_data");
                if (rs.next()) {
        // System.out.println("Игровых данных: " + rs.getInt("count"));
                }
                
                rs = stmt.executeQuery("SELECT COUNT(*) as count FROM player_stats");
                if (rs.next()) {
        // System.out.println("Статистики игроков: " + rs.getInt("count"));
                }
                
        // System.out.println("===============================");
            } catch (SQLException e) {
                System.err.println("Ошибка получения статистики: " + e.getMessage());
            }
        }
    }
    
    /**
     * Получить соединение с базой данных (для внешнего использования)
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * Закрытие соединения с базой данных
     */
    public void close() {
        synchronized (dbLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
        // System.out.println("Соединение с базой данных закрыто");
                }
            } catch (SQLException e) {
                System.err.println("Ошибка закрытия соединения: " + e.getMessage());
            }
        }
    }
    
    /**
     * Проверка соединения с базой данных
     */
    public boolean isConnected() {
        synchronized (dbLock) {
            try {
                return connection != null && !connection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }
    }
}
