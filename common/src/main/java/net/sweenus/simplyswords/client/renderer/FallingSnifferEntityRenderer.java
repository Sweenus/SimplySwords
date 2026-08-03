package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SnifferEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.entity.FallingSnifferEntity;

public class FallingSnifferEntityRenderer extends MobEntityRenderer<FallingSnifferEntity, SnifferEntityModel<FallingSnifferEntity>> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/sniffer/sniffer.png");

    public FallingSnifferEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SnifferEntityModel<>(context.getPart(EntityModelLayers.SNIFFER)), 1.1F);
    }

    @Override
    public Identifier getTexture(FallingSnifferEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(FallingSnifferEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        entity.forceFlatDiggingPose(tickDelta);
        this.shadowRadius = entity.getWorld().isClient() && entity.isOnGround() ? 1.35F : 0.75F;
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
