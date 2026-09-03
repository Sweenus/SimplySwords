package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.LivyatanWaveManager;
import net.sweenus.simplyswords.world.WraithmawAbilityManager;
import net.sweenus.simplyswords.world.WraithmawTuningSnapshot;
import net.sweenus.simplyswords.world.GloamStainManager;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class WraithmawCutlassEntity extends Entity {
    public static final int STATE_MUSTER = 0;
    public static final int STATE_POSITIONING = 1;
    public static final int STATE_FALLING = 2;
    public static final int STATE_EMBEDDED = 3;
    public static final int STATE_ORBITING = 4;
    public static final int STATE_LAUNCHED = 5;

    private static final int MUSTER_TICKS = 12;
    private static final int POSITION_TICKS = 10;
    private static final int DETONATION_TARGET_CAP = 8;
    private static final TrackedData<Integer> STATE = DataTracker.registerData(
            WraithmawCutlassEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STATE_START_AGE = DataTracker.registerData(
            WraithmawCutlassEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(
            WraithmawCutlassEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ORBIT_SLOT = DataTracker.registerData(
            WraithmawCutlassEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(
            WraithmawCutlassEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> WEAPON_STACK = DataTracker.registerData(
            WraithmawCutlassEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final DustColorTransitionParticleEffect SPECTRAL_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.08F, 0.01F, 0.16F),
                    new Vector3f(0.58F, 0.10F, 0.92F), 1.15F);

    private UUID ownerUuid;
    private Vec3d castOrigin = Vec3d.ZERO;
    private Vec3d landing = Vec3d.ZERO;
    private int sequence;
    private float damage;
    private long expiresAtTick;
    private double traveled;
    private boolean struckEntity;
    private boolean impactBurstUsed;
    private boolean launchMultiplierApplied;
    private boolean loadedFromNbt;
    private boolean gravewalkStruck;
    private int missingOwnerTicks;
    private WraithmawTuningSnapshot tuning = WraithmawTuningSnapshot.from(null);
    private @Nullable UniqueAbilityExecution abilityExecution;

    public WraithmawCutlassEntity(EntityType<? extends WraithmawCutlassEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public WraithmawCutlassEntity(ServerWorld world, LivingEntity owner, ItemStack stack,
                                  Vec3d castOrigin, Vec3d landing, int sequence, float damage) {
        this(world, owner, stack, castOrigin, landing, sequence, damage, null);
    }

    public WraithmawCutlassEntity(ServerWorld world, LivingEntity owner, ItemStack stack,
                                  Vec3d castOrigin, Vec3d landing, int sequence, float damage,
                                  @Nullable UniqueAbilityExecution execution) {
        this(EntityRegistry.WRAITHMAW_CUTLASS.get(), world);
        this.ownerUuid = owner.getUuid();
        this.setOwnerId(owner.getId());
        this.setWeaponStack(stack);
        this.castOrigin = castOrigin;
        this.landing = landing;
        this.sequence = Math.max(0, sequence);
        this.damage = Math.max(0.0F, damage);
        this.setSeed(world.random.nextInt());
        this.setPosition(castOrigin);
        this.abilityExecution = execution;
        this.tuning = WraithmawTuningSnapshot.from(execution);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STATE, STATE_MUSTER);
        builder.add(STATE_START_AGE, 0);
        builder.add(OWNER_ID, -1);
        builder.add(ORBIT_SLOT, -1);
        builder.add(SEED, 0);
        builder.add(WEAPON_STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (this.getWorld().isClient() || !(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (loadedFromNbt) {
            loadedFromNbt = false;
            if (getState() != STATE_EMBEDDED && getState() != STATE_ORBITING) {
                discard();
                return;
            }
        }
        switch (getState()) {
            case STATE_MUSTER -> tickMuster(world);
            case STATE_POSITIONING -> tickPositioning(world);
            case STATE_FALLING -> tickFalling(world);
            case STATE_EMBEDDED -> tickEmbedded(world);
            case STATE_ORBITING -> tickOrbiting(world);
            case STATE_LAUNCHED -> tickLaunched(world);
            default -> discard();
        }
    }

    private void tickMuster(ServerWorld world) {
        LivingEntity owner = resolveOwner(world);
        if (owner == null) {
            discard();
            return;
        }
        double phase = sequence * 2.399963229728653 + age * 0.31;
        double radius = 0.75 + age * 0.075;
        double height = 1.15 + sequence % 4 * 0.23 + Math.sin(age * 0.42 + sequence) * 0.14;
        setPosition(owner.getX() + Math.cos(phase) * radius,
                owner.getY() + height, owner.getZ() + Math.sin(phase) * radius);
        if (age % 3 == 0) {
            world.spawnParticles(SPECTRAL_DUST, getX(), getY(), getZ(), 1, 0.03, 0.03, 0.03, 0.0);
        }
        if (age >= tuning.materializeTicks()) {
            setState(STATE_POSITIONING);
        }
    }

    private void tickPositioning(ServerWorld world) {
        int stateAge = getStateAge();
        Vec3d ring = musterAnchor();
        Vec3d overhead = landing.add(0.0, 8.0 + Math.floorMod(getSeed(), 5) * 0.32, 0.0);
        if (stateAge < POSITION_TICKS) {
            double progress = easeOut(stateAge / (double) POSITION_TICKS);
            Vec3d arc = ring.lerp(overhead, progress).add(0.0, Math.sin(progress * Math.PI) * 2.0, 0.0);
            setPosition(arc);
        } else {
            double hover = Math.sin((stateAge - POSITION_TICKS) * 0.42 + sequence) * 0.13;
            double angle = sequence * 1.73 + stateAge * 0.08;
            setPosition(overhead.add(Math.cos(angle) * 0.18, hover, Math.sin(angle) * 0.18));
        }
        if (age % 2 == 0) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 1,
                    0.04, 0.04, 0.04, 0.01);
        }
        int sequenceDelay = tuning.fallIntervalTicks();
        if (stateAge >= POSITION_TICKS + sequence * sequenceDelay) {
            setVelocity(0.0, -tuning.fallSpeed(), 0.0);
            setState(STATE_FALLING);
        }
    }

    private void tickFalling(ServerWorld world) {
        LivingEntity owner = resolveOwner(world);
        if (owner == null) {
            discard();
            return;
        }
        Vec3d current = getPos();
        Vec3d correction = new Vec3d(landing.x - current.x, 0.0, landing.z - current.z).multiply(0.2);
        Vec3d velocity = new Vec3d(
                MathHelper.clamp(correction.x, -0.18, 0.18),
                -tuning.fallSpeed(),
                MathHelper.clamp(correction.z, -0.18, 0.18));
        Vec3d next = current.add(velocity);
        BlockHitResult blockHit = world.raycast(new RaycastContext(current, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        Vec3d segmentEnd = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getPos();
        if (!struckEntity) {
            LivingEntity target = findCollisionTarget(world, owner, current, segmentEnd);
            if (target != null) {
                struckEntity = true;
                ItemStack stack = getWeaponStack();
                float amount = gloamAdjustedDamage(world, target);
                if (!stack.isEmpty() && SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, amount)) {
                    setWeaponStack(stack);
                    recordHit(world, target, amount);
                    spawnEntityImpact(world, target);
                    burst(world, owner, target.getPos(), target);
                }
            }
        }
        setPosition(segmentEnd);
        setVelocity(velocity);
        if (age % 2 == 0) {
            world.spawnParticles(SPECTRAL_DUST, current.x, current.y, current.z,
                    2, 0.06, 0.18, 0.06, 0.0);
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            burst(world, owner, blockHit.getPos(), null);
            embed(world, blockHit.getPos());
        } else if (getStateAge() > 100 || getY() < world.getBottomY() - 4) {
            discard();
        }
    }

    private void tickEmbedded(ServerWorld world) {
        if (world.getTime() >= expiresAtTick) {
            dissipate(world, getPos(), false);
            return;
        }
        LivingEntity owner = resolveOwner(world);
        if (owner == null) {
            return;
        }
        if (tuning.hauntsEnemies()) {
            tickHaunt(world, owner);
        }
        if (tuning.walksToEnemies()) {
            tickGravewalk(world, owner);
            return;
        }
        double recovery = tuning.recoveryRadius();
        if (owner.squaredDistanceTo(this) <= recovery * recovery
                && WraithmawAbilityManager.tryRecover(world, owner, this)) {
            onRecovered(world, owner);
        }
    }

    private void onRecovered(ServerWorld world, LivingEntity owner) {
        world.spawnParticles(SPECTRAL_DUST, getX(), getY() + 0.35, getZ(),
                18, 0.28, 0.34, 0.28, 0.02);
        world.playSound(null, getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.55F, 1.45F);
        if (abilityExecution != null) {
            UniqueAbilityApi.emit(abilityExecution,
                    net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    AbyssalSpectralMasteryAbilities.RECOVER, null, 1, 0.0);
        }
        if (!tuning.pullsOnRecovery()) {
            return;
        }
        Vec3d center = getPos();
        double range = tuning.recallRange();
        List<LivingEntity> pulled = gloamTargets(world, owner, center, range, tuning.recallTargetCap());
        for (LivingEntity target : pulled) {
            Vec3d offset = center.subtract(target.getPos());
            if (offset.lengthSquared() < 1.0E-6) {
                continue;
            }
            Vec3d step = offset.normalize().multiply(
                    Math.min(tuning.recallPullStrength(), offset.length()));
            target.setPosition(target.getPos().add(step.x, 0.0, step.z));
            target.velocityModified = true;
            world.spawnParticles(SPECTRAL_DUST, target.getX(), target.getY() + 0.4, target.getZ(),
                    5, 0.18, 0.24, 0.18, 0.02);
        }
    }

    private void tickHaunt(ServerWorld world, LivingEntity owner) {
        int stateAge = getStateAge();
        if (stateAge <= 0 || stateAge % tuning.hauntIntervalTicks() != 0) {
            return;
        }
        LivingEntity target = gloamTargets(world, owner, getPos(),
                tuning.hauntRange(), tuning.hauntTargetCap()).stream().findFirst().orElse(null);
        if (target == null) {
            return;
        }
        ItemStack stack = getWeaponStack();
        float amount = damage * (float) tuning.hauntDamageMultiplier();
        if (!stack.isEmpty() && amount > 0
                && SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, amount)) {
            setWeaponStack(stack);
            recordHit(world, target, amount);
            spawnEntityImpact(world, target);
        }
    }

    private void tickGravewalk(ServerWorld world, LivingEntity owner) {
        LivingEntity target = gloamTargets(world, owner, getPos(),
                tuning.gravewalkRange(), 8).stream().findFirst().orElse(null);
        if (target == null) {
            return;
        }
        Vec3d offset = target.getPos().subtract(getPos());
        double horizontal = offset.horizontalLength();
        if (horizontal <= Math.max(0.6, target.getWidth() * 0.5 + 0.4)) {
            strikeGravewalk(world, owner, target);
            return;
        }
        Vec3d step = new Vec3d(offset.x, 0.0, offset.z).normalize()
                .multiply(Math.min(tuning.gravewalkSpeed(), horizontal));
        double x = getX() + step.x;
        double z = getZ() + step.z;
        setPosition(x, LivyatanWaveManager.findGroundTopY(world, x, z, getY() + 1.5) + 0.16, z);
        if (age % 3 == 0) {
            world.spawnParticles(SPECTRAL_DUST, getX(), getY() + 0.2, getZ(),
                    2, 0.1, 0.12, 0.1, 0.01);
        }
    }

    private void strikeGravewalk(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (gravewalkStruck) {
            return;
        }
        gravewalkStruck = true;
        ItemStack stack = getWeaponStack();
        float amount = damage * (float) tuning.gravewalkDamageMultiplier();
        if (!stack.isEmpty() && amount > 0
                && SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, amount)) {
            setWeaponStack(stack);
            recordHit(world, target, amount);
            spawnEntityImpact(world, target);
        }
        dissipate(world, target.getPos().add(0.0, target.getHeight() * 0.4, 0.0), true);
    }

    private List<LivingEntity> gloamTargets(ServerWorld world, LivingEntity owner, Vec3d center,
                                            double range, int cap) {
        if (range <= 0 || cap <= 0 || ownerUuid == null) {
            return List.of();
        }
        return world.getEntitiesByClass(LivingEntity.class,
                        Box.of(center, range * 2.0, range * 2.0, range * 2.0),
                        candidate -> candidate != owner && candidate.isAlive() && !candidate.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                                && HelperMethods.checkAbilityTarget(candidate, owner)
                                && candidate.squaredDistanceTo(center) <= range * range
                                && GloamStainManager.isOnOwnerGloam(world, ownerUuid, candidate))
                .stream()
                .sorted(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)))
                .limit(cap)
                .toList();
    }

    private void tickOrbiting(ServerWorld world) {
        LivingEntity owner = resolveOwner(world);
        if (world.getTime() >= expiresAtTick) {
            dissipate(world, getPos(), false);
            return;
        }
        if (owner == null) {
            setOwnerId(-1);
            if (++missingOwnerTicks >= 100) dissipate(world, getPos(), false);
            return;
        }
        missingOwnerTicks = 0;
        setPosition(owner.getX(), owner.getY(), owner.getZ());
        setOwnerId(owner.getId());
        if (age % 8 == 0) {
            Vec3d display = WraithmawAbilityManager.orbitPosition(owner, getOrbitSlot(), world.getTime());
            world.spawnParticles(SPECTRAL_DUST, display.x, display.y, display.z,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void tickLaunched(ServerWorld world) {
        LivingEntity owner = resolveOwner(world);
        if (owner == null) {
            discard();
            return;
        }
        Vec3d current = getPos();
        Vec3d velocity = getVelocity();
        if (velocity.lengthSquared() < 1.0E-6) {
            dissipate(world, current, false);
            return;
        }
        LivingEntity homingTarget = findHomingTarget(world, owner, current, velocity.normalize());
        if (homingTarget != null) {
            Vec3d desired = homingTarget.getPos().add(0.0, homingTarget.getHeight() * 0.58, 0.0)
                    .subtract(current).normalize();
            Vec3d turned = turnToward(velocity.normalize(), desired,
                    Math.toRadians(tuning.homingTurnDegrees()));
            velocity = turned.multiply(tuning.launchSpeed());
            setVelocity(velocity);
        }
        Vec3d next = current.add(velocity);
        BlockHitResult blockHit = world.raycast(new RaycastContext(current, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        Vec3d segmentEnd = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getPos();
        LivingEntity target = findCollisionTarget(world, owner, current, segmentEnd);
        if (target != null) {
            ItemStack stack = getWeaponStack();
            float amount = gloamAdjustedDamage(world, target);
            if (!stack.isEmpty() && SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, amount)) {
                setWeaponStack(stack);
                spawnEntityImpact(world, target);
                recordHit(world, target, amount);
            }
            dissipate(world, target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), true);
            return;
        }
        traveled += current.distanceTo(segmentEnd);
        setPosition(segmentEnd);
        if (age % 2 == 0) {
            world.spawnParticles(SPECTRAL_DUST, current.x, current.y, current.z,
                    2, 0.05, 0.05, 0.05, 0.0);
        }
        if (blockHit.getType() != HitResult.Type.MISS
                || traveled >= tuning.launchRange()
                || getStateAge() > tuning.projectileLifetimeTicks()) {
            dissipate(world, segmentEnd, blockHit.getType() != HitResult.Type.MISS);
        }
    }

    private LivingEntity findCollisionTarget(ServerWorld world, LivingEntity owner,
                                              Vec3d start, Vec3d end) {
        double grace = Math.max(0.0, Config.uniqueEffects.wraithmaw.collisionGrace);
        Box search = new Box(start, end).expand(grace + 0.6);
        return world.getEntitiesByClass(LivingEntity.class, search,
                        target -> target != owner && target.isAlive() && !target.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                                && HelperMethods.checkAbilityTarget(target, owner)
                                && target.getBoundingBox().expand(target.getTargetingMargin() + grace)
                                .raycast(start, end).isPresent())
                .stream()
                .min(Comparator.comparingDouble(target -> target.squaredDistanceTo(start)))
                .orElse(null);
    }

    private LivingEntity findHomingTarget(ServerWorld world, LivingEntity owner,
                                          Vec3d origin, Vec3d direction) {
        double range = tuning.homingRange();
        if (range <= 0.0) {
            return null;
        }
        return world.getEntitiesByClass(LivingEntity.class, Box.of(origin, range * 2.0, range * 2.0, range * 2.0),
                        target -> target != owner && target.isAlive() && !target.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                                && HelperMethods.checkAbilityTarget(target, owner))
                .stream()
                .filter(target -> {
                    Vec3d offset = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(origin);
                    double ahead = offset.dotProduct(direction);
                    if (ahead <= 0.0) {
                        return false;
                    }
                    Vec3d lateral = offset.subtract(direction.multiply(ahead));
                    return lateral.lengthSquared() <= range * range;
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3d offset = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(origin);
                    double ahead = offset.dotProduct(direction);
                    return offset.subtract(direction.multiply(ahead)).lengthSquared() + ahead * 0.04;
                }))
                .orElse(null);
    }

    private void embed(ServerWorld world, Vec3d impact) {
        createGloam(world, impact);
        if (tuning.hasMode(WraithmawTuningSnapshot.MODE_NO_EMBED)) {
            dissipate(world, impact, true);
            return;
        }
        setPosition(impact.add(0.0, 0.16, 0.0));
        setVelocity(Vec3d.ZERO);
        setState(STATE_EMBEDDED);
        expiresAtTick = world.getTime() + tuning.embeddedDurationTicks();
        world.spawnParticles(SPECTRAL_DUST, impact.x, impact.y + 0.12, impact.z,
                16, 0.38, 0.12, 0.38, 0.04);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, impact.x, impact.y + 0.18, impact.z,
                5, 0.24, 0.14, 0.24, 0.015);
        world.playSound(null, impact.x, impact.y, impact.z, SoundRegistry.OBJECT_IMPACT_THUD.get(),
                SoundCategory.PLAYERS, 0.68F, 1.25F + world.random.nextFloat() * 0.12F);
    }

    private void createGloam(ServerWorld world, Vec3d impact) {
        GloamStainManager.createPatch(world, ownerUuid, impact, tuning.stainRadius(),
                tuning.stainDurationTicks(),
                Math.max(1, Config.uniqueEffects.wraithmaw.stainFadeDuration),
                tuning.stainAmplifier(),
                new GloamStainManager.PatchBehavior(getWeaponStack(), damage,
                        tuning.slowDurationTicks(), tuning.appliesSlowness(),
                        0, 0, 0, 0, 0));
    }

    private void spawnEntityImpact(ServerWorld world, LivingEntity target) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        world.spawnParticles(SPECTRAL_DUST, center.x, center.y, center.z,
                14, 0.28, 0.3, 0.28, 0.05);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z,
                8, 0.24, 0.2, 0.24, 0.02);
        world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_02.get(),
                SoundCategory.PLAYERS, 0.52F, 1.25F + world.random.nextFloat() * 0.16F);
    }

    private void burst(ServerWorld world, LivingEntity owner, Vec3d center, @Nullable LivingEntity directTarget) {
        double radius = tuning.impactRadius();
        int cap = tuning.impactTargetCap();
        float burstDamage = damage * (float) tuning.impactDamageMultiplier();
        if (impactBurstUsed || radius <= 0 || cap <= 0 || burstDamage <= 0) return;
        impactBurstUsed = true;
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                new Box(center, center).expand(radius), candidate -> candidate != owner
                        && candidate != directTarget && candidate.isAlive()
                        && candidate.squaredDistanceTo(center) <= radius * radius
                        && HelperMethods.checkAbilityTarget(candidate, owner));
        targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));
        ItemStack stack = getWeaponStack();
        for (int index = 0; index < Math.min(cap, targets.size()); index++) {
            SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, owner, stack, targets.get(index),
                    burstDamage, SpellScalingProfile.SOUL);
        }
    }

    private void dissipate(ServerWorld world, Vec3d position, boolean impact) {
        world.spawnParticles(SPECTRAL_DUST, position.x, position.y, position.z,
                impact ? 12 : 7, 0.2, 0.2, 0.2, 0.025);
        if (impact) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.28F, 1.65F);
        }
        discard();
    }

    private float gloamAdjustedDamage(ServerWorld world, LivingEntity target) {
        if (tuning.gloamVulnerabilityBonus() <= 0 || ownerUuid == null
                || !GloamStainManager.isOnOwnerGloam(world, ownerUuid, target)) {
            return damage;
        }
        return damage * (float) (1 + tuning.gloamVulnerabilityBonus());
    }

    private void recordHit(ServerWorld world, LivingEntity target, float amount) {
        if (abilityExecution != null) UniqueAbilityApi.emit(abilityExecution,
                net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                AbyssalSpectralMasteryAbilities.HIT, target, 1, amount);
        if (target.isAlive()) {
            return;
        }
        if (abilityExecution != null) UniqueAbilityApi.emit(abilityExecution,
                net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                AbyssalSpectralMasteryAbilities.KILL, target, 1, amount);
        if (tuning.extendsGloamOnKill()) {
            GloamStainManager.extendPatches(world, ownerUuid, target.getPos(), tuning.burialRange(),
                    tuning.stainExtensionTicks(), tuning.extraDurationCapTicks());
        }
    }

    private Vec3d musterAnchor() {
        double phase = sequence * 2.399963229728653 + MUSTER_TICKS * 0.31;
        double radius = 0.75 + MUSTER_TICKS * 0.075;
        double height = 1.15 + sequence % 4 * 0.23
                + Math.sin(MUSTER_TICKS * 0.42 + sequence) * 0.14;
        return castOrigin.add(Math.cos(phase) * radius, height, Math.sin(phase) * radius);
    }

    private static double easeOut(double progress) {
        double clamped = MathHelper.clamp(progress, 0.0, 1.0);
        return 1.0 - (1.0 - clamped) * (1.0 - clamped);
    }

    private static Vec3d turnToward(Vec3d current, Vec3d desired, double maximumAngle) {
        double dot = MathHelper.clamp(current.dotProduct(desired), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maximumAngle || angle < 1.0E-5) {
            return desired;
        }
        double progress = maximumAngle / angle;
        return current.multiply(1.0 - progress).add(desired.multiply(progress)).normalize();
    }

    private LivingEntity resolveOwner(ServerWorld world) {
        if (ownerUuid == null) {
            return null;
        }
        Entity owner = getOwnerId() >= 0 ? world.getEntityById(getOwnerId()) : null;
        if (owner == null || !ownerUuid.equals(owner.getUuid())) {
            owner = world.getEntity(ownerUuid);
        }
        if (owner instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            setOwnerId(living.getId());
            return living;
        }
        return null;
    }

    public void detonate(ServerWorld world, LivingEntity owner) {
        Vec3d center = getPos();
        ItemStack stack = getWeaponStack();
        float amount = damage * (float) tuning.burstDamageMultiplier();
        double radius = Math.max(0.5, tuning.stainRadius());
        if (!stack.isEmpty() && amount > 0) {
            List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                            Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0),
                            candidate -> candidate != owner && candidate.isAlive() && !candidate.isRemoved()
                                    && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                                    && HelperMethods.checkAbilityTarget(candidate, owner)
                                    && candidate.squaredDistanceTo(center) <= radius * radius)
                    .stream()
                    .sorted(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)))
                    .limit(DETONATION_TARGET_CAP)
                    .toList();
            for (LivingEntity target : targets) {
                if (SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, amount)) {
                    recordHit(world, target, amount);
                }
            }
            setWeaponStack(stack);
        }
        world.spawnParticles(SPECTRAL_DUST, center.x, center.y + 0.3, center.z,
                24, 0.5, 0.35, 0.5, 0.08);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.4, center.z,
                8, 0.35, 0.3, 0.35, 0.03);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                SoundCategory.PLAYERS, 0.7F, 0.72F);
        dissipate(world, center, true);
    }

    public void recover(int slot, long expiresAtTick) {
        setOrbitSlot(slot);
        this.expiresAtTick = expiresAtTick;
        setVelocity(Vec3d.ZERO);
        setState(STATE_ORBITING);
    }

    public void launch(Vec3d origin, Vec3d direction, double speed) {
        if (!launchMultiplierApplied) {
            damage *= (float) tuning.projectileDamageMultiplier();
            launchMultiplierApplied = true;
        }
        setPosition(origin);
        setVelocity(direction.normalize().multiply(Math.max(0.1, speed)));
        traveled = 0.0;
        struckEntity = false;
        setState(STATE_LAUNCHED);
    }

    public int getState() {
        return dataTracker.get(STATE);
    }

    private void setState(int state) {
        dataTracker.set(STATE, Math.clamp(state, STATE_MUSTER, STATE_LAUNCHED));
        dataTracker.set(STATE_START_AGE, age);
    }

    public int getStateAge() {
        return Math.max(0, age - dataTracker.get(STATE_START_AGE));
    }

    public int getOwnerId() {
        return dataTracker.get(OWNER_ID);
    }

    private void setOwnerId(int ownerId) {
        dataTracker.set(OWNER_ID, ownerId);
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getOrbitSlot() {
        return dataTracker.get(ORBIT_SLOT);
    }

    private void setOrbitSlot(int slot) {
        dataTracker.set(ORBIT_SLOT, slot);
    }

    public int getSeed() {
        return dataTracker.get(SEED);
    }

    private void setSeed(int seed) {
        dataTracker.set(SEED, seed);
    }

    public ItemStack getWeaponStack() {
        ItemStack stack = dataTracker.get(WEAPON_STACK);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private void setWeaponStack(ItemStack stack) {
        dataTracker.set(WEAPON_STACK, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public long getExpiresAtTick() {
        return expiresAtTick;
    }

    public WraithmawTuningSnapshot getTuning() {
        return tuning;
    }

    public float getDamage() {
        return damage;
    }

    public void multiplyDamage(double multiplier) {
        damage *= (float) Math.max(0, multiplier);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return (getState() == STATE_EMBEDDED || getState() == STATE_ORBITING)
                && super.shouldSave();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        loadedFromNbt = true;
        ownerUuid = nbt.containsUuid("owner_uuid") ? nbt.getUuid("owner_uuid") : null;
        setOwnerId(nbt.getInt("owner_id"));
        setState(nbt.getInt("state"));
        dataTracker.set(STATE_START_AGE, nbt.getInt("state_start_age"));
        setOrbitSlot(nbt.getInt("orbit_slot"));
        setSeed(nbt.getInt("seed"));
        castOrigin = new Vec3d(nbt.getDouble("cast_x"), nbt.getDouble("cast_y"), nbt.getDouble("cast_z"));
        landing = new Vec3d(nbt.getDouble("landing_x"), nbt.getDouble("landing_y"), nbt.getDouble("landing_z"));
        sequence = nbt.getInt("sequence");
        damage = nbt.getFloat("damage");
        expiresAtTick = nbt.getLong("expires_at");
        traveled = nbt.getDouble("traveled");
        struckEntity = nbt.getBoolean("struck_entity");
        impactBurstUsed = nbt.getBoolean("impact_burst_used");
        launchMultiplierApplied = nbt.getBoolean("launch_multiplier_applied");
        gravewalkStruck = nbt.getBoolean("gravewalk_struck");
        tuning = WraithmawTuningSnapshot.read(nbt);
        if (nbt.contains("weapon")) {
            setWeaponStack(ItemStack.fromNbt(getRegistryManager(), nbt.getCompound("weapon")).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (ownerUuid != null) {
            nbt.putUuid("owner_uuid", ownerUuid);
        }
        nbt.putInt("owner_id", getOwnerId());
        nbt.putInt("state", getState());
        nbt.putInt("state_start_age", dataTracker.get(STATE_START_AGE));
        nbt.putInt("orbit_slot", getOrbitSlot());
        nbt.putInt("seed", getSeed());
        nbt.putDouble("cast_x", castOrigin.x);
        nbt.putDouble("cast_y", castOrigin.y);
        nbt.putDouble("cast_z", castOrigin.z);
        nbt.putDouble("landing_x", landing.x);
        nbt.putDouble("landing_y", landing.y);
        nbt.putDouble("landing_z", landing.z);
        nbt.putInt("sequence", sequence);
        nbt.putFloat("damage", damage);
        nbt.putLong("expires_at", expiresAtTick);
        nbt.putDouble("traveled", traveled);
        nbt.putBoolean("struck_entity", struckEntity);
        nbt.putBoolean("impact_burst_used", impactBurstUsed);
        nbt.putBoolean("launch_multiplier_applied", launchMultiplierApplied);
        nbt.putBoolean("gravewalk_struck", gravewalkStruck);
        tuning.write(nbt);
        ItemStack stack = getWeaponStack();
        if (!stack.isEmpty()) {
            nbt.put("weapon", stack.encode(getRegistryManager()));
        }
    }
}
