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

        if (livingEntity instanceof PlayerEntity player && player.age % 240 == 0) {
            ItemStack stack = player.getMainHandStack();
            double corruption = CorruptionAPI.getTotalCorruptionLevel(livingEntity);
            int maxStacks = 1 + ((int) corruption / 20);

            System.out.println("trying to apply voidcloak with corruption level: " + corruption + " up to a maximum stack count of: " +maxStacks);
            if (!stack.isEmpty() && !player.getItemCooldownManager().isCoolingDown(stack.getItem())
                    && (stack.isOf(EldritchEndCompat.DREADTIDE))) {
                HelperMethods.incrementStatusEffect(livingEntity, EffectRegistry.VOIDCLOAK.get(), 280, 1, maxStacks);
            }
        }

    }


}
