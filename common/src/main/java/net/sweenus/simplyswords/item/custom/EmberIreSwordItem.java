package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ChanceDurationSettings;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class EmberIreSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    private static final double TARGET_RANGE = 18.0;

    public EmberIreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    private static SimpleParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static SimpleParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static SimpleParticleType particlePassive = ParticleTypes.SMOKE;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && remainingUseTicks %10 == 0 && remainingUseTicks < getMaxUseTime(stack, user) - 5) {
            world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_RECHARGE.get(),
                    user.getSoundCategory(), 0.2f, 1.1f - (remainingUseTicks * 0.001f));

            if (remainingUseTicks < 20) {
                onStoppedUsing(stack, world, user, remainingUseTicks);
            }

        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            LivingEntity targetEntity = user instanceof PlayerEntity player ? findPlayerTarget(player) : null;
            double damageAmount = HelperMethods.abilityScaledDamage("fire", user, stack,
                    Config.uniqueEffects.emberblade.initialDamageScaling, Config.uniqueEffects.emberblade.initialSpellScaling);
            if (targetEntity != null) {
                SoundEvent soundSelect = SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get();
                int particleCount = 20;
                HelperMethods.spawnWaistHeightParticles((ServerWorld) world, ParticleTypes.SMOKE, user, targetEntity, particleCount);
                HelperMethods.spawnWaistHeightParticles((ServerWorld) world, ParticleTypes.POOF, user, targetEntity, particleCount);
                HelperMethods.spawnWaistHeightParticles((ServerWorld) world, ParticleTypes.ASH, user, targetEntity, particleCount);
                world.playSound(null, user.getBlockPos(), soundSelect,
                        user.getSoundCategory(), 0.4f, 1.5f);
                DamageSource damageSource = user.getDamageSources().generic();
                if (user instanceof PlayerEntity player) {
                    damageSource = user.getDamageSources().playerAttack(player);
                    player.getItemCooldownManager().set(stack.getItem(), 10);
                }

                final float minAdditionalDamage = 0.0f;
                final float maxAdditionalDamage = HelperMethods.abilityScaledDamage("fire", user, stack,
                        Config.uniqueEffects.emberblade.maxChargeDamageScaling, Config.uniqueEffects.emberblade.maxChargeSpellScaling);
                float chargeRatio = 1.0f - ((float) remainingUseTicks / getMaxUseTime(stack, user));
                float additionalDamage = minAdditionalDamage + (maxAdditionalDamage - minAdditionalDamage) * chargeRatio;
                float finalDamage = (float) damageAmount + additionalDamage;
                targetEntity.timeUntilRegen = 0;
                targetEntity.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) world, stack, targetEntity, damageSource, finalDamage));

                world.playSound(null, targetEntity.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        user.getSoundCategory(), 0.4f, 1.1f);
                HelperMethods.spawnOrbitParticles((ServerWorld) world, targetEntity.getPos(), ParticleTypes.EXPLOSION, 1, 1 );
                HelperMethods.spawnOrbitParticles((ServerWorld) world, targetEntity.getPos(), ParticleTypes.POOF, 1, 20 );
                user.setVelocity(user.getRotationVector().negate().multiply(+1.1));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z);
                user.velocityModified = true;

                int hitChance = Config.uniqueEffects.emberblade.chance;
                int duration = Config.uniqueEffects.emberblade.duration;

                if (user.getRandom().nextInt((int) (250 - (chargeRatio * 100))) <= hitChance) {
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0), user);
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), user);
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 0), user);
                    world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_01.get(),
                            user.getSoundCategory(), 0.5f, 2f);
                    particlePassive = ParticleTypes.LAVA;
                    particleWalk = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                    particleSprint = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                }

            }
        }
    }

    public static LivingEntity findPlayerTarget(PlayerEntity player) {
        return StealSwordItem.findLenientTarget(player, TARGET_RANGE);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        if (target == null || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        ServerWorld world = context.world();
        double damageAmount = HelperMethods.abilityScaledDamage("fire", actor, context.stack(),
                Config.uniqueEffects.emberblade.initialDamageScaling, Config.uniqueEffects.emberblade.initialSpellScaling);
        float finalDamage = (float) (damageAmount + HelperMethods.abilityScaledDamage("fire", actor, context.stack(),
                Config.uniqueEffects.emberblade.maxChargeDamageScaling, Config.uniqueEffects.emberblade.maxChargeSpellScaling));
        SoundEvent soundSelect = SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get();
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.SMOKE, actor, target, 20);
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.POOF, actor, target, 20);
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.ASH, actor, target, 20);
        world.playSound(null, actor.getBlockPos(), soundSelect, actor.getSoundCategory(), 0.4f, 1.5f);
        target.timeUntilRegen = 0;
        DamageSource damageSource = actor.getDamageSources().mobAttack(actor);
        target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, context.stack(), target, damageSource, finalDamage));
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), actor.getSoundCategory(), 0.4f, 1.1f);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.EXPLOSION, 1, 1);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.POOF, 1, 20);

        int hitChance = Config.uniqueEffects.emberblade.chance;
        int duration = Config.uniqueEffects.emberblade.duration;
        if (actor.getRandom().nextInt(150) <= hitChance) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 0), actor);
            world.playSoundFromEntity(null, actor, SoundRegistry.MAGIC_SWORD_SPELL_01.get(),
                    actor.getSoundCategory(), 0.5f, 2f);
        }
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 10;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 80;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if ((entity instanceof PlayerEntity player)) {
            if (!player.hasStatusEffect(StatusEffects.STRENGTH) && !player.isOnFire()) {
                particlePassive = ParticleTypes.SMOKE;
                particleWalk = ParticleTypes.FALLING_LAVA;
                particleSprint = ParticleTypes.FALLING_LAVA;
            }
        }
        int stepMod = 7 - (int)(world.getTime() % 7);
        HelperMethods.createFootfalls(entity, stack, world, particleWalk, particleSprint, particlePassive, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip9").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, 10);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    @Translation(prefix = "", negate = true)
    public static class EffectSettings extends ChanceDurationSettings {

        public EffectSettings() {
            super(30, 150, new ItemStackTooltipAppender(ItemsRegistry.EMBERBLADE::get));
        }

        public float initialDamageScaling = 0.24f;
        public float initialSpellScaling = 0.48f;
        public float maxChargeDamageScaling = 2.4f;
        public float maxChargeSpellScaling = 4.8f;
    }
}
