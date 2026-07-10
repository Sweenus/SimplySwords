package net.sweenus.simplyswords.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardModel;
import net.sweenus.simplyswords.entity.BattleStandardEntity;

@Environment(value= EnvType.CLIENT)
public class BattleStandardRenderer extends MobEntityRenderer<BattleStandardEntity, BattleStandardModel> {


     private static final Identifier TEXTURE = Identifier.of("simplyswords","textures/entity/battlestandard/battlestandard_texture.png");
     private static final double FIELD_CULLING_RADIUS = 6.25;

     public BattleStandardRenderer(EntityRendererFactory.Context context) {
         super(context, new BattleStandardModel(context.getPart(SimplySwords.Client.BATTLESTANDARD_MODEL)), 0.1f);
     }

    @Override
    public Identifier getTexture(BattleStandardEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(BattleStandardEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || (BattleStandardFieldRenderer.isEnabled()
                && "sunfire".equals(entity.getStandardType())
                && frustum.isVisible(entity.getBoundingBox().expand(FIELD_CULLING_RADIUS, 1.0, FIELD_CULLING_RADIUS)));
    }

    @Override
    public void render(BattleStandardEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        if ("sunfire".equals(entity.getStandardType())) {
            BattleStandardFieldRenderer.renderSunfire(matrices, vertexConsumers, entity.age);
        }
    }
}
