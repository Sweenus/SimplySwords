package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.CaelestisBreachVisualEntity;
import org.joml.Matrix4f;

public class CaelestisBreachVisualEntityRenderer
        extends EntityRenderer<CaelestisBreachVisualEntity> {

    private static final int SEGMENTS = 96;
    private static final float CORE_HALF_WIDTH = 0.11F;
    private static final float VEIL_HEIGHT = 2.8F;

    public CaelestisBreachVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CaelestisBreachVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CaelestisBreachVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!Config.general.enableModernFieldEffects || entity.getRadius() <= 0.05F) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        float time = entity.age + tickDelta;
        float radius = entity.getRadius();
        float pulse = 0.5F + 0.5F * MathHelper.sin(time * 0.18F);
        boolean collapsing = entity.getPhase() == CaelestisBreachVisualEntity.PHASE_COLLAPSING;

        int coreRed = collapsing ? 218 : 126;
        int coreGreen = collapsing ? 20 : 26;
        int coreBlue = collapsing ? 70 : 182;
        int edgeRed = collapsing ? 255 : 176;
        int edgeGreen = collapsing ? 34 : 20;
        int edgeBlue = collapsing ? 56 : 116;

        drawTerrainBand(entity, vertices, matrix, radius, CORE_HALF_WIDTH,
                coreRed, coreGreen, coreBlue, 155 + (int) (pulse * 55.0F), false, 0);
        drawTerrainBand(entity, vertices, matrix, Math.max(0.1F, radius - 0.42F), 0.055F,
                edgeRed, edgeGreen, edgeBlue, 65 + (int) (pulse * 35.0F), true, 5);
        drawTerrainBand(entity, vertices, matrix, radius + 0.34F, 0.05F,
                74, 8, 104, 50 + (int) ((1.0F - pulse) * 30.0F), true, 9);

        float rippleProgress = Math.floorMod(entity.age, 34) / 34.0F;
        float rippleRadius = collapsing
                ? MathHelper.lerp(rippleProgress, radius + 1.1F, Math.max(0.1F, radius - 1.2F))
                : MathHelper.lerp(rippleProgress, Math.max(0.1F, radius - 1.2F), radius + 0.75F);
        drawTerrainBand(entity, vertices, matrix, rippleRadius, 0.04F,
                edgeRed, edgeGreen, edgeBlue,
                MathHelper.clamp((int) (88.0F * (1.0F - rippleProgress)), 0, 88), true, 13);

        drawBoundaryVeil(entity, vertices, matrix, radius, time, edgeRed, edgeGreen, edgeBlue);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void drawTerrainBand(CaelestisBreachVisualEntity entity, VertexConsumer vertices,
                                        Matrix4f matrix, float radius, float halfWidth,
                                        int red, int green, int blue, int alpha,
                                        boolean broken, int salt) {
        if (radius <= 0.05F || alpha <= 0) {
            return;
        }
        float inner = Math.max(0.0F, radius - halfWidth);
        float outer = radius + halfWidth;
        for (int i = 0; i < SEGMENTS; i++) {
            if (broken && Math.floorMod(i * 31 + entity.getSeed() + salt, 17) < 3) {
                continue;
            }
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            double next = Math.PI * 2.0 * (i + 1) / SEGMENTS;
            float ix = (float) (Math.cos(angle) * inner);
            float iz = (float) (Math.sin(angle) * inner);
            float ox = (float) (Math.cos(angle) * outer);
            float oz = (float) (Math.sin(angle) * outer);
            float nix = (float) (Math.cos(next) * inner);
            float niz = (float) (Math.sin(next) * inner);
            float nox = (float) (Math.cos(next) * outer);
            float noz = (float) (Math.sin(next) * outer);
            float y0 = groundOffset(entity, ox, oz);
            float y1 = groundOffset(entity, nox, noz);

            vertices.vertex(matrix, ix, y0, iz).color(red, green, blue, alpha);
            vertices.vertex(matrix, ox, y0, oz).color(red, green, blue, alpha);
            vertices.vertex(matrix, nox, y1, noz).color(red, green, blue, alpha);
            vertices.vertex(matrix, nix, y1, niz).color(red, green, blue, alpha);
        }
    }

    private static void drawBoundaryVeil(CaelestisBreachVisualEntity entity, VertexConsumer vertices,
                                         Matrix4f matrix, float radius, float time,
                                         int red, int green, int blue) {
        for (int i = 0; i < SEGMENTS; i += 2) {
            if (Math.floorMod(i * 19 + entity.getSeed(), 13) < 4) {
                continue;
            }
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            double next = Math.PI * 2.0 * (i + 2) / SEGMENTS;
            float x0 = (float) (Math.cos(angle) * radius);
            float z0 = (float) (Math.sin(angle) * radius);
            float x1 = (float) (Math.cos(next) * radius);
            float z1 = (float) (Math.sin(next) * radius);
            float y0 = groundOffset(entity, x0, z0);
            float y1 = groundOffset(entity, x1, z1);
            float wave = 0.5F + 0.5F * MathHelper.sin(time * 0.24F + i * 0.43F);
            float h0 = VEIL_HEIGHT * (0.45F + wave * 0.55F);
            float h1 = VEIL_HEIGHT * (0.45F + (1.0F - wave) * 0.55F);
            int alpha = 15 + (int) (wave * 24.0F);

            vertices.vertex(matrix, x0, y0, z0).color(red, green, blue, alpha);
            vertices.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
            vertices.vertex(matrix, x1, y1 + h1, z1).color(red, green, blue, 0);
            vertices.vertex(matrix, x0, y0 + h0, z0).color(red, green, blue, 0);
        }
    }

    private static float groundOffset(CaelestisBreachVisualEntity entity, float localX, float localZ) {
        int x = MathHelper.floor(entity.getX() + localX);
        int z = MathHelper.floor(entity.getZ() + localZ);
        int start = Math.min(entity.getWorld().getTopY() - 1, MathHelper.floor(entity.getY()) + 6);
        int end = Math.max(entity.getWorld().getBottomY() + 1, MathHelper.floor(entity.getY()) - 12);
        BlockPos.Mutable cursor = new BlockPos.Mutable(x, start, z);
        for (int y = start; y >= end; y--) {
            cursor.setY(y);
            BlockState state = entity.getWorld().getBlockState(cursor);
            if (!state.getCollisionShape(entity.getWorld(), cursor).isEmpty()) {
                return (float) (y + state.getCollisionShape(entity.getWorld(), cursor)
                        .getBoundingBox().maxY - entity.getY() + 0.045);
            }
        }
        return 0.045F;
    }
}
