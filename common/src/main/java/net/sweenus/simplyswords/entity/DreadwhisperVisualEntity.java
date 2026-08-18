package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class DreadwhisperVisualEntity extends Entity {
    public static final int REAVE_FRONT = 0;
    public static final int WOUND_BURST = 1;
    public static final int LEECH_HIT = 2;

    private static final TrackedData<Integer> KIND = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> DIRECTION_X = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DIRECTION_Z = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLLAPSE_AGE = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(DreadwhisperVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public DreadwhisperVisualEntity(EntityType<? extends DreadwhisperVisualEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public static DreadwhisperVisualEntity reaveFront(World world, LivingEntity owner, Vec3d direction,
                                                      float width, float height, int lifetime) {
        DreadwhisperVisualEntity visual = create(world, owner.getPos(), REAVE_FRONT, lifetime);
        visual.setOwnerId(owner.getId());
        visual.setDirection(direction);
        visual.setVisualWidth(width);
        visual.setVisualHeight(height);
        return visual;
    }

    public static DreadwhisperVisualEntity leechHit(World world, Vec3d position,
                                                    LivingEntity owner, float width, float height, int lifetime) {
        DreadwhisperVisualEntity visual = create(world, position, LEECH_HIT, lifetime);
        visual.setOwnerId(owner.getId());
        visual.setVisualWidth(width);
        visual.setVisualHeight(height);
        return visual;
    }

    public static DreadwhisperVisualEntity woundBurst(World world, Vec3d position,
                                                       float width, float height, int lifetime) {
        DreadwhisperVisualEntity visual = create(world, position, WOUND_BURST, lifetime);
        visual.setVisualWidth(width);
        visual.setVisualHeight(height);
        return visual;
    }

    private static DreadwhisperVisualEntity create(World world, Vec3d position, int kind, int lifetime) {
        DreadwhisperVisualEntity visual = new DreadwhisperVisualEntity(EntityRegistry.DREADWHISPER_VISUAL.get(), world);
        visual.setPosition(position);
        visual.setKind(kind);
        visual.setLifetime(lifetime);
        visual.setSeed(world.random.nextInt());
        return visual;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(KIND, REAVE_FRONT);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(DIRECTION_X, 0.0F);
        this.dataTracker.startTracking(DIRECTION_Z, 1.0F);
        this.dataTracker.startTracking(WIDTH, 4.5F);
        this.dataTracker.startTracking(HEIGHT, 2.8F);
        this.dataTracker.startTracking(LIFETIME, 20);
        this.dataTracker.startTracking(COLLAPSE_AGE, -1);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient()) {
            if (age >= getLifetime()) {
                discard();
                return;
            }
            if (getKind() == REAVE_FRONT && getCollapseAge() < 0) {
                LivingEntity owner = getOwner();
                if (owner == null) {
                    discard();
                } else {
                    setPosition(owner.getPos());
                }
            }
        }
    }

    public LivingEntity getOwner() {
        Entity owner = getOwnerId() < 0 ? null : getWorld().getEntityById(getOwnerId());
        return owner instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    public Vec3d getDirection() {
        Vec3d direction = new Vec3d(dataTracker.get(DIRECTION_X), 0.0, dataTracker.get(DIRECTION_Z));
        return direction.horizontalLengthSquared() < 0.0001 ? new Vec3d(0.0, 0.0, 1.0) : direction.normalize();
    }

    public void setDirection(Vec3d direction) {
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.horizontalLengthSquared() < 0.0001) {
            horizontal = new Vec3d(0.0, 0.0, 1.0);
        } else {
            horizontal = horizontal.normalize();
        }
        dataTracker.set(DIRECTION_X, (float) horizontal.x);
        dataTracker.set(DIRECTION_Z, (float) horizontal.z);
    }

    public int getKind() {
        return dataTracker.get(KIND);
    }

    public void setKind(int kind) {
        dataTracker.set(KIND, kind);
    }

    public int getOwnerId() {
        return dataTracker.get(OWNER_ID);
    }

    public void setOwnerId(int ownerId) {
        dataTracker.set(OWNER_ID, ownerId);
    }

    public float getVisualWidth() {
        return dataTracker.get(WIDTH);
    }

    public void setVisualWidth(float width) {
        dataTracker.set(WIDTH, Math.max(0.1F, width));
    }

    public float getVisualHeight() {
        return dataTracker.get(HEIGHT);
    }

    public void setVisualHeight(float height) {
        dataTracker.set(HEIGHT, Math.max(0.1F, height));
    }

    public int getLifetime() {
        return dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getCollapseAge() {
        return dataTracker.get(COLLAPSE_AGE);
    }

    public void beginCollapse() {
        dataTracker.set(COLLAPSE_AGE, age);
    }

    public int getSeed() {
        return dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        dataTracker.set(SEED, seed);
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public Box getVisibilityBoundingBox() {
        LivingEntity owner = getOwner();
        if (getKind() == LEECH_HIT && owner != null) {
            return new Box(getPos(), owner.getPos().add(0.0, owner.getHeight() * 0.55, 0.0)).expand(1.5);
        }
        Vec3d center = getKind() == REAVE_FRONT && getCollapseAge() < 0 && owner != null
                ? owner.getPos() : getPos();
        double padding = Math.max(4.0, getVisualWidth() + 4.5);
        return new Box(center, center.add(0.0, getVisualHeight(), 0.0)).expand(padding);
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
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
