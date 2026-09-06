package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.MagispearFallingSpearVisualEntity;
import net.sweenus.simplyswords.entity.MagispearFirmamentVisualEntity;
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

public final class MagispearAbilityManager {

    private static final int LAUNCH_END_TICK = 6;
    private static final int RAIN_START_TICK = 6;
    private static final int RAIN_SPAWN_SPAN = 12;
    private static final int RAIN_FALL_TICKS = 5;
    private static final int LEAP_START_TICK = 20;
    private static final int AIRBORNE_TICKS = 16;
    private static final int BASE_SPEARS_PER_WAVE = 2;
    private static final int MAX_SPEARS_PER_WAVE = 12;
    private static final int ECHO_TARGET_CAP = 64;
    private static final int FIELD_VISUAL_LIFETIME = 46;
    private static final double SKY_HEIGHT = 12.0;
    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 32;
    private static final String FIELD_VISUAL_TAG = "simplyswords_magispear_firmament_visual";
    private static final String SPEAR_VISUAL_TAG = "simplyswords_magispear_spear_visual";

    private static final Map<ServerWorld, Map<UUID, ActiveMagislam>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<PendingEcho>> ECHOES = new HashMap<>();

    public static ArcaneCosmicMasteryTuning spellpointBase(int chance) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.CHANCE, chance)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning rainBase(int waveCount, double radius, double pull) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.STACK_CAP, waveCount)
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, radius)
                .with(ArcaneCosmicMasteryTuning.Setting.PULL_STRENGTH, pull)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning slamBase(double slamRadius, double diveHeight) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, slamRadius)
                .with(ArcaneCosmicMasteryTuning.Setting.HEIGHT, diveHeight)
                .with(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1);
    }

    private static ArcaneCosmicMasteryTuning rainBase() {
        return rainBase(Config.uniqueEffects.magispear.rainWaveCount, Config.uniqueEffects.magispear.radius,
                Config.uniqueEffects.magispear.inwardPushStrength)
                .with(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, RAIN_START_TICK)
                .with(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, RAIN_SPAWN_SPAN)
                .with(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, BASE_SPEARS_PER_WAVE)
                .with(ArcaneCosmicMasteryTuning.Setting.COUNT, 2);
    }

    private static ArcaneCosmicMasteryTuning slamBase() {
        return slamBase(Config.uniqueEffects.magispear.radius, Config.uniqueEffects.magispear.diveHeight)
                .with(ArcaneCosmicMasteryTuning.Setting.COOLDOWN_TICKS,
                        Config.uniqueEffects.magispear.cooldown)
                .with(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, AIRBORNE_TICKS)
                .with(ArcaneCosmicMasteryTuning.Setting.KNOCKBACK, 1);
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveMagislam> active = ACTIVE.remove(world);
        if (active != null) active.values().forEach(m -> ArcaneCosmicMasteryCombatManager.finish(m.execution, 0));
        ECHOES.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        ACTIVE.values().forEach(map -> {
            ActiveMagislam m = map.remove(actor.getUuid());
            if (m != null) ArcaneCosmicMasteryCombatManager.finish(m.execution, 0);
        });
        ECHOES.values().forEach(list -> list.removeIf(echo -> echo.ownerId.equals(actor.getUuid())));
    }

    public static void clearAll() {
        ACTIVE.values().forEach(map -> map.values().forEach(
                m -> ArcaneCosmicMasteryCombatManager.finish(m.execution, 0)));
        ACTIVE.clear();
        ECHOES.clear();
    }

    public static void scheduleEcho(ServerWorld world, LivingEntity owner, LivingEntity target,
                                    net.minecraft.item.ItemStack stack, float damage, int delay) {
        if (world == null || owner == null || target == null || stack == null || damage <= 0) {
            return;
        }
        List<PendingEcho> echoes = ECHOES.computeIfAbsent(world, ignored -> new ArrayList<>());
        if (echoes.size() >= ECHO_TARGET_CAP) {
            return;
        }
        echoes.add(new PendingEcho(owner.getUuid(), target.getUuid(), stack.copy(), damage,
                world.getTime() + Math.max(1, delay)));
    }

    private static void tickEchoes(ServerWorld world) {
        List<PendingEcho> echoes = ECHOES.get(world);
        if (echoes == null) {
            return;
        }
        echoes.removeIf(echo -> {
            if (world.getTime() < echo.at) {
                return false;
            }
            if (world.getEntity(echo.ownerId) instanceof LivingEntity owner
                    && world.getEntity(echo.targetId) instanceof LivingEntity target
                    && owner.isAlive() && target.isAlive()) {
                damageTarget(world, owner, echo.stack, target, echo.damage);
            }
            return true;
        });
        if (echoes.isEmpty()) {
            ECHOES.remove(world);
        }
    }

    private MagispearAbilityManager() {
    }

    public static boolean canStart(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().isOf(ItemsRegistry.MAGISPEAR.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !isActive(context.actor())
                && resolveTargetPosition(context) != null;
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canStart(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        Vec3d center = resolveTargetPosition(context);
        if (center == null) {
            return false;
        }

        UniqueAbilityExecution slamExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.MAGISPEAR_SLAM, world, context.stack(), actor, context.target(),
                slamBase());
        ArcaneCosmicMasteryTuning slam = ArcaneCosmicMasteryAbilities.tuning(slamExecution);
        ArcaneCosmicMasteryCombatManager.finish(slamExecution, 0);
        int cooldown = Math.max(0, slam.integer(ArcaneCosmicMasteryTuning.Setting.COOLDOWN_TICKS,
                Config.uniqueEffects.magispear.cooldown));
        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginActive(
                ArcaneCosmicMasteryAbilities.MAGISPEAR_RAIN, context, cooldown, rainBase());
        ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
        int waveCount = MathHelper.clamp(tuning.integer(ArcaneCosmicMasteryTuning.Setting.STACK_CAP,
                Config.uniqueEffects.magispear.rainWaveCount), 1, 12);
        float radius = (float) Math.max(1.0, tuning.get(
                ArcaneCosmicMasteryTuning.Setting.RADIUS, Config.uniqueEffects.magispear.radius));
        ActiveMagislam magislam = new ActiveMagislam(
                actor.getUuid(), context.stack().copy(), center, world.getTime(), waveCount,
                new boolean[waveCount], new boolean[waveCount], tuning, execution
        );
        magislam.slam = slam;
        int airborne = MathHelper.clamp(slam.integer(
                ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, AIRBORNE_TICKS), 4, 120);
        magislam.plungeStartTick = LEAP_START_TICK + airborne / 2;
        magislam.impactTick = LEAP_START_TICK + airborne;
        magislam.rooted = tuning.flag(1 << 16);
        LivingEntity followed = context.target();
        magislam.targetId = followed == null ? null : followed.getUuid();

        if (Config.general.enableModernFieldEffects) {
            MagispearFirmamentVisualEntity field = new MagispearFirmamentVisualEntity(
                    world, center.x, center.y, center.z, radius, waveCount, FIELD_VISUAL_LIFETIME);
            field.addCommandTag(FIELD_VISUAL_TAG);
            world.spawnEntity(field);
            magislam.fieldVisualId = field.getUuid();

            Vec3d launchStart = actor.getEyePos().add(actor.getRotationVec(1.0F).multiply(0.55));
            Vec3d launchEnd = center.add(0.0, SKY_HEIGHT, 0.0);
            spawnSpearVisual(world, magislam, launchStart, launchEnd, LAUNCH_END_TICK,
                    MagispearFallingSpearVisualEntity.MODE_LAUNCH, 1.0F);
        }

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), magislam);
        spawnActivationEffects(world, actor, center);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        return active != null && !active.isEmpty();
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    public static boolean blocksIncomingDamage(LivingEntity actor, DamageSource source) {
        if (actor == null || source == null || actor.getWorld().isClient()
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        ActiveMagislam magislam = active == null ? null : active.get(actor.getUuid());
        if (magislam == null) {
            return false;
        }
        long age = world.getTime() - magislam.startedAt;
        return !magislam.rooted && age >= LEAP_START_TICK && age <= magislam.impactTick;
    }

    public static boolean hasWork(ServerWorld world) {
        return hasActive(world) || !ECHOES.getOrDefault(world, List.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        tickEchoes(world);
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        Iterator<ActiveMagislam> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveMagislam magislam = iterator.next();
            Entity ownerEntity = world.getEntity(magislam.ownerId);
            if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) {
                cancel(world, magislam);
                iterator.remove();
                continue;
            }

            if (tickMagislam(world, owner, magislam)) {
                ArcaneCosmicMasteryCombatManager.finish(magislam.execution, magislam.hitTargets.size());
                iterator.remove();
            }
        }

        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static boolean tickMagislam(ServerWorld world, LivingEntity owner, ActiveMagislam magislam) {
        if (magislam.craterPulseAt > 0) {
            if (world.getTime() < magislam.craterPulseAt) {
                return false;
            }
            craterPulse(world, owner, magislam);
            return true;
        }

        long age = world.getTime() - magislam.startedAt;
        tickRainWaves(world, owner, magislam, age);

        if (magislam.rooted) {
            return allWavesImpacted(magislam);
        }

        if (age >= LEAP_START_TICK && age < magislam.plungeStartTick) {
            guideOwner(owner, followedCenter(world, magislam).add(0.0,
                    Math.max(1.0, magislam.slam.get(ArcaneCosmicMasteryTuning.Setting.HEIGHT,
                            Config.uniqueEffects.magispear.diveHeight)), 0.0),
                    (int) Math.max(1L, magislam.plungeStartTick - age));
            magislam.movementObstructed |= owner.horizontalCollision;
        } else if (age >= magislam.plungeStartTick && age < magislam.impactTick) {
            Vec3d aim = followedCenter(world, magislam);
            if (!magislam.finalSpearSpawned) {
                magislam.finalSpearSpawned = true;
                if (Config.general.enableModernFieldEffects) {
                    Vec3d start = aim.add(0.0, SKY_HEIGHT + 2.0, 0.0);
                    spawnSpearVisual(world, magislam, start, aim,
                            magislam.impactTick - magislam.plungeStartTick,
                            MagispearFallingSpearVisualEntity.MODE_FINAL, 1.0F);
                }
                world.playSound(null, aim.x, aim.y + 4.0, aim.z,
                        SoundRegistry.MAGIC_SHAMANIC_NORDIC_27.get(), SoundCategory.PLAYERS, 0.75F, 0.55F);
            }

            guideOwner(owner, aim.add(0.0, 0.08, 0.0),
                    (int) Math.max(1L, magislam.impactTick - age));
            magislam.movementObstructed |= owner.horizontalCollision;
            if (age > magislam.plungeStartTick + 1L && (owner.horizontalCollision || owner.isOnGround())) {
                Vec3d impact = getGroundPosition(world, owner.getPos());
                flushPendingWaves(world, owner, magislam);
                finish(world, owner, magislam, impact);
                return magislam.craterPulseAt <= 0;
            }
        }

        if (age >= magislam.impactTick) {
            Vec3d aim = followedCenter(world, magislam);
            Vec3d impact = magislam.movementObstructed
                    || owner.getPos().squaredDistanceTo(aim) > 9.0
                    ? getGroundPosition(world, owner.getPos())
                    : aim;
            flushPendingWaves(world, owner, magislam);
            finish(world, owner, magislam, impact);
            return magislam.craterPulseAt <= 0;
        }
        return false;
    }

    private static void flushPendingWaves(ServerWorld world, LivingEntity owner, ActiveMagislam magislam) {
        for (int wave = 0; wave < magislam.waveCount; wave++) {
            if (!magislam.waveImpacted[wave]) {
                magislam.waveSpawned[wave] = true;
                magislam.waveImpacted[wave] = true;
                impactRainWave(world, owner, magislam, wave);
            }
        }
    }

    private static boolean allWavesImpacted(ActiveMagislam magislam) {
        for (boolean impacted : magislam.waveImpacted) {
            if (!impacted) {
                return false;
            }
        }
        return true;
    }

    private static Vec3d followedCenter(ServerWorld world, ActiveMagislam magislam) {
        double leash = magislam.slam.get(ArcaneCosmicMasteryTuning.Setting.RANGE, 0);
        if (leash <= 0.0 || magislam.targetId == null) {
            return magislam.center;
        }
        if (!(world.getEntity(magislam.targetId) instanceof LivingEntity target) || !target.isAlive()) {
            return magislam.center;
        }
        Vec3d offset = new Vec3d(target.getX() - magislam.origin.x, 0.0, target.getZ() - magislam.origin.z);
        double distance = offset.length();
        if (distance > leash) {
            offset = offset.multiply(leash / distance);
        }
        magislam.center = getGroundPosition(world, magislam.origin.add(offset));
        return magislam.center;
    }

    private static void tickRainWaves(ServerWorld world, LivingEntity owner,
                                      ActiveMagislam magislam, long age) {
        for (int wave = 0; wave < magislam.waveCount; wave++) {
            int spawnTick = getWaveSpawnTick(magislam, wave);
            if (age >= spawnTick && !magislam.waveSpawned[wave]) {
                magislam.waveSpawned[wave] = true;
                spawnRainWave(world, magislam, wave);
            }
            if (age >= spawnTick + RAIN_FALL_TICKS && !magislam.waveImpacted[wave]) {
                magislam.waveImpacted[wave] = true;
                impactRainWave(world, owner, magislam, wave);
            }
        }
    }

    private static int getWaveSpawnTick(ActiveMagislam magislam, int wave) {
        int start = Math.max(0, magislam.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, RAIN_START_TICK));
        int span = Math.max(0, magislam.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, RAIN_SPAWN_SPAN));
        if (magislam.waveCount <= 1) {
            return clampWaveSpawnTick(magislam, start + span / 2);
        }
        if (magislam.rooted) {
            return start + Math.round((float) wave * span / (magislam.waveCount - 1));
        }
        int latest = latestWaveSpawnTick(magislam);
        if (start > latest) {
            start = latest;
            span = 0;
        } else if (start + span > latest) {
            span = latest - start;
        }
        return start + Math.round((float) wave * span / (magislam.waveCount - 1));
    }

    private static int latestWaveSpawnTick(ActiveMagislam magislam) {
        return Math.max(0, magislam.impactTick - RAIN_FALL_TICKS);
    }

    private static int clampWaveSpawnTick(ActiveMagislam magislam, int spawnTick) {
        return magislam.rooted ? spawnTick : Math.min(spawnTick, latestWaveSpawnTick(magislam));
    }

    private static int getSpearsPerWave(ActiveMagislam magislam) {
        return MathHelper.clamp(magislam.tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, BASE_SPEARS_PER_WAVE),
                1, MAX_SPEARS_PER_WAVE);
    }

    private static void spawnRainWave(ServerWorld world, ActiveMagislam magislam, int wave) {
        int spears = getSpearsPerWave(magislam);
        for (int index = 0; index < spears; index++) {
            Vec3d strike = getStrikePosition(world, magislam, wave, index, spears);
            if (Config.general.enableModernFieldEffects) {
                spawnSpearVisual(world, magislam, strike.add(0.0, SKY_HEIGHT, 0.0), strike,
                        RAIN_FALL_TICKS, MagispearFallingSpearVisualEntity.MODE_RAIN, 1.0F);
            }
            spawnTelegraphParticles(world, strike);
        }
        world.playSound(null, magislam.center.x, magislam.center.y + 6.0, magislam.center.z,
                SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.35F, 1.35F + wave * 0.08F);
    }

    private static void impactRainWave(ServerWorld world, LivingEntity owner,
                                       ActiveMagislam magislam, int wave) {
        int spears = getSpearsPerWave(magislam);
        double splashRadius = Math.max(0.25, Config.uniqueEffects.magispear.rainSplashRadius);
        Set<UUID> hitThisWave = new HashSet<>();
        float waveScaling = Config.uniqueEffects.magispear.throwDamageScaling / Math.max(1, magislam.waveCount);
        float waveSpellScaling = Config.uniqueEffects.magispear.throwSpellScaling / Math.max(1, magislam.waveCount);
        float baseDamage = HelperMethods.abilityScaledDamage("arcane", owner, magislam.stack,
                waveScaling * (float) magislam.tuning.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                waveSpellScaling);
        for (int index = 0; index < spears; index++) {
            Vec3d strike = getStrikePosition(world, magislam, wave, index, spears);
            damageRainArea(world, owner, magislam, strike, splashRadius, baseDamage, hitThisWave);
            spawnRainImpactEffects(world, strike);
        }
    }

    private static void damageRainArea(ServerWorld world, LivingEntity owner, ActiveMagislam magislam,
                                       Vec3d impact, double radius, float baseDamage, Set<UUID> hitThisWave) {
        Box box = new Box(impact.x - radius, impact.y - 1.5, impact.z - radius,
                impact.x + radius, impact.y + 3.0, impact.z + radius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity != owner
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && horizontalSquaredDistance(entity.getPos(), impact) <= radius * radius
                        && hitThisWave.add(entity.getUuid()))) {
            float damage = magislam.marked.contains(target.getUuid())
                    ? baseDamage * (float) magislam.tuning.get(
                    ArcaneCosmicMasteryTuning.Setting.OUTGOING_MULTIPLIER, 1) : baseDamage;
            if (damageTarget(world, owner, magislam.stack, target, damage)) {
                magislam.hitTargets.add(target.getUuid());
                pullTowardCenter(target, magislam.center,
                        Math.max(0.0, magislam.tuning.get(ArcaneCosmicMasteryTuning.Setting.PULL_STRENGTH,
                                Config.uniqueEffects.magispear.inwardPushStrength)));
                int hits = magislam.waveHits.merge(target.getUuid(), 1, Integer::sum);
                if (magislam.tuning.flag(1 << 15) && hits >= Math.max(1, magislam.tuning.integer(
                        ArcaneCosmicMasteryTuning.Setting.COUNT, 2))) magislam.marked.add(target.getUuid());
            }
        }
    }

    private static void finish(ServerWorld world, LivingEntity owner,
                               ActiveMagislam magislam, Vec3d impact) {
        magislam.center = impact;
        updateFieldVisual(world, magislam, impact);
        owner.setVelocity(0.0, 0.12, 0.0);
        owner.velocityModified = true;
        owner.fallDistance = 0.0F;
        applyLandingBuffs(world, owner, magislam, impact);

        double radius = Math.max(1.0, magislam.slam.get(
                ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, Config.uniqueEffects.magispear.radius));
        float baseDamage = HelperMethods.abilityScaledDamage(
                "arcane", owner, magislam.stack,
                Config.uniqueEffects.magispear.damageScaling * (float) magislam.slam.get(
                        ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)
                        * (float) magislam.tuning.get(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1),
                Config.uniqueEffects.magispear.spellScaling);
        if (magislam.tuning.get(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1) <= 0
                || magislam.slam.get(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1) <= 0) {
            spawnFinalImpactEffects(world, impact, radius);
            return;
        }
        double knockback = magislam.slam.get(ArcaneCosmicMasteryTuning.Setting.KNOCKBACK, 1);
        int quakeCap = magislam.slam.flag(1 << 24)
                ? magislam.slam.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 10) : 0;
        int quaked = 0;
        for (LivingEntity target : slamTargets(world, owner, impact, radius)) {
            if (damageTarget(world, owner, magislam.stack, target, baseDamage)) {
                magislam.hitTargets.add(target.getUuid());
                knockFromImpact(target, impact, knockback);
                if (quaked < quakeCap) {
                    quaked++;
                    target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.SLOWNESS,
                            magislam.slam.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 50),
                            magislam.slam.integer(ArcaneCosmicMasteryTuning.Setting.STATUS_AMPLIFIER, 1)), owner);
                }
            }
        }
        spawnFinalImpactEffects(world, impact, radius);
        if (magislam.slam.flag(1 << 23)) {
            magislam.craterPulseAt = world.getTime() + Math.max(1,
                    magislam.slam.integer(ArcaneCosmicMasteryTuning.Setting.DELAY_TICKS, 10));
            magislam.craterPulseDamage = baseDamage * (float) magislam.slam.get(
                    ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .35);
            magislam.craterPulseRadius = radius;
        }
    }

    private static List<LivingEntity> slamTargets(ServerWorld world, LivingEntity owner,
                                                  Vec3d impact, double radius) {
        Box box = new Box(impact.x - radius, impact.y - 2.0, impact.z - radius,
                impact.x + radius, impact.y + radius, impact.z + radius);
        return world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity != owner
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && horizontalSquaredDistance(entity.getPos(), impact) <= radius * radius);
    }

    private static void applyLandingBuffs(ServerWorld world, LivingEntity owner,
                                          ActiveMagislam magislam, Vec3d impact) {
        if (magislam.slam.flag(1 << 22)) {
            owner.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE,
                    Math.max(1, magislam.slam.integer(
                            ArcaneCosmicMasteryTuning.Setting.TERTIARY_STATUS_DURATION_TICKS, 8)), 2));
        }
        if (!magislam.slam.flag(1 << 26)) {
            return;
        }
        double allyRadius = Math.max(1.0, magislam.slam.get(
                ArcaneCosmicMasteryTuning.Setting.TERTIARY_RADIUS, 5));
        float absorption = (float) magislam.slam.get(ArcaneCosmicMasteryTuning.Setting.ABSORPTION, 8);
        int duration = Math.max(1, magislam.slam.integer(
                ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 100));
        for (LivingEntity ally : world.getEntitiesByClass(LivingEntity.class,
                new Box(impact.x - allyRadius, impact.y - 2.0, impact.z - allyRadius,
                        impact.x + allyRadius, impact.y + allyRadius, impact.z + allyRadius),
                entity -> entity != owner
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && !HelperMethods.checkAbilityTarget(entity, owner)
                        && horizontalSquaredDistance(entity.getPos(), impact)
                        <= allyRadius * allyRadius)) {
            MasteryAbsorptionTracker.grant(ally, absorption, duration, absorption);
        }
    }

    private static void craterPulse(ServerWorld world, LivingEntity owner, ActiveMagislam magislam) {
        Vec3d impact = magislam.center;
        double radius = magislam.craterPulseRadius;
        for (LivingEntity target : slamTargets(world, owner, impact, radius)) {
            if (damageTarget(world, owner, magislam.stack, target, magislam.craterPulseDamage)) {
                magislam.hitTargets.add(target.getUuid());
            }
        }
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y + 0.35, impact.z,
                18, radius * 0.42, 0.25, radius * 0.42, 0.12);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, impact.x, impact.y + 0.25, impact.z,
                20, radius * 0.45, 0.18, radius * 0.45, 0.08);
        world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.55F, 0.95F);
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity owner, net.minecraft.item.ItemStack stack,
                                        LivingEntity target, float baseDamage) {
        DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, baseDamage);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        return damaged[0];
    }

    private static void pullTowardCenter(LivingEntity target, Vec3d center, double strength) {
        Vec3d direction = new Vec3d(center.x - target.getX(), 0.0, center.z - target.getZ());
        if (direction.lengthSquared() < 0.001 || strength <= 0.0) {
            return;
        }
        Vec3d pull = direction.normalize().multiply(strength);
        Vec3d current = target.getVelocity();
        target.setVelocity(current.x * 0.35 + pull.x, Math.max(current.y, 0.08), current.z * 0.35 + pull.z);
        target.velocityModified = true;
    }

    private static void knockFromImpact(LivingEntity target, Vec3d impact, double knockback) {
        Vec3d direction = new Vec3d(target.getX() - impact.x, 0.0, target.getZ() - impact.z);
        if (direction.lengthSquared() < 0.001) {
            direction = new Vec3d(0.0, 0.0, 1.0);
        }
        Vec3d push = direction.normalize().multiply(0.55);
        target.setVelocity(push.x, Math.max(target.getVelocity().y, 0.32 * Math.max(0.0, knockback)),
                push.z);
        target.velocityModified = true;
    }

    private static void guideOwner(LivingEntity owner, Vec3d destination, int remainingTicks) {
        Vec3d delta = destination.subtract(owner.getPos());
        Vec3d velocity = delta.multiply(1.0 / Math.max(1, remainingTicks));
        double maxSpeed = 2.25;
        if (velocity.lengthSquared() > maxSpeed * maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }
        owner.setVelocity(velocity);
        owner.velocityModified = true;
        owner.fallDistance = 0.0F;
    }

    private static Vec3d getStrikePosition(ServerWorld world, ActiveMagislam magislam,
                                           int wave, int index, int spears) {
        double radius = Math.max(1.0, magislam.tuning.get(
                ArcaneCosmicMasteryTuning.Setting.RADIUS, Config.uniqueEffects.magispear.radius)) * 0.78;
        double angle = MathHelper.TAU * wave / (magislam.waveCount * 2.0)
                + MathHelper.TAU * index / Math.max(1, spears);
        Vec3d rough = magislam.center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return getGroundPosition(world, rough);
    }

    private static Vec3d resolveTargetPosition(WeaponAbilityContext context) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        double range = Math.max(1.0, Config.uniqueEffects.magispear.targetingRange);

        LivingEntity target = context.target();
        if (target != null && target.isAlive() && HelperMethods.checkAbilityTarget(target, actor)
                && actor.squaredDistanceTo(target) <= range * range) {
            return getGroundPosition(world, target.getPos());
        }
        if (!(actor instanceof net.minecraft.entity.player.PlayerEntity)) {
            return null;
        }

        Vec3d start = actor.getEyePos();
        Vec3d end = start.add(context.facing().normalize().multiply(range));
        HitResult result = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, actor));
        Vec3d candidate = result.getType() == HitResult.Type.BLOCK ? result.getPos() : end;
        return findGroundPosition(world, candidate.x, candidate.z, candidate.y);
    }

    private static Vec3d getGroundPosition(ServerWorld world, Vec3d position) {
        Vec3d ground = findGroundPosition(world, position.x, position.z, position.y);
        return ground == null ? position : ground;
    }

    private static Vec3d findGroundPosition(ServerWorld world, double x, double z, double referenceY) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int startY = Math.min(world.getTopY() - 1, MathHelper.floor(referenceY) + GROUND_SCAN_UP);
        int minY = Math.max(world.getBottomY(), MathHelper.floor(referenceY) - GROUND_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return new Vec3d(x, y + 1.02, z);
            }
        }
        return null;
    }

    private static void spawnSpearVisual(ServerWorld world, ActiveMagislam magislam,
                                         Vec3d start, Vec3d end, int lifetime, int mode, float scale) {
        MagispearFallingSpearVisualEntity visual = new MagispearFallingSpearVisualEntity(
                world, start.x, start.y, start.z,
                end.x - start.x, end.y - start.y, end.z - start.z,
                lifetime, mode, scale);
        visual.addCommandTag(SPEAR_VISUAL_TAG);
        world.spawnEntity(visual);
        magislam.spearVisualIds.add(visual.getUuid());
    }

    private static void updateFieldVisual(ServerWorld world, ActiveMagislam magislam, Vec3d position) {
        Entity entity = magislam.fieldVisualId == null ? null : world.getEntity(magislam.fieldVisualId);
        if (entity instanceof MagispearFirmamentVisualEntity field) {
            field.setPosition(position.x, position.y, position.z);
        }
    }

    private static void cancel(ServerWorld world, ActiveMagislam magislam) {
        Entity field = magislam.fieldVisualId == null ? null : world.getEntity(magislam.fieldVisualId);
        if (field != null) {
            field.discard();
        }
        for (UUID visualId : magislam.spearVisualIds) {
            Entity visual = world.getEntity(visualId);
            if (visual != null) {
                visual.discard();
            }
        }
        ArcaneCosmicMasteryCombatManager.finish(magislam.execution, magislam.hitTargets.size());
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor, Vec3d center) {
        world.spawnParticles(ParticleTypes.ENCHANT, actor.getX(), actor.getEyeY(), actor.getZ(),
                24, 0.45, 0.55, 0.45, 0.2);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.1, center.z,
                Config.general.enableModernFieldEffects ? 24 : 12, 0.65, 0.05, 0.65, 0.04);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_NORDIC_27.get(),
                actor.getSoundCategory(), 0.65F, 1.05F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.45F, 1.35F);
    }

    private static void spawnTelegraphParticles(ServerWorld world, Vec3d position) {
        world.spawnParticles(ParticleTypes.ENCHANT, position.x, position.y + 0.08, position.z,
                Config.general.enableModernFieldEffects ? 10 : 18, 0.28, 0.04, 0.28, 0.02);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.25, position.z,
                5, 0.2, 0.2, 0.2, 0.02);
    }

    private static void spawnRainImpactEffects(ServerWorld world, Vec3d position) {
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, position.x, position.y + 0.3, position.z,
                Config.general.enableModernFieldEffects ? 18 : 10, 0.42, 0.32, 0.42, 0.1);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.18, position.z,
                12, 0.35, 0.18, 0.35, 0.08);
        world.spawnParticles(ParticleTypes.GLOW, position.x, position.y + 0.25, position.z,
                7, 0.28, 0.24, 0.28, 0.04);
        world.playSound(null, position.x, position.y, position.z,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.45F,
                1.25F + world.random.nextFloat() * 0.25F);
    }

    private static void spawnFinalImpactEffects(ServerWorld world, Vec3d impact, double radius) {
        int multiplier = Config.general.enableModernFieldEffects ? 2 : 1;
        world.spawnParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.45, impact.z,
                2, 0.15, 0.05, 0.15, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y + 0.45, impact.z,
                24 * multiplier, radius * 0.45, 0.4, radius * 0.45, 0.15);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, impact.x, impact.y + 0.35, impact.z,
                32 * multiplier, radius * 0.48, 0.25, radius * 0.48, 0.12);
        world.spawnParticles(ParticleTypes.GLOW, impact.x, impact.y + 0.55, impact.z,
                18 * multiplier, radius * 0.32, 0.45, radius * 0.32, 0.08);
        world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.85F, 0.7F);
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 0.85F, 0.62F);
        world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS, 0.55F, 0.55F);
    }

    private static double horizontalSquaredDistance(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private record PendingEcho(UUID ownerId, UUID targetId, net.minecraft.item.ItemStack stack,
                               float damage, long at) {
    }

    private static final class ActiveMagislam {
        private ArcaneCosmicMasteryTuning slam = ArcaneCosmicMasteryTuning.EMPTY;
        private final UUID ownerId;
        private final net.minecraft.item.ItemStack stack;
        private Vec3d center;
        private final Vec3d origin;
        private UUID targetId;
        private int plungeStartTick;
        private int impactTick;
        private boolean rooted;
        private long craterPulseAt;
        private float craterPulseDamage;
        private double craterPulseRadius;
        private final long startedAt;
        private final int waveCount;
        private final boolean[] waveSpawned;
        private final boolean[] waveImpacted;
        private final List<UUID> spearVisualIds = new ArrayList<>();
        private UUID fieldVisualId;
        private boolean finalSpearSpawned;
        private boolean movementObstructed;
        private final ArcaneCosmicMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final Map<UUID, Integer> waveHits = new HashMap<>();
        private final Set<UUID> marked = new HashSet<>();
        private final Set<UUID> hitTargets = new HashSet<>();

        private ActiveMagislam(UUID ownerId, net.minecraft.item.ItemStack stack, Vec3d center,
                               long startedAt, int waveCount,
                               boolean[] waveSpawned, boolean[] waveImpacted,
                               ArcaneCosmicMasteryTuning tuning, UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.center = center;
            this.origin = center;
            this.startedAt = startedAt;
            this.waveCount = waveCount;
            this.waveSpawned = waveSpawned;
            this.waveImpacted = waveImpacted;
            this.tuning = tuning;
            this.execution = execution;
        }
    }
}
