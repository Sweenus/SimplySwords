package net.sweenus.simplyswords.item;

import net.minecraft.item.Item;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bridges 1.21's data-component weapon attributes to the constructor-based
 * weapon attributes used by Minecraft 1.20.1.
 */
public final class LegacyWeaponAttributes {
    private static final Map<Item.Settings, Values> VALUES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private LegacyWeaponAttributes() {
    }

    public static Item.Settings configure(Item.Settings settings, int attackDamage, float attackSpeed) {
        VALUES.put(settings, new Values(attackDamage, attackSpeed));
        return settings;
    }

    public static int attackDamage(Item.Settings settings) {
        Values values = VALUES.get(settings);
        return values == null ? 3 : values.attackDamage();
    }

    public static float attackSpeed(Item.Settings settings) {
        Values values = VALUES.get(settings);
        return values == null ? -2.4F : values.attackSpeed();
    }

    private record Values(int attackDamage, float attackSpeed) {
    }
}
