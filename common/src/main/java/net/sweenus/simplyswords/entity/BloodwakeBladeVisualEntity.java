package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.EntityRegistry;

public class BloodwakeBladeVisualEntity extends Entity {
    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(
            BloodwakeBladeVisualEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Float> PLUNGE_PROGRESS = DataTracker.registerData(
            BloodwakeBladeVisualEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public BloodwakeBladeVisualEntity(EntityType<? extends BloodwakeBladeVisualEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public BloodwakeBladeVisualEntity(World world, double x, double y, double z, ItemStack stack) {
        this(EntityRegistry.BLOODWAKE_BLADE_VISUAL.get(), world);
        this.setPosition(x, y, z);
        this.setItemStack(stack);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(ITEM_STACK, ItemStack.EMPTY);
        builder.add(PLUNGE_PROGRESS, 0.0F);
    }

    public ItemStack getItemStack() {
        return this.dataTracker.get(ITEM_STACK);
    }

    public void setItemStack(ItemStack stack) {
        this.dataTracker.set(ITEM_STACK, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    public float getPlungeProgress() {
        return this.dataTracker.get(PLUNGE_PROGRESS);
    }

    public void setPlungeProgress(float progress) {
        this.dataTracker.set(PLUNGE_PROGRESS, Math.clamp(progress, 0.0F, 1.0F));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setPlungeProgress(nbt.getFloat("plunge_progress"));
        if (nbt.contains("item")) {
            this.setItemStack(ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("item")).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("plunge_progress", this.getPlungeProgress());
        if (!this.getItemStack().isEmpty()) {
            nbt.put("item", this.getItemStack().encode(this.getRegistryManager()));
        }
    }
}
