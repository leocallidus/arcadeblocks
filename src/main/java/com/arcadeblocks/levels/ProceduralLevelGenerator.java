package com.arcadeblocks.levels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Генератор процедурных уровней для второй половины кампании (51-100).
 */
public final class ProceduralLevelGenerator {

    private static final int DEFAULT_COLUMNS = 16;
    private static final int DEFAULT_ROWS = 11;
    private static final int BRICK_WIDTH = 60;
    private static final int BRICK_HEIGHT = 30;
    private static final int BRICK_SPACING = 2;
    private static final int START_Y = 80;

    private static final String[][] COLOR_SETS = {
        {"green", "purple", "pink", "light_gray"},
        {"cyan", "yellow", "orange", "red"},
        {"blue", "dark_blue", "purple", "light_gray"},
        {"pink", "orange", "yellow", "green"},
        {"purple", "cyan", "light_gray", "red"}
    };

    private ProceduralLevelGenerator() {
    }

    public static LevelLoader.LevelLayout generate(int levelNumber) {
        if (levelNumber < 51 || levelNumber > 100) {
            return null;
        }

        if (levelNumber % 10 == 0) {
            return generateBossLayout(levelNumber);
        }

        Template template = pickTemplate(levelNumber);
        Map<Character, BrickStyle> palette = createPalette(levelNumber);
        List<LevelLoader.BrickDefinition> bricks = new ArrayList<>();

        for (int row = 0; row < DEFAULT_ROWS; row++) {
            for (int col = 0; col < DEFAULT_COLUMNS; col++) {
                char token = template.compute(row, col, DEFAULT_ROWS, DEFAULT_COLUMNS, levelNumber);
                if (token == '.') {
                    continue;
                }
                BrickStyle style = palette.getOrDefault(token, palette.getOrDefault('A', BrickStyle.DEFAULT_STYLE));
                boolean isExplosive = "explosive".equals(style.color());
                bricks.add(new LevelLoader.BrickDefinition(
                    row,
                    col,
                    style.color(),
                    style.health(),
                    style.points(),
                    isExplosive,
                    true
                ));
            }
        }

        return new LevelLoader.LevelLayout(
            DEFAULT_COLUMNS,
            BRICK_WIDTH,
            BRICK_HEIGHT,
            BRICK_SPACING,
            START_Y,
            bricks
        );
    }

    private static Map<Character, BrickStyle> createPalette(int levelNumber) {
        Map<Character, BrickStyle> palette = new HashMap<>();
        String[] colors = COLOR_SETS[(levelNumber - 51) % COLOR_SETS.length];
        int difficulty = Math.max(0, levelNumber - 51);
        int baseHealth = 1 + difficulty / 6;
        int heavyBonus = 1 + difficulty / 10;
        int basePoints = 120 + difficulty * 4;

        palette.put('A', new BrickStyle(colors[0], baseHealth, basePoints));
        palette.put('B', new BrickStyle(colors[1], baseHealth + 1, basePoints + 18));
        palette.put('C', new BrickStyle(colors[2], baseHealth + heavyBonus, basePoints + 36));
        palette.put('D', new BrickStyle(colors[3], baseHealth + heavyBonus + 1, basePoints + 56));
        palette.put('E', new BrickStyle(colors[1], baseHealth + heavyBonus + 2, basePoints + 78));
        palette.put('F', new BrickStyle(colors[2], baseHealth + heavyBonus + 3, basePoints + 96));
        palette.put('H', new BrickStyle("light_gray", baseHealth + heavyBonus + 2, basePoints + 110));
        palette.put('X', new BrickStyle("explosive", 1, basePoints + 140));
        return palette;
    }

    private static Template pickTemplate(int levelNumber) {
        if (levelNumber <= 54) {
            return Template.RING;
        } else if (levelNumber <= 57) {
            return Template.CASCADE;
        } else if (levelNumber <= 59) {
            return Template.WAVE;
        } else if (levelNumber <= 65) {
            return Template.CIRCUIT;
        } else if (levelNumber <= 69) {
            return Template.FRACTAL;
        } else if (levelNumber <= 75) {
            return Template.PILLARS;
        } else if (levelNumber <= 79) {
            return Template.LABYRINTH;
        } else if (levelNumber <= 85) {
            return Template.VECTOR;
        } else if (levelNumber <= 89) {
            return Template.SPIRAL;
        } else if (levelNumber <= 95) {
            return Template.NOVA;
        } else {
            return Template.AURORA;
        }
    }

    private static LevelLoader.LevelLayout generateBossLayout(int levelNumber) {
        BossPalette palette = bossPaletteFor(levelNumber);
        if (palette == null) {
            return null;
        }
        BossParameters parameters = bossParametersFor(levelNumber);

        List<LevelLoader.BrickDefinition> bricks = new ArrayList<>();
        double centerX = (DEFAULT_COLUMNS - 1) / 2.0;
        double centerY = (DEFAULT_ROWS - 1) / 2.0;
        int explosiveInterval = parameters.explosiveIntervalOverride() > 0
            ? parameters.explosiveIntervalOverride()
            : palette.explosiveInterval();

        for (int row = 0; row < DEFAULT_ROWS; row++) {
            for (int col = 0; col < DEFAULT_COLUMNS; col++) {
                double dx = Math.abs(col - centerX);
                double dy = Math.abs(row - centerY);
                double manhattan = dx + dy;
                double chebyshev = Math.max(dx, dy);

                if (parameters.indestructibleThickness() > 0
                    && (row < parameters.indestructibleThickness()
                        || row >= DEFAULT_ROWS - parameters.indestructibleThickness()
                        || col < parameters.indestructibleThickness()
                        || col >= DEFAULT_COLUMNS - parameters.indestructibleThickness())) {
                    bricks.add(new LevelLoader.BrickDefinition(
                        row,
                        col,
                        "indestructible",
                        Math.max(5, palette.coreHealth() + parameters.additionalHealth()),
                        palette.basePoints() + 220,
                        false,
                        true
                    ));
                    continue;
                }

                if (manhattan <= parameters.coreRadius()) {
                    bricks.add(new LevelLoader.BrickDefinition(
                        row,
                        col,
                        palette.coreColor(),
                        palette.coreHealth() + parameters.additionalHealth(),
                        palette.basePoints() + 200,
                        false,
                        true
                    ));
                    continue;
                }

                if (manhattan <= parameters.shieldRadius()) {
                    bricks.add(new LevelLoader.BrickDefinition(
                        row,
                        col,
                        palette.shieldColor(),
                        Math.max(1, palette.coreHealth() + parameters.additionalHealth() - 1),
                        palette.basePoints() + 150,
                        false,
                        true
                    ));
                    continue;
                }

                if (dx <= parameters.accentHalfWidth() && dy <= parameters.accentHalfHeight()) {
                    String color = (row + col + levelNumber) % 2 == 0
                        ? palette.accentColor()
                        : palette.shieldColor();
                    bricks.add(new LevelLoader.BrickDefinition(
                        row,
                        col,
                        color,
                        Math.max(2, palette.accentHealth() + parameters.additionalHealth()),
                        palette.basePoints() + 120,
                        false,
                        true
                    ));
                    continue;
                }

                int ringIndex = (int) Math.round(manhattan);
                if (explosiveInterval > 0
                    && manhattan >= parameters.explosiveMinRadius()
                    && manhattan <= parameters.explosiveMaxRadius()
                    && ringIndex % explosiveInterval == 0) {
                    bricks.add(new LevelLoader.BrickDefinition(
                        row,
                        col,
                        "explosive",
                        1,
                        palette.basePoints() + 180,
                        true,
                        true
                    ));
                    continue;
                }

                if (parameters.fillOuter()) {
                    String color = (row + col + levelNumber) % 2 == 0
                        ? palette.shieldColor()
                        : palette.accentColor();
                    int health = Math.max(2, palette.accentHealth() + parameters.additionalHealth());
                    int points = palette.basePoints() + 90 + (int) (chebyshev * 12);
                    bricks.add(new LevelLoader.BrickDefinition(
                        row,
                        col,
                        color,
                        health,
                        points,
                        false,
                        true
                    ));
                }
            }
        }

        return new LevelLoader.LevelLayout(
            DEFAULT_COLUMNS,
            BRICK_WIDTH,
            BRICK_HEIGHT,
            BRICK_SPACING,
            START_Y,
            bricks
        );
    }

    private static BossPalette bossPaletteFor(int levelNumber) {
        return switch (levelNumber) {
            case 60 -> new BossPalette("red", "orange", "light_gray", 6, 360, 3);
            case 70 -> new BossPalette("dark_blue", "purple", "cyan", 7, 420, 3);
            case 80 -> new BossPalette("orange", "yellow", "light_gray", 8, 480, 3);
            case 90 -> new BossPalette("pink", "purple", "red", 9, 560, 3);
            case 100 -> new BossPalette("light_gray", "yellow", "pink", 12, 720, 2);
            default -> null;
        };
    }

    private static BossParameters bossParametersFor(int levelNumber) {
        return switch (levelNumber) {
            case 60 -> new BossParameters(1.3, 2.4, 3.5, 3.0, 3.6, 5.8, 0, 0, true, 0);
            case 70 -> new BossParameters(1.4, 2.8, 3.8, 3.2, 3.4, 6.2, 0, 0, true, 1);
            case 80 -> new BossParameters(1.6, 3.1, 4.0, 3.4, 3.2, 6.6, 0, 0, true, 1);
            case 90 -> new BossParameters(1.8, 3.5, 4.2, 3.6, 3.0, 6.9, 0, 1, true, 2);
            case 100 -> new BossParameters(2.1, 3.9, 4.4, 3.8, 2.8, 7.4, 2, 1, true, 3);
            default -> new BossParameters(1.2, 2.2, 3.4, 3.0, 4.0, 5.8, 0, 0, false, 0);
        };
    }

    private enum Template {
        RING {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                double cx = (columns - 1) / 2.0;
                double cy = (rows - 1) / 2.0;
                double dx = Math.abs(col - cx);
                double dy = Math.abs(row - cy);
                double max = Math.max(dx, dy);
                double manhattan = dx + dy;

                if (max > 7) {
                    return '.';
                }
                if (manhattan >= 7.5) {
                    return (row + col) % 2 == 0 ? 'A' : '.';
                }
                if (manhattan >= 6.5) {
                    return 'A';
                }
                if (manhattan >= 5.5) {
                    return (row + col) % 2 == 0 ? 'B' : 'A';
                }
                if (manhattan >= 4.0) {
                    return (row % 2 == 0) ? 'C' : 'B';
                }
                if (max >= 2.5) {
                    return (row + col) % 2 == 0 ? 'D' : 'C';
                }
                if (max >= 1.5) {
                    return 'E';
                }
                return 'H';
            }
        },
        CASCADE {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                int value = Math.floorMod(col + row, 8);
                if (value == 0) {
                    return 'X';
                }
                if (value <= 2) {
                    return 'D';
                }
                if (value <= 4) {
                    return 'C';
                }
                if (value <= 6) {
                    return 'B';
                }
                return 'A';
            }
        },
        WAVE {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                double normalizedRow = row / (double) rows;
                double wave = Math.sin((col * 0.4) + (normalizedRow * Math.PI * 2) + levelNumber * 0.2);
                int band = (int) Math.round((wave + 1) * 2.0);
                return switch (band) {
                    case 0 -> '.';
                    case 1 -> 'A';
                    case 2 -> (row % 2 == 0 ? 'B' : 'C');
                    case 3 -> 'D';
                    case 4 -> 'E';
                    default -> 'F';
                };
            }
        },
        CIRCUIT {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                if (row == 0 || row == rows - 1 || col == 0 || col == columns - 1) {
                    return 'B';
                }
                if ((row + col + levelNumber) % 7 == 0) {
                    return 'X';
                }
                if (row % 3 == 0 && col % 2 == 0) {
                    return 'D';
                }
                if (col % 4 == 0 || row % 4 == 0) {
                    return 'C';
                }
                return 'A';
            }
        },
        FRACTAL {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                int block = ((row / 2) ^ (col / 2)) & 3;
                return switch (block) {
                    case 0 -> 'A';
                    case 1 -> 'B';
                    case 2 -> 'C';
                    case 3 -> 'D';
                    default -> 'A';
                };
            }
        },
        PILLARS {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                if (col % 3 == 0) {
                    return 'D';
                }
                if (col % 3 == 1) {
                    return (row % 2 == 0) ? 'B' : 'A';
                }
                if (row % 4 == 0) {
                    return 'C';
                }
                if ((row + col) % 5 == 0) {
                    return 'X';
                }
                return 'A';
            }
        },
        LABYRINTH {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                if (row % 2 == 0 && col % 2 == 0) {
                    return 'D';
                }
                if (row % 2 == 0) {
                    return 'C';
                }
                if (col % 4 == 0) {
                    return 'B';
                }
                if ((row + col + levelNumber) % 6 == 0) {
                    return 'X';
                }
                return 'A';
            }
        },
        VECTOR {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                int centerRow = rows / 2;
                int centerCol = columns / 2;
                if (col == centerCol) {
                    return 'H';
                }
                if (col > centerCol && Math.abs(row - centerRow) <= (col - centerCol) / 2 + 1) {
                    return 'D';
                }
                if (Math.abs(row - centerRow) <= 1) {
                    return 'C';
                }
                if ((row + col) % 5 == 0) {
                    return 'X';
                }
                return 'B';
            }
        },
        SPIRAL {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                int layer = Math.min(Math.min(row, rows - 1 - row), Math.min(col, columns - 1 - col));
                if (layer > 5) {
                    return '.';
                }
                if (layer == 0) {
                    return 'A';
                }
                if (layer == 1) {
                    return 'B';
                }
                if (layer == 2) {
                    return 'C';
                }
                if (layer == 3) {
                    return 'D';
                }
                if ((row + col + levelNumber) % 4 == 0) {
                    return 'X';
                }
                return 'E';
            }
        },
        NOVA {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                int centerRow = rows / 2;
                int centerCol = columns / 2;
                if (row == centerRow || col == centerCol) {
                    return 'H';
                }
                if (Math.abs(row - centerRow) + Math.abs(col - centerCol) <= 2) {
                    return 'E';
                }
                if ((row + col + levelNumber) % 4 == 0) {
                    return 'X';
                }
                return (row % 2 == 0) ? 'C' : 'D';
            }
        },
        AURORA {
            @Override
            char compute(int row, int col, int rows, int columns, int levelNumber) {
                double gradient = (double) row / rows;
                if (gradient < 0.2) {
                    return 'B';
                }
                if (gradient < 0.4) {
                    return (col % 2 == 0) ? 'C' : 'B';
                }
                if (gradient < 0.6) {
                    return (row + col) % 3 == 0 ? 'X' : 'D';
                }
                if (gradient < 0.8) {
                    return (col % 3 == 0) ? 'E' : 'C';
                }
                return 'F';
            }
        };

        abstract char compute(int row, int col, int rows, int columns, int levelNumber);
    }

    private record BrickStyle(String color, int health, int points) {
        static final BrickStyle DEFAULT_STYLE = new BrickStyle("purple", 2, 180);
    }

    private record BossPalette(String coreColor, String shieldColor, String accentColor,
                               int coreHealth, int basePoints, int explosiveInterval) {
        int accentHealth() {
            return Math.max(2, coreHealth - 1);
        }
    }

    private record BossParameters(double coreRadius,
                                  double shieldRadius,
                                  double accentHalfWidth,
                                  double accentHalfHeight,
                                  double explosiveMinRadius,
                                  double explosiveMaxRadius,
                                  int explosiveIntervalOverride,
                                  int indestructibleThickness,
                                  boolean fillOuter,
                                  int additionalHealth) {
    }
}
