package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class WatcherEffect extends StatusEffect {
    public WatcherEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.world;
            BlockPos position = pLivingEntity.getBlockPos();
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            int pduration = 1;
            float rAmount = SimplySwordsConfig.getFloatValue("watcher_restore_amount");
            Box box = new Box(x + 20, y +10, z + 20, x - 20, y - 10, z - 20);
            var pPlayer = pLivingEntity.getAttacker();

            for(Entity e: world.getEntitiesByType(pLivingEntity.getType(), box, EntityPredicates.VALID_ENTITY))
            {
                if (e != null && pPlayer != null){
                    e.damage(DamageSource.FREEZE, rAmount);
                    pPlayer.setHealth(pPlayer.getHealth() + rAmount);
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
