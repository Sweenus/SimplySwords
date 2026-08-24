package net.sweenus.simplyswords.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public record DelegatedWeaponHitContext(@Nullable ServerPlayerEntity owner, LivingEntity actor, Vec3d origin, Vec3d facing) {
}
