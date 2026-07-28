package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BatEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

public class WatcherBatEntityRenderer extends BatEntityRenderer {
    private static final Identifier TEXTURE =
            Identifier.of(SimplySwords.MOD_ID, "textures/entity/watcher_bat.png");
    private static final float SCALE = 0.82F;

    public WatcherBatEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BatEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BatEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(SCALE, SCALE, SCALE);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, 0x00F000F0);
        matrices.pop();
    }
}
