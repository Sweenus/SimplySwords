package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.StormscaleRodVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class StormscaleRodVisualEntityRenderer extends EntityRenderer<StormscaleRodVisualEntity> {

    private static final float PULSE_DURATION = 14.0F;
    private final ItemRenderer itemRenderer;

    public StormscaleRodVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(StormscaleRodVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(StormscaleRodVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(entity.getRadius() + 2.0F, 4.0F,
                entity.getRadius() + 2.0F));
    }

    @Override
    public void render(StormscaleRodVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float pulseAge = age - entity.getPulseStartAge();
        ModernFieldRenderer.renderStormscaleCircle(
                matrices, vertexConsumers, age, pulseAge, entity.getRadius());

        float pulse = pulseAge >= 0.0F && pulseAge <= PULSE_DURATION
                ? 1.0F - pulseAge / PULSE_DURATION
                : 0.0F;
        float scale = 2.15F + pulse * 0.22F;
        float sway = MathHelper.sin(age * 0.11F) * 1.4F;
        ItemStack stack = ItemsRegistry.STORMSCALE.get().getDefaultStack();

        matrices.push();
        matrices.translate(0.0, 1.55 + pulse * 0.08, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw() + sway));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(225.0F));
        matrices.scale(scale, scale, scale);
        BakedModel model = this.itemRenderer.getModels().getModel(stack);
        this.itemRenderer.renderItem(
                stack,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                model
        );
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
