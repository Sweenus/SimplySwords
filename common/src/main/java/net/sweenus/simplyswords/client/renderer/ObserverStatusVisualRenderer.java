package net.sweenus.simplyswords.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.ObserverStatusEffectSnapshot;
import net.sweenus.simplyswords.api.ObserverStatusVisualRegistry;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualShape;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualStyle;
import net.sweenus.simplyswords.client.api.ObserverStatusEffectClientApi;
import net.sweenus.simplyswords.client.render.BoltPath;
import net.sweenus.simplyswords.client.render.LightningRenderLayers;
import net.sweenus.simplyswords.client.render.MinecraftLightningRenderer;
import net.sweenus.simplyswords.client.util.ParchmentRendering;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class ObserverStatusVisualRenderer {

    private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int GROUND_RING_SEGMENTS = 48;

    private ObserverStatusVisualRenderer() {
    }

    public static void render(LivingEntity entity, float tickDelta, MatrixStack matrices,
                              VertexConsumerProvider consumers, int light) {
        if (entity == null) return;
        for (Map.Entry<Identifier, ObserverStatusVisualStyle> entry
                : ObserverStatusVisualRegistry.registeredStyles().entrySet()) {
            Optional<ObserverStatusEffectSnapshot> snapshot =
                    ObserverStatusEffectClientApi.get(entity, entry.getKey());
            if (snapshot.isEmpty()) continue;
            ObserverStatusVisualStyle style = entry.getValue();
            int stacks = Math.max(1, snapshot.get().amplifier() + 1);
            switch (style.shape()) {
                case LIGHTNING_ROD -> renderRod(entity, tickDelta, style, matrices, consumers, light);
                case ORBITING_GLYPHS -> renderOrbitingGlyphs(entity, tickDelta, style, stacks, matrices, consumers);
                case PARCHMENT_BAND -> renderParchmentBand(entity, tickDelta, style, matrices, consumers, light);
                case GROUND_RING -> renderGroundRing(entity, tickDelta, style, matrices, consumers);
                default -> renderStatic(entity, tickDelta, style, matrices, consumers);
            }
        }
    }

    private static void renderGroundRing(LivingEntity entity, float tickDelta, ObserverStatusVisualStyle style,
                                         MatrixStack matrices, VertexConsumerProvider consumers) {
        float age = entity.age + tickDelta;
        float pulse = 0.96F + MathHelper.sin(age * 0.09F) * 0.04F;
        float radius = Math.max(0.5F, entity.getWidth() * 0.82F) * style.scale() * pulse;
        float width = Math.max(0.03F, radius * 0.065F);
        float rotation = age * 0.006F + (entity.getId() & 31) * 0.17F;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        drawGroundRing(dark, matrix, radius, width, 0.055F, rotation,
                style.primaryColor(), 178, false);

        VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        drawGroundRing(glow, matrix, radius, width * 0.56F, 0.061F, -rotation,
                style.coreColor(), 205, true);
    }

    private static void drawGroundRing(VertexConsumer vertices, Matrix4f matrix,
                                       float radius, float width, float y, float rotation,
                                       int color, int alpha, boolean emissive) {
        float inner = Math.max(0.01F, radius - width);
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        for (int segment = 0; segment < GROUND_RING_SEGMENTS; segment++) {
            float start = rotation + MathHelper.TAU * segment / GROUND_RING_SEGMENTS;
            float end = rotation + MathHelper.TAU * (segment + 1) / GROUND_RING_SEGMENTS;
            float outerStartX = MathHelper.cos(start) * radius;
            float outerStartZ = MathHelper.sin(start) * radius;
            float outerEndX = MathHelper.cos(end) * radius;
            float outerEndZ = MathHelper.sin(end) * radius;
            float innerEndX = MathHelper.cos(end) * inner;
            float innerEndZ = MathHelper.sin(end) * inner;
            float innerStartX = MathHelper.cos(start) * inner;
            float innerStartZ = MathHelper.sin(start) * inner;
            ringVertex(vertices, matrix, outerStartX, y, outerStartZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, outerEndX, y, outerEndZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, innerEndX, y, innerEndZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, innerStartX, y, innerStartZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, innerStartX, y, innerStartZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, innerEndX, y, innerEndZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, outerEndX, y, outerEndZ, red, green, blue, alpha, emissive);
            ringVertex(vertices, matrix, outerStartX, y, outerStartZ, red, green, blue, alpha, emissive);
        }
    }

    private static void ringVertex(VertexConsumer vertices, Matrix4f matrix,
                                   float x, float y, float z,
                                   int red, int green, int blue, int alpha, boolean emissive) {
        var vertex = vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha);
        if (emissive) {
            vertex.texture(0.5F, 0.5F)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(0.0F, 1.0F, 0.0F);
        } else {
            vertex.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
    }

    private static void renderStatic(LivingEntity entity, float tickDelta, ObserverStatusVisualStyle style,
                                     MatrixStack matrices, VertexConsumerProvider consumers) {
        float age = entity.age + tickDelta;
        long epoch = MathHelper.floor(age / 4.0F);
        VertexConsumer vertices = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float bodyRadius = Math.max(0.28F, entity.getWidth() * 0.72F) * style.scale();
        float height = Math.max(0.7F, entity.getHeight());
        for (int i = 0; i < style.density(); i++) {
            long seed = entity.getUuid().getLeastSignificantBits() + i * 7919L;
            double angle = BoltPath.unit(seed, epoch, 1L) * MathHelper.TAU;
            double y = height * (0.14 + BoltPath.unit(seed, epoch, 2L) * 0.72);
            Vec3d start = new Vec3d(Math.cos(angle) * bodyRadius, y, Math.sin(angle) * bodyRadius);
            double sweep = (BoltPath.unit(seed, epoch, 3L) - 0.5) * 1.4;
            Vec3d end = new Vec3d(Math.cos(angle + sweep) * bodyRadius,
                    MathHelper.clamp(y + (BoltPath.unit(seed, epoch, 4L) - 0.5) * height * 0.34,
                            0.1, height),
                    Math.sin(angle + sweep) * bodyRadius);
            float[] path = MinecraftLightningRenderer.generate(start, end, seed, age,
                    0.11F, 4, 4.0F);
            MinecraftLightningRenderer.draw(vertices, matrix, path, 0.026F, 0.012F,
                    style.coreColor(), style.primaryColor(), 0.82F, true);
        }
    }

    private static void renderOrbitingGlyphs(LivingEntity entity, float tickDelta, ObserverStatusVisualStyle style,
                                             int stacks, MatrixStack matrices, VertexConsumerProvider consumers) {
        float age = entity.age + tickDelta;
        float radius = Math.max(0.3F, entity.getWidth() * 0.7F) * style.scale();
        float height = Math.max(0.7F, entity.getHeight());
        long entitySeed = entity.getUuid().getLeastSignificantBits();
        float spin = age * 0.9F;

        for (int i = 0; i < stacks; i++) {
            long seed = entitySeed + i * 6871L;
            float angle = i * (360.0F / stacks) + spin;
            float rad = angle * MathHelper.RADIANS_PER_DEGREE;
            float y = height * (0.28F + (float) BoltPath.unit(seed, 0L, 1L) * 0.46F)
                    + MathHelper.sin(age * 0.09F + i * 1.7F) * 0.05F;
            int glyphIndex = (int) (BoltPath.unit(seed, 0L, 2L) * 64.0);

            matrices.push();
            matrices.translate(MathHelper.cos(rad) * radius, y, MathHelper.sin(rad) * radius);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F - angle));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.sin(age * 0.12F + i * 2.1F) * 11.0F));
            ParchmentRendering.renderGlyph(matrices, consumers, style.glyphStyleId(), glyphIndex,
                    0.22F * style.scale(), 0.85F);
            matrices.pop();
        }
    }

    private static void renderParchmentBand(LivingEntity entity, float tickDelta, ObserverStatusVisualStyle style,
                                            MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        float age = entity.age + tickDelta;
        float radius = Math.max(0.32F, entity.getWidth() * 0.62F) * style.scale();
        float phase = (entity.getUuid().getLeastSignificantBits() % 628L) / 100.0F;

        int samples = Math.max(32, (int) (radius * 24.0F));
        List<Vec3d> path = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            float angle = (i / (float) samples) * MathHelper.TAU;
            float waveY = 0.38F + MathHelper.sin(angle * 3.0F + phase + age * 0.16F) * 0.14F;
            path.add(new Vec3d(
                    MathHelper.cos(angle) * radius,
                    waveY,
                    MathHelper.sin(angle) * radius));
        }

        matrices.push();
        ParchmentRendering.renderRibbon(matrices, consumers, light, path, 0.14F, UP, age, 0.85F);
        matrices.pop();
    }

    private static void renderRod(LivingEntity entity, float tickDelta, ObserverStatusVisualStyle style,
                                  MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        float scale = MathHelper.clamp(0.62F * style.scale(), 0.35F, 0.9F);
        float baseY = Math.max(0.28F, entity.getHeight() * 0.45F);
        float behind = Math.max(0.12F, entity.getWidth() * 0.18F);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0.0, baseY, behind);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F));
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, 0.0, -0.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.LIGHTNING_ROD.getDefaultState(), matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
