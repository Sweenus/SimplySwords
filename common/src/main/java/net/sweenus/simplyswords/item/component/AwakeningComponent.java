package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

//
// Persistent Runic Tablet charge stored on an awakenable unique weapon.
//
public record AwakeningComponent(int level) {
    public static final int MAX_LEVEL = 8;
    public static final AwakeningComponent DORMANT = new AwakeningComponent(0);
    public static final AwakeningComponent FULL = new AwakeningComponent(MAX_LEVEL);

    public static final Codec<AwakeningComponent> CODEC =
            Codec.INT.xmap(AwakeningComponent::new, AwakeningComponent::level);
    public static final PacketCodec<RegistryByteBuf, AwakeningComponent> PACKET_CODEC =
            PacketCodecs.INTEGER.xmap(AwakeningComponent::new, AwakeningComponent::level).cast();

    public AwakeningComponent {
        level = Math.clamp(level, 0, MAX_LEVEL);
    }
}
