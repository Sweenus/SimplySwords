package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.render.LightningPhenomenonShape;
import net.sweenus.simplyswords.api.render.LightningPhenomenonStyle;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class LightningPhenomenonVisualEntity extends Entity {

    private static final TrackedData<Integer> START_ID = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> END_ID = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> START_OFFSET = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_OFFSET = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_X = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_Y = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_Z = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_X = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Y = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Z = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SHAPE = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PRIMARY = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CORE = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DELAY = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LEADER = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> BRANCHES = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SPREAD = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(LightningPhenomenonVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public LightningPhenomenonVisualEntity(EntityType<? extends LightningPhenomenonVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public LightningPhenomenonVisualEntity(World world, Vec3d start, Vec3d end,
                                           LightningPhenomenonStyle style, int seed) {
        this(EntityRegistry.LIGHTNING_PHENOMENON_VISUAL.get(), world);
        setEndpoints(start, end);
        this.setPosition(start);
        apply(style, seed);
    }

    public void anchor(Entity start, float startOffset, Entity end, float endOffset) {
        this.dataTracker.set(START_ID, start == null ? -1 : start.getId());
        this.dataTracker.set(END_ID, end == null ? -1 : end.getId());
        this.dataTracker.set(START_OFFSET, startOffset);
        this.dataTracker.set(END_OFFSET, endOffset);
        updateAnchors();
    }

    public void anchorEnd(Entity end, float endOffset) {
        this.dataTracker.set(END_ID, end == null ? -1 : end.getId());
        this.dataTracker.set(END_OFFSET, endOffset);
        updateAnchors();
    }

    private void apply(LightningPhenomenonStyle style, int seed) {
        this.dataTracker.set(SHAPE, style.shape().ordinal());
        this.dataTracker.set(PRIMARY, style.primaryColor());
        this.dataTracker.set(CORE, style.coreColor());
        this.dataTracker.set(LIFETIME, style.lifetimeTicks());
        this.dataTracker.set(DELAY, style.delayTicks());
        this.dataTracker.set(LEADER, style.leaderTicks());
        this.dataTracker.set(WIDTH, style.coreWidth());
        this.dataTracker.set(BRANCHES, style.branches());
        this.dataTracker.set(SPREAD, style.spread());
        this.dataTracker.set(SEED, seed);
    }

    private void setEndpoints(Vec3d start, Vec3d end) {
        this.dataTracker.set(START_X, (float) start.x);
        this.dataTracker.set(START_Y, (float) start.y);
        this.dataTracker.set(START_Z, (float) start.z);
        this.dataTracker.set(END_X, (float) end.x);
        this.dataTracker.set(END_Y, (float) end.y);
        this.dataTracker.set(END_Z, (float) end.z);
    }

    private boolean updateAnchors() {
        Entity start = getStartId() < 0 ? null : this.getWorld().getEntityById(getStartId());
        Entity end = getEndId() < 0 ? null : this.getWorld().getEntityById(getEndId());
        if (getStartId() >= 0 && (start == null || !start.isAlive())) return false;
        if (getEndId() >= 0 && (end == null || !end.isAlive())) return false;
        Vec3d startPos = start == null ? getStart() : start.getPos().add(0.0, getStartOffset(), 0.0);
        Vec3d endPos = end == null ? getEnd() : end.getPos().add(0.0, getEndOffset(), 0.0);
        setEndpoints(startPos, endPos);
        this.setPosition(startPos);
        return true;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(START_ID, -1); this.dataTracker.startTracking(END_ID, -1);
        this.dataTracker.startTracking(START_OFFSET, 0.0F); this.dataTracker.startTracking(END_OFFSET, 0.0F);
        this.dataTracker.startTracking(START_X, 0.0F); this.dataTracker.startTracking(START_Y, 0.0F); this.dataTracker.startTracking(START_Z, 0.0F);
        this.dataTracker.startTracking(END_X, 0.0F); this.dataTracker.startTracking(END_Y, 0.0F); this.dataTracker.startTracking(END_Z, 0.0F);
        this.dataTracker.startTracking(SHAPE, LightningPhenomenonShape.DIRECT_FLASH.ordinal());
        this.dataTracker.startTracking(PRIMARY, 0x2F8CFF); this.dataTracker.startTracking(CORE, 0xF7FDFF);
        this.dataTracker.startTracking(LIFETIME, 10); this.dataTracker.startTracking(DELAY, 0); this.dataTracker.startTracking(LEADER, 0);
        this.dataTracker.startTracking(WIDTH, 0.025F); this.dataTracker.startTracking(BRANCHES, 3); this.dataTracker.startTracking(SPREAD, 0.2F);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            if (!updateAnchors() || this.age > getLifetime() + getDelay()) this.discard();
        }
    }

    public int getStartId() { return this.dataTracker.get(START_ID); }
    public int getEndId() { return this.dataTracker.get(END_ID); }
    public float getStartOffset() { return this.dataTracker.get(START_OFFSET); }
    public float getEndOffset() { return this.dataTracker.get(END_OFFSET); }
    public Vec3d getStart() { return new Vec3d(this.dataTracker.get(START_X), this.dataTracker.get(START_Y), this.dataTracker.get(START_Z)); }
    public Vec3d getEnd() { return new Vec3d(this.dataTracker.get(END_X), this.dataTracker.get(END_Y), this.dataTracker.get(END_Z)); }
    public LightningPhenomenonShape getShape() { return LightningPhenomenonShape.values()[MathHelper.clamp(this.dataTracker.get(SHAPE), 0, LightningPhenomenonShape.values().length - 1)]; }
    public int getPrimaryColor() { return this.dataTracker.get(PRIMARY); }
    public int getCoreColor() { return this.dataTracker.get(CORE); }
    public int getLifetime() { return this.dataTracker.get(LIFETIME); }
    public int getDelay() { return this.dataTracker.get(DELAY); }
    public int getLeaderTicks() { return this.dataTracker.get(LEADER); }
    public float getCoreWidth() { return this.dataTracker.get(WIDTH); }
    public int getBranches() { return this.dataTracker.get(BRANCHES); }
    public float getSpread() { return this.dataTracker.get(SPREAD); }
    public int getSeed() { return this.dataTracker.get(SEED); }

    @Override public boolean isAttackable() { return false; }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(START_ID, nbt.getInt("start_id")); this.dataTracker.set(END_ID, nbt.getInt("end_id"));
        this.dataTracker.set(START_OFFSET, nbt.getFloat("start_offset")); this.dataTracker.set(END_OFFSET, nbt.getFloat("end_offset"));
        setEndpoints(new Vec3d(nbt.getDouble("start_x"), nbt.getDouble("start_y"), nbt.getDouble("start_z")),
                new Vec3d(nbt.getDouble("end_x"), nbt.getDouble("end_y"), nbt.getDouble("end_z")));
        this.dataTracker.set(SHAPE, nbt.getInt("shape")); this.dataTracker.set(PRIMARY, nbt.getInt("primary"));
        this.dataTracker.set(CORE, nbt.getInt("core")); this.dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        this.dataTracker.set(DELAY, nbt.getInt("delay")); this.dataTracker.set(LEADER, nbt.getInt("leader"));
        this.dataTracker.set(WIDTH, nbt.getFloat("width")); this.dataTracker.set(BRANCHES, nbt.getInt("branches"));
        this.dataTracker.set(SPREAD, nbt.getFloat("spread")); this.dataTracker.set(SEED, nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("start_id", getStartId()); nbt.putInt("end_id", getEndId());
        nbt.putFloat("start_offset", getStartOffset()); nbt.putFloat("end_offset", getEndOffset());
        nbt.putDouble("start_x", getStart().x); nbt.putDouble("start_y", getStart().y); nbt.putDouble("start_z", getStart().z);
        nbt.putDouble("end_x", getEnd().x); nbt.putDouble("end_y", getEnd().y); nbt.putDouble("end_z", getEnd().z);
        nbt.putInt("shape", this.dataTracker.get(SHAPE)); nbt.putInt("primary", getPrimaryColor()); nbt.putInt("core", getCoreColor());
        nbt.putInt("lifetime", getLifetime()); nbt.putInt("delay", getDelay()); nbt.putInt("leader", getLeaderTicks());
        nbt.putFloat("width", getCoreWidth()); nbt.putInt("branches", getBranches()); nbt.putFloat("spread", getSpread());
        nbt.putInt("seed", getSeed());
    }
}
