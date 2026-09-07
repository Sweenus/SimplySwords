package net.sweenus.simplyswords.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PhoenixStandardPacket(Identifier dimensionId, ItemStack displayStack) implements CustomPayload {
    public static final Id<PhoenixStandardPacket> ID = new Id<>(Identifier.of("simplyswords", "phoenix_standard"));
    public static final PacketCodec<RegistryByteBuf, PhoenixStandardPacket> CODEC =
            PacketCodec.of(PhoenixStandardPacket::write, PhoenixStandardPacket::new);

    public PhoenixStandardPacket {
        displayStack = displayStack.copyWithCount(1);
    }

    private PhoenixStandardPacket(RegistryByteBuf buf) {
        this(buf.readIdentifier(), ItemStack.PACKET_CODEC.decode(buf));
    }

    private void write(RegistryByteBuf buf) {
        buf.writeIdentifier(dimensionId);
        ItemStack.PACKET_CODEC.encode(buf, displayStack);
    }

    @Override
    public Id<PhoenixStandardPacket> getId() {
        return ID;
    }
}
