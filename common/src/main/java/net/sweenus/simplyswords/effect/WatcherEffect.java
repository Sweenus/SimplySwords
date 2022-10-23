package net.sweenus.simplyswords.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class WatcherEffect extends StatusEffect {
    public WatcherEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.world;
            BlockPos position = pLivingEntity.getBlockPos();
            int hradius = (int) (SimplySwordsConfig.getFloatValue("watcher_radius"));
            int vradius = (int) (SimplySwordsConfig.getFloatValue("watcher_radius") / 2);
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            float rAmount = SimplySwordsConfig.getFloatValue("watcher_restore_amount");
            Box box = new Box(x + hradius, y +vradius, z + hradius, x - hradius, y - vradius, z - hradius);
            var pPlayer = pLivingEntity.getAttacker();

            for(Entity e: world.getOtherEntities(pPlayer, box, EntityPredicates.VALID_ENTITY))
            {
                if (e != null && pPlayer != null){
                    e.damage(DamageSource.FREEZE, rAmount);
                    pPlayer.setHealth(pPlayer.getHealth() + rAmount);
                    BlockPos position2 = e.getBlockPos();
                    world.playSound(null, position2, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.05f, 1.2f);
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
