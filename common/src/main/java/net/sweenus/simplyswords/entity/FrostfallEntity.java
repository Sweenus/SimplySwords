package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class FrostfallEntity extends ThrownSwordEntity {
    private int remainingDetonations = 5;
    public double detonateRadius = 8;
    public float detonateDamage = 11;
    public int duration = 40;
    public int addedChance = 0;

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
        HelperMethods.spawnOrbitParticles(serverWorld, this.getEntityPos(), ParticleTypes.POOF, 0.5f, 3+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getEntityPos(), ParticleTypes.CRIT, 0.5f, 5+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getEntityPos(), ParticleTypes.ITEM_SNOWBALL, 0.5f, 2+bonusParticles);
        if (baseDamage > primaryBaseDamage)
            serverWorld.playSoundFromEntity(null, entity, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                    this.getSoundCategory(), 0.3f, 1.2f);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.FROSTFALL.get());
    }

    @Override
    protected void doOnTick(Entity entity) {
        super.doOnTick(entity);

        if (entity != null && entity instanceof LivingEntity livingEntity) {
            if (this.isInGround()) {
                int detonateDelay = 20;
                int chance = 1;
                ServerWorld world = (ServerWorld) this.getEntityWorld();
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
                                HelperMethods.checkFriendlyFire(le, livingEntity)) {
                            HelperMethods.damageThroughIframes(le, damageSource, detonateDamage - detonateCount);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, Math.min( 3, 6-detonateCount)), livingEntity);
                            if (le.distanceTo(this) > 1)
                                le.setVelocity((this.getX() - le.getX()) / 8, (this.getY() - le.getY()) / 8, (this.getZ() - le.getZ()) / 8);
                        }
                    }
                    for (Entity otherEntity2 : world.getOtherEntities(this, box, EntityPredicates.VALID_ENTITY)) {
                        if (otherEntity2 instanceof FrostfallEntity) {
                            chance = Math.min(50, chance+addedChance);
                        }
                    }

                    world.playSoundFromEntity(null, this,
                            SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                            this.getSoundCategory(),
                            0.6f - ((float) detonateCount / 10),
                            0.8f + ((float) detonateCount / 10)
                    );

                    HelperMethods.spawnOrbitParticles(world, this.getEntityPos(), ParticleTypes.POOF, 6f - detonateCount, 9 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getEntityPos(), ParticleTypes.CRIT, 6f - detonateCount, 15 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getEntityPos(), ParticleTypes.ITEM_SNOWBALL, 6f - detonateCount, 10 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getEntityPos().add(0, 1, 0), ParticleTypes.WHITE_ASH, 6f - detonateCount, 40 - detonateCount);

                    if (random.nextInt(100) > chance)
                        remainingDetonations--;
                }
            }
        }
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
