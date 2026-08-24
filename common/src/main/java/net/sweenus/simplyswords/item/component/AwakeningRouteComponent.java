package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

//
// Persistent route selected for a branching awakening family.
//
public record AwakeningRouteComponent(Identifier route) {
    public static final Codec<AwakeningRouteComponent> CODEC =
            Identifier.CODEC.xmap(AwakeningRouteComponent::new, AwakeningRouteComponent::route);
    public static final PacketCodec<RegistryByteBuf, AwakeningRouteComponent> PACKET_CODEC =
            Identifier.PACKET_CODEC.xmap(AwakeningRouteComponent::new, AwakeningRouteComponent::route).cast();
}
