package net.sweenus.simplyswords.item.interfaces;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface RevivalWeapon {
    boolean canRevive(PlayerEntity entity, ItemStack stack, DamageSource source);
    void postRevive(PlayerEntity entity, ItemStack stack, DamageSource source);
    float getReviveHealth(PlayerEntity entity, ItemStack stack, DamageSource source);
}
