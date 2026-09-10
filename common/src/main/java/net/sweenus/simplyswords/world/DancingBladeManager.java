package net.sweenus.simplyswords.world;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DancingBladeVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DancingBladeManager {

    private static final double BASE_ANGULAR_SPEED = 0.085;
    private static final double BLADE_HEIGHT = 1.15;
    private static final double BOB_AMPLITUDE = 0.12;
    private static final double BOB_SPEED = 0.16;
    private static final double CONTACT_RADIUS = 0.85;
    private static final String DANCING_BLADE_VISUAL_TAG = "simplyswords_dancing_blade_visual";
    private static final Set<CollisionKey> COLLIDING_TARGETS = new HashSet<>();

    private DancingBladeManager() {
    }

    public static boolean trySummon(LivingEntity player, ItemStack stack) {
        try (var masteryProvenanceScope = CombatProvenanceApi.scope(CombatProvenanceApi.from(stack, null))) {
        if (player == null || stack == null || stack.isEmpty() || !player.isAlive()) {
            return false;
        }

        ServerWorld world = (net.minecraft.server.world.ServerWorld) player.getWorld();
        int maxSwords = Math.max(1, Config.gemPowers.dancingBlades.maxSwords);
        int slot = firstAvailableSlot(world, player, maxSwords);
        if (slot < 0) {
            return false;
        }

        float damage = HelperMethods.gemPowerScaledDamage(SpellScalingComponents.power("dancing_blades"), player, stack,
                Config.gemPowers.dancingBlades.damageScaling,
                Config.gemPowers.dancingBlades.spellScaling);
        DancingBladeVisualEntity blade = new DancingBladeVisualEntity(
                world,
                player.getUuid(),
                player.getId(),
                stack.copy(),
                slot,
                Math.max(0.5, Config.gemPowers.dancingBlades.orbitRadius),
                world.getTime() + Math.max(1, Config.gemPowers.dancingBlades.duration),
                damage
        );
        blade.addCommandTag(DANCING_BLADE_VISUAL_TAG);
        Vec3d start = getBladePosition(player.getPos(), world.getTime(), slot, maxSwords, blade.getOrbitRadius());
        blade.setOrbitPhase((float) getOrbitPhase(world.getTime()));
        blade.setPos(player.getX(), player.getY(), player.getZ());
        boolean spawned = world.spawnEntity(blade);
        if (spawned) {
            world.spawnParticles(ParticleTypes.ENCHANT, start.x, start.y, start.z, 16, 0.25, 0.18, 0.25, 0.08);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.35F, 1.55F);
        }
        return spawned;
        }
    }

    public static void tickBlade(DancingBladeVisualEntity blade) {
        if (!(blade.getWorld() instanceof ServerWorld world)) {
            return;
        }

        LivingEntity owner = getOwner(world, blade);
        ItemStack stack = blade.getWeaponStack();
        if (owner == null || !owner.isAlive() || world.getTime() > blade.getExpiresAtTick() || stack == null || stack.isEmpty()) {
            discardBlade(blade);
            return;
        }

        int maxSwords = Math.max(1, Config.gemPowers.dancingBlades.maxSwords);
        double orbitRadius = Math.max(0.5, blade.getOrbitRadius());
        double orbitPhase = getOrbitPhase(world.getTime());
        Vec3d pos = getBladePosition(owner.getPos(), orbitPhase, blade.getOrbitSlot(), maxSwords, orbitRadius);
        blade.setPosition(owner.getX(), owner.getY(), owner.getZ());
        blade.setOwnerEntityId(owner.getId());
        blade.setOrbitPhase((float) orbitPhase);
        damageCollidingTargets(world, owner, blade, pos);
    }

    private static void damageCollidingTargets(ServerWorld world, LivingEntity owner, DancingBladeVisualEntity blade, Vec3d bladePos) {
        double damage = blade.getDamage();
        if (damage <= 0.0) {
            return;
        }

        Box searchBox = new Box(bladePos, bladePos).expand(CONTACT_RADIUS);
        Set<CollisionKey> current = new HashSet<>();
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, searchBox, target ->
                target != owner && target.isAlive() && EntityPredicates.VALID_LIVING_ENTITY.test(target))) {
            if (!HelperMethods.checkFriendlyFire(target, owner)) {
                continue;
            }

            CollisionKey key = new CollisionKey(blade.getUuid(), target.getUuid());
            current.add(key);
            if (COLLIDING_TARGETS.contains(key)) {
                continue;
            }

            damageTarget(world, owner, blade, target, (float) damage, bladePos);
        }

        COLLIDING_TARGETS.removeIf(key -> key.bladeId.equals(blade.getUuid()) && !current.contains(key));
        COLLIDING_TARGETS.addAll(current);
    }

    private static void damageTarget(ServerWorld world, LivingEntity owner, DancingBladeVisualEntity blade, LivingEntity target, float damage, Vec3d bladePos) {
        ItemStack stack = blade.getWeaponStack();
        if (stack == null || stack.isEmpty()) {
            discardBlade(blade);
            return;
        }

        target.timeUntilRegen = 0;
        var damageSource = world.getDamageSources().trident(blade, owner);
        float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage);
        boolean damaged = CombatProvenanceApi.damage(stack, blade, target, damageSource, enchantedDamage);
        target.timeUntilRegen = 0;
        if (!damaged) {
            return;
        }

        EnchantmentHelper.onTargetDamaged(world, target, world.getDamageSources().trident(blade, owner), stack);
        Item item = stack.getItem();
        if (item instanceof SwordItem) {
            item.postHit(stack, target, owner);
            blade.setItemStack(stack);
        }
        Vec3d targetPos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        Vec3d direction = targetPos.subtract(bladePos);
        blade.triggerAttackAnimation(direction.x, direction.z);
        spawnHitEffects(world, target, bladePos, direction);
    }

    private static void spawnHitEffects(ServerWorld world, LivingEntity target, Vec3d bladePos, Vec3d direction) {
        Vec3d targetPos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        Vec3d normalized = direction.lengthSquared() > 0.0001 ? direction.normalize() : new Vec3d(0.0, 0.0, 1.0);
        Vec3d slashPos = targetPos.subtract(normalized.multiply(0.25));
        Vec3d trailPos = bladePos.lerp(targetPos, 0.65);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, slashPos.x, slashPos.y, slashPos.z, 1, 0.02, 0.02, 0.02, 0.0);
        world.spawnParticles(ParticleTypes.CRIT, trailPos.x, trailPos.y, trailPos.z, 6, 0.18, 0.12, 0.18, 0.04);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, targetPos.x, targetPos.y, targetPos.z, 5, 0.2, 0.14, 0.2, 0.04);
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.35F, 1.25F);
    }

    private static int firstAvailableSlot(ServerWorld world, LivingEntity player, int maxSwords) {
        boolean[] occupied = new boolean[maxSwords];
        int active = 0;
        for (DancingBladeVisualEntity blade : getOwnedBlades(world, player)) {
            if (blade.getOrbitSlot() >= 0 && blade.getOrbitSlot() < maxSwords) {
                occupied[blade.getOrbitSlot()] = true;
            }
            active++;
        }
        if (active >= maxSwords) {
            return -1;
        }
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) {
                return i;
            }
        }
        return -1;
    }

    private static Iterable<DancingBladeVisualEntity> getOwnedBlades(ServerWorld world, LivingEntity player) {
        double radius = Math.max(8.0, Config.gemPowers.dancingBlades.orbitRadius + 6.0);
        return world.getEntitiesByClass(DancingBladeVisualEntity.class, player.getBoundingBox().expand(radius, 4.0, radius), blade ->
                player.getUuid().equals(blade.getOwnerUuid()) && blade.isAlive());
    }

    private static LivingEntity getOwner(ServerWorld world, DancingBladeVisualEntity blade) {
        UUID ownerId = blade.getOwnerUuid();
        if (ownerId == null) return null;
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static Vec3d getBladePosition(Vec3d center, long time, int slot, int maxSwords, double orbitRadius) {
        return getBladePosition(center, getOrbitPhase(time), slot, maxSwords, orbitRadius);
    }

    private static Vec3d getBladePosition(Vec3d center, double baseAngle, int slot, int maxSwords, double orbitRadius) {
        double angle = baseAngle + ((Math.PI * 2.0) / maxSwords) * slot;
        double bob = Math.sin(baseAngle * (BOB_SPEED / BASE_ANGULAR_SPEED) + slot * 1.7) * BOB_AMPLITUDE;
        return center.add(Math.cos(angle) * orbitRadius, BLADE_HEIGHT + bob, Math.sin(angle) * orbitRadius);
    }

    private static double getOrbitPhase(long time) {
        double phase = time * BASE_ANGULAR_SPEED;
        while (phase > Math.PI * 2.0) {
            phase -= Math.PI * 2.0;
        }
        return phase;
    }

    private static void discardBlade(DancingBladeVisualEntity blade) {
        COLLIDING_TARGETS.removeIf(key -> key.bladeId.equals(blade.getUuid()));
        blade.discard();
    }

    private record CollisionKey(UUID bladeId, UUID targetId) {
    }
}
