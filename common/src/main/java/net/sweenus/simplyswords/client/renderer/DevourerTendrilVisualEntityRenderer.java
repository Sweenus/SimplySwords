package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.DevourerMassVisualEntity;
import net.sweenus.simplyswords.entity.DevourerTendrilVisualEntity;
import org.joml.Matrix4f;

public final class DevourerTendrilVisualEntityRenderer extends EntityRenderer<DevourerTendrilVisualEntity> {
    private static final Identifier WHITE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int SEGMENTS = 13;

    public DevourerTendrilVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(DevourerTendrilVisualEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(DevourerTendrilVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(DevourerTendrilVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        DevourerMassVisualEntity mass = entity.getMass();
        if (mass == null) {
            return;
        }
        float age = entity.age + tickDelta;
        Vec3d entityPosition = entity.getLerpedPos(tickDelta);
        Vec3d center = mass.getRenderPosition(tickDelta).subtract(entityPosition);
        Entity target = entity.getTarget();
        Vec3d endpoint = target == null
                ? entity.getStoredEnd().subtract(entityPosition)
                : target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.55, 0.0).subtract(entityPosition);
        Vec3d direction = endpoint.subtract(center);
        if (direction.lengthSquared() < 1.0E-5) {
            return;
        }
        float massRadius = mass.getCurrentRadius(tickDelta);
        Vec3d outward = direction.normalize();
        Vec3d anchorSide = new Vec3d(-outward.z, 0.0, outward.x);
        if (anchorSide.lengthSquared() < 1.0E-5) {
            anchorSide = new Vec3d(1.0, 0.0, 0.0);
        } else {
            anchorSide = anchorSide.normalize();
        }
        long seed = entity.getSeed();
        double radialFactor = MathHelper.lerp(
                noise(seed ^ 0x632be59bd9b4e019L), 0.10, 0.25);
        double lateralOffset = (noise(seed ^ 0x9e3779b97f4a7c15L) - 0.5)
                * massRadius * 0.14;
        double verticalOffset = (noise(seed ^ 0xc2b2ae3d27d4eb4fL) - 0.5)
                * massRadius * 0.18;
        Vec3d start = center.add(outward.multiply(massRadius * radialFactor))
                .add(anchorSide.multiply(lateralOffset))
                .add(0.0, verticalOffset, 0.0);
        float retract = Math.max(mass.getCollapseProgress(tickDelta), retractionProgress(entity, age));
        endpoint = endpoint.lerp(start, ease(retract));
        float reveal = ease(MathHelper.clamp(age / 7.0F, 0.0F, 1.0F));
        Vec3d[] path = buildPath(entity, start, endpoint, age);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer dark = consumers.getBuffer(RenderLayer.getDebugQuads());
        renderPath(entity, path, reveal, retract, age, matrix, dark, false, light);
        if (target != null && reveal > 0.82F && retract < 0.35F) {
            renderGrip(endpoint, target, age, matrix, dark, false, light);
        }

        VertexConsumer glow = consumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(WHITE));
        renderPath(entity, path, reveal, retract, age, matrix, glow, true,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        if (target != null && reveal > 0.82F && retract < 0.35F) {
            renderGrip(endpoint, target, age, matrix, glow, true,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static Vec3d[] buildPath(DevourerTendrilVisualEntity entity,
                                     Vec3d start, Vec3d end, float age) {
        Vec3d line = end.subtract(start);
        double length = line.length();
        Vec3d side = new Vec3d(-line.z, 0.0, line.x);
        if (side.lengthSquared() < 1.0E-5) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        double bend = Math.min(1.35, 0.35 + length * 0.09);
        double seedAngle = noise(entity.getSeed()) * MathHelper.TAU;
        Vec3d firstControl = start.lerp(end, 0.27)
                .add(side.multiply(Math.cos(seedAngle) * bend)).add(0.0, 0.55 + bend * 0.25, 0.0);
        Vec3d secondControl = start.lerp(end, 0.72)
                .add(side.multiply(-Math.sin(seedAngle) * bend * 0.7)).add(0.0, 0.32, 0.0);
        Vec3d[] points = new Vec3d[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float progress = i / (float) SEGMENTS;
            Vec3d point = cubic(start, firstControl, secondControl, end, progress);
            double envelope = Math.sin(progress * Math.PI);
            double wriggle = Math.sin(age * 0.22 + progress * Math.PI * 5.0 + seedAngle)
                    * envelope * Math.min(0.24, 0.08 + length * 0.012);
            points[i] = point.add(side.multiply(wriggle));
        }
        return points;
    }

    private static void renderPath(DevourerTendrilVisualEntity entity, Vec3d[] path,
                                   float reveal, float retract, float age,
                                   Matrix4f matrix, VertexConsumer vertices,
                                   boolean glow, int light) {
        float pulseAge = age - entity.getPulseAge();
        float pulsePosition = pulseAge >= 0.0F && pulseAge <= 9.0F ? 1.0F - pulseAge / 9.0F : -1.0F;
        float fade = 1.0F - ease(retract);
        for (int i = 0; i < SEGMENTS; i++) {
            float segmentStart = i / (float) SEGMENTS;
            if (segmentStart >= reveal) {
                break;
            }
            float segmentEnd = (i + 1.0F) / SEGMENTS;
            float clip = MathHelper.clamp((reveal - segmentStart) * SEGMENTS, 0.0F, 1.0F);
            Vec3d start = path[i];
            Vec3d end = path[i].lerp(path[i + 1], clip);
            float progress = MathHelper.lerp(clip * 0.5F, segmentStart, segmentEnd);
            float pulse = pulsePosition < 0.0F ? 0.0F
                    : MathHelper.clamp(1.0F - Math.abs(progress - pulsePosition) / 0.16F, 0.0F, 1.0F);
            float width = MathHelper.lerp(progress, 0.155F, 0.062F) * (0.68F + reveal * 0.32F);
            if (glow) {
                float glowWidth = width * (0.18F + pulse * 0.42F);
                int alpha = Math.round((110.0F + pulse * 135.0F) * fade);
                drawCrossRibbon(vertices, matrix, start, end, glowWidth,
                        pulse > 0.05F ? 184 : 92, pulse > 0.05F ? 112 : 54,
                        255, alpha, light, true);
            } else {
                int alpha = Math.round(245.0F * fade);
                drawCrossRibbon(vertices, matrix, start, end, width,
                        20, 8, 48, alpha, light, false);
                drawCrossRibbon(vertices, matrix, start, end, width * 0.42F,
                        46, 14, 78, Math.round(230.0F * fade), light, false);
            }
        }
    }

    private static void renderGrip(Vec3d center, Entity target, float age,
                                   Matrix4f matrix, VertexConsumer vertices,
                                   boolean glow, int light) {
        if (target instanceof ItemEntity || target instanceof ExperienceOrbEntity) {
            renderLootGrip(center, target, age, matrix, vertices, glow, light);
            return;
        }
        float radius = Math.max(0.28F, target.getWidth() * 0.62F);
        for (int ring = 0; ring < 2; ring++) {
            float y = (ring - 0.5F) * Math.min(0.7F, target.getHeight() * 0.34F);
            float rotation = age * (ring == 0 ? 0.08F : -0.07F) + ring * 0.7F;
            for (int i = 0; i < 12; i++) {
                float firstAngle = rotation + i * MathHelper.TAU / 12.0F;
                float secondAngle = rotation + (i + 1) * MathHelper.TAU / 12.0F;
                Vec3d first = center.add(Math.cos(firstAngle) * radius, y, Math.sin(firstAngle) * radius);
                Vec3d second = center.add(Math.cos(secondAngle) * radius, y, Math.sin(secondAngle) * radius);
                drawCrossRibbon(vertices, matrix, first, second, glow ? 0.018F : 0.052F,
                        glow ? 124 : 28, glow ? 72 : 10, glow ? 255 : 62,
                        glow ? 165 : 230, light, glow);
            }
        }
    }

    private static void renderLootGrip(Vec3d center, Entity target, float age,
                                       Matrix4f matrix, VertexConsumer vertices,
                                       boolean glow, int light) {
        float radius = MathHelper.clamp(target.getWidth() * 0.7F, 0.13F, 0.23F);
        float rotation = age * 0.075F;
        for (int i = 0; i < 4; i++) {
            float firstAngle = rotation + i * MathHelper.TAU / 4.0F;
            float secondAngle = rotation + (i + 1) * MathHelper.TAU / 4.0F;
            Vec3d first = center.add(Math.cos(firstAngle) * radius, 0.0,
                    Math.sin(firstAngle) * radius);
            Vec3d second = center.add(Math.cos(secondAngle) * radius, 0.0,
                    Math.sin(secondAngle) * radius);
            drawCrossRibbon(vertices, matrix, first, second, glow ? 0.012F : 0.032F,
                    glow ? 124 : 28, glow ? 72 : 10, glow ? 255 : 62,
                    glow ? 150 : 220, light, glow);
        }
    }

    private static void drawCrossRibbon(VertexConsumer vertices, Matrix4f matrix,
                                        Vec3d start, Vec3d end, float halfWidth,
                                        int red, int green, int blue, int alpha,
                                        int light, boolean emissive) {
        if (alpha <= 0) {
            return;
        }
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-5) {
            return;
        }
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x);
        if (side.lengthSquared() < 1.0E-5) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = direction.normalize().crossProduct(side).normalize();
        drawRibbon(vertices, matrix, start, end, side.multiply(halfWidth),
                red, green, blue, alpha, light, emissive);
        drawRibbon(vertices, matrix, start, end, up.multiply(halfWidth),
                red, green, blue, alpha, light, emissive);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix,
                                   Vec3d start, Vec3d end, Vec3d width,
                                   int red, int green, int blue, int alpha,
                                   int light, boolean emissive) {
        drawQuad(vertices, matrix, start.add(width), start.subtract(width),
                end.subtract(width), end.add(width), red, green, blue, alpha, light, emissive);
    }

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix,
                                 Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                 int red, int green, int blue, int alpha,
                                 int light, boolean emissive) {
        putVertex(vertices, matrix, a, red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, b, red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, c, red, green, blue, alpha, light, emissive);
        putVertex(vertices, matrix, d, red, green, blue, alpha, light, emissive);
    }

    private static void putVertex(VertexConsumer vertices, Matrix4f matrix, Vec3d position,
                                  int red, int green, int blue, int alpha,
                                  int light, boolean emissive) {
        var vertex = vertices.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha);
        if (emissive) {
            vertex.texture(0.5F, 0.5F)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(0.0F, 1.0F, 0.0F);
        } else {
            vertex.light(light);
        }
    }

    private static Vec3d cubic(Vec3d a, Vec3d b, Vec3d c, Vec3d d, float t) {
        double inverse = 1.0 - t;
        return a.multiply(inverse * inverse * inverse)
                .add(b.multiply(3.0 * inverse * inverse * t))
                .add(c.multiply(3.0 * inverse * t * t))
                .add(d.multiply(t * t * t));
    }

    private static float retractionProgress(DevourerTendrilVisualEntity entity, float age) {
        return entity.getRetractAge() < 0 ? 0.0F
                : MathHelper.clamp((age - entity.getRetractAge()) / 7.0F, 0.0F, 1.0F);
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
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
}
