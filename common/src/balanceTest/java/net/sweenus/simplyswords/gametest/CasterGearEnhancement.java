package net.sweenus.simplyswords.gametest;

import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.SpellScalingProfile;

public final class CasterGearEnhancement {

    @FunctionalInterface
    public interface Enhancer {
        String enhance(ServerWorld world, ItemStack armour, SpellScalingProfile profile);
    }

    private static final Enhancer NONE = (world, armour, profile) -> "";

    private static volatile Enhancer active = NONE;

    private CasterGearEnhancement() {
    }

    public static void register(Enhancer enhancer) {
        active = enhancer == null ? NONE : enhancer;
    }

    static String enhance(ServerWorld world, ItemStack armour, SpellScalingProfile profile) {
        try {
            return active.enhance(world, armour, profile);
        } catch (RuntimeException | LinkageError failure) {
            return "enhancer failed: " + failure.getClass().getSimpleName();
        }
    }
}
