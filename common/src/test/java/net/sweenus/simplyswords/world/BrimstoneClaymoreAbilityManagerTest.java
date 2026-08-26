package net.sweenus.simplyswords.world;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrimstoneClaymoreAbilityManagerTest {

    @Test
    void wakeSpacingIgnoresPerTickMovementAndTracksTravelledDistance() {
        Vec3d anchor = new Vec3d(0.0, 64.0, 0.0);

        assertFalse(BrimstoneClaymoreAbilityManager.shouldCreateWake(anchor, new Vec3d(0.28, 64.0, 0.0), 1.5));
        assertFalse(BrimstoneClaymoreAbilityManager.shouldCreateWake(anchor, new Vec3d(1.4, 64.0, 0.0), 1.5));
        assertTrue(BrimstoneClaymoreAbilityManager.shouldCreateWake(anchor, new Vec3d(1.5, 64.0, 0.0), 1.5));
        assertTrue(BrimstoneClaymoreAbilityManager.shouldCreateWake(anchor, new Vec3d(8.0, 64.0, 0.0), 1.5));
        assertTrue(BrimstoneClaymoreAbilityManager.shouldCreateWake(null, new Vec3d(0.0, 64.0, 0.0), 1.5));
    }

    @Test
    void sprintingTargetLeavesWakesOverCumulativeTravel() {
        Vec3d anchor = new Vec3d(0.0, 64.0, 0.0);
        int wakes = 0;
        for (int tick = 1; tick <= 20; tick++) {
            Vec3d current = new Vec3d(tick * 0.28, 64.0, 0.0);
            if (BrimstoneClaymoreAbilityManager.shouldCreateWake(anchor, current, 1.5)) {
                wakes++;
                anchor = current;
            }
        }

        assertEquals(3, wakes);
    }

    @Test
    void perpetualPulseMultiplierReachesItsCapWithinACast() {
        assertEquals(1.5F, peak(0.75F, 0.10F, 1.5F, 10), 1.0E-4F);
        assertEquals(1.2F, peak(0.75F, 0.05F, 1.5F, 10), 1.0E-4F);
    }

    private static float peak(float start, float perPulse, float cap, int pulses) {
        float multiplier = start;
        for (int pulse = 1; pulse < pulses; pulse++) {
            multiplier = BrimstoneClaymoreAbilityManager.grownPulseMultiplier(multiplier, perPulse, cap);
        }
        return multiplier;
    }
}
