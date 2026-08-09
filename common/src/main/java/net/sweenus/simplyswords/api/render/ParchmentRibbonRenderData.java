package net.sweenus.simplyswords.api.render;

import net.minecraft.util.math.Vec3d;

public interface ParchmentRibbonRenderData {

    int STYLE_LINK = 0;
    int STYLE_SKY_SPIRAL = 1;
    int STYLE_NOVA_RING = 2;
    int STYLE_ARC = 3;

    int getStyle();

    Vec3d getEndOffset();

    int getSeed();

    float getHalfWidth();

    float getLifeProgress(float tickDelta);
}
