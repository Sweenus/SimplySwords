package net.sweenus.simplyswords.loot;

import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;

//
// Per-thread handoff used while a lootable inventory expands its loot table.
//
public final class PityLootContext {
    private static final ThreadLocal<RegistryKey<LootTable>> CAPTURED_TABLE = new ThreadLocal<>();

    private PityLootContext() {
    }

    public static void capture(RegistryKey<LootTable> table) {
        if (table == null) {
            CAPTURED_TABLE.remove();
        } else {
            CAPTURED_TABLE.set(table);
        }
    }

    public static RegistryKey<LootTable> take() {
        RegistryKey<LootTable> table = CAPTURED_TABLE.get();
        CAPTURED_TABLE.remove();
        return table;
    }
}
