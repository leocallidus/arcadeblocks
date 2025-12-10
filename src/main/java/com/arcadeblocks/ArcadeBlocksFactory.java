package com.arcadeblocks;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.gameplay.*;
import com.arcadeblocks.util.TextureUtils;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

/**
 * Фабрика сущностей для игры Arcade Blocks
 */
public class ArcadeBlocksFactory implements EntityFactory {
    
    @Spawns("paddle")
    public Entity newPaddle(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);
        physics.setFixtureDef(new com.almasb.fxgl.physics.box2d.dynamics.FixtureDef()
            .friction(0.0f)
            .restitution(1.0f)); // Полная упругость для отскока
        
        var texture = TextureUtils.loadScaledTexture("paddle.png", GameConfig.PADDLE_WIDTH, GameConfig.PADDLE_HEIGHT);
        
        return entityBuilder(data)
            .type(EntityType.PADDLE)
            .bbox(new HitBox(BoundingShape.box(GameConfig.PADDLE_WIDTH, GameConfig.PADDLE_HEIGHT)))
            .view(texture)
            .with(physics)
            .collidable()
            .build();
    }
    
    @Spawns("ball")
    public Entity newBall(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        
        // Настройка физики с улучшенным обнаружением столкновений
        com.almasb.fxgl.physics.box2d.dynamics.FixtureDef fixtureDef = 
            new com.almasb.fxgl.physics.box2d.dynamics.FixtureDef()
                .friction(0.0f)         // Нет трения
                .restitution(1.0f)      // Полная упругость
                .density(0.1f);         // Очень легкий мяч для невесомости
        physics.setFixtureDef(fixtureDef);
        
        // Включаем Continuous Collision Detection (CCD) для предотвращения туннелирования
        // Это критично для быстро движущихся объектов
        physics.setOnPhysicsInitialized(() -> {
            try {
                var body = physics.getBody();
                if (body != null) {
                    body.setBullet(true); // Включаем режим "пули" для непрерывного обнаружения столкновений
                }
            } catch (Exception e) {
                System.err.println("Не удалось активировать CCD для мяча: " + e.getMessage());
            }
        });
        
        int ballSize = GameConfig.BALL_RADIUS * 2;
        var texture = TextureUtils.loadScaledTexture("ball.png", ballSize);
        
        return entityBuilder(data)
            .type(EntityType.BALL)
            .bbox(new HitBox(BoundingShape.circle(GameConfig.BALL_RADIUS)))
            .view(texture)
            .with(physics)
            .collidable()
            .build();
    }
    
    @Spawns("brick")
    public Entity newBrick(SpawnData data) {
        // Получаем цвет кирпича из data, если указан
        String color = data.get("color");
        String sprite = "purple_brick.png";
        
        if (color != null) {
            switch (color) {
                case "blue": sprite = "blue_brick.png"; break;
                case "green": sprite = "green_brick.png"; break;
                case "pink": sprite = "pink_brick.png"; break;
                case "purple": sprite = "purple_brick.png"; break;
                case "yellow": sprite = "yellow_brick.png"; break;
                case "orange": sprite = "yellow_brick.png"; break;
                case "red": sprite = "pink_brick.png"; break;
                case "light_gray": sprite = "light_gray_brick.png"; break;
                case "explosive": sprite = "explosive_bricks.png"; break;
                case "dark_blue": sprite = "dark_blue_brick.png"; break;
            }
        }
        
        var texture = FXGL.texture(sprite, GameConfig.BRICK_WIDTH, GameConfig.BRICK_HEIGHT);
        
        Entity entity = entityBuilder(data)
            .type(EntityType.BRICK)
            .bbox(new HitBox(BoundingShape.box(GameConfig.BRICK_WIDTH, GameConfig.BRICK_HEIGHT)))
            .view(texture)
            .collidable()
            .build();
        
        entity.setProperty("sprite", sprite);
        if (color != null) {
            entity.setProperty("color", color);
        }
        
        return entity;
    }
    
    @Spawns("powerup")
    public Entity newPowerUp(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        
        var texture = TextureUtils.loadScaledTexture("bonus_score.png", 39);
        
        return entityBuilder(data)
            .type(EntityType.POWERUP)
            .bbox(new HitBox(BoundingShape.circle(20)))
            .view(texture)
            .with(physics)
            .collidable()
            .build();
    }
    
    @Spawns("bonus")
    public Entity newBonus(SpawnData data) {
        // Получаем тип бонуса из data
        com.arcadeblocks.gameplay.BonusType bonusType = data.get("bonusType");
        if (bonusType == null) {
            bonusType = com.arcadeblocks.gameplay.BonusType.BONUS_SCORE; // По умолчанию
        }
        
        // Загружаем текстуру бонуса (масштаб под 1024px исходники)
        var texture = TextureUtils.loadScaledTexture(bonusType.getTextureName(), 81, 42);

        boolean invisibleCapsule = false;
        try {
            int lvl = FXGL.geti("level");
            invisibleCapsule = (lvl == 100 || lvl == 1021);
        } catch (Exception ignored) {
        }

        if (invisibleCapsule) {
            texture.setOpacity(0.0);
            texture.setVisible(false);
        }
        
        Entity entity = entityBuilder(data)
            .type(EntityType.BONUS)
            .bbox(new HitBox(BoundingShape.box(81, 42)))
            .view(texture)
            .collidable()
            .build();

        if (invisibleCapsule) {
            entity.setProperty("forceInvisibleCapsule", true);
        }

        return entity;
    }
    
    @Spawns("projectile")
    public Entity newProjectile(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        Projectile projectileComponent = new Projectile();

        double shotWidth = 18;
        double shotHeight = 27;
        var texture = TextureUtils.loadScaledTexture("plasma_shot.png", shotWidth, shotHeight);
        texture.setTranslateX(-shotWidth / 2.0);
        texture.setTranslateY(-shotHeight / 2.0);

        Entity projectile = entityBuilder(data)
            .type(EntityType.PROJECTILE)
            .bbox(new HitBox(new Point2D(-shotWidth / 2.0, -shotHeight / 2.0), BoundingShape.box(shotWidth, shotHeight)))
            .view(texture)
            .with(physics)
            .with(projectileComponent)
            .collidable()
            .build();

        try {
            Object ownerObj = data.get("owner");
            if (ownerObj != null) {
                projectileComponent.setOwner(ownerObj.toString());
            }
        } catch (Exception ignored) {}

        return projectile;
    }
    
    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        
        // Проверяем, является ли это дополнительной стеной (защитный барьер)
        Boolean isProtectiveWall = data.get("isProtectiveWall");
        boolean isExtraWall = (isProtectiveWall != null && isProtectiveWall);
        
        // Выбираем текстуру в зависимости от типа стены
        if (isExtraWall) {
            // Используем текстуру для дополнительной стены
            var texture = FXGL.texture("extra_wall.png", width, height);
            
            // Добавляем PhysicsComponent для корректной работы CCD (предотвращение туннелирования)
            PhysicsComponent physics = new PhysicsComponent();
            physics.setBodyType(BodyType.STATIC);
            physics.setFixtureDef(new com.almasb.fxgl.physics.box2d.dynamics.FixtureDef()
                .friction(0.0f)
                .restitution(1.0f));
            
            return entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(texture)
                .with(physics)
                .collidable()
                .build();
        } else {
            // Обычная стена с цветом
            return entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(new Rectangle(width, height, Color.web("#404040")))
                .collidable()
                .build();
        }
    }
    
    @Spawns("floor")
    public Entity newFloor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        
        return entityBuilder(data)
            .type(EntityType.FLOOR)
            .bbox(new HitBox(BoundingShape.box(width, height)))
            .view(new Rectangle(width, height, Color.web(GameConfig.NEON_GREEN)))
            .collidable()
            .build();
    }

    private Boolean extractValue(SpawnData data, String key) {
        try {
            Object value = data.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (Exception ignored) {
        }
        return Boolean.FALSE;
    }

    private double extractDouble(SpawnData data, String key) {
        try {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Exception ignored) {
        }
        return Double.NaN;
    }
}
