package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
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
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponSecondaryAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(method = "interactItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ItemCooldownManager;isCoolingDown(Lnet/minecraft/item/Item;)Z"))
    private boolean simplyswords$allowSecondaryActionDuringCooldown(ItemCooldownManager cooldownManager,
                                                                     Item item,
                                                                     ServerPlayerEntity player,
                                                                     World world,
                                                                     ItemStack stack,
                                                                     Hand hand) {
        return simplyswords$shouldBlockItemUse(cooldownManager.isCoolingDown(item),
                stack.getItem() instanceof UniqueWeaponSecondaryAction);
    }

    @Unique
    private static boolean simplyswords$shouldBlockItemUse(boolean coolingDown, boolean hasSecondaryAction) {
        return coolingDown && !hasSecondaryAction;
    }

    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void simplyswords$preventShadowDanceBlockUse(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (ShadowstingShadowDanceManager.isActive(player) || WaxweaverEncasementManager.isEncased(player)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
