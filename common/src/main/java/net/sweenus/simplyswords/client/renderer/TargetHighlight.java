package net.sweenus.simplyswords.client.renderer;

import net.minecraft.entity.LivingEntity;

public record TargetHighlight(LivingEntity target, Style style) {

    public enum Style {
        SOUL,
        BRIMSTONE,
        WATCHER,
        WAX,
        BRAMBLE
    }
}
