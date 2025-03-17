package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class AstralShiftEffect extends StatusEffect {
    public AstralShiftEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }
    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (!entity.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) entity.getWorld();

            if (entity instanceof PlayerEntity player) {
                if (player.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT))) {
                    StatusEffectInstance effectInstance = player.getStatusEffect(EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT));
                    if (effectInstance != null && effectInstance.getDuration() < 10) {

                        double x = entity.getX();
                        double y = entity.getY();
                        double z = entity.getZ();
                        float damageMulti = Config.uniqueEffects.caelestis.damageModifier;
                        float damageMax = Config.uniqueEffects.caelestis.damageMax;
                        double radius = 8;

                        float damage = Math.min((amplifier) * damageMulti, damageMax);

                        world.playSound(null, entity.getBlockPos(), SoundRegistry.DARK_ACTIVATION_DISTORTED.get(),
                                entity.getSoundCategory(), 0.5f, 1.1f);

                        DamageSource damageSource = entity.getDamageSources().playerAttack(player);
                        Box box = new Box(x - radius, y - 1, z - radius, x + radius, y + 1, z + radius);
                        List<Entity> nearbyEntities = world.getOtherEntities(entity, box, EntityPredicates.VALID_LIVING_ENTITY);

                        for (Entity nearbyEntity : nearbyEntities) {
                            if (nearbyEntity instanceof LivingEntity target && HelperMethods.checkFriendlyFire(target, player)) {
                                if (target instanceof PlayerEntity) {
                                    damageSource = entity.getDamageSources().playerAttack(player);
                                }
                                target.timeUntilRegen = 0;
                                HelperMethods.applyDamageWithoutKnockback(target, damageSource, damage);
                                target.timeUntilRegen = 0;
                                HelperMethods.spawnRainingParticles(world, ParticleTypes.ENCHANT, target, 4, 2);
                                HelperMethods.spawnRainingParticles(world, ParticleTypes.WARPED_SPORE, target, 4, 2);
                                HelperMethods.spawnOrbitParticles(world, target.getPos(), ParticleTypes.WARPED_SPORE, 0.5, 6);
                                HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.SMOKE, player, target, 15);
                                HelperMethods.spawnRainingParticles(world, ParticleTypes.EXPLOSION, target, 2, 1);
                            }
                        }
                        entity.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT));
                    }
                }
            }
        }

        super.applyUpdateEffect(entity, amplifier);
        return true;
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }
}