package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class BloodPlagueSpreadVisualEntity extends Entity {
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(BloodPlagueSpreadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(BloodPlagueSpreadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(BloodPlagueSpreadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BloodPlagueSpreadVisualEntity(EntityType<? extends BloodPlagueSpreadVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BloodPlagueSpreadVisualEntity(World world, Entity target, float radius, int lifetime) {
        this(EntityRegistry.BLOOD_PLAGUE_SPREAD_VISUAL.get(), world);
        this.setTargetId(target.getId());
        this.setRadius(radius);
        this.setLifetime(lifetime);
        this.setPosition(target.getPos());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(RADIUS, 4.0F);
        this.dataTracker.startTracking(LIFETIME, 8);
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = this.getWorld().getEntityById(this.getTargetId());
        if (target != null && target.isAlive()) {
            this.setPosition(target.getPos());
        }
        if (!this.getWorld().isClient()
                && (target == null || !target.isAlive() || this.age > this.getLifetime())) {
            this.discard();
        }
    }

    public int getTargetId() {
        return this.dataTracker.get(TARGET_ID);
    }

    public void setTargetId(int targetId) {
        this.dataTracker.set(TARGET_ID, targetId);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.5F, radius));
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setTargetId(nbt.getInt("target_id"));
        this.setRadius(nbt.getFloat("radius"));
        this.setLifetime(nbt.getInt("lifetime"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("target_id", this.getTargetId());
        nbt.putFloat("radius", this.getRadius());
        nbt.putInt("lifetime", this.getLifetime());
    }
}
