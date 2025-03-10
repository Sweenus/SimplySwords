package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
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
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class StealSwordItem extends UniqueSwordItem {
    public StealSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.getWorld();
            int hitChance = Config.uniqueEffects.steal.chance;
            int duration = Config.uniqueEffects.steal.duration;
            attacker.setVelocity(attacker.getRotationVector().multiply(+1));
            attacker.velocityModified = true;

            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= hitChance) {
                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10) {
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(),
                            target.getSoundCategory(), 0.5f, 2f);
                } else if (choose_sound <= 20) {
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(),
                            target.getSoundCategory(), 0.5f, 2f);
                } else {
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(),
                            target.getSoundCategory(), 0.5f, 2f);
                }

                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 2), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, duration, 1), attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            double sradius = Config.uniqueEffects.steal.radius;
            double vradius = Config.uniqueEffects.steal.radius / 2.0;

            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.getWorld();
            Box box = new Box(x + sradius, y + vradius, z + sradius,
                    x - sradius, y - vradius, z - sradius);
            for (Entity entity : sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                    int iduration = Config.uniqueEffects.steal.invisDuration;
                    int bduration = Config.uniqueEffects.steal.blindDuration;

                    if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING)) {
                        if (le.distanceTo(user) > 5) { //can we check target here?
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, bduration, 1), user);
                            user.teleport(le.getX(), le.getY(), le.getZ(), false);
                            sworld.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                                    le.getSoundCategory(), 0.3f, 1.5f);
                            float abilityDamage = HelperMethods.spellScaledDamage("soul", user, Config.uniqueEffects.steal.spellScaling, 5);
                            le.damage(user.getDamageSources().freeze(), abilityDamage);
                        } else {
                            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, iduration, 1), user);
                            user.setVelocity(user.getRotationVector().multiply(+2));
                            user.velocityModified = true;
                            sworld.playSoundFromEntity(null, entity, SoundRegistry.MAGIC_BOW_SHOOT_MISS_01.get(),
                                    entity.getSoundCategory(), 0.3f, 1.5f);
                        }
                        le.removeStatusEffect(StatusEffects.SLOWNESS);
                        le.removeStatusEffect(StatusEffects.GLOWING);
                    }
                }
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.NAUTILUS, ParticleTypes.NAUTILUS,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip9").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip10").setStyle(Styles.TEXT));
        HelperMethods.appendSpellScaleTooltip(tooltip, "soul");
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULSTEALER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 400;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 30.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 2.6f;

        @ValidatedInt.Restrict(min = 0)
        public int blindDuration = 200;
        @ValidatedInt.Restrict(min = 0)
        public int invisDuration = 120;
    }
}