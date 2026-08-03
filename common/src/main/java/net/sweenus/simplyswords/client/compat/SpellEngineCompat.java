package net.sweenus.simplyswords.client.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.sweenus.simplyswords.client.AbilityKeybindHandler;

public final class SpellEngineCompat {

    private SpellEngineCompat() {
    }

    public static UseAction getExpectedUseAction(ItemStack stack) {
        UseAction original = stack.getUseAction();
        if (!AbilityKeybindHandler.isAbilityStack(stack)) {
            return original;
        }

        Hand hand = findHeldHand(stack);
        if (hand != null && AbilityKeybindHandler.isAbilityKeyRebound(hand)) {
            return UseAction.NONE;
        }

        return original == UseAction.NONE ? UseAction.BLOCK : original;
    }

    private static Hand findHeldHand(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return null;
        }
        for (Hand hand : Hand.values()) {
            if (client.player.getStackInHand(hand) == stack) {
                return hand;
            }
        }
        for (Hand hand : Hand.values()) {
            if (ItemStack.areEqual(client.player.getStackInHand(hand), stack)) {
                return hand;
            }
        }
        return null;
    }
}
