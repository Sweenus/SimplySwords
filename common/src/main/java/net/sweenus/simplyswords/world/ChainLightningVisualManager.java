package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.entity.ChainLightningVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class ChainLightningVisualManager {

    public static final LightningVisualSettings STORMBRINGER_SETTINGS = new LightningVisualSettings(0x83E8FF, 8, 0.04F, 3);

    private ChainLightningVisualManager() {
    }

    public static void spawnBolt(ServerWorld world, Vec3d start, Vec3d end, LightningVisualSettings settings) {
        spawnBolt(world, start, end, settings, true);
    }

    public static void spawnBolt(ServerWorld world, Vec3d start, Vec3d end,
                                 LightningVisualSettings settings, boolean illuminate) {
        Vec3d offset = end.subtract(start);
        if (offset.lengthSquared() < 0.01) {
            return;
        }
        ChainLightningVisualEntity visual = new ChainLightningVisualEntity(
                world,
                start.x,
                start.y,
                start.z,
                offset.x,
                offset.y,
                offset.z,
                Math.max(1, settings.lifetime()),
                world.random.nextInt(),
                settings.color(),
                Math.max(0.01F, settings.thickness()),
                Math.max(0, settings.branches())
        );
        world.spawnEntity(visual);
        if (illuminate) {
            TemporaryWorldLightManager.placeBoltLights(world, start, end, 5, 3);
        }
    }

    public static void spawnChain(ServerWorld world, List<Vec3d> points, LightningVisualSettings settings) {
        for (int i = 0; i < points.size() - 1; i++) {
            spawnBolt(world, points.get(i), points.get(i + 1), settings);
        }
    }

    public static int damageStormbringerChain(ServerWorld world, LivingEntity player, LivingEntity firstTarget, int chainCount, float damage, double range) {
        return damageChain(world, player, firstTarget, chainCount, damage, range, STORMBRINGER_SETTINGS);
    }

    public static boolean damageSkyBolt(ServerWorld world, LivingEntity player, ItemStack stack, LivingEntity target, float damage, double skyHeight, LightningVisualSettings settings) {
        if (world == null || player == null || target == null || !player.isAlive() || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, player)) {
            return false;
        }

        DamageSource source = player.getDamageSources().indirectMagic(player, player);
        boolean[] result = {false};
        float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage);
        WeaponImplicitRegistry.runSuppressed(() -> result[0] = HelperMethods.damageThroughIframes(target, source, enchantedDamage));

        Vec3d end = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0);
        Vec3d start = end.add(0.0, Math.max(1.0, skyHeight), 0.0);
        spawnBolt(world, start, end, settings);
        spawnImpactEffects(world, target);
        playTargetCrackle(world, target, 1);
        return result[0];
    }

    public static int damageChain(ServerWorld world, LivingEntity player, LivingEntity firstTarget, int chainCount, float damage, double range, LightningVisualSettings settings) {
        return damageChain(world, player, player, firstTarget, chainCount, damage, range, settings);
    }

    public static int damageChain(ServerWorld world, LivingEntity player, LivingEntity sourceEntity, LivingEntity firstTarget,
                                  int chainCount, float damage, double range, LightningVisualSettings settings) {
        return damageChain(world, player, sourceEntity, player.getMainHandStack(), firstTarget,
                chainCount, damage, range, settings);
    }

    public static int damageChain(ServerWorld world, LivingEntity player, LivingEntity sourceEntity, ItemStack stack,
                                  LivingEntity firstTarget, int chainCount, float damage, double range,
                                  LightningVisualSettings settings) {
        if (chainCount <= 0 || firstTarget == null || !firstTarget.isAlive()) {
            return 0;
        }

        List<LivingEntity> chain = buildChain(world, player, firstTarget, chainCount, Math.max(0.5, range));
        if (chain.isEmpty()) {
            return 0;
        }

        DamageSource source = player.getDamageSources().indirectMagic(player, player);
        int damaged = 0;
        for (LivingEntity target : chain) {
            boolean[] result = {false};
            float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, damage);
            WeaponImplicitRegistry.runSuppressed(() -> result[0] = HelperMethods.damageThroughIframes(target, source, enchantedDamage));
            if (result[0]) {
                damaged++;
                spawnImpactEffects(world, target);
                playTargetCrackle(world, target, damaged);
            }
        }

        List<Vec3d> points = new ArrayList<>();
        LivingEntity visualSource = sourceEntity == null ? player : sourceEntity;
        points.add(visualSource.getPos().add(0.0, Math.max(0.55, visualSource.getHeight() * 0.6), 0.0));
        for (LivingEntity target : chain) {
            points.add(target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0));
        }
        spawnChain(world, points, settings);
        playChainStartSounds(world, visualSource, firstTarget);
        return damaged;
    }

    private static List<LivingEntity> buildChain(ServerWorld world, LivingEntity player, LivingEntity firstTarget, int chainCount, double range) {
        List<LivingEntity> chain = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        LivingEntity current = firstTarget;
        for (int i = 0; i < chainCount && current != null; i++) {
            chain.add(current);
            visited.add(current.getUuid());
            current = findNextTarget(world, player, current, visited, range);
        }
        return chain;
    }

    private static LivingEntity findNextTarget(ServerWorld world, LivingEntity player, LivingEntity current, Set<UUID> visited, double range) {
        Box box = current.getBoundingBox().expand(range, range * 0.5, range);
        return world.getEntitiesByClass(LivingEntity.class, box, target ->
                        target != player
                                && target.isAlive()
                                && !visited.contains(target.getUuid())
                                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                                && HelperMethods.checkFriendlyFire(target, player))
                .stream()
                .min(Comparator.comparingDouble(target -> target.squaredDistanceTo(current)))
                .orElse(null);
    }

    private static void spawnImpactEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 10, 0.22, 0.2, 0.22, 0.08);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 5, 0.16, 0.16, 0.16, 0.03);
    }

    private static void playChainStartSounds(ServerWorld world, LivingEntity source, LivingEntity firstTarget) {
        world.playSound(null, source.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_01.get(), SoundCategory.PLAYERS, 0.35F, 1.35F + world.random.nextFloat() * 0.25F);
        world.playSound(null, firstTarget.getBlockPos(), SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_01.get(), SoundCategory.PLAYERS, 0.6F, 0.9F + world.random.nextFloat() * 0.2F);
    }

    private static void playTargetCrackle(ServerWorld world, LivingEntity target, int hitIndex) {
        float volume = hitIndex == 1 ? 0.34F : 0.22F;
        float pitch = 1.15F + world.random.nextFloat() * 0.45F + Math.min(0.2F, hitIndex * 0.03F);
        world.playSound(null, target.getBlockPos(), randomThunderAttack(world), SoundCategory.PLAYERS, volume, pitch);
    }

    private static SoundEvent randomThunderAttack(ServerWorld world) {
        return switch (world.random.nextInt(3)) {
            case 0 -> SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_01.get();
            case 1 -> SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get();
            default -> SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_03.get();
        };
    }

    public record LightningVisualSettings(int color, int lifetime, float thickness, int branches) {
    }
}
