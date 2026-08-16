package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class RiftmaneRiftVisualEntity extends Entity {

    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(RiftmaneRiftVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(RiftmaneRiftVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> HEIGHT =
            DataTracker.registerData(RiftmaneRiftVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public RiftmaneRiftVisualEntity(EntityType<? extends RiftmaneRiftVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public RiftmaneRiftVisualEntity(World world, double x, double y, double z, float yaw,
                                    int lifetime, float height, int seed) {
        this(EntityRegistry.RIFTMANE_RIFT_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setYaw(yaw);
        this.prevYaw = yaw;
        this.setLifetime(lifetime);
        this.setRiftHeight(height);
        this.setSeed(seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LIFETIME, 30);
        builder.add(SEED, 0);
        builder.add(HEIGHT, 2.4F);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (!this.getWorld().isClient() && this.age >= this.getLifetime()) {
            this.discard();
        }
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
    }

    public float getRiftHeight() {
        return this.dataTracker.get(HEIGHT);
    }

    public void setRiftHeight(float height) {
        this.dataTracker.set(HEIGHT, Math.max(0.5F, height));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setLifetime(nbt.getInt("lifetime"));
        this.setSeed(nbt.getInt("seed"));
        this.setRiftHeight(nbt.getFloat("height"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("seed", this.getSeed());
        nbt.putFloat("height", this.getRiftHeight());
    }
}
