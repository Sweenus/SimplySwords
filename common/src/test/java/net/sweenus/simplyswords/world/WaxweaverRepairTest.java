package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import net.sweenus.simplyswords.item.custom.WaxweaverSwordItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WaxweaverRepairTest {
    @Test
    void prisonBonusesComposeAgainstConfigurationAndRemainIndependent() {
        Phase7AbilityTuning tuning = Phase7AbilityTuning.EMPTY
                .with(s("WAX_PRISON_RANGE_BONUS"), 3)
                .with(s("WAX_PRISON_DURATION_BONUS_TICKS"), 20)
                .with(s("WAX_TAUNT_RADIUS_BONUS"), 2)
                .with(s("WAX_TAUNT_INTERVAL_BONUS_TICKS"), -2)
                .with(s("WAX_EXPLOSION_RADIUS_BONUS"), .75)
                .with(s("WAX_EXPLOSION_FIRE_BONUS_TICKS"), 20)
                .with(s("WAX_BRITTLE_DURATION_REDUCTION_TICKS"), 10);

        assertEquals(21, WaxweaverEncasementManager.prisonRange(18, tuning), 1.0E-6);
        assertEquals(180, WaxweaverEncasementManager.prisonDuration(160, tuning));
        assertEquals(16, WaxweaverEncasementManager.tauntRadius(14, tuning), 1.0E-6);
        assertEquals(10, WaxweaverEncasementManager.tauntInterval(12, tuning));
        assertEquals(6.75, WaxweaverEncasementManager.explosionRadius(6, tuning), 1.0E-6);
        assertEquals(140, WaxweaverEncasementManager.explosionFireTicks(120, tuning));
        assertEquals(10, tuning.integer(s("WAX_BRITTLE_DURATION_REDUCTION_TICKS"), 0));
    }

    @Test
    void prisonCapstonesRetainThePrerequisiteDurationAndDamageComposition() {
        Phase7AbilityTuning iron = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 7)
                .with(s("WAX_PRISON_DURATION_BONUS_TICKS"), 20)
                .with(s("WAX_IRON_DURATION_TICKS"), 200)
                .with(s("WAX_EXPLOSION_DAMAGE_MULTIPLIER"), 1.12)
                .with(s("WAX_IRON_DAMAGE_MULTIPLIER"), .6);
        Phase7AbilityTuning volatileWax = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 8)
                .with(s("WAX_PRISON_DURATION_BONUS_TICKS"), 20)
                .with(s("WAX_TAUNT_RADIUS_BONUS"), 2)
                .with(s("WAX_VOLATILE_DURATION_TICKS"), 60)
                .with(s("WAX_VOLATILE_DAMAGE_MULTIPLIER"), 1.9)
                .with(s("WAX_VOLATILE_TAUNT_RADIUS_MULTIPLIER"), .5);

        assertEquals(220, WaxweaverEncasementManager.prisonDuration(120, iron));
        assertEquals(.672, WaxweaverEncasementManager.explosionDamageMultiplier(iron), 1.0E-6);
        assertEquals(80, WaxweaverEncasementManager.prisonDuration(120, volatileWax));
        assertEquals(6, WaxweaverEncasementManager.tauntRadius(10, volatileWax), 1.0E-6);
        assertEquals(1.9, WaxweaverEncasementManager.explosionDamageMultiplier(volatileWax), 1.0E-6);
    }

    @Test
    void tempoCapsDurationsAndFrenzyBonusesAreExact() {
        Phase7AbilityTuning frenzy = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 16)
                .with(s("WAX_TEMPO_DURATION_BONUS_TICKS"), 20)
                .with(s("WAX_FRENZY_DURATION_TICKS"), 30);
        Phase7AbilityTuning patient = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 17)
                .with(s("WAX_TEMPO_DURATION_BONUS_TICKS"), 20)
                .with(s("WAX_PATIENT_DURATION_TICKS"), 160);

        assertEquals(1, WaxweaverSwordItem.nextTempoStacks(0, 1));
        assertEquals(1, WaxweaverSwordItem.nextTempoStacks(1, 1));
        assertEquals(4, WaxweaverSwordItem.nextTempoStacks(3, 4));
        assertEquals(50, WaxweaverSwordItem.tempoDuration(frenzy));
        assertEquals(180, WaxweaverSwordItem.tempoDuration(patient));
        assertEquals(10.5, Phase7CombatManager.frenzyDamageBonus(6, 1.5), 1.0E-6);
        assertEquals(.35, Phase7CombatManager.frenzySpeedBonus(6, 1.5), 1.0E-6);
    }

    @Test
    void revivalBonusesComposeWithoutSharingAreaSettings() {
        Phase7AbilityTuning queen = Phase7AbilityTuning.EMPTY
                .with(s("MODE"), 1 << 25)
                .with(s("WAX_REVIVE_RESISTANCE_BONUS_TICKS"), 40)
                .with(s("WAX_REVIVE_COOLDOWN_BONUS_TICKS"), -100)
                .with(s("WAX_QUEEN_RESISTANCE_TICKS"), 100)
                .with(s("WAX_QUEEN_COOLDOWN_BONUS_TICKS"), 400)
                .with(s("WAX_MOLTEN_RADIUS"), 4)
                .with(s("WAX_MOLTEN_TARGET_CAP"), 8)
                .with(s("WAX_EMERGENCE_RADIUS"), 6)
                .with(s("WAX_EMERGENCE_TARGET_CAP"), 16);

        assertEquals(2100, WaxweaverSwordItem.revivalCooldown(1800, queen));
        assertEquals(140, WaxweaverSwordItem.resistanceDuration(queen));
        assertEquals(4, queen.get(s("WAX_MOLTEN_RADIUS"), 0), 1.0E-6);
        assertEquals(8, queen.integer(s("WAX_MOLTEN_TARGET_CAP"), 0));
        assertEquals(6, queen.get(s("WAX_EMERGENCE_RADIUS"), 0), 1.0E-6);
        assertEquals(16, queen.integer(s("WAX_EMERGENCE_TARGET_CAP"), 0));

        Phase7AbilityTuning emergence = queen.with(s("MODE"), 1 << 26);
        assertEquals(0, WaxweaverSwordItem.resistanceDuration(emergence));
    }

    private static Phase7AbilityTuning.Setting s(String name) {
        return Phase7AbilityTuning.Setting.valueOf(name);
    }
}
