package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryTuning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HiveheartSwarmRepairTest {
    @Test
    void configuredSwarmBonusesRespectCaps() {
        NatureSwarmMasteryTuning tuning = NatureSwarmMasteryTuning.EMPTY
                .with(s("HIVE_SWARM_COUNT_BONUS"), 2)
                .with(s("HIVE_SWARM_COUNT_CAP"), 12)
                .with(s("HIVE_SWARM_RADIUS_BONUS"), 2)
                .with(s("HIVE_SWARM_DURATION_BONUS_TICKS"), 60)
                .with(s("HIVE_SWARM_STING_COUNT"), 12);
        assertEquals(12, HivemindSwarmManager.swarmCount(20, tuning));
        assertEquals(10, HivemindSwarmManager.swarmRadius(8, tuning), 1.0E-6);
        assertEquals(300, HivemindSwarmManager.swarmDuration(240, tuning));
        assertEquals(12, HivemindSwarmManager.swarmStings(10, tuning));
    }

    @Test
    void cloudAndHuntingFlightRetainTheirOwnOverrides() {
        NatureSwarmMasteryTuning cloud = NatureSwarmMasteryTuning.EMPTY
                .with(s("MODE"), 1 << 16)
                .with(s("HIVE_SWARM_DURATION_BONUS_TICKS"), 60)
                .with(s("HIVE_CLOUD_COUNT"), 16)
                .with(s("HIVE_CLOUD_RADIUS"), 12)
                .with(s("HIVE_CLOUD_DAMAGE_MULTIPLIER"), .55)
                .with(s("HIVE_CLOUD_DURATION_MULTIPLIER"), .6);
        assertEquals(16, HivemindSwarmManager.swarmCount(8, cloud));
        assertEquals(12, HivemindSwarmManager.swarmRadius(8, cloud), 1.0E-6);
        assertEquals(180, HivemindSwarmManager.swarmDuration(240, cloud));
        assertEquals(.55, HivemindSwarmManager.swarmDamageMultiplier(cloud), 1.0E-6);

        NatureSwarmMasteryTuning hunt = NatureSwarmMasteryTuning.EMPTY
                .with(s("MODE"), (1 << 17) | (1 << 26))
                .with(s("HIVE_HUNT_COUNT"), 4)
                .with(s("HIVE_HUNT_STING_COUNT"), 24)
                .with(s("HIVE_HUNT_DURATION_TICKS"), 240)
                .with(s("HIVE_HUNT_DAMAGE_MULTIPLIER"), 1.5)
                .with(s("HIVE_HUNT_RADIUS"), 12)
                .with(s("HIVE_VENGEFUL_DAMAGE_MULTIPLIER"), 1.5)
                .with(s("HIVE_VENGEFUL_RANGE"), 12);
        assertEquals(4, HivemindSwarmManager.swarmCount(8, hunt));
        assertEquals(24, HivemindSwarmManager.swarmStings(10, hunt));
        assertEquals(240, HivemindSwarmManager.swarmDuration(300, hunt));
        assertEquals(12, HivemindSwarmManager.swarmRadius(8, hunt), 1.0E-6);
        assertEquals(2.25, HivemindSwarmManager.swarmDamageMultiplier(hunt), 1.0E-6);
    }

    private static NatureSwarmMasteryTuning.Setting s(String name) {
        return NatureSwarmMasteryTuning.Setting.valueOf(name);
    }
}
