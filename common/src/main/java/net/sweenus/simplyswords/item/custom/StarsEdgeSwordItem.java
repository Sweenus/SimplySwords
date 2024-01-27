package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class StarsEdgeSwordItem extends UniqueSwordItem {
    public StarsEdgeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            float skillDamageModifier = Config.getFloat("celestialSurgeDamageModifier", "UniqueEffects", ConfigDefaultValues.celestialSurgeDamageModifier);
            float skillLifestealModifier = Config.getFloat("celestialSurgeLifestealModifier", "UniqueEffects", ConfigDefaultValues.celestialSurgeLifestealModifier);
            ServerWorld world = (ServerWorld) attacker.getWorld();
            DamageSource damageSource = world.getDamageSources().generic();
            float abilityDamage = getAttackDamage();
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
        int skillCooldown = (int) Config.getFloat("celestialSurgeCooldown", "UniqueEffects", ConfigDefaultValues.celestialSurgeCooldown);
        int skillDuration = (int) Config.getFloat("celestialSurgeDuration", "UniqueEffects", ConfigDefaultValues.celestialSurgeDuration);
        int skillStacks = (int) Config.getFloat("celestialSurgeStacks", "UniqueEffects", ConfigDefaultValues.celestialSurgeStacks);

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
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.FALLING_OBSIDIAN_TEAR,
                ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");
        float skillDamageModifier = Config.getFloat("celestialSurgeDamageModifier", "UniqueEffects", ConfigDefaultValues.celestialSurgeDamageModifier);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip3", getAttackDamage() * skillDamageModifier).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip8").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
