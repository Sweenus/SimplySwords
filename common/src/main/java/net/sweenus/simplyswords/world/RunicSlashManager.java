package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.RunicSlashProjectileEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RunicSlashManager {

    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IGNORE_ATTACK_READY = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> LAST_ACTIVATION = new HashMap<>();

    private RunicSlashManager() {
    }

    public static boolean isSuppressed() {
        return SUPPRESSED.get();
    }

    public static void runSuppressed(Runnable runnable) {
        SUPPRESSED.set(true);
        try {
            runnable.run();
        } finally {
            SUPPRESSED.set(false);
        }
    }

    public static boolean isIgnoringAttackReady() {
        return IGNORE_ATTACK_READY.get();
    }

    public static void runIgnoringAttackReady(Runnable runnable) {
        boolean previous = IGNORE_ATTACK_READY.get();
        IGNORE_ATTACK_READY.set(true);
        try {
            runnable.run();
        } finally {
            IGNORE_ATTACK_READY.set(previous);
        }
    }

    public static void tryFire(ServerWorld world, LivingEntity user, ItemStack stack) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (world == null || user == null || stack == null || stack.isEmpty() || isSuppressed() || !user.isAlive()) {
            return;
        }

        if (!IGNORE_ATTACK_READY.get() && !isAttackReady(world, user, stack)) {
            return;
        }

        Vec3d direction = user.getRotationVec(1.0F);
        if (direction.lengthSquared() < 0.0001) {
            direction = Vec3d.fromPolar(user.getPitch(), user.getYaw());
        }
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        direction = direction.normalize();

        double speed = Math.max(0.05, Config.gemPowers.runicSlash.speed);
        double distance = Math.max(0.25, Config.gemPowers.runicSlash.distance);
        float damage = HelperMethods.gemPowerScaledDamage(SpellScalingComponents.power("runic_slash"), user, stack,
                Config.gemPowers.runicSlash.damageScaling,
                Config.gemPowers.runicSlash.spellScaling);
        Vec3d start = user.getEyePos().subtract(0.0, Math.max(0.15, user.getHeight() * 0.18), 0.0).add(direction.multiply(0.65));

        RunicSlashProjectileEntity slash = new RunicSlashProjectileEntity(world, user, stack.copy(), direction, distance, speed, damage, Config.gemPowers.runicSlash.width);
        slash.setPosition(start);
        slash.setYaw(user.getYaw());
        slash.setPitch(user.getPitch());
        world.spawnEntity(slash);

        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundRegistry.SWING_WOOSH.get(), SoundCategory.PLAYERS, 0.75F, 1.25F + world.random.nextFloat() * 0.12F);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundRegistry.ELEMENTAL_SWORD_WIND_ATTACK_03.get(), SoundCategory.PLAYERS, 0.45F, 1.45F + world.random.nextFloat() * 0.12F);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static boolean isAttackReady(ServerWorld world, LivingEntity user, ItemStack stack) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            purgeOldEntries(now);
        }
        Long nextEligible = LAST_ACTIVATION.get(user.getUuid());
        if (nextEligible != null && now < nextEligible) {
            return false;
        }
        LAST_ACTIVATION.put(user.getUuid(), now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                stack, user, getAttackReadyCooldownTicks(user)));
        return true;
    }

    private static int getAttackReadyCooldownTicks(LivingEntity user) {
        EntityAttributeInstance attackSpeedAttribute = user.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        double attackSpeed = attackSpeedAttribute != null ? attackSpeedAttribute.getValue() : 4.0;
        if (attackSpeed <= 0.0) {
            attackSpeed = 4.0;
        }
        return Math.max(Config.gemPowers.runicSlash.minimumSwingCooldownTicks, (int) Math.ceil(20.0 / attackSpeed));
    }

    private static void purgeOldEntries(long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = LAST_ACTIVATION.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
