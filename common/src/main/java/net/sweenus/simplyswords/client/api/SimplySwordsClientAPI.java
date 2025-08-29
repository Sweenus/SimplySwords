package net.sweenus.simplyswords.client.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueSwordItem;

import java.util.List;

public class SimplySwordsClientAPI {

    // See UniqueSwordItem for example usage
    public static void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, String modId, String itemPath, String uniquePath, String runicPath, Identifier customConfigPath) {
        Identifier entry = TooltipUtils.generateDefaultTooltipEntry(itemStack, itemPath);

        if (!Config.general.enableTooltipInfoButtons) {return;}
        if (!(MinecraftClient.getInstance().currentScreen instanceof InventoryScreen || MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen) && Config.general.tooltipInfoButtonsRequireInventoryScreen) {return;}

        // Add dynamic tooltip button
        TooltipUtils.addDynamicButtonTooltip(
                tooltip,
                Text.translatable("item.simplyswords.common.showtooltip.info"),
                Text.translatable("item.simplyswords.common.showtooltip.search"),
                Text.translatable("item.simplyswords.common.showtooltip.config"),
                Screen.hasAltDown(),
                Screen.hasControlDown()
        );

        // Handle specific item types
        if (itemStack.getItem() instanceof UniqueSwordItem) {
            entry = TooltipUtils.handleUniqueSwordTooltip(itemStack, tooltipContext, tooltip, type, uniquePath);
        } else if (itemStack.getItem() instanceof RunicSwordItem) {
            entry = TooltipUtils.handleRunicSwordTooltip(itemStack, tooltipContext, tooltip, type, itemPath, runicPath);
        }

        // Process Control + Alt key events for navigation
        TooltipUtils.processCtrlAltNavigation(entry, modId, customConfigPath, itemStack, tooltip);
    }

}
