package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryTuning;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.WaxweaverWaxVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WaxweaverEncasementManager {

    public static final int FORMATION_TICKS = 10;
    private static final double PLAYER_FALLBACK_RANGE = 8.0;
    private static final double MAX_MASTERY_RANGE_BONUS = 3.0;
    private static final Map<ServerWorld, Map<UUID, ActiveEncasement>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, FlashWax>> FLASH_WAX = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, RefundWindow>> TEMPO_REFUNDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> REACTIVE_COOLDOWN = new HashMap<>();

    private WaxweaverEncasementManager() {
    }

    public static double prisonRange(double configured, NatureSwarmMasteryTuning tuning) {
        return Math.max(1, configured + tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_PRISON_RANGE_BONUS, 0));
    }

    public static int prisonDuration(int configured, NatureSwarmMasteryTuning tuning) {
        int base = tuning.flag(1 << 7)
                ? tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_IRON_DURATION_TICKS, 200)
                : tuning.flag(1 << 8)
                ? tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_VOLATILE_DURATION_TICKS, 60)
                : configured;
        return Math.max(1, base + tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_PRISON_DURATION_BONUS_TICKS, 0));
    }

    public static int tauntInterval(int configured, NatureSwarmMasteryTuning tuning) {
        return Math.max(1, configured + tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_TAUNT_INTERVAL_BONUS_TICKS, 0));
    }

    public static double tauntRadius(double configured, NatureSwarmMasteryTuning tuning) {
        double radius = configured + tuning.get(NatureSwarmMasteryTuning.Setting.WAX_TAUNT_RADIUS_BONUS, 0);
        return Math.max(1, radius * tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_VOLATILE_TAUNT_RADIUS_MULTIPLIER, 1));
    }

    public static double explosionRadius(double configured, NatureSwarmMasteryTuning tuning) {
        return Math.max(.5, configured + tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_EXPLOSION_RADIUS_BONUS, 0));
    }

    public static float explosionDamageMultiplier(NatureSwarmMasteryTuning tuning) {
        double multiplier = tuning.get(NatureSwarmMasteryTuning.Setting.WAX_EXPLOSION_DAMAGE_MULTIPLIER, 1);
        if (tuning.flag(1 << 7)) multiplier *= tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_IRON_DAMAGE_MULTIPLIER, .6);
        if (tuning.flag(1 << 8)) multiplier *= tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_VOLATILE_DAMAGE_MULTIPLIER, 1.9);
        return (float) multiplier;
    }

    public static int explosionFireTicks(int configured, NatureSwarmMasteryTuning tuning) {
        return Math.max(0, configured + tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_EXPLOSION_FIRE_BONUS_TICKS, 0));
    }

    public static boolean canStart(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.WAXWEAVER.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !isCasterActive(context.actor())
                && resolveTarget(context) != null;
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canStart(context)) return false;
        LivingEntity target = resolveTarget(context);
        if (target == null) return false;

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        UniqueAbilityExecution execution = NatureSwarmMasteryCombatManager.beginActive(
                NatureSwarmMasteryAbilities.WAXWEAVER_PRISON, context, Config.uniqueEffects.waxweaver.activeCooldown);
        NatureSwarmMasteryTuning tuning = NatureSwarmMasteryAbilities.tuning(execution);
        double targetRange = prisonRange(Config.uniqueEffects.waxweaver.targetRange, tuning);
        if (actor.squaredDistanceTo(target) > targetRange * targetRange) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        UUID principalId = context.sourcePlayer() == null ? actor.getUuid() : context.sourcePlayer().getUuid();
        float attack = HelperMethods.abilityScaledDamage(SpellScalingProfile.FIRE, actor, context.stack(),
                1.0F, Config.uniqueEffects.waxweaver.spellScaling);
        int duration = prisonDuration(Config.uniqueEffects.waxweaver.encasementDuration, tuning);
        Vec3d anchor = target.getPos();

        target.stopRiding();
        ActiveEncasement state = new ActiveEncasement(
                actor.getUuid(), principalId, target.getUuid(), context.stack().copy(),
                anchor, world.getTime(), world.getTime() + duration, attack, tuning, execution,
                target.getHealth(), consumeFlashWax(world, actor.getUuid()), true, true);
        WaxweaverWaxVisualEntity visual = new WaxweaverWaxVisualEntity(
                world, WaxweaverWaxVisualEntity.MODE_ENCASE, actor, target,
                anchor.x, anchor.y, anchor.z, Math.max(target.getWidth(), target.getHeight()), duration);
        if (world.spawnEntity(visual)) state.visualId = visual.getUuid();

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), state);
        UniqueAbilityApi.start(execution);
        int absorption = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_FIRST_LAYER_ABSORPTION, 0);
        if (absorption > 0 && tuning.flag(1 << 20)) {
            actor.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.ABSORPTION,
                    tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_FIRST_LAYER_DURATION_TICKS, 60),
                    Math.max(0, absorption / 4 - 1), false, true, true));
        }
        applyPrison(world, target, state);
        spawnEncasementEffects(world, target);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        return states != null && !states.isEmpty()
                || hasEntries(FLASH_WAX.get(world))
                || hasEntries(TEMPO_REFUNDS.get(world))
                || hasEntries(REACTIVE_COOLDOWN.get(world));
    }

    public static boolean isEncased(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.WAX_ENCASED))) return true;
        if (!(entity.getWorld() instanceof ServerWorld world)) return false;
        return findByTarget(world, entity.getUuid()) != null;
    }

    //
    // Client-side visual test used by entity renderers. The prisoner remains visible while the
    // sarcophagus forms, then its model is hidden once the opaque shell has sealed around it.
    //
    public static boolean isVisuallySealed(LivingEntity entity) {
        if (entity == null) return false;
        StatusEffectInstance effect = entity.getStatusEffect(
                EffectRegistry.getReference(EffectRegistry.WAX_ENCASED));
        if (effect == null) return false;
        return effect.getAmplifier() > 0;
    }

    public static LivingEntity findPlayerTarget(PlayerEntity player) {
        if (player == null || !player.isAlive()) return null;
        double range = Math.max(1.0, Config.uniqueEffects.waxweaver.targetRange + MAX_MASTERY_RANGE_BONUS);
        return StealSwordItem.findLenientTarget(player, range,
                target -> isEligibleTarget(player, null, player.getWorld(), target, range));
    }

    public static void onTargetDeath(LivingEntity target) {
        if (target == null || !(target.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        ActiveEncasement state = findByTarget(world, target.getUuid());
        if (states == null || state == null) return;
        states.remove(state.ownerId);
        finish(world, state, target, state.detonates && !state.tuning.flag(1 << 7));
        if (states.isEmpty()) ACTIVE.remove(world);
    }

    public static void tick(ServerWorld world) {
        sweepAuxiliaryState(world);
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        if (states == null || states.isEmpty()) return;

        for (ActiveEncasement state : new ArrayList<>(states.values())) {
            if (states.get(state.ownerId) != state) continue;
            LivingEntity owner = resolveLiving(world, state.ownerId);
            LivingEntity target = resolveLiving(world, state.targetId);
            if (owner == null || !owner.isAlive() || owner.isRemoved()) {
                states.remove(state.ownerId);
                finish(world, state, target, false);
                continue;
            }
            if (target == null || target.isRemoved()) {
                if (target != null) {
                    states.remove(state.ownerId);
                    finish(world, state, target, false);
                }
                continue;
            }
            if (!target.isAlive()) {
                states.remove(state.ownerId);
                finish(world, state, target, state.detonates && !state.tuning.flag(1 << 7));
                continue;
            }
            if (world.getTime() >= state.expiresAt) {
                states.remove(state.ownerId);
                finish(world, state, target, state.detonates);
                continue;
            }

            applyPrison(world, target, state);
            if (state.tuning.flag(1 << 6) && !state.tuning.flag(1 << 7)) {
                int steps = Math.min(5, (int) ((state.initialHealth - target.getHealth())
                        / Math.max(1.0F, target.getMaxHealth()) * 5.0F));
                if (steps > state.brittleSteps) {
                    state.expiresAt -= (long) (steps - state.brittleSteps)
                            * state.tuning.integer(
                            NatureSwarmMasteryTuning.Setting.WAX_BRITTLE_DURATION_REDUCTION_TICKS, 10);
                    state.brittleSteps = steps;
                }
            }
            if (state.taunts) tickTaunt(world, owner, target, state);
            tickPrisonEffects(world, target, state);
        }
        if (states.isEmpty()) ACTIVE.remove(world);
    }

    private static boolean hasEntries(Map<?, ?> values) {
        return values != null && !values.isEmpty();
    }

    private static void sweepAuxiliaryState(ServerWorld world) {
        long now = world.getTime();
        Map<UUID, FlashWax> flashes = FLASH_WAX.get(world);
        if (flashes != null) {
            flashes.values().removeIf(flash -> flash.expiresAt < now);
            if (flashes.isEmpty()) FLASH_WAX.remove(world);
        }
        Map<UUID, RefundWindow> refunds = TEMPO_REFUNDS.get(world);
        if (refunds != null) {
            refunds.values().removeIf(window -> now - window.startedAt >= 80);
            if (refunds.isEmpty()) TEMPO_REFUNDS.remove(world);
        }
        Map<UUID, Long> cooldowns = REACTIVE_COOLDOWN.get(world);
        if (cooldowns != null) {
            cooldowns.values().removeIf(expiresAt -> expiresAt <= now);
            if (cooldowns.isEmpty()) REACTIVE_COOLDOWN.remove(world);
        }
    }

    private static void applyPrison(ServerWorld world, LivingEntity target, ActiveEncasement state) {
        int remaining = Math.max(2, (int) (state.expiresAt - world.getTime()) + 2);
        long duration = Math.max(1, state.initialExpiresAt - state.startedAt);
        int amplifier = world.getTime() - state.startedAt >= Math.min(FORMATION_TICKS, duration) ? 1 : 0;
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.WAX_ENCASED), remaining, amplifier,
                false, false, false));
        target.stopUsingItem();
        target.fallDistance = 0.0F;
        target.setVelocity(Vec3d.ZERO);
        target.velocityModified = true;
        target.velocityDirty = true;
        if (target.getPos().squaredDistanceTo(state.anchor) > 0.0004) {
            target.teleport(state.anchor.x, state.anchor.y, state.anchor.z, false);
        }
    }

    private static void tickTaunt(ServerWorld world, LivingEntity owner, LivingEntity prisoner,
                                  ActiveEncasement state) {
        int interval = tauntInterval(Config.uniqueEffects.waxweaver.tauntInterval, state.tuning);
        if ((world.getTime() - state.startedAt) % interval != 0L) return;

        Iterator<Map.Entry<UUID, UUID>> taunted = state.previousTargets.entrySet().iterator();
        while (taunted.hasNext()) {
            Map.Entry<UUID, UUID> entry = taunted.next();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof MobEntity mob) || !mob.isAlive()) {
                taunted.remove();
                continue;
            }
            mob.setTarget(prisoner);
        }

        int maximum = Math.max(0, Config.uniqueEffects.waxweaver.tauntMaxTargets);
        int remaining = maximum - state.previousTargets.size();
        if (remaining <= 0) return;

        double radius = tauntRadius(Config.uniqueEffects.waxweaver.tauntRadius, state.tuning);
        List<MobEntity> candidates = new ArrayList<>(world.getEntitiesByClass(
                MobEntity.class,
                prisoner.getBoundingBox().expand(radius, radius * 0.5, radius),
                mob -> mob != prisoner
                        && mob.isAlive()
                        && !state.previousTargets.containsKey(mob.getUuid())
                        && !isEncased(mob)
                        && HelperMethods.checkAbilityTarget(mob, owner)));
        candidates.sort(Comparator.comparingDouble(prisoner::squaredDistanceTo));
        for (int i = 0; i < Math.min(remaining, candidates.size()); i++) {
            MobEntity mob = candidates.get(i);
            LivingEntity previous = mob.getTarget();
            state.previousTargets.put(mob.getUuid(), previous == null ? null : previous.getUuid());
            mob.setTarget(prisoner);
        }
    }

    private static void finish(ServerWorld world, ActiveEncasement state,
                               LivingEntity prisoner, boolean detonate) {
        discardVisual(world, state.visualId);
        restoreTauntedMobs(world, state, prisoner);
        if (prisoner != null) {
            prisoner.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.WAX_ENCASED));
            prisoner.fallDistance = 0.0F;
            prisoner.velocityModified = true;
        }
        if (!detonate) {
            if (prisoner != null) spawnReleaseEffects(world, prisoner.getPos());
            UniqueAbilityApi.finish(state.execution, NatureSwarmMasteryAbilities.FINISH, 0);
            return;
        }
        Vec3d center = prisoner == null ? state.anchor : prisoner.getPos();
        detonate(world, state, center);
    }

    private static void restoreTauntedMobs(ServerWorld world, ActiveEncasement state,
                                           LivingEntity prisoner) {
        for (Map.Entry<UUID, UUID> entry : state.previousTargets.entrySet()) {
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof MobEntity mob) || !mob.isAlive()) continue;
            if (prisoner != null && mob.getTarget() != prisoner) continue;
            LivingEntity previous = entry.getValue() == null ? null : resolveLiving(world, entry.getValue());
            mob.setTarget(previous != null && previous.isAlive() ? previous : null);
        }
        state.previousTargets.clear();
    }

    private static void detonate(ServerWorld world, ActiveEncasement state, Vec3d center) {
        LivingEntity owner = resolveLiving(world, state.ownerId);
        LivingEntity principal = resolveLiving(world, state.principalId);
        if (owner == null) {
            spawnDetonationEffects(world, center,
                    explosionRadius(Config.uniqueEffects.waxweaver.explosionRadius, state.tuning));
            UniqueAbilityApi.finish(state.execution, NatureSwarmMasteryAbilities.FINISH, 0);
            return;
        }
        if (principal == null) principal = owner;
        LivingEntity damagePrincipal = principal;

        double radius = explosionRadius(Config.uniqueEffects.waxweaver.explosionRadius, state.tuning);
        float damage = state.attack * Math.max(0.0F, Config.uniqueEffects.waxweaver.explosionDamageScaling)
                * explosionDamageMultiplier(state.tuning);
        damage *= state.flashMultiplier;
        damage *= 1.0F + state.brittleSteps
                * (float) state.tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_BRITTLE_DAMAGE_PER_STEP, 0);
        Box box = new Box(center.x - radius, center.y - radius * 0.5, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        int affected = 0;
        int maximum = Math.max(1, state.tuning.integer(NatureSwarmMasteryTuning.Setting.TARGET_CAP, 64));
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                target -> target.isAlive() && isValidTarget(owner, damagePrincipal, target))) {
            if (affected >= maximum) break;
            if (target.getPos().squaredDistanceTo(center) > radius * radius) continue;
            DamageSource source = world.getDamageSources().indirectMagic(owner, damagePrincipal);
            float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                    world, state.stack, target, source, damage);
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] =
                    HelperMethods.damageThroughIframes(target, source, finalDamage));
            if (!damaged[0]) continue;
            affected++;
            UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, NatureSwarmMasteryAbilities.HIT,
                    target, 1, finalDamage);

            int fireTicks = explosionFireTicks(
                    Config.uniqueEffects.waxweaver.explosionIgniteSeconds * 20, state.tuning);
            target.setOnFireFor(Math.max(0, (fireTicks + 19) / 20));
            Vec3d outward = target.getPos().subtract(center);
            if (outward.lengthSquared() > 0.001) {
                double strength = Math.max(0.0, Config.uniqueEffects.waxweaver.explosionKnockback);
                Vec3d direction = outward.normalize();
                target.addVelocity(direction.x * strength, strength * 0.35, direction.z * strength);
                target.velocityModified = true;
            }
        }
        spawnDetonationEffects(world, center, radius);
        UniqueAbilityApi.finish(state.execution, NatureSwarmMasteryAbilities.FINISH, affected);
    }

    public static int detonateRevival(ServerWorld world, LivingEntity owner, ItemStack stack,
                                      NatureSwarmMasteryTuning tuning, UniqueAbilityExecution execution) {
        double radius = Math.max(.5, tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_EMERGENCE_RADIUS, 6));
        int maximum = Math.max(1, tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_EMERGENCE_TARGET_CAP, 16));
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.FIRE, owner, stack,
                1, Config.uniqueEffects.waxweaver.spellScaling)
                * Math.max(0, Config.uniqueEffects.waxweaver.explosionDamageScaling)
                * (float) tuning.get(
                NatureSwarmMasteryTuning.Setting.WAX_EMERGENCE_DAMAGE_MULTIPLIER, 1.25);
        List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(
                LivingEntity.class, owner.getBoundingBox().expand(radius),
                target -> target.isAlive() && target != owner
                        && target.squaredDistanceTo(owner) <= radius * radius
                        && HelperMethods.checkAbilityTarget(target, owner)));
        targets.sort(Comparator.comparingDouble(owner::squaredDistanceTo));
        DamageSource source = world.getDamageSources().indirectMagic(owner, owner);
        int affected = 0;
        for (LivingEntity target : targets) {
            if (affected >= maximum) break;
            float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                    world, stack, target, source, damage);
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] =
                    HelperMethods.damageThroughIframes(target, source, finalDamage));
            if (!damaged[0]) continue;
            affected++;
            UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                    NatureSwarmMasteryAbilities.HIT, target, 1, finalDamage);
            Vec3d outward = target.getPos().subtract(owner.getPos());
            if (outward.lengthSquared() <= .001) continue;
            double strength = Math.max(0, Config.uniqueEffects.waxweaver.explosionKnockback);
            Vec3d direction = outward.normalize();
            target.addVelocity(direction.x * strength, strength * .35, direction.z * strength);
            target.velocityModified = true;
        }
        spawnDetonationEffects(world, owner.getPos(), radius);
        return affected;
    }

    private static boolean isValidTarget(LivingEntity actor, LivingEntity principal,
                                         LivingEntity target) {
        return target != actor
                && target != principal
                && HelperMethods.checkAbilityTarget(target, actor)
                && (principal == actor || HelperMethods.checkAbilityTarget(target, principal));
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)) return amount;
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        if (states == null) return amount;
        ActiveEncasement own = states.get(target.getUuid());
        if (own != null && own.tuning.flag(1 << 23)
                && source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)) {
            amount *= own.tuning.get(
                    NatureSwarmMasteryTuning.Setting.WAX_REFUGE_INCOMING_MULTIPLIER, .85);
        }
        if (source.getAttacker() instanceof LivingEntity attacker) {
            for (ActiveEncasement state : states.values()) {
                if (state.tuning.flag(1 << 7) && state.previousTargets.containsKey(attacker.getUuid())) {
                    amount *= state.tuning.get(
                            NatureSwarmMasteryTuning.Setting.WAX_IRON_OUTGOING_MULTIPLIER, .75);
                    break;
                }
            }
        }
        return amount;
    }

    public static void primeFlashWax(LivingEntity actor, NatureSwarmMasteryTuning tuning) {
        if (actor != null && actor.getWorld() instanceof ServerWorld world && tuning.flag(1 << 15)) {
            FLASH_WAX.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(),
                    new FlashWax(world.getTime() + tuning.integer(
                            NatureSwarmMasteryTuning.Setting.WAX_FLASH_WINDOW_TICKS, 120),
                            (float) tuning.get(NatureSwarmMasteryTuning.Setting.WAX_FLASH_MULTIPLIER, 1.3)));
        }
    }

    public static void reduceActiveCooldown(LivingEntity actor, ItemStack stack,
                                            NatureSwarmMasteryTuning tuning) {
        int refund = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_REFUND_PER_HIT_TICKS, 0);
        if (refund <= 0 || tuning.flag(1 << 17) || !(actor instanceof PlayerEntity player)
                || !(actor.getWorld() instanceof ServerWorld world)) return;
        long now = world.getTime();
        Map<UUID, RefundWindow> windows = TEMPO_REFUNDS.computeIfAbsent(world, ignored -> new HashMap<>());
        RefundWindow window = windows.get(actor.getUuid());
        int limit = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_REFUND_CAP_TICKS, 24);
        int used = window == null || now - window.startedAt >= tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_REFUND_WINDOW_TICKS, 80) ? 0 : window.used;
        int applied = Math.min(refund, Math.max(0, limit - used));
        if (applied <= 0) return;
        int total = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, actor,
                Config.uniqueEffects.waxweaver.activeCooldown);
        int remaining = Math.round(player.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0) * total);
        player.getItemCooldownManager().set(stack.getItem(), Math.max(0, remaining - applied));
        windows.put(actor.getUuid(), new RefundWindow(used == 0 ? now : window.startedAt, used + applied));
    }

    public static boolean tryReactiveShell(ServerWorld world, LivingEntity owner, LivingEntity attacker,
                                           ItemStack stack, NatureSwarmMasteryTuning tuning,
                                           UniqueAbilityExecution execution) {
        if (!tuning.flag(1 << 21) || owner.getHealth() / owner.getMaxHealth()
                >= tuning.get(NatureSwarmMasteryTuning.Setting.WAX_REACTIVE_HEALTH_THRESHOLD, .35)
                || isCasterActive(owner) || !HelperMethods.checkAbilityTarget(attacker, owner)) return false;
        Map<UUID, Long> cooldowns = REACTIVE_COOLDOWN.computeIfAbsent(world, ignored -> new HashMap<>());
        if (world.getTime() < cooldowns.getOrDefault(owner.getUuid(), 0L)) return false;
        int duration = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_REACTIVE_DURATION_TICKS, 40);
        Vec3d anchor = attacker.getPos();
        ActiveEncasement state = new ActiveEncasement(owner.getUuid(), owner.getUuid(), attacker.getUuid(),
                stack.copy(), anchor, world.getTime(), world.getTime() + duration, 0, tuning,
                execution, attacker.getHealth(), 1, false, false);
        WaxweaverWaxVisualEntity visual = new WaxweaverWaxVisualEntity(world,
                WaxweaverWaxVisualEntity.MODE_ENCASE, owner, attacker, anchor.x, anchor.y, anchor.z,
                Math.max(attacker.getWidth(), attacker.getHeight()), duration);
        if (world.spawnEntity(visual)) state.visualId = visual.getUuid();
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(), state);
        cooldowns.put(owner.getUuid(), world.getTime()
                + tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_REACTIVE_COOLDOWN_TICKS, 200));
        applyPrison(world, attacker, state);
        spawnEncasementEffects(world, attacker);
        return true;
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, ActiveEncasement> states = ACTIVE.remove(world);
        if (states != null) {
            for (ActiveEncasement state : new ArrayList<>(states.values())) {
                LivingEntity prisoner = resolveLiving(world, state.targetId);
                finish(world, state, prisoner, false);
            }
        }
        FLASH_WAX.remove(world);
        TEMPO_REFUNDS.remove(world);
        REACTIVE_COOLDOWN.remove(world);
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>(ACTIVE.keySet());
        worlds.addAll(FLASH_WAX.keySet());
        worlds.addAll(TEMPO_REFUNDS.keySet());
        worlds.addAll(REACTIVE_COOLDOWN.keySet());
        for (ServerWorld world : worlds) clear(world);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        UUID actorId = actor.getUuid();
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        if (states != null) {
            for (ActiveEncasement state : new ArrayList<>(states.values())) {
                if (!state.ownerId.equals(actorId) && !state.targetId.equals(actorId)) continue;
                states.remove(state.ownerId);
                LivingEntity prisoner = state.targetId.equals(actorId)
                        ? actor : resolveLiving(world, state.targetId);
                finish(world, state, prisoner, false);
            }
            if (states.isEmpty()) ACTIVE.remove(world);
        }
        removeOwner(FLASH_WAX, world, actorId);
        removeOwner(TEMPO_REFUNDS, world, actorId);
        removeOwner(REACTIVE_COOLDOWN, world, actorId);
    }

    private static <T> void removeOwner(Map<ServerWorld, Map<UUID, T>> states,
                                        ServerWorld world, UUID ownerId) {
        Map<UUID, T> values = states.get(world);
        if (values == null) return;
        values.remove(ownerId);
        if (values.isEmpty()) states.remove(world);
    }

    private static float consumeFlashWax(ServerWorld world, UUID ownerId) {
        Map<UUID, FlashWax> flashes = FLASH_WAX.get(world);
        if (flashes == null) return 1;
        FlashWax flash = flashes.remove(ownerId);
        if (flashes.isEmpty()) FLASH_WAX.remove(world);
        return flash != null && flash.expiresAt >= world.getTime() ? flash.multiplier : 1;
    }

    private static LivingEntity resolveTarget(WeaponAbilityContext context) {
        if (context == null || context.actor() == null || context.world() == null) return null;
        if (context.activationSource() == WeaponAbilityActivationSource.PLAYER
                && context.actor() instanceof PlayerEntity player) {
            return findPlayerTarget(player);
        }
        LivingEntity direct = context.target();
        if (isEligibleTarget(context, direct)) return direct;

        double range = Math.min(PLAYER_FALLBACK_RANGE,
                Math.max(1.0, Config.uniqueEffects.waxweaver.targetRange + MAX_MASTERY_RANGE_BONUS));
        Box box = context.actor().getBoundingBox().expand(range);
        return context.world().getEntitiesByClass(LivingEntity.class, box,
                        target -> isEligibleTarget(context, target)
                                && context.actor().squaredDistanceTo(target) <= range * range)
                .stream()
                .min(Comparator.comparingDouble(context.actor()::squaredDistanceTo))
                .orElse(null);
    }

    private static boolean isEligibleTarget(WeaponAbilityContext context, LivingEntity target) {
        return context != null && isEligibleTarget(
                context.actor(), context.sourcePlayer(), context.world(), target,
                Config.uniqueEffects.waxweaver.targetRange + MAX_MASTERY_RANGE_BONUS);
    }

    private static boolean isEligibleTarget(LivingEntity actor, LivingEntity sourcePlayer,
                                            net.minecraft.world.World world, LivingEntity target,
                                            double allowedRange) {
        if (actor == null || world == null || target == null || !target.isAlive() || target.isRemoved()
                || target == actor || target.getWorld() != world
                || isEncased(target)) return false;
        double range = Math.max(1.0, allowedRange);
        if (actor.squaredDistanceTo(target) > range * range
                || target.getWidth() > Math.max(0.1F, Config.uniqueEffects.waxweaver.maximumTargetWidth)
                || target.getHeight() > Math.max(0.1F, Config.uniqueEffects.waxweaver.maximumTargetHeight)
                || !HelperMethods.checkAbilityTarget(target, actor)) return false;
        return sourcePlayer == null
                || target != sourcePlayer
                && HelperMethods.checkAbilityTarget(target, sourcePlayer);
    }

    private static boolean isCasterActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return false;
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        return states != null && states.containsKey(actor.getUuid());
    }

    private static ActiveEncasement findByTarget(ServerWorld world, UUID targetId) {
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        if (states == null) return null;
        for (ActiveEncasement state : states.values()) {
            if (state.targetId.equals(targetId)) return state;
        }
        return null;
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) return;
        Entity visual = world.getEntity(visualId);
        if (visual != null) visual.discard();
    }

    private static void tickPrisonEffects(ServerWorld world, LivingEntity prisoner,
                                          ActiveEncasement state) {
        long age = world.getTime() - state.startedAt;
        if (age == FORMATION_TICKS) {
            world.spawnParticles(ParticleTypes.LANDING_HONEY,
                    prisoner.getX(), prisoner.getBodyY(0.52), prisoner.getZ(),
                    22, prisoner.getWidth() * 0.58, prisoner.getHeight() * 0.42,
                    prisoner.getWidth() * 0.58, 0.025);
            world.playSound(null, prisoner.getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_PLACE,
                    SoundCategory.PLAYERS, 1.15F, 0.46F);
            world.playSound(null, prisoner.getBlockPos(), SoundEvents.BLOCK_CANDLE_PLACE,
                    SoundCategory.PLAYERS, 0.9F, 0.52F);
        } else if (age % 6L == 0L) {
            world.spawnParticles(ParticleTypes.FALLING_HONEY,
                    prisoner.getX(), prisoner.getBodyY(0.68), prisoner.getZ(),
                    2, prisoner.getWidth() * 0.38, prisoner.getHeight() * 0.26,
                    prisoner.getWidth() * 0.38, 0.012);
        }
        long remaining = state.expiresAt - world.getTime();
        if (age >= FORMATION_TICKS && age % 2L == 0L) {
            int count = remaining <= 20L ? 2 : 1;
            world.spawnParticles(ParticleTypes.SMALL_FLAME,
                    prisoner.getX(), prisoner.getY() + prisoner.getHeight() + 0.46, prisoner.getZ(),
                    count, 0.025, 0.025, 0.025, 0.008);
        }
        if (remaining <= 20L) {
            if (age % 3L == 0L) {
                world.spawnParticles(ParticleTypes.SMALL_FLAME,
                        prisoner.getX(), prisoner.getBodyY(0.7), prisoner.getZ(),
                        2, prisoner.getWidth() * 0.38, prisoner.getHeight() * 0.26,
                        prisoner.getWidth() * 0.38, 0.012);
            }
            if (age % 5L == 0L) {
                world.spawnParticles(ParticleTypes.LAVA,
                        prisoner.getX(), prisoner.getBodyY(0.58), prisoner.getZ(),
                        2, prisoner.getWidth() * 0.34, prisoner.getHeight() * 0.22,
                        prisoner.getWidth() * 0.34, 0.025);
            }
            if (age % 7L == 0L) {
                world.spawnParticles(ParticleTypes.SMOKE,
                        prisoner.getX(), prisoner.getY() + prisoner.getHeight() + 0.18, prisoner.getZ(),
                        1, 0.1, 0.05, 0.1, 0.015);
            }
        }
    }

    private static void spawnEncasementEffects(ServerWorld world, LivingEntity target) {
        world.spawnParticles(ParticleTypes.WAX_ON, target.getX(), target.getBodyY(0.45), target.getZ(),
                8, target.getWidth() * 0.5, target.getHeight() * 0.42,
                target.getWidth() * 0.5, 0.025);
        world.spawnParticles(ParticleTypes.LANDING_HONEY, target.getX(), target.getY() + 0.08, target.getZ(),
                28, target.getWidth() * 0.72, 0.08, target.getWidth() * 0.72, 0.025);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_PLACE,
                SoundCategory.PLAYERS, 1.0F, 0.62F);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_CANDLE_PLACE,
                SoundCategory.PLAYERS, 0.85F, 0.72F);
    }

    private static void spawnReleaseEffects(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.WAX_OFF, center.x, center.y + 0.8, center.z,
                18, 0.45, 0.65, 0.45, 0.025);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_CANDLE_EXTINGUISH,
                SoundCategory.PLAYERS, 0.8F, 0.8F);
    }

    private static void spawnDetonationEffects(ServerWorld world, Vec3d center, double radius) {
        radius = Math.max(.5, radius);
        world.spawnEntity(new WaxweaverWaxVisualEntity(
                world, WaxweaverWaxVisualEntity.MODE_DETONATION, null, null,
                center.x, center.y + 0.05, center.z, (float) radius, 22));
        world.spawnParticles(ParticleTypes.FLASH, center.x, center.y + 0.75, center.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.7, center.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 0.65, center.z,
                42, radius * 0.55, 0.8, radius * 0.55, 0.075);
        world.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 0.5, center.z,
                28, radius * 0.42, 0.65, radius * 0.42, 0.095);
        world.spawnParticles(ParticleTypes.WAX_OFF, center.x, center.y + 0.8, center.z,
                16, radius * 0.62, 0.9, radius * 0.62, 0.055);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.55, center.z,
                18, radius * 0.35, 0.4, radius * 0.35, 0.04);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 1.15F, 0.72F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_HONEY_BLOCK_BREAK,
                SoundCategory.PLAYERS, 1.25F, 0.55F);
        world.playSound(null, center.x, center.y, center.z,
                SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 0.9F, 0.7F);
    }

    private static final class ActiveEncasement {
        private final UUID ownerId;
        private final UUID principalId;
        private final UUID targetId;
        private final ItemStack stack;
        private final Vec3d anchor;
        private final long startedAt;
        private final long initialExpiresAt;
        private long expiresAt;
        private final float attack;
        private final NatureSwarmMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final float initialHealth;
        private final float flashMultiplier;
        private final boolean taunts;
        private final boolean detonates;
        private int brittleSteps;
        private final Map<UUID, UUID> previousTargets = new HashMap<>();
        private UUID visualId;

        private ActiveEncasement(UUID ownerId, UUID principalId, UUID targetId,
                                 ItemStack stack, Vec3d anchor, long startedAt,
                                 long expiresAt, float attack, NatureSwarmMasteryTuning tuning,
                                 UniqueAbilityExecution execution, float initialHealth,
                                 float flashMultiplier, boolean taunts, boolean detonates) {
            this.ownerId = ownerId;
            this.principalId = principalId;
            this.targetId = targetId;
            this.stack = stack;
            this.anchor = anchor;
            this.startedAt = startedAt;
            this.initialExpiresAt = expiresAt;
            this.expiresAt = expiresAt;
            this.attack = attack;
            this.tuning = tuning;
            this.execution = execution;
            this.initialHealth = initialHealth;
            this.flashMultiplier = flashMultiplier;
            this.taunts = taunts;
            this.detonates = detonates;
        }
    }

    private record RefundWindow(long startedAt, int used) {
    }

    private record FlashWax(long expiresAt, float multiplier) {
    }
}
