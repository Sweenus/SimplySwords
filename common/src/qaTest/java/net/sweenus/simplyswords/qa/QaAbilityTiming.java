package net.sweenus.simplyswords.qa;

record QaAbilityTiming(boolean channelled, long holdTicks, long observationTicks) {
    static final long MAX_TIMING_TICKS = 2400;
    static final QaAbilityTiming INSTANT = new QaAbilityTiming(false, 1, 45);

    static QaAbilityTiming resolve(String weapon, boolean ability, boolean unlocked, boolean player,
                                   int maxUseTicks, long effectTicks, int drawTicks, int lichbladeTicks,
                                   int chargeTicks, int summonTicks) {
        if (!ability || !unlocked) return INSTANT;
        if (weapon.equals("magiblade")) {
            effectTicks = Math.max(45, Math.max(1L, chargeTicks) + Math.max(1L, summonTicks) + 20);
        }
        if (!player || maxUseTicks == 0) return new QaAbilityTiming(false, 1, effectTicks);
        long holdTicks = weapon.equals("magiblade") ? Math.max(1L, chargeTicks) + 5
                : weapon.equals("dawnquiver") ? Math.max(1L, drawTicks)
                : weapon.endsWith("lichblade") ? Math.max(1L, (long) lichbladeTicks + 5)
                : maxUseTicks;
        long observationTicks = weapon.equals("dawnquiver")
                ? saturatedAdd(holdTicks, effectTicks) : Math.max(effectTicks, holdTicks + 45);
        return new QaAbilityTiming(true, holdTicks, observationTicks);
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    String validationError() {
        if (holdTicks <= 0 || observationTicks <= 0) return "Timing budgets must be positive";
        if (observationTicks < holdTicks) return "Observation budget is shorter than hold budget";
        if (holdTicks > MAX_TIMING_TICKS || observationTicks > MAX_TIMING_TICKS) {
            return "Timing budget exceeds " + MAX_TIMING_TICKS + " ticks";
        }
        return null;
    }

    int validatedHoldTicks() {
        String error = validationError();
        if (error != null) throw new IllegalStateException(error);
        return Math.toIntExact(holdTicks);
    }
}
