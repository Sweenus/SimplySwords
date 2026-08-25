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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.FrostfallIceSpikeFieldManager;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;

public class FrostfallEntity extends ThrownSwordEntity {
    private int remainingDetonations = 5;
    public double detonateRadius = 8;
    public float detonateDamage = 11;
    public int duration = 40;
    public int addedChance = 0;
    private Phase6AbilityTuning masteryTuning = Phase6AbilityTuning.EMPTY;
    private UniqueAbilityExecution masteryExecution;

    // Base Constructor
    public FrostfallEntity(EntityType<? extends FrostfallEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public FrostfallEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }

    public void setMastery(Phase6AbilityTuning tuning, UniqueAbilityExecution execution) {
        masteryTuning = tuning == null ? Phase6AbilityTuning.EMPTY : tuning;
        masteryExecution = execution;
        remainingDetonations = masteryTuning.integer(s("PULSE_COUNT"), 5);
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

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        if (this.getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity target
                && !HelperMethods.checkAbilityTarget(target, owner)) {
            return;
        }
        boolean wasNonReturning = this.nonReturning;
        this.nonReturning = false;
        super.onEntityHit(entityHitResult);
        if (entity instanceof LivingEntity target && this.getOwner() instanceof LivingEntity owner) {
            int slow = masteryTuning.integer(s("STATUS_DURATION_TICKS"), 0);
            if (slow > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0), owner);
            int freeze = masteryTuning.integer(s("FREEZE_TICKS"), 0);
            if (freeze > 0) target.setFrozenTicks(Math.min(target.getMinFreezeDamageTicks(), target.getFrozenTicks() + freeze));
            if (masteryExecution != null) UniqueAbilityApi.emit(masteryExecution, UniqueAbilityPhase.HIT,
                    Phase6UniqueAbilities.HIT, target, 1, primaryBaseDamage);
        }
        this.nonReturning = wasNonReturning;
        if (!this.isRemoved()) {
            this.inGround = true;
            this.setVelocity(Vec3d.ZERO);
            this.velocityModified = true;
        }
    }

    @Override
    protected void doOnTick(Entity entity) {
        super.doOnTick(entity);

        if (entity != null && entity instanceof LivingEntity livingEntity) {
            if (this.inGround) {
                int detonateDelay = 20;
                int chance = 1;
                ServerWorld world = (ServerWorld) this.getWorld();
                DamageSource damageSource = this.getDamageSources().trident(this, entity);

                if (remainingDetonations <= 0) {
                    returnToPlayer = true;
                    if (masteryExecution != null) {
                        UniqueAbilityApi.finish(masteryExecution, Phase6UniqueAbilities.FINISH, 0);
                        masteryExecution = null;
                    }
                }

                if ((age % detonateDelay == 0) && remainingDetonations > 0) {
                    int detonateCount = remainingDetonations;

                    float pulseMultiplier = (6.0f - detonateCount) / 5.0f;
                    float pulseDamage = detonateDamage * pulseMultiplier
                            * (float) masteryTuning.get(s("FINAL_DAMAGE_MULTIPLIER"), 1);

                    Box box = HelperMethods.createBox(this, detonateRadius - detonateCount);

                    int affected = 0;
                    int cap = masteryTuning.has(s("TARGET_CAP"))
                            ? masteryTuning.integer(s("TARGET_CAP"), 64) : Integer.MAX_VALUE;
                    for (Entity otherEntity : world.getOtherEntities(this, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if ((otherEntity instanceof LivingEntity le) &&
                                HelperMethods.checkFriendlyFire(le, livingEntity)) {
                            float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, le, damageSource, pulseDamage);
                            HelperMethods.damageThroughIframes(le, damageSource, damage);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, Math.min( 3, 6-detonateCount)), livingEntity);
                            if (le.distanceTo(this) > 1)
                                le.setVelocity((this.getX() - le.getX()) / 8 * masteryTuning.get(s("PULL_STRENGTH"), 1),
                                        (this.getY() - le.getY()) / 8,
                                        (this.getZ() - le.getZ()) / 8 * masteryTuning.get(s("PULL_STRENGTH"), 1));
                            affected++;
                            if (affected >= cap) break;
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

                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.POOF, 6f - detonateCount, 9 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.CRIT, 6f - detonateCount, 15 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.ITEM_SNOWBALL, 6f - detonateCount, 10 - detonateCount);
                    HelperMethods.spawnOrbitParticles(world, this.getPos().add(0, 1, 0), ParticleTypes.WHITE_ASH, 6f - detonateCount, 40 - detonateCount);
                    FrostfallIceSpikeFieldManager.createPulse(world, this.getPos(), detonateRadius - detonateCount, detonateCount);
                    if (masteryExecution != null) UniqueAbilityApi.emit(masteryExecution, UniqueAbilityPhase.HIT,
                            Phase6UniqueAbilities.PULSE, null, affected, pulseDamage);

                    if (random.nextInt(100) > chance)
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

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }


}
