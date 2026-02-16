package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.util.HelperMethods;

public class WildfireEffect extends StatusEffect {
    public WildfireEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getEntityWorld().isClient()) {
            LivingEntity pPlayer = pLivingEntity.getAttacker();
            if (pPlayer != null) {
                if (pPlayer instanceof PlayerEntity) {
                    double hradius = Config.gemPowers.wildfire.radius;
                    double vradius = Config.gemPowers.wildfire.radius / 2.0;
                    double x = pLivingEntity.getX();
                    double y = pLivingEntity.getY();
                    double z = pLivingEntity.getZ();
                    int pduration = Config.gemPowers.wildfire.duration / 20;
                    Box box = new Box(x + hradius, y + vradius, z + hradius, x - hradius, y - vradius, z - hradius);

                    for (Entity e : world.getEntitiesByType(pLivingEntity.getType(), box, EntityPredicates.VALID_ENTITY)) {
                        if (e instanceof LivingEntity) {
                            if (HelperMethods.checkFriendlyFire((LivingEntity) e, (PlayerEntity) pPlayer))
                                e.setOnFireFor(pduration);
                        }
                    }
                }
            }
        }

        super.applyUpdateEffect(world, pLivingEntity, pAmplifier);

        return true;
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
