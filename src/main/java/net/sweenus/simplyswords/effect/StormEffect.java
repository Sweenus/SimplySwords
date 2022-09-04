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

public class StormEffect extends StatusEffect {
    public StormEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }
    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.world;
            BlockPos position = pLivingEntity.getBlockPos();
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            var pPlayer = pLivingEntity.getAttacker();
            Box box = new Box(x + 15, y +5, z + 15, x - 15, y - 5, z - 15);

            //for(Entity e: world.getEntitiesByType(pLivingEntity.getType(), box, EntityPredicates.VALID_ENTITY))
            for(Entity e: world.getOtherEntities(pPlayer, box, EntityPredicates.VALID_LIVING_ENTITY))
            {
                if (e != null) {
                    if (e.isTouchingWaterOrRain()) {
                        var stormtarget = e.getBlockPos();
                        if (e.distanceTo(pPlayer) >= 5 ){
                            Entity storm = EntityType.LIGHTNING_BOLT.spawn(world, null, null, null, stormtarget, SpawnReason.TRIGGERED, true, true);
                        }
                        //e.damage(DamageSource.LIGHTNING_BOLT, 5f);


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
