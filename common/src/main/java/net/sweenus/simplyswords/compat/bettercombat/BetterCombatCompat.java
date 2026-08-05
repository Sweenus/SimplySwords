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
    // The Better Combat handler BetterCombatServerNetworkMixin injects into. Kept in sync with that mixin.
    private static final String ATTACK_HANDLER = "handleAttackRequest";
    // The mixin's injector method, merged into ServerNetwork once the mixin applies.
    private static final String INJECTED_HANDLER = "simplyswords$triggerRunicSlashFromBetterCombat";

    private static Method getCurrentAttackMethod;
    private static Method isOffHandMethod;
    private static Method itemStackMethod;
    private static Method comboCountMethod;
    private static boolean initialized;
    private static boolean reflectionUnavailableLogged;

    private BetterCombatCompat() {
    }

    //
    // Reports if the Better Combat attack hook is not in place. A failed injection crashes on its
    // own, but a mixin that gets skipped before it can be applied is completely silent - that is
    // the failure this catches.
    //
    // Checks both modes separately - Better Combat's handler having moved, and the mixin not
    // having applied at all.
    //
    public static void verifyAttackHookTarget() {
        try {
            // Loaded without initializing so this doesn't run Better Combat's static setup early.
            Class<?> serverNetwork = Class.forName(
                    "net.bettercombat.network.ServerNetwork", false,
                    BetterCombatCompat.class.getClassLoader());

            boolean targetPresent = false;
            boolean injected = false;
            for (Method method : serverNetwork.getDeclaredMethods()) {
                String name = method.getName();
                targetPresent |= name.equals(ATTACK_HANDLER);
                injected |= name.contains(INJECTED_HANDLER);
            }

            if (!targetPresent) {
                SimplySwords.LOGGER.warn(
                        "Better Combat is installed but {}#{} is missing, so on-swing weapon "
                                + "effects (runic slash, Livyatan waves) will not fire for Better "
                                + "Combat attacks. Its attack handler has most likely moved and "
                                + "BetterCombatServerNetworkMixin needs retargeting.",
                        serverNetwork.getName(), ATTACK_HANDLER);
            } else if (!injected) {
                SimplySwords.LOGGER.warn(
                        "BetterCombatServerNetworkMixin did not apply to {}, so on-swing weapon "
                                + "effects (runic slash, Livyatan waves) will not fire for Better "
                                + "Combat attacks. The mixin was skipped before it could be "
                                + "applied - check for a 'Skipping virtual target' line, and note "
                                + "that mod-loaded gating in SimplySwordsCommonMixinPlugin cannot "
                                + "work on NeoForge.",
                        serverNetwork.getName());
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            SimplySwords.LOGGER.warn(
                    "Unable to inspect Better Combat's server attack handler; on-swing weapon "
                            + "effects may not fire for Better Combat attacks.", e);
        }
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
