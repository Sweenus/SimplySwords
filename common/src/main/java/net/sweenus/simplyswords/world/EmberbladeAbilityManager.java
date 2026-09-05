package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.WeaponManaCost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EmberbladeAbilityManager {
    private static final int CHANNEL_SLOW_GRACE_TICKS = 20;
    private static final int BASE_CHANNEL_TICKS = 80;
    private static final int EXTRA_SHRAPNEL_WINDOW_TICKS = 20;
    private static final Map<ServerWorld, Map<UUID, State>> STATES = new HashMap<>();

    private EmberbladeAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, State> states = STATES.get(world);
        return states != null && !states.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, State> states = STATES.get(world);
        if (states == null) return;
        for (UUID actorId : List.copyOf(states.keySet())) {
            if (world.getEntity(actorId) instanceof LivingEntity actor) {
                tickHolder(world, actor);
                State state = states.get(actorId);
                if (state != null && isIdle(state, world.getTime())) states.remove(actorId);
            } else {
                State removed = states.remove(actorId);
                if (removed != null && removed.channel != null) UniqueAbilityApi.cancel(removed.channel.execution);
            }
        }
        if (states.isEmpty()) STATES.remove(world);
    }

    public static boolean startChannel(ServerWorld world, ServerPlayerEntity actor, ItemStack stack, Hand hand) {
        if (world == null || actor == null || stack == null || stack.isEmpty()
                || actor.getItemCooldownManager().isCoolingDown(stack.getItem())) return false;
        State state = state(world, actor);
        cancelChannel(actor, state);
        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, actor, actor, null, hand,
                WeaponAbilityActivationSource.PLAYER);
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(
                FireForgeMasteryAbilities.EMBERBLADE_SHRAPNEL, context, Config.uniqueEffects.emberblade.cooldown);
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        prune(state, world.getTime());
        state.channel = new Channel(execution, tuning, stack.copy(), hand, world.getTime(),
                channelTicks(tuning));
        if (tuning.flag(1 << 7)) applyChannelSlow(actor, state.channel);
        return true;
    }

    public static boolean releaseChannel(ServerWorld world, ItemStack stack, LivingEntity actor,
                                         LivingEntity target) {
        State state = get(world, actor);
        if (state == null || state.channel == null || state.channel.released) return false;
        Channel channel = state.channel;
        channel.released = true;
        state.channel = null;
        removeChannelSlow(actor, channel);
        long now = world.getTime();
        int elapsed = elapsedTicks(now, channel.startedAt, channel.channelTicks);
        if (!ItemStack.areItemsEqual(channel.stack, stack) || !validTarget(actor, target)
                || !WeaponManaCost.canAfford(actor, stack)) {
            bankInterrupted(state, channel.tuning, elapsed, channel.channelTicks, now);
            UniqueAbilityApi.cancel(channel.execution);
            return false;
        }
        float charge = chargeRatio(elapsed, channel.channelTicks,
                validBank(state.interruptedBank, state.interruptedBankUntil, now)
                        + validBank(state.flameBank, state.flameBankUntil, now));
        state.interruptedBank = 0;
        state.flameBank = 0;
        WeaponManaCost.spend(actor, stack);
        boolean extraShrapnel = inFullChargeWindow(elapsed, channel.channelTicks,
                EXTRA_SHRAPNEL_WINDOW_TICKS);
        boolean released = release(channel.execution, world, stack, actor, target, charge, elapsed,
                inFullChargeWindow(elapsed, channel.channelTicks,
                        channel.tuning.integer(s("EMBERBLADE_FULL_CHARGE_WINDOW_TICKS"), 0)),
                extraShrapnel);
        if (released) {
            SimplySwordsAPI.setWeaponCooldown(actor, stack,
                    channel.execution.cooldownTicks(Config.uniqueEffects.emberblade.cooldown));
        }
        return released;
    }

    public static void interruptChannel(ServerWorld world, LivingEntity actor) {
        State state = get(world, actor);
        if (state == null || state.channel == null) return;
        Channel channel = state.channel;
        int elapsed = elapsedTicks(world.getTime(), channel.startedAt, channel.channelTicks);
        bankInterrupted(state, channel.tuning, elapsed, channel.channelTicks, world.getTime());
        cancelChannel(actor, state);
    }

    public static boolean releaseDelegated(WeaponAbilityContext context) {
        if (context == null || !validTarget(context.actor(), context.target())) return false;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(
                FireForgeMasteryAbilities.EMBERBLADE_SHRAPNEL, context, Config.uniqueEffects.emberblade.cooldown);
        UniqueAbilityApi.start(execution);
        boolean released = release(execution, context.world(), context.stack(), context.actor(), context.target(),
                1, BASE_CHANNEL_TICKS, true, true);
        UniqueAbilityApi.publishStartedExecution(execution);
        return released;
    }

    public static int channelTicks(ServerWorld world, LivingEntity actor) {
        State state = get(world, actor);
        return state == null || state.channel == null ? BASE_CHANNEL_TICKS : state.channel.channelTicks;
    }

    public static boolean isFullyCharged(ServerWorld world, LivingEntity actor) {
        State state = get(world, actor);
        return state != null && state.channel != null
                && elapsedTicks(world.getTime(), state.channel.startedAt, state.channel.channelTicks)
                >= state.channel.channelTicks;
    }

    public static void tickHolder(ServerWorld world, LivingEntity actor) {
        State state = get(world, actor);
        if (state == null) return;
        long now = world.getTime();
        prune(state, now);
        if (state.channel != null && (!actor.isAlive() || !isActivelyChanneling(actor, state.channel))) {
            interruptChannel(world, actor);
        }
        if (state.fallProtectionUntil >= now) actor.fallDistance = 0;
        if (state.buffUntil >= now && state.moveRequired > 0) {
            Vec3d current = actor.getPos();
            if (state.lastMove != null) state.distanceMoved += Math.min(2, current.distanceTo(state.lastMove));
            state.lastMove = current;
            if (state.distanceMoved >= state.moveRequired) {
                state.nextHitUntil = now + state.nextHitDuration;
                state.nextHitMultiplier = Math.max(1, state.primedMultiplier);
                state.moveRequired = 0;
            }
        }
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack, LivingEntity actor, LivingEntity target) {
        if (world == null || actor == null || target == null || !target.isOnFire()) return;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(
                FireForgeMasteryAbilities.EMBERBLADE_SHRAPNEL, world, stack, actor, target);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        double gain = tuning.get(s("EMBERBLADE_BANK_GAIN"), 0);
        if (gain > 0) {
            State state = state(world, actor);
            long now = world.getTime();
            prune(state, now);
            state.flameBank = (float) Math.min(tuning.get(s("EMBERBLADE_BANK_CAP"), .25),
                    state.flameBank + gain);
            state.flameBankUntil = now
                    + tuning.integer(s("EMBERBLADE_FLAME_BANK_DURATION_TICKS"), 100);
        }
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, 0);
    }

    public static float modifyOutgoingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0 || !(actor.getWorld() instanceof ServerWorld world)
                || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)) return amount;
        State state = get(world, actor);
        if (state == null) return amount;
        long now = world.getTime();
        prune(state, now);
        if (state.nextHitUntil < now || state.nextHitMultiplier <= 1) return amount;
        float result = amount * (float) state.nextHitMultiplier;
        state.nextHitUntil = 0;
        state.nextHitMultiplier = 1;
        return result;
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0 || !(actor.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        State state = get(world, actor);
        if (state == null) return amount;
        long now = world.getTime();
        prune(state, now);
        if (source.isIn(DamageTypeTags.IS_FALL) && state.fallProtectionUntil >= now) return 0;
        Channel channel = state.channel;
        if (channel == null) return amount;
        int guardTicks = channel.tuning.integer(s("EMBERBLADE_LATE_GUARD_TICKS"), 0);
        int elapsed = elapsedTicks(now, channel.startedAt, channel.channelTicks);
        if (inFullChargeWindow(elapsed, channel.channelTicks, guardTicks)) {
            amount *= (float) (1 - channel.tuning.get(s("EMBERBLADE_LATE_DAMAGE_REDUCTION"), 0));
        }
        return amount;
    }

    public static void onDamageTaken(ServerWorld world, LivingEntity actor) {
        State state = get(world, actor);
        if (state == null) return;
        state.interruptedBank = 0;
        state.interruptedBankUntil = 0;
    }

    public static void clear(ServerWorld world, LivingEntity actor) {
        if (world == null || actor == null) return;
        Map<UUID, State> states = STATES.get(world);
        State state = states == null ? null : states.remove(actor.getUuid());
        if (state != null) {
            removeChannelSlow(actor, state.channel);
            cancelChannel(actor, state);
        }
        if (states != null && states.isEmpty()) STATES.remove(world);
    }

    public static void clearAll(ServerWorld world) {
        Map<UUID, State> states = STATES.remove(world);
        if (states == null) return;
        states.forEach((id, state) -> {
            if (world.getEntity(id) instanceof LivingEntity actor) {
                removeChannelSlow(actor, state.channel);
                cancelChannel(actor, state);
            } else if (state.channel != null) {
                UniqueAbilityApi.cancel(state.channel.execution);
            }
        });
    }

    private static boolean release(UniqueAbilityExecution execution, ServerWorld world, ItemStack stack,
                                   LivingEntity actor, LivingEntity target, float charge, int elapsed,
                                   boolean finalWindow, boolean extraShrapnel) {
        if (!validTarget(actor, target)) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        UniqueAbilityApi.start(execution);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        float minimum = HelperMethods.abilityScaledDamage("fire", actor, stack,
                Config.uniqueEffects.emberblade.initialDamageScaling,
                Config.uniqueEffects.emberblade.initialSpellScaling)
                * (float) tuned(tuning, s("EMBERBLADE_MIN_DAMAGE_MULTIPLIER"), s("DAMAGE_MULTIPLIER"), 1);
        double maximumMultiplier = tuned(tuning, s("EMBERBLADE_MAX_DAMAGE_MULTIPLIER"),
                s("FINAL_DAMAGE_MULTIPLIER"), 1)
                * fullChargeBonus(finalWindow, tuning.get(s("EMBERBLADE_FULL_CHARGE_MULTIPLIER"), 1));
        float maximum = HelperMethods.abilityScaledDamage("fire", actor, stack,
                Config.uniqueEffects.emberblade.maxChargeDamageScaling,
                Config.uniqueEffects.emberblade.maxChargeSpellScaling)
                * (float) maximumMultiplier;
        float damage = minimum + maximum * Math.clamp(charge, 0, 1);
        if (tuning.flag(1 << 3) && aimAngle(actor, target) <= 2) {
            damage *= (float) tuned(tuning, s("EMBERBLADE_AIM_DAMAGE_MULTIPLIER"),
                    s("OUTGOING_MULTIPLIER"), 1.2);
        }

        int affected = 0;
        boolean primaryHit = fireShrapnel(execution, world, actor, stack, target, damage, tuning);
        if (primaryHit) affected++;
        if (extraShrapnel && target.isAlive()) {
            if (fireShrapnel(execution, world, actor, stack, target, damage, tuning)) affected++;
        }
        int fireTicks = tuning.integer(s("FIRE_TICKS"), 0);
        if (finalWindow && tuning.flag(1 << 6)) {
            fireTicks = Math.max(fireTicks,
                    tuning.integer(s("EMBERBLADE_FULL_CHARGE_FIRE_TICKS"), 80));
        }
        ignite(target, fireTicks);

        boolean duelist = tuning.flag(1 << 16);
        if (!duelist) {
            int pierceCount = tunedInteger(tuning, s("EMBERBLADE_PIERCE_COUNT"), s("COUNT"),
                    tuning.flag(1 << 2) ? 1 : 0);
            double pierceRange = tuned(tuning, s("EMBERBLADE_PIERCE_RANGE"), s("RANGE"), 12);
            float pierceDamage = damage * (float) tuned(tuning,
                    s("EMBERBLADE_PIERCE_DAMAGE_MULTIPLIER"), s("SECONDARY_DAMAGE_MULTIPLIER"), .7);
            for (LivingEntity candidate : targets(world, actor, target.getPos(), pierceRange, pierceCount + 1)) {
                if (candidate == target) continue;
                if (dealAndRecord(execution, world, actor, stack, candidate, pierceDamage, tuning)) affected++;
                ignite(candidate, fireTicks);
                if (--pierceCount <= 0) break;
            }
            affected += splash(execution, world, actor, stack, target,
                    tuned(tuning, s("EMBERBLADE_SPLASH_RADIUS"), s("RADIUS"), 0),
                    tunedInteger(tuning, s("EMBERBLADE_SPLASH_TARGET_CAP"), s("TARGET_CAP"), 0),
                    damage * (float) tuned(tuning, s("EMBERBLADE_SPLASH_DAMAGE_MULTIPLIER"),
                            s("SECONDARY_DAMAGE_MULTIPLIER"), 0), tuning);
        }

        boolean fullCharge = charge >= .9999F;
        if (fullCharge) affected += fragments(execution, world, actor, stack, target, damage, tuning);
        applyRewards(execution, world, actor, stack, target, tuning, charge, elapsed, fullCharge, affected);
        if (!duelist || primaryHit) applyReleaseMovement(actor, target, tuning, world.getTime());
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.HIT,
                target, affected, damage);
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, affected);
        return true;
    }

    private static boolean fireShrapnel(UniqueAbilityExecution execution, ServerWorld world, LivingEntity actor,
                                        ItemStack stack, LivingEntity target, float damage,
                                        FireForgeMasteryTuning tuning) {
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.SMOKE, actor, target, 20);
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.POOF, actor, target, 20);
        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.ASH, actor, target, 20);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), .4F, 1.5F);
        boolean hit = dealAndRecord(execution, world, actor, stack, target, damage, tuning);
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                actor.getSoundCategory(), .4F, 1.1F);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.EXPLOSION, 1, 1);
        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.POOF, 1, 20);
        return hit;
    }

    private static int fragments(UniqueAbilityExecution execution, ServerWorld world, LivingEntity actor,
                                 ItemStack stack, LivingEntity primary, float damage, FireForgeMasteryTuning tuning) {
        int count = tunedInteger(tuning, s("EMBERBLADE_FRAGMENT_COUNT"), s("COUNT"), 0);
        if (count <= 0) return 0;
        double range = tuning.get(s("EMBERBLADE_FRAGMENT_RANGE"), 5);
        double seekRange = tuning.get(s("EMBERBLADE_FRAGMENT_SEEK_RANGE"), 0);
        double searchRange = Math.max(range, seekRange);
        List<LivingEntity> candidates = new ArrayList<>(targets(world, actor, primary.getPos(), searchRange, 64));
        if (seekRange > 0) candidates.sort(Comparator
                .comparing((LivingEntity candidate) -> !candidate.isOnFire())
                .thenComparingDouble(candidate -> candidate.squaredDistanceTo(primary))
                .thenComparing(candidate -> candidate.getUuid().toString()));
        Set<UUID> used = new HashSet<>();
        used.add(primary.getUuid());
        int affected = 0;
        float fragmentDamage = damage * (float) tuned(tuning,
                s("EMBERBLADE_FRAGMENT_DAMAGE_MULTIPLIER"), s("SECONDARY_DAMAGE_MULTIPLIER"), .35);
        for (LivingEntity candidate : candidates) {
            if (used.contains(candidate.getUuid())) continue;
            boolean seeking = candidate.isOnFire() && candidate.squaredDistanceTo(primary) <= seekRange * seekRange;
            if (!seeking && candidate.squaredDistanceTo(primary) > range * range) continue;
            used.add(candidate.getUuid());
            float dealt = seeking ? fragmentDamage
                    * (float) tuning.get(s("EMBERBLADE_FRAGMENT_SEEK_MULTIPLIER"), 1) : fragmentDamage;
            boolean fragmentHit = dealAndRecord(execution, world, actor, stack, candidate, dealt, tuning);
            if (fragmentHit) affected++;
            double jumpRange = tuning.get(s("EMBERBLADE_FRAGMENT_JUMP_RANGE"), 0);
            if (jumpRange > 0 && fragmentHit) {
                LivingEntity jump = targets(world, actor, candidate.getPos(), jumpRange, 64).stream()
                        .filter(next -> !used.contains(next.getUuid())).findFirst().orElse(null);
                if (jump != null) {
                    used.add(jump.getUuid());
                    if (dealAndRecord(execution, world, actor, stack, jump,
                            dealt * (float) tuning.get(s("EMBERBLADE_FRAGMENT_JUMP_MULTIPLIER"), .5), tuning)) {
                        affected++;
                    }
                }
            }
            if (--count <= 0) break;
        }
        return affected;
    }

    private static void applyRewards(UniqueAbilityExecution execution, ServerWorld world, LivingEntity actor,
                                     ItemStack stack, LivingEntity target, FireForgeMasteryTuning tuning,
                                     float charge, int elapsed, boolean fullCharge, int affected) {
        if (affected <= 0) return;
        State state = state(world, actor);
        long now = world.getTime();
        int duration = ireDuration(tuning.integer(s("STATUS_DURATION_TICKS"),
                        Config.uniqueEffects.emberblade.duration),
                tuning.integer(s("EMBERBLADE_IRE_DURATION_BONUS_TICKS"), 0));
        double bonusPoints = tuning.flag(1 << 18) ? tuning.get(s("CHANCE"), 10) : 0;
        double ireChance = ireChance(charge, Config.uniqueEffects.emberblade.chance, bonusPoints);
        float ireRoll = actor.getRandom().nextFloat();
        boolean proc = ireRoll < ireChance;
        UniqueAbilityApi.reportRoll(actor, FireForgeMasteryAbilities.EMBERBLADE_SHRAPNEL.id(),
                "EMBER_IRE_CHANCE", ireChance * 100, ireRoll * 100, proc);
        if (proc) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 0), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), actor);
            startMovementReward(state, actor, tuning, now, duration);
            world.playSoundFromEntity(null, actor, SoundRegistry.MAGIC_SWORD_SPELL_01.get(),
                    actor.getSoundCategory(), .5F, 2);
        }
        if (tuning.flag(1 << 10)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                    tuning.integer(s("EMBERBLADE_HASTE_DURATION_TICKS"), 80), 0), actor);
        }
        if (tuning.flag(1 << 9) && elapsed >= 20) {
            int quickdrawDuration = tuning.integer(s("EMBERBLADE_QUICKDRAW_DURATION_TICKS"), 60);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, quickdrawDuration, 0), actor);
            startMovementReward(state, actor, tuning, now, quickdrawDuration);
        }
        if (fullCharge && tuning.flag(1 << 14) && now >= state.ireRushReady) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                    tuning.integer(s("EMBERBLADE_IRE_RUSH_DURATION_TICKS"), 60), 0), actor);
            state.ireRushReady = now + tuning.integer(s("EMBERBLADE_FULL_BUFF_LOCKOUT_TICKS"), 100);
        }
        if (fullCharge && tuning.flag(1 << 26)) {
            int incarnateDuration = ireDuration(
                    tuning.integer(s("EMBERBLADE_INCARNATE_DURATION_TICKS"), 100),
                    tuning.integer(s("EMBERBLADE_IRE_DURATION_BONUS_TICKS"), 0));
            int amplifier = tuning.integer(s("STATUS_AMPLIFIER"), 1);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, incarnateDuration, amplifier), actor);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, incarnateDuration, amplifier), actor);
            startMovementReward(state, actor, tuning, now, incarnateDuration);
        }
        int flashCount = tuning.integer(s("EMBERBLADE_FLASHOVER_COUNT"), 0);
        if (flashCount > 0) {
            if (state.flashoverUntil < now) state.flashoverHits = 0;
            state.flashoverHits += affected;
            state.flashoverUntil = now + tuning.integer(s("EMBERBLADE_FLASHOVER_WINDOW_TICKS"), 100);
            if (state.flashoverHits >= flashCount) {
                float blast = HelperMethods.abilityScaledDamage("fire", actor, stack, 1F, 0)
                        * (float) tuning.get(s("EMBERBLADE_FLASHOVER_DAMAGE_MULTIPLIER"), .4);
                splash(execution, world, actor, stack, target,
                        tuning.get(s("EMBERBLADE_FLASHOVER_RADIUS"), 3),
                        tuning.integer(s("EMBERBLADE_FLASHOVER_TARGET_CAP"), 8), blast, tuning);
                state.flashoverHits = 0;
                state.flashoverUntil = 0;
            }
        }
    }

    private static void startMovementReward(State state, LivingEntity actor, FireForgeMasteryTuning tuning,
                                            long now, int buffDuration) {
        double required = tuning.get(s("EMBERBLADE_MOVE_DISTANCE"), 0);
        if (required <= 0) return;
        state.buffUntil = Math.max(state.buffUntil, now + buffDuration);
        state.moveRequired = required;
        state.nextHitDuration = tuning.integer(s("EMBERBLADE_NEXT_HIT_DURATION_TICKS"), 80);
        state.primedMultiplier = tuning.get(s("EMBERBLADE_NEXT_HIT_MULTIPLIER"), 1.15);
        state.distanceMoved = 0;
        state.lastMove = actor.getPos();
    }

    private static void applyReleaseMovement(LivingEntity actor, LivingEntity target,
                                             FireForgeMasteryTuning tuning, long now) {
        State state = state((ServerWorld) actor.getWorld(), actor);
        if (tuning.flag(1 << 16)) {
            Vec3d side = target.getPos().subtract(actor.getPos()).normalize().multiply(-1.2);
            actor.requestTeleport(target.getX() + side.x, target.getY(), target.getZ() + side.z);
            return;
        }
        double recoil = tuning.get(s("EMBERBLADE_RECOIL_DISTANCE"), 0);
        if (recoil <= 0) return;
        actor.setVelocity(actor.getRotationVec(1).negate().multiply(recoil).multiply(1, 0, 1));
        actor.velocityModified = true;
        state.fallProtectionUntil = now
                + tuning.integer(s("EMBERBLADE_FALL_PROTECTION_TICKS"), 0);
        if (tuning.flag(1 << 17)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    tuning.integer(s("EMBERBLADE_BLASTBACK_RESISTANCE_TICKS"), 40), 0), actor);
        }
    }

    private static int splash(UniqueAbilityExecution execution, ServerWorld world, LivingEntity actor,
                              ItemStack stack, LivingEntity center, double radius, int cap, float damage,
                              FireForgeMasteryTuning tuning) {
        if (radius <= 0 || cap <= 0 || damage <= 0) return 0;
        int affected = 0;
        for (LivingEntity target : targets(world, actor, center.getPos(), radius, cap + 1)) {
            if (target == center) continue;
            boolean hit = execution == null ? deal(world, actor, stack, target, damage)
                    : dealAndRecord(execution, world, actor, stack, target, damage, tuning);
            if (hit) affected++;
            if (affected >= cap) break;
        }
        return affected;
    }

    private static boolean dealAndRecord(UniqueAbilityExecution execution, ServerWorld world, LivingEntity actor,
                                         ItemStack stack, LivingEntity target, float damage,
                                         FireForgeMasteryTuning tuning) {
        boolean hit = deal(world, actor, stack, target, damage);
        if (hit && !target.isAlive()) {
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, FireForgeMasteryAbilities.KILL, target, 1, damage);
            if (tuning.flag(1 << 15)) {
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                        tuning.integer(s("EMBERBLADE_PURSUIT_SPEED_TICKS"), 40), 1), actor);
                State state = state(world, actor);
                state.fallProtectionUntil = Math.max(state.fallProtectionUntil, world.getTime()
                        + tuning.integer(s("EMBERBLADE_FALL_PROTECTION_TICKS"), 20));
            }
        }
        return hit;
    }

    private static boolean deal(ServerWorld world, LivingEntity actor, ItemStack stack,
                                LivingEntity target, float damage) {
        DamageSource source = actor instanceof PlayerEntity player
                ? world.getDamageSources().playerAttack(player) : world.getDamageSources().mobAttack(actor);
        float adjusted = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage);
        return HelperMethods.damageThroughIframes(target, source, adjusted);
    }

    private static List<LivingEntity> targets(ServerWorld world, LivingEntity actor, Vec3d center,
                                              double radius, int cap) {
        if (radius <= 0 || cap <= 0) return List.of();
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        return world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(target -> target != actor && validTarget(actor, target)
                        && target.getPos().squaredDistanceTo(center) <= radius * radius)
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.min(64, cap)).toList();
    }

    private static void bankInterrupted(State state, FireForgeMasteryTuning tuning, int elapsed,
                                        int channelTicks, long now) {
        int duration = tuning.integer(s("EMBERBLADE_BANK_DURATION_TICKS"), 0);
        if (duration <= 0 || elapsed < 40) return;
        state.interruptedBank = interruptedBank(elapsed, channelTicks,
                tuning.get(s("EMBERBLADE_BANK_MULTIPLIER"), .5), 40);
        state.interruptedBankUntil = now + duration;
    }

    static double fullChargeBonus(boolean finalWindow, double multiplier) {
        return finalWindow ? Math.max(0, multiplier) : 1;
    }

    private static void applyChannelSlow(LivingEntity actor, Channel channel) {
        StatusEffectInstance existing = actor.getStatusEffect(StatusEffects.SLOWNESS);
        channel.previousSlowness = existing == null ? null : new StatusEffectInstance(existing);
        channel.slowed = true;
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                channel.channelTicks + CHANNEL_SLOW_GRACE_TICKS, 1), actor);
    }

    // Only clears the channel's own Slowness, restoring whatever the wielder already had.
    private static void removeChannelSlow(LivingEntity actor, Channel channel) {
        if (channel == null || !channel.slowed) return;
        channel.slowed = false;
        StatusEffectInstance current = actor.getStatusEffect(StatusEffects.SLOWNESS);
        if (current != null && current.getAmplifier() == 1
                && current.getDuration() <= channel.channelTicks + CHANNEL_SLOW_GRACE_TICKS) {
            actor.removeStatusEffect(StatusEffects.SLOWNESS);
            if (channel.previousSlowness != null) actor.addStatusEffect(channel.previousSlowness, actor);
        }
    }

    private static void cancelChannel(LivingEntity actor, State state) {
        Channel channel = state.channel;
        state.channel = null;
        removeChannelSlow(actor, channel);
        if (channel != null && !channel.execution.isTerminal()) UniqueAbilityApi.cancel(channel.execution);
    }

    private static void ignite(LivingEntity target, int ticks) {
        if (ticks > 0) target.setOnFireFor(Math.max(1, (int) Math.ceil(ticks / 20.0)));
    }

    private static boolean validTarget(LivingEntity actor, LivingEntity target) {
        return actor != null && target != null && target.isAlive()
                && HelperMethods.checkAbilityTarget(target, actor);
    }

    private static boolean isActivelyChanneling(LivingEntity actor, Channel channel) {
        ItemStack held = actor.getStackInHand(channel.hand);
        if (!ItemStack.areItemsEqual(held, channel.stack)) return false;
        if (actor.isUsingItem() && actor.getActiveHand() == channel.hand) return true;
        return actor instanceof ServerPlayerEntity player
                && PlayerWeaponAbilityChannelManager.isChanneling(player, channel.hand, channel.stack.getItem());
    }

    private static double aimAngle(LivingEntity actor, LivingEntity target) {
        Vec3d direction = target.getEyePos().subtract(actor.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(Math.clamp(
                actor.getRotationVec(1).normalize().dotProduct(direction), -1, 1)));
    }

    static int channelTicks(FireForgeMasteryTuning tuning) {
        return Math.max(1, tuning.integer(s("EMBERBLADE_CHANNEL_TICKS"), BASE_CHANNEL_TICKS));
    }

    static int elapsedTicks(long now, long startedAt, int channelTicks) {
        return (int) Math.clamp(now - startedAt, 0, channelTicks);
    }

    static float chargeRatio(int elapsed, int channelTicks, double banked) {
        return (float) Math.clamp(elapsed / (double) Math.max(1, channelTicks) + banked, 0, 1);
    }

    static boolean inFullChargeWindow(int elapsed, int channelTicks, int windowTicks) {
        return windowTicks > 0 && elapsed >= Math.max(0, channelTicks - windowTicks);
    }

    static float interruptedBank(int elapsed, int channelTicks, double multiplier, int minimumTicks) {
        if (elapsed < minimumTicks) return 0;
        return chargeRatio(elapsed, channelTicks, 0) * (float) Math.clamp(multiplier, 0, 1);
    }

    static double ireChance(float charge, int baseChance, double additivePercentagePoints) {
        double denominator = 250 - Math.clamp(charge, 0, 1) * 100;
        return Math.clamp(baseChance / denominator + additivePercentagePoints / 100, 0, 1);
    }

    static int ireDuration(int baseTicks, int bonusTicks) {
        return Math.max(0, baseTicks) + Math.max(0, bonusTicks);
    }

    private static float validBank(float value, long expiresAt, long now) {
        return expiresAt >= now ? value : 0;
    }

    private static void prune(State state, long now) {
        if (state.interruptedBankUntil < now) state.interruptedBank = 0;
        if (state.flameBankUntil < now) state.flameBank = 0;
        if (state.nextHitUntil < now) state.nextHitMultiplier = 1;
        if (state.buffUntil < now) {
            state.moveRequired = 0;
            state.lastMove = null;
            state.distanceMoved = 0;
        }
        if (state.flashoverUntil < now) state.flashoverHits = 0;
    }

    private static boolean isIdle(State state, long now) {
        return state.channel == null && state.interruptedBank <= 0 && state.flameBank <= 0
                && state.fallProtectionUntil < now && state.ireRushReady < now
                && state.flashoverHits <= 0 && state.moveRequired <= 0
                && state.nextHitMultiplier <= 1;
    }

    private static State state(ServerWorld world, LivingEntity actor) {
        return STATES.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(actor.getUuid(), ignored -> new State());
    }

    private static State get(ServerWorld world, LivingEntity actor) {
        if (world == null || actor == null) return null;
        Map<UUID, State> states = STATES.get(world);
        return states == null ? null : states.get(actor.getUuid());
    }

    private static FireForgeMasteryTuning.Setting s(String name) {
        return FireForgeMasteryTuning.Setting.valueOf(name);
    }

    private static double tuned(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                FireForgeMasteryTuning.Setting legacy, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(legacy, fallback);
    }

    private static int tunedInteger(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                    FireForgeMasteryTuning.Setting legacy, int fallback) {
        return tuning.has(scoped) ? tuning.integer(scoped, fallback) : tuning.integer(legacy, fallback);
    }

    private static final class Channel {
        private final UniqueAbilityExecution execution;
        private final FireForgeMasteryTuning tuning;
        private final ItemStack stack;
        private final Hand hand;
        private final long startedAt;
        private final int channelTicks;
        private boolean released;
        private boolean slowed;
        private StatusEffectInstance previousSlowness;

        private Channel(UniqueAbilityExecution execution, FireForgeMasteryTuning tuning, ItemStack stack,
                        Hand hand, long startedAt, int channelTicks) {
            this.execution = execution;
            this.tuning = tuning;
            this.stack = stack;
            this.hand = hand;
            this.startedAt = startedAt;
            this.channelTicks = channelTicks;
        }
    }

    private static final class State {
        private Channel channel;
        private float interruptedBank;
        private long interruptedBankUntil;
        private float flameBank;
        private long flameBankUntil;
        private long fallProtectionUntil;
        private long ireRushReady;
        private int flashoverHits;
        private long flashoverUntil;
        private long buffUntil;
        private double moveRequired;
        private int nextHitDuration;
        private double primedMultiplier = 1;
        private double distanceMoved;
        private Vec3d lastMove;
        private long nextHitUntil;
        private double nextHitMultiplier = 1;
    }
}
