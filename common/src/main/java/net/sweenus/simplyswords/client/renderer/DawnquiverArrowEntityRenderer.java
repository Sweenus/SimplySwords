package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DawnquiverArrowEntity;
import org.joml.Matrix4f;

public final class DawnquiverArrowEntityRenderer extends EntityRenderer<DawnquiverArrowEntity> {

    private static final Identifier WHITE_TEXTURE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int TRAIL_SPANS = 14;

    public DawnquiverArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(DawnquiverArrowEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(DawnquiverArrowEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(14.0));
    }

    @Override
    public void render(DawnquiverArrowEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }

        Vec3d velocity = entity.getVelocity();
        if (velocity.lengthSquared() < 1.0E-6) {
            return;
        }
        Vec3d forward = velocity.normalize();
        Vec3d reference = Math.abs(forward.y) < 0.88
                ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
        Vec3d side = forward.crossProduct(reference).normalize();
        Vec3d up = forward.crossProduct(side).normalize();

        float scale = entity.getScale();
        float age = entity.age + tickDelta;
        float pulse = 0.92F + MathHelper.sin(age * 0.72F + entity.getSeed()) * 0.08F;
        drawPass(DawnquiverRenderPass.BODY, DawnquiverRenderPass.BODY.getBuffer(consumers), entity,
                matrices, forward, side, up, scale, age, pulse);
        drawPass(DawnquiverRenderPass.GLOW, DawnquiverRenderPass.GLOW.getBuffer(consumers), entity,
                matrices, forward, side, up, scale, age, pulse);

        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void drawPass(DawnquiverRenderPass pass, VertexConsumer vertices,
                          DawnquiverArrowEntity entity, MatrixStack matrices,
                          Vec3d forward, Vec3d side, Vec3d up,
                          float scale, float age, float pulse) {
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        if (Config.general.enableModernFieldEffects) {
            drawTrail(pass, vertices, matrix, forward, side, up, scale, age, entity.getSeed());
            if (pass.isGlow()) {
                drawReleaseWisps(vertices, matrix, forward, side, up, scale, age);
            }
        }
        drawShaft(pass, vertices, matrix, forward, side, up, scale * pulse);
        if (entity.getMode() == DawnquiverArrowEntity.MODE_PIERCING) {
            drawPiercingLances(pass, vertices, matrix, forward, side, up, scale, age);
        }
        matrices.pop();

        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        drawHead(pass, vertices, matrices.peek().getPositionMatrix(), scale, pulse);
        matrices.pop();
    }

    private static void drawShaft(DawnquiverRenderPass pass, VertexConsumer vertices, Matrix4f matrix,
                                  Vec3d forward, Vec3d side, Vec3d up, float scale) {
        VertexConsumer body = pass.isBody() ? vertices : null;
        VertexConsumer glow = pass.isGlow() ? vertices : null;
        Vec3d head = forward.multiply(1.58 * scale);
        Vec3d tail = forward.multiply(-1.05 * scale);
        double outer = 0.30 * scale;
        double inner = 0.105 * scale;

        DawnquiverRenderGeometry.line(glow, matrix, tail, head, side, outer,
                255, 173, 39, 205);
        DawnquiverRenderGeometry.line(glow, matrix, tail, head, up, outer,
                255, 194, 51, 205);
        DawnquiverRenderGeometry.line(body, matrix, tail, forward.multiply(1.82 * scale), side, inner,
                255, 250, 216, 250);
        DawnquiverRenderGeometry.line(body, matrix, tail, forward.multiply(1.82 * scale), up, inner,
                255, 250, 216, 250);
    }

    private static void drawHead(DawnquiverRenderPass pass, VertexConsumer vertices, Matrix4f matrix,
                                 float scale, float pulse) {
        VertexConsumer body = pass.isBody() ? vertices : null;
        VertexConsumer glow = pass.isGlow() ? vertices : null;
        float outer = 0.78F * scale * pulse;
        DawnquiverRenderGeometry.billboardLens(glow, matrix, Vec3d.ZERO, outer, outer * 0.82F,
                255, 167, 35, 190);
        float mid = outer * 0.55F;
        DawnquiverRenderGeometry.billboardLens(glow, matrix, new Vec3d(0.0, 0.0, 0.002), mid, mid,
                255, 228, 111, 235);
        float core = outer * 0.23F;
        DawnquiverRenderGeometry.billboardLens(body, matrix, new Vec3d(0.0, 0.0, 0.004), core, core,
                255, 255, 241, 255);
        DawnquiverRenderGeometry.rays(glow, matrix, Vec3d.ZERO, 8,
                outer * 0.36, outer * 1.45, outer * 0.09, 0.0F,
                255, 206, 75, 145, 0);
    }

    private static void drawPiercingLances(DawnquiverRenderPass pass, VertexConsumer vertices, Matrix4f matrix,
                                           Vec3d forward, Vec3d side, Vec3d up,
                                           float scale, float age) {
        VertexConsumer body = pass.isBody() ? vertices : null;
        VertexConsumer glow = pass.isGlow() ? vertices : null;
        double separation = (0.22 + MathHelper.sin(age * 0.3F) * 0.035) * scale;
        Vec3d tail = forward.multiply(-1.25 * scale);
        Vec3d head = forward.multiply(1.95 * scale);
        for (double sign : new double[]{-1.0, 1.0}) {
            Vec3d offset = side.multiply(separation * sign);
            DawnquiverRenderGeometry.line(glow, matrix, tail.add(offset), head.add(offset), up,
                    0.11 * scale, 102, 236, 255, 180);
            DawnquiverRenderGeometry.line(body, matrix, tail.add(offset), head.add(offset), up,
                    0.032 * scale, 239, 255, 255, 235);
        }
    }

    private static void drawTrail(DawnquiverRenderPass pass, VertexConsumer vertices, Matrix4f matrix,
                                  Vec3d forward, Vec3d side, Vec3d up,
                                  float scale, float age, int seed) {
        VertexConsumer body = pass.isBody() ? vertices : null;
        VertexConsumer glow = pass.isGlow() ? vertices : null;
        Vec3d[] path = new Vec3d[TRAIL_SPANS + 1];
        for (int span = 0; span <= TRAIL_SPANS; span++) {
            float t = span / (float) TRAIL_SPANS;
            double wave = Math.sin(t * Math.PI * 1.4 + age * 0.16 + seed * 0.01)
                    * t * 0.10 * scale;
            path[span] = forward.multiply(-0.72 * scale - t * 6.8 * scale)
                    .add(side.multiply(wave));
        }

        DawnquiverRenderGeometry.taperedRibbon(glow, matrix, path, side,
                0.48 * scale, 0.008, 255, 156, 28, 210, 0);
        DawnquiverRenderGeometry.taperedRibbon(glow, matrix, path, up,
                0.48 * scale, 0.008, 255, 181, 38, 210, 0);
        DawnquiverRenderGeometry.taperedRibbon(body, matrix, path, side,
                0.15 * scale, 0.002, 255, 241, 190, 230, 0);
        DawnquiverRenderGeometry.taperedRibbon(body, matrix, path, up,
                0.15 * scale, 0.002, 255, 247, 218, 230, 0);

        for (int filament = 0; filament < 3; filament++) {
            Vec3d[] filamentPath = new Vec3d[TRAIL_SPANS + 1];
            double phase = seed * 0.017 + filament * MathHelper.TAU / 3.0;
            for (int span = 0; span <= TRAIL_SPANS; span++) {
                float t = span / (float) TRAIL_SPANS;
                double radius = (0.18 + t * 0.42) * scale;
                double angle = phase + t * MathHelper.TAU * 0.72 + age * 0.08;
                filamentPath[span] = forward.multiply(-0.55 * scale - t * 5.2 * scale)
                        .add(side.multiply(Math.cos(angle) * radius))
                        .add(up.multiply(Math.sin(angle) * radius));
            }
            DawnquiverRenderGeometry.taperedRibbon(glow, matrix, filamentPath,
                    filament % 2 == 0 ? up : side,
                    0.07 * scale, 0.003, 255, 221, 116, 145, 0);
        }
    }

    private static void drawReleaseWisps(VertexConsumer glow, Matrix4f matrix,
                                         Vec3d forward, Vec3d side, Vec3d up,
                                         float scale, float age) {
        float strength = MathHelper.clamp(1.0F - age / 7.0F, 0.0F, 1.0F);
        if (strength <= 0.0F) {
            return;
        }
        for (float sign : new float[]{-1.0F, 1.0F}) {
            Vec3d[] path = new Vec3d[7];
            for (int index = 0; index < path.length; index++) {
                float t = index / (float) (path.length - 1);
                path[index] = forward.multiply((-0.25 - t * 2.0) * scale)
                        .add(side.multiply(sign * Math.sin(t * Math.PI) * 0.85 * scale))
                        .add(up.multiply(sign * (0.08 + t * 0.28) * scale));
            }
            DawnquiverRenderGeometry.taperedRibbon(glow, matrix, path, up,
                    0.18 * scale, 0.008,
                    95, 235, 255, Math.round(190 * strength), 0);
        }
    }
}
