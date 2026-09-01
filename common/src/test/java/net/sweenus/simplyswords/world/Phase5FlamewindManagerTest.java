package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.Phase5AbilityTuning;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Phase5FlamewindManagerTest {

    @Test
    void furnaceDraftScalesWithSeedsAndStopsAtItsCap() {
        assertEquals(0, Phase5FlamewindManager.draftBonus(0, .03, .15), 1.0E-6);
        assertEquals(.09, Phase5FlamewindManager.draftBonus(3, .03, .15), 1.0E-6);
        assertEquals(.15, Phase5FlamewindManager.draftBonus(5, .03, .15), 1.0E-6);
        assertEquals(.15, Phase5FlamewindManager.draftBonus(40, .03, .15), 1.0E-6);
        assertEquals(0, Phase5FlamewindManager.draftBonus(5, 0, .15), 1.0E-6);
    }

    @Test
    void sparkHarvestRefundsUpToItsPerCastCeiling() {
        assertEquals(6, Phase5FlamewindManager.nextRefund(0, 6, 30));
        assertEquals(6, Phase5FlamewindManager.nextRefund(24, 6, 30));
        assertEquals(0, Phase5FlamewindManager.nextRefund(30, 6, 30));
        assertEquals(2, Phase5FlamewindManager.nextRefund(28, 6, 30));
        assertEquals(6, Phase5FlamewindManager.nextRefund(999, 6, 0));
    }

    @Test
    void freshFuelShortensOnlySpreadSeeds() {
        assertEquals(101, Phase5FlamewindManager.spreadDuration(101, 0));
        assertEquals(81, Phase5FlamewindManager.spreadDuration(101, .8));
        assertEquals(101, Phase5FlamewindManager.spreadDuration(101, 1.5));
    }

    @Test
    void generationScalingCompoundsAndRespectsItsCap() {
        assertEquals(1, Phase5FlamewindManager.generationMultiplier(1.25, 0, 3), 1.0E-6);
        assertEquals(1.25, Phase5FlamewindManager.generationMultiplier(1.25, 1, 3), 1.0E-6);
        assertEquals(1.953125, Phase5FlamewindManager.generationMultiplier(1.25, 3, 3), 1.0E-6);
        assertEquals(1.953125, Phase5FlamewindManager.generationMultiplier(1.25, 9, 3), 1.0E-6);
        assertEquals(.64, Phase5FlamewindManager.generationMultiplier(.8, 2, 0), 1.0E-6);
        assertEquals(1, Phase5FlamewindManager.generationMultiplier(0, 4, 0), 1.0E-6);
    }

    @Test
    void stormfrontPushesOutwardAndConvergingFlamePullsInward() {
        assertEquals(.28, Phase5FlamewindManager.detonationKnockback(0, 1), 1.0E-6);
        assertEquals(.336, Phase5FlamewindManager.detonationKnockback(0, 1.2), 1.0E-6);
        assertEquals(-.28, Phase5FlamewindManager.detonationKnockback(.25, 1), 1.0E-6);
        assertEquals(0, Phase5FlamewindManager.detonationKnockback(0, 0), 1.0E-6);
    }

    @Test
    void staleSeedSnapshotsArePrunedBeforeOwnershipChecks() {
        UUID ownerId = UUID.randomUUID();
        UUID liveTarget = UUID.randomUUID();
        UUID staleTarget = UUID.randomUUID();
        Phase5FlamewindManager.SeedSnapshot snapshot = new Phase5FlamewindManager.SeedSnapshot(
                ownerId, null, Phase5AbilityTuning.EMPTY, null, 0);
        Map<UUID, Phase5FlamewindManager.SeedSnapshot> seeds = new HashMap<>();
        seeds.put(liveTarget, snapshot);
        seeds.put(staleTarget, snapshot);

        Phase5FlamewindManager.pruneSeeds(seeds, liveTarget::equals);

        assertEquals(1, seeds.size());
        assertTrue(seeds.containsKey(liveTarget));
    }
}
