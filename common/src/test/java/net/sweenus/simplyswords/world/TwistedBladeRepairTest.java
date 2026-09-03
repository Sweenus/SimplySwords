package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TwistedBladeRepairTest {
    @Test
    void ferocityChanceDurationAndCapsComposeAgainstConfiguration() {
        Phase8AbilityTuning path = Phase8AbilityTuning.EMPTY
                .with(s("MODE"), (1 << 0) | (1 << 1) | (1 << 6))
                .with(s("TWISTED_CHANCE_BONUS"), 8)
                .with(s("TWISTED_DURATION_BONUS_TICKS"), 30)
                .with(s("TWISTED_MAX_STACK_BONUS"), 3);
        assertEquals(48, TwistedBladeAbilityManager.ferocityChance(40, path));
        assertEquals(150, TwistedBladeAbilityManager.ferocityDuration(120, path));
        assertEquals(20, TwistedBladeAbilityManager.maximumStacks(17, path));

        Phase8AbilityTuning endless = path.with(s("MODE"), 1 << 7)
                .with(s("TWISTED_ENDLESS_MAX_STACKS"), 10);
        assertEquals(10, TwistedBladeAbilityManager.maximumStacks(17, endless));

        Phase8AbilityTuning fever = path.with(s("MODE"), 1 << 8)
                .with(s("TWISTED_FEVER_MAX_STACKS"), 20)
                .with(s("TWISTED_FEVER_DURATION_TICKS"), 80);
        assertEquals(20, TwistedBladeAbilityManager.maximumStacks(17, fever));
        assertEquals(80, TwistedBladeAbilityManager.ferocityDuration(120, fever));
    }

    @Test
    void overwoundUsesFivePercentOnlyForOverflowStacks() {
        Phase8AbilityTuning tuning = Phase8AbilityTuning.EMPTY
                .with(s("MODE"), (1 << 2) | (1 << 6))
                .with(s("TWISTED_ATTACK_SPEED_PER_STACK_BONUS"), .01)
                .with(s("TWISTED_OVERFLOW_THRESHOLD"), 15)
                .with(s("TWISTED_OVERFLOW_ATTACK_SPEED_PER_STACK"), .05);
        assertEquals(2.1, TwistedBladeAbilityManager.attackSpeedBonus(18, .12, tuning), 1.0E-6);
    }

    @Test
    void crescendoBonusesAndOrchestraComposeFromLiveValues() {
        Phase8AbilityTuning tuning = Phase8AbilityTuning.EMPTY
                .with(s("MODE"), (1 << 10) | (1 << 12) | (1 << 17))
                .with(s("TWISTED_CRESCENDO_RADIUS_BONUS"), .4)
                .with(s("TWISTED_CRESCENDO_KNOCKBACK_BONUS"), .15)
                .with(s("TWISTED_ORCHESTRA_RADIUS_MULTIPLIER"), 1.75)
                .with(s("TWISTED_ORCHESTRA_KNOCKBACK_MULTIPLIER"), 0);
        assertEquals(5.075, TwistedBladeAbilityManager.crescendoRadius(2.5, tuning), 1.0E-6);
        assertEquals(0, TwistedBladeAbilityManager.crescendoKnockback(.25, tuning), 1.0E-6);

        Phase8AbilityTuning accelerated = tuning.with(s("TWISTED_CRESCENDO_INTERVAL_BONUS"), -1);
        assertEquals(4, TwistedBladeAbilityManager.crescendoBaseInterval(5, accelerated));

        Phase8AbilityTuning solo = Phase8AbilityTuning.EMPTY
                .with(s("MODE"), (1 << 9) | (1 << 16))
                .with(s("TWISTED_CRESCENDO_DAMAGE_MULTIPLIER"), 1.1)
                .with(s("TWISTED_SOLO_DAMAGE_MULTIPLIER"), 1.9);
        assertEquals(.7315F, TwistedBladeAbilityManager.crescendoScaling(.35F, solo, false), 1.0E-6);
    }

    @Test
    void finaleBonusesInterpolateAndSustainedPenaltyAppliesOnce() {
        Phase8AbilityTuning tuning = Phase8AbilityTuning.EMPTY
                .with(s("MODE"), (1 << 19) | (1 << 20) | (1 << 21) | (1 << 22) | (1 << 26))
                .with(s("TWISTED_FINALE_MIN_DAMAGE_MULTIPLIER"), 1.15)
                .with(s("TWISTED_FINALE_MAX_DAMAGE_MULTIPLIER"), 1.2)
                .with(s("TWISTED_FINALE_RADIUS_BONUS"), .5)
                .with(s("TWISTED_FINALE_KNOCKBACK_BONUS"), .2)
                .with(s("TWISTED_SUSTAINED_DAMAGE_MULTIPLIER"), .55);
        assertEquals(.31625F, TwistedBladeAbilityManager.finaleDamageScaling(0, .5F, 2.5F, tuning), 1.0E-6);
        assertEquals(1.65F, TwistedBladeAbilityManager.finaleDamageScaling(1, .5F, 2.5F, tuning), 1.0E-6);
        assertEquals(3, TwistedBladeAbilityManager.finaleRadius(0, 2.5, 4, tuning), 1.0E-6);
        assertEquals(4.5, TwistedBladeAbilityManager.finaleRadius(1, 2.5, 4, tuning), 1.0E-6);
        assertEquals(.55, TwistedBladeAbilityManager.finaleKnockback(0, .35, .9, tuning), 1.0E-6);
        assertEquals(1.1, TwistedBladeAbilityManager.finaleKnockback(1, .35, .9, tuning), 1.0E-6);
    }

    @Test
    void finaleWindowsAndSustainedConsumptionHonorOverrides() {
        Phase8AbilityTuning patient = Phase8AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 18)
                .with(s("TWISTED_FINALE_WINDOW_BONUS_TICKS"), 30);
        assertEquals(130, TwistedBladeAbilityManager.finaleWindow(100, patient));

        Phase8AbilityTuning shatter = patient.with(s("MODE"), 1 << 25)
                .with(s("TWISTED_SHATTER_WINDOW_TICKS"), 40)
                .with(s("TWISTED_SHATTER_RADIUS"), 5);
        assertEquals(40, TwistedBladeAbilityManager.finaleWindow(100, shatter));
        assertEquals(5, TwistedBladeAbilityManager.finaleRadius(0, 2.5, 4, shatter), 1.0E-6);
        assertEquals(3, TwistedBladeAbilityManager.sustainedConsumption(7, .5));
        assertEquals(1, TwistedBladeAbilityManager.sustainedConsumption(1, .5));
    }

    private static Phase8AbilityTuning.Setting s(String name) {
        return Phase8AbilityTuning.Setting.valueOf(name);
    }
}
