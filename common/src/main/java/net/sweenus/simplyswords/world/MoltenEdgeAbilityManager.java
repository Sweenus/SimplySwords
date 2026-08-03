package net.sweenus.simplyswords.world;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.AwakeningApi;
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

public final class MoltenEdgeAbilityManager {

    private static final int VISUAL_RISE_TICKS = 2;
    private static final int VISUAL_HOLD_TICKS = 2;
    private static final int VISUAL_SINK_TICKS = 5;
    private static final double VISUAL_START_DEPTH = 1.35;
    private static final int GROUND_SCAN_UP = 5;
    private static final int GROUND_SCAN_DOWN = 14;
    private static final String RUPTURE_VISUAL_TAG = "simplyswords_molten_rupture_visual";

    private static final Map<ServerWorld, Map<UUID, ActiveVent>> ACTIVE_VENTS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveShockwave>> ACTIVE_SHOCKWAVES = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveRupture>> ACTIVE_RUPTURES = new HashMap<>();
    private static final Map<ServerWorld, List<RuptureVisual>> ACTIVE_VISUALS = new HashMap<>();
    private static final Map<UUID, Long> LAST_RUPTURE_SWING = new HashMap<>();

    private MoltenEdgeAbilityManager() {
    }

    public static MoltenHeatComponent getHeatComponent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
            return MoltenHeatComponent.DEFAULT;
        }
        return ComponentTypeRegistry.MOLTEN_HEAT.getOrDefault(stack, MoltenHeatComponent.DEFAULT);
    }

    public static int getHeat(LivingEntity wielder) {
        if (!isWieldingMoltenEdge(wielder)) {
            return 0;
        }
        long now = wielder.getWorld().getTime();
        int heat = 0;
        for (ItemStack stack : getHeldMoltenEdges(wielder)) {
            heat = Math.max(heat, getEffectiveHeat(getHeatComponent(stack), now));
        }
        return heat;
    }

    public static int getHeat(LivingEntity wielder, ItemStack stack) {
        if (!isHeldMoltenEdge(wielder, stack)) {
            return 0;
        }
        return getEffectiveHeat(getHeatComponent(stack), wielder.getWorld().getTime());
    }

    public static boolean isVenting(LivingEntity wielder) {
        if (!isWieldingMoltenEdge(wielder)) {
            return false;
        }
        long now = wielder.getWorld().getTime();
        for (ItemStack stack : getHeldMoltenEdges(wielder)) {
            if (getHeatComponent(stack).isVentingAt(now, getVentDrainPerTick())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVenting(LivingEntity wielder, ItemStack stack) {
        if (!isHeldMoltenEdge(wielder, stack)) {
            return false;
        }
        return getHeatComponent(stack).isVentingAt(wielder.getWorld().getTime(), getVentDrainPerTick());
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
        if (stack == null
                || attacker == null
                || attacker.getWorld().isClient()
                || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())
                || !isHeldMoltenEdge(attacker, stack)) {
            return;
        }

        MoltenHeatComponent heat = getHeatComponent(stack);
        long now = attacker.getWorld().getTime();
        if (heat.isVentingAt(now, getVentDrainPerTick())) {
            return;
        }
        MoltenHeatComponent increased = heat.normalizedAt(now, getVentDrainPerTick())
                .addHeat(Config.uniqueEffects.molten_edge.heatPerHit);
        ComponentTypeRegistry.MOLTEN_HEAT.set(stack, increased);
        igniteAtMaximumHeat(attacker, increased);
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
            if (heat.isVentingAt(now, getVentDrainPerTick())) {
                continue;
            }
            MoltenHeatComponent increased = heat.normalizedAt(now, getVentDrainPerTick())
                    .addHeat(Config.uniqueEffects.molten_edge.heatPerDamageTaken);
            ComponentTypeRegistry.MOLTEN_HEAT.set(stack, increased);
            igniteAtMaximumHeat(target, increased);
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
        return amount * (1.0F + heat / 200.0F);
    }

    public static float modifyIncomingDamage(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F) {
            return amount;
        }
        int heat = getHeat(target);
        if (heat <= 0) {
            return amount;
        }
        return amount * (1.0F + heat / 100.0F);
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
        int currentHeat = getEffectiveHeat(heat, now);
        if (currentHeat <= 0 || heat.isVentingAt(now, getVentDrainPerTick())) {
            return false;
        }

        Map<UUID, ActiveVent> vents = ACTIVE_VENTS.computeIfAbsent(world, ignored -> new HashMap<>());
        if (vents.containsKey(actor.getUuid())) {
            return false;
        }

        MoltenHeatComponent ventingHeat = new MoltenHeatComponent(currentHeat, false).startVenting(now);
        ComponentTypeRegistry.MOLTEN_HEAT.set(heldStack, ventingHeat);
        vents.put(actor.getUuid(), new ActiveVent(actor.getUuid(), heldStack));

        float heatFraction = currentHeat / (float) MoltenHeatComponent.MAX_HEAT;
        float shockwaveDamage = HelperMethods.abilityScaledDamage(
                "fire",
                actor,
                heldStack,
                Config.uniqueEffects.molten_edge.shockwaveDamageScaling,
                Config.uniqueEffects.molten_edge.shockwaveSpellScaling
        ) * heatFraction;
        ACTIVE_SHOCKWAVES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveShockwave(
                actor.getUuid(),
                heldStack.copy(),
                actor.getPos(),
                heatFraction,
                shockwaveDamage,
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

        MoltenHeatComponent heat = getHeatComponent(stack);
        long now = world.getTime();
        if (!heat.isVentingAt(now, getVentDrainPerTick())
                || getEffectiveHeat(heat, now) <= 0
                || !isAttackReady(world, wielder)) {
            return;
        }

        Vec3d forward = horizontalDirection(wielder);
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        Vec3d origin = wielder.getPos().add(forward.multiply(0.65));
        float damage = HelperMethods.abilityScaledDamage(
                "fire",
                wielder,
                stack,
                Config.uniqueEffects.molten_edge.ruptureDamageScaling,
                Config.uniqueEffects.molten_edge.ruptureSpellScaling
        );
        int steps = Math.max(1, (int) Math.ceil(
                Math.max(1.0, Config.uniqueEffects.molten_edge.ruptureLength)
                        / Math.max(0.25, Config.uniqueEffects.molten_edge.ruptureStepDistance)
        ));
        ACTIVE_RUPTURES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveRupture(
                wielder.getUuid(),
                stack.copy(),
                origin,
                forward,
                right,
                damage,
                steps,
                0,
                new HashSet<>()
        ));

        world.playSound(null, wielder.getX(), wielder.getY(), wielder.getZ(), SoundRegistry.SWING_WOOSH.get(),
                SoundCategory.PLAYERS, 0.7F, 0.72F + world.random.nextFloat() * 0.12F);
        world.playSound(null, wielder.getX(), wielder.getY(), wielder.getZ(), SoundEvents.BLOCK_FIRE_AMBIENT,
                SoundCategory.PLAYERS, 0.55F, 0.62F + world.random.nextFloat() * 0.1F);
    }

    public static void tickHeldStack(ItemStack stack, World world, Entity entity) {
        if (world.isClient() || !(entity instanceof LivingEntity living) || !stack.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
            return;
        }

        MoltenHeatComponent heat = getHeatComponent(stack);
        if (!AwakeningApi.isAbilityUnlocked(stack)) {
            if (heat.heat() > 0 || heat.venting()) {
                ComponentTypeRegistry.MOLTEN_HEAT.set(stack, MoltenHeatComponent.DEFAULT);
                cancelVent(living, stack);
            }
            return;
        }
        if (!isHeldMoltenEdge(living, stack)) {
            if (heat.heat() > 0 || heat.venting()) {
                ComponentTypeRegistry.MOLTEN_HEAT.set(stack, MoltenHeatComponent.DEFAULT);
                cancelVent(living, stack);
            }
            return;
        }

        if (!living.isAlive()) {
            ComponentTypeRegistry.MOLTEN_HEAT.set(stack, MoltenHeatComponent.DEFAULT);
            cancelVent(living, stack);
            return;
        }

        if (heat.heat() >= MoltenHeatComponent.MAX_HEAT && !heat.venting() && living.age % 10 == 0) {
            living.setOnFireFor(2);
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
        }
        if (vent != null) {
            ComponentTypeRegistry.MOLTEN_HEAT.set(vent.stackReference, MoltenHeatComponent.DEFAULT);
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
            ComponentTypeRegistry.MOLTEN_HEAT.set(stack, MoltenHeatComponent.DEFAULT);
        }
        cancelVent(wielder);
    }

    public static boolean hasActive(ServerWorld world) {
        return hasEntries(ACTIVE_VENTS.get(world))
                || hasEntries(ACTIVE_SHOCKWAVES.get(world))
                || hasEntries(ACTIVE_RUPTURES.get(world))
                || hasEntries(ACTIVE_VISUALS.get(world));
    }

    public static void tick(ServerWorld world) {
        tickVents(world);
        tickShockwaves(world);
        tickRuptures(world);
        tickVisuals(world);
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
            if (!(entity instanceof LivingEntity owner)
                    || !owner.isAlive()
                    || !isHeldMoltenEdge(owner, vent.stackReference)
                    || !vent.stackReference.isOf(ItemsRegistry.MOLTEN_EDGE.get())) {
                ComponentTypeRegistry.MOLTEN_HEAT.set(vent.stackReference, MoltenHeatComponent.DEFAULT);
                iterator.remove();
                continue;
            }

            MoltenHeatComponent heat = getHeatComponent(vent.stackReference);
            int effectiveHeat = getEffectiveHeat(heat, world.getTime());
            if (!heat.isVentingAt(world.getTime(), getVentDrainPerTick()) || effectiveHeat <= 0) {
                spawnVentEndEffects(world, owner);
                iterator.remove();
                continue;
            }

            if (world.getTime() % 3L == 0L) {
                float fraction = effectiveHeat / (float) MoltenHeatComponent.MAX_HEAT;
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

            double maxRadius = Math.max(1.0, Config.uniqueEffects.molten_edge.radius);
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

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target == owner
                    || shockwave.hitTargets.contains(target.getUuid())
                    || !HelperMethods.checkAbilityTarget(target, owner)) {
                continue;
            }
            double horizontalDistance = horizontalDistance(target.getPos(), shockwave.origin);
            if (horizontalDistance < inner || horizontalDistance > outer) {
                continue;
            }

            shockwave.hitTargets.add(target.getUuid());
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, shockwave.stack, target, source, shockwave.damage);
            if (!HelperMethods.damageThroughIframes(target, source, damage)) {
                continue;
            }

            target.setOnFireFor(Math.max(0, Config.uniqueEffects.molten_edge.shockwaveIgniteSeconds));
            Vec3d push = target.getPos().subtract(shockwave.origin);
            push = new Vec3d(push.x, 0.0, push.z);
            if (push.lengthSquared() < 0.0001) {
                push = horizontalDirection(owner);
            } else {
                push = push.normalize();
            }
            double strength = Math.max(0.0, Config.uniqueEffects.molten_edge.shockwaveKnockback)
                    * (0.5 + shockwave.heatFraction * 0.5);
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
                return true;
            }

            rupture.step++;
            double stepDistance = Math.max(0.25, Config.uniqueEffects.molten_edge.ruptureStepDistance);
            Vec3d center = rupture.origin.add(rupture.forward.multiply(rupture.step * stepDistance));
            double groundY = findGroundTopY(world, center.x, center.z, center.y);
            Vec3d groundedCenter = new Vec3d(center.x, groundY, center.z);
            spawnRuptureStepEffects(world, rupture, groundedCenter);
            damageRuptureTargets(world, owner, rupture, groundedCenter, stepDistance);
            return rupture.step >= rupture.maxSteps;
        });

        if (ruptures.isEmpty()) {
            ACTIVE_RUPTURES.remove(world);
        }
    }

    private static void damageRuptureTargets(ServerWorld world, LivingEntity owner, ActiveRupture rupture, Vec3d center, double segmentLength) {
        double width = Math.max(0.5, Config.uniqueEffects.molten_edge.ruptureWidth);
        double xSize = Math.abs(rupture.forward.x) * segmentLength * 2.0 + Math.abs(rupture.right.x) * width + 2.0;
        double zSize = Math.abs(rupture.forward.z) * segmentLength * 2.0 + Math.abs(rupture.right.z) * width + 2.0;
        Box box = Box.of(center.add(0.0, 0.75, 0.0), xSize, 3.0, zSize);
        DamageSource source = world.getDamageSources().indirectMagic(owner, owner);

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target == owner
                    || rupture.hitTargets.contains(target.getUuid())
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

            rupture.hitTargets.add(target.getUuid());
            float damage = HelperMethods.applyAbilityDamageEnchantments(world, rupture.stack, target, source, rupture.damage);
            if (!HelperMethods.damageThroughIframes(target, source, damage)) {
                continue;
            }
            target.setOnFireFor(Math.max(0, Config.uniqueEffects.molten_edge.ruptureIgniteSeconds));
            target.addVelocity(rupture.forward.x * 0.18, Math.max(0.0, Config.uniqueEffects.molten_edge.ruptureKnockUp), rupture.forward.z * 0.18);
            target.velocityModified = true;
            target.velocityDirty = true;
            world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(0.45), target.getZ(), 5, 0.25, 0.25, 0.25, 0.04);
            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.45), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private static void spawnRuptureStepEffects(ServerWorld world, ActiveRupture rupture, Vec3d center) {
        double width = Math.max(0.5, Config.uniqueEffects.molten_edge.ruptureWidth);
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

    private static boolean isAttackReady(ServerWorld world, LivingEntity user) {
        long now = world.getTime();
        if (now % 200L == 0L) {
            LAST_RUPTURE_SWING.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
        }
        Long last = LAST_RUPTURE_SWING.get(user.getUuid());
        if (last != null && now - last < getAttackReadyCooldownTicks(user)) {
            return false;
        }
        LAST_RUPTURE_SWING.put(user.getUuid(), now);
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
        return heat.heatAt(worldTime, getVentDrainPerTick());
    }

    private static int getVentDrainPerTick() {
        return Math.max(1, Config.uniqueEffects.molten_edge.ventDrainPerTick);
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

    private static void igniteAtMaximumHeat(LivingEntity wielder, MoltenHeatComponent heat) {
        if (heat.heat() >= MoltenHeatComponent.MAX_HEAT && !heat.venting()) {
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

        private ActiveVent(UUID ownerId, ItemStack stackReference) {
            this.ownerId = ownerId;
            this.stackReference = stackReference;
        }
    }

    private static final class ActiveShockwave {
        private final UUID ownerId;
        private final ItemStack stack;
        private final Vec3d origin;
        private final float heatFraction;
        private final float damage;
        private int step;
        private final Set<UUID> hitTargets;

        private ActiveShockwave(UUID ownerId, ItemStack stack, Vec3d origin, float heatFraction, float damage, int step, Set<UUID> hitTargets) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.origin = origin;
            this.heatFraction = heatFraction;
            this.damage = damage;
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
        private final Set<UUID> hitTargets;

        private ActiveRupture(UUID ownerId, ItemStack stack, Vec3d origin, Vec3d forward, Vec3d right, float damage, int maxSteps, int step, Set<UUID> hitTargets) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.origin = origin;
            this.forward = forward;
            this.right = right;
            this.damage = damage;
            this.maxSteps = maxSteps;
            this.step = step;
            this.hitTargets = hitTargets;
        }
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
