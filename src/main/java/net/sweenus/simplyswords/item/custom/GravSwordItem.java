package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

import java.util.List;

public class GravSwordItem extends SwordItem {
    public GravSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int fhitchance = SimplySwordsConfig.getIntValue("gravity_chance");
        int fduration = SimplySwordsConfig.getIntValue("gravity_duration");


        if (attacker.getRandom().nextInt(100) <= fhitchance) {

            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 3), attacker);

        }

        if (!attacker.world.isClient()) {
            double x = attacker.getX();
            double y = attacker.getY();
            double z = attacker.getZ();
            ServerWorld sworld = (ServerWorld) attacker.world;
            Box box = new Box(x + 10, y + 5, z + 10, x - 10, y - 5, z - 10);

            for(Entity ee: sworld.getOtherEntities(attacker, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (ee != null) {
                    if (ee instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity) ee;

                        if (attacker.hasStatusEffect(StatusEffects.HASTE) && le.distanceTo(attacker) > 1){ //can we check target here?
                            le.setVelocity(le.getRotationVector().multiply(+1));
                            sworld.playSoundFromEntity (null, le, SimplySwords.EVENT_OMEN_ONE , SoundCategory.BLOCKS, 0.1f, 2f);
                        }
                        if (attacker.hasStatusEffect(StatusEffects.HASTE) && le.distanceTo(attacker) <= 1){
                            le.setVelocity(le.getRotationVector().multiply(-1));
                            //sworld.playSoundFromEntity (null, ee, SimplySwords.EVENT_OMEN_ONE , SoundCategory.BLOCKS, 0.1f, 2f);
                        }
                    }
                }
            }
        }

        return super.postHit(stack, target, attacker);

    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        return super.use(world,user,hand);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip1").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip3"));
    }

}
