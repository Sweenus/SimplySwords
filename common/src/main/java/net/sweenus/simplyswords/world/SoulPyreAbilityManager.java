package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulPyreVisualEntity;
import net.sweenus.simplyswords.entity.SoulPyreWispEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SoulPyreAbilityManager {

    private static final String FIELD_VISUAL_TAG = "simplyswords_soul_pyre_visual";
    private static final String WISP_VISUAL_TAG = "simplyswords_soul_pyre_wisp";
    private static final int RADIUS_GROWTH_TICKS = 10;
    private static final int WISP_LIFETIME = 30;
    private static final double WISP_SPEED = 0.78;
    private static final double WISP_IMPACT_DISTANCE = 0.72;
    private static final float[] RADIUS_MILESTONES = {0.25F, 0.50F, 0.75F, 1.0F};

    private static final Map<ServerWorld, Map<UUID, ActivePyre>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveWisp>> WISPS = new HashMap<>();

    private SoulPyreAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        List<ActiveWisp> wisps = WISPS.get(world);
        return active != null && !active.isEmpty()
                || wisps != null && !wisps.isEmpty();
    }

    public static boolean hasActiveForActor(ServerWorld world, UUID actorId) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        return active != null && actorId != null && active.containsKey(actorId);
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().isOf(ItemsRegistry.SOULPYRE.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !hasActiveForActor(context.world(), context.actor().getUuid());
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        int pulseCount = Math.max(1, Config.uniqueEffects.soulpyre.pulseCount);
        int duration = Math.max(pulseCount, Config.uniqueEffects.soulpyre.duration);
        int collapseDuration = Math.max(1, Config.uniqueEffects.soulpyre.collapseDuration);
        float maxRadius = (float) Math.max(1.5, Config.uniqueEffects.soulpyre.radius);
        float startRadius = MathHelper.clamp(
                Config.uniqueEffects.soulpyre.startingRadius,
                1.5F,
                maxRadius
        );
        long now = world.getTime();

        SoulPyreVisualEntity visual = null;
        if (Config.general.enableModernFieldEffects) {
            visual = new SoulPyreVisualEntity(
                    world,
                    actor,
                    maxRadius,
                    Config.uniqueEffects.soulpyre.verticalRange,
                    duration + collapseDuration + 20,
                    world.random.nextInt()
            );
            visual.setRadius(startRadius);
            visual.addCommandTag(FIELD_VISUAL_TAG);
            world.spawnEntity(visual);
        }

        ActivePyre pyre = new ActivePyre(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.stack().copy(),
                visual == null ? null : visual.getUuid(),
                now,
                now + duration,
                pulseCount,
                duration,
                collapseDuration,
                startRadius,
                maxRadius,
                HelperMethods.attackScaledDamage(
                        actor,
                        context.stack(),
                        Config.uniqueEffects.soulpyre.damageScaling
                )
        );
        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), pyre);
        actor.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.SOULTETHER),
                duration + 2,
                0,
                false,
                true
        ));

        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                actor.getSoundCategory(),
                0.45F,
                0.72F
        );
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                actor.getX(),
                actor.getBodyY(0.45),
                actor.getZ(),
                32,
                1.1,
                0.3,
                1.1,
                0.035
        );
        return true;
    }

    public static void tick(ServerWorld world) {
        tickPyres(world);
        tickWisps(world);
    }

    public static void onDeath(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || source == null) {
            return;
        }
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        Entity directSource = source.getSource();
        Entity attacker = source.getAttacker();
        ActivePyre selected = null;
        LivingEntity selectedActor = null;
        int bestScore = 0;
        double bestDistance = Double.MAX_VALUE;

        for (ActivePyre pyre : active.values()) {
            if (pyre.collapsing) {
                continue;
            }
            LivingEntity actor = resolveLiving(world, pyre.actorId);
            LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
            if (actor == null
                    || !actor.isAlive()
                    || !isEligibleTarget(actor, sourceOwner, target)
                    || !isInsideField(actor, target, pyre.currentRadius)) {
                continue;
            }

            int score = sourceMatchScore(pyre, directSource, attacker);
            if (score <= 0) {
                continue;
            }
            double distance = actor.squaredDistanceTo(target);
            if (score > bestScore || score == bestScore && distance < bestDistance) {
                selected = pyre;
                selectedActor = actor;
                bestScore = score;
                bestDistance = distance;
            }
        }

        if (selected == null || selectedActor == null) {
            return;
        }

        growRadius(world, selectedActor, selected);
        harvestSoul(world, target, selectedActor, selected);
    }

    private static void tickPyres(ServerWorld world) {
        Map<UUID, ActivePyre> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, ActivePyre>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            ActivePyre pyre = iterator.next().getValue();
            LivingEntity actor = resolveLiving(world, pyre.actorId);
            if (actor == null || !actor.isAlive()) {
                discardVisual(world, pyre.visualId);
                iterator.remove();
                continue;
            }

            if (pyre.collapsing) {
                if (tickCollapse(world, actor, pyre, now)) {
                    iterator.remove();
                }
                continue;
            }

            if (!actor.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER))) {
                discardVisual(world, pyre.visualId);
                iterator.remove();
                continue;
            }

            updateGrowth(world, actor, pyre, now);
            launchPendingVolleys(world, actor, pyre);

            while (!pyre.collapsing && pyre.pulsesCompleted < pyre.pulseCount
                    && now >= pulseTick(pyre, pyre.pulsesCompleted + 1)) {
                boolean finalPulse = pyre.pulsesCompleted == pyre.pulseCount - 1;
                if (finalPulse) {
                    beginCollapse(world, actor, pyre, now);
                } else {
                    performPulse(world, actor, pyre);
                }
                pyre.pulsesCompleted++;
            }

            if (!pyre.collapsing && now >= pyre.endTick) {
                beginCollapse(world, actor, pyre, now);
                pyre.pulsesCompleted = pyre.pulseCount;
            }
        }

        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static long pulseTick(ActivePyre pyre, int pulseNumber) {
        return pyre.startTick + Math.round(
                (double) pyre.duration * pulseNumber / pyre.pulseCount);
    }

    private static void performPulse(ServerWorld world, LivingEntity actor,
                                     ActivePyre pyre) {
        int remaining = Math.max(1, pyre.pulseCount - pyre.pulsesCompleted);
        float basePulseDamage = Math.min(
                30.0F,
                Math.max(0.0F, pyre.baseDamage - remaining)
        );
        float soulBonus = Math.max(
                0.0F,
                Config.uniqueEffects.soulpyre.pulseDamageBonusPerSoul
        );
        float damage = basePulseDamage * (1.0F + pyre.souls * soulBonus);

        actor.heal(Math.max(0.0F, Config.uniqueEffects.soulpyre.heal));
        damagePulseArea(world, actor, pyre, pyre.currentRadius, damage);
        triggerPulseVisual(world, pyre.visualId);
        spawnPulseEffects(
                world,
                actor,
                pyre.currentRadius,
                pyre.pulsesCompleted,
                false
        );
    }

    private static void beginCollapse(ServerWorld world, LivingEntity actor,
                                      ActivePyre pyre, long now) {
        if (pyre.collapsing) {
            return;
        }

        launchPendingVolleys(world, actor, pyre);
        pyre.collapsing = true;
        pyre.collapseStartTick = now;
        pyre.collapseEndTick = now + pyre.collapseDuration;
        pyre.collapseStartRadius = pyre.currentRadius;
        actor.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER));

        int requiemSouls = pyre.souls;
        pyre.souls = 0;
        float finalBaseDamage = Math.min(
                30.0F,
                Math.max(0.0F, pyre.baseDamage - 1.0F)
        );
        float soulBonus = Math.max(
                0.0F,
                Config.uniqueEffects.soulpyre.requiemDamageBonusPerSoul
        );
        float damage = finalBaseDamage * (1.0F + requiemSouls * soulBonus);

        damagePulseArea(world, actor, pyre, pyre.currentRadius, damage);
        actor.heal(
                Math.max(0.0F, Config.uniqueEffects.soulpyre.heal)
                        + requiemSouls * Math.max(
                        0.0F,
                        Config.uniqueEffects.soulpyre.requiemHealingPerSoul)
        );
        launchWispVolley(world, actor, pyre, requiemSouls, pyre.currentRadius);
        beginVisualCollapse(world, actor, pyre);
        spawnPulseEffects(
                world,
                actor,
                pyre.currentRadius,
                pyre.pulsesCompleted,
                true
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(),
                actor.getSoundCategory(),
                1.15F,
                0.55F
        );
    }

    private static boolean tickCollapse(ServerWorld world, LivingEntity actor,
                                        ActivePyre pyre, long now) {
        float progress = MathHelper.clamp(
                (float) (now - pyre.collapseStartTick)
                        / Math.max(1.0F, pyre.collapseDuration),
                0.0F,
                1.0F
        );
        float eased = progress * progress * (3.0F - 2.0F * progress);
        pyre.currentRadius = MathHelper.lerp(
                eased,
                pyre.collapseStartRadius,
                0.0F
        );
        updateVisual(world, actor, pyre, pyre.currentRadius);
        if (now < pyre.collapseEndTick) {
            return false;
        }
        discardVisual(world, pyre.visualId);
        return true;
    }

    private static void growRadius(ServerWorld world, LivingEntity actor,
                                   ActivePyre pyre) {
        float growth = Math.max(
                0.0F,
                Config.uniqueEffects.soulpyre.radiusGrowthPerKill
        );
        float nextRadius = Math.min(pyre.maxRadius, pyre.targetRadius + growth);
        if (nextRadius <= pyre.targetRadius + 1.0E-4F) {
            return;
        }
        pyre.growthStartRadius = pyre.currentRadius;
        pyre.targetRadius = nextRadius;
        pyre.growthStartTick = world.getTime();
        pyre.growing = true;
        updateVisual(world, actor, pyre, pyre.currentRadius);
    }

    private static void updateGrowth(ServerWorld world, LivingEntity actor,
                                     ActivePyre pyre, long now) {
        if (pyre.growing) {
            float progress = MathHelper.clamp(
                    (float) (now - pyre.growthStartTick) / RADIUS_GROWTH_TICKS,
                    0.0F,
                    1.0F
            );
            float eased = progress * progress * (3.0F - 2.0F * progress);
            pyre.currentRadius = MathHelper.lerp(
                    eased,
                    pyre.growthStartRadius,
                    pyre.targetRadius
            );
            if (progress >= 1.0F) {
                pyre.currentRadius = pyre.targetRadius;
                pyre.growing = false;
            }
            checkRadiusMilestones(world, actor, pyre);
        }
        updateVisual(world, actor, pyre, pyre.currentRadius);
    }

    private static void checkRadiusMilestones(ServerWorld world, LivingEntity actor,
                                              ActivePyre pyre) {
        if (pyre.nextMilestone >= RADIUS_MILESTONES.length
                || pyre.maxRadius <= pyre.startRadius) {
            return;
        }

        int crossedMilestone = -1;
        while (pyre.nextMilestone < RADIUS_MILESTONES.length) {
            float threshold = MathHelper.lerp(
                    RADIUS_MILESTONES[pyre.nextMilestone],
                    pyre.startRadius,
                    pyre.maxRadius
            );
            if (pyre.currentRadius + 1.0E-4F < threshold) {
                break;
            }
            crossedMilestone = pyre.nextMilestone;
            pyre.nextMilestone++;
        }
        if (crossedMilestone >= 0) {
            spawnRadiusMilestoneEffects(
                    world,
                    actor,
                    pyre,
                    crossedMilestone
            );
        }
    }

    private static void harvestSoul(ServerWorld world, LivingEntity target,
                                    LivingEntity actor, ActivePyre pyre) {
        int harvestedCount = pyre.souls + 1;
        pyre.souls++;
        int volleySize = Math.max(1, Config.uniqueEffects.soulpyre.wispVolleySize);
        while (pyre.souls >= volleySize) {
            pyre.souls -= volleySize;
            pyre.pendingVolleys++;
        }
        updateVisual(world, actor, pyre, pyre.currentRadius);
        spawnHarvestEffects(
                world,
                target,
                actor,
                Math.min(volleySize, harvestedCount)
        );
    }

    private static void launchPendingVolleys(ServerWorld world, LivingEntity actor,
                                             ActivePyre pyre) {
        int volleySize = Math.max(1, Config.uniqueEffects.soulpyre.wispVolleySize);
        while (pyre.pendingVolleys > 0) {
            launchWispVolley(
                    world,
                    actor,
                    pyre,
                    volleySize,
                    pyre.currentRadius
            );
            pyre.pendingVolleys--;
            spawnEarlyVolleyEffects(world, actor, pyre.currentRadius);
        }
    }

    private static void damagePulseArea(ServerWorld world, LivingEntity actor,
                                        ActivePyre pyre, float radius, float damage) {
        LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
        double verticalRange = Math.max(
                2.0,
                Config.uniqueEffects.soulpyre.verticalRange
        );
        Box search = actor.getBoundingBox().expand(radius, verticalRange, radius);
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                search,
                EntityPredicates.VALID_LIVING_ENTITY
        )) {
            if (!isInsideField(actor, target, radius)
                    || !isValidTarget(actor, sourceOwner, target)) {
                continue;
            }
            applyAbilityDamage(
                    world,
                    actor,
                    sourceOwner,
                    pyre.stack,
                    target,
                    damage
            );
            pullTarget(actor, target);
        }
    }

    private static void launchWispVolley(ServerWorld world, LivingEntity actor,
                                         ActivePyre pyre, int soulCount,
                                         float fieldRadius) {
        if (soulCount <= 0) {
            return;
        }

        LivingEntity sourceOwner = resolveLiving(world, pyre.sourceOwnerId);
        double searchRadius = Math.max(1.0, fieldRadius + 4.0);
        List<LivingEntity> targets = findWispTargets(
                world,
                actor,
                sourceOwner,
                searchRadius
        );
        float damage = pyre.baseDamage
                * Math.max(0.0F, Config.uniqueEffects.soulpyre.wispDamageMultiplier);

        for (int i = 0; i < soulCount; i++) {
            double angle = MathHelper.TAU * i / Math.max(1, soulCount);
            Vec3d start = actor.getPos().add(
                    Math.cos(angle) * 1.2,
                    actor.getHeight() * 0.62 + Math.sin(angle * 2.0) * 0.18,
                    Math.sin(angle) * 1.2
            );
            SoulPyreWispEntity visual = new SoulPyreWispEntity(
                    world,
                    start.x,
                    start.y,
                    start.z,
                    WISP_LIFETIME,
                    world.random.nextInt()
            );
            visual.setVelocity(
                    Math.cos(angle) * 0.18,
                    0.09,
                    Math.sin(angle) * 0.18
            );
            visual.addCommandTag(WISP_VISUAL_TAG);
            world.spawnEntity(visual);
            WISPS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                    new ActiveWisp(
                            visual.getUuid(),
                            pyre.actorId,
                            pyre.sourceOwnerId,
                            targets.isEmpty()
                                    ? null
                                    : targets.get(i % targets.size()).getUuid(),
                            pyre.stack.copy(),
                            damage,
                            searchRadius,
                            world.getTime() + WISP_LIFETIME
                    )
            );
        }
    }

    private static void tickWisps(ServerWorld world) {
        List<ActiveWisp> wisps = WISPS.get(world);
        if (wisps == null || wisps.isEmpty()) {
            return;
        }

        Iterator<ActiveWisp> iterator = wisps.iterator();
        while (iterator.hasNext()) {
            ActiveWisp wisp = iterator.next();
            Entity entity = world.getEntity(wisp.visualId);
            LivingEntity actor = resolveLiving(world, wisp.actorId);
            LivingEntity sourceOwner = resolveLiving(world, wisp.sourceOwnerId);
            if (!(entity instanceof SoulPyreWispEntity visual)
                    || actor == null
                    || !actor.isAlive()
                    || world.getTime() >= wisp.expiresAt) {
                if (entity != null) {
                    entity.discard();
                }
                iterator.remove();
                continue;
            }

            LivingEntity target = resolveLiving(world, wisp.targetId);
            if (target == null
                    || !target.isAlive()
                    || !isValidTarget(actor, sourceOwner, target)) {
                target = findWispTargets(
                        world,
                        actor,
                        sourceOwner,
                        wisp.searchRadius
                ).stream().findFirst().orElse(null);
                wisp.targetId = target == null ? null : target.getUuid();
            }
            if (target == null) {
                Vec3d velocity = visual.getVelocity()
                        .multiply(0.94)
                        .add(0.0, 0.006, 0.0);
                moveWisp(visual, velocity);
                spawnWispTrail(world, visual);
                continue;
            }

            Vec3d targetPos = target.getPos().add(
                    0.0,
                    target.getHeight() * 0.55,
                    0.0
            );
            Vec3d delta = targetPos.subtract(visual.getPos());
            if (delta.lengthSquared()
                    <= WISP_IMPACT_DISTANCE * WISP_IMPACT_DISTANCE) {
                applyAbilityDamage(
                        world,
                        actor,
                        sourceOwner,
                        wisp.stack,
                        target,
                        wisp.damage
                );
                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        targetPos.x,
                        targetPos.y,
                        targetPos.z,
                        18,
                        0.3,
                        0.35,
                        0.3,
                        0.04
                );
                world.playSoundFromEntity(
                        null,
                        target,
                        SoundEvents.BLOCK_SOUL_SAND_BREAK,
                        target.getSoundCategory(),
                        0.65F,
                        1.35F
                );
                visual.discard();
                iterator.remove();
                continue;
            }

            Vec3d desiredVelocity = delta.normalize().multiply(WISP_SPEED);
            Vec3d velocity = visual.getVelocity()
                    .multiply(0.42)
                    .add(desiredVelocity.multiply(0.58));
            if (velocity.lengthSquared() > WISP_SPEED * WISP_SPEED) {
                velocity = velocity.normalize().multiply(WISP_SPEED);
            }
            moveWisp(visual, velocity);
            spawnWispTrail(world, visual);
        }

        if (wisps.isEmpty()) {
            WISPS.remove(world);
        }
    }

    private static List<LivingEntity> findWispTargets(
            ServerWorld world, LivingEntity actor,
            LivingEntity sourceOwner, double radius) {
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                actor.getBoundingBox().expand(radius),
                EntityPredicates.VALID_LIVING_ENTITY
        )) {
            if (target.squaredDistanceTo(actor) <= radius * radius
                    && isValidTarget(actor, sourceOwner, target)) {
                targets.add(target);
            }
        }
        targets.sort(Comparator.comparingDouble(
                target -> target.squaredDistanceTo(actor)));
        return targets;
    }

    private static int sourceMatchScore(ActivePyre pyre,
                                        Entity directSource, Entity attacker) {
        UUID directId = directSource == null ? null : directSource.getUuid();
        UUID attackerId = attacker == null ? null : attacker.getUuid();
        if (pyre.actorId.equals(directId)) {
            return 4;
        }
        if (pyre.actorId.equals(attackerId)) {
            return 3;
        }
        if (pyre.sourceOwnerId != null
                && (pyre.sourceOwnerId.equals(directId)
                || pyre.sourceOwnerId.equals(attackerId))) {
            return 2;
        }
        return 0;
    }

    private static boolean applyAbilityDamage(
            ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
            ItemStack stack, LivingEntity target, float damage) {
        LivingEntity attributedOwner = sourceOwner == null ? actor : sourceOwner;
        DamageSource source = world.getDamageSources().indirectMagic(
                actor,
                attributedOwner
        );
        float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                world,
                stack,
                target,
                source,
                Math.max(0.0F, damage)
        );
        return HelperMethods.damageThroughIframes(target, source, finalDamage);
    }

    private static boolean isValidTarget(
            LivingEntity actor, LivingEntity sourceOwner, LivingEntity target) {
        return target.isAlive()
                && isEligibleTarget(actor, sourceOwner, target);
    }

    private static boolean isEligibleTarget(
            LivingEntity actor, LivingEntity sourceOwner, LivingEntity target) {
        return target != actor
                && target != sourceOwner
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null
                || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static boolean isInsideField(
            LivingEntity actor, LivingEntity target, float radius) {
        double dx = target.getX() - actor.getX();
        double dz = target.getZ() - actor.getZ();
        double verticalRange = Math.max(
                2.0,
                Config.uniqueEffects.soulpyre.verticalRange
        );
        return dx * dx + dz * dz <= radius * radius
                && Math.abs(target.getBodyY(0.5) - actor.getBodyY(0.5))
                <= verticalRange;
    }

    private static void pullTarget(LivingEntity actor, LivingEntity target) {
        Vec3d direction = actor.getPos()
                .add(0.0, actor.getHeight() * 0.45, 0.0)
                .subtract(target.getPos()
                        .add(0.0, target.getHeight() * 0.45, 0.0));
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        Vec3d pull = direction.normalize().multiply(0.125);
        target.setVelocity(target.getVelocity().multiply(0.72).add(pull));
        target.velocityModified = true;
        target.velocityDirty = true;
    }

    private static void moveWisp(SoulPyreWispEntity visual, Vec3d velocity) {
        visual.setVelocity(velocity);
        visual.setPosition(
                visual.getX() + velocity.x,
                visual.getY() + velocity.y,
                visual.getZ() + velocity.z
        );
        visual.velocityModified = true;
        visual.velocityDirty = true;
    }

    private static void spawnWispTrail(
            ServerWorld world, SoulPyreWispEntity visual) {
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                visual.getX(),
                visual.getY(),
                visual.getZ(),
                2,
                0.08,
                0.08,
                0.08,
                0.005
        );
    }

    private static void updateVisual(ServerWorld world, LivingEntity actor,
                                     ActivePyre pyre, float radius) {
        Entity entity = pyre.visualId == null
                ? null
                : world.getEntity(pyre.visualId);
        if (entity instanceof SoulPyreVisualEntity visual) {
            visual.setPosition(actor.getX(), actor.getY(), actor.getZ());
            visual.setOwnerEntityId(actor.getId());
            visual.setRadius(radius);
            visual.setSoulCount(pyre.souls);
            visual.setPhase(
                    pyre.collapsing
                            ? SoulPyreVisualEntity.PHASE_COLLAPSING
                            : SoulPyreVisualEntity.PHASE_ACTIVE
            );
        }
    }

    private static void triggerPulseVisual(ServerWorld world, UUID visualId) {
        Entity entity = visualId == null ? null : world.getEntity(visualId);
        if (entity instanceof SoulPyreVisualEntity visual) {
            visual.triggerPulse();
        }
    }

    private static void beginVisualCollapse(ServerWorld world, LivingEntity actor,
                                            ActivePyre pyre) {
        Entity entity = pyre.visualId == null
                ? null
                : world.getEntity(pyre.visualId);
        if (entity instanceof SoulPyreVisualEntity visual) {
            visual.setPosition(actor.getX(), actor.getY(), actor.getZ());
            visual.setSoulCount(0);
            visual.beginCollapse(pyre.collapseDuration + 2);
        }
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity entity = visualId == null ? null : world.getEntity(visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void spawnHarvestEffects(
            ServerWorld world, LivingEntity target,
            LivingEntity actor, int soulCount) {
        HelperMethods.spawnWaistHeightParticles(
                world,
                ParticleTypes.SOUL,
                target,
                actor,
                10
        );
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getBodyY(0.5),
                target.getZ(),
                14,
                0.25,
                0.35,
                0.25,
                0.025
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(),
                actor.getSoundCategory(),
                0.35F + Math.min(5, soulCount) * 0.035F,
                1.15F + Math.min(5, soulCount) * 0.035F
        );
    }

    private static void spawnPulseEffects(
            ServerWorld world, LivingEntity actor,
            float radius, int pulseIndex, boolean requiem) {
        HelperMethods.spawnOrbitParticles(
                world,
                actor.getPos().add(0.0, 0.08, 0.0),
                ParticleTypes.SOUL_FIRE_FLAME,
                radius,
                requiem ? 72 : 30
        );
        HelperMethods.spawnOrbitParticles(
                world,
                actor.getPos().add(0.0, 0.14, 0.0),
                ParticleTypes.SOUL,
                radius,
                requiem ? 48 : 20
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                actor.getSoundCategory(),
                requiem ? 0.85F : 0.12F,
                requiem ? 0.58F : 0.82F + pulseIndex * 0.055F
        );
    }

    private static void spawnRadiusMilestoneEffects(
            ServerWorld world, LivingEntity actor,
            ActivePyre pyre, int milestone) {
        triggerPulseVisual(world, pyre.visualId);
        HelperMethods.spawnOrbitParticles(
                world,
                actor.getPos().add(0.0, 0.1, 0.0),
                ParticleTypes.SOUL_FIRE_FLAME,
                pyre.currentRadius,
                42 + milestone * 10
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                actor.getSoundCategory(),
                0.38F + milestone * 0.12F,
                0.76F + milestone * 0.16F
        );
    }

    private static void spawnEarlyVolleyEffects(
            ServerWorld world, LivingEntity actor, float radius) {
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                actor.getX(),
                actor.getBodyY(0.6),
                actor.getZ(),
                28,
                Math.min(1.6, radius * 0.18),
                0.45,
                Math.min(1.6, radius * 0.18),
                0.045
        );
        world.playSoundFromEntity(
                null,
                actor,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(),
                actor.getSoundCategory(),
                0.9F,
                0.78F
        );
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID entityId) {
        if (entityId == null) {
            return null;
        }
        Entity entity = world.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static final class ActivePyre {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final ItemStack stack;
        private final UUID visualId;
        private final long startTick;
        private final long endTick;
        private final int pulseCount;
        private final int duration;
        private final int collapseDuration;
        private final float startRadius;
        private final float maxRadius;
        private final float baseDamage;
        private int pulsesCompleted;
        private int souls;
        private int pendingVolleys;
        private int nextMilestone;
        private float currentRadius;
        private float targetRadius;
        private float growthStartRadius;
        private long growthStartTick;
        private boolean growing;
        private boolean collapsing;
        private long collapseStartTick;
        private long collapseEndTick;
        private float collapseStartRadius;

        private ActivePyre(UUID actorId, UUID sourceOwnerId, ItemStack stack,
                           UUID visualId, long startTick, long endTick,
                           int pulseCount, int duration, int collapseDuration,
                           float startRadius, float maxRadius, float baseDamage) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.stack = stack;
            this.visualId = visualId;
            this.startTick = startTick;
            this.endTick = endTick;
            this.pulseCount = pulseCount;
            this.duration = duration;
            this.collapseDuration = collapseDuration;
            this.startRadius = startRadius;
            this.maxRadius = maxRadius;
            this.baseDamage = baseDamage;
            this.currentRadius = startRadius;
            this.targetRadius = startRadius;
            this.growthStartRadius = startRadius;
        }
    }

    private static final class ActiveWisp {
        private final UUID visualId;
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private UUID targetId;
        private final ItemStack stack;
        private final float damage;
        private final double searchRadius;
        private final long expiresAt;

        private ActiveWisp(UUID visualId, UUID actorId, UUID sourceOwnerId,
                           UUID targetId, ItemStack stack, float damage,
                           double searchRadius, long expiresAt) {
            this.visualId = visualId;
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.targetId = targetId;
            this.stack = stack;
            this.damage = damage;
            this.searchRadius = searchRadius;
            this.expiresAt = expiresAt;
        }
    }
}
