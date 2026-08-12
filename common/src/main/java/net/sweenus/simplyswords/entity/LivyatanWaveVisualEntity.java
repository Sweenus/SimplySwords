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

import java.util.ArrayList;
import java.util.List;

public class LivyatanWaveVisualEntity extends Entity {

    private static final TrackedData<Float> TARGET_HEIGHT = DataTracker.registerData(LivyatanWaveVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEIGHT_SCALE = DataTracker.registerData(LivyatanWaveVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> BLOODWAKE_STYLE = DataTracker.registerData(LivyatanWaveVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<NbtCompound> BLOODWAKE_LANES = DataTracker.registerData(LivyatanWaveVisualEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final int MAX_TRACKED_LANES = 64;

    private NbtCompound cachedLaneData;
    private List<WaveLane> cachedLanes = List.of();

    public LivyatanWaveVisualEntity(EntityType<? extends LivyatanWaveVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public LivyatanWaveVisualEntity(World world, double x, double y, double z, float targetHeight) {
        this(EntityRegistry.LIVYATAN_WAVE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setTargetHeight(targetHeight);
        this.setHeightScale(0.0F);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(TARGET_HEIGHT, 1.0F);
        this.dataTracker.startTracking(HEIGHT_SCALE, 0.0F);
        this.dataTracker.startTracking(BLOODWAKE_STYLE, false);
        this.dataTracker.startTracking(BLOODWAKE_LANES, new NbtCompound());
    }

    public void setTargetHeight(float targetHeight) {
        this.dataTracker.set(TARGET_HEIGHT, targetHeight);
    }

    public float getTargetHeight() {
        return this.dataTracker.get(TARGET_HEIGHT);
    }

    public void setHeightScale(float heightScale) {
        this.dataTracker.set(HEIGHT_SCALE, heightScale);
    }

    public float getHeightScale() {
        return this.dataTracker.get(HEIGHT_SCALE);
    }

    public void setBloodwakeStyle(boolean bloodwakeStyle) {
        this.dataTracker.set(BLOODWAKE_STYLE, bloodwakeStyle);
    }

    public boolean isBloodwakeStyle() {
        return this.dataTracker.get(BLOODWAKE_STYLE);
    }

    public void setBloodwakeLanes(List<WaveLane> lanes) {
        NbtCompound data = new NbtCompound();
        int count = Math.min(MAX_TRACKED_LANES, lanes == null ? 0 : lanes.size());
        data.putInt("count", count);
        for (int i = 0; i < count; i++) {
            WaveLane lane = lanes.get(i);
            data.putFloat("x_" + i, lane.offsetX);
            data.putFloat("y_" + i, lane.offsetY);
            data.putFloat("z_" + i, lane.offsetZ);
            data.putFloat("height_" + i, lane.targetHeight);
        }
        this.dataTracker.set(BLOODWAKE_LANES, data);
        this.cachedLaneData = null;
    }

    public List<WaveLane> getBloodwakeLanes() {
        NbtCompound data = this.dataTracker.get(BLOODWAKE_LANES);
        if (data == this.cachedLaneData) {
            return this.cachedLanes;
        }
        int count = MathHelper.clamp(data.getInt("count"), 0, MAX_TRACKED_LANES);
        List<WaveLane> lanes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lanes.add(new WaveLane(
                    data.getFloat("x_" + i),
                    data.getFloat("y_" + i),
                    data.getFloat("z_" + i),
                    Math.max(0.08F, data.getFloat("height_" + i))));
        }
        this.cachedLaneData = data;
        this.cachedLanes = List.copyOf(lanes);
        return this.cachedLanes;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.isBloodwakeStyle()) {
            if (this.age >= 14) {
                this.discard();
                return;
            }
            float scale;
            if (this.age < 3) {
                float progress = this.age / 3.0F;
                scale = 1.0F - (1.0F - progress) * (1.0F - progress);
            } else if (this.age < 6) {
                scale = 1.0F;
            } else {
                float progress = (this.age - 6) / 8.0F;
                scale = 1.0F - progress * progress;
            }
            this.setHeightScale(Math.max(0.0F, scale));
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("target_height")) {
            this.setTargetHeight(nbt.getFloat("target_height"));
        }
        if (nbt.contains("height_scale")) {
            this.setHeightScale(nbt.getFloat("height_scale"));
        }
        this.setBloodwakeStyle(nbt.getBoolean("bloodwake_style"));
        if (nbt.contains("bloodwake_lanes")) {
            this.dataTracker.set(BLOODWAKE_LANES,
                    nbt.getCompound("bloodwake_lanes").copy());
            this.cachedLaneData = null;
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("target_height", this.getTargetHeight());
        nbt.putFloat("height_scale", this.getHeightScale());
        nbt.putBoolean("bloodwake_style", this.isBloodwakeStyle());
        NbtCompound lanes = this.dataTracker.get(BLOODWAKE_LANES);
        if (!lanes.isEmpty()) {
            nbt.put("bloodwake_lanes", lanes.copy());
        }
    }

    public record WaveLane(
            float offsetX, float offsetY, float offsetZ, float targetHeight) {
    }
}
