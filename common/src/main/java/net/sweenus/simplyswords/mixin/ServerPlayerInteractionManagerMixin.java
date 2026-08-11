package net.sweenus.simplyswords.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {

    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    private void simplyswords$preventShadowDanceItemUse(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (ShadowstingShadowDanceManager.isActive(player) || WaxweaverEncasementManager.isEncased(player)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void simplyswords$preventShadowDanceBlockUse(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (ShadowstingShadowDanceManager.isActive(player) || WaxweaverEncasementManager.isEncased(player)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
