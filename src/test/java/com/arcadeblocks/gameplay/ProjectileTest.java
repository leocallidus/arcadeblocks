package com.arcadeblocks.gameplay;

/**
 * Lightweight unit test without external test framework to validate
 * projectile paddle-collision grace period logic.
 *
 * Run with: `gradle run` of a small harness or via IDE by executing main().
 * Uses Java assertions; enable with JVM flag -ea.
 */
public class ProjectileTest {

    public static void main(String[] args) throws Exception {
        shouldIgnorePaddleCollisionImmediatelyAfterSpawn();
        shouldNotIgnorePaddleCollisionAfterGracePeriod();
        System.out.println("ProjectileTest: OK");
    }

    private static void shouldIgnorePaddleCollisionImmediatelyAfterSpawn() {
        Projectile projectile = new Projectile();
        // Simulate FXGL adding component lifecycle
        projectile.onAdded();

        assert projectile.shouldIgnorePaddleCollision() :
            "Projectile should ignore paddle collision right after spawn";
    }

    private static void shouldNotIgnorePaddleCollisionAfterGracePeriod() throws InterruptedException {
        Projectile projectile = new Projectile();
        projectile.onAdded();

        // Wait longer than IGNORE_PADDLE_COLLISION_MS (150ms)
        Thread.sleep(200);

        assert !projectile.shouldIgnorePaddleCollision() :
            "Projectile should not ignore paddle collision after grace period expires";
    }
}
