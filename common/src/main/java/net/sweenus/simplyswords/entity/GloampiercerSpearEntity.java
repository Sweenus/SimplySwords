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
import net.sweenus.simplyswords.api.ability.Phase2UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.GloamStainManager;
import net.sweenus.simplyswords.world.GloampiercerTuningSnapshot;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class GloampiercerSpearEntity extends Entity {
    public static final int STATE_FLYING = 0;
    public static final int STATE_EMBEDDED = 1;

    private static final TrackedData<Integer> STATE = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> WEAPON_STACK = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Float> DIRECTION_X = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DIRECTION_Y = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DIRECTION_Z = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(
            GloampiercerSpearEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.03F, 0.01F, 0.08F),
                    new Vector3f(0.12F, 0.92F, 0.94F), 1.2F);

    private UUID targetUuid;
    private float weaponDamage;
    private long detonateAtTick;
    private double traveled;
    private int missingOwnerTicks;
    private boolean chainedDetonation;
    private GloampiercerTuningSnapshot tuning = GloampiercerTuningSnapshot.from(null);
    private @Nullable UniqueAbilityExecution abilityExecution;

    public GloampiercerSpearEntity(EntityType<? extends GloampiercerSpearEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public GloampiercerSpearEntity(ServerWorld world, LivingEntity owner, ItemStack stack,
                                   Vec3d origin, Vec3d destination, LivingEntity target,
                                   float weaponDamage, double speed) {
        this(world, owner, stack, origin, destination, target, weaponDamage, speed, null);
    }

    public GloampiercerSpearEntity(ServerWorld world, LivingEntity owner, ItemStack stack,
                                   Vec3d origin, Vec3d destination, LivingEntity target,
                                   float weaponDamage, double speed,
                                   @Nullable UniqueAbilityExecution execution) {
        this(EntityRegistry.GLOAMPIERCER_SPEAR.get(), world);
        setOwnerUuid(owner.getUuid());
        setOwnerId(owner.getId());
        setWeaponStack(stack.copy());
        setPosition(origin);
        targetUuid = target == null ? null : target.getUuid();
        this.weaponDamage = Math.max(0.0F, weaponDamage);
        setSeed(owner.getRandom().nextInt());
        Vec3d direction = destination.subtract(origin);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = owner.getRotationVec(1.0F);
        }
        setFlightDirection(direction.normalize());
        setVelocity(direction.normalize().multiply(Math.max(0.1, speed)));
        abilityExecution = execution;
        tuning = GloampiercerTuningSnapshot.from(execution);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STATE, STATE_FLYING);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(OWNER_ID, -1);
        builder.add(WEAPON_STACK, ItemStack.EMPTY);
        builder.add(DIRECTION_X, 0.0F);
        builder.add(DIRECTION_Y, -1.0F);
        builder.add(DIRECTION_Z, 0.0F);
        builder.add(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) {
            return;
        }
        LivingEntity owner = resolveOwner(world);
        if (owner == null) {
            setOwnerId(-1);
            if (++missingOwnerTicks >= 40) {
                discard();
            }
            return;
        }
        missingOwnerTicks = 0;
        if (getState() == STATE_EMBEDDED) {
            tickEmbedded(world, owner);
        } else {
            tickFlying(world, owner);
        }
    }

    private void tickFlying(ServerWorld world, LivingEntity owner) {
        Vec3d current = getPos();
        Vec3d velocity = getVelocity();
        LivingEntity target = resolveTarget(world, owner);
        if (target != null) {
            Vec3d desired = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0).subtract(current);
            if (desired.lengthSquared() > 1.0E-6) {
                double turn = Math.toRadians(tuning.homingTurnDegrees());
                Vec3d direction = GloampiercerTuningSnapshot.turnToward(
                        velocity.normalize(), desired.normalize(), turn);
                velocity = direction.multiply(tuning.projectileSpeed());
                setVelocity(velocity);
                setFlightDirection(direction);
            }
        }
        if (velocity.lengthSquared() < 1.0E-6) {
            dissipate(world, current);
            return;
        }
        Vec3d next = current.add(velocity);
        BlockHitResult blockHit = world.raycast(new RaycastContext(current, next,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        Vec3d segmentEnd = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getPos();
        LivingEntity collision = findCollisionTarget(world, owner, current, segmentEnd);
        if (collision != null) {
            double grace = Math.max(0.0, Config.uniqueEffects.gloampiercer.collisionGrace);
            Vec3d impact = collision.getBoundingBox().expand(collision.getTargetingMargin() + grace)
                    .raycast(current, segmentEnd)
                    .orElse(collision.getPos().add(0.0, collision.getHeight() * 0.55, 0.0));
            setPosition(impact);
            ItemStack stack = getWeaponStack();
            if (!stack.isEmpty()) {
                SimplySwordsAPI.applyEntityWeaponHit(stack, collision, owner, weaponDamage);
                setWeaponStack(stack);
            }
            createStain(world, impact);
            explode(world, owner, collision.getUuid(), false);
            return;
        }
        setPosition(segmentEnd);
        traveled += current.distanceTo(segmentEnd);
        if (age % 2 == 0) {
            world.spawnParticles(GLOAM_DUST, current.x, current.y, current.z,
                    2, 0.05, 0.05, 0.05, 0.0);
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            embed(world, segmentEnd);
        } else if (tuning.flightExpired(age, traveled) || getY() < world.getBottomY() - 4) {
            dissipate(world, segmentEnd);
        }
    }

    private void tickEmbedded(ServerWorld world, LivingEntity owner) {
        if (world.getTime() >= detonateAtTick) {
            explode(world, owner, null, false);
            return;
        }
        double trigger = tuning.triggerRadius();
        Box box = getBoundingBox().expand(trigger, Math.max(1.0, trigger), trigger);
        boolean triggered = !world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)).isEmpty();
        if (triggered) {
            explode(world, owner, null, true);
        } else if (age % 10 == 0) {
            world.spawnParticles(GLOAM_DUST, getX(), getY() + 0.12, getZ(),
                    1, 0.08, 0.04, 0.08, 0.0);
        }
    }

    private LivingEntity findCollisionTarget(ServerWorld world, LivingEntity owner, Vec3d start, Vec3d end) {
        double grace = Math.max(0.0, Config.uniqueEffects.gloampiercer.collisionGrace);
        Box search = new Box(start, end).expand(grace + 0.55);
        return world.getEntitiesByClass(LivingEntity.class, search,
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && entity.getBoundingBox().expand(entity.getTargetingMargin() + grace)
                                .raycast(start, end).isPresent())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(start)))
                .orElse(null);
    }

    private LivingEntity resolveTarget(ServerWorld world, LivingEntity owner) {
        Entity entity = targetUuid == null ? null : world.getEntity(targetUuid);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                && HelperMethods.checkAbilityTarget(living, owner) ? living : null;
    }

    private void embed(ServerWorld world, Vec3d impact) {
        setPosition(impact);
        setVelocity(Vec3d.ZERO);
        setState(STATE_EMBEDDED);
        detonateAtTick = world.getTime() + tuning.embeddedDurationTicks();
        createStain(world, impact);
        world.spawnParticles(GLOAM_DUST, impact.x, impact.y + 0.12, impact.z,
                12, 0.3, 0.12, 0.3, 0.035);
        world.playSound(null, impact.x, impact.y, impact.z, SoundRegistry.OBJECT_IMPACT_THUD.get(),
                SoundCategory.PLAYERS, 0.58F, 1.25F + world.random.nextFloat() * 0.14F);
    }

    private void explode(ServerWorld world, LivingEntity owner, UUID directTarget, boolean proximityTriggered) {
        Vec3d center = getPos();
        double radius = tuning.explosionRadius();
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        ItemStack stack = getWeaponStack();
        java.util.List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && !entity.getUuid().equals(directTarget)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && entity.squaredDistanceTo(center) <= radius * radius).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(center)))
                .limit(tuning.explosionTargetCap()).toList();
        if (tuning.hasMode(128) && tuning.pullTargetCap() > 0 && tuning.pullStrength() > 0) {
            targets.stream()
                    .filter(target -> GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), target))
                    .limit(tuning.pullTargetCap())
                    .forEach(target -> moveToward(world, target, center, tuning.pullStrength()));
        }
        for (LivingEntity target : targets) {
            if (!stack.isEmpty()) {
                boolean onOwnerGloam = GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), target);
                float multiplier = (float) tuning.explosionDamageMultiplier();
                if (onOwnerGloam) multiplier *= (float) (1 + tuning.vulnerabilityBonus());
                SimplySwordsAPI.applyEntityWeaponHit(stack, target, owner, weaponDamage * multiplier);
            }
        }
        setWeaponStack(stack);
        if (abilityExecution != null) {
            UniqueAbilityApi.emit(abilityExecution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    Phase2UniqueAbilities.HIT, null, targets.size(), weaponDamage);
            if (abilityExecution.definition() == Phase2UniqueAbilities.GLOAMPIERCER_AMBUSH) {
                UniqueAbilityApi.finish(abilityExecution, abilityExecution.definition().id(), targets.size());
            }
        }
        if (proximityTriggered) scheduleChainedDetonation(world);
        createStain(world, center);
        world.spawnParticles(GLOAM_DUST, center.x, center.y + 0.18, center.z,
                30, radius * 0.38, radius * 0.24, radius * 0.38, 0.08);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.22, center.z,
                9, radius * 0.26, radius * 0.2, radius * 0.26, 0.035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.12, center.z,
                18, radius * 0.32, radius * 0.12, radius * 0.32, 0.1);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                SoundCategory.PLAYERS, 0.72F, 0.7F + world.random.nextFloat() * 0.12F);
        world.playSound(null, center.x, center.y, center.z, SoundRegistry.DARK_SWORD_ATTACK_03.get(),
                SoundCategory.PLAYERS, 0.54F, 0.78F + world.random.nextFloat() * 0.1F);
        discard();
    }

    private void createStain(ServerWorld world, Vec3d center) {
        GloamStainManager.createPatch(world, getOwnerUuid(), center,
                tuning.stainRadius(), tuning.stainDurationTicks(),
                Math.max(1, Config.uniqueEffects.gloampiercer.stainFadeDuration),
                tuning.stainAmplifier(), new GloamStainManager.PatchBehavior(
                        getWeaponStack(), weaponDamage, tuning.slowDurationTicks(), !tuning.hasMode(512),
                        tuning.hasMode(256) ? tuning.moveRange() : 0,
                        tuning.hasMode(256) ? tuning.moveSpeed() : 0,
                        tuning.hasMode(512) ? tuning.expiryDamageMultiplier() : 0,
                        tuning.expiryRadius(), tuning.expiryTargetCap()));
    }

    private void dissipate(ServerWorld world, Vec3d position) {
        world.spawnParticles(GLOAM_DUST, position.x, position.y, position.z,
                8, 0.18, 0.18, 0.18, 0.025);
        discard();
    }

    private void scheduleChainedDetonation(ServerWorld world) {
        if (!tuning.hasMode(64) || tuning.chainRange() <= 0 || tuning.chainDelayTicks() <= 0) return;
        UUID ownerUuid = getOwnerUuid();
        GloampiercerSpearEntity nearest = world.getEntitiesByClass(GloampiercerSpearEntity.class,
                        getBoundingBox().expand(tuning.chainRange()),
                        spear -> spear != this && spear.getState() == STATE_EMBEDDED
                                && ownerUuid != null && ownerUuid.equals(spear.getOwnerUuid())
                                && !spear.chainedDetonation)
                .stream().min(Comparator.comparingDouble(spear -> spear.squaredDistanceTo(this))).orElse(null);
        if (nearest != null) {
            nearest.chainedDetonation = true;
            nearest.detonateAtTick = Math.min(nearest.detonateAtTick,
                    world.getTime() + tuning.chainDelayTicks());
        }
    }

    private static void moveToward(ServerWorld world, LivingEntity target, Vec3d center, double distance) {
        Vec3d offset = new Vec3d(center.x - target.getX(), 0, center.z - target.getZ());
        if (offset.lengthSquared() < 1.0E-6) return;
        offset = offset.normalize().multiply(Math.min(distance, offset.horizontalLength()));
        if (world.isSpaceEmpty(target, target.getBoundingBox().offset(offset))) {
            target.setPosition(target.getPos().add(offset));
        }
    }

    private LivingEntity resolveOwner(ServerWorld world) {
        UUID ownerUuid = getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        Entity entity = getOwnerId() >= 0 ? world.getEntityById(getOwnerId()) : null;
        if (entity == null || !ownerUuid.equals(entity.getUuid())) {
            entity = world.getEntity(ownerUuid);
        }
        if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
            setOwnerId(living.getId());
            return living;
        }
        return null;
    }

    public int getState() {
        return dataTracker.get(STATE);
    }

    public void setState(int value) {
        dataTracker.set(STATE, value);
    }

    public UUID getOwnerUuid() {
        return dataTracker.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUuid(UUID value) {
        dataTracker.set(OWNER_UUID, Optional.ofNullable(value));
    }

    public int getOwnerId() {
        return dataTracker.get(OWNER_ID);
    }

    public void setOwnerId(int value) {
        dataTracker.set(OWNER_ID, value);
    }

    public ItemStack getWeaponStack() {
        return dataTracker.get(WEAPON_STACK);
    }

    public void setWeaponStack(ItemStack value) {
        dataTracker.set(WEAPON_STACK, value == null ? ItemStack.EMPTY : value.copy());
    }

    public Vec3d getFlightDirection() {
        return new Vec3d(dataTracker.get(DIRECTION_X), dataTracker.get(DIRECTION_Y), dataTracker.get(DIRECTION_Z));
    }

    public void setFlightDirection(Vec3d value) {
        Vec3d direction = value.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, -1.0, 0.0) : value.normalize();
        dataTracker.set(DIRECTION_X, (float) direction.x);
        dataTracker.set(DIRECTION_Y, (float) direction.y);
        dataTracker.set(DIRECTION_Z, (float) direction.z);
    }

    public int getSeed() {
        return dataTracker.get(SEED);
    }

    public void setSeed(int value) {
        dataTracker.set(SEED, value);
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
        return getState() == STATE_EMBEDDED && super.shouldSave();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("owner_uuid")) {
            setOwnerUuid(nbt.getUuid("owner_uuid"));
        }
        if (nbt.containsUuid("target_uuid")) {
            targetUuid = nbt.getUuid("target_uuid");
        }
        if (nbt.contains("weapon_stack")) {
            setWeaponStack(ItemStack.fromNbt(getRegistryManager(), nbt.getCompound("weapon_stack"))
                    .orElse(ItemsRegistry.GLOAMPIERCER.get().getDefaultStack()));
        }
        setState(nbt.getInt("state"));
        setFlightDirection(new Vec3d(nbt.getDouble("direction_x"), nbt.getDouble("direction_y"),
                nbt.getDouble("direction_z")));
        setSeed(nbt.getInt("seed"));
        weaponDamage = nbt.getFloat("weapon_damage");
        detonateAtTick = nbt.getLong("detonate_at");
        traveled = nbt.getDouble("traveled");
        chainedDetonation = nbt.getBoolean("chained_detonation");
        tuning = GloampiercerTuningSnapshot.read(nbt);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        UUID ownerUuid = getOwnerUuid();
        if (ownerUuid != null) {
            nbt.putUuid("owner_uuid", ownerUuid);
        }
        if (targetUuid != null) {
            nbt.putUuid("target_uuid", targetUuid);
        }
        ItemStack stack = getWeaponStack();
        if (!stack.isEmpty()) {
            nbt.put("weapon_stack", stack.encode(getRegistryManager()));
        }
        Vec3d direction = getFlightDirection();
        nbt.putInt("state", getState());
        nbt.putDouble("direction_x", direction.x);
        nbt.putDouble("direction_y", direction.y);
        nbt.putDouble("direction_z", direction.z);
        nbt.putInt("seed", getSeed());
        nbt.putFloat("weapon_damage", weaponDamage);
        nbt.putLong("detonate_at", detonateAtTick);
        nbt.putDouble("traveled", traveled);
        nbt.putBoolean("chained_detonation", chainedDetonation);
        tuning.write(nbt);
    }
}
