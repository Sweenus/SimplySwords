package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class DevourerReprisalVisualEntity extends Entity {
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(DevourerReprisalVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(DevourerReprisalVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(DevourerReprisalVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> FED = DataTracker.registerData(DevourerReprisalVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public DevourerReprisalVisualEntity(EntityType<? extends DevourerReprisalVisualEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public DevourerReprisalVisualEntity(World world, Vec3d center,
                                        int lifetime, float radius, boolean fed) {
        this(EntityRegistry.DEVOURER_REPRISAL_VISUAL.get(), world);
        setPosition(center);
        dataTracker.set(LIFETIME, Math.max(1, lifetime));
        dataTracker.set(SEED, world.random.nextInt());
        dataTracker.set(RADIUS, Math.max(0.5F, radius));
        dataTracker.set(FED, fed);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(LIFETIME, 14);
        this.dataTracker.startTracking(SEED, 0);
        this.dataTracker.startTracking(RADIUS, 3.5F);
        this.dataTracker.startTracking(FED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient() && age >= getLifetime()) {
            discard();
        }
    }

    public int getLifetime() {
        return dataTracker.get(LIFETIME);
    }

    public int getSeed() {
        return dataTracker.get(SEED);
    }

    public float getRadius() {
        return dataTracker.get(RADIUS);
    }

    public boolean isFed() {
        return dataTracker.get(FED);
    }

    @Override
    public Box getVisibilityBoundingBox() {
        return getBoundingBox().expand(getRadius() + 2.0F, 3.0, getRadius() + 2.0F);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        dataTracker.set(SEED, nbt.getInt("seed"));
        dataTracker.set(RADIUS, nbt.getFloat("radius"));
        dataTracker.set(FED, nbt.getBoolean("fed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("lifetime", getLifetime());
        nbt.putInt("seed", getSeed());
        nbt.putFloat("radius", getRadius());
        nbt.putBoolean("fed", isFed());
    }
}
