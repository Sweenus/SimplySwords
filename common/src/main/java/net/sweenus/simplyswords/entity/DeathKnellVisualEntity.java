package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.world.DeathKnellAbilityManager;

public class DeathKnellVisualEntity extends Entity {

    public static final int MODE_FEVER = 0;
    public static final int MODE_TOLL = 1;

    private static final TrackedData<Integer> MODE =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STACKS =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> MAX_STACKS =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> IMPACT_AGE =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> END_AGE =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHAIN_DEPTH =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(DeathKnellVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public DeathKnellVisualEntity(EntityType<? extends DeathKnellVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static DeathKnellVisualEntity fever(World world, Entity target, int stacks, int maximumStacks) {
        DeathKnellVisualEntity visual =
                new DeathKnellVisualEntity(EntityRegistry.DEATH_KNELL_VISUAL.get(), world);
        visual.setMode(MODE_FEVER);
        visual.setTargetId(target.getId());
        visual.setStacks(stacks);
        visual.setMaxStacks(maximumStacks);
        visual.setSeed(target.getId() * 31 + target.age);
        visual.setPosition(target.getX(), target.getY(), target.getZ());
        return visual;
    }

    public static DeathKnellVisualEntity toll(World world, Entity target, float radius,
                                               int impactAge, int endAge, int chainDepth) {
        DeathKnellVisualEntity visual =
                new DeathKnellVisualEntity(EntityRegistry.DEATH_KNELL_VISUAL.get(), world);
        visual.setMode(MODE_TOLL);
        visual.setTargetId(target.getId());
        visual.setRadius(radius);
        visual.setImpactAge(impactAge);
        visual.setEndAge(endAge);
        visual.setChainDepth(chainDepth);
        visual.setSeed(target.getId() * 31 + target.age + chainDepth * 997);
        visual.setPosition(target.getX(), target.getY(), target.getZ());
        return visual;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(MODE, MODE_FEVER);
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(STACKS, 1);
        this.dataTracker.startTracking(MAX_STACKS, 5);
        this.dataTracker.startTracking(RADIUS, 5.0F);
        this.dataTracker.startTracking(IMPACT_AGE, 6);
        this.dataTracker.startTracking(END_AGE, 22);
        this.dataTracker.startTracking(CHAIN_DEPTH, 0);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = this.getWorld().getEntityById(this.getTargetId());
        if (target != null && target.isAlive()) {
            this.setPosition(target.getX(), target.getY(), target.getZ());
        }

        if (!this.getWorld().isClient()) {
            if (this.getMode() == MODE_TOLL && this.age >= this.getEndAge()) {
                this.discard();
            } else if (this.getMode() == MODE_FEVER
                    && this.age > 10
                    && this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld
                    && !DeathKnellAbilityManager.isManagedVisual(serverWorld, this.getUuid())) {
                this.discard();
            }
        }
    }

    public int getMode() {
        return this.dataTracker.get(MODE);
    }

    public void setMode(int mode) {
        this.dataTracker.set(MODE, mode);
    }

    public int getTargetId() {
        return this.dataTracker.get(TARGET_ID);
    }

    public void setTargetId(int targetId) {
        this.dataTracker.set(TARGET_ID, targetId);
    }

    public int getStacks() {
        return this.dataTracker.get(STACKS);
    }

    public void setStacks(int stacks) {
        this.dataTracker.set(STACKS, Math.max(0, stacks));
    }

    public int getMaxStacks() {
        return this.dataTracker.get(MAX_STACKS);
    }

    public void setMaxStacks(int maximumStacks) {
        this.dataTracker.set(MAX_STACKS, Math.max(1, maximumStacks));
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.1F, radius));
    }

    public int getImpactAge() {
        return this.dataTracker.get(IMPACT_AGE);
    }

    public void setImpactAge(int impactAge) {
        this.dataTracker.set(IMPACT_AGE, Math.max(1, impactAge));
    }

    public int getEndAge() {
        return this.dataTracker.get(END_AGE);
    }

    public void setEndAge(int endAge) {
        this.dataTracker.set(END_AGE, Math.max(2, endAge));
    }

    public int getChainDepth() {
        return this.dataTracker.get(CHAIN_DEPTH);
    }

    public void setChainDepth(int chainDepth) {
        this.dataTracker.set(CHAIN_DEPTH, Math.max(0, chainDepth));
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setMode(nbt.getInt("mode"));
        this.setTargetId(nbt.getInt("target_id"));
        this.setStacks(nbt.getInt("stacks"));
        this.setMaxStacks(nbt.getInt("max_stacks"));
        this.setRadius(nbt.getFloat("radius"));
        this.setImpactAge(nbt.getInt("impact_age"));
        this.setEndAge(nbt.getInt("end_age"));
        this.setChainDepth(nbt.getInt("chain_depth"));
        this.setSeed(nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("mode", this.getMode());
        nbt.putInt("target_id", this.getTargetId());
        nbt.putInt("stacks", this.getStacks());
        nbt.putInt("max_stacks", this.getMaxStacks());
        nbt.putFloat("radius", this.getRadius());
        nbt.putInt("impact_age", this.getImpactAge());
        nbt.putInt("end_age", this.getEndAge());
        nbt.putInt("chain_depth", this.getChainDepth());
        nbt.putInt("seed", this.getSeed());
    }
}
