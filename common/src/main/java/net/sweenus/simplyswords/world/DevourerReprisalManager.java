package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DevourerReprisalVisualEntity;
import net.sweenus.simplyswords.entity.DevourerTendrilVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DevourerReprisalManager {
    private static final int VISUAL_LIFETIME = 14;
    private static final int CLOSING_EFFECT_TICK = 8;
    private static final int TENDRIL_RETRACT_TICK = 4;
    private static final DustColorTransitionParticleEffect REPRISAL_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.025F, 0.006F, 0.055F),
                    new Vector3f(0.48F, 0.08F, 0.78F), 1.45F);
    private static final Map<ServerWorld, List<ActiveReprisal>> ACTIVE = new HashMap<>();

    private DevourerReprisalManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveReprisal> active = ACTIVE.get(world);
        return active != null && !active.isEmpty();
    }

    public static void trigger(ItemStack stack, LivingEntity bearer, DamageSource incoming) {
        if (!(bearer.getWorld() instanceof ServerWorld world)
                || stack == null || !stack.isOf(ItemsRegistry.THE_DEVOURER.get())
                || !(incoming.getAttacker() instanceof LivingEntity attacker)
                || !attacker.isAlive() || attacker.isRemoved() || attacker.getWorld() != world
                || isInvulnerablePlayer(attacker)
                || attacker == bearer || !EntityPredicates.VALID_LIVING_ENTITY.test(attacker)
                || !HelperMethods.checkAbilityTarget(attacker, bearer)) {
            return;
        }
        double reach = Math.max(1.0, Config.uniqueEffects.devourer.reprisalReach);
        if (attacker.squaredDistanceTo(bearer) > reach * reach) {
            return;
        }

        double radius = Math.max(0.5, Config.uniqueEffects.devourer.reprisalRadius);
        int targetCap = Math.max(1, Config.uniqueEffects.devourer.reprisalTargetCap);
        Vec3d center = resolveMawCenter(world, attacker);
        List<LivingEntity> targets = collectTargets(world, bearer, attacker, center, radius, targetCap);
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, bearer, stack,
                Config.uniqueEffects.devourer.reprisalDamageScaling,
                Config.uniqueEffects.devourer.reprisalSpellScaling);
        DevourerAbilityManager.ReprisalRedirect redirect =
                DevourerAbilityManager.feedFromReprisal(world, bearer.getUuid(), center,
                        attacker, Math.max(10, Config.uniqueEffects.devourer.reprisalDragDuration + 7));
        for (LivingEntity target : targets) {
            damageTarget(world, bearer, stack, target, damage);
        }
        Vec3d destination = redirect == null ? center : redirect.destination();
        UUID tendrilId = redirect == null ? null : redirect.tendrilId();
        DevourerReprisalVisualEntity visual = new DevourerReprisalVisualEntity(world, center,
                VISUAL_LIFETIME, (float) radius, redirect != null);
        world.spawnEntity(visual);
        spawnOpeningEffects(world, center, redirect != null);

        List<UUID> targetIds = targets.stream()
                .filter(LivingEntity::isAlive)
                .map(Entity::getUuid)
                .toList();
        long now = world.getTime();
        int dragDuration = Math.max(1, Config.uniqueEffects.devourer.reprisalDragDuration);
        long dragEndTick = now + dragDuration;
        long cleanupTick = Math.max(dragEndTick, now + CLOSING_EFFECT_TICK + 1L);
        ActiveReprisal reprisal = new ActiveReprisal(bearer.getUuid(), center, destination,
                redirect != null, targetIds, dragEndTick, cleanupTick,
                now + CLOSING_EFFECT_TICK, now + TENDRIL_RETRACT_TICK, tendrilId);
        ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>()).add(reprisal);
    }

    public static void tick(ServerWorld world) {
        List<ActiveReprisal> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<ActiveReprisal> iterator = active.iterator();
        while (iterator.hasNext()) {
            ActiveReprisal reprisal = iterator.next();
            LivingEntity bearer = resolveLiving(world, reprisal.bearerId);
            if (!reprisal.closingEffectsPlayed && now >= reprisal.closingEffectTick) {
                reprisal.closingEffectsPlayed = true;
                spawnClosingEffects(world, reprisal.mawCenter, reprisal.redirected);
            }
            if (!reprisal.tendrilRetracting && now >= reprisal.tendrilRetractTick) {
                reprisal.tendrilRetracting = true;
                beginTendrilRetraction(world, reprisal);
            }
            if (bearer != null && now < reprisal.dragEndTick) {
                tickPull(world, bearer, reprisal);
            }
            if (now >= reprisal.cleanupTick) {
                beginTendrilRetraction(world, reprisal);
                iterator.remove();
            }
        }
        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static List<LivingEntity> collectTargets(ServerWorld world, LivingEntity bearer,
                                                       LivingEntity attacker, Vec3d center,
                                                       double radius, int cap) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(attacker);
        if (cap <= 1) {
            return targets;
        }
        Box search = new Box(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        world.getEntitiesByClass(LivingEntity.class, search,
                        target -> target != attacker && target != bearer && target.isAlive()
                                && !target.isRemoved() && !isInvulnerablePlayer(target)
                                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                                && HelperMethods.checkAbilityTarget(target, bearer)
                                && horizontalDistanceSquared(target.getPos(), center) <= radius * radius)
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(center)))
                .limit(cap - 1L)
                .forEach(targets::add);
        return targets;
    }

    private static void tickPull(ServerWorld world, LivingEntity bearer, ActiveReprisal reprisal) {
        double configured = Math.max(0.0, Config.uniqueEffects.devourer.reprisalPullStrength);
        for (UUID targetId : reprisal.targetIds) {
            LivingEntity target = resolveLiving(world, targetId);
            if (target == null || isInvulnerablePlayer(target)
                    || !HelperMethods.checkAbilityTarget(target, bearer)) {
                continue;
            }
            Vec3d offset;
            if (reprisal.redirected) {
                Vec3d desiredBase = reprisal.destination.subtract(0.0, target.getHeight() * 0.5, 0.0);
                offset = desiredBase.subtract(target.getPos());
            } else {
                offset = new Vec3d(reprisal.destination.x - target.getX(), 0.0,
                        reprisal.destination.z - target.getZ());
            }
            double distance = offset.length();
            if (distance < 0.08) {
                continue;
            }
            double size = Math.max(1.0, Math.max(target.getWidth(), target.getHeight()));
            double resistance = MathHelper.clamp(
                    target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double response = MathHelper.clamp(1.0 / (size * (1.0 + resistance * 2.0)), 0.12, 1.0);
            double strength = Math.min(0.48, configured * response * Math.min(1.5, 0.9 + distance * 0.08));
            Vec3d pull = offset.normalize().multiply(strength);
            Vec3d current = target.getVelocity().multiply(0.52);
            target.setVelocity(current.x + pull.x,
                    MathHelper.clamp(current.y + pull.y, -0.32, 0.38),
                    current.z + pull.z);
            target.velocityModified = true;
            target.fallDistance = 0.0F;
        }
    }

    private static void damageTarget(ServerWorld world, LivingEntity bearer, ItemStack stack,
                                     LivingEntity target, float baseDamage) {
        if (baseDamage <= 0.0F || !target.isAlive()) {
            return;
        }
        DamageSource magic = bearer.getDamageSources().magic();
        DamageSource source = new DamageSource(magic.getTypeRegistryEntry(), bearer, bearer);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, baseDamage);
        WeaponImplicitRegistry.runSuppressed(() -> HelperMethods.damageThroughIframes(target, source, damage));
    }

    private static Vec3d resolveMawCenter(ServerWorld world, LivingEntity attacker) {
        Vec3d start = attacker.getPos().add(0.0, 0.3, 0.0);
        Vec3d end = start.add(0.0, -5.0, 0.0);
        BlockHitResult hit = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, attacker));
        return hit.getType() == HitResult.Type.BLOCK
                ? hit.getPos().add(0.0, 0.025, 0.0)
                : attacker.getPos().add(0.0, 0.025, 0.0);
    }

    private static void spawnOpeningEffects(ServerWorld world, Vec3d center, boolean fed) {
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                SoundCategory.PLAYERS, fed ? 0.95F : 0.78F, 0.58F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK,
                SoundCategory.PLAYERS, 0.38F, 1.55F);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.08, center.z,
                30, 0.85, 0.08, 0.85, 0.045);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.08, center.z,
                12, 0.72, 0.12, 0.72, 0.025);
        world.spawnParticles(REPRISAL_DUST, center.x, center.y + 0.08, center.z,
                26, 0.9, 0.1, 0.9, 0.035);
    }

    private static void spawnClosingEffects(ServerWorld world, Vec3d center, boolean fed) {
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,
                SoundCategory.PLAYERS, fed ? 1.18F : 1.0F, 0.68F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                SoundCategory.PLAYERS, 0.72F, 0.82F);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.12, center.z,
                38, 1.15, 0.12, 1.15, 0.075);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.1, center.z,
                18, 0.9, 0.18, 0.9, 0.04);
        world.spawnParticles(REPRISAL_DUST, center.x, center.y + 0.1, center.z,
                34, 1.05, 0.14, 1.05, 0.055);
    }

    private static void beginTendrilRetraction(ServerWorld world, ActiveReprisal reprisal) {
        if (reprisal.tendrilId == null) {
            return;
        }
        Entity entity = world.getEntity(reprisal.tendrilId);
        if (entity instanceof DevourerTendrilVisualEntity tendril && tendril.getRetractAge() < 0) {
            Entity target = tendril.getTarget();
            tendril.beginRetraction(target == null ? tendril.getStoredEnd() : target.getPos());
        }
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static boolean isInvulnerablePlayer(LivingEntity target) {
        return target instanceof PlayerEntity player && (player.isCreative() || player.isSpectator());
    }

    private static final class ActiveReprisal {
        private final UUID bearerId;
        private final Vec3d mawCenter;
        private final Vec3d destination;
        private final boolean redirected;
        private final List<UUID> targetIds;
        private final long dragEndTick;
        private final long cleanupTick;
        private final long closingEffectTick;
        private final long tendrilRetractTick;
        private final UUID tendrilId;
        private boolean closingEffectsPlayed;
        private boolean tendrilRetracting;

        private ActiveReprisal(UUID bearerId, Vec3d mawCenter, Vec3d destination,
                               boolean redirected, List<UUID> targetIds,
                               long dragEndTick, long cleanupTick, long closingEffectTick,
                               long tendrilRetractTick, UUID tendrilId) {
            this.bearerId = bearerId;
            this.mawCenter = mawCenter;
            this.destination = destination;
            this.redirected = redirected;
            this.targetIds = targetIds;
            this.dragEndTick = dragEndTick;
            this.cleanupTick = cleanupTick;
            this.closingEffectTick = closingEffectTick;
            this.tendrilRetractTick = tendrilRetractTick;
            this.tendrilId = tendrilId;
        }
    }
}
