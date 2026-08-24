package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class TwistedBladeCrescendoVisualEntity extends Entity {

    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(TwistedBladeCrescendoVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> GROUND_OFFSET =
            DataTracker.registerData(TwistedBladeCrescendoVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME =
            DataTracker.registerData(TwistedBladeCrescendoVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> EMPOWERED =
            DataTracker.registerData(TwistedBladeCrescendoVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> MIRRORED =
            DataTracker.registerData(TwistedBladeCrescendoVisualEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public TwistedBladeCrescendoVisualEntity(EntityType<? extends TwistedBladeCrescendoVisualEntity> type,
                                             World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public TwistedBladeCrescendoVisualEntity(World world, double x, double y, double z, float yaw,
                                             float scale, float groundOffset, boolean empowered,
                                             boolean mirrored, int lifetime) {
        this(EntityRegistry.TWISTED_BLADE_CRESCENDO_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setYaw(yaw);
        this.setScale(scale);
        this.setGroundOffset(groundOffset);
        this.setEmpowered(empowered);
        this.setMirrored(mirrored);
        this.setLifetime(lifetime);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SCALE, 1.0F);
        builder.add(GROUND_OFFSET, 0.9F);
        builder.add(LIFETIME, 8);
        builder.add(EMPOWERED, false);
        builder.add(MIRRORED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age > this.getLifetime()) {
            this.discard();
        }
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, Math.max(0.1F, scale));
    }

    public float getGroundOffset() {
        return this.dataTracker.get(GROUND_OFFSET);
    }

    public void setGroundOffset(float groundOffset) {
        this.dataTracker.set(GROUND_OFFSET, Math.max(0.0F, groundOffset));
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, Math.max(1, lifetime));
    }

    public boolean isEmpowered() {
        return this.dataTracker.get(EMPOWERED);
    }

    public void setEmpowered(boolean empowered) {
        this.dataTracker.set(EMPOWERED, empowered);
    }

    public boolean isMirrored() {
        return this.dataTracker.get(MIRRORED);
    }

    public void setMirrored(boolean mirrored) {
        this.dataTracker.set(MIRRORED, mirrored);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setScale(nbt.getFloat("scale"));
        this.setGroundOffset(nbt.getFloat("ground_offset"));
        this.setLifetime(nbt.getInt("lifetime"));
        this.setEmpowered(nbt.getBoolean("empowered"));
        this.setMirrored(nbt.getBoolean("mirrored"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("scale", this.getScale());
        nbt.putFloat("ground_offset", this.getGroundOffset());
        nbt.putInt("lifetime", this.getLifetime());
        nbt.putBoolean("empowered", this.isEmpowered());
        nbt.putBoolean("mirrored", this.isMirrored());
    }
}
