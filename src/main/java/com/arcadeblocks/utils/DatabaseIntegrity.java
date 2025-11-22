package com.arcadeblocks.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Утилита для проверки целостности базы данных с использованием HMAC подписей.
 * Обнаруживает несанкционированные изменения данных через внешние инструменты.
 */
public class DatabaseIntegrity {
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String INTEGRITY_TABLE = "integrity_metadata";
    
    // Обфусцированный секретный ключ (в реальном приложении можно усложнить)
    private static final byte[] SECRET_KEY = deriveSecretKey();
    
    /**
     * Действие при обнаружении манипуляции с данными
     */
    public enum TamperingAction {
        RESET,      // Сбросить поврежденные данные
        LOG_ONLY    // Только записать в лог
    }
    
    // Настройка поведения при обнаружении манипуляции
    private static final TamperingAction DEFAULT_ACTION = TamperingAction.RESET;
    
    /**
     * Генерация секретного ключа из обфусцированной строки
     */
    private static byte[] deriveSecretKey() {
        // Обфусцированная строка (можно заменить на более сложную схему)
        String obfuscated = new StringBuilder()
            .append("arcade").append("-").append("blocks")
            .append("-").append("integrity").append("-")
            .append("key").append("-").append("2024")
            .toString();
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(obfuscated.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback на простой ключ
            return obfuscated.getBytes(StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Создание таблицы для хранения метаданных целостности
     */
    public static void createIntegrityTable(Connection conn) throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS integrity_metadata (
                table_name VARCHAR(255) PRIMARY KEY,
                signature TEXT NOT NULL,
                last_check TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }
    
    /**
     * Вычисление HMAC подписи для таблицы
     */
    public static String computeSignature(Connection conn, String tableName) throws SQLException {
        // Получаем все данные из таблицы в отсортированном порядке
        List<String> rows = new ArrayList<>();
        
        String sql = String.format("SELECT * FROM %s ORDER BY ROWID", tableName);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                StringBuilder rowData = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    // Пропускаем временные метки (создания и обновления)
                    if (columnName.equals("created_at") || columnName.equals("updated_at") || columnName.equals("last_check")) {
                        continue;
                    }
                    
                    String value = rs.getString(i);
                    if (value != null) {
                        rowData.append(columnName).append("=").append(value).append(";");
                    }
                }
                rows.add(rowData.toString());
            }
        }
        
        // Объединяем все строки в одну строку для подписи
        String dataToSign = String.join("|", rows);
        // System.out.println("[DEBUG] Данные для подписи таблицы " + tableName + ": " + dataToSign.substring(0, Math.min(100, dataToSign.length())) + "...");
        
        // Вычисляем HMAC
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, HMAC_ALGORITHM);
            mac.init(keySpec);
            
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            
            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            System.err.println("Ошибка вычисления HMAC подписи: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Сохранение подписи в таблицу метаданных
     */
    public static void saveSignature(Connection conn, String tableName, String signature) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO integrity_metadata (table_name, signature, last_check)
            VALUES (?, ?, CURRENT_TIMESTAMP)
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, signature);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Обновление подписи для таблицы
     */
    public static void updateSignature(Connection conn, String tableName) {
        try {
            // Проверяем, существует ли таблица целостности
            if (!tableExists(conn, INTEGRITY_TABLE)) {
                // Таблица целостности еще не создана, пропускаем обновление
                // System.out.println("[DEBUG] Таблица integrity_metadata не существует, пропускаем обновление подписи для " + tableName);
                return;
            }
            
            String signature = computeSignature(conn, tableName);
            // System.out.println("[DEBUG] Обновление подписи для " + tableName + ": " + (signature != null ? signature.substring(0, Math.min(16, signature.length())) + "..." : "null"));
            saveSignature(conn, tableName, signature);
        } catch (SQLException e) {
            // Игнорируем ошибки блокировки БД - они возникают при параллельных подключениях
            if (!e.getMessage().contains("database is locked")) {
                System.err.println("Ошибка обновления подписи для " + tableName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Проверка целостности таблицы
     */
    public static boolean verifyIntegrity(Connection conn, String tableName) {
        try {
            // Получаем сохраненную подпись
            String savedSignature = getSavedSignature(conn, tableName);
            
            // System.out.println("[DEBUG] Проверка целостности таблицы: " + tableName);
            // System.out.println("[DEBUG] Сохраненная подпись: " + (savedSignature != null ? savedSignature.substring(0, Math.min(16, savedSignature.length())) + "..." : "null"));
            
            // Если подписи нет, создаем новую (первый запуск)
            if (savedSignature == null || savedSignature.isEmpty()) {
                // System.out.println("[DEBUG] Подпись не найдена, создаем новую для " + tableName);
                updateSignature(conn, tableName);
                return true;
            }
            
            // Вычисляем текущую подпись
            String currentSignature = computeSignature(conn, tableName);
            // System.out.println("[DEBUG] Текущая подпись: " + (currentSignature != null ? currentSignature.substring(0, Math.min(16, currentSignature.length())) + "..." : "null"));
            
            // Сравниваем подписи
            boolean match = savedSignature.equals(currentSignature);
            // System.out.println("[DEBUG] Подписи совпадают: " + match);
            
            return match;
            
        } catch (SQLException e) {
            System.err.println("Ошибка проверки целостности " + tableName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Получение сохраненной подписи из метаданных
     */
    private static String getSavedSignature(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT signature FROM integrity_metadata WHERE table_name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("signature");
            }
        }
        
        return null;
    }
    
    /**
     * Проверка целостности всех критичных таблиц
     */
    public static boolean verifyAllTables(Connection conn) {
        // System.out.println("[DEBUG] ========== НАЧАЛО ПРОВЕРКИ ЦЕЛОСТНОСТИ БД ==========");
        String[] criticalTables = {"game_settings", "game_data", "player_stats"};
        boolean allValid = true;
        
        for (String table : criticalTables) {
            if (!tableExists(conn, table)) {
                // System.out.println("[DEBUG] Таблица " + table + " не существует, пропускаем");
                continue;
            }
            
            if (!verifyIntegrity(conn, table)) {
                System.err.println("⚠️ ОБНАРУЖЕНА МАНИПУЛЯЦИЯ: таблица " + table + " была изменена!");
                handleTampering(conn, table, DEFAULT_ACTION);
                allValid = false;
            } else {
                // System.out.println("[DEBUG] ✓ Таблица " + table + " целостна");
            }
        }
        
        // System.out.println("[DEBUG] ========== КОНЕЦ ПРОВЕРКИ ЦЕЛОСТНОСТИ БД ==========");
        return allValid;
    }
    
    /**
     * Проверка существования таблицы
     */
    private static boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, tableName, null);
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Обработка обнаруженной манипуляции
     */
    public static void handleTampering(Connection conn, String tableName, TamperingAction action) {
        System.err.println("🔒 Обработка манипуляции с таблицей: " + tableName);
        
        switch (action) {
            case RESET:
                resetTable(conn, tableName);
                System.err.println("✓ Таблица " + tableName + " была сброшена");
                break;
                
            case LOG_ONLY:
                System.err.println("⚠️ Обнаружена манипуляция с " + tableName + ", но данные сохранены");
                // Обновляем подпись для текущего состояния
                updateSignature(conn, tableName);
                break;
        }
    }
    
    /**
     * Сброс содержимого таблицы
     */
    private static void resetTable(Connection conn, String tableName) {
        try {
            String sql = "DELETE FROM " + tableName;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            
            // Обновляем подпись для пустой таблицы
            updateSignature(conn, tableName);
            
        } catch (SQLException e) {
            System.err.println("Ошибка сброса таблицы " + tableName + ": " + e.getMessage());
        }
    }
    
    /**
     * Инициализация подписей для всех таблиц (первый запуск)
     * ВАЖНО: создает подписи ТОЛЬКО если их еще нет!
     */
    public static void initializeSignatures(Connection conn) {
        // System.out.println("[DEBUG] Инициализация подписей для всех таблиц");
        String[] tables = {"game_settings", "game_data", "player_stats"};
        
        for (String table : tables) {
            if (tableExists(conn, table)) {
                // Проверяем, есть ли уже подпись для этой таблицы
                try {
                    String existingSignature = getSavedSignature(conn, table);
                    if (existingSignature == null || existingSignature.isEmpty()) {
                        // Подписи нет - создаем
                        // System.out.println("[DEBUG] Создаем новую подпись для " + table);
                        updateSignature(conn, table);
                    } else {
                        // Подпись уже существует - НЕ перезаписываем!
                        // System.out.println("[DEBUG] Подпись для " + table + " уже существует, пропускаем");
                    }
                } catch (SQLException e) {
                    System.err.println("Ошибка при проверке подписи для " + table + ": " + e.getMessage());
                }
            }
        }
        // System.out.println("[DEBUG] Инициализация подписей завершена");
    }
}
