package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class StormscaleRodVisualEntity extends Entity {

    private static final TrackedData<Float> RADIUS = DataTracker.registerData(
            StormscaleRodVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(
            StormscaleRodVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE = DataTracker.registerData(
            StormscaleRodVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE_START_AGE = DataTracker.registerData(
            StormscaleRodVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public StormscaleRodVisualEntity(EntityType<? extends StormscaleRodVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public StormscaleRodVisualEntity(World world, double x, double y, double z,
                                     float radius, int lifetime, float yaw) {
        this(EntityRegistry.STORMSCALE_ROD_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setRadius(radius);
        this.setLifetime(lifetime);
        this.setYaw(yaw);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(RADIUS, 3.5F);
        this.dataTracker.startTracking(LIFETIME, 210);
        this.dataTracker.startTracking(PULSE, 0);
        this.dataTracker.startTracking(PULSE_START_AGE, -1000);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            if (this.age > getLifetime()) {
                this.discard();
            }
            return;
        }
        if (this.age % 3 == 0) {
            this.getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.45,
                    this.getY() + 0.15 + this.random.nextDouble() * 2.1,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.45,
                    0.0, 0.018, 0.0);
        }
    }

    public void triggerPulse() {
        this.dataTracker.set(PULSE, getPulse() + 1);
        this.dataTracker.set(PULSE_START_AGE, this.age);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.1F, radius));
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getPulse() {
        return this.dataTracker.get(PULSE);
    }

    public int getPulseStartAge() {
        return this.dataTracker.get(PULSE_START_AGE);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setRadius(nbt.getFloat("radius"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.dataTracker.set(PULSE, nbt.getInt("pulse"));
        this.dataTracker.set(PULSE_START_AGE, nbt.getInt("pulse_start_age"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("radius", getRadius());
        nbt.putInt("lifetime", getLifetime());
        nbt.putInt("pulse", getPulse());
        nbt.putInt("pulse_start_age", getPulseStartAge());
    }
}
