package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.FaultlineSpikeVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FaultlineSunderManager {

    private static final double VISUAL_WIDTH_MULTIPLIER = 0.78;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 14;
    private static final double START_DEPTH = 2.8;
    private static final double BASE_GROUND_OFFSET = -0.18;
    private static final double ORPHAN_VISUAL_CLEANUP_RADIUS = 192.0;
    private static final String SPIKE_VISUAL_TAG = "simplyswords_faultline_spike_visual";
    private static final Map<ServerWorld, List<ActiveSunder>> ACTIVE_SUNDERS = new HashMap<>();

    private FaultlineSunderManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveSunder> sunders = ACTIVE_SUNDERS.get(world);
        return (sunders != null && !sunders.isEmpty()) || world.getTime() % 20L == 0L;
    }

    public static void createSunder(ServerWorld world, ServerPlayerEntity owner, float damage) {
        createSunder(world, owner, owner == null ? Vec3d.ZERO : owner.getPos(), owner == null ? Vec3d.ZERO : owner.getRotationVec(1.0F), damage);
    }

    public static void createSunder(ServerWorld world, ServerPlayerEntity owner, Vec3d origin, Vec3d facing, float damage) {
        if (world == null || owner == null || !owner.isAlive()) {
            return;
        }

        if (origin == null) {
            origin = owner.getPos();
        }
        Vec3d direction = facing == null ? Vec3d.ZERO : facing;
        direction = new Vec3d(direction.x, 0.0, direction.z);
        if (direction.lengthSquared() < 0.0001) {
            direction = Vec3d.fromPolar(0.0F, owner.getYaw()).normalize();
        } else {
            direction = direction.normalize();
        }
        Vec3d right = new Vec3d(-direction.z, 0.0, direction.x).normalize();

        long now = world.getTime();
        int riseTicks = Math.max(1, Config.gemPowers.faultline.riseTicks);
        int holdTicks = Math.max(0, Config.gemPowers.faultline.holdTicks);
        int sinkTicks = Math.max(1, Config.gemPowers.faultline.sinkTicks);
        int delayTicks = Math.max(0, Config.gemPowers.faultline.waveStepDelayTicks);
        int steps = Math.max(1, (int) Math.ceil(Math.max(1.0, Config.gemPowers.faultline.length) / Math.max(0.25, Config.gemPowers.faultline.stepDistance)));
        ActiveSunder sunder = new ActiveSunder(owner.getUuid(), now + (long) steps * delayTicks + riseTicks + holdTicks + sinkTicks + 4L, new ArrayList<>());

        if (Config.general.enableModernFieldEffects) {
            spawnSpikeLine(world, sunder, origin, direction, right, now, steps, delayTicks);
        }
        damageEnemies(world, owner, origin, direction, right, damage);
        ACTIVE_SUNDERS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(sunder);

        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.0F, 0.75F + world.random.nextFloat() * 0.12F);
        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.45F, 0.65F + world.random.nextFloat() * 0.08F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveSunder> sunders = ACTIVE_SUNDERS.get(world);
        if (sunders == null || sunders.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanSpikeVisuals(world);
            }
            return;
        }

        sunders.removeIf(sunder -> {
            animateVisuals(world, sunder);
            if (world.getTime() > sunder.expiryTick()) {
                removeSunder(world, sunder);
                return true;
            }
            return false;
        });

        if (sunders.isEmpty()) {
            ACTIVE_SUNDERS.remove(world);
        }
    }

    private static void spawnSpikeLine(ServerWorld world, ActiveSunder sunder, Vec3d origin, Vec3d direction, Vec3d right, long now, int steps, int delayTicks) {
        double length = Math.max(1.0, Config.gemPowers.faultline.length);
        double stepDistance = Math.max(0.25, Config.gemPowers.faultline.stepDistance);
        double halfWidth = Math.max(0.25, Config.gemPowers.faultline.width * 0.5 * VISUAL_WIDTH_MULTIPLIER);

        for (int step = 1; step <= steps; step++) {
            double forwardDistance = Math.min(length, step * stepDistance);
            Vec3d center = origin.add(direction.multiply(forwardDistance));
            long spawnTick = now + (long) (step - 1) * delayTicks;
            spawnSpike(world, sunder, center, spawnTick, Config.gemPowers.faultline.centerSpikeHeight, false, step);
            spawnSpike(world, sunder, center.add(right.multiply(halfWidth)), spawnTick, Config.gemPowers.faultline.edgeSpikeHeight, true, step);
            spawnSpike(world, sunder, center.add(right.multiply(-halfWidth)), spawnTick, Config.gemPowers.faultline.edgeSpikeHeight, true, step);
        }
    }

    private static void spawnSpike(ServerWorld world, ActiveSunder sunder, Vec3d pos, long spawnTick, float baseHeight, boolean edge, int step) {
        double groundY = findGroundTopY(world, pos.x, pos.z, pos.y) + BASE_GROUND_OFFSET;
        float jitter = 0.9F + (((step * (edge ? 17 : 11)) % 7) * 0.035F);
        FaultlineSpikeVisualEntity visual = new FaultlineSpikeVisualEntity(world, pos.x, groundY - START_DEPTH, pos.z, Math.max(0.1F, baseHeight * jitter));
        visual.addCommandTag(SPIKE_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }
        sunder.visuals().add(new SunderVisual(visual.getUuid(), pos.x, groundY, pos.z, spawnTick));
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()), pos.x, groundY + 0.12, pos.z, edge ? 5 : 3, 0.12, 0.06, 0.12, 0.01);
        world.spawnParticles(ParticleTypes.POOF, pos.x, groundY + 0.08, pos.z, edge ? 2 : 1, 0.1, 0.04, 0.1, 0.0);
    }

    private static void damageEnemies(ServerWorld world, ServerPlayerEntity owner, Vec3d origin, Vec3d direction, Vec3d right, float damage) {
        double length = Math.max(1.0, Config.gemPowers.faultline.length);
        double halfWidth = Math.max(0.25, Config.gemPowers.faultline.width * 0.5 * VISUAL_WIDTH_MULTIPLIER);
        Vec3d center = origin.add(direction.multiply(length * 0.5));
        Box box = Box.of(center, Math.abs(direction.x) * length + Math.abs(right.x) * halfWidth * 2.0 + 2.0, 3.0, Math.abs(direction.z) * length + Math.abs(right.z) * halfWidth * 2.0 + 2.0);
        Set<UUID> damaged = new HashSet<>();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!damaged.add(target.getUuid()) || !HelperMethods.checkFriendlyFire(target, owner)) {
                continue;
            }
            Vec3d relative = target.getPos().subtract(origin);
            double forward = relative.dotProduct(direction);
            double lateral = Math.abs(relative.dotProduct(right));
            if (forward < 0.0 || forward > length || lateral > halfWidth + target.getWidth() * 0.5) {
                continue;
            }
            target.damage(owner.getDamageSources().indirectMagic(owner, owner), damage);
        }
    }

    private static void animateVisuals(ServerWorld world, ActiveSunder sunder) {
        sunder.visuals().removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof FaultlineSpikeVisualEntity spike)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            int lifetime = Math.max(1, Config.gemPowers.faultline.riseTicks)
                    + Math.max(0, Config.gemPowers.faultline.holdTicks)
                    + Math.max(1, Config.gemPowers.faultline.sinkTicks);
            if (age >= lifetime) {
                spike.discard();
                return true;
            }

            spike.setHeightScale(getHeightScale(age));
            spike.setPos(visual.baseX(), visual.baseY() + getVerticalOffset(age), visual.baseZ());
            return false;
        });
    }

    private static float getHeightScale(long age) {
        if (age < 0) {
            return 0.0F;
        }
        int riseTicks = Math.max(1, Config.gemPowers.faultline.riseTicks);
        int holdTicks = Math.max(0, Config.gemPowers.faultline.holdTicks);
        int sinkTicks = Math.max(1, Config.gemPowers.faultline.sinkTicks);
        if (age < riseTicks) {
            float t = MathHelper.clamp((float) age / (float) riseTicks, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < riseTicks + holdTicks) {
            return 1.0F;
        }
        long sinkAge = age - riseTicks - holdTicks;
        if (sinkAge < sinkTicks) {
            float t = MathHelper.clamp((float) sinkAge / (float) sinkTicks, 0.0F, 1.0F);
            return 1.0F - (t * t * t);
        }
        return 0.0F;
    }

    private static double getVerticalOffset(long age) {
        if (age < 0) {
            return -START_DEPTH;
        }
        int riseTicks = Math.max(1, Config.gemPowers.faultline.riseTicks);
        int holdTicks = Math.max(0, Config.gemPowers.faultline.holdTicks);
        int sinkTicks = Math.max(1, Config.gemPowers.faultline.sinkTicks);
        if (age < riseTicks) {
            float t = MathHelper.clamp((float) age / (float) riseTicks, 0.0F, 1.0F);
            return -START_DEPTH + START_DEPTH * easeOutBack(t);
        }
        if (age < riseTicks + holdTicks) {
            return 0.0;
        }
        long sinkAge = age - riseTicks - holdTicks;
        if (sinkAge < sinkTicks) {
            float t = MathHelper.clamp((float) sinkAge / (float) sinkTicks, 0.0F, 1.0F);
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

    private static void removeSunder(ServerWorld world, ActiveSunder sunder) {
        for (SunderVisual visual : sunder.visuals()) {
            Entity entity = world.getEntity(visual.id());
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void purgeOrphanSpikeVisuals(ServerWorld world) {
        Set<UUID> cleaned = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            Box searchBox = player.getBoundingBox().expand(ORPHAN_VISUAL_CLEANUP_RADIUS);
            for (FaultlineSpikeVisualEntity visual : world.getEntitiesByClass(
                    FaultlineSpikeVisualEntity.class,
                    searchBox,
                    entity -> entity.getCommandTags().contains(SPIKE_VISUAL_TAG)
            )) {
                if (cleaned.add(visual.getUuid())) {
                    visual.discard();
                }
            }
        }
    }

    private record ActiveSunder(UUID ownerId, long expiryTick, List<SunderVisual> visuals) {
    }

    private record SunderVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick) {
    }
}
