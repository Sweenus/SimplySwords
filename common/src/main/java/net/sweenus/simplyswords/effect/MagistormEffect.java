package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.custom.MagiscytheSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;
import java.util.Random;

public class MagistormEffect extends HighOrbitingEffect {
    public MagistormEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        setParticleType1(ParticleTypes.ENCHANT);
        setParticleType2(ParticleTypes.POOF);
        setParticleType3(ParticleTypes.CRIT);
        yOffset = 4f;
        width = 4;
    }
    @Override
    public void applyUpdateEffect(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) livingEntity.getWorld();
            double x = livingEntity.getX();
            double y = livingEntity.getY();
            double z = livingEntity.getZ();
            double radius = Config.uniqueEffects.magiscythe.radius;
            float duration = Config.uniqueEffects.magiscythe.duration;
            int frequency = Math.max(3, 10 - amplifier);

            float damage = HelperMethods.abilityScaledDamage("arcane", livingEntity, livingEntity.getMainHandStack(),
                    Config.uniqueEffects.magiscythe.damageScaling, Config.uniqueEffects.magiscythe.spellScaling);

            DamageSource damageSource =  livingEntity.getDamageSources().indirectMagic(livingEntity, livingEntity);
            if (livingEntity.age % frequency == 0) {
                Box box = new Box(x - radius, y - 1, z - radius, x + radius, y + 1, z + radius);
                List<Entity> nearbyEntities = world.getOtherEntities(livingEntity, box, EntityPredicates.VALID_LIVING_ENTITY);

                if (!nearbyEntities.isEmpty() && livingEntity.getMainHandStack().getItem() instanceof MagiscytheSwordItem) {
                    List<LivingEntity> validTargets = nearbyEntities.stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .map(entity -> (LivingEntity) entity)
                            .filter(target -> HelperMethods.checkAbilityTarget(target, livingEntity))
                            .toList();
                    if (!validTargets.isEmpty()) {
                        LivingEntity target = validTargets.get(livingEntity.getRandom().nextInt(validTargets.size()));
                        if (target instanceof PlayerEntity && livingEntity instanceof PlayerEntity player)
                            damageSource = livingEntity.getDamageSources().playerAttack(player);
                        target.timeUntilRegen = 0;
                        float enchantedDamage = HelperMethods.applyAbilityDamageEnchantments(world, livingEntity.getMainHandStack(), target, damageSource, damage);
                        HelperMethods.applyDamageWithoutKnockback(target, damageSource, enchantedDamage);
                        target.timeUntilRegen = 0;
                        HelperMethods.spawnRainingParticles(world, ParticleTypes.ENCHANT, target, 20, yOffset);
                        HelperMethods.spawnRainingParticles(world, ParticleTypes.GLOW, target, 4, yOffset);
                        HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.GLOW, 0.5, 6);
                        livingEntity.getWorld().playSoundFromEntity(null, livingEntity, SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_03.get(),
                                SoundCategory.PLAYERS, 0.1f, 1.0f + (livingEntity.getRandom().nextFloat()));

                        if (new Random().nextInt(100) < 5)
                            HelperMethods.incrementStatusEffect(livingEntity, EffectRegistry.getReference(EffectRegistry.MAGISTORM), (int) duration, 1, 10);
                    }
                }
            }
        }

        super.applyUpdateEffect(livingEntity, amplifier);
    }



    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return super.canApplyUpdateEffect(duration, amplifier);
    }

}
