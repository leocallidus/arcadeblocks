package com.arcadeblocks.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы со статистикой игроков в базе данных
 */
public class PlayerStats {
    
    private DatabaseManager databaseManager;
    
    public PlayerStats(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Добавить или обновить статистику игрока
     */
    public void updatePlayerStats(String playerName, int level, int score, int gamesPlayed) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            return;
        }
        
        try {
            Connection connection = getConnection();
            if (connection == null) return;
            
            // Проверяем, существует ли запись для этого игрока и уровня
            String selectSql = """
                SELECT best_score, games_played 
                FROM player_stats 
                WHERE player_name = ? AND level = ?
            """;
            
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setString(1, playerName);
                selectStmt.setInt(2, level);
                ResultSet rs = selectStmt.executeQuery();
                
                if (rs.next()) {
                    // Обновляем существующую запись
                    int currentBestScore = rs.getInt("best_score");
                    int currentGamesPlayed = rs.getInt("games_played");
                    
                    String updateSql = """
                        UPDATE player_stats 
                        SET score = ?, best_score = ?, games_played = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE player_name = ? AND level = ?
                    """;
                    
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, score);
                        updateStmt.setInt(2, Math.max(score, currentBestScore));
                        updateStmt.setInt(3, currentGamesPlayed + gamesPlayed);
                        updateStmt.setString(4, playerName);
                        updateStmt.setInt(5, level);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Создаем новую запись
                    String insertSql = """
                        INSERT INTO player_stats (player_name, level, score, best_score, games_played)
                        VALUES (?, ?, ?, ?, ?)
                    """;
                    
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, playerName);
                        insertStmt.setInt(2, level);
                        insertStmt.setInt(3, score);
                        insertStmt.setInt(4, score);
                        insertStmt.setInt(5, gamesPlayed);
                        insertStmt.executeUpdate();
                    }
                }
            }
            
            // Обновляем подпись после изменения статистики
            DatabaseIntegrity.updateSignature(connection, "player_stats");
            
        } catch (SQLException e) {
            System.err.println("Ошибка обновления статистики игрока: " + e.getMessage());
        }
    }
    
    /**
     * Получить статистику игрока для определенного уровня
     */
    public PlayerStatsRecord getPlayerStats(String playerName, int level) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            return null;
        }
        
        try {
            Connection connection = getConnection();
            if (connection == null) return null;
            
            String sql = """
                SELECT player_name, level, score, best_score, games_played, created_at, updated_at
                FROM player_stats 
                WHERE player_name = ? AND level = ?
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerName);
                stmt.setInt(2, level);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return new PlayerStatsRecord(
                        rs.getString("player_name"),
                        rs.getInt("level"),
                        rs.getInt("score"),
                        rs.getInt("best_score"),
                        rs.getInt("games_played"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения статистики игрока: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Получить топ игроков для определенного уровня
     */
    public List<PlayerStatsRecord> getTopPlayers(int level, int limit) {
        List<PlayerStatsRecord> topPlayers = new ArrayList<>();
        
        if (databaseManager == null || !databaseManager.isConnected()) {
            return topPlayers;
        }
        
        try {
            Connection connection = getConnection();
            if (connection == null) return topPlayers;
            
            String sql = """
                SELECT player_name, level, score, best_score, games_played, created_at, updated_at
                FROM player_stats 
                WHERE level = ?
                ORDER BY best_score DESC, games_played ASC
                LIMIT ?
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, level);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    topPlayers.add(new PlayerStatsRecord(
                        rs.getString("player_name"),
                        rs.getInt("level"),
                        rs.getInt("score"),
                        rs.getInt("best_score"),
                        rs.getInt("games_played"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения топа игроков: " + e.getMessage());
        }
        
        return topPlayers;
    }
    
    /**
     * Получить общую статистику всех игроков
     */
    public List<PlayerStatsRecord> getAllPlayerStats() {
        List<PlayerStatsRecord> allStats = new ArrayList<>();
        
        if (databaseManager == null || !databaseManager.isConnected()) {
            return allStats;
        }
        
        try {
            Connection connection = getConnection();
            if (connection == null) return allStats;
            
            String sql = """
                SELECT player_name, level, score, best_score, games_played, created_at, updated_at
                FROM player_stats 
                ORDER BY player_name, level
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    allStats.add(new PlayerStatsRecord(
                        rs.getString("player_name"),
                        rs.getInt("level"),
                        rs.getInt("score"),
                        rs.getInt("best_score"),
                        rs.getInt("games_played"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения всей статистики: " + e.getMessage());
        }
        
        return allStats;
    }
    
    /**
     * Получить соединение с базой данных
     */
    private Connection getConnection() {
        return databaseManager.getConnection();
    }
    
    /**
     * Запись статистики игрока
     */
    public static class PlayerStatsRecord {
        private final String playerName;
        private final int level;
        private final int score;
        private final int bestScore;
        private final int gamesPlayed;
        private final Timestamp createdAt;
        private final Timestamp updatedAt;
        
        public PlayerStatsRecord(String playerName, int level, int score, int bestScore, 
                               int gamesPlayed, Timestamp createdAt, Timestamp updatedAt) {
            this.playerName = playerName;
            this.level = level;
            this.score = score;
            this.bestScore = bestScore;
            this.gamesPlayed = gamesPlayed;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        // Геттеры
        public String getPlayerName() { return playerName; }
        public int getLevel() { return level; }
        public int getScore() { return score; }
        public int getBestScore() { return bestScore; }
        public int getGamesPlayed() { return gamesPlayed; }
        public Timestamp getCreatedAt() { return createdAt; }
        public Timestamp getUpdatedAt() { return updatedAt; }
        
        @Override
        public String toString() {
            return String.format("%s (Уровень %d): %d очков (лучший: %d, игр: %d)", 
                               playerName, level, score, bestScore, gamesPlayed);
        }
    }
}
