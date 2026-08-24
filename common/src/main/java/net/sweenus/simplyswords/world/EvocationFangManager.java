package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EvocationFangManager {

    public static final String VISUAL_ONLY_TAG = "simplyswords_evocation_visual_only";

    private static final Map<ServerWorld, List<PendingStrike>> PENDING_STRIKES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> TARGET_COOLDOWNS = new HashMap<>();

    private EvocationFangManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<PendingStrike> strikes = PENDING_STRIKES.get(world);
        Map<UUID, Long> cooldowns = TARGET_COOLDOWNS.get(world);
        return (strikes != null && !strikes.isEmpty()) || (cooldowns != null && !cooldowns.isEmpty());
    }

    public static void tryStrikeNearby(LivingEntity owner, ItemStack stack) {
        if (owner == null
                || stack == null
                || stack.isEmpty()
                || !(owner.getWorld() instanceof ServerWorld world)
                || !owner.isAlive()) {
            return;
        }

        double range = Math.max(0.5, Config.gemPowers.evocation.range);
        Box searchBox = owner.getBoundingBox().expand(range, Math.max(2.0, range * 0.5), range);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, searchBox, target ->
                        isValidTarget(owner, target) && isTargetReady(world, target))
                .stream()
                .sorted(Comparator.comparingDouble(owner::squaredDistanceTo))
                .limit(Math.max(1, Config.gemPowers.evocation.maxTargetsPerScan))
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        float damage = HelperMethods.gemPowerScaledDamage(SpellScalingComponents.power("evocation"), owner, stack,
                Config.gemPowers.evocation.damageScaling,
                Config.gemPowers.evocation.spellScaling);
        if (damage <= 0.0F) {
            return;
        }

        long now = world.getTime();
        for (LivingEntity target : targets) {
            markTargetCooldown(world, target, now);
            spawnFangs(world, owner, target);
            scheduleStrike(world, owner, target, stack, damage, now);
        }

        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5F, 0.85F + world.random.nextFloat() * 0.12F);
    }

    public static void tick(ServerWorld world) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            purgeCooldowns(world, now);
        }

        List<PendingStrike> strikes = PENDING_STRIKES.get(world);
        if (strikes == null || strikes.isEmpty()) {
            PENDING_STRIKES.remove(world);
            return;
        }

        Iterator<PendingStrike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            PendingStrike strike = iterator.next();
            if (now < strike.executeTick()) {
                continue;
            }
            iterator.remove();
            executeStrike(world, strike);
        }

        if (strikes.isEmpty()) {
            PENDING_STRIKES.remove(world);
        }
    }

    private static boolean isValidTarget(LivingEntity owner, LivingEntity target) {
        return target != null
                && target != owner
                && target.isAlive()
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkFriendlyFire(target, owner);
    }

    private static boolean isTargetReady(ServerWorld world, LivingEntity target) {
        Map<UUID, Long> cooldowns = TARGET_COOLDOWNS.get(world);
        if (cooldowns == null) {
            return true;
        }
        Long lastStrike = cooldowns.get(target.getUuid());
        return lastStrike == null || world.getTime() - lastStrike >= Math.max(1, Config.gemPowers.evocation.targetCooldownTicks);
    }

    private static void markTargetCooldown(ServerWorld world, LivingEntity target, long now) {
        TARGET_COOLDOWNS.computeIfAbsent(world, ignored -> new HashMap<>()).put(target.getUuid(), now);
    }

    private static void spawnFangs(ServerWorld world, LivingEntity owner, LivingEntity target) {
        Vec3d pos = target.getPos();
        float yaw = (float) MathHelper.atan2(target.getZ() - owner.getZ(), target.getX() - owner.getX());
        EvokerFangsEntity fangs = new EvokerFangsEntity(world, pos.x, target.getY(), pos.z, yaw,
                Math.max(0, Config.gemPowers.evocation.fangWarmupTicks), owner);
        fangs.addCommandTag(VISUAL_ONLY_TAG);
        world.spawnEntity(fangs);

        world.spawnParticles(ParticleTypes.SOUL, pos.x, target.getBodyY(0.25), pos.z, 6, 0.35, 0.08, 0.35, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.35F, 0.75F + world.random.nextFloat() * 0.08F);
    }

    private static void scheduleStrike(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack, float damage, long now) {
        long executeTick = now + Math.max(0, Config.gemPowers.evocation.fangWarmupTicks)
                + Math.max(0, Config.gemPowers.evocation.damageDelayTicks);
        PENDING_STRIKES.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new PendingStrike(owner.getUuid(), target.getUuid(), stack.copy(), damage, executeTick));
    }

    private static void executeStrike(ServerWorld world, PendingStrike strike) {
        Entity ownerEntity = world.getEntity(strike.ownerId());
        Entity targetEntity = world.getEntity(strike.targetId());
        if (!(ownerEntity instanceof LivingEntity owner)
                || !(targetEntity instanceof LivingEntity target)
                || !owner.isAlive()
                || !isValidTarget(owner, target)) {
            return;
        }

        boolean damaged = SimplySwordsAPI.applyEntityWeaponHit(strike.stack(), target, owner, strike.damage());
        if (!damaged) {
            return;
        }

        int amplifier = MathHelper.clamp(Config.gemPowers.evocation.slownessAmplifier, 0, 1);
        int duration = AwakeningApi.scaleGemPowerDuration(
                strike.stack(), Math.max(1, Config.gemPowers.evocation.slownessDurationTicks));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amplifier, false, false, true), owner);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 0.8F, 0.9F + world.random.nextFloat() * 0.1F);
    }

    private static void purgeCooldowns(ServerWorld world, long now) {
        Map<UUID, Long> cooldowns = TARGET_COOLDOWNS.get(world);
        if (cooldowns == null) {
            return;
        }
        long expiry = Math.max(20L, Config.gemPowers.evocation.targetCooldownTicks * 4L);
        cooldowns.entrySet().removeIf(entry -> now - entry.getValue() > expiry);
        if (cooldowns.isEmpty()) {
            TARGET_COOLDOWNS.remove(world);
        }
    }

    private record PendingStrike(UUID ownerId, UUID targetId, ItemStack stack, float damage, long executeTick) {
    }
}
