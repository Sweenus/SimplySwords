package net.sweenus.simplyswords.api.render;

public record LightningPhenomenonStyle(
        LightningPhenomenonShape shape,
        int primaryColor,
        int coreColor,
        int lifetimeTicks,
        int delayTicks,
        int leaderTicks,
        float coreWidth,
        int branches,
        float spread,
        boolean illuminate
) {
    public LightningPhenomenonStyle {
        shape = shape == null ? LightningPhenomenonShape.DIRECT_FLASH : shape;
        lifetimeTicks = Math.max(1, lifetimeTicks);
        delayTicks = Math.max(0, delayTicks);
        leaderTicks = Math.max(0, Math.min(lifetimeTicks, leaderTicks));
        coreWidth = Math.max(0.006F, coreWidth);
        branches = Math.max(0, Math.min(16, branches));
        spread = Math.max(0.0F, spread);
    }
}
