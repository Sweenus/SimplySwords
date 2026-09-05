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
    private static final Map<ServerWorld, Map<UUID, Long>> REOPEN_LOCKOUT = new HashMap<>();

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
        DreadwhisperVisualEntity visual = DreadwhisperVisualEntity.reaveFront(
                context.world(), actor, direction,
                (float) Math.max(1.0, tuning.get(StormSoulMasteryTuning.Setting.REND_WIDTH,
                        Config.uniqueEffects.dreadwhisper.frontWidth)),
                (float) Math.max(0.5, tuning.get(StormSoulMasteryTuning.Setting.HEIGHT,
                        Config.uniqueEffects.dreadwhisper.frontHeight)),
                maximumTicks + COLLAPSE_LIFETIME + 4);
        context.world().spawnEntity(visual);

        UUID stainId = GloamStainManager.beginTrail(
                context.world(), actor.getUuid(), groundPosition(context.world(), actor.getPos()),
                direction, Math.max(1.0, tuning.get(StormSoulMasteryTuning.Setting.REND_WIDTH,
                        Config.uniqueEffects.dreadwhisper.frontWidth)),
                Math.max(20, tuning.integer(StormSoulMasteryTuning.Setting.STAIN_DURATION_TICKS,
                        Config.uniqueEffects.dreadwhisper.stainDuration)),
                Math.max(1, Config.uniqueEffects.dreadwhisper.stainFadeDuration),
                Math.clamp(tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_LEVEL,
                        Config.uniqueEffects.dreadwhisper.stainSlowAmplifier), 0, 4),
                trailBehavior(tuning));

        ActiveRend active = new ActiveRend(
                actor.getUuid(), context.stack().copy(), direction, actor.getPos(),
                context.world().getTime(), visual.getUuid(), stainId, tuning, execution);
        ACTIVE.computeIfAbsent(context.world(), ignored -> new HashMap<>()).put(actor.getUuid(), active);
        applyDashVelocity(actor, direction, tuning);
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
                && (isDashing(actor) || DreadwhisperTrailManager.isVeiled(actor));
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

        ServerWorld world = (ServerWorld) target.getWorld();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.DREADWHISPER_WOUND,
                UniqueAbilityContext.passive(world, stack, attacker, target, null), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);

        StatusEffectInstance wound = target.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND));
        target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND));
        spawnWoundBurst(world, target, attacker);

        double bonus = tuning.get(StormSoulMasteryTuning.Setting.WOUND_DAMAGE_MULTIPLIER, 1);
        bonus *= 1.0 + woundAgeBonus(tuning, wound);
        bonus *= 1.0 + mortalBonus(tuning, target);
        bonus *= 1.0 + gloamRider(world, attacker, target, tuning);

        float result = isNaturalCritical(attacker)
                ? amount
                : amount * Math.max(1.0F, Config.uniqueEffects.dreadwhisper.criticalMultiplier) * (float) bonus;
        splinterPain(world, attacker, target, stack, tuning, result - amount);
        reopenWound(world, attacker, target, tuning);
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT, target, 1, result);
        UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, 1);
        return result;
    }


    // Clutching Shadow: the trail's slow level and how long it lingers are both tuned.
    private static GloamStainManager.PatchBehavior trailBehavior(StormSoulMasteryTuning tuning) {
        int ticks = tuning.integer(StormSoulMasteryTuning.Setting.TRAIL_SLOW_TICKS, 0);
        return ticks > 0
                ? new GloamStainManager.PatchBehavior(ItemStack.EMPTY, 0, ticks, true, 0, 0, 0, 0, 0)
                : GloamStainManager.PatchBehavior.NONE;
    }

    // Reaped Momentum: each enemy already reached speeds up and sharpens the rest of the dash.
    private static double momentumBonus(ActiveRend active, StormSoulMasteryTuning.Setting perHit,
                                        StormSoulMasteryTuning.Setting cap) {
        double bonus = active.tuning.get(perHit, 0);
        if (bonus <= 0) return 0;
        return Math.min(active.tuning.get(cap, 0), active.hitTargets.size() * bonus);
    }

    // Dark Patience: an unconsumed wound sharpens as it ages.
    private static double woundAgeBonus(StormSoulMasteryTuning tuning, StatusEffectInstance wound) {
        double perInterval = tuning.get(StormSoulMasteryTuning.Setting.WOUND_AGE_BONUS, 0);
        if (perInterval <= 0 || wound == null) return 0;
        int total = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.WOUND_DURATION_TICKS,
                Config.uniqueEffects.dreadwhisper.woundDuration));
        int interval = Math.max(1, tuning.integer(
                StormSoulMasteryTuning.Setting.WOUND_AGE_INTERVAL_TICKS, 20));
        int elapsed = Math.max(0, total - wound.getDuration());
        return Math.min(tuning.get(StormSoulMasteryTuning.Setting.WOUND_AGE_CAP, 0),
                (elapsed / interval) * perInterval);
    }

    // Mortal Tell: a wounded enemy near death, or a boss, pays extra.
    private static double mortalBonus(StormSoulMasteryTuning tuning, LivingEntity target) {
        double threshold = tuning.get(StormSoulMasteryTuning.Setting.MORTAL_THRESHOLD, 0);
        if (threshold <= 0) return 0;
        if (WatcherAbilityManager.isExecutionImmune(target)) {
            return tuning.get(StormSoulMasteryTuning.Setting.MORTAL_BOSS_BONUS, 0);
        }
        return target.getHealth() <= target.getMaxHealth() * threshold
                ? tuning.get(StormSoulMasteryTuning.Setting.MORTAL_BONUS, 0) : 0;
    }

    // Gloam Hunger: an enemy standing in the wielder's Gloam takes more.
    private static double gloamRider(ServerWorld world, LivingEntity attacker, LivingEntity target,
                                     StormSoulMasteryTuning tuning) {
        double rider = tuning.get(StormSoulMasteryTuning.Setting.GLOAM_DAMAGE_RIDER, 0);
        return rider > 0 && GloamStainManager.isOnOwnerGloam(world, attacker.getUuid(), target) ? rider : 0;
    }

    // Splintered Pain: part of the wound's bonus jumps to the nearest neighbour.
    private static void splinterPain(ServerWorld world, LivingEntity attacker, LivingEntity victim,
                                     ItemStack stack, StormSoulMasteryTuning tuning, float bonusDamage) {
        double multiplier = tuning.get(StormSoulMasteryTuning.Setting.SPLINTER_MULTIPLIER, 0);
        double range = tuning.get(StormSoulMasteryTuning.Setting.SPLINTER_RANGE, 0);
        if (multiplier <= 0 || range <= 0 || bonusDamage <= 0) return;
        world.getEntitiesByClass(LivingEntity.class, victim.getBoundingBox().expand(range),
                        candidate -> candidate != victim && validTarget(candidate, attacker))
                .stream()
                .min(java.util.Comparator.comparingDouble(victim::squaredDistanceTo))
                .ifPresent(nearest -> runWithoutWoundConsumption(() ->
                        SimplySwordsAPI.applyEntityWeaponHit(stack, nearest, attacker,
                                (float) (bonusDamage * multiplier))));
    }

    // Reopen: consuming a wound can leave a fresh, shorter one behind.
    private static void reopenWound(ServerWorld world, LivingEntity attacker, LivingEntity target,
                                    StormSoulMasteryTuning tuning) {
        int chance = tuning.integer(StormSoulMasteryTuning.Setting.REOPEN_CHANCE, 0);
        int duration = tuning.integer(StormSoulMasteryTuning.Setting.REOPEN_DURATION_TICKS, 0);
        if (chance <= 0 || duration <= 0) return;
        long now = world.getTime();
        Long ready = REOPEN_LOCKOUT.computeIfAbsent(world, ignored -> new HashMap<>()).get(target.getUuid());
        if (ready != null && now < ready) return;
        int roll = world.random.nextInt(100);
        boolean passed = roll < chance;
        UniqueAbilityApi.reportRoll(attacker, StormSoulMasteryAbilities.DREADWHISPER_WOUND.id(),
                "REOPEN_CHANCE", chance, roll, passed);
        if (!passed) return;
        REOPEN_LOCKOUT.get(world).put(target.getUuid(), now + Math.max(1,
                tuning.integer(StormSoulMasteryTuning.Setting.REOPEN_LOCKOUT_TICKS, 120)));
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND),
                duration, 0, false, false, false));
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
    }

    public static void clearAll() {
        for (Map<UUID, ActiveRend> active : ACTIVE.values()) {
            for (ActiveRend rend : active.values()) UniqueAbilityApi.cancel(rend.execution);
        }
        ACTIVE.clear();
        REOPEN_LOCKOUT.clear();
    }

    private static boolean tickDash(ServerWorld world, LivingEntity owner, ActiveRend active) {
        Vec3d current = owner.getPos();
        double moved = horizontalDistance(active.previousPosition, current);
        if (moved > 0.01) {
            active.distanceTravelled += moved;
            damageDashTargets(world, owner, active, active.previousPosition, current);
            GloamStainManager.extendTrail(world, active.stainId, groundPosition(world, current));
        }
        active.previousPosition = current;
        updateFrontPosition(world, active, current);
        owner.fallDistance = 0.0F;

        active.lastGroundPosition = current;
        long elapsed = world.getTime() - active.startedAt;
        if (active.stopped
                || (elapsed > 1L && owner.horizontalCollision)
                || active.distanceTravelled >= Math.max(1.0, active.tuning.get(StormSoulMasteryTuning.Setting.REND_RANGE,
                        Config.uniqueEffects.dreadwhisper.dashDistance))
                || elapsed >= maximumDashTicks(active.tuning)) {
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
        double halfWidth = Math.max(1.0, active.tuning.get(StormSoulMasteryTuning.Setting.REND_WIDTH,
                Config.uniqueEffects.dreadwhisper.frontWidth)) * 0.5;
        double height = Math.max(0.5, active.tuning.get(StormSoulMasteryTuning.Setting.HEIGHT,
                Config.uniqueEffects.dreadwhisper.frontHeight));
        Box search = segmentBox(start, end, height, halfWidth);
        float weaponDamage = HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner, active.stack,
                Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitScaling),
                Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitSpellScaling))
                * (float) active.tuning.get(StormSoulMasteryTuning.Setting.REND_DAMAGE_MULTIPLIER, 1)
                * (float) (1.0 + momentumBonus(active,
                        StormSoulMasteryTuning.Setting.MOMENTUM_DAMAGE_BONUS,
                        StormSoulMasteryTuning.Setting.MOMENTUM_DAMAGE_CAP));
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
            grantShelter(owner, active);
            if (target.isAlive()) {
                applyWound(target, active.tuning);
                spreadWound(world, target, active);
            }
            UniqueAbilityApi.emit(active.execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                    target, 1, weaponDamage);
            spawnContactEffects(world, target, owner, removedHealth > 0.0F);
            if (active.tuning.get(StormSoulMasteryTuning.Setting.REND_STOP_ON_HIT, 0) >= 1) {
                active.stopped = true;
                break;
            }
            if (active.tuning.has(StormSoulMasteryTuning.Setting.REND_TARGET_CAP)
                    && active.hitTargets.size() >= active.tuning.integer(
                            StormSoulMasteryTuning.Setting.REND_TARGET_CAP, 64)) break;
        }
    }

    private static void applyWound(LivingEntity target, StormSoulMasteryTuning tuning) {
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND),
                Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.WOUND_DURATION_TICKS,
                        Config.uniqueEffects.dreadwhisper.woundDuration)),
                0, false, false, false));
        int weakness = tuning.integer(StormSoulMasteryTuning.Setting.WOUND_WEAKNESS_TICKS, 0);
        if (weakness > 0) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, weakness, 0,
                    false, true, true));
        }
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
        resolveDashFinish(world, owner, active);
        UniqueAbilityApi.finish(active.execution, StormSoulMasteryAbilities.FINISH, active.hitTargets.size());
    }

    private static void resolveDashFinish(ServerWorld world, LivingEntity owner, ActiveRend active) {
        Vec3d end = owner.getPos();
        float base = HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, owner, active.stack,
                Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitScaling),
                Math.max(0.0F, Config.uniqueEffects.dreadwhisper.weaponHitSpellScaling));

        double collision = active.tuning.get(StormSoulMasteryTuning.Setting.COLLISION_MULTIPLIER, 0);
        if (collision > 0 && owner.horizontalCollision) {
            burst(world, owner, active, end, (float) (base * collision),
                    active.tuning.get(StormSoulMasteryTuning.Setting.COLLISION_RADIUS, 3),
                    active.tuning.integer(StormSoulMasteryTuning.Setting.COLLISION_TARGET_CAP, 8), false);
        }

        double voidCollapse = active.tuning.get(StormSoulMasteryTuning.Setting.VOID_COLLAPSE_MULTIPLIER, 0);
        if (voidCollapse > 0) {
            burst(world, owner, active, end, (float) (base * voidCollapse),
                    active.tuning.get(StormSoulMasteryTuning.Setting.VOID_COLLAPSE_RADIUS, 12),
                    active.tuning.integer(StormSoulMasteryTuning.Setting.VOID_COLLAPSE_TARGET_CAP, 12), true);
        }

        double recall = active.tuning.get(StormSoulMasteryTuning.Setting.RECALL_STRENGTH, 0);
        double recallRange = active.tuning.get(StormSoulMasteryTuning.Setting.RECALL_RANGE, 0);
        if (recall > 0 && recallRange > 0) {
            int cap = Math.max(1, active.tuning.integer(StormSoulMasteryTuning.Setting.RECALL_TARGET_CAP, 8));
            int pulled = 0;
            for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                    owner.getBoundingBox().expand(recallRange),
                    candidate -> validTarget(candidate, owner)
                            && GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), candidate))) {
                Vec3d inward = end.subtract(target.getPos()).multiply(1.0, 0.0, 1.0);
                double length = inward.horizontalLength();
                if (length < 1.0E-4) continue;
                double resistance = MathHelper.clamp(target.getAttributeValue(
                        net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
                double applied = Math.min(recall * (1.0 - resistance), length);
                if (applied <= 0) continue;
                Vec3d step = inward.multiply(applied / length);
                target.setPosition(target.getX() + step.x, target.getY(), target.getZ() + step.z);
                target.velocityModified = true;
                if (++pulled >= cap) break;
            }
        }

        DreadwhisperTrailManager.start(world, owner, active.stack, active.tuning, end, base);
        DreadwhisperTrailManager.startVeil(world, owner, active.tuning);
    }

    private static void burst(ServerWorld world, LivingEntity owner, ActiveRend active, Vec3d centre,
                              float damage, double radius, int targetCap, boolean gloamOnly) {
        if (damage <= 0 || radius <= 0) return;
        int affected = 0;
        int cap = Math.max(1, targetCap);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                new Box(centre, centre).expand(radius),
                candidate -> validTarget(candidate, owner)
                        && (!gloamOnly || GloamStainManager.isOnOwnerGloam(world, owner.getUuid(), candidate)))) {
            runWithoutWoundConsumption(() ->
                    SimplySwordsAPI.applyEntityWeaponHit(active.stack, target, owner, damage));
            if (++affected >= cap) break;
        }
        world.spawnParticles(REND_DUST, centre.x, centre.y + 0.6, centre.z,
                26, radius * 0.4, 0.6, radius * 0.4, 0.07);
    }

    // Umbral Shelter: each enemy the dash reaches leaves a sliver of Absorption.
    private static void grantShelter(LivingEntity owner, ActiveRend active) {
        int perHit = active.tuning.integer(StormSoulMasteryTuning.Setting.SHELTER_ABSORPTION_PER_HIT, 0);
        if (perHit <= 0) return;
        int limit = Math.max(perHit, active.tuning.integer(
                StormSoulMasteryTuning.Setting.SHELTER_ABSORPTION_LIMIT, perHit));
        if (active.shelterAbsorption >= limit) return;
        active.shelterAbsorption = Math.min(limit, active.shelterAbsorption + perHit);
        int ticks = Math.max(1, active.tuning.integer(StormSoulMasteryTuning.Setting.SHELTER_BUFF_TICKS, 80));
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ticks,
                MathHelper.clamp(active.shelterAbsorption / 4, 0, 9), false, true, true), owner);
        owner.setAbsorptionAmount(Math.max(owner.getAbsorptionAmount(),
                Math.min(Config.uniqueEffects.abilityAbsorptionCap, active.shelterAbsorption)));
    }

    // Plague of Whispers: a wound from the dash seeds nearby enemies.
    private static void spreadWound(ServerWorld world, LivingEntity origin, ActiveRend active) {
        int count = active.tuning.integer(StormSoulMasteryTuning.Setting.SPREAD_COUNT, 0);
        double range = active.tuning.get(StormSoulMasteryTuning.Setting.SPREAD_RANGE, 0);
        if (count <= 0 || range <= 0) return;
        int duration = Math.max(1, active.tuning.integer(
                StormSoulMasteryTuning.Setting.SPREAD_DURATION_TICKS, 120));
        Entity ownerEntity = world.getEntity(active.ownerId);
        if (!(ownerEntity instanceof LivingEntity owner)) return;
        int spread = 0;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                origin.getBoundingBox().expand(range),
                candidate -> candidate != origin && validTarget(candidate, owner)
                        && !candidate.hasStatusEffect(
                                EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND)))) {
            target.addStatusEffect(new StatusEffectInstance(
                    EffectRegistry.getReference(EffectRegistry.CORRUPTED_WOUND),
                    duration, 0, false, false, false));
            if (++spread >= count) break;
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
        private float leeched;
        private int shelterAbsorption;
        private boolean stopped;
        private Vec3d lastGroundPosition = Vec3d.ZERO;
        private final StormSoulMasteryTuning tuning;
        private final UniqueAbilityExecution execution;

        private ActiveRend(UUID ownerId, ItemStack stack, Vec3d direction,
                           Vec3d previousPosition, long startedAt, UUID visualId, UUID stainId,
                           StormSoulMasteryTuning tuning, UniqueAbilityExecution execution) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.direction = direction;
            this.previousPosition = previousPosition;
            this.startedAt = startedAt;
            this.visualId = visualId;
            this.stainId = stainId;
            this.tuning = tuning;
            this.execution = execution;
        }
    }
}
