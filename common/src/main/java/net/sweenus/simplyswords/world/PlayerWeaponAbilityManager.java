package net.sweenus.simplyswords.world;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.power.GemPowerComponent;

public final class PlayerWeaponAbilityManager {

    private PlayerWeaponAbilityManager() {
    }

    public static void handleInput(ServerPlayerEntity player, Hand hand, boolean pressed) {
        if (player == null || hand == null || !player.isAlive()) {
            return;
        }
        if (pressed) {
            start(player, hand);
        } else {
            stop(player, hand);
        }
    }

    public static boolean start(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!canUse(player, stack)) {
            return false;
        }

        if (stack.getItem() instanceof UniqueWeaponActiveAbility) {
            TypedActionResult<ItemStack> result = ((UniqueWeaponActiveAbility) stack.getItem()).startPlayerAbility(player.getWorld(), player, hand);
            if (result.getResult().isAccepted()) {
                PlayerWeaponAbilityChannelManager.start(player, hand, player.getStackInHand(hand));
                return true;
            }
            return false;
        }

        if (stack.getItem() instanceof RunicSwordItem) {
            GemPowerComponent component = SimplySwordsAPI.getComponent(stack);
            if (!component.isEmpty()) {
                TypedActionResult<ItemStack> result = component.use(player.getWorld(), player, hand);
                if (result.getResult().isAccepted()) {
                    PlayerWeaponAbilityChannelManager.start(player, hand, player.getStackInHand(hand));
                    player.swingHand(hand, true);
                    return true;
                }
            }
        }

        return false;
    }

    public static void stop(ServerPlayerEntity player, Hand hand) {
        if (PlayerWeaponAbilityChannelManager.stop(player, hand)) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isEmpty() && player.isUsingItem() && player.getActiveHand() == hand) {
            stack.onStoppedUsing(player.getWorld(), player, player.getItemUseTimeLeft());
            player.clearActiveItem();
        }
    }

    private static boolean canUse(ServerPlayerEntity player, ItemStack stack) {
        return player != null
                && player.isAlive()
                && stack != null
                && !stack.isEmpty()
                && stack.getDamage() < stack.getMaxDamage() - 1
                && !player.getItemCooldownManager().isCoolingDown(stack.getItem());
    }

}
