package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;

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

    public RelicAttunementComponent {
        route = net.minecraft.util.math.MathHelper.clamp(route, NONE, HARBINGER);
    }

    public boolean isSun() {
        return route == SUN;
    }

    public boolean isHarbinger() {
        return route == HARBINGER;
    }
}
