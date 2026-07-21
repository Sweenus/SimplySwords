package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.ParryComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StormbringerParryManager {

    private static final Map<UUID, ActiveParry> ACTIVE_PARRIES = new HashMap<>();

    private StormbringerParryManager() {
    }

    public static void activate(ServerPlayerEntity player, Hand hand) {
        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        int blockDuration = Math.max(1, Config.uniqueEffects.stormbringer.blockDuration);
        int parryDuration = Math.clamp(Config.uniqueEffects.stormbringer.parryDuration, 1, blockDuration);
        ACTIVE_PARRIES.put(player.getUuid(), new ActiveParry(hand, now + blockDuration, now + parryDuration));
        spawnActivationEffects(world, player);
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ActiveParry active = ACTIVE_PARRIES.get(player.getUuid());
        if (active == null) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        if (!player.isAlive() || world.getTime() > active.expiresAt || !isStillUsingStormbringer(player, active.hand)) {
            ACTIVE_PARRIES.remove(player.getUuid());
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
        if (world.getTime() > active.expiresAt || !isStillUsingStormbringer(player, active.hand)) {
            ACTIVE_PARRIES.remove(player.getUuid());
            return false;
        }

        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        if (world.getTime() <= active.parryExpiresAt && isValidParryAttacker(player, attacker)) {
            ItemStack stack = player.getStackInHand(active.hand);
            ParryComponent parryComponent = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT)
                    .gainStormCharges(Config.uniqueEffects.stormbringer.stormChargesPerParry, Config.uniqueEffects.stormbringer.maxStormCharges);
            stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent);
            spawnParryCatchEffects(world, player, attacker, parryComponent);
            player.stopUsingItem();
        } else {
            ItemStack stack = player.getStackInHand(active.hand);
            ParryComponent parryComponent = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT)
                    .gainBlockedStormCharges(Config.uniqueEffects.stormbringer.stormChargesPerBlock, Config.uniqueEffects.stormbringer.maxStormCharges);
            stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent);
            spawnBlockedHitEffects(world, player);
        }

        return true;
    }

    public static void finishUse(ServerPlayerEntity player, ItemStack stack) {
        ACTIVE_PARRIES.remove(player.getUuid());

        int skillCooldown = Math.max(0, Config.uniqueEffects.stormbringer.cooldown);
        ParryComponent parryComponent = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
        if (parryComponent.parried()) {
            performCounterattack(player);
            stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent.resetParry());
        } else {
            spawnMissEffects(player.getServerWorld(), player);
            stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent.resetFull());
        }

        player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
    }

    private static boolean isStillUsingStormbringer(ServerPlayerEntity player, Hand hand) {
        return player.isUsingItem()
                && player.getActiveHand() == hand
                && player.getStackInHand(hand).isOf(ItemsRegistry.STORMBRINGER.get());
    }

    private static boolean isValidParryAttacker(ServerPlayerEntity player, LivingEntity attacker) {
        if (attacker == null || attacker == player || !attacker.isAlive() || !HelperMethods.checkFriendlyFire(attacker, player)) {
            return false;
        }

        double radius = Math.max(0.5, Config.uniqueEffects.stormbringer.radius);
        return attacker.squaredDistanceTo(player) <= radius * radius;
    }

    private static void performCounterattack(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        double radius = Math.max(0.5, Config.uniqueEffects.stormbringer.radius);
        float abilityDamage = HelperMethods.spellScaledDamage("lightning", player,
                Config.uniqueEffects.stormbringer.spellScaling, Config.uniqueEffects.stormbringer.damage);
        Box box = player.getBoundingBox().expand(radius, radius, radius);

        for (Entity entity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity target) || !HelperMethods.checkFriendlyFire(target, player)) {
                continue;
            }

            target.damage(player.getDamageSources().indirectMagic(player, player), abilityDamage);
            Vec3d direction = target.getPos().subtract(player.getPos());
            if (direction.lengthSquared() > 0.0001) {
                direction = direction.normalize();
            }
            target.setVelocity(direction.x * 1.15, 0.35, direction.z * 1.15);
            target.velocityModified = true;
            spawnTargetHitEffects(world, target);
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

    private record ActiveParry(Hand hand, long expiresAt, long parryExpiresAt) {
    }
}
