package net.sweenus.simplyswords.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.network.UseWeaponAbilityPacket;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import org.lwjgl.glfw.GLFW;

public final class AbilityKeybindHandler {

    private static final String CATEGORY = "key.categories.simplyswords";
    private static final KeyBinding MAINHAND_ABILITY = new KeyBinding(
            "key.simplyswords.mainhand_ability",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );
    private static final KeyBinding OFFHAND_ABILITY = new KeyBinding(
            "key.simplyswords.offhand_ability",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    private static boolean mainhandPressed;
    private static boolean offhandPressed;
    private static ItemStack mainhandActiveStack = ItemStack.EMPTY;
    private static ItemStack offhandActiveStack = ItemStack.EMPTY;

    private AbilityKeybindHandler() {
    }

    public static void init() {
        KeyMappingRegistry.register(MAINHAND_ABILITY);
        KeyMappingRegistry.register(OFFHAND_ABILITY);
        ClientTickEvent.CLIENT_POST.register(AbilityKeybindHandler::tick);
    }

    public static boolean shouldSuppressDefaultUse(ItemStack stack, Hand hand) {
        return isReboundAbilityStack(stack, hand);
    }

    public static boolean shouldSuppressVanillaStopUsing(PlayerEntity player) {
        if (player == null || !player.isUsingItem()) {
            return false;
        }

        Hand hand = player.getActiveHand();
        ItemStack activeStack = hand == Hand.MAIN_HAND ? mainhandActiveStack : offhandActiveStack;
        boolean keyPressed = hand == Hand.MAIN_HAND ? mainhandPressed : offhandPressed;
        return keyPressed
                && !activeStack.isEmpty()
                && isAbilityStack(activeStack)
                && ItemStack.areItemsEqual(player.getStackInHand(hand), activeStack);
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.currentScreen != null) {
            releaseIfNeeded(Hand.MAIN_HAND);
            releaseIfNeeded(Hand.OFF_HAND);
            return;
        }

        tickHand(Hand.MAIN_HAND, MAINHAND_ABILITY);
        tickHand(Hand.OFF_HAND, OFFHAND_ABILITY);
    }

    private static void tickHand(Hand hand, KeyBinding keyBinding) {
        boolean pressed = !keyBinding.isUnbound() && keyBinding.isPressed();
        boolean wasPressed = hand == Hand.MAIN_HAND ? mainhandPressed : offhandPressed;

        if (pressed && !wasPressed) {
            rememberActiveStack(MinecraftClient.getInstance(), hand);
            new UseWeaponAbilityPacket(hand, true).sendToServer();
        } else if (!pressed && wasPressed) {
            new UseWeaponAbilityPacket(hand, false).sendToServer();
            clearActiveStack(hand);
        }

        if (hand == Hand.MAIN_HAND) {
            mainhandPressed = pressed;
        } else {
            offhandPressed = pressed;
        }
    }

    private static void releaseIfNeeded(Hand hand) {
        if (hand == Hand.MAIN_HAND && mainhandPressed) {
            new UseWeaponAbilityPacket(hand, false).sendToServer();
            mainhandPressed = false;
            clearActiveStack(hand);
        } else if (hand == Hand.OFF_HAND && offhandPressed) {
            new UseWeaponAbilityPacket(hand, false).sendToServer();
            offhandPressed = false;
            clearActiveStack(hand);
        }
    }

    private static void rememberActiveStack(MinecraftClient client, Hand hand) {
        ItemStack stack = client != null && client.player != null ? client.player.getStackInHand(hand) : ItemStack.EMPTY;
        if (hand == Hand.MAIN_HAND) {
            mainhandActiveStack = stack.copy();
        } else {
            offhandActiveStack = stack.copy();
        }
    }

    private static void clearActiveStack(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            mainhandActiveStack = ItemStack.EMPTY;
        } else {
            offhandActiveStack = ItemStack.EMPTY;
        }
    }

    private static boolean isReboundAbilityStack(ItemStack stack, Hand hand) {
        KeyBinding keyBinding = hand == Hand.MAIN_HAND ? MAINHAND_ABILITY : OFFHAND_ABILITY;
        return !keyBinding.isUnbound() && isAbilityStack(stack);
    }

    public static boolean isAbilityStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof UniqueWeaponActiveAbility) {
            return true;
        }
        return stack.getItem() instanceof RunicSwordItem && !SimplySwordsAPI.getComponent(stack).isEmpty();
    }
}
