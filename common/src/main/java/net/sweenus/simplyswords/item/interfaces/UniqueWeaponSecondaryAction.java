package net.sweenus.simplyswords.item.interfaces;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public interface UniqueWeaponSecondaryAction {
    TypedActionResult<ItemStack> startPlayerSecondaryAbility(World world, PlayerEntity user, Hand hand);
}
