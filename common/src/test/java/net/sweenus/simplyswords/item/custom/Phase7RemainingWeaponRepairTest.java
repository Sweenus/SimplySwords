package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class Phase7RemainingWeaponRepairTest {
    @Test
    void hiveheartProcBonusesComposeAgainstConfiguration() {
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 7)
                .with(s("HIVE_PROC_COOLDOWN_BONUS_TICKS"), -10)
                .with(s("HIVE_EXECUTION_COOLDOWN_MULTIPLIER"), 2);
        assertEquals(100, HiveheartSwordItem.procCooldown(60, tuning));
    }

    @Test
    void chompolotlPassiveCapstonesKeepIndependentDamageAndDuration() {
        Phase7AbilityTuning colossal = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 7)
                .with(s("CHOMP_PROC_DAMAGE_MULTIPLIER"), 1.12)
                .with(s("CHOMP_COLOSSAL_DAMAGE_MULTIPLIER"), 2.5)
                .with(s("CHOMP_COLOSSAL_SPLASH_MULTIPLIER"), 1)
                .with(s("CHOMP_COLOSSAL_SPLASH_RADIUS"), 3)
                .with(s("CHOMP_COLOSSAL_COOLDOWN_MULTIPLIER"), 3);
        assertEquals(180, ChompolotlSwordItem.procCooldown(60, colossal));
        assertEquals(2.8, ChompolotlSwordItem.summonDamageMultiplier(false, colossal), 1.0E-6);
        assertEquals(1, ChompolotlSwordItem.splashMultiplier(colossal), 1.0E-6);
        assertEquals(3, ChompolotlSwordItem.splashRadius(colossal), 1.0E-6);

        Phase7AbilityTuning release = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 8)
                .with(s("CHOMP_LIFESPAN_BONUS_TICKS"), 80)
                .with(s("CHOMP_RELEASE_COUNT"), 4)
                .with(s("CHOMP_RELEASE_LIFESPAN_TICKS"), 160);
        assertEquals(4, ChompolotlSwordItem.passiveSummonCount(release));
        assertEquals(160, ChompolotlSwordItem.summonDuration(500, false, release));
    }

    @Test
    void rallyAndGuardianPathsComposeWithoutSharingGenericSettings() {
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), (1 << 16) | (1 << 20))
                .with(s("CHOMP_BRIGADE_COUNT"), 3)
                .with(s("CHOMP_BRIGADE_AURA_MULTIPLIER"), 3)
                .with(s("CHOMP_SHOULDER_AURA_BONUS"), 2)
                .with(s("CHOMP_BLUE_DAMAGE_MULTIPLIER"), 1.25);
        assertEquals(3, ChompolotlSwordItem.activeSummonCount(tuning));
        assertEquals(21, ChompolotlSwordItem.shoulderAuraRadius(tuning), 1.0E-6);
        assertEquals(1.25, ChompolotlSwordItem.summonDamageMultiplier(true, tuning), 1.0E-6);

        Phase7AbilityTuning eternal = tuning
                .with(s("MODE"), 1 << 25)
                .with(s("CHOMP_ETERNAL_AURA_RADIUS"), 6);
        assertEquals(6, ChompolotlSwordItem.shoulderAuraRadius(eternal), 1.0E-6);
    }

    private static Phase7AbilityTuning.Setting s(String name) {
        return Phase7AbilityTuning.Setting.valueOf(name);
    }
}
