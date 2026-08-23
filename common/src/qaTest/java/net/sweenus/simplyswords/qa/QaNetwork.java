package net.sweenus.simplyswords.qa;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class QaNetwork {
    public static final int PROTOCOL = 1;
    public static final SimpleNetworkManager NETWORK = SimpleNetworkManager.create("simplyswords_qa");
    public static final MessageType CLIENT_STATUS = NETWORK.registerC2S("client_status", ClientStatus::new);
    public static final MessageType CLIENT_ACTION = NETWORK.registerS2C("client_action", ClientAction::new);

    private QaNetwork() {}

    public static void init() {}

    public static final class ClientStatus extends BaseC2SMessage {
        public final int protocol;
        public final String kind;
        public final String caseId;
        public final String detail;
        public final long clientNanos;

        public ClientStatus(int protocol, String kind, String caseId, String detail, long clientNanos) {
            this.protocol = protocol;
            this.kind = kind;
            this.caseId = caseId;
            this.detail = detail;
            this.clientNanos = clientNanos;
        }

        public ClientStatus(PacketByteBuf buf) {
            this(buf.readVarInt(), buf.readString(32), buf.readString(512), buf.readString(2048), buf.readLong());
        }

        @Override public MessageType getType() { return CLIENT_STATUS; }
        @Override public void write(PacketByteBuf buf) {
            buf.writeVarInt(protocol);
            buf.writeString(kind, 32);
            buf.writeString(caseId, 512);
            buf.writeString(detail, 2048);
            buf.writeLong(clientNanos);
        }

        @Override public void handle(NetworkManager.PacketContext context) {
            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                QaHarness.onClientStatus(player, this);
            }
        }
    }

    public static final class ClientAction extends BaseS2CMessage {
        public final String caseId;
        public final String action;
        public final int targetEntityId;
        public final int holdTicks;

        public ClientAction(String caseId, String action, int targetEntityId, int holdTicks) {
            this.caseId = caseId;
            this.action = action;
            this.targetEntityId = targetEntityId;
            this.holdTicks = holdTicks;
        }

        public ClientAction(PacketByteBuf buf) {
            this(buf.readString(512), buf.readString(32), buf.readVarInt(), buf.readVarInt());
        }

        @Override public MessageType getType() { return CLIENT_ACTION; }
        @Override public void write(PacketByteBuf buf) {
            buf.writeString(caseId, 512);
            buf.writeString(action, 32);
            buf.writeVarInt(targetEntityId);
            buf.writeVarInt(holdTicks);
        }

        @Override public void handle(NetworkManager.PacketContext context) {
            QaClient.handle(this);
        }
    }
}
