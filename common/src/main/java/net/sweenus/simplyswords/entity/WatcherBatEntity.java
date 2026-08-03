package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.WatcherAbilityManager;
import net.sweenus.simplyswords.world.WatcherWeaponType;

public class WatcherBatEntity extends BatEntity {
    public static final int MODE_MARK = 0;
    public static final int MODE_HUNT = 1;
    public static final int MODE_OMEN = 2;
    public static final int MODE_SWOOP = 3;
    public static final int MODE_RETURN = 4;

    private static final TrackedData<Integer> WATCHER_OWNER_ID =
            DataTracker.registerData(WatcherBatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> WATCHER_TARGET_ID =
            DataTracker.registerData(WatcherBatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> WATCHER_WEAPON_TYPE =
            DataTracker.registerData(WatcherBatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> WATCHER_MODE =
            DataTracker.registerData(WatcherBatEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public WatcherBatEntity(EntityType<? extends BatEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setPersistent();
        this.setRoosting(false);
    }

    public WatcherBatEntity(World world, double x, double y, double z) {
        this(EntityRegistry.WATCHER_BAT.get(), world);
        this.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
    }

    public static DefaultAttributeContainer.Builder createWatcherBatAttributes() {
        return BatEntity.createBatAttributes();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(WATCHER_OWNER_ID, -1);
        this.dataTracker.startTracking(WATCHER_TARGET_ID, -1);
        this.dataTracker.startTracking(WATCHER_WEAPON_TYPE, WatcherWeaponType.WARGLAIVE.ordinal());
        this.dataTracker.startTracking(WATCHER_MODE, MODE_MARK);
    }

    public void configureWatcher(LivingEntity owner, LivingEntity target, WatcherWeaponType weaponType, int mode) {
        this.dataTracker.set(WATCHER_OWNER_ID, owner == null ? -1 : owner.getId());
        this.dataTracker.set(WATCHER_TARGET_ID, target == null ? -1 : target.getId());
        this.dataTracker.set(WATCHER_WEAPON_TYPE,
                weaponType == null ? WatcherWeaponType.WARGLAIVE.ordinal() : weaponType.ordinal());
        this.dataTracker.set(WATCHER_MODE, mode);
    }

    public int getWatcherOwnerId() {
        return this.dataTracker.get(WATCHER_OWNER_ID);
    }

    public int getWatcherTargetId() {
        return this.dataTracker.get(WATCHER_TARGET_ID);
    }

    public WatcherWeaponType getWatcherWeaponType() {
        int ordinal = this.dataTracker.get(WATCHER_WEAPON_TYPE);
        WatcherWeaponType[] values = WatcherWeaponType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WatcherWeaponType.WARGLAIVE;
    }

    public int getWatcherMode() {
        return this.dataTracker.get(WATCHER_MODE);
    }

    public void setWatcherMode(int mode) {
        this.dataTracker.set(WATCHER_MODE, mode);
    }

    @Override
    public void tick() {
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setRoosting(false);
        super.tick();
        this.setRoosting(false);

        if (!this.getWorld().isClient()
                && this.getWorld() instanceof ServerWorld world
                && this.age > 5
                && !WatcherAbilityManager.isManagedBat(world, this.getUuid())) {
            this.discard();
        }
    }

    @Override
    protected void mobTick() {
        this.setRoosting(false);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    protected void tickCramming() {
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean causedByPlayer) {
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setRoosting(false);
        this.dataTracker.set(WATCHER_OWNER_ID, nbt.getInt("watcher_owner_id"));
        this.dataTracker.set(WATCHER_TARGET_ID, nbt.getInt("watcher_target_id"));
        this.dataTracker.set(WATCHER_WEAPON_TYPE, nbt.getInt("watcher_weapon_type"));
        this.dataTracker.set(WATCHER_MODE, nbt.getInt("watcher_mode"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("watcher_owner_id", this.getWatcherOwnerId());
        nbt.putInt("watcher_target_id", this.getWatcherTargetId());
        nbt.putInt("watcher_weapon_type", this.dataTracker.get(WATCHER_WEAPON_TYPE));
        nbt.putInt("watcher_mode", this.getWatcherMode());
    }
}
