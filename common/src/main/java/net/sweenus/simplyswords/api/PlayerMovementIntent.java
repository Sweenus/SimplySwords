package net.sweenus.simplyswords.api;

import net.minecraft.util.math.Vec3d;

public record PlayerMovementIntent(int forward, int strafe) {

    public static final PlayerMovementIntent NONE = new PlayerMovementIntent(0, 0);

    public PlayerMovementIntent {
        forward = Integer.compare(forward, 0);
        strafe = Integer.compare(strafe, 0);
    }

    public boolean isNeutral() {
        return forward == 0 && strafe == 0;
    }

    public Vec3d toDirection(float yaw) {
        Vec3d polar = Vec3d.fromPolar(0.0F, yaw);
        Vec3d ahead = new Vec3d(polar.x, 0.0, polar.z).normalize();
        Vec3d right = new Vec3d(-ahead.z, 0.0, ahead.x);
        Vec3d direction = ahead.multiply(forward).add(right.multiply(strafe));
        return direction.lengthSquared() < 1.0E-4 ? ahead : direction.normalize();
    }
}
