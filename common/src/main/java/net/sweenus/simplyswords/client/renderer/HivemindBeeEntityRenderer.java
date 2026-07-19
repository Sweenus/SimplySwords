package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BeeEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.BeeEntity;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;

public class HivemindBeeEntityRenderer extends BeeEntityRenderer {

    private static final float SWARM_BEE_SCALE = 0.35F;

    public HivemindBeeEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(BeeEntity bee, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (bee instanceof SimplySwordsBeeEntity simplySwordsBee && simplySwordsBee.isHivemindSwarmBee()) {
            matrices.push();
            matrices.scale(SWARM_BEE_SCALE, SWARM_BEE_SCALE, SWARM_BEE_SCALE);
            super.render(bee, yaw, tickDelta, matrices, vertexConsumers, light);
            matrices.pop();
            return;
        }
        super.render(bee, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
