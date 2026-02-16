package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class TwistedBladeItem extends UniqueSwordItem implements TwoHandedWeapon {
    public TwistedBladeItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

	private static int stepMod = 0;

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getEntityWorld();
            int hitChance = Config.uniqueEffects.twisted_blade.chance;
            int duration = Config.uniqueEffects.twisted_blade.duration;
            int maxStacks = Config.uniqueEffects.twisted_blade.maxStacks;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= hitChance) {
                if (attacker.hasStatusEffect(StatusEffects.HASTE)) {

                    int a = (attacker.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1);
                    world.playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_BOW_HOLY_SHOOT_IMPACT_02.get(),
                            attacker.getSoundCategory(), 0.3f, 1f + (a / 10f));

                    if ((attacker.getStatusEffect(StatusEffects.HASTE).getAmplifier() < maxStacks)) {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, a), attacker);
                    } else {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, a - 1), attacker);
                    }
                } else {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1), attacker);
                }
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user.hasStatusEffect(StatusEffects.HASTE)) {
            int strength_tier = Config.uniqueEffects.twisted_blade.strengthTier;

            int a = (user.getStatusEffect(StatusEffects.HASTE).getAmplifier() * 20);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, a, strength_tier), user);
            user.swingHand(hand);
            user.removeStatusEffect(StatusEffects.HASTE);
            world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                    user.getSoundCategory(), 0.5f, 1.5f);
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ASH,
                ParticleTypes.ASH, ParticleTypes.ASH, false);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.ferocitysworditem.tooltip6").setStyle(Styles.TEXT));

        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.TWISTED_BLADE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 75;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;
        @ValidatedInt.Restrict(min = 0)
        public int maxStacks = 15;
        @ValidatedInt.Restrict(min = 0)
        public int strengthTier = 2;
    }
}