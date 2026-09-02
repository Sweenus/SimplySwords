package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BramblethornAbilityManagerTest {
    @Test
    void graspGeometryComposesAgainstConfiguredValues() {
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_GRASP_RANGE_BONUS"), 4)
                .with(s("BRAMBLE_GRASP_RADIUS_BONUS"), 1.5)
                .with(s("BRAMBLE_GRASP_TRAVEL_TICK_BONUS"), -3)
                .with(s("BRAMBLE_BIND_DURATION_BONUS_TICKS"), 20)
                .with(s("BRAMBLE_ANCIENT_BIND_DURATION_MULTIPLIER"), 1.5);
        assertEquals(24, BramblethornAbilityManager.graspRange(20, tuning), 1.0E-6);
        assertEquals(9.5, BramblethornAbilityManager.graspRadius(8, tuning), 1.0E-6);
        assertEquals(17, BramblethornAbilityManager.graspTravelTicks(20, tuning));
        assertEquals(180, BramblethornAbilityManager.bindingDuration(100, tuning));
    }

    @Test
    void signatureDamageAndControlChannelsComposeIndependently() {
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_PULL_MULTIPLIER"), 1.2)
                .with(s("BRAMBLE_SHARED_DAMAGE_RATIO"), .42)
                .with(s("BRAMBLE_SLAM_DAMAGE_MULTIPLIER"), 1.2)
                .with(s("BRAMBLE_LIFT_FORCE_MULTIPLIER"), 1.15)
                .with(s("BRAMBLE_ANCIENT_SHARED_DAMAGE_MULTIPLIER"), .7);
        assertEquals(.36, BramblethornAbilityManager.pullStrength(.3, tuning), 1.0E-6);
        assertEquals(.294, BramblethornAbilityManager.sharedDamageRatio(.35, tuning), 1.0E-6);
        assertEquals(1.2, BramblethornAbilityManager.slamDamageMultiplier(tuning), 1.0E-6);
        assertEquals(1.15, BramblethornAbilityManager.liftForceMultiplier(tuning), 1.0E-6);
    }

    @Test
    void graspCapstonesDoNotOverwriteConfiguredCaptureRadius() {
        Phase7AbilityTuning tangled = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_GRASP_RADIUS_BONUS"), 1.5)
                .with(s("BRAMBLE_TANGLED_TARGET_CAP"), 10)
                .with(s("BRAMBLE_TANGLED_SHARED_DAMAGE_RATIO"), .25);
        assertEquals(9.5, BramblethornAbilityManager.graspRadius(8, tangled), 1.0E-6);
        assertEquals(10, BramblethornAbilityManager.graspTargetCap(12, tangled));
        assertEquals(.25, BramblethornAbilityManager.sharedDamageRatio(.6, tangled), 1.0E-6);

        Phase7AbilityTuning hangman = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_HANGMAN_TARGET_CAP"), 1)
                .with(s("BRAMBLE_HANGMAN_LIFT_FORCE_MULTIPLIER"), 1.4)
                .with(s("BRAMBLE_HANGMAN_SLAM_DAMAGE_MULTIPLIER"), 2.25);
        assertEquals(1, BramblethornAbilityManager.graspTargetCap(12, hangman));
        assertEquals(1.4, BramblethornAbilityManager.liftForceMultiplier(hangman), 1.0E-6);
        assertEquals(2.25, BramblethornAbilityManager.slamDamageMultiplier(hangman), 1.0E-6);
    }

    @Test
    void huntBonusesUseConfigurationAndPreserveStrongerSlow() {
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_HUNT_MEMORY_BONUS_TICKS"), 40)
                .with(s("BRAMBLE_HUNT_COOLDOWN_BONUS_TICKS"), -2)
                .with(s("BRAMBLE_HUNT_RANGE_BONUS"), 2)
                .with(s("BRAMBLE_HUNT_WIDTH_BONUS"), .25)
                .with(s("BRAMBLE_HUNT_SLOW_DURATION_BONUS_TICKS"), 20)
                .with(s("BRAMBLE_HUNT_MIN_SLOW_AMPLIFIER"), 1)
                .with(s("BRAMBLE_HUNT_PATH_TARGET_CAP"), 4);
        assertEquals(120, BramblethornAbilityManager.huntMemoryDuration(80, tuning));
        assertEquals(4, BramblethornAbilityManager.huntCooldown(5, tuning));
        assertEquals(16, BramblethornAbilityManager.huntRange(14, tuning), 1.0E-6);
        assertEquals(1.25, BramblethornAbilityManager.huntWidth(1, tuning), 1.0E-6);
        assertEquals(70, BramblethornAbilityManager.huntSlowDuration(50, tuning));
        assertEquals(3, BramblethornAbilityManager.huntSlowAmplifier(3, tuning));
        assertEquals(4, BramblethornAbilityManager.huntTargetCap(tuning));
    }

    @Test
    void huntCapstonesComposeWithoutLeakingIntoGraspDamage() {
        Phase7AbilityTuning waltz = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_HUNT_DAMAGE_MULTIPLIER"), 1.1)
                .with(s("BRAMBLE_WALTZ_PROJECTILE_COUNT"), 2)
                .with(s("BRAMBLE_WALTZ_DISABLE_SLOW"), 1);
        assertEquals(1.1, BramblethornAbilityManager.huntDamageMultiplier(waltz), 1.0E-6);
        assertEquals(0, BramblethornAbilityManager.huntSlowDuration(50, waltz));
        assertEquals(1, BramblethornAbilityManager.slamDamageMultiplier(waltz), 1.0E-6);

        Phase7AbilityTuning predator = Phase7AbilityTuning.EMPTY
                .with(s("BRAMBLE_HUNT_DAMAGE_MULTIPLIER"), 1.1)
                .with(s("BRAMBLE_PREDATOR_DAMAGE_MULTIPLIER"), 1.8)
                .with(s("BRAMBLE_PREDATOR_MEMORY_TICKS"), 30)
                .with(s("BRAMBLE_PREDATOR_TARGET_ONLY"), 1)
                .with(s("BRAMBLE_HUNT_PATH_TARGET_CAP"), 4);
        assertEquals(1.98, BramblethornAbilityManager.huntDamageMultiplier(predator), 1.0E-6);
        assertEquals(30, BramblethornAbilityManager.huntMemoryDuration(80, predator));
        assertEquals(1, BramblethornAbilityManager.huntTargetCap(predator));
    }

    @Test
    void reclaimedGrowthSubtractsElapsedTimeAndEveryRefund() {
        assertEquals(208, BramblethornAbilityManager.remainingCooldown(240, 20, 12));
        assertEquals(196, BramblethornAbilityManager.remainingCooldown(240, 20, 24));
        assertEquals(0, BramblethornAbilityManager.remainingCooldown(240, 220, 48));
    }

    private static Phase7AbilityTuning.Setting s(String name) {
        return Phase7AbilityTuning.Setting.valueOf(name);
    }
}
