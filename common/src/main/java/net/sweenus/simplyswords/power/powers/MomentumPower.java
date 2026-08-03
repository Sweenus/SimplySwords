package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunicGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class MomentumPower extends RunicGemPower {

	public MomentumPower(boolean isGreater) {
		super(isGreater);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand, ItemStack itemStack) {

		if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
			return TypedActionResult.fail(itemStack);
		}
		world.playSoundFromEntity(user, user, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_FLYBY_01.get(),
				user.getSoundCategory(), 0.3f, 0.7f);
		user.setCurrentHand(hand);
		return TypedActionResult.consume(itemStack);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		int skillCooldown = Config.gemPowers.momentum.cooldown;

		if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
			//Player dash forward
			int velocity = 3;
			if (!user.isOnGround()) {velocity = 1;}
			if (remainingUseTicks >= 10 && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
				user.setVelocity(user.getRotationVector().multiply(velocity + (this.isGreater() ? 1 : 0)));
				user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent player flying to the heavens
				user.velocityModified = true;
				if (user instanceof PlayerEntity player) {
					player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
				}
			}
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
			user.setVelocity(0, 0, 0); // Stop player at end of charge
			user.velocityModified = true;
		}
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 15;
	}

	@Override
	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {

		tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip1").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.momentumsworditem.tooltip2").setStyle(Styles.RUNIC_DESCRIPTION)));
		}
	}



	public static class Settings extends TooltipSettings {

		public Settings() {
			super(GemPowerRegistry.MOMENTUM);
		}

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 1)
		public int cooldown = 140;
	}
}
