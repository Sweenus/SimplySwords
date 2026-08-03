package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.CaelestisBreachCreature;
import net.sweenus.simplyswords.entity.CaelestisBreachVisualEntity;
import net.sweenus.simplyswords.entity.CaelestisDreadglareEntity;
import net.sweenus.simplyswords.entity.CaelestisHollowEntity;
import net.sweenus.simplyswords.entity.CaelestisRiftlingEntity;
import net.sweenus.simplyswords.entity.CaelestisTentacleEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.MinionTargeting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CaelestisBreachManager {

    private static final float START_RADIUS = 1.5F;
    private static final int GROUND_SCAN_UP = 6;
    private static final int GROUND_SCAN_DOWN = 12;
    private static final int SPAWN_ATTEMPTS = 12;
    private static final int CREATURE_TARGET_INTERVAL = 10;
    private static final int TENTACLE_CONTACT_INTERVAL = 5;
    private static final int TENTACLE_SPAWN_ATTEMPTS = 18;
    private static final int AGGRO_PULSE_INTERVAL = 40;
    private static final double AGGRO_REDIRECT_RADIUS = 10.0;
    private static final String VISUAL_TAG = "simplyswords_caelestis_breach_visual";
    private static final String UNBOUND_REWARD_ROLLED_TAG =
            "simplyswords_caelestis_unbound_reward_rolled";
    private static final Map<ServerWorld, Map<UUID, ActiveBreach>> ACTIVE = new HashMap<>();

    private CaelestisBreachManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveBreach> breaches = ACTIVE.get(world);
        return breaches != null && !breaches.isEmpty() || world.getTime() % 80L == 0L;
    }

    public static boolean isActive(net.minecraft.world.World world, UUID breachId) {
        if (!(world instanceof ServerWorld serverWorld) || breachId == null) {
            return false;
        }
        Map<UUID, ActiveBreach> breaches = ACTIVE.get(serverWorld);
        return breaches != null && breaches.containsKey(breachId);
    }

    public static boolean hasActiveForActor(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveBreach> breaches = ACTIVE.get(world);
        if (breaches == null || actorId == null) {
            return false;
        }
        return breaches.values().stream().anyMatch(breach -> actorId.equals(breach.actorId));
    }

    public static boolean start(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null
                || !context.actor().isAlive() || context.stack() == null || context.stack().isEmpty()
                || hasActiveForActor(context.world(), context.actor().getUuid())) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        LivingEntity principal = context.sourcePlayer() == null ? actor : context.sourcePlayer();
        UUID breachId = UUID.randomUUID();
        int seed = world.random.nextInt();
        Vec3d center = findAnchorPosition(world, context.origin());
        long now = world.getTime();
        BreachProfile profile = BreachProfile.capture(actor instanceof PlayerEntity);
        int duration = profile.duration;
        boolean betrayalPending = world.random.nextInt(100)
                < Math.clamp(Config.uniqueEffects.caelestis.betrayalChance, 0, 100);
        long betrayalTick = now + getBetrayalDelay(world, profile);
        float baseDamage = HelperMethods.abilityScaledDamage(
                "eldritch",
                actor,
                context.stack(),
                Config.uniqueEffects.caelestis.minionDamageScaling,
                Config.uniqueEffects.caelestis.minionSpellScaling
        ) * profile.damageMultiplier;
        float ownerAttackValue = HelperMethods.attackScaledDamage(
                actor, context.stack(), 1.0F);

        CaelestisBreachVisualEntity visual = new CaelestisBreachVisualEntity(
                world, breachId, center.x, center.y, center.z, seed);
        visual.setMaxRadius(profile.maxRadius);
        visual.setVerticalRange(profile.verticalRange);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);

        ActiveBreach breach = new ActiveBreach(
                breachId,
                actor.getUuid(),
                principal.getUuid(),
                context.stack().copy(),
                center,
                now,
                now + duration,
                now + 20L,
                now + Math.max(1, Config.uniqueEffects.caelestis.tentacleSpawnInterval),
                betrayalTick,
                betrayalPending,
                false,
                visual.getUuid(),
                baseDamage,
                ownerAttackValue,
                seed,
                profile,
                new HashSet<>(),
                new HashSet<>()
        );
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(breachId, breach);
        spawnOpeningEffects(world, center);
        return true;
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveBreach> breaches = ACTIVE.get(world);
        if (breaches == null || breaches.isEmpty()) {
            if (world.getTime() % 80L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        Iterator<ActiveBreach> iterator = breaches.values().iterator();
        while (iterator.hasNext()) {
            ActiveBreach breach = iterator.next();
            if (tickBreach(world, breach)) {
                close(world, breach);
                iterator.remove();
            }
        }
        if (breaches.isEmpty()) {
            ACTIVE.remove(world);
            purgeOrphanVisuals(world);
        }
    }

    private static boolean tickBreach(ServerWorld world, ActiveBreach breach) {
        Entity actorEntity = world.getEntity(breach.actorId);
        if (!(actorEntity instanceof LivingEntity actor) || !actor.isAlive()
                || !world.isChunkLoaded(ChunkPos.toLong(BlockPos.ofFloored(breach.center)))) {
            return true;
        }

        long now = world.getTime();
        if (!breach.forcedCollapse && now >= breach.endTick) {
            return true;
        }
        if (!breach.forcedCollapse
                && actor instanceof PlayerEntity
                && !isWieldingCaelestis(actor)) {
            beginForcedCollapse(world, breach, now);
        }
        if (breach.forcedCollapse && now >= breach.forcedCollapseEndTick) {
            return true;
        }

        float radius = radiusAt(now, breach);
        int phase = phaseAt(now, breach);
        updateVisual(world, breach, radius, phase);
        cleanCreatureIds(world, breach);
        cleanTentacleIds(world, breach);

        if (phase != CaelestisBreachVisualEntity.PHASE_COLLAPSING
                && now >= breach.nextSpawnTick) {
            spawnWave(world, breach);
            breach.nextSpawnTick = now + Math.max(1, Config.uniqueEffects.caelestis.spawnInterval);
        }
        if (phase != CaelestisBreachVisualEntity.PHASE_COLLAPSING
                && now >= breach.nextTentacleSpawnTick) {
            spawnTentacle(world, breach, radius);
            breach.nextTentacleSpawnTick = now
                    + Math.max(1, Config.uniqueEffects.caelestis.tentacleSpawnInterval);
        }
        if (breach.betrayalPending && !breach.betrayalSpawned
                && phase != CaelestisBreachVisualEntity.PHASE_COLLAPSING
                && now >= breach.betrayalTick) {
            breach.betrayalSpawned = spawnUnbound(world, breach);
        }
        if (!breach.forcedCollapse
                && Math.floorMod(now + breach.seed, AGGRO_PULSE_INTERVAL) == 0L) {
            redirectAggroToBoundMinions(world, breach);
        }

        if (phase == CaelestisBreachVisualEntity.PHASE_COLLAPSING) {
            sweepCollapsingPerimeter(world, breach, radius);
            sweepCollapsingTentacles(world, breach, radius);
        }
        if (now % 5L == 0L) {
            spawnBoundaryParticles(world, breach.center, radius, phase, breach.seed);
        }
        if (now % 12L == 0L) {
            spawnInteriorMotes(world, breach, radius);
        }
        if (now % 60L == 0L) {
            playAmbientPulse(world, breach.center, phase);
        }
        return false;
    }

    private static void updateVisual(ServerWorld world, ActiveBreach breach, float radius, int phase) {
        Entity entity = world.getEntity(breach.visualId);
        if (entity instanceof CaelestisBreachVisualEntity visual) {
            visual.setRadius(radius);
            visual.setPhase(phase);
        }
    }

    private static void spawnWave(ServerWorld world, ActiveBreach breach) {
        int cap = breach.profile.maxMinions;
        int friendlyCount = countFriendlyCreatures(world, breach);
        if (friendlyCount >= cap) {
            return;
        }
        int min = breach.profile.minSpawnPerWave;
        int max = breach.profile.maxSpawnPerWave;
        int count = min + world.random.nextInt(max - min + 1);
        count = Math.min(count, cap - friendlyCount);
        float radius = radiusAt(world.getTime(), breach);
        for (int i = 0; i < count; i++) {
            spawnCreature(world, breach, radius, chooseArchetype(world), false);
        }
    }

    private static boolean spawnUnbound(ServerWorld world, ActiveBreach breach) {
        float radius = radiusAt(world.getTime(), breach);
        int archetype = world.random.nextBoolean() ? 1 : 2;
        return spawnCreature(world, breach, radius, archetype, true);
    }

    private static int chooseArchetype(ServerWorld world) {
        int riftling = Math.max(0, Config.uniqueEffects.caelestis.riftlingWeight);
        int hollow = Math.max(0, Config.uniqueEffects.caelestis.hollowWeight);
        int dreadglare = Math.max(0, Config.uniqueEffects.caelestis.dreadglareWeight);
        int total = riftling + hollow + dreadglare;
        if (total <= 0) {
            return 0;
        }
        int roll = world.random.nextInt(total);
        if (roll < riftling) {
            return 0;
        }
        return roll < riftling + hollow ? 1 : 2;
    }

    private static boolean spawnCreature(ServerWorld world, ActiveBreach breach, float radius,
                                         int archetype, boolean unbound) {
        MobEntity mob = createCreature(world, archetype);
        if (!(mob instanceof CaelestisBreachCreature creature)) {
            return false;
        }

        Vec3d pos = findSpawnPosition(world, breach.center, radius, mob, breach.profile);
        if (pos == null) {
            return false;
        }

        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, world.random.nextFloat() * 360.0F, 0.0F);
        if (!world.isSpaceEmpty(mob, mob.getBoundingBox())) {
            return false;
        }
        creature.configureBreachCreature(
                breach.id, breach.actorId, breach.principalId, unbound, world.random.nextInt());
        applyScaledCreatureHealth(mob, breach.ownerAttackValue);
        mob.setPersistent();
        if (!world.spawnEntity(mob)) {
            return false;
        }

        breach.creatureIds.add(mob.getUuid());
        spawnCreatureArrivalEffects(world, mob, unbound);
        return true;
    }

    private static MobEntity createCreature(ServerWorld world, int archetype) {
        return switch (archetype) {
            case 1 -> new CaelestisHollowEntity(EntityRegistry.CAELESTIS_HOLLOW.get(), world);
            case 2 -> new CaelestisDreadglareEntity(EntityRegistry.CAELESTIS_DREADGLARE.get(), world);
            default -> new CaelestisRiftlingEntity(EntityRegistry.CAELESTIS_RIFTLING.get(), world);
        };
    }

    private static void spawnTentacle(ServerWorld world, ActiveBreach breach, float radius) {
        if (breach.profile.maxTentacles <= 0
                || countActiveTentacles(world, breach) >= breach.profile.maxTentacles) {
            return;
        }

        int size = chooseTentacleSize(world);
        int seed = world.random.nextInt();
        CaelestisTentacleEntity tentacle = new CaelestisTentacleEntity(
                world, breach.id, 0.0, 0.0, 0.0, size, seed);
        Vec3d position = findTentaclePosition(world, breach, radius, tentacle);
        if (position == null) {
            return;
        }
        tentacle.refreshPositionAndAngles(
                position.x, position.y, position.z, Math.floorMod(seed, 360), 0.0F);
        if (!world.spawnEntity(tentacle)) {
            return;
        }
        breach.tentacleIds.add(tentacle.getUuid());
        world.spawnParticles(
                ParticleTypes.REVERSE_PORTAL,
                position.x, position.y + 0.2, position.z,
                18, tentacle.getContactRadius() * 0.65, 0.18,
                tentacle.getContactRadius() * 0.65, 0.045);
        world.spawnParticles(
                ParticleTypes.CRIMSON_SPORE,
                position.x, position.y + 0.12, position.z,
                7, tentacle.getContactRadius() * 0.45, 0.12,
                tentacle.getContactRadius() * 0.45, 0.02);
    }

    private static int chooseTentacleSize(ServerWorld world) {
        int roll = world.random.nextInt(100);
        if (roll < 45) {
            return CaelestisTentacleEntity.SIZE_SMALL;
        }
        return roll < 80
                ? CaelestisTentacleEntity.SIZE_MEDIUM
                : CaelestisTentacleEntity.SIZE_LARGE;
    }

    private static Vec3d findTentaclePosition(ServerWorld world, ActiveBreach breach, float radius,
                                              CaelestisTentacleEntity tentacle) {
        double outerRadius = radius - tentacle.getContactRadius() - 0.65;
        if (outerRadius < 0.7) {
            return null;
        }
        double innerRadius = Math.min(2.0, outerRadius * 0.42);
        for (int attempt = 0; attempt < TENTACLE_SPAWN_ATTEMPTS; attempt++) {
            double angle = world.random.nextDouble() * MathHelper.TAU;
            double distance = Math.sqrt(MathHelper.lerp(
                    world.random.nextDouble(),
                    innerRadius * innerRadius,
                    outerRadius * outerRadius));
            double x = breach.center.x + Math.cos(angle) * distance;
            double z = breach.center.z + Math.sin(angle) * distance;
            BlockPos column = BlockPos.ofFloored(x, breach.center.y, z);
            if (!world.getWorldBorder().contains(column)
                    || !world.isChunkLoaded(ChunkPos.toLong(column))) {
                continue;
            }
            Vec3d ground = findGround(world, x, z, breach.center.y);
            if (ground == null || !hasTentacleSpacing(world, breach, ground, tentacle)) {
                continue;
            }
            return ground;
        }
        return null;
    }

    private static boolean hasTentacleSpacing(ServerWorld world, ActiveBreach breach, Vec3d position,
                                              CaelestisTentacleEntity candidate) {
        for (UUID tentacleId : breach.tentacleIds) {
            Entity entity = world.getEntity(tentacleId);
            if (!(entity instanceof CaelestisTentacleEntity other) || other.isRetracting()) {
                continue;
            }
            double required = candidate.getContactRadius() + other.getContactRadius() + 0.9;
            double dx = position.x - other.getX();
            double dz = position.z - other.getZ();
            if (dx * dx + dz * dz < required * required) {
                return false;
            }
        }
        return true;
    }

    private static void applyScaledCreatureHealth(MobEntity mob, float ownerAttackValue) {
        EntityAttributeInstance health = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null) {
            return;
        }
        double scaling = Math.max(0.0F, Config.uniqueEffects.caelestis.minionHealthScaling);
        double multiplier = 1.0 + Math.max(0.0F, ownerAttackValue) * scaling;
        health.setBaseValue(health.getBaseValue() * multiplier);
        mob.setHealth(mob.getMaxHealth());
    }

    private static Vec3d findSpawnPosition(ServerWorld world, Vec3d center, float radius, MobEntity mob,
                                           BreachProfile profile) {
        double spawnRadius = Math.max(1.0, radius - 0.8);
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double jitter = (world.random.nextDouble() - 0.5) * 1.1;
            double x = center.x + Math.cos(angle) * (spawnRadius + jitter);
            double z = center.z + Math.sin(angle) * (spawnRadius + jitter);
            BlockPos column = BlockPos.ofFloored(x, center.y, z);
            if (!world.getWorldBorder().contains(column)
                    || !world.isChunkLoaded(ChunkPos.toLong(column))) {
                continue;
            }
            Vec3d ground = findGround(world, x, z, center.y);
            if (ground == null) {
                continue;
            }
            if (mob instanceof CaelestisDreadglareEntity) {
                double verticalRange = profile.verticalRange;
                double desiredHeight = Math.min(
                        center.y + verticalRange - 0.75,
                        ground.y + 3.0 + world.random.nextDouble() * 1.5);
                Vec3d airborne = new Vec3d(ground.x, desiredHeight, ground.z);
                mob.setPosition(airborne.x, airborne.y, airborne.z);
                if (world.isSpaceEmpty(mob, mob.getBoundingBox())) {
                    return airborne;
                }
                continue;
            }
            mob.setPosition(ground.x, ground.y, ground.z);
            if (world.isSpaceEmpty(mob, mob.getBoundingBox())) {
                return ground;
            }
        }
        return null;
    }

    private static Vec3d findAnchorPosition(ServerWorld world, Vec3d origin) {
        Vec3d ground = findGround(world, origin.x, origin.z, origin.y);
        return ground == null ? origin : ground;
    }

    private static Vec3d findGround(ServerWorld world, double x, double z, double referenceY) {
        int startY = Math.min(world.getTopY() - 1, MathHelper.floor(referenceY) + GROUND_SCAN_UP);
        int endY = Math.max(world.getBottomY() + 1, MathHelper.floor(referenceY) - GROUND_SCAN_DOWN);
        BlockPos.Mutable cursor = new BlockPos.Mutable(MathHelper.floor(x), startY, MathHelper.floor(z));
        for (int y = startY; y >= endY; y--) {
            cursor.setY(y);
            BlockPos below = cursor.down();
            if (!world.getBlockState(below).isSolidBlock(world, below)
                    || !world.getBlockState(cursor).getCollisionShape(world, cursor).isEmpty()
                    || !world.getBlockState(cursor.up()).getCollisionShape(world, cursor.up()).isEmpty()) {
                continue;
            }
            return new Vec3d(x, y, z);
        }
        return null;
    }

    public static boolean tickCreature(MobEntity mob, CaelestisBreachCreature creature) {
        if (!(mob.getWorld() instanceof ServerWorld world) || creature.getBreachId() == null) {
            return false;
        }
        ActiveBreach breach = get(world, creature.getBreachId());
        if (breach == null || breach.forcedCollapse
                || world.getTime() >= breach.endTick) {
            dissolveCreature(world, mob, creature.isUnbound());
            return false;
        }

        float radius = radiusAt(world.getTime(), breach);
        boolean collapsing = phaseAt(world.getTime(), breach)
                == CaelestisBreachVisualEntity.PHASE_COLLAPSING;
        double dx = mob.getX() - breach.center.x;
        double dz = mob.getZ() - breach.center.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double edgeDistance = distance + Math.max(0.2, mob.getWidth() * 0.5);
        if (collapsing && edgeDistance >= radius) {
            dissolveCreature(world, mob, creature.isUnbound());
            return false;
        }

        if (!collapsing && edgeDistance > Math.max(0.5, radius - 0.25)) {
            Vec3d inward = new Vec3d(breach.center.x - mob.getX(), 0.0, breach.center.z - mob.getZ());
            if (inward.horizontalLengthSquared() > 0.0001) {
                inward = inward.normalize();
                mob.setVelocity(mob.getVelocity().multiply(0.35, 1.0, 0.35).add(inward.multiply(0.22)));
                if (edgeDistance > radius + 0.75) {
                    double safeRadius = Math.max(0.5, radius - mob.getWidth() * 0.5 - 0.35);
                    mob.refreshPositionAndAngles(
                            breach.center.x - inward.x * safeRadius,
                            mob.getY(),
                            breach.center.z - inward.z * safeRadius,
                            mob.getYaw(),
                            mob.getPitch());
                }
            }
            mob.setTarget(null);
        }

        double vertical = mob.getY() - breach.center.y;
        double verticalRange = breach.profile.verticalRange;
        if (Math.abs(vertical) > verticalRange) {
            mob.setVelocity(mob.getVelocity().add(0.0, Math.copySign(0.16, -vertical), 0.0));
            mob.setTarget(null);
        }

        if (mob.age % CREATURE_TARGET_INTERVAL == Math.floorMod(mob.getId(), CREATURE_TARGET_INTERVAL)) {
            updateCreatureTarget(world, breach, mob, creature);
        } else if (mob.getTarget() != null && !isValidCreatureTarget(world, breach, creature, mob.getTarget())) {
            mob.setTarget(null);
        }
        return true;
    }

    public static boolean tickTentacle(CaelestisTentacleEntity tentacle) {
        if (!(tentacle.getWorld() instanceof ServerWorld world)
                || tentacle.getBreachId() == null) {
            return false;
        }
        ActiveBreach breach = get(world, tentacle.getBreachId());
        if (breach == null || breach.forcedCollapse
                || world.getTime() >= breach.endTick) {
            return false;
        }

        float radius = radiusAt(world.getTime(), breach);
        double dx = tentacle.getX() - breach.center.x;
        double dz = tentacle.getZ() - breach.center.z;
        double edgeDistance = Math.sqrt(dx * dx + dz * dz) + tentacle.getContactRadius();
        if (phaseAt(world.getTime(), breach)
                == CaelestisBreachVisualEntity.PHASE_COLLAPSING
                && edgeDistance >= radius) {
            return false;
        }

        if (tentacle.age % TENTACLE_CONTACT_INTERVAL
                == Math.floorMod(tentacle.getId(), TENTACLE_CONTACT_INTERVAL)) {
            applyTentacleContactSlow(world, breach, tentacle);
        }
        return true;
    }

    private static void applyTentacleContactSlow(ServerWorld world, ActiveBreach breach,
                                                 CaelestisTentacleEntity tentacle) {
        float radius = tentacle.getContactRadius();
        Box contactBox = new Box(
                tentacle.getX() - radius,
                tentacle.getY(),
                tentacle.getZ() - radius,
                tentacle.getX() + radius,
                tentacle.getY() + tentacle.getTentacleHeight(),
                tentacle.getZ() + radius
        );
        LivingEntity actor = getLiving(world, breach.actorId);
        LivingEntity principal = getLiving(world, breach.principalId);
        LivingEntity source = principal == null ? actor : principal;
        boolean touched = false;
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                contactBox,
                candidate -> candidate.isAlive()
                        && candidate.getBoundingBox().intersects(contactBox)
                        && isTentacleHostile(world, breach, candidate))) {
            int duration = Math.max(1, Config.uniqueEffects.caelestis.tentacleSlowDuration);
            int amplifier = Math.clamp(
                    Config.uniqueEffects.caelestis.tentacleSlowAmplifier, 0, 4);
            target.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SLOWNESS, duration, amplifier, false, false, true),
                    source
            );
            touched = true;
        }

        tentacle.setContactIntensity(touched
                ? 1.0F
                : Math.max(0.0F, tentacle.getContactIntensity() - 0.35F));
        if (touched) {
            world.spawnParticles(
                    ParticleTypes.WITCH,
                    tentacle.getX(),
                    tentacle.getY() + Math.min(1.0F, tentacle.getTentacleHeight() * 0.35F),
                    tentacle.getZ(),
                    2, radius * 0.4, 0.25, radius * 0.4, 0.0);
        }
    }

    private static boolean isTentacleHostile(ServerWorld world, ActiveBreach breach,
                                             LivingEntity target) {
        if (target instanceof CaelestisBreachCreature creature) {
            return breach.id.equals(creature.getBreachId()) && creature.isUnbound();
        }
        return isValidBoundTarget(world, breach, target);
    }

    private static void updateCreatureTarget(ServerWorld world, ActiveBreach breach, MobEntity mob,
                                             CaelestisBreachCreature creature) {
        LivingEntity current = mob.getTarget();
        if (current != null && isValidCreatureTarget(world, breach, creature, current)) {
            return;
        }
        Box search = new Box(
                breach.center.x - breach.profile.maxRadius,
                breach.center.y - breach.profile.verticalRange,
                breach.center.z - breach.profile.maxRadius,
                breach.center.x + breach.profile.maxRadius,
                breach.center.y + breach.profile.verticalRange,
                breach.center.z + breach.profile.maxRadius
        );
        LivingEntity target = world.getEntitiesByClass(
                        LivingEntity.class,
                        search,
                        candidate -> candidate != mob
                                && isValidCreatureTarget(world, breach, creature, candidate))
                .stream()
                .min(Comparator.<LivingEntity>comparingInt(candidate -> targetPriority(breach, creature, candidate))
                        .thenComparingDouble(candidate -> candidate.squaredDistanceTo(mob)))
                .orElse(null);
        mob.setTarget(target);
    }

    private static int targetPriority(ActiveBreach breach, CaelestisBreachCreature creature,
                                      LivingEntity target) {
        if (creature.isUnbound()) {
            return target.getUuid().equals(breach.actorId) ? 0 : 1;
        }
        if (target instanceof CaelestisBreachCreature other && other.isUnbound()) {
            return 0;
        }
        if (target.getUuid().equals(breach.actorId) || target.getUuid().equals(breach.principalId)) {
            return 5;
        }
        return HelperMethods.isMonsterFaction(target) ? 1 : 2;
    }

    private static boolean isValidCreatureTarget(ServerWorld world, ActiveBreach breach,
                                                 CaelestisBreachCreature creature, LivingEntity target) {
        if (target == null || !target.isAlive() || !isInside(breach, target)) {
            return false;
        }
        if (creature.isUnbound()) {
            if (target.getUuid().equals(breach.actorId)) {
                return true;
            }
            return target instanceof CaelestisBreachCreature other
                    && breach.id.equals(other.getBreachId()) && !other.isUnbound();
        }

        if (target instanceof CaelestisBreachCreature other) {
            return breach.id.equals(other.getBreachId()) && other.isUnbound();
        }

        return isValidBoundTarget(world, breach, target);
    }

    private static boolean isValidBoundTarget(ServerWorld world, ActiveBreach breach,
                                              LivingEntity target) {
        if (target == null || !target.isAlive() || !isInside(breach, target)) {
            return false;
        }
        LivingEntity actor = getLiving(world, breach.actorId);
        LivingEntity principal = getLiving(world, breach.principalId);
        LivingEntity allegiance = principal == null ? actor : principal;
        if (allegiance == null || target == actor || target == principal
                || !HelperMethods.checkAbilityTarget(target, allegiance)) {
            return false;
        }
        if (HelperMethods.isMonsterFaction(target)) {
            return true;
        }
        if (actor instanceof MobEntity mobActor && mobActor.getTarget() == target) {
            return true;
        }
        return MinionTargeting.getRecentAttackTarget(world, allegiance) == target
                || MinionTargeting.isMarkedTarget(world, allegiance, target);
    }

    public static boolean tryCreatureAttack(MobEntity mob, Entity targetEntity) {
        if (!(mob instanceof CaelestisBreachCreature creature)
                || !(targetEntity instanceof LivingEntity target)
                || !(mob.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        ActiveBreach breach = get(world, creature.getBreachId());
        if (breach == null || breach.forcedCollapse
                || !isValidCreatureTarget(world, breach, creature, target)) {
            return false;
        }

        float multiplier = archetypeDamageMultiplier(mob);
        if (creature.isUnbound()) {
            multiplier *= Math.max(0.0F, Config.uniqueEffects.caelestis.unboundDamageMultiplier);
            boolean damaged = target.damage(world.getDamageSources().mobAttack(mob),
                    Math.max(0.0F, breach.baseDamage * multiplier));
            if (damaged) {
                playCreatureAttack(world, mob, true);
            }
            return damaged;
        }

        LivingEntity actor = getLiving(world, breach.actorId);
        LivingEntity principal = getLiving(world, breach.principalId);
        LivingEntity attacker = principal == null ? actor : principal;
        if (attacker == null) {
            return false;
        }
        DamageSource source = world.getDamageSources().indirectMagic(mob, attacker);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world, breach.stack, target, source, breach.baseDamage * multiplier);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        if (damaged[0]) {
            playCreatureAttack(world, mob, false);
            if (target instanceof MobEntity targetMob) {
                targetMob.setTarget(mob);
            }
        }
        return damaged[0];
    }

    public static boolean shouldIgnoreCreatureDamage(MobEntity mob, CaelestisBreachCreature creature,
                                                     DamageSource source) {
        Entity attacker = source == null ? null : source.getAttacker();
        if (attacker == null || creature.isUnbound()) {
            return false;
        }
        if (attacker.getUuid().equals(creature.getBreachActorId())
                || attacker.getUuid().equals(creature.getBreachPrincipalId())) {
            return true;
        }
        if (attacker instanceof CaelestisBreachCreature other
                && creature.getBreachId() != null
                && creature.getBreachId().equals(other.getBreachId())) {
            return !other.isUnbound();
        }
        if (attacker instanceof LivingEntity living) {
            LivingEntity principal = mob.getWorld() instanceof ServerWorld world
                    ? getLiving(world, creature.getBreachPrincipalId()) : null;
            return principal != null && !HelperMethods.checkFriendlyFire(living, principal);
        }
        return false;
    }

    private static float archetypeDamageMultiplier(MobEntity mob) {
        if (mob instanceof CaelestisHollowEntity) {
            return 1.20F;
        }
        if (mob instanceof CaelestisDreadglareEntity) {
            return 0.85F;
        }
        return 0.55F;
    }

    private static boolean isInside(ActiveBreach breach, LivingEntity entity) {
        double dx = entity.getX() - breach.center.x;
        double dz = entity.getZ() - breach.center.z;
        float radius = radiusAt(entity.getWorld().getTime(), breach);
        return dx * dx + dz * dz <= radius * radius
                && Math.abs(entity.getY() - breach.center.y)
                <= breach.profile.verticalRange;
    }

    private static void sweepCollapsingPerimeter(ServerWorld world, ActiveBreach breach, float radius) {
        for (UUID creatureId : List.copyOf(breach.creatureIds)) {
            Entity entity = world.getEntity(creatureId);
            if (!(entity instanceof MobEntity mob)
                    || !(entity instanceof CaelestisBreachCreature creature)) {
                continue;
            }
            double dx = mob.getX() - breach.center.x;
            double dz = mob.getZ() - breach.center.z;
            double edgeDistance = Math.sqrt(dx * dx + dz * dz) + Math.max(0.2, mob.getWidth() * 0.5);
            if (edgeDistance >= radius) {
                dissolveCreature(world, mob, creature.isUnbound());
            }
        }
    }

    private static void sweepCollapsingTentacles(ServerWorld world, ActiveBreach breach, float radius) {
        for (UUID tentacleId : List.copyOf(breach.tentacleIds)) {
            Entity entity = world.getEntity(tentacleId);
            if (!(entity instanceof CaelestisTentacleEntity tentacle) || tentacle.isRetracting()) {
                continue;
            }
            double dx = tentacle.getX() - breach.center.x;
            double dz = tentacle.getZ() - breach.center.z;
            double edgeDistance = Math.sqrt(dx * dx + dz * dz) + tentacle.getContactRadius();
            if (edgeDistance >= radius) {
                tentacle.beginRetraction();
            }
        }
    }

    private static int countFriendlyCreatures(ServerWorld world, ActiveBreach breach) {
        int count = 0;
        for (UUID creatureId : breach.creatureIds) {
            Entity entity = world.getEntity(creatureId);
            if (entity instanceof LivingEntity living && living.isAlive()
                    && entity instanceof CaelestisBreachCreature creature && !creature.isUnbound()) {
                count++;
            }
        }
        return count;
    }

    private static int countActiveTentacles(ServerWorld world, ActiveBreach breach) {
        int count = 0;
        for (UUID tentacleId : breach.tentacleIds) {
            Entity entity = world.getEntity(tentacleId);
            if (entity instanceof CaelestisTentacleEntity tentacle && !tentacle.isRetracting()) {
                count++;
            }
        }
        return count;
    }

    private static void redirectAggroToBoundMinions(ServerWorld world, ActiveBreach breach) {
        List<MobEntity> boundMinions = new ArrayList<>();
        for (UUID creatureId : breach.creatureIds) {
            Entity entity = world.getEntity(creatureId);
            if (entity instanceof MobEntity mob && mob.isAlive()
                    && entity instanceof CaelestisBreachCreature creature && !creature.isUnbound()) {
                boundMinions.add(mob);
            }
        }
        if (boundMinions.isEmpty()) {
            return;
        }

        LivingEntity actor = getLiving(world, breach.actorId);
        LivingEntity principal = getLiving(world, breach.principalId);
        if (actor == null && principal == null) {
            return;
        }

        Box search = new Box(
                breach.center.x - breach.profile.maxRadius,
                breach.center.y - breach.profile.verticalRange,
                breach.center.z - breach.profile.maxRadius,
                breach.center.x + breach.profile.maxRadius,
                breach.center.y + breach.profile.verticalRange,
                breach.center.z + breach.profile.maxRadius
        );
        List<MobEntity> eligible = world.getEntitiesByClass(
                MobEntity.class,
                search,
                mob -> mob.isAlive()
                        && !(mob instanceof CaelestisBreachCreature)
                        && isInside(breach, mob)
                        && (mob.getTarget() == actor || mob.getTarget() == principal)
                        && isValidBoundTarget(world, breach, mob));
        if (eligible.isEmpty()) {
            return;
        }

        eligible.sort(Comparator.comparingDouble(enemy -> boundMinions.stream()
                .mapToDouble(enemy::squaredDistanceTo)
                .min()
                .orElse(Double.MAX_VALUE)));
        int redirects = Math.min(3, Math.min(eligible.size(), (boundMinions.size() + 3) / 4));
        Map<UUID, Integer> assignments = new HashMap<>();
        for (int i = 0; i < redirects; i++) {
            MobEntity enemy = eligible.get(i);
            MobEntity minion = boundMinions.stream()
                    .filter(candidate -> candidate.squaredDistanceTo(enemy)
                            <= AGGRO_REDIRECT_RADIUS * AGGRO_REDIRECT_RADIUS)
                    .min(Comparator.<MobEntity>comparingInt(candidate ->
                                    assignments.getOrDefault(candidate.getUuid(), 0))
                            .thenComparingDouble(candidate -> candidate.squaredDistanceTo(enemy)))
                    .orElse(null);
            if (minion != null) {
                enemy.setTarget(minion);
                assignments.merge(minion.getUuid(), 1, Integer::sum);
            }
        }
    }

    private static void cleanCreatureIds(ServerWorld world, ActiveBreach breach) {
        breach.creatureIds.removeIf(uuid -> {
            Entity entity = world.getEntity(uuid);
            return !(entity instanceof LivingEntity living) || !living.isAlive();
        });
    }

    private static void cleanTentacleIds(ServerWorld world, ActiveBreach breach) {
        breach.tentacleIds.removeIf(uuid ->
                !(world.getEntity(uuid) instanceof CaelestisTentacleEntity));
    }

    private static void beginForcedCollapse(ServerWorld world, ActiveBreach breach, long now) {
        float currentRadius = radiusAt(now, breach);
        boolean alreadyCollapsing = phaseAt(now, breach)
                == CaelestisBreachVisualEntity.PHASE_COLLAPSING;
        long collapseTicks = alreadyCollapsing
                ? Math.max(1L, breach.endTick - now)
                : Math.max(1, breach.profile.collapseTicks);

        breach.forcedCollapse = true;
        breach.forcedCollapseStartTick = now;
        breach.forcedCollapseEndTick = now + collapseTicks;
        breach.forcedCollapseStartRadius = currentRadius;

        for (UUID creatureId : List.copyOf(breach.creatureIds)) {
            Entity entity = world.getEntity(creatureId);
            if (entity instanceof MobEntity mob
                    && entity instanceof CaelestisBreachCreature creature) {
                dissolveCreature(world, mob, creature.isUnbound());
            }
        }
        breach.creatureIds.clear();
        for (UUID tentacleId : List.copyOf(breach.tentacleIds)) {
            Entity entity = world.getEntity(tentacleId);
            if (entity instanceof CaelestisTentacleEntity tentacle) {
                tentacle.beginRetraction();
            }
        }
        breach.tentacleIds.clear();
    }

    private static void close(ServerWorld world, ActiveBreach breach) {
        for (UUID creatureId : List.copyOf(breach.creatureIds)) {
            Entity entity = world.getEntity(creatureId);
            if (entity instanceof MobEntity mob && entity instanceof CaelestisBreachCreature creature) {
                dissolveCreature(world, mob, creature.isUnbound());
            }
        }
        for (UUID tentacleId : List.copyOf(breach.tentacleIds)) {
            Entity entity = world.getEntity(tentacleId);
            if (entity instanceof CaelestisTentacleEntity tentacle) {
                tentacle.beginRetraction();
            }
        }
        Entity visual = world.getEntity(breach.visualId);
        if (visual != null) {
            visual.discard();
        }
        spawnClosingEffects(world, breach.center);
    }

    private static void dissolveCreature(ServerWorld world, MobEntity mob, boolean unbound) {
        world.spawnParticles(unbound ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.REVERSE_PORTAL,
                mob.getX(), mob.getBodyY(0.55), mob.getZ(),
                unbound ? 18 : 26, 0.35, 0.45, 0.35, 0.06);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                mob.getX(), mob.getBodyY(0.6), mob.getZ(),
                8, 0.25, 0.35, 0.25, 0.035);
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.35F, unbound ? 0.55F : 1.65F);
        mob.discard();
    }

    public static void handleCreatureDeath(MobEntity mob, CaelestisBreachCreature creature) {
        if (!(mob.getWorld() instanceof ServerWorld world) || !creature.isUnbound()
                || mob.getCommandTags().contains(UNBOUND_REWARD_ROLLED_TAG)) {
            return;
        }
        mob.addCommandTag(UNBOUND_REWARD_ROLLED_TAG);
        int chance = Math.clamp(Config.uniqueEffects.caelestis.unboundTabletDropChance, 0, 100);
        if (chance > 0 && world.random.nextInt(100) < chance) {
            mob.dropStack(new ItemStack(ItemsRegistry.RUNIC_TABLET.get()), mob.getHeight() * 0.5F);
        }
    }

    private static void spawnOpeningEffects(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.15, center.z,
                70, 1.0, 0.25, 1.0, 0.13);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.2, center.z,
                24, 0.65, 0.35, 0.65, 0.08);
        world.playSound(null, center.x, center.y, center.z,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(), SoundCategory.PLAYERS, 0.9F, 0.58F);
        world.playSound(null, center.x, center.y, center.z,
                SoundRegistry.ACTIVATE_PLINTH_03.get(), SoundCategory.PLAYERS, 0.55F, 0.72F);
    }

    private static void spawnClosingEffects(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.35, center.z,
                90, 1.2, 0.65, 1.2, 0.18);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.5, center.z,
                34, 0.8, 0.7, 0.8, 0.1);
        world.playSound(null, center.x, center.y, center.z,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(), SoundCategory.PLAYERS, 0.85F, 1.55F);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.PLAYERS, 0.35F, 0.5F);
    }

    private static void spawnCreatureArrivalEffects(ServerWorld world, MobEntity mob, boolean unbound) {
        world.spawnParticles(unbound ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.REVERSE_PORTAL,
                mob.getX(), mob.getBodyY(0.45), mob.getZ(),
                unbound ? 28 : 20, 0.32, 0.5, 0.32, 0.075);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                mob.getX(), mob.getBodyY(0.6), mob.getZ(),
                8, 0.24, 0.32, 0.24, 0.04);
        world.playSound(null, mob.getBlockPos(), SoundRegistry.CAELESTIS_CREATURE_ARRIVAL.get(),
                SoundCategory.PLAYERS, unbound ? 0.65F : 0.28F, unbound ? 0.52F : 1.7F);
        world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.25F, unbound ? 0.6F : 1.8F);
    }

    private static void spawnBoundaryParticles(ServerWorld world, Vec3d center, float radius,
                                               int phase, int seed) {
        int count = Config.general.enableModernFieldEffects ? 10 : 5;
        for (int i = 0; i < count; i++) {
            double angle = ((world.getTime() * 0.035) + i * (Math.PI * 2.0 / count)
                    + (seed & 255) * 0.013) % (Math.PI * 2.0);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.12 + world.random.nextDouble() * 1.8;
            world.spawnParticles(
                    phase == CaelestisBreachVisualEntity.PHASE_COLLAPSING
                            ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.REVERSE_PORTAL,
                    x, y, z, 1, 0.04, 0.12, 0.04, 0.025);
        }
    }

    private static void spawnInteriorMotes(ServerWorld world, ActiveBreach breach, float radius) {
        Box box = new Box(
                breach.center.x - radius,
                breach.center.y - breach.profile.verticalRange,
                breach.center.z - radius,
                breach.center.x + radius,
                breach.center.y + breach.profile.verticalRange,
                breach.center.z + radius
        );
        int emitted = 0;
        for (Entity entity : world.getOtherEntities(null, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity living) || !isInside(breach, living) || emitted >= 12) {
                continue;
            }
            world.spawnParticles(ParticleTypes.WITCH,
                    living.getX(), living.getBodyY(0.55), living.getZ(),
                    1, living.getWidth() * 0.25, living.getHeight() * 0.2, living.getWidth() * 0.25, 0.0);
            emitted++;
        }
    }

    private static void playAmbientPulse(ServerWorld world, Vec3d center, int phase) {
        float pitch = phase == CaelestisBreachVisualEntity.PHASE_COLLAPSING
                ? 1.35F + world.random.nextFloat() * 0.25F
                : 0.48F + world.random.nextFloat() * 0.16F;
        world.playSound(null, center.x, center.y, center.z,
                switch (world.random.nextInt(3)) {
                    case 1 -> SoundRegistry.DISTORTION_ARC_02.get();
                    case 2 -> SoundRegistry.DISTORTION_ARC_03.get();
                    default -> SoundRegistry.DISTORTION_ARC_01.get();
                },
                SoundCategory.PLAYERS, 0.26F, pitch);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.PLAYERS, 0.18F, pitch * 0.8F);
    }

    private static void playCreatureAttack(ServerWorld world, MobEntity mob, boolean unbound) {
        world.playSound(null, mob.getBlockPos(),
                SoundRegistry.CAELESTIS_CREATURE_ATTACK.get(),
                SoundCategory.HOSTILE, unbound ? 0.42F : 0.2F,
                unbound ? 0.55F : 1.75F + world.random.nextFloat() * 0.2F);
    }

    private static long getBetrayalDelay(ServerWorld world, BreachProfile profile) {
        float scale = profile.reduced ? 0.5F : 1.0F;
        int latest = Math.max(1, profile.preCollapseTicks() - 20);
        int earliest = Math.min(latest, Math.max(1, Math.round(180.0F * scale)));
        int upper = Math.min(latest, Math.max(earliest, Math.round(460.0F * scale)));
        return earliest + world.random.nextInt(upper - earliest + 1);
    }

    private static float radiusAt(long age, BreachProfile profile) {
        int collapse = profile.collapseTicks;
        int preCollapse = profile.preCollapseTicks();
        float maxRadius = profile.maxRadius;
        if (age >= preCollapse) {
            float progress = MathHelper.clamp((age - preCollapse) / (float) collapse, 0.0F, 1.0F);
            float eased = progress * progress;
            return MathHelper.lerp(eased, maxRadius, 0.0F);
        }

        float progress = MathHelper.clamp(age / (float) preCollapse, 0.0F, 1.0F);
        float curve = MathHelper.clamp(
                preCollapse / (float) profile.expansionTimeScale, 0.25F, 12.0F);
        float exponential = (float) ((1.0 - Math.exp(-curve * progress))
                / (1.0 - Math.exp(-curve)));
        float continuousEaseOut = 0.12F * progress + 0.88F * exponential;
        return MathHelper.lerp(continuousEaseOut, Math.min(START_RADIUS, maxRadius), maxRadius);
    }

    private static int phaseAt(long age, BreachProfile profile) {
        return age >= profile.preCollapseTicks()
                ? CaelestisBreachVisualEntity.PHASE_COLLAPSING
                : CaelestisBreachVisualEntity.PHASE_EXPANDING;
    }

    private static float radiusAt(long now, ActiveBreach breach) {
        if (!breach.forcedCollapse) {
            return radiusAt(now - breach.startTick, breach.profile);
        }
        float progress = MathHelper.clamp(
                (now - breach.forcedCollapseStartTick)
                        / (float) Math.max(
                        1L,
                        breach.forcedCollapseEndTick
                                - breach.forcedCollapseStartTick
                ),
                0.0F,
                1.0F
        );
        return MathHelper.lerp(
                progress * progress,
                breach.forcedCollapseStartRadius,
                0.0F
        );
    }

    private static int phaseAt(long now, ActiveBreach breach) {
        return breach.forcedCollapse
                ? CaelestisBreachVisualEntity.PHASE_COLLAPSING
                : phaseAt(now - breach.startTick, breach.profile);
    }

    private static boolean isWieldingCaelestis(LivingEntity actor) {
        return actor.getMainHandStack().isOf(ItemsRegistry.CAELESTIS.get())
                || actor.getOffHandStack().isOf(ItemsRegistry.CAELESTIS.get());
    }

    private static ActiveBreach get(ServerWorld world, UUID breachId) {
        Map<UUID, ActiveBreach> breaches = ACTIVE.get(world);
        return breaches == null || breachId == null ? null : breaches.get(breachId);
    }

    public static Vec3d getBreachCenter(ServerWorld world, UUID breachId) {
        ActiveBreach breach = get(world, breachId);
        return breach == null ? null : breach.center;
    }

    public static Vec3d constrainCreatureFlightPoint(ServerWorld world, UUID breachId,
                                                     Vec3d desired, double margin) {
        ActiveBreach breach = get(world, breachId);
        if (breach == null) {
            return desired;
        }

        double safeMargin = Math.max(0.25, margin);
        double maxHorizontal = Math.max(0.5,
                radiusAt(world.getTime(), breach) - safeMargin);
        double dx = desired.x - breach.center.x;
        double dz = desired.z - breach.center.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double x = desired.x;
        double z = desired.z;
        if (horizontal > maxHorizontal && horizontal > 0.0001) {
            double scale = maxHorizontal / horizontal;
            x = breach.center.x + dx * scale;
            z = breach.center.z + dz * scale;
        }

        double verticalRange = breach.profile.verticalRange;
        double y = MathHelper.clamp(
                desired.y,
                breach.center.y + 0.75,
                breach.center.y + verticalRange - 0.5
        );
        return new Vec3d(x, y, z);
    }

    private static LivingEntity getLiving(ServerWorld world, UUID uuid) {
        Entity entity = uuid == null ? null : world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof CaelestisBreachVisualEntity visual
                    && visual.getCommandTags().contains(VISUAL_TAG)
                    && !isActive(world, visual.getBreachId())) {
                visual.discard();
            }
        }
    }

    private static final class ActiveBreach {
        private final UUID id;
        private final UUID actorId;
        private final UUID principalId;
        private final ItemStack stack;
        private final Vec3d center;
        private final long startTick;
        private final long endTick;
        private long nextSpawnTick;
        private long nextTentacleSpawnTick;
        private final long betrayalTick;
        private final boolean betrayalPending;
        private boolean betrayalSpawned;
        private final UUID visualId;
        private final float baseDamage;
        private final float ownerAttackValue;
        private final int seed;
        private final BreachProfile profile;
        private final Set<UUID> creatureIds;
        private final Set<UUID> tentacleIds;
        private boolean forcedCollapse;
        private long forcedCollapseStartTick;
        private long forcedCollapseEndTick;
        private float forcedCollapseStartRadius;

        private ActiveBreach(UUID id, UUID actorId, UUID principalId, ItemStack stack, Vec3d center,
                             long startTick, long endTick, long nextSpawnTick,
                             long nextTentacleSpawnTick, long betrayalTick,
                             boolean betrayalPending, boolean betrayalSpawned, UUID visualId,
                             float baseDamage, float ownerAttackValue, int seed,
                             BreachProfile profile, Set<UUID> creatureIds,
                             Set<UUID> tentacleIds) {
            this.id = id;
            this.actorId = actorId;
            this.principalId = principalId;
            this.stack = stack;
            this.center = center;
            this.startTick = startTick;
            this.endTick = endTick;
            this.nextSpawnTick = nextSpawnTick;
            this.nextTentacleSpawnTick = nextTentacleSpawnTick;
            this.betrayalTick = betrayalTick;
            this.betrayalPending = betrayalPending;
            this.betrayalSpawned = betrayalSpawned;
            this.visualId = visualId;
            this.baseDamage = baseDamage;
            this.ownerAttackValue = ownerAttackValue;
            this.seed = seed;
            this.profile = profile;
            this.creatureIds = creatureIds;
            this.tentacleIds = tentacleIds;
        }
    }

    private static final class BreachProfile {
        private final boolean reduced;
        private final int duration;
        private final int expansionTimeScale;
        private final int collapseTicks;
        private final float maxRadius;
        private final float verticalRange;
        private final int minSpawnPerWave;
        private final int maxSpawnPerWave;
        private final int maxMinions;
        private final int maxTentacles;
        private final float damageMultiplier;

        private BreachProfile(boolean reduced, int duration, int expansionTimeScale,
                              int collapseTicks, float maxRadius, float verticalRange,
                              int minSpawnPerWave, int maxSpawnPerWave, int maxMinions,
                              int maxTentacles, float damageMultiplier) {
            this.reduced = reduced;
            this.duration = duration;
            this.expansionTimeScale = expansionTimeScale;
            this.collapseTicks = collapseTicks;
            this.maxRadius = maxRadius;
            this.verticalRange = verticalRange;
            this.minSpawnPerWave = minSpawnPerWave;
            this.maxSpawnPerWave = maxSpawnPerWave;
            this.maxMinions = maxMinions;
            this.maxTentacles = maxTentacles;
            this.damageMultiplier = damageMultiplier;
        }

        private static BreachProfile capture(boolean playerCast) {
            boolean reduced = !playerCast;
            float scale = reduced ? 0.5F : 1.0F;
            int duration = Math.max(2, Math.round(Math.max(2,
                    Config.uniqueEffects.caelestis.duration) * scale));
            int collapse = Math.clamp(Math.max(1, Math.round(
                            Config.uniqueEffects.caelestis.collapseDuration * scale)),
                    1, duration - 1);
            int preCollapse = Math.max(1, duration - collapse);
            int expansion = Math.clamp(Math.max(1, Math.round(
                            Config.uniqueEffects.caelestis.expansionDuration * scale)),
                    1, preCollapse);
            int minWave = scaledCount(Config.uniqueEffects.caelestis.minSpawnPerWave, scale);
            int maxWave = Math.max(minWave,
                    scaledCount(Config.uniqueEffects.caelestis.maxSpawnPerWave, scale));
            return new BreachProfile(
                    reduced,
                    duration,
                    expansion,
                    collapse,
                    Math.max(1.0F, Config.uniqueEffects.caelestis.maxRadius * scale),
                    Math.max(2.0F, Config.uniqueEffects.caelestis.verticalRange * scale),
                    minWave,
                    maxWave,
                    scaledCount(Config.uniqueEffects.caelestis.maxMinions, scale),
                    scaledOptionalCount(Config.uniqueEffects.caelestis.maxTentacles, scale),
                    scale
            );
        }

        private static int scaledCount(int value, float scale) {
            return Math.max(1, (int) Math.ceil(Math.max(1, value) * scale));
        }

        private static int scaledOptionalCount(int value, float scale) {
            return value <= 0 ? 0 : Math.max(1, (int) Math.ceil(value * scale));
        }

        private int preCollapseTicks() {
            return Math.max(1, duration - collapseTicks);
        }
    }
}
