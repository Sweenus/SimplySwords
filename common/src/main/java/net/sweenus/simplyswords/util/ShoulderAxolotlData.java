package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class ShoulderAxolotlData {
    public static final String LEFT_PREFIX = "simplyswords_left_axolotl_";
    public static final String RIGHT_PREFIX = "simplyswords_right_axolotl_";

    private ShoulderAxolotlData() {
    }

    public static OptionalInt getLeftVariant(Entity entity) {
        return getVariant(entity, LEFT_PREFIX);
    }

    public static OptionalInt getRightVariant(Entity entity) {
        return getVariant(entity, RIGHT_PREFIX);
    }

    public static void setLeftVariant(PlayerEntity player, int variantId) {
        clearLeftVariant(player);
        player.addCommandTag(LEFT_PREFIX + normalizeVariant(variantId));
    }

    public static void setRightVariant(PlayerEntity player, int variantId) {
        clearRightVariant(player);
        player.addCommandTag(RIGHT_PREFIX + normalizeVariant(variantId));
    }

    public static void clearLeftVariant(PlayerEntity player) {
        clearVariants(player, LEFT_PREFIX);
    }

    public static void clearRightVariant(PlayerEntity player) {
        clearVariants(player, RIGHT_PREFIX);
    }

    public static int normalizeVariant(int variantId) {
        int max = AxolotlEntity.Variant.values().length;
        return Math.floorMod(variantId, max);
    }

    public static int toAxolotlVariantId(ParrotEntity.Variant variant) {
        if (variant == null) return 0;
        return normalizeVariant(variant.ordinal());
    }

    public static ParrotEntity.Variant toParrotVariant(int axolotlVariantId) {
        ParrotEntity.Variant[] variants = ParrotEntity.Variant.values();
        return variants[Math.floorMod(axolotlVariantId, variants.length)];
    }

    public static AxolotlEntity.Variant toAxolotlVariant(int variantId) {
        AxolotlEntity.Variant[] variants = AxolotlEntity.Variant.values();
        return variants[normalizeVariant(variantId) % variants.length];
    }

    private static OptionalInt getVariant(Entity entity, String prefix) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(prefix)) {
                String raw = tag.substring(prefix.length());
                try {
                    return OptionalInt.of(normalizeVariant(Integer.parseInt(raw)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return OptionalInt.empty();
    }

    private static void clearVariants(PlayerEntity player, String prefix) {
        List<String> tags = new ArrayList<>(player.getCommandTags());
        for (String tag : tags) {
            if (tag.startsWith(prefix)) {
                player.removeCommandTag(tag);
            }
        }
    }
}
