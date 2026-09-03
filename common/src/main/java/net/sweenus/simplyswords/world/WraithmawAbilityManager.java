package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.WraithmawCutlassEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryTuning;
import net.sweenus.simplyswords.api.ability.AbyssalSpectralMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WraithmawAbilityManager {
    private static final double GOLDEN_ANGLE = 2.399963229728653;
    private static final int MAX_ACTIVE_CASTS_PER_WORLD = 32;
    private static final DustColorTransitionParticleEffect SPECTRAL_DUST =
            new DustColorTransitionParticleEffect(new Vector3f(0.06F, 0.008F, 0.13F),
                    new Vector3f(0.62F, 0.12F, 0.96F), 1.35F);
    private static final Map<ServerWorld, Map<UUID, Long>> SUPPRESSED_SWINGS = new HashMap<>();
    private static final Map<ServerWorld, Map<Long, ActiveCast>> ACTIVE_CASTS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> RECOVERY_LOCKOUTS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, LastCast>> LAST_CASTS = new HashMap<>();

    private WraithmawAbilityManager() {
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && context.stack().isOf(ItemsRegistry.WRAITHMAW.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && resolveCenter(context) != null;
    }

    public static boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        Vec3d center = resolveCenter(context);
        if (center == null) {
            return false;
        }
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(AbyssalSpectralMasteryAbilities.WRAITHMAW_MUSTER,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(AbyssalSpectralMasteryAbilities.COOLDOWN_TICKS, Config.uniqueEffects.wraithmaw.cooldown)
                        .set(AbyssalSpectralMasteryAbilities.TUNING, AbyssalSpectralMasteryTuning.EMPTY
                                .with(AbyssalSpectralMasteryTuning.Setting.COOLDOWN_TICKS, Config.uniqueEffects.wraithmaw.cooldown)
                                .with(AbyssalSpectralMasteryTuning.Setting.SPEAR_COUNT, Config.uniqueEffects.wraithmaw.cutlassCount)
                                .with(AbyssalSpectralMasteryTuning.Setting.RADIUS, Config.uniqueEffects.wraithmaw.stormRadius)
                                .with(AbyssalSpectralMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1)
                                .with(AbyssalSpectralMasteryTuning.Setting.MATERIALIZE_TICKS, 12)
                                .with(AbyssalSpectralMasteryTuning.Setting.FALL_SPEED, 1.25)
                                .with(AbyssalSpectralMasteryTuning.Setting.ORBIT_CAP, Config.uniqueEffects.wraithmaw.maxRecovered)
                                .with(AbyssalSpectralMasteryTuning.Setting.RECOVERY_RADIUS, 1.5)
                                .with(AbyssalSpectralMasteryTuning.Setting.ORBIT_DURATION_TICKS, Config.uniqueEffects.wraithmaw.recoveredDuration)
                                .with(AbyssalSpectralMasteryTuning.Setting.LAUNCH_SPEED, Config.uniqueEffects.wraithmaw.launchSpeed)
                                .with(AbyssalSpectralMasteryTuning.Setting.LAUNCH_RANGE, Config.uniqueEffects.wraithmaw.launchRange)
                                .with(AbyssalSpectralMasteryTuning.Setting.PROJECTILE_LIFETIME, 80)
                                .with(AbyssalSpectralMasteryTuning.Setting.HOMING_RANGE, Config.uniqueEffects.wraithmaw.homingRange)
                                .with(AbyssalSpectralMasteryTuning.Setting.HOMING_TURN_DEGREES, Config.uniqueEffects.wraithmaw.homingTurnRate)
                                .with(AbyssalSpectralMasteryTuning.Setting.STAIN_RADIUS, Config.uniqueEffects.wraithmaw.stainRadius)
                                .with(AbyssalSpectralMasteryTuning.Setting.STAIN_DURATION_TICKS, Config.uniqueEffects.wraithmaw.stainDuration)
                                .with(AbyssalSpectralMasteryTuning.Setting.EMBEDDED_DURATION_TICKS, Config.uniqueEffects.wraithmaw.embeddedDuration)));
        AbyssalSpectralMasteryTuning tuning = AbyssalSpectralMasteryAbilities.tuning(execution);
        WraithmawTuningSnapshot castTuning = WraithmawTuningSnapshot.from(execution);
        boolean singleTarget = castTuning.hasMode(WraithmawTuningSnapshot.MODE_SINGLE_TARGET);
        Vec3d origin = actor.getPos();
        int count = Math.clamp(tuning.integer(AbyssalSpectralMasteryTuning.Setting.SPEAR_COUNT,
                Config.uniqueEffects.wraithmaw.cutlassCount), 1, 32);
        double radius = Math.max(0.5, tuning.get(AbyssalSpectralMasteryTuning.Setting.RADIUS,
                Config.uniqueEffects.wraithmaw.stormRadius));
        ItemStack snapshot = context.stack().copy();
        float damage = Math.max(1.0F, HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, actor, snapshot,
                Config.uniqueEffects.wraithmaw.cutlassDamageScaling,
                Config.uniqueEffects.wraithmaw.cutlassSpellScaling))
                * (float) tuning.get(AbyssalSpectralMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        for (int index = 0; index < count; index++) {
            double fraction = Math.sqrt((index + 0.5) / count);
            double angle = index * GOLDEN_ANGLE + actor.getRandom().nextDouble() * 0.28;
            double distance = singleTarget ? actor.getRandom().nextDouble() * 0.35
                    : index == 0 ? 0.0 : radius * fraction * (0.82 + actor.getRandom().nextDouble() * 0.18);
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = LivyatanWaveManager.findGroundTopY(world, x, z, center.y + 2.0);
            Vec3d landing = new Vec3d(x, y, z);
            WraithmawCutlassEntity cutlass = new WraithmawCutlassEntity(
                    world, actor, snapshot, origin, landing, index, damage, execution);
            world.spawnEntity(cutlass);
        }
        spawnActivation(world, actor, center, count);
        registerCast(world, new ActiveCast(execution, world.getTime() + 1200));
        if (castTuning.detonatesEmbedded()) {
            LAST_CASTS.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(actor.getUuid(), new LastCast(world.getTime(), castTuning));
        }
        if (context.activationSource() == WeaponAbilityActivationSource.MOB) {
            SUPPRESSED_SWINGS.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(actor.getUuid(), world.getTime());
        }
        return true;
    }

    public static void onSwing(ItemStack stack, ServerWorld world, LivingEntity actor) {
        if (stack == null || !stack.isOf(ItemsRegistry.WRAITHMAW.get()) || actor == null || !actor.isAlive()) {
            return;
        }
        Map<UUID, Long> suppressed = SUPPRESSED_SWINGS.get(world);
        if (suppressed != null) {
            Long suppressedAt = suppressed.remove(actor.getUuid());
            if (suppressed.isEmpty()) SUPPRESSED_SWINGS.remove(world);
            if (suppressedAt != null && suppressedAt == world.getTime()) return;
        }
        List<WraithmawCutlassEntity> available = ownedCutlasses(world, actor).stream()
                .filter(entity -> entity.getState() == WraithmawCutlassEntity.STATE_ORBITING)
                .sorted(Comparator.comparingLong(WraithmawCutlassEntity::getExpiresAtTick)
                        .thenComparingInt(WraithmawCutlassEntity::getOrbitSlot))
                .toList();
        if (available.isEmpty()) {
            return;
        }
        Vec3d direction = actor.getRotationVec(1.0F);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        int launchCount = available.getFirst().getTuning().launchCount();
        for (int index = 0; index < Math.min(launchCount, available.size()); index++) {
            WraithmawCutlassEntity cutlass = available.get(index);
            Vec3d start = orbitPosition(actor, cutlass.getOrbitSlot(), world.getTime());
            cutlass.launch(start, direction, cutlass.getTuning().launchSpeed());
            world.spawnParticles(SPECTRAL_DUST, start.x, start.y, start.z,
                    10, 0.16, 0.16, 0.16, 0.03);
        }
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_WHOOSH_05.get(),
                SoundCategory.PLAYERS, 0.48F, 1.4F + actor.getRandom().nextFloat() * 0.12F);
    }

    public static boolean tryRecover(ServerWorld world, LivingEntity owner,
                                     WraithmawCutlassEntity candidate) {
        if (candidate == null || candidate.getState() != WraithmawCutlassEntity.STATE_EMBEDDED
                || candidate.getOwnerUuid() == null || !candidate.getOwnerUuid().equals(owner.getUuid())) {
            return false;
        }
        WraithmawTuningSnapshot tuning = candidate.getTuning();
        if (tuning.walksToEnemies()) {
            return false;
        }
        Map<UUID, Long> lockouts = RECOVERY_LOCKOUTS.get(world);
        if (recoveryLocked(world.getTime(), lockouts == null ? null : lockouts.get(owner.getUuid()))) {
            return false;
        }
        int maximum = tuning.orbitCap();
        boolean[] occupied = new boolean[maximum];
        int count = 0;
        for (WraithmawCutlassEntity cutlass : ownedCutlasses(world, owner)) {
            if (cutlass == candidate || cutlass.getState() != WraithmawCutlassEntity.STATE_ORBITING) {
                continue;
            }
            count++;
            int slot = cutlass.getOrbitSlot();
            if (slot >= 0 && slot < occupied.length) {
                occupied[slot] = true;
            }
        }
        if (count >= maximum) {
            return false;
        }
        candidate.multiplyDamage(storedMaliceMultiplier(
                tuning.storedBonusPerCutlass(), tuning.storedBonusCap(), count));
        int slot = nextFreeSlot(occupied);
        if (slot < 0) {
            return false;
        }
        candidate.recover(slot, world.getTime() + tuning.orbitDurationTicks());
        if (tuning.recoveryLockoutTicks() > 0) {
            RECOVERY_LOCKOUTS.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(owner.getUuid(), world.getTime() + tuning.recoveryLockoutTicks());
        }
        return true;
    }

    public static boolean tryDetonate(ServerWorld world, LivingEntity owner) {
        if (world == null || owner == null || !owner.isAlive()) {
            return false;
        }
        Map<UUID, LastCast> casts = LAST_CASTS.get(world);
        LastCast last = casts == null ? null : casts.get(owner.getUuid());
        if (last == null) {
            return false;
        }
        WraithmawTuningSnapshot tuning = last.tuning();
        if (!tuning.detonatesEmbedded()
                || !withinBurstWindow(world.getTime(), last.castTick(), tuning.burstWindowTicks())) {
            return false;
        }
        List<WraithmawCutlassEntity> embedded = ownedCutlasses(world, owner).stream()
                .filter(entity -> entity.getState() == WraithmawCutlassEntity.STATE_EMBEDDED)
                .filter(entity -> entity.squaredDistanceTo(owner)
                        <= tuning.burstRange() * tuning.burstRange())
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(owner)))
                .limit(tuning.burstTargetCap())
                .toList();
        if (embedded.isEmpty()) {
            return false;
        }
        casts.remove(owner.getUuid());
        if (casts.isEmpty()) LAST_CASTS.remove(world);
        for (WraithmawCutlassEntity cutlass : embedded) {
            cutlass.detonate(world, owner);
        }
        world.playSound(null, owner.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.68F, 0.7F);
        return true;
    }

    public static void clear(ServerWorld world) {
        SUPPRESSED_SWINGS.remove(world);
        RECOVERY_LOCKOUTS.remove(world);
        LAST_CASTS.remove(world);
        Map<Long, ActiveCast> casts = ACTIVE_CASTS.remove(world);
        if (casts != null) {
            casts.values().forEach(cast -> UniqueAbilityApi.cancel(cast.execution()));
        }
    }

    public static void clearAll() {
        SUPPRESSED_SWINGS.clear();
        RECOVERY_LOCKOUTS.clear();
        LAST_CASTS.clear();
        ACTIVE_CASTS.values().forEach(casts ->
                casts.values().forEach(cast -> UniqueAbilityApi.cancel(cast.execution())));
        ACTIVE_CASTS.clear();
    }

    public static Vec3d orbitPosition(LivingEntity owner, int slot, long time) {
        double[][] slots = {
                {-0.62, 1.55, 0.12},
                {0.62, 1.55, 0.12},
                {-0.92, 2.02, 0.10},
                {-0.31, 2.24, 0.14},
                {0.31, 2.24, 0.14},
                {0.92, 2.02, 0.10},
                {-0.52, 2.62, 0.18},
                {0.52, 2.62, 0.18},
                {-1.08, 1.72, -0.16},
                {1.08, 1.72, -0.16},
                {-0.78, 2.92, -0.12},
                {0.78, 2.92, -0.12}
        };
        int index = Math.floorMod(slot, slots.length);
        double localX = slots[index][0];
        double localY = slots[index][1] + Math.sin(time * 0.11 + index * 1.7) * 0.08;
        double localZ = slots[index][2];
        double yaw = Math.toRadians(owner.getYaw());
        Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        return owner.getPos().add(right.multiply(localX)).add(forward.multiply(localZ)).add(0.0, localY, 0.0);
    }

    public static boolean hasActive(ServerWorld world) {
        return !SUPPRESSED_SWINGS.getOrDefault(world, Map.of()).isEmpty()
                || !ACTIVE_CASTS.getOrDefault(world, Map.of()).isEmpty()
                || !RECOVERY_LOCKOUTS.getOrDefault(world, Map.of()).isEmpty()
                || !LAST_CASTS.getOrDefault(world, Map.of()).isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, Long> suppressed = SUPPRESSED_SWINGS.get(world);
        long now = world.getTime();
        if (suppressed != null) {
            suppressed.values().removeIf(tick -> tick < now);
            if (suppressed.isEmpty()) SUPPRESSED_SWINGS.remove(world);
        }
        Map<Long, ActiveCast> casts = ACTIVE_CASTS.get(world);
        if (casts != null) {
            casts.values().removeIf(cast -> {
                if (cast.expiresAt > now) return false;
                UniqueAbilityApi.finish(cast.execution, cast.execution.definition().id(), 0);
                return true;
            });
            if (casts.isEmpty()) ACTIVE_CASTS.remove(world);
        }
        Map<UUID, Long> lockouts = RECOVERY_LOCKOUTS.get(world);
        if (lockouts != null) {
            lockouts.values().removeIf(tick -> tick <= now);
            if (lockouts.isEmpty()) RECOVERY_LOCKOUTS.remove(world);
        }
        Map<UUID, LastCast> lastCasts = LAST_CASTS.get(world);
        if (lastCasts != null) {
            lastCasts.values().removeIf(cast -> now - cast.castTick() > cast.tuning().burstWindowTicks());
            if (lastCasts.isEmpty()) LAST_CASTS.remove(world);
        }
    }

    static double storedMaliceMultiplier(double bonusPerCutlass, double cap, int orbiting) {
        if (bonusPerCutlass <= 0 || cap <= 0 || orbiting <= 0) {
            return 1;
        }
        return 1 + Math.min(cap, orbiting * bonusPerCutlass);
    }

    static int nextFreeSlot(boolean[] occupied) {
        for (int slot = 0; slot < occupied.length; slot++) {
            if (!occupied[slot]) {
                return slot;
            }
        }
        return -1;
    }

    static boolean recoveryLocked(long now, Long lockedUntil) {
        return lockedUntil != null && now < lockedUntil;
    }

    static boolean withinBurstWindow(long now, long castTick, int windowTicks) {
        return windowTicks > 0 && now >= castTick && now - castTick <= windowTicks;
    }

    private static void registerCast(ServerWorld world, ActiveCast cast) {
        Map<Long, ActiveCast> casts = ACTIVE_CASTS.computeIfAbsent(world, ignored -> new HashMap<>());
        if (casts.size() >= MAX_ACTIVE_CASTS_PER_WORLD) {
            ActiveCast oldest = casts.values().stream()
                    .min(Comparator.comparingLong(ActiveCast::expiresAt)
                            .thenComparingLong(value -> value.execution().id()))
                    .orElse(null);
            if (oldest != null) {
                casts.remove(oldest.execution().id());
                UniqueAbilityApi.cancel(oldest.execution());
            }
        }
        casts.put(cast.execution().id(), cast);
    }

    private static List<WraithmawCutlassEntity> ownedCutlasses(ServerWorld world, LivingEntity owner) {
        Box search = owner.getBoundingBox().expand(32.0, 16.0, 32.0);
        return world.getEntitiesByClass(WraithmawCutlassEntity.class, search,
                entity -> owner.getUuid().equals(entity.getOwnerUuid()) && entity.isAlive());
    }

    private static Vec3d resolveCenter(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        double range = Math.max(1.0, Config.uniqueEffects.wraithmaw.castRange);
        if (context.target() != null && context.target().isAlive()
                && context.target().squaredDistanceTo(actor) <= range * range
                && HelperMethods.checkAbilityTarget(context.target(), actor)) {
            LivingEntity target = context.target();
            return new Vec3d(target.getX(),
                    LivyatanWaveManager.findGroundTopY(context.world(), target.getX(), target.getZ(), target.getY() + 2.0),
                    target.getZ());
        }
        Vec3d direction = context.facing().lengthSquared() < 1.0E-6
                ? actor.getRotationVec(1.0F) : context.facing().normalize();
        Vec3d start = actor.getEyePos();
        Vec3d end = start.add(direction.multiply(range));
        BlockHitResult hit = context.world().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, actor));
        Vec3d point = hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
        return new Vec3d(point.x,
                LivyatanWaveManager.findGroundTopY(context.world(), point.x, point.z, point.y + 2.0),
                point.z);
    }

    private static void spawnActivation(ServerWorld world, LivingEntity actor, Vec3d center, int count) {
        Vec3d chest = actor.getPos().add(0.0, actor.getHeight() * 0.62, 0.0);
        world.spawnParticles(SPECTRAL_DUST, chest.x, chest.y, chest.z,
                Math.min(42, 18 + count), 0.7, 0.8, 0.7, 0.04);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, chest.x, chest.y, chest.z,
                8, 0.5, 0.6, 0.5, 0.025);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.15, center.z,
                28, 1.4, 0.12, 1.4, 0.08);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.72F, 0.82F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.82F, 1.02F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.PLAYERS, 0.58F, 0.72F);
    }

    private record ActiveCast(UniqueAbilityExecution execution, long expiresAt) {
    }

    private record LastCast(long castTick, WraithmawTuningSnapshot tuning) {
    }
}
