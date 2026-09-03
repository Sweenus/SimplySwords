package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LivyatanAbilityManagerTest {
    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }

    @Test
    void waveGeometryComposesAgainstConfiguredValues() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_WAVE_WIDTH_BONUS"), 1)
                .with(s("LIVYATAN_WALL_WIDTH_BONUS"), 2)
                .with(s("LIVYATAN_WAVE_LENGTH_BONUS_STEPS"), 2)
                .with(s("LIVYATAN_WAVE_KNOCKBACK_MULTIPLIER"), 1.2)
                .with(s("LIVYATAN_WALL_KNOCKBACK_MULTIPLIER"), 1.8);
        assertEquals(11, LivyatanWaveManager.waveWidth(8, tuning), 1.0E-6);
        assertEquals(12, LivyatanWaveManager.waveLength(10, tuning));
        assertEquals(1.08, LivyatanWaveManager.waveKnockback(.5, tuning), 1.0E-6);
    }

    @Test
    void riptideLanceNarrowsAndExtendsTheComposedWave() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_WAVE_WIDTH_BONUS"), 1)
                .with(s("LIVYATAN_WAVE_LENGTH_BONUS_STEPS"), 2)
                .with(s("LIVYATAN_LANCE_WIDTH"), 2)
                .with(s("LIVYATAN_LANCE_LENGTH_MULTIPLIER"), 1.5)
                .with(s("LIVYATAN_LANCE_KNOCKBACK_MULTIPLIER"), 0);
        assertEquals(2, LivyatanWaveManager.waveWidth(9, tuning), 1.0E-6);
        assertEquals(18, LivyatanWaveManager.waveLength(10, tuning));
        assertEquals(0, LivyatanWaveManager.waveKnockback(.8, tuning), 1.0E-6);
    }

    @Test
    void waveDamageBranchesComposeWithoutReachingThrowOrReturnSettings() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_WAVE_DAMAGE_MULTIPLIER"), 1.1)
                .with(s("LIVYATAN_WALL_DAMAGE_MULTIPLIER"), .6)
                .with(s("LIVYATAN_UNBOUND_WAVE_DAMAGE_MULTIPLIER"), 1.3)
                .with(s("LIVYATAN_THROW_DAMAGE_MULTIPLIER"), 1.12)
                .with(s("LIVYATAN_CALM_DAMAGE_MULTIPLIER"), 1.5);
        assertEquals(.858, LivyatanWaveManager.waveDamageMultiplier(tuning), 1.0E-6);
        assertEquals(1.12, tuning.get(s("LIVYATAN_THROW_DAMAGE_MULTIPLIER"), 1), 1.0E-6);
        assertEquals(1.5, tuning.get(s("LIVYATAN_CALM_DAMAGE_MULTIPLIER"), 1), 1.0E-6);
    }

    @Test
    void unboundDoublesOnlyTheLiveSwingCooldown() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_UNBOUND_COOLDOWN_MULTIPLIER"), 2);
        assertEquals(10, LivyatanWaveManager.swingCooldown(5, tuning));
        assertEquals(16, LivyatanWaveManager.swingCooldown(8, tuning));
        assertEquals(65, LivyatanAbilityManager.activeCooldown(65, tuning));
    }

    @Test
    void returnRadiusAndPullUseConfiguredBases() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_RETURN_RADIUS_BONUS"), 1)
                .with(s("LIVYATAN_RETURN_PULL_MULTIPLIER"), 1.25);
        assertEquals(10, LivyatanAbilityManager.returnRadius(9, tuning), 1.0E-6);
        assertEquals(.75, LivyatanAbilityManager.returnPull(.6, tuning), 1.0E-6);
    }

    @Test
    void returnCapstonesRetainChargedTideComposition() {
        StormFrostWaterMasteryTuning thunderhead = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_RETURN_LIGHTNING_DAMAGE_MULTIPLIER"), 1.15)
                .with(s("LIVYATAN_THUNDERHEAD_LIGHTNING_MULTIPLIER"), .8);
        assertEquals(.92, LivyatanAbilityManager.returnLightningMultiplier(thunderhead), 1.0E-6);
        assertEquals(100, LivyatanAbilityManager.returnLightningChance(20, thunderhead));
        assertEquals(0, LivyatanAbilityManager.returnPull(.42, thunderhead), 1.0E-6);

        StormFrostWaterMasteryTuning maelstrom = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_MAELSTROM_ROTATIONS"), 2)
                .with(s("LIVYATAN_MAELSTROM_PULL_MULTIPLIER"), 1.5);
        assertEquals(0, LivyatanAbilityManager.returnLightningChance(20, maelstrom));
        assertEquals(.63, LivyatanAbilityManager.returnPull(.42, maelstrom), 1.0E-6);
    }

    @Test
    void calmBeforeAddsToTheConfiguredActiveCooldown() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("LIVYATAN_ACTIVE_COOLDOWN_BONUS_TICKS"), 20);
        assertEquals(85, LivyatanAbilityManager.activeCooldown(65, tuning));
        assertEquals(120, LivyatanAbilityManager.activeCooldown(100, tuning));
    }
}
