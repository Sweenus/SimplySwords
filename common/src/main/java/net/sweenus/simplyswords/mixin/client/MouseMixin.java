package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SmoothUtil;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow @Final private SmoothUtil cursorXSmoother;
    @Shadow @Final private SmoothUtil cursorYSmoother;

    @Unique private boolean simplyswords$beamSmoothingActive;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void simplyswords$resetIonBeamSmoothing(CallbackInfo ci) {
        boolean beamActive = IonboundBeamClientState.isOwnedBeamActive();
        if (beamActive != simplyswords$beamSmoothingActive) {
            cursorXSmoother.clear();
            cursorYSmoother.clear();
            simplyswords$beamSmoothingActive = beamActive;
        }
    }

    @Redirect(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/SmoothUtil;clear()V"
            )
    )
    private void simplyswords$preserveIonBeamSmoothing(SmoothUtil smoother) {
        if (!IonboundBeamClientState.isOwnedBeamActive()) {
            smoother.clear();
        }
    }

    @Redirect(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void simplyswords$scaleIonBeamLook(ClientPlayerEntity player, double deltaX, double deltaY) {
        if (IonboundBeamClientState.isOwnedBeamActive()) {
            player.changeLookDirection(
                    IonboundBeamClientState.scaleLookDelta(deltaX),
                    IonboundBeamClientState.scaleLookDelta(deltaY));
            return;
        }
        player.changeLookDirection(deltaX, deltaY);
    }
}
