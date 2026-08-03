package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class MagibladeWardenHeadVisualEntity extends Entity {

    public static final int DISMISS_DURATION_TICKS = 12;

    private static final TrackedData<Integer> OWNER_ENTITY_ID =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ENTITY_ID =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> TARGET_YAW =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TARGET_PITCH =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_PHASE =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> AIMING =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> SHOT_PULSE_TICKS =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DISMISSING =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> DISMISS_TICKS =
            DataTracker.registerData(MagibladeWardenHeadVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public MagibladeWardenHeadVisualEntity(EntityType<? extends MagibladeWardenHeadVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public MagibladeWardenHeadVisualEntity(World world, Entity owner, double x, double y, double z,
                                           float scale, float orbitPhase) {
        this(EntityRegistry.MAGIBLADE_WARDEN_HEAD_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setOwnerEntityId(owner == null ? -1 : owner.getId());
        this.setScale(scale);
        this.setOrbitPhase(orbitPhase);
        float ownerYaw = owner == null ? 0.0F : owner.getYaw();
        this.setTargetYaw(ownerYaw);
        this.setYaw(ownerYaw);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(OWNER_ENTITY_ID, -1);
        this.dataTracker.startTracking(TARGET_ENTITY_ID, -1);
        this.dataTracker.startTracking(TARGET_YAW, 0.0F);
        this.dataTracker.startTracking(TARGET_PITCH, 0.0F);
        this.dataTracker.startTracking(SCALE, 0.65F);
        this.dataTracker.startTracking(ORBIT_PHASE, 0.0F);
        this.dataTracker.startTracking(AIMING, false);
        this.dataTracker.startTracking(SHOT_PULSE_TICKS, 0);
        this.dataTracker.startTracking(DISMISSING, false);
        this.dataTracker.startTracking(DISMISS_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        if (this.getShotPulseTicks() > 0) {
            this.setShotPulseTicks(this.getShotPulseTicks() - 1);
        }
        if (this.isDismissing()) {
            this.setDismissTicks(this.getDismissTicks() + 1);
            if (this.getDismissTicks() >= DISMISS_DURATION_TICKS) {
                this.discard();
            }
            return;
        }

        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            this.beginDismissal();
        }
    }

    public void beginDismissal() {
        if (this.isDismissing()) {
            return;
        }
        this.setDismissing(true);
        this.setDismissTicks(0);
        this.setAiming(false);
        this.setTargetEntityId(-1);
    }

    public Entity getOwner() {
        int ownerId = this.getOwnerEntityId();
        return ownerId < 0 ? null : this.getWorld().getEntityById(ownerId);
    }

    public void setOwnerEntityId(int ownerEntityId) {
        this.dataTracker.set(OWNER_ENTITY_ID, ownerEntityId);
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public void setTargetEntityId(int targetEntityId) {
        this.dataTracker.set(TARGET_ENTITY_ID, targetEntityId);
    }

    public int getTargetEntityId() {
        return this.dataTracker.get(TARGET_ENTITY_ID);
    }

    public void setTargetYaw(float targetYaw) {
        this.dataTracker.set(TARGET_YAW, targetYaw);
        this.setYaw(targetYaw);
    }

    public float getTargetYaw() {
        return this.dataTracker.get(TARGET_YAW);
    }

    public void setTargetPitch(float targetPitch) {
        this.dataTracker.set(TARGET_PITCH, targetPitch);
        this.setPitch(targetPitch);
    }

    public float getTargetPitch() {
        return this.dataTracker.get(TARGET_PITCH);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, Math.max(0.1F, scale));
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setOrbitPhase(float orbitPhase) {
        this.dataTracker.set(ORBIT_PHASE, orbitPhase);
    }

    public float getOrbitPhase() {
        return this.dataTracker.get(ORBIT_PHASE);
    }

    public void setAiming(boolean aiming) {
        this.dataTracker.set(AIMING, aiming);
    }

    public boolean isAiming() {
        return this.dataTracker.get(AIMING);
    }

    public void setShotPulseTicks(int ticks) {
        this.dataTracker.set(SHOT_PULSE_TICKS, Math.max(0, ticks));
    }

    public int getShotPulseTicks() {
        return this.dataTracker.get(SHOT_PULSE_TICKS);
    }

    public void setDismissing(boolean dismissing) {
        this.dataTracker.set(DISMISSING, dismissing);
    }

    public boolean isDismissing() {
        return this.dataTracker.get(DISMISSING);
    }

    public void setDismissTicks(int ticks) {
        this.dataTracker.set(DISMISS_TICKS, Math.max(0, ticks));
    }

    public int getDismissTicks() {
        return this.dataTracker.get(DISMISS_TICKS);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setOwnerEntityId(nbt.getInt("owner_entity_id"));
        this.setTargetEntityId(nbt.getInt("target_entity_id"));
        this.setTargetYaw(nbt.getFloat("target_yaw"));
        this.setTargetPitch(nbt.getFloat("target_pitch"));
        this.setScale(nbt.getFloat("scale"));
        this.setOrbitPhase(nbt.getFloat("orbit_phase"));
        this.setAiming(nbt.getBoolean("aiming"));
        this.setShotPulseTicks(nbt.getInt("shot_pulse_ticks"));
        this.setDismissing(nbt.getBoolean("dismissing"));
        this.setDismissTicks(nbt.getInt("dismiss_ticks"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("owner_entity_id", this.getOwnerEntityId());
        nbt.putInt("target_entity_id", this.getTargetEntityId());
        nbt.putFloat("target_yaw", this.getTargetYaw());
        nbt.putFloat("target_pitch", this.getTargetPitch());
        nbt.putFloat("scale", this.getScale());
        nbt.putFloat("orbit_phase", this.getOrbitPhase());
        nbt.putBoolean("aiming", this.isAiming());
        nbt.putInt("shot_pulse_ticks", this.getShotPulseTicks());
        nbt.putBoolean("dismissing", this.isDismissing());
        nbt.putInt("dismiss_ticks", this.getDismissTicks());
    }
}
