package net.sweenus.simplyswords.mixin;

import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.world.FrostfallIceSpikeFieldManager;
import net.sweenus.simplyswords.world.SoulrenderMarkVisualManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void simplyswords$tickFieldManagers(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;
        if (FrostfallIceSpikeFieldManager.hasActive(world)) {
            FrostfallIceSpikeFieldManager.tick(world);
        }
        if (SoulrenderMarkVisualManager.hasActive(world)) {
            SoulrenderMarkVisualManager.tick(world);
        }
    }
}
