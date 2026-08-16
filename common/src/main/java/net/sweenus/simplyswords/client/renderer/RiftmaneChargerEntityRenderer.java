package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.HorseEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.RiftmaneChargerEntity;
import org.joml.Matrix4f;

public class RiftmaneChargerEntityRenderer extends EntityRenderer<RiftmaneChargerEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int FADE_IN_TICKS = 4;
    private static final int FADE_OUT_TICKS = 8;
    private static final float EXTRA_REAR_DEGREES = 26.0F;
    private static final int STREAMER_COUNT = 5;
    private static final int STREAMER_SPANS = 7;

    private final HorseEntityModel<RiftmaneChargerEntity> model;

    public RiftmaneChargerEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new HorseEntityModel<>(context.getPart(EntityModelLayers.HORSE));
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(RiftmaneChargerEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(RiftmaneChargerEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(5.0));
    }

    @Override
    public void render(RiftmaneChargerEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }

        float age = entity.age + tickDelta;
        float remaining = entity.getLifetime() - age;
        float opacity = MathHelper.clamp(age / FADE_IN_TICKS, 0.0F, 1.0F)
                * MathHelper.clamp(remaining / FADE_OUT_TICKS, 0.0F, 1.0F);
        if (opacity < 0.01F) {
            return;
        }

        float rear = entity.getRearProgress(tickDelta);
        float emerge = RiftmaneRenderGeometry.easeOutCubic(MathHelper.clamp(age / 5.0F, 0.0F, 1.0F));
        float scale = MathHelper.lerp(emerge, 0.55F, 1.0F);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevHeadYaw, entity.headYaw) - bodyYaw;
        float headPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
        float limbAngle = entity.limbAnimator.getPos(tickDelta);
        float limbDistance = entity.limbAnimator.getSpeed(tickDelta);

        this.model.child = false;
        this.model.riding = false;
        this.model.handSwingProgress = 0.0F;
        this.model.animateModel(entity, limbAngle, limbDistance, tickDelta);
        this.model.setAngles(entity, limbAngle, limbDistance, age, headYaw, headPitch);

        int bodyColor = ColorHelper.Argb.getArgb((int) (150 * opacity), 46, 214, 210);
        matrices.push();
        applyModelTransform(matrices, bodyYaw, scale, rear);
        VertexConsumer body = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(WHITE_TEXTURE));
        this.model.render(matrices, body, light, OverlayTexture.DEFAULT_UV,
                LegacyRenderColor.red(bodyColor), LegacyRenderColor.green(bodyColor),
                LegacyRenderColor.blue(bodyColor), LegacyRenderColor.alpha(bodyColor));
        matrices.pop();

        int glowColor = ColorHelper.Argb.getArgb((int) ((95 + 90 * rear) * opacity), 190, 255, 248);
        matrices.push();
        applyModelTransform(matrices, bodyYaw, scale, rear);
        VertexConsumer glow = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        this.model.render(matrices, glow, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                LegacyRenderColor.red(glowColor), LegacyRenderColor.green(glowColor),
                LegacyRenderColor.blue(glowColor), LegacyRenderColor.alpha(glowColor));
        matrices.pop();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        matrices.scale(scale, scale, scale);
        VertexConsumer wisps = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        Matrix4f wispMatrix = matrices.peek().getPositionMatrix();
        drawStreamers(wisps, wispMatrix, entity, age, opacity, rear);
        drawCoreGlow(wisps, wispMatrix, age, opacity, rear);
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void applyModelTransform(MatrixStack matrices, float bodyYaw, float scale, float rear) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        matrices.scale(scale, scale, scale);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.501F, 0.0F);
        if (rear > 0.001F) {
            matrices.translate(0.0F, -0.15F, -0.55F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(EXTRA_REAR_DEGREES * rear));
            matrices.translate(0.0F, 0.15F, 0.55F);
        }
    }

    private static void drawStreamers(VertexConsumer vertices, Matrix4f matrix,
                                      RiftmaneChargerEntity entity, float age,
                                      float opacity, float rear) {
        int seed = entity.getSeed();
        float strength = MathHelper.clamp(0.45F + rear * 0.55F, 0.0F, 1.0F);
        int alpha = Math.round(165 * opacity * strength);
        if (alpha <= 0) {
            return;
        }
        Vec3d side = new Vec3d(1.0, 0.0, 0.0);
        for (int index = 0; index < STREAMER_COUNT; index++) {
            boolean mane = index < 3;
            double anchorX = (index % 3 - 1) * 0.18;
            double anchorY = mane ? 1.45 : 1.15;
            double anchorZ = mane ? -0.55 : 0.72;
            double reach = mane ? 1.15 : 0.95;
            Vec3d[] path = new Vec3d[STREAMER_SPANS + 1];
            for (int span = 0; span <= STREAMER_SPANS; span++) {
                float t = (float) span / STREAMER_SPANS;
                float phase = age * 0.32F + seed * 0.7F + index * 1.9F + t * 3.1F;
                double sway = MathHelper.sin(phase) * 0.22 * t;
                double curl = MathHelper.cos(phase * 0.7F) * 0.16 * t;
                path[span] = new Vec3d(
                        anchorX + sway,
                        anchorY + reach * t * (0.75F + 0.35F * rear),
                        anchorZ + curl + (mane ? -0.35 : 0.42) * t
                );
            }
            RiftmaneRenderGeometry.taperedRibbon(vertices, matrix, path, side,
                    0.075, 0.008, 168, 255, 248, alpha, 0);
        }
    }

    private static void drawCoreGlow(VertexConsumer vertices, Matrix4f matrix,
                                     float age, float opacity, float rear) {
        float pulse = 0.85F + MathHelper.sin(age * 0.4F) * 0.15F;
        int alpha = Math.round((70 + 110 * rear) * opacity);
        if (alpha <= 0) {
            return;
        }
        RiftmaneRenderGeometry.quad(vertices, matrix,
                new Vec3d(0.0, 1.35 + 0.5 * pulse, -0.35),
                new Vec3d(0.42 * pulse, 1.35, -0.35),
                new Vec3d(0.0, 1.35 - 0.5 * pulse, -0.35),
                new Vec3d(-0.42 * pulse, 1.35, -0.35),
                214, 255, 250, alpha);
        RiftmaneRenderGeometry.quad(vertices, matrix,
                new Vec3d(0.0, 1.35 + 0.5 * pulse, -0.35),
                new Vec3d(0.0, 1.35, -0.35 + 0.42 * pulse),
                new Vec3d(0.0, 1.35 - 0.5 * pulse, -0.35),
                new Vec3d(0.0, 1.35, -0.35 - 0.42 * pulse),
                214, 255, 250, alpha);
    }

}
