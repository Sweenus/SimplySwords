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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.DeathKnellVisualEntity;
import org.joml.Matrix4f;

public class DeathKnellVisualEntityRenderer extends EntityRenderer<DeathKnellVisualEntity> {

    private static final Identifier WHITE_TEXTURE =
            new Identifier("minecraft", "textures/misc/white.png");
    private static final Identifier[] SOUL_TEXTURES = particleFrames("soul", 11);
    private static final Identifier[] SMOKE_TEXTURES = particleFrames("big_smoke", 12);
    private static final int BELL_SEGMENTS = 18;
    private static final int RING_SEGMENTS = 64;

    public DeathKnellVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(DeathKnellVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(DeathKnellVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(
                entity.getMode() == DeathKnellVisualEntity.MODE_TOLL
                        ? Math.max(6.0F, entity.getRadius() + 1.0F)
                        : 3.0F
        ));
    }

    @Override
    public void render(DeathKnellVisualEntity visual, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Entity target = visual.getWorld().getEntityById(visual.getTargetId());
        Vec3d relativeTarget = target == null
                ? Vec3d.ZERO
                : lerpedPosition(target, tickDelta).subtract(lerpedPosition(visual, tickDelta));
        float targetHeight = target instanceof LivingEntity living ? living.getHeight() : 1.8F;
        float targetWidth = target instanceof LivingEntity living ? living.getWidth() : 0.6F;
        float time = visual.age + tickDelta;

        if (visual.getMode() == DeathKnellVisualEntity.MODE_FEVER) {
            renderFever(visual, matrices, vertexConsumers,
                    relativeTarget, targetHeight, targetWidth, time, light);
        } else {
            renderToll(visual, matrices, vertexConsumers,
                    relativeTarget, targetHeight, targetWidth, time, light);
        }
        super.render(visual, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void renderFever(DeathKnellVisualEntity visual, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers,
                             Vec3d target, float targetHeight, float targetWidth,
                             float time, int light) {
        int maximum = Math.max(1, visual.getMaxStacks());
        int stacks = net.minecraft.util.math.MathHelper.clamp(visual.getStacks(), 0, maximum);
        if (stacks <= 0) {
            return;
        }
        float progress = stacks / (float) maximum;
        float bodyRadius = MathHelper.clamp(targetWidth * 0.88F, 0.46F, 1.55F);
        float circleRadius = Math.max(0.5F, targetWidth * 0.82F);
        float phase = visual.getSeed() * 0.017F;

        VertexConsumer dark = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        drawHorizontalRing(
                dark,
                null,
                matrices.peek().getPositionMatrix(),
                target.add(0.0, 0.055, 0.0),
                circleRadius * (1.0F + progress * 0.32F),
                0.025F + progress * 0.018F,
                phase + time * 0.018F,
                20, 33, 25, 120 + (int) (progress * 65),
                89, 240, 72, 90 + (int) (progress * 115)
        );
        VertexConsumer glow = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        drawHorizontalRing(
                null,
                glow,
                matrices.peek().getPositionMatrix(),
                target.add(0.0, 0.055, 0.0),
                circleRadius * (1.0F + progress * 0.32F),
                0.025F + progress * 0.018F,
                phase + time * 0.018F,
                20, 33, 25, 120 + (int) (progress * 65),
                89, 240, 72, 90 + (int) (progress * 115)
        );

        for (int i = 0; i < stacks; i++) {
            renderPestilentWisp(
                    visual,
                    matrices,
                    vertexConsumers,
                    target,
                    targetHeight,
                    bodyRadius,
                    stacks,
                    i,
                    progress,
                    phase,
                    time,
                    light
            );
        }
    }

    private void renderToll(DeathKnellVisualEntity visual, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers,
                            Vec3d target, float targetHeight, float targetWidth,
                            float time, int light) {
        float impactAge = Math.max(1.0F, visual.getImpactAge());
        float endAge = Math.max(impactAge + 1.0F, visual.getEndAge());
        float formation = easeOutBack(MathHelper.clamp(time / impactAge, 0.0F, 1.0F));
        float afterImpact = Math.max(0.0F, time - impactAge);
        float fade = MathHelper.clamp((endAge - time) / 6.0F, 0.0F, 1.0F);
        float depthHue = Math.min(1.0F, visual.getChainDepth() / 5.0F);
        Vec3d bellCenter = target.add(
                0.0,
                targetHeight + 1.10F + MathHelper.sin(time * 0.18F + visual.getSeed()) * 0.035F,
                0.0
        );

        renderFormationMotes(visual, matrices, vertexConsumers, target, bellCenter,
                time, formation, fade, light);

        VertexConsumer dark = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        renderTollPass(visual, matrices, dark, null, target, targetWidth, time,
                impactAge, afterImpact, formation, fade, depthHue, bellCenter);
        VertexConsumer glow = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        renderTollPass(visual, matrices, null, glow, target, targetWidth, time,
                impactAge, afterImpact, formation, fade, depthHue, bellCenter);
    }

    private static void renderTollPass(
            DeathKnellVisualEntity visual, MatrixStack matrices,
            VertexConsumer dark, VertexConsumer glow,
            Vec3d target, float targetWidth, float time,
            float impactAge, float afterImpact, float formation,
            float fade, float depthHue, Vec3d bellCenter) {
        matrices.push();
        matrices.translate(bellCenter.x, bellCenter.y, bellCenter.z);
        float swing;
        if (time < impactAge) {
            swing = MathHelper.lerp(MathHelper.clamp(time / impactAge, 0.0F, 1.0F), -27.0F, 20.0F);
        } else {
            swing = MathHelper.sin(afterImpact * 0.68F) * 24.0F
                    * MathHelper.clamp(1.0F - afterImpact / 15.0F, 0.0F, 1.0F);
        }
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(swing));
        float bellScale = Math.max(0.02F, formation) * (1.0F + visual.getChainDepth() * 0.025F);
        matrices.scale(bellScale, bellScale, bellScale);
        Matrix4f bellMatrix = matrices.peek().getPositionMatrix();
        renderBellShell(dark, glow, bellMatrix, time, fade, depthHue);
        matrices.pop();

        if (afterImpact > 0.0F) {
            float waveProgress = MathHelper.clamp(afterImpact / 12.0F, 0.0F, 1.0F);
            float eased = 1.0F - (float) Math.pow(1.0F - waveProgress, 3.0);
            float waveFade = MathHelper.clamp((1.0F - waveProgress) * 1.7F, 0.0F, 1.0F) * fade;
            float radius = MathHelper.lerp(eased, Math.max(0.45F, targetWidth), visual.getRadius());
            Vec3d groundCenter = target.add(0.0, 0.07, 0.0);
            Matrix4f matrix = matrixOf(matrices);
            drawHorizontalRing(
                    dark,
                    glow,
                    matrix,
                    groundCenter,
                    radius,
                    0.10F * (1.0F - waveProgress * 0.55F),
                    visual.getSeed() * 0.01F,
                    18, 31, 22, (int) (165 * waveFade),
                    100 + (int) (depthHue * 45),
                    255,
                    66 + (int) (depthHue * 65),
                    (int) (235 * waveFade)
            );
            drawHorizontalRing(
                    dark,
                    glow,
                    matrix,
                    groundCenter.add(0.0, 0.045, 0.0),
                    radius * 0.83F,
                    0.035F,
                    -visual.getSeed() * 0.007F,
                    12, 24, 17, (int) (105 * waveFade),
                    31, 181, 145, (int) (155 * waveFade)
            );
        }
    }

    private static void renderBellShell(VertexConsumer dark, VertexConsumer glow,
                                        Matrix4f matrix, float time, float fade,
                                        float depthHue) {
        int darkAlpha = (int) (235 * fade);
        drawFrustum(dark, matrix, 0.60F, 0.15F, 0.42F, 0.30F,
                17, 30, 22, darkAlpha, false);
        drawFrustum(dark, matrix, 0.42F, 0.30F, 0.08F, 0.50F,
                20, 38, 26, darkAlpha, false);
        drawFrustum(dark, matrix, 0.08F, 0.50F, -0.42F, 0.70F,
                15, 31, 21, darkAlpha, false);
        drawFrustum(dark, matrix, -0.42F, 0.70F, -0.54F, 0.80F,
                9, 22, 15, darkAlpha, false);

        drawVerticalBand(glow, matrix, -0.52F, 0.80F, 0.07F,
                114 + (int) (depthHue * 50), 255,
                66 + (int) (depthHue * 68), (int) (225 * fade));
        drawVerticalBand(glow, matrix, 0.08F, 0.505F, 0.028F,
                42, 188, 136, (int) (135 * fade));
        drawBrokenBellCracks(glow, matrix, time, fade, depthHue);
        drawBellHandle(dark, glow, matrix, fade);

        drawBox(dark, matrix, new Vec3d(0.0, -0.62, 0.0),
                0.075F, 0.32F, 0.075F,
                11, 23, 16, darkAlpha, false);
        drawCube(dark, matrix, new Vec3d(0.0, -0.84, 0.0),
                0.15F, 9, 20, 14, darkAlpha, false);
        drawCube(glow, matrix, new Vec3d(0.0, -0.84, 0.0),
                0.075F, 105, 255, 71, (int) (205 * fade), true);
    }

    private static void drawBrokenBellCracks(VertexConsumer glow, Matrix4f matrix,
                                              float time, float fade, float depthHue) {
        for (int i = 0; i < 7; i++) {
            float angle = i * MathHelper.TAU / 7.0F + 0.24F;
            float yTop = 0.30F - (i % 3) * 0.10F;
            float yBottom = -0.25F - (i % 2) * 0.08F;
            Vec3d start = new Vec3d(
                    MathHelper.cos(angle) * radiusAtBellY(yTop) * 1.015F,
                    yTop,
                    MathHelper.sin(angle) * radiusAtBellY(yTop) * 1.015F
            );
            float lowerAngle = angle + (i % 2 == 0 ? 0.12F : -0.10F);
            Vec3d end = new Vec3d(
                    MathHelper.cos(lowerAngle) * radiusAtBellY(yBottom) * 1.018F,
                    yBottom,
                    MathHelper.sin(lowerAngle) * radiusAtBellY(yBottom) * 1.018F
            );
            float pulse = 0.76F + MathHelper.sin(time * 0.33F + i) * 0.2F;
            drawCrossRibbon(
                    glow,
                    matrix,
                    start,
                    end,
                    0.018F,
                    118 + (int) (depthHue * 48),
                    255,
                    67 + (int) (depthHue * 70),
                    (int) (205 * fade * pulse),
                    true
            );
        }
    }

    private static void drawBellHandle(VertexConsumer dark, VertexConsumer glow,
                                       Matrix4f matrix, float fade) {
        int pieces = 9;
        for (int i = 0; i < pieces; i++) {
            float t = i / (float) (pieces - 1);
            float angle = MathHelper.lerp(t, MathHelper.PI, 0.0F);
            Vec3d point = new Vec3d(
                    MathHelper.cos(angle) * 0.25F,
                    0.60F + MathHelper.sin(angle) * 0.28F,
                    0.0
            );
            drawCube(dark, matrix, point, 0.075F,
                    10, 24, 16, (int) (230 * fade), false);
            if (i == 1 || i == pieces - 2) {
                drawCube(glow, matrix, point, 0.035F,
                        74, 218, 99, (int) (150 * fade), true);
            }
        }
    }

    private void renderFormationMotes(DeathKnellVisualEntity visual, MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers,
                                      Vec3d target, Vec3d bellCenter, float time,
                                      float formation, float fade, int light) {
        int count = 10;
        for (int i = 0; i < count; i++) {
            float offset = i / (float) count;
            float angle = visual.getSeed() * 0.013F + i * 2.399F + time * 0.10F;
            float inverse = 1.0F - MathHelper.clamp(formation, 0.0F, 1.0F);
            float radius = 0.20F + inverse * (0.75F + offset * 0.55F);
            Vec3d destination = bellCenter.add(
                    MathHelper.cos(angle) * radius,
                    MathHelper.lerp(offset, -0.42F, 0.60F),
                    MathHelper.sin(angle) * radius
            );
            Vec3d origin = target.add(
                    MathHelper.cos(angle + offset) * (0.45F + offset * 0.7F),
                    0.15F + offset * 1.25F,
                    MathHelper.sin(angle + offset) * (0.45F + offset * 0.7F)
            );
            Vec3d point = origin.lerp(destination, formation);
            int soulFrame = Math.floorMod((int) (time * 0.72F) + i * 2, SOUL_TEXTURES.length);
            int smokeFrame = Math.floorMod((int) (time * 0.48F) + i * 3, SMOKE_TEXTURES.length);
            renderWispBillboard(
                    matrices,
                    vertexConsumers,
                    point,
                    0.11F + (i % 3) * 0.012F,
                    angle * (180.0F / MathHelper.PI),
                    SOUL_TEXTURES[soulFrame],
                    SMOKE_TEXTURES[smokeFrame],
                    (int) (145 * fade),
                    (int) (215 * fade),
                    0.84F,
                    light
            );
        }
    }

    private void renderPestilentWisp(DeathKnellVisualEntity visual,
                                     MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     Vec3d target, float targetHeight, float bodyRadius,
                                     int stackCount, int stackIndex, float progress,
                                     float phase, float time, int light) {
        float speed = 0.035F + progress * 0.050F;
        float trailSpacing = 1.45F + progress * 0.65F;
        float baseSize = MathHelper.clamp(
                0.20F + progress * 0.055F + bodyRadius * 0.025F,
                0.20F,
                0.31F
        );

        for (int trail = 3; trail >= 0; trail--) {
            float sampleTime = time - trail * trailSpacing;
            Vec3d point = feverWispPosition(
                    visual,
                    target,
                    targetHeight,
                    bodyRadius,
                    stackCount,
                    stackIndex,
                    progress,
                    phase,
                    sampleTime,
                    speed
            );
            float trailFade = trail == 0
                    ? 1.0F
                    : (1.0F - trail / 4.2F) * (0.42F + progress * 0.18F);
            float size = baseSize * (1.0F - trail * 0.105F);
            int soulFrame = Math.floorMod(
                    (int) (sampleTime * 0.70F) + stackIndex * 3,
                    SOUL_TEXTURES.length
            );
            int smokeFrame = Math.floorMod(
                    (int) (sampleTime * 0.48F) + stackIndex * 5,
                    SMOKE_TEXTURES.length
            );
            float pulse = 0.92F
                    + MathHelper.sin(sampleTime * 0.22F + stackIndex * 1.91F) * 0.08F;
            renderWispBillboard(
                    matrices,
                    vertexConsumers,
                    point,
                    size * pulse,
                    -sampleTime * (2.8F + progress * 2.1F) + stackIndex * 37.0F,
                    SOUL_TEXTURES[soulFrame],
                    SMOKE_TEXTURES[smokeFrame],
                    Math.max(1, (int) ((trail == 0 ? 178 : 78) * trailFade)),
                    trail == 0 ? 230 : Math.max(1, (int) (82 * trailFade)),
                    trail == 0 ? 1.0F : 0.58F,
                    light
            );
        }
    }

    private static Vec3d feverWispPosition(DeathKnellVisualEntity visual,
                                           Vec3d target, float targetHeight, float bodyRadius,
                                           int stackCount, int stackIndex, float progress,
                                           float phase, float sampleTime, float speed) {
        float separation = MathHelper.TAU * stackIndex / Math.max(1, stackCount);
        float angle = phase + separation + sampleTime * speed;
        float heightSeed = Math.floorMod(
                visual.getSeed() + stackIndex * 41,
                97
        ) / 96.0F;
        float baseHeight = targetHeight * MathHelper.lerp(heightSeed, 0.30F, 0.72F);
        float bob = MathHelper.sin(sampleTime * 0.16F + stackIndex * 1.73F)
                * MathHelper.clamp(targetHeight * 0.065F, 0.07F, 0.18F);
        float agitation = Math.max(0.0F, progress - 0.55F) / 0.45F;
        float dartWave = Math.max(
                0.0F,
                MathHelper.sin(sampleTime * 0.24F + stackIndex * 2.37F)
        );
        float inwardDart = (float) Math.pow(dartWave, 7.0) * agitation * 0.30F;
        float radiusPulse = 1.0F
                + MathHelper.sin(sampleTime * 0.075F + stackIndex) * 0.10F
                - inwardDart;
        return target.add(
                MathHelper.cos(angle) * bodyRadius * radiusPulse,
                baseHeight + bob + inwardDart * targetHeight * 0.18F,
                MathHelper.sin(angle) * bodyRadius * radiusPulse
        );
    }

    private void renderWispBillboard(MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     Vec3d position, float size, float rollDegrees,
                                     Identifier soulTexture, Identifier smokeTexture,
                                     int smokeAlpha, int coreAlpha,
                                     float coreScale, int light) {
        matrices.push();
        matrices.translate(position.x, position.y, position.z);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollDegrees));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer smoke = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucent(smokeTexture));
        drawSpriteQuad(
                smoke,
                matrix,
                size * 0.82F,
                size * 1.22F,
                0.0F,
                30,
                92,
                58,
                smokeAlpha,
                light
        );

        VertexConsumer core = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(soulTexture));
        drawSpriteQuad(
                core,
                matrix,
                size * 0.52F * coreScale,
                size * 0.88F * coreScale,
                0.006F,
                142,
                255,
                108,
                coreAlpha,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );
        matrices.pop();
    }

    private static float radiusAtBellY(float y) {
        if (y >= 0.42F) {
            return MathHelper.lerp((0.60F - y) / 0.18F, 0.15F, 0.30F);
        }
        if (y >= 0.08F) {
            return MathHelper.lerp((0.42F - y) / 0.34F, 0.30F, 0.50F);
        }
        return MathHelper.lerp((0.08F - y) / 0.50F, 0.50F, 0.70F);
    }

    private static void drawFrustum(VertexConsumer vertices, Matrix4f matrix,
                                    float topY, float topRadius,
                                    float bottomY, float bottomRadius,
                                    int red, int green, int blue, int alpha,
                                    boolean emissive) {
        for (int segment = 0; segment < BELL_SEGMENTS; segment++) {
            float start = MathHelper.TAU * segment / BELL_SEGMENTS;
            float end = MathHelper.TAU * (segment + 1) / BELL_SEGMENTS;
            Vec3d a = new Vec3d(MathHelper.cos(start) * topRadius, topY,
                    MathHelper.sin(start) * topRadius);
            Vec3d b = new Vec3d(MathHelper.cos(end) * topRadius, topY,
                    MathHelper.sin(end) * topRadius);
            Vec3d c = new Vec3d(MathHelper.cos(end) * bottomRadius, bottomY,
                    MathHelper.sin(end) * bottomRadius);
            Vec3d d = new Vec3d(MathHelper.cos(start) * bottomRadius, bottomY,
                    MathHelper.sin(start) * bottomRadius);
            drawQuad(vertices, matrix, a, b, c, d, red, green, blue, alpha, emissive);
        }
    }

    private static void drawVerticalBand(VertexConsumer vertices, Matrix4f matrix,
                                         float y, float radius, float height,
                                         int red, int green, int blue, int alpha) {
        drawFrustum(vertices, matrix, y + height, radius, y - height, radius,
                red, green, blue, alpha, true);
    }

    private static void drawHorizontalRing(VertexConsumer dark, VertexConsumer glow,
                                           Matrix4f matrix, Vec3d center,
                                           float radius, float width, float rotation,
                                           int darkRed, int darkGreen, int darkBlue, int darkAlpha,
                                           int glowRed, int glowGreen, int glowBlue, int glowAlpha) {
        float inner = Math.max(0.01F, radius - Math.max(0.01F, width));
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            float start = rotation + MathHelper.TAU * segment / RING_SEGMENTS;
            float end = rotation + MathHelper.TAU * (segment + 1) / RING_SEGMENTS;
            Vec3d a = center.add(MathHelper.cos(start) * radius, 0.0,
                    MathHelper.sin(start) * radius);
            Vec3d b = center.add(MathHelper.cos(end) * radius, 0.0,
                    MathHelper.sin(end) * radius);
            Vec3d c = center.add(MathHelper.cos(end) * inner, 0.0,
                    MathHelper.sin(end) * inner);
            Vec3d d = center.add(MathHelper.cos(start) * inner, 0.0,
                    MathHelper.sin(start) * inner);
            drawQuad(dark, matrix, a, b, c, d,
                    darkRed, darkGreen, darkBlue, darkAlpha, false);
            drawQuad(glow, matrix, a.add(0.0, 0.006, 0.0),
                    b.add(0.0, 0.006, 0.0),
                    c.add(0.0, 0.006, 0.0),
                    d.add(0.0, 0.006, 0.0),
                    glowRed, glowGreen, glowBlue, glowAlpha, true);
        }
    }

    private static void drawCrossRibbon(VertexConsumer vertices, Matrix4f matrix,
                                        Vec3d start, Vec3d end, float width,
                                        int red, int green, int blue, int alpha,
                                        boolean emissive) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-6) {
            return;
        }
        Vec3d horizontal = new Vec3d(-direction.z, 0.0, direction.x);
        if (horizontal.lengthSquared() < 1.0E-6) {
            horizontal = new Vec3d(1.0, 0.0, 0.0);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3d vertical = direction.crossProduct(horizontal).normalize();
        drawRibbon(vertices, matrix, start, end, horizontal, width,
                red, green, blue, alpha, emissive);
        drawRibbon(vertices, matrix, start, end, vertical, width,
                red, green, blue, alpha, emissive);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix,
                                   Vec3d start, Vec3d end, Vec3d widthAxis, float width,
                                   int red, int green, int blue, int alpha,
                                   boolean emissive) {
        Vec3d offset = widthAxis.multiply(width);
        drawQuad(vertices, matrix,
                start.add(offset), start.subtract(offset),
                end.subtract(offset), end.add(offset),
                red, green, blue, alpha, emissive);
    }

    private static void drawCube(VertexConsumer vertices, Matrix4f matrix,
                                 Vec3d center, float halfSize,
                                 int red, int green, int blue, int alpha,
                                 boolean emissive) {
        drawBox(vertices, matrix, center, halfSize, halfSize, halfSize,
                red, green, blue, alpha, emissive);
    }

    private static void drawBox(VertexConsumer vertices, Matrix4f matrix,
                                Vec3d center, float halfX, float halfY, float halfZ,
                                int red, int green, int blue, int alpha,
                                boolean emissive) {
        Vec3d p000 = center.add(-halfX, -halfY, -halfZ);
        Vec3d p001 = center.add(-halfX, -halfY, halfZ);
        Vec3d p010 = center.add(-halfX, halfY, -halfZ);
        Vec3d p011 = center.add(-halfX, halfY, halfZ);
        Vec3d p100 = center.add(halfX, -halfY, -halfZ);
        Vec3d p101 = center.add(halfX, -halfY, halfZ);
        Vec3d p110 = center.add(halfX, halfY, -halfZ);
        Vec3d p111 = center.add(halfX, halfY, halfZ);
        drawQuad(vertices, matrix, p000, p100, p110, p010, red, green, blue, alpha, emissive);
        drawQuad(vertices, matrix, p101, p001, p011, p111, red, green, blue, alpha, emissive);
        drawQuad(vertices, matrix, p001, p000, p010, p011, red, green, blue, alpha, emissive);
        drawQuad(vertices, matrix, p100, p101, p111, p110, red, green, blue, alpha, emissive);
        drawQuad(vertices, matrix, p010, p110, p111, p011, red, green, blue, alpha, emissive);
        drawQuad(vertices, matrix, p001, p101, p100, p000, red, green, blue, alpha, emissive);
    }

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix,
                                 Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                 int red, int green, int blue, int alpha,
                                 boolean emissive) {
        if (vertices == null || alpha <= 0) {
            return;
        }
        putVertex(vertices, matrix, a, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, b, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, c, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, d, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, d, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, c, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, b, red, green, blue, alpha, emissive);
        putVertex(vertices, matrix, a, red, green, blue, alpha, emissive);
    }

    private static void putVertex(VertexConsumer vertices, Matrix4f matrix,
                                  Vec3d position,
                                  int red, int green, int blue, int alpha,
                                  boolean emissive) {
        var vertex = vertices.vertex(
                matrix,
                (float) position.x,
                (float) position.y,
                (float) position.z
        ).color(red, green, blue, alpha);
        if (emissive) {
            vertex.texture(0.5F, 0.5F)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(0.0F, 1.0F, 0.0F);
        } else {
            vertex.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        vertex.next();
    }

    private static void drawSpriteQuad(VertexConsumer vertices, Matrix4f matrix,
                                       float halfWidth, float halfHeight, float z,
                                       int red, int green, int blue, int alpha,
                                       int light) {
        putTexturedVertex(vertices, matrix, -halfWidth, -halfHeight, z,
                0.0F, 1.0F, red, green, blue, alpha, light);
        putTexturedVertex(vertices, matrix, halfWidth, -halfHeight, z,
                1.0F, 1.0F, red, green, blue, alpha, light);
        putTexturedVertex(vertices, matrix, halfWidth, halfHeight, z,
                1.0F, 0.0F, red, green, blue, alpha, light);
        putTexturedVertex(vertices, matrix, -halfWidth, halfHeight, z,
                0.0F, 0.0F, red, green, blue, alpha, light);
    }

    private static void putTexturedVertex(VertexConsumer vertices, Matrix4f matrix,
                                          Vec3d position, float u, float v,
                                          int red, int green, int blue, int alpha,
                                          int light) {
        putTexturedVertex(
                vertices,
                matrix,
                (float) position.x,
                (float) position.y,
                (float) position.z,
                u,
                v,
                red,
                green,
                blue,
                alpha,
                light
        );
    }

    private static void putTexturedVertex(VertexConsumer vertices, Matrix4f matrix,
                                          float x, float y, float z, float u, float v,
                                          int red, int green, int blue, int alpha,
                                          int light) {
        vertices.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0.0F, 1.0F, 0.0F).next();
    }

    private static Identifier[] particleFrames(String name, int count) {
        Identifier[] frames = new Identifier[Math.max(1, count)];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new Identifier("minecraft", "textures/particle/" + name + "_" + i + ".png");
        }
        return frames;
    }

    private static Matrix4f matrixOf(MatrixStack matrices) {
        return matrices.peek().getPositionMatrix();
    }

    private static Vec3d lerpedPosition(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private static float easeOutBack(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float shifted = clamped - 1.0F;
        float overshoot = 1.35F;
        return 1.0F + shifted * shifted * ((overshoot + 1.0F) * shifted + overshoot);
    }
}
