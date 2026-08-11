package net.sweenus.simplyswords.client.api;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.client.renderer.ModernFieldRenderer;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueWeaponItem;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SimplySwordsClientAPI {

    private static final Set<String> UNIQUE_TOOLTIP_NAMESPACES = ConcurrentHashMap.newKeySet();

    static {
        UNIQUE_TOOLTIP_NAMESPACES.add(SimplySwords.MOD_ID);
    }

    public static void registerUniqueTooltipNamespace(String namespace) {
        if (namespace != null && !namespace.isBlank()) {
            UNIQUE_TOOLTIP_NAMESPACES.add(namespace);
        }
    }

    public static boolean isUniqueTooltipNamespace(String namespace) {
        return namespace != null && UNIQUE_TOOLTIP_NAMESPACES.contains(namespace);
    }

    public static void registerParchmentGlyphStyle(Identifier id, ParchmentGlyphStyle style) {
        ParchmentVisualRegistry.registerGlyphStyle(id, style);
    }

    public static void renderAbilityTargetHighlight(MatrixStack matrices,
                                                    VertexConsumerProvider vertexConsumers,
                                                    int age, Vec3d targetOffset, float targetWidth,
                                                    AbilityTargetHighlightStyle style) {
        if (matrices == null || vertexConsumers == null || targetOffset == null || style == null) {
            return;
        }
        int primary = style.primaryColor();
        int pulse = style.pulseColor();
        ModernFieldRenderer.renderTargetLine(matrices, vertexConsumers, age, targetOffset,
                primary >> 16 & 255, primary >> 8 & 255, primary & 255,
                pulse >> 16 & 255, pulse >> 8 & 255, pulse & 255);
        ModernFieldRenderer.renderTargetRing(matrices, vertexConsumers, age, targetOffset, targetWidth,
                primary >> 16 & 255, primary >> 8 & 255, primary & 255,
                pulse >> 16 & 255, pulse >> 8 & 255, pulse & 255);
    }

    public static void pushWeaponHudTransform(DrawContext context) {
        context.getMatrices().push();
        context.getMatrices().translate(
                context.getScaledWindowWidth() / 2 + Config.gui.xOffset,
                context.getScaledWindowHeight() - 68 + Config.gui.yOffset,
                0.0F
        );
        float scale = Math.clamp(Config.gui.scale, 0.25F, 4.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
    }

    public static void registerAwakeningFormModelProperty(Item item, Identifier propertyId) {
        ItemPropertiesRegistry.register(
                item,
                propertyId,
                (stack, world, entity, seed) -> AwakeningApi.getFormModelValue(stack)
        );
    }

    public static void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, String modId, String itemPath, String uniquePath, String runicPath, Identifier customConfigPath) {
        Identifier entry = TooltipUtils.generateDefaultTooltipEntry(itemStack, itemPath);

        boolean showInfoButtons = Config.general.enableTooltipInfoButtons
                && (!Config.general.tooltipInfoButtonsRequireInventoryScreen
                        || isInfoButtonScreen(MinecraftClient.getInstance().currentScreen));

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

        if (itemStack.getItem() instanceof UniqueWeaponItem) {
            entry = TooltipUtils.handleUniqueSwordTooltip(itemStack, tooltipContext, tooltip, type, uniquePath);
        } else if (itemStack.getItem() instanceof RunicSwordItem) {
            entry = TooltipUtils.handleRunicSwordTooltip(itemStack, tooltipContext, tooltip, type, itemPath, runicPath);
        }

        if (showInfoButtons) {
            TooltipUtils.processCtrlAltNavigation(entry, modId, customConfigPath, itemStack, tooltip);
        }
    }

    public static void appendSpellScaleTooltip(List<Text> tooltip, String spellSchool) {
        TooltipUtils.appendSpellScaleTooltip(tooltip, spellSchool);
    }

    private static boolean isInfoButtonScreen(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen;
    }

}
