package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlFeatureRenderer;
import net.sweenus.simplyswords.client.renderer.feature.ShoulderAxolotlRenderStateAccess;
import net.sweenus.simplyswords.util.ShoulderAxolotlData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplyswords$addCustomShoulderFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).invokeAddFeature(new ShoulderAxolotlFeatureRenderer(
                (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (FeatureRendererContext<?, ?>) this,
                context.getPart(EntityModelLayers.AXOLOTL)
        ));
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void simplyswords$updateShoulderRenderState(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ShoulderAxolotlRenderStateAccess access = (ShoulderAxolotlRenderStateAccess) state;
        access.simplyswords$setLeftShoulderAxolotlVariant(ShoulderAxolotlData.getLeftVariant(entity).orElse(-1));
        access.simplyswords$setRightShoulderAxolotlVariant(ShoulderAxolotlData.getRightVariant(entity).orElse(-1));
    }
}



