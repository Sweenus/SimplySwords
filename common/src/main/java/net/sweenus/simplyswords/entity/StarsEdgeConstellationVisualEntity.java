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

public class StarsEdgeConstellationVisualEntity extends Entity {

    public static final int KIND_NODE = 0;
    public static final int KIND_LINK = 1;

    public static final int PHASE_RECORDING = 0;
    public static final int PHASE_ARMED = 2;
    public static final int PHASE_DETONATE = 3;
    public static final int PHASE_FADE = 4;

    private static final TrackedData<Integer> KIND = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PHASE_START_AGE = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> END_OFFSET_X = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Y = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET_Z = DataTracker.registerData(
            StarsEdgeConstellationVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public StarsEdgeConstellationVisualEntity(
            EntityType<? extends StarsEdgeConstellationVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static StarsEdgeConstellationVisualEntity node(World world, Vec3d position, int lifetime) {
        StarsEdgeConstellationVisualEntity visual = new StarsEdgeConstellationVisualEntity(
                EntityRegistry.STARS_EDGE_CONSTELLATION_VISUAL.get(), world);
        visual.setPosition(position);
        visual.setKind(KIND_NODE);
        visual.setLifetime(lifetime);
        return visual;
    }

    public static StarsEdgeConstellationVisualEntity link(
            World world, Vec3d start, Vec3d end, int lifetime) {
        StarsEdgeConstellationVisualEntity visual = new StarsEdgeConstellationVisualEntity(
                EntityRegistry.STARS_EDGE_CONSTELLATION_VISUAL.get(), world);
        visual.setPosition(start);
        visual.setKind(KIND_LINK);
        visual.setEndOffset(end.subtract(start));
        visual.setLifetime(lifetime);
        return visual;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(KIND, KIND_NODE);
        this.dataTracker.startTracking(PHASE, PHASE_RECORDING);
        this.dataTracker.startTracking(PHASE_START_AGE, 0);
        this.dataTracker.startTracking(LIFETIME, 280);
        this.dataTracker.startTracking(END_OFFSET_X, 0.0F);
        this.dataTracker.startTracking(END_OFFSET_Y, 0.0F);
        this.dataTracker.startTracking(END_OFFSET_Z, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age > this.getLifetime()) {
            this.discard();
        }
    }

    public int getKind() {
        return this.dataTracker.get(KIND);
    }

    public void setKind(int kind) {
        this.dataTracker.set(KIND, kind);
    }

    public int getPhase() {
        return this.dataTracker.get(PHASE);
    }

    public void setPhase(int phase) {
        if (this.getPhase() == phase) {
            return;
        }
        this.dataTracker.set(PHASE, phase);
        this.dataTracker.set(PHASE_START_AGE, this.age);
    }

    public int getPhaseStartAge() {
        return this.dataTracker.get(PHASE_START_AGE);
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }

    public Vec3d getEndOffset() {
        return new Vec3d(
                this.dataTracker.get(END_OFFSET_X),
                this.dataTracker.get(END_OFFSET_Y),
                this.dataTracker.get(END_OFFSET_Z));
    }

    public void setEndOffset(Vec3d offset) {
        this.dataTracker.set(END_OFFSET_X, (float) offset.x);
        this.dataTracker.set(END_OFFSET_Y, (float) offset.y);
        this.dataTracker.set(END_OFFSET_Z, (float) offset.z);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setKind(nbt.getInt("kind"));
        this.dataTracker.set(PHASE, nbt.getInt("phase"));
        this.dataTracker.set(PHASE_START_AGE, nbt.getInt("phase_start_age"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setEndOffset(new Vec3d(
                nbt.getFloat("end_offset_x"),
                nbt.getFloat("end_offset_y"),
                nbt.getFloat("end_offset_z")));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("kind", this.getKind());
        nbt.putInt("phase", this.getPhase());
        nbt.putInt("phase_start_age", this.getPhaseStartAge());
        nbt.putInt("lifetime", this.getLifetime());
        Vec3d offset = this.getEndOffset();
        nbt.putFloat("end_offset_x", (float) offset.x);
        nbt.putFloat("end_offset_y", (float) offset.y);
        nbt.putFloat("end_offset_z", (float) offset.z);
    }
}
