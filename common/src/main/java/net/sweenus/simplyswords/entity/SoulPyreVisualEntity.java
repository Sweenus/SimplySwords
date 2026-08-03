package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class SoulPyreVisualEntity extends Entity {

    public static final int PHASE_ACTIVE = 0;
    public static final int PHASE_COLLAPSING = 1;

    private static final TrackedData<Integer> OWNER_ENTITY_ID =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_RADIUS =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> VERTICAL_RANGE =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SOUL_COUNT =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE_COUNTER =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE_START_AGE =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(SoulPyreVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public SoulPyreVisualEntity(EntityType<? extends SoulPyreVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public SoulPyreVisualEntity(World world, Entity owner, float maxRadius,
                                float verticalRange, int lifetime, int seed) {
        this(EntityRegistry.SOUL_PYRE_VISUAL.get(), world);
        this.setOwnerEntityId(owner == null ? -1 : owner.getId());
        this.setMaxRadius(maxRadius);
        this.setVerticalRange(verticalRange);
        this.setLifetime(lifetime);
        this.setSeed(seed);
        if (owner != null) {
            this.setPosition(owner.getX(), owner.getY(), owner.getZ());
        }
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(OWNER_ENTITY_ID, -1);
        this.dataTracker.startTracking(RADIUS, 1.5F);
        this.dataTracker.startTracking(MAX_RADIUS, 12.0F);
        this.dataTracker.startTracking(VERTICAL_RANGE, 8.0F);
        this.dataTracker.startTracking(SOUL_COUNT, 0);
        this.dataTracker.startTracking(PULSE_COUNTER, 0);
        this.dataTracker.startTracking(PULSE_START_AGE, -1000);
        this.dataTracker.startTracking(PHASE, PHASE_ACTIVE);
        this.dataTracker.startTracking(SEED, 0);
        this.dataTracker.startTracking(LIFETIME, 180);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (!this.getWorld().isClient()) {
            if (this.age >= this.getLifetime()) {
                this.discard();
            }
        }
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public void setOwnerEntityId(int ownerEntityId) {
        this.dataTracker.set(OWNER_ENTITY_ID, ownerEntityId);
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.0F, radius));
    }

    public float getMaxRadius() {
        return this.dataTracker.get(MAX_RADIUS);
    }

    public void setMaxRadius(float maxRadius) {
        this.dataTracker.set(MAX_RADIUS, Math.max(1.0F, maxRadius));
    }

    public float getVerticalRange() {
        return this.dataTracker.get(VERTICAL_RANGE);
    }

    public void setVerticalRange(float verticalRange) {
        this.dataTracker.set(VERTICAL_RANGE, Math.max(2.0F, verticalRange));
    }

    public int getSoulCount() {
        return this.dataTracker.get(SOUL_COUNT);
    }

    public void setSoulCount(int soulCount) {
        this.dataTracker.set(SOUL_COUNT, Math.max(0, soulCount));
    }

    public int getPulseCounter() {
        return this.dataTracker.get(PULSE_COUNTER);
    }

    public int getPulseStartAge() {
        return this.dataTracker.get(PULSE_START_AGE);
    }

    public void triggerPulse() {
        this.dataTracker.set(PULSE_COUNTER, this.getPulseCounter() + 1);
        this.dataTracker.set(PULSE_START_AGE, this.age);
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public void setPhase(int phase) {
        this.dataTracker.set(PHASE,
                net.minecraft.util.math.MathHelper.clamp(phase, PHASE_ACTIVE, PHASE_COLLAPSING));
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public void beginCollapse(int remainingLifetime) {
        this.setPhase(PHASE_COLLAPSING);
        this.triggerPulse();
        this.setLifetime(this.age + Math.max(1, remainingLifetime));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setOwnerEntityId(nbt.getInt("owner_entity_id"));
        this.setRadius(nbt.getFloat("radius"));
        this.setMaxRadius(nbt.getFloat("max_radius"));
        this.setVerticalRange(nbt.getFloat("vertical_range"));
        this.setSoulCount(nbt.getInt("soul_count"));
        this.dataTracker.set(PULSE_COUNTER, nbt.getInt("pulse_counter"));
        this.dataTracker.set(PULSE_START_AGE,
                nbt.contains("pulse_start_age") ? nbt.getInt("pulse_start_age") : -1000);
        this.setPhase(nbt.getInt("phase"));
        this.setSeed(nbt.getInt("seed"));
        this.setLifetime(nbt.getInt("lifetime"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("owner_entity_id", this.getOwnerEntityId());
        nbt.putFloat("radius", this.getRadius());
        nbt.putFloat("max_radius", this.getMaxRadius());
        nbt.putFloat("vertical_range", this.getVerticalRange());
        nbt.putInt("soul_count", this.getSoulCount());
        nbt.putInt("pulse_counter", this.getPulseCounter());
        nbt.putInt("pulse_start_age", this.getPulseStartAge());
        nbt.putInt("phase", this.getPhase());
        nbt.putInt("seed", this.getSeed());
        nbt.putInt("lifetime", this.getLifetime());
    }
}
