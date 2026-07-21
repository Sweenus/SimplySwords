package net.sweenus.simplyswords.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridge provider that renders Simply Swords items using the Simply Tooltips engine.
 * Intercepts all simplyswords-namespace SwordItem instances.
 */
public final class SimplySwordsTooltipProvider implements TooltipProvider {

    @Override
    public boolean supports(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return false;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id != null && "simplyswords".equals(id.getNamespace());
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String title = rawLines.isEmpty() ? stack.getName().getString() : rawLines.get(0).getString();

        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path   = id != null ? id.getPath() : "";

        // ---- Badges: weapon type + rarity ----
        List<String> badges = new ArrayList<>();
        for (String type : WEAPON_TYPES) {
            if (path.contains(type)) { badges.add(type.toUpperCase()); break; }
        }
        String rarityBadge = "COMMON";
        if (stack.getItem() instanceof UniqueSwordItem u) {
            rarityBadge = u.getItemRarity(); // "UNIQUE" or "LEGENDARY"
            badges.add(rarityBadge);
        }

        // ---- Theme key: drive theme resolution in TooltipRenderer ----
        String themeKey = null;
        if ("LEGENDARY".equals(rarityBadge)) themeKey = "rarity_epic";   // purple
        else if ("UNIQUE".equals(rarityBadge)) themeKey = "rarity_rare";  // aqua

        // ---- Parse ability + attribute lines from rawLines ----
        List<String> abilityLines = parseAbilityLines(rawLines);
        List<String> implicitLines = parseImplicitLines(stack, altDown);
        if (!implicitLines.isEmpty()) {
            abilityLines.addAll(0, implicitLines);
        }
        List<Text>   extraLines   = parseExtraLines(rawLines);
        Text         hint         = parseHintLine(rawLines);

        return new ModernTooltipModel(
                title,
                badges,
                TooltipBorderStyle.DEFAULT,
                abilityLines,
                List.of(),
                extraLines,
                TooltipTheme.defaultTheme(),
                null,
                null,
                themeKey,
                hint,
                List.of()
        );
    }

    // --- Constants ---

    /**
     * Known weapon types matched against the registry path.
     * Order matters: "warglaive" must precede "glaive" to avoid a substring false-match.
     */
    private static final String[] WEAPON_TYPES = {
            "longsword", "twinblade", "rapier", "katana", "sai", "spear",
            "warglaive", "glaive", "cutlass", "claymore", "greathammer",
            "greataxe", "chakram", "halberd", "scythe"
    };

    /**
     * Action-label prefixes that should be rendered as sub-section headers inside the LORE tab.
     * Matching is case-insensitive against the start of each ability line.
     */
    private static final String[] SECTION_HEADER_PREFIXES = {
            "on right click",
            "hold right click",
            "on left click",
            "hold left click",
            "on sneak",
    };

    // --- Parsing helpers ---

    /**
     * Collects ability description lines from rawLines[1..N].
     * Stops at the vanilla attribute block header ("When in Main Hand" / "When in Off Hand").
     * Skips button-hint lines; preserves internal blank lines as paragraph separators.
     * Action-label lines ("On Right Click:", etc.) are prefixed with
     * {@link ModernTooltipModel#SECTION_MARKER} so the renderer draws them as sub-section headers.
     */
    private static List<String> parseAbilityLines(List<Text> rawLines) {
        List<String> result = new ArrayList<>();
        String attrMain = Text.translatable("item.modifiers.mainhand").getString();
        String attrOff  = Text.translatable("item.modifiers.offhand").getString();
        String implicitHeader = Text.translatable("tooltip.simplyswords.implicit.header").getString();
        String runicHeader = "Runic Power";

        boolean seenContent = false;
        boolean inRunicSection = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
            if (s.equals(attrMain) || s.equals(attrOff)) break;
            if (isButtonHint(s)) continue;
            if (s.equals(implicitHeader)) {
                i = skipImplicitValue(rawLines, i, attrMain, attrOff);
                continue;
            }

            if (s.isBlank()) {
                if (seenContent) result.add("");  // blank line between paragraphs → visual gap
            } else if (isRunicPowerLine(s) || isRunicModifierLine(s)) {
                if (!inRunicSection) {
                    if (!result.isEmpty() && result.get(result.size() - 1).isBlank()) {
                        result.remove(result.size() - 1);
                    }
                    result.add(ModernTooltipModel.SECTION_MARKER + runicHeader);
                    inRunicSection = true;
                }
                result.add(formatRunicLine(s));
                seenContent = true;
            } else if (isSectionHeader(s) && !inRunicSection) {
                // Consume the preceding blank (separator replaces the visual gap)
                if (!result.isEmpty() && result.get(result.size() - 1).isBlank()) {
                    result.remove(result.size() - 1);
                }
                result.add(ModernTooltipModel.SECTION_MARKER + s);
                seenContent = true;
                inRunicSection = false;
            } else {
                seenContent = true;
                result.add(s);
            }
        }
        // Trim trailing blank lines
        while (!result.isEmpty() && result.get(result.size() - 1).isBlank()) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static int skipImplicitValue(List<Text> rawLines, int headerIndex, String attrMain, String attrOff) {
        int i = headerIndex;
        for (int j = headerIndex + 1; j < rawLines.size(); j++) {
            String line = rawLines.get(j).getString();
            if (line.isBlank() || line.equals(attrMain) || line.equals(attrOff) || isSectionHeader(line) || isButtonHint(line)) {
                break;
            }
            i = j;
        }
        return i;
    }

    /**
     * Collects vanilla attribute Text lines (attack damage / attack speed block).
     * These populate the STATS tab in the ST renderer.
     */
    private static List<Text> parseExtraLines(List<Text> rawLines) {
        List<Text> result = new ArrayList<>();
        String attrMain = Text.translatable("item.modifiers.mainhand").getString();
        String attrOff  = Text.translatable("item.modifiers.offhand").getString();
        boolean inAttr = false;

        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
            if (s.equals(attrMain) || s.equals(attrOff)) inAttr = true;
            if (inAttr) result.add(rawLines.get(i));
        }
        return result;
    }

    private static List<String> parseImplicitLines(ItemStack stack, boolean altDown) {
        Text implicit = WeaponImplicitRegistry.formatTooltip(stack, altDown);
        if (implicit == null) {
            return List.of();
        }
        return List.of(
                ModernTooltipModel.SECTION_MARKER + Text.translatable("tooltip.simplyswords.implicit.header").getString(),
                implicit.getString()
        );
    }

    /**
     * Returns the first button-hint {@link Text} found in rawLines (preserving its styled colours),
     * or {@code null} if none exists. The hint is shown below the badge row in the header area.
     */
    private static Text parseHintLine(List<Text> rawLines) {
        for (int i = 1; i < rawLines.size(); i++) {
            if (isButtonHint(rawLines.get(i).getString())) {
                return rawLines.get(i);
            }
        }
        return null;
    }

    /**
     * Detects the interactive tooltip buttons {@code TooltipUtils.addDynamicButtonTooltip()}.
     * SS button glyphs are custom-font Unicode codepoints separated by non-breaking spaces (\u00A0);
     */
    private static boolean isButtonHint(String s) {
        if (s == null || s.isBlank()) return false;

        // The dynamic hint row is made of three labeled segments (info/search/config)
        // separated by NBSP runs. Ability description lines may also contain NBSP for
        // indentation, so require at least three non-blank segments to avoid false matches.
        String[] parts = s.split("\\u00A0+");
        int nonBlankParts = 0;
        for (String part : parts) {
            if (!part.isBlank()) nonBlankParts++;
        }
        return nonBlankParts >= 3;
    }

    /**
     * Returns {@code true} if this line should be rendered as a sub-section header
     * (separator above + sectionHeader colour). Matches known action-label prefixes
     * case-insensitively anywhere in the string, because SS prepends a custom glyph
     * icon (\uAB40) before the human-readable text (e.g. "\uAB40 On Right Click: ").
     */
    private static boolean isSectionHeader(String s) {
        String lower = s.toLowerCase();
        for (String prefix : SECTION_HEADER_PREFIXES) {
            if (lower.contains(prefix)) return true;
        }
        return false;
    }

    private static boolean isRunicPowerLine(String s) {
        return s != null && s.startsWith("Runic Power:");
    }

    private static boolean isRunicModifierLine(String s) {
        return "Greater".equals(s);
    }

    private static String formatRunicLine(String s) {
        if (isRunicPowerLine(s)) {
            return s.substring("Runic Power:".length()).trim();
        }
        return s;
    }
}
