package net.sweenus.simplyswords.mixin;

import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.MoltenEdgeAbilityManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
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
        if (ShadowstingShadowDanceManager.isActive(player) || WaxweaverEncasementManager.isEncased(player)) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "onDisconnected")
    private void simplyswords$resetMoltenHeatOnDisconnect(Text reason, CallbackInfo ci) {
        MoltenEdgeAbilityManager.resetWielder(player);
    }
}
