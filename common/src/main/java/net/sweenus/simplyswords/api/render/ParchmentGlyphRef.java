package net.sweenus.simplyswords.api.render;

import net.minecraft.util.Identifier;

public record ParchmentGlyphRef(Identifier styleId, int baseIndex, int glyphCount) {

    public ParchmentGlyphRef {
        styleId = styleId == null ? ParchmentVisualIds.DEFAULT_GLYPH_STYLE : styleId;
        glyphCount = Math.max(1, glyphCount);
    }
}
