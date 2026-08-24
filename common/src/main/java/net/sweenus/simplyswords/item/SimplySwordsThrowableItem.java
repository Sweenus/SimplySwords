package net.sweenus.simplyswords.item;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sweenus.simplyswords.entity.ThrownSpearEntity;
import net.sweenus.simplyswords.util.HelperMethods;

public class SimplySwordsThrowableItem extends SimplySwordsSwordItem {

    public SimplySwordsThrowableItem(ToolMaterial toolMaterial, Settings settings, String... repairIngredient) {
        super(toolMaterial, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            ThrownSpearEntity thrownSwordEntity = new ThrownSpearEntity(world, user, itemStack.copy() );
            thrownSwordEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            thrownSwordEntity.setYaw(user.getYaw());
            thrownSwordEntity.setPitch(user.getPitch()-90);
            double[] doubles = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
            thrownSwordEntity.primaryBaseDamage = (float) doubles[0];
            //System.out.println("Returned Attack value: " + (float) doubles[0]);
            thrownSwordEntity.hasLoyalty = MathHelper.clamp(EnchantmentHelper.getTridentReturnAcceleration((ServerWorld) world, itemStack, user), 0, 127);
            if (hand == Hand.OFF_HAND)
                thrownSwordEntity.offhandThrow = true;
            thrownSwordEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(thrownSwordEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);

        SimplySwordsAPI.setWeaponCooldown(user, itemStack, 1);
        return TypedActionResult.success(itemStack, world.isClient());
    }

}
