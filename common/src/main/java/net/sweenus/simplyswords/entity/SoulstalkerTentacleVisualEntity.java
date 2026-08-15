package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class SoulstalkerTentacleVisualEntity extends Entity {
    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(SoulstalkerTentacleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(SoulstalkerTentacleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(SoulstalkerTentacleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> IMPACT_AGE =
            DataTracker.registerData(SoulstalkerTentacleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(SoulstalkerTentacleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public SoulstalkerTentacleVisualEntity(EntityType<? extends SoulstalkerTentacleVisualEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public SoulstalkerTentacleVisualEntity(World world, LivingEntity owner, LivingEntity target,
                                           int impactAge, int lifetime) {
        this(EntityRegistry.SOULSTALKER_TENTACLE_VISUAL.get(), world);
        dataTracker.set(OWNER_ID, owner.getId());
        dataTracker.set(TARGET_ID, target.getId());
        dataTracker.set(IMPACT_AGE, Math.max(1, impactAge));
        dataTracker.set(LIFETIME, Math.max(impactAge + 1, lifetime));
        dataTracker.set(SEED, world.random.nextInt());
        setPosition(owner.getPos());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(LIFETIME, 18);
        this.dataTracker.startTracking(IMPACT_AGE, 8);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (!getWorld().isClient() && (age >= getLifetime() || owner == null || target == null)) {
            discard();
            return;
        }
        if (owner != null) {
            setPosition(owner.getPos());
        }
    }

    public LivingEntity getOwner() {
        Entity entity = getOwnerId() < 0 ? null : getWorld().getEntityById(getOwnerId());
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    public LivingEntity getTarget() {
        Entity entity = getTargetId() < 0 ? null : getWorld().getEntityById(getTargetId());
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    public Vec3d getRoot(float tickDelta) {
        LivingEntity owner = getOwner();
        if (owner == null) {
            return getLerpedPos(tickDelta);
        }
        Vec3d backward = getBackDirection(tickDelta);
        return owner.getLerpedPos(tickDelta)
                .add(0.0, owner.getHeight() * 0.62, 0.0)
                .add(backward.multiply(0.32));
    }

    public Vec3d getBackDirection(float tickDelta) {
        LivingEntity owner = getOwner();
        if (owner == null) {
            return new Vec3d(0.0, 0.0, -1.0);
        }
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, owner.prevBodyYaw, owner.bodyYaw)
                * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(MathHelper.sin(bodyYaw), 0.0, -MathHelper.cos(bodyYaw));
    }

    public Vec3d getEndpoint(float tickDelta) {
        LivingEntity target = getTarget();
        return target == null ? getLerpedPos(tickDelta)
                : target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.56, 0.0);
    }

    public int getOwnerId() {
        return dataTracker.get(OWNER_ID);
    }

    public int getTargetId() {
        return dataTracker.get(TARGET_ID);
    }

    public int getLifetime() {
        return dataTracker.get(LIFETIME);
    }

    public int getImpactAge() {
        return dataTracker.get(IMPACT_AGE);
    }

    public int getSeed() {
        return dataTracker.get(SEED);
    }

    @Override
    public Box getVisibilityBoundingBox() {
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || target == null) {
            return getBoundingBox().expand(8.0);
        }
        return new Box(owner.getPos(), target.getPos()).expand(2.0);
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
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
