package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.FallingSnifferEntity;
import net.sweenus.simplyswords.util.HelperMethods;

public final class SnifferSlamManager {

    private SnifferSlamManager() {
    }

    public static boolean trySummon(LivingEntity attacker, LivingEntity target, ItemStack stack) {
        if (attacker == null || target == null || stack == null || stack.isEmpty() || !attacker.isAlive() || !target.isAlive()) {
            return false;
        }
        if (!(attacker.getWorld() instanceof ServerWorld world) || target.getWorld() != world) {
            return false;
        }
        if (!HelperMethods.checkFriendlyFire(target, attacker)) {
            return false;
        }

        int maxActiveSlams = Math.max(1, Config.gemPowers.snifferSlam.maxActiveSlams);
        if (getActiveSlamCount(world, attacker) >= maxActiveSlams) {
            return false;
        }

        float damage = AwakeningApi.scaleGemPower(stack,
                HelperMethods.attackScaledDamage(attacker, stack, (float) Config.gemPowers.snifferSlam.damageScaling));
        if (damage <= 0.0F) {
            return false;
        }

        FallingSnifferEntity sniffer = new FallingSnifferEntity(world, attacker, target, stack,
                damage,
                Config.gemPowers.snifferSlam.fallHeight,
                Config.gemPowers.snifferSlam.fallSpeed,
                Config.gemPowers.snifferSlam.impactRadius,
                AwakeningApi.scaleGemPowerDuration(stack, Config.gemPowers.snifferSlam.immobiliseDurationTicks),
                Config.gemPowers.snifferSlam.lingerTicks);
        if (!world.spawnEntity(sniffer)) {
            return false;
        }
        playSpawnSound(world, sniffer);
        return true;
    }

    private static void playSpawnSound(ServerWorld world, FallingSnifferEntity sniffer) {
        if (world.random.nextInt(100) < Config.gemPowers.snifferSlam.screamingGoatSoundChance) {
            world.playSound(null, sniffer.getBlockPos(), SoundEvents.ENTITY_GOAT_SCREAMING_HURT, SoundCategory.PLAYERS, 1.2F,
                    0.9F + world.random.nextFloat() * 0.18F);
            return;
        }

        float[] pitches = {0.72F, 0.88F, 1.04F};
        world.playSound(null, sniffer.getBlockPos(), SoundEvents.ENTITY_SNIFFER_HURT, SoundCategory.PLAYERS, 1.2F,
                pitches[world.random.nextInt(pitches.length)]);
    }

    private static int getActiveSlamCount(ServerWorld world, LivingEntity owner) {
        int count = 0;
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof FallingSnifferEntity sniffer
                    && sniffer.isAlive()
                    && owner.getUuid().equals(sniffer.getOwnerUuid())) {
                count++;
            }
        }
        return count;
    }
}
