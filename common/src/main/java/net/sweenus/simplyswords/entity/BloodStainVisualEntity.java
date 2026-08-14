package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class BloodStainVisualEntity extends Entity {
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_TRAIL = 1;
    public static final int STYLE_BLOOD = 0;
    public static final int STYLE_DEVOURER = 1;

    private static final TrackedData<Integer> SHAPE =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HALF_LENGTH =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> VERTICAL_RANGE =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FADE_DURATION =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STYLE =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SOURCE_ENTITY_ID =
            DataTracker.registerData(BloodStainVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BloodStainVisualEntity(EntityType<? extends BloodStainVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public BloodStainVisualEntity(World world, double x, double y, double z,
                                  int shape, float radius, float halfLength,
                                  float yaw, float verticalRange,
                                  int lifetime, int fadeDuration, int seed) {
        this(world, x, y, z, shape, radius, halfLength, yaw, verticalRange,
                lifetime, fadeDuration, seed, STYLE_BLOOD);
    }

    public BloodStainVisualEntity(World world, double x, double y, double z,
                                  int shape, float radius, float halfLength,
                                  float yaw, float verticalRange,
                                  int lifetime, int fadeDuration, int seed, int style) {
        this(EntityRegistry.BLOOD_STAIN_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setShape(shape);
        this.setRadius(radius);
        this.setHalfLength(halfLength);
        this.setYaw(yaw);
        this.setVerticalRange(verticalRange);
        this.setLifetime(lifetime);
        this.setFadeDuration(fadeDuration);
        this.setSeed(seed);
        this.setStyle(style);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(SHAPE, SHAPE_CIRCLE);
        this.dataTracker.startTracking(RADIUS, 2.0F);
        this.dataTracker.startTracking(HALF_LENGTH, 0.0F);
        this.dataTracker.startTracking(VERTICAL_RANGE, 6.0F);
        this.dataTracker.startTracking(LIFETIME, 600);
        this.dataTracker.startTracking(FADE_DURATION, 100);
        this.dataTracker.startTracking(SEED, 0);
        this.dataTracker.startTracking(STYLE, STYLE_BLOOD);
        this.dataTracker.startTracking(SOURCE_ENTITY_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (!this.getWorld().isClient() && this.age >= this.getLifetime()) {
            this.discard();
        }
    }

    public int getShape() {
        return this.dataTracker.get(SHAPE);
    }

    public void setShape(int shape) {
        this.dataTracker.set(SHAPE, MathHelper.clamp(shape, SHAPE_CIRCLE, SHAPE_TRAIL));
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.1F, radius));
    }

    public float getHalfLength() {
        return this.dataTracker.get(HALF_LENGTH);
    }

    public void setHalfLength(float halfLength) {
        this.dataTracker.set(HALF_LENGTH, Math.max(0.0F, halfLength));
    }

    public float getVerticalRange() {
        return this.dataTracker.get(VERTICAL_RANGE);
    }

    public void setVerticalRange(float verticalRange) {
        this.dataTracker.set(VERTICAL_RANGE, Math.max(2.0F, verticalRange));
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public int getFadeDuration() {
        return this.dataTracker.get(FADE_DURATION);
    }

    public void setFadeDuration(int fadeDuration) {
        this.dataTracker.set(FADE_DURATION, Math.max(1, fadeDuration));
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
    }

    public void setSeed(int seed) {
        this.dataTracker.set(SEED, seed);
    }

    public int getStyle() {
        return this.dataTracker.get(STYLE);
    }

    public void setStyle(int style) {
        this.dataTracker.set(STYLE, MathHelper.clamp(style, STYLE_BLOOD, STYLE_DEVOURER));
    }

    public int getSourceEntityId() {
        return this.dataTracker.get(SOURCE_ENTITY_ID);
    }

    public void setSourceEntityId(int sourceEntityId) {
        this.dataTracker.set(SOURCE_ENTITY_ID, sourceEntityId);
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
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setShape(nbt.getInt("shape"));
        this.setRadius(nbt.getFloat("radius"));
        this.setHalfLength(nbt.getFloat("half_length"));
        this.setYaw(nbt.getFloat("yaw"));
        this.setVerticalRange(nbt.getFloat("vertical_range"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setFadeDuration(nbt.getInt("fade_duration"));
        this.setSeed(nbt.getInt("seed"));
        this.setStyle(nbt.getInt("style"));
        this.setSourceEntityId(nbt.contains("source_entity_id")
                ? nbt.getInt("source_entity_id") : -1);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("shape", this.getShape());
        nbt.putFloat("radius", this.getRadius());
        nbt.putFloat("half_length", this.getHalfLength());
        nbt.putFloat("yaw", this.getYaw());
        nbt.putFloat("vertical_range", this.getVerticalRange());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("fade_duration", this.getFadeDuration());
        nbt.putInt("seed", this.getSeed());
        nbt.putInt("style", this.getStyle());
        nbt.putInt("source_entity_id", this.getSourceEntityId());
    }
}
