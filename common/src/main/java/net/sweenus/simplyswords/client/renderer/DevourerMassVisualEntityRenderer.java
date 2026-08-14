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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DevourerMassVisualEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DevourerMassVisualEntityRenderer extends EntityRenderer<DevourerMassVisualEntity> {
    private static final Identifier WHITE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int CORE_COUNT = 8;
    private static final int MIN_BLOCKS = 18;
    private static final int BLOCK_VARIATION = 7;
    private static final int EYE_COUNT = 7;
    private static final int CACHE_LIMIT = 64;
    private static final FaceDirection[] HORIZONTAL_DIRECTIONS = {
            FaceDirection.NORTH, FaceDirection.EAST, FaceDirection.SOUTH, FaceDirection.WEST
    };
    private static final Map<Integer, BlockMassDefinition> MASS_CACHE = new LinkedHashMap<>(CACHE_LIMIT, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, BlockMassDefinition> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private final TerrainFieldOverlayRenderer terrainOverlay = new TerrainFieldOverlayRenderer();

    public DevourerMassVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(DevourerMassVisualEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(DevourerMassVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(DevourerMassVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        float radius = entity.getCurrentRadius(tickDelta);
        renderTerrainStain(entity, age, tickDelta, matrices, consumers);
        if (radius <= 0.01F) {
            return;
        }

        Vec3d offset = entity.getRenderPosition(tickDelta).subtract(entity.getLerpedPos(tickDelta));
        matrices.push();
        matrices.translate(offset.x, offset.y, offset.z);
        if (age < entity.getTravelTicks()) {
            VertexConsumer body = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
            renderSeed(age, radius, matrices, body, light, false);
            VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE));
            renderSeed(age, radius, matrices, glow,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, true);
        } else {
            BlockMassDefinition definition = definition(entity.getSeed());
            VertexConsumer body = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE));
            renderBodyPass(entity, definition, age, radius, tickDelta, matrices, body, light);
            VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE));
            renderGlowPass(entity, definition, age, radius, tickDelta, matrices, glow);
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void renderTerrainStain(DevourerMassVisualEntity entity,
                                    float age, float tickDelta,
                                    MatrixStack matrices, VertexConsumerProvider consumers) {
        if (!Config.general.enableModernFieldEffects || age < entity.getTravelTicks()) {
            terrainOverlay.clear(entity.getUuid());
            return;
        }
        float bloom = entity.getBloomProgress(tickDelta);
        float collapse = entity.getCollapseProgress(tickDelta);
        float opacity = ease(bloom) * (1.0F - ease(collapse));
        float stableRadius = entity.getStableRadius(tickDelta);
        if (stableRadius <= 0.01F || opacity <= 0.01F) {
            terrainOverlay.clear(entity.getUuid());
            return;
        }
        float stainRadius = stableRadius * 2.70F;
        float maximumStainRadius = entity.getMaximumRadius() * 2.70F;
        Vec3d center = entity.getLerpedPos(tickDelta);
        terrainOverlay.renderDevourerStain(entity.getWorld(), entity.getUuid(), entity.getId(),
                center.x, center.y, center.z, stainRadius, maximumStainRadius,
                6.0F, entity.getSeed(), opacity, age, tickDelta,
                matrices.peek(), consumers);
    }

    private static void renderBodyPass(DevourerMassVisualEntity entity,
                                       BlockMassDefinition definition,
                                       float age, float radius, float tickDelta,
                                       MatrixStack matrices, VertexConsumer vertices, int light) {
        float growth = growthProgress(entity, radius);
        float bloom = entity.getBloomProgress(tickDelta);
        float collapse = entity.getCollapseProgress(tickDelta);
        float feedPulse = feedPulse(entity, age);
        float feedProgress = feedProgress(entity, age);
        for (MassBlock block : definition.blocks()) {
            BlockTransform transform = blockTransform(block, definition, age,
                    growth, bloom, collapse, feedPulse, feedProgress);
            if (transform.scale() <= 0.01F) {
                continue;
            }
            matrices.push();
            applyTransform(matrices, block, transform, age);
            MatrixStack.Entry entry = matrices.peek();
            drawCubeBody(vertices, entry, block, light);
            renderBlockPatches(vertices, entry, block, light);
            if (block.fissureFace() != null) {
                renderPixelSeam(vertices, entry, block.fissureFace(), block.fissurePattern(),
                        10, 2, 19, 255, light, 1.85F);
            }
            for (EyeMount eye : definition.eyes()) {
                if (eye.blockIndex() == block.index()) {
                    renderEyeBody(entity, eye, age, bloom, collapse, feedPulse,
                            entry, vertices, light);
                }
            }
            matrices.pop();
        }
    }

    private static void renderGlowPass(DevourerMassVisualEntity entity,
                                       BlockMassDefinition definition,
                                       float age, float radius, float tickDelta,
                                       MatrixStack matrices, VertexConsumer vertices) {
        float growth = growthProgress(entity, radius);
        float bloom = entity.getBloomProgress(tickDelta);
        float collapse = entity.getCollapseProgress(tickDelta);
        float feedPulse = feedPulse(entity, age);
        float feedProgress = feedProgress(entity, age);
        for (MassBlock block : definition.blocks()) {
            BlockTransform transform = blockTransform(block, definition, age,
                    growth, bloom, collapse, feedPulse, feedProgress);
            if (transform.scale() <= 0.01F) {
                continue;
            }
            matrices.push();
            applyTransform(matrices, block, transform, age);
            MatrixStack.Entry entry = matrices.peek();
            if (block.fissureFace() != null) {
                float wave = feedWave(entity, age, block.revealOrder());
                int alpha = Math.min(255, Math.round(100.0F + wave * 145.0F + feedPulse * 18.0F));
                int red = Math.min(194, Math.round(112.0F + wave * 72.0F));
                int green = Math.min(93, Math.round(33.0F + wave * 52.0F));
                int blue = Math.min(255, Math.round(202.0F + wave * 53.0F));
                renderPixelSeam(vertices, entry, block.fissureFace(), block.fissurePattern(),
                        red, green, blue, alpha,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, 0.75F);
            }
            for (EyeMount eye : definition.eyes()) {
                if (eye.blockIndex() == block.index()) {
                    renderEyeGlow(entity, eye, age, bloom, collapse, feedPulse,
                            entry, vertices);
                }
            }
            matrices.pop();
        }
    }

    private static float growthProgress(DevourerMassVisualEntity entity, float radius) {
        float start = Math.max(0.2F, entity.getStartingRadius());
        float maximum = Math.max(start + 0.001F, entity.getMaximumRadius());
        return MathHelper.clamp((radius - start) / (maximum - start), 0.0F, 1.0F);
    }

    private static BlockTransform blockTransform(MassBlock block,
                                                 BlockMassDefinition definition,
                                                 float age, float growth, float bloom,
                                                 float collapse, float feedPulse,
                                                 float feedProgress) {
        float availability = block.core() ? bloom : Math.min(bloom, growth);
        float revealWidth = block.core() ? 0.55F : 0.34F;
        float reveal = ease(MathHelper.clamp((availability - block.revealOrder())
                / revealWidth, 0.0F, 1.0F));
        float collapseStart = block.core() ? 0.44F + block.revealOrder() * 0.16F
                : Math.max(0.0F, (1.0F - block.revealOrder()) * 0.3F);
        float retract = 1.0F - ease(MathHelper.clamp((collapse - collapseStart)
                / (block.core() ? 0.56F : 0.62F), 0.0F, 1.0F));
        float pulse = block.pulses()
                ? MathHelper.sin(age * block.pulseSpeed() + block.phase()) : 0.0F;
        float scale = reveal * retract * (1.0F + pulse * block.pulseAmount());
        float feedDistance = Math.abs(block.revealOrder() - feedProgress);
        float feedWave = feedPulse <= 0.0F ? 0.0F
                : ease(MathHelper.clamp(1.0F - feedDistance * 4.0F, 0.0F, 1.0F));
        scale *= 1.0F + feedWave * 0.13F;

        Vec3d center = block.center();
        if (reveal < 1.0F) {
            MassBlock parent = definition.blocks().get(block.parentIndex());
            center = parent.center().lerp(center, reveal);
        }
        if (block.bobs()) {
            float bob = MathHelper.sin(age * block.bobSpeed() + block.phase()) * block.bobAmount();
            center = center.add(block.bobAxis().multiply(bob * reveal * retract));
        }
        if (feedWave > 0.0F) {
            Vec3d outward = center.lengthSquared() < 1.0E-5
                    ? new Vec3d(0.0, 1.0, 0.0) : center.normalize();
            center = center.add(outward.multiply(feedWave * 0.12F));
        }
        return new BlockTransform(center, scale);
    }

    private static void applyTransform(MatrixStack matrices, MassBlock block,
                                       BlockTransform transform, float age) {
        matrices.translate(transform.center().x, transform.center().y, transform.center().z);
        float rotation = block.rotates() ? age * block.rotationSpeed() + block.phase() : 0.0F;
        float rock = block.bobs()
                ? MathHelper.sin(age * block.bobSpeed() * 0.72F + block.phase()) * 3.5F : 0.0F;
        matrices.multiply(new Quaternionf().rotationXYZ(
                block.basePitch() + rotation * block.rotationAxisX() + (float) Math.toRadians(rock * 0.45F),
                block.baseYaw() + rotation * block.rotationAxisY() + (float) Math.toRadians(rock),
                block.baseRoll() + rotation * block.rotationAxisZ()));
        matrices.scale(transform.scale(), transform.scale(), transform.scale());
    }

    private static float feedProgress(DevourerMassVisualEntity entity, float age) {
        float pulseAge = age - entity.getLastFeedAge();
        if (pulseAge < 0.0F || pulseAge > 8.0F) {
            return -1.0F;
        }
        return pulseAge / 8.0F;
    }

    private static void drawCubeBody(VertexConsumer vertices, MatrixStack.Entry entry,
                                     MassBlock block, int light) {
        for (FaceDirection direction : FaceDirection.values()) {
            float shade = direction == FaceDirection.UP ? 1.12F
                    : direction == FaceDirection.DOWN ? 0.62F
                    : direction == FaceDirection.SOUTH ? 0.92F
                    : direction == FaceDirection.EAST ? 0.84F : 0.76F;
            int red = MathHelper.clamp(Math.round(block.red() * shade), 8, 82);
            int green = MathHelper.clamp(Math.round(block.green() * shade), 2, 24);
            int blue = MathHelper.clamp(Math.round(block.blue() * shade), 18, 116);
            drawFace(vertices, entry, direction, 0.5F,
                    red, green, blue, 255, light);
        }
    }

    private static void renderBlockPatches(VertexConsumer vertices, MatrixStack.Entry entry,
                                           MassBlock block, int light) {
        if (block.patchFace() == null) {
            return;
        }
        FaceBasis basis = basis(block.patchFace(), 0.506F);
        int red = Math.max(8, Math.round(block.red() * (block.patchStyle() == 3 ? 1.12F : 0.67F)));
        int green = Math.max(2, Math.round(block.green() * (block.patchStyle() == 3 ? 1.08F : 0.63F)));
        int blue = Math.max(18, Math.round(block.blue() * (block.patchStyle() == 3 ? 1.13F : 0.71F)));
        if (block.patchStyle() == 1) {
            Vec3d center = basis.center().subtract(basis.right().multiply(0.18))
                    .add(basis.up().multiply(0.15));
            drawPlaneRect(vertices, entry, center, basis.right(), basis.up(), basis.normal(),
                    0.14F, 0.1F, red, green, blue, 255, light);
        } else if (block.patchStyle() == 2) {
            Vec3d center = basis.center().add(basis.up().multiply(0.12));
            drawPlaneRect(vertices, entry, center, basis.right(), basis.up(), basis.normal(),
                    0.23F, 0.045F, red, green, blue, 255, light);
        } else {
            Vec3d first = basis.center().add(basis.right().multiply(0.16))
                    .subtract(basis.up().multiply(0.15));
            Vec3d second = basis.center().add(basis.right().multiply(0.06))
                    .subtract(basis.up().multiply(0.04));
            drawPlaneRect(vertices, entry, first, basis.right(), basis.up(), basis.normal(),
                    0.1F, 0.08F, red, green, blue, 255, light);
            drawPlaneRect(vertices, entry, second, basis.right(), basis.up(), basis.normal(),
                    0.065F, 0.05F, red, green, blue, 255, light);
        }
    }

    private static void renderPixelSeam(VertexConsumer vertices, MatrixStack.Entry entry,
                                        FaceDirection direction, int pattern,
                                        int red, int green, int blue, int alpha, int light,
                                        float widthMultiplier) {
        FaceBasis basis = basis(direction, 0.512F);
        float thin = 0.017F * widthMultiplier;
        if ((pattern & 1) == 0) {
            drawPlaneRect(vertices, entry,
                    basis.center().add(basis.up().multiply(0.19))
                            .subtract(basis.right().multiply(0.065)),
                    basis.right(), basis.up(), basis.normal(), thin, 0.125F,
                    red, green, blue, alpha, light);
            drawPlaneRect(vertices, entry,
                    basis.center().add(basis.up().multiply(0.06)),
                    basis.right(), basis.up(), basis.normal(), 0.09F, thin,
                    red, green, blue, alpha, light);
            drawPlaneRect(vertices, entry,
                    basis.center().subtract(basis.up().multiply(0.105))
                            .add(basis.right().multiply(0.065)),
                    basis.right(), basis.up(), basis.normal(), thin, 0.125F,
                    red, green, blue, alpha, light);
        } else {
            drawPlaneRect(vertices, entry,
                    basis.center().subtract(basis.right().multiply(0.185))
                            .add(basis.up().multiply(0.065)),
                    basis.right(), basis.up(), basis.normal(), 0.125F, thin,
                    red, green, blue, alpha, light);
            drawPlaneRect(vertices, entry,
                    basis.center().subtract(basis.right().multiply(0.05)),
                    basis.right(), basis.up(), basis.normal(), thin, 0.09F,
                    red, green, blue, alpha, light);
            drawPlaneRect(vertices, entry,
                    basis.center().add(basis.right().multiply(0.115))
                            .subtract(basis.up().multiply(0.065)),
                    basis.right(), basis.up(), basis.normal(), 0.125F, thin,
                    red, green, blue, alpha, light);
        }
    }

    private static void renderEyeBody(DevourerMassVisualEntity entity, EyeMount eye,
                                      float age, float bloom, float collapse, float feedPulse,
                                      MatrixStack.Entry entry, VertexConsumer vertices, int light) {
        FaceBasis basis = basis(eye.direction(), 0.516F);
        float halfWidth = eye.width();
        float halfHeight = halfWidth * eye.height();
        float opening = eyeOpening(age, bloom, collapse, eye.index(), eye.seed());
        drawPlaneDiamond(vertices, entry, basis.center(), basis.right(), basis.up(), basis.normal(),
                halfWidth, halfHeight, 7, 2, 14, 255, light);
        Vec3d pupilCenter = basis.center().add(basis.normal().multiply(0.014));
        drawPlaneRect(vertices, entry, pupilCenter, basis.right(), basis.up(), basis.normal(),
                halfWidth * 0.11F, Math.max(0.006F, halfHeight * 0.36F * opening),
                8, 3, 17, 255, light);

        float lidHeight = halfHeight * (0.13F + (1.0F - opening) * 0.42F);
        float lidOffset = halfHeight - lidHeight;
        int lidVariation = Math.round(noise(eye.seed() + 97L) * 8.0F);
        Vec3d lidSurface = basis.normal().multiply(0.019);
        drawPlaneRect(vertices, entry,
                basis.center().add(basis.up().multiply(lidOffset)).add(lidSurface),
                basis.right(), basis.up(), basis.normal(), halfWidth * 0.82F, lidHeight,
                32 + lidVariation, 9 + lidVariation / 3, 53 + lidVariation * 2,
                255, light);
        drawPlaneRect(vertices, entry,
                basis.center().subtract(basis.up().multiply(lidOffset)).add(lidSurface),
                basis.right(), basis.up(), basis.normal(), halfWidth * 0.82F, lidHeight,
                25 + lidVariation, 7 + lidVariation / 3, 45 + lidVariation * 2,
                255, light);
    }

    private static void renderEyeGlow(DevourerMassVisualEntity entity, EyeMount eye,
                                      float age, float bloom, float collapse, float feedPulse,
                                      MatrixStack.Entry entry, VertexConsumer vertices) {
        FaceBasis basis = basis(eye.direction(), 0.522F);
        float halfWidth = eye.width();
        float halfHeight = halfWidth * eye.height();
        float opening = eyeOpening(age, bloom, collapse, eye.index(), eye.seed());
        int red = Math.min(255, Math.round(220.0F + feedPulse * 35.0F));
        int green = Math.min(255, Math.round(151.0F + feedPulse * 58.0F));
        drawPlaneDiamond(vertices, entry, basis.center(), basis.right(), basis.up(), basis.normal(),
                halfWidth * 0.76F, Math.max(0.005F, halfHeight * 0.67F * opening),
                red, green, 29, Math.round(225.0F + feedPulse * 30.0F),
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        if (opening > 0.48F) {
            Vec3d glintCenter = basis.center().add(basis.right().multiply(halfWidth * 0.19F))
                    .add(basis.up().multiply(halfHeight * 0.17F))
                    .add(basis.normal().multiply(0.006));
            float glint = halfWidth * (0.045F + feedPulse * 0.018F);
            drawPlaneRect(vertices, entry, glintCenter, basis.right(), basis.up(), basis.normal(),
                    glint, glint, 255, 226, 111, 245,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
    }

    private static float eyeOpening(float age, float bloom, float collapse,
                                    int eye, long eyeSeed) {
        float awaken = ease(MathHelper.clamp((bloom - eye * 0.045F) / 0.48F,
                0.0F, 1.0F));
        float close = 1.0F - ease(MathHelper.clamp(collapse * 1.65F, 0.0F, 1.0F));
        float cycle = 58.0F + Math.round(noise(eyeSeed + 131L) * 47.0F);
        float phase = (age + noise(eyeSeed + 149L) * cycle) % cycle;
        float blink = 1.0F;
        if (phase > cycle - 7.0F) {
            float progress = (phase - cycle + 7.0F) / 7.0F;
            blink = Math.abs(progress * 2.0F - 1.0F);
        }
        return MathHelper.clamp(awaken * close * blink, 0.0F, 1.0F);
    }

    private static float feedPulse(DevourerMassVisualEntity entity, float age) {
        float pulseAge = age - entity.getLastFeedAge();
        if (pulseAge < 0.0F || pulseAge > 8.0F) {
            return 0.0F;
        }
        return MathHelper.sin(pulseAge / 8.0F * MathHelper.PI);
    }

    private static float feedWave(DevourerMassVisualEntity entity, float age, float order) {
        float pulseAge = age - entity.getLastFeedAge();
        if (pulseAge < 0.0F || pulseAge > 8.0F) {
            return 0.0F;
        }
        float progress = pulseAge / 8.0F;
        return ease(MathHelper.clamp(1.0F - Math.abs(progress - order) * 4.2F,
                0.0F, 1.0F));
    }

    private static void renderSeed(float age, float radius,
                                   MatrixStack matrices, VertexConsumer vertices,
                                   int light, boolean glow) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age * 18.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 24.0F));
        MatrixStack.Entry entry = matrices.peek();
        if (glow) {
            drawBox(vertices, entry, radius * 0.72F, radius * 0.72F, radius * 0.72F,
                    118, 48, 242, 170, light);
        } else {
            drawBox(vertices, entry, radius, radius, radius,
                    18, 7, 42, 255, light);
        }
        matrices.pop();
    }

    private static BlockMassDefinition definition(int seed) {
        BlockMassDefinition cached = MASS_CACHE.get(seed);
        if (cached != null) {
            return cached;
        }
        BlockMassDefinition created = createDefinition(seed);
        MASS_CACHE.put(seed, created);
        return created;
    }

    private static BlockMassDefinition createDefinition(int seed) {
        int count = MIN_BLOCKS + Math.floorMod(seed, BLOCK_VARIATION);
        List<MassBlock> blocks = new ArrayList<>(count);
        Set<PositionKey> occupied = new HashSet<>();
        int[][] corePositions = {
                {0, 0, 0}, {-1, 0, 0}, {1, 0, 0}, {0, 0, -1},
                {0, 0, 1}, {0, 1, 0}, {-1, -1, 0}, {0, -1, 1}
        };
        for (int index = 0; index < CORE_COUNT; index++) {
            PositionKey key = new PositionKey(corePositions[index][0],
                    corePositions[index][1], corePositions[index][2]);
            occupied.add(key);
            blocks.add(createBlock(seed, index, key, Math.max(0, index - 1), true,
                    index / (float) (CORE_COUNT * 2)));
        }

        for (int index = CORE_COUNT; index < count; index++) {
            int parentIndex = chooseParent(seed, index, blocks, occupied);
            MassBlock parent = blocks.get(parentIndex);
            PositionKey position = choosePosition(seed, index, parent.position(), occupied);
            occupied.add(position);
            float order = 0.18F + (index - CORE_COUNT)
                    / (float) Math.max(1, count - CORE_COUNT - 1) * 0.52F;
            blocks.add(createBlock(seed, index, position, parentIndex, false, order));
        }

        List<EyeMount> eyes = selectEyes(seed, blocks, occupied);
        return new BlockMassDefinition(List.copyOf(blocks), List.copyOf(eyes));
    }

    private static MassBlock createBlock(int seed, int index, PositionKey position,
                                         int parentIndex, boolean core, float revealOrder) {
        long blockSeed = seed * 421L + index * 10009L;
        float spacing = 0.78F + noise(blockSeed + 17L) * 0.06F;
        Vec3d center = new Vec3d(position.x() * spacing,
                position.y() * spacing - 0.22,
                position.z() * spacing);
        float animationRoll = noise(blockSeed + 31L);
        int animation = core && index < 6 ? 4
                : animationRoll < 0.3F ? 0
                : animationRoll < 0.6F ? 1
                : animationRoll < 0.8F ? 2 : 3;
        boolean rotates = animation == 0 || animation == 3;
        boolean pulses = animation == 1 || animation == 3;
        boolean bobs = animation == 2;
        Vec3d axis = seededAxis(blockSeed + 43L);
        Vec3d bobAxis = seededAxis(blockSeed + 59L);
        float variation = noise(blockSeed + 71L);
        int red = Math.round(31.0F + variation * 22.0F);
        int green = Math.round(7.0F + variation * 8.0F);
        int blue = Math.round(49.0F + variation * 31.0F);
        FaceDirection patchFace = noise(blockSeed + 83L) < 0.58F
                ? FaceDirection.values()[(int) (noise(blockSeed + 97L) * FaceDirection.values().length)]
                : null;
        FaceDirection fissureFace = noise(blockSeed + 109L) < 0.38F
                ? FaceDirection.values()[(int) (noise(blockSeed + 127L) * FaceDirection.values().length)]
                : null;
        return new MassBlock(index, position, parentIndex, core, center, revealOrder,
                rotates, pulses, bobs,
                (float) Math.toRadians((noise(blockSeed + 139L) - 0.5F) * 9.0F),
                (float) Math.toRadians((noise(blockSeed + 151L) - 0.5F) * 9.0F),
                (float) Math.toRadians((noise(blockSeed + 163L) - 0.5F) * 9.0F),
                (float) axis.x, (float) axis.y, (float) axis.z,
                (float) Math.toRadians(0.12F + noise(blockSeed + 179L) * 0.16F),
                MathHelper.TAU / (70.0F + noise(blockSeed + 191L) * 50.0F),
                0.075F + noise(blockSeed + 211L) * 0.015F,
                MathHelper.TAU / (90.0F + noise(blockSeed + 223L) * 70.0F),
                0.04F + noise(blockSeed + 239L) * 0.05F,
                (float) (noise(blockSeed + 251L) * MathHelper.TAU),
                bobAxis, red, green, blue,
                patchFace, 1 + (int) (noise(blockSeed + 263L) * 3.0F),
                fissureFace, (int) (noise(blockSeed + 277L) * 4.0F));
    }

    private static int chooseParent(int seed, int index, List<MassBlock> blocks,
                                    Set<PositionKey> occupied) {
        int candidateCount = Math.min(blocks.size(), CORE_COUNT + Math.max(1, (index - CORE_COUNT) / 2));
        int start = MathHelper.clamp((int) (noise(seed * 439L + index * 7027L) * candidateCount),
                0, candidateCount - 1);
        for (int offset = 0; offset < candidateCount; offset++) {
            int candidate = (start + offset) % candidateCount;
            if (hasOpenNeighbor(blocks.get(candidate).position(), occupied)) {
                return candidate;
            }
        }
        return start;
    }

    private static PositionKey choosePosition(int seed, int index, PositionKey parent,
                                              Set<PositionKey> occupied) {
        long value = seed * 457L + index * 8191L;
        for (int attempt = 0; attempt < 28; attempt++) {
            int direction = (int) (noise(value + attempt * 1741L) * 6.0F);
            int[] offset = switch (MathHelper.clamp(direction, 0, 5)) {
                case 0 -> new int[]{1, 0, 0};
                case 1 -> new int[]{-1, 0, 0};
                case 2 -> new int[]{0, 0, 1};
                case 3 -> new int[]{0, 0, -1};
                case 4 -> new int[]{0, 1, 0};
                default -> new int[]{0, -1, 0};
            };
            PositionKey candidate = new PositionKey(parent.x() + offset[0],
                    parent.y() + offset[1], parent.z() + offset[2]);
            if (!occupied.contains(candidate)
                    && candidate.y() >= -3 && candidate.y() <= 3
                    && Math.abs(candidate.x()) <= 3 && Math.abs(candidate.z()) <= 3) {
                return candidate;
            }
        }
        for (int y = -3; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    PositionKey candidate = new PositionKey(x, y, z);
                    if (!occupied.contains(candidate) && touches(candidate, occupied)) {
                        return candidate;
                    }
                }
            }
        }
        return new PositionKey(index, 0, 0);
    }

    private static boolean touches(PositionKey candidate, Set<PositionKey> occupied) {
        return occupied.contains(new PositionKey(candidate.x() + 1, candidate.y(), candidate.z()))
                || occupied.contains(new PositionKey(candidate.x() - 1, candidate.y(), candidate.z()))
                || occupied.contains(new PositionKey(candidate.x(), candidate.y() + 1, candidate.z()))
                || occupied.contains(new PositionKey(candidate.x(), candidate.y() - 1, candidate.z()))
                || occupied.contains(new PositionKey(candidate.x(), candidate.y(), candidate.z() + 1))
                || occupied.contains(new PositionKey(candidate.x(), candidate.y(), candidate.z() - 1));
    }

    private static boolean hasOpenNeighbor(PositionKey position, Set<PositionKey> occupied) {
        for (FaceDirection direction : FaceDirection.values()) {
            PositionKey candidate = position.move(direction);
            if (!occupied.contains(candidate)
                    && candidate.y() >= -3 && candidate.y() <= 3
                    && Math.abs(candidate.x()) <= 3 && Math.abs(candidate.z()) <= 3) {
                return true;
            }
        }
        return false;
    }

    private static List<EyeMount> selectEyes(int seed, List<MassBlock> blocks,
                                             Set<PositionKey> occupied) {
        List<EyeMount> eyes = new ArrayList<>();
        Set<Integer> usedBlocks = new HashSet<>();
        int rotation = Math.floorMod(seed * 31, HORIZONTAL_DIRECTIONS.length);
        for (int eye = 0; eye < EYE_COUNT; eye++) {
            FaceDirection desired = HORIZONTAL_DIRECTIONS[(rotation + eye) & 3];
            int selected = bestEyeBlock(seed, eye, desired, blocks, occupied, usedBlocks);
            if (selected < 0) {
                continue;
            }
            usedBlocks.add(selected);
            long eyeSeed = seed * 479L + eye * 10009L;
            eyes.add(new EyeMount(selected, desired, eye, eyeSeed,
                    0.29F + noise(eyeSeed + 41L) * 0.07F,
                    0.46F + noise(eyeSeed + 59L) * 0.12F));
        }
        return eyes;
    }

    private static int bestEyeBlock(int seed, int eye, FaceDirection desired,
                                    List<MassBlock> blocks, Set<PositionKey> occupied,
                                    Set<Integer> usedBlocks) {
        int selected = -1;
        float bestScore = -1000.0F;
        Vec3d normal = desired.normal();
        for (MassBlock block : blocks) {
            if (usedBlocks.contains(block.index())
                    || occupied.contains(block.position().move(desired))) {
                continue;
            }
            double outward = block.center().dotProduct(normal);
            float score = (float) outward * 0.8F
                    - Math.abs((float) block.center().y) * 0.14F
                    + noise(seed * 487L + eye * 9341L + block.index() * 619L) * 1.7F;
            if (block.index() < 2) {
                score += 0.22F;
            }
            if (score > bestScore) {
                bestScore = score;
                selected = block.index();
            }
        }
        return selected;
    }

    private static Vec3d seededAxis(long value) {
        Vec3d axis = new Vec3d(noise(value) - 0.5, noise(value + 17L) - 0.5,
                noise(value + 31L) - 0.5);
        return axis.lengthSquared() < 1.0E-5 ? new Vec3d(0.0, 1.0, 0.0) : axis.normalize();
    }

    private static FaceBasis basis(FaceDirection direction, float distance) {
        return new FaceBasis(direction.normal().multiply(distance), direction.normal(),
                direction.right(), direction.up());
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float noise(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (mixed & 0xFFFFFFL) / (float) 0x1000000;
    }

    private static void drawFace(VertexConsumer vertices, MatrixStack.Entry entry,
                                 FaceDirection direction, float halfSize,
                                 int red, int green, int blue, int alpha, int light) {
        FaceBasis basis = basis(direction, halfSize);
        drawPlaneRect(vertices, entry, basis.center(), basis.right(), basis.up(), basis.normal(),
                halfSize, halfSize, red, green, blue, alpha, light);
    }

    private static void drawPlaneDiamond(VertexConsumer vertices, MatrixStack.Entry entry,
                                         Vec3d center, Vec3d right, Vec3d up, Vec3d normal,
                                         float halfWidth, float halfHeight,
                                         int red, int green, int blue, int alpha, int light) {
        drawLayerQuad(vertices, entry,
                center.add(up.multiply(halfHeight)),
                center.add(right.multiply(halfWidth)),
                center.subtract(up.multiply(halfHeight)),
                center.subtract(right.multiply(halfWidth)),
                red, green, blue, alpha, light, normal);
    }

    private static void drawPlaneRect(VertexConsumer vertices, MatrixStack.Entry entry,
                                      Vec3d center, Vec3d right, Vec3d up, Vec3d normal,
                                      float halfWidth, float halfHeight,
                                      int red, int green, int blue, int alpha, int light) {
        Vec3d horizontal = right.multiply(halfWidth);
        Vec3d vertical = up.multiply(halfHeight);
        drawLayerQuad(vertices, entry,
                center.add(horizontal).add(vertical),
                center.add(horizontal).subtract(vertical),
                center.subtract(horizontal).subtract(vertical),
                center.subtract(horizontal).add(vertical),
                red, green, blue, alpha, light, normal);
    }

    private static void drawBox(VertexConsumer vertices, MatrixStack.Entry entry,
                                float halfX, float halfY, float halfZ,
                                int red, int green, int blue, int alpha, int light) {
        Vec3d p000 = new Vec3d(-halfX, -halfY, -halfZ);
        Vec3d p001 = new Vec3d(-halfX, -halfY, halfZ);
        Vec3d p010 = new Vec3d(-halfX, halfY, -halfZ);
        Vec3d p011 = new Vec3d(-halfX, halfY, halfZ);
        Vec3d p100 = new Vec3d(halfX, -halfY, -halfZ);
        Vec3d p101 = new Vec3d(halfX, -halfY, halfZ);
        Vec3d p110 = new Vec3d(halfX, halfY, -halfZ);
        Vec3d p111 = new Vec3d(halfX, halfY, halfZ);
        drawLayerQuad(vertices, entry, p000, p100, p110, p010,
                red, green, blue, alpha, light, new Vec3d(0.0, 0.0, -1.0));
        drawLayerQuad(vertices, entry, p101, p001, p011, p111,
                red, green, blue, alpha, light, new Vec3d(0.0, 0.0, 1.0));
        drawLayerQuad(vertices, entry, p001, p000, p010, p011,
                red, green, blue, alpha, light, new Vec3d(-1.0, 0.0, 0.0));
        drawLayerQuad(vertices, entry, p100, p101, p111, p110,
                red, green, blue, alpha, light, new Vec3d(1.0, 0.0, 0.0));
        drawLayerQuad(vertices, entry, p010, p110, p111, p011,
                red, green, blue, alpha, light, new Vec3d(0.0, 1.0, 0.0));
        drawLayerQuad(vertices, entry, p001, p101, p100, p000,
                red, green, blue, alpha, light, new Vec3d(0.0, -1.0, 0.0));
    }

    private static void drawLayerQuad(VertexConsumer vertices, MatrixStack.Entry entry,
                                      Vec3d first, Vec3d second, Vec3d third, Vec3d fourth,
                                      int red, int green, int blue, int alpha, int light,
                                      Vec3d normal) {
        layerVertex(vertices, entry, first, 0.0F, 0.0F,
                red, green, blue, alpha, light, normal);
        layerVertex(vertices, entry, second, 0.0F, 1.0F,
                red, green, blue, alpha, light, normal);
        layerVertex(vertices, entry, third, 1.0F, 1.0F,
                red, green, blue, alpha, light, normal);
        layerVertex(vertices, entry, fourth, 1.0F, 0.0F,
                red, green, blue, alpha, light, normal);
    }

    private static void layerVertex(VertexConsumer vertices, MatrixStack.Entry entry,
                                    Vec3d position, float u, float v,
                                    int red, int green, int blue, int alpha,
                                    int light, Vec3d normal) {
        vertices.vertex(entry.getPositionMatrix(), (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), (float) normal.x, (float) normal.y, (float) normal.z)
                .next();
    }

    private enum FaceDirection {
        NORTH(0, 0, -1, -1, 0, 0, 0, 1, 0),
        EAST(1, 0, 0, 0, 0, -1, 0, 1, 0),
        SOUTH(0, 0, 1, 1, 0, 0, 0, 1, 0),
        WEST(-1, 0, 0, 0, 0, 1, 0, 1, 0),
        UP(0, 1, 0, 1, 0, 0, 0, 0, -1),
        DOWN(0, -1, 0, 1, 0, 0, 0, 0, 1);

        private final int dx;
        private final int dy;
        private final int dz;
        private final Vec3d normal;
        private final Vec3d right;
        private final Vec3d up;

        FaceDirection(int normalX, int normalY, int normalZ,
                      int rightX, int rightY, int rightZ,
                      int upX, int upY, int upZ) {
            dx = normalX;
            dy = normalY;
            dz = normalZ;
            normal = new Vec3d(normalX, normalY, normalZ);
            right = new Vec3d(rightX, rightY, rightZ);
            up = new Vec3d(upX, upY, upZ);
        }

        Vec3d normal() {
            return normal;
        }

        Vec3d right() {
            return right;
        }

        Vec3d up() {
            return up;
        }
    }

    private record PositionKey(int x, int y, int z) {
        PositionKey move(FaceDirection direction) {
            return new PositionKey(x + direction.dx, y + direction.dy, z + direction.dz);
        }
    }

    private record MassBlock(int index, PositionKey position, int parentIndex, boolean core,
                             Vec3d center, float revealOrder,
                             boolean rotates, boolean pulses, boolean bobs,
                             float basePitch, float baseYaw, float baseRoll,
                             float rotationAxisX, float rotationAxisY, float rotationAxisZ,
                             float rotationSpeed, float pulseSpeed, float pulseAmount,
                             float bobSpeed, float bobAmount, float phase, Vec3d bobAxis,
                             int red, int green, int blue,
                             FaceDirection patchFace, int patchStyle,
                             FaceDirection fissureFace, int fissurePattern) {
    }

    private record EyeMount(int blockIndex, FaceDirection direction,
                            int index, long seed, float width, float height) {
    }

    private record BlockTransform(Vec3d center, float scale) {
    }

    private record FaceBasis(Vec3d center, Vec3d normal, Vec3d right, Vec3d up) {
    }

    private record BlockMassDefinition(List<MassBlock> blocks, List<EyeMount> eyes) {
    }
}
