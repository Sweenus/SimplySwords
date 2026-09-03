package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.SoundHelper;
import net.sweenus.simplyswords.api.ability.StormFrostWaterMasteryTuning;
import net.sweenus.simplyswords.world.StormFrostWaterMasteryCombatManager;
import net.sweenus.simplyswords.world.TempestAbilityManager;

public class ElementalVortexEffect extends OrbitingEffect {
    public LivingEntity sourceEntity; // The player who applied the effect
    public int additionalData; // Additional integer data
    public ElementalVortexEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType(ParticleTypes.CLOUD);
    }
    public void setSourcePlayer(LivingEntity livingEntity) {
        sourceEntity = livingEntity;
    }
    public void setAdditionalData(int data) {
        additionalData = data;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) livingEntity.getWorld();
			SoundHelper.loopSound(livingEntity, SoundRegistry.AMBIENCE_WIND_LOOP.getId(), 6, 20);

            if (TempestAbilityManager.hasManagedVortex(livingEntity)) {
                if (livingEntity.age % 40 == 0) {
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0,
                            livingEntity.getHeight() / 3, 0), ParticleTypes.LAVA, .5, 4);
                    HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0,
                            livingEntity.getHeight() / 2, 0), ParticleTypes.SNOWFLAKE, 1, 6);
                }
                super.applyUpdateEffect(livingEntity, amplifier);
                return true;
            }

            if (livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX)) instanceof SimplySwordsStatusEffectInstance statusEffect) {
                sourceEntity = statusEffect.getSourceEntity();
                additionalData = statusEffect.getAdditionalData();
            }

            StormFrostWaterMasteryTuning tuning = StormFrostWaterMasteryCombatManager.tempestTuning(livingEntity);
            int interval = tuning.integer(s("INTERVAL_TICKS"), 10);
            if (livingEntity.age % interval == 0) {
                double radius = tuning.get(s("RADIUS"), 1 + (amplifier / 6.0));
                Box box = HelperMethods.createBox(livingEntity, radius);
                int affected = 0;
                int cap = tuning.has(s("SEARCH_CAP")) ? tuning.integer(s("SEARCH_CAP"), 64) : Integer.MAX_VALUE;
                for (Entity entity : serverWorld.getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, livingEntity)) {

                        if (additionalData != 0) {
                            DamageSource damageSource = livingEntity.getDamageSources().indirectMagic(livingEntity, sourceEntity);
							le.timeUntilRegen = 0;
                            float damage = HelperMethods.applyNonPlayerAbilityDamageModifier(sourceEntity,
                                    additionalData + ((float) amplifier / 2))
                                    * (float) tuning.get(s("DAMAGE_MULTIPLIER"), 1);
                            le.damage(damageSource, damage);
                        }
                        double pull = tuning.get(s("PULL_STRENGTH"), 0);
                        if (pull > 0) {
                            var direction = livingEntity.getPos().subtract(le.getPos()).multiply(1, 0, 1);
                            if (direction.lengthSquared() > .0001) {
                                le.addVelocity(direction.normalize().multiply(.08 * pull));
                                le.velocityModified = true;
                            }
                        }
                        if (++affected >= cap) break;
                    }
                }
            }

            if (livingEntity.age % 40 == 0) {
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 3), 0), ParticleTypes.LAVA, 0.5, 4);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.SNOWFLAKE, 1, 6);
                HelperMethods.spawnOrbitParticles(serverWorld, livingEntity.getPos().add(0, (livingEntity.getHeight() / 2), 0), ParticleTypes.MYCELIUM, 1.5, 8);
            }
        }
        super.applyUpdateEffect(livingEntity, amplifier);
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        LivingEntity entity = getEntityFromAttributeContainer(attributes);
        SoundHelper.stopLoopingSound(entity, SoundRegistry.AMBIENCE_WIND_LOOP.getId());
        super.onRemoved(attributes);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }

    private static StormFrostWaterMasteryTuning.Setting s(String name) {
        return StormFrostWaterMasteryTuning.Setting.valueOf(name);
    }
}
