package net.sweenus.simplyswords.world;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.ability.Phase5AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase5UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.MoltenRuptureVisualEntity;
import net.sweenus.simplyswords.item.component.MoltenHeatComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;

public final class MoltenEdgeAbilityManager {

    private static final int VISUAL_RISE_TICKS = 2;
    private static final int VISUAL_HOLD_TICKS = 2;
    private static final int VISUAL_SINK_TICKS = 5;
    private static final double VISUAL_START_DEPTH = 1.35;
    private static final int GROUND_SCAN_UP = 5;
    private static final int GROUND_SCAN_DOWN = 14;
    private static final String RUPTURE_VISUAL_TAG = "simplyswords_molten_rupture_visual";
    private static final Identifier VENT_SPEED_ID = Identifier.of("simplyswords", "molten_edge_vent_speed");

    private static final Map<ServerWorld, Map<UUID, ActiveVent>> ACTIVE_VENTS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveShockwave>> ACTIVE_SHOCKWAVES = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveRupture>> ACTIVE_RUPTURES = new HashMap<>();
    private static final Map<ServerWorld, List<RuptureVisual>> ACTIVE_VISUALS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Map<UUID, SequenceState>>> SEQUENCES = new HashMap<>();

    private MoltenEdgeAbilityManager() {
    }

    public static MoltenHeatComponent getHeatComponent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
            return MoltenHeatComponent.DEFAULT;
        }
        return stack.getOrDefault(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
    }

    public static int getHeat(LivingEntity wielder) {
        if (!isWieldingMoltenEdge(wielder)) {
            return 0;
        }
        long now = wielder.getWorld().getTime();
        int heat = 0;
        for (ItemStack stack : getHeldMoltenEdges(wielder)) {
            heat = Math.max(heat, getEffectiveHeat(getHeatComponent(stack), now,
                    getVentDrainPerTick(wielder, stack)));
        }
        return heat;
    }

    public static int getHeat(LivingEntity wielder, ItemStack stack) {
        if (!isHeldMoltenEdge(wielder, stack)) {
            return 0;
        }
        return getEffectiveHeat(getHeatComponent(stack), wielder.getWorld().getTime(),
                getVentDrainPerTick(wielder, stack));
    }

    public static boolean isVenting(LivingEntity wielder) {
        if (!isWieldingMoltenEdge(wielder)) {
            return false;
        }
        long now = wielder.getWorld().getTime();
        for (ItemStack stack : getHeldMoltenEdges(wielder)) {
            if (getHeatComponent(stack).isVentingAt(now, getVentDrainPerTick(wielder, stack))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVenting(LivingEntity wielder, ItemStack stack) {
        if (!isHeldMoltenEdge(wielder, stack)) {
            return false;
        }
        return getHeatComponent(stack).isVentingAt(wielder.getWorld().getTime(),
                getVentDrainPerTick(wielder, stack));
    }

    public static boolean isHeldMoltenEdge(LivingEntity wielder, ItemStack stack) {
        return wielder != null
                && stack != null
                && !stack.isEmpty()
                && stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())
                && AwakeningApi.isAbilityUnlocked(stack)
                && (wielder.getMainHandStack() == stack || wielder.getOffHandStack() == stack);
    }

    public static void gainHeatFromMelee(ItemStack stack, LivingEntity attacker) {
        gainHeatFromMelee(stack, attacker, null);
    }

    public static void gainHeatFromMelee(ItemStack stack, LivingEntity attacker, LivingEntity target) {
        if (stack == null
                || attacker == null
                || attacker.getWorld().isClient()
                || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())
                || !isHeldMoltenEdge(attacker, stack)) {
            return;
        }

        MoltenHeatComponent heat = getHeatComponent(stack);
        long now = attacker.getWorld().getTime();
        int drain = getVentDrainPerTick(attacker, stack);
        if (heat.isVentingAt(now, drain)) {
            return;
        }
        Phase5AbilityTuning tuning = Phase5MoltenManager.heat((ServerWorld) attacker.getWorld(), stack, attacker);
        int gain = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_MELEE_HEAT_GAIN,
                Config.uniqueEffects.molten_edge.heatPerHit);
        int maximum = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX, MoltenHeatComponent.MAX_HEAT);
        MoltenHeatComponent normalized = heat.normalizedAt(now, drain);
        MoltenHeatComponent increased = normalized.addHeat(gain);
        if (increased.heat() > maximum) increased = new MoltenHeatComponent(maximum, false);
        stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), increased);
        applyHeatGainRewards(attacker, stack, tuning, normalized.heat(), increased.heat());
        igniteAtMaximumHeat(attacker, increased, tuning);
        if (target != null && target.isAlive() && tuning.flag(1 << 6) && increased.heat() >= maximum) {
            int fireTicks = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_WHITE_HOT_FIRE_TICKS, 60);
            if (fireTicks > 0) target.setOnFireForTicks(fireTicks);
        }
    }

    public static void gainHeatFromIncomingDamage(LivingEntity target, float amount, boolean damageApplied) {
        if (!damageApplied
                || amount <= 0.0F
                || target == null
                || target.getWorld().isClient()
                || !isWieldingMoltenEdge(target)) {
            return;
        }

        long now = target.getWorld().getTime();
        for (ItemStack stack : getHeldMoltenEdges(target)) {
            MoltenHeatComponent heat = getHeatComponent(stack);
            int drain = getVentDrainPerTick(target, stack);
            if (heat.isVentingAt(now, drain)) {
                continue;
            }
            Phase5AbilityTuning tuning = Phase5MoltenManager.heat((ServerWorld) target.getWorld(), stack, target);
            int gain = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_INCOMING_HEAT_GAIN,
                    Config.uniqueEffects.molten_edge.heatPerDamageTaken);
            int maximum = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX, MoltenHeatComponent.MAX_HEAT);
            MoltenHeatComponent normalized = heat.normalizedAt(now, drain);
            MoltenHeatComponent increased = normalized.addHeat(gain);
            if (increased.heat() > maximum) increased = new MoltenHeatComponent(maximum, false);
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), increased);
            applyHeatGainRewards(target, stack, tuning, normalized.heat(), increased.heat());
            igniteAtMaximumHeat(target, increased, tuning);
        }
    }

    public static float modifyOutgoingDamage(DamageSource source, float amount) {
        if (source == null || amount <= 0.0F) {
            return amount;
        }
        LivingEntity attacker = HelperMethods.resolveAbilityDamageActor(source);
        int heat = getHeat(attacker);
        if (heat <= 0) {
            return amount;
        }
        ItemStack stack = attacker == null ? null : getHeldMoltenEdges(attacker).stream().findFirst().orElse(null);
        Phase5AbilityTuning tuning = stack == null || !(attacker.getWorld() instanceof ServerWorld world)
                ? Phase5AbilityTuning.EMPTY : Phase5MoltenManager.heat(world, stack, attacker);
        float multiplier = 1 + heat / 200F;
        boolean moltenMelee = source.getWeaponStack() != null
                && source.getWeaponStack().isOf(ItemsRegistry.MOLTEN_EDGE.get());
        int maximum = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX, 100);
        if (moltenMelee && tuning.flag(1 << 3)
                && heat >= tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_REDLINE_THRESHOLD, 75)) {
            multiplier *= (float) tuning.get(Phase5AbilityTuning.Setting.MOLTEN_REDLINE_DAMAGE_MULTIPLIER, 1.1);
        }
        if (moltenMelee && tuning.flag(1 << 6) && heat >= maximum) {
            multiplier *= (float) tuning.get(Phase5AbilityTuning.Setting.MOLTEN_WHITE_HOT_DAMAGE_MULTIPLIER, 1.12);
        }
        if (tuning.flag(1 << 7) && stack.getOrDefault(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false)) {
            multiplier *= (float) tuning.get(Phase5AbilityTuning.Setting.MOLTEN_OVERCLOCK_OUTGOING_MULTIPLIER, 1.35);
        }
        if (tuning.flag(1 << 8)) {
            multiplier = Math.min(multiplier, 1 + (float) tuning.get(
                    Phase5AbilityTuning.Setting.MOLTEN_OUTGOING_BONUS_CAP, .35));
        }
        return amount * multiplier;
    }

    public static float modifyIncomingDamage(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F) {
            return amount;
        }
        int heat = getHeat(target);
        if (heat <= 0) {
            return amount;
        }
        ItemStack stack = getHeldMoltenEdges(target).stream().findFirst().orElse(null);
        Phase5AbilityTuning tuning = stack == null || !(target.getWorld() instanceof ServerWorld world)
                ? Phase5AbilityTuning.EMPTY : Phase5MoltenManager.heat(world, stack, target);
        float amplification = heat / 100F;
        if (tuning.flag(1 << 7) && stack.getOrDefault(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false)) {
            amplification = (float) tuning.get(
                    Phase5AbilityTuning.Setting.MOLTEN_OVERCLOCK_INCOMING_AMPLIFICATION, 1);
        }
        if (tuning.flag(1 << 2)) amplification *= (float) tuning.get(
                Phase5AbilityTuning.Setting.MOLTEN_INCOMING_PENALTY_MULTIPLIER, .85);
        if (tuning.flag(1 << 8)) amplification = Math.min(amplification, (float) tuning.get(
                Phase5AbilityTuning.Setting.MOLTEN_INCOMING_AMPLIFICATION_CAP, .5));
        return amount * (1 + amplification);
    }

    private static void applyHeatGainRewards(LivingEntity actor, ItemStack stack, Phase5AbilityTuning tuning,
                                             int previousHeat, int currentHeat) {
        if (tuning.flag(1 << 7) && currentHeat >= MoltenHeatComponent.MAX_HEAT) {
            stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), true);
        }
        if (!tuning.flag(1 << 4) || currentHeat <= previousHeat) return;
        int step = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_SINK_STEP, 20);
        if (!crossedHeatThreshold(previousHeat, currentHeat, step)) return;
        Phase4AbsorptionTracker.grant(actor,
                (float) tuning.get(Phase5AbilityTuning.Setting.MOLTEN_HEAT_SINK_ABSORPTION, 2),
                tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_SINK_DURATION_TICKS, 60),
                (float) tuning.get(Phase5AbilityTuning.Setting.MOLTEN_HEAT_SINK_CAP, 6));
    }

    public static boolean startVent(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || !isHeldMoltenEdge(context.actor(), context.stack())) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        ItemStack heldStack = context.stack();
        MoltenHeatComponent heat = getHeatComponent(heldStack);
        long now = world.getTime();
        int currentHeat = getEffectiveHeat(heat, now, getVentDrainPerTick(actor, heldStack));
        if (currentHeat <= 0 || heat.venting()) {
            return false;
        }

        Map<UUID, ActiveVent> vents = ACTIVE_VENTS.computeIfAbsent(world, ignored -> new HashMap<>());
        if (vents.containsKey(actor.getUuid())) {
            return false;
        }

        Phase5MoltenManager.Snapshot mastery = Phase5MoltenManager.beginVent(context);
        Phase5AbilityTuning tuning = mastery.tuning();
        Phase5AbilityTuning heatTuning = Phase5MoltenManager.heat(world, heldStack, actor);
        MoltenHeatComponent ventingHeat = new MoltenHeatComponent(currentHeat, false).startVenting(now);
        heldStack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), ventingHeat);
        StatusEffectInstance resistance = actor.getStatusEffect(StatusEffects.RESISTANCE);
        ActiveVent vent = new ActiveVent(actor.getUuid(), heldStack, mastery, tuning, heatTuning, now,
                resistance == null ? null : new StatusEffectInstance(resistance));
        vents.put(actor.getUuid(), vent);
        applyVentMovement(actor, tuning);
        if (tuning.flag(1 << 12)) {
            actor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                    tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_BLAST_SPEED_TICKS, 60), 0), actor);
        }

        int heatMaximum = heatTuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX,
                MoltenHeatComponent.MAX_HEAT);
        float heatFraction = shockwaveHeatFraction(currentHeat, heatMaximum);
        float shockwaveDamage = HelperMethods.abilityScaledDamage(
                "fire",
                actor,
                heldStack,
                Config.uniqueEffects.molten_edge.shockwaveDamageScaling,
                Config.uniqueEffects.molten_edge.shockwaveSpellScaling
        ) * heatFraction * (float) tuning.get(
                Phase5AbilityTuning.Setting.MOLTEN_SHOCKWAVE_DAMAGE_MULTIPLIER, 1);
        ACTIVE_SHOCKWAVES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveShockwave(
                actor.getUuid(),
                heldStack.copy(),
                actor.getPos(),
                heatFraction,
                shockwaveDamage,
                tuning,
                0,
                new HashSet<>()
        ));
        spawnVentStartEffects(world, actor, heatFraction);
        return true;
    }

    public static void tryFireRupture(ServerWorld world, LivingEntity wielder, ItemStack stack, Hand hand) {
        if (world == null
                || wielder == null
                || stack == null
                || stack.isEmpty()
                || hand == null
                || !wielder.isAlive()
                || wielder.getStackInHand(hand) != stack
                || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
            return;
        }

        ActiveVent vent = activeVent(world, wielder.getUuid());
        MoltenHeatComponent heat = getHeatComponent(stack);
        long now = world.getTime();
        if (vent == null || vent.stackReference != stack || vent.tuning.flag(1 << 17)
                || !heat.isVentingAt(now, ventDrain(vent))
                || getEffectiveHeat(heat, now, ventDrain(vent)) <= 0) {
            return;
        }

        Phase5MoltenManager.Snapshot rupture = Phase5MoltenManager.beginRupture(world, stack, wielder);
        if (!isAttackReady(world, wielder, stack, vent, rupture.tuning())) {
            Phase5MoltenManager.cancelRupture(rupture);
            return;
        }
        vent.swingCount++;
        spawnRuptureCast(world, wielder, stack, vent, rupture, horizontalDirection(wielder), true);

        world.playSound(null, wielder.getX(), wielder.getY(), wielder.getZ(), SoundRegistry.SWING_WOOSH.get(),
                SoundCategory.PLAYERS, 0.7F, 0.72F + world.random.nextFloat() * 0.12F);
        world.playSound(null, wielder.getX(), wielder.getY(), wielder.getZ(), SoundEvents.BLOCK_FIRE_AMBIENT,
                SoundCategory.PLAYERS, 0.55F, 0.62F + world.random.nextFloat() * 0.1F);
    }

    private static void spawnRuptureCast(ServerWorld world, LivingEntity owner, ItemStack stack, ActiveVent vent,
                                         Phase5MoltenManager.Snapshot snapshot, Vec3d requestedDirection,
                                         boolean swing) {
        Phase5AbilityTuning tuning = snapshot.tuning();
        Vec3d forward = seekDirection(world, owner, requestedDirection, vent.tuning);
        float baseDamage = HelperMethods.abilityScaledDamage("fire", owner, stack,
                Config.uniqueEffects.molten_edge.ruptureDamageScaling,
                Config.uniqueEffects.molten_edge.ruptureSpellScaling)
                * (float) tuning.get(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_DAMAGE_MULTIPLIER, 1);
        List<LaneOrigin> origins = new ArrayList<>();
        origins.add(new LaneOrigin(forward, 1));
        boolean fissure = tuning.flag(1 << 25);
        boolean shatter = tuning.flag(1 << 26);
        if (swing && tuning.flag(1 << 23) && shouldFork(vent.swingCount,
                tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_FORK_SWING_INTERVAL, 3), fissure || shatter)) {
            int forks = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_FORK_COUNT, 2);
            double angle = tuning.get(Phase5AbilityTuning.Setting.MOLTEN_FORK_ANGLE_DEGREES, 18);
            double multiplier = tuning.get(Phase5AbilityTuning.Setting.MOLTEN_FORK_DAMAGE_MULTIPLIER, .55);
            for (int i = 0; i < forks; i++) {
                double sign = i % 2 == 0 ? -1 : 1;
                double ring = i / 2 + 1;
                origins.add(new LaneOrigin(rotateHorizontal(forward, sign * ring * angle), multiplier));
            }
        }
        int laneCount = shatter ? Math.max(1, tuning.integer(
                Phase5AbilityTuning.Setting.MOLTEN_SHATTER_LANE_COUNT, 5)) : 1;
        int totalLanes = Math.max(1, origins.size() * laneCount);
        RuptureCast cast = new RuptureCast(snapshot, totalLanes, new HashSet<>());
        for (LaneOrigin laneOrigin : origins) {
            for (int lane = 0; lane < laneCount; lane++) {
                double offset = shatter ? laneOffset(lane, laneCount, tuning.get(
                        Phase5AbilityTuning.Setting.MOLTEN_SHATTER_LANE_ANGLE_DEGREES, 18)) : 0;
                Vec3d laneForward = rotateHorizontal(laneOrigin.forward, offset);
                double length = shatter
                        ? tuning.get(Phase5AbilityTuning.Setting.MOLTEN_SHATTER_LANE_LENGTH, 5)
                        : tuning.get(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_LENGTH,
                        Config.uniqueEffects.molten_edge.ruptureLength);
                double damageMultiplier = laneOrigin.damageMultiplier * (shatter
                        ? tuning.get(Phase5AbilityTuning.Setting.MOLTEN_SHATTER_DAMAGE_MULTIPLIER, .45) : 1);
                spawnRuptureLane(world, owner, stack, cast, laneForward, baseDamage * (float) damageMultiplier, length);
            }
        }
    }

    private static void spawnRuptureLane(ServerWorld world, LivingEntity owner, ItemStack stack, RuptureCast cast,
                                         Vec3d forward, float damage, double length) {
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        Vec3d origin = owner.getPos().add(forward.multiply(.65));
        int steps = Math.max(1, (int) Math.ceil(length
                / Math.max(.25, Config.uniqueEffects.molten_edge.ruptureStepDistance)));
        ACTIVE_RUPTURES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveRupture(
                owner.getUuid(), stack.copy(), origin, forward, right, damage, steps, 0, cast));
    }

    public static void tickHeldStack(ItemStack stack, World world, Entity entity) {
        if (world.isClient() || !(entity instanceof LivingEntity living) || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
            return;
        }

        MoltenHeatComponent heat = getHeatComponent(stack);
        if (!AwakeningApi.isAbilityUnlocked(stack)) {
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
            stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
            cancelVent(living, stack);
            return;
        }
        if (!isHeldMoltenEdge(living, stack)) {
            Phase5AbilityTuning tuning = Phase5MoltenManager.heat((ServerWorld) world, stack, living);
            int retained = getEffectiveHeat(heat, world.getTime(), getVentDrainPerTick(living, stack));
            cancelVent(living, stack);
            stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
            if (tuning.flag(1 << 5) && retained > 0) {
                int interval = tuning.integer(
                        Phase5AbilityTuning.Setting.MOLTEN_UNWIELDED_DECAY_INTERVAL_TICKS, 2);
                retained = decayedUnwieldedHeat(retained, world.getTime(), interval);
                stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), new MoltenHeatComponent(retained, false));
            } else {
                stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
            }
            return;
        }

        if (!living.isAlive()) {
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
            stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
            cancelVent(living, stack);
            return;
        }

        ActiveVent vent = activeVent((ServerWorld) world, living.getUuid());
        if (vent == null || vent.stackReference != stack) removeVentMovement(living);
        if (heat.venting() && (vent == null || vent.stackReference != stack)) {
            heat = heat.normalizedAt(world.getTime(), getVentDrainPerTick(living, stack));
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), heat);
        }
        Phase5AbilityTuning tuning = Phase5MoltenManager.heat((ServerWorld) world, stack, living);
        int maximum = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX,
                MoltenHeatComponent.MAX_HEAT);
        if (heat.heat() > maximum) {
            heat = new MoltenHeatComponent(maximum, false);
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), heat);
        }
        if (!tuning.flag(1 << 7)) stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
        if (!heat.venting() && living.age % 10 == 0) {
            igniteAtMaximumHeat(living, heat, tuning);
        }
    }

    public static void cancelVent(LivingEntity wielder) {
        cancelVent(wielder, null);
    }

    public static void cancelVent(LivingEntity wielder, ItemStack stack) {
        if (wielder == null || !(wielder.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Map<UUID, ActiveVent> vents = ACTIVE_VENTS.get(world);
        ActiveVent vent = vents == null ? null : vents.get(wielder.getUuid());
        if (vent != null && stack != null && vent.stackReference != stack) {
            return;
        }
        if (vent != null) {
            vents.remove(wielder.getUuid());
            cleanupVent(wielder, vent, false, 0);
        }
        if (vents != null && vents.isEmpty()) {
            ACTIVE_VENTS.remove(world);
        }
    }

    public static void resetWielder(LivingEntity wielder) {
        if (wielder == null) {
            return;
        }
        for (ItemStack stack : getHeldMoltenEdges(wielder)) {
            stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
            stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
        }
        if (wielder instanceof PlayerEntity player) {
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (!stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) continue;
                stack.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
                stack.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
            }
        }
        cancelVent(wielder);
        if (wielder.getWorld() instanceof ServerWorld world) {
            Map<UUID, Map<UUID, SequenceState>> sequences = SEQUENCES.get(world);
            if (sequences != null) sequences.remove(wielder.getUuid());
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        Map<UUID, ActiveVent> vents = ACTIVE_VENTS.remove(world);
        if (vents != null) {
            for (ActiveVent vent : vents.values()) {
                Entity entity = world.getEntity(vent.ownerId);
                if (entity instanceof LivingEntity owner) cleanupVent(owner, vent, false, 0);
                else Phase5MoltenManager.cancel(vent.ownerId);
            }
        }
        ACTIVE_SHOCKWAVES.remove(world);
        List<ActiveRupture> ruptures = ACTIVE_RUPTURES.remove(world);
        if (ruptures != null) ruptures.forEach(rupture -> rupture.cast.cancel());
        List<RuptureVisual> visuals = ACTIVE_VISUALS.remove(world);
        if (visuals != null) visuals.forEach(visual -> {
            Entity entity = world.getEntity(visual.id);
            if (entity != null) entity.discard();
        });
        SEQUENCES.remove(world);
        Phase5MoltenManager.clear(world);
    }

    public static void clearAll() {
        Set<ServerWorld> worlds = new HashSet<>();
        worlds.addAll(ACTIVE_VENTS.keySet());
        worlds.addAll(ACTIVE_SHOCKWAVES.keySet());
        worlds.addAll(ACTIVE_RUPTURES.keySet());
        worlds.addAll(ACTIVE_VISUALS.keySet());
        worlds.forEach(MoltenEdgeAbilityManager::clear);
        ACTIVE_VENTS.clear();
        ACTIVE_SHOCKWAVES.clear();
        ACTIVE_RUPTURES.clear();
        ACTIVE_VISUALS.clear();
        SEQUENCES.clear();
        Phase5MoltenManager.clearAll();
    }

    public static boolean hasActive(ServerWorld world) {
        return hasEntries(ACTIVE_VENTS.get(world))
                || hasEntries(ACTIVE_SHOCKWAVES.get(world))
                || hasEntries(ACTIVE_RUPTURES.get(world))
                || hasEntries(ACTIVE_VISUALS.get(world))
                || hasEntries(SEQUENCES.get(world));
    }

    public static void tick(ServerWorld world) {
        tickVents(world);
        tickShockwaves(world);
        tickRuptures(world);
        tickVisuals(world);
        pruneSequences(world);
    }

    private static void tickVents(ServerWorld world) {
        Map<UUID, ActiveVent> vents = ACTIVE_VENTS.get(world);
        if (vents == null || vents.isEmpty()) {
            return;
        }

        Iterator<ActiveVent> iterator = vents.values().iterator();
        while (iterator.hasNext()) {
            ActiveVent vent = iterator.next();
            Entity entity = world.getEntity(vent.ownerId);
            if (!(entity instanceof LivingEntity owner)) {
                vent.stackReference.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
                vent.stackReference.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
                Phase5MoltenManager.cancel(vent.ownerId);
                iterator.remove();
                continue;
            }
            if (!owner.isAlive()
                    || !isHeldMoltenEdge(owner, vent.stackReference)
                    || !vent.stackReference.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
                vent.stackReference.set(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
                vent.stackReference.set(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
                cleanupVent(owner, vent, false, 0);
                iterator.remove();
                continue;
            }

            MoltenHeatComponent heat = getHeatComponent(vent.stackReference);
            int drain = ventDrain(vent);
            int effectiveHeat = getEffectiveHeat(heat, world.getTime(), drain);
            boolean overclocked = vent.heatTuning.flag(1 << 7)
                    && vent.stackReference.getOrDefault(ComponentTypeRegistry.MOLTEN_OVERCLOCKED.get(), false);
            int heatFloor = overclocked ? Math.min(heat.heat(), vent.heatTuning.integer(
                    Phase5AbilityTuning.Setting.MOLTEN_OVERCLOCK_HEAT_FLOOR, 75)) : 0;
            if (heatFloor > 0 && effectiveHeat <= heatFloor) {
                spawnVentEndEffects(world, owner);
                cleanupVent(owner, vent, true, heatFloor);
                iterator.remove();
                continue;
            }
            if (!heat.isVentingAt(world.getTime(), drain) || effectiveHeat <= 0) {
                spawnVentEndEffects(world, owner);
                cleanupVent(owner, vent, true, 0);
                iterator.remove();
                continue;
            }

            vent.stackReference.set(ComponentTypeRegistry.MOLTEN_HEAT.get(),
                    new MoltenHeatComponent(effectiveHeat, false).startVenting(world.getTime()));
            if (vent.tuning.flag(1 << 17)) {
                owner.setVelocity(0, owner.getVelocity().y, 0);
                owner.velocityModified = true;
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 5,
                        vent.tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_RESISTANCE_AMPLIFIER, 1)), owner);
            }
            if (vent.tuning.flag(1 << 16)) {
                int interval = vent.tuning.integer(
                        Phase5AbilityTuning.Setting.MOLTEN_AUTO_RUPTURE_INTERVAL_TICKS, 8);
                if ((world.getTime() - vent.startedAt) % Math.max(1, interval) == 0) {
                    Vec3d movement = owner.getVelocity().multiply(1, 0, 1);
                    if (movement.lengthSquared() > .0025) {
                        Phase5MoltenManager.Snapshot rupture = Phase5MoltenManager.beginRupture(
                                world, vent.stackReference, owner);
                        spawnRuptureCast(world, owner, vent.stackReference, vent, rupture,
                                movement.normalize(), false);
                    }
                }
            }

            if (world.getTime() % 3L == 0L) {
                int maximum = vent.heatTuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX,
                        MoltenHeatComponent.MAX_HEAT);
                float fraction = shockwaveHeatFraction(effectiveHeat, maximum);
                spawnVentingEffects(world, owner, fraction);
            }
        }

        if (vents.isEmpty()) {
            ACTIVE_VENTS.remove(world);
        }
    }

    private static void tickShockwaves(ServerWorld world) {
        List<ActiveShockwave> shockwaves = ACTIVE_SHOCKWAVES.get(world);
        if (shockwaves == null || shockwaves.isEmpty()) {
            return;
        }

        shockwaves.removeIf(shockwave -> {
            Entity entity = world.getEntity(shockwave.ownerId);
            if (!(entity instanceof LivingEntity owner) || !owner.isAlive()) {
                return true;
            }

            int totalTicks = Math.max(1, Config.uniqueEffects.molten_edge.shockwaveTicks);
            if (shockwave.step >= totalTicks) {
                return true;
            }

            double maxRadius = shockwave.tuning.get(Phase5AbilityTuning.Setting.MOLTEN_SHOCKWAVE_RADIUS,
                    Config.uniqueEffects.molten_edge.radius);
            double previousRadius = maxRadius * shockwave.step / totalTicks;
            shockwave.step++;
            double currentRadius = maxRadius * shockwave.step / totalTicks;
            spawnShockwaveRing(world, shockwave.origin, currentRadius, shockwave.step);
            damageShockwaveTargets(world, owner, shockwave, previousRadius, currentRadius);
            return shockwave.step >= totalTicks;
        });

        if (shockwaves.isEmpty()) {
            ACTIVE_SHOCKWAVES.remove(world);
        }
    }

    private static void damageShockwaveTargets(ServerWorld world, LivingEntity owner, ActiveShockwave shockwave, double previousRadius, double currentRadius) {
        double outer = currentRadius + 1.0;
        double inner = Math.max(0.0, previousRadius - 1.0);
        Box box = Box.of(shockwave.origin.add(0.0, 0.8, 0.0), outer * 2.0, 3.5, outer * 2.0);
        DamageSource source = world.getDamageSources().indirectMagic(owner, owner);

        int cap = Math.max(0, shockwave.tuning.integer(
                Phase5AbilityTuning.Setting.MOLTEN_SHOCKWAVE_TARGET_CAP,
                Config.uniqueEffects.molten_edge.shockwaveTargetCap));
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)
                .stream().sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(shockwave.origin)))
                .toList();
        for (LivingEntity target : candidates) {
            if (shockwave.hitTargets.size() >= cap) break;
            if (target == owner
                    || shockwave.hitTargets.contains(target.getUuid())
                    || !HelperMethods.checkAbilityTarget(target, owner)) {
                continue;
            }
            double horizontalDistance = horizontalDistance(target.getPos(), shockwave.origin);
            if (horizontalDistance < inner || horizontalDistance > outer) {
                continue;
            }

            float damage = HelperMethods.applyAbilityDamageEnchantments(world, shockwave.stack, target, source, shockwave.damage);
            if (!HelperMethods.damageThroughIframes(target, source, damage)) {
                continue;
            }
            shockwave.hitTargets.add(target.getUuid());
            recordVentHit(world, owner, target, damage);

            target.setOnFireFor(Math.max(0, Config.uniqueEffects.molten_edge.shockwaveIgniteSeconds));
            Vec3d push = target.getPos().subtract(shockwave.origin);
            push = new Vec3d(push.x, 0.0, push.z);
            if (push.lengthSquared() < 0.0001) {
                push = horizontalDirection(owner);
            } else {
                push = push.normalize();
            }
            double strength = Math.max(0.0, Config.uniqueEffects.molten_edge.shockwaveKnockback)
                    * (0.5 + shockwave.heatFraction * 0.5)
                    * shockwave.tuning.get(
                    Phase5AbilityTuning.Setting.MOLTEN_SHOCKWAVE_KNOCKBACK_MULTIPLIER, 1);
            target.addVelocity(push.x * strength, 0.25, push.z * strength);
            target.velocityModified = true;
            target.velocityDirty = true;
            spawnShockwaveHitEffects(world, target);
        }
    }

    private static void tickRuptures(ServerWorld world) {
        List<ActiveRupture> ruptures = ACTIVE_RUPTURES.get(world);
        if (ruptures == null || ruptures.isEmpty()) {
            return;
        }

        ruptures.removeIf(rupture -> {
            Entity entity = world.getEntity(rupture.ownerId);
            if (!(entity instanceof LivingEntity owner) || !owner.isAlive() || rupture.step >= rupture.maxSteps) {
                rupture.finish(!(entity instanceof LivingEntity living) || !living.isAlive());
                return true;
            }

            rupture.step++;
            double stepDistance = Math.max(0.25, Config.uniqueEffects.molten_edge.ruptureStepDistance);
            Vec3d center = rupture.origin.add(rupture.forward.multiply(rupture.step * stepDistance));
            double groundY = findGroundTopY(world, center.x, center.z, center.y);
            Vec3d groundedCenter = new Vec3d(center.x, groundY, center.z);
            spawnRuptureStepEffects(world, rupture, groundedCenter);
            damageRuptureTargets(world, owner, rupture, groundedCenter, stepDistance);
            if (rupture.step >= rupture.maxSteps) {
                rupture.finish(false);
                return true;
            }
            return false;
        });

        if (ruptures.isEmpty()) {
            ACTIVE_RUPTURES.remove(world);
        }
    }

    private static void damageRuptureTargets(ServerWorld world, LivingEntity owner, ActiveRupture rupture, Vec3d center, double segmentLength) {
        Phase5AbilityTuning tuning = rupture.cast.snapshot.tuning();
        double width = tuning.get(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_WIDTH,
                Config.uniqueEffects.molten_edge.ruptureWidth);
        double xSize = Math.abs(rupture.forward.x) * segmentLength * 2.0 + Math.abs(rupture.right.x) * width + 2.0;
        double zSize = Math.abs(rupture.forward.z) * segmentLength * 2.0 + Math.abs(rupture.right.z) * width + 2.0;
        Box box = Box.of(center.add(0.0, 0.75, 0.0), xSize, 3.0, zSize);
        DamageSource source = world.getDamageSources().indirectMagic(owner, owner);

        int cap = Math.max(0, tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_SEGMENT_TARGET_CAP,
                Config.uniqueEffects.molten_edge.ruptureSegmentTargetCap));
        int affected = 0;
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)
                .stream().sorted(Comparator.comparingDouble(target -> target.squaredDistanceTo(center))).toList();
        for (LivingEntity target : candidates) {
            if (affected >= cap) break;
            if (target == owner
                    || rupture.cast.hitTargets.contains(target.getUuid())
                    || !HelperMethods.checkAbilityTarget(target, owner)) {
                continue;
            }

            Vec3d relative = target.getPos().subtract(center);
            double forwardDistance = Math.abs(relative.dotProduct(rupture.forward));
            double lateralDistance = Math.abs(relative.dotProduct(rupture.right));
            if (forwardDistance > segmentLength + target.getWidth() * 0.5
                    || lateralDistance > width * 0.5 + target.getWidth() * 0.5) {
                continue;
            }

            float sequence = sequenceMultiplier(world, owner, target, tuning);
            float rawDamage = rupture.damage * sequence;
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, rupture.stack, target, source, rawDamage);
            if (!HelperMethods.damageThroughIframes(target, source, damage)) {
                continue;
            }
            affected++;
            rupture.cast.hitTargets.add(target.getUuid());
            recordSequenceHit(world, owner, target, tuning);
            recordVentHit(world, owner, target, rawDamage);
            if (!rupture.refunded) {
                refundRuptureCooldown(world, owner, rupture.cast.snapshot.tuning());
                rupture.refunded = true;
            }
            UniqueAbilityApi.emit(rupture.cast.snapshot.execution(), UniqueAbilityPhase.HIT,
                    Phase5UniqueAbilities.HIT, target, 1, rawDamage);
            target.setOnFireForTicks(Math.max(0, tuning.integer(
                    Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_FIRE_TICKS,
                    Config.uniqueEffects.molten_edge.ruptureIgniteSeconds * 20)));
            target.addVelocity(rupture.forward.x * 0.18, tuning.get(
                    Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_KNOCK_UP,
                    Config.uniqueEffects.molten_edge.ruptureKnockUp), rupture.forward.z * 0.18);
            target.velocityModified = true;
            target.velocityDirty = true;
            world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(0.45), target.getZ(), 5, 0.25, 0.25, 0.25, 0.04);
            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.45), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private static void spawnRuptureStepEffects(ServerWorld world, ActiveRupture rupture, Vec3d center) {
        double width = rupture.cast.snapshot.tuning().get(
                Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_WIDTH,
                Config.uniqueEffects.molten_edge.ruptureWidth);
        double edgeOffset = width * 0.34;
        spawnRuptureVisual(world, center, 0.85F);
        spawnRuptureVisual(world, center.add(rupture.right.multiply(edgeOffset)), 0.48F);
        spawnRuptureVisual(world, center.add(rupture.right.multiply(-edgeOffset)), 0.48F);

        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.getDefaultState()),
                center.x, center.y + 0.12, center.z, 8, width * 0.28, 0.12, width * 0.28, 0.04);
        world.spawnParticles(ParticleTypes.LAVA, center.x, center.y + 0.18, center.z, 3, width * 0.22, 0.12, width * 0.22, 0.04);
        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z, 7, width * 0.3, 0.16, width * 0.3, 0.035);
        world.spawnParticles(ParticleTypes.SMOKE, center.x, center.y + 0.28, center.z, 5, width * 0.26, 0.18, width * 0.26, 0.025);
        if (rupture.step % 2 == 0) {
            world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_STONE_BREAK,
                    SoundCategory.PLAYERS, 0.38F, 0.58F + world.random.nextFloat() * 0.12F);
        }
    }

    private static void spawnRuptureVisual(ServerWorld world, Vec3d pos, float height) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }
        double groundY = findGroundTopY(world, pos.x, pos.z, pos.y);
        MoltenRuptureVisualEntity visual = new MoltenRuptureVisualEntity(world, pos.x, groundY - VISUAL_START_DEPTH, pos.z, height);
        visual.addCommandTag(RUPTURE_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            ACTIVE_VISUALS.computeIfAbsent(world, ignored -> new ArrayList<>())
                    .add(new RuptureVisual(visual.getUuid(), pos.x, groundY, pos.z, world.getTime()));
        }
    }

    private static void tickVisuals(ServerWorld world) {
        List<RuptureVisual> visuals = ACTIVE_VISUALS.get(world);
        if (visuals == null || visuals.isEmpty()) {
            return;
        }

        visuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id);
            if (!(entity instanceof MoltenRuptureVisualEntity ruptureVisual)) {
                return true;
            }
            long age = world.getTime() - visual.spawnTick;
            int lifetime = VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS + VISUAL_SINK_TICKS;
            if (age >= lifetime) {
                ruptureVisual.discard();
                return true;
            }
            ruptureVisual.setHeightScale(getVisualHeightScale(age));
            ruptureVisual.setPos(visual.x, visual.y + getVisualVerticalOffset(age), visual.z);
            return false;
        });

        if (visuals.isEmpty()) {
            ACTIVE_VISUALS.remove(world);
        }
    }

    private static float getVisualHeightScale(long age) {
        if (age < VISUAL_RISE_TICKS) {
            float t = MathHelper.clamp((float) age / VISUAL_RISE_TICKS, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS) {
            return 1.0F;
        }
        float t = MathHelper.clamp((float) (age - VISUAL_RISE_TICKS - VISUAL_HOLD_TICKS) / VISUAL_SINK_TICKS, 0.0F, 1.0F);
        return 1.0F - t * t * t;
    }

    private static double getVisualVerticalOffset(long age) {
        if (age < VISUAL_RISE_TICKS) {
            float t = MathHelper.clamp((float) age / VISUAL_RISE_TICKS, 0.0F, 1.0F);
            return -VISUAL_START_DEPTH + VISUAL_START_DEPTH * easeOutBack(t);
        }
        if (age < VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS) {
            return 0.0;
        }
        float t = MathHelper.clamp((float) (age - VISUAL_RISE_TICKS - VISUAL_HOLD_TICKS) / VISUAL_SINK_TICKS, 0.0F, 1.0F);
        return -VISUAL_START_DEPTH * t * t * t;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    private static void spawnVentStartEffects(ServerWorld world, LivingEntity owner, float heatFraction) {
        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE,
                owner.getSoundCategory(), 0.75F, 0.7F);
        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                owner.getSoundCategory(), 0.85F, 0.72F + heatFraction * 0.28F);
        world.spawnParticles(ParticleTypes.LAVA, owner.getX(), owner.getBodyY(0.45), owner.getZ(),
                12 + (int) (heatFraction * 18), 0.7, 0.55, 0.7, 0.09);
        world.spawnParticles(ParticleTypes.FLAME, owner.getX(), owner.getBodyY(0.45), owner.getZ(),
                24 + (int) (heatFraction * 24), 0.85, 0.65, 0.85, 0.1);
        world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, owner.getX(), owner.getBodyY(0.4), owner.getZ(),
                10, 0.55, 0.35, 0.55, 0.04);
    }

    private static void spawnVentingEffects(ServerWorld world, LivingEntity owner, float heatFraction) {
        world.spawnParticles(ParticleTypes.FLAME, owner.getX(), owner.getBodyY(0.42), owner.getZ(),
                2 + (int) (heatFraction * 3), 0.42, 0.48, 0.42, 0.025);
        world.spawnParticles(ParticleTypes.SMOKE, owner.getX(), owner.getBodyY(0.55), owner.getZ(),
                2, 0.35, 0.45, 0.35, 0.025);
        if (heatFraction > 0.55F) {
            world.spawnParticles(ParticleTypes.LAVA, owner.getX(), owner.getBodyY(0.35), owner.getZ(),
                    1, 0.28, 0.3, 0.28, 0.02);
        }
    }

    private static void spawnVentEndEffects(ServerWorld world, LivingEntity owner) {
        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                owner.getSoundCategory(), 0.55F, 0.85F);
        world.spawnParticles(ParticleTypes.CLOUD, owner.getX(), owner.getBodyY(0.55), owner.getZ(),
                18, 0.6, 0.45, 0.6, 0.045);
        world.spawnParticles(ParticleTypes.ASH, owner.getX(), owner.getBodyY(0.45), owner.getZ(),
                14, 0.65, 0.4, 0.65, 0.025);
    }

    private static void spawnShockwaveRing(ServerWorld world, Vec3d origin, double radius, int step) {
        int segments = Math.max(24, (int) Math.ceil(radius * 12.0));
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, origin.y);
            world.spawnParticles(ParticleTypes.FLAME, x, y + 0.18, z, 1, 0.03, 0.06, 0.03, 0.03);
            world.spawnParticles(ParticleTypes.SMOKE, x, y + 0.25, z, 1, 0.04, 0.08, 0.04, 0.02);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.LAVA, x, y + 0.12, z, 1, 0.02, 0.04, 0.02, 0.02);
            }
            if (i % 4 == 0) {
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.getDefaultState()),
                        x, y + 0.08, z, 1, 0.04, 0.05, 0.04, 0.025);
            }
        }
        if (step % 2 == 0) {
            world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS, 0.22F, 0.82F + step * 0.035F);
        }
    }

    private static void spawnShockwaveHitEffects(ServerWorld world, LivingEntity target) {
        world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(),
                12, 0.32, 0.34, 0.32, 0.055);
        world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(0.4), target.getZ(),
                6, 0.25, 0.24, 0.25, 0.04);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                target.getSoundCategory(), 0.5F, 0.82F + world.random.nextFloat() * 0.14F);
    }

    private static boolean isAttackReady(ServerWorld world, LivingEntity user, ItemStack stack, ActiveVent vent,
                                         Phase5AbilityTuning tuning) {
        long now = world.getTime();
        if (now < vent.nextSwingTick) return false;
        int base = (int) Math.ceil(getAttackReadyCooldownTicks(user) * tuning.get(
                Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_COOLDOWN_MULTIPLIER, 1));
        vent.nextSwingTick = now + SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, user, base);
        return true;
    }

    private static int getAttackReadyCooldownTicks(LivingEntity user) {
        EntityAttributeInstance attackSpeed = user.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        double value = attackSpeed == null ? 4.0 : attackSpeed.getValue();
        if (value <= 0.0) {
            value = 4.0;
        }
        return Math.max(Config.uniqueEffects.molten_edge.minimumSwingCooldownTicks, (int) Math.ceil(20.0 / value));
    }

    private static int getEffectiveHeat(MoltenHeatComponent heat, long worldTime) {
        return getEffectiveHeat(heat, worldTime, Math.max(1, Config.uniqueEffects.molten_edge.ventDrainPerTick));
    }

    private static int getEffectiveHeat(MoltenHeatComponent heat, long worldTime, int drain) {
        return heat.heatAt(worldTime, Math.max(1, drain));
    }

    private static int getVentDrainPerTick(LivingEntity owner, ItemStack stack) {
        if (owner != null && owner.getWorld() instanceof ServerWorld world) {
            ActiveVent vent = activeVent(world, owner.getUuid());
            if (vent != null && vent.stackReference == stack) return ventDrain(vent);
        }
        return Math.max(1, Config.uniqueEffects.molten_edge.ventDrainPerTick);
    }

    private static int ventDrain(ActiveVent vent) {
        return vent.tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_VENT_DRAIN,
                Config.uniqueEffects.molten_edge.ventDrainPerTick);
    }

    private static ActiveVent activeVent(ServerWorld world, UUID ownerId) {
        Map<UUID, ActiveVent> vents = ACTIVE_VENTS.get(world);
        return vents == null ? null : vents.get(ownerId);
    }

    private static void refundRuptureCooldown(ServerWorld world, LivingEntity owner, Phase5AbilityTuning tuning) {
        ActiveVent vent = activeVent(world, owner.getUuid());
        if (vent == null || !vent.tuning.flag(1 << 13)) return;
        int refund = vent.tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_REFUND_TICKS, 1);
        int minimum = vent.tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_MIN_COOLDOWN_TICKS, 3);
        vent.nextSwingTick = refundedDeadline(world.getTime(), vent.nextSwingTick, refund, minimum);
    }

    static long refundedDeadline(long now, long deadline, int refund, int minimum) {
        if (deadline <= now || refund <= 0) return deadline;
        return Math.max(now + Math.max(1, minimum), deadline - refund);
    }

    static boolean crossedHeatThreshold(int previousHeat, int currentHeat, int step) {
        int boundedStep = Math.max(1, step);
        return currentHeat > previousHeat && currentHeat / boundedStep > previousHeat / boundedStep;
    }

    static int decayedUnwieldedHeat(int heat, long worldTime, int interval) {
        int bounded = Math.max(1, interval);
        return Math.max(0, heat - (worldTime % bounded == 0 ? 1 : 0));
    }

    static float shockwaveHeatFraction(int heat, int maximum) {
        return MathHelper.clamp(heat / (float) Math.max(1, maximum), 0, 1);
    }

    static boolean shouldFork(int swingCount, int interval, boolean fissure) {
        return !fissure && swingCount > 0 && swingCount % Math.max(1, interval) == 0;
    }

    static double laneOffset(int lane, int laneCount, double angle) {
        return (lane - (Math.max(1, laneCount) - 1) / 2.0) * Math.max(0, angle);
    }

    private static void recordVentHit(ServerWorld world, LivingEntity owner, LivingEntity target, float damage) {
        ActiveVent vent = activeVent(world, owner.getUuid());
        if (vent == null) return;
        vent.hitTargets.add(target.getUuid());
        UniqueAbilityApi.emit(vent.snapshot.execution(), UniqueAbilityPhase.HIT,
                Phase5UniqueAbilities.HIT, target, 1, damage);
    }

    private static void cleanupVent(LivingEntity owner, ActiveVent vent, boolean normal, int retainedHeat) {
        removeVentMovement(owner);
        restoreResistance(owner, vent);
        int resultHeat = Math.max(0, retainedHeat);
        if (normal && vent.tuning.flag(1 << 15)
                && vent.hitTargets.size() >= vent.tuning.integer(
                Phase5AbilityTuning.Setting.MOLTEN_RECLAIM_TARGET_COUNT, 3)) {
            resultHeat = Math.max(resultHeat, vent.tuning.integer(
                    Phase5AbilityTuning.Setting.MOLTEN_RECLAIM_HEAT, 15));
        }
        int maximum = vent.heatTuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX,
                MoltenHeatComponent.MAX_HEAT);
        vent.stackReference.set(ComponentTypeRegistry.MOLTEN_HEAT.get(),
                new MoltenHeatComponent(Math.min(maximum, resultHeat), false));
        if (normal) Phase5MoltenManager.finish(vent.ownerId, vent.hitTargets.size());
        else Phase5MoltenManager.cancel(vent.ownerId);
    }

    private static void applyVentMovement(LivingEntity owner, Phase5AbilityTuning tuning) {
        EntityAttributeInstance movement = owner.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement == null) return;
        movement.removeModifier(VENT_SPEED_ID);
        double multiplier = tuning.flag(1 << 17) ? tuning.get(
                Phase5AbilityTuning.Setting.MOLTEN_ROOT_SPEED_MULTIPLIER, 0)
                : tuning.get(Phase5AbilityTuning.Setting.MOLTEN_VENT_SPEED_MULTIPLIER, 1);
        double adjustment = MathHelper.clamp(multiplier - 1, -.99, 15);
        if (adjustment != 0) movement.addTemporaryModifier(new EntityAttributeModifier(VENT_SPEED_ID,
                adjustment, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeVentMovement(LivingEntity owner) {
        EntityAttributeInstance movement = owner.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(VENT_SPEED_ID);
    }

    private static void restoreResistance(LivingEntity owner, ActiveVent vent) {
        if (!vent.tuning.flag(1 << 17)) return;
        StatusEffectInstance current = owner.getStatusEffect(StatusEffects.RESISTANCE);
        int managedAmplifier = vent.tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_RESISTANCE_AMPLIFIER, 1);
        if (current != null && current.getAmplifier() == managedAmplifier && current.getDuration() <= 5) {
            owner.removeStatusEffect(StatusEffects.RESISTANCE);
            if (vent.previousResistance != null) {
                int elapsed = (int) Math.max(0, owner.getWorld().getTime() - vent.startedAt);
                int remaining = Math.max(1, vent.previousResistance.getDuration() - elapsed);
                owner.addStatusEffect(new StatusEffectInstance(vent.previousResistance.getEffectType(), remaining,
                        vent.previousResistance.getAmplifier(), vent.previousResistance.isAmbient(),
                        vent.previousResistance.shouldShowParticles(), vent.previousResistance.shouldShowIcon()), owner);
            }
        }
    }

    private static Vec3d seekDirection(ServerWorld world, LivingEntity owner, Vec3d requested,
                                       Phase5AbilityTuning ventTuning) {
        if (!ventTuning.flag(1 << 14)) return requested.normalize();
        double range = ventTuning.get(Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_SEEK_RANGE, 5);
        LivingEntity target = world.getEntitiesByClass(LivingEntity.class,
                        owner.getBoundingBox().expand(range), candidate -> candidate.isAlive()
                                && candidate != owner && HelperMethods.checkAbilityTarget(candidate, owner))
                .stream().min(Comparator.comparingDouble(owner::squaredDistanceTo)).orElse(null);
        if (target == null) return requested.normalize();
        Vec3d desired = target.getPos().subtract(owner.getPos()).multiply(1, 0, 1);
        if (desired.lengthSquared() < .0001) return requested.normalize();
        return turnToward(requested, desired, ventTuning.get(
                Phase5AbilityTuning.Setting.MOLTEN_RUPTURE_TURN_DEGREES, 20));
    }

    static Vec3d turnToward(Vec3d current, Vec3d desired, double maximumDegrees) {
        Vec3d from = current.multiply(1, 0, 1).normalize();
        Vec3d to = desired.multiply(1, 0, 1).normalize();
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(from.dotProduct(to), -1, 1)));
        if (angle <= maximumDegrees) return to;
        double sign = Math.signum(from.x * to.z - from.z * to.x);
        return rotateHorizontal(from, sign * Math.max(0, maximumDegrees));
    }

    private static Vec3d rotateHorizontal(Vec3d direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Vec3d(direction.x * cosine - direction.z * sine, 0,
                direction.x * sine + direction.z * cosine).normalize();
    }

    private static float sequenceMultiplier(ServerWorld world, LivingEntity owner, LivingEntity target,
                                            Phase5AbilityTuning tuning) {
        if (!tuning.flag(1 << 24)) return 1;
        SequenceState state = SEQUENCES.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(owner.getUuid(), ignored -> new HashMap<>()).get(target.getUuid());
        if (state == null || state.expiresAt <= world.getTime()) return 1;
        return sequenceDamageMultiplier(state.stacks,
                tuning.get(Phase5AbilityTuning.Setting.MOLTEN_SEQUENCE_PER_STACK_MULTIPLIER, .1));
    }

    static float sequenceDamageMultiplier(int stacks, double perStack) {
        return 1 + Math.max(0, stacks) * (float) Math.max(0, perStack);
    }

    private static void recordSequenceHit(ServerWorld world, LivingEntity owner, LivingEntity target,
                                          Phase5AbilityTuning tuning) {
        if (!tuning.flag(1 << 24)) return;
        Map<UUID, SequenceState> states = SEQUENCES.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(owner.getUuid(), ignored -> new HashMap<>());
        SequenceState state = states.computeIfAbsent(target.getUuid(), ignored -> new SequenceState());
        if (state.expiresAt <= world.getTime()) state.stacks = 0;
        state.stacks = Math.min(tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_SEQUENCE_STACK_CAP, 3),
                state.stacks + 1);
        state.expiresAt = world.getTime() + tuning.integer(
                Phase5AbilityTuning.Setting.MOLTEN_SEQUENCE_WINDOW_TICKS, 40);
    }

    private static void pruneSequences(ServerWorld world) {
        Map<UUID, Map<UUID, SequenceState>> owners = SEQUENCES.get(world);
        if (owners == null) return;
        long now = world.getTime();
        owners.values().forEach(states -> states.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now));
        owners.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (owners.isEmpty()) SEQUENCES.remove(world);
    }

    private static boolean isWieldingMoltenEdge(LivingEntity entity) {
        return entity != null
                && entity.isAlive()
                && (!getHeldMoltenEdges(entity).isEmpty());
    }

    private static List<ItemStack> getHeldMoltenEdges(LivingEntity entity) {
        if (entity == null) {
            return List.of();
        }

        ItemStack mainHand = entity.getMainHandStack();
        ItemStack offHand = entity.getOffHandStack();
        boolean mainIsMoltenEdge = mainHand.isOf(ItemsRegistry.MOLTEN_EDGE.get());
        boolean offIsMoltenEdge = offHand.isOf(ItemsRegistry.MOLTEN_EDGE.get());
        if (mainIsMoltenEdge && offIsMoltenEdge && mainHand != offHand) {
            return List.of(mainHand, offHand);
        }
        if (mainIsMoltenEdge) {
            return List.of(mainHand);
        }
        if (offIsMoltenEdge) {
            return List.of(offHand);
        }
        return List.of();
    }

    private static void igniteAtMaximumHeat(LivingEntity wielder, MoltenHeatComponent heat,
                                            Phase5AbilityTuning tuning) {
        int maximum = tuning.integer(Phase5AbilityTuning.Setting.MOLTEN_HEAT_MAX,
                MoltenHeatComponent.MAX_HEAT);
        if (heat.heat() >= maximum && !heat.venting() && !tuning.flag(1 << 8)) {
            wielder.setOnFireFor(2);
        }
    }

    private static Vec3d horizontalDirection(LivingEntity entity) {
        Vec3d direction = entity.getRotationVec(1.0F);
        direction = new Vec3d(direction.x, 0.0, direction.z);
        if (direction.lengthSquared() < 0.0001) {
            direction = Vec3d.fromPolar(0.0F, entity.getYaw());
        }
        return direction.normalize();
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int startY = MathHelper.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), MathHelper.floor(centerY) - GROUND_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }
        return centerY;
    }

    private static boolean hasEntries(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    private static boolean hasEntries(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static final class ActiveVent {
        private final UUID ownerId;
        private final ItemStack stackReference;
        private final Phase5MoltenManager.Snapshot snapshot;
        private final Phase5AbilityTuning tuning;
        private final Phase5AbilityTuning heatTuning;
        private final long startedAt;
        private final StatusEffectInstance previousResistance;
        private final Set<UUID> hitTargets = new HashSet<>();
        private long nextSwingTick;
        private int swingCount;

        private ActiveVent(UUID ownerId, ItemStack stackReference, Phase5MoltenManager.Snapshot snapshot,
                           Phase5AbilityTuning tuning, Phase5AbilityTuning heatTuning, long startedAt,
                           StatusEffectInstance previousResistance) {
            this.ownerId = ownerId;
            this.stackReference = stackReference;
            this.snapshot = snapshot;
            this.tuning = tuning;
            this.heatTuning = heatTuning;
            this.startedAt = startedAt;
            this.previousResistance = previousResistance;
        }
    }

    private static final class ActiveShockwave {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Vec3d origin;
        private final float heatFraction;
        private final float damage;
        private final Phase5AbilityTuning tuning;
        private int step;
        private final Set<UUID> hitTargets;

        private ActiveShockwave(UUID ownerId, ItemStack stack, Vec3d origin, float heatFraction, float damage,
                                Phase5AbilityTuning tuning, int step, Set<UUID> hitTargets) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.origin = origin;
            this.heatFraction = heatFraction;
            this.damage = damage;
            this.tuning = tuning;
            this.step = step;
            this.hitTargets = hitTargets;
        }
    }

    private static final class ActiveRupture {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Vec3d origin;
        private final Vec3d forward;
        private final Vec3d right;
        private final float damage;
        private final int maxSteps;
        private int step;
        private final RuptureCast cast;
        private boolean refunded;
        private boolean finished;

        private ActiveRupture(UUID ownerId, ItemStack stack, Vec3d origin, Vec3d forward, Vec3d right,
                              float damage, int maxSteps, int step, RuptureCast cast) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.origin = origin;
            this.forward = forward;
            this.right = right;
            this.damage = damage;
            this.maxSteps = maxSteps;
            this.step = step;
            this.cast = cast;
        }

        private void finish(boolean cancel) {
            if (finished) return;
            finished = true;
            if (cancel) cast.cancel();
            else cast.laneFinished();
        }
    }

    private static final class RuptureCast {
        private final Phase5MoltenManager.Snapshot snapshot;
        private final Set<UUID> hitTargets;
        private int lanesRemaining;
        private boolean terminal;

        private RuptureCast(Phase5MoltenManager.Snapshot snapshot, int lanesRemaining, Set<UUID> hitTargets) {
            this.snapshot = snapshot;
            this.lanesRemaining = lanesRemaining;
            this.hitTargets = hitTargets;
        }

        private void laneFinished() {
            if (terminal || --lanesRemaining > 0) return;
            terminal = true;
            Phase5MoltenManager.finishRupture(snapshot, hitTargets.size());
        }

        private void cancel() {
            if (terminal) return;
            terminal = true;
            Phase5MoltenManager.cancelRupture(snapshot);
        }
    }

    private record LaneOrigin(Vec3d forward, double damageMultiplier) {
    }

    private static final class SequenceState {
        private int stacks;
        private long expiresAt;
    }

    private static final class RuptureVisual {
        private final UUID id;
        private final double x;
        private final double y;
        private final double z;
        private final long spawnTick;

        private RuptureVisual(UUID id, double x, double y, double z, long spawnTick) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnTick = spawnTick;
        }
    }
}
