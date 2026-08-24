package net.sweenus.simplyswords.client.renderer;

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
import net.sweenus.simplyswords.entity.BloodwakeBladeVisualEntity;

public class BloodwakeBladeVisualEntityRenderer extends EntityRenderer<BloodwakeBladeVisualEntity> {
    private final ItemRenderer itemRenderer;

    public BloodwakeBladeVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(BloodwakeBladeVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(BloodwakeBladeVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        ItemStack stack = entity.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        float age = entity.age + tickDelta;
        float plunge = MathHelper.clamp(entity.getPlungeProgress(), 0.0F, 1.0F);
        float pulse = 1.0F + MathHelper.sin(age * 0.35F) * 0.08F * (1.0F - plunge);

        matrices.push();
        matrices.scale(1.25F * pulse, 1.25F * pulse, 1.25F * pulse);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * (6.0F + plunge * 18.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(plunge, -92.0F, 178.0F)));
        matrices.translate(0.0, MathHelper.sin(age * 0.18F) * 0.08F * (1.0F - plunge), 0.0);
        BakedModel model = this.itemRenderer.getModels().getModel(stack);
        this.itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, false, matrices, consumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, model);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
}
