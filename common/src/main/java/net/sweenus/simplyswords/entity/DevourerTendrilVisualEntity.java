package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class DevourerTendrilVisualEntity extends Entity {
    private static final TrackedData<Integer> MASS_ID = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE_AGE = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> RETRACT_AGE = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> END_X = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Y = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Z = DataTracker.registerData(DevourerTendrilVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public DevourerTendrilVisualEntity(EntityType<? extends DevourerTendrilVisualEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public DevourerTendrilVisualEntity(World world, DevourerMassVisualEntity mass,
                                       Entity target, int lifetime) {
        this(EntityRegistry.DEVOURER_TENDRIL_VISUAL.get(), world);
        setPosition(mass.getPos());
        dataTracker.set(MASS_ID, mass.getId());
        dataTracker.set(TARGET_ID, target.getId());
        dataTracker.set(LIFETIME, Math.max(1, lifetime));
        dataTracker.set(SEED, world.random.nextInt());
        setStoredEnd(target.getY() + target.getHeight() * 0.55, target.getX(), target.getZ());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(MASS_ID, -1);
        builder.add(TARGET_ID, -1);
        builder.add(LIFETIME, 120);
        builder.add(SEED, 0);
        builder.add(PULSE_AGE, -1000);
        builder.add(RETRACT_AGE, -1);
        builder.add(END_X, 0.0F);
        builder.add(END_Y, 0.0F);
        builder.add(END_Z, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) {
            return;
        }
        DevourerMassVisualEntity mass = getMass();
        if (mass == null || mass.isRemoved() || age >= getLifetime()) {
            discard();
            return;
        }
        Entity target = getTarget();
        if (target != null) {
            setStoredEnd(target.getY() + target.getHeight() * 0.55, target.getX(), target.getZ());
        } else if (getRetractAge() < 0) {
            beginRetraction(getStoredEnd());
        }
        if (getRetractAge() >= 0 && age - getRetractAge() >= 7) {
            discard();
        }
    }

    public DevourerMassVisualEntity getMass() {
        Entity entity = getMassId() < 0 ? null : getWorld().getEntityById(getMassId());
        return entity instanceof DevourerMassVisualEntity mass ? mass : null;
    }

    public Entity getTarget() {
        Entity entity = getTargetId() < 0 ? null : getWorld().getEntityById(getTargetId());
        return entity != null && entity.isAlive() && !entity.isRemoved() ? entity : null;
    }

    public void triggerPulse() {
        dataTracker.set(PULSE_AGE, age);
    }

    public void beginRetraction(Vec3d endpoint) {
        setStoredEnd(endpoint);
        dataTracker.set(TARGET_ID, -1);
        dataTracker.set(RETRACT_AGE, age);
    }

    public int getMassId() { return dataTracker.get(MASS_ID); }
    public int getTargetId() { return dataTracker.get(TARGET_ID); }
    public int getLifetime() { return dataTracker.get(LIFETIME); }
    public int getSeed() { return dataTracker.get(SEED); }
    public int getPulseAge() { return dataTracker.get(PULSE_AGE); }
    public int getRetractAge() { return dataTracker.get(RETRACT_AGE); }

    public Vec3d getStoredEnd() {
        return new Vec3d(dataTracker.get(END_X), dataTracker.get(END_Y), dataTracker.get(END_Z));
    }

    private void setStoredEnd(Vec3d endpoint) {
        setStoredEnd(endpoint.y, endpoint.x, endpoint.z);
    }

    private void setStoredEnd(double y, double x, double z) {
        dataTracker.set(END_X, (float) x);
        dataTracker.set(END_Y, (float) y);
        dataTracker.set(END_Z, (float) z);
    }

    @Override
    public Box getVisibilityBoundingBox() {
        DevourerMassVisualEntity mass = getMass();
        Vec3d start = mass == null ? getPos() : mass.getPos();
        Entity target = getTarget();
        Vec3d end = target == null ? getStoredEnd()
                : target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        return new Box(start, end).expand(2.5);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(MASS_ID, nbt.getInt("mass_id"));
        dataTracker.set(TARGET_ID, nbt.getInt("target_id"));
        dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        dataTracker.set(SEED, nbt.getInt("seed"));
        dataTracker.set(PULSE_AGE, nbt.getInt("pulse_age"));
        dataTracker.set(RETRACT_AGE, nbt.getInt("retract_age"));
        dataTracker.set(END_X, nbt.getFloat("end_x"));
        dataTracker.set(END_Y, nbt.getFloat("end_y"));
        dataTracker.set(END_Z, nbt.getFloat("end_z"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("mass_id", getMassId());
        nbt.putInt("target_id", getTargetId());
        nbt.putInt("lifetime", getLifetime());
        nbt.putInt("seed", getSeed());
        nbt.putInt("pulse_age", getPulseAge());
        nbt.putInt("retract_age", getRetractAge());
        nbt.putFloat("end_x", dataTracker.get(END_X));
        nbt.putFloat("end_y", dataTracker.get(END_Y));
        nbt.putFloat("end_z", dataTracker.get(END_Z));
    }
}
