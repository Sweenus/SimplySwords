package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;

public record StoredChargeComponent(int charge) {

	public StoredChargeComponent add(int addedCharge) {
		return new StoredChargeComponent(this.charge + addedCharge);
	}

	public StoredChargeComponent increment() {
		return new StoredChargeComponent(charge + 1);
	}

	public StoredChargeComponent decrement() {
		return new StoredChargeComponent(Math.max(0, charge - 1));
	}

	public StoredChargeComponent set(int newCharge) {
		return new StoredChargeComponent(newCharge);
	}

	public static StoredChargeComponent DEFAULT = new StoredChargeComponent(0);

	public static Codec<StoredChargeComponent> CODEC = Codec.INT.xmap(StoredChargeComponent::new, StoredChargeComponent::charge);

}