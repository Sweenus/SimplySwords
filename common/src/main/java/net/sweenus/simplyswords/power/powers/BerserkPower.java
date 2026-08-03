package net.sweenus.simplyswords.power.powers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class BerserkPower extends NetherGemPower {

	public BerserkPower() {
		super(false);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		float damage = AwakeningApi.scaleGemPower(stack, HelperMethods.isUniqueTwohanded(stack) ? 4.0F : 2.0F);
		if (attacker.getArmor() < 10) {
			damage = HelperMethods.applyNonPlayerWeaponHitDamageModifier(attacker, damage);
			damage = HelperMethods.applyWeaponAbilityDamageToPlayersModifier(target, damage);
			target.setHealth(target.getHealth() - damage);
			attacker.heal(damage / 2.0F);
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk").setStyle(Styles.NETHERFUSED));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
		}
	}
}
