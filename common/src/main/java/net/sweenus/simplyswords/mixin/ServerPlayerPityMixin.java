package net.sweenus.simplyswords.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.loot.PityStateHolder;
import net.sweenus.simplyswords.loot.PlayerPityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerPityMixin implements PityStateHolder {
    @Unique
    private PlayerPityState simplyswords$pityState = new PlayerPityState();

    @Override
    public PlayerPityState simplyswords$getPityState() {
        return simplyswords$pityState;
    }

    @Override
    public void simplyswords$setPityState(PlayerPityState state) {
        simplyswords$pityState = state == null ? new PlayerPityState() : state;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void simplyswords$writePity(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("SimplySwordsPity", simplyswords$pityState.writeNbt());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void simplyswords$readPity(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("SimplySwordsPity")) {
            simplyswords$pityState = PlayerPityState.readNbt(nbt.getCompound("SimplySwordsPity"));
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void simplyswords$copyPity(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        simplyswords$pityState = ((PityStateHolder) oldPlayer).simplyswords$getPityState().copy();
    }
}
