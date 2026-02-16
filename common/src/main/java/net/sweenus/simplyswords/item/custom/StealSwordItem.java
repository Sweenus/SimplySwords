package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
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
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.getEntityWorld();
            int hitChance = Config.uniqueEffects.soulstealer.chance;
            int duration = Config.uniqueEffects.soulstealer.duration;
            attacker.setVelocity(attacker.getRotationVector().multiply(+1));
            attacker.velocityDirty = true;

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
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.getEntityWorld().isClient()) {
            double sradius = Config.uniqueEffects.soulstealer.radius;
            double vradius = Config.uniqueEffects.soulstealer.radius / 2.0;

            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.getEntityWorld();
            Box box = new Box(x + sradius, y + vradius, z + sradius,
                    x - sradius, y - vradius, z - sradius);
            for (Entity entity : sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                    int iduration = Config.uniqueEffects.soulstealer.invisDuration;
                    int bduration = Config.uniqueEffects.soulstealer.blindDuration;

                    if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING)) {
                        if (le.distanceTo(user) > 5) { //can we check target here?
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, bduration, 1), user);
                            user.teleport(le.getX(), le.getY(), le.getZ(), false);
                            sworld.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                                    le.getSoundCategory(), 0.3f, 1.5f);
                            float abilityDamage = HelperMethods.spellScaledDamage("soul", user, Config.uniqueEffects.soulstealer.spellScaling, 5);
                            le.damage((ServerWorld) user.getEntityWorld(), user.getDamageSources().freeze(), abilityDamage);
                        } else {
                            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, iduration, 1), user);
                            user.setVelocity(user.getRotationVector().multiply(+2));
                            user.velocityDirty = true;
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
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.NAUTILUS, ParticleTypes.NAUTILUS,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
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
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "soul");
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
