package net.sweenus.simplyswords.power.powers;

import net.minecraft.entity.LivingEntity;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.effect.ImmolationEffect;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class RadiancePower extends NetherGemPower {

	public RadiancePower() {
		super(false);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
			attacker.addStatusEffect(ImmolationEffect.createScaledInstance(attacker, stack,
					AwakeningApi.scaleGemPowerDuration(stack, 200), 4,
					Config.gemPowers.radiance.spellScaling), attacker);
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance").setStyle(Styles.NETHERFUSED));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
		}
		TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
	}

	public static class Settings extends TooltipSettings {
		public Settings() {
			super(GemPowerRegistry.RADIANCE);
		}

		@ValidatedFloat.Restrict(min = 0f)
		public float spellScaling = 2.0f;
	}
}
