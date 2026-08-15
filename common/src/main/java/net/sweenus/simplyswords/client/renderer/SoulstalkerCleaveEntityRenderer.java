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
    private static final Identifier WHITE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int SEGMENTS = 20;
    private static final int DARK = 0x160824;
    private static final int PURPLE = 0x351052;
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
        drawCrescent(dark, matrix, width, entity.getSeed(), age,
                DARK, PURPLE, alpha, packedLight, false);
        VertexConsumer glow = IonDeferredRenderController.getLightningBuffer();
        drawCrescent(glow, matrix, width * 1.015F, entity.getSeed(), age,
                GLOW, GLOW, Math.round(alpha * 0.82F), 0, true);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static void drawCrescent(VertexConsumer vertices, Matrix4f matrix,
                                     float width, int seed, float age,
                                     int firstColor, int secondColor,
                                     int alpha, int light, boolean deferred) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float startProgress = segment / (float) SEGMENTS;
            float endProgress = (segment + 1.0F) / SEGMENTS;
            CrescentPoint a = point(startProgress, width, seed, age, deferred);
            CrescentPoint b = point(endProgress, width, seed, age, deferred);
            int color = ((segment + seed) & 3) == 0 ? secondColor : firstColor;
            int localAlpha = Math.round(alpha * Math.min(a.fade, b.fade));
            SoulstalkerRenderGeometry.quad(vertices, matrix,
                    a.outer, b.outer, b.inner, a.inner,
                    color, localAlpha, light, deferred);
            SoulstalkerRenderGeometry.quad(vertices, matrix,
                    a.inner.add(0.0, 0.0, 0.035), b.inner.add(0.0, 0.0, 0.035),
                    b.outer.add(0.0, 0.0, 0.035), a.outer.add(0.0, 0.0, 0.035),
                    color, localAlpha, light, deferred);
        }
    }

    private static CrescentPoint point(float progress, float width, int seed,
                                       float age, boolean glow) {
        float angle = MathHelper.lerp(progress, -142.0F, 142.0F) * MathHelper.RADIANS_PER_DEGREE;
        float taper = (float) Math.pow(Math.max(0.0F, MathHelper.sin(progress * MathHelper.PI)), 0.38);
        float irregular = 0.94F + noise(seed + Math.round(progress * SEGMENTS) * 7919L) * 0.12F;
        float pulse = 1.0F + MathHelper.sin(age * 0.42F + progress * 8.0F) * 0.025F;
        float radiusX = width * 0.7F * irregular * pulse;
        float radiusY = width * 0.88F * irregular * pulse;
        float centerX = MathHelper.sin(angle) * radiusX;
        float centerY = MathHelper.cos(angle) * radiusY - radiusY * 0.12F;
        float thickness = Math.max(glow ? 0.018F : 0.06F,
                width * (glow ? 0.045F : 0.19F) * taper);
        float normalX = MathHelper.sin(angle);
        float normalY = MathHelper.cos(angle);
        float depth = MathHelper.sin(progress * MathHelper.PI) * (glow ? 0.06F : 0.12F);
        Vec3d outer = new Vec3d(centerX + normalX * thickness,
                centerY + normalY * thickness, depth);
        Vec3d inner = new Vec3d(centerX - normalX * thickness * (glow ? 0.1F : 0.42F),
                centerY - normalY * thickness * (glow ? 0.1F : 0.42F), depth + 0.02F);
        return new CrescentPoint(outer, inner, MathHelper.clamp(taper * 1.5F, 0.12F, 1.0F));
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

    private record CrescentPoint(Vec3d outer, Vec3d inner, float fade) {
    }
}
