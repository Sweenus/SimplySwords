package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ImbuedPower extends RunefusedGemPower {

	public ImbuedPower(boolean isGreater) {
		super(isGreater);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int hitChance = Config.gemPowers.imbued.chance;

		float fullValue = (this.isGreater() ? 10.0F : 6.0F)
				- ((stack.getDamage() / stack.getMaxDamage()) * 100) / 20.0F;
		float damage = HelperMethods.gemPowerScaledValue(SpellScalingProfile.ARCANE, attacker, stack,
				fullValue, Config.gemPowers.imbued.spellScaling);

		if (attacker.getRandom().nextInt(100) <= hitChance) {
			target.timeUntilRegen = 0;
			float hitDamage = HelperMethods.applyNonPlayerAbilityDamageModifier(attacker, damage);
			hitDamage = HelperMethods.applyNonPlayerWeaponHitDamageModifier(attacker, hitDamage);
			target.damage(attacker.getDamageSources().magic(), HelperMethods.applyWeaponAbilityDamageToPlayersModifier(target, hitDamage));
			attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
					attacker.getSoundCategory(), 0.2f, 1.8f);
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		if (isRunic)
			tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip1").setStyle(Styles.RUNIC));
		else
			tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.imbued").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
		TooltipUtils.appendSpellScaleTooltip(tooltip, "arcane");
	}

	public static class Settings extends TooltipSettings {

		public Settings() {
			super(GemPowerRegistry.IMBUED);
		}

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 0, max = 100)
		public int chance = 15;

		@ValidatedFloat.Restrict(min = 0f)
		public float spellScaling = 2.0f;
	}
}
