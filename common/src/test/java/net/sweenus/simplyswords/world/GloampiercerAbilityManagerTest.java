package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GloampiercerAbilityManagerTest {

    @Test
    void splitReflectionOnlyExpandsTheThirdCommittedActivation() {
        assertEquals(1, GloampiercerAbilityManager.nextPassiveProc(0));
        assertEquals(2, GloampiercerAbilityManager.nextPassiveProc(1));
        assertEquals(3, GloampiercerAbilityManager.nextPassiveProc(2));
        assertEquals(1, GloampiercerAbilityManager.nextPassiveProc(3));
        assertEquals(1, GloampiercerAbilityManager.passiveCloneCount(1, 1, 3));
        assertEquals(1, GloampiercerAbilityManager.passiveCloneCount(1, 2, 3));
        assertEquals(3, GloampiercerAbilityManager.passiveCloneCount(1, 3, 3));
    }
}
