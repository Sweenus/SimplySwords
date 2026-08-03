package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class MagispearFallingSpearVisualEntity extends Entity {

    public static final int MODE_LAUNCH = 0;
    public static final int MODE_RAIN = 1;
    public static final int MODE_FINAL = 2;

    private static final TrackedData<Float> END_OFFSET_X = DataTracker.registerData(
            MagispearFallingSpearVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Y = DataTracker.registerData(
            MagispearFallingSpearVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Z = DataTracker.registerData(
            MagispearFallingSpearVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(
            MagispearFallingSpearVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MODE = DataTracker.registerData(
            MagispearFallingSpearVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(
            MagispearFallingSpearVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public MagispearFallingSpearVisualEntity(EntityType<? extends MagispearFallingSpearVisualEntity> type,
                                             World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public MagispearFallingSpearVisualEntity(World world, double x, double y, double z,
                                             double endOffsetX, double endOffsetY, double endOffsetZ,
                                             int lifetime, int mode, float scale) {
        this(EntityRegistry.MAGISPEAR_FALLING_SPEAR_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setEndOffset((float) endOffsetX, (float) endOffsetY, (float) endOffsetZ);
        this.setLifetime(lifetime);
        this.setMode(mode);
        this.setScale(scale);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(END_OFFSET_X, 0.0F);
        this.dataTracker.startTracking(END_OFFSET_Y, -10.0F);
        this.dataTracker.startTracking(END_OFFSET_Z, 0.0F);
        this.dataTracker.startTracking(LIFETIME, 6);
        this.dataTracker.startTracking(MODE, MODE_RAIN);
        this.dataTracker.startTracking(SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age > this.getLifetime()) {
            this.discard();
        }
    }

    public void setEndOffset(float x, float y, float z) {
        this.dataTracker.set(END_OFFSET_X, x);
        this.dataTracker.set(END_OFFSET_Y, y);
        this.dataTracker.set(END_OFFSET_Z, z);
    }

    public float getEndOffsetX() {
        return this.dataTracker.get(END_OFFSET_X);
    }

    public float getEndOffsetY() {
        return this.dataTracker.get(END_OFFSET_Y);
    }

    public float getEndOffsetZ() {
        return this.dataTracker.get(END_OFFSET_Z);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }

    public int getMode() {
        return this.dataTracker.get(MODE);
    }

    public void setMode(int mode) {
        this.dataTracker.set(MODE, mode);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setEndOffset(nbt.getFloat("end_offset_x"), nbt.getFloat("end_offset_y"), nbt.getFloat("end_offset_z"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setMode(nbt.getInt("mode"));
        this.setScale(nbt.getFloat("scale"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("end_offset_x", this.getEndOffsetX());
        nbt.putFloat("end_offset_y", this.getEndOffsetY());
        nbt.putFloat("end_offset_z", this.getEndOffsetZ());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("mode", this.getMode());
        nbt.putFloat("scale", this.getScale());
    }
}
