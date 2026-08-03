package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class ChainLightningVisualEntity extends Entity {

    private static final TrackedData<Float> END_OFFSET_X = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Y = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Z = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> THICKNESS = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> BRANCHES = DataTracker.registerData(ChainLightningVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public ChainLightningVisualEntity(EntityType<? extends ChainLightningVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public ChainLightningVisualEntity(World world, double x, double y, double z, double endOffsetX, double endOffsetY, double endOffsetZ,
                                      int lifetime, int seed, int color, float thickness, int branches) {
        this(EntityRegistry.CHAIN_LIGHTNING_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setEndOffset((float) endOffsetX, (float) endOffsetY, (float) endOffsetZ);
        this.setLifetime(lifetime);
        this.setSeed(seed);
        this.setColor(color);
        this.setThickness(thickness);
        this.setBranches(branches);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(END_OFFSET_X, 0.0F);
        this.dataTracker.startTracking(END_OFFSET_Y, 0.0F);
        this.dataTracker.startTracking(END_OFFSET_Z, 0.0F);
        this.dataTracker.startTracking(LIFETIME, 8);
        this.dataTracker.startTracking(SEED, 0);
        this.dataTracker.startTracking(COLOR, 0x83E8FF);
        this.dataTracker.startTracking(THICKNESS, 0.075F);
        this.dataTracker.startTracking(BRANCHES, 4);
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

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setColor(int color) {
        this.dataTracker.set(COLOR, color);
    }

    public int getColor() {
        return this.dataTracker.get(COLOR);
    }

    public void setThickness(float thickness) {
        this.dataTracker.set(THICKNESS, thickness);
    }

    public float getThickness() {
        return this.dataTracker.get(THICKNESS);
    }

    public void setBranches(int branches) {
        this.dataTracker.set(BRANCHES, branches);
    }

    public int getBranches() {
        return this.dataTracker.get(BRANCHES);
    }

    public double getBoltLength() {
        return Math.sqrt(this.getEndOffsetX() * this.getEndOffsetX()
                + this.getEndOffsetY() * this.getEndOffsetY()
                + this.getEndOffsetZ() * this.getEndOffsetZ());
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setEndOffset(nbt.getFloat("end_offset_x"), nbt.getFloat("end_offset_y"), nbt.getFloat("end_offset_z"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setSeed(nbt.getInt("seed"));
        this.setColor(nbt.getInt("color"));
        this.setThickness(nbt.getFloat("thickness"));
        this.setBranches(nbt.getInt("branches"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("end_offset_x", this.getEndOffsetX());
        nbt.putFloat("end_offset_y", this.getEndOffsetY());
        nbt.putFloat("end_offset_z", this.getEndOffsetZ());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("seed", this.getSeed());
        nbt.putInt("color", this.getColor());
        nbt.putFloat("thickness", this.getThickness());
        nbt.putInt("branches", this.getBranches());
    }
}
