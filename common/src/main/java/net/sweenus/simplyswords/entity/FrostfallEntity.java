package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.FrostfallAbilityManager;
import net.sweenus.simplyswords.world.FrostfallIceSpikeFieldManager;
import net.sweenus.simplyswords.world.StormFrostWaterMasteryCombatManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FrostfallEntity extends ThrownSwordEntity {
    public double detonateRadius = 8;
    public float detonateDamage = 11;
    public int duration = 80;
    public int addedChance;

    private StormFrostWaterMasteryTuning throwTuning = StormFrostWaterMasteryTuning.EMPTY;
    private StormFrostWaterMasteryTuning fieldTuning = StormFrostWaterMasteryTuning.EMPTY;
    private UniqueAbilityExecution throwExecution;
    private UniqueAbilityExecution fieldExecution;
    private final Set<UUID> outboundHits = new HashSet<>();
    private final Set<UUID> returnHits = new HashSet<>();
    private final Set<UUID> orbitHits = new HashSet<>();
    private final Map<UUID, Integer> pulseHits = new HashMap<>();
    private Vec3d launchPosition = Vec3d.ZERO;
    private Vec3d impactPosition = Vec3d.ZERO;
    private UUID markedTarget;
    private int markExpiresAt;
    private int impactAge = -1;
    private int pulseIndex;
    private int pulseCount = 5;
    private int fieldDuration = 100;
    private int returnStartedAge = -1;
    private int orbitAge = -1;
    private int configuredCooldown = 60;
    private float launchPitch;
    private boolean deadfall;
    private boolean directHit;
    private boolean fieldStarted;
    private boolean fieldEnded;
    private boolean fieldSnapshot;
    private boolean caught;
    private boolean previousSneaking;

    public FrostfallEntity(EntityType<? extends FrostfallEntity> entityType, World world) {
        super(entityType, world);
    }

    public FrostfallEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }

    public void setMastery(StormFrostWaterMasteryTuning tuning, UniqueAbilityExecution execution) {
        throwTuning = tuning == null ? StormFrostWaterMasteryTuning.EMPTY : tuning;
        throwExecution = execution;
    }

    public void setFieldMastery(StormFrostWaterMasteryTuning tuning, UniqueAbilityExecution execution) {
        fieldTuning = tuning == null ? StormFrostWaterMasteryTuning.EMPTY : tuning;
        fieldExecution = execution;
        fieldSnapshot = true;
    }

    public void setLaunchState(Vec3d position, float pitch, int cooldown) {
        launchPosition = position == null ? getPos() : position;
        launchPitch = pitch;
        configuredCooldown = Math.max(0, cooldown);
        previousSneaking = getOwner() instanceof LivingEntity owner && owner.isSneaking();
        deadfall = launchPitch >= throwTuning.get(s("FROSTFALL_DEADFALL_ANGLE_DEGREES"), 91);
    }

    @Override
    public void tick() {
        if (!getWorld().isClient() && (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive())) {
            if (pickupType == PickupPermission.ALLOWED && !asItemStack().isEmpty()) dropStack(asItemStack(), .1F);
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected void doEffects(ServerWorld world, float baseDamage, Entity entity) {
        int bonusParticles = (int) baseDamage / 2;
        HelperMethods.spawnOrbitParticles(world, getPos(), ParticleTypes.POOF, .5f, 3 + bonusParticles);
        HelperMethods.spawnOrbitParticles(world, getPos(), ParticleTypes.CRIT, .5f, 5 + bonusParticles);
        HelperMethods.spawnOrbitParticles(world, getPos(), ParticleTypes.ITEM_SNOWBALL, .5f, 2 + bonusParticles);
        if (baseDamage > primaryBaseDamage) world.playSoundFromEntity(null, entity,
                SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(), getSoundCategory(), .3f, 1.2f);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.FROSTFALL.get());
    }

    @Override
    protected float doExtraDamage(Entity entity, float baseDamage, DamageSource damageSource) {
        boolean slowed = entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.SLOWNESS);
        float tuned = baseDamage * (float) FrostfallAbilityManager.resolveDirectMultiplier(
                throwTuning, age, slowed, deadfall);
        return super.doExtraDamage(entity, tuned, damageSource);
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        if (getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity target
                && !HelperMethods.checkAbilityTarget(target, owner)) return;
        boolean wasNonReturning = nonReturning;
        nonReturning = false;
        super.onEntityHit(hitResult);
        nonReturning = wasNonReturning;
        if (entity instanceof LivingEntity target && getOwner() instanceof LivingEntity owner) {
            directHit = true;
            outboundHits.add(target.getUuid());
            applyDirectEffects(owner, target);
            splinter((ServerWorld) getWorld(), owner, target);
            if (throwExecution != null) UniqueAbilityApi.emit(throwExecution, UniqueAbilityPhase.HIT,
                    StormFrostWaterMasteryAbilities.HIT, target, 1, primaryBaseDamage);
        }
        if (!isRemoved()) {
            inGround = true;
            setVelocity(Vec3d.ZERO);
            velocityModified = true;
            startField(entity instanceof LivingEntity living ? living : null);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (!getWorld().isClient()) startField(null);
    }

    private void applyDirectEffects(LivingEntity owner, LivingEntity target) {
        int slow = throwTuning.integer(s("FROSTFALL_DIRECT_SLOW_TICKS"), 0);
        if (slow > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0), owner);
        int freeze = throwTuning.integer(s("FROSTFALL_DIRECT_FREEZE_TICKS"),
                throwTuning.integer(s("FREEZE_TICKS"), 0));
        if (freeze > 0) target.setFrozenTicks(Math.min(target.getMinFreezeDamageTicks(),
                target.getFrozenTicks() + freeze));
        double range = throwTuning.get(s("FROSTFALL_MARK_RANGE"), 0);
        if (range > 0 && launchPosition.squaredDistanceTo(target.getPos()) >= range * range) {
            markedTarget = target.getUuid();
            markExpiresAt = age + throwTuning.integer(s("FROSTFALL_MARK_DURATION_TICKS"), 100);
        }
    }

    private void splinter(ServerWorld world, LivingEntity owner, LivingEntity directTarget) {
        int count = throwTuning.integer(s("FROSTFALL_SPLINTER_COUNT"), 0);
        double range = throwTuning.get(s("FROSTFALL_SPLINTER_RANGE"), 0);
        double multiplier = throwTuning.get(s("FROSTFALL_SPLINTER_DAMAGE_MULTIPLIER"), 0);
        if (count <= 0 || range <= 0 || multiplier <= 0) return;
        DamageSource source = getDamageSources().trident(this, owner);
        List<LivingEntity> targets = targets(world, getPos(), range, owner, count, Set.of(directTarget.getUuid()));
        for (LivingEntity target : targets) damage(target, source, baseImpactDamage() * (float) multiplier);
    }

    private void startField(LivingEntity directTarget) {
        if (fieldStarted || !(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)) return;
        fieldStarted = true;
        impactAge = age;
        impactPosition = getPos();
        if (fieldExecution == null) {
            fieldExecution = StormFrostWaterMasteryCombatManager.beginPassive(StormFrostWaterMasteryAbilities.FROSTFALL_FIELD,
                    world, stack, owner, directTarget);
            if (!fieldSnapshot) fieldTuning = StormFrostWaterMasteryAbilities.tuning(fieldExecution);
        } else {
            UniqueAbilityApi.start(fieldExecution);
        }
        pulseCount = FrostfallAbilityManager.resolvePulseCount(throwTuning, fieldTuning);
        fieldDuration = FrostfallAbilityManager.resolveFieldDuration(throwTuning, fieldTuning, pulseCount);
        if (pulseCount == 1 && fieldTuning.has(s("FROSTFALL_GLACIER_DELAY_TICKS"))) {
            FrostfallIceSpikeFieldManager.createPulse(world, impactPosition,
                    detonateRadius + fieldTuning.get(s("FROSTFALL_PULSE_RADIUS_BONUS"), 0), 0);
        }
        if (pulseCount <= 0) finishField();
    }

    @Override
    protected void doOnTick(Entity entity) {
        if (!(getWorld() instanceof ServerWorld world) || !(entity instanceof LivingEntity owner)) return;
        if (inGround && !fieldStarted) startField(null);
        boolean sneaking = owner.isSneaking();
        if (!fieldEnded && throwTuning.has(s("FROSTFALL_RECALL_DAMAGE_MULTIPLIER"))
                && sneaking && !previousSneaking) recall(world, owner);
        previousSneaking = sneaking;
        if (orbitAge >= 0) {
            tickOrbit(world, owner);
            return;
        }
        if (!fieldStarted || fieldEnded) return;
        int elapsed = age - impactAge;
        while (pulseIndex < pulseCount && elapsed >= FrostfallAbilityManager.pulseDueTick(
                fieldTuning, pulseIndex + 1, pulseCount, fieldDuration)) emitPulse(world, owner);
        if (pulseIndex >= pulseCount) finishField();
    }

    private void emitPulse(ServerWorld world, LivingEntity owner) {
        pulseIndex++;
        double radius = FrostfallAbilityManager.resolvePulseRadius(
                throwTuning, fieldTuning, detonateRadius, pulseIndex, pulseCount);
        int cap = fieldTuning.integer(s("FROSTFALL_PULSE_TARGET_CAP"), fieldTuning.has(s("TARGET_CAP"))
                ? fieldTuning.integer(s("TARGET_CAP"), 64) : throwTuning.integer(s("TARGET_CAP"), 64));
        List<LivingEntity> targets = targets(world, impactPosition, radius, owner, cap, Set.of());
        DamageSource source = getDamageSources().trident(this, owner);
        int affected = 0;
        for (LivingEntity target : targets) {
            int priorHits = pulseHits.getOrDefault(target.getUuid(), 0);
            boolean everyPulse = priorHits == pulseIndex - 1;
            boolean finalPulse = pulseIndex == pulseCount;
            if (finalPulse && everyPulse && fieldTuning.has(s("FROSTFALL_PERMAFROST_DURATION_TICKS"))) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        fieldTuning.integer(s("FROSTFALL_PERMAFROST_DURATION_TICKS"), 60),
                        fieldTuning.integer(s("FROSTFALL_PERMAFROST_AMPLIFIER"), 2)), owner);
            }
            StatusEffectInstance slowness = target.getStatusEffect(StatusEffects.SLOWNESS);
            boolean shatter = finalPulse && (target.isFrozen()
                    || slowness != null && slowness.getAmplifier() >= 2);
            double multiplier = FrostfallAbilityManager.resolvePulseMultiplier(
                    throwTuning, fieldTuning, pulseIndex, pulseCount, deadfall, shatter);
            if (markedTarget != null && markedTarget.equals(target.getUuid()) && age <= markExpiresAt)
                multiplier *= throwTuning.get(s("FROSTFALL_MARK_PULSE_DAMAGE_MULTIPLIER"), 1);
            if (damage(target, source, detonateDamage * (float) multiplier)) affected++;
            int slowTicks = FrostfallAbilityManager.resolvePulseSlowTicks(throwTuning, fieldTuning, duration);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks,
                    Math.min(3, pulseIndex)), owner);
            pulseHits.put(target.getUuid(), everyPulse ? pulseIndex : -64);
            pull(target, impactPosition, fieldPullStrength());
        }
        spike(world, owner, source, targets);
        playPulseEffects(world, radius);
        if (fieldExecution != null) UniqueAbilityApi.emit(fieldExecution, UniqueAbilityPhase.HIT,
                StormFrostWaterMasteryAbilities.PULSE, null, affected, detonateDamage);
    }

    private double fieldPullStrength() {
        double generic = fieldTuning.has(s("PULL_STRENGTH"))
                ? fieldTuning.get(s("PULL_STRENGTH"), 1) : throwTuning.get(s("PULL_STRENGTH"), 1);
        return generic
                * fieldTuning.get(s("FROSTFALL_PULSE_PULL_MULTIPLIER"), 1)
                * fieldTuning.get(s("FROSTFALL_AVALANCHE_PULL_MULTIPLIER"), 1)
                * fieldTuning.get(s("FROSTFALL_GLACIER_PULL_MULTIPLIER"), 1);
    }

    private void spike(ServerWorld world, LivingEntity owner, DamageSource source, List<LivingEntity> targets) {
        int configuredIndex = fieldTuning.integer(s("FROSTFALL_SPIKE_PULSE_INDEX"), 0);
        if (configuredIndex <= 0 || pulseIndex != configuredIndex && pulseCount != 1) return;
        int count = Math.min(fieldTuning.integer(s("FROSTFALL_SPIKE_COUNT"), 0), targets.size());
        double spikeMultiplier = fieldTuning.get(s("FROSTFALL_SPIKE_DAMAGE_MULTIPLIER"), 0);
        for (int i = 0; i < count; i++) {
            LivingEntity target = targets.get(i);
            double pulseMultiplier = FrostfallAbilityManager.resolvePulseMultiplier(
                    throwTuning, fieldTuning, pulseIndex, pulseCount, deadfall, false);
            if (markedTarget != null && markedTarget.equals(target.getUuid()) && age <= markExpiresAt)
                pulseMultiplier *= throwTuning.get(s("FROSTFALL_MARK_PULSE_DAMAGE_MULTIPLIER"), 1);
            damage(target, source, detonateDamage * (float) (pulseMultiplier * spikeMultiplier));
            FrostfallIceSpikeFieldManager.createTargetBurst(world, target.getPos(), 1, 1.25F);
        }
    }

    private void playPulseEffects(ServerWorld world, double radius) {
        world.playSoundFromEntity(null, this, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                getSoundCategory(), .45f, .8f + (float) pulseIndex / Math.max(1, pulseCount) * .5f);
        HelperMethods.spawnOrbitParticles(world, impactPosition, ParticleTypes.POOF, (float) radius, 8);
        HelperMethods.spawnOrbitParticles(world, impactPosition, ParticleTypes.CRIT, (float) radius, 12);
        HelperMethods.spawnOrbitParticles(world, impactPosition, ParticleTypes.ITEM_SNOWBALL, (float) radius, 9);
        FrostfallIceSpikeFieldManager.createPulse(world, impactPosition, radius, pulseCount - pulseIndex + 1);
    }

    private void finishField() {
        if (fieldEnded) return;
        fieldEnded = true;
        if (fieldExecution != null) {
            UniqueAbilityApi.finish(fieldExecution, StormFrostWaterMasteryAbilities.FINISH, pulseIndex);
            fieldExecution = null;
        }
        int orbitDuration = throwTuning.integer(s("FROSTFALL_ORBIT_DURATION_TICKS"), 0);
        if (orbitDuration > 0) {
            orbitAge = 0;
            inGround = false;
            setNoClip(true);
        } else {
            returnToPlayer = true;
            inGround = false;
        }
    }

    private void tickOrbit(ServerWorld world, LivingEntity owner) {
        int durationTicks = Math.max(1, throwTuning.integer(s("FROSTFALL_ORBIT_DURATION_TICKS"), 20));
        double radius = throwTuning.get(s("FROSTFALL_ORBIT_RADIUS"), 5);
        double angle = Math.PI * 2 * orbitAge / durationTicks;
        setPos(impactPosition.x + Math.cos(angle) * radius, impactPosition.y + 1,
                impactPosition.z + Math.sin(angle) * radius);
        double multiplier = throwTuning.get(s("FROSTFALL_ORBIT_DAMAGE_MULTIPLIER"), 0);
        if (multiplier > 0) {
            DamageSource source = getDamageSources().trident(this, owner);
            for (LivingEntity target : targets(world, getPos(), 1.25, owner, 16, orbitHits)) {
                orbitHits.add(target.getUuid());
                damage(target, source, baseImpactDamage() * (float) multiplier);
            }
        }
        orbitAge++;
        if (orbitAge >= durationTicks) {
            orbitAge = -1;
            returnToPlayer = true;
        }
    }

    private void recall(ServerWorld world, LivingEntity owner) {
        if (fieldExecution != null) {
            if (fieldStarted) UniqueAbilityApi.finish(fieldExecution, StormFrostWaterMasteryAbilities.RECALL, pulseIndex);
            else UniqueAbilityApi.cancel(fieldExecution);
            fieldExecution = null;
        }
        fieldEnded = true;
        orbitAge = -1;
        DamageSource source = getDamageSources().trident(this, owner);
        double multiplier = throwTuning.get(s("FROSTFALL_RECALL_DAMAGE_MULTIPLIER"), 0);
        int affected = 0;
        for (LivingEntity target : lineTargets(world, getPos(), owner.getEyePos(), owner, 16)) {
            if (damage(target, source, baseImpactDamage() * (float) multiplier)) affected++;
        }
        if (throwExecution != null) UniqueAbilityApi.emit(throwExecution, UniqueAbilityPhase.HIT,
                StormFrostWaterMasteryAbilities.RECALL, null, affected, primaryBaseDamage * multiplier);
        inGround = false;
        setNoClip(true);
        returnToPlayer = true;
    }

    @Override
    protected void damageOnReturn(double ignoredRadius, float ignoredDamage) {
        if (!(getWorld() instanceof ServerWorld world) || !(getOwner() instanceof LivingEntity owner)) return;
        if (returnStartedAge < 0) returnStartedAge = age;
        double multiplier = throwTuning.get(s("FROSTFALL_RETURN_DAMAGE_MULTIPLIER"), 0);
        if (multiplier <= 0) return;
        int cap = throwTuning.integer(s("FROSTFALL_RETURN_TARGET_CAP"), 6);
        DamageSource source = getDamageSources().trident(this, owner);
        for (LivingEntity target : targets(world, getPos(), 1.25, owner,
                Math.max(0, cap - returnHits.size()), returnHits)) {
            returnHits.add(target.getUuid());
            damage(target, source, primaryBaseDamage * (float) FrostfallAbilityManager.resolveDirectMultiplier(
                    throwTuning, 0, false, false) * (float) multiplier);
            pull(target, owner.getPos(), throwTuning.get(s("FROSTFALL_RETURN_PULL_STRENGTH"), 0));
            if (outboundHits.contains(target.getUuid())) {
                int root = throwTuning.integer(s("FROSTFALL_RETURN_ROOT_TICKS"), 0);
                if (root > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, root, 255), owner);
            }
            if (throwExecution != null) UniqueAbilityApi.emit(throwExecution, UniqueAbilityPhase.HIT,
                    StormFrostWaterMasteryAbilities.RETURN_HIT, target, 1, primaryBaseDamage * multiplier);
        }
    }

    @Override
    protected double getReturnSpeedMultiplier() {
        return throwTuning.get(s("FROSTFALL_RETURN_SPEED_MULTIPLIER"),
                throwTuning.get(s("SPEED"), 1));
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        boolean pickedUp = super.tryPickup(player);
        if (pickedUp && !caught && isOwner(player)) {
            caught = true;
            int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(asItemStack(), player, configuredCooldown);
            int refund = directHit ? throwTuning.integer(s("FROSTFALL_CATCH_REFUND_TICKS"), 0) : 0;
            player.getItemCooldownManager().set(asItemStack().getItem(), Math.max(0, cooldown - refund));
            int speedTicks = throwTuning.integer(s("FROSTFALL_CATCH_SPEED_TICKS"), 0);
            if (speedTicks > 0) player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, speedTicks,
                    throwTuning.integer(s("FROSTFALL_CATCH_SPEED_AMPLIFIER"), 0)), player);
            int window = throwTuning.integer(s("FROSTFALL_PERFECT_RETURN_WINDOW_TICKS"), 0);
            if (window > 0 && returnStartedAge >= 0 && age - returnStartedAge <= window
                    && getWorld() instanceof ServerWorld world) {
                FrostfallAbilityManager.grantPerfectReturn(world, player,
                        throwTuning.get(s("FROSTFALL_PERFECT_RETURN_DAMAGE_MULTIPLIER"), 1),
                        throwTuning.integer(s("FROSTFALL_PERFECT_RETURN_DURATION_TICKS"), 80));
            }
            if (throwExecution != null) {
                UniqueAbilityApi.emit(throwExecution, UniqueAbilityPhase.HIT, StormFrostWaterMasteryAbilities.CATCH,
                        player, 1, 0);
                UniqueAbilityApi.finish(throwExecution, StormFrostWaterMasteryAbilities.FINISH, returnHits.size());
                throwExecution = null;
            }
        }
        return pickedUp;
    }

    private boolean damage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0) return false;
        float damage = HelperMethods.applyAbilityDamageEnchantments((ServerWorld) getWorld(), stack, target, source, amount);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        return damaged[0];
    }

    private float baseImpactDamage() {
        return primaryBaseDamage * (float) FrostfallAbilityManager.resolveDirectMultiplier(
                throwTuning, 0, false, false);
    }

    private static void pull(LivingEntity target, Vec3d destination, double strength) {
        if (strength <= 0 || target.getPos().squaredDistanceTo(destination) <= 1) return;
        Vec3d delta = destination.subtract(target.getPos()).multiply(strength / 8);
        target.setVelocity(delta.x, delta.y, delta.z);
        target.velocityModified = true;
    }

    private List<LivingEntity> targets(ServerWorld world, Vec3d center, double radius, LivingEntity owner,
                                       int cap, Set<UUID> excluded) {
        if (cap <= 0 || radius <= 0) return List.of();
        Box box = new Box(center.subtract(radius, radius / 3, radius), center.add(radius, radius / 3, radius));
        return world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(target -> !excluded.contains(target.getUuid()) && HelperMethods.checkAbilityTarget(target, owner))
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(center)))
                .limit(cap).toList();
    }

    private List<LivingEntity> lineTargets(ServerWorld world, Vec3d start, Vec3d end,
                                           LivingEntity owner, int cap) {
        Box box = new Box(start, end).expand(1.25);
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity target) || !HelperMethods.checkAbilityTarget(target, owner)) continue;
            if (distanceToSegmentSquared(target.getPos(), start, end) <= 1.5625) result.add(target);
        }
        result.sort(Comparator.comparingDouble(target -> target.squaredDistanceTo(start)));
        return result.stream().limit(cap).toList();
    }

    private static double distanceToSegmentSquared(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d segment = end.subtract(start);
        double length = segment.lengthSquared();
        if (length < 1.0E-6) return point.squaredDistanceTo(start);
        double t = Math.clamp(point.subtract(start).dotProduct(segment) / length, 0, 1);
        return point.squaredDistanceTo(start.add(segment.multiply(t)));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!getWorld().isClient()) {
            if (fieldExecution != null) UniqueAbilityApi.cancel(fieldExecution);
            if (throwExecution != null) UniqueAbilityApi.cancel(throwExecution);
            fieldExecution = null;
            throwExecution = null;
        }
        super.remove(reason);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("FrostfallThrowTuning", throwTuning.toNbt());
        nbt.put("FrostfallFieldTuning", fieldTuning.toNbt());
        nbt.putInt("FrostfallPulseIndex", pulseIndex);
        nbt.putInt("FrostfallPulseCount", pulseCount);
        nbt.putInt("FrostfallFieldDuration", fieldDuration);
        nbt.putInt("FrostfallImpactAge", impactAge);
        nbt.putBoolean("FrostfallFieldStarted", fieldStarted);
        nbt.putBoolean("FrostfallFieldEnded", fieldEnded);
        nbt.putBoolean("FrostfallDeadfall", deadfall);
        nbt.putBoolean("FrostfallDirectHit", directHit);
        nbt.putBoolean("FrostfallReturn", returnToPlayer);
        nbt.putBoolean("FrostfallNonReturning", nonReturning);
        nbt.putInt("FrostfallNonReturningMaxAge", nonReturningMaxAge);
        nbt.putInt("FrostfallReturnStartedAge", returnStartedAge);
        nbt.putInt("FrostfallOrbitAge", orbitAge);
        nbt.putInt("FrostfallConfiguredCooldown", configuredCooldown);
        nbt.putInt("FrostfallMarkExpiresAt", markExpiresAt);
        nbt.putFloat("FrostfallLaunchPitch", launchPitch);
        nbt.putFloat("FrostfallPrimaryDamage", primaryBaseDamage);
        nbt.putFloat("FrostfallPulseDamage", detonateDamage);
        nbt.putDouble("FrostfallRadius", detonateRadius);
        nbt.putInt("FrostfallSlowDuration", duration);
        nbt.putDouble("FrostfallLaunchX", launchPosition.x);
        nbt.putDouble("FrostfallLaunchY", launchPosition.y);
        nbt.putDouble("FrostfallLaunchZ", launchPosition.z);
        nbt.putDouble("FrostfallImpactX", impactPosition.x);
        nbt.putDouble("FrostfallImpactY", impactPosition.y);
        nbt.putDouble("FrostfallImpactZ", impactPosition.z);
        if (markedTarget != null) nbt.putUuid("FrostfallMarkedTarget", markedTarget);
        writeUuidSet(nbt, "FrostfallOutbound", outboundHits);
        writeUuidSet(nbt, "FrostfallReturnHits", returnHits);
        writeUuidSet(nbt, "FrostfallOrbitHits", orbitHits);
        nbt.putInt("FrostfallPulseHitCount", pulseHits.size());
        int pulseHitIndex = 0;
        for (Map.Entry<UUID, Integer> entry : pulseHits.entrySet()) {
            nbt.putUuid("FrostfallPulseHitTarget" + pulseHitIndex, entry.getKey());
            nbt.putInt("FrostfallPulseHitValue" + pulseHitIndex, entry.getValue());
            pulseHitIndex++;
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        throwTuning = StormFrostWaterMasteryTuning.fromNbt(nbt.getCompound("FrostfallThrowTuning"));
        fieldTuning = StormFrostWaterMasteryTuning.fromNbt(nbt.getCompound("FrostfallFieldTuning"));
        fieldSnapshot = nbt.contains("FrostfallFieldTuning");
        pulseIndex = nbt.getInt("FrostfallPulseIndex");
        pulseCount = nbt.getInt("FrostfallPulseCount");
        fieldDuration = nbt.getInt("FrostfallFieldDuration");
        impactAge = nbt.getInt("FrostfallImpactAge");
        fieldStarted = nbt.getBoolean("FrostfallFieldStarted");
        fieldEnded = nbt.getBoolean("FrostfallFieldEnded");
        deadfall = nbt.getBoolean("FrostfallDeadfall");
        directHit = nbt.getBoolean("FrostfallDirectHit");
        returnToPlayer = nbt.getBoolean("FrostfallReturn");
        nonReturning = nbt.getBoolean("FrostfallNonReturning");
        if (nbt.contains("FrostfallNonReturningMaxAge")) nonReturningMaxAge = nbt.getInt("FrostfallNonReturningMaxAge");
        if (nbt.contains("FrostfallReturnStartedAge")) returnStartedAge = nbt.getInt("FrostfallReturnStartedAge");
        if (nbt.contains("FrostfallOrbitAge")) orbitAge = nbt.getInt("FrostfallOrbitAge");
        if (nbt.contains("FrostfallConfiguredCooldown")) configuredCooldown = nbt.getInt("FrostfallConfiguredCooldown");
        if (nbt.contains("FrostfallMarkExpiresAt")) markExpiresAt = nbt.getInt("FrostfallMarkExpiresAt");
        if (nbt.contains("FrostfallLaunchPitch")) launchPitch = nbt.getFloat("FrostfallLaunchPitch");
        if (nbt.contains("FrostfallPrimaryDamage")) primaryBaseDamage = nbt.getFloat("FrostfallPrimaryDamage");
        if (nbt.contains("FrostfallPulseDamage")) detonateDamage = nbt.getFloat("FrostfallPulseDamage");
        if (nbt.contains("FrostfallRadius")) detonateRadius = nbt.getDouble("FrostfallRadius");
        if (nbt.contains("FrostfallSlowDuration")) duration = nbt.getInt("FrostfallSlowDuration");
        if (nbt.contains("FrostfallLaunchX")) launchPosition = new Vec3d(nbt.getDouble("FrostfallLaunchX"),
                nbt.getDouble("FrostfallLaunchY"), nbt.getDouble("FrostfallLaunchZ"));
        if (nbt.contains("FrostfallImpactX")) impactPosition = new Vec3d(nbt.getDouble("FrostfallImpactX"),
                nbt.getDouble("FrostfallImpactY"), nbt.getDouble("FrostfallImpactZ"));
        markedTarget = nbt.containsUuid("FrostfallMarkedTarget") ? nbt.getUuid("FrostfallMarkedTarget") : null;
        readUuidSet(nbt, "FrostfallOutbound", outboundHits);
        readUuidSet(nbt, "FrostfallReturnHits", returnHits);
        readUuidSet(nbt, "FrostfallOrbitHits", orbitHits);
        pulseHits.clear();
        for (int i = 0; i < nbt.getInt("FrostfallPulseHitCount"); i++) {
            String key = "FrostfallPulseHitTarget" + i;
            if (nbt.containsUuid(key)) pulseHits.put(nbt.getUuid(key), nbt.getInt("FrostfallPulseHitValue" + i));
        }
    }

    private static void writeUuidSet(NbtCompound nbt, String key, Set<UUID> values) {
        nbt.putInt(key + "Count", values.size());
        int index = 0;
        for (UUID value : values) nbt.putUuid(key + index++, value);
    }

    private static void readUuidSet(NbtCompound nbt, String key, Set<UUID> values) {
        values.clear();
        for (int i = 0; i < nbt.getInt(key + "Count"); i++) {
            if (nbt.containsUuid(key + i)) values.add(nbt.getUuid(key + i));
        }
    }

    @Override
    protected byte getLoyalty() {
        return getWorld() instanceof ServerWorld ? (byte) 3 : 0;
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
