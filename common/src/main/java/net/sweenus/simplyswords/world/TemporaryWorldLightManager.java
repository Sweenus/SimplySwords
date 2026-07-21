package net.sweenus.simplyswords.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TemporaryWorldLightManager {

    private static final Map<World, Map<BlockPos, Long>> ACTIVE_LIGHTS = new HashMap<>();
    private static final int UPDATE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.NO_REDRAW;

    private TemporaryWorldLightManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<BlockPos, Long> lights = ACTIVE_LIGHTS.get(world);
        return lights != null && !lights.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<BlockPos, Long> lights = ACTIVE_LIGHTS.get(world);
        if (lights == null || lights.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<Map.Entry<BlockPos, Long>> iterator = lights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (entry.getValue() > now) {
                continue;
            }

            BlockPos pos = entry.getKey();
            if (world.getBlockState(pos).isOf(Blocks.LIGHT)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), UPDATE_FLAGS);
            }
            iterator.remove();
        }

        if (lights.isEmpty()) {
            ACTIVE_LIGHTS.remove(world);
        }
    }

    public static void placeBoltLights(ServerWorld world, Vec3d start, Vec3d end, int lightLevel, int lifetimeTicks) {
        Vec3d offset = end.subtract(start);
        double length = offset.length();
        if (length < 0.01) {
            return;
        }

        int samples = MathHelper.clamp((int) Math.ceil(length / 2.0) + 1, 2, 4);
        for (int i = 0; i < samples; i++) {
            double progress = samples == 1 ? 0.0 : (double) i / (samples - 1);
            placeLight(world, start.add(offset.multiply(progress)), lightLevel, lifetimeTicks);
        }
    }

    private static void placeLight(ServerWorld world, Vec3d pos, int lightLevel, int lifetimeTicks) {
        BlockPos blockPos = BlockPos.ofFloored(pos);
        Map<BlockPos, Long> lights = ACTIVE_LIGHTS.computeIfAbsent(world, key -> new HashMap<>());
        BlockState currentState = world.getBlockState(blockPos);
        boolean ownLight = currentState.isOf(Blocks.LIGHT) && lights.containsKey(blockPos);
        if (!currentState.isAir() && !ownLight) {
            return;
        }
        if (!currentState.getFluidState().isEmpty()) {
            return;
        }

        int clampedLight = MathHelper.clamp(lightLevel, 1, 15);
        BlockState lightState = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, clampedLight);
        if (world.setBlockState(blockPos, lightState, UPDATE_FLAGS)) {
            lights.put(blockPos.toImmutable(), world.getTime() + Math.max(1, lifetimeTicks));
        }
    }
}
