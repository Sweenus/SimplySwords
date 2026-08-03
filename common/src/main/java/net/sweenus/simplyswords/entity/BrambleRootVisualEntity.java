package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class BrambleRootVisualEntity extends Entity {

    public static final int MODE_PRIMARY = 0;
    public static final int MODE_BRANCH = 1;
    public static final int MODE_CORE = 2;
    public static final int MODE_HUNT = 3;

    public static final int PHASE_GROW = 0;
    public static final int PHASE_BIND = 1;
    public static final int PHASE_LIFT = 2;
    public static final int PHASE_SLAM = 3;
    public static final int PHASE_FADE = 4;

    private static final TrackedData<Integer> MODE =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SOURCE_ID =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_START_AGE =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULSE_START_AGE =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> VISUAL_SEED =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> END_AGE =
            DataTracker.registerData(BrambleRootVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BrambleRootVisualEntity(EntityType<? extends BrambleRootVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BrambleRootVisualEntity(World world, Entity source, Entity target, int mode,
                                   Vec3d center, int lifetime) {
        this(EntityRegistry.BRAMBLE_ROOT_VISUAL.get(), world);
        this.setPosition(center.x, center.y + 0.035, center.z);
        this.setSourceId(source == null ? -1 : source.getId());
        this.setTargetId(target == null ? -1 : target.getId());
        this.setMode(mode);
        this.setVisualSeed(world.random.nextInt());
        this.setEndAge(Math.max(1, lifetime));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(MODE, MODE_BRANCH);
        this.dataTracker.startTracking(SOURCE_ID, -1);
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(PHASE, PHASE_GROW);
        this.dataTracker.startTracking(PHASE_START_AGE, 0);
        this.dataTracker.startTracking(PULSE, 0);
        this.dataTracker.startTracking(PULSE_START_AGE, -1000);
        this.dataTracker.startTracking(VISUAL_SEED, 0);
        this.dataTracker.startTracking(END_AGE, 120);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age >= this.getEndAge()) {
            this.discard();
        }
    }

    public int getMode() {
        return this.dataTracker.get(MODE);
    }

    public void setMode(int mode) {
        this.dataTracker.set(MODE, mode);
    }

    public int getSourceId() {
        return this.dataTracker.get(SOURCE_ID);
    }

    public void setSourceId(int id) {
        this.dataTracker.set(SOURCE_ID, id);
    }

    public int getTargetId() {
        return this.dataTracker.get(TARGET_ID);
    }

    public void setTargetId(int id) {
        this.dataTracker.set(TARGET_ID, id);
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public void setPhase(int phase) {
        if (phase != this.getPhase()) {
            this.dataTracker.set(PHASE, phase);
            this.dataTracker.set(PHASE_START_AGE, this.age);
        }
    }

    public int getPhaseStartAge() {
        return this.dataTracker.get(PHASE_START_AGE);
    }

    public int getPulse() {
        return this.dataTracker.get(PULSE);
    }

    public int getPulseStartAge() {
        return this.dataTracker.get(PULSE_START_AGE);
    }

    public void triggerPulse() {
        this.dataTracker.set(PULSE, this.getPulse() + 1);
        this.dataTracker.set(PULSE_START_AGE, this.age);
    }

    public int getVisualSeed() {
        return this.dataTracker.get(VISUAL_SEED);
    }

    public void setVisualSeed(int seed) {
        this.dataTracker.set(VISUAL_SEED, seed);
    }

    public int getEndAge() {
        return this.dataTracker.get(END_AGE);
    }

    public void setEndAge(int endAge) {
        this.dataTracker.set(END_AGE, Math.max(1, endAge));
    }

    public void beginFade(int duration) {
        this.setPhase(PHASE_FADE);
        this.setEndAge(this.age + Math.max(1, duration));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setMode(nbt.getInt("mode"));
        this.setSourceId(nbt.getInt("source_id"));
        this.setTargetId(nbt.getInt("target_id"));
        this.dataTracker.set(PHASE, nbt.getInt("phase"));
        this.dataTracker.set(PHASE_START_AGE, nbt.getInt("phase_start_age"));
        this.dataTracker.set(PULSE, nbt.getInt("pulse"));
        this.dataTracker.set(PULSE_START_AGE, nbt.getInt("pulse_start_age"));
        this.setVisualSeed(nbt.getInt("visual_seed"));
        this.setEndAge(nbt.getInt("end_age"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("mode", this.getMode());
        nbt.putInt("source_id", this.getSourceId());
        nbt.putInt("target_id", this.getTargetId());
        nbt.putInt("phase", this.getPhase());
        nbt.putInt("phase_start_age", this.getPhaseStartAge());
        nbt.putInt("pulse", this.getPulse());
        nbt.putInt("pulse_start_age", this.getPulseStartAge());
        nbt.putInt("visual_seed", this.getVisualSeed());
        nbt.putInt("end_age", this.getEndAge());
    }
}
