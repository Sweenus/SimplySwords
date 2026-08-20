package net.sweenus.simplyswords.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

final class BalancePlayerEntity extends ServerPlayerEntity {

    BalancePlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile, SyncedClientOptions.createDefault());
    }

    // lastAttackedTicks only advances in PlayerEntity#tick, so every swing would land at minimum charge.
    void primeAttackCharge() {
        this.lastAttackedTicks = Math.max(this.lastAttackedTicks, 200);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return true;
    }
}
