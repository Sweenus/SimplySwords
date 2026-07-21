package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulkeeperLanternVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class SoulkeeperLanternManager {

    private static final String SOULKEEPER_LANTERN_VISUAL_TAG = "simplyswords_soulkeeper_lantern_visual";
    private static final double BASE_ANGULAR_SPEED = 0.055;
    private static final double LANTERN_HEIGHT = 1.05;
    private static final double BOB_AMPLITUDE = 0.08;
    private static final double BOB_SPEED = 0.13;
    private static final double CONTACT_RADIUS = 0.85;
    private static final int BASE_LANTERN_COUNT = 2;
    private static final int ACTIVE_LANTERN_COUNT = 4;

    private static final Map<UUID, ActiveLanterns> ACTIVE_LANTERNS = new HashMap<>();

    private SoulkeeperLanternManager() {
    }

    public static void tickPlayerFromItem(ServerPlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.SOULKEEPER.get()) || !player.getMainHandStack().equals(stack)) {
            discardActive(player.getServerWorld(), player.getUuid());
            ACTIVE_LANTERNS.remove(player.getUuid());
            return;
        }
        tickActivePlayer(player, stack);
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        if (!isHoldingSoulkeeper(player) || !player.isAlive()) {
            discardActive(player.getServerWorld(), player.getUuid());
            ACTIVE_LANTERNS.remove(player.getUuid());
        }
    }

    public static void tickWorld(ServerWorld world) {
        if (world.getTime() % 80L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof SoulkeeperLanternVisualEntity visual) || !entity.getCommandTags().contains(SOULKEEPER_LANTERN_VISUAL_TAG)) {
                continue;
            }
            UUID ownerId = visual.getOwnerUuid();
            ServerPlayerEntity owner = ownerId == null ? null : world.getServer().getPlayerManager().getPlayer(ownerId);
            ActiveLanterns active = ownerId == null ? null : ACTIVE_LANTERNS.get(ownerId);
            if (owner == null || !owner.isAlive() || !isHoldingSoulkeeper(owner)) {
                visual.discard();
                if (ownerId != null) {
                    ACTIVE_LANTERNS.remove(ownerId);
                }
                continue;
            }
            if (active == null || !visual.getUuid().equals(active.visualId)) {
                visual.discard();
            }
        }
    }

    public static void onSoulkeeperHit(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player && isHoldingSoulkeeper(player)) {
            increaseSpeed(player);
        }
    }

    public static void activate(ServerPlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.SOULKEEPER.get()) || !isHoldingSoulkeeper(player)) {
            return;
        }

        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(player.getUuid(), ignored -> new ActiveLanterns());
        active.extraLanternsUntilTick = player.getServerWorld().getTime() + Config.uniqueEffects.soulkeeper.activeExtraLanternDuration;
        increaseSpeed(active);

        ServerWorld world = player.getServerWorld();
        Vec3d pos = player.getPos().add(0.0, 1.0, 0.0);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 18, 0.55, 0.35, 0.55, 0.03);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 8, 0.45, 0.3, 0.45, 0.02);
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_03.get(), SoundCategory.PLAYERS, 0.75F, 0.8F + player.getRandom().nextFloat() * 0.2F);
    }

    private static void tickActivePlayer(ServerPlayerEntity player, ItemStack stack) {
        ServerWorld world = player.getServerWorld();
        UUID ownerId = player.getUuid();
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(ownerId, ignored -> new ActiveLanterns());
        active.tick(world);

        int lanternCount = active.hasExtraLanterns(world) ? ACTIVE_LANTERN_COUNT : BASE_LANTERN_COUNT;
        SoulkeeperLanternVisualEntity visual = resolveTracked(world, active.visualId, ownerId);
        if (visual == null) {
            Vec3d start = player.getPos();
            visual = new SoulkeeperLanternVisualEntity(world, ownerId, player.getId(), start.x, start.y, start.z);
            visual.addCommandTag(SOULKEEPER_LANTERN_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                active.visualId = visual.getUuid();
            }
        }

        Vec3d pos = player.getPos();
        visual.setPos(pos.x, pos.y, pos.z);
        visual.setOwnerEntityId(player.getId());
        visual.setLanternCount(lanternCount);
        visual.setSpeedMultiplier(active.speedMultiplier);
        visual.setOrbitRadius((float) Config.uniqueEffects.soulkeeper.orbitRadius);
        visual.setOrbitPhase((float) active.orbitPhase);

        damageCollidingTargets(world, player, stack, active, lanternCount);
    }

    private static void damageCollidingTargets(ServerWorld world, ServerPlayerEntity player, ItemStack stack, ActiveLanterns active, int lanternCount) {
        float damage = getLanternDamage(player, stack);
        if (damage <= 0.0F || lanternCount <= 0) {
            return;
        }

        double orbitRadius = Math.max(0.25, Config.uniqueEffects.soulkeeper.orbitRadius);
        double searchRadius = orbitRadius + CONTACT_RADIUS + 1.0;
        Box searchBox = player.getBoundingBox().expand(searchRadius, 2.5, searchRadius);
        Set<UUID> currentlyColliding = new HashSet<>();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, target -> target != player && target.isAlive())) {
            if (!HelperMethods.checkFriendlyFire(target, player)) {
                continue;
            }

            if (!isCollidingWithAnyLantern(player.getPos(), target.getBoundingBox(), active.orbitPhase, orbitRadius, lanternCount)) {
                continue;
            }

            UUID targetId = target.getUuid();
            currentlyColliding.add(targetId);
            if (active.collidingTargets.contains(targetId)) {
                continue;
            }

            if (damageTarget(world, player, target, damage)) {
                increaseSpeed(active);
                spawnLanternHitEffects(world, target);
            }
        }

        active.collidingTargets.clear();
        active.collidingTargets.addAll(currentlyColliding);
    }

    private static float getLanternDamage(ServerPlayerEntity player, ItemStack stack) {
        double attackDamage = HelperMethods.getEntityAttackDamage(player);
        if (attackDamage <= 0.0) {
            attackDamage = Math.max(1.0, 1.0 + HelperMethods.getAttackFromSlot(player, stack, Hand.MAIN_HAND)[0]);
        }
        return (float) (attackDamage * Config.uniqueEffects.soulkeeper.lanternDamageMultiplier);
    }

    private static boolean damageTarget(ServerWorld world, ServerPlayerEntity player, LivingEntity target, float damage) {
        target.timeUntilRegen = 0;
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(player.getDamageSources().indirectMagic(player, player), damage));
        target.timeUntilRegen = 0;
        if (!damaged[0]) {
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(world.getDamageSources().magic(), damage));
            target.timeUntilRegen = 0;
        }
        return damaged[0];
    }

    private static boolean isCollidingWithAnyLantern(Vec3d center, Box targetBox, double baseAngle, double orbitRadius, int lanternCount) {
        for (int i = 0; i < lanternCount; i++) {
            double angle = baseAngle + ((Math.PI * 2.0) / lanternCount) * i;
            double bob = Math.sin(baseAngle * (BOB_SPEED / BASE_ANGULAR_SPEED) + i * 1.7) * BOB_AMPLITUDE;
            Vec3d lanternCenter = center.add(Math.cos(angle) * orbitRadius, LANTERN_HEIGHT + 0.5 + bob, Math.sin(angle) * orbitRadius);
            if (squaredDistanceToBox(lanternCenter, targetBox) <= CONTACT_RADIUS * CONTACT_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static double squaredDistanceToBox(Vec3d point, Box box) {
        double dx = distanceToRange(point.x, box.minX, box.maxX);
        double dy = distanceToRange(point.y, box.minY, box.maxY);
        double dz = distanceToRange(point.z, box.minZ, box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceToRange(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0;
    }

    private static void increaseSpeed(ServerPlayerEntity player) {
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(player.getUuid(), ignored -> new ActiveLanterns());
        increaseSpeed(active);
    }

    private static void increaseSpeed(ActiveLanterns active) {
        active.speedMultiplier = Math.min((float) Config.uniqueEffects.soulkeeper.maxSpeedMultiplier,
                active.speedMultiplier + (float) Config.uniqueEffects.soulkeeper.speedIncreasePerHit);
    }

    private static void spawnLanternHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 4, 0.18, 0.18, 0.18, 0.025);
    }

    private static boolean isHoldingSoulkeeper(PlayerEntity player) {
        return player.getMainHandStack().isOf(ItemsRegistry.SOULKEEPER.get());
    }

    private static SoulkeeperLanternVisualEntity resolveTracked(ServerWorld world, UUID visualId, UUID ownerId) {
        if (visualId == null) {
            return null;
        }

        Entity entity = world.getEntity(visualId);
        if (!(entity instanceof SoulkeeperLanternVisualEntity visual) || !ownerId.equals(visual.getOwnerUuid()) || !visual.isAlive()) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        return visual;
    }

    private static void discardActive(ServerWorld world, UUID ownerId) {
        ActiveLanterns active = ACTIVE_LANTERNS.get(ownerId);
        if (active == null || active.visualId == null) {
            return;
        }
        Entity entity = world.getEntity(active.visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static final class ActiveLanterns {
        private UUID visualId;
        private float speedMultiplier = 1.0F;
        private double orbitPhase;
        private long extraLanternsUntilTick;
        private final Set<UUID> collidingTargets = new HashSet<>();

        private void tick(ServerWorld world) {
            orbitPhase += BASE_ANGULAR_SPEED * speedMultiplier;
            while (orbitPhase > Math.PI * 2.0) {
                orbitPhase -= Math.PI * 2.0;
            }
            if (speedMultiplier > 1.0F) {
                speedMultiplier = Math.max(1.0F, speedMultiplier - (float) Config.uniqueEffects.soulkeeper.speedLossPerSecond / 20.0F);
            }
            if (extraLanternsUntilTick > 0L && world.getTime() > extraLanternsUntilTick) {
                extraLanternsUntilTick = 0L;
            }
        }

        private boolean hasExtraLanterns(ServerWorld world) {
            return extraLanternsUntilTick > world.getTime();
        }
    }
}
