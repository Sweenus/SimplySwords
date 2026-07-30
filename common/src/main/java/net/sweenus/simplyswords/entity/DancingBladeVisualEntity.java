package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.DancingBladeManager;

import java.util.UUID;

public class DancingBladeVisualEntity extends Entity {

    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ORBIT_SLOT = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> ORBIT_RADIUS = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ORBIT_PHASE = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> ATTACK_START_AGE = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> ATTACK_DIRECTION_X = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ATTACK_DIRECTION_Z = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(DancingBladeVisualEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private UUID ownerUuid;
    private long expiresAtTick;
    private float damage;

    public DancingBladeVisualEntity(EntityType<? extends DancingBladeVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public DancingBladeVisualEntity(
            World world,
            UUID ownerUuid,
            int ownerEntityId,
            ItemStack stack,
            int orbitSlot,
            double orbitRadius,
            long expiresAtTick,
            float damage) {
        this(EntityRegistry.DANCING_BLADE_VISUAL.get(), world);
        this.ownerUuid = ownerUuid;
        this.setOwnerEntityId(ownerEntityId);
        this.setOrbitSlot(orbitSlot);
        this.setOrbitRadius((float) orbitRadius);
        this.setItemStack(stack.copy());
        this.expiresAtTick = expiresAtTick;
        this.damage = Math.max(0.0F, damage);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ENTITY_ID, -1);
        builder.add(ORBIT_SLOT, 0);
        builder.add(ORBIT_RADIUS, 2.65F);
        builder.add(ORBIT_PHASE, 0.0F);
        builder.add(ATTACK_START_AGE, -1000);
        builder.add(ATTACK_DIRECTION_X, 0.0F);
        builder.add(ATTACK_DIRECTION_Z, 1.0F);
        builder.add(ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            DancingBladeManager.tickBlade(this);
        }
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

    public int getOrbitSlot() {
        return this.dataTracker.get(ORBIT_SLOT);
    }

    public void setOrbitSlot(int orbitSlot) {
        this.dataTracker.set(ORBIT_SLOT, orbitSlot);
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

    public int getAttackStartAge() {
        return this.dataTracker.get(ATTACK_START_AGE);
    }

    public void triggerAttackAnimation(double directionX, double directionZ) {
        double length = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (length > 0.0001) {
            this.dataTracker.set(ATTACK_DIRECTION_X, (float) (directionX / length));
            this.dataTracker.set(ATTACK_DIRECTION_Z, (float) (directionZ / length));
        }
        this.dataTracker.set(ATTACK_START_AGE, this.age);
    }

    public float getAttackDirectionX() {
        return this.dataTracker.get(ATTACK_DIRECTION_X);
    }

    public float getAttackDirectionZ() {
        return this.dataTracker.get(ATTACK_DIRECTION_Z);
    }

    public ItemStack getWeaponStack() {
        ItemStack stack = this.dataTracker.get(ITEM_STACK);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public void setItemStack(ItemStack stack) {
        this.dataTracker.set(ITEM_STACK, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public long getExpiresAtTick() {
        return this.expiresAtTick;
    }

    public void setExpiresAtTick(long expiresAtTick) {
        this.expiresAtTick = expiresAtTick;
    }

    public float getDamage() {
        return this.damage;
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
        if (nbt.contains("orbit_slot")) {
            this.setOrbitSlot(nbt.getInt("orbit_slot"));
        }
        if (nbt.contains("orbit_radius")) {
            this.setOrbitRadius(nbt.getFloat("orbit_radius"));
        }
        if (nbt.contains("orbit_phase")) {
            this.setOrbitPhase(nbt.getFloat("orbit_phase"));
        }
        if (nbt.contains("attack_start_age")) {
            this.dataTracker.set(ATTACK_START_AGE, nbt.getInt("attack_start_age"));
        }
        if (nbt.contains("attack_direction_x")) {
            this.dataTracker.set(ATTACK_DIRECTION_X, nbt.getFloat("attack_direction_x"));
        }
        if (nbt.contains("attack_direction_z")) {
            this.dataTracker.set(ATTACK_DIRECTION_Z, nbt.getFloat("attack_direction_z"));
        }
        if (nbt.contains("expires_at_tick")) {
            this.setExpiresAtTick(nbt.getLong("expires_at_tick"));
        }
        if (nbt.contains("damage")) {
            this.damage = Math.max(0.0F, nbt.getFloat("damage"));
        }
        if (nbt.contains("item")) {
            this.setItemStack(ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("item")).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putInt("owner_entity_id", this.getOwnerEntityId());
        nbt.putInt("orbit_slot", this.getOrbitSlot());
        nbt.putFloat("orbit_radius", this.getOrbitRadius());
        nbt.putFloat("orbit_phase", this.getOrbitPhase());
        nbt.putInt("attack_start_age", this.getAttackStartAge());
        nbt.putFloat("attack_direction_x", this.getAttackDirectionX());
        nbt.putFloat("attack_direction_z", this.getAttackDirectionZ());
        nbt.putLong("expires_at_tick", this.getExpiresAtTick());
        nbt.putFloat("damage", this.getDamage());
        ItemStack stack = this.getWeaponStack();
        if (stack != null && !stack.isEmpty()) {
            nbt.put("item", stack.encode(this.getRegistryManager()));
        }
    }
}
