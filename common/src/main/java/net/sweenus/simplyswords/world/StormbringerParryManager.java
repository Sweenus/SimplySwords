package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.item.component.ParryComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StormbringerParryManager {

    private static final Map<UUID, ActiveParry> ACTIVE_PARRIES = new HashMap<>();

    private StormbringerParryManager() {
    }

    public static void activate(ServerPlayerEntity player, Hand hand) {
        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        ItemStack stack = player.getStackInHand(hand);
        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, player, player, null, hand,
                WeaponAbilityActivationSource.PLAYER);
        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginActive(
                StormFrostWaterMasteryAbilities.STORMBRINGER_GUARD, context, Config.uniqueEffects.stormbringer.cooldown);
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
        StormbringerAbilityManager.observeTuning(player, tuning);
        int blockDuration = integer(tuning, s("STORMBRINGER_BLOCK_DURATION_TICKS"),
                s("DURATION_TICKS"), Math.max(1, Config.uniqueEffects.stormbringer.blockDuration));
        int parryDuration = Math.clamp(integer(tuning, s("STORMBRINGER_PARRY_WINDOW_TICKS"),
                s("INTERVAL_TICKS"), Config.uniqueEffects.stormbringer.parryDuration), 1, blockDuration);
        ActiveParry replaced = ACTIVE_PARRIES.put(player.getUuid(), new ActiveParry(hand, stack,
                world.getRegistryKey(), now + blockDuration, now + parryDuration, tuning, execution));
        if (replaced != null) UniqueAbilityApi.cancel(replaced.execution);
        spawnActivationEffects(world, player);
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ActiveParry active = ACTIVE_PARRIES.get(player.getUuid());
        if (active == null) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        if (!player.isAlive() || !world.getRegistryKey().equals(active.world)
                || player.getStackInHand(active.hand) != active.stack
                || !isStillUsingStormbringer(player, active.hand)) {
            ACTIVE_PARRIES.remove(player.getUuid());
            UniqueAbilityApi.cancel(active.execution);
            return;
        }
        if (world.getTime() > active.expiresAt) {
            player.stopUsingItem();
            return;
        }

        if (world.getTime() % 3L == 0L) {
            spawnGuardEffects(world, player, world.getTime() <= active.parryExpiresAt);
        }
    }

    public static boolean handleIncomingDamage(ServerPlayerEntity player, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        ActiveParry active = ACTIVE_PARRIES.get(player.getUuid());
        if (active == null) {
            return false;
        }

        ServerWorld world = player.getServerWorld();
        if (world.getTime() > active.expiresAt || !world.getRegistryKey().equals(active.world)
                || player.getStackInHand(active.hand) != active.stack
                || !isStillUsingStormbringer(player, active.hand)) {
            ACTIVE_PARRIES.remove(player.getUuid());
            UniqueAbilityApi.cancel(active.execution);
            return false;
        }

        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        if (world.getTime() <= active.parryExpiresAt && isValidParryAttacker(player, attacker)) {
            ItemStack stack = player.getStackInHand(active.hand);
            ParryComponent parryComponent = StormbringerAbilityManager.gainStormCharges(player, stack,
                    active.tuning, integer(active.tuning, s("STORMBRINGER_PARRY_CHARGE_GAIN"),
                            s("PULSE_COUNT"), Config.uniqueEffects.stormbringer.stormChargesPerParry), true);
            applyWard(player, active.tuning);
            spawnParryCatchEffects(world, player, attacker, parryComponent);
            player.stopUsingItem();
        } else {
            ItemStack stack = player.getStackInHand(active.hand);
            ParryComponent parryComponent = StormbringerAbilityManager.gainStormCharges(player, stack,
                    active.tuning, integer(active.tuning, s("STORMBRINGER_NORMAL_CHARGE_GAIN"),
                            s("COUNT"), Config.uniqueEffects.stormbringer.stormChargesPerBlock), false);
            applyWard(player, active.tuning);
            spawnBlockedHitEffects(world, player);
        }

        return true;
    }

    public static void finishUse(ServerPlayerEntity player, ItemStack stack) {
        ActiveParry active = ACTIVE_PARRIES.remove(player.getUuid());

        int skillCooldown = Math.max(0, Config.uniqueEffects.stormbringer.cooldown);
        ParryComponent parryComponent = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
        if (parryComponent.parried()) {
            performCounterattack(player, active == null ? StormFrostWaterMasteryTuning.EMPTY : active.tuning,
                    active == null ? null : active.execution);
            stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent.resetParry());
        } else {
            spawnMissEffects(player.getServerWorld(), player);
            stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent.resetFull());
        }

        SimplySwordsAPI.setWeaponCooldown(player, stack, skillCooldown);
        if (active != null && active.tuning.flag(1 << 6)) {
            SimplySwordsAPI.reduceWeaponCooldown(player, stack, skillCooldown,
                    integer(active.tuning, s("STORMBRINGER_ACTIVE_REFUND_TICKS"), s("REFUND_TICKS"), 30));
        }
        if (active != null) UniqueAbilityApi.finish(active.execution, StormFrostWaterMasteryAbilities.FINISH, 0);
    }

    public static int maxUseTime(LivingEntity user, ItemStack stack) {
        int base = Math.max(1, Config.uniqueEffects.stormbringer.blockDuration);
        if (user instanceof ServerPlayerEntity player) {
            ActiveParry active = ACTIVE_PARRIES.get(player.getUuid());
            if (active != null && active.stack == stack) {
                return Math.max(base, integer(active.tuning, s("STORMBRINGER_BLOCK_DURATION_TICKS"),
                        s("DURATION_TICKS"), base));
            }
        }
        return base + 20;
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        RegistryKey<World> key = world.getRegistryKey();
        List<ActiveParry> removed = new ArrayList<>();
        ACTIVE_PARRIES.entrySet().removeIf(entry -> {
            if (!entry.getValue().world.equals(key)) return false;
            removed.add(entry.getValue());
            return true;
        });
        removed.forEach(active -> UniqueAbilityApi.cancel(active.execution));
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        ActiveParry active = ACTIVE_PARRIES.remove(actor.getUuid());
        if (active != null) UniqueAbilityApi.cancel(active.execution);
    }

    public static void clearAll() {
        ACTIVE_PARRIES.values().forEach(active -> UniqueAbilityApi.cancel(active.execution));
        ACTIVE_PARRIES.clear();
    }

    private static boolean isStillUsingStormbringer(ServerPlayerEntity player, Hand hand) {
        return (player.isUsingItem()
                && player.getActiveHand() == hand
                && player.getStackInHand(hand).isOf(ItemsRegistry.STORMBRINGER.get()))
                || PlayerWeaponAbilityChannelManager.isChanneling(player, hand, ItemsRegistry.STORMBRINGER.get());
    }

    private static boolean isValidParryAttacker(ServerPlayerEntity player, LivingEntity attacker) {
        if (attacker == null || attacker == player || !attacker.isAlive() || !HelperMethods.checkFriendlyFire(attacker, player)) {
            return false;
        }

        double radius = Math.max(0.5, Config.uniqueEffects.stormbringer.radius);
        return attacker.squaredDistanceTo(player) <= radius * radius;
    }

    private static void performCounterattack(ServerPlayerEntity player, StormFrostWaterMasteryTuning tuning,
                                             UniqueAbilityExecution execution) {
        ServerWorld world = player.getServerWorld();
        double radius = value(tuning, s("STORMBRINGER_COUNTER_RADIUS"), s("RADIUS"),
                Math.max(0.5, Config.uniqueEffects.stormbringer.radius));
        ItemStack stack = execution == null ? player.getMainHandStack() : execution.context().stack();
        float abilityDamage = HelperMethods.abilityScaledDamage("lightning", player, stack,
                Config.uniqueEffects.stormbringer.damageScaling, Config.uniqueEffects.stormbringer.spellScaling);
        abilityDamage *= (float) value(tuning, s("STORMBRINGER_COUNTER_DAMAGE_MULTIPLIER"),
                s("DAMAGE_MULTIPLIER"), 1);
        Box box = player.getBoundingBox().expand(radius, radius, radius);
        int affected = 0;
        int cap = integer(tuning, s("STORMBRINGER_COUNTER_TARGET_CAP"), s("TARGET_CAP"),
                Config.uniqueEffects.stormbringer.counterTargetCap);
        List<Entity> targets = world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity target
                        && HelperMethods.checkFriendlyFire(target, player))
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)))
                .limit(Math.max(1, cap)).toList();
        for (Entity entity : targets) {
            LivingEntity target = (LivingEntity) entity;

            DamageSource damageSource = player.getDamageSources().indirectMagic(player, player);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, abilityDamage);
            if (!target.damage(damageSource, damage)) continue;
            affected++;
            Vec3d direction = target.getPos().subtract(player.getPos());
            if (direction.lengthSquared() > 0.0001) {
                direction = direction.normalize();
            }
            double knockback = value(tuning, s("STORMBRINGER_COUNTER_KNOCKBACK_MULTIPLIER"),
                    s("KNOCKBACK"), 1);
            target.setVelocity(direction.x * 1.15 * knockback, 0.35, direction.z * 1.15 * knockback);
            int fireTicks = integer(tuning, s("STORMBRINGER_COUNTER_FIRE_TICKS"), s("FIRE_TICKS"), 0);
            if (fireTicks > 0) target.setOnFireForTicks(fireTicks);
            target.velocityModified = true;
            spawnTargetHitEffects(world, target);
            if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                    StormFrostWaterMasteryAbilities.HIT, target, 1, damage);
            if (affected >= cap) break;
        }

        Vec3d recoil = player.getRotationVector().multiply(-0.75);
        player.setVelocity(recoil.x, 0.0, recoil.z);
        player.velocityModified = true;
        spawnCounterattackEffects(world, player, radius);
        world.playSound(null, player.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_01.get(),
                SoundCategory.PLAYERS, 0.8F, 0.85F);
    }

    private static void spawnActivationEffects(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, player.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 24, 0.45, 0.45, 0.45, 0.08);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 8, 0.3, 0.35, 0.3, 0.035);
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_PARRY_02.get(), SoundCategory.PLAYERS, 0.8F, 0.8F);
    }

    private static void spawnGuardEffects(ServerWorld world, ServerPlayerEntity player, boolean parryWindowActive) {
        Vec3d pos = player.getPos().add(0.0, player.getHeight() * 0.55, 0.0);
        int sparks = parryWindowActive ? 4 : 2;
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, sparks, 0.34, 0.42, 0.34, 0.025);
        if (parryWindowActive) {
            world.spawnParticles(ParticleTypes.WAX_OFF, pos.x, pos.y, pos.z, 1, 0.22, 0.3, 0.22, 0.015);
        }
    }

    private static void spawnBlockedHitEffects(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, player.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 12, 0.35, 0.38, 0.35, 0.06);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 8, 0.25, 0.28, 0.25, 0.025);
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_PARRY_03.get(), SoundCategory.PLAYERS, 0.55F, 1.2F);
    }

    private static void spawnParryCatchEffects(ServerWorld world, ServerPlayerEntity player, LivingEntity attacker, ParryComponent parryComponent) {
        Vec3d center = player.getPos().add(attacker.getPos()).multiply(0.5).add(0.0, player.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 30, 0.35, 0.35, 0.35, 0.11);
        world.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 18, 0.25, 0.25, 0.25, 0.08);
        float pitch = 0.85F + Math.min(0.45F, parryComponent.stormCharges() * 0.025F);
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_PARRY_01.get(), SoundCategory.PLAYERS, 1.0F, pitch);
    }

    private static void spawnCounterattackEffects(ServerWorld world, ServerPlayerEntity player, double radius) {
        Vec3d pos = player.getPos();
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.55, pos.z, 44, radius * 0.35, 0.35, radius * 0.35, 0.12);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.08, pos.z, 16, radius * 0.28, 0.08, radius * 0.28, 0.03);
    }

    private static void spawnTargetHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 8, 0.22, 0.24, 0.22, 0.08);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 8, 0.18, 0.18, 0.18, 0.03);
        world.playSound(null, target.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(), SoundCategory.HOSTILE, 0.25F, 1.1F);
    }

    private static void spawnMissEffects(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, player.getHeight() * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 5, 0.22, 0.18, 0.22, 0.01);
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }

    private static void applyWard(ServerPlayerEntity player, StormFrostWaterMasteryTuning tuning) {
        if (!tuning.flag(1 << 3)) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                integer(tuning, s("STORMBRINGER_BLOCK_RESISTANCE_TICKS"),
                        s("STATUS_DURATION_TICKS"), 30), 0), player);
    }

    private static double value(StormFrostWaterMasteryTuning tuning, StormFrostWaterMasteryTuning.Setting scoped,
                                StormFrostWaterMasteryTuning.Setting generic, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(generic, fallback);
    }

    private static int integer(StormFrostWaterMasteryTuning tuning, StormFrostWaterMasteryTuning.Setting scoped,
                               StormFrostWaterMasteryTuning.Setting generic, int fallback) {
        return (int) Math.round(value(tuning, scoped, generic, fallback));
    }

    private record ActiveParry(Hand hand, ItemStack stack, RegistryKey<World> world,
                               long expiresAt, long parryExpiresAt,
                               StormFrostWaterMasteryTuning tuning, UniqueAbilityExecution execution) {
    }
}
