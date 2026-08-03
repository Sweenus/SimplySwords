package net.sweenus.simplyswords.compat.eldritch_end;

import net.minecraft.entity.LivingEntity;

public class EldritchEndCompatMethods {


    public static void generateVoidcloakStacks(LivingEntity livingEntity) {
        /*

        if (livingEntity instanceof PlayerEntity player) {
            ItemStack stack = player.getMainHandStack();
            if (player.age % 180 == 0 && Registries.ATTRIBUTE.get(new Identifier("eldritch_end:corruption")) != null) {
                double corruption = livingEntity.getAttributeValue(Registries.ATTRIBUTE.get(new Identifier("eldritch_end:corruption")));
                int maxStacks = ((int) corruption / 20);

                if (!stack.isEmpty() && (stack.isOf(EldritchEndCompat.DREADTIDE.get()))) {
                    HelperMethods.incrementStatusEffect(livingEntity, EffectRegistry.VOIDCLOAK, 280, 1, maxStacks-1);
                    livingEntity.getWorld().playSound(null, livingEntity.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                            livingEntity.getSoundCategory(), 0.1f, 1.4f);
                }
            }
            int voidcallerCorruptionFrequency = Config.uniqueEffects.voidcaller.get().corruptionFrequency;
            if (player.age % voidcallerCorruptionFrequency == 0) {
                generateVoidhungerStacks(player, stack);
            }
        }
    }

    public static void generateVoidhungerStacks(LivingEntity livingEntity, ItemStack stack) {
        int voidcallerCorruptionPerTick = Config.uniqueEffects.voidcaller.get().corruptionPerTick;
        int voidcallerCorruptionDuration = Config.uniqueEffects.voidcaller.get().corruptionDuration;
        int voidcallerCorruptionMax = Config.uniqueEffects.voidcaller.get().corruptionMax;
        if (!stack.isEmpty() && (stack.isOf(EldritchEndCompat.DREADTIDE.get()))) {
            HelperMethods.incrementStatusEffect(livingEntity, EldritchEndCompatRegistry.VOIDHUNGER, voidcallerCorruptionDuration, voidcallerCorruptionPerTick, voidcallerCorruptionMax);
        }
         */
    }

}