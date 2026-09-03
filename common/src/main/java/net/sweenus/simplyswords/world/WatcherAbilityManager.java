package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.MartialCommandEldritchMasteryTuning;
import net.sweenus.simplyswords.api.ability.MartialCommandEldritchMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
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
    private static final Map<ServerWorld, Map<UUID, OwnerState>> OWNER_STATES = new HashMap<>();
    private static final int MAX_OWNER_STATES = 256;
    private static final int OWNER_STATE_TICKS = 400;
    private static final TagKey<EntityType<?>> EXECUTION_IMMUNE = TagKey.of(RegistryKeys.ENTITY_TYPE,
            Identifier.of(SimplySwords.MOD_ID, "execution_immune"));

    private WatcherAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        return hasEntries(ACTIVE_MARKS.get(world))
                || hasEntries(ACTIVE_HUNTS.get(world))
                || hasEntries(ACTIVE_OMENS.get(world))
                || hasEntries(OWNER_STATES.get(world))
                || world.getTime() % 100L == 0L;
    }

    public static boolean isManagedBat(ServerWorld world, UUID batId) {
        Set<UUID> bats = MANAGED_BATS.get(world);
        return bats != null && bats.contains(batId);
    }

    public static void addDread(ServerWorld world, LivingEntity actor, LivingEntity target, WatcherWeaponType type) {
        addDread(world, actor, target, type, ItemStack.EMPTY);
    }

    public static void addDread(ServerWorld world, LivingEntity actor, LivingEntity target, WatcherWeaponType type,
                                ItemStack stack) {
        if (world == null || actor == null || target == null || type == null
                || !actor.isAlive() || !isValidTarget(world, actor, null, target)) {
            return;
        }

        UniqueAbilityExecution execution = null;
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryTuning.EMPTY;
        UniqueAbilityExecution wargExecution = null;
        MartialCommandEldritchMasteryTuning warg = MartialCommandEldritchMasteryTuning.EMPTY;
        if (type == WatcherWeaponType.CLAYMORE && stack != null && !stack.isEmpty()) {
            execution = UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.WATCHER_DREAD,
                    UniqueAbilityContext.passive(world, stack, actor, target, null), builder -> builder
                            .set(AbyssalSpectralMasteryAbilities.TUNING, AbyssalSpectralMasteryTuning.EMPTY
                                    .with(AbyssalSpectralMasteryTuning.Setting.STACK_DURATION_TICKS,
                                            Config.uniqueEffects.watcher.dreadDuration)
                                    .with(AbyssalSpectralMasteryTuning.Setting.STACK_CAP,
                                            Config.uniqueEffects.watcher.maxDread)
                                    .with(AbyssalSpectralMasteryTuning.Setting.MARKED_TARGET_CAP,
                                            Config.uniqueEffects.watcher.maxMarkedTargets)));
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        } else if (type == WatcherWeaponType.WARGLAIVE && stack != null && !stack.isEmpty()) {
            wargExecution = MartialCommandEldritchMasteryCombatManager.beginPassive(
                    MartialCommandEldritchMasteryAbilities.WARG_MARK, world, stack, actor, target, warglaiveMarkBase());
            warg = MartialCommandEldritchMasteryAbilities.tuning(wargExecution);
        }

        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.computeIfAbsent(world, ignored -> new HashMap<>());
        MarkKey key = new MarkKey(actor.getUuid(), target.getUuid(), type);
        long now = world.getTime();
        int mode = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0);
        int wargMode = warg.integer(MartialCommandEldritchMasteryTuning.Setting.MODE, 0);
        if ((wargMode & (1 << 7)) != 0) {
            Iterator<DreadMark> iterator = marks.values().iterator();
            while (iterator.hasNext()) {
                DreadMark existing = iterator.next();
                if (existing.key.ownerId.equals(actor.getUuid()) && existing.key.type == type
                        && !existing.key.targetId.equals(target.getUuid())) {
                    discardBats(world, existing.batIds);
                    iterator.remove();
                }
            }
        }
        if ((mode & 16) != 0) {
            Iterator<DreadMark> iterator = marks.values().iterator();
            while (iterator.hasNext()) {
                DreadMark existing = iterator.next();
                if (existing.key.ownerId.equals(actor.getUuid()) && existing.key.type == type
                        && !existing.key.targetId.equals(target.getUuid())) {
                    discardBats(world, existing.batIds);
                    iterator.remove();
                }
            }
        }
        int maxDread = type == WatcherWeaponType.WARGLAIVE
                ? Math.max(1, warg.integer(MartialCommandEldritchMasteryTuning.Setting.STACK_CAP,
                Config.uniqueEffects.watcher.maxDread))
                : Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_CAP,
                Config.uniqueEffects.watcher.maxDread));
        DreadMark mark = marks.get(key);
        if (mark == null) {
            int targetCap = type == WatcherWeaponType.WARGLAIVE
                    ? Math.max(1, warg.integer(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP,
                    Config.uniqueEffects.watcher.maxMarkedTargets))
                    : Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.MARKED_TARGET_CAP,
                    Config.uniqueEffects.watcher.maxMarkedTargets));
            evictOldestMarkIfNeeded(world, marks, actor.getUuid(), type, targetCap);
            mark = new DreadMark(key, now);
            marks.put(key, mark);
        }

        if (type == WatcherWeaponType.WARGLAIVE) {
            mark.maxDread = maxDread;
            mark.legacyRadius = warg.flag(1 << 5) ? warg.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_RADIUS, 6) : 0;
        }
        if (type == WatcherWeaponType.CLAYMORE) recordOwnerState(world, actor, tuning);

        double bonusPerStack = tuning.get(AbyssalSpectralMasteryTuning.Setting.MELEE_BONUS_PER_STACK, 0);
        double bonusCap = tuning.get(AbyssalSpectralMasteryTuning.Setting.MELEE_BONUS_CAP, 0);
        if (mark.dread > 0 && bonusPerStack > 0 && bonusCap > 0 && stack != null && !stack.isEmpty()) {
            float weaponDamage = (float) HelperMethods.getAttackFromStack(stack,
                    net.minecraft.component.type.AttributeModifierSlot.MAINHAND);
            damageTarget(world, actor, stack, target,
                    weaponDamage * (float) Math.min(bonusCap, mark.dread * bonusPerStack));
        }
        if (type == WatcherWeaponType.CLAYMORE) applyClaimedVitality(world, actor, target, stack, tuning);
        mark.expiryTick = now + (type == WatcherWeaponType.WARGLAIVE
                ? Math.max(1, warg.integer(MartialCommandEldritchMasteryTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.watcher.dreadDuration))
                : Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_DURATION_TICKS,
                Config.uniqueEffects.watcher.dreadDuration)));
        int dreadGain = 1;
        mark.hitCount++;
        if (type == WatcherWeaponType.WARGLAIVE && warg.flag(1 << 2)
                && mark.hitCount % Math.max(1, warg.integer(MartialCommandEldritchMasteryTuning.Setting.COUNT, 3)) == 0) dreadGain++;
        if (type == WatcherWeaponType.WARGLAIVE && warg.flag(1 << 7)) dreadGain = 2;
        if ((mode & 2) != 0 && now - mark.lastHitTick <= tuning.integer(
                AbyssalSpectralMasteryTuning.Setting.REPEAT_WINDOW_TICKS, 30) && now >= mark.unblinkingReadyTick) {
            dreadGain = 2;
            mark.unblinkingReadyTick = now + tuning.integer(
                    AbyssalSpectralMasteryTuning.Setting.REPEAT_LOCKOUT_TICKS, 80);
        }
        mark.lastHitTick = now;
        int previousDread = mark.dread;
        mark.dread = Math.min(maxDread, mark.dread + dreadGain);
        for (int added = previousDread; added < mark.dread; added++) {
            WatcherBatEntity bat = spawnBat(world, actor, target, type, WatcherBatEntity.MODE_MARK,
                    target.getPos().add(0.0, target.getHeight() * 0.7, 0.0));
            if (bat != null) {
                mark.batIds.add(bat.getUuid());
            }
        }
        if (mark.dread > previousDread) spawnDreadGainEffects(world, target, mark.dread, maxDread);
        if (type == WatcherWeaponType.WARGLAIVE && warg.flag(1 << 4))
            revealMarks(world, actor, marks, type, warg.get(MartialCommandEldritchMasteryTuning.Setting.SEARCH_RANGE, 16),
                    warg.integer(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 20));
        if (type == WatcherWeaponType.WARGLAIVE && warg.flag(1 << 6) && mark.dread >= maxDread)
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    warg.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_DURATION_TICKS, 40),
                    warg.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER, 0)), actor);
        if (type == WatcherWeaponType.WARGLAIVE && warg.flag(1 << 3) && mark.dread >= maxDread
                && now >= mark.sharedTerrorReadyTick) {
            spreadDread(world, actor, target, marks, type, warg.get(MartialCommandEldritchMasteryTuning.Setting.RADIUS, 3),
                    warg.integer(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, Config.uniqueEffects.watcher.maxMarkedTargets));
            mark.sharedTerrorReadyTick = now + warg.integer(MartialCommandEldritchMasteryTuning.Setting.LOCKOUT_TICKS, 20);
        }
        if ((mode & 1) != 0 && mark.dread >= tuning.integer(AbyssalSpectralMasteryTuning.Setting.THRESHOLD, 4)
                && now >= mark.sharedTerrorReadyTick) {
            double radius = tuning.get(AbyssalSpectralMasteryTuning.Setting.SCAN_RADIUS, 4);
            LivingEntity shared = world.getEntitiesByClass(LivingEntity.class,
                            new Box(target.getPos(), target.getPos()).expand(radius), candidate -> candidate != actor
                                    && candidate != target && candidate.isAlive()
                                    && candidate.squaredDistanceTo(target) <= radius * radius
                                    && isValidTarget(world, actor, null, candidate)
                                    && !marks.containsKey(new MarkKey(actor.getUuid(), candidate.getUuid(), type)))
                    .stream().min(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(target)))
                    .orElse(null);
            long ownedMarks = marks.values().stream().filter(existing -> existing.key.ownerId.equals(actor.getUuid())
                    && existing.key.type == type).count();
            int markedCap = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MARKED_TARGET_CAP,
                    Config.uniqueEffects.watcher.maxMarkedTargets);
            if (shared != null && ownedMarks < markedCap) {
                MarkKey sharedKey = new MarkKey(actor.getUuid(), shared.getUuid(), type);
                DreadMark sharedMark = new DreadMark(sharedKey, now);
                sharedMark.dread = 1;
                sharedMark.expiryTick = now + 160;
                WatcherBatEntity bat = spawnBat(world, actor, shared, type, WatcherBatEntity.MODE_MARK,
                        shared.getPos().add(0, shared.getHeight() * .7, 0));
                if (bat != null) sharedMark.batIds.add(bat.getUuid());
                marks.put(sharedKey, sharedMark);
                mark.sharedTerrorReadyTick = now + tuning.integer(AbyssalSpectralMasteryTuning.Setting.LOCKOUT_TICKS, 40);
            }
        }
        if (execution != null) {
            UniqueAbilityApi.emit(execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    AbyssalSpectralMasteryAbilities.HIT, target, 1, mark.dread);
            UniqueAbilityApi.finish(execution, execution.definition().id(), 1);
        }
        if (wargExecution != null) MartialCommandEldritchMasteryCombatManager.finish(wargExecution, 1);
    }

    private static void spreadDread(ServerWorld world, LivingEntity actor, LivingEntity target,
                                    Map<MarkKey, DreadMark> marks, WatcherWeaponType type,
                                    double radius, int targetCap) {
        LivingEntity shared = world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius),
                        candidate -> candidate != target && isValidTarget(world, actor, null, candidate))
                .stream().filter(candidate -> !marks.containsKey(new MarkKey(actor.getUuid(), candidate.getUuid(), type)))
                .min(Comparator.comparingDouble(target::squaredDistanceTo)).orElse(null);
        long count = marks.values().stream().filter(mark -> mark.key.ownerId.equals(actor.getUuid())
                && mark.key.type == type).count();
        if (shared == null || count >= targetCap) return;
        DreadMark sharedMark = new DreadMark(new MarkKey(actor.getUuid(), shared.getUuid(), type), world.getTime());
        sharedMark.dread = 1;
        sharedMark.expiryTick = world.getTime() + Config.uniqueEffects.watcher.dreadDuration;
        WatcherBatEntity bat = spawnBat(world, actor, shared, type, WatcherBatEntity.MODE_MARK,
                shared.getPos().add(0, shared.getHeight() * .7, 0));
        if (bat != null) sharedMark.batIds.add(bat.getUuid());
        marks.put(sharedMark.key, sharedMark);
    }

    private static void inheritDread(ServerWorld world, LivingEntity actor, LivingEntity target,
                                     Map<MarkKey, DreadMark> marks, DreadMark source) {
        double radius = source.legacyRadius;
        LivingEntity heir = world.getEntitiesByClass(LivingEntity.class,
                        target.getBoundingBox().expand(radius),
                        candidate -> candidate != target && isValidTarget(world, actor, null, candidate))
                .stream().min(Comparator.comparingDouble(target::squaredDistanceTo)).orElse(null);
        if (heir == null) return;
        MarkKey key = new MarkKey(actor.getUuid(), heir.getUuid(), source.key.type);
        DreadMark mark = marks.get(key);
        if (mark == null) {
            mark = new DreadMark(key, world.getTime());
            mark.expiryTick = source.expiryTick;
            mark.legacyRadius = source.legacyRadius;
            marks.put(key, mark);
        }
        mark.dread = Math.min(source.maxDread, mark.dread + Math.max(1, source.dread / 2));
    }

    private static void revealMarks(ServerWorld world, LivingEntity actor, Map<MarkKey, DreadMark> marks,
                                    WatcherWeaponType type, double range, int duration) {
        double squared = range * range;
        for (DreadMark mark : marks.values()) {
            if (!mark.key.ownerId.equals(actor.getUuid()) || mark.key.type != type) continue;
            LivingEntity marked = getLivingEntity(world, mark.key.targetId);
            if (marked == null || !marked.isAlive() || marked.squaredDistanceTo(actor) > squared) continue;
            marked.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,
                    Math.max(1, duration), 0, false, false, true), actor);
        }
    }

    public static MartialCommandEldritchMasteryTuning warglaiveMarkBase(int dreadDuration, int maxDread,
                                                        int maxMarkedTargets, double activationRange) {
        return MartialCommandEldritchMasteryTuning.EMPTY
                .with(MartialCommandEldritchMasteryTuning.Setting.DURATION_TICKS, dreadDuration)
                .with(MartialCommandEldritchMasteryTuning.Setting.STACK_CAP, maxDread)
                .with(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, maxMarkedTargets)
                .with(MartialCommandEldritchMasteryTuning.Setting.RANGE, activationRange)
                .with(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static MartialCommandEldritchMasteryTuning warglaiveHuntBase(double huntRadius, int maxTargets) {
        return MartialCommandEldritchMasteryTuning.EMPTY
                .with(MartialCommandEldritchMasteryTuning.Setting.RADIUS, huntRadius)
                .with(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP, maxTargets)
                .with(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1)
                .with(MartialCommandEldritchMasteryTuning.Setting.COUNT, 1)
                .with(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static MartialCommandEldritchMasteryTuning warglaiveSanguineBase(double healCap) {
        return MartialCommandEldritchMasteryTuning.EMPTY
                .with(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                .with(MartialCommandEldritchMasteryTuning.Setting.HEAL_MULTIPLIER, 1)
                .with(MartialCommandEldritchMasteryTuning.Setting.HEALTH_THRESHOLD, healCap);
    }

    private static MartialCommandEldritchMasteryTuning warglaiveMarkBase() {
        return warglaiveMarkBase(Config.uniqueEffects.watcher.dreadDuration,
                Config.uniqueEffects.watcher.maxDread, Config.uniqueEffects.watcher.maxMarkedTargets,
                Config.uniqueEffects.watcher.activationRange);
    }

    private static MartialCommandEldritchMasteryTuning warglaiveHuntBase() {
        return warglaiveHuntBase(Config.uniqueEffects.watcher.warglaiveHuntRadius,
                Config.uniqueEffects.watcher.warglaiveMaxTargets);
    }

    private static MartialCommandEldritchMasteryTuning warglaiveSanguineBase() {
        return warglaiveSanguineBase(Config.uniqueEffects.watcher.warglaiveHealCap);
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
            pruneOwnerStates(world);
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
                if (actor != null && actor.isAlive() && target != null && !target.isAlive()
                        && mark.key.type == WatcherWeaponType.WARGLAIVE && mark.legacyRadius > 0
                        && mark.dread > 1) {
                    inheritDread(world, actor, target, marks, mark);
                }
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
        UniqueAbilityExecution execution = MartialCommandEldritchMasteryCombatManager.beginActive(
                MartialCommandEldritchMasteryAbilities.WARG_HUNT, context, Config.uniqueEffects.watcher.warglaiveCooldown,
                warglaiveHuntBase());
        MartialCommandEldritchMasteryTuning tuning = MartialCommandEldritchMasteryAbilities.tuning(execution);
        UniqueAbilityExecution markExecution = MartialCommandEldritchMasteryCombatManager.beginPassive(
                MartialCommandEldritchMasteryAbilities.WARG_MARK, world, context.stack(), context.actor(), primaryTarget,
                warglaiveMarkBase());
        MartialCommandEldritchMasteryTuning markTuning = MartialCommandEldritchMasteryAbilities.tuning(markExecution);
        MartialCommandEldritchMasteryCombatManager.finish(markExecution, 0);
        UniqueAbilityExecution sanguineExecution = MartialCommandEldritchMasteryCombatManager.beginPassive(
                MartialCommandEldritchMasteryAbilities.WARG_SANGUINE, world, context.stack(), context.actor(), primaryTarget,
                warglaiveSanguineBase());
        MartialCommandEldritchMasteryTuning sanguine = MartialCommandEldritchMasteryAbilities.tuning(sanguineExecution);
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            MartialCommandEldritchMasteryCombatManager.finish(sanguineExecution, 0);
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        double radius = Math.max(1.0, tuning.get(MartialCommandEldritchMasteryTuning.Setting.RADIUS,
                Config.uniqueEffects.watcher.warglaiveHuntRadius));
        int maxTargets = Math.max(1, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.TARGET_CAP,
                Config.uniqueEffects.watcher.warglaiveMaxTargets));
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
            MartialCommandEldritchMasteryCombatManager.finish(sanguineExecution, 0);
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        int dreadConsumed = selected.stream().mapToInt(mark -> mark.dread).sum();
        List<UUID> batIds = new ArrayList<>();
        List<UUID> destinationSlots = new ArrayList<>();
        List<UUID> originSlots = new ArrayList<>();
        int extraBats = Math.max(0, tuning.integer(MartialCommandEldritchMasteryTuning.Setting.COUNT, 1) - 1);
        for (DreadMark mark : selected) {
            marks.remove(mark.key);
            LivingEntity marked = getLivingEntity(world, mark.key.targetId);
            for (UUID batId : mark.batIds) {
                if (world.getEntity(batId) instanceof WatcherBatEntity) {
                    batIds.add(batId);
                    originSlots.add(mark.key.targetId);
                }
            }
            for (int extra = 0; extra < extraBats * mark.dread && marked != null; extra++) {
                WatcherBatEntity bat = spawnBat(world, context.actor(), marked, WatcherWeaponType.WARGLAIVE,
                        WatcherBatEntity.MODE_HUNT, marked.getPos().add(0.0, marked.getHeight() * 0.7, 0.0));
                if (bat == null) break;
                batIds.add(bat.getUuid());
                originSlots.add(mark.key.targetId);
            }
            for (int i = 0; i < mark.dread * (extraBats + 1); i++) {
                destinationSlots.add(mark.key.targetId);
            }
        }
        if (batIds.isEmpty()) {
            MartialCommandEldritchMasteryCombatManager.finish(sanguineExecution, 0);
            UniqueAbilityApi.cancel(execution);
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
        if (tuning.flag(1 << 17) && orders.size() > 1) {
            HuntBat lead = orders.get(0);
            for (int i = 1; i < orders.size(); i++) {
                if (!lead.route.contains(orders.get(i).targetId)) lead.route.add(orders.get(i).targetId);
                if (world.getEntity(orders.get(i).batId) instanceof WatcherBatEntity extra) {
                    discardBat(world, extra);
                } else {
                    unregisterBat(world, orders.get(i).batId);
                }
            }
            while (lead.route.size() > maxTargets - 1) lead.route.remove(lead.route.size() - 1);
            orders = new ArrayList<>(List.of(lead));
        }

        float baseDamage = HelperMethods.abilityScaledDamage(
                "soul",
                context.actor(),
                context.stack(),
                Config.uniqueEffects.watcher.warglaiveDamageScaling,
                Config.uniqueEffects.watcher.warglaiveSpellScaling
        );
        baseDamage *= (float) tuning.get(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        baseDamage *= (float) sanguine.get(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        if (markTuning.flag(1 << 7)) baseDamage *= (float) markTuning.get(MartialCommandEldritchMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        ActiveHunt hunt = new ActiveHunt(
                context.actor().getUuid(),
                sourcePlayerId(context),
                context.stack().copy(),
                now,
                now + HUNT_MAX_TICKS,
                baseDamage,
                orders,
                tuning,
                sanguine,
                dreadConsumed,
                execution,
                sanguineExecution
        );
        ACTIVE_HUNTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(hunt);
        world.playSound(null, context.actor().getX(), context.actor().getY(), context.actor().getZ(),
                SoundRegistry.DARK_SWORD_SPELL.get(), SoundCategory.PLAYERS, 0.7F, 1.45F);
        UniqueAbilityApi.publishStartedExecution(execution);
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
                    moveBatToward(bat, returnPoint, Config.uniqueEffects.watcher.warglaiveBatSpeed
                            * hunt.tuning.get(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1));
                    if (bat.squaredDistanceTo(returnPoint) <= RETURN_REACH_SQUARED) {
                        applyReturningShadow(world, actor, hunt);
                        spawnReturnEffects(world, actor, bat);
                        discardBat(world, bat);
                        batIterator.remove();
                    }
                    continue;
                }

                if (world.getTime() < order.readyTick) {
                    moveBatToward(bat, order.stagePos, Config.uniqueEffects.watcher.warglaiveBatSpeed
                            * hunt.tuning.get(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1));
                    continue;
                }

                LivingEntity target = getLivingEntity(world, order.targetId);
                if (target == null || !target.isAlive()
                        || !isValidTarget(world, actor, hunt.sourcePlayerId, target)) {
                    order.returning = true;
                    continue;
                }

                Vec3d strikePoint = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
                moveBatToward(bat, strikePoint, Config.uniqueEffects.watcher.warglaiveBatSpeed
                        * hunt.tuning.get(MartialCommandEldritchMasteryTuning.Setting.SPEED, 1));
                if (bat.squaredDistanceTo(strikePoint) <= BAT_REACH_SQUARED) {
                    applyHuntStrike(world, actor, target, hunt, order.damageScale);
                    if (!advanceOrder(world, actor, hunt, order, target)) order.returning = true;
                }
            }

            if (hunt.bats.isEmpty()) {
                MartialCommandEldritchMasteryCombatManager.finish(hunt.sanguineExecution, hunt.struckTargets.size());
                MartialCommandEldritchMasteryCombatManager.finish(hunt.execution, hunt.struckTargets.size());
                iterator.remove();
            }
        }

        if (hunts.isEmpty()) {
            ACTIVE_HUNTS.remove(world);
        }
    }

    public static float huntStrikeMultiplier(MartialCommandEldritchMasteryTuning hunt, MartialCommandEldritchMasteryTuning sanguine,
                                            int struckTargets, int dreadConsumed) {
        float distinct = 1 + Math.min(hunt.integer(MartialCommandEldritchMasteryTuning.Setting.STACK_CAP, 5), struckTargets)
                * (float) hunt.get(MartialCommandEldritchMasteryTuning.Setting.PER_STACK_MULTIPLIER, 0);
        return distinct * (1 + Math.min(sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.STACK_CAP, 0), dreadConsumed)
                * (float) sanguine.get(MartialCommandEldritchMasteryTuning.Setting.PER_STACK_MULTIPLIER, 0));
    }

    public static int huntKillRefund(MartialCommandEldritchMasteryTuning sanguine, int alreadyRefunded) {
        if (!sanguine.flag(1 << 24)) return 0;
        int cap = Math.max(0, sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.LOCKOUT_TICKS, 60));
        return Math.max(0, Math.min(sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.REFUND_TICKS, 15),
                cap - alreadyRefunded));
    }

    private static void applyHuntStrike(ServerWorld world, LivingEntity actor, LivingEntity target,
                                        ActiveHunt hunt, float scale) {
        float distinctMultiplier = huntStrikeMultiplier(hunt.tuning, hunt.sanguine,
                hunt.struckTargets.size(), hunt.dreadConsumed);
        float beforeVitality = target.getHealth() + target.getAbsorptionAmount();
        if (damageTarget(world, actor, hunt.stack, target, hunt.baseDamage * distinctMultiplier * scale)) {
            if (hunt.struckTargets.add(target.getUuid())) hunt.struckOrder.add(target.getUuid());
            if (hunt.sanguine.flag(1 << 26))
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,
                        hunt.sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_DURATION_TICKS, 50),
                        hunt.sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER, 1)), actor);
            else if (hunt.tuning.has(MartialCommandEldritchMasteryTuning.Setting.STATUS_DURATION_TICKS))
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        hunt.tuning.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_DURATION_TICKS, 20),
                        hunt.tuning.integer(MartialCommandEldritchMasteryTuning.Setting.STATUS_AMPLIFIER, 1)), actor);
            float afterVitality = target.getHealth() + target.getAbsorptionAmount();
            float removed = Math.max(0.0F, beforeVitality - afterVitality);
            float healCap = actor.getMaxHealth() * MathHelper.clamp(
                    (float) hunt.sanguine.get(MartialCommandEldritchMasteryTuning.Setting.HEALTH_THRESHOLD,
                            Config.uniqueEffects.watcher.warglaiveHealCap), 0.0F, 1.0F);
            float remainingCap = Math.max(0.0F, healCap - hunt.healed);
            float heal = Math.min(remainingCap,
                    removed * Math.max(0.0F, Config.uniqueEffects.watcher.warglaiveLifeSteal)
                            * (float) hunt.sanguine.get(MartialCommandEldritchMasteryTuning.Setting.HEAL_MULTIPLIER, 1));
            if (heal > 0.0F) {
                actor.heal(heal);
                hunt.healed += heal;
            }
            if (hunt.sanguine.flag(1 << 22) && heal > 0 && actor.getHealth() >= actor.getMaxHealth())
                MasteryAbsorptionTracker.grant(actor, heal, 200,
                        (float) hunt.sanguine.get(MartialCommandEldritchMasteryTuning.Setting.ABSORPTION, 8));
            if (hunt.sanguine.flag(1 << 23)
                    && hunt.struckTargets.size() >= Math.max(1, hunt.sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.COUNT, 3)))
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                        hunt.sanguine.integer(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_DURATION_TICKS, 60), 0), actor);
            if (!target.isAlive()) onHuntKill(hunt, actor, target);
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

    private static void onHuntKill(ActiveHunt hunt, LivingEntity actor, LivingEntity victim) {
        UniqueAbilityApi.emit(hunt.execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                MartialCommandEldritchMasteryAbilities.KILL, victim, 1, 1.0);
        int granted = huntKillRefund(hunt.sanguine, hunt.refunded);
        if (granted <= 0) return;
        hunt.refunded += granted;
        SimplySwordsAPI.reduceWeaponCooldown(actor, hunt.stack,
                Config.uniqueEffects.watcher.warglaiveCooldown, granted);
    }

    private static boolean advanceOrder(ServerWorld world, LivingEntity actor, ActiveHunt hunt,
                                        HuntBat order, LivingEntity struck) {
        if (!order.route.isEmpty()) {
            order.targetId = order.route.remove(0);
            order.readyTick = world.getTime() + HUNT_STAGE_TICKS;
            if (world.getEntity(order.batId) instanceof WatcherBatEntity bat)
                bat.configureWatcher(actor, getLivingEntity(world, order.targetId),
                        WatcherWeaponType.WARGLAIVE, WatcherBatEntity.MODE_HUNT);
            return true;
        }
        if (struck.isAlive() || !hunt.tuning.flag(1 << 12)) return false;
        double radius = hunt.tuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_RADIUS, 8);
        LivingEntity next = world.getEntitiesByClass(LivingEntity.class,
                        struck.getBoundingBox().expand(radius),
                        candidate -> candidate != struck && candidate.isAlive()
                                && !hunt.struckTargets.contains(candidate.getUuid())
                                && isValidTarget(world, actor, hunt.sourcePlayerId, candidate))
                .stream().min(Comparator.comparingDouble(struck::squaredDistanceTo)).orElse(null);
        if (next == null) return false;
        order.targetId = next.getUuid();
        order.readyTick = world.getTime() + HUNT_STAGE_TICKS;
        order.damageScale = (float) hunt.tuning.get(MartialCommandEldritchMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, .6);
        if (world.getEntity(order.batId) instanceof WatcherBatEntity bat)
            bat.configureWatcher(actor, next, WatcherWeaponType.WARGLAIVE, WatcherBatEntity.MODE_HUNT);
        return true;
    }

    private static void applyReturningShadow(ServerWorld world, LivingEntity actor, ActiveHunt hunt) {
        if (!hunt.tuning.flag(1 << 15) || hunt.bats.size() != 1 || hunt.struckOrder.isEmpty()) return;
        LivingEntity first = getLivingEntity(world, hunt.struckOrder.get(0));
        if (first == null || !first.isAlive()
                || !isValidTarget(world, actor, hunt.sourcePlayerId, first)) return;
        damageTarget(world, actor, hunt.stack, first, hunt.baseDamage
                * (float) hunt.tuning.get(MartialCommandEldritchMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 0.4));
    }

    private static boolean startFinalOmen(WeaponAbilityContext context, LivingEntity target) {
        UniqueAbilityExecution[] primary = new UniqueAbilityExecution[1];
        if (!startOmenOn(context, target, primary)) return false;
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(primary[0]);
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 8) == 0) return true;
        int cap = Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.SECONDARY_TARGET_CAP, 3));
        for (LivingEntity extra : additionalOmenTargets(context, target, cap - 1,
                tuning.get(AbyssalSpectralMasteryTuning.Setting.IMPACT_RADIUS, 12))) {
            UniqueAbilityExecution[] secondary = new UniqueAbilityExecution[1];
            if (startOmenOn(context, extra, secondary)) {
                UniqueAbilityApi.takeStartedExecution();
                UniqueAbilityApi.start(secondary[0]);
            }
        }
        UniqueAbilityApi.publishStartedExecution(primary[0]);
        return true;
    }

    private static List<LivingEntity> additionalOmenTargets(WeaponAbilityContext context, LivingEntity primary,
                                                            int limit, double radius) {
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(context.world());
        if (marks == null || limit <= 0 || radius <= 0) return List.of();
        return marks.values().stream()
                .filter(mark -> mark.key.ownerId.equals(context.actor().getUuid())
                        && mark.key.type == WatcherWeaponType.CLAYMORE && mark.dread > 0)
                .map(mark -> getLivingEntity(context.world(), mark.key.targetId))
                .filter(candidate -> candidate != null && candidate != primary && candidate.isAlive()
                        && candidate.squaredDistanceTo(context.actor()) <= radius * radius
                        && isValidTarget(context.world(), context.actor(), sourcePlayerId(context), candidate))
                .sorted(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(context.actor())))
                .limit(limit)
                .toList();
    }

    private static boolean startOmenOn(WeaponAbilityContext context, LivingEntity target,
                                       UniqueAbilityExecution[] started) {
        ServerWorld world = context.world();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.WATCHER_OMEN,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.watcher.claymoreCooldown)
                        .set(AbyssalSpectralMasteryAbilities.TUNING, AbyssalSpectralMasteryTuning.EMPTY
                                .with(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS,
                                        Config.uniqueEffects.watcher.claymoreCooldown)
                                .with(AbyssalSpectralMasteryTuning.Setting.DURATION_TICKS,
                                        Config.uniqueEffects.watcher.claymoreSwoopDuration)
                                .with(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS,
                                        Config.uniqueEffects.watcher.claymoreSwoopDuration + 10)
                                .with(AbyssalSpectralMasteryTuning.Setting.SWOOP_COUNT_BASE,
                                        Config.uniqueEffects.watcher.claymoreMinimumSwoops)
                                .with(AbyssalSpectralMasteryTuning.Setting.TARGET_CAP,
                                        Config.uniqueEffects.watcher.claymoreMaximumSwoops)
                                .with(AbyssalSpectralMasteryTuning.Setting.SWOOP_DAMAGE_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.MISSING_HEALTH_BONUS_CAP,
                                        Config.uniqueEffects.watcher.claymoreMissingHealthBonus)
                                .with(AbyssalSpectralMasteryTuning.Setting.EXECUTE_THRESHOLD,
                                        Config.uniqueEffects.watcher.omenInstantKillThreshold)
                                .with(AbyssalSpectralMasteryTuning.Setting.ABSORPTION_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.STACK_CAP,
                                        Config.uniqueEffects.watcher.maxDread)));
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        if (started != null) started[0] = execution;
        MarkKey key = new MarkKey(context.actor().getUuid(), target.getUuid(), WatcherWeaponType.CLAYMORE);
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        DreadMark mark = marks == null ? null : marks.remove(key);
        if (mark == null || mark.dread <= 0) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        for (UUID batId : mark.batIds) {
            Entity entity = world.getEntity(batId);
            if (entity instanceof WatcherBatEntity bat) {
                bat.configureWatcher(context.actor(), target, WatcherWeaponType.CLAYMORE,
                        WatcherBatEntity.MODE_OMEN);
            }
        }

        int duration = Math.max(20, tuning.integer(AbyssalSpectralMasteryTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.watcher.claymoreSwoopDuration));
        int slowDuration = tuning.integer(AbyssalSpectralMasteryTuning.Setting.STATUS_DURATION_TICKS, duration + 10);
        StatusEffectInstance currentSlowness = target.getStatusEffect(StatusEffects.SLOWNESS);
        if (currentSlowness == null || currentSlowness.getAmplifier() < 3
                || currentSlowness.getDuration() < slowDuration) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    slowDuration, 3, false, false, true), context.actor());
        }

        int maxDread = Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_CAP,
                Config.uniqueEffects.watcher.maxDread));
        int minimumSwoops = Math.max(0, tuning.integer(AbyssalSpectralMasteryTuning.Setting.SWOOP_COUNT_BASE,
                Config.uniqueEffects.watcher.claymoreMinimumSwoops));
        int maximumSwoops = Math.max(minimumSwoops, tuning.integer(AbyssalSpectralMasteryTuning.Setting.TARGET_CAP,
                Config.uniqueEffects.watcher.claymoreMaximumSwoops));
        float dreadProgress = maxDread <= 1 ? 1.0F
                : MathHelper.clamp((float) (mark.dread - 1) / (maxDread - 1), 0.0F, 1.0F);
        int totalSwoops = totalSwoops(tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0),
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.SPEAR_COUNT, 0), minimumSwoops, maximumSwoops,
                mark.dread, tuning.integer(AbyssalSpectralMasteryTuning.Setting.SWOOPS_PER_STACK, 0), dreadProgress);
        float swoopDamage = HelperMethods.abilityScaledDamage(
                "soul",
                context.actor(),
                context.stack(),
                Config.uniqueEffects.watcher.claymoreSwoopDamageScaling,
                Config.uniqueEffects.watcher.claymoreSwoopSpellScaling
        ) * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.SWOOP_DAMAGE_MULTIPLIER, 1)
                * manyEyedScale(tuning);
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
                new ArrayList<>(mark.batIds), execution
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
                UniqueAbilityApi.cancel(omen.execution);
                discardBats(world, omen.batIds);
                iterator.remove();
                continue;
            }

            long elapsed = Math.max(0L, world.getTime() - omen.startedTick);
            AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(omen.execution);
            if (!omen.noEscapeTriggered
                    && (tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 32) != 0
                    && actor.squaredDistanceTo(target) > Math.pow(tuning.get(
                    AbyssalSpectralMasteryTuning.Setting.RANGE, 12), 2)) {
                Vec3d pull = actor.getPos().subtract(target.getPos());
                if (pull.lengthSquared() > 1.0E-6) {
                    target.setVelocity(target.getVelocity().add(pull.normalize().multiply(
                            tuning.get(AbyssalSpectralMasteryTuning.Setting.PULL_STRENGTH, 2))));
                    target.velocityModified = true;
                }
                omen.noEscapeTriggered = true;
            }
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
        omen.successfulSwoops++;
        UniqueAbilityApi.emit(omen.execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                AbyssalSpectralMasteryAbilities.HIT, target, 1, omen.swoopDamage);
        if (!target.isAlive()) {
            int mode = AbyssalSpectralMasteryAbilities.tuning(omen.execution).integer(
                    AbyssalSpectralMasteryTuning.Setting.MODE, 0);
            if ((mode & 8192) == 0) resetClaymoreCooldown(actor, omen.stack);
            if ((mode & 512) != 0) {
                actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                        60, 0, false, false, true));
            }
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
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(omen.execution);
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
        float missingHealthMultiplier = 1.0F + missingHealth * (float) tuning.get(
                AbyssalSpectralMasteryTuning.Setting.MISSING_HEALTH_BONUS_CAP,
                Config.uniqueEffects.watcher.claymoreMissingHealthBonus);
        float gathered = Math.min((float) tuning.get(AbyssalSpectralMasteryTuning.Setting.BONUS_CAP, 0),
                omen.successfulSwoops * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.BONUS_PER_TRIGGER, 0));
        float vitalityBefore = target.getHealth() + target.getAbsorptionAmount();
        int maxDread = Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_CAP,
                Config.uniqueEffects.watcher.maxDread));
        float threshold = (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.EXECUTE_THRESHOLD,
                Config.uniqueEffects.watcher.omenInstantKillThreshold)
                * target.getMaxHealth();
        int mode = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0);
        boolean immune = isExecutionImmune(target);
        boolean mercy = (mode & 8192) != 0 && !immune;
        boolean absolute = (mode & 16384) != 0;
        boolean absoluteReady = absoluteReady(absolute, omen.dread,
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.EXECUTE_DREAD_THRESHOLD, 6), maxDread);
        float finalDamage = baseDamage * dreadMultiplier * missingHealthMultiplier * (1 + gathered)
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1)
                * (1 + omen.dread * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.FINAL_PER_STACK_MULTIPLIER, 0))
                * manyEyedScale(tuning);
        if (absolute && (!absoluteReady || target.getHealth() > threshold)) {
            finalDamage *= (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.DAMAGE_MULTIPLIER, .7);
        }
        if (mercy && omen.dread >= maxDread && target.getHealth() <= threshold) {
            finalDamage = Math.min(finalDamage, Math.max(0, vitalityBefore - 1));
        }
        boolean damaged = damageTarget(world, actor, omen.stack, target, finalDamage);

        boolean executed = false;
        if (!mercy && !immune && absoluteReady && damaged && omen.dread >= maxDread
                && (!target.isAlive() || target.getHealth() <= threshold)) {
            executed = true;
            if (target.isAlive()) {
                float lethalDamage = Math.max(1000.0F,
                        target.getMaxHealth() * 10.0F + target.getAbsorptionAmount());
                damageTarget(world, actor, omen.stack, target, lethalDamage);
            }
        }

        if (mercy && damaged && omen.dread >= maxDread && target.isAlive() && target.getHealth() <= threshold) {
            target.setHealth(Math.max(1, target.getHealth()));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.EMBEDDED_DURATION_TICKS, 60),
                    9, false, false, true), actor);
            grantOmenAbsorption(world, actor, tuning, Math.max(0.0F,
                    vitalityBefore - (target.getHealth() + target.getAbsorptionAmount())));
        }

        if (executed) {
            grantOmenAbsorption(world, actor, tuning, Math.max(0.0F,
                    vitalityBefore - (target.getHealth() + target.getAbsorptionAmount())));
        } else if (damaged && (mode & 256) != 0 && target.isAlive()) {
            float removed = Math.max(0, vitalityBefore - target.getHealth() - target.getAbsorptionAmount());
            float reserve = (float) Math.min(tuning.get(AbyssalSpectralMasteryTuning.Setting.REVIVE_ABSORPTION, 4),
                    Math.floor(removed / Math.max(1, target.getMaxHealth() * .1F)));
            float gained = Math.max(0.0F, reserve - actor.getAbsorptionAmount());
            actor.setAbsorptionAmount(Math.max(actor.getAbsorptionAmount(), reserve));
            recordClaim(world, actor, tuning, gained);
        }

        recordOwnerState(world, actor, tuning);
        if (!target.isAlive()) {
            if (!mercy) resetClaymoreCooldown(actor, omen.stack);
            if ((mode & 512) != 0) actor.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, 60, 0, false, false, true));
        } else if ((mode & 4) != 0 && omen.dread >= maxDread) {
            SimplySwordsAPI.setWeaponCooldown(actor, omen.stack, Math.max(0,
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS,
                            Config.uniqueEffects.watcher.claymoreCooldown) - omen.durationTicks
                            - tuning.integer(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_REFUND_TICKS, 30)));
        } else if (absolute && (!absoluteReady || target.getHealth() > threshold)) {
            SimplySwordsAPI.setWeaponCooldown(actor, omen.stack, Math.max(0,
                    tuning.integer(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS,
                            Config.uniqueEffects.watcher.claymoreCooldown) - omen.durationTicks + 100));
        }
        spawnFinalOmenEffects(world, actor, target, executed);
        UniqueAbilityApi.emit(omen.execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                AbyssalSpectralMasteryAbilities.HIT, target, damaged ? 1 : 0, baseDamage);
        UniqueAbilityApi.finish(omen.execution, omen.execution.definition().id(), damaged ? 1 : 0);
    }

    static int totalSwoops(int mode, int spearCount, int minimum, int maximum, int dread,
                           int perStack, float progress) {
        if ((mode & 128) != 0) return 0;
        if ((mode & 64) != 0) return Math.max(0, spearCount);
        return Math.max(0, Math.min(maximum,
                Math.round(MathHelper.lerp(progress, minimum, maximum)) + dread * perStack));
    }

    static boolean absoluteReady(boolean absolute, int dread, int threshold, int maxDread) {
        return !absolute || dread >= Math.max(1, Math.min(threshold, maxDread));
    }

    static boolean crossedLowHealth(float current, float maximum, float amount, int percent) {
        if (percent <= 0 || maximum <= 0.0F) return false;
        float threshold = maximum * percent / 100.0F;
        return current > threshold && current - amount <= threshold;
    }

    static float claimBonus(int points, double perPoint, double cap) {
        if (points <= 0 || perPoint <= 0.0 || cap <= 0.0) return 0.0F;
        return (float) Math.min(cap, points * perPoint);
    }

    static boolean isExecutionImmune(LivingEntity target) {
        return target.getType().isIn(EXECUTION_IMMUNE);
    }

    private static float manyEyedScale(AbyssalSpectralMasteryTuning tuning) {
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 8) == 0) return 1.0F;
        return (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1);
    }

    private static void grantOmenAbsorption(ServerWorld world, LivingEntity actor,
                                            AbyssalSpectralMasteryTuning tuning, float removed) {
        float cap = Math.min(Math.max(0.0F, Config.uniqueEffects.watcher.omenAbsorptionCap),
                Math.max(0.0F, Config.uniqueEffects.abilityAbsorptionCap));
        if (actor.getAbsorptionAmount() >= cap) return;
        float updated = Math.min(cap, actor.getAbsorptionAmount() + removed
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.ABSORPTION_MULTIPLIER, 1));
        float gained = Math.max(0.0F, updated - actor.getAbsorptionAmount());
        actor.setAbsorptionAmount(updated);
        recordClaim(world, actor, tuning, gained);
    }

    private static void recordClaim(ServerWorld world, LivingEntity actor,
                                    AbyssalSpectralMasteryTuning tuning, float gained) {
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 4096) == 0 || gained <= 0.0F) return;
        OwnerState state = ownerState(world, actor.getUuid(), true);
        state.claimPoints = Math.round(gained);
        state.claimExpiryTick = world.getTime()
                + Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.CLAIM_DURATION_TICKS, 120));
        state.expiresAt = Math.max(state.expiresAt, state.claimExpiryTick);
    }

    private static void applyClaimedVitality(ServerWorld world, LivingEntity actor, LivingEntity target,
                                             ItemStack stack, AbyssalSpectralMasteryTuning tuning) {
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & 4096) == 0
                || stack == null || stack.isEmpty()) return;
        OwnerState state = ownerState(world, actor.getUuid(), false);
        if (state == null || state.claimPoints <= 0 || world.getTime() > state.claimExpiryTick) return;
        float bonus = claimBonus(state.claimPoints,
                tuning.get(AbyssalSpectralMasteryTuning.Setting.CLAIM_BONUS_PER_POINT, .05),
                tuning.get(AbyssalSpectralMasteryTuning.Setting.CLAIM_BONUS_CAP, .4));
        state.claimPoints = 0;
        state.claimExpiryTick = 0L;
        if (bonus <= 0.0F) return;
        float weaponDamage = (float) HelperMethods.getAttackFromStack(stack,
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND);
        damageTarget(world, actor, stack, target, weaponDamage * bonus);
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)) return amount;
        OwnerState state = ownerState(world, target.getUuid(), false);
        if (state == null || world.getTime() > state.expiresAt) return amount;
        AbyssalSpectralMasteryTuning tuning = state.tuning;
        int mode = tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0);
        float result = amount;
        if ((mode & 1024) != 0 && isMeleeAttack(source)
                && source.getAttacker() instanceof LivingEntity attacker) {
            Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
            DreadMark mark = marks == null ? null : marks.get(new MarkKey(target.getUuid(),
                    attacker.getUuid(), WatcherWeaponType.CLAYMORE));
            int threshold = Math.max(1, tuning.integer(
                    AbyssalSpectralMasteryTuning.Setting.INCOMING_DREAD_THRESHOLD, 4));
            if (mark != null && mark.dread >= threshold) {
                result *= 1.0F - (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.DAMAGE_REDUCTION, .15);
            }
        }
        if ((mode & 2048) != 0 && world.getTime() >= state.witnessReadyTick
                && crossedLowHealth(target.getHealth(), target.getMaxHealth(), result,
                tuning.integer(AbyssalSpectralMasteryTuning.Setting.LOW_HEALTH_PERCENT, 30))) {
            state.witnessReadyTick = world.getTime() + Math.max(1, tuning.integer(
                    AbyssalSpectralMasteryTuning.Setting.PASSIVE_COOLDOWN_TICKS, 200));
            addWitnessDread(world, target, tuning);
        }
        return result;
    }

    private static boolean isMeleeAttack(DamageSource source) {
        return source != null && (source.isOf(DamageTypes.PLAYER_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }

    private static void addWitnessDread(ServerWorld world, LivingEntity owner, AbyssalSpectralMasteryTuning tuning) {
        Map<MarkKey, DreadMark> marks = ACTIVE_MARKS.get(world);
        if (marks == null) return;
        double range = tuning.get(AbyssalSpectralMasteryTuning.Setting.TRIGGER_RADIUS, 12);
        DreadMark nearest = null;
        LivingEntity nearestTarget = null;
        double best = range * range;
        for (DreadMark mark : marks.values()) {
            if (!mark.key.ownerId.equals(owner.getUuid()) || mark.key.type != WatcherWeaponType.CLAYMORE) continue;
            LivingEntity target = getLivingEntity(world, mark.key.targetId);
            if (target == null || !target.isAlive()
                    || !isValidTarget(world, owner, null, target)) continue;
            double distance = target.squaredDistanceTo(owner);
            if (distance > best) continue;
            best = distance;
            nearest = mark;
            nearestTarget = target;
        }
        if (nearest == null) return;
        int maxDread = Math.max(1, tuning.integer(AbyssalSpectralMasteryTuning.Setting.STACK_CAP,
                Config.uniqueEffects.watcher.maxDread));
        int previous = nearest.dread;
        nearest.dread = Math.min(maxDread, nearest.dread + 2);
        nearest.expiryTick = world.getTime() + Math.max(1, tuning.integer(
                AbyssalSpectralMasteryTuning.Setting.STACK_DURATION_TICKS,
                Config.uniqueEffects.watcher.dreadDuration));
        for (int added = previous; added < nearest.dread; added++) {
            WatcherBatEntity bat = spawnBat(world, owner, nearestTarget, WatcherWeaponType.CLAYMORE,
                    WatcherBatEntity.MODE_MARK,
                    nearestTarget.getPos().add(0.0, nearestTarget.getHeight() * 0.7, 0.0));
            if (bat != null) nearest.batIds.add(bat.getUuid());
        }
        if (nearest.dread > previous) spawnDreadGainEffects(world, nearestTarget, nearest.dread, maxDread);
    }

    private static void recordOwnerState(ServerWorld world, LivingEntity actor, AbyssalSpectralMasteryTuning tuning) {
        if ((tuning.integer(AbyssalSpectralMasteryTuning.Setting.MODE, 0) & (1024 | 2048 | 4096)) == 0) return;
        OwnerState state = ownerState(world, actor.getUuid(), true);
        state.tuning = tuning;
        state.expiresAt = Math.max(state.expiresAt, world.getTime() + OWNER_STATE_TICKS);
    }

    private static OwnerState ownerState(ServerWorld world, UUID ownerId, boolean create) {
        Map<UUID, OwnerState> existing = OWNER_STATES.get(world);
        if (existing == null && !create) return null;
        Map<UUID, OwnerState> states = existing != null ? existing
                : OWNER_STATES.computeIfAbsent(world, ignored -> new HashMap<>());
        OwnerState state = states.get(ownerId);
        if (state != null || !create) return state;
        if (states.size() >= MAX_OWNER_STATES) {
            states.values().stream().min(Comparator.comparingLong(oldest -> oldest.expiresAt))
                    .ifPresent(oldest -> states.remove(oldest.ownerId));
        }
        state = new OwnerState(ownerId);
        states.put(ownerId, state);
        return state;
    }

    private static void pruneOwnerStates(ServerWorld world) {
        Map<UUID, OwnerState> states = OWNER_STATES.get(world);
        if (states == null) return;
        long now = world.getTime();
        states.values().removeIf(state -> state.expiresAt < now);
        if (states.isEmpty()) OWNER_STATES.remove(world);
    }

    private static final class OwnerState {
        private final UUID ownerId;
        private AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryTuning.EMPTY;
        private long expiresAt;
        private long witnessReadyTick;
        private int claimPoints;
        private long claimExpiryTick;

        private OwnerState(UUID ownerId) {
            this.ownerId = ownerId;
        }
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
        SimplySwordsAPI.setWeaponCooldown(actor, stack, 0);
    }

    private static double warglaiveActivationRange(WeaponAbilityContext context) {
        if (context.stack() == null || context.stack().isEmpty()) {
            return Config.uniqueEffects.watcher.activationRange;
        }
        UniqueAbilityExecution execution = MartialCommandEldritchMasteryCombatManager.beginPassive(
                MartialCommandEldritchMasteryAbilities.WARG_MARK, context.world(), context.stack(),
                context.actor(), null, warglaiveMarkBase());
        double range = MartialCommandEldritchMasteryAbilities.tuning(execution).get(
                MartialCommandEldritchMasteryTuning.Setting.RANGE, Config.uniqueEffects.watcher.activationRange);
        MartialCommandEldritchMasteryCombatManager.finish(execution, 0);
        return range;
    }

    private static LivingEntity resolveActivationTarget(WeaponAbilityContext context, WatcherWeaponType type) {
        double range = Math.max(1.0, type == WatcherWeaponType.WARGLAIVE
                ? warglaiveActivationRange(context)
                : Config.uniqueEffects.watcher.activationRange);
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
                                                 UUID ownerId, WatcherWeaponType type, int limit) {
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
        MartialCommandEldritchMasteryCombatManager.finish(hunt.sanguineExecution, hunt.struckTargets.size());
        MartialCommandEldritchMasteryCombatManager.finish(hunt.execution, hunt.struckTargets.size());
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
        private long lastHitTick = Long.MIN_VALUE / 2;
        private long unblinkingReadyTick;
        private long sharedTerrorReadyTick;
        private int hitCount;
        private double legacyRadius;
        private int maxDread = 5;

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
        private final MartialCommandEldritchMasteryTuning tuning;
        private final MartialCommandEldritchMasteryTuning sanguine;
        private final int dreadConsumed;
        private final UniqueAbilityExecution execution;
        private final UniqueAbilityExecution sanguineExecution;
        private final Set<UUID> struckTargets = new HashSet<>();
        private final List<UUID> struckOrder = new ArrayList<>();
        private float healed;
        private int refunded;

        private ActiveHunt(UUID actorId, UUID sourcePlayerId, ItemStack stack, long startedTick,
                           long expiryTick, float baseDamage, List<HuntBat> bats,
                           MartialCommandEldritchMasteryTuning tuning, MartialCommandEldritchMasteryTuning sanguine, int dreadConsumed,
                           UniqueAbilityExecution execution, UniqueAbilityExecution sanguineExecution) {
            this.actorId = actorId;
            this.sourcePlayerId = sourcePlayerId;
            this.stack = stack;
            this.startedTick = startedTick;
            this.expiryTick = expiryTick;
            this.baseDamage = baseDamage;
            this.bats = bats;
            this.tuning = tuning;
            this.sanguine = sanguine;
            this.dreadConsumed = dreadConsumed;
            this.execution = execution;
            this.sanguineExecution = sanguineExecution;
        }
    }

    private static final class HuntBat {
        private final UUID batId;
        private UUID targetId;
        private final Vec3d stagePos;
        private long readyTick;
        private final List<UUID> route = new ArrayList<>();
        private float damageScale = 1.0F;
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
        private final UniqueAbilityExecution execution;
        private int spawnedSwoops;
        private int successfulSwoops;
        private boolean noEscapeTriggered;

        private ActiveOmen(UUID actorId, UUID sourcePlayerId, UUID targetId, ItemStack stack,
                           Hand hand, int dread, long startedTick, long impactTick, int durationTicks,
                           int totalSwoops, float swoopDamage, List<UUID> batIds,
                           List<UUID> waitingBatIds, UniqueAbilityExecution execution) {
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
            this.execution = execution;
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
