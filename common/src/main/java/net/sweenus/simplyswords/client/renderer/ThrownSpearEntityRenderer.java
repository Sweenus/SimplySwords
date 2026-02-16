package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.sweenus.simplyswords.entity.ThrownSpearEntity;

public class ThrownSpearEntityRenderer extends FlyingItemEntityRenderer<ThrownSpearEntity> {

    public ThrownSpearEntityRenderer(EntityRendererFactory.Context context) {
        super(context, 1.0f, false);
    }
}
