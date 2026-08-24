package net.sweenus.simplyswords.api.render;

public record ShockFrontStyle(
        int color,
        int lifetimeTicks,
        float startRadius,
        float endRadius,
        float width,
        float height
) {
    public ShockFrontStyle {
        lifetimeTicks = Math.max(1, lifetimeTicks);
        startRadius = Math.max(0.0F, startRadius);
        endRadius = Math.max(startRadius, endRadius);
        width = Math.max(0.1F, width);
        height = Math.max(0.1F, height);
    }
}
