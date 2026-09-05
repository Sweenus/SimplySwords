package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

import java.util.Optional;
import java.util.UUID;

public class BrimstoneWakeVisualEntity extends Entity {
    private static final TrackedData<Optional<UUID>> GROUP_ID =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Float> RADIUS =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> VERTICAL_RANGE =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FADE_IN_DURATION =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FADE_OUT_DURATION =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(BrimstoneWakeVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BrimstoneWakeVisualEntity(EntityType<? extends BrimstoneWakeVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public BrimstoneWakeVisualEntity(World world, UUID groupId,
                                     double x, double y, double z,
                                     float radius, float verticalRange,
                                     int lifetime, int seed) {
        this(EntityRegistry.BRIMSTONE_WAKE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setGroupId(groupId);
        this.setRadius(radius);
        this.setVerticalRange(verticalRange);
        this.setLifetime(lifetime);
        this.setSeed(seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(GROUP_ID, Optional.empty());
        builder.add(RADIUS, 2.5F);
        builder.add(VERTICAL_RANGE, 6.0F);
        builder.add(LIFETIME, 40);
        builder.add(FADE_IN_DURATION, 3);
        builder.add(FADE_OUT_DURATION, 6);
        builder.add(SEED, 0);
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

    public UUID getGroupId() {
        return this.dataTracker.get(GROUP_ID).orElse(null);
    }

    public void setGroupId(UUID groupId) {
        this.dataTracker.set(GROUP_ID, Optional.ofNullable(groupId));
    }

    public float getRadius() {
        return this.dataTracker.get(RADIUS);
    }

    public void setRadius(float radius) {
        this.dataTracker.set(RADIUS, Math.max(0.1F, radius));
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

    public int getFadeInDuration() {
        return this.dataTracker.get(FADE_IN_DURATION);
    }

    public void setFadeInDuration(int fadeInDuration) {
        this.dataTracker.set(FADE_IN_DURATION, Math.max(0, fadeInDuration));
    }

    public int getFadeOutDuration() {
        return this.dataTracker.get(FADE_OUT_DURATION);
    }

    public void setFadeOutDuration(int fadeOutDuration) {
        this.dataTracker.set(FADE_OUT_DURATION, Math.max(1, fadeOutDuration));
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
        this.setGroupId(nbt.containsUuid("group_id") ? nbt.getUuid("group_id") : null);
        this.setRadius(nbt.getFloat("radius"));
        this.setVerticalRange(nbt.getFloat("vertical_range"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setFadeInDuration(nbt.getInt("fade_in_duration"));
        this.setFadeOutDuration(nbt.getInt("fade_out_duration"));
        this.setSeed(nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        UUID groupId = this.getGroupId();
        if (groupId != null) {
            nbt.putUuid("group_id", groupId);
        }
        nbt.putFloat("radius", this.getRadius());
        nbt.putFloat("vertical_range", this.getVerticalRange());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("fade_in_duration", this.getFadeInDuration());
        nbt.putInt("fade_out_duration", this.getFadeOutDuration());
        nbt.putInt("seed", this.getSeed());
    }
}
