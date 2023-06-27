package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.util.HelperMethods;

public class StormEffect extends StatusEffect {
    public StormEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }
    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.getWorld();
            int hradius = (int) (SimplySwordsConfig.getFloatValue("storm_radius"));
            int vradius = (int) (SimplySwordsConfig.getFloatValue("storm_radius") / 2);
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            var pPlayer = pLivingEntity.getAttacker();
            Box box = new Box(x + hradius, y +vradius, z + hradius, x - hradius, y - vradius, z - hradius);

            for(Entity e: world.getOtherEntities(pPlayer, box, EntityPredicates.VALID_LIVING_ENTITY))
            {
                if ((e instanceof LivingEntity ee) && (pPlayer instanceof PlayerEntity player)) {
                    if (ee.isTouchingWaterOrRain() && HelperMethods.checkFriendlyFire( ee, player)) {
                        var stormtarget = ee.getBlockPos();
                        if (ee.distanceTo(pPlayer) >= 5 ){
                            Entity storm = EntityType.LIGHTNING_BOLT.spawn(world, stormtarget, SpawnReason.TRIGGERED);
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
