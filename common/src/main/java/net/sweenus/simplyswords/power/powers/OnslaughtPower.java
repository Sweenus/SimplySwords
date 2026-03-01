package net.sweenus.simplyswords.power.powers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class OnslaughtPower extends NetherGemPower {

	public OnslaughtPower() {
		super(false);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (target.hasStatusEffect(StatusEffects.SLOWNESS) && !attacker.hasStatusEffect(StatusEffects.WEAKNESS)) {
			attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.ONSLAUGHT), 80, 0), attacker);
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught").setStyle(Styles.NETHERFUSED));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
		}
	}
}
