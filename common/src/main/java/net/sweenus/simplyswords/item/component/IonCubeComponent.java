package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record IonCubeComponent(int cubes, int rechargeTicks) {

    public static final int MAX_CUBES = 3;
    public static final IonCubeComponent DEFAULT = new IonCubeComponent(0, 0);

    public static final Codec<IonCubeComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("cubes").forGetter(IonCubeComponent::cubes),
            Codec.INT.optionalFieldOf("recharge_ticks", 0).forGetter(IonCubeComponent::rechargeTicks)
    ).apply(instance, IonCubeComponent::new));

    public static final PacketCodec<RegistryByteBuf, IonCubeComponent> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, IonCubeComponent::cubes,
            PacketCodecs.VAR_INT, IonCubeComponent::rechargeTicks,
            IonCubeComponent::new
    );

    public IonCubeComponent {
        cubes = Math.clamp(cubes, 0, MAX_CUBES);
        rechargeTicks = cubes >= MAX_CUBES ? 0 : Math.max(0, rechargeTicks);
    }

    public IonCubeComponent consume() {
        return cubes <= 0 ? this : new IonCubeComponent(cubes - 1, rechargeTicks);
    }
}
