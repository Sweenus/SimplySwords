package net.sweenus.simplyswords.world;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulPyreVisualEntity;
import net.sweenus.simplyswords.entity.SoulPyreWispEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SoulPyreAbilityManager {

    private static final String FIELD_VISUAL_TAG = "simplyswords_soul_pyre_visual";
    private static final String WISP_VISUAL_TAG = "simplyswords_soul_pyre_wisp";
    private static final int RADIUS_GROWTH_TICKS = 10;
    private static final int WISP_LIFETIME = 30;
    private static final int TRANSMUTED_LAVA_DAMAGE_INTERVAL = 10;
    private static final double WISP_SPEED = 0.78;
    private static final double WISP_IMPACT_DISTANCE = 0.72;
    private static final float[] RADIUS_MILESTONES = {0.25F, 0.50F, 0.75F, 1.0F};
    private static final Identifier KNOCKBACK_RESISTANCE_ID = Identifier.of(
            "simplyswords", "soul_pyre_knockback_resistance");

    private static final Map<ServerWorld, Map<UUID, ActivePyre>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveWisp>> WISPS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAVA_DAMAGE_COOLDOWNS =
            new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LINGERING_KNOCKBACK = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Map<UUID, Long>>> WISP_MARKS = new HashMap<>();

    private SoulPyreAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        List<ActiveWisp> wisps = WISPS.get(world);
        return active != null && !active.isEmpty()
                || wisps != null && !wisps.isEmpty()
                || hasEntries(LAVA_DAMAGE_COOLDOWNS.get(world))
                || hasEntries(LINGERING_KNOCKBACK.get(world))
                || hasEntries(WISP_MARKS.get(world));
    }

    public static boolean hasActiveForActor(ServerWorld world, UUID actorId) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        return active != null && actorId != null && active.containsKey(actorId);
    }

    public static boolean isInTransmutedLava(Entity entity) {
        return findTransmutedLavaContact(entity) != null;
    }

    public static boolean handleTransmutedLavaContact(Entity entity) {
        TransmutedLavaContact contact = findTransmutedLavaContact(entity);
        if (contact == null) {
            return false;
        }

        LivingEntity target = contact.target;
        if (target.isFireImmune()) {
            return true;
        }

        target.setOnFireFor(15.0F);
        long now = contact.world.getTime();
        Map<UUID, Long> cooldowns = LAVA_DAMAGE_COOLDOWNS.computeIfAbsent(
                contact.world,
                ignored -> new HashMap<>()
        );
        if (target.timeUntilRegen > TRANSMUTED_LAVA_DAMAGE_INTERVAL
                || now < cooldowns.getOrDefault(target.getUuid(), Long.MIN_VALUE)) {
            return true;
        }
        cooldowns.put(
                target.getUuid(),
                now + TRANSMUTED_LAVA_DAMAGE_INTERVAL
        );

        LivingEntity attributedOwner = contact.sourceOwner == null
                ? contact.actor
                : contact.sourceOwner;
        DamageSource source = new DamageSource(
                contact.world.getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .entryOf(DamageTypes.LAVA),
                contact.actor,
                attributedOwner
        );
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = target.damage(source, 4.0F)
        );
        if (damaged[0]) {
            target.playSound(
                    SoundEvents.ENTITY_GENERIC_BURN,
                    0.4F,
                    2.0F + target.getRandom().nextFloat() * 0.4F
            );
        }
        return true;
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().isOf(ItemsRegistry.SOULPYRE.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !hasActiveForActor(context.world(), context.actor().getUuid());
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(FireForgeMasteryAbilities.SOUL_PYRE_TETHER,
                context, Config.uniqueEffects.soulpyre.cooldown);
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        int pulseCount = tuning.integer(FireForgeMasteryTuning.Setting.PULSE_COUNT,
                Config.uniqueEffects.soulpyre.pulseCount);
        if (tuning.flag(1 << 7)) pulseCount += tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_DEVOURING_EXTRA_PULSES, 1);
        int duration = Math.max(pulseCount, tunedInteger(tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_TETHER_DURATION_TICKS,
                FireForgeMasteryTuning.Setting.DURATION_TICKS, Config.uniqueEffects.soulpyre.duration));
        int collapseDuration = tunedInteger(tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_COLLAPSE_DURATION_TICKS,
                FireForgeMasteryTuning.Setting.COLLAPSE_DURATION_TICKS,
                Config.uniqueEffects.soulpyre.collapseDuration);
        float maxRadius = (float) Math.max(1.5, tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_MAX_RADIUS, Config.uniqueEffects.soulpyre.radius));
        double configuredStartRadius = tuned(tuning, FireForgeMasteryTuning.Setting.SOULPYRE_START_RADIUS,
                FireForgeMasteryTuning.Setting.RADIUS, Config.uniqueEffects.soulpyre.startingRadius);
        if (tuning.flag(1 << 8)) configuredStartRadius = tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_CLOSED_RADIUS, 4);
        float startRadius = MathHelper.clamp(
                (float) configuredStartRadius,
                1.5F,
                maxRadius
        );
        int pulseTargetCap = Math.max(0, tunedInteger(tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_PULSE_TARGET_CAP,
                FireForgeMasteryTuning.Setting.TARGET_CAP,
                Config.uniqueEffects.soulpyre.pulseTargetCap));
        long now = world.getTime();

        SoulPyreVisualEntity visual = null;
        if (Config.general.enableModernFieldEffects) {
            visual = new SoulPyreVisualEntity(
                    world,
                    actor,
                    maxRadius,
                    Config.uniqueEffects.soulpyre.verticalRange,
                    duration + collapseDuration + 20,
                    world.random.nextInt()
            );
            visual.setRadius(startRadius);
            visual.addCommandTag(FIELD_VISUAL_TAG);
            world.spawnEntity(visual);
        }

        ActivePyre pyre = new ActivePyre(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.stack().copy(),
                visual == null ? null : visual.getUuid(),
                now,
                now + duration,
                pulseCount,
                duration,
                collapseDuration,
                startRadius,
                maxRadius,
                pulseTargetCap,
                HelperMethods.abilityScaledDamage(
                        "soul", actor, context.stack(),
                        Config.uniqueEffects.soulpyre.damageScaling,
                        Config.uniqueEffects.soulpyre.spellScaling)
                        * (float) tuning.get(FireForgeMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                tuning,
                execution
        );
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), pyre);
        applyKnockbackResistance(actor);
        Map<UUID, Long> lingering = LINGERING_KNOCKBACK.get(world);
        if (lingering != null) {
            lingering.remove(actor.getUuid());
            if (lingering.isEmpty()) LINGERING_KNOCKBACK.remove(world);
        }
        actor.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.SOULTETHER),
                duration + 2,
                0,
                false,
                true
        ));
        if (tuning.flag(1 << 20)) {
            StatusEffectInstance previous = actor.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
            pyre.previousFireResistance = previous == null ? null : new StatusEffectInstance(previous);
            int linger = tuning.integer(
                    FireForgeMasteryTuning.Setting.SOULPYRE_FIRE_RESISTANCE_LINGER_TICKS, 40);
            int total = duration + Math.max(0, linger);
            if (previous == null || previous.getAmplifier() == 0 && previous.getDuration() < total) {
                pyre.managedFireResistance = true;
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,
                        total, 0, false, true, true), actor);
            }
        }

        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                actor.getSoundCategory(),
                0.45F,
                0.72F
        );
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                actor.getX(),
                actor.getBodyY(0.45),
                actor.getZ(),
                32,
                1.1,
                0.3,
                1.1,
                0.035
        );
        return true;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)) return amount;
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        ActivePyre pyre = active == null ? null : active.get(target.getUuid());
        if (pyre == null || pyre.collapsing) return amount;
        double reduction = pyre.tuning.flag(1 << 26)
                ? pyre.tuning.get(FireForgeMasteryTuning.Setting.SOULPYRE_FUNERAL_DAMAGE_REDUCTION, 0)
                : tuned(pyre.tuning, FireForgeMasteryTuning.Setting.SOULPYRE_DAMAGE_REDUCTION,
                FireForgeMasteryTuning.Setting.DAMAGE_REDUCTION, .5);
        return amount * (1F - (float) reduction);
    }

    public static boolean tryUndyingDominion(LivingEntity target, DamageSource source) {
        if (target == null || source == null || !(target.getWorld() instanceof ServerWorld world)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        ActivePyre pyre = activePyre(world, target.getUuid());
        if (pyre == null || pyre.collapsing || !pyre.tuning.flag(1 << 25)) return false;
        int threshold = pyre.tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_UNDYING_SOUL_COUNT, 10);
        if (pyre.undyingReserve < threshold) return false;
        pyre.undyingReserve -= threshold;
        target.setHealth(1);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                Math.max(1, pyre.tuning.integer(
                        FireForgeMasteryTuning.Setting.SOULPYRE_UNDYING_RESISTANCE_TICKS, 60)),
                pyre.tuning.integer(
                        FireForgeMasteryTuning.Setting.SOULPYRE_UNDYING_RESISTANCE_AMPLIFIER, 2),
                false, true, true), target);
        cancelPyre(world, target, pyre, world.getTime());
        return true;
    }

    public static void tick(ServerWorld world) {
        tickLavaDamageCooldowns(world);
        tickLingeringKnockback(world);
        pruneWispMarks(world);
        tickPyres(world);
        tickWisps(world);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, ActivePyre> pyres = ACTIVE.remove(world);
        if (pyres != null) {
            for (ActivePyre pyre : pyres.values()) {
                LivingEntity actor = resolveLiving(world, pyre.actorId);
                if (actor != null) finishTetherDefense(world, actor, pyre, false);
                discardVisual(world, pyre.visualId);
                cancelWispPlan(pyre);
                cancelExecution(pyre.execution);
            }
        }
        List<ActiveWisp> wisps = WISPS.remove(world);
        if (wisps != null) {
            for (ActiveWisp wisp : wisps) {
                Entity visual = world.getEntity(wisp.visualId);
                if (visual != null) visual.discard();
                wisp.volley.cancel();
            }
        }
        Map<UUID, Long> lingering = LINGERING_KNOCKBACK.remove(world);
        if (lingering != null) lingering.keySet().forEach(ownerId -> {
            LivingEntity owner = resolveLiving(world, ownerId);
            if (owner != null) removeKnockbackResistance(owner);
        });
        LAVA_DAMAGE_COOLDOWNS.remove(world);
        WISP_MARKS.remove(world);
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>();
        worlds.addAll(ACTIVE.keySet());
        worlds.addAll(WISPS.keySet());
        worlds.addAll(LAVA_DAMAGE_COOLDOWNS.keySet());
        worlds.addAll(LINGERING_KNOCKBACK.keySet());
        worlds.addAll(WISP_MARKS.keySet());
        worlds.forEach(SoulPyreAbilityManager::clear);
        ACTIVE.clear();
        WISPS.clear();
        LAVA_DAMAGE_COOLDOWNS.clear();
        LINGERING_KNOCKBACK.clear();
        WISP_MARKS.clear();
    }

    public static void clear(ServerWorld world, LivingEntity actor) {
        if (world == null || actor == null) return;
        Map<UUID, ActivePyre> pyres = ACTIVE.get(world);
        ActivePyre pyre = pyres == null ? null : pyres.remove(actor.getUuid());
        if (pyre != null) {
            finishTetherDefense(world, actor, pyre, false);
            discardVisual(world, pyre.visualId);
            cancelWispPlan(pyre);
            cancelExecution(pyre.execution);
        }
        if (pyres != null && pyres.isEmpty()) ACTIVE.remove(world);
        List<ActiveWisp> wisps = WISPS.get(world);
        if (wisps != null) wisps.removeIf(wisp -> {
            if (!wisp.volley.actorId.equals(actor.getUuid())) return false;
            Entity visual = world.getEntity(wisp.visualId);
            if (visual != null) visual.discard();
            wisp.volley.cancel();
            return true;
        });
        if (wisps != null && wisps.isEmpty()) WISPS.remove(world);
        Map<UUID, Long> lingering = LINGERING_KNOCKBACK.get(world);
        if (lingering != null) {
            lingering.remove(actor.getUuid());
            if (lingering.isEmpty()) LINGERING_KNOCKBACK.remove(world);
        }
        removeKnockbackResistance(actor);
        Map<UUID, Map<UUID, Long>> marks = WISP_MARKS.get(world);
        if (marks != null) {
            marks.remove(actor.getUuid());
            if (marks.isEmpty()) WISP_MARKS.remove(world);
        }
    }

    private static void tickLavaDamageCooldowns(ServerWorld world) {
        Map<UUID, Long> cooldowns = LAVA_DAMAGE_COOLDOWNS.get(world);
        if (cooldowns == null) {
            return;
        }
        long now = world.getTime();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (cooldowns.isEmpty()) {
            LAVA_DAMAGE_COOLDOWNS.remove(world);
        }
    }

    private static void tickLingeringKnockback(ServerWorld world) {
        Map<UUID, Long> lingering = LINGERING_KNOCKBACK.get(world);
        if (lingering == null) return;
        long now = world.getTime();
        lingering.entrySet().removeIf(entry -> {
            LivingEntity owner = resolveLiving(world, entry.getKey());
            if (owner != null && owner.isAlive() && now < entry.getValue()) return false;
            if (owner != null) removeKnockbackResistance(owner);
            return true;
        });
        if (lingering.isEmpty()) LINGERING_KNOCKBACK.remove(world);
    }

    public static void onDeath(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || source == null) {
            return;
        }
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            clear(world, target);
            return;
        }

        Entity directSource = source.getSource();
        Entity attacker = source.getAttacker();
        ActivePyre selected = null;
        LivingEntity selectedActor = null;
        int bestScore = 0;
        double bestDistance = Double.MAX_VALUE;

        for (ActivePyre pyre : active.values()) {
            if (pyre.collapsing) {
                continue;
            }
            LivingEntity actor = resolveLiving(world, pyre.actorId);
            LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
            if (actor == null
                    || !actor.isAlive()
                    || !isEligibleTarget(actor, sourceOwner, target)
                    || !isInsideField(actor, target, pyre.currentRadius)) {
                continue;
            }

            int score = sourceMatchScore(pyre, directSource, attacker);
            if (score <= 0) {
                continue;
            }
            double distance = actor.squaredDistanceTo(target);
            if (score > bestScore || score == bestScore && distance < bestDistance) {
                selected = pyre;
                selectedActor = actor;
                bestScore = score;
                bestDistance = distance;
            }
        }

        if (selected != null && selectedActor != null) {
            growRadius(world, selectedActor, selected);
            harvestSoul(world, target, selectedActor, selected);
        }
        if (active.containsKey(target.getUuid())) clear(world, target);
    }

    private static void tickPyres(ServerWorld world) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, ActivePyre>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            ActivePyre pyre = iterator.next().getValue();
            LivingEntity actor = resolveLiving(world, pyre.actorId);
            if (actor == null || !actor.isAlive()) {
                discardVisual(world, pyre.visualId);
                if (actor != null) finishTetherDefense(world, actor, pyre, false);
                cancelWispPlan(pyre);
                cancelExecution(pyre.execution);
                iterator.remove();
                continue;
            }

            if (!pyre.cancelled
                    && actor instanceof PlayerEntity
                    && !isWieldingSoulPyre(actor)) {
                cancelPyre(world, actor, pyre, now);
            }

            if (pyre.collapsing) {
                if (tickCollapse(world, actor, pyre, now)) {
                    iterator.remove();
                }
                continue;
            }

            if (!actor.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER))) {
                discardVisual(world, pyre.visualId);
                finishTetherDefense(world, actor, pyre, false);
                cancelWispPlan(pyre);
                cancelExecution(pyre.execution);
                iterator.remove();
                continue;
            }

            maintainNetherWard(world, actor, pyre);
            updateGrowth(world, actor, pyre, now);
            if (pyre.tuning.flag(1 << 8)) {
                while (!pyre.collapsing && now >= pyre.nextClosedPulseTick
                        && pyre.nextClosedPulseTick < pyre.endTick) {
                    performPulse(world, actor, pyre);
                    pyre.pulsesCompleted++;
                    pyre.nextClosedPulseTick += pyre.closedPulseInterval;
                }
            } else {
                while (!pyre.collapsing && pyre.pulsesCompleted < pyre.pulseCount
                        && now >= pulseTick(pyre, pyre.pulsesCompleted + 1)) {
                    boolean finalPulse = pyre.pulsesCompleted == pyre.pulseCount - 1;
                    if (finalPulse) beginCollapse(world, actor, pyre, now);
                    else performPulse(world, actor, pyre);
                    pyre.pulsesCompleted++;
                }
            }

            if (!pyre.collapsing && now >= pyre.endTick) {
                beginCollapse(world, actor, pyre, now);
                pyre.pulsesCompleted = pyre.pulseCount;
            }
        }

        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void cancelPyre(ServerWorld world, LivingEntity actor,
                                   ActivePyre pyre, long now) {
        pyre.cancelled = true;
        pyre.souls = 0;
        cancelWispPlan(pyre);
        pyre.growing = false;
        actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER));
        finishTetherDefense(world, actor, pyre, true);
        cancelExecution(pyre.execution);

        if (!pyre.collapsing) {
            pyre.collapsing = true;
            pyre.collapseStartTick = now;
            pyre.collapseEndTick = now + pyre.collapseDuration;
            pyre.collapseStartRadius = pyre.currentRadius;
            beginVisualCollapse(world, actor, pyre);
        } else {
            updateVisual(world, actor, pyre, pyre.currentRadius);
        }
    }

    private static long pulseTick(ActivePyre pyre, int pulseNumber) {
        return pyre.startTick + Math.round(
                (double) pyre.duration * pulseNumber / pyre.pulseCount);
    }

    private static void performPulse(ServerWorld world, LivingEntity actor,
                                     ActivePyre pyre) {
        int remaining = Math.max(1, pyre.pulseCount - pyre.pulsesCompleted);
        float basePulseDamage = Math.min(
                30.0F,
                Math.max(0.0F, pyre.baseDamage - remaining)
        );
        float soulBonus = Math.max(0.0F, (float) tuned(pyre.tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_PULSE_SOUL_MULTIPLIER,
                FireForgeMasteryTuning.Setting.PER_STACK_MULTIPLIER,
                Config.uniqueEffects.soulpyre.pulseDamageBonusPerSoul));
        float damage = basePulseDamage * (1.0F + pyre.souls * soulBonus);
        if (pyre.tuning.flag(1 << 7)) damage *= (float) pyre.tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_DEVOURING_PULSE_DAMAGE_MULTIPLIER, .75);
        if (pyre.tuning.flag(1 << 8)) damage *= (float) pyre.tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_CLOSED_PULSE_DAMAGE_MULTIPLIER, 1.6);
        pyre.lastPulseDamage = damage;

        actor.heal(Math.max(0.0F, Config.uniqueEffects.soulpyre.heal));
        int pulseNumber = pyre.pulsesCompleted + 1;
        boolean pull = pyre.tuning.flag(1 << 5) && pulseNumber % Math.max(1,
                pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_PULL_INTERVAL, 5)) == 0;
        int hits = damagePulseArea(world, actor, pyre, pyre.currentRadius, damage,
                pyre.pulseTargetCap, pull, true);
        UniqueAbilityApi.emit(pyre.execution, UniqueAbilityPhase.HIT,
                FireForgeMasteryAbilities.PULSE, actor, hits, damage);
        triggerPulseVisual(world, pyre.visualId);
        spawnPulseEffects(
                world,
                actor,
                pyre.currentRadius,
                pyre.pulsesCompleted,
                false
        );
    }

    private static void beginCollapse(ServerWorld world, LivingEntity actor,
                                      ActivePyre pyre, long now) {
        if (pyre.collapsing) {
            return;
        }

        pyre.collapsing = true;
        pyre.collapseStartTick = now;
        pyre.collapseEndTick = now + pyre.collapseDuration;
        pyre.collapseStartRadius = pyre.currentRadius;
        actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER));
        finishTetherDefense(world, actor, pyre, true);

        int requiemSouls = pyre.souls;
        pyre.souls = 0;
        float finalBaseDamage = Math.min(
                30.0F,
                Math.max(0.0F, pyre.baseDamage - 1.0F)
        );
        float soulBonus = Math.max(0.0F, Config.uniqueEffects.soulpyre.requiemDamageBonusPerSoul);
        int requiemCap = pyre.tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_REQUIEM_SOUL_CAP, 10);
        float multiplier = finalSoulMultiplier(requiemSouls, soulBonus,
                pyre.totalSoulsHarvested,
                pyre.tuning.get(FireForgeMasteryTuning.Setting.SOULPYRE_REQUIEM_BONUS_PER_SOUL, .12),
                requiemCap, pyre.tuning.flag(1 << 23));
        float damage = finalBaseDamage * multiplier;
        float finalRadius = pyre.currentRadius;
        int finalTargetCap = pyre.pulseTargetCap;
        if (pyre.tuning.flag(1 << 26)) {
            damage *= (float) pyre.tuning.get(
                    FireForgeMasteryTuning.Setting.SOULPYRE_FUNERAL_DAMAGE_MULTIPLIER, 1.75);
            finalRadius = (float) pyre.tuning.get(
                    FireForgeMasteryTuning.Setting.SOULPYRE_FUNERAL_RADIUS, 8);
            finalTargetCap = pyre.tuning.integer(
                    FireForgeMasteryTuning.Setting.SOULPYRE_FUNERAL_TARGET_CAP, 24);
        }

        int hits = damagePulseArea(world, actor, pyre, finalRadius, damage,
                Math.max(0, finalTargetCap), false, false);
        UniqueAbilityApi.emit(pyre.execution, UniqueAbilityPhase.HIT,
                FireForgeMasteryAbilities.PULSE, actor, hits, damage);
        actor.heal(
                Math.max(0.0F, Config.uniqueEffects.soulpyre.heal)
                        + requiemSouls * Math.max(
                        0.0F,
                        Config.uniqueEffects.soulpyre.requiemHealingPerSoul)
        );
        WispPlan finalPlan = pyre.pendingWispPlan;
        pyre.pendingWispPlan = null;
        if (requiemSouls > 0) {
            if (finalPlan == null) finalPlan = prepareWispPlan(world, actor, pyre);
            if (finalPlan.tuning.flag(1 << 17)) launchSoulLance(
                    world, actor, pyre, requiemSouls, finalRadius, finalPlan);
            else launchWispVolley(world, actor, pyre, requiemSouls, finalRadius, finalPlan);
        } else if (finalPlan != null) {
            cancelExecution(finalPlan.execution);
        }
        if (pyre.tuning.flag(1 << 24) && pyre.totalSoulsHarvested >= pyre.tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_LAST_RITES_SOUL_COUNT, 5)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    pyre.tuning.integer(
                            FireForgeMasteryTuning.Setting.SOULPYRE_LAST_RITES_DURATION_TICKS, 80),
                    0, false, true, true), actor);
        }
        if (pyre.tuning.flag(1 << 22)) {
            pyre.collapsePeriodicDamage = pyre.lastPulseDamage * (float) pyre.tuning.get(
                    FireForgeMasteryTuning.Setting.SOULPYRE_COLLAPSE_DAMAGE_MULTIPLIER, .2);
            pyre.nextCollapseDamageTick = now + pyre.tuning.integer(
                    FireForgeMasteryTuning.Setting.SOULPYRE_COLLAPSE_INTERVAL_TICKS, 20);
        }
        beginVisualCollapse(world, actor, pyre);
        spawnPulseEffects(
                world,
                actor,
                pyre.currentRadius,
                pyre.pulsesCompleted,
                true
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(),
                actor.getSoundCategory(),
                1.15F,
                0.55F
        );
    }

    private static boolean tickCollapse(ServerWorld world, LivingEntity actor,
                                        ActivePyre pyre, long now) {
        float progress = MathHelper.clamp(
                (float) (now - pyre.collapseStartTick)
                        / Math.max(1.0F, pyre.collapseDuration),
                0.0F,
                1.0F
        );
        float eased = progress * progress * (3.0F - 2.0F * progress);
        pyre.currentRadius = MathHelper.lerp(
                eased,
                pyre.collapseStartRadius,
                0.0F
        );
        updateVisual(world, actor, pyre, pyre.currentRadius);
        if (!pyre.cancelled && pyre.collapsePeriodicDamage > 0) {
            int interval = pyre.tuning.integer(
                    FireForgeMasteryTuning.Setting.SOULPYRE_COLLAPSE_INTERVAL_TICKS, 20);
            while (pyre.nextCollapseDamageTick < pyre.collapseEndTick
                    && now >= pyre.nextCollapseDamageTick) {
                int hits = damagePulseArea(world, actor, pyre, pyre.currentRadius,
                        pyre.collapsePeriodicDamage, pyre.pulseTargetCap, false, false);
                UniqueAbilityApi.emit(pyre.execution, UniqueAbilityPhase.HIT,
                        FireForgeMasteryAbilities.PULSE, actor, hits, pyre.collapsePeriodicDamage);
                pyre.nextCollapseDamageTick += Math.max(1, interval);
            }
        }
        if (now < pyre.collapseEndTick) {
            return false;
        }
        discardVisual(world, pyre.visualId);
        if (!pyre.cancelled) {
            UniqueAbilityApi.finish(pyre.execution, FireForgeMasteryAbilities.FINISH, pyre.pulsesCompleted);
        }
        return true;
    }

    private static void growRadius(ServerWorld world, LivingEntity actor,
                                   ActivePyre pyre) {
        if (pyre.tuning.flag(1 << 8)) return;
        float growth = (float) tuned(pyre.tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_RADIUS_GROWTH,
                FireForgeMasteryTuning.Setting.RADIUS_GROWTH,
                Config.uniqueEffects.soulpyre.radiusGrowthPerKill);
        float nextRadius = Math.min(pyre.maxRadius, pyre.targetRadius + growth);
        if (nextRadius <= pyre.targetRadius + 1.0E-4F) {
            return;
        }
        pyre.growthStartRadius = pyre.currentRadius;
        pyre.targetRadius = nextRadius;
        pyre.growthStartTick = world.getTime();
        pyre.growing = true;
        updateVisual(world, actor, pyre, pyre.currentRadius);
    }

    private static void updateGrowth(ServerWorld world, LivingEntity actor,
                                     ActivePyre pyre, long now) {
        if (pyre.growing) {
            float progress = MathHelper.clamp(
                    (float) (now - pyre.growthStartTick) / RADIUS_GROWTH_TICKS,
                    0.0F,
                    1.0F
            );
            float eased = progress * progress * (3.0F - 2.0F * progress);
            pyre.currentRadius = MathHelper.lerp(
                    eased,
                    pyre.growthStartRadius,
                    pyre.targetRadius
            );
            if (progress >= 1.0F) {
                pyre.currentRadius = pyre.targetRadius;
                pyre.growing = false;
            }
            checkRadiusMilestones(world, actor, pyre);
        }
        updateVisual(world, actor, pyre, pyre.currentRadius);
    }

    private static void checkRadiusMilestones(ServerWorld world, LivingEntity actor,
                                              ActivePyre pyre) {
        if (pyre.nextMilestone >= RADIUS_MILESTONES.length
                || pyre.maxRadius <= pyre.startRadius) {
            return;
        }

        int crossedMilestone = -1;
        while (pyre.nextMilestone < RADIUS_MILESTONES.length) {
            float threshold = MathHelper.lerp(
                    RADIUS_MILESTONES[pyre.nextMilestone],
                    pyre.startRadius,
                    pyre.maxRadius
            );
            if (pyre.currentRadius + 1.0E-4F < threshold) {
                break;
            }
            crossedMilestone = pyre.nextMilestone;
            pyre.nextMilestone++;
        }
        if (crossedMilestone >= 0) {
            spawnRadiusMilestoneEffects(
                    world,
                    actor,
                    pyre,
                    crossedMilestone
            );
        }
    }

    private static void harvestSoul(ServerWorld world, LivingEntity target,
                                    LivingEntity actor, ActivePyre pyre) {
        pyre.totalSoulsHarvested++;
        pyre.souls++;
        if (pyre.tuning.flag(1 << 25)) {
            int threshold = pyre.tuning.integer(
                    FireForgeMasteryTuning.Setting.SOULPYRE_UNDYING_SOUL_COUNT, 10);
            pyre.undyingReserve = Math.min(threshold, pyre.undyingReserve + 1);
        }
        if (pyre.tuning.flag(1 << 6) && pyre.totalSoulsHarvested <= pyre.tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_FEAST_KILL_COUNT, 3)) {
            MasteryAbsorptionTracker.grant(actor,
                    (float) pyre.tuning.get(
                            FireForgeMasteryTuning.Setting.SOULPYRE_FEAST_ABSORPTION, 2),
                    pyre.tuning.integer(
                            FireForgeMasteryTuning.Setting.SOULPYRE_FEAST_ABSORPTION_TICKS, 80),
                    (float) pyre.tuning.get(
                            FireForgeMasteryTuning.Setting.SOULPYRE_FEAST_ABSORPTION_CAP, 6));
        }
        if (pyre.tuning.flag(1 << 21)) {
            int step = pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_MANTLE_SOUL_STEP, 5);
            if (isHarvestMilestone(pyre.totalSoulsHarvested, step)) {
                actor.setAbsorptionAmount(boundedAbsorptionAfterGrant(actor.getAbsorptionAmount(),
                        (float) pyre.tuning.get(FireForgeMasteryTuning.Setting.SOULPYRE_MANTLE_ABSORPTION, 2),
                        (float) pyre.tuning.get(FireForgeMasteryTuning.Setting.SOULPYRE_MANTLE_ABSORPTION_CAP, 8)));
            }
        }
        WispPlan plan = pyre.pendingWispPlan;
        if (plan == null) {
            plan = prepareWispPlan(world, actor, pyre);
            pyre.pendingWispPlan = plan;
        }
        int volleySize = effectiveVolleySize(plan.tuning);
        if (!plan.tuning.flag(1 << 17) && pyre.souls >= volleySize) {
            pyre.souls -= volleySize;
            pyre.pendingWispPlan = null;
            launchWispVolley(world, actor, pyre, volleySize, pyre.currentRadius, plan);
            spawnEarlyVolleyEffects(world, actor, pyre.currentRadius);
        }
        updateVisual(world, actor, pyre, pyre.currentRadius);
        spawnHarvestEffects(
                world,
                target,
                actor,
                Math.min(volleySize, pyre.totalSoulsHarvested)
        );
        UniqueAbilityApi.emit(pyre.execution, UniqueAbilityPhase.HIT,
                FireForgeMasteryAbilities.KILL, target, 1, 0);
    }

    private static int damagePulseArea(ServerWorld world, LivingEntity actor,
                                       ActivePyre pyre, float radius, float damage,
                                       int targetCap, boolean pull, boolean binding) {
        LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
        double verticalRange = Math.max(
                2.0,
                Config.uniqueEffects.soulpyre.verticalRange
        );
        Box search = actor.getBoundingBox().expand(radius, verticalRange, radius);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                search,
                EntityPredicates.VALID_LIVING_ENTITY
        ).stream().filter(target -> isInsideField(actor, target, radius)
                        && isValidTarget(actor, sourceOwner, target))
                .sorted(Comparator.comparingDouble(actor::squaredDistanceTo)).toList();
        int hits = 0;
        for (LivingEntity target : targets) {
            if (hits >= targetCap) break;
            if (!isInsideField(actor, target, radius)
                    || !isValidTarget(actor, sourceOwner, target)) {
                continue;
            }
            if (!applyAbilityDamage(
                    world,
                    actor,
                    sourceOwner,
                    pyre.stack,
                    target,
                    damage)) continue;
            hits++;
            UniqueAbilityApi.emit(pyre.execution, UniqueAbilityPhase.HIT,
                    FireForgeMasteryAbilities.HIT, target, 1, damage);
            if (binding) recordBindingPulse(world, actor, pyre, target);
            if (pull) pullTarget(actor, target, pyre.tuning.get(
                    FireForgeMasteryTuning.Setting.SOULPYRE_PULL_STRENGTH, .3));
        }
        return hits;
    }

    private static void launchWispVolley(ServerWorld world, LivingEntity actor,
                                         ActivePyre pyre, int soulCount,
                                         float fieldRadius, WispPlan plan) {
        if (soulCount <= 0) {
            return;
        }

        LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
        UniqueAbilityExecution execution = plan.execution;
        FireForgeMasteryTuning tuning = plan.tuning;
        double searchRadius = Math.max(1.0, fieldRadius + 4.0 + tuned(tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_WISP_RANGE_BONUS,
                FireForgeMasteryTuning.Setting.RANGE, 0));
        int searchCap = tunedInteger(tuning, FireForgeMasteryTuning.Setting.SOULPYRE_WISP_SEARCH_CAP,
                FireForgeMasteryTuning.Setting.SEARCH_CAP, Config.uniqueEffects.soulpyre.wispSearchCap);
        List<LivingEntity> targets = findWispTargets(
                world,
                actor,
                sourceOwner,
                actor.getPos(),
                searchRadius,
                searchCap,
                Set.of()
        );
        float damage = pyre.baseDamage * Math.max(0.0F, Config.uniqueEffects.soulpyre.wispDamageMultiplier)
                * (float) tuned(tuning, FireForgeMasteryTuning.Setting.SOULPYRE_WISP_DAMAGE_MULTIPLIER,
                FireForgeMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        if (tuning.flag(1 << 16)) damage *= (float) tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_LEGION_DAMAGE_MULTIPLIER, .55);
        boolean legion = tuning.flag(1 << 16);
        int perTargetCap = legion ? tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_LEGION_PER_TARGET_CAP, 3) : Integer.MAX_VALUE;
        WispVolley volley = new WispVolley(execution, tuning, pyre.actorId,
                pyre.sourceOwnerId, soulCount, Math.max(1, perTargetCap));
        boolean wailing = tuning.flag(1 << 15) && soulCount >= tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_WAILING_FULL_VOLLEY_COUNT, 6);

        for (int i = 0; i < soulCount; i++) {
            double angle = MathHelper.TAU * i / Math.max(1, soulCount);
            Vec3d start = actor.getPos().add(
                    Math.cos(angle) * 1.2,
                    actor.getHeight() * 0.62 + Math.sin(angle * 2.0) * 0.18,
                    Math.sin(angle) * 1.2
            );
            SoulPyreWispEntity visual = new SoulPyreWispEntity(
                    world,
                    start.x,
                    start.y,
                    start.z,
                    WISP_LIFETIME,
                    world.random.nextInt()
            );
            visual.setVelocity(
                    Math.cos(angle) * 0.18,
                    0.09,
                    Math.sin(angle) * 0.18
            );
            visual.addCommandTag(WISP_VISUAL_TAG);
            if (!world.spawnEntity(visual)) {
                volley.wispFinished();
                continue;
            }
            LivingEntity assigned = targets.isEmpty() ? null : legion
                    ? selectWispTarget(targets, volley.assignments, volley.perTargetCap)
                    : targets.get(i % targets.size());
            if (assigned != null) volley.assignments.merge(assigned.getUuid(), 1, Integer::sum);
            WISPS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                    new ActiveWisp(
                            visual.getUuid(),
                            assigned == null ? null : assigned.getUuid(),
                            pyre.stack.copy(),
                            damage,
                            searchRadius,
                            world.getTime() + WISP_LIFETIME,
                            volley,
                            wailing && i == soulCount - 1,
                            tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_WISP_RETARGET_COUNT, 0),
                            false,
                            1
                    )
            );
        }
    }

    private static void launchSoulLance(ServerWorld world, LivingEntity actor,
                                        ActivePyre pyre, int soulCount, float fieldRadius,
                                        WispPlan plan) {
        if (soulCount <= 0) return;
        UniqueAbilityExecution execution = plan.execution;
        FireForgeMasteryTuning tuning = plan.tuning;
        int soulCap = tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_LANCE_SOUL_CAP, 10);
        int targetCap = tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_LANCE_TARGET_CAP, 6);
        float damage = pyre.baseDamage * lanceDamageMultiplier(soulCount, soulCap, tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_LANCE_DAMAGE_PER_SOUL, .25))
                * (float) tuned(tuning, FireForgeMasteryTuning.Setting.SOULPYRE_WISP_DAMAGE_MULTIPLIER,
                FireForgeMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        double searchRadius = Math.max(1, fieldRadius + 4 + tuned(tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_WISP_RANGE_BONUS,
                FireForgeMasteryTuning.Setting.RANGE, 0));
        LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
        List<LivingEntity> targets = findWispTargets(world, actor, sourceOwner, actor.getPos(), searchRadius,
                tunedInteger(tuning, FireForgeMasteryTuning.Setting.SOULPYRE_WISP_SEARCH_CAP,
                        FireForgeMasteryTuning.Setting.SEARCH_CAP,
                        Config.uniqueEffects.soulpyre.wispSearchCap), Set.of());
        LivingEntity assigned = targets.isEmpty() ? null : targets.getFirst();
        Vec3d start = actor.getPos().add(0, actor.getHeight() * .62, 0);
        SoulPyreWispEntity visual = new SoulPyreWispEntity(world, start.x, start.y, start.z,
                WISP_LIFETIME, world.random.nextInt());
        visual.addCommandTag(WISP_VISUAL_TAG);
        WispVolley volley = new WispVolley(execution, tuning, pyre.actorId,
                pyre.sourceOwnerId, 1, Integer.MAX_VALUE);
        if (!world.spawnEntity(visual)) {
            volley.wispFinished();
            return;
        }
        WISPS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveWisp(
                visual.getUuid(), assigned == null ? null : assigned.getUuid(), pyre.stack.copy(), damage,
                searchRadius, world.getTime() + WISP_LIFETIME, volley, false,
                Integer.MAX_VALUE, true, Math.max(1, targetCap)));
    }

    private static void tickWisps(ServerWorld world) {
        List<ActiveWisp> wisps = WISPS.get(world);
        if (wisps == null || wisps.isEmpty()) {
            return;
        }

        Iterator<ActiveWisp> iterator = wisps.iterator();
        while (iterator.hasNext()) {
            ActiveWisp wisp = iterator.next();
            Entity entity = world.getEntity(wisp.visualId);
            LivingEntity actor = resolveLiving(world, wisp.volley.actorId);
            LivingEntity sourceOwner = resolveLiving(world, wisp.volley.sourceOwnerId);
            if (!(entity instanceof SoulPyreWispEntity visual)
                    || actor == null
                    || !actor.isAlive()
                    || world.getTime() >= wisp.expiresAt) {
                if (entity != null) entity.discard();
                wisp.volley.wispFinished();
                iterator.remove();
                continue;
            }

            LivingEntity target = resolveLiving(world, wisp.targetId);
            if (target == null
                    || !target.isAlive()
                    || !isValidTarget(actor, sourceOwner, target)) {
                if (wisp.hadTarget && !wisp.lance && wisp.retargetsRemaining <= 0) {
                    visual.discard();
                    wisp.volley.wispFinished();
                    iterator.remove();
                    continue;
                }
                double range = wisp.hadTarget && !wisp.lance
                        ? tuned(wisp.volley.tuning,
                        FireForgeMasteryTuning.Setting.SOULPYRE_WISP_RETARGET_RANGE,
                        FireForgeMasteryTuning.Setting.RETARGET_RANGE, 5)
                        : wisp.searchRadius;
                Vec3d center = wisp.hadTarget ? visual.getPos() : actor.getPos();
                int searchCap = tunedInteger(wisp.volley.tuning,
                        FireForgeMasteryTuning.Setting.SOULPYRE_WISP_SEARCH_CAP,
                        FireForgeMasteryTuning.Setting.SEARCH_CAP,
                        Config.uniqueEffects.soulpyre.wispSearchCap);
                List<LivingEntity> candidates = findWispTargets(world, actor, sourceOwner, center,
                        range, searchCap, wisp.lance
                                ? wisp.volley.visitedTargets : wisp.volley.hitTargets);
                target = selectWispTarget(candidates, wisp.volley.assignments,
                        wisp.volley.perTargetCap);
                if (wisp.hadTarget && !wisp.lance) wisp.retargetsRemaining--;
                wisp.targetId = target == null ? null : target.getUuid();
                if (target != null) {
                    wisp.hadTarget = true;
                    wisp.volley.assignments.merge(target.getUuid(), 1, Integer::sum);
                }
            }
            if (target == null) {
                Vec3d velocity = visual.getVelocity()
                        .multiply(0.94)
                        .add(0.0, 0.006, 0.0);
                moveWisp(visual, velocity);
                spawnWispTrail(world, visual);
                continue;
            }

            Vec3d targetPos = target.getPos().add(
                    0.0,
                    target.getHeight() * 0.55,
                    0.0
            );
            Vec3d delta = targetPos.subtract(visual.getPos());
            if (delta.lengthSquared()
                    <= WISP_IMPACT_DISTANCE * WISP_IMPACT_DISTANCE) {
                float damage = wisp.damage;
                if (wisp.volley.tuning.flag(1 << 14)
                        && isWispMarked(world, wisp.volley.actorId, target.getUuid())) {
                    damage *= (float) wisp.volley.tuning.get(
                            FireForgeMasteryTuning.Setting.SOULPYRE_WISP_MARK_DAMAGE_MULTIPLIER, 1.2);
                }
                boolean damaged = applyAbilityDamage(
                        world,
                        actor,
                        sourceOwner,
                        wisp.stack,
                        target,
                        damage
                );
                wisp.volley.visitedTargets.add(target.getUuid());
                if (damaged) {
                    wisp.volley.hitTargets.add(target.getUuid());
                    UniqueAbilityApi.emit(wisp.volley.execution, UniqueAbilityPhase.HIT,
                            FireForgeMasteryAbilities.HIT, target, 1, damage);
                    if (wisp.volley.tuning.flag(1 << 13)) {
                        target.setOnFireForTicks(tunedInteger(wisp.volley.tuning,
                                FireForgeMasteryTuning.Setting.SOULPYRE_WISP_FIRE_TICKS,
                                FireForgeMasteryTuning.Setting.FIRE_TICKS, 60));
                        markWispTarget(world, wisp.volley.actorId, target.getUuid(),
                                wisp.volley.tuning.integer(
                                        FireForgeMasteryTuning.Setting.SOULPYRE_WISP_MARK_DURATION_TICKS, 80));
                    }
                    if (wisp.wailing) explodeWailingWisp(world, actor, sourceOwner, wisp, target, damage);
                }
                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        targetPos.x,
                        targetPos.y,
                        targetPos.z,
                        18,
                        0.3,
                        0.35,
                        0.3,
                        0.04
                );
                world.playSoundFromEntity(
                        null,
                        target,
                        SoundEvents.BLOCK_SOUL_SAND_BREAK,
                        target.getSoundCategory(),
                        0.65F,
                        1.35F
                );
                if (wisp.lance && --wisp.remainingHits > 0) {
                    List<LivingEntity> candidates = findWispTargets(world, actor, sourceOwner,
                            visual.getPos(), wisp.searchRadius,
                            tunedInteger(wisp.volley.tuning,
                                    FireForgeMasteryTuning.Setting.SOULPYRE_WISP_SEARCH_CAP,
                                    FireForgeMasteryTuning.Setting.SEARCH_CAP,
                                    Config.uniqueEffects.soulpyre.wispSearchCap), wisp.volley.visitedTargets);
                    LivingEntity next = candidates.isEmpty() ? null : candidates.getFirst();
                    if (next != null) {
                        wisp.targetId = next.getUuid();
                        moveWisp(visual, visual.getVelocity().normalize().multiply(.35));
                        continue;
                    }
                }
                visual.discard();
                wisp.volley.wispFinished();
                iterator.remove();
                continue;
            }

            Vec3d desiredVelocity = delta.normalize().multiply(WISP_SPEED);
            Vec3d velocity = visual.getVelocity()
                    .multiply(0.42)
                    .add(desiredVelocity.multiply(0.58));
            if (velocity.lengthSquared() > WISP_SPEED * WISP_SPEED) {
                velocity = velocity.normalize().multiply(WISP_SPEED);
            }
            moveWisp(visual, velocity);
            spawnWispTrail(world, visual);
        }

        if (wisps.isEmpty()) {
            WISPS.remove(world);
        }
    }

    private static List<LivingEntity> findWispTargets(
            ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
            Vec3d center, double radius, int cap, Set<UUID> excluded) {
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                Box.of(center, radius * 2, radius * 2, radius * 2),
                EntityPredicates.VALID_LIVING_ENTITY
        )) {
            if (target.getPos().squaredDistanceTo(center) <= radius * radius
                    && !excluded.contains(target.getUuid())
                    && isValidTarget(actor, sourceOwner, target)) {
                targets.add(target);
            }
        }
        targets.sort(Comparator.comparingDouble(
                target -> target.getPos().squaredDistanceTo(center)));
        if (targets.size() > Math.max(0, cap)) return new ArrayList<>(targets.subList(0, Math.max(0, cap)));
        return targets;
    }

    private static LivingEntity selectWispTarget(List<LivingEntity> targets,
                                                  Map<UUID, Integer> assignments,
                                                  int perTargetCap) {
        for (LivingEntity target : targets) {
            if (assignments.getOrDefault(target.getUuid(), 0) < perTargetCap) return target;
        }
        return null;
    }

    private static void explodeWailingWisp(ServerWorld world, LivingEntity actor,
                                           LivingEntity sourceOwner, ActiveWisp wisp,
                                           LivingEntity primary, float directDamage) {
        double radius = tuned(wisp.volley.tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_WAILING_RADIUS,
                FireForgeMasteryTuning.Setting.RADIUS, 2.5);
        int cap = tunedInteger(wisp.volley.tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_WAILING_TARGET_CAP,
                FireForgeMasteryTuning.Setting.TARGET_CAP, 8);
        float damage = directDamage * (float) tuned(wisp.volley.tuning,
                FireForgeMasteryTuning.Setting.SOULPYRE_WAILING_DAMAGE_MULTIPLIER,
                FireForgeMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .4);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                        primary.getBoundingBox().expand(radius), EntityPredicates.VALID_LIVING_ENTITY)
                .stream().filter(target -> target != primary
                        && target.squaredDistanceTo(primary) <= radius * radius
                        && isValidTarget(actor, sourceOwner, target))
                .sorted(Comparator.comparingDouble(primary::squaredDistanceTo)).toList();
        int hits = 0;
        for (LivingEntity target : targets) {
            if (hits >= cap) break;
            if (!applyAbilityDamage(world, actor, sourceOwner, wisp.stack, target, damage)) continue;
            hits++;
            wisp.volley.hitTargets.add(target.getUuid());
            UniqueAbilityApi.emit(wisp.volley.execution, UniqueAbilityPhase.HIT,
                    FireForgeMasteryAbilities.HIT, target, 1, damage);
        }
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, primary.getX(), primary.getBodyY(.5),
                primary.getZ(), 24, radius * .35, .35, radius * .35, .04);
    }

    private static void recordBindingPulse(ServerWorld world, LivingEntity actor,
                                           ActivePyre pyre, LivingEntity target) {
        if (!pyre.tuning.flag(1 << 4)) return;
        int count = pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_BINDING_HIT_COUNT, 3);
        int window = pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_BINDING_WINDOW_TICKS, 80);
        List<Long> hits = pyre.bindingHits.computeIfAbsent(target.getUuid(), ignored -> new ArrayList<>());
        hits.removeIf(hit -> hit + window < world.getTime());
        hits.add(world.getTime());
        if (hits.size() < count) return;
        hits.clear();
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_BINDING_SLOW_TICKS, 40),
                pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_BINDING_SLOW_AMPLIFIER, 1),
                false, true, true), actor);
    }

    private static void markWispTarget(ServerWorld world, UUID ownerId, UUID targetId, int duration) {
        WISP_MARKS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(ownerId, ignored -> new HashMap<>())
                .put(targetId, world.getTime() + Math.max(1, duration));
    }

    private static boolean isWispMarked(ServerWorld world, UUID ownerId, UUID targetId) {
        Map<UUID, Map<UUID, Long>> owners = WISP_MARKS.get(world);
        Map<UUID, Long> marks = owners == null ? null : owners.get(ownerId);
        return marks != null && marks.getOrDefault(targetId, Long.MIN_VALUE) > world.getTime();
    }

    private static void pruneWispMarks(ServerWorld world) {
        Map<UUID, Map<UUID, Long>> owners = WISP_MARKS.get(world);
        if (owners == null) return;
        long now = world.getTime();
        owners.values().forEach(marks -> marks.entrySet().removeIf(entry -> entry.getValue() <= now));
        owners.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (owners.isEmpty()) WISP_MARKS.remove(world);
    }

    private static int sourceMatchScore(ActivePyre pyre,
                                        Entity directSource, Entity attacker) {
        UUID directId = directSource == null ? null : directSource.getUuid();
        UUID attackerId = attacker == null ? null : attacker.getUuid();
        if (pyre.actorId.equals(directId)) {
            return 4;
        }
        if (pyre.actorId.equals(attackerId)) {
            return 3;
        }
        if (pyre.sourceOwnerId != null
                && (pyre.sourceOwnerId.equals(directId)
                || pyre.sourceOwnerId.equals(attackerId))) {
            return 2;
        }
        return 0;
    }

    private static boolean applyAbilityDamage(
            ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
            ItemStack stack, LivingEntity target, float damage) {
        LivingEntity attributedOwner = sourceOwner == null ? actor : sourceOwner;
        DamageSource source = world.getDamageSources().indirectMagic(
                actor,
                attributedOwner
        );
        float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                world,
                stack,
                target,
                source,
                Math.max(0.0F, damage)
        );
        return HelperMethods.damageThroughIframes(target, source, finalDamage);
    }

    private static boolean isValidTarget(
            LivingEntity actor, LivingEntity sourceOwner, LivingEntity target) {
        return target.isAlive()
                && isEligibleTarget(actor, sourceOwner, target);
    }

    private static boolean isEligibleTarget(
            LivingEntity actor, LivingEntity sourceOwner, LivingEntity target) {
        return target != actor
                && target != sourceOwner
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null
                || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static boolean isInsideField(
            LivingEntity actor, LivingEntity target, float radius) {
        double dx = target.getX() - actor.getX();
        double dz = target.getZ() - actor.getZ();
        double verticalRange = Math.max(
                2.0,
                Config.uniqueEffects.soulpyre.verticalRange
        );
        return dx * dx + dz * dz <= radius * radius
                && Math.abs(target.getBodyY(0.5) - actor.getBodyY(0.5))
                <= verticalRange;
    }

    private static TransmutedLavaContact findTransmutedLavaContact(Entity entity) {
        if (!(entity instanceof LivingEntity target)
                || !(entity.getWorld() instanceof ServerWorld world)
                || !target.isAlive()
                || !target.isTouchingWater()
                || !touchesReplaceableWater(world, target)) {
            return null;
        }

        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return null;
        }

        TransmutedLavaContact selected = null;
        double bestDistance = Double.MAX_VALUE;
        UUID bestActorId = null;
        for (ActivePyre pyre : active.values()) {
            if (pyre.cancelled || pyre.currentRadius <= 0.05F) {
                continue;
            }
            LivingEntity actor = resolveLiving(world, pyre.actorId);
            LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
            if (actor == null
                    || !actor.isAlive()
                    || !isEligibleTarget(actor, sourceOwner, target)
                    || !isInsideField(actor, target, pyre.currentRadius)) {
                continue;
            }

            double distance = actor.squaredDistanceTo(target);
            if (selected == null
                    || distance < bestDistance - 1.0E-6
                    || Math.abs(distance - bestDistance) <= 1.0E-6
                    && actor.getUuid().compareTo(bestActorId) < 0) {
                selected = new TransmutedLavaContact(
                        world,
                        target,
                        actor,
                        sourceOwner
                );
                bestDistance = distance;
                bestActorId = actor.getUuid();
            }
        }
        return selected;
    }

    private static boolean touchesReplaceableWater(
            ServerWorld world, LivingEntity target) {
        Box bounds = target.getBoundingBox().contract(0.001);
        int minX = MathHelper.floor(bounds.minX);
        int maxX = MathHelper.floor(bounds.maxX);
        int minY = MathHelper.floor(bounds.minY);
        int maxY = MathHelper.floor(bounds.maxY);
        int minZ = MathHelper.floor(bounds.minZ);
        int maxZ = MathHelper.floor(bounds.maxZ);
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = world.getBlockState(cursor);
                    if (!state.getFluidState().isIn(FluidTags.WATER)
                            || !state.getCollisionShape(world, cursor).isEmpty()) {
                        continue;
                    }
                    double fluidTop = y
                            + state.getFluidState().getHeight(world, cursor);
                    if (bounds.maxY > y && bounds.minY < fluidTop) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void pullTarget(LivingEntity actor, LivingEntity target, double strength) {
        Vec3d direction = actor.getPos()
                .add(0.0, actor.getHeight() * 0.45, 0.0)
                .subtract(target.getPos()
                        .add(0.0, target.getHeight() * 0.45, 0.0));
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        Vec3d pull = direction.normalize().multiply(Math.max(0, strength));
        target.setVelocity(target.getVelocity().multiply(0.72).add(pull));
        target.velocityModified = true;
        target.velocityDirty = true;
    }

    private static void moveWisp(SoulPyreWispEntity visual, Vec3d velocity) {
        visual.setVelocity(velocity);
        visual.setPosition(
                visual.getX() + velocity.x,
                visual.getY() + velocity.y,
                visual.getZ() + velocity.z
        );
        visual.velocityModified = true;
        visual.velocityDirty = true;
    }

    private static void spawnWispTrail(
            ServerWorld world, SoulPyreWispEntity visual) {
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                visual.getX(),
                visual.getY(),
                visual.getZ(),
                2,
                0.08,
                0.08,
                0.08,
                0.005
        );
    }

    private static void updateVisual(ServerWorld world, LivingEntity actor,
                                     ActivePyre pyre, float radius) {
        Entity entity = pyre.visualId == null
                ? null
                : world.getEntity(pyre.visualId);
        if (entity instanceof SoulPyreVisualEntity visual) {
            visual.setPosition(actor.getX(), actor.getY(), actor.getZ());
            visual.setOwnerEntityId(actor.getId());
            visual.setRadius(radius);
            visual.setSoulCount(pyre.souls);
            visual.setPhase(
                    pyre.collapsing
                            ? SoulPyreVisualEntity.PHASE_COLLAPSING
                            : SoulPyreVisualEntity.PHASE_ACTIVE
            );
        }
    }

    private static void triggerPulseVisual(ServerWorld world, UUID visualId) {
        Entity entity = visualId == null ? null : world.getEntity(visualId);
        if (entity instanceof SoulPyreVisualEntity visual) {
            visual.triggerPulse();
        }
    }

    private static void beginVisualCollapse(ServerWorld world, LivingEntity actor,
                                            ActivePyre pyre) {
        Entity entity = pyre.visualId == null
                ? null
                : world.getEntity(pyre.visualId);
        if (entity instanceof SoulPyreVisualEntity visual) {
            visual.setPosition(actor.getX(), actor.getY(), actor.getZ());
            visual.setSoulCount(0);
            visual.beginCollapse(pyre.collapseDuration + 2);
        }
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity entity = visualId == null ? null : world.getEntity(visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void spawnHarvestEffects(
            ServerWorld world, LivingEntity target,
            LivingEntity actor, int soulCount) {
        HelperMethods.spawnWaistHeightParticles(
                world,
                ParticleTypes.SOUL,
                target,
                actor,
                10
        );
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getBodyY(0.5),
                target.getZ(),
                14,
                0.25,
                0.35,
                0.25,
                0.025
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(),
                actor.getSoundCategory(),
                0.35F + Math.min(5, soulCount) * 0.035F,
                1.15F + Math.min(5, soulCount) * 0.035F
        );
    }

    private static void spawnPulseEffects(
            ServerWorld world, LivingEntity actor,
            float radius, int pulseIndex, boolean requiem) {
        HelperMethods.spawnOrbitParticles(
                world,
                actor.getPos().add(0.0, 0.08, 0.0),
                ParticleTypes.SOUL_FIRE_FLAME,
                radius,
                requiem ? 72 : 30
        );
        HelperMethods.spawnOrbitParticles(
                world,
                actor.getPos().add(0.0, 0.14, 0.0),
                ParticleTypes.SOUL,
                radius,
                requiem ? 48 : 20
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                actor.getSoundCategory(),
                requiem ? 0.85F : 0.12F,
                requiem ? 0.58F : 0.82F + pulseIndex * 0.055F
        );
    }

    private static void spawnRadiusMilestoneEffects(
            ServerWorld world, LivingEntity actor,
            ActivePyre pyre, int milestone) {
        triggerPulseVisual(world, pyre.visualId);
        HelperMethods.spawnOrbitParticles(
                world,
                actor.getPos().add(0.0, 0.1, 0.0),
                ParticleTypes.SOUL_FIRE_FLAME,
                pyre.currentRadius,
                42 + milestone * 10
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                actor.getSoundCategory(),
                0.38F + milestone * 0.12F,
                0.76F + milestone * 0.16F
        );
    }

    private static void spawnEarlyVolleyEffects(
            ServerWorld world, LivingEntity actor, float radius) {
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                actor.getX(),
                actor.getBodyY(0.6),
                actor.getZ(),
                28,
                Math.min(1.6, radius * 0.18),
                0.45,
                Math.min(1.6, radius * 0.18),
                0.045
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(),
                actor.getSoundCategory(),
                0.9F,
                0.78F
        );
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID entityId) {
        if (entityId == null) {
            return null;
        }
        Entity entity = world.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static ActivePyre activePyre(ServerWorld world, UUID actorId) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        return active == null ? null : active.get(actorId);
    }

    private static WispPlan prepareWispPlan(ServerWorld world, LivingEntity actor,
                                            ActivePyre pyre) {
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(
                FireForgeMasteryAbilities.SOUL_PYRE_WISP, world, pyre.stack, actor, null);
        return new WispPlan(execution, FireForgeMasteryAbilities.tuning(execution));
    }

    private static void cancelWispPlan(ActivePyre pyre) {
        if (pyre.pendingWispPlan == null) return;
        cancelExecution(pyre.pendingWispPlan.execution);
        pyre.pendingWispPlan = null;
    }

    private static int effectiveVolleySize(FireForgeMasteryTuning tuning) {
        int size = tunedInteger(tuning, FireForgeMasteryTuning.Setting.SOULPYRE_WISP_VOLLEY_SIZE,
                FireForgeMasteryTuning.Setting.WISP_COUNT, Config.uniqueEffects.soulpyre.wispVolleySize);
        return volleySize(size, tuning.flag(1 << 16), tuning.get(
                FireForgeMasteryTuning.Setting.SOULPYRE_LEGION_VOLLEY_MULTIPLIER, 2));
    }

    static int closedPulseInterval(int duration, int pulseCount, double multiplier) {
        return Math.max(1, (int) Math.round(duration / (double) Math.max(1, pulseCount)
                * Math.max(0, multiplier)));
    }

    static int volleySize(int baseSize, boolean legion, double multiplier) {
        double scale = legion ? Math.max(0, multiplier) : 1;
        return Math.max(1, (int) Math.round(Math.max(1, baseSize) * scale));
    }

    static float finalSoulMultiplier(int storedSouls, double storedBonus,
                                     int harvestedSouls, double harvestedBonus,
                                     int harvestedCap, boolean requiem) {
        float result = 1 + Math.max(0, storedSouls) * (float) Math.max(0, storedBonus);
        if (requiem) result += Math.min(Math.max(0, harvestedSouls), Math.max(0, harvestedCap))
                * (float) Math.max(0, harvestedBonus);
        return result;
    }

    static float lanceDamageMultiplier(int souls, int soulCap, double damagePerSoul) {
        return Math.min(Math.max(0, souls), Math.max(0, soulCap))
                * (float) Math.max(0, damagePerSoul);
    }

    static boolean isHarvestMilestone(int totalHarvested, int step) {
        return totalHarvested > 0 && totalHarvested % Math.max(1, step) == 0;
    }

    static float boundedAbsorptionAfterGrant(float current, float amount, float cap) {
        float safeCurrent = Math.max(0, current);
        return Math.max(safeCurrent, Math.min(Math.max(0, cap), safeCurrent + Math.max(0, amount)));
    }

    private static double tuned(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                FireForgeMasteryTuning.Setting generic, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(generic, fallback);
    }

    private static int tunedInteger(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                    FireForgeMasteryTuning.Setting generic, int fallback) {
        return (int) Math.round(tuned(tuning, scoped, generic, fallback));
    }

    private static void applyKnockbackResistance(LivingEntity actor) {
        EntityAttributeInstance attribute = actor.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        attribute.removeModifier(KNOCKBACK_RESISTANCE_ID);
        attribute.addTemporaryModifier(new EntityAttributeModifier(KNOCKBACK_RESISTANCE_ID,
                1, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeKnockbackResistance(LivingEntity actor) {
        EntityAttributeInstance attribute = actor.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null) attribute.removeModifier(KNOCKBACK_RESISTANCE_ID);
    }

    private static void finishTetherDefense(ServerWorld world, LivingEntity actor,
                                            ActivePyre pyre, boolean allowLinger) {
        int knockbackLinger = allowLinger && pyre.tuning.flag(1 << 19)
                ? pyre.tuning.integer(FireForgeMasteryTuning.Setting.SOULPYRE_KNOCKBACK_LINGER_TICKS, 60) : 0;
        if (knockbackLinger > 0) {
            LINGERING_KNOCKBACK.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(actor.getUuid(), world.getTime() + knockbackLinger);
        } else {
            removeKnockbackResistance(actor);
        }
        if (!pyre.tuning.flag(1 << 20)) return;
        if (pyre.managedFireResistance) {
            StatusEffectInstance current = actor.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
            if (current != null && current.getAmplifier() == 0) {
                actor.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
            }
            if (pyre.previousFireResistance != null) {
                int elapsed = (int) Math.max(0, world.getTime() - pyre.startTick);
                int remaining = pyre.previousFireResistance.getDuration() - elapsed;
                if (remaining > 0) actor.addStatusEffect(new StatusEffectInstance(
                        pyre.previousFireResistance.getEffectType(), remaining,
                        pyre.previousFireResistance.getAmplifier(), pyre.previousFireResistance.isAmbient(),
                        pyre.previousFireResistance.shouldShowParticles(),
                        pyre.previousFireResistance.shouldShowIcon()), actor);
            }
        }
        if (allowLinger) {
            int linger = pyre.tuning.integer(
                    FireForgeMasteryTuning.Setting.SOULPYRE_FIRE_RESISTANCE_LINGER_TICKS, 40);
            if (linger > 0) actor.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.FIRE_RESISTANCE, linger, 0, false, true, true), actor);
        }
        pyre.managedFireResistance = false;
    }

    private static void maintainNetherWard(ServerWorld world, LivingEntity actor, ActivePyre pyre) {
        if (!pyre.tuning.flag(1 << 20)) return;
        int linger = pyre.tuning.integer(
                FireForgeMasteryTuning.Setting.SOULPYRE_FIRE_RESISTANCE_LINGER_TICKS, 40);
        int required = (int) Math.max(1, pyre.endTick - world.getTime() + Math.max(0, linger));
        StatusEffectInstance current = actor.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
        if (current != null && (current.getAmplifier() > 0 || current.getDuration() >= required)) return;
        pyre.managedFireResistance = true;
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,
                required, 0, false, true, true), actor);
    }

    private static void cancelExecution(UniqueAbilityExecution execution) {
        if (execution != null && !execution.isTerminal()) UniqueAbilityApi.cancel(execution);
    }

    private static boolean hasEntries(Map<?, ?> values) {
        return values != null && !values.isEmpty();
    }

    private static boolean isWieldingSoulPyre(LivingEntity actor) {
        return actor.getMainHandStack().isOf(ItemsRegistry.SOULPYRE.get())
                || actor.getOffHandStack().isOf(ItemsRegistry.SOULPYRE.get());
    }

    private static final class ActivePyre {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final ItemStack stack;
        private final UUID visualId;
        private final long startTick;
        private final long endTick;
        private final int pulseCount;
        private final int duration;
        private final int collapseDuration;
        private final float startRadius;
        private final float maxRadius;
        private final int pulseTargetCap;
        private final float baseDamage;
        private final FireForgeMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final Map<UUID, List<Long>> bindingHits = new HashMap<>();
        private int pulsesCompleted;
        private int souls;
        private int totalSoulsHarvested;
        private int undyingReserve;
        private WispPlan pendingWispPlan;
        private int nextMilestone;
        private final int closedPulseInterval;
        private long nextClosedPulseTick;
        private float currentRadius;
        private float targetRadius;
        private float growthStartRadius;
        private long growthStartTick;
        private boolean growing;
        private boolean collapsing;
        private boolean cancelled;
        private long collapseStartTick;
        private long collapseEndTick;
        private float collapseStartRadius;
        private float lastPulseDamage;
        private float collapsePeriodicDamage;
        private long nextCollapseDamageTick;
        private StatusEffectInstance previousFireResistance;
        private boolean managedFireResistance;

        private ActivePyre(UUID actorId, UUID sourceOwnerId, ItemStack stack,
                           UUID visualId, long startTick, long endTick,
                           int pulseCount, int duration, int collapseDuration,
                           float startRadius, float maxRadius, int pulseTargetCap,
                           float baseDamage, FireForgeMasteryTuning tuning,
                           UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.stack = stack;
            this.visualId = visualId;
            this.startTick = startTick;
            this.endTick = endTick;
            this.pulseCount = pulseCount;
            this.duration = duration;
            this.collapseDuration = collapseDuration;
            this.startRadius = startRadius;
            this.maxRadius = maxRadius;
            this.pulseTargetCap = pulseTargetCap;
            this.baseDamage = baseDamage;
            this.tuning = tuning;
            this.execution = execution;
            this.closedPulseInterval = SoulPyreAbilityManager.closedPulseInterval(
                    duration,
                    pulseCount,
                    tuning.get(FireForgeMasteryTuning.Setting.SOULPYRE_CLOSED_INTERVAL_MULTIPLIER, 0.8)
            );
            this.nextClosedPulseTick = startTick + closedPulseInterval;
            this.currentRadius = startRadius;
            this.targetRadius = startRadius;
            this.growthStartRadius = startRadius;
        }
    }

    private static final class ActiveWisp {
        private final UUID visualId;
        private UUID targetId;
        private final ItemStack stack;
        private final float damage;
        private final double searchRadius;
        private final long expiresAt;
        private final WispVolley volley;
        private final boolean wailing;
        private int retargetsRemaining;
        private final boolean lance;
        private int remainingHits;
        private boolean hadTarget;

        private ActiveWisp(UUID visualId, UUID targetId, ItemStack stack,
                           float damage, double searchRadius, long expiresAt,
                           WispVolley volley, boolean wailing,
                           int retargetsRemaining, boolean lance, int remainingHits) {
            this.visualId = visualId;
            this.targetId = targetId;
            this.stack = stack;
            this.damage = damage;
            this.searchRadius = searchRadius;
            this.expiresAt = expiresAt;
            this.volley = volley;
            this.wailing = wailing;
            this.retargetsRemaining = Math.max(0, retargetsRemaining);
            this.lance = lance;
            this.remainingHits = Math.max(1, remainingHits);
            this.hadTarget = targetId != null;
        }
    }

    private record WispPlan(
            UniqueAbilityExecution execution,
            FireForgeMasteryTuning tuning) {
    }

    private static final class WispVolley {
        private final UniqueAbilityExecution execution;
        private final FireForgeMasteryTuning tuning;
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final Map<UUID, Integer> assignments = new HashMap<>();
        private final Set<UUID> hitTargets = new HashSet<>();
        private final Set<UUID> visitedTargets = new HashSet<>();
        private final int perTargetCap;
        private int remaining;
        private boolean terminal;

        private WispVolley(UniqueAbilityExecution execution, FireForgeMasteryTuning tuning,
                           UUID actorId, UUID sourceOwnerId, int remaining,
                           int perTargetCap) {
            this.execution = execution;
            this.tuning = tuning;
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.remaining = Math.max(0, remaining);
            this.perTargetCap = Math.max(1, perTargetCap);
        }

        private void wispFinished() {
            if (terminal || --remaining > 0) return;
            terminal = true;
            UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, hitTargets.size());
        }

        private void cancel() {
            if (terminal) return;
            terminal = true;
            cancelExecution(execution);
        }
    }

    private record TransmutedLavaContact(
            ServerWorld world,
            LivingEntity target,
            LivingEntity actor,
            LivingEntity sourceOwner) {
    }
}
