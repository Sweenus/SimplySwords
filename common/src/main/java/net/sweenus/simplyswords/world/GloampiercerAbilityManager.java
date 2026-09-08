package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.GloampiercerCloneVisualEntity;
import net.sweenus.simplyswords.entity.GloampiercerSpearEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GloampiercerAbilityManager {
    private static final double GOLDEN_ANGLE = 2.399963229728653;
    private static final int FIRE_START_TICK = 12;
    private static final int FIRE_END_MARGIN = 12;
    private static final int PASSIVE_FIRE_DELAY_TICKS = 9;
    private static final int CHANNEL_MODE = 16;
    private static final Identifier CHANNEL_ROOT_ID =
            Identifier.of("simplyswords", "gloampiercer_channel_root");
    private static final UUID CAST_NAMESPACE = UUID.randomUUID();
    private static final DustColorTransitionParticleEffect GLOAM_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.025F, 0.008F, 0.07F),
                    new Vector3f(0.14F, 0.94F, 0.96F), 1.35F);
    private static final Map<ServerWorld, Map<UUID, ActiveChannel>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<PendingPassiveStrike>> PASSIVE_STRIKES = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> LAST_PASSIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Integer>> PASSIVE_PROCS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, StoredPassive>> STORED_PASSIVES = new HashMap<>();
    private static final Map<UniqueAbilityExecution, PendingExecution> PENDING = new IdentityHashMap<>();
    private static final long STALE_TRACKING_TICKS = 1200L;
    private static long sweepTick;

    private GloampiercerAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.GLOAMPIERCER.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !isActive(context.actor())
                && resolveCenter(context) != null;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        Vec3d center = resolveCenter(context);
        if (center == null) {
            return false;
        }
        ServerWorld world = context.world();
        LivingEntity owner = context.actor();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.GLOAMPIERCER_BARRAGE,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.gloampiercer.cooldown)
                        .set(AbyssalSpectralMasteryAbilities.TUNING, barrageTuning(Config.uniqueEffects.gloampiercer.cooldown,
                                Config.uniqueEffects.gloampiercer.channelDuration,
                                Config.uniqueEffects.gloampiercer.spearCount,
                                Config.uniqueEffects.gloampiercer.cloneCount)));
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        int duration = Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.CHANNEL_DURATION_TICKS,
                Config.uniqueEffects.gloampiercer.channelDuration), 20, 120);
        int cloneCount = Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.CLONE_COUNT,
                Config.uniqueEffects.gloampiercer.cloneCount), 1, 8);
        double lift = findLiftHeight(world, owner, Math.max(0.0, Config.uniqueEffects.gloampiercer.liftHeight));
        ActiveChannel channel = new ActiveChannel(owner.getUuid(), context.stack().copy(), context.hand(),
                owner.getPos(), center, owner.getY() + lift, world.getTime(), duration,
                Math.max(1.0F, HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner, context.stack(),
                        Config.uniqueEffects.gloampiercer.strikeDamageScaling,
                        Config.uniqueEffects.gloampiercer.strikeSpellScaling))
                        * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1), execution);
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 32) != 0) {
            LivingEntity royalTarget = findNearestBarrageTarget(world, owner, center);
            channel.royalTargetId = royalTarget == null ? null : royalTarget.getUuid();
            channel.royalDestination = royalTarget == null ? center
                    : royalTarget.getPos().add(0, royalTarget.getHeight() * .55, 0);
        }
        spawnActiveClones(world, owner, channel, cloneCount);
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & CHANNEL_MODE) != 0) {
            channel.rooted = true;
            applyChannelRoot(owner);
        }
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(), channel);
        spawnActivationEffects(world, owner, center);
        return true;
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity owner) {
        if (stack == null || !stack.isOf(ItemsRegistry.GLOAMPIERCER.get())
                || owner == null || !owner.isAlive() || isActive(owner)) {
            return;
        }
        long now = world.getTime();
        Map<UUID, Long> cooldowns = LAST_PASSIVE.get(world);
        Long nextEligible = cooldowns == null ? null : cooldowns.get(owner.getUuid());
        if (nextEligible != null && now < nextEligible) {
            return;
        }
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.GLOAMPIERCER_AMBUSH,
                UniqueAbilityContext.passive(world, stack, owner, null, null), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.TUNING, passiveTuning()));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        int mode = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0);
        Map<UUID, Integer> procCounts = PASSIVE_PROCS.get(world);
        int proc = nextPassiveProc(procCounts == null ? 0 : procCounts.getOrDefault(owner.getUuid(), 0));
        int cloneCount = passiveCloneCount(mode, proc,
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.CLONE_COUNT, 1));
        StoredPassive stored = peekStoredPassive(world, owner, stack, now);
        int totalClones = cloneCount + (stored == null ? 0 : 1);
        List<LivingEntity> targets = findPassiveTargets(world, owner, tuning, totalClones);
        if (targets.isEmpty()) {
            if ((mode & 2) == 0) {
                abandon(execution);
                return;
            }
            int duration = Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.DURATION_TICKS, 80));
            if (stored == null) {
                STORED_PASSIVES.computeIfAbsent(world, ignored -> new HashMap<>())
                        .put(owner.getUuid(), new StoredPassive(stack, now + duration));
            }
            commitPassiveCooldown(world, owner, stack, tuning, now);
            complete(execution, 0);
            return;
        }
        commitPassiveActivation(world, owner, stack, tuning, proc, now);
        if (stored != null) removeStoredPassive(world, owner);
        int seed = owner.getRandom().nextInt();
        int throwTick = tuning.integer(AbyssalSpectralMasteryTuning.Setting.FIRE_DELAY_TICKS,
                PASSIVE_FIRE_DELAY_TICKS);
        float baseDamage = Math.max(1.0F, HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner, stack,
                Config.uniqueEffects.gloampiercer.strikeDamageScaling,
                Config.uniqueEffects.gloampiercer.strikeSpellScaling))
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1);
        float secondary = (mode & 12) != 0 ? 1.0F
                : (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1);
        List<PassiveTarget> strikes = new ArrayList<>();
        for (int index = 0; index < totalClones; index++) {
            strikes.add(new PassiveTarget(targets.get(index % targets.size()),
                    index == 0 ? 1.0F : secondary));
        }
        for (int index = 0; index < strikes.size(); index++) {
            LivingEntity target = strikes.get(index).target;
            int cloneSeed = seed + index * 7919;
            Vec3d clonePosition = passiveClonePosition(owner, target, cloneSeed);
            GloampiercerCloneVisualEntity clone = new GloampiercerCloneVisualEntity(world,
                    clonePosition.x, clonePosition.y, clonePosition.z, yawToward(clonePosition, target.getPos()),
                    22, throwTick, 0, 1, cloneSeed);
            if (!world.spawnEntity(clone)) {
                continue;
            }
            float damage = baseDamage * strikes.get(index).damageMultiplier;
            retain(execution);
            PASSIVE_STRIKES.computeIfAbsent(world, ignored -> new ArrayList<>())
                    .add(new PendingPassiveStrike(owner.getUuid(), target.getUuid(), clone.getUuid(),
                            stack.copy(), cloneHandOrigin(clonePosition, target.getPos()), now + throwTick,
                            damage, execution));
            spawnCloneMaterialization(world, clonePosition);
        }
        complete(execution, 0);
    }

    public static boolean isActive(LivingEntity owner) {
        if (owner == null || !(owner.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveChannel> channels = ACTIVE.get(world);
        return channels != null && channels.containsKey(owner.getUuid());
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveChannel> channels = ACTIVE.get(world);
        List<PendingPassiveStrike> strikes = PASSIVE_STRIKES.get(world);
        Map<UUID, Long> cooldowns = LAST_PASSIVE.get(world);
        Map<UUID, StoredPassive> stored = STORED_PASSIVES.get(world);
        return channels != null && !channels.isEmpty()
                || strikes != null && !strikes.isEmpty()
                || cooldowns != null && !cooldowns.isEmpty()
                || stored != null && !stored.isEmpty()
                || !PENDING.isEmpty();
    }

    public static void tick(ServerWorld world) {
        tickChannels(world);
        tickPassiveStrikes(world);
        tickStoredPassives(world);
        if (world.getTime() % 200L == 0L) {
            Map<UUID, Long> cooldowns = LAST_PASSIVE.get(world);
            if (cooldowns != null) {
                long now = world.getTime();
                cooldowns.values().removeIf(expiry -> expiry <= now);
                if (cooldowns.isEmpty()) {
                    LAST_PASSIVE.remove(world);
                }
            }
            Map<UUID, Integer> procs = PASSIVE_PROCS.get(world);
            if (procs != null) {
                procs.keySet().removeIf(ownerId -> world.getEntity(ownerId) == null);
                if (procs.isEmpty()) PASSIVE_PROCS.remove(world);
            }
        }
        sweepTick++;
        if (sweepTick % 200L == 0L) {
            sweepPending();
        }
    }

    private static void sweepPending() {
        Iterator<Map.Entry<UniqueAbilityExecution, PendingExecution>> iterator =
                PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UniqueAbilityExecution, PendingExecution> entry = iterator.next();
            PendingExecution pending = entry.getValue();
            if (sweepTick - pending.touchedAt < STALE_TRACKING_TICKS) continue;
            iterator.remove();
            UniqueAbilityApi.finish(entry.getKey(), entry.getKey().definition().id(), pending.hits);
        }
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveChannel> channels = ACTIVE.remove(world);
        if (channels != null) channels.values().forEach(channel -> cancel(world, channel));
        List<PendingPassiveStrike> strikes = PASSIVE_STRIKES.remove(world);
        if (strikes != null) strikes.forEach(strike -> release(strike.execution));
        LAST_PASSIVE.remove(world);
        PASSIVE_PROCS.remove(world);
        STORED_PASSIVES.remove(world);
    }

    public static void clearAll() {
        ACTIVE.forEach((world, channels) -> channels.values().forEach(channel -> cancel(world, channel)));
        PASSIVE_STRIKES.values().forEach(strikes -> strikes
                .forEach(strike -> release(strike.execution)));
        ACTIVE.clear();
        PASSIVE_STRIKES.clear();
        LAST_PASSIVE.clear();
        PASSIVE_PROCS.clear();
        STORED_PASSIVES.clear();
        PENDING.clear();
    }

    public static void clearActor(LivingEntity owner) {
        if (owner == null || ACTIVE.isEmpty() && PASSIVE_STRIKES.isEmpty() && LAST_PASSIVE.isEmpty()
                && PASSIVE_PROCS.isEmpty() && STORED_PASSIVES.isEmpty()) {
            return;
        }
        UUID ownerId = owner.getUuid();
        removeChannelRoot(owner);
        ACTIVE.forEach((world, channels) -> {
            ActiveChannel channel = channels.remove(ownerId);
            if (channel != null) cancel(world, channel);
        });
        ACTIVE.values().removeIf(Map::isEmpty);
        PASSIVE_STRIKES.forEach((world, strikes) -> strikes.removeIf(strike -> {
            if (!strike.ownerId.equals(ownerId)) return false;
            discard(world, strike.cloneId);
            release(strike.execution);
            return true;
        }));
        PASSIVE_STRIKES.values().removeIf(List::isEmpty);
        LAST_PASSIVE.values().forEach(cooldowns -> cooldowns.remove(ownerId));
        LAST_PASSIVE.values().removeIf(Map::isEmpty);
        PASSIVE_PROCS.values().forEach(procs -> procs.remove(ownerId));
        PASSIVE_PROCS.values().removeIf(Map::isEmpty);
        STORED_PASSIVES.values().forEach(stored -> stored.remove(ownerId));
        STORED_PASSIVES.values().removeIf(Map::isEmpty);
    }

    private static void tickStoredPassives(ServerWorld world) {
        Map<UUID, StoredPassive> stored = STORED_PASSIVES.get(world);
        if (stored == null) return;
        long now = world.getTime();
        stored.entrySet().removeIf(entry -> {
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) return true;
            StoredPassive value = entry.getValue();
            return now >= value.expiresAt || owner.getMainHandStack() != value.stack
                    && owner.getOffHandStack() != value.stack;
        });
        if (stored.isEmpty()) STORED_PASSIVES.remove(world);
    }

    private static void commitPassiveActivation(ServerWorld world, LivingEntity owner, ItemStack stack,
                                                AbyssalSpectralMasteryTuning tuning, int proc, long now) {
        PASSIVE_PROCS.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(), proc);
        commitPassiveCooldown(world, owner, stack, tuning, now);
    }

    private static void commitPassiveCooldown(ServerWorld world, LivingEntity owner, ItemStack stack,
                                              AbyssalSpectralMasteryTuning tuning, long now) {
        int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, owner,
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.PASSIVE_COOLDOWN_TICKS,
                        Config.uniqueEffects.gloampiercer.passiveCooldown));
        LAST_PASSIVE.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(owner.getUuid(), now + cooldown);
    }

    private static StoredPassive peekStoredPassive(ServerWorld world, LivingEntity owner,
                                                   ItemStack stack, long now) {
        Map<UUID, StoredPassive> stored = STORED_PASSIVES.get(world);
        StoredPassive value = stored == null ? null : stored.get(owner.getUuid());
        return value != null && value.stack == stack && now < value.expiresAt ? value : null;
    }

    private static void removeStoredPassive(ServerWorld world, LivingEntity owner) {
        Map<UUID, StoredPassive> stored = STORED_PASSIVES.get(world);
        if (stored == null) return;
        stored.remove(owner.getUuid());
        if (stored.isEmpty()) STORED_PASSIVES.remove(world);
    }

    private static void tickChannels(ServerWorld world) {
        Map<UUID, ActiveChannel> channels = ACTIVE.get(world);
        if (channels == null || channels.isEmpty()) {
            return;
        }
        Iterator<ActiveChannel> iterator = channels.values().iterator();
        while (iterator.hasNext()) {
            ActiveChannel channel = iterator.next();
            Entity entity = world.getEntity(channel.ownerId);
            if (!(entity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || !isStillWielding(owner, channel)) {
                cancel(world, channel);
                iterator.remove();
                continue;
            }
            long age = world.getTime() - channel.startedAt;
            guideOwner(owner, channel, age);
            fireScheduledSpears(world, owner, channel, age);
            if (age >= channel.duration) {
                owner.setVelocity(owner.getVelocity().x, Math.min(owner.getVelocity().y, -0.04), owner.getVelocity().z);
                owner.velocityModified = true;
                if (channel.rooted) removeChannelRoot(owner);
                complete(channel.execution, 0);
                iterator.remove();
            }
        }
        if (channels.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void tickPassiveStrikes(ServerWorld world) {
        List<PendingPassiveStrike> strikes = PASSIVE_STRIKES.get(world);
        if (strikes == null || strikes.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<PendingPassiveStrike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            PendingPassiveStrike strike = iterator.next();
            if (now < strike.triggerAt) {
                continue;
            }
            Entity ownerEntity = world.getEntity(strike.ownerId);
            Entity targetEntity = world.getEntity(strike.targetId);
            if (ownerEntity instanceof LivingEntity owner && owner.isAlive() && !owner.isRemoved()
                    && targetEntity instanceof LivingEntity target && target.isAlive() && !target.isRemoved()
                    && HelperMethods.checkAbilityTarget(target, owner)) {
                launchSpear(world, owner, strike.stack, strike.origin,
                        target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), target, strike.damage,
                        strike.execution);
                world.playSound(null, strike.origin.x, strike.origin.y, strike.origin.z,
                        SoundRegistry.DARK_SWORD_WHOOSH_02.get(), SoundCategory.PLAYERS,
                        0.56F, 1.35F + world.random.nextFloat() * 0.12F);
            } else {
                discard(world, strike.cloneId);
            }
            release(strike.execution);
            iterator.remove();
        }
        if (strikes.isEmpty()) {
            PASSIVE_STRIKES.remove(world);
        }
    }

    private static void guideOwner(LivingEntity owner, ActiveChannel channel, long age) {
        int liftTicks = Math.min(10, Math.max(4, channel.duration / 4));
        int releaseTick = Math.max(liftTicks, channel.duration - 8);
        double retention = MathHelper.clamp(AbyssalSpectralMasteryAbilities.tuning(channel.execution).get(
                AbyssalSpectralMasteryTuning.Setting.MOVEMENT_RETENTION,
                Config.uniqueEffects.gloampiercer.movementRetention), 0.0, 1.0);
        Vec3d velocity = owner.getVelocity();
        if (age < releaseTick) {
            double targetY;
            if (age < liftTicks) {
                double progress = MathHelper.clamp((double) (age + 1) / liftTicks, 0.0, 1.0);
                double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
                targetY = MathHelper.lerp(eased, channel.start.y, channel.hoverY);
            } else {
                targetY = channel.hoverY + Math.sin((age - liftTicks) * 0.22) * 0.05;
            }
            owner.setVelocity(velocity.x * retention,
                    MathHelper.clamp((targetY - owner.getY()) * 0.36, -0.18, 0.46),
                    velocity.z * retention);
            owner.fallDistance = 0.0F;
            owner.velocityModified = true;
        } else {
            owner.setVelocity(velocity.x * retention, Math.min(velocity.y, -0.03), velocity.z * retention);
            owner.velocityModified = true;
        }
    }

    private static void fireScheduledSpears(ServerWorld world, LivingEntity owner,
                                             ActiveChannel channel, long age) {
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(channel.execution);
        int count = spearCount(tuning);
        int sources = channel.clonePositions.size() + 1;
        int startTick = tuning.integer(AbyssalSpectralMasteryTuning.Setting.FIRE_DELAY_TICKS, FIRE_START_TICK);
        int endMargin = tuning.integer(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, FIRE_END_MARGIN);
        while (channel.fired < count && age >= scheduledFireTick(channel.fired, count, sources,
                channel.duration, startTick, endMargin)) {
            fireSpear(world, owner, channel, channel.fired, count);
            channel.fired++;
        }
    }

    private static int spearCount(AbyssalSpectralMasteryTuning tuning) {
        return Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.SPEAR_COUNT,
                Config.uniqueEffects.gloampiercer.spearCount), 3, 36);
    }

    static int fireIntervalTicks(int spearCount, int sourceCount, int span) {
        return Math.max(1, (int) Math.round((double) Math.max(1, span) * Math.max(1, sourceCount)
                / Math.max(1, spearCount)));
    }

    static int scheduledFireTick(int index, int spearCount, int sourceCount, int duration,
                                 int startTick, int endMargin) {
        int endTick = Math.max(startTick + 1, duration - endMargin);
        int sources = Math.max(1, sourceCount);
        int interval = fireIntervalTicks(spearCount, sources, endTick - startTick);
        return startTick + index / sources * interval
                + (int) Math.round((double) (index % sources) * interval / sources);
    }

    private static void fireSpear(ServerWorld world, LivingEntity owner,
                                  ActiveChannel channel, int index, int count) {
        int sourceCount = channel.clonePositions.size() + 1;
        int sourceIndex = Math.floorMod(index, sourceCount);
        Vec3d origin = sourceIndex == 0
                ? owner.getPos().add(0.0, owner.getHeight() * 0.68, 0.0)
                : cloneHandOrigin(channel.clonePositions.get(sourceIndex - 1), channel.center);
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(channel.execution);
        int mode = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0);
        int groundInterval = tuning.integer(AbyssalSpectralMasteryTuning.Setting.INTERVAL_TICKS, 3);
        boolean groundStrike = (mode & 16) != 0 || (mode & 32) == 0 && index % groundInterval == groundInterval - 1;
        LivingEntity target = groundStrike ? null : selectBarrageTarget(world, owner, channel, index);
        if (target != null && (mode & 32) != 0) {
            channel.royalDestination = target.getPos().add(0, target.getHeight() * .55, 0);
        }
        Vec3d destination = target == null
                ? (mode & 32) != 0 && channel.royalDestination != null ? channel.royalDestination
                : groundStrikePosition(world, channel.center, index, count,
                        tuning.get(AbyssalSpectralMasteryTuning.Setting.RADIUS, Config.uniqueEffects.gloampiercer.barrageRadius))
                : target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        launchSpear(world, owner, channel.stack, origin, destination, target, channel.damage, channel.execution);
        if (sourceIndex == 0) {
            Hand hand = channel.hand == null ? Hand.MAIN_HAND : channel.hand;
            RunicSlashManager.runSuppressed(() -> owner.swingHand(hand, true));
        }
        world.spawnParticles(GLOAM_DUST, origin.x, origin.y, origin.z,
                7, 0.12, 0.12, 0.12, 0.025);
        world.playSound(null, origin.x, origin.y, origin.z, SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                SoundCategory.PLAYERS, 0.4F, 1.25F + world.random.nextFloat() * 0.18F);
    }

    private static void launchSpear(ServerWorld world, LivingEntity owner, ItemStack stack,
                                    Vec3d origin, Vec3d destination, LivingEntity target, float damage,
                                    UniqueAbilityExecution execution) {
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        GloampiercerSpearEntity spear = new GloampiercerSpearEntity(world, owner, stack,
                origin, destination, target, damage,
                Math.max(0.1, tuning.get(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED,
                        Config.uniqueEffects.gloampiercer.projectileSpeed)), execution);
        if (!world.spawnEntity(spear)) {
            spear.releaseTracking();
        }
    }

    private static LivingEntity selectBarrageTarget(ServerWorld world, LivingEntity owner,
                                                     ActiveChannel channel, int index) {
        if (channel.royalDestination != null) {
            Entity entity = channel.royalTargetId == null ? null : world.getEntity(channel.royalTargetId);
            return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                    && HelperMethods.checkAbilityTarget(living, owner) ? living : null;
        }
        double radius = Math.max(1.0, Config.uniqueEffects.gloampiercer.barrageRadius);
        Box box = Box.of(channel.center, radius * 2.0, 8.0, radius * 2.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && horizontalDistanceSquared(entity.getPos(), channel.center) <= radius * radius)
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(channel.center)))
                .toList();
        return targets.isEmpty() ? null : targets.get(Math.floorMod(index / 3 + index, targets.size()));
    }

    private static LivingEntity findNearestBarrageTarget(ServerWorld world, LivingEntity owner, Vec3d center) {
        double radius = Math.max(1, Config.uniqueEffects.gloampiercer.barrageRadius);
        return world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, 8, radius * 2),
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && horizontalDistanceSquared(entity.getPos(), center) <= radius * radius)
                .stream().min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(center))).orElse(null);
    }

    private static Vec3d groundStrikePosition(ServerWorld world, Vec3d center, int index, int count,
                                              double configuredRadius) {
        double radius = Math.max(1.0, configuredRadius);
        double fraction = Math.sqrt((index + 0.5) / Math.max(1, count));
        double angle = index * GOLDEN_ANGLE + Math.floorMod(index * 31, 17) * 0.037;
        double distance = radius * fraction * (0.78 + Math.floorMod(index * 13, 19) / 90.0);
        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        double y = LivyatanWaveManager.findGroundTopY(world, x, z, center.y + 4.0);
        return new Vec3d(x, y + 0.05, z);
    }

    private static List<LivingEntity> findPassiveTargets(ServerWorld world, LivingEntity owner,
                                                          AbyssalSpectralMasteryTuning tuning, int cap) {
        double minimum = Math.max(0.0, Config.uniqueEffects.gloampiercer.passiveMinRange);
        double maximum = Math.max(minimum + 0.1, tuning.get(AbyssalSpectralMasteryTuning.Setting.RANGE,
                Config.uniqueEffects.gloampiercer.passiveMaxRange));
        double minimumSquared = minimum * minimum;
        double maximumSquared = maximum * maximum;
        double threshold = Math.cos(Math.toRadians(
                MathHelper.clamp(tuning.get(AbyssalSpectralMasteryTuning.Setting.CONE_DEGREES,
                        Config.uniqueEffects.gloampiercer.passiveConeDegrees), 1.0, 180.0) * 0.5));
        boolean nearestFirst = (tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 8) != 0;
        Vec3d look = owner.getRotationVec(1.0F).normalize();
        return world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(maximum),
                        entity -> entity != owner && entity.isAlive() && !entity.isRemoved()
                                && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                                && HelperMethods.checkAbilityTarget(entity, owner)
                                && entity.squaredDistanceTo(owner) >= minimumSquared
                                && entity.squaredDistanceTo(owner) <= maximumSquared
                                && owner.canSee(entity))
                .stream()
                .filter(entity -> directionTo(owner, entity).dotProduct(look) >= threshold)
                .sorted(Comparator.comparingDouble(entity -> nearestFirst
                        ? owner.squaredDistanceTo(entity)
                        : (1.0 - directionTo(owner, entity).dotProduct(look)) * 100.0
                        + owner.squaredDistanceTo(entity) * 0.02))
                .limit(cap)
                .toList();
    }

    private static Vec3d directionTo(LivingEntity owner, LivingEntity target) {
        Vec3d offset = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0).subtract(owner.getEyePos());
        return offset.lengthSquared() < 1.0E-6 ? owner.getRotationVec(1.0F) : offset.normalize();
    }

    private static Vec3d passiveClonePosition(LivingEntity owner, LivingEntity target, int seed) {
        Vec3d forward = target.getPos().subtract(owner.getPos());
        forward = forward.horizontalLengthSquared() < 1.0E-6
                ? Vec3d.fromPolar(0.0F, owner.getYaw())
                : new Vec3d(forward.x, 0.0, forward.z).normalize();
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        double sign = (seed & 1) == 0 ? 1.0 : -1.0;
        double lateral = 1.8 + Math.floorMod(seed, 9) * 0.1;
        double backward = 0.6 + Math.floorMod(seed >>> 4, 7) * 0.08;
        double height = target.getHeight() + 1.0 + Math.floorMod(seed >>> 8, 8) * 0.1;
        return target.getPos().add(side.multiply(sign * lateral)).subtract(forward.multiply(backward)).add(0.0, height, 0.0);
    }

    private static void spawnActiveClones(ServerWorld world, LivingEntity owner,
                                          ActiveChannel channel, int cloneCount) {
        Vec3d facing = channel.center.subtract(channel.start);
        facing = facing.horizontalLengthSquared() < 1.0E-6
                ? Vec3d.fromPolar(0.0F, owner.getYaw())
                : new Vec3d(facing.x, 0.0, facing.z).normalize();
        Vec3d side = new Vec3d(-facing.z, 0.0, facing.x);
        for (int index = 0; index < cloneCount; index++) {
            double centered = index - (cloneCount - 1) * 0.5;
            double lateral = centered * 1.45;
            double depth = 0.9 + Math.abs(centered) * 0.28 + (index % 2) * 0.55;
            double height = index % 3 == 1 ? 0.65 : index % 3 == 2 ? 1.15 : 0.15;
            Vec3d position = channel.start.add(side.multiply(lateral)).subtract(facing.multiply(depth))
                    .add(0.0, height, 0.0);
            int seed = owner.getRandom().nextInt();
            AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(channel.execution);
            int spearCount = spearCount(tuning);
            int sourceCount = cloneCount + 1;
            int startTick = tuning.integer(AbyssalSpectralMasteryTuning.Setting.FIRE_DELAY_TICKS, FIRE_START_TICK);
            int endMargin = tuning.integer(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, FIRE_END_MARGIN);
            int firstSpear = index + 1;
            int throwCount = firstSpear < spearCount
                    ? (spearCount - firstSpear + sourceCount - 1) / sourceCount : 0;
            int firstThrow = throwCount > 0
                    ? scheduledFireTick(firstSpear, spearCount, sourceCount, channel.duration, startTick, endMargin)
                    : channel.duration + 8;
            int endTick = Math.max(startTick + 1, channel.duration - endMargin);
            int throwInterval = throwCount > 1
                    ? fireIntervalTicks(spearCount, sourceCount, endTick - startTick) : 0;
            GloampiercerCloneVisualEntity clone = new GloampiercerCloneVisualEntity(
                    world, position.x, position.y, position.z, yawToward(position, channel.center),
                    channel.duration + 4, firstThrow, throwInterval, throwCount, seed);
            if (!world.spawnEntity(clone)) {
                continue;
            }
            channel.clonePositions.add(position);
            channel.cloneIds.add(clone.getUuid());
            spawnCloneMaterialization(world, position);
        }
    }

    private static Vec3d cloneHandOrigin(Vec3d clonePosition, Vec3d targetPosition) {
        Vec3d forward = targetPosition.subtract(clonePosition);
        forward = forward.horizontalLengthSquared() < 1.0E-6
                ? new Vec3d(0.0, 0.0, 1.0)
                : new Vec3d(forward.x, 0.0, forward.z).normalize();
        return clonePosition.add(forward.multiply(0.56)).add(0.0, 1.36, 0.0);
    }

    private static double findLiftHeight(ServerWorld world, LivingEntity owner, double requested) {
        if (requested <= 0.0) {
            return 0.0;
        }
        double available = 0.0;
        Box box = owner.getBoundingBox();
        for (double offset = 0.25; offset <= requested + 1.0E-4; offset += 0.25) {
            if (!world.isSpaceEmpty(owner, box.offset(0.0, offset, 0.0))) {
                break;
            }
            available = offset;
        }
        return Math.min(requested, available);
    }

    private static Vec3d resolveCenter(WeaponAbilityContext context) {
        LivingEntity owner = context.actor();
        double range = Math.max(1.0, Config.uniqueEffects.gloampiercer.castRange);
        if (context.target() != null && context.target().isAlive()
                && context.target().squaredDistanceTo(owner) <= range * range
                && HelperMethods.checkAbilityTarget(context.target(), owner)) {
            LivingEntity target = context.target();
            return new Vec3d(target.getX(),
                    LivyatanWaveManager.findGroundTopY(context.world(), target.getX(), target.getZ(), target.getY() + 3.0),
                    target.getZ());
        }
        Vec3d direction = context.facing().lengthSquared() < 1.0E-6
                ? owner.getRotationVec(1.0F) : context.facing().normalize();
        Vec3d start = owner.getEyePos();
        Vec3d end = start.add(direction.multiply(range));
        BlockHitResult hit = context.world().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        Vec3d point = hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
        return new Vec3d(point.x,
                LivyatanWaveManager.findGroundTopY(context.world(), point.x, point.z, point.y + 4.0),
                point.z);
    }

    private static boolean isStillWielding(LivingEntity owner, ActiveChannel channel) {
        if (channel.hand != null) {
            return owner.getStackInHand(channel.hand).isOf(ItemsRegistry.GLOAMPIERCER.get());
        }
        return owner.getMainHandStack().isOf(ItemsRegistry.GLOAMPIERCER.get())
                || owner.getOffHandStack().isOf(ItemsRegistry.GLOAMPIERCER.get());
    }

    private static void cancel(ServerWorld world, ActiveChannel channel) {
        abandon(channel.execution);
        if (channel.rooted && world.getEntity(channel.ownerId) instanceof LivingEntity owner) {
            removeChannelRoot(owner);
        }
        for (UUID cloneId : channel.cloneIds) {
            discard(world, cloneId);
        }
    }

    private static void applyChannelRoot(LivingEntity owner) {
        EntityAttributeInstance movement = owner.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement == null) return;
        movement.removeModifier(CHANNEL_ROOT_ID);
        movement.addTemporaryModifier(new EntityAttributeModifier(CHANNEL_ROOT_ID, -0.99,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeChannelRoot(LivingEntity owner) {
        EntityAttributeInstance movement = owner.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(CHANNEL_ROOT_ID);
    }

    private static void discard(ServerWorld world, UUID entityId) {
        Entity entity = entityId == null ? null : world.getEntity(entityId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity owner, Vec3d center) {
        Vec3d chest = owner.getPos().add(0.0, owner.getHeight() * 0.62, 0.0);
        world.spawnParticles(GLOAM_DUST, chest.x, chest.y, chest.z,
                38, 0.7, 0.9, 0.7, 0.05);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, chest.x, chest.y, chest.z,
                10, 0.45, 0.65, 0.45, 0.025);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.15, center.z,
                34, 1.8, 0.12, 1.8, 0.1);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.88F, 0.68F);
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.78F, 0.9F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR,
                SoundCategory.PLAYERS, 0.62F, 0.72F);
    }

    private static void spawnCloneMaterialization(ServerWorld world, Vec3d position) {
        world.spawnParticles(GLOAM_DUST, position.x, position.y + 0.9, position.z,
                18, 0.28, 0.65, 0.28, 0.035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.8, position.z,
                9, 0.2, 0.55, 0.2, 0.05);
        world.playSound(null, position.x, position.y, position.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.25F, 1.48F + world.random.nextFloat() * 0.12F);
    }

    private static float yawToward(Vec3d from, Vec3d to) {
        Vec3d direction = to.subtract(from);
        return (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
    }

    private static double horizontalDistanceSquared(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    static int nextPassiveProc(int current) {
        return Math.floorMod(current, 3) + 1;
    }

    static int passiveCloneCount(int mode, int proc, int tunedCount) {
        int count = Math.clamp(tunedCount, 1, 3);
        if ((mode & 12) != 0) return count;
        return (mode & 1) != 0 && proc != 3 ? 1 : count;
    }

    public static UUID castId(UniqueAbilityExecution execution) {
        return execution == null ? null
                : UUID.nameUUIDFromBytes((CAST_NAMESPACE + "/" + execution.id()).getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void retain(UniqueAbilityExecution execution) {
        if (execution == null || execution.isTerminal()) return;
        PendingExecution pending = PENDING.computeIfAbsent(execution, ignored -> new PendingExecution());
        pending.outstanding++;
        pending.touchedAt = sweepTick;
    }

    public static void reportHits(UniqueAbilityExecution execution, int hits) {
        if (execution == null || hits <= 0) return;
        PendingExecution pending = PENDING.get(execution);
        if (pending != null) {
            pending.hits += hits;
            pending.touchedAt = sweepTick;
        }
    }

    public static void release(UniqueAbilityExecution execution) {
        PendingExecution pending = execution == null ? null : PENDING.get(execution);
        if (pending == null) return;
        pending.outstanding = Math.max(0, pending.outstanding - 1);
        settle(execution, pending);
    }

    private static void complete(UniqueAbilityExecution execution, int reportedHits) {
        if (execution == null) return;
        PendingExecution pending = PENDING.computeIfAbsent(execution, ignored -> new PendingExecution());
        pending.hits += Math.max(0, reportedHits);
        pending.completed = true;
        settle(execution, pending);
    }

    private static void settle(UniqueAbilityExecution execution, PendingExecution pending) {
        if (!pending.completed || pending.outstanding > 0) return;
        PENDING.remove(execution);
        UniqueAbilityApi.finish(execution, execution.definition().id(), pending.hits);
    }

    private static void abandon(UniqueAbilityExecution execution) {
        if (execution == null) return;
        PENDING.remove(execution);
        UniqueAbilityApi.cancel(execution);
    }

    private static final class PendingExecution {
        private int outstanding;
        private int hits;
        private long touchedAt = sweepTick;
        private boolean completed;
    }

    private static final class ActiveChannel {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Hand hand;
        private final Vec3d start;
        private final Vec3d center;
        private final double hoverY;
        private final long startedAt;
        private final int duration;
        private final float damage;
        private final UniqueAbilityExecution execution;
        private final List<Vec3d> clonePositions = new ArrayList<>();
        private final List<UUID> cloneIds = new ArrayList<>();
        private UUID royalTargetId;
        private Vec3d royalDestination;
        private boolean rooted;
        private int fired;

        private ActiveChannel(UUID ownerId, ItemStack stack, Hand hand, Vec3d start, Vec3d center,
                              double hoverY, long startedAt, int duration, float damage,
                              UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.hand = hand;
            this.start = start;
            this.center = center;
            this.hoverY = hoverY;
            this.startedAt = startedAt;
            this.duration = duration;
            this.damage = damage;
            this.execution = execution;
        }
    }

    private record PendingPassiveStrike(UUID ownerId, UUID targetId, UUID cloneId, ItemStack stack,
                                        Vec3d origin, long triggerAt, float damage,
                                        UniqueAbilityExecution execution) {
    }

    private record PassiveTarget(LivingEntity target, float damageMultiplier) {
    }

    private record StoredPassive(ItemStack stack, long expiresAt) {
    }

    private static AbyssalSpectralMasteryTuning passiveTuning() {
        return baseTuning(0, 0, 1, 1)
                .with(AbyssalSpectralMasteryTuning.Setting.FIRE_DELAY_TICKS, PASSIVE_FIRE_DELAY_TICKS);
    }

    private static AbyssalSpectralMasteryTuning barrageTuning(int cooldown, int duration,
                                                              int spears, int clones) {
        return baseTuning(cooldown, duration, spears, clones)
                .with(AbyssalSpectralMasteryTuning.Setting.FIRE_DELAY_TICKS, FIRE_START_TICK)
                .with(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, FIRE_END_MARGIN);
    }

    private static AbyssalSpectralMasteryTuning baseTuning(int cooldown, int duration, int spears, int clones) {
        return AbyssalSpectralMasteryTuning.EMPTY
                .with(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS, cooldown)
                .with(AbyssalSpectralMasteryTuning.Setting.CHANNEL_DURATION_TICKS, duration)
                .with(AbyssalSpectralMasteryTuning.Setting.SPEAR_COUNT, spears)
                .with(AbyssalSpectralMasteryTuning.Setting.CLONE_COUNT, clones)
                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_SPEED, Config.uniqueEffects.gloampiercer.projectileSpeed)
                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_DAMAGE_MULTIPLIER, 1)
                .with(AbyssalSpectralMasteryTuning.Setting.PASSIVE_COOLDOWN_TICKS, Config.uniqueEffects.gloampiercer.passiveCooldown)
                .with(AbyssalSpectralMasteryTuning.Setting.CONE_DEGREES, Config.uniqueEffects.gloampiercer.passiveConeDegrees)
                .with(AbyssalSpectralMasteryTuning.Setting.RANGE, Config.uniqueEffects.gloampiercer.passiveMaxRange)
                .with(AbyssalSpectralMasteryTuning.Setting.EXPLOSION_RADIUS, Config.uniqueEffects.gloampiercer.explosionRadius)
                .with(AbyssalSpectralMasteryTuning.Setting.TRIGGER_RADIUS, Config.uniqueEffects.gloampiercer.triggerRadius)
                .with(AbyssalSpectralMasteryTuning.Setting.EMBEDDED_DURATION_TICKS, Config.uniqueEffects.gloampiercer.embeddedDuration)
                .with(AbyssalSpectralMasteryTuning.Setting.STAIN_RADIUS, Config.uniqueEffects.gloampiercer.stainRadius)
                .with(AbyssalSpectralMasteryTuning.Setting.STAIN_DURATION_TICKS, Config.uniqueEffects.gloampiercer.stainDuration)
                .with(AbyssalSpectralMasteryTuning.Setting.STAIN_AMPLIFIER, Config.uniqueEffects.gloampiercer.stainSlowAmplifier);
    }
}
