package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ActiveDefencePower extends RunefusedGemPower {

	public ActiveDefencePower() {
		super(false);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
		if (user instanceof PlayerEntity player && player.getInventory().contains(Items.ARROW.getDefaultStack())) {
			int frequency = Config.gemPowers.activeDefence.frequency;
			if (player.age % frequency == 0) {
				double sradius = Config.gemPowers.activeDefence.radius;
				double vradius = Config.gemPowers.activeDefence.radius / 2.0;
				double x = player.getX();
				double y = player.getY();
				double z = player.getZ();
				Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
				for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

					if (entity instanceof LivingEntity le && HelperMethods.checkFriendlyFire(le, player)) {

						int arrowSlot = player.getInventory().getSlotWithStack(Items.ARROW.getDefaultStack());
						ItemStack arrowStack = player.getInventory().getStack(arrowSlot);
						int randomc = (int) (Math.random() * 100);
						if (randomc < 15) {
							arrowStack.decrement(1);
						}
						ArrowEntity arrow = new ArrowEntity(EntityType.ARROW, world);
						arrow.updatePosition(player.getX(), (player.getY() + 1.5), player.getZ());
						arrow.setOwner(player);
						arrow.setVelocity(le.getX() - player.getX(), (le.getY() - player.getY()) - 1, le.getZ() - player.getZ());
						world.spawnEntity(arrow);
						break;
					}
				}
			}
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		if (isRunic)
			tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip1").setStyle(Styles.RUNIC));
		else
			tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.active_defence").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, TooltipUtils.runic_tags)) {
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
			tooltip.add(Text.literal("\u00A0\u00A0\u00A0").append(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
	}

	public static class Settings extends TooltipSettings {

		public Settings() {
			super(GemPowerRegistry.ACTIVE_DEFENCE);
		}

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 1)
		public int frequency = 20;

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedDouble.Restrict(min = 0.0)
		public double radius = 5.0;
	}
}