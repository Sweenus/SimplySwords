package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MjolnirStormManager {

    private static final ChainLightningVisualManager.LightningVisualSettings SKY_BOLT_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0x65DFFF, 10, 0.075F, 6);
    private static final ChainLightningVisualManager.LightningVisualSettings CONDUCTIVE_INDICATOR_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0x8CEBFF, 3, 0.025F, 1);
    private static final ChainLightningVisualManager.LightningVisualSettings THUNDERCLAP_SETTINGS =
            new ChainLightningVisualManager.LightningVisualSettings(0xB6F4FF, 8, 0.055F, 4);
    private static final int AURA_INTERVAL = 5;
    private static final int LOCAL_RING_LIFETIME = 4;
    private static final int FINAL_RING_LIFETIME = 7;
    private static final Map<ServerWorld, Map<UUID, ActiveStorm>> ACTIVE_STORMS = new HashMap<>();
    private static final Map<ServerWorld, List<ThunderclapRing>> ACTIVE_RINGS = new HashMap<>();

    private MjolnirStormManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        List<ThunderclapRing> rings = ACTIVE_RINGS.get(world);
        return (storms != null && !storms.isEmpty()) || (rings != null && !rings.isEmpty());
    }

    public static boolean canActivate(WeaponAbilityContext context) {
        if (context == null
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.stack() == null
                || context.stack().isEmpty()
                || !context.stack().isOf(ItemsRegistry.MJOLNIR.get())
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || isActive(context.world(), context.actor())) {
            return false;
        }

        if (context.actor() instanceof PlayerEntity) {
            return true;
        }

        LivingEntity target = context.target();
        return isValidTarget(context.world(), context.actor(), context.sourcePlayer(), target);
    }

    public static boolean start(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }

        ServerWorld world = context.world();
        LivingEntity actor = context.actor();
        long now = world.getTime();
        int duration = Math.max(0, Config.uniqueEffects.mjolnir.duration);
        float boltDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                context.stack(),
                Config.uniqueEffects.mjolnir.damageScaling,
                Config.uniqueEffects.mjolnir.spellScaling
        );
        float conductiveBurstDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                context.stack(),
                Config.uniqueEffects.mjolnir.conductiveBurstDamageScaling,
                Config.uniqueEffects.mjolnir.conductiveBurstSpellScaling
        );
        float finalThunderclapDamage = HelperMethods.abilityScaledDamage(
                "lightning",
                actor,
                context.stack(),
                Config.uniqueEffects.mjolnir.finalThunderclapDamageScaling,
                Config.uniqueEffects.mjolnir.finalThunderclapSpellScaling
        );

        ActiveStorm storm = new ActiveStorm(
                actor.getUuid(),
                context.sourcePlayer() == null ? null : context.sourcePlayer().getUuid(),
                context.stack().copy(),
                now,
                now,
                now + duration,
                boltDamage,
                conductiveBurstDamage,
                finalThunderclapDamage
        );
        ACTIVE_STORMS.computeIfAbsent(world, ignored -> new HashMap<>()).put(actor.getUuid(), storm);
        spawnActivationEffects(world, actor);
        return true;
    }

    public static void onMeleeHit(ServerWorld world, ItemStack stack,
                                  LivingEntity reportedAttacker, LivingEntity target) {
        if (world == null
                || stack == null
                || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.MJOLNIR.get())
                || reportedAttacker == null
                || target == null
                || !target.isAlive()
                || target.getWorld() != world) {
            return;
        }

        DelegatedWeaponHitContext delegated = SimplySwordsAPI.getDelegatedWeaponHitContext();
        LivingEntity actor = delegated == null ? reportedAttacker : delegated.actor();
        LivingEntity sourceOwner = delegated == null ? null : delegated.owner();
        if (!isValidTarget(world, actor, sourceOwner, target)) {
            return;
        }

        target.addStatusEffect(
                new StatusEffectInstance(
                        EffectRegistry.getReference(EffectRegistry.STORM),
                        Math.max(1, Config.uniqueEffects.mjolnir.conductiveDuration),
                        0,
                        false,
                        false,
                        true
                ),
                actor
        );
    }

    public static void tick(ServerWorld world) {
        tickStorms(world);
        tickRings(world);
    }

    public static void spawnConductiveIndicator(ServerWorld world, LivingEntity target) {
        if (world == null || target == null || !target.isAlive()) {
            return;
        }

        double radius = net.minecraft.util.math.MathHelper.clamp(Math.max(0.25, target.getWidth() * 0.55), 0.25, 0.8);
        double startAngle = world.random.nextDouble() * Math.PI * 2.0;
        spawnConductiveJump(world, target, radius, startAngle);
        if (world.random.nextInt(4) == 0) {
            spawnConductiveJump(world, target, radius, startAngle + Math.PI);
        }
    }

    private static void tickStorms(ServerWorld world) {
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveStorm> iterator = storms.values().iterator();
        while (iterator.hasNext()) {
            ActiveStorm storm = iterator.next();
            LivingEntity actor = resolveLiving(world, storm.actorId);
            LivingEntity sourceOwner = resolveLiving(world, storm.sourceOwnerId);
            if (actor == null || (storm.sourceOwnerId != null && sourceOwner == null)) {
                iterator.remove();
                continue;
            }

            if ((now - storm.startedAt) % AURA_INTERVAL == 0L) {
                spawnAuraEffects(world, actor);
            }

            if (!storm.finishing && now >= storm.expiresAt) {
                storm.finishing = true;
                storm.nextFinalBoltAt = now;
            }

            if (!storm.finishing) {
                if (now >= storm.nextPulseAt) {
                    strikePulse(world, actor, sourceOwner, storm);
                    storm.nextPulseAt = now + Math.max(1, Config.uniqueEffects.mjolnir.frequency);
                }
                continue;
            }

            int finalBoltCount = Math.max(0, Config.uniqueEffects.mjolnir.finalBoltCount);
            if (storm.finalBoltsReleased < finalBoltCount && now >= storm.nextFinalBoltAt) {
                strikePulse(world, actor, sourceOwner, storm);
                storm.finalBoltsReleased++;
                storm.nextFinalBoltAt = now + Math.max(1, Config.uniqueEffects.mjolnir.finalBoltInterval);
            }
            if (storm.finalBoltsReleased >= finalBoltCount) {
                releaseFinalThunderclap(world, actor, sourceOwner, storm);
                iterator.remove();
            }
        }

        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static void tickRings(ServerWorld world) {
        List<ThunderclapRing> rings = ACTIVE_RINGS.get(world);
        if (rings == null || rings.isEmpty()) {
            return;
        }

        Iterator<ThunderclapRing> iterator = rings.iterator();
        while (iterator.hasNext()) {
            ThunderclapRing ring = iterator.next();
            double progress = (ring.age + 1.0) / ring.lifetime;
            double radius = ring.radius * progress;
            int points = Math.max(16, (int) Math.ceil(radius * 11.0));
            double rotation = ring.age * 0.24;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points + rotation;
                double x = ring.center.x + Math.cos(angle) * radius;
                double z = ring.center.z + Math.sin(angle) * radius;
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, ring.center.y + 0.12, z,
                        1, 0.015, 0.025, 0.015, ring.finalRing ? 0.025 : 0.015);
                if ((i + ring.age) % (ring.finalRing ? 2 : 4) == 0) {
                    world.spawnParticles(ParticleTypes.CLOUD, x, ring.center.y + 0.04, z,
                            1, 0.025, 0.015, 0.025, 0.012);
                }
            }
            ring.age++;
            if (ring.age >= ring.lifetime) {
                iterator.remove();
            }
        }

        if (rings.isEmpty()) {
            ACTIVE_RINGS.remove(world);
        }
    }

    private static void strikePulse(ServerWorld world, LivingEntity actor,
                                    LivingEntity sourceOwner, ActiveStorm storm) {
        LivingEntity target = selectTarget(world, actor, sourceOwner, storm);
        if (target == null) {
            spawnAmbientBolt(world, actor);
            return;
        }

        storm.struckThisCycle.add(target.getUuid());
        boolean conductive = target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM));
        Vec3d impact = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0);
        spawnSkyBolt(world, impact);
        spawnBoltImpactEffects(world, target);

        if (conductive) {
            target.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM));
        }

        damageTarget(world, actor, storm.stack, target, storm.boltDamage);
        if (conductive) {
            releaseConductiveBurst(world, actor, sourceOwner, storm, impact);
        }
    }

    private static LivingEntity selectTarget(ServerWorld world, LivingEntity actor,
                                             LivingEntity sourceOwner, ActiveStorm storm) {
        double radius = Math.max(0.5, Config.uniqueEffects.mjolnir.radius);
        double verticalRadius = Math.max(1.0, radius * 0.5);
        Box box = actor.getBoundingBox().expand(radius, verticalRadius, radius);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(world, actor, sourceOwner, target)
                        && isInsideCylinder(target, actor.getPos(), radius, verticalRadius)
        );
        if (targets.isEmpty()) {
            storm.struckThisCycle.clear();
            return null;
        }

        targets.sort(Comparator.comparingDouble(actor::squaredDistanceTo));
        LivingEntity target = firstEligible(targets, storm.struckThisCycle, true);
        if (target == null) {
            target = firstEligible(targets, storm.struckThisCycle, false);
        }
        if (target == null) {
            storm.struckThisCycle.clear();
            target = firstEligible(targets, storm.struckThisCycle, true);
            if (target == null) {
                target = firstEligible(targets, storm.struckThisCycle, false);
            }
        }
        return target;
    }

    private static LivingEntity firstEligible(List<LivingEntity> targets, Set<UUID> struck,
                                              boolean requireConductive) {
        for (LivingEntity target : targets) {
            if (!struck.contains(target.getUuid())
                    && (!requireConductive
                    || target.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.STORM)))) {
                return target;
            }
        }
        return null;
    }

    private static void releaseConductiveBurst(ServerWorld world, LivingEntity actor,
                                               LivingEntity sourceOwner, ActiveStorm storm,
                                               Vec3d center) {
        double radius = Math.max(0.1, Config.uniqueEffects.mjolnir.conductiveBurstRadius);
        damageArea(
                world,
                actor,
                sourceOwner,
                storm.stack,
                center,
                radius,
                storm.conductiveBurstDamage,
                Math.max(0.0, Config.uniqueEffects.mjolnir.conductiveBurstKnockback),
                Math.max(0.0, Config.uniqueEffects.mjolnir.conductiveBurstKnockUp)
        );
        addRing(world, new Vec3d(center.x, center.y - 0.35, center.z),
                radius, LOCAL_RING_LIFETIME, false);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                28, 0.5, 0.32, 0.5, 0.12);
        world.playSound(null, center.x, center.y, center.z,
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_02.get(),
                SoundCategory.PLAYERS, 0.62F, 0.88F + world.random.nextFloat() * 0.16F);
    }

    private static void releaseFinalThunderclap(ServerWorld world, LivingEntity actor,
                                                LivingEntity sourceOwner, ActiveStorm storm) {
        Vec3d center = actor.getPos();
        double radius = Math.max(0.1, Config.uniqueEffects.mjolnir.finalThunderclapRadius);
        damageArea(
                world,
                actor,
                sourceOwner,
                storm.stack,
                center,
                radius,
                storm.finalThunderclapDamage,
                Math.max(0.0, Config.uniqueEffects.mjolnir.finalThunderclapKnockback),
                Math.max(0.0, Config.uniqueEffects.mjolnir.finalThunderclapKnockUp)
        );
        addRing(world, center, radius, FINAL_RING_LIFETIME, true);
        spawnThunderclapSpokes(world, center, radius);
        world.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3, center.z,
                2, 0.15, 0.1, 0.15, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.35, center.z,
                72, 0.85, 0.45, 0.85, 0.18);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.08, center.z,
                38, 1.1, 0.12, 1.1, 0.11);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_03.get(),
                actor.getSoundCategory(), 1.0F, 0.72F);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_03.get(),
                actor.getSoundCategory(), 0.9F, 0.68F);
    }

    private static void damageArea(ServerWorld world, LivingEntity actor,
                                   LivingEntity sourceOwner, ItemStack stack,
                                   Vec3d center, double radius, float damage,
                                   double knockback, double knockUp) {
        double verticalRadius = Math.max(1.0, radius * 0.65);
        Box box = new Box(
                center.x - radius,
                center.y - verticalRadius,
                center.z - radius,
                center.x + radius,
                center.y + verticalRadius,
                center.z + radius
        );
        Vec3d fallbackDirection = horizontalDirection(actor.getRotationVec(1.0F), actor);
        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(world, actor, sourceOwner, target)
                        && isInsideCylinder(target, center, radius, verticalRadius)
        )) {
            if (damageTarget(world, actor, stack, target, damage)) {
                knockAway(target, center, fallbackDirection, knockback, knockUp);
            }
        }
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity actor,
                                        ItemStack stack, LivingEntity target, float baseDamage) {
        DamageSource source = actor.getDamageSources().indirectMagic(actor, actor);
        float damage = HelperMethods.applyAbilityDamageEnchantments(
                world,
                stack,
                target,
                source,
                Math.max(0.0F, baseDamage)
        );
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(
                () -> damaged[0] = HelperMethods.damageThroughIframes(target, source, damage)
        );
        return damaged[0];
    }

    private static boolean isInsideCylinder(LivingEntity target, Vec3d center,
                                            double radius, double verticalRadius) {
        Vec3d targetPos = target.getPos();
        double targetRadius = Math.max(0.1, target.getWidth() * 0.5);
        double horizontalRadius = radius + targetRadius;
        double dx = targetPos.x - center.x;
        double dz = targetPos.z - center.z;
        return dx * dx + dz * dz <= horizontalRadius * horizontalRadius
                && target.getBoundingBox().maxY >= center.y - verticalRadius
                && target.getBoundingBox().minY <= center.y + verticalRadius;
    }

    private static boolean isValidTarget(ServerWorld world, LivingEntity actor,
                                         LivingEntity sourceOwner, LivingEntity target) {
        return actor != null
                && actor.isAlive()
                && actor.getWorld() == world
                && target != null
                && target.isAlive()
                && target != actor
                && target != sourceOwner
                && target.getWorld() == world
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor)
                && (sourceOwner == null || HelperMethods.checkAbilityTarget(target, sourceOwner));
    }

    private static boolean isActive(ServerWorld world, LivingEntity actor) {
        Map<UUID, ActiveStorm> storms = ACTIVE_STORMS.get(world);
        return storms != null && storms.containsKey(actor.getUuid());
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? living
                : null;
    }

    private static void spawnSkyBolt(ServerWorld world, Vec3d impact) {
        double height = Math.max(1.0, Config.uniqueEffects.mjolnir.skyHeight);
        Vec3d start = impact.add(
                (world.random.nextDouble() - 0.5) * 1.2,
                height,
                (world.random.nextDouble() - 0.5) * 1.2
        );
        ChainLightningVisualManager.spawnBolt(world, start, impact, SKY_BOLT_SETTINGS);
    }

    private static void spawnAmbientBolt(ServerWorld world, LivingEntity actor) {
        double radius = Math.max(0.5, Config.uniqueEffects.mjolnir.radius);
        double angle = world.random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(world.random.nextDouble()) * radius;
        double x = actor.getX() + Math.cos(angle) * distance;
        double z = actor.getZ() + Math.sin(angle) * distance;
        double y = findLocalGroundY(world, x, z, actor.getY());
        Vec3d impact = new Vec3d(x, y + 0.12, z);
        spawnSkyBolt(world, impact);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z,
                7, 0.18, 0.08, 0.18, 0.05);
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundRegistry.ELEMENTAL_SWORD_THUNDER_ATTACK_01.get(),
                SoundCategory.PLAYERS, 0.22F, 1.35F + world.random.nextFloat() * 0.25F);
    }

    private static double findLocalGroundY(ServerWorld world, double x, double z, double referenceY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(referenceY) + 6;
        int minimumY = Math.max(world.getBottomY(), (int) Math.floor(referenceY) - 12);
        for (int y = startY; y >= minimumY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }
        return referenceY;
    }

    private static void spawnConductiveJump(ServerWorld world, LivingEntity target,
                                            double radius, double startAngle) {
        double height = Math.max(0.35, target.getHeight());
        double endAngle = startAngle
                + (0.65 + world.random.nextDouble() * 1.05)
                * (world.random.nextBoolean() ? 1.0 : -1.0);
        double startY = target.getY() + height * (0.2 + world.random.nextDouble() * 0.65);
        double endY = target.getY() + height * (0.2 + world.random.nextDouble() * 0.65);
        Vec3d start = new Vec3d(
                target.getX() + Math.cos(startAngle) * radius,
                startY,
                target.getZ() + Math.sin(startAngle) * radius
        );
        Vec3d end = new Vec3d(
                target.getX() + Math.cos(endAngle) * radius,
                endY,
                target.getZ() + Math.sin(endAngle) * radius
        );
        ChainLightningVisualManager.spawnBolt(
                world,
                start,
                end,
                CONDUCTIVE_INDICATOR_SETTINGS,
                false
        );
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z,
                1, 0.015, 0.015, 0.015, 0.015);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z,
                1, 0.015, 0.015, 0.015, 0.015);
    }

    private static void spawnActivationEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getPos().add(0.0, Math.max(0.55, actor.getHeight() * 0.55), 0.0);
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2.0 * i / 3.0 + world.random.nextDouble() * 0.35;
            Vec3d start = center.add(Math.cos(angle) * 2.25, 5.5 + i * 0.45, Math.sin(angle) * 2.25);
            ChainLightningVisualManager.spawnBolt(world, start, center, SKY_BOLT_SETTINGS);
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                34, 0.5, 0.65, 0.5, 0.12);
        world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + 0.08, actor.getZ(),
                16, 0.55, 0.08, 0.55, 0.06);
        world.playSound(null, actor.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_FLYBY_03.get(),
                actor.getSoundCategory(), 0.7F, 0.78F);
    }

    private static void spawnAuraEffects(ServerWorld world, LivingEntity actor) {
        Vec3d center = actor.getPos().add(0.0, actor.getHeight() * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                3, 0.55, 0.55, 0.55, 0.025);
        world.spawnParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + actor.getHeight() + 2.0, actor.getZ(),
                3, 1.7, 0.25, 1.7, 0.015);
    }

    private static void spawnBoltImpactEffects(ServerWorld world, LivingEntity target) {
        Vec3d center = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.58), 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                16, 0.3, 0.3, 0.3, 0.11);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z,
                7, 0.22, 0.22, 0.22, 0.04);
        world.playSound(null, target.getBlockPos(),
                SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(),
                target.getSoundCategory(), 0.58F, 0.9F + world.random.nextFloat() * 0.18F);
    }

    private static void spawnThunderclapSpokes(ServerWorld world, Vec3d center, double radius) {
        Vec3d start = center.add(0.0, 0.35, 0.0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vec3d end = center.add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
            ChainLightningVisualManager.spawnBolt(world, start, end, THUNDERCLAP_SETTINGS);
        }
    }

    private static void addRing(ServerWorld world, Vec3d center, double radius,
                                int lifetime, boolean finalRing) {
        ACTIVE_RINGS.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new ThunderclapRing(center, radius, Math.max(1, lifetime), finalRing));
    }

    private static void knockAway(LivingEntity target, Vec3d center, Vec3d fallbackDirection,
                                  double strength, double lift) {
        Vec3d outward = target.getPos().subtract(center).multiply(1.0, 0.0, 1.0);
        if (outward.horizontalLengthSquared() < 0.0001) {
            outward = fallbackDirection;
        }
        if (strength > 0.0 && outward.horizontalLengthSquared() > 0.0001) {
            Vec3d direction = outward.normalize();
            target.takeKnockback(strength, -direction.x, -direction.z);
        }

        double resistance = net.minecraft.util.math.MathHelper.clamp(
                target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE),
                0.0,
                1.0
        );
        if (lift > 0.0 && resistance < 1.0) {
            target.addVelocity(0.0, lift * (1.0 - resistance), 0.0);
            target.velocityModified = true;
        }
    }

    private static Vec3d horizontalDirection(Vec3d direction, LivingEntity actor) {
        Vec3d horizontal = direction == null ? Vec3d.ZERO : direction.multiply(1.0, 0.0, 1.0);
        if (horizontal.horizontalLengthSquared() < 0.0001) {
            horizontal = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return horizontal.normalize();
    }

    private static final class ActiveStorm {
        private final UUID actorId;
        private final UUID sourceOwnerId;
        private final ItemStack stack;
        private final long startedAt;
        private final long expiresAt;
        private final float boltDamage;
        private final float conductiveBurstDamage;
        private final float finalThunderclapDamage;
        private final Set<UUID> struckThisCycle = new HashSet<>();
        private long nextPulseAt;
        private long nextFinalBoltAt;
        private int finalBoltsReleased;
        private boolean finishing;

        private ActiveStorm(UUID actorId, UUID sourceOwnerId, ItemStack stack,
                            long startedAt, long nextPulseAt, long expiresAt,
                            float boltDamage, float conductiveBurstDamage,
                            float finalThunderclapDamage) {
            this.actorId = actorId;
            this.sourceOwnerId = sourceOwnerId;
            this.stack = stack;
            this.startedAt = startedAt;
            this.nextPulseAt = nextPulseAt;
            this.expiresAt = expiresAt;
            this.boltDamage = boltDamage;
            this.conductiveBurstDamage = conductiveBurstDamage;
            this.finalThunderclapDamage = finalThunderclapDamage;
        }
    }

    private static final class ThunderclapRing {
        private final Vec3d center;
        private final double radius;
        private final int lifetime;
        private final boolean finalRing;
        private int age;

        private ThunderclapRing(Vec3d center, double radius, int lifetime, boolean finalRing) {
            this.center = center;
            this.radius = radius;
            this.lifetime = lifetime;
            this.finalRing = finalRing;
        }
    }
}
