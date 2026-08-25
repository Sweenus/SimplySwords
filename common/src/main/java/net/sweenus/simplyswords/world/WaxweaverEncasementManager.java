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
import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase7UniqueAbilities;
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
import java.util.UUID;

public final class WaxweaverEncasementManager {

    public static final int FORMATION_TICKS = 10;
    private static final double PLAYER_FALLBACK_RANGE = 8.0;
    private static final Map<ServerWorld, Map<UUID, ActiveEncasement>> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> FLASH_WAX = new HashMap<>();
    private static final Map<UUID, RefundWindow> TEMPO_REFUNDS = new HashMap<>();
    private static final Map<UUID, Long> REACTIVE_COOLDOWN = new HashMap<>();

    private WaxweaverEncasementManager() {
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
        UniqueAbilityExecution execution = Phase7CombatManager.beginActive(
                Phase7UniqueAbilities.WAXWEAVER_PRISON, context, Config.uniqueEffects.waxweaver.activeCooldown);
        Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
        double targetRange = tuning.get(Phase7AbilityTuning.Setting.RANGE,
                Config.uniqueEffects.waxweaver.targetRange);
        if (actor.squaredDistanceTo(target) > targetRange * targetRange) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }
        UUID principalId = context.sourcePlayer() == null ? actor.getUuid() : context.sourcePlayer().getUuid();
        float attack = HelperMethods.abilityScaledDamage(SpellScalingProfile.FIRE, actor, context.stack(),
                1.0F, Config.uniqueEffects.waxweaver.spellScaling);
        int duration = Math.max(1, tuning.integer(Phase7AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.waxweaver.encasementDuration));
        Vec3d anchor = target.getPos();

        target.stopRiding();
        ActiveEncasement state = new ActiveEncasement(
                actor.getUuid(), principalId, target.getUuid(), context.stack().copy(),
                anchor, world.getTime(), world.getTime() + duration, attack, target.hasNoGravity(),
                target instanceof MobEntity mob && mob.isAiDisabled(), tuning, execution,
                target.getHealth(), true, true);
        WaxweaverWaxVisualEntity visual = new WaxweaverWaxVisualEntity(
                world, WaxweaverWaxVisualEntity.MODE_ENCASE, actor, target,
                anchor.x, anchor.y, anchor.z, Math.max(target.getWidth(), target.getHeight()), duration);
        if (world.spawnEntity(visual)) state.visualId = visual.getUuid();

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), state);
        UniqueAbilityApi.start(execution);
        int absorption = tuning.integer(Phase7AbilityTuning.Setting.ABSORPTION, 0);
        if (absorption > 0 && tuning.flag(1 << 20)) {
            actor.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.ABSORPTION,
                    tuning.integer(Phase7AbilityTuning.Setting.STATUS_DURATION_TICKS, 60),
                    Math.max(0, absorption / 4 - 1), false, true, true));
        }
        applyPrison(world, target, state);
        spawnEncasementEffects(world, target);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        return states != null && !states.isEmpty();
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
        int configuredDuration = Math.max(1, Config.uniqueEffects.waxweaver.encasementDuration);
        int elapsed = configuredDuration + 2 - effect.getDuration();
        return elapsed >= Math.min(FORMATION_TICKS, configuredDuration);
    }

    public static LivingEntity findPlayerTarget(PlayerEntity player) {
        if (player == null || !player.isAlive()) return null;
        double range = Math.max(1.0, Config.uniqueEffects.waxweaver.targetRange + 3.0);
        return StealSwordItem.findLenientTarget(player, range,
                target -> isEligibleTarget(player, null, player.getWorld(), target));
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
                states.remove(state.ownerId);
                finish(world, state, null, false);
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
                            * state.tuning.integer(Phase7AbilityTuning.Setting.INTERVAL_TICKS, 10);
                    state.brittleSteps = steps;
                }
            }
            if (state.taunts) tickTaunt(world, owner, target, state);
            tickPrisonEffects(world, target, state);
        }
        if (states.isEmpty()) ACTIVE.remove(world);
    }

    private static void applyPrison(ServerWorld world, LivingEntity target, ActiveEncasement state) {
        int remaining = Math.max(2, (int) (state.expiresAt - world.getTime()) + 2);
        target.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.WAX_ENCASED), remaining, 0,
                false, false, false));
        target.stopUsingItem();
        if (target instanceof MobEntity mob) mob.setAiDisabled(true);
        target.setNoGravity(true);
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
        int interval = Math.max(1, state.tuning.integer(Phase7AbilityTuning.Setting.INTERVAL_TICKS,
                Config.uniqueEffects.waxweaver.tauntInterval));
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

        int maximum = Math.max(0, state.tuning.integer(Phase7AbilityTuning.Setting.TARGET_CAP,
                Config.uniqueEffects.waxweaver.tauntMaxTargets));
        int remaining = maximum - state.previousTargets.size();
        if (remaining <= 0) return;

        double radius = Math.max(1.0, state.tuning.get(Phase7AbilityTuning.Setting.WIDTH,
                Config.uniqueEffects.waxweaver.tauntRadius));
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
            if (prisoner instanceof MobEntity mob) mob.setAiDisabled(state.hadAiDisabled);
            prisoner.setNoGravity(state.hadNoGravity);
            prisoner.fallDistance = 0.0F;
            prisoner.velocityModified = true;
        }
        if (!detonate) {
            if (prisoner != null) spawnReleaseEffects(world, prisoner.getPos());
            UniqueAbilityApi.finish(state.execution, Phase7UniqueAbilities.FINISH, 0);
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
            spawnDetonationEffects(world, center);
            return;
        }
        if (principal == null) principal = owner;
        LivingEntity damagePrincipal = principal;

        double radius = Math.max(0.5, state.tuning.get(Phase7AbilityTuning.Setting.RADIUS,
                Config.uniqueEffects.waxweaver.explosionRadius));
        float damage = state.attack * Math.max(0.0F, Config.uniqueEffects.waxweaver.explosionDamageScaling)
                * (float) state.tuning.get(Phase7AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
        damage *= state.flashMultiplier;
        damage *= 1.0F + state.brittleSteps
                * (float) state.tuning.get(Phase7AbilityTuning.Setting.PER_STACK_MULTIPLIER, 0);
        Box box = new Box(center.x - radius, center.y - radius * 0.5, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        int affected = 0;
        int maximum = Math.max(1, state.tuning.integer(Phase7AbilityTuning.Setting.TARGET_CAP, 64));
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
            UniqueAbilityApi.emit(state.execution, UniqueAbilityPhase.HIT, Phase7UniqueAbilities.HIT,
                    target, 1, finalDamage);

            int fireTicks = state.tuning.integer(Phase7AbilityTuning.Setting.FIRE_TICKS,
                    Config.uniqueEffects.waxweaver.explosionIgniteSeconds * 20);
            target.setOnFireFor(Math.max(0, (fireTicks + 19) / 20));
            Vec3d outward = target.getPos().subtract(center);
            if (outward.lengthSquared() > 0.001) {
                double strength = Math.max(0.0, Config.uniqueEffects.waxweaver.explosionKnockback);
                Vec3d direction = outward.normalize();
                target.addVelocity(direction.x * strength, strength * 0.35, direction.z * strength);
                target.velocityModified = true;
            }
        }
        spawnDetonationEffects(world, center);
        UniqueAbilityApi.finish(state.execution, Phase7UniqueAbilities.FINISH, affected);
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
            amount *= own.tuning.get(Phase7AbilityTuning.Setting.INCOMING_MULTIPLIER, .85);
        }
        if (source.getAttacker() instanceof LivingEntity attacker) {
            for (ActiveEncasement state : states.values()) {
                if (state.tuning.flag(1 << 7) && state.previousTargets.containsKey(attacker.getUuid())) {
                    amount *= state.tuning.get(Phase7AbilityTuning.Setting.OUTGOING_MULTIPLIER, .75);
                    break;
                }
            }
        }
        return amount;
    }

    public static void primeFlashWax(LivingEntity actor, Phase7AbilityTuning tuning) {
        if (actor != null && tuning.flag(1 << 15)) {
            FLASH_WAX.put(actor.getUuid(), actor.getWorld().getTime()
                    + tuning.integer(Phase7AbilityTuning.Setting.LOCKOUT_TICKS, 120));
        }
    }

    public static void reduceActiveCooldown(LivingEntity actor, ItemStack stack,
                                            Phase7AbilityTuning tuning) {
        int refund = tuning.integer(Phase7AbilityTuning.Setting.REFUND_TICKS, 0);
        if (refund <= 0 || !(actor instanceof PlayerEntity player)) return;
        long now = actor.getWorld().getTime();
        RefundWindow window = TEMPO_REFUNDS.get(actor.getUuid());
        int limit = tuning.integer(Phase7AbilityTuning.Setting.STACK_CAP, 24);
        int used = window == null || now - window.startedAt > tuning.integer(
                Phase7AbilityTuning.Setting.LOCKOUT_TICKS, 80) ? 0 : window.used;
        int applied = Math.min(refund, Math.max(0, limit - used));
        if (applied <= 0) return;
        int total = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, actor,
                Config.uniqueEffects.waxweaver.activeCooldown);
        int remaining = Math.round(player.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0) * total);
        player.getItemCooldownManager().set(stack.getItem(), Math.max(0, remaining - applied));
        TEMPO_REFUNDS.put(actor.getUuid(), new RefundWindow(used == 0 ? now : window.startedAt, used + applied));
    }

    public static boolean tryReactiveShell(ServerWorld world, LivingEntity owner, LivingEntity attacker,
                                           ItemStack stack, Phase7AbilityTuning tuning,
                                           UniqueAbilityExecution execution) {
        if (!tuning.flag(1 << 21) || owner.getHealth() / owner.getMaxHealth()
                >= tuning.get(Phase7AbilityTuning.Setting.CHANCE, 35) / 100.0
                || world.getTime() < REACTIVE_COOLDOWN.getOrDefault(owner.getUuid(), 0L)
                || isCasterActive(owner) || !HelperMethods.checkAbilityTarget(attacker, owner)) return false;
        int duration = tuning.integer(Phase7AbilityTuning.Setting.INTERVAL_TICKS, 40);
        Vec3d anchor = attacker.getPos();
        ActiveEncasement state = new ActiveEncasement(owner.getUuid(), owner.getUuid(), attacker.getUuid(),
                stack.copy(), anchor, world.getTime(), world.getTime() + duration, 0,
                attacker.hasNoGravity(), attacker instanceof MobEntity mob && mob.isAiDisabled(),
                tuning, execution, attacker.getHealth(), false, false);
        WaxweaverWaxVisualEntity visual = new WaxweaverWaxVisualEntity(world,
                WaxweaverWaxVisualEntity.MODE_ENCASE, owner, attacker, anchor.x, anchor.y, anchor.z,
                Math.max(attacker.getWidth(), attacker.getHeight()), duration);
        if (world.spawnEntity(visual)) state.visualId = visual.getUuid();
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(owner.getUuid(), state);
        REACTIVE_COOLDOWN.put(owner.getUuid(), world.getTime()
                + tuning.integer(Phase7AbilityTuning.Setting.LOCKOUT_TICKS, 200));
        applyPrison(world, attacker, state);
        spawnEncasementEffects(world, attacker);
        return true;
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
                Math.max(1.0, Config.uniqueEffects.waxweaver.targetRange));
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
                context.actor(), context.sourcePlayer(), context.world(), target);
    }

    private static boolean isEligibleTarget(LivingEntity actor, LivingEntity sourcePlayer,
                                            net.minecraft.world.World world, LivingEntity target) {
        if (actor == null || world == null || target == null || !target.isAlive() || target.isRemoved()
                || target == actor || target.getWorld() != world
                || isEncased(target)) return false;
        double range = Math.max(1.0, Config.uniqueEffects.waxweaver.targetRange);
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

    private static void spawnDetonationEffects(ServerWorld world, Vec3d center) {
        double radius = Math.max(0.5, Config.uniqueEffects.waxweaver.explosionRadius);
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
        private long expiresAt;
        private final float attack;
        private final boolean hadNoGravity;
        private final boolean hadAiDisabled;
        private final Phase7AbilityTuning tuning;
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
                                 long expiresAt, float attack, boolean hadNoGravity,
                                 boolean hadAiDisabled, Phase7AbilityTuning tuning,
                                 UniqueAbilityExecution execution, float initialHealth,
                                 boolean taunts, boolean detonates) {
            this.ownerId = ownerId;
            this.principalId = principalId;
            this.targetId = targetId;
            this.stack = stack;
            this.anchor = anchor;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.attack = attack;
            this.hadNoGravity = hadNoGravity;
            this.hadAiDisabled = hadAiDisabled;
            this.tuning = tuning;
            this.execution = execution;
            this.initialHealth = initialHealth;
            Long flashUntil = detonates ? FLASH_WAX.remove(ownerId) : null;
            this.flashMultiplier = flashUntil != null && flashUntil >= startedAt
                    ? (float) tuning.get(Phase7AbilityTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1.3) : 1.0F;
            this.taunts = taunts;
            this.detonates = detonates;
        }
    }

    private record RefundWindow(long startedAt, int used) {
    }
}
