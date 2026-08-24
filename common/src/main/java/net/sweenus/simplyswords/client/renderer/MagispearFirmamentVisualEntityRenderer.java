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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.MagispearFirmamentVisualEntity;
import org.joml.Matrix4f;

public class MagispearFirmamentVisualEntityRenderer
        extends EntityRenderer<MagispearFirmamentVisualEntity> {

    private static final int SEGMENTS = 72;

    public MagispearFirmamentVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(MagispearFirmamentVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(MagispearFirmamentVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(Math.max(14.0F, entity.getRadius() * 2.0F)));
    }

    @Override
    public void render(MagispearFirmamentVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float lifetime = Math.max(1.0F, entity.getLifetime());
        float grow = easeOut(MathHelper.clamp(age / 6.0F, 0.0F, 1.0F));
        float fade = age <= 36.0F ? 1.0F : MathHelper.clamp(1.0F - (age - 36.0F) / Math.max(1.0F, lifetime - 36.0F), 0.0F, 1.0F);
        float radius = Math.max(0.5F, entity.getRadius()) * grow;
        int packedLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());

        matrices.push();
        Matrix4f groundMatrix = matrices.peek().getPositionMatrix();
        drawRing(vertices, groundMatrix, radius, 0.105F, 72, 222, 255, alpha(205, fade), packedLight);
        drawRing(vertices, groundMatrix, radius * 0.70F, 0.055F, 174, 72, 255, alpha(155, fade), packedLight);
        drawRing(vertices, groundMatrix, radius * 0.34F, 0.04F, 235, 225, 255, alpha(130, fade), packedLight);
        drawSpokes(vertices, groundMatrix, radius, entity.getWaveCount(), age, fade, packedLight);
        drawStrikeIndicators(vertices, groundMatrix, radius, entity.getWaveCount(), age, fade, packedLight);

        renderSkyRing(matrices, vertices, age, radius * 0.88F, 9.0F, 0.08F,
                age * 2.4F, 64, 218, 255, alpha(155, fade), packedLight);
        renderSkyRing(matrices, vertices, age, radius * 0.63F, 10.35F, 0.07F,
                -age * 3.1F, 178, 70, 255, alpha(175, fade), packedLight);
        renderSkyRing(matrices, vertices, age, radius * 0.38F, 11.45F, 0.055F,
                age * 4.0F, 241, 230, 255, alpha(190, fade), packedLight);

        if (age >= 34.0F) {
            float shockProgress = MathHelper.clamp((age - 34.0F) / 9.0F, 0.0F, 1.0F);
            float shockFade = (1.0F - shockProgress) * fade;
            drawRing(vertices, matrices.peek().getPositionMatrix(),
                    MathHelper.lerp(easeOut(shockProgress), radius * 0.2F, radius * 1.65F),
                    0.13F, 221, 190, 255, alpha(220, shockFade), packedLight);
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void drawSpokes(VertexConsumer vertices, Matrix4f matrix, float radius,
                                   int waveCount, float age, float fade, int light) {
        int count = MathHelper.clamp(waveCount, 1, 8) * 2;
        float rotation = age * 0.012F;
        for (int i = 0; i < count; i++) {
            double angle = MathHelper.TAU * i / count + rotation;
            float startX = (float) Math.cos(angle) * radius * 0.18F;
            float startZ = (float) Math.sin(angle) * radius * 0.18F;
            float endX = (float) Math.cos(angle) * radius * 0.92F;
            float endZ = (float) Math.sin(angle) * radius * 0.92F;
            drawGroundLine(vertices, matrix, startX, startZ, endX, endZ, 0.025F,
                    137, 68, 244, alpha(95, fade), light);
        }
    }

    private static void drawStrikeIndicators(VertexConsumer vertices, Matrix4f matrix, float radius,
                                             int waveCount, float age, float fade, int light) {
        int count = MathHelper.clamp(waveCount, 1, 8) * 2;
        float markerRadius = MathHelper.clamp(radius * 0.095F, 0.22F, 0.48F);
        for (int i = 0; i < count; i++) {
            double angle = MathHelper.TAU * i / count;
            matricesRing(vertices, matrix,
                    (float) Math.cos(angle) * radius * 0.78F,
                    (float) Math.sin(angle) * radius * 0.78F,
                    markerRadius * (0.86F + MathHelper.sin(age * 0.32F + i) * 0.12F),
                    0.035F, 220, 198, 255, alpha(170, fade), light);
        }
    }

    private static void renderSkyRing(MatrixStack matrices, VertexConsumer vertices, float age,
                                      float radius, float height, float thickness, float rotation,
                                      int red, int green, int blue, int alpha, int light) {
        matrices.push();
        matrices.translate(0.0, height, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(61.0F + MathHelper.sin(age * 0.05F) * 7.0F));
        drawRing(vertices, matrices.peek().getPositionMatrix(), radius, thickness, red, green, blue, alpha, light);
        matrices.pop();
    }

    private static void drawRing(VertexConsumer vertices, Matrix4f matrix, float radius, float halfThickness,
                                 int red, int green, int blue, int alpha, int light) {
        matricesRing(vertices, matrix, 0.0F, 0.0F, radius, halfThickness, red, green, blue, alpha, light);
    }

    private static void matricesRing(VertexConsumer vertices, Matrix4f matrix, float centerX, float centerZ,
                                     float radius, float halfThickness, int red, int green, int blue,
                                     int alpha, int light) {
        if (radius <= 0.01F || alpha <= 0) {
            return;
        }
        float inner = Math.max(0.0F, radius - halfThickness);
        float outer = radius + halfThickness;
        for (int i = 0; i < SEGMENTS; i++) {
            double a = MathHelper.TAU * i / SEGMENTS;
            double b = MathHelper.TAU * (i + 1) / SEGMENTS;
            quad(vertices, matrix,
                    centerX + (float) Math.cos(a) * inner, 0.055F, centerZ + (float) Math.sin(a) * inner,
                    centerX + (float) Math.cos(a) * outer, 0.055F, centerZ + (float) Math.sin(a) * outer,
                    centerX + (float) Math.cos(b) * outer, 0.055F, centerZ + (float) Math.sin(b) * outer,
                    centerX + (float) Math.cos(b) * inner, 0.055F, centerZ + (float) Math.sin(b) * inner,
                    red, green, blue, alpha, light);
        }
    }

    private static void drawGroundLine(VertexConsumer vertices, Matrix4f matrix,
                                       float startX, float startZ, float endX, float endZ, float halfWidth,
                                       int red, int green, int blue, int alpha, int light) {
        float dx = endX - startX;
        float dz = endZ - startZ;
        float length = MathHelper.sqrt(dx * dx + dz * dz);
        if (length <= 0.001F) {
            return;
        }
        float sideX = -dz / length * halfWidth;
        float sideZ = dx / length * halfWidth;
        quad(vertices, matrix,
                startX + sideX, 0.052F, startZ + sideZ,
                endX + sideX, 0.052F, endZ + sideZ,
                endX - sideX, 0.052F, endZ - sideZ,
                startX - sideX, 0.052F, startZ - sideZ,
                red, green, blue, alpha, light);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             int red, int green, int blue, int alpha, int light) {
        vertex(vertices, matrix, ax, ay, az, red, green, blue, alpha, light);
        vertex(vertices, matrix, bx, by, bz, red, green, blue, alpha, light);
        vertex(vertices, matrix, cx, cy, cz, red, green, blue, alpha, light);
        vertex(vertices, matrix, dx, dy, dz, red, green, blue, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
                               int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha).light(light);
    }

    private static int alpha(int base, float multiplier) {
        return MathHelper.clamp(Math.round(base * multiplier), 0, 255);
    }

    private static float easeOut(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }
}
