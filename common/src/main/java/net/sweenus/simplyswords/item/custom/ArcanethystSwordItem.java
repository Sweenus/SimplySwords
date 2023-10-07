package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class ArcanethystSwordItem extends UniqueSwordItem {
    public ArcanethystSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;
    public static boolean scalesWithSpellPower;
    int radius = (int) Config.getFloat("arcaneAssaultRadius", "UniqueEffects", ConfigDefaultValues.arcaneAssaultRadius);
    float abilityDamage = Config.getFloat("arcaneAssaultDamage", "UniqueEffects", ConfigDefaultValues.arcaneAssaultDamage);
    int arcane_timer_max = (int) Config.getFloat("arcaneAssaultDuration", "UniqueEffects", ConfigDefaultValues.arcaneAssaultDuration);
    int skillCooldown = (int) Config.getFloat("arcaneAssaultCooldown", "UniqueEffects", ConfigDefaultValues.arcaneAssaultCooldown);
    int chargeChance = (int) Config.getFloat("arcaneAssaultChance", "UniqueEffects", ConfigDefaultValues.arcaneAssaultChance);
    int spellScalingModifier = (int) Config.getFloat("arcaneAssaultSpellScaling", "UniqueEffects", ConfigDefaultValues.arcaneAssaultSpellScaling);

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            if (attacker.getRandom().nextInt(100) <= chargeChance) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 1), attacker);
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_01.get(),
                        attacker.getSoundCategory(), 0.5f, 1.2f);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            ItemStack itemStack = user.getStackInHand(hand);
            if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
                return TypedActionResult.fail(itemStack);
            }
            world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_02.get(),
                    user.getSoundCategory(), 0.4f, 1.2f);
            user.setCurrentHand(hand);
            return TypedActionResult.consume(itemStack);
        }
        return super.use(world, user, hand);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack && user instanceof PlayerEntity) {
            AbilityMethods.tickAbilityArcaneAssault(stack, world, user, remainingUseTicks, arcane_timer_max, abilityDamage,
                    skillCooldown, radius);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return arcane_timer_max;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && (user instanceof PlayerEntity player)) {
            player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, entity, "arcane") > 0) {
            abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, entity, "arcane");
            scalesWithSpellPower = true;
        }
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.DRAGON_BREATH,
                ParticleTypes.DRAGON_BREATH, ParticleTypes.REVERSE_PORTAL, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip6").setStyle(TEXT));
        if (scalesWithSpellPower) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleArcane"));
        }
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
