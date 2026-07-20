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

public class BrimstoneClaymoreVisualEntity extends Entity {

    private static final TrackedData<Float> RADIUS = DataTracker.registerData(BrimstoneClaymoreVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(BrimstoneClaymoreVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> PLUNGING = DataTracker.registerData(BrimstoneClaymoreVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PLUNGE_START_AGE = DataTracker.registerData(BrimstoneClaymoreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BrimstoneClaymoreVisualEntity(EntityType<? extends BrimstoneClaymoreVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BrimstoneClaymoreVisualEntity(World world, double x, double y, double z, float radius) {
        this(EntityRegistry.BRIMSTONE_CLAYMORE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setRadius(radius);
        this.setScale(1.0F);
        this.setPlunging(false);
        this.setPlungeStartAge(0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(RADIUS, 2.5F);
        builder.add(SCALE, 1.0F);
        builder.add(PLUNGING, false);
        builder.add(PLUNGE_START_AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient && !this.isPlunging() && this.age % 2 == 0) {
            this.getWorld().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.02, 0.0);
            this.getWorld().addParticle(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.getWorld().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25, this.getZ(), 0.0, 0.03, 0.0);
        }
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, radius);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
    }

    public boolean isPlunging() {
        return this.dataTracker.get(PLUNGING);
    }

    public void setPlunging(boolean plunging) {
        this.dataTracker.set(PLUNGING, plunging);
    }

    public int getPlungeStartAge() {
        return this.dataTracker.get(PLUNGE_START_AGE);
    }

    public void setPlungeStartAge(int plungeStartAge) {
        this.dataTracker.set(PLUNGE_START_AGE, plungeStartAge);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("radius")) {
            this.setRadius(nbt.getFloat("radius"));
        }
        if (nbt.contains("scale")) {
            this.setScale(nbt.getFloat("scale"));
        }
        if (nbt.contains("plunging")) {
            this.setPlunging(nbt.getBoolean("plunging"));
        }
        if (nbt.contains("plunge_start_age")) {
            this.setPlungeStartAge(nbt.getInt("plunge_start_age"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("radius", this.getRadius());
        nbt.putFloat("scale", this.getScale());
        nbt.putBoolean("plunging", this.isPlunging());
        nbt.putInt("plunge_start_age", this.getPlungeStartAge());
    }
}
