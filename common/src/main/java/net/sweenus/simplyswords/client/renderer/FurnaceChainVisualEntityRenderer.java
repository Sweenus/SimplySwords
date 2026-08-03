package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.FurnaceChainVisualEntity;
import org.joml.Matrix4f;

public class FurnaceChainVisualEntityRenderer extends EntityRenderer<FurnaceChainVisualEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int CHAIN_SEGMENTS = 28;
    private static final int RING_SEGMENTS = 64;
    private static final float PULSE_TRAVEL_TICKS = 10.0F;
    private static final float PULSE_TRAIL_LINKS = 5.0F;

    public FurnaceChainVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(FurnaceChainVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(FurnaceChainVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(28.0));
    }

    @Override
    public void render(FurnaceChainVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float time = entity.age + tickDelta;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer dark = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        Vec3d entityPos = lerpedPosition(entity, tickDelta);

        renderPass(entity, tickDelta, time, entityPos, matrix, dark, null);
        VertexConsumer glow = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        renderPass(entity, tickDelta, time, entityPos, matrix, null, glow);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void renderPass(FurnaceChainVisualEntity entity, float tickDelta, float time,
                                   Vec3d entityPos, Matrix4f matrix,
                                   VertexConsumer dark, VertexConsumer glow) {
        switch (entity.getMode()) {
            case FurnaceChainVisualEntity.MODE_BRAND ->
                    renderBrand(entity, tickDelta, time, entityPos, matrix, dark, glow);
            case FurnaceChainVisualEntity.MODE_CHAIN, FurnaceChainVisualEntity.MODE_SNAP ->
                    renderChain(entity, tickDelta, time, entityPos, matrix, dark, glow);
            case FurnaceChainVisualEntity.MODE_CORE ->
                    renderCore(entity, tickDelta, time, entityPos, matrix, dark, glow);
            case FurnaceChainVisualEntity.MODE_FINALE ->
                    renderFinale(entity, tickDelta, time, entityPos, matrix, dark, glow);
            default -> {
            }
        }
    }

    private static void renderBrand(FurnaceChainVisualEntity visual, float tickDelta, float time,
                                    Vec3d entityPos, Matrix4f matrix,
                                    VertexConsumer dark, VertexConsumer glow) {
        Entity target = visual.getWorld().getEntityById(visual.getTargetId());
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        Vec3d center = lerpedPosition(living, tickDelta)
                .add(0.0, living.getHeight() * 0.52, 0.0)
                .subtract(entityPos);
        float radius = Math.max(0.38F, living.getWidth() * 0.68F);
        float pulse = 0.82F + MathHelper.sin(time * 0.22F) * 0.12F;

        drawBrokenRing(dark, glow, matrix, center, radius, 0.035F,
                time * 0.045F, 18, 10, 8, 150,
                255, 74, 18, (int) (165 * pulse));
        drawBrokenRing(dark, glow, matrix, center.add(0.0, 0.13, 0.0), radius * 0.82F, 0.028F,
                -time * 0.06F, 26, 12, 8, 125,
                255, 132, 28, (int) (145 * pulse));
        if (glow != null) {
            drawVerticalEmbers(glow, matrix, center, radius, time, 8, 255, 84, 18);
        }
    }

    private static void renderCore(FurnaceChainVisualEntity visual, float tickDelta, float time,
                                   Vec3d entityPos, Matrix4f matrix,
                                   VertexConsumer dark, VertexConsumer glow) {
        Entity owner = visual.getWorld().getEntityById(visual.getOwnerId());
        if (!(owner instanceof LivingEntity living)) {
            return;
        }
        Vec3d center = lerpedPosition(living, tickDelta)
                .add(0.0, living.getHeight() * 0.43, 0.0)
                .subtract(entityPos);
        float heat = visual.getHeat();
        float pulse = 0.78F + MathHelper.sin(time * (0.12F + heat * 0.18F)) * (0.08F + heat * 0.08F);
        float radius = 0.65F + heat * 0.35F;

        drawBrokenRing(dark, glow, matrix, center, radius, 0.045F,
                time * (0.025F + heat * 0.035F), 24, 12, 9, 160,
                heatRed(heat), heatGreen(heat), 15, (int) (145 * pulse + heat * 70));
        drawBrokenRing(dark, glow, matrix, center.add(0.0, 0.18, 0.0), radius * 0.72F, 0.032F,
                -time * (0.04F + heat * 0.04F), 30, 13, 9, 135,
                255, MathHelper.clamp((int) (82 + heat * 100), 0, 255), 20, (int) (135 * pulse));
        if (glow != null) {
            drawVerticalEmbers(glow, matrix, center, radius, time, 6 + (int) (heat * 8.0F),
                    255, heatGreen(heat), 18);
        }

        if (heat >= 0.99F) {
            float flare = 0.5F + 0.5F * MathHelper.sin(time * 0.55F);
            drawBrokenRing(dark, glow, matrix, center.add(0.0, 0.38, 0.0),
                    radius * (1.05F + flare * 0.12F), 0.05F,
                    time * 0.09F, 255, 68, 20, 110,
                    255, 218, 125, 190 + (int) (flare * 55));
        }
    }

    private static void renderChain(FurnaceChainVisualEntity visual, float tickDelta, float time,
                                    Vec3d entityPos, Matrix4f matrix,
                                    VertexConsumer dark, VertexConsumer glow) {
        Entity ownerEntity = visual.getWorld().getEntityById(visual.getOwnerId());
        Entity targetEntity = visual.getWorld().getEntityById(visual.getTargetId());
        if (!(ownerEntity instanceof LivingEntity owner) || !(targetEntity instanceof LivingEntity target)) {
            return;
        }

        Vec3d start = weaponSideAnchor(owner, tickDelta).subtract(entityPos);
        Vec3d end = lerpedPosition(target, tickDelta)
                .add(0.0, target.getHeight() * 0.52, 0.0)
                .subtract(entityPos);
        float heat = visual.getHeat();
        float snapProgress = visual.getMode() == FurnaceChainVisualEntity.MODE_SNAP
                ? MathHelper.clamp((time - visual.getTransitionAge()) / 10.0F, 0.0F, 1.0F)
                : 0.0F;
        if (snapProgress > 0.0F) {
            end = end.lerp(start, easeOutCubic(snapProgress));
        }

        double length = end.subtract(start).length();
        if (length < 0.08) {
            return;
        }
        float fade = visual.getMode() == FurnaceChainVisualEntity.MODE_SNAP ? 1.0F - snapProgress : 1.0F;
        float sag = (float) Math.min(1.35, length * 0.12) * (1.0F - heat * 0.82F);
        float vibration = 0.015F + heat * 0.05F;
        Vec3d[] points = new Vec3d[CHAIN_SEGMENTS + 1];
        Vec3d line = end.subtract(start);
        Vec3d side = horizontalPerpendicular(line);
        for (int i = 0; i <= CHAIN_SEGMENTS; i++) {
            float progress = i / (float) CHAIN_SEGMENTS;
            double drop = -4.0 * sag * progress * (1.0 - progress);
            double wave = Math.sin(time * (0.22 + heat * 0.16) + i * 1.35 + visual.getPulse() * 0.7)
                    * vibration * Math.sin(progress * Math.PI);
            points[i] = start.lerp(end, progress).add(side.multiply(wave)).add(0.0, drop, 0.0);
        }

        int red = heatRed(heat);
        int green = heatGreen(heat);
        int blue = 12;
        int darkAlpha = MathHelper.clamp((int) (205.0F * fade), 0, 205);
        int glowAlpha = MathHelper.clamp((int) ((65.0F + heat * 180.0F) * fade), 0, 245);
        float pulseAge = time - visual.getPulseStartAge();
        boolean pulseActive = visual.getPulse() > 0
                && pulseAge >= 0.0F
                && pulseAge <= PULSE_TRAVEL_TICKS;
        float pulseHead = pulseActive
                ? MathHelper.clamp(pulseAge / PULSE_TRAVEL_TICKS, 0.0F, 1.0F)
                : -1.0F;
        float pulseLifetimeFade = pulseActive
                ? Math.min(
                MathHelper.clamp(pulseAge / 1.25F, 0.0F, 1.0F),
                MathHelper.clamp((PULSE_TRAVEL_TICKS - pulseAge) / 1.25F, 0.0F, 1.0F)
        )
                : 0.0F;

        for (int i = 0; i < CHAIN_SEGMENTS; i++) {
            Vec3d from = points[i];
            Vec3d to = points[i + 1];
            Vec3d direction = to.subtract(from);
            if (direction.lengthSquared() < 0.0001) {
                continue;
            }
            Vec3d firstAxis = horizontalPerpendicular(direction);
            Vec3d secondAxis = direction.normalize().crossProduct(firstAxis).normalize();
            Vec3d widthAxis = (i & 1) == 0 ? firstAxis : secondAxis;
            float pulseIntensity = pulseIntensity(i, pulseHead, pulseLifetimeFade);
            int linkDarkRed = lerpChannel(30 + (int) (heat * 35), 132, pulseIntensity);
            int linkDarkGreen = lerpChannel(18 + (int) (heat * 10), 58, pulseIntensity);
            int linkDarkBlue = lerpChannel(15, 20, pulseIntensity);
            int linkEdgeRed = lerpChannel(58 + (int) (heat * 55), 205, pulseIntensity);
            int linkEdgeGreen = lerpChannel(26, 92, pulseIntensity);
            int linkEdgeBlue = lerpChannel(16, 24, pulseIntensity);
            int linkGlowRed = lerpChannel(red, 255, pulseIntensity);
            int linkGlowGreen = lerpChannel(green, 226, pulseIntensity);
            int linkGlowBlue = lerpChannel(blue, 135, pulseIntensity);
            int linkGlowAlpha = MathHelper.clamp(
                    glowAlpha + (int) ((255 - glowAlpha) * pulseIntensity),
                    0,
                    255
            );
            if (dark != null) {
                drawRibbon(dark, matrix, from, to, widthAxis, 0.11F,
                        linkDarkRed, linkDarkGreen, linkDarkBlue, darkAlpha);
                drawRibbon(dark, matrix, from, to, secondAxis, 0.055F,
                        linkEdgeRed, linkEdgeGreen, linkEdgeBlue, darkAlpha);
            }
            if (glow != null) {
                drawEmissiveRibbon(glow, matrix, from, to, widthAxis,
                        0.035F + pulseIntensity * 0.025F,
                        linkGlowRed, linkGlowGreen, linkGlowBlue, linkGlowAlpha);
            }
            if (glow != null && pulseIntensity > 0.02F) {
                float coreIntensity = MathHelper.clamp(
                        (pulseIntensity - 0.18F) / 0.82F,
                        0.0F,
                        1.0F
                );
                drawEmissiveRibbon(glow, matrix, from, to, secondAxis,
                        0.018F + coreIntensity * 0.012F,
                        255,
                        lerpChannel(176, 248, coreIntensity),
                        lerpChannel(72, 205, coreIntensity),
                        MathHelper.clamp((int) (225.0F * pulseIntensity * fade), 0, 225));
            }
        }
    }

    private static void renderFinale(FurnaceChainVisualEntity visual, float tickDelta, float time,
                                     Vec3d entityPos, Matrix4f matrix,
                                     VertexConsumer dark, VertexConsumer glow) {
        Entity ownerEntity = visual.getWorld().getEntityById(visual.getOwnerId());
        Vec3d center;
        if (ownerEntity instanceof LivingEntity owner) {
            center = lerpedPosition(owner, tickDelta).subtract(entityPos).add(0.0, 0.08, 0.0);
        } else {
            center = Vec3d.ZERO;
        }
        float progress = MathHelper.clamp((time - visual.getTransitionAge()) / 16.0F, 0.0F, 1.0F);
        float fade = 1.0F - progress;
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float radius = 0.5F + eased * (4.2F + visual.getHeat() * 1.6F);

        drawSolidRing(dark, glow, matrix, center, radius, 0.10F + fade * 0.08F,
                70, 22, 10, (int) (145 * fade),
                255, 76 + (int) (visual.getHeat() * 80), 14, (int) (220 * fade));
        drawSolidRing(dark, glow, matrix, center, radius * 0.72F, 0.055F,
                60, 20, 10, (int) (100 * fade),
                255, 185, 65, (int) (175 * fade));

        float columnHeight = 0.6F + MathHelper.sin(progress * MathHelper.PI) * (4.5F + visual.getHeat() * 2.0F);
        if (glow != null) {
            for (int i = 0; i < 6; i++) {
                double angle = time * 0.15 + i * MathHelper.TAU / 6.0;
                Vec3d base = center.add(Math.cos(angle) * 0.34, 0.0, Math.sin(angle) * 0.34);
                Vec3d top = center.add(Math.cos(angle + 0.55) * 0.13, columnHeight, Math.sin(angle + 0.55) * 0.13);
                drawEmissiveRibbon(glow, matrix, base, top,
                        horizontalPerpendicular(top.subtract(base)), 0.09F + fade * 0.05F,
                        255, 82 + i * 18, 12, (int) (190 * fade));
            }
        }
    }

    private static Vec3d weaponSideAnchor(LivingEntity owner, float tickDelta) {
        Vec3d ownerPos = lerpedPosition(owner, tickDelta);
        float yaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevYaw, owner.getYaw());
        Vec3d forward = Vec3d.fromPolar(0.0F, yaw).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        return ownerPos.add(0.0, owner.getHeight() * 0.58, 0.0)
                .add(right.multiply(0.28 + owner.getWidth() * 0.2))
                .add(forward.multiply(0.16));
    }

    private static Vec3d lerpedPosition(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private static Vec3d horizontalPerpendicular(Vec3d direction) {
        Vec3d horizontal = new Vec3d(-direction.z, 0.0, direction.x);
        if (horizontal.lengthSquared() < 0.0001) {
            return new Vec3d(1.0, 0.0, 0.0);
        }
        return horizontal.normalize();
    }

    private static float pulseIntensity(int linkIndex, float pulseHead, float lifetimeFade) {
        if (pulseHead < 0.0F || lifetimeFade <= 0.0F) {
            return 0.0F;
        }
        float linkProgress = (linkIndex + 0.5F) / CHAIN_SEGMENTS;
        float distanceInLinks = (pulseHead - linkProgress) * CHAIN_SEGMENTS;
        if (distanceInLinks < -0.75F || distanceInLinks > PULSE_TRAIL_LINKS) {
            return 0.0F;
        }
        float shape = distanceInLinks < 0.0F
                ? 1.0F + distanceInLinks / 0.75F
                : 1.0F - distanceInLinks / PULSE_TRAIL_LINKS;
        shape = MathHelper.clamp(shape, 0.0F, 1.0F);
        return shape * shape * (3.0F - 2.0F * shape) * lifetimeFade;
    }

    private static int lerpChannel(int from, int to, float progress) {
        return MathHelper.clamp((int) MathHelper.lerp(
                MathHelper.clamp(progress, 0.0F, 1.0F),
                (float) from,
                (float) to
        ), 0, 255);
    }

    private static void drawBrokenRing(VertexConsumer dark, VertexConsumer glow, Matrix4f matrix,
                                       Vec3d center, float radius, float halfWidth, float rotation,
                                       int darkRed, int darkGreen, int darkBlue, int darkAlpha,
                                       int glowRed, int glowGreen, int glowBlue, int glowAlpha) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            if ((i + (int) (rotation * 11.0F)) % 7 == 0) {
                continue;
            }
            double angle = rotation + MathHelper.TAU * i / RING_SEGMENTS;
            double next = rotation + MathHelper.TAU * (i + 1) / RING_SEGMENTS;
            Vec3d from = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            Vec3d to = center.add(Math.cos(next) * radius, 0.0, Math.sin(next) * radius);
            Vec3d outward = new Vec3d(Math.cos((angle + next) * 0.5), 0.0, Math.sin((angle + next) * 0.5));
            if (dark != null) {
                drawRibbon(dark, matrix, from, to, outward, halfWidth,
                        darkRed, darkGreen, darkBlue, darkAlpha);
            }
            if (glow != null) {
                drawEmissiveRibbon(glow, matrix, from.add(0.0, 0.004, 0.0), to.add(0.0, 0.004, 0.0),
                        outward, halfWidth * 0.42F, glowRed, glowGreen, glowBlue, glowAlpha);
            }
        }
    }

    private static void drawSolidRing(VertexConsumer dark, VertexConsumer glow, Matrix4f matrix,
                                      Vec3d center, float radius, float halfWidth,
                                      int darkRed, int darkGreen, int darkBlue, int darkAlpha,
                                      int glowRed, int glowGreen, int glowBlue, int glowAlpha) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double angle = MathHelper.TAU * i / RING_SEGMENTS;
            double next = MathHelper.TAU * (i + 1) / RING_SEGMENTS;
            Vec3d from = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            Vec3d to = center.add(Math.cos(next) * radius, 0.0, Math.sin(next) * radius);
            Vec3d outward = new Vec3d(Math.cos((angle + next) * 0.5), 0.0, Math.sin((angle + next) * 0.5));
            if (dark != null) {
                drawRibbon(dark, matrix, from, to, outward, halfWidth,
                        darkRed, darkGreen, darkBlue, darkAlpha);
            }
            if (glow != null) {
                drawEmissiveRibbon(glow, matrix, from.add(0.0, 0.006, 0.0), to.add(0.0, 0.006, 0.0),
                        outward, halfWidth * 0.48F, glowRed, glowGreen, glowBlue, glowAlpha);
            }
        }
    }

    private static void drawVerticalEmbers(VertexConsumer glow, Matrix4f matrix, Vec3d center,
                                           float radius, float time, int count,
                                           int red, int green, int blue) {
        for (int i = 0; i < count; i++) {
            double angle = i * 2.399963 + time * (0.025 + (i % 3) * 0.008);
            double distance = radius * (0.35 + (i % 4) * 0.18);
            double rise = Math.floorMod((int) (time * 13.0F + i * 137), 1000) / 1000.0;
            Vec3d ember = center.add(
                    Math.cos(angle) * distance,
                    0.05 + rise * 1.35,
                    Math.sin(angle) * distance
            );
            drawFlare(glow, matrix, ember, 0.025F + (i % 3) * 0.008F,
                    red, green, blue, (int) (190 * (1.0 - rise)));
        }
    }

    private static void drawFlare(VertexConsumer glow, Matrix4f matrix, Vec3d center, float radius,
                                  int red, int green, int blue, int alpha) {
        Vec3d x = new Vec3d(radius, 0.0, 0.0);
        Vec3d y = new Vec3d(0.0, radius, 0.0);
        drawEmissiveQuad(glow, matrix,
                center.subtract(x).subtract(y),
                center.add(x).subtract(y),
                center.add(x).add(y),
                center.subtract(x).add(y),
                red, green, blue, alpha);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix,
                                   Vec3d start, Vec3d end, Vec3d widthAxis, float halfWidth,
                                   int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }
        Vec3d width = widthAxis.normalize().multiply(halfWidth);
        putDarkVertex(vertices, matrix, start.add(width), red, green, blue, alpha);
        putDarkVertex(vertices, matrix, start.subtract(width), red, green, blue, alpha);
        putDarkVertex(vertices, matrix, end.subtract(width), red, green, blue, alpha);
        putDarkVertex(vertices, matrix, end.add(width), red, green, blue, alpha);
    }

    private static void drawEmissiveRibbon(VertexConsumer vertices, Matrix4f matrix,
                                           Vec3d start, Vec3d end, Vec3d widthAxis, float halfWidth,
                                           int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }
        Vec3d width = widthAxis.normalize().multiply(halfWidth);
        drawEmissiveQuad(vertices, matrix,
                start.add(width), start.subtract(width), end.subtract(width), end.add(width),
                red, green, blue, alpha);
    }

    private static void drawEmissiveQuad(VertexConsumer vertices, Matrix4f matrix,
                                         Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                         int red, int green, int blue, int alpha) {
        putGlowVertex(vertices, matrix, a, red, green, blue, alpha);
        putGlowVertex(vertices, matrix, b, red, green, blue, alpha);
        putGlowVertex(vertices, matrix, c, red, green, blue, alpha);
        putGlowVertex(vertices, matrix, d, red, green, blue, alpha);
    }

    private static void putDarkVertex(VertexConsumer vertices, Matrix4f matrix, Vec3d pos,
                                      int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE).next();
    }

    private static void putGlowVertex(VertexConsumer vertices, Matrix4f matrix, Vec3d pos,
                                      int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0.0F, 1.0F, 0.0F).next();
    }

    private static int heatRed(float heat) {
        return MathHelper.clamp((int) MathHelper.lerp(heat, 118.0F, 255.0F), 0, 255);
    }

    private static int heatGreen(float heat) {
        return MathHelper.clamp((int) MathHelper.lerp(heat, 28.0F, 176.0F), 0, 255);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - MathHelper.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }
}
