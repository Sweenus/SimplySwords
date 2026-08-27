package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WraithmawTuningSnapshotTest {

    @Test
    void embeddedAndOrbitingCutlassTuningSurvivesNbtRoundTrip() {
        WraithmawTuningSnapshot expected = snapshot(WraithmawTuningSnapshot.MODE_TWIN_LAUNCH
                | WraithmawTuningSnapshot.MODE_HAUNTED_STEEL
                | WraithmawTuningSnapshot.MODE_SHARED_BURIAL
                | WraithmawTuningSnapshot.MODE_GRAVE_RECALL);
        NbtCompound nbt = new NbtCompound();

        expected.write(nbt);

        assertEquals(expected, WraithmawTuningSnapshot.read(nbt));
    }

    @Test
    void everyTransformationBehaviourNeedsItsModeBitAndItsValues() {
        WraithmawTuningSnapshot armed = snapshot(WraithmawTuningSnapshot.MODE_HAUNTED_STEEL
                | WraithmawTuningSnapshot.MODE_SHARED_BURIAL
                | WraithmawTuningSnapshot.MODE_GRAVE_RECALL
                | WraithmawTuningSnapshot.MODE_LIVING_GRAVEYARD
                | WraithmawTuningSnapshot.MODE_MAUSOLEUM_BURST);
        WraithmawTuningSnapshot unarmed = snapshot(0);

        assertTrue(armed.hauntsEnemies());
        assertTrue(armed.extendsGloamOnKill());
        assertTrue(armed.pullsOnRecovery());
        assertTrue(armed.walksToEnemies());
        assertTrue(armed.detonatesEmbedded());
        assertFalse(unarmed.hauntsEnemies());
        assertFalse(unarmed.extendsGloamOnKill());
        assertFalse(unarmed.pullsOnRecovery());
        assertFalse(unarmed.walksToEnemies());
        assertFalse(unarmed.detonatesEmbedded());
    }

    @Test
    void onlyMausoleumBurstSuppressesGloamSlowness() {
        assertTrue(snapshot(0).appliesSlowness());
        assertTrue(snapshot(WraithmawTuningSnapshot.MODE_LIVING_GRAVEYARD).appliesSlowness());
        assertFalse(snapshot(WraithmawTuningSnapshot.MODE_MAUSOLEUM_BURST).appliesSlowness());
    }

    @Test
    void aCutlassSavedBeforeTheSnapshotFallsBackInsteadOfLosingItsBounds() {
        WraithmawTuningSnapshot fallback = snapshot(0);

        WraithmawTuningSnapshot restored = WraithmawTuningSnapshot.read(new NbtCompound(), fallback);

        assertEquals(fallback, restored);
        assertTrue(restored.fallIntervalTicks() >= 1);
        assertTrue(restored.orbitCap() >= 1);
        assertFalse(restored.detonatesEmbedded());
    }

    private static WraithmawTuningSnapshot snapshot(int mode) {
        return new WraithmawTuningSnapshot(
                10, 1, 1.45,
                2, .3, 4,
                600, 1.6, 320,
                1, 60, mode,
                7, 2, 1100, 80,
                1.55, 28, 100,
                5, 9, .65,
                2, .04, .24,
                .15,
                3, 40, .25, 8,
                5, 40, 80,
                4, 5, 1,
                6, .18, .6,
                12, 8, .9, 200);
    }
}
