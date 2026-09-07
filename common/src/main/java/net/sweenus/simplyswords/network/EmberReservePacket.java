package net.sweenus.simplyswords.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EmberReservePacket(Identifier dimensionId, int charges, int capacity) implements CustomPayload {
    public static final Id<EmberReservePacket> ID = new Id<>(Identifier.of("simplyswords", "ember_reserve"));
    public static final PacketCodec<RegistryByteBuf, EmberReservePacket> CODEC =
            PacketCodec.of(EmberReservePacket::write, EmberReservePacket::new);

    public EmberReservePacket {
        capacity = Math.clamp(capacity, 0, 64);
        charges = Math.clamp(charges, 0, capacity);
    }

    private EmberReservePacket(RegistryByteBuf buf) {
        this(buf.readIdentifier(), buf.readVarInt(), buf.readVarInt());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeIdentifier(dimensionId);
        buf.writeVarInt(charges);
        buf.writeVarInt(capacity);
    }

    @Override
    public Id<EmberReservePacket> getId() {
        return ID;
    }
}
