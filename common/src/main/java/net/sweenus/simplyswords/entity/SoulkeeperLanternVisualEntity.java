package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

import java.util.UUID;

public class SoulkeeperLanternVisualEntity extends Entity {

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(SoulkeeperLanternVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LANTERN_COUNT = DataTracker.registerData(SoulkeeperLanternVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SPEED_MULTIPLIER = DataTracker.registerData(SoulkeeperLanternVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_RADIUS = DataTracker.registerData(SoulkeeperLanternVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_PHASE = DataTracker.registerData(SoulkeeperLanternVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private UUID ownerUuid;

    public SoulkeeperLanternVisualEntity(EntityType<? extends SoulkeeperLanternVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public SoulkeeperLanternVisualEntity(World world, UUID ownerUuid, int ownerEntityId, double x, double y, double z) {
        this(EntityRegistry.SOULKEEPER_LANTERN_VISUAL.get(), world);
        this.ownerUuid = ownerUuid;
        this.setOwnerEntityId(ownerEntityId);
        this.setLanternCount(2);
        this.setSpeedMultiplier(1.0F);
        this.setOrbitRadius(2.65F);
        this.setOrbitPhase(0.0F);
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(LANTERN_COUNT, 2);
        builder.add(SPEED_MULTIPLIER, 1.0F);
        builder.add(ORBIT_RADIUS, 2.65F);
        builder.add(ORBIT_PHASE, 0.0F);
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public void setOwnerEntityId(int ownerEntityId) {
        this.dataTracker.set(OWNER_ENTITY_ID, ownerEntityId);
    }

    public int getLanternCount() {
        return this.dataTracker.get(LANTERN_COUNT);
    }

    public void setLanternCount(int lanternCount) {
        this.dataTracker.set(LANTERN_COUNT, lanternCount);
    }

    public float getSpeedMultiplier() {
        return this.dataTracker.get(SPEED_MULTIPLIER);
    }

    public void setSpeedMultiplier(float speedMultiplier) {
        this.dataTracker.set(SPEED_MULTIPLIER, speedMultiplier);
    }

    public float getOrbitRadius() {
        return this.dataTracker.get(ORBIT_RADIUS);
    }

    public void setOrbitRadius(float orbitRadius) {
        this.dataTracker.set(ORBIT_RADIUS, orbitRadius);
    }

    public float getOrbitPhase() {
        return this.dataTracker.get(ORBIT_PHASE);
    }

    public void setOrbitPhase(float orbitPhase) {
        this.dataTracker.set(ORBIT_PHASE, orbitPhase);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("owner_uuid")) {
            this.ownerUuid = nbt.getUuid("owner_uuid");
        }
        if (nbt.contains("owner_entity_id")) {
            this.setOwnerEntityId(nbt.getInt("owner_entity_id"));
        }
        if (nbt.contains("lantern_count")) {
            this.setLanternCount(nbt.getInt("lantern_count"));
        }
        if (nbt.contains("speed_multiplier")) {
            this.setSpeedMultiplier(nbt.getFloat("speed_multiplier"));
        }
        if (nbt.contains("orbit_radius")) {
            this.setOrbitRadius(nbt.getFloat("orbit_radius"));
        }
        if (nbt.contains("orbit_phase")) {
            this.setOrbitPhase(nbt.getFloat("orbit_phase"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putInt("owner_entity_id", this.getOwnerEntityId());
        nbt.putInt("lantern_count", this.getLanternCount());
        nbt.putFloat("speed_multiplier", this.getSpeedMultiplier());
        nbt.putFloat("orbit_radius", this.getOrbitRadius());
        nbt.putFloat("orbit_phase", this.getOrbitPhase());
    }
}
