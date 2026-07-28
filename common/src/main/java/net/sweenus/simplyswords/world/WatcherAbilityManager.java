package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.WatcherBatEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WatcherAbilityManager {
    private static final double BAT_REACH_SQUARED = 0.7 * 0.7;
    private static final double RETURN_REACH_SQUARED = 0.8 * 0.8;
    private static final double OMEN_MAX_TETHER_SQUARED = 24.0 * 24.0;
    private static final int HUNT_STAGE_TICKS = 4;
    private static final int HUNT_MAX_TICKS = 60;
    private static final DustColorTransitionParticleEffect WATCHER_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.08F, 0.015F, 0.12F),
                    new Vector3f(0.85F, 0.05F, 1.0F), 1.15F);

    private static final Map<ServerWorld, Map<MarkKey, DreadMark>> ACTIVE_MARKS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveHunt>> ACTIVE_HUNTS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveOmen>> ACTIVE_OMENS = new HashMap<>();
    private static final Map<ServerWorld, Set<UUID>> MANAGED_BATS = new HashMap<>();

    private WatcherAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        return hasEntries(ACTIVE_MARKS.get(world))
                || hasEntries(ACTIVE_HUNTS.get(world))
                || hasEntries(ACTIVE_OMENS.get(world))
                || world.getTime() % 100L == 0L;
    }

    public static boolean isManagedBat(ServerWorld world, UUID batId) {
        Set<UUID> bats = MANAGED_BATS.get(world);
        return bats != null && bats.contains(batId);
    }

    public static void addDread(ServerWorld world, LivingEntity actor, LivingEntity target, WatcherWeaponType type) {
        if (world == null || actor == null || target == null || type == null
                || !actor.isAlive() || !isValidTarget(world, actor, null, target)) {
            return;
        }

        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.computeIfAbsent(world, ignored -> new HashMap<>());
        MarkKey key = new MarkKey(actor.getUuid(), target.getUuid(), type);
        long now = world.getTime();
        int maxDread = Math.max(1, Config.uniqueEffects.watcher.maxDread);
        DreadMark mark = marks.get(key);
        if (mark == null) {
            evictOldestMarkIfNeeded(world, marks, actor.getUuid(), type);
            mark = new DreadMark(key, now);
            marks.put(key, mark);
        }

        mark.expiryTick = now + Math.max(1, Config.uniqueEffects.watcher.dreadDuration);
        if (mark.dread < maxDread) {
            mark.dread++;
            WatcherBatEntity bat = spawnBat(world, actor, target, type, WatcherBatEntity.MODE_MARK,
                    target.getPos().add(0.0, target.getHeight() * 0.7, 0.0));
            if (bat != null) {
                mark.batIds.add(bat.getUuid());
            }
            spawnDreadGainEffects(world, target, mark.dread, maxDread);
        }
    }

    public static boolean canActivate(WeaponAbilityContext context, WatcherWeaponType type) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && resolveActivationTarget(context, type) != null;
    }

    public static boolean activate(WeaponAbilityContext context, WatcherWeaponType type) {
        if (!canActivate(context, type)) {
            return false;
        }
        LivingEntity target = resolveActivationTarget(context, type);
        if (target == null) {
            return false;
        }
        return type == WatcherWeaponType.WARGLAIVE
                ? startNightPursuit(context, target)
                : startFinalOmen(context, target);
    }

    public static void tick(ServerWorld world) {
        tickMarks(world);
        tickHunts(world);
        tickOmens(world);
        if (world.getTime() % 100L == 0L) {
            purgeOrphanBats(world);
        }
    }

    private static void tickMarks(ServerWorld world) {
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<MarkKey, DreadMark>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MarkKey, DreadMark> entry = iterator.next();
            DreadMark mark = entry.getValue();
            LivingEntity actor = getLivingEntity(world, mark.key.ownerId);
            LivingEntity target = getLivingEntity(world, mark.key.targetId);
            if (actor == null || !actor.isAlive() || target == null || !target.isAlive()
                    || world.getTime() > mark.expiryTick) {
                discardBats(world, mark.batIds);
                iterator.remove();
                continue;
            }

            mark.batIds.removeIf(id -> !(world.getEntity(id) instanceof WatcherBatEntity));
            while (mark.batIds.size() < mark.dread) {
                WatcherBatEntity bat = spawnBat(world, actor, target, mark.key.type,
                        WatcherBatEntity.MODE_MARK,
                        target.getPos().add(0.0, target.getHeight() * 0.7, 0.0));
                if (bat == null) {
                    break;
                }
                mark.batIds.add(bat.getUuid());
            }

            double angularSpeed = mark.key.type == WatcherWeaponType.WARGLAIVE ? 0.23 : -0.14;
            double radius = mark.key.type == WatcherWeaponType.WARGLAIVE ? 0.9 : 0.68;
            for (int i = 0; i < mark.batIds.size(); i++) {
                Entity entity = world.getEntity(mark.batIds.get(i));
                if (!(entity instanceof WatcherBatEntity bat)) {
                    continue;
                }
                double angle = world.getTime() * angularSpeed + mark.angleOffset
                        + (Math.PI * 2.0 * i / Math.max(1, mark.batIds.size()));
                double bob = Math.sin(world.getTime() * 0.19 + i * 1.7) * 0.16;
                Vec3d desired = target.getPos().add(
                        Math.cos(angle) * radius,
                        Math.max(0.55, target.getHeight() * 0.7) + bob,
                        Math.sin(angle) * radius
                );
                moveBatToward(bat, desired, 0.48);
                if ((world.getTime() + bat.getId()) % 8L == 0L) {
                    spawnBatAura(world, bat.getPos());
                }
            }
        }

        if (marks.isEmpty()) {
            ACTIVE_MARKS.remove(world);
        }
    }

    private static boolean startNightPursuit(WeaponAbilityContext context, LivingEntity primaryTarget) {
        ServerWorld world = context.world();
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            return false;
        }

        double radius = Math.max(1.0, Config.uniqueEffects.watcher.warglaiveHuntRadius);
        int maxTargets = Math.max(1, Config.uniqueEffects.watcher.warglaiveMaxTargets);
        List<DreadMark> selected = marks.values().stream()
                .filter(mark -> mark.key.ownerId.equals(context.actor().getUuid())
                        && mark.key.type == WatcherWeaponType.WARGLAIVE)
                .filter(mark -> {
                    LivingEntity target = getLivingEntity(world, mark.key.targetId);
                    return target != null && target.isAlive()
                            && target.squaredDistanceTo(primaryTarget) <= radius * radius
                            && isValidTarget(world, context.actor(), sourcePlayerId(context), target);
                })
                .sorted(Comparator
                        .comparing((DreadMark mark) -> !mark.key.targetId.equals(primaryTarget.getUuid()))
                        .thenComparingDouble(mark -> {
                            LivingEntity target = getLivingEntity(world, mark.key.targetId);
                            return target == null ? Double.MAX_VALUE : target.squaredDistanceTo(primaryTarget);
                        }))
                .limit(maxTargets)
                .toList();
        if (selected.isEmpty()) {
            return false;
        }

        List<UUID> batIds = new ArrayList<>();
        List<UUID> destinationSlots = new ArrayList<>();
        List<UUID> originSlots = new ArrayList<>();
        for (DreadMark mark : selected) {
            marks.remove(mark.key);
            for (UUID batId : mark.batIds) {
                if (world.getEntity(batId) instanceof WatcherBatEntity) {
                    batIds.add(batId);
                    originSlots.add(mark.key.targetId);
                }
            }
            for (int i = 0; i < mark.dread; i++) {
                destinationSlots.add(mark.key.targetId);
            }
        }
        if (batIds.isEmpty()) {
            return false;
        }
        while (destinationSlots.size() < batIds.size()) {
            destinationSlots.add(primaryTarget.getUuid());
        }

        int rotation = selected.size() > 1 ? Math.max(1, selected.get(0).dread) : 0;
        List<HuntBat> orders = new ArrayList<>();
        long now = world.getTime();
        for (int i = 0; i < batIds.size(); i++) {
            UUID destination = destinationSlots.get((i + rotation) % destinationSlots.size());
            LivingEntity origin = getLivingEntity(world, originSlots.get(i));
            LivingEntity destinationTarget = getLivingEntity(world, destination);
            Entity batEntity = world.getEntity(batIds.get(i));
            if (batEntity instanceof WatcherBatEntity bat) {
                bat.configureWatcher(context.actor(), destinationTarget, WatcherWeaponType.WARGLAIVE,
                        WatcherBatEntity.MODE_HUNT);
            }
            double angle = (Math.PI * 2.0 * i) / Math.max(1, batIds.size());
            Vec3d center = origin == null ? context.actor().getPos() : origin.getPos();
            Vec3d stagePos = center.add(Math.cos(angle) * 1.6,
                    0.65 + (i % 3) * 0.18, Math.sin(angle) * 1.6);
            orders.add(new HuntBat(batIds.get(i), destination, stagePos, now + HUNT_STAGE_TICKS));
        }

        float baseDamage = HelperMethods.abilityScaledDamage(
                "soul",
                context.actor(),
                context.stack(),
                Config.uniqueEffects.watcher.warglaiveDamageScaling,
                Config.uniqueEffects.watcher.warglaiveSpellScaling
        );
        ActiveHunt hunt = new ActiveHunt(
                context.actor().getUuid(),
                sourcePlayerId(context),
                context.stack().copy(),
                now,
                now + HUNT_MAX_TICKS,
                baseDamage,
                orders
        );
        ACTIVE_HUNTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(hunt);
        world.playSound(null, context.actor().getX(), context.actor().getY(), context.actor().getZ(),
                SoundRegistry.DARK_SWORD_SPELL.get(), SoundCategory.PLAYERS, 0.7F, 1.45F);
        return true;
    }

    private static void tickHunts(ServerWorld world) {
        List<ActiveHunt> hunts = ACTIVE_HUNTS.get(world);
        if (hunts == null || hunts.isEmpty()) {
            return;
        }

        Iterator<ActiveHunt> iterator = hunts.iterator();
        while (iterator.hasNext()) {
            ActiveHunt hunt = iterator.next();
            LivingEntity actor = getLivingEntity(world, hunt.actorId);
            if (actor == null || !actor.isAlive() || world.getTime() > hunt.expiryTick) {
                discardHunt(world, hunt);
                iterator.remove();
                continue;
            }

            Iterator<HuntBat> batIterator = hunt.bats.iterator();
            while (batIterator.hasNext()) {
                HuntBat order = batIterator.next();
                Entity entity = world.getEntity(order.batId);
                if (!(entity instanceof WatcherBatEntity bat)) {
                    unregisterBat(world, order.batId);
                    batIterator.remove();
                    continue;
                }

                if (order.returning) {
                    bat.setWatcherMode(WatcherBatEntity.MODE_RETURN);
                    Vec3d returnPoint = actor.getPos().add(0.0, actor.getHeight() * 0.65, 0.0);
                    moveBatToward(bat, returnPoint, Config.uniqueEffects.watcher.warglaiveBatSpeed);
                    if (bat.squaredDistanceTo(returnPoint) <= RETURN_REACH_SQUARED) {
                        spawnReturnEffects(world, actor, bat);
                        discardBat(world, bat);
                        batIterator.remove();
                    }
                    continue;
                }

                if (world.getTime() < order.readyTick) {
                    moveBatToward(bat, order.stagePos, Config.uniqueEffects.watcher.warglaiveBatSpeed);
                    continue;
                }

                LivingEntity target = getLivingEntity(world, order.targetId);
                if (target == null || !target.isAlive()
                        || !isValidTarget(world, actor, hunt.sourcePlayerId, target)) {
                    order.returning = true;
                    continue;
                }

                Vec3d strikePoint = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
                moveBatToward(bat, strikePoint, Config.uniqueEffects.watcher.warglaiveBatSpeed);
                if (bat.squaredDistanceTo(strikePoint) <= BAT_REACH_SQUARED) {
                    applyHuntStrike(world, actor, target, hunt);
                    order.returning = true;
                }
            }

            if (hunt.bats.isEmpty()) {
                iterator.remove();
            }
        }

        if (hunts.isEmpty()) {
            ACTIVE_HUNTS.remove(world);
        }
    }

    private static void applyHuntStrike(ServerWorld world, LivingEntity actor, LivingEntity target, ActiveHunt hunt) {
        float beforeVitality = target.getHealth() + target.getAbsorptionAmount();
        if (damageTarget(world, actor, hunt.stack, target, hunt.baseDamage)) {
            float afterVitality = target.getHealth() + target.getAbsorptionAmount();
            float removed = Math.max(0.0F, beforeVitality - afterVitality);
            float healCap = actor.getMaxHealth() * MathHelper.clamp(
                    Config.uniqueEffects.watcher.warglaiveHealCap, 0.0F, 1.0F);
            float remainingCap = Math.max(0.0F, healCap - hunt.healed);
            float heal = Math.min(remainingCap,
                    removed * Math.max(0.0F, Config.uniqueEffects.watcher.warglaiveLifeSteal));
            if (heal > 0.0F) {
                actor.heal(heal);
                hunt.healed += heal;
            }
            Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z,
                    6, 0.24, 0.28, 0.24, 0.035);
            world.spawnParticles(WATCHER_DUST, pos.x, pos.y, pos.z,
                    8, 0.3, 0.3, 0.3, 0.02);
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                    target.getSoundCategory(), 0.45F, 1.5F);
        }
    }

    private static boolean startFinalOmen(WeaponAbilityContext context, LivingEntity target) {
        ServerWorld world = context.world();
        MarkKey key = new MarkKey(context.actor().getUuid(), target.getUuid(), WatcherWeaponType.CLAYMORE);
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        DreadMark mark = marks == null ? null : marks.remove(key);
        if (mark == null || mark.dread <= 0) {
            return false;
        }

        for (UUID batId : mark.batIds) {
            Entity entity = world.getEntity(batId);
            if (entity instanceof WatcherBatEntity bat) {
                bat.configureWatcher(context.actor(), target, WatcherWeaponType.CLAYMORE,
                        WatcherBatEntity.MODE_OMEN);
            }
        }

        int duration = Math.max(20, Config.uniqueEffects.watcher.claymoreSwoopDuration);
        int slowDuration = duration + 10;
        StatusEffectInstance currentSlowness = target.getStatusEffect(StatusEffects.SLOWNESS);
        if (currentSlowness == null || currentSlowness.getAmplifier() < 3
                || currentSlowness.getDuration() < slowDuration) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    slowDuration, 3, false, false, true), context.actor());
        }

        int maxDread = Math.max(1, Config.uniqueEffects.watcher.maxDread);
        int minimumSwoops = Math.max(1, Config.uniqueEffects.watcher.claymoreMinimumSwoops);
        int maximumSwoops = Math.max(minimumSwoops, Config.uniqueEffects.watcher.claymoreMaximumSwoops);
        float dreadProgress = maxDread <= 1 ? 1.0F
                : MathHelper.clamp((float) (mark.dread - 1) / (maxDread - 1), 0.0F, 1.0F);
        int totalSwoops = Math.max(1, Math.round(MathHelper.lerp(dreadProgress, minimumSwoops, maximumSwoops)));
        float swoopDamage = HelperMethods.abilityScaledDamage(
                "soul",
                context.actor(),
                context.stack(),
                Config.uniqueEffects.watcher.claymoreSwoopDamageScaling,
                Config.uniqueEffects.watcher.claymoreSwoopSpellScaling
        );
        long now = world.getTime();
        ACTIVE_OMENS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveOmen(
                context.actor().getUuid(),
                sourcePlayerId(context),
                target.getUuid(),
                context.stack().copy(),
                context.hand() == null ? Hand.MAIN_HAND : context.hand(),
                mark.dread,
                now,
                now + duration,
                duration,
                totalSwoops,
                swoopDamage,
                new ArrayList<>(mark.batIds),
                new ArrayList<>(mark.batIds)
        ));
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z,
                10, 0.35, 0.4, 0.35, 0.025);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BAT_TAKEOFF,
                SoundCategory.PLAYERS, 0.9F, 0.55F);
        return true;
    }

    private static void tickOmens(ServerWorld world) {
        List<ActiveOmen> omens = ACTIVE_OMENS.get(world);
        if (omens == null || omens.isEmpty()) {
            return;
        }

        Iterator<ActiveOmen> iterator = omens.iterator();
        while (iterator.hasNext()) {
            ActiveOmen omen = iterator.next();
            LivingEntity actor = getLivingEntity(world, omen.actorId);
            LivingEntity target = getLivingEntity(world, omen.targetId);
            if (actor == null || !actor.isAlive() || target == null || !target.isAlive()
                    || actor.squaredDistanceTo(target) > OMEN_MAX_TETHER_SQUARED
                    || !isValidTarget(world, actor, omen.sourcePlayerId, target)) {
                discardBats(world, omen.batIds);
                iterator.remove();
                continue;
            }

            long elapsed = Math.max(0L, world.getTime() - omen.startedTick);
            while (omen.spawnedSwoops < omen.totalSwoops
                    && elapsed >= scheduledSwoopTick(omen, omen.spawnedSwoops)) {
                spawnOmenSwoop(world, actor, target, omen);
                omen.spawnedSwoops++;
            }

            for (int i = 0; i < omen.waitingBatIds.size(); i++) {
                Entity entity = world.getEntity(omen.waitingBatIds.get(i));
                if (!(entity instanceof WatcherBatEntity bat)) {
                    continue;
                }
                double angle = world.getTime() * 0.28
                        + Math.PI * 2.0 * i / Math.max(1, omen.waitingBatIds.size());
                Vec3d desired = target.getPos().add(
                        Math.cos(angle) * 0.72,
                        Math.max(0.55, target.getHeight() * 0.68) + Math.sin(angle * 1.7) * 0.15,
                        Math.sin(angle) * 0.72
                );
                moveBatToward(bat, desired, 0.68);
                spawnBatAura(world, bat.getPos());
            }

            tickOmenSwoops(world, actor, target, omen);
            if (world.getTime() >= omen.impactTick) {
                resolveFinalOmen(world, actor, target, omen);
                discardBats(world, omen.batIds);
                iterator.remove();
            }
        }

        if (omens.isEmpty()) {
            ACTIVE_OMENS.remove(world);
        }
    }

    private static long scheduledSwoopTick(ActiveOmen omen, int index) {
        if (omen.totalSwoops <= 1) {
            return 0L;
        }
        return Math.round((double) index * Math.max(1, omen.durationTicks - 8)
                / (omen.totalSwoops - 1));
    }

    private static void spawnOmenSwoop(ServerWorld world, LivingEntity actor,
                                       LivingEntity target, ActiveOmen omen) {
        double angle = world.random.nextDouble() * Math.PI * 2.0;
        double radius = 4.5 + world.random.nextDouble() * 2.5;
        double height = 4.0 + world.random.nextDouble() * 3.0;
        Vec3d spawnPos = target.getPos().add(
                Math.cos(angle) * radius,
                height,
                Math.sin(angle) * radius
        );

        WatcherBatEntity bat = null;
        while (!omen.waitingBatIds.isEmpty() && bat == null) {
            UUID batId = omen.waitingBatIds.remove(0);
            Entity entity = world.getEntity(batId);
            if (entity instanceof WatcherBatEntity watcherBat) {
                bat = watcherBat;
                bat.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z,
                        bat.getYaw(), bat.getPitch());
            }
        }
        if (bat == null) {
            bat = spawnBat(world, actor, target, WatcherWeaponType.CLAYMORE,
                    WatcherBatEntity.MODE_SWOOP, spawnPos);
            if (bat == null) {
                return;
            }
            omen.batIds.add(bat.getUuid());
        } else {
            bat.configureWatcher(actor, target, WatcherWeaponType.CLAYMORE,
                    WatcherBatEntity.MODE_SWOOP);
        }

        Vec3d strikePoint = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        Vec3d passDirection = strikePoint.subtract(spawnPos);
        if (passDirection.lengthSquared() < 1.0E-4) {
            passDirection = new Vec3d(0.0, -1.0, 0.0);
        }
        Vec3d exitPoint = strikePoint.add(passDirection.normalize().multiply(4.5));
        omen.activeSwoops.add(new OmenSwoop(bat.getUuid(), exitPoint,
                world.getTime() + 30L));
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, spawnPos.x, spawnPos.y, spawnPos.z,
                3, 0.18, 0.18, 0.18, 0.015);
        world.spawnParticles(WATCHER_DUST, spawnPos.x, spawnPos.y, spawnPos.z,
                4, 0.16, 0.16, 0.16, 0.01);
    }

    private static void tickOmenSwoops(ServerWorld world, LivingEntity actor,
                                        LivingEntity target, ActiveOmen omen) {
        Iterator<OmenSwoop> iterator = omen.activeSwoops.iterator();
        while (iterator.hasNext()) {
            OmenSwoop swoop = iterator.next();
            Entity entity = world.getEntity(swoop.batId);
            if (!(entity instanceof WatcherBatEntity bat)) {
                unregisterBat(world, swoop.batId);
                iterator.remove();
                continue;
            }

            if (world.getTime() > swoop.expiryTick) {
                discardBat(world, bat);
                iterator.remove();
                continue;
            }

            if (!swoop.hit) {
                Vec3d strikePoint = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
                moveBatToward(bat, strikePoint, 2.0);
                if (bat.squaredDistanceTo(strikePoint) <= BAT_REACH_SQUARED) {
                    applyOmenSwoopStrike(world, actor, target, omen);
                    swoop.hit = true;
                }
            } else {
                moveBatToward(bat, swoop.exitPoint, 1.75);
                if (bat.squaredDistanceTo(swoop.exitPoint) <= RETURN_REACH_SQUARED) {
                    discardBat(world, bat);
                    iterator.remove();
                }
            }
        }
    }

    private static void applyOmenSwoopStrike(ServerWorld world, LivingEntity actor,
                                              LivingEntity target, ActiveOmen omen) {
        if (!damageTargetWithoutKnockback(world, actor, omen.stack, target, omen.swoopDamage)) {
            return;
        }
        if (!target.isAlive()) {
            resetClaymoreCooldown(actor, omen.stack);
        }
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z,
                4, 0.22, 0.24, 0.22, 0.03);
        world.spawnParticles(WATCHER_DUST, pos.x, pos.y, pos.z,
                5, 0.2, 0.22, 0.2, 0.015);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                target.getSoundCategory(), 0.25F, 1.55F + world.random.nextFloat() * 0.18F);
    }

    private static void resolveFinalOmen(ServerWorld world, LivingEntity actor, LivingEntity target, ActiveOmen omen) {
        actor.swingHand(omen.hand, true);
        float missingHealth = 1.0F - MathHelper.clamp(target.getHealth() / Math.max(1.0F, target.getMaxHealth()), 0.0F, 1.0F);
        float baseDamage = HelperMethods.abilityScaledDamage(
                "soul",
                actor,
                omen.stack,
                Config.uniqueEffects.watcher.claymoreDamageScaling,
                Config.uniqueEffects.watcher.claymoreSpellScaling
        );
        float dreadMultiplier = 1.0F + Math.max(0, omen.dread - 1)
                * Math.max(0.0F, Config.uniqueEffects.watcher.claymoreDreadBonusPerStack);
        float missingHealthMultiplier = 1.0F + missingHealth
                * Math.max(0.0F, Config.uniqueEffects.watcher.claymoreMissingHealthBonus);
        float vitalityBefore = target.getHealth() + target.getAbsorptionAmount();
        boolean damaged = damageTarget(world, actor, omen.stack, target,
                baseDamage * dreadMultiplier * missingHealthMultiplier);

        boolean executed = false;
        int maxDread = Math.max(1, Config.uniqueEffects.watcher.maxDread);
        float threshold = MathHelper.clamp(Config.uniqueEffects.watcher.omenInstantKillThreshold, 0.0F, 1.0F)
                * target.getMaxHealth();
        if (damaged && omen.dread >= maxDread && (!target.isAlive() || target.getHealth() <= threshold)) {
            executed = true;
            if (target.isAlive()) {
                float lethalDamage = Math.max(1000.0F,
                        target.getMaxHealth() * 10.0F + target.getAbsorptionAmount());
                damageTarget(world, actor, omen.stack, target, lethalDamage);
            }
        }

        if (executed) {
            float removed = Math.max(0.0F,
                    vitalityBefore - (target.getHealth() + target.getAbsorptionAmount()));
            float cap = Math.min(Math.max(0.0F, Config.uniqueEffects.watcher.omenAbsorptionCap),
                    Math.max(0.0F, Config.uniqueEffects.abilityAbsorptionCap));
            if (actor.getAbsorptionAmount() < cap) {
                actor.setAbsorptionAmount(Math.min(cap, actor.getAbsorptionAmount() + removed));
            }
        }

        if (!target.isAlive()) {
            resetClaymoreCooldown(actor, omen.stack);
        }
        spawnFinalOmenEffects(world, actor, target, executed);
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                        LivingEntity target, float baseDamage) {
        return damageTarget(world, stack, target,
                actor.getDamageSources().indirectMagic(actor, actor), baseDamage);
    }

    private static boolean damageTargetWithoutKnockback(ServerWorld world, LivingEntity actor,
                                                        ItemStack stack, LivingEntity target,
                                                        float baseDamage) {
        DamageSource magic = actor.getDamageSources().magic();
        DamageSource attributedMagic =
                new DamageSource(magic.getTypeRegistryEntry(), actor, actor);
        return damageTarget(world, stack, target, attributedMagic, baseDamage);
    }

    private static boolean damageTarget(ServerWorld world, ItemStack stack, LivingEntity target,
                                        DamageSource source, float baseDamage) {
        if (baseDamage <= 0.0F || !target.isAlive()) {
            return false;
        }
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world, stack, target, source, baseDamage);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage)
        );
        return damaged[0];
    }

    private static void resetClaymoreCooldown(LivingEntity actor, ItemStack stack) {
        if (actor instanceof ServerPlayerEntity player) {
            player.getItemCooldownManager().set(stack.getItem(), 0);
        } else {
            WeaponAbilityCooldownManager.clearCooldown(actor, stack);
        }
    }

    private static LivingEntity resolveActivationTarget(WeaponAbilityContext context, WatcherWeaponType type) {
        double range = Math.max(1.0, Config.uniqueEffects.watcher.activationRange);
        LivingEntity preferred = context.target();
        if (preferred != null
                && preferred.squaredDistanceTo(context.actor()) <= range * range
                && hasMark(context.world(), context.actor(), preferred, type)
                && isValidTarget(context.world(), context.actor(), sourcePlayerId(context), preferred)) {
            return preferred;
        }

        if (context.activationSource() != WeaponAbilityActivationSource.PLAYER
                || !(context.actor() instanceof ServerPlayerEntity)) {
            return null;
        }

        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(context.world());
        if (marks == null) {
            return null;
        }
        return marks.values().stream()
                .filter(mark -> mark.key.ownerId.equals(context.actor().getUuid())
                        && mark.key.type == type)
                .map(mark -> getLivingEntity(context.world(), mark.key.targetId))
                .filter(target -> target != null && target.isAlive()
                        && target.squaredDistanceTo(context.actor()) <= range * range
                        && isValidTarget(context.world(), context.actor(), sourcePlayerId(context), target))
                .min(Comparator.comparingDouble(target -> target.squaredDistanceTo(context.actor())))
                .orElse(null);
    }

    private static boolean hasMark(ServerWorld world, LivingEntity actor, LivingEntity target, WatcherWeaponType type) {
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        return marks != null && marks.containsKey(new MarkKey(actor.getUuid(), target.getUuid(), type));
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor, UUID sourcePlayerId,
                                         LivingEntity target) {
        if (target == null || !target.isAlive() || target == actor || target.getWorld() != world
                || !EntityPredicates.VALID_LIVING_ENTITY.test(target)
                || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        LivingEntity sourcePlayer = getLivingEntity(world, sourcePlayerId);
        return sourcePlayer == null || target != sourcePlayer
                && HelperMethods.checkAbilityTarget(target, sourcePlayer);
    }

    private static void evictOldestMarkIfNeeded(ServerWorld world, Map<MarkKey, DreadMark> marks,
                                                 UUID ownerId, WatcherWeaponType type) {
        int limit = Math.max(1, Config.uniqueEffects.watcher.maxMarkedTargets);
        List<DreadMark> owned = marks.values().stream()
                .filter(mark -> mark.key.ownerId.equals(ownerId) && mark.key.type == type)
                .sorted(Comparator.comparingLong(mark -> mark.createdTick))
                .toList();
        if (owned.size() < limit) {
            return;
        }
        DreadMark oldest = owned.get(0);
        marks.remove(oldest.key);
        discardBats(world, oldest.batIds);
    }

    private static WatcherBatEntity spawnBat(ServerWorld world, LivingEntity owner, LivingEntity target,
                                             WatcherWeaponType weaponType, int mode, Vec3d pos) {
        WatcherBatEntity bat = new WatcherBatEntity(world, pos.x, pos.y, pos.z);
        bat.configureWatcher(owner, target, weaponType, mode);
        if (!world.spawnEntity(bat)) {
            return null;
        }
        MANAGED_BATS.computeIfAbsent(world, ignored -> new HashSet<>()).add(bat.getUuid());
        return bat;
    }

    private static void moveBatToward(WatcherBatEntity bat, Vec3d destination, double speed) {
        Vec3d delta = destination.subtract(bat.getPos());
        double distance = delta.length();
        if (distance < 1.0E-4) {
            bat.setVelocity(Vec3d.ZERO);
            return;
        }
        double appliedSpeed = Math.min(Math.max(0.05, speed), distance);
        Vec3d velocity = delta.multiply(appliedSpeed / distance);
        bat.setVelocity(velocity);
        bat.velocityModified = true;
        bat.setYaw((float) (Math.atan2(velocity.x, velocity.z) * (180.0 / Math.PI)));
    }

    private static void discardHunt(ServerWorld world, ActiveHunt hunt) {
        for (HuntBat order : hunt.bats) {
            Entity entity = world.getEntity(order.batId);
            if (entity instanceof WatcherBatEntity bat) {
                discardBat(world, bat);
            } else {
                unregisterBat(world, order.batId);
            }
        }
    }

    private static void discardBats(ServerWorld world, List<UUID> batIds) {
        for (UUID batId : batIds) {
            Entity entity = world.getEntity(batId);
            if (entity instanceof WatcherBatEntity bat) {
                discardBat(world, bat);
            } else {
                unregisterBat(world, batId);
            }
        }
    }

    private static void discardBat(ServerWorld world, WatcherBatEntity bat) {
        UUID id = bat.getUuid();
        bat.discard();
        unregisterBat(world, id);
    }

    private static void unregisterBat(ServerWorld world, UUID batId) {
        Set<UUID> managed = MANAGED_BATS.get(world);
        if (managed == null) {
            return;
        }
        managed.remove(batId);
        if (managed.isEmpty()) {
            MANAGED_BATS.remove(world);
        }
    }

    private static void purgeOrphanBats(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof WatcherBatEntity bat && !isManagedBat(world, bat.getUuid())) {
                bat.discard();
            }
        }
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID uuid) {
        if (world == null || uuid == null) {
            return null;
        }
        Entity entity = world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static UUID sourcePlayerId(WeaponAbilityContext context) {
        return context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid();
    }

    private static void spawnDreadGainEffects(ServerWorld world, LivingEntity target, int dread, int maxDread) {
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.62, 0.0);
        int count = 3 + Math.min(7, dread);
        world.spawnParticles(WATCHER_DUST, pos.x, pos.y, pos.z,
                count, 0.2, 0.28, 0.2, 0.015);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BAT_TAKEOFF,
                target.getSoundCategory(), 0.18F,
                dread >= maxDread ? 0.55F : 0.9F + dread * 0.08F);
    }

    private static void spawnBatAura(ServerWorld world, Vec3d pos) {
        world.spawnParticles(WATCHER_DUST, pos.x, pos.y, pos.z,
                1, 0.025, 0.025, 0.025, 0.002);
    }

    private static void spawnReturnEffects(ServerWorld world, LivingEntity actor, WatcherBatEntity bat) {
        Vec3d pos = bat.getPos();
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z,
                3, 0.12, 0.12, 0.12, 0.02);
        if ((world.getTime() + bat.getId()) % 2L == 0L) {
            world.playSound(null, actor.getX(), actor.getY(), actor.getZ(),
                    SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 0.15F, 1.55F);
        }
    }

    private static void spawnFinalOmenEffects(ServerWorld world, LivingEntity actor,
                                               LivingEntity target, boolean executed) {
        Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        Vec3d forward = target.getPos().subtract(actor.getPos()).multiply(1.0, 0.0, 1.0);
        if (forward.lengthSquared() < 1.0E-4) {
            forward = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        forward = forward.normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);

        for (int i = 0; i <= 28; i++) {
            double angle = -Math.PI * 0.5 + Math.PI * i / 28.0;
            double horizontal = Math.cos(angle) * 2.15;
            double vertical = 0.15 + (Math.sin(angle) + 1.0) * 1.65;
            Vec3d point = target.getPos().add(right.multiply(horizontal)).add(0.0, vertical, 0.0);
            world.spawnParticles(WATCHER_DUST, point.x, point.y, point.z,
                    2, 0.035, 0.035, 0.035, 0.005);
        }
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2.0 * i / 32.0;
            Vec3d point = target.getPos().add(Math.cos(angle) * 2.2, 0.12, Math.sin(angle) * 2.2);
            world.spawnParticles(WATCHER_DUST, point.x, point.y, point.z,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
        world.spawnParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z,
                executed ? 26 : 14, 0.55, 0.7, 0.55, 0.06);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                executed ? 16 : 8, 0.4, 0.5, 0.4, 0.025);
        world.playSound(null, center.x, center.y, center.z,
                executed ? SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get()
                        : SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                SoundCategory.PLAYERS, executed ? 1.0F : 0.8F, executed ? 0.55F : 0.78F);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.35F, 1.45F);
    }

    private static boolean hasEntries(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    private static boolean hasEntries(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private record MarkKey(UUID ownerId, UUID targetId, WatcherWeaponType type) {
    }

    private static final class DreadMark {
        private final MarkKey key;
        private final long createdTick;
        private final double angleOffset;
        private final List<UUID> batIds = new ArrayList<>();
        private int dread;
        private long expiryTick;

        private DreadMark(MarkKey key, long createdTick) {
            this.key = key;
            this.createdTick = createdTick;
            this.angleOffset = ((key.ownerId.hashCode() * 31L + key.targetId.hashCode()) & 1023L)
                    / 1023.0 * Math.PI * 2.0;
        }
    }

    private static final class ActiveHunt {
        private final UUID actorId;
        private final UUID sourcePlayerId;
        private final ItemStack stack;
        private final long startedTick;
        private final long expiryTick;
        private final float baseDamage;
        private final List<HuntBat> bats;
        private float healed;

        private ActiveHunt(UUID actorId, UUID sourcePlayerId, ItemStack stack, long startedTick,
                           long expiryTick, float baseDamage, List<HuntBat> bats) {
            this.actorId = actorId;
            this.sourcePlayerId = sourcePlayerId;
            this.stack = stack;
            this.startedTick = startedTick;
            this.expiryTick = expiryTick;
            this.baseDamage = baseDamage;
            this.bats = bats;
        }
    }

    private static final class HuntBat {
        private final UUID batId;
        private final UUID targetId;
        private final Vec3d stagePos;
        private final long readyTick;
        private boolean returning;

        private HuntBat(UUID batId, UUID targetId, Vec3d stagePos, long readyTick) {
            this.batId = batId;
            this.targetId = targetId;
            this.stagePos = stagePos;
            this.readyTick = readyTick;
        }
    }

    private static final class ActiveOmen {
        private final UUID actorId;
        private final UUID sourcePlayerId;
        private final UUID targetId;
        private final ItemStack stack;
        private final Hand hand;
        private final int dread;
        private final long startedTick;
        private final long impactTick;
        private final int durationTicks;
        private final int totalSwoops;
        private final float swoopDamage;
        private final List<UUID> batIds;
        private final List<UUID> waitingBatIds;
        private final List<OmenSwoop> activeSwoops = new ArrayList<>();
        private int spawnedSwoops;

        private ActiveOmen(UUID actorId, UUID sourcePlayerId, UUID targetId, ItemStack stack,
                           Hand hand, int dread, long startedTick, long impactTick, int durationTicks,
                           int totalSwoops, float swoopDamage, List<UUID> batIds,
                           List<UUID> waitingBatIds) {
            this.actorId = actorId;
            this.sourcePlayerId = sourcePlayerId;
            this.targetId = targetId;
            this.stack = stack;
            this.hand = hand;
            this.dread = dread;
            this.startedTick = startedTick;
            this.impactTick = impactTick;
            this.durationTicks = durationTicks;
            this.totalSwoops = totalSwoops;
            this.swoopDamage = swoopDamage;
            this.batIds = batIds;
            this.waitingBatIds = waitingBatIds;
        }
    }

    private static final class OmenSwoop {
        private final UUID batId;
        private final Vec3d exitPoint;
        private final long expiryTick;
        private boolean hit;

        private OmenSwoop(UUID batId, Vec3d exitPoint, long expiryTick) {
            this.batId = batId;
            this.exitPoint = exitPoint;
            this.expiryTick = expiryTick;
        }
    }
}
