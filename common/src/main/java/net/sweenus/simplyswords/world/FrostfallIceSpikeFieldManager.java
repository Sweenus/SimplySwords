package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.FrostfallIceSpikeVisualEntity;

import java.util.*;

public final class FrostfallIceSpikeFieldManager {

    private static final int RAISE_TICKS = 7;
    private static final int HOLD_TICKS = 2;
    private static final int SINK_TICKS = 11;
    private static final int VISUAL_DURATION_TICKS = RAISE_TICKS + HOLD_TICKS + SINK_TICKS;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 16;
    private static final double START_DEPTH = 2.6;
    private static final double BASE_GROUND_OFFSET = -0.12;
    private static final double SPIKE_SEGMENT_HEIGHT = 0.24;
    private static final int MIN_RING_POINTS = 12;
    private static final int MAX_RING_POINTS = 48;
    private static final String ICE_SPIKE_VISUAL_TAG = "simplyswords_frostfall_ice_spike_visual";

    private static final Map<ServerWorld, List<ActiveIceSpikePulse>> ACTIVE_PULSES = new HashMap<>();

    private FrostfallIceSpikeFieldManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveIceSpikePulse> pulses = ACTIVE_PULSES.get(world);
        return (pulses != null && !pulses.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void createPulse(ServerWorld world, Vec3d center, double radius, int detonationIndex) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }

        double visualRadius = Math.max(0.75, radius);
        List<ActiveIceSpikePulse> pulses = ACTIVE_PULSES.computeIfAbsent(world, w -> new ArrayList<>());
        long now = world.getTime();
        ActiveIceSpikePulse pulse = new ActiveIceSpikePulse(now + VISUAL_DURATION_TICKS);
        spawnSpikeRing(world, pulse, center, visualRadius, detonationIndex, now);
        pulses.add(pulse);

        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.12, center.z, 10, visualRadius * 0.25, 0.08, visualRadius * 0.25, 0.01);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState()), center.x, center.y + 0.08, center.z, 12, visualRadius * 0.18, 0.08, visualRadius * 0.18, 0.01);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.35F, 0.55F + world.random.nextFloat() * 0.2F);
    }

    public static void createTargetBurst(ServerWorld world, Vec3d center, int spikeCount, float heightScale) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }

        int pointCount = MathHelper.clamp(spikeCount, 1, 8);
        long now = world.getTime();
        ActiveIceSpikePulse pulse = new ActiveIceSpikePulse(now + VISUAL_DURATION_TICKS);
        double phase = world.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < pointCount; i++) {
            double angle = phase + ((Math.PI * 2.0) / pointCount) * i;
            double distance = pointCount == 1 ? 0.0 : 0.18 + world.random.nextDouble() * 0.28;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = findGroundTopY(world, x, z, center.y) + BASE_GROUND_OFFSET;
            int heightSegments = Math.max(2, Math.round((3 + world.random.nextInt(4)) * heightScale));
            spawnSpikeVisual(world, pulse, x, y, z, heightSegments, now);
        }
        ACTIVE_PULSES.computeIfAbsent(world, w -> new ArrayList<>()).add(pulse);
    }

    public static void tick(ServerWorld world) {
        List<ActiveIceSpikePulse> pulses = ACTIVE_PULSES.get(world);
        if (pulses == null || pulses.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanSpikeVisuals(world);
            }
            return;
        }

        pulses.removeIf(pulse -> {
            if (world.getTime() > pulse.expiryTick()) {
                removePulse(world, pulse);
                return true;
            }
            return false;
        });

        if (pulses.isEmpty()) {
            ACTIVE_PULSES.remove(world);
            purgeOrphanSpikeVisuals(world);
            return;
        }

        for (ActiveIceSpikePulse pulse : pulses) {
            animatePulse(world, pulse);
        }
    }

    public static void clear(ServerWorld world) {
        List<ActiveIceSpikePulse> pulses = ACTIVE_PULSES.remove(world);
        if (pulses != null) pulses.forEach(pulse -> removePulse(world, pulse));
        purgeOrphanSpikeVisuals(world);
    }

    public static void clearAll() {
        new ArrayList<>(ACTIVE_PULSES.keySet()).forEach(FrostfallIceSpikeFieldManager::clear);
        ACTIVE_PULSES.clear();
    }

    private static void spawnSpikeRing(ServerWorld world, ActiveIceSpikePulse pulse, Vec3d center, double radius, int detonationIndex, long spawnTick) {
        int pointCount = MathHelper.clamp((int) Math.ceil(radius * Math.PI * 1.6), MIN_RING_POINTS, MAX_RING_POINTS);
        double phase = world.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < pointCount; i++) {
            double angle = phase + ((Math.PI * 2.0) / pointCount) * i;
            double jitter = 0.88 + ((i * 7) % 7) * 0.035;
            double x = center.x + Math.cos(angle) * radius * jitter;
            double z = center.z + Math.sin(angle) * radius * jitter;
            double y = findGroundTopY(world, x, z, center.y) + BASE_GROUND_OFFSET;
            int heightSegments = 3 + ((i * 11 + detonationIndex) % 7);
            spawnSpikeVisual(world, pulse, x, y, z, heightSegments, spawnTick);
        }
    }

    private static void spawnSpikeVisual(ServerWorld world, ActiveIceSpikePulse pulse, double x, double y, double z, int heightSegments, long spawnTick) {
        float targetHeight = (float) (heightSegments * SPIKE_SEGMENT_HEIGHT);
        FrostfallIceSpikeVisualEntity visual = new FrostfallIceSpikeVisualEntity(world, x, y - START_DEPTH, z, targetHeight);
        visual.addCommandTag(ICE_SPIKE_VISUAL_TAG);
        world.spawnEntity(visual);
        world.spawnParticles(ParticleTypes.POOF, x, y + 0.04, z, 3, 0.16, 0.04, 0.16, 0.01);
        world.spawnParticles(ParticleTypes.CLOUD, x, y + 0.02, z, 2, 0.12, 0.03, 0.12, 0.005);
        pulse.visuals().add(new SpikeVisual(visual.getUuid(), x, y, z, spawnTick));
    }

    private static void animatePulse(ServerWorld world, ActiveIceSpikePulse pulse) {
        pulse.visuals().removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof FrostfallIceSpikeVisualEntity iceSpike)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            if (age >= VISUAL_DURATION_TICKS) {
                iceSpike.discard();
                return true;
            }

            iceSpike.setHeightScale(getHeightScale(age));
            iceSpike.setPos(visual.baseX(), visual.baseY() + getVerticalOffset(age), visual.baseZ());
            return false;
        });
    }

    private static float getHeightScale(long age) {
        if (age < 0) {
            return 0.0F;
        }
        if (age < RAISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) RAISE_TICKS, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < RAISE_TICKS + HOLD_TICKS) {
            return 1.0F;
        }
        long sinkAge = age - RAISE_TICKS - HOLD_TICKS;
        if (sinkAge < SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) SINK_TICKS, 0.0F, 1.0F);
            return 1.0F - (t * t * t);
        }
        return 0.0F;
    }

    private static double getVerticalOffset(long age) {
        if (age < RAISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) RAISE_TICKS, 0.0F, 1.0F);
            return -START_DEPTH + (START_DEPTH * easeOutBack(t));
        }
        if (age < RAISE_TICKS + HOLD_TICKS) {
            return 0.0;
        }
        long sinkAge = age - RAISE_TICKS - HOLD_TICKS;
        if (sinkAge < SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) SINK_TICKS, 0.0F, 1.0F);
            return -(START_DEPTH * t * t * t);
        }
        return -START_DEPTH;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
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

    private static void removePulse(ServerWorld world, ActiveIceSpikePulse pulse) {
        for (SpikeVisual visual : pulse.visuals()) {
            Entity entity = world.getEntity(visual.id());
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void purgeOrphanSpikeVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof FrostfallIceSpikeVisualEntity && entity.getCommandTags().contains(ICE_SPIKE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveIceSpikePulse {
        private final long expiryTick;
        private final List<SpikeVisual> visuals = new ArrayList<>();

        private ActiveIceSpikePulse(long expiryTick) {
            this.expiryTick = expiryTick;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private List<SpikeVisual> visuals() {
            return this.visuals;
        }
    }

    private record SpikeVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick) {
    }
}
