package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DevourerReprisalManagerTest {

    @Test
    void onlyTheTriggeringAttackerKeepsFullReprisalDamage() {
        assertEquals(10.0F, DevourerReprisalManager.targetDamage(10.0F, true, .7), 1.0E-4F);
        assertEquals(7.0F, DevourerReprisalManager.targetDamage(10.0F, false, .7), 1.0E-4F);
    }

    @Test
    void anUntunedSecondaryMultiplierLeavesDamageUnchanged() {
        assertEquals(10.0F, DevourerReprisalManager.targetDamage(10.0F, false, 1.0), 1.0E-4F);
        assertEquals(10.0F, DevourerReprisalManager.targetDamage(10.0F, false, 0.0), 1.0E-4F);
    }
}
