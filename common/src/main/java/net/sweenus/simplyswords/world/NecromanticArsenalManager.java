package net.sweenus.simplyswords.world;

import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsMinion;
import net.sweenus.simplyswords.entity.SimplySwordsSkeletonMinionEntity;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import java.util.UUID;

public final class NecromanticArsenalManager {

    private static final double MINION_SEARCH_RADIUS = 96.0;

    private NecromanticArsenalManager() {
    }

    public static boolean trySummon(LivingEntity attacker, ItemStack stack) {
        if (attacker instanceof SimplySwordsMinion) {
            return false;
        }
        if (attacker instanceof ServerPlayerEntity player) {
            return trySummon(player, stack);
        }
        return trySummonNonPlayer(attacker, stack);
    }

    private static boolean trySummonNonPlayer(LivingEntity attacker, ItemStack stack) {
        if (attacker == null || stack == null || stack.isEmpty() || !attacker.isAlive()) {
            return false;
        }

        ServerWorld world = (ServerWorld) attacker.getWorld();
        int maxMinions = Math.max(1, Config.gemPowers.necromanticArsenal.maxMinions);
        if (getActiveMinionCount(world, attacker) >= maxMinions) {
            return false;
        }

        SimplySwordsSkeletonMinionEntity minion = EntityRegistry.SKELETON_MINION.get()
                .spawn(world, attacker.getBlockPos(), SpawnReason.MOB_SUMMONED);
        if (minion == null) {
            return false;
        }

        minion.initializeMinion(attacker, stack.copy(), -1,
                world.getTime() + Math.max(1, Config.gemPowers.necromanticArsenal.duration),
                getSummonPower(attacker, stack));
        world.spawnParticles(ParticleTypes.SOUL, minion.getX(), minion.getBodyY(0.55), minion.getZ(), 22, 0.45, 0.55, 0.45, 0.08);
        world.spawnParticles(ParticleTypes.SMOKE, minion.getX(), minion.getBodyY(0.45), minion.getZ(), 14, 0.35, 0.35, 0.35, 0.035);
        world.playSound(null, minion.getBlockPos(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.65F, 1.35F);
        return true;
    }

    public static boolean trySummon(ServerPlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || !player.isAlive()) {
            return false;
        }

        ServerWorld world = player.getServerWorld();
        int maxMinions = Math.max(1, Config.gemPowers.necromanticArsenal.maxMinions);
        if (getActiveMinionCount(world, player) >= maxMinions) {
            return false;
        }

        SimplySwordsSkeletonMinionEntity minion = EntityRegistry.SKELETON_MINION.get()
                .spawn(world, player.getBlockPos(), SpawnReason.MOB_SUMMONED);
        if (minion == null) {
            return false;
        }

        minion.initializeMinion(player, stack.copy(), findSourceWeaponSlot(player, stack),
                world.getTime() + Math.max(1, Config.gemPowers.necromanticArsenal.duration),
                getSummonPower(player, stack));
        world.spawnParticles(ParticleTypes.SOUL, minion.getX(), minion.getBodyY(0.55), minion.getZ(), 22, 0.45, 0.55, 0.45, 0.08);
        world.spawnParticles(ParticleTypes.SMOKE, minion.getX(), minion.getBodyY(0.45), minion.getZ(), 14, 0.35, 0.35, 0.35, 0.035);
        world.playSound(null, minion.getBlockPos(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.65F, 1.35F);
        return true;
    }

    public static void retargetMinions(ServerPlayerEntity player, LivingEntity attacker) {
        if (player == null || attacker == null || !player.isAlive() || !attacker.isAlive()) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        for (SimplySwordsSkeletonMinionEntity minion : getOwnedMinions(world, player)) {
            minion.trySetRetaliationTarget(attacker);
        }
    }

    private static float getSummonPower(LivingEntity owner, ItemStack stack) {
        float damageScaling = (float) Math.max(0.0001, Config.gemPowers.necromanticArsenal.damageScaling);
        return HelperMethods.gemPowerScaledDamage("soul", owner, stack, 1.0F,
                (float) Config.gemPowers.necromanticArsenal.spellScaling / damageScaling);
    }

    private static int getActiveMinionCount(ServerWorld world, ServerPlayerEntity player) {
        return getOwnedMinions(world, player).size();
    }

    private static int getActiveMinionCount(ServerWorld world, LivingEntity owner) {
        Box searchBox = owner.getBoundingBox().expand(MINION_SEARCH_RADIUS);
        return world.getEntitiesByClass(SimplySwordsSkeletonMinionEntity.class,
                searchBox, minion -> minion.isAlive() && owner.getUuid().equals(minion.getOwnerUuid())).size();
    }

    private static java.util.List<SimplySwordsSkeletonMinionEntity> getOwnedMinions(ServerWorld world, ServerPlayerEntity player) {
        Box searchBox = player.getBoundingBox().expand(MINION_SEARCH_RADIUS);
        return world.getEntitiesByClass(SimplySwordsSkeletonMinionEntity.class, searchBox, minion ->
                minion.isAlive() && player.getUuid().equals(minion.getOwnerUuid()));
    }

    private static int findSourceWeaponSlot(ServerPlayerEntity player, ItemStack sourceStack) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack inventoryStack = player.getInventory().getStack(slot);
            if (inventoryStack == sourceStack || inventoryStack.equals(sourceStack)) {
                return slot;
            }
        }
        return -1;
    }
}
