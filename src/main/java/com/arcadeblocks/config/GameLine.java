package com.arcadeblocks.config;

/**
 * Игровые линии (кампании)
 */
public enum GameLine {
    ARCADE_BLOCKS("arcade_blocks", "gameline.arcade_blocks.name", 1, 116, true),
    ARCADE_BLOCKS_BONUS("arcade_blocks_bonus", "gameline.arcade_blocks_bonus.name", 1001, 1021, true),
    LBREAKOUT1("lbreakout1", "gameline.lbreakout1.name", 5000, 5029, true),
    LBREAKOUT2("lbreakout2", "gameline.lbreakout2.name", 2001, 2001, false),
    LBREAKOUT2_COMMUNITY("lbreakout2_community", "gameline.lbreakout2_community.name", 3001, 3001, false);

    private final String id;
    private final String nameKey;
    private final int startLevel;
    private final int endLevel;
    private final boolean available;

    GameLine(String id, String nameKey, int startLevel, int endLevel, boolean available) {
        this.id = id;
        this.nameKey = nameKey;
        this.startLevel = startLevel;
        this.endLevel = endLevel;
        this.available = available;
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public int getStartLevel() { return startLevel; }
    public int getEndLevel() { return endLevel; }
    public boolean isAvailable() { return available; }

    public static GameLine fromId(String id) {
        for (GameLine line : values()) {
            if (line.id.equals(id)) return line;
        }
        return ARCADE_BLOCKS;
    }

    public static GameLine fromLevel(int levelNumber) {
        for (GameLine line : values()) {
            if (levelNumber >= line.startLevel && levelNumber <= line.endLevel) {
                return line;
            }
        }
        return ARCADE_BLOCKS;
    }
}
