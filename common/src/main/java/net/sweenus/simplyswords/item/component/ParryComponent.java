package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record ParryComponent(boolean parried, int parrySuccession) {

	public ParryComponent success() {
		return new ParryComponent(true, Math.max(20, parrySuccession + 1));
	}

	public ParryComponent resetFull() {
		return DEFAULT;
	}

	public ParryComponent resetParry() {
		return new ParryComponent(false, parrySuccession);
	}

	public static ParryComponent DEFAULT = new ParryComponent(false, 0);

	public static Codec<ParryComponent> CODEC = RecordCodecBuilder.create( instance ->
			instance.group(
					Codec.BOOL.fieldOf("parried").forGetter(ParryComponent::parried),
					Codec.INT.fieldOf("succession").forGetter(ParryComponent::parrySuccession)
			).apply(instance, ParryComponent::new));

	public static PacketCodec<RegistryByteBuf, ParryComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.BOOLEAN,
			ParryComponent::parried,
			PacketCodecs.INTEGER,
			ParryComponent::parrySuccession,
			ParryComponent::new
	);

}
