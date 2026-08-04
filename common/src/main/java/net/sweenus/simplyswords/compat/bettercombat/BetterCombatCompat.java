package net.sweenus.simplyswords.compat.bettercombat;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.world.RunicSlashManager;

import java.lang.reflect.Method;

/**
 * Bridges Better Combat attacks onto {@link SimplySwordsAPI#onWeaponSwing}.
 * <p>
 * Called from {@code BetterCombatServerNetworkMixin}, which hands us the {@code AttackHand}
 * Better Combat already resolved for the request. Better Combat is a soft dependency, so that
 * object is reflected rather than typed.
 */
public final class BetterCombatCompat {
    // The lambda BetterCombatServerNetworkMixin injects into. Kept in sync with that mixin.
    private static final String ATTACK_HANDLER_LAMBDA = "lambda$initializeHandlers$5";
    // The mixin's injector method, merged into ServerNetwork once the mixin applies.
    private static final String INJECTED_HANDLER = "simplyswords$triggerRunicSlashFromBetterCombat";

    private static Method attackHandIsOffHand;
    private static Method attackHandItemStack;
    private static boolean initialized;
    private static boolean reflectionUnavailableLogged;

    private BetterCombatCompat() {
    }

    //
    // Reports if the Better Combat attack hook is not in place, which is otherwise silent:
    // BetterCombatServerNetworkMixin uses require = 0 so it degrades to "no
    // on-swing effects under Better Combat" rather than crashing.
    //
    // Checks both failure modes separately - Better Combat's handler having moved, and the mixin
    // not having applied at all.
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
                targetPresent |= name.equals(ATTACK_HANDLER_LAMBDA);
                injected |= name.contains(INJECTED_HANDLER);
            }

            if (!targetPresent) {
                SimplySwords.LOGGER.warn(
                        "Better Combat is installed but {}#{} is missing, so on-swing weapon "
                                + "effects (runic slash, Livyatan waves) will not fire for Better "
                                + "Combat attacks. Its attack handler has most likely moved and "
                                + "BetterCombatServerNetworkMixin needs retargeting.",
                        serverNetwork.getName(), ATTACK_HANDLER_LAMBDA);
            } else if (!injected) {
                SimplySwords.LOGGER.warn(
                        "BetterCombatServerNetworkMixin did not apply to {}, so on-swing weapon "
                                + "effects (runic slash, Livyatan waves) will not fire for Better "
                                + "Combat attacks. The mixin was skipped before it could be "
                                + "applied - check for a 'Skipping virtual target' line, and note "
                                + "that mod-loaded gating in SimplySwordsCommonMixinPlugin cannot "
                                + "work on Forge.",
                        serverNetwork.getName());
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            SimplySwords.LOGGER.warn(
                    "Unable to inspect Better Combat's server attack handler; on-swing weapon "
                            + "effects may not fire for Better Combat attacks.", e);
        }
    }

    //
    // Fires on-swing weapon effects for a Better Combat attack.
    //
    // @param attackHand Better Combat's net.bettercombat.api.AttackHand for this attack,
    //                   used to tell a dual-wielded off-hand attack from a main-hand one.
    //
    public static void triggerRunicSlash(ServerPlayerEntity player, Object attackHand) {
        if (player == null
                || !(player.getWorld() instanceof ServerWorld world)
                || RunicSlashManager.isSuppressed()) {
            return;
        }

        Hand hand = resolveHand(attackHand);
        ItemStack stack = resolveStack(attackHand, player, hand);
        if (stack.isEmpty()) {
            return;
        }

        // Better Combat paces its own attacks, so bypass the vanilla attack-speed gate that the
        // swingHand path relies on - otherwise faster BC combos would drop swings.
        RunicSlashManager.runIgnoringAttackReady(
                () -> SimplySwordsAPI.onWeaponSwing(stack, world, player, hand));
    }

    private static Hand resolveHand(Object attackHand) {
        if (attackHand == null || !ensureReflectionReady()) {
            return Hand.MAIN_HAND;
        }
        try {
            return (boolean) attackHandIsOffHand.invoke(attackHand) ? Hand.OFF_HAND : Hand.MAIN_HAND;
        } catch (ReflectiveOperationException | RuntimeException e) {
            logReflectionUnavailable(e);
            return Hand.MAIN_HAND;
        }
    }

    private static ItemStack resolveStack(Object attackHand, ServerPlayerEntity player, Hand hand) {
        if (attackHand != null && ensureReflectionReady()) {
            try {
                if (attackHandItemStack.invoke(attackHand) instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                logReflectionUnavailable(e);
            }
        }
        return player.getStackInHand(hand);
    }

    private static boolean ensureReflectionReady() {
        if (initialized) {
            return attackHandIsOffHand != null;
        }
        initialized = true;

        try {
            Class<?> attackHandClass = Class.forName("net.bettercombat.api.AttackHand");
            attackHandIsOffHand = attackHandClass.getMethod("isOffHand");
            attackHandItemStack = attackHandClass.getMethod("itemStack");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            attackHandIsOffHand = null;
            attackHandItemStack = null;
            logReflectionUnavailable(e);
            return false;
        }
    }

    private static void logReflectionUnavailable(Exception e) {
        if (!reflectionUnavailableLogged) {
            reflectionUnavailableLogged = true;
            SimplySwords.LOGGER.warn("Unable to resolve the Better Combat attack hand; "
                    + "assuming main-hand attacks for on-swing weapon effects.", e);
        }
    }
}
