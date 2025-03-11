package net.sweenus.simplyswords.client.util;

import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class TooltipUtils {
    public static final Identifier runic_tags = Identifier.of(SimplySwords.MOD_ID, "runic_weapons");

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

    public static void addDynamicButtonTooltip(List<Text> tooltip, Text info, Text search, boolean isAlt, boolean isCtrl) {
        tooltip.add(
                Text.literal("")
                        .append(
                                info.copy().setStyle(info.getStyle().withColor(isAlt ? Styles.UNIQUE.getColor() : Styles.COMMON.getColor()))
                        ) // "INFO" styling
                        .append("\u00A0") // Spacer
                        .append(
                                search.copy().setStyle(search.getStyle().withColor(isCtrl ? Styles.UNIQUE.getColor() : Styles.COMMON.getColor()))
                        ) // "SEARCH" styling
        );
    }

    public static void appendSpellScaleTooltip(List<Text> tooltip, String spellSchool) {
        if (Platform.isModLoaded("spell_power") || Platform.isModLoaded("irons_spellbooks")) {
            if (Screen.hasAltDown()) {
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
        return Screen.hasAltDown() // Don't hide info on these items
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

    public static void commonPatchouli(Identifier entry) {
        SimplySwordsExpectPlatform.openPatchouli(entry);
    }


}