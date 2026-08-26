package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrimstoneEruptionManagerTest {

    @Test
    void defeatedPrimaryStillProvidesAnEruptionOrigin() {
        assertTrue(BrimstoneEruptionManager.canErupt(true, false));
    }

    @Test
    void eruptionRequiresAPresentPrimary() {
        assertFalse(BrimstoneEruptionManager.canErupt(false, false));
        assertFalse(BrimstoneEruptionManager.canErupt(true, true));
    }
}
