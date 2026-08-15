package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class SoulstalkerStrideContactSolver {
    private SoulstalkerStrideContactSolver() {
    }

    public static Contact find(World world, Entity source, Vec3d center, float yawDegrees,
                               double riderHeight, byte surfaceMode, Vec3d surfaceNormal, int leg) {
        float yaw = yawDegrees * MathHelper.RADIANS_PER_DEGREE;
        Vec3d forward = new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
        Vec3d right = new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
        if (surfaceMode == SoulstalkerStrideEntity.SURFACE_WALL) {
            return findWall(world, source, center, forward, right, riderHeight, surfaceNormal, leg);
        }
        if (surfaceMode == SoulstalkerStrideEntity.SURFACE_CEILING) {
            return findCeiling(world, source, center, yaw, riderHeight, leg);
        }
        return findGround(world, source, center, yaw, leg);
    }

    private static Contact findGround(World world, Entity source, Vec3d center, float yaw, int leg) {
        double angle = yaw + leg * MathHelper.TAU / 6.0;
        double lean = Math.sin(leg * MathHelper.TAU / 6.0);
        double radius = (1.32 + (leg % 3) * 0.08) * (1.0 + lean * 0.14);
        Vec3d desired = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        BlockHitResult hit = world.raycast(new RaycastContext(
                desired.add(0.0, 3.0, 0.0), desired.add(0.0, -5.0, 0.0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        if (hit.getType() != HitResult.Type.MISS) {
            return new Contact(hit.getPos().add(0.0, 0.035, 0.0), Vec3d.of(hit.getSide().getVector()),
                    hit.getBlockPos(), true);
        }
        return fallback(desired, new Vec3d(0.0, 1.0, 0.0));
    }

    private static Contact findWall(World world, Entity source, Vec3d center,
                                    Vec3d forward, Vec3d right, double riderHeight,
                                    Vec3d surfaceNormal, int leg) {
        Vec3d normal = horizontalNormal(surfaceNormal, forward.multiply(-1.0));
        Vec3d towardWall = normal.multiply(-1.0);
        Vec3d wallRight = new Vec3d(-normal.z, 0.0, normal.x);
        if (wallRight.dotProduct(right) < 0.0) {
            wallRight = wallRight.multiply(-1.0);
        }
        int row = leg / 2;
        double side = (leg & 1) == 0 ? -1.0 : 1.0;
        double[] heights = {-0.8, 0.35, 1.5};
        double lateral = side * (0.72 + row * 0.12);
        Vec3d start = center.add(wallRight.multiply(lateral))
                .add(0.0, riderHeight + heights[MathHelper.clamp(row, 0, heights.length - 1)], 0.0);
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, start.add(towardWall.multiply(2.25)),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3d hitNormal = Vec3d.of(hit.getSide().getVector());
            return new Contact(hit.getPos().add(hitNormal.multiply(0.035)), hitNormal,
                    hit.getBlockPos(), true);
        }
        return fallback(start.add(towardWall.multiply(1.15)), normal);
    }

    private static Contact findCeiling(World world, Entity source, Vec3d center,
                                       float yaw, double riderHeight, int leg) {
        double angle = yaw + leg * MathHelper.TAU / 6.0;
        double radius = 0.72 + (leg % 2) * 0.2;
        Vec3d desired = center.add(Math.cos(angle) * radius,
                riderHeight + 1.25, Math.sin(angle) * radius);
        BlockHitResult hit = world.raycast(new RaycastContext(
                desired, desired.add(0.0, 2.5, 0.0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3d normal = Vec3d.of(hit.getSide().getVector());
            return new Contact(hit.getPos().add(normal.multiply(0.035)), normal,
                    hit.getBlockPos(), true);
        }
        return fallback(desired.add(0.0, 1.1, 0.0), new Vec3d(0.0, -1.0, 0.0));
    }

    private static Vec3d horizontalNormal(Vec3d value, Vec3d fallback) {
        Vec3d horizontal = new Vec3d(value.x, 0.0, value.z);
        if (horizontal.lengthSquared() < 1.0E-6) {
            horizontal = new Vec3d(fallback.x, 0.0, fallback.z);
        }
        return horizontal.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 0.0, -1.0) : horizontal.normalize();
    }

    private static Contact fallback(Vec3d position, Vec3d normal) {
        Vec3d resolvedNormal = normal.lengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 1.0, 0.0) : normal.normalize();
        BlockPos blockPos = BlockPos.ofFloored(position.subtract(resolvedNormal.multiply(0.08)));
        return new Contact(position, resolvedNormal, blockPos, false);
    }

    public record Contact(Vec3d position, Vec3d normal, BlockPos blockPos, boolean valid) {
    }
}
