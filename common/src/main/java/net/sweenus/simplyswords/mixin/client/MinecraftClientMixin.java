package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.client.AbilityKeybindHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Redirect(method = "handleInputEvents",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    private void simplyswords$suppressReboundAbilityStopUsing(ClientPlayerInteractionManager interactionManager, PlayerEntity player) {
        if (!AbilityKeybindHandler.shouldSuppressVanillaStopUsing(player)) {
            interactionManager.stopUsingItem(player);
        }
    }
}
