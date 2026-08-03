package net.sweenus.simplyswords.power.powers;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.power.RunefusedGemPower;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class FrostWardPower extends RunefusedGemPower {

	public FrostWardPower() {
		super(false);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, LivingEntity user, int slot, boolean selected) {
		int frequency = Config.gemPowers.frostWard.frequency;

		if (user.age % frequency == 0) {
			int duration = AwakeningApi.scaleGemPowerDuration(stack, Config.gemPowers.frostWard.duration);
			double sRadius = Config.gemPowers.frostWard.radius;
			double vRadius = Config.gemPowers.frostWard.radius / 2.0;
			double x = user.getX();
			double y = user.getY();
			double z = user.getZ();
			ServerWorld serverWorld = (ServerWorld) world;
			Box box = new Box(x + sRadius, y + vRadius, z + sRadius, x - sRadius, y - vRadius, z - sRadius);
			for (Entity entity : serverWorld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

				if (entity instanceof LivingEntity le && HelperMethods.checkFriendlyFire(le, user)) {

					if (le.distanceTo(user) < sRadius) {
						SnowballEntity snowball = new SnowballEntity(EntityType.SNOWBALL, serverWorld);
						snowball.updatePosition(user.getX(), (user.getY() + 1.5), user.getZ());
						snowball.setOwner(user);
						le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration));
						snowball.setVelocity(le.getX() - user.getX(), (le.getY() - user.getY()) - 1, le.getZ() - user.getZ());
						serverWorld.spawnEntity(snowball);
					}
				}
			}
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext, boolean isRunic) {
		if (isRunic)
			tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip1").setStyle(Styles.RUNIC));
		else
			tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.frost_ward").setStyle(Styles.RUNIC));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2")).setStyle(Styles.RUNIC_DESCRIPTION));
		}
	}

	public static class Settings extends TooltipSettings {

		public Settings() {
			super(GemPowerRegistry.FROST_WARD);
		}

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 1)
		public int frequency = 20;

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedDouble.Restrict(min = 0.0)
		public double radius = 5.0;

		@Translation(prefix = "simplyswords.config.basic_settings")
		@ValidatedInt.Restrict(min = 0)
		public int duration = 60;
	}
}
