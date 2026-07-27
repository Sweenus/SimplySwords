package net.sweenus.simplyswords.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.ChainLightningVisualManager;
import net.sweenus.simplyswords.world.LivyatanWaveManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LivyatanEntity extends ThrownSwordEntity {
    public int slownessDuration;
    private final Set<UUID> returnLightningRolledTargets = new HashSet<>();

    // Base Constructor
    public LivyatanEntity(EntityType<? extends LivyatanEntity> entityType, World world) {
        super(entityType, world);
    }

    // Constructor for owner and item stack
    public LivyatanEntity(World world, LivingEntity owner, ItemStack stack) {
        super(world, owner, stack);
        this.stack = stack;
    }
    @Override
    public void tick() {
        if (!nonReturning) {
            returnToPlayer = true;
        }
        super.tick();
    }

    @Override
    protected void damageOnReturn(double radius, float damage) {
        if (getWorld().isClient() || this.stack == null || this.stack.isEmpty()) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld world) || !(this.getOwner() instanceof LivingEntity user) || !user.isAlive()) {
            return;
        }

        Vec3d toOwner = user.getEyePos().subtract(this.getPos());
        Vec3d horizontalToOwner = new Vec3d(toOwner.x, 0.0, toOwner.z);
        if (horizontalToOwner.lengthSquared() <= 1.0E-6) {
            horizontalToOwner = Vec3d.fromPolar(0.0F, user.getYaw());
        } else {
            horizontalToOwner = horizontalToOwner.normalize();
        }
        LivyatanWaveManager.spawnReturnPulse(world, this.getPos(), horizontalToOwner, this.returnTimer);

        double waveRadius = Math.max(0.5, radius);
        Box box = Box.of(this.getPos().add(0.0, 0.5, 0.0), waveRadius * 2.0, 3.0, waveRadius * 2.0);
        DamageSource damageSource = user.getDamageSources().trident(this, user);
        boolean damageTick = this.returnTimer % 5 == 0;
        float lightningDamage = 0.0F;
        boolean calculatedLightningDamage = false;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!HelperMethods.checkAbilityTarget(target, user) || horizontalDistanceSquared(target.getPos(), this.getPos()) > waveRadius * waveRadius) {
                continue;
            }

            pullTargetTowardOwner(target, user);
            if (damageTick) {
                float returnDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage);
                if (HelperMethods.damageThroughIframes(target, damageSource, returnDamage)) {
                    world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_01.get(), user.getSoundCategory(), 0.2f, 1.5f);
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slownessDuration, 2), user);
                    HelperMethods.spawnOrbitParticles(world, this.getPos(), ParticleTypes.POOF, 0.5f, 3);
                }
            }
            if (this.returnLightningRolledTargets.add(target.getUuid())
                    && world.random.nextInt(100) < MathHelper.clamp(Config.uniqueEffects.livyatan.returnLightningChance, 0, 100)) {
                if (!calculatedLightningDamage) {
                    lightningDamage = HelperMethods.abilityScaledDamage("lightning", user, stack,
                            Config.uniqueEffects.livyatan.returnLightningDamageScaling,
                            Config.uniqueEffects.livyatan.returnLightningSpellScaling);
                    calculatedLightningDamage = true;
                }
                ChainLightningVisualManager.damageSkyBolt(world, user, stack, target, lightningDamage,
                        Config.uniqueEffects.livyatan.returnLightningSkyHeight,
                        ChainLightningVisualManager.STORMBRINGER_SETTINGS);
            }
        }
        pullLooseEntitiesTowardOwner(world, user, box, this.getPos(), waveRadius);
    }

    private static void pullTargetTowardOwner(LivingEntity target, LivingEntity owner) {
        double strength = Math.max(0.0, Config.uniqueEffects.livyatan.returnWavePullStrength) * getLivingPullScale(target);
        pullEntityTowardOwner(target, owner, strength, true);
        target.fallDistance = 0.0F;
    }

    private static double getLivingPullScale(LivingEntity target) {
        double sizeScore = Math.max(target.getHeight() / 1.25, target.getWidth() / 0.75);
        if (sizeScore <= 1.0) {
            return 1.0;
        }
        return MathHelper.clamp(1.0 / sizeScore, 0.25, 1.0);
    }

    private static void pullLooseEntitiesTowardOwner(ServerWorld world, LivingEntity owner, Box box, Vec3d waveCenter, double waveRadius) {
        double strength = Math.max(0.0, Config.uniqueEffects.livyatan.returnWavePullStrength) * 1.35;
        if (strength <= 0.0) {
            return;
        }

        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box, entity -> entity.isAlive() && entity.isOnGround())) {
            if (horizontalDistanceSquared(item.getPos(), waveCenter) <= waveRadius * waveRadius) {
                pullEntityTowardOwner(item, owner, strength, false);
            }
        }
        for (ExperienceOrbEntity orb : world.getEntitiesByClass(ExperienceOrbEntity.class, box, entity -> entity.isAlive() && entity.isOnGround())) {
            if (horizontalDistanceSquared(orb.getPos(), waveCenter) <= waveRadius * waveRadius) {
                pullEntityTowardOwner(orb, owner, strength, false);
            }
        }
    }

    private static void pullEntityTowardOwner(Entity target, LivingEntity owner, double strength, boolean matchOwnerHeight) {
        Vec3d pull = owner.getPos().subtract(target.getPos());
        Vec3d horizontal = new Vec3d(pull.x, 0.0, pull.z);
        if (horizontal.lengthSquared() <= 1.0E-6) {
            return;
        }
        Vec3d velocity = horizontal.normalize().multiply(strength);
        double upward = matchOwnerHeight && target instanceof LivingEntity livingTarget
                ? MathHelper.clamp((owner.getBodyY(0.55) - livingTarget.getBodyY(0.45)) * 0.08, -0.08, 0.16)
                : 0.08;
        target.addVelocity(velocity.x, upward, velocity.z);
        target.velocityModified = true;
        target.velocityDirty = true;
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    @Override
    protected void doEffects(ServerWorld serverWorld, float baseDamage, Entity entity) {
        int bonusParticles =  ((int) baseDamage / 2);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.POOF, 0.5f, 3+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.CRIT, 0.5f, 5+bonusParticles);
        HelperMethods.spawnOrbitParticles(serverWorld, this.getPos(), ParticleTypes.SNOWFLAKE, 0.5f, 2+bonusParticles);
        if (baseDamage > primaryBaseDamage)
            serverWorld.playSoundFromEntity(null, entity, SoundRegistry.MAGIC_SWORD_PARRY_VARIOUS_HITS.get(),
                    this.getSoundCategory(), 0.3f, 1.2f);
    }

    @Override
    protected SoundEvent getReturnSound() {
        return SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_01.get();
    }

    @Override
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ItemsRegistry.LIVYATAN.get());
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

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if ((this.stack == null || this.stack.isEmpty()) && nbt.contains("item")) {
            this.stack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("item")).orElse(this.getDefaultItemStack());
        }
    }

}
