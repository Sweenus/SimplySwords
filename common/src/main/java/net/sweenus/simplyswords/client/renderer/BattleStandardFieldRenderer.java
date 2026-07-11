package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import org.joml.Matrix4f;

public final class BattleStandardFieldRenderer {

    private static final double FIELD_RADIUS = 6.0;
    private static final float BORDER_HALF_THICKNESS = 0.12F;
    private static final float Y_OFFSET = 0.055F;
    private static final float WAVE_HALF_THICKNESS = 0.055F;
    private static final float WAVE_START_RADIUS = 0.75F;
    private static final int WAVE_INTERVAL_TICKS = 40;
    private static final float WAVE_DURATION_TICKS = 28.0F;
    private static final int CIRCLE_SEGMENTS = 96;

    private BattleStandardFieldRenderer() {
    }

    public static boolean isEnabled() {
        return Config.general.enableModernFieldEffects;
    }

    public static void renderSunfire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age) {
        if (!isEnabled()) {
            return;
        }
        renderRing(matrices, vertexConsumers, age, 255, 179, 64);
    }

    public static void renderHarbinger(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age) {
        if (!isEnabled()) {
            return;
        }
        renderRing(matrices, vertexConsumers, age, 157, 98, 202);
    }

    public static void renderImmolation(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age, float radius) {
        if (!isEnabled()) {
            return;
        }
        renderCircle(matrices, vertexConsumers, age, Math.max(0.75F, radius), 255, 179, 64);
    }

    public static void renderSoulstealerTargetLine(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age, Vec3d targetOffset) {
        if (!isEnabled()) {
            return;
        }

        Vec3d horizontalOffset = new Vec3d(targetOffset.x, 0.0, targetOffset.z);
        if (horizontalOffset.horizontalLengthSquared() < 0.01) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        Vec3d direction = horizontalOffset.normalize();
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x);
        double length = horizontalOffset.horizontalLength();
        double startInset = Math.min(0.85, length * 0.18);
        double endInset = Math.min(0.75, length * 0.14);
        if (length <= startInset + endInset + 0.25) {
            return;
        }

        Vec3d start = direction.multiply(startInset);
        Vec3d end = direction.multiply(length - endInset);
        float pulse = 0.72F + 0.16F * MathHelper.sin(age * 0.16F);
        int coreAlpha = MathHelper.clamp((int) (155.0F * pulse), 95, 180);

        drawLineBand(vertices, matrix, start, end, side, 0.09F, 118, 238, 218, coreAlpha);
        drawLinePulse(vertices, matrix, age, start, end, side);
    }

    public static void renderSoulstealerTargetRing(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age, Vec3d targetOffset, float targetWidth) {
        if (!isEnabled()) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        Vec3d center = new Vec3d(targetOffset.x, targetOffset.y, targetOffset.z);
        float radius = Math.max(0.75F, targetWidth * 0.72F + 0.35F);
        float borderPulse = 0.76F + 0.12F * MathHelper.sin(age * 0.14F);
        int borderAlpha = MathHelper.clamp((int) (165.0F * borderPulse), 90, 185);

        drawOffsetCircleWave(vertices, matrix, center, age, radius, 157, 98, 202);
        drawOffsetCircleBand(vertices, matrix, center, radius, 0.08F, 118, 238, 218, borderAlpha);
    }

    private static void renderRing(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age, int red, int green, int blue) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        float borderPulse = 0.76F + 0.12F * MathHelper.sin(age * 0.12F);
        int borderAlpha = MathHelper.clamp((int) (170.0F * borderPulse), 95, 190);
        float radius = (float) FIELD_RADIUS;

        drawWave(vertices, matrix, age, radius, red, green, blue);
        drawQuad(vertices, matrix, -radius, radius - BORDER_HALF_THICKNESS, radius, radius + BORDER_HALF_THICKNESS, red, green, blue, borderAlpha);
        drawQuad(vertices, matrix, -radius, -radius - BORDER_HALF_THICKNESS, radius, -radius + BORDER_HALF_THICKNESS, red, green, blue, borderAlpha);
        drawQuad(vertices, matrix, -radius - BORDER_HALF_THICKNESS, -radius, -radius + BORDER_HALF_THICKNESS, radius, red, green, blue, borderAlpha);
        drawQuad(vertices, matrix, radius - BORDER_HALF_THICKNESS, -radius, radius + BORDER_HALF_THICKNESS, radius, red, green, blue, borderAlpha);
    }

    private static void drawWave(VertexConsumer vertices, Matrix4f matrix, int age, float radius, int red, int green, int blue) {
        int ticksSinceWaveStart = Math.floorMod(age, WAVE_INTERVAL_TICKS);
        if (ticksSinceWaveStart >= WAVE_DURATION_TICKS) {
            return;
        }

        float progress = ticksSinceWaveStart / WAVE_DURATION_TICKS;
        float waveRadius = MathHelper.lerp(progress, WAVE_START_RADIUS, radius);
        float fade = 1.0F - progress;

        drawWaveRing(vertices, matrix, waveRadius, WAVE_HALF_THICKNESS, red, green, blue, MathHelper.clamp((int) (120.0F * fade), 0, 120));
        for (int trailIndex = 1; trailIndex <= 3; trailIndex++) {
            float trailRadius = waveRadius - trailIndex * 0.45F;
            if (trailRadius <= WAVE_START_RADIUS) {
                continue;
            }

            float trailFade = fade * (1.0F - trailIndex * 0.22F);
            int trailAlpha = MathHelper.clamp((int) (70.0F * trailFade), 0, 70);
            drawWaveRing(vertices, matrix, trailRadius, WAVE_HALF_THICKNESS, red, green, blue, trailAlpha);
        }
    }

    private static void drawWaveRing(VertexConsumer vertices, Matrix4f matrix, float radius, float halfThickness, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        drawQuad(vertices, matrix, -radius, radius - halfThickness, radius, radius + halfThickness, red, green, blue, alpha);
        drawQuad(vertices, matrix, -radius, -radius - halfThickness, radius, -radius + halfThickness, red, green, blue, alpha);
        drawQuad(vertices, matrix, -radius - halfThickness, -radius, -radius + halfThickness, radius, red, green, blue, alpha);
        drawQuad(vertices, matrix, radius - halfThickness, -radius, radius + halfThickness, radius, red, green, blue, alpha);
    }

    private static void renderCircle(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int age, float radius, int red, int green, int blue) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        float borderPulse = 0.76F + 0.12F * MathHelper.sin(age * 0.12F);
        int borderAlpha = MathHelper.clamp((int) (170.0F * borderPulse), 95, 190);

        drawCircleWave(vertices, matrix, age, radius, red, green, blue);
        drawCircleBand(vertices, matrix, radius, BORDER_HALF_THICKNESS, red, green, blue, borderAlpha);
    }

    private static void drawCircleWave(VertexConsumer vertices, Matrix4f matrix, int age, float radius, int red, int green, int blue) {
        int ticksSinceWaveStart = Math.floorMod(age, WAVE_INTERVAL_TICKS);
        if (ticksSinceWaveStart >= WAVE_DURATION_TICKS) {
            return;
        }

        float progress = ticksSinceWaveStart / WAVE_DURATION_TICKS;
        float waveRadius = MathHelper.lerp(progress, Math.min(WAVE_START_RADIUS, radius * 0.35F), radius);
        float fade = 1.0F - progress;

        drawCircleBand(vertices, matrix, waveRadius, WAVE_HALF_THICKNESS, red, green, blue, MathHelper.clamp((int) (120.0F * fade), 0, 120));
        for (int trailIndex = 1; trailIndex <= 3; trailIndex++) {
            float trailRadius = waveRadius - trailIndex * 0.32F;
            if (trailRadius <= 0.2F) {
                continue;
            }

            float trailFade = fade * (1.0F - trailIndex * 0.22F);
            int trailAlpha = MathHelper.clamp((int) (70.0F * trailFade), 0, 70);
            drawCircleBand(vertices, matrix, trailRadius, WAVE_HALF_THICKNESS, red, green, blue, trailAlpha);
        }
    }

    private static void drawCircleBand(VertexConsumer vertices, Matrix4f matrix, float radius, float halfThickness, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        float innerRadius = Math.max(0.0F, radius - halfThickness);
        float outerRadius = radius + halfThickness;
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            double nextAngle = (Math.PI * 2.0 * (i + 1)) / CIRCLE_SEGMENTS;
            float innerX = (float) (Math.cos(angle) * innerRadius);
            float innerZ = (float) (Math.sin(angle) * innerRadius);
            float outerX = (float) (Math.cos(angle) * outerRadius);
            float outerZ = (float) (Math.sin(angle) * outerRadius);
            float nextInnerX = (float) (Math.cos(nextAngle) * innerRadius);
            float nextInnerZ = (float) (Math.sin(nextAngle) * innerRadius);
            float nextOuterX = (float) (Math.cos(nextAngle) * outerRadius);
            float nextOuterZ = (float) (Math.sin(nextAngle) * outerRadius);

            vertices.vertex(matrix, innerX, Y_OFFSET, innerZ).color(red, green, blue, alpha);
            vertices.vertex(matrix, outerX, Y_OFFSET, outerZ).color(red, green, blue, alpha);
            vertices.vertex(matrix, nextOuterX, Y_OFFSET, nextOuterZ).color(red, green, blue, alpha);
            vertices.vertex(matrix, nextInnerX, Y_OFFSET, nextInnerZ).color(red, green, blue, alpha);
        }
    }

    private static void drawOffsetCircleWave(VertexConsumer vertices, Matrix4f matrix, Vec3d center, int age, float radius, int red, int green, int blue) {
        int ticksSinceWaveStart = Math.floorMod(age, WAVE_INTERVAL_TICKS);
        if (ticksSinceWaveStart >= WAVE_DURATION_TICKS) {
            return;
        }

        float progress = ticksSinceWaveStart / WAVE_DURATION_TICKS;
        float waveRadius = MathHelper.lerp(progress, Math.min(WAVE_START_RADIUS, radius * 0.35F), radius);
        float fade = 1.0F - progress;

        drawOffsetCircleBand(vertices, matrix, center, waveRadius, WAVE_HALF_THICKNESS, red, green, blue, MathHelper.clamp((int) (110.0F * fade), 0, 110));
        for (int trailIndex = 1; trailIndex <= 2; trailIndex++) {
            float trailRadius = waveRadius - trailIndex * 0.24F;
            if (trailRadius <= 0.2F) {
                continue;
            }

            float trailFade = fade * (1.0F - trailIndex * 0.25F);
            int trailAlpha = MathHelper.clamp((int) (60.0F * trailFade), 0, 60);
            drawOffsetCircleBand(vertices, matrix, center, trailRadius, WAVE_HALF_THICKNESS, red, green, blue, trailAlpha);
        }
    }

    private static void drawOffsetCircleBand(VertexConsumer vertices, Matrix4f matrix, Vec3d center, float radius, float halfThickness, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        float innerRadius = Math.max(0.0F, radius - halfThickness);
        float outerRadius = radius + halfThickness;
        float y = (float) center.y + Y_OFFSET;
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            double nextAngle = (Math.PI * 2.0 * (i + 1)) / CIRCLE_SEGMENTS;
            float innerX = (float) center.x + (float) (Math.cos(angle) * innerRadius);
            float innerZ = (float) center.z + (float) (Math.sin(angle) * innerRadius);
            float outerX = (float) center.x + (float) (Math.cos(angle) * outerRadius);
            float outerZ = (float) center.z + (float) (Math.sin(angle) * outerRadius);
            float nextInnerX = (float) center.x + (float) (Math.cos(nextAngle) * innerRadius);
            float nextInnerZ = (float) center.z + (float) (Math.sin(nextAngle) * innerRadius);
            float nextOuterX = (float) center.x + (float) (Math.cos(nextAngle) * outerRadius);
            float nextOuterZ = (float) center.z + (float) (Math.sin(nextAngle) * outerRadius);

            vertices.vertex(matrix, innerX, y, innerZ).color(red, green, blue, alpha);
            vertices.vertex(matrix, outerX, y, outerZ).color(red, green, blue, alpha);
            vertices.vertex(matrix, nextOuterX, y, nextOuterZ).color(red, green, blue, alpha);
            vertices.vertex(matrix, nextInnerX, y, nextInnerZ).color(red, green, blue, alpha);
        }
    }

    private static void drawLinePulse(VertexConsumer vertices, Matrix4f matrix, int age, Vec3d start, Vec3d end, Vec3d side) {
        Vec3d line = end.subtract(start);
        double length = line.horizontalLength();
        if (length <= 0.01) {
            return;
        }

        Vec3d direction = line.normalize();
        int ticksSinceWaveStart = Math.floorMod(age, WAVE_INTERVAL_TICKS);
        if (ticksSinceWaveStart >= WAVE_DURATION_TICKS) {
            return;
        }

        float progress = ticksSinceWaveStart / WAVE_DURATION_TICKS;
        double centerDistance = MathHelper.lerp(progress, 0.0F, (float) length);
        float fade = 1.0F - progress;
        for (int trailIndex = 0; trailIndex <= 3; trailIndex++) {
            double trailDistance = centerDistance - trailIndex * 0.42;
            if (trailDistance < 0.0 || trailDistance > length) {
                continue;
            }

            double halfLength = trailIndex == 0 ? 0.42 : 0.32;
            Vec3d pulseStart = start.add(direction.multiply(Math.max(0.0, trailDistance - halfLength)));
            Vec3d pulseEnd = start.add(direction.multiply(Math.min(length, trailDistance + halfLength)));
            float trailFade = fade * (1.0F - trailIndex * 0.2F);
            int alpha = MathHelper.clamp((int) (120.0F * trailFade), 0, 120);
            drawLineBand(vertices, matrix, pulseStart, pulseEnd, side, 0.16F, 157, 98, 202, alpha);
        }
    }

    private static void drawLineBand(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end, Vec3d side, float halfThickness, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        Vec3d offset = side.multiply(halfThickness);
        vertices.vertex(matrix, (float) (start.x + offset.x), Y_OFFSET, (float) (start.z + offset.z)).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) (end.x + offset.x), Y_OFFSET, (float) (end.z + offset.z)).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) (end.x - offset.x), Y_OFFSET, (float) (end.z - offset.z)).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) (start.x - offset.x), Y_OFFSET, (float) (start.z - offset.z)).color(red, green, blue, alpha);
    }

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix, float minX, float minZ, float maxX, float maxZ, int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, minX, Y_OFFSET, minZ).color(red, green, blue, alpha);
        vertices.vertex(matrix, minX, Y_OFFSET, maxZ).color(red, green, blue, alpha);
        vertices.vertex(matrix, maxX, Y_OFFSET, maxZ).color(red, green, blue, alpha);
        vertices.vertex(matrix, maxX, Y_OFFSET, minZ).color(red, green, blue, alpha);
    }
}
