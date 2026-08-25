package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase5AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase5UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.FurnaceChainVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HearthflameAbilityManager {

    private static final String VISUAL_TAG = "simplyswords_furnace_chain_visual";
    private static final double MAX_BREAK_DISTANCE = 24.0;
    private static final float MAX_TENSION = 100.0F;
    private static final float BRAND_STARTING_TENSION = 25.0F;
    private static final float HIT_TENSION = 20.0F;
    private static final float BRAND_PROC_TENSION = 10.0F;
    private static final float HIT_PRESSURE = 20.0F;
    private static final float SNAP_PRESSURE = 10.0F;
    private static final float TENSION_PER_EXCESS_BLOCK = 0.45F;
    private static final int SNAP_VISUAL_TICKS = 10;
    private static final int FINALE_VISUAL_TICKS = 16;

    private static final Map<ServerWorld, Map<BrandKey, FurnaceBrand>> BRANDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ActiveChains>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Set<UUID>> MANAGED_VISUALS = new HashMap<>();

    private HearthflameAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<BrandKey, FurnaceBrand> brands = BRANDS.get(world);
        Map<UUID, ActiveChains> active = ACTIVE.get(world);
        return (brands != null && !brands.isEmpty())
                || (active != null && !active.isEmpty())
                || world.getTime() % 40L == 0L;
    }

    public static boolean isManagedVisual(ServerWorld world, UUID visualId) {
        Set<UUID> visuals = MANAGED_VISUALS.get(world);
        return visuals != null && visuals.contains(visualId);
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.HEARTHFLAME.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || isActive(context.world(), context.actor())) {
            return false;
        }
        return !findTargets(context, Phase5AbilityTuning.EMPTY.with(
                Phase5AbilityTuning.Setting.RANGE, Config.uniqueEffects.hearthflame.radius + 2)).isEmpty();
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        UniqueAbilityExecution execution = Phase5CombatManager.beginActive(Phase5UniqueAbilities.HEARTHFLAME_CHAINS,
                context, Config.uniqueEffects.hearthflame.cooldown);
        Phase5AbilityTuning tuning = Phase5UniqueAbilities.tuning(execution);
        List<LivingEntity> targets = findTargets(context, tuning);
        if (targets.isEmpty()) {
            UniqueAbilityApi.cancel(execution);
            UniqueAbilityApi.clearStartedExecution();
            return false;
        }
        UniqueAbilityApi.start(execution);

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        long now = world.getTime();
        int duration = tuning.integer(Phase5AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.hearthflame.duration);
        ActiveChains ability = new ActiveChains(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.stack().copy(),
                now,
                now + duration,
                HelperMethods.abilityScaledDamage(
                        "fire",
                        actor,
                        context.stack(),
                        Config.uniqueEffects.hearthflame.echoDamageScaling,
                        Config.uniqueEffects.hearthflame.echoSpellScaling
                ) * (float) tuning.get(Phase5AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1),
                HelperMethods.abilityScaledDamage(
                        "fire",
                        actor,
                        context.stack(),
                        Config.uniqueEffects.hearthflame.snapDamageScaling,
                        Config.uniqueEffects.hearthflame.snapSpellScaling
                ) * (float) tuning.get(Phase5AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1),
                HelperMethods.abilityScaledDamage(
                        "fire",
                        actor,
                        context.stack(),
                        Config.uniqueEffects.hearthflame.finalDamageScaling,
                        Config.uniqueEffects.hearthflame.finalSpellScaling
                ) * (float) tuning.get(Phase5AbilityTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1),
                tuning,
                execution
        );

        FurnaceChainVisualEntity coreVisual = spawnVisual(
                world,
                actor,
                null,
                FurnaceChainVisualEntity.MODE_CORE,
                duration + FINALE_VISUAL_TICKS + 20
        );
        ability.coreVisualId = coreVisual == null ? null : coreVisual.getUuid();

        for (LivingEntity target : targets) {
            boolean branded = consumeBrand(world, actor, target);
            FurnaceChainVisualEntity chainVisual = spawnVisual(
                    world,
                    actor,
                    target,
                    FurnaceChainVisualEntity.MODE_CHAIN,
                    duration + SNAP_VISUAL_TICKS + 20
            );
            ability.chains.add(new FurnaceChain(
                    target.getUuid(),
                    chainVisual == null ? null : chainVisual.getUuid(),
                    Math.max(Config.uniqueEffects.hearthflame.minimumChainLength, actor.distanceTo(target)),
                    branded ? BRAND_STARTING_TENSION : 0.0F
            ));
        }

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), ability);
        if (tuning.flag(1 << 9)) actor.setAbsorptionAmount(Math.max(actor.getAbsorptionAmount(), 4));
        if (tuning.flag(1 << 16)) {
            actor.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE, duration, 1), actor);
            actor.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.SLOWNESS, duration, 0), actor);
        }
        if (tuning.flag(1 << 17)) actor.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED, duration, 0), actor);
        updateVisuals(world, actor, ability);
        spawnActivationEffects(world, actor, targets);
        return true;
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack,
                                  LivingEntity reportedAttacker, LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.HEARTHFLAME.get())
                || reportedAttacker == null
                || target == null
                || target.getWorld() != world) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        LivingEntity sourceOwner = delegated == null ? null : delegated.owner();
        if (actor == null || !actor.isAlive() || actor.getWorld() != world) {
            return;
        }

        ActiveChains ability = getActive(world, actor);
        FurnaceChain struckChain = ability == null ? null : findChain(ability, target.getUuid());
        if (ability != null && struckChain != null && ability.primed) {
            finishAbility(world, actor, sourceOwner, ability, 1.0F, true);
            removeActive(world, actor.getUuid());
            return;
        }

        if (!target.isAlive() || !isValidTarget(actor, sourceOwner, target)) {
            return;
        }

        if (ability != null && struckChain != null) {
            echoHit(world, actor, sourceOwner, ability, target);
            struckChain.tension = Math.min(MAX_TENSION, struckChain.tension + HIT_TENSION);
            ability.pressure = Math.min(maximumPressure(ability), ability.pressure + HIT_PRESSURE);
            pulseChains(world, ability);

            if (rollBrand(actor, ability.tuning)) {
                struckChain.tension = Math.min(MAX_TENSION, struckChain.tension + BRAND_PROC_TENSION);
            }
            if (struckChain.tension >= MAX_TENSION) {
                snapChain(world, actor, sourceOwner, ability, struckChain);
                ability.chains.remove(struckChain);
                ability.pressure = Math.min(maximumPressure(ability), ability.pressure + SNAP_PRESSURE);
            }
            updatePrimed(world, actor, ability);
            updateVisuals(world, actor, ability);
            return;
        }

        UniqueAbilityExecution brandExecution = Phase5CombatManager.beginPassive(Phase5UniqueAbilities.HEARTHFLAME_BRAND,
                world, stack, actor, target);
        Phase5AbilityTuning brandTuning = Phase5UniqueAbilities.tuning(brandExecution);
        if (hasBrand(world, actor, target) && brandTuning.flag(1 << 20)) {
            applyAbilityDamage(world, actor, sourceOwner, stack, target,
                    (float) actor.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * .12F, true);
            target.setOnFireFor(1);
        }
        if (hasBrand(world, actor, target) && brandTuning.flag(1 << 13)) {
            actor.setAbsorptionAmount(Math.max(actor.getAbsorptionAmount(), 2));
        }
        if (rollBrand(actor, brandTuning)) applyBrand(world, actor, sourceOwner, target, brandTuning);
        UniqueAbilityApi.finish(brandExecution, Phase5UniqueAbilities.FINISH, 1);
    }

    public static void tick(ServerWorld world) {
        tickBrands(world);
        tickAbilities(world);
        if (world.getTime() % 40L == 0L) {
            purgeOrphanVisuals(world);
        }
    }

    private static void tickBrands(ServerWorld world) {
        Map<BrandKey, FurnaceBrand> brands = BRANDS.get(world);
        if (brands == null || brands.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<BrandKey, FurnaceBrand>> iterator = brands.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BrandKey, FurnaceBrand> entry = iterator.next();
            LivingEntity owner = resolveLiving(world, entry.getKey().ownerId);
            LivingEntity target = resolveLiving(world, entry.getKey().targetId);
            FurnaceBrand brand = entry.getValue();
            LivingEntity sourceOwner = resolveLiving(world, brand.sourceOwnerId);
            if (owner == null
                    || target == null
                    || (brand.sourceOwnerId != null && sourceOwner == null)
                    || now >= brand.expiresAt
                    || !isValidTarget(owner, sourceOwner, target)) {
                discardVisual(world, brand.visualId);
                iterator.remove();
                continue;
            }

            updateVisualPosition(world, brand.visualId, target);
            if ((now + target.getId()) % 8L == 0L) {
                world.spawnParticles(
                        ParticleTypes.SMALL_FLAME,
                        target.getX(),
                        target.getBodyY(0.55),
                        target.getZ(),
                        2,
                        Math.max(0.1, target.getWidth() * 0.35),
                        Math.max(0.15, target.getHeight() * 0.2),
                        Math.max(0.1, target.getWidth() * 0.35),
                        0.01
                );
            }
        }

        if (brands.isEmpty()) {
            BRANDS.remove(world);
        }
    }

    private static void tickAbilities(ServerWorld world) {
        Map<UUID, ActiveChains> abilities = ACTIVE.get(world);
        if (abilities == null || abilities.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<UUID, ActiveChains>> abilityIterator = abilities.entrySet().iterator();
        while (abilityIterator.hasNext()) {
            ActiveChains ability = abilityIterator.next().getValue();
            LivingEntity actor = resolveLiving(world, ability.actorId);
            LivingEntity sourceOwner = resolveLiving(world, ability.sourceOwnerId);
            if (actor == null
                    || (ability.sourceOwnerId != null && sourceOwner == null)
                    || !isWieldingHearthflame(actor)) {
                discardAbilityVisuals(world, ability);
                UniqueAbilityApi.cancel(ability.execution);
                abilityIterator.remove();
                continue;
            }

            if (ability.chains.isEmpty()) {
                discardAbilityVisuals(world, ability);
                UniqueAbilityApi.cancel(ability.execution);
                abilityIterator.remove();
                continue;
            }

            float totalTensionGain = 0.0F;
            int tensionSamples = ability.chains.size();
            Iterator<FurnaceChain> chainIterator = ability.chains.iterator();
            while (chainIterator.hasNext()) {
                FurnaceChain chain = chainIterator.next();
                LivingEntity target = resolveLiving(world, chain.targetId);
                if (target == null || !isValidTarget(actor, sourceOwner, target)) {
                    discardVisual(world, chain.visualId);
                    chainIterator.remove();
                    continue;
                }
                if (actor.squaredDistanceTo(target) > MAX_BREAK_DISTANCE * MAX_BREAK_DISTANCE) {
                    discardVisual(world, chain.visualId);
                    chainIterator.remove();
                    continue;
                }

                float progress = MathHelper.clamp(
                        (float) (now - ability.startedAt) / Math.max(1.0F, ability.expiresAt - ability.startedAt),
                        0.0F,
                        1.0F
                );
                double minimumLength = Math.max(0.5, Config.uniqueEffects.hearthflame.minimumChainLength);
                double restLength = MathHelper.lerp(progress, chain.initialLength, Math.min(chain.initialLength, minimumLength));
                double distance = actor.distanceTo(target);
                double excess = Math.max(0.0, distance - restLength);
                SizeResponse response = getSizeResponse(target);
                if (excess > 0.0) {
                    pullTarget(actor, target, excess, response.pullMultiplier);
                    float tensionGain = (float) (excess * TENSION_PER_EXCESS_BLOCK * response.tensionMultiplier);
                    chain.tension = Math.min(MAX_TENSION, chain.tension + tensionGain);
                    totalTensionGain += tensionGain;
                }

                updateChainVisual(world, actor, target, chain);
                if (chain.tension >= MAX_TENSION) {
                    snapChain(world, actor, sourceOwner, ability, chain);
                    chainIterator.remove();
                    ability.pressure = Math.min(maximumPressure(ability), ability.pressure + SNAP_PRESSURE);
                }
            }

            if (tensionSamples > 0) {
                ability.pressure = Math.min(maximumPressure(ability), ability.pressure + totalTensionGain / tensionSamples);
            }
            updatePrimed(world, actor, ability);
            updateVisuals(world, actor, ability);

            if (ability.chains.isEmpty()) {
                discardAbilityVisuals(world, ability);
                abilityIterator.remove();
                continue;
            }
            if (now >= ability.expiresAt) {
                float strength = 0.5F + 0.5F * pressureFraction(ability);
                finishAbility(world, actor, sourceOwner, ability, strength, false);
                abilityIterator.remove();
            }
        }

        if (abilities.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void echoHit(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                ActiveChains ability, LivingEntity struckTarget) {
        for (FurnaceChain chain : ability.chains) {
            if (chain.targetId.equals(struckTarget.getUuid())) {
                continue;
            }
            LivingEntity echoTarget = resolveLiving(world, chain.targetId);
            if (echoTarget == null || !isValidTarget(actor, sourceOwner, echoTarget)) {
                continue;
            }
            applyAbilityDamage(world, actor, sourceOwner, ability.stack, echoTarget, ability.echoDamage, true);
            int fireTicks = ability.tuning.integer(Phase5AbilityTuning.Setting.FIRE_TICKS, 0);
            if (fireTicks > 0) echoTarget.setOnFireFor(Math.max(1, fireTicks / 20));
            spawnEchoEffects(world, echoTarget);
        }
        world.playSoundFromEntity(
                null,
                struckTarget,
                SoundEvents.BLOCK_CHAIN_HIT,
                struckTarget.getSoundCategory(),
                0.65F,
                0.75F + pressureFraction(ability) * 0.35F
        );
    }

    private static void snapChain(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                  ActiveChains ability, FurnaceChain chain) {
        LivingEntity target = resolveLiving(world, chain.targetId);
        if (target != null && isValidTarget(actor, sourceOwner, target)) {
            damageArea(
                    world,
                    actor,
                    sourceOwner,
                    ability.stack,
                    target.getPos().add(0.0, target.getHeight() * 0.45, 0.0),
                    ability.tuning.get(Phase5AbilityTuning.Setting.RADIUS,
                            Config.uniqueEffects.hearthflame.snapRadius),
                    ability.snapDamage,
                    new HashSet<>()
            );
            spawnSnapEffects(world, target);
        }
        beginVisualTransition(world, chain.visualId, FurnaceChainVisualEntity.MODE_SNAP, SNAP_VISUAL_TICKS, 1.0F);
    }

    private static void finishAbility(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                      ActiveChains ability, float strength, boolean struckFinale) {
        Set<UUID> damaged = new HashSet<>();
        for (FurnaceChain chain : ability.chains) {
            LivingEntity target = resolveLiving(world, chain.targetId);
            if (target != null && isValidTarget(actor, sourceOwner, target)) {
                damageArea(
                        world,
                        actor,
                        sourceOwner,
                        ability.stack,
                        target.getPos().add(0.0, target.getHeight() * 0.45, 0.0),
                        ability.tuning.get(Phase5AbilityTuning.Setting.RADIUS,
                                Config.uniqueEffects.hearthflame.snapRadius),
                        ability.finalDamage * MathHelper.clamp(strength, 0.0F, 1.0F),
                        damaged
                );
                spawnFinalTargetEffects(world, target);
            }
            beginVisualTransition(world, chain.visualId, FurnaceChainVisualEntity.MODE_SNAP, SNAP_VISUAL_TICKS, 1.0F);
        }
        ability.chains.clear();

        beginVisualTransition(
                world,
                ability.coreVisualId,
                FurnaceChainVisualEntity.MODE_FINALE,
                FINALE_VISUAL_TICKS,
                MathHelper.clamp(strength, 0.0F, 1.0F)
        );
        spawnFinaleEffects(world, actor, strength, struckFinale);
        UniqueAbilityApi.finish(ability.execution, Phase5UniqueAbilities.FINISH, damaged.size());
    }

    private static void damageArea(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                   ItemStack stack, Vec3d center, double radius, float damage, Set<UUID> damaged) {
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (damaged.contains(target.getUuid())
                    || !isValidTarget(actor, sourceOwner, target)
                    || target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).squaredDistanceTo(center) > radius * radius) {
                continue;
            }
            damaged.add(target.getUuid());
            applyAbilityDamage(world, actor, sourceOwner, stack, target, damage, false);
        }
    }

    private static boolean applyAbilityDamage(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                              ItemStack stack, LivingEntity target, float damage, boolean preserveVelocity) {
        LivingEntity attributedOwner = sourceOwner == null ? actor : sourceOwner;
        DamageSource source = world.getDamageSources().indirectMagic(actor, attributedOwner);
        float finalDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage);
        Vec3d previousVelocity = target.getVelocity();
        boolean damaged = HelperMethods.damageThroughIframes(target, source, finalDamage);
        if (damaged && preserveVelocity) {
            target.setVelocity(previousVelocity);
            target.velocityModified = true;
            target.velocityDirty = true;
        }
        if (damaged) {
            target.setOnFireFor(Math.max(0, Config.uniqueEffects.hearthflame.igniteSeconds));
        }
        return damaged;
    }

    private static void pullTarget(LivingEntity actor, LivingEntity target, double excess, double pullMultiplier) {
        Vec3d direction = actor.getPos().add(0.0, actor.getHeight() * 0.45, 0.0)
                .subtract(target.getPos().add(0.0, target.getHeight() * 0.45, 0.0));
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        direction = direction.normalize();
        double strength = Math.min(
                0.35,
                (getActive((ServerWorld) actor.getWorld(), actor) == null
                        ? Math.max(0.0, Config.uniqueEffects.hearthflame.pullStrength)
                        : getActive((ServerWorld) actor.getWorld(), actor).tuning.get(
                                Phase5AbilityTuning.Setting.PULL_STRENGTH,
                                Config.uniqueEffects.hearthflame.pullStrength))
                        * pullMultiplier
                        * Math.min(2.0, 0.75 + excess * 0.25)
        );
        target.addVelocity(direction.x * strength, MathHelper.clamp(direction.y * strength, -0.08, 0.08), direction.z * strength);
        target.velocityModified = true;
        target.velocityDirty = true;
    }

    private static SizeResponse getSizeResponse(LivingEntity target) {
        double size = Math.max(1.0, Math.max(target.getWidth(), target.getHeight() / 1.8));
        double resistance = MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE),
                0.0,
                1.0
        );
        double pullMultiplier = MathHelper.clamp(1.0 / (size * (1.0 + resistance * 2.0)), 0.15, 1.0);
        double tensionMultiplier = MathHelper.clamp(size * (1.0 + resistance), 1.0, 3.0);
        return new SizeResponse(pullMultiplier, tensionMultiplier);
    }

    private static List<LivingEntity> findTargets(WeaponAbilityContext context) {
        return findTargets(context, Phase5AbilityTuning.EMPTY);
    }

    private static List<LivingEntity> findTargets(WeaponAbilityContext context, Phase5AbilityTuning tuning) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        LivingEntity sourceOwner = context.sourcePlayer();
        double radius = tuning.get(Phase5AbilityTuning.Setting.RANGE, Config.uniqueEffects.hearthflame.radius);
        double brandedRadius = radius + Math.max(0.0, Config.uniqueEffects.hearthflame.brandedRangeBonus);
        int maxChains = tuning.integer(Phase5AbilityTuning.Setting.TARGET_CAP,
                Config.uniqueEffects.hearthflame.maxChains);
        Box box = Box.of(actor.getPos().add(0.0, actor.getHeight() * 0.5, 0.0),
                brandedRadius * 2.0, brandedRadius * 2.0, brandedRadius * 2.0);

        List<LivingEntity> targets = world.getEntitiesByClass(
                        LivingEntity.class,
                        box,
                        target -> isValidTarget(actor, sourceOwner, target)
                ).stream()
                .filter(target -> {
                    boolean branded = hasBrand(world, actor, target);
                    double allowed = branded ? brandedRadius : radius;
                    return actor.squaredDistanceTo(target) <= allowed * allowed;
                })
                .sorted(Comparator
                        .comparingInt((LivingEntity target) -> target == context.target() ? 0 : 1)
                        .thenComparingInt(target -> hasBrand(world, actor, target) ? 0 : 1)
                        .thenComparingDouble(actor::squaredDistanceTo))
                .limit(maxChains)
                .toList();
        return new ArrayList<>(targets);
    }

    private static boolean isValidTarget(LivingEntity actor, LivingEntity sourceOwner, LivingEntity target) {
        return actor != null
                && actor.isAlive()
                && target != null
                && target.isAlive()
                && target != actor
                && target.getWorld() == actor.getWorld()
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null
                || target != sourceOwner && HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static boolean isWieldingHearthflame(LivingEntity actor) {
        return actor.getMainHandStack().isOf(ItemsRegistry.HEARTHFLAME.get())
                || actor.getOffHandStack().isOf(ItemsRegistry.HEARTHFLAME.get());
    }

    private static boolean isActive(ServerWorld world, LivingEntity actor) {
        Map<UUID, ActiveChains> active = ACTIVE.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    private static ActiveChains getActive(ServerWorld world, LivingEntity actor) {
        Map<UUID, ActiveChains> active = ACTIVE.get(world);
        return active == null ? null : active.get(actor.getUuid());
    }

    private static void removeActive(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveChains> active = ACTIVE.get(world);
        if (active == null) {
            return;
        }
        active.remove(actorId);
        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static FurnaceChain findChain(ActiveChains ability, UUID targetId) {
        for (FurnaceChain chain : ability.chains) {
            if (chain.targetId.equals(targetId)) {
                return chain;
            }
        }
        return null;
    }

    private static void updatePrimed(ServerWorld world, LivingEntity actor, ActiveChains ability) {
        boolean wasPrimed = ability.primed;
        ability.primed = ability.pressure >= maximumPressure(ability);
        if (ability.primed && !wasPrimed) {
            world.playSoundFromEntity(
                    null,
                    actor,
                    SoundRegistry.SPELL_FIRE.get(),
                    actor.getSoundCategory(),
                    0.9F,
                    0.65F
            );
            world.playSoundFromEntity(
                    null,
                    actor,
                    SoundEvents.BLOCK_ANVIL_USE,
                    actor.getSoundCategory(),
                    0.75F,
                    1.35F
            );
            world.spawnParticles(ParticleTypes.FLAME, actor.getX(), actor.getBodyY(0.6), actor.getZ(),
                    28, 0.65, 0.7, 0.65, 0.06);
        }
    }

    private static boolean rollBrand(LivingEntity actor, Phase5AbilityTuning tuning) {
        int chance = tuning.integer(Phase5AbilityTuning.Setting.CHANCE,
                Math.clamp(Config.uniqueEffects.hearthflame.chance, 0, 100));
        return chance > 0 && actor.getRandom().nextInt(100) < chance;
    }

    private static void applyBrand(ServerWorld world, LivingEntity owner,
                                   LivingEntity sourceOwner, LivingEntity target, Phase5AbilityTuning tuning) {
        BrandKey key = new BrandKey(owner.getUuid(), target.getUuid());
        Map<BrandKey, FurnaceBrand> brands = BRANDS.computeIfAbsent(world, ignored -> new HashMap<>());
        FurnaceBrand previous = brands.remove(key);
        if (previous != null) {
            discardVisual(world, previous.visualId);
        }

        int duration = tuning.integer(Phase5AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.hearthflame.brandDuration);
        FurnaceChainVisualEntity visual = spawnVisual(
                world,
                owner,
                target,
                FurnaceChainVisualEntity.MODE_BRAND,
                duration + 20
        );
        brands.put(key, new FurnaceBrand(
                world.getTime() + duration,
                sourceOwner == null ? null : sourceOwner.getUuid(),
                visual == null ? null : visual.getUuid()
        ));
        target.setOnFireFor(Math.max(0, Config.uniqueEffects.hearthflame.igniteSeconds));
        world.playSoundFromEntity(
                null,
                target,
                SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(),
                target.getSoundCategory(),
                0.45F,
                0.85F
        );
        world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.55), target.getZ(),
                12, 0.25, 0.35, 0.25, 0.03);
    }

    private static boolean hasBrand(ServerWorld world, LivingEntity owner, LivingEntity target) {
        Map<BrandKey, FurnaceBrand> brands = BRANDS.get(world);
        if (brands == null) {
            return false;
        }
        FurnaceBrand brand = brands.get(new BrandKey(owner.getUuid(), target.getUuid()));
        return brand != null && brand.expiresAt > world.getTime();
    }

    private static boolean consumeBrand(ServerWorld world, LivingEntity owner, LivingEntity target) {
        Map<BrandKey, FurnaceBrand> brands = BRANDS.get(world);
        if (brands == null) {
            return false;
        }
        FurnaceBrand brand = brands.remove(new BrandKey(owner.getUuid(), target.getUuid()));
        if (brand == null) {
            return false;
        }
        discardVisual(world, brand.visualId);
        if (brands.isEmpty()) {
            BRANDS.remove(world);
        }
        return brand.expiresAt > world.getTime();
    }

    private static FurnaceChainVisualEntity spawnVisual(ServerWorld world, Entity owner, Entity target,
                                                        int mode, int lifetime) {
        if (!Config.general.enableModernFieldEffects) {
            return null;
        }
        FurnaceChainVisualEntity visual = new FurnaceChainVisualEntity(world, owner, target, mode, lifetime);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        MANAGED_VISUALS.computeIfAbsent(world, ignored -> new HashSet<>()).add(visual.getUuid());
        return visual;
    }

    private static void updateVisuals(ServerWorld world, LivingEntity actor, ActiveChains ability) {
        updateVisualPosition(world, ability.coreVisualId, actor);
        Entity coreEntity = ability.coreVisualId == null ? null : world.getEntity(ability.coreVisualId);
        if (coreEntity instanceof FurnaceChainVisualEntity coreVisual) {
            coreVisual.setHeat(pressureFraction(ability));
        }
        for (FurnaceChain chain : ability.chains) {
            LivingEntity target = resolveLiving(world, chain.targetId);
            if (target != null) {
                updateChainVisual(world, actor, target, chain);
                if (ability.primed) {
                    Entity entity = chain.visualId == null ? null : world.getEntity(chain.visualId);
                    if (entity instanceof FurnaceChainVisualEntity visual) {
                        visual.setHeat(1.0F);
                    }
                }
            }
        }
    }

    private static void updateChainVisual(ServerWorld world, LivingEntity actor,
                                          LivingEntity target, FurnaceChain chain) {
        Entity entity = chain.visualId == null ? null : world.getEntity(chain.visualId);
        if (entity instanceof FurnaceChainVisualEntity visual) {
            visual.setPosition(actor.getX(), actor.getY(), actor.getZ());
            visual.setOwnerId(actor.getId());
            visual.setTargetId(target.getId());
            visual.setHeat(MathHelper.clamp(chain.tension / MAX_TENSION, 0.0F, 1.0F));
        }
    }

    private static void updateVisualPosition(ServerWorld world, UUID visualId, LivingEntity anchor) {
        Entity entity = visualId == null ? null : world.getEntity(visualId);
        if (entity instanceof FurnaceChainVisualEntity visual) {
            visual.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
            if (visual.getMode() == FurnaceChainVisualEntity.MODE_BRAND) {
                visual.setTargetId(anchor.getId());
            } else {
                visual.setOwnerId(anchor.getId());
            }
        }
    }

    private static void pulseChains(ServerWorld world, ActiveChains ability) {
        for (FurnaceChain chain : ability.chains) {
            Entity entity = chain.visualId == null ? null : world.getEntity(chain.visualId);
            if (entity instanceof FurnaceChainVisualEntity visual) {
                visual.triggerPulse();
            }
        }
        Entity coreEntity = ability.coreVisualId == null ? null : world.getEntity(ability.coreVisualId);
        if (coreEntity instanceof FurnaceChainVisualEntity visual) {
            visual.triggerPulse();
        }
    }

    private static float maximumPressure() {
        return Math.max(1.0F, Config.uniqueEffects.hearthflame.maximumPressure);
    }

    private static float maximumPressure(ActiveChains ability) {
        return (float) ability.tuning.get(Phase5AbilityTuning.Setting.MAX_PRESSURE, maximumPressure());
    }

    private static float pressureFraction(ActiveChains ability) {
        return MathHelper.clamp(ability.pressure / maximumPressure(ability), 0.0F, 1.0F);
    }

    private static void beginVisualTransition(ServerWorld world, UUID visualId,
                                              int mode, int lifetime, float heat) {
        if (visualId == null) {
            return;
        }
        unmanageVisual(world, visualId);
        Entity entity = world.getEntity(visualId);
        if (entity instanceof FurnaceChainVisualEntity visual) {
            visual.setHeat(heat);
            visual.beginTransition(mode, lifetime);
        }
    }

    private static void discardAbilityVisuals(ServerWorld world, ActiveChains ability) {
        discardVisual(world, ability.coreVisualId);
        for (FurnaceChain chain : ability.chains) {
            discardVisual(world, chain.visualId);
        }
        ability.chains.clear();
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return;
        }
        unmanageVisual(world, visualId);
        Entity entity = world.getEntity(visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void unmanageVisual(ServerWorld world, UUID visualId) {
        Set<UUID> visuals = MANAGED_VISUALS.get(world);
        if (visuals == null) {
            return;
        }
        visuals.remove(visualId);
        if (visuals.isEmpty()) {
            MANAGED_VISUALS.remove(world);
        }
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof FurnaceChainVisualEntity visual
                    && visual.getCommandTags().contains(VISUAL_TAG)
                    && (visual.getMode() == FurnaceChainVisualEntity.MODE_BRAND
                    || visual.getMode() == FurnaceChainVisualEntity.MODE_CHAIN
                    || visual.getMode() == FurnaceChainVisualEntity.MODE_CORE)
                    && !isManagedVisual(world, visual.getUuid())) {
                visual.discard();
            }
        }
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor, List<LivingEntity> targets) {
        world.playSoundFromEntity(null, actor, SoundEvents.BLOCK_CHAIN_PLACE,
                actor.getSoundCategory(), 1.1F, 0.65F);
        world.playSoundFromEntity(null, actor, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                actor.getSoundCategory(), 0.75F, 0.55F);
        world.spawnParticles(ParticleTypes.LAVA, actor.getX(), actor.getBodyY(0.45), actor.getZ(),
                12, 0.55, 0.45, 0.55, 0.03);
        for (LivingEntity target : targets) {
            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(),
                    7, 0.2, 0.35, 0.2, 0.025);
        }
    }

    private static void spawnEchoEffects(ServerWorld world, LivingEntity target) {
        world.spawnParticles(ParticleTypes.SMALL_FLAME, target.getX(), target.getBodyY(0.55), target.getZ(),
                6, 0.2, 0.25, 0.2, 0.025);
        world.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getBodyY(0.45), target.getZ(),
                4, 0.18, 0.2, 0.18, 0.02);
    }

    private static void spawnSnapEffects(ServerWorld world, LivingEntity target) {
        world.playSoundFromEntity(null, target, SoundEvents.BLOCK_CHAIN_BREAK,
                target.getSoundCategory(), 1.0F, 0.7F);
        world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                target.getSoundCategory(), 0.7F, 0.8F);
        world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.45), target.getZ(),
                22, 0.55, 0.55, 0.55, 0.08);
        world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(0.35), target.getZ(),
                7, 0.4, 0.4, 0.4, 0.04);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getBodyY(0.45), target.getZ(),
                12, 0.45, 0.5, 0.45, 0.045);
    }

    private static void spawnFinalTargetEffects(ServerWorld world, LivingEntity target) {
        world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.45), target.getZ(),
                28, 0.7, 0.65, 0.7, 0.1);
        world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(0.35), target.getZ(),
                10, 0.45, 0.5, 0.45, 0.08);
    }

    private static void spawnFinaleEffects(ServerWorld world, LivingEntity actor, float strength, boolean struckFinale) {
        float clamped = MathHelper.clamp(strength, 0.0F, 1.0F);
        world.playSoundFromEntity(null, actor, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY,
                actor.getSoundCategory(), 1.4F, struckFinale ? 0.68F : 0.78F);
        world.playSoundFromEntity(null, actor, SoundEvents.BLOCK_ANVIL_LAND,
                actor.getSoundCategory(), 1.2F, 0.55F);
        world.playSoundFromEntity(null, actor, SoundRegistry.SPELL_FIRE.get(),
                actor.getSoundCategory(), 1.0F, 0.6F);
        world.spawnParticles(ParticleTypes.FLAME, actor.getX(), actor.getY() + 0.3, actor.getZ(),
                45 + (int) (35 * clamped), 1.2, 1.25, 1.2, 0.13);
        world.spawnParticles(ParticleTypes.LAVA, actor.getX(), actor.getY() + 0.2, actor.getZ(),
                12 + (int) (10 * clamped), 0.85, 0.55, 0.85, 0.1);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, actor.getX(), actor.getY() + 0.6, actor.getZ(),
                26 + (int) (18 * clamped), 1.0, 1.3, 1.0, 0.09);
    }

    private static final class ActiveChains {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final ItemStack stack;
        private final long startedAt;
        private final long expiresAt;
        private final float echoDamage;
        private final float snapDamage;
        private final float finalDamage;
        private final Phase5AbilityTuning tuning;
        private final UniqueAbilityExecution execution;
        private final List<FurnaceChain> chains = new ArrayList<>();
        private UUID coreVisualId;
        private float pressure;
        private boolean primed;

        private ActiveChains(UUID actorId, UUID sourceOwnerId, ItemStack stack,
                             long startedAt, long expiresAt,
                             float echoDamage, float snapDamage, float finalDamage,
                             Phase5AbilityTuning tuning, UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.stack = stack;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
            this.echoDamage = echoDamage;
            this.snapDamage = snapDamage;
            this.finalDamage = finalDamage;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private static final class FurnaceChain {
        private final UUID targetId;
        private final UUID visualId;
        private final double initialLength;
        private float tension;

        private FurnaceChain(UUID targetId, UUID visualId, double initialLength, float tension) {
            this.targetId = targetId;
            this.visualId = visualId;
            this.initialLength = initialLength;
            this.tension = tension;
        }
    }

    private record BrandKey(UUID ownerId, UUID targetId) {
    }

    private record FurnaceBrand(long expiresAt, UUID sourceOwnerId, UUID visualId) {
    }

    private record SizeResponse(double pullMultiplier, double tensionMultiplier) {
    }
}
