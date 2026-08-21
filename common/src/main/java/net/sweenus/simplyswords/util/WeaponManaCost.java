package net.sweenus.simplyswords.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.config.Config;

public final class WeaponManaCost {
    private WeaponManaCost() {
    }

    public static int of(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !SimplySwordsExpectPlatform.hasManaSystem()) {
            return 0;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        Integer cost = Config.uniqueEffects.weaponManaCosts.get(id);
        return cost == null ? 0 : Math.max(0, cost);
    }

    public static boolean canAfford(LivingEntity user, ItemStack stack) {
        return SimplySwordsExpectPlatform.hasMana(user, of(stack));
    }

    public static void spend(LivingEntity user, ItemStack stack) {
        SimplySwordsExpectPlatform.spendMana(user, of(stack));
    }
}
