package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class ImplicitStatusVisualEntity extends Entity {

    private static final TrackedData<Integer> BLEED_STACKS = DataTracker.registerData(ImplicitStatusVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SUNDER_AMOUNT = DataTracker.registerData(ImplicitStatusVisualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public ImplicitStatusVisualEntity(EntityType<? extends ImplicitStatusVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public ImplicitStatusVisualEntity(World world, double x, double y, double z) {
        this(EntityRegistry.IMPLICIT_STATUS_VISUAL.get(), world);
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(BLEED_STACKS, 0);
        this.dataTracker.startTracking(SUNDER_AMOUNT, 0);
    }

    public void setBleedStacks(int stacks) {
        this.dataTracker.set(BLEED_STACKS, Math.max(0, stacks));
    }

    public int getBleedStacks() {
        return this.dataTracker.get(BLEED_STACKS);
    }

    public void setSunderAmount(int amount) {
        this.dataTracker.set(SUNDER_AMOUNT, Math.max(0, amount));
    }

    public int getSunderAmount() {
        return this.dataTracker.get(SUNDER_AMOUNT);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setBleedStacks(nbt.getInt("bleed_stacks"));
        this.setSunderAmount(nbt.getInt("sunder_amount"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("bleed_stacks", this.getBleedStacks());
        nbt.putInt("sunder_amount", this.getSunderAmount());
    }
}
