package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.MagispearFallingSpearVisualEntity;
import net.sweenus.simplyswords.entity.MagispearFirmamentVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MagispearAbilityManager {

    private static final int LAUNCH_END_TICK = 6;
    private static final int RAIN_START_TICK = 6;
    private static final int RAIN_SPAWN_SPAN = 12;
    private static final int RAIN_FALL_TICKS = 5;
    private static final int LEAP_START_TICK = 20;
    private static final int PLUNGE_START_TICK = 28;
    private static final int IMPACT_TICK = 36;
    private static final int FIELD_VISUAL_LIFETIME = 46;
    private static final double SKY_HEIGHT = 12.0;
    private static final int GROUND_SCAN_UP = 8;
    private static final int GROUND_SCAN_DOWN = 32;
    private static final String FIELD_VISUAL_TAG = "simplyswords_magispear_firmament_visual";
    private static final String SPEAR_VISUAL_TAG = "simplyswords_magispear_spear_visual";

    private static final Map<ServerWorld, Map<UUID, ActiveMagislam>> ACTIVE = new HashMap<>();

    private MagispearAbilityManager() {
    }

    public static boolean canStart(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().isOf(ItemsRegistry.MAGISPEAR.get())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !isActive(context.actor())
                && resolveTargetPosition(context) != null;
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canStart(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        Vec3d center = resolveTargetPosition(context);
        if (center == null) {
            return false;
        }

        int waveCount = MathHelper.clamp(Config.uniqueEffects.magispear.rainWaveCount, 1, 8);
        float radius = (float) Math.max(1.0, Config.uniqueEffects.magispear.radius);
        ActiveMagislam magislam = new ActiveMagislam(
                actor.getUuid(), context.stack().copy(), center, world.getTime(), waveCount,
                new boolean[waveCount], new boolean[waveCount]
        );

        if (Config.general.enableModernFieldEffects) {
            MagispearFirmamentVisualEntity field = new MagispearFirmamentVisualEntity(
                    world, center.x, center.y, center.z, radius, waveCount, FIELD_VISUAL_LIFETIME);
            field.addCommandTag(FIELD_VISUAL_TAG);
            world.spawnEntity(field);
            magislam.fieldVisualId = field.getUuid();

            Vec3d launchStart = actor.getEyePos().add(actor.getRotationVec(1.0F).multiply(0.55));
            Vec3d launchEnd = center.add(0.0, SKY_HEIGHT, 0.0);
            spawnSpearVisual(world, magislam, launchStart, launchEnd, LAUNCH_END_TICK,
                    MagispearFallingSpearVisualEntity.MODE_LAUNCH, 1.0F);
        }

        ACTIVE.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), magislam);
        spawnActivationEffects(world, actor, center);
        return true;
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        return active != null && !active.isEmpty();
    }

    public static boolean isActive(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        return active != null && active.containsKey(actor.getUuid());
    }

    public static boolean blocksIncomingDamage(LivingEntity actor, DamageSource source) {
        if (actor == null || source == null || actor.getWorld().isClient()
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(actor.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        ActiveMagislam magislam = active == null ? null : active.get(actor.getUuid());
        if (magislam == null) {
            return false;
        }
        long age = world.getTime() - magislam.startedAt;
        return age >= LEAP_START_TICK && age <= IMPACT_TICK;
    }

    public static void tick(ServerWorld world) {
        Map<UUID, ActiveMagislam> active = ACTIVE.get(world);
        if (active == null || active.isEmpty()) {
            return;
        }

        Iterator<ActiveMagislam> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            ActiveMagislam magislam = iterator.next();
            Entity ownerEntity = world.getEntity(magislam.ownerId);
            if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) {
                cancel(world, magislam);
                iterator.remove();
                continue;
            }

            if (tickMagislam(world, owner, magislam)) {
                iterator.remove();
            }
        }

        if (active.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static boolean tickMagislam(ServerWorld world, LivingEntity owner, ActiveMagislam magislam) {
        long age = world.getTime() - magislam.startedAt;
        tickRainWaves(world, owner, magislam, age);

        if (age >= LEAP_START_TICK && age < PLUNGE_START_TICK) {
            guideOwner(owner, magislam.center.add(0.0,
                    Math.max(1.0, Config.uniqueEffects.magispear.diveHeight), 0.0),
                    (int) Math.max(1L, PLUNGE_START_TICK - age));
            magislam.movementObstructed |= owner.horizontalCollision;
        } else if (age >= PLUNGE_START_TICK && age < IMPACT_TICK) {
            if (!magislam.finalSpearSpawned) {
                magislam.finalSpearSpawned = true;
                if (Config.general.enableModernFieldEffects) {
                    Vec3d start = magislam.center.add(0.0, SKY_HEIGHT + 2.0, 0.0);
                    spawnSpearVisual(world, magislam, start, magislam.center,
                            IMPACT_TICK - PLUNGE_START_TICK,
                            MagispearFallingSpearVisualEntity.MODE_FINAL, 1.0F);
                }
                world.playSound(null, magislam.center.x, magislam.center.y + 4.0, magislam.center.z,
                        SoundRegistry.MAGIC_SHAMANIC_NORDIC_27.get(), SoundCategory.PLAYERS, 0.75F, 0.55F);
            }

            guideOwner(owner, magislam.center.add(0.0, 0.08, 0.0),
                    (int) Math.max(1L, IMPACT_TICK - age));
            magislam.movementObstructed |= owner.horizontalCollision;
            if (age > PLUNGE_START_TICK + 1L && (owner.horizontalCollision || owner.isOnGround())) {
                Vec3d impact = getGroundPosition(world, owner.getPos());
                finish(world, owner, magislam, impact);
                return true;
            }
        }

        if (age >= IMPACT_TICK) {
            Vec3d impact = magislam.movementObstructed
                    || owner.getPos().squaredDistanceTo(magislam.center) > 9.0
                    ? getGroundPosition(world, owner.getPos())
                    : magislam.center;
            finish(world, owner, magislam, impact);
            return true;
        }
        return false;
    }

    private static void tickRainWaves(ServerWorld world, LivingEntity owner,
                                      ActiveMagislam magislam, long age) {
        for (int wave = 0; wave < magislam.waveCount; wave++) {
            int spawnTick = getWaveSpawnTick(wave, magislam.waveCount);
            if (age >= spawnTick && !magislam.waveSpawned[wave]) {
                magislam.waveSpawned[wave] = true;
                spawnRainWave(world, magislam, wave);
            }
            if (age >= spawnTick + RAIN_FALL_TICKS && !magislam.waveImpacted[wave]) {
                magislam.waveImpacted[wave] = true;
                impactRainWave(world, owner, magislam, wave);
            }
        }
    }

    private static int getWaveSpawnTick(int wave, int waveCount) {
        if (waveCount <= 1) {
            return RAIN_START_TICK + RAIN_SPAWN_SPAN / 2;
        }
        return RAIN_START_TICK + Math.round((float) wave * RAIN_SPAWN_SPAN / (waveCount - 1));
    }

    private static void spawnRainWave(ServerWorld world, ActiveMagislam magislam, int wave) {
        Vec3d first = getStrikePosition(world, magislam, wave, false);
        Vec3d opposite = getStrikePosition(world, magislam, wave, true);
        if (Config.general.enableModernFieldEffects) {
            spawnSpearVisual(world, magislam, first.add(0.0, SKY_HEIGHT, 0.0), first,
                    RAIN_FALL_TICKS, MagispearFallingSpearVisualEntity.MODE_RAIN, 1.0F);
            spawnSpearVisual(world, magislam, opposite.add(0.0, SKY_HEIGHT, 0.0), opposite,
                    RAIN_FALL_TICKS, MagispearFallingSpearVisualEntity.MODE_RAIN, 1.0F);
        }
        spawnTelegraphParticles(world, first);
        spawnTelegraphParticles(world, opposite);
        world.playSound(null, magislam.center.x, magislam.center.y + 6.0, magislam.center.z,
                SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.35F, 1.35F + wave * 0.08F);
    }

    private static void impactRainWave(ServerWorld world, LivingEntity owner,
                                       ActiveMagislam magislam, int wave) {
        Vec3d first = getStrikePosition(world, magislam, wave, false);
        Vec3d opposite = getStrikePosition(world, magislam, wave, true);
        double splashRadius = Math.max(0.25, Config.uniqueEffects.magispear.rainSplashRadius);
        Set<UUID> hitThisWave = new HashSet<>();
        float waveScaling = Config.uniqueEffects.magispear.throwDamageScaling / Math.max(1, magislam.waveCount);
        float baseDamage = HelperMethods.attackScaledDamage(owner, magislam.stack, waveScaling);
        damageRainArea(world, owner, magislam, first, splashRadius, baseDamage, hitThisWave);
        damageRainArea(world, owner, magislam, opposite, splashRadius, baseDamage, hitThisWave);
        spawnRainImpactEffects(world, first);
        spawnRainImpactEffects(world, opposite);
    }

    private static void damageRainArea(ServerWorld world, LivingEntity owner, ActiveMagislam magislam,
                                       Vec3d impact, double radius, float baseDamage, Set<UUID> hitThisWave) {
        Box box = new Box(impact.x - radius, impact.y - 1.5, impact.z - radius,
                impact.x + radius, impact.y + 3.0, impact.z + radius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity != owner
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && horizontalSquaredDistance(entity.getPos(), impact) <= radius * radius
                        && hitThisWave.add(entity.getUuid()))) {
            if (damageTarget(world, owner, magislam.stack, target, baseDamage)) {
                pullTowardCenter(target, magislam.center,
                        Math.max(0.0, Config.uniqueEffects.magispear.inwardPushStrength));
            }
        }
    }

    private static void finish(ServerWorld world, LivingEntity owner,
                               ActiveMagislam magislam, Vec3d impact) {
        magislam.center = impact;
        updateFieldVisual(world, magislam, impact);
        owner.setVelocity(0.0, 0.12, 0.0);
        owner.velocityModified = true;
        owner.fallDistance = 0.0F;

        double radius = Math.max(1.0, Config.uniqueEffects.magispear.radius);
        float baseDamage = HelperMethods.attackScaledDamage(
                owner, magislam.stack, Config.uniqueEffects.magispear.damageScaling);
        Box box = new Box(impact.x - radius, impact.y - 2.0, impact.z - radius,
                impact.x + radius, impact.y + radius, impact.z + radius);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity != owner
                        && entity.isAlive()
                        && EntityPredicates.VALID_LIVING_ENTITY.test(entity)
                        && HelperMethods.checkAbilityTarget(entity, owner)
                        && horizontalSquaredDistance(entity.getPos(), impact) <= radius * radius)) {
            if (damageTarget(world, owner, magislam.stack, target, baseDamage)) {
                knockFromImpact(target, impact);
            }
        }
        spawnFinalImpactEffects(world, impact, radius);
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity owner, net.minecraft.item.ItemStack stack,
                                        LivingEntity target, float baseDamage) {
        DamageSource source = owner.getDamageSources().indirectMagic(owner, owner);
        float damage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source, baseDamage);
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage));
        return damaged[0];
    }

    private static void pullTowardCenter(LivingEntity target, Vec3d center, double strength) {
        Vec3d direction = new Vec3d(center.x - target.getX(), 0.0, center.z - target.getZ());
        if (direction.lengthSquared() < 0.001 || strength <= 0.0) {
            return;
        }
        Vec3d pull = direction.normalize().multiply(strength);
        Vec3d current = target.getVelocity();
        target.setVelocity(current.x * 0.35 + pull.x, Math.max(current.y, 0.08), current.z * 0.35 + pull.z);
        target.velocityModified = true;
    }

    private static void knockFromImpact(LivingEntity target, Vec3d impact) {
        Vec3d direction = new Vec3d(target.getX() - impact.x, 0.0, target.getZ() - impact.z);
        if (direction.lengthSquared() < 0.001) {
            direction = new Vec3d(0.0, 0.0, 1.0);
        }
        Vec3d knockback = direction.normalize().multiply(0.55);
        target.setVelocity(knockback.x, Math.max(target.getVelocity().y, 0.32), knockback.z);
        target.velocityModified = true;
    }

    private static void guideOwner(LivingEntity owner, Vec3d destination, int remainingTicks) {
        Vec3d delta = destination.subtract(owner.getPos());
        Vec3d velocity = delta.multiply(1.0 / Math.max(1, remainingTicks));
        double maxSpeed = 2.25;
        if (velocity.lengthSquared() > maxSpeed * maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }
        owner.setVelocity(velocity);
        owner.velocityModified = true;
        owner.fallDistance = 0.0F;
    }

    private static Vec3d getStrikePosition(ServerWorld world, ActiveMagislam magislam,
                                           int wave, boolean opposite) {
        double radius = Math.max(1.0, Config.uniqueEffects.magispear.radius) * 0.78;
        double angle = MathHelper.TAU * wave / (magislam.waveCount * 2.0)
                + (opposite ? Math.PI : 0.0);
        Vec3d rough = magislam.center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return getGroundPosition(world, rough);
    }

    private static Vec3d resolveTargetPosition(WeaponAbilityContext context) {
        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        double range = Math.max(1.0, Config.uniqueEffects.magispear.targetingRange);

        LivingEntity target = context.target();
        if (target != null && target.isAlive() && HelperMethods.checkAbilityTarget(target, actor)
                && actor.squaredDistanceTo(target) <= range * range) {
            return getGroundPosition(world, target.getPos());
        }
        if (!(actor instanceof net.minecraft.entity.player.PlayerEntity)) {
            return null;
        }

        Vec3d start = actor.getEyePos();
        Vec3d end = start.add(context.facing().normalize().multiply(range));
        HitResult result = world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, actor));
        Vec3d candidate = result.getType() == HitResult.Type.BLOCK ? result.getPos() : end;
        return findGroundPosition(world, candidate.x, candidate.z, candidate.y);
    }

    private static Vec3d getGroundPosition(ServerWorld world, Vec3d position) {
        Vec3d ground = findGroundPosition(world, position.x, position.z, position.y);
        return ground == null ? position : ground;
    }

    private static Vec3d findGroundPosition(ServerWorld world, double x, double z, double referenceY) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int startY = Math.min(world.getTopY() - 1, MathHelper.floor(referenceY) + GROUND_SCAN_UP);
        int minY = Math.max(world.getBottomY(), MathHelper.floor(referenceY) - GROUND_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return new Vec3d(x, y + 1.02, z);
            }
        }
        return null;
    }

    private static void spawnSpearVisual(ServerWorld world, ActiveMagislam magislam,
                                         Vec3d start, Vec3d end, int lifetime, int mode, float scale) {
        MagispearFallingSpearVisualEntity visual = new MagispearFallingSpearVisualEntity(
                world, start.x, start.y, start.z,
                end.x - start.x, end.y - start.y, end.z - start.z,
                lifetime, mode, scale);
        visual.addCommandTag(SPEAR_VISUAL_TAG);
        world.spawnEntity(visual);
        magislam.spearVisualIds.add(visual.getUuid());
    }

    private static void updateFieldVisual(ServerWorld world, ActiveMagislam magislam, Vec3d position) {
        Entity entity = magislam.fieldVisualId == null ? null : world.getEntity(magislam.fieldVisualId);
        if (entity instanceof MagispearFirmamentVisualEntity field) {
            field.setPosition(position.x, position.y, position.z);
        }
    }

    private static void cancel(ServerWorld world, ActiveMagislam magislam) {
        Entity field = magislam.fieldVisualId == null ? null : world.getEntity(magislam.fieldVisualId);
        if (field != null) {
            field.discard();
        }
        for (UUID visualId : magislam.spearVisualIds) {
            Entity visual = world.getEntity(visualId);
            if (visual != null) {
                visual.discard();
            }
        }
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor, Vec3d center) {
        world.spawnParticles(ParticleTypes.ENCHANT, actor.getX(), actor.getEyeY(), actor.getZ(),
                24, 0.45, 0.55, 0.45, 0.2);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.1, center.z,
                Config.general.enableModernFieldEffects ? 24 : 12, 0.65, 0.05, 0.65, 0.04);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_NORDIC_27.get(),
                actor.getSoundCategory(), 0.65F, 1.05F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.45F, 1.35F);
    }

    private static void spawnTelegraphParticles(ServerWorld world, Vec3d position) {
        world.spawnParticles(ParticleTypes.ENCHANT, position.x, position.y + 0.08, position.z,
                Config.general.enableModernFieldEffects ? 10 : 18, 0.28, 0.04, 0.28, 0.02);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.25, position.z,
                5, 0.2, 0.2, 0.2, 0.02);
    }

    private static void spawnRainImpactEffects(ServerWorld world, Vec3d position) {
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, position.x, position.y + 0.3, position.z,
                Config.general.enableModernFieldEffects ? 18 : 10, 0.42, 0.32, 0.42, 0.1);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.18, position.z,
                12, 0.35, 0.18, 0.35, 0.08);
        world.spawnParticles(ParticleTypes.GLOW, position.x, position.y + 0.25, position.z,
                7, 0.28, 0.24, 0.28, 0.04);
        world.playSound(null, position.x, position.y, position.z,
                SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.45F,
                1.25F + world.random.nextFloat() * 0.25F);
    }

    private static void spawnFinalImpactEffects(ServerWorld world, Vec3d impact, double radius) {
        int multiplier = Config.general.enableModernFieldEffects ? 2 : 1;
        world.spawnParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.45, impact.z,
                2, 0.15, 0.05, 0.15, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y + 0.45, impact.z,
                24 * multiplier, radius * 0.45, 0.4, radius * 0.45, 0.15);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, impact.x, impact.y + 0.35, impact.z,
                32 * multiplier, radius * 0.48, 0.25, radius * 0.48, 0.12);
        world.spawnParticles(ParticleTypes.GLOW, impact.x, impact.y + 0.55, impact.z,
                18 * multiplier, radius * 0.32, 0.45, radius * 0.32, 0.08);
        world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.85F, 0.7F);
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                SoundCategory.PLAYERS, 0.85F, 0.62F);
        world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS, 0.55F, 0.55F);
    }

    private static double horizontalSquaredDistance(Vec3d first, Vec3d second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    private static final class ActiveMagislam {
        private final UUID ownerId;
        private final net.minecraft.item.ItemStack stack;
        private Vec3d center;
        private final long startedAt;
        private final int waveCount;
        private final boolean[] waveSpawned;
        private final boolean[] waveImpacted;
        private final List<UUID> spearVisualIds = new ArrayList<>();
        private UUID fieldVisualId;
        private boolean finalSpearSpawned;
        private boolean movementObstructed;

        private ActiveMagislam(UUID ownerId, net.minecraft.item.ItemStack stack, Vec3d center,
                               long startedAt, int waveCount,
                               boolean[] waveSpawned, boolean[] waveImpacted) {
            this.ownerId = ownerId;
            this.stack = stack;
            this.center = center;
            this.startedAt = startedAt;
            this.waveCount = waveCount;
            this.waveSpawned = waveSpawned;
            this.waveImpacted = waveImpacted;
        }
    }
}
