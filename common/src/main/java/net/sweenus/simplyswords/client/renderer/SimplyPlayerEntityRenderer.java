package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlFeatureRenderer;

public class SimplyPlayerEntityRenderer extends PlayerEntityRenderer {

    public SimplyPlayerEntityRenderer(EntityRendererFactory.Context context, boolean slim) {
        super(context, slim);

        // Add the axolotl shoulder feature
        this.addFeature(new ShoulderAxolotlFeatureRenderer(this, context.getPart(EntityModelLayers.AXOLOTL)));
    }
}