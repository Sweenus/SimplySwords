package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record ParryComponent(boolean parried, int parrySuccession) {

	public ParryComponent success() {
		return new ParryComponent(true, parrySuccession);
	}

	public ParryComponent gainStormCharges(int amount, int maxCharges) {
		return new ParryComponent(true, Math.min(Math.max(0, maxCharges), Math.max(0, parrySuccession) + Math.max(0, amount)));
	}

	public ParryComponent gainBlockedStormCharges(int amount, int maxCharges) {
		return new ParryComponent(false, Math.min(Math.max(0, maxCharges), Math.max(0, parrySuccession) + Math.max(0, amount)));
	}

	public ParryComponent consumeStormCharge() {
		return new ParryComponent(false, Math.max(0, parrySuccession - 1));
	}

	public int stormCharges() {
		return Math.max(0, parrySuccession);
	}

	public ParryComponent resetFull() {
		return new ParryComponent(false, parrySuccession);
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
			PacketCodecs.BOOL,
			ParryComponent::parried,
			PacketCodecs.INTEGER,
			ParryComponent::parrySuccession,
			ParryComponent::new
	);

}
