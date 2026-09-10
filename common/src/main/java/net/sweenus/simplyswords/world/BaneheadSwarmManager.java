package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsCreeperHeadEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BaneheadSwarmManager {

    private static final double MINION_SEARCH_RADIUS = 32.0;
    private static final Map<UUID, Long> NEXT_HOMING_ELIGIBLE_TICK = new HashMap<>();

    private BaneheadSwarmManager() {
    }

    public static boolean tryConsumeHomingCooldown(ServerWorld world, UUID ownerUuid) {
        if (ownerUuid == null) {
            return true;
        }
        long now = world.getTime();
        Long nextEligible = NEXT_HOMING_ELIGIBLE_TICK.get(ownerUuid);
        if (nextEligible != null && now < nextEligible) {
            return false;
        }
        int cooldownTicks = Math.max(0, Config.gemPowers.baneheadSwarm.homingCooldownTicks);
        NEXT_HOMING_ELIGIBLE_TICK.put(ownerUuid, now + cooldownTicks);
        return true;
    }

    public static void trySpawnHead(LivingEntity user, ItemStack stack) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (user == null || stack == null || stack.isEmpty() || !user.isAlive()) {
            return;
        }
        if (!(user.getWorld() instanceof ServerWorld world)) {
            return;
        }

        int maxActiveHeads = Math.max(1, Config.gemPowers.baneheadSwarm.maxActiveHeads);
        int activeCount = getActiveHeadCount(world, user);
        if (activeCount >= maxActiveHeads) {
            return;
        }

        double orbitBaseAngle = activeCount * ((Math.PI * 2.0) / maxActiveHeads);
        double orbitRadius = Config.gemPowers.baneheadSwarm.orbitRadius;
        double orbitHeight = Config.gemPowers.baneheadSwarm.orbitHeight;
        float damage = HelperMethods.gemPowerScaledDamage(SpellScalingComponents.power("banehead_swarm"), user, stack,
                (float) Config.gemPowers.baneheadSwarm.damageScaling,
                (float) Config.gemPowers.baneheadSwarm.spellScaling);

        SimplySwordsCreeperHeadEntity head = new SimplySwordsCreeperHeadEntity(EntityRegistry.CREEPER_HEAD.get(), world);
        Vec3d spawnPos = SimplySwordsCreeperHeadEntity.ringPositionAt(user.getPos(), orbitBaseAngle, orbitRadius, orbitHeight);
        head.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, user.getYaw(), 0.0F);
        head.initializeHead(user, stack, orbitBaseAngle, orbitRadius, orbitHeight,
                Config.gemPowers.baneheadSwarm.orbitAngularSpeed,
                Config.gemPowers.baneheadSwarm.homingRange,
                Config.gemPowers.baneheadSwarm.homingSpeed,
                Config.gemPowers.baneheadSwarm.homingInitialSpeedFraction,
                Config.gemPowers.baneheadSwarm.homingAccelerationTicks,
                Config.gemPowers.baneheadSwarm.homingTurnRateDegrees,
                Config.gemPowers.baneheadSwarm.explosionProximity,
                Config.gemPowers.baneheadSwarm.explosionRadius,
                damage,
                Config.gemPowers.baneheadSwarm.maxHomingTicks);

        world.spawnEntity(head);
        }
    }

    private static int getActiveHeadCount(ServerWorld world, LivingEntity owner) {
        Box searchBox = owner.getBoundingBox().expand(MINION_SEARCH_RADIUS);
        return world.getEntitiesByClass(SimplySwordsCreeperHeadEntity.class, searchBox,
                head -> head.isAlive() && owner.getUuid().equals(head.getOwnerUuid())).size();
    }
}
