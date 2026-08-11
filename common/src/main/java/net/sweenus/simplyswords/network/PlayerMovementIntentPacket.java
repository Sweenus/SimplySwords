package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.PlayerMovementIntentManager;

public final class PlayerMovementIntentPacket extends BaseC2SMessage {

    private final int forward;
    private final int strafe;

    public PlayerMovementIntentPacket(int forward, int strafe) {
        this.forward = Integer.compare(forward, 0);
        this.strafe = Integer.compare(strafe, 0);
    }

    public PlayerMovementIntentPacket(PacketByteBuf buf) {
        this(buf.readByte(), buf.readByte());
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.PLAYER_MOVEMENT_INTENT;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeByte(forward);
        buf.writeByte(strafe);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            PlayerMovementIntentManager.update(player, forward, strafe);
        }
    }
}
