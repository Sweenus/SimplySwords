package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class DevourerMassVisualEntity extends Entity {
    private static final TrackedData<Float> START_X = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_Y = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_Z = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> TRAVEL_TICKS = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> BLOOM_TICKS = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ACTIVE_TICKS = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLLAPSE_TICKS = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> START_RADIUS = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> MAX_RADIUS = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> FEED_COUNT = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LAST_FEED_AGE = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(DevourerMassVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public DevourerMassVisualEntity(EntityType<? extends DevourerMassVisualEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public DevourerMassVisualEntity(World world, Vec3d start, Vec3d center,
                                    int travelTicks, int bloomTicks, int activeTicks, int collapseTicks,
                                    float startRadius, float maxRadius) {
        this(EntityRegistry.DEVOURER_MASS_VISUAL.get(), world);
        setPosition(center);
        dataTracker.set(START_X, (float) start.x);
        dataTracker.set(START_Y, (float) start.y);
        dataTracker.set(START_Z, (float) start.z);
        dataTracker.set(TRAVEL_TICKS, Math.max(1, travelTicks));
        dataTracker.set(BLOOM_TICKS, Math.max(1, bloomTicks));
        dataTracker.set(ACTIVE_TICKS, Math.max(1, activeTicks));
        dataTracker.set(COLLAPSE_TICKS, Math.max(1, collapseTicks));
        dataTracker.set(START_RADIUS, Math.max(0.2F, startRadius));
        dataTracker.set(MAX_RADIUS, Math.max(startRadius, maxRadius));
        dataTracker.set(SEED, world.random.nextInt());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(START_X, 0.0F);
        this.dataTracker.startTracking(START_Y, 0.0F);
        this.dataTracker.startTracking(START_Z, 0.0F);
        this.dataTracker.startTracking(TRAVEL_TICKS, 8);
        this.dataTracker.startTracking(BLOOM_TICKS, 10);
        this.dataTracker.startTracking(ACTIVE_TICKS, 100);
        this.dataTracker.startTracking(COLLAPSE_TICKS, 8);
        this.dataTracker.startTracking(START_RADIUS, 0.7F);
        this.dataTracker.startTracking(MAX_RADIUS, 1.9F);
        this.dataTracker.startTracking(FEED_COUNT, 0);
        this.dataTracker.startTracking(LAST_FEED_AGE, -1000);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient() && age >= getTotalLifetime()) {
            discard();
        }
    }

    public Vec3d getStartPosition() {
        return new Vec3d(dataTracker.get(START_X), dataTracker.get(START_Y), dataTracker.get(START_Z));
    }

    public Vec3d getRenderPosition(float tickDelta) {
        float progress = ease(MathHelper.clamp((age + tickDelta) / getTravelTicks(), 0.0F, 1.0F));
        return getStartPosition().lerp(getPos(), progress);
    }

    public float getCurrentRadius(float tickDelta) {
        return calculateRadius(tickDelta, true);
    }

    public float getStableRadius(float tickDelta) {
        return calculateRadius(tickDelta, false);
    }

    private float calculateRadius(float tickDelta, boolean includeFeedPulse) {
        float visualAge = age + tickDelta;
        if (visualAge < getTravelTicks()) {
            return 0.13F;
        }
        float bloom = ease(MathHelper.clamp((visualAge - getTravelTicks()) / getBloomTicks(), 0.0F, 1.0F));
        float startRadius = getStartingRadius();
        float maximumRadius = getMaximumRadius();
        float activeAge = Math.max(0.0F, visualAge - getTravelTicks() - getBloomTicks());
        float timeGrowth = (maximumRadius - startRadius) * 0.42F
                * MathHelper.clamp(activeAge / Math.max(1.0F, getActiveTicks()), 0.0F, 1.0F);
        float feedGrowth = Math.min((maximumRadius - startRadius) * 0.58F, getFeedCount() * 0.075F);
        float radius = MathHelper.lerp(bloom, 0.18F, startRadius + timeGrowth + feedGrowth);
        float pulseAge = visualAge - getLastFeedAge();
        if (includeFeedPulse && pulseAge >= 0.0F && pulseAge <= 8.0F) {
            radius += MathHelper.sin(pulseAge / 8.0F * MathHelper.PI) * 0.11F;
        }
        float radiusLimit = maximumRadius + (includeFeedPulse ? 0.11F : 0.0F);
        return Math.min(radiusLimit, radius) * (1.0F - ease(getCollapseProgress(tickDelta)));
    }

    public float getBloomProgress(float tickDelta) {
        return ease(MathHelper.clamp((age + tickDelta - getTravelTicks()) / getBloomTicks(), 0.0F, 1.0F));
    }

    public float getCollapseProgress(float tickDelta) {
        float collapseStart = getTravelTicks() + getBloomTicks() + getActiveTicks();
        return MathHelper.clamp((age + tickDelta - collapseStart) / getCollapseTicks(), 0.0F, 1.0F);
    }

    public void feed() {
        dataTracker.set(FEED_COUNT, Math.min(24, getFeedCount() + 1));
        dataTracker.set(LAST_FEED_AGE, age);
    }

    public int getTravelTicks() { return dataTracker.get(TRAVEL_TICKS); }
    public int getBloomTicks() { return dataTracker.get(BLOOM_TICKS); }
    public int getActiveTicks() { return dataTracker.get(ACTIVE_TICKS); }
    public int getCollapseTicks() { return dataTracker.get(COLLAPSE_TICKS); }
    public float getStartingRadius() { return dataTracker.get(START_RADIUS); }
    public float getMaximumRadius() { return dataTracker.get(MAX_RADIUS); }
    public int getFeedCount() { return dataTracker.get(FEED_COUNT); }
    public int getLastFeedAge() { return dataTracker.get(LAST_FEED_AGE); }
    public int getSeed() { return dataTracker.get(SEED); }

    public int getTotalLifetime() {
        return getTravelTicks() + getBloomTicks() + getActiveTicks() + getCollapseTicks();
    }

    @Override
    public Box getVisibilityBoundingBox() {
        double trailReach = Math.ceil(Math.max(1,
                Config.uniqueEffects.devourer.stainSpreadDuration) / 5.0) * 3.0
                + Math.max(0.5, Config.uniqueEffects.devourer.stainTrailWidth);
        double extent = Math.max(10.0, getMaximumRadius() * 2.7 + trailReach);
        return getBoundingBox().expand(extent);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(START_X, nbt.getFloat("start_x"));
        dataTracker.set(START_Y, nbt.getFloat("start_y"));
        dataTracker.set(START_Z, nbt.getFloat("start_z"));
        dataTracker.set(TRAVEL_TICKS, nbt.getInt("travel_ticks"));
        dataTracker.set(BLOOM_TICKS, nbt.getInt("bloom_ticks"));
        dataTracker.set(ACTIVE_TICKS, nbt.getInt("active_ticks"));
        dataTracker.set(COLLAPSE_TICKS, nbt.getInt("collapse_ticks"));
        dataTracker.set(START_RADIUS, nbt.getFloat("start_radius"));
        dataTracker.set(MAX_RADIUS, nbt.getFloat("max_radius"));
        dataTracker.set(FEED_COUNT, nbt.getInt("feed_count"));
        dataTracker.set(LAST_FEED_AGE, nbt.getInt("last_feed_age"));
        dataTracker.set(SEED, nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("start_x", dataTracker.get(START_X));
        nbt.putFloat("start_y", dataTracker.get(START_Y));
        nbt.putFloat("start_z", dataTracker.get(START_Z));
        nbt.putInt("travel_ticks", getTravelTicks());
        nbt.putInt("bloom_ticks", getBloomTicks());
        nbt.putInt("active_ticks", getActiveTicks());
        nbt.putInt("collapse_ticks", getCollapseTicks());
        nbt.putFloat("start_radius", getStartingRadius());
        nbt.putFloat("max_radius", getMaximumRadius());
        nbt.putInt("feed_count", getFeedCount());
        nbt.putInt("last_feed_age", getLastFeedAge());
        nbt.putInt("seed", getSeed());
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
