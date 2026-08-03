package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ChargedLocationComponent(int charge, double lastX, double lastY, double lastZ) {

	public ChargedLocationComponent(double lastX, double lastY, double lastZ) {
		this(0, lastX, lastY, lastZ);
	}

	public ChargedLocationComponent decrement() {
		return new ChargedLocationComponent(Math.max(0, charge - 1), lastX, lastY, lastZ);
	}

	public static ChargedLocationComponent DEFAULT = new ChargedLocationComponent(0, 0.0, 0.0, 0.0);

	public static Codec<ChargedLocationComponent> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.INT.fieldOf("charge").forGetter(ChargedLocationComponent::charge),
						Codec.DOUBLE.fieldOf("x").forGetter(ChargedLocationComponent::lastX),
						Codec.DOUBLE.fieldOf("y").forGetter(ChargedLocationComponent::lastY),
						Codec.DOUBLE.fieldOf("z").forGetter(ChargedLocationComponent::lastZ)
				).apply(instance, ChargedLocationComponent::new));


}