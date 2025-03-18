package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FrostfallEntity extends ThrownSwordEntity {
    private int remainingDetonations = 5;

    // Base Constructor
    public FrostfallEntity(EntityType<? extends FrostfallEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public FrostfallEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }

    @Override
    protected void doEffects(ServerWorld serverWorld, float baseDamage, Entity entity) {
        int bonusParticles =  ((int) baseDamage / 2);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.POOF, 0.5f, 3+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.CRIT, 0.5f, 5+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.ITEM_SNOWBALL, 0.5f, 2+bonusParticles);
        if (baseDamage > primaryBaseDamage)
            serverWorld.playSoundFromEntity(null, entity, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                    this.getSoundCategory(), 0.3f, 1.2f);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.FROSTFALL.get());
    }

    protected boolean tryPickup(PlayerEntity player) {
        if (this.isNoClip() && this.isOwner(player)) {
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
    protected void doOnTick(Entity entity) {
        super.doOnTick(entity);

        if (entity != null && entity instanceof LivingEntity livingEntity) {
            if (entity.isOnGround()) {
                int detonateDelay = 20;
                int detonateRadius = 8;
                float detonateDamage = 6;
                ServerWorld world = (ServerWorld) this.getWorld();
                DamageSource damageSource = this.getDamageSources().trident(this, entity);

                if (remainingDetonations <= 0) {
                    //remainingDetonations = 5; // For Cycling effect
                    returnToPlayer = true;
                }

                if ((age % detonateDelay == 0) && remainingDetonations > 0) {
                    int detonateCount = remainingDetonations;

                    Box box = HelperMethods.createBox(this, detonateRadius - detonateCount);

                    for (Entity otherEntity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((otherEntity instanceof LivingEntity le) &&
                                HelperMethods.checkFriendlyFire(le, livingEntity) &&
                                le.timeUntilRegen == 0) {
                            le.damage(damageSource, detonateDamage - detonateCount);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, Math.min( 3, 6-detonateCount)), livingEntity);
                            if (le.distanceTo(this) > 1)
                                le.setVelocity((this.getX() - le.getX()) / 8, (this.getY() - le.getY()) / 8, (this.getZ() - le.getZ()) / 8);
                        }
                    }

                    world.playSoundFromEntity(null, entity,
                            SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                            this.getSoundCategory(),
                            0.6f - ((float) detonateCount / 10),
                            0.8f + ((float) detonateCount / 10)
                    );

                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.POOF, 6f - detonateCount, 9 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.CRIT, 6f - detonateCount, 15 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.ITEM_SNOWBALL, 6f - detonateCount, 10 - detonateCount);

                    remainingDetonations--;
                }
            }
        }
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
