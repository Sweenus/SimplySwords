package net.sweenus.simplyswords.client.renderer;

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
import net.sweenus.simplyswords.entity.BrimstoneClaymoreVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class BrimstoneClaymoreVisualEntityRenderer extends EntityRenderer<BrimstoneClaymoreVisualEntity> {
    private static final float PLUNGE_WINDUP_TICKS = 7.0F;
    private static final float PLUNGE_TOTAL_TICKS = 18.0F;
    private final ItemRenderer itemRenderer;

    public BrimstoneClaymoreVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(BrimstoneClaymoreVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(BrimstoneClaymoreVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float radius = Math.max(0.1F, entity.getRadius());
        BattleStandardFieldRenderer.renderBrimstoneCircle(matrices, vertexConsumers, entity.age, radius);

        ItemStack stack = ItemsRegistry.BRIMSTONE_CLAYMORE.get().getDefaultStack();
        float bob = entity.isPlunging() ? 0.0F : MathHelper.sin((entity.age + tickDelta) * 0.14F) * 0.08F;
        float scale = Math.max(0.75F, entity.getScale());
        float hoverHeight = 4.35F + (scale - 1.0F) * 1.55F;
        float height = entity.isPlunging() ? getPlungeHeight(entity, tickDelta, hoverHeight) : hoverHeight + bob;

        matrices.push();
        matrices.translate(0.0, height, 0.0);
        matrices.scale(scale, scale, scale);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(225.0F));
        BakedModel model = this.itemRenderer.getModels().getModel(stack);
        this.itemRenderer.renderItem(
                stack,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                light,
                OverlayTexture.DEFAULT_UV,
                model
        );
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static float getPlungeHeight(BrimstoneClaymoreVisualEntity entity, float tickDelta, float hoverHeight) {
        float plungeAge = Math.max(0.0F, entity.age + tickDelta - entity.getPlungeStartAge());
        if (plungeAge < PLUNGE_WINDUP_TICKS) {
            float progress = plungeAge / PLUNGE_WINDUP_TICKS;
            float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
            return MathHelper.lerp(eased, hoverHeight, hoverHeight + 0.9F);
        }

        float progress = MathHelper.clamp((plungeAge - PLUNGE_WINDUP_TICKS) / (PLUNGE_TOTAL_TICKS - PLUNGE_WINDUP_TICKS), 0.0F, 1.0F);
        float eased = progress * progress * progress;
        return MathHelper.lerp(eased, hoverHeight + 0.9F, 0.45F);
    }
}
