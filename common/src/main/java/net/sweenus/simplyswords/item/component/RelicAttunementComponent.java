package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

//
// Legacy Dormant Relic route data retained for existing saves and advancement
// predicates. New form logic uses AwakeningRouteComponent.
//
public record RelicAttunementComponent(int route) {
    public static final int NONE = 0;
    public static final int SUN = 1;
    public static final int HARBINGER = 2;
    public static final RelicAttunementComponent UNATTUNED = new RelicAttunementComponent(NONE);

    public static final Codec<RelicAttunementComponent> CODEC =
            Codec.INT.xmap(RelicAttunementComponent::new, RelicAttunementComponent::route);
    public static final PacketCodec<RegistryByteBuf, RelicAttunementComponent> PACKET_CODEC =
            PacketCodecs.INTEGER.xmap(RelicAttunementComponent::new, RelicAttunementComponent::route).cast();

    public RelicAttunementComponent {
        route = Math.clamp(route, NONE, HARBINGER);
    }

    public boolean isSun() {
        return route == SUN;
    }

    public boolean isHarbinger() {
        return route == HARBINGER;
    }
}
