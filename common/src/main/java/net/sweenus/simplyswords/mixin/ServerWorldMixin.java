package net.sweenus.simplyswords.mixin;

import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.effect.FlameSeedEffect;
import net.sweenus.simplyswords.world.EmberlashSmoulderVisualManager;
import net.sweenus.simplyswords.world.FlamewindVisualManager;
import net.sweenus.simplyswords.world.FrostfallIceSpikeFieldManager;
import net.sweenus.simplyswords.world.ImplicitStatusVisualManager;
import net.sweenus.simplyswords.world.RevivalCandleVisualManager;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.SoulkeeperLanternManager;
import net.sweenus.simplyswords.world.SoulrenderMarkVisualManager;
import net.sweenus.simplyswords.world.WhisperwindVisualManager;
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
        if (FlamewindVisualManager.hasActive(world)) {
            FlamewindVisualManager.tick(world);
        }
        if (FlameSeedEffect.hasPendingDeathDetonations(world)) {
            FlameSeedEffect.tickPendingDeathDetonations(world);
        }
        if (WhisperwindVisualManager.hasActive(world)) {
            WhisperwindVisualManager.tick(world);
        }
        if (EmberlashSmoulderVisualManager.hasActive(world)) {
            EmberlashSmoulderVisualManager.tick(world);
        }
        if (ImplicitStatusVisualManager.hasActive(world)) {
            ImplicitStatusVisualManager.tick(world);
        }
        if (ShadowstingShadowDanceManager.hasPendingCloneStrikes(world)) {
            ShadowstingShadowDanceManager.tickCloneStrikes(world);
        }
        RevivalCandleVisualManager.tickWorld(world);
        SoulkeeperLanternManager.tickWorld(world);
    }
}
