package net.sweenus.simplyswords.item.interfaces;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.util.HelperMethods;

public interface UniqueWeaponActiveAbility {

    default boolean canActivate(WeaponAbilityContext context) {
        if (context != null && context.sourcePlayer() != null) {
            return canActivateFromEntity(context.stack(), context.world(), context.sourcePlayer(), context.actor(), context.target());
        }
        if (context == null
                || context.stack() == null
                || context.stack().isEmpty()
                || context.world() == null
                || context.actor() == null
                || !context.actor().isAlive()
                || context.target() == null
                || !context.target().isAlive()
                || context.stack().getDamage() >= context.stack().getMaxDamage() - 1
                || !HelperMethods.checkFriendlyFire(context.target(), context.actor())) {
            return false;
        }
        return context.sourcePlayer() == null
                || context.target() != context.sourcePlayer()
                && HelperMethods.checkFriendlyFire(context.target(), context.sourcePlayer());
    }

    default boolean activate(WeaponAbilityContext context) {
        if (context != null && context.sourcePlayer() != null) {
            return activateFromEntity(context.stack(), context.world(), context.sourcePlayer(), context.actor(), context.target());
        }
        return false;
    }

    default int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return getEntityActivationCooldownTicks(stack);
    }

    @Deprecated
    default boolean canActivateFromEntity(ItemStack stack, ServerWorld world, ServerPlayerEntity owner,
                                          LivingEntity actor, LivingEntity target) {
        return stack != null
                && !stack.isEmpty()
                && world != null
                && owner != null
                && owner.isAlive()
                && actor != null
                && actor.isAlive()
                && target != null
                && target.isAlive()
                && stack.getDamage() < stack.getMaxDamage() - 1
                && HelperMethods.checkFriendlyFire(target, actor)
                && target != owner
                && HelperMethods.checkFriendlyFire(target, owner);
    }

    @Deprecated
    default boolean activateFromEntity(ItemStack stack, ServerWorld world, ServerPlayerEntity owner,
                                       LivingEntity actor, LivingEntity target) {
        return false;
    }

    @Deprecated
    default int getEntityActivationCooldownTicks(ItemStack stack) {
        return 20;
    }
}
