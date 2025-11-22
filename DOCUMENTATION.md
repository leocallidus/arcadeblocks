## Project Overview

Arcade Blocks is a modern arcade-style block breaking game (Arkanoid-style) built with JavaFX and the FXGL game engine. The game features 116 levels with boss battles, powerups, a story mode, and localization support (English/Russian).

**Main Application**: `com.arcadeblocks.ArcadeBlocksApp` (7700+ lines - central game logic)

**Technology Stack**:
- Java 21
- FXGL 21.1 game engine
- JavaFX 21 for UI
- VLC (via VLCJ 4.8.2) for boss battle videos
- SDL2 + SDL2_mixer (via JNA) for cross-platform audio
- SQLite for persistent storage
- Jackson for JSON level data

## Build Commands

### Development
```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Run tests
./gradlew test

# Run a specific test class
./gradlew test --tests com.arcadeblocks.utils.DatabaseIntegrityTest

# Clean build artifacts
./gradlew clean
```

### Distribution Builds

```bash
# Create JAR with dependencies
./gradlew jar

# Linux AppImage with embedded JRE
./gradlew createLinuxAppImage

# Windows portable (no installer)
./gradlew createWindowsPortable

# Windows installer (.exe)
./gradlew createWindowsExe

# Download/setup native library directories
./gradlew downloadNativeLibraries
```

## Architecture Overview

### Core Game Loop (ArcadeBlocksApp.java)

The main application class extends `GameApplication` from FXGL. It's a massive class (~7700 lines) that orchestrates:
- Game state management (menu, gameplay, boss battles, story sequences)
- UI view transitions and lifecycle
- Collision detection handlers
- Powerup system logic
- Audio/video backend initialization
- Save/load game state

**Key responsibilities**:
- `initSettings()` - Game window and engine configuration
- `initGame()` - Spawn paddle, ball, load levels, initialize walls
- `initPhysics()` - Register collision handlers (ball-brick, ball-paddle, bonus-paddle, projectile-brick, etc.)
- `initUI()` - Set up HUD, menus, overlays
- `onUpdate(double tpf)` - Per-frame game logic (paddle movement, ball physics, bonus timers, boss AI)

### Entity System (FXGL-based)

**Entity Types** (`EntityType.java`):
- PADDLE, BALL, BRICK, BONUS, POWERUP, PROJECTILE, WALL, FLOOR, BOSS

**Entity Factory** (`ArcadeBlocksFactory.java`):
- `@Spawns` annotated methods create entities with physics, collision boxes, and textures
- Paddle: kinematic body with restitution=1.0
- Ball: dynamic body with CCD (bullet mode) to prevent tunneling
- Bricks: static collidable entities with health/color properties
- Bonuses: falling capsules with different powerup types

**Components** (attach to entities):
- `Ball`, `Brick`, `Paddle`, `Bonus`, `Projectile`, `Boss` - gameplay behavior components

### Level System

**Levels**: 116 JSON files in `src/main/resources/assets/levels/`
- Define brick layout (position, color, health, points)
- Boss levels: 50, 100, 116
- Procedural generation fallback via `ProceduralLevelGenerator`

**LevelLoader**: Parses JSON, spawns bricks, applies level-specific bonus configurations

**LevelManager**: Handles level progression, difficulty scaling, chapter transitions

### Audio System (SDL2AudioManager)

**Critical for Linux compatibility**: JavaFX Media has issues on Linux, so SDL2 is used via JNA bindings.

**Architecture**:
- `SDL2AudioManager` - Main audio interface using SDL2_mixer
- `NativeLibraryLoader` - Platform detection and native library extraction
- Libraries extracted to temp directory at runtime from `src/main/resources/natives/{platform}/`

**Supported platforms**:
- linux-x64, linux-aarch64
- windows-x64, windows-aarch64
- macos-x64, macos-aarch64

**Features**:
- Separate volume controls for music/SFX
- Sound caching
- Sequential playback queue

### Video System (VLCJ)

**Used for boss battle cutscenes** - JavaFX Media support was removed.

**Architecture**:
- `VideoBackendFactory` - Creates backend (VLC or Stub)
- `VlcjMediaBackend` - VLCJ 4 implementation
- `StubVideoBackend` - Fallback when VLC unavailable
- `VlcContext` - Singleton VLC initialization
- `VideoResourceExtractor` - Extracts video files from resources

**Graceful degradation**: If VLC not found, game continues without videos.

### UI Architecture

All UI views implement cleanup patterns to prevent memory leaks:
- `SupportsCleanup` interface for views that need disposal
- `UINodeCleanup` utility for recursive cleanup
- Views in `com.arcadeblocks.ui/`:
  - `MainMenuView` - Main menu
  - `GameplayUIView` - In-game HUD (lives, score, level)
  - `PauseView` - Pause menu
  - `SettingsView` - Settings and controls
  - `ChapterStoryView` - Story text + video player
  - `GameOverView`, `LevelIntroView`, `CreditsView`, etc.
  - Debug views: `DebugMenuView`, `DebugLevelsView`, `DebugBonusesView`

**UI Lifecycle**:
1. View created and added to scene
2. Animations/timers started
3. On cleanup: stop animations, clear references, remove from scene
4. Dispose of video/audio resources

### Persistence

**SaveManager**: Handles game progress
- SQLite database: `arcade_blocks_save.db`
- Tracks: unlocked levels, high scores, completion status
- Location: `~/.arcadeblocks/` (Linux/Mac) or `%APPDATA%/ArcadeBlocks/` (Windows)

**GameSnapshot**: Serializable mid-game state for continue feature

**Settings**: Stored in `arcade_blocks_settings.dat` (audio volumes, controls, language)

### Powerup/Bonus System

**BonusType enum** defines all powerups:
- Positive: EXTRA_LIFE, SHIELD, TURBO_BALL, EXPAND_PADDLE, PLASMA_CANNON, CALL_BALL, etc.
- Negative: SHRINK_PADDLE, CHAOS_BALLS, GHOST_PADDLE, FROZEN_PADDLE

**Configuration**:
- `BonusConfig` - Default drop chances and durations
- `LevelConfig` - Per-level bonus overrides (e.g., level 100 has invisible capsules)

**Mechanics**:
- Bonuses drop from destroyed bricks (configurable chance)
- Paddle collision activates bonus
- Active bonuses tracked with timers in UI

### Localization

**LocalizationManager**: Loads property files
- `src/main/resources/i18n/messages_en.properties`
- `src/main/resources/i18n/messages_ru.properties`

**Story text**: `StoryConfig` + `ChapterStoryData` for level-specific narratives

## Important Configuration

### Game Constants (GameConfig.java)

- **Game world**: Always 1600x900 (internal resolution)
- **Window size**: Configurable via `Resolution` (1600x900, 1920x1080)
- **Paddle**: 156x26px
- **Ball**: 10px radius
- **Brick**: 80x30px
- **Ball speed**: 650 units/sec
- **Paddle speed**: 400 units/sec
- **Lives**: 6 initial
- **Total levels**: 116

### Physics Tuning

The ball uses **Continuous Collision Detection (CCD)** to prevent tunneling through bricks at high speeds:
```java
body.setBullet(true); // in ArcadeBlocksFactory
```

All collisions use `restitution=1.0` (perfect elasticity) for predictable bounces.

## Testing

Tests use JUnit 5 + Mockito + TestFX:
- `DatabaseIntegrityTest` - Validates save system
- `ProjectileTest` - Projectile behavior
- `MemoryLeakTests` - Detect entity leaks
- `UITransitionMemoryLeakTests` - UI cleanup validation
- `VlcWarningTest` - Video backend checks

**TestFX** requires headless mode for CI. Run tests with:
```bash
./gradlew test -Dtestfx.robot=glass -Dtestfx.headless=true
```

## Native Libraries

**Critical for distribution**: Native libraries (SDL2, VLC) must be bundled.

**Structure**:
```
src/main/resources/natives/
├── linux-x64/
├── linux-aarch64/
├── windows-x64/
├── windows-aarch64/
├── macos-x64/
└── macos-aarch64/
```

**Runtime behavior**:
- `NativeLibraryLoader` detects platform
- Extracts libraries to temp directory
- Sets `java.library.path`
- Deletes temp files on JVM shutdown

**For Windows builds**: VLC DLLs are required for video playback. If missing, game falls back to `StubVideoBackend`.

## Common Development Patterns

### Adding a New Powerup

1. Add enum to `BonusType` with texture name
2. Update `BonusConfig` with drop chance/duration
3. Implement activation logic in `ArcadeBlocksApp.activateBonus()`
4. Add deactivation in `deactivateBonus()`
5. Update UI in `GameplayUIView` or `BonusTimerView`
6. Add localization keys to `messages_*.properties`

### Adding a New Level

1. Create `levelXXX.json` in `src/main/resources/assets/levels/`
2. Define brick layout with positions, colors, health, points
3. Optional: Add level-specific bonus config in `LevelConfig`
4. Optional: Add story text in `StoryConfig`

### Creating a New Boss

1. Extend `Boss` component in `com.arcadeblocks.bosses/`
2. Implement `onUpdate()` for AI patterns
3. Add projectile shooting logic
4. Create boss entity in level JSON or programmatically
5. Add boss video in `src/main/resources/assets/videos/`

## Memory Management Notes

The game has strict cleanup protocols to prevent memory leaks in long play sessions:

**UI Views**: Must implement `SupportsCleanup` and call `UINodeCleanup.cleanup(node)`
**Entities**: Remove from world with `entity.removeFromWorld()`
**Timers**: Always clear with `timer.cancel()` or `timer.clear()`
**Animations**: Stop and set to null
**Audio**: Dispose streams with `audioManager.cleanup()`

## Debugging

**Debug menu**: Accessible via `DebugMenuView` (usually bound to a special key combo)
- Level select: `DebugLevelsView`
- Bonus testing: `DebugBonusesView`
- Trigger any powerup or jump to any level

**Console logging**: Uses FXGL's logger (outputs to `logs/` directory)

## File Exclusions

- `SimpleApp.java` - Excluded from compilation (build.gradle line 110)
- VLC DLLs previously bundled in `src/main/resources/natives/windows-x64/` - now deleted, using system VLC or extracting at runtime
