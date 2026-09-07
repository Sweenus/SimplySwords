package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.ChompolotlMasteryManager;

public final class ChompolotlMountLeapPacket extends BaseC2SMessage {

    private final int mountId;

    public ChompolotlMountLeapPacket(int mountId) {
        this.mountId = mountId;
    }

    public ChompolotlMountLeapPacket(RegistryByteBuf buf) {
        this(buf.readVarInt());
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.CHOMPOLOTL_MOUNT_LEAP;
    }

    @Override
    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(mountId);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            ChompolotlMasteryManager.leap(player, mountId);
        }
    }
}
