package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DevourerMassVisualEntity;
import net.sweenus.simplyswords.entity.DevourerTendrilVisualEntity;
import net.sweenus.simplyswords.item.component.AwakeningRouteComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DevourerAbilityManager {
    private static final int TRAVEL_TICKS = 8;
    private static final int BLOOM_TICKS = 10;
    private static final int COLLAPSE_TICKS = 8;
    private static final int ACQUISITION_INTERVAL = 4;
    private static final int LOOSE_LAUNCH_DELAY_MIN = 3;
    private static final int LOOSE_LAUNCH_DELAY_VARIANCE = 6;
    private static final double LOOSE_PULL_SPEED_SCALE = 0.5;
    private static final int LOST_SIGHT_GRACE = 8;
    private static final double RELEASE_BUFFER = 3.0;
    private static final String CAPTURED_GRAVITY_TAG = "simplyswords_devourer_captured_gravity";
    private static final DustColorTransitionParticleEffect DEVOURER_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.025F, 0.008F, 0.07F),
                    new Vector3f(0.42F, 0.08F, 0.72F), 1.35F);
    private static final Map<ServerWorld, Map<UUID, ActiveMass>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Set<UUID>> CAPTURED_LOOT = new HashMap<>();

    private DevourerAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveMass> active = ACTIVE.get(world);
        return active != null && !active.isEmpty()
                || !CAPTURED_LOOT.getOrDefault(world, Set.of()).isEmpty()
                || world.getTime() % 40L == 0L;
    }

    public static boolean hasActiveFor(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveMass> active = ACTIVE.get(world);
        return active != null && active.containsKey(actorId);
    }

    public static boolean isCapturedLoot(Entity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Set<UUID> captured = CAPTURED_LOOT.get(world);
        return captured != null && captured.contains(entity.getUuid());
    }

    public static ReprisalRedirect feedFromReprisal(ServerWorld world, UUID actorId,
                                                     Vec3d origin, LivingEntity primaryTarget,
                                                     int tendrilLifetime) {
        Map<UUID, ActiveMass> active = ACTIVE.get(world);
        ActiveMass mass = active == null ? null : active.get(actorId);
        if (mass == null || primaryTarget == null || !primaryTarget.isAlive()) {
            return null;
        }
        long now = world.getTime();
        double radius = Math.max(1.0, Config.uniqueEffects.devourer.targetingRadius);
        double vertical = Math.max(1.0, Config.uniqueEffects.devourer.verticalRange);
        if (now < mass.activeStartTick || now >= mass.activeEndTick
                || horizontalDistanceSquared(origin, mass.center) > radius * radius
                || Math.abs(origin.y - mass.center.y) > vertical) {
            return null;
        }
        DevourerMassVisualEntity visual = resolveMassVisual(world, mass.visualId);
        if (visual == null) {
            return null;
        }
        visual.feed();
        DevourerTendrilVisualEntity tendril = new DevourerTendrilVisualEntity(
                world, visual, primaryTarget, Math.max(1, tendrilLifetime));
        world.spawnEntity(tendril);
        tendril.triggerPulse();
        spawnFeedingEffects(world, primaryTarget, mass.center);
        world.playSound(null, mass.center.x, mass.center.y, mass.center.z,
                SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 0.72F, 0.82F);
        return new ReprisalRedirect(mass.center, tendril.getUuid());
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.THE_DEVOURER.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !hasActiveFor(context.world(), context.actor().getUuid())
                && resolveCastCenter(context) != null;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        Vec3d center = resolveCastCenter(context);
        Hand hand = resolveHand(context.actor(), context.stack(), context.hand());
        if (center == null || hand == null) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        Vec3d facing = context.facing().lengthSquared() < 1.0E-5
                ? actor.getRotationVec(1.0F) : context.facing().normalize();
        Vec3d start = actor.getEyePos().add(facing.multiply(0.55)).add(0.0, -0.18, 0.0);
        int duration = Math.max(1, Config.uniqueEffects.devourer.duration);
        float startingRadius = Math.max(0.2F, Config.uniqueEffects.devourer.startingMassRadius);
        float maximumRadius = Math.max(startingRadius, Config.uniqueEffects.devourer.maximumMassRadius);
        DevourerMassVisualEntity visual = new DevourerMassVisualEntity(world, start, center,
                TRAVEL_TICKS, BLOOM_TICKS, duration, COLLAPSE_TICKS, startingRadius, maximumRadius);
        world.spawnEntity(visual);

        long now = world.getTime();
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, actor, context.stack(),
                Config.uniqueEffects.devourer.damageScaling,
                Config.uniqueEffects.devourer.spellScaling);
        ActiveMass mass = new ActiveMass(actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                hand, context.stack(), context.stack().copy(), center,
                now + TRAVEL_TICKS, now + TRAVEL_TICKS + BLOOM_TICKS,
                now + TRAVEL_TICKS + BLOOM_TICKS + duration,
                now + TRAVEL_TICKS + BLOOM_TICKS + duration + COLLAPSE_TICKS,
                damage, visual.getUuid());
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), mass);
        DevourerStainManager.begin(world, actor.getUuid(), mass.sourcePlayerId,
                visual.getUuid(), center, mass.seedArrivalTick,
                mass.activeEndTick, mass.collapseEndTick);
        spawnCastEffects(world, actor, start);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            purgeOrphans(world);
        }
        Map<UUID, ActiveMass> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }
        Iterator<ActiveMass> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveMass mass = iterator.next();
            LivingEntity actor = resolveLiving(world, mass.actorId);
            DevourerMassVisualEntity visual = resolveMassVisual(world, mass.visualId);
            if (actor == null || visual == null || actor.getStackInHand(mass.hand) != mass.stackReference
                    || !mass.stackReference.isOf(ItemsRegistry.THE_DEVOURER.get())) {
                cancel(world, mass, visual);
                iterator.remove();
                continue;
            }

            long now = world.getTime();
            if (!mass.bloomed && now >= mass.seedArrivalTick) {
                mass.bloomed = true;
                spawnSeedArrivalEffects(world, mass.center);
            }
            if (now < mass.activeStartTick) {
                continue;
            }
            if (now < mass.activeEndTick) {
                boolean acquisitionTick = (now - mass.activeStartTick) % ACQUISITION_INTERVAL == 0L;
                if (acquisitionTick) {
                    acquireTargets(world, actor, mass, visual);
                }
                tickTargets(world, actor, mass, visual, now);
                boolean looseSlotFreed = tickLooseTargets(world, mass, visual, now);
                if (looseSlotFreed) {
                    mass.nextLooseLaunchTick = now + looseLaunchDelay(world);
                }
                if (now >= mass.nextLooseLaunchTick) {
                    if (acquireLooseTarget(world, mass, visual)) {
                        mass.nextLooseLaunchTick = now + looseLaunchDelay(world);
                    } else {
                        mass.nextLooseLaunchTick = now + ACQUISITION_INTERVAL;
                    }
                }
                continue;
            }
            if (!mass.collapsing) {
                mass.collapsing = true;
                releaseLooseTargets(world, mass, true);
                spawnCollapseEffects(world, mass.center);
            }
            if (now >= mass.collapseEndTick) {
                cleanupVisuals(world, mass, true);
                iterator.remove();
            }
        }
        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static boolean acquireLooseTarget(ServerWorld world, ActiveMass mass,
                                              DevourerMassVisualEntity visual) {
        int totalCap = Math.max(1, Config.uniqueEffects.devourer.looseTargetCap);
        int tendrilCap = Math.clamp(Config.uniqueEffects.devourer.looseTendrilCap, 1, 3);
        long incoming = mass.looseTargets.values().stream().filter(target -> !target.stored).count();
        int available = Math.min(totalCap - mass.looseTargets.size(), tendrilCap - (int) incoming);
        if (available <= 0) {
            return false;
        }
        double radius = Math.max(1.0, Config.uniqueEffects.devourer.targetingRadius);
        double vertical = Math.max(1.0, Config.uniqueEffects.devourer.verticalRange);
        Box search = new Box(mass.center.x - radius, mass.center.y - vertical, mass.center.z - radius,
                mass.center.x + radius, mass.center.y + vertical, mass.center.z + radius);
        List<Entity> candidates = new ArrayList<>();
        candidates.addAll(world.getEntitiesByClass(ItemEntity.class, search,
                target -> isAvailableLooseTarget(world, mass, target, radius)));
        candidates.addAll(world.getEntitiesByClass(ExperienceOrbEntity.class, search,
                target -> isAvailableLooseTarget(world, mass, target, radius)));
        Entity target = candidates.stream()
                .sorted(Comparator
                        .comparingInt(DevourerAbilityManager::looseTargetPriority)
                        .thenComparingDouble(candidate -> candidate.squaredDistanceTo(mass.center)))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return false;
        }
        captureLooseTarget(world, mass, visual, target);
        return true;
    }

    private static int looseLaunchDelay(ServerWorld world) {
        return LOOSE_LAUNCH_DELAY_MIN + world.random.nextInt(LOOSE_LAUNCH_DELAY_VARIANCE);
    }

    private static boolean isAvailableLooseTarget(ServerWorld world, ActiveMass mass,
                                                   Entity target, double radius) {
        return target.isAlive() && !target.isRemoved() && target.getWorld() == world
                && !mass.looseTargets.containsKey(target.getUuid()) && !isCapturedLoot(target)
                && horizontalDistanceSquared(target.getPos(), mass.center) <= radius * radius
                && hasLineOfSight(world, mass.center, target);
    }

    private static void captureLooseTarget(ServerWorld world, ActiveMass mass,
                                           DevourerMassVisualEntity visual, Entity target) {
        UUID targetId = target.getUuid();
        long mixed = mix(targetId.getMostSignificantBits() ^ targetId.getLeastSignificantBits());
        double baseAngle = unit(mixed) * MathHelper.TAU;
        double angularSpeed = (0.018 + unit(mix(mixed + 1L)) * 0.010)
                * ((mixed & 1L) == 0L ? 1.0 : -1.0);
        double radialVariation = unit(mix(mixed + 2L));
        double verticalVariation = unit(mix(mixed + 3L)) - 0.5;
        int lifetime = Math.max(1, (int) (mass.collapseEndTick - world.getTime()));
        DevourerTendrilVisualEntity tendril = new DevourerTendrilVisualEntity(
                world, visual, target, lifetime);
        world.spawnEntity(tendril);
        mass.looseTargets.put(targetId, new CapturedLoot(
                targetId, target.hasNoGravity(), target.getPos(), tendril.getUuid(),
                baseAngle, angularSpeed, radialVariation, verticalVariation));
        CAPTURED_LOOT.computeIfAbsent(world, ignored -> new HashSet<>()).add(targetId);
        if (!target.hasNoGravity()) {
            target.addCommandTag(CAPTURED_GRAVITY_TAG);
        }
        target.setNoGravity(true);
    }

    private static boolean tickLooseTargets(ServerWorld world, ActiveMass mass,
                                            DevourerMassVisualEntity visual, long now) {
        double releaseRadius = Math.max(1.0, Config.uniqueEffects.devourer.targetingRadius) + RELEASE_BUFFER;
        double releaseVertical = Math.max(1.0, Config.uniqueEffects.devourer.verticalRange) + RELEASE_BUFFER;
        boolean slotFreed = false;
        Iterator<CapturedLoot> iterator = mass.looseTargets.values().iterator();
        while (iterator.hasNext()) {
            CapturedLoot captured = iterator.next();
            Entity target = resolveLoose(world, captured.targetId);
            DevourerTendrilVisualEntity tendril = resolveTendrilVisual(world, captured.tendrilId);
            if (target == null
                    || !captured.stored && tendril == null
                    || horizontalDistanceSquared(target.getPos(), mass.center) > releaseRadius * releaseRadius
                    || Math.abs(target.getY() - mass.center.y) > releaseVertical) {
                releaseLooseTarget(world, captured, target, tendril, true);
                iterator.remove();
                slotFreed = true;
                continue;
            }
            captured.lastPosition = target.getPos();
            if (!captured.stored && !hasLineOfSight(world, mass.center, target)) {
                captured.blockedTicks++;
                if (captured.blockedTicks >= LOST_SIGHT_GRACE) {
                    releaseLooseTarget(world, captured, target, tendril, true);
                    iterator.remove();
                    slotFreed = true;
                    continue;
                }
            } else if (!captured.stored) {
                captured.blockedTicks = 0;
            }
            target.setNoGravity(true);
            float radius = visual.getStableRadius(0.0F);
            Vec3d slot = captured.stored
                    ? looseStoredSlot(mass.center, radius, captured, target, now)
                    : looseDeliverySlot(mass.center, radius, captured, target);
            boolean delivered = pullLooseTowardSlot(target, slot, !captured.stored);
            if (!captured.stored && delivered) {
                if (corruptWraithfang(world, target, mass.center)) {
                    visual.feed();
                }
                captured.stored = true;
                if (tendril != null) {
                    tendril.discard();
                }
                captured.tendrilId = null;
                spawnLooseDeliveryEffects(world, target, mass.center);
                slotFreed = true;
            }
        }
        return slotFreed;
    }

    private static int looseTargetPriority(Entity entity) {
        return entity instanceof ItemEntity itemEntity
                && itemEntity.getStack().isOf(ItemsRegistry.WRAITHFANG.get()) ? 0 : 1;
    }

    private static boolean corruptWraithfang(ServerWorld world, Entity target, Vec3d center) {
        if (!(target instanceof ItemEntity itemEntity)) {
            return false;
        }
        ItemStack source = itemEntity.getStack();
        if (!source.isOf(ItemsRegistry.WRAITHFANG.get())) {
            return false;
        }
        int level = AwakeningApi.getLevel(source);
        ItemStack result = source.copyComponentsToNewStack(ItemsRegistry.WRAITHMAW.get(), source.getCount());
        result.set(ComponentTypeRegistry.AWAKENING_ROUTE.get(),
                new AwakeningRouteComponent(AwakeningFormRegistry.WRAITHMAW_ROUTE));
        AwakeningApi.setLevel(result, level);
        itemEntity.setStack(result);
        world.spawnParticles(DEVOURER_DUST, center.x, center.y, center.z,
                42, 0.72, 0.72, 0.72, 0.07);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z,
                18, 0.48, 0.48, 0.48, 0.045);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                36, 0.64, 0.64, 0.64, 0.12);
        world.playSound(null, center.x, center.y, center.z, SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.9F, 0.62F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.PLAYERS, 0.72F, 0.82F);
        return true;
    }

    private static Vec3d looseDeliverySlot(Vec3d center, float radius,
                                           CapturedLoot captured, Entity target) {
        double coreRadius = Math.min(0.18, Math.max(0.06, radius * 0.04));
        double vertical = captured.verticalVariation * Math.min(0.2, radius * 0.09)
                - target.getHeight() * 0.55;
        return center.add(Math.cos(captured.baseAngle) * coreRadius, vertical,
                Math.sin(captured.baseAngle) * coreRadius);
    }

    private static Vec3d looseStoredSlot(Vec3d center, float radius, CapturedLoot captured,
                                         Entity target, long now) {
        double angle = captured.baseAngle + now * captured.angularSpeed;
        double orbitRadius = Math.min(0.3,
                Math.max(0.1, radius * (0.045 + captured.radialVariation * 0.025)));
        double vertical = captured.verticalVariation * Math.min(0.28, radius * 0.13)
                - target.getHeight() * 0.55;
        return center.add(Math.cos(angle) * orbitRadius, vertical, Math.sin(angle) * orbitRadius);
    }

    private static boolean pullLooseTowardSlot(Entity target, Vec3d slot, boolean inbound) {
        Vec3d offset = slot.subtract(target.getPos());
        double distance = offset.length();
        double configured = Math.max(0.0, Config.uniqueEffects.devourer.loosePullStrength);
        double speedScale = inbound ? LOOSE_PULL_SPEED_SCALE : 1.0;
        Vec3d current = target.getVelocity();
        Vec3d velocity;
        if (distance <= 0.16) {
            velocity = current.multiply(0.22).add(offset.multiply(0.38 * speedScale));
        } else {
            double strength = Math.min(0.52 * speedScale,
                    configured * speedScale * Math.min(1.45, 0.78 + distance * 0.12));
            velocity = current.multiply(0.4).add(offset.normalize().multiply(strength));
        }
        double maximumSpeed = 0.62 * speedScale;
        double speed = velocity.length();
        if (speed > maximumSpeed) {
            velocity = velocity.multiply(maximumSpeed / speed);
        }
        target.setVelocity(velocity);
        target.velocityModified = true;
        target.velocityDirty = true;
        target.fallDistance = 0.0F;
        return distance <= 0.18 + target.getWidth() * 0.08;
    }

    private static void acquireTargets(ServerWorld world, LivingEntity actor,
                                       ActiveMass mass, DevourerMassVisualEntity visual) {
        int cap = Math.max(1, Config.uniqueEffects.devourer.maxTargets);
        if (mass.targets.size() >= cap) {
            return;
        }
        double radius = Math.max(1.0, Config.uniqueEffects.devourer.targetingRadius);
        double vertical = Math.max(1.0, Config.uniqueEffects.devourer.verticalRange);
        Box search = new Box(mass.center.x - radius, mass.center.y - vertical, mass.center.z - radius,
                mass.center.x + radius, mass.center.y + vertical, mass.center.z + radius);
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, search,
                        target -> !mass.targets.containsKey(target.getUuid())
                                && isValidTarget(world, actor, mass.sourcePlayerId, target)
                                && horizontalDistanceSquared(target.getPos(), mass.center) <= radius * radius
                                && hasLineOfSight(world, mass.center, target))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(mass.center)))
                .limit(cap - mass.targets.size())
                .toList();
        for (LivingEntity target : candidates) {
            int lifetime = Math.max(1, (int) (mass.collapseEndTick - world.getTime()));
            DevourerTendrilVisualEntity tendril = new DevourerTendrilVisualEntity(world, visual, target, lifetime);
            world.spawnEntity(tendril);
            mass.targets.put(target.getUuid(), new CapturedTarget(target.getUuid(), tendril.getUuid(),
                    target.getPos()));
            visual.feed();
            spawnCaptureEffects(world, target);
        }
    }

    private static void tickTargets(ServerWorld world, LivingEntity actor,
                                    ActiveMass mass, DevourerMassVisualEntity visual, long now) {
        boolean damageTick = (now - mass.activeStartTick) % Math.max(1, Config.uniqueEffects.devourer.damageInterval) == 0L;
        boolean fed = false;
        Iterator<CapturedTarget> iterator = mass.targets.values().iterator();
        while (iterator.hasNext()) {
            CapturedTarget captured = iterator.next();
            LivingEntity target = resolveLiving(world, captured.targetId);
            DevourerTendrilVisualEntity tendril = resolveTendrilVisual(world, captured.tendrilId);
            double releaseRadius = Math.max(1.0, Config.uniqueEffects.devourer.targetingRadius) + RELEASE_BUFFER;
            if (target == null || !captured.ingested && tendril == null
                    || !isValidTarget(world, actor, mass.sourcePlayerId, target)
                    || horizontalDistanceSquared(target.getPos(), mass.center) > releaseRadius * releaseRadius) {
                releaseTarget(tendril, target == null ? captured.lastPosition : target.getPos());
                iterator.remove();
                continue;
            }
            captured.lastPosition = target.getPos();
            if (!captured.ingested && !hasLineOfSight(world, mass.center, target)) {
                captured.blockedTicks++;
                if (captured.blockedTicks >= LOST_SIGHT_GRACE) {
                    releaseTarget(tendril, target.getPos());
                    iterator.remove();
                    continue;
                }
            } else if (!captured.ingested) {
                captured.blockedTicks = 0;
            }

            Vec3d slot = holdingSlot(mass.center, visual.getCurrentRadius(0.0F), captured, target);
            boolean held = pullTowardSlot(target, slot);
            if (held && !captured.ingested) {
                captured.ingested = true;
                if (tendril != null) {
                    tendril.discard();
                }
                captured.tendrilId = null;
            }
            if (held && damageTick && damageTarget(world, actor, mass.stackSnapshot, target, mass.damage)) {
                fed = true;
                spawnFeedingEffects(world, target, mass.center);
                pullTowardSlot(target, slot);
            }
        }
        if (fed) {
            visual.feed();
            world.playSound(null, mass.center.x, mass.center.y, mass.center.z,
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 0.55F, 1.15F);
        }
    }

    private static Vec3d holdingSlot(Vec3d center, float radius,
                                     CapturedTarget captured, LivingEntity target) {
        long mixed = captured.targetId.getMostSignificantBits() ^ captured.targetId.getLeastSignificantBits();
        double angle = ((mixed & 0xFFFFL) / 65535.0) * MathHelper.TAU;
        double vertical = (((mixed >>> 16) & 0xFFFFL) / 65535.0 - 0.5)
                * Math.min(0.5, radius * 0.22);
        double core = Math.min(0.4, Math.max(0.14, radius * 0.1 + target.getWidth() * 0.04));
        return center.add(Math.cos(angle) * core, vertical, Math.sin(angle) * core);
    }

    private static boolean pullTowardSlot(LivingEntity target, Vec3d bodySlot) {
        Vec3d desiredBase = bodySlot.subtract(0.0, target.getHeight() * 0.5, 0.0);
        Vec3d offset = desiredBase.subtract(target.getPos());
        double distance = offset.length();
        double size = Math.max(1.0, Math.max(target.getWidth(), target.getHeight()));
        double resistance = MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double response = MathHelper.clamp(1.0 / (size * (1.0 + resistance * 2.0)), 0.12, 1.0);
        double configured = Math.max(0.0, Config.uniqueEffects.devourer.pullStrength);
        if (distance <= 0.34 + target.getWidth() * 0.18) {
            Vec3d hold = offset.multiply(0.28 * response);
            target.setVelocity(hold.x, MathHelper.clamp(hold.y, -0.18, 0.18), hold.z);
            target.velocityModified = true;
            target.fallDistance = 0.0F;
            return true;
        }
        if (distance > 1.0E-4) {
            double strength = Math.min(0.46, configured * response * Math.min(1.8, 0.8 + distance * 0.14));
            Vec3d pull = offset.normalize().multiply(strength);
            Vec3d current = target.getVelocity().multiply(0.38);
            target.setVelocity(current.x + pull.x,
                    MathHelper.clamp(current.y + pull.y, -0.45, 0.55),
                    current.z + pull.z);
            target.velocityModified = true;
            target.fallDistance = 0.0F;
        }
        return false;
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                        LivingEntity target, float baseDamage) {
        if (baseDamage <= 0.0F || !target.isAlive()) {
            return false;
        }
        DamageSource magic = actor.getDamageSources().magic();
        DamageSource source = new DamageSource(magic.getTypeRegistryEntry(), actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, baseDamage);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        return damaged[0];
    }

    private static Vec3d resolveCastCenter(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        ServerWorld world = context.world();
        double range = Math.max(1.0, Config.uniqueEffects.devourer.castRange);
        Vec3d facing = context.facing().lengthSquared() < 1.0E-5
                ? actor.getRotationVec(1.0F) : context.facing().normalize();
        Vec3d start = actor.getEyePos();
        Vec3d end = start.add(facing.multiply(range));
        BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, actor));
        Box search = actor.getBoundingBox().stretch(facing.multiply(range)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(actor, start, end, search,
                entity -> entity instanceof LivingEntity living && living.isAlive()
                        && entity != actor && !entity.isSpectator() && entity.canHit(), range * range);

        Vec3d candidate;
        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || start.squaredDistanceTo(entityHit.getPos()) < start.squaredDistanceTo(blockHit.getPos()))) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            candidate = target.getPos().add(0.0, target.getHeight() * 0.56, 0.0);
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3d normal = Vec3d.of(blockHit.getSide().getVector());
            candidate = blockHit.getPos().add(normal.multiply(0.58));
            if (blockHit.getSide().getOffsetY() > 0) {
                candidate = candidate.add(0.0, 0.62, 0.0);
            }
        } else if (context.target() != null && context.target().isAlive()
                && context.target().squaredDistanceTo(actor) <= range * range) {
            candidate = context.target().getPos().add(0.0, context.target().getHeight() * 0.56, 0.0);
        } else {
            candidate = end;
        }

        Vec3d towardActor = start.subtract(candidate);
        if (towardActor.lengthSquared() > 1.0E-5) {
            towardActor = towardActor.normalize();
        }
        for (int attempt = 0; attempt < 7; attempt++) {
            Vec3d adjusted = candidate.add(towardActor.multiply(attempt * 0.32));
            if (world.isSpaceEmpty(Box.of(adjusted, 1.15, 1.15, 1.15))) {
                return adjusted;
            }
        }
        return null;
    }

    private static boolean hasLineOfSight(ServerWorld world, Vec3d center, Entity target) {
        Vec3d end = targetCenter(target);
        HitResult result = world.raycast(new RaycastContext(center, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, target));
        return result.getType() == HitResult.Type.MISS;
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor, UUID sourcePlayerId,
                                         LivingEntity target) {
        if (target == null || target == actor || !target.isAlive() || target.isRemoved()
                || target.getWorld() != world || !EntityPredicates.VALID_LIVING_ENTITY.test(target)
                || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        LivingEntity sourcePlayer = resolveLiving(world, sourcePlayerId);
        return sourcePlayer == null || target != sourcePlayer
                && HelperMethods.checkAbilityTarget(target, sourcePlayer);
    }

    private static Hand resolveHand(LivingEntity actor, ItemStack stack, Hand preferred) {
        if (preferred != null && actor.getStackInHand(preferred) == stack) {
            return preferred;
        }
        if (actor.getMainHandStack() == stack) {
            return Hand.MAIN_HAND;
        }
        return actor.getOffHandStack() == stack ? Hand.OFF_HAND : null;
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static Entity resolveLoose(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return isLooseTarget(entity) && entity.isAlive() && !entity.isRemoved() ? entity : null;
    }

    private static boolean isLooseTarget(Entity entity) {
        return entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity;
    }

    private static Vec3d targetCenter(Entity target) {
        return target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
    }

    private static long mix(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    private static double unit(long value) {
        return (value & 0x1FFFFFFFFFFFFFL) / (double) 0x20000000000000L;
    }

    private static DevourerMassVisualEntity resolveMassVisual(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof DevourerMassVisualEntity visual ? visual : null;
    }

    private static DevourerTendrilVisualEntity resolveTendrilVisual(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof DevourerTendrilVisualEntity visual ? visual : null;
    }

    private static void releaseTarget(DevourerTendrilVisualEntity tendril, Vec3d endpoint) {
        if (tendril != null) {
            tendril.beginRetraction(endpoint);
        }
    }

    private static void cancel(ServerWorld world, ActiveMass mass, DevourerMassVisualEntity visual) {
        DevourerStainManager.cancel(world, mass.visualId);
        cleanupVisuals(world, mass, false);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void cleanupVisuals(ServerWorld world, ActiveMass mass, boolean discardMass) {
        releaseLooseTargets(world, mass, false);
        for (CapturedTarget captured : new ArrayList<>(mass.targets.values())) {
            DevourerTendrilVisualEntity tendril = resolveTendrilVisual(world, captured.tendrilId);
            if (tendril != null) {
                tendril.discard();
            }
        }
        mass.targets.clear();
        if (discardMass) {
            DevourerMassVisualEntity visual = resolveMassVisual(world, mass.visualId);
            if (visual != null) {
                visual.discard();
            }
        }
    }

    private static void releaseLooseTargets(ServerWorld world, ActiveMass mass, boolean retract) {
        for (CapturedLoot captured : new ArrayList<>(mass.looseTargets.values())) {
            Entity target = resolveLoose(world, captured.targetId);
            DevourerTendrilVisualEntity tendril = resolveTendrilVisual(world, captured.tendrilId);
            releaseLooseTarget(world, captured, target, tendril, retract);
        }
        mass.looseTargets.clear();
    }

    private static void releaseLooseTarget(ServerWorld world, CapturedLoot captured,
                                           Entity target, DevourerTendrilVisualEntity tendril,
                                           boolean retract) {
        if (target != null) {
            target.setNoGravity(captured.originalNoGravity);
            target.removeCommandTag(CAPTURED_GRAVITY_TAG);
        }
        Set<UUID> capturedIds = CAPTURED_LOOT.get(world);
        if (capturedIds != null) {
            capturedIds.remove(captured.targetId);
            if (capturedIds.isEmpty()) {
                CAPTURED_LOOT.remove(world);
            }
        }
        if (tendril != null) {
            if (retract) {
                tendril.beginRetraction(target == null ? captured.lastPosition : targetCenter(target));
            } else {
                tendril.discard();
            }
        }
    }

    private static void spawnCastEffects(ServerWorld world, LivingEntity actor, Vec3d start) {
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(),
                SoundCategory.PLAYERS, 0.95F, 0.62F);
        world.playSound(null, actor.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                SoundCategory.PLAYERS, 0.5F, 1.25F);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, start.x, start.y, start.z,
                10, 0.16, 0.14, 0.16, 0.025);
        world.spawnParticles(DEVOURER_DUST, start.x, start.y, start.z,
                16, 0.2, 0.18, 0.2, 0.03);
    }

    private static void purgeOrphans(ServerWorld world) {
        Set<UUID> activeMassIds = new HashSet<>();
        Set<Integer> activeMassEntityIds = new HashSet<>();
        Set<UUID> activeLootIds = new HashSet<>();
        for (ActiveMass mass : ACTIVE.getOrDefault(world, Map.of()).values()) {
            activeMassIds.add(mass.visualId);
            activeLootIds.addAll(mass.looseTargets.keySet());
            DevourerMassVisualEntity visual = resolveMassVisual(world, mass.visualId);
            if (visual != null) activeMassEntityIds.add(visual.getId());
        }

        List<DevourerMassVisualEntity> orphanMasses = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if ((entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity)
                    && entity.getCommandTags().contains(CAPTURED_GRAVITY_TAG)
                    && !activeLootIds.contains(entity.getUuid())) {
                entity.setNoGravity(false);
                entity.removeCommandTag(CAPTURED_GRAVITY_TAG);
            } else if (entity instanceof DevourerMassVisualEntity mass
                    && !activeMassIds.contains(entity.getUuid())) {
                orphanMasses.add(mass);
            } else if (entity instanceof DevourerTendrilVisualEntity tendril
                    && !activeMassEntityIds.contains(tendril.getMassId())) {
                tendril.discard();
            }
        }

        for (DevourerMassVisualEntity mass : orphanMasses) {
            double radius = Math.max(1.0, mass.getMaximumRadius());
            for (Entity entity : world.getOtherEntities(mass,
                    mass.getBoundingBox().expand(radius), candidate ->
                            (candidate instanceof ItemEntity || candidate instanceof ExperienceOrbEntity)
                                    && candidate.hasNoGravity())) {
                entity.setNoGravity(false);
                entity.removeCommandTag(CAPTURED_GRAVITY_TAG);
            }
            mass.discard();
        }

        Set<UUID> captured = CAPTURED_LOOT.get(world);
        if (captured != null) {
            captured.retainAll(activeLootIds);
            if (captured.isEmpty()) CAPTURED_LOOT.remove(world);
        }
    }

    private static void spawnSeedArrivalEffects(ServerWorld world, Vec3d center) {
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                SoundCategory.PLAYERS, 1.25F, 0.62F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_EMERGE,
                SoundCategory.PLAYERS, 0.65F, 1.35F);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z,
                28, 0.75, 0.75, 0.75, 0.055);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                42, 0.85, 0.85, 0.85, 0.09);
        world.spawnParticles(DEVOURER_DUST, center.x, center.y, center.z,
                36, 0.8, 0.8, 0.8, 0.06);
    }

    private static void spawnCaptureEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS,
                target.getSoundCategory(), 0.75F, 1.2F + world.random.nextFloat() * 0.22F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                target.getSoundCategory(), 0.55F, 0.72F + world.random.nextFloat() * 0.15F);
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP, pos.x, pos.y, pos.z,
                8, 0.3, 0.35, 0.3, 0.02);
        world.spawnParticles(DEVOURER_DUST, pos.x, pos.y, pos.z,
                12, 0.35, 0.4, 0.35, 0.025);
    }

    private static void spawnFeedingEffects(ServerWorld world, LivingEntity target, Vec3d center) {
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z,
                4, 0.22, 0.25, 0.22, 0.02);
        world.spawnParticles(DEVOURER_DUST, pos.x, pos.y, pos.z,
                7, 0.24, 0.28, 0.24, 0.025);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                5, 0.25, 0.25, 0.25, 0.035);
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.DARK_SWORD_ATTACK_02.get(),
                target.getSoundCategory(), 0.32F, 0.72F + world.random.nextFloat() * 0.16F);
    }

    private static void spawnLooseDeliveryEffects(ServerWorld world, Entity target, Vec3d center) {
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                4, 0.18, 0.18, 0.18, 0.025);
        world.spawnParticles(DEVOURER_DUST, center.x, center.y, center.z,
                5, 0.2, 0.2, 0.2, 0.02);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                target.getSoundCategory(), 0.26F, 0.82F + world.random.nextFloat() * 0.14F);
    }

    private static void spawnCollapseEffects(ServerWorld world, Vec3d center) {
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                SoundCategory.PLAYERS, 0.58F, 0.55F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_SLIME_SQUISH,
                SoundCategory.PLAYERS, 0.9F, 0.48F);
        world.playSound(null, center.x, center.y, center.z, SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.7F, 0.72F);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                34, 0.8, 0.8, 0.8, 0.12);
        world.spawnParticles(DEVOURER_DUST, center.x, center.y, center.z,
                30, 0.75, 0.75, 0.75, 0.07);
    }

    private static final class ActiveMass {
        private final UUID actorId;
        private final UUID sourcePlayerId;
        private final Hand hand;
        private final ItemStack stackReference;
        private final ItemStack stackSnapshot;
        private final Vec3d center;
        private final long seedArrivalTick;
        private final long activeStartTick;
        private final long activeEndTick;
        private final long collapseEndTick;
        private final float damage;
        private final UUID visualId;
        private final Map<UUID, CapturedTarget> targets = new HashMap<>();
        private final Map<UUID, CapturedLoot> looseTargets = new HashMap<>();
        private long nextLooseLaunchTick;
        private boolean bloomed;
        private boolean collapsing;

        private ActiveMass(UUID actorId, UUID sourcePlayerId, Hand hand,
                           ItemStack stackReference, ItemStack stackSnapshot, Vec3d center,
                           long seedArrivalTick, long activeStartTick,
                           long activeEndTick, long collapseEndTick, float damage, UUID visualId) {
            this.actorId = actorId;
            this.sourcePlayerId = sourcePlayerId;
            this.hand = hand;
            this.stackReference = stackReference;
            this.stackSnapshot = stackSnapshot;
            this.center = center;
            this.seedArrivalTick = seedArrivalTick;
            this.activeStartTick = activeStartTick;
            this.nextLooseLaunchTick = activeStartTick;
            this.activeEndTick = activeEndTick;
            this.collapseEndTick = collapseEndTick;
            this.damage = damage;
            this.visualId = visualId;
        }
    }

    private static final class CapturedTarget {
        private final UUID targetId;
        private UUID tendrilId;
        private Vec3d lastPosition;
        private int blockedTicks;
        private boolean ingested;

        private CapturedTarget(UUID targetId, UUID tendrilId, Vec3d lastPosition) {
            this.targetId = targetId;
            this.tendrilId = tendrilId;
            this.lastPosition = lastPosition;
        }
    }

    private static final class CapturedLoot {
        private final UUID targetId;
        private final boolean originalNoGravity;
        private final double baseAngle;
        private final double angularSpeed;
        private final double radialVariation;
        private final double verticalVariation;
        private UUID tendrilId;
        private Vec3d lastPosition;
        private int blockedTicks;
        private boolean stored;

        private CapturedLoot(UUID targetId, boolean originalNoGravity, Vec3d lastPosition,
                             UUID tendrilId, double baseAngle, double angularSpeed,
                             double radialVariation, double verticalVariation) {
            this.targetId = targetId;
            this.originalNoGravity = originalNoGravity;
            this.lastPosition = lastPosition;
            this.tendrilId = tendrilId;
            this.baseAngle = baseAngle;
            this.angularSpeed = angularSpeed;
            this.radialVariation = radialVariation;
            this.verticalVariation = verticalVariation;
        }
    }

    public record ReprisalRedirect(Vec3d destination, UUID tendrilId) {
    }
}
