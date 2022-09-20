package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class OmenEffect extends StatusEffect {
    public OmenEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.world;
            BlockPos position = pLivingEntity.getBlockPos();
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            var pPlayer = pLivingEntity.getAttacker();
            float absAmount = SimplySwordsConfig.getFloatValue("omen_absorption_amount");
            int pduration = 5;
            float pthreshold = (SimplySwordsConfig.getFloatValue("omen_instantkill_threshold") * pLivingEntity.getMaxHealth());
            Box box = new Box(x + 20, y +10, z + 20, x - 20, y - 10, z - 20);

            if (pLivingEntity.getHealth() <= pthreshold && pPlayer != null) {
                if (pPlayer.getAbsorptionAmount() < 6f) {
                    pPlayer.setAbsorptionAmount(pPlayer.getAbsorptionAmount() + absAmount);
                    world.playSound(null, position, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.6f, 1f);
                }
                pLivingEntity.damage(DamageSource.GENERIC, 1000);
            }

        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
