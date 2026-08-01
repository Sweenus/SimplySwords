package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.StarsEdgeConstellationVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StarsEdgeAbilityManager {

    private static final String VISUAL_TAG = "simplyswords_stars_edge_constellation";
    private static final int PHASE_INITIAL_DASH = 0;
    private static final int PHASE_RECORDING = 1;
    private static final int PHASE_DETONATING = 2;

    private static final Map<ServerWorld, Map<UUID, ActiveReprise>> ACTIVE = new HashMap<>();

    private StarsEdgeAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.STARS_EDGE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1) {
            return false;
        }

        ActiveReprise active = getActive(context.world(), context.actor().getUuid());
        if (active != null) {
            return active.phase == PHASE_RECORDING && isWieldingStarsEdge(context.actor());
        }

        if (!isWieldingStarsEdge(context.actor())) {
            return false;
        }
        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        return context.target() != null
                && context.target().isAlive()
                && HelperMethods.checkAbilityTarget(context.target(), context.actor());
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        ActiveReprise existing = getActive(context.world(), context.actor().getUuid());
        if (existing != null) {
            sealCurrentEndpoint(context.world(), context.actor(), existing);
            beginDetonation(context.world(), context.actor(), existing);
            return true;
        }
        return start(context);
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        return getActive(world, actor.getUuid()) != null;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveReprise> active = ACTIVE.get(world);
        return active != null && !active.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveReprise> activeByOwner = ACTIVE.get(world);
        if (activeByOwner == null || activeByOwner.isEmpty()) {
            return;
        }

        Iterator<ActiveReprise> iterator = activeByOwner.values().iterator();
        while (iterator.hasNext()) {
            ActiveReprise active = iterator.next();
            Entity ownerEntity = world.getEntity(active.actorId);
            if (!(ownerEntity instanceof LivingEntity actor) || !actor.isAlive() || actor.isRemoved()) {
                applyMissingOwnerCooldown(world, active);
                fadeVisuals(world, active);
                iterator.remove();
                continue;
            }

            if (active.phase != PHASE_DETONATING && !isWieldingStarsEdge(actor)) {
                stopForcedMovement(actor);
                applyCooldown(world, actor, active);
                fadeVisuals(world, active);
                spawnCancellationEffects(world, actor);
                iterator.remove();
                continue;
            }

            if (tickActive(world, actor, active)) {
                iterator.remove();
            }
        }

        if (activeByOwner.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static boolean start(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        Vec3d direction = resolveDirection(context);
        if (direction.lengthSquared() < 0.0001) {
            return false;
        }

        long now = context.world().getTime();
        ItemStack abilityStack = context.stack().copy();
        float constellationDamage = AwakeningApi.scaleEffect(
                abilityStack,
                HelperMethods.attackScaledDamage(actor, abilityStack,
                        Math.max(0.0F, Config.uniqueEffects.stars_edge.constellationDamageScaling))
        );
        ActiveReprise active = new ActiveReprise(
                actor.getUuid(),
                abilityStack,
                direction,
                actor.getPos(),
                now,
                constellationDamage
        );
        ACTIVE.computeIfAbsent(context.world(), ignored -> new HashMap<>())
                .put(actor.getUuid(), active);
        appendNode(context.world(), active, actor.getPos());
        applyForcedVelocity(actor, direction, Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashSpeed));
        spawnActivationEffects(context.world(), actor);
        return true;
    }

    private static boolean tickActive(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        return switch (active.phase) {
            case PHASE_INITIAL_DASH -> tickInitialDash(world, actor, active);
            case PHASE_RECORDING -> tickRecording(world, actor, active);
            case PHASE_DETONATING -> tickDetonation(world, actor, active);
            default -> true;
        };
    }

    private static boolean tickInitialDash(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        Vec3d current = actor.getPos();
        double moved = horizontalDistance(active.previousPosition, current);
        active.dashDistanceTravelled += moved;
        active.previousPosition = current;
        if (recordMovement(world, active, current)) {
            beginDetonation(world, actor, active);
            return false;
        }

        long elapsed = world.getTime() - active.phaseStartedAt;
        double distance = Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashDistance);
        double speed = Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashSpeed);
        int maximumTicks = Math.max(2, (int) Math.ceil(distance / speed) + 4);
        if ((elapsed > 1L && actor.horizontalCollision)
                || active.dashDistanceTravelled >= distance
                || elapsed >= maximumTicks) {
            stopForcedMovement(actor);
            active.phase = PHASE_RECORDING;
            active.phaseStartedAt = world.getTime();
            active.previousPosition = actor.getPos();
            spawnRecordingReadyEffects(world, actor);
            return false;
        }

        applyForcedVelocity(actor, active.initialDirection,
                Math.min(speed, Math.max(0.0, distance - active.dashDistanceTravelled)));
        spawnCometTrail(world, actor);
        return false;
    }

    private static boolean tickRecording(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        Vec3d current = actor.getPos();
        active.previousPosition = current;
        if (recordMovement(world, active, current)) {
            beginDetonation(world, actor, active);
            return false;
        }

        int recordingDuration = Math.max(1, Config.uniqueEffects.stars_edge.recordingDuration);
        if (world.getTime() - active.phaseStartedAt >= recordingDuration) {
            sealCurrentEndpoint(world, actor, active);
            beginDetonation(world, actor, active);
        } else if ((actor.age + actor.getId()) % 3 == 0) {
            spawnRecordingMotes(world, actor);
        }
        return false;
    }

    private static boolean tickDetonation(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        long phaseAge = world.getTime() - active.phaseStartedAt;
        if (phaseAge < 8L) {
            spawnCompletionRing(world, active, (int) phaseAge);
        }
        if (active.nextSegmentIndex >= active.nodes.size()) {
            fadeVisuals(world, active);
            return true;
        }
        if (world.getTime() < active.nextExplosionTick) {
            damageConstellationOnContact(world, actor, active, phaseAge);
            return false;
        }

        int segmentIndex = active.nextSegmentIndex;
        RouteNode startNode = active.nodes.get(segmentIndex - 1);
        RouteNode endNode = active.nodes.get(segmentIndex);
        detonateSegment(world, actor, active, startNode.position, endNode.position);
        setVisualPhase(world, startNode.nodeVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
        setVisualPhase(world, endNode.linkVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);

        active.nextSegmentIndex++;
        if (active.nextSegmentIndex >= active.nodes.size()) {
            setVisualPhase(world, endNode.nodeVisualId, StarsEdgeConstellationVisualEntity.PHASE_DETONATE);
            return true;
        }

        damageConstellationOnContact(world, actor, active, phaseAge);
        active.nextExplosionTick = world.getTime()
                + Math.max(1, Config.uniqueEffects.stars_edge.segmentExplosionInterval);
        return false;
    }

    private static boolean recordMovement(ServerWorld world, ActiveReprise active, Vec3d current) {
        double spacing = Math.max(0.25, Config.uniqueEffects.stars_edge.nodeSpacing);
        int maxNodes = Math.clamp(Config.uniqueEffects.stars_edge.maxNodes, 2, 16);
        RouteNode lastNode = active.nodes.getLast();
        Vec3d remaining = current.subtract(lastNode.position);
        while (remaining.length() >= spacing && active.nodes.size() < maxNodes) {
            Vec3d next = lastNode.position.add(remaining.normalize().multiply(spacing));
            appendNode(world, active, next);
            lastNode = active.nodes.getLast();
            remaining = current.subtract(lastNode.position);
        }
        return active.nodes.size() >= maxNodes;
    }

    private static void sealCurrentEndpoint(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        Vec3d current = actor.getPos();
        recordMovement(world, active, current);
        int maxNodes = Math.clamp(Config.uniqueEffects.stars_edge.maxNodes, 2, 16);
        if (active.nodes.size() < maxNodes && active.nodes.getLast().position.distanceTo(current) > 0.25) {
            appendNode(world, active, current);
        }
    }

    private static void beginDetonation(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        if (active.phase == PHASE_DETONATING) {
            return;
        }
        stopForcedMovement(actor);
        active.phase = PHASE_DETONATING;
        active.phaseStartedAt = world.getTime();
        active.nextSegmentIndex = 1;
        active.nextExplosionTick = world.getTime()
                + Math.max(1, Config.uniqueEffects.stars_edge.constellationDuration);
        applyCooldown(world, actor, active);
        setAllVisualPhases(world, active, StarsEdgeConstellationVisualEntity.PHASE_ARMED);
        spawnCompletionEffects(world, actor, actor.getPos());
    }

    private static void appendNode(ServerWorld world, ActiveReprise active, Vec3d position) {
        StarsEdgeConstellationVisualEntity nodeVisual = StarsEdgeConstellationVisualEntity.node(
                world, position, visualLifetime());
        nodeVisual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(nodeVisual);

        UUID linkVisualId = null;
        if (!active.nodes.isEmpty()) {
            Vec3d previous = active.nodes.getLast().position;
            StarsEdgeConstellationVisualEntity linkVisual = StarsEdgeConstellationVisualEntity.link(
                    world, previous, position, visualLifetime());
            linkVisual.addCommandTag(VISUAL_TAG);
            world.spawnEntity(linkVisual);
            linkVisualId = linkVisual.getUuid();
        }
        active.nodes.add(new RouteNode(position, nodeVisual.getUuid(), linkVisualId));
        spawnNodeEffects(world, position, active.nodes.size());
    }

    private static int visualLifetime() {
        long requested = (long) Math.max(1, Config.uniqueEffects.stars_edge.recordingDuration)
                + Math.max(1, Config.uniqueEffects.stars_edge.constellationDuration)
                + (long) Math.max(2, Config.uniqueEffects.stars_edge.maxNodes)
                * Math.max(1, Config.uniqueEffects.stars_edge.segmentExplosionInterval)
                + 60L;
        return (int) Math.min(Integer.MAX_VALUE - 1L, Math.max(120L, requested));
    }

    private static void setAllVisualPhases(ServerWorld world, ActiveReprise active, int phase) {
        for (RouteNode node : active.nodes) {
            setVisualPhase(world, node.nodeVisualId, phase);
            setVisualPhase(world, node.linkVisualId, phase);
        }
    }

    private static void setVisualPhase(ServerWorld world, UUID visualId, int phase) {
        if (visualId == null) {
            return;
        }
        if (world.getEntity(visualId) instanceof StarsEdgeConstellationVisualEntity visual) {
            visual.setPhase(phase);
            if (phase == StarsEdgeConstellationVisualEntity.PHASE_DETONATE) {
                visual.setLifetime(visual.age + 5);
            } else if (phase == StarsEdgeConstellationVisualEntity.PHASE_FADE) {
                visual.setLifetime(visual.age + 10);
            }
        }
    }

    private static void fadeVisuals(ServerWorld world, ActiveReprise active) {
        setAllVisualPhases(world, active, StarsEdgeConstellationVisualEntity.PHASE_FADE);
    }

    private static void detonateSegment(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                        Vec3d start, Vec3d end) {
        damageSegmentExplosion(world, actor, active, start, end,
                Math.max(0.1, Config.uniqueEffects.stars_edge.segmentExplosionRadius),
                active.constellationDamage);
        spawnSegmentExplosionEffects(world, actor, start, end);
    }

    private static void damageConstellationOnContact(ServerWorld world, LivingEntity actor,
                                                      ActiveReprise active, long phaseAge) {
        int interval = Math.max(1, Config.uniqueEffects.stars_edge.constellationDamageInterval);
        if (phaseAge % interval != 0L || active.constellationDamage <= 0.0F) {
            return;
        }

        double width = Math.max(0.1, Config.uniqueEffects.stars_edge.constellationDamageWidth);
        Set<UUID> pulseHitTargets = new HashSet<>();
        for (int segmentIndex = active.nextSegmentIndex;
             segmentIndex < active.nodes.size();
             segmentIndex++) {
            damageContactSegment(world, actor, active,
                    active.nodes.get(segmentIndex - 1).position,
                    active.nodes.get(segmentIndex).position,
                    width,
                    pulseHitTargets);
        }
    }

    private static void damageContactSegment(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                             Vec3d start, Vec3d end, double width,
                                             Set<UUID> pulseHitTargets) {
        if (start.squaredDistanceTo(end) < 0.0001) {
            return;
        }

        Vec3d bodyStart = start.add(0.0, 0.85, 0.0);
        Vec3d bodyEnd = end.add(0.0, 0.85, 0.0);
        double halfWidth = width * 0.5;
        Box search = new Box(bodyStart, bodyEnd).expand(halfWidth + 1.0);

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                target -> target != actor
                        && target.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && !pulseHitTargets.contains(target.getUuid())
                        && HelperMethods.checkAbilityTarget(target, actor))) {
            Box targetBox = target.getBoundingBox();
            double lineMidY = (bodyStart.y + bodyEnd.y) * 0.5;
            Vec3d targetCenter = new Vec3d(
                    target.getX(),
                    Math.clamp(lineMidY, targetBox.minY, targetBox.maxY),
                    target.getZ());
            double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
            double allowed = halfWidth + targetRadius;
            if (distanceSquaredToSegment(targetCenter, bodyStart, bodyEnd) > allowed * allowed) {
                continue;
            }
            if (!pulseHitTargets.add(target.getUuid())) {
                continue;
            }
            damageTarget(world, actor, active.stack, target, active.constellationDamage, true);
        }
    }

    private static void damageSegmentExplosion(ServerWorld world, LivingEntity actor, ActiveReprise active,
                                               Vec3d start, Vec3d end, double radius,
                                               float baseDamage) {
        if (baseDamage <= 0.0F || start.squaredDistanceTo(end) < 0.0001) {
            return;
        }

        Vec3d bodyStart = start.add(0.0, 0.85, 0.0);
        Vec3d bodyEnd = end.add(0.0, 0.85, 0.0);
        Box search = new Box(bodyStart, bodyEnd).expand(radius + 1.0);
        Set<UUID> segmentHitTargets = new HashSet<>();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                target -> target != actor
                        && target.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && HelperMethods.checkAbilityTarget(target, actor))) {
            Box targetBox = target.getBoundingBox();
            double lineMidY = (bodyStart.y + bodyEnd.y) * 0.5;
            Vec3d targetCenter = new Vec3d(
                    target.getX(),
                    Math.clamp(lineMidY, targetBox.minY, targetBox.maxY),
                    target.getZ());
            double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
            double allowed = radius + targetRadius;
            if (distanceSquaredToSegment(targetCenter, bodyStart, bodyEnd) > allowed * allowed) {
                continue;
            }
            if (!segmentHitTargets.add(target.getUuid())) {
                continue;
            }
            damageTarget(world, actor, active.stack, target, baseDamage, false);
        }
    }

    private static void spawnSegmentExplosionEffects(ServerWorld world, LivingEntity actor,
                                                      Vec3d start, Vec3d end) {
        Vec3d raisedStart = start.add(0.0, 0.85, 0.0);
        Vec3d raisedEnd = end.add(0.0, 0.85, 0.0);
        double length = raisedStart.distanceTo(raisedEnd);
        int samples = Math.max(3, (int) Math.ceil(length * 3.0));
        for (int i = 0; i <= samples; i++) {
            Vec3d point = raisedStart.lerp(raisedEnd, i / (double) samples);
            world.spawnParticles(ParticleTypes.FIREWORK, point.x, point.y, point.z,
                    7, 0.2, 0.24, 0.2, 0.08);
            world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                    4, 0.16, 0.2, 0.16, 0.045);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y, point.z,
                    5, 0.22, 0.24, 0.22, 0.06);
        }

        Vec3d midpoint = raisedStart.lerp(raisedEnd, 0.5);
        double radius = Math.max(0.1, Config.uniqueEffects.stars_edge.segmentExplosionRadius);
        int ringPoints = Math.max(24, (int) Math.ceil(radius * 14.0));
        for (int i = 0; i < ringPoints; i++) {
            double angle = Math.PI * 2.0 * i / ringPoints;
            world.spawnParticles(ParticleTypes.FIREWORK,
                    midpoint.x + Math.cos(angle) * radius,
                    midpoint.y,
                    midpoint.z + Math.sin(angle) * radius,
                    1, 0.015, 0.025, 0.015, 0.02);
        }
        world.playSound(null, midpoint.x, midpoint.y, midpoint.z,
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 0.75F, 1.05F + world.random.nextFloat() * 0.2F);
        world.playSound(null, midpoint.x, midpoint.y, midpoint.z,
                SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                actor.getSoundCategory(), 0.55F, 1.35F + world.random.nextFloat() * 0.2F);
    }

    private static void damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                     LivingEntity target, float baseDamage, boolean preserveVelocity) {
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world, stack, target, source, baseDamage);
        Vec3d previousVelocity = preserveVelocity ? target.getVelocity() : null;
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        if (damaged[0] && preserveVelocity) {
            target.setVelocity(previousVelocity);
            target.velocityModified = true;
            target.velocityDirty = true;
        }
        Vec3d center = target.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                10, 0.28, 0.32, 0.28, 0.05);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                8, 0.24, 0.28, 0.24, 0.04);
    }

    private static double distanceSquaredToSegment(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d segment = end.subtract(start);
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared < 0.0001) {
            return point.squaredDistanceTo(start);
        }
        double projection = point.subtract(start).dotProduct(segment) / lengthSquared;
        double t = Math.clamp(projection, 0.0, 1.0);
        return point.squaredDistanceTo(start.add(segment.multiply(t)));
    }

    private static Vec3d resolveDirection(WeaponAbilityContext context) {
        Vec3d direction;
        if (!(context.actor() instanceof PlayerEntity)
                && context.target() != null
                && context.target().isAlive()) {
            direction = context.target().getPos().subtract(context.actor().getPos());
        } else {
            direction = context.facing();
        }
        Vec3d horizontal = direction == null ? Vec3d.ZERO : new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, context.actor().getYaw());
        }
        return horizontal.normalize();
    }

    private static void applyForcedVelocity(LivingEntity actor, Vec3d direction, double speed) {
        actor.setVelocity(direction.multiply(speed));
        actor.fallDistance = 0.0F;
        actor.velocityModified = true;
    }

    private static void stopForcedMovement(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        actor.setVelocity(Vec3d.ZERO);
        actor.velocityModified = true;
    }

    private static boolean isWieldingStarsEdge(LivingEntity actor) {
        return actor.getMainHandStack().isOf(ItemsRegistry.STARS_EDGE.get())
                || actor.getOffHandStack().isOf(ItemsRegistry.STARS_EDGE.get());
    }

    private static void applyCooldown(ServerWorld world, LivingEntity actor, ActiveReprise active) {
        if (active.cooldownApplied) {
            return;
        }
        active.cooldownApplied = true;
        int cooldown = Math.max(1, Config.uniqueEffects.stars_edge.cooldown);
        if (actor instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(active.stack.getItem(), cooldown);
        } else {
            WeaponAbilityCooldownManager.setCooldown(world, actor, active.stack, cooldown);
        }
    }

    private static void applyMissingOwnerCooldown(ServerWorld world, ActiveReprise active) {
        if (active.cooldownApplied) {
            return;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(active.actorId);
        if (player != null) {
            applyCooldown(player.getServerWorld(), player, active);
        }
    }

    private static ActiveReprise getActive(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveReprise> active = ACTIVE.get(world);
        return active == null ? null : active.get(actorId);
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double dx = second.x - first.x;
        double dz = second.z - first.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                28, 0.42, 0.5, 0.42, 0.12);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                22, 0.38, 0.45, 0.38, 0.08);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_FLYBY_01.get(),
                actor.getSoundCategory(), 0.65F, 1.2F);
    }

    private static void spawnRecordingReadyEffects(ServerWorld world, LivingEntity actor) {
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                actor.getSoundCategory(), 0.35F, 1.65F);
    }

    private static void spawnNodeEffects(ServerWorld world, Vec3d position, int index) {
        world.spawnParticles(ParticleTypes.END_ROD, position.x, position.y + 0.85, position.z,
                7, 0.12, 0.18, 0.12, 0.025);
        world.playSound(null, position.x, position.y, position.z,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                net.minecraft.sound.SoundCategory.PLAYERS, 0.18F,
                Math.min(1.9F, 1.05F + index * 0.08F));
    }

    private static void spawnRecordingMotes(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                2, 0.22, 0.28, 0.22, 0.015);
    }

    private static void spawnCometTrail(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                5, 0.16, 0.22, 0.16, 0.025);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                3, 0.18, 0.2, 0.18, 0.035);
    }

    private static void spawnCompletionRing(ServerWorld world, ActiveReprise active, int age) {
        Vec3d center = active.nodes.isEmpty() ? active.previousPosition : active.nodes.getFirst().position;
        double maximumRadius = 2.5;
        for (RouteNode node : active.nodes) {
            double dx = node.position.x - center.x;
            double dz = node.position.z - center.z;
            maximumRadius = Math.max(maximumRadius, Math.sqrt(dx * dx + dz * dz));
        }
        maximumRadius = Math.min(9.0, maximumRadius + 1.25);
        double radius = maximumRadius * (age + 1.0) / 8.0;
        int points = Math.max(24, (int) Math.ceil(radius * 10.0));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points + age * 0.14;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.END_ROD, x, center.y + 0.16, z,
                    1, 0.01, 0.02, 0.01, 0.01);
            if ((i + age) % 3 == 0) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, center.y + 0.22, z,
                        1, 0.015, 0.025, 0.015, 0.015);
            }
        }
    }

    private static void spawnCompletionEffects(ServerWorld world, LivingEntity actor, Vec3d origin) {
        world.spawnParticles(ParticleTypes.FIREWORK, origin.x, origin.y + 0.7, origin.z,
                64, 1.0, 0.7, 1.0, 0.16);
        world.spawnParticles(ParticleTypes.END_ROD, origin.x, origin.y + 0.7, origin.z,
                42, 0.85, 0.55, 0.85, 0.11);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 0.95F, 0.8F);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                actor.getSoundCategory(), 0.75F, 1.15F);
    }

    private static void spawnCancellationEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getBoundingBox().getCenter();
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                12, 0.3, 0.35, 0.3, 0.02);
    }

    private static final class ActiveReprise {
        private final UUID actorId;
        private final ItemStack stack;
        private final Vec3d initialDirection;
        private final float constellationDamage;
        private final List<RouteNode> nodes = new ArrayList<>();
        private int phase = PHASE_INITIAL_DASH;
        private long phaseStartedAt;
        private Vec3d previousPosition;
        private double dashDistanceTravelled;
        private int nextSegmentIndex;
        private long nextExplosionTick;
        private boolean cooldownApplied;

        private ActiveReprise(UUID actorId, ItemStack stack, Vec3d initialDirection,
                              Vec3d previousPosition, long startedAt,
                              float constellationDamage) {
            this.actorId = actorId;
            this.stack = stack;
            this.initialDirection = initialDirection;
            this.previousPosition = previousPosition;
            this.phaseStartedAt = startedAt;
            this.constellationDamage = constellationDamage;
        }
    }

    private record RouteNode(Vec3d position, UUID nodeVisualId, UUID linkVisualId) {
    }
}
