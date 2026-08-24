package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.util.ParchmentRendering;
import net.sweenus.simplyswords.api.render.ParchmentChannelRenderData;
import net.sweenus.simplyswords.client.api.ParchmentVisualRegistry;

public class SpellChannelVisualEntityRenderer<T extends Entity & ParchmentChannelRenderData> extends EntityRenderer<T> {

    private static final float CHEST_HEIGHT_FRACTION = 0.72F;
    private static final float FORWARD_OFFSET = 0.35F;

    public SpellChannelVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return ParchmentVisualRegistry.getGlyphStyle(entity.getGlyphStyleId()).glyphTexture();
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        // The glyph arc extends well beyond the entity's culling box.
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(4.0));
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float progress = entity.getProgress();
        if (progress <= 0.0F) {
            return;
        }

        float age = entity.age + tickDelta;
        Entity owner = resolveOwner(entity);

        Vec3d offset = Vec3d.ZERO;
        float lookYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        float lookPitch = 0.0F;
        float height = 1.2F;
        if (owner != null) {
            Vec3d ownerPos = new Vec3d(
                    MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                    MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                    MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ()));
            Vec3d selfPos = new Vec3d(
                    MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                    MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                    MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
            offset = ownerPos.subtract(selfPos);
            height = owner.getHeight() * CHEST_HEIGHT_FRACTION;
            lookYaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevYaw, owner.getYaw());
            lookPitch = MathHelper.lerp(tickDelta, owner.prevPitch, owner.getPitch());
        }

        matrices.push();
        matrices.translate(offset.x, offset.y + height, offset.z);
        // Local -Z follows the owner's look direction.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - lookYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-lookPitch));
        matrices.translate(0.0F, 0.0F, -FORWARD_OFFSET);

        ParchmentRendering.renderGlyphArc(matrices, vertexConsumers, entity.getGlyphStyleId(),
                entity.getGlyphBase(), entity.getGlyphCount(), progress, age, 1.35F);
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static <T extends Entity & ParchmentChannelRenderData> Entity resolveOwner(T entity) {
        int ownerId = entity.getOwnerEntityId();
        if (ownerId < 0 || MinecraftClient.getInstance().world == null) {
            return null;
        }
        return MinecraftClient.getInstance().world.getEntityById(ownerId);
    }
}
