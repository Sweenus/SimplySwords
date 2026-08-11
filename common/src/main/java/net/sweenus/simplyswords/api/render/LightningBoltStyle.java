package net.sweenus.simplyswords.api.render;

public record LightningBoltStyle(
        int color,
        int lifetimeTicks,
        float thickness,
        int branches,
        boolean illuminate
) {

    public LightningBoltStyle {
        lifetimeTicks = Math.max(1, lifetimeTicks);
        thickness = Math.max(0.01F, thickness);
        branches = Math.max(0, branches);
    }
}
