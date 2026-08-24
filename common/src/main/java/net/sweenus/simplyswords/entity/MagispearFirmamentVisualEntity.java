package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class MagispearFirmamentVisualEntity extends Entity {

    private static final TrackedData<Float> RADIUS = DataTracker.registerData(
            MagispearFirmamentVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> WAVE_COUNT = DataTracker.registerData(
            MagispearFirmamentVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(
            MagispearFirmamentVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public MagispearFirmamentVisualEntity(EntityType<? extends MagispearFirmamentVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public MagispearFirmamentVisualEntity(World world, double x, double y, double z,
                                          float radius, int waveCount, int lifetime) {
        this(EntityRegistry.MAGISPEAR_FIRMAMENT_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setRadius(radius);
        this.setWaveCount(waveCount);
        this.setLifetime(lifetime);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(RADIUS, 4.0F);
        builder.add(WAVE_COUNT, 4);
        builder.add(LIFETIME, 46);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age > this.getLifetime()) {
            this.discard();
        }
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, radius);
    }

    public int getWaveCount() {
        return this.dataTracker.get(WAVE_COUNT);
    }

    public void setWaveCount(int waveCount) {
        this.dataTracker.set(WAVE_COUNT, waveCount);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setRadius(nbt.getFloat("radius"));
        this.setWaveCount(nbt.getInt("wave_count"));
        this.setLifetime(nbt.getInt("lifetime"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("radius", this.getRadius());
        nbt.putInt("wave_count", this.getWaveCount());
        nbt.putInt("lifetime", this.getLifetime());
    }
}
