package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryTuning;

public final class PlagueMasteryTuningMath {
    private PlagueMasteryTuningMath() {
    }

    public static int conversionChance(int configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.clamp(configured + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_CONVERSION_CHANCE_BONUS, 0), 0, 100);
    }

    public static int feverDuration(int configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(1, configured + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_FEVER_DURATION_BONUS_TICKS, 0));
    }

    public static int conversionFever(int configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(0, configured + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_CONVERSION_FEVER_BONUS, 0));
    }

    public static double tollRadius(double configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(.1, configured + tuning.get(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_TOLL_RADIUS_BONUS, 0));
    }

    public static int feverSpread(int configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(0, configured + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_FEVER_SPREAD_BONUS, 0));
    }

    public static int maximumTolls(int configured, DeathShadowBloodMasteryTuning tuning) {
        if (tuning.flag(1 << 26)) return 1;
        int result = Math.max(1, configured + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_CASCADE_TOLL_BONUS, 0));
        if (tuning.flag(1 << 25)) result = Math.max(1, result - tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.PLAGUE_REVISIT_TOLL_PENALTY, 2));
        int cap = tuning.integer(DeathShadowBloodMasteryTuning.Setting.PLAGUE_CASCADE_TOLL_CAP, 0);
        return cap > 0 ? Math.min(result, cap) : result;
    }

    public static float carriedDurationMultiplier(float configured, DeathShadowBloodMasteryTuning pestilence,
                                                  DeathShadowBloodMasteryTuning outbreak) {
        return (float) Math.clamp(configured
                + pestilence.get(DeathShadowBloodMasteryTuning.Setting.PLAGUE_CARRIED_DURATION_BONUS, 0)
                + outbreak.get(DeathShadowBloodMasteryTuning.Setting.PLAGUE_CARRIED_DURATION_BONUS, 0), 0, 1);
    }

    public static boolean criticalCondition(int stacks, int threshold, double fraction) {
        return stacks >= Math.max(1, (int) Math.ceil(Math.max(1, threshold)
                * Math.clamp(fraction, 0, 1)));
    }
}
