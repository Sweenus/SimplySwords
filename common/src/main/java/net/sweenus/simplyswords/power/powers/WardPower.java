package net.sweenus.simplyswords.power.powers;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.power.RunicGemPower;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class WardPower extends RunicGemPower {

	public WardPower() {
		super(false);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand, ItemStack itemStack) {
		user.setCurrentHand(hand);
		user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.WARD), 120, 0), user);
		user.getItemCooldownManager().set(itemStack.getItem(), 120);
		user.setHealth(user.getHealth() / 2);
		world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
				user.getSoundCategory(), 0.3f, 1.2f);
		return TypedActionResult.consume(itemStack);
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 1;
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip1").setStyle(Styles.RUNIC));
		tooltip.add(Text.literal(""));
		tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
		if (Screen.hasAltDown()) {
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.wardsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.wardsworditem.tooltip3")).setStyle(Styles.RUNIC_DESCRIPTION));
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.wardsworditem.tooltip4")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
	}
}