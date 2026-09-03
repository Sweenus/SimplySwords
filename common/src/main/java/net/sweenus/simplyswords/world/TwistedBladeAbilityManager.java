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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryTuning;
import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.TwistedBladeCrescendoVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TwistedBladeAbilityManager {

    private static final int VISUAL_LIFETIME = 8;
    private static final Identifier MASTERY_ATTACK_SPEED = Identifier.of("simplyswords", "twisted_mastery_attack_speed");
    private static final Map<ServerWorld, Map<UUID, WielderState>> WIELDER_STATES = new HashMap<>();

    private TwistedBladeAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, WielderState> states = WIELDER_STATES.get(world);
        return states != null && !states.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, WielderState> states = WIELDER_STATES.get(world);
        if (states == null || states.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, WielderState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WielderState> entry = iterator.next();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity actor) || !actor.isAlive() || actor.isRemoved()) {
                cancelState(entry.getValue());
                iterator.remove();
                continue;
            }

            WielderState state = entry.getValue();
            if (state.armed != null && now >= state.armed.expiresAt) {
                UniqueAbilityApi.finish(state.armed.execution, DeathShadowBloodMasteryAbilities.FINISH,
                        state.armed.affectedTargets);
                state.armed = null;
                state.hitCounter = 0;
            }
            state.pendingCrescendos.removeIf(pending -> {
                if (now < pending.at) return false;
                Entity targetEntity = world.getEntity(pending.targetId);
                Entity ownerEntity = pending.sourceOwnerId == null ? null : world.getEntity(pending.sourceOwnerId);
                if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                    triggerCrescendo(world, pending.stack, actor,
                            ownerEntity instanceof LivingEntity owner ? owner : null, target, false,
                            true, pending.stacks, state, pending.tuning);
                }
                return true;
            });
            if (state.ferocityRefundAt > 0 && now >= state.ferocityRefundAt) {
                int stacks = Math.min(maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks,
                                state.ferocityTuning),
                        getFerocityStacks(actor) + state.ferocityRefundStacks);
                setFerocityStacks(actor, stacks,
                        ferocityDuration(Config.uniqueEffects.twisted_blade.duration, state.ferocityTuning),
                        state.ferocityTuning);
                state.ferocityRefundAt = 0;
                state.ferocityRefundStacks = 0;
            }
            if (getFerocityStacks(actor) <= 0) {
                state.hitCounter = 0;
                state.bonusDuration = 0;
                state.wounds.clear();
                EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
                if (attackSpeed != null) attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
            } else if (state.ferocityTuning.flag(1 << 7) && now - state.lastHitTick <= state.ferocityTuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.TWISTED_ENDLESS_HIT_WINDOW_TICKS, 40)) {
                StatusEffectInstance current = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
                if (current != null && current.getDuration() < 3) {
                    actor.addStatusEffect(new StatusEffectInstance(
                            EffectRegistry.getReference(EffectRegistry.FEROCITY), 3,
                            Math.min(maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks,
                                    state.ferocityTuning) - 1, current.getAmplifier()), false, false, true), actor);
                }
            }
            maintainFootwork(actor, state.ferocityTuning);
            updateMasteryAttackSpeed(actor, getFerocityStacks(actor), state.ferocityTuning);
            int woundWindow = state.crescendoTuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.TWISTED_WOUND_WINDOW_TICKS, 60);
            state.wounds.entrySet().removeIf(wound -> now - wound.getValue().lastHit > woundWindow);
            if (state.armed == null && state.hitCounter <= 0 && getFerocityStacks(actor) <= 0
                    && state.pendingCrescendos.isEmpty() && state.ferocityRefundAt <= 0) {
                iterator.remove();
            }
        }

        if (states.isEmpty()) {
            WIELDER_STATES.remove(world);
        }
    }

    public static int getFerocityStacks(LivingEntity actor) {
        if (actor == null) {
            return 0;
        }
        StatusEffectInstance ferocity =
                actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
        return ferocity == null ? 0 : Math.max(0, ferocity.getAmplifier() + 1);
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.TWISTED_BLADE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || getFerocityStacks(context.actor()) <= 0
                || isArmed(context.world(), context.actor())) {
            return false;
        }

        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        LivingEntity target = context.target();
        return target != null
                && target.isAlive()
                && target != context.actor()
                && HelperMethods.checkAbilityTarget(target, context.actor())
                && (context.sourcePlayer() == null
                || target != context.sourcePlayer()
                && HelperMethods.checkAbilityTarget(target, context.sourcePlayer()));
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        UniqueAbilityExecution execution = DeathShadowBloodMasteryCombatManager.beginActive(
                DeathShadowBloodMasteryAbilities.TWISTED_FINALE, context, 0);
        DeathShadowBloodMasteryTuning finaleTuning = DeathShadowBloodMasteryAbilities.tuning(execution);
        LivingEntity actor = context.actor();
        int originalStacks = getFerocityStacks(actor);
        boolean sustained = finaleTuning.flag(1 << 26);
        int consumedStacks = sustained
                ? sustainedConsumption(originalStacks, finaleTuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SUSTAINED_CONSUME_RATIO, .5))
                : originalStacks;
        if (consumedStacks <= 0) {
            return false;
        }

        WielderState state = state(context.world(), actor);
        state.hitCounter = 0;
        int empoweredWindow = finaleWindow(Config.uniqueEffects.twisted_blade.empoweredWindow, finaleTuning);
        int retainedStacks = sustained ? originalStacks - consumedStacks : 0;
        if (retainedStacks > 0) {
            setFerocityStacks(actor, retainedStacks,
                    ferocityDuration(Config.uniqueEffects.twisted_blade.duration, state.ferocityTuning),
                    state.ferocityTuning);
        } else {
            actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
            updateMasteryAttackSpeed(actor, 0, state.ferocityTuning);
        }
        state.armed = new ArmedCrescendo(
                consumedStacks,
                context.world().getTime() + empoweredWindow,
                finaleTuning,
                context.world().getTime(),
                sustained ? finaleTuning.integer(
                        DeathShadowBloodMasteryTuning.Setting.TWISTED_SUSTAINED_HIT_COUNT, 3) : 1,
                execution
        );
        spawnActivationCue(context.world(), actor, consumedStacks,
                maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks, state.ferocityTuning));
        return true;
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack,
                                  LivingEntity reportedAttacker, LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.TWISTED_BLADE.get())
                || reportedAttacker == null
                || target == null) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        LivingEntity sourceOwner = delegated == null ? null : delegated.owner();
        if (actor == null || !actor.isAlive() || actor.getWorld() != world || target.getWorld() != world) {
            return;
        }

        WielderState existingState = getState(world, actor);
        if (existingState == null) existingState = state(world, actor);
        UniqueAbilityExecution ferocityExecution = DeathShadowBloodMasteryCombatManager.beginPassive(
                DeathShadowBloodMasteryAbilities.TWISTED_FEROCITY, world, stack, actor, target);
        existingState.ferocityTuning = DeathShadowBloodMasteryAbilities.tuning(ferocityExecution);
        UniqueAbilityExecution crescendoExecution = DeathShadowBloodMasteryCombatManager.beginPassive(
                DeathShadowBloodMasteryAbilities.TWISTED_CRESCENDO, world, stack, actor, target);
        existingState.crescendoTuning = DeathShadowBloodMasteryAbilities.tuning(crescendoExecution);
        ArmedCrescendo armed = existingState.armed;
        boolean empowered = armed != null && world.getTime() < armed.expiresAt;
        if (empowered) {
            armed.remainingHits--;
            CrescendoResult result = triggerCrescendo(
                    world,
                    stack,
                    actor,
                    sourceOwner,
                    target,
                    true,
                    false,
                    armed.consumedStacks,
                    existingState,
                    armed.tuning
            );
            armed.affectedTargets += result.affectedTargets;
            if (result.affectedTargets > 0) UniqueAbilityApi.emit(armed.execution,
                    net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    DeathShadowBloodMasteryAbilities.HIT, target, result.affectedTargets, result.damage);
            if (result.kills > 0) UniqueAbilityApi.emit(armed.execution,
                    net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    DeathShadowBloodMasteryAbilities.KILL, target, result.kills, result.damage);
            if (result.kills > 0 && armed.tuning.flag(1 << 23)) {
                existingState.ferocityRefundAt = world.getTime()
                        + armed.tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_ENCORE_DELAY_TICKS, 20);
                existingState.ferocityRefundStacks = armed.tuning.integer(
                        DeathShadowBloodMasteryTuning.Setting.TWISTED_ENCORE_REFUND_STACKS, 4);
            }
            if (armed.remainingHits <= 0) {
                existingState.armed = null;
                UniqueAbilityApi.finish(armed.execution, DeathShadowBloodMasteryAbilities.FINISH, armed.affectedTargets);
            }
            existingState.hitCounter = 0;
        }

        existingState.lastHitTick = world.getTime();
        int previousStacks = getFerocityStacks(actor);
        int stacks = tryGainFerocity(world, actor, existingState);
        UniqueAbilityApi.finish(ferocityExecution, DeathShadowBloodMasteryAbilities.FINISH,
                stacks > previousStacks ? 1 : 0);
        if (empowered || stacks <= 0) {
            UniqueAbilityApi.finish(crescendoExecution, DeathShadowBloodMasteryAbilities.FINISH, 0);
            return;
        }

        WielderState state = existingState;
        state.hitCounter++;
        int interval = getCrescendoInterval(stacks, state.crescendoTuning,
                maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks, state.ferocityTuning));
        if (state.hitCounter < interval) {
            UniqueAbilityApi.finish(crescendoExecution, DeathShadowBloodMasteryAbilities.FINISH, 0);
            return;
        }

        state.hitCounter = 0;
        CrescendoResult result = triggerCrescendo(world, stack, actor, sourceOwner, target, false,
                false, stacks, state,
                state.crescendoTuning);
        if (result.affectedTargets > 0) UniqueAbilityApi.emit(crescendoExecution,
                net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                DeathShadowBloodMasteryAbilities.HIT, target, result.affectedTargets, result.damage);
        if (result.kills > 0) UniqueAbilityApi.emit(crescendoExecution,
                net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                DeathShadowBloodMasteryAbilities.KILL, target, result.kills, result.damage);
        UniqueAbilityApi.finish(crescendoExecution, DeathShadowBloodMasteryAbilities.FINISH, result.affectedTargets);
        state.crescendoCounter++;
        int maximumStacks = maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks,
                state.ferocityTuning);
        if (state.crescendoTuning.flag(1 << 15) && stacks >= maximumStacks
                && state.crescendoCounter % state.crescendoTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_DOUBLE_INTERVAL, 3) == 0) {
            state.pendingCrescendos.add(new PendingCrescendo(target.getUuid(),
                    sourceOwner == null ? null : sourceOwner.getUuid(), stack.copy(), stacks,
                    world.getTime() + state.crescendoTuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.TWISTED_DOUBLE_DELAY_TICKS, 4), state.crescendoTuning));
        }
    }

    private static int tryGainFerocity(ServerWorld world, LivingEntity actor, WielderState state) {
        DeathShadowBloodMasteryTuning tuning = state.ferocityTuning;
        int chance = ferocityChance(Config.uniqueEffects.twisted_blade.chance, tuning);
        int currentStacks = getFerocityStacks(actor);
        state.meleeCounter++;
        boolean guaranteed = tuning.flag(1 << 3) && state.meleeCounter % tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_GUARANTEED_HIT_INTERVAL, 5) == 0;
        if (!guaranteed && (chance <= 0 || actor.getRandom().nextInt(100) >= chance)) {
            return currentStacks;
        }

        int maximumStacks = maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks, tuning);
        int gain = tuning.flag(1 << 8) && actor.getHealth() / actor.getMaxHealth()
                < tuning.get(DeathShadowBloodMasteryTuning.Setting.TWISTED_FEVER_HEALTH_THRESHOLD, .5)
                ? tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_FEVER_GAIN, 2) : 1;
        int newStacks = Math.min(maximumStacks, currentStacks + gain);
        int duration = ferocityDuration(Config.uniqueEffects.twisted_blade.duration, tuning);
        if (tuning.flag(1 << 4) && currentStacks >= tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_CADENCE_THRESHOLD, 8)) {
            int durationCap = tuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.TWISTED_CADENCE_BONUS_CAP_TICKS, 120);
            state.bonusDuration = Math.min(durationCap,
                    state.bonusDuration + tuning.integer(
                            DeathShadowBloodMasteryTuning.Setting.TWISTED_CADENCE_BONUS_TICKS, 40));
            duration += state.bonusDuration;
        }
        setFerocityStacks(actor, newStacks, duration, tuning);
        spawnStackGainCue(world, actor, newStacks, maximumStacks);
        maintainFootwork(actor, tuning);
        return newStacks;
    }

    private static void updateMasteryAttackSpeed(LivingEntity actor, int stacks, DeathShadowBloodMasteryTuning tuning) {
        EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed == null) return;
        attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
        double desired = attackSpeedBonus(stacks, Config.uniqueEffects.twisted_blade.attackSpeedPerStack, tuning);
        double baseline = Config.uniqueEffects.twisted_blade.attackSpeedPerStack * stacks;
        double correction = desired - baseline;
        if (Math.abs(correction) > .0001) attackSpeed.addTemporaryModifier(new EntityAttributeModifier(
                MASTERY_ATTACK_SPEED, correction, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static int getCrescendoInterval(int stacks, DeathShadowBloodMasteryTuning tuning, int maximumStacks) {
        int tier = stacks >= maximumStacks
                ? 3
                : Math.min(3, Math.max(0, (Math.max(1, stacks) - 1) * 4 / maximumStacks));
        int baseInterval = crescendoBaseInterval(
                Config.uniqueEffects.twisted_blade.crescendoBaseInterval, tuning);
        int minimumInterval = Math.max(1, Math.min(
                baseInterval,
                Config.uniqueEffects.twisted_blade.crescendoMinimumInterval
        ));
        return Math.max(minimumInterval, Math.round(MathHelper.lerp(tier / 3.0F, baseInterval, minimumInterval)));
    }

    static int ferocityChance(int configuredChance, DeathShadowBloodMasteryTuning tuning) {
        return Math.clamp(configuredChance + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_CHANCE_BONUS, 0), 0, 100);
    }

    static int maximumStacks(int configuredMaximum, DeathShadowBloodMasteryTuning tuning) {
        if (tuning.flag(1 << 7)) return tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_ENDLESS_MAX_STACKS, 10);
        if (tuning.flag(1 << 8)) return tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_FEVER_MAX_STACKS, 20);
        return Math.max(1, configuredMaximum + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_MAX_STACK_BONUS, 0));
    }

    static int ferocityDuration(int configuredDuration, DeathShadowBloodMasteryTuning tuning) {
        if (tuning.flag(1 << 8)) return tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_FEVER_DURATION_TICKS, 80);
        return Math.max(1, configuredDuration + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_DURATION_BONUS_TICKS, 0));
    }

    static double attackSpeedBonus(int stacks, double configuredPerStack, DeathShadowBloodMasteryTuning tuning) {
        double ordinary = configuredPerStack + tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_ATTACK_SPEED_PER_STACK_BONUS, 0);
        if (!tuning.flag(1 << 6)) return Math.max(0, ordinary) * stacks;
        int threshold = tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_OVERFLOW_THRESHOLD, 15);
        double overflow = tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_OVERFLOW_ATTACK_SPEED_PER_STACK, .05);
        return Math.max(0, ordinary) * Math.min(stacks, threshold)
                + Math.max(0, overflow) * Math.max(0, stacks - threshold);
    }

    static double crescendoRadius(double configuredRadius, DeathShadowBloodMasteryTuning tuning) {
        double radius = Math.max(.1, configuredRadius) + tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_CRESCENDO_RADIUS_BONUS, 0);
        if (tuning.flag(1 << 17)) radius *= tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_ORCHESTRA_RADIUS_MULTIPLIER, 1.75);
        return Math.max(.1, radius);
    }

    static int crescendoBaseInterval(int configuredInterval, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(1, configuredInterval + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_CRESCENDO_INTERVAL_BONUS, 0));
    }

    static float crescendoScaling(float configuredScaling, DeathShadowBloodMasteryTuning tuning,
                                   boolean secondary) {
        float scaling = Math.max(0, configuredScaling) * (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_CRESCENDO_DAMAGE_MULTIPLIER, 1);
        if (tuning.flag(1 << 16)) scaling *= (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SOLO_DAMAGE_MULTIPLIER, 1.9);
        if (tuning.flag(1 << 17)) scaling *= (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_ORCHESTRA_DAMAGE_MULTIPLIER, .65);
        if (secondary) scaling *= (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_DOUBLE_DAMAGE_MULTIPLIER, .6);
        return scaling;
    }

    static double crescendoKnockback(double configuredKnockback, DeathShadowBloodMasteryTuning tuning) {
        double knockback = Math.max(0, configuredKnockback) + tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_CRESCENDO_KNOCKBACK_BONUS, 0);
        if (tuning.flag(1 << 17)) knockback *= tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_ORCHESTRA_KNOCKBACK_MULTIPLIER, 0);
        return Math.max(0, knockback);
    }

    static int finaleWindow(int configuredWindow, DeathShadowBloodMasteryTuning tuning) {
        if (tuning.flag(1 << 25)) return tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SHATTER_WINDOW_TICKS, 40);
        return Math.max(1, configuredWindow + tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_FINALE_WINDOW_BONUS_TICKS, 0));
    }

    static int sustainedConsumption(int stacks, double ratio) {
        return Math.min(stacks, Math.max(1, (int) Math.floor(stacks * Math.clamp(ratio, 0, 1))));
    }

    static float finaleDamageScaling(float fraction, float configuredMinimum, float configuredMaximum,
                                     DeathShadowBloodMasteryTuning tuning) {
        float minimum = Math.max(0, configuredMinimum) * (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_FINALE_MIN_DAMAGE_MULTIPLIER, 1);
        float maximum = Math.max(0, configuredMaximum) * (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_FINALE_MAX_DAMAGE_MULTIPLIER, 1);
        float damage = MathHelper.lerp(MathHelper.clamp(fraction, 0, 1), minimum, maximum);
        if (tuning.flag(1 << 26)) damage *= (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SUSTAINED_DAMAGE_MULTIPLIER, .55);
        return damage;
    }

    static double finaleRadius(float fraction, double configuredMinimum, double configuredMaximum,
                               DeathShadowBloodMasteryTuning tuning) {
        if (tuning.flag(1 << 25)) return tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SHATTER_RADIUS, 5);
        return Math.max(.1, MathHelper.lerp(MathHelper.clamp(fraction, 0, 1),
                Math.max(.1, configuredMinimum), Math.max(.1, configuredMaximum))
                + tuning.get(DeathShadowBloodMasteryTuning.Setting.TWISTED_FINALE_RADIUS_BONUS, 0));
    }

    static double finaleKnockback(float fraction, double configuredMinimum, double configuredMaximum,
                                  DeathShadowBloodMasteryTuning tuning) {
        return Math.max(0, MathHelper.lerp(MathHelper.clamp(fraction, 0, 1),
                Math.max(0, configuredMinimum), Math.max(0, configuredMaximum))
                + tuning.get(DeathShadowBloodMasteryTuning.Setting.TWISTED_FINALE_KNOCKBACK_BONUS, 0));
    }

    private static CrescendoResult triggerCrescendo(ServerWorld world, ItemStack stack, LivingEntity actor,
                                                    LivingEntity sourceOwner, LivingEntity impactTarget,
                                                    boolean empowered, boolean secondary, int stacks,
                                                    WielderState state, DeathShadowBloodMasteryTuning tuning) {
        int maximumStacks = maximumStacks(Config.uniqueEffects.twisted_blade.maxStacks,
                state.ferocityTuning);
        float stackFraction = tuning.flag(1 << 25) ? 1 : MathHelper.clamp(
                stacks / (float) maximumStacks, 0.0F, 1.0F);
        float damageScaling;
        if (empowered) {
            damageScaling = finaleDamageScaling(stackFraction,
                    Config.uniqueEffects.twisted_blade.empoweredMinimumDamageScaling,
                    Config.uniqueEffects.twisted_blade.empoweredMaximumDamageScaling, tuning);
        } else {
            damageScaling = crescendoScaling(
                    Config.uniqueEffects.twisted_blade.crescendoDamageScaling, tuning, secondary);
        }
        float spellScaling = empowered
                ? finaleDamageScaling(stackFraction,
                Config.uniqueEffects.twisted_blade.empoweredMinimumSpellScaling,
                Config.uniqueEffects.twisted_blade.empoweredMaximumSpellScaling, tuning)
                : crescendoScaling(Config.uniqueEffects.twisted_blade.crescendoSpellScaling,
                tuning, secondary);
        double radius = empowered
                ? finaleRadius(stackFraction, Config.uniqueEffects.twisted_blade.empoweredMinimumRadius,
                Config.uniqueEffects.twisted_blade.empoweredMaximumRadius, tuning)
                : crescendoRadius(Config.uniqueEffects.twisted_blade.crescendoRadius, tuning);
        double knockback = empowered
                ? finaleKnockback(stackFraction,
                Config.uniqueEffects.twisted_blade.empoweredMinimumKnockback,
                Config.uniqueEffects.twisted_blade.empoweredMaximumKnockback, tuning)
                : crescendoKnockback(Config.uniqueEffects.twisted_blade.crescendoKnockback, tuning);

        Vec3d center = impactTarget.getPos().add(
                0.0,
                Math.max(0.35, impactTarget.getHeight() * 0.5),
                0.0
        );
        Vec3d facing = horizontalDirection(impactTarget.getPos().subtract(actor.getPos()), actor);
        float damage = HelperMethods.abilityScaledDamage("soul", actor, stack, damageScaling, spellScaling);
        if (empowered && tuning.flag(1 << 24) && state.armed != null
                && world.getTime() - state.armed.armedAt <= tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_PERFECT_WINDOW_TICKS, 20)) {
            damage *= (float) tuning.get(
                    DeathShadowBloodMasteryTuning.Setting.TWISTED_PERFECT_DAMAGE_MULTIPLIER, 1.2);
        }
        Box area = new Box(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );
        DamageSource source = SimplySwordsAPI.getWeaponDamageSource(actor);

        int targetCap = empowered && tuning.flag(1 << 25)
                ? tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_SHATTER_TARGET_CAP, 16)
                : empowered && tuning.flag(1 << 26)
                ? 1
                : tuning.flag(1 << 16)
                ? tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_SOLO_TARGET_CAP, 1)
                : tuning.flag(1 << 17)
                ? tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_ORCHESTRA_TARGET_CAP, 16)
                : Integer.MAX_VALUE;
        int affected = 0;
        int kills = 0;
        java.util.List<LivingEntity> candidates;
        if (empowered && tuning.flag(1 << 26)) {
            candidates = new java.util.ArrayList<>(java.util.List.of(impactTarget));
        } else if (!empowered && tuning.flag(1 << 16)) {
            double range = tuning.get(DeathShadowBloodMasteryTuning.Setting.TWISTED_SOLO_RANGE, 5);
            Vec3d actorCenter = actor.getPos().add(0, actor.getHeight() * .5, 0);
            Box soloArea = new Box(actorCenter.x - range, actorCenter.y - range, actorCenter.z - range,
                    actorCenter.x + range, actorCenter.y + range, actorCenter.z + range);
            candidates = world.getEntitiesByClass(LivingEntity.class, soloArea,
                    candidate -> isValidTarget(world, actor, sourceOwner, candidate)
                            && candidate.squaredDistanceTo(actor) <= range * range);
            candidates.sort(java.util.Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(actor)));
        } else {
            candidates = world.getEntitiesByClass(LivingEntity.class, area,
                    candidate -> isValidTarget(world, actor, sourceOwner, candidate));
            candidates.sort(java.util.Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));
        }
        boolean syncopated = !empowered && !secondary && tuning.flag(1 << 13)
                && (state.crescendoCounter + 1) % tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SYNC_INTERVAL, 2) == 0;
        if (syncopated) damage *= (float) tuning.get(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_SYNC_DAMAGE_MULTIPLIER, 1.15);
        for (LivingEntity candidate : candidates) {
            if (affected >= targetCap) break;
            double woundMultiplier = 1;
            WoundState wound = state.wounds.get(candidate.getUuid());
            if (!empowered && tuning.flag(1 << 14) && wound != null
                    && world.getTime() - wound.lastHit <= tuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.TWISTED_WOUND_WINDOW_TICKS, 60)
                    && wound.hits >= tuning.integer(DeathShadowBloodMasteryTuning.Setting.TWISTED_WOUND_HITS, 2)) {
                woundMultiplier = tuning.get(
                        DeathShadowBloodMasteryTuning.Setting.TWISTED_WOUND_DAMAGE_MULTIPLIER, 1.1);
                wound.hits = 0;
            }
            float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(
                    world,
                    stack,
                    candidate,
                    source,
                    damage * (float) woundMultiplier
            );
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(
                    () -> damaged[0] = HelperMethods.damageThroughIframes(candidate, source, enchantedDamage)
            );
            if (damaged[0]) {
                affected++;
                if (!candidate.isAlive()) kills++;
                if (!empowered && tuning.flag(1 << 14)) {
                    WoundState updated = woundState(state, candidate.getUuid());
                    if (world.getTime() - updated.lastHit > tuning.integer(
                            DeathShadowBloodMasteryTuning.Setting.TWISTED_WOUND_WINDOW_TICKS, 60)) updated.hits = 0;
                    updated.hits++;
                    updated.lastHit = world.getTime();
                }
                if (syncopated) {
                    Vec3d pull = center.subtract(candidate.getPos()).multiply(1, 0, 1);
                    if (pull.lengthSquared() > 0) candidate.addVelocity(pull.normalize().multiply(tuning.get(
                            DeathShadowBloodMasteryTuning.Setting.TWISTED_SYNC_PULL_STRENGTH, .2)));
                } else knockAway(candidate, center, facing, knockback, empowered ? 0.16 : 0.08);
            }
        }

        spawnCrescendoEffects(
                world,
                actor,
                center,
                (float) Math.max(0.0, center.y - impactTarget.getY()),
                facing,
                radius,
                empowered,
                syncopated
        );
        return new CrescendoResult(affected, kills, damage);
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity target) {
        return target != null
                && target.isAlive()
                && target != actor
                && target != sourceOwner
                && target.getWorld() == world
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static void setFerocityStacks(LivingEntity actor, int stacks, int duration,
                                          DeathShadowBloodMasteryTuning tuning) {
        if (stacks <= 0) {
            actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
            updateMasteryAttackSpeed(actor, 0, tuning);
            return;
        }
        StatusEffectInstance current = actor.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.FEROCITY));
        if (current != null && current.getAmplifier() + 1 > stacks) {
            actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
        }
        actor.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.FEROCITY),
                Math.max(1, duration), stacks - 1, false, false, true), actor);
        updateMasteryAttackSpeed(actor, stacks, tuning);
    }

    private static void maintainFootwork(LivingEntity actor, DeathShadowBloodMasteryTuning tuning) {
        if (!tuning.flag(1 << 5) || getFerocityStacks(actor) < tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.TWISTED_FOOTWORK_THRESHOLD, 8)) return;
        StatusEffectInstance speed = actor.getStatusEffect(StatusEffects.SPEED);
        if (speed == null || speed.getAmplifier() == 0 && speed.getDuration() < 3) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 3, 0,
                    false, false, true), actor);
        }
    }

    private static WoundState woundState(WielderState state, UUID targetId) {
        WoundState existing = state.wounds.get(targetId);
        if (existing != null) return existing;
        if (state.wounds.size() >= 64) {
            UUID oldest = null;
            long oldestTick = Long.MAX_VALUE;
            for (Map.Entry<UUID, WoundState> entry : state.wounds.entrySet()) {
                if (entry.getValue().lastHit < oldestTick) {
                    oldest = entry.getKey();
                    oldestTick = entry.getValue().lastHit;
                }
            }
            if (oldest != null) state.wounds.remove(oldest);
        }
        WoundState created = new WoundState();
        state.wounds.put(targetId, created);
        return created;
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

    private static void spawnStackGainCue(ServerWorld world, LivingEntity actor,
                                          int stacks, int maximumStacks) {
        float progress = MathHelper.clamp(stacks / (float) maximumStacks, 0.0F, 1.0F);
        Vec3d pos = actor.getPos().add(0.0, Math.max(0.45, actor.getHeight() * 0.62), 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z,
                2 + Math.round(progress * 3.0F), 0.22, 0.22, 0.22, 0.035);
        if (progress >= 0.5F) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z,
                    1 + Math.round(progress * 2.0F), 0.2, 0.25, 0.2, 0.02);
        }
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_02.get(),
                actor.getSoundCategory(),
                0.28F,
                0.85F + progress * 0.65F
        );
    }

    private static void spawnActivationCue(ServerWorld world, LivingEntity actor, int stacks,
                                           int maximumStacks) {
        float progress = MathHelper.clamp(stacks / (float) maximumStacks, 0.0F, 1.0F);
        Vec3d pos = actor.getPos().add(0.0, Math.max(0.4, actor.getHeight() * 0.5), 0.0);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z,
                12 + Math.round(progress * 14.0F), 0.42, 0.55, 0.42, 0.045);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z,
                8 + Math.round(progress * 8.0F), 0.38, 0.48, 0.38, 0.035);
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                actor.getSoundCategory(),
                0.65F,
                0.78F + progress * 0.42F
        );
    }

    private static void spawnCrescendoEffects(ServerWorld world, LivingEntity actor,
                                              Vec3d center, float groundOffset, Vec3d facing, double radius,
                                              boolean empowered, boolean mirrored) {
        int multiplier = empowered ? 2 : 1;
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z,
                12 * multiplier, radius * 0.22, radius * 0.18, radius * 0.22, 0.07);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                8 * multiplier, radius * 0.25, radius * 0.2, radius * 0.25, 0.055);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
                empowered ? 3 : 1, radius * 0.12, radius * 0.08, radius * 0.12, 0.0);
        if (empowered) {
            world.spawnParticles(ParticleTypes.POOF, center.x, center.y, center.z,
                    16, radius * 0.3, radius * 0.12, radius * 0.3, 0.04);
        }

        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                empowered
                        ? SoundRegistry.MAGIC_SWORD_ATTACK_04.get()
                        : SoundRegistry.MAGIC_SWORD_WHOOSH_04.get(),
                actor.getSoundCategory(),
                empowered ? 1.0F : 0.72F,
                empowered ? 0.72F : 1.08F + world.random.nextFloat() * 0.12F
        );
        if (!empowered) {
            world.playSound(
                    null,
                    center.x,
                    center.y,
                    center.z,
                    SoundRegistry.MAGIC_SWORD_ATTACK_02.get(),
                    actor.getSoundCategory(),
                    0.82F,
                    1.02F + world.random.nextFloat() * 0.10F
            );
        }

        if (!Config.general.enableModernFieldEffects) {
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
        TwistedBladeCrescendoVisualEntity visual = new TwistedBladeCrescendoVisualEntity(
                world,
                center.x,
                center.y,
                center.z,
                yaw,
                (float) (radius * 0.82),
                groundOffset,
                empowered,
                mirrored,
                VISUAL_LIFETIME
        );
        world.spawnEntity(visual);
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction.multiply(1.0, 0.0, 1.0);
        if (horizontal.horizontalLengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw()).multiply(1.0, 0.0, 1.0);
        }
        return horizontal.horizontalLengthSquared() < 0.0001
                ? new Vec3d(0.0, 0.0, 1.0)
                : horizontal.normalize();
    }

    private static boolean isArmed(ServerWorld world, LivingEntity actor) {
        WielderState state = getState(world, actor);
        if (state == null || state.armed == null) {
            return false;
        }
        if (world.getTime() >= state.armed.expiresAt) {
            state.armed = null;
            return false;
        }
        return true;
    }

    private static WielderState getState(ServerWorld world, LivingEntity actor) {
        Map<UUID, WielderState> states = WIELDER_STATES.get(world);
        return states == null ? null : states.get(actor.getUuid());
    }

    private static WielderState state(ServerWorld world, LivingEntity actor) {
        return WIELDER_STATES
                .computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(actor.getUuid(), ignored -> new WielderState());
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, WielderState> states = WIELDER_STATES.remove(world);
        if (states == null) return;
        states.forEach((actorId, state) -> {
            cancelState(state);
            if (world.getEntity(actorId) instanceof LivingEntity actor) {
                EntityAttributeInstance attackSpeed = actor.getAttributeInstance(
                        EntityAttributes.GENERIC_ATTACK_SPEED);
                if (attackSpeed != null) attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
            }
        });
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID actorId = actor.getUuid();
        WIELDER_STATES.values().forEach(states -> {
            WielderState removed = states.remove(actorId);
            if (removed != null) cancelState(removed);
        });
        WIELDER_STATES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
    }

    public static void clearAll() {
        WIELDER_STATES.forEach((world, states) -> states.forEach((actorId, state) -> {
            cancelState(state);
            if (world.getEntity(actorId) instanceof LivingEntity actor) {
                EntityAttributeInstance attackSpeed = actor.getAttributeInstance(
                        EntityAttributes.GENERIC_ATTACK_SPEED);
                if (attackSpeed != null) attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
            }
        }));
        WIELDER_STATES.clear();
    }

    private static void cancelState(WielderState state) {
        if (state != null && state.armed != null) UniqueAbilityApi.cancel(state.armed.execution);
    }

    private static final class WielderState {
        private int hitCounter;
        private int meleeCounter;
        private int bonusDuration;
        private long lastHitTick;
        private int crescendoCounter;
        private long ferocityRefundAt;
        private int ferocityRefundStacks;
        private ArmedCrescendo armed;
        private final java.util.List<PendingCrescendo> pendingCrescendos = new java.util.ArrayList<>();
        private final Map<UUID, WoundState> wounds = new HashMap<>();
        private DeathShadowBloodMasteryTuning ferocityTuning = DeathShadowBloodMasteryTuning.EMPTY;
        private DeathShadowBloodMasteryTuning crescendoTuning = DeathShadowBloodMasteryTuning.EMPTY;
    }

    private static final class ArmedCrescendo {
        private final int consumedStacks;
        private final long expiresAt;
        private final DeathShadowBloodMasteryTuning tuning;
        private final long armedAt;
        private int remainingHits;
        private final UniqueAbilityExecution execution;
        private int affectedTargets;

        private ArmedCrescendo(int consumedStacks, long expiresAt, DeathShadowBloodMasteryTuning tuning,
                              long armedAt, int remainingHits, UniqueAbilityExecution execution) {
            this.consumedStacks = consumedStacks;
            this.expiresAt = expiresAt;
            this.tuning = tuning;
            this.armedAt = armedAt;
            this.remainingHits = remainingHits;
            this.execution = execution;
        }
    }

    private record PendingCrescendo(UUID targetId, UUID sourceOwnerId, ItemStack stack, int stacks,
                                    long at, DeathShadowBloodMasteryTuning tuning) {
    }

    private static final class WoundState {
        private int hits;
        private long lastHit;
    }

    private record CrescendoResult(int affectedTargets, int kills, float damage) {
    }
}
