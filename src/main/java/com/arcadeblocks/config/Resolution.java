package com.arcadeblocks.config;

/**
 * Класс для представления разрешения экрана
 */
public class Resolution {
    private final int width;
    private final int height;
    private final String displayName;
    
    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
        this.displayName = width + "x" + height;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Resolution that = (Resolution) obj;
        return width == that.width && height == that.height;
    }
    
    @Override
    public int hashCode() {
        return width * 10000 + height;
    }
    
    /**
     * Создает Resolution из строкового представления "1600x900"
     */
    public static Resolution fromString(String str) {
        if (str == null || !str.contains("x")) {
            return GameConfig.DEFAULT_RESOLUTION;
        }
        try {
            String[] parts = str.split("x");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            return new Resolution(width, height);
        } catch (Exception e) {
            return GameConfig.DEFAULT_RESOLUTION;
        }
    }
}

