package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.Phase4AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase4UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Phase4StandardManager {
    private static final Map<UUID, StandardState> ACTIVE = new HashMap<>();

    private Phase4StandardManager() {
    }

    public static boolean tickSunfire(BattleStandardEntity standard, UniqueAbilityExecution execution,
                                      ItemStack stack) {
        if (execution == null || execution.isTerminal() || !(standard.getWorld() instanceof ServerWorld world)
                || standard.ownerEntity == null || !standard.ownerEntity.isAlive()) {
            terminate(execution, false, 0);
            return false;
        }
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        StandardState state = ACTIVE.computeIfAbsent(standard.getUuid(), ignored ->
                new StandardState(standard.ownerEntity.getUuid(), standard.getUuid(), true, execution, stack));
        moveSunfire(standard, tuning);
        int life = (int) Math.round(500 * tuning.get(s("LIFETIME_MULTIPLIER"), 1));
        if (standard.age >= life) {
            terminate(execution, true, state.hits);
            ACTIVE.remove(standard.getUuid());
            standard.discard();
            return true;
        }
        if (standard.isOnGround() && !state.landed) {
            state.landed = true;
            state.hits += sunfireLanding(world, standard, state, tuning);
        }
        int interval = tuning.integer(s("INTERVAL_TICKS"), 10);
        if (standard.age % interval == 0) state.hits += sunfireHostilePulse(world, standard, state, tuning);
        int supportInterval = tuning.integer(s("SUPPORT_INTERVAL_TICKS"), 80);
        if (standard.age % supportInterval == 0) sunfireSupportPulse(world, standard, state, tuning);
        return true;
    }

    public static boolean tickHarbinger(BattleStandardDarkEntity standard, UniqueAbilityExecution execution,
                                        ItemStack stack) {
        if (execution == null || execution.isTerminal() || !(standard.getWorld() instanceof ServerWorld world)
                || standard.ownerEntity == null || !standard.ownerEntity.isAlive()) {
            terminate(execution, false, 0);
            return false;
        }
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(execution);
        StandardState state = ACTIVE.computeIfAbsent(standard.getUuid(), ignored ->
                new StandardState(standard.ownerEntity.getUuid(), standard.getUuid(), false, execution, stack));
        moveHarbinger(world, standard, tuning);
        int life = (int) Math.round(500 * tuning.get(s("LIFETIME_MULTIPLIER"), 1));
        if (standard.age >= life) {
            terminate(execution, true, state.hits);
            ACTIVE.remove(standard.getUuid());
            standard.discard();
            return true;
        }
        if (standard.isOnGround() && !state.landed) {
            state.landed = true;
            state.hits += harbingerLanding(world, standard, state, tuning);
        }
        int interval = tuning.integer(s("INTERVAL_TICKS"), 10);
        if (standard.age % interval == 0) state.hits += harbingerHostilePulse(world, standard, state, tuning);
        int supportInterval = tuning.integer(s("SUPPORT_INTERVAL_TICKS"), 80);
        if (standard.age % supportInterval == 0) harbingerSupportPulse(world, standard, state, tuning);
        harbingerOwnerAura(standard, tuning);
        return true;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        StandardState state = ownerState(target.getUuid(), true);
        if (state == null || !(target.getWorld() instanceof ServerWorld world)) return amount;
        Entity entity = world.getEntity(state.entityId);
        if (!(entity instanceof BattleStandardEntity standard)) return amount;
        Phase4AbilityTuning tuning = Phase4UniqueAbilities.tuning(state.execution);
        double distance = target.squaredDistanceTo(standard);
        if (tuning.flag(16384) && distance <= 49) amount *= 1F - (float) tuning.get(s("DAMAGE_REDUCTION"), .15);
        if (tuning.flag(65536) && amount >= target.getHealth()) {
            standard.discard();
            ACTIVE.remove(standard.getUuid());
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 2));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 1));
            SimplySwordsAPI.setWeaponCooldown(target, state.stack,
                    state.execution.cooldownTicks(Config.uniqueEffects.sunfire.cooldown) + 300);
            terminate(state.execution, true, state.hits);
            return Math.max(0, target.getHealth() - 1);
        }
        if (tuning.flag(8192) && source.getAttacker() instanceof LivingEntity attacker
                && attacker.squaredDistanceTo(target) <= 16) attacker.setOnFireFor(2);
        return amount;
    }

    public static Vec3d standardPosition(UUID ownerId, boolean sunfire) {
        StandardState state = ownerState(ownerId, sunfire);
        if (state == null) return null;
        if (!(state.execution.context().world().getEntity(state.entityId) instanceof LivingEntity standard)
                || !standard.isAlive()) return null;
        return standard.getPos();
    }

    public static void reduceCooldown(UUID ownerId, int ticks) {
        StandardState state = ownerState(ownerId, false);
        if (state == null) state = ownerState(ownerId, true);
        if (state == null || state.refunded >= 100) return;
        int applied = Math.min(ticks, 100 - state.refunded);
        state.refunded += applied;
        Entity entity = state.execution.context().world().getEntity(state.ownerId);
        if (entity instanceof LivingEntity owner) {
            int base = state.execution.cooldownTicks(state.sunfire
                    ? Config.uniqueEffects.sunfire.cooldown : Config.uniqueEffects.harbinger.cooldown);
            SimplySwordsAPI.setWeaponCooldown(owner, state.stack, Math.max(0, base - state.refunded));
        }
    }

    private static int sunfireHostilePulse(ServerWorld world, BattleStandardEntity standard,
                                            StandardState state, Phase4AbilityTuning tuning) {
        if (tuning.flag(512)) return 0;
        double radius = tuning.get(s("RADIUS"), 6);
        int cap = tuning.integer(s("TARGET_CAP"), 32);
        List<LivingEntity> targets = targets(world, standard, standard.ownerEntity, radius, cap, true);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(standard.spellScalingOwner, "damage"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.sunfire.damageScaling,
                Config.uniqueEffects.sunfire.spellScaling);
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        if (tuning.flag(4) && standard.age <= 100) multiplier *= tuning.get(s("EARLY_DAMAGE_MULTIPLIER"), 1.2);
        int pulse = ++state.pulses;
        if (tuning.flag(2) && pulse % tuning.integer(s("COUNT"), 4) == 0) {
            multiplier *= tuning.get(s("CYCLE_DAMAGE_MULTIPLIER"), 1.4);
        }
        int affected = 0;
        for (LivingEntity target : targets) {
            int count = state.pulseCounts.merge(target.getUuid(), 1, Integer::sum);
            float damage = base * (float) multiplier;
            if (deal(world, standard.ownerEntity, state.stack, target, damage)) {
                affected++;
                target.setOnFireFor(Math.max(1, tuning.integer(s("FIRE_TICKS"), 20) / 20));
                if (!tuning.flag(16)) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1), standard);
                if (tuning.flag(1) && count >= 3) target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS, tuning.integer(s("STATUS_DURATION_TICKS"), 60), 0), standard);
                if (tuning.flag(2) && pulse % tuning.integer(s("COUNT"), 4) == 0) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0), standard);
                }
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.PULSE,
                        target, 1, damage);
            }
        }
        trim(state.pulseCounts, 32);
        return affected;
    }

    private static int sunfireLanding(ServerWorld world, BattleStandardEntity standard,
                                       StandardState state, Phase4AbilityTuning tuning) {
        double radius = tuning.get(s("LANDING_RADIUS"), 1);
        int cap = tuning.integer(s("LANDING_TARGET_CAP"), 64);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(standard.spellScalingOwner, "damage"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.sunfire.damageScaling,
                Config.uniqueEffects.sunfire.spellScaling);
        double landing = tuning.get(s("LANDING_DAMAGE_MULTIPLIER"), 3);
        int affected = 0;
        for (LivingEntity target : targets(world, standard, standard.ownerEntity, radius, cap, true)) {
            if (deal(world, standard.ownerEntity, state.stack, target, base * (float) landing)) {
                target.setOnFireFor(Math.max(1, tuning.integer(s("FIRE_TICKS"), 20) / 20));
                affected++;
            }
        }
        return affected;
    }

    private static void sunfireSupportPulse(ServerWorld world, BattleStandardEntity standard,
                                             StandardState state, Phase4AbilityTuning tuning) {
        if (tuning.flag(8)) return;
        double radius = tuning.get(s("SUPPORT_RADIUS"), 6);
        List<LivingEntity> allies = targets(world, standard, standard.ownerEntity, radius,
                tuning.integer(s("SUPPORT_TARGET_CAP"), 16), false);
        float heal = HelperMethods.abilityScaledDamage(SpellScalingComponents.component(standard.spellScalingOwner, "healing"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.sunfire.healScaling,
                Config.uniqueEffects.sunfire.spellScalingHeal) * (float) tuning.get(s("HEAL_MULTIPLIER"), 1);
        int affected = 0;
        for (LivingEntity ally : allies) {
            float applied = tuning.flag(512) && ally == standard.ownerEntity ? heal * .5F : heal;
            if (!tuning.flag(1024)) ally.heal(applied);
            if (tuning.flag(512)) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0), standard);
            } else {
                int duration = tuning.integer(s("STATUS_DURATION_TICKS"), 90);
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration,
                        tuning.flag(1024) ? 2 : 1), standard);
            }
            if (tuning.flag(64)) ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0), standard);
            if (tuning.flag(1024)) Phase4PassiveManager.supportSunfireAlly(ally,
                    tuning.integer(s("DURATION_TICKS"), 60), tuning.integer(s("FIRE_TICKS"), 40), world.getTime());
            if (tuning.flag(128) && ally.getHealth() / ally.getMaxHealth() < .35F
                    && state.allyLocks.getOrDefault(ally.getUuid(), 0L) <= world.getTime()) {
                ally.setAbsorptionAmount(Math.max(ally.getAbsorptionAmount(), 4));
                state.allyLocks.put(ally.getUuid(), world.getTime() + 200);
            }
            if (tuning.flag(32) && state.cleanseLocks.getOrDefault(ally.getUuid(), 0L) <= world.getTime()) {
                removeHarmful(ally);
                state.cleanseLocks.put(ally.getUuid(), world.getTime() + 160);
            }
            affected++;
        }
        trim(state.allyLocks, 32);
        trim(state.cleanseLocks, 32);
        if (tuning.flag(256) && affected >= 3 && !state.supportRefunded) {
            state.supportRefunded = true;
            reduceCooldown(state.ownerId, 40);
        }
        UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.SUPPORT,
                null, affected, heal);
    }

    private static int harbingerHostilePulse(ServerWorld world, BattleStandardDarkEntity standard,
                                              StandardState state, Phase4AbilityTuning tuning) {
        if (tuning.flag(1024)) return 0;
        double radius = tuning.get(s("RADIUS"), 6);
        List<LivingEntity> targets = targets(world, standard, standard.ownerEntity, radius,
                tuning.integer(s("TARGET_CAP"), 32), true);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("harbinger"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.harbinger.damageScaling,
                Config.uniqueEffects.harbinger.spellScaling);
        int pulse = ++state.pulses;
        int affected = 0;
        for (LivingEntity target : targets) {
            int count = state.pulseCounts.merge(target.getUuid(), 1, Integer::sum);
            double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1);
            if (tuning.flag(2) && target.squaredDistanceTo(standard) <= 4) {
                multiplier *= tuning.get(s("NEAR_DAMAGE_MULTIPLIER"), 1.2);
            }
            if (tuning.flag(4) && pulse % 5 == 0) multiplier *= tuning.get(s("CYCLE_DAMAGE_MULTIPLIER"), 1.4);
            if (tuning.flag(32768) && target.getHealth() / target.getMaxHealth() < .25F) {
                multiplier *= tuning.get(s("LOW_HEALTH_DAMAGE_MULTIPLIER"), 1.2);
            }
            if (tuning.flag(131072) && Phase4PassiveManager.isExecutionOmen(standard.ownerEntity, target)) {
                multiplier *= tuning.get(s("WEAKENED_DAMAGE_MULTIPLIER"), 1.4);
            }
            if (tuning.flag(65536) && target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                multiplier *= tuning.get(s("WEAKENED_DAMAGE_MULTIPLIER"), .8);
            }
            float damage = base * (float) multiplier;
            if (deal(world, standard.ownerEntity, state.stack, target, damage)) {
                affected++;
                if (tuning.flag(1) && count >= 3) target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS, tuning.integer(s("STATUS_DURATION_TICKS"), 80), 0), standard);
                if (tuning.flag(65536)) target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS, tuning.integer(s("STATUS_DURATION_TICKS"), 120), 0), standard);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), standard);
                double pull = tuning.get(s("PULL_STRENGTH"), .25);
                if ((tuning.flag(4) && pulse % 5 == 0) || tuning.flag(16)) pull = 1.5;
                if (target.distanceTo(standard) > radius - 1 || tuning.flag(16) || pulse % 5 == 0) {
                    pull(target, standard.getPos(), pull);
                }
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.PULSE,
                        target, 1, damage);
            }
        }
        trim(state.pulseCounts, 32);
        return affected;
    }

    private static int harbingerLanding(ServerWorld world, BattleStandardDarkEntity standard,
                                         StandardState state, Phase4AbilityTuning tuning) {
        double radius = tuning.get(s("LANDING_RADIUS"), 1);
        int cap = tuning.integer(s("LANDING_TARGET_CAP"), 64);
        float base = HelperMethods.abilityScaledDamage(SpellScalingComponents.id("harbinger"),
                standard.ownerEntity, state.stack, Config.uniqueEffects.harbinger.damageScaling,
                Config.uniqueEffects.harbinger.spellScaling);
        int affected = 0;
        for (LivingEntity target : targets(world, standard, standard.ownerEntity, radius, cap, true)) {
            if (deal(world, standard.ownerEntity, state.stack, target,
                    base * (float) tuning.get(s("LANDING_DAMAGE_MULTIPLIER"), 3))) affected++;
        }
        return affected;
    }

    private static void harbingerSupportPulse(ServerWorld world, BattleStandardDarkEntity standard,
                                               StandardState state, Phase4AbilityTuning tuning) {
        if (tuning.flag(8) || tuning.flag(2048)) return;
        List<LivingEntity> allies = targets(world, standard, standard.ownerEntity,
                tuning.get(s("SUPPORT_RADIUS"), 6), tuning.integer(s("SUPPORT_TARGET_CAP"), 16), false);
        int affected = 0;
        for (LivingEntity ally : allies) {
            int amplifier = tuning.flag(1024) ? (ally == standard.ownerEntity ? 1 : 3) : 2;
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                    tuning.integer(s("STATUS_DURATION_TICKS"), 90), amplifier), standard);
            if (tuning.flag(64) || tuning.flag(1024)) ally.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, tuning.integer(s("DURATION_TICKS"), 80), tuning.flag(1024) ? 1 : 0), standard);
            Phase4PassiveManager.supportHarbingerAlly(standard.ownerEntity, ally, tuning, world.getTime());
            affected++;
        }
        if (tuning.flag(256) && affected >= 3 && !state.supportRefunded) {
            state.supportRefunded = true;
            reduceCooldown(state.ownerId, 40);
        }
        UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase4UniqueAbilities.SUPPORT,
                null, affected, 0);
    }

    private static void harbingerOwnerAura(BattleStandardDarkEntity standard, Phase4AbilityTuning tuning) {
        double range = tuning.flag(2048) ? 6 : tuning.get(s("RANGE"), 3);
        if (standard.ownerEntity.squaredDistanceTo(standard) <= range * range && standard.age % 20 == 0) {
            int amplifier = tuning.flag(2048) ? 2 : 0;
            standard.ownerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 30, amplifier), standard);
            Phase4PassiveManager.setHarbingerOwnerBonus(standard.ownerEntity, tuning.flag(2048) ? .25F : 0,
                    standard.getWorld().getTime() + 30);
        }
    }

    private static void moveSunfire(BattleStandardEntity standard, Phase4AbilityTuning tuning) {
        if (!tuning.flag(8) || !standard.isOnGround()) return;
        moveToward(standard, standard.ownerEntity.getPos(), tuning.get(s("MOVEMENT_SPEED"), .35));
    }

    private static void moveHarbinger(ServerWorld world, BattleStandardDarkEntity standard,
                                       Phase4AbilityTuning tuning) {
        if (!tuning.flag(8) || !standard.isOnGround()) return;
        List<LivingEntity> targets = targets(world, standard, standard.ownerEntity,
                tuning.get(s("RANGE"), 12), 1, true);
        if (!targets.isEmpty()) moveToward(standard, targets.getFirst().getPos(), tuning.get(s("MOVEMENT_SPEED"), .3));
    }

    private static void moveToward(LivingEntity entity, Vec3d target, double speed) {
        Vec3d delta = target.subtract(entity.getPos());
        Vec3d horizontal = new Vec3d(delta.x, 0, delta.z);
        if (horizontal.lengthSquared() > .01) {
            Vec3d velocity = horizontal.normalize().multiply(speed);
            entity.setVelocity(velocity.x, entity.getVelocity().y, velocity.z);
            entity.velocityModified = true;
        }
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity origin, LivingEntity owner,
                                               double radius, int cap, boolean hostile) {
        Box box = new Box(origin.getX() + radius, origin.getY() + radius / 3, origin.getZ() + radius,
                origin.getX() - radius, origin.getY() - radius / 3, origin.getZ() - radius);
        return world.getOtherEntities(origin, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(entity -> (!hostile || entity != owner) && !(entity instanceof BattleStandardEntity)
                        && !(entity instanceof BattleStandardDarkEntity))
                .filter(entity -> hostile ? HelperMethods.checkAbilityTarget(entity, owner)
                        : !HelperMethods.checkFriendlyFire(entity, owner))
                .sorted(Comparator.comparingDouble((LivingEntity entity) -> entity.squaredDistanceTo(origin))
                        .thenComparing(entity -> entity.getUuid().toString()))
                .limit(Math.clamp(cap, 0, 64)).toList();
    }

    private static boolean deal(ServerWorld world, LivingEntity owner, ItemStack stack,
                                LivingEntity target, float amount) {
        DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
        return target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, amount));
    }

    private static void pull(LivingEntity target, Vec3d center, double strength) {
        Vec3d delta = center.subtract(target.getPos());
        if (delta.lengthSquared() < .001) return;
        Vec3d velocity = delta.normalize().multiply(strength);
        target.setVelocity(velocity.x, Math.clamp(velocity.y, -.4, .4), velocity.z);
        target.velocityModified = true;
    }

    private static void removeHarmful(LivingEntity ally) {
        for (StatusEffectInstance instance : new ArrayList<>(ally.getStatusEffects())) {
            RegistryEntry<StatusEffect> type = instance.getEffectType();
            if (!type.value().isBeneficial() && !type.equals(EffectRegistry.getReference(EffectRegistry.BATTLE_FATIGUE))) {
                ally.removeStatusEffect(type);
                return;
            }
        }
    }

    private static StandardState ownerState(UUID ownerId, boolean sunfire) {
        return ACTIVE.values().stream().filter(state -> state.ownerId.equals(ownerId) && state.sunfire == sunfire
                        && !state.execution.isTerminal())
                .min(Comparator.comparing(state -> state.entityId.toString())).orElse(null);
    }

    private static void terminate(UniqueAbilityExecution execution, boolean finish, int hits) {
        if (execution == null || execution.isTerminal()) return;
        if (finish) UniqueAbilityApi.finish(execution, Phase4UniqueAbilities.FINISH, hits);
        else UniqueAbilityApi.cancel(execution);
    }

    private static <T> void trim(Map<UUID, T> map, int cap) {
        if (map.size() <= cap) return;
        map.keySet().stream().sorted(Comparator.comparing(UUID::toString)).limit(map.size() - cap)
                .toList().forEach(map::remove);
    }

    private static Phase4AbilityTuning.Setting s(String name) {
        return Phase4AbilityTuning.Setting.valueOf(name);
    }

    private static final class StandardState {
        private final UUID ownerId;
        private final UUID entityId;
        private final boolean sunfire;
        private final UniqueAbilityExecution execution;
        private final ItemStack stack;
        private final Map<UUID, Integer> pulseCounts = new HashMap<>();
        private final Map<UUID, Long> allyLocks = new HashMap<>();
        private final Map<UUID, Long> cleanseLocks = new HashMap<>();
        private boolean landed;
        private boolean supportRefunded;
        private int pulses;
        private int hits;
        private int refunded;

        private StandardState(UUID ownerId, UUID entityId, boolean sunfire,
                              UniqueAbilityExecution execution, ItemStack stack) {
            this.ownerId = ownerId;
            this.entityId = entityId;
            this.sunfire = sunfire;
            this.execution = execution;
            this.stack = stack.copy();
        }
    }
}
