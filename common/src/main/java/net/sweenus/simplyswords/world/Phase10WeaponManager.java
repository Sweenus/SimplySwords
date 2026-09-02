package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.ability.*;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class Phase10WeaponManager {
    private static final int MAX_ASSAULTS = 256;
    private static final int ASSAULT_COOLDOWN = 20;
    private static final double MOVING_STEP = 0.01;
    private static final double FLOWING_STEP = 0.15;
    private static final double RUSH_SPEED = 1.7;
    private static final int RUSH_TICKS = 8;
    private static final int RIBBONCLEAVE_TICKS = 60;
    private static final Map<UUID, HeldState> RIBBON = new HashMap<>();
    private static final Map<UUID, HeldState> DREAD = new HashMap<>();
    private static final Map<UUID, AssaultState> ASSAULTS = new HashMap<>();
    private static final Map<ItemStack, Long> DREAD_UNWIELDED = new WeakHashMap<>();
    private static final Identifier RIBBON_SPEED = Identifier.of("simplyswords", "mastery_ribbon_speed");
    private static final Identifier RIBBON_KNOCKBACK = Identifier.of("simplyswords", "mastery_ribbon_knockback");
    private static final Identifier DREAD_SPEED = Identifier.of("simplyswords", "mastery_dread_speed");
    private static final Identifier DREAD_ATTACK_SPEED = Identifier.of("simplyswords", "mastery_dread_attack_speed");
    private static final Identifier DREAD_HEALTH = Identifier.of("simplyswords", "mastery_dread_health");

    private Phase10WeaponManager() {
    }

    public static void tickRibbon(LivingEntity actor, ItemStack stack) {
        if (!(actor.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!HelperMethods.isHolding(stack, actor)) {
            clearActor(actor);
            return;
        }
        HeldState state = refresh(RIBBON, Phase10UniqueAbilities.RIBBON_HEAVY, world, actor, stack,
                ribbonHeavyBase());
        Phase10AbilityTuning tuning = state.tuning;
        double speedFactor = tuning.get(Phase10AbilityTuning.Setting.SPEED, .95);
        if (tuning.flag(1 << 5) && actor.getHealth() <= actor.getMaxHealth()
                * tuning.get(Phase10AbilityTuning.Setting.HEALTH_THRESHOLD, .35)) {
            speedFactor = Math.max(speedFactor, tuning.get(Phase10AbilityTuning.Setting.PER_STACK_MULTIPLIER, 1));
        }
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(RIBBON_SPEED);
            double correction = speedFactor - .95;
            if (Math.abs(correction) > .0001) speed.addTemporaryModifier(new EntityAttributeModifier(
                    RIBBON_SPEED, correction, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        var knockback = actor.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(RIBBON_KNOCKBACK);
            if (tuning.has(Phase10AbilityTuning.Setting.KNOCKBACK) && actor.isOnGround()) knockback.addTemporaryModifier(
                    new EntityAttributeModifier(RIBBON_KNOCKBACK,
                            tuning.get(Phase10AbilityTuning.Setting.KNOCKBACK, .15),
                            EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (tuning.flag(1 << 3) && horizontalStep(actor) > MOVING_STEP) state.movingTicks++;
        else state.movingTicks = 0;
        if (tuning.flag(1 << 3)
                && state.movingTicks >= Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.DURATION_TICKS, 40)))
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                    Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS, 20)), 0), actor);
        if (tuning.flag(1 << 7) && actor.isSprinting()) actor.setSprinting(false);
        tickRush(world, actor, state);
    }

    private static void tickRush(ServerWorld world, LivingEntity actor, HeldState state) {
        long now = world.getTime();
        if (now > state.rushUntil) {
            state.rushTargets.clear();
            return;
        }
        Phase10AbilityTuning rush = state.rushTuning;
        if (now <= state.steerUntil && state.steerTargetId != null
                && world.getEntity(state.steerTargetId) instanceof LivingEntity target && target.isAlive()) {
            Vec3d velocity = actor.getVelocity();
            Vec3d flat = new Vec3d(velocity.x, 0, velocity.z);
            Vec3d desired = target.getPos().subtract(actor.getPos());
            desired = new Vec3d(desired.x, 0, desired.z);
            if (flat.lengthSquared() > 1.0E-4 && desired.lengthSquared() > 1.0E-4) {
                double limit = Math.toRadians(Math.max(0, rush.get(Phase10AbilityTuning.Setting.ANGLE, 12)));
                Vec3d steered = rotateToward(flat, desired, limit);
                actor.setVelocity(steered.x, velocity.y, steered.z);
                actor.velocityModified = true;
            }
        }
        int cap = rush.integer(Phase10AbilityTuning.Setting.TARGET_CAP, 0);
        double multiplier = rush.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 0);
        if (cap <= 0 || multiplier <= 0) return;
        float weapon = (float) HelperMethods.getEntityAttackDamage(actor);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class,
                actor.getBoundingBox().expand(1.0),
                candidate -> candidate != actor && HelperMethods.checkAbilityTarget(candidate, actor))) {
            if (state.rushTargets.size() >= cap) break;
            if (!state.rushTargets.add(other.getUuid())) continue;
            HelperMethods.damageThroughIframes(other,
                    actor.getDamageSources().indirectMagic(actor, actor), weapon * (float) multiplier);
            double knock = rush.get(Phase10AbilityTuning.Setting.KNOCKBACK, 0);
            if (knock > 0) other.takeKnockback(knock, actor.getX() - other.getX(), actor.getZ() - other.getZ());
            if (rush.has(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS))
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        rush.integer(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS, 30), 0), actor);
            int refund = rush.integer(Phase10AbilityTuning.Setting.REFUND_TICKS, 0);
            if (refund > 0 && !state.rushRefunded) {
                state.rushRefunded = true;
                SimplySwordsAPI.reduceWeaponCooldown(actor, actor.getMainHandStack(),
                        Config.uniqueEffects.ribboncleaver.cooldown, refund);
            }
        }
    }

    private static Vec3d rotateToward(Vec3d current, Vec3d desired, double limit) {
        Vec3d from = current.normalize();
        Vec3d to = desired.normalize();
        double angle = Math.acos(Math.clamp(from.dotProduct(to), -1.0, 1.0));
        if (angle <= limit || angle < 1.0E-4) return to.multiply(current.length());
        double sign = from.x * to.z - from.z * to.x >= 0 ? 1 : -1;
        double rotation = limit * sign;
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        return new Vec3d(from.x * cos - from.z * sin, 0, from.x * sin + from.z * cos)
                .multiply(current.length());
    }

    private static double horizontalStep(LivingEntity actor) {
        double dx = actor.getX() - actor.prevX;
        double dz = actor.getZ() - actor.prevZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static float promiseBonusDamage(Phase10AbilityTuning tuning, float weaponDamage, double base) {
        return weaponDamage * (float) Math.max(0, tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, base) - base);
    }

    public static float cleaveDamage(Phase10AbilityTuning tuning, float weaponDamage, double base) {
        double bonus = Math.max(0, tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, base) - base);
        return weaponDamage * (float) ((1 + base + bonus)
                * tuning.get(Phase10AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .4));
    }

    public static int rushTravelTicks(Phase10AbilityTuning tuning) {
        return Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.RANGE, RUSH_TICKS));
    }

    public static Phase10AbilityTuning ribbonPromiseBase(double damageBonusPercent) {
        return Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, damageBonusPercent)
                .with(Phase10AbilityTuning.Setting.DURATION_TICKS, RIBBONCLEAVE_TICKS);
    }

    public static Phase10AbilityTuning ribbonHeavyBase() {
        return Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.SPEED, .95)
                .with(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .85);
    }

    public static Phase10AbilityTuning ribbonRushBase() {
        return Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.SPEED, RUSH_SPEED)
                .with(Phase10AbilityTuning.Setting.RANGE, RUSH_TICKS);
    }

    private static Phase10AbilityTuning ribbonPromiseBase() {
        return ribbonPromiseBase(Config.uniqueEffects.ribboncleaver.damageBonusPercent);
    }

    public static void tickDreadtide(LivingEntity actor, ItemStack stack) {
        if (!(actor.getWorld() instanceof ServerWorld world)) return;
        if (!HelperMethods.isHolding(stack, actor)) {
            removeDreadAttributes(actor);
            long since = DREAD_UNWIELDED.computeIfAbsent(stack, ignored -> world.getTime());
            HeldState state = DREAD.get(actor.getUuid());
            int duration = state == null ? Config.uniqueEffects.dreadtide.get().corruptionDuration
                    : state.pact.integer(Phase10AbilityTuning.Setting.SECONDARY_DURATION_TICKS,
                    Config.uniqueEffects.dreadtide.get().corruptionDuration);
            if (world.getTime() - since >= duration) {
                setCorruption(stack, 0);
                DREAD.remove(actor.getUuid());
            }
            return;
        }
        DREAD_UNWIELDED.remove(stack);
        HeldState state = refresh(DREAD, Phase10UniqueAbilities.DREAD_CLOAK, world, actor, stack,
                dreadCloakBase());
        if (world.getTime() >= state.pactRefreshAt) {
            UniqueAbilityExecution pactExecution = Phase10CombatManager.beginPassive(
                    Phase10UniqueAbilities.DREAD_PACT, world, stack, actor, null, dreadPactBase());
            state.pact = Phase10UniqueAbilities.tuning(pactExecution);
            state.pactRefreshAt = world.getTime() + 20;
            Phase10CombatManager.finish(pactExecution, 0);
        }
        Phase10AbilityTuning cloak = state.tuning;
        Phase10AbilityTuning pact = state.pact;
        if (cloak.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0 && pact.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0) return;
        int interval = Math.max(1, pact.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS,
                Config.uniqueEffects.dreadtide.get().corruptionFrequency));
        if (pact.flag(1 << 24) && corruption(stack) > pact.get(Phase10AbilityTuning.Setting.FINAL_CORRUPTION, 80))
            interval = Math.max(1, Math.round(interval * (float) pact.get(
                    Phase10AbilityTuning.Setting.SPEED, .7)));
        int ceiling = Math.round(Config.uniqueEffects.dreadtide.get().corruptionMax);
        int floor = pact.flag(1 << 25) ? pact.integer(Phase10AbilityTuning.Setting.PITY_CHANCE, 75) : 0;
        if (world.getTime() >= state.nextCorruption) {
            state.nextCorruption = world.getTime() + interval;
            setCorruption(stack, Math.clamp(corruption(stack)
                    + Math.max(1, Math.round(Config.uniqueEffects.dreadtide.get().corruptionPerTick)), floor, ceiling));
        } else if (corruption(stack) < floor) {
            setCorruption(stack, floor);
        }
        int threshold = Math.max(10, cloak.integer(Phase10AbilityTuning.Setting.FLAT_DAMAGE, 20));
        int max = Math.max(1, cloak.integer(Phase10AbilityTuning.Setting.STACK_CAP, 5));
        int stacks = Math.min(max, corruption(stack) / threshold);
        StatusEffectInstance active = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        int current = active == null ? 0 : active.getAmplifier() + 1;
        if (stacks > current && world.getTime() >= state.cloakDisabledUntil)
            actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK),
                    280, stacks - 1, false, false, true), actor);
        if (cloak.has(Phase10AbilityTuning.Setting.HEALTH_THRESHOLD)
                && actor.getHealth() <= actor.getMaxHealth() * cloak.get(
                Phase10AbilityTuning.Setting.HEALTH_THRESHOLD, .3)
                && world.getTime() >= state.emergencyReady) {
            actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK),
                    280, Math.min(max - 1, stacks), false, false, true), actor);
            state.emergencyReady = world.getTime() + cloak.integer(
                    Phase10AbilityTuning.Setting.DELAY_TICKS, 200);
        }
        applyDreadAttributes(actor, cloak, pact, current);
    }

    public static Phase10AbilityTuning dreadCloakBase() {
        return Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.FLAT_DAMAGE, 20)
                .with(Phase10AbilityTuning.Setting.STACK_CAP, 5)
                .with(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .9);
    }

    public static Phase10AbilityTuning dreadPactBase(int corruptionFrequency, int corruptionDuration) {
        return Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.INTERVAL_TICKS, corruptionFrequency)
                .with(Phase10AbilityTuning.Setting.SECONDARY_DURATION_TICKS, corruptionDuration);
    }

    private static Phase10AbilityTuning dreadPactBase() {
        return dreadPactBase(Config.uniqueEffects.dreadtide.get().corruptionFrequency,
                Config.uniqueEffects.dreadtide.get().corruptionDuration);
    }

    private static HeldState refresh(Map<UUID, HeldState> states, UniqueAbilityDefinition definition,
                                     ServerWorld world, LivingEntity actor, ItemStack stack) {
        return refresh(states, definition, world, actor, stack, Phase10AbilityTuning.EMPTY);
    }

    private static HeldState refresh(Map<UUID, HeldState> states, UniqueAbilityDefinition definition,
                                     ServerWorld world, LivingEntity actor, ItemStack stack,
                                     Phase10AbilityTuning base) {
        HeldState state = states.computeIfAbsent(actor.getUuid(), ignored -> new HeldState());
        if (world.getTime() >= state.refreshAt) {
            UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(
                    definition, world, stack, actor, null, base);
            state.tuning = Phase10UniqueAbilities.tuning(execution);
            state.refreshAt = world.getTime() + 20;
            Phase10CombatManager.finish(execution, 0);
        }
        return state;
    }

    public static void sweepHolder(LivingEntity actor) {
        if (actor == null) return;
        if (!HelperMethods.isHoldingItem(ItemsRegistry.RIBBONCLEAVER.get(), actor)) clearActor(actor);
        if (!(actor.getMainHandStack().getItem() instanceof net.sweenus.simplyswords.item.custom.DreadtideSwordItem)
                && !(actor.getOffHandStack().getItem() instanceof net.sweenus.simplyswords.item.custom.DreadtideSwordItem)) {
            DREAD.remove(actor.getUuid());
            removeDreadAttributes(actor);
        }
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        RIBBON.remove(actor.getUuid());
        removeRibbonAttributes(actor);
    }

    public static void clearActorState(LivingEntity actor) {
        if (actor == null) return;
        clearActor(actor);
        DREAD.remove(actor.getUuid());
        removeDreadAttributes(actor);
        ASSAULTS.remove(actor.getUuid());
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        ASSAULTS.values().forEach(state -> Phase10CombatManager.finish(state.execution, 0));
        ASSAULTS.clear();
        RIBBON.clear();
        DREAD.clear();
        DREAD_UNWIELDED.clear();
    }

    public static void clearAll() {
        ASSAULTS.values().forEach(state -> Phase10CombatManager.finish(state.execution, 0));
        ASSAULTS.clear();
        RIBBON.clear();
        DREAD.clear();
        DREAD_UNWIELDED.clear();
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        ItemStack stack = actor.getMainHandStack();
        if (stack.isOf(ItemsRegistry.RIBBONCLEAVER.get())) {
            HeldState state = RIBBON.get(actor.getUuid());
            if (state != null && state.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0) != 0) {
                Phase10AbilityTuning tuning = state.tuning;
                long now = actor.getWorld().getTime();
                double desired = tuning.get(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .85);
                if (tuning.flag(1 << 8) && horizontalStep(actor) >= FLOWING_STEP)
                    desired = tuning.get(Phase10AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .88);
                if (tuning.flag(1 << 4) && source.getAttacker() instanceof LivingEntity
                        && now >= state.retortReadyAt) {
                    state.retortUntil = now + 40;
                    state.retortReadyAt = now + tuning.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS, 40);
                }
                if (tuning.flag(1 << 6)
                        && ++state.absorbedHits % Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.COUNT, 3)) == 0)
                    Phase4AbsorptionTracker.grant(actor,
                            (float) tuning.get(Phase10AbilityTuning.Setting.ABSORPTION, 4),
                            Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.SECONDARY_DURATION_TICKS, 60)),
                            (float) tuning.get(Phase10AbilityTuning.Setting.ABSORPTION, 4));
                return amount * (float) (desired / .85);
            }
        }
        return amount;
    }

    public static float modifyVoidcloakDamage(LivingEntity actor, DamageSource source, float amount,
                                              StatusEffectInstance cloak) {
        HeldState state = DREAD.get(actor.getUuid());
        if (state == null || state.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0
                && state.pact.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0) {
            HelperMethods.decrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            return amount * (1 - (cloak.getAmplifier() + 1) * .10F);
        }
        Phase10AbilityTuning tuning = state.tuning;
        Phase10AbilityTuning pact = state.pact;
        int stacks = cloak.getAmplifier() + 1;
        double perStack = 1 - tuning.get(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .9);
        long now = actor.getWorld().getTime();
        if (!tuning.flag(1 << 2) || now >= state.cloakLossAt) {
            HelperMethods.decrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            state.cloakLossAt = now + tuning.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS, 15);
            if (tuning.flag(1 << 5)) state.retortUntil = now
                    + tuning.integer(Phase10AbilityTuning.Setting.DURATION_TICKS, 60);
        }
        int corruption = corruption(actor.getMainHandStack());
        if (pact.flag(1 << 24) && corruption > pact.get(Phase10AbilityTuning.Setting.FINAL_CORRUPTION, 80))
            amount *= (float) pact.get(Phase10AbilityTuning.Setting.HEAL_MULTIPLIER, 1.08);
        if (pact.flag(1 << 21) && corruption > pact.get(Phase10AbilityTuning.Setting.SECONDARY_CORRUPTION, 60))
            amount *= (float) pact.get(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .92);
        return amount * (float) Math.max(0, 1 - stacks * perStack);
    }

    public static void onRibbonHit(ServerWorld world, LivingEntity actor, LivingEntity target, ItemStack stack) {
        UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.RIBBON_PROMISE, world, stack, actor, target, ribbonPromiseBase());
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        float weapon = (float) HelperMethods.getEntityAttackDamage(actor);
        HeldState state = RIBBON.get(actor.getUuid());
        if (state != null && world.getTime() <= state.retortUntil) {
            state.retortUntil = 0;
            float retort = weapon * (float) Math.max(0,
                    state.tuning.get(Phase10AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1.1) - 1);
            if (retort > 0) HelperMethods.damageThroughIframes(target,
                    actor.getDamageSources().indirectMagic(actor, actor), retort);
        }
        if (actor.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE))) {
            double base = Config.uniqueEffects.ribboncleaver.damageBonusPercent;
            double bonus = Math.max(0, tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, base) - base);
            float damage = weapon * (float) bonus;
            if (tuning.has(Phase10AbilityTuning.Setting.WINDUP_TICKS) && state != null
                    && world.getTime() - state.activatedAt >= tuning.integer(
                    Phase10AbilityTuning.Setting.WINDUP_TICKS, 40))
                damage += weapon * (float) tuning.get(Phase10AbilityTuning.Setting.PER_STACK_MULTIPLIER, .2);
            if (tuning.has(Phase10AbilityTuning.Setting.HEALTH_THRESHOLD)
                    && target.getHealth() <= target.getMaxHealth() * tuning.get(
                    Phase10AbilityTuning.Setting.HEALTH_THRESHOLD, .3)) damage *= tuning.get(
                    Phase10AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1.25);
            if (tuning.has(Phase10AbilityTuning.Setting.ARMOR_IGNORE)) damage += target.getArmor()
                    * tuning.get(Phase10AbilityTuning.Setting.ARMOR_IGNORE, .12);
            if (damage > 0) HelperMethods.damageThroughIframes(target,
                    actor.getDamageSources().indirectMagic(actor, actor), damage);
            if (tuning.has(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS)) target.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.SLOWNESS,
                            tuning.integer(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS, 40),
                            tuning.integer(Phase10AbilityTuning.Setting.STATUS_AMPLIFIER, 1)), actor);
            double radius = tuning.get(Phase10AbilityTuning.Setting.RADIUS, 0);
            int cap = tuning.integer(Phase10AbilityTuning.Setting.TARGET_CAP, 1);
            if (radius > 0 && cap > 1) {
                float cleaveDamage = cleaveDamage(tuning, weapon, base);
                world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius),
                                other -> other != target && HelperMethods.checkAbilityTarget(other, actor))
                        .stream().sorted(Comparator.comparingDouble(target::squaredDistanceTo))
                        .limit(cap - 1L)
                        .forEach(other -> HelperMethods.damageThroughIframes(other,
                                actor.getDamageSources().indirectMagic(actor, actor), cleaveDamage));
            }
        }
        Phase10CombatManager.finish(execution, 1);
    }

    public static Phase10AbilityTuning ribbonRush(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                   LivingEntity target) {
        UniqueAbilityExecution execution = Phase10CombatManager.beginActive(
                Phase10UniqueAbilities.RIBBON_RUSH,
                net.sweenus.simplyswords.api.WeaponAbilityContext.of(world, stack, actor,
                        actor instanceof net.minecraft.server.network.ServerPlayerEntity player ? player : null,
                        target, net.minecraft.util.Hand.MAIN_HAND,
                        net.sweenus.simplyswords.api.WeaponAbilityActivationSource.PLAYER),
                Config.uniqueEffects.ribboncleaver.cooldown, ribbonRushBase());
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        UniqueAbilityApi.start(execution);
        UniqueAbilityExecution promiseExecution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.RIBBON_PROMISE, world, stack, actor, target, ribbonPromiseBase());
        Phase10AbilityTuning promise = Phase10UniqueAbilities.tuning(promiseExecution);
        HeldState state = RIBBON.computeIfAbsent(actor.getUuid(), ignored -> new HeldState());
        state.activatedAt = world.getTime();
        state.rushTuning = tuning;
        state.rushTargets.clear();
        state.rushRefunded = false;
        state.steerTargetId = null;
        state.steerUntil = 0;
        boolean blink = tuning.flag(1 << 17);
        double speed = tuning.get(Phase10AbilityTuning.Setting.SPEED, RUSH_SPEED);
        int travelTicks = rushTravelTicks(tuning);
        state.rushUntil = world.getTime() + travelTicks;
        if (blink) {
            blinkForward(world, actor, tuning.get(Phase10AbilityTuning.Setting.SECONDARY_RADIUS, 5));
            state.rushUntil = world.getTime();
        } else if (target != null) {
            LivingEntityAbilityMovementManager.dashTowardTarget(world, actor, target, speed, travelTicks);
            if (tuning.has(Phase10AbilityTuning.Setting.ANGLE) && tuning.get(Phase10AbilityTuning.Setting.ANGLE, 0) > 0) {
                state.steerTargetId = target.getUuid();
                state.steerUntil = world.getTime()
                        + Math.max(1, tuning.integer(Phase10AbilityTuning.Setting.DURATION_TICKS, 4));
            }
        } else {
            Vec3d velocity = actor.getRotationVector().multiply(speed);
            actor.setVelocity(velocity.x, 0, velocity.z);
            actor.velocityModified = true;
        }
        int window = promise.integer(Phase10AbilityTuning.Setting.DURATION_TICKS, RIBBONCLEAVE_TICKS);
        actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE),
                window, 0, false, false, true));
        if (!blink) actor.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.RESILIENCE),
                15 + tuning.integer(Phase10AbilityTuning.Setting.SECONDARY_DURATION_TICKS, 0),
                Config.uniqueEffects.ribboncleaver.resilienceAmplifier, false, false, true));
        Phase10CombatManager.finish(promiseExecution, 0);
        Phase10CombatManager.finish(execution, target == null ? 0 : 1);
        return tuning;
    }

    private static void blinkForward(ServerWorld world, LivingEntity actor, double distance) {
        Vec3d look = actor.getRotationVector();
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        if (flat.lengthSquared() < 1.0E-4) return;
        Vec3d direction = flat.normalize();
        Vec3d destination = actor.getPos();
        for (double step = 0.5; step <= Math.max(0.5, distance); step += 0.5) {
            Vec3d candidate = actor.getPos().add(direction.multiply(step));
            Box box = actor.getBoundingBox().offset(candidate.subtract(actor.getPos()));
            if (!world.isSpaceEmpty(actor, box)) break;
            destination = candidate;
        }
        actor.requestTeleport(destination.x, destination.y, destination.z);
        actor.setVelocity(Vec3d.ZERO);
        actor.velocityModified = true;
    }

    public static void onDreadtideHit(LivingEntity actor, LivingEntity target, ItemStack stack) {
        HeldState state = DREAD.get(actor.getUuid());
        if (state == null || actor.getWorld().getTime() > state.retortUntil) return;
        float bonus = (float) HelperMethods.getAttackFromStack(stack,
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND)
                * (float) Math.max(0, state.tuning.get(Phase10AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1.12) - 1);
        HelperMethods.damageThroughIframes(target, actor.getDamageSources().indirectMagic(actor, actor), bonus);
        state.retortUntil = 0;
    }

    private static void applyDreadAttributes(LivingEntity actor, Phase10AbilityTuning cloak,
                                             Phase10AbilityTuning pact, int stacks) {
        removeDreadAttributes(actor);
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && cloak.get(Phase10AbilityTuning.Setting.SPEED, 0) > 0 && !cloak.flag(1 << 7))
            speed.addTemporaryModifier(new EntityAttributeModifier(DREAD_SPEED,
                    stacks * cloak.get(Phase10AbilityTuning.Setting.SPEED, 0),
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null && cloak.flag(1 << 8)) attackSpeed.addTemporaryModifier(
                new EntityAttributeModifier(DREAD_ATTACK_SPEED,
                        stacks * cloak.get(Phase10AbilityTuning.Setting.PER_STACK_MULTIPLIER, .08),
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var health = actor.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null && pact.flag(1 << 25)) health.addTemporaryModifier(new EntityAttributeModifier(
                DREAD_HEALTH, -pact.get(Phase10AbilityTuning.Setting.HEIGHT, 8),
                EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeDreadAttributes(LivingEntity actor) {
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(DREAD_SPEED);
        var attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(DREAD_ATTACK_SPEED);
        var health = actor.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) health.removeModifier(DREAD_HEALTH);
    }

    private static void removeRibbonAttributes(LivingEntity actor) {
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(RIBBON_SPEED);
        var knockback = actor.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.removeModifier(RIBBON_KNOCKBACK);
    }

    public static Phase10AbilityTuning dreadAssaultBase(int duration, int startingTickFrequency) {
        return Phase10AbilityTuning.EMPTY
                .with(Phase10AbilityTuning.Setting.DURATION_TICKS, duration)
                .with(Phase10AbilityTuning.Setting.INTERVAL_TICKS, startingTickFrequency)
                .with(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    private static Phase10AbilityTuning dreadAssaultBase() {
        return dreadAssaultBase(Config.uniqueEffects.dreadtide.get().duration,
                Config.uniqueEffects.dreadtide.get().startingTickFrequency);
    }

    public static boolean canActivateDreadtide(ServerWorld world, LivingEntity actor, ItemStack stack) {
        return actor != null && actor.isAlive() && stack != null && !stack.isEmpty()
                && actor.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK))
                && nearestAssaultTarget(world, actor) != null;
    }

    private static LivingEntity nearestAssaultTarget(ServerWorld world, LivingEntity actor) {
        Entity closest = world.getOtherEntities(actor, HelperMethods.createBox(actor, 10),
                        EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity living && HelperMethods.checkFriendlyFire(living, actor))
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(actor))).orElse(null);
        return closest instanceof LivingEntity living ? living : null;
    }

    public static boolean activateDreadtide(ServerWorld world, LivingEntity actor, ItemStack stack) {
        HeldState cloakState = refresh(DREAD, Phase10UniqueAbilities.DREAD_CLOAK, world, actor, stack,
                dreadCloakBase());
        UniqueAbilityExecution pactExecution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.DREAD_PACT, world, stack, actor, null, dreadPactBase());
        Phase10AbilityTuning pact = Phase10UniqueAbilities.tuning(pactExecution);
        cloakState.pact = pact;
        cloakState.pactRefreshAt = world.getTime() + 20;
        StatusEffectInstance cloak = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        LivingEntity target = nearestAssaultTarget(world, actor);
        if (target == null || cloak == null) {
            Phase10CombatManager.finish(pactExecution, 0);
            return false;
        }
        UniqueAbilityExecution execution = Phase10CombatManager.beginDirectActive(
                Phase10UniqueAbilities.DREAD_ASSAULT, world, actor, stack, net.minecraft.util.Hand.MAIN_HAND,
                ASSAULT_COOLDOWN, dreadAssaultBase());
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        int stacks = cloak.getAmplifier() + 1;
        int duration = tuning.integer(Phase10AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.dreadtide.get().duration);
        int corruption = corruption(stack);
        if (pact.flag(1 << 26)) duration += corruption;
        float multiplier = (float) tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        if (pact.flag(1 << 20) && corruption > pact.get(Phase10AbilityTuning.Setting.CORRUPTION, 40))
            multiplier *= (float) pact.get(Phase10AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1.1);
        if (pact.flag(1 << 25)) multiplier *= (float) pact.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1.25);
        multiplier *= 1 + stacks * (float) tuning.get(Phase10AbilityTuning.Setting.PER_STACK_MULTIPLIER, 0);
        float damage = HelperMethods.abilityScaledDamage("eldritch", actor, stack,
                Config.uniqueEffects.dreadtide.get().damageScaling * multiplier,
                Config.uniqueEffects.dreadtide.get().spellScaling);
        SimplySwordsStatusEffectInstance assault = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.VOIDASSAULT), duration, stacks - 1,
                false, false, true);
        assault.setSourceEntity(actor);
        assault.setAdditionalData((int) damage);
        target.addStatusEffect(assault);
        if (!ASSAULTS.containsKey(target.getUuid()) && ASSAULTS.size() >= MAX_ASSAULTS) {
            UUID oldest = ASSAULTS.entrySet().stream().min(Comparator.comparingLong(entry -> entry.getValue().expiresAt))
                    .map(Map.Entry::getKey).orElse(null);
            AssaultState removed = oldest == null ? null : ASSAULTS.remove(oldest);
            if (removed != null) Phase10CombatManager.finish(removed.execution, 0);
        }
        ASSAULTS.put(target.getUuid(), new AssaultState(tuning, execution, actor.getUuid(), stack.copy(),
                world.getTime() + duration, stacks, tuning.integer(Phase10AbilityTuning.Setting.COUNT, 0),
                new java.util.HashSet<>()));
        int purge = pact.flag(1 << 26) ? corruption
                : pact.flag(1 << 22) ? Math.min(corruption, stacks * pact.integer(Phase10AbilityTuning.Setting.COUNT, 5)) : 0;
        int floor = pact.flag(1 << 25) ? pact.integer(Phase10AbilityTuning.Setting.PITY_CHANCE, 75) : 0;
        if (purge > 0) setCorruption(stack, Math.max(floor, corruption - purge));
        int perGrant = Math.max(1, pact.integer(Phase10AbilityTuning.Setting.FLAT_DAMAGE, 20));
        if (purge >= perGrant && pact.flag(1 << 23))
            Phase4AbsorptionTracker.grant(actor,
                    purge / (float) perGrant * (float) pact.get(Phase10AbilityTuning.Setting.ABSORPTION, 4),
                    Math.max(1, pact.integer(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS, 200)),
                    (float) pact.get(Phase10AbilityTuning.Setting.SEARCH_RADIUS, 12));
        if (pact.flag(1 << 26)) cloakState.cloakDisabledUntil = world.getTime()
                + pact.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS, 200);
        else if (tuning.flag(1 << 16)) cloakState.cloakDisabledUntil = world.getTime() + duration;
        actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        Phase10CombatManager.finish(pactExecution, 1);
        return true;
    }

    public static boolean tickAssault(LivingEntity target, int amplifier, int baseDamage) {
        if (!(target.getWorld() instanceof ServerWorld world)) return false;
        AssaultState state = ASSAULTS.get(target.getUuid());
        if (state == null) return false;
        LivingEntity actor = world.getEntity(state.actorId) instanceof LivingEntity living ? living : null;
        if (actor == null || world.getTime() >= state.expiresAt || !target.isAlive()) {
            if (target != null && !target.isAlive() && actor != null && state.jumpsLeft > 0
                    && jumpAssault(world, actor, target, state)) {
                ASSAULTS.remove(target.getUuid());
                return true;
            }
            if (actor != null && world.getTime() >= state.expiresAt && target.isAlive())
                finalScream(world, actor, target, state, baseDamage);
            Phase10CombatManager.finish(state.execution, target.isAlive() ? 1 : 0);
            ASSAULTS.remove(target.getUuid());
            return true;
        }
        int interval = Math.max(1, state.tuning.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS,
                Math.max(1, Config.uniqueEffects.dreadtide.get().startingTickFrequency - amplifier * 2)));
        if (state.tuning.flag(1 << 17)) interval = Math.max(1,
                state.tuning.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS, 8));
        if (target.age % interval != 0) return true;
        if (state.tuning.flag(1 << 17)) {
            if (state.strikesLeft <= 0) return true;
            state.strikesLeft--;
        }
        float damage = baseDamage + amplifier;
        target.timeUntilRegen = 0;
        target.damage(target.getDamageSources().indirectMagic(target, actor), damage);
        if (!state.tuning.flag(1 << 17) && state.tuning.has(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS))
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    state.tuning.integer(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS, 30),
                    state.tuning.integer(Phase10AbilityTuning.Setting.STATUS_AMPLIFIER, 1)), actor);
        return true;
    }

    private static boolean jumpAssault(ServerWorld world, LivingEntity actor, LivingEntity victim,
                                       AssaultState state) {
        if (state.tuning.flag(1 << 17)) return false;
        double radius = state.tuning.get(Phase10AbilityTuning.Setting.RANGE, 0);
        if (radius <= 0) return false;
        LivingEntity next = world.getEntitiesByClass(LivingEntity.class,
                        victim.getBoundingBox().expand(radius),
                        candidate -> candidate != victim && candidate.isAlive()
                                && !state.struck.contains(candidate.getUuid())
                                && HelperMethods.checkAbilityTarget(candidate, actor))
                .stream().min(Comparator.comparingDouble(victim::squaredDistanceTo)).orElse(null);
        if (next == null) return false;
        StatusEffectInstance existing = victim.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.VOIDASSAULT));
        int remaining = (int) Math.max(1, state.expiresAt - world.getTime());
        int amplifier = existing == null ? state.stacks - 1 : existing.getAmplifier();
        SimplySwordsStatusEffectInstance jumped = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.VOIDASSAULT), remaining, amplifier,
                false, false, true);
        jumped.setSourceEntity(actor);
        jumped.setAdditionalData(existing instanceof SimplySwordsStatusEffectInstance instance
                ? instance.getAdditionalData() : 0);
        next.addStatusEffect(jumped);
        state.struck.add(next.getUuid());
        ASSAULTS.put(next.getUuid(), new AssaultState(state.tuning, state.execution, state.actorId,
                state.stack, state.expiresAt, state.stacks, state.jumpsLeft - 1, state.struck));
        return true;
    }

    private static void finalScream(ServerWorld world, LivingEntity actor, LivingEntity victim,
                                    AssaultState state, int baseDamage) {
        double radius = state.tuning.get(Phase10AbilityTuning.Setting.RADIUS, 0);
        int cap = state.tuning.integer(Phase10AbilityTuning.Setting.TARGET_CAP, 0);
        double multiplier = state.tuning.get(Phase10AbilityTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 0);
        if (radius <= 0 || cap <= 0 || multiplier <= 0) return;
        float damage = (float) (baseDamage * multiplier);
        world.getEntitiesByClass(LivingEntity.class, victim.getBoundingBox().expand(radius),
                        other -> other != actor && HelperMethods.checkAbilityTarget(other, actor))
                .stream().sorted(Comparator.comparingDouble(victim::squaredDistanceTo)).limit(cap)
                .forEach(other -> HelperMethods.damageThroughIframes(other,
                        actor.getDamageSources().indirectMagic(actor, actor), damage));
    }

    private static int corruption(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ComponentTypeRegistry.STORED_BONUS.get(),
                StoredChargeComponent.DEFAULT).charge(), 0, 100);
    }

    private static void setCorruption(ItemStack stack, int value) {
        stack.set(ComponentTypeRegistry.STORED_BONUS.get(), new StoredChargeComponent(Math.clamp(value, 0, 100)));
    }

    private static final class HeldState {
        private Phase10AbilityTuning tuning = Phase10AbilityTuning.EMPTY;
        private Phase10AbilityTuning rushTuning = Phase10AbilityTuning.EMPTY;
        private Phase10AbilityTuning pact = Phase10AbilityTuning.EMPTY;
        private long pactRefreshAt;
        private final java.util.Set<UUID> rushTargets = new java.util.HashSet<>();
        private UUID steerTargetId;
        private long rushUntil;
        private long steerUntil;
        private boolean rushRefunded;
        private long refreshAt;
        private long nextCorruption;
        private long cloakLossAt;
        private long retortUntil;
        private long retortReadyAt;
        private long cloakDisabledUntil;
        private long emergencyReady;
        private long activatedAt;
        private int movingTicks;
        private int absorbedHits;
    }

    private static final class AssaultState {
        private final Phase10AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private final UUID actorId;
        private final ItemStack stack;
        private final long expiresAt;
        private final int stacks;
        private final int jumpsLeft;
        private final java.util.Set<UUID> struck;
        private int strikesLeft;

        private AssaultState(Phase10AbilityTuning tuning, UniqueAbilityExecution execution, UUID actorId,
                             ItemStack stack, long expiresAt, int stacks, int jumpsLeft,
                             java.util.Set<UUID> struck) {
            this.tuning = tuning;
            this.execution = execution;
            this.actorId = actorId;
            this.stack = stack;
            this.expiresAt = expiresAt;
            this.stacks = stacks;
            this.jumpsLeft = jumpsLeft;
            this.struck = struck;
            this.strikesLeft = stacks;
        }
    }
}
