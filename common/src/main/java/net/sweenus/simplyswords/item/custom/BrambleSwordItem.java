package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class BrambleSwordItem extends UniqueSwordItem {
    public BrambleSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("bramble_chance");
            HelperMethods.playHitSounds(attacker, target);


            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                    int sradius = (int) SimplySwordsConfig.getFloatValue("bramble_radius");
                    int vradius = (int) (SimplySwordsConfig.getFloatValue("bramble_radius") / 2);
                    double x = target.getX();
                    double y = target.getY();
                    double z = target.getZ();
                    Box box = new Box(x + 10, y + 5, z + 10, x - 10, y - 5, z - 10);

                    for (Entity e : world.getOtherEntities(attacker, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                        if (e != null && HelperMethods.checkFriendlyFire((LivingEntity) e, (PlayerEntity) attacker)) {
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
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19.x

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.bramblesworditem.tooltip3"));
        /*
        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.bramblesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.bramblesworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.bramblesworditem.tooltip3"));

         */
        super.appendTooltip(itemStack,world, tooltip, tooltipContext);

    }

}
