package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.Mouse;
import net.minecraft.client.util.GlfwUtil;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow private double lastMouseUpdateTime;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void simplyswords$trackIonBeamLookTiming(CallbackInfo ci) {
        IonboundBeamClientState.updateLookTiming(GlfwUtil.getTime() - lastMouseUpdateTime);
    }
}
