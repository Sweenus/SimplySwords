package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class MinionTargeting {

    private static final double TAUNT_RADIUS = 16.0;
    private static final int TAUNT_INTERVAL_TICKS = 90;
    private static final int MIN_TAUNTS_PER_PULSE = 1;
    private static final int MAX_TAUNTS_PER_PULSE = 3;
    private static final long RECENT_ATTACK_WINDOW_TICKS = 100L;

    private static final Map<UUID, LastAttack> LAST_ATTACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> MARKED_TARGETS = new ConcurrentHashMap<>();

    private MinionTargeting() {
    }

    public static void recordLastAttack(LivingEntity player, LivingEntity target) {
        if (player == null || target == null || IgnoredEntities.isIgnored(target)) {
            return;
        }
        LAST_ATTACK.put(player.getUuid(), new LastAttack(target.getUuid(), player.getWorld().getTime()));
        MARKED_TARGETS.computeIfAbsent(player.getUuid(), k -> new HashSet<>()).add(target.getUuid());
    }

    public static boolean isMarkedTarget(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (owner == null || target == null) {
            return false;
        }
        Set<UUID> marked = MARKED_TARGETS.get(owner.getUuid());
        if (marked == null || marked.isEmpty()) {
            return false;
        }
        if (!marked.contains(target.getUuid())) {
            return false;
        }
        Entity entity = world.getEntity(target.getUuid());
        if (!(entity instanceof LivingEntity living) || !living.isAlive()
                || IgnoredEntities.isIgnored(living)) {
            marked.remove(target.getUuid());
            if (marked.isEmpty()) {
                MARKED_TARGETS.remove(owner.getUuid());
            }
            return false;
        }
        return true;
    }

    public static void cleanupMarkedTargets(ServerWorld world, LivingEntity owner) {
        if (owner == null) {
            return;
        }
        Set<UUID> marked = MARKED_TARGETS.get(owner.getUuid());
        if (marked == null || marked.isEmpty()) {
            return;
        }
        Iterator<UUID> it = marked.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            Entity entity = world.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || IgnoredEntities.isIgnored(living)) {
                it.remove();
            }
        }
        if (marked.isEmpty()) {
            MARKED_TARGETS.remove(owner.getUuid());
        }
    }

    public static LivingEntity getRecentAttackTarget(ServerWorld world, LivingEntity owner) {
        if (owner == null) {
            return null;
        }
        LastAttack record = LAST_ATTACK.get(owner.getUuid());
        if (record == null) {
            return null;
        }
        if (world.getTime() - record.time() > RECENT_ATTACK_WINDOW_TICKS) {
            return null;
        }
        Entity entity = world.getEntity(record.targetUuid());
        if (entity instanceof LivingEntity living && living.isAlive()
                && !IgnoredEntities.isIgnored(living)) {
            return living;
        }
        LAST_ATTACK.remove(owner.getUuid(), record);
        return null;
    }

    public static void tauntNearbyEnemies(ServerWorld world, LivingEntity minion, LivingEntity owner, Predicate<LivingEntity> validEnemy) {
        if (owner == null) {
            return;
        }
        Box box = minion.getBoundingBox().expand(TAUNT_RADIUS);
        List<MobEntity> eligible = new ArrayList<>();
        for (Entity entity : world.getOtherEntities(minion, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof MobEntity mob) || IgnoredEntities.isIgnored(mob)) {
                continue;
            }
            LivingEntity current = mob.getTarget();
            if (current != null && current != minion && current != owner) {
                continue;
            }
            if (!validEnemy.test(mob)) {
                continue;
            }
            eligible.add(mob);
        }
        if (eligible.isEmpty()) {
            return;
        }
        int count = Math.min(eligible.size(), world.random.nextInt(MAX_TAUNTS_PER_PULSE) + MIN_TAUNTS_PER_PULSE);
        eligible.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(minion)));
        for (int i = 0; i < count; i++) {
            eligible.get(i).setTarget(minion);
        }
    }

    public static LivingEntity findNearestValidToOwner(ServerWorld world, LivingEntity owner, Predicate<LivingEntity> validEnemy, double radius) {
        if (owner == null) {
            return null;
        }
        Box box = owner.getBoundingBox().expand(radius);
        return world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> !IgnoredEntities.isIgnored(entity))
                .filter(validEnemy)
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(owner)))
                .orElse(null);
    }

    public static LivingEntity getOwnerCurrentTarget(ServerWorld world, LivingEntity owner) {
        if (owner == null) return null;
        if (owner instanceof MobEntity mob) {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive() && !IgnoredEntities.isIgnored(target)) return target;
        }
        return getRecentAttackTarget(world, owner);
    }

    public static int tauntIntervalTicks() {
        return TAUNT_INTERVAL_TICKS;
    }

    private record LastAttack(UUID targetUuid, long time) {
    }
}
