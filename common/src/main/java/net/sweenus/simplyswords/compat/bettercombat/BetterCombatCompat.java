package net.sweenus.simplyswords.compat.bettercombat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.world.RunicSlashManager;

import java.lang.reflect.Method;

public final class BetterCombatCompat {
    private static Method getCurrentAttackMethod;
    private static Method isOffHandMethod;
    private static Method itemStackMethod;
    private static Method comboCountMethod;
    private static boolean initialized;
    private static boolean reflectionUnavailableLogged;

    private BetterCombatCompat() {
    }

    public static void triggerRunicSlash(Object request, ServerPlayerEntity player) {
        if (request == null
                || player == null
                || !(player.getWorld() instanceof ServerWorld world)
                || RunicSlashManager.isSuppressed()) {
            return;
        }

        AttackData attackData = resolveAttackData(request, player);
        if (attackData == null) {
            return;
        }

        ItemStack stack = attackData.stack();
        Hand hand = attackData.offHand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (stack == null || stack.isEmpty()) {
            stack = player.getStackInHand(hand);
        }
        if (!stack.isEmpty()) {
            ItemStack finalStack = stack;
            RunicSlashManager.runIgnoringAttackReady(() -> SimplySwordsAPI.onWeaponSwing(finalStack, world, player, hand));
        }
    }

    private static AttackData resolveAttackData(Object request, ServerPlayerEntity player) {
        if (!ensureReflectionReady(request)) {
            return fallbackMainHand(player);
        }

        try {
            int comboCount = (int) comboCountMethod.invoke(request);
            Object attackHand = getCurrentAttackMethod.invoke(null, player, comboCount);
            if (attackHand == null) {
                return null;
            }

            boolean offHand = (boolean) isOffHandMethod.invoke(attackHand);
            Object stack = itemStackMethod.invoke(attackHand);
            return stack instanceof ItemStack itemStack
                    ? new AttackData(offHand, itemStack)
                    : new AttackData(offHand, player.getStackInHand(offHand ? Hand.OFF_HAND : Hand.MAIN_HAND));
        } catch (ReflectiveOperationException | RuntimeException e) {
            logReflectionUnavailable(e);
            return fallbackMainHand(player);
        }
    }

    private static AttackData fallbackMainHand(ServerPlayerEntity player) {
        return new AttackData(false, player.getMainHandStack());
    }

    private static boolean ensureReflectionReady(Object request) {
        if (initialized) {
            return true;
        }

        try {
            Class<?> helperClass = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
            getCurrentAttackMethod = helperClass.getMethod("getCurrentAttack", PlayerEntity.class, int.class);
            comboCountMethod = request.getClass().getMethod("comboCount");

            Class<?> attackHandClass = Class.forName("net.bettercombat.api.AttackHand");
            isOffHandMethod = attackHandClass.getMethod("isOffHand");
            itemStackMethod = attackHandClass.getMethod("itemStack");
            initialized = true;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            logReflectionUnavailable(e);
            return false;
        }
    }

    private static void logReflectionUnavailable(Exception e) {
        if (!reflectionUnavailableLogged) {
            reflectionUnavailableLogged = true;
            SimplySwords.LOGGER.warn("Unable to resolve Better Combat attack hand for Runic Slash compatibility; falling back to main hand.", e);
        }
    }

    private record AttackData(boolean offHand, ItemStack stack) {
    }
}
