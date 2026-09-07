package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.ChompolotlMasteryManager;

public final class ChompolotlWaveChargePacket extends BaseC2SMessage {

    public static final int START = 0;
    public static final int RELEASE = 1;
    public static final int CANCEL = 2;

    private final int mountId;
    private final int action;

    public ChompolotlWaveChargePacket(int mountId, int action) {
        this.mountId = mountId;
        this.action = action;
    }

    public ChompolotlWaveChargePacket(RegistryByteBuf buf) {
        this(buf.readVarInt(), buf.readByte());
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.CHOMPOLOTL_WAVE_CHARGE;
    }

    @Override
    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(mountId);
        buf.writeByte(action);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            ChompolotlMasteryManager.charge(player, mountId, action);
        }
    }
}
