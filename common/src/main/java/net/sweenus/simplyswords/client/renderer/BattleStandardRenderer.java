package net.sweenus.simplyswords.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardModel;
import net.sweenus.simplyswords.entity.BattleStandardEntity;

@Environment(value= EnvType.CLIENT)
public class BattleStandardRenderer extends MobEntityRenderer<BattleStandardEntity, LivingEntityRenderState, BattleStandardModel> {


     private static final Identifier TEXTURE = Identifier.of("simplyswords","textures/entity/battlestandard/battlestandard_texture.png");

     public BattleStandardRenderer(EntityRendererFactory.Context context) {
         super(context, new BattleStandardModel(context.getPart(SimplySwords.Client.BATTLESTANDARD_MODEL)), 0.1f);
     }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTexture(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
