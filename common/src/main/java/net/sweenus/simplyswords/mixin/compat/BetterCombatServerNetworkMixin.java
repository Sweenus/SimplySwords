package net.sweenus.simplyswords.mixin.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.compat.bettercombat.BetterCombatCompat;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.bettercombat.network.ServerNetwork", remap = false)
public abstract class BetterCombatServerNetworkMixin {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "handleAttackRequest", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;executeSync(Ljava/lang/Runnable;)V", remap = true))
    private static void simplyswords$wraithfangAttack(MinecraftServer server, Runnable action,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ServerPlayerEntity player) {
        original.call(server, (Runnable) () -> {
            net.sweenus.simplyswords.world.WraithfangAbilityManager.beginAttack(player);
            net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.beginAttack(player);
            try { action.run(); }
            finally {
                net.sweenus.simplyswords.world.LongPathFinalFormsMasteryCombatManager.endAttack();
                net.sweenus.simplyswords.world.WraithfangAbilityManager.endAttack();
            }
        });
    }

    @Inject(method = "handleAttackRequest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void simplyswords$triggerRunicSlashFromBetterCombat(
            @Coerce Object request,
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler networkHandler,
            CallbackInfo ci) {
        if (WaxweaverEncasementManager.isEncased(player)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(player)) {
            ci.cancel();
            return;
        }
        BetterCombatCompat.triggerRunicSlash(request, player);
    }
}
