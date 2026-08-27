package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulrenderMarkVisualEntity;
import net.sweenus.simplyswords.api.ability.Phase3UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class SoulrenderMarkVisualManager {

    private static final String SOULRENDER_MARK_VISUAL_TAG = "simplyswords_soulrender_mark_visual";
    private static final double ORBIT_RADIUS = 0.72;
    private static final double ORBIT_HEIGHT = 1.15;
    private static final double ORBIT_BOB_HEIGHT = 0.16;
    private static final double ORBIT_ANGULAR_SPEED = 0.18;
    private static final double ORBIT_ANGULAR_SPEED_RANDOM_RANGE = 0.035;
    private static final double ORBIT_POSITION_SMOOTHING = 0.32;
    private static final int FADE_OUT_TICKS = 20;
    private static final int CONSUME_SHAKE_TICKS = 9;
    private static final int CONSUME_FLY_TICKS = 13;
    private static final double CONSUME_REACH_DISTANCE_SQUARED = 0.36;
    private static final float BASE_MARK_SCALE = 0.75F;
    private static final float SCALE_PER_STACK = 0.12F;
    private static final float MAX_MARK_SCALE = 1.85F;

    private static final Map<ServerWorld, Map<UUID, ActiveSoulrenderMark>> ACTIVE_MARKS = new HashMap<>();
    private static final Map<ServerWorld, Map<UniqueAbilityExecution, Integer>> PENDING_FINISH = new HashMap<>();

    private SoulrenderMarkVisualManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveSoulrenderMark> marks = ACTIVE_MARKS.get(world);
        return (marks != null && !marks.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void refreshMark(ServerWorld world, LivingEntity target, int durationTicks) {
        if (!Config.general.enableModernFieldEffects || world == null || target == null || !target.isAlive()) {
            return;
        }

        Map<UUID, ActiveSoulrenderMark> marks = ACTIVE_MARKS.computeIfAbsent(world, ignored -> new HashMap<>());
        UUID targetId = target.getUuid();
        long now = world.getTime();
        long expiryTick = now + Math.max(20, durationTicks);
        float scale = getStackScale(target);
        ActiveSoulrenderMark existing = marks.get(targetId);
        if (existing != null && world.getEntity(existing.visualId) instanceof SoulrenderMarkVisualEntity visual) {
            existing.expiryTick = expiryTick;
            existing.baseScale = scale;
            existing.consuming = false;
            existing.consumeStartTick = 0L;
            existing.ownerId = null;
            visual.setScale(scale);
            return;
        }

        Vec3d start = target.getPos().add(ORBIT_RADIUS, ORBIT_HEIGHT, 0.0);
        SoulrenderMarkVisualEntity visual = new SoulrenderMarkVisualEntity(world, start.x, start.y, start.z);
        visual.addCommandTag(SOULRENDER_MARK_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }

        marks.put(targetId, new ActiveSoulrenderMark(
                targetId,
                visual.getUuid(),
                now,
                expiryTick,
                world.random.nextDouble() * Math.PI * 2.0,
                ORBIT_ANGULAR_SPEED + (world.random.nextDouble() * 2.0 - 1.0) * ORBIT_ANGULAR_SPEED_RANDOM_RANGE,
                scale
        ));
    }

    public static void consumeMark(ServerWorld world, LivingEntity target, LivingEntity owner) {
        if (!Config.general.enableModernFieldEffects || world == null || target == null || owner == null) {
            return;
        }

        Map<UUID, ActiveSoulrenderMark> marks = ACTIVE_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            return;
        }

        ActiveSoulrenderMark mark = marks.get(target.getUuid());
        if (mark == null || mark.consuming) {
            return;
        }

        Entity visualEntity = world.getEntity(mark.visualId);
        if (!(visualEntity instanceof SoulrenderMarkVisualEntity visual)) {
            marks.remove(target.getUuid());
            return;
        }

        mark.consuming = true;
        mark.consumeStartTick = world.getTime();
        mark.ownerId = owner.getUuid();
        mark.consumeStartPos = visual.getPos();
        mark.baseScale = getStackScale(target);
        mark.expiryTick = world.getTime() + CONSUME_SHAKE_TICKS + CONSUME_FLY_TICKS + 5L;
        visual.setScale(mark.baseScale);

        Vec3d pos = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.65, 0.0);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 4, 0.16, 0.12, 0.16, 0.01);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 0.35F, 1.25F + world.random.nextFloat() * 0.2F);
    }

    public static void tick(ServerWorld world) {
        Map<UniqueAbilityExecution, Integer> finishing = PENDING_FINISH.remove(world);
        if (finishing != null) finishing.forEach((execution, affected) ->
                UniqueAbilityApi.finish(execution, Phase3UniqueAbilities.FINISH, affected));
        Map<UUID, ActiveSoulrenderMark> marks = ACTIVE_MARKS.get(world);
        if (marks == null || marks.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        Iterator<Map.Entry<UUID, ActiveSoulrenderMark>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveSoulrenderMark mark = iterator.next().getValue();
            Entity visualEntity = world.getEntity(mark.visualId);
            if (!(visualEntity instanceof SoulrenderMarkVisualEntity visual)) {
                iterator.remove();
                continue;
            }

            if (mark.consuming) {
                if (updateConsume(world, visual, mark)) {
                    iterator.remove();
                }
                continue;
            }

            LivingEntity target = getLivingEntity(world, mark.targetId);
            if (target == null || !target.isAlive() || world.getTime() > mark.expiryTick || !isSoulrenderMarked(target)) {
                visual.discard();
                iterator.remove();
                continue;
            }

            updateOrbit(world, visual, target, mark);
        }

        if (marks.isEmpty()) {
            ACTIVE_MARKS.remove(world);
        }
    }

    public static void finishNextTick(ServerWorld world, UniqueAbilityExecution execution, int affected) {
        PENDING_FINISH.computeIfAbsent(world, ignored -> new HashMap<>()).put(execution, affected);
    }

    public static void clear(ServerWorld world) {
        Map<UniqueAbilityExecution, Integer> finishing = PENDING_FINISH.remove(world);
        if (finishing != null) finishing.keySet().forEach(UniqueAbilityApi::cancel);
        Map<UUID, ActiveSoulrenderMark> marks = ACTIVE_MARKS.remove(world);
        if (marks != null) {
            for (ActiveSoulrenderMark mark : marks.values()) {
                if (world.getEntity(mark.visualId) instanceof SoulrenderMarkVisualEntity visual) visual.discard();
            }
        }
    }

    public static void clearAll() {
        for (Map<UniqueAbilityExecution, Integer> finishing : PENDING_FINISH.values()) {
            finishing.keySet().forEach(UniqueAbilityApi::cancel);
        }
        PENDING_FINISH.clear();
        ACTIVE_MARKS.clear();
    }

    private static void updateOrbit(ServerWorld world, SoulrenderMarkVisualEntity visual, LivingEntity target, ActiveSoulrenderMark mark) {
        long ticksRemaining = mark.expiryTick - world.getTime();
        float fade = MathHelper.clamp((float) ticksRemaining / FADE_OUT_TICKS, 0.0F, 1.0F);
        mark.baseScale = getStackScale(target);
        visual.setScale(mark.baseScale * fade);

        double t = (world.getTime() * mark.angularSpeed) + mark.angleOffset;
        double targetX = target.getX() + Math.cos(t) * ORBIT_RADIUS;
        double targetZ = target.getZ() + Math.sin(t) * ORBIT_RADIUS;
        double targetY = target.getY() + ORBIT_HEIGHT + Math.sin(t * 1.7) * ORBIT_BOB_HEIGHT;

        Vec3d smoothedPos = visual.getPos().lerp(new Vec3d(targetX, targetY, targetZ), ORBIT_POSITION_SMOOTHING);
        Vec3d motion = smoothedPos.subtract(visual.getPos());
        if (motion.horizontalLengthSquared() > 1.0E-4) {
            visual.setYaw((float) (Math.atan2(motion.x, motion.z) * (180.0F / Math.PI)));
        }
        visual.setPos(smoothedPos.x, smoothedPos.y, smoothedPos.z);
    }

    private static boolean updateConsume(ServerWorld world, SoulrenderMarkVisualEntity visual, ActiveSoulrenderMark mark) {
        LivingEntity owner = getLivingEntity(world, mark.ownerId);
        if (owner == null || !owner.isAlive()) {
            visual.discard();
            return true;
        }

        long consumeAge = world.getTime() - mark.consumeStartTick;
        if (consumeAge < CONSUME_SHAKE_TICKS) {
            double shakeX = (world.random.nextDouble() - 0.5) * 0.16;
            double shakeY = (world.random.nextDouble() - 0.5) * 0.12;
            double shakeZ = (world.random.nextDouble() - 0.5) * 0.16;
            visual.setPos(mark.consumeStartPos.x + shakeX, mark.consumeStartPos.y + shakeY, mark.consumeStartPos.z + shakeZ);
            visual.setScale(mark.baseScale * (1.0F + ((float) consumeAge / CONSUME_SHAKE_TICKS) * 0.85F));
            return false;
        }

        float flyProgress = MathHelper.clamp((float) (consumeAge - CONSUME_SHAKE_TICKS) / CONSUME_FLY_TICKS, 0.0F, 1.0F);
        Vec3d ownerPos = owner.getPos().add(0.0, owner.getStandingEyeHeight() * 0.62, 0.0);
        Vec3d targetPos = mark.consumeStartPos.lerp(ownerPos, easeIn(flyProgress));
        visual.setPos(targetPos.x, targetPos.y, targetPos.z);
        visual.setScale(mark.baseScale * (1.85F - flyProgress * 1.15F));

        Vec3d motion = ownerPos.subtract(visual.getPos());
        if (motion.horizontalLengthSquared() > 1.0E-4) {
            visual.setYaw((float) (Math.atan2(motion.x, motion.z) * (180.0F / Math.PI)));
        }

        if (flyProgress >= 1.0F || visual.squaredDistanceTo(owner) <= CONSUME_REACH_DISTANCE_SQUARED) {
            world.spawnParticles(ParticleTypes.SCULK_SOUL, ownerPos.x, ownerPos.y, ownerPos.z, 5, 0.18, 0.12, 0.18, 0.01);
            visual.discard();
            return true;
        }
        return false;
    }

    private static boolean isSoulrenderMarked(LivingEntity target) {
        return target.hasStatusEffect(StatusEffects.SLOWNESS) && target.hasStatusEffect(StatusEffects.WEAKNESS);
    }

    private static float getStackScale(LivingEntity target) {
        if (!target.hasStatusEffect(StatusEffects.SLOWNESS)) {
            return BASE_MARK_SCALE;
        }

        int stacks = target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1;
        return MathHelper.clamp(BASE_MARK_SCALE + stacks * SCALE_PER_STACK, BASE_MARK_SCALE, MAX_MARK_SCALE);
    }

    private static float easeIn(float t) {
        return t * t;
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Entity entity = world.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SoulrenderMarkVisualEntity && entity.getCommandTags().contains(SOULRENDER_MARK_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveSoulrenderMark {
        private final UUID targetId;
        private final UUID visualId;
        private final long spawnTick;
        private long expiryTick;
        private final double angleOffset;
        private final double angularSpeed;
        private boolean consuming;
        private long consumeStartTick;
        private UUID ownerId;
        private Vec3d consumeStartPos;
        private float baseScale;

        private ActiveSoulrenderMark(UUID targetId, UUID visualId, long spawnTick, long expiryTick, double angleOffset, double angularSpeed, float baseScale) {
            this.targetId = targetId;
            this.visualId = visualId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.angleOffset = angleOffset;
            this.angularSpeed = angularSpeed;
            this.consumeStartPos = Vec3d.ZERO;
            this.baseScale = baseScale;
        }
    }
}
