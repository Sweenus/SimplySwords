package net.sweenus.simplyswords.power.powers;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.power.NetherGemPower;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class NullificationPower extends NetherGemPower {

	public NullificationPower() {
		super(false);
	}

	@Override
	public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.BATTLE_FATIGUE))) {
			if (!attacker.getWorld().isClient()) {
				ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
				BlockState currentState = serverWorld.getBlockState(attacker.getBlockPos().up(4).offset(attacker.getMovementDirection(), 3));
				BlockState state = Blocks.AIR.getDefaultState();
				if (currentState == state) {
					serverWorld.playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
							attacker.getSoundCategory(), 0.4f, 0.8f);
					BattleStandardEntity banner = EntityRegistry.BATTLESTANDARD.get().spawn(
							serverWorld,
							attacker.getBlockPos().up(4).offset(attacker.getMovementDirection(), 3),
							SpawnReason.MOB_SUMMONED);
					if (banner != null) {
						banner.setVelocity(0, -1, 0);
						banner.ownerEntity = attacker;
						banner.decayRate = 3;
						banner.standardType = "nullification";
						banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", attacker.getName()));
					}
					attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.BATTLE_FATIGUE), 800, 0), attacker);
				}
			}
		}
	}

	@Override
	public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, boolean isRunic) {
		tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification").setStyle(Styles.NETHERFUSED));

		if (TooltipUtils.shouldDisplayTooltip(itemStack, null)) {
			tooltip.add(Text.literal("").append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description")).setStyle(Styles.NETHERFUSED_DESCRIPTION));
		}
	}
}
