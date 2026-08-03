package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class ShadowstingAfterimageVisualEntity extends Entity {

    private static final TrackedData<Float> VISUAL_YAW = DataTracker.registerData(ShadowstingAfterimageVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(ShadowstingAfterimageVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public ShadowstingAfterimageVisualEntity(EntityType<? extends ShadowstingAfterimageVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public ShadowstingAfterimageVisualEntity(World world, double x, double y, double z, float visualYaw, int lifetime) {
        this(EntityRegistry.SHADOWSTING_AFTERIMAGE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setVisualYaw(visualYaw);
        this.setYaw(visualYaw);
        this.setLifetime(lifetime);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(VISUAL_YAW, 0.0F);
        this.dataTracker.startTracking(LIFETIME, 8);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age > this.getLifetime()) {
            this.discard();
        }
    }

    public float getVisualYaw() {
        return this.dataTracker.get(VISUAL_YAW);
    }

    public void setVisualYaw(float visualYaw) {
        this.dataTracker.set(VISUAL_YAW, visualYaw);
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
        this.setVisualYaw(nbt.getFloat("visual_yaw"));
        if (nbt.contains("lifetime")) {
            this.setLifetime(nbt.getInt("lifetime"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("visual_yaw", this.getVisualYaw());
        nbt.putInt("lifetime", this.getLifetime());
    }
}
