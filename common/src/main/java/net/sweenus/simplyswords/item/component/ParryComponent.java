package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record ParryComponent(boolean parried, int parrySuccession, int stormChargeCapacity) {

	public ParryComponent(boolean parried, int parrySuccession) {
		this(parried, parrySuccession, 0);
	}

	public ParryComponent gainStormCharges(int amount, int maxCharges) {
		int capacity = Math.max(0, maxCharges);
		return new ParryComponent(true, Math.clamp(Math.max(0, parrySuccession) + Math.max(0, amount), 0, capacity), capacity);
	}

	public ParryComponent gainBlockedStormCharges(int amount, int maxCharges) {
		int capacity = Math.max(0, maxCharges);
		return new ParryComponent(false, Math.clamp(Math.max(0, parrySuccession) + Math.max(0, amount), 0, capacity), capacity);
	}

	public ParryComponent consumeStormCharge() {
		return new ParryComponent(false, Math.max(0, parrySuccession - 1), stormChargeCapacity);
	}

	public int stormCharges() {
		return Math.max(0, parrySuccession);
	}

	public int effectiveStormChargeCapacity(int fallback) {
		return stormChargeCapacity > 0 ? stormChargeCapacity : Math.max(1, fallback);
	}

	public ParryComponent withStormChargeCapacity(int maxCharges) {
		int capacity = Math.max(0, maxCharges);
		return new ParryComponent(parried, Math.clamp(Math.max(0, parrySuccession), 0, capacity), capacity);
	}

	public ParryComponent resetFull() {
		return new ParryComponent(false, parrySuccession, stormChargeCapacity);
	}

	public ParryComponent resetParry() {
		return new ParryComponent(false, parrySuccession, stormChargeCapacity);
	}

	public static ParryComponent DEFAULT = new ParryComponent(false, 0, 0);

	public static Codec<ParryComponent> CODEC = RecordCodecBuilder.create( instance ->
			instance.group(
					Codec.BOOL.fieldOf("parried").forGetter(ParryComponent::parried),
					Codec.INT.fieldOf("succession").forGetter(ParryComponent::parrySuccession),
					Codec.INT.optionalFieldOf("storm_charge_capacity", 0).forGetter(ParryComponent::stormChargeCapacity)
			).apply(instance, ParryComponent::new));

	public static PacketCodec<RegistryByteBuf, ParryComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL,
			ParryComponent::parried,
			PacketCodecs.INTEGER,
			ParryComponent::parrySuccession,
			PacketCodecs.INTEGER,
			ParryComponent::stormChargeCapacity,
			ParryComponent::new
	);

}
