package net.sweenus.simplyswords.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public abstract class UniqueSwordItem extends SwordItem {

    String iRarity = "UNIQUE";

    public UniqueSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings.fireproof());
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 0;
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

        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.HARBINGER.get())
                || this.getDefaultStack().isOf(ItemsRegistry.SUNFIRE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGISPEAR.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGIBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGISCYTHE.get())) {
            this.iRarity = "LEGENDARY";
            return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.LEGENDARY);
        }

        if (this.iRarity.equals("UNIQUE")) return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.UNIQUE);
        else return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.COMMON);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        TooltipUtils.addDynamicButtonTooltip(
                tooltip,
                Text.translatable("item.simplyswords.common.showtooltip.info"),
                Text.translatable("item.simplyswords.common.showtooltip.search"),
                Screen.hasAltDown(),
                Screen.hasControlDown()
        );
        tooltip.add(Text.literal(""));
        SimplySwordsAPI.appendTooltipGemSocketLogic(itemStack, tooltipContext, tooltip, type);
        if (Screen.hasControlDown()) {
            final Identifier entry = Identifier.of("simplyswords:uniques/entry_" + this.getDefaultStack().getItem().getRegistryEntry().registryKey().getValue().getPath());
            //System.out.println("Entry: " + entry.getPath());
            TooltipUtils.openPatchouli(entry);
        }
    }

}