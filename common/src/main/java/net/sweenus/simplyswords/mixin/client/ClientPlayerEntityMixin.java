package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import net.sweenus.simplyswords.world.GloamMechanicsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void simplyswords$clearIncapacitatedInput(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if ((!IncapacitatingStatusEffectRegistry.isIncapacitated(player)
                && !GloamMechanicsManager.isGrasped(player)) || player.input == null) return;
        player.input.movementForward = 0.0F;
        player.input.movementSideways = 0.0F;
        player.input.pressingForward = false;
        player.input.pressingBack = false;
        player.input.pressingLeft = false;
        player.input.pressingRight = false;
        player.input.jumping = false;
    }
}
