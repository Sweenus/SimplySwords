package net.sweenus.simplyswords.power.powers;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.effect.ImmolationEffect;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.power.RunicGemPower;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ImmolationPower extends RunicGemPower {

	public ImmolationPower() {
		super(false);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand, ItemStack itemStack) {
		user.setCurrentHand(hand);
		user.addStatusEffect(ImmolationEffect.createScaledInstance(user, itemStack,
				AwakeningApi.scaleGemPowerDuration(itemStack, 800), 3,
				Config.gemPowers.immolation.spellScaling,
				SpellScalingComponents.power("immolation")), user);
		SimplySwordsAPI.setWeaponCooldown(user, itemStack, 40);
		world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
				user.getSoundCategory(), 0.3f, 0.6f);
		return TypedActionResult.consume(itemStack);
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 1;
	}

	@Override
	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip1").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.immolationsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
        TooltipUtils.appendGemPowerSpellScaleTooltip(tooltip, "immolation");
	}

	public static class Settings extends TooltipSettings {
		public Settings() {
			super(GemPowerRegistry.IMMOLATION);
		}

		@ValidatedFloat.Restrict(min = 0f)
		public float spellScaling = 2.0f;
	}
}
