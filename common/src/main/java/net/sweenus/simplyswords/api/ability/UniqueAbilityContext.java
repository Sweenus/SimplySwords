package net.sweenus.simplyswords.api.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record UniqueAbilityContext(
        ServerWorld world,
        ItemStack stack,
        LivingEntity actor,
        @Nullable ServerPlayerEntity sourcePlayer,
        @Nullable LivingEntity target,
        @Nullable Hand hand,
        Vec3d origin,
        Vec3d facing,
        @Nullable WeaponAbilityActivationSource activationSource
) {
    public UniqueAbilityContext {
        Objects.requireNonNull(world);
        Objects.requireNonNull(stack);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(facing);
    }

    public static UniqueAbilityContext active(WeaponAbilityContext context) {
        return new UniqueAbilityContext(context.world(), context.stack(), context.actor(), context.sourcePlayer(),
                context.target(), context.hand(), context.origin(), context.facing(), context.activationSource());
    }

    public static UniqueAbilityContext passive(ServerWorld world, ItemStack stack, LivingEntity actor,
                                               @Nullable LivingEntity target, @Nullable Hand hand) {
        ServerPlayerEntity sourcePlayer = actor instanceof ServerPlayerEntity player ? player : null;
        return new UniqueAbilityContext(world, stack, actor, sourcePlayer, target, hand,
                actor.getPos(), actor.getRotationVec(1.0F), null);
    }
}
