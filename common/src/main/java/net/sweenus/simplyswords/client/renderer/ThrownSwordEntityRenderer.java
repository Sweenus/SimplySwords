package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.sweenus.simplyswords.entity.ThrownSwordEntity;

public class ThrownSwordEntityRenderer extends FlyingItemEntityRenderer<ThrownSwordEntity> {

    public ThrownSwordEntityRenderer(EntityRendererFactory.Context context) {
        super(context, 1.0f, false);
    }
}
