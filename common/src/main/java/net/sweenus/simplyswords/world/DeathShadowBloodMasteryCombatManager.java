package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryTuning;
import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

public final class DeathShadowBloodMasteryCombatManager {
    private static final Map<UUID, ShieldLockout> DEBT_SHIELD_LOCKOUTS = new HashMap<>();
    private static final Map<UUID, PendingShield> PENDING_SHIELDS = new HashMap<>();
    private static final Map<ServerWorld, List<DelayedReturn>> RETURNS = new HashMap<>();
    private static final Map<ServerWorld, List<CooldownRefund>> REFUNDS = new HashMap<>();
    private static final Map<ServerWorld, List<DelayedFinish>> FINISHES = new HashMap<>();
    private static final Map<ServerWorld, List<ScheduledAction>> ACTIONS = new HashMap<>();
    private DeathShadowBloodMasteryCombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(DeathShadowBloodMasteryAbilities.TUNING, DeathShadowBloodMasteryTuning.EMPTY)
                .set(DeathShadowBloodMasteryAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(DeathShadowBloodMasteryAbilities.TUNING, DeathShadowBloodMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world) || amount <= 0
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        ItemStack stack = target.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.SOULSTEALER.get())) return amount;
        PENDING_SHIELDS.remove(target.getUuid());
        int debt = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
        UniqueAbilityExecution execution = beginPassive(DeathShadowBloodMasteryAbilities.SOULSTEALER_DEBT,
                world, stack, target, source.getAttacker() instanceof LivingEntity living ? living : null);
        DeathShadowBloodMasteryTuning tuning = DeathShadowBloodMasteryAbilities.tuning(execution);
        int maximum = StealSwordItem.synchronizeSoulDebtCapacity(stack, tuning);
        long now = world.getTime();
        ShieldLockout lockout = DEBT_SHIELD_LOCKOUTS.get(target.getUuid());
        if (tuning.flag(1 << 4) && debt >= maximum
                && (lockout == null || lockout.world != world || lockout.until <= now)) {
            PENDING_SHIELDS.put(target.getUuid(), new PendingShield(world, stack, now,
                    tuning.integer(DeathShadowBloodMasteryTuning.Setting.SOULSTEALER_SHIELD_LOCKOUT_TICKS, 40)));
            amount *= tuning.get(DeathShadowBloodMasteryTuning.Setting.SOULSTEALER_SHIELD_MULTIPLIER, .85);
        }
        DEBT_SHIELD_LOCKOUTS.entrySet().removeIf(entry -> entry.getValue().world == world
                && entry.getValue().until <= now);
        UniqueAbilityApi.finish(execution, DeathShadowBloodMasteryAbilities.FINISH, 0);
        return amount;
    }

    public static void onDamageApplied(LivingEntity target) {
        if (target == null || !(target.getWorld() instanceof ServerWorld world)) return;
        PendingShield pending = PENDING_SHIELDS.remove(target.getUuid());
        if (pending == null || pending.world != world || pending.at != world.getTime()) return;
        int debt = StealSwordItem.getSoulDebt(pending.stack);
        if (debt <= 0) return;
        StealSwordItem.setSoulDebt(pending.stack, debt - 1);
        DEBT_SHIELD_LOCKOUTS.put(target.getUuid(), new ShieldLockout(world,
                world.getTime() + pending.lockoutTicks));
    }

    public static void scheduleReturn(ServerWorld world, LivingEntity actor, Vec3d position,
                                      int delay, int resistanceTicks, int resistanceAmplifier) {
        RETURNS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new DelayedReturn(
                actor.getUuid(), position, world.getTime() + Math.max(1, delay), resistanceTicks,
                resistanceAmplifier));
    }

    public static void scheduleAction(ServerWorld world, LivingEntity actor, int delay,
                                      Runnable action, Runnable cancellation) {
        ACTIONS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ScheduledAction(
                actor.getUuid(), world.getTime() + Math.max(1, delay), action, cancellation));
    }

    public static void scheduleCooldownRefund(ServerWorld world, LivingEntity actor, ItemStack stack,
                                              int refundTicks) {
        if (refundTicks <= 0) return;
        int base = stack.isOf(ItemsRegistry.SOULSTEALER.get())
                ? net.sweenus.simplyswords.config.Config.uniqueEffects.soulstealer.cooldown
                : stack.isOf(ItemsRegistry.SHADOWSTING.get())
                ? net.sweenus.simplyswords.config.Config.uniqueEffects.shadowsting.cooldown : 20;
        int total = net.sweenus.simplyswords.api.SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                stack, actor, base);
        REFUNDS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new CooldownRefund(
                actor.getUuid(), stack.getItem(), world.getTime() + 1, refundTicks, total));
    }

    public static void scheduleFinish(ServerWorld world, UniqueAbilityExecution execution, int affectedTargets) {
        scheduleFinish(world, execution, 1, affectedTargets);
    }

    public static void scheduleFinish(ServerWorld world, UniqueAbilityExecution execution, int delay,
                                      int affectedTargets) {
        FINISHES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new DelayedFinish(
                execution, world.getTime() + Math.max(1, delay), affectedTargets));
    }

    public static boolean hasScheduled(ServerWorld world) {
        return !RETURNS.getOrDefault(world, List.of()).isEmpty()
                || !REFUNDS.getOrDefault(world, List.of()).isEmpty()
                || !FINISHES.getOrDefault(world, List.of()).isEmpty()
                || !ACTIONS.getOrDefault(world, List.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        List<DelayedReturn> returns = RETURNS.get(world);
        if (returns != null) {
            returns.removeIf(task -> {
                if (world.getTime() < task.at) return false;
                if (world.getEntity(task.actorId) instanceof LivingEntity actor && actor.isAlive()) {
                    if (actor instanceof ServerPlayerEntity player) {
                        player.networkHandler.requestTeleport(task.position.x, task.position.y, task.position.z,
                                player.getYaw(), player.getPitch());
                    } else actor.refreshPositionAndAngles(task.position.x, task.position.y, task.position.z,
                            actor.getYaw(), actor.getPitch());
                    actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                            task.resistanceTicks, task.resistanceAmplifier), actor);
                }
                return true;
            });
            if (returns.isEmpty()) RETURNS.remove(world);
        }
        List<CooldownRefund> refunds = REFUNDS.get(world);
        if (refunds != null) {
            refunds.removeIf(task -> {
                if (world.getTime() < task.at) return false;
                if (world.getEntity(task.actorId) instanceof ServerPlayerEntity player) {
                    int remaining = Math.round(player.getItemCooldownManager().getCooldownProgress(
                            task.item, 0) * task.total);
                    player.getItemCooldownManager().set(task.item, Math.max(0, remaining - task.amount));
                }
                return true;
            });
            if (refunds.isEmpty()) REFUNDS.remove(world);
        }
        List<DelayedFinish> finishes = FINISHES.get(world);
        if (finishes != null) {
            finishes.removeIf(task -> {
                if (world.getTime() < task.at) return false;
                UniqueAbilityApi.finish(task.execution, DeathShadowBloodMasteryAbilities.FINISH, task.affectedTargets);
                return true;
            });
            if (finishes.isEmpty()) FINISHES.remove(world);
        }
        List<ScheduledAction> actions = ACTIONS.get(world);
        if (actions != null) {
            actions.removeIf(task -> {
                if (world.getTime() < task.at) return false;
                task.action.run();
                return true;
            });
            if (actions.isEmpty()) ACTIONS.remove(world);
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        RETURNS.remove(world);
        REFUNDS.remove(world);
        DEBT_SHIELD_LOCKOUTS.entrySet().removeIf(entry -> entry.getValue().world == world);
        PENDING_SHIELDS.entrySet().removeIf(entry -> entry.getValue().world == world);
        List<DelayedFinish> finishes = FINISHES.remove(world);
        if (finishes != null) finishes.forEach(task ->
                net.sweenus.simplyswords.api.ability.UniqueAbilityApi.cancel(task.execution));
        List<ScheduledAction> actions = ACTIONS.remove(world);
        if (actions != null) actions.forEach(task -> task.cancellation.run());
        StealSwordItem.clearWorld(world);
        DeathKnellAbilityManager.clear(world);
        SoulkeeperLanternManager.clear(world);
        TwistedBladeAbilityManager.clear(world);
        ShadowstingShadowDanceManager.clear(world);
        BloodwakeAbilityManager.clear(world);
        BloodStainManager.clear(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID actorId = actor.getUuid();
        DEBT_SHIELD_LOCKOUTS.remove(actorId);
        PENDING_SHIELDS.remove(actorId);
        RETURNS.values().forEach(tasks -> tasks.removeIf(task -> task.actorId.equals(actorId)));
        REFUNDS.values().forEach(tasks -> tasks.removeIf(task -> task.actorId.equals(actorId)));
        FINISHES.values().forEach(tasks -> tasks.removeIf(task -> {
            if (!task.execution.context().actor().getUuid().equals(actorId)) return false;
            net.sweenus.simplyswords.api.ability.UniqueAbilityApi.cancel(task.execution);
            return true;
        }));
        ACTIONS.values().forEach(tasks -> tasks.removeIf(task -> {
            if (!task.actorId.equals(actorId)) return false;
            task.cancellation.run();
            return true;
        }));
        RETURNS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        REFUNDS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        FINISHES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        ACTIONS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        StealSwordItem.clearActor(actor);
        DeathKnellAbilityManager.clearActor(actor);
        SoulkeeperLanternManager.clearActor(actor);
        TwistedBladeAbilityManager.clearActor(actor);
        ShadowstingShadowDanceManager.clearActor(actor);
        BloodwakeAbilityManager.clearActor(actor);
        BloodStainManager.clearActor(actor);
    }

    public static void clearAll() {
        FINISHES.values().forEach(tasks -> tasks.forEach(task ->
                net.sweenus.simplyswords.api.ability.UniqueAbilityApi.cancel(task.execution)));
        DEBT_SHIELD_LOCKOUTS.clear();
        PENDING_SHIELDS.clear();
        RETURNS.clear();
        REFUNDS.clear();
        FINISHES.clear();
        ACTIONS.values().forEach(tasks -> tasks.forEach(task -> task.cancellation.run()));
        ACTIONS.clear();
        StealSwordItem.clearAllState();
        DeathKnellAbilityManager.clearAll();
        SoulkeeperLanternManager.clearAll();
        TwistedBladeAbilityManager.clearAll();
        ShadowstingShadowDanceManager.clearAll();
        BloodwakeAbilityManager.clearAll();
        BloodStainManager.clearAll();
    }

    private record PendingShield(ServerWorld world, ItemStack stack, long at, int lockoutTicks) {
    }

    private record ShieldLockout(ServerWorld world, long until) {
    }

    private record DelayedReturn(UUID actorId, Vec3d position, long at, int resistanceTicks,
                                 int resistanceAmplifier) {
    }

    private record CooldownRefund(UUID actorId, net.minecraft.item.Item item, long at, int amount, int total) {
    }

    private record DelayedFinish(UniqueAbilityExecution execution, long at, int affectedTargets) {
    }

    private record ScheduledAction(UUID actorId, long at, Runnable action, Runnable cancellation) {
    }
}
