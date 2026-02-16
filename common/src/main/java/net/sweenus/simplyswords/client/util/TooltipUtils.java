package net.sweenus.simplyswords.client.util;

import dev.architectury.platform.Platform;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.power.GemPower;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TooltipUtils {
    public static final Identifier runic_tags = Identifier.of(SimplySwords.MOD_ID, "runic_weapons");
    private static long ctrlKeyPressTimestamp = 0;

    public static boolean isAltDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isControlDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }


    public static void centerAlignTooltip(List<Text> tooltip, Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Imperfect center alignment, as it will only adapt to information that has been applied to the tooltip prior to method call
        // Calculate the total width of the tooltip (based on all lines added so far)
        int tooltipWidth = 0;
        for (Text line : tooltip) {
            tooltipWidth = Math.max(tooltipWidth, client.textRenderer.getWidth(line));
        }

        // Calculate the width of the given text
        int textWidth = client.textRenderer.getWidth(text);

        // Calculate the padding needed to center the text
        int paddingWidth = (tooltipWidth - textWidth) / 2;

        // Make padding using spaces
        String padding = " ".repeat(Math.max(0, paddingWidth / client.textRenderer.getWidth(" ")));

        // Add the centered text to the tooltip
        tooltip.add(Text.literal(padding).append(text));
    }

    public static void addDynamicButtonTooltip(List<Text> tooltip, Text info, Text search, Text config, boolean isAlt, boolean isCtrl) {
        boolean isAltAndCtrl = isAlt && isCtrl; // Check if both Alt and Ctrl are being held

        tooltip.add(
                Text.literal("")
                        .append(
                                info.copy().setStyle(
                                        info.getStyle().withColor(
                                                isAltAndCtrl ? Styles.COMMON.getColor() : // Both Alt and Ctrl held
                                                        isAlt ? Styles.UNIQUE.getColor() : // Only Alt held
                                                                Styles.COMMON.getColor() // Default styling
                                        )
                                )
                        )
                        .append("\u00A0") // Spacer
                        .append(
                                search.copy().setStyle(
                                        search.getStyle().withColor(
                                                isAltAndCtrl ? Styles.COMMON.getColor() : // Both Alt and Ctrl held
                                                        isCtrl ? Styles.UNIQUE.getColor() : // Only Ctrl held
                                                                Styles.COMMON.getColor() // Default styling
                                        )
                                )
                        )
                        .append("\u00A0") // Spacer
                        .append(
                                config.copy().setStyle(
                                        config.getStyle().withColor(
                                                isAltAndCtrl ? Styles.UNIQUE.getColor() : // Both Alt and Ctrl held
                                                        Styles.COMMON.getColor() // Default styling
                                        )
                                )
                        )
        );
    }



    public static void appendSpellScaleTooltip(List<Text> tooltip, String spellSchool) {
        if (Platform.isModLoaded("spell_power") || Platform.isModLoaded("irons_spellbooks")) {
            if (isAltDown() && !isControlDown()) {
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.compat.spellScaling").setStyle(Styles.COMMON));
                switch (spellSchool) {
                    case "fire" ->
                            tooltip.add(Text.literal("\uAB42").append(Text.translatable("item.simplyswords.compat.scaleFire")));
                    case "frost" ->
                            tooltip.add(Text.literal("\uAB43").append(Text.translatable("item.simplyswords.compat.scaleFrost")));
                    case "lightning" ->
                            tooltip.add(Text.literal("\uAB44").append(Text.translatable("item.simplyswords.compat.scaleLightning")));
                    case "soul" ->
                            tooltip.add(Text.literal("\uAB45").append(Text.translatable("item.simplyswords.compat.scaleSoul")));
                    case "arcane" ->
                            tooltip.add(Text.literal("\uAB46").append(Text.translatable("item.simplyswords.compat.scaleArcane")));
                    case "frost_fire" ->
                            tooltip.add(Text.literal("\uAB43").append(Text.translatable("item.simplyswords.compat.scaleFrost")).append(Text.literal("   \uAB42")).append(Text.translatable("item.simplyswords.compat.scaleFire")));
                    case "healing_fire" ->
                            tooltip.add(Text.literal("\uAB47").append(Text.translatable("item.simplyswords.compat.scaleHealing")).append(Text.literal("   \uAB42")).append(Text.translatable("item.simplyswords.compat.scaleFire")));
                }
                tooltip.add(Text.literal(""));
            }
        }
    }

    public static boolean shouldDisplayTooltip(ItemStack stack, Identifier tagId) {
        return (isAltDown() && !isControlDown()) // Don't hide info on these items
                || HelperMethods.isInTag(stack, tagId)
                || stack.isOf(ItemsRegistry.RUNEFUSED_GEM.get())
                || stack.isOf(ItemsRegistry.NETHERFUSED_GEM.get());
    }


    public static void openFzzyConfig(String path) {
        if (!ConfigApiJava.isScreenOpen("simplyswords.unique_effects.")) {
            //System.out.println(path);
            ConfigApiJava.INSTANCE.openScreen(path);
        }
    }

    public static Identifier generateDefaultTooltipEntry(ItemStack itemStack, String itemPath) {
        return Identifier.of(itemPath + "/" +
                itemStack.getItem().getRegistryEntry().registryKey().getValue().getPath() + ".mdx");
    }

    public static Identifier handleUniqueSwordTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, String uniquePath) {
        tooltip.add(Text.literal(""));

        SimplySwordsAPI.appendTooltipGemSocketLogic(itemStack, tooltipContext, tooltip, type);

        return Identifier.of(uniquePath + "/" +
                itemStack.getItem().getRegistryEntry().registryKey().getValue().getPath() + ".mdx");
    }

    public static Identifier handleRunicSwordTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, String itemPath, String runicPath) {
        tooltip.add(Text.literal(""));

        GemPowerComponent component = SimplySwordsAPI.getComponent(itemStack);
        if (component.isEmpty()) {
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").setStyle(Styles.RUNIC));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(Styles.TEXT));
        } else {
            component.appendTooltip(itemStack, tooltipContext, tooltip, type, true);

            if (!Platform.isNeoForge()) { // NeoForge / Architectury 1.21.1 conflict. Have to disable this on NeoForge. Can re-enable post 1.21.3 :/
                RegistryEntry<GemPower> mainComponent = component.runicPower();
                String powerId = mainComponent.getIdAsString()
                        .replaceAll("[A-Za-z0-9_-]+:", "")
                        .replace("greater_", "");
                return Identifier.of(runicPath + "/" + powerId + ".mdx");
            }
        }

        return generateDefaultTooltipEntry(itemStack, itemPath);
    }

    public static void processCtrlAltNavigation(Identifier entry, String modId, Identifier customConfigPath, ItemStack itemStack, List<Text> tooltip) {
        String customPath;
        if (isControlDown()) {
            if (ctrlKeyPressTimestamp == 0) {
                ctrlKeyPressTimestamp = System.currentTimeMillis();
            }

            // Show error message in tooltip if no info mods installed
            if (!Platform.isModLoaded("oracle_index") && !Platform.isModLoaded("roughlyenoughitems") && !Platform.isModLoaded("emi")) {
                tooltip.add(Text.translatable("message.simplyswords.documentation.error").setStyle(Styles.TEXT));
                tooltip.add(Text.translatable("message.simplyswords.documentation.error2").setStyle(Styles.TEXT));
            }

            if ((System.currentTimeMillis() - ctrlKeyPressTimestamp) >= 500) {
                if (isAltDown()) {
                    if (customConfigPath == null) {
                        TooltipUtils.openFzzyConfig(modId);
                    }
                    else {
                        customPath = customConfigPath.getPath().replace(modId+":", "");
                        TooltipUtils.openFzzyConfig(customPath);
                    }
                } else {
                    // Open documentation depending on installed info mods
                    if (Platform.isModLoaded("oracle_index")) OracleIndexUtils.openOracleIndex(entry, modId);
                    else if (Platform.isModLoaded("roughlyenoughitems")) ReiUtils.openRei(entry, modId, itemStack);
                    else if (Platform.isModLoaded("emi")) EmiUtils.openEmi(entry, modId, itemStack);
                }
                ctrlKeyPressTimestamp = 0;
            }
        } else {
            ctrlKeyPressTimestamp = 0;
        }
    }
}
