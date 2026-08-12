package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.entity.BloodwakeBladeVisualEntity;
import net.sweenus.simplyswords.entity.BloodPlagueSpreadVisualEntity;
import net.sweenus.simplyswords.entity.LivyatanWaveVisualEntity;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.ParticlesRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.BleedHelper;
import net.sweenus.simplyswords.util.HelperMethods;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BloodwakeAbilityManager {
    public static final int MAX_FRENZY = 5;
    private static final DustParticleEffect CRIMSON_DUST = new DustParticleEffect(new Vector3f(0.55F, 0.015F, 0.025F), 1.25F);
    private static final String WAVE_VISUAL_TAG = "simplyswords_bloodwake_wave_visual";
    private static final String BLADE_VISUAL_TAG = "simplyswords_bloodwake_blade_visual";
    private static final Map<ServerWorld, List<ActiveVolley>> ACTIVE_VOLLEYS = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveBlade>> ACTIVE_BLADES = new HashMap<>();
    private static final Map<ServerWorld, List<ActiveScream>> ACTIVE_SCREAMS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, ActiveDeluge>> ACTIVE_DELUGES = new HashMap<>();
    private static final ThreadLocal<Boolean> SUPPRESS_PLAGUE_SPREAD = ThreadLocal.withInitial(() -> false);

    private BloodwakeAbilityManager() {
    }

    public static int getFrenzy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0
                : Math.clamp(stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge(), 0, MAX_FRENZY);
    }

    public static void setFrenzy(ItemStack stack, int amount) {
        if (stack != null && !stack.isEmpty()) {
            stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), new StoredChargeComponent(Math.clamp(amount, 0, MAX_FRENZY)));
        }
    }

    public static void addFrenzy(ItemStack stack, LivingEntity actor, LivingEntity sourceTarget) {
        int before = getFrenzy(stack);
        if (before >= MAX_FRENZY) {
            return;
        }
        setFrenzy(stack, before + 1);
        if (!(actor.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Vec3d start = sourceTarget == null ? actor.getPos().add(0.0, 0.8, 0.0)
                : sourceTarget.getPos().add(0.0, sourceTarget.getHeight() * 0.55, 0.0);
        Vec3d end = actor.getPos().add(0.0, actor.getHeight() * 0.58, 0.0);
        for (int i = 0; i <= 8; i++) {
            Vec3d pos = start.lerp(end, i / 8.0);
            world.spawnParticles(CRIMSON_DUST, pos.x, pos.y, pos.z, 2, 0.04, 0.04, 0.04, 0.01);
            world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(), pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.02);
        }
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                SoundCategory.PLAYERS, 0.42F, 0.92F + (before * 0.12F));
    }

    public static void triggerPassiveHit(ServerWorld world, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        if (BleedHelper.getStacks(target) >= BleedHelper.MAX_STACKS) {
            BleedHelper.clear(target);
            triggerBloodBurst(world, stack, attacker, target);
            addFrenzy(stack, attacker, target);
            return;
        }
        BleedHelper.apply(target, attacker, (float) HelperMethods.getEntityAttackDamage(attacker));
    }

    private static void triggerBloodBurst(ServerWorld world, ItemStack stack, LivingEntity attacker, LivingEntity primary) {
        double radius = Math.max(0.5, Config.uniqueEffects.bloodwake.burstRadius);
        float damage = HelperMethods.abilityScaledDamage(SpellScalingProfile.SOUL, attacker, stack,
                Config.uniqueEffects.bloodwake.burstDamageScaling,
                Config.uniqueEffects.bloodwake.burstSpellScaling);
        DamageSource source = world.getDamageSources().indirectMagic(attacker, attacker);
        Box box = primary.getBoundingBox().expand(radius, radius * 0.65, radius);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box,
                candidate -> candidate.isAlive() && HelperMethods.checkAbilityTarget(candidate, attacker))) {
            float enchanted = HelperMethods.applyAbilityDamageEnchantments(world, stack, candidate, source, damage);
            HelperMethods.applyDamageWithoutKnockback(candidate, source, enchanted);
            if (candidate != primary && candidate.isAlive()) {
                BleedHelper.apply(candidate, attacker, damage);
            }
        }
        Vec3d center = primary.getPos().add(0.0, Math.max(0.3, primary.getHeight() * 0.45), 0.0);
        BloodStainManager.createCircle(world, attacker, primary.getPos(), radius);
        spawnBloodBurstParticles(world, center, radius);
        world.spawnParticles(CRIMSON_DUST, center.x, center.y, center.z,
                12, 0.22, 0.18, 0.22, 0.055);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
                2, 0.12, 0.08, 0.12, 0.0);
        world.playSound(null, primary.getBlockPos(), SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(),
                SoundCategory.PLAYERS, 0.9F, 0.72F);
        world.playSound(null, primary.getBlockPos(), SoundRegistry.OBJECT_IMPACT_THUD.get(),
                SoundCategory.PLAYERS, 0.65F, 0.62F);
    }

    public static boolean activate(ServerWorld world, LivingEntity actor, ItemStack stack, Hand hand, Vec3d facing, int tier) {
        return switch (tier) {
            case 1 -> launchCrimsonWake(world, actor, facing);
            case 2 -> unleashScream(world, actor);
            case 3 -> summonJudgment(world, actor, stack, hand);
            case 4 -> HivemindSwarmManager.activateBloodFlies(world, actor);
            case 5 -> beginDeluge(world, actor, hand);
            default -> false;
        };
    }

    private static boolean launchCrimsonWake(ServerWorld world, LivingEntity actor, Vec3d facing) {
        Vec3d direction = horizontalDirection(facing, actor.getYaw());
        launchBloodWave(world, actor, direction, 0, false);
        playWaveCast(world, actor, 0.95F);
        return true;
    }

    private static boolean unleashScream(ServerWorld world, LivingEntity actor) {
        List<LivingEntity> bleeding = findBleedingTargets(world, actor, Config.uniqueEffects.bloodwake.targetingRadius,
                Math.max(1, Config.uniqueEffects.bloodwake.maximumScreamTargets));
        if (bleeding.isEmpty()) {
            fail(world, actor);
            return false;
        }
        int count = Math.min(6, bleeding.size());
        actor.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.BLOODWAKE_FRENZY),
                Math.max(1, Config.uniqueEffects.bloodwake.screamDuration), count - 1, false, false, true), actor);
        ACTIVE_SCREAMS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveScream(
                actor.getUuid(), bleeding.stream().map(Entity::getUuid).toList(), world.getTime()));
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_VOICE_20.get(),
                SoundCategory.PLAYERS, 1.15F, 0.62F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_VOICE_12.get(),
                SoundCategory.PLAYERS, 0.72F, 0.48F);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                SoundCategory.PLAYERS, 0.82F, 0.82F);
        return true;
    }

    private static boolean summonJudgment(ServerWorld world, LivingEntity actor, ItemStack stack, Hand hand) {
        List<LivingEntity> targets = findBleedingTargets(world, actor, Config.uniqueEffects.bloodwake.targetingRadius,
                Math.max(1, Config.uniqueEffects.bloodwake.bladeTargetCap));
        if (targets.isEmpty()) {
            fail(world, actor);
            return false;
        }
        long now = world.getTime();
        int hoverTicks = Math.max(1, Config.uniqueEffects.bloodwake.bladeHoverTicks);
        int plungeTicks = Math.max(1, Config.uniqueEffects.bloodwake.bladePlungeTicks);
        float weaponDamage = (float) Math.max(1.0, HelperMethods.getEntityAttackDamage(actor));
        List<ActiveBlade> blades = ACTIVE_BLADES.computeIfAbsent(world, ignored -> new ArrayList<>());
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            double angle = i * 2.399963229728653;
            Vec3d pos = target.getPos().add(Math.cos(angle) * 0.42,
                    target.getHeight() + 2.2 + (i % 3) * 0.18, Math.sin(angle) * 0.42);
            BloodwakeBladeVisualEntity visual = new BloodwakeBladeVisualEntity(world, pos.x, pos.y, pos.z, stack.copy());
            visual.addCommandTag(BLADE_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                blades.add(new ActiveBlade(actor.getUuid(), target.getUuid(), visual.getUuid(), stack.copy(), hand,
                        now, hoverTicks, plungeTicks, weaponDamage, i));
                spawnCrimsonTrail(world,
                        target.getPos().add(0.0, target.getHeight() * 0.55, 0.0), pos, 9);
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z,
                        5, 0.15, 0.2, 0.15, 0.03);
            }
        }
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_UNFOLD.get(), SoundCategory.PLAYERS, 0.75F, 0.72F);
        return !blades.isEmpty();
    }

    private static boolean beginDeluge(ServerWorld world, LivingEntity actor, Hand hand) {
        Map<UUID, ActiveDeluge> deluges = ACTIVE_DELUGES.computeIfAbsent(world, ignored -> new HashMap<>());
        ActiveDeluge activeDeluge = deluges.get(actor.getUuid());
        if (activeDeluge != null) {
            activeDeluge.expiryTick += Math.max(1, Config.uniqueEffects.bloodwake.delugeExtensionDuration);
            Vec3d center = actor.getPos().add(0.0, actor.getHeight() * 0.5, 0.0);
            spawnCrimsonRing(world, center, 1.35, 30, 0.2);
            world.spawnParticles(CRIMSON_DUST, center.x, center.y, center.z,
                    24, 0.55, 0.34, 0.55, 0.045);
            world.playSound(null, actor.getBlockPos(), SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
                    SoundCategory.PLAYERS, 0.65F, 0.82F);
            return true;
        }
        int duration = Math.max(20, Config.uniqueEffects.bloodwake.delugeDuration);
        ActiveDeluge deluge = new ActiveDeluge(actor.getUuid(), hand,
                world.getTime(), world.getTime() + duration, 0);
        deluges.put(actor.getUuid(), deluge);
        deluge.previousTargetId = spawnDelugeWave(world, actor, 0, null);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(), SoundCategory.PLAYERS, 0.9F, 0.58F);
        return true;
    }

    public static boolean isDelugeActive(ServerWorld world, LivingEntity actor) {
        Map<UUID, ActiveDeluge> deluges = ACTIVE_DELUGES.get(world);
        return deluges != null && actor != null && deluges.containsKey(actor.getUuid());
    }

    public static boolean hasActive(ServerWorld world) {
        return !ACTIVE_VOLLEYS.getOrDefault(world, List.of()).isEmpty()
                || !ACTIVE_BLADES.getOrDefault(world, List.of()).isEmpty()
                || !ACTIVE_SCREAMS.getOrDefault(world, List.of()).isEmpty()
                || !ACTIVE_DELUGES.getOrDefault(world, Map.of()).isEmpty()
                || world.getTime() % 40L == 0L;
    }

    public static void tick(ServerWorld world) {
        tickDeluges(world);
        tickVolleys(world);
        tickBlades(world);
        tickScreams(world);
        if (world.getTime() % 40L == 0L) {
            purgeOrphanVisuals(world);
        }
    }

    private static void tickDeluges(ServerWorld world) {
        Map<UUID, ActiveDeluge> deluges = ACTIVE_DELUGES.get(world);
        if (deluges == null) {
            return;
        }
        long now = world.getTime();
        Iterator<ActiveDeluge> iterator = deluges.values().iterator();
        while (iterator.hasNext()) {
            ActiveDeluge deluge = iterator.next();
            LivingEntity actor = resolveLiving(world, deluge.ownerId);
            ItemStack liveStack = actor == null ? ItemStack.EMPTY : getHeldStack(actor, deluge.hand);
            if (actor == null || now >= deluge.expiryTick || !liveStack.isOf(ItemsRegistry.BLOODWAKE.get())) {
                if (actor != null) {
                    world.spawnParticles(CRIMSON_DUST, actor.getX(), actor.getBodyY(0.55), actor.getZ(), 18, 0.5, 0.35, 0.5, 0.04);
                }
                iterator.remove();
                continue;
            }
            int interval = Math.max(1, Config.uniqueEffects.bloodwake.delugeVolleyInterval);
            if (now > deluge.startedAt && (now - deluge.startedAt) % interval == 0L) {
                deluge.volleyIndex++;
                deluge.previousTargetId = spawnDelugeWave(world, actor, deluge.volleyIndex, deluge.previousTargetId);
            }
            if (now % 5L == 0L) {
                world.spawnParticles(CRIMSON_DUST, actor.getX(), actor.getBodyY(0.48), actor.getZ(), 8, 0.65, 0.2, 0.65, 0.025);
            }
        }
        if (deluges.isEmpty()) {
            ACTIVE_DELUGES.remove(world);
        }
    }

    private static UUID spawnDelugeWave(ServerWorld world, LivingEntity actor, int volleyIndex, UUID previousTargetId) {
        LivingEntity target = chooseDelugeTarget(world, actor, previousTargetId);
        Vec3d direction;
        if (target == null) {
            double angle = world.random.nextDouble() * MathHelper.TAU;
            direction = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
        } else {
            direction = horizontalDirection(target.getPos().subtract(actor.getPos()), actor.getYaw());
        }
        launchBloodWave(world, actor, direction, volleyIndex, true);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(),
                SoundCategory.PLAYERS, 0.36F, 0.66F + (volleyIndex % 4) * 0.055F);
        world.playSound(null, actor.getBlockPos(), SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
                SoundCategory.PLAYERS, 0.24F, 1.02F + world.random.nextFloat() * 0.12F);
        return target == null ? null : target.getUuid();
    }

    private static LivingEntity chooseDelugeTarget(ServerWorld world, LivingEntity actor, UUID previousTargetId) {
        double radius = Math.max(1.0, Config.uniqueEffects.bloodwake.targetingRadius);
        List<LivingEntity> candidates = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class,
                actor.getBoundingBox().expand(radius, radius * 0.65, radius),
                target -> target.isAlive() && target != actor && HelperMethods.checkAbilityTarget(target, actor)));
        if (candidates.size() > 1 && previousTargetId != null) {
            candidates.removeIf(target -> previousTargetId.equals(target.getUuid()));
        }
        return candidates.isEmpty() ? null : candidates.get(world.random.nextInt(candidates.size()));
    }

    private static void launchBloodWave(ServerWorld world, LivingEntity actor, Vec3d facing,
                                        int volleyIndex, boolean redTide) {
        Vec3d direction = horizontalDirection(facing, actor.getYaw());
        spawnVolley(world, actor, actor.getPos().add(direction.multiply(1.2)),
                List.of(direction), volleyIndex, redTide);
    }

    private static void spawnVolley(ServerWorld world, LivingEntity actor, Vec3d origin,
                                    List<Vec3d> directions, int volleyIndex, boolean redTide) {
        List<UUID> stainIds = directions.stream()
                .map(direction -> BloodStainManager.beginTrail(
                        world, actor, origin, direction, LivyatanWaveManager.waveWidthBlocks()))
                .toList();
        ACTIVE_VOLLEYS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(new ActiveVolley(
                actor.getUuid(), origin, directions, stainIds,
                world.getTime(), volleyIndex, redTide));
    }

    private static void tickVolleys(ServerWorld world) {
        List<ActiveVolley> volleys = ACTIVE_VOLLEYS.get(world);
        if (volleys == null) {
            return;
        }
        int interval = Math.max(1, Config.uniqueEffects.bloodwake.waveStepInterval);
        int maxSteps = Math.max(1, Config.uniqueEffects.bloodwake.waveLengthSteps);
        long now = world.getTime();
        volleys.removeIf(volley -> {
            LivingEntity owner = resolveLiving(world, volley.ownerId);
            if (owner == null) {
                volley.finishStains(world);
                return true;
            }
            long age = now - volley.startedAt;
            if (age < 0L || age % interval != 0L) {
                return false;
            }
            int step = volley.currentStep++;
            if (step > maxSteps) {
                volley.finishStains(world);
                return true;
            }
            double travel = step * Math.max(0.1, Config.uniqueEffects.bloodwake.waveStepDistance);
            for (int i = 0; i < volley.directions.size(); i++) {
                Vec3d direction = volley.directions.get(i);
                Vec3d center = volley.origin.add(direction.multiply(travel));
                BloodStainManager.extendTrail(world, volley.stainIds.get(i), center);
                applyWaveContact(world, owner, volley, center, direction);
                spawnBloodWaveVisual(world, center, direction, step, volley.volleyIndex);
            }
            return false;
        });
        if (volleys.isEmpty()) {
            ACTIVE_VOLLEYS.remove(world);
        }
    }

    private static void applyWaveContact(ServerWorld world, LivingEntity owner, ActiveVolley volley, Vec3d center, Vec3d direction) {
        double width = LivyatanWaveManager.waveWidthBlocks();
        double thickness = Math.max(0.25, Config.uniqueEffects.bloodwake.waveThickness);
        double reach = Math.max(width, thickness * 2.0);
        Box hitBox = Box.of(center.add(0.0, 0.65, 0.0), reach, 2.6, reach);
        double yaw = Math.atan2(direction.z, direction.x);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, hitBox, LivingEntity::isAlive)) {
            Vec3d local = target.getPos().subtract(center);
            double forwardDistance = local.x * Math.cos(yaw) + local.z * Math.sin(yaw);
            double sideDistance = -local.x * Math.sin(yaw) + local.z * Math.cos(yaw);
            if (Math.abs(forwardDistance) > thickness || Math.abs(sideDistance) > width * 0.5
                    || !volley.hitEntities.add(target.getUuid()) || !HelperMethods.checkAbilityTarget(target, owner)) {
                continue;
            }
            applyBloodPlague(target, owner);
            if (volley.redTide) {
                BleedHelper.apply(target, owner, (float) HelperMethods.getEntityAttackDamage(owner));
            }
            double push = Math.max(0.0, Config.uniqueEffects.bloodwake.wavePush);
            target.addVelocity(direction.x * push, Math.max(0.0, Config.uniqueEffects.bloodwake.waveLift), direction.z * push);
            target.velocityModified = true;
            target.velocityDirty = true;
            world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(), target.getX(), target.getBodyY(0.55), target.getZ(), 8, 0.3, 0.25, 0.3, 0.08);
        }
    }

    private static void spawnBloodWaveVisual(ServerWorld world, Vec3d center, Vec3d direction, int step, int volleyIndex) {
        Vec3d right = new Vec3d(-direction.z, 0.0, direction.x).normalize();
        int laneCount = LivyatanWaveManager.visualLaneCount();
        double laneSpacing = LivyatanWaveManager.visualLaneSpacing();
        double centerGroundY = LivyatanWaveManager.findGroundTopY(world, center.x, center.z, center.y);
        List<LivyatanWaveVisualEntity.WaveLane> lanes = new ArrayList<>(laneCount);
        float maximumHeight = 0.35F;
        for (int lane = 0; lane < laneCount; lane++) {
            float laneCenter = (laneCount - 1) * 0.5F;
            float laneOffsetUnits = lane - laneCenter;
            Vec3d lanePos = center.add(right.multiply(laneOffsetUnits * laneSpacing));
            double groundY = LivyatanWaveManager.findGroundTopY(world, lanePos.x, lanePos.z, lanePos.y);
            float laneFalloff = 1.0F - Math.abs(laneOffsetUnits) * 0.18F;
            float targetHeight = Math.max(0.35F, 1.75F * laneFalloff);
            maximumHeight = Math.max(maximumHeight, targetHeight);
            lanes.add(new LivyatanWaveVisualEntity.WaveLane(
                    (float) (lanePos.x - center.x),
                    (float) (groundY - centerGroundY),
                    (float) (lanePos.z - center.z),
                    targetHeight));
            world.spawnParticles(CRIMSON_DUST, lanePos.x, groundY + 0.25, lanePos.z,
                    2, 0.14, 0.16, 0.14, 0.035);
        }
        LivyatanWaveVisualEntity visual = new LivyatanWaveVisualEntity(
                world, center.x, centerGroundY, center.z, maximumHeight);
        visual.setBloodwakeStyle(true);
        visual.setBloodwakeLanes(lanes);
        visual.setYaw((float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) + 90.0));
        visual.addCommandTag(WAVE_VISUAL_TAG);
        visual.setHeightScale(1.0F);
        world.spawnEntity(visual);
        if (step % 4 == 0 && volleyIndex == 0) {
            world.playSound(null, center.x, centerGroundY, center.z,
                    SoundRegistry.DARK_SWORD_WHOOSH_03.get(), SoundCategory.PLAYERS, 0.24F, 0.78F);
        }
    }

    private static void tickBlades(ServerWorld world) {
        List<ActiveBlade> blades = ACTIVE_BLADES.get(world);
        if (blades == null) {
            return;
        }
        long now = world.getTime();
        blades.removeIf(blade -> tickBlade(world, blade, now));
        if (blades.isEmpty()) {
            ACTIVE_BLADES.remove(world);
        }
    }

    private static void tickScreams(ServerWorld world) {
        List<ActiveScream> screams = ACTIVE_SCREAMS.get(world);
        if (screams == null) {
            return;
        }
        long now = world.getTime();
        screams.removeIf(scream -> tickScream(world, scream, now));
        if (screams.isEmpty()) {
            ACTIVE_SCREAMS.remove(world);
        }
    }

    private static boolean tickScream(ServerWorld world, ActiveScream scream, long now) {
        LivingEntity actor = resolveLiving(world, scream.ownerId());
        long age = now - scream.startedAt();
        if (actor == null || age < 0L || age > 20L) {
            return true;
        }

        Vec3d actorCenter = actor.getPos().add(0.0, actor.getHeight() * 0.56, 0.0);
        spawnScreamSpiral(world, actorCenter, age);

        if (age <= 10L && age % 2L == 0L) {
            for (UUID targetId : scream.targetIds()) {
                LivingEntity target = resolveLiving(world, targetId);
                if (target == null) {
                    continue;
                }
                Vec3d targetCenter = target.getPos().add(0.0, target.getHeight() * 0.58, 0.0);
                spawnCrimsonTrail(world, targetCenter, actorCenter, 10);
                double pullProgress = MathHelper.clamp(age / 10.0, 0.0, 1.0);
                Vec3d drawnBlood = targetCenter.lerp(actorCenter, pullProgress);
                world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(),
                        drawnBlood.x, drawnBlood.y, drawnBlood.z, 3, 0.08, 0.08, 0.08, 0.02);
                if (age == 0L || age == 6L || age == 10L) {
                    spawnCrimsonRing(world, targetCenter, 0.48 + age * 0.025, 14, 0.22);
                }
            }
        }

        if (age == 3L || age == 8L || age == 13L || age == 18L) {
            int pulse = age == 3L ? 0 : age == 8L ? 1 : age == 13L ? 2 : 3;
            spawnCrimsonRing(world, actorCenter.add(0.0, -0.46, 0.0),
                    1.8 + pulse * 1.55, 30 + pulse * 10, 0.18 + pulse * 0.07);
        }
        if (age == 8L) {
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    actorCenter.x, actorCenter.y, actorCenter.z, 5, 0.8, 0.45, 0.8, 0.0);
            world.spawnParticles(CRIMSON_DUST,
                    actorCenter.x, actorCenter.y, actorCenter.z, 42, 0.9, 0.72, 0.9, 0.075);
            world.playSound(null, actor.getBlockPos(), SoundRegistry.OBJECT_IMPACT_THUD_REPEAT.get(),
                    SoundCategory.PLAYERS, 0.88F, 0.58F);
            world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(),
                    SoundCategory.PLAYERS, 0.82F, 0.66F);
        } else if (age == 18L) {
            world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_WHOOSH_03.get(),
                    SoundCategory.PLAYERS, 0.55F, 0.62F);
        }
        return false;
    }

    private static void spawnScreamSpiral(ServerWorld world, Vec3d center, long age) {
        double radius = 0.42 + age * 0.035;
        for (int strand = 0; strand < 3; strand++) {
            for (int pointIndex = 0; pointIndex < 5; pointIndex++) {
                double heightProgress = pointIndex / 4.0;
                double angle = age * 0.48 + strand * MathHelper.TAU / 3.0 - heightProgress * 1.35;
                double pointRadius = radius * (0.82 + heightProgress * 0.28);
                Vec3d point = center.add(
                        Math.cos(angle) * pointRadius,
                        -0.62 + heightProgress * 1.72,
                        Math.sin(angle) * pointRadius);
                world.spawnParticles(CRIMSON_DUST, point.x, point.y, point.z,
                        1, 0.035, 0.035, 0.035, 0.006);
                if (pointIndex == 4 && age % 2L == 0L) {
                    world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(),
                            point.x, point.y, point.z, 1, 0.04, 0.06, 0.04, 0.015);
                }
            }
        }
    }

    private static boolean tickBlade(ServerWorld world, ActiveBlade blade, long now) {
        LivingEntity owner = resolveLiving(world, blade.ownerId);
        LivingEntity target = resolveLiving(world, blade.targetId);
        Entity visualEntity = world.getEntity(blade.visualId);
        BloodwakeBladeVisualEntity visual = visualEntity instanceof BloodwakeBladeVisualEntity entity ? entity : null;
        if (owner == null || target == null || visual == null || !target.isAlive()) {
            fizzleBlade(world, visual);
            return true;
        }
        long age = now - blade.startedAt;
        Vec3d targetCenter = target.getPos().add(0.0, target.getHeight() * 0.58, 0.0);
        double angle = blade.index * 2.399963229728653 + age * 0.035;
        Vec3d hover = target.getPos().add(Math.cos(angle) * 0.42,
                target.getHeight() + 2.2 + Math.sin(age * 0.16 + blade.index) * 0.16,
                Math.sin(angle) * 0.42);
        if (age < blade.hoverTicks) {
            visual.setPosition(hover.x, hover.y, hover.z);
            visual.setPlungeProgress(0.0F);
            if (age % 6L == 0L) {
                world.spawnParticles(CRIMSON_DUST, hover.x, hover.y, hover.z, 4, 0.12, 0.16, 0.12, 0.02);
            }
            return false;
        }
        float progress = MathHelper.clamp((float) (age - blade.hoverTicks) / blade.plungeTicks, 0.0F, 1.0F);
        float eased = progress * progress;
        Vec3d pos = hover.lerp(targetCenter, eased);
        visual.setPosition(pos.x, pos.y, pos.z);
        visual.setPlungeProgress(progress);
        world.spawnParticles(CRIMSON_DUST, pos.x, pos.y, pos.z, 2, 0.07, 0.07, 0.07, 0.015);
        if (progress < 1.0F) {
            return false;
        }

        int bleedStacks = BleedHelper.getStacks(target);
        float damage = blade.weaponDamage * (1.0F + Math.max(0.0F, Config.uniqueEffects.bloodwake.bladeDamagePerBleedStack) * bleedStacks);
        int before = getFrenzy(blade.stack);
        boolean hit = SimplySwordsAPI.applyEntityWeaponHit(blade.stack, target, owner, damage);
        int earned = Math.max(0, getFrenzy(blade.stack) - before);
        if (earned > 0) {
            ItemStack liveStack = getHeldStack(owner, blade.hand);
            if (liveStack.isOf(ItemsRegistry.BLOODWAKE.get())) {
                for (int i = 0; i < earned; i++) {
                    addFrenzy(liveStack, owner, target);
                }
            }
        }
        if (hit) {
            world.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(), targetCenter.x, targetCenter.y, targetCenter.z, 18, 0.38, 0.34, 0.38, 0.12);
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, targetCenter.x, targetCenter.y, targetCenter.z, 2, 0.1, 0.1, 0.1, 0.0);
            world.playSound(null, target.getBlockPos(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.72F, 0.86F);
        }
        visual.discard();
        return true;
    }

    public static void applyBloodPlague(LivingEntity target, LivingEntity owner) {
        if (target == null || !target.isAlive()) {
            return;
        }
        SimplySwordsStatusEffectInstance plague = new SimplySwordsStatusEffectInstance(
                EffectRegistry.getReference(EffectRegistry.BLOOD_PLAGUE),
                Math.max(1, Config.uniqueEffects.bloodwake.plagueDuration), 0, false, false, true);
        plague.setSourceEntity(owner);
        target.addStatusEffect(plague, owner);
        if (target.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(CRIMSON_DUST, target.getX(), target.getBodyY(0.62), target.getZ(), 13, 0.35, 0.36, 0.35, 0.035);
        }
    }

    public static void onTargetDamaged(LivingEntity plaguedTarget, DamageSource source) {
        if (SUPPRESS_PLAGUE_SPREAD.get() || !(plaguedTarget.getWorld() instanceof ServerWorld world)
                || !(plaguedTarget.getStatusEffect(EffectRegistry.getReference(EffectRegistry.BLOOD_PLAGUE))
                instanceof SimplySwordsStatusEffectInstance plague)
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || !isDirectAttack(source)) {
            return;
        }
        LivingEntity owner = plague.getSourceEntity();
        if (owner == null || !owner.isAlive() || owner.getWorld() != world) {
            owner = attacker;
        }
        double radius = Math.max(0.5, Config.uniqueEffects.bloodwake.plagueRadius);
        float snapshotDamage = (float) Math.max(1.0, HelperMethods.getEntityAttackDamage(owner));
        int spreadTargets = 0;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class,
                plaguedTarget.getBoundingBox().expand(radius), candidate -> candidate.isAlive() && candidate != plaguedTarget)) {
            if (HelperMethods.checkAbilityTarget(candidate, owner)) {
                BleedHelper.apply(candidate, owner, snapshotDamage);
                spreadTargets++;
            }
        }
        if (spreadTargets > 0) {
            BloodStainManager.createCircle(world, owner, plaguedTarget.getPos(), radius);
            world.spawnEntity(new BloodPlagueSpreadVisualEntity(world, plaguedTarget, (float) radius, 8));
            world.playSound(null, plaguedTarget.getBlockPos(), SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(),
                    SoundCategory.PLAYERS, 0.28F, 1.34F);
        }
    }

    public static void runWithoutPlagueSpread(Runnable action) {
        boolean previous = SUPPRESS_PLAGUE_SPREAD.get();
        SUPPRESS_PLAGUE_SPREAD.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_PLAGUE_SPREAD.set(previous);
        }
    }

    private static boolean isDirectAttack(DamageSource source) {
        ItemStack weapon = source.getWeaponStack();
        return source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)
                || source.isIn(DamageTypeTags.IS_PROJECTILE)
                || weapon != null && !weapon.isEmpty()
                || source.isDirect() && !"indirectMagic".equals(source.getName()) && !"magic".equals(source.getName());
    }

    private static List<LivingEntity> findBleedingTargets(ServerWorld world, LivingEntity actor, double configuredRadius, int cap) {
        double radius = Math.max(1.0, configuredRadius);
        return world.getEntitiesByClass(LivingEntity.class, actor.getBoundingBox().expand(radius, radius * 0.65, radius),
                        target -> target.isAlive() && target != actor && BleedHelper.hasBleed(target)
                                && HelperMethods.checkAbilityTarget(target, actor))
                .stream()
                .sorted(Comparator.comparingDouble(actor::squaredDistanceTo))
                .limit(Math.max(1, cap))
                .toList();
    }

    private static LivingEntity resolveLiving(ServerWorld world, UUID uuid) {
        Entity entity = uuid == null ? null : world.getEntity(uuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static ItemStack getHeldStack(LivingEntity actor, Hand hand) {
        if (actor == null) {
            return ItemStack.EMPTY;
        }
        return actor.getStackInHand(hand == null ? Hand.MAIN_HAND : hand);
    }

    private static Vec3d horizontalDirection(Vec3d facing, float yaw) {
        Vec3d horizontal = facing == null ? Vec3d.ZERO : new Vec3d(facing.x, 0.0, facing.z);
        return horizontal.lengthSquared() < 1.0E-6 ? Vec3d.fromPolar(0.0F, yaw).normalize() : horizontal.normalize();
    }

    private static void playWaveCast(ServerWorld world, LivingEntity actor, float pitch) {
        world.playSound(null, actor.getBlockPos(), SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.7F, pitch);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_WHOOSH_02.get(), SoundCategory.PLAYERS, 0.55F, 0.78F);
    }

    private static void spawnCrimsonRing(ServerWorld world, Vec3d center, double radius, int points, double verticalJitter) {
        double phase = world.random.nextDouble() * MathHelper.TAU;
        for (int i = 0; i < Math.max(8, points); i++) {
            double angle = phase + MathHelper.TAU * i / Math.max(8, points);
            world.spawnParticles(CRIMSON_DUST,
                    center.x + Math.cos(angle) * radius,
                    center.y + (world.random.nextDouble() - 0.5) * verticalJitter,
                    center.z + Math.sin(angle) * radius,
                    1, 0.025, 0.025, 0.025, 0.005);
        }
    }

    private static void spawnBloodBurstParticles(ServerWorld world, Vec3d center, double radius) {
        double visualScale = MathHelper.clamp(radius / 4.0, 0.65, 1.4);
        for (int i = 0; i < 48; i++) {
            boolean upwardJet = i >= 36;
            double angle = MathHelper.TAU * (i % 36) / 36.0 + (world.random.nextDouble() - 0.5) * 0.24;
            double horizontalSpeed = (upwardJet
                    ? 0.08 + world.random.nextDouble() * 0.18
                    : 0.20 + world.random.nextDouble() * 0.30) * visualScale;
            double verticalSpeed = (upwardJet
                    ? 0.30 + world.random.nextDouble() * 0.22
                    : 0.14 + world.random.nextDouble() * 0.24) * visualScale;
            double jitterX = (world.random.nextDouble() - 0.5) * 0.24;
            double jitterY = (world.random.nextDouble() - 0.5) * 0.18;
            double jitterZ = (world.random.nextDouble() - 0.5) * 0.24;
            world.spawnParticles(ParticlesRegistry.BLOOD_SPRAY.get(),
                    center.x + jitterX, center.y + jitterY, center.z + jitterZ,
                    0, Math.cos(angle) * horizontalSpeed, verticalSpeed,
                    Math.sin(angle) * horizontalSpeed, 1.0);
        }
    }

    private static void spawnCrimsonTrail(ServerWorld world, Vec3d from, Vec3d to, int points) {
        for (int i = 1; i < Math.max(2, points); i++) {
            Vec3d point = from.lerp(to, i / (double) Math.max(2, points));
            world.spawnParticles(CRIMSON_DUST, point.x, point.y, point.z,
                    1, 0.025, 0.025, 0.025, 0.004);
        }
    }

    private static void fail(ServerWorld world, LivingEntity actor) {
        world.spawnParticles(ParticleTypes.SMOKE, actor.getX(), actor.getBodyY(0.55), actor.getZ(), 5, 0.18, 0.16, 0.18, 0.01);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_BLOCK.get(), SoundCategory.PLAYERS, 0.34F, 1.5F);
    }

    private static void fizzleBlade(ServerWorld world, BloodwakeBladeVisualEntity visual) {
        if (visual != null) {
            world.spawnParticles(ParticleTypes.SMOKE, visual.getX(), visual.getY(), visual.getZ(), 5, 0.15, 0.15, 0.15, 0.01);
            visual.discard();
        }
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        Set<UUID> activeBladeIds = new HashSet<>();
        for (ActiveBlade blade : ACTIVE_BLADES.getOrDefault(world, List.of())) {
            activeBladeIds.add(blade.visualId);
        }
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof LivyatanWaveVisualEntity && entity.getCommandTags().contains(WAVE_VISUAL_TAG)
                    && entity.age > 20) {
                entity.discard();
            } else if (entity instanceof BloodwakeBladeVisualEntity
                    && entity.getCommandTags().contains(BLADE_VISUAL_TAG)
                    && !activeBladeIds.contains(entity.getUuid())) {
                entity.discard();
            }
        }
    }

    private static final class ActiveVolley {
        private final UUID ownerId;
        private final Vec3d origin;
        private final List<Vec3d> directions;
        private final List<UUID> stainIds;
        private final long startedAt;
        private final int volleyIndex;
        private final boolean redTide;
        private final Set<UUID> hitEntities = new HashSet<>();
        private int currentStep;

        private ActiveVolley(UUID ownerId, Vec3d origin, List<Vec3d> directions,
                             List<UUID> stainIds,
                             long startedAt, int volleyIndex, boolean redTide) {
            this.ownerId = ownerId;
            this.origin = origin;
            this.directions = directions;
            this.stainIds = stainIds;
            this.startedAt = startedAt;
            this.volleyIndex = volleyIndex;
            this.redTide = redTide;
        }

        private void finishStains(ServerWorld world) {
            for (UUID stainId : this.stainIds) {
                BloodStainManager.finishTrail(world, stainId);
            }
        }
    }

    private record ActiveBlade(UUID ownerId, UUID targetId, UUID visualId, ItemStack stack, Hand hand,
                               long startedAt, int hoverTicks, int plungeTicks, float weaponDamage, int index) {
    }

    private record ActiveScream(UUID ownerId, List<UUID> targetIds, long startedAt) {
    }

    private static final class ActiveDeluge {
        private final UUID ownerId;
        private final Hand hand;
        private final long startedAt;
        private long expiryTick;
        private int volleyIndex;
        private UUID previousTargetId;

        private ActiveDeluge(UUID ownerId, Hand hand, long startedAt, long expiryTick, int volleyIndex) {
            this.ownerId = ownerId;
            this.hand = hand;
            this.startedAt = startedAt;
            this.expiryTick = expiryTick;
            this.volleyIndex = volleyIndex;
        }
    }
}
