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
