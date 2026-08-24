package net.sweenus.simplyswords.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.render.ParchmentVisualIds;
import net.sweenus.simplyswords.client.api.ParchmentGlyphStyle;
import net.sweenus.simplyswords.client.api.ParchmentVisualRegistry;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public final class ParchmentRendering {

    public static final Identifier PAGE_TEXTURE = Identifier.of("simplyswords", "textures/entity/parchment_page.png");
    public static final Identifier RIBBON_TEXTURE = Identifier.of("simplyswords", "textures/entity/parchment_ribbon.png");
    public static final Identifier SEAL_TEXTURE = Identifier.of("simplyswords", "textures/entity/wax_seal.png");
    public static final Identifier GLYPHS_TEXTURE = Identifier.of("simplyswords", "textures/entity/parchment_glyphs.png");
    public static final Identifier GLYPHS_GLOW_TEXTURE = Identifier.of("simplyswords", "textures/entity/parchment_glyphs_glow.png");

    private static final int FULLBRIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    private static final float RIBBON_TILE_ASPECT = 8.0F;

    private ParchmentRendering() {
    }

    public static int floorLight(int light) {
        int block = Math.max(LightmapTextureManager.getBlockLightCoordinates(light), 6);
        int sky = LightmapTextureManager.getSkyLightCoordinates(light);
        return LightmapTextureManager.pack(block, sky);
    }

    public static void renderBentPage(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                      float age, float seed, float width, float height, float curlAmp, float alpha) {
        int segments = 5;
        float segLength = width / segments;

        float[] xs = new float[segments + 1];
        float[] zs = new float[segments + 1];
        float[] shade = new float[segments + 1];

        float x = -width * 0.5F;
        float z = 0.0F;
        float theta = 0.0F;
        xs[0] = x;
        zs[0] = z;
        shade[0] = 1.0F;
        for (int i = 1; i <= segments; i++) {
            theta += curlAmp * MathHelper.sin(age * 0.17F + seed + i * 0.9F);
            x += segLength * MathHelper.cos(theta);
            z += segLength * MathHelper.sin(theta);
            xs[i] = x;
            zs[i] = z;
            shade[i] = 0.78F + 0.22F * MathHelper.cos(theta);
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(PAGE_TEXTURE));
        int packedLight = floorLight(light);
        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        float halfHeight = height * 0.5F;

        for (int i = 0; i < segments; i++) {
            float u0 = i / (float) segments;
            float u1 = (i + 1) / (float) segments;
            int s0 = (int) (255 * shade[i]);
            int s1 = (int) (255 * shade[i + 1]);
            vertex(vertices, matrix, xs[i], -halfHeight, zs[i], u0, 1.0F, s0, a, packedLight);
            vertex(vertices, matrix, xs[i + 1], -halfHeight, zs[i + 1], u1, 1.0F, s1, a, packedLight);
            vertex(vertices, matrix, xs[i + 1], halfHeight, zs[i + 1], u1, 0.0F, s1, a, packedLight);
            vertex(vertices, matrix, xs[i], halfHeight, zs[i], u0, 0.0F, s0, a, packedLight);
        }
    }

    public static void renderRibbon(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    List<Vec3d> path, float halfWidth, Vec3d widthDir,
                                    float age, float alpha) {
        int count = path.size();
        if (count < 2) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(RIBBON_TEXTURE));
        int packedLight = floorLight(light);
        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);

        Vec3d[] sides = new Vec3d[count];
        float[] lengths = new float[count];
        float total = 0.0F;
        for (int i = 0; i < count; i++) {
            Vec3d tangent = path.get(Math.min(i + 1, count - 1)).subtract(path.get(Math.max(i - 1, 0)));
            Vec3d side;
            if (widthDir != null) {
                side = widthDir;
            } else {
                side = tangent.crossProduct(new Vec3d(0.0, 1.0, 0.0));
                if (side.lengthSquared() < 1.0E-4) {
                    side = tangent.crossProduct(new Vec3d(1.0, 0.0, 0.0));
                }
            }
            float taper = Math.min(1.0F, Math.min(i, count - 1 - i) / 2.5F + 0.25F);
            sides[i] = side.normalize().multiply(halfWidth * taper);
            if (i > 0) {
                total += (float) path.get(i).distanceTo(path.get(i - 1));
            }
            lengths[i] = total;
        }

        float tileLength = Math.max(0.25F, halfWidth * 2.0F * RIBBON_TILE_ASPECT);
        float scroll = age * 0.012F;
        for (int i = 0; i < count - 1; i++) {
            float v0 = lengths[i] / tileLength + scroll;
            float v1 = lengths[i + 1] / tileLength + scroll;
            Vec3d p0a = path.get(i).add(sides[i]);
            Vec3d p0b = path.get(i).subtract(sides[i]);
            Vec3d p1a = path.get(i + 1).add(sides[i + 1]);
            Vec3d p1b = path.get(i + 1).subtract(sides[i + 1]);
            vertex(vertices, matrix, p0a, 0.0F, v0, 255, a, packedLight);
            vertex(vertices, matrix, p0b, 1.0F, v0, 255, a, packedLight);
            vertex(vertices, matrix, p1b, 1.0F, v1, 255, a, packedLight);
            vertex(vertices, matrix, p1a, 0.0F, v1, 255, a, packedLight);
        }
    }

    public static List<Vec3d> buildWindingPath(Vec3d start, Vec3d end, long seed, float age,
                                               float amplitude, int samples) {
        Random random = new Random(seed);
        float phase1 = random.nextFloat() * MathHelper.TAU;
        float phase2 = random.nextFloat() * MathHelper.TAU;
        float waves1 = 1.5F + random.nextFloat() * 1.5F;
        float waves2 = 2.0F + random.nextFloat() * 1.5F;

        Vec3d axis = end.subtract(start);
        Vec3d perp1 = axis.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (perp1.lengthSquared() < 1.0E-4) {
            perp1 = axis.crossProduct(new Vec3d(1.0, 0.0, 0.0));
        }
        perp1 = perp1.normalize();
        Vec3d perp2 = axis.crossProduct(perp1).normalize();

        List<Vec3d> path = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            float t = i / (float) samples;
            float envelope = MathHelper.sin(t * MathHelper.PI);
            float o1 = MathHelper.sin(t * MathHelper.TAU * waves1 + phase1 + age * 0.055F) * amplitude * envelope;
            float o2 = MathHelper.sin(t * MathHelper.TAU * waves2 + phase2 + age * 0.038F) * amplitude * 0.6F * envelope;
            path.add(start.add(axis.multiply(t)).add(perp1.multiply(o1)).add(perp2.multiply(o2)));
        }
        return path;
    }

    public static void renderSeal(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  float scale, float alpha) {
        renderTexturedQuad(matrices, vertexConsumers, RenderLayer.getEntityTranslucent(SEAL_TEXTURE),
                floorLight(light), scale, 0.0F, 0.0F, 1.0F, 1.0F, alpha);
    }

    public static void renderGlyph(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int glyphIndex, float scale, float alpha) {
        renderGlyph(matrices, vertexConsumers, ParchmentVisualIds.DEFAULT_GLYPH_STYLE,
                glyphIndex, scale, alpha);
    }

    public static void renderGlyph(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   Identifier styleId, int glyphIndex, float scale, float alpha) {
        ParchmentGlyphStyle style = ParchmentVisualRegistry.getGlyphStyle(styleId);
        int cell = Math.floorMod(glyphIndex, style.columns() * style.rows());
        float u0 = (cell % style.columns()) / (float) style.columns();
        float v0 = (cell / style.columns()) / (float) style.rows();
        float u1 = u0 + 1.0F / style.columns();
        float v1 = v0 + 1.0F / style.rows();

        renderTexturedQuad(matrices, vertexConsumers,
                RenderLayer.getEntityTranslucentEmissive(style.glowTexture()), FULLBRIGHT,
                scale * style.haloScale(), u0, v0, u1, v1,
                alpha * style.haloAlpha(), style.glowColor());
        renderTexturedQuad(matrices, vertexConsumers,
                RenderLayer.getEntityTranslucentEmissive(style.glyphTexture()), FULLBRIGHT,
                scale, u0, v0, u1, v1, alpha, style.glyphColor());
    }

    public static void renderGlyphArc(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      int glyphBase, float progress, float age, float scale) {
        renderGlyphArc(matrices, vertexConsumers, ParchmentVisualIds.DEFAULT_GLYPH_STYLE,
                glyphBase, 7, progress, age, scale);
    }

    public static void renderGlyphArc(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      Identifier styleId, int glyphBase, int glyphCount,
                                      float progress, float age, float scale) {
        int count = Math.max(1, glyphCount);
        float written = progress * count;
        for (int i = 0; i < count; i++) {
            float reveal = MathHelper.clamp(written - i, 0.0F, 1.0F);
            if (reveal <= 0.0F) {
                continue;
            }
            float arc = (i / (float) Math.max(1, count - 1) - 0.5F) * 1.75F;
            float jitterX = MathHelper.sin(age * 0.23F + i * 3.7F) * 0.02F;
            float jitterY = MathHelper.sin(age * 0.19F + i * 2.3F) * 0.02F;

            matrices.push();
            matrices.translate(
                    (MathHelper.sin(arc) * 0.55F + jitterX) * scale,
                    (0.12F + MathHelper.cos(arc) * 0.08F + jitterY + (1.0F - reveal) * 0.08F) * scale,
                    (-MathHelper.cos(arc) * 0.5F - 0.15F) * scale);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - arc * 24.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.sin(age * 0.11F + i * 1.9F) * 9.0F));
            renderGlyph(matrices, vertexConsumers, styleId, glyphBase + i,
                    (0.16F + reveal * 0.04F) * scale, reveal * (0.6F + progress * 0.4F));
            matrices.pop();
        }
    }

    private static void renderTexturedQuad(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                           RenderLayer layer, int packedLight, float scale,
                                           float u0, float v0, float u1, float v1, float alpha) {
        renderTexturedQuad(matrices, vertexConsumers, layer, packedLight, scale,
                u0, v0, u1, v1, alpha, 0xFFFFFFFF);
    }

    private static void renderTexturedQuad(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                           RenderLayer layer, int packedLight, float scale,
                                           float u0, float v0, float u1, float v1, float alpha, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(layer);
        int colorAlpha = color >>> 24 & 0xFF;
        int a = (int) (MathHelper.clamp(alpha, 0.0F, 1.0F) * colorAlpha);
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        float half = scale * 0.5F;
        vertex(vertices, matrix, -half, -half, 0.0F, u0, v1, red, green, blue, a, packedLight);
        vertex(vertices, matrix, half, -half, 0.0F, u1, v1, red, green, blue, a, packedLight);
        vertex(vertices, matrix, half, half, 0.0F, u1, v0, red, green, blue, a, packedLight);
        vertex(vertices, matrix, -half, half, 0.0F, u0, v0, red, green, blue, a, packedLight);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d pos,
                               float u, float v, int shade, int alpha, int light) {
        vertex(vertices, matrix, (float) pos.x, (float) pos.y, (float) pos.z, u, v, shade, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
                               float u, float v, int shade, int alpha, int light) {
        vertex(vertices, matrix, x, y, z, u, v, shade, shade, shade, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
                               float u, float v, int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0.0F, 1.0F, 0.0F);
    }
}
