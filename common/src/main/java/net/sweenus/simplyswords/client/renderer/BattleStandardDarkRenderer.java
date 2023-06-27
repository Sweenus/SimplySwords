package net.sweenus.simplyswords.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardDarkModel;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;

@Environment(value= EnvType.CLIENT)
public class BattleStandardDarkRenderer extends MobEntityRenderer<BattleStandardDarkEntity, BattleStandardDarkModel> {


     private static final Identifier TEXTURE = new Identifier("simplyswords","textures/entity/battlestandard/battlestandarddark_texture.png");

     public BattleStandardDarkRenderer(EntityRendererFactory.Context context) {
         super(context, new BattleStandardDarkModel(context.getPart(SimplySwords.Client.BATTLESTANDARD_DARK_MODEL)), 0.1f);
     }

    @Override
    public Identifier getTexture(BattleStandardDarkEntity entity) {
        return TEXTURE;
    }
}
