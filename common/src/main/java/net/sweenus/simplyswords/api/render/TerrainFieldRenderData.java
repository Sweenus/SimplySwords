package net.sweenus.simplyswords.api.render;

import net.minecraft.util.Identifier;

public interface TerrainFieldRenderData {

    float getRadius();

    default Identifier getTerrainStyleId() {
        return ParchmentVisualIds.WHITE_MARBLE;
    }
}
