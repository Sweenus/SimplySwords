package net.sweenus.simplyswords.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GloampiercerTuningSnapshotTest {

    @Test
    void embeddedSpearTuningSurvivesNbtRoundTrip() {
        GloampiercerTuningSnapshot expected = snapshot(100, Double.MAX_VALUE, 64 | 128 | 256 | 512);
        NbtCompound nbt = new NbtCompound();

        expected.write(nbt);

        assertEquals(expected, GloampiercerTuningSnapshot.read(nbt));
    }

    @Test
    void projectileLifetimeAndRangeAreBothBounded() {
        GloampiercerTuningSnapshot base = snapshot(120, 48, 0);
        GloampiercerTuningSnapshot pursuit = snapshot(100, Double.MAX_VALUE, 0);

        assertFalse(base.flightExpired(120, 48));
        assertTrue(base.flightExpired(121, 48));
        assertTrue(base.flightExpired(20, 49));
        assertFalse(pursuit.flightExpired(100, 155));
        assertTrue(pursuit.flightExpired(101, 155));
    }

    @Test
    void homingTurnNeverExceedsItsTunedAngle() {
        Vec3d current = new Vec3d(1, 0, 0);
        Vec3d desired = new Vec3d(0, 0, 1);

        Vec3d result = GloampiercerTuningSnapshot.turnToward(current, desired, Math.toRadians(8));

        assertEquals(Math.toRadians(8), Math.acos(current.dotProduct(result)), 1.0E-3);
    }

    private static GloampiercerTuningSnapshot snapshot(int lifetime, double range, int mode) {
        return new GloampiercerTuningSnapshot(1.55, 8, lifetime, range,
                140, 1.5, 3, 8, 1.8, 300, 1, 60, mode,
                .15, 6, 1, .75, 5, 4, 6, .15, .8, 2.5, 6);
    }
}
