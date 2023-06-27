package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class WhisperwindSwordItem extends UniqueSwordItem {

    private static int stepMod = 0;
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("fatalflicker_cooldown"));
    int abilityChance =  (int) (SimplySwordsConfig.getFloatValue("fatalflicker_chance"));

    public WhisperwindSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {



        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient()) {

            if (attacker.getRandom().nextInt(100) <= abilityChance && (attacker instanceof PlayerEntity player)) {
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.3f, 1.8f);
                player.getItemCooldownManager().set(this.getDefaultStack().getItem(), 0);
            }
        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.6f, 1.0f);
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.FATAL_FLICKER.get(), 12));
        HelperMethods.incrementStatusEffect(user, StatusEffects.ABSORPTION, 30, 1, 4);
        user.getItemCooldownManager().set(this.getDefaultStack().getItem(), skillCooldown);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }



    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.whisperwindsworditem.tooltip7").setStyle(TEXT));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
