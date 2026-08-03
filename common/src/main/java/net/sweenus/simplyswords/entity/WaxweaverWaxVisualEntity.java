package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class WaxweaverWaxVisualEntity extends Entity {

    public static final int MODE_ENCASE = 0;
    public static final int MODE_DETONATION = 1;

    private static final TrackedData<Integer> MODE = DataTracker.registerData(WaxweaverWaxVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SOURCE_ID = DataTracker.registerData(WaxweaverWaxVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TARGET_ID = DataTracker.registerData(WaxweaverWaxVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(WaxweaverWaxVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(WaxweaverWaxVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED = DataTracker.registerData(WaxweaverWaxVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public WaxweaverWaxVisualEntity(EntityType<? extends WaxweaverWaxVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public WaxweaverWaxVisualEntity(World world, int mode, Entity source, Entity target,
                                    double x, double y, double z, float radius, int lifetime) {
        this(EntityRegistry.WAXWEAVER_WAX_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.dataTracker.set(MODE, mode);
        this.dataTracker.set(SOURCE_ID, source == null ? -1 : source.getId());
        this.dataTracker.set(TARGET_ID, target == null ? -1 : target.getId());
        this.dataTracker.set(RADIUS, radius);
        this.dataTracker.set(LIFETIME, lifetime);
        this.dataTracker.set(SEED, this.random.nextInt());
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(MODE, MODE_ENCASE);
        this.dataTracker.startTracking(SOURCE_ID, -1);
        this.dataTracker.startTracking(TARGET_ID, -1);
        this.dataTracker.startTracking(RADIUS, 1.0F);
        this.dataTracker.startTracking(LIFETIME, 10);
        this.dataTracker.startTracking(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age >= this.getLifetime()) this.discard();
    }

    public int getMode() { return this.dataTracker.get(MODE); }
    public int getSourceId() { return this.dataTracker.get(SOURCE_ID); }
    public int getTargetId() { return this.dataTracker.get(TARGET_ID); }
    public float getRadius() { return this.dataTracker.get(RADIUS); }
    public int getLifetime() { return this.dataTracker.get(LIFETIME); }
    public int getSeed() { return this.dataTracker.get(SEED); }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(MODE, nbt.getInt("mode"));
        this.dataTracker.set(SOURCE_ID, nbt.getInt("source_id"));
        this.dataTracker.set(TARGET_ID, nbt.getInt("target_id"));
        this.dataTracker.set(RADIUS, nbt.getFloat("radius"));
        this.dataTracker.set(LIFETIME, nbt.getInt("lifetime"));
        this.dataTracker.set(SEED, nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("mode", this.getMode());
        nbt.putInt("source_id", this.getSourceId());
        nbt.putInt("target_id", this.getTargetId());
        nbt.putFloat("radius", this.getRadius());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("seed", this.getSeed());
    }
}
