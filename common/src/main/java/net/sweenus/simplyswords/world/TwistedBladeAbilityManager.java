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
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
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
                iterator.remove();
                continue;
            }

            WielderState state = entry.getValue();
            if (state.armed != null && now >= state.armed.expiresAt) {
                state.armed = null;
                actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
                state.hitCounter = 0;
            }
            state.pendingCrescendos.removeIf(pending -> {
                if (now < pending.at) return false;
                Entity targetEntity = world.getEntity(pending.targetId);
                Entity ownerEntity = pending.sourceOwnerId == null ? null : world.getEntity(pending.sourceOwnerId);
                if (targetEntity instanceof LivingEntity target && target.isAlive()) {
                    triggerCrescendo(world, pending.stack, actor,
                            ownerEntity instanceof LivingEntity owner ? owner : null, target, false,
                            pending.stacks, state, pending.tuning.multiply(
                                    Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, .6, 1));
                }
                return true;
            });
            if (state.ferocityRefundAt > 0 && now >= state.ferocityRefundAt) {
                int stacks = Math.max(getFerocityStacks(actor), state.ferocityRefundStacks);
                actor.addStatusEffect(new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FEROCITY),
                        Math.max(1, Config.uniqueEffects.twisted_blade.duration), stacks - 1,
                        false, false, true), actor);
                state.ferocityRefundAt = 0;
                state.ferocityRefundStacks = 0;
            }
            if (getFerocityStacks(actor) <= 0) {
                state.hitCounter = 0;
                state.bonusDuration = 0;
                EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
                if (attackSpeed != null) attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
            } else if (state.ferocityTuning.flag(1 << 7) && now - state.lastHitTick <= 40) {
                StatusEffectInstance current = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
                if (current != null && current.getDuration() < 3) {
                    actor.addStatusEffect(new StatusEffectInstance(
                            EffectRegistry.getReference(EffectRegistry.FEROCITY), 3,
                            Math.min(9, current.getAmplifier()), false, false, true), actor);
                }
            }
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

        UniqueAbilityExecution execution = Phase8CombatManager.beginActive(
                Phase8UniqueAbilities.TWISTED_FINALE, context, 0);
        Phase8AbilityTuning finaleTuning = Phase8UniqueAbilities.tuning(execution);
        LivingEntity actor = context.actor();
        int originalStacks = getFerocityStacks(actor);
        boolean sustained = finaleTuning.flag(1 << 26);
        int consumedStacks = sustained ? Math.max(1, (originalStacks + 1) / 2) : originalStacks;
        if (consumedStacks <= 0) {
            return false;
        }

        WielderState state = state(context.world(), actor);
        state.hitCounter = 0;
        StatusEffectInstance currentFerocity =
                actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
        int empoweredWindow = Math.max(1, finaleTuning.integer(Phase8AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.twisted_blade.empoweredWindow));
        actor.addStatusEffect(
                new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FEROCITY),
                        Math.max(empoweredWindow, currentFerocity == null ? 0 : currentFerocity.getDuration()),
                        (sustained ? originalStacks : consumedStacks) - 1,
                        false,
                        false,
                        true
                ),
                actor
        );
        state.armed = new ArmedCrescendo(
                consumedStacks,
                context.world().getTime() + empoweredWindow,
                finaleTuning,
                context.world().getTime(),
                sustained ? 3 : 1,
                sustained ? originalStacks - consumedStacks : 0
        );
        Phase8CombatManager.scheduleFinish(context.world(), execution, empoweredWindow, 1);
        spawnActivationCue(context.world(), actor, consumedStacks);
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
        UniqueAbilityExecution ferocityExecution = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.TWISTED_FEROCITY, world, stack, actor, target);
        existingState.ferocityTuning = Phase8UniqueAbilities.tuning(ferocityExecution);
        UniqueAbilityApi.finish(ferocityExecution, Phase8UniqueAbilities.FINISH, 1);
        UniqueAbilityExecution crescendoExecution = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.TWISTED_CRESCENDO, world, stack, actor, target);
        existingState.crescendoTuning = Phase8UniqueAbilities.tuning(crescendoExecution);
        UniqueAbilityApi.finish(crescendoExecution, Phase8UniqueAbilities.FINISH, 1);
        ArmedCrescendo armed = existingState == null ? null : existingState.armed;
        boolean empowered = armed != null && world.getTime() < armed.expiresAt;
        if (empowered) {
            armed.remainingHits--;
            triggerCrescendo(
                    world,
                    stack,
                    actor,
                    sourceOwner,
                    target,
                    true,
                    armed.consumedStacks,
                    existingState,
                    armed.tuning
            );
            if (!target.isAlive() && armed.tuning.flag(1 << 23)) {
                existingState.ferocityRefundAt = world.getTime()
                        + armed.tuning.integer(Phase8AbilityTuning.Setting.DELAY_TICKS, 20);
                existingState.ferocityRefundStacks = armed.tuning.integer(
                        Phase8AbilityTuning.Setting.COUNT, 4);
            }
            if (armed.remainingHits <= 0) {
                existingState.armed = null;
                if (armed.retainedStacks > 0) actor.addStatusEffect(new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FEROCITY),
                        Math.max(1, Config.uniqueEffects.twisted_blade.duration), armed.retainedStacks - 1,
                        false, false, true), actor);
                else actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FEROCITY));
            }
            existingState.hitCounter = 0;
        }

        existingState.lastHitTick = world.getTime();
        int stacks = tryGainFerocity(world, actor, existingState);
        if (empowered || stacks <= 0) {
            return;
        }

        WielderState state = existingState == null ? state(world, actor) : existingState;
        state.hitCounter++;
        int interval = getCrescendoInterval(stacks, state.crescendoTuning,
                state.ferocityTuning.integer(Phase8AbilityTuning.Setting.STACK_CAP,
                        Config.uniqueEffects.twisted_blade.maxStacks));
        if (state.hitCounter < interval) {
            return;
        }

        state.hitCounter = 0;
        triggerCrescendo(world, stack, actor, sourceOwner, target, false, stacks, state,
                state.crescendoTuning);
        state.crescendoCounter++;
        int maximumStacks = state.ferocityTuning.integer(Phase8AbilityTuning.Setting.STACK_CAP,
                Config.uniqueEffects.twisted_blade.maxStacks);
        if (state.crescendoTuning.flag(1 << 15) && stacks >= maximumStacks
                && state.crescendoCounter % 3 == 0) {
            state.pendingCrescendos.add(new PendingCrescendo(target.getUuid(),
                    sourceOwner == null ? null : sourceOwner.getUuid(), stack.copy(), stacks,
                    world.getTime() + state.crescendoTuning.integer(
                    Phase8AbilityTuning.Setting.DELAY_TICKS, 4), state.crescendoTuning));
        }
    }

    private static int tryGainFerocity(ServerWorld world, LivingEntity actor, WielderState state) {
        Phase8AbilityTuning tuning = state.ferocityTuning;
        int chance = tuning.integer(Phase8AbilityTuning.Setting.CHANCE, Config.uniqueEffects.twisted_blade.chance);
        int currentStacks = getFerocityStacks(actor);
        state.meleeCounter++;
        boolean guaranteed = tuning.flag(1 << 3) && state.meleeCounter % 5 == 0;
        if (!guaranteed && (chance <= 0 || actor.getRandom().nextInt(100) >= chance)) {
            return currentStacks;
        }

        int maximumStacks = Math.max(1, tuning.integer(Phase8AbilityTuning.Setting.STACK_CAP,
                Config.uniqueEffects.twisted_blade.maxStacks));
        int gain = tuning.flag(1 << 8) && actor.getHealth() / actor.getMaxHealth()
                < tuning.get(Phase8AbilityTuning.Setting.HEALTH_THRESHOLD, .5) ? 2 : 1;
        int newStacks = Math.min(maximumStacks, currentStacks + gain);
        int duration = Math.max(1, tuning.integer(Phase8AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.twisted_blade.duration));
        if (tuning.flag(1 << 4) && currentStacks >= tuning.integer(Phase8AbilityTuning.Setting.STACK_CAP, 8)) {
            int durationCap = tuning.has(Phase8AbilityTuning.Setting.DURATION_CAP_TICKS)
                    ? tuning.integer(Phase8AbilityTuning.Setting.DURATION_CAP_TICKS, 120)
                    : tuning.integer(Phase8AbilityTuning.Setting.TARGET_CAP, 120);
            state.bonusDuration = Math.min(durationCap,
                    state.bonusDuration + 40);
            duration += state.bonusDuration;
        }
        actor.addStatusEffect(
                new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.FEROCITY),
                        duration,
                        newStacks - 1,
                        false,
                        false,
                        true
                ),
                actor
        );
        spawnStackGainCue(world, actor, newStacks, maximumStacks);
        if (tuning.flag(1 << 5)) actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                duration, 0), actor);
        updateMasteryAttackSpeed(actor, newStacks, tuning);
        return newStacks;
    }

    private static void updateMasteryAttackSpeed(LivingEntity actor, int stacks, Phase8AbilityTuning tuning) {
        EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed == null) return;
        attackSpeed.removeModifier(MASTERY_ATTACK_SPEED);
        double desired = tuning.get(Phase8AbilityTuning.Setting.PER_STACK_MULTIPLIER,
                Config.uniqueEffects.twisted_blade.attackSpeedPerStack) * stacks;
        if (tuning.flag(1 << 6) && stacks > 15) desired -= (stacks - 15) * .06;
        double baseline = Config.uniqueEffects.twisted_blade.attackSpeedPerStack * stacks;
        double correction = desired - baseline;
        if (Math.abs(correction) > .0001) attackSpeed.addTemporaryModifier(new EntityAttributeModifier(
                MASTERY_ATTACK_SPEED, correction, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static int getCrescendoInterval(int stacks, Phase8AbilityTuning tuning, int maximumStacks) {
        int tier = stacks >= maximumStacks
                ? 3
                : Math.min(3, Math.max(0, (Math.max(1, stacks) - 1) * 4 / maximumStacks));
        int baseInterval = Math.max(1, tuning.integer(Phase8AbilityTuning.Setting.INTERVAL_TICKS,
                Config.uniqueEffects.twisted_blade.crescendoBaseInterval));
        int minimumInterval = Math.max(1, Math.min(
                baseInterval,
                Config.uniqueEffects.twisted_blade.crescendoMinimumInterval
        ));
        return Math.max(minimumInterval, Math.round(MathHelper.lerp(tier / 3.0F, baseInterval, minimumInterval)));
    }

    private static void triggerCrescendo(ServerWorld world, ItemStack stack, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity impactTarget,
                                         boolean empowered, int stacks, WielderState state,
                                         Phase8AbilityTuning tuning) {
        int maximumStacks = Math.max(1, state.ferocityTuning.integer(Phase8AbilityTuning.Setting.STACK_CAP,
                Config.uniqueEffects.twisted_blade.maxStacks));
        float stackFraction = tuning.flag(1 << 25) ? 1 : MathHelper.clamp(
                stacks / (float) maximumStacks, 0.0F, 1.0F);
        float damageScaling;
        if (empowered) {
            float minimum = Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMinimumDamageScaling);
            float maximum = Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMaximumDamageScaling);
            if (tuning.flag(1 << 19)) minimum *= (float) tuning.get(
                    Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1.15);
            if (tuning.flag(1 << 20)) maximum *= (float) tuning.get(
                    Phase8AbilityTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1.2);
            damageScaling = MathHelper.lerp(stackFraction, minimum, maximum);
            if (tuning.flag(1 << 26)) damageScaling *= (float) tuning.get(
                    Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, .55);
        } else {
            damageScaling = Math.max(0.0F, Config.uniqueEffects.twisted_blade.crescendoDamageScaling)
                    * (float) tuning.get(Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        }
        if (empowered && tuning.flag(1 << 24) && state.armed != null
                && world.getTime() - state.armed.armedAt <= tuning.integer(
                Phase8AbilityTuning.Setting.LOCKOUT_TICKS, 20)) damageScaling *= 1.2F;
        float spellScaling = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMinimumSpellScaling),
                Math.max(0.0F, Config.uniqueEffects.twisted_blade.empoweredMaximumSpellScaling)
        )
                : Math.max(0.0F, Config.uniqueEffects.twisted_blade.crescendoSpellScaling);
        double radius = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.1, Config.uniqueEffects.twisted_blade.empoweredMinimumRadius),
                Math.max(0.1, Config.uniqueEffects.twisted_blade.empoweredMaximumRadius)
        )
                : Math.max(0.1, Config.uniqueEffects.twisted_blade.crescendoRadius);
        radius = tuning.get(Phase8AbilityTuning.Setting.RADIUS, radius);
        double knockback = empowered
                ? MathHelper.lerp(
                stackFraction,
                Math.max(0.0, Config.uniqueEffects.twisted_blade.empoweredMinimumKnockback),
                Math.max(0.0, Config.uniqueEffects.twisted_blade.empoweredMaximumKnockback)
        )
                : Math.max(0.0, Config.uniqueEffects.twisted_blade.crescendoKnockback);
        knockback = tuning.get(Phase8AbilityTuning.Setting.KNOCKBACK, knockback);

        Vec3d center = impactTarget.getPos().add(
                0.0,
                Math.max(0.35, impactTarget.getHeight() * 0.5),
                0.0
        );
        Vec3d facing = horizontalDirection(impactTarget.getPos().subtract(actor.getPos()), actor);
        float damage = HelperMethods.abilityScaledDamage("soul", actor, stack, damageScaling, spellScaling);
        Box area = new Box(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );
        DamageSource source = SimplySwordsAPI.getWeaponDamageSource(actor);

        int targetCap = tuning.has(Phase8AbilityTuning.Setting.TARGET_CAP)
                ? tuning.integer(Phase8AbilityTuning.Setting.TARGET_CAP, 64) : Integer.MAX_VALUE;
        int affected = 0;
        java.util.List<LivingEntity> candidates = empowered && tuning.flag(1 << 26)
                ? new java.util.ArrayList<>(java.util.List.of(impactTarget))
                : world.getEntitiesByClass(LivingEntity.class, area,
                candidate -> isValidTarget(world, actor, sourceOwner, candidate));
        candidates.sort(java.util.Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));
        boolean syncopated = !empowered && tuning.flag(1 << 13) && state.nextMirrored;
        if (syncopated) damage *= 1.15F;
        for (LivingEntity candidate : candidates) {
            if (affected >= targetCap) break;
            double woundMultiplier = 1;
            WoundState wound = state.wounds.get(candidate.getUuid());
            if (!empowered && tuning.flag(1 << 14) && wound != null
                    && world.getTime() - wound.lastHit <= tuning.integer(
                    Phase8AbilityTuning.Setting.LOCKOUT_TICKS, 60) && wound.hits >= 2) {
                woundMultiplier = tuning.get(Phase8AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1.1);
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
                if (!empowered && tuning.flag(1 << 14)) {
                    WoundState updated = state.wounds.computeIfAbsent(candidate.getUuid(), ignored -> new WoundState());
                    if (world.getTime() - updated.lastHit > tuning.integer(
                            Phase8AbilityTuning.Setting.LOCKOUT_TICKS, 60)) updated.hits = 0;
                    updated.hits++;
                    updated.lastHit = world.getTime();
                }
                if (syncopated) {
                    Vec3d pull = center.subtract(candidate.getPos()).multiply(1, 0, 1);
                    if (pull.lengthSquared() > 0) candidate.addVelocity(pull.normalize().multiply(.2));
                } else knockAway(candidate, center, facing, knockback, empowered ? 0.16 : 0.08);
            }
        }

        boolean mirrored = state.nextMirrored;
        state.nextMirrored = !state.nextMirrored;
        spawnCrescendoEffects(
                world,
                actor,
                center,
                (float) Math.max(0.0, center.y - impactTarget.getY()),
                facing,
                radius,
                empowered,
                mirrored
        );
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

    private static void spawnActivationCue(ServerWorld world, LivingEntity actor, int stacks) {
        int maximumStacks = Math.max(1, Config.uniqueEffects.twisted_blade.maxStacks);
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

    private static final class WielderState {
        private int hitCounter;
        private int meleeCounter;
        private int bonusDuration;
        private long lastHitTick;
        private boolean nextMirrored;
        private int crescendoCounter;
        private long ferocityRefundAt;
        private int ferocityRefundStacks;
        private ArmedCrescendo armed;
        private final java.util.List<PendingCrescendo> pendingCrescendos = new java.util.ArrayList<>();
        private final Map<UUID, WoundState> wounds = new HashMap<>();
        private Phase8AbilityTuning ferocityTuning = Phase8AbilityTuning.EMPTY;
        private Phase8AbilityTuning crescendoTuning = Phase8AbilityTuning.EMPTY;
    }

    private static final class ArmedCrescendo {
        private final int consumedStacks;
        private final long expiresAt;
        private final Phase8AbilityTuning tuning;
        private final long armedAt;
        private int remainingHits;
        private final int retainedStacks;

        private ArmedCrescendo(int consumedStacks, long expiresAt, Phase8AbilityTuning tuning,
                              long armedAt, int remainingHits, int retainedStacks) {
            this.consumedStacks = consumedStacks;
            this.expiresAt = expiresAt;
            this.tuning = tuning;
            this.armedAt = armedAt;
            this.remainingHits = remainingHits;
            this.retainedStacks = retainedStacks;
        }
    }

    private record PendingCrescendo(UUID targetId, UUID sourceOwnerId, ItemStack stack, int stacks,
                                    long at, Phase8AbilityTuning tuning) {
    }

    private static final class WoundState {
        private int hits;
        private long lastHit;
    }
}
