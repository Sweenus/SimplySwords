package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.IcewhisperCometVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class IcewhisperCometManager {

    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 24;
    private static final double SPAWN_HEIGHT = 18.0;
    private static final String COMET_VISUAL_TAG = "simplyswords_icewhisper_comet_visual";
    private static final Map<ServerWorld, List<ActiveComet>> ACTIVE_COMETS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveStorm>> ACTIVE_STORMS = new HashMap<>();

    private IcewhisperCometManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveComet> comets = ACTIVE_COMETS.get(world);
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        return (comets != null && !comets.isEmpty()) || (storms != null && !storms.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void startStorm(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage, int durationTicks) {
        if (owner == null || durationTicks <= 0) {
            return;
        }

        List<ActiveStorm> storms = ACTIVE_STORMS.computeIfAbsent(world, w -> new ArrayList<>());
        storms.removeIf(storm -> storm.ownerId().equals(owner.getUuid()));
        storms.add(new ActiveStorm(owner.getUuid(), stack.copy(), world.getTime() + durationTicks, world.getTime(), radius, damage));
    }

    public static void spawnWave(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage) {
        int cometCount = Math.max(0, Config.uniqueEffects.icewhisper.cometsPerWave);
        if (cometCount <= 0 || owner == null) {
            return;
        }

        for (int i = 0; i < cometCount; i++) {
            Vec3d impact = chooseImpactPosition(world, owner.getPos(), radius);
            spawnComet(world, owner, stack, impact, damage);
        }
    }

    public static void tick(ServerWorld world) {
        tickStorms(world);

        List<ActiveComet> comets = ACTIVE_COMETS.get(world);
        if (comets == null || comets.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanCometVisuals(world);
            }
            return;
        }

        comets.removeIf(comet -> tickComet(world, comet));
        if (comets.isEmpty()) {
            ACTIVE_COMETS.remove(world);
            purgeOrphanCometVisuals(world);
        }
    }

    private static void tickStorms(ServerWorld world) {
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        storms.removeIf(storm -> {
            if (world.getTime() >= storm.expiryTick()) {
                return true;
            }
            Entity ownerEntity = world.getEntity(storm.ownerId());
            if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
                return true;
            }
            if (world.getTime() >= storm.nextWaveTick()) {
                spawnWave(world, owner, storm.stack(), storm.radius(), storm.damage());
                storm.setNextWaveTick(world.getTime() + Math.max(1, Config.uniqueEffects.icewhisper.cometInterval));
            }
            return false;
        });

        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static void spawnComet(ServerWorld world, LivingEntity owner, ItemStack stack, Vec3d impact, float damage) {
        long startTick = world.getTime();
        int fallTicks = Math.max(1, Config.uniqueEffects.icewhisper.cometFallTicks);
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
        }

        ActiveComet comet = new ActiveComet(owner.getUuid(), stack.copy(), visualId, start, impact, startTick, fallTicks, damage);
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

        Vec3d impact = comet.impact();
        float splashRadius = Math.max(0.0F, Config.uniqueEffects.icewhisper.cometSplashRadius);
        Box box = new Box(
                impact.x - splashRadius, impact.y - splashRadius, impact.z - splashRadius,
                impact.x + splashRadius, impact.y + splashRadius, impact.z + splashRadius
        );
        for (Entity entity : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity target && HelperMethods.checkAbilityTarget(target, owner)
                    && target.squaredDistanceTo(impact) <= splashRadius * splashRadius) {
                var damageSource = world.getDamageSources().indirectMagic(owner, owner);
                float damage = HelperMethods.applyAbilityDamageEnchantments(world, comet.stack(), target, damageSource, comet.damage());
                HelperMethods.damageThroughIframes(target, damageSource, damage);
            }
        }

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
        double y = findGroundTopY(world, x, z, center.y);
        return new Vec3d(x, y, z);
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

    private record ActiveComet(UUID ownerId, ItemStack stack, UUID visualId, Vec3d start, Vec3d impact, long startTick, int fallTicks, float damage) {
    }

    private static final class ActiveStorm {
        private final UUID ownerId;
        private final ItemStack stack;
        private final long expiryTick;
        private long nextWaveTick;
        private final double radius;
        private final float damage;

        private ActiveStorm(UUID ownerId, ItemStack stack, long expiryTick, long nextWaveTick, double radius, float damage) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.expiryTick = expiryTick;
            this.nextWaveTick = nextWaveTick;
            this.radius = radius;
            this.damage = damage;
        }

        private UUID ownerId() {
            return this.ownerId;
        }

        private ItemStack stack() {
            return this.stack;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private long nextWaveTick() {
            return this.nextWaveTick;
        }

        private void setNextWaveTick(long nextWaveTick) {
            this.nextWaveTick = nextWaveTick;
        }

        private double radius() {
            return this.radius;
        }

        private float damage() {
            return this.damage;
        }
    }
}
