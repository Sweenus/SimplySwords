package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.BaneheadSwarmManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SimplySwordsCreeperHeadEntity extends PathAwareEntity {

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(SimplySwordsCreeperHeadEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> ORBIT_BASE_ANGLE = DataTracker.registerData(SimplySwordsCreeperHeadEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_RADIUS_TRACKED = DataTracker.registerData(SimplySwordsCreeperHeadEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_HEIGHT_TRACKED = DataTracker.registerData(SimplySwordsCreeperHeadEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_ANGULAR_SPEED_TRACKED = DataTracker.registerData(SimplySwordsCreeperHeadEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> HOMING = DataTracker.registerData(SimplySwordsCreeperHeadEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private UUID ownerUuid;
    private ItemStack sourceStack = ItemStack.EMPTY;

    private double orbitRadius;
    private double orbitHeight;
    private double orbitAngularSpeed;
    private double homingRange;
    private double homingSpeed;
    private double homingInitialSpeedFraction;
    private int homingAccelerationTicks;
    private double homingTurnRateDegrees;
    private double explosionProximity;
    private double explosionRadius;
    private float damage;
    private int maxHomingTicks;

    private UUID targetUuid;
    private int homingTicks;
    private Vec3d headingDirection = Vec3d.ZERO;

    private boolean initialized;

    public SimplySwordsCreeperHeadEntity(EntityType<? extends SimplySwordsCreeperHeadEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static DefaultAttributeContainer.Builder createHeadAttributes() {
        return MobEntity.createMobAttributes();
    }

    public void initializeHead(LivingEntity owner, ItemStack stack, double orbitBaseAngle, double orbitRadius, double orbitHeight,
                                double orbitAngularSpeed, double homingRange, double homingSpeed,
                                double homingInitialSpeedFraction, int homingAccelerationTicks, double homingTurnRateDegrees,
                                double explosionProximity, double explosionRadius,
                                float damage, int maxHomingTicks) {
        this.ownerUuid = owner.getUuid();
        this.sourceStack = stack.copy();
        this.orbitRadius = orbitRadius;
        this.orbitHeight = orbitHeight;
        this.orbitAngularSpeed = orbitAngularSpeed;
        this.homingRange = homingRange;
        this.homingSpeed = homingSpeed;
        this.homingInitialSpeedFraction = MathHelper.clamp(homingInitialSpeedFraction, 0.0, 1.0);
        this.homingAccelerationTicks = Math.max(1, homingAccelerationTicks);
        this.homingTurnRateDegrees = Math.max(0.0, homingTurnRateDegrees);
        this.explosionProximity = explosionProximity;
        this.explosionRadius = explosionRadius;
        this.damage = Math.max(0.0F, damage);
        this.maxHomingTicks = Math.max(1, maxHomingTicks);
        this.setAiDisabled(true);
        this.setPersistent();
        this.initialized = true;

        this.dataTracker.set(OWNER_ENTITY_ID, owner.getId());
        this.dataTracker.set(ORBIT_BASE_ANGLE, (float) orbitBaseAngle);
        this.dataTracker.set(ORBIT_RADIUS_TRACKED, (float) orbitRadius);
        this.dataTracker.set(ORBIT_HEIGHT_TRACKED, (float) orbitHeight);
        this.dataTracker.set(ORBIT_ANGULAR_SPEED_TRACKED, (float) orbitAngularSpeed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(ORBIT_BASE_ANGLE, 0.0F);
        builder.add(ORBIT_RADIUS_TRACKED, 1.6F);
        builder.add(ORBIT_HEIGHT_TRACKED, 1.4F);
        builder.add(ORBIT_ANGULAR_SPEED_TRACKED, 0.05F);
        builder.add(HOMING, false);
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public float getOrbitBaseAngle() {
        return this.dataTracker.get(ORBIT_BASE_ANGLE);
    }

    public float getOrbitRadius() {
        return this.dataTracker.get(ORBIT_RADIUS_TRACKED);
    }

    public float getOrbitHeight() {
        return this.dataTracker.get(ORBIT_HEIGHT_TRACKED);
    }

    public float getOrbitAngularSpeed() {
        return this.dataTracker.get(ORBIT_ANGULAR_SPEED_TRACKED);
    }

    public boolean isHoming() {
        return this.dataTracker.get(HOMING);
    }

    private void setHoming(boolean homing) {
        this.dataTracker.set(HOMING, homing);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Nullable
    private LivingEntity getOwner(ServerWorld world) {
        if (this.ownerUuid == null) return null;
        Entity entity = world.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private Vec3d ringPosition(Vec3d ownerPos) {
        double angle = getOrbitBaseAngle() + this.age * this.orbitAngularSpeed;
        return ringPositionAt(ownerPos, angle, this.orbitRadius, this.orbitHeight);
    }

    public static Vec3d ringPositionAt(Vec3d ownerPos, double angle, double orbitRadius, double orbitHeight) {
        double bob = Math.sin(angle * 2.0) * 0.08;
        return ownerPos.add(Math.cos(angle) * orbitRadius, orbitHeight + bob, Math.sin(angle) * orbitRadius);
    }

    private Vec3d ringTangent() {
        double angle = getOrbitBaseAngle() + this.age * this.orbitAngularSpeed;
        return new Vec3d(-Math.sin(angle), 0.0, Math.cos(angle));
    }

    private static Vec3d turnToward(Vec3d current, Vec3d target, double maxRadians) {
        double dot = MathHelper.clamp(current.dotProduct(target), -1.0, 1.0);
        double angleBetween = Math.acos(dot);
        if (angleBetween <= maxRadians || angleBetween < 1.0E-4) {
            return target;
        }

        double sinTotal = Math.sin(angleBetween);
        if (sinTotal < 1.0E-4) {
            // current/target are (near) exactly opposite — slerp is undefined right at 180
            // degrees (would divide by ~0 and produce NaNs, which read as glitchy jitter).
            // Nudge sideways using an arbitrary perpendicular axis instead of stalling.
            Vec3d perpendicular = Math.abs(current.y) < 0.99 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
            Vec3d sidestep = current.crossProduct(perpendicular).normalize();
            return current.multiply(Math.cos(maxRadians)).add(sidestep.multiply(Math.sin(maxRadians))).normalize();
        }

        double t = maxRadians / angleBetween;
        double a = Math.sin((1.0 - t) * angleBetween) / sinTotal;
        double b = Math.sin(t * angleBetween) / sinTotal;
        return new Vec3d(
                current.x * a + target.x * b,
                current.y * a + target.y * b,
                current.z * a + target.z * b
        ).normalize();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }

        if (!this.initialized) {
            this.discard();
            return;
        }

        LivingEntity owner = getOwner(world);
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        if (isHoming()) {
            tickHoming(world, owner);
        } else {
            tickOrbiting(world, owner);
        }
    }

    private void tickOrbiting(ServerWorld world, LivingEntity owner) {
        this.setPosition(owner.getX(), owner.getY(), owner.getZ());

        LivingEntity target = findTarget(world, owner);
        if (target != null && BaneheadSwarmManager.tryConsumeHomingCooldown(world, this.ownerUuid)) {
            Vec3d ringPos = ringPosition(owner.getPos());
            this.setPosition(ringPos.x, ringPos.y, ringPos.z);

            this.targetUuid = target.getUuid();
            this.homingTicks = 0;
            this.headingDirection = ringTangent();
            faceHeading(this.headingDirection);
            setHoming(true);
            world.playSound(null, ringPos.x, ringPos.y, ringPos.z, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.PLAYERS, 0.6F, 1.0F);
        }
    }

    private void faceHeading(Vec3d direction) {
        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        this.setYaw(yaw);
        this.setPitch((float) Math.toDegrees(-Math.asin(MathHelper.clamp(direction.y, -1.0, 1.0))));
    }

    private void tickHoming(ServerWorld world, LivingEntity owner) {
        LivingEntity target = this.targetUuid != null && world.getEntity(this.targetUuid) instanceof LivingEntity living ? living : null;
        if (target == null || !target.isAlive() || target.squaredDistanceTo(owner) > (2.0 * this.homingRange) * (2.0 * this.homingRange)) {
            this.targetUuid = null;
            setHoming(false);
            return;
        }

        Vec3d toTarget = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(this.getPos());
        double distance = toTarget.length();

        if (distance <= this.explosionProximity) {
            explode(world, owner);
            return;
        }

        Vec3d desiredDirection = toTarget.multiply(1.0 / Math.max(distance, 1.0E-4));
        this.headingDirection = turnToward(this.headingDirection, desiredDirection, Math.toRadians(this.homingTurnRateDegrees));

        double rampProgress = MathHelper.clamp((double) this.homingTicks / (double) this.homingAccelerationTicks, 0.0, 1.0);
        double effectiveSpeed = MathHelper.lerp(rampProgress, this.homingSpeed * this.homingInitialSpeedFraction, this.homingSpeed);
        Vec3d velocity = this.headingDirection.multiply(effectiveSpeed);
        this.move(MovementType.SELF, velocity);
        faceHeading(this.headingDirection);

        this.homingTicks++;
        if (this.homingTicks >= this.maxHomingTicks) {
            explode(world, owner);
        }
    }

    @Nullable
    private LivingEntity findTarget(ServerWorld world, LivingEntity owner) {
        Box box = owner.getBoundingBox().expand(this.homingRange);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity candidate)) {
                continue;
            }
            if (candidate instanceof SimplySwordsCreeperHeadEntity) {
                continue;
            }
            if (!HelperMethods.checkAbilityTarget(candidate, owner)) {
                continue;
            }
            double distance = candidate.squaredDistanceTo(owner);
            if (distance <= this.homingRange * this.homingRange && distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void explode(ServerWorld world, LivingEntity owner) {
        Vec3d pos = this.getPos();
        Box box = new Box(
                pos.x - this.explosionRadius, pos.y - this.explosionRadius, pos.z - this.explosionRadius,
                pos.x + this.explosionRadius, pos.y + this.explosionRadius, pos.z + this.explosionRadius
        );
        for (Entity entity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity target && !(target instanceof SimplySwordsCreeperHeadEntity)
                    && HelperMethods.checkAbilityTarget(target, owner)
                    && target.squaredDistanceTo(pos) <= this.explosionRadius * this.explosionRadius) {
                DamageSource damageSource = world.getDamageSources().indirectMagic(owner, owner);
                float finalDamage = HelperMethods.applyAbilityDamageEnchantments(world, this.sourceStack, target, damageSource, this.damage);
                HelperMethods.damageThroughIframes(target, damageSource, finalDamage);
            }
        }

        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 12, 0.3, 0.3, 0.3, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6F, 1.1F + world.random.nextFloat() * 0.2F);

        this.discard();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.FAIL;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("owner_uuid")) {
            this.ownerUuid = nbt.getUuid("owner_uuid");
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
    }
}
