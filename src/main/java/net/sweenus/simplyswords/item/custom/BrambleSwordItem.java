package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class BrambleSwordItem extends SwordItem {
    public BrambleSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int fhitchance = 50; //SimplySwordsConfig.getIntValue("brimstone_chance");


        if (attacker.getRandom().nextInt(100) <= fhitchance) {
            if (!target.world.isClient()) {
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                ServerWorld world = (ServerWorld) target.world;
                Box box = new Box(x + 10, y + 5, z + 10, x - 10, y - 5, z - 10);

                for(Entity e: world.getOtherEntities(attacker, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if (e != null) {
                        target = (LivingEntity) e;
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 300, 1), attacker);
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 150, 1), attacker);
                    }
                }
            }
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip1").formatted(Formatting.GOLD));

        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip2"));
    }

}
