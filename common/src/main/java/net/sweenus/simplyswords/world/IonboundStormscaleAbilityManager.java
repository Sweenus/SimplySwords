package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.IonboundStormscaleVisualEntity;
import net.sweenus.simplyswords.item.component.IonCubeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class IonboundStormscaleAbilityManager {
    private static final String VISUAL_TAG = "simplyswords_ionbound_stormscale_visual";
    private static final Identifier BEAM_MOVEMENT_SLOW_ID = Identifier.of("simplyswords", "ion_beam_channel_slow");
    private static final Map<ServerWorld, Map<UUID, ActiveCorridor>> CORRIDORS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ActiveShield>> SHIELDS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ActiveBeam>> BEAMS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, UUID>> ORBIT_VISUALS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, WielderRecharge>> RECHARGE = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ReserveState>> RESERVES = new HashMap<>();
    private static final int MODE_ION_REBOUND = 1;
    private static final int MODE_STORED_CHARGE = 2;
    private static final int MODE_EMERGENCY_CELL = 4;
    private static final int MODE_RECYCLED_CURRENT = 8;
    private static final int MODE_AEGIS_REACTOR = 16;
    private static final int MODE_OVERCHARGED_CORE = 32;
    private static final int MODE_CRUSHING_FOCUS = 64;
    private static final int MODE_ION_COFFIN = 128;
    private static final int MODE_REPULSOR_GATE = 256;
    private static final int MODE_DISINTEGRATION = 1024;
    private static final int MODE_PARALYTIC_WALLS = 2048;

    private IonboundStormscaleAbilityManager() {
    }

    public static IonCubeComponent getCubes(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? IonCubeComponent.DEFAULT
                : stack.getOrDefault(ComponentTypeRegistry.ION_CUBES.get(), IonCubeComponent.DEFAULT);
    }

    public static void tickStack(ItemStack stack, World world, Entity entity) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)
                || !(entity instanceof LivingEntity actor) || !stack.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get())) {
            return;
        }
        Hand hand = heldHand(actor, stack);
        if (hand == null || !actor.isAlive() || !AwakeningApi.isAbilityUnlocked(stack)) {
            flushRechargeForStack(serverWorld, actor.getUuid(), stack);
            if (!actor.isAlive() || hand != null && !AwakeningApi.isAbilityUnlocked(stack)
                    || !isHoldingIonbound(actor)) {
                discardOrbit(serverWorld, actor.getUuid());
            }
            return;
        }

        RechargeState state = getOrCreateRechargeState(serverWorld, actor, hand, stack);
        tickEmergencyCell(serverWorld, actor, stack, state.tuning);
        IonCubeComponent cubes = getCubes(stack);
        if (cubes.cubes() >= IonCubeComponent.MAX_CUBES) {
            state.rechargeTicks = 0;
            grantStoredCharge(serverWorld, actor, state.tuning);
        } else if (state.lastAdvancedTick != serverWorld.getTime()) {
            state.lastAdvancedTick = serverWorld.getTime();
            state.rechargeTicks++;
            ReserveState reserve = reserve(serverWorld, actor.getUuid());
            int interval = Math.max(1, state.tuning.integer(
                    StormSoulMasteryTuning.Setting.RESERVE_INTERVAL_TICKS,
                    Config.uniqueEffects.ionbound_stormscale.rechargeInterval));
            if (reserve != null && reserve.rechargeRefund > 0) {
                interval = Math.max(1, interval - reserve.rechargeRefund);
            }
            if (state.rechargeTicks >= interval) {
                cubes = new IonCubeComponent(cubes.cubes() + 1, 0);
                state.rechargeTicks = 0;
                if (reserve != null) reserve.rechargeRefund = 0;
                stack.set(ComponentTypeRegistry.ION_CUBES.get(), cubes);
                spawnCubeGeneratedEffects(serverWorld, actor, cubes.cubes());
            }
        }
        ensureOrbit(serverWorld, actor, visibleCubeCount(actor));
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null || context.world() == null || context.actor() == null || !context.actor().isAlive()
                || context.stack() == null || !context.stack().isOf(ItemsRegistry.IONBOUND_STORMSCALE.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1) {
            return false;
        }
        ActiveCorridor corridor = getCorridor(context.world(), context.actor().getUuid());
        if (corridor != null) {
            return corridor.slammed && context.world().getTime() <= corridor.followupEnds
                    && (corridor.tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0) & MODE_ION_COFFIN) == 0
                    && getCubes(context.stack()).cubes() > 0;
        }
        return getCubes(context.stack()).cubes() > 0;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) return false;
        ActiveCorridor corridor = getCorridor(context.world(), context.actor().getUuid());
        if (corridor != null) {
            boolean fired = startBeam(context.world(), context.actor(), context.stack(), corridor);
            if (fired) {
                Map<UUID, ActiveCorridor> active = CORRIDORS.get(context.world());
                if (active != null) active.remove(context.actor().getUuid());
            }
            return fired;
        }
        return startCorridor(context);
    }

    public static boolean hasPendingFollowup(ServerWorld world, LivingEntity actor) {
        if (world == null || actor == null) return false;
        ActiveCorridor corridor = getCorridor(world, actor.getUuid());
        return corridor != null && (!corridor.slammed || world.getTime() <= corridor.followupEnds);
    }

    public static boolean handleIncomingDamage(LivingEntity actor, DamageSource source, float amount) {
        if (actor == null || source == null || amount <= 0.0F || !(actor.getWorld() instanceof ServerWorld world)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Map<UUID, ActiveShield> shields = SHIELDS.get(world);
        ActiveShield active = shields == null ? null : shields.get(actor.getUuid());
        if (active != null && world.getTime() < active.expiresAt) {
            spawnShieldImpactEffects(world, actor);
            return true;
        }

        ItemStack stack = findHeldIonbound(actor);
        if (stack.isEmpty() || !AwakeningApi.isAbilityUnlocked(stack)) {
            return false;
        }

        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.IONBOUND_SHIELD,
                UniqueAbilityContext.passive(world, stack, actor, source.getAttacker() instanceof LivingEntity target
                        ? target : null, heldHand(actor, stack)), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        int mode = tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0);
        int cost = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.SHIELD_CUBE_COST, 1));
        if ((mode & MODE_OVERCHARGED_CORE) != 0
                || amount <= actor.getMaxHealth() * tuning.get(StormSoulMasteryTuning.Setting.SHIELD_THRESHOLD,
                        Config.uniqueEffects.ionbound_stormscale.shieldTriggerHealthFraction)
                || !consumeCubes(stack, cost)) {
            UniqueAbilityApi.cancel(execution);
            return false;
        }

        int duration = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.SHIELD_DURATION_TICKS,
                Config.uniqueEffects.ionbound_stormscale.shieldDuration));
        IonboundStormscaleVisualEntity visual = IonboundStormscaleVisualEntity.shield(world, actor, duration);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        SHIELDS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(),
                new ActiveShield(actor.getUuid(), world.getTime() + duration, visual.getUuid()));
        updateOrbit(world, actor);
        spawnShieldStartEffects(world, actor);
        double push = tuning.get(StormSoulMasteryTuning.Setting.SHIELD_PUSH_STRENGTH, 0);
        if ((mode & MODE_ION_REBOUND) != 0 && push > 0
                && source.getAttacker() instanceof LivingEntity attacker && attacker != actor) {
            Vec3d offset = attacker.getPos().subtract(actor.getPos());
            Vec3d horizontal = new Vec3d(offset.x, 0.0, offset.z);
            if (horizontal.lengthSquared() > 1.0E-6) {
                double resistance = MathHelper.clamp(attacker.getAttributeValue(
                        EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
                Vec3d shove = horizontal.normalize().multiply(push * (1.0 - resistance) * 0.25);
                attacker.addVelocity(shove.x, 0.18, shove.z);
                attacker.velocityModified = true;
            }
        }
        UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                source.getAttacker() instanceof LivingEntity target ? target : null, 1, amount);
        UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, 1);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        return !CORRIDORS.getOrDefault(world, Map.of()).isEmpty()
                || !SHIELDS.getOrDefault(world, Map.of()).isEmpty()
                || !BEAMS.getOrDefault(world, Map.of()).isEmpty()
                || !ORBIT_VISUALS.getOrDefault(world, Map.of()).isEmpty()
                || !RECHARGE.getOrDefault(world, Map.of()).isEmpty()
                || world.getTime() % 40L == 0L;
    }

    public static void tick(ServerWorld world) {
        tickCorridors(world);
        tickBeams(world);
        tickShields(world);
        tickOrbits(world);
        tickRecharge(world);
        if (world.getTime() % 40L == 0L) purgeOrphanVisuals(world);
    }

    public static void flushWielder(LivingEntity actor) {
        if (actor != null && actor.getWorld() instanceof ServerWorld world) {
            flushWielder(world, actor.getUuid());
        }
    }

    private static boolean startCorridor(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        ItemStack stack = context.stack();
        if (!consumeCube(stack)) return false;
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.IONBOUND_CRUSHER,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                        .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, 1));
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);

        Vec3d forward = horizontalDirection(context.facing(), actor);
        Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);
        Vec3d origin = actor.getPos().add(forward.multiply(2.0));
        int materialize = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_MATERIALIZE_TICKS,
                Config.uniqueEffects.ionbound_stormscale.corridorMaterializeTicks));
        int hold = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_HOLD_TICKS,
                Config.uniqueEffects.ionbound_stormscale.corridorHoldTicks));
        int close = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_CLOSE_TICKS,
                Config.uniqueEffects.ionbound_stormscale.corridorCloseTicks));
        ActiveCorridor corridor = new ActiveCorridor(actor.getUuid(), stack, stack.copy(), origin, forward, right,
                context.world().getTime(), context.world().getTime() + materialize + hold + close,
                tuning, execution);
        CORRIDORS.computeIfAbsent(context.world(), ignored -> new HashMap<>()).put(actor.getUuid(), corridor);
        orCreateReserve(context.world(), actor.getUuid()).beamWidthMultiplier =
                tuning.get(StormSoulMasteryTuning.Setting.BEAM_WIDTH_MULTIPLIER, 0);

        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        IonboundStormscaleVisualEntity visual = IonboundStormscaleVisualEntity.corridor(context.world(), origin, yaw,
                (float) tuning.get(StormSoulMasteryTuning.Setting.RANGE,
                        Config.uniqueEffects.ionbound_stormscale.corridorLength),
                (float) tuning.get(StormSoulMasteryTuning.Setting.WIDTH,
                        Config.uniqueEffects.ionbound_stormscale.corridorWidth),
                (float) tuning.get(StormSoulMasteryTuning.Setting.HEIGHT,
                        Config.uniqueEffects.ionbound_stormscale.corridorHeight),
                materialize, hold, close);
        visual.addCommandTag(VISUAL_TAG);
        context.world().spawnEntity(visual);
        corridor.visualId = visual.getUuid();
        updateOrbit(context.world(), actor);
        spawnCorridorStartEffects(context.world(), actor, origin);
        return true;
    }

    private static void tickCorridors(ServerWorld world) {
        Map<UUID, ActiveCorridor> active = CORRIDORS.get(world);
        if (active == null) return;
        long now = world.getTime();
        Iterator<ActiveCorridor> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveCorridor corridor = iterator.next();
            LivingEntity actor = resolveLiving(world, corridor.actorId);
            if (actor == null) {
                discardVisual(world, corridor.visualId);
                UniqueAbilityApi.cancel(corridor.execution);
                iterator.remove();
                continue;
            }
            if (!corridor.slammed) {
                int materialize = Math.max(1, corridor.tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_MATERIALIZE_TICKS,
                        Config.uniqueEffects.ionbound_stormscale.corridorMaterializeTicks));
                int hold = Math.max(1, corridor.tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_HOLD_TICKS,
                        Config.uniqueEffects.ionbound_stormscale.corridorHoldTicks));
                long closingAt = corridor.startedAt + materialize + hold;
                if (now >= closingAt) pullTargetsToCentre(world, actor, corridor, now - closingAt);
                if (now >= corridor.slamAt) slam(world, actor, corridor);
            } else if (!(actor instanceof PlayerEntity) && now >= corridor.slamAt + 4
                    && getCubes(corridor.stackReference).cubes() > 0) {
                startBeam(world, actor, corridor.stackReference, corridor);
                iterator.remove();
                continue;
            }
            if (corridor.slammed && now > corridor.followupEnds) {
                applyCooldown(world, actor, corridor.stackReference);
                UniqueAbilityApi.finish(corridor.execution, StormSoulMasteryAbilities.FINISH, 0);
                iterator.remove();
            }
        }
        if (active.isEmpty()) CORRIDORS.remove(world);
    }

    private static void slam(ServerWorld world, LivingEntity actor, ActiveCorridor corridor) {
        corridor.slammed = true;
        corridor.followupEnds = world.getTime() + Math.max(1, corridor.tuning.integer(
                StormSoulMasteryTuning.Setting.LOCKOUT_TICKS, Config.uniqueEffects.ionbound_stormscale.followupWindow));
        int mode = corridor.tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0);
        ReserveState reserve = reserve(world, actor.getUuid());
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.LIGHTNING, actor, corridor.stackSnapshot,
                Config.uniqueEffects.ionbound_stormscale.slamDamageScaling,
                Config.uniqueEffects.ionbound_stormscale.slamSpellScaling)
                * (float) corridor.tuning.get(StormSoulMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                * abilityDamageFactor(world, actor, corridor.stackReference, corridor.tuning)
                * storedChargeFactor(world, actor, corridor.tuning);
        if (reserve != null) reserve.storedCharges = 0;
        double laneWidth = corridor.tuning.get(StormSoulMasteryTuning.Setting.FOCUS_LANE_WIDTH, 0);
        double focusBonus = corridor.tuning.get(StormSoulMasteryTuning.Setting.FOCUS_DAMAGE_BONUS, 0);
        double burst = corridor.tuning.get(StormSoulMasteryTuning.Setting.BURST_STRENGTH, 0);
        double trapSpeed = corridor.tuning.get(StormSoulMasteryTuning.Setting.TRAP_MOVEMENT_SPEED, 0);
        int affected = 0;
        for (LivingEntity target : targetsInCorridor(world, actor, corridor,
                Math.max(0.55, corridor.tuning.get(StormSoulMasteryTuning.Setting.WIDTH,
                        Config.uniqueEffects.ionbound_stormscale.corridorWidth) * 0.5))) {
            float targetDamage = damage;
            if ((mode & MODE_CRUSHING_FOCUS) != 0 && laneWidth > 0 && focusBonus > 0
                    && Math.abs(target.getPos().subtract(corridor.origin).dotProduct(corridor.right))
                            <= laneWidth * 0.5) {
                targetDamage *= (float) (1 + focusBonus);
            }
            if (damageTarget(world, actor, corridor.stackSnapshot, target, targetDamage, false)) {
                int statusTicks = Math.max(1, corridor.tuning.integer(
                        StormSoulMasteryTuning.Setting.STATUS_DURATION_TICKS,
                        Config.uniqueEffects.ionbound_stormscale.slowDuration));
                if ((mode & MODE_REPULSOR_GATE) == 0) {
                    target.addStatusEffect(new StatusEffectInstance(
                            (mode & MODE_PARALYTIC_WALLS) != 0
                                    ? EffectRegistry.getReference(EffectRegistry.ION_PARALYSIS)
                                    : StatusEffects.SLOWNESS,
                            statusTicks, Math.max(0, Config.uniqueEffects.ionbound_stormscale.slowAmplifier),
                            false, true, true), actor);
                }
                if ((mode & MODE_ION_COFFIN) != 0 && trapSpeed > 0) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                            statusTicks, trapAmplifier(trapSpeed), false, true, true), actor);
                }
                if ((mode & MODE_REPULSOR_GATE) != 0 && burst > 0) {
                    pushOutward(target, corridor, burst);
                }
                spawnSlamTargetEffects(world, target);
                affected++;
                UniqueAbilityApi.emit(corridor.execution, UniqueAbilityPhase.HIT,
                        StormSoulMasteryAbilities.HIT, target, 1, targetDamage);
                if (affected >= corridor.tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_TARGET_CAP,
                        Config.uniqueEffects.ionbound_stormscale.corridorTargetCap)) break;
            }
        }
        spawnSlamEffects(world, corridor);
    }

    private static boolean startBeam(ServerWorld world, LivingEntity actor, ItemStack heldStack, ActiveCorridor corridor) {
        if (!corridor.slammed || world.getTime() > corridor.followupEnds
                || (corridor.tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0) & MODE_ION_COFFIN) != 0
                || !consumeCube(heldStack)) return false;
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.IONBOUND_BEAM,
                UniqueAbilityContext.passive(world, heldStack, actor, null, heldHand(actor, heldStack)), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                        .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.ionbound_stormscale.cooldown));
        StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
        int duration = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.BEAM_DURATION_TICKS,
                Config.uniqueEffects.ionbound_stormscale.beamDuration));
        IonboundStormscaleVisualEntity visual = IonboundStormscaleVisualEntity.beam(world, actor,
                (float) tuning.get(StormSoulMasteryTuning.Setting.RANGE,
                        Config.uniqueEffects.ionbound_stormscale.corridorLength),
                (float) beamWidth(world, actor, tuning),
                duration);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        ActiveBeam beam = new ActiveBeam(actor.getUuid(), heldStack, corridor.stackSnapshot,
                world.getTime(), world.getTime() + duration, visual.getUuid(), tuning, execution);
        BEAMS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), beam);
        applyBeamMovementSlow(actor, tuning);
        discardVisual(world, corridor.visualId);
        updateOrbit(world, actor);
        spawnBeamStartEffects(world, actor);
        pulseBeam(world, actor, beam);
        UniqueAbilityApi.finish(corridor.execution, StormSoulMasteryAbilities.FINISH, 0);
        return true;
    }

    private static void tickBeams(ServerWorld world) {
        Map<UUID, ActiveBeam> active = BEAMS.get(world);
        if (active == null) return;
        long now = world.getTime();
        Iterator<ActiveBeam> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveBeam beam = iterator.next();
            LivingEntity actor = resolveLiving(world, beam.actorId);
            boolean valid = actor != null
                    && !IncapacitatingStatusEffectRegistry.isIncapacitated(actor)
                    && heldHand(actor, beam.stackReference) != null;
            if (!valid || now >= beam.endsAt) {
                discardVisual(world, beam.visualId);
                LivingEntity channeler = actor != null ? actor : resolveLivingForCleanup(world, beam.actorId);
                if (channeler != null) {
                    removeBeamMovementSlow(channeler);
                    if (actor != null) spawnBeamEndEffects(world, actor);
                }
                if (actor == null) UniqueAbilityApi.cancel(beam.execution);
                else {
                    UniqueAbilityApi.finish(beam.execution, StormSoulMasteryAbilities.FINISH, 0);
                    grantRechargeRefund(world, beam.actorId, beam.tuning);
                }
                iterator.remove();
                continue;
            }
            applyBeamMovementSlow(actor, beam.tuning);
            Entity visual = world.getEntity(beam.visualId);
            if (visual != null) {
                visual.setPosition(actor.getPos());
                visual.setYaw(actor.getYaw());
                visual.setPitch(actor.getPitch());
            }
            int interval = Math.max(1, beam.tuning.integer(StormSoulMasteryTuning.Setting.INTERVAL_TICKS,
                    Config.uniqueEffects.ionbound_stormscale.beamDamageInterval));
            long age = now - beam.startedAt;
            if (age > 0 && age % interval == 0) pulseBeam(world, actor, beam);
            if (age > 0 && age % 20 == 0) spawnBeamSustainEffects(world, actor, age);
        }
        if (active.isEmpty()) BEAMS.remove(world);
    }

    private static void pulseBeam(ServerWorld world, LivingEntity actor, ActiveBeam beam) {
        int duration = Math.max(1, beam.tuning.integer(StormSoulMasteryTuning.Setting.BEAM_DURATION_TICKS,
                Config.uniqueEffects.ionbound_stormscale.beamDuration));
        int interval = Math.max(1, beam.tuning.integer(StormSoulMasteryTuning.Setting.INTERVAL_TICKS,
                Config.uniqueEffects.ionbound_stormscale.beamDamageInterval));
        int pulses = Math.max(1, (duration + interval - 1) / interval);
        float baseDamage = HelperMethods.abilityScaledDamage(SpellScalingProfile.LIGHTNING, actor, beam.stackSnapshot,
                Config.uniqueEffects.ionbound_stormscale.beamDamageScaling,
                Config.uniqueEffects.ionbound_stormscale.beamSpellScaling);
        float damage = baseDamage * Math.max(0.0F, Config.uniqueEffects.ionbound_stormscale.beamTotalDamageMultiplier)
                * (float) beam.tuning.get(StormSoulMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                * abilityDamageFactor(world, actor, beam.stackReference, beam.tuning)
                / pulses;
        double armorIgnore = beam.tuning.get(StormSoulMasteryTuning.Setting.ARMOR_IGNORE, 0);
        Vec3d start = beamOrigin(actor);
        Vec3d direction = actor.getRotationVec(1.0F).normalize();
        Vec3d end = start.add(direction.multiply(Math.max(1.0, beam.tuning.get(StormSoulMasteryTuning.Setting.RANGE,
                Config.uniqueEffects.ionbound_stormscale.corridorLength))));
        double halfWidth = Math.max(0.1, beamWidth(world, actor, beam.tuning) * 0.5);
        int affected = 0;
        for (LivingEntity target : targetsAlongBeam(world, actor, start, end, halfWidth)) {
            if (damageTarget(world, actor, beam.stackSnapshot, target,
                    armorIgnore > 0 ? damage * (float) (1 + Math.min(1, armorIgnore / 20.0)) : damage, true)) {
                target.stopUsingItem();
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.ION_PARALYSIS),
                        Math.max(1, beam.tuning.integer(StormSoulMasteryTuning.Setting.STATUS_DURATION_TICKS,
                                Config.uniqueEffects.ionbound_stormscale.paralysisDuration)),
                        0, false, false, false), actor);
                spawnBeamTargetEffects(world, target);
                affected++;
                UniqueAbilityApi.emit(beam.execution, UniqueAbilityPhase.HIT,
                        StormSoulMasteryAbilities.PULSE, target, 1, damage);
                if (affected >= beam.tuning.integer(StormSoulMasteryTuning.Setting.BEAM_TARGET_CAP,
                        Config.uniqueEffects.ionbound_stormscale.beamTargetCap)) break;
            }
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z,
                12, 0.24, 0.24, 0.24, 0.08);
    }

    private static List<LivingEntity> targetsAlongBeam(ServerWorld world, LivingEntity actor,
                                                        Vec3d start, Vec3d end, double halfWidth) {
        Box search = new Box(start, end).expand(halfWidth + 1.0);
        return world.getEntitiesByClass(LivingEntity.class, search, target -> {
            if (target == actor || !target.isAlive() || !EntityPredicates.VALID_LIVING_ENTITY.test(target)
                    || !HelperMethods.checkAbilityTarget(target, actor)) return false;
            Box hitbox = target.getBoundingBox().expand(halfWidth);
            return hitbox.contains(start) || hitbox.raycast(start, end).isPresent();
        });
    }

    private static Vec3d beamOrigin(LivingEntity actor) {
        return actor.getPos().add(0.0, actor.getHeight() * 0.68, 0.0);
    }

    private static void pullTargetsToCentre(ServerWorld world, LivingEntity actor, ActiveCorridor corridor, long closingAge) {
        int closeTicks = Math.max(1, corridor.tuning.integer(StormSoulMasteryTuning.Setting.CORRIDOR_CLOSE_TICKS,
                Config.uniqueEffects.ionbound_stormscale.corridorCloseTicks));
        double progress = MathHelper.clamp((closingAge + 1.0) / closeTicks, 0.0, 1.0);
        double halfWidth = corridor.tuning.get(StormSoulMasteryTuning.Setting.WIDTH,
                Config.uniqueEffects.ionbound_stormscale.corridorWidth) * 0.5 * (1.0 - progress * 0.82);
        for (LivingEntity target : targetsInCorridor(world, actor, corridor, halfWidth + 0.8)) {
            double side = target.getPos().subtract(corridor.origin).dotProduct(corridor.right);
            double resistance = MathHelper.clamp(target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double strength = Math.max(0.0, corridor.tuning.get(StormSoulMasteryTuning.Setting.PULL_STRENGTH,
                    Config.uniqueEffects.ionbound_stormscale.corridorPullStrength)) * (1.0 - resistance);
            target.addVelocity(corridor.right.x * -Math.signum(side) * strength, 0.025,
                    corridor.right.z * -Math.signum(side) * strength);
            target.velocityModified = true;
        }
    }

    private static List<LivingEntity> targetsInCorridor(ServerWorld world, LivingEntity actor,
                                                         ActiveCorridor corridor, double halfWidth) {
        double length = Math.max(1.0, corridor.tuning.get(StormSoulMasteryTuning.Setting.RANGE,
                Config.uniqueEffects.ionbound_stormscale.corridorLength));
        double height = Math.max(1.0, corridor.tuning.get(StormSoulMasteryTuning.Setting.HEIGHT,
                Config.uniqueEffects.ionbound_stormscale.corridorHeight));
        Vec3d end = corridor.origin.add(corridor.forward.multiply(length));
        Box search = new Box(Math.min(corridor.origin.x, end.x), corridor.origin.y - 0.5,
                Math.min(corridor.origin.z, end.z), Math.max(corridor.origin.x, end.x),
                corridor.origin.y + height, Math.max(corridor.origin.z, end.z)).expand(halfWidth + 1.5, 0.5, halfWidth + 1.5);
        return world.getEntitiesByClass(LivingEntity.class, search, target -> target != actor
                && target.isAlive() && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor)
                && insideOrientedBox(target, corridor, length, halfWidth, height));
    }

    private static boolean insideOrientedBox(LivingEntity target, ActiveCorridor corridor,
                                              double length, double halfWidth, double height) {
        Vec3d relative = target.getPos().subtract(corridor.origin);
        double along = relative.dotProduct(corridor.forward);
        double side = Math.abs(relative.dotProduct(corridor.right));
        double radius = Math.max(0.1, target.getWidth() * 0.5);
        return along >= -radius && along <= length + radius && side <= halfWidth + radius
                && target.getBoundingBox().maxY >= corridor.origin.y - 0.5
                && target.getBoundingBox().minY <= corridor.origin.y + height;
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor, ItemStack stack,
                                        LivingEntity target, float baseDamage, boolean throughIframes) {
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, baseDamage);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(() -> {
            if (throughIframes) damaged[0] = HelperMethods.damageThroughIframes(target, source, damage);
            else {
                float health = target.getHealth();
                HelperMethods.applyDamageWithoutKnockback(target, source, damage);
                damaged[0] = target.getHealth() < health || !target.isAlive();
            }
        });
        return damaged[0];
    }

    private static void tickShields(ServerWorld world) {
        Map<UUID, ActiveShield> active = SHIELDS.get(world);
        if (active == null) return;
        long now = world.getTime();
        active.values().removeIf(shield -> {
            LivingEntity actor = resolveLiving(world, shield.actorId);
            boolean remove = actor == null || now >= shield.expiresAt;
            if (remove) {
                discardVisual(world, shield.visualId);
                if (actor != null) spawnShieldEndEffects(world, actor);
            }
            return remove;
        });
        if (active.isEmpty()) SHIELDS.remove(world);
    }

    private static void tickOrbits(ServerWorld world) {
        Map<UUID, UUID> visuals = ORBIT_VISUALS.get(world);
        if (visuals == null) return;
        Iterator<Map.Entry<UUID, UUID>> iterator = visuals.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            LivingEntity owner = resolveLiving(world, entry.getKey());
            Entity visual = world.getEntity(entry.getValue());
            if (owner == null || !isHoldingIonbound(owner) || visibleCubeCount(owner) <= 0
                    || !(visual instanceof IonboundStormscaleVisualEntity orbit)
                    || orbit.getKind() != IonboundStormscaleVisualEntity.ORBIT
                    || !orbit.isOwnedBy(owner) || orbit.needsOrbitRenewal()) {
                if (visual != null) visual.discard();
                iterator.remove();
                continue;
            }
            orbit.refreshOrbitLease(owner, visibleCubeCount(owner));
        }
        if (visuals.isEmpty()) ORBIT_VISUALS.remove(world);
    }

    private static void ensureOrbit(ServerWorld world, LivingEntity actor, int cubes) {
        Map<UUID, UUID> visuals = ORBIT_VISUALS.computeIfAbsent(world, ignored -> new HashMap<>());
        Entity existing = visuals.containsKey(actor.getUuid()) ? world.getEntity(visuals.get(actor.getUuid())) : null;
        if (cubes <= 0) {
            if (existing != null) existing.discard();
            visuals.remove(actor.getUuid());
            return;
        }
        if (existing instanceof IonboundStormscaleVisualEntity visual
                && visual.getKind() == IonboundStormscaleVisualEntity.ORBIT
                && visual.isOwnedBy(actor) && !visual.needsOrbitRenewal()) {
            visual.refreshOrbitLease(actor, cubes);
            return;
        }
        if (existing != null) existing.discard();
        IonboundStormscaleVisualEntity visual = IonboundStormscaleVisualEntity.orbit(world, actor, cubes);
        visual.addCommandTag(VISUAL_TAG);
        world.spawnEntity(visual);
        visuals.put(actor.getUuid(), visual.getUuid());
    }

    private static void updateOrbit(ServerWorld world, LivingEntity actor) {
        ensureOrbit(world, actor, visibleCubeCount(actor));
    }

    private static void discardOrbit(ServerWorld world, UUID actorId) {
        Map<UUID, UUID> visuals = ORBIT_VISUALS.get(world);
        if (visuals == null) return;
        UUID id = visuals.remove(actorId);
        discardVisual(world, id);
        if (visuals.isEmpty()) ORBIT_VISUALS.remove(world);
    }

    private static boolean consumeCube(ItemStack stack) {
        return consumeCubes(stack, 1);
    }

    private static boolean consumeCubes(ItemStack stack, int count) {
        IonCubeComponent cubes = getCubes(stack);
        if (cubes.cubes() < Math.max(1, count)) return false;
        IonCubeComponent remaining = cubes;
        for (int index = 0; index < Math.max(1, count); index++) {
            remaining = remaining.consume();
        }
        stack.set(ComponentTypeRegistry.ION_CUBES.get(), remaining);
        return true;
    }

    private static ReserveState reserve(ServerWorld world, UUID actorId) {
        Map<UUID, ReserveState> states = RESERVES.get(world);
        return states == null ? null : states.get(actorId);
    }

    private static ReserveState orCreateReserve(ServerWorld world, UUID actorId) {
        return RESERVES.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(actorId, ignored -> new ReserveState());
    }

    private static void grantStoredCharge(ServerWorld world, LivingEntity actor, StormSoulMasteryTuning tuning) {
        int mode = tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0);
        int cap = tuning.integer(StormSoulMasteryTuning.Setting.RESERVE_STACK_CAP, 0);
        int duration = tuning.integer(StormSoulMasteryTuning.Setting.RESERVE_STACK_DURATION_TICKS, 0);
        if ((mode & MODE_STORED_CHARGE) == 0 || cap <= 0 || duration <= 0) return;
        ReserveState state = orCreateReserve(world, actor.getUuid());
        long now = world.getTime();
        if (now < state.storedChargeNext) return;
        if (now >= state.storedChargeExpires) state.storedCharges = 0;
        state.storedCharges = Math.min(cap, state.storedCharges + 1);
        state.storedChargeExpires = now + duration;
        state.storedChargeNext = now + duration;
    }

    private static float storedChargeFactor(ServerWorld world, LivingEntity actor, StormSoulMasteryTuning tuning) {
        ReserveState state = reserve(world, actor.getUuid());
        double bonus = tuning.get(StormSoulMasteryTuning.Setting.RESERVE_STACK_BONUS, 0);
        if (state == null || bonus <= 0 || state.storedCharges <= 0
                || world.getTime() >= state.storedChargeExpires) {
            return 1.0F;
        }
        return (float) (1 + state.storedCharges * bonus);
    }

    private static float abilityDamageFactor(ServerWorld world, LivingEntity actor,
                                             ItemStack stack, StormSoulMasteryTuning tuning) {
        double factor = 1.0;
        double penalty = tuning.get(StormSoulMasteryTuning.Setting.ABILITY_DAMAGE_PENALTY, 0);
        if (penalty > 0) factor *= 1 - Math.min(1, penalty);
        double perCube = tuning.get(StormSoulMasteryTuning.Setting.ABILITY_DAMAGE_PER_CUBE, 0);
        double cubeCap = tuning.get(StormSoulMasteryTuning.Setting.ABILITY_DAMAGE_CUBE_CAP, 0);
        if (perCube > 0 && cubeCap > 0) {
            factor *= 1 + Math.min(cubeCap, getCubes(stack).cubes() * perCube);
        }
        return (float) Math.max(0, factor);
    }

    private static double beamWidth(ServerWorld world, LivingEntity actor, StormSoulMasteryTuning tuning) {
        double width = Math.max(0.1, tuning.get(StormSoulMasteryTuning.Setting.WIDTH,
                Config.uniqueEffects.ionbound_stormscale.beamWidth));
        ReserveState state = reserve(world, actor.getUuid());
        if (state != null && state.beamWidthMultiplier > 0) width *= state.beamWidthMultiplier;
        return Math.max(0.1, width);
    }

    private static void grantRechargeRefund(ServerWorld world, UUID actorId, StormSoulMasteryTuning tuning) {
        int mode = tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0);
        int refund = tuning.integer(StormSoulMasteryTuning.Setting.RESERVE_REFUND_TICKS, 0);
        if ((mode & MODE_RECYCLED_CURRENT) == 0 || refund <= 0) return;
        orCreateReserve(world, actorId).rechargeRefund = refund;
    }

    private static void tickEmergencyCell(ServerWorld world, LivingEntity actor,
                                          ItemStack stack, StormSoulMasteryTuning tuning) {
        int mode = tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0);
        double threshold = tuning.get(StormSoulMasteryTuning.Setting.SHIELD_LOW_HEALTH_THRESHOLD, 0);
        int lockout = tuning.integer(StormSoulMasteryTuning.Setting.SHIELD_LOCKOUT_TICKS, 0);
        if ((mode & MODE_EMERGENCY_CELL) == 0 || threshold <= 0 || lockout <= 0
                || getCubes(stack).cubes() > 0
                || actor.getHealth() > actor.getMaxHealth() * threshold) {
            return;
        }
        ReserveState state = orCreateReserve(world, actor.getUuid());
        long now = world.getTime();
        if (now < state.emergencyReady) return;
        state.emergencyReady = now + lockout;
        stack.set(ComponentTypeRegistry.ION_CUBES.get(), new IonCubeComponent(1, 0));
        spawnCubeGeneratedEffects(world, actor, 1);
    }

    private static int trapAmplifier(double movementSpeed) {
        double reduction = MathHelper.clamp(1 - movementSpeed, 0.0, 1.0);
        return Math.clamp((int) Math.round(reduction / 0.15) - 1, 0, 9);
    }

    private static void pushOutward(LivingEntity target, ActiveCorridor corridor, double strength) {
        double side = target.getPos().subtract(corridor.origin).dotProduct(corridor.right);
        double resistance = MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double applied = strength * (1.0 - resistance) * 0.25;
        double direction = side == 0 ? 1 : Math.signum(side);
        target.addVelocity(corridor.right.x * direction * applied, 0.22,
                corridor.right.z * direction * applied);
        target.velocityModified = true;
    }

    public static void clear(ServerWorld world) {
        Map<UUID, ActiveCorridor> corridors = CORRIDORS.remove(world);
        if (corridors != null) {
            for (ActiveCorridor corridor : corridors.values()) {
                discardVisual(world, corridor.visualId);
                UniqueAbilityApi.cancel(corridor.execution);
            }
        }
        Map<UUID, ActiveBeam> beams = BEAMS.remove(world);
        if (beams != null) {
            for (ActiveBeam beam : beams.values()) {
                discardVisual(world, beam.visualId);
                LivingEntity actor = resolveLivingForCleanup(world, beam.actorId);
                if (actor != null) removeBeamMovementSlow(actor);
                UniqueAbilityApi.cancel(beam.execution);
            }
        }
        Map<UUID, ActiveShield> shields = SHIELDS.remove(world);
        if (shields != null) {
            for (ActiveShield shield : shields.values()) discardVisual(world, shield.visualId());
        }
        Map<UUID, UUID> orbits = ORBIT_VISUALS.remove(world);
        if (orbits != null) {
            for (UUID visualId : orbits.values()) discardVisual(world, visualId);
        }
        RECHARGE.remove(world);
        RESERVES.remove(world);
    }

    public static void clearAll() {
        for (Map<UUID, ActiveCorridor> corridors : CORRIDORS.values()) {
            for (ActiveCorridor corridor : corridors.values()) UniqueAbilityApi.cancel(corridor.execution);
        }
        for (Map<UUID, ActiveBeam> beams : BEAMS.values()) {
            for (ActiveBeam beam : beams.values()) UniqueAbilityApi.cancel(beam.execution);
        }
        CORRIDORS.clear();
        BEAMS.clear();
        SHIELDS.clear();
        ORBIT_VISUALS.clear();
        RECHARGE.clear();
        RESERVES.clear();
    }

    private static RechargeState getOrCreateRechargeState(ServerWorld world, LivingEntity actor,
                                                           Hand hand, ItemStack stack) {
        WielderRecharge wielder = RECHARGE.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(actor.getUuid(), ignored -> new WielderRecharge());
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        RechargeState transferred = wielder.hands.get(otherHand);
        if (transferred != null && transferred.stackReference == stack) {
            wielder.hands.remove(otherHand);
            wielder.hands.put(hand, transferred);
            return transferred;
        }
        RechargeState current = wielder.hands.get(hand);
        if (current == null || current.stackReference != stack) {
            checkpointRecharge(current);
            IonCubeComponent cubes = getCubes(stack);
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.IONBOUND_SHIELD,
                    UniqueAbilityContext.passive(world, stack, actor, null, hand), builder -> builder
                            .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
            UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, 0);
            current = new RechargeState(stack, cubes.rechargeTicks(), tuning);
            wielder.hands.put(hand, current);
        }
        return current;
    }

    private static void tickRecharge(ServerWorld world) {
        Map<UUID, WielderRecharge> active = RECHARGE.get(world);
        if (active == null) return;
        Iterator<Map.Entry<UUID, WielderRecharge>> wielders = active.entrySet().iterator();
        while (wielders.hasNext()) {
            Map.Entry<UUID, WielderRecharge> entry = wielders.next();
            LivingEntity actor = resolveLiving(world, entry.getKey());
            WielderRecharge wielder = entry.getValue();
            if (actor == null) {
                wielder.hands.values().forEach(IonboundStormscaleAbilityManager::checkpointRecharge);
                wielders.remove();
                continue;
            }
            Iterator<Map.Entry<Hand, RechargeState>> hands = wielder.hands.entrySet().iterator();
            while (hands.hasNext()) {
                Map.Entry<Hand, RechargeState> handEntry = hands.next();
                ItemStack held = actor.getStackInHand(handEntry.getKey());
                if (held != handEntry.getValue().stackReference
                        || !held.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get())) {
                    checkpointRecharge(handEntry.getValue());
                    hands.remove();
                }
            }
            if (wielder.hands.isEmpty()) wielders.remove();
        }
        if (active.isEmpty()) RECHARGE.remove(world);
    }

    private static void flushRechargeForStack(ServerWorld world, UUID actorId, ItemStack stack) {
        Map<UUID, WielderRecharge> active = RECHARGE.get(world);
        WielderRecharge wielder = active == null ? null : active.get(actorId);
        if (wielder == null) return;
        Iterator<RechargeState> iterator = wielder.hands.values().iterator();
        while (iterator.hasNext()) {
            RechargeState state = iterator.next();
            if (state.stackReference == stack) {
                checkpointRecharge(state);
                iterator.remove();
            }
        }
        if (wielder.hands.isEmpty()) active.remove(actorId);
        if (active.isEmpty()) RECHARGE.remove(world);
    }

    private static void flushWielder(ServerWorld world, UUID actorId) {
        Map<UUID, WielderRecharge> active = RECHARGE.get(world);
        WielderRecharge wielder = active == null ? null : active.remove(actorId);
        if (wielder != null) wielder.hands.values().forEach(IonboundStormscaleAbilityManager::checkpointRecharge);
        if (active != null && active.isEmpty()) RECHARGE.remove(world);
        Map<UUID, ActiveCorridor> corridors = CORRIDORS.get(world);
        ActiveCorridor corridor = corridors == null ? null : corridors.remove(actorId);
        if (corridor != null) {
            discardVisual(world, corridor.visualId);
            UniqueAbilityApi.cancel(corridor.execution);
        }
        if (corridors != null && corridors.isEmpty()) CORRIDORS.remove(world);
        Map<UUID, ActiveShield> shields = SHIELDS.get(world);
        ActiveShield shield = shields == null ? null : shields.remove(actorId);
        if (shield != null) discardVisual(world, shield.visualId());
        if (shields != null && shields.isEmpty()) SHIELDS.remove(world);
        Map<UUID, ActiveBeam> beams = BEAMS.get(world);
        ActiveBeam beam = beams == null ? null : beams.remove(actorId);
        if (beam != null) {
            discardVisual(world, beam.visualId);
            UniqueAbilityApi.cancel(beam.execution);
        }
        if (beams != null && beams.isEmpty()) BEAMS.remove(world);
        LivingEntity actor = resolveLivingForCleanup(world, actorId);
        if (actor != null) removeBeamMovementSlow(actor);
        discardOrbit(world, actorId);
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        java.util.Set<UUID> tracked = new java.util.HashSet<>();
        CORRIDORS.getOrDefault(world, Map.of()).values().forEach(corridor -> {
            if (corridor.visualId != null) tracked.add(corridor.visualId);
        });
        SHIELDS.getOrDefault(world, Map.of()).values().forEach(shield -> tracked.add(shield.visualId()));
        BEAMS.getOrDefault(world, Map.of()).values().forEach(beam -> tracked.add(beam.visualId));
        tracked.addAll(ORBIT_VISUALS.getOrDefault(world, Map.of()).values());
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof IonboundStormscaleVisualEntity && !tracked.contains(entity.getUuid())) {
                entity.discard();
            }
        }
    }

    private static void applyBeamMovementSlow(LivingEntity actor) {
        applyBeamMovementSlow(actor, StormSoulMasteryTuning.EMPTY);
    }

    private static void applyBeamMovementSlow(LivingEntity actor, StormSoulMasteryTuning tuning) {
        EntityAttributeInstance movementSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null) return;
        double reduction = -MathHelper.clamp(
                1 - tuning.get(StormSoulMasteryTuning.Setting.MOVEMENT_SPEED,
                        1 - Config.uniqueEffects.ionbound_stormscale.beamMovementSpeedReduction), 0.0, 0.99);
        EntityAttributeModifier current = movementSpeed.getModifier(BEAM_MOVEMENT_SLOW_ID);
        if (current != null && current.value() == reduction
                && current.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return;
        }
        movementSpeed.removeModifier(BEAM_MOVEMENT_SLOW_ID);
        movementSpeed.addTemporaryModifier(new EntityAttributeModifier(BEAM_MOVEMENT_SLOW_ID, reduction,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeBeamMovementSlow(LivingEntity actor) {
        EntityAttributeInstance movementSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) movementSpeed.removeModifier(BEAM_MOVEMENT_SLOW_ID);
    }

    private static void checkpointRecharge(RechargeState state) {
        if (state == null || !state.stackReference.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get())) return;
        IonCubeComponent current = getCubes(state.stackReference);
        IonCubeComponent checkpoint = new IonCubeComponent(current.cubes(), state.rechargeTicks);
        if (!checkpoint.equals(current)) {
            state.stackReference.set(ComponentTypeRegistry.ION_CUBES.get(), checkpoint);
        }
    }

    private static Hand heldHand(LivingEntity actor, ItemStack stack) {
        if (actor.getMainHandStack() == stack) return Hand.MAIN_HAND;
        if (actor.getOffHandStack() == stack) return Hand.OFF_HAND;
        return null;
    }

    private static int visibleCubeCount(LivingEntity actor) {
        ItemStack main = actor.getMainHandStack();
        if (main.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get())) return getCubes(main).cubes();
        ItemStack off = actor.getOffHandStack();
        return off.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get()) ? getCubes(off).cubes() : 0;
    }

    private static ItemStack findHeldIonbound(LivingEntity actor) {
        ItemStack main = actor.getMainHandStack();
        if (main.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get())) return main;
        ItemStack off = actor.getOffHandStack();
        return off.isOf(ItemsRegistry.IONBOUND_STORMSCALE.get()) ? off : ItemStack.EMPTY;
    }

    private static boolean isHoldingIonbound(LivingEntity actor) {
        return !findHeldIonbound(actor).isEmpty();
    }

    private static ActiveCorridor getCorridor(ServerWorld world, UUID actorId) {
        Map<UUID, ActiveCorridor> active = CORRIDORS.get(world);
        return active == null ? null : active.get(actorId);
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        Entity entity = id == null ? null : world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static LivingEntity resolveLivingForCleanup(ServerWorld world, UUID id) {
        if (id == null) return null;
        for (ServerWorld candidate : world.getServer().getWorlds()) {
            Entity entity = candidate.getEntity(id);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction == null ? Vec3d.ZERO : new Vec3d(direction.x, 0.0, direction.z);
        if (horizontal.lengthSquared() < 0.0001) {
            Vec3d look = actor.getRotationVec(1.0F);
            horizontal = new Vec3d(look.x, 0.0, look.z);
        }
        if (horizontal.lengthSquared() < 0.0001) horizontal = Vec3d.fromPolar(0.0F, actor.getYaw());
        return horizontal.normalize();
    }

    private static void applyCooldown(ServerWorld world, LivingEntity actor, ItemStack stack) {
        int cooldown = Math.max(1, Config.uniqueEffects.ionbound_stormscale.cooldown);
        SimplySwordsAPI.setWeaponCooldown(actor, stack, cooldown);
    }

    private static void discardVisual(ServerWorld world, UUID id) {
        Entity visual = id == null ? null : world.getEntity(id);
        if (visual != null) visual.discard();
    }

    private static void spawnCubeGeneratedEffects(ServerWorld world, LivingEntity actor, int cubes) {
        Vec3d centre = actor.getPos().add(0.0, actor.getHeight() * 0.7, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 18, 0.7, 0.45, 0.7, 0.08);
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK,
                actor.getSoundCategory(), 0.55F, 1.25F + cubes * 0.08F);
    }

    private static void spawnShieldStartEffects(ServerWorld world, LivingEntity actor) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, actor.getX(), actor.getBodyY(0.55), actor.getZ(),
                42, 0.65, 0.8, 0.65, 0.12);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_BLOCK_01.get(), actor.getSoundCategory(), 1.0F, 1.35F);
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, actor.getSoundCategory(), 0.7F, 1.6F);
    }

    private static void spawnShieldImpactEffects(ServerWorld world, LivingEntity actor) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, actor.getX(), actor.getBodyY(0.55), actor.getZ(),
                10, 0.5, 0.65, 0.5, 0.1);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_BLOCK_01.get(), actor.getSoundCategory(), 0.45F, 1.65F);
    }

    private static void spawnShieldEndEffects(ServerWorld world, LivingEntity actor) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, actor.getX(), actor.getBodyY(0.5), actor.getZ(),
                22, 0.75, 0.85, 0.75, 0.16);
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, actor.getSoundCategory(), 0.55F, 1.8F);
    }

    private static void spawnCorridorStartEffects(ServerWorld world, LivingEntity actor, Vec3d origin) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y + 0.15, origin.z,
                28, 0.7, 0.12, 0.7, 0.1);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                SoundCategory.PLAYERS, 1.0F, 0.72F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_01.get(),
                SoundCategory.PLAYERS, 0.78F, 0.7F);
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.8F, 0.75F);
    }

    private static void spawnSlamEffects(ServerWorld world, ActiveCorridor corridor) {
        Vec3d centre = corridor.origin.add(corridor.forward.multiply(Config.uniqueEffects.ionbound_stormscale.corridorLength * 0.5));
        world.spawnParticles(ParticleTypes.FLASH, centre.x, centre.y + 1.8, centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y + 1.5, centre.z, 60, 0.45, 1.4, 0.45, 0.18);
        world.playSound(null, centre.x, centre.y, centre.z, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 1.2F, 0.68F);
        world.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS, 0.65F, 1.45F);
        world.playSound(null, centre.x, centre.y, centre.z, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 0.9F, 0.78F);
    }

    private static void spawnSlamTargetEffects(ServerWorld world, LivingEntity target) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(),
                12, 0.3, 0.45, 0.3, 0.09);
    }

    private static void spawnBeamStartEffects(ServerWorld world, LivingEntity actor) {
        Vec3d origin = beamOrigin(actor);
        world.spawnParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z,
                1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                32, 0.4, 0.5, 0.4, 0.16);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(),
                SoundCategory.PLAYERS, 1.0F, 1.75F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_03.get(),
                SoundCategory.PLAYERS, 0.9F, 1.35F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                SoundCategory.PLAYERS, 1.15F, 0.62F);
    }

    private static void spawnBeamSustainEffects(ServerWorld world, LivingEntity actor, long beamAge) {
        Vec3d origin = beamOrigin(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                10, 0.28, 0.35, 0.28, 0.08);
        if ((beamAge / 20L & 1L) == 0L) {
            world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_FLYBY_03.get(),
                    SoundCategory.PLAYERS, 0.62F, 0.92F);
        } else {
            world.playSound(null, actor.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_02.get(),
                    SoundCategory.PLAYERS, 0.58F, 1.12F);
        }
    }

    private static void spawnBeamEndEffects(ServerWorld world, LivingEntity actor) {
        Vec3d origin = beamOrigin(actor);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                18, 0.4, 0.45, 0.4, 0.12);
        world.playSound(null, actor.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS, 0.55F, 1.65F);
    }

    private static void spawnBeamTargetEffects(ServerWorld world, LivingEntity target) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(),
                24, 0.3, 0.55, 0.3, 0.13);
    }

    private static final class ActiveCorridor {
        private final UUID actorId;
        private final ItemStack stackReference;
        private final ItemStack stackSnapshot;
        private final Vec3d origin;
        private final Vec3d forward;
        private final Vec3d right;
        private final long startedAt;
        private final long slamAt;
        private UUID visualId;
        private boolean slammed;
        private long followupEnds = Long.MIN_VALUE;
        private final StormSoulMasteryTuning tuning;
        private final UniqueAbilityExecution execution;

        private ActiveCorridor(UUID actorId, ItemStack stackReference, ItemStack stackSnapshot,
                               Vec3d origin, Vec3d forward, Vec3d right, long startedAt, long slamAt,
                               StormSoulMasteryTuning tuning, UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.stackReference = stackReference;
            this.stackSnapshot = stackSnapshot;
            this.origin = origin;
            this.forward = forward;
            this.right = right;
            this.startedAt = startedAt;
            this.slamAt = slamAt;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private static final class ActiveBeam {
        private final UUID actorId;
        private final ItemStack stackReference;
        private final ItemStack stackSnapshot;
        private final long startedAt;
        private final long endsAt;
        private final UUID visualId;
        private final StormSoulMasteryTuning tuning;
        private final UniqueAbilityExecution execution;

        private ActiveBeam(UUID actorId, ItemStack stackReference, ItemStack stackSnapshot,
                           long startedAt, long endsAt, UUID visualId,
                           StormSoulMasteryTuning tuning, UniqueAbilityExecution execution) {
            this.actorId = actorId;
            this.stackReference = stackReference;
            this.stackSnapshot = stackSnapshot;
            this.startedAt = startedAt;
            this.endsAt = endsAt;
            this.visualId = visualId;
            this.tuning = tuning;
            this.execution = execution;
        }
    }

    private record ActiveShield(UUID actorId, long expiresAt, UUID visualId) {
    }

    private static final class ReserveState {
        private int storedCharges;
        private long storedChargeExpires = Long.MIN_VALUE;
        private long storedChargeNext = Long.MIN_VALUE;
        private long emergencyReady = Long.MIN_VALUE;
        private int rechargeRefund;
        private double beamWidthMultiplier;
    }

    private static final class WielderRecharge {
        private final Map<Hand, RechargeState> hands = new EnumMap<>(Hand.class);
    }

    private static final class RechargeState {
        private final ItemStack stackReference;
        private int rechargeTicks;
        private long lastAdvancedTick = Long.MIN_VALUE;
        private final StormSoulMasteryTuning tuning;

        private RechargeState(ItemStack stackReference, int rechargeTicks, StormSoulMasteryTuning tuning) {
            this.stackReference = stackReference;
            this.rechargeTicks = Math.max(0, rechargeTicks);
            this.tuning = tuning;
        }
    }
}
