package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.FlamewindVisualManager;

import java.util.*;

public class FlameSeedEffect extends OrbitingEffect {
    private static final int FLAME_SEED_DURATION = 101;
    private static final Set<UUID> DETONATING_TARGETS = new HashSet<>();
    private static final Map<ServerWorld, List<PendingDeathDetonation>> PENDING_DEATH_DETONATIONS = new HashMap<>();

    private record PendingDeathDetonation(UUID targetId, Vec3d position, LivingEntity sourceEntity, int spreadRemaining) {
    }

    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public FlameSeedEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.ASH);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            float abilityDamage = 0f;
            float volume = 0.3f;
            float pitch = 1.3f;
            int frequency = 20;
            SoundEvent soundEvent = SoundEvents.ENTITY_GENERIC_BURN;
            StatusEffectInstance currentEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
            int duration = currentEffect == null ? 0 : currentEffect.getDuration();
            if (currentEffect instanceof SimplySwordsStatusEffectInstance statusEffect) {
                this.sourceEntity = statusEffect.getSourceEntity();
                this.additionalData = statusEffect.getAdditionalData();
            }
            if (this.sourceEntity != null) {
                abilityDamage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("flamewind"), this.sourceEntity, this.sourceEntity.getMainHandStack(),
                        Config.uniqueEffects.flamewind.damageScaling, Config.uniqueEffects.flamewind.spellScaling);
            }
            FlamewindVisualManager.refreshSeed(serverWorld, livingEntity);

            if (livingEntity.age % frequency == 0 && this.additionalData != 0) {
                DamageSource damageSource = livingEntity.getDamageSources().magic();
                livingEntity.timeUntilRegen = 0;

                boolean expiryDetonation = false;
                if (duration < 20  && this.sourceEntity != null) {
                    triggerDetonation(serverWorld, livingEntity, this.sourceEntity, this.additionalData);
                    livingEntity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
                    expiryDetonation = true;
                    abilityDamage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("flamewind"), this.sourceEntity,
                            this.sourceEntity == null ? null : this.sourceEntity.getMainHandStack(),
                            Config.uniqueEffects.flamewind.detonationDamageScaling,
                            Config.uniqueEffects.flamewind.detonationSpellScaling);
                    volume = 0.6f;
                    pitch = 1.0f;
                    soundEvent = SoundRegistry.SPELL_FIRE.get();
                }

                if (this.sourceEntity != null) {
                    damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, this.sourceEntity);
                }

                if (livingEntity instanceof PlayerEntity && this.sourceEntity !=null && this.sourceEntity instanceof  PlayerEntity playerSourceEntity)
                    damageSource = livingEntity.getDamageSources().playerAttack(playerSourceEntity);

                if (expiryDetonation) {
                    DETONATING_TARGETS.add(livingEntity.getUuid());
                }
                float damage = this.additionalData + ((float) amplifier / 4) + abilityDamage;
                if (this.sourceEntity != null) {
                    damage = HelperMethods.applyAbilityDamageEnchantments(serverWorld, this.sourceEntity.getMainHandStack(), livingEntity, damageSource, damage);
                }
                livingEntity.damage(damageSource, damage);
                if (expiryDetonation) {
                    DETONATING_TARGETS.remove(livingEntity.getUuid());
                }
                serverWorld.playSound(null, livingEntity.getBlockPos(), soundEvent,
                        livingEntity.getSoundCategory(), volume, pitch);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.LAVA, 1, 4);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.ASH, 1, 6);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos(), ParticleTypes.SMOKE, 1, 6);
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
    }

    public static boolean hasPendingDeathDetonations(ServerWorld world) {
        List<PendingDeathDetonation> pending = PENDING_DEATH_DETONATIONS.get(world);
        return pending != null && !pending.isEmpty();
    }

    public static void tickPendingDeathDetonations(ServerWorld world) {
        List<PendingDeathDetonation> pending = PENDING_DEATH_DETONATIONS.remove(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        for (PendingDeathDetonation detonation : pending) {
            Entity targetEntity = world.getEntity(detonation.targetId());
            if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                DETONATING_TARGETS.remove(detonation.targetId());
                continue;
            }

            triggerDetonation(world, detonation.position(), detonation.targetId(), detonation.sourceEntity(), detonation.spreadRemaining());
            DETONATING_TARGETS.remove(detonation.targetId());
        }
    }

    public static void queueLethalDeathDetonation(LivingEntity livingEntity, float incomingDamage) {
        if (livingEntity == null
                || livingEntity.getWorld().isClient()
                || incomingDamage < livingEntity.getHealth()
                || DETONATING_TARGETS.contains(livingEntity.getUuid())) {
            return;
        }

        StatusEffectInstance currentEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
        if (!(currentEffect instanceof SimplySwordsStatusEffectInstance statusEffect)) {
            return;
        }

        LivingEntity sourceEntity = statusEffect.getSourceEntity();
        int additionalData = statusEffect.getAdditionalData();
        if (sourceEntity == null || additionalData == 0 || !(livingEntity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        UUID targetId = livingEntity.getUuid();
        if (!DETONATING_TARGETS.add(targetId)) {
            return;
        }
        PENDING_DEATH_DETONATIONS.computeIfAbsent(serverWorld, ignored -> new ArrayList<>())
                .add(new PendingDeathDetonation(targetId, livingEntity.getPos(), sourceEntity, additionalData));
    }

    public static void triggerDeathDetonation(LivingEntity livingEntity) {
        if (livingEntity == null || livingEntity.getWorld().isClient()) {
            return;
        }
        UUID targetId = livingEntity.getUuid();
        if (!DETONATING_TARGETS.add(targetId)) {
            return;
        }

        StatusEffectInstance currentEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
        if (!(currentEffect instanceof SimplySwordsStatusEffectInstance statusEffect)) {
            DETONATING_TARGETS.remove(targetId);
            return;
        }

        LivingEntity sourceEntity = statusEffect.getSourceEntity();
        int additionalData = statusEffect.getAdditionalData();
        if (sourceEntity == null || additionalData == 0 || !(livingEntity.getWorld() instanceof ServerWorld serverWorld)) {
            DETONATING_TARGETS.remove(targetId);
            return;
        }

        try {
            triggerDetonation(serverWorld, livingEntity, sourceEntity, additionalData);
            livingEntity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
        } finally {
            DETONATING_TARGETS.remove(targetId);
        }
    }

    private static void triggerDetonation(ServerWorld serverWorld, LivingEntity livingEntity, LivingEntity sourceEntity, int spreadRemaining) {
        triggerDetonation(serverWorld, livingEntity.getPos(), livingEntity.getUuid(), sourceEntity, spreadRemaining);
        FlamewindVisualManager.removeSeed(serverWorld, livingEntity);
    }

    private static void triggerDetonation(ServerWorld serverWorld, Vec3d center, UUID excludedTargetId, LivingEntity sourceEntity, int spreadRemaining) {
        FlamewindVisualManager.spawnDetonation(serverWorld, center);
        serverWorld.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 0.4, center.z, 8, 0.75, 0.35, 0.75, 0.02);
        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, center.x, center.y + 0.4, center.z, 6, 0.8, 0.45, 0.8, 0.03);
        serverWorld.spawnParticles(ParticleTypes.POOF, center.x, center.y + 0.2, center.z, 10, 0.55, 0.25, 0.55, 0.04);
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.35, center.z, 2, 0.35, 0.2, 0.35, 0.01);
        serverWorld.spawnParticles(ParticleTypes.WARPED_SPORE, center.x, center.y + 0.35, center.z, 10, 0.8, 0.35, 0.8, 0.02);

        float abilityDamage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("flamewind"), sourceEntity,
                sourceEntity == null ? null : sourceEntity.getMainHandStack(),
                Config.uniqueEffects.flamewind.detonationDamageScaling,
                Config.uniqueEffects.flamewind.detonationSpellScaling);
        if (center.distanceTo(sourceEntity.getPos()) < 30) {
            int maxHaste = Config.uniqueEffects.flamewind.maxHaste;
            HelperMethods.incrementStatusEffect(sourceEntity, StatusEffects.HASTE, 120, 1, maxHaste);
        }

        DamageSource damageSource = serverWorld.getDamageSources().magic();
        double detonationRadius = Config.uniqueEffects.flamewind.spreadDistance;
        Box box = new Box(
                center.x - detonationRadius,
                center.y - detonationRadius / 3.0,
                center.z - detonationRadius,
                center.x + detonationRadius,
                center.y + detonationRadius / 3.0,
                center.z + detonationRadius
        );
        int remaining = spreadRemaining;
        for (LivingEntity le : serverWorld.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (le.getUuid().equals(excludedTargetId) || !HelperMethods.checkFriendlyFire(le, sourceEntity)) {
                continue;
            }

            float damage = sourceEntity == null ? abilityDamage
                    : HelperMethods.applyAbilityDamageEnchantments(serverWorld, sourceEntity.getMainHandStack(), le, damageSource, abilityDamage);
            le.damage(damageSource, damage);
            if (!le.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED)) && remaining > 0) {
                remaining -= 1;
                SimplySwordsStatusEffectInstance flameSeedEffect = new SimplySwordsStatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FLAMESEED), FLAME_SEED_DURATION, 0, false,
                        false, true);
                flameSeedEffect.setSourceEntity(sourceEntity);
                flameSeedEffect.setAdditionalData(remaining);
                le.addStatusEffect(flameSeedEffect);
                Vec3d knockback = new Vec3d(le.getX() - center.x, 0.0, le.getZ() - center.z);
                if (knockback.lengthSquared() > 0.0001) {
                    knockback = knockback.normalize().multiply(0.28);
                }
                le.setVelocity(knockback.x, 0.12, knockback.z);
                FlamewindVisualManager.refreshSeed(serverWorld, le);
                FlamewindVisualManager.spawnSpreadArc(serverWorld, center, le);
            }
        }
    }

    @Override
    public void onRemoved(LivingEntity effectEntity, AttributeContainer attributes, int amplifier) {
        LivingEntity livingEntity = getEntityFromAttributeContainer(attributes);
        if (livingEntity != null && !livingEntity.getWorld().isClient() && livingEntity.getWorld() instanceof ServerWorld serverWorld) {
            FlamewindVisualManager.removeSeed(serverWorld, livingEntity);
        }
        super.onRemoved(effectEntity, attributes, amplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
