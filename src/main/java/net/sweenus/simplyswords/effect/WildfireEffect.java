package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class WildfireEffect extends StatusEffect {
    public WildfireEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.world;
            BlockPos position = pLivingEntity.getBlockPos();
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            int pduration = SimplySwordsConfig.getIntValue("wildfire_duration");
            Box box = new Box(x + 10, y +5, z + 10, x - 10, y - 5, z - 10);

            for(Entity e: world.getEntitiesByType(pLivingEntity.getType(), box, EntityPredicates.VALID_ENTITY))
            {
                if (e != null) {
                    e.setOnFireFor(pduration);
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
