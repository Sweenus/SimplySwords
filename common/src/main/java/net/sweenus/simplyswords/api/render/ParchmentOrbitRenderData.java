package net.sweenus.simplyswords.api.render;

public interface ParchmentOrbitRenderData {

    int MODE_BLESSING = 0;
    int MODE_GROUND_RING = 1;
    int MODE_SEAL_STAMP = 2;

    int getMode();

    int getPageCount();

    float getOrbitRadius();

    float getOrbitHeight();

    int getOwnerEntityId();
}
