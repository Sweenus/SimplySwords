package net.sweenus.simplyswords.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.api.ability.Phase2AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase2UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.minecraft.entity.player.PlayerEntity;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.WickpiercerThrowMath;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WickpiercerEntity extends ThrownSpearEntity {
    private static final int ORBIT_MODE = 2;
    private static final double ORBIT_RADIUS = 1.6;

    public int slownessDuration;
    private @Nullable UniqueAbilityExecution abilityExecution;
    private boolean impactBurstUsed;
    private @Nullable LivingEntity orbitTarget;
    private int orbitEndAge;
    private int nextOrbitStrike;
    private boolean orbitUsed;
    private boolean loyaltyRestored;
    private final Set<UUID> trailTargets = new HashSet<>();

    // Base Constructor
    public WickpiercerEntity(EntityType<? extends WickpiercerEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public WickpiercerEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        boolean orbiting = isOrbiting();
        if (!nonReturning) returnToPlayer = !orbiting;
        if (orbiting) {
            setNoClip(true);
            setVelocity(Vec3d.ZERO);
        }
        super.tick();
        if (!getWorld().isClient) {
            tickOrbit();
            tickDelayedReturn();
        }
        if (isRemoved()) finishExecution(0);
    }

    public void setAbilityExecution(UniqueAbilityExecution execution) {
        this.abilityExecution = execution;
    }

    private Phase2AbilityTuning tuning() {
        return abilityExecution == null ? Phase2AbilityTuning.EMPTY : Phase2UniqueAbilities.tuning(abilityExecution);
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target, float damage) {
        if (abilityExecution == null || !(getWorld() instanceof ServerWorld)) return;
        Phase2AbilityTuning tuning = tuning();
        int fireTicks = tuning.integer(Phase2AbilityTuning.Setting.PROJECTILE_FIRE_TICKS, 0);
        if (fireTicks > 0) target.setOnFireFor(WickpiercerThrowMath.scaledFireSeconds(fireTicks));
        UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                Phase2UniqueAbilities.HIT, target, 1, damage);
        burst(target, damage, tuning);
        startOrbit(target, tuning);
    }

    @Override
    protected int getAdditionalPierces() {
        return tuning().integer(Phase2AbilityTuning.Setting.PIERCE_COUNT, 0);
    }

    @Override
    protected float getPierceDamageMultiplier() {
        return (float) tuning().get(Phase2AbilityTuning.Setting.PIERCE_DAMAGE_MULTIPLIER, 1);
    }

    private boolean isOrbiting() {
        return orbitTarget != null
                && WickpiercerThrowMath.orbitContinues(age, orbitEndAge, orbitTarget.isAlive() && !orbitTarget.isRemoved());
    }

    private void startOrbit(LivingEntity target, Phase2AbilityTuning tuning) {
        int duration = tuning.integer(Phase2AbilityTuning.Setting.ORBIT_DURATION_TICKS, 0);
        if (orbitUsed || nonReturning || duration <= 0 || target.isRemoved()
                || (tuning.integer(Phase2AbilityTuning.Setting.MODE, 0) & ORBIT_MODE) == 0) return;
        orbitUsed = true;
        orbitTarget = target;
        orbitEndAge = age + duration;
        nextOrbitStrike = WickpiercerThrowMath.nextStrikeAge(age, orbitInterval(tuning));
    }

    private int orbitInterval(Phase2AbilityTuning tuning) {
        return Math.max(1, tuning.integer(Phase2AbilityTuning.Setting.INTERVAL_TICKS, 20));
    }

    private void tickOrbit() {
        if (orbitTarget == null) return;
        if (!isOrbiting()) {
            orbitTarget = null;
            setNoClip(true);
            return;
        }
        Phase2AbilityTuning tuning = tuning();
        double angle = age * 0.35;
        setPos(orbitTarget.getX() + Math.cos(angle) * ORBIT_RADIUS,
                orbitTarget.getBodyY(0.55),
                orbitTarget.getZ() + Math.sin(angle) * ORBIT_RADIUS);
        if (age < nextOrbitStrike || !(getWorld() instanceof ServerWorld world)
                || !(getOwner() instanceof LivingEntity owner)) return;
        nextOrbitStrike = WickpiercerThrowMath.nextStrikeAge(age, orbitInterval(tuning));
        float damage = primaryBaseDamage
                * (float) tuning.get(Phase2AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 0);
        if (damage <= 0 || !HelperMethods.checkAbilityTarget(orbitTarget, owner)) return;
        SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, orbitTarget,
                damage, SpellScalingProfile.FIRE);
    }

    private void tickDelayedReturn() {
        int delay = tuning().integer(Phase2AbilityTuning.Setting.RETURN_DELAY_TICKS, 0);
        if (loyaltyRestored || nonReturning || delay <= 0 || hasLoyalty > 0 || age < delay) return;
        loyaltyRestored = true;
        hasLoyalty = 3;
        setNoClip(true);
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
                    burstDamage, SpellScalingProfile.FIRE);
        }
    }

    @Override
    protected void damageOnReturn(double ignoredRadius, float ignoredDamage) {
        Phase2AbilityTuning tuning = tuning();
        double radius = tuning.get(Phase2AbilityTuning.Setting.RETURN_TRAIL_RADIUS, 0);
        float damage = primaryBaseDamage
                * (float) tuning.get(Phase2AbilityTuning.Setting.RETURN_TRAIL_DAMAGE_MULTIPLIER, 0);
        int fireTicks = tuning.integer(Phase2AbilityTuning.Setting.PROJECTILE_FIRE_TICKS, 0);
        if (radius <= 0 || damage <= 0 || !(getWorld() instanceof ServerWorld world)
                || !(getOwner() instanceof LivingEntity owner)) return;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                getBoundingBox().expand(radius),
                candidate -> candidate != owner && candidate.isAlive()
                        && !trailTargets.contains(candidate.getUuid())
                        && HelperMethods.checkAbilityTarget(candidate, owner));
        for (LivingEntity target : targets) {
            trailTargets.add(target.getUuid());
            if (fireTicks > 0) target.setOnFireFor(WickpiercerThrowMath.scaledFireSeconds(fireTicks));
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, target,
                    damage, SpellScalingProfile.FIRE);
        }
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        boolean pickedUp = super.tryPickup(player);
        if (pickedUp) finishExecution(1);
        return pickedUp;
    }

    private void finishExecution(int affected) {
        if (abilityExecution != null && !abilityExecution.isTerminal()) {
            UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), affected);
        }
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.WICKPIERCER.get());
    }

    // Constant so a chunk-reloaded spear, whose hasLoyalty is not persisted, still returns.
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
