package net.sweenus.simplyswords.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
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

    public int slownessDuration;
    private @Nullable UniqueAbilityExecution abilityExecution;
    private boolean impactBurstUsed;
    private @Nullable LivingEntity orbitTarget;
    private int orbitStartAge;
    private int orbitEndAge;
    private int nextOrbitStrike;
    private float orbitPhase;
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

    private AbyssalSpectralMasteryTuning tuning() {
        return abilityExecution == null ? AbyssalSpectralMasteryTuning.EMPTY : AbyssalSpectralMasteryAbilities.tuning(abilityExecution);
    }

    @Override
    protected void onSuccessfulHit(LivingEntity target, float damage) {
        if (abilityExecution == null || !(getWorld() instanceof ServerWorld)) return;
        AbyssalSpectralMasteryTuning tuning = tuning();
        int fireTicks = tuning.integer(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_FIRE_TICKS, 0);
        if (fireTicks > 0) target.setOnFireFor(WickpiercerThrowMath.scaledFireSeconds(fireTicks));
        UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                AbyssalSpectralMasteryAbilities.HIT, target, 1, damage);
        burst(target, damage, tuning);
        startOrbit(target, tuning);
    }

    @Override
    protected int getAdditionalPierces() {
        return tuning().integer(AbyssalSpectralMasteryTuning.Setting.PIERCE_COUNT, 0);
    }

    @Override
    protected float getPierceDamageMultiplier() {
        return (float) tuning().get(AbyssalSpectralMasteryTuning.Setting.PIERCE_DAMAGE_MULTIPLIER, 1);
    }

    private boolean isOrbiting() {
        return orbitTarget != null
                && WickpiercerThrowMath.orbitContinues(age, orbitEndAge, orbitTarget.isAlive() && !orbitTarget.isRemoved());
    }

    private void startOrbit(LivingEntity target, AbyssalSpectralMasteryTuning tuning) {
        int duration = tuning.integer(AbyssalSpectralMasteryTuning.Setting.ORBIT_DURATION_TICKS, 0);
        if (orbitUsed || nonReturning || duration <= 0 || target.isRemoved()
                || (tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & ORBIT_MODE) == 0) return;
        orbitUsed = true;
        orbitTarget = target;
        orbitStartAge = age;
        orbitEndAge = age + duration;
        int interval = orbitInterval(tuning);
        nextOrbitStrike = WickpiercerThrowMath.nextStrikeAge(age, interval);
        orbitPhase = initialOrbitPhase(target);
        setOrbitPresentation(target, duration, interval, orbitPhase);
    }

    private float initialOrbitPhase(LivingEntity target) {
        Vec3d direction = getVelocity().multiply(1.0, 0.0, 1.0);
        if (direction.lengthSquared() < 1.0E-6 && getOwner() instanceof LivingEntity owner) {
            direction = target.getPos().subtract(owner.getPos()).multiply(1.0, 0.0, 1.0);
        }
        if (direction.lengthSquared() < 1.0E-6) {
            return 0.0F;
        }
        return (float) Math.atan2(direction.z, direction.x);
    }

    private int orbitInterval(AbyssalSpectralMasteryTuning tuning) {
        return Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.INTERVAL_TICKS, 20));
    }

    private void tickOrbit() {
        if (orbitTarget == null) return;
        if (!isOrbiting()) {
            orbitTarget = null;
            clearOrbitPresentation();
            setNoClip(true);
            return;
        }
        AbyssalSpectralMasteryTuning tuning = tuning();
        int interval = orbitInterval(tuning);
        Vec3d offset = WickpiercerThrowMath.orbitOffset(
                age - orbitStartAge, interval, orbitPhase, WickpiercerThrowMath.DEFAULT_ORBIT_RADIUS);
        setPos(orbitTarget.getX() + offset.x,
                orbitTarget.getBodyY(0.55) + offset.y,
                orbitTarget.getZ() + offset.z);
        if (age < nextOrbitStrike || !(getWorld() instanceof ServerWorld world)
                || !(getOwner() instanceof LivingEntity owner)) return;
        nextOrbitStrike = WickpiercerThrowMath.nextStrikeAge(age, interval);
        float damage = primaryBaseDamage
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 0);
        if (damage <= 0 || !HelperMethods.checkAbilityTarget(orbitTarget, owner)) return;
        if (SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, orbitTarget,
                damage, SpellScalingProfile.FIRE)) {
            world.spawnParticles(ParticleTypes.SMALL_FLAME,
                    orbitTarget.getX(), orbitTarget.getBodyY(0.55), orbitTarget.getZ(),
                    5, 0.18, 0.18, 0.18, 0.015);
            world.spawnParticles(ParticleTypes.WAX_OFF,
                    orbitTarget.getX(), orbitTarget.getBodyY(0.55), orbitTarget.getZ(),
                    3, 0.14, 0.16, 0.14, 0.01);
        }
    }

    private void tickDelayedReturn() {
        int delay = tuning().integer(AbyssalSpectralMasteryTuning.Setting.RETURN_DELAY_TICKS, 0);
        if (loyaltyRestored || nonReturning || delay <= 0 || hasLoyalty > 0 || age < delay) return;
        loyaltyRestored = true;
        hasLoyalty = 3;
        setNoClip(true);
    }

    private void burst(LivingEntity directTarget, float damage, AbyssalSpectralMasteryTuning tuning) {
        double radius = tuning.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_RADIUS, 0);
        int cap = tuning.integer(AbyssalSpectralMasteryTuning.Setting.IMPACT_TARGET_CAP, 0);
        float burstDamage = damage * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_DAMAGE_MULTIPLIER, 0);
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
        AbyssalSpectralMasteryTuning tuning = tuning();
        double radius = tuning.get(AbyssalSpectralMasteryTuning.Setting.RETURN_TRAIL_RADIUS, 0);
        float damage = primaryBaseDamage
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.RETURN_TRAIL_DAMAGE_MULTIPLIER, 0);
        int fireTicks = tuning.integer(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_FIRE_TICKS, 0);
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
        if (isOrbiting()) return false;
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
