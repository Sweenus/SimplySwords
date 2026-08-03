package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.client.ObserverStatusEffectClientState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObserverStatusEffectsPacket extends BaseS2CMessage {

    private static final int MAX_ENTRIES = 65536;
    private final Identifier dimensionId;
    private final long serverWorldTime;
    private final boolean replaceAll;
    private final List<Entry> entries;

    public ObserverStatusEffectsPacket(Identifier dimensionId, long serverWorldTime,
                                       boolean replaceAll, List<Entry> entries) {
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many observer status-effect entries: " + entries.size());
        }
        this.dimensionId = dimensionId;
        this.serverWorldTime = serverWorldTime;
        this.replaceAll = replaceAll;
        this.entries = List.copyOf(entries);
    }

    public ObserverStatusEffectsPacket(PacketByteBuf buf) {
        this.dimensionId = buf.readIdentifier();
        this.serverWorldTime = buf.readLong();
        this.replaceAll = buf.readBoolean();
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid observer status-effect entry count: " + size);
        }
        List<Entry> decoded = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            decoded.add(Entry.read(buf));
        }
        this.entries = List.copyOf(decoded);
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.OBSERVER_STATUS_EFFECTS;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(dimensionId);
        buf.writeLong(serverWorldTime);
        buf.writeBoolean(replaceAll);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            entry.write(buf);
        }
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> ObserverStatusEffectClientState.apply(this));
    }

    public Identifier dimensionId() {
        return dimensionId;
    }

    public long serverWorldTime() {
        return serverWorldTime;
    }

    public boolean replaceAll() {
        return replaceAll;
    }

    public List<Entry> entries() {
        return entries;
    }

    public record Entry(UUID entityId,
                        Identifier effectId,
                        boolean active,
                        int amplifier,
                        int duration,
                        boolean infinite,
                        boolean ambient,
                        boolean showParticles,
                        boolean showIcon) {

        public static Entry removed(UUID entityId, Identifier effectId) {
            return new Entry(entityId, effectId, false, 0, 0, false, false, false, false);
        }

        private static Entry read(PacketByteBuf buf) {
            UUID entityId = buf.readUuid();
            Identifier effectId = buf.readIdentifier();
            boolean active = buf.readBoolean();
            if (!active) {
                return removed(entityId, effectId);
            }
            return new Entry(
                    entityId,
                    effectId,
                    true,
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean());
        }

        private void write(PacketByteBuf buf) {
            buf.writeUuid(entityId);
            buf.writeIdentifier(effectId);
            buf.writeBoolean(active);
            if (!active) {
                return;
            }
            buf.writeVarInt(amplifier);
            buf.writeVarInt(duration);
            buf.writeBoolean(infinite);
            buf.writeBoolean(ambient);
            buf.writeBoolean(showParticles);
            buf.writeBoolean(showIcon);
        }
    }
}
