package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HearthflameAbilityManagerTest {

    @Test
    void sixfoldSentenceRequiresAllSixInitialBindings() {
        assertEquals(10, HearthflameAbilityManager.gatedFinalDamage(10, 5, 6, 1.25), 1.0E-6);
        assertEquals(12.5F, HearthflameAbilityManager.gatedFinalDamage(10, 6, 6, 1.25), 1.0E-6);
        assertEquals(12.5F, HearthflameAbilityManager.gatedFinalDamage(10, 8, 6, 1.25), 1.0E-6);
        assertEquals(10, HearthflameAbilityManager.gatedFinalDamage(10, 8, 0, 1.25), 1.0E-6);
    }

    @Test
    void sixfoldSentenceNeedsNoConstantMirroredInTheMasteryDefinition() {
        assertEquals(15, HearthflameAbilityManager.gatedFinalDamage(10, 6, 6, 1.5), 1.0E-6);
        assertEquals(10, HearthflameAbilityManager.gatedFinalDamage(10, 5, 6, 1.5), 1.0E-6);
        assertEquals(10, HearthflameAbilityManager.gatedFinalDamage(10, 1, 1, 1.0), 1.0E-6);
    }

    @Test
    void tunedFireDurationsExtendTheWeaponIgniteInsteadOfBeingSwallowed() {
        assertEquals(4, HearthflameAbilityManager.igniteSeconds(4, 0));
        assertEquals(4, HearthflameAbilityManager.igniteSeconds(4, 40));
        assertEquals(6, HearthflameAbilityManager.igniteSeconds(4, 120));
        assertEquals(4, HearthflameAbilityManager.igniteSeconds(4, -20));
        assertEquals(6, HearthflameAbilityManager.igniteSeconds(0, 120));
    }

    @Test
    void chainPullIsBoundedAndScalesWithExcessLength() {
        assertEquals(0, HearthflameAbilityManager.pullStrength(0, 1, 4), 1.0E-6);
        assertEquals(.16 * .75, HearthflameAbilityManager.pullStrength(.16, 1, 0), 1.0E-6);
        assertEquals(.16 * 1.25, HearthflameAbilityManager.pullStrength(.16, 1, 2), 1.0E-6);
        assertEquals(.32, HearthflameAbilityManager.pullStrength(.16, 1, 100), 1.0E-6);
        assertEquals(.35, HearthflameAbilityManager.pullStrength(1, 1, 100), 1.0E-6);
    }

    @Test
    void endlessFurnaceDecayIsBoundToGeneration() {
        assertEquals(1, HearthflameAbilityManager.rebindDamageMultiplier(.75, 0), 1.0E-6);
        assertEquals(.75F, HearthflameAbilityManager.rebindDamageMultiplier(.75, 1), 1.0E-6);
        assertEquals(.5625F, HearthflameAbilityManager.rebindDamageMultiplier(.75, 2), 1.0E-6);
        assertEquals(.421875F, HearthflameAbilityManager.rebindDamageMultiplier(.75, 3), 1.0E-6);
    }

    @Test
    void furnacePressureImprovesSnapGainWithoutMovingTheThreshold() {
        assertEquals(110, HearthflameAbilityManager.pressureAfterSnap(100, 300, 1), 1.0E-6);
        assertEquals(112, HearthflameAbilityManager.pressureAfterSnap(100, 300, 1.2), 1.0E-6);
        assertEquals(300, HearthflameAbilityManager.pressureAfterSnap(298, 300, 1.2), 1.0E-6);
    }

    @Test
    void stokedResolveRefreshesUpToItsMaximum() {
        assertEquals(30, HearthflameAbilityManager.extendedResistanceTicks(0, 30, 90));
        assertEquals(75, HearthflameAbilityManager.extendedResistanceTicks(45, 30, 90));
        assertEquals(90, HearthflameAbilityManager.extendedResistanceTicks(80, 30, 90));
    }

    @Test
    void defensiveReductionsComposeMultiplicatively() {
        float afterFire = HearthflameAbilityManager.reducedDamage(100, .3);
        assertEquals(70, afterFire, 1.0E-6);
        assertEquals(59.5F, HearthflameAbilityManager.reducedDamage(afterFire, .15), 1.0E-6);
    }
}
