package net.sweenus.simplyswords.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObserverStatusEffectsPacket implements CustomPayload {

    private static final int MAX_ENTRIES = 65536;
    public static final CustomPayload.Id<ObserverStatusEffectsPacket> ID =
            new CustomPayload.Id<>(Identifier.of(SimplySwords.MOD_ID, "observer_status_effects"));
    public static final PacketCodec<RegistryByteBuf, ObserverStatusEffectsPacket> CODEC =
            PacketCodec.of(ObserverStatusEffectsPacket::write, ObserverStatusEffectsPacket::new);

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

    public ObserverStatusEffectsPacket(RegistryByteBuf buf) {
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
    public CustomPayload.Id<ObserverStatusEffectsPacket> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buf) {
        buf.writeIdentifier(dimensionId);
        buf.writeLong(serverWorldTime);
        buf.writeBoolean(replaceAll);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            entry.write(buf);
        }
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

        private static Entry read(RegistryByteBuf buf) {
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

        private void write(RegistryByteBuf buf) {
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
