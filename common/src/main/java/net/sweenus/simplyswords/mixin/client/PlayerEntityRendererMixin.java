package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplyswords$addCustomShoulderFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        // Call the protected addFeature method via the accessor mixin
        System.out.println("Simply Swords: Adding axolotl shoulder feature to client");
        ((LivingEntityRendererAccessor) this).invokeAddFeature(new ShoulderAxolotlFeatureRenderer(
                (FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) this,
                context.getPart(EntityModelLayers.AXOLOTL)
        ));
    }
}




