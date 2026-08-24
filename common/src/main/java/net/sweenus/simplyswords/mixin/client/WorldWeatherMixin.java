package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.LocalStormVisualManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldWeatherMixin {

    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void simplyswords$applyLocalStormRain(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), LocalStormVisualManager.intensity()));
        }
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void simplyswords$applyLocalStormThunder(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), LocalStormVisualManager.intensity()));
        }
    }
}
