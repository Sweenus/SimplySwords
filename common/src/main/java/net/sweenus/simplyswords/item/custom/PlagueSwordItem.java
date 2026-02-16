package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class PlagueSwordItem extends UniqueSwordItem {
    public PlagueSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int hitChance = Config.uniqueEffects.toxic_longsword.chance;
        HelperMethods.playHitSounds(attacker, target);

        if (attacker.getRandom().nextInt(100) <= hitChance) {

            //Convert Haste
            if (target.hasStatusEffect(StatusEffects.HASTE)) {
                var statdur = (target.getStatusEffect(StatusEffects.HASTE).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.HASTE).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.HASTE);
            }

            //Convert Regeneration
            if (target.hasStatusEffect(StatusEffects.REGENERATION)) {
                var statdur = (target.getStatusEffect(StatusEffects.REGENERATION).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.REGENERATION).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.REGENERATION);
            }

            //Convert Strength
            if (target.hasStatusEffect(StatusEffects.STRENGTH)) {
                var statdur = (target.getStatusEffect(StatusEffects.STRENGTH).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.STRENGTH).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.STRENGTH);
            }

            //Convert Speed
            if (target.hasStatusEffect(StatusEffects.SPEED)) {
                var statdur = (target.getStatusEffect(StatusEffects.SPEED).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.SPEED).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.SPEED);
            }

            //Convert Invisibility
            if (target.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                var statdur = (target.getStatusEffect(StatusEffects.INVISIBILITY).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.INVISIBILITY).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.INVISIBILITY);
            }

            //Convert Resistance
            if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                var statdur = (target.getStatusEffect(StatusEffects.RESISTANCE).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.RESISTANCE);
            }

            //Convert Saturation
            if (target.hasStatusEffect(StatusEffects.SATURATION)) {
                var statdur = (target.getStatusEffect(StatusEffects.SATURATION).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.SATURATION).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.SATURATION);
            }

            //Convert Fire Resistance
            if (target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                var statdur = (target.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getAmplifier());
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, statdur, statamp), attacker);
                target.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
            }

            //Convert Absorption
            if (target.hasStatusEffect(StatusEffects.ABSORPTION)) {
                var statdur = (target.getStatusEffect(StatusEffects.ABSORPTION).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.ABSORPTION).getAmplifier() / 2);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 0, statamp), attacker);
                target.removeStatusEffect(StatusEffects.ABSORPTION);
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SPORE_BLOSSOM_AIR,
                ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.FALLING_SPORE_BLOSSOM, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip3").setStyle(Styles.TEXT));

        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.TOXIC_LONGSWORD::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 55;

    }
}