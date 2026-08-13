package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import net.sweenus.simplyswords.client.ShadowDanceFovHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow private float fovMultiplier;
    @Shadow private float lastFovMultiplier;

    @Inject(method = "updateFovMultiplier", at = @At("TAIL"))
    private void simplyswords$removeIonBeamFovZoom(CallbackInfo ci) {
        if (IonboundBeamClientState.isOwnedBeamActive()) {
            this.fovMultiplier = 1.0F;
            this.lastFovMultiplier = 1.0F;
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void simplyswords$shadowDanceFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double fov = ShadowDanceFovHandler.modifyFov(camera.getFocusedEntity(), cir.getReturnValueD());
        fov += IonboundBeamClientState.getFovAddition(tickDelta);
        cir.setReturnValue(fov);
    }
}
