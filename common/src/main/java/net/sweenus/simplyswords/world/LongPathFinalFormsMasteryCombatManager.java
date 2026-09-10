package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryTuning;
import net.sweenus.simplyswords.api.ability.LongPathFinalFormsMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.item.custom.HarbingerSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LongPathFinalFormsMasteryCombatManager {
    private static final int HELD_RESOLVE_INTERVAL = 5;
    private static final Map<UUID, PassiveState> STATES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, PassiveState>> SUNFIRE_STATES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, AllyCharge>> SUNFIRE_CHARGES = new HashMap<>();
    private static final Map<UUID, ReserveHudState> RESERVE_HUD = new HashMap<>();
    private static final ThreadLocal<AttackAction> ATTACK_ACTION = new ThreadLocal<>();

    public static void beginAttack(LivingEntity owner) {
        AttackAction action = ATTACK_ACTION.get();
        if (action == null) ATTACK_ACTION.set(new AttackAction(owner));
        else action.depth++;
    }

    public static void endAttack() {
        AttackAction action = ATTACK_ACTION.get();
        if (action != null && --action.depth == 0) ATTACK_ACTION.remove();
    }

    public static boolean isSunfire(ItemStack stack) {
        return stack.getItem() instanceof net.sweenus.simplyswords.item.custom.SunfireSwordItem
                || stack.getItem() instanceof net.sweenus.simplyswords.item.custom.DormantRelicSwordItem
                && net.sweenus.simplyswords.api.AwakeningApi.getFormId(stack)
                .filter(id -> id.equals(net.minecraft.util.Identifier.of("simplyswords", "sunfire"))).isPresent();
    }

    public static boolean isHarbinger(ItemStack stack) {
        return stack.getItem() instanceof HarbingerSwordItem
                || stack.getItem() instanceof net.sweenus.simplyswords.item.custom.DormantRelicSwordItem
                && net.sweenus.simplyswords.api.AwakeningApi.getFormId(stack)
                .filter(id -> id.equals(net.minecraft.util.Identifier.of("simplyswords", "harbinger"))).isPresent();
    }

    public static boolean melee(DamageSource source) {
        return source.getSource() == source.getAttacker()
                && !source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)
                && directAttack(source);
    }

    public static ItemStack heldSunfire(LivingEntity owner) {
        if (isSunfire(owner.getMainHandStack()))
            return owner.getMainHandStack();
        if (isSunfire(owner.getOffHandStack()))
            return owner.getOffHandStack();
        return ItemStack.EMPTY;
    }

    private static PassiveState sunfireState(LivingEntity owner) {
        ServerWorld world = (ServerWorld) owner.getWorld();
        SUNFIRE_STATES.forEach((otherWorld, values) -> {
            if (otherWorld != world) values.remove(owner.getUuid());
        });
        Map<UUID, PassiveState> states = SUNFIRE_STATES.computeIfAbsent(world, ignored -> new HashMap<>());
        states.entrySet().removeIf(entry -> entry.getValue().expiresAt <= world.getTime()
                || !(world.getEntity(entry.getKey()) instanceof LivingEntity living) || !living.isAlive()
                || living.getWorld() != world);
        PassiveState state = states.compute(owner.getUuid(), (ignored, previous) -> {
            if (previous != null && previous.sunfireOwner == owner) return previous;
            PassiveState created = new PassiveState();
            created.sunfireOwner = owner;
            return created;
        });
        state.expiresAt = world.getTime() + 2400;
        return state;
    }

    public static void grantSunfireRegeneration(LivingEntity owner, int duration, int amplifier) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return;
        if (owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, amplifier), owner)) {
            PassiveState state = sunfireState(owner);
            state.regenUntil = Math.max(state.regenUntil, world.getTime() + duration);
        }
    }

    public static void onStatusEffectRemoved(LivingEntity owner, StatusEffectInstance effect) {
        if (effect.getEffectType().equals(StatusEffects.REGENERATION)
                && owner.getWorld() instanceof ServerWorld world) {
            PassiveState state = SUNFIRE_STATES.getOrDefault(world, Map.of()).get(owner.getUuid());
            if (state != null) state.regenUntil = 0;
        }
    }

    private static boolean directAttack(DamageSource source) {
        return source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.PLAYER_ATTACK)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.MOB_ATTACK)
                || source.isOf(net.minecraft.entity.damage.DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    private static final class AttackAction {
        private final LivingEntity owner;
        private int depth = 1;
        private boolean consumed;
        private AttackAction(LivingEntity owner) { this.owner = owner; }
    }
    private static final java.util.Set<LivingEntity> HARBINGER_DEATHS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private LongPathFinalFormsMasteryCombatManager() {
    }

    public static void sunfireMelee(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;
        UniqueAbilityExecution execution = begin(LongPathFinalFormsMasteryAbilities.SUNFIRE_REGEN, world, stack, attacker, target);
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        int chance = tuning.integer(s("CHANCE"), Config.uniqueEffects.sunfire.chance);
        boolean exact = tuning.has(s("CHANCE"));
        int roll = -1;
        boolean proc;
        if (chance >= 100) proc = true;
        else if (chance <= 0) proc = false;
        else {
            roll = attacker.getRandom().nextInt(100);
            proc = exact ? roll < chance : roll <= chance;
        }
        UniqueAbilityApi.reportRoll(attacker, LongPathFinalFormsMasteryAbilities.SUNFIRE_REGEN.id(),
                "CHANCE", chance, roll, proc);
        PassiveState state = sunfireState(attacker);
        state.sunfireTuning = tuning;
        if (!tuning.flag(4096) || tuning.flag(131072)) state.reserve = 0;
        if (!tuning.flag(131072)) state.comboHits.clear();
        if (tuning.flag(131072)) {
            state.reserve = 0;
            AttackAction action = ATTACK_ACTION.get();
            if (action != null && action.owner == attacker && action.consumed) {
                UniqueAbilityApi.cancel(execution);
                return;
            }
            if (action != null && action.owner == attacker) action.consumed = true;
            long now = world.getTime();
            state.comboHits.removeIf(tick -> now - tick > tuning.integer(s("COMBO_WINDOW_TICKS"), 80));
            state.comboHits.addLast(now);
            if (state.comboHits.size() >= Math.max(1, tuning.integer(s("COMBO_COUNT"), 3))) {
                state.comboHits.clear();
                if (now >= state.flareReadyAt) {
                    state.flareReadyAt = now + tuning.integer(s("FLARE_LOCKOUT_TICKS"), 40);
                    flare(world, stack, attacker, target.getPos(), tuning);
                    attacker.heal((float) tuning.get(s("HEAL_AMOUNT"), 1));
                    UniqueAbilityApi.start(execution);
                    UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.PULSE,
                            target, 1, tuning.get(s("FLARE_DAMAGE_MULTIPLIER"), .7));
                }
            }
        } else if (proc) {
            world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), .3F, 1.7F);
            grantSunfireRegeneration(attacker, tuning.integer(s("STATUS_DURATION_TICKS"), 40), 1);
            if (tuning.flag(4096) && attacker.getHealth() >= attacker.getMaxHealth()) {
                state.reserve = Math.min(tuning.integer(s("RESERVE_CAP"), 4), state.reserve + 1);
            }
            if (tuning.flag(2048)) attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,
                    tuning.integer(s("FIRE_RESISTANCE_TICKS"), 80), 0), attacker);
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.HIT,
                    target, 1, chance);
        }
        if (execution.isStarted()) UniqueAbilityApi.finish(execution, LongPathFinalFormsMasteryAbilities.FINISH, 1);
        else UniqueAbilityApi.cancel(execution);
        }
    }

    public static void harbingerMelee(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.getWorld() instanceof ServerWorld world)
                || !HelperMethods.checkAbilityTarget(target, attacker)) return;
        UniqueAbilityExecution execution = begin(LongPathFinalFormsMasteryAbilities.HARBINGER_OMEN, world, stack, attacker, target);
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        PassiveState state = state(attacker, world.getTime());
        int chance = tuning.integer(s("CHANCE"), Config.uniqueEffects.harbinger.chance);
        boolean exact = tuning.has(s("CHANCE"));
        int roll = -1;
        boolean proc;
        if (chance >= 100) proc = true;
        else if (chance <= 0) proc = false;
        else {
            roll = attacker.getRandom().nextInt(100);
            proc = exact ? roll < chance : roll <= chance;
        }
        UniqueAbilityApi.reportRoll(attacker, LongPathFinalFormsMasteryAbilities.HARBINGER_OMEN.id(),
                "CHANCE", chance, roll, proc);
        if (proc && target.isAlive() && target.canHaveStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS))) {
            world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), .3F, 1.6F);
            int amplifier = 0;
            if (tuning.flag(4096)) {
                int count = state.omenCounts.merge(target.getUuid(), 1, Integer::sum);
                if (count % Math.max(1, tuning.integer(s("OMEN_UPGRADE_COUNT"), 3)) == 0) amplifier = 1;
            }
            int duration = tuning.flag(65536) ? tuning.integer(s("PLAGUE_WEAKNESS_TICKS"), 120)
                    : amplifier == 1 ? tuning.integer(s("OMEN_UPGRADE_TICKS"), 80)
                    : tuning.integer(s("STATUS_DURATION_TICKS"), 160);
            HarbingerMasteryState.applyWeakness(attacker, target, duration, amplifier, tuning.flag(131072), true);
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, LongPathFinalFormsMasteryAbilities.HIT,
                    target, 1, chance);
        }
        if (tuning.flag(8192) && target.hasStatusEffect(StatusEffects.WEAKNESS)) {
            Vec3d standard = BattleStandardMasteryManager.standardPosition(attacker, target.getPos());
            double range = tuning.get(s("DOOM_PULL_RANGE"), 10);
            if (standard != null && target.getPos().squaredDistanceTo(standard) <= range * range
                    && state.pullLocks.getOrDefault(target.getUuid(), 0L) <= world.getTime()) {
                state.pullLocks.put(target.getUuid(), world.getTime()
                        + tuning.integer(s("DOOM_PULL_LOCKOUT_TICKS"), 20));
                state.pullLocks.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
                pull(target, standard, tuning.get(s("PULL_STRENGTH"), .75));
            }
        }
        if (execution.isStarted()) UniqueAbilityApi.finish(execution, LongPathFinalFormsMasteryAbilities.FINISH, 1);
        else UniqueAbilityApi.cancel(execution);
    }

    public static void tickHeld(ItemStack stack, LivingEntity owner) {
        if (!(owner.getWorld() instanceof ServerWorld world)) return;
        MasteryAbsorptionTracker.tick(owner);
        stack = heldSunfire(owner);
        if (stack.isEmpty() || !net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) return;
        PassiveState state = sunfireState(owner);
        if (state.lastHeldTick == world.getTime()) return;
        state.lastHeldTick = world.getTime();
        if (owner.age % HELD_RESOLVE_INTERVAL != 0) return;
        UniqueAbilityExecution execution = begin(LongPathFinalFormsMasteryAbilities.SUNFIRE_REGEN, world, stack, owner, null);
        LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
        state.sunfireTuning = tuning;
        if (!tuning.flag(4096) || tuning.flag(131072)) state.reserve = 0;
        if (!tuning.flag(131072)) state.comboHits.clear();
        if (tuning.flag(4096) && !tuning.flag(131072) && state.reserve > 0
                && owner.getHealth() / owner.getMaxHealth() < tuning.get(s("RESERVE_THRESHOLD"), .5)) {
            float before = owner.getAbsorptionAmount();
            MasteryAbsorptionTracker.grant(owner, "sunfire/reserve", state.reserve,
                    tuning.integer(s("RESERVE_ABSORPTION_TICKS"), 100), state.reserve);
            if (owner.getAbsorptionAmount() > before) {
                state.reserve = 0;
                UniqueAbilityApi.start(execution);
            }
        }
        if (tuning.flag(32768)
                && owner.getHealth() / owner.getMaxHealth() < tuning.get(s("REKINDLE_THRESHOLD"), .3)
                && world.getTime() >= state.rekindleReadyAt) {
            state.rekindleReadyAt = world.getTime() + tuning.integer(s("REKINDLE_LOCKOUT_TICKS"), 600);
            int duration = tuning.integer(s("REKINDLE_DURATION_TICKS"), 100);
            grantSunfireRegeneration(owner, duration, 1);
            MasteryAbsorptionTracker.grant(owner, "sunfire/rekindle", (float) tuning.get(s("REKINDLE_ABSORPTION"), 4), duration,
                    (float) tuning.get(s("REKINDLE_ABSORPTION"), 4));
            UniqueAbilityApi.start(execution);
        }
        if (execution.isStarted()) UniqueAbilityApi.finish(execution, LongPathFinalFormsMasteryAbilities.FINISH, 1);
        else UniqueAbilityApi.cancel(execution);
    }

    public static float modifyOutgoingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return amount;
        ItemStack weapon = source.getWeaponStack();
        if (weapon != null && !weapon.isEmpty() && isHarbinger(weapon) && melee(source)
                && net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(weapon)
                && target.hasStatusEffect(StatusEffects.WEAKNESS)
                && attacker.getWorld() instanceof ServerWorld world) {
            UniqueAbilityExecution execution = begin(LongPathFinalFormsMasteryAbilities.HARBINGER_OMEN,
                    world, weapon, attacker, target);
            LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
            double multiplier = tuning.get(s("MELEE_DAMAGE_MULTIPLIER"), 1);
            if (tuning.flag(65536)) multiplier *= tuning.get(s("PLAGUE_DAMAGE_MULTIPLIER"), .8);
            if (tuning.flag(131072) && isExecutionOmen(attacker, target)) {
                multiplier *= tuning.get(s("EXECUTION_DAMAGE_MULTIPLIER"), 1.4);
            }
            amount *= (float) multiplier;
            UniqueAbilityApi.cancel(execution);
        }
        if (melee(source) && HelperMethods.checkAbilityTarget(target, attacker)) {
            amount *= 1 + HarbingerMasteryState.damageBonus(attacker);
        }
        return amount;
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;
        if (target.getWorld() instanceof ServerWorld world) {
            if (directAttack(source)) {
                Map<UUID, AllyCharge> charges = SUNFIRE_CHARGES.get(world);
                AllyCharge sunfireCharge = charges == null ? null : charges.get(attacker.getUuid());
                if (sunfireCharge != null) {
                    if (sunfireCharge.expiresAt <= world.getTime()) charges.remove(attacker.getUuid());
                    else if (HelperMethods.checkAbilityTarget(target, attacker)) {
                        charges.remove(attacker.getUuid());
                        target.setOnFireFor(Math.max(1, sunfireCharge.fireTicks / 20));
                    }
                }
            }
            PassiveState defender = SUNFIRE_STATES.getOrDefault(world, Map.of()).get(target.getUuid());
            ItemStack held = heldSunfire(target);
            if (defender != null && world.getTime() < defender.regenUntil
                    && target.hasStatusEffect(StatusEffects.REGENERATION) && source.getSource() == attacker
                    && directAttack(source) && !source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)
                    && !held.isEmpty() && net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(held)
                    && HelperMethods.checkAbilityTarget(attacker, target)) {
                UniqueAbilityExecution execution = begin(LongPathFinalFormsMasteryAbilities.SUNFIRE_REGEN, world, held, target, attacker);
                LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
                if (tuning.flag(8192)) attacker.setOnFireFor(Math.max(1, tuning.integer(s("REPRISAL_FIRE_TICKS"), 40) / 20));
                UniqueAbilityApi.cancel(execution);
            }
        }
        HarbingerMasteryState.onDamageApplied(target, source);
    }

    public static boolean isExecutionOmen(LivingEntity owner, LivingEntity target) {
        return HarbingerMasteryState.isMarked(owner, target);
    }

    public static void onHarbingerDeath(LivingEntity target, DamageSource source) {
        if (!HARBINGER_DEATHS.add(target)) return;
        if (source.getAttacker() instanceof LivingEntity owner && target.hasStatusEffect(StatusEffects.WEAKNESS)) {
            ItemStack stack = BattleStandardMasteryManager.harbingerStack(owner);
            if (!stack.isEmpty() && owner.getWorld() instanceof ServerWorld world) {
                UniqueAbilityExecution execution = begin(LongPathFinalFormsMasteryAbilities.HARBINGER_OMEN,
                        world, stack, owner, target);
                LongPathFinalFormsMasteryTuning tuning = LongPathFinalFormsMasteryAbilities.tuning(execution);
                if (tuning.flag(16384)) BattleStandardMasteryManager.refundProphecy(owner,
                        tuning.integer(s("REFUND_TICKS"), 20), tuning.integer(s("PROPHECY_REFUND_CAP_TICKS"), 100));
                UniqueAbilityApi.cancel(execution);
            }
        }
        STATES.remove(target.getUuid());
        HarbingerMasteryState.clearOwner(target);
    }

    public static void supportSunfireAlly(LivingEntity ally, int duration, int fireTicks, long tick) {
        if (!(ally.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, AllyCharge> charges = SUNFIRE_CHARGES.computeIfAbsent(world, ignored -> new HashMap<>());
        charges.entrySet().removeIf(entry -> entry.getValue().expiresAt <= tick || world.getEntity(entry.getKey()) == null);
        charges.put(ally.getUuid(), new AllyCharge(tick + duration, fireTicks));
    }

    private static UniqueAbilityExecution begin(UniqueAbilityDefinition definition, ServerWorld world,
                                                ItemStack stack, LivingEntity actor, LivingEntity target) {
        return UniqueAbilityApi.preparePassive(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null), tuning -> {
                    tuning.set(LongPathFinalFormsMasteryAbilities.TUNING, LongPathFinalFormsMasteryTuning.EMPTY);
                    if (definition.cooldownKey().isPresent()) tuning.set(LongPathFinalFormsMasteryAbilities.COOLDOWN_TICKS,
                            definition == LongPathFinalFormsMasteryAbilities.SUNFIRE_STANDARD
                                    ? Config.uniqueEffects.sunfire.cooldown : Config.uniqueEffects.harbinger.cooldown);
                });
    }

    private static void flare(ServerWorld world, ItemStack stack, LivingEntity owner,
                              Vec3d center, LongPathFinalFormsMasteryTuning tuning) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        double radius = tuning.get(s("RADIUS"), 3);
        int cap = tuning.integer(s("TARGET_CAP"), 8);
        Box box = new Box(center.x + radius, center.y + radius, center.z + radius,
                center.x - radius, center.y - radius, center.z - radius);
        float base = HelperMethods.abilityScaledDamage(net.sweenus.simplyswords.compat.SpellScalingComponents.component("sunfire", "damage"), owner, stack,
                Config.uniqueEffects.sunfire.damageScaling, Config.uniqueEffects.sunfire.spellScaling)
                * (float) tuning.get(s("FLARE_DAMAGE_MULTIPLIER"), .7);
        world.getOtherEntities(owner, box).stream().filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast).filter(target -> target.isAlive() && HelperMethods.checkAbilityTarget(target, owner))
                .filter(target -> target.getPos().squaredDistanceTo(center) <= radius * radius)
                .sorted(Comparator.comparingDouble((LivingEntity target) -> target.getPos().squaredDistanceTo(center))
                        .thenComparing(target -> target.getUuid().toString()))
                .limit(Math.clamp(cap, 0, 64)).forEach(target -> {
                    DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
                    HelperMethods.damageThroughIframes(target, source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, base));
                });
        }
    }

    private static void pull(LivingEntity target, Vec3d center, double strength) {
        Vec3d delta = center.subtract(target.getPos());
        if (delta.lengthSquared() < .001) return;
        Vec3d velocity = delta.normalize().multiply(strength);
        target.setVelocity(velocity.x, Math.clamp(velocity.y, -.3, .3), velocity.z);
        target.velocityModified = true;
    }

    private static PassiveState state(LivingEntity owner, long tick) {
        STATES.entrySet().removeIf(entry -> entry.getValue().sunfireOwner == null
                || !entry.getValue().sunfireOwner.isAlive() || entry.getValue().sunfireOwner.isRemoved()
                || entry.getValue().harbingerWorld != entry.getValue().sunfireOwner.getWorld());
        PassiveState state = STATES.computeIfAbsent(owner.getUuid(), ignored -> new PassiveState());
        state.expiresAt = tick + 2400;
        state.sunfireOwner = owner;
        state.harbingerWorld = owner.getWorld();
        return state;
    }

    public static void tickReserveHud(ServerWorld world) {
        HarbingerMasteryState.tick(world);
        STATES.values().stream().filter(state -> state.harbingerWorld == world).forEach(state ->
                state.omenCounts.keySet().removeIf(uuid -> !(world.getEntity(uuid) instanceof LivingEntity target) || !target.isAlive()));
        STATES.entrySet().removeIf(entry -> entry.getValue().sunfireOwner == null
                || !entry.getValue().sunfireOwner.isAlive() || entry.getValue().sunfireOwner.isRemoved()
                || entry.getValue().harbingerWorld != entry.getValue().sunfireOwner.getWorld());
        Map<UUID, PassiveState> states = SUNFIRE_STATES.get(world);
        if (states != null) {
            states.entrySet().removeIf(entry -> entry.getValue().expiresAt <= world.getTime()
                    || !(world.getEntity(entry.getKey()) instanceof LivingEntity living)
                    || !living.isAlive() || living.getWorld() != world
                    || living != entry.getValue().sunfireOwner);
        }
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            PassiveState state = states == null ? null : states.get(player.getUuid());
            ItemStack held = heldSunfire(player);
            int charges = 0;
            int capacity = 4;
            if (player.isAlive() && state != null && state.sunfireTuning != null && !held.isEmpty()
                    && net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(held)
                    && state.sunfireTuning.flag(4096) && !state.sunfireTuning.flag(131072)) {
                capacity = state.sunfireTuning.integer(s("RESERVE_CAP"), 4);
                charges = Math.clamp(state.reserve, 0, capacity);
            }
            ReserveHudState snapshot = new ReserveHudState(world, player, charges, capacity);
            if (!snapshot.equals(RESERVE_HUD.put(player.getUuid(), snapshot))) {
                dev.architectury.networking.NetworkManager.sendToPlayer(player,
                        new net.sweenus.simplyswords.network.EmberReservePacket(
                                world.getRegistryKey().getValue(), charges, capacity));
            }
        }
    }

    public static void clearSunfireOwner(LivingEntity owner) {
        STATES.remove(owner.getUuid());
        HarbingerMasteryState.clearOwner(owner);
        SUNFIRE_STATES.values().forEach(states -> states.remove(owner.getUuid()));
        RESERVE_HUD.remove(owner.getUuid());
    }

    private record ReserveHudState(ServerWorld world, LivingEntity owner, int charges, int capacity) {
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        HarbingerMasteryState.clear(world);
        RESERVE_HUD.entrySet().removeIf(entry -> entry.getValue().world == world);
        SUNFIRE_STATES.remove(world);
        SUNFIRE_CHARGES.remove(world);
        STATES.keySet().removeIf(uuid -> world.getEntity(uuid) != null);
        HARBINGER_DEATHS.removeIf(entity -> entity.getWorld() == world);
    }

    public static void clearAll() {
        HarbingerMasteryState.clearAll();
        RESERVE_HUD.clear();
        STATES.clear();
        SUNFIRE_STATES.clear();
        SUNFIRE_CHARGES.clear();
        ATTACK_ACTION.remove();
        HARBINGER_DEATHS.clear();
    }

    private static LongPathFinalFormsMasteryTuning.Setting s(String name) {
        return LongPathFinalFormsMasteryTuning.Setting.valueOf(name);
    }

    private static final class PassiveState {
        private LivingEntity sunfireOwner;
        private net.minecraft.world.World harbingerWorld;
        private final Map<UUID, Integer> omenCounts = new HashMap<>();
        private long expiresAt;
        private long regenUntil;
        private long lastHeldTick = Long.MIN_VALUE;
        private final java.util.ArrayDeque<Long> comboHits = new java.util.ArrayDeque<>();
        private long comboDeadline;
        private long flareReadyAt;
        private long rekindleReadyAt;
        private final Map<UUID, Long> pullLocks = new HashMap<>();
        private int reserve;
        private int combo;
        private LongPathFinalFormsMasteryTuning sunfireTuning;
        private final Map<UUID, Long> attackerLocks = new HashMap<>();
    }

    private record AllyCharge(long expiresAt, int fireTicks) {
    }

}
