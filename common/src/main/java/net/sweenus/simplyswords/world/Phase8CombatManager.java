package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.damage.DamageSource;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
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
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

public final class Phase8CombatManager {
    private static final Map<UUID, Long> DEBT_SHIELD_LOCKOUTS = new HashMap<>();
    private static final Map<ServerWorld, List<DelayedReturn>> RETURNS = new HashMap<>();
    private static final Map<ServerWorld, List<CooldownRefund>> REFUNDS = new HashMap<>();
    private static final Map<ServerWorld, List<DelayedFinish>> FINISHES = new HashMap<>();
    private Phase8CombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(Phase8UniqueAbilities.TUNING, Phase8AbilityTuning.EMPTY)
                .set(Phase8UniqueAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(Phase8UniqueAbilities.TUNING, Phase8AbilityTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)) return amount;
        ItemStack stack = target.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.SOULSTEALER.get())) return amount;
        int debt = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
        UniqueAbilityExecution execution = beginPassive(Phase8UniqueAbilities.SOULSTEALER_DEBT,
                world, stack, target, source.getAttacker() instanceof LivingEntity living ? living : null);
        Phase8AbilityTuning tuning = Phase8UniqueAbilities.tuning(execution);
        int maximum = tuning.integer(Phase8AbilityTuning.Setting.STACK_CAP,
                net.sweenus.simplyswords.config.Config.uniqueEffects.soulstealer.maxStacks);
        long now = world.getTime();
        if (tuning.flag(1 << 4) && debt >= maximum
                && DEBT_SHIELD_LOCKOUTS.getOrDefault(target.getUuid(), 0L) <= now) {
            stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), new StoredChargeComponent(Math.max(0, debt - 1)));
            DEBT_SHIELD_LOCKOUTS.put(target.getUuid(), now
                    + tuning.integer(Phase8AbilityTuning.Setting.LOCKOUT_TICKS, 40));
            amount *= tuning.get(Phase8AbilityTuning.Setting.INCOMING_MULTIPLIER, .85);
        }
        DEBT_SHIELD_LOCKOUTS.entrySet().removeIf(entry -> entry.getValue() <= now);
        UniqueAbilityApi.finish(execution, Phase8UniqueAbilities.FINISH, 0);
        return amount;
    }

    public static void scheduleReturn(ServerWorld world, LivingEntity actor, Vec3d position,
                                      int delay, int resistanceTicks) {
        RETURNS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new DelayedReturn(
                actor.getUuid(), position, world.getTime() + Math.max(1, delay), resistanceTicks));
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
                || !FINISHES.getOrDefault(world, List.of()).isEmpty();
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
                            task.resistanceTicks, 1), actor);
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
                UniqueAbilityApi.finish(task.execution, Phase8UniqueAbilities.FINISH, task.affectedTargets);
                return true;
            });
            if (finishes.isEmpty()) FINISHES.remove(world);
        }
    }

    private record DelayedReturn(UUID actorId, Vec3d position, long at, int resistanceTicks) {
    }

    private record CooldownRefund(UUID actorId, net.minecraft.item.Item item, long at, int amount, int total) {
    }

    private record DelayedFinish(UniqueAbilityExecution execution, long at, int affectedTargets) {
    }
}
