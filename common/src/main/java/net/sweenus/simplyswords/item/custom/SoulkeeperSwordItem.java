package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class SoulkeeperSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public SoulkeeperSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            int hitChance = Config.uniqueEffects.soulkeeper.chance;
            int duration = Config.uniqueEffects.soulkeeper.duration;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= hitChance) {
                //world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.3f, 1.2f);
                if (attacker.hasStatusEffect(StatusEffects.MINING_FATIGUE) && attacker.hasStatusEffect(StatusEffects.RESISTANCE)) {

                    int a = (attacker.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1);

                    if ((attacker.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() <= 2)) {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, a), attacker);
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, a), attacker);
                    }
                } else {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, 1), attacker);
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1), attacker);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            double hradius = Config.uniqueEffects.soulkeeper.radius;
            double vradius = Config.uniqueEffects.soulkeeper.radius;
            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld serverWorld = (ServerWorld) user.getWorld();
            Box box = new Box(x + hradius, y + vradius, z + hradius, x - hradius, y - vradius, z - hradius);

            for (Entity entity : serverWorld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if (entity instanceof LivingEntity le && user.hasStatusEffect(StatusEffects.MINING_FATIGUE) && HelperMethods.checkFriendlyFire((LivingEntity) entity, user)) {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, user.getStatusEffect(StatusEffects.MINING_FATIGUE).getDuration(),
                            user.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()), user);
                    world.playSoundFromEntity(null, entity, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                            entity.getSoundCategory(), 0.1f, 1f);
                }
            }

            if (user.hasStatusEffect(StatusEffects.MINING_FATIGUE) && user.hasStatusEffect(StatusEffects.RESISTANCE)) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                        user.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() * 40, 2), user);

                user.removeStatusEffect(StatusEffects.MINING_FATIGUE);
                user.removeStatusEffect(StatusEffects.RESISTANCE);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL, ParticleTypes.SOUL,
                ParticleTypes.SPORE_BLOSSOM_AIR, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip4").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULKEEPER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 75;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 250;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 5.0;
    }
}
