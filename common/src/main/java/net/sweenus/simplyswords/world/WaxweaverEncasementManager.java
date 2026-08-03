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
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
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
        UUID principalId = context.sourcePlayer() == null ? actor.getUuid() : context.sourcePlayer().getUuid();
        float attack = HelperMethods.abilityScaledDamage(SpellScalingProfile.FIRE, actor, context.stack(),
                1.0F, Config.uniqueEffects.waxweaver.spellScaling);
        int duration = Math.max(1, Config.uniqueEffects.waxweaver.encasementDuration);
        Vec3d anchor = target.getPos();

        target.stopRiding();
        ActiveEncasement state = new ActiveEncasement(
                actor.getUuid(), principalId, target.getUuid(), context.stack().copy(),
                anchor, world.getTime(), world.getTime() + duration, attack, target.hasNoGravity(),
                target instanceof MobEntity mob && mob.isAiDisabled());
        WaxweaverWaxVisualEntity visual = new WaxweaverWaxVisualEntity(
                world, WaxweaverWaxVisualEntity.MODE_ENCASE, actor, target,
                anchor.x, anchor.y, anchor.z, Math.max(target.getWidth(), target.getHeight()), duration);
        if (world.spawnEntity(visual)) state.visualId = visual.getUuid();

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), state);
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
        double range = Math.max(1.0, Config.uniqueEffects.waxweaver.targetRange);
        return StealSwordItem.findLenientTarget(player, range,
                target -> isEligibleTarget(player, null, player.getWorld(), target));
    }

    public static void onTargetDeath(LivingEntity target) {
        if (target == null || !(target.getWorld() instanceof ServerWorld world)) return;
        Map<UUID, ActiveEncasement> states = ACTIVE.get(world);
        ActiveEncasement state = findByTarget(world, target.getUuid());
        if (states == null || state == null) return;
        states.remove(state.ownerId);
        finish(world, state, target, true);
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
                finish(world, state, target, true);
                continue;
            }
            if (world.getTime() >= state.expiresAt) {
                states.remove(state.ownerId);
                finish(world, state, target, true);
                continue;
            }

            applyPrison(world, target, state);
            tickTaunt(world, owner, target, state);
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
        int interval = Math.max(1, Config.uniqueEffects.waxweaver.tauntInterval);
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

        double radius = Math.max(1.0, Config.uniqueEffects.waxweaver.tauntRadius);
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

        double radius = Math.max(0.5, Config.uniqueEffects.waxweaver.explosionRadius);
        float damage = state.attack * Math.max(0.0F, Config.uniqueEffects.waxweaver.explosionDamageScaling);
        Box box = new Box(center.x - radius, center.y - radius * 0.5, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                target -> target.isAlive() && isValidTarget(owner, damagePrincipal, target))) {
            if (target.getPos().squaredDistanceTo(center) > radius * radius) continue;
            DamageSource source = world.getDamageSources().indirectMagic(owner, damagePrincipal);
            float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                    world, state.stack, target, source, damage);
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] =
                    HelperMethods.damageThroughIframes(target, source, finalDamage));
            if (!damaged[0]) continue;

            target.setOnFireFor(Math.max(0, Config.uniqueEffects.waxweaver.explosionIgniteSeconds));
            Vec3d outward = target.getPos().subtract(center);
            if (outward.lengthSquared() > 0.001) {
                double strength = Math.max(0.0, Config.uniqueEffects.waxweaver.explosionKnockback);
                Vec3d direction = outward.normalize();
                target.addVelocity(direction.x * strength, strength * 0.35, direction.z * strength);
                target.velocityModified = true;
            }
        }
        spawnDetonationEffects(world, center);
    }

    private static boolean isValidTarget(LivingEntity actor, LivingEntity principal,
                                         LivingEntity target) {
        return target != actor
                && target != principal
                && HelperMethods.checkAbilityTarget(target, actor)
                && (principal == actor || HelperMethods.checkAbilityTarget(target, principal));
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
        private final long expiresAt;
        private final float attack;
        private final boolean hadNoGravity;
        private final boolean hadAiDisabled;
        private final Map<UUID, UUID> previousTargets = new HashMap<>();
        private UUID visualId;

        private ActiveEncasement(UUID ownerId, UUID principalId, UUID targetId,
                                 ItemStack stack, Vec3d anchor, long startedAt,
                                 long expiresAt, float attack, boolean hadNoGravity,
                                 boolean hadAiDisabled) {
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
        }
    }
}
