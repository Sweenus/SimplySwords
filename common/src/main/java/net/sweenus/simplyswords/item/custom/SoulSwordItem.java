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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class SoulSwordItem extends UniqueSwordItem {
    public SoulSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("soulmeld_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("soulmeld_duration");
            HelperMethods.playHitSounds(attacker, target);


            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                //world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.3f, 1.2f);
                if (attacker.hasStatusEffect(StatusEffects.MINING_FATIGUE) && attacker.hasStatusEffect(StatusEffects.RESISTANCE)) {

                    int a = (attacker.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1);

                    if ((attacker.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() <= 2)) {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, fduration, a), attacker);
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, fduration, a), attacker);
                    }
                } else {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, fduration, 1), attacker);
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, fduration, 1), attacker);
                }
            }
        }

        return super.postHit(stack, target, attacker);

    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {
            int hradius = (int) SimplySwordsConfig.getFloatValue("soulmeld_radius");
            int vradius = (int) (SimplySwordsConfig.getFloatValue("soulmeld_radius") / 2);
            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld serverWorld = (ServerWorld) user.world;
            Box box = new Box(x + hradius, y + vradius, z + hradius, x - hradius, y - vradius, z - hradius);

            for(Entity ee: serverWorld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (ee != null) {
                    if (ee instanceof LivingEntity && user.hasStatusEffect(StatusEffects.MINING_FATIGUE) && HelperMethods.checkFriendlyFire((LivingEntity) ee, user)){
                        LivingEntity le = (LivingEntity) ee;
                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, user.getStatusEffect(StatusEffects.MINING_FATIGUE).getDuration(), user.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) , user);
                        world.playSoundFromEntity (null, ee, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get() , SoundCategory.BLOCKS, 0.1f, 1f);
                    }
                }
            }

            if (user.hasStatusEffect(StatusEffects.MINING_FATIGUE) && user.hasStatusEffect(StatusEffects.RESISTANCE)) {

                user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                        user.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() * 40, 2), user);

                user.removeStatusEffect(StatusEffects.MINING_FATIGUE);
                user.removeStatusEffect(StatusEffects.RESISTANCE);
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
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SOUL, ParticleTypes.SOUL, ParticleTypes.SPORE_BLOSSOM_AIR, false);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip6"));

        /*

        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.soulsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.soulsworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.soulsworditem.tooltip3"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.soulsworditem.tooltip4"));
        tooltip.add(new TranslatableText("item.simplyswords.soulsworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.soulsworditem.tooltip6"));

         */
        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
