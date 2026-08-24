package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class WhisperwindSlashVisualEntity extends Entity {

    private static final TrackedData<Float> END_OFFSET_X = DataTracker.registerData(WhisperwindSlashVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Y = DataTracker.registerData(WhisperwindSlashVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Z = DataTracker.registerData(WhisperwindSlashVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(WhisperwindSlashVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public WhisperwindSlashVisualEntity(EntityType<? extends WhisperwindSlashVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public WhisperwindSlashVisualEntity(World world, double x, double y, double z, double endOffsetX, double endOffsetY, double endOffsetZ, int lifetime) {
        this(EntityRegistry.WHISPERWIND_SLASH_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setEndOffset((float) endOffsetX, (float) endOffsetY, (float) endOffsetZ);
        this.setLifetime(lifetime);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(END_OFFSET_X, 0.0F);
        builder.add(END_OFFSET_Y, 0.0F);
        builder.add(END_OFFSET_Z, 0.0F);
        builder.add(LIFETIME, 12);
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

    public double getSlashLength() {
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
        if (nbt.contains("lifetime")) {
            this.setLifetime(nbt.getInt("lifetime"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("end_offset_x", this.getEndOffsetX());
        nbt.putFloat("end_offset_y", this.getEndOffsetY());
        nbt.putFloat("end_offset_z", this.getEndOffsetZ());
        nbt.putInt("lifetime", this.getLifetime());
    }
}
