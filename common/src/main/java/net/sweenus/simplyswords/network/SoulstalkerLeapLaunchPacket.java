package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.SoulstalkerStrideEntity;

public final class SoulstalkerLeapLaunchPacket extends BaseS2CMessage {
    private final int strideId;
    private final byte sourceMode;
    private final Vec3d launch;

    public SoulstalkerLeapLaunchPacket(int strideId, byte sourceMode, Vec3d launch) {
        this.strideId = strideId;
        this.sourceMode = sourceMode;
        this.launch = launch;
    }

    public SoulstalkerLeapLaunchPacket(PacketByteBuf buf) {
        this(buf.readVarInt(), buf.readByte(),
                new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.SOULSTALKER_LEAP_LAUNCH;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(strideId);
        buf.writeByte(sourceMode);
        buf.writeDouble(launch.x);
        buf.writeDouble(launch.y);
        buf.writeDouble(launch.z);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            PlayerEntity player = context.getPlayer();
            if (player != null && player.getVehicle() instanceof SoulstalkerStrideEntity stride
                    && stride.getId() == strideId && player.getUuid().equals(stride.getOwnerUuid())) {
                stride.applyNetworkLeap(sourceMode, launch);
            }
        });
    }
}
