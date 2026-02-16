package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
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
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class CaelestisSwordItem extends UniqueSwordItem {
    public CaelestisSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {

            HelperMethods.playHitSounds(attacker, target);

        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        int skillCooldown = Config.uniqueEffects.caelestis.cooldown;
        int duration = Config.uniqueEffects.caelestis.duration;


        world.playSound(null, user.getBlockPos(), SoundRegistry.ACTIVATE_PLINTH_03.get(),
                user.getSoundCategory(), 0.4f, 1.3f);

        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT),
                duration, 0, false, false, true));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,
                duration, 0, false, false, true));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                duration, 0, false, false, true));
        user.getItemCooldownManager().set(this.getDefaultStack(), skillCooldown);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip7", Config.uniqueEffects.caelestis.duration / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip9").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip10").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip11").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip12").setStyle(Styles.TEXT));

        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.CAELESTIS::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 5;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 420;
        @ValidatedFloat.Restrict(min = 0)
        public float damageMax = 300f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageModifier = 1.0f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;

    }
}