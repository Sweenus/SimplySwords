package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ArcanethystSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public ArcanethystSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            if (attacker.getRandom().nextInt(100) <= Config.uniqueEffects.arcanethyst.chance) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 1), attacker);
                attacker.getEntityWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_01.get(),
                        attacker.getSoundCategory(), 0.5f, 1.2f);
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.getEntityWorld().isClient()) {
            ItemStack itemStack = user.getStackInHand(hand);
            if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
                return ActionResult.FAIL;
            }
            world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_02.get(),
                    user.getSoundCategory(), 0.4f, 1.2f);
            user.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }
        return super.use(world, user, hand);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (HelperMethods.isHolding(stack, user) && user instanceof PlayerEntity) {
            int radius = Config.uniqueEffects.arcanethyst.radius;
            float abilityDamage = HelperMethods.spellScaledDamage("arcane", user, Config.uniqueEffects.arcanethyst.spellScaling, Config.uniqueEffects.arcanethyst.damage);
            AbilityMethods.tickAbilityArcaneAssault(stack, world, user, remainingUseTicks, abilityDamage, radius);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Config.uniqueEffects.arcanethyst.duration;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient() && (user instanceof PlayerEntity player)) {
            player.getItemCooldownManager().set(stack, Config.uniqueEffects.arcanethyst.cooldown);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.REVERSE_PORTAL, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Environment(EnvType.CLIENT)
    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip6").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.ARCANETHYST::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 220;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 1f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 120;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 6;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.4f;
    }
}
