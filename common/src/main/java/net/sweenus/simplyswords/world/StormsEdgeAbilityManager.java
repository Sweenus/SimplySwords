package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
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

public final class StormsEdgeAbilityManager {

    private static final int TRAIL_COLOR = 0xA6F3FF;
    private static final int THUNDERCLAP_VISUAL_TICKS = 5;
    private static final ChainLightningVisualManager.LightningVisualSettings TRAIL_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(TRAIL_COLOR, 6, 0.05F, 3);
    private static final Map<ServerWorld, Map<UUID, ActiveStormbreak>> ACTIVE_DASHES = new HashMap<>();
    private static final Map<ServerWorld, List<ThunderclapVisual>> ACTIVE_THUNDERCLAPS = new HashMap<>();

    private StormsEdgeAbilityManager() {
    }

    public static boolean start(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.STORMS_EDGE.get())) {
            return false;
        }

        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        if (!(actor instanceof PlayerEntity)
                && (target == null || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, actor))) {
            return false;
        }

        ServerWorld world = context.world();
        Map<UUID, ActiveStormbreak> active = ACTIVE_DASHES.computeIfAbsent(world, ignored -> new HashMap<>());
        if (active.containsKey(actor.getUuid())) {
            return false;
        }

        Vec3d direction = resolveDirection(context);
        if (direction.horizontalLengthSquared() < 0.0001) {
            return false;
        }

        Vec3d start = actor.getPos();
        ActiveStormbreak stormbreak = new ActiveStormbreak(
                actor.getUuid(),
                context.stack().copy(),
                direction,
                start,
                world.getTime()
        );
        active.put(actor.getUuid(), stormbreak);
        double initialSpeed = Math.min(
                Math.max(0.1, Config.uniqueEffects.storms_edge.dashSpeed),
                Math.max(0.1, Config.uniqueEffects.storms_edge.dashDistance)
        );
        applyDashVelocity(actor, direction, initialSpeed);
        spawnDashStartEffects(world, actor);
        return true;
    }

    public static boolean isDashing(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveStormbreak> active = ACTIVE_DASHES.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    public static boolean blocksIncomingDamage(LivingEntity actor, DamageSource source) {
        return actor != null
                && source != null
                && !actor.getWorld().isClient()
                && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && isDashing(actor);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveStormbreak> dashes = ACTIVE_DASHES.get(world);
        List<ThunderclapVisual> thunderclaps = ACTIVE_THUNDERCLAPS.get(world);
        return (dashes != null && !dashes.isEmpty())
                || (thunderclaps != null && !thunderclaps.isEmpty());
    }

    public static void tick(ServerWorld world) {
        tickDashes(world);
        tickThunderclapVisuals(world);
    }

    private static void tickDashes(ServerWorld world) {
        Map<UUID, ActiveStormbreak> active = ACTIVE_DASHES.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        Iterator<ActiveStormbreak> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveStormbreak stormbreak = iterator.next();
            Entity entity = world.getEntity(stormbreak.actorId);
            if (!(entity instanceof LivingEntity actor) || !actor.isAlive() || actor.isRemoved()) {
                stopDashMovement(entity instanceof LivingEntity living ? living : null);
                iterator.remove();
                continue;
            }

            if (tickDash(world, actor, stormbreak)) {
                iterator.remove();
            }
        }

        if (active.isEmpty()) {
            ACTIVE_DASHES.remove(world);
        }
    }

    private static boolean tickDash(ServerWorld world, LivingEntity actor, ActiveStormbreak stormbreak) {
        Vec3d current = actor.getPos();
        Vec3d previous = stormbreak.previousPosition;
        double moved = horizontalDistance(previous, current);
        if (moved > 0.01) {
            stormbreak.distanceTravelled += moved;
            damageCorridorTargets(world, actor, stormbreak, previous, current);
            spawnDashTrail(world, actor, previous, current);
        }
        stormbreak.previousPosition = current;

        long elapsed = world.getTime() - stormbreak.startedAt;
        double maxDistance = Math.max(0.1, Config.uniqueEffects.storms_edge.dashDistance);
        double dashSpeed = Math.max(0.1, Config.uniqueEffects.storms_edge.dashSpeed);
        int maximumTicks = Math.max(2, (int) Math.ceil(maxDistance / dashSpeed) + 4);
        boolean struckTerrain = elapsed > 1L && actor.horizontalCollision;
        if (struckTerrain || stormbreak.distanceTravelled >= maxDistance || elapsed >= maximumTicks) {
            finishDash(world, actor, stormbreak);
            return true;
        }

        applyDashVelocity(actor, stormbreak.direction,
                Math.min(dashSpeed, Math.max(0.0, maxDistance - stormbreak.distanceTravelled)));
        return false;
    }

    private static void damageCorridorTargets(ServerWorld world, LivingEntity actor, ActiveStormbreak stormbreak,
                                              Vec3d previous, Vec3d current) {
        double width = Math.max(0.1, Config.uniqueEffects.storms_edge.corridorWidth);
        double halfWidth = width * 0.5;
        double verticalPadding = Math.max(0.5, halfWidth * 0.6);
        double minY = Math.min(previous.y, current.y) - verticalPadding;
        double maxY = Math.max(previous.y + actor.getHeight(), current.y + actor.getHeight()) + verticalPadding;
        Box searchBox = new Box(
                Math.min(previous.x, current.x) - halfWidth,
                minY,
                Math.min(previous.z, current.z) - halfWidth,
                Math.max(previous.x, current.x) + halfWidth,
                maxY,
                Math.max(previous.z, current.z) + halfWidth
        );

        float baseDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                stormbreak.stack,
                Config.uniqueEffects.storms_edge.damageScaling,
                Config.uniqueEffects.storms_edge.spellScaling
        );
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox,
                target -> target != actor
                        && target.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && !stormbreak.corridorHitTargets.contains(target.getUuid())
                        && HelperMethods.checkAbilityTarget(target, actor)
                        && intersectsCorridor(target, previous, current, halfWidth, minY, maxY))) {
            stormbreak.corridorHitTargets.add(target.getUuid());
            if (damageTarget(world, actor, stormbreak.stack, target, baseDamage)) {
                knockAside(target, stormbreak.direction, previous, current,
                        Config.uniqueEffects.storms_edge.corridorKnockback,
                        Config.uniqueEffects.storms_edge.corridorKnockUp);
                spawnCorridorHitEffects(world, target);
            }
        }
    }

    private static void finishDash(ServerWorld world, LivingEntity actor, ActiveStormbreak stormbreak) {
        stopDashMovement(actor);
        applyThunderclap(world, actor, stormbreak.stack);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1), actor);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), actor);
        ACTIVE_THUNDERCLAPS.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new ThunderclapVisual(actor.getPos(), Math.max(0.1, Config.uniqueEffects.storms_edge.thunderclapRadius)));
        spawnThunderclapStartEffects(world, actor);
    }

    private static void applyThunderclap(ServerWorld world, LivingEntity actor, ItemStack stack) {
        double radius = Math.max(0.1, Config.uniqueEffects.storms_edge.thunderclapRadius);
        double verticalRadius = Math.max(1.5, radius * 0.65);
        Box searchBox = actor.getBoundingBox().expand(radius, verticalRadius, radius);
        float baseDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                stack,
                Config.uniqueEffects.storms_edge.thunderclapDamageScaling,
                Config.uniqueEffects.storms_edge.thunderclapSpellScaling
        );
        Vec3d center = actor.getPos();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox,
                target -> target != actor
                        && target.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                        && HelperMethods.checkAbilityTarget(target, actor)
                        && isInsideThunderclap(target, center, radius, verticalRadius))) {
            if (damageTarget(world, actor, stack, target, baseDamage)) {
                knockAway(target, center,
                        Config.uniqueEffects.storms_edge.thunderclapKnockback,
                        Config.uniqueEffects.storms_edge.thunderclapKnockUp);
                spawnThunderclapHitEffects(world, target);
            }
        }
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                        LivingEntity target, float baseDamage) {
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, baseDamage);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage)
        );
        return damaged[0];
    }

    private static boolean intersectsCorridor(LivingEntity target, Vec3d start, Vec3d end,
                                              double halfWidth, double minY, double maxY) {
        Box box = target.getBoundingBox();
        if (box.maxY < minY || box.minY > maxY) {
            return false;
        }
        double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
        return horizontalDistanceSquaredToSegment(target.getPos(), start, end)
                <= Math.pow(halfWidth + targetRadius, 2.0);
    }

    private static boolean isInsideThunderclap(LivingEntity target, Vec3d center,
                                               double radius, double verticalRadius) {
        Vec3d targetPos = target.getPos();
        double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
        double horizontalRadius = radius + targetRadius;
        double dx = targetPos.x - center.x;
        double dz = targetPos.z - center.z;
        return dx * dx + dz * dz <= horizontalRadius * horizontalRadius
                && target.getBoundingBox().maxY >= center.y - verticalRadius
                && target.getBoundingBox().minY <= center.y + verticalRadius;
    }

    private static double horizontalDistanceSquaredToSegment(Vec3d point, Vec3d start, Vec3d end) {
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared < 0.0001) {
            double px = point.x - start.x;
            double pz = point.z - start.z;
            return px * px + pz * pz;
        }
        double projection = ((point.x - start.x) * dx + (point.z - start.z) * dz) / lengthSquared;
        double t = net.minecraft.util.math.MathHelper.clamp(projection, 0.0, 1.0);
        double closestX = start.x + dx * t;
        double closestZ = start.z + dz * t;
        double px = point.x - closestX;
        double pz = point.z - closestZ;
        return px * px + pz * pz;
    }

    private static void knockAside(LivingEntity target, Vec3d dashDirection, Vec3d start, Vec3d end,
                                   double strength, double lift) {
        if (strength <= 0.0 && lift <= 0.0) {
            return;
        }
        Vec3d perpendicular = new Vec3d(-dashDirection.z, 0.0, dashDirection.x);
        Vec3d closest = closestHorizontalPointOnSegment(target.getPos(), start, end);
        double side = target.getPos().subtract(closest).dotProduct(perpendicular);
        if (Math.abs(side) < 0.001) {
            side = (target.getId() & 1) == 0 ? 1.0 : -1.0;
        }
        Vec3d outward = perpendicular.multiply(Math.signum(side));
        applyKnockback(target, outward, strength, lift);
    }

    private static Vec3d closestHorizontalPointOnSegment(Vec3d point, Vec3d start, Vec3d end) {
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared < 0.0001) {
            return start;
        }
        double projection = ((point.x - start.x) * dx + (point.z - start.z) * dz) / lengthSquared;
        double t = net.minecraft.util.math.MathHelper.clamp(projection, 0.0, 1.0);
        return new Vec3d(start.x + dx * t, point.y, start.z + dz * t);
    }

    private static void knockAway(LivingEntity target, Vec3d center, double strength, double lift) {
        Vec3d outward = target.getPos().subtract(center).multiply(1.0, 0.0, 1.0);
        if (outward.horizontalLengthSquared() < 0.0001) {
            outward = Vec3d.fromPolar(0.0F, target.getYaw());
        }
        applyKnockback(target, outward.normalize(), strength, lift);
    }

    private static void applyKnockback(LivingEntity target, Vec3d outward, double strength, double lift) {
        double resistance = net.minecraft.util.math.MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE),
                0.0,
                1.0
        );
        double appliedFactor = 1.0 - resistance;
        if (strength > 0.0 && outward.horizontalLengthSquared() > 0.0001) {
            target.takeKnockback(strength, -outward.x, -outward.z);
        }
        if (lift > 0.0 && appliedFactor > 0.0) {
            target.addVelocity(0.0, lift * appliedFactor, 0.0);
            target.velocityModified = true;
        }
    }

    private static Vec3d resolveDirection(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        if (!(actor instanceof PlayerEntity)
                && context.target() != null
                && context.target().isAlive()
                && HelperMethods.checkAbilityTarget(context.target(), actor)) {
            return horizontalDirection(context.target().getPos().subtract(actor.getPos()), actor);
        }
        return horizontalDirection(context.facing(), actor);
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction == null ? Vec3d.ZERO : new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 0.0001) {
            Vec3d rotation = actor.getRotationVec(1.0F);
            horizontal = new Vec3d(rotation.x, 0.0, rotation.z);
        }
        if (horizontal.lengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return horizontal.normalize();
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double dx = second.x - first.x;
        double dz = second.z - first.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void applyDashVelocity(LivingEntity actor, Vec3d direction, double speed) {
        actor.setVelocity(direction.x * speed, 0.0, direction.z * speed);
        actor.fallDistance = 0.0F;
        actor.velocityModified = true;
    }

    private static void stopDashMovement(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        actor.setVelocity(0.0, actor.getVelocity().y, 0.0);
        actor.velocityModified = true;
    }

    private static void spawnDashStartEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getPos().add(0.0, actor.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                24, 0.4, 0.45, 0.4, 0.11);
        world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 0.08, actor.getZ(),
                8, 0.35, 0.04, 0.35, 0.025);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                actor.getSoundCategory(), 0.45F, 1.55F);
    }

    private static void spawnDashTrail(ServerWorld world, LivingEntity actor, Vec3d previous, Vec3d current) {
        double height = Math.max(0.5, actor.getHeight() * 0.5);
        Vec3d start = previous.add(0.0, height, 0.0);
        Vec3d end = current.add(0.0, height, 0.0);
        ChainLightningVisualManager.spawnBolt(world, start, end, TRAIL_SETTINGS);
        Vec3d midpoint = start.lerp(end, 0.5);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, midpoint.x, midpoint.y, midpoint.z,
                5, 0.28, 0.3, 0.28, 0.04);
        world.spawnParticles(ParticleTypes.CLOUD, midpoint.x, current.y + 0.08, midpoint.z,
                3, 0.25, 0.03, 0.25, 0.015);
    }

    private static void spawnCorridorHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                12, 0.3, 0.35, 0.3, 0.09);
    }

    private static void spawnThunderclapStartEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getPos();
        world.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.25, center.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.3, center.z,
                46, 0.65, 0.35, 0.65, 0.15);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.08, center.z,
                24, 0.8, 0.08, 0.8, 0.09);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 0.85F, 0.85F);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_03.get(),
                actor.getSoundCategory(), 0.7F, 0.72F);
    }

    private static void spawnThunderclapHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                16, 0.38, 0.42, 0.38, 0.12);
    }

    private static void tickThunderclapVisuals(ServerWorld world) {
        List<ThunderclapVisual> thunderclaps = ACTIVE_THUNDERCLAPS.get(world);
        if (thunderclaps == null || thunderclaps.isEmpty()) {
            return;
        }

        Iterator<ThunderclapVisual> iterator = thunderclaps.iterator();
        while (iterator.hasNext()) {
            ThunderclapVisual visual = iterator.next();
            spawnThunderclapRing(world, visual);
            visual.age++;
            if (visual.age >= THUNDERCLAP_VISUAL_TICKS) {
                iterator.remove();
            }
        }
        if (thunderclaps.isEmpty()) {
            ACTIVE_THUNDERCLAPS.remove(world);
        }
    }

    private static void spawnThunderclapRing(ServerWorld world, ThunderclapVisual visual) {
        double progress = (visual.age + 1.0) / THUNDERCLAP_VISUAL_TICKS;
        double radius = visual.radius * progress;
        int points = Math.max(18, (int) Math.ceil(radius * 12.0));
        double rotation = visual.age * 0.22;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points + rotation;
            double x = visual.center.x + Math.cos(angle) * radius;
            double z = visual.center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, visual.center.y + 0.15, z,
                    1, 0.015, 0.025, 0.015, 0.015);
            if ((i + visual.age) % 3 == 0) {
                world.spawnParticles(ParticleTypes.CLOUD, x, visual.center.y + 0.05, z,
                        1, 0.025, 0.015, 0.025, 0.01);
            }
        }
    }

    private static final class ActiveStormbreak {
        private final UUID actorId;
        private final ItemStack stack;
        private final Vec3d direction;
        private final long startedAt;
        private final Set<UUID> corridorHitTargets = new HashSet<>();
        private Vec3d previousPosition;
        private double distanceTravelled;

        private ActiveStormbreak(UUID actorId, ItemStack stack, Vec3d direction,
                                 Vec3d previousPosition, long startedAt) {
            this.actorId = actorId;
            this.stack = stack;
            this.direction = direction;
            this.previousPosition = previousPosition;
            this.startedAt = startedAt;
        }
    }

    private static final class ThunderclapVisual {
        private final Vec3d center;
        private final double radius;
        private int age;

        private ThunderclapVisual(Vec3d center, double radius) {
            this.center = center;
            this.radius = radius;
        }
    }
}
