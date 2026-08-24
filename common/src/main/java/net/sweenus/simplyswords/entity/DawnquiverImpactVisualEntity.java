package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public final class DawnquiverImpactVisualEntity extends Entity {

    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(DawnquiverImpactVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(DawnquiverImpactVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(DawnquiverImpactVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public DawnquiverImpactVisualEntity(EntityType<? extends DawnquiverImpactVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public DawnquiverImpactVisualEntity(World world, double x, double y, double z,
                                        float scale, int seed) {
        this(EntityRegistry.DAWNQUIVER_IMPACT_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.dataTracker.set(SCALE, Math.max(0.2F, scale));
        this.dataTracker.set(SEED, seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LIFETIME, 11);
        builder.add(SCALE, 1.0F);
        builder.add(SEED, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age >= getLifetime()) {
            this.discard();
        }
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public int getSeed() {
        return this.dataTracker.get(SEED);
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
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
