package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class ImmolationEffect extends StatusEffect {
    public ImmolationEffect(StatusEffectCategory statusEffectCategory, int color) {
        super (statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            if (pLivingEntity instanceof PlayerEntity player) {
                if (pLivingEntity.age % 40 == 0) {
                    if (!player.isCreative() && player.getHealth() > 4f)
                        pLivingEntity.setHealth(pLivingEntity.getHealth() - 0.5f);

                    player.getWorld().playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                            SoundCategory.PLAYERS, 0.3f, 1f);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), 0.3, 0.8, 0.2);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), -0.2, 0.6, 0.3);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.LAVA, player.getX(), player.getY()+0.5, player.getZ(), 0.5, 0.3, -0.2);
                    HelperMethods.spawnParticle(player.getWorld(), ParticleTypes.SMOKE, player.getX(), player.getY()+0.5, player.getZ(), 0, 0, 0);

                    int radius = 3;
                    float abilityDamage = (player.getHealth() / 6);

                    ItemStack checkMainStack = player.getMainHandStack();
                    ItemStack checkOffStack = player.getOffHandStack();

                    if (checkMainStack.getItem() instanceof SwordItem || checkOffStack.getItem() instanceof SwordItem) {

                        //Check mainhand for immolation effect. Remove effect if not present
                        if (player.getMainHandStack().hasNbt()) {
                            NbtCompound rpnbt = player.getMainHandStack().getNbt();
                            if (rpnbt != null) {
                                if (!player.getMainHandStack().getNbt().getString("runic_power").equals("immolation")
                                        && !player.getMainHandStack().getNbt().getString("nether_power").equals("radiance")) {
                                    player.removeStatusEffect(EffectRegistry.IMMOLATION.get());
                                }
                            }
                        }
                        //Check offhand for immolation effect. Remove effect if not present
                        if (player.getMainHandStack().hasNbt()) {
                            NbtCompound rpnbt = player.getOffHandStack().getNbt();
                            if (rpnbt != null) {
                                if (!player.getOffHandStack().getNbt().getString("runic_power").equals("immolation")
                                        && !player.getOffHandStack().getNbt().getString("nether_power").equals("radiance")) {
                                    player.removeStatusEffect(EffectRegistry.IMMOLATION.get());
                                }
                            }
                        }
                    }
                    else {player.removeStatusEffect(EffectRegistry.IMMOLATION.get());}

                    //Check low HP. Remove effect if close to death
                    if (player.getHealth() < 4f)
                        player.removeStatusEffect(EffectRegistry.IMMOLATION.get());



                    //Damage
                    Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius,
                            player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                    for (Entity entities : player.getWorld().getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)){

                                le.damage(DamageSource.MAGIC, abilityDamage);
                                le.setOnFireFor(1);
                            }
                        }
                    }
                }
            }
        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
