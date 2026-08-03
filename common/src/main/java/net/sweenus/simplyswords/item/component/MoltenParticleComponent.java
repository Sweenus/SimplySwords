package net.sweenus.simplyswords.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

public record MoltenParticleComponent(ParticleEffect walk, ParticleEffect sprint, ParticleEffect passive) {

	public static MoltenParticleComponent DEFAULT = new MoltenParticleComponent(ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA, ParticleTypes.SMOKE);

	public static Codec<MoltenParticleComponent> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					ParticleTypes.TYPE_CODEC.fieldOf("walk").forGetter(MoltenParticleComponent::walk),
					ParticleTypes.TYPE_CODEC.fieldOf("sprint").forGetter(MoltenParticleComponent::sprint),
					ParticleTypes.TYPE_CODEC.fieldOf("passive").forGetter(MoltenParticleComponent::passive)
			).apply(instance, MoltenParticleComponent::new));

}