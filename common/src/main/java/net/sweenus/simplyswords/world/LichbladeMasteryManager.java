package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryTuning;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class LichbladeMasteryManager {
    private static final Identifier CHANNEL_MOVEMENT_SLOW_ID =
            Identifier.of("simplyswords", "lichblade_channel_slow");
    private static final int AURA_RESOLVE_INTERVAL = 5;
    private static final Map<UUID, ChannelState> CHANNELS = new HashMap<>();
    private static final Map<PulseKey, PulseHistory> PULSE_HISTORY = new HashMap<>();
    private static final Map<UUID, Long> AURA_ACTIVE = new HashMap<>();

    private LichbladeMasteryManager() {
    }

    public static LivingEntity beginChannel(ServerWorld world, PlayerEntity owner, ItemStack stack) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(LongPathFinalFormsMasteryAbilities.LICHBLADE_CHANNEL,
                UniqueAbilityContext.passive(world, stack, owner, null, null), tuning -> tuning
                        .set(LongPathFinalFormsMasteryAbilities.TUNING, LongPathFinalFormsMasteryTuning.EMPTY)
                        .set(LongPathFinalFormsMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.lichblade.cooldown));
        UniqueAbilityApi.takeStartedExecution();
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        LivingEntity target = StealSwordItem.findLenientTarget(owner,
                tuning.get(s("ACQUISITION_RANGE"), Config.uniqueEffects.lichblade.range));
        if (target == null) {
            UniqueAbilityApi.cancel(execution);
            return null;
        }
        if (tuning.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return target;
        }
        UniqueAbilityApi.start(execution);
        evictFinishedChannels(world);
        CHANNELS.put(owner.getUuid(), new ChannelState(execution, stack, target, world.getTime(),
                world.getRegistryKey()));
        applyChannelMovementPenalty(owner, tuning);
        return target;
    }

    public static int maxUseTime(LivingEntity owner) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null) return Config.uniqueEffects.lichblade.duration * 2;
        return LongPathFinalFormsMasteryAbilities.tuning(state.execution).integer(s("DURATION_TICKS"),
                Config.uniqueEffects.lichblade.duration) * 2;
    }

    public static boolean tickChannel(ServerWorld world, LivingEntity owner, ItemStack stack) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null || state.execution.isTerminal()) return false;
        if (owner.getEquippedStack(EquipmentSlot.MAINHAND) != stack || !owner.isAlive()) {
            finish(owner, stack, false);
            return true;
        }
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
        long elapsed = world.getTime() - state.startedAt;
        int duration = tuning.integer(s("DURATION_TICKS"), Config.uniqueEffects.lichblade.duration);
        LivingEntity target = entity(world, state.targetId);
        if (tuning.flag(32) && !valid(target, owner)) {
            finish(owner, stack, true);
            owner.stopUsingItem();
            return true;
        }
        if (!valid(target, owner)) target = retargetOrReturn(world, owner, state, tuning, elapsed);
        if (target == null) {
            finish(owner, stack, true);
            owner.stopUsingItem();
            return true;
        }
        int moveInterval = Math.max(1, tuning.integer(s("CLOUD_MOVE_INTERVAL_TICKS"), 5));
        if (elapsed % moveInterval == 0) moveCloud(state, target, tuning);
        int pulseInterval = pulseInterval(tuning.flag(8), elapsed,
                tuning.integer(s("FAST_PULSE_AFTER_TICKS"), 60),
                tuning.integer(s("FAST_PULSE_INTERVAL_TICKS"), 4));
        if (!state.returning && elapsed % pulseInterval == 0) {
            pulse(world, owner, state, tuning, target, true);
            spawnCloudParticles(world, state.cloud, tuning.get(s("RADIUS"),
                    Config.uniqueEffects.lichblade.radius));
        }
        if (elapsed >= duration) {
            state.returning = true;
            state.targetId = owner.getUuid();
        }
        if (state.returning && state.cloud.squaredDistanceTo(owner.getPos())
                <= Math.pow(tuning.get(s("RADIUS"), Config.uniqueEffects.lichblade.radius), 2)) {
            finish(owner, stack, true);
            owner.stopUsingItem();
        }
        return true;
    }

    static int pulseInterval(boolean unceasing, long elapsed, int after, int fastInterval) {
        return unceasing && elapsed >= after ? Math.max(1, fastInterval) : 5;
    }

    // Recalls the souls when Soul Recall is owned and the channel is released before it would end.
    public static boolean releaseChannel(LivingEntity owner, ItemStack stack) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null) return false;
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
        if (tuning.flag(65536) && !state.returning && owner.getWorld() instanceof ServerWorld world) {
            state.recalled = true;
            recallBurst(world, owner, state, tuning);
        }
        return finish(owner, stack, true);
    }

    public static boolean finish(LivingEntity owner, ItemStack stack, boolean resolve) {
        ChannelState state = CHANNELS.remove(owner.getUuid());
        if (state == null) return false;
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
        removeChannelMovementPenalty(owner);
        if (resolve) resolveAbsorption(owner, state, tuning);
        SimplySwordsAPI.setWeaponCooldown(owner, stack, channelCooldown(state, tuning));
        if (stack != null && !stack.isEmpty()) {
            stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), null);
            stack.set(ComponentTypeRegistry.TARGETED_LOCATION.get(), null);
        }
        if (resolve) UniqueAbilityApi.finish(state.execution, LongPathFinalFormsMasteryAbilities.FINISH, state.hits);
        else UniqueAbilityApi.cancel(state.execution);
        return true;
    }

    private static int channelCooldown(ChannelState state, LongPathFinalFormsMasteryTuning tuning) {
        return cooldownTicks(state.execution.cooldownTicks(Config.uniqueEffects.lichblade.cooldown),
                tuning.get(s("COOLDOWN_MULTIPLIER"), 1),
                state.recalled ? tuning.integer(s("RECALL_COOLDOWN_TICKS"), 100) : 0,
                tuning.flag(2048) ? state.successfulHeals : 0,
                tuning.integer(s("COOLDOWN_PER_SIPHON_TICKS"), 10),
                tuning.integer(s("COOLDOWN_PENALTY_CAP_TICKS"), 200));
    }

    static int cooldownTicks(int base, double multiplier, int recallTicks, int siphons,
                             int perSiphon, int penaltyCap) {
        int cooldown = (int) Math.round(base * multiplier) + Math.max(0, recallTicks);
        if (siphons > 0) cooldown += Math.min(Math.max(0, penaltyCap), siphons * Math.max(0, perSiphon));
        return Math.max(0, cooldown);
    }

    public static boolean tickPassive(ServerWorld world, LivingEntity owner, ItemStack stack) {
        if (owner.age % AURA_RESOLVE_INTERVAL != 0) return auraHandled(owner);
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(LongPathFinalFormsMasteryAbilities.LICHBLADE_AURA,
                UniqueAbilityContext.passive(world, stack, owner, null, null), tuning ->
                        tuning.set(LongPathFinalFormsMasteryAbilities.TUNING, LongPathFinalFormsMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        if (tuning.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            AURA_ACTIVE.remove(owner.getUuid());
            return false;
        }
        AURA_ACTIVE.put(owner.getUuid(), world.getTime());
        int interval = auraInterval(tuning);
        if (owner.age % interval != 0 || owner.getEquippedStack(EquipmentSlot.MAINHAND) != stack
                || owner.isUsingItem()) {
            UniqueAbilityApi.cancel(execution);
            return true;
        }
        UniqueAbilityApi.start(execution);
        ChannelState state = new ChannelState(execution, stack, owner, world.getTime(), world.getRegistryKey());
        state.cloud = owner.getPos();
        pulse(world, owner, state, tuning, owner, false);
        spawnAuraParticles(world, owner, tuning.get(s("RADIUS"), Config.uniqueEffects.lichblade.radius));
        UniqueAbilityApi.finish(execution, LongPathFinalFormsMasteryAbilities.FINISH, state.hits);
        return true;
    }

    // The aura only resolves every AURA_RESOLVE_INTERVAL ticks, so tuned intervals snap to that step.
    private static int auraInterval(LongPathFinalFormsMasteryTuning tuning) {
        return auraInterval(tuning.integer(s("AURA_INTERVAL_TICKS"), 35));
    }

    static int auraInterval(int tuned) {
        return Math.max(AURA_RESOLVE_INTERVAL,
                Math.round(tuned / (float) AURA_RESOLVE_INTERVAL) * AURA_RESOLVE_INTERVAL);
    }

    private static boolean auraHandled(LivingEntity owner) {
        Long seen = AURA_ACTIVE.get(owner.getUuid());
        return seen != null && owner.getWorld().getTime() - seen <= AURA_RESOLVE_INTERVAL * 2L;
    }

    public static boolean activateOnce(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(LongPathFinalFormsMasteryAbilities.LICHBLADE_CHANNEL,
                UniqueAbilityContext.active(context), tuning -> tuning
                        .set(LongPathFinalFormsMasteryAbilities.TUNING, LongPathFinalFormsMasteryTuning.EMPTY)
                        .set(LongPathFinalFormsMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.lichblade.cooldown));
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        if (tuning.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        ChannelState state = new ChannelState(execution, context.stack(), context.target(),
                context.world().getTime(), context.world().getRegistryKey());
        state.cloud = context.target().getPos();
        UniqueAbilityApi.start(execution);
        pulse(context.world(), context.actor(), state, tuning, context.target(), true);
        resolveAbsorption(context.actor(), state, tuning);
        UniqueAbilityApi.finish(execution, LongPathFinalFormsMasteryAbilities.FINISH, state.hits);
        return true;
    }

    public static void onOwnerDamaged(LivingEntity owner) {
        ChannelState state = CHANNELS.get(owner.getUuid());
        if (state == null || state.interruptIgnored) return;
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(state.execution);
        if (tuning.flag(4096)) {
            state.interruptIgnored = true;
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    tuning.integer(s("INTERRUPT_RESISTANCE_TICKS"), 20), 0), owner);
        }
    }

    public static void tickOwner(LivingEntity owner) {
        MasteryAbsorptionTracker.tick(owner);
    }

    private static void pulse(ServerWorld world, LivingEntity owner, ChannelState state,
                              LongPathFinalFormsMasteryTuning tuning, LivingEntity primary, boolean siphon) {
        double radius = tuning.get(s("RADIUS"), Config.uniqueEffects.lichblade.radius);
        int cap = tuning.integer(s("TARGET_CAP"), tuning.flag(16) ? 32 : 24);
        List<LivingEntity> targets;
        if (tuning.flag(32)) {
            targets = valid(primary, owner) ? List.of(primary) : List.of();
        } else {
            targets = nearest(world, owner, state.cloud, radius, Math.clamp(cap, 0, 64));
        }
        float base = HelperMethods.abilityScaledDamage("soul", owner, state.stack,
                Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling);
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        if (tuning.flag(2)) multiplier *= 1 + Math.min(tuning.get(s("BONUS_CAP"), .24),
                Math.max(0, targets.size() - 1) * tuning.get(s("PER_TARGET_BONUS"), .03));
        multiplier *= retargetPenalty(state, tuning);
        for (LivingEntity target : targets) {
            if (!valid(target, owner)) continue;
            DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, state.stack, target, source,
                    base * (float) multiplier);
            float before = target.getHealth();
            if (!target.damage(source, damage)) continue;
            state.hits++;
            recordPulse(world, target, owner, world.getTime(), tuning);
            addCharge(state, target, world.getTime(), tuning);
            if (siphon) heal(owner, state, tuning);
            UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.PULSE,
                    target, 1, damage);
            if (before > 0 && target.isDead()) {
                UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.KILL,
                        target, 1, damage);
                if (tuning.flag(4)) deathBurst(world, owner, state, tuning, target.getPos(), damage);
            }
        }
        world.playSoundFromEntity(null, owner, SoundRegistry.DARK_SWORD_BLOCK.get(),
                owner.getSoundCategory(), .1F, .3F);
    }

    // Only Wandering Phylactery pays for the targets it chains into.
    private static double retargetPenalty(ChannelState state, LongPathFinalFormsMasteryTuning tuning) {
        return retargetPenalty(state.retargets, tuning.flag(32768),
                tuning.get(s("RETARGET_DAMAGE_PENALTY"), .15), tuning.get(s("RETARGET_DAMAGE_FLOOR"), .4));
    }

    static double retargetPenalty(int retargets, boolean wandering, double penalty, double floor) {
        if (retargets <= 0 || !wandering) return 1;
        return Math.max(floor, 1 - retargets * penalty);
    }

    private static List<LivingEntity> nearest(ServerWorld world, LivingEntity owner, Vec3d center,
                                              double radius, int limit) {
        Box box = new Box(center.x + radius, center.y + radius, center.z + radius,
                center.x - radius, center.y - radius, center.z - radius);
        return world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(target -> HelperMethods.checkFriendlyFire(target, owner))
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.max(0, limit)).toList();
    }

    private static void recallBurst(ServerWorld world, LivingEntity owner, ChannelState state,
                                    LongPathFinalFormsMasteryTuning tuning) {
        double radius = tuning.get(s("RECALL_RADIUS"), 4);
        double multiplier = tuning.get(s("DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("RECALL_DAMAGE_MULTIPLIER"), 1.25) * retargetPenalty(state, tuning);
        float base = HelperMethods.abilityScaledDamage("soul", owner, state.stack,
                Config.uniqueEffects.lichblade.damageScaling, Config.uniqueEffects.lichblade.spellScaling)
                * (float) multiplier;
        for (LivingEntity target : nearest(world, owner, state.cloud, radius, 24)) {
            DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
            if (target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, state.stack,
                    target, source, base))) {
                state.hits++;
            }
        }
        spawnCloudParticles(world, state.cloud, radius);
    }

    private static void recordPulse(ServerWorld world, LivingEntity target, LivingEntity owner, long tick,
                                    LongPathFinalFormsMasteryTuning tuning) {
        if (!tuning.flag(1)) return;
        PulseKey key = new PulseKey(world.getRegistryKey(), target.getUuid());
        PulseHistory history = PULSE_HISTORY.computeIfAbsent(key, ignored -> new PulseHistory());
        if (history.ownerId != null && owner.getUuid().equals(history.ownerId)
                && tick - history.lastTick <= tuning.integer(s("REPEAT_WINDOW_TICKS"), 40)) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    tuning.integer(s("SLOW_DURATION_TICKS"), 60), 0), owner);
        }
        history.ownerId = owner.getUuid();
        history.lastTick = tick;
        PULSE_HISTORY.entrySet().removeIf(entry -> entry.getKey().world().equals(world.getRegistryKey())
                && tick - entry.getValue().lastTick > 200);
    }

    private static void addCharge(ChannelState state, LivingEntity target, long tick,
                                  LongPathFinalFormsMasteryTuning tuning) {
        int amount = 1;
        if (tuning.flag(64)) {
            long ready = state.chargeLocks.getOrDefault(target.getUuid(), 0L);
            if (ready > tick) return;
            state.chargeLocks.put(target.getUuid(), tick + tuning.integer(s("CHARGE_LOCKOUT_TICKS"), 10));
            amount = tuning.integer(s("CHARGE_PER_HIT"), 2);
        }
        state.charge += amount;
        state.chargeLocks.entrySet().removeIf(entry -> entry.getValue() <= tick);
    }

    private static void heal(LivingEntity owner, ChannelState state, LongPathFinalFormsMasteryTuning tuning) {
        int chance = tuning.integer(s("CHANCE"), 9);
        boolean exact = tuning.has(s("CHANCE"));
        boolean success = chance >= 100 || chance > 0 && (exact
                ? owner.getRandom().nextInt(100) < chance : owner.getRandom().nextInt(100) <= chance - 1);
        if (!success) return;
        float amount = (float) tuning.get(s("HEAL_AMOUNT"), Config.uniqueEffects.lichblade.heal);
        float missing = Math.max(0, owner.getMaxHealth() - owner.getHealth());
        owner.heal(amount);
        if (tuning.flag(256) && amount > missing) {
            float granted = overhealGrant(amount, missing, (float) tuning.get(s("HEAL_MULTIPLIER"), .5),
                    (float) tuning.get(s("TEMP_ABSORPTION_CAP"), 4), state.overhealAbsorption);
            if (granted > 0) {
                state.overhealAbsorption += granted;
                grantTemporaryAbsorption(owner, granted,
                        tuning.integer(s("OVERHEAL_ABSORPTION_TICKS"), 80));
            }
        }
        state.successfulHeals++;
    }

    private static void resolveAbsorption(LivingEntity owner, ChannelState state, LongPathFinalFormsMasteryTuning tuning) {
        if (tuning.flag(2048)) return;
        float cap = (float) tuning.get(s("ABSORPTION_CAP"), Config.uniqueEffects.lichblade.absorptionCap);
        if (tuning.flag(1024)) {
            float conversion = Math.min(cap,
                    state.charge / (float) Math.max(1, tuning.integer(s("BASTION_CHARGE_PER_ABSORPTION"), 2)));
            grantTemporaryAbsorption(owner, conversion, tuning.integer(s("BASTION_ABSORPTION_TICKS"), 160));
            return;
        }
        double interest = tuning.flag(512) ? interestMultiplier(state.charge,
                tuning.integer(s("INTEREST_CHARGE_STEP"), 10), tuning.get(s("INTEREST_PER_STEP"), .1),
                tuning.get(s("INTEREST_CAP"), .4)) : 1;
        float conversion = state.charge / 2F * (float) interest;
        float granted = Math.min(conversion, cap);
        owner.setAbsorptionAmount(Math.min(Config.uniqueEffects.abilityAbsorptionCap,
                owner.getAbsorptionAmount() + granted));
        if (tuning.flag(128)) applyOverflowResistance(owner, state, tuning, cap, granted);
    }

    // Overflowing Spirit only pays out once the absorption grant is capped, on the charge beyond it.
    private static void applyOverflowResistance(LivingEntity owner, ChannelState state,
                                                LongPathFinalFormsMasteryTuning tuning, float cap, float granted) {
        int duration = overflowResistanceTicks(cap, granted, state.charge,
                tuning.integer(s("RESISTANCE_CHARGE_STEP"), 4),
                tuning.integer(s("RESISTANCE_STEP_TICKS"), 40),
                tuning.integer(s("RESISTANCE_DURATION_CAP_TICKS"), 120));
        if (duration <= 0) return;
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0), owner);
    }

    static int overflowResistanceTicks(float cap, float granted, int charge, int step,
                                       int stepTicks, int durationCap) {
        if (cap <= 0 || granted < cap) return 0;
        int additional = Math.max(0, charge - (int) Math.ceil(cap * 2));
        int steps = additional / Math.max(1, step);
        if (steps <= 0) return 0;
        return Math.max(0, Math.min(durationCap, steps * stepTicks));
    }

    static float overhealGrant(float amount, float missing, float multiplier, float cap, float already) {
        float converted = (amount - Math.max(0, missing)) * multiplier;
        if (converted <= 0) return 0;
        return Math.max(0, Math.min(converted, cap - already));
    }

    static double interestMultiplier(int charge, int step, double perStep, double cap) {
        return 1 + Math.min(cap, charge / Math.max(1, step) * perStep);
    }

    static int retargetMaximum(boolean wandering, boolean selection, long elapsed, int window,
                               int wanderingCap, int selectionCap) {
        if (wandering) return wanderingCap;
        return selection && elapsed < window ? selectionCap : 0;
    }

    static double returnSpeed(boolean returning, boolean fromDeath, boolean returningShade, double speed) {
        return returning && fromDeath && returningShade ? speed : 1;
    }

    private static void grantTemporaryAbsorption(LivingEntity owner, float amount, int ticks) {
        MasteryAbsorptionTracker.grant(owner, Math.min(amount,
                Config.uniqueEffects.abilityAbsorptionCap - owner.getAbsorptionAmount()), ticks, 0);
    }

    private static void deathBurst(ServerWorld world, LivingEntity owner, ChannelState state,
                                   LongPathFinalFormsMasteryTuning tuning, Vec3d center, float pulseDamage) {
        double radius = tuning.get(s("DEATH_BURST_RADIUS"), 3);
        int cap = tuning.integer(s("DEATH_BURST_TARGET_CAP"), 4);
        for (LivingEntity target : nearest(world, owner, center, radius, cap)) {
            DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
            target.damage(source, pulseDamage * (float) tuning.get(s("DEATH_BURST_DAMAGE_MULTIPLIER"), .35));
        }
    }

    private static LivingEntity retargetOrReturn(ServerWorld world, LivingEntity owner, ChannelState state,
                                                  LongPathFinalFormsMasteryTuning tuning, long elapsed) {
        int maximum = retargetMaximum(tuning.flag(32768), tuning.flag(16384), elapsed,
                tuning.integer(s("RETARGET_WINDOW_TICKS"), 80),
                tuning.integer(s("RETARGET_CAP"), 4), tuning.integer(s("RETARGET_CAP"), 1));
        if (state.retargets < maximum) {
            double range = tuning.get(s("RETARGET_RANGE"), 8);
            Box box = new Box(state.cloud.x + range, state.cloud.y + range, state.cloud.z + range,
                    state.cloud.x - range, state.cloud.y - range, state.cloud.z - range);
            LivingEntity target = world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                    .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                    .filter(entity -> valid(entity, owner))
                    .sorted(Comparator.comparingDouble((LivingEntity entity) -> entity.getPos().squaredDistanceTo(state.cloud))
                            .thenComparing(entity -> entity.getUuid().toString())).findFirst().orElse(null);
            if (target != null) {
                state.targetId = target.getUuid();
                state.retargets++;
                return target;
            }
        }
        state.returning = true;
        state.returnedFromDeath = true;
        state.targetId = owner.getUuid();
        return owner;
    }

    private static void moveCloud(ChannelState state, LivingEntity target, LongPathFinalFormsMasteryTuning tuning) {
        Vec3d delta = target.getPos().subtract(state.cloud);
        double speed = returnSpeed(state.returning, state.returnedFromDeath, tuning.flag(8192),
                tuning.get(s("SPEED"), 1.5));
        if (delta.lengthSquared() <= speed * speed) state.cloud = target.getPos();
        else state.cloud = state.cloud.add(delta.normalize().multiply(speed));
    }

    private static void applyChannelMovementPenalty(LivingEntity owner, LongPathFinalFormsMasteryTuning tuning) {
        if (!tuning.has(s("MOVEMENT_MULTIPLIER"))) return;
        EntityAttributeInstance movementSpeed = owner.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null) return;
        double reduction = -MathHelper.clamp(1 - tuning.get(s("MOVEMENT_MULTIPLIER"), 1), 0.0, 0.99);
        if (reduction == 0) return;
        movementSpeed.removeModifier(CHANNEL_MOVEMENT_SLOW_ID);
        movementSpeed.addTemporaryModifier(new EntityAttributeModifier(CHANNEL_MOVEMENT_SLOW_ID, reduction,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeChannelMovementPenalty(LivingEntity owner) {
        EntityAttributeInstance movementSpeed = owner.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) movementSpeed.removeModifier(CHANNEL_MOVEMENT_SLOW_ID);
    }

    private static void spawnCloudParticles(ServerWorld world, Vec3d center, double radius) {
        int span = Math.max(1, (int) Math.round(radius) * 2);
        double xPos = center.x - (radius + 1);
        double zPos = center.z - (radius + 1);
        for (int i = span; i > 0; i--) {
            for (int j = span; j > 0; j--) {
                float choose = (float) Math.random();
                HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM,
                        xPos + i + choose, center.y, zPos + j + choose, choose / 3, -0.3, choose / 3);
                choose = (float) Math.random();
                HelperMethods.spawnParticle(world, ParticleTypes.SOUL,
                        xPos + i + choose, center.y, zPos + j + choose, choose / 3, 0, choose / 3);
            }
        }
    }

    private static void spawnAuraParticles(ServerWorld world, LivingEntity owner, double radius) {
        int span = Math.max(1, (int) Math.round(radius) * 2);
        double xPos = owner.getX() - (radius + 1);
        double yPos = owner.getY();
        double zPos = owner.getZ() - (radius + 1);
        for (int i = span; i > 0; i--) {
            for (int j = span; j > 0; j--) {
                float choose = (float) Math.random();
                HelperMethods.spawnParticle(world, ParticleTypes.SCULK_SOUL,
                        xPos + i + choose, yPos, zPos + j + choose, 0, 0.1, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.SOUL,
                        xPos + i + choose, yPos + 0.1, zPos + j + choose, 0, 0, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM,
                        xPos + i + choose, yPos + 2, zPos + j + choose, 0, 0, 0);
            }
        }
    }

    private static LivingEntity entity(ServerWorld world, UUID uuid) {
        Entity entity = world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean valid(LivingEntity target, LivingEntity owner) {
        return target != null && target.isAlive() && (target == owner || HelperMethods.checkFriendlyFire(target, owner));
    }

    // Only drops channels that can no longer progress; a live channel is never evicted.
    private static void evictFinishedChannels(ServerWorld world) {
        Iterator<Map.Entry<UUID, ChannelState>> iterator = CHANNELS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ChannelState> entry = iterator.next();
            ChannelState state = entry.getValue();
            if (state.execution.isTerminal()) {
                iterator.remove();
                continue;
            }
            if (!state.world.equals(world.getRegistryKey())) continue;
            if (world.getEntity(entry.getKey()) == null) {
                UniqueAbilityApi.cancel(state.execution);
                iterator.remove();
            }
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        RegistryKey<World> key = world.getRegistryKey();
        CHANNELS.entrySet().removeIf(entry -> {
            if (!entry.getValue().world.equals(key)) return false;
            Entity owner = world.getEntity(entry.getKey());
            if (owner instanceof LivingEntity living) removeChannelMovementPenalty(living);
            UniqueAbilityApi.cancel(entry.getValue().execution);
            return true;
        });
        PULSE_HISTORY.keySet().removeIf(pulseKey -> pulseKey.world().equals(key));
        AURA_ACTIVE.clear();
        MasteryAbsorptionTracker.clear(world);
    }

    public static void clearAll() {
        CHANNELS.values().forEach(state -> UniqueAbilityApi.cancel(state.execution));
        CHANNELS.clear();
        PULSE_HISTORY.clear();
        MasteryAbsorptionTracker.clearAll();
        AURA_ACTIVE.clear();
    }

    private static LongPathFinalFormsMasteryTuning.Setting s(String name) {
        return LongPathFinalFormsMasteryTuning.Setting.valueOf(name);
    }

    private record PulseKey(RegistryKey<World> world, UUID target) {
        private PulseKey {
            Objects.requireNonNull(world);
            Objects.requireNonNull(target);
        }
    }

    private static final class ChannelState {
        private final UniqueAbilityExecution execution;
        private final ItemStack stack;
        private final long startedAt;
        private final RegistryKey<World> world;
        private final Map<UUID, Long> chargeLocks = new HashMap<>();
        private UUID targetId;
        private Vec3d cloud;
        private boolean returning;
        private boolean returnedFromDeath;
        private boolean interruptIgnored;
        private boolean recalled;
        private int retargets;
        private int charge;
        private int successfulHeals;
        private float overhealAbsorption;
        private int hits;

        private ChannelState(UniqueAbilityExecution execution, ItemStack stack,
                             LivingEntity target, long startedAt, RegistryKey<World> world) {
            this.execution = execution;
            this.stack = stack.copy();
            this.startedAt = startedAt;
            this.world = world;
            this.targetId = target.getUuid();
            this.cloud = target.getPos();
        }
    }

    private static final class PulseHistory {
        private UUID ownerId;
        private long lastTick;
    }

}
