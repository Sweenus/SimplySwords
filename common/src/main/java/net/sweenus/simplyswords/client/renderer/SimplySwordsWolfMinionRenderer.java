package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.WolfEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.WolfEntity;
import net.sweenus.simplyswords.client.renderer.feature.WolfMinionMouthItemFeatureRenderer;

public class SimplySwordsWolfMinionRenderer extends WolfEntityRenderer {

    private static final float MINION_SCALE = 1.15F;

    public SimplySwordsWolfMinionRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.addFeature(new WolfMinionMouthItemFeatureRenderer(this, context.getModelLoader().getModelPart(EntityModelLayers.WOLF)));
    }

    @Override
    public void render(WolfEntity wolf, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(MINION_SCALE, MINION_SCALE, MINION_SCALE);
        super.render(wolf, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
