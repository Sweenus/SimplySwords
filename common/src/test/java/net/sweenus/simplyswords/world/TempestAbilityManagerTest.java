package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TempestAbilityManagerTest {
    @Test
    void markAndVortexDurationsStayDefinitionScopedAndConfigRelative() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("TEMPEST_MARK_DURATION_BONUS_TICKS"), 200)
                .with(s("TEMPEST_DURATION_BONUS_TICKS"), 40)
                .with(s("TEMPEST_SINGULARITY_DURATION_MULTIPLIER"), .5);

        assertEquals(700, TempestAbilityManager.resolveMarkDuration(tuning, 500));
        assertEquals(620, TempestAbilityManager.resolveVortexDuration(tuning, 1200));
    }

    @Test
    void startingRadiusBonusRetainsStackGrowth() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("TEMPEST_START_RADIUS_BONUS"), 1)
                .with(s("TEMPEST_RADIUS_PER_STACK_MULTIPLIER"), 1.1);

        assertEquals(2, TempestAbilityManager.resolveRadius(tuning, 0, 30), 1.0E-6);
        assertEquals(7.5, TempestAbilityManager.resolveRadius(tuning, 30, 30), 1.0E-6);
    }

    @Test
    void stormCrownOnlyChangesCadenceAtMaximumSize() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("TEMPEST_MAX_CADENCE_INTERVAL_TICKS"), 8);

        assertEquals(10, TempestAbilityManager.resolvePulseInterval(tuning, 29, 30));
        assertEquals(8, TempestAbilityManager.resolvePulseInterval(tuning, 30, 30));
    }

    @Test
    void capstonesComposeWithoutOverwritingConfiguredSizeOrDamage() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("TEMPEST_MAX_SIZE_MULTIPLIER"), 1.35)
                .with(s("TEMPEST_VORTEX_DAMAGE_MULTIPLIER"), 1.1)
                .with(s("TEMPEST_SINGULARITY_DAMAGE_MULTIPLIER"), 1.8)
                .with(s("TEMPEST_CONSUMED_DAMAGE_PER_STACK"), .12)
                .with(s("TEMPEST_PRISMATIC_DAMAGE_MULTIPLIER"), 1.25);

        assertEquals(41, TempestAbilityManager.resolveMaximumSize(tuning, 30));
        assertEquals(1.1 * 1.8 * 3.4 * 1.25,
                TempestAbilityManager.resolveDamageMultiplier(tuning, 20, true), 1.0E-6);
    }

    @Test
    void efficientRecallLeavesOneButAlwaysConsumesAtLeastOne() {
        assertEquals(1, TempestAbilityManager.consumedCount(1, 1));
        assertEquals(1, TempestAbilityManager.consumedCount(2, 1));
        assertEquals(7, TempestAbilityManager.consumedCount(8, 1));
        assertEquals(8, TempestAbilityManager.consumedCount(8, 0));
    }

    @Test
    void historicalGenericAddonKeysRemainFallbacks() {
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryTuning.EMPTY
                .with(s("DURATION_TICKS"), 800)
                .with(s("STACK_CAP"), 20)
                .with(s("RADIUS"), 5)
                .with(s("INTERVAL_TICKS"), 7)
                .with(s("DAMAGE_MULTIPLIER"), 1.4);

        assertEquals(800, TempestAbilityManager.resolveMarkDuration(tuning, 500));
        assertEquals(20, TempestAbilityManager.resolveMarkCap(tuning, 10));
        assertEquals(800, TempestAbilityManager.resolveVortexDuration(tuning, 1200));
        assertEquals(20, TempestAbilityManager.resolveMaximumSize(tuning, 30));
        assertEquals(5, TempestAbilityManager.resolveRadius(tuning, 12, 20), 1.0E-6);
        assertEquals(7, TempestAbilityManager.resolvePulseInterval(tuning, 12, 20));
        assertEquals(1.4, TempestAbilityManager.resolveDamageMultiplier(tuning, 0, false), 1.0E-6);
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
