package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.RevivalCandleVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RevivalCandleVisualManager {

    private static final String REVIVAL_CANDLE_VISUAL_TAG = "simplyswords_revival_candle_visual";
    private static final double IDLE_SIDE_OFFSET = 0.72;
    private static final double IDLE_BACK_OFFSET = -0.18;
    private static final double IDLE_HEIGHT_OFFSET = 0.08;
    private static final double ACTIVE_FORWARD_OFFSET = 1.15;
    private static final double ACTIVE_HEIGHT_OFFSET = -0.08;
    private static final double BOB_AMPLITUDE = 0.045;
    private static final double BOB_SPEED = 0.18;
    private static final double FOLLOW_LERP = 0.28;
    private static final double FOLLOW_TELEPORT_DISTANCE_SQ = 9.0;
    private static final int ACTIVATE_MOVE_TICKS = 7;
    private static final int ACTIVATE_WOBBLE_TICKS = 9;
    private static final int ACTIVATE_SHRINK_TICKS = 12;
    private static final int TOTAL_ACTIVATE_TICKS = ACTIVATE_MOVE_TICKS + ACTIVATE_WOBBLE_TICKS + ACTIVATE_SHRINK_TICKS;
    private static final double CANDLE_RENDER_SCALE = 1.04;
    private static final double CANDLE_FLAME_Y_OFFSET = CANDLE_RENDER_SCALE * 0.48;
    private static final double CANDLE_WAX_Y_OFFSET = CANDLE_RENDER_SCALE * 0.18;
    private static final int WEAPON_WICKPIERCER = 1;
    private static final int WEAPON_WAXWEAVER = 2;

    private static final Map<UUID, ActiveRevivalCandle> ACTIVE_CANDLES = new HashMap<>();

    private RevivalCandleVisualManager() {
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID ownerId = player.getUuid();
        ActiveRevivalCandle active = ACTIVE_CANDLES.get(ownerId);

        if (active != null && active.activating) {
            if (tickActivation(world, player, active)) {
                ACTIVE_CANDLES.remove(ownerId);
            }
            return;
        }

        int weaponType = getHeldRevivalWeaponType(player);
        if (!Config.general.enableModernFieldEffects || weaponType == 0 || isHeldWeaponCoolingDown(player)) {
            discardActive(world, ownerId);
            ACTIVE_CANDLES.remove(ownerId);
            return;
        }

        RevivalCandleVisualEntity candle = resolveTracked(world, active != null ? active.visualId : null, ownerId);
        if (candle == null) {
            Vec3d start = getIdleAnchor(player, weaponType, 0.0F);
            candle = new RevivalCandleVisualEntity(world, ownerId, player.getId(), weaponType, start.x, start.y, start.z);
            candle.addCommandTag(REVIVAL_CANDLE_VISUAL_TAG);
            if (!world.spawnEntity(candle)) {
                return;
            }
            active = new ActiveRevivalCandle(candle.getUuid(), weaponType);
            ACTIVE_CANDLES.put(ownerId, active);
        }

        active.weaponType = weaponType;
        candle.setOwnerEntityId(player.getId());
        candle.setWeaponType(weaponType);
        candle.setActivating(false);
        candle.setScale(1.0F);
        updateIdlePosition(player, candle, weaponType);
        spawnIdleParticles(world, candle);

        if (player.age % 80 == 0) {
            cleanupOwnerOrphans(world, ownerId, candle.getUuid());
        }
    }

    public static void activate(ServerPlayerEntity player, ItemStack stack) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (!Config.general.enableModernFieldEffects || player == null || stack == null || stack.isEmpty()) {
            return;
        }

        int weaponType = getWeaponType(stack.getItem());
        if (weaponType == 0) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        UUID ownerId = player.getUuid();
        ActiveRevivalCandle active = ACTIVE_CANDLES.get(ownerId);
        RevivalCandleVisualEntity candle = resolveTracked(world, active != null ? active.visualId : null, ownerId);
        if (candle == null) {
            Vec3d start = getIdleAnchor(player, weaponType, 0.0F);
            candle = new RevivalCandleVisualEntity(world, ownerId, player.getId(), weaponType, start.x, start.y, start.z);
            candle.addCommandTag(REVIVAL_CANDLE_VISUAL_TAG);
            if (!world.spawnEntity(candle)) {
                return;
            }
        }

        candle.setOwnerEntityId(player.getId());
        candle.setWeaponType(weaponType);
        candle.setActivating(true);
        candle.setScale(1.0F);

        ActiveRevivalCandle activating = new ActiveRevivalCandle(candle.getUuid(), weaponType);
        activating.activating = true;
        activating.activationStartTick = world.getTime();
        activating.activationStartPos = candle.getPos();
        ACTIVE_CANDLES.put(ownerId, activating);

        Vec3d pos = player.getCameraPosVec(1.0F);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 6, 0.14, 0.1, 0.14, 0.01);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 0.65F, 0.85F + player.getRandom().nextFloat() * 0.15F);
        }
    }

    public static void tickWorld(ServerWorld world) {
        if (world.getTime() % 80L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof RevivalCandleVisualEntity candle) || !entity.getCommandTags().contains(REVIVAL_CANDLE_VISUAL_TAG)) {
                continue;
            }
            UUID ownerId = candle.getOwnerUuid();
            ServerPlayerEntity owner = ownerId == null ? null : world.getServer().getPlayerManager().getPlayer(ownerId);
            ActiveRevivalCandle active = ownerId == null ? null : ACTIVE_CANDLES.get(ownerId);
            if (owner == null || !owner.isAlive()) {
                candle.discard();
                if (ownerId != null) {
                    ACTIVE_CANDLES.remove(ownerId);
                }
                continue;
            }
            if (active != null && candle.getUuid().equals(active.visualId)) {
                continue;
            }
            if (!Config.general.enableModernFieldEffects || getHeldRevivalWeaponType(owner) == 0 || isHeldWeaponCoolingDown(owner)) {
                candle.discard();
                ACTIVE_CANDLES.remove(ownerId);
                continue;
            }
            if (active == null) {
                candle.discard();
            }
        }
    }

    private static boolean tickActivation(ServerWorld world, ServerPlayerEntity player, ActiveRevivalCandle active) {
        RevivalCandleVisualEntity candle = resolveTracked(world, active.visualId, player.getUuid());
        if (candle == null) {
            return true;
        }

        long age = world.getTime() - active.activationStartTick;
        Vec3d front = getActivationAnchor(player);
        candle.setOwnerEntityId(player.getId());
        candle.setWeaponType(active.weaponType);
        candle.setActivating(true);

        if (age < ACTIVATE_MOVE_TICKS) {
            float t = MathHelper.clamp((float) age / ACTIVATE_MOVE_TICKS, 0.0F, 1.0F);
            Vec3d pos = active.activationStartPos.lerp(front, easeOut(t));
            candle.setPos(pos.x, pos.y, pos.z);
            candle.setScale(1.0F);
            return false;
        }

        if (age < ACTIVATE_MOVE_TICKS + ACTIVATE_WOBBLE_TICKS) {
            long wobbleAge = age - ACTIVATE_MOVE_TICKS;
            double wobble = Math.sin(wobbleAge * 1.9) * (0.12 * (1.0 - (double) wobbleAge / ACTIVATE_WOBBLE_TICKS));
            Vec3d right = getRightVector(player);
            Vec3d up = new Vec3d(0.0, Math.cos(wobbleAge * 2.3) * 0.035, 0.0);
            Vec3d pos = front.add(right.multiply(wobble)).add(up);
            candle.setPos(pos.x, pos.y, pos.z);
            candle.setScale(1.0F + (float) Math.sin(wobbleAge * 0.7) * 0.08F);
            return false;
        }

        if (age < TOTAL_ACTIVATE_TICKS) {
            long shrinkAge = age - ACTIVATE_MOVE_TICKS - ACTIVATE_WOBBLE_TICKS;
            float t = MathHelper.clamp((float) shrinkAge / ACTIVATE_SHRINK_TICKS, 0.0F, 1.0F);
            Vec3d right = getRightVector(player);
            double wobble = Math.sin(shrinkAge * 2.2) * 0.035 * (1.0 - t);
            Vec3d pos = front.add(right.multiply(wobble));
            candle.setPos(pos.x, pos.y, pos.z);
            candle.setScale(1.0F - easeIn(t));
            return false;
        }

        Vec3d pos = candle.getPos();
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 8, 0.08, 0.08, 0.08, 0.01);
        candle.discard();
        return true;
    }

    private static void updateIdlePosition(ServerPlayerEntity player, RevivalCandleVisualEntity candle, int weaponType) {
        Vec3d idle = getIdleAnchor(player, weaponType, 0.0F);
        if (candle.age < 2 || candle.squaredDistanceTo(idle.x, idle.y, idle.z) > FOLLOW_TELEPORT_DISTANCE_SQ) {
            candle.setPos(idle.x, idle.y, idle.z);
        } else {
            Vec3d smoothed = candle.getPos().lerp(idle, FOLLOW_LERP);
            candle.setPos(smoothed.x, smoothed.y, smoothed.z);
        }
    }

    private static Vec3d getIdleAnchor(ServerPlayerEntity player, int weaponType, float tickDelta) {
        Vec3d forward = Vec3d.fromPolar(0.0F, player.getYaw()).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        double side = weaponType == WEAPON_WAXWEAVER ? -IDLE_SIDE_OFFSET : IDLE_SIDE_OFFSET;
        double bob = Math.sin((player.age + tickDelta) * BOB_SPEED) * BOB_AMPLITUDE;
        return player.getPos()
                .add(0.0, player.getStandingEyeHeight() + IDLE_HEIGHT_OFFSET + bob, 0.0)
                .add(right.multiply(side))
                .add(forward.multiply(IDLE_BACK_OFFSET));
    }

    private static void spawnIdleParticles(ServerWorld world, RevivalCandleVisualEntity candle) {
        if (candle.isSubmergedIn(FluidTags.WATER)) {
            return;
        }

        double flameY = candle.getY() + CANDLE_FLAME_Y_OFFSET * candle.getScale();
        double waxY = candle.getY() + CANDLE_WAX_Y_OFFSET * candle.getScale();
        if (world.getTime() % 2L == 0L) {
            world.spawnParticles(ParticleTypes.FLAME, candle.getX(), flameY, candle.getZ(), 1, 0.01, 0.01, 0.01, 0.0);
        }
        if (world.getTime() % 4L == 0L) {
            world.spawnParticles(ParticleTypes.SMOKE, candle.getX(), flameY + 0.04, candle.getZ(), 2, 0.025, 0.025, 0.025, 0.003);
        }
        if (world.getTime() % 5L == 0L) {
            world.spawnParticles(ParticleTypes.WHITE_ASH, candle.getX(), waxY, candle.getZ(), 2, 0.025, 0.01, 0.025, 0.002);
        }
    }

    private static Vec3d getActivationAnchor(ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0F).normalize();
        return player.getPos()
                .add(0.0, player.getStandingEyeHeight() + ACTIVE_HEIGHT_OFFSET, 0.0)
                .add(look.multiply(ACTIVE_FORWARD_OFFSET));
    }

    private static Vec3d getRightVector(ServerPlayerEntity player) {
        Vec3d forward = Vec3d.fromPolar(0.0F, player.getYaw()).normalize();
        return new Vec3d(-forward.z, 0.0, forward.x);
    }

    private static RevivalCandleVisualEntity resolveTracked(ServerWorld world, UUID candleId, UUID ownerId) {
        if (candleId == null) {
            return null;
        }

        Entity entity = world.getEntity(candleId);
        if (!(entity instanceof RevivalCandleVisualEntity candle) || !ownerId.equals(candle.getOwnerUuid()) || !candle.isAlive()) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        return candle;
    }

    private static void discardActive(ServerWorld world, UUID ownerId) {
        ActiveRevivalCandle active = ACTIVE_CANDLES.get(ownerId);
        if (active == null || active.visualId == null) {
            return;
        }
        Entity entity = world.getEntity(active.visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void cleanupOwnerOrphans(ServerWorld world, UUID ownerId, UUID keepId) {
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof RevivalCandleVisualEntity candle) || !entity.getCommandTags().contains(REVIVAL_CANDLE_VISUAL_TAG)) {
                continue;
            }
            if (ownerId.equals(candle.getOwnerUuid()) && !entity.getUuid().equals(keepId)) {
                entity.discard();
            }
        }
    }

    private static int getHeldRevivalWeaponType(ServerPlayerEntity player) {
        return getWeaponType(player.getMainHandStack().getItem());
    }

    private static int getWeaponType(Item item) {
        if (item == ItemsRegistry.WICKPIERCER.get()) {
            return WEAPON_WICKPIERCER;
        }
        if (item == ItemsRegistry.WAXWEAVER.get()) {
            return WEAPON_WAXWEAVER;
        }
        return 0;
    }

    private static boolean isHeldWeaponCoolingDown(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) return true;
        if (stack.isOf(ItemsRegistry.WAXWEAVER.get())) {
            return RevivalCooldownManager.isCoolingDown(player.getServerWorld(), player, stack);
        }
        return player.getItemCooldownManager().isCoolingDown(stack.getItem());
    }

    private static float easeOut(float t) {
        float inverted = 1.0F - t;
        return 1.0F - inverted * inverted;
    }

    private static float easeIn(float t) {
        return t * t;
    }

    private static final class ActiveRevivalCandle {
        private final UUID visualId;
        private int weaponType;
        private boolean activating;
        private long activationStartTick;
        private Vec3d activationStartPos = Vec3d.ZERO;

        private ActiveRevivalCandle(UUID visualId, int weaponType) {
            this.visualId = visualId;
            this.weaponType = weaponType;
        }
    }
}
