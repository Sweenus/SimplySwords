package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.sweenus.simplyswords.client.render.LightningRenderLayers;

enum DawnquiverRenderPass {
    BODY(LightningRenderLayers.BLOCKY_LIGHTNING),
    GLOW(LightningRenderLayers.LIGHTNING);

    private final RenderLayer layer;

    DawnquiverRenderPass(RenderLayer layer) {
        this.layer = layer;
    }

    VertexConsumer getBuffer(VertexConsumerProvider consumers) {
        return consumers.getBuffer(layer);
    }

    boolean isBody() {
        return this == BODY;
    }

    boolean isGlow() {
        return this == GLOW;
    }
}
