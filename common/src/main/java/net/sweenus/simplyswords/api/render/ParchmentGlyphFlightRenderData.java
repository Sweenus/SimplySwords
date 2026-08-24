package net.sweenus.simplyswords.api.render;

import net.minecraft.util.Identifier;

public interface ParchmentGlyphFlightRenderData {

    Identifier getGlyphStyleId();

    int getGlyphCount();

    int getOwnerEntityId();

    int getSeed();

    int getLifetime();

    float getLifeProgress(float tickDelta);
}
