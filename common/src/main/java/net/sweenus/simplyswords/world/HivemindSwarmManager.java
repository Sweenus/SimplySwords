package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class HivemindSwarmManager {

    private static final double STING_RANGE = 0.8;
    private static final double MOVE_SPEED = 0.36;
    private static final double ATTACK_PASS_MIN_SPEED = 0.32;
    private static final double ATTACK_PASS_MAX_SPEED = 0.68;
    private static final double ATTACK_PASS_MIN_DISTANCE = 1.55;
    private static final double ATTACK_PASS_MAX_DISTANCE = 2.45;
    private static final double ATTACK_PASS_ANGLE_VARIATION = Math.toRadians(42.0);
    private static final double ATTACK_PASS_VERTICAL_VARIATION = 0.42;
    private static final double TURN_LINEUP_SPEED = 0.28;
    private static final double TURN_LINEUP_DISTANCE = 3.6;
    private static final double TURN_LINEUP_ANGLE = Math.toRadians(95.0);
    private static final double TURN_LINEUP_COMPLETE_DISTANCE = 1.45;
    private static final double PASS_COMPLETE_DISTANCE = 0.45;
    private static final int DIVE_DELAY_MIN_TICKS = 4;
    private static final int DIVE_DELAY_MAX_TICKS = 22;

    private HivemindSwarmManager() {
    }

    public static void activate(ServerWorld world, ServerPlayerEntity player) {
        activate(world, player, player);
    }

    public static void activate(ServerWorld world, ServerPlayerEntity player, LivingEntity actor) {
        activate(world, actor, getStingDamage(player));
    }

    public static void activate(ServerWorld world, LivingEntity actor) {
        activate(world, actor, getStingDamage(actor));
    }

    private static void activate(ServerWorld world, LivingEntity actor, float stingDamage) {
        long now = world.getTime();
        int beeCount = Math.max(0, Config.uniqueEffects.hiveheart.swarmBeeCount);
        if (beeCount <= 0 || actor == null || !actor.isAlive()) {
            return;
        }
        int stings = Math.max(1, Config.uniqueEffects.hiveheart.stingsPerBee);
        long expiryTick = now + Math.max(20, Config.uniqueEffects.hiveheart.swarmLifetime);

        for (int i = 0; i < beeCount; i++) {
            double angle = (Math.PI * 2.0 / beeCount) * i;
            Vec3d spawnPos = actor.getPos().add(Math.cos(angle) * 0.9, 1.3 + (i % 3) * 0.15, Math.sin(angle) * 0.9);
            SimplySwordsBeeEntity bee = EntityRegistry.SIMPLYBEEENTITY.get().spawn(world, actor.getBlockPos(), SpawnReason.MOB_SUMMONED);
            if (bee == null) {
                continue;
            }
            bee.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, actor.getYaw(), 0.0F);
            bee.setOwner(actor);
            bee.setSwarmAnchorUuid(actor.getUuid());
            bee.setHivemindSwarmBee(true);
            bee.setSwarmStingsRemaining(stings);
            bee.setSwarmStingDamage(stingDamage);
            bee.setSwarmExpiryTick(expiryTick);
            bee.setSwarmNextStingTick(now + i % Math.max(1, Config.uniqueEffects.hiveheart.stingIntervalTicks));
            bee.setSwarmNextDiveTick(now + randomDiveDelay(world));
            bee.setInvulnerable(true);
            bee.setNoGravity(true);
            bee.setAiDisabled(true);
            playBeeSound(bee, i % 3 == 0 ? SoundEvents.ENTITY_BEE_POLLINATE : SoundEvents.ENTITY_BEE_LOOP, 0.08F, 1.35F);
        }

        world.spawnParticles(ParticleTypes.FALLING_HONEY, actor.getX(), actor.getBodyY(0.6), actor.getZ(), 18, 0.55, 0.35, 0.55, 0.04);
    }

    public static boolean hasActive(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SimplySwordsBeeEntity bee && bee.isHivemindSwarmBee()) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerWorld world) {
        List<SimplySwordsBeeEntity> bees = getSwarmBees(world);
        if (bees.isEmpty()) {
            return;
        }

        Map<UUID, Integer> targetCounts = new HashMap<>();
        for (SimplySwordsBeeEntity bee : bees) {
            LivingEntity owner = getOwner(world, bee);
            LivingEntity anchor = getAnchor(world, bee, owner);
            LivingEntity target = getTarget(world, bee);
            if (owner == null || anchor == null || !isValidTarget(owner, anchor, target)) {
                bee.setSwarmTargetUuid(null);
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                continue;
            }
            targetCounts.merge(target.getUuid(), 1, Integer::sum);
        }

        for (SimplySwordsBeeEntity bee : bees) {
            LivingEntity owner = getOwner(world, bee);
            if (owner == null) {
                bee.discard();
                continue;
            }
            LivingEntity anchor = getAnchor(world, bee, owner);
            if (anchor == null) {
                bee.discard();
                continue;
            }

            LivingEntity target = getTarget(world, bee);
            if (!isValidTarget(owner, anchor, target)) {
                if (target != null) {
                    targetCounts.computeIfPresent(target.getUuid(), (ignored, count) -> Math.max(0, count - 1));
                }
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                target = selectTarget(world, owner, anchor, targetCounts);
                bee.setSwarmTargetUuid(target == null ? null : target.getUuid());
                if (target != null) {
                    targetCounts.merge(target.getUuid(), 1, Integer::sum);
                }
            }

            if (target == null) {
                bee.clearSwarmPass();
                bee.clearSwarmLineup();
                hoverNearAnchor(bee, anchor);
                continue;
            }

            moveTowardTarget(world, bee, target);
            trySting(world, owner, bee, target);
        }

        applySlowness(world, targetCounts);
    }

    private static List<SimplySwordsBeeEntity> getSwarmBees(ServerWorld world) {
        List<SimplySwordsBeeEntity> bees = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SimplySwordsBeeEntity bee && bee.isHivemindSwarmBee() && bee.isAlive()) {
                bees.add(bee);
            }
        }
        return bees;
    }

    private static float getStingDamage(LivingEntity actor) {
        return HelperMethods.abilityScaledDamage("nature", actor,
                actor == null ? ItemStack.EMPTY : actor.getMainHandStack(),
                (float) Config.uniqueEffects.hiveheart.stingDamageScaling,
                (float) Config.uniqueEffects.hiveheart.stingSpellScaling);
    }

    private static LivingEntity getOwner(ServerWorld world, SimplySwordsBeeEntity bee) {
        UUID ownerUuid = bee.getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (player != null && player.isAlive()) {
            return player;
        }
        Entity entity = world.getEntity(ownerUuid);
        return entity instanceof LivingEntity livingEntity && livingEntity.isAlive() ? livingEntity : null;
    }

    private static LivingEntity getAnchor(ServerWorld world, SimplySwordsBeeEntity bee, LivingEntity owner) {
        UUID anchorUuid = bee.getSwarmAnchorUuid();
        if (anchorUuid == null) {
            return owner;
        }
        Entity entity = world.getEntity(anchorUuid);
        return entity instanceof LivingEntity livingEntity && livingEntity.isAlive() ? livingEntity : null;
    }

    private static LivingEntity getTarget(ServerWorld world, SimplySwordsBeeEntity bee) {
        UUID targetUuid = bee.getSwarmTargetUuid();
        if (targetUuid == null) {
            return null;
        }
        Entity entity = world.getEntity(targetUuid);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private static boolean isValidTarget(LivingEntity owner, LivingEntity anchor, LivingEntity target) {
        return target != null
                && target.isAlive()
                && target != owner
                && target != anchor
                && target.squaredDistanceTo(anchor) <= Config.uniqueEffects.hiveheart.swarmRadius * Config.uniqueEffects.hiveheart.swarmRadius
                && HelperMethods.checkAbilityTarget(target, owner);
    }

    private static LivingEntity selectTarget(ServerWorld world, LivingEntity owner, LivingEntity anchor, Map<UUID, Integer> targetCounts) {
        double radius = Math.max(1.0, Config.uniqueEffects.hiveheart.swarmRadius);
        Box searchBox = anchor.getBoundingBox().expand(radius, radius * 0.5, radius);
        LivingEntity selected = null;
        int selectedCount = Integer.MAX_VALUE;
        double selectedDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, searchBox, candidate -> isValidTarget(owner, anchor, candidate))) {
            int count = targetCounts.getOrDefault(candidate.getUuid(), 0);
            double distance = candidate.squaredDistanceTo(anchor);
            if (count < selectedCount || (count == selectedCount && distance < selectedDistance)) {
                selected = candidate;
                selectedCount = count;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private static void hoverNearAnchor(SimplySwordsBeeEntity bee, LivingEntity owner) {
        double angle = (bee.age * 0.19) + (bee.getId() * 0.7);
        Vec3d destination = owner.getPos().add(Math.cos(angle) * 1.4, 1.5 + Math.sin(angle * 0.7) * 0.25, Math.sin(angle) * 1.4);
        moveToward(bee, destination, MOVE_SPEED * 0.7);
    }

    private static void moveTowardTarget(ServerWorld world, SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d lineup = getLineupPos(bee, target);
        if (lineup != null) {
            moveToward(bee, lineup, TURN_LINEUP_SPEED);
            if (bee.getPos().squaredDistanceTo(lineup) <= TURN_LINEUP_COMPLETE_DISTANCE * TURN_LINEUP_COMPLETE_DISTANCE) {
                bee.setSwarmNextDiveTick(world.getTime() + randomDiveDelay(world));
                bee.clearSwarmLineup();
            }
            return;
        }

        if (bee.getSwarmPassExitPos() == null && world.getTime() < bee.getSwarmNextDiveTick()) {
            hoverNearTarget(bee, target);
            return;
        }

        Vec3d destination = getOrCreatePassExit(bee, target);
        moveToward(bee, destination, getAttackPassSpeed(bee, destination));
        if (bee.getPos().squaredDistanceTo(destination) <= PASS_COMPLETE_DISTANCE * PASS_COMPLETE_DISTANCE) {
            createLineupPoint(bee, target, destination);
            bee.clearSwarmPass();
        }
    }

    private static Vec3d getLineupPos(SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d lineup = bee.getSwarmLineupPos();
        UUID lineupTargetUuid = bee.getSwarmLineupTargetUuid();
        if (lineup != null && target.getUuid().equals(lineupTargetUuid)) {
            return lineup;
        }
        if (lineup != null || lineupTargetUuid != null) {
            bee.clearSwarmLineup();
        }
        return null;
    }

    private static void createLineupPoint(SimplySwordsBeeEntity bee, LivingEntity target, Vec3d passExit) {
        Vec3d targetCenter = getTargetCenter(target);
        Vec3d away = passExit.subtract(targetCenter);
        double horizontalLength = Math.sqrt(away.x * away.x + away.z * away.z);
        Vec3d horizontal = horizontalLength < 0.001
                ? new Vec3d(1.0, 0.0, 0.0)
                : new Vec3d(away.x / horizontalLength, 0.0, away.z / horizontalLength);
        double turnSign = ((bee.getId() + bee.age) & 1) == 0 ? 1.0 : -1.0;
        double turnAngle = TURN_LINEUP_ANGLE * turnSign;
        double cos = Math.cos(turnAngle);
        double sin = Math.sin(turnAngle);
        double x = horizontal.x * cos - horizontal.z * sin;
        double z = horizontal.x * sin + horizontal.z * cos;
        double y = Math.sin((bee.age + bee.getId()) * 0.43) * ATTACK_PASS_VERTICAL_VARIATION;
        Vec3d lineup = targetCenter.add(new Vec3d(x, y, z).normalize().multiply(TURN_LINEUP_DISTANCE + Math.max(0.35, target.getWidth() * 0.5)));
        bee.setSwarmLineupTargetUuid(target.getUuid());
        bee.setSwarmLineupPos(lineup);
    }

    private static void hoverNearTarget(SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d targetCenter = getTargetCenter(target);
        double angle = (bee.age * 0.16) + (bee.getId() * 0.91);
        double radius = 2.0 + Math.sin((bee.age + bee.getId()) * 0.23) * 0.35;
        Vec3d destination = targetCenter.add(Math.cos(angle) * radius, 0.35 + Math.sin(angle * 0.7) * 0.25, Math.sin(angle) * radius);
        moveToward(bee, destination, MOVE_SPEED * 0.75);
        if ((bee.age + bee.getId() * 11) % 48 == 0) {
            playBeeSound(bee, SoundEvents.ENTITY_BEE_LOOP, 0.055F, 1.45F);
        }
    }

    private static Vec3d getOrCreatePassExit(SimplySwordsBeeEntity bee, LivingEntity target) {
        Vec3d existingExit = bee.getSwarmPassExitPos();
        UUID passTargetUuid = bee.getSwarmPassTargetUuid();
        if (existingExit != null && target.getUuid().equals(passTargetUuid)) {
            return existingExit;
        }

        Vec3d targetCenter = getTargetCenter(target);
        Vec3d approach = targetCenter.subtract(bee.getPos());
        if (approach.lengthSquared() < 0.001) {
            double angle = (bee.age * 0.31) + (bee.getId() * 0.9);
            approach = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
        }
        Vec3d direction = getVariedPassDirection(bee, approach);
        Vec3d exit = targetCenter.add(direction.multiply(randomizedPassDistance(bee) + Math.max(0.35, target.getWidth() * 0.5)));
        bee.setSwarmPassTargetUuid(target.getUuid());
        bee.setSwarmPassStartPos(bee.getPos());
        bee.setSwarmPassExitPos(exit);
        bee.setSwarmPassStung(false);
        playBeeSound(bee, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 0.12F, 1.55F);
        return exit;
    }

    private static double getAttackPassSpeed(SimplySwordsBeeEntity bee, Vec3d destination) {
        Vec3d start = bee.getSwarmPassStartPos();
        if (start == null) {
            return ATTACK_PASS_MIN_SPEED;
        }

        double totalDistance = Math.sqrt(start.squaredDistanceTo(destination));
        if (totalDistance < 0.001) {
            return ATTACK_PASS_MIN_SPEED;
        }

        double remainingDistance = Math.sqrt(bee.getPos().squaredDistanceTo(destination));
        double progress = 1.0 - remainingDistance / totalDistance;
        progress = net.minecraft.util.math.MathHelper.clamp(progress, 0.0, 1.0);
        double ease = Math.sin(progress * Math.PI);
        return ATTACK_PASS_MIN_SPEED + (ATTACK_PASS_MAX_SPEED - ATTACK_PASS_MIN_SPEED) * ease;
    }

    private static int randomDiveDelay(ServerWorld world) {
        return DIVE_DELAY_MIN_TICKS + world.random.nextInt(DIVE_DELAY_MAX_TICKS - DIVE_DELAY_MIN_TICKS + 1);
    }

    private static double randomizedPassDistance(SimplySwordsBeeEntity bee) {
        double seed = Math.sin((bee.age * 0.37) + (bee.getId() * 2.17)) * 0.5 + 0.5;
        return ATTACK_PASS_MIN_DISTANCE + (ATTACK_PASS_MAX_DISTANCE - ATTACK_PASS_MIN_DISTANCE) * seed;
    }

    private static Vec3d getVariedPassDirection(SimplySwordsBeeEntity bee, Vec3d approach) {
        double horizontalLength = Math.sqrt(approach.x * approach.x + approach.z * approach.z);
        Vec3d horizontal = horizontalLength < 0.001
                ? new Vec3d(1.0, 0.0, 0.0)
                : new Vec3d(approach.x / horizontalLength, 0.0, approach.z / horizontalLength);
        double seed = bee.age * 0.73 + bee.getId() * 1.91;
        double angleOffset = Math.sin(seed) * ATTACK_PASS_ANGLE_VARIATION;
        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);
        double x = horizontal.x * cos - horizontal.z * sin;
        double z = horizontal.x * sin + horizontal.z * cos;
        double y = Math.sin(seed * 1.37) * ATTACK_PASS_VERTICAL_VARIATION;
        return new Vec3d(x, y, z).normalize();
    }

    private static Vec3d getTargetCenter(LivingEntity target) {
        return target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.58), 0.0);
    }

    private static void moveToward(SimplySwordsBeeEntity bee, Vec3d destination, double speed) {
        Vec3d delta = destination.subtract(bee.getPos());
        if (delta.lengthSquared() < 0.01) {
            bee.setVelocity(Vec3d.ZERO);
            return;
        }
        Vec3d step = delta.normalize().multiply(Math.min(speed, delta.length()));
        Vec3d nextPos = bee.getPos().add(step);
        double horizontalLength = Math.sqrt(step.x * step.x + step.z * step.z);
        float yaw = horizontalLength < 0.001
                ? bee.getYaw()
                : (float) (Math.atan2(step.z, step.x) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(step.y, horizontalLength) * (180.0F / Math.PI));
        bee.refreshPositionAndAngles(nextPos.x, nextPos.y, nextPos.z, yaw, pitch);
        bee.setYaw(yaw);
        bee.setPitch(pitch);
        bee.setBodyYaw(yaw);
        bee.setHeadYaw(yaw);
        bee.prevBodyYaw = yaw;
        bee.prevHeadYaw = yaw;
        bee.setVelocity(Vec3d.ZERO);
    }

    private static void trySting(ServerWorld world, LivingEntity owner, SimplySwordsBeeEntity bee, LivingEntity target) {
        if (bee.hasSwarmPassStung() || world.getTime() < bee.getSwarmNextStingTick() || bee.getSwarmStingsRemaining() <= 0) {
            return;
        }
        if (!target.getBoundingBox().expand(STING_RANGE).contains(bee.getPos())) {
            return;
        }

        int iframes = target.timeUntilRegen;
        Vec3d velocity = target.getVelocity();
        target.timeUntilRegen = 0;
        boolean[] damaged = {false};
        DamageSource damageSource = owner.getDamageSources().indirectMagic(owner, owner);
        ItemStack stack = owner.getMainHandStack();
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, bee.getSwarmStingDamage());
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(damageSource, damage));
        target.timeUntilRegen = iframes;
        target.setVelocity(velocity);
        target.velocityModified = true;
        if (damaged[0]) {
            bee.decrementSwarmStingsRemaining();
            bee.setSwarmPassStung(true);
            bee.setSwarmNextStingTick(world.getTime() + Math.max(1, Config.uniqueEffects.hiveheart.stingIntervalTicks));
            Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
            world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 2, 0.16, 0.14, 0.16, 0.02);
            world.spawnParticles(ParticleTypes.FALLING_HONEY, pos.x, pos.y, pos.z, 2, 0.18, 0.18, 0.18, 0.015);
            playBeeSound(bee, SoundEvents.ENTITY_BEE_STING, 0.16F, 1.65F);
            if (bee.getSwarmStingsRemaining() <= 0) {
                bee.discard();
            }
        }
    }

    private static void applySlowness(ServerWorld world, Map<UUID, Integer> targetCounts) {
        for (Map.Entry<UUID, Integer> entry : targetCounts.entrySet()) {
            int beeCount = entry.getValue();
            if (beeCount <= 0) {
                continue;
            }
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }
            int amplifier = net.minecraft.util.math.MathHelper.clamp(beeCount - 1, 0, Math.max(0, Config.uniqueEffects.hiveheart.maxSlowAmplifier));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 12, amplifier, false, false, true));
        }
    }

    private static void playBeeSound(SimplySwordsBeeEntity bee, SoundEvent sound, float volume, float basePitch) {
        float pitch = basePitch + (bee.getWorld().random.nextFloat() - 0.5F) * 0.35F;
        bee.getWorld().playSound(null, bee.getX(), bee.getY(), bee.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
