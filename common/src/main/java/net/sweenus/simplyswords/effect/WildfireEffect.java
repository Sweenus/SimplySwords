package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.util.HelperMethods;

public class WildfireEffect extends StatusEffect {
    public WildfireEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            LivingEntity pPlayer = pLivingEntity.getAttacker();
            if (pPlayer != null){
                if (pPlayer instanceof PlayerEntity) {
                    ServerWorld world = (ServerWorld) pLivingEntity.getWorld();
                    int hradius = (int) Config.getFloat("wildfireRadius", "RunicEffects", ConfigDefaultValues.wildfireRadius);
                    int vradius = (int) (Config.getFloat("wildfireRadius", "RunicEffects", ConfigDefaultValues.wildfireRadius) / 2);
                    double x = pLivingEntity.getX();
                    double y = pLivingEntity.getY();
                    double z = pLivingEntity.getZ();
                    int pduration = (int) SimplySwords.runicEffectsConfig.wildfireDuration / 20;
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

        super.applyUpdateEffect(pLivingEntity, pAmplifier);

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
