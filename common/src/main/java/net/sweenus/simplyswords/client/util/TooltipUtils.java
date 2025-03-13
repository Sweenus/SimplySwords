package net.sweenus.simplyswords.client.util;

import dev.architectury.platform.Platform;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import rearth.oracle.ui.OracleScreen;

import java.util.List;

public class TooltipUtils {
    public static final Identifier runic_tags = Identifier.of(SimplySwords.MOD_ID, "runic_weapons");
    private static long ctrlKeyPressTimestamp = 0;


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
            if (Screen.hasAltDown() && !Screen.hasControlDown()) {
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
        return (Screen.hasAltDown() && !Screen.hasControlDown()) // Don't hide info on these items
                || HelperMethods.isInTag(stack, tagId)
                || stack.isOf(ItemsRegistry.RUNEFUSED_GEM.get())
                || stack.isOf(ItemsRegistry.NETHERFUSED_GEM.get());
    }


    public static void openPatchouli(Identifier entry) {
        if (Platform.isModLoaded("patchouli")) {
            if (entry.getPath().contains("lichblade"))
                entry = Identifier.of("simplyswords:uniques/entry_slumbering_lichblade");
            if (entry.getPath().contains("righteous_relic"))
                entry = Identifier.of("simplyswords:uniques/entry_dormant_relic");
            if (entry.getPath().contains("tainted_relic"))
                entry = Identifier.of("simplyswords:uniques/entry_dormant_relic");

            commonPatchouli(entry);
        }
    }

    public static void openOracleIndex(Identifier identifier) {
        if (!(MinecraftClient.getInstance().currentScreen instanceof OracleScreen)) {
            if (identifier.getPath().contains("lichblade")) // Lichblade variants are contained within one wiki entry
                identifier = Identifier.of("oracle_index:books/simplyswords/unique-weapons/lichblade.mdx");

            OracleScreen.activeBook = "simplyswords";
            OracleScreen.activeEntry = identifier;
            MinecraftClient.getInstance().setScreen(new OracleScreen());
        }
    }

    private static boolean isTooltipRendering = false;
    public static void openFzzyConfig(Identifier identifier) {
        if (isTooltipRendering) {
            return;
        }

        isTooltipRendering = true;
        try {
            ConfigApiJava.INSTANCE.openScreen("simplyswords.unique_effects.flickerfury");
        } finally {
            isTooltipRendering = false;
        }
    }

    public static void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        Identifier entry = Identifier.of("oracle_index:books/simplyswords/weapon-types/" + itemStack.getItem().getRegistryEntry().registryKey().getValue().getPath() + ".mdx");
        TooltipUtils.addDynamicButtonTooltip(
                tooltip,
                Text.translatable("item.simplyswords.common.showtooltip.info"),
                Text.translatable("item.simplyswords.common.showtooltip.search"),
                Text.translatable("item.simplyswords.common.showtooltip.config"),
                Screen.hasAltDown(),
                Screen.hasControlDown()
        );
        if (itemStack.getItem() instanceof UniqueSwordItem) {
            tooltip.add(Text.literal(""));
            SimplySwordsAPI.appendTooltipGemSocketLogic(itemStack, tooltipContext, tooltip, type);
            entry = Identifier.of("oracle_index:books/simplyswords/unique-weapons/" + itemStack.getItem().getRegistryEntry().registryKey().getValue().getPath() + ".mdx");
        }
        else if (itemStack.getItem() instanceof RunicSwordItem) {
            tooltip.add(Text.literal(""));
            GemPowerComponent component = SimplySwordsAPI.getComponent(itemStack);
            if (component.isEmpty()) {
                tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").setStyle(Styles.RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(Styles.TEXT));
            } else {
                component.appendTooltip(itemStack, tooltipContext, tooltip, type, true);
            }
        }
        if (Screen.hasControlDown()) {
            if (ctrlKeyPressTimestamp == 0) {
                ctrlKeyPressTimestamp = System.currentTimeMillis();
            }
            // Check if Control has been held for at least 500 milliseconds (0.5 seconds)
            if ((System.currentTimeMillis() - ctrlKeyPressTimestamp) >= 500) {
                if (Screen.hasAltDown())
                    openFzzyConfig(entry);
                else TooltipUtils.openOracleIndex(entry);
                ctrlKeyPressTimestamp = 0;
            }
        } else {
            ctrlKeyPressTimestamp = 0;
        }
    }

    public static void commonPatchouli(Identifier entry) {
        SimplySwordsExpectPlatform.openPatchouli(entry);
    }


}