package net.sweenus.simplyswords.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryTuning;
import net.sweenus.simplyswords.api.ability.DeathShadowBloodMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulkeeperLanternVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.*;

public final class SoulkeeperLanternManager {

    private static final String SOULKEEPER_LANTERN_VISUAL_TAG = "simplyswords_soulkeeper_lantern_visual";
    private static final double BASE_ANGULAR_SPEED = 0.055;
    private static final double LANTERN_HEIGHT = 1.05;
    private static final double BOB_AMPLITUDE = 0.08;
    private static final double BOB_SPEED = 0.13;
    private static final double CONTACT_RADIUS = 0.85;
    private static final int BASE_LANTERN_COUNT = 2;
    private static final int ACTIVE_LANTERN_COUNT = 4;

    private static final Map<UUID, ActiveLanterns> ACTIVE_LANTERNS = new HashMap<>();

    private SoulkeeperLanternManager() {
    }

    public static void tickPlayerFromItem(ServerPlayerEntity player, ItemStack stack) {
        tickFromItem(player, stack);
    }

    public static void tickFromItem(LivingEntity owner, ItemStack stack) {
        if (owner == null || stack == null || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.SOULKEEPER.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || !owner.getMainHandStack().equals(stack)) {
            if (owner != null && owner.getWorld() instanceof ServerWorld world) {
                discardActive(world, owner.getUuid());
                ACTIVE_LANTERNS.remove(owner.getUuid());
            }
            return;
        }
        if (owner.getWorld() instanceof ServerWorld world) {
            tickActive(world, owner, stack);
        }
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        if (!isHoldingSoulkeeper(player) || !player.isAlive()) {
            discardActive(player.getServerWorld(), player.getUuid());
            ACTIVE_LANTERNS.remove(player.getUuid());
        }
    }

    public static void tickWorld(ServerWorld world) {
        if (world.getTime() % 80L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof SoulkeeperLanternVisualEntity visual) || !entity.getCommandTags().contains(SOULKEEPER_LANTERN_VISUAL_TAG)) {
                continue;
            }
            UUID ownerId = visual.getOwnerUuid();
            ActiveLanterns active = ownerId == null ? null : ACTIVE_LANTERNS.get(ownerId);
            LivingEntity owner = resolveOwner(world, ownerId);
            if (active == null || owner == null || !owner.isAlive() || !isHoldingSoulkeeper(owner)) {
                visual.discard();
                if (ownerId != null) {
                    ACTIVE_LANTERNS.remove(ownerId);
                }
                continue;
            }
            if (!visual.getUuid().equals(active.visualId)) {
                visual.discard();
            }
        }
    }

    public static void onSoulkeeperHit(LivingEntity attacker) {
        ActiveLanterns active = attacker == null ? null : ACTIVE_LANTERNS.get(attacker.getUuid());
        if (isHoldingSoulkeeper(attacker) && (active == null || !active.lanternTuning.flag(1 << 7))) {
            increaseSpeed(attacker);
        }
    }

    public static void onSoulkeeperHit(LivingEntity attacker, LivingEntity target, ItemStack stack) {
        onSoulkeeperHit(attacker);
        ActiveLanterns active = ACTIVE_LANTERNS.get(attacker.getUuid());
        if (active == null || target == null || !(attacker.getWorld() instanceof ServerWorld world)) return;
        if (active.soulbrandUntil.getOrDefault(target.getUuid(), 0L) > world.getTime()) {
            float bonus = (float) (HelperMethods.getEntityAttackDamage(attacker)
                    * (active.lanternTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOULBRAND_DAMAGE_MULTIPLIER, 1) - 1));
            if (bonus > 0) damageTarget(world, attacker, target, bonus);
        }
        if (!target.isAlive() && active.velocityTuning.flag(1 << 14)) {
            active.capturedUntil = world.getTime() + active.velocityTuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_CAPTURE_DURATION_TICKS, 60);
        }
    }

    public static void activate(ServerPlayerEntity player, ItemStack stack) {
        activate((LivingEntity) player, stack);
    }

    public static void activate(LivingEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || !stack.isOf(ItemsRegistry.SOULKEEPER.get()) || !isHoldingSoulkeeper(player)) {
            return;
        }

        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        ActiveLanterns active = activeFor(world, player.getUuid());
        active.extraLanternsUntilTick = world.getTime() + extraLanternDuration(
                Config.uniqueEffects.soulkeeper.activeExtraLanternDuration, active.conclaveTuning);
        active.fifthLanternUntilTick = !active.conclaveTuning.flag(1 << 25)
                && !active.conclaveTuning.flag(1 << 26) && active.conclaveTuning.flag(1 << 20)
                ? world.getTime() + active.conclaveTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_FIFTH_LANTERN_DURATION_TICKS, 80) : 0;
        active.recallUsed = false;
        increaseSpeed(active);
        int absorption = active.conclaveTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_WARD_ABSORPTION, 0);
        if (absorption > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                    active.conclaveTuning.integer(DeathShadowBloodMasteryTuning.Setting.SOUL_WARD_DURATION_TICKS, 80),
                    Math.max(0, absorption / 4 - 1)), player);
        }

        Vec3d pos = player.getPos().add(0.0, 1.0, 0.0);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 18, 0.55, 0.35, 0.55, 0.03);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 8, 0.45, 0.3, 0.45, 0.02);
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_03.get(), SoundCategory.PLAYERS, 0.75F, 0.8F + player.getRandom().nextFloat() * 0.2F);
        tickActive(world, player, stack);
    }

    public static void activate(WeaponAbilityContext context) {
        if (tryDetonate(context.world(), context.actor(), context.stack())) {
            return;
        }
        UniqueAbilityExecution execution = DeathShadowBloodMasteryCombatManager.beginActive(
                DeathShadowBloodMasteryAbilities.SOULKEEPER_CONCLAVE, context, Config.uniqueEffects.soulkeeper.cooldown);
        ActiveLanterns active = activeFor(context.world(), context.actor().getUuid());
        active.conclaveTuning = DeathShadowBloodMasteryAbilities.tuning(execution);
        activate(context.actor(), context.stack());
        UniqueAbilityApi.emit(execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                DeathShadowBloodMasteryAbilities.HIT, null, 0, 0);
        DeathShadowBloodMasteryCombatManager.scheduleFinish(context.world(), execution,
                Math.max(1, (int) (active.extraLanternsUntilTick - context.world().getTime())), 0);
    }

    public static boolean tryDetonate(ServerWorld world, LivingEntity actor, ItemStack stack) {
        if (world == null || actor == null || !actor.isAlive() || stack == null || stack.isEmpty()
                || !stack.isOf(ItemsRegistry.SOULKEEPER.get()) || !AwakeningApi.isAbilityUnlocked(stack)
                || !actor.isSneaking() || !isHoldingSoulkeeper(actor)) {
            return false;
        }
        ActiveLanterns active = activeFor(world, actor.getUuid());
        refreshTunings(world, actor, stack, active, true);
        if (!active.velocityTuning.flag(1 << 17) || active.speedMultiplier <= 1) {
            return false;
        }

        float consumed = active.speedMultiplier - 1;
        float damage = HelperMethods.abilityScaledDamage("soul", actor, stack,
                consumed * (float) active.velocityTuning.get(
                        DeathShadowBloodMasteryTuning.Setting.SOUL_DETONATION_DAMAGE_PER_SPEED, .18), 0);
        double radius = active.velocityTuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_DETONATION_RADIUS, 4);
        Vec3d center = actor.getPos().add(0, actor.getHeight() * .5, 0);
        world.getEntitiesByClass(LivingEntity.class, actor.getBoundingBox().expand(radius),
                        target -> HelperMethods.checkAbilityTarget(target, actor)
                                && target.getPos().add(0, target.getHeight() * .5, 0)
                                .squaredDistanceTo(center) <= radius * radius)
                .forEach(target -> damageTarget(world, actor, target, damage));
        active.speedMultiplier = 1;
        active.extraLanternsUntilTick = 0;
        active.fifthLanternUntilTick = 0;
        spawnSoulDetonationEffects(world, actor, radius);
        return true;
    }

    private static void tickActivePlayer(ServerPlayerEntity player, ItemStack stack) {
        tickActive(player.getServerWorld(), player, stack);
    }

    private static void tickActive(ServerWorld world, LivingEntity player, ItemStack stack) {
        UUID ownerId = player.getUuid();
        ActiveLanterns active = activeFor(world, ownerId);
        final ActiveLanterns current = active;
        refreshTunings(world, player, stack, active);
        active.tick(world);

        int lanternCount = active.lanternCount(world);
        SoulkeeperLanternVisualEntity visual = resolveTracked(world, active.visualId, ownerId);
        if (visual == null) {
            Vec3d start = player.getPos();
            visual = new SoulkeeperLanternVisualEntity(world, ownerId, player.getId(), start.x, start.y, start.z);
            visual.addCommandTag(SOULKEEPER_LANTERN_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                active.visualId = visual.getUuid();
            }
        }

        Vec3d pos = player.getPos();
        visual.setPos(pos.x, pos.y, pos.z);
        visual.setOwnerEntityId(player.getId());
        visual.setLanternCount(lanternCount);
        visual.setSpeedMultiplier(active.speedMultiplier);
        visual.setOrbitRadius((float) orbitRadius(Config.uniqueEffects.soulkeeper.orbitRadius,
                active.lanternTuning));
        visual.setOrbitPhase((float) active.orbitPhase);

        damageCollidingTargets(world, player, stack, active, lanternCount);
        if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 22)
                && world.getTime() - active.lastIntercept >= active.conclaveTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_INTERCEPT_INTERVAL_TICKS, 40)) {
            double interceptRadius = active.conclaveTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_INTERCEPT_RADIUS, 1);
            double orbitRadius = orbitRadius(Config.uniqueEffects.soulkeeper.orbitRadius,
                    active.lanternTuning);
            world.getEntitiesByClass(ProjectileEntity.class,
                            player.getBoundingBox().expand(orbitRadius + interceptRadius + 1),
                            projectile -> hostileProjectile(projectile, player)
                                    && intersectsExtraLantern(player.getPos(), projectile.getPos(), current.orbitPhase,
                                    orbitRadius, lanternCount, interceptRadius))
                    .stream().min(Comparator.comparingDouble(player::squaredDistanceTo)).ifPresent(projectile -> {
                        projectile.discard();
                        current.lastIntercept = world.getTime();
                    });
        }
        if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 23)) {
            double allyRadius = active.conclaveTuning.get(DeathShadowBloodMasteryTuning.Setting.SOUL_ALLY_RADIUS, 5);
            Vec3d center = player.getPos().add(0, player.getHeight() * .5, 0);
            world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(allyRadius),
                            ally -> ally != player && !HelperMethods.checkAbilityTarget(ally, player)
                                    && ally.getPos().add(0, ally.getHeight() * .5, 0)
                                    .squaredDistanceTo(center) <= allyRadius * allyRadius)
                    .stream().limit(active.conclaveTuning.integer(
                            DeathShadowBloodMasteryTuning.Setting.SOUL_ALLY_TARGET_CAP, 4))
                    .forEach(ally -> ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                            current.conclaveTuning.integer(
                                    DeathShadowBloodMasteryTuning.Setting.SOUL_ALLY_DURATION_TICKS, 20), 0), player));
        }
        if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 26)
                && world.getTime() - active.lastBastionPulse >= active.conclaveTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_LONE_INTERVAL_TICKS, 30)) {
            active.lastBastionPulse = world.getTime();
            int absorption = active.conclaveTuning.integer(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_LONE_ABSORPTION, 4);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 40,
                    Math.max(0, absorption / 4 - 1)), player);
        }
    }

    private static void damageCollidingTargets(ServerWorld world, LivingEntity player, ItemStack stack, ActiveLanterns active, int lanternCount) {
        float damage = getLanternDamage(player, stack);
        damage *= (float) active.lanternTuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_CONTACT_DAMAGE_MULTIPLIER, 1);
        if (active.hasExtraLanterns(world)) {
            damage *= active.conclaveTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_GRAND_DAMAGE_MULTIPLIER, 1);
        }
        if (damage <= 0.0F || lanternCount <= 0) {
            return;
        }

        double orbitRadius = orbitRadius(Config.uniqueEffects.soulkeeper.orbitRadius,
                active.lanternTuning);
        double searchRadius = orbitRadius + CONTACT_RADIUS + 1.0;
        Box searchBox = player.getBoundingBox().expand(searchRadius, 2.5, searchRadius);
        Set<ContactKey> currentlyColliding = new HashSet<>();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, target -> target != player && target.isAlive())) {
            if (!HelperMethods.checkAbilityTarget(target, player)) {
                continue;
            }

            UUID targetId = target.getUuid();
            for (int lantern = 0; lantern < lanternCount; lantern++) {
                ContactKey contact = new ContactKey(targetId, lantern);
                if (!isCollidingWithLantern(player.getPos(), target.getBoundingBox(), active.orbitPhase,
                        orbitRadius, lanternCount, lantern)) continue;
                currentlyColliding.add(contact);
                if (active.conclaveTuning.flag(1 << 26) && lantern >= BASE_LANTERN_COUNT) continue;
                long lastContact = active.lastContact.getOrDefault(contact, Long.MIN_VALUE / 2);
                int repeatDelay = active.lanternTuning.integer(
                        DeathShadowBloodMasteryTuning.Setting.SOUL_REPEAT_DELAY_TICKS, Integer.MAX_VALUE);
                if (active.collidingTargets.contains(contact)
                        && world.getTime() - lastContact < repeatDelay) continue;
                float contactDamage = contactDamage(active, damage, targetId, lantern, world.getTime());
                if (!damageTarget(world, player, target, contactDamage)) continue;
                active.successfulContacts++;
                active.lastContact.put(contact, world.getTime());
                active.lastPassage.put(targetId, new LanternPassage(lantern, world.getTime()));
                trimContactState(active);
                if (!active.lanternTuning.flag(1 << 7)) increaseSpeed(active);
                if (active.lanternTuning.flag(1 << 2)) target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.GLOWING, active.lanternTuning.integer(
                        DeathShadowBloodMasteryTuning.Setting.SOUL_GLOW_DURATION_TICKS, 40), 0), player);
                if (active.lanternTuning.flag(1 << 4)) active.soulbrandUntil.put(targetId,
                        world.getTime() + active.lanternTuning.integer(
                                DeathShadowBloodMasteryTuning.Setting.SOULBRAND_DURATION_TICKS, 40));
                if (!target.isAlive() && active.velocityTuning.flag(1 << 14)) active.capturedUntil =
                        world.getTime() + active.velocityTuning.integer(
                                DeathShadowBloodMasteryTuning.Setting.SOUL_CAPTURE_DURATION_TICKS, 60);
                applyCyclone(world, player, target, active);
                if (active.hasExtraLanterns(world) && active.conclaveTuning.flag(1 << 24)
                        && !active.recallUsed && active.extraLanternsUntilTick - world.getTime()
                        <= active.conclaveTuning.integer(
                        DeathShadowBloodMasteryTuning.Setting.SOUL_RECALL_WINDOW_TICKS, 60)) {
                    active.recallUsed = true;
                    active.extraLanternsUntilTick += active.conclaveTuning.integer(
                            DeathShadowBloodMasteryTuning.Setting.SOUL_RECALL_EXTENSION_TICKS, 80);
                }
                applyCleave(world, player, target, active, contactDamage);
                spawnLanternHitEffects(world, target);
            }
        }

        active.collidingTargets.clear();
        active.collidingTargets.addAll(currentlyColliding);
    }

    private static float getLanternDamage(LivingEntity player, ItemStack stack) {
        return HelperMethods.abilityScaledDamage("soul", player, stack,
                Config.uniqueEffects.soulkeeper.lanternDamageScaling,
                Config.uniqueEffects.soulkeeper.spellScaling);
    }

    private static boolean damageTarget(ServerWorld world, LivingEntity player, LivingEntity target, float damage) {
        target.timeUntilRegen = 0;
        boolean[] damaged = {false};
        ItemStack stack = player.getMainHandStack();
        var damageSource = player.getDamageSources().indirectMagic(player, player);
        float scaledDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage);
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(damageSource, scaledDamage));
        target.timeUntilRegen = 0;
        if (!damaged[0]) {
            float fallbackDamage = HelperMethods.applyNonPlayerAbilityDamageModifier(player, damage);
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(world.getDamageSources().magic(), fallbackDamage));
            target.timeUntilRegen = 0;
        }
        return damaged[0];
    }

    private static float contactDamage(ActiveLanterns active, float baseDamage, UUID targetId,
                                       int lantern, long now) {
        float damage = baseDamage;
        if (active.velocityTuning.flag(1 << 12) && active.speedMultiplier > 3) {
            double perSpeed = active.velocityTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_FRICTION_DAMAGE_PER_SPEED, .03);
            double cap = active.velocityTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_FRICTION_DAMAGE_CAP, .12);
            damage *= 1 + Math.min(cap, (active.speedMultiplier - 3) * perSpeed);
        }
        LanternPassage passage = active.lastPassage.get(targetId);
        if (active.lanternTuning.flag(1 << 3) && passage != null && passage.lantern != lantern
                && now - passage.at <= active.lanternTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_CROSSING_WINDOW_TICKS, 20)) {
            damage *= active.lanternTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_CROSSING_DAMAGE_MULTIPLIER, 1.2);
        }
        int shearCount = active.lanternTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_SHEAR_HIT_COUNT, 5);
        if (active.lanternTuning.flag(1 << 6) && shearCount > 0
                && (active.successfulContacts + 1) % shearCount == 0) {
            damage *= active.lanternTuning.get(
                    DeathShadowBloodMasteryTuning.Setting.SOUL_SHEAR_DAMAGE_MULTIPLIER, 1.5);
        }
        return damage;
    }

    private static void applyCyclone(ServerWorld world, LivingEntity owner, LivingEntity target,
                                     ActiveLanterns active) {
        if (!active.velocityTuning.flag(1 << 15)
                || active.speedMultiplier < maximumSpeed(
                Config.uniqueEffects.soulkeeper.maxSpeedMultiplier, active.velocityTuning)) return;
        int interval = active.velocityTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_CYCLONE_INTERVAL_TICKS, 10);
        if (world.getTime() - active.cycloneWindowStarted >= interval) {
            active.cycloneWindowStarted = world.getTime();
            active.cycloneTargets.clear();
        }
        if (active.cycloneTargets.size() >= active.velocityTuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_CYCLONE_TARGET_CAP, 8)
                || !active.cycloneTargets.add(target.getUuid())) return;
        Vec3d pull = owner.getPos().subtract(target.getPos()).multiply(1, 0, 1);
        if (pull.lengthSquared() > 0) target.addVelocity(pull.normalize().multiply(
                active.velocityTuning.get(
                        DeathShadowBloodMasteryTuning.Setting.SOUL_CYCLONE_PULL_STRENGTH, .25)));
    }

    private static void applyCleave(ServerWorld world, LivingEntity owner, LivingEntity target,
                                    ActiveLanterns active, float contactDamage) {
        if (!active.lanternTuning.flag(1 << 8)) return;
        double radius = active.lanternTuning.get(DeathShadowBloodMasteryTuning.Setting.SOUL_CLEAVE_RADIUS, 1.5);
        float cleaveDamage = contactDamage / Math.max(.01F, (float) active.lanternTuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_CONTACT_DAMAGE_MULTIPLIER, .75))
                * (float) active.lanternTuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_CLEAVE_DAMAGE_MULTIPLIER, .6);
        Vec3d center = target.getPos().add(0, target.getHeight() * .5, 0);
        world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius),
                        other -> other != target && other != owner && other.isAlive()
                                && HelperMethods.checkAbilityTarget(other, owner)
                                && other.getPos().add(0, other.getHeight() * .5, 0)
                                .squaredDistanceTo(center) <= radius * radius)
                .stream().limit(active.lanternTuning.integer(
                        DeathShadowBloodMasteryTuning.Setting.SOUL_CLEAVE_TARGET_CAP, 2))
                .forEach(other -> damageTarget(world, owner, other, cleaveDamage));
    }

    private static boolean isCollidingWithLantern(Vec3d center, Box targetBox, double baseAngle,
                                                   double orbitRadius, int lanternCount, int lantern) {
        return squaredDistanceToBox(lanternPosition(center, baseAngle, orbitRadius, lanternCount, lantern),
                targetBox) <= CONTACT_RADIUS * CONTACT_RADIUS;
    }

    private static Vec3d lanternPosition(Vec3d center, double baseAngle, double orbitRadius,
                                         int lanternCount, int lantern) {
        double angle = baseAngle + ((Math.PI * 2.0) / lanternCount) * lantern;
        double bob = Math.sin(baseAngle * (BOB_SPEED / BASE_ANGULAR_SPEED) + lantern * 1.7) * BOB_AMPLITUDE;
        return center.add(Math.cos(angle) * orbitRadius, LANTERN_HEIGHT + .5 + bob,
                Math.sin(angle) * orbitRadius);
    }

    private static boolean intersectsExtraLantern(Vec3d center, Vec3d projectile, double baseAngle,
                                                   double orbitRadius, int lanternCount, double radius) {
        for (int lantern = BASE_LANTERN_COUNT; lantern < lanternCount; lantern++) {
            if (projectile.squaredDistanceTo(lanternPosition(
                    center, baseAngle, orbitRadius, lanternCount, lantern)) <= radius * radius) return true;
        }
        return false;
    }

    private static boolean hostileProjectile(ProjectileEntity projectile, LivingEntity owner) {
        Entity projectileOwner = projectile.getOwner();
        return projectileOwner != owner && (!(projectileOwner instanceof LivingEntity living)
                || HelperMethods.checkAbilityTarget(living, owner));
    }

    private static void trimContactState(ActiveLanterns active) {
        if (active.lastContact.size() > 64) active.lastContact.entrySet().stream()
                .min(Map.Entry.comparingByValue()).ifPresent(entry -> active.lastContact.remove(entry.getKey()));
        if (active.lastPassage.size() > 32) active.lastPassage.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().at))
                .ifPresent(entry -> active.lastPassage.remove(entry.getKey()));
        if (active.soulbrandUntil.size() > 32) active.soulbrandUntil.entrySet().stream()
                .min(Map.Entry.comparingByValue()).ifPresent(entry -> active.soulbrandUntil.remove(entry.getKey()));
    }

    private static double squaredDistanceToBox(Vec3d point, Box box) {
        double dx = distanceToRange(point.x, box.minX, box.maxX);
        double dy = distanceToRange(point.y, box.minY, box.maxY);
        double dz = distanceToRange(point.z, box.minZ, box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceToRange(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0;
    }

    private static void increaseSpeed(LivingEntity player) {
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(player.getUuid(), ignored -> new ActiveLanterns());
        increaseSpeed(active);
    }

    private static void increaseSpeed(ActiveLanterns active) {
        float maximum = (float) maximumSpeed(Config.uniqueEffects.soulkeeper.maxSpeedMultiplier,
                active.velocityTuning);
        float gain = (float) speedGain(Config.uniqueEffects.soulkeeper.speedIncreasePerHit,
                active.velocityTuning);
        float previous = active.speedMultiplier;
        active.speedMultiplier = Math.min(maximum, active.speedMultiplier + gain);
        double threshold = active.velocityTuning.get(DeathShadowBloodMasteryTuning.Setting.SOUL_RUSH_THRESHOLD, 4);
        if (active.velocityTuning.flag(1 << 13) && previous < threshold
                && active.speedMultiplier >= threshold && active.owner != null) {
            active.owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,
                    active.velocityTuning.integer(
                            DeathShadowBloodMasteryTuning.Setting.SOUL_RUSH_DURATION_TICKS, 40), 0), active.owner);
        }
    }

    private static void refreshTunings(ServerWorld world, LivingEntity owner, ItemStack stack, ActiveLanterns active) {
        refreshTunings(world, owner, stack, active, false);
    }

    private static void refreshTunings(ServerWorld world, LivingEntity owner, ItemStack stack,
                                       ActiveLanterns active, boolean force) {
        if (!force && world.getTime() < active.refreshAt) return;
        active.refreshAt = world.getTime() + 20;
        active.owner = owner;
        UniqueAbilityExecution lanterns = DeathShadowBloodMasteryCombatManager.beginPassive(
                DeathShadowBloodMasteryAbilities.SOULKEEPER_LANTERNS, world, stack, owner, null);
        active.lanternTuning = DeathShadowBloodMasteryAbilities.tuning(lanterns);
        UniqueAbilityApi.finish(lanterns, DeathShadowBloodMasteryAbilities.FINISH, 0);
        UniqueAbilityExecution velocity = DeathShadowBloodMasteryCombatManager.beginPassive(
                DeathShadowBloodMasteryAbilities.SOULKEEPER_VELOCITY, world, stack, owner, null);
        active.velocityTuning = DeathShadowBloodMasteryAbilities.tuning(velocity);
        UniqueAbilityApi.finish(velocity, DeathShadowBloodMasteryAbilities.FINISH, 0);
    }

    private static void spawnSoulDetonationEffects(ServerWorld world, LivingEntity actor, double radius) {
        Vec3d ringCenter = actor.getPos().add(0, 0.15, 0);
        double effectRadius = Math.max(0.5, radius);
        for (int point = 0; point < 32; point++) {
            double angle = Math.PI * 2 * point / 32;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    ringCenter.x + Math.cos(angle) * effectRadius,
                    ringCenter.y,
                    ringCenter.z + Math.sin(angle) * effectRadius,
                    1, 0.03, 0.06, 0.03, 0.005);
        }
        world.spawnParticles(ParticleTypes.SCULK_SOUL, actor.getX(), actor.getBodyY(0.5), actor.getZ(),
                18, 0.45, 0.55, 0.45, 0.04);
        world.playSound(null, actor.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(),
                SoundCategory.PLAYERS, 0.9F, 0.7F);
        world.playSound(null, actor.getX(), actor.getY(), actor.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE,
                SoundCategory.PLAYERS, 0.8F, 0.65F);
    }

    private static void spawnLanternHitEffects(ServerWorld world, LivingEntity target) {
        Vec3d pos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 4, 0.18, 0.18, 0.18, 0.025);
    }

    private static boolean isHoldingSoulkeeper(LivingEntity player) {
        return player != null && player.getMainHandStack().isOf(ItemsRegistry.SOULKEEPER.get());
    }

    private static LivingEntity resolveOwner(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        if (world.getEntity(ownerId) instanceof LivingEntity owner) {
            return owner;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerId);
        return player != null ? player : null;
    }

    private static SoulkeeperLanternVisualEntity resolveTracked(ServerWorld world, UUID visualId, UUID ownerId) {
        if (visualId == null) {
            return null;
        }

        Entity entity = world.getEntity(visualId);
        if (!(entity instanceof SoulkeeperLanternVisualEntity visual) || !ownerId.equals(visual.getOwnerUuid()) || !visual.isAlive()) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }
        return visual;
    }

    private static void discardActive(ServerWorld world, UUID ownerId) {
        ActiveLanterns active = ACTIVE_LANTERNS.get(ownerId);
        if (active == null || active.visualId == null) {
            return;
        }
        Entity entity = world.getEntity(active.visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static ActiveLanterns activeFor(ServerWorld world, UUID ownerId) {
        ActiveLanterns active = ACTIVE_LANTERNS.computeIfAbsent(ownerId, ignored -> new ActiveLanterns());
        if (active.world == null || active.world == world) {
            return active;
        }
        discardActive(active.world, ownerId);
        ActiveLanterns replacement = new ActiveLanterns();
        ACTIVE_LANTERNS.put(ownerId, replacement);
        return replacement;
    }

    public static double orbitRadius(double configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(.25, (configured + tuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_ORBIT_RADIUS_BONUS, 0)) * tuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_ORBIT_RADIUS_MULTIPLIER, 1));
    }

    public static double speedGain(double configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(0, configured * tuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_SPEED_GAIN_MULTIPLIER, 1));
    }

    public static double speedDecay(double configured, DeathShadowBloodMasteryTuning tuning) {
        return Math.max(0, configured * tuning.get(
                DeathShadowBloodMasteryTuning.Setting.SOUL_SPEED_DECAY_MULTIPLIER, 1));
    }

    public static double maximumSpeed(double configured, DeathShadowBloodMasteryTuning tuning) {
        double maximum = configured + tuning.get(DeathShadowBloodMasteryTuning.Setting.SOUL_MAX_SPEED_BONUS, 0);
        if (tuning.flag(1 << 16)) maximum = Math.min(maximum,
                tuning.get(DeathShadowBloodMasteryTuning.Setting.SOUL_CEASELESS_MAX_SPEED, 4));
        return Math.max(1, maximum);
    }

    public static int extraLanternDuration(int configured, DeathShadowBloodMasteryTuning tuning) {
        int bonus = tuning.integer(DeathShadowBloodMasteryTuning.Setting.SOUL_EXTRA_DURATION_BONUS_TICKS, 0);
        if (tuning.flag(1 << 25)) return Math.max(1,
                tuning.integer(DeathShadowBloodMasteryTuning.Setting.SOUL_GRAND_DURATION_TICKS, 180) + bonus);
        if (tuning.flag(1 << 26)) return Math.max(1,
                tuning.integer(DeathShadowBloodMasteryTuning.Setting.SOUL_LONE_DURATION_TICKS, 220) + bonus);
        return Math.max(1, configured + bonus);
    }

    public static int activeLanternCount(DeathShadowBloodMasteryTuning tuning, boolean fifthLanternActive) {
        if (tuning.flag(1 << 25)) return tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_GRAND_LANTERN_COUNT, 6);
        if (tuning.flag(1 << 26)) return tuning.integer(
                DeathShadowBloodMasteryTuning.Setting.SOUL_LONE_LANTERN_COUNT, 3);
        return ACTIVE_LANTERN_COUNT + (fifthLanternActive ? 1 : 0);
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        List<UUID> owners = ACTIVE_LANTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().world == world).map(Map.Entry::getKey).toList();
        owners.forEach(owner -> {
            discardActive(world, owner);
            ACTIVE_LANTERNS.remove(owner);
        });
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        if (actor.getWorld() instanceof ServerWorld world) discardActive(world, actor.getUuid());
        ACTIVE_LANTERNS.remove(actor.getUuid());
    }

    public static void clearAll() {
        for (Map.Entry<UUID, ActiveLanterns> entry : new ArrayList<>(ACTIVE_LANTERNS.entrySet())) {
            if (entry.getValue().world != null) discardActive(entry.getValue().world, entry.getKey());
        }
        ACTIVE_LANTERNS.clear();
    }

    private static final class ActiveLanterns {
        private UUID visualId;
        private float speedMultiplier = 1.0F;
        private double orbitPhase;
        private long extraLanternsUntilTick;
        private long fifthLanternUntilTick;
        private long refreshAt;
        private long capturedUntil;
        private long lastBastionPulse;
        private long lastIntercept;
        private long cycloneWindowStarted;
        private int successfulContacts;
        private boolean recallUsed;
        private LivingEntity owner;
        private ServerWorld world;
        private final Map<ContactKey, Long> lastContact = new HashMap<>();
        private final Map<UUID, LanternPassage> lastPassage = new HashMap<>();
        private final Map<UUID, Long> soulbrandUntil = new HashMap<>();
        private final Set<UUID> cycloneTargets = new HashSet<>();
        private DeathShadowBloodMasteryTuning lanternTuning = DeathShadowBloodMasteryTuning.EMPTY;
        private DeathShadowBloodMasteryTuning velocityTuning = DeathShadowBloodMasteryTuning.EMPTY;
        private DeathShadowBloodMasteryTuning conclaveTuning = DeathShadowBloodMasteryTuning.EMPTY;
        private final Set<ContactKey> collidingTargets = new HashSet<>();

        private void tick(ServerWorld world) {
            this.world = world;
            orbitPhase += BASE_ANGULAR_SPEED * speedMultiplier;
            while (orbitPhase > Math.PI * 2.0) {
                orbitPhase -= Math.PI * 2.0;
            }
            if (speedMultiplier > 1.0F) {
                if (world.getTime() >= capturedUntil) speedMultiplier = Math.max(1.0F,
                        speedMultiplier - (float) speedDecay(
                                Config.uniqueEffects.soulkeeper.speedLossPerSecond, velocityTuning) / 20.0F);
            }
            if (velocityTuning.flag(1 << 16)) speedMultiplier = Math.max(
                    (float) velocityTuning.get(DeathShadowBloodMasteryTuning.Setting.SOUL_CEASELESS_MIN_SPEED, 3),
                    Math.min((float) maximumSpeed(Config.uniqueEffects.soulkeeper.maxSpeedMultiplier,
                            velocityTuning), speedMultiplier));
            if (extraLanternsUntilTick > 0L && world.getTime() > extraLanternsUntilTick) {
                extraLanternsUntilTick = 0L;
                fifthLanternUntilTick = 0L;
            }
        }

        private boolean hasExtraLanterns(ServerWorld world) {
            return extraLanternsUntilTick > world.getTime();
        }

        private int lanternCount(ServerWorld world) {
            if (!hasExtraLanterns(world)) return BASE_LANTERN_COUNT;
            return activeLanternCount(conclaveTuning, fifthLanternUntilTick > world.getTime());
        }
    }

    private record ContactKey(UUID targetId, int lantern) {
    }

    private record LanternPassage(int lantern, long at) {
    }
}
