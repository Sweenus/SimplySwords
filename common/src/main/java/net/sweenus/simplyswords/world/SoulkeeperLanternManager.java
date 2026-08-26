package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulkeeperLanternVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class SoulkeeperLanternManager {

    private static final String SOULKEEPER_LANTERN_VISUAL_TAG = "simplyswords_soulkeeper_lantern_visual";
    private static final double BASE_ANGULAR_SPEED = 0.055;
    private static final double LANTERN_HEIGHT = 1.05;
    private static final double BOB_AMPLITUDE = 0.08;
    private static final double BOB_SPEED = 0.13;
    private static final double CONTACT_RADIUS = 0.85;
    private static final int BASE_LANTERN_COUNT = 2;
    private static final int ACTIVE_LANTERN_COUNT = 4;

    private static final Map<UUID, ActiveLanterns> ACTIVE_LANTERNS = new HashMap<>();

    private SoulkeeperLanternManager() {
    }

    public static void tickPlayerFromItem(ServerPlayerEntity player, ItemStack stack) {
        tickFromItem(player, stack);
    }

    public static void tickFromItem(LivingEntity owner, ItemStack stack) {
        if (owner == null || stack == null || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.SOULKEEPER.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || !owner.getMainHandStack().equals(stack)) {
            if (owner != null && owner.getWorld() instanceof ServerWorld world) {
                discardActive(world, owner.getUuid());
                ACTIVE_LANTERNS.remove(owner.getUuid());
            }
            return;
        }
        if (owner.getWorld() instanceof ServerWorld world) {
            tickActive(world, owner, stack);
        }
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        if (!isHoldingSoulkeeper(player) || !player.isAlive()) {
            discardActive(player.getServerWorld(), player.getUuid());
            ACTIVE_LANTERNS.remove(player.getUuid());
        }
    }

    public static void tickWorld(ServerWorld world) {
        if (world.getTime() % 80L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof SoulkeeperLanternVisualEntity visual) || !entity.getCommandTags().contains(SOULKEEPER_LANTERN_VISUAL_TAG)) {
                continue;
            }
            UUID ownerId = visual.getOwnerUuid();
            ActiveLanterns active = ownerId == null ? null : ACTIVE_LANTERNS.get(ownerId);
            LivingEntity owner = resolveOwner(world, ownerId);
            if (active == null || owner == null || !owner.isAlive() || !isHoldingSoulkeeper(owner)) {
                visual.discard();
                if (ownerId != null) {
                    ACTIVE_LANTERNS.remove(ownerId);
                }
                continue;
            }
            if (!visual.getUuid().equals(active.visualId)) {
                visual.discard();
            }
        }
    }

    public static void onSoulkeeperHit(LivingEntity attacker) {
        if (isHoldingSoulkeeper(attacker)) {
            increaseSpeed(attacker);
        }
    }

    public static void onSoulkeeperHit(LivingEntity attacker, LivingEntity target, ItemStack stack) {
        onSoulkeeperHit(attacker);
        ActiveLanterns active = ACTIVE_LANTERNS.get(attacker.getUuid());
        if (active == null || target == null || !(attacker.getWorld() instanceof ServerWorld world)) return;
        if (active.soulbrandUntil.getOrDefault(target.getUuid(), 0L) > world.getTime()) {
            float bonus = (float) (HelperMethods.getEntityAttackDamage(attacker)
                    * (active.lanternTuning.get(Phase8AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1) - 1));
            if (bonus > 0) target.damage(attacker.getDamageSources().mobAttack(attacker), bonus);
        }
    }

    public static void activate(ServerPlayerEntity player, ItemStack stack) {
        activate((LivingEntity) player, stack);
    }

    public static void activate(LivingEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.SOULKEEPER.get()) || !isHoldingSoulkeeper(player)) {
            return;
        }

        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(player.getUuid(), ignored -> new ActiveLanterns());
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        active.extraLanternsUntilTick = world.getTime() + active.conclaveTuning.integer(
                Phase8AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.soulkeeper.activeExtraLanternDuration);
        increaseSpeed(active);
        int absorption = active.conclaveTuning.integer(Phase8AbilityTuning.Setting.ABSORPTION, 0);
        if (absorption > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                    active.conclaveTuning.integer(Phase8AbilityTuning.Setting.STATUS_DURATION_TICKS, 80),
                    Math.max(0, absorption / 4 - 1)), player);
        }

        Vec3d pos = player.getPos().add(0.0, 1.0, 0.0);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 18, 0.55, 0.35, 0.55, 0.03);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 8, 0.45, 0.3, 0.45, 0.02);
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_03.get(), SoundCategory.PLAYERS, 0.75F, 0.8F + player.getRandom().nextFloat() * 0.2F);
        tickActive(world, player, stack);
    }

    public static void activate(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = Phase8CombatManager.beginActive(
                Phase8UniqueAbilities.SOULKEEPER_CONCLAVE, context, Config.uniqueEffects.soulkeeper.cooldown);
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(context.actor().getUuid(), ignored -> new ActiveLanterns());
        active.conclaveTuning = Phase8UniqueAbilities.tuning(execution);
        if (context.actor().isSneaking() && active.velocityTuning.flag(1 << 17)
                && active.speedMultiplier > 1 && context.actor().getWorld() instanceof ServerWorld world) {
            float consumed = active.speedMultiplier - 1;
            float damage = HelperMethods.abilityScaledDamage("soul", context.actor(), context.stack(),
                    consumed * (float) active.velocityTuning.get(Phase8AbilityTuning.Setting.PER_STACK_MULTIPLIER, .18), 0);
            world.getEntitiesByClass(LivingEntity.class, context.actor().getBoundingBox().expand(4),
                            target -> HelperMethods.checkAbilityTarget(target, context.actor()))
                    .forEach(target -> target.damage(world.getDamageSources().indirectMagic(
                            context.actor(), context.actor()), damage));
            active.speedMultiplier = 1;
            active.extraLanternsUntilTick = 0;
            Phase8CombatManager.scheduleFinish(world, execution, 1);
            return;
        }
        activate(context.actor(), context.stack());
        UniqueAbilityApi.emit(execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                Phase8UniqueAbilities.HIT, null, 0, 0);
        Phase8CombatManager.scheduleFinish(context.world(), execution,
                active.conclaveTuning.integer(Phase8AbilityTuning.Setting.DURATION_TICKS,
                        Config.uniqueEffects.soulkeeper.activeExtraLanternDuration), 0);
    }

    private static void tickActivePlayer(ServerPlayerEntity player, ItemStack stack) {
        tickActive(player.getServerWorld(), player, stack);
    }

    private static void tickActive(ServerWorld world, LivingEntity player, ItemStack stack) {
        UUID ownerId = player.getUuid();
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(ownerId, ignored -> new ActiveLanterns());
        refreshTunings(world, player, stack, active);
        active.tick(world);

        int lanternCount = active.hasExtraLanterns(world)
                ? active.conclaveTuning.integer(Phase8AbilityTuning.Setting.COUNT, ACTIVE_LANTERN_COUNT)
                : BASE_LANTERN_COUNT;
        SoulkeeperLanternVisualEntity visual = resolveTracked(world, active.visualId, ownerId);
        if (visual == null) {
            Vec3d start = player.getPos();
            visual = new SoulkeeperLanternVisualEntity(world, ownerId, player.getId(), start.x, start.y, start.z);
            visual.addCommandTag(SOULKEEPER_LANTERN_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                active.visualId = visual.getUuid();
            }
        }

        Vec3d pos = player.getPos();
        visual.setPos(pos.x, pos.y, pos.z);
        visual.setOwnerEntityId(player.getId());
        visual.setLanternCount(lanternCount);
        visual.setSpeedMultiplier(active.speedMultiplier);
        visual.setOrbitRadius((float) active.lanternTuning.get(Phase8AbilityTuning.Setting.RADIUS,
                Config.uniqueEffects.soulkeeper.orbitRadius));
        visual.setOrbitPhase((float) active.orbitPhase);

        damageCollidingTargets(world, player, stack, active, lanternCount);
        if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 22)
                && world.getTime() - active.lastIntercept >= active.conclaveTuning.integer(
                Phase8AbilityTuning.Setting.INTERVAL_TICKS, 40)) {
            world.getEntitiesByClass(ProjectileEntity.class, player.getBoundingBox().expand(1),
                            projectile -> projectile.getOwner() != player)
                    .stream().findFirst().ifPresent(projectile -> {
                        projectile.discard();
                        active.lastIntercept = world.getTime();
                    });
        }
        if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 23)) {
            world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(5),
                            ally -> ally != player && !HelperMethods.checkAbilityTarget(ally, player))
                    .stream().limit(4).forEach(ally -> ally.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 0), player));
        }
        if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 26)
                && world.getTime() - active.lastBastionPulse >= 30) {
            active.lastBastionPulse = world.getTime();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 40, 0), player);
        }
    }

    private static void damageCollidingTargets(ServerWorld world, LivingEntity player, ItemStack stack, ActiveLanterns active, int lanternCount) {
        float damage = getLanternDamage(player, stack);
        damage = damage * (float) active.lanternTuning.get(Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                + active.lanternTuning.integer(Phase8AbilityTuning.Setting.FLAT_DAMAGE, 0);
        if (active.hasExtraLanterns(world)) {
            damage *= active.conclaveTuning.get(Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        }
        if (damage <= 0.0F || lanternCount <= 0) {
            return;
        }

        double orbitRadius = Math.max(0.25, active.lanternTuning.get(Phase8AbilityTuning.Setting.RADIUS,
                Config.uniqueEffects.soulkeeper.orbitRadius));
        double searchRadius = orbitRadius + CONTACT_RADIUS + 1.0;
        Box searchBox = player.getBoundingBox().expand(searchRadius, 2.5, searchRadius);
        Set<UUID> currentlyColliding = new HashSet<>();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, target -> target != player && target.isAlive())) {
            if (!HelperMethods.checkAbilityTarget(target, player)) {
                continue;
            }

            if (!isCollidingWithAnyLantern(player.getPos(), target.getBoundingBox(), active.orbitPhase, orbitRadius, lanternCount)) {
                continue;
            }

            UUID targetId = target.getUuid();
            currentlyColliding.add(targetId);
            long lastContact = active.lastContact.getOrDefault(targetId, Long.MIN_VALUE / 2);
            int repeatDelay = active.lanternTuning.integer(Phase8AbilityTuning.Setting.DELAY_TICKS, Integer.MAX_VALUE);
            if (active.collidingTargets.contains(targetId) && world.getTime() - lastContact < repeatDelay) {
                continue;
            }

            float contactDamage = damage;
            if (active.velocityTuning.flag(1 << 12) && active.speedMultiplier > 3) {
                contactDamage *= 1 + Math.min(.12F, (active.speedMultiplier - 3) * .03F);
            }
            if (active.lanternTuning.flag(1 << 3) && world.getTime() - lastContact <= 20) {
                contactDamage *= 1.2F;
            }
            active.contactCounter++;
            if (active.lanternTuning.flag(1 << 6) && active.contactCounter % 5 == 0) {
                contactDamage *= 1.5F;
            }
            if (damageTarget(world, player, target, contactDamage)) {
                active.lastContact.put(targetId, world.getTime());
                if (active.lastContact.size() > 32) active.lastContact.entrySet().stream()
                        .min(Map.Entry.comparingByValue()).ifPresent(entry -> active.lastContact.remove(entry.getKey()));
                if (!active.lanternTuning.flag(1 << 7)) increaseSpeed(active);
                if (active.lanternTuning.flag(1 << 2)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0), player);
                }
                if (active.lanternTuning.flag(1 << 4)) {
                    active.soulbrandUntil.put(targetId, world.getTime()
                            + active.lanternTuning.integer(Phase8AbilityTuning.Setting.DURATION_TICKS, 40));
                    if (active.soulbrandUntil.size() > 32) active.soulbrandUntil.entrySet().stream()
                            .min(Map.Entry.comparingByValue()).ifPresent(entry -> active.soulbrandUntil.remove(entry.getKey()));
                }
                if (!target.isAlive() && active.velocityTuning.flag(1 << 14)) {
                    active.capturedUntil = world.getTime()
                            + active.velocityTuning.integer(Phase8AbilityTuning.Setting.DURATION_TICKS, 60);
                }
                if (active.velocityTuning.flag(1 << 15)
                        && active.speedMultiplier >= active.velocityTuning.get(
                        Phase8AbilityTuning.Setting.STACK_CAP, Config.uniqueEffects.soulkeeper.maxSpeedMultiplier)) {
                    Vec3d pull = player.getPos().subtract(target.getPos()).multiply(1, 0, 1);
                    if (pull.lengthSquared() > 0) target.addVelocity(pull.normalize().multiply(
                            active.velocityTuning.get(Phase8AbilityTuning.Setting.PULL_STRENGTH, .25)));
                }
                if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 24)
                        && !active.recallUsed && active.extraLanternsUntilTick - world.getTime() <= 60) {
                    active.recallUsed = true;
                    active.extraLanternsUntilTick += active.conclaveTuning.integer(
                            Phase8AbilityTuning.Setting.SECONDARY_DURATION_TICKS, 80);
                }
                if (active.lanternTuning.flag(1 << 8)) {
                    double cleaveRadius = active.lanternTuning.get(
                            Phase8AbilityTuning.Setting.SECONDARY_RADIUS, 1.5);
                    float primaryMultiplier = (float) active.lanternTuning.get(
                            Phase8AbilityTuning.Setting.DAMAGE_MULTIPLIER, .75);
                    float cleaveDamage = contactDamage / Math.max(.01F, primaryMultiplier)
                            * (float) active.lanternTuning.get(
                            Phase8AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .6);
                    world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(cleaveRadius),
                                    other -> other != target && other != player && other.isAlive()
                                            && HelperMethods.checkAbilityTarget(other, player))
                            .stream().limit(2).forEach(other -> damageTarget(
                                    world, player, other, cleaveDamage));
                }
                spawnLanternHitEffects(world, target);
            }
        }

        active.collidingTargets.clear();
        active.collidingTargets.addAll(currentlyColliding);
    }

    private static float getLanternDamage(LivingEntity player, ItemStack stack) {
        return HelperMethods.abilityScaledDamage("soul", player, stack,
                Config.uniqueEffects.soulkeeper.lanternDamageScaling,
                Config.uniqueEffects.soulkeeper.spellScaling);
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity player, LivingEntity target, float damage) {
        target.timeUntilRegen = 0;
        boolean[] damaged = {false};
        ItemStack stack = player.getMainHandStack();
        var damageSource = player.getDamageSources().indirectMagic(player, player);
        float scaledDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage);
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(damageSource, scaledDamage));
        target.timeUntilRegen = 0;
        if (!damaged[0]) {
            float fallbackDamage = HelperMethods.applyNonPlayerAbilityDamageModifier(player, damage);
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(world.getDamageSources().magic(), fallbackDamage));
            target.timeUntilRegen = 0;
        }
        return damaged[0];
    }

    private static boolean isCollidingWithAnyLantern(Vec3d center, Box targetBox, double baseAngle, double orbitRadius, int lanternCount) {
        for (int i = 0; i < lanternCount; i++) {
            double angle = baseAngle + ((Math.PI * 2.0) / lanternCount) * i;
            double bob = Math.sin(baseAngle * (BOB_SPEED / BASE_ANGULAR_SPEED) + i * 1.7) * BOB_AMPLITUDE;
            Vec3d lanternCenter = center.add(Math.cos(angle) * orbitRadius, LANTERN_HEIGHT + 0.5 + bob, Math.sin(angle) * orbitRadius);
            if (squaredDistanceToBox(lanternCenter, targetBox) <= CONTACT_RADIUS * CONTACT_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static double squaredDistanceToBox(Vec3d point, Box box) {
        double dx = distanceToRange(point.x, box.minX, box.maxX);
        double dy = distanceToRange(point.y, box.minY, box.maxY);
        double dz = distanceToRange(point.z, box.minZ, box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceToRange(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0;
    }

    private static void increaseSpeed(LivingEntity player) {
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(player.getUuid(), ignored -> new ActiveLanterns());
        increaseSpeed(active);
    }

    private static void increaseSpeed(ActiveLanterns active) {
        float maximum = (float) active.velocityTuning.get(Phase8AbilityTuning.Setting.STACK_CAP,
                Config.uniqueEffects.soulkeeper.maxSpeedMultiplier);
        float gain = (float) active.velocityTuning.get(Phase8AbilityTuning.Setting.SPEED,
                Config.uniqueEffects.soulkeeper.speedIncreasePerHit);
        active.speedMultiplier = Math.min(maximum, active.speedMultiplier + gain);
        if (active.velocityTuning.flag(1 << 13) && active.speedMultiplier >= 4 && active.owner != null) {
            active.owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0), active.owner);
        }
    }

    private static void refreshTunings(ServerWorld world, LivingEntity owner, ItemStack stack, ActiveLanterns active) {
        if (world.getTime() < active.refreshAt) return;
        active.refreshAt = world.getTime() + 20;
        active.owner = owner;
        UniqueAbilityExecution lanterns = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.SOULKEEPER_LANTERNS, world, stack, owner, null);
        active.lanternTuning = Phase8UniqueAbilities.tuning(lanterns);
        UniqueAbilityApi.finish(lanterns, Phase8UniqueAbilities.FINISH, 0);
        UniqueAbilityExecution velocity = Phase8CombatManager.beginPassive(
                Phase8UniqueAbilities.SOULKEEPER_VELOCITY, world, stack, owner, null);
        active.velocityTuning = Phase8UniqueAbilities.tuning(velocity);
        UniqueAbilityApi.finish(velocity, Phase8UniqueAbilities.FINISH, 0);
    }

    private static void spawnLanternHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 4, 0.18, 0.18, 0.18, 0.025);
    }

    private static boolean isHoldingSoulkeeper(LivingEntity player) {
        return player.getMainHandStack().isOf(ItemsRegistry.SOULKEEPER.get());
    }

    private static LivingEntity resolveOwner(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        if (world.getEntity(ownerId) instanceof LivingEntity owner) {
            return owner;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerId);
        return player != null ? player : null;
    }

    private static SoulkeeperLanternVisualEntity resolveTracked(ServerWorld world, UUID visualId, UUID ownerId) {
        if (visualId == null) {
            return null;
        }

        Entity entity = world.getEntity(visualId);
        if (!(entity instanceof SoulkeeperLanternVisualEntity visual) || !ownerId.equals(visual.getOwnerUuid()) || !visual.isAlive()) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        return visual;
    }

    private static void discardActive(ServerWorld world, UUID ownerId) {
        ActiveLanterns active = ACTIVE_LANTERNS.get(ownerId);
        if (active == null || active.visualId == null) {
            return;
        }
        Entity entity = world.getEntity(active.visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static final class ActiveLanterns {
        private UUID visualId;
        private float speedMultiplier = 1.0F;
        private double orbitPhase;
        private long extraLanternsUntilTick;
        private long refreshAt;
        private long capturedUntil;
        private long lastBastionPulse;
        private long lastIntercept;
        private int contactCounter;
        private boolean recallUsed;
        private LivingEntity owner;
        private final Map<UUID, Long> lastContact = new HashMap<>();
        private final Map<UUID, Long> soulbrandUntil = new HashMap<>();
        private Phase8AbilityTuning lanternTuning = Phase8AbilityTuning.EMPTY;
        private Phase8AbilityTuning velocityTuning = Phase8AbilityTuning.EMPTY;
        private Phase8AbilityTuning conclaveTuning = Phase8AbilityTuning.EMPTY;
        private final Set<UUID> collidingTargets = new HashSet<>();

        private void tick(ServerWorld world) {
            orbitPhase += BASE_ANGULAR_SPEED * speedMultiplier;
            while (orbitPhase > Math.PI * 2.0) {
                orbitPhase -= Math.PI * 2.0;
            }
            if (speedMultiplier > 1.0F) {
                if (world.getTime() >= capturedUntil) speedMultiplier = Math.max(1.0F,
                        speedMultiplier - (float) velocityTuning.get(Phase8AbilityTuning.Setting.PER_STACK_MULTIPLIER,
                                Config.uniqueEffects.soulkeeper.speedLossPerSecond) / 20.0F);
            }
            if (velocityTuning.flag(1 << 16)) speedMultiplier = Math.max(3, Math.min(4, speedMultiplier));
            if (extraLanternsUntilTick > 0L && world.getTime() > extraLanternsUntilTick) {
                extraLanternsUntilTick = 0L;
            }
        }

        private boolean hasExtraLanterns(ServerWorld world) {
            return extraLanternsUntilTick > world.getTime();
        }
    }
}
