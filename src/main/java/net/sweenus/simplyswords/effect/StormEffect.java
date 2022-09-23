package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class StormEffect extends StatusEffect {
    public StormEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }
    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.world;
            BlockPos position = pLivingEntity.getBlockPos();
            int hradius = (int) (SimplySwordsConfig.getFloatValue("storm_radius"));
            int vradius = (int) (SimplySwordsConfig.getFloatValue("storm_radius") / 2);
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            var pPlayer = pLivingEntity.getAttacker();
            Box box = new Box(x + hradius, y +vradius, z + hradius, x - hradius, y - vradius, z - hradius);

            for(Entity e: world.getOtherEntities(pPlayer, box, EntityPredicates.VALID_LIVING_ENTITY))
            {
                if (e != null) {
                    if (e.isTouchingWaterOrRain()) {
                        var stormtarget = e.getBlockPos();
                        if (e.distanceTo(pPlayer) >= 5 ){
                            Entity storm = EntityType.LIGHTNING_BOLT.spawn(world, null, null, null, stormtarget, SpawnReason.TRIGGERED, true, true);
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
