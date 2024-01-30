package net.sweenus.simplyswords.fabric.compat.eldritch_end;

import elocindev.eldritch_end.api.CorruptionAPI;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.fabric.compat.EldritchEndCompat;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

public class EldritchEndMethods {


    public static void generateVoidcloakStacks(LivingEntity livingEntity) {

        if (livingEntity instanceof PlayerEntity player) {
            ItemStack stack = player.getMainHandStack();
            if (player.age % 240 == 0) {
                double corruption = CorruptionAPI.getTotalCorruptionLevel(livingEntity);
                int maxStacks = ((int) corruption / 20);

                //System.out.println("trying to apply voidcloak with corruption level: " + corruption + " up to a maximum stack count of: " +maxStacks);
                if (!stack.isEmpty() && (stack.isOf(EldritchEndCompat.DREADTIDE))) {
                    HelperMethods.incrementStatusEffect(livingEntity, EffectRegistry.VOIDCLOAK.get(), 280, 1, maxStacks+1);
                }
            }
            if (player.age % 60 == 0) {
                generateVoidhungerStacks(player, stack);
            }
        }
    }

    public static void generateVoidhungerStacks(LivingEntity livingEntity, ItemStack stack) {
        int upperLimit = 100;
        if (!stack.isEmpty() && (stack.isOf(EldritchEndCompat.DREADTIDE))) {
            HelperMethods.incrementStatusEffect(livingEntity, EldritchEndRegistry.VOIDHUNGER.get(), 1200, 1, upperLimit);
        }
    }


}
