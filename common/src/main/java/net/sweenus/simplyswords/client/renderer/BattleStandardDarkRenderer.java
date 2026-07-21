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
import net.sweenus.simplyswords.client.renderer.model.BattleStandardDarkModel;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;

@Environment(value= EnvType.CLIENT)
public class BattleStandardDarkRenderer extends MobEntityRenderer<BattleStandardDarkEntity, BattleStandardDarkModel> {


     private static final Identifier TEXTURE = Identifier.of("simplyswords","textures/entity/battlestandard/battlestandarddark_texture.png");
     private static final double FIELD_CULLING_RADIUS = 6.25;

     public BattleStandardDarkRenderer(EntityRendererFactory.Context context) {
         super(context, new BattleStandardDarkModel(context.getPart(SimplySwords.Client.BATTLESTANDARD_DARK_MODEL)), 0.1f);
     }

    @Override
    public Identifier getTexture(BattleStandardDarkEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(BattleStandardDarkEntity entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || (ModernFieldRenderer.isEnabled()
                && "harbinger".equals(entity.getStandardType())
                && frustum.isVisible(entity.getBoundingBox().expand(FIELD_CULLING_RADIUS, 1.0, FIELD_CULLING_RADIUS)));
    }

    @Override
    public void render(BattleStandardDarkEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        if ("harbinger".equals(entity.getStandardType())) {
            ModernFieldRenderer.renderHarbinger(matrices, vertexConsumers, entity.age);
        }
    }
}
