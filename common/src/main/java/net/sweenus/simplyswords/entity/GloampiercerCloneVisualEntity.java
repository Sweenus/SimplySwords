package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class GloampiercerCloneVisualEntity extends Entity {
    private static final TrackedData<Float> VISUAL_YAW = DataTracker.registerData(
            GloampiercerCloneVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(
            GloampiercerCloneVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> THROW_TICK = DataTracker.registerData(
            GloampiercerCloneVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> THROW_INTERVAL = DataTracker.registerData(
            GloampiercerCloneVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> THROW_COUNT = DataTracker.registerData(
            GloampiercerCloneVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(
            GloampiercerCloneVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public GloampiercerCloneVisualEntity(EntityType<? extends GloampiercerCloneVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public GloampiercerCloneVisualEntity(World world, double x, double y, double z,
                                         float visualYaw, int lifetime, int throwTick,
                                         int throwInterval, int throwCount, int seed) {
        this(EntityRegistry.GLOAMPIERCER_CLONE_VISUAL.get(), world);
        setPosition(x, y, z);
        setVisualYaw(visualYaw);
        setYaw(visualYaw);
        setLifetime(lifetime);
        setThrowTick(throwTick);
        setThrowInterval(throwInterval);
        setThrowCount(throwCount);
        setSeed(seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(VISUAL_YAW, 0.0F);
        builder.add(LIFETIME, 20);
        builder.add(THROW_TICK, 10);
        builder.add(THROW_INTERVAL, 0);
        builder.add(THROW_COUNT, 1);
        builder.add(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient() && age > getLifetime()) {
            discard();
        }
    }

    public float getVisualYaw() {
        return dataTracker.get(VISUAL_YAW);
    }

    public void setVisualYaw(float value) {
        dataTracker.set(VISUAL_YAW, value);
    }

    public int getLifetime() {
        return dataTracker.get(LIFETIME);
    }

    public void setLifetime(int value) {
        dataTracker.set(LIFETIME, Math.max(1, value));
    }

    public int getThrowTick() {
        return dataTracker.get(THROW_TICK);
    }

    public void setThrowTick(int value) {
        dataTracker.set(THROW_TICK, Math.max(1, value));
    }

    public int getThrowInterval() {
        return dataTracker.get(THROW_INTERVAL);
    }

    public void setThrowInterval(int value) {
        dataTracker.set(THROW_INTERVAL, Math.max(0, value));
    }

    public int getThrowCount() {
        return dataTracker.get(THROW_COUNT);
    }

    public void setThrowCount(int value) {
        dataTracker.set(THROW_COUNT, Math.max(0, value));
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
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setVisualYaw(nbt.getFloat("visual_yaw"));
        setLifetime(nbt.getInt("lifetime"));
        setThrowTick(nbt.getInt("throw_tick"));
        setThrowInterval(nbt.getInt("throw_interval"));
        setThrowCount(nbt.contains("throw_count") ? nbt.getInt("throw_count") : 1);
        setSeed(nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("visual_yaw", getVisualYaw());
        nbt.putInt("lifetime", getLifetime());
        nbt.putInt("throw_tick", getThrowTick());
        nbt.putInt("throw_interval", getThrowInterval());
        nbt.putInt("throw_count", getThrowCount());
        nbt.putInt("seed", getSeed());
    }
}
