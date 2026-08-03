package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ThunderbrandAbilityManager {

    private static final int MAX_STORED_DAMAGE_INSTANCES = 15;
    private static final Map<ServerWorld, Map<UUID, ActiveThunderBlitz>> ACTIVE_ABILITIES = new HashMap<>();

    private ThunderbrandAbilityManager() {
    }

    public static boolean start(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.THUNDERBRAND.get())) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        Map<UUID, ActiveThunderBlitz> active = ACTIVE_ABILITIES.computeIfAbsent(world, ignored -> new HashMap<>());
        if (active.containsKey(actor.getUuid())) {
            return false;
        }

        Vec3d facing = horizontalDirection(context.facing(), actor);
        UUID targetId = isValidTarget(context.target(), actor) ? context.target().getUuid() : null;
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        active.put(actor.getUuid(), new ActiveThunderBlitz(
                actor.getUuid(),
                targetId,
                context.stack().copy(),
                hand,
                facing,
                world.getTime()
        ));

        int chargeDuration = chargeDuration();
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, chargeDuration + 2, 3), actor);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, chargeDuration + 2, 3), actor);
        spawnChargeStartEffects(world, actor);
        return true;
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveThunderBlitz> active = ACTIVE_ABILITIES.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    public static boolean handleIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null
                || source == null
                || amount <= 0.0F
                || !(actor.getWorld() instanceof ServerWorld world)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || actor instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        Map<UUID, ActiveThunderBlitz> active = ACTIVE_ABILITIES.get(world);
        ActiveThunderBlitz ability = active == null ? null : active.get(actor.getUuid());
        if (ability == null || ability.phase != Phase.CHARGING || !isStillHolding(actor, ability)) {
            return false;
        }

        if (ability.storedDamageInstances < MAX_STORED_DAMAGE_INSTANCES) {
            ability.storedDamageInstances++;
        }
        spawnAbsorbEffects(world, actor, ability.storedDamageInstances);
        return true;
    }

    public static void cancelCharging(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Map<UUID, ActiveThunderBlitz> active = ACTIVE_ABILITIES.get(world);
        ActiveThunderBlitz ability = active == null ? null : active.get(actor.getUuid());
        if (ability == null || ability.phase != Phase.CHARGING) {
            return;
        }

        active.remove(actor.getUuid());
        if (active.isEmpty()) {
            ACTIVE_ABILITIES.remove(world);
        }
        spawnCancelledEffects(world, actor);
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveThunderBlitz> active = ACTIVE_ABILITIES.get(world);
        return active != null && !active.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveThunderBlitz> active = ACTIVE_ABILITIES.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveThunderBlitz> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveThunderBlitz ability = iterator.next();
            if (!(world.getEntity(ability.actorId) instanceof LivingEntity actor)
                    || !actor.isAlive()
                    || !isStillHolding(actor, ability)
                    || ability.phase == Phase.CHARGING && !isPlayerStillCharging(actor, ability)) {
                stopDashMovement(actorFor(world, ability));
                iterator.remove();
                continue;
            }

            if (ability.phase == Phase.CHARGING) {
                tickCharging(world, actor, ability, now);
            }
            if (ability.phase == Phase.DASHING) {
                if (!tickDashing(world, actor, ability, now)) {
                    iterator.remove();
                }
            }
        }

        if (active.isEmpty()) {
            ACTIVE_ABILITIES.remove(world);
        }
    }

    private static void tickCharging(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, long now) {
        if ((now - ability.startedAt) % 3L == 0L) {
            spawnChargingEffects(world, actor, ability.storedDamageInstances);
        }
        if (now - ability.startedAt >= chargeDuration()) {
            beginDash(world, actor, ability, now);
        }
    }

    private static void beginDash(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, long now) {
        ability.phase = Phase.DASHING;
        ability.dashStartedAt = now;
        ability.previousPosition = actor.getPos();
        ability.dashDirection = resolveDashDirection(world, actor, ability);

        double speed = Math.max(0.1, Config.uniqueEffects.thunderbrand.dashSpeed);
        actor.setVelocity(ability.dashDirection.x * speed, 0.0, ability.dashDirection.z * speed);
        actor.velocityModified = true;
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), actor);

        if (actor instanceof ServerPlayerEntity player) {
            ItemStack currentStack = player.getStackInHand(ability.hand);
            if (!PlayerWeaponAbilityChannelManager.finishEarly(player, currentStack) && player.isUsingItem()) {
                player.stopUsingItem();
            }
        }

        spawnDashStartEffects(world, actor, ability.storedDamageInstances);
    }

    private static boolean tickDashing(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability, long now) {
        int dashDuration = dashDuration();
        int dashTick = (int) Math.max(0L, now - ability.dashStartedAt);
        if (dashTick >= dashDuration) {
            damageDashTargets(world, actor, ability);
            stopDashMovement(actor);
            spawnDashEndEffects(world, actor);
            return false;
        }

        damageDashTargets(world, actor, ability);
        releaseScheduledChains(world, actor, ability, dashTick, dashDuration);
        ability.previousPosition = actor.getPos();
        return true;
    }

    private static void damageDashTargets(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        Vec3d currentPosition = actor.getPos();
        Vec3d previousPosition = ability.previousPosition == null ? currentPosition : ability.previousPosition;
        Box currentBox = actor.getBoundingBox();
        Box previousBox = currentBox.offset(previousPosition.subtract(currentPosition));
        double radius = Math.max(0.1, Config.uniqueEffects.thunderbrand.radius);
        Box sweptBox = new Box(
                Math.min(currentBox.minX, previousBox.minX) - radius,
                Math.min(currentBox.minY, previousBox.minY) - Math.max(0.5, radius * 0.5),
                Math.min(currentBox.minZ, previousBox.minZ) - radius,
                Math.max(currentBox.maxX, previousBox.maxX) + radius,
                Math.max(currentBox.maxY, previousBox.maxY) + Math.max(0.5, radius * 0.5),
                Math.max(currentBox.maxZ, previousBox.maxZ) + radius
        );

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, sweptBox,
                target -> target != actor
                        && target.isAlive()
                        && !ability.dashHitTargets.contains(target.getUuid())
                        && HelperMethods.checkAbilityTarget(target, actor))) {
            ability.dashHitTargets.add(target.getUuid());
            DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
            float baseDamage = HelperMethods.abilityScaledDamage("lightning", actor, ability.stack,
                    Config.uniqueEffects.thunderbrand.damageScaling,
                    Config.uniqueEffects.thunderbrand.spellScaling);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, ability.stack, target, source, baseDamage * 3.0F);
            if (target.damage(source, damage)) {
                spawnDashHitEffects(world, target);
            }
        }
    }

    private static void releaseScheduledChains(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability,
                                               int dashTick, int dashDuration) {
        int totalChains = net.minecraft.util.math.MathHelper.clamp(ability.storedDamageInstances, 0, MAX_STORED_DAMAGE_INSTANCES);
        if (totalChains <= 0) {
            return;
        }

        while (ability.chainsReleased < totalChains
                && scheduledDashTick(ability.chainsReleased, totalChains, dashDuration) <= dashTick) {
            releaseChain(world, actor, ability);
            ability.chainsReleased++;
        }
    }

    private static int scheduledDashTick(int chainIndex, int totalChains, int dashDuration) {
        return (int) (((2L * chainIndex + 1L) * dashDuration) / (2L * totalChains));
    }

    private static void releaseChain(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        double range = Math.max(0.5, Config.uniqueEffects.thunderbrand.chainRange);
        LivingEntity firstTarget = findNearestChainTarget(world, actor, range);
        if (firstTarget == null) {
            spawnDissipatedChainEffects(world, actor);
            return;
        }

        float damage = HelperMethods.abilityScaledDamage("lightning", actor, ability.stack,
                Config.uniqueEffects.thunderbrand.damageScaling,
                Config.uniqueEffects.thunderbrand.spellScaling);
        ChainLightningVisualManager.damageChain(
                world,
                actor,
                actor,
                ability.stack,
                firstTarget,
                Math.max(1, Config.uniqueEffects.thunderbrand.chainTargets),
                damage,
                range,
                ChainLightningVisualManager.STORMBRINGER_SETTINGS
        );
    }

    private static LivingEntity findNearestChainTarget(ServerWorld world, LivingEntity actor, double range) {
        Box box = actor.getBoundingBox().expand(range, range * 0.5, range);
        return world.getEntitiesByClass(LivingEntity.class, box,
                        target -> target != actor && target.isAlive() && HelperMethods.checkAbilityTarget(target, actor))
                .stream()
                .min(Comparator.comparingDouble(actor::squaredDistanceTo))
                .orElse(null);
    }

    private static Vec3d resolveDashDirection(ServerWorld world, LivingEntity actor, ActiveThunderBlitz ability) {
        if (!(actor instanceof PlayerEntity)) {
            LivingEntity target = ability.targetId != null
                    && world.getEntity(ability.targetId) instanceof LivingEntity originalTarget
                    && isValidTarget(originalTarget, actor)
                    ? originalTarget
                    : findNearestChainTarget(world, actor,
                    Math.max(8.0, Config.uniqueEffects.thunderbrand.chainRange));
            if (target != null) {
                return horizontalDirection(target.getPos().subtract(actor.getPos()), actor);
            }
        }
        return horizontalDirection(actor instanceof PlayerEntity ? actor.getRotationVec(1.0F) : ability.fallbackDirection, actor);
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction == null ? Vec3d.ZERO : new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 0.0001) {
            Vec3d rotation = actor.getRotationVec(1.0F);
            horizontal = new Vec3d(rotation.x, 0.0, rotation.z);
        }
        if (horizontal.lengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return horizontal.normalize();
    }

    private static boolean isValidTarget(LivingEntity target, LivingEntity actor) {
        return target != null && target.isAlive() && HelperMethods.checkAbilityTarget(target, actor);
    }

    private static boolean isStillHolding(LivingEntity actor, ActiveThunderBlitz ability) {
        return actor.getStackInHand(ability.hand).isOf(ItemsRegistry.THUNDERBRAND.get());
    }

    private static boolean isPlayerStillCharging(LivingEntity actor, ActiveThunderBlitz ability) {
        if (!(actor instanceof ServerPlayerEntity player)) {
            return true;
        }
        return player.isUsingItem() && player.getActiveHand() == ability.hand
                || PlayerWeaponAbilityChannelManager.isChanneling(player, ability.hand, ItemsRegistry.THUNDERBRAND.get());
    }

    private static LivingEntity actorFor(ServerWorld world, ActiveThunderBlitz ability) {
        return world.getEntity(ability.actorId) instanceof LivingEntity actor ? actor : null;
    }

    private static void stopDashMovement(LivingEntity actor) {
        if (actor == null) {
            return;
        }
        actor.setVelocity(0.0, actor.getVelocity().y, 0.0);
        actor.velocityModified = true;
    }

    private static int chargeDuration() {
        return Math.max(1, Config.uniqueEffects.thunderbrand.chargeDuration);
    }

    private static int dashDuration() {
        return Math.max(1, Config.uniqueEffects.thunderbrand.dashDuration);
    }

    private static void spawnChargeStartEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 20, 0.4, 0.4, 0.4, 0.08);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_BOW_CHARGE_LONG_VERSION.get(),
                actor.getSoundCategory(), 0.4F, 0.6F);
    }

    private static void spawnChargingEffects(ServerWorld world, LivingEntity actor, int storedDamageInstances) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.5, 0.0);
        int sparks = 3 + Math.min(7, storedDamageInstances / 2);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, sparks, 0.38, 0.45, 0.38, 0.035);
        world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 0.08, actor.getZ(), 2, 0.35, 0.04, 0.35, 0.01);
    }

    private static void spawnAbsorbEffects(ServerWorld world, LivingEntity actor, int storedDamageInstances) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 10, 0.32, 0.38, 0.32, 0.08);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 6, 0.22, 0.28, 0.22, 0.035);
        float pitch = 0.9F + Math.min(0.55F, storedDamageInstances * 0.035F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_BLOCK_01.get(),
                actor.getSoundCategory(), 0.45F, pitch);
    }

    private static void spawnDashStartEffects(ServerWorld world, LivingEntity actor, int storedDamageInstances) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                24 + storedDamageInstances, 0.45, 0.45, 0.45, 0.12);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                actor.getSoundCategory(), 0.55F, 1.35F);
    }

    private static void spawnDashHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, 0.35, 0.35, 0.35, 0.1);
        world.playSound(null, target.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                target.getSoundCategory(), 0.25F, 1.2F);
    }

    private static void spawnDissipatedChainEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.65, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 8, 0.28, 0.3, 0.28, 0.08);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(),
                actor.getSoundCategory(), 0.2F, 1.6F);
    }

    private static void spawnDashEndEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0.0, 0.2, 0.0);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, 0.35, 0.08, 0.35, 0.03);
    }

    private static void spawnCancelledEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0.01);
    }

    private enum Phase {
        CHARGING,
        DASHING
    }

    private static final class ActiveThunderBlitz {
        private final UUID actorId;
        private final UUID targetId;
        private final ItemStack stack;
        private final Hand hand;
        private final Vec3d fallbackDirection;
        private final long startedAt;
        private final Set<UUID> dashHitTargets = new HashSet<>();
        private Phase phase = Phase.CHARGING;
        private int storedDamageInstances;
        private long dashStartedAt;
        private Vec3d dashDirection;
        private Vec3d previousPosition;
        private int chainsReleased;

        private ActiveThunderBlitz(UUID actorId, UUID targetId, ItemStack stack, Hand hand,
                                   Vec3d fallbackDirection, long startedAt) {
            this.actorId = actorId;
            this.targetId = targetId;
            this.stack = stack;
            this.hand = hand;
            this.fallbackDirection = fallbackDirection;
            this.startedAt = startedAt;
        }
    }
}
