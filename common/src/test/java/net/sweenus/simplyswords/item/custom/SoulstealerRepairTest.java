package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SoulstealerRepairTest {
    @Test
    void debtChanceComposesAgainstConfigurationAndUsurerGuaranteesAProc() {
        Phase8AbilityTuning hoard = Phase8AbilityTuning.EMPTY
                .with(setting("MODE"), (1 << 0) | (1 << 8))
                .with(setting("SOULSTEALER_CHANCE_BONUS"), 8)
                .with(setting("SOULSTEALER_CHANCE_MULTIPLIER"), .5);
        assertEquals(24, StealSwordItem.soulDebtChance(40, hoard));

        Phase8AbilityTuning usurer = hoard.with(setting("MODE"), 1 << 7);
        assertEquals(100, StealSwordItem.soulDebtChance(0, usurer));
    }

    @Test
    void debtCapAndKillGainComposeAcrossPathAndCapstone() {
        Phase8AbilityTuning tuning = Phase8AbilityTuning.EMPTY
                .with(setting("SOULSTEALER_MAX_DEBT_BONUS"), 7)
                .with(setting("SOULSTEALER_KILL_DEBT_OVERRIDE"), 3)
                .with(setting("SOULSTEALER_KILL_DEBT_BONUS"), 1);
        assertEquals(12, StealSwordItem.maximumDebt(5, tuning));
        assertEquals(4, StealSwordItem.killDebt(2, tuning));
    }

    @Test
    void sharperClaimUsesEightPercentAndInstallmentsScaleOnlySpentDebt() {
        Phase8AbilityTuning sharper = Phase8AbilityTuning.EMPTY
                .with(setting("SOULSTEALER_DAMAGE_PER_DEBT_BONUS"), .08);
        StealSwordItem.ReapProfile sharperProfile = StealSwordItem.reapProfile(3,
                Phase8AbilityTuning.EMPTY, sharper, 5, 2, 5);
        float base = StealSwordItem.getBackstabMultiplier(3, 5, 2, 5);
        assertEquals(base * 1.24F, sharperProfile.multiplier(), 1.0E-5);

        Phase8AbilityTuning installment = sharper
                .with(setting("MODE"), (1 << 18) | (1 << 26))
                .with(setting("SOULSTEALER_INSTALLMENT_MAX_SPEND"), 3)
                .with(setting("SOULSTEALER_INSTALLMENT_DAMAGE_MULTIPLIER"), .8);
        StealSwordItem.ReapProfile installmentProfile = StealSwordItem.reapProfile(12,
                Phase8AbilityTuning.EMPTY, installment, 5, 2, 5);
        assertEquals(3, installmentProfile.consumed());
        assertEquals(base * 1.24F * .8F, installmentProfile.multiplier(), 1.0E-5);
    }

    @Test
    void foreclosureCapsItsBonusAndPursuitCapsBeforeEscapePenalty() {
        Phase8AbilityTuning foreclosure = Phase8AbilityTuning.EMPTY
                .with(setting("MODE"), 1 << 25)
                .with(setting("SOULSTEALER_FORECLOSURE_DAMAGE_PER_DEBT"), .2)
                .with(setting("SOULSTEALER_FORECLOSURE_STACK_CAP"), 12);
        StealSwordItem.ReapProfile uncapped = StealSwordItem.reapProfile(20,
                Phase8AbilityTuning.EMPTY, foreclosure, 5, 2, 5);
        float base = StealSwordItem.getBackstabMultiplier(20, 5, 2, 5);
        assertEquals(base * 3.4F, uncapped.multiplier(), 1.0E-5);

        Phase8AbilityTuning approach = Phase8AbilityTuning.EMPTY
                .with(setting("MODE"), (1 << 16) | (1 << 17))
                .with(setting("SOULSTEALER_PURSUIT_DAMAGE_CAP"), 3)
                .with(setting("SOULSTEALER_ESCAPE_DAMAGE_MULTIPLIER"), .75);
        assertEquals(2.25F, StealSwordItem.reapProfile(20, approach, foreclosure,
                5, 2, 5).multiplier(), 1.0E-5);
    }

    @Test
    void armorIgnoreUsesReducedArmorDamageMath() {
        float multiplier = StealSwordItem.armorIgnoreMultiplier(10, 20, 8, 2);
        assertTrue(multiplier > 1);
        assertTrue(multiplier < 1.5F);
    }

    private static Phase8AbilityTuning.Setting setting(String name) {
        return Phase8AbilityTuning.Setting.valueOf(name);
    }
}
