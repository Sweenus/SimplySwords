package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.ShadowstingAfterimageVisualEntity;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShadowstingShadowDanceManager {

    private static final int FINISH_TRANSLATION_TICKS = 8;
    private static final int AFTERIMAGE_LIFETIME = 6;
    private static final int PASSIVE_CLONE_DELAY_TICKS = 3;
    private static final int MAX_CLONE_CHAIN_DEPTH = 32;
    private static final Map<UUID, ActiveShadowDance> ACTIVE_DANCES = new HashMap<>();
    private static final Map<ServerWorld, List<PendingShadowCloneStrike>> PENDING_CLONE_STRIKES = new HashMap<>();
    private static final ThreadLocal<Integer> CURRENT_CLONE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private ShadowstingShadowDanceManager() {
    }

    public static boolean start(ServerWorld world, ServerPlayerEntity player) {
        if (world == null || player == null || ACTIVE_DANCES.containsKey(player.getUuid())) {
            return false;
        }

        LivingEntity target = findRandomTarget(world, player);
        if (target == null) {
            spawnFailParticles(world, player);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                    SoundCategory.PLAYERS, 0.45F, 0.55F);
            return false;
        }

        ActiveShadowDance dance = new ActiveShadowDance(
                world.getRegistryKey().getValue().toString(),
                world.getTime() + getActiveDurationTicks(),
                world.getTime(),
                player.getPos()
        );
        ACTIVE_DANCES.put(player.getUuid(), dance);
        player.addStatusEffect(new StatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.SHADOW_DANCE),
                getActiveDurationTicks() + FINISH_TRANSLATION_TICKS + 2,
                Math.max(0, getActiveDurationTicks() - 1),
                false,
                false,
                false
        ), player);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.75F, 1.25F);
        spawnDepartureParticles(world, player.getPos().add(0.0, player.getHeight() * 0.5, 0.0));
        performStrike(world, player, dance, target);
        return true;
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ActiveShadowDance dance = ACTIVE_DANCES.get(player.getUuid());
        if (dance == null) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        if (!dance.worldKey.equals(world.getRegistryKey().getValue().toString()) || !player.isAlive()) {
            finishNow(player);
            return;
        }

        if (dance.finishing) {
            tickFinishTranslation(world, player, dance);
            return;
        }

        if (world.getTime() >= dance.endTick) {
            beginFinishTranslation(world, player, dance);
            return;
        }

        lockPlayer(player, dance);
        clearMobTargets(world, player);

        if (world.getTime() >= dance.nextStrikeTick) {
            LivingEntity target = findRandomTarget(world, player);
            if (target != null) {
                performStrike(world, player, dance, target);
            } else {
                dance.nextStrikeTick = world.getTime() + getStrikeIntervalTicks();
                spawnIdleParticles(world, player);
            }
        } else if (Config.general.enableModernFieldEffects && world.getTime() % 3L == 0L) {
            spawnIdleParticles(world, player);
        }
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return player != null && ACTIVE_DANCES.containsKey(player.getUuid());
    }

    public static boolean hasPendingCloneStrikes(ServerWorld world) {
        List<PendingShadowCloneStrike> strikes = PENDING_CLONE_STRIKES.get(world);
        return strikes != null && !strikes.isEmpty();
    }

    public static void tickCloneStrikes(ServerWorld world) {
        List<PendingShadowCloneStrike> strikes = PENDING_CLONE_STRIKES.get(world);
        if (strikes == null || strikes.isEmpty()) {
            return;
        }

        List<PendingShadowCloneStrike> dueStrikes = new java.util.ArrayList<>();
        Iterator<PendingShadowCloneStrike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            PendingShadowCloneStrike strike = iterator.next();
            if (world.getTime() < strike.triggerTick()) {
                continue;
            }
            iterator.remove();
            dueStrikes.add(strike);
        }
        if (strikes.isEmpty()) {
            PENDING_CLONE_STRIKES.remove(world);
        }
        for (PendingShadowCloneStrike strike : dueStrikes) {
            executePassiveCloneStrike(world, strike);
        }
    }

    public static void schedulePassiveCloneStrike(ServerWorld world, ServerPlayerEntity owner, LivingEntity target) {
        if (world == null || owner == null || target == null || !owner.isAlive() || !target.isAlive()) {
            return;
        }

        int chainDepth = CURRENT_CLONE_DEPTH.get() + 1;
        if (chainDepth > MAX_CLONE_CHAIN_DEPTH) {
            return;
        }

        PENDING_CLONE_STRIKES.computeIfAbsent(world, ignored -> new java.util.ArrayList<>())
                .add(new PendingShadowCloneStrike(owner.getUuid(), target.getUuid(), world.getTime() + PASSIVE_CLONE_DELAY_TICKS, chainDepth));
    }

    public static void end(ServerPlayerEntity player) {
        finishNow(player);
    }

    private static void finishNow(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        if (ACTIVE_DANCES.remove(player.getUuid()) != null) {
            lockPlayer(player);
        }
    }

    private static void beginFinishTranslation(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance) {
        if (dance.lastStrikePos == null) {
            finishNow(player);
            return;
        }

        dance.finishing = true;
        dance.finishStartTick = world.getTime();
        dance.finishStartPos = player.getPos();
        dance.finishEndPos = dance.lastStrikePos;
        float[] rotation = dance.lastLookTarget == null
                ? new float[]{player.getYaw(), player.getPitch()}
                : getFacingRotation(dance.finishEndPos.add(0.0, player.getEyeHeight(player.getPose()), 0.0), dance.lastLookTarget);
        dance.finishStartYaw = player.getYaw();
        dance.finishStartPitch = player.getPitch();
        dance.finishEndYaw = rotation[0];
        dance.finishEndPitch = rotation[1];

        spawnDepartureParticles(world, player.getPos().add(0.0, player.getHeight() * 0.5, 0.0));
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 0.25F, 0.65F);
        tickFinishTranslation(world, player, dance);
    }

    private static void tickFinishTranslation(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance) {
        lockPlayer(player);
        if (dance.finishStartPos == null || dance.finishEndPos == null) {
            finishNow(player);
            return;
        }

        float progress = MathHelper.clamp((float) (world.getTime() - dance.finishStartTick + 1L) / FINISH_TRANSLATION_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        Vec3d pos = dance.finishStartPos.lerp(dance.finishEndPos, eased);
        float yaw = MathHelper.lerpAngleDegrees(eased, dance.finishStartYaw, dance.finishEndYaw);
        float pitch = MathHelper.lerp(eased, dance.finishStartPitch, dance.finishEndPitch);

        player.networkHandler.requestTeleport(pos.x, pos.y, pos.z, yaw, pitch);
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
        if (Config.general.enableModernFieldEffects && world.getTime() % 2L == 0L) {
            spawnTrail(world, dance.finishStartPos.add(0.0, player.getHeight() * 0.5, 0.0), pos.add(0.0, player.getHeight() * 0.5, 0.0));
        }

        if (progress >= 1.0F) {
            spawnDepartureParticles(world, dance.finishEndPos.add(0.0, player.getHeight() * 0.5, 0.0));
            world.playSound(null, dance.finishEndPos.x, dance.finishEndPos.y, dance.finishEndPos.z,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 0.35F, 0.75F);
            finishNow(player);
        }
    }

    private static void performStrike(ServerWorld world, ServerPlayerEntity player, ActiveShadowDance dance, LivingEntity target) {
        Vec3d previousPos = dance.lastVisualPos == null ? dance.anchorPos : dance.lastVisualPos;
        Vec3d strikePos = findStrikePosition(world, player, target);
        Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);

        spawnDepartureParticles(world, previousPos.add(0.0, player.getHeight() * 0.5, 0.0));
        dance.lastStrikePos = strikePos;
        dance.lastVisualPos = strikePos;
        dance.lastLookTarget = lookTarget;
        lockPlayer(player);

        performWeaponStrike(player, target);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                SoundCategory.PLAYERS, 0.55F, 1.45F + world.random.nextFloat() * 0.25F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.45F, 0.85F + world.random.nextFloat() * 0.2F);

        spawnArrivalParticles(world, target, strikePos);
        spawnTrail(world, previousPos.add(0.0, player.getHeight() * 0.5, 0.0), lookTarget);
        spawnShadowEcho(world, strikePos, lookTarget, player.getHeight());
        dance.nextStrikeTick = world.getTime() + getStrikeIntervalTicks();
    }

    private static int getActiveDurationTicks() {
        return Math.max(1, Config.uniqueEffects.shadowsting.duration / 2);
    }

    private static int getStrikeIntervalTicks() {
        return Math.max(1, Config.uniqueEffects.shadowsting.strikeInterval / 2);
    }

    private static void executePassiveCloneStrike(ServerWorld world, PendingShadowCloneStrike strike) {
        if (!(world.getEntity(strike.ownerId()) instanceof ServerPlayerEntity owner)
                || !(world.getEntity(strike.targetId()) instanceof LivingEntity target)
                || !owner.isAlive()
                || !target.isAlive()
                || !owner.getMainHandStack().isOf(ItemsRegistry.SHADOWSTING.get())) {
            return;
        }

        Vec3d strikePos = findStrikePosition(world, owner, target);
        Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        spawnDepartureParticles(world, owner.getPos().add(0.0, owner.getHeight() * 0.5, 0.0));
        spawnArrivalParticles(world, target, strikePos);
        spawnTrail(world, owner.getPos().add(0.0, owner.getHeight() * 0.5, 0.0), lookTarget);
        spawnShadowEcho(world, strikePos, lookTarget, owner.getHeight());
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundRegistry.DARK_SWORD_WHOOSH_01.get(),
                SoundCategory.PLAYERS, 0.45F, 1.7F + world.random.nextFloat() * 0.25F);

        int previousDepth = CURRENT_CLONE_DEPTH.get();
        CURRENT_CLONE_DEPTH.set(strike.chainDepth());
        try {
            performWeaponStrike(owner, target);
        } finally {
            CURRENT_CLONE_DEPTH.set(previousDepth);
        }
    }

    private static void performWeaponStrike(ServerPlayerEntity player, LivingEntity target) {
        if (player == null || target == null || !player.isAlive() || !target.isAlive()) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        DamageSource damageSource = player.getDamageSources().playerAttack(player);
        target.timeUntilRegen = 0;
        if (target.damage(damageSource, (float) HelperMethods.getEntityAttackDamage(player)) && !stack.isEmpty()) {
            stack.getItem().postHit(stack, target, player);
        }
    }

    private static LivingEntity findRandomTarget(ServerWorld world, ServerPlayerEntity player) {
        double radius = Config.uniqueEffects.shadowsting.strikeRadius;
        Box box = new Box(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                entity != player
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkFriendlyFire(entity, player));

        if (targets.isEmpty()) {
            return null;
        }
        return targets.get(world.random.nextInt(targets.size()));
    }

    private static Vec3d findStrikePosition(ServerWorld world, ServerPlayerEntity player, LivingEntity target) {
        double baseAngle = world.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 12; i++) {
            double angle = baseAngle + (Math.PI * 2.0 * i / 12.0);
            Vec3d offset = new Vec3d(Math.cos(angle) * 1.35, 0.0, Math.sin(angle) * 1.35);
            Vec3d candidate = target.getPos().add(offset);
            candidate = new Vec3d(candidate.x, target.getY(), candidate.z);
            if (isSafePosition(world, player, candidate)) {
                return candidate;
            }
        }

        Vec3d away = player.getPos().subtract(target.getPos());
        if (away.horizontalLengthSquared() < 0.001) {
            away = target.getRotationVec(1.0F).negate();
        }
        return target.getPos().add(away.normalize().multiply(1.35));
    }

    private static boolean isSafePosition(ServerWorld world, ServerPlayerEntity player, Vec3d pos) {
        Box playerBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        if (!world.isSpaceEmpty(player, playerBox)) {
            return false;
        }
        return !world.getBlockState(BlockPos.ofFloored(pos)).isLiquid();
    }

    private static float[] getFacingRotation(Vec3d fromEye, Vec3d to) {
        Vec3d diff = to.subtract(fromEye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new float[]{yaw, pitch};
    }

    private static void lockPlayer(ServerPlayerEntity player) {
        player.clearActiveItem();
        player.setVelocity(0.0, 0.0, 0.0);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        player.extinguish();
    }

    private static void lockPlayer(ServerPlayerEntity player, ActiveShadowDance dance) {
        lockPlayer(player);
        if (dance.anchorPos != null) {
            player.networkHandler.requestTeleport(dance.anchorPos.x, dance.anchorPos.y, dance.anchorPos.z, player.getYaw(), player.getPitch());
        }
    }

    private static void clearMobTargets(ServerWorld world, ServerPlayerEntity player) {
        double radius = Config.uniqueEffects.shadowsting.strikeRadius + 8.0;
        Box box = new Box(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    private static void spawnDepartureParticles(ServerWorld world, Vec3d pos) {
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.15, pos.z, 8, 0.22, 0.28, 0.22, 0.06);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.35, pos.z, 2, 0.14, 0.18, 0.14, 0.02);
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.1, pos.z, 2, 0.12, 0.16, 0.12, 0.006);
        }
    }

    private static void spawnArrivalParticles(ServerWorld world, LivingEntity target, Vec3d strikePos) {
        Vec3d targetPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.5), 0.0);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, targetPos.x, targetPos.y + 0.15, targetPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, targetPos.x, targetPos.y, targetPos.z, 8, 0.22, 0.2, 0.22, 0.018);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, targetPos.x, targetPos.y, targetPos.z, 5, 0.18, 0.22, 0.18, 0.045);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, targetPos.x, targetPos.y + 0.15, targetPos.z, 2, 0.12, 0.16, 0.12, 0.018);
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, strikePos.x, strikePos.y + 0.2, strikePos.z, 2, 0.12, 0.12, 0.12, 0.006);
        }
    }

    private static void spawnTrail(ServerWorld world, Vec3d from, Vec3d to) {
        if (!Config.general.enableModernFieldEffects || from == null || to == null) {
            return;
        }

        Vec3d delta = to.subtract(from);
        int steps = Math.max(3, Math.min(10, (int) (delta.length() * 1.35)));
        for (int i = 1; i < steps; i++) {
            double progress = (double) i / (double) steps;
            Vec3d pos = from.add(delta.multiply(progress));
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.12, pos.z, 1, 0.025, 0.025, 0.025, 0.018);
        }
    }

    private static void spawnShadowEcho(ServerWorld world, Vec3d strikePos, Vec3d lookTarget, float playerHeight) {
        if (!Config.general.enableModernFieldEffects || strikePos == null || lookTarget == null) {
            return;
        }

        Vec3d facing = lookTarget.subtract(strikePos);
        if (facing.horizontalLengthSquared() < 0.001) {
            facing = new Vec3d(0.0, 0.0, 1.0);
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(facing.z, facing.x)) - 90.0F);
        ShadowstingAfterimageVisualEntity afterimage = new ShadowstingAfterimageVisualEntity(world, strikePos.x, strikePos.y, strikePos.z, yaw, AFTERIMAGE_LIFETIME);
        world.spawnEntity(afterimage);

        Vec3d side = new Vec3d(-facing.z, 0.0, facing.x).normalize();
        double height = Math.max(1.45, playerHeight);
        for (int i = 0; i < 12; i++) {
            double y = strikePos.y + 0.15 + height * i / 12.0;
            double width = 0.12 + 0.18 * Math.sin((double) i / 12.0 * Math.PI);
            Vec3d left = strikePos.add(side.multiply(width)).add(0.0, y - strikePos.y, 0.0);
            Vec3d right = strikePos.subtract(side.multiply(width)).add(0.0, y - strikePos.y, 0.0);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, left.x, left.y, left.z, 1, 0.012, 0.012, 0.012, 0.014);
            if (i % 2 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL, right.x, right.y, right.z, 1, 0.01, 0.01, 0.01, 0.006);
            }
        }
    }

    private static void spawnIdleParticles(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, 0.18, 0.0);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 1, 0.18, 0.05, 0.18, 0.018);
            if (world.getTime() % 4 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.18, pos.z, 1, 0.08, 0.04, 0.08, 0.006);
            }
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.08, 0.04, 0.08, 0.004);
        }
    }

    private static void spawnFailParticles(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, 0.8, 0.0);
        if (Config.general.enableModernFieldEffects) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 3, 0.16, 0.14, 0.16, 0.02);
        } else {
            world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 2, 0.12, 0.1, 0.12, 0.006);
        }
    }

    private static final class ActiveShadowDance {
        private final String worldKey;
        private final long endTick;
        private final Vec3d anchorPos;
        private long nextStrikeTick;
        private Vec3d lastStrikePos;
        private Vec3d lastVisualPos;
        private Vec3d lastLookTarget;
        private boolean finishing;
        private long finishStartTick;
        private Vec3d finishStartPos;
        private Vec3d finishEndPos;
        private float finishStartYaw;
        private float finishStartPitch;
        private float finishEndYaw;
        private float finishEndPitch;

        private ActiveShadowDance(String worldKey, long endTick, long nextStrikeTick, Vec3d anchorPos) {
            this.worldKey = worldKey;
            this.endTick = endTick;
            this.nextStrikeTick = nextStrikeTick;
            this.anchorPos = anchorPos;
            this.lastVisualPos = anchorPos;
        }
    }

    private record PendingShadowCloneStrike(UUID ownerId, UUID targetId, long triggerTick, int chainDepth) {
    }
}
