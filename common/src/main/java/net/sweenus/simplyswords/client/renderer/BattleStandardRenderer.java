package net.sweenus.simplyswords.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CowEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardModel;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.SimplySwords;

@Environment(value= EnvType.CLIENT)
public class BattleStandardRenderer extends MobEntityRenderer<BattleStandardEntity, BattleStandardModel> {


     private static final Identifier TEXTURE = new Identifier("simplyswords","textures/entity/battlestandard/battlestandard_texture.png");

     public BattleStandardRenderer(EntityRendererFactory.Context context) {
         super(context, new BattleStandardModel(context.getPart(SimplySwords.BATTLESTANDARD_MODEL)), 0.1f);
     }

    @Override
    public Identifier getTexture(BattleStandardEntity entity) {
        return TEXTURE;
    }
}
