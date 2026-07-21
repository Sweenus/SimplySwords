package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class EarthshatterSpikeVisualEntity extends Entity {

    private static final TrackedData<Float> TARGET_HEIGHT = DataTracker.registerData(EarthshatterSpikeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(EarthshatterSpikeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public EarthshatterSpikeVisualEntity(EntityType<? extends EarthshatterSpikeVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public EarthshatterSpikeVisualEntity(World world, double x, double y, double z, float targetHeight) {
        this(EntityRegistry.EARTHSHATTER_SPIKE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setTargetHeight(targetHeight);
        this.setHeightScale(0.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(TARGET_HEIGHT, 1.0F);
        builder.add(HEIGHT_SCALE, 0.0F);
    }

    public float getTargetHeight() {
        return this.dataTracker.get(TARGET_HEIGHT);
    }

    public void setTargetHeight(float targetHeight) {
        this.dataTracker.set(TARGET_HEIGHT, targetHeight);
    }

    public float getHeightScale() {
        return this.dataTracker.get(HEIGHT_SCALE);
    }

    public void setHeightScale(float heightScale) {
        this.dataTracker.set(HEIGHT_SCALE, heightScale);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("target_height")) {
            this.setTargetHeight(nbt.getFloat("target_height"));
        }
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("target_height", this.getTargetHeight());
        nbt.putFloat("height_scale", this.getHeightScale());
    }
}
