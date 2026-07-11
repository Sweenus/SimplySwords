package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class EmberlashSmoulderVisualEntity extends Entity {

    private static final TrackedData<Integer> STACKS = DataTracker.registerData(EmberlashSmoulderVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(EmberlashSmoulderVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public EmberlashSmoulderVisualEntity(EntityType<? extends EmberlashSmoulderVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public EmberlashSmoulderVisualEntity(World world, double x, double y, double z, int stacks) {
        this(EntityRegistry.EMBERLASH_SMOULDER_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setStacks(stacks);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STACKS, 1);
        builder.add(SCALE, 1.0F);
    }

    public void setStacks(int stacks) {
        this.dataTracker.set(STACKS, Math.max(1, stacks));
    }

    public int getStacks() {
        return this.dataTracker.get(STACKS);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, Math.max(0.0F, scale));
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
        if (nbt.contains("stacks")) {
            this.setStacks(nbt.getInt("stacks"));
        }
        if (nbt.contains("scale")) {
            this.setScale(nbt.getFloat("scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("stacks", this.getStacks());
        nbt.putFloat("scale", this.getScale());
    }
}
