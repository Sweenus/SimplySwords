package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class VerdantTrailVisualEntity extends Entity {

    private static final TrackedData<Integer> PLANT_TYPE = DataTracker.registerData(VerdantTrailVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(VerdantTrailVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public VerdantTrailVisualEntity(EntityType<? extends VerdantTrailVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public VerdantTrailVisualEntity(World world, double x, double y, double z, int plantType) {
        this(EntityRegistry.VERDANT_TRAIL_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setPlantType(plantType);
        this.setHeightScale(0.0F);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(PLANT_TYPE, 0);
        this.dataTracker.startTracking(HEIGHT_SCALE, 0.0F);
    }

    public int getPlantType() {
        return this.dataTracker.get(PLANT_TYPE);
    }

    public void setPlantType(int plantType) {
        this.dataTracker.set(PLANT_TYPE, plantType);
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
        if (nbt.contains("plant_type")) {
            this.setPlantType(nbt.getInt("plant_type"));
        }
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("plant_type", this.getPlantType());
        nbt.putFloat("height_scale", this.getHeightScale());
    }
}
