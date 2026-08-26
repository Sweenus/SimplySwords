package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.Mouse;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void simplyswords$trackIonBeamLookTiming(double timeDelta, CallbackInfo ci) {
        IonboundBeamClientState.updateLookTiming(timeDelta);
    }
}
