package net.sweenus.simplyswords.world;

import net.minecraft.util.math.Vec3d;

public final class WickpiercerThrowMath {

    public static final double DEFAULT_ORBIT_RADIUS = 1.6;
    public static final double ORBIT_ANGULAR_SPEED = 0.35;

    private WickpiercerThrowMath() {
    }

    public static int scaledFireSeconds(int fireTicks) {
        return fireTicks <= 0 ? 0 : Math.max(1, (fireTicks + 19) / 20);
    }

    public static int nextStrikeAge(int currentAge, int interval) {
        return currentAge + Math.max(1, interval);
    }

    public static boolean orbitContinues(int currentAge, int endAge, boolean targetAlive) {
        return targetAlive && currentAge <= endAge;
    }

    public static Vec3d orbitOffset(double elapsedTicks, int intervalTicks,
                                    double initialPhase, double orbitRadius) {
        double elapsed = Math.max(0.0, elapsedTicks);
        int interval = Math.max(1, intervalTicks);
        double cycleProgress = (elapsed % interval) / interval;
        if (cycleProgress < 1.0E-9) {
            return Vec3d.ZERO;
        }
        double envelope = Math.pow(Math.max(0.0, Math.sin(Math.PI * cycleProgress)), 0.65);
        double angle = initialPhase + elapsed * ORBIT_ANGULAR_SPEED;
        double radius = Math.max(0.0, orbitRadius) * envelope;
        double height = envelope * (0.35 + Math.sin(angle * 2.0) * 0.15);
        return new Vec3d(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }
}
