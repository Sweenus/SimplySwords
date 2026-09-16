package net.sweenus.simplyswords.qa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QaAbilityTimingTest {
    private QaAbilityTiming timing(String weapon, boolean ability, boolean unlocked, boolean player,
                                   int maxUseTicks, int chargeTicks, int summonTicks) {
        return QaAbilityTiming.resolve(weapon, ability, unlocked, player, maxUseTicks, 45,
                20, 100, chargeTicks, summonTicks);
    }

    @Test
    void magibladeUsesFunctionalDurationInsteadOfItemUseSentinel() {
        QaAbilityTiming timing = timing("magiblade", true, true, true, 72005, 10, 300);
        assertEquals(new QaAbilityTiming(true, 15, 330), timing);
        assertNull(timing.validationError());
        assertEquals(15, timing.validatedHoldTicks());
    }

    @Test
    void magibladeObservesCustomChargeAndSummonDurations() {
        assertEquals(new QaAbilityTiming(true, 45, 660),
                timing("magiblade", true, true, true, 72005, 40, 600));
        assertEquals(new QaAbilityTiming(true, 6, 51),
                timing("magiblade", true, true, true, 72005, 0, -1));
    }

    @Test
    void mobMagibladeObservesTheSummonWithoutHoldingInput() {
        assertEquals(new QaAbilityTiming(false, 1, 330),
                timing("magiblade", true, true, false, 72005, 10, 300));
    }

    @Test
    void attacksAndLockedAbilitiesKeepTheirShortWindow() {
        assertEquals(QaAbilityTiming.INSTANT,
                timing("magiblade", false, true, true, 72005, 10, 300));
        assertEquals(QaAbilityTiming.INSTANT,
                timing("magiblade", true, false, true, 72005, 10, 300));
    }

    @Test
    void dawnquiverAndLichbladeKeepFunctionalHoldRules() {
        assertEquals(new QaAbilityTiming(true, 20, 65),
                timing("dawnquiver", true, true, true, 72000, 10, 300));
        assertEquals(new QaAbilityTiming(true, 105, 150),
                timing("ascended_lichblade", true, true, true, 72000, 10, 300));
        assertEquals(new QaAbilityTiming(true, 30, 75),
                timing("thunderbrand", true, true, true, 30, 10, 300));
    }

    @Test
    void instantAbilitiesKeepTheirEffectWindow() {
        assertEquals(new QaAbilityTiming(false, 1, 90),
                QaAbilityTiming.resolve("arcanethyst", true, true, true, 0, 90, 20, 100, 10, 300));
    }

    @Test
    void guardAcceptsItsExactBoundary() {
        assertNull(new QaAbilityTiming(true, 2355, 2400).validationError());
        assertEquals(2355, new QaAbilityTiming(true, 2355, 2400).validatedHoldTicks());
    }

    @Test
    void guardRejectsOversizedAndInconsistentBudgetsBeforePacketConversion() {
        for (QaAbilityTiming timing : new QaAbilityTiming[] {
                new QaAbilityTiming(true, 2356, 2401),
                new QaAbilityTiming(true, 72005, 72050),
                new QaAbilityTiming(true, 0, 45),
                new QaAbilityTiming(true, -1, 45),
                new QaAbilityTiming(true, 1, 0),
                new QaAbilityTiming(true, 46, 45)}) {
            assertNotNull(timing.validationError());
            assertThrows(IllegalStateException.class, timing::validatedHoldTicks);
        }
    }

    @Test
    void extremeConfigurationCannotOverflowIntoAValidBudget() {
        QaAbilityTiming timing = timing("magiblade", true, true, true, 72005,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(2147483652L, timing.holdTicks());
        assertEquals(4294967314L, timing.observationTicks());
        assertNotNull(timing.validationError());
        assertThrows(IllegalStateException.class, timing::validatedHoldTicks);
        QaAbilityTiming lichblade = QaAbilityTiming.resolve("lichblade", true, true, true,
                72000, 45, 20, Integer.MAX_VALUE, 10, 300);
        assertTrue(lichblade.holdTicks() > Integer.MAX_VALUE);
        assertNotNull(lichblade.validationError());
    }

    @Test
    void extremeEffectBudgetCannotWrapDuringDawnquiverHoldAddition() {
        QaAbilityTiming timing = QaAbilityTiming.resolve("dawnquiver", true, true, true,
                72000, Long.MAX_VALUE, 20, 100, 10, 300);
        assertEquals(Long.MAX_VALUE, timing.observationTicks());
        assertNotNull(timing.validationError());
        assertThrows(IllegalStateException.class, timing::validatedHoldTicks);
    }

    @Test
    void unknownItemUseSentinelIsRejected() {
        QaAbilityTiming timing = timing("future_weapon", true, true, true, 72005, 10, 300);
        assertNotNull(timing.validationError());
        assertThrows(IllegalStateException.class, timing::validatedHoldTicks);
    }
}
