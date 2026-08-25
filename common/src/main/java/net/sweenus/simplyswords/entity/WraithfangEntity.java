package net.sweenus.simplyswords.entity;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase2UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class WraithfangEntity extends ThrownSpearEntity {
    public int slownessDuration;
    private @Nullable UniqueAbilityExecution abilityExecution;
    private boolean impactBurstUsed;

    // Base Constructor
    public WraithfangEntity(EntityType<? extends WraithfangEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public WraithfangEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        returnToPlayer = true;

        Entity owner = getOwner();
        if (owner != null && owner.distanceTo(this) > 1 && owner.distanceTo(this) < 500
                && getWorld() instanceof ServerWorld serverWorld && age < 100) {
            owner.setVelocity((this.getX() - owner.getX()) / 8, (this.getY() - owner.getY()) / 8, (this.getZ() - owner.getZ()) / 8);
            owner.velocityModified = true;
            HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.OMINOUS_SPAWNING, this, owner, (int) this.distanceTo(owner));
            if (owner instanceof LivingEntity livingEntity) {
                livingEntity.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RESILIENCE), 20, 4, false, false, true));
            }
        }

        super.tick();
        if (isRemoved() && abilityExecution != null && !abilityExecution.isTerminal()) {
            UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), 0);
        }
    }

    public void setAbilityExecution(UniqueAbilityExecution execution) {
        this.abilityExecution = execution;
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target, float damage) {
        if (abilityExecution == null || !(getWorld() instanceof ServerWorld)) return;
        Phase2AbilityTuning tuning = Phase2UniqueAbilities.tuning(abilityExecution);
        int duration = tuning.integer(Phase2AbilityTuning.Setting.STATUS_DURATION_TICKS, 0);
        if (duration > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,
                duration, tuning.integer(Phase2AbilityTuning.Setting.STATUS_AMPLIFIER, 0),
                false, false, true), getOwner());
        UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                Phase2UniqueAbilities.HIT, target, 1, damage);
        burst(target, damage, tuning);
    }

    @Override
    protected float doExtraDamage(Entity entity, float baseDamage, DamageSource source) {
        float damage = super.doExtraDamage(entity, baseDamage, source);
        if (abilityExecution == null) return damage;
        Phase2AbilityTuning tuning = Phase2UniqueAbilities.tuning(abilityExecution);
        double perTick = tuning.get(Phase2AbilityTuning.Setting.BONUS_PER_TRIGGER, .5);
        int cap = tuning.integer(Phase2AbilityTuning.Setting.THRESHOLD, age);
        return damage - age * .5F + (float) (Math.min(age, cap) * perTick);
    }

    @Override
    protected int getAdditionalPierces() {
        return abilityExecution == null ? 0 : Phase2UniqueAbilities.tuning(abilityExecution)
                .integer(Phase2AbilityTuning.Setting.PIERCE_COUNT, 0);
    }

    @Override
    protected float getPierceDamageMultiplier() {
        return abilityExecution == null ? 1.0F : (float) Phase2UniqueAbilities.tuning(abilityExecution)
                .get(Phase2AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1);
    }

    private void burst(LivingEntity directTarget, float damage, Phase2AbilityTuning tuning) {
        double radius = tuning.get(Phase2AbilityTuning.Setting.IMPACT_RADIUS, 0);
        int cap = tuning.integer(Phase2AbilityTuning.Setting.IMPACT_TARGET_CAP, 0);
        float burstDamage = damage * (float) tuning.get(Phase2AbilityTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0);
        if (impactBurstUsed || radius <= 0 || cap <= 0 || burstDamage <= 0
                || !(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)) return;
        impactBurstUsed = true;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                new Box(directTarget.getPos(), directTarget.getPos()).expand(radius),
                candidate -> candidate != directTarget && candidate != owner && candidate.isAlive()
                        && candidate.squaredDistanceTo(directTarget) <= radius * radius
                        && HelperMethods.checkAbilityTarget(candidate, owner));
        targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(directTarget)));
        for (int i = 0; i < Math.min(cap, targets.size()); i++) {
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, targets.get(i),
                    burstDamage, SpellScalingProfile.SOUL);
        }
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        boolean canPickup = super.tryPickup(player);

        if (canPickup) {
            player.getWorld().playSound(this, this.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_IMPACT_02.get(),
                    this.getSoundCategory(), 0.1f, 1.2f);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, Config.uniqueEffects.wraithfang.duration, Config.uniqueEffects.wraithfang.hasteAmplifier, false, false, true));

            int cooldown = 10;
            SimplySwordsAPI.setWeaponCooldown(player, this.asItemStack(), cooldown);
            if (abilityExecution != null) {
                UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                        Phase2UniqueAbilities.RETURN, player, 1, 0);
                UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), 1);
            }
        }

        return canPickup;
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.WRAITHFANG.get());
    }

    @Override
    protected byte getLoyalty() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            return 3;
        } else {
            return 0;
        }
    }

}
