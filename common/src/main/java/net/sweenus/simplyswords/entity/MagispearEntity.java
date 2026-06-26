package net.sweenus.simplyswords.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public class MagispearEntity extends ThrownSpearEntity {
    public int slownessDuration;

    // Base Constructor
    public MagispearEntity(EntityType<? extends MagispearEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public MagispearEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        returnToPlayer = true;
        super.tick();
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        boolean canPickup = super.tryPickup(player);

        if (canPickup) {
            int cooldown = Config.uniqueEffects.magispear.cooldown;
            if (offhandThrow) cooldown = Config.uniqueEffects.magispear.cooldown + 4;
            player.getItemCooldownManager().set(this.asItemStack().getItem(), cooldown);
        }

        return canPickup;
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.MAGISPEAR.get());
    }

    @Override
    protected byte getLoyalty() {
        World world = this.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            return 3;
        } else {
            return 0;
        }
    }

}
