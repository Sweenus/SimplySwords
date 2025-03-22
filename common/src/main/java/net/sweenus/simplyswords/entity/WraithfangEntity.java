package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class WraithfangEntity extends ThrownSpearEntity {
    public int slownessDuration;

    // Base Constructor
    public WraithfangEntity(EntityType<? extends WraithfangEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public WraithfangEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        returnToPlayer = true;

        Entity owner = getOwner();
        if (owner != null && owner.distanceTo(this) > 1 && getWorld() instanceof ServerWorld serverWorld) {
            owner.setVelocity((this.getX() - owner.getX()) / 8, (this.getY() - owner.getY()) / 8, (this.getZ() - owner.getZ()) / 8);
            owner.velocityModified = true;
            HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.OMINOUS_SPAWNING, this, owner, (int) this.distanceTo(owner));
            if (owner instanceof LivingEntity livingEntity) {
                livingEntity.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RESILIENCE), 20, 4, false, false, true));
            }
        }

        super.tick();
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        if (this.isNoClip() && this.isOwner(player)) {
            player.getWorld().playSound(this, this.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_IMPACT_02.get(),
                    this.getSoundCategory(), 0.1f, 1.2f);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, Config.uniqueEffects.wraithfang.duration, Config.uniqueEffects.wraithfang.hasteAmplifier, false, false, true));
            int cooldown = 10;
            player.getItemCooldownManager().set(this.asItemStack().getItem(), cooldown);
            if (offhandThrow && player.getOffHandStack().isEmpty()) {
                // Send the ItemStack to the player's offhand slot if it's free
                player.setStackInHand(Hand.OFF_HAND, this.asItemStack());
                return true;
            } else {
                return player.getInventory().insertStack(this.asItemStack());
            }
        }
        return super.tryPickup(player);
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.WRAITHFANG.get());
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
