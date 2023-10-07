package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class UniqueSwordItem extends SwordItem {

    String iRarity = "UNIQUE";

    public UniqueSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings.fireproof());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        SimplySwordsAPI.inventoryTickGemSocketLogic(stack, world, entity, 50, 50);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {
        SimplySwordsAPI.onClickedGemSocketLogic(stack, otherStack, player);
        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            SimplySwordsAPI.postHitGemSocketLogic(stack, target, attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public Text getName(ItemStack stack) {

        Style COMMON = HelperMethods.getStyle("common");
        Style UNIQUE = HelperMethods.getStyle("unique");
        Style LEGENDARY = HelperMethods.getStyle("legendary");

        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.HARBINGER.get())
                || this.getDefaultStack().isOf(ItemsRegistry.SUNFIRE.get())) {
            this.iRarity = "LEGENDARY";
            return Text.translatable(this.getTranslationKey(stack)).setStyle(LEGENDARY);
        }

        if (this.iRarity.equals("UNIQUE")) return Text.translatable(this.getTranslationKey(stack)).setStyle(UNIQUE);
        else return Text.translatable(this.getTranslationKey(stack)).setStyle(COMMON);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext
            tooltipContext) {
        SimplySwordsAPI.appendTooltipGemSocketLogic(itemStack, tooltip);
    }
}