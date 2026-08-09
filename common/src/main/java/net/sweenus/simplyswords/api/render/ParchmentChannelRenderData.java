package net.sweenus.simplyswords.api.render;

public interface ParchmentChannelRenderData extends ParchmentGlyphRenderData {

    int getOwnerEntityId();

    float getProgress();
}
