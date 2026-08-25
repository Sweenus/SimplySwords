package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.BuiltinUniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BrimstoneEruptionManager {
    private static final int MAX_AREA_TARGETS = 64;

    private BrimstoneEruptionManager() {
    }

    public static int erupt(UniqueAbilityExecution execution) {
        UniqueAbilityContext context = execution.context();
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        LivingEntity primary = context.target();
        if (primary == null || !primary.isAlive()) return 0;
        double radius = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_RADIUS)
                * execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_RADIUS_MULTIPLIER);
        float baseDamage = HelperMethods.abilityScaledDamage("fire", actor, context.stack(),
                execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_DAMAGE_SCALING).floatValue(),
                execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_SPELL_SCALING).floatValue())
                * execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_DAMAGE_MULTIPLIER).floatValue();
        List<LivingEntity> killed = new ArrayList<>();
        Set<UUID> directHits = new HashSet<>();
        int affected = 0;
        for (LivingEntity target : targets(world, actor, primary.getPos(), radius)) {
            if (affected >= MAX_AREA_TARGETS) break;
            boolean burning = target.isOnFire();
            float damage = baseDamage;
            if (burning) damage *= execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_BURNING_DAMAGE_MULTIPLIER).floatValue();
            if (target == primary) damage *= execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_PRIMARY_DAMAGE_MULTIPLIER).floatValue();
            ignite(target, execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_FIRE_TICKS));
            applyForce(execution, primary, target);
            int slow = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_SLOWNESS_TICKS);
            if (slow > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0,
                    false, true, true), actor);
            if (damage(world, actor, context, target, damage)) {
                directHits.add(target.getUuid());
                affected++;
                spawnHit(world, actor, primary, target);
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                        BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_HIT, target, 1, damage);
                if (!target.isAlive()) killed.add(target);
            }
        }
        affected += scatterCinders(execution, primary, baseDamage, directHits);
        affected += chainReactions(execution, killed, baseDamage, directHits);
        playSound(world, primary);
        return affected;
    }

    private static int scatterCinders(UniqueAbilityExecution execution, LivingEntity primary, float baseDamage,
                                      Set<UUID> excluded) {
        int count = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CINDER_COUNT);
        double multiplier = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CINDER_DAMAGE_MULTIPLIER);
        if (count <= 0 || multiplier <= 0.0) return 0;
        UniqueAbilityContext context = execution.context();
        List<LivingEntity> candidates = targets(context.world(), context.actor(), primary.getPos(),
                execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CINDER_RANGE)).stream()
                .filter(target -> !excluded.contains(target.getUuid()))
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(primary)))
                .limit(count)
                .toList();
        int affected = 0;
        for (LivingEntity target : candidates) {
            float damage = baseDamage * (float) multiplier;
            ignite(target, execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CINDER_FIRE_TICKS));
            if (damage(context.world(), context.actor(), context, target, damage)) {
                affected++;
                excluded.add(target.getUuid());
                context.world().spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(),
                        8, 0.2, 0.2, 0.2, 0.04);
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                        BuiltinUniqueAbilities.BRIMSTONE_CINDER_HIT, target, 1, damage);
            }
        }
        return affected;
    }

    private static int chainReactions(UniqueAbilityExecution execution, List<LivingEntity> killed, float baseDamage,
                                      Set<UUID> hitTargets) {
        int maximum = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CHAIN_MAX_DETONATIONS);
        double multiplier = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CHAIN_DAMAGE_MULTIPLIER);
        if (maximum <= 0 || multiplier <= 0.0) return 0;
        UniqueAbilityContext context = execution.context();
        int detonations = 0;
        int affected = 0;
        for (LivingEntity source : killed) {
            if (detonations++ >= maximum) break;
            double radius = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_CHAIN_RADIUS);
            context.world().spawnParticles(ParticleTypes.EXPLOSION, source.getX(), source.getBodyY(0.5), source.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            for (LivingEntity target : targets(context.world(), context.actor(), source.getPos(), radius)) {
                if (!hitTargets.add(target.getUuid())) continue;
                float damage = baseDamage * (float) multiplier;
                if (damage(context.world(), context.actor(), context, target, damage)) {
                    affected++;
                    ignite(target, 40);
                    UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                            BuiltinUniqueAbilities.BRIMSTONE_CHAIN_HIT, target, 1, damage);
                    if (affected >= MAX_AREA_TARGETS) return affected;
                }
            }
        }
        return affected;
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity actor, Vec3d center, double radius) {
        Box box = new Box(center, center).expand(radius);
        return world.getEntitiesByClass(LivingEntity.class, box, target -> target != actor
                && target.isAlive()
                && target.squaredDistanceTo(center) <= radius * radius
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor));
    }

    private static boolean damage(ServerWorld world, LivingEntity actor, UniqueAbilityContext context,
                                  LivingEntity target, float amount) {
        DamageSource source = world.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, context.stack(), target, source, amount);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        return damaged[0];
    }

    private static void applyForce(UniqueAbilityExecution execution, LivingEntity primary, LivingEntity target) {
        if (BuiltinUniqueAbilities.BRIMSTONE_FORCE_BACKDRAFT.equals(
                execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_FORCE_MODE))) {
            double pull = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_BACKDRAFT_PULL);
            Vec3d delta = primary.getPos().subtract(target.getPos());
            double length = delta.horizontalLength();
            if (pull > 0.0 && length > 0.001) {
                target.setVelocity(target.getVelocity().add(delta.x / length * pull, 0.05, delta.z / length * pull));
                target.velocityModified = true;
            }
        } else {
            target.takeKnockback(1.0, 0.1, 0.1);
        }
    }

    private static void ignite(LivingEntity target, int ticks) {
        if (ticks > 0) target.setOnFireFor(Math.max(1, (ticks + 19) / 20));
    }

    private static void spawnHit(ServerWorld world, LivingEntity actor, LivingEntity primary, LivingEntity target) {
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.LAVA, actor, primary, 3);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.LAVA, 1, 3);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 2, 6);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.POOF, 1, 10);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.EXPLOSION, 0.5, 2);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.WARPED_SPORE, 1, 10);
    }

    private static void playSound(ServerWorld world, LivingEntity target) {
        int sound = world.random.nextInt(3);
        if (sound <= 1) {
            world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(),
                    target.getSoundCategory(), 0.5F, 1.2F);
        } else {
            world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                    target.getSoundCategory(), 0.7F, 1.1F);
        }
    }
}
