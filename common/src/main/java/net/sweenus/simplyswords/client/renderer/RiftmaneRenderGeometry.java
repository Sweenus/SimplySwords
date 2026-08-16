package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

final class RiftmaneRenderGeometry {

    private RiftmaneRenderGeometry() {
    }

    static float easeOutBack(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float shifted = clamped - 1.0F;
        float overshoot = 1.35F;
        return 1.0F + shifted * shifted * ((overshoot + 1.0F) * shifted + overshoot);
    }

    static float easeOutCubic(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float inverted = 1.0F - clamped;
        return 1.0F - inverted * inverted * inverted;
    }

    static void quad(VertexConsumer vertices, Matrix4f matrix,
                     Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                     int red, int green, int blue, int alpha) {
        quad(vertices, matrix, a, b, c, d, red, green, blue, alpha, alpha, alpha, alpha);
    }

    static void quad(VertexConsumer vertices, Matrix4f matrix,
                     Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                     int red, int green, int blue,
                     int alphaA, int alphaB, int alphaC, int alphaD) {
        if (vertices == null || (alphaA <= 0 && alphaB <= 0 && alphaC <= 0 && alphaD <= 0)) {
            return;
        }
        vertex(vertices, matrix, a, red, green, blue, alphaA);
        vertex(vertices, matrix, b, red, green, blue, alphaB);
        vertex(vertices, matrix, c, red, green, blue, alphaC);
        vertex(vertices, matrix, d, red, green, blue, alphaD);
        vertex(vertices, matrix, d, red, green, blue, alphaD);
        vertex(vertices, matrix, c, red, green, blue, alphaC);
        vertex(vertices, matrix, b, red, green, blue, alphaB);
        vertex(vertices, matrix, a, red, green, blue, alphaA);
    }

    static void lensFill(VertexConsumer vertices, Matrix4f matrix, int segments,
                         double centerY, double halfWidth, double halfHeight,
                         int red, int green, int blue, int centerAlpha, int edgeAlpha) {
        if (vertices == null || halfWidth <= 0.0 || halfHeight <= 0.0) {
            return;
        }
        Vec3d center = new Vec3d(0.0, centerY, 0.0);
        for (int segment = 0; segment < segments; segment++) {
            Vec3d first = lensPoint(segment, segments, centerY, halfWidth, halfHeight);
            Vec3d second = lensPoint(segment + 1, segments, centerY, halfWidth, halfHeight);
            quad(vertices, matrix, center, center, first, second,
                    red, green, blue, centerAlpha, centerAlpha, edgeAlpha, edgeAlpha);
        }
    }

    static void lensRim(VertexConsumer vertices, Matrix4f matrix, int segments,
                        double centerY, double halfWidth, double halfHeight,
                        double thickness, float wobble, float wobblePhase,
                        int red, int green, int blue, int innerAlpha, int outerAlpha) {
        if (vertices == null || halfWidth <= 0.0 || halfHeight <= 0.0 || thickness <= 0.0) {
            return;
        }
        for (int segment = 0; segment < segments; segment++) {
            double firstScale = rimScale(segment, segments, thickness, wobble, wobblePhase);
            double secondScale = rimScale(segment + 1, segments, thickness, wobble, wobblePhase);
            Vec3d innerFirst = lensPoint(segment, segments, centerY, halfWidth, halfHeight);
            Vec3d innerSecond = lensPoint(segment + 1, segments, centerY, halfWidth, halfHeight);
            Vec3d outerFirst = lensPoint(segment, segments, centerY,
                    halfWidth + firstScale, halfHeight + firstScale);
            Vec3d outerSecond = lensPoint(segment + 1, segments, centerY,
                    halfWidth + secondScale, halfHeight + secondScale);
            quad(vertices, matrix, innerFirst, innerSecond, outerSecond, outerFirst,
                    red, green, blue, innerAlpha, innerAlpha, outerAlpha, outerAlpha);
        }
    }

    static void groundRing(VertexConsumer vertices, Matrix4f matrix, int segments,
                           double y, double radius, double width,
                           int red, int green, int blue, int alpha) {
        if (vertices == null || radius <= 0.0 || alpha <= 0) {
            return;
        }
        double inner = Math.max(0.01, radius - width);
        for (int segment = 0; segment < segments; segment++) {
            double start = MathHelper.TAU * segment / segments;
            double end = MathHelper.TAU * (segment + 1) / segments;
            Vec3d outerStart = new Vec3d(Math.cos(start) * radius, y, Math.sin(start) * radius);
            Vec3d outerEnd = new Vec3d(Math.cos(end) * radius, y, Math.sin(end) * radius);
            Vec3d innerEnd = new Vec3d(Math.cos(end) * inner, y, Math.sin(end) * inner);
            Vec3d innerStart = new Vec3d(Math.cos(start) * inner, y, Math.sin(start) * inner);
            quad(vertices, matrix, outerStart, outerEnd, innerEnd, innerStart,
                    red, green, blue, alpha, alpha, alpha, alpha);
        }
    }

    static void taperedRibbon(VertexConsumer vertices, Matrix4f matrix,
                              Vec3d[] path, Vec3d side, double startWidth, double endWidth,
                              int red, int green, int blue, int startAlpha, int endAlpha) {
        if (vertices == null || path.length < 2) {
            return;
        }
        int spans = path.length - 1;
        for (int index = 0; index < spans; index++) {
            float from = (float) index / spans;
            float to = (float) (index + 1) / spans;
            double widthFrom = MathHelper.lerp(from, (float) startWidth, (float) endWidth);
            double widthTo = MathHelper.lerp(to, (float) startWidth, (float) endWidth);
            int alphaFrom = Math.round(MathHelper.lerp(from, startAlpha, endAlpha));
            int alphaTo = Math.round(MathHelper.lerp(to, startAlpha, endAlpha));
            Vec3d offsetFrom = side.multiply(widthFrom);
            Vec3d offsetTo = side.multiply(widthTo);
            quad(vertices, matrix,
                    path[index].add(offsetFrom), path[index].subtract(offsetFrom),
                    path[index + 1].subtract(offsetTo), path[index + 1].add(offsetTo),
                    red, green, blue, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }
    }

    static void billboardLens(VertexConsumer vertices, Matrix4f matrix,
                              float halfWidth, float halfHeight,
                              int red, int green, int blue, int alpha) {
        if (vertices == null || alpha <= 0) {
            return;
        }
        quad(vertices, matrix,
                new Vec3d(0.0, halfHeight, 0.0),
                new Vec3d(halfWidth, 0.0, 0.0),
                new Vec3d(0.0, -halfHeight, 0.0),
                new Vec3d(-halfWidth, 0.0, 0.0),
                red, green, blue, alpha);
    }

    private static double rimScale(int segment, int segments, double thickness,
                                   float wobble, float wobblePhase) {
        float angle = MathHelper.TAU * segment / segments;
        float ripple = MathHelper.sin(angle * 3.0F + wobblePhase) * 0.5F
                + MathHelper.sin(angle * 7.0F - wobblePhase * 0.6F) * 0.5F;
        return thickness * (1.0 + ripple * wobble);
    }

    private static Vec3d lensPoint(int segment, int segments,
                                   double centerY, double halfWidth, double halfHeight) {
        double angle = MathHelper.TAU * segment / segments;
        return new Vec3d(Math.cos(angle) * halfWidth, centerY + Math.sin(angle) * halfHeight, 0.0);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0.0F, 1.0F, 0.0F)
                .next();
    }
}
