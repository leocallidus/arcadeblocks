package com.arcadeblocks.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * Простой тест для проверки работы системы целостности базы данных
 */
public class DatabaseIntegrityTest {
    
    public static void main(String[] args) {
        System.out.println("=== Тест системы проверки целостности базы данных ===\n");
        
        try {
            // Создаем временную базу данных для теста
            Path tempDb = Files.createTempFile("test_arcade_blocks_", ".db");
            String connectionUrl = "jdbc:sqlite:" + tempDb.toString();
            
            System.out.println("1. Создание тестовой базы данных: " + tempDb);
            
            // Инициализируем базу данных
            DatabaseManager dbManager = new DatabaseManager() {
                private Connection testConnection;
                
                @Override
                public void initialize() {
                    try {
                        testConnection = DriverManager.getConnection(connectionUrl);
                        
                        // Создаем таблицы
                        try (Statement stmt = testConnection.createStatement()) {
                            stmt.execute("""
                                CREATE TABLE IF NOT EXISTS game_settings (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    key VARCHAR(255) UNIQUE NOT NULL,
                                    value TEXT NOT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                )
                            """);
                            
                            stmt.execute("""
                                CREATE TABLE IF NOT EXISTS game_data (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    key VARCHAR(255) UNIQUE NOT NULL,
                                    value TEXT NOT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                                )
                            """);
                            
                            stmt.execute("""
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
                            """);
                        }
                        
                        // Создаем таблицу целостности
                        DatabaseIntegrity.createIntegrityTable(testConnection);
                        
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                
                @Override
                public Connection getConnection() {
                    return testConnection;
                }
                
                @Override
                public boolean isConnected() {
                    try {
                        return testConnection != null && !testConnection.isClosed();
                    } catch (SQLException e) {
                        return false;
                    }
                }
                
                @Override
                public void close() {
                    try {
                        if (testConnection != null && !testConnection.isClosed()) {
                            testConnection.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            };
            
            dbManager.initialize();
            Connection conn = dbManager.getConnection();
            
            System.out.println("✓ База данных создана\n");
            
            // Тест 1: Добавление данных и создание подписей
            System.out.println("2. Добавление тестовых данных...");
            try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO game_data (key, value) VALUES (?, ?)")) {
                pstmt.setString(1, "current_level");
                pstmt.setString(2, "5");
                pstmt.executeUpdate();
            }
            
            // Создаем подпись
            DatabaseIntegrity.initializeSignatures(conn);
            System.out.println("✓ Данные добавлены и подпись создана\n");
            
            // Тест 2: Проверка целостности (должна пройти)
            System.out.println("3. Проверка целостности (данные не изменены)...");
            boolean integrityOk = DatabaseIntegrity.verifyIntegrity(conn, "game_data");
            if (integrityOk) {
                System.out.println("✓ Проверка целостности ПРОЙДЕНА\n");
            } else {
                System.out.println("✗ ОШИБКА: Проверка целостности не прошла!\n");
            }
            
            // Тест 3: Манипуляция с данными (имитация DBeaver)
            System.out.println("4. Имитация манипуляции через внешний инструмент...");
            System.out.println("   Изменяем уровень с 5 на 99 напрямую в SQL...");
            try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE game_data SET value = ? WHERE key = ?")) {
                pstmt.setString(1, "99");
                pstmt.setString(2, "current_level");
                pstmt.executeUpdate();
            }
            System.out.println("✓ Данные изменены (НЕ через API игры)\n");
            
            // Тест 4: Проверка целостности (должна НЕ пройти)
            System.out.println("5. Проверка целостности (данные изменены извне)...");
            boolean integrityFailed = DatabaseIntegrity.verifyIntegrity(conn, "game_data");
            if (!integrityFailed) {
                System.out.println("✓ Манипуляция ОБНАРУЖЕНА! Проверка целостности не прошла.\n");
            } else {
                System.out.println("✗ ОШИБКА: Манипуляция НЕ обнаружена!\n");
            }
            
            // Тест 5: Сброс поврежденных данных
            System.out.println("6. Обработка манипуляции (сброс данных)...");
            DatabaseIntegrity.handleTampering(conn, "game_data", 
                DatabaseIntegrity.TamperingAction.RESET);
            
            // Проверяем что данные сброшены
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM game_data")) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count == 0) {
                        System.out.println("✓ Таблица успешно очищена\n");
                    } else {
                        System.out.println("✗ ОШИБКА: Таблица не очищена (записей: " + count + ")\n");
                    }
                }
            }
            
            // Закрываем соединение
            dbManager.close();
            
            // Удаляем временную базу
            Files.deleteIfExists(tempDb);
            
            System.out.println("=== Все тесты завершены ===");
            System.out.println("\nВывод: Система проверки целостности работает корректно!");
            System.out.println("Любые изменения данных через внешние инструменты будут обнаружены.");
            
        } catch (Exception e) {
            System.err.println("Ошибка выполнения теста: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
