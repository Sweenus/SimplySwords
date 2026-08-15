package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IonDeferredRenderController;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.SoulstalkerTentacleVisualEntity;
import org.joml.Matrix4f;

public final class SoulstalkerTentacleVisualEntityRenderer extends EntityRenderer<SoulstalkerTentacleVisualEntity> {
    private static final Identifier WHITE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int SEGMENTS = 12;

    public SoulstalkerTentacleVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(SoulstalkerTentacleVisualEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(SoulstalkerTentacleVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(SoulstalkerTentacleVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        float reach = reach(entity, age);
        float fade = MathHelper.clamp((entity.getLifetime() - age) / 5.0F, 0.0F, 1.0F);
        if (reach <= 0.01F || fade <= 0.01F) {
            return;
        }
        Vec3d origin = entity.getLerpedPos(tickDelta);
        Vec3d root = entity.getRoot(tickDelta);
        Vec3d target = entity.getEndpoint(tickDelta);
        Vec3d endpoint = root.lerp(target, reach);
        Vec3d[] path = buildPath(root, endpoint, origin, entity.getBackDirection(tickDelta), entity.getSeed(), age);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int packedLight = LightmapTextureManager.pack(
                Math.max(5, LightmapTextureManager.getBlockLightCoordinates(light)),
                LightmapTextureManager.getSkyLightCoordinates(light));
        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        for (int segment = 0; segment < path.length - 1; segment++) {
            float progress = segment / (float) (path.length - 1);
            float width = MathHelper.lerp(progress, 0.17F, 0.046F);
            int color = ((segment + entity.getSeed()) & 2) == 0 ? 0x08040D : 0x160922;
            SoulstalkerRenderGeometry.prism(dark, matrix, path[segment], path[segment + 1],
                    width, color, Math.round(245.0F * fade), packedLight, false);
        }
        VertexConsumer glow = IonDeferredRenderController.getLightningBuffer();
        float impactPulse = MathHelper.clamp(1.0F - Math.abs(age - entity.getImpactAge()) / 4.0F, 0.0F, 1.0F);
        for (int segment = 0; segment < path.length - 1; segment++) {
            float progress = segment / (float) (path.length - 1);
            float pulse = MathHelper.clamp(impactPulse - Math.abs(progress - reach) * 0.3F, 0.0F, 1.0F);
            SoulstalkerRenderGeometry.crossRibbon(glow, matrix, path[segment], path[segment + 1],
                    MathHelper.lerp(progress, 0.034F, 0.011F) * (1.0F + pulse * 0.55F),
                    pulse > 0.1F ? 0x9361B8 : 0x68208F,
                    Math.round((92.0F + pulse * 74.0F) * fade));
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static Vec3d[] buildPath(Vec3d root, Vec3d end, Vec3d origin, Vec3d back,
                                     int seed, float age) {
        Vec3d line = end.subtract(root);
        double length = Math.max(0.001, line.length());
        Vec3d side = new Vec3d(-line.z, 0.0, line.x);
        if (side.lengthSquared() < 1.0E-6) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        double sign = (seed & 1) == 0 ? 1.0 : -1.0;
        double bend = Math.min(1.4, 0.45 + length * 0.1);
        Vec3d firstControl = root.add(back.multiply(0.82 + Math.min(0.45, length * 0.035)))
                .add(side.multiply(sign * bend * 0.42)).add(0.0, 0.28, 0.0);
        Vec3d approach = root.subtract(end);
        if (approach.lengthSquared() < 1.0E-6) {
            approach = back;
        } else {
            approach = approach.normalize();
        }
        Vec3d secondControl = end.add(approach.multiply(Math.min(0.82, 0.32 + length * 0.045)))
                .add(side.multiply(-sign * bend * 0.22)).add(0.0, 0.12, 0.0);
        Vec3d[] points = new Vec3d[SEGMENTS + 1];
        for (int index = 0; index <= SEGMENTS; index++) {
            double progress = index / (double) SEGMENTS;
            double inverse = 1.0 - progress;
            Vec3d point = root.multiply(inverse * inverse * inverse)
                    .add(firstControl.multiply(3.0 * inverse * inverse * progress))
                    .add(secondControl.multiply(3.0 * inverse * progress * progress))
                    .add(end.multiply(progress * progress * progress));
            double envelope = Math.sin(progress * Math.PI);
            double flex = Math.sin(age * 0.3 + seed * 0.001 + progress * Math.PI * 4.0)
                    * envelope * Math.min(0.14, length * 0.018);
            points[index] = point.add(side.multiply(flex)).subtract(origin);
        }
        return points;
    }

    private static float reach(SoulstalkerTentacleVisualEntity entity, float age) {
        float impact = Math.max(1.0F, entity.getImpactAge());
        if (age <= impact) {
            return ease(MathHelper.clamp(age / impact, 0.0F, 1.0F));
        }
        float retractStart = impact + 3.0F;
        if (age <= retractStart) {
            return 1.0F;
        }
        return 1.0F - ease(MathHelper.clamp(
                (age - retractStart) / Math.max(1.0F, entity.getLifetime() - retractStart),
                0.0F, 1.0F));
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
