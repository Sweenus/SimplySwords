package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.entity.IcewhisperCometVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;

import java.util.*;

public final class IcewhisperCometManager {

    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 24;
    private static final double SPAWN_HEIGHT = 18.0;
    public static final int MAX_COMETS_PER_WAVE = 8;
    private static final String COMET_VISUAL_TAG = "simplyswords_icewhisper_comet_visual";
    private static final Map<ServerWorld, List<ActiveComet>> ACTIVE_COMETS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveStorm>> ACTIVE_STORMS = new HashMap<>();
    private static final Set<ServerWorld> PENDING_PURGE = new HashSet<>();

    private IcewhisperCometManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveComet> comets = ACTIVE_COMETS.get(world);
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        return (comets != null && !comets.isEmpty())
                || (storms != null && !storms.isEmpty())
                || PENDING_PURGE.contains(world)
                || IcewhisperAbilityManager.hasPending();
    }

    public static void startStorm(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage,
                                  int durationTicks) {
        startStorm(world, owner, stack, radius, damage, durationTicks, Phase6AbilityTuning.EMPTY, null);
    }

    public static void startStorm(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage,
                                  int durationTicks, Phase6AbilityTuning tuning, UniqueAbilityExecution execution) {
        if (owner == null || durationTicks <= 0) {
            return;
        }

        List<ActiveStorm> storms = ACTIVE_STORMS.computeIfAbsent(world, w -> new ArrayList<>());
        storms.removeIf(storm -> {
            if (!storm.ownerId.equals(owner.getUuid())) {
                return false;
            }
            finishStorm(storm);
            return true;
        });
        int duration = stormDuration(tuning, durationTicks);
        long expiry = world.getTime() + duration;
        storms.add(new ActiveStorm(owner.getUuid(), stack.copy(), expiry, world.getTime(), radius,
                damage * (float) tuning.get(s("ICEWHISPER_COMET_DAMAGE_MULTIPLIER"), 1), tuning, execution));
        IcewhisperAbilityManager.onStormStarted(owner, tuning, expiry);
        int globe = tuning.integer(s("ICEWHISPER_GLOBE_RESISTANCE_TICKS"), duration);
        if (tuning.flag(IcewhisperAbilityManager.MODE_SNOWGLOBE) && globe > 0
                && !tuning.flag(IcewhisperAbilityManager.MODE_BLACK_ICE)) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, globe,
                    tuning.integer(s("ICEWHISPER_GLOBE_AMPLIFIER"), 0)), owner);
        }
    }

    // Extinction Comet's absolute count is authoritative and suppresses Twin Wake's extra comet.
    public static int cometCount(Phase6AbilityTuning tuning, int configured, int waveIndex) {
        if (tuning.has(s("ICEWHISPER_COMET_COUNT"))) {
            return Math.clamp(tuning.integer(s("ICEWHISPER_COMET_COUNT"), configured), 0, MAX_COMETS_PER_WAVE);
        }
        int base = (int) Math.round(Math.max(0, configured)
                * tuning.get(s("ICEWHISPER_COMET_COUNT_MULTIPLIER"), 1));
        int interval = tuning.integer(s("ICEWHISPER_EXTRA_COMET_WAVE_INTERVAL"), 0);
        int extra = tuning.flag(IcewhisperAbilityManager.MODE_TWIN_WAKE) && interval > 0
                && (waveIndex + 1) % interval == 0
                ? tuning.integer(s("ICEWHISPER_EXTRA_COMET_COUNT"), 0) : 0;
        return Math.clamp(base + extra, 0, MAX_COMETS_PER_WAVE);
    }

    public static int waveInterval(Phase6AbilityTuning tuning, int configured) {
        return Math.max(1, tuning.integer(s("ICEWHISPER_WAVE_INTERVAL_TICKS"), Math.max(1, configured)));
    }

    public static int fallTicks(Phase6AbilityTuning tuning, int configured) {
        return Math.max(1, configured - tuning.integer(s("ICEWHISPER_FALL_REDUCTION_TICKS"), 0));
    }

    public static double splashRadius(Phase6AbilityTuning tuning, double configured) {
        return tuning.has(s("ICEWHISPER_SPLASH_RADIUS"))
                ? tuning.get(s("ICEWHISPER_SPLASH_RADIUS"), configured)
                : Math.max(0, configured) + tuning.get(s("ICEWHISPER_SPLASH_RADIUS_BONUS"), 0);
    }

    public static int stormDuration(Phase6AbilityTuning tuning, int configured) {
        return Math.max(1, configured + tuning.integer(s("ICEWHISPER_STORM_DURATION_BONUS_TICKS"), 0));
    }

    private static void spawnWave(ServerWorld world, LivingEntity owner, ActiveStorm storm) {
        Phase6AbilityTuning tuning = storm.tuning;
        int cometCount = cometCount(tuning, Math.max(0, Config.uniqueEffects.icewhisper.cometsPerWave),
                storm.waveIndex);
        storm.waveIndex++;
        if (cometCount <= 0 || owner == null) {
            return;
        }

        double radius = tuning.flag(IcewhisperAbilityManager.MODE_SNOWGLOBE)
                ? IcewhisperAbilityManager.auraRadius(owner, storm.radius) : storm.radius;
        int hunters = tuning.flag(IcewhisperAbilityManager.MODE_HUNTERS_SKY)
                ? Math.min(cometCount, tuning.integer(s("ICEWHISPER_HUNTER_COMET_COUNT"), 1)) : 0;
        boolean lastSnow = lastSnowReady(owner, tuning);

        for (int i = 0; i < cometCount; i++) {
            boolean self = false;
            Vec3d impact;
            if (lastSnow) {
                impact = groundAt(world, owner.getX(), owner.getZ(), owner.getY());
                lastSnow = false;
                self = true;
            } else if (i < hunters) {
                impact = hunterImpact(world, owner, tuning).orElseGet(
                        () -> chooseImpactPosition(world, owner.getPos(), radius));
            } else {
                impact = chooseImpactPosition(world, owner.getPos(), radius);
            }
            spawnComet(world, owner, storm, impact, self);
        }
    }

    private static boolean lastSnowReady(LivingEntity owner, Phase6AbilityTuning tuning) {
        if (!tuning.flag(IcewhisperAbilityManager.MODE_LAST_SNOW)) {
            return false;
        }
        double percent = tuning.get(s("ICEWHISPER_LAST_SNOW_HEALTH_PERCENT"), 0);
        if (percent <= 0 || owner.getMaxHealth() <= 0
                || owner.getHealth() > owner.getMaxHealth() * (percent / 100.0)) {
            return false;
        }
        return IcewhisperAbilityManager.consumeLastSnow(owner);
    }

    // Hunter's Sky retargets one comet onto the nearest enemy Permafrost is currently holding.
    private static Optional<Vec3d> hunterImpact(ServerWorld world, LivingEntity owner, Phase6AbilityTuning tuning) {
        double range = tuning.get(s("ICEWHISPER_HUNTER_RANGE"), 0);
        if (range <= 0) {
            return Optional.empty();
        }
        Box box = owner.getBoundingBox().expand(range);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (Entity entity : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(entity instanceof LivingEntity target)
                    || !HelperMethods.checkAbilityTarget(target, owner)
                    || !IcewhisperAbilityManager.isAuraAffected(owner, target)) {
                continue;
            }
            double distance = target.squaredDistanceTo(owner);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = target;
            }
        }
        return best == null ? Optional.empty()
                : Optional.of(groundAt(world, best.getX(), best.getZ(), best.getY()));
    }

    public static void tick(ServerWorld world) {
        tickStorms(world);
        IcewhisperAbilityManager.sweepAttackSlows(world);

        List<ActiveComet> comets = ACTIVE_COMETS.get(world);
        if (comets == null || comets.isEmpty()) {
            ACTIVE_COMETS.remove(world);
            if (PENDING_PURGE.remove(world)) {
                purgeOrphanCometVisuals(world);
            }
            return;
        }

        comets.removeIf(comet -> tickComet(world, comet));
        if (comets.isEmpty()) {
            ACTIVE_COMETS.remove(world);
            if (PENDING_PURGE.remove(world)) {
                purgeOrphanCometVisuals(world);
            }
        }
    }

    private static void tickStorms(ServerWorld world) {
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        storms.removeIf(storm -> {
            Entity ownerEntity = world.getEntity(storm.ownerId);
            if (world.getTime() >= storm.expiryTick
                    || !(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
                finishStorm(storm);
                return true;
            }
            if (world.getTime() >= storm.nextWaveTick) {
                spawnWave(world, owner, storm);
                storm.nextWaveTick = world.getTime()
                        + waveInterval(storm.tuning, Config.uniqueEffects.icewhisper.cometInterval);
            }
            return false;
        });

        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static void finishStorm(ActiveStorm storm) {
        if (storm.execution != null) {
            UniqueAbilityApi.finish(storm.execution, Phase6UniqueAbilities.FINISH, 0);
        }
        IcewhisperAbilityManager.onStormEnded(storm.ownerId);
    }

    private static void spawnComet(ServerWorld world, LivingEntity owner, ActiveStorm storm, Vec3d impact,
                                   boolean selfTargeted) {
        long startTick = world.getTime();
        int fallTicks = fallTicks(storm.tuning, Math.max(1, Config.uniqueEffects.icewhisper.cometFallTicks));
        Vec3d start = impact.add(
                -1.75 + world.random.nextDouble() * 3.5,
                SPAWN_HEIGHT,
                -1.75 + world.random.nextDouble() * 3.5
        );

        UUID visualId = null;
        if (Config.general.enableModernFieldEffects) {
            IcewhisperCometVisualEntity visual = new IcewhisperCometVisualEntity(world, start.x, start.y, start.z);
            visual.addCommandTag(COMET_VISUAL_TAG);
            world.spawnEntity(visual);
            visualId = visual.getUuid();
            PENDING_PURGE.add(world);
        }

        ActiveComet comet = new ActiveComet(owner.getUuid(), storm.stack, visualId, start, impact,
                startTick, fallTicks, storm.damage, selfTargeted, storm.tuning, storm.execution);
        ACTIVE_COMETS.computeIfAbsent(world, w -> new ArrayList<>()).add(comet);
    }

    private static boolean tickComet(ServerWorld world, ActiveComet comet) {
        long age = world.getTime() - comet.startTick();
        if (age >= comet.fallTicks()) {
            impact(world, comet);
            removeVisual(world, comet.visualId());
            return true;
        }

        Entity visual = comet.visualId() != null ? world.getEntity(comet.visualId()) : null;
        if (visual instanceof IcewhisperCometVisualEntity cometVisual) {
            double t = MathHelper.clamp((double) age / (double) comet.fallTicks(), 0.0, 1.0);
            double eased = t * t;
            Vec3d pos = comet.start().lerp(comet.impact(), eased);
            cometVisual.setPos(pos.x, pos.y, pos.z);
            cometVisual.setScale((float) (1.0 + t * 0.35));
        }
        return false;
    }

    private static void impact(ServerWorld world, ActiveComet comet) {
        Entity ownerEntity = world.getEntity(comet.ownerId());
        if (!(ownerEntity instanceof LivingEntity owner)) {
            return;
        }

        Phase6AbilityTuning tuning = comet.tuning;
        Vec3d impact = comet.impact();
        float splashRadius = (float) splashRadius(tuning,
                Math.max(0.0F, Config.uniqueEffects.icewhisper.cometSplashRadius));
        Box box = new Box(
                impact.x - splashRadius, impact.y - splashRadius, impact.z - splashRadius,
                impact.x + splashRadius, impact.y + splashRadius, impact.z + splashRadius
        );
        int affected = 0;
        int cap = tuning.has(s("ICEWHISPER_SPLASH_TARGET_CAP"))
                ? tuning.integer(s("ICEWHISPER_SPLASH_TARGET_CAP"), 8) : Integer.MAX_VALUE;
        boolean blackIce = tuning.flag(IcewhisperAbilityManager.MODE_BLACK_ICE);
        double blackIceMultiplier = tuning.get(s("ICEWHISPER_BLACK_ICE_MULTIPLIER"), 1);
        int blind = tuning.flag(IcewhisperAbilityManager.MODE_SNOWBLIND)
                ? tuning.integer(s("ICEWHISPER_BLIND_TICKS"), 30) : 0;
        double fracture = -1;
        for (Entity entity : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity target && HelperMethods.checkAbilityTarget(target, owner)
                    && target.squaredDistanceTo(impact) <= splashRadius * splashRadius) {
                if (fracture < 0) {
                    fracture = tuning.flag(IcewhisperAbilityManager.MODE_FRACTURE)
                            ? IcewhisperAbilityManager.fractureMultiplier(owner, world.getTime(), tuning) : 1;
                }
                boolean frozen = target.getFrozenTicks() > 0;
                float damage = comet.damage() * (float) fracture;
                if (blackIce && frozen) {
                    damage *= (float) blackIceMultiplier;
                }
                SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(
                        world, owner, comet.stack(), target, damage, SpellScalingProfile.FROST);
                if (blackIce && frozen) {
                    target.setFrozenTicks(0);
                }
                if (blind > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, blind, 0), owner);
                if (comet.execution != null) UniqueAbilityApi.emit(comet.execution, UniqueAbilityPhase.HIT,
                        Phase6UniqueAbilities.HIT, target, 1, damage);
                if (++affected >= cap) break;
            }
        }

        grantImpactBuffs(owner, comet, impact);

        if (Config.general.enableModernFieldEffects) {
            FrostfallIceSpikeFieldManager.createTargetBurst(world, impact, 5, 1.25F);
            world.spawnParticles(ParticleTypes.SNOWFLAKE, impact.x, impact.y + 0.15, impact.z, 28, 0.75, 0.18, 0.75, 0.04);
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, impact.x, impact.y + 0.25, impact.z, 16, 0.55, 0.18, 0.55, 0.08);
            world.spawnParticles(ParticleTypes.WHITE_ASH, impact.x, impact.y + 0.35, impact.z, 24, 0.65, 0.28, 0.65, 0.02);
            world.spawnParticles(ParticleTypes.POOF, impact.x, impact.y + 0.08, impact.z, 12, 0.55, 0.08, 0.55, 0.02);
            world.spawnParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.35, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.CLOUD, impact.x, impact.y + 0.12, impact.z, 20, 0.85, 0.12, 0.85, 0.05);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState()), impact.x, impact.y + 0.08, impact.z, 18, 0.45, 0.12, 0.45, 0.04);
            world.playSound(null, impact.x, impact.y, impact.z, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.65F, 0.65F + world.random.nextFloat() * 0.25F);
            world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.45F, 0.6F + world.random.nextFloat() * 0.25F);
            world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.35F, 1.35F + world.random.nextFloat() * 0.25F);
            world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.25F, 1.6F + world.random.nextFloat() * 0.25F);
        }
    }

    private static void grantImpactBuffs(LivingEntity owner, ActiveComet comet, Vec3d impact) {
        Phase6AbilityTuning tuning = comet.tuning;
        if (tuning.flag(IcewhisperAbilityManager.MODE_BLACK_ICE)) {
            return;
        }
        int speed = tuning.integer(s("ICEWHISPER_STEP_SPEED_TICKS"), 0);
        double stepRadius = tuning.get(s("ICEWHISPER_STEP_RADIUS"), 0);
        if (tuning.flag(IcewhisperAbilityManager.MODE_WINTER_STEP) && speed > 0 && stepRadius > 0
                && owner.squaredDistanceTo(impact) <= stepRadius * stepRadius) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, speed,
                    tuning.integer(s("ICEWHISPER_STEP_SPEED_AMPLIFIER"), 0)), owner);
        }
        int resistance = tuning.integer(s("ICEWHISPER_LAST_SNOW_RESISTANCE_TICKS"), 0);
        if (comet.selfTargeted() && tuning.flag(IcewhisperAbilityManager.MODE_LAST_SNOW) && resistance > 0) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistance,
                    tuning.integer(s("ICEWHISPER_LAST_SNOW_AMPLIFIER"), 1)), owner);
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) {
            return;
        }
        List<ActiveStorm> storms = ACTIVE_STORMS.remove(world);
        if (storms != null) {
            storms.forEach(IcewhisperCometManager::finishStorm);
        }
        List<ActiveComet> comets = ACTIVE_COMETS.remove(world);
        if (comets != null) {
            comets.forEach(comet -> removeVisual(world, comet.visualId()));
        }
        if (PENDING_PURGE.remove(world)) {
            purgeOrphanCometVisuals(world);
        }
        IcewhisperAbilityManager.clear(world);
    }

    public static void clearAll() {
        ACTIVE_STORMS.values().forEach(storms -> storms.forEach(IcewhisperCometManager::finishStorm));
        ACTIVE_STORMS.clear();
        ACTIVE_COMETS.clear();
        PENDING_PURGE.clear();
        IcewhisperAbilityManager.clearAll();
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        UUID id = actor.getUuid();
        ACTIVE_STORMS.values().forEach(storms -> storms.removeIf(storm -> {
            if (!storm.ownerId.equals(id)) {
                return false;
            }
            finishStorm(storm);
            return true;
        }));
        ACTIVE_STORMS.values().removeIf(List::isEmpty);
        if (actor.getWorld() instanceof ServerWorld world) {
            List<ActiveComet> comets = ACTIVE_COMETS.get(world);
            if (comets != null) {
                comets.removeIf(comet -> {
                    if (!comet.ownerId().equals(id)) {
                        return false;
                    }
                    removeVisual(world, comet.visualId());
                    return true;
                });
            }
        }
        IcewhisperAbilityManager.clearActor(actor);
    }

    private static void removeVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return;
        }
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static Vec3d chooseImpactPosition(ServerWorld world, Vec3d center, double radius) {
        double angle = world.random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(world.random.nextDouble()) * Math.max(1.0, radius);
        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        return groundAt(world, x, z, center.y);
    }

    private static Vec3d groundAt(ServerWorld world, double x, double z, double centerY) {
        return new Vec3d(x, findGroundTopY(world, x, z, centerY), z);
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }
        return centerY;
    }

    private static void purgeOrphanCometVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof IcewhisperCometVisualEntity && entity.getCommandTags().contains(COMET_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private record ActiveComet(UUID ownerId, ItemStack stack, UUID visualId, Vec3d start, Vec3d impact,
                               long startTick, int fallTicks, float damage, boolean selfTargeted,
                               Phase6AbilityTuning tuning, UniqueAbilityExecution execution) {
    }

    private static final class ActiveStorm {
        private final UUID ownerId;
        private final ItemStack stack;
        private final long expiryTick;
        private final double radius;
        private final float damage;
        private final Phase6AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private long nextWaveTick;
        private int waveIndex;

        private ActiveStorm(UUID ownerId, ItemStack stack, long expiryTick, long nextWaveTick, double radius,
                            float damage, Phase6AbilityTuning tuning, UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.expiryTick = expiryTick;
            this.nextWaveTick = nextWaveTick;
            this.radius = radius;
            this.damage = damage;
            this.tuning = tuning;
            this.execution = execution;
        }

    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
