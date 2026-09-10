package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.api.combat.ProvenanceCarrier;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.FlamewindVisualManager;
import net.sweenus.simplyswords.world.FlamewindMasteryManager;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;

import java.util.*;

public class FlameSeedEffect extends OrbitingEffect {
    private static final int FLAME_SEED_DURATION = 101;
    private static final Set<UUID> DETONATING_TARGETS = new HashSet<>();
    private static final Map<ServerWorld, List<PendingDeathDetonation>> PENDING_DEATH_DETONATIONS = new HashMap<>();

    private record PendingDeathDetonation(UUID targetId, Vec3d position, LivingEntity sourceEntity, int spreadRemaining,
                                          FlamewindMasteryManager.SeedSnapshot snapshot, CombatProvenance provenance) {
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
    public boolean applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            sourceEntity = null;
            additionalData = 0;
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            StatusEffectInstance currentEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
            int duration = currentEffect == null ? 0 : currentEffect.getDuration();
            if (currentEffect instanceof SimplySwordsStatusEffectInstance statusEffect) {
                this.sourceEntity = statusEffect.getSourceEntity();
                this.additionalData = statusEffect.getAdditionalData();
            }
            FlamewindMasteryManager.SeedSnapshot snapshot = FlamewindMasteryManager.snapshot(livingEntity);
            FireForgeMasteryTuning tuning = snapshot == null ? FireForgeMasteryTuning.EMPTY : snapshot.tuning();
            ItemStack castingStack = snapshot == null ? ItemStack.EMPTY : snapshot.stack();
            LivingEntity caster = this.sourceEntity;
            int spreadRemaining = this.additionalData;
            FlamewindVisualManager.refreshSeed(serverWorld, livingEntity);
            if (caster != null && livingEntity.age % 20 == 0) {
                FlamewindMasteryManager.refreshDraft(serverWorld, caster, tuning);
            }

            if (caster != null && duration <= 1) {
                triggerDetonation(serverWorld, livingEntity, caster, spreadRemaining);
                livingEntity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
                float damage = baseDetonationDamage(caster, castingStack)
                        * (float) tuning.get(FireForgeMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)
                        * (float) tuning.get(FireForgeMasteryTuning.Setting.FLAMEWIND_DEATH_DAMAGE_MULTIPLIER, 1);
                damageSeedHost(serverWorld, livingEntity, caster, castingStack, damage, true);
            } else {
                int frequency = tuning.integer(FireForgeMasteryTuning.Setting.INTERVAL_TICKS, 20);
                double periodicMultiplier = tuning.get(FireForgeMasteryTuning.Setting.PERIODIC_DAMAGE_MULTIPLIER, 1);
                if (caster != null && periodicMultiplier > 0 && livingEntity.age % frequency == 0) {
                    double detonationMultiplier = tuning.get(
                            FireForgeMasteryTuning.Setting.FLAMEWIND_PERIODIC_DETONATION_MULTIPLIER, 0);
                    float damage = detonationMultiplier > 0
                            ? baseDetonationDamage(caster, castingStack) * (float) detonationMultiplier
                            : basePeriodicDamage(caster, castingStack);
                    damageSeedHost(serverWorld, livingEntity, caster, castingStack,
                            damage * (float) periodicMultiplier, false);
                }
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
        return true;
    }

    private static float basePeriodicDamage(LivingEntity sourceEntity, ItemStack castingStack) {
        return HelperMethods.abilityScaledDamage(SpellScalingComponents.id("flamewind"), sourceEntity,
                castingStack, Config.uniqueEffects.flamewind.damageScaling,
                Config.uniqueEffects.flamewind.spellScaling);
    }

    private static float baseDetonationDamage(LivingEntity sourceEntity, ItemStack castingStack) {
        return HelperMethods.abilityScaledDamage(SpellScalingComponents.id("flamewind"), sourceEntity,
                castingStack, Config.uniqueEffects.flamewind.detonationDamageScaling,
                Config.uniqueEffects.flamewind.detonationSpellScaling);
    }

    private static void damageSeedHost(ServerWorld world, LivingEntity target, LivingEntity sourceEntity,
                                       ItemStack castingStack, float damage, boolean detonation) {
        if (damage <= 0) return;
        DamageSource damageSource = target.getDamageSources().indirectMagic(target, sourceEntity);
        if (target instanceof PlayerEntity && sourceEntity instanceof PlayerEntity playerSourceEntity) {
            damageSource = target.getDamageSources().playerAttack(playerSourceEntity);
        }
        target.timeUntilRegen = 0;
        if (detonation) DETONATING_TARGETS.add(target.getUuid());
        damage = HelperMethods.applyAbilityDamageEnchantments(world, castingStack, target,
                damageSource, damage);
        CombatProvenanceApi.damage(castingStack, sourceEntity, target, damageSource, damage);
        if (detonation) DETONATING_TARGETS.remove(target.getUuid());
        SoundEvent soundEvent = detonation ? SoundRegistry.SPELL_FIRE.get() : SoundEvents.ENTITY_GENERIC_BURN;
        world.playSound(null, target.getBlockPos(), soundEvent, target.getSoundCategory(),
                detonation ? .6f : .3f, detonation ? 1f : 1.3f);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.LAVA, 1, 4);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.ASH, 1, 6);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.SMOKE, 1, 6);
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

            try (var ignored = CombatProvenanceApi.scope(detonation.provenance())) {
                triggerDetonation(world, detonation.position(), detonation.targetId(), detonation.sourceEntity(),
                        detonation.spreadRemaining(), detonation.snapshot());
            } finally {
                FlamewindMasteryManager.remove(world, detonation.targetId());
                DETONATING_TARGETS.remove(detonation.targetId());
            }
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
        if (sourceEntity == null || !(livingEntity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        UUID targetId = livingEntity.getUuid();
        if (!DETONATING_TARGETS.add(targetId)) {
            return;
        }
        PENDING_DEATH_DETONATIONS.computeIfAbsent(serverWorld, ignored -> new ArrayList<>())
                .add(new PendingDeathDetonation(targetId, livingEntity.getPos(), sourceEntity, additionalData,
                        FlamewindMasteryManager.snapshot(livingEntity), ((ProvenanceCarrier) statusEffect).simplyswords$getProvenance()));
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
        if (sourceEntity == null || !(livingEntity.getWorld() instanceof ServerWorld serverWorld)) {
            DETONATING_TARGETS.remove(targetId);
            return;
        }

        try (var ignored = CombatProvenanceApi.scope(
                ((ProvenanceCarrier) statusEffect).simplyswords$getProvenance())) {
            triggerDetonation(serverWorld, livingEntity, sourceEntity, additionalData);
            livingEntity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
        } finally {
            DETONATING_TARGETS.remove(targetId);
        }
    }

    private static void triggerDetonation(ServerWorld serverWorld, LivingEntity livingEntity, LivingEntity sourceEntity, int spreadRemaining) {
        triggerDetonation(serverWorld, livingEntity.getPos(), livingEntity.getUuid(), sourceEntity, spreadRemaining,
                FlamewindMasteryManager.snapshot(livingEntity));
        FlamewindVisualManager.removeSeed(serverWorld, livingEntity);
        FlamewindMasteryManager.remove(serverWorld, livingEntity.getUuid());
    }

    private static void triggerDetonation(ServerWorld serverWorld, Vec3d center, UUID excludedTargetId, LivingEntity sourceEntity,
                                          int spreadRemaining, FlamewindMasteryManager.SeedSnapshot snapshot) {
        FlamewindVisualManager.spawnDetonation(serverWorld, center);
        serverWorld.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 0.4, center.z, 8, 0.75, 0.35, 0.75, 0.02);
        serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, center.x, center.y + 0.4, center.z, 6, 0.8, 0.45, 0.8, 0.03);
        serverWorld.spawnParticles(ParticleTypes.POOF, center.x, center.y + 0.2, center.z, 10, 0.55, 0.25, 0.55, 0.04);
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.35, center.z, 2, 0.35, 0.2, 0.35, 0.01);
        serverWorld.spawnParticles(ParticleTypes.WARPED_SPORE, center.x, center.y + 0.35, center.z, 10, 0.8, 0.35, 0.8, 0.02);

        ItemStack castingStack = snapshot == null ? ItemStack.EMPTY : snapshot.stack();
        float abilityDamage = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("flamewind"), sourceEntity,
                castingStack,
                Config.uniqueEffects.flamewind.detonationDamageScaling,
                Config.uniqueEffects.flamewind.detonationSpellScaling);
        FireForgeMasteryTuning tuning = snapshot == null ? FireForgeMasteryTuning.EMPTY : snapshot.tuning();
        abilityDamage *= (float) tuning.get(FireForgeMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1);
        if (center.distanceTo(sourceEntity.getPos()) < 30) {
            int maxHaste = Config.uniqueEffects.flamewind.maxHaste;
            HelperMethods.incrementStatusEffect(sourceEntity, StatusEffects.HASTE,
                    tuning.integer(FireForgeMasteryTuning.Setting.FLAMEWIND_HASTE_DURATION_TICKS, 120),
                    tuning.integer(FireForgeMasteryTuning.Setting.FLAMEWIND_HASTE_AMPLIFIER, 1), maxHaste);
        }
        abilityDamage *= (float) FlamewindMasteryManager.chainBonus(serverWorld, sourceEntity, tuning);
        abilityDamage *= (float) FlamewindMasteryManager.generationMultiplier(
                tuning.get(FireForgeMasteryTuning.Setting.FLAMEWIND_GENERATION_MULTIPLIER, 0),
                snapshot == null ? 0 : snapshot.generation(),
                tuning.integer(FireForgeMasteryTuning.Setting.FLAMEWIND_GENERATION_CAP, 0));

        DamageSource damageSource = serverWorld.getDamageSources().magic();
        double detonationRadius = tuning.get(FireForgeMasteryTuning.Setting.RADIUS,
                Config.uniqueEffects.flamewind.spreadDistance);
        Box box = new Box(
                center.x - detonationRadius,
                center.y - detonationRadius / 3.0,
                center.z - detonationRadius,
                center.x + detonationRadius,
                center.y + detonationRadius / 3.0,
                center.z + detonationRadius
        );
        int remaining = Math.min(spreadRemaining, tuning.integer(FireForgeMasteryTuning.Setting.SPREAD_CAP, spreadRemaining));
        int generationCap = tuning.integer(FireForgeMasteryTuning.Setting.FLAMEWIND_GENERATION_CAP, 0);
        if (generationCap > 0 && snapshot != null && snapshot.generation() >= generationCap) remaining = 0;
        double spreadRange = tuning.get(FireForgeMasteryTuning.Setting.FLAMEWIND_SPREAD_RANGE, detonationRadius);
        int affected = 0;
        int targetCap = tuning.integer(FireForgeMasteryTuning.Setting.TARGET_CAP, 10);
        for (LivingEntity le : serverWorld.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY)
                .stream().sorted(Comparator.comparingDouble((LivingEntity entity) -> entity.getPos().squaredDistanceTo(center))
                        .thenComparing(entity -> entity.getUuid().toString())).limit(Math.min(64, targetCap)).toList()) {
            if (le.getUuid().equals(excludedTargetId) || !HelperMethods.checkFriendlyFire(le, sourceEntity)) {
                continue;
            }

            float damage = sourceEntity == null ? abilityDamage
                    : HelperMethods.applyAbilityDamageEnchantments(serverWorld, castingStack, le, damageSource, abilityDamage);
            boolean lethal = CombatProvenanceApi.damage(castingStack, sourceEntity, le, damageSource, damage) && !le.isAlive();
            if (lethal) FlamewindMasteryManager.onReleaseKill(serverWorld, sourceEntity);
            affected++;
            int fireTicks = tuning.integer(FireForgeMasteryTuning.Setting.FIRE_TICKS, 0);
            if (fireTicks > 0) le.setOnFireFor(Math.max(1, fireTicks / 20));
            if (!le.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED)) && remaining > 0
                    && le.getPos().squaredDistanceTo(center) <= spreadRange * spreadRange) {
                remaining -= 1;
                SimplySwordsStatusEffectInstance flameSeedEffect = new SimplySwordsStatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FLAMESEED),
                        FlamewindMasteryManager.spreadDuration(FLAME_SEED_DURATION,
                                tuning.get(FireForgeMasteryTuning.Setting.FLAMEWIND_SPREAD_DURATION_FRACTION, 0)),
                        0, false, false, true);
                flameSeedEffect.setSourceEntity(sourceEntity);
                flameSeedEffect.setAdditionalData(remaining);
                le.addStatusEffect(flameSeedEffect);
                FlamewindMasteryManager.inherit(snapshot, le);
                FlamewindVisualManager.refreshSeed(serverWorld, le);
                FlamewindVisualManager.spawnSpreadArc(serverWorld, center, le);
            }
            applyDetonationKnockback(le, center, tuning);
        }
        if (snapshot != null) {
            FlamewindMasteryManager.onDetonation(serverWorld, sourceEntity, snapshot.stack(), tuning);
            UniqueAbilityApi.emit(snapshot.execution(), UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.PULSE,
                    null, affected, abilityDamage);
        }
    }


    private static void applyDetonationKnockback(LivingEntity target, Vec3d center, FireForgeMasteryTuning tuning) {
        Vec3d away = new Vec3d(target.getX() - center.x, 0.0, target.getZ() - center.z);
        if (away.lengthSquared() <= 0.0001) return;
        double strength = FlamewindMasteryManager.detonationKnockback(
                tuning.get(FireForgeMasteryTuning.Setting.PULL_STRENGTH, 0),
                tuning.get(FireForgeMasteryTuning.Setting.FLAMEWIND_KNOCKBACK_MULTIPLIER, 1));
        Vec3d push = away.normalize().multiply(strength);
        target.setVelocity(push.x, 0.12, push.z);
        target.velocityModified = true;
    }

    public static int detonateOwned(ServerWorld world, LivingEntity owner, int limit) {
        List<LivingEntity> targets = FlamewindMasteryManager.ownedSeeds(world, owner, limit);
        for (LivingEntity target : targets) {
            StatusEffectInstance effect = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
            int remaining = effect instanceof SimplySwordsStatusEffectInstance seeded ? seeded.getAdditionalData() : 0;
            triggerDetonation(world, target, owner, remaining);
            target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED));
        }
        return targets.size();
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        LivingEntity livingEntity = getEntityFromAttributeContainer(attributes);
        if (livingEntity != null && !livingEntity.getWorld().isClient() && livingEntity.getWorld() instanceof ServerWorld serverWorld) {
            FlamewindVisualManager.removeSeed(serverWorld, livingEntity);
            FlamewindMasteryManager.remove(serverWorld, livingEntity.getUuid());
        }
        super.onRemoved(attributes);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}
