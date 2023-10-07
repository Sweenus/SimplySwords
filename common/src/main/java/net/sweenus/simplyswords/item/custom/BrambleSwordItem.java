package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class BrambleSwordItem extends UniqueSwordItem {
    public BrambleSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            int fhitchance = (int) Config.getFloat("brambleChance", "UniqueEffects", ConfigDefaultValues.brambleChance);
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                int sradius = (int) Config.getFloat("brambleRadius", "UniqueEffects", ConfigDefaultValues.brambleRadius);
                int vradius = (int) (Config.getFloat("brambleRadius", "UniqueEffects", ConfigDefaultValues.brambleRadius) / 2);
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                Box box = new Box(x + 10, y + 5, z + 10, x - 10, y - 5, z - 10);

                for (Entity entity : world.getOtherEntities(attacker, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if (entity != null && HelperMethods.checkFriendlyFire((LivingEntity) entity, attacker)) {
                        target = (LivingEntity) entity;
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 300, 1), attacker);
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 150, 1), attacker);
                    }
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SPORE_BLOSSOM_AIR,
                ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip3").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
