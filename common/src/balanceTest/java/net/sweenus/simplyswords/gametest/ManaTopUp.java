package net.sweenus.simplyswords.gametest;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Ability mana costs would otherwise drain the harness player and silently block activations.
 */
public final class ManaTopUp {

    @FunctionalInterface
    public interface Refiller {
        void refill(ServerPlayerEntity player);
    }

    private static final Refiller NONE = player -> {
    };

    private static volatile Refiller active = NONE;

    private ManaTopUp() {
    }

    public static void register(Refiller refiller) {
        active = refiller == null ? NONE : refiller;
    }

    static void refill(ServerPlayerEntity player) {
        try {
            active.refill(player);
        } catch (RuntimeException | LinkageError ignored) {
            // A missing mana system is not a harness failure.
        }
    }
}
