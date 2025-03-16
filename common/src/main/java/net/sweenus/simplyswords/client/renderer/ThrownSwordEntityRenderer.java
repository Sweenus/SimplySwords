package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.ThrownSwordEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class ThrownSwordEntityRenderer extends EntityRenderer<ThrownSwordEntity> {
    private final ItemRenderer itemRenderer;

    public ThrownSwordEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSwordEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ItemStack swordStack = ItemsRegistry.LIVYATAN.get().getDefaultStack(); // Ideally this would be dynamic and used for any itemStack
        if (swordStack == null || swordStack.isEmpty()) {
            System.out.println("ThrownSwordEntityRenderer: swordStack is null or empty!");
        }

        if (swordStack != null && !swordStack.isEmpty()) {
            matrices.push();
            matrices.translate(0, 0.5, 0);
            matrices.scale(1.0F, 1.0F, 1.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-entity.getPitch()));
            BakedModel model = this.itemRenderer.getModels().getModel(swordStack); // Get the BakedModel for the swordStack
            this.itemRenderer.renderItem(
                    swordStack,
                    ModelTransformationMode.GROUND,
                    false,
                    matrices,
                    vertexConsumers,
                    light,
                    0,
                    model
            );

            matrices.translate(0, 0.25, 0);
            matrices.pop();
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(ThrownSwordEntity entity) {
        // This renderer doesn't use a specific texture, as it renders the ItemStack
        return null;
    }
}
