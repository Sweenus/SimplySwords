package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class FlameSeedVisualEntity extends Entity {

    private static final TrackedData<Float> SCALE = DataTracker.registerData(FlameSeedVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public FlameSeedVisualEntity(EntityType<? extends FlameSeedVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public FlameSeedVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.FLAME_SEED_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setScale(1.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SCALE, 1.0F);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("scale")) {
            this.setScale(nbt.getFloat("scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("scale", this.getScale());
    }
}
