package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FlamewindMasteryManagerTest {

    @Test
    void furnaceDraftScalesWithSeedsAndStopsAtItsCap() {
        assertEquals(0, FlamewindMasteryManager.draftBonus(0, .03, .15), 1.0E-6);
        assertEquals(.09, FlamewindMasteryManager.draftBonus(3, .03, .15), 1.0E-6);
        assertEquals(.15, FlamewindMasteryManager.draftBonus(5, .03, .15), 1.0E-6);
        assertEquals(.15, FlamewindMasteryManager.draftBonus(40, .03, .15), 1.0E-6);
        assertEquals(0, FlamewindMasteryManager.draftBonus(5, 0, .15), 1.0E-6);
    }

    @Test
    void sparkHarvestRefundsUpToItsPerCastCeiling() {
        assertEquals(6, FlamewindMasteryManager.nextRefund(0, 6, 30));
        assertEquals(6, FlamewindMasteryManager.nextRefund(24, 6, 30));
        assertEquals(0, FlamewindMasteryManager.nextRefund(30, 6, 30));
        assertEquals(2, FlamewindMasteryManager.nextRefund(28, 6, 30));
        assertEquals(6, FlamewindMasteryManager.nextRefund(999, 6, 0));
    }

    @Test
    void freshFuelShortensOnlySpreadSeeds() {
        assertEquals(101, FlamewindMasteryManager.spreadDuration(101, 0));
        assertEquals(81, FlamewindMasteryManager.spreadDuration(101, .8));
        assertEquals(101, FlamewindMasteryManager.spreadDuration(101, 1.5));
    }

    @Test
    void generationScalingCompoundsAndRespectsItsCap() {
        assertEquals(1, FlamewindMasteryManager.generationMultiplier(1.25, 0, 3), 1.0E-6);
        assertEquals(1.25, FlamewindMasteryManager.generationMultiplier(1.25, 1, 3), 1.0E-6);
        assertEquals(1.953125, FlamewindMasteryManager.generationMultiplier(1.25, 3, 3), 1.0E-6);
        assertEquals(1.953125, FlamewindMasteryManager.generationMultiplier(1.25, 9, 3), 1.0E-6);
        assertEquals(.64, FlamewindMasteryManager.generationMultiplier(.8, 2, 0), 1.0E-6);
        assertEquals(1, FlamewindMasteryManager.generationMultiplier(0, 4, 0), 1.0E-6);
    }

    @Test
    void stormfrontPushesOutwardAndConvergingFlamePullsInward() {
        assertEquals(.28, FlamewindMasteryManager.detonationKnockback(0, 1), 1.0E-6);
        assertEquals(.336, FlamewindMasteryManager.detonationKnockback(0, 1.2), 1.0E-6);
        assertEquals(-.28, FlamewindMasteryManager.detonationKnockback(.25, 1), 1.0E-6);
        assertEquals(0, FlamewindMasteryManager.detonationKnockback(0, 0), 1.0E-6);
    }

    @Test
    void staleSeedSnapshotsArePrunedBeforeOwnershipChecks() {
        UUID ownerId = UUID.randomUUID();
        UUID liveTarget = UUID.randomUUID();
        UUID staleTarget = UUID.randomUUID();
        FlamewindMasteryManager.SeedSnapshot snapshot = new FlamewindMasteryManager.SeedSnapshot(
                ownerId, null, FireForgeMasteryTuning.EMPTY, null, 0);
        Map<UUID, FlamewindMasteryManager.SeedSnapshot> seeds = new HashMap<>();
        seeds.put(liveTarget, snapshot);
        seeds.put(staleTarget, snapshot);

        FlamewindMasteryManager.pruneSeeds(seeds, liveTarget::equals);

        assertEquals(1, seeds.size());
        assertTrue(seeds.containsKey(liveTarget));
    }
}
