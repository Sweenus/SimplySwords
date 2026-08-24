package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.ChainLightningVisualEntity;
import org.joml.Matrix4f;

import java.util.Random;

public class ChainLightningVisualEntityRenderer extends EntityRenderer<ChainLightningVisualEntity> {

    private static final int SEGMENTS = 9;

    public ChainLightningVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ChainLightningVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(ChainLightningVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(Math.max(4.0, entity.getBoltLength())));
    }

    @Override
    public void render(ChainLightningVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Vec3d end = new Vec3d(entity.getEndOffsetX(), entity.getEndOffsetY(), entity.getEndOffsetZ());
        if (end.lengthSquared() < 0.01) {
            return;
        }

        float progress = MathHelper.clamp((entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float fade = 1.0F - progress;
        if (fade <= 0.0F) {
            return;
        }

        int color = entity.getColor();
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        Vec3d[] points = buildBoltPoints(end, entity.getSeed());

        drawBoltRibbon(vertices, matrix, points, entity.getThickness() * 1.7F, red, green, blue, MathHelper.clamp((int) (120.0F * fade), 0, 120));
        drawBoltRibbon(vertices, matrix, points, entity.getThickness() * 0.55F, 235, 250, 255, MathHelper.clamp((int) (235.0F * fade), 0, 235));

        int branches = MathHelper.clamp(entity.getBranches(), 0, 8);
        Random random = new Random(entity.getSeed() * 31L + 17L);
        for (int i = 0; i < branches; i++) {
            int index = 1 + random.nextInt(points.length - 2);
            Vec3d origin = points[index];
            Vec3d direction = randomPerpendicular(end, random).multiply(0.18 + random.nextDouble() * 0.32);
            Vec3d branchEnd = origin.add(direction).add(end.normalize().multiply(0.08 + random.nextDouble() * 0.18));
            drawSegment(vertices, matrix, origin, branchEnd, entity.getThickness() * 0.45F, red, green, blue, MathHelper.clamp((int) (110.0F * fade), 0, 110));
        }
    }

    private static Vec3d[] buildBoltPoints(Vec3d end, int seed) {
        Vec3d[] points = new Vec3d[SEGMENTS + 1];
        Vec3d direction = end.normalize();
        Vec3d side = perpendicular(direction);
        Vec3d up = direction.crossProduct(side).normalize();
        Random random = new Random(seed);
        points[0] = Vec3d.ZERO;
        points[SEGMENTS] = end;

        for (int i = 1; i < SEGMENTS; i++) {
            double progress = (double) i / SEGMENTS;
            double intensity = Math.sin(progress * Math.PI);
            double jagX = (random.nextDouble() - 0.5) * 0.32 * intensity;
            double jagY = (random.nextDouble() - 0.5) * 0.32 * intensity;
            points[i] = end.multiply(progress).add(side.multiply(jagX)).add(up.multiply(jagY));
        }
        return points;
    }

    private static Vec3d perpendicular(Vec3d direction) {
        Vec3d cross = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (cross.lengthSquared() < 0.001) {
            cross = direction.crossProduct(new Vec3d(1.0, 0.0, 0.0));
        }
        return cross.normalize();
    }

    private static Vec3d randomPerpendicular(Vec3d end, Random random) {
        Vec3d direction = end.normalize();
        Vec3d side = perpendicular(direction);
        Vec3d up = direction.crossProduct(side).normalize();
        double angle = random.nextDouble() * MathHelper.TAU;
        return side.multiply(Math.cos(angle)).add(up.multiply(Math.sin(angle))).normalize();
    }

    private static void drawSegment(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end, float halfThickness, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        Vec3d line = end.subtract(start);
        if (line.lengthSquared() < 0.001) {
            return;
        }
        Vec3d side = verticalPerpendicular(line).multiply(halfThickness);
        vertex(vertices, matrix, start.add(side), red, green, blue, alpha);
        vertex(vertices, matrix, start.subtract(side), red, green, blue, alpha);
        vertex(vertices, matrix, end.subtract(side), red, green, blue, alpha);
        vertex(vertices, matrix, end.add(side), red, green, blue, alpha);
    }

    private static void drawBoltRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3d[] points, float halfThickness, int red, int green, int blue, int alpha) {
        if (alpha <= 0 || points.length < 2) {
            return;
        }

        Vec3d[] sides = new Vec3d[points.length];
        for (int i = 0; i < points.length; i++) {
            Vec3d before = i > 0 ? points[i].subtract(points[i - 1]) : null;
            Vec3d after = i < points.length - 1 ? points[i + 1].subtract(points[i]) : null;
            Vec3d side;
            if (before != null && after != null && before.lengthSquared() > 0.001 && after.lengthSquared() > 0.001) {
                Vec3d beforeSide = verticalPerpendicular(before);
                Vec3d afterSide = verticalPerpendicular(after);
                side = beforeSide.add(afterSide);
                if (side.lengthSquared() < 0.001) {
                    side = afterSide;
                } else {
                    side = side.normalize();
                }
            } else {
                Vec3d line = after != null ? after : before;
                side = line != null && line.lengthSquared() > 0.001 ? verticalPerpendicular(line) : new Vec3d(0.0, 1.0, 0.0);
            }
            sides[i] = side.multiply(halfThickness);
        }

        for (int i = 0; i < points.length - 1; i++) {
            vertex(vertices, matrix, points[i].add(sides[i]), red, green, blue, alpha);
            vertex(vertices, matrix, points[i].subtract(sides[i]), red, green, blue, alpha);
            vertex(vertices, matrix, points[i + 1].subtract(sides[i + 1]), red, green, blue, alpha);
            vertex(vertices, matrix, points[i + 1].add(sides[i + 1]), red, green, blue, alpha);
        }
    }

    private static Vec3d verticalPerpendicular(Vec3d line) {
        Vec3d direction = line.normalize();
        Vec3d vertical = new Vec3d(0.0, 1.0, 0.0);
        Vec3d side = vertical.subtract(direction.multiply(vertical.dotProduct(direction)));
        if (side.lengthSquared() < 0.001) {
            return perpendicular(direction);
        }
        return side.normalize();
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d pos, int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }
}
