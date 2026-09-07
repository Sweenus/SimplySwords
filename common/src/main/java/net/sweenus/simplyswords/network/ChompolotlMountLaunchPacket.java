package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;

public final class ChompolotlMountLaunchPacket extends BaseS2CMessage {

    private final int mountId;
    private final Vec3d launch;

    public ChompolotlMountLaunchPacket(int mountId, Vec3d launch) {
        this.mountId = mountId;
        this.launch = launch;
    }

    public ChompolotlMountLaunchPacket(RegistryByteBuf buf) {
        this(buf.readVarInt(), new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.CHOMPOLOTL_MOUNT_LAUNCH;
    }

    @Override
    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(mountId);
        buf.writeDouble(launch.x);
        buf.writeDouble(launch.y);
        buf.writeDouble(launch.z);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        PlayerEntity player = context.getPlayer();
        if (player != null && player.getVehicle() instanceof SimplySwordsAxolotlEntity mount
                && mount.getId() == mountId && mount.getControllingPassenger() == player) {
            mount.applyMountLeap(launch);
        }
    }
}
