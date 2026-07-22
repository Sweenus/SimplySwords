package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
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
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class StormSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public StormSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

            int hitChance = Config.uniqueEffects.mjolnir.chance;

            if (attacker.getRandom().nextInt(100) <= hitChance) {
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.STORM), 2, 1), attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return startPlayerAbility(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), user);
        user.setCurrentHand(hand);
        int cooldown = Config.uniqueEffects.mjolnir.cooldown;
        user.getItemCooldownManager().set(this, cooldown);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {
            int radius = Config.uniqueEffects.mjolnir.radius;
            int cooldown = Config.uniqueEffects.mjolnir.cooldown;
            AbilityMethods.tickAbilityStorm(stack, world, user, remainingUseTicks, cooldown, radius);
        }
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        if (target == null || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Config.uniqueEffects.mjolnir.frequency + 5, 5), actor);
        target.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FREEZE), Config.uniqueEffects.mjolnir.frequency + 5, 0), actor);
        LightningEntity storm = EntityType.LIGHTNING_BOLT.spawn(context.world(), target.getBlockPos(), SpawnReason.TRIGGERED);
        if (storm != null) {
            storm.setCosmetic(true);
        }
        DamageSource damageSource = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.attackScaledDamage(actor, context.stack(), Config.uniqueEffects.mjolnir.damageScaling);
        target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(context.world(), context.stack(), target, damageSource, damage));
        context.world().spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 2.0, actor.getZ(), 24,
                Config.uniqueEffects.mjolnir.radius * 0.25, 0.6, Config.uniqueEffects.mjolnir.radius * 0.25, 0.02);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.mjolnir.cooldown;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Config.uniqueEffects.mjolnir.duration;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FIREWORK, ParticleTypes.FIREWORK, ParticleTypes.ELECTRIC_SPARK, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.mjolnir.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MJOLNIR::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 700;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 200;
        @ValidatedInt.Restrict(min = 1)
        public int frequency = 10;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 10;
        public float damageScaling = 0.56f;

    }
}
