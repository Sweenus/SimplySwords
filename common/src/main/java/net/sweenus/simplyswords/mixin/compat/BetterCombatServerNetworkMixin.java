package net.sweenus.simplyswords.mixin.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.compat.bettercombat.BetterCombatCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.bettercombat.network.ServerNetwork", remap = false)
public abstract class BetterCombatServerNetworkMixin {

    @Inject(method = "handleAttackRequest", at = @At("HEAD"), remap = false)
    private static void simplyswords$triggerRunicSlashFromBetterCombat(
            @Coerce Object request,
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler networkHandler,
            CallbackInfo ci) {
        BetterCombatCompat.triggerRunicSlash(request, player);
    }
}
