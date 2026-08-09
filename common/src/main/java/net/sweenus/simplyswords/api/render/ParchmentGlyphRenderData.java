package net.sweenus.simplyswords.api.render;

import net.minecraft.util.Identifier;

public interface ParchmentGlyphRenderData {

    default Identifier getGlyphStyleId() {
        return ParchmentVisualIds.DEFAULT_GLYPH_STYLE;
    }

    int getGlyphBase();

    default int getGlyphCount() {
        return 7;
    }
}
