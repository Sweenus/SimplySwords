package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.StarsEdgeConstellationVisualEntity;
import org.joml.Matrix4f;

public class StarsEdgeConstellationVisualEntityRenderer
        extends EntityRenderer<StarsEdgeConstellationVisualEntity> {

    private static final int FULL_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private static final int RING_SEGMENTS = 36;

    public StarsEdgeConstellationVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(StarsEdgeConstellationVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(StarsEdgeConstellationVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) {
            return true;
        }
        Vec3d start = entity.getPos();
        Vec3d end = start.add(entity.getEndOffset());
        return frustum.isVisible(new Box(start, end).expand(2.5));
    }

    @Override
    public void render(StarsEdgeConstellationVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float phaseAge = Math.max(0.0F, age - entity.getPhaseStartAge());
        float alpha = getPhaseAlpha(entity, phaseAge);
        if (alpha <= 0.01F) {
            return;
        }

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        matrices.push();
        if (entity.getKind() == StarsEdgeConstellationVisualEntity.KIND_LINK) {
            renderLink(entity, vertices, matrices.peek().getPositionMatrix(), age, phaseAge, alpha);
        } else {
            renderNode(entity, vertices, matrices, age, phaseAge, alpha);
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void renderNode(StarsEdgeConstellationVisualEntity entity, VertexConsumer vertices,
                                   MatrixStack matrices, float age, float phaseAge, float alpha) {
        float grow = easeOut(MathHelper.clamp(age / 7.0F, 0.0F, 1.0F));
        float detonationProgress = MathHelper.clamp(phaseAge / 5.0F, 0.0F, 1.0F);
        float phasePulse = entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_DETONATE
                ? (1.0F + 1.15F * MathHelper.sin(detonationProgress * MathHelper.PI))
                * (1.0F - detonationProgress * 0.35F)
                : entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_ARMED
                ? 1.0F + 0.12F * MathHelper.sin(phaseAge * 0.38F)
                : 1.0F + 0.06F * MathHelper.sin(age * 0.22F);
        float scale = grow * phasePulse;

        matrices.translate(0.0, 0.88 + MathHelper.sin(age * 0.10F) * 0.06F, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 2.8F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 1.35F));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        drawDiamondXY(vertices, matrix, Vec3d.ZERO, 0.42F * scale, 0.20F * scale,
                104, 35, 224, alpha(118, alpha), FULL_LIGHT);
        drawDiamondZY(vertices, matrix, Vec3d.ZERO, 0.42F * scale, 0.20F * scale,
                104, 35, 224, alpha(105, alpha), FULL_LIGHT);
        drawDiamondXY(vertices, matrix, Vec3d.ZERO, 0.29F * scale, 0.125F * scale,
                65, 230, 255, alpha(220, alpha), FULL_LIGHT);
        drawDiamondZY(vertices, matrix, Vec3d.ZERO, 0.29F * scale, 0.125F * scale,
                65, 230, 255, alpha(205, alpha), FULL_LIGHT);
        drawDiamondXY(vertices, matrix, Vec3d.ZERO, 0.13F * scale, 0.07F * scale,
                246, 250, 255, alpha(255, alpha), FULL_LIGHT);
        drawDiamondZY(vertices, matrix, Vec3d.ZERO, 0.13F * scale, 0.07F * scale,
                246, 250, 255, alpha(245, alpha), FULL_LIGHT);

        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-age * 3.4F));
        drawRing(vertices, matrices.peek().getPositionMatrix(), 0.52F * grow,
                0.018F, 81, 208, 255, alpha(150, alpha), FULL_LIGHT);
    }

    private static void renderLink(StarsEdgeConstellationVisualEntity entity, VertexConsumer vertices,
                                   Matrix4f matrix, float age, float phaseAge, float alpha) {
        Vec3d start = new Vec3d(0.0, 0.88, 0.0);
        Vec3d offset = entity.getEndOffset();
        float grow = easeOut(MathHelper.clamp(age / 8.0F, 0.0F, 1.0F));
        Vec3d end = start.add(offset.multiply(grow));
        if (start.squaredDistanceTo(end) < 0.0001) {
            return;
        }

        float detonationProgress = MathHelper.clamp(phaseAge / 5.0F, 0.0F, 1.0F);
        float pulse = entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_DETONATE
                ? 1.0F + 2.1F * MathHelper.sin(detonationProgress * MathHelper.PI)
                : entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_ARMED
                ? 1.0F + 0.16F * MathHelper.sin(phaseAge * 0.45F)
                : 1.0F;
        drawCrossRibbon(vertices, matrix, start, end, 0.105F * pulse,
                91, 30, 205, alpha(105, alpha), FULL_LIGHT);
        drawCrossRibbon(vertices, matrix, start, end, 0.048F * pulse,
                63, 222, 255, alpha(205, alpha), FULL_LIGHT);
        drawCrossRibbon(vertices, matrix, start, end, 0.017F * pulse,
                244, 249, 255, alpha(245, alpha), FULL_LIGHT);

        int moteCount = Math.max(1, Math.min(4, (int) Math.ceil(offset.length() / 2.0)));
        for (int i = 0; i < moteCount; i++) {
            double progress = positiveModulo(age * 0.045F + i / (double) moteCount, 1.0);
            if (entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_ARMED) {
                progress = positiveModulo(phaseAge * 0.075F + i / (double) moteCount, 1.0);
            }
            Vec3d mote = start.lerp(end, progress);
            float motePulse = 0.07F + 0.025F * MathHelper.sin(age * 0.4F + i * 1.7F);
            drawTinyStar(vertices, matrix, mote, motePulse,
                    226, 245, 255, alpha(235, alpha), FULL_LIGHT);
        }
    }

    private static float getPhaseAlpha(StarsEdgeConstellationVisualEntity entity, float phaseAge) {
        if (entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_DETONATE) {
            float progress = MathHelper.clamp(phaseAge / 5.0F, 0.0F, 1.0F);
            return 1.0F - progress * progress;
        }
        if (entity.getPhase() == StarsEdgeConstellationVisualEntity.PHASE_FADE) {
            return MathHelper.clamp(1.0F - phaseAge / 10.0F, 0.0F, 1.0F);
        }
        return MathHelper.clamp((entity.getLifetime() - entity.age) / 10.0F, 0.0F, 1.0F);
    }

    private static void drawCrossRibbon(VertexConsumer vertices, Matrix4f matrix,
                                        Vec3d start, Vec3d end, float halfWidth,
                                        int red, int green, int blue, int alpha, int light) {
        Vec3d direction = end.subtract(start).normalize();
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 0.0001) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = direction.crossProduct(side).normalize();
        drawRibbon(vertices, matrix, start, end, side.multiply(halfWidth), red, green, blue, alpha, light);
        drawRibbon(vertices, matrix, start, end, up.multiply(halfWidth), red, green, blue, alpha, light);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix,
                                   Vec3d start, Vec3d end, Vec3d width,
                                   int red, int green, int blue, int alpha, int light) {
        quad(vertices, matrix,
                start.add(width), start.subtract(width), end.subtract(width), end.add(width),
                red, green, blue, alpha, light);
    }

    private static void drawTinyStar(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                                     float radius, int red, int green, int blue, int alpha, int light) {
        drawDiamondXY(vertices, matrix, center, radius * 1.5F, radius,
                red, green, blue, alpha, light);
        drawDiamondZY(vertices, matrix, center, radius * 1.5F, radius,
                red, green, blue, alpha, light);
    }

    private static void drawDiamondXY(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                                      float verticalRadius, float horizontalRadius,
                                      int red, int green, int blue, int alpha, int light) {
        quad(vertices, matrix,
                center.add(0.0, verticalRadius, 0.0),
                center.add(horizontalRadius, 0.0, 0.0),
                center.add(0.0, -verticalRadius, 0.0),
                center.add(-horizontalRadius, 0.0, 0.0),
                red, green, blue, alpha, light);
    }

    private static void drawDiamondZY(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                                      float verticalRadius, float horizontalRadius,
                                      int red, int green, int blue, int alpha, int light) {
        quad(vertices, matrix,
                center.add(0.0, verticalRadius, 0.0),
                center.add(0.0, 0.0, horizontalRadius),
                center.add(0.0, -verticalRadius, 0.0),
                center.add(0.0, 0.0, -horizontalRadius),
                red, green, blue, alpha, light);
    }

    private static void drawRing(VertexConsumer vertices, Matrix4f matrix, float radius,
                                 float halfThickness, int red, int green, int blue,
                                 int alpha, int light) {
        float inner = Math.max(0.0F, radius - halfThickness);
        float outer = radius + halfThickness;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double first = MathHelper.TAU * i / RING_SEGMENTS;
            double second = MathHelper.TAU * (i + 1) / RING_SEGMENTS;
            quad(vertices, matrix,
                    new Vec3d(Math.cos(first) * inner, 0.0, Math.sin(first) * inner),
                    new Vec3d(Math.cos(first) * outer, 0.0, Math.sin(first) * outer),
                    new Vec3d(Math.cos(second) * outer, 0.0, Math.sin(second) * outer),
                    new Vec3d(Math.cos(second) * inner, 0.0, Math.sin(second) * inner),
                    red, green, blue, alpha, light);
        }
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             Vec3d first, Vec3d second, Vec3d third, Vec3d fourth,
                             int red, int green, int blue, int alpha, int light) {
        vertex(vertices, matrix, first, red, green, blue, alpha, light);
        vertex(vertices, matrix, second, red, green, blue, alpha, light);
        vertex(vertices, matrix, third, red, green, blue, alpha, light);
        vertex(vertices, matrix, fourth, red, green, blue, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                               int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .light(light).next();
    }

    private static int alpha(int base, float multiplier) {
        return MathHelper.clamp(Math.round(base * multiplier), 0, 255);
    }

    private static float easeOut(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0 ? result + modulus : result;
    }
}
