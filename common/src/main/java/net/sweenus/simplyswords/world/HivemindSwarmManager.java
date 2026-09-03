package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase7UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ParticlesRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.BleedHelper;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class HivemindSwarmManager {

    private static final double STING_RANGE = 0.8;
    private static final double MOVE_SPEED = 0.36;
    private static final double ATTACK_PASS_MIN_SPEED = 0.32;
    private static final double ATTACK_PASS_MAX_SPEED = 0.68;
    private static final double ATTACK_PASS_MIN_DISTANCE = 1.55;
    private static final double ATTACK_PASS_MAX_DISTANCE = 2.45;
    private static final double ATTACK_PASS_ANGLE_VARIATION = Math.toRadians(42.0);
    private static final double ATTACK_PASS_VERTICAL_VARIATION = 0.42;
    private static final double TURN_LINEUP_SPEED = 0.28;
    private static final double TURN_LINEUP_DISTANCE = 3.6;
    private static final double TURN_LINEUP_ANGLE = Math.toRadians(95.0);
    private static final double TURN_LINEUP_COMPLETE_DISTANCE = 1.45;
    private static final double PASS_COMPLETE_DISTANCE = 0.45;
    private static final int DIVE_DELAY_MIN_TICKS = 4;
    private static final int DIVE_DELAY_MAX_TICKS = 22;
    private static final Map<UUID, Long> GUARD_RECHARGE = new HashMap<>();
    private static final Map<UUID, FocusState> SWARM_FOCUS = new HashMap<>();
    private static final Map<UUID, Integer> ROYAL_STINGS = new HashMap<>();
    private static final Map<UUID, Long> RETORT_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> WARNING_COOLDOWN = new HashMap<>();
    private static final Map<UUID, LastAttacker> LAST_ATTACKERS = new HashMap<>();

    private HivemindSwarmManager() {
    }

    public static void activate(ServerWorld world, ServerPlayerEntity player) {
        activate(world, player, player);
    }

    public static void activate(ServerWorld world, ServerPlayerEntity player, LivingEntity actor) {
        activate(world, actor, getStingDamage(player));
    }

    public static void activate(ServerWorld world, LivingEntity actor) {
        activate(world, actor, getStingDamage(actor));
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (context == null || context.actor() == null || !context.actor().isAlive()) return false;
        UniqueAbilityExecution execution = Phase7CombatManager.beginActive(
                Phase7UniqueAbilities.HIVEHEART_SWARM, context, Config.uniqueEffects.hiveheart.activeCooldown);
        Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
        boolean spawned = activate(context.world(), context.actor(), getStingDamage(context.actor()), tuning, execution);
        if (spawned && tuning.flag(1 << 18) && !tuning.flag(1 << 26)) {
            int absorption = tuning.integer(Phase7AbilityTuning.Setting.HIVE_WARD_ABSORPTION, 4);
            Phase4AbsorptionTracker.grant(context.actor(), absorption,
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_WARD_DURATION_TICKS, 80), absorption);
        }
        if (!spawned) UniqueAbilityApi.cancel(execution);
        return spawned;
    }

    public static boolean activateBloodFlies(ServerWorld world, LivingEntity actor) {
        return activateBloodFlies(world, actor, Config.uniqueEffects.bloodwake.bloodFlyCount);
    }

    public static boolean activateBloodFlies(ServerWorld world, LivingEntity actor, int requestedCount) {
        return activateBloodFlies(world, actor, requestedCount, 1.0);
    }

    public static boolean activateBloodFlies(ServerWorld world, LivingEntity actor, int requestedCount,
                                             double damageMultiplier) {
        int flyCount = Math.clamp(requestedCount, 1, 12);
        int contacts = Math.max(1, Config.uniqueEffects.bloodwake.bloodFlyContacts);
        long now = world.getTime();
        long expiry = now + Math.max(20, Config.uniqueEffects.bloodwake.bloodFlyLifetime);
        int spawned = 0;
        for (int i = 0; i < flyCount; i++) {
            double angle = MathHelper.TAU * i / flyCount;
            Vec3d spawnPos = actor.getPos().add(Math.cos(angle) * 0.8,
                    1.15 + (i % 3) * 0.13, Math.sin(angle) * 0.8);
            SimplySwordsBeeEntity fly = EntityRegistry.SIMPLYBEEENTITY.get().spawn(world, actor.getBlockPos(), SpawnReason.MOB_SUMMONED);
            if (fly == null) {
                continue;
            }
            fly.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, actor.getYaw(), 0.0F);
            fly.setOwner(actor);
            fly.setSwarmAnchorUuid(actor.getUuid());
            fly.setHivemindSwarmBee(true);
            fly.setBloodwakeFly(true);
            fly.setSwarmStingsRemaining(contacts);
            fly.setSwarmStingDamage((float) Math.max(1.0,
                    HelperMethods.getEntityAttackDamage(actor) * Math.max(0.0, damageMultiplier)));
            fly.setSwarmExpiryTick(expiry);
            fly.setSwarmNextStingTick(now + i % Math.max(1, Config.uniqueEffects.bloodwake.bloodFlyContactInterval));
            fly.setSwarmNextDiveTick(now + randomDiveDelay(world));
            fly.setInvulnerable(true);
            fly.setNoGravity(true);
            fly.setAiDisabled(true);
            playBeeSound(fly, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 0.08F, 0.72F);
            spawned++;
        }
        if (spawned > 0) {
            world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(), actor.getX(), actor.getBodyY(0.6), actor.getZ(), 28, 0.75, 0.45, 0.75, 0.08);
            world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.68F, 0.62F);
        }
        return spawned > 0;
    }

    private static void activate(ServerWorld world, LivingEntity actor, float stingDamage) {
        activate(world, actor, stingDamage, Phase7AbilityTuning.EMPTY, null);
    }

    private static boolean activate(ServerWorld world, LivingEntity actor, float stingDamage,
                                    Phase7AbilityTuning tuning, UniqueAbilityExecution execution) {
        long now = world.getTime();
        int beeCount = swarmCount(Config.uniqueEffects.hiveheart.swarmBeeCount, tuning);
        if (beeCount <= 0 || actor == null || !actor.isAlive()) {
            return false;
        }
        int stings = swarmStings(Config.uniqueEffects.hiveheart.stingsPerBee, tuning);
        long expiryTick = now + swarmDuration(Config.uniqueEffects.hiveheart.swarmLifetime, tuning);
        double radius = swarmRadius(Config.uniqueEffects.hiveheart.swarmRadius, tuning);
        int interval = tuning.integer(Phase7AbilityTuning.Setting.HIVE_SWARM_STING_INTERVAL_TICKS,
                Config.uniqueEffects.hiveheart.stingIntervalTicks);
        int slow = Math.max(0, Config.uniqueEffects.hiveheart.maxSlowAmplifier + tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_SWARM_SLOW_AMPLIFIER_BONUS, 0));
        stingDamage *= (float) swarmDamageMultiplier(tuning);
        int spawned = 0;

        for (int i = 0; i < beeCount; i++) {
            double angle = (Math.PI * 2.0 / beeCount) * i;
            Vec3d spawnPos = actor.getPos().add(Math.cos(angle) * 0.9, 1.3 + (i % 3) * 0.15, Math.sin(angle) * 0.9);
            SimplySwordsBeeEntity bee = EntityRegistry.SIMPLYBEEENTITY.get().spawn(world, actor.getBlockPos(), SpawnReason.MOB_SUMMONED);
            if (bee == null) {
                continue;
            }
            bee.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, actor.getYaw(), 0.0F);
            bee.setOwner(actor);
            bee.setSwarmAnchorUuid(actor.getUuid());
            bee.setHivemindSwarmBee(true);
            bee.setSwarmStingsRemaining(stings);
            bee.setSwarmStingDamage(stingDamage);
            bee.setSwarmExpiryTick(expiryTick);
            bee.setSwarmNextStingTick(now + i % Math.max(1, interval));
            bee.setMasterySwarmRadius(radius);
            bee.setMasteryStingInterval(interval);
            bee.setMasterySlowAmplifier(slow);
            bee.setMasteryMode(tuning.integer(Phase7AbilityTuning.Setting.MODE, 0));
            bee.setMasteryGuardDrone(tuning.flag(1 << 19) && !tuning.flag(1 << 26) && i == 0);
            bee.setMasterySearchCap(tuning.integer(Phase7AbilityTuning.Setting.HIVE_SWARM_SEARCH_CAP, 0));
            bee.configureMasterySwarmCombat(
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_FOCUS_REQUIRED_STINGS, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_FOCUS_DAMAGE_MULTIPLIER, 1),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_FOCUS_DURATION_TICKS, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_GUARD_RANGE, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_GUARD_INCOMING_MULTIPLIER, 1),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_GUARD_LOCKOUT_TICKS, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_WARNING_RADIUS, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_WARNING_DURATION_TICKS, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_WARNING_LOCKOUT_TICKS, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_RETORT_DAMAGE_MULTIPLIER, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_RETORT_LOCKOUT_TICKS, 0));
            bee.configureMasteryRoyalGuard(
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_RALLY_STING_COUNT, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_RALLY_RESISTANCE_TICKS, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_ESCORT_BEE_COUNT, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_ESCORT_SPEED_MULTIPLIER, 1),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_SAVE_HEALTH_THRESHOLD, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_SAVE_ABSORPTION_PER_BEE, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_SAVE_ABSORPTION_CAP, 0),
                    tuning.get(Phase7AbilityTuning.Setting.HIVE_PHALANX_RANGE, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_PHALANX_BEE_COUNT, 0),
                    tuning.integer(Phase7AbilityTuning.Setting.HIVE_PHALANX_RESISTANCE_AMPLIFIER, 0));
            bee.setSwarmNextDiveTick(now + randomDiveDelay(world));
            bee.setInvulnerable(true);
            bee.setNoGravity(true);
            bee.setAiDisabled(true);
            playBeeSound(bee, i % 3 == 0 ? SoundEvents.ENTITY_BEE_POLLINATE : SoundEvents.ENTITY_BEE_LOOP, 0.08F, 1.35F);
            spawned++;
        }

        world.spawnParticles(ParticleTypes.FALLING_HONEY, actor.getX(), actor.getBodyY(0.6), actor.getZ(), 18, 0.55, 0.35, 0.55, 0.04);
        if (spawned > 0 && execution != null) {
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase7UniqueAbilities.PULSE,
                    null, spawned, stingDamage);
        }
        return spawned > 0;
    }

    static int swarmCount(int configuredCount, Phase7AbilityTuning tuning) {
        int count = Math.max(0, configuredCount + tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_SWARM_COUNT_BONUS, 0));
        int cap = tuning.integer(Phase7AbilityTuning.Setting.HIVE_SWARM_COUNT_CAP, 0);
        if (cap > 0) count = Math.min(count, cap);
        if (tuning.flag(1 << 16)) count = tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_CLOUD_COUNT, count);
        if (tuning.flag(1 << 17)) count = tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_HUNT_COUNT, count);
        return Math.max(0, count);
    }

    static int swarmDuration(int configuredDuration, Phase7AbilityTuning tuning) {
        double duration = Math.max(20, configuredDuration + tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_SWARM_DURATION_BONUS_TICKS, 0));
        if (tuning.flag(1 << 16)) duration *= tuning.get(
                Phase7AbilityTuning.Setting.HIVE_CLOUD_DURATION_MULTIPLIER, 1);
        if (tuning.flag(1 << 17)) duration = tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_HUNT_DURATION_TICKS, (int) Math.round(duration));
        return Math.max(20, (int) Math.round(duration));
    }

    static int swarmStings(int configuredStings, Phase7AbilityTuning tuning) {
        int stings = tuning.integer(Phase7AbilityTuning.Setting.HIVE_SWARM_STING_COUNT,
                configuredStings);
        if (tuning.flag(1 << 17)) stings = tuning.integer(
                Phase7AbilityTuning.Setting.HIVE_HUNT_STING_COUNT, stings);
        return Math.max(1, stings);
    }

    static double swarmRadius(double configuredRadius, Phase7AbilityTuning tuning) {
        double radius = Math.max(1, configuredRadius + tuning.get(
                Phase7AbilityTuning.Setting.HIVE_SWARM_RADIUS_BONUS, 0));
        if (tuning.flag(1 << 16)) radius = tuning.get(
                Phase7AbilityTuning.Setting.HIVE_CLOUD_RADIUS, radius);
        if (tuning.flag(1 << 17)) radius = tuning.get(
                Phase7AbilityTuning.Setting.HIVE_HUNT_RADIUS, radius);
        if (tuning.flag(1 << 26)) radius = tuning.get(
                Phase7AbilityTuning.Setting.HIVE_VENGEFUL_RANGE, radius);
        return Math.max(1, radius);
    }

    static double swarmDamageMultiplier(Phase7AbilityTuning tuning) {
        double multiplier = 1;
        if (tuning.flag(1 << 16)) multiplier *= tuning.get(
                Phase7AbilityTuning.Setting.HIVE_CLOUD_DAMAGE_MULTIPLIER, 1);
        if (tuning.flag(1 << 17)) multiplier *= tuning.get(
                Phase7AbilityTuning.Setting.HIVE_HUNT_DAMAGE_MULTIPLIER, 1);
        if (tuning.flag(1 << 26)) multiplier *= tuning.get(
                Phase7AbilityTuning.Setting.HIVE_VENGEFUL_DAMAGE_MULTIPLIER, 1);
        return multiplier;
    }

    public static boolean hasActive(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SimplySwordsBeeEntity bee && bee.isHivemindSwarmBee()) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerWorld world) {
        long now = world.getTime();
        GUARD_RECHARGE.entrySet().removeIf(entry -> entry.getValue() <= now);
        RETORT_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        WARNING_COOLDOWN.entrySet().removeIf(entry -> entry.getValue() <= now);
        LAST_ATTACKERS.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        SWARM_FOCUS.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        List<SimplySwordsBeeEntity> bees = getSwarmBees(world);
        if (bees.isEmpty()) {
            return;
        }

        Map<UUID, Integer> targetCounts = new HashMap<>();
        for (SimplySwordsBeeEntity bee : bees) {
            LivingEntity owner = getOwner(world, bee);
            LivingEntity anchor = getAnchor(world, bee, owner);
            LivingEntity target = getTarget(world, bee);
            if (owner == null || anchor == null || !isValidTarget(bee, owner, anchor, target)) {
                if ((bee.getMasteryMode() & (1 << 17)) != 0 && bee.getSwarmTargetUuid() != null) {
                    bee.discard();
                    continue;
                }
                bee.setSwarmTargetUuid(null);
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                continue;
            }

            if (bee.isMasteryGuardDrone()) {
                bee.setSwarmTargetUuid(null);
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                hoverNearAnchor(bee, anchor);
                continue;
            }
            targetCounts.merge(target.getUuid(), 1, Integer::sum);
        }

        for (SimplySwordsBeeEntity bee : bees) {
            LivingEntity owner = getOwner(world, bee);
            if (owner == null) {
                bee.discard();
                continue;
            }
            LivingEntity anchor = getAnchor(world, bee, owner);
            if (anchor == null) {
                bee.discard();
                continue;
            }

            LivingEntity target = getTarget(world, bee);
            if (!isValidTarget(bee, owner, anchor, target)) {
                if ((bee.getMasteryMode() & (1 << 17)) != 0 && bee.getSwarmTargetUuid() != null) {
                    bee.discard();
                    continue;
                }
                if (target != null) {
                    targetCounts.computeIfPresent(target.getUuid(), (ignored, count) -> Math.max(0, count - 1));
                }
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                target = selectTarget(world, bee, owner, anchor, targetCounts);
                bee.setSwarmTargetUuid(target == null ? null : target.getUuid());
                if (target != null) {
                    targetCounts.merge(target.getUuid(), 1, Integer::sum);
                }
            }

            if (target == null) {
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                hoverNearAnchor(bee, anchor);
                continue;
            }

            moveTowardTarget(world, bee, target);
            trySting(world, owner, bee, target);
        }

        applySlowness(world, targetCounts);
        applyRoyalGuard(world, bees);
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || !source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)) return amount;
        long now = world.getTime();
        if (now < GUARD_RECHARGE.getOrDefault(target.getUuid(), 0L)) return amount;
        for (SimplySwordsBeeEntity bee : getSwarmBees(world)) {
            if (!bee.isBloodwakeFly() && target.getUuid().equals(bee.getOwnerUuid())
                    && bee.isMasteryGuardDrone()
                    && (bee.getMasteryMode() & (1 << 26)) == 0
                    && bee.squaredDistanceTo(target) <= bee.getMasteryGuardRange()
                    * bee.getMasteryGuardRange()) {
                GUARD_RECHARGE.put(target.getUuid(), now + bee.getMasteryGuardLockout());
                return amount * bee.getMasteryGuardMultiplier();
            }
        }
        return amount;
    }

    public static void onOwnerDamaged(LivingEntity owner, DamageSource source) {
        if (!(owner.getWorld() instanceof ServerWorld world)
                || !(source.getAttacker() instanceof LivingEntity attacker)) return;
        LAST_ATTACKERS.put(owner.getUuid(), new LastAttacker(attacker.getUuid(), world.getTime() + 200));
        if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)
                || world.getTime() < RETORT_COOLDOWN.getOrDefault(owner.getUuid(), 0L)) return;
        for (SimplySwordsBeeEntity bee : getSwarmBees(world)) {
            if (!bee.isBloodwakeFly() && owner.getUuid().equals(bee.getOwnerUuid())
                    && (bee.getMasteryMode() & (1 << 21)) != 0
                    && (bee.getMasteryMode() & (1 << 26)) == 0) {
                RETORT_COOLDOWN.put(owner.getUuid(), world.getTime() + bee.getMasteryRetortLockout());
                attacker.damage(world.getDamageSources().indirectMagic(owner, owner),
                        bee.getSwarmStingDamage() * bee.getMasteryRetortMultiplier());
                return;
            }
        }
    }

    private static void applyRoyalGuard(ServerWorld world, List<SimplySwordsBeeEntity> bees) {
        if (world.getTime() % 10L != 0L) return;
        Map<UUID, List<SimplySwordsBeeEntity>> byOwner = new HashMap<>();
        for (SimplySwordsBeeEntity bee : bees) {
            if (!bee.isBloodwakeFly() && bee.getOwnerUuid() != null) {
                byOwner.computeIfAbsent(bee.getOwnerUuid(), ignored -> new ArrayList<>()).add(bee);
            }
        }
        for (Map.Entry<UUID, List<SimplySwordsBeeEntity>> entry : byOwner.entrySet()) {
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity owner)) continue;
            int mode = entry.getValue().getFirst().getMasteryMode();
            if ((mode & (1 << 26)) != 0) continue;
            SimplySwordsBeeEntity sample = entry.getValue().getFirst();
            if ((mode & (1 << 23)) != 0 && entry.getValue().size() >= sample.getMasteryEscortCount()) {
                Phase7CombatManager.applyHiveEscort(world, owner, 15,
                        sample.getMasteryEscortSpeedMultiplier());
            }
            if ((mode & (1 << 25)) != 0 && entry.getValue().size() >= sample.getMasteryPhalanxCount()) {
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15,
                        sample.getMasteryPhalanxAmplifier(), false, false, true));
            }
            if ((mode & (1 << 20)) != 0
                    && world.getTime() >= WARNING_COOLDOWN.getOrDefault(owner.getUuid(), 0L)) {
                List<LivingEntity> hostiles = world.getEntitiesByClass(LivingEntity.class,
                        owner.getBoundingBox().expand(sample.getMasteryWarningRadius()),
                        candidate -> HelperMethods.checkAbilityTarget(candidate, owner));
                if (!hostiles.isEmpty()) {
                    WARNING_COOLDOWN.put(owner.getUuid(), world.getTime() + sample.getMasteryWarningLockout());
                }
                for (LivingEntity hostile : hostiles) {
                    hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            sample.getMasteryWarningDuration(), 0,
                            false, true, true), owner);
                }
            }
            if ((mode & (1 << 24)) != 0 && owner.getHealth() / owner.getMaxHealth()
                    < sample.getMasterySaveThreshold()) {
                owner.setAbsorptionAmount(Math.min(sample.getMasterySaveCap(), owner.getAbsorptionAmount()
                        + entry.getValue().size() * sample.getMasterySaveAbsorption()));
                entry.getValue().forEach(Entity::discard);
            }
        }
    }

    private static List<SimplySwordsBeeEntity> getSwarmBees(ServerWorld world) {
        List<SimplySwordsBeeEntity> bees = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SimplySwordsBeeEntity bee && bee.isHivemindSwarmBee() && bee.isAlive()) {
                bees.add(bee);
            }
        }
        return bees;
    }

    private static float getStingDamage(LivingEntity actor) {
        return HelperMethods.abilityScaledDamage("nature", actor,
                actor == null ? ItemStack.EMPTY : actor.getMainHandStack(),
                (float) Config.uniqueEffects.hiveheart.stingDamageScaling,
                (float) Config.uniqueEffects.hiveheart.stingSpellScaling);
    }

    private static LivingEntity getOwner(ServerWorld world, SimplySwordsBeeEntity bee) {
        UUID ownerUuid = bee.getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (player != null && player.isAlive()) {
            return player;
        }
        Entity entity = world.getEntity(ownerUuid);
        return entity instanceof LivingEntity livingEntity && livingEntity.isAlive() ? livingEntity : null;
    }

    private static LivingEntity getAnchor(ServerWorld world, SimplySwordsBeeEntity bee, LivingEntity owner) {
        UUID anchorUuid = bee.getSwarmAnchorUuid();
        if (anchorUuid == null) {
            return owner;
        }
        Entity entity = world.getEntity(anchorUuid);
        return entity instanceof LivingEntity livingEntity && livingEntity.isAlive() ? livingEntity : null;
    }

    private static LivingEntity getTarget(ServerWorld world, SimplySwordsBeeEntity bee) {
        UUID targetUuid = bee.getSwarmTargetUuid();
        if (targetUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(targetUuid);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private static LivingEntity lastAttacker(ServerWorld world, LivingEntity owner, double range) {
        LastAttacker last = LAST_ATTACKERS.get(owner.getUuid());
        if (last == null || last.expiresAt < world.getTime()) return null;
        Entity entity = world.getEntity(last.attackerId);
        if (!(entity instanceof LivingEntity attacker) || !attacker.isAlive()
                || attacker.squaredDistanceTo(owner) > range * range
                || !HelperMethods.checkAbilityTarget(attacker, owner)) return null;
        return attacker;
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID id = actor.getUuid();
        GUARD_RECHARGE.remove(id);
        RETORT_COOLDOWN.remove(id);
        WARNING_COOLDOWN.remove(id);
        LAST_ATTACKERS.remove(id);
        ROYAL_STINGS.remove(id);
        SWARM_FOCUS.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(id));
    }

    public static void clearAll() {
        GUARD_RECHARGE.clear();
        SWARM_FOCUS.clear();
        ROYAL_STINGS.clear();
        RETORT_COOLDOWN.clear();
        WARNING_COOLDOWN.clear();
        LAST_ATTACKERS.clear();
    }

    private static boolean isValidTarget(SimplySwordsBeeEntity bee, LivingEntity owner, LivingEntity anchor, LivingEntity target) {
        double radius = bee.isBloodwakeFly()
                ? Math.max(1.0, Config.uniqueEffects.bloodwake.targetingRadius)
                : Math.max(1.0, bee.getMasterySwarmRadius() > 0
                ? bee.getMasterySwarmRadius() : Config.uniqueEffects.hiveheart.swarmRadius);
        if (!bee.isBloodwakeFly() && (bee.getMasteryMode() & (1 << 25)) != 0) {
            radius = Math.min(radius, bee.getMasteryPhalanxRange());
        }
        if (!bee.isBloodwakeFly() && (bee.getMasteryMode() & (1 << 26)) != 0
                && target != lastAttacker((ServerWorld) bee.getWorld(), owner, radius)) return false;
        return target != null
                && target.isAlive()
                && target != owner
                && target != anchor
                && target.squaredDistanceTo(anchor) <= radius * radius
                && HelperMethods.checkAbilityTarget(target, owner);
    }

    private static LivingEntity selectTarget(ServerWorld world, SimplySwordsBeeEntity bee, LivingEntity owner, LivingEntity anchor, Map<UUID, Integer> targetCounts) {
        double radius = bee.isBloodwakeFly()
                ? Math.max(1.0, Config.uniqueEffects.bloodwake.targetingRadius)
                : Math.max(1.0, bee.getMasterySwarmRadius() > 0
                ? bee.getMasterySwarmRadius() : Config.uniqueEffects.hiveheart.swarmRadius);
        if (!bee.isBloodwakeFly() && (bee.getMasteryMode() & (1 << 25)) != 0) {
            radius = Math.min(radius, bee.getMasteryPhalanxRange());
        }
        if (!bee.isBloodwakeFly() && (bee.getMasteryMode() & (1 << 26)) != 0) {
            return lastAttacker(world, owner, radius);
        }
        Box searchBox = anchor.getBoundingBox().expand(radius, radius * 0.5, radius);
        LivingEntity selected = null;
        int selectedCount = Integer.MAX_VALUE;
        double selectedDistance = Double.MAX_VALUE;
        int evaluated = 0;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, searchBox, candidate -> isValidTarget(bee, owner, anchor, candidate))) {
            if (bee.getMasterySearchCap() > 0 && evaluated++ >= bee.getMasterySearchCap()) break;
            int count = targetCounts.getOrDefault(candidate.getUuid(), 0);
            double distance = candidate.squaredDistanceTo(anchor);
            if (count < selectedCount || (count == selectedCount && distance < selectedDistance)) {
                selected = candidate;
                selectedCount = count;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private static void hoverNearAnchor(SimplySwordsBeeEntity bee, LivingEntity owner) {
        double angle = (bee.age * 0.19) + (bee.getId() * 0.7);
        Vec3d destination = owner.getPos().add(Math.cos(angle) * 1.4, 1.5 + Math.sin(angle * 0.7) * 0.25, Math.sin(angle) * 1.4);
        moveToward(bee, destination, MOVE_SPEED * 0.7);
    }

    private static void moveTowardTarget(ServerWorld world, SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d lineup = getLineupPos(bee, target);
        if (lineup != null) {
            moveToward(bee, lineup, TURN_LINEUP_SPEED);
            if (bee.getPos().squaredDistanceTo(lineup) <= TURN_LINEUP_COMPLETE_DISTANCE * TURN_LINEUP_COMPLETE_DISTANCE) {
                bee.setSwarmNextDiveTick(world.getTime() + randomDiveDelay(world));
                bee.clearSwarmLineup();
            }
            return;
        }

        if (bee.getSwarmPassExitPos() == null && world.getTime() < bee.getSwarmNextDiveTick()) {
            hoverNearTarget(bee, target);
            return;
        }

        Vec3d destination = getOrCreatePassExit(bee, target);
        moveToward(bee, destination, getAttackPassSpeed(bee, destination));
        if (bee.getPos().squaredDistanceTo(destination) <= PASS_COMPLETE_DISTANCE * PASS_COMPLETE_DISTANCE) {
            createLineupPoint(bee, target, destination);
            bee.clearSwarmPass();
        }
    }

    private static Vec3d getLineupPos(SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d lineup = bee.getSwarmLineupPos();
        UUID lineupTargetUuid = bee.getSwarmLineupTargetUuid();
        if (lineup != null && target.getUuid().equals(lineupTargetUuid)) {
            return lineup;
        }
        if (lineup != null || lineupTargetUuid != null) {
            bee.clearSwarmLineup();
        }
        return null;
    }

    private static void createLineupPoint(SimplySwordsBeeEntity bee, LivingEntity target, Vec3d passExit) {
        Vec3d targetCenter = getTargetCenter(target);
        Vec3d away = passExit.subtract(targetCenter);
        double horizontalLength = Math.sqrt(away.x * away.x + away.z * away.z);
        Vec3d horizontal = horizontalLength < 0.001
                ? new Vec3d(1.0, 0.0, 0.0)
                : new Vec3d(away.x / horizontalLength, 0.0, away.z / horizontalLength);
        double turnSign = ((bee.getId() + bee.age) & 1) == 0 ? 1.0 : -1.0;
        double turnAngle = TURN_LINEUP_ANGLE * turnSign;
        double cos = Math.cos(turnAngle);
        double sin = Math.sin(turnAngle);
        double x = horizontal.x * cos - horizontal.z * sin;
        double z = horizontal.x * sin + horizontal.z * cos;
        double y = Math.sin((bee.age + bee.getId()) * 0.43) * ATTACK_PASS_VERTICAL_VARIATION;
        Vec3d lineup = targetCenter.add(new Vec3d(x, y, z).normalize().multiply(TURN_LINEUP_DISTANCE + Math.max(0.35, target.getWidth() * 0.5)));
        bee.setSwarmLineupTargetUuid(target.getUuid());
        bee.setSwarmLineupPos(lineup);
    }

    private static void hoverNearTarget(SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d targetCenter = getTargetCenter(target);
        double angle = (bee.age * 0.16) + (bee.getId() * 0.91);
        double radius = 2.0 + Math.sin((bee.age + bee.getId()) * 0.23) * 0.35;
        Vec3d destination = targetCenter.add(Math.cos(angle) * radius, 0.35 + Math.sin(angle * 0.7) * 0.25, Math.sin(angle) * radius);
        moveToward(bee, destination, MOVE_SPEED * 0.75);
        if ((bee.age + bee.getId() * 11) % 48 == 0) {
            playBeeSound(bee, SoundEvents.ENTITY_BEE_LOOP, 0.055F, 1.45F);
        }
    }

    private static Vec3d getOrCreatePassExit(SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d existingExit = bee.getSwarmPassExitPos();
        UUID passTargetUuid = bee.getSwarmPassTargetUuid();
        if (existingExit != null && target.getUuid().equals(passTargetUuid)) {
            return existingExit;
        }

        Vec3d targetCenter = getTargetCenter(target);
        Vec3d approach = targetCenter.subtract(bee.getPos());
        if (approach.lengthSquared() < 0.001) {
            double angle = (bee.age * 0.31) + (bee.getId() * 0.9);
            approach = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
        }
        Vec3d direction = getVariedPassDirection(bee, approach);
        Vec3d exit = targetCenter.add(direction.multiply(randomizedPassDistance(bee) + Math.max(0.35, target.getWidth() * 0.5)));
        bee.setSwarmPassTargetUuid(target.getUuid());
        bee.setSwarmPassStartPos(bee.getPos());
        bee.setSwarmPassExitPos(exit);
        bee.setSwarmPassStung(false);
        playBeeSound(bee, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 0.12F, 1.55F);
        return exit;
    }

    private static double getAttackPassSpeed(SimplySwordsBeeEntity bee, Vec3d destination) {
        Vec3d start = bee.getSwarmPassStartPos();
        if (start == null) {
            return ATTACK_PASS_MIN_SPEED;
        }

        double totalDistance = Math.sqrt(start.squaredDistanceTo(destination));
        if (totalDistance < 0.001) {
            return ATTACK_PASS_MIN_SPEED;
        }

        double remainingDistance = Math.sqrt(bee.getPos().squaredDistanceTo(destination));
        double progress = 1.0 - remainingDistance / totalDistance;
        progress = Math.clamp(progress, 0.0, 1.0);
        double ease = Math.sin(progress * Math.PI);
        return ATTACK_PASS_MIN_SPEED + (ATTACK_PASS_MAX_SPEED - ATTACK_PASS_MIN_SPEED) * ease;
    }

    private static int randomDiveDelay(ServerWorld world) {
        return DIVE_DELAY_MIN_TICKS + world.random.nextInt(DIVE_DELAY_MAX_TICKS - DIVE_DELAY_MIN_TICKS + 1);
    }

    private static double randomizedPassDistance(SimplySwordsBeeEntity bee) {
        double seed = Math.sin((bee.age * 0.37) + (bee.getId() * 2.17)) * 0.5 + 0.5;
        return ATTACK_PASS_MIN_DISTANCE + (ATTACK_PASS_MAX_DISTANCE - ATTACK_PASS_MIN_DISTANCE) * seed;
    }

    private static Vec3d getVariedPassDirection(SimplySwordsBeeEntity bee, Vec3d approach) {
        double horizontalLength = Math.sqrt(approach.x * approach.x + approach.z * approach.z);
        Vec3d horizontal = horizontalLength < 0.001
                ? new Vec3d(1.0, 0.0, 0.0)
                : new Vec3d(approach.x / horizontalLength, 0.0, approach.z / horizontalLength);
        double seed = bee.age * 0.73 + bee.getId() * 1.91;
        double angleOffset = Math.sin(seed) * ATTACK_PASS_ANGLE_VARIATION;
        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);
        double x = horizontal.x * cos - horizontal.z * sin;
        double z = horizontal.x * sin + horizontal.z * cos;
        double y = Math.sin(seed * 1.37) * ATTACK_PASS_VERTICAL_VARIATION;
        return new Vec3d(x, y, z).normalize();
    }

    private static Vec3d getTargetCenter(LivingEntity target) {
        return target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.58), 0.0);
    }

    private static void moveToward(SimplySwordsBeeEntity bee, Vec3d destination, double speed) {
        Vec3d delta = destination.subtract(bee.getPos());
        if (delta.lengthSquared() < 0.01) {
            bee.setVelocity(Vec3d.ZERO);
            return;
        }
        Vec3d step = delta.normalize().multiply(Math.min(speed, delta.length()));
        Vec3d nextPos = bee.getPos().add(step);
        double horizontalLength = Math.sqrt(step.x * step.x + step.z * step.z);
        float yaw = horizontalLength < 0.001
                ? bee.getYaw()
                : (float) (Math.atan2(step.z, step.x) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(step.y, horizontalLength) * (180.0F / Math.PI));
        bee.refreshPositionAndAngles(nextPos.x, nextPos.y, nextPos.z, yaw, pitch);
        bee.setYaw(yaw);
        bee.setPitch(pitch);
        bee.setBodyYaw(yaw);
        bee.setHeadYaw(yaw);
        bee.prevBodyYaw = yaw;
        bee.prevHeadYaw = yaw;
        bee.setVelocity(Vec3d.ZERO);
    }

    private static void trySting(ServerWorld world, LivingEntity owner, SimplySwordsBeeEntity bee, LivingEntity target) {
        if (bee.hasSwarmPassStung() || world.getTime() < bee.getSwarmNextStingTick() || bee.getSwarmStingsRemaining() <= 0) {
            return;
        }
        if (!target.getBoundingBox().expand(STING_RANGE).contains(bee.getPos())) {
            return;
        }

        if (bee.isBloodwakeFly()) {
            BleedHelper.apply(target, owner, bee.getSwarmStingDamage());
            bee.decrementSwarmStingsRemaining();
            bee.setSwarmPassStung(true);
            bee.setSwarmNextStingTick(world.getTime() + Math.max(1, Config.uniqueEffects.bloodwake.bloodFlyContactInterval));
            Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
            world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(), pos.x, pos.y, pos.z, 7, 0.22, 0.2, 0.22, 0.07);
            world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 1, 0.12, 0.1, 0.12, 0.01);
            playBeeSound(bee, SoundEvents.ENTITY_BEE_STING, 0.16F, 0.78F);
            if (bee.getSwarmStingsRemaining() <= 0) {
                bee.discard();
            }
            return;
        }

        int iframes = target.timeUntilRegen;
        Vec3d velocity = target.getVelocity();
        target.timeUntilRegen = 0;
        boolean[] damaged = {false};
        DamageSource damageSource = owner.getDamageSources().indirectMagic(owner, owner);
        ItemStack stack = owner.getMainHandStack();
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, bee.getSwarmStingDamage());
        if ((bee.getMasteryMode() & (1 << 15)) != 0) {
            FocusState focus = SWARM_FOCUS.get(target.getUuid());
            if (focus != null && focus.ownerId.equals(owner.getUuid())
                    && focus.stings >= bee.getMasteryFocusStings()
                    && focus.expiresAt >= world.getTime()) damage *= bee.getMasteryFocusMultiplier();
        }
        float finalDamage = damage;
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(damageSource, finalDamage));
        target.timeUntilRegen = iframes;
        target.setVelocity(velocity);
        target.velocityModified = true;
        if (damaged[0]) {
            if ((bee.getMasteryMode() & (1 << 15)) != 0) {
                FocusState current = SWARM_FOCUS.get(target.getUuid());
                int stings = current != null && current.ownerId.equals(owner.getUuid())
                        && current.expiresAt >= world.getTime() ? current.stings + 1 : 1;
                SWARM_FOCUS.put(target.getUuid(), new FocusState(owner.getUuid(), stings,
                        world.getTime() + bee.getMasteryFocusTicks()));
            }
            if ((bee.getMasteryMode() & (1 << 22)) != 0) {
                int stings = ROYAL_STINGS.merge(owner.getUuid(), 1, Integer::sum);
                if (stings >= bee.getMasteryRallyStings()) {
                    ROYAL_STINGS.put(owner.getUuid(), 0);
                    owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                            bee.getMasteryRallyDuration(), 0,
                            false, true, true));
                }
            }
            bee.decrementSwarmStingsRemaining();
            bee.setSwarmPassStung(true);
            bee.setSwarmNextStingTick(world.getTime() + Math.max(1, bee.getMasteryStingInterval() > 0
                    ? bee.getMasteryStingInterval() : Config.uniqueEffects.hiveheart.stingIntervalTicks));
            Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
            world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 2, 0.16, 0.14, 0.16, 0.02);
            world.spawnParticles(ParticleTypes.FALLING_HONEY, pos.x, pos.y, pos.z, 2, 0.18, 0.18, 0.18, 0.015);
            playBeeSound(bee, SoundEvents.ENTITY_BEE_STING, 0.16F, 1.65F);
            if (bee.getSwarmStingsRemaining() <= 0) {
                bee.discard();
            }
        }
    }

    private static void applySlowness(ServerWorld world, Map<UUID, Integer> targetCounts) {
        Map<UUID, Integer> hivemindCounts = new HashMap<>();
        for (SimplySwordsBeeEntity bee : getSwarmBees(world)) {
            if (!bee.isBloodwakeFly() && bee.getSwarmTargetUuid() != null) {
                hivemindCounts.merge(bee.getSwarmTargetUuid(), 1, Integer::sum);
            }
        }
        for (Map.Entry<UUID, Integer> entry : hivemindCounts.entrySet()) {
            int beeCount = entry.getValue();
            if (beeCount <= 0) {
                continue;
            }
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }
            int maximum = getSwarmBees(world).stream()
                    .filter(bee -> !bee.isBloodwakeFly() && entry.getKey().equals(bee.getSwarmTargetUuid()))
                    .mapToInt(bee -> bee.getMasterySlowAmplifier() >= 0
                            ? bee.getMasterySlowAmplifier() : Config.uniqueEffects.hiveheart.maxSlowAmplifier)
                    .max().orElse(Config.uniqueEffects.hiveheart.maxSlowAmplifier);
            int amplifier = Math.clamp(beeCount - 1, 0, Math.max(0, maximum));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 12, amplifier, false, false, true));
        }
    }

    private static void playBeeSound(SimplySwordsBeeEntity bee, SoundEvent sound, float volume, float basePitch) {
        float pitch = basePitch + (bee.getWorld().random.nextFloat() - 0.5F) * 0.35F;
        bee.getWorld().playSound(null, bee.getX(), bee.getY(), bee.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private record FocusState(UUID ownerId, int stings, long expiresAt) {
    }

    private record LastAttacker(UUID attackerId, long expiresAt) {
    }
}
