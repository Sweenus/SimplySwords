package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.LivyatanWaveVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LivyatanWaveManager {

    private static final int WAVE_PARTICLE_STRIPS = 9;
    private static final int VISUAL_RISE_TICKS = 3;
    private static final int VISUAL_HOLD_TICKS = 2;
    private static final int VISUAL_SINK_TICKS = 7;
    private static final double VISUAL_START_DEPTH = 1.15;
    private static final double VISUAL_LANE_OVERLAP = 0.02;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 12;
    private static final int CALM_VISUAL_INTERVAL_TICKS = 2;
    private static final String WAVE_VISUAL_TAG = "simplyswords_livyatan_wave_visual";
    private static final ChainLightningVisualManager.LightningVisualSettings UNBOUND_LIGHTNING_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0x83E8FF, 6, 0.055F, 4);
    private static final ChainLightningVisualManager.LightningVisualSettings CALM_LIGHTNING_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0x9CF4FF, 7, 0.065F, 3);
    private static final Map<ServerWorld, List<ActiveWave>> ACTIVE_WAVES = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveCalmBreaker>> ACTIVE_CALM_BREAKERS = new HashMap<>();
    private static final Map<ServerWorld, List<WaveVisual>> RETURN_VISUALS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTIVATION = new HashMap<>();

    private LivyatanWaveManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        List<ActiveCalmBreaker> breakers = ACTIVE_CALM_BREAKERS.get(world);
        List<WaveVisual> returnVisuals = RETURN_VISUALS.get(world);
        return (waves != null && !waves.isEmpty())
                || (breakers != null && !breakers.isEmpty())
                || (returnVisuals != null && !returnVisuals.isEmpty())
                || LivyatanAbilityManager.hasExpiringState(world);
    }

    public static void tryFire(ServerWorld world, LivingEntity caster, net.minecraft.item.ItemStack stack) {
        if (world == null || caster == null || stack == null || stack.isEmpty() || !caster.isAlive()) {
            return;
        }
        UniqueAbilityExecution execution = StormFrostWaterMasteryCombatManager.beginPassive(
                StormFrostWaterMasteryAbilities.LIVYATAN_WAVE, world, stack, caster, null);
        StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryAbilities.tuning(execution);
        if (!isAttackReady(world, caster, stack, tuning)) {
            UniqueAbilityApi.finish(execution, StormFrostWaterMasteryAbilities.FINISH, 0);
            return;
        }

        Vec3d look = caster.getRotationVec(1.0F);
        Vec3d horizontalForward = new Vec3d(look.x, 0.0, look.z);
        if (horizontalForward.lengthSquared() <= 1.0E-6) {
            horizontalForward = Vec3d.fromPolar(0.0F, caster.getYaw());
        } else {
            horizontalForward = horizontalForward.normalize();
        }

        Vec3d start = caster.getPos().add(horizontalForward.multiply(waveForwardStartOffset()));
        LivyatanAbilityManager.WavePlan plan = LivyatanAbilityManager.prepareWave(world, caster, tuning);
        String standardSchool = plan.lightning() ? "lightning" : "frost";
        float standardDamage = HelperMethods.abilityScaledDamage(standardSchool, caster, stack,
                Config.uniqueEffects.livyatan.waveDamageScaling, Config.uniqueEffects.livyatan.spellScaling)
                * (float) waveDamageMultiplier(tuning);
        int length = waveLength(baseLengthSteps(), tuning);
        double width = waveWidth(waveWidthBlocks(), tuning);
        double knockback = waveKnockback(waveKnockback(), tuning);
        int targetCap = tuning.integer(s("LIVYATAN_WALL_TARGET_CAP"),
                Config.uniqueEffects.livyatan.waveTargetCap);
        if (plan.calmBreaker()) {
            float breakerDamage = HelperMethods.abilityScaledDamage("lightning", caster, stack,
                    Config.uniqueEffects.livyatan.waveDamageScaling, Config.uniqueEffects.livyatan.spellScaling)
                    * (float) waveDamageMultiplier(tuning)
                    * (float) tuning.get(s("LIVYATAN_CALM_DAMAGE_MULTIPLIER"), 2);
            double breakerRadius = tuning.get(s("LIVYATAN_CALM_RADIUS"), 7);
            int breakerDuration = tuning.integer(s("LIVYATAN_CALM_DURATION_TICKS"), 12);
            int breakerTargetCap = tuning.integer(s("LIVYATAN_CALM_TARGET_CAP"), 16);
            ACTIVE_CALM_BREAKERS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                    new ActiveCalmBreaker(caster.getPos(), horizontalForward, caster.getUuid(), stack.copy(),
                            world.getTime(), Math.max(1, breakerDuration), Math.max(.5, breakerRadius),
                            breakerDamage, knockback, Math.max(1, breakerTargetCap), tuning, execution,
                            !plan.secondary()));
        } else {
            ACTIVE_WAVES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                    new ActiveWave(start, horizontalForward, caster.getUuid(), stack.copy(),
                            world.getTime(), Math.max(1, length), standardDamage, knockback,
                            Math.max(.5, width), Math.max(1, targetCap), plan.lightning(), tuning,
                            execution, !plan.secondary()));
        }
        if (plan.secondary()) {
            int delay = tuning.integer(s("LIVYATAN_DOUBLE_DELAY_TICKS"), 6);
            float secondaryBaseDamage = plan.calmBreaker()
                    ? HelperMethods.abilityScaledDamage("frost", caster, stack,
                    Config.uniqueEffects.livyatan.waveDamageScaling, Config.uniqueEffects.livyatan.spellScaling)
                    * (float) waveDamageMultiplier(tuning)
                    : standardDamage;
            float secondaryDamage = secondaryBaseDamage
                    * (float) tuning.get(s("LIVYATAN_DOUBLE_DAMAGE_MULTIPLIER"), .55);
            ACTIVE_WAVES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                    new ActiveWave(start, horizontalForward, caster.getUuid(), stack.copy(),
                    world.getTime() + delay, Math.max(1, length), secondaryDamage, knockback,
                    Math.max(.5, width), Math.max(1, targetCap), plan.lightning() && !plan.calmBreaker(),
                            tuning, execution, true));
        }

        playWaveStartSounds(world, caster, start, plan);
    }

    public static void tick(ServerWorld world) {
        LivyatanAbilityManager.tick(world);
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        if (waves != null && !waves.isEmpty()) {
            waves.removeIf(wave -> tickWave(world, wave) && wave.visuals.isEmpty());
            animateVisuals(world, waves);
            if (waves.isEmpty()) ACTIVE_WAVES.remove(world);
        }
        List<ActiveCalmBreaker> breakers = ACTIVE_CALM_BREAKERS.get(world);
        if (breakers != null && !breakers.isEmpty()) {
            breakers.removeIf(breaker -> tickCalmBreaker(world, breaker) && breaker.visuals.isEmpty());
            animateCalmBreakerVisuals(world, breakers);
            if (breakers.isEmpty()) ACTIVE_CALM_BREAKERS.remove(world);
        }
        animateReturnVisuals(world);
    }

    public static void spawnReturnPulse(ServerWorld world, Vec3d center, Vec3d forward, int step) {
        spawnReturnPulse(world, center, forward, step, waveWidthBlocks());
    }

    public static void spawnReturnPulse(ServerWorld world, Vec3d center, Vec3d forward, int step, double width) {
        Vec3d horizontalForward = new Vec3d(forward.x, 0.0, forward.z);
        if (horizontalForward.lengthSquared() <= 1.0E-6) {
            horizontalForward = Vec3d.fromPolar(0.0F, 0.0F);
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3d right = new Vec3d(-horizontalForward.z, 0.0, horizontalForward.x).normalize();
        double groundY = findGroundTopY(world, center.x, center.z, center.y);
        Vec3d groundedCenter = new Vec3d(center.x, groundY, center.z);
        spawnWaveParticles(world, groundedCenter, right, Math.max(.5, width), step);
        spawnWaveVisualSegments(world, groundedCenter, right, Math.max(.5, width), step, 1.55F,
                RETURN_VISUALS.computeIfAbsent(world, ignored -> new ArrayList<>()));
        if (step % 3 == 0) {
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.28F, 0.82F + world.random.nextFloat() * 0.18F);
        }
    }

    private static boolean tickWave(ServerWorld world, ActiveWave wave) {
        if (wave.completed) return true;
        long age = world.getTime() - wave.spawnTick;
        if (age < 0 || age % waveStepIntervalTicks() != 0L) {
            return false;
        }

        int step = wave.currentStep++;
        if (step > wave.maxSteps) {
            if (wave.finishesExecution) {
                UniqueAbilityApi.finish(wave.execution, StormFrostWaterMasteryAbilities.FINISH, wave.hitEntities.size());
            }
            wave.completed = true;
            return true;
        }

        Entity ownerEntity = world.getEntity(wave.ownerId);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            UniqueAbilityApi.cancel(wave.execution);
            wave.completed = true;
            return true;
        }
        if (step > 0) {
            wave.forward = LivyatanAbilityManager.steer(world, owner, wave.center, wave.forward, wave.tuning);
            wave.right = new Vec3d(-wave.forward.z, 0, wave.forward.x).normalize();
            wave.center = wave.center.add(wave.forward.multiply(waveStepDistance()));
        }
        Vec3d center = wave.center;
        if (wave.lightning) {
            spawnLightningWaveFront(world, center, wave, step);
        } else {
            spawnWaveParticles(world, center, wave, step);
            spawnWaveVisualSegments(world, center, wave, step);
        }
        applyWaveDamage(world, center, wave);

        if (step % 2 == 0) {
            if (wave.lightning) {
                world.playSound(null, center.x, center.y, center.z,
                        SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_01.get(), SoundCategory.PLAYERS,
                        0.28F, 1.25F + world.random.nextFloat() * 0.25F);
            } else {
                world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_SPLASH,
                        SoundCategory.PLAYERS, 0.35F, 1.15F + world.random.nextFloat() * 0.15F);
            }
        }
        return step > wave.maxSteps;
    }

    private static boolean tickCalmBreaker(ServerWorld world, ActiveCalmBreaker breaker) {
        if (breaker.completed) return true;
        long age = world.getTime() - breaker.spawnTick;
        if (age < 0) return false;
        int step = breaker.currentStep++;
        if (step >= breaker.duration) {
            if (breaker.finishesExecution) {
                UniqueAbilityApi.finish(breaker.execution, StormFrostWaterMasteryAbilities.FINISH,
                        breaker.hitEntities.size());
            }
            breaker.completed = true;
            world.playSound(null, breaker.center.x, breaker.center.y, breaker.center.z,
                    SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_03.get(), SoundCategory.PLAYERS,
                    0.8F, 0.85F + world.random.nextFloat() * 0.15F);
            return true;
        }

        Entity ownerEntity = world.getEntity(breaker.ownerId);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            UniqueAbilityApi.cancel(breaker.execution);
            breaker.completed = true;
            return true;
        }

        double previousRadius = breaker.radius * step / breaker.duration;
        double currentRadius = breaker.radius * (step + 1) / breaker.duration;
        applyCalmBreakerDamage(world, breaker, owner, previousRadius, currentRadius,
                step == breaker.duration - 1);
        if (step % CALM_VISUAL_INTERVAL_TICKS == 0 || step == breaker.duration - 1) {
            spawnCalmBreakerVisual(world, breaker, currentRadius, step);
            spawnCalmBreakerLightning(world, breaker.center, currentRadius);
        }
        return false;
    }

    private static void applyCalmBreakerDamage(ServerWorld world, ActiveCalmBreaker breaker,
                                                LivingEntity owner, double previousRadius,
                                                double currentRadius, boolean finalStep) {
        if (breaker.hitEntities.size() >= breaker.targetCap) return;
        double tolerance = Math.max(.65, breaker.radius / breaker.duration);
        double innerRadius = Math.max(0, previousRadius - tolerance);
        double outerRadius = currentRadius + tolerance;
        Box hitBox = Box.of(breaker.center.add(0, .75, 0), outerRadius * 2, 3.5, outerRadius * 2);
        DamageSource damageSource = world.getDamageSources().indirectMagic(owner, owner);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, hitBox, LivingEntity::isAlive)
                .stream()
                .filter(target -> HelperMethods.checkAbilityTarget(target, owner))
                .filter(target -> !breaker.hitEntities.contains(target.getUuid()))
                .filter(target -> {
                    double distance = horizontalDistance(target.getPos(), breaker.center);
                    return distance >= innerRadius && distance <= outerRadius;
                })
                .sorted(java.util.Comparator.comparingDouble(target -> target.squaredDistanceTo(breaker.center)))
                .toList();
        for (LivingEntity target : targets) {
            if (breaker.hitEntities.size() >= breaker.targetCap) break;
            breaker.hitEntities.add(target.getUuid());
            float rawDamage = breaker.damage
                    * (float) LivyatanAbilityManager.waveDamageMultiplier(world, owner, target, breaker.tuning)
                    * (finalStep
                    ? (float) breaker.tuning.get(s("LIVYATAN_WAVE_FINAL_DAMAGE_MULTIPLIER"), 1) : 1);
            float damage = HelperMethods.applyAbilityDamageEnchantments(
                    world, breaker.stack, target, damageSource, rawDamage);
            if (!HelperMethods.damageThroughIframes(target, damageSource, damage)) continue;
            Vec3d push = target.getPos().subtract(breaker.center).multiply(1, 0, 1);
            if (push.lengthSquared() <= 1.0E-6) push = breaker.fallbackDirection;
            push = push.normalize().multiply(breaker.knockback);
            target.addVelocity(push.x, waveKnockUp(), push.z);
            target.velocityModified = true;
            target.velocityDirty = true;
            int slow = breaker.tuning.integer(s("LIVYATAN_WAVE_SLOW_TICKS"), 0);
            if (slow > 0) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0), owner);
            }
            spawnLightningImpact(world, target);
            UniqueAbilityApi.emit(breaker.execution, UniqueAbilityPhase.HIT,
                    StormFrostWaterMasteryAbilities.HIT, target, 1, damage);
            LivyatanAbilityManager.recordWaveHit(world, owner, breaker.stack, target, breaker.tuning);
        }
    }

    private static void spawnCalmBreakerVisual(ServerWorld world, ActiveCalmBreaker breaker,
                                               double radius, int step) {
        if (radius <= .1) return;
        double baseY = findGroundTopY(world, breaker.center.x, breaker.center.z, breaker.center.y);
        int laneCount = Math.clamp((int) Math.ceil(Math.PI * 2 * radius), 8, 48);
        List<LivyatanWaveVisualEntity.WaveLane> lanes = new ArrayList<>(laneCount);
        for (int lane = 0; lane < laneCount; lane++) {
            double angle = Math.PI * 2 * lane / laneCount;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double laneY = findGroundTopY(world, breaker.center.x + offsetX,
                    breaker.center.z + offsetZ, breaker.center.y);
            float crestHeight = 1.25F + (float) Math.sin(step * .7 + angle * 2) * .2F;
            lanes.add(new LivyatanWaveVisualEntity.WaveLane(
                    (float) offsetX, (float) (laneY - baseY), (float) offsetZ, crestHeight));
        }
        LivyatanWaveVisualEntity visual = new LivyatanWaveVisualEntity(world, breaker.center.x,
                baseY - VISUAL_START_DEPTH, breaker.center.z, 1.45F);
        visual.setWaveLanes(lanes);
        visual.addCommandTag(WAVE_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            breaker.visuals.add(new WaveVisual(visual.getUuid(), breaker.center.x, baseY,
                    breaker.center.z, world.getTime(), step));
        }
    }

    private static void spawnCalmBreakerLightning(ServerWorld world, Vec3d center, double radius) {
        if (radius < .4) return;
        int segments = Math.clamp((int) Math.ceil(Math.PI * radius), 8, 20);
        double baseY = findGroundTopY(world, center.x, center.z, center.y) + .65;
        Vec3d first = null;
        Vec3d previous = null;
        for (int segment = 0; segment < segments; segment++) {
            double angle = Math.PI * 2 * segment / segments;
            Vec3d point = new Vec3d(center.x + Math.cos(angle) * radius,
                    baseY + Math.sin(segment * 1.7) * .18,
                    center.z + Math.sin(angle) * radius);
            if (first == null) first = point;
            if (previous != null) {
                ChainLightningVisualManager.spawnBolt(world, previous, point,
                        CALM_LIGHTNING_SETTINGS, false);
            }
            previous = point;
        }
        if (previous != null && first != null) {
            ChainLightningVisualManager.spawnBolt(world, previous, first,
                    CALM_LIGHTNING_SETTINGS, false);
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, baseY, center.z,
                Math.max(12, segments), radius * .55, .25, radius * .55, .04);
    }

    private static void applyWaveDamage(ServerWorld world, Vec3d center, ActiveWave wave) {
        Entity ownerEntity = world.getEntity(wave.ownerId);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) return;

        Box hitBox = Box.of(center.add(0.0, 0.5, 0.0), waveSegmentThickness() * 2.0, 2.2, wave.width);
        DamageSource damageSource = world.getDamageSources().indirectMagic(owner, owner);
        int affected = 0;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, hitBox, LivingEntity::isAlive)) {
            if (!wave.hitEntities.add(candidate.getUuid()) || !HelperMethods.checkAbilityTarget(candidate, owner)) {
                continue;
            }

            float rawDamage = wave.damage
                    * (float) LivyatanAbilityManager.waveDamageMultiplier(world, owner, candidate, wave.tuning)
                    * (wave.currentStep - 1 == wave.maxSteps
                    ? (float) wave.tuning.get(s("LIVYATAN_WAVE_FINAL_DAMAGE_MULTIPLIER"), 1) : 1);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, wave.stack, candidate, damageSource, rawDamage);
            if (!HelperMethods.damageThroughIframes(candidate, damageSource, damage)) {
                continue;
            }
            Vec3d push = wave.forward.multiply(wave.knockback);
            candidate.addVelocity(push.x, waveKnockUp(), push.z);
            candidate.velocityModified = true;
            candidate.velocityDirty = true;
            int slow = wave.tuning.integer(s("LIVYATAN_WAVE_SLOW_TICKS"), 0);
            if (slow > 0) candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0), owner);
            UniqueAbilityApi.emit(wave.execution, UniqueAbilityPhase.HIT, StormFrostWaterMasteryAbilities.HIT,
                    candidate, 1, damage);
            LivyatanAbilityManager.recordWaveHit(world, owner, wave.stack, candidate, wave.tuning);
            if (wave.lightning) spawnLightningImpact(world, candidate);
            if (++affected >= wave.targetCap) break;
        }
    }

    private static void spawnWaveVisualSegments(ServerWorld world, Vec3d center, ActiveWave wave, int step) {
        spawnWaveVisualSegments(world, center, wave.right, wave.width, step, 2.0F, wave.visuals);
    }

    private static void spawnWaveVisualSegments(ServerWorld world, Vec3d center, Vec3d right, int step, float crestHeight, List<WaveVisual> visuals) {
        spawnWaveVisualSegments(world, center, right, waveWidthBlocks(), step, crestHeight, visuals);
    }

    private static void spawnWaveVisualSegments(ServerWorld world, Vec3d center, Vec3d right, double width,
                                                int step, float crestHeight, List<WaveVisual> visuals) {
        int laneCount = Math.max(1, (int) Math.ceil(width));
        double laneSpacing = visualLaneSpacing();
        for (int lane = 0; lane < laneCount; lane++) {
            float laneCenter = (laneCount - 1) * 0.5F;
            float laneOffsetUnits = lane - laneCenter;
            Vec3d lanePos = center.add(right.multiply(laneOffsetUnits * laneSpacing));
            double groundY = findGroundTopY(world, lanePos.x, lanePos.z, lanePos.y);
            float laneFalloff = 1.0F - Math.abs(laneOffsetUnits) * 0.18F;
            float targetHeight = Math.max(0.35F, crestHeight * laneFalloff);
            spawnVisualSegment(world, visuals, lanePos.x, groundY, lanePos.z, targetHeight, step);
        }
    }

    private static void spawnVisualSegment(ServerWorld world, List<WaveVisual> visuals, double x, double y, double z, float targetHeight, int spawnStep) {
        LivyatanWaveVisualEntity visual = new LivyatanWaveVisualEntity(world, x, y - VISUAL_START_DEPTH, z, targetHeight);
        visual.addCommandTag(WAVE_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            visuals.add(new WaveVisual(visual.getUuid(), x, y, z, world.getTime(), spawnStep));
        }
    }

    private static void animateVisuals(ServerWorld world, List<ActiveWave> waves) {
        for (ActiveWave wave : waves) {
            wave.visuals.removeIf(visual -> {
                Entity entity = world.getEntity(visual.id);
                if (!(entity instanceof LivyatanWaveVisualEntity waveVisual)) {
                    return true;
                }

                long age = world.getTime() - visual.spawnTick;
                if (age >= visualLifetimeTicks()) {
                    waveVisual.discard();
                    return true;
                }

                float baseScale = getHeightScale(age);
                int behindSteps = Math.max(0, wave.currentStep - visual.spawnStep);
                float trailFactor = Math.max(0.26F, 1.0F - behindSteps * 0.16F);
                float finalScale = Math.max(0.0F, baseScale * trailFactor);
                waveVisual.setHeightScale(finalScale);
                waveVisual.setPos(visual.baseX, visual.baseY + getVerticalOffset(age), visual.baseZ);
                return false;
            });
        }
    }

    private static void animateCalmBreakerVisuals(ServerWorld world, List<ActiveCalmBreaker> breakers) {
        for (ActiveCalmBreaker breaker : breakers) {
            breaker.visuals.removeIf(visual -> {
                int behindSteps = Math.max(0, breaker.currentStep - visual.spawnStep);
                float trailFactor = Math.max(.3F, 1.0F - behindSteps * .12F);
                return animateVisual(world, visual, trailFactor);
            });
        }
    }

    private static void animateReturnVisuals(ServerWorld world) {
        List<WaveVisual> visuals = RETURN_VISUALS.get(world);
        if (visuals == null || visuals.isEmpty()) {
            return;
        }
        visuals.removeIf(visual -> animateVisual(world, visual, 1.0F));
        if (visuals.isEmpty()) {
            RETURN_VISUALS.remove(world);
        }
    }

    private static boolean animateVisual(ServerWorld world, WaveVisual visual, float trailFactor) {
        Entity entity = world.getEntity(visual.id);
        if (!(entity instanceof LivyatanWaveVisualEntity waveVisual)) {
            return true;
        }

        long age = world.getTime() - visual.spawnTick;
        if (age >= visualLifetimeTicks()) {
            waveVisual.discard();
            return true;
        }

        float baseScale = getHeightScale(age);
        float finalScale = Math.max(0.0F, baseScale * trailFactor);
        waveVisual.setHeightScale(finalScale);
        waveVisual.setPos(visual.baseX, visual.baseY + getVerticalOffset(age), visual.baseZ);
        return false;
    }

    private static void spawnWaveParticles(ServerWorld world, Vec3d center, ActiveWave wave, int step) {
        spawnWaveParticles(world, center, wave.right, wave.width, step);
        if (wave.lightning) {
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + .45, center.z,
                    6, wave.width * .3, .15, .2, .03);
        }
    }

    private static void spawnLightningWaveFront(ServerWorld world, Vec3d center,
                                                ActiveWave wave, int step) {
        double groundY = findGroundTopY(world, center.x, center.z, center.y);
        Vec3d grounded = new Vec3d(center.x, groundY, center.z);
        Vec3d lateral = wave.right.multiply(wave.width * .5);
        Vec3d lowerStart = grounded.subtract(lateral).add(0, .32, 0);
        Vec3d lowerEnd = grounded.add(lateral).add(0, .32, 0);
        Vec3d upperStart = lowerStart.add(0, .75, 0);
        Vec3d upperEnd = lowerEnd.add(0, .75, 0);
        ChainLightningVisualManager.spawnBolt(world, lowerStart, lowerEnd,
                UNBOUND_LIGHTNING_SETTINGS, false);
        ChainLightningVisualManager.spawnBolt(world, upperEnd, upperStart,
                UNBOUND_LIGHTNING_SETTINGS, false);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, grounded.x, grounded.y + .65, grounded.z,
                14, wave.width * .38, .34, .18, .06);
        world.spawnParticles(ParticleTypes.END_ROD, grounded.x, grounded.y + .7, grounded.z,
                4, wave.width * .28, .24, .12, .025);
        if (step % 2 == 0) {
            TemporaryWorldLightManager.placeBoltLights(world, lowerStart, lowerEnd, 4, 2);
        }
    }

    private static void spawnLightningImpact(ServerWorld world, LivingEntity target) {
        Vec3d position = target.getPos().add(0, Math.max(.45, target.getHeight() * .58), 0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z,
                10, .24, .24, .24, .08);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, position.x, position.y, position.z,
                5, .18, .18, .18, .03);
        world.playSound(null, target.getBlockPos(), SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get(),
                SoundCategory.PLAYERS, .3F, 1.15F + world.random.nextFloat() * .35F);
    }

    private static void playWaveStartSounds(ServerWorld world, LivingEntity caster, Vec3d start,
                                            LivyatanAbilityManager.WavePlan plan) {
        if (plan.calmBreaker()) {
            world.playSound(null, caster.getBlockPos(),
                    SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(), SoundCategory.PLAYERS,
                    .8F, .72F + world.random.nextFloat() * .12F);
            world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
                    SoundCategory.PLAYERS, 1.0F, .7F + world.random.nextFloat() * .1F);
            return;
        }
        if (plan.lightning()) {
            world.playSound(null, caster.getBlockPos(),
                    SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(), SoundCategory.PLAYERS,
                    .65F, 1.05F + world.random.nextFloat() * .18F);
            world.playSound(null, caster.getBlockPos(), SoundRegistry.SWING_WOOSH.get(),
                    SoundCategory.PLAYERS, .45F, 1.25F + world.random.nextFloat() * .12F);
            return;
        }
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_DOLPHIN_SPLASH,
                SoundCategory.PLAYERS, .85F, .9F + world.random.nextFloat() * .15F);
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
                SoundCategory.PLAYERS, .8F, .9F + world.random.nextFloat() * .1F);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundRegistry.SWING_WOOSH.get(),
                SoundCategory.PLAYERS, .55F, 1.05F + world.random.nextFloat() * .1F);
    }

    private static void spawnWaveParticles(ServerWorld world, Vec3d center, Vec3d right, double width, int step) {
        double halfWidth = width * 0.5;
        for (int i = 0; i < WAVE_PARTICLE_STRIPS; i++) {
            double t = WAVE_PARTICLE_STRIPS == 1 ? 0.0 : (double) i / (WAVE_PARTICLE_STRIPS - 1);
            double lateral = (t - 0.5) * width;
            Vec3d edgePoint = center.add(right.multiply(lateral));
            double crest = 0.22 + Math.sin((step * 0.55) + (t * Math.PI)) * 0.08;
            world.spawnParticles(ParticleTypes.SPLASH, edgePoint.x, edgePoint.y + crest, edgePoint.z, 1, 0.02, 0.03, 0.02, 0.12);
            world.spawnParticles(ParticleTypes.BUBBLE, edgePoint.x, edgePoint.y + crest - 0.1, edgePoint.z, 1, 0.02, 0.03, 0.02, 0.0);
            world.spawnParticles(ParticleTypes.BUBBLE_POP, edgePoint.x, edgePoint.y + crest + 0.02, edgePoint.z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 2 == 0) {
                world.spawnParticles(ParticleTypes.FALLING_WATER, edgePoint.x, edgePoint.y + crest + 0.06, edgePoint.z, 1, 0.01, 0.02, 0.01, 0.0);
            }
            if (i % 3 != 1) {
                world.spawnParticles(ParticleTypes.WHITE_ASH, edgePoint.x, edgePoint.y + crest + 0.08, edgePoint.z, 1, 0.03, 0.02, 0.03, 0.0);
            }
        }

        world.spawnParticles(ParticleTypes.BUBBLE_POP, center.x, center.y + 0.28, center.z, 4, halfWidth * 0.33, 0.09, 0.18, 0.0);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.24, center.z, 2, halfWidth * 0.28, 0.08, 0.16, 0.0);
        if (step % 2 == 0) {
            world.spawnParticles(ParticleTypes.FISHING, center.x, center.y + 0.32, center.z, 2, halfWidth * 0.35, 0.08, 0.2, 0.0);
        }
        if (step % 3 == 0) {
            world.spawnParticles(ParticleTypes.WAX_OFF, center.x, center.y + 0.26, center.z, 2, halfWidth * 0.25, 0.06, 0.12, 0.0);
        }
    }

    private static boolean isAttackReady(ServerWorld world, LivingEntity user, ItemStack stack,
                                         StormFrostWaterMasteryTuning tuning) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            purgeOldSwingEntries(now);
        }
        int cooldown = swingCooldown(getAttackReadyCooldownTicks(user), tuning);
        cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, user, Math.max(1, cooldown));
        if (RunicSlashManager.isIgnoringAttackReady()) {
            LAST_ACTIVATION.put(user.getUuid(), now + cooldown);
            return true;
        }
        Long nextEligible = LAST_ACTIVATION.get(user.getUuid());
        if (nextEligible != null && now < nextEligible) {
            return false;
        }
        LAST_ACTIVATION.put(user.getUuid(), now + cooldown);
        return true;
    }

    private static int getAttackReadyCooldownTicks(LivingEntity user) {
        EntityAttributeInstance attackSpeedAttribute = user.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        double attackSpeed = attackSpeedAttribute != null ? attackSpeedAttribute.getValue() : 4.0;
        if (attackSpeed <= 0.0) {
            attackSpeed = 4.0;
        }
        return Math.max(Config.uniqueEffects.livyatan.swingWaveMinimumCooldownTicks, (int) Math.ceil(20.0 / attackSpeed));
    }

    private static void purgeOldSwingEntries(long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = LAST_ACTIVATION.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private static float getHeightScale(long age) {
        if (age < 0L) {
            return 0.0F;
        }
        if (age < VISUAL_RISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) VISUAL_RISE_TICKS, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS) {
            return 1.0F;
        }
        long sinkAge = age - VISUAL_RISE_TICKS - VISUAL_HOLD_TICKS;
        float t = MathHelper.clamp((float) sinkAge / (float) VISUAL_SINK_TICKS, 0.0F, 1.0F);
        return 1.0F - (t * t * t);
    }

    private static double getVerticalOffset(long age) {
        if (age < VISUAL_RISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) VISUAL_RISE_TICKS, 0.0F, 1.0F);
            return -VISUAL_START_DEPTH + (VISUAL_START_DEPTH * easeOutBack(t));
        }
        if (age < VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS) {
            return 0.0;
        }
        long sinkAge = age - VISUAL_RISE_TICKS - VISUAL_HOLD_TICKS;
        float t = MathHelper.clamp((float) sinkAge / (float) VISUAL_SINK_TICKS, 0.0F, 1.0F);
        return -(VISUAL_START_DEPTH * t * t * t);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    public static double waveWidthBlocks() {
        return Math.max(0.5, Config.uniqueEffects.livyatan.waveWidthBlocks);
    }

    public static int visualLaneCount() {
        return Math.max(1, (int) Math.ceil(waveWidthBlocks()));
    }

    public static double visualLaneSpacing() {
        return Math.max(0.1, 1.0 - VISUAL_LANE_OVERLAP);
    }

    private static double waveSegmentThickness() {
        return Math.max(0.25, Config.uniqueEffects.livyatan.waveSegmentThickness);
    }

    private static double waveStepDistance() {
        return Math.max(0.1, Config.uniqueEffects.livyatan.waveStepDistance);
    }

    private static int waveStepIntervalTicks() {
        return Math.max(1, Config.uniqueEffects.livyatan.waveStepIntervalTicks);
    }

    private static double waveForwardStartOffset() {
        return Math.max(0.1, Config.uniqueEffects.livyatan.waveForwardStartOffset);
    }

    private static int baseLengthSteps() {
        return Math.max(1, Config.uniqueEffects.livyatan.waveLengthSteps);
    }

    private static double waveKnockback() {
        return Math.max(0.0, Config.uniqueEffects.livyatan.waveKnockback);
    }

    private static double waveKnockUp() {
        return Math.max(0.0, Config.uniqueEffects.livyatan.waveKnockUp);
    }

    private static int visualLifetimeTicks() {
        return VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS + VISUAL_SINK_TICKS;
    }

    public static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
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

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    public static void clear(ServerWorld world) {
        List<ActiveWave> waves = ACTIVE_WAVES.remove(world);
        if (waves != null) waves.forEach(wave -> {
            UniqueAbilityApi.cancel(wave.execution);
            wave.visuals.forEach(visual -> {
                Entity entity = world.getEntity(visual.id);
                if (entity != null) entity.discard();
            });
        });
        List<ActiveCalmBreaker> breakers = ACTIVE_CALM_BREAKERS.remove(world);
        if (breakers != null) breakers.forEach(breaker -> {
            UniqueAbilityApi.cancel(breaker.execution);
            breaker.visuals.forEach(visual -> {
                Entity entity = world.getEntity(visual.id);
                if (entity != null) entity.discard();
            });
        });
        List<WaveVisual> visuals = RETURN_VISUALS.remove(world);
        if (visuals != null) visuals.forEach(visual -> {
            Entity entity = world.getEntity(visual.id);
            if (entity != null) entity.discard();
        });
        LivyatanAbilityManager.clear(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (!(actor.getWorld() instanceof ServerWorld world)) return;
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        if (waves != null) {
            waves.removeIf(wave -> {
                if (!wave.ownerId.equals(actor.getUuid())) return false;
                UniqueAbilityApi.cancel(wave.execution);
                wave.visuals.forEach(visual -> {
                    Entity entity = world.getEntity(visual.id);
                    if (entity != null) entity.discard();
                });
                return true;
            });
            if (waves.isEmpty()) ACTIVE_WAVES.remove(world);
        }
        List<ActiveCalmBreaker> breakers = ACTIVE_CALM_BREAKERS.get(world);
        if (breakers != null) {
            breakers.removeIf(breaker -> {
                if (!breaker.ownerId.equals(actor.getUuid())) return false;
                UniqueAbilityApi.cancel(breaker.execution);
                breaker.visuals.forEach(visual -> {
                    Entity entity = world.getEntity(visual.id);
                    if (entity != null) entity.discard();
                });
                return true;
            });
            if (breakers.isEmpty()) ACTIVE_CALM_BREAKERS.remove(world);
        }
        LAST_ACTIVATION.remove(actor.getUuid());
        LivyatanAbilityManager.clearActor(actor);
    }

    public static void clearAll() {
        new ArrayList<>(ACTIVE_WAVES.keySet()).forEach(LivyatanWaveManager::clear);
        new ArrayList<>(ACTIVE_CALM_BREAKERS.keySet()).forEach(LivyatanWaveManager::clear);
        new ArrayList<>(RETURN_VISUALS.keySet()).forEach(LivyatanWaveManager::clear);
        LAST_ACTIVATION.clear();
        LivyatanAbilityManager.clearAll();
    }

    public static double waveDamageMultiplier(StormFrostWaterMasteryTuning tuning) {
        return tuning.get(s("LIVYATAN_WAVE_DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_WALL_DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_LANCE_DAMAGE_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_UNBOUND_WAVE_DAMAGE_MULTIPLIER"), 1);
    }

    public static double waveWidth(double configured, StormFrostWaterMasteryTuning tuning) {
        return tuning.has(s("LIVYATAN_LANCE_WIDTH"))
                ? tuning.get(s("LIVYATAN_LANCE_WIDTH"), configured)
                : configured + tuning.get(s("LIVYATAN_WAVE_WIDTH_BONUS"), 0)
                + tuning.get(s("LIVYATAN_WALL_WIDTH_BONUS"), 0);
    }

    public static int waveLength(int configured, StormFrostWaterMasteryTuning tuning) {
        return Math.max(1, (int) Math.round((configured
                + tuning.get(s("LIVYATAN_WAVE_LENGTH_BONUS_STEPS"), 0))
                * tuning.get(s("LIVYATAN_LANCE_LENGTH_MULTIPLIER"), 1)));
    }

    public static double waveKnockback(double configured, StormFrostWaterMasteryTuning tuning) {
        return configured * tuning.get(s("LIVYATAN_WAVE_KNOCKBACK_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_WALL_KNOCKBACK_MULTIPLIER"), 1)
                * tuning.get(s("LIVYATAN_LANCE_KNOCKBACK_MULTIPLIER"), 1);
    }

    public static int swingCooldown(int configured, StormFrostWaterMasteryTuning tuning) {
        return Math.max(1, (int) Math.round(configured
                * tuning.get(s("LIVYATAN_UNBOUND_COOLDOWN_MULTIPLIER"), 1)));
    }

    private static final class ActiveWave {
        private Vec3d center;
        private Vec3d forward;
        private Vec3d right;
        private final UUID ownerId;
        private final net.minecraft.item.ItemStack stack;
        private final long spawnTick;
        private final int maxSteps;
        private final float damage;
        private final double knockback;
        private final double width;
        private final int targetCap;
        private final boolean lightning;
        private final StormFrostWaterMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final boolean finishesExecution;
        private final Set<UUID> hitEntities = new HashSet<>();
        private final List<WaveVisual> visuals = new ArrayList<>();
        private int currentStep;
        private boolean completed;

        private ActiveWave(Vec3d start, Vec3d forward, UUID ownerId,
                           net.minecraft.item.ItemStack stack, long spawnTick, int maxSteps, float damage,
                           double knockback, double width, int targetCap, boolean lightning,
                           StormFrostWaterMasteryTuning tuning, UniqueAbilityExecution execution,
                           boolean finishesExecution) {
            this.center = start;
            this.forward = forward;
            this.right = new Vec3d(-forward.z, 0, forward.x).normalize();
            this.ownerId = ownerId;
            this.stack = stack;
            this.spawnTick = spawnTick;
            this.maxSteps = maxSteps;
            this.damage = damage;
            this.knockback = knockback;
            this.width = width;
            this.targetCap = targetCap;
            this.lightning = lightning;
            this.tuning = tuning;
            this.execution = execution;
            this.finishesExecution = finishesExecution;
            this.currentStep = 0;
        }
    }

    private static final class ActiveCalmBreaker {
        private final Vec3d center;
        private final Vec3d fallbackDirection;
        private final UUID ownerId;
        private final ItemStack stack;
        private final long spawnTick;
        private final int duration;
        private final double radius;
        private final float damage;
        private final double knockback;
        private final int targetCap;
        private final StormFrostWaterMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final boolean finishesExecution;
        private final Set<UUID> hitEntities = new HashSet<>();
        private final List<WaveVisual> visuals = new ArrayList<>();
        private int currentStep;
        private boolean completed;

        private ActiveCalmBreaker(Vec3d center, Vec3d fallbackDirection, UUID ownerId,
                                  ItemStack stack, long spawnTick, int duration, double radius,
                                  float damage, double knockback, int targetCap,
                                  StormFrostWaterMasteryTuning tuning,
                                  UniqueAbilityExecution execution, boolean finishesExecution) {
            this.center = center;
            this.fallbackDirection = fallbackDirection;
            this.ownerId = ownerId;
            this.stack = stack;
            this.spawnTick = spawnTick;
            this.duration = duration;
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.targetCap = targetCap;
            this.tuning = tuning;
            this.execution = execution;
            this.finishesExecution = finishesExecution;
        }
    }

    private static final class WaveVisual {
        private final UUID id;
        private final double baseX;
        private final double baseY;
        private final double baseZ;
        private final long spawnTick;
        private final int spawnStep;

        private WaveVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick, int spawnStep) {
            this.id = id;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
            this.spawnTick = spawnTick;
            this.spawnStep = spawnStep;
        }
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
