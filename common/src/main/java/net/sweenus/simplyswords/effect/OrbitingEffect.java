package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.WeakHashMap;

public abstract class OrbitingEffect extends StatusEffect {
    protected ParticleEffect particleType = ParticleTypes.SMOKE; // Default particle type

    // 1.21 removed LivingEntity from onRemoved, but we still need this for some effects. Using WeakHashMap for this to store and retrieve entity.
    private static final WeakHashMap<LivingEntity, AttributeContainer> entityAttributeMap = new WeakHashMap<>();

    public OrbitingEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
    }

    private double currentAngle = 0.0;

    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
            Vec3d center = livingEntity.getPos().add(0, livingEntity.getHeight() / 2.0, 0); // Center around the entity's waist
            double baseRadius = 1.0; // base radius for the first orbit
            double speed = Math.PI / 8; // Control the speed of the orbit

            // Spawn orbits equal to the amplifier of the status effect
            for (int i = 0; i <= amplifier; i++) {
                double radius = baseRadius + (i * 0.05); // Increment the radius for each orbit
                // Offset each orbit's starting angle
                double angleOffset = i * (Math.PI / 4);
                double verticalOffset = center.y;
                if (amplifier > 2)
                    verticalOffset = livingEntity.getPos().y + Math.min(i * 0.4, 3);
                if (amplifier > 2)
                    radius = 0.3 + (i * 0.1);
                // Calculate the x and z coordinates on the orbit with the angle offset
                double x = center.x + radius * Math.cos(currentAngle + angleOffset);
                double z = center.z + radius * Math.sin(currentAngle + angleOffset);
                double y = verticalOffset;

                // Spawn the particle at the calculated position
                serverWorld.spawnParticles(particleType, x, y, z, 1, 0, 0, 0, 0);
            }

            // Increment the angle for the next particle
            currentAngle += speed;
            if (currentAngle >= 2 * Math.PI) {
                currentAngle -= 2 * Math.PI;
            }
        }
    }

    // Protected method to set the particle type
    protected void setParticleType(ParticleEffect particleType) {
        this.particleType = particleType;
    }

    @Override
    public void onRemoved(LivingEntity effectEntity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(effectEntity, attributes, amplifier);
    }


    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration > 0;
    }

    // Store the entity and its attribute container when the effect is applied
    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onApplied(entity, attributes, amplifier);
        entityAttributeMap.put(entity, attributes);
    }

    // Retrieve the entity from the attribute container
    protected LivingEntity getEntityFromAttributeContainer(AttributeContainer attributeContainer) {
        for (LivingEntity entity : entityAttributeMap.keySet()) {
            if (entity.getAttributes() == attributeContainer) {
                return entity;
            }
        }
        return null;
    }
}
