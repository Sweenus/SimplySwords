package net.sweenus.simplyswords.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StormsEdgeAbilityManagerTest {

    @Test
    void stormCellPulsesAtItsIntervalThroughTheFinalActiveTick() {
        assertFalse(StormsEdgeAbilityManager.shouldPulseStormCell(0, 80, 20));
        assertFalse(StormsEdgeAbilityManager.shouldPulseStormCell(19, 80, 20));
        assertTrue(StormsEdgeAbilityManager.shouldPulseStormCell(20, 80, 20));
        assertTrue(StormsEdgeAbilityManager.shouldPulseStormCell(80, 80, 20));
        assertFalse(StormsEdgeAbilityManager.shouldPulseStormCell(81, 80, 20));
    }

    @Test
    void stormCellDoesNotPulseWithDisabledOrInvalidTiming() {
        assertFalse(StormsEdgeAbilityManager.shouldPulseStormCell(20, 0, 20));
        assertFalse(StormsEdgeAbilityManager.shouldPulseStormCell(20, 80, 0));
        assertFalse(StormsEdgeAbilityManager.shouldPulseStormCell(-20, 80, 20));
    }
}
