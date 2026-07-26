package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class DragonWingBuffetVisualEntity extends Entity {

    public static final double BACK_OFFSET_BLOCKS = 0.35;
    public static final double VERTICAL_OFFSET_BLOCKS = -1.5;

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(DragonWingBuffetVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> OWNER_YAW = DataTracker.registerData(DragonWingBuffetVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(DragonWingBuffetVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(DragonWingBuffetVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> PHASE_OFFSET = DataTracker.registerData(DragonWingBuffetVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public DragonWingBuffetVisualEntity(EntityType<? extends DragonWingBuffetVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public DragonWingBuffetVisualEntity(World world, LivingEntity owner, double x, double y, double z, float ownerYaw, int lifetime, float scale, float phaseOffset) {
        this(EntityRegistry.DRAGON_WING_BUFFET_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setOwnerEntityId(owner == null ? -1 : owner.getId());
        this.setOwnerYaw(ownerYaw);
        this.setLifetime(lifetime);
        this.setScale(scale);
        this.setPhaseOffset(phaseOffset);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(OWNER_YAW, 0.0F);
        builder.add(LIFETIME, 18);
        builder.add(SCALE, 0.65F);
        builder.add(PHASE_OFFSET, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.updateOwnerPosition();
        if (!this.getWorld().isClient() && this.age > this.getLifetime()) {
            this.discard();
        }
    }

    private void updateOwnerPosition() {
        Entity owner = this.getWorld().getEntityById(this.getOwnerEntityId());
        if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
            return;
        }

        this.setPosition(getAnchorPosition(livingOwner, livingOwner.getRotationVec(1.0F)));
        this.setOwnerYaw(livingOwner.getYaw());
    }

    public static Vec3d getAnchorPosition(LivingEntity owner, Vec3d direction) {
        Vec3d horizontalDirection = new Vec3d(direction.x, 0.0, direction.z);
        horizontalDirection = horizontalDirection.lengthSquared() > 1.0E-5 ? horizontalDirection.normalize() : Vec3d.fromPolar(0.0F, owner.getYaw()).normalize();
        Vec3d backOffset = horizontalDirection.multiply(-BACK_OFFSET_BLOCKS);
        return new Vec3d(owner.getX() + backOffset.x, owner.getY() + owner.getHeight() * 0.55 + VERTICAL_OFFSET_BLOCKS, owner.getZ() + backOffset.z);
    }

    public void setOwnerEntityId(int ownerEntityId) {
        this.dataTracker.set(OWNER_ENTITY_ID, ownerEntityId);
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public void setOwnerYaw(float ownerYaw) {
        this.dataTracker.set(OWNER_YAW, ownerYaw);
    }

    public float getOwnerYaw() {
        return this.dataTracker.get(OWNER_YAW);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setPhaseOffset(float phaseOffset) {
        this.dataTracker.set(PHASE_OFFSET, phaseOffset);
    }

    public float getPhaseOffset() {
        return this.dataTracker.get(PHASE_OFFSET);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setOwnerEntityId(nbt.getInt("owner_entity_id"));
        this.setOwnerYaw(nbt.getFloat("owner_yaw"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setScale(nbt.getFloat("scale"));
        this.setPhaseOffset(nbt.getFloat("phase_offset"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("owner_entity_id", this.getOwnerEntityId());
        nbt.putFloat("owner_yaw", this.getOwnerYaw());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putFloat("scale", this.getScale());
        nbt.putFloat("phase_offset", this.getPhaseOffset());
    }
}
