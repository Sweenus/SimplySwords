package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.MagispearEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;

import java.util.List;
import java.util.Random;

public class MagispearSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public MagispearSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            ServerWorld world = (ServerWorld) attacker.getWorld();
            float hitChance = Config.uniqueEffects.magispear.magicChance;
            int random = new Random().nextInt(100);
            if (random < hitChance) {
                float damage = HelperMethods.attackScaledDamage(attacker, stack, Config.uniqueEffects.magispear.magicDamageScaling);
                DamageSource damageSource = attacker.getDamageSources().indirectMagic(attacker, attacker);
                target.timeUntilRegen = 0;
                target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage));
                target.timeUntilRegen = 0;
                world.playSound(null, attacker.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.2f, 1.1f);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        int skillCooldown = Config.uniqueEffects.magispear.cooldown;

        world.playSound(null, user.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_NORDIC_27.get(),
                user.getSoundCategory(), 0.2f, 1.1f);
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISLAM), 62, 1));
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RESILIENCE), 64, 3));
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            MagispearEntity magispearEntity = new MagispearEntity(world, user, itemStack.copy() );
            magispearEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            magispearEntity.setYaw(user.getYaw());
            magispearEntity.setPitch(user.getPitch()-90);
            magispearEntity.primaryBaseDamage = HelperMethods.attackScaledDamage(user, itemStack, Config.uniqueEffects.magispear.throwDamageScaling);
            magispearEntity.hasLoyalty = 3;
            if (hand == Hand.OFF_HAND)
                magispearEntity.offhandThrow = true;
            magispearEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(magispearEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);

        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntity actor = context.actor();
        actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.MAGISLAM), 62, 1));
        actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RESILIENCE), 64, 3));
        MagispearEntity magispearEntity = new MagispearEntity(context.world(), actor, context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(actor, context.target());
        magispearEntity.setVelocity(direction.x, direction.y, direction.z, 1.65F, 1.0F);
        magispearEntity.setYaw(actor.getYaw());
        magispearEntity.setPitch(actor.getPitch() - 90);
        magispearEntity.primaryBaseDamage = HelperMethods.attackScaledDamage(actor, context.stack(), Config.uniqueEffects.magispear.throwDamageScaling);
        magispearEntity.hasLoyalty = 0;
        magispearEntity.setPos(actor.getX(), actor.getEyeY() - 0.5, actor.getZ());
        magispearEntity.markNonReturning(80);
        context.world().spawnEntity(magispearEntity);
        LivingEntityAbilityMovementManager.dashTowardTarget(context.world(), actor, context.target(), 1.35, 10);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.magispear.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip9").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.magispear.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MAGISPEAR::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 1.6f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 4.0;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int magicChance = 35;
        @ValidatedFloat.Restrict(min = 0f)
        public float magicDamageScaling = 0.16f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwDamageScaling = 0.4f;

    }
}
