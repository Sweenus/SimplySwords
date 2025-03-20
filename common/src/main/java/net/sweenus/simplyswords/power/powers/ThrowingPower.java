package net.sweenus.simplyswords.power.powers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.entity.ThrownRunicEntity;
import net.sweenus.simplyswords.power.RunicGemPower;
import net.sweenus.simplyswords.registry.TagRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ThrowingPower extends RunicGemPower {

	public ThrowingPower() {
		super(false);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand, ItemStack itemStack) {
		if (!world.isClient) {
			if (TagRegistry.isInTag(TagRegistry.spearsTag, itemStack.getItem())) return TypedActionResult.fail(itemStack);

			itemStack = user.getStackInHand(hand);
			ThrownRunicEntity thrownSwordEntity = new ThrownRunicEntity(world, user, itemStack.copy() );
			thrownSwordEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
			thrownSwordEntity.setYaw(user.getYaw());
			thrownSwordEntity.setPitch(user.getPitch());
			double[] doubles = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
			thrownSwordEntity.primaryBaseDamage = (float) doubles[0];
			float weight = 0.010f;
			if (TagRegistry.isInTag(TagRegistry.lightWeaponsTag, itemStack.getItem()))
				weight = 0.09f;
			else if (TagRegistry.isInTag(TagRegistry.mediumWeaponsTag, itemStack.getItem()))
				weight = 0.13f;
			else if (TagRegistry.isInTag(TagRegistry.heavyWeaponsTag, itemStack.getItem()))
				weight = 0.20f;
			thrownSwordEntity.weightValue = weight;
			if (hand == Hand.OFF_HAND)
				thrownSwordEntity.offhandThrow = true;
			thrownSwordEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
			world.spawnEntity(thrownSwordEntity);

			if (!user.getAbilities().creativeMode) {
				itemStack.decrement(1);
			}
		}

		user.swingHand(hand);
		return TypedActionResult.success(itemStack, world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.throwingsworditem.tooltip1").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.throwingsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.throwingsworditem.tooltip3")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
	}
}