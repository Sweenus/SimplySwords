package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.util.HelperMethods;

public class WildfireEffect extends StatusEffect {
    public WildfireEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            LivingEntity pPlayer = pLivingEntity.getAttacker();
            if (pPlayer != null){
                if (pPlayer instanceof PlayerEntity) {
                    ServerWorld world = (ServerWorld) pLivingEntity.world;
                    BlockPos position = pLivingEntity.getBlockPos();
                    int hradius = (int) (SimplySwordsConfig.getFloatValue("wildfire_radius"));
                    int vradius = (int) (SimplySwordsConfig.getFloatValue("wildfire_radius") / 2);
                    double x = pLivingEntity.getX();
                    double y = pLivingEntity.getY();
                    double z = pLivingEntity.getZ();
                    int pduration = (int) SimplySwordsConfig.getFloatValue("wildfire_duration") / 20;
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
