package net.sweenus.simplyswords.item.interfaces;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.WeaponManaCost;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityManager;

public interface UniqueWeaponActiveAbility {

    default TypedActionResult<ItemStack> useFromDefaultInput(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!AwakeningApi.isAbilityUnlocked(stack)) {
            return TypedActionResult.pass(stack);
        }
        if (PlayerWeaponAbilityManager.shouldSkipDefaultAbilityUse(world, user, hand, stack)) {
            return TypedActionResult.pass(stack);
        }
        if (!WeaponManaCost.canAfford(user, stack)) {
            return TypedActionResult.fail(stack);
        }
        TypedActionResult<ItemStack> result = startPlayerAbility(world, user, hand);
        // Charge weapons pay on release instead, so a canceled draw costs nothing.
        if (result.getResult().isAccepted() && !chargesManaOnRelease()) {
            WeaponManaCost.spend(user, stack);
        }
        return result;
    }

    default boolean chargesManaOnRelease() {
        return false;
    }

    default TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack == null || stack.isEmpty()
                || !AwakeningApi.isAbilityUnlocked(stack)
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return TypedActionResult.fail(stack);
        }
        if (!world.isClient && world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            LivingEntity target = null;
            Entity targeted = HelperMethods.getTargetedEntity(serverPlayer, 24.0);
            if (targeted instanceof LivingEntity livingTarget && HelperMethods.checkAbilityTarget(livingTarget, serverPlayer)) {
                target = livingTarget;
            } else {
                target = HelperMethods.findClosestTarget(serverPlayer, 8.0, 8.0)
                        .filter(fallback -> fallback.getWorld() == serverWorld && HelperMethods.checkAbilityTarget(fallback, serverPlayer))
                        .orElse(null);
            }
            WeaponAbilityContext context = WeaponAbilityContext.of(
                    serverWorld,
                    stack,
                    serverPlayer,
                    null,
                    target,
                    hand,
                    net.sweenus.simplyswords.api.WeaponAbilityActivationSource.PLAYER
            );
            return net.sweenus.simplyswords.api.SimplySwordsAPI.tryActivateWeaponAbility(context)
                    ? TypedActionResult.success(stack, false)
                    : TypedActionResult.fail(stack);
        }
        return TypedActionResult.success(stack, true);
    }

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

    default boolean usesSpellCooldownReduction(ItemStack stack) {
        return true;
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
