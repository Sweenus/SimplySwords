package net.sweenus.simplyswords.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.sweenus.simplyswords.api.ObserverStatusEffectSnapshot;
import net.sweenus.simplyswords.api.ObserverStatusVisualRegistry;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualShape;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualStyle;
import net.sweenus.simplyswords.client.api.ObserverStatusEffectClientApi;
import net.sweenus.simplyswords.client.render.BoltPath;
import net.sweenus.simplyswords.client.render.LightningRenderLayers;
import net.sweenus.simplyswords.client.render.MinecraftLightningRenderer;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.client.util.ParchmentRendering;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class ObserverStatusVisualRenderer {

    private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
    private static final Identifier WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int GROUND_RING_SEGMENTS = 48;
    private static final int WOUND_SEGMENTS = 8;
    private static final int WOUND_VOID = 0x07020C;
    private static final int GLOAM_TENDRIL_SLOTS = 4;
    private static final float GLOAM_TENDRIL_EMERGE_TICKS = 8.0F;
    private static final float GLOAM_TENDRIL_SNAP_TICKS = 6.0F;
    private static final float GLOAM_TENDRIL_RETRACT_TICKS = 6.0F;
    private static final double GLOAM_TENDRIL_SNAP_DISTANCE_SQUARED = 16.0;
    private static final Map<UUID, GloamTendrilState> GLOAM_TENDRIL_STATES = new HashMap<>();
    private static ClientWorld gloamWorld;
    private static int gloamAccentsThisFrame;

    private ObserverStatusVisualRenderer() {
    }

    public static void beginWorldFrame() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != gloamWorld) {
            GLOAM_TENDRIL_STATES.clear();
            gloamWorld = client.world;
        }
        long now = Util.getMeasuringTimeMs();
        GLOAM_TENDRIL_STATES.entrySet().removeIf(
                entry -> now - entry.getValue().lastSeenTime > 2000L);
        gloamAccentsThisFrame = 0;
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
                case CORRUPTED_WOUNDS -> renderCorruptedWounds(entity, tickDelta, style, matrices, consumers);
                case GLOAM_EXPOSURE -> renderGloamExposure(
                        entity, tickDelta, style, stacks, matrices, consumers);
                case GLOAM_GRASP -> renderGloamGrasp(
                        entity, tickDelta, style, matrices, consumers);
                default -> renderStatic(entity, tickDelta, style, matrices, consumers);
            }
        }
    }

    private static void renderCorruptedWounds(LivingEntity entity, float tickDelta,
                                               ObserverStatusVisualStyle style,
                                               MatrixStack matrices,
                                               VertexConsumerProvider consumers) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        float height = Math.max(0.7F, entity.getHeight());
        float radius = Math.max(0.31F, entity.getWidth() * 0.54F) + 0.035F;
        int count = Math.min(5, Math.max(1, style.density()));
        long seed = entity.getUuid().getLeastSignificantBits();
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw)
                * MathHelper.RADIANS_PER_DEGREE;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        for (int i = 0; i < count; i++) {
            drawWoundBase(dark, matrix, seed, i, count, height, radius, bodyYaw,
                    style.primaryColor());
        }

        VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        for (int i = 0; i < count; i++) {
            drawWoundRim(glow, matrix, seed, i, count, height, radius, bodyYaw,
                    style.primaryColor());
        }
        for (int i = 0; i < count; i++) {
            drawWoundEnergy(glow, matrix, age, seed, i, count, height, radius, bodyYaw,
                    style.primaryColor(), style.coreColor());
        }
        drawWoundMotes(glow, matrix, age, seed, count, height, radius, bodyYaw, style.coreColor());
    }

    private static void drawWoundBase(VertexConsumer vertices, Matrix4f matrix,
                                      long seed, int index, int count, float height,
                                      float radius, float bodyYaw, int primaryColor) {
        float angle = woundAngle(seed, index, count, bodyYaw);
        Vec3d outward = new Vec3d(MathHelper.sin(angle), 0.0, MathHelper.cos(angle));
        Vec3d tangent = new Vec3d(outward.z, 0.0, -outward.x);
        float tilt = woundTilt(seed, index);
        Vec3d longAxis = tangent.multiply(MathHelper.cos(tilt)).add(UP.multiply(MathHelper.sin(tilt)));
        Vec3d shortAxis = UP.multiply(MathHelper.cos(tilt)).subtract(tangent.multiply(MathHelper.sin(tilt)));
        float halfLength = woundHalfLength(seed, index, height);
        float halfWidth = woundHalfWidth(seed, index, halfLength);
        Vec3d center = woundCenter(seed, index, height, radius, outward);
        fillWound(vertices, matrix, seed, index, center, outward, longAxis, shortAxis,
                halfLength, halfWidth, 1.22F, 0.0F, primaryColor, 88);
        fillWound(vertices, matrix, seed, index, center, outward, longAxis, shortAxis,
                halfLength, halfWidth, 0.69F, 0.003F, WOUND_VOID, 238);
    }

    private static void drawWoundRim(VertexConsumer vertices, Matrix4f matrix,
                                     long seed, int index, int count, float height,
                                     float radius, float bodyYaw, int primaryColor) {
        float angle = woundAngle(seed, index, count, bodyYaw);
        Vec3d outward = new Vec3d(MathHelper.sin(angle), 0.0, MathHelper.cos(angle));
        Vec3d tangent = new Vec3d(outward.z, 0.0, -outward.x);
        float tilt = woundTilt(seed, index);
        Vec3d longAxis = tangent.multiply(MathHelper.cos(tilt)).add(UP.multiply(MathHelper.sin(tilt)));
        Vec3d shortAxis = UP.multiply(MathHelper.cos(tilt)).subtract(tangent.multiply(MathHelper.sin(tilt)));
        float halfLength = woundHalfLength(seed, index, height);
        float halfWidth = woundHalfWidth(seed, index, halfLength);
        Vec3d center = woundCenter(seed, index, height, radius, outward);
        int brightPurple = scaleColor(primaryColor, 1.62F);
        for (int segment = 0; segment < WOUND_SEGMENTS; segment++) {
            int next = (segment + 1) % WOUND_SEGMENTS;
            Vec3d outerA = woundPoint(seed, index, segment, center, outward, longAxis, shortAxis,
                    halfLength, halfWidth, 0.98F, 0.011F);
            Vec3d outerB = woundPoint(seed, index, next, center, outward, longAxis, shortAxis,
                    halfLength, halfWidth, 0.98F, 0.011F);
            Vec3d innerB = woundPoint(seed, index, next, center, outward, longAxis, shortAxis,
                    halfLength, halfWidth, 0.66F, 0.014F);
            Vec3d innerA = woundPoint(seed, index, segment, center, outward, longAxis, shortAxis,
                    halfLength, halfWidth, 0.66F, 0.014F);
            int color = (segment & 1) == 0 ? primaryColor : brightPurple;
            int alpha = (segment & 1) == 0 ? 198 : 226;
            woundGlowDoubleQuad(vertices, matrix, outerA, outerB, innerB, innerA, outward, color, alpha);
        }
    }

    private static void drawWoundEnergy(VertexConsumer vertices, Matrix4f matrix, float age,
                                        long seed, int index, int count, float height,
                                        float radius, float bodyYaw, int primaryColor, int coreColor) {
        float angle = woundAngle(seed, index, count, bodyYaw);
        Vec3d outward = new Vec3d(MathHelper.sin(angle), 0.0, MathHelper.cos(angle));
        Vec3d tangent = new Vec3d(outward.z, 0.0, -outward.x);
        float tilt = woundTilt(seed, index);
        Vec3d longAxis = tangent.multiply(MathHelper.cos(tilt)).add(UP.multiply(MathHelper.sin(tilt)));
        Vec3d shortAxis = UP.multiply(MathHelper.cos(tilt)).subtract(tangent.multiply(MathHelper.sin(tilt)));
        float halfLength = woundHalfLength(seed, index, height);
        float halfWidth = woundHalfWidth(seed, index, halfLength);
        Vec3d center = woundCenter(seed, index, height, radius, outward).add(outward.multiply(0.018F));
        float pulse = 0.86F + MathHelper.sin(age * 0.16F + index * 1.73F) * 0.12F;
        int purple = scaleColor(primaryColor, 1.9F);
        int paleCore = mixColor(coreColor, 0xFFFFFF, 0.34F);
        for (int segment = 0; segment < 3; segment++) {
            Vec3d start = woundCorePoint(seed, index, segment, center, longAxis, shortAxis,
                    halfLength, halfWidth);
            Vec3d end = woundCorePoint(seed, index, segment + 1, center, longAxis, shortAxis,
                    halfLength, halfWidth);
            float taper = 1.0F - Math.abs(segment - 1) * 0.14F;
            woundGlowRibbon(vertices, matrix, start, end, shortAxis,
                    halfWidth * 0.21F * taper, outward, purple, Math.round(184.0F * pulse));
            woundGlowRibbon(vertices, matrix, start.add(outward.multiply(0.002F)),
                    end.add(outward.multiply(0.002F)), shortAxis,
                    halfWidth * 0.085F * taper, outward,
                    segment == 1 ? paleCore : coreColor, Math.round(242.0F * pulse));
        }
        for (int branch = 0; branch < 2; branch++) {
            int sample = branch + 1;
            Vec3d start = woundCorePoint(seed, index, sample, center, longAxis, shortAxis,
                    halfLength, halfWidth);
            float direction = branch == 0 ? -1.0F : 1.0F;
            Vec3d end = start.add(shortAxis.multiply(direction * halfWidth * 0.6F))
                    .add(longAxis.multiply((BoltPath.unit(seed + index * 181L, branch, 67L) - 0.5F)
                            * halfLength * 0.32F));
            woundGlowRibbon(vertices, matrix, start, end, longAxis,
                    Math.max(0.005F, halfWidth * 0.035F), outward,
                    branch == 0 ? purple : coreColor, Math.round(174.0F * pulse));
        }
    }

    private static void drawWoundMotes(VertexConsumer vertices, Matrix4f matrix, float age,
                                       long seed, int count, float height, float radius,
                                       float bodyYaw, int coreColor) {
        int moteCount = Math.min(3, count);
        for (int mote = 0; mote < moteCount; mote++) {
            int wound = Math.floorMod(mote * 2 + 1, count);
            float angle = woundAngle(seed, wound, count, bodyYaw);
            Vec3d outward = new Vec3d(MathHelper.sin(angle), 0.0, MathHelper.cos(angle));
            Vec3d tangent = new Vec3d(outward.z, 0.0, -outward.x);
            float phase = fractional((float) BoltPath.unit(seed + mote * 313L, 0L, 79L) + age * 0.025F);
            Vec3d center = woundCenter(seed, wound, height, radius + 0.07F, outward)
                    .add(tangent.multiply(MathHelper.sin(phase * MathHelper.TAU + mote) * 0.06F))
                    .add(0.0, phase * 0.27F, 0.0);
            float fade = MathHelper.sin(phase * MathHelper.PI);
            float size = 0.018F + (float) BoltPath.unit(seed, mote, 83L) * 0.018F;
            Vec3d top = center.add(0.0, size * 1.35F, 0.0);
            Vec3d right = center.add(tangent.multiply(size));
            Vec3d bottom = center.add(0.0, -size * 1.35F, 0.0);
            Vec3d left = center.subtract(tangent.multiply(size));
            woundGlowDoubleQuad(vertices, matrix, top, right, bottom, left, outward,
                    coreColor, MathHelper.clamp(Math.round(178.0F * fade), 0, 178));
        }
    }

    private static void fillWound(VertexConsumer vertices, Matrix4f matrix,
                                  long seed, int index, Vec3d center, Vec3d outward,
                                  Vec3d longAxis, Vec3d shortAxis, float halfLength,
                                  float halfWidth, float scale, float normalOffset,
                                  int color, int alpha) {
        Vec3d middle = center.add(outward.multiply(normalOffset));
        for (int segment = 0; segment < WOUND_SEGMENTS; segment++) {
            int next = (segment + 1) % WOUND_SEGMENTS;
            Vec3d a = woundPoint(seed, index, segment, center, outward, longAxis, shortAxis,
                    halfLength, halfWidth, scale, normalOffset);
            Vec3d b = woundPoint(seed, index, next, center, outward, longAxis, shortAxis,
                    halfLength, halfWidth, scale, normalOffset);
            woundColorQuad(vertices, matrix, middle, a, b, middle, color, alpha);
        }
    }

    private static Vec3d woundPoint(long seed, int index, int segment,
                                    Vec3d center, Vec3d outward, Vec3d longAxis, Vec3d shortAxis,
                                    float halfLength, float halfWidth, float scale, float normalOffset) {
        float angle = MathHelper.TAU * segment / WOUND_SEGMENTS;
        float jitter = 0.82F + (float) BoltPath.unit(seed + index * 977L, segment, 41L) * 0.31F;
        return center.add(longAxis.multiply(MathHelper.cos(angle) * halfLength * scale * jitter))
                .add(shortAxis.multiply(MathHelper.sin(angle) * halfWidth * scale * jitter))
                .add(outward.multiply(normalOffset));
    }

    private static Vec3d woundCorePoint(long seed, int index, int sample,
                                        Vec3d center, Vec3d longAxis, Vec3d shortAxis,
                                        float halfLength, float halfWidth) {
        float progress = sample / 3.0F;
        float along = MathHelper.lerp(progress, -0.54F, 0.54F) * halfLength;
        float crooked = ((float) BoltPath.unit(seed + index * 1237L, sample, 53L) - 0.5F)
                * halfWidth * (sample == 0 || sample == 3 ? 0.24F : 0.58F);
        return center.add(longAxis.multiply(along)).add(shortAxis.multiply(crooked));
    }

    private static Vec3d woundCenter(long seed, int index, float height, float radius, Vec3d outward) {
        float centerY = height * (0.2F + (float) BoltPath.unit(seed, index, 7L) * 0.58F);
        return outward.multiply(radius).add(0.0, centerY, 0.0);
    }

    private static float woundAngle(long seed, int index, int count, float bodyYaw) {
        return (float) (MathHelper.TAU * index / count
                + BoltPath.unit(seed + index * 7919L, 0L, 4L) * 0.58F - bodyYaw);
    }

    private static float woundTilt(long seed, int index) {
        float magnitude = MathHelper.lerp((float) BoltPath.unit(seed, index, 13L), 0.38F, 0.78F);
        return (index & 1) == 0 ? magnitude : -magnitude;
    }

    private static float woundHalfLength(long seed, int index, float height) {
        return MathHelper.clamp(height * (0.105F + (float) BoltPath.unit(seed, index, 11L) * 0.058F),
                0.16F, 0.58F);
    }

    private static float woundHalfWidth(long seed, int index, float halfLength) {
        return MathHelper.clamp(halfLength * (0.34F + (float) BoltPath.unit(seed, index, 17L) * 0.15F),
                0.065F, 0.25F);
    }

    private static void woundGlowRibbon(VertexConsumer vertices, Matrix4f matrix,
                                        Vec3d start, Vec3d end, Vec3d widthAxis, float halfWidth,
                                        Vec3d normal, int color, int alpha) {
        Vec3d offset = widthAxis.normalize().multiply(halfWidth);
        woundGlowDoubleQuad(vertices, matrix,
                start.add(offset), start.subtract(offset), end.subtract(offset), end.add(offset),
                normal, color, MathHelper.clamp(alpha, 0, 255));
    }

    private static void woundGlowDoubleQuad(VertexConsumer vertices, Matrix4f matrix,
                                            Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                            Vec3d normal, int color, int alpha) {
        woundGlowQuad(vertices, matrix, a, b, c, d, normal, color, alpha);
        woundGlowQuad(vertices, matrix, d, c, b, a, normal.multiply(-1.0), color, alpha);
    }

    private static void woundColorQuad(VertexConsumer vertices, Matrix4f matrix,
                                       Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                       int color, int alpha) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        vertices.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(red, green, blue, alpha);
        vertices.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(red, green, blue, alpha);
    }

    private static void woundGlowQuad(VertexConsumer vertices, Matrix4f matrix,
                                      Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                      Vec3d normal, int color, int alpha) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        woundGlowVertex(vertices, matrix, a, normal, red, green, blue, alpha);
        woundGlowVertex(vertices, matrix, b, normal, red, green, blue, alpha);
        woundGlowVertex(vertices, matrix, c, normal, red, green, blue, alpha);
        woundGlowVertex(vertices, matrix, d, normal, red, green, blue, alpha);
    }

    private static void woundGlowVertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                                        Vec3d normal, int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static int scaleColor(int color, float scale) {
        int red = MathHelper.clamp(Math.round((color >> 16 & 0xFF) * scale), 0, 255);
        int green = MathHelper.clamp(Math.round((color >> 8 & 0xFF) * scale), 0, 255);
        int blue = MathHelper.clamp(Math.round((color & 0xFF) * scale), 0, 255);
        return red << 16 | green << 8 | blue;
    }

    private static int mixColor(int first, int second, float progress) {
        int red = Math.round(MathHelper.lerp(progress, first >> 16 & 0xFF, second >> 16 & 0xFF));
        int green = Math.round(MathHelper.lerp(progress, first >> 8 & 0xFF, second >> 8 & 0xFF));
        int blue = Math.round(MathHelper.lerp(progress, first & 0xFF, second & 0xFF));
        return red << 16 | green << 8 | blue;
    }

    private static float fractional(float value) {
        return value - MathHelper.floor(value);
    }

    private static void renderGloamExposure(LivingEntity entity, float tickDelta,
                                             ObserverStatusVisualStyle style, int stacks,
                                             MatrixStack matrices,
                                             VertexConsumerProvider consumers) {
        renderGloamTendrils(entity, tickDelta, style,
                Math.clamp(stacks, 1, 3), matrices, consumers);
    }

    private static void renderGloamGrasp(LivingEntity entity, float tickDelta,
                                          ObserverStatusVisualStyle style,
                                          MatrixStack matrices,
                                          VertexConsumerProvider consumers) {
        renderGloamTendrils(entity, tickDelta, style, 4, matrices, consumers);
    }

    private static void renderGloamTendrils(LivingEntity entity, float tickDelta,
                                             ObserverStatusVisualStyle style, int stage,
                                             MatrixStack matrices,
                                             VertexConsumerProvider consumers) {
        if (!canRenderGloamAccent(entity)) {
            return;
        }
        float age = entity.age + tickDelta;
        Vec3d targetPosition = lerpedEntityPosition(entity, tickDelta);
        GloamTendrilState state = updateGloamTendrilState(
                entity, stage, age, targetPosition, style.scale());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getDebugQuads());
        int color = style.primaryColor();
        int alpha = 138;
        for (int index = 0;
             index < GLOAM_TENDRIL_SLOTS && gloamAccentsThisFrame < 24;
             index++) {
            AnchoredGloamTendril tendril = state.tendrils[index];
            if (tendril == null) {
                continue;
            }
            renderAnchoredGloamTendril(vertices, matrix, entity, tendril,
                    index, targetPosition, age, color, alpha);
            gloamAccentsThisFrame++;
        }
    }

    private static GloamTendrilState updateGloamTendrilState(
            LivingEntity entity, int stage, float age,
            Vec3d targetPosition, float scale) {
        long now = Util.getMeasuringTimeMs();
        GloamTendrilState state = GLOAM_TENDRIL_STATES.get(entity.getUuid());
        if (state == null) {
            state = new GloamTendrilState(now);
            GLOAM_TENDRIL_STATES.put(entity.getUuid(), state);
        }
        state.lastSeenTime = now;
        for (int index = 0; index < GLOAM_TENDRIL_SLOTS; index++) {
            boolean desired = index < stage;
            AnchoredGloamTendril tendril = state.tendrils[index];
            if (tendril == null) {
                if (desired) {
                    state.tendrils[index] = createAnchoredGloamTendril(
                            entity, state, index, targetPosition, age, scale);
                }
                continue;
            }
            Vec3d attachment = gloamTendrilAttachment(
                    entity, targetPosition, tendril.anchor, index);
            float phaseAge = age - tendril.phaseStart;
            if (tendril.phase == GloamTendrilPhase.SNAPPING) {
                if (phaseAge >= GLOAM_TENDRIL_SNAP_TICKS) {
                    state.tendrils[index] = desired
                            ? createAnchoredGloamTendril(
                            entity, state, index, targetPosition, age, scale)
                            : null;
                }
                continue;
            }
            if (tendril.phase == GloamTendrilPhase.RETRACTING) {
                if (desired) {
                    state.tendrils[index] = createAnchoredGloamTendril(
                            entity, state, index, targetPosition, age, scale);
                } else if (phaseAge >= GLOAM_TENDRIL_RETRACT_TICKS) {
                    state.tendrils[index] = null;
                }
                continue;
            }
            if (!desired) {
                tendril.phase = GloamTendrilPhase.RETRACTING;
                tendril.phaseStart = age;
                tendril.phaseAttachment = attachment;
                continue;
            }
            if (tendril.phase == GloamTendrilPhase.EMERGING
                    && phaseAge >= GLOAM_TENDRIL_EMERGE_TICKS) {
                tendril.phase = GloamTendrilPhase.ATTACHED;
                tendril.phaseStart = age;
            }
            if (tendril.anchor.squaredDistanceTo(attachment)
                    > GLOAM_TENDRIL_SNAP_DISTANCE_SQUARED) {
                tendril.phase = GloamTendrilPhase.SNAPPING;
                tendril.phaseStart = age;
                tendril.phaseAttachment = attachment;
                tendril.breakPoint = tendril.anchor.lerp(attachment, 0.56);
            }
        }
        return state;
    }

    private static AnchoredGloamTendril createAnchoredGloamTendril(
            LivingEntity entity, GloamTendrilState state, int index,
            Vec3d targetPosition, float age, float scale) {
        int generation = state.generations[index]++;
        long seed = entity.getUuid().getLeastSignificantBits()
                + index * 65537L + generation * 104729L;
        float baseRotation = (float) (BoltPath.unit(
                entity.getUuid().getLeastSignificantBits(), 0L, 317L) * MathHelper.TAU);
        float angle = baseRotation + index * MathHelper.TAU / GLOAM_TENDRIL_SLOTS
                + ((float) BoltPath.unit(seed, generation, 331L) - 0.5F) * 0.34F;
        double radius = Math.max(0.5, entity.getWidth() * 0.82) * scale;
        Vec3d candidate = targetPosition.add(
                Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        Vec3d anchor = new Vec3d(candidate.x,
                sampleGloamGroundY(entity, candidate, targetPosition.y) + 0.07,
                candidate.z);
        return new AnchoredGloamTendril(
                anchor, seed, GloamTendrilPhase.EMERGING, age);
    }

    private static void renderAnchoredGloamTendril(
            VertexConsumer vertices, Matrix4f matrix, LivingEntity entity,
            AnchoredGloamTendril tendril, int index,
            Vec3d targetPosition, float age, int color, int alpha) {
        Vec3d anchor = tendril.anchor.subtract(targetPosition);
        Vec3d attachmentWorld = gloamTendrilAttachment(
                entity, targetPosition, tendril.anchor, index);
        Vec3d attachment = attachmentWorld.subtract(targetPosition);
        float phaseAge = age - tendril.phaseStart;
        switch (tendril.phase) {
            case EMERGING -> {
                float reveal = gloamEase(MathHelper.clamp(
                        phaseAge / GLOAM_TENDRIL_EMERGE_TICKS, 0.0F, 1.0F));
                drawGloamTendril(vertices, matrix, anchor, attachment,
                        age, tendril.seed, reveal, color, alpha);
            }
            case ATTACHED -> drawGloamTendril(vertices, matrix, anchor, attachment,
                    age, tendril.seed, 1.0F, color, alpha);
            case SNAPPING -> drawSnappingGloamTendril(
                    vertices, matrix, tendril, targetPosition,
                    attachment, age, color, alpha);
            case RETRACTING -> {
                float progress = gloamEase(MathHelper.clamp(
                        phaseAge / GLOAM_TENDRIL_RETRACT_TICKS, 0.0F, 1.0F));
                Vec3d previousAttachment = tendril.phaseAttachment.subtract(targetPosition);
                drawGloamTendril(vertices, matrix, anchor, previousAttachment,
                        age, tendril.seed, 1.0F - progress, color,
                        Math.round(alpha * (1.0F - progress * 0.55F)));
            }
        }
    }

    private static void drawSnappingGloamTendril(
            VertexConsumer vertices, Matrix4f matrix,
            AnchoredGloamTendril tendril, Vec3d targetPosition,
            Vec3d currentAttachment, float age, int color, int alpha) {
        float progress = gloamEase(MathHelper.clamp(
                (age - tendril.phaseStart) / GLOAM_TENDRIL_SNAP_TICKS, 0.0F, 1.0F));
        Vec3d anchor = tendril.anchor.subtract(targetPosition);
        Vec3d breakPoint = tendril.breakPoint.subtract(targetPosition);
        Vec3d snapAttachment = tendril.phaseAttachment.subtract(targetPosition);
        Vec3d span = tendril.phaseAttachment.subtract(tendril.anchor);
        Vec3d side = horizontalPerpendicular(span);
        double whip = Math.sin(progress * MathHelper.PI)
                * Math.min(0.42, 0.18 + span.length() * 0.045);
        Vec3d groundEnd = breakPoint.lerp(anchor, progress * 0.82)
                .add(side.multiply(whip));
        Vec3d targetStart = breakPoint.lerp(currentAttachment, progress * 0.82)
                .subtract(side.multiply(whip));
        int groundAlpha = Math.round(alpha * (1.0F - progress * 0.52F));
        int targetAlpha = Math.round(alpha * (1.0F - progress));
        drawGloamTendril(vertices, matrix, anchor, groundEnd,
                age, tendril.seed, 1.0F, color, groundAlpha);
        if (targetAlpha > 2) {
            Vec3d targetEnd = snapAttachment.lerp(currentAttachment, progress);
            drawGloamTendril(vertices, matrix, targetEnd, targetStart,
                    age, tendril.seed ^ 0x5deece66dL, 1.0F, color, targetAlpha);
        }
    }

    private static boolean canRenderGloamAccent(LivingEntity entity) {
        if (IrisCompat.isRenderingShadowPass() || gloamAccentsThisFrame >= 24) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return client.gameRenderer == null
                || client.gameRenderer.getCamera().getPos().squaredDistanceTo(entity.getPos()) <= 2304.0;
    }

    private static void drawGloamTendril(
            VertexConsumer vertices, Matrix4f matrix,
            Vec3d start, Vec3d end, float age, long seed,
            float reveal, int color, int alpha) {
        Vec3d span = end.subtract(start);
        if (span.lengthSquared() < 0.0001 || reveal <= 0.0F || alpha <= 0) {
            return;
        }
        Vec3d side = horizontalPerpendicular(span);
        double curlAmplitude = Math.min(0.18, 0.055 + span.length() * 0.025);
        Vec3d previous = start;
        float previousWidth = 0.085F;
        for (int segment = 1; segment <= 4; segment++) {
            float visible = MathHelper.clamp(reveal * 4.0F - segment + 1.0F, 0.0F, 1.0F);
            if (visible <= 0.0F) {
                break;
            }
            float progress = segment / 4.0F;
            float curl = (float) (MathHelper.sin(age * 0.065F + segment * 0.74F
                    + (float) BoltPath.unit(seed, segment, 347L) * 2.0F)
                    * curlAmplitude * MathHelper.sin(progress * MathHelper.PI));
            Vec3d fullNext = start.lerp(end, progress)
                    .add(side.multiply(curl))
                    .add(0.0, MathHelper.sin(progress * MathHelper.PI) * 0.055, 0.0);
            Vec3d next = previous.lerp(fullNext, visible);
            float nextWidth = MathHelper.lerp(progress, 0.085F, 0.025F);
            nextWidth = MathHelper.lerp(visible, previousWidth, nextWidth);
            Vec3d previousOffset = side.multiply(previousWidth);
            Vec3d nextOffset = side.multiply(nextWidth);
            woundColorQuad(vertices, matrix,
                    previous.add(previousOffset), previous.subtract(previousOffset),
                    next.subtract(nextOffset), next.add(nextOffset), color, alpha);
            woundColorQuad(vertices, matrix,
                    next.add(nextOffset), next.subtract(nextOffset),
                    previous.subtract(previousOffset), previous.add(previousOffset), color, alpha);
            Vec3d direction = next.subtract(previous);
            Vec3d cross = direction.lengthSquared() < 0.0001
                    ? UP
                    : direction.normalize().crossProduct(side).normalize();
            Vec3d previousCross = cross.multiply(previousWidth * 0.68F);
            Vec3d nextCross = cross.multiply(nextWidth * 0.68F);
            woundColorQuad(vertices, matrix,
                    previous.add(previousCross), previous.subtract(previousCross),
                    next.subtract(nextCross), next.add(nextCross), color, alpha);
            woundColorQuad(vertices, matrix,
                    next.add(nextCross), next.subtract(nextCross),
                    previous.subtract(previousCross), previous.add(previousCross), color, alpha);
            previous = next;
            previousWidth = nextWidth;
            if (visible < 1.0F) {
                break;
            }
        }
    }

    private static Vec3d gloamTendrilAttachment(
            LivingEntity entity, Vec3d targetPosition,
            Vec3d anchor, int index) {
        Vec3d inward = anchor.subtract(targetPosition);
        inward = new Vec3d(inward.x, 0.0, inward.z);
        if (inward.lengthSquared() > 0.0001) {
            inward = inward.normalize().multiply(Math.max(0.08, entity.getWidth() * 0.26));
        }
        double height = MathHelper.clamp(
                entity.getHeight() * (0.23 + index * 0.11),
                0.22 + index * 0.08,
                1.08);
        return targetPosition.add(inward).add(0.0, height, 0.0);
    }

    private static double sampleGloamGroundY(
            LivingEntity entity, Vec3d position, double fallbackY) {
        BlockPos origin = BlockPos.ofFloored(position.x, fallbackY, position.z);
        for (int offset = 0; offset >= -6; offset--) {
            BlockPos blockPos = origin.up(offset);
            BlockState state = entity.getWorld().getBlockState(blockPos);
            VoxelShape shape = state.getCollisionShape(entity.getWorld(), blockPos);
            if (!shape.isEmpty()) {
                return blockPos.getY() + shape.getMax(Direction.Axis.Y);
            }
        }
        return fallbackY;
    }

    private static Vec3d lerpedEntityPosition(LivingEntity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
    }

    private static Vec3d horizontalPerpendicular(Vec3d direction) {
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x);
        return side.lengthSquared() < 0.0001
                ? new Vec3d(1.0, 0.0, 0.0)
                : side.normalize();
    }

    private static float gloamEase(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static final class GloamTendrilState {
        private final AnchoredGloamTendril[] tendrils =
                new AnchoredGloamTendril[GLOAM_TENDRIL_SLOTS];
        private final int[] generations = new int[GLOAM_TENDRIL_SLOTS];
        private long lastSeenTime;

        private GloamTendrilState(long lastSeenTime) {
            this.lastSeenTime = lastSeenTime;
        }
    }

    private static final class AnchoredGloamTendril {
        private final Vec3d anchor;
        private final long seed;
        private GloamTendrilPhase phase;
        private float phaseStart;
        private Vec3d phaseAttachment;
        private Vec3d breakPoint;

        private AnchoredGloamTendril(
                Vec3d anchor, long seed,
                GloamTendrilPhase phase, float phaseStart) {
            this.anchor = anchor;
            this.seed = seed;
            this.phase = phase;
            this.phaseStart = phaseStart;
            this.phaseAttachment = anchor;
            this.breakPoint = anchor;
        }
    }

    private enum GloamTendrilPhase {
        EMERGING,
        ATTACHED,
        SNAPPING,
        RETRACTING
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
