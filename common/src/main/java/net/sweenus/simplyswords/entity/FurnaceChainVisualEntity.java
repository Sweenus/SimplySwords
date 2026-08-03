package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.HearthflameAbilityManager;

public class FurnaceChainVisualEntity extends Entity {

    public static final int MODE_BRAND = 0;
    public static final int MODE_CHAIN = 1;
    public static final int MODE_CORE = 2;
    public static final int MODE_SNAP = 3;
    public static final int MODE_FINALE = 4;

    private static final TrackedData<Integer> MODE =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ID =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> HEAT =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> PULSE =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE_START_AGE =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TRANSITION_AGE =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> END_AGE =
            DataTracker.registerData(FurnaceChainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public FurnaceChainVisualEntity(EntityType<? extends FurnaceChainVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public FurnaceChainVisualEntity(World world, Entity owner, Entity target, int mode, int endAge) {
        this(EntityRegistry.FURNACE_CHAIN_VISUAL.get(), world);
        Entity anchor = owner == null ? target : owner;
        if (anchor != null) {
            this.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
        }
        this.setOwnerId(owner == null ? -1 : owner.getId());
        this.setTargetId(target == null ? -1 : target.getId());
        this.setMode(mode);
        this.setEndAge(endAge);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(MODE, MODE_BRAND);
        this.dataTracker.startTracking(OWNER_ID, -1);
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(HEAT, 0.0F);
        this.dataTracker.startTracking(PULSE, 0);
        this.dataTracker.startTracking(PULSE_START_AGE, -1000);
        this.dataTracker.startTracking(TRANSITION_AGE, 0);
        this.dataTracker.startTracking(END_AGE, 200);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }
        if (this.age >= this.getEndAge()) {
            this.discard();
            return;
        }
        if ((this.getMode() == MODE_BRAND || this.getMode() == MODE_CHAIN || this.getMode() == MODE_CORE)
                && this.age > 10
                && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld
                && !HearthflameAbilityManager.isManagedVisual(serverWorld, this.getUuid())) {
            this.discard();
        }
    }

    public int getMode() {
        return this.dataTracker.get(MODE);
    }

    public void setMode(int mode) {
        this.dataTracker.set(MODE, mode);
    }

    public int getOwnerId() {
        return this.dataTracker.get(OWNER_ID);
    }

    public void setOwnerId(int ownerId) {
        this.dataTracker.set(OWNER_ID, ownerId);
    }

    public int getTargetId() {
        return this.dataTracker.get(TARGET_ID);
    }

    public void setTargetId(int targetId) {
        this.dataTracker.set(TARGET_ID, targetId);
    }

    public float getHeat() {
        return this.dataTracker.get(HEAT);
    }

    public void setHeat(float heat) {
        this.dataTracker.set(HEAT, net.minecraft.util.math.MathHelper.clamp(heat, 0.0F, 1.0F));
    }

    public int getPulse() {
        return this.dataTracker.get(PULSE);
    }

    public void triggerPulse() {
        this.dataTracker.set(PULSE, this.getPulse() + 1);
        this.dataTracker.set(PULSE_START_AGE, this.age);
    }

    public int getPulseStartAge() {
        return this.dataTracker.get(PULSE_START_AGE);
    }

    public int getTransitionAge() {
        return this.dataTracker.get(TRANSITION_AGE);
    }

    public void setTransitionAge(int transitionAge) {
        this.dataTracker.set(TRANSITION_AGE, transitionAge);
    }

    public int getEndAge() {
        return this.dataTracker.get(END_AGE);
    }

    public void setEndAge(int endAge) {
        this.dataTracker.set(END_AGE, Math.max(1, endAge));
    }

    public void beginTransition(int mode, int lifetime) {
        this.setMode(mode);
        this.setTransitionAge(this.age);
        this.setEndAge(this.age + Math.max(1, lifetime));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setMode(nbt.getInt("mode"));
        this.setOwnerId(nbt.getInt("owner_id"));
        this.setTargetId(nbt.getInt("target_id"));
        this.setHeat(nbt.getFloat("heat"));
        this.dataTracker.set(PULSE, nbt.getInt("pulse"));
        this.dataTracker.set(PULSE_START_AGE, nbt.contains("pulse_start_age")
                ? nbt.getInt("pulse_start_age")
                : -1000);
        this.setTransitionAge(nbt.getInt("transition_age"));
        this.setEndAge(nbt.getInt("end_age"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("mode", this.getMode());
        nbt.putInt("owner_id", this.getOwnerId());
        nbt.putInt("target_id", this.getTargetId());
        nbt.putFloat("heat", this.getHeat());
        nbt.putInt("pulse", this.getPulse());
        nbt.putInt("pulse_start_age", this.getPulseStartAge());
        nbt.putInt("transition_age", this.getTransitionAge());
        nbt.putInt("end_age", this.getEndAge());
    }
}
