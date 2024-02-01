package net.sweenus.simplyswords.compat.eldritch_end;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.compat.EldritchEndCompat;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class EldritchEndCompatMethods {


    public static void generateVoidcloakStacks(LivingEntity livingEntity) {

        if (livingEntity instanceof PlayerEntity player) {
            ItemStack stack = player.getMainHandStack();
            if (player.age % 180 == 0 && Registries.ATTRIBUTE.get(new Identifier("eldritch_end:corruption")) != null) {
                double corruption = livingEntity.getAttributeValue(Registries.ATTRIBUTE.get(new Identifier("eldritch_end:corruption")));
                int maxStacks = ((int) corruption / 20);

                //System.out.println("trying to apply voidcloak with corruption level: " + corruption + " up to a maximum stack count of: " +maxStacks);
                if (!stack.isEmpty() && (stack.isOf(EldritchEndCompat.DREADTIDE.get()))) {
                    HelperMethods.incrementStatusEffect(livingEntity, EffectRegistry.VOIDCLOAK.get(), 280, 1, maxStacks+1);
                    livingEntity.getWorld().playSound(null, livingEntity.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                            livingEntity.getSoundCategory(), 0.1f, 1.4f);
                }
            }
            if (player.age % 60 == 0) {
                generateVoidhungerStacks(player, stack);
            }
        }
    }

    public static void generateVoidhungerStacks(LivingEntity livingEntity, ItemStack stack) {
        int upperLimit = 100;
        if (!stack.isEmpty() && (stack.isOf(EldritchEndCompat.DREADTIDE.get()))) {
            HelperMethods.incrementStatusEffect(livingEntity, EldritchEndCompatRegistry.VOIDHUNGER.get(), 1200, 1, upperLimit);
        }
    }


}
