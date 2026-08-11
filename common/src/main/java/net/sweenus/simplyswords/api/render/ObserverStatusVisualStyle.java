package net.sweenus.simplyswords.api.render;

import net.minecraft.util.Identifier;

public record ObserverStatusVisualStyle(
        ObserverStatusVisualShape shape,
        int primaryColor,
        int coreColor,
        int density,
        float scale,
        Identifier glyphStyleId
) {
    public ObserverStatusVisualStyle(ObserverStatusVisualShape shape, int primaryColor, int coreColor,
                                     int density, float scale) {
        this(shape, primaryColor, coreColor, density, scale, ParchmentVisualIds.DEFAULT_GLYPH_STYLE);
    }

    public ObserverStatusVisualStyle {
        shape = shape == null ? ObserverStatusVisualShape.STATIC_STREAKS : shape;
        density = Math.max(1, Math.min(8, density));
        scale = Math.max(0.1F, Math.min(3.0F, scale));
        glyphStyleId = glyphStyleId == null ? ParchmentVisualIds.DEFAULT_GLYPH_STYLE : glyphStyleId;
    }
}
