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
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
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

public final class DreadwhisperAbilityManager {
    private static final int COLLAPSE_LIFETIME = 8;
    private static final int LEECH_LIFETIME = 10;
    private static final int BURST_LIFETIME = 12;
    private static final int MODE_VOID_CROSSING = 1;
    private static final float VANILLA_CRITICAL_FACTOR = 1.5F;
    private static final double RECALL_STEP = 0.25;
    private static final double TRAIL_VERTICAL_RANGE = 6.0;
    private static final int RECORD_SWEEP_INTERVAL = 40;
    private static final DustColorTransitionParticleEffect REND_DUST = new DustColorTransitionParticleEffect(
            new Vector3f(0.035F, 0.006F, 0.07F), new Vector3f(0.35F, 0.95F, 0.36F), 1.15F);
    private static final Map<ServerWorld, Map<UUID, ActiveRend>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<ReopenKey, Long>> REOPEN_LOCKOUT = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, WoundRecord>> WOUNDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, WoundRecord>> PENDING_WEAKNESS = new HashMap<>();
    private static final ThreadLocal<Boolean> ATTACK_CRITICAL = new ThreadLocal<>();

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
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.DREADWHISPER_REAVE,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                        .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.dreadwhisper.cooldown));
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        Vec3d direction = resolveDirection(context);
        if (direction.horizontalLengthSquared() < 0.0001) {
            return false;
        }

        int maximumTicks = maximumDashTicks(tuning);
        double width = rendWidth(tuning);
        DreadwhisperVisualEntity visual = DreadwhisperVisualEntity.reaveFront(
                context.world(), actor, direction, (float) width,
                (float) Math.max(0.5, tuning.get(StormSoulMasteryTuning.Setting.HEIGHT,
                        Config.uniqueEffects.dreadwhisper.frontHeight)),
                maximumTicks + COLLAPSE_LIFETIME + 4);
        context.world().spawnEntity(visual);

        UUID castId = UUID.randomUUID();
        UUID stainId = GloamStainManager.beginTrail(
                context.world(), actor.getUuid(), groundPosition(context.world(), actor.getPos()),
                direction, width,
                stainDuration(tuning),
                Math.max(1, Config.uniqueEffects.dreadwhisper.stainFadeDuration),
                trailSlowAmplifier(tuning),
                trailBehavior(castId, tuning));

        ActiveRend active = new ActiveRend(
                actor.getUuid(), context.stack().copy(), direction, actor.getPos(),
                context.world().getTime(), visual.getUuid(), stainId, castId, tuning, execution);
        ACTIVE.computeIfAbsent(context.world(), ignored -> new HashMap<>()).put(actor.getUuid(), active);
        applyDashVelocity(actor, direction, tuning);
        spawnActivationEffects(context.world(), actor);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        return !ACTIVE.getOrDefault(world, Map.of()).isEmpty()
                || !REOPEN_LOCKOUT.getOrDefault(world, Map.of()).isEmpty()
                || !WOUNDS.getOrDefault(world, Map.of()).isEmpty()
                || !PENDING_WEAKNESS.getOrDefault(world, Map.of()).isEmpty();
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
                && (isDashing(actor) || DreadwhisperTrailManager.isVeiled(actor));
    }

    public static void beginAttack(PlayerEntity player) {
        ATTACK_CRITICAL.set(player != null
                && player.getAttackCooldownProgress(0.5F) > 0.9F
                && player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle()
                && !player.isSprinting());
    }

    public static void endAttack() {
        ATTACK_CRITICAL.remove();
    }

    public static void tick(ServerWorld world) {
        long now = world.getTime();
        resolveWeaknessCleanup(world);
        pruneReopenLockouts(world, now);
        if (now % RECORD_SWEEP_INTERVAL == 0L) {
            pruneWoundRecords(world);
        }
        tickDashes(world);
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (target == null
                || source == null
                || amount <= 0.0F
                || target.getWorld().isClient()
                || SimplySwordsAPI.getDelegatedWeaponHitContext() != null
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || source.getSource() != attacker
                || !isDirectMeleeSource(source)) {
            return amount;
        }

        StatusEffectInstance wound = target.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND));
        if (wound == null) {
            return amount;
        }
        ItemStack stack = source.getWeaponStack();
        if (stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.DREADWHISPER.get())) {
            return amount;
        }

        ServerWorld world = (ServerWorld) target.getWorld();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.DREADWHISPER_WOUND,
                UniqueAbilityContext.passive(world, stack, attacker, target, null), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);

        boolean critical = wasNaturalCritical(attacker);
        float baseline = critical ? amount / VANILLA_CRITICAL_FACTOR : amount;
        double multiplier = tuning.get(StormSoulMasteryTuning.Setting.WOUND_DAMAGE_MULTIPLIER, 1)
                * (1.0 + woundAgeBonus(world, tuning, target, wound))
                * (1.0 + mortalBonus(tuning, target))
                * (1.0 + gloamRider(world, attacker, target, tuning));
        float woundBonus = (float) (baseline * Math.max(0.0F,
                Config.uniqueEffects.dreadwhisper.criticalMultiplier - 1.0F) * multiplier);
        float applied = critical ? Math.max(0.0F, woundBonus - (amount - baseline)) : woundBonus;
        float result = amount + applied;
        if (target.getAbsorptionAmount() >= result) {
            UniqueAbilityApi.cancel(execution);
            return amount;
        }
        consumeWound(world, attacker, target, stack, tuning, execution, woundBonus);
        return result;
    }

    public static void onStatusEffectRemoved(LivingEntity entity, StatusEffectInstance effect) {
        if (entity == null || effect == null
                || !(entity.getWorld() instanceof ServerWorld world)
                || !effect.getEffectType().equals(
                        EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND))) {
            return;
        }
        WoundRecord record = takeWoundRecord(world, entity.getUuid());
        if (record != null && record.weaknessApplied()) {
            PENDING_WEAKNESS.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(entity.getUuid(), record);
        }
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Map<UUID, ActiveRend> active = ACTIVE.get(world);
        ActiveRend rend = active == null ? null : active.remove(actor.getUuid());
        if (rend != null) {
            cancel(world, actor, rend);
            if (active.isEmpty()) ACTIVE.remove(world);
        }
        takeWoundRecord(world, actor.getUuid());
        Map<UUID, WoundRecord> pending = PENDING_WEAKNESS.get(world);
        if (pending != null) {
            pending.remove(actor.getUuid());
            if (pending.isEmpty()) PENDING_WEAKNESS.remove(world);
        }
        Map<ReopenKey, Long> lockouts = REOPEN_LOCKOUT.get(world);
        if (lockouts != null) {
            lockouts.keySet().removeIf(key -> key.attacker().equals(actor.getUuid())
                    || key.victim().equals(actor.getUuid()));
            if (lockouts.isEmpty()) REOPEN_LOCKOUT.remove(world);
        }
        DreadwhisperTrailManager.clearActor(actor);
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveRend> active = ACTIVE.remove(world);
        if (active != null) {
            for (ActiveRend rend : active.values()) {
                Entity visual = world.getEntity(rend.visualId);
                if (visual != null) visual.discard();
                GloamStainManager.finishTrail(world, rend.stainId);
                UniqueAbilityApi.cancel(rend.execution);
            }
        }
        REOPEN_LOCKOUT.remove(world);
        WOUNDS.remove(world);
        PENDING_WEAKNESS.remove(world);
    }

    public static void clearAll() {
        for (Map<UUID, ActiveRend> active : ACTIVE.values()) {
            for (ActiveRend rend : active.values()) UniqueAbilityApi.cancel(rend.execution);
        }
        ACTIVE.clear();
        REOPEN_LOCKOUT.clear();
        WOUNDS.clear();
        PENDING_WEAKNESS.clear();
    }

    private static void tickDashes(ServerWorld world) {
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

    private static void consumeWound(ServerWorld world, LivingEntity attacker, LivingEntity target,
                                     ItemStack stack, StormSoulMasteryTuning tuning,
                                     UniqueAbilityExecution execution, float woundBonus) {
        target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND));
        spawnWoundBurst(world, target, attacker);
        int splintered = splinterPain(world, attacker, target, stack, tuning, woundBonus, execution);
        reopenWound(world, attacker, target, tuning);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                target, 1, woundBonus);
        UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, 1 + splintered);
    }

    private static boolean wasNaturalCritical(LivingEntity attacker) {
        return attacker instanceof PlayerEntity && Boolean.TRUE.equals(ATTACK_CRITICAL.get());
    }

    private static void resolveWeaknessCleanup(ServerWorld world) {
        Map<UUID, WoundRecord> pending = PENDING_WEAKNESS.get(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        long now = world.getTime();
        for (Map.Entry<UUID, WoundRecord> entry : pending.entrySet()) {
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) continue;
            StatusEffectInstance weakness = living.getStatusEffect(StatusEffects.WEAKNESS);
            if (weakness != null && weakness.getAmplifier() == 0
                    && weakness.getDuration() <= Math.max(0L, entry.getValue().weaknessExpiry() - now)) {
                living.removeStatusEffect(StatusEffects.WEAKNESS);
            }
        }
        PENDING_WEAKNESS.remove(world);
    }

    private static void pruneReopenLockouts(ServerWorld world, long now) {
        Map<ReopenKey, Long> lockouts = REOPEN_LOCKOUT.get(world);
        if (lockouts == null) return;
        lockouts.values().removeIf(ready -> ready <= now);
        if (lockouts.isEmpty()) REOPEN_LOCKOUT.remove(world);
    }

    private static void pruneWoundRecords(ServerWorld world) {
        Map<UUID, WoundRecord> records = WOUNDS.get(world);
        if (records == null) return;
        records.keySet().removeIf(id -> !(world.getEntity(id) instanceof LivingEntity living)
                || !living.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND)));
        if (records.isEmpty()) WOUNDS.remove(world);
    }

    private static WoundRecord takeWoundRecord(ServerWorld world, UUID victimId) {
        Map<UUID, WoundRecord> records = WOUNDS.get(world);
        if (records == null) return null;
        WoundRecord record = records.remove(victimId);
        if (records.isEmpty()) WOUNDS.remove(world);
        return record;
    }

    // Clutching Shadow: the trail's slow level and how long it lingers are both tuned.
    static GloamStainManager.PatchBehavior trailBehavior(UUID castId, StormSoulMasteryTuning tuning) {
        int ticks = tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_TICKS, 0);
        return new GloamStainManager.PatchBehavior(castId, ItemStack.EMPTY, 0,
                trailSlowAmplifier(tuning), ticks > 0 ? ticks : 11, true,
                0, 0, 0, 0, 0, 0, null);
    }

    static int trailSlowAmplifier(StormSoulMasteryTuning tuning) {
        return Math.clamp(tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_LEVEL,
                Config.uniqueEffects.dreadwhisper.stainSlowAmplifier), 0, 4);
    }

    static int stainDuration(StormSoulMasteryTuning tuning) {
        return Math.max(20, tuning.integer(StormSoulMasteryTuning.Setting.STAIN_DURATION_TICKS,
                Config.uniqueEffects.dreadwhisper.stainDuration));
    }

    static double rendWidth(StormSoulMasteryTuning tuning) {
        return Math.max(1.0, tuning.get(StormSoulMasteryTuning.Setting.REND_WIDTH,
                Config.uniqueEffects.dreadwhisper.frontWidth));
    }

    // Reaped Momentum: each enemy already reached speeds up and sharpens the rest of the dash.
    private static double momentumBonus(ActiveRend active, StormSoulMasteryTuning.Setting perHit,
                                        StormSoulMasteryTuning.Setting cap) {
        double bonus = active.tuning.get(perHit, 0);
        if (bonus <= 0) return 0;
        return Math.min(active.tuning.get(cap, 0), active.victims.size() * bonus);
    }

    // Dark Patience: an unconsumed wound sharpens as it ages.
    private static double woundAgeBonus(ServerWorld world, StormSoulMasteryTuning tuning,
                                        LivingEntity target, StatusEffectInstance wound) {
        double perInterval = tuning.get(StormSoulMasteryTuning.Setting.WOUND_AGE_BONUS, 0);
        if (perInterval <= 0 || wound == null) return 0;
        Map<UUID, WoundRecord> records = WOUNDS.get(world);
        WoundRecord record = records == null ? null : records.get(target.getUuid());
        if (record == null) return 0;
        int interval = Math.max(1, tuning.integer(
                StormSoulMasteryTuning.Setting.WOUND_AGE_INTERVAL_TICKS, 20));
        long elapsed = Math.min(Math.max(0L, world.getTime() - record.appliedTick()),
                Math.max(0L, record.durationTicks() - wound.getDuration()));
        return Math.min(tuning.get(StormSoulMasteryTuning.Setting.WOUND_AGE_CAP, 0),
                (elapsed / interval) * perInterval);
    }

    // Mortal Tell: a wounded enemy near death, or a boss, pays extra.
    private static double mortalBonus(StormSoulMasteryTuning tuning, LivingEntity target) {
        double threshold = tuning.get(StormSoulMasteryTuning.Setting.MORTAL_THRESHOLD, 0);
        if (threshold <= 0 || target.getHealth() > target.getMaxHealth() * threshold) return 0;
        return WatcherAbilityManager.isExecutionImmune(target)
                ? tuning.get(StormSoulMasteryTuning.Setting.MORTAL_BOSS_BONUS, 0)
                : tuning.get(StormSoulMasteryTuning.Setting.MORTAL_BONUS, 0);
    }

    // Gloam Hunger: an enemy standing in the wielder's Gloam takes more.
    private static double gloamRider(ServerWorld world, LivingEntity attacker, LivingEntity target,
                                     StormSoulMasteryTuning tuning) {
        double rider = tuning.get(StormSoulMasteryTuning.Setting.GLOAM_DAMAGE_RIDER, 0);
        return rider > 0 && target.isOnGround()
                && GloamStainManager.isOnOwnerGloam(world, attacker.getUuid(), target) ? rider : 0;
    }

    // Splintered Pain: part of the wound's bonus jumps to the nearest neighbour.
    private static int splinterPain(ServerWorld world, LivingEntity attacker, LivingEntity victim,
                                    ItemStack stack, StormSoulMasteryTuning tuning, float bonusDamage,
                                    UniqueAbilityExecution execution) {
        double multiplier = tuning.get(StormSoulMasteryTuning.Setting.SPLINTER_MULTIPLIER, 0);
        double range = tuning.get(StormSoulMasteryTuning.Setting.SPLINTER_RANGE, 0);
        if (multiplier <= 0 || range <= 0 || bonusDamage <= 0) return 0;
        LivingEntity nearest = nearestWithin(world, victim, victim.getPos(), range,
                candidate -> candidate != victim && validTarget(candidate, attacker));
        if (nearest == null) return 0;
        float damage = (float) (bonusDamage * multiplier);
        if (!SimplySwordsAPI.applyEntityWeaponHit(stack, nearest, attacker, damage)) return 0;
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                nearest, 1, damage);
        return 1;
    }

    // Reopen: consuming a wound can leave a fresh, shorter one behind.
    private static void reopenWound(ServerWorld world, LivingEntity attacker, LivingEntity target,
                                    StormSoulMasteryTuning tuning) {
        int chance = tuning.integer(StormSoulMasteryTuning.Setting.REOPEN_CHANCE, 0);
        int duration = tuning.integer(StormSoulMasteryTuning.Setting.REOPEN_DURATION_TICKS, 0);
        if (chance <= 0 || duration <= 0 || !target.isAlive() || target.isRemoved()) return;
        long now = world.getTime();
        ReopenKey key = new ReopenKey(attacker.getUuid(), target.getUuid());
        Map<ReopenKey, Long> lockouts = REOPEN_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>());
        Long ready = lockouts.get(key);
        if (ready != null && now < ready) return;
        int roll = world.random.nextInt(100);
        boolean passed = roll < chance;
        UniqueAbilityApi.reportRoll(attacker, StormSoulMasteryAbilities.DREADWHISPER_WOUND.id(),
                "REOPEN_CHANCE", chance, roll, passed);
        if (!passed) return;
        lockouts.put(key, now + Math.max(1,
                tuning.integer(StormSoulMasteryTuning.Setting.REOPEN_LOCKOUT_TICKS, 120)));
        applyWound(world, target, tuning, duration);
    }

    private static boolean tickDash(ServerWorld world, LivingEntity owner, ActiveRend active) {
        double range = Math.max(1.0, active.tuning.get(StormSoulMasteryTuning.Setting.REND_RANGE,
                Config.uniqueEffects.dreadwhisper.dashDistance));
        Vec3d start = active.previousPosition;
        Vec3d current = owner.getPos();
        double moved = horizontalDistance(start, current);
        double remaining = Math.max(0.0, range - active.distanceTravelled);
        boolean exhausted = false;
        if (moved > remaining) {
            current = clampTravel(start, current, remaining);
            owner.setPosition(current.x, current.y, current.z);
            owner.velocityModified = true;
            moved = remaining;
            exhausted = true;
        }
        if (moved > 0.01) {
            active.distanceTravelled += moved;
            damageDashTargets(world, owner, active, start, current);
            GloamStainManager.extendTrail(world, active.stainId, groundPosition(world, owner.getPos()));
        }
        active.previousPosition = owner.getPos();
        updateFrontPosition(world, active, owner.getPos());
        owner.fallDistance = 0.0F;

        long elapsed = world.getTime() - active.startedAt;
        boolean collided = elapsed > 1L && owner.horizontalCollision;
        if (active.stopped || collided || exhausted
                || active.distanceTravelled >= range
                || elapsed >= maximumDashTicks(active.tuning)) {
            active.endedOnCollision = collided && !active.stopped && !exhausted
                    && active.distanceTravelled < range;
            finishDash(world, owner, active);
            return true;
        }

        applyDashVelocity(owner, active.direction, active.tuning, momentumBonus(active,
                StormSoulMasteryTuning.Setting.MOMENTUM_SPEED_BONUS,
                StormSoulMasteryTuning.Setting.MOMENTUM_SPEED_CAP));
        return false;
    }

    private static void damageDashTargets(ServerWorld world, LivingEntity owner, ActiveRend active,
                                          Vec3d start, Vec3d end) {
        if (active.voidCrossing) {
            return;
        }
        int cap = active.tuning.has(StormSoulMasteryTuning.Setting.REND_TARGET_CAP)
                ? Math.max(1, active.tuning.integer(StormSoulMasteryTuning.Setting.REND_TARGET_CAP, 64))
                : Integer.MAX_VALUE;
        if (active.victims.size() >= cap) {
            return;
        }
        double halfWidth = rendWidth(active.tuning) * 0.5;
        double height = Math.max(0.5, active.tuning.get(StormSoulMasteryTuning.Setting.HEIGHT,
                Config.uniqueEffects.dreadwhisper.frontHeight));
        Box search = segmentBox(start, end, height, halfWidth);
        List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, search,
                candidate -> validTarget(candidate, owner)
                        && !active.attempted.contains(candidate.getUuid())
                        && intersectsSegment(candidate, start, end, halfWidth,
                        Math.max(start.y, end.y) + height)));
        candidates.sort(Comparator.comparingDouble(candidate -> travelOrder(start, end, candidate.getPos())));
        boolean stopOnHit = active.tuning.get(StormSoulMasteryTuning.Setting.REND_STOP_ON_HIT, 0) >= 1;
        for (LivingEntity target : candidates) {
            if (active.victims.size() >= cap) {
                break;
            }
            active.attempted.add(target.getUuid());
            float weaponDamage = dashDamage(owner, active)
                    * (float) (1.0 + gloamRider(world, owner, target, active.tuning));
            float healthBefore = target.getHealth();
            if (!SimplySwordsAPI.applyEntityWeaponHit(active.stack, target, owner, weaponDamage)) {
                continue;
            }
            active.victims.add(target.getUuid());

            float removedHealth = Math.max(0.0F, healthBefore - target.getHealth());
            float healing = removedHealth * (float) MathHelper.clamp(
                    active.tuning.get(StormSoulMasteryTuning.Setting.LEECH_RATIO,
                            Config.uniqueEffects.dreadwhisper.healRatio), 0, 1);
            double leechCap = active.tuning.get(StormSoulMasteryTuning.Setting.LEECH_CAP, 0);
            if (leechCap > 0) {
                healing = (float) Math.max(0.0, Math.min(healing, leechCap - active.leeched));
            }
            if (healing > 0.0F) {
                owner.heal(healing);
                active.leeched += healing;
            }
            grantShelter(world, owner, active);
            if (target.isAlive() && !active.livingShadow) {
                applyWound(world, target, active.tuning, active.tuning.integer(
                        StormSoulMasteryTuning.Setting.WOUND_DURATION_TICKS,
                        Config.uniqueEffects.dreadwhisper.woundDuration));
                spreadWound(world, owner, target, active);
            }
            UniqueAbilityApi.emit(active.execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                    target, 1, weaponDamage);
            spawnContactEffects(world, target, owner, removedHealth > 0.0F);
            if (stopOnHit) {
                stopAtContact(owner, active, start, end, target);
                break;
            }
        }
    }

    private static void stopAtContact(LivingEntity owner, ActiveRend active, Vec3d start, Vec3d end,
                                      LivingEntity victim) {
        active.stopped = true;
        Vec3d line = new Vec3d(end.x - start.x, 0.0, end.z - start.z);
        double length = line.horizontalLength();
        if (length < 1.0E-4) {
            return;
        }
        double progress = MathHelper.clamp(travelOrder(start, end, victim.getPos()), 0.0, 1.0);
        Vec3d contact = start.add(line.multiply(progress));
        active.distanceTravelled -= length * (1.0 - progress);
        owner.setPosition(contact.x, owner.getY(), contact.z);
        owner.velocityModified = true;
        active.previousPosition = owner.getPos();
    }

    private static void applyWound(ServerWorld world, LivingEntity target,
                                   StormSoulMasteryTuning tuning, int durationTicks) {
        int ticks = Math.max(1, durationTicks);
        StatusEffectInstance existing = target.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND));
        if (existing != null && existing.getDuration() >= ticks) {
            return;
        }
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND),
                ticks, 0, false, false, false));
        long now = world.getTime();
        Map<UUID, WoundRecord> stale = PENDING_WEAKNESS.get(world);
        if (stale != null) {
            stale.remove(target.getUuid());
            if (stale.isEmpty()) PENDING_WEAKNESS.remove(world);
        }
        boolean weakness = tuning.integer(StormSoulMasteryTuning.Setting.WOUND_WEAKNESS_TICKS, 0) > 0;
        if (weakness) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 0,
                    false, true, true));
        }
        WOUNDS.computeIfAbsent(world, ignored -> new HashMap<>()).put(target.getUuid(),
                new WoundRecord(now, ticks, weakness, now + ticks));
    }

    private static void updateFrontPosition(ServerWorld world, ActiveRend active, Vec3d position) {
        Entity entity = world.getEntity(active.visualId);
        if (entity instanceof DreadwhisperVisualEntity visual) {
            visual.setPosition(position);
        }
    }

    private static void finishDash(ServerWorld world, LivingEntity owner, ActiveRend active) {
        stopDash(owner);
        GloamStainManager.finishTrail(world, active.stainId);
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
        int secondary = resolveDashFinish(world, owner, active);
        if (!DreadwhisperTrailManager.start(world, owner, active.stack, active.tuning,
                active.castId, active.origin, owner.getPos(), dashDamage(owner, active), active.execution,
                active.victims.size() + secondary)) {
            UniqueAbilityApi.finish(active.execution, StormSoulMasteryAbilities.FINISH,
                    active.victims.size() + secondary);
        }
        DreadwhisperTrailManager.startVeil(world, owner, active.tuning);
    }

    private static int resolveDashFinish(ServerWorld world, LivingEntity owner, ActiveRend active) {
        Vec3d end = owner.getPos();
        float base = dashDamage(owner, active);
        int affected = 0;

        double collision = active.tuning.get(StormSoulMasteryTuning.Setting.COLLISION_MULTIPLIER, 0);
        if (collision > 0 && active.endedOnCollision && !active.voidCrossing
                && active.tuning.get(StormSoulMasteryTuning.Setting.REND_STOP_ON_HIT, 0) < 1) {
            affected += burst(world, owner, active, end, (float) (base * collision),
                    active.tuning.get(StormSoulMasteryTuning.Setting.COLLISION_RADIUS, 3),
                    active.tuning.integer(StormSoulMasteryTuning.Setting.COLLISION_TARGET_CAP, 8));
        }

        double voidCollapse = active.tuning.get(StormSoulMasteryTuning.Setting.VOID_COLLAPSE_MULTIPLIER, 0);
        if (active.voidCrossing && voidCollapse > 0) {
            affected += collapseCast(world, owner, active, end, (float) (base * voidCollapse));
        }

        double recall = active.tuning.get(StormSoulMasteryTuning.Setting.RECALL_STRENGTH, 0);
        double recallRange = active.tuning.get(StormSoulMasteryTuning.Setting.RECALL_RANGE, 0);
        if (recall > 0 && recallRange > 0) {
            recallTargets(world, owner, active, end, recall, recallRange);
        }
        return affected;
    }

    private static void recallTargets(ServerWorld world, LivingEntity owner, ActiveRend active,
                                      Vec3d end, double strength, double range) {
        int cap = Math.max(1, active.tuning.integer(StormSoulMasteryTuning.Setting.RECALL_TARGET_CAP, 8));
        int pulled = 0;
        for (LivingEntity target : sortedWithin(world, owner, end, range,
                candidate -> validTarget(candidate, owner) && candidate.isOnGround()
                        && GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), candidate))) {
            if (pulled >= cap) break;
            Vec3d inward = end.subtract(target.getPos()).multiply(1.0, 0.0, 1.0);
            double length = inward.horizontalLength();
            if (length < 1.0E-4) continue;
            double resistance = MathHelper.clamp(target.getAttributeValue(
                    net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double applied = Math.min(strength * (1.0 - resistance), length);
            if (applied <= 0) continue;
            Vec3d unit = inward.multiply(1.0 / length);
            Vec3d accepted = null;
            for (double travelled = applied; travelled >= RECALL_STEP; travelled -= RECALL_STEP) {
                Vec3d candidate = unit.multiply(travelled);
                if (world.isSpaceEmpty(target,
                        target.getBoundingBox().offset(candidate.x, 0.0, candidate.z))) {
                    accepted = candidate;
                    break;
                }
            }
            if (accepted == null) continue;
            target.setPosition(target.getX() + accepted.x, target.getY(), target.getZ() + accepted.z);
            target.velocityModified = true;
            pulled++;
        }
    }

    private static int burst(ServerWorld world, LivingEntity owner, ActiveRend active, Vec3d centre,
                             float damage, double radius, int targetCap) {
        if (damage <= 0 || radius <= 0) return 0;
        int affected = 0;
        int cap = Math.max(1, targetCap);
        for (LivingEntity target : sortedWithin(world, owner, centre, radius,
                candidate -> validTarget(candidate, owner))) {
            if (affected >= cap) break;
            if (!SimplySwordsAPI.applyEntityWeaponHit(active.stack, target, owner, damage)) continue;
            UniqueAbilityApi.emit(active.execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                    target, 1, damage);
            grantShelter(world, owner, active);
            affected++;
        }
        world.spawnParticles(REND_DUST, centre.x, centre.y + 0.6, centre.z,
                26, radius * 0.4, 0.6, radius * 0.4, 0.07);
        return affected;
    }

    private static int collapseCast(ServerWorld world, LivingEntity owner, ActiveRend active,
                                    Vec3d end, float damage) {
        int cap = Math.max(1, active.tuning.integer(
                StormSoulMasteryTuning.Setting.VOID_COLLAPSE_TARGET_CAP, 12));
        double padding = Math.max(rendWidth(active.tuning),
                active.tuning.get(StormSoulMasteryTuning.Setting.VOID_COLLAPSE_RADIUS, 12));
        int affected = 0;
        if (damage > 0) {
            List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                    new Box(active.origin, end).expand(padding, TRAIL_VERTICAL_RANGE, padding),
                    candidate -> validTarget(candidate, owner) && candidate.isOnGround()
                            && GloamStainManager.isOnSourceGloam(world, active.castId, candidate)));
            targets.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(end)));
            for (LivingEntity target : targets) {
                if (affected >= cap) break;
                if (!SimplySwordsAPI.applyEntityWeaponHit(active.stack, target, owner, damage)) continue;
                UniqueAbilityApi.emit(active.execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                        target, 1, damage);
                grantShelter(world, owner, active);
                affected++;
            }
            world.spawnParticles(REND_DUST, end.x, end.y + 0.6, end.z,
                    26, padding * 0.2, 0.6, padding * 0.2, 0.07);
        }
        GloamStainManager.removeSourcePatches(world, active.castId);
        return affected;
    }

    // Umbral Shelter: each enemy the dash reaches leaves a sliver of Absorption.
    private static void grantShelter(ServerWorld world, LivingEntity owner, ActiveRend active) {
        int perHit = active.tuning.integer(StormSoulMasteryTuning.Setting.SHELTER_ABSORPTION_PER_HIT, 0);
        if (perHit <= 0) return;
        int limit = Math.max(perHit, active.tuning.integer(
                StormSoulMasteryTuning.Setting.SHELTER_ABSORPTION_LIMIT, perHit));
        int granted = Math.min(perHit, limit - active.shelterAbsorption);
        if (granted <= 0) return;
        int ticks = Math.max(1, active.tuning.integer(
                StormSoulMasteryTuning.Setting.SHELTER_BUFF_TICKS, 80));
        active.shelterAbsorption += DreadwhisperTrailManager.grantShelter(world, owner, granted, ticks);
    }

    // Plague of Whispers: a wound from the dash seeds nearby enemies.
    private static void spreadWound(ServerWorld world, LivingEntity owner, LivingEntity origin,
                                    ActiveRend active) {
        int count = active.tuning.integer(StormSoulMasteryTuning.Setting.SPREAD_COUNT, 0);
        double range = active.tuning.get(StormSoulMasteryTuning.Setting.SPREAD_RANGE, 0);
        if (count <= 0 || range <= 0) return;
        int duration = Math.max(1, active.tuning.integer(
                StormSoulMasteryTuning.Setting.SPREAD_DURATION_TICKS, 120));
        int spread = 0;
        for (LivingEntity target : sortedWithin(world, origin, origin.getPos(), range,
                candidate -> candidate != origin && validTarget(candidate, owner)
                        && !candidate.hasStatusEffect(
                                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND)))) {
            if (spread >= count) break;
            applyWound(world, target, active.tuning, duration);
            spread++;
        }
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

    private static int maximumDashTicks(StormSoulMasteryTuning tuning) {
        return Math.max(6, (int) Math.ceil(
                Math.max(1.0, tuning.get(StormSoulMasteryTuning.Setting.REND_RANGE,
                        Config.uniqueEffects.dreadwhisper.dashDistance))
                        / Math.max(0.1, tuning.get(StormSoulMasteryTuning.Setting.REND_SPEED,
                        Config.uniqueEffects.dreadwhisper.dashSpeed))) + 6);
    }

    private static float tunedDashDamage(LivingEntity owner, ActiveRend active) {
        return HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner, active.stack,
                Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitScaling),
                Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitSpellScaling))
                * (float) active.tuning.get(StormSoulMasteryTuning.Setting.REND_DAMAGE_MULTIPLIER, 1);
    }

    private static float dashDamage(LivingEntity owner, ActiveRend active) {
        return tunedDashDamage(owner, active) * (float) (1.0 + momentumBonus(active,
                StormSoulMasteryTuning.Setting.MOMENTUM_DAMAGE_BONUS,
                StormSoulMasteryTuning.Setting.MOMENTUM_DAMAGE_CAP));
    }

    private static void applyDashVelocity(LivingEntity owner, Vec3d direction, StormSoulMasteryTuning tuning) {
        applyDashVelocity(owner, direction, tuning, 0);
    }

    private static void applyDashVelocity(LivingEntity owner, Vec3d direction, StormSoulMasteryTuning tuning,
                                          double speedBonus) {
        double speed = Math.max(0.1, tuning.get(StormSoulMasteryTuning.Setting.REND_SPEED,
                Config.uniqueEffects.dreadwhisper.dashSpeed)) * (1.0 + speedBonus);
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
        GloamStainManager.finishTrail(world, active.stainId);
        Entity visual = world.getEntity(active.visualId);
        if (visual != null) {
            visual.discard();
        }
        UniqueAbilityApi.cancel(active.execution);
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

    private static boolean validTarget(LivingEntity target, LivingEntity owner) {
        return target != owner
                && target.isAlive()
                && !target.isRemoved()
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, owner);
    }

    private static List<LivingEntity> sortedWithin(ServerWorld world, Entity origin, Vec3d centre,
                                                   double range,
                                                   java.util.function.Predicate<LivingEntity> filter) {
        double squared = range * range;
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                Box.of(centre, range * 2.0, range * 2.0, range * 2.0),
                candidate -> candidate != origin && filter.test(candidate)
                        && candidate.getBoundingBox().squaredMagnitude(centre) <= squared));
        targets.sort(Comparator.comparingDouble(candidate ->
                candidate.getBoundingBox().squaredMagnitude(centre)));
        return targets;
    }

    private static LivingEntity nearestWithin(ServerWorld world, Entity origin, Vec3d centre, double range,
                                              java.util.function.Predicate<LivingEntity> filter) {
        List<LivingEntity> targets = sortedWithin(world, origin, centre, range, filter);
        return targets.isEmpty() ? null : targets.getFirst();
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

    private static double travelOrder(Vec3d start, Vec3d end, Vec3d position) {
        Vec3d line = new Vec3d(end.x - start.x, 0.0, end.z - start.z);
        double lengthSquared = line.lengthSquared();
        if (lengthSquared < 0.0001) {
            return 0.0;
        }
        Vec3d point = new Vec3d(position.x - start.x, 0.0, position.z - start.z);
        return MathHelper.clamp(point.dotProduct(line) / lengthSquared, 0.0, 1.0);
    }

    private static Vec3d clampTravel(Vec3d start, Vec3d end, double allowance) {
        Vec3d line = new Vec3d(end.x - start.x, 0.0, end.z - start.z);
        double length = line.horizontalLength();
        if (allowance <= 0.0 || length < 1.0E-4) {
            return new Vec3d(start.x, end.y, start.z);
        }
        Vec3d limited = start.add(line.multiply(Math.min(1.0, allowance / length)));
        return new Vec3d(limited.x, end.y, limited.z);
    }

    private static boolean intersectsSegment(LivingEntity target, Vec3d start, Vec3d end,
                                             double halfWidth, double maxY) {
        if (target.getBoundingBox().maxY < Math.min(start.y, end.y) - 0.5
                || target.getBoundingBox().minY > maxY) {
            return false;
        }
        Vec3d line = new Vec3d(end.x - start.x, 0.0, end.z - start.z);
        double progress = travelOrder(start, end, target.getPos());
        Vec3d closest = new Vec3d(start.x, 0.0, start.z).add(line.multiply(progress));
        double radius = halfWidth + target.getWidth() * 0.5 + target.getTargetingMargin();
        double dx = target.getX() - closest.x;
        double dz = target.getZ() - closest.z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean isDirectMeleeSource(DamageSource source) {
        return source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK);
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

    private record ReopenKey(UUID attacker, UUID victim) {
    }

    private record WoundRecord(long appliedTick, int durationTicks, boolean weaknessApplied,
                               long weaknessExpiry) {
    }

    private static final class ActiveRend {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Vec3d direction;
        private final Vec3d origin;
        private final long startedAt;
        private final UUID visualId;
        private final UUID stainId;
        private final UUID castId;
        private final Set<UUID> attempted = new HashSet<>();
        private final Set<UUID> victims = new HashSet<>();
        private Vec3d previousPosition;
        private double distanceTravelled;
        private float leeched;
        private int shelterAbsorption;
        private boolean stopped;
        private boolean endedOnCollision;
        private final boolean voidCrossing;
        private final boolean livingShadow;
        private final StormSoulMasteryTuning tuning;
        private final UniqueAbilityExecution execution;

        private ActiveRend(UUID ownerId, ItemStack stack, Vec3d direction,
                           Vec3d previousPosition, long startedAt, UUID visualId, UUID stainId,
                           UUID castId, StormSoulMasteryTuning tuning, UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.direction = direction;
            this.origin = previousPosition;
            this.previousPosition = previousPosition;
            this.startedAt = startedAt;
            this.visualId = visualId;
            this.stainId = stainId;
            this.castId = castId;
            this.tuning = tuning;
            this.execution = execution;
            this.voidCrossing = (tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0)
                    & MODE_VOID_CROSSING) != 0;
            this.livingShadow = tuning.integer(StormSoulMasteryTuning.Setting.LIVING_SHADOW_TICKS, 0) > 0
                    && tuning.get(StormSoulMasteryTuning.Setting.LIVING_SHADOW_MULTIPLIER, 0) > 0;
        }
    }
}
