package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

final class DawnquiverRenderGeometry {

    private DawnquiverRenderGeometry() {
    }

    static float easeOutCubic(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float inverted = 1.0F - clamped;
        return 1.0F - inverted * inverted * inverted;
    }

    static float easeOutBack(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float shifted = clamped - 1.0F;
        float overshoot = 1.35F;
        return 1.0F + shifted * shifted * ((overshoot + 1.0F) * shifted + overshoot);
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

    static void planarRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3d[] path,
                             double startWidth, double endWidth,
                             int startRed, int startGreen, int startBlue,
                             int endRed, int endGreen, int endBlue,
                             int startAlpha, int endAlpha) {
        if (vertices == null || path.length < 2) {
            return;
        }
        int spans = path.length - 1;
        for (int index = 0; index < spans; index++) {
            float from = (float) index / spans;
            float to = (float) (index + 1) / spans;
            Vec3d sideFrom = planarNormal(path, index);
            Vec3d sideTo = planarNormal(path, index + 1);
            Vec3d offsetFrom = sideFrom.multiply(MathHelper.lerp(from, (float) startWidth, (float) endWidth));
            Vec3d offsetTo = sideTo.multiply(MathHelper.lerp(to, (float) startWidth, (float) endWidth));
            int alphaFrom = Math.round(MathHelper.lerp(from, startAlpha, endAlpha));
            int alphaTo = Math.round(MathHelper.lerp(to, startAlpha, endAlpha));
            int redFrom = Math.round(MathHelper.lerp(from, startRed, endRed));
            int greenFrom = Math.round(MathHelper.lerp(from, startGreen, endGreen));
            int blueFrom = Math.round(MathHelper.lerp(from, startBlue, endBlue));
            quad(vertices, matrix,
                    path[index].add(offsetFrom), path[index].subtract(offsetFrom),
                    path[index + 1].subtract(offsetTo), path[index + 1].add(offsetTo),
                    redFrom, greenFrom, blueFrom, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }
    }

    static void planarRibbonXZ(VertexConsumer vertices, Matrix4f matrix, Vec3d[] path,
                               double startWidth, double endWidth,
                               int startRed, int startGreen, int startBlue,
                               int endRed, int endGreen, int endBlue,
                               int startAlpha, int endAlpha) {
        if (vertices == null || path.length < 2) {
            return;
        }
        int spans = path.length - 1;
        for (int index = 0; index < spans; index++) {
            float from = (float) index / spans;
            float to = (float) (index + 1) / spans;
            Vec3d sideFrom = planarNormalXZ(path, index);
            Vec3d sideTo = planarNormalXZ(path, index + 1);
            Vec3d offsetFrom = sideFrom.multiply(MathHelper.lerp(from, (float) startWidth, (float) endWidth));
            Vec3d offsetTo = sideTo.multiply(MathHelper.lerp(to, (float) startWidth, (float) endWidth));
            int alphaFrom = Math.round(MathHelper.lerp(from, startAlpha, endAlpha));
            int alphaTo = Math.round(MathHelper.lerp(to, startAlpha, endAlpha));
            int redFrom = Math.round(MathHelper.lerp(from, startRed, endRed));
            int greenFrom = Math.round(MathHelper.lerp(from, startGreen, endGreen));
            int blueFrom = Math.round(MathHelper.lerp(from, startBlue, endBlue));
            quad(vertices, matrix,
                    path[index].add(offsetFrom), path[index].subtract(offsetFrom),
                    path[index + 1].subtract(offsetTo), path[index + 1].add(offsetTo),
                    redFrom, greenFrom, blueFrom, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }
    }

    static void profiledRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3d[] path, double[] widths,
                               int startRed, int startGreen, int startBlue,
                               int endRed, int endGreen, int endBlue,
                               int startAlpha, int endAlpha) {
        if (vertices == null || path.length < 2 || widths.length != path.length) {
            return;
        }
        int spans = path.length - 1;
        for (int index = 0; index < spans; index++) {
            float from = (float) index / spans;
            float to = (float) (index + 1) / spans;
            Vec3d offsetFrom = planarNormal(path, index).multiply(widths[index]);
            Vec3d offsetTo = planarNormal(path, index + 1).multiply(widths[index + 1]);
            int alphaFrom = Math.round(MathHelper.lerp(from, startAlpha, endAlpha));
            int alphaTo = Math.round(MathHelper.lerp(to, startAlpha, endAlpha));
            int red = Math.round(MathHelper.lerp(from, startRed, endRed));
            int green = Math.round(MathHelper.lerp(from, startGreen, endGreen));
            int blue = Math.round(MathHelper.lerp(from, startBlue, endBlue));
            quad(vertices, matrix,
                    path[index].add(offsetFrom), path[index].subtract(offsetFrom),
                    path[index + 1].subtract(offsetTo), path[index + 1].add(offsetTo),
                    red, green, blue, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }
    }

    static void profiledRibbonXZ(VertexConsumer vertices, Matrix4f matrix, Vec3d[] path, double[] widths,
                                 int startRed, int startGreen, int startBlue,
                                 int endRed, int endGreen, int endBlue,
                                 int startAlpha, int endAlpha) {
        profiledRibbonWithNormals(vertices, matrix, path, widths, true,
                startRed, startGreen, startBlue, endRed, endGreen, endBlue, startAlpha, endAlpha);
    }

    static void profiledRibbonWithSide(VertexConsumer vertices, Matrix4f matrix,
                                       Vec3d[] path, double[] widths, Vec3d side,
                                       int startRed, int startGreen, int startBlue,
                                       int endRed, int endGreen, int endBlue,
                                       int startAlpha, int endAlpha) {
        if (vertices == null || path.length < 2 || widths.length != path.length
                || side.lengthSquared() < 1.0E-8) {
            return;
        }
        Vec3d normal = side.normalize();
        int spans = path.length - 1;
        for (int index = 0; index < spans; index++) {
            float from = (float) index / spans;
            float to = (float) (index + 1) / spans;
            Vec3d offsetFrom = normal.multiply(widths[index]);
            Vec3d offsetTo = normal.multiply(widths[index + 1]);
            int alphaFrom = Math.round(MathHelper.lerp(from, startAlpha, endAlpha));
            int alphaTo = Math.round(MathHelper.lerp(to, startAlpha, endAlpha));
            int red = Math.round(MathHelper.lerp(from, startRed, endRed));
            int green = Math.round(MathHelper.lerp(from, startGreen, endGreen));
            int blue = Math.round(MathHelper.lerp(from, startBlue, endBlue));
            quad(vertices, matrix,
                    path[index].add(offsetFrom), path[index].subtract(offsetFrom),
                    path[index + 1].subtract(offsetTo), path[index + 1].add(offsetTo),
                    red, green, blue, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }
    }

    private static void profiledRibbonWithNormals(VertexConsumer vertices, Matrix4f matrix,
                                                  Vec3d[] path, double[] widths, boolean xzPlane,
                                                  int startRed, int startGreen, int startBlue,
                                                  int endRed, int endGreen, int endBlue,
                                                  int startAlpha, int endAlpha) {
        if (vertices == null || path.length < 2 || widths.length != path.length) {
            return;
        }
        int spans = path.length - 1;
        for (int index = 0; index < spans; index++) {
            float from = (float) index / spans;
            float to = (float) (index + 1) / spans;
            Vec3d normalFrom = xzPlane ? planarNormalXZ(path, index) : planarNormal(path, index);
            Vec3d normalTo = xzPlane ? planarNormalXZ(path, index + 1) : planarNormal(path, index + 1);
            Vec3d offsetFrom = normalFrom.multiply(widths[index]);
            Vec3d offsetTo = normalTo.multiply(widths[index + 1]);
            int alphaFrom = Math.round(MathHelper.lerp(from, startAlpha, endAlpha));
            int alphaTo = Math.round(MathHelper.lerp(to, startAlpha, endAlpha));
            int red = Math.round(MathHelper.lerp(from, startRed, endRed));
            int green = Math.round(MathHelper.lerp(from, startGreen, endGreen));
            int blue = Math.round(MathHelper.lerp(from, startBlue, endBlue));
            quad(vertices, matrix,
                    path[index].add(offsetFrom), path[index].subtract(offsetFrom),
                    path[index + 1].subtract(offsetTo), path[index + 1].add(offsetTo),
                    red, green, blue, alphaFrom, alphaFrom, alphaTo, alphaTo);
        }
    }

    static void chevron(VertexConsumer vertices, Matrix4f matrix, Vec3d tip, Vec3d direction,
                        double length, double halfSpan, int red, int green, int blue, int alpha) {
        if (vertices == null || alpha <= 0 || direction.lengthSquared() < 1.0E-8) {
            return;
        }
        Vec3d forward = direction.normalize();
        Vec3d side = new Vec3d(-forward.y, forward.x, 0.0);
        Vec3d back = tip.subtract(forward.multiply(length));
        Vec3d left = back.add(side.multiply(halfSpan));
        Vec3d right = back.subtract(side.multiply(halfSpan));
        Vec3d notch = tip.subtract(forward.multiply(length * 0.45));
        quad(vertices, matrix, tip, left, notch, notch, red, green, blue, alpha, 0, alpha, alpha);
        quad(vertices, matrix, tip, right, notch, notch, red, green, blue, alpha, 0, alpha, alpha);
    }

    static void chevronXZ(VertexConsumer vertices, Matrix4f matrix, Vec3d tip, Vec3d direction,
                          double length, double halfSpan, int red, int green, int blue, int alpha) {
        if (direction.lengthSquared() < 1.0E-8) {
            return;
        }
        Vec3d forward = direction.normalize();
        chevronWithSide(vertices, matrix, tip, forward,
                new Vec3d(-forward.z, 0.0, forward.x),
                length, halfSpan, red, green, blue, alpha);
    }

    static void chevronWithSide(VertexConsumer vertices, Matrix4f matrix,
                                Vec3d tip, Vec3d direction, Vec3d side,
                                double length, double halfSpan,
                                int red, int green, int blue, int alpha) {
        if (vertices == null || alpha <= 0 || direction.lengthSquared() < 1.0E-8
                || side.lengthSquared() < 1.0E-8) {
            return;
        }
        Vec3d forward = direction.normalize();
        Vec3d lateral = side.normalize();
        Vec3d back = tip.subtract(forward.multiply(length));
        Vec3d left = back.add(lateral.multiply(halfSpan));
        Vec3d right = back.subtract(lateral.multiply(halfSpan));
        Vec3d notch = tip.subtract(forward.multiply(length * 0.45));
        quad(vertices, matrix, tip, left, notch, notch, red, green, blue, alpha, 0, alpha, alpha);
        quad(vertices, matrix, tip, right, notch, notch, red, green, blue, alpha, 0, alpha, alpha);
    }

    private static Vec3d planarNormal(Vec3d[] path, int index) {
        Vec3d before = path[Math.max(0, index - 1)];
        Vec3d after = path[Math.min(path.length - 1, index + 1)];
        Vec3d direction = after.subtract(before);
        if (direction.lengthSquared() < 1.0E-8) {
            return new Vec3d(0.0, 1.0, 0.0);
        }
        Vec3d normal = new Vec3d(-direction.y, direction.x, 0.0);
        return normal.lengthSquared() < 1.0E-8 ? new Vec3d(1.0, 0.0, 0.0) : normal.normalize();
    }

    private static Vec3d planarNormalXZ(Vec3d[] path, int index) {
        Vec3d before = path[Math.max(0, index - 1)];
        Vec3d after = path[Math.min(path.length - 1, index + 1)];
        Vec3d direction = after.subtract(before);
        if (direction.lengthSquared() < 1.0E-8) {
            return new Vec3d(0.0, 0.0, 1.0);
        }
        Vec3d normal = new Vec3d(-direction.z, 0.0, direction.x);
        return normal.lengthSquared() < 1.0E-8 ? new Vec3d(0.0, 0.0, 1.0) : normal.normalize();
    }

    static void rays(VertexConsumer vertices, Matrix4f matrix, Vec3d center, int count,
                     double innerRadius, double outerRadius, double halfWidth, float rotation,
                     int red, int green, int blue, int innerAlpha, int outerAlpha) {
        if (vertices == null || outerRadius <= innerRadius || innerAlpha <= 0) {
            return;
        }
        for (int index = 0; index < count; index++) {
            double angle = MathHelper.TAU * index / count + rotation;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3d inner = center.add(cos * innerRadius, sin * innerRadius, 0.0);
            Vec3d outer = center.add(cos * outerRadius, sin * outerRadius, 0.0);
            Vec3d side = new Vec3d(-sin, cos, 0.0);
            quad(vertices, matrix,
                    inner.add(side.multiply(halfWidth)), inner.subtract(side.multiply(halfWidth)),
                    outer.subtract(side.multiply(halfWidth * 0.15)), outer.add(side.multiply(halfWidth * 0.15)),
                    red, green, blue, innerAlpha, innerAlpha, outerAlpha, outerAlpha);
        }
    }

    static void flatRing(VertexConsumer vertices, Matrix4f matrix, int segments, Vec3d center,
                         double radius, double width, int red, int green, int blue, int alpha) {
        if (vertices == null || radius <= 0.0 || alpha <= 0) {
            return;
        }
        double inner = Math.max(0.01, radius - width);
        for (int segment = 0; segment < segments; segment++) {
            double start = MathHelper.TAU * segment / segments;
            double end = MathHelper.TAU * (segment + 1) / segments;
            Vec3d outerStart = center.add(Math.cos(start) * radius, Math.sin(start) * radius, 0.0);
            Vec3d outerEnd = center.add(Math.cos(end) * radius, Math.sin(end) * radius, 0.0);
            Vec3d innerEnd = center.add(Math.cos(end) * inner, Math.sin(end) * inner, 0.0);
            Vec3d innerStart = center.add(Math.cos(start) * inner, Math.sin(start) * inner, 0.0);
            quad(vertices, matrix, outerStart, outerEnd, innerEnd, innerStart, red, green, blue, alpha);
        }
    }

    static void line(VertexConsumer vertices, Matrix4f matrix, Vec3d from, Vec3d to,
                     Vec3d side, double halfWidth, int red, int green, int blue, int alpha) {
        Vec3d offset = side.multiply(halfWidth);
        quad(vertices, matrix,
                from.add(offset), from.subtract(offset),
                to.subtract(offset), to.add(offset),
                red, green, blue, alpha);
    }

    static void billboardLens(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                              float halfWidth, float halfHeight,
                              int red, int green, int blue, int alpha) {
        if (vertices == null || alpha <= 0) {
            return;
        }
        quad(vertices, matrix,
                center.add(0.0, halfHeight, 0.0),
                center.add(halfWidth, 0.0, 0.0),
                center.add(0.0, -halfHeight, 0.0),
                center.add(-halfWidth, 0.0, 0.0),
                red, green, blue, alpha);
    }

    static void ring(VertexConsumer vertices, Matrix4f matrix, int segments,
                     Vec3d center, double radius, double width,
                     int red, int green, int blue, int alpha) {
        if (vertices == null || radius <= 0.0 || alpha <= 0) {
            return;
        }
        double inner = Math.max(0.01, radius - width);
        for (int segment = 0; segment < segments; segment++) {
            double start = MathHelper.TAU * segment / segments;
            double end = MathHelper.TAU * (segment + 1) / segments;
            Vec3d outerStart = center.add(Math.cos(start) * radius, 0.0, Math.sin(start) * radius);
            Vec3d outerEnd = center.add(Math.cos(end) * radius, 0.0, Math.sin(end) * radius);
            Vec3d innerEnd = center.add(Math.cos(end) * inner, 0.0, Math.sin(end) * inner);
            Vec3d innerStart = center.add(Math.cos(start) * inner, 0.0, Math.sin(start) * inner);
            quad(vertices, matrix, outerStart, outerEnd, innerEnd, innerStart, red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, MathHelper.clamp(alpha, 0, 255)).next();
    }
}
