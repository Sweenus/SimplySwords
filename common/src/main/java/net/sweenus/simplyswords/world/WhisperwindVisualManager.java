package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.WhisperwindSlashVisualEntity;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class WhisperwindVisualManager {

    private static final String SLASH_VISUAL_TAG = "simplyswords_whisperwind_slash_visual";
    private static final int SLASH_LIFETIME = 5;
    private static final Map<ServerWorld, Map<UUID, ActiveDash>> ACTIVE_DASHES = new HashMap<>();
    private static final Map<ServerWorld, Set<PendingStrike>> PENDING_STRIKES = new HashMap<>();

    private WhisperwindVisualManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveDash> dashes = ACTIVE_DASHES.get(world);
        Set<PendingStrike> strikes = PENDING_STRIKES.get(world);
        return (dashes != null && !dashes.isEmpty()) || (strikes != null && !strikes.isEmpty()) || (world.getTime() % 80L == 0L);
    }

    public static void startDash(ServerWorld world, LivingEntity user, ItemStack stack) {
        if (world == null || user == null) {
            return;
        }

        ACTIVE_DASHES.computeIfAbsent(world, ignored -> new HashMap<>())
                .put(user.getUuid(), new ActiveDash(user.getPos(), user.getPos(), stack.copy()));
    }

    public static void recordDashTick(ServerWorld world, LivingEntity user, Iterable<? extends Entity> entities) {
        Map<UUID, ActiveDash> dashes = ACTIVE_DASHES.get(world);
        if (dashes == null || user == null) {
            return;
        }

        ActiveDash dash = dashes.get(user.getUuid());
        if (dash == null) {
            dash = new ActiveDash(user.getPos(), user.getPos(), user.getMainHandStack().copy());
            dashes.put(user.getUuid(), dash);
        }
        dash.end = user.getPos();

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity target && target.isAlive() && EntityPredicates.VALID_LIVING_ENTITY.test(target) && HelperMethods.checkFriendlyFire(target, user)) {
                dash.targets.add(target.getUuid());
            }
        }
    }

    public static void finishDash(ServerWorld world, LivingEntity user) {
        Map<UUID, ActiveDash> dashes = ACTIVE_DASHES.get(world);
        if (dashes == null || user == null) {
            return;
        }

        ActiveDash dash = dashes.remove(user.getUuid());
        if (dash == null) {
            return;
        }
        dash.end = user.getPos();
        world.playSound(null, dash.end.x, dash.end.y, dash.end.z,
                SoundRegistry.SWING_SMALL.get(),
                SoundCategory.PLAYERS, 0.45F, 1.65F + world.random.nextFloat() * 0.12F);
        if (!dash.targets.isEmpty()) {
            long triggerTick = world.getTime() + Config.uniqueEffects.whisperwind.delayedDamageDelay;
            PENDING_STRIKES.computeIfAbsent(world, ignored -> new HashSet<>())
                    .add(new PendingStrike(user.getUuid(), dash.stack.copy(), dash.start, dash.end, new HashSet<>(dash.targets), triggerTick));
        }
        if (dashes.isEmpty()) {
            ACTIVE_DASHES.remove(world);
        }
    }

    public static void scheduleTargetStrike(ServerWorld world, LivingEntity user, LivingEntity target, ItemStack stack) {
        if (world == null || user == null || target == null || !user.isAlive()
                || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, user)) {
            return;
        }
        Vec3d start = user.getPos();
        Vec3d end = target.getPos();
        Set<UUID> targets = new HashSet<>();
        targets.add(target.getUuid());
        PENDING_STRIKES.computeIfAbsent(world, ignored -> new HashSet<>())
                .add(new PendingStrike(user.getUuid(), stack.copy(), start, end, targets,
                        world.getTime() + Config.uniqueEffects.whisperwind.delayedDamageDelay));
        world.playSound(null, start.x, start.y, start.z, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_01.get(),
                SoundCategory.PLAYERS, 0.6F, 1.0F);
    }

    public static void tick(ServerWorld world) {
        Set<PendingStrike> strikes = PENDING_STRIKES.get(world);
        if (strikes != null && !strikes.isEmpty()) {
            Iterator<PendingStrike> iterator = strikes.iterator();
            while (iterator.hasNext()) {
                PendingStrike strike = iterator.next();
                if (world.getTime() < strike.triggerTick) {
                    spawnDelayCue(world, strike);
                    continue;
                }
                applyStrike(world, strike);
                iterator.remove();
            }
            if (strikes.isEmpty()) {
                PENDING_STRIKES.remove(world);
            }
        }

        if (world.getTime() % 80L == 0L) {
            purgeOrphanSlashes(world);
        }
    }

    private static void applyStrike(ServerWorld world, PendingStrike strike) {
        Entity sourceEntity = world.getEntity(strike.sourceId);
        if (!(sourceEntity instanceof LivingEntity source) || !source.isAlive()) {
            return;
        }

        float damage = HelperMethods.abilityScaledDamage("evocation", source, strike.stack,
                Config.uniqueEffects.whisperwind.delayedDamageScaling
                        + strike.targetIds.size() * Config.uniqueEffects.whisperwind.delayedDamagePerTargetScaling,
                Config.uniqueEffects.whisperwind.delayedSpellScaling
                        + strike.targetIds.size() * Config.uniqueEffects.whisperwind.delayedSpellPerTargetScaling);
        for (UUID targetId : strike.targetIds) {
            Entity entity = world.getEntity(targetId);
            if (!(entity instanceof LivingEntity target) || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, source)) {
                continue;
            }

            target.timeUntilRegen = 0;
            var damageSource = world.getDamageSources().indirectMagic(source, source);
            target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, strike.stack, target, damageSource, damage));
            spawnBlossoms(world, target);
        }

        world.playSound(null, strike.end.x, strike.end.y, strike.end.z,
                SoundRegistry.ELEMENTAL_SWORD_WIND_ATTACK_03.get(),
                SoundCategory.PLAYERS, 0.75F, 1.25F + world.random.nextFloat() * 0.18F);
        spawnSlash(world, strike.start, strike.end);
    }

    private static void spawnDelayCue(ServerWorld world, PendingStrike strike) {
        if (!Config.general.enableModernFieldEffects || world.getTime() % 4L != 0L) {
            return;
        }

        for (UUID targetId : strike.targetIds) {
            Entity entity = world.getEntity(targetId);
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }

            Vec3d pos = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.62), 0.0);
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 1, 0.16, 0.02, 0.16, 0.0);
            if (world.random.nextBoolean()) {
                world.spawnParticles(ParticleTypes.CHERRY_LEAVES, pos.x, pos.y + 0.05, pos.z, 1, 0.22, 0.08, 0.22, 0.01);
            }
        }
    }

    private static void spawnBlossoms(ServerWorld world, LivingEntity target) {
        if (!Config.general.enableModernFieldEffects) {
            return;
        }

        Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.45), 0.0);
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, pos.x, pos.y + 0.1, pos.z, 28, 0.55, 0.75, 0.55, 0.08);
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, pos.x, target.getY() + 0.08, pos.z, 16, 0.8, 0.05, 0.8, 0.015);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.35, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, target.getY() + 0.05, pos.z, 8, 0.45, 0.04, 0.45, 0.01);
    }

    private static void spawnSlash(ServerWorld world, Vec3d start, Vec3d end) {
        if (!Config.general.enableModernFieldEffects || start == null || end == null || start.squaredDistanceTo(end) < 0.04) {
            return;
        }

        WhisperwindSlashVisualEntity slash = new WhisperwindSlashVisualEntity(
                world,
                start.x,
                start.y,
                start.z,
                end.x - start.x,
                end.y - start.y,
                end.z - start.z,
                SLASH_LIFETIME
        );
        slash.addCommandTag(SLASH_VISUAL_TAG);
        world.spawnEntity(slash);
    }

    private static void purgeOrphanSlashes(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof WhisperwindSlashVisualEntity && entity.getCommandTags().contains(SLASH_VISUAL_TAG) && entity.age > SLASH_LIFETIME + 4) {
                entity.discard();
            }
        }
    }

    private static final class ActiveDash {
        private final Vec3d start;
        private final Set<UUID> targets = new HashSet<>();
        private final ItemStack stack;
        private Vec3d end;

        private ActiveDash(Vec3d start, Vec3d end, ItemStack stack) {
            this.start = start;
            this.end = end;
            this.stack = stack;
        }
    }

    private record PendingStrike(UUID sourceId, ItemStack stack, Vec3d start, Vec3d end, Set<UUID> targetIds, long triggerTick) {
    }
}
