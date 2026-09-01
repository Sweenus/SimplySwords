package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MjolnirStormManager {

    public static final int MODE_FORKED_BOLT = 1 << 5;
    public static final int MODE_ENDLESS_SQUALL = 1 << 7;
    public static final int MODE_STORMSTRIDE = 1 << 9;
    public static final int MODE_HAMMERFALL = 1 << 10;
    public static final int MODE_CHARGED_ADVANCE = 1 << 11;
    public static final int MODE_LIGHTNING_RECALL = 1 << 12;
    public static final int MODE_THUNDER_WAKE = 1 << 13;
    public static final int MODE_SKYBOUND = 1 << 14;
    public static final int MODE_FLASH_STEP = 1 << 15;
    public static final int MODE_RIDE_THE_BOLT = 1 << 16;
    public static final int MODE_STORM_ANCHOR = 1 << 17;
    public static final int MODE_STATIC_SHELL = 1 << 18;
    public static final int MODE_CONDUCTIVE_GUARD = 1 << 19;
    public static final int MODE_THUNDER_REBUKE = 1 << 20;
    public static final int MODE_FINAL_SHELTER = 1 << 22;
    public static final int MODE_AEGIS_OF_THUNDER = 1 << 25;
    public static final int MODE_WRATH_OF_THUNDER = 1 << 26;

    private static final ChainLightningVisualManager.LightningVisualSettings SKY_BOLT_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0x65DFFF, 10, 0.075F, 6);
    private static final ChainLightningVisualManager.LightningVisualSettings CONDUCTIVE_INDICATOR_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0x8CEBFF, 3, 0.025F, 1);
    private static final ChainLightningVisualManager.LightningVisualSettings THUNDERCLAP_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0xB6F4FF, 8, 0.055F, 4);
    private static final int AURA_INTERVAL = 5;
    private static final int LOCAL_RING_LIFETIME = 4;
    private static final int FINAL_RING_LIFETIME = 7;
    private static final int ANCHOR_BUFF_TICKS = 20;
    private static final Map<ServerWorld, Map<UUID, ActiveStorm>> ACTIVE_STORMS = new HashMap<>();
    private static final Map<ServerWorld, List<ThunderclapRing>> ACTIVE_RINGS = new HashMap<>();

    private MjolnirStormManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        List<ThunderclapRing> rings = ACTIVE_RINGS.get(world);
        return (storms != null && !storms.isEmpty()) || (rings != null && !rings.isEmpty());
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.MJOLNIR.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || isActive(context.world(), context.actor())) {
            return false;
        }

        if (context.actor() instanceof PlayerEntity) {
            return true;
        }

        LivingEntity target = context.target();
        return isValidTarget(context.world(), context.actor(), context.sourcePlayer(), target);
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        long now = world.getTime();
        UniqueAbilityExecution execution = Phase6CombatManager.beginActive(
                Phase6UniqueAbilities.MJOLNIR_STORM, context, Config.uniqueEffects.mjolnir.cooldown);
        UniqueAbilityApi.start(execution);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        int duration = resolveDuration(tuning);
        float boltDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                context.stack(),
                Config.uniqueEffects.mjolnir.damageScaling,
                Config.uniqueEffects.mjolnir.spellScaling
        );
        float conductiveBurstDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                context.stack(),
                Config.uniqueEffects.mjolnir.conductiveBurstDamageScaling,
                Config.uniqueEffects.mjolnir.conductiveBurstSpellScaling
        );
        float finalThunderclapDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                context.stack(),
                Config.uniqueEffects.mjolnir.finalThunderclapDamageScaling,
                Config.uniqueEffects.mjolnir.finalThunderclapSpellScaling
        );

        boltDamage *= (float) scoped(tuning, s("MJOLNIR_BOLT_DAMAGE_MULTIPLIER"), s("DAMAGE_MULTIPLIER"), 1);
        finalThunderclapDamage *= (float) scoped(tuning,
                s("MJOLNIR_FINAL_DAMAGE_MULTIPLIER"), s("FINAL_DAMAGE_MULTIPLIER"), 1);
        ActiveStorm storm = new ActiveStorm(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.stack().copy(),
                now,
                now,
                now + duration,
                boltDamage,
                conductiveBurstDamage,
                finalThunderclapDamage,
                tuning,
                execution
        );
        if (tuning.flag(MODE_STORM_ANCHOR)) {
            storm.anchor = actor.getPos();
        }
        storm.lastPosition = actor.getPos();
        storm.recallArmedAt = now + tuning.integer(s("MJOLNIR_RECALL_ARM_TICKS"), 60);
        ACTIVE_STORMS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), storm);
        MjolnirCombatManager.onStormStarted(actor, tuning);
        if (grantsDefensiveBuffs(tuning)) {
            float shell = (float) tuning.get(s("MJOLNIR_SHELL_ABSORPTION"), 0);
            if (tuning.flag(MODE_STATIC_SHELL) && shell > 0) {
                Phase4AbsorptionTracker.grant(actor, shell,
                        tuning.integer(s("MJOLNIR_SHELL_DURATION_TICKS"), 80), shell);
            }
        }
        int speedDuration = scopedInt(tuning, s("MJOLNIR_SPEED_DURATION_TICKS"), s("STATUS_DURATION_TICKS"), 0);
        if (tuning.flag(MODE_STORMSTRIDE) && speedDuration > 0) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, speedDuration,
                    tuning.integer(s("MJOLNIR_SPEED_AMPLIFIER"), 0)), actor);
        }
        spawnActivationEffects(world, actor);
        return true;
    }

    // Lightning Recall: an early release of the finale, reached through the secondary action so the
    // weapon cooldown that is already running cannot gate it.
    public static boolean tryEarlyRelease(ServerWorld world, LivingEntity actor) {
        if (world == null || actor == null) {
            return false;
        }
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        ActiveStorm storm = storms == null ? null : storms.get(actor.getUuid());
        if (storm == null
                || storm.recallUsed
                || storm.finishing
                || !storm.tuning.flag(MODE_LIGHTNING_RECALL)
                || world.getTime() < storm.recallArmedAt) {
            return false;
        }
        storm.recallUsed = true;
        storm.finishing = true;
        storm.nextFinalBoltAt = world.getTime();
        beginFinalSequence(world, actor, storm);
        return true;
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack,
                                  LivingEntity reportedAttacker, LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.MJOLNIR.get())
                || reportedAttacker == null
                || target == null
                || !target.isAlive()
                || target.getWorld() != world) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        LivingEntity sourceOwner = delegated == null ? null : delegated.owner();
        if (!isValidTarget(world, actor, sourceOwner, target)) {
            return;
        }

        UniqueAbilityExecution execution = Phase6CombatManager.beginPassive(
                Phase6UniqueAbilities.MJOLNIR_STORM, world, stack, actor, target);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        applyConductive(world, actor, target, tuning);
        UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, 1);
    }

    public static void tick(ServerWorld world) {
        tickStorms(world);
        tickRings(world);
    }

    public static void spawnConductiveIndicator(ServerWorld world, LivingEntity target) {
        if (world == null || target == null || !target.isAlive()) {
            return;
        }

        double radius = Math.clamp(Math.max(0.25, target.getWidth() * 0.55), 0.25, 0.8);
        double startAngle = world.random.nextDouble() * Math.PI * 2.0;
        spawnConductiveJump(world, target, radius, startAngle);
        if (world.random.nextInt(4) == 0) {
            spawnConductiveJump(world, target, radius, startAngle + Math.PI);
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) {
            return;
        }
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.remove(world);
        if (storms != null) {
            storms.values().forEach(MjolnirStormManager::abandon);
        }
        ACTIVE_RINGS.remove(world);
    }

    public static void clearAll() {
        ACTIVE_STORMS.values().forEach(storms -> storms.values().forEach(MjolnirStormManager::abandon));
        ACTIVE_STORMS.clear();
        ACTIVE_RINGS.clear();
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        for (Map.Entry<ServerWorld, Map<UUID, ActiveStorm>> entry : ACTIVE_STORMS.entrySet()) {
            ActiveStorm storm = entry.getValue().remove(actor.getUuid());
            if (storm != null) {
                abandon(storm);
            }
        }
        ACTIVE_STORMS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private static void abandon(ActiveStorm storm) {
        UniqueAbilityApi.finish(storm.execution, Phase6UniqueAbilities.FINISH, storm.distinctTargets.size());
    }

    private static void tickStorms(ServerWorld world) {
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveStorm> iterator = storms.values().iterator();
        while (iterator.hasNext()) {
            ActiveStorm storm = iterator.next();
            LivingEntity actor = resolveLiving(world, storm.actorId);
            LivingEntity sourceOwner = resolveLiving(world, storm.sourceOwnerId);
            if (actor == null || (storm.sourceOwnerId != null && sourceOwner == null)) {
                abandon(storm);
                iterator.remove();
                continue;
            }

            Vec3d center = center(storm, actor);
            if (storm.anchor != null && !holdsAnchor(storm, actor)) {
                abandon(storm);
                iterator.remove();
                continue;
            }
            if (storm.anchor != null) {
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                        ANCHOR_BUFF_TICKS, 0, false, false, true), actor);
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                        ANCHOR_BUFF_TICKS, 0, false, false, true), actor);
            }

            if ((now - storm.startedAt) % AURA_INTERVAL == 0L) {
                spawnAuraEffects(world, actor, center);
            }

            tickHammerfall(world, actor, sourceOwner, storm, center, now);
            tickThunderWake(world, actor, sourceOwner, storm, now);
            tickChargedAdvance(actor, storm, center, now);

            if (!storm.finishing && now >= storm.expiresAt) {
                storm.finishing = true;
                storm.nextFinalBoltAt = now;
                beginFinalSequence(world, actor, storm);
            }

            if (!storm.finishing) {
                if (now >= storm.nextPulseAt) {
                    strikePulse(world, actor, sourceOwner, storm);
                    storm.nextPulseAt = now + pulseInterval(storm.tuning, Config.uniqueEffects.mjolnir.frequency);
                }
                continue;
            }

            int finalBoltCount = finalBoltCount(storm.tuning, Config.uniqueEffects.mjolnir.finalBoltCount);
            if (storm.finalBoltsReleased < finalBoltCount && now >= storm.nextFinalBoltAt) {
                strikePulse(world, actor, sourceOwner, storm);
                storm.finalBoltsReleased++;
                storm.nextFinalBoltAt = now + Math.max(1, Config.uniqueEffects.mjolnir.finalBoltInterval);
            }
            if (storm.finalBoltsReleased >= finalBoltCount) {
                releaseFinalThunderclap(world, actor, sourceOwner, storm);
                UniqueAbilityApi.finish(storm.execution, Phase6UniqueAbilities.FINISH, storm.distinctTargets.size());
                iterator.remove();
            }
        }

        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static boolean holdsAnchor(ActiveStorm storm, LivingEntity actor) {
        double radius = Math.max(0.5, storm.tuning.get(s("MJOLNIR_ANCHOR_RADIUS"), 3));
        Vec3d delta = actor.getPos().subtract(storm.anchor).multiply(1.0, 0.0, 1.0);
        return delta.horizontalLengthSquared() <= radius * radius;
    }

    private static void beginFinalSequence(ServerWorld world, LivingEntity actor, ActiveStorm storm) {
        if (!storm.tuning.flag(MODE_FINAL_SHELTER)) {
            return;
        }
        int boltCount = finalBoltCount(storm.tuning, Config.uniqueEffects.mjolnir.finalBoltCount);
        int span = boltCount * Math.max(1, Config.uniqueEffects.mjolnir.finalBoltInterval) + 20;
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, span,
                storm.tuning.integer(s("MJOLNIR_SHELTER_AMPLIFIER"), 1), false, false, true), actor);
    }

    private static void tickHammerfall(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                       ActiveStorm storm, Vec3d center, long now) {
        double radius = stormRadius(storm.tuning);
        Set<UUID> present = new HashSet<>();
        for (LivingEntity candidate : candidates(world, actor, sourceOwner, center, radius, 0)) {
            present.add(candidate.getUuid());
            if (!storm.tuning.flag(MODE_HAMMERFALL)
                    || storm.insideStorm.contains(candidate.getUuid())
                    || now < storm.entryReadyAt) {
                continue;
            }
            storm.entryReadyAt = now + storm.tuning.integer(s("MJOLNIR_ENTRY_LOCKOUT_TICKS"), 40);
            float damage = storm.boltDamage
                    * (float) storm.tuning.get(s("MJOLNIR_ENTRY_DAMAGE_MULTIPLIER"), 0.3);
            Vec3d impact = candidate.getPos().add(0.0, Math.max(0.45, candidate.getHeight() * 0.5), 0.0);
            double burst = Math.max(0.1, storm.tuning.get(s("MJOLNIR_ENTRY_RADIUS"), 2.5));
            damageArea(world, actor, sourceOwner, storm.stack, impact, burst, damage, 0, 0, 0);
            addRing(world, new Vec3d(impact.x, candidate.getY(), impact.z), burst, LOCAL_RING_LIFETIME, false);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z,
                    18, 0.4, 0.28, 0.4, 0.1);
            world.playSound(null, candidate.getBlockPos(),
                    SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get(),
                    candidate.getSoundCategory(), 0.5F, 1.05F);
        }
        storm.insideStorm.clear();
        storm.insideStorm.addAll(present);
    }

    private static void tickThunderWake(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                        ActiveStorm storm, long now) {
        if (!storm.tuning.flag(MODE_THUNDER_WAKE) || !actor.isSprinting() || now < storm.wakeReadyAt) {
            return;
        }
        double radius = Math.max(0.1, storm.tuning.get(s("MJOLNIR_WAKE_RADIUS"), 2));
        int cap = storm.tuning.integer(s("MJOLNIR_WAKE_TARGET_CAP"), 4);
        List<LivingEntity> targets = candidates(world, actor, sourceOwner, actor.getPos(), radius, cap);
        if (targets.isEmpty()) {
            return;
        }
        storm.wakeReadyAt = now + storm.tuning.integer(s("MJOLNIR_WAKE_LOCKOUT_TICKS"), 20);
        float damage = storm.boltDamage * (float) storm.tuning.get(s("MJOLNIR_WAKE_DAMAGE_MULTIPLIER"), 0.2);
        for (LivingEntity target : targets) {
            damageTarget(world, actor, storm.stack, target, damage);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.25, 0.3, 0.25, 0.08);
        }
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_01.get(),
                actor.getSoundCategory(), 0.42F, 1.2F);
    }

    private static void tickChargedAdvance(LivingEntity actor, ActiveStorm storm, Vec3d center, long now) {
        Vec3d position = actor.getPos();
        Vec3d previous = storm.lastPosition == null ? position : storm.lastPosition;
        storm.lastPosition = position;
        if (!storm.tuning.flag(MODE_CHARGED_ADVANCE)) {
            return;
        }
        Vec3d delta = position.subtract(previous).multiply(1.0, 0.0, 1.0);
        if (!isInsideCylinder(actor, center, stormRadius(storm.tuning),
                Math.max(1.0, stormRadius(storm.tuning) * 0.5))) {
            return;
        }
        storm.advanceDistance += delta.horizontalLength();
        double required = Math.max(0.5, storm.tuning.get(s("MJOLNIR_ADVANCE_DISTANCE"), 8));
        if (storm.advanceDistance < required) {
            return;
        }
        storm.advanceDistance = 0;
        MjolnirCombatManager.armChargedAdvance(actor,
                storm.tuning.get(s("MJOLNIR_ADVANCE_DAMAGE_MULTIPLIER"), 1.15), now);
    }

    private static void tickRings(ServerWorld world) {
        List<ThunderclapRing> rings = ACTIVE_RINGS.get(world);
        if (rings == null || rings.isEmpty()) {
            return;
        }

        Iterator<ThunderclapRing> iterator = rings.iterator();
        while (iterator.hasNext()) {
            ThunderclapRing ring = iterator.next();
            double progress = (ring.age + 1.0) / ring.lifetime;
            double radius = ring.radius * progress;
            int points = Math.max(16, (int) Math.ceil(radius * 11.0));
            double rotation = ring.age * 0.24;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points + rotation;
                double x = ring.center.x + Math.cos(angle) * radius;
                double z = ring.center.z + Math.sin(angle) * radius;
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, ring.center.y + 0.12, z,
                        1, 0.015, 0.025, 0.015, ring.finalRing ? 0.025 : 0.015);
                if ((i + ring.age) % (ring.finalRing ? 2 : 4) == 0) {
                    world.spawnParticles(ParticleTypes.CLOUD, x, ring.center.y + 0.04, z,
                            1, 0.025, 0.015, 0.025, 0.012);
                }
            }
            ring.age++;
            if (ring.age >= ring.lifetime) {
                iterator.remove();
            }
        }

        if (rings.isEmpty()) {
            ACTIVE_RINGS.remove(world);
        }
    }

    private static void strikePulse(ServerWorld world, LivingEntity actor,
                                    LivingEntity sourceOwner, ActiveStorm storm) {
        LivingEntity target = selectTarget(world, actor, sourceOwner, storm);
        if (target == null) {
            storm.lockedTarget = null;
            spawnAmbientBolt(world, actor, center(storm, actor));
            return;
        }

        storm.pulseCount++;
        storm.struckThisCycle.add(target.getUuid());
        storm.distinctTargets.add(target.getUuid());
        boolean conductive = target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM));
        if (conductive) {
            storm.conductiveTargets.add(target.getUuid());
            MjolnirCombatManager.rememberConductive(actor, target, world.getTime());
        }
        Vec3d impact = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0);
        spawnSkyBolt(world, impact);
        spawnBoltImpactEffects(world, target);

        if (conductive) {
            target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM));
        }

        damageTarget(world, actor, storm.stack, target, storm.boltDamage);
        UniqueAbilityApi.emit(storm.execution, UniqueAbilityPhase.HIT, Phase6UniqueAbilities.HIT,
                target, 1, storm.boltDamage);
        applyBoltRewards(world, actor, storm, target);
        if (conductive) {
            releaseConductiveBurst(world, actor, sourceOwner, storm, impact);
        }
        releaseFork(world, actor, sourceOwner, storm, target);
    }

    private static void applyBoltRewards(ServerWorld world, LivingEntity actor,
                                         ActiveStorm storm, LivingEntity target) {
        if (storm.tuning.flag(MODE_SKYBOUND)) {
            int duration = storm.tuning.integer(s("MJOLNIR_SKYBOUND_DURATION_TICKS"), 40);
            if (duration > 0) {
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration,
                        storm.tuning.integer(s("MJOLNIR_SKYBOUND_AMPLIFIER"), 1)), actor);
                MjolnirCombatManager.grantFallProtection(actor, world.getTime() + duration);
            }
        }
        if (!storm.tuning.flag(MODE_RIDE_THE_BOLT)) {
            return;
        }
        storm.lockedTarget = target.getUuid();
        Vec3d side = target.getPos().subtract(actor.getPos()).normalize().multiply(-1.2);
        actor.requestTeleport(target.getX() + side.x, target.getY(), target.getZ() + side.z);
        int resistance = storm.tuning.integer(s("MJOLNIR_RIDE_RESISTANCE_TICKS"), 20);
        if (resistance > 0) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistance, 0), actor);
        }
    }

    private static void releaseFork(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                    ActiveStorm storm, LivingEntity struck) {
        int every = storm.tuning.integer(s("MJOLNIR_FORK_COUNT"), 0);
        if (!storm.tuning.flag(MODE_FORKED_BOLT) || every <= 0 || storm.pulseCount % every != 0) {
            return;
        }
        double range = Math.max(0.1, storm.tuning.get(s("MJOLNIR_FORK_RANGE"), 5));
        LivingEntity fork = null;
        for (LivingEntity candidate : candidates(world, actor, sourceOwner, struck.getPos(), range, 0)) {
            if (candidate != struck
                    && candidate.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM))) {
                fork = candidate;
                break;
            }
        }
        if (fork == null) {
            return;
        }
        storm.distinctTargets.add(fork.getUuid());
        storm.conductiveTargets.add(fork.getUuid());
        MjolnirCombatManager.rememberConductive(actor, fork, world.getTime());
        Vec3d impact = fork.getPos().add(0.0, Math.max(0.45, fork.getHeight() * 0.58), 0.0);
        ChainLightningVisualManager.spawnBolt(world,
                struck.getPos().add(0.0, Math.max(0.45, struck.getHeight() * 0.58), 0.0),
                impact, SKY_BOLT_SETTINGS);
        spawnBoltImpactEffects(world, fork);
        fork.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM));
        float damage = storm.boltDamage * (float) storm.tuning.get(s("MJOLNIR_FORK_DAMAGE_MULTIPLIER"), 0.6);
        damageTarget(world, actor, storm.stack, fork, damage);
        UniqueAbilityApi.emit(storm.execution, UniqueAbilityPhase.HIT, Phase6UniqueAbilities.HIT,
                fork, 1, damage);
        releaseConductiveBurst(world, actor, sourceOwner, storm, impact);
    }

    private static double stormRadius(Phase6AbilityTuning tuning) {
        return stormRadius(tuning, Config.uniqueEffects.mjolnir.radius);
    }

    public static double stormRadius(Phase6AbilityTuning tuning, double configured) {
        if (tuning.has(s("RADIUS"))) {
            return Math.max(0.5, tuning.get(s("RADIUS"), configured));
        }
        double base = Math.max(0.5, configured) + tuning.get(s("MJOLNIR_STORM_RADIUS_BONUS"), 0);
        return Math.max(0.5, base * tuning.get(s("MJOLNIR_STORM_RADIUS_MULTIPLIER"), 1));
    }

    public static double finalRadius(Phase6AbilityTuning tuning, double configured) {
        return tuning.has(s("RADIUS"))
                ? tuning.get(s("RADIUS"), configured)
                : Math.max(0.1, configured) + tuning.get(s("MJOLNIR_FINAL_RADIUS_BONUS"), 0);
    }

    private static int resolveDuration(Phase6AbilityTuning tuning) {
        return resolveDuration(tuning, Config.uniqueEffects.mjolnir.duration);
    }

    public static int resolveDuration(Phase6AbilityTuning tuning, int configuredDuration) {
        int configured = Math.max(0, configuredDuration);
        if (tuning.has(s("MJOLNIR_DURATION_TICKS"))) {
            return tuning.integer(s("MJOLNIR_DURATION_TICKS"), configured);
        }
        double base = tuning.has(s("DURATION_TICKS"))
                ? tuning.get(s("DURATION_TICKS"), configured)
                : configured + tuning.get(s("MJOLNIR_DURATION_BONUS_TICKS"), 0);
        return (int) Math.round(base * tuning.get(s("MJOLNIR_DURATION_MULTIPLIER"), 1));
    }

    public static int conductiveDuration(Phase6AbilityTuning tuning, int configuredDuration) {
        int configured = Math.max(1, configuredDuration);
        if (tuning.has(s("MJOLNIR_CONDUCTIVE_DURATION_TICKS"))) {
            return tuning.integer(s("MJOLNIR_CONDUCTIVE_DURATION_TICKS"), configured);
        }
        return tuning.has(s("STATUS_DURATION_TICKS"))
                ? tuning.integer(s("STATUS_DURATION_TICKS"), configured)
                : configured + tuning.integer(s("MJOLNIR_CONDUCTIVE_BONUS_TICKS"), 0);
    }

    public static float finaleDamage(Phase6AbilityTuning tuning, float base, int conductiveTargets) {
        return base * (1 + Math.min(
                scopedInt(tuning, s("MJOLNIR_FINALE_TARGET_CAP"), s("STACK_CAP"), 0), conductiveTargets)
                * (float) scoped(tuning, s("MJOLNIR_FINALE_PER_TARGET_MULTIPLIER"),
                s("PER_STACK_MULTIPLIER"), 0));
    }

    public static int finalBoltCount(Phase6AbilityTuning tuning, int configured) {
        return scopedInt(tuning, s("MJOLNIR_FINAL_BOLT_COUNT"), s("COUNT"), Math.max(0, configured));
    }

    public static int pulseInterval(Phase6AbilityTuning tuning, int configured) {
        return scopedInt(tuning, s("MJOLNIR_PULSE_INTERVAL_TICKS"), s("INTERVAL_TICKS"),
                Math.max(1, configured));
    }

    public static boolean suppressesFinalClap(Phase6AbilityTuning tuning) {
        return tuning.flag(MODE_ENDLESS_SQUALL);
    }

    public static boolean grantsDefensiveBuffs(Phase6AbilityTuning tuning) {
        return !tuning.flag(MODE_WRATH_OF_THUNDER);
    }

    private static Vec3d center(ActiveStorm storm, LivingEntity actor) {
        return storm.anchor == null ? actor.getPos() : storm.anchor;
    }

    private static List<LivingEntity> candidates(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                                 Vec3d center, double radius, int cap) {
        double verticalRadius = Math.max(1.0, radius * 0.5);
        Box box = new Box(
                center.x - radius, center.y - verticalRadius, center.z - radius,
                center.x + radius, center.y + verticalRadius, center.z + radius
        );
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(world, actor, sourceOwner, target)
                        && isInsideCylinder(target, center, radius, verticalRadius)
        ));
        targets.sort(Comparator
                .comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                .thenComparing(target -> target.getUuid().toString()));
        return cap > 0 && targets.size() > cap ? targets.subList(0, cap) : targets;
    }

    private static LivingEntity selectTarget(ServerWorld world, LivingEntity actor,
                                             LivingEntity sourceOwner, ActiveStorm storm) {
        double radius = stormRadius(storm.tuning);
        Vec3d center = center(storm, actor);
        int cap = scopedInt(storm.tuning, s("MJOLNIR_STORM_TARGET_CAP"), s("TARGET_CAP"), 0);
        List<LivingEntity> targets = candidates(world, actor, sourceOwner, center, radius, cap);
        if (targets.isEmpty()) {
            storm.struckThisCycle.clear();
            return null;
        }

        if (storm.tuning.flag(MODE_RIDE_THE_BOLT) && storm.lockedTarget != null) {
            for (LivingEntity target : targets) {
                if (target.getUuid().equals(storm.lockedTarget)) {
                    return target;
                }
            }
        }

        LivingEntity target = firstEligible(targets, storm.struckThisCycle, true);
        if (target == null) {
            target = firstEligible(targets, storm.struckThisCycle, false);
        }
        if (target == null) {
            storm.struckThisCycle.clear();
            target = firstEligible(targets, storm.struckThisCycle, true);
            if (target == null) {
                target = firstEligible(targets, storm.struckThisCycle, false);
            }
        }
        return target;
    }

    private static LivingEntity firstEligible(List<LivingEntity> targets, Set<UUID> struck,
                                              boolean requireConductive) {
        for (LivingEntity target : targets) {
            if (!struck.contains(target.getUuid())
                    && (!requireConductive
                    || target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM)))) {
                return target;
            }
        }
        return null;
    }

    private static void applyConductive(ServerWorld world, LivingEntity actor, LivingEntity target,
                                        Phase6AbilityTuning tuning) {
        int duration = conductiveDuration(tuning, Config.uniqueEffects.mjolnir.conductiveDuration);
        if (duration <= 0) {
            return;
        }
        target.addStatusEffect(
                new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.STORM),
                        duration,
                        0,
                        false,
                        false,
                        true
                ),
                actor
        );
    }

    private static void releaseConductiveBurst(ServerWorld world, LivingEntity actor,
                                               LivingEntity sourceOwner, ActiveStorm storm,
                                               Vec3d center) {
        double radius = Math.max(0.1, Config.uniqueEffects.mjolnir.conductiveBurstRadius);
        damageArea(
                world,
                actor,
                sourceOwner,
                storm.stack,
                center,
                radius,
                storm.conductiveBurstDamage,
                Math.max(0.0, Config.uniqueEffects.mjolnir.conductiveBurstKnockback)
                        * storm.tuning.get(s("MJOLNIR_BURST_KNOCKBACK_MULTIPLIER"), 1),
                Math.max(0.0, Config.uniqueEffects.mjolnir.conductiveBurstKnockUp)
                        * storm.tuning.get(s("MJOLNIR_BURST_KNOCKUP_MULTIPLIER"), 1),
                0
        );
        addRing(world, new Vec3d(center.x, center.y - 0.35, center.z),
                radius, LOCAL_RING_LIFETIME, false);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                28, 0.5, 0.32, 0.5, 0.12);
        world.playSound(null, center.x, center.y, center.z,
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get(),
                SoundCategory.PLAYERS, 0.62F, 0.88F + world.random.nextFloat() * 0.16F);
    }

    private static void releaseFinalThunderclap(ServerWorld world, LivingEntity actor,
                                                LivingEntity sourceOwner, ActiveStorm storm) {
        if (suppressesFinalClap(storm.tuning)) {
            return;
        }

        Vec3d center = center(storm, actor);
        double radius = finalRadius(storm.tuning, Config.uniqueEffects.mjolnir.finalThunderclapRadius);
        boolean aegis = storm.tuning.flag(MODE_AEGIS_OF_THUNDER);
        if (aegis) {
            grantAegis(actor, storm.tuning);
        } else {
            float damage = finaleDamage(storm.tuning, storm.finalThunderclapDamage,
                    storm.conductiveTargets.size());
            damageArea(
                    world,
                    actor,
                    sourceOwner,
                    storm.stack,
                    center,
                    radius,
                    damage,
                    Math.max(0.0, Config.uniqueEffects.mjolnir.finalThunderclapKnockback)
                            * scoped(storm.tuning, s("MJOLNIR_FINAL_KNOCKBACK_MULTIPLIER"), s("KNOCKBACK"), 1),
                    Math.max(0.0, Config.uniqueEffects.mjolnir.finalThunderclapKnockUp),
                    scopedInt(storm.tuning, s("MJOLNIR_FINAL_TARGET_CAP"), s("TARGET_CAP"), 0)
            );
        }
        addRing(world, center, radius, FINAL_RING_LIFETIME, true);
        spawnThunderclapSpokes(world, center, radius);
        world.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z,
                2, 0.15, 0.1, 0.15, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.35, center.z,
                72, 0.85, 0.45, 0.85, 0.18);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.08, center.z,
                38, 1.1, 0.12, 1.1, 0.11);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 1.0F, 0.72F);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_03.get(),
                actor.getSoundCategory(), 0.9F, 0.68F);
    }

    private static void grantAegis(LivingEntity actor, Phase6AbilityTuning tuning) {
        if (!grantsDefensiveBuffs(tuning)) {
            return;
        }
        int duration = tuning.integer(s("MJOLNIR_AEGIS_DURATION_TICKS"), 120);
        float absorption = (float) tuning.get(s("MJOLNIR_AEGIS_ABSORPTION"), 8);
        if (absorption > 0 && duration > 0) {
            Phase4AbsorptionTracker.grant(actor, absorption, duration, absorption);
        }
        if (duration > 0) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration,
                    tuning.integer(s("MJOLNIR_AEGIS_AMPLIFIER"), 1)), actor);
        }
    }

    private static void damageArea(ServerWorld world, LivingEntity actor,
                                   LivingEntity sourceOwner, ItemStack stack,
                                   Vec3d center, double radius, float damage,
                                   double knockback, double knockUp, int cap) {
        double verticalRadius = Math.max(1.0, radius * 0.65);
        Box box = new Box(
                center.x - radius,
                center.y - verticalRadius,
                center.z - radius,
                center.x + radius,
                center.y + verticalRadius,
                center.z + radius
        );
        Vec3d fallbackDirection = horizontalDirection(actor.getRotationVec(1.0F), actor);
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(world, actor, sourceOwner, target)
                        && isInsideCylinder(target, center, radius, verticalRadius)
        ));
        if (cap > 0 && targets.size() > cap) {
            targets.sort(Comparator
                    .comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                    .thenComparing(target -> target.getUuid().toString()));
            targets = targets.subList(0, cap);
        }
        for (LivingEntity target : targets) {
            if (damageTarget(world, actor, stack, target, damage)) {
                knockAway(target, center, fallbackDirection, knockback, knockUp);
            }
        }
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor,
                                        ItemStack stack, LivingEntity target, float baseDamage) {
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world,
                stack,
                target,
                source,
                Math.max(0.0F, baseDamage)
        );
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage)
        );
        return damaged[0];
    }

    private static boolean isInsideCylinder(LivingEntity target, Vec3d center,
                                            double radius, double verticalRadius) {
        Vec3d targetPos = target.getPos();
        double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
        double horizontalRadius = radius + targetRadius;
        double dx = targetPos.x - center.x;
        double dz = targetPos.z - center.z;
        return dx * dx + dz * dz <= horizontalRadius * horizontalRadius
                && target.getBoundingBox().maxY >= center.y - verticalRadius
                && target.getBoundingBox().minY <= center.y + verticalRadius;
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity target) {
        return actor != null
                && actor.isAlive()
                && actor.getWorld() == world
                && target != null
                && target.isAlive()
                && target != actor
                && target != sourceOwner
                && target.getWorld() == world
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static boolean isActive(ServerWorld world, LivingEntity actor) {
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        return storms != null && storms.containsKey(actor.getUuid());
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living
                : null;
    }

    private static void spawnSkyBolt(ServerWorld world, Vec3d impact) {
        double height = Math.max(1.0, Config.uniqueEffects.mjolnir.skyHeight);
        Vec3d start = impact.add(
                (world.random.nextDouble() - 0.5) * 1.2,
                height,
                (world.random.nextDouble() - 0.5) * 1.2
        );
        ChainLightningVisualManager.spawnBolt(world, start, impact, SKY_BOLT_SETTINGS);
    }

    private static void spawnAmbientBolt(ServerWorld world, LivingEntity actor, Vec3d center) {
        double radius = Math.max(0.5, Config.uniqueEffects.mjolnir.radius);
        double angle = world.random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(world.random.nextDouble()) * radius;
        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        double y = findLocalGroundY(world, x, z, center.y);
        Vec3d impact = new Vec3d(x, y + 0.12, z);
        spawnSkyBolt(world, impact);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z,
                7, 0.18, 0.08, 0.18, 0.05);
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_01.get(),
                SoundCategory.PLAYERS, 0.22F, 1.35F + world.random.nextFloat() * 0.25F);
    }

    private static double findLocalGroundY(ServerWorld world, double x, double z, double referenceY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(referenceY) + 6;
        int minimumY = Math.max(world.getBottomY(), (int) Math.floor(referenceY) - 12);
        for (int y = startY; y >= minimumY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }
        return referenceY;
    }

    private static void spawnConductiveJump(ServerWorld world, LivingEntity target,
                                            double radius, double startAngle) {
        double height = Math.max(0.35, target.getHeight());
        double endAngle = startAngle
                + (0.65 + world.random.nextDouble() * 1.05)
                * (world.random.nextBoolean() ? 1.0 : -1.0);
        double startY = target.getY() + height * (0.2 + world.random.nextDouble() * 0.65);
        double endY = target.getY() + height * (0.2 + world.random.nextDouble() * 0.65);
        Vec3d start = new Vec3d(
                target.getX() + Math.cos(startAngle) * radius,
                startY,
                target.getZ() + Math.sin(startAngle) * radius
        );
        Vec3d end = new Vec3d(
                target.getX() + Math.cos(endAngle) * radius,
                endY,
                target.getZ() + Math.sin(endAngle) * radius
        );
        ChainLightningVisualManager.spawnBolt(
                world,
                start,
                end,
                CONDUCTIVE_INDICATOR_SETTINGS,
                false
        );
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z,
                1, 0.015, 0.015, 0.015, 0.015);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z,
                1, 0.015, 0.015, 0.015, 0.015);
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getPos().add(0.0, Math.max(0.55, actor.getHeight() * 0.55), 0.0);
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2.0 * i / 3.0 + world.random.nextDouble() * 0.35;
            Vec3d start = center.add(Math.cos(angle) * 2.25, 5.5 + i * 0.45, Math.sin(angle) * 2.25);
            ChainLightningVisualManager.spawnBolt(world, start, center, SKY_BOLT_SETTINGS);
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                34, 0.5, 0.65, 0.5, 0.12);
        world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 0.08, actor.getZ(),
                16, 0.55, 0.08, 0.55, 0.06);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                actor.getSoundCategory(), 0.7F, 0.78F);
    }

    private static void spawnAuraEffects(ServerWorld world, LivingEntity actor, Vec3d center) {
        Vec3d high = center.add(0.0, actor.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, high.x, high.y, high.z,
                3, 0.55, 0.55, 0.55, 0.025);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + actor.getHeight() + 2.0, center.z,
                3, 1.7, 0.25, 1.7, 0.015);
    }

    private static void spawnBoltImpactEffects(ServerWorld world, LivingEntity target) {
        Vec3d center = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                16, 0.3, 0.3, 0.3, 0.11);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z,
                7, 0.22, 0.22, 0.22, 0.04);
        world.playSound(null, target.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                target.getSoundCategory(), 0.58F, 0.9F + world.random.nextFloat() * 0.18F);
    }

    private static void spawnThunderclapSpokes(ServerWorld world, Vec3d center, double radius) {
        Vec3d start = center.add(0.0, 0.35, 0.0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vec3d end = center.add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
            ChainLightningVisualManager.spawnBolt(world, start, end, THUNDERCLAP_SETTINGS);
        }
    }

    private static void addRing(ServerWorld world, Vec3d center, double radius,
                                int lifetime, boolean finalRing) {
        ACTIVE_RINGS.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new ThunderclapRing(center, radius, Math.max(1, lifetime), finalRing));
    }

    private static void knockAway(LivingEntity target, Vec3d center, Vec3d fallbackDirection,
                                  double strength, double lift) {
        Vec3d outward = target.getPos().subtract(center).multiply(1.0, 0.0, 1.0);
        if (outward.horizontalLengthSquared() < 0.0001) {
            outward = fallbackDirection;
        }
        if (strength > 0.0 && outward.horizontalLengthSquared() > 0.0001) {
            Vec3d direction = outward.normalize();
            target.takeKnockback(strength, -direction.x, -direction.z);
        }

        double resistance = Math.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE),
                0.0,
                1.0
        );
        if (lift > 0.0 && resistance < 1.0) {
            target.addVelocity(0.0, lift * (1.0 - resistance), 0.0);
            target.velocityModified = true;
        }
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction == null ? Vec3d.ZERO : direction.multiply(1.0, 0.0, 1.0);
        if (horizontal.horizontalLengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return horizontal.normalize();
    }

    private static double scoped(Phase6AbilityTuning tuning, Phase6AbilityTuning.Setting scoped,
                                 Phase6AbilityTuning.Setting legacy, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(legacy, fallback);
    }

    private static int scopedInt(Phase6AbilityTuning tuning, Phase6AbilityTuning.Setting scoped,
                                 Phase6AbilityTuning.Setting legacy, int fallback) {
        return tuning.has(scoped) ? tuning.integer(scoped, fallback) : tuning.integer(legacy, fallback);
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }

    private static final class ActiveStorm {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final ItemStack stack;
        private final long startedAt;
        private final long expiresAt;
        private final float boltDamage;
        private final float conductiveBurstDamage;
        private final float finalThunderclapDamage;
        private final Phase6AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private final Set<UUID> struckThisCycle = new HashSet<>();
        private final Set<UUID> distinctTargets = new HashSet<>();
        private final Set<UUID> conductiveTargets = new HashSet<>();
        private final Set<UUID> insideStorm = new HashSet<>();
        private Vec3d anchor;
        private Vec3d lastPosition;
        private UUID lockedTarget;
        private double advanceDistance;
        private long nextPulseAt;
        private long nextFinalBoltAt;
        private long entryReadyAt;
        private long wakeReadyAt;
        private long recallArmedAt;
        private int pulseCount;
        private int finalBoltsReleased;
        private boolean finishing;
        private boolean recallUsed;

        private ActiveStorm(UUID actorId, UUID sourceOwnerId, ItemStack stack,
                            long startedAt, long nextPulseAt, long expiresAt,
                            float boltDamage, float conductiveBurstDamage,
                            float finalThunderclapDamage, Phase6AbilityTuning tuning,
                            UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.stack = stack;
            this.startedAt = startedAt;
            this.nextPulseAt = nextPulseAt;
            this.expiresAt = expiresAt;
            this.boltDamage = boltDamage;
            this.conductiveBurstDamage = conductiveBurstDamage;
            this.finalThunderclapDamage = finalThunderclapDamage;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private static final class ThunderclapRing {
        private final Vec3d center;
        private final double radius;
        private final int lifetime;
        private final boolean finalRing;
        private int age;

        private ThunderclapRing(Vec3d center, double radius, int lifetime, boolean finalRing) {
            this.center = center;
            this.radius = radius;
            this.lifetime = lifetime;
            this.finalRing = finalRing;
        }
    }
}
