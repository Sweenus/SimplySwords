package net.sweenus.simplyswords.api.render;

public record StormVolumeStyle(
        StormVolumeShape shape,
        int cloudColor,
        int accentColor,
        int lifetimeTicks,
        float radius,
        float height,
        int density,
        float rotationSpeed,
        float precipitation,
        boolean illuminate
) {
    public StormVolumeStyle {
        shape = shape == null ? StormVolumeShape.SHELF_CLOUD : shape;
        lifetimeTicks = Math.max(1, lifetimeTicks);
        radius = Math.max(0.1F, radius);
        height = Math.max(0.1F, height);
        density = Math.max(1, Math.min(96, density));
        precipitation = Math.max(0.0F, Math.min(1.0F, precipitation));
    }
}
