package net.sweenus.simplyswords.client.renderer;

import net.minecraft.util.math.ColorHelper;

public final class LegacyRenderColor {
    private LegacyRenderColor() {
    }

    public static float red(int color) {
        return ColorHelper.Argb.getRed(color) / 255.0F;
    }

    public static float green(int color) {
        return ColorHelper.Argb.getGreen(color) / 255.0F;
    }

    public static float blue(int color) {
        return ColorHelper.Argb.getBlue(color) / 255.0F;
    }

    public static float alpha(int color) {
        return ColorHelper.Argb.getAlpha(color) / 255.0F;
    }
}
