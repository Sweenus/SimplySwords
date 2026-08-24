package net.sweenus.simplyswords.client.util;

import dev.architectury.platform.Platform;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.Arrays;
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

        // Initialize the hold timer on the first frame Ctrl is pressed
        TextColor searchColor;
        if (isCtrl && !isAlt) {
            if (ctrlKeyPressTimestamp == 0) {
                ctrlKeyPressTimestamp = System.currentTimeMillis();
            }
            float progress = Math.min(1.0f, (System.currentTimeMillis() - ctrlKeyPressTimestamp) / 1200.0f);
            searchColor = TextColor.fromRgb(lerpColor(0xFFFFFF, 0xE2A834, progress));
        } else {
            searchColor = isAltAndCtrl ? Styles.COMMON.getColor() : Styles.COMMON.getColor();
        }

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
                                        Style.EMPTY.withColor(searchColor)
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

    /** Linear interpolation between two packed RGB colours. */
    private static int lerpColor(int from, int to, float t) {
        int r = (int) (((from >> 16) & 0xFF) + t * (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)));
        int g = (int) (((from >>  8) & 0xFF) + t * (((to >>  8) & 0xFF) - ((from >>  8) & 0xFF)));
        int b = (int) ((from         & 0xFF) + t * ((to         & 0xFF) - (from         & 0xFF)));
        return (r << 16) | (g << 8) | b;
    }



    private static String schoolGlyph(String school) {
        if (school.contains("lightning")) return "\uAB44";
        if (school.contains("fire")) return "\uAB42";
        if (school.contains("frost")) return "\uAB43";
        if (school.contains("soul") || school.contains("eldritch")) return "\uAB45";
        if (school.contains("arcane") || school.contains("evocation")) return "\uAB46";
        return "\uAB47";
    }

    public static void appendSpellScaleTooltip(List<Text> tooltip, String spellSchool) {
        if (Platform.isModLoaded("spell_power") || Platform.isModLoaded("irons_spellbooks")) {
            if (Screen.hasAltDown() && !Screen.hasControlDown()) {
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.compat.spellScaling").setStyle(Styles.COMMON));
                MutableText line = Text.empty();
                String[] parts = (spellSchool == null ? "" : spellSchool).split("_");
                for (int index = 0; index < parts.length; index++) {
                    String part = parts[index];
                    if (part.isEmpty()) {
                        continue;
                    }
                    if (index > 0) {
                        line.append(Text.literal("   "));
                    }
                    line.append(Text.literal(schoolGlyph(part)))
                            .append(Text.translatable(SimplySwordsExpectPlatform.getSpellSchoolDisplayKey(part)));
                }
                tooltip.add(line);
                tooltip.add(Text.literal(""));
            }
        }
    }

    public static void appendSpellScaleTooltip(List<Text> tooltip, SpellScalingProfile profile) {
        appendSpellScaleTooltip(tooltip,
                (profile == null ? SpellScalingProfile.ARCANE : profile).registryId());
    }

    public static void appendWeaponSpellScaleTooltip(List<Text> tooltip, ItemStack stack, String spellSchools) {
        String[] parts = (spellSchools == null ? "" : spellSchools).split("_");
        List<Identifier> components = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                components.add(SpellScalingComponents.weaponComponent(
                        stack, SpellScalingProfile.fromLegacyName(part)));
            }
        }
        appendSpellScaleTooltip(tooltip, components.toArray(Identifier[]::new));
    }

    public static void appendWeaponSpellScaleTooltip(List<Text> tooltip, ItemStack stack,
                                                      SpellScalingProfile... profiles) {
        SpellScalingProfile[] requested = profiles == null || profiles.length == 0
                ? new SpellScalingProfile[]{SpellScalingProfile.ARCANE}
                : profiles;
        appendSpellScaleTooltip(tooltip, Arrays.stream(requested)
                .map(profile -> SpellScalingComponents.weaponComponent(stack, profile))
                .distinct()
                .toArray(Identifier[]::new));
    }

    public static void appendGemPowerSpellScaleTooltip(List<Text> tooltip, String powerPath) {
        appendSpellScaleTooltip(tooltip, SpellScalingComponents.power(powerPath));
    }

    public static void appendSpellScaleTooltip(List<Text> tooltip, Identifier... scalingProfileIds) {
        if ((Platform.isModLoaded("spell_power") || Platform.isModLoaded("irons_spellbooks"))
                && Screen.hasAltDown() && !Screen.hasControlDown()) {
            Identifier[] profileIds = scalingProfileIds == null || scalingProfileIds.length == 0
                    ? new Identifier[]{SpellScalingProfile.ARCANE.registryId()}
                    : scalingProfileIds;
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.spellScaling").setStyle(Styles.COMMON));
            for (Identifier profileId : profileIds) {
                if (profileId == null) {
                    continue;
                }
                SpellSchoolDisplay display = SimplySwordsExpectPlatform.getActiveSpellSchoolDisplay(profileId);
                MutableText line = Text.empty();
                SpellScalingComponents.get(profileId)
                        .map(SpellScalingComponents.Definition::effectTranslationKey)
                        .filter(key -> !key.isBlank())
                        .ifPresent(key -> line.append(Text.translatable(key)).append(Text.literal(": ")));
                line.append(Text.literal(schoolGlyph(display.schoolId().getPath())))
                        .append(display.name());
                tooltip.add(line);
            }
            tooltip.add(Text.literal(""));
        }
    }

    public static int getEffectiveWeaponCooldownTicks(ItemStack stack, int baseCooldownTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Math.max(0, baseCooldownTicks);
        }
        return SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, client.player, baseCooldownTicks);
    }

    public static boolean shouldDisplayTooltip(ItemStack stack, Identifier tagId) {
        return (Screen.hasAltDown() && !Screen.hasControlDown()) // Don't hide info on these items
                || (tagId != null && HelperMethods.isInTag(stack, tagId))
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
        List<Text> lines = new ArrayList<>();
        SimplySwordsAPI.appendTooltipGemSocketLogic(itemStack, tooltipContext, lines, type);
        appendWithSeparator(tooltip, lines);

        return Identifier.of(uniquePath + "/" +
                itemStack.getItem().getRegistryEntry().registryKey().getValue().getPath() + ".mdx");
    }

    public static Identifier handleRunicSwordTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type, String itemPath, String runicPath) {
        List<Text> lines = new ArrayList<>();
        Identifier entry = generateDefaultTooltipEntry(itemStack, itemPath);

        GemPowerComponent component = SimplySwordsAPI.getComponent(itemStack);
        if (component.isEmpty()) {
            lines.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").setStyle(Styles.RUNIC));
            lines.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(Styles.TEXT));
        } else {
            component.appendTooltip(itemStack, tooltipContext, lines, type, true);

            if (component.hasRunicSlotFilled()) {
                String powerId = component.runicPower().getPath().replace("greater_", "");
                entry = Identifier.of(runicPath + "/" + powerId + ".mdx");
            }
        }

        appendWithSeparator(tooltip, lines);
        return entry;
    }

    // Appends a single blank separator line followed by the given lines, and does nothing at
    // all when there is nothing to append.
    private static void appendWithSeparator(List<Text> tooltip, List<Text> lines) {
        int start = 0;
        while (start < lines.size() && lines.get(start).getString().isEmpty()) {
            start++;
        }
        if (start == lines.size()) {
            return;
        }
        tooltip.add(Text.literal(""));
        tooltip.addAll(lines.subList(start, lines.size()));
    }

    public static void processCtrlAltNavigation(Identifier entry, String modId, Identifier customConfigPath, ItemStack itemStack, List<Text> tooltip) {
        String customPath;
        if (Screen.hasControlDown()) {
            // Note: ctrlKeyPressTimestamp is initialised in addDynamicButtonTooltip(),
            // which is called earlier in the same tooltip-render frame.

            // Show error message in tooltip if no info mods installed
            if (!Platform.isModLoaded("oracle_index") && !Platform.isModLoaded("roughlyenoughitems") && !Platform.isModLoaded("emi")) {
                tooltip.add(Text.translatable("message.simplyswords.documentation.error").setStyle(Styles.TEXT));
                tooltip.add(Text.translatable("message.simplyswords.documentation.error2").setStyle(Styles.TEXT));
            }

            if ((System.currentTimeMillis() - ctrlKeyPressTimestamp) >= 500) {
                if (Screen.hasAltDown()) {
                    if (customConfigPath == null) {
                        TooltipUtils.openFzzyConfig(modId);
                    }
                    else {
                        customPath = customConfigPath.getPath().replace(modId+":", "");
                        TooltipUtils.openFzzyConfig(customPath);
                    }
                    ctrlKeyPressTimestamp = 0;
                } else if (Platform.isModLoaded("oracle_index")) {
                    // Do NOT reset ctrlKeyPressTimestamp here — the gradient must continue
                    // running from 500 ms up to 1 200 ms so the icon finishes filling.
                    // OracleIndexUtils guards against re-setting pendingSetTime on repeat calls.
                    OracleIndexUtils.openOracleIndex(entry, modId);
                } else {
                    // REI / EMI open instantly, so the gradient can stop immediately.
                    if (Platform.isModLoaded("roughlyenoughitems")) ReiUtils.openRei(entry, modId, itemStack);
                    else if (Platform.isModLoaded("emi")) EmiUtils.openEmi(entry, modId, itemStack);
                    ctrlKeyPressTimestamp = 0;
                }
            }
        } else {
            ctrlKeyPressTimestamp = 0;
        }
    }


}
