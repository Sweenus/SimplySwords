package net.sweenus.simplyswords.client.api;

import net.minecraft.util.Identifier;

public record ParchmentGlyphStyle(
        Identifier glyphTexture,
        Identifier glowTexture,
        int columns,
        int rows,
        int glyphColor,
        int glowColor,
        float haloScale,
        float haloAlpha
) {
    public ParchmentGlyphStyle {
        columns = Math.max(1, columns);
        rows = Math.max(1, rows);
        haloScale = Math.max(1.0F, haloScale);
        haloAlpha = Math.clamp(haloAlpha, 0.0F, 1.0F);
    }
}
