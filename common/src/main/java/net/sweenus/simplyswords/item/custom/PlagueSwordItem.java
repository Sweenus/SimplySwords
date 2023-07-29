package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class PlagueSwordItem extends UniqueSwordItem {
    public PlagueSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int phitchance = (int) SimplySwords.uniqueEffectsConfig.plagueChance;
        HelperMethods.playHitSounds(attacker, target);

        if (attacker.getRandom().nextInt(100) <= phitchance) {

            //Convert Haste
            if (target.hasStatusEffect(StatusEffects.HASTE)) {
                var statdur = (target.getStatusEffect(StatusEffects.SLOWNESS).getDuration());
                var statamp = (target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier());
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
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SPORE_BLOSSOM_AIR,
                ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.FALLING_SPORE_BLOSSOM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.plaguesworditem.tooltip3").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
