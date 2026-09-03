package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BloodwakeRepairTest {
    @Test
    void burstRadiusAndDamageComposeWithoutSharedKeys() {
        Phase8AbilityTuning spatter = tuning((1 << 1) | (1 << 2), "BLOOD_BURST_RADIUS_BONUS", .5)
                .with(s("BLOOD_BURST_DAMAGE_MULTIPLIER"), 1.12);
        assertEquals(4.5, BloodwakeAbilityManager.burstRadius(4, spatter), 1.0E-6);
        assertEquals(1.12, BloodwakeAbilityManager.burstDamageMultiplier(spatter), 1.0E-6);

        Phase8AbilityTuning sacrament = spatter.with(s("MODE"), (1 << 1) | (1 << 2) | (1 << 8))
                .with(s("BLOOD_SACRAMENT_RADIUS_MULTIPLIER"), .65)
                .with(s("BLOOD_SACRAMENT_DAMAGE_MULTIPLIER"), .7);
        assertEquals(2.925, BloodwakeAbilityManager.burstRadius(4, sacrament), 1.0E-6);
        assertEquals(.784, BloodwakeAbilityManager.burstDamageMultiplier(sacrament), 1.0E-6);

        Phase8AbilityTuning spray = spatter.with(s("MODE"), (1 << 1) | (1 << 2) | (1 << 7))
                .with(s("BLOOD_SPRAY_DAMAGE_MULTIPLIER"), 1.35);
        assertEquals(1.512, BloodwakeAbilityManager.burstDamageMultiplier(spray), 1.0E-6);
    }

    @Test
    void contagiousBloodLengthensTheConfiguredBleed() {
        Phase8AbilityTuning contagious = tuning(1 << 3, "BLOOD_BLEED_DURATION_BONUS_TICKS", 40);
        assertEquals(200, BloodwakeAbilityManager.burstBleedDuration(160, contagious));
        assertEquals(160, BloodwakeAbilityManager.burstBleedDuration(160, Phase8AbilityTuning.EMPTY));
    }

    @Test
    void riteCooldownAndDamageComposeAgainstConfiguration() {
        Phase8AbilityTuning cheap = tuning(1 << 9, "BLOOD_COOLDOWN_BONUS_TICKS", -2);
        assertEquals(8, BloodwakeAbilityManager.riteCooldown(10, cheap));

        Phase8AbilityTuning ascending = cheap.with(s("MODE"), (1 << 9) | (1 << 16))
                .with(s("BLOOD_COOLDOWN_BONUS_TICKS"), 28)
                .with(s("BLOOD_RITE_DAMAGE_MULTIPLIER"), .85);
        assertEquals(38, BloodwakeAbilityManager.riteCooldown(10, ascending));
        assertEquals(.85, BloodwakeAbilityManager.riteDamageMultiplier(ascending), 1.0E-6);
        assertEquals(.85, BloodwakeAbilityManager.bladeDamageMultiplier(ascending), 1.0E-6);

        Phase8AbilityTuning blades = ascending.with(s("BLOOD_BLADE_DAMAGE_MULTIPLIER"), 1.1);
        assertEquals(.935, BloodwakeAbilityManager.bladeDamageMultiplier(blades), 1.0E-6);
    }

    @Test
    void riteProfilesComposeAgainstTheirOwnConfiguredValues() {
        assertEquals(13, BloodwakeAbilityManager.waveSteps(10,
                tuning(1 << 10, "BLOOD_WAVE_STEP_BONUS", 3)));
        assertEquals(8, BloodwakeAbilityManager.screamTargets(6,
                tuning(1 << 11, "BLOOD_SCREAM_TARGET_BONUS", 2)));
        assertEquals(24, BloodwakeAbilityManager.bladeHoverTicks(30,
                tuning(1 << 12, "BLOOD_BLADE_HOVER_BONUS_TICKS", -6)));

        Phase8AbilityTuning carrion = tuning(1 << 13, "BLOOD_FLY_COUNT_BONUS", 2)
                .with(s("BLOOD_FLY_COUNT_CAP"), 12);
        assertEquals(10, BloodwakeAbilityManager.bloodFlyCount(8, carrion));
        assertEquals(12, BloodwakeAbilityManager.bloodFlyCount(20, carrion));

        Phase8AbilityTuning redTide = tuning(1 << 14, "BLOOD_DELUGE_INTERVAL_BONUS", -3)
                .with(s("BLOOD_DELUGE_INTERVAL_FLOOR"), 8);
        assertEquals(12, BloodwakeAbilityManager.delugeInterval(15, redTide));
        assertEquals(8, BloodwakeAbilityManager.delugeInterval(9, redTide));
    }

    @Test
    void stainProfilesComposeAndCapstonesStayIndependent() {
        Phase8AbilityTuning lasting = tuning(1 << 18, "BLOOD_STAIN_DURATION_BONUS_TICKS", 100);
        assertEquals(700, BloodStainManager.stainDuration(600, lasting));

        Phase8AbilityTuning thick = tuning(1 << 19, "BLOOD_STAIN_SLOW_BONUS", 1)
                .with(s("BLOOD_STAIN_SLOW_CAP"), 1);
        assertEquals(1, BloodStainManager.slowAmplifier(0, thick));
        assertEquals(1, BloodStainManager.slowAmplifier(3, thick));

        Phase8AbilityTuning renewal = tuning(1 << 20, "BLOOD_STAIN_HEAL_INTERVAL_BONUS", -8)
                .with(s("BLOOD_STAIN_HEAL_INTERVAL_FLOOR"), 20);
        assertEquals(32, BloodStainManager.healInterval(40, renewal));
        assertEquals(20, BloodStainManager.healInterval(24, renewal));

        Phase8AbilityTuning rich = tuning(1 << 21, "BLOOD_STAIN_HEAL_BONUS", .5);
        assertEquals(1.5F, BloodStainManager.healAmount(1.0F, rich), 1.0E-6F);

        Phase8AbilityTuning sea = rich.with(s("MODE"), (1 << 21) | (1 << 25))
                .with(s("BLOOD_SEA_RADIUS_MULTIPLIER"), 1.6)
                .with(s("BLOOD_STAIN_DURATION_BONUS_TICKS"), 200);
        assertEquals(0F, BloodStainManager.healAmount(1.0F, sea), 1.0E-6F);
        assertEquals(6.4, BloodStainManager.stainRadius(4, sea), 1.0E-6);
        assertEquals(800, BloodStainManager.stainDuration(600, sea));

        Phase8AbilityTuning heartpool = rich.with(s("MODE"), (1 << 21) | (1 << 26))
                .with(s("BLOOD_HEARTPOOL_RADIUS_MULTIPLIER"), .6)
                .with(s("BLOOD_HEARTPOOL_HEAL_MULTIPLIER"), 2);
        assertEquals(3.0F, BloodStainManager.healAmount(1.0F, heartpool), 1.0E-6F);
        assertEquals(2.4, BloodStainManager.stainRadius(4, heartpool), 1.0E-6);
        assertEquals(40, BloodStainManager.healInterval(40, heartpool));
    }

    private static Phase8AbilityTuning tuning(int mode, String setting, double value) {
        return Phase8AbilityTuning.EMPTY.with(s("MODE"), mode).with(s(setting), value);
    }

    private static Phase8AbilityTuning.Setting s(String name) {
        return Phase8AbilityTuning.Setting.valueOf(name);
    }
}
