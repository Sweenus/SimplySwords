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

public class RevivalCandleVisualEntity extends Entity {

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(RevivalCandleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> WEAPON_TYPE = DataTracker.registerData(RevivalCandleVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(RevivalCandleVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> ACTIVATING = DataTracker.registerData(RevivalCandleVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private UUID ownerUuid;

    public RevivalCandleVisualEntity(EntityType<? extends RevivalCandleVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public RevivalCandleVisualEntity(World world, UUID ownerUuid, int ownerEntityId, int weaponType, double x, double y, double z) {
        this(EntityRegistry.REVIVAL_CANDLE_VISUAL.get(), world);
        this.ownerUuid = ownerUuid;
        this.setOwnerEntityId(ownerEntityId);
        this.setWeaponType(weaponType);
        this.setScale(1.0F);
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(WEAPON_TYPE, 0);
        builder.add(SCALE, 1.0F);
        builder.add(ACTIVATING, false);
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

    public int getWeaponType() {
        return this.dataTracker.get(WEAPON_TYPE);
    }

    public void setWeaponType(int weaponType) {
        this.dataTracker.set(WEAPON_TYPE, weaponType);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
    }

    public boolean isActivating() {
        return this.dataTracker.get(ACTIVATING);
    }

    public void setActivating(boolean activating) {
        this.dataTracker.set(ACTIVATING, activating);
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
        if (nbt.contains("weapon_type")) {
            this.setWeaponType(nbt.getInt("weapon_type"));
        }
        if (nbt.contains("scale")) {
            this.setScale(nbt.getFloat("scale"));
        }
        if (nbt.contains("activating")) {
            this.setActivating(nbt.getBoolean("activating"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putInt("owner_entity_id", this.getOwnerEntityId());
        nbt.putInt("weapon_type", this.getWeaponType());
        nbt.putFloat("scale", this.getScale());
        nbt.putBoolean("activating", this.isActivating());
    }
}
