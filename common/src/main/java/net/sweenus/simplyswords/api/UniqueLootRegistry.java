package net.sweenus.simplyswords.api;

import net.minecraft.item.Item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

//
// Addon-facing registry for adding unique weapons to Simply Swords' pity-controlled loot pool.
//
public final class UniqueLootRegistry {
    private static final Map<Item, Integer> ENTRIES = new IdentityHashMap<>();

    private UniqueLootRegistry() {
    }

    public static void register(Item item) {
        register(item, 1);
    }

    public static void register(Item item, int weight) {
        ENTRIES.put(item, Math.max(1, weight));
    }

    public static Map<Item, Integer> entries() {
        return Collections.unmodifiableMap(ENTRIES);
    }
}
