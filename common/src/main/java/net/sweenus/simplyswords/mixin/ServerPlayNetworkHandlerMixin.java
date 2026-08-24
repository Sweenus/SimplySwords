package net.sweenus.simplyswords.mixin;

import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.IonboundStormscaleAbilityManager;
import net.sweenus.simplyswords.world.MoltenEdgeAbilityManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.world.PlayerMovementIntentManager;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "onHandSwing", cancellable = true)
    private void simplyswords$preventShadowDanceSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        if (ShadowstingShadowDanceManager.isActive(player) || WaxweaverEncasementManager.isEncased(player)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(player)) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "onPlayerAction", cancellable = true)
    private void simplyswords$preventIncapacitatedPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (IncapacitatingStatusEffectRegistry.isIncapacitated(player)) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onPlayerInteractEntity", cancellable = true)
    private void simplyswords$preventIncapacitatedEntityInteraction(PlayerInteractEntityC2SPacket packet,
                                                                    CallbackInfo ci) {
        if (IncapacitatingStatusEffectRegistry.isIncapacitated(player)) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onDisconnected")
    private void simplyswords$resetMoltenHeatOnDisconnect(DisconnectionInfo info, CallbackInfo ci) {
        IonboundStormscaleAbilityManager.flushWielder(player);
        MoltenEdgeAbilityManager.resetWielder(player);
        PlayerMovementIntentManager.clear(player);
    }
}
