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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IonDeferredRenderController;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.SoulstalkerCleaveEntity;
import org.joml.Matrix4f;

public final class SoulstalkerCleaveEntityRenderer extends EntityRenderer<SoulstalkerCleaveEntity> {
    private static final Identifier WHITE = new Identifier("minecraft", "textures/misc/white.png");
    private static final int SEGMENTS = 22;
    private static final int WISPS = 5;
    private static final float SWEEP = 52.0F;
    private static final int DARK = 0x0A0410;
    private static final int PURPLE = 0x1C0A2E;
    private static final int GLOW = 0xA548FF;

    public SoulstalkerCleaveEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(SoulstalkerCleaveEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(SoulstalkerCleaveEntity entity, Frustum frustum,
                                double x, double y, double z) {
        double reach = Math.max(2.5, entity.getCurrentWidth() * 1.7);
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(new Box(entity.getPos(), entity.getPos()).expand(reach));
    }

    @Override
    public void render(SoulstalkerCleaveEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        float appear = ease(MathHelper.clamp(age / 2.5F, 0.0F, 1.0F));
        float fade = ease(MathHelper.clamp((1.0F - entity.getProgress()) * 3.5F, 0.0F, 1.0F));
        float width = entity.getCurrentWidth() * appear;
        int alpha = Math.round(240.0F * fade);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw(tickDelta)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(tickDelta)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int packedLight = Math.max(light, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        drawBody(dark, matrix, width, entity.getSeed(), age, alpha, packedLight, false);
        drawWisps(dark, matrix, width, entity.getSeed(), age, DARK,
                Math.round(alpha * 0.78F), packedLight, false);
        VertexConsumer glow = IonDeferredRenderController.getLightningBuffer();
        drawRim(glow, matrix, width, entity.getSeed(), age, Math.round(alpha * 0.95F));
        drawWisps(glow, matrix, width, entity.getSeed(), age, GLOW,
                Math.round(alpha * 0.3F), 0, true);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static void drawBody(VertexConsumer vertices, Matrix4f matrix, float width,
                                 int seed, float age, int alpha, int light, boolean deferred) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            CrescentPoint a = point(segment / (float) SEGMENTS, width, seed, age);
            CrescentPoint b = point((segment + 1.0F) / SEGMENTS, width, seed, age);
            int color = ((segment + seed) & 3) == 0 ? PURPLE : DARK;
            int localAlpha = Math.round(alpha * Math.min(a.fade, b.fade));
            SoulstalkerRenderGeometry.quad(vertices, matrix,
                    a.outer, b.outer, b.inner, a.inner, color, localAlpha, light, deferred);
            SoulstalkerRenderGeometry.quad(vertices, matrix,
                    a.inner.add(0.0, 0.0, 0.035), b.inner.add(0.0, 0.0, 0.035),
                    b.outer.add(0.0, 0.0, 0.035), a.outer.add(0.0, 0.0, 0.035),
                    color, localAlpha, light, deferred);
        }
    }

    private static void drawRim(VertexConsumer vertices, Matrix4f matrix, float width,
                                int seed, float age, int alpha) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            CrescentPoint a = point(segment / (float) SEGMENTS, width, seed, age);
            CrescentPoint b = point((segment + 1.0F) / SEGMENTS, width, seed, age);
            int localAlpha = Math.round(alpha * Math.min(a.fade, b.fade));
            SoulstalkerRenderGeometry.quad(vertices, matrix,
                    a.rim, b.rim, b.outer, a.outer, GLOW, localAlpha, 0, true);
        }
    }

    private static void drawWisps(VertexConsumer vertices, Matrix4f matrix, float width,
                                  int seed, float age, int color, int alpha,
                                  int light, boolean deferred) {
        for (int index = 0; index < WISPS; index++) {
            float progress = 0.18F + noise(seed + index * 5779L) * 0.64F;
            CrescentPoint anchor = point(progress, width, seed, age);
            float length = width * (0.22F + noise(seed + index * 7717L) * 0.38F);
            float sway = (noise(seed + index * 3313L) - 0.5F) * width * 0.3F
                    + MathHelper.sin(age * 0.3F + index * 1.7F) * width * 0.06F;
            float lift = (noise(seed + index * 2477L) - 0.5F) * width * 0.22F;
            float half = Math.max(0.02F, width * 0.055F);
            Vec3d base = anchor.inner;
            Vec3d middle = new Vec3d(base.x + sway * 0.45, base.y - length * 0.5, base.z + lift * 0.5);
            Vec3d tip = new Vec3d(base.x + sway, base.y - length, base.z + lift);
            wisp(vertices, matrix, base, middle, half, half * 0.55F, color, alpha, light, deferred);
            wisp(vertices, matrix, middle, tip, half * 0.55F, half * 0.12F, color,
                    Math.round(alpha * 0.45F), light, deferred);
        }
    }

    private static void wisp(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                             float startHalf, float endHalf, int color, int alpha,
                             int light, boolean deferred) {
        Vec3d a = start.add(startHalf, 0.0, 0.0);
        Vec3d b = start.add(-startHalf, 0.0, 0.0);
        Vec3d c = end.add(-endHalf, 0.0, 0.0);
        Vec3d d = end.add(endHalf, 0.0, 0.0);
        SoulstalkerRenderGeometry.quad(vertices, matrix, a, b, c, d, color, alpha, light, deferred);
        if (!deferred) {
            SoulstalkerRenderGeometry.quad(vertices, matrix,
                    d.add(0.0, 0.0, 0.02), c.add(0.0, 0.0, 0.02),
                    b.add(0.0, 0.0, 0.02), a.add(0.0, 0.0, 0.02),
                    color, alpha, light, deferred);
        }
    }

    private static CrescentPoint point(float progress, float width, int seed, float age) {
        float angle = MathHelper.lerp(progress, -SWEEP, SWEEP) * MathHelper.RADIANS_PER_DEGREE;
        float taper = (float) Math.pow(Math.max(0.0F, MathHelper.sin(progress * MathHelper.PI)), 0.55);
        float irregular = 0.94F + noise(seed + Math.round(progress * SEGMENTS) * 7919L) * 0.12F;
        float pulse = 1.0F + MathHelper.sin(age * 0.42F + progress * 8.0F) * 0.025F;
        float radiusX = width * 0.9F * irregular * pulse;
        float radiusY = width * 0.42F * irregular * pulse;
        float centerX = MathHelper.sin(angle) * radiusX;
        float centerY = MathHelper.cos(angle) * radiusY - radiusY * 0.12F;
        float thickness = Math.max(0.05F, width * 0.16F * taper);
        float rimWidth = Math.max(0.025F, width * 0.045F) * (0.55F + taper * 0.45F);
        float normalX = MathHelper.sin(angle);
        float normalY = MathHelper.cos(angle);
        float crest = (noise(seed + Math.round(progress * SEGMENTS) * 104729L) - 0.5F)
                * width * 0.18F * taper;
        float depth = MathHelper.sin(progress * MathHelper.PI) * 0.1F;
        Vec3d outer = new Vec3d(centerX + normalX * thickness,
                centerY + normalY * thickness, depth + crest);
        Vec3d rim = new Vec3d(centerX + normalX * (thickness + rimWidth),
                centerY + normalY * (thickness + rimWidth), depth + crest * 1.35F);
        Vec3d inner = new Vec3d(centerX - normalX * thickness * 0.42F,
                centerY - normalY * thickness * 0.42F, depth + 0.02F);
        return new CrescentPoint(outer, inner, rim, MathHelper.clamp(taper * 1.5F, 0.12F, 1.0F));
    }

    private static float noise(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (mixed & 0xFFFFFFL) / (float) 0x1000000;
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private record CrescentPoint(Vec3d outer, Vec3d inner, Vec3d rim, float fade) {
    }
}
