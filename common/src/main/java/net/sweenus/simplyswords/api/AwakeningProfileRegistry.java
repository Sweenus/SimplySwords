package net.sweenus.simplyswords.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.item.UniqueWeaponItem;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

//
// Registry used by addons to opt custom unique weapons into awakening and the Runic Forge.
//
public final class AwakeningProfileRegistry {
    private static final Map<Item, AwakeningProfile> PROFILES = new IdentityHashMap<>();

    private AwakeningProfileRegistry() {
    }

    public static void register(Item item, AwakeningProfile profile) {
        PROFILES.put(item, profile);
    }

    public static Optional<AwakeningProfile> get(Item item) {
        AwakeningProfile registered = PROFILES.get(item);
        if (registered != null) {
            return Optional.of(registered);
        }
        return item instanceof UniqueWeaponItem ? Optional.of(AwakeningProfile.DEFAULT) : Optional.empty();
    }

    public static Optional<AwakeningProfile> get(ItemStack stack) {
        return stack == null || stack.isEmpty() ? Optional.empty() : get(stack.getItem());
    }

    public static boolean isAwakenable(ItemStack stack) {
        return get(stack).isPresent();
    }
}
