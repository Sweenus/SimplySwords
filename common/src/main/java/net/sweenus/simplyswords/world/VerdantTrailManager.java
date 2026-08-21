package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.VerdantTrailVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VerdantTrailManager {

    private static final int GROUND_SCAN_UP = 2;
    private static final int GROUND_SCAN_DOWN = 5;
    private static final int GROWTH_TICKS = 8;
    private static final double ORPHAN_VISUAL_CLEANUP_RADIUS = 192.0;
    private static final String TRAIL_VISUAL_TAG = "simplyswords_verdant_trail_visual";
    private static final Map<ServerWorld, List<TrailSegment>> ACTIVE_SEGMENTS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, LastPlacement>> LAST_PLACEMENTS = new HashMap<>();

    private VerdantTrailManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<TrailSegment> segments = ACTIVE_SEGMENTS.get(world);
        Map<UUID, LastPlacement> placements = LAST_PLACEMENTS.get(world);
        return (segments != null && !segments.isEmpty()) || (placements != null && !placements.isEmpty()) || world.getTime() % 20L == 0L;
    }

    public static void tryPlaceTrail(LivingEntity user, ItemStack stack) {
        if (!(user.getWorld() instanceof ServerWorld world)) return;
        if (stack == null || stack.isEmpty()) return;
        boolean spectator = user instanceof PlayerEntity p && p.isSpectator();
        if (!user.isAlive() || spectator || !user.isOnGround()) {
            return;
        }

        Vec3d center = new Vec3d(user.getX(), user.getY(), user.getZ());
        Vec3d ground = findGroundPosition(world, center);
        if (ground == null) {
            return;
        }

        Map<UUID, LastPlacement> placements = LAST_PLACEMENTS.computeIfAbsent(world, ignored -> new HashMap<>());
        UUID ownerId = user.getUuid();
        LastPlacement previous = placements.get(ownerId);
        long now = world.getTime();
        int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                stack, user, Config.gemPowers.verdantTrail.placementCooldown);
        double spacing = Math.max(0.1, Config.gemPowers.verdantTrail.placementSpacing);
        if (previous != null
                && (now < previous.nextEligibleTick() || previous.position().squaredDistanceTo(ground) < spacing * spacing)) {
            return;
        }

        placements.put(ownerId, new LastPlacement(ground, now, now + cooldown));
        createSegment(world, user, ground, stack);
    }

    public static void tick(ServerWorld world) {
        long now = world.getTime();
        if (now % 80L == 0L) {
            Map<UUID, LastPlacement> placements = LAST_PLACEMENTS.get(world);
            if (placements != null) {
                placements.entrySet().removeIf(entry -> now - entry.getValue().placedTick()
                        > Math.max(200, Config.gemPowers.verdantTrail.duration));
                if (placements.isEmpty()) {
                    LAST_PLACEMENTS.remove(world);
                }
            }
        }

        List<TrailSegment> segments = ACTIVE_SEGMENTS.get(world);
        if (segments == null || segments.isEmpty()) {
            if (now % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        Map<UUID, Set<UUID>> affectedThisPulse = new HashMap<>();
        Iterator<TrailSegment> iterator = segments.iterator();
        while (iterator.hasNext()) {
            TrailSegment segment = iterator.next();
            LivingEntity owner;
            {
                Entity entity = world.getEntity(segment.ownerId());
                owner = entity instanceof LivingEntity l ? l : null;
            }
            if (owner == null || now >= segment.expiryTick()) {
                removeSegment(world, segment);
                iterator.remove();
                continue;
            }

            animateVisuals(world, segment, now);
            spawnAmbientParticles(world, segment.center(), now);
            if (now % Math.max(1, Config.gemPowers.verdantTrail.auraInterval) == 0L) {
                applyAura(world, segment, owner, affectedThisPulse.computeIfAbsent(segment.ownerId(), ignored -> new HashSet<>()));
            }
        }

        if (segments.isEmpty()) {
            ACTIVE_SEGMENTS.remove(world);
        }
    }

    private static void createSegment(ServerWorld world, LivingEntity owner, Vec3d center, ItemStack stack) {
        int duration = Math.max(1, Config.gemPowers.verdantTrail.duration);
        float damage = HelperMethods.gemPowerScaledDamage(SpellScalingComponents.power("verdant_trail"), owner, stack,
                Config.gemPowers.verdantTrail.damageScaling,
                Config.gemPowers.verdantTrail.spellScaling);
        int regenerationDuration = AwakeningApi.scaleGemPowerDuration(
                stack, Config.gemPowers.verdantTrail.regenerationDuration);
        TrailSegment segment = new TrailSegment(
                owner.getUuid(),
                center,
                world.getTime(),
                world.getTime() + duration,
                stack.copy(),
                damage,
                regenerationDuration,
                new ArrayList<>());
        int minPoints = Math.max(1, Config.gemPowers.verdantTrail.visualPointsMin);
        int maxPoints = Math.max(minPoints, Config.gemPowers.verdantTrail.visualPointsMax);
        int pointCount = minPoints + world.random.nextInt(maxPoints - minPoints + 1);
        double radius = Math.max(0.1, Config.gemPowers.verdantTrail.radius);

        for (int i = 0; i < pointCount; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(world.random.nextDouble()) * radius * 0.9;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            Vec3d visualPos = findGroundPosition(world, new Vec3d(x, center.y, z));
            if (visualPos == null) {
                continue;
            }
            spawnPlantVisual(world, segment.visualIds(), visualPos, choosePlantType(world, i));
        }

        if (!segment.visualIds().isEmpty()) {
            ACTIVE_SEGMENTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(segment);
            world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS, 0.25F, 1.15F + world.random.nextFloat() * 0.2F);
            world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.12, center.z, 4, 0.45, 0.04, 0.45, 0.0);
        }
    }

    private static void applyAura(ServerWorld world, TrailSegment segment, LivingEntity owner, Set<UUID> affectedThisPulse) {
        double radius = Math.max(0.1, Config.gemPowers.verdantTrail.radius);
        Box box = Box.of(segment.center(), radius * 2.0, 1.6, radius * 2.0);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.squaredDistanceTo(segment.center()) > radius * radius || !affectedThisPulse.add(entity.getUuid())) {
                continue;
            }

            if (entity == owner || !HelperMethods.checkFriendlyFire(entity, owner)) {
                entity.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.REGENERATION,
                                segment.regenerationDuration(),
                                Math.max(0, Config.gemPowers.verdantTrail.regenerationAmplifier),
                                false,
                                true),
                        owner
                );
            } else {
                damageWithoutKnockback(entity, owner, segment.weaponStack(), segment.damage());
            }
        }
    }

    private static void damageWithoutKnockback(
            LivingEntity target, LivingEntity owner, ItemStack stack, float damage) {
        if (damage <= 0.0F) {
            return;
        }
        Vec3d velocity = target.getVelocity();
        var damageSource = owner.getDamageSources().indirectMagic(owner, owner);
        if (target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(
                (ServerWorld) owner.getWorld(), stack, target, damageSource, damage))) {
            target.setVelocity(velocity);
            target.velocityModified = true;
        }
    }

    private static void animateVisuals(ServerWorld world, TrailSegment segment, long now) {
        float growProgress = MathHelper.clamp((float) (now - segment.createdTick()) / (float) GROWTH_TICKS, 0.0F, 1.0F);
        float fadeProgress = MathHelper.clamp((float) (segment.expiryTick() - now) / (float) GROWTH_TICKS, 0.0F, 1.0F);
        float scale = Math.min(growProgress, fadeProgress);
        for (UUID id : segment.visualIds()) {
            Entity entity = world.getEntity(id);
            if (entity instanceof VerdantTrailVisualEntity visual) {
                visual.setHeightScale(scale);
            }
        }
    }

    private static void spawnAmbientParticles(ServerWorld world, Vec3d center, long now) {
        if (now % 8L != 0L) {
            return;
        }
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, center.x, center.y + 0.25, center.z, 1, 0.45, 0.08, 0.45, 0.0);
        if (world.random.nextFloat() < 0.45F) {
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SHORT_GRASS.getDefaultState()), center.x, center.y + 0.08, center.z, 1, 0.35, 0.03, 0.35, 0.01);
        }
    }

    private static void spawnPlantVisual(ServerWorld world, List<UUID> visualIds, Vec3d pos, int plantType) {
        VerdantTrailVisualEntity visual = new VerdantTrailVisualEntity(world, pos.x, pos.y + 0.03, pos.z, plantType);
        visual.setYaw(world.random.nextFloat() * 360.0F);
        visual.addCommandTag(TRAIL_VISUAL_TAG);
        world.spawnEntity(visual);
        visualIds.add(visual.getUuid());
    }

    private static int choosePlantType(ServerWorld world, int index) {
        if (index % 5 == 0) {
            return 2 + world.random.nextInt(5);
        }
        return world.random.nextInt(3);
    }

    private static Vec3d findGroundPosition(ServerWorld world, Vec3d origin) {
        BlockPos start = BlockPos.ofFloored(origin.x, origin.y, origin.z);
        for (int offset = GROUND_SCAN_UP; offset >= -GROUND_SCAN_DOWN; offset--) {
            BlockPos groundPos = start.up(offset);
            BlockPos plantPos = groundPos.up();
            BlockState ground = world.getBlockState(groundPos);
            BlockState plantSpace = world.getBlockState(plantPos);
            if (!ground.getCollisionShape(world, groundPos).isEmpty()
                    && plantSpace.getFluidState().isEmpty()
                    && plantSpace.getCollisionShape(world, plantPos).isEmpty()) {
                return new Vec3d(origin.x, plantPos.getY(), origin.z);
            }
        }
        return null;
    }

    private static void removeSegment(ServerWorld world, TrailSegment segment) {
        for (UUID id : segment.visualIds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        Set<UUID> cleaned = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            Box searchBox = player.getBoundingBox().expand(ORPHAN_VISUAL_CLEANUP_RADIUS);
            for (VerdantTrailVisualEntity visual : world.getEntitiesByClass(
                    VerdantTrailVisualEntity.class,
                    searchBox,
                    entity -> entity.getCommandTags().contains(TRAIL_VISUAL_TAG)
            )) {
                if (cleaned.add(visual.getUuid())) {
                    visual.discard();
                }
            }
        }
    }

    private record TrailSegment(
            UUID ownerId,
            Vec3d center,
            long createdTick,
            long expiryTick,
            ItemStack weaponStack,
            float damage,
            int regenerationDuration,
            List<UUID> visualIds) {
    }

    private record LastPlacement(Vec3d position, long placedTick, long nextEligibleTick) {
    }
}
