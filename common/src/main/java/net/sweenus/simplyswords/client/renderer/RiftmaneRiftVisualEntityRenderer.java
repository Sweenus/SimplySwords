package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
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
import net.sweenus.simplyswords.client.render.LightningRenderLayers;
import net.sweenus.simplyswords.client.render.MinecraftLightningRenderer;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.RiftmaneRiftVisualEntity;
import org.joml.Matrix4f;

public class RiftmaneRiftVisualEntityRenderer extends EntityRenderer<RiftmaneRiftVisualEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int LENS_SEGMENTS = 40;
    private static final float OPEN_TICKS = 6.0F;
    private static final float CLOSE_TICKS = 4.0F;
    private static final float HALF_WIDTH_RATIO = 0.34F;
    private static final int ARC_COUNT = 5;
    private static final int RING_SEGMENTS = 40;
    private static final float SLAM_RING_TICKS = 8.0F;

    public RiftmaneRiftVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(RiftmaneRiftVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(RiftmaneRiftVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(6.0));
    }

    @Override
    public void render(RiftmaneRiftVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (IrisCompat.isRenderingShadowPass() || !Config.general.enableModernFieldEffects) {
            return;
        }

        float age = entity.age + tickDelta;
        float remaining = entity.getLifetime() - age;
        float open = RiftmaneRenderGeometry.easeOutBack(MathHelper.clamp(age / OPEN_TICKS, 0.0F, 1.0F));
        float close = MathHelper.clamp(remaining / CLOSE_TICKS, 0.0F, 1.0F);
        float widthScale = open * MathHelper.clamp(close * 1.7F, 0.0F, 1.0F);
        float heightScale = open * close;
        boolean lensVisible = widthScale >= 0.01F && heightScale >= 0.01F;
        float ringProgress = MathHelper.clamp(
                (age - (entity.getLifetime() - SLAM_RING_TICKS)) / SLAM_RING_TICKS, 0.0F, 1.0F);
        boolean ringVisible = ringProgress > 0.0F && ringProgress < 1.0F;
        if (!lensVisible && !ringVisible) {
            return;
        }

        float height = entity.getRiftHeight();
        double halfHeight = height * 0.5 * heightScale;
        double halfWidth = height * HALF_WIDTH_RATIO * widthScale;
        double centerY = height * 0.5;
        int seed = entity.getSeed();
        float wobblePhase = age * 0.35F + seed;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (lensVisible) {
            VertexConsumer core = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE));
            RiftmaneRenderGeometry.lensFill(core, matrix, LENS_SEGMENTS, centerY, halfWidth, halfHeight,
                    4, 8, 14, 235, 120);
        }

        VertexConsumer glow = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        if (lensVisible) {
            RiftmaneRenderGeometry.lensRim(glow, matrix, LENS_SEGMENTS, centerY, halfWidth, halfHeight,
                    0.14 + 0.05 * MathHelper.sin(wobblePhase * 1.7F), 0.45F, wobblePhase,
                    150, 255, 246, 230, 0);
            RiftmaneRenderGeometry.lensRim(glow, matrix, LENS_SEGMENTS, centerY, halfWidth, halfHeight,
                    0.05, 0.2F, wobblePhase * 1.4F, 226, 255, 252, 255, 90);
            drawLightColumn(glow, matrix, height, halfWidth, heightScale, wobblePhase);
        }
        if (ringVisible) {
            drawSlamRing(glow, matrix, ringProgress);
        }

        if (lensVisible) {
            VertexConsumer arcs = vertexConsumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
            drawRimArcs(arcs, matrix, centerY, halfWidth, halfHeight, seed, age, heightScale);
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void drawLightColumn(VertexConsumer glow, Matrix4f matrix, float height,
                                        double halfWidth, float heightScale, float wobblePhase) {
        double columnHalf = Math.max(0.06, halfWidth * 0.45);
        double top = height * (1.35 + 0.08 * MathHelper.sin(wobblePhase)) * heightScale;
        Vec3d bottomLeft = new Vec3d(-columnHalf, 0.02, -0.06);
        Vec3d bottomRight = new Vec3d(columnHalf, 0.02, -0.06);
        Vec3d topRight = new Vec3d(columnHalf * 0.35, top, -0.06);
        Vec3d topLeft = new Vec3d(-columnHalf * 0.35, top, -0.06);
        RiftmaneRenderGeometry.quad(glow, matrix, bottomLeft, bottomRight, topRight, topLeft,
                170, 255, 248, 110, 110, 0, 0);
        Vec3d sideBottomNear = new Vec3d(0.0, 0.02, -0.06 - columnHalf);
        Vec3d sideBottomFar = new Vec3d(0.0, 0.02, -0.06 + columnHalf);
        Vec3d sideTopFar = new Vec3d(0.0, top, -0.06 + columnHalf * 0.35);
        Vec3d sideTopNear = new Vec3d(0.0, top, -0.06 - columnHalf * 0.35);
        RiftmaneRenderGeometry.quad(glow, matrix, sideBottomNear, sideBottomFar, sideTopFar, sideTopNear,
                170, 255, 248, 110, 110, 0, 0);
    }

    private static void drawSlamRing(VertexConsumer vertices, Matrix4f matrix, float progress) {
        float eased = RiftmaneRenderGeometry.easeOutCubic(progress);
        float fade = MathHelper.clamp((1.0F - progress) * 1.35F, 0.0F, 1.0F);
        int alpha = Math.round(200 * fade);
        double radius = MathHelper.lerp(eased, 0.35F, 2.6F);
        double width = Math.max(0.04, 0.28 * (1.0 - progress * 0.6));
        RiftmaneRenderGeometry.groundRing(vertices, matrix, RING_SEGMENTS,
                0.06 + eased * 0.12, radius, width, 120, 250, 240, alpha);
        RiftmaneRenderGeometry.groundRing(vertices, matrix, RING_SEGMENTS,
                0.05 + eased * 0.09, radius * 0.72, width * 0.7, 205, 255, 250,
                Math.round(alpha * 0.55F));
    }

    private static void drawRimArcs(VertexConsumer arcs, Matrix4f matrix, double centerY,
                                    double halfWidth, double halfHeight, int seed, float age,
                                    float heightScale) {
        float intensity = MathHelper.clamp(heightScale * 1.2F, 0.0F, 1.0F);
        if (intensity <= 0.02F) {
            return;
        }
        for (int index = 0; index < ARC_COUNT; index++) {
            double base = MathHelper.TAU * index / ARC_COUNT + age * 0.06 + seed * 0.31;
            double span = MathHelper.TAU * 0.16;
            Vec3d start = new Vec3d(Math.cos(base) * halfWidth,
                    centerY + Math.sin(base) * halfHeight, 0.02);
            Vec3d end = new Vec3d(Math.cos(base + span) * halfWidth,
                    centerY + Math.sin(base + span) * halfHeight, 0.02);
            float[] path = MinecraftLightningRenderer.generate(start, end,
                    seed * 31L + index, age, 0.35F, 6, 3.0F);
            MinecraftLightningRenderer.draw(arcs, matrix, path, 0.035F, 0.02F,
                    0xE6FFFC, 0x3FD6CE, intensity * 0.85F, false);
        }
    }
}
