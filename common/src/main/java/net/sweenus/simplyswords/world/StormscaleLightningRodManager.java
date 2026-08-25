package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.Phase3AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase3UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.api.render.LightningPhenomenonShape;
import net.sweenus.simplyswords.api.render.LightningPhenomenonStyle;
import net.sweenus.simplyswords.api.render.SurfaceDischargeStyle;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.StormscaleRodVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StormscaleLightningRodManager {

    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 32;
    private static final String ROD_VISUAL_TAG = "simplyswords_stormscale_rod_visual";
    private static final int PRIMARY_COLOR = 0x42C8FF;
    private static final int CORE_COLOR = 0xF3FFFF;
    private static final float ROD_LINK_OFFSET = 1.45F;
    private static final Map<ServerWorld, Map<UUID, ActiveRod>> ACTIVE = new HashMap<>();

    private StormscaleLightningRodManager() {
    }

    public static boolean canStart(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.STORMSCALE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || !AwakeningApi.isAbilityUnlocked(context.stack())) {
            return false;
        }
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        ItemStack held = context.actor().getStackInHand(hand);
        if (!held.isOf(ItemsRegistry.STORMSCALE.get()) || !AwakeningApi.isAbilityUnlocked(held)) return false;
        ActiveRod rod = active(context.world(), context.actor().getUuid());
        return rod == null ? resolveAnchor(context) != null : canReactivate(context, rod);
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canStart(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        ActiveRod existing = active(world, actor.getUuid());
        if (existing != null) return reactivate(context, existing);

        UniqueAbilityExecution execution = UniqueAbilityApi.begin(Phase3UniqueAbilities.STORMSCALE_ROD,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(Phase3UniqueAbilities.TUNING, Phase3AbilityTuning.EMPTY)
                        .set(Phase3UniqueAbilities.COOLDOWN_TICKS, Config.uniqueEffects.stormscale.cooldown));
        Phase3AbilityTuning tuning = Phase3UniqueAbilities.tuning(execution);
        Vec3d anchor = resolveAnchor(context, tuning.get(Phase3AbilityTuning.Setting.RANGE,
                Config.uniqueEffects.stormscale.targetingRange));
        if (anchor == null) return false;
        int duration = Math.max(1, tuning.integer(Phase3AbilityTuning.Setting.ROD_DURATION_TICKS,
                Config.uniqueEffects.stormscale.duration));
        int travelTicks = Math.max(1, tuning.integer(Phase3AbilityTuning.Setting.TRAVEL_TICKS,
                Config.uniqueEffects.stormscale.energyTravelTicks));
        float radius = (float) Math.max(0.1, tuning.get(Phase3AbilityTuning.Setting.RADIUS,
                Config.uniqueEffects.stormscale.pulseRadius));
        float growthPerHit = (float) Math.max(0, tuning.get(Phase3AbilityTuning.Setting.GROWTH_PER_HIT,
                Config.uniqueEffects.stormscale.pulseGrowthPerHit));
        float maximumGrowth = (float) Math.max(0, tuning.get(Phase3AbilityTuning.Setting.GROWTH_CAP,
                Config.uniqueEffects.stormscale.maximumPulseGrowth));
        double pullStrength = tuning.get(Phase3AbilityTuning.Setting.PULL_STRENGTH,
                Config.uniqueEffects.stormscale.pulsePullStrength);
        Hand hand = context.hand() == null ? Hand.MAIN_HAND : context.hand();
        long now = world.getTime();

        StormscaleRodVisualEntity rod = new StormscaleRodVisualEntity(
                world, anchor.x, anchor.y, anchor.z, radius,
                duration + travelTicks + 20, actor.getYaw());
        rod.addCommandTag(ROD_VISUAL_TAG);
        world.spawnEntity(rod);
        UUID tetherVisualId = spawnEnergyLink(world, actor, rod, duration + travelTicks + 20);

        ActiveRod activeRod = new ActiveRod(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.stack().copy(),
                hand,
                anchor,
                rod.getUuid(),
                tetherVisualId,
                now + duration,
                travelTicks,
                radius,
                growthPerHit,
                maximumGrowth,
                pullStrength,
                Math.max(1.0, tuning.get(Phase3AbilityTuning.Setting.TETHER_RANGE,
                        Config.uniqueEffects.stormscale.maxTetherDistance)),
                HelperMethods.abilityScaledDamage(
                        SpellScalingProfile.LIGHTNING,
                        actor,
                        context.stack(),
                        Config.uniqueEffects.stormscale.pulseDamageScaling,
                        Config.uniqueEffects.stormscale.pulseSpellScaling
                ) * (float) tuning.get(Phase3AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1),
                tuning.has(Phase3AbilityTuning.Setting.TARGET_CAP)
                        ? tuning.integer(Phase3AbilityTuning.Setting.TARGET_CAP, 64) : Integer.MAX_VALUE,
                tuning.integer(Phase3AbilityTuning.Setting.SLOW_DURATION_TICKS, 0),
                tuning.integer(Phase3AbilityTuning.Setting.MODE, 0),
                tuning,
                execution
        );
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), activeRod);
        if ((activeRod.mode & 1) != 0) {
            pulse(world, actor, activeRod, radius, activeRod.baseDamage * .7F);
        }
        spawnActivationEffects(world, actor, anchor);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveRod> active = ACTIVE.get(world);
        return active != null && !active.isEmpty() || world.getTime() % 40L == 0L;
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveRod> active = ACTIVE.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    public static boolean hasPendingReactivation(ServerWorld world, LivingEntity actor) {
        ActiveRod rod = world == null || actor == null ? null : active(world, actor.getUuid());
        return rod != null && ((rod.mode & 2) != 0 && !rod.repositioned
                || (rod.mode & 512) != 0 || (rod.mode & 4096) != 0);
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack, LivingEntity reportedAttacker) {
        onMeleeHit(world, stack, reportedAttacker, null);
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack, LivingEntity reportedAttacker,
                                  LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.STORMSCALE.get())
                || reportedAttacker == null) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        if (actor == null || !actor.isAlive() || actor.getWorld() != world) {
            return;
        }

        Map<UUID, ActiveRod> active = ACTIVE.get(world);
        ActiveRod rod = active == null ? null : active.get(actor.getUuid());
        long now = world.getTime();
        if (rod == null || now >= rod.expiresAt || rod.lastLaunchTick == now) {
            return;
        }

        Hand hitHand = resolveHitHand(actor, stack);
        if (hitHand == null || hitHand != rod.hand || !isStillHolding(actor, rod)) {
            return;
        }

        Entity visual = world.getEntity(rod.rodVisualId);
        if (!(visual instanceof StormscaleRodVisualEntity rodVisual)) {
            return;
        }

        rod.lastLaunchTick = now;
        int travelTicks = rod.travelTicks;
        if (target != null && rod.conductive.getOrDefault(target.getUuid(), 0L) >= now) {
            travelTicks = Math.max(1, travelTicks - 4);
        }
        if ((rod.mode & 256) != 0) {
            rod.arrivedHits++;
            float growth = Math.min(rod.maximumGrowth, rod.arrivedHits * rod.growthPerHit);
            pulse(world, actor, rod, Math.min(4.5F, rod.baseRadius * (1 + growth)),
                    rod.baseDamage * (1 + growth) * .55F);
        } else {
            UUID pulseVisualId = spawnTravellingPulse(world, actor, rodVisual, travelTicks);
            rod.pending.add(new PendingPulse(now + travelTicks, pulseVisualId, 1, 1));
        }
        if ((rod.mode & 16) != 0 && actor.fallDistance > 0 && !actor.isOnGround()
                && now >= rod.doubleChargeReady) {
            rod.doubleChargeReady = now + 10;
            rod.pending.add(new PendingPulse(now + travelTicks + 1, null, .5F, .5F));
        }
        spawnLaunchEffects(world, actor);
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveRod> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            if (world.getTime() % 40L == 0L) {
                purgeOrphans(world);
            }
            return;
        }

        Iterator<ActiveRod> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveRod rod = iterator.next();
            if (tickRod(world, rod)) {
                iterator.remove();
            }
        }
        if (active.isEmpty()) {
            ACTIVE.remove(world);
            purgeOrphans(world);
        }
    }

    private static boolean tickRod(ServerWorld world, ActiveRod rod) {
        Entity actorEntity = world.getEntity(rod.actorId);
        Entity visualEntity = world.getEntity(rod.rodVisualId);
        if (!(actorEntity instanceof LivingEntity actor)
                || !actor.isAlive()
                || actor.getWorld() != world
                || !(visualEntity instanceof StormscaleRodVisualEntity rodVisual)
                || !isStillHolding(actor, rod)
                || actor.squaredDistanceTo(rod.anchor) > rod.maxTetherDistance * rod.maxTetherDistance) {
            cancel(world, rod, true);
            return true;
        }

        long now = world.getTime();
        rod.conductive.entrySet().removeIf(entry -> entry.getValue() < now);
        if (rod.supercellExpires < now) rod.supercellCharges = 0;
        if ((rod.mode & 4) != 0) {
            rod.anchor = rod.anchor.lerp(actor.getPos(), Math.min(1, 0.3 / Math.max(.01, rod.anchor.distanceTo(actor.getPos()))));
            rodVisual.setPosition(rod.anchor);
        }
        Iterator<PendingPulse> pulses = rod.pending.iterator();
        while (pulses.hasNext()) {
            PendingPulse pulse = pulses.next();
            if (pulse.arrivalTick <= now) {
                if ((rod.mode & 512) != 0) {
                    rod.supercellCharges = Math.min(10, rod.supercellCharges + 1);
                    rod.supercellExpires = now + 120;
                    pulses.remove();
                    continue;
                }
                boolean capped = rod.arrivedHits * rod.growthPerHit >= rod.maximumGrowth;
                rod.arrivedHits += pulse.growth;
                if (capped && (rod.mode & 64) != 0) rod.overflow = Math.min(4, rod.overflow + 1);
                float growth = Math.min(rod.maximumGrowth, rod.arrivedHits * rod.growthPerHit);
                float radius = rod.baseRadius * (1.0F + growth);
                float damage = rod.baseDamage * (1.0F + growth) * pulse.damage * (1 + rod.overflow * .05F);
                pulse(world, actor, rod, radius, damage);
                rod.overflow = 0;
                rod.arrivals++;
                if ((rod.mode & 128) != 0 && rod.arrivals % 5 == 0 && rod.extraPulseTick != now) {
                    rod.extraPulseTick = now;
                    pulse(world, actor, rod, radius, rod.baseDamage * (1 + growth) * .6F);
                }
                UniqueAbilityApi.emit(rod.execution, UniqueAbilityPhase.HIT, Phase3UniqueAbilities.PULSE,
                        null, 0, damage);
                pulses.remove();
            }
        }

        if (now >= rod.expiresAt && rod.pending.isEmpty()) {
            finish(world, rod);
            return true;
        }
        return false;
    }

    private static void pulse(ServerWorld world, LivingEntity actor, ActiveRod rod,
                              float radius, float baseDamage) {
        LivingEntity sourceOwner = resolveLiving(world, rod.sourceOwnerId);
        double verticalRadius = Math.max(1.5, radius * 0.72);
        Box box = new Box(
                rod.anchor.x - radius,
                rod.anchor.y - 0.5,
                rod.anchor.z - radius,
                rod.anchor.x + radius,
                rod.anchor.y + verticalRadius,
                rod.anchor.z + radius
        );
        int damaged = 0;
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(world, actor, sourceOwner, target)
                        && isInsideCylinder(target, rod.anchor, radius, verticalRadius)
        );
        if (rod.targetCap != Integer.MAX_VALUE) targets.sort(targetOrder(rod.anchor));
        HashSet<UUID> hitTargets = new HashSet<>();
        boolean reverse = rod.reverseNext || (rod.mode & 32768) != 0;
        int rooted = 0;
        for (LivingEntity target : targets) {
            LivingEntity attributedOwner = sourceOwner == null ? actor : sourceOwner;
            DamageSource source = world.getDamageSources().indirectMagic(actor, attributedOwner);
            double distance = horizontalDistance(target.getPos(), rod.anchor);
            float adjusted = baseDamage;
            double centerRadius = rod.tuning.get(Phase3AbilityTuning.Setting.CENTER_RADIUS, 1.5);
            if ((rod.mode & 2048) != 0 && distance <= centerRadius) adjusted *= 1.2F;
            if ((rod.mode & 32768) != 0) {
                if (distance >= radius * .75) {
                    adjusted *= (float) (1 + rod.tuning.get(Phase3AbilityTuning.Setting.EDGE_BONUS, .35));
                } else if (distance <= 2) {
                    adjusted *= .7F;
                }
            }
            float damage = HelperMethods.applyAbilityDamageEnchantments(
                    world, rod.stack, target, source, Math.max(0.0F, adjusted));
            Vec3d previousVelocity = target.getVelocity();
            boolean[] hit = {false};
            WeaponImplicitRegistry.runSuppressed(
                    () -> hit[0] = HelperMethods.damageThroughIframes(target, source, damage));
            if (hit[0]) {
                target.setVelocity(previousVelocity);
                target.velocityModified = true;
                target.velocityDirty = true;
                double reverseStrength = rod.tuning.get(Phase3AbilityTuning.Setting.REVERSE_STRENGTH, 1.5);
                pullTowardRod(target, rod.anchor, reverse ? -Math.max(reverseStrength,
                        Math.abs(rod.pullStrength)) : rod.pullStrength);
                if ((rod.mode & 2048) != 0 && distance <= centerRadius) {
                    target.addVelocity(0, .2, 0);
                }
                if ((rod.mode & 16384) != 0 && distance <= rod.tuning.get(
                        Phase3AbilityTuning.Setting.ROOT_RADIUS, 2.5)
                        && rooted < rod.tuning.integer(Phase3AbilityTuning.Setting.ROOT_TARGET_CAP, 8)) {
                    target.setVelocity(0, target.getVelocity().y, 0);
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 9,
                            false, true, true), actor);
                    rooted++;
                }
                if (rod.reverseNext) target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS, 60, 0, false, true, true), actor);
                damaged++;
                hitTargets.add(target.getUuid());
                if ((rod.mode & 32) != 0 && rod.conductive.size() < 12) {
                    rod.conductive.put(target.getUuid(), world.getTime() + 60);
                }
                if (rod.slowTicks > 0) target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.SLOWNESS, rod.slowTicks, 0, false, true, true), actor);
                UniqueAbilityApi.emit(rod.execution, UniqueAbilityPhase.HIT, Phase3UniqueAbilities.HIT,
                        target, 1, damage);
                if (damaged >= rod.targetCap) break;
            }
        }
        if ((rod.mode & 1024) != 0 && damaged > 0) {
            chainBeyondPulse(world, actor, sourceOwner, rod, radius, baseDamage, hitTargets);
        }
        if ((rod.mode & 8192) != 0 && damaged >= 6 && world.getTime() >= rod.eyeReady) {
            rod.eyeReady = world.getTime() + 100;
            actor.setAbsorptionAmount(Math.max(actor.getAbsorptionAmount(), 3));
            SimplySwordsAPI.setWeaponCooldown(actor, rod.stack, Math.max(0,
                    rod.execution.cooldownTicks(Config.uniqueEffects.stormscale.cooldown) - 20));
        }
        rod.reverseNext = false;

        Entity visual = world.getEntity(rod.rodVisualId);
        if (visual instanceof StormscaleRodVisualEntity rodVisual) {
            rodVisual.setRadius(radius);
            rodVisual.triggerPulse();
        }
        spawnPulseEffects(world, actor, rod.anchor, radius, damaged);
    }

    private static void pullTowardRod(LivingEntity target, Vec3d center, double configuredStrength) {
        if (configuredStrength == 0.0) {
            return;
        }
        Vec3d offset = new Vec3d(center.x - target.getX(), 0.0, center.z - target.getZ());
        double distance = offset.horizontalLength();
        Vec3d current = target.getVelocity();
        if (distance < 0.18) {
            target.setVelocity(current.x * 0.35, Math.max(current.y, 0.08), current.z * 0.35);
            target.velocityModified = true;
            target.velocityDirty = true;
            target.fallDistance = 0.0F;
            return;
        }

        double size = Math.max(1.0, Math.max(target.getWidth(), target.getHeight() / 1.8));
        double resistance = MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double response = MathHelper.clamp(1.0 / (size * (1.0 + resistance * 2.0)), 0.15, 1.0);
        double strength = Math.copySign(Math.min(Math.abs(configuredStrength) * response, distance * 0.2),
                configuredStrength);
        Vec3d pull = offset.multiply(strength / distance);
        target.setVelocity(
                current.x * 0.35 + pull.x,
                Math.max(current.y, 0.08),
                current.z * 0.35 + pull.z
        );
        target.velocityModified = true;
        target.velocityDirty = true;
        target.fallDistance = 0.0F;
    }

    private static boolean isInsideCylinder(LivingEntity target, Vec3d center,
                                            double radius, double verticalRadius) {
        double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
        double horizontalRadius = radius + targetRadius;
        double dx = target.getX() - center.x;
        double dz = target.getZ() - center.z;
        return dx * dx + dz * dz <= horizontalRadius * horizontalRadius
                && target.getBoundingBox().maxY >= center.y - 0.5
                && target.getBoundingBox().minY <= center.y + verticalRadius;
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity target) {
        return actor != null
                && actor.isAlive()
                && actor.getWorld() == world
                && target != null
                && target.isAlive()
                && target != actor
                && target != sourceOwner
                && target.getWorld() == world
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static Hand resolveHitHand(LivingEntity actor, ItemStack stack) {
        for (Hand hand : Hand.values()) {
            if (actor.getStackInHand(hand) == stack) {
                return hand;
            }
        }
        Hand match = null;
        for (Hand hand : Hand.values()) {
            if (ItemStack.areItemsAndComponentsEqual(actor.getStackInHand(hand), stack)) {
                if (match != null) {
                    return null;
                }
                match = hand;
            }
        }
        return match;
    }

    private static boolean isStillHolding(LivingEntity actor, ActiveRod rod) {
        ItemStack held = actor.getStackInHand(rod.hand);
        return held.isOf(ItemsRegistry.STORMSCALE.get()) && AwakeningApi.isAbilityUnlocked(held);
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living
                : null;
    }

    private static UUID spawnEnergyLink(ServerWorld world, LivingEntity actor,
                                        StormscaleRodVisualEntity rodVisual, int lifetime) {
        LightningPhenomenonStyle style = new LightningPhenomenonStyle(
                LightningPhenomenonShape.ENERGY_LINK,
                PRIMARY_COLOR, CORE_COLOR,
                lifetime, 0, 0,
                0.032F, 0, 0.04F, false
        );
        return AbilityVisualManager.spawnLightningPhenomenon(
                world, actor, actorLinkOffset(actor), rodVisual, ROD_LINK_OFFSET, style);
    }

    private static UUID spawnTravellingPulse(ServerWorld world, LivingEntity actor,
                                             StormscaleRodVisualEntity rodVisual, int travelTicks) {
        LightningPhenomenonStyle style = new LightningPhenomenonStyle(
                LightningPhenomenonShape.TRAVELLING_PULSE,
                PRIMARY_COLOR, CORE_COLOR,
                travelTicks, 0, 0,
                0.075F, 0, 0.04F, false
        );
        return AbilityVisualManager.spawnLightningPhenomenon(
                world, actor, actorLinkOffset(actor), rodVisual, ROD_LINK_OFFSET, style);
    }

    private static float actorLinkOffset(LivingEntity actor) {
        return Math.max(0.55F, actor.getHeight() * 0.55F);
    }

    private static Vec3d resolveAnchor(WeaponAbilityContext context) {
        return resolveAnchor(context, Config.uniqueEffects.stormscale.targetingRange);
    }

    private static Vec3d resolveAnchor(WeaponAbilityContext context, double configuredRange) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        double range = Math.max(1.0, configuredRange);
        LivingEntity target = context.target();
        if (target != null
                && target.isAlive()
                && HelperMethods.checkAbilityTarget(target, actor)
                && actor.squaredDistanceTo(target) <= range * range) {
            return findGroundPosition(world, target.getX(), target.getZ(), target.getY());
        }
        if (!(actor instanceof net.minecraft.entity.player.PlayerEntity)) {
            return null;
        }

        Vec3d start = actor.getEyePos();
        Vec3d facing = context.facing().lengthSquared() < 0.0001
                ? actor.getRotationVec(1.0F)
                : context.facing().normalize();
        Vec3d end = start.add(facing.multiply(range));
        HitResult result = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, actor));
        Vec3d candidate = result.getType() == HitResult.Type.BLOCK ? result.getPos() : end;
        return findGroundPosition(world, candidate.x, candidate.z, candidate.y);
    }

    private static Vec3d findGroundPosition(ServerWorld world, double x, double z, double referenceY) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int startY = Math.min(world.getTopY() - 1, MathHelper.floor(referenceY) + GROUND_SCAN_UP);
        int minY = Math.max(world.getBottomY(), MathHelper.floor(referenceY) - GROUND_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return new Vec3d(x, y + 1.02, z);
            }
        }
        return null;
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor, Vec3d anchor) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                anchor.x, anchor.y + 1.1, anchor.z,
                Config.general.enableModernFieldEffects ? 34 : 18,
                0.55, 0.85, 0.55, 0.12);
        world.spawnParticles(ParticleTypes.FLASH, anchor.x, anchor.y + 1.3, anchor.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, anchor.x, anchor.y, anchor.z,
                SoundRegistry.OBJECT_IMPACT_THUD.get(), SoundCategory.PLAYERS, 0.65F, 0.82F);
        world.playSound(null, anchor.x, anchor.y, anchor.z,
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 0.75F, 0.72F);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_02.get(),
                actor.getSoundCategory(), 0.4F, 0.92F);
    }

    private static void spawnLaunchEffects(ServerWorld world, LivingEntity actor) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                actor.getX(), actor.getEyeY() - 0.2, actor.getZ(),
                Config.general.enableModernFieldEffects ? 18 : 9,
                0.35, 0.45, 0.35, 0.14);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(),
                actor.getSoundCategory(), 0.42F, 1.25F + world.random.nextFloat() * 0.2F);
    }

    private static void spawnPulseEffects(ServerWorld world, LivingEntity actor,
                                          Vec3d anchor, float radius, int damaged) {
        world.spawnParticles(ParticleTypes.FLASH, anchor.x, anchor.y + 1.35, anchor.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                anchor.x, anchor.y + 0.3, anchor.z,
                Config.general.enableModernFieldEffects ? 52 + damaged * 3 : 24 + damaged * 2,
                radius * 0.58, 0.5, radius * 0.58, 0.18);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                anchor.x, anchor.y + 0.25, anchor.z,
                Config.general.enableModernFieldEffects ? 24 : 12,
                radius * 0.42, 0.2, radius * 0.42, 0.1);

        if (Config.general.enableModernFieldEffects) {
            SurfaceDischargeStyle discharge = new SurfaceDischargeStyle(
                    PRIMARY_COLOR, CORE_COLOR, 12, 4, 4, 0.04F, 4, 0.45F);
            double phase = world.random.nextDouble() * Math.PI * 0.5;
            for (int i = 0; i < 8; i++) {
                double angle = phase + MathHelper.TAU * i / 8.0;
                Vec3d direction = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
                AbilityVisualManager.spawnSurfaceDischarge(
                        world, anchor.add(0.0, 0.05, 0.0), direction,
                        radius * (0.82 + world.random.nextDouble() * 0.25), discharge);
            }
            TemporaryWorldLightManager.placeBoltLights(
                    world, actor.getEyePos(), anchor.add(0.0, 1.45, 0.0), 15, 5);
        }

        world.playSound(null, anchor.x, anchor.y, anchor.z,
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get(),
                SoundCategory.PLAYERS, 0.82F, 0.9F + world.random.nextFloat() * 0.12F);
        world.playSound(null, anchor.x, anchor.y, anchor.z,
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                SoundCategory.PLAYERS, 0.62F, 0.82F);
    }

    private static void finish(ServerWorld world, ActiveRod rod) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                rod.anchor.x, rod.anchor.y + 0.8, rod.anchor.z,
                12, 0.3, 0.5, 0.3, 0.05);
        world.playSound(null, rod.anchor.x, rod.anchor.y, rod.anchor.z,
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_02.get(),
                SoundCategory.PLAYERS, 0.3F, 0.75F);
        cancel(world, rod, false);
    }

    private static void cancel(ServerWorld world, ActiveRod rod, boolean snapped) {
        Entity visual = world.getEntity(rod.rodVisualId);
        if (visual != null) {
            visual.discard();
        }
        AbilityVisualManager.discard(world, rod.tetherVisualId);
        for (PendingPulse pulse : rod.pending) {
            if (pulse.visualId != null) AbilityVisualManager.discard(world, pulse.visualId);
        }
        if (snapped) {
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    rod.anchor.x, rod.anchor.y + 0.9, rod.anchor.z,
                    10, 0.3, 0.45, 0.3, 0.08);
        }
        if (snapped) UniqueAbilityApi.cancel(rod.execution);
        else UniqueAbilityApi.finish(rod.execution, Phase3UniqueAbilities.FINISH, Math.round(rod.arrivedHits));
        if ((rod.mode & (2 | 512 | 4096)) != 0) {
            LivingEntity actor = resolveLiving(world, rod.actorId);
            if (actor != null) SimplySwordsAPI.setWeaponCooldown(actor, rod.stack,
                    rod.execution.cooldownTicks(Config.uniqueEffects.stormscale.cooldown));
        }
    }

    private static ActiveRod active(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveRod> rods = ACTIVE.get(world);
        return rods == null ? null : rods.get(actorId);
    }

    private static boolean canReactivate(WeaponAbilityContext context, ActiveRod rod) {
        long now = context.world().getTime();
        if ((rod.mode & 4096) != 0 && context.actor().isSneaking() && now >= rod.reverseReady) return true;
        if ((rod.mode & 512) != 0 && rod.supercellCharges > 0 && rod.supercellExpires >= now) return true;
        return (rod.mode & 2) != 0 && !rod.repositioned
                && resolveAnchor(context, rod.tuning.get(Phase3AbilityTuning.Setting.REPOSITION_RANGE, 14)) != null;
    }

    private static boolean reactivate(WeaponAbilityContext context, ActiveRod rod) {
        long now = context.world().getTime();
        if ((rod.mode & 4096) != 0 && context.actor().isSneaking() && now >= rod.reverseReady) {
            rod.reverseNext = true;
            rod.reverseReady = now + 60;
            return true;
        }
        if ((rod.mode & 512) != 0 && rod.supercellCharges > 0 && rod.supercellExpires >= now) {
            int charges = rod.supercellCharges;
            rod.supercellCharges = 0;
            pulse(context.world(), context.actor(), rod, 6, rod.baseDamage * .35F * charges);
            return true;
        }
        Vec3d anchor = resolveAnchor(context, rod.tuning.get(Phase3AbilityTuning.Setting.REPOSITION_RANGE, 14));
        if ((rod.mode & 2) == 0 || rod.repositioned || anchor == null) return false;
        rod.anchor = anchor;
        rod.repositioned = true;
        rod.expiresAt = Math.max(now + 1, rod.expiresAt - 80);
        Entity visual = context.world().getEntity(rod.rodVisualId);
        if (visual != null) visual.setPosition(anchor);
        spawnActivationEffects(context.world(), context.actor(), anchor);
        return true;
    }

    private static Comparator<LivingEntity> targetOrder(Vec3d origin) {
        return Comparator.comparingDouble((LivingEntity target) -> target.squaredDistanceTo(origin))
                .thenComparing(target -> target.getUuid().toString());
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        return Math.sqrt(MathHelper.square(first.x - second.x) + MathHelper.square(first.z - second.z));
    }

    private static void chainBeyondPulse(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                         ActiveRod rod, float radius, float baseDamage, HashSet<UUID> excluded) {
        double chainRange = rod.tuning.get(Phase3AbilityTuning.Setting.CHAIN_RANGE, 3);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
                new Box(rod.anchor, rod.anchor).expand(radius + chainRange), target ->
                        isValidTarget(world, actor, sourceOwner, target)
                                && !excluded.contains(target.getUuid())
                                && horizontalDistance(target.getPos(), rod.anchor) > radius);
        targets.sort(targetOrder(rod.anchor));
        LivingEntity attributedOwner = sourceOwner == null ? actor : sourceOwner;
        int cap = rod.tuning.integer(Phase3AbilityTuning.Setting.CHAIN_TARGET_CAP, 3);
        for (int index = 0; index < Math.min(cap, targets.size()); index++) {
            LivingEntity target = targets.get(index);
            DamageSource source = world.getDamageSources().indirectMagic(actor, attributedOwner);
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, rod.stack, target, source,
                    baseDamage * .25F);
            WeaponImplicitRegistry.runSuppressed(() -> HelperMethods.damageThroughIframes(target, source, damage));
        }
    }

    private static void purgeOrphans(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof StormscaleRodVisualEntity
                    && entity.getCommandTags().contains(ROD_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveRod {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final ItemStack stack;
        private final Hand hand;
        private Vec3d anchor;
        private final UUID rodVisualId;
        private final UUID tetherVisualId;
        private long expiresAt;
        private final int travelTicks;
        private final float baseRadius;
        private final float growthPerHit;
        private final float maximumGrowth;
        private final double pullStrength;
        private final double maxTetherDistance;
        private final float baseDamage;
        private final int targetCap;
        private final int slowTicks;
        private final int mode;
        private final Phase3AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private final List<PendingPulse> pending = new ArrayList<>();
        private final Map<UUID, Long> conductive = new HashMap<>();
        private long lastLaunchTick = Long.MIN_VALUE;
        private long doubleChargeReady;
        private long reverseReady;
        private long eyeReady;
        private long supercellExpires;
        private long extraPulseTick = Long.MIN_VALUE;
        private float arrivedHits;
        private int arrivals;
        private int overflow;
        private int supercellCharges;
        private boolean repositioned;
        private boolean reverseNext;

        private ActiveRod(UUID actorId, UUID sourceOwnerId, ItemStack stack, Hand hand,
                          Vec3d anchor, UUID rodVisualId, UUID tetherVisualId,
                          long expiresAt, int travelTicks, float baseRadius,
                          float growthPerHit, float maximumGrowth,
                          double pullStrength,
                          double maxTetherDistance, float baseDamage, int targetCap, int slowTicks,
                          int mode, Phase3AbilityTuning tuning, UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.stack = stack;
            this.hand = hand;
            this.anchor = anchor;
            this.rodVisualId = rodVisualId;
            this.tetherVisualId = tetherVisualId;
            this.expiresAt = expiresAt;
            this.travelTicks = travelTicks;
            this.baseRadius = baseRadius;
            this.growthPerHit = growthPerHit;
            this.maximumGrowth = maximumGrowth;
            this.pullStrength = pullStrength;
            this.maxTetherDistance = maxTetherDistance;
            this.baseDamage = baseDamage;
            this.targetCap = targetCap;
            this.slowTicks = slowTicks;
            this.mode = mode;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private static final class PendingPulse {
        private final long arrivalTick;
        private final UUID visualId;
        private final float growth;
        private final float damage;

        private PendingPulse(long arrivalTick, UUID visualId, float growth, float damage) {
            this.arrivalTick = arrivalTick;
            this.visualId = visualId;
            this.growth = growth;
            this.damage = damage;
        }
    }
}
