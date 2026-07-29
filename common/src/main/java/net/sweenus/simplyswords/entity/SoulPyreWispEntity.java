package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class SoulPyreWispEntity extends Entity {

    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(SoulPyreWispEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SEED =
            DataTracker.registerData(SoulPyreWispEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public SoulPyreWispEntity(EntityType<? extends SoulPyreWispEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public SoulPyreWispEntity(World world, double x, double y, double z,
                              int lifetime, int seed) {
        this(EntityRegistry.SOUL_PYRE_WISP.get(), world);
        this.setPosition(x, y, z);
        this.setLifetime(lifetime);
        this.setSeed(seed);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LIFETIME, 30);
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

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
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
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setLifetime(nbt.getInt("lifetime"));
        this.setSeed(nbt.getInt("seed"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putInt("seed", this.getSeed());
    }
}
