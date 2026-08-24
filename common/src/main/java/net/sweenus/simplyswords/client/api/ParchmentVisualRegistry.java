package net.sweenus.simplyswords.client.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.render.ParchmentVisualIds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ParchmentVisualRegistry {

    private static final ParchmentGlyphStyle DEFAULT = new ParchmentGlyphStyle(
            Identifier.of("simplyswords", "textures/entity/parchment_glyphs.png"),
            Identifier.of("simplyswords", "textures/entity/parchment_glyphs_glow.png"),
            8, 8, 0xFFFFFFFF, 0xFFFFFFFF, 1.7F, 0.55F);
    private static final Map<Identifier, ParchmentGlyphStyle> GLYPH_STYLES = new HashMap<>();
    private static final Set<Identifier> MISSING_STYLES = new HashSet<>();

    static {
        GLYPH_STYLES.put(ParchmentVisualIds.DEFAULT_GLYPH_STYLE, DEFAULT);
    }

    private ParchmentVisualRegistry() {
    }

    public static void registerGlyphStyle(Identifier id, ParchmentGlyphStyle style) {
        if (id == null || style == null) {
            throw new IllegalArgumentException("Parchment glyph styles require an id and value");
        }
        ParchmentGlyphStyle previous = GLYPH_STYLES.putIfAbsent(id, style);
        if (previous != null && !previous.equals(style)) {
            throw new IllegalStateException("Duplicate parchment glyph style " + id);
        }
    }

    public static ParchmentGlyphStyle getGlyphStyle(Identifier id) {
        ParchmentGlyphStyle style = GLYPH_STYLES.get(id);
        if (style != null) {
            return style;
        }
        if (id != null && MISSING_STYLES.add(id)) {
            SimplySwords.LOGGER.warn("Unknown parchment glyph style {}, using default", id);
        }
        return DEFAULT;
    }
}
