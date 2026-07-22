package net.sweenus.simplyswords.item.custom;

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
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;

public class StormsEdgeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public StormsEdgeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    private static final int radius = 1;
    private static final int ability_timer_max = 13;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        int chargeChance = Config.uniqueEffects.storms_edge.chance;
        if (!attacker.getWorld().isClient() && attacker.getRandom().nextInt(100) <= chargeChance && (attacker instanceof PlayerEntity player)
                && player.getItemCooldownManager().getCooldownProgress(this, 1f) > 0) {
            player.getItemCooldownManager().set(this, 0);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                    attacker.getSoundCategory(), 0.7f, 1f);
        } else if (!attacker.getWorld().isClient()
                && attacker.getRandom().nextInt(100) <= chargeChance
                && attacker.getWorld() instanceof ServerWorld
                && !(attacker instanceof PlayerEntity)) {
            WeaponAbilityCooldownManager.clearCooldown(attacker, stack);
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                    attacker.getSoundCategory(), 0.7f, 1f);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        if (target == null || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        LivingEntityAbilityMovementManager.dashTowardTargetWithImpact(context.world(), actor, target, 1.6, 6, 0.85, () -> {
            if (!target.isAlive() || !HelperMethods.checkAbilityTarget(target, actor)) {
                return;
            }
            context.world().spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.35, 0.35, 0.35, 0.08);
            DamageSource damageSource = actor.getDamageSources().indirectMagic(actor, actor);
            float damage = HelperMethods.attackScaledDamage(actor, context.stack(), Config.uniqueEffects.storms_edge.damageScaling);
            target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(context.world(), context.stack(), target, damageSource, damage));
        });
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), actor);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1), actor);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), actor);
        context.world().playSoundFromEntity(null, actor, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                actor.getSoundCategory(), 0.3f, 1.6f);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.storms_edge.cooldown;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(),
                user.getSoundCategory(), 0.4f, 1.2f);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), user);
        user.timeUntilRegen = 15;
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && HelperMethods.isHolding(stack, user)) {
            int skillCooldown = Config.uniqueEffects.storms_edge.cooldown;
            AbilityMethods.tickAbilityStormJolt(stack, world, user, remainingUseTicks, skillCooldown, radius);
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        //Player dash end
        if (!world.isClient && HelperMethods.isHolding(stack, user)) {
            user.setVelocity(0, 0, 0); // Stop player at end of charge
            user.velocityModified = true;
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), user);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return ability_timer_max;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormsedgesworditem.tooltip3").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.storms_edge.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STORMS_EDGE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 100;
        public float damageScaling = 0.5f;

    }
}
