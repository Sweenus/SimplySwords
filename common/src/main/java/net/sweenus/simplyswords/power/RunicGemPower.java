package net.sweenus.simplyswords.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class RunicGemPower extends GemPower {

	public RunicGemPower(boolean isGreater) {
		super(isGreater, PowerType.RUNIC);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand, ItemStack itemStack) { return ActionResult.FAIL; }
	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {}
	@Override
	public int getMaxUseTime(ItemStack stack) { return 0; }
	@Override
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) { return false; }
}
