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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.BuiltinUniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityKey;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BrimstoneClaymoreVisualEntity;
import net.sweenus.simplyswords.entity.BrimstoneWakeVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BrimstoneClaymoreAbilityManager {
    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 24;
    private static final int PLUNGE_TICKS = 18;
    private static final int MAX_AREA_TARGETS = 64;
    private static final int MAX_WAKES = 16;
    private static final String VISUAL_TAG = "simplyswords_brimstone_claymore_visual";
    private static final String WAKE_VISUAL_TAG = "simplyswords_brimstone_wake_visual";
    private static final float WAKE_VERTICAL_RANGE = 6.0F;
    private static final int WAKE_FADE_IN_TICKS = 3;
    private static final int WAKE_FADE_OUT_TICKS = 6;
    private static final Map<ServerWorld, List<ActiveBrimstoneClaymore>> ACTIVE = new HashMap<>();

    private BrimstoneClaymoreAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveBrimstoneClaymore> active = ACTIVE.get(world);
        return active != null && !active.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static void start(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack) {
        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, owner, null, target, null,
                owner instanceof net.minecraft.entity.player.PlayerEntity
                        ? WeaponAbilityActivationSource.PLAYER : WeaponAbilityActivationSource.MOB);
        if (start(context)) {
            UniqueAbilityExecution execution = UniqueAbilityApi.takeStartedExecution();
            if (execution != null) UniqueAbilityApi.start(execution);
        }
    }

    public static boolean start(WeaponAbilityContext context) {
        if (context == null || context.actor() == null || !context.actor().isAlive()
                || context.target() == null || !context.target().isAlive()
                || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) return false;
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(BuiltinUniqueAbilities.BRIMSTONE_RITE,
                UniqueAbilityContext.active(context), tuning -> tuning
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_COOLDOWN_TICKS,
                                Config.uniqueEffects.brimstone_claymore.cooldown)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_DURATION_TICKS,
                                Config.uniqueEffects.brimstone_claymore.duration)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_BASE_RADIUS,
                                (double) Config.uniqueEffects.brimstone_claymore.baseRadius)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_MAX_RADIUS,
                                (double) Config.uniqueEffects.brimstone_claymore.maxRadius)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_INTERVAL_TICKS,
                                Config.uniqueEffects.brimstone_claymore.pulseInterval)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_DAMAGE_SCALING, 1.0)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_SPELL_SCALING,
                                (double) Config.uniqueEffects.brimstone_claymore.spellScaling)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_DAMAGE_MULTIPLIER,
                                (double) Config.uniqueEffects.brimstone_claymore.pulseDamageScaling)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_RADIUS_GROWTH_PER_HIT,
                                (double) Config.uniqueEffects.brimstone_claymore.radiusGrowthPerHit)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_FINAL_DAMAGE_MULTIPLIER,
                                (double) Config.uniqueEffects.brimstone_claymore.finalDamageScaling)
                        .set(BuiltinUniqueAbilities.BRIMSTONE_RITE_TARGET_JUMP_RANGE,
                                Config.uniqueEffects.brimstone_claymore.targetJumpRange));
        ServerWorld world = context.world();
        LivingEntity owner = context.actor();
        LivingEntity target = context.target();
        boolean walking = BuiltinUniqueAbilities.BRIMSTONE_GUARD_WALKING.equals(
                execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_GUARD_MODE));
        Vec3d groundPos = getGroundPos(world, walking ? owner.getPos() : target.getPos());
        float baseRadius = value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_BASE_RADIUS).floatValue();
        if (walking) baseRadius *= value(execution, BuiltinUniqueAbilities.BRIMSTONE_WALKING_RADIUS_MULTIPLIER).floatValue();
        UUID visualId = spawnVisual(world, groundPos, baseRadius);
        long now = world.getTime();
        float weaponDamage = HelperMethods.abilityScaledDamage("fire", owner, context.stack(),
                value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_DAMAGE_SCALING).floatValue(),
                value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_SPELL_SCALING).floatValue());
        double healthRatio = owner.getHealth() / Math.max(1.0F, owner.getMaxHealth());
        ActiveBrimstoneClaymore instance = new ActiveBrimstoneClaymore(
                owner.getUuid(), target.getUuid(), visualId, groundPos,
                now + value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_DURATION_TICKS),
                now + value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_INTERVAL_TICKS),
                baseRadius, context.stack().copy(), weaponDamage, execution, healthRatio);
        ACTIVE.computeIfAbsent(world, ignored -> new ArrayList<>()).add(instance);
        createInitialWake(world, instance);
        spawnStartEffects(world, groundPos);
        return true;
    }

    public static void tick(ServerWorld world) {
        List<ActiveBrimstoneClaymore> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            if (world.getTime() % 40L == 0L) purgeOrphans(world);
            return;
        }
        active.removeIf(instance -> tickInstance(world, instance));
        if (active.isEmpty()) {
            ACTIVE.remove(world);
            purgeOrphans(world);
        }
    }

    private static boolean tickInstance(ServerWorld world, ActiveBrimstoneClaymore instance) {
        Entity ownerEntity = world.getEntity(instance.ownerId);
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            removeVisual(world, instance.visualId);
            removeWakeVisuals(world, instance);
            UniqueAbilityApi.cancel(instance.execution);
            return true;
        }
        tickWakes(world, owner, instance);
        if (instance.impactDone) {
            if (!instance.wakes.isEmpty()) return false;
            UniqueAbilityApi.finish(instance.execution, instance.execution.definition().id(), instance.totalAffected);
            return true;
        }
        if (instance.plungeEndTick > 0L) {
            if (world.getTime() >= instance.plungeEndTick) {
                impact(world, owner, instance);
                removeVisual(world, instance.visualId);
                instance.impactDone = true;
                if (instance.wakes.isEmpty()) {
                    UniqueAbilityApi.finish(instance.execution, instance.execution.definition().id(), instance.totalAffected);
                    return true;
                }
            } else {
                tickPlungeWindup(world, instance);
            }
            return false;
        }
        if (triggerEmergencyPlunge(world, owner, instance)) return false;
        if (BuiltinUniqueAbilities.BRIMSTONE_GUARD_WALKING.equals(
                value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_GUARD_MODE))) {
            instance.pos = getGroundPos(world, owner.getPos());
        } else {
            updateTarget(world, owner, instance);
        }
        updateWake(world, instance);
        updateVisual(world, instance);
        if (world.getTime() >= instance.nextPulseTick) {
            pulse(world, owner, instance);
            instance.nextPulseTick = world.getTime()
                    + value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_INTERVAL_TICKS);
        }
        if (world.getTime() >= instance.expiryTick) startPlunge(world, instance);
        instance.previousHealthRatio = owner.getHealth() / Math.max(1.0F, owner.getMaxHealth());
        return false;
    }

    private static boolean triggerEmergencyPlunge(ServerWorld world, LivingEntity owner,
                                                   ActiveBrimstoneClaymore instance) {
        if (!BuiltinUniqueAbilities.BRIMSTONE_GUARD_LAST_REPRISAL.equals(
                value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_GUARD_MODE))) return false;
        int threshold = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_EMERGENCY_HEALTH_PERCENT);
        double current = owner.getHealth() / Math.max(1.0F, owner.getMaxHealth());
        if (threshold <= 0 || instance.previousHealthRatio <= threshold / 100.0 || current > threshold / 100.0) {
            instance.previousHealthRatio = current;
            return false;
        }
        instance.emergency = true;
        instance.pos = getGroundPos(world, owner.getPos());
        UniqueAbilityApi.emit(instance.execution, UniqueAbilityPhase.HIT,
                BuiltinUniqueAbilities.BRIMSTONE_EMERGENCY_PLUNGE, null, 0, current);
        startPlunge(world, instance);
        return true;
    }

    private static void updateTarget(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        Entity entity = instance.targetId == null ? null : world.getEntity(instance.targetId);
        if (entity instanceof LivingEntity target && target.isAlive() && HelperMethods.checkAbilityTarget(target, owner)) {
            instance.pos = getGroundPos(world, target.getPos());
            return;
        }
        LivingEntity next = findJumpTarget(world, owner, instance.pos, instance.targetId,
                value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_TARGET_JUMP_RANGE));
        instance.targetId = next == null ? null : next.getUuid();
        if (next == null) return;
        instance.pos = getGroundPos(world, next.getPos());
        spawnJumpEffects(world, instance.pos);
        UniqueAbilityApi.emit(instance.execution, UniqueAbilityPhase.HIT,
                BuiltinUniqueAbilities.BRIMSTONE_TARGET_JUMP, next, 1, 0.0);
        double snapback = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_SNAPBACK_DAMAGE_MULTIPLIER);
        if (snapback > 0.0) {
            float damage = instance.weaponDamage
                    * value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_DAMAGE_MULTIPLIER).floatValue()
                    * (float) snapback;
            instance.totalAffected += damageInRadius(world, owner, instance, instance.pos,
                    value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_SNAPBACK_RADIUS), damage,
                    BuiltinUniqueAbilities.BRIMSTONE_SNAPBACK_HIT, false);
        }
    }

    private static LivingEntity findJumpTarget(ServerWorld world, LivingEntity owner, Vec3d pos,
                                               UUID previousTarget, double range) {
        Box box = new Box(pos, pos).expand(Math.max(0.0, range));
        return world.getOtherEntities(owner, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(target -> previousTarget == null || !target.getUuid().equals(previousTarget))
                .filter(target -> target.isAlive() && target.squaredDistanceTo(pos) <= range * range)
                .filter(target -> HelperMethods.checkAbilityTarget(target, owner))
                .min(Comparator.comparingDouble(target -> target.squaredDistanceTo(pos))).orElse(null);
    }

    private static void pulse(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        float multiplier = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_DAMAGE_MULTIPLIER).floatValue()
                * instance.pulseMultiplier * (1.0F + instance.overpressure);
        float damage = instance.weaponDamage * multiplier;
        int damaged = damageInRadius(world, owner, instance, instance.pos, instance.radius, damage,
                BuiltinUniqueAbilities.BRIMSTONE_PULSE_HIT, false);
        instance.totalAffected += damaged;
        if (damaged > 0) {
            float maxRadius = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_MAX_RADIUS).floatValue();
            float growth = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_RADIUS_GROWTH_PER_HIT).floatValue() * damaged;
            instance.radius = MathHelper.clamp(instance.radius + growth, instance.radius, maxRadius);
            syncWakeVisuals(world, instance);
            instance.overpressure = Math.min(value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_OVERPRESSURE_CAP).floatValue(),
                    instance.overpressure + value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_OVERPRESSURE_PER_PULSE).floatValue());
            if (BuiltinUniqueAbilities.BRIMSTONE_RITE_PERPETUAL.equals(
                    value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_DURATION_MODE))) {
                instance.pulseMultiplier = grownPulseMultiplier(instance.pulseMultiplier,
                        value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_PERPETUAL_PER_PULSE).floatValue(),
                        value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_PERPETUAL_CAP).floatValue());
            }
        }
        UniqueAbilityApi.emit(instance.execution, UniqueAbilityPhase.HIT,
                BuiltinUniqueAbilities.BRIMSTONE_PULSE_FINISH, null, damaged, damage);
        spawnPulseEffects(world, instance.pos, instance.radius, damaged);
    }

    private static void startPlunge(ServerWorld world, ActiveBrimstoneClaymore instance) {
        instance.plungeEndTick = world.getTime() + PLUNGE_TICKS;
        Entity entity = instance.visualId == null ? null : world.getEntity(instance.visualId);
        if (entity instanceof BrimstoneClaymoreVisualEntity visual) {
            visual.setPlunging(true);
            visual.setPlungeStartAge(visual.age);
            visual.setRadius(instance.radius);
            visual.setScale(getScale(instance.execution, instance.radius) * 1.15F);
        }
        UniqueAbilityApi.emit(instance.execution, UniqueAbilityPhase.HIT,
                BuiltinUniqueAbilities.BRIMSTONE_PLUNGE_START, null, 0, instance.radius);
        Vec3d pos = instance.pos;
        world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.2, pos.z, 18, 0.35, 0.55, 0.35, 0.05);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.0, pos.z, 24, 0.5, 0.55, 0.5, 0.04);
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.DARK_SWORD_UNFOLD.get(), SoundCategory.PLAYERS, 0.75F, 0.55F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.65F, 0.55F);
    }

    private static void tickPlungeWindup(ServerWorld world, ActiveBrimstoneClaymore instance) {
        if (!Config.general.enableModernFieldEffects || world.getTime() % 2L != 0L) return;
        long plungeAge = PLUNGE_TICKS - Math.max(0L, instance.plungeEndTick - world.getTime());
        Vec3d pos = instance.pos;
        double height = plungeAge < 7L ? 2.6 + plungeAge * 0.12 : Math.max(0.35, 3.6 - (plungeAge - 7L) * 0.32);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + height, pos.z, 4, 0.18, 0.18, 0.18, 0.04);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + height, pos.z, 3, 0.22, 0.18, 0.22, 0.035);
        if (plungeAge >= 7L) world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + height, pos.z, 2, 0.12, 0.12, 0.12, 0.04);
    }

    private static void impact(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        float baseRadius = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_BASE_RADIUS).floatValue();
        float finalMultiplier = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_FINAL_DAMAGE_MULTIPLIER).floatValue();
        if (instance.emergency) finalMultiplier *= value(instance.execution,
                BuiltinUniqueAbilities.BRIMSTONE_EMERGENCY_FINAL_MULTIPLIER).floatValue();
        float damage = instance.weaponDamage * finalMultiplier * (instance.radius / Math.max(0.1F, baseRadius))
                * (1.0F + instance.overpressure);
        int damaged = damageInRadius(world, owner, instance, instance.pos, instance.radius, damage,
                BuiltinUniqueAbilities.BRIMSTONE_PLUNGE_HIT, true);
        instance.totalAffected += damaged;
        spawnImpactEffects(world, instance.pos, instance.radius, damaged);
    }

    private static int damageInRadius(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance,
                                      Vec3d pos, double radius, float damage, Identifier eventId, boolean finalImpact) {
        Box box = new Box(pos, pos).expand(radius);
        int damaged = 0;
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, candidate -> candidate != owner
                && candidate.isAlive() && candidate.squaredDistanceTo(pos) <= radius * radius
                && EntityPredicates.VALID_LIVING_ENTITY.test(candidate)
                && HelperMethods.checkAbilityTarget(candidate, owner))) {
            target.setOnFireFor(finalImpact ? 5 : 2);
            DamageSource source = world.getDamageSources().indirectMagic(owner, owner);
            float enchanted = HelperMethods.applyAbilityDamageEnchantments(world, instance.stack, target, source, damage);
            boolean[] applied = {false};
            WeaponImplicitRegistry.runSuppressed(() -> applied[0] = HelperMethods.damageThroughIframes(target, source, enchanted));
            if (!applied[0]) continue;
            damaged++;
            if (!finalImpact) applyPulseControl(instance, target, pos);
            Vec3d targetPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.5), 0.0);
            world.spawnParticles(ParticleTypes.FLAME, targetPos.x, targetPos.y, targetPos.z,
                    finalImpact ? 10 : 4, 0.18, 0.2, 0.18, 0.03);
            world.spawnParticles(ParticleTypes.LAVA, targetPos.x, targetPos.y, targetPos.z,
                    finalImpact ? 5 : 2, 0.16, 0.12, 0.16, 0.02);
            UniqueAbilityApi.emit(instance.execution, UniqueAbilityPhase.HIT, eventId, target, 1, damage);
            if (damaged >= MAX_AREA_TARGETS) break;
        }
        return damaged;
    }

    private static void applyPulseControl(ActiveBrimstoneClaymore instance, LivingEntity target, Vec3d center) {
        double pull = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_PULL);
        if (BuiltinUniqueAbilities.BRIMSTONE_GUARD_WALKING.equals(
                value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_GUARD_MODE))) {
            pull = Math.max(pull, value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WALKING_PULL));
        }
        Vec3d delta = center.subtract(target.getPos());
        double length = delta.horizontalLength();
        if (pull > 0.0 && length > 0.001) {
            target.setVelocity(target.getVelocity().add(delta.x / length * pull, 0.03, delta.z / length * pull));
            target.velocityModified = true;
        }
        int slow = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_SLOWNESS_TICKS);
        if (slow > 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slow, 0,
                false, true, true), instance.execution.context().actor());
    }

    private static void createInitialWake(ServerWorld world, ActiveBrimstoneClaymore instance) {
        int duration = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_DURATION_TICKS);
        double damage = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_DAMAGE_MULTIPLIER);
        if (duration <= 0 || damage <= 0.0) return;
        addWake(world, instance, instance.pos, duration);
    }

    private static void updateWake(ServerWorld world, ActiveBrimstoneClaymore instance) {
        int duration = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_DURATION_TICKS);
        double damage = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_DAMAGE_MULTIPLIER);
        double minimum = value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_MIN_MOVE);
        if (duration <= 0 || damage <= 0.0) return;
        WakeUpdate update = wakeUpdate(instance.currentWake != null, instance.lastWakePos, instance.pos,
                minimum, world.getTime(), instance.nextWakeRefreshTick);
        if (update == WakeUpdate.NONE) return;
        if (update == WakeUpdate.REFRESH) {
            instance.currentWake.expiryTick = world.getTime() + duration;
            refreshWakeVisual(world, instance.currentWake, instance.radius);
            instance.nextWakeRefreshTick = world.getTime()
                    + value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_INTERVAL_TICKS);
            return;
        }
        addWake(world, instance, instance.pos, duration);
    }

    private static void addWake(ServerWorld world, ActiveBrimstoneClaymore instance, Vec3d pos, int duration) {
        instance.lastWakePos = pos;
        if (instance.wakes.size() >= MAX_WAKES) {
            Wake removed = instance.wakes.remove(0);
            removeVisual(world, removed.visualId);
            if (removed == instance.currentWake) instance.currentWake = null;
        }
        UUID wakeVisualId = spawnWakeVisual(
                world, instance.wakeGroupId, pos, instance.radius, duration);
        Wake wake = new Wake(pos, world.getTime() + duration, world.getTime(), wakeVisualId);
        instance.wakes.add(wake);
        instance.currentWake = wake;
        instance.nextWakeRefreshTick = world.getTime()
                + value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_INTERVAL_TICKS);
    }

    static boolean shouldCreateWake(Vec3d lastWake, Vec3d current, double minimum) {
        return lastWake == null || lastWake.squaredDistanceTo(current) >= minimum * minimum;
    }

    static WakeUpdate wakeUpdate(boolean hasCurrentWake, Vec3d lastWake, Vec3d current,
                                 double minimum, long now, long nextRefreshTick) {
        if (!hasCurrentWake || shouldCreateWake(lastWake, current, minimum)) return WakeUpdate.CREATE;
        return now >= nextRefreshTick ? WakeUpdate.REFRESH : WakeUpdate.NONE;
    }

    static float grownPulseMultiplier(float current, float perPulse, float cap) {
        return Math.min(cap, current + perPulse);
    }

    private static void tickWakes(ServerWorld world, LivingEntity owner, ActiveBrimstoneClaymore instance) {
        Iterator<Wake> iterator = instance.wakes.iterator();
        while (iterator.hasNext()) {
            Wake wake = iterator.next();
            if (world.getTime() > wake.expiryTick) {
                if (wake == instance.currentWake) instance.currentWake = null;
                removeVisual(world, wake.visualId);
                iterator.remove();
                continue;
            }
            if (world.getTime() < wake.nextPulseTick) continue;
            wake.nextPulseTick = world.getTime()
                    + value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_INTERVAL_TICKS);
            float damage = instance.weaponDamage
                    * value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_PULSE_DAMAGE_MULTIPLIER).floatValue()
                    * value(instance.execution, BuiltinUniqueAbilities.BRIMSTONE_WAKE_DAMAGE_MULTIPLIER).floatValue();
            instance.totalAffected += damageInRadius(world, owner, instance, wake.pos, instance.radius, damage,
                    BuiltinUniqueAbilities.BRIMSTONE_WAKE_HIT, false);
            spawnPulseEffects(world, wake.pos, instance.radius, 0);
        }
    }

    private static void updateVisual(ServerWorld world, ActiveBrimstoneClaymore instance) {
        Entity entity = instance.visualId == null ? null : world.getEntity(instance.visualId);
        if (entity instanceof BrimstoneClaymoreVisualEntity visual) {
            visual.setPos(instance.pos.x, instance.pos.y, instance.pos.z);
            visual.setRadius(instance.radius);
            visual.setScale(getScale(instance.execution, instance.radius));
        }
    }

    private static UUID spawnVisual(ServerWorld world, Vec3d pos, float radius) {
        if (!Config.general.enableModernFieldEffects) return null;
        BrimstoneClaymoreVisualEntity visual = new BrimstoneClaymoreVisualEntity(world, pos.x, pos.y, pos.z, radius);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    private static UUID spawnWakeVisual(ServerWorld world, UUID groupId, Vec3d pos,
                                        float radius, int duration) {
        if (!Config.general.enableModernFieldEffects) return null;
        BrimstoneWakeVisualEntity visual = new BrimstoneWakeVisualEntity(
                world, groupId, pos.x, pos.y, pos.z, radius,
                WAKE_VERTICAL_RANGE, duration + 1, world.random.nextInt());
        visual.setFadeInDuration(WAKE_FADE_IN_TICKS);
        visual.setFadeOutDuration(WAKE_FADE_OUT_TICKS);
        visual.addCommandTag(WAKE_VISUAL_TAG);
        world.spawnEntity(visual);
        return visual.getUuid();
    }

    private static void refreshWakeVisual(ServerWorld world, Wake wake, float radius) {
        Entity entity = wake.visualId == null ? null : world.getEntity(wake.visualId);
        if (entity instanceof BrimstoneWakeVisualEntity visual) {
            visual.setRadius(radius);
            visual.setLifetime(visual.age + visualLifetime(world.getTime(), wake.expiryTick));
        }
    }

    private static void syncWakeVisuals(ServerWorld world, ActiveBrimstoneClaymore instance) {
        for (Wake wake : instance.wakes) {
            Entity entity = wake.visualId == null ? null : world.getEntity(wake.visualId);
            if (entity instanceof BrimstoneWakeVisualEntity visual) {
                visual.setRadius(instance.radius);
            }
        }
    }

    static int visualLifetime(long now, long expiryTick) {
        return Math.toIntExact(Math.max(1L,
                Math.min(Integer.MAX_VALUE, expiryTick - now + 1L)));
    }

    private static float getScale(UniqueAbilityExecution execution, float radius) {
        float base = value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_BASE_RADIUS).floatValue();
        float maximum = Math.max(base, value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_MAX_RADIUS).floatValue());
        if (maximum <= base) return 1.25F;
        return 1.25F + MathHelper.clamp((radius - base) / (maximum - base), 0.0F, 1.0F) * 1.05F;
    }

    private static Vec3d getGroundPos(ServerWorld world, Vec3d pos) {
        return new Vec3d(pos.x, findGroundTopY(world, pos.x, pos.z, pos.y), pos.z);
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) return y + 1.0;
        }
        return centerY;
    }

    private static void removeVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) return;
        Entity visual = world.getEntity(visualId);
        if (visual != null) visual.discard();
    }

    private static void removeWakeVisuals(ServerWorld world, ActiveBrimstoneClaymore instance) {
        for (Wake wake : instance.wakes) {
            removeVisual(world, wake.visualId);
        }
        instance.wakes.clear();
        instance.currentWake = null;
    }

    private static void purgeOrphans(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if ((entity instanceof BrimstoneClaymoreVisualEntity
                    && entity.getCommandTags().contains(VISUAL_TAG))
                    || (entity instanceof BrimstoneWakeVisualEntity
                    && entity.getCommandTags().contains(WAKE_VISUAL_TAG))) {
                entity.discard();
            }
        }
    }

    private static void spawnStartEffects(ServerWorld world, Vec3d pos) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.5, pos.z, 18, 0.35, 0.45, 0.35, 0.04);
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.3, pos.z, 8, 0.2, 0.35, 0.2, 0.02);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.4, pos.z, 10, 0.3, 0.35, 0.3, 0.03);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.55F, 0.85F);
    }

    private static void spawnJumpEffects(ServerWorld world, Vec3d pos) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.2, pos.z, 12, 0.3, 0.35, 0.3, 0.05);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.2, pos.z, 8, 0.28, 0.3, 0.28, 0.04);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.35F, 0.75F);
    }

    private static void spawnPulseEffects(ServerWorld world, Vec3d pos, float radius, int damaged) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.2, pos.z, 10 + damaged * 2, radius * 0.35, 0.12, radius * 0.35, 0.03);
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.25, pos.z, 8 + damaged, radius * 0.32, 0.12, radius * 0.32, 0.02);
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.25, pos.z, 3 + damaged, radius * 0.25, 0.08, radius * 0.25, 0.02);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.35F, 0.7F + world.random.nextFloat() * 0.2F);
        if (damaged > 0) world.playSound(null, pos.x, pos.y, pos.z,
                SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.45F, 0.95F);
    }

    private static void spawnImpactEffects(ServerWorld world, Vec3d pos, float radius, int damaged) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.35, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.45, pos.z, 28 + damaged * 2, radius * 0.35, 0.3, radius * 0.35, 0.09);
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.35, pos.z, 48, radius * 0.45, 0.25, radius * 0.45, 0.08);
            world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.x, pos.y + 0.45, pos.z, 24, radius * 0.38, 0.35, radius * 0.38, 0.05);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.getDefaultState()),
                    pos.x, pos.y + 0.15, pos.z, 30, radius * 0.32, 0.16, radius * 0.32, 0.08);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 0.65F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.55F, 0.85F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.8F, 0.65F);
    }

    private static <T> T value(UniqueAbilityExecution execution, UniqueAbilityKey<T> key) {
        return execution.tuning().get(key);
    }

    private static final class ActiveBrimstoneClaymore {
        private final UUID ownerId;
        private UUID targetId;
        private final UUID visualId;
        private Vec3d pos;
        private final long expiryTick;
        private long nextPulseTick;
        private long plungeEndTick = -1L;
        private float radius;
        private final ItemStack stack;
        private final float weaponDamage;
        private final UniqueAbilityExecution execution;
        private final List<Wake> wakes = new ArrayList<>();
        private final UUID wakeGroupId = UUID.randomUUID();
        private Wake currentWake;
        private Vec3d lastWakePos;
        private long nextWakeRefreshTick;
        private float overpressure;
        private float pulseMultiplier;
        private double previousHealthRatio;
        private int totalAffected;
        private boolean emergency;
        private boolean impactDone;

        private ActiveBrimstoneClaymore(UUID ownerId, UUID targetId, UUID visualId, Vec3d pos,
                                        long expiryTick, long nextPulseTick, float radius, ItemStack stack,
                                        float weaponDamage, UniqueAbilityExecution execution, double healthRatio) {
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.visualId = visualId;
            this.pos = pos;
            this.lastWakePos = pos;
            this.expiryTick = expiryTick;
            this.nextPulseTick = nextPulseTick;
            this.radius = radius;
            this.stack = stack;
            this.weaponDamage = weaponDamage;
            this.execution = execution;
            this.previousHealthRatio = healthRatio;
            this.pulseMultiplier = BuiltinUniqueAbilities.BRIMSTONE_RITE_PERPETUAL.equals(
                    value(execution, BuiltinUniqueAbilities.BRIMSTONE_RITE_DURATION_MODE))
                    ? value(execution, BuiltinUniqueAbilities.BRIMSTONE_PERPETUAL_START_MULTIPLIER).floatValue() : 1.0F;
        }
    }

    private static final class Wake {
        private final Vec3d pos;
        private final UUID visualId;
        private long expiryTick;
        private long nextPulseTick;

        private Wake(Vec3d pos, long expiryTick, long nextPulseTick, UUID visualId) {
            this.pos = pos;
            this.expiryTick = expiryTick;
            this.nextPulseTick = nextPulseTick;
            this.visualId = visualId;
        }
    }

    enum WakeUpdate {
        NONE,
        CREATE,
        REFRESH
    }
}
