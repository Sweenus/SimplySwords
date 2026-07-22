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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class ArcanethystAssaultManager {

    private static final Map<ServerWorld, List<ActiveAssault>> ACTIVE_ASSAULTS = new HashMap<>();
    private static final int TARGET_SCAN_INTERVAL_TICKS = 5;
    private static final double PLAYER_WIDTH_LIMIT = 1.6001;
    private static final double PLAYER_HEIGHT_LIMIT = 2.8001;

    private ArcanethystAssaultManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveAssault> assaults = ACTIVE_ASSAULTS.get(world);
        return assaults != null && !assaults.isEmpty();
    }

    public static void start(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage) {
        if (owner == null || !owner.isAlive()) {
            return;
        }

        long now = world.getTime();
        List<ActiveAssault> assaults = ACTIVE_ASSAULTS.computeIfAbsent(world, w -> new ArrayList<>());
        assaults.removeIf(assault -> {
            if (!assault.ownerId().equals(owner.getUuid())) {
                return false;
            }
            restoreTargets(world, assault);
            return true;
        });
        ActiveAssault assault = new ActiveAssault(owner.getUuid(), stack.copy(), now, now + Math.max(1, Config.uniqueEffects.arcanethyst.duration), now, radius, damage, new ArrayList<>(), new HashSet<>());
        scanForTargets(world, owner, assault);
        assaults.add(assault);
        spawnCastParticles(world, owner.getPos());
        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.55F, 1.15F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveAssault> assaults = ACTIVE_ASSAULTS.get(world);
        if (assaults == null || assaults.isEmpty()) {
            return;
        }

        Iterator<ActiveAssault> iterator = assaults.iterator();
        while (iterator.hasNext()) {
            ActiveAssault assault = iterator.next();
            if (tickAssault(world, assault)) {
                iterator.remove();
            }
        }
        if (assaults.isEmpty()) {
            ACTIVE_ASSAULTS.remove(world);
        }
    }

    private static boolean tickAssault(ServerWorld world, ActiveAssault assault) {
        Entity ownerEntity = world.getEntity(assault.ownerId());
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            restoreTargets(world, assault);
            return true;
        }
        if (world.getTime() >= assault.expiryTick()) {
            return assault.targets().isEmpty() || tickTargets(world, owner, assault);
        }

        if (world.getTime() >= assault.nextScanTick()) {
            scanForTargets(world, owner, assault);
            assault.setNextScanTick(world.getTime() + TARGET_SCAN_INTERVAL_TICKS);
        }

        tickTargets(world, owner, assault);

        if (world.getTime() % 5L == 0L) {
            world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.12F, 0.75F);
        }
        return world.getTime() >= assault.expiryTick() && assault.targets().isEmpty();
    }

    private static boolean tickTargets(ServerWorld world, LivingEntity owner, ActiveAssault assault) {
        int liftTicks = Math.max(1, Config.uniqueEffects.arcanethyst.liftTicks);
        int suspendTicks = Math.max(0, Config.uniqueEffects.arcanethyst.suspendTicks);
        int slamTicks = Math.max(1, Config.uniqueEffects.arcanethyst.slamTicks);

        assault.targets().removeIf(active -> {
            Entity entity = world.getEntity(active.targetId());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                restoreTarget(world, active);
                return true;
            }

            long age = world.getTime() - active.startTick();
            target.fallDistance = 0.0F;
            if (age < liftTicks) {
                tickLift(world, target, active, age, liftTicks);
                return false;
            }
            if (age < liftTicks + suspendTicks) {
                tickSuspend(world, target, active);
                return false;
            }

            long slamAge = age - liftTicks - suspendTicks;
            if (!active.slamStarted()) {
                active.markSlamStarted();
                target.setNoGravity(false);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(), SoundCategory.PLAYERS, 0.35F, 0.85F + world.random.nextFloat() * 0.2F);
            }
            return tickSlam(world, owner, assault.stack(), target, active, slamAge, slamTicks, assault.damage());
        });

        return assault.targets().isEmpty();
    }

    private static void scanForTargets(ServerWorld world, LivingEntity owner, ActiveAssault assault) {
        Box box = new Box(
                owner.getX() + assault.radius(), owner.getY() + assault.radius(), owner.getZ() + assault.radius(),
                owner.getX() - assault.radius(), owner.getY() - assault.radius(), owner.getZ() - assault.radius()
        );
        for (Entity entity : world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity target && canTarget(owner, target, assault)) {
                assault.processedTargets().add(target.getUuid());
                assault.targets().add(new ActiveTarget(target.getUuid(), world.getTime(), target.getY(), target.getY() + Config.uniqueEffects.arcanethyst.liftHeight, target.hasNoGravity()));
                target.setNoGravity(true);
                target.fallDistance = 0.0F;
                spawnLiftStartParticles(world, target);
            }
        }
    }

    private static boolean canTarget(LivingEntity owner, LivingEntity target, ActiveAssault assault) {
        return !assault.processedTargets().contains(target.getUuid())
                && HelperMethods.checkAbilityTarget(target, owner)
                && target.getWidth() <= PLAYER_WIDTH_LIMIT
                && target.getHeight() <= PLAYER_HEIGHT_LIMIT;
    }

    private static void tickLift(ServerWorld world, LivingEntity target, ActiveTarget active, long age, int liftTicks) {
        double t = MathHelper.clamp((double) age / (double) liftTicks, 0.0, 1.0);
        double eased = 1.0 - Math.pow(1.0 - t, 3.0);
        double wantedY = MathHelper.lerp(eased, active.startY(), active.hoverY());
        target.setVelocity(0.0, MathHelper.clamp((wantedY - target.getY()) * 0.34, 0.08, 0.45), 0.0);
        target.velocityModified = true;
        spawnLiftParticles(world, target);
    }

    private static void tickSuspend(ServerWorld world, LivingEntity target, ActiveTarget active) {
        target.setVelocity(0.0, (active.hoverY() - target.getY()) * 0.28, 0.0);
        target.velocityModified = true;
        spawnSuspendParticles(world, target);
    }

    private static boolean tickSlam(ServerWorld world, LivingEntity owner, ItemStack stack, LivingEntity target, ActiveTarget active, long slamAge, int slamTicks, float damage) {
        target.setVelocity(0.0, -1.75, 0.0);
        target.velocityModified = true;
        spawnSlamTrail(world, target);
        if (target.isOnGround() || slamAge >= slamTicks) {
            restoreTarget(world, active);
            target.fallDistance = 0.0F;
            var damageSource = world.getDamageSources().indirectMagic(owner, owner);
            float slamDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource,
                    damage * Config.uniqueEffects.arcanethyst.slamDamageMultiplier);
            HelperMethods.damageThroughIframes(target, damageSource, slamDamage);
            spawnImpact(world, target.getPos());
            return true;
        }
        return false;
    }

    private static void restoreTargets(ServerWorld world, ActiveAssault assault) {
        for (ActiveTarget target : assault.targets()) {
            restoreTarget(world, target);
        }
    }

    private static void restoreTarget(ServerWorld world, ActiveTarget active) {
        Entity entity = world.getEntity(active.targetId());
        if (entity instanceof LivingEntity target) {
            target.setNoGravity(active.originalNoGravity());
            target.fallDistance = 0.0F;
        }
    }

    private static void spawnCastParticles(ServerWorld world, Vec3d pos) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 0.2, pos.z, 28, 1.4, 0.18, 1.4, 0.04);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.6, pos.z, 36, 1.2, 0.45, 1.2, 0.09);
        world.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 0.8, pos.z, 24, 1.0, 0.35, 1.0, 0.08);
    }

    private static void spawnLiftParticles(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects || world.getTime() % 2L != 0L) {
            return;
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getBodyY(0.45), target.getZ(), 3, 0.22, 0.45, 0.22, 0.04);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + 0.05, target.getZ(), 2, 0.18, 0.05, 0.18, 0.02);
    }

    private static void spawnLiftStartParticles(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }
        world.spawnParticles(ParticleTypes.PORTAL, target.getX(), target.getBodyY(0.45), target.getZ(), 12, 0.35, 0.35, 0.35, 0.08);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + 0.05, target.getZ(), 8, 0.3, 0.08, 0.3, 0.03);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.2F, 1.15F + world.random.nextFloat() * 0.25F);
    }

    private static void spawnSuspendParticles(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects || world.getTime() % 3L != 0L) {
            return;
        }
        world.spawnParticles(ParticleTypes.PORTAL, target.getX(), target.getBodyY(0.55), target.getZ(), 5, 0.32, 0.28, 0.32, 0.02);
        world.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.2), target.getZ(), 3, 0.2, 0.18, 0.2, 0.06);
    }

    private static void spawnSlamTrail(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects || world.getTime() % 2L != 0L) {
            return;
        }
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getBodyY(0.75), target.getZ(), 5, 0.18, 0.28, 0.18, 0.035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getBodyY(0.65), target.getZ(), 4, 0.14, 0.22, 0.14, 0.04);
    }

    private static void spawnImpact(ServerWorld world, Vec3d pos) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y + 0.1, pos.z, 18, 0.55, 0.08, 0.55, 0.03);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 0.2, pos.z, 24, 0.7, 0.2, 0.7, 0.045);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.3, pos.z, 18, 0.55, 0.25, 0.55, 0.07);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.getDefaultState()), pos.x, pos.y + 0.1, pos.z, 20, 0.45, 0.12, 0.45, 0.05);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.65F, 0.75F + world.random.nextFloat() * 0.25F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.3F, 1.45F + world.random.nextFloat() * 0.2F);
    }

    private static final class ActiveAssault {
        private final UUID ownerId;
        private final ItemStack stack;
        private final long startTick;
        private final long expiryTick;
        private long nextScanTick;
        private final double radius;
        private final float damage;
        private final List<ActiveTarget> targets;
        private final Set<UUID> processedTargets;

        private ActiveAssault(UUID ownerId, ItemStack stack, long startTick, long expiryTick, long nextScanTick, double radius, float damage, List<ActiveTarget> targets, Set<UUID> processedTargets) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.startTick = startTick;
            this.expiryTick = expiryTick;
            this.nextScanTick = nextScanTick;
            this.radius = radius;
            this.damage = damage;
            this.targets = targets;
            this.processedTargets = processedTargets;
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

        private long nextScanTick() {
            return this.nextScanTick;
        }

        private void setNextScanTick(long nextScanTick) {
            this.nextScanTick = nextScanTick;
        }

        private double radius() {
            return this.radius;
        }

        private float damage() {
            return this.damage;
        }

        private List<ActiveTarget> targets() {
            return this.targets;
        }

        private Set<UUID> processedTargets() {
            return this.processedTargets;
        }
    }

    private static final class ActiveTarget {
        private final UUID targetId;
        private final long startTick;
        private final double startY;
        private final double hoverY;
        private final boolean originalNoGravity;
        private boolean slamStarted;

        private ActiveTarget(UUID targetId, long startTick, double startY, double hoverY, boolean originalNoGravity) {
            this.targetId = targetId;
            this.startTick = startTick;
            this.startY = startY;
            this.hoverY = hoverY;
            this.originalNoGravity = originalNoGravity;
        }

        private UUID targetId() {
            return this.targetId;
        }

        private long startTick() {
            return this.startTick;
        }

        private double startY() {
            return this.startY;
        }

        private double hoverY() {
            return this.hoverY;
        }

        private boolean originalNoGravity() {
            return this.originalNoGravity;
        }

        private boolean slamStarted() {
            return this.slamStarted;
        }

        private void markSlamStarted() {
            this.slamStarted = true;
        }
    }
}
