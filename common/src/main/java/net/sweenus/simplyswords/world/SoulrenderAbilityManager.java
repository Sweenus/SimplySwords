package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SoulrenderAbilityManager {
    private static final net.minecraft.util.Identifier PATIENCE_ID =
            net.minecraft.util.Identifier.of("simplyswords", "soulrender_deathly_patience");
    private static final int PATIENCE_INTERVAL_TICKS = 10;
    private static final Map<ServerWorld, Map<UUID, OwnerState>> OWNERS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> FRESH_INK = new HashMap<>();

    private SoulrenderAbilityManager() {
    }

    public static boolean isMarked(LivingEntity target) {
        return target != null && target.hasStatusEffect(StatusEffects.SLOWNESS)
                && target.hasStatusEffect(StatusEffects.WEAKNESS);
    }

    // Rendmarks: Fresh Ink grants extra opening stacks against an unafflicted enemy.
    public static int openingStacks(ServerWorld world, LivingEntity target, StormSoulMasteryTuning tuning) {
        int stacks = tuning.integer(StormSoulMasteryTuning.Setting.FRESH_INK_STACKS, 1);
        if (stacks <= 1 || world == null || target == null) return 1;
        int lockout = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.FRESH_INK_LOCKOUT_TICKS, 80));
        Map<UUID, Long> ready = FRESH_INK.computeIfAbsent(world, ignored -> new HashMap<>());
        long now = world.getTime();
        Long next = ready.get(target.getUuid());
        if (next != null && now < next) return 1;
        ready.put(target.getUuid(), now + lockout);
        return stacks;
    }

    // Rendmarks: Echoed Curse copies a mark to the nearest other enemy.
    public static void echoMark(ServerWorld world, LivingEntity attacker, LivingEntity target,
                                StormSoulMasteryTuning tuning) {
        int chance = tuning.integer(StormSoulMasteryTuning.Setting.ECHO_CHANCE, 0);
        double range = tuning.get(StormSoulMasteryTuning.Setting.ECHO_RANGE, 0);
        if (chance <= 0 || range <= 0 || world == null || attacker == null || target == null) return;
        OwnerState state = owner(world, attacker.getUuid());
        long now = world.getTime();
        if (now < state.echoReady) return;
        int roll = attacker.getRandom().nextInt(100);
        boolean passed = roll < chance;
        UniqueAbilityApi.reportRoll(attacker, StormSoulMasteryAbilities.SOULRENDER_MARK.id(),
                "ECHO_CHANCE", chance, roll, passed);
        if (!passed) return;
        state.echoReady = now + Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.ECHO_LOCKOUT_TICKS, 20));

        int cap = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.ECHO_TARGET_CAP, 1));
        int duration = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.ECHO_DURATION_TICKS, 300));
        int copied = 0;
        for (LivingEntity nearby : nearestTargets(world, attacker, target.getPos(), range, cap + 1)) {
            if (nearby == target) continue;
            applyStack(nearby, StatusEffects.SLOWNESS, duration, attacker, 64);
            applyStack(nearby, StatusEffects.WEAKNESS, duration, attacker, 1);
            SoulrenderMarkVisualManager.refreshMark(world, nearby, duration);
            world.spawnParticles(ParticleTypes.SOUL, nearby.getX(), nearby.getBodyY(0.6), nearby.getZ(),
                    4, 0.2, 0.2, 0.2, 0.01);
            if (++copied >= cap) break;
        }
    }

    // The Reaping: enemies just beyond the radius are dragged inward before the harvest.
    public static void reachPull(ServerWorld world, LivingEntity user, double radius, StormSoulMasteryTuning tuning) {
        double bonus = tuning.get(StormSoulMasteryTuning.Setting.REACH_BONUS_RANGE, 0);
        double strength = tuning.get(StormSoulMasteryTuning.Setting.REACH_PULL_STRENGTH, 0);
        if (bonus <= 0 || strength <= 0 || world == null || user == null) return;
        int cap = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.REACH_TARGET_CAP, 8));
        int pulled = 0;
        for (LivingEntity target : nearestTargets(world, user, user.getPos(), radius + bonus, cap * 2)) {
            if (!isMarked(target) || user.squaredDistanceTo(target) <= radius * radius) continue;
            Vec3d inward = user.getPos().subtract(target.getPos());
            double length = inward.horizontalLength();
            if (length < 1.0E-4) continue;
            double resistance = MathHelper.clamp(
                    target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double applied = strength * (1.0 - resistance);
            if (applied <= 0) continue;
            Vec3d step = inward.multiply(1.0 / length).multiply(Math.min(applied, length));
            target.setPosition(target.getX() + step.x, target.getY(), target.getZ() + step.z);
            target.velocityModified = true;
            if (++pulled >= cap) break;
        }
    }

    // The Reaping: Shared Ending splits a lethal harvest into the nearest surviving mark.
    public static void sharedEnding(ServerWorld world, LivingEntity user, ItemStack stack, LivingEntity victim,
                                    float dealt, StormSoulMasteryTuning tuning, OwnerReapState reap) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        double multiplier = tuning.get(StormSoulMasteryTuning.Setting.SHARED_END_DAMAGE_MULTIPLIER, 0);
        double range = tuning.get(StormSoulMasteryTuning.Setting.SHARED_END_RANGE, 0);
        if (multiplier <= 0 || range <= 0 || dealt <= 0 || reap == null) return;
        int cap = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.SHARED_END_TARGET_CAP, 6));
        if (reap.sharedEndings >= cap) return;
        for (LivingEntity nearby : nearestTargets(world, user, victim.getPos(), range, 4)) {
            if (nearby == victim || !nearby.isAlive() || !isMarked(nearby)) continue;
            DamageSource source = user.getDamageSources().indirectMagic(user, user);
            CombatProvenanceApi.damage(stack, (source).getAttacker(), nearby, source, HelperMethods.applyAbilityDamageEnchantments(world, stack, nearby, source,
                    (float) (dealt * multiplier)));
            reap.sharedEndings++;
            return;
        }
        }
    }

    // Gravebound: records what a completed harvest leaves behind on its wielder.
    public static void recordReap(ServerWorld world, LivingEntity user, StormSoulMasteryTuning tuning,
                                  int consumed, float dealt, int kills, int quietusAbsorption) {
        if (world == null || user == null || consumed <= 0) return;
        long now = world.getTime();

        int sheath = tuning.integer(StormSoulMasteryTuning.Setting.SHEATH_ABSORPTION, 0);
        if (sheath > 0) {
            grantAbsorption(user, sheath,
                    Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.SHEATH_DURATION_TICKS, 80)));
        }
        if (quietusAbsorption > 0) {
            grantAbsorption(user, quietusAbsorption,
                    Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.SHEATH_DURATION_TICKS, 80)));
        }

        double shelterRatio = tuning.get(StormSoulMasteryTuning.Setting.SHELTER_ABSORPTION_RATIO, 0);
        if (shelterRatio > 0 && dealt > 0) {
            int amount = (int) Math.min(tuning.get(StormSoulMasteryTuning.Setting.SHELTER_ABSORPTION_CAP, 8),
                    Math.round(dealt * shelterRatio));
            if (amount > 0) {
                grantAbsorption(user, amount,
                        Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.SHELTER_DURATION_TICKS, 120)));
            }
        }

        int speedThreshold = tuning.integer(StormSoulMasteryTuning.Setting.REAP_SPEED_THRESHOLD, 0);
        if (speedThreshold > 0 && consumed >= speedThreshold) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                    Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.REAP_SPEED_DURATION_TICKS, 60)),
                    0, false, true, true), user);
        }
        int hasteThreshold = tuning.integer(StormSoulMasteryTuning.Setting.REAP_HASTE_THRESHOLD, 0);
        if (hasteThreshold > 0 && consumed >= hasteThreshold) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,
                    Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.REAP_HASTE_DURATION_TICKS, 100)),
                    Math.max(0, tuning.integer(StormSoulMasteryTuning.Setting.REAP_HASTE_AMPLIFIER, 1)),
                    false, true, true), user);
        }

        OwnerState state = owner(world, user.getUuid());
        double reserveRatio = tuning.get(StormSoulMasteryTuning.Setting.GRAVE_RESERVE_RATIO, 0);
        if (reserveRatio > 0 && dealt > 0) {
            state.reserveStored = (float) Math.min(tuning.get(StormSoulMasteryTuning.Setting.GRAVE_RESERVE_CAP, 6),
                    state.reserveStored + dealt * reserveRatio);
            state.reserveThreshold = tuning.get(StormSoulMasteryTuning.Setting.GRAVE_RESERVE_HEALTH_THRESHOLD, 0.35);
            state.reserveDuration = Math.max(1,
                    tuning.integer(StormSoulMasteryTuning.Setting.GRAVE_RESERVE_DURATION_TICKS, 100));
        }

        double titheBonus = tuning.get(StormSoulMasteryTuning.Setting.TITHE_KILL_BONUS, 0);
        if (titheBonus > 0 && kills > 0) {
            double cap = tuning.get(StormSoulMasteryTuning.Setting.TITHE_BONUS_CAP, 0.5);
            if (now >= state.titheExpires) state.titheBonus = 0;
            state.titheBonus = Math.min(cap, state.titheBonus + titheBonus * kills);
            state.titheExpires = now + Math.max(1,
                    tuning.integer(StormSoulMasteryTuning.Setting.TITHE_DURATION_TICKS, 100));
        }
    }

    // Gravebound: Death's Due spends its stored bonus on the next attack.
    public static double takeTitheBonus(ServerWorld world, LivingEntity user) {
        Map<UUID, OwnerState> states = OWNERS.get(world);
        OwnerState state = states == null ? null : states.get(user.getUuid());
        if (state == null || state.titheBonus <= 0) return 0;
        if (world.getTime() >= state.titheExpires) {
            state.titheBonus = 0;
            return 0;
        }
        double bonus = state.titheBonus;
        state.titheBonus = 0;
        return bonus;
    }

    public static float modifyIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0.0F
                || !(actor.getWorld() instanceof ServerWorld world)) {
            return amount;
        }
        if (!(source.getAttacker() instanceof LivingEntity attacker) || !isMarked(attacker)) return amount;
        ItemStack stack = findHeldSoulrender(actor);
        if (stack.isEmpty()) return amount;

        OwnerState state = owner(world, actor.getUuid());
        long now = world.getTime();
        if (now < state.coldGripReady) return amount;
        StormSoulMasteryTuning tuning = graveTuning(world, actor, stack, attacker);
        int slowTicks = tuning.integer(StormSoulMasteryTuning.Setting.COLD_GRIP_SLOW_TICKS, 0);
        if (slowTicks <= 0) return amount;
        state.coldGripReady = now + Math.max(1,
                tuning.integer(StormSoulMasteryTuning.Setting.COLD_GRIP_LOCKOUT_TICKS, 40));
        int cap = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.COLD_GRIP_TARGET_CAP, 8));
        int affected = 0;
        for (LivingEntity nearby : nearestTargets(world, actor, actor.getPos(), 6.0, cap * 2)) {
            if (!isMarked(nearby)) continue;
            nearby.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, 0,
                    false, true, true), actor);
            if (++affected >= cap) break;
        }
        if (affected == 0) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, 0,
                    false, true, true), actor);
        }
        return amount;
    }

    // Gravebound: Unbroken Reaper spends nearby marks to survive a lethal blow.
    public static boolean tryUnbrokenReaper(LivingEntity actor, DamageSource source) {
        if (actor == null || source == null || !(actor.getWorld() instanceof ServerWorld world)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        ItemStack stack = findHeldSoulrender(actor);
        if (stack.isEmpty()) return false;
        OwnerState state = owner(world, actor.getUuid());
        long now = world.getTime();
        if (now < state.unbrokenReady) return false;

        StormSoulMasteryTuning tuning = graveTuning(world, actor, stack, null);
        int threshold = tuning.integer(StormSoulMasteryTuning.Setting.UNBROKEN_MARK_THRESHOLD, 0);
        double range = tuning.get(StormSoulMasteryTuning.Setting.UNBROKEN_RANGE, 0);
        if (threshold <= 0 || range <= 0) return false;
        int cap = Math.max(threshold, tuning.integer(StormSoulMasteryTuning.Setting.UNBROKEN_TARGET_CAP, threshold));

        List<LivingEntity> marked = new ArrayList<>();
        for (LivingEntity nearby : nearestTargets(world, actor, actor.getPos(), range, cap * 2)) {
            if (!isMarked(nearby)) continue;
            marked.add(nearby);
            if (marked.size() >= cap) break;
        }
        if (marked.size() < threshold) return false;

        state.unbrokenReady = now + Math.max(1,
                tuning.integer(StormSoulMasteryTuning.Setting.UNBROKEN_LOCKOUT_TICKS, 1200));
        for (LivingEntity consumed : marked) {
            SoulrenderMarkVisualManager.consumeMark(world, consumed, actor);
            consumed.removeStatusEffect(StatusEffects.SLOWNESS);
            consumed.removeStatusEffect(StatusEffects.WEAKNESS);
        }
        actor.setHealth(1.0F);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.UNBROKEN_RESIST_TICKS, 40)),
                1, false, true, true), actor);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, actor.getX(), actor.getBodyY(0.6), actor.getZ(),
                24, 0.4, 0.6, 0.4, 0.02);
        world.playSound(null, actor.getX(), actor.getY(), actor.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE,
                SoundCategory.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    // Gravebound: Borrowed Time rewards a kill on a marked enemy.
    public static void onTargetDeath(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null || !(victim.getWorld() instanceof ServerWorld world)) return;
        if (!(source.getAttacker() instanceof LivingEntity killer) || !isMarked(victim)) return;
        ItemStack stack = findHeldSoulrender(killer);
        if (stack.isEmpty()) return;
        OwnerState state = owner(world, killer.getUuid());
        long now = world.getTime();
        if (now < state.borrowedTimeReady) return;
        StormSoulMasteryTuning tuning = graveTuning(world, killer, stack, victim);
        int duration = tuning.integer(StormSoulMasteryTuning.Setting.BORROWED_TIME_TICKS, 0);
        if (duration <= 0) return;
        state.borrowedTimeReady = now + Math.max(1,
                tuning.integer(StormSoulMasteryTuning.Setting.BORROWED_TIME_LOCKOUT_TICKS, 40));
        killer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0,
                false, true, true), killer);
    }

    // Gravebound: Deathly Patience and Grave Reserve are evaluated while the sword is held.
    public static void tickHolder(ServerWorld world, LivingEntity holder, ItemStack stack) {
        if (world == null || holder == null || stack.isEmpty()) return;
        owner(world, holder.getUuid());
        tickHolder(world, holder);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % PATIENCE_INTERVAL_TICKS != 0) return;
        Map<UUID, OwnerState> states = OWNERS.get(world);
        if (states == null) return;
        for (UUID id : new ArrayList<>(states.keySet())) {
            if (world.getEntity(id) instanceof LivingEntity holder && holder.isAlive()) tickHolder(world, holder);
            else states.remove(id);
        }
        if (states.isEmpty()) OWNERS.remove(world);
    }

    private static void tickHolder(ServerWorld world, LivingEntity holder) {
        long now = world.getTime();
        if (now % PATIENCE_INTERVAL_TICKS != 0) return;
        OwnerState state = owner(world, holder.getUuid());
        if (state.lastHeldTick == now) return;
        state.lastHeldTick = now;
        double resistance = 0;
        boolean held = false;
        for (ItemStack stack : List.of(holder.getMainHandStack(), holder.getOffHandStack())) {
            if (!stack.isOf(ItemsRegistry.SOULRENDER.get()) || !AwakeningApi.isAbilityUnlocked(stack)) continue;
            held = true;
            StormSoulMasteryTuning tuning = graveTuning(world, holder, stack, null);
            double range = tuning.get(StormSoulMasteryTuning.Setting.PATIENCE_RANGE, 0);
            double bonus = tuning.get(StormSoulMasteryTuning.Setting.KNOCKBACK_RESISTANCE_BONUS, 0);
            if (range > 0 && bonus > resistance && anyMarkedWithin(world, holder, range)) resistance = bonus;
        }
        if (resistance > 0) applyPatience(holder, resistance);
        else removePatience(holder);
        if (held && state.reserveStored > 0 && state.reserveThreshold > 0
                && holder.getHealth() <= holder.getMaxHealth() * state.reserveThreshold) {
            grantAbsorption(holder, Math.round(state.reserveStored), state.reserveDuration);
            state.reserveStored = 0;
        }
    }

    public static void clear(ServerWorld world) {
        Map<UUID, OwnerState> states = OWNERS.remove(world);
        if (states != null) {
            for (UUID ownerId : states.keySet()) {
                Entity entity = world.getEntity(ownerId);
                if (entity instanceof LivingEntity living) removePatience(living);
            }
        }
        FRESH_INK.remove(world);
    }

    public static void clearAll() {
        OWNERS.clear();
        FRESH_INK.clear();
    }

    public static void removeActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, OwnerState> states = OWNERS.get(world);
        if (states != null) states.remove(actor.getUuid());
        Map<UUID, Long> ready = FRESH_INK.get(world);
        if (ready != null) ready.remove(actor.getUuid());
        removePatience(actor);
    }

    private static boolean anyMarkedWithin(ServerWorld world, LivingEntity holder, double range) {
        Box box = holder.getBoundingBox().expand(range, range / 2.0, range);
        for (Entity entity : world.getOtherEntities(holder, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity living && HelperMethods.checkAbilityTarget(living, holder)
                    && isMarked(living) && holder.squaredDistanceTo(living) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private static void applyPatience(LivingEntity holder, double resistance) {
        EntityAttributeInstance attribute = holder.getAttributeInstance(
                EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        EntityAttributeModifier current = attribute.getModifier(PATIENCE_ID);
        if (current != null && current.value() == resistance) return;
        attribute.removeModifier(PATIENCE_ID);
        attribute.addTemporaryModifier(new EntityAttributeModifier(PATIENCE_ID, resistance,
                EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private static void removePatience(LivingEntity holder) {
        EntityAttributeInstance attribute = holder.getAttributeInstance(
                EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (attribute != null) attribute.removeModifier(PATIENCE_ID);
    }

    private static void applyStack(LivingEntity target, net.minecraft.registry.entry.RegistryEntry
            <net.minecraft.entity.effect.StatusEffect> effect, int duration, LivingEntity source, int cap) {
        StatusEffectInstance existing = target.getStatusEffect(effect);
        int amplifier = existing == null ? 0 : Math.min(cap, existing.getAmplifier() + 1);
        target.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier), source);
    }

    private static void grantAbsorption(LivingEntity user, int amount, int durationTicks) {
        if (amount <= 0) return;
        float capped = Math.min(Config.uniqueEffects.abilityAbsorptionCap, amount);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, Math.max(1, durationTicks),
                MathHelper.clamp(amount / 4, 0, 9), false, true, true), user);
        user.setAbsorptionAmount(Math.max(user.getAbsorptionAmount(), capped));
    }

    private static List<LivingEntity> nearestTargets(ServerWorld world, LivingEntity user, Vec3d origin,
                                                     double range, int limit) {
        Box box = new Box(origin.x - range, origin.y - range / 2.0, origin.z - range,
                origin.x + range, origin.y + range / 2.0, origin.z + range);
        List<LivingEntity> found = new ArrayList<>();
        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity living && HelperMethods.checkAbilityTarget(living, user)
                    && living.squaredDistanceTo(origin.x, origin.y, origin.z) <= range * range) {
                found.add(living);
            }
        }
        found.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(origin.x, origin.y, origin.z)));
        return found.size() > limit ? found.subList(0, limit) : found;
    }

    private static StormSoulMasteryTuning graveTuning(ServerWorld world, LivingEntity owner, ItemStack stack,
                                                   LivingEntity other) {
        UniqueAbilityExecution execution = UniqueAbilityApi.preparePassive(StormSoulMasteryAbilities.SOULRENDER_GRAVE,
                UniqueAbilityContext.passive(world, stack, owner, other, null), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
        UniqueAbilityApi.start(execution);
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, 0);
        return tuning;
    }

    private static ItemStack findHeldSoulrender(LivingEntity actor) {
        ItemStack main = actor.getMainHandStack();
        if (main.isOf(ItemsRegistry.SOULRENDER.get()) && AwakeningApi.isAbilityUnlocked(main)) return main;
        ItemStack off = actor.getOffHandStack();
        if (off.isOf(ItemsRegistry.SOULRENDER.get()) && AwakeningApi.isAbilityUnlocked(off)) return off;
        return ItemStack.EMPTY;
    }

    private static OwnerState owner(ServerWorld world, UUID ownerId) {
        return OWNERS.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(ownerId, ignored -> new OwnerState());
    }

    public static final class OwnerReapState {
        public int sharedEndings;
    }

    private static final class OwnerState {
        private long lastHeldTick = Long.MIN_VALUE;
        private long echoReady = Long.MIN_VALUE;
        private long coldGripReady = Long.MIN_VALUE;
        private long borrowedTimeReady = Long.MIN_VALUE;
        private long unbrokenReady = Long.MIN_VALUE;
        private float reserveStored;
        private double reserveThreshold;
        private int reserveDuration = 100;
        private double titheBonus;
        private long titheExpires = Long.MIN_VALUE;
    }
}
