package net.sweenus.simplyswords.power.powers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ChanceDurationSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ZephyrPower extends RunefusedGemPower {

	public ZephyrPower(boolean isGreater) {
		super(isGreater);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int hitChance = Config.gemPowers.zephyr.chance;
		int duration = Config.gemPowers.zephyr.duration;

		if (attacker.getRandom().nextInt(100) <= hitChance) {
			attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, this.isGreater() ? 1 : 0), attacker);
			attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, this.isGreater() ? 1 : 0), attacker);
			attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
					attacker.getSoundCategory(), 0.1f, 1.8f);
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
		if (isRunic)
			tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip1").setStyle(Styles.RUNIC));
		else
			tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.zephyr").setStyle(Styles.RUNIC));

		tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
		tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
	}

	public static class Settings extends ChanceDurationSettings {

		public Settings() {
			super(15, 180, GemPowerRegistry.ZEPHYR);
		}
	}
}