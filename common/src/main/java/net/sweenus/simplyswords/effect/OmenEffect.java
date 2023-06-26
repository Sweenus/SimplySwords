package net.sweenus.simplyswords.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;

public class OmenEffect extends StatusEffect {
    public OmenEffect(StatusEffectCategory statusEffectCategory, int color) {super (statusEffectCategory, color); }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.getWorld().isClient()) {
            ServerWorld world = (ServerWorld)pLivingEntity.getWorld();
            BlockPos position = pLivingEntity.getBlockPos();
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();
            var pPlayer = pLivingEntity.getAttacker();
            float absAmount = SimplySwordsConfig.getFloatValue("omen_absorption_amount");
            float pthreshold = (SimplySwordsConfig.getFloatValue("omen_instantkill_threshold") * pLivingEntity.getMaxHealth());
            Box box = new Box(x + 20, y +10, z + 20, x - 20, y - 10, z - 20);

            if (pLivingEntity.getHealth() <= pthreshold && pPlayer != null) {
                if (!pPlayer.hasStatusEffect(StatusEffects.REGENERATION)) {
                    pPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, (int) absAmount), pPlayer);
                    world.playSound(null, position, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.7f, 1.2f);
                }
                pLivingEntity.damage(pPlayer.getDamageSources().magic(), 1000);
            }

        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);

    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

}
