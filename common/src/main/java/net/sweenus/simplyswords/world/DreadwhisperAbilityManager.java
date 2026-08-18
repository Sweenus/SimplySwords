package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DreadwhisperVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DreadwhisperAbilityManager {
    private static final int COLLAPSE_LIFETIME = 8;
    private static final int LEECH_LIFETIME = 10;
    private static final int BURST_LIFETIME = 12;
    private static final ThreadLocal<Boolean> SUPPRESS_WOUND_CONSUMPTION = ThreadLocal.withInitial(() -> false);
    private static final DustColorTransitionParticleEffect REND_DUST = new DustColorTransitionParticleEffect(
            new Vector3f(0.035F, 0.006F, 0.07F), new Vector3f(0.35F, 0.95F, 0.36F), 1.15F);
    private static final Map<ServerWorld, Map<UUID, ActiveRend>> ACTIVE = new HashMap<>();

    private DreadwhisperAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.DREADWHISPER.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || getActive(context.world(), context.actor().getUuid()) != null) {
            return false;
        }
        if (context.actor() instanceof PlayerEntity) {
            return true;
        }
        return context.target() != null
                && context.target().isAlive()
                && HelperMethods.checkAbilityTarget(context.target(), context.actor());
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        LivingEntity actor = context.actor();
        Vec3d direction = resolveDirection(context);
        if (direction.horizontalLengthSquared() < 0.0001) {
            return false;
        }

        int maximumTicks = maximumDashTicks();
        DreadwhisperVisualEntity visual = DreadwhisperVisualEntity.reaveFront(
                context.world(), actor, direction,
                (float) Math.max(1.0, Config.uniqueEffects.dreadwhisper.frontWidth),
                (float) Math.max(0.5, Config.uniqueEffects.dreadwhisper.frontHeight),
                maximumTicks + COLLAPSE_LIFETIME + 4);
        context.world().spawnEntity(visual);

        UUID stainId = WraithmawStainManager.beginTrail(
                context.world(), actor.getUuid(), groundPosition(context.world(), actor.getPos()),
                direction, Math.max(1.0, Config.uniqueEffects.dreadwhisper.frontWidth),
                Math.max(20, Config.uniqueEffects.dreadwhisper.stainDuration),
                Math.max(1, Config.uniqueEffects.dreadwhisper.stainFadeDuration),
                Math.clamp(Config.uniqueEffects.dreadwhisper.stainSlowAmplifier, 0, 4));

        ActiveRend active = new ActiveRend(
                actor.getUuid(), context.stack().copy(), direction, actor.getPos(),
                context.world().getTime(), visual.getUuid(), stainId);
        ACTIVE.computeIfAbsent(context.world(), ignored -> new HashMap<>()).put(actor.getUuid(), active);
        applyDashVelocity(actor, direction);
        spawnActivationEffects(context.world(), actor);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveRend> active = ACTIVE.get(world);
        return active != null && !active.isEmpty();
    }

    public static boolean isDashing(LivingEntity actor) {
        return actor != null
                && actor.getWorld() instanceof ServerWorld world
                && getActive(world, actor.getUuid()) != null;
    }

    public static boolean blocksIncomingDamage(LivingEntity actor, DamageSource source) {
        return actor != null
                && source != null
                && !actor.getWorld().isClient()
                && !source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && isDashing(actor);
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveRend> activeByOwner = ACTIVE.get(world);
        if (activeByOwner == null || activeByOwner.isEmpty()) {
            return;
        }

        Iterator<ActiveRend> iterator = activeByOwner.values().iterator();
        while (iterator.hasNext()) {
            ActiveRend active = iterator.next();
            Entity entity = world.getEntity(active.ownerId);
            if (!(entity instanceof LivingEntity owner)
                    || !owner.isAlive()
                    || owner.isRemoved()
                    || !isWieldingDreadwhisper(owner)) {
                cancel(world, entity instanceof LivingEntity living ? living : null, active);
                iterator.remove();
                continue;
            }

            if (tickDash(world, owner, active)) {
                iterator.remove();
            }
        }

        if (activeByOwner.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (SUPPRESS_WOUND_CONSUMPTION.get()
                || target == null
                || source == null
                || target.getWorld().isClient()
                || !target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND))
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || source.getSource() != attacker
                || !isDirectMeleeSource(source)) {
            return amount;
        }

        ItemStack stack = source.getWeaponStack();
        if (stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.DREADWHISPER.get())) {
            stack = resolveHeldDreadwhisper(attacker);
        }
        if (stack.isEmpty()) {
            return amount;
        }

        target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND));
        if (target.getWorld() instanceof ServerWorld world) {
            spawnWoundBurst(world, target, attacker);
        }
        return isNaturalCritical(attacker)
                ? amount
                : amount * Math.max(1.0F, Config.uniqueEffects.dreadwhisper.criticalMultiplier);
    }

    private static boolean tickDash(ServerWorld world, LivingEntity owner, ActiveRend active) {
        Vec3d current = owner.getPos();
        double moved = horizontalDistance(active.previousPosition, current);
        if (moved > 0.01) {
            active.distanceTravelled += moved;
            damageDashTargets(world, owner, active, active.previousPosition, current);
            WraithmawStainManager.extendTrail(world, active.stainId, groundPosition(world, current));
        }
        active.previousPosition = current;
        updateFrontPosition(world, active, current);
        owner.fallDistance = 0.0F;

        long elapsed = world.getTime() - active.startedAt;
        if ((elapsed > 1L && owner.horizontalCollision)
                || active.distanceTravelled >= Math.max(1.0, Config.uniqueEffects.dreadwhisper.dashDistance)
                || elapsed >= maximumDashTicks()) {
            finishDash(world, owner, active);
            return true;
        }

        applyDashVelocity(owner, active.direction);
        return false;
    }

    private static void damageDashTargets(ServerWorld world, LivingEntity owner, ActiveRend active,
                                          Vec3d start, Vec3d end) {
        double halfWidth = Math.max(1.0, Config.uniqueEffects.dreadwhisper.frontWidth) * 0.5;
        double height = Math.max(0.5, Config.uniqueEffects.dreadwhisper.frontHeight);
        Box search = segmentBox(start, end, height, halfWidth);
        float weaponDamage = HelperMethods.attackScaledDamage(
                owner, active.stack, Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitScaling));
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, search,
                candidate -> validTarget(candidate, owner)
                        && !active.hitTargets.contains(candidate.getUuid())
                        && intersectsSegment(candidate, start, end, halfWidth,
                        Math.max(start.y, end.y) + height))) {
            active.hitTargets.add(target.getUuid());
            float healthBefore = target.getHealth();
            boolean[] damaged = {false};
            runWithoutWoundConsumption(() -> damaged[0] = SimplySwordsAPI.applyEntityWeaponHit(
                    active.stack, target, owner, weaponDamage));
            if (!damaged[0]) {
                continue;
            }

            float removedHealth = Math.max(0.0F, healthBefore - target.getHealth());
            float healing = removedHealth * MathHelper.clamp(Config.uniqueEffects.dreadwhisper.healRatio, 0.0F, 1.0F);
            if (healing > 0.0F) {
                owner.heal(healing);
            }
            if (target.isAlive()) {
                applyWound(target);
            }
            spawnContactEffects(world, target, owner, removedHealth > 0.0F);
        }
    }

    private static void applyWound(LivingEntity target) {
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND),
                Math.max(1, Config.uniqueEffects.dreadwhisper.woundDuration),
                0, false, false, false));
    }

    private static void updateFrontPosition(ServerWorld world, ActiveRend active, Vec3d position) {
        Entity entity = world.getEntity(active.visualId);
        if (entity instanceof DreadwhisperVisualEntity visual) {
            visual.setPosition(position);
        }
    }

    private static void finishDash(ServerWorld world, LivingEntity owner, ActiveRend active) {
        stopDash(owner);
        WraithmawStainManager.finishTrail(world, active.stainId);
        Entity entity = world.getEntity(active.visualId);
        if (entity instanceof DreadwhisperVisualEntity visual) {
            visual.setPosition(owner.getPos());
            visual.beginCollapse();
            visual.setLifetime(visual.age + COLLAPSE_LIFETIME);
        }
        Vec3d center = owner.getPos().add(0.0, owner.getHeight() * 0.48, 0.0);
        world.spawnParticles(REND_DUST, center.x, center.y, center.z, 16, 0.65, 0.7, 0.65, 0.055);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 8, 0.5, 0.55, 0.5, 0.055);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_BREAKS.get(),
                SoundCategory.PLAYERS, 0.72F, 0.76F);
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity owner) {
        Vec3d center = owner.getPos().add(0.0, owner.getHeight() * 0.5, 0.0);
        world.spawnParticles(REND_DUST, center.x, center.y, center.z, 18, 0.5, 0.72, 0.5, 0.065);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 10, 0.42, 0.55, 0.42, 0.08);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 1.0F, 0.76F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.92F, 0.68F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_WHOOSH_04.get(),
                SoundCategory.PLAYERS, 0.85F, 0.72F);
    }

    private static void spawnContactEffects(ServerWorld world, LivingEntity target,
                                            LivingEntity owner, boolean leechedHealth) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.52, 0.0);
        DreadwhisperVisualEntity visual = DreadwhisperVisualEntity.leechHit(
                world, center, owner,
                Math.max(0.85F, target.getWidth() * 1.45F),
                Math.max(1.0F, target.getHeight()), LEECH_LIFETIME);
        world.spawnEntity(visual);
        world.spawnParticles(REND_DUST, center.x, center.y, center.z, 14, 0.38, 0.48, 0.38, 0.09);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 5, 0.24, 0.34, 0.24, 0.045);
        world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_02.get(),
                SoundCategory.PLAYERS, 0.88F, 0.78F);
        if (leechedHealth) {
            world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(),
                    SoundCategory.PLAYERS, 0.48F, 1.38F);
        }
    }

    private static void spawnWoundBurst(ServerWorld world, LivingEntity target, LivingEntity attacker) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        DreadwhisperVisualEntity visual = DreadwhisperVisualEntity.woundBurst(
                world, center, Math.max(0.65F, target.getWidth() * 1.35F),
                Math.max(1.0F, target.getHeight()), BURST_LIFETIME);
        world.spawnEntity(visual);
        world.spawnParticles(REND_DUST, center.x, center.y, center.z, 36, 0.48, 0.52, 0.48, 0.12);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 10, 0.35, 0.4, 0.35, 0.07);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.08, 0.12, 0.08, 0.0);
        if (attacker instanceof PlayerEntity player) {
            player.addCritParticles(target);
        }
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                SoundCategory.PLAYERS, 1.0F, 0.82F);
        world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                SoundCategory.PLAYERS, 0.75F, 0.72F);
    }

    private static int maximumDashTicks() {
        return Math.max(6, (int) Math.ceil(
                Math.max(1.0, Config.uniqueEffects.dreadwhisper.dashDistance)
                        / Math.max(0.1, Config.uniqueEffects.dreadwhisper.dashSpeed)) + 6);
    }

    private static void applyDashVelocity(LivingEntity owner, Vec3d direction) {
        double speed = Math.max(0.1, Config.uniqueEffects.dreadwhisper.dashSpeed);
        owner.setVelocity(direction.x * speed, owner.getVelocity().y, direction.z * speed);
        owner.velocityModified = true;
    }

    private static void stopDash(LivingEntity owner) {
        if (owner == null) {
            return;
        }
        owner.setVelocity(0.0, owner.getVelocity().y, 0.0);
        owner.velocityModified = true;
        owner.fallDistance = 0.0F;
    }

    private static void cancel(ServerWorld world, LivingEntity owner, ActiveRend active) {
        stopDash(owner);
        WraithmawStainManager.finishTrail(world, active.stainId);
        Entity visual = world.getEntity(active.visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static ActiveRend getActive(ServerWorld world, UUID ownerId) {
        Map<UUID, ActiveRend> active = ACTIVE.get(world);
        return active == null ? null : active.get(ownerId);
    }

    private static Vec3d resolveDirection(WeaponAbilityContext context) {
        Vec3d direction;
        if (!(context.actor() instanceof PlayerEntity)
                && context.target() != null
                && context.target().isAlive()) {
            direction = context.target().getPos().subtract(context.actor().getPos());
        } else {
            direction = context.facing();
        }
        direction = new Vec3d(direction.x, 0.0, direction.z);
        if (direction.horizontalLengthSquared() < 0.0001) {
            direction = Vec3d.fromPolar(0.0F, context.actor().getYaw());
        }
        return direction.normalize();
    }

    private static boolean isWieldingDreadwhisper(LivingEntity entity) {
        return entity.getMainHandStack().isOf(ItemsRegistry.DREADWHISPER.get())
                || entity.getOffHandStack().isOf(ItemsRegistry.DREADWHISPER.get());
    }

    private static ItemStack resolveHeldDreadwhisper(LivingEntity entity) {
        if (entity.getMainHandStack().isOf(ItemsRegistry.DREADWHISPER.get())) {
            return entity.getMainHandStack();
        }
        if (entity.getOffHandStack().isOf(ItemsRegistry.DREADWHISPER.get())) {
            return entity.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private static boolean validTarget(LivingEntity target, LivingEntity owner) {
        return target != owner
                && target.isAlive()
                && !target.isRemoved()
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, owner);
    }

    private static Box segmentBox(Vec3d start, Vec3d end, double height, double padding) {
        return new Box(
                Math.min(start.x, end.x) - padding,
                Math.min(start.y, end.y) - 0.5,
                Math.min(start.z, end.z) - padding,
                Math.max(start.x, end.x) + padding,
                Math.max(start.y, end.y) + height,
                Math.max(start.z, end.z) + padding);
    }

    private static boolean intersectsSegment(LivingEntity target, Vec3d start, Vec3d end,
                                             double halfWidth, double maxY) {
        if (target.getBoundingBox().maxY < Math.min(start.y, end.y) - 0.5
                || target.getBoundingBox().minY > maxY) {
            return false;
        }
        Vec3d line = new Vec3d(end.x - start.x, 0.0, end.z - start.z);
        double lengthSquared = line.lengthSquared();
        Vec3d point = new Vec3d(target.getX() - start.x, 0.0, target.getZ() - start.z);
        double progress = lengthSquared < 0.0001
                ? 0.0
                : MathHelper.clamp(point.dotProduct(line) / lengthSquared, 0.0, 1.0);
        Vec3d closest = new Vec3d(start.x, 0.0, start.z).add(line.multiply(progress));
        double radius = halfWidth + target.getWidth() * 0.5 + target.getTargetingMargin();
        double dx = target.getX() - closest.x;
        double dz = target.getZ() - closest.z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean isDirectMeleeSource(DamageSource source) {
        return source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK);
    }

    private static boolean isNaturalCritical(LivingEntity attacker) {
        if (!(attacker instanceof PlayerEntity player)) {
            return false;
        }
        return player.getAttackCooldownProgress(0.5F) > 0.9F
                && player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle()
                && !player.isSprinting();
    }

    private static void runWithoutWoundConsumption(Runnable action) {
        boolean previous = SUPPRESS_WOUND_CONSUMPTION.get();
        SUPPRESS_WOUND_CONSUMPTION.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_WOUND_CONSUMPTION.set(previous);
        }
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double x = second.x - first.x;
        double z = second.z - first.z;
        return Math.sqrt(x * x + z * z);
    }

    private static Vec3d groundPosition(ServerWorld world, Vec3d position) {
        return new Vec3d(position.x,
                LivyatanWaveManager.findGroundTopY(world, position.x, position.z, position.y),
                position.z);
    }

    private static final class ActiveRend {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Vec3d direction;
        private final long startedAt;
        private final UUID visualId;
        private final UUID stainId;
        private final Set<UUID> hitTargets = new HashSet<>();
        private Vec3d previousPosition;
        private double distanceTravelled;

        private ActiveRend(UUID ownerId, ItemStack stack, Vec3d direction,
                           Vec3d previousPosition, long startedAt, UUID visualId, UUID stainId) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.direction = direction;
            this.previousPosition = previousPosition;
            this.startedAt = startedAt;
            this.visualId = visualId;
            this.stainId = stainId;
        }
    }
}
