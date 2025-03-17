package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
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

public class StarsEdgeSwordItem extends UniqueSwordItem {
    public StarsEdgeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            float skillDamageModifier = Config.uniqueEffects.stars_edge.damageModifier;
            float skillLifestealModifier = Config.uniqueEffects.stars_edge.lifestealModifier;
            ServerWorld world = (ServerWorld) attacker.getWorld();
            DamageSource damageSource = world.getDamageSources().generic();
            float abilityDamage = (float) HelperMethods.getEntityAttackDamage(attacker);
            if (attacker instanceof PlayerEntity player)
                damageSource = attacker.getDamageSources().playerAttack(player);

            HelperMethods.playHitSounds(attacker, target);

            if (world.isDay()) {
                target.timeUntilRegen = 0;
                target.damage(damageSource, abilityDamage * skillDamageModifier);
            }
            else if (world.isNight()) {
                attacker.heal(abilityDamage * skillLifestealModifier);
            }

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        int skillCooldown = Config.uniqueEffects.stars_edge.cooldown;
        int skillDuration = Config.uniqueEffects.stars_edge.duration;
        int skillStacks = Config.uniqueEffects.stars_edge.stacks;

        if (!user.hasStatusEffect(StatusEffects.SPEED)) {
            user.swingHand(hand);
            world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_02.get(),
                    user.getSoundCategory(), 0.3f, 1.0f);

            user.setVelocity(user.getRotationVector().negate().multiply(+1.5));
            user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent user flying to the heavens
            user.velocityModified = true;
            HelperMethods.incrementStatusEffect(user, StatusEffects.SPEED, skillDuration, 1, 2);
        } else {
            StatusEffectInstance speedEffect = user.getStatusEffect(StatusEffects.SPEED);
            if (speedEffect != null && speedEffect.getDuration() < (skillDuration - 10)) {
                world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_01.get(),
                        user.getSoundCategory(), 0.5f, 1.3f);
                user.setVelocity(user.getRotationVector().multiply(+1.7));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent user flying to the heavens
                user.velocityModified = true;
                user.removeStatusEffect(StatusEffects.SPEED);
                HelperMethods.incrementStatusEffect(user, StatusEffects.RESISTANCE, skillDuration / 2, 2, 3);
                HelperMethods.incrementStatusEffect(user, StatusEffects.HASTE, skillDuration / 2, skillStacks, 7);
                user.getItemCooldownManager().set(this, skillCooldown);
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_OBSIDIAN_TEAR,
                ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {

        float skillDamageModifier = Config.uniqueEffects.stars_edge.damageModifier;

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip3", skillDamageModifier).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip8").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STARS_EDGE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 120;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 120;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageModifier = 0.40f;
        @ValidatedFloat.Restrict(min = 0f)
        public float lifestealModifier = 0.10f;
        @ValidatedInt.Restrict(min = 1)
        public int stacks = 6;

    }
}