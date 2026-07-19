package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class IcewhisperCometVisualEntity extends Entity {

    private static final TrackedData<Float> SCALE = DataTracker.registerData(IcewhisperCometVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public IcewhisperCometVisualEntity(EntityType<? extends IcewhisperCometVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public IcewhisperCometVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.ICEWHISPER_COMET_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setScale(1.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient && this.age % 2 == 0) {
            this.getWorld().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.02, 0.0);
            this.getWorld().addParticle(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
            this.getWorld().addParticle(ParticleTypes.POOF, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
            this.getWorld().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("scale")) {
            this.setScale(nbt.getFloat("scale"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("scale", this.getScale());
    }
}
