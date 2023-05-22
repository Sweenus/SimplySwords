package net.sweenus.simplyswords.item.custom;


import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class StealSwordItem extends UniqueSwordItem {
    public StealSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("steal_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("steal_duration");
            attacker.setVelocity(attacker.getRotationVector().multiply(+1));
            attacker.velocityModified = true;

            HelperMethods.playHitSounds(attacker, target);


            if (attacker.getRandom().nextInt(100) <= fhitchance) {

                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.5f, 2f);
                if (choose_sound <= 20 && choose_sound > 10)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.5f, 2f);
                if (choose_sound <= 30 && choose_sound > 20)
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.5f, 2f);

                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 2), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, 1), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, fduration, 1), attacker);

            }

            /*
            //Awakening
            if(stack.getOrCreateNbt().getInt("awakening_exp") < 100 && stack.getOrCreateNbt().getInt("awakening") < 5) {
                stack.getOrCreateNbt().putInt("awakening_exp", stack.getOrCreateNbt().getInt("awakening_exp") + 10);
            }
            else if(stack.getOrCreateNbt().getInt("awakening_exp") >= 100) {
                stack.getOrCreateNbt().putInt("awakening_exp", 0);
                if(stack.getOrCreateNbt().getInt("awakening") < 5) {
                    stack.getOrCreateNbt().putInt("awakening", stack.getOrCreateNbt().getInt("awakening") +1);
                }
            }
             */

        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {


        if (!user.world.isClient()) {
            int sradius = (int) SimplySwordsConfig.getFloatValue("steal_radius");
            int vradius = (int) (SimplySwordsConfig.getFloatValue("steal_radius") / 2);

            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.world;
            Box box = new Box(x + sradius, y + vradius, z + sradius, x - sradius, y - vradius, z - sradius);
            for(Entity entities: sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (entities != null) {
                    if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                        int iduration = (int) SimplySwordsConfig.getFloatValue("steal_invis_duration");
                        int bduration = (int) SimplySwordsConfig.getFloatValue("steal_blind_duration");

                        if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING) && le.distanceTo(user) > 5){ //can we check target here?
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, bduration, 1), user);
                            user.teleport(le.getX(), le.getY(), le.getZ());
                            sworld.playSoundFromEntity (null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get() , SoundCategory.PLAYERS, 0.3f, 1.5f);
                            le.damage(DamageSource.FREEZE, 5f);
                            le.removeStatusEffect(StatusEffects.SLOWNESS);
                            le.removeStatusEffect(StatusEffects.GLOWING);
                        }
                        if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING) && le.distanceTo(user) <= 5){
                            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, iduration, 1), user);
                            user.setVelocity(user.getRotationVector().multiply(+2));
                            user.velocityModified = true;
                            sworld.playSoundFromEntity (null, entities, SoundRegistry.MAGIC_BOW_SHOOT_MISS_01.get(), SoundCategory.PLAYERS, 0.3f, 1.5f);
                            le.removeStatusEffect(StatusEffects.SLOWNESS);
                            le.removeStatusEffect(StatusEffects.GLOWING);
                        }
                    }
                }
            }
        }

        return super.use(world,user,hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.NAUTILUS, ParticleTypes.NAUTILUS, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        return false;
    }
/*
    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

        if(stack.getOrCreateNbt().getInt("awakening") < 5)
        {
            stack.getOrCreateNbt().putInt("awakening", stack.getOrCreateNbt().getInt("awakening") + 1);
        }
    }
*/

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip4"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip6"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip7"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip8"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip9"));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip10"));
        tooltip.add(Text.literal(""));

        /*
        if(itemStack.getOrCreateNbt().getInt("awakening") < 5) {

            tooltip.add(Text.translatable("item.simplyswords.awakening",
                    (itemStack.getOrCreateNbt().getInt("awakening"))).formatted(Formatting.GOLD));

            tooltip.add(Text.translatable("item.simplyswords.awakening.exp",
                    (itemStack.getOrCreateNbt().getInt("awakening_exp"))).formatted(Formatting.GREEN));

        } else if(itemStack.getOrCreateNbt().getInt("awakening") == 5) {
            tooltip.add(Text.translatable("item.simplyswords.awakening",
                    (itemStack.getOrCreateNbt().getInt("awakening"))).formatted(Formatting.RED));
        }
        tooltip.add(Text.translatable("item.simplyswords.awakening.powers"));
         */


/*
        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip3"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip4"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip6"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip7"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip8"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip9"));
        tooltip.add(new TranslatableText("item.simplyswords.stealsworditem.tooltip10"));

 */
        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
