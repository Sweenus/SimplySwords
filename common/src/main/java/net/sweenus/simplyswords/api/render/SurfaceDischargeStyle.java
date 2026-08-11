package net.sweenus.simplyswords.api.render;

public record SurfaceDischargeStyle(
        int primaryColor,
        int coreColor,
        int lifetimeTicks,
        int crawlTicks,
        int pulseInterval,
        float coreWidth,
        int branches,
        float spread
) {
    public SurfaceDischargeStyle {
        lifetimeTicks = Math.max(1, lifetimeTicks);
        crawlTicks = Math.max(1, Math.min(lifetimeTicks, crawlTicks));
        pulseInterval = Math.max(1, pulseInterval);
        coreWidth = Math.max(0.006F, coreWidth);
        branches = Math.max(1, Math.min(24, branches));
        spread = Math.max(0.0F, Math.min(1.0F, spread));
    }
}
