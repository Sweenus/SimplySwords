package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BeeEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;

public class HivemindBeeEntityRenderer extends BeeEntityRenderer {

    private static final float SWARM_BEE_SCALE = 0.35F;
    private static final float BLOOD_FLY_SCALE = 0.28F;
    private static final Identifier BLOOD_FLY_TEXTURE = Identifier.of(SimplySwords.MOD_ID, "textures/entity/blood_fly.png");

    public HivemindBeeEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(BeeEntity bee, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (bee instanceof SimplySwordsBeeEntity simplySwordsBee && simplySwordsBee.isHivemindSwarmBee()) {
            matrices.push();
            float scale = simplySwordsBee.isBloodwakeFly() ? BLOOD_FLY_SCALE : SWARM_BEE_SCALE;
            matrices.scale(scale, scale, scale);
            super.render(bee, yaw, tickDelta, matrices, vertexConsumers, light);
            matrices.pop();
            return;
        }
        super.render(bee, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(BeeEntity bee) {
        return bee instanceof SimplySwordsBeeEntity simplySwordsBee && simplySwordsBee.isBloodwakeFly()
                ? BLOOD_FLY_TEXTURE
                : super.getTexture(bee);
    }
}
