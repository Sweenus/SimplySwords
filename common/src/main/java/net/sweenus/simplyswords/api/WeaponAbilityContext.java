package net.sweenus.simplyswords.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public record WeaponAbilityContext(ServerWorld world,
                                   ItemStack stack,
                                   LivingEntity actor,
                                   @Nullable ServerPlayerEntity sourcePlayer,
                                   @Nullable LivingEntity target,
                                   Vec3d origin,
                                   Vec3d facing,
                                   @Nullable Hand hand,
                                   WeaponAbilityActivationSource activationSource) {

    public static WeaponAbilityContext of(ServerWorld world, ItemStack stack, LivingEntity actor,
                                          @Nullable ServerPlayerEntity sourcePlayer,
                                          @Nullable LivingEntity target,
                                          @Nullable Hand hand,
                                          WeaponAbilityActivationSource activationSource) {
        Vec3d origin = actor.getPos();
        Vec3d facing = actor.getRotationVec(1.0F);
        if (facing.lengthSquared() < 0.0001 && target != null) {
            facing = target.getPos().subtract(origin);
        }
        if (facing.lengthSquared() < 0.0001) {
            facing = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        return new WeaponAbilityContext(world, stack, actor, sourcePlayer, target, origin, facing.normalize(), hand, activationSource);
    }

    public boolean isDelegated() {
        return sourcePlayer != null && sourcePlayer != actor;
    }
}
