package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DragonMawHeadVisualEntity;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.Comparator;
import java.util.List;

public final class DragonMawManager {

    private DragonMawManager() {
    }

    public static void tryActivate(LivingEntity owner, ItemStack stack) {
        if (owner == null
                || stack == null
                || stack.isEmpty()
                || !owner.isAlive()
                || !(owner.getWorld() instanceof ServerWorld world)
                || hasActiveHead(world, owner)) {
            return;
        }

        double range = Math.max(0.5, Config.gemPowers.dragonMaw.range);
        Box searchBox = owner.getBoundingBox().expand(range, Math.max(2.0, range * 0.5), range);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, searchBox, target -> isValidTarget(owner, target))
                .stream()
                .sorted(Comparator.comparingDouble(owner::squaredDistanceTo))
                .toList();

        int chance = Math.clamp(Config.gemPowers.dragonMaw.chance, 0, 100);
        if (chance <= 0) {
            return;
        }

        LivingEntity selectedTarget = null;
        for (LivingEntity target : targets) {
            if (owner.getRandom().nextInt(100) < chance) {
                selectedTarget = target;
                break;
            }
        }
        if (selectedTarget == null) {
            return;
        }

        float damage = HelperMethods.gemPowerScaledDamage(SpellScalingComponents.power("dragon_maw"), owner, stack,
                Config.gemPowers.dragonMaw.damageScaling,
                Config.gemPowers.dragonMaw.spellScaling);
        if (damage <= 0.0F) {
            return;
        }

        DragonMawHeadVisualEntity head = new DragonMawHeadVisualEntity(world, owner, selectedTarget, stack, damage,
                Math.max(300, Config.gemPowers.dragonMaw.visualLifetimeTicks),
                Math.max(0, Config.gemPowers.dragonMaw.breathDelayTicks),
                Math.max(1, Config.gemPowers.dragonMaw.breathRepeatTicks),
                Math.max(0.1F, Config.gemPowers.dragonMaw.headScale),
                Math.max(0.1F, Config.gemPowers.dragonMaw.turnSpeedDegreesPerTick));
        world.spawnEntity(head);

        world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.9F, 0.75F);
        world.playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.65F, 1.35F);
    }

    public static boolean isValidTarget(LivingEntity owner, LivingEntity target) {
        if (owner == null
                || target == null
                || target == owner
                || !owner.isAlive()
                || !target.isAlive()
                || !EntityPredicates.VALID_LIVING_ENTITY.test(target)
                || !HelperMethods.checkFriendlyFire(target, owner)) {
            return false;
        }

        if (target instanceof PlayerEntity) {
            return true;
        }
        if (target instanceof HostileEntity || HelperMethods.isMonsterFaction(target)) {
            return true;
        }
        if (target instanceof MobEntity targetMob && targetMob.getTarget() == owner) {
            return true;
        }
        return owner instanceof MobEntity ownerMob && ownerMob.getTarget() == target;
    }

    private static boolean hasActiveHead(ServerWorld world, LivingEntity owner) {
        double range = Math.max(8.0, Config.gemPowers.dragonMaw.range + 4.0);
        Box searchBox = owner.getBoundingBox().expand(range, range, range);
        return !world.getEntitiesByClass(DragonMawHeadVisualEntity.class, searchBox,
                head -> head.getOwnerEntityId() == owner.getId()).isEmpty();
    }
}
