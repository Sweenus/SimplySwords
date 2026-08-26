package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityLookMixin {

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double simplyswords$scaleIonBeamLookX(double cursorDeltaX) {
        return simplyswords$isIonBeamOwner()
                ? IonboundBeamClientState.scaleLookDeltaX(cursorDeltaX)
                : cursorDeltaX;
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double simplyswords$scaleIonBeamLookY(double cursorDeltaY) {
        return simplyswords$isIonBeamOwner()
                ? IonboundBeamClientState.scaleLookDeltaY(cursorDeltaY)
                : cursorDeltaY;
    }

    @Unique
    private boolean simplyswords$isIonBeamOwner() {
        return IonboundBeamClientState.isOwnedBeamActive()
                && (Object) this == MinecraftClient.getInstance().player;
    }
}
