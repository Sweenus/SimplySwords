package net.sweenus.simplyswords.mixin.compat;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.compat.bettercombat.BetterCombatCompat;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//
// Hooks Better Combat's server-side attack handler so runic slash and other on-swing weapon
// effects fire for BC attacks.
//
// Better Combat cancels vanilla MinecraftClient.doAttack(), so no hand-swing packet is
// ever sent and {@code LivingEntity.swingHand} is never reached on the server. Every on-swing
// effect has to be driven off BC's own attack request instead.
//
// Unlike newer builds, Better Combat 1.20.1 (1.8.6 / 1.9.0) has no named
// handleAttackRequest: the per-attack-request body is inlined into
// initializeHandlers() as {@code lambda$initializeHandlers$5}. That lambda has the same
// signature and index in both the Fabric and Forge builds of 1.8.6 and 1.9.0, and runs once per
// request before the target-count check, so it fires on misses too.
//
// require = 0 keeps a future Better Combat build that shifts the lambda index from
// turning into a hard mixin failure; BetterCombatCompat#warnIfNeverHooked() reports the
// degraded state instead.
//
@Pseudo
@Mixin(targets = "net.bettercombat.network.ServerNetwork", remap = false)
public abstract class BetterCombatServerNetworkMixin {

    @Inject(
            method = "lambda$initializeHandlers$5",
            at = @At("HEAD"),
            remap = false,
            cancellable = true,
            require = 0
    )
    private static void simplyswords$triggerRunicSlashFromBetterCombat(
            ServerPlayerEntity player,
            @Coerce Object request,
            @Coerce Object attributes,
            @Coerce Object attack,
            @Coerce Object hand,
            ServerWorld world,
            boolean dualWielding,
            ServerPlayNetworkHandler networkHandler,
            CallbackInfo ci) {
        if (WaxweaverEncasementManager.isEncased(player)) {
            ci.cancel();
            return;
        }
        BetterCombatCompat.triggerRunicSlash(player, hand);
    }
}
