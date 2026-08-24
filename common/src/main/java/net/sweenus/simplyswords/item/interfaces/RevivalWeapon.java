package net.sweenus.simplyswords.item.interfaces;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface RevivalWeapon {
    boolean canRevive(LivingEntity entity, ItemStack stack, DamageSource source);
    void postRevive(LivingEntity entity, ItemStack stack, DamageSource source);
    float getReviveHealth(LivingEntity entity, ItemStack stack, DamageSource source);
}
