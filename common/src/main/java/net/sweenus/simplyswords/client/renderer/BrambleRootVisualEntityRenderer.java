package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BrambleRootVisualEntity;
import org.joml.Matrix4f;

public class BrambleRootVisualEntityRenderer extends EntityRenderer<BrambleRootVisualEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int PATH_SEGMENTS = 24;
    private static final int SPIRAL_SEGMENTS = 30;
    private static final int CORE_SEGMENTS = 44;
    private static final int CORE_ROOT_COUNT = 8;
    private static final int CORE_ROOT_SEGMENTS = 6;
    private static final float PRIMARY_TRAIL_LENGTH = 0.40F;
    private static final float PRIMARY_TRAIL_FADE_LENGTH = 0.14F;
    private static final float PRIMARY_ARRIVAL_FADE_TICKS = 6.0F;
    private static final int FULL_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    public BrambleRootVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(BrambleRootVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(BrambleRootVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(24.0, 8.0, 24.0));
    }

    @Override
    public void render(BrambleRootVisualEntity visual, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float age = visual.age + tickDelta;
        float alpha = phaseAlpha(visual, age);
        if (alpha <= 0.01F) {
            return;
        }

        Vec3d entityPos = lerpedPosition(visual, tickDelta);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Keep each pass self-contained. NeoForge may finish a shared buffer when a new
        // render layer is requested, so consumers must not be retained across layer switches.
        VertexConsumer solid = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        renderPass(visual, tickDelta, age, alpha, entityPos, matrix, solid, false, light);

        float pulseAge = age - visual.getPulseStartAge();
        boolean needsGlow = pulseAge >= 0.0F && pulseAge <= 13.0F
                || visual.getMode() == BrambleRootVisualEntity.MODE_CORE
                && visual.getPhase() == BrambleRootVisualEntity.PHASE_SLAM;
        if (needsGlow) {
            VertexConsumer glow = vertexConsumers.getBuffer(
                    RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
            renderPass(visual, tickDelta, age, alpha, entityPos, matrix, glow, true, FULL_LIGHT);
        }

        super.render(visual, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void renderPass(BrambleRootVisualEntity visual, float tickDelta,
                                   float age, float alpha, Vec3d entityPos,
                                   Matrix4f matrix, VertexConsumer vertices,
                                   boolean glow, int renderLight) {
        if (visual.getMode() == BrambleRootVisualEntity.MODE_CORE) {
            renderCore(visual, age, alpha, matrix, vertices, glow, renderLight);
            return;
        }

        Entity targetEntity = visual.getWorld().getEntityById(visual.getTargetId());
        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        Vec3d start;
        if (visual.getMode() == BrambleRootVisualEntity.MODE_PRIMARY) {
            Entity source = visual.getWorld().getEntityById(visual.getSourceId());
            start = source == null ? Vec3d.ZERO
                    : lerpedPosition(source, tickDelta).subtract(entityPos);
        } else {
            start = Vec3d.ZERO;
        }
        Vec3d targetPosition = lerpedPosition(target, tickDelta).subtract(entityPos);
        Vec3d targetWorldPosition = entityPos.add(targetPosition);
        Vec3d end = new Vec3d(targetPosition.x,
                sampleGroundY(visual, targetWorldPosition, targetWorldPosition.y) - entityPos.y + 0.045,
                targetPosition.z);

        Vec3d startWorldPosition = entityPos.add(start);
        Vec3d groundedStart = new Vec3d(start.x,
                sampleGroundY(visual, startWorldPosition, startWorldPosition.y) - entityPos.y + 0.045,
                start.z);
        Vec3d[] points = buildPath(visual, groundedStart, end, entityPos);
        float travelTicks = visual.getMode() == BrambleRootVisualEntity.MODE_HUNT
                ? Math.max(1.0F, Config.uniqueEffects.bramblethorn.huntTravelTicks)
                : Math.max(1.0F, Config.uniqueEffects.bramblethorn.rootTravelTicks);
        float branchDelay = visual.getMode() == BrambleRootVisualEntity.MODE_BRANCH
                ? travelTicks * 0.34F : 0.0F;
        float branchDuration = Math.max(1.0F, travelTicks - branchDelay);
        float reveal = visual.getPhase() == BrambleRootVisualEntity.PHASE_GROW
                ? easeOut(MathHelper.clamp((age - branchDelay) / branchDuration, 0.0F, 1.0F))
                : 1.0F;
        renderRootPath(visual, points, reveal, age, travelTicks, alpha,
                matrix, vertices, glow, renderLight);

        if (visual.getMode() != BrambleRootVisualEntity.MODE_HUNT
                && reveal >= 0.94F
                && visual.getPhase() != BrambleRootVisualEntity.PHASE_GROW) {
            renderBinding(visual, target, targetPosition, age, alpha, matrix, vertices, glow, renderLight);
        }
    }

    private static Vec3d[] buildPath(BrambleRootVisualEntity visual, Vec3d start,
                                     Vec3d end, Vec3d entityPos) {
        Vec3d[] points = new Vec3d[PATH_SEGMENTS + 1];
        Vec3d line = end.subtract(start);
        Vec3d side = horizontalPerpendicular(line);
        double seedA = visual.getVisualSeed() * 0.000137;
        double seedB = visual.getVisualSeed() * 0.000071;
        for (int i = 0; i <= PATH_SEGMENTS; i++) {
            double progress = i / (double) PATH_SEGMENTS;
            double envelope = Math.sin(progress * Math.PI);
            double meander = (Math.sin(progress * Math.PI * 3.0 + seedA) * 0.28
                    + Math.sin(progress * Math.PI * 7.0 + seedB) * 0.09) * envelope;
            Vec3d raw = start.lerp(end, progress).add(side.multiply(meander));
            Vec3d worldPoint = entityPos.add(raw);
            double ground = sampleGroundY(visual, worldPoint, worldPoint.y)
                    - entityPos.y + 0.045;
            points[i] = new Vec3d(raw.x, ground, raw.z);
        }
        return points;
    }

    private static void renderRootPath(BrambleRootVisualEntity visual, Vec3d[] points,
                                       float reveal, float age, float travelTicks, float alpha,
                                       Matrix4f matrix, VertexConsumer vertices,
                                       boolean glow, int renderLight) {
        boolean huntTrail = visual.getMode() == BrambleRootVisualEntity.MODE_HUNT;
        boolean movingTrail = visual.getMode() == BrambleRootVisualEntity.MODE_PRIMARY
                || huntTrail;
        float headProgress = reveal;
        float tailProgress = 0.0F;
        float arrivalAlpha = 1.0F;
        if (movingTrail) {
            tailProgress = Math.max(0.0F, headProgress - PRIMARY_TRAIL_LENGTH);
            if (huntTrail && age > travelTicks) {
                headProgress = 1.0F;
                tailProgress = 1.0F - PRIMARY_TRAIL_LENGTH;
                arrivalAlpha = 1.0F - MathHelper.clamp(
                        (age - travelTicks) / PRIMARY_ARRIVAL_FADE_TICKS,
                        0.0F, 1.0F);
            } else if (!huntTrail && visual.getPhase() == BrambleRootVisualEntity.PHASE_BIND) {
                headProgress = 1.0F;
                tailProgress = 1.0F - PRIMARY_TRAIL_LENGTH;
                arrivalAlpha = 1.0F - MathHelper.clamp(
                        (age - visual.getPhaseStartAge()) / PRIMARY_ARRIVAL_FADE_TICKS,
                        0.0F, 1.0F);
            } else if (!huntTrail && visual.getPhase() != BrambleRootVisualEntity.PHASE_GROW) {
                return;
            }
            if (arrivalAlpha <= 0.01F) {
                return;
            }
        }

        float pulseAge = age - visual.getPulseStartAge();
        float pulseHead = pulseAge >= 0.0F && pulseAge <= 13.0F
                ? pulseAge / 13.0F : -1.0F;

        for (int i = 0; i < PATH_SEGMENTS; i++) {
            float segmentStart = i / (float) PATH_SEGMENTS;
            float segmentEnd = (i + 1.0F) / PATH_SEGMENTS;
            if (segmentStart >= headProgress) {
                break;
            }
            if (segmentEnd <= tailProgress) {
                continue;
            }

            Vec3d from = points[i];
            Vec3d to = points[i + 1];
            float headVisibility = MathHelper.clamp(
                    (headProgress - segmentStart) * PATH_SEGMENTS, 0.0F, 1.0F);
            float clipStart = movingTrail ? MathHelper.clamp(
                    (tailProgress - segmentStart) * PATH_SEGMENTS, 0.0F, 1.0F) : 0.0F;
            float clipEnd = movingTrail ? headVisibility : 1.0F;
            if (clipEnd <= clipStart) {
                continue;
            }
            Vec3d visibleFrom = from.lerp(to, clipStart);
            Vec3d visibleTo = from.lerp(to, clipEnd);
            Vec3d direction = visibleTo.subtract(visibleFrom);
            if (direction.lengthSquared() < 0.0001) {
                continue;
            }

            float progress = MathHelper.lerp((clipStart + clipEnd) * 0.5F,
                    segmentStart, segmentEnd);
            float trailFade = 1.0F;
            if (movingTrail) {
                float fadeEnd = Math.min(headProgress,
                        tailProgress + PRIMARY_TRAIL_FADE_LENGTH);
                trailFade = smoothStep(tailProgress, fadeEnd, progress);
            }
            float pathAlpha = alpha * trailFade * arrivalAlpha;
            if (pathAlpha <= 0.01F) {
                continue;
            }
            float width = MathHelper.lerp(progress, 0.16F, 0.075F)
                    * (0.55F + headVisibility * 0.45F)
                    * (movingTrail ? 0.45F + trailFade * 0.55F : 1.0F);
            float pulse = pulseHead < 0.0F ? 0.0F
                    : MathHelper.clamp(1.0F - Math.abs(progress - pulseHead) / 0.16F, 0.0F, 1.0F);

            if (!glow) {
                drawCrossRibbon(vertices, matrix, visibleFrom, visibleTo, width,
                        42, 24, 18, alpha(235, pathAlpha), renderLight);
                drawCrossRibbon(vertices, matrix,
                        visibleFrom.add(0.0, 0.012, 0.0), visibleTo.add(0.0, 0.012, 0.0),
                        width * 0.38F,
                        58, 96, 43, alpha(215, pathAlpha), renderLight);
                boolean completeSegment = clipStart <= 0.05F && clipEnd >= 0.95F;
                if (completeSegment && trailFade >= 0.45F
                        && i > 1 && i % 3 == Math.floorMod(visual.getVisualSeed(), 3)) {
                    drawThorn(vertices, matrix, visibleFrom.lerp(visibleTo, 0.52), direction,
                            width * 2.4F, alpha(230, pathAlpha), renderLight);
                }
            } else if (pulse > 0.01F) {
                drawEmissiveCrossRibbon(vertices, matrix,
                        visibleFrom.add(0.0, 0.025, 0.0), visibleTo.add(0.0, 0.025, 0.0),
                        0.024F + pulse * 0.035F,
                        142, 255, 104, alpha((int) (225 * pulse), pathAlpha), FULL_LIGHT);
            }
        }
    }

    private static void renderBinding(BrambleRootVisualEntity visual, LivingEntity target,
                                      Vec3d targetPosition, float age, float alpha,
                                      Matrix4f matrix, VertexConsumer vertices,
                                      boolean glow, int renderLight) {
        float phaseAge = Math.max(0.0F, age - visual.getPhaseStartAge());
        float tighten = visual.getPhase() == BrambleRootVisualEntity.PHASE_LIFT
                ? MathHelper.clamp(phaseAge / 6.0F, 0.0F, 1.0F)
                : visual.getPhase() == BrambleRootVisualEntity.PHASE_SLAM ? 1.0F : 0.0F;
        float height = Math.max(0.75F, target.getHeight() * 0.78F);
        float radius = Math.max(0.34F, target.getWidth() * (0.66F - tighten * 0.13F));
        double seed = visual.getVisualSeed() * 0.00091;
        Vec3d previous = null;
        float pulseAge = age - visual.getPulseStartAge();
        for (int i = 0; i <= SPIRAL_SEGMENTS; i++) {
            float progress = i / (float) SPIRAL_SEGMENTS;
            double angle = progress * Math.PI * 5.2 + seed + age * 0.025;
            float breathing = 1.0F + 0.07F * MathHelper.sin(age * 0.16F + i * 0.7F);
            Vec3d point = targetPosition.add(
                    Math.cos(angle) * radius * breathing,
                    0.06 + progress * height,
                    Math.sin(angle) * radius * breathing);
            if (previous != null) {
                float pulse = pulseAge < 0.0F || pulseAge > 13.0F ? 0.0F
                        : MathHelper.clamp(1.0F - Math.abs(progress - pulseAge / 13.0F) / 0.22F,
                        0.0F, 1.0F);
                if (!glow) {
                    drawCrossRibbon(vertices, matrix, previous, point, 0.075F,
                            48, 78, 34, alpha(235, alpha), renderLight);
                    if (i % 5 == 2) {
                        drawThorn(vertices, matrix, previous.lerp(point, 0.5),
                                point.subtract(previous), 0.17F, alpha(225, alpha), renderLight);
                    }
                } else if (pulse > 0.01F) {
                    drawEmissiveCrossRibbon(vertices, matrix, previous, point,
                            0.025F + pulse * 0.025F,
                            158, 255, 112, alpha((int) (220 * pulse), alpha), FULL_LIGHT);
                }
            }
            previous = point;
        }
    }

    private static void renderCore(BrambleRootVisualEntity visual, float age, float alpha,
                                   Matrix4f matrix, VertexConsumer vertices, boolean glow,
                                   int renderLight) {
        float grow = visual.getPhase() == BrambleRootVisualEntity.PHASE_GROW
                ? easeOut(MathHelper.clamp(age / Math.max(1.0F,
                Config.uniqueEffects.bramblethorn.rootTravelTicks), 0.0F, 1.0F))
                : 1.0F;
        float phaseAge = Math.max(0.0F, age - visual.getPhaseStartAge());
        float slam = visual.getPhase() == BrambleRootVisualEntity.PHASE_SLAM
                ? MathHelper.clamp(phaseAge / 8.0F, 0.0F, 1.0F) : 0.0F;
        float baseRadius = 0.65F + grow * 0.95F;
        if (slam > 0.0F) {
            baseRadius += MathHelper.sin(slam * MathHelper.PI) * 2.1F;
        }
        float pulseAge = age - visual.getPulseStartAge();
        float pulse = pulseAge >= 0.0F && pulseAge <= 13.0F
                ? MathHelper.sin(pulseAge / 13.0F * MathHelper.PI) : 0.0F;

        Vec3d previous = null;
        for (int i = 0; i <= CORE_SEGMENTS; i++) {
            double angle = MathHelper.TAU * i / CORE_SEGMENTS;
            float wobble = 1.0F + 0.10F * MathHelper.sin(i * 2.17F + visual.getVisualSeed() * 0.001F);
            Vec3d point = new Vec3d(Math.cos(angle) * baseRadius * wobble,
                    0.055 + 0.035 * Math.sin(angle * 3.0 + age * 0.06),
                    Math.sin(angle) * baseRadius * wobble);
            if (previous != null) {
                if (!glow) {
                    drawCrossRibbon(vertices, matrix, previous, point, 0.12F,
                            45, 25, 20, alpha(235, alpha), renderLight);
                    drawCrossRibbon(vertices, matrix,
                            previous.add(0.0, 0.015, 0.0), point.add(0.0, 0.015, 0.0),
                            0.042F, 71, 105, 48, alpha(220, alpha), renderLight);
                    if (i % 4 == 1) {
                        drawThorn(vertices, matrix, previous.lerp(point, 0.5),
                                point.subtract(previous), 0.22F + slam * 0.12F,
                                alpha(235, alpha), renderLight);
                    }
                } else if (pulse > 0.01F || slam > 0.0F) {
                    float intensity = Math.max(pulse, MathHelper.sin(slam * MathHelper.PI));
                    drawEmissiveCrossRibbon(vertices, matrix, previous, point,
                            0.025F + intensity * 0.025F,
                            150, 255, 104, alpha((int) (205 * intensity), alpha), FULL_LIGHT);
                }
            }
            previous = point;
        }

        if (!glow) {
            renderCoreRoots(visual, grow, slam, baseRadius, alpha,
                    matrix, vertices, renderLight);
        }
    }

    private static void renderCoreRoots(BrambleRootVisualEntity visual, float grow,
                                        float slam, float baseRadius, float alpha,
                                        Matrix4f matrix, VertexConsumer vertices,
                                        int renderLight) {
        float visibleSegments = grow * CORE_ROOT_SEGMENTS;
        int seed = visual.getVisualSeed();

        for (int root = 0; root < CORE_ROOT_COUNT; root++) {
            double baseAngle = MathHelper.TAU * root / CORE_ROOT_COUNT
                    + seed * 0.0003
                    + (visualNoise(seed, root, 0) - 0.5F) * 0.26F;
            double bend = (0.18 + visualNoise(seed, root, 1) * 0.26)
                    * (visualNoise(seed, root, 2) < 0.5F ? -1.0 : 1.0);
            double reach = baseRadius * (0.74 + visualNoise(seed, root, 3) * 0.16);
            double startRadius = 0.10 + visualNoise(seed, root, 4) * 0.09;
            double wavePhase = visualNoise(seed, root, 5) * MathHelper.TAU;
            float widthScale = 0.88F + visualNoise(seed, root, 6) * 0.24F;
            int firstThorn = 2 + (int) (visualNoise(seed, root, 7) * 2.0F);
            int secondThorn = 4 + (int) (visualNoise(seed, root, 8) * 2.0F);
            boolean hasSecondThorn = visualNoise(seed, root, 9) > 0.35F;

            Vec3d from = coreRootPoint(0, startRadius, reach, baseAngle, bend, wavePhase);
            for (int segment = 0; segment < CORE_ROOT_SEGMENTS; segment++) {
                Vec3d to = coreRootPoint(segment + 1, startRadius, reach,
                        baseAngle, bend, wavePhase);
                float segmentVisibility = MathHelper.clamp(visibleSegments - segment,
                        0.0F, 1.0F);
                if (segmentVisibility <= 0.0F) {
                    break;
                }

                Vec3d visibleEnd = from.lerp(to, segmentVisibility);
                float progress = (segment + 0.5F) / CORE_ROOT_SEGMENTS;
                float width = MathHelper.lerp(progress, 0.115F, 0.055F)
                        * widthScale * (0.58F + segmentVisibility * 0.42F);
                drawCrossRibbon(vertices, matrix, from, visibleEnd, width,
                        45, 25, 20, alpha(230, alpha), renderLight);
                drawCrossRibbon(vertices, matrix,
                        from.add(0.0, 0.014, 0.0), visibleEnd.add(0.0, 0.014, 0.0),
                        width * 0.36F, 71, 105, 48, alpha(215, alpha), renderLight);

                boolean thornSegment = segment == firstThorn
                        || hasSecondThorn && segment == secondThorn;
                if (thornSegment && segmentVisibility >= 0.92F) {
                    float thornHeight = 0.14F + visualNoise(seed, root, 10 + segment) * 0.08F
                            + slam * 0.04F;
                    drawThorn(vertices, matrix, from.lerp(visibleEnd, 0.58),
                            visibleEnd.subtract(from), thornHeight,
                            alpha(230, alpha), renderLight);
                }
                from = to;
            }
        }
    }

    private static Vec3d coreRootPoint(int point, double startRadius, double reach,
                                       double baseAngle, double bend, double wavePhase) {
        double progress = point / (double) CORE_ROOT_SEGMENTS;
        double envelope = Math.sin(progress * Math.PI);
        double radius = MathHelper.lerp(progress, startRadius, reach);
        double angle = baseAngle
                + bend * envelope
                + Math.sin(progress * MathHelper.TAU + wavePhase) * envelope * 0.055;
        double height = 0.055
                + Math.sin(progress * Math.PI * 3.0 + wavePhase) * envelope * 0.018;
        return new Vec3d(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    private static float visualNoise(int seed, int index, int channel) {
        int hash = seed;
        hash = 31 * hash + index;
        hash = 31 * hash + channel;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;
        return (hash & 0x00FFFFFF) / 16777216.0F;
    }

    private static void drawThorn(VertexConsumer vertices, Matrix4f matrix, Vec3d base,
                                  Vec3d pathDirection, float height, int alpha, int light) {
        Vec3d side = horizontalPerpendicular(pathDirection);
        Vec3d tip = base.add(side.multiply(height * 0.45)).add(0.0, height, 0.0);
        Vec3d width = side.multiply(height * 0.19);
        quad(vertices, matrix,
                base.add(width), base.subtract(width), tip, tip,
                104, 37, 55, alpha, light);
        Vec3d cross = new Vec3d(-side.z, 0.0, side.x).multiply(height * 0.16);
        quad(vertices, matrix,
                base.add(cross), base.subtract(cross), tip, tip,
                77, 28, 42, alpha, light);
    }

    private static void drawCrossRibbon(VertexConsumer vertices, Matrix4f matrix,
                                        Vec3d start, Vec3d end, float halfWidth,
                                        int red, int green, int blue, int alpha,
                                        int light) {
        drawCrossRibbon(vertices, matrix, start, end, halfWidth,
                red, green, blue, alpha, light, false);
    }

    private static void drawEmissiveCrossRibbon(VertexConsumer vertices, Matrix4f matrix,
                                                Vec3d start, Vec3d end, float halfWidth,
                                                int red, int green, int blue, int alpha,
                                                int light) {
        drawCrossRibbon(vertices, matrix, start, end, halfWidth,
                red, green, blue, alpha, light, true);
    }

    private static void drawCrossRibbon(VertexConsumer vertices, Matrix4f matrix,
                                        Vec3d start, Vec3d end, float halfWidth,
                                        int red, int green, int blue, int alpha,
                                        int light, boolean emissive) {
        if (alpha <= 0) {
            return;
        }
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        Vec3d side = horizontalPerpendicular(direction).multiply(halfWidth);
        Vec3d up = direction.normalize().crossProduct(side.normalize()).normalize().multiply(halfWidth);
        drawRibbon(vertices, matrix, start, end, side, red, green, blue, alpha,
                light, emissive);
        drawRibbon(vertices, matrix, start, end, up, red, green, blue, alpha,
                light, emissive);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix,
                                   Vec3d start, Vec3d end, Vec3d width,
                                   int red, int green, int blue, int alpha, int light,
                                   boolean emissive) {
        Vec3d first = start.add(width);
        Vec3d second = start.subtract(width);
        Vec3d third = end.subtract(width);
        Vec3d fourth = end.add(width);
        if (emissive) {
            emissiveQuad(vertices, matrix, first, second, third, fourth,
                    red, green, blue, alpha, light);
        } else {
            quad(vertices, matrix, first, second, third, fourth,
                    red, green, blue, alpha, light);
        }
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             Vec3d first, Vec3d second, Vec3d third, Vec3d fourth,
                             int red, int green, int blue, int alpha, int light) {
        vertex(vertices, matrix, first, red, green, blue, alpha, light);
        vertex(vertices, matrix, second, red, green, blue, alpha, light);
        vertex(vertices, matrix, third, red, green, blue, alpha, light);
        vertex(vertices, matrix, fourth, red, green, blue, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                               int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha).light(light).next();
    }

    private static void emissiveQuad(VertexConsumer vertices, Matrix4f matrix,
                                     Vec3d first, Vec3d second, Vec3d third, Vec3d fourth,
                                     int red, int green, int blue, int alpha, int light) {
        Vec3d normal = second.subtract(first).crossProduct(fourth.subtract(first));
        if (normal.lengthSquared() < 0.000001) {
            normal = new Vec3d(0.0, 1.0, 0.0);
        } else {
            normal = normal.normalize();
        }
        emissiveVertex(vertices, matrix, first, 0.0F, 0.0F,
                red, green, blue, alpha, light, normal);
        emissiveVertex(vertices, matrix, second, 0.0F, 1.0F,
                red, green, blue, alpha, light, normal);
        emissiveVertex(vertices, matrix, third, 1.0F, 1.0F,
                red, green, blue, alpha, light, normal);
        emissiveVertex(vertices, matrix, fourth, 1.0F, 0.0F,
                red, green, blue, alpha, light, normal);
    }

    private static void emissiveVertex(VertexConsumer vertices, Matrix4f matrix,
                                       Vec3d position, float u, float v,
                                       int red, int green, int blue, int alpha, int light,
                                       Vec3d normal) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).next();
    }

    private static Vec3d horizontalPerpendicular(Vec3d direction) {
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x);
        return side.lengthSquared() < 0.0001 ? new Vec3d(1.0, 0.0, 0.0) : side.normalize();
    }

    private static double sampleGroundY(BrambleRootVisualEntity visual, Vec3d worldPosition,
                                        double fallbackY) {
        BlockPos origin = BlockPos.ofFloored(worldPosition.x, fallbackY, worldPosition.z);
        for (int offset = 3; offset >= -5; offset--) {
            BlockPos pos = origin.up(offset);
            BlockState state = visual.getWorld().getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(visual.getWorld(), pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.getMax(Direction.Axis.Y);
            }
        }
        return worldPosition.y;
    }

    private static Vec3d lerpedPosition(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
    }

    private static float phaseAlpha(BrambleRootVisualEntity visual, float age) {
        if (visual.getMode() == BrambleRootVisualEntity.MODE_HUNT) {
            return 1.0F;
        }
        if (visual.getPhase() == BrambleRootVisualEntity.PHASE_FADE) {
            return MathHelper.clamp(1.0F - (age - visual.getPhaseStartAge()) / 12.0F,
                    0.0F, 1.0F);
        }
        return MathHelper.clamp((visual.getEndAge() - age) / 8.0F, 0.0F, 1.0F);
    }

    private static int alpha(int base, float multiplier) {
        return MathHelper.clamp(Math.round(base * multiplier), 0, 255);
    }

    private static float easeOut(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float smoothStep(float start, float end, float value) {
        if (end <= start) {
            return value >= end ? 1.0F : 0.0F;
        }
        float progress = MathHelper.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return progress * progress * (3.0F - 2.0F * progress);
    }
}
