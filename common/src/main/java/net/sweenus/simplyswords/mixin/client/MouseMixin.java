package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.Smoother;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private Smoother cursorXSmoother;
    @Shadow @Final private Smoother cursorYSmoother;

    @Unique private boolean simplyswords$beamSmoothingActive;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void simplyswords$resetIonBeamSmoothing(double timeDelta, CallbackInfo ci) {
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
                    target = "Lnet/minecraft/util/math/Smoother;clear()V"
            )
    )
    private void simplyswords$preserveIonBeamSmoothing(Smoother smoother) {
        if (!IonboundBeamClientState.isOwnedBeamActive()) {
            smoother.clear();
        }
    }

    @ModifyArgs(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void simplyswords$smoothIonBeamLook(Args args, double timeDelta) {
        if (!IonboundBeamClientState.isOwnedBeamActive()) {
            return;
        }

        double horizontal = args.get(0);
        double vertical = args.get(1);
        if (!client.options.smoothCameraEnabled) {
            double sensitivity = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
            double sensitivityScale = sensitivity * sensitivity * sensitivity * 8.0;
            horizontal = cursorXSmoother.smooth(horizontal, timeDelta * sensitivityScale);
            vertical = cursorYSmoother.smooth(vertical, timeDelta * sensitivityScale);
        }

        args.set(0, IonboundBeamClientState.scaleLookDelta(horizontal));
        args.set(1, IonboundBeamClientState.scaleLookDelta(vertical));
    }
}
