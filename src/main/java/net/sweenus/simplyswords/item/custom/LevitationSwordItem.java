package net.sweenus.simplyswords.item.custom;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.ActionResult;

public class LevitationSwordItem extends SwordItem {
    public LevitationSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    public class SimplySwords implements ModInitializer
    {

    @Override
    public void onInitialize()
    {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
        {
            /* Manual spectator check is necessary because AttackBlockCallbacks
               fire before the spectator check */
            if (!player.isSpectator())
            {
                //player.damage(DamageSource.GENERIC, 1.0F);
                entity.damage(DamageSource.LIGHTNING_BOLT, 1.0F);
            }
            return ActionResult.PASS;
        });
    }
}

}
