package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.DragonHeadEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DragonMawHeadVisualEntity;

public class DragonMawHeadVisualEntityRenderer extends EntityRenderer<DragonMawHeadVisualEntity> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/enderdragon/dragon.png");
    private final DragonHeadEntityModel model;

    public DragonMawHeadVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new DragonHeadEntityModel(context.getPart(EntityModelLayers.DRAGON_SKULL));
    }

    @Override
    public Identifier getTexture(DragonMawHeadVisualEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(DragonMawHeadVisualEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(4.0));
    }

    @Override
    public void render(DragonMawHeadVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        if (shouldSkipFirstPersonOwner(entity)) {
            return;
        }

        float fade = MathHelper.clamp((entity.age + tickDelta) / 8.0F, 0.0F, 1.0F);
        if (fade <= 0.01F) {
            return;
        }

        float renderYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getTargetYaw());
        float renderPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getTargetPitch());
        float mouthOpen = entity.getMouthOpenProgress(tickDelta);

        Vec3d renderAnchor = getRenderAnchor(entity, tickDelta);
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        Vec3d offset = renderAnchor.subtract(entityPos);

        matrices.push();
        matrices.translate(offset.x, offset.y, offset.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - renderYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-renderPitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        float scale = entity.getEffectiveScale();
        matrices.scale(scale, scale, scale);
        matrices.translate(0.0F, -0.35F, 0.16F);

        this.model.setHeadRotation(2.5F * mouthOpen, 0.0F, 0.0F);
        RenderLayer layer = SkullBlockEntityRenderer.getRenderLayer(SkullBlock.Type.DRAGON, null);
        VertexConsumer vertices = vertexConsumers.getBuffer(layer);
        this.model.render(matrices, vertices,
                Math.max(light, LightmapTextureManager.MAX_LIGHT_COORDINATE),
                OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, fade);
        matrices.pop();
    }

    private static Vec3d getRenderAnchor(DragonMawHeadVisualEntity entity, float tickDelta) {
        if (entity.getOwner() instanceof LivingEntity owner) {
            return new Vec3d(
                    MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                    MathHelper.lerp(tickDelta, owner.prevY, owner.getY()) + owner.getEyeHeight(owner.getPose()) + Config.gemPowers.dragonMaw.headVerticalOffset,
                    MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ())
            );
        }
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private static boolean shouldSkipFirstPersonOwner(DragonMawHeadVisualEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return false;
        }
        Entity owner = entity.getOwner();
        return owner == client.player && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }
}
