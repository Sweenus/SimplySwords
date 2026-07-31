package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SimplySwordsGoatStampedeEntity;
import net.sweenus.simplyswords.entity.SimplySwordsMinion;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GoatStampedeManager {

    private static final double LATERAL_SPACING = 0.9;

    private GoatStampedeManager() {
    }

    public static boolean trySummon(LivingEntity attacker, ItemStack stack) {
        if (attacker instanceof SimplySwordsMinion) {
            return false;
        }
        if (attacker == null || stack == null || stack.isEmpty() || !attacker.isAlive()) {
            return false;
        }
        if (!(attacker.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        int maxActiveStampedes = Math.max(1, Config.gemPowers.goatStampede.maxActiveStampedes);
        if (getActiveStampedeCount(world, attacker) >= maxActiveStampedes) {
            return false;
        }

        Vec3d look = attacker.getRotationVec(1.0F);
        Vec3d direction = new Vec3d(look.x, 0.0, look.z);
        direction = direction.lengthSquared() > 1.0E-4 ? direction.normalize() : Vec3d.fromPolar(0.0F, attacker.getYaw());
        Vec3d perpendicular = new Vec3d(-direction.z, 0.0, direction.x);

        double summonerKnockback = Config.gemPowers.goatStampede.summonerKnockback;
        if (summonerKnockback > 0.0) {
            Vec3d pushedVelocity = attacker.getVelocity().add(
                    -direction.x * summonerKnockback, 0.1 * summonerKnockback, -direction.z * summonerKnockback);
            attacker.setVelocity(pushedVelocity);
            attacker.velocityModified = true;
        }

        int goatCount = Math.max(1, Config.gemPowers.goatStampede.goatCount);
        long expiresAtTick = world.getTime() + Math.max(1, Config.gemPowers.goatStampede.duration);
        UUID stampedeId = UUID.randomUUID();
        boolean anyScreaming = false;

        for (int i = 0; i < goatCount; i++) {
            double lateralOffset = (i - (goatCount - 1) / 2.0) * LATERAL_SPACING;
            Vec3d spawnPos = attacker.getPos().add(perpendicular.multiply(lateralOffset));

            SimplySwordsGoatStampedeEntity goat = EntityRegistry.GOAT_STAMPEDE.get()
                    .spawn(world, attacker.getBlockPos(), SpawnReason.MOB_SUMMONED);
            if (goat == null) {
                continue;
            }

            boolean screaming = attacker.getRandom().nextInt(100) < Config.gemPowers.goatStampede.screamingChance;
            anyScreaming |= screaming;
            double damageScaling = screaming ? Config.gemPowers.goatStampede.screamingDamageScaling : Config.gemPowers.goatStampede.damageScaling;
            float damage = AwakeningApi.scaleGemPower(stack,
                    HelperMethods.attackScaledDamage(attacker, stack, (float) damageScaling));

            goat.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, attacker.getYaw(), 0.0F);
            goat.initializeStampede(attacker, stack, stampedeId, direction, expiresAtTick, damage,
                    AwakeningApi.scaleGemPower(stack, Config.gemPowers.goatStampede.knockbackStrength),
                    Config.gemPowers.goatStampede.chargeSpeed, screaming);
        }

        world.spawnParticles(ParticleTypes.POOF, attacker.getX(), attacker.getBodyY(0.4), attacker.getZ(), 14, 0.4, 0.3, 0.4, 0.03);
        world.playSound(null, attacker.getBlockPos(),
                anyScreaming ? SoundEvents.ENTITY_GOAT_SCREAMING_AMBIENT : SoundEvents.ENTITY_GOAT_AMBIENT,
                SoundCategory.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    private static int getActiveStampedeCount(ServerWorld world, LivingEntity owner) {
        Set<UUID> activeStampedes = new HashSet<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SimplySwordsGoatStampedeEntity goat
                    && goat.isAlive()
                    && owner.getUuid().equals(goat.getOwnerUuid())) {
                UUID stampedeId = goat.getStampedeId();
                activeStampedes.add(stampedeId == null ? goat.getUuid() : stampedeId);
            }
        }
        return activeStampedes.size();
    }
}
