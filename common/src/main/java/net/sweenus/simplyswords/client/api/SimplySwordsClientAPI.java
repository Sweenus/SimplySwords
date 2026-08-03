package net.sweenus.simplyswords.client.api;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueSwordItem;

import java.util.List;

public class SimplySwordsClientAPI {

    //
    // Registers a model predicate exposing the current awakening form stage's
    // configured model value. Call this from the addon's client initializer.
    //
    public static void registerAwakeningFormModelProperty(Item item, Identifier propertyId) {
        ItemPropertiesRegistry.register(
                item,
                propertyId,
                (stack, world, entity, seed) -> AwakeningApi.getFormModelValue(stack)
        );
    }

    // See UniqueSwordItem for example usage
    public static void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, String modId, String itemPath, String uniquePath, String runicPath, Identifier customConfigPath) {
        Identifier entry = TooltipUtils.generateDefaultTooltipEntry(itemStack, itemPath);

        boolean showInfoButtons = Config.general.enableTooltipInfoButtons
                && (!Config.general.tooltipInfoButtonsRequireInventoryScreen
                        || isInfoButtonScreen(MinecraftClient.getInstance().currentScreen));

        // Add dynamic tooltip button
        if (showInfoButtons) {
            TooltipUtils.addDynamicButtonTooltip(
                    tooltip,
                    Text.translatable("item.simplyswords.common.showtooltip.info"),
                    Text.translatable("item.simplyswords.common.showtooltip.search"),
                    Text.translatable("item.simplyswords.common.showtooltip.config"),
                    Screen.hasAltDown(),
                    Screen.hasControlDown()
            );
        }

        if (itemStack.getItem() instanceof UniqueSwordItem) {
            entry = TooltipUtils.handleUniqueSwordTooltip(itemStack, tooltipContext, tooltip, type, uniquePath);
        } else if (itemStack.getItem() instanceof RunicSwordItem) {
            entry = TooltipUtils.handleRunicSwordTooltip(itemStack, tooltipContext, tooltip, type, itemPath, runicPath);
        }

        // Process Control + Alt key events for navigation
        if (showInfoButtons) {
            TooltipUtils.processCtrlAltNavigation(entry, modId, customConfigPath, itemStack, tooltip);
        }
    }

    private static boolean isInfoButtonScreen(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen;
    }

}
