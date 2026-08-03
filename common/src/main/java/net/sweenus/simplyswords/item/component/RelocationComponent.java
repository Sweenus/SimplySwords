package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record RelocationComponent(double relocateX, double relocateY, double relocateZ, UUID relocateTarget, int relocationTimer, boolean canRelocate) {

	public boolean ready() {
		return relocationTimer <= 0 && canRelocate;
	}

	public boolean almostReady() {
		return relocationTimer == 40;
	}

	public RelocationComponent tickDown() {
		if (DEFAULT.equals(this)) return this;
		return new RelocationComponent(relocateX, relocateY, relocateZ, relocateTarget, Math.max(0, relocationTimer - 1), canRelocate);
	}

	public RelocationComponent clear() {
		return DEFAULT;
	}

	// Fixed sentinel, not UUID.randomUUID() — see TargetedLocationComponent.
	public static RelocationComponent DEFAULT = new RelocationComponent(0.0, 0.0, 0.0, new UUID(0L, 0L), 0, false);

	public static Codec<RelocationComponent> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.DOUBLE.fieldOf("x").forGetter(RelocationComponent::relocateX),
						Codec.DOUBLE.fieldOf("y").forGetter(RelocationComponent::relocateY),
						Codec.DOUBLE.fieldOf("z").forGetter(RelocationComponent::relocateZ),
						Uuids.CODEC.fieldOf("target").forGetter(RelocationComponent::relocateTarget),
						Codec.INT.fieldOf("timer").forGetter(RelocationComponent::relocationTimer),
						Codec.BOOL.fieldOf("canRelocate").forGetter(RelocationComponent::canRelocate)
				).apply(instance, RelocationComponent::new));

	public static PacketCodec<RegistryByteBuf, RelocationComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.DOUBLE,
			RelocationComponent::relocateX,
			PacketCodecs.DOUBLE,
			RelocationComponent::relocateY,
			PacketCodecs.DOUBLE,
			RelocationComponent::relocateZ,
			PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
			RelocationComponent::relocateTarget,
			PacketCodecs.INTEGER,
			RelocationComponent::relocationTimer,
			PacketCodecs.BOOL,
			RelocationComponent::canRelocate,
			RelocationComponent::new
	);
}