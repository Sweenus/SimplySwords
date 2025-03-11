package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class ImmolationEffect extends WideOrbitingEffect {
    public ImmolationEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
        particleType1 = ParticleTypes.CRIT;
        particleType2 = ParticleTypes.ENCHANT;
        yOffset = 15f;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            if (pLivingEntity instanceof PlayerEntity player) {
                if (pLivingEntity.age % 15 == 0) {

                    player.getWorld().playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_FLYBY_03.get(),
                            SoundCategory.PLAYERS, 0.1f, 1.0f);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), 0.3, 0.8, 0.2);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), -0.2, 0.6, 0.3);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), 0.5, 0.3, -0.2);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.SMOKE, player.getX(), player.getY()+0.5, player.getZ(), 0, 0, 0);

                    ItemStack checkMainStack = player.getMainHandStack();
                    ItemStack checkOffStack = player.getOffHandStack();

                    if (checkMainStack.getItem() instanceof SwordItem || checkOffStack.getItem() instanceof SwordItem) {
                        GemPowerComponent mainComponent = SimplySwordsAPI.getComponent(checkMainStack);
                        GemPowerComponent offComponent = SimplySwordsAPI.getComponent(checkOffStack);
                        if (!(mainComponent.hasRunic(GemPowerRegistry.IMMOLATION)
                                || offComponent.hasRunic(GemPowerRegistry.IMMOLATION)
                                || mainComponent.hasNether(GemPowerRegistry.RADIANCE)
                                || offComponent.hasNether(GemPowerRegistry.RADIANCE))) {
                            player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.IMMOLATION));
                        }
                    } else {
                        player.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.IMMOLATION));
                    }

                    float abilityDamage = (player.getHealth() / 3);

                    //Damage
                    Box box = HelperMethods.createBox(pLivingEntity, pAmplifier);
                    for (Entity entities : player.getWorld().getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                                le.timeUntilRegen = 0;
                                le.damage(player.getDamageSources().indirectMagic(player, player), abilityDamage);
                                le.setOnFireFor(1);
                                le.timeUntilRegen = 0;
                            }
                        }
                    }
                }
            }
        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);
        return true;
    }

    @Override
    public void onRemoved(AttributeContainer attributes) {
        super.onRemoved(attributes);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return super.canApplyUpdateEffect(pDuration, pAmplifier);
    }
}