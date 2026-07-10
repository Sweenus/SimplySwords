package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
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

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix, float minX, float minZ, float maxX, float maxZ, int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, minX, Y_OFFSET, minZ).color(red, green, blue, alpha);
        vertices.vertex(matrix, minX, Y_OFFSET, maxZ).color(red, green, blue, alpha);
        vertices.vertex(matrix, maxX, Y_OFFSET, maxZ).color(red, green, blue, alpha);
        vertices.vertex(matrix, maxX, Y_OFFSET, minZ).color(red, green, blue, alpha);
    }
}
