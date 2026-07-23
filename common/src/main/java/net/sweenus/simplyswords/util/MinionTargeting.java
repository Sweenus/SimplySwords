package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    private MinionTargeting() {
    }

    public static void recordLastAttack(ServerPlayerEntity player, LivingEntity target) {
        if (player == null || target == null) {
            return;
        }
        LAST_ATTACK.put(player.getUuid(), new LastAttack(target.getUuid(), player.getWorld().getTime()));
    }

    public static LivingEntity getRecentAttackTarget(ServerWorld world, ServerPlayerEntity owner) {
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
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }

    public static void tauntNearbyEnemies(ServerWorld world, LivingEntity minion, ServerPlayerEntity owner, Predicate<LivingEntity> validEnemy) {
        if (owner == null) {
            return;
        }
        Box box = minion.getBoundingBox().expand(TAUNT_RADIUS);
        List<MobEntity> eligible = new ArrayList<>();
        for (Entity entity : world.getOtherEntities(minion, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof MobEntity mob)) {
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

    public static LivingEntity findNearestValidToOwner(ServerWorld world, ServerPlayerEntity owner, Predicate<LivingEntity> validEnemy, double radius) {
        if (owner == null) {
            return null;
        }
        Box box = owner.getBoundingBox().expand(radius);
        return world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(validEnemy)
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(owner)))
                .orElse(null);
    }

    public static int tauntIntervalTicks() {
        return TAUNT_INTERVAL_TICKS;
    }

    private record LastAttack(UUID targetUuid, long time) {
    }
}
