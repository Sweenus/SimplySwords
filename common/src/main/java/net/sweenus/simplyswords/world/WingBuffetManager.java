package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DragonWingBuffetVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

public final class WingBuffetManager {

    private WingBuffetManager() {
    }

    public static boolean tryActivate(LivingEntity attacker, ItemStack stack) {
        if (attacker == null || stack == null || stack.isEmpty() || !attacker.isAlive()) {
            return false;
        }
        if (!(attacker.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (hasActiveBuffet(world, attacker)) {
            return false;
        }

        Vec3d direction = horizontalDirection(attacker);
        Vec3d center = attacker.getPos().add(0.0, attacker.getHeight() * 0.55, 0.0);
        double range = Math.max(0.5, Config.gemPowers.wingBuffet.range);
        double halfAngleCos = Math.cos(Math.toRadians(MathHelper.clamp(Config.gemPowers.wingBuffet.coneAngleDegrees, 1.0, 180.0) * 0.5));
        float damage = HelperMethods.gemPowerScaledDamage("evocation", attacker, stack,
                Config.gemPowers.wingBuffet.damageScaling,
                Config.gemPowers.wingBuffet.spellScaling);
        DamageSource damageSource = SimplySwordsAPI.getWeaponDamageSource(attacker);
        boolean hitAny = false;

        Box searchBox = Box.of(center.add(direction.multiply(range * 0.5)), range * 2.0, range, range * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (target == attacker || !target.isAlive() || !HelperMethods.checkFriendlyFire(target, attacker)) {
                continue;
            }

            Vec3d toTarget = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(center);
            Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);
            double distanceSquared = horizontalToTarget.lengthSquared();
            if (distanceSquared > range * range || distanceSquared < 1.0E-5) {
                continue;
            }

            Vec3d targetDirection = horizontalToTarget.normalize();
            if (targetDirection.dotProduct(direction) < halfAngleCos) {
                continue;
            }

            if (damageTarget(world, stack, target, damageSource, damage)) {
                applyKnockback(target, targetDirection, AwakeningApi.getGemPowerMultiplier(stack));
                hitAny = true;
            }
        }

        spawnVisual(world, attacker, direction);
        spawnEffects(world, attacker, direction, hitAny);
        return true;
    }

    private static Vec3d horizontalDirection(LivingEntity attacker) {
        Vec3d look = attacker.getRotationVec(1.0F);
        Vec3d direction = new Vec3d(look.x, 0.0, look.z);
        return direction.lengthSquared() > 1.0E-5 ? direction.normalize() : Vec3d.fromPolar(0.0F, attacker.getYaw()).normalize();
    }

    private static boolean hasActiveBuffet(ServerWorld world, LivingEntity owner) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof DragonWingBuffetVisualEntity visual
                    && visual.isAlive()
                    && visual.getOwnerEntityId() == owner.getId()) {
                return true;
            }
        }
        return false;
    }

    private static void applyKnockback(LivingEntity target, Vec3d targetDirection, float multiplier) {
        double strength = Math.max(0.0, Config.gemPowers.wingBuffet.knockbackStrength) * multiplier;
        double upward = Math.max(0.0, Config.gemPowers.wingBuffet.upwardKnockback) * multiplier;
        target.setVelocity(target.getVelocity().add(targetDirection.x * strength, upward, targetDirection.z * strength));
        target.velocityModified = true;
    }

    private static boolean damageTarget(ServerWorld world, ItemStack stack, LivingEntity target, DamageSource damageSource, float damage) {
        int timeUntilRegen = target.timeUntilRegen;
        target.timeUntilRegen = 0;
        float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage);
        boolean damaged = target.damage(damageSource, enchantedDamage);
        target.timeUntilRegen = timeUntilRegen;
        return damaged;
    }

    private static void spawnVisual(ServerWorld world, LivingEntity attacker, Vec3d direction) {
        Vec3d anchor = DragonWingBuffetVisualEntity.getAnchorPosition(attacker, direction);
        DragonWingBuffetVisualEntity visual = new DragonWingBuffetVisualEntity(
                world,
                attacker,
                anchor.x,
                anchor.y,
                anchor.z,
                attacker.getYaw(),
                Math.max(1, Config.gemPowers.wingBuffet.visualLifetime),
                Math.max(0.1F, Config.gemPowers.wingBuffet.visualScale),
                attacker.getRandom().nextFloat()
        );
        world.spawnEntity(visual);
    }

    private static void spawnEffects(ServerWorld world, LivingEntity attacker, Vec3d direction, boolean hitAny) {
        Vec3d center = attacker.getPos().add(0.0, attacker.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.POOF, center.x, center.y, center.z, 18, 0.55, 0.25, 0.55, 0.06);
        for (int i = 1; i <= 4; i++) {
            Vec3d pos = center.add(direction.multiply(i * 0.9));
            world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 5, 0.35, 0.18, 0.35, 0.025);
        }

        world.playSound(null, attacker.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.2F, 0.85F);
        world.playSound(null, attacker.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.55F, 1.35F);
        world.playSound(null, attacker.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9F, 0.55F);
        if (hitAny) {
            world.playSound(null, attacker.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.55F, 1.15F);
            world.playSound(null, attacker.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.55F, 1.6F);
        }
    }
}
