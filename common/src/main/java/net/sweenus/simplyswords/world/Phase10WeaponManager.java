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
        if (!(actor.getWorld() instanceof ServerWorld world) || !HelperMethods.isHolding(stack, actor)) return;
        HeldState state = refresh(RIBBON, Phase10UniqueAbilities.RIBBON_HEAVY, world, actor, stack);
        Phase10AbilityTuning tuning = state.tuning;
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(RIBBON_SPEED);
            double correction = tuning.get(Phase10AbilityTuning.Setting.SPEED, .95) - .95;
            if (Math.abs(correction) > .0001) speed.addTemporaryModifier(new EntityAttributeModifier(
                    RIBBON_SPEED, correction, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        var knockback = actor.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(RIBBON_KNOCKBACK);
            if (tuning.has(Phase10AbilityTuning.Setting.KNOCKBACK)) knockback.addTemporaryModifier(
                    new EntityAttributeModifier(RIBBON_KNOCKBACK,
                            tuning.get(Phase10AbilityTuning.Setting.KNOCKBACK, .15),
                            EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (tuning.flag(1 << 3) && actor.horizontalSpeed > .05F) state.movingTicks++;
        else state.movingTicks = 0;
        if (state.movingTicks >= 40) actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 0), actor);
        if (tuning.flag(1 << 7) && actor.isSprinting()) actor.setSprinting(false);
    }

    public static void tickDreadtide(LivingEntity actor, ItemStack stack) {
        if (!(actor.getWorld() instanceof ServerWorld world)) return;
        if (!HelperMethods.isHolding(stack, actor)) {
            removeDreadAttributes(actor);
            long since = DREAD_UNWIELDED.computeIfAbsent(stack, ignored -> world.getTime());
            HeldState state = DREAD.get(actor.getUuid());
            int duration = state == null ? 1200 : state.tuning.integer(
                    Phase10AbilityTuning.Setting.SECONDARY_DURATION_TICKS, 1200);
            if (world.getTime() - since >= duration) setCorruption(stack, 0);
            return;
        }
        DREAD_UNWIELDED.remove(stack);
        HeldState state = refresh(DREAD, Phase10UniqueAbilities.DREAD_CLOAK, world, actor, stack);
        if (state.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0) return;
        int interval = (state.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0)
                & ((1 << 18) | (1 << 24))) == 0
                ? 60 : Math.max(1, state.tuning.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS, 60));
        if (state.tuning.flag(1 << 24) && corruption(stack) > 80) interval = Math.max(1, Math.round(interval * .7F));
        if (world.getTime() >= state.nextCorruption) {
            state.nextCorruption = world.getTime() + interval;
            setCorruption(stack, Math.min(100, corruption(stack) + 1));
        }
        int threshold = Math.max(10, state.tuning.integer(Phase10AbilityTuning.Setting.FLAT_DAMAGE, 20));
        int max = Math.max(1, state.tuning.integer(Phase10AbilityTuning.Setting.STACK_CAP, 5));
        int stacks = Math.min(max, corruption(stack) / threshold);
        StatusEffectInstance cloak = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        int current = cloak == null ? 0 : cloak.getAmplifier() + 1;
        if (stacks > current && world.getTime() >= state.cloakDisabledUntil)
            actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK),
                    280, stacks - 1, false, false, true), actor);
        if (state.tuning.has(Phase10AbilityTuning.Setting.HEALTH_THRESHOLD)
                && actor.getHealth() <= actor.getMaxHealth() * state.tuning.get(
                Phase10AbilityTuning.Setting.HEALTH_THRESHOLD, .3)
                && world.getTime() >= state.emergencyReady) {
            actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK),
                    280, Math.min(max - 1, stacks), false, false, true), actor);
            state.emergencyReady = world.getTime() + state.tuning.integer(
                    Phase10AbilityTuning.Setting.LOCKOUT_TICKS, 200);
        }
        applyDreadAttributes(actor, state.tuning, current);
    }

    private static HeldState refresh(Map<UUID, HeldState> states, UniqueAbilityDefinition definition,
                                     ServerWorld world, LivingEntity actor, ItemStack stack) {
        HeldState state = states.computeIfAbsent(actor.getUuid(), ignored -> new HeldState());
        if (world.getTime() >= state.refreshAt) {
            UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(definition, world, stack, actor, null);
            state.tuning = Phase10UniqueAbilities.tuning(execution);
            state.refreshAt = world.getTime() + 20;
            Phase10CombatManager.finish(execution, 0);
        }
        return state;
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        ItemStack stack = actor.getMainHandStack();
        if (stack.isOf(ItemsRegistry.RIBBONCLEAVER.get())) {
            HeldState state = RIBBON.get(actor.getUuid());
            if (state != null && state.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0) != 0) {
                double desired = state.tuning.get(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .85);
                if (state.tuning.flag(1 << 8) && actor.horizontalSpeed > .15F) desired = .88;
                if (state.tuning.flag(1 << 4) && source.getAttacker() instanceof LivingEntity)
                    state.retortUntil = actor.getWorld().getTime() + 40;
                if (state.tuning.flag(1 << 6) && ++state.absorbedHits % 3 == 0)
                    actor.setAbsorptionAmount(Math.max(actor.getAbsorptionAmount(), 4));
                return amount * (float) (desired / .85);
            }
        }
        return amount;
    }

    public static float modifyVoidcloakDamage(LivingEntity actor, DamageSource source, float amount,
                                              StatusEffectInstance cloak) {
        HeldState state = DREAD.get(actor.getUuid());
        if (state == null || state.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0) {
            HelperMethods.decrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            return amount * (1 - (cloak.getAmplifier() + 1) * .10F);
        }
        int stacks = cloak.getAmplifier() + 1;
        double perStack = 1 - state.tuning.get(Phase10AbilityTuning.Setting.INCOMING_MULTIPLIER, .9);
        long now = actor.getWorld().getTime();
        if (!state.tuning.flag(1 << 2) || now >= state.cloakLossAt) {
            HelperMethods.decrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            state.cloakLossAt = now + state.tuning.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS, 15);
            if (state.tuning.flag(1 << 5)) state.retortUntil = now + 60;
        }
        if (state.tuning.flag(1 << 24) && corruption(actor.getMainHandStack()) > 80) amount *= 1.08F;
        if (state.tuning.flag(1 << 21) && corruption(actor.getMainHandStack()) > 60) amount *= .92F;
        return amount * (float) Math.max(0, 1 - stacks * perStack);
    }

    public static void onRibbonHit(ServerWorld world, LivingEntity actor, LivingEntity target, ItemStack stack) {
        UniqueAbilityExecution execution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.RIBBON_PROMISE, world, stack, actor, target);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        if (actor.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE))) {
            float weapon = (float) HelperMethods.getEntityAttackDamage(actor);
            float damage = weapon * (float) Math.max(0, tuning.get(
                    Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER,
                    Config.uniqueEffects.ribboncleaver.damageBonusPercent)
                    - Config.uniqueEffects.ribboncleaver.damageBonusPercent);
            HeldState state = RIBBON.get(actor.getUuid());
            if (tuning.has(Phase10AbilityTuning.Setting.WINDUP_TICKS) && state != null
                    && world.getTime() - state.activatedAt >= tuning.integer(
                    Phase10AbilityTuning.Setting.WINDUP_TICKS, 40)) damage += weapon * .2F;
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
            if (radius > 0 && tuning.integer(Phase10AbilityTuning.Setting.TARGET_CAP, 1) > 1) {
                float cleaveDamage = damage * (float) tuning.get(
                        Phase10AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .4);
                world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius),
                                other -> other != target && HelperMethods.checkAbilityTarget(other, actor))
                        .stream().sorted(Comparator.comparingDouble(target::squaredDistanceTo))
                        .limit(tuning.integer(Phase10AbilityTuning.Setting.TARGET_CAP, 1) - 1L)
                        .forEach(other -> HelperMethods.damageThroughIframes(other,
                                actor.getDamageSources().indirectMagic(actor, actor), cleaveDamage));
            }
        }
        Phase10CombatManager.finish(execution, 1);
    }

    public static Phase10AbilityTuning ribbonRush(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                   LivingEntity target) {
        UniqueAbilityExecution execution = Phase10CombatManager.beginDirectActive(
                Phase10UniqueAbilities.RIBBON_RUSH, world, actor, stack,
                net.minecraft.util.Hand.MAIN_HAND, Config.uniqueEffects.ribboncleaver.cooldown);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        UniqueAbilityExecution promiseExecution = Phase10CombatManager.beginPassive(
                Phase10UniqueAbilities.RIBBON_PROMISE, world, stack, actor, target);
        Phase10AbilityTuning promise = Phase10UniqueAbilities.tuning(promiseExecution);
        HeldState state = RIBBON.computeIfAbsent(actor.getUuid(), ignored -> new HeldState());
        state.activatedAt = world.getTime();
        double speed = tuning.get(Phase10AbilityTuning.Setting.SPEED, 1.7);
        if (target != null && !tuning.flag(1 << 17))
            LivingEntityAbilityMovementManager.dashTowardTarget(world, actor, target, speed, 8);
        else {
            Vec3d velocity = actor.getRotationVector().multiply(speed);
            actor.setVelocity(velocity.x, tuning.flag(1 << 17) ? velocity.y : 0, velocity.z);
            actor.velocityModified = true;
        }
        int window = promise.integer(Phase10AbilityTuning.Setting.DURATION_TICKS, 60);
        actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE),
                window, 0, false, false, true));
        if (!tuning.flag(1 << 17)) actor.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.RESILIENCE),
                15 + tuning.integer(Phase10AbilityTuning.Setting.SECONDARY_DURATION_TICKS, 0),
                Config.uniqueEffects.ribboncleaver.resilienceAmplifier, false, false, true));
        int cooldown = tuning.integer(Phase10AbilityTuning.Setting.COOLDOWN_TICKS,
                Config.uniqueEffects.ribboncleaver.cooldown);
        SimplySwordsAPI.setWeaponCooldown(actor, stack, cooldown);
        Phase10CombatManager.finish(execution, target == null ? 0 : 1);
        Phase10CombatManager.finish(promiseExecution, 0);
        return tuning;
    }

    public static void onDreadtideHit(LivingEntity actor, LivingEntity target, ItemStack stack) {
        HeldState state = DREAD.get(actor.getUuid());
        if (state == null || actor.getWorld().getTime() > state.retortUntil) return;
        float bonus = (float) HelperMethods.getAttackFromStack(stack,
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND) * .12F;
        HelperMethods.damageThroughIframes(target, actor.getDamageSources().indirectMagic(actor, actor), bonus);
        state.retortUntil = 0;
    }

    private static void applyDreadAttributes(LivingEntity actor, Phase10AbilityTuning tuning, int stacks) {
        removeDreadAttributes(actor);
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && tuning.get(Phase10AbilityTuning.Setting.SPEED, 0) > 0 && !tuning.flag(1 << 7))
            speed.addTemporaryModifier(new EntityAttributeModifier(DREAD_SPEED,
                    stacks * tuning.get(Phase10AbilityTuning.Setting.SPEED, 0),
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null && tuning.flag(1 << 8)) attackSpeed.addTemporaryModifier(
                new EntityAttributeModifier(DREAD_ATTACK_SPEED, stacks * .08,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var health = actor.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null && tuning.flag(1 << 25)) health.addTemporaryModifier(new EntityAttributeModifier(
                DREAD_HEALTH, -8, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeDreadAttributes(LivingEntity actor) {
        var speed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(DREAD_SPEED);
        var attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(DREAD_ATTACK_SPEED);
        var health = actor.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) health.removeModifier(DREAD_HEALTH);
    }

    public static boolean activateDreadtide(ServerWorld world, LivingEntity actor, ItemStack stack) {
        HeldState cloakState = refresh(DREAD, Phase10UniqueAbilities.DREAD_CLOAK, world, actor, stack);
        if (cloakState.tuning.integer(Phase10AbilityTuning.Setting.MODE, 0) == 0) return false;
        Entity closest = world.getOtherEntities(actor, HelperMethods.createBox(actor, 10),
                        EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity living && HelperMethods.checkFriendlyFire(living, actor))
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(actor))).orElse(null);
        StatusEffectInstance cloak = actor.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        if (!(closest instanceof LivingEntity target) || cloak == null) return false;
        UniqueAbilityExecution execution = Phase10CombatManager.beginDirectActive(
                Phase10UniqueAbilities.DREAD_ASSAULT, world, actor, stack, net.minecraft.util.Hand.MAIN_HAND, 20);
        Phase10AbilityTuning tuning = Phase10UniqueAbilities.tuning(execution);
        int stacks = cloak.getAmplifier() + 1;
        int duration = tuning.integer(Phase10AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.dreadtide.get().duration);
        if (cloakState.tuning.flag(1 << 26)) duration += corruption(stack);
        float multiplier = (float) tuning.get(Phase10AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        int currentCorruption = corruption(stack);
        if (cloakState.tuning.flag(1 << 20) && currentCorruption > 40) multiplier *= 1.1F;
        if (cloakState.tuning.flag(1 << 25)) multiplier *= 1.25F;
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
        ASSAULTS.put(target.getUuid(), new AssaultState(tuning, execution, actor.getUuid(), stack.copy(),
                world.getTime() + duration));
        int removed = cloakState.tuning.flag(1 << 26) ? corruption(stack)
                : cloakState.tuning.flag(1 << 22) ? Math.min(corruption(stack), stacks * 5) : 0;
        if (removed > 0) setCorruption(stack, Math.max(cloakState.tuning.flag(1 << 25) ? 75 : 0,
                corruption(stack) - removed));
        if (removed >= 20 && cloakState.tuning.flag(1 << 23)) actor.setAbsorptionAmount((float) Math.min(
                actor.getAbsorptionAmount() + removed / 20F * 4F,
                cloakState.tuning.get(Phase10AbilityTuning.Setting.TARGET_CAP, 12)));
        if (cloakState.tuning.flag(1 << 26)) cloakState.cloakDisabledUntil = world.getTime()
                + cloakState.tuning.integer(Phase10AbilityTuning.Setting.LOCKOUT_TICKS, 200);
        else if (tuning.flag(1 << 16)) cloakState.cloakDisabledUntil = world.getTime() + duration;
        actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
        SimplySwordsAPI.setWeaponCooldown(actor, stack, 20);
        return true;
    }

    public static boolean tickAssault(LivingEntity target, int amplifier, int baseDamage) {
        if (!(target.getWorld() instanceof ServerWorld world)) return false;
        AssaultState state = ASSAULTS.get(target.getUuid());
        if (state == null) return false;
        LivingEntity actor = world.getEntity(state.actorId) instanceof LivingEntity living ? living : null;
        if (actor == null || world.getTime() >= state.expiresAt || !target.isAlive()) {
            Phase10CombatManager.finish(state.execution, target.isAlive() ? 1 : 0);
            ASSAULTS.remove(target.getUuid());
            return true;
        }
        int interval = Math.max(1, state.tuning.integer(Phase10AbilityTuning.Setting.INTERVAL_TICKS,
                Math.max(1, Config.uniqueEffects.dreadtide.get().startingTickFrequency - amplifier * 2)));
        if (target.age % interval != 0) return true;
        float damage = baseDamage + amplifier;
        target.timeUntilRegen = 0;
        target.damage(target.getDamageSources().indirectMagic(target, actor), damage);
        if (state.tuning.has(Phase10AbilityTuning.Setting.STATUS_DURATION_TICKS)) target.addStatusEffect(
                new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1), actor);
        return true;
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
        private long refreshAt;
        private long nextCorruption;
        private long cloakLossAt;
        private long retortUntil;
        private long cloakDisabledUntil;
        private long emergencyReady;
        private long activatedAt;
        private int movingTicks;
        private int absorbedHits;
    }

    private record AssaultState(Phase10AbilityTuning tuning, UniqueAbilityExecution execution,
                                UUID actorId, ItemStack stack, long expiresAt) {
    }
}
