package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BrimstoneClaymoreVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class BrimstoneClaymoreAbilityManager {

    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 24;
    private static final int PLUNGE_TICKS = 18;
    private static final String VISUAL_TAG = "simplyswords_brimstone_claymore_visual";
    private static final Map<ServerWorld, List<ActiveBrimstoneClaymore>> ACTIVE = new HashMap<>();

    private BrimstoneClaymoreAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveBrimstoneClaymore> active = ACTIVE.get(world);
        return active != null && !active.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static void start(ServerWorld world, LivingEntity owner, LivingEntity target) {
        if (owner == null || target == null || !target.isAlive()) {
            return;
        }

        Vec3d groundPos = getGroundPos(world, target.getPos());
        float baseRadius = Math.max(0.5F, Config.uniqueEffects.brimstone_claymore.baseRadius);
        UUID visualId = null;
        if (Config.general.enableModernFieldEffects) {
            BrimstoneClaymoreVisualEntity visual = new BrimstoneClaymoreVisualEntity(world, groundPos.x, groundPos.y, groundPos.z, baseRadius);
            visual.addCommandTag(VISUAL_TAG);
            world.spawnEntity(visual);
            visualId = visual.getUuid();
        }

        long now = world.getTime();
        ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveBrimstoneClaymore(
                owner.getUuid(),
                target.getUuid(),
                visualId,
                groundPos,
                now + Math.max(1, Config.uniqueEffects.brimstone_claymore.duration),
                now + Math.max(1, Config.uniqueEffects.brimstone_claymore.pulseInterval),
                -1L,
                baseRadius,
                (float) HelperMethods.getEntityAttackDamage(owner)
        ));
        spawnStartEffects(world, groundPos);
    }

    public static void tick(ServerWorld world) {
        List<ActiveBrimstoneClaymore> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            if (world.getTime() % 40L == 0L) {
                purgeOrphans(world);
            }
            return;
        }

        active.removeIf(instance -> tickInstance(world, instance));
        if (active.isEmpty()) {
            ACTIVE.remove(world);
            purgeOrphans(world);
        }
    }

    private static boolean tickInstance(ServerWorld world, ActiveBrimstoneClaymore instance) {
        Entity ownerEntity = world.getEntity(instance.ownerId());
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            removeVisual(world, instance.visualId());
            return true;
        }

        if (instance.plungeEndTick() > 0L) {
            if (world.getTime() >= instance.plungeEndTick()) {
                impact(world, owner, instance);
                removeVisual(world, instance.visualId());
                return true;
            }
            tickPlungeWindup(world, instance);
            return false;
        }

        updateTarget(world, owner, instance);
        updateVisual(world, instance);
        if (world.getTime() >= instance.nextPulseTick()) {
            pulse(world, owner, instance);
            instance.setNextPulseTick(world.getTime() + Math.max(1, Config.uniqueEffects.brimstone_claymore.pulseInterval));
        }
        if (world.getTime() >= instance.expiryTick()) {
            startPlunge(world, instance);
        }
        return false;
    }

    private static void updateTarget(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        Entity entity = instance.targetId() == null ? null : world.getEntity(instance.targetId());
        if (entity instanceof LivingEntity target && target.isAlive() && HelperMethods.checkAbilityTarget(target, owner)) {
            instance.setPos(getGroundPos(world, target.getPos()));
            return;
        }

        LivingEntity next = findJumpTarget(world, owner, instance.pos(), instance.targetId());
        instance.setTargetId(next == null ? null : next.getUuid());
        if (next != null) {
            instance.setPos(getGroundPos(world, next.getPos()));
            spawnJumpEffects(world, instance.pos());
        }
    }

    private static LivingEntity findJumpTarget(ServerWorld world, LivingEntity owner, Vec3d pos, UUID previousTarget) {
        double range = Math.max(0.0, Config.uniqueEffects.brimstone_claymore.targetJumpRange);
        Box box = new Box(pos.x - range, pos.y - range, pos.z - range, pos.x + range, pos.y + range, pos.z + range);
        return world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(target -> !target.getUuid().equals(previousTarget))
                .filter(target -> target.isAlive() && target.squaredDistanceTo(pos) <= range * range)
                .filter(target -> HelperMethods.checkAbilityTarget(target, owner))
                .min(Comparator.comparingDouble(target -> target.squaredDistanceTo(pos)))
                .orElse(null);
    }

    private static void updateVisual(ServerWorld world, ActiveBrimstoneClaymore instance) {
        Entity entity = instance.visualId() == null ? null : world.getEntity(instance.visualId());
        if (entity instanceof BrimstoneClaymoreVisualEntity visual) {
            Vec3d pos = instance.pos();
            visual.setPos(pos.x, pos.y, pos.z);
            visual.setRadius(instance.radius());
            visual.setScale(getScale(instance.radius()));
        }
    }

    private static void pulse(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        int damaged = damageInRadius(world, owner, instance.pos(), instance.radius(), instance.weaponDamage() * Config.uniqueEffects.brimstone_claymore.pulseDamageMultiplier, false);
        if (damaged > 0) {
            float maxRadius = Math.max(instance.radius(), Config.uniqueEffects.brimstone_claymore.maxRadius);
            float growth = Math.max(0.0F, Config.uniqueEffects.brimstone_claymore.radiusGrowthPerHit) * damaged;
            instance.setRadius(MathHelper.clamp(instance.radius() + growth, instance.radius(), maxRadius));
        }
        spawnPulseEffects(world, instance.pos(), instance.radius(), damaged);
    }

    private static void startPlunge(ServerWorld world, ActiveBrimstoneClaymore instance) {
        instance.setPlungeEndTick(world.getTime() + PLUNGE_TICKS);
        Entity entity = instance.visualId() == null ? null : world.getEntity(instance.visualId());
        if (entity instanceof BrimstoneClaymoreVisualEntity visual) {
            visual.setPlunging(true);
            visual.setPlungeStartAge(visual.age);
            visual.setRadius(instance.radius());
            visual.setScale(getScale(instance.radius()) * 1.15F);
        }
        Vec3d pos = instance.pos();
        world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.2, pos.z, 18, 0.35, 0.55, 0.35, 0.05);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.0, pos.z, 24, 0.5, 0.55, 0.5, 0.04);
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.DARK_SWORD_UNFOLD.get(), SoundCategory.PLAYERS, 0.75F, 0.55F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.65F, 0.55F);
    }

    private static void tickPlungeWindup(ServerWorld world, ActiveBrimstoneClaymore instance) {
        if (!Config.general.enableModernFieldEffects || world.getTime() % 2L != 0L) {
            return;
        }
        long plungeAge = PLUNGE_TICKS - Math.max(0L, instance.plungeEndTick() - world.getTime());
        Vec3d pos = instance.pos();
        double height = plungeAge < 7L ? 2.6 + plungeAge * 0.12 : Math.max(0.35, 3.6 - (plungeAge - 7L) * 0.32);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + height, pos.z, 4, 0.18, 0.18, 0.18, 0.04);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + height, pos.z, 3, 0.22, 0.18, 0.22, 0.035);
        if (plungeAge >= 7L) {
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + height, pos.z, 2, 0.12, 0.12, 0.12, 0.04);
        }
    }

    private static void impact(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        float baseRadius = Math.max(0.5F, Config.uniqueEffects.brimstone_claymore.baseRadius);
        float damage = instance.weaponDamage() * Config.uniqueEffects.brimstone_claymore.finalDamageMultiplier * (instance.radius() / baseRadius);
        int damaged = damageInRadius(world, owner, instance.pos(), instance.radius(), damage, true);
        spawnImpactEffects(world, instance.pos(), instance.radius(), damaged);
    }

    private static int damageInRadius(ServerWorld world, LivingEntity owner, Vec3d pos, float radius, float damage, boolean finalImpact) {
        Box box = new Box(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius);
        int damaged = 0;
        for (Entity entity : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity target && target.squaredDistanceTo(pos) <= radius * radius && HelperMethods.checkAbilityTarget(target, owner)) {
                target.setOnFireFor(finalImpact ? 5 : 2);
                if (HelperMethods.damageThroughIframes(target, world.getDamageSources().indirectMagic(owner, owner), damage)) {
                    damaged++;
                    Vec3d targetPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.5), 0.0);
                    world.spawnParticles(ParticleTypes.FLAME, targetPos.x, targetPos.y, targetPos.z, finalImpact ? 10 : 4, 0.18, 0.2, 0.18, 0.03);
                    world.spawnParticles(ParticleTypes.LAVA, targetPos.x, targetPos.y, targetPos.z, finalImpact ? 5 : 2, 0.16, 0.12, 0.16, 0.02);
                }
            }
        }
        return damaged;
    }

    private static void spawnStartEffects(ServerWorld world, Vec3d pos) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.5, pos.z, 18, 0.35, 0.45, 0.35, 0.04);
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.3, pos.z, 8, 0.2, 0.35, 0.2, 0.02);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.4, pos.z, 10, 0.3, 0.35, 0.3, 0.03);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.55F, 0.85F);
    }

    private static void spawnJumpEffects(ServerWorld world, Vec3d pos) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.2, pos.z, 12, 0.3, 0.35, 0.3, 0.05);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.2, pos.z, 8, 0.28, 0.3, 0.28, 0.04);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.35F, 0.75F);
    }

    private static void spawnPulseEffects(ServerWorld world, Vec3d pos, float radius, int damaged) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.2, pos.z, 10 + damaged * 2, radius * 0.35, 0.12, radius * 0.35, 0.03);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.25, pos.z, 8 + damaged, radius * 0.32, 0.12, radius * 0.32, 0.02);
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.25, pos.z, 3 + damaged, radius * 0.25, 0.08, radius * 0.25, 0.02);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.35F, 0.7F + world.random.nextFloat() * 0.2F);
        if (damaged > 0) {
            world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.45F, 0.95F);
        }
    }

    private static void spawnImpactEffects(ServerWorld world, Vec3d pos, float radius, int damaged) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.35, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.45, pos.z, 28 + damaged * 2, radius * 0.35, 0.3, radius * 0.35, 0.09);
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.35, pos.z, 48, radius * 0.45, 0.25, radius * 0.45, 0.08);
            world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.x, pos.y + 0.45, pos.z, 24, radius * 0.38, 0.35, radius * 0.38, 0.05);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.getDefaultState()), pos.x, pos.y + 0.15, pos.z, 30, radius * 0.32, 0.16, radius * 0.32, 0.08);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 0.65F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.55F, 0.85F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.8F, 0.65F);
    }

    private static float getScale(float radius) {
        float baseRadius = Math.max(0.5F, Config.uniqueEffects.brimstone_claymore.baseRadius);
        float maxRadius = Math.max(baseRadius, Config.uniqueEffects.brimstone_claymore.maxRadius);
        if (maxRadius <= baseRadius) {
            return 1.25F;
        }
        float progress = MathHelper.clamp((radius - baseRadius) / (maxRadius - baseRadius), 0.0F, 1.0F);
        return 1.25F + progress * 1.05F;
    }

    private static Vec3d getGroundPos(ServerWorld world, Vec3d pos) {
        return new Vec3d(pos.x, findGroundTopY(world, pos.x, pos.z, pos.y), pos.z);
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

    private static void removeVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return;
        }
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void purgeOrphans(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BrimstoneClaymoreVisualEntity && entity.getCommandTags().contains(VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveBrimstoneClaymore {
        private final UUID ownerId;
        private UUID targetId;
        private final UUID visualId;
        private Vec3d pos;
        private final long expiryTick;
        private long nextPulseTick;
        private long plungeEndTick;
        private float radius;
        private final float weaponDamage;

        private ActiveBrimstoneClaymore(UUID ownerId, UUID targetId, UUID visualId, Vec3d pos, long expiryTick, long nextPulseTick, long plungeEndTick, float radius, float weaponDamage) {
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.visualId = visualId;
            this.pos = pos;
            this.expiryTick = expiryTick;
            this.nextPulseTick = nextPulseTick;
            this.plungeEndTick = plungeEndTick;
            this.radius = radius;
            this.weaponDamage = weaponDamage;
        }

        private UUID ownerId() {
            return this.ownerId;
        }

        private UUID targetId() {
            return this.targetId;
        }

        private void setTargetId(UUID targetId) {
            this.targetId = targetId;
        }

        private UUID visualId() {
            return this.visualId;
        }

        private Vec3d pos() {
            return this.pos;
        }

        private void setPos(Vec3d pos) {
            this.pos = pos;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private long nextPulseTick() {
            return this.nextPulseTick;
        }

        private void setNextPulseTick(long nextPulseTick) {
            this.nextPulseTick = nextPulseTick;
        }

        private long plungeEndTick() {
            return this.plungeEndTick;
        }

        private void setPlungeEndTick(long plungeEndTick) {
            this.plungeEndTick = plungeEndTick;
        }

        private float radius() {
            return this.radius;
        }

        private void setRadius(float radius) {
            this.radius = radius;
        }

        private float weaponDamage() {
            return this.weaponDamage;
        }
    }
}
