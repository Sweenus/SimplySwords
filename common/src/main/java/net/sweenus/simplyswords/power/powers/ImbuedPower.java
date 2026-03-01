package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ImbuedPower extends RunefusedGemPower {

	public ImbuedPower(boolean isGreater) {
		super(isGreater);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		int hitChance = Config.gemPowers.imbued.chance;

		int damage = (this.isGreater() ? 10 : 6) - ((stack.getDamage() / stack.getMaxDamage()) * 100) / 20;

		if (attacker.getRandom().nextInt(100) <= hitChance) {
			target.timeUntilRegen = 0;
			target.damage(attacker.getDamageSources().magic(), damage);
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
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
	}

	public static class Settings extends TooltipSettings {

		public Settings() {
			super(GemPowerRegistry.IMBUED);
		}

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 0, max = 100)
		public int chance = 15;
	}
}
