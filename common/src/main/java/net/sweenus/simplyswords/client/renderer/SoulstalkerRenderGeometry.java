package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

final class SoulstalkerRenderGeometry {
    private SoulstalkerRenderGeometry() {
    }

    static void prism(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                      float halfWidth, int color, int alpha, int light, boolean deferred) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-6) {
            return;
        }
        Vec3d forward = direction.normalize();
        Vec3d reference = Math.abs(forward.y) > 0.88
                ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
        Vec3d side = forward.crossProduct(reference).normalize().multiply(halfWidth);
        Vec3d up = forward.crossProduct(side.normalize()).normalize().multiply(halfWidth);
        Vec3d a = start.add(side).add(up);
        Vec3d b = start.subtract(side).add(up);
        Vec3d c = start.subtract(side).subtract(up);
        Vec3d d = start.add(side).subtract(up);
        Vec3d e = end.add(side).add(up);
        Vec3d f = end.subtract(side).add(up);
        Vec3d g = end.subtract(side).subtract(up);
        Vec3d h = end.add(side).subtract(up);
        quad(vertices, matrix, a, b, f, e, color, alpha, light, deferred);
        quad(vertices, matrix, b, c, g, f, color, alpha, light, deferred);
        quad(vertices, matrix, c, d, h, g, color, alpha, light, deferred);
        quad(vertices, matrix, d, a, e, h, color, alpha, light, deferred);
        quad(vertices, matrix, a, d, c, b, color, alpha, light, deferred);
        quad(vertices, matrix, e, f, g, h, color, alpha, light, deferred);
    }

    static void crossRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                            float halfWidth, int color, int alpha) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-6) {
            return;
        }
        Vec3d normalized = direction.normalize();
        Vec3d side = new Vec3d(-normalized.z, 0.0, normalized.x);
        if (side.lengthSquared() < 1.0E-6) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = normalized.crossProduct(side).normalize();
        ribbon(vertices, matrix, start, end, side.multiply(halfWidth), color, alpha);
        ribbon(vertices, matrix, start, end, up.multiply(halfWidth), color, alpha);
    }

    static void quad(VertexConsumer vertices, Matrix4f matrix,
                     Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                     int color, int alpha, int light, boolean deferred) {
        vertex(vertices, matrix, a, color, alpha, light, deferred);
        vertex(vertices, matrix, b, color, alpha, light, deferred);
        vertex(vertices, matrix, c, color, alpha, light, deferred);
        vertex(vertices, matrix, d, color, alpha, light, deferred);
    }

    private static void ribbon(VertexConsumer vertices, Matrix4f matrix,
                               Vec3d start, Vec3d end, Vec3d width,
                               int color, int alpha) {
        quad(vertices, matrix, start.add(width), start.subtract(width),
                end.subtract(width), end.add(width), color, alpha, 0, true);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d point,
                               int color, int alpha, int light, boolean deferred) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        var vertex = vertices.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha);
        if (!deferred) {
            vertex.light(light);
        }
    }
}
