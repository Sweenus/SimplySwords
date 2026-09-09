package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryTuning;
import net.sweenus.simplyswords.api.ability.ArcaneCosmicMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class ArcanethystAssaultManager {

    private static final Map<ServerWorld, List<ActiveAssault>> ACTIVE_ASSAULTS = new HashMap<>();
    private static final Map<ServerWorld, List<PendingPulse>> PENDING_PULSES = new HashMap<>();
    private static final Map<ServerWorld, List<AmethystField>> FIELDS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Long>> ARCANE_BRANDS = new HashMap<>();
    private static final Map<UUID, Integer> PASSIVE_PROCS = new HashMap<>();
    private static final Map<UUID, Long> OVERFLOW_LOCKOUTS = new HashMap<>();
    private static final int TARGET_SCAN_INTERVAL_TICKS = 5;
    private static final double PLAYER_WIDTH_LIMIT = 1.6001;
    private static final double PLAYER_HEIGHT_LIMIT = 2.8001;

    private ArcanethystAssaultManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveAssault> assaults = ACTIVE_ASSAULTS.get(world);
        return assaults != null && !assaults.isEmpty()
                || !PENDING_PULSES.getOrDefault(world, List.of()).isEmpty()
                || !FIELDS.getOrDefault(world, List.of()).isEmpty();
    }

    public static void clear(ServerWorld world) {
        List<ActiveAssault> assaults = ACTIVE_ASSAULTS.remove(world);
        if (assaults != null) assaults.forEach(assault -> {
            restoreTargets(world, assault);
            ArcaneCosmicMasteryCombatManager.finish(assault.execution, assault.processedTargets.size());
        });
        PENDING_PULSES.remove(world);
        FIELDS.remove(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        ARCANE_BRANDS.remove(actor.getUuid());
        OVERFLOW_LOCKOUTS.remove(actor.getUuid());
        PASSIVE_PROCS.remove(actor.getUuid());
    }

    public static void clearAll() {
        ACTIVE_ASSAULTS.clear();
        PENDING_PULSES.clear();
        FIELDS.clear();
        ARCANE_BRANDS.clear();
        OVERFLOW_LOCKOUTS.clear();
        PASSIVE_PROCS.clear();
    }

    public static void start(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage) {
        start(world, owner, stack, radius, damage, ArcaneCosmicMasteryTuning.EMPTY, null);
    }

    public static void start(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage,
                             ArcaneCosmicMasteryTuning suspension, UniqueAbilityExecution execution) {
        if (owner == null || !owner.isAlive()) return;
        UniqueAbilityExecution impactExecution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.ARCANETHYST_IMPACT, world, stack, owner, null, impactBase());
        ArcaneCosmicMasteryTuning impact = ArcaneCosmicMasteryAbilities.tuning(impactExecution);
        ArcaneCosmicMasteryCombatManager.finish(impactExecution, 0);
        start(world, owner, stack, radius, damage, suspension, impact, execution);
    }

    public static void start(ServerWorld world, LivingEntity owner, ItemStack stack, double radius, float damage,
                             ArcaneCosmicMasteryTuning suspension, ArcaneCosmicMasteryTuning impact,
                             UniqueAbilityExecution execution) {
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
            ArcaneCosmicMasteryCombatManager.finish(assault.execution, assault.processedTargets.size());
            return true;
        });
        ActiveAssault assault = new ActiveAssault(owner.getUuid(), stack.copy(), now,
                now + Math.max(1, suspension.integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS,
                        Config.uniqueEffects.arcanethyst.duration)), now,
                suspension.get(ArcaneCosmicMasteryTuning.Setting.RADIUS, radius), damage,
                new ArrayList<>(), new HashSet<>(), suspension, impact, execution);
        scanForTargets(world, owner, assault);
        assaults.add(assault);
        spawnCastParticles(world, owner.getPos());
        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.55F, 1.15F);
    }

    public static void tick(ServerWorld world) {
        tickPendingPulses(world);
        tickFields(world);
        List<ActiveAssault> assaults = ACTIVE_ASSAULTS.get(world);
        if (assaults == null || assaults.isEmpty()) {
            return;
        }

        Iterator<ActiveAssault> iterator = assaults.iterator();
        while (iterator.hasNext()) {
            ActiveAssault assault = iterator.next();
            if (tickAssault(world, assault)) {
                ArcaneCosmicMasteryCombatManager.finish(assault.execution, assault.processedTargets.size());
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
        int liftTicks = Math.max(1, assault.suspension().integer(
                ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, Config.uniqueEffects.arcanethyst.liftTicks));
        int suspendTicks = Math.max(0, assault.suspension().integer(
                ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, Config.uniqueEffects.arcanethyst.suspendTicks));
        int slamTicks = Math.max(6, assault.impact().integer(
                ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, Config.uniqueEffects.arcanethyst.slamTicks));

        assault.targets().removeIf(active -> {
            Entity entity = world.getEntity(active.targetId());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                restoreTarget(world, active);
                return true;
            }

            long age = world.getTime() - active.startTick();
            target.fallDistance = 0.0F;
            if (age < liftTicks) {
                tickLift(world, owner, target, active, age, liftTicks, assault.suspension());
                return false;
            }
            if (age < liftTicks + suspendTicks) {
                tickSuspend(world, owner, target, active, assault.suspension());
                return false;
            }

            long slamAge = age - liftTicks - suspendTicks;
            if (!active.slamStarted()) {
                active.markSlamStarted();
                target.setNoGravity(false);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(), SoundCategory.PLAYERS, 0.35F, 0.85F + world.random.nextFloat() * 0.2F);
            }
            return tickSlam(world, owner, assault, target, active, slamAge, slamTicks);
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
                double height = assault.impact().flag(1 << 25)
                        ? assault.impact().get(ArcaneCosmicMasteryTuning.Setting.HEIGHT,
                        Config.uniqueEffects.arcanethyst.liftHeight)
                        : assault.suspension().get(ArcaneCosmicMasteryTuning.Setting.HEIGHT,
                        Config.uniqueEffects.arcanethyst.liftHeight);
                assault.targets().add(new ActiveTarget(target.getUuid(), world.getTime(), target.getY(),
                        target.getY() + height, target.hasNoGravity()));
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

    private static void tickLift(ServerWorld world, LivingEntity owner, LivingEntity target, ActiveTarget active,
                                 long age, int liftTicks, ArcaneCosmicMasteryTuning tuning) {
        if (tuning.flag(1 << 17)) {
            Vec3d pull = owner.getPos().subtract(target.getPos()).multiply(1, 0, 1);
            target.setVelocity(pull.lengthSquared() == 0 ? Vec3d.ZERO : pull.normalize().multiply(
                    tuning.get(ArcaneCosmicMasteryTuning.Setting.PULL_STRENGTH, .18)));
            target.velocityModified = true;
            return;
        }
        double t = MathHelper.clamp((double) age / (double) liftTicks, 0.0, 1.0);
        double eased = 1.0 - Math.pow(1.0 - t, 3.0);
        double wantedY = MathHelper.lerp(eased, active.startY(), active.hoverY());
        target.setVelocity(0.0, MathHelper.clamp((wantedY - target.getY()) * 0.34, 0.08, 0.45), 0.0);
        target.velocityModified = true;
        spawnLiftParticles(world, target);
    }

    private static void tickSuspend(ServerWorld world, LivingEntity owner, LivingEntity target,
                                    ActiveTarget active, ArcaneCosmicMasteryTuning tuning) {
        Vec3d velocity = target.getVelocity();
        double horizontal = tuning.flag(1 << 13) ? .3 : 0;
        target.setVelocity(velocity.x * horizontal, (active.hoverY() - target.getY()) * 0.28,
                velocity.z * horizontal);
        target.velocityModified = true;
        spawnSuspendParticles(world, target);
    }

    private static boolean tickSlam(ServerWorld world, LivingEntity owner, ActiveAssault assault,
                                    LivingEntity target, ActiveTarget active, long slamAge, int slamTicks) {
        target.setVelocity(0.0, -1.75, 0.0);
        target.velocityModified = true;
        spawnSlamTrail(world, target);
        if (target.isOnGround() || slamAge >= slamTicks) {
            restoreTarget(world, active);
            target.fallDistance = 0.0F;
            var damageSource = world.getDamageSources().indirectMagic(owner, owner);
            float multiplier = (float) assault.impact().get(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER,
                    Config.uniqueEffects.arcanethyst.slamDamageMultiplier);
            Map<UUID, Long> brands = ARCANE_BRANDS.get(owner.getUuid());
            if (brands != null && brands.getOrDefault(target.getUuid(), 0L) > world.getTime()) multiplier *= 1.1F;
            if (assault.suspension().flag(1 << 15)) multiplier *= 1 + Math.min(8,
                    assault.targets().size()) * assault.suspension().get(
                    ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, .03);
            if (assault.suspension().flag(1 << 16)) multiplier *= assault.suspension().get(
                    ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1.4);
            if (assault.suspension().flag(1 << 17)) multiplier *= assault.suspension().get(
                    ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
            if (assault.impact().flag(1 << 22)) {
                double lifted = Math.max(0, active.hoverY() - active.startY());
                double beyond = Math.max(0, lifted - assault.impact().integer(ArcaneCosmicMasteryTuning.Setting.COUNT, 4));
                multiplier *= 1 + Math.min(beyond, assault.impact().integer(ArcaneCosmicMasteryTuning.Setting.COUNT, 4))
                        * assault.impact().get(ArcaneCosmicMasteryTuning.Setting.PER_STACK_MULTIPLIER, .05);
            }
            float slamDamage = HelperMethods.applyAbilityDamageEnchantments(world, assault.stack(), target,
                    damageSource, assault.damage() * multiplier);
            HelperMethods.damageThroughIframes(target, damageSource, slamDamage);
            applyImpact(world, owner, assault, target, slamDamage);
            if (assault.suspension().flag(1 << 14)) target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, assault.suspension().integer(
                    ArcaneCosmicMasteryTuning.Setting.STATUS_DURATION_TICKS, 30), 2), owner);
            spawnImpact(world, target.getPos());
            return true;
        }
        return false;
    }

    public static void markTarget(ServerWorld world, LivingEntity owner, LivingEntity target,
                                  ArcaneCosmicMasteryTuning tuning) {
        if (!tuning.flag(1 << 3)) return;
        Map<UUID, Long> brands = ARCANE_BRANDS.computeIfAbsent(owner.getUuid(), ignored -> new HashMap<>());
        brands.put(target.getUuid(), world.getTime() + tuning.integer(
                ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, 60));
        if (brands.size() > 32) brands.entrySet().stream().min(Map.Entry.comparingByValue())
                .ifPresent(entry -> brands.remove(entry.getKey()));
    }

    public static float sparkDamage(ArcaneCosmicMasteryTuning tuning, float base) {
        return base * (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                * (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.SPELL_MULTIPLIER, 1);
    }

    public static float modifyOutgoingMeleeDamage(LivingEntity target, DamageSource source, float amount) {
        if (target == null || source == null || amount <= 0 || !target.hasStatusEffect(StatusEffects.LEVITATION)
                || !source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || !(source.getAttacker() instanceof ServerPlayerEntity player)
                || source.getSource() != player) return amount;
        ItemStack stack = source.getWeaponStack();
        if (stack == null || !stack.isOf(ItemsRegistry.ARCANETHYST.get())) stack = player.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.ARCANETHYST.get()) || !AwakeningApi.isAbilityUnlocked(stack)) return amount;
        UniqueAbilityExecution execution = ArcaneCosmicMasteryCombatManager.beginPassive(
                ArcaneCosmicMasteryAbilities.ARCANETHYST_SPARK, player.getServerWorld(), stack, player, target,
                sparkBase());
        ArcaneCosmicMasteryTuning tuning = ArcaneCosmicMasteryAbilities.tuning(execution);
        try {
            if (!tuning.flag(1 << 2)) return amount;
            return amount * (float) tuning.get(
                    ArcaneCosmicMasteryTuning.Setting.ARCANETHYST_LEVITATION_MELEE_DAMAGE_MULTIPLIER, 1.12);
        } finally {
            ArcaneCosmicMasteryCombatManager.finish(execution, 0);
        }
    }

    public static void onPassiveProc(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack,
                                     ArcaneCosmicMasteryTuning tuning) {
        if (tuning.flag(1 << 5)) target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WEAKNESS, tuning.integer(
                        ArcaneCosmicMasteryTuning.Setting.SECONDARY_STATUS_DURATION_TICKS, 40), 0), owner);
        if (tuning.flag(1 << 6) && owner.getHealth() >= owner.getMaxHealth()
                && OVERFLOW_LOCKOUTS.getOrDefault(owner.getUuid(), 0L) <= world.getTime()) {
            MasteryAbsorptionTracker.grant(owner, "arcanethyst/overflow", (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.ABSORPTION, 4),
                    Math.max(1, tuning.integer(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, 60)),
                    (float) tuning.get(ArcaneCosmicMasteryTuning.Setting.ABSORPTION, 4));
            OVERFLOW_LOCKOUTS.put(owner.getUuid(), world.getTime()
                    + tuning.integer(ArcaneCosmicMasteryTuning.Setting.LOCKOUT_TICKS, 100));
        }
        int proc = PASSIVE_PROCS.merge(owner.getUuid(), 1, Integer::sum);
        if (tuning.flag(1 << 4) && proc % 4 == 0) PENDING_PULSES.computeIfAbsent(world,
                ignored -> new ArrayList<>()).add(new PendingPulse(owner.getUuid(), target.getUuid(), stack.copy(),
                world.getTime() + tuning.integer(ArcaneCosmicMasteryTuning.Setting.DELAY_TICKS, 6), tuning, false));
        if (tuning.flag(1 << 7)) PENDING_PULSES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                new PendingPulse(owner.getUuid(), target.getUuid(), stack.copy(), world.getTime() + 1, tuning, true));
    }

    private static void applyImpact(ServerWorld world, LivingEntity owner, ActiveAssault assault,
                                    LivingEntity primary, float slamDamage) {
        ArcaneCosmicMasteryTuning tuning = assault.impact();
        if (tuning.flag(1 << 21)) {
            primary.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0), owner);
            primary.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0), owner);
        }
        double radius = tuning.get(ArcaneCosmicMasteryTuning.Setting.SECONDARY_RADIUS, 0);
        if (radius > 0) {
            world.getEntitiesByClass(LivingEntity.class, primary.getBoundingBox().expand(radius),
                            entity -> entity != primary && HelperMethods.checkAbilityTarget(entity, owner))
                    .stream().limit(tuning.integer(ArcaneCosmicMasteryTuning.Setting.TARGET_CAP, 6)).forEach(entity -> {
                        HelperMethods.damageThroughIframes(entity, world.getDamageSources().indirectMagic(owner, owner),
                                slamDamage * (float) tuning.get(
                                        ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .35));
                        if (tuning.flag(1 << 23)) {
                            Vec3d push = entity.getPos().subtract(primary.getPos()).multiply(1, 0, 1);
                            if (push.lengthSquared() > 0) entity.addVelocity(push.normalize().multiply(
                                    tuning.get(ArcaneCosmicMasteryTuning.Setting.KNOCKBACK, .5)));
                        }
                    });
        }
        if (tuning.flag(1 << 24)) PENDING_PULSES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                new PendingPulse(owner.getUuid(), primary.getUuid(), assault.stack().copy(), world.getTime()
                        + tuning.integer(ArcaneCosmicMasteryTuning.Setting.DELAY_TICKS, 12), tuning, false));
        if (tuning.flag(1 << 26)) FIELDS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                new AmethystField(owner.getUuid(), assault.stack().copy(), primary.getPos(),
                        world.getTime() + Math.max(1, tuning.integer(
                                ArcaneCosmicMasteryTuning.Setting.TERTIARY_DURATION_TICKS, 80)),
                        world.getTime(), tuning, slamDamage));
    }

    private static void tickFields(ServerWorld world) {
        List<AmethystField> fields = FIELDS.get(world);
        if (fields == null || fields.isEmpty()) return;
        fields.removeIf(field -> {
            if (world.getTime() >= field.expiresAt) return true;
            LivingEntity owner = world.getEntity(field.ownerId) instanceof LivingEntity living ? living : null;
            if (owner == null || !owner.isAlive()) return true;
            int interval = Math.max(1, field.tuning.integer(
                    ArcaneCosmicMasteryTuning.Setting.TERTIARY_INTERVAL_TICKS, 20));
            if ((world.getTime() - field.startedAt) % interval != 0) return false;
            double radius = Math.max(.5, field.tuning.get(ArcaneCosmicMasteryTuning.Setting.RADIUS, 5));
            float damage = field.slamDamage * (float) field.tuning.get(
                    ArcaneCosmicMasteryTuning.Setting.SPELL_MULTIPLIER, .2);
            world.getEntitiesByClass(LivingEntity.class,
                            new net.minecraft.util.math.Box(field.center, field.center).expand(radius),
                            entity -> HelperMethods.checkAbilityTarget(entity, owner))
                    .stream().limit(field.tuning.integer(
                            ArcaneCosmicMasteryTuning.Setting.TERTIARY_TARGET_CAP, 8))
                    .forEach(entity -> HelperMethods.damageThroughIframes(entity,
                            world.getDamageSources().indirectMagic(owner, owner), damage));
            spawnImpact(world, field.center);
            return false;
        });
        if (fields.isEmpty()) FIELDS.remove(world);
    }

    private record AmethystField(UUID ownerId, ItemStack stack, Vec3d center, long expiresAt,
                                 long startedAt, ArcaneCosmicMasteryTuning tuning, float slamDamage) {
    }

    public static ArcaneCosmicMasteryTuning impactBase(double slamDamageMultiplier, int slamTicks) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, slamDamageMultiplier)
                .with(ArcaneCosmicMasteryTuning.Setting.INTERVAL_TICKS, slamTicks);
    }

    public static ArcaneCosmicMasteryTuning suspensionBase(double liftHeight, int liftTicks, int suspendTicks,
                                                     double radius, int duration) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.HEIGHT, liftHeight)
                .with(ArcaneCosmicMasteryTuning.Setting.WINDUP_TICKS, liftTicks)
                .with(ArcaneCosmicMasteryTuning.Setting.DURATION_TICKS, suspendTicks)
                .with(ArcaneCosmicMasteryTuning.Setting.RADIUS, radius)
                .with(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, duration);
    }

    public static ArcaneCosmicMasteryTuning sparkBase(int chance) {
        return ArcaneCosmicMasteryTuning.EMPTY
                .with(ArcaneCosmicMasteryTuning.Setting.CHANCE, chance)
                .with(ArcaneCosmicMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(ArcaneCosmicMasteryTuning.Setting.SPELL_MULTIPLIER, 1);
    }

    public static ArcaneCosmicMasteryTuning impactBase() {
        return impactBase(Config.uniqueEffects.arcanethyst.slamDamageMultiplier,
                Config.uniqueEffects.arcanethyst.slamTicks)
                .with(ArcaneCosmicMasteryTuning.Setting.COOLDOWN_TICKS, Config.uniqueEffects.arcanethyst.cooldown);
    }

    public static ArcaneCosmicMasteryTuning suspensionBase() {
        return suspensionBase(Config.uniqueEffects.arcanethyst.liftHeight,
                Config.uniqueEffects.arcanethyst.liftTicks, Config.uniqueEffects.arcanethyst.suspendTicks,
                Config.uniqueEffects.arcanethyst.radius, Config.uniqueEffects.arcanethyst.duration);
    }

    public static ArcaneCosmicMasteryTuning sparkBase() {
        return sparkBase(Config.uniqueEffects.arcanethyst.chance);
    }

    private static void tickPendingPulses(ServerWorld world) {
        List<PendingPulse> pulses = PENDING_PULSES.get(world);
        if (pulses == null) return;
        pulses.removeIf(pulse -> {
            if (world.getTime() < pulse.at()) return false;
            Entity ownerEntity = world.getEntity(pulse.ownerId());
            Entity targetEntity = world.getEntity(pulse.targetId());
            if (ownerEntity instanceof LivingEntity owner && targetEntity instanceof LivingEntity target
                    && owner.isAlive() && target.isAlive()) {
                if (pulse.extraLevitation()) {
                    world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(4),
                                    entity -> entity != target && HelperMethods.checkAbilityTarget(entity, owner))
                            .stream().findFirst().ifPresent(entity -> entity.addStatusEffect(
                                    new StatusEffectInstance(StatusEffects.LEVITATION,
                                            pulse.tuning().integer(ArcaneCosmicMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 36), 1), owner));
                } else {
                    float damage = HelperMethods.abilityScaledDamage("arcane", owner, pulse.stack(),
                            Config.uniqueEffects.arcanethyst.damageScaling * (float) pulse.tuning().get(
                                    ArcaneCosmicMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .4),
                            Config.uniqueEffects.arcanethyst.spellScaling);
                    world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(
                                            pulse.tuning().get(ArcaneCosmicMasteryTuning.Setting.RADIUS, 3)),
                                    entity -> HelperMethods.checkAbilityTarget(entity, owner))
                            .forEach(entity -> HelperMethods.damageThroughIframes(entity,
                                    world.getDamageSources().indirectMagic(owner, owner), damage));
                }
            }
            return true;
        });
        if (pulses.isEmpty()) PENDING_PULSES.remove(world);
        OVERFLOW_LOCKOUTS.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
        ARCANE_BRANDS.values().forEach(brands -> brands.entrySet().removeIf(
                entry -> entry.getValue() <= world.getTime()));
        ARCANE_BRANDS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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
        private final ArcaneCosmicMasteryTuning suspension;
        private final ArcaneCosmicMasteryTuning impact;
        private final UniqueAbilityExecution execution;

        private ActiveAssault(UUID ownerId, ItemStack stack, long startTick, long expiryTick, long nextScanTick,
                              double radius, float damage, List<ActiveTarget> targets, Set<UUID> processedTargets,
                              ArcaneCosmicMasteryTuning suspension, ArcaneCosmicMasteryTuning impact,
                              UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.startTick = startTick;
            this.expiryTick = expiryTick;
            this.nextScanTick = nextScanTick;
            this.radius = radius;
            this.damage = damage;
            this.targets = targets;
            this.processedTargets = processedTargets;
            this.suspension = suspension;
            this.impact = impact;
            this.execution = execution;
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

        private ArcaneCosmicMasteryTuning suspension() { return suspension; }
        private ArcaneCosmicMasteryTuning impact() { return impact; }
    }

    private record PendingPulse(UUID ownerId, UUID targetId, ItemStack stack, long at,
                                ArcaneCosmicMasteryTuning tuning, boolean extraLevitation) {
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
