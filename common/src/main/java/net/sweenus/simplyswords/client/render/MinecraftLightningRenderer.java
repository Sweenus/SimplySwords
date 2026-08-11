package net.sweenus.simplyswords.client.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class MinecraftLightningRenderer {

    private static final float GRID = 1.0F / 16.0F;

    private MinecraftLightningRenderer() {
    }

    public static float[] generate(Vec3d start, Vec3d end, long seed, float age,
                                   float spread, int requestedSegments, float epochTicks) {
        int segments = MathHelper.clamp(requestedSegments, 4, 14);
        float[] path = new float[(segments + 1) * 3];
        Vec3d direction = end.subtract(start);
        double length = direction.length();
        if (length < 1.0E-4) return path;

        Vec3d forward = direction.multiply(1.0 / length);
        Vec3d reference = Math.abs(forward.y) < 0.88 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
        Vec3d side = forward.crossProduct(reference).normalize();
        Vec3d up = side.crossProduct(forward).normalize();
        long epoch = MathHelper.floor(age / Math.max(1.0F, epochTicks));
        double amplitude = MathHelper.clamp(length * spread, 0.12, Math.max(0.14, length * 0.14));

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3d point = start.add(direction.multiply(t));
            if (i > 0 && i < segments) {
                float envelope = MathHelper.sin((float) (t * MathHelper.PI));
                double lateral = signed(seed, epoch, i * 2L) * amplitude * envelope;
                double vertical = signed(seed, epoch, i * 2L + 1L) * amplitude * envelope;
                point = point.add(side.multiply(lateral)).add(up.multiply(vertical));
            }
            int offset = i * 3;
            path[offset] = snap((float) point.x);
            path[offset + 1] = snap((float) point.y);
            path[offset + 2] = snap((float) point.z);
        }
        return path;
    }

    public static void draw(VertexConsumer vertices, Matrix4f matrix, float[] path,
                            float startWidth, float endWidth, int coreColor,
                            int edgeColor, float intensity, boolean fadeTail) {
        int points = path.length / 3;
        if (points < 2 || intensity <= 0.0F) return;
        float alpha = MathHelper.clamp(intensity, 0.0F, 1.0F);
        for (int i = 0; i < points - 1; i++) {
            float progressA = i / (float) (points - 1);
            float progressB = (i + 1) / (float) (points - 1);
            float tailA = fadeTail ? MathHelper.clamp((1.0F - progressA) * 5.0F, 0.0F, 1.0F) : 1.0F;
            float tailB = fadeTail ? MathHelper.clamp((1.0F - progressB) * 5.0F, 0.0F, 1.0F) : 1.0F;
            float widthA = MathHelper.lerp(progressA, startWidth, endWidth);
            float widthB = MathHelper.lerp(progressB, startWidth, endWidth);
            drawSegment(vertices, matrix, path, i, widthA * 1.65F, widthB * 1.65F,
                    edgeColor, alpha * 0.28F * tailA, alpha * 0.28F * tailB);
            drawSegment(vertices, matrix, path, i, widthA, widthB,
                    coreColor, alpha * 0.92F * tailA, alpha * 0.92F * tailB);
        }
    }

    public static void drawBranches(VertexConsumer vertices, Matrix4f matrix, float[] trunk,
                                    long seed, float age, int requested, float reach,
                                    float width, int coreColor, int edgeColor, float intensity) {
        int points = trunk.length / 3;
        if (points < 4) return;
        int count = MathHelper.clamp(requested, 0, 6);
        for (int branch = 0; branch < count; branch++) {
            long key = seed * 31L + branch * 7919L;
            int index = 1 + MathHelper.floor(BoltPath.unit(key, 7L, 0L) * (points - 3));
            int offset = index * 3;
            int previous = (index - 1) * 3;
            int next = (index + 1) * 3;
            Vec3d tangent = new Vec3d(trunk[next] - trunk[previous], trunk[next + 1] - trunk[previous + 1],
                    trunk[next + 2] - trunk[previous + 2]).normalize();
            Vec3d reference = Math.abs(tangent.y) < 0.88 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
            Vec3d side = tangent.crossProduct(reference).normalize();
            Vec3d up = side.crossProduct(tangent).normalize();
            double angle = BoltPath.unit(key, 11L, 0L) * MathHelper.TAU;
            Vec3d branchDirection = tangent.multiply(0.22)
                    .add(side.multiply(Math.cos(angle) * 0.92))
                    .add(up.multiply(Math.sin(angle) * 0.92)).normalize();
            Vec3d start = new Vec3d(trunk[offset], trunk[offset + 1], trunk[offset + 2]);
            Vec3d end = start.add(branchDirection.multiply(reach * (0.55 + BoltPath.unit(key, 13L, 0L) * 0.45)));
            float[] path = generate(start, end, key, age, 0.07F, 4, 2.0F);
            draw(vertices, matrix, path, width, width * 0.35F, coreColor, edgeColor,
                    intensity * 0.72F, true);
        }
    }

    private static void drawSegment(VertexConsumer vertices, Matrix4f matrix, float[] path, int segment,
                                    float widthA, float widthB, int color, float alphaA, float alphaB) {
        int a = segment * 3;
        int b = (segment + 1) * 3;
        Vec3d direction = new Vec3d(path[b] - path[a], path[b + 1] - path[a + 1], path[b + 2] - path[a + 2]);
        if (direction.lengthSquared() < 1.0E-6) return;
        direction = direction.normalize();
        Vec3d reference = Math.abs(direction.y) < 0.88 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
        Vec3d first = direction.crossProduct(reference).normalize();
        Vec3d second = direction.crossProduct(first).normalize();
        emitQuad(vertices, matrix, path, a, b, first, widthA, widthB, color, alphaA, alphaB);
        emitQuad(vertices, matrix, path, a, b, second, widthA, widthB, color, alphaA, alphaB);
    }

    private static void emitQuad(VertexConsumer vertices, Matrix4f matrix, float[] path, int a, int b,
                                 Vec3d side, float widthA, float widthB, int color,
                                 float alphaA, float alphaB) {
        vertex(vertices, matrix, path[a] - side.x * widthA, path[a + 1] - side.y * widthA,
                path[a + 2] - side.z * widthA, color, alphaA);
        vertex(vertices, matrix, path[a] + side.x * widthA, path[a + 1] + side.y * widthA,
                path[a + 2] + side.z * widthA, color, alphaA);
        vertex(vertices, matrix, path[b] + side.x * widthB, path[b + 1] + side.y * widthB,
                path[b + 2] + side.z * widthB, color, alphaB);
        vertex(vertices, matrix, path[b] - side.x * widthB, path[b + 1] - side.y * widthB,
                path[b + 2] - side.z * widthB, color, alphaB);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, double x, double y, double z,
                               int color, float alpha) {
        vertices.vertex(matrix, (float) x, (float) y, (float) z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255,
                        MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255))
                .next();
    }

    private static float snap(float value) {
        return Math.round(value / GRID) * GRID;
    }

    private static float signed(long seed, long epoch, long salt) {
        return BoltPath.unit(seed, epoch * 37L + salt, salt * 17L) * 2.0F - 1.0F;
    }
}
