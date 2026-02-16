package net.sweenus.simplyswords.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class ThrownRunicEntity extends ThrownSwordEntity {

    // Base Constructor
    public ThrownRunicEntity(EntityType<? extends ThrownRunicEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public ThrownRunicEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }


    @Override
    public void tick() {
        returnToPlayer = true;
        super.tick();
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.RUNIC_LONGSWORD.get());
    }

    @Override
    protected byte getLoyalty() {
        World world = this.getEntityWorld();
        if (world instanceof ServerWorld serverWorld) {
            return 3;
        } else {
            return 0;
        }
    }

}
