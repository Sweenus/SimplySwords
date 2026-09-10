package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryTuning;
import net.sweenus.simplyswords.api.ability.FireForgeMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
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
    private static final Identifier BASTION_SPEED_ID = Identifier.of("simplyswords", "hearthflame_bastion_speed");

    private static final Map<ServerWorld, Map<BrandKey, FurnaceBrand>> BRANDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ActiveChains>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, Set<UUID>> MANAGED_VISUALS = new HashMap<>();
    private static final Map<ServerWorld, Map<BrandKey, Long>> BRAND_SHELTER_LOCKOUTS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> REACTIVE_BRAND_LOCKOUTS = new HashMap<>();
    private static final Map<ServerWorld, Long> ABSORPTION_SWEEP_UNTIL = new HashMap<>();

    private HearthflameAbilityManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<BrandKey, FurnaceBrand> brands = BRANDS.get(world);
        Map<UUID, ActiveChains> active = ACTIVE.get(world);
        return (brands != null && !brands.isEmpty())
                || (active != null && !active.isEmpty())
                || hasPendingLockouts(world)
                || ABSORPTION_SWEEP_UNTIL.getOrDefault(world, 0L) >= world.getTime()
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
        return !findTargets(context, FireForgeMasteryTuning.EMPTY.with(
                FireForgeMasteryTuning.Setting.HEARTH_BIND_RANGE,
                Config.uniqueEffects.hearthflame.radius + 2)).isEmpty();
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginActive(FireForgeMasteryAbilities.HEARTHFLAME_CHAINS,
                context, Config.uniqueEffects.hearthflame.cooldown);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
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
        int duration = tunedInteger(tuning, FireForgeMasteryTuning.Setting.HEARTH_CHAIN_DURATION_TICKS,
                FireForgeMasteryTuning.Setting.DURATION_TICKS, Config.uniqueEffects.hearthflame.duration);
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
                ) * (float) tuned(tuning, FireForgeMasteryTuning.Setting.HEARTH_ECHO_DAMAGE_MULTIPLIER,
                        FireForgeMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1),
                HelperMethods.abilityScaledDamage(
                        "fire",
                        actor,
                        context.stack(),
                        Config.uniqueEffects.hearthflame.snapDamageScaling,
                        Config.uniqueEffects.hearthflame.snapSpellScaling
                ) * (float) tuned(tuning, FireForgeMasteryTuning.Setting.HEARTH_SNAP_DAMAGE_MULTIPLIER,
                        FireForgeMasteryTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1),
                HelperMethods.abilityScaledDamage(
                        "fire",
                        actor,
                        context.stack(),
                        Config.uniqueEffects.hearthflame.finalDamageScaling,
                        Config.uniqueEffects.hearthflame.finalSpellScaling
                ) * (float) tuned(tuning, FireForgeMasteryTuning.Setting.HEARTH_FINAL_DAMAGE_MULTIPLIER,
                        FireForgeMasteryTuning.Setting.FINAL_DAMAGE_MULTIPLIER, 1),
                tuning,
                execution,
                actor.getPos()
        );
        ability.bastion = tuning.flag(1 << 16);

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
                    Math.max(minimumLength(tuning), actor.distanceTo(target)),
                    branded ? BRAND_STARTING_TENSION : 0.0F,
                    branded,
                    0,
                    1.0F,
                    forcedSnapAt(now, tuning)
            ));
        }
        ability.initialChainCount = ability.chains.size();

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), ability);
        if (tuning.has(FireForgeMasteryTuning.Setting.HEARTH_CAST_ABSORPTION)) {
            grantTimedAbsorption(world, actor, "hearthflame/cast",
                    (float) tuning.get(FireForgeMasteryTuning.Setting.HEARTH_CAST_ABSORPTION, 4),
                    tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_CAST_ABSORPTION_DURATION_TICKS, 80),
                    (float) tuning.get(FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION_CAP, 8));
        }
        if (ability.bastion) {
            StatusEffectInstance existing = actor.getStatusEffect(StatusEffects.RESISTANCE);
            ability.previousResistance = existing == null ? null : new StatusEffectInstance(existing);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1), actor);
            applyBastionMovementPenalty(actor, tuning);
        }
        if (tuning.flag(1 << 17)) {
            StatusEffectInstance existing = actor.getStatusEffect(StatusEffects.SPEED);
            ability.previousSpeed = existing == null ? null : new StatusEffectInstance(existing);
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 0), actor);
            ability.roaming = true;
        }
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
                long now = world.getTime();
                int preserveTicks = ability.tuning.integer(
                        FireForgeMasteryTuning.Setting.HEARTH_CHAIN_PRESERVE_TICKS, 0);
                if (preserveTicks > 0 && !struckChain.preserved) {
                    struckChain.preserved = true;
                    struckChain.preserveUntil = now + preserveTicks;
                } else if (now >= struckChain.preserveUntil) {
                    ability.chains.remove(struckChain);
                    FurnaceChain rebound = snapChain(world, actor, sourceOwner, ability, struckChain);
                    if (rebound != null) ability.chains.add(rebound);
                    addSnapPressure(ability);
                    grantSnapResistance(actor, ability);
                }
            }
            updatePrimed(world, actor, ability);
            updateVisuals(world, actor, ability);
            return;
        }

        UniqueAbilityExecution brandExecution = EmberWeaponsMasteryManager.beginPassive(FireForgeMasteryAbilities.HEARTHFLAME_BRAND,
                world, stack, actor, target);
        FireForgeMasteryTuning brandTuning = FireForgeMasteryAbilities.tuning(brandExecution);
        if (hasBrand(world, actor, target)
                && brandTuning.has(FireForgeMasteryTuning.Setting.HEARTH_BRAND_HIT_DAMAGE_MULTIPLIER)) {
            applyAbilityDamage(world, actor, sourceOwner, stack, target,
                    (float) actor.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                            * ((float) brandTuning.get(
                            FireForgeMasteryTuning.Setting.HEARTH_BRAND_HIT_DAMAGE_MULTIPLIER, 1) - 1.0F), true,
                    brandTuning.integer(FireForgeMasteryTuning.Setting.HEARTH_BRAND_HIT_FIRE_TICKS, 0));
        }
        if (hasBrand(world, actor, target)
                && brandTuning.has(FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION)
                && claimLockout(BRAND_SHELTER_LOCKOUTS, world,
                new BrandKey(actor.getUuid(), target.getUuid()),
                brandTuning.integer(FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION_LOCKOUT_TICKS, 40))) {
            grantTimedAbsorption(world, actor, "hearthflame/brand",
                    (float) brandTuning.get(FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION, 2),
                    brandTuning.integer(
                            FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION_DURATION_TICKS, 200),
                    (float) brandTuning.get(FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION_CAP, 8));
        }
        if (rollBrand(actor, brandTuning)) applyBrand(world, actor, sourceOwner, target, brandTuning);
        UniqueAbilityApi.finish(brandExecution, FireForgeMasteryAbilities.FINISH, 1);
    }

    public static void tick(ServerWorld world) {
        MasteryAbsorptionTracker.sweep(world);
        tickBrands(world);
        tickAbilities(world);
        pruneTimedState(world);
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
                if (actor != null) cleanupActor(actor, ability);
                UniqueAbilityApi.cancel(ability.execution);
                abilityIterator.remove();
                continue;
            }

            if (ability.chains.isEmpty()) {
                discardAbilityVisuals(world, ability);
                cleanupActor(actor, ability);
                UniqueAbilityApi.cancel(ability.execution);
                abilityIterator.remove();
                continue;
            }

            float totalTensionGain = 0.0F;
            int tensionSamples = ability.chains.size();
            List<FurnaceChain> rebounds = new ArrayList<>();
            Iterator<FurnaceChain> chainIterator = ability.chains.iterator();
            while (chainIterator.hasNext()) {
                FurnaceChain chain = chainIterator.next();
                LivingEntity target = resolveLiving(world, chain.targetId);
                if (target == null || !isValidTarget(actor, sourceOwner, target)) {
                    discardVisual(world, chain.visualId);
                    chainIterator.remove();
                    continue;
                }
                Vec3d anchor = anchor(ability, actor);
                double breakRange = ability.tuning.get(FireForgeMasteryTuning.Setting.HEARTH_BREAK_RANGE,
                        MAX_BREAK_DISTANCE);
                if (anchor.squaredDistanceTo(target.getPos()) > breakRange * breakRange) {
                    discardVisual(world, chain.visualId);
                    chainIterator.remove();
                    continue;
                }

                if (chain.snapAt > 0 && now >= chain.snapAt) {
                    chainIterator.remove();
                    FurnaceChain rebound = snapChain(world, actor, sourceOwner, ability, chain);
                    if (rebound != null) rebounds.add(rebound);
                    addSnapPressure(ability);
                    grantSnapResistance(actor, ability);
                    continue;
                }

                if (chain.snapAt <= 0) {
                    float progress = MathHelper.clamp(
                            (float) (now - ability.startedAt) / Math.max(1.0F, ability.expiresAt - ability.startedAt),
                            0.0F,
                            1.0F
                    );
                    double minimumLength = minimumLength(ability.tuning);
                    double restLength = MathHelper.lerp(progress, chain.initialLength,
                            Math.min(chain.initialLength, minimumLength));
                    double distance = anchor.distanceTo(target.getPos());
                    double excess = Math.max(0.0, distance - restLength);
                    SizeResponse response = getSizeResponse(target);
                    if (excess > 0.0) {
                        pullTarget(ability.tuning, anchor, actor, target, excess, response.pullMultiplier);
                        float tensionGain = (float) (excess * TENSION_PER_EXCESS_BLOCK * response.tensionMultiplier);
                        chain.tension = Math.min(MAX_TENSION, chain.tension + tensionGain);
                        totalTensionGain += tensionGain;
                    }
                }

                updateChainVisual(world, actor, target, ability, chain);
                if (chain.tension >= MAX_TENSION && now >= chain.preserveUntil) {
                    chainIterator.remove();
                    FurnaceChain rebound = snapChain(world, actor, sourceOwner, ability, chain);
                    if (rebound != null) rebounds.add(rebound);
                    addSnapPressure(ability);
                    grantSnapResistance(actor, ability);
                }
            }
            ability.chains.addAll(rebounds);

            if (tensionSamples > 0) {
                ability.pressure = Math.min(maximumPressure(ability), ability.pressure + totalTensionGain / tensionSamples);
            }
            updatePrimed(world, actor, ability);
            updateVisuals(world, actor, ability);

            if (ability.chains.isEmpty()) {
                discardAbilityVisuals(world, ability);
                grantCompletionAbsorption(world, actor, ability);
                cleanupActor(actor, ability);
                UniqueAbilityApi.finish(ability.execution, FireForgeMasteryAbilities.FINISH, ability.completedChains);
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
            applyAbilityDamage(world, actor, sourceOwner, ability.stack, echoTarget,
                    ability.echoDamage * chain.damageMultiplier, true,
                    tunedInteger(ability.tuning, FireForgeMasteryTuning.Setting.HEARTH_ECHO_FIRE_TICKS,
                            FireForgeMasteryTuning.Setting.FIRE_TICKS, 0));
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

    private static FurnaceChain snapChain(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                          ActiveChains ability, FurnaceChain chain) {
        LivingEntity target = resolveLiving(world, chain.targetId);
        if (target != null && isValidTarget(actor, sourceOwner, target)) {
            damageArea(
                    world,
                    actor,
                    sourceOwner,
                    ability.stack,
                    target.getPos().add(0.0, target.getHeight() * 0.45, 0.0),
                    tuned(ability.tuning, FireForgeMasteryTuning.Setting.HEARTH_SNAP_RADIUS,
                            FireForgeMasteryTuning.Setting.RADIUS,
                            Config.uniqueEffects.hearthflame.snapRadius),
                    ability.snapDamage * chain.damageMultiplier,
                    new HashSet<>(),
                    anchor(ability, actor),
                    0.0
            );
            spawnSnapEffects(world, target);
            ability.completedChains++;
            spreadBrand(world, actor, sourceOwner, ability, chain, target);
        }
        beginVisualTransition(world, chain.visualId, FurnaceChainVisualEntity.MODE_SNAP, SNAP_VISUAL_TICKS, 1.0F);
        return target == null ? null : createReboundChain(world, actor, sourceOwner, ability, chain, target);
    }

    private static void finishAbility(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                      ActiveChains ability, float strength, boolean struckFinale) {
        Set<UUID> damaged = new HashSet<>();
        for (FurnaceChain chain : ability.chains) {
            LivingEntity target = resolveLiving(world, chain.targetId);
            if (target != null && isValidTarget(actor, sourceOwner, target)) {
                ability.completedChains++;
                float finalDamage = gatedFinalDamage(ability.finalDamage * chain.damageMultiplier,
                        ability.initialChainCount,
                        ability.tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_FINAL_CHAIN_REQUIREMENT, 0),
                        ability.tuning.get(FireForgeMasteryTuning.Setting.HEARTH_FINAL_CHAIN_MULTIPLIER, 1));
                damageArea(
                        world,
                        actor,
                        sourceOwner,
                        ability.stack,
                        target.getPos().add(0.0, target.getHeight() * 0.45, 0.0),
                        tuned(ability.tuning, FireForgeMasteryTuning.Setting.HEARTH_SNAP_RADIUS,
                                FireForgeMasteryTuning.Setting.RADIUS,
                                Config.uniqueEffects.hearthflame.snapRadius),
                        finalDamage * MathHelper.clamp(strength, 0.0F, 1.0F),
                        damaged,
                        anchor(ability, actor),
                        ability.tuning.has(FireForgeMasteryTuning.Setting.HEARTH_FINAL_KNOCKBACK_MULTIPLIER)
                                ? ability.tuning.get(
                                FireForgeMasteryTuning.Setting.HEARTH_FINAL_KNOCKBACK_MULTIPLIER, 1) : 0.0
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
        spawnFinaleEffects(world, actor, anchor(ability, actor), strength, struckFinale);
        grantCompletionAbsorption(world, actor, ability);
        cleanupActor(actor, ability);
        UniqueAbilityApi.finish(ability.execution, FireForgeMasteryAbilities.FINISH, damaged.size());
    }

    private static void damageArea(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                   ItemStack stack, Vec3d center, double radius, float damage, Set<UUID> damaged,
                                   Vec3d knockbackOrigin, double knockbackMultiplier) {
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (damaged.contains(target.getUuid())
                    || !isValidTarget(actor, sourceOwner, target)
                    || target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).squaredDistanceTo(center) > radius * radius) {
                continue;
            }
            damaged.add(target.getUuid());
            if (applyAbilityDamage(world, actor, sourceOwner, stack, target, damage, false)
                    && knockbackMultiplier > 0) {
                Vec3d away = target.getPos().subtract(center);
                if (away.horizontalLengthSquared() <= 0.0001) {
                    away = target.getPos().subtract(knockbackOrigin);
                }
                if (away.horizontalLengthSquared() > 0.0001) {
                    target.takeKnockback(0.5 * knockbackMultiplier, -away.x, -away.z);
                }
            }
        }
    }

    private static boolean applyAbilityDamage(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                              ItemStack stack, LivingEntity target, float damage,
                                              boolean preserveVelocity) {
        return applyAbilityDamage(world, actor, sourceOwner, stack, target, damage, preserveVelocity, 0);
    }

    private static boolean applyAbilityDamage(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                              ItemStack stack, LivingEntity target, float damage,
                                              boolean preserveVelocity, int igniteTicks) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
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
            target.setOnFireFor(igniteSeconds(Config.uniqueEffects.hearthflame.igniteSeconds, igniteTicks));
        }
        return damaged;
        }
    }

    private static void pullTarget(FireForgeMasteryTuning tuning, Vec3d anchor, LivingEntity actor,
                                   LivingEntity target, double excess, double pullMultiplier) {
        Vec3d direction = anchor.add(0.0, actor.getHeight() * 0.45, 0.0)
                .subtract(target.getPos().add(0.0, target.getHeight() * 0.45, 0.0));
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        direction = direction.normalize();
        double strength = pullStrength(
                tuning.get(FireForgeMasteryTuning.Setting.PULL_STRENGTH,
                        Config.uniqueEffects.hearthflame.pullStrength),
                pullMultiplier, excess);
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

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world) || source == null || amount <= 0) {
            return amount;
        }

        ActiveChains ability = getActive(world, target);
        if (ability != null) {
            if (source.isIn(DamageTypeTags.IS_FIRE)
                    && ability.tuning.has(FireForgeMasteryTuning.Setting.HEARTH_FIRE_DAMAGE_REDUCTION)) {
                amount = reducedDamage(amount, ability.tuning.get(
                        FireForgeMasteryTuning.Setting.HEARTH_FIRE_DAMAGE_REDUCTION, 0.3));
            }
            if (source.getAttacker() instanceof LivingEntity attacker
                    && findChain(ability, attacker.getUuid()) != null
                    && target.squaredDistanceTo(attacker) <= MathHelper.square(ability.tuning.get(
                    FireForgeMasteryTuning.Setting.HEARTH_BOUND_DAMAGE_REDUCTION_RANGE, 6))) {
                amount = reducedDamage(amount, ability.tuning.get(
                        FireForgeMasteryTuning.Setting.HEARTH_BOUND_DAMAGE_REDUCTION, 0));
            }
        }

        return Math.max(0, amount);
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || source == null
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || !isMelee(source)) {
            return;
        }
        ItemStack stack = heldHearthflame(target);
        if (stack == null) return;
        UniqueAbilityExecution execution = EmberWeaponsMasteryManager.beginPassive(
                FireForgeMasteryAbilities.HEARTHFLAME_BRAND, world, stack, target, attacker);
        FireForgeMasteryTuning tuning = FireForgeMasteryAbilities.tuning(execution);
        int duration = tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_REACTIVE_BRAND_DURATION_TICKS, 0);
        boolean applied = duration > 0 && isValidTarget(target, null, attacker)
                && claimLockout(REACTIVE_BRAND_LOCKOUTS, world, target.getUuid(),
                tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_REACTIVE_BRAND_LOCKOUT_TICKS, 60));
        if (applied) applyBrand(world, target, null, attacker, tuning, duration);
        UniqueAbilityApi.finish(execution, FireForgeMasteryAbilities.FINISH, applied ? 1 : 0);
    }

    private static List<LivingEntity> findTargets(WeaponAbilityContext context) {
        return findTargets(context, FireForgeMasteryTuning.EMPTY);
    }

    private static List<LivingEntity> findTargets(WeaponAbilityContext context, FireForgeMasteryTuning tuning) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        LivingEntity sourceOwner = context.sourcePlayer();
        double radius = tuned(tuning, FireForgeMasteryTuning.Setting.HEARTH_BIND_RANGE,
                FireForgeMasteryTuning.Setting.RANGE, Config.uniqueEffects.hearthflame.radius);
        double brandedRadius = radius + Math.max(0.0, Config.uniqueEffects.hearthflame.brandedRangeBonus);
        int maxChains = tuning.integer(FireForgeMasteryTuning.Setting.TARGET_CAP,
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

    private static ItemStack heldHearthflame(LivingEntity actor) {
        if (actor.getMainHandStack().isOf(ItemsRegistry.HEARTHFLAME.get())) return actor.getMainHandStack();
        if (actor.getOffHandStack().isOf(ItemsRegistry.HEARTHFLAME.get())) return actor.getOffHandStack();
        return null;
    }

    private static boolean isMelee(DamageSource source) {
        return source.isOf(DamageTypes.PLAYER_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO);
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

    private static boolean rollBrand(LivingEntity actor, FireForgeMasteryTuning tuning) {
        int chance = tuning.integer(FireForgeMasteryTuning.Setting.CHANCE,
                Math.clamp(Config.uniqueEffects.hearthflame.chance, 0, 100));
        int roll = actor.getRandom().nextInt(100);
        boolean passed = chance > 0 && roll < chance;
        UniqueAbilityApi.reportRoll(actor, FireForgeMasteryAbilities.HEARTHFLAME_BRAND.id(),
                "CHANCE", chance, roll, passed);
        return passed;
    }

    private static void applyBrand(ServerWorld world, LivingEntity owner,
                                   LivingEntity sourceOwner, LivingEntity target, FireForgeMasteryTuning tuning) {
        applyBrand(world, owner, sourceOwner, target, tuning,
                tunedInteger(tuning, FireForgeMasteryTuning.Setting.HEARTH_BRAND_DURATION_TICKS,
                        FireForgeMasteryTuning.Setting.DURATION_TICKS,
                        Config.uniqueEffects.hearthflame.brandDuration));
    }

    private static void applyBrand(ServerWorld world, LivingEntity owner,
                                   LivingEntity sourceOwner, LivingEntity target,
                                   FireForgeMasteryTuning tuning, int duration) {
        BrandKey key = new BrandKey(owner.getUuid(), target.getUuid());
        Map<BrandKey, FurnaceBrand> brands = BRANDS.computeIfAbsent(world, ignored -> new HashMap<>());
        FurnaceBrand previous = brands.remove(key);
        if (previous != null) {
            discardVisual(world, previous.visualId);
        }

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

    private static void spreadBrand(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                    ActiveChains ability, FurnaceChain snapped, LivingEntity center) {
        if (!snapped.branded) return;
        int count = ability.tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_BRAND_SPREAD_COUNT, 0);
        if (count <= 0) return;
        double range = ability.tuning.get(FireForgeMasteryTuning.Setting.HEARTH_BRAND_SPREAD_RANGE, 4);
        int duration = ability.tuning.integer(
                FireForgeMasteryTuning.Setting.HEARTH_BRAND_SPREAD_DURATION_TICKS, 100);
        Box box = Box.of(center.getPos(), range * 2, range * 2, range * 2);
        world.getEntitiesByClass(LivingEntity.class, box,
                        candidate -> candidate != center && isValidTarget(actor, sourceOwner, candidate))
                .stream()
                .filter(candidate -> center.squaredDistanceTo(candidate) <= range * range)
                .filter(candidate -> !hasBrand(world, actor, candidate))
                .sorted(Comparator.comparingDouble(center::squaredDistanceTo))
                .limit(count)
                .forEach(candidate -> applyBrand(world, actor, sourceOwner, candidate, ability.tuning, duration));
    }

    private static FurnaceChain createReboundChain(ServerWorld world, LivingEntity actor, LivingEntity sourceOwner,
                                                    ActiveChains ability, FurnaceChain snapped, LivingEntity center) {
        int maximumGeneration = ability.tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_REBIND_COUNT, 0);
        if (snapped.generation >= maximumGeneration) return null;
        double range = ability.tuning.get(FireForgeMasteryTuning.Setting.HEARTH_REBIND_RANGE, 5);
        Map<BrandKey, FurnaceBrand> brands = BRANDS.get(world);
        if (brands == null || brands.isEmpty()) return null;

        LivingEntity reboundTarget = brands.keySet().stream()
                .filter(key -> key.ownerId.equals(actor.getUuid()))
                .map(key -> resolveLiving(world, key.targetId))
                .filter(candidate -> candidate != null && candidate != center)
                .filter(candidate -> isValidTarget(actor, sourceOwner, candidate))
                .filter(candidate -> center.squaredDistanceTo(candidate) <= range * range)
                .filter(candidate -> findChain(ability, candidate.getUuid()) == null)
                .min(Comparator.comparingDouble(center::squaredDistanceTo))
                .orElse(null);
        if (reboundTarget == null || !consumeBrand(world, actor, reboundTarget)) return null;

        int duration = Math.max(1, (int) Math.min(Integer.MAX_VALUE, ability.expiresAt - world.getTime()));
        FurnaceChainVisualEntity visual = spawnVisual(world, actor, reboundTarget,
                FurnaceChainVisualEntity.MODE_CHAIN, duration + SNAP_VISUAL_TICKS + 20);
        double generationMultiplier = ability.tuning.get(
                FireForgeMasteryTuning.Setting.HEARTH_REBIND_DAMAGE_MULTIPLIER, 0.75);
        return new FurnaceChain(
                reboundTarget.getUuid(),
                visual == null ? null : visual.getUuid(),
                Math.max(minimumLength(ability.tuning), anchor(ability, actor).distanceTo(reboundTarget.getPos())),
                BRAND_STARTING_TENSION,
                true,
                snapped.generation + 1,
                rebindDamageMultiplier(generationMultiplier, snapped.generation + 1),
                forcedSnapAt(world.getTime(), ability.tuning)
        );
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
        Entity coreEntity = ability.coreVisualId == null ? null : world.getEntity(ability.coreVisualId);
        if (coreEntity instanceof FurnaceChainVisualEntity coreVisual) {
            if (ability.bastion) {
                coreVisual.setPosition(ability.castAnchor.x, ability.castAnchor.y, ability.castAnchor.z);
                coreVisual.setOwnerId(-1);
            } else {
                updateVisualPosition(world, ability.coreVisualId, actor);
            }
            coreVisual.setHeat(pressureFraction(ability));
        }
        for (FurnaceChain chain : ability.chains) {
            LivingEntity target = resolveLiving(world, chain.targetId);
            if (target != null) {
                updateChainVisual(world, actor, target, ability, chain);
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
                                          LivingEntity target, ActiveChains ability, FurnaceChain chain) {
        Entity entity = chain.visualId == null ? null : world.getEntity(chain.visualId);
        if (entity instanceof FurnaceChainVisualEntity visual) {
            Vec3d visualAnchor = anchor(ability, actor);
            visual.setPosition(visualAnchor.x, visualAnchor.y, visualAnchor.z);
            visual.setOwnerId(ability.bastion ? -1 : actor.getId());
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

    private static Vec3d anchor(ActiveChains ability, LivingEntity actor) {
        return ability.bastion ? ability.castAnchor : actor.getPos();
    }

    private static double minimumLength(FireForgeMasteryTuning tuning) {
        return Math.max(0.5, tuning.get(FireForgeMasteryTuning.Setting.HEARTH_MIN_LENGTH,
                Config.uniqueEffects.hearthflame.minimumChainLength));
    }

    private static double tuned(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                FireForgeMasteryTuning.Setting legacy, double fallback) {
        return tuning.has(scoped) ? tuning.get(scoped, fallback) : tuning.get(legacy, fallback);
    }

    private static int tunedInteger(FireForgeMasteryTuning tuning, FireForgeMasteryTuning.Setting scoped,
                                    FireForgeMasteryTuning.Setting legacy, int fallback) {
        return tuning.has(scoped) ? tuning.integer(scoped, fallback) : tuning.integer(legacy, fallback);
    }

    private static long forcedSnapAt(long now, FireForgeMasteryTuning tuning) {
        int ticks = tuning.integer(FireForgeMasteryTuning.Setting.HEARTH_FORCED_SNAP_TICKS, 0);
        return ticks <= 0 ? 0 : now + ticks;
    }

    private static void addSnapPressure(ActiveChains ability) {
        float multiplier = (float) ability.tuning.get(
                FireForgeMasteryTuning.Setting.HEARTH_SNAP_PRESSURE_MULTIPLIER, 1);
        ability.pressure = pressureAfterSnap(ability.pressure, maximumPressure(ability), multiplier);
    }

    static float pressureAfterSnap(float pressure, float maximum, double multiplier) {
        return Math.min(Math.max(0, maximum), Math.max(0, pressure) + SNAP_PRESSURE * (float) multiplier);
    }

    private static void grantSnapResistance(LivingEntity actor, ActiveChains ability) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(ability == null ? null : CombatProvenanceApi.from(ability.stack, null))) {
        int grant = ability.tuning.integer(
                FireForgeMasteryTuning.Setting.HEARTH_SNAP_RESISTANCE_DURATION_TICKS, 0);
        if (grant <= 0) return;
        int cap = Math.max(grant, ability.tuning.integer(
                FireForgeMasteryTuning.Setting.HEARTH_SNAP_RESISTANCE_MAX_TICKS, grant));
        StatusEffectInstance existing = actor.getStatusEffect(StatusEffects.RESISTANCE);
        int existingDuration = existing != null && existing.getAmplifier() == 0 ? existing.getDuration() : 0;
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                extendedResistanceTicks(existingDuration, grant, cap), 0), actor);
        }
    }

    static float gatedFinalDamage(float tunedDamage, int initialChains, int requiredChains, double multiplier) {
        return requiredChains > 0 && initialChains >= requiredChains
                ? tunedDamage * (float) multiplier : tunedDamage;
    }

    static int igniteSeconds(int baseSeconds, int tunedIgniteTicks) {
        return Math.max(Math.max(0, baseSeconds), Math.max(0, tunedIgniteTicks) / 20);
    }

    static float rebindDamageMultiplier(double perGeneration, int generation) {
        return (float) Math.pow(MathHelper.clamp(perGeneration, 0.0, 10.0), Math.max(0, generation));
    }

    static int extendedResistanceTicks(int existing, int grant, int cap) {
        return Math.min(Math.max(grant, cap), Math.max(0, existing) + Math.max(0, grant));
    }

    static double pullStrength(double tuned, double pullMultiplier, double excess) {
        return Math.min(0.35, Math.max(0.0, tuned) * pullMultiplier * Math.min(2.0, 0.75 + excess * 0.25));
    }

    static float reducedDamage(float amount, double reduction) {
        return Math.max(0, amount) * (1.0F - (float) MathHelper.clamp(reduction, 0.0, 1.0));
    }

    private static void grantTimedAbsorption(ServerWorld world, LivingEntity actor, String source, float amount, int ticks,
                                             float cap) {
        if (amount <= 0 || ticks <= 0) return;
        MasteryAbsorptionTracker.grant(actor, source, amount, ticks, Math.max(amount, cap));
        ABSORPTION_SWEEP_UNTIL.merge(world, world.getTime() + ticks, Math::max);
    }

    private static void grantCompletionAbsorption(ServerWorld world, LivingEntity actor, ActiveChains ability) {
        int completionMinimum = ability.tuning.integer(
                FireForgeMasteryTuning.Setting.HEARTH_COMPLETION_MIN_CHAINS, Integer.MAX_VALUE);
        if (ability.completedChains < completionMinimum
                || !ability.tuning.has(FireForgeMasteryTuning.Setting.HEARTH_COMPLETION_ABSORPTION)) return;
        grantTimedAbsorption(world, actor, "hearthflame/completion",
                (float) ability.tuning.get(FireForgeMasteryTuning.Setting.HEARTH_COMPLETION_ABSORPTION, 4),
                ability.tuning.integer(
                        FireForgeMasteryTuning.Setting.HEARTH_COMPLETION_ABSORPTION_DURATION_TICKS, 60),
                (float) ability.tuning.get(FireForgeMasteryTuning.Setting.HEARTH_BRAND_ABSORPTION_CAP, 8));
    }

    private static void applyBastionMovementPenalty(LivingEntity actor, FireForgeMasteryTuning tuning) {
        EntityAttributeInstance movement = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement == null) return;
        movement.removeModifier(BASTION_SPEED_ID);
        double multiplier = tuning.get(FireForgeMasteryTuning.Setting.HEARTH_ANCHOR_SPEED_MULTIPLIER, 0.75);
        movement.addTemporaryModifier(new EntityAttributeModifier(BASTION_SPEED_ID,
                -MathHelper.clamp(1.0 - multiplier, 0.0, 0.99),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void cleanupActor(LivingEntity actor, ActiveChains ability) {
        EntityAttributeInstance movement = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(BASTION_SPEED_ID);
        if (ability.bastion) {
            restorePreviousEffect(actor, StatusEffects.RESISTANCE, 1,
                    ability.expiresAt - actor.getWorld().getTime(), ability.previousResistance,
                    actor.getWorld().getTime() - ability.startedAt);
        }
        if (ability.roaming) {
            restorePreviousEffect(actor, StatusEffects.SPEED, 0,
                    ability.expiresAt - actor.getWorld().getTime(), ability.previousSpeed,
                    actor.getWorld().getTime() - ability.startedAt);
        }
    }

    private static void restorePreviousEffect(LivingEntity actor,
                                              net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> type,
                                              int amplifier, long managedRemaining,
                                              StatusEffectInstance previous, long elapsed) {
        StatusEffectInstance current = actor.getStatusEffect(type);
        if (current == null) return;
        StatusEffectInstance restoredPrevious = remainingCopy(previous, elapsed);
        boolean managedCurrent = current.getAmplifier() == amplifier
                && current.getDuration() <= Math.max(0, managedRemaining) + 2;
        StatusEffectInstance externalCurrent = managedCurrent ? null : new StatusEffectInstance(
                type, current.getDuration(), current.getAmplifier(), current.isAmbient(),
                current.shouldShowParticles(), current.shouldShowIcon(),
                restoredPrevious != null && restoredPrevious.getAmplifier() != current.getAmplifier()
                        ? restoredPrevious : null);
        actor.removeStatusEffect(type);
        if (externalCurrent != null) {
            actor.addStatusEffect(externalCurrent, actor);
            return;
        }
        if (restoredPrevious != null) actor.addStatusEffect(restoredPrevious, actor);
    }

    private static StatusEffectInstance remainingCopy(StatusEffectInstance previous, long elapsed) {
        if (previous == null) return null;
        int remaining = previous.getDuration() - (int) Math.max(0, elapsed);
        if (remaining <= 0) return null;
        return new StatusEffectInstance(previous.getEffectType(), remaining, previous.getAmplifier(),
                previous.isAmbient(), previous.shouldShowParticles(), previous.shouldShowIcon());
    }

    private static <K> boolean claimLockout(Map<ServerWorld, Map<K, Long>> stores, ServerWorld world,
                                            K key, int ticks) {
        Map<K, Long> lockouts = stores.computeIfAbsent(world, ignored -> new HashMap<>());
        long now = world.getTime();
        if (lockouts.getOrDefault(key, 0L) > now) return false;
        lockouts.put(key, now + Math.max(1, ticks));
        return true;
    }

    private static boolean hasPendingLockouts(ServerWorld world) {
        Map<BrandKey, Long> shelter = BRAND_SHELTER_LOCKOUTS.get(world);
        Map<UUID, Long> reactive = REACTIVE_BRAND_LOCKOUTS.get(world);
        return shelter != null && !shelter.isEmpty() || reactive != null && !reactive.isEmpty();
    }

    private static void pruneTimedState(ServerWorld world) {
        long now = world.getTime();
        pruneLockouts(BRAND_SHELTER_LOCKOUTS, world, now);
        pruneLockouts(REACTIVE_BRAND_LOCKOUTS, world, now);
        if (ABSORPTION_SWEEP_UNTIL.getOrDefault(world, 0L) < now) ABSORPTION_SWEEP_UNTIL.remove(world);
    }

    private static <K> void pruneLockouts(Map<ServerWorld, Map<K, Long>> stores,
                                          ServerWorld world, long now) {
        Map<K, Long> lockouts = stores.get(world);
        if (lockouts == null) return;
        lockouts.values().removeIf(expiresAt -> expiresAt <= now);
        if (lockouts.isEmpty()) stores.remove(world);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, ActiveChains> abilities = ACTIVE.remove(world);
        if (abilities != null) {
            for (ActiveChains ability : abilities.values()) {
                LivingEntity actor = resolveLiving(world, ability.actorId);
                if (actor != null) cleanupActor(actor, ability);
                discardAbilityVisuals(world, ability);
                UniqueAbilityApi.cancel(ability.execution);
            }
        }
        Map<BrandKey, FurnaceBrand> brands = BRANDS.remove(world);
        if (brands != null) brands.values().forEach(brand -> discardVisual(world, brand.visualId));
        Set<UUID> visuals = new HashSet<>(MANAGED_VISUALS.getOrDefault(world, Set.of()));
        visuals.forEach(id -> discardVisual(world, id));
        MANAGED_VISUALS.remove(world);
        BRAND_SHELTER_LOCKOUTS.remove(world);
        REACTIVE_BRAND_LOCKOUTS.remove(world);
        ABSORPTION_SWEEP_UNTIL.remove(world);
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>();
        worlds.addAll(ACTIVE.keySet());
        worlds.addAll(BRANDS.keySet());
        worlds.addAll(MANAGED_VISUALS.keySet());
        worlds.forEach(HearthflameAbilityManager::clear);
        BRAND_SHELTER_LOCKOUTS.clear();
        REACTIVE_BRAND_LOCKOUTS.clear();
        ABSORPTION_SWEEP_UNTIL.clear();
    }

    private static float maximumPressure() {
        return Math.max(1.0F, Config.uniqueEffects.hearthflame.maximumPressure);
    }

    private static float maximumPressure(ActiveChains ability) {
        return (float) ability.tuning.get(FireForgeMasteryTuning.Setting.MAX_PRESSURE, maximumPressure());
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

    private static void spawnFinaleEffects(ServerWorld world, LivingEntity actor, Vec3d center,
                                           float strength, boolean struckFinale) {
        float clamped = MathHelper.clamp(strength, 0.0F, 1.0F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY,
                actor.getSoundCategory(), 1.4F, struckFinale ? 0.68F : 0.78F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_ANVIL_LAND,
                actor.getSoundCategory(), 1.2F, 0.55F);
        world.playSound(null, center.x, center.y, center.z, SoundRegistry.SPELL_FIRE.get(),
                actor.getSoundCategory(), 1.0F, 0.6F);
        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 0.3, center.z,
                45 + (int) (35 * clamped), 1.2, 1.25, 1.2, 0.13);
        world.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z,
                12 + (int) (10 * clamped), 0.85, 0.55, 0.85, 0.1);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.6, center.z,
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
        private final FireForgeMasteryTuning tuning;
        private final UniqueAbilityExecution execution;
        private final Vec3d castAnchor;
        private final List<FurnaceChain> chains = new ArrayList<>();
        private UUID coreVisualId;
        private float pressure;
        private boolean primed;
        private boolean bastion;
        private boolean roaming;
        private StatusEffectInstance previousResistance;
        private StatusEffectInstance previousSpeed;
        private int initialChainCount;
        private int completedChains;

        private ActiveChains(UUID actorId, UUID sourceOwnerId, ItemStack stack,
                             long startedAt, long expiresAt,
                             float echoDamage, float snapDamage, float finalDamage,
                             FireForgeMasteryTuning tuning, UniqueAbilityExecution execution,
                             Vec3d castAnchor) {
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
            this.castAnchor = castAnchor;
        }
    }

    private static final class FurnaceChain {
        private final UUID targetId;
        private final UUID visualId;
        private final double initialLength;
        private final boolean branded;
        private final int generation;
        private final float damageMultiplier;
        private final long snapAt;
        private float tension;
        private boolean preserved;
        private long preserveUntil;

        private FurnaceChain(UUID targetId, UUID visualId, double initialLength, float tension,
                             boolean branded, int generation, float damageMultiplier, long snapAt) {
            this.targetId = targetId;
            this.visualId = visualId;
            this.initialLength = initialLength;
            this.tension = tension;
            this.branded = branded;
            this.generation = generation;
            this.damageMultiplier = damageMultiplier;
            this.snapAt = snapAt;
        }
    }

    private record BrandKey(UUID ownerId, UUID targetId) {
    }

    private record FurnaceBrand(long expiresAt, UUID sourceOwnerId, UUID visualId) {
    }

    private record SizeResponse(double pullMultiplier, double tensionMultiplier) {
    }
}
