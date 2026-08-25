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
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
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
    private static final String WAVE_VISUAL_TAG = "simplyswords_livyatan_wave_visual";
    private static final Map<ServerWorld, List<ActiveWave>> ACTIVE_WAVES = new HashMap<>();
    private static final Map<ServerWorld, List<WaveVisual>> RETURN_VISUALS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTIVATION = new HashMap<>();

    private LivyatanWaveManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        List<WaveVisual> returnVisuals = RETURN_VISUALS.get(world);
        return (waves != null && !waves.isEmpty())
                || (returnVisuals != null && !returnVisuals.isEmpty())
                || world.getTime() % 20L == 0L;
    }

    public static void tryFire(ServerWorld world, LivingEntity caster, net.minecraft.item.ItemStack stack) {
        if (world == null || caster == null || stack == null || stack.isEmpty() || !caster.isAlive()) {
            return;
        }
        UniqueAbilityExecution execution = Phase6CombatManager.beginPassive(
                Phase6UniqueAbilities.LIVYATAN_WAVE, world, stack, caster, null);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        if (tuning.flag(1 << 26) || !isAttackReady(world, caster, stack, tuning)) {
            UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, 0);
            return;
        }

        Vec3d look = caster.getRotationVec(1.0F);
        Vec3d horizontalForward = new Vec3d(look.x, 0.0, look.z);
        if (horizontalForward.lengthSquared() <= 1.0E-6) {
            horizontalForward = Vec3d.fromPolar(0.0F, caster.getYaw());
        } else {
            horizontalForward = horizontalForward.normalize();
        }

        Vec3d right = new Vec3d(-horizontalForward.z, 0.0, horizontalForward.x).normalize();
        Vec3d start = caster.getPos().add(horizontalForward.multiply(waveForwardStartOffset()));
        float damage = HelperMethods.abilityScaledDamage("frost", caster, stack,
                Config.uniqueEffects.livyatan.waveDamageScaling, Config.uniqueEffects.livyatan.spellScaling)
                * (float) tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        ActiveWave wave = new ActiveWave(start, horizontalForward, right, caster.getUuid(), stack.copy(),
                world.getTime(), tuning.integer(s("LENGTH"), baseLengthSteps()), damage,
                tuning.get(s("KNOCKBACK"), waveKnockback()), tuning.get(s("WIDTH"), waveWidthBlocks()),
                tuning, execution);
        ACTIVE_WAVES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(wave);

        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_DOLPHIN_SPLASH, SoundCategory.PLAYERS, 0.85F, 0.9F + world.random.nextFloat() * 0.15F);
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 0.8F, 0.9F + world.random.nextFloat() * 0.1F);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundRegistry.SWING_WOOSH.get(), SoundCategory.PLAYERS, 0.55F, 1.05F + world.random.nextFloat() * 0.1F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        if (waves == null || waves.isEmpty()) {
            animateReturnVisuals(world);
            List<WaveVisual> returnVisuals = RETURN_VISUALS.get(world);
            if ((returnVisuals == null || returnVisuals.isEmpty()) && world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        waves.removeIf(wave -> tickWave(world, wave) && wave.visuals.isEmpty());
        animateVisuals(world, waves);
        animateReturnVisuals(world);
        if (waves.isEmpty()) {
            ACTIVE_WAVES.remove(world);
        }
    }

    public static void spawnReturnPulse(ServerWorld world, Vec3d center, Vec3d forward, int step) {
        Vec3d horizontalForward = new Vec3d(forward.x, 0.0, forward.z);
        if (horizontalForward.lengthSquared() <= 1.0E-6) {
            horizontalForward = Vec3d.fromPolar(0.0F, 0.0F);
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3d right = new Vec3d(-horizontalForward.z, 0.0, horizontalForward.x).normalize();
        double groundY = findGroundTopY(world, center.x, center.z, center.y);
        Vec3d groundedCenter = new Vec3d(center.x, groundY, center.z);
        spawnWaveParticles(world, groundedCenter, right, waveWidthBlocks(), step);
        spawnWaveVisualSegments(world, groundedCenter, right, step, 1.55F, RETURN_VISUALS.computeIfAbsent(world, ignored -> new ArrayList<>()));
        if (step % 3 == 0) {
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.28F, 0.82F + world.random.nextFloat() * 0.18F);
        }
    }

    private static boolean tickWave(ServerWorld world, ActiveWave wave) {
        long age = world.getTime() - wave.spawnTick;
        if (age < 0 || age % waveStepIntervalTicks() != 0L) {
            return false;
        }

        int step = wave.currentStep++;
        if (step > wave.maxSteps) {
            UniqueAbilityApi.finish(wave.execution, Phase6UniqueAbilities.FINISH, wave.hitEntities.size());
            return true;
        }

        double travel = step * waveStepDistance();
        Vec3d center = wave.start.add(wave.forward.multiply(travel));
        spawnWaveParticles(world, center, wave, step);
        spawnWaveVisualSegments(world, center, wave, step);
        applyWaveDamage(world, center, wave);

        if (step % 2 == 0) {
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.35F, 1.15F + world.random.nextFloat() * 0.15F);
        }
        return step > wave.maxSteps;
    }

    private static void applyWaveDamage(ServerWorld world, Vec3d center, ActiveWave wave) {
        Entity ownerEntity = world.getEntity(wave.ownerId);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            return;
        }

        Box hitBox = Box.of(center.add(0.0, 0.5, 0.0), waveSegmentThickness() * 2.0, 2.2, wave.width);
        DamageSource damageSource = world.getDamageSources().indirectMagic(owner, owner);
        int affected = 0;
        int cap = wave.tuning.has(s("TARGET_CAP"))
                ? wave.tuning.integer(s("TARGET_CAP"), 8) : Integer.MAX_VALUE;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, hitBox, LivingEntity::isAlive)) {
            if (!wave.hitEntities.add(candidate.getUuid()) || !HelperMethods.checkAbilityTarget(candidate, owner)) {
                continue;
            }

            float rawDamage = wave.damage * (wave.currentStep >= wave.maxSteps
                    ? (float) wave.tuning.get(s("FINAL_DAMAGE_MULTIPLIER"), 1) : 1);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, wave.stack, candidate, damageSource, rawDamage);
            if (!HelperMethods.damageThroughIframes(candidate, damageSource, damage)) {
                continue;
            }
            Vec3d push = wave.forward.multiply(wave.knockback);
            candidate.addVelocity(push.x, waveKnockUp(), push.z);
            candidate.velocityModified = true;
            candidate.velocityDirty = true;
            int slow = wave.tuning.integer(s("STATUS_DURATION_TICKS"), 0);
            if (slow > 0) candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0), owner);
            UniqueAbilityApi.emit(wave.execution, UniqueAbilityPhase.HIT, Phase6UniqueAbilities.HIT,
                    candidate, 1, damage);
            if (++affected >= cap) break;
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
                                         Phase6AbilityTuning tuning) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            purgeOldSwingEntries(now);
        }
        int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, user,
                tuning.integer(s("COOLDOWN_TICKS"), getAttackReadyCooldownTicks(user)));
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

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof LivyatanWaveVisualEntity && entity.getCommandTags().contains(WAVE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveWave {
        private final Vec3d start;
        private final Vec3d forward;
        private final Vec3d right;
        private final UUID ownerId;
        private final net.minecraft.item.ItemStack stack;
        private final long spawnTick;
        private final int maxSteps;
        private final float damage;
        private final double knockback;
        private final double width;
        private final Phase6AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private final Set<UUID> hitEntities = new HashSet<>();
        private final List<WaveVisual> visuals = new ArrayList<>();
        private int currentStep;

        private ActiveWave(Vec3d start, Vec3d forward, Vec3d right, UUID ownerId,
                           net.minecraft.item.ItemStack stack, long spawnTick, int maxSteps, float damage,
                           double knockback, double width, Phase6AbilityTuning tuning,
                           UniqueAbilityExecution execution) {
            this.start = start;
            this.forward = forward;
            this.right = right;
            this.ownerId = ownerId;
            this.stack = stack;
            this.spawnTick = spawnTick;
            this.maxSteps = maxSteps;
            this.damage = damage;
            this.knockback = knockback;
            this.width = width;
            this.tuning = tuning;
            this.execution = execution;
            this.currentStep = 0;
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

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
