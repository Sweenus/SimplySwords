package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.SoulPyreVisualEntity;
import org.joml.Matrix4f;

public class SoulPyreVisualEntityRenderer extends EntityRenderer<SoulPyreVisualEntity> {

    private static final int SEGMENTS = 96;
    private static final Identifier WHITE_TEXTURE =
            Identifier.ofVanilla("textures/misc/white.png");

    private final TerrainFieldOverlayRenderer terrainOverlay =
            new TerrainFieldOverlayRenderer();

    public SoulPyreVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SoulPyreVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(SoulPyreVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        float reach = entity.getMaxRadius() + 3.0F;
        float vertical = entity.getVerticalRange() + 2.0F;
        return frustum.isVisible(new Box(
                entity.getX() - reach,
                entity.getY() - vertical,
                entity.getZ() - reach,
                entity.getX() + reach,
                entity.getY() + vertical,
                entity.getZ() + reach
        ));
    }

    @Override
    public void render(SoulPyreVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light) {
        if (!Config.general.enableModernFieldEffects || entity.getRadius() <= 0.05F) {
            terrainOverlay.clear(entity.getUuid());
            return;
        }

        Vec3d entityRenderPos = interpolatedPosition(entity, tickDelta);
        Entity owner = entity.getWorld().getEntityById(entity.getOwnerEntityId());
        Vec3d center = owner == null
                ? entityRenderPos
                : interpolatedPosition(owner, tickDelta);
        Vec3d offset = center.subtract(entityRenderPos);

        matrices.push();
        matrices.translate(offset.x, offset.y, offset.z);
        float radius = entity.getRadius();
        terrainOverlay.render(
                entity.getWorld(),
                entity.getUuid(),
                center.x,
                center.y,
                center.z,
                radius,
                entity.getMaxRadius(),
                entity.getVerticalRange(),
                TerrainFieldOverlayRenderer.SOUL_PYRE,
                matrices.peek(),
                vertexConsumers
        );

        float time = entity.age + tickDelta;
        float pulse = 0.5F + 0.5F * MathHelper.sin(time * 0.22F);
        VertexConsumer bands = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        VertexConsumer glow = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));

        drawBand(entity, center, matrices.peek(), bands, radius, 0.085F,
                24, 180, 188, 125 + (int) (pulse * 55.0F), false, 0);
        drawBand(entity, center, matrices.peek(), bands,
                Math.max(0.1F, radius - 0.36F), 0.035F,
                65, 225, 218, 50 + (int) (pulse * 30.0F), true, 5);
        drawBand(entity, center, matrices.peek(), bands,
                Math.max(0.1F, radius * 0.58F), 0.028F,
                16, 105, 112, 35, true, 11);

        float pulseAge = time - entity.getPulseStartAge();
        if (pulseAge >= 0.0F && pulseAge <= 13.0F) {
            float progress = pulseAge / 13.0F;
            float inwardRadius = MathHelper.lerp(progress, radius, 0.25F);
            drawBand(entity, center, matrices.peek(), bands,
                    inwardRadius, 0.07F,
                    72, 245, 231,
                    (int) (145.0F * (1.0F - progress)), false, 17);
        }

        drawPerimeterFlames(entity, center, matrices.peek(), glow, radius, time);
        drawOrbitingSouls(entity, matrices, vertexConsumers, time);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawOrbitingSouls(SoulPyreVisualEntity entity, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, float time) {
        int count = MathHelper.clamp(entity.getSoulCount(), 0, 16);
        if (count <= 0) {
            return;
        }
        VertexConsumer vertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        float orbitRadius = 1.15F + Math.min(0.4F, count * 0.035F);
        for (int i = 0; i < count; i++) {
            float angle = time * 0.075F + MathHelper.TAU * i / count;
            float bob = MathHelper.sin(time * 0.16F + i * 1.41F) * 0.18F;
            matrices.push();
            matrices.translate(
                    MathHelper.cos(angle) * orbitRadius,
                    1.05F + bob,
                    MathHelper.sin(angle) * orbitRadius
            );
            matrices.multiply(this.dispatcher.getRotation());
            float scale = 0.13F + MathHelper.sin(time * 0.24F + i) * 0.018F;
            drawSoulDiamond(vertices, matrices.peek().getPositionMatrix(),
                    scale * 1.8F, 42, 218, 220, 185);
            drawSoulDiamond(vertices, matrices.peek().getPositionMatrix(),
                    scale, 190, 255, 245, 235);
            matrices.pop();
        }
    }

    private static void drawSoulDiamond(VertexConsumer vertices, Matrix4f matrix,
                                        float size, int red, int green,
                                        int blue, int alpha) {
        vertex(vertices, matrix, 0.0F, size, 0.0F, 0.5F, 0.0F,
                red, green, blue, alpha);
        vertex(vertices, matrix, size * 0.62F, 0.0F, 0.0F, 1.0F, 0.5F,
                red, green, blue, alpha);
        vertex(vertices, matrix, 0.0F, -size, 0.0F, 0.5F, 1.0F,
                red, green, blue, alpha);
        vertex(vertices, matrix, -size * 0.62F, 0.0F, 0.0F, 0.0F, 0.5F,
                red, green, blue, alpha);
    }

    private static void drawBand(SoulPyreVisualEntity entity, Vec3d center,
                                 MatrixStack.Entry matrices, VertexConsumer vertices,
                                 float radius, float halfWidth,
                                 int red, int green, int blue, int alpha,
                                 boolean broken, int salt) {
        if (radius <= 0.05F || alpha <= 0) {
            return;
        }
        float inner = Math.max(0.0F, radius - halfWidth);
        float outer = radius + halfWidth;
        for (int i = 0; i < SEGMENTS; i++) {
            if (broken && Math.floorMod(i * 31 + entity.getSeed() + salt, 17) < 5) {
                continue;
            }
            double angle = MathHelper.TAU * i / SEGMENTS;
            double next = MathHelper.TAU * (i + 1) / SEGMENTS;
            float ix = (float) (Math.cos(angle) * inner);
            float iz = (float) (Math.sin(angle) * inner);
            float ox = (float) (Math.cos(angle) * outer);
            float oz = (float) (Math.sin(angle) * outer);
            float nix = (float) (Math.cos(next) * inner);
            float niz = (float) (Math.sin(next) * inner);
            float nox = (float) (Math.cos(next) * outer);
            float noz = (float) (Math.sin(next) * outer);
            float y0 = groundOffset(entity, center, ox, oz);
            float y1 = groundOffset(entity, center, nox, noz);
            vertices.vertex(matrices, ix, y0, iz).color(red, green, blue, alpha);
            vertices.vertex(matrices, ox, y0, oz).color(red, green, blue, alpha);
            vertices.vertex(matrices, nox, y1, noz).color(red, green, blue, alpha);
            vertices.vertex(matrices, nix, y1, niz).color(red, green, blue, alpha);
        }
    }

    private static void drawPerimeterFlames(
            SoulPyreVisualEntity entity, Vec3d center,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            float radius, float time) {
        for (int i = 0; i < SEGMENTS; i += 3) {
            if (Math.floorMod(i * 23 + entity.getSeed(), 13) < 4) {
                continue;
            }
            double angle = MathHelper.TAU * i / SEGMENTS;
            double next = MathHelper.TAU * (i + 2) / SEGMENTS;
            float x0 = (float) (Math.cos(angle) * radius);
            float z0 = (float) (Math.sin(angle) * radius);
            float x1 = (float) (Math.cos(next) * radius);
            float z1 = (float) (Math.sin(next) * radius);
            float y0 = groundOffset(entity, center, x0, z0);
            float y1 = groundOffset(entity, center, x1, z1);
            float wave = 0.5F + 0.5F * MathHelper.sin(time * 0.31F + i * 0.57F);
            float h0 = 0.18F + wave * 0.52F;
            float h1 = 0.18F + (1.0F - wave) * 0.52F;
            int alpha = 26 + (int) (wave * 42.0F);
            float nx0 = (float) Math.cos(angle);
            float nz0 = (float) Math.sin(angle);
            float nx1 = (float) Math.cos(next);
            float nz1 = (float) Math.sin(next);
            flameVertex(matrices, vertices, x0, y0, z0,
                    36, 214, 211, alpha, nx0, nz0);
            flameVertex(matrices, vertices, x1, y1, z1,
                    36, 214, 211, alpha, nx1, nz1);
            flameVertex(matrices, vertices, x1, y1 + h1, z1,
                    98, 244, 230, 0, nx1, nz1);
            flameVertex(matrices, vertices, x0, y0 + h0, z0,
                    98, 244, 230, 0, nx0, nz0);
        }
    }

    private static float groundOffset(
            SoulPyreVisualEntity entity, Vec3d center, float localX, float localZ) {
        int x = MathHelper.floor(center.x + localX);
        int z = MathHelper.floor(center.z + localZ);
        int verticalRange = MathHelper.ceil(entity.getVerticalRange());
        int start = Math.min(
                entity.getWorld().getTopY() - 1,
                MathHelper.floor(center.y) + verticalRange);
        int end = Math.max(
                entity.getWorld().getBottomY() + 1,
                MathHelper.floor(center.y) - verticalRange);
        BlockPos.Mutable cursor = new BlockPos.Mutable(x, start, z);
        for (int y = start; y >= end; y--) {
            cursor.setY(y);
            BlockState state = entity.getWorld().getBlockState(cursor);
            if (!state.getCollisionShape(entity.getWorld(), cursor).isEmpty()) {
                return (float) (y + state.getCollisionShape(entity.getWorld(), cursor)
                        .getBoundingBox().maxY - center.y + 0.045);
            }
        }
        return 0.045F;
    }

    private static Vec3d interpolatedPosition(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private static void flameVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices,
            float x, float y, float z,
            int red, int green, int blue, int alpha,
            float normalX, float normalZ) {
        vertices.vertex(matrices, x, y, z)
                .color(red, green, blue, alpha)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(matrices, normalX, 0.0F, normalZ);
    }

    private static void vertex(
            VertexConsumer vertices, Matrix4f matrix,
            float x, float y, float z, float u, float v,
            int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0.0F, 1.0F, 0.0F);
    }
}
