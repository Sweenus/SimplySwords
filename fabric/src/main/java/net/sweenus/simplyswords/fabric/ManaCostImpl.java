package net.sweenus.simplyswords.fabric;

import net.minecraft.entity.LivingEntity;

public final class ManaCostImpl {
    private ManaCostImpl() {
    }

    public static boolean hasManaSystem() {
        return false;
    }

    public static boolean hasMana(LivingEntity entity, float amount) {
        return true;
    }

    public static void spendMana(LivingEntity entity, float amount) {
    }
}
