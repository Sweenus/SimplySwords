package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class UnstablePower extends RunefusedGemPower {

	public UnstablePower() {
		super(false);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
		int duration = AwakeningApi.scaleGemPowerDuration(stack, Config.gemPowers.unstable.duration);
		int frequency = Config.gemPowers.unstable.frequency;

		if (user.age % frequency == 0) {
			int random = (int) (Math.random() * 100);
			if (random < 10)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration));
			else if (random < 20)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration));
			else if (random < 30)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration));
			else if (random < 40)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration));
			else if (random < 50)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration));
			else if (random < 60)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration));
			else if (random < 70)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration));
			else if (random < 80)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration));
			else if (random < 90)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration));
			else if (random < 95)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, duration));
			else if (random < 100)
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration));
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
		if (isRunic)
			tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip1").setStyle(Styles.RUNIC));
		else
			tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.unstable").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.translatable("").append(Text.translatable("item.simplyswords.unstablesworditem.tooltip2").setStyle(Styles.RUNIC_DESCRIPTION)));
		}
	}

	public static class Settings extends TooltipSettings {

		public Settings() {
			super(GemPowerRegistry.UNSTABLE);
		}

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 1)
		public int frequency = 140;

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 0)
		public int duration = 140;
	}

}
