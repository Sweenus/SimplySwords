package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.LivyatanWaveVisualEntity;

import java.util.List;

public class LivyatanWaveVisualEntityRenderer extends EntityRenderer<LivyatanWaveVisualEntity> {

    private static final Identifier WATER_STILL_SPRITE = new Identifier("minecraft", "block/water_still");
    private static final float BLOCK_INSET = 0.01F;
    private static final float FOAM_CAP_THICKNESS = 0.07F;
    private static final float FOAM_SIDE_BAND_HEIGHT = 0.24F;

    public LivyatanWaveVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(LivyatanWaveVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(LivyatanWaveVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        List<LivyatanWaveVisualEntity.WaveLane> lanes = entity.getBloodwakeLanes();
        if (lanes.isEmpty()) {
            return super.shouldRender(entity, frustum, x, y, z);
        }
        double minX = entity.getX() - 0.75;
        double minY = entity.getY() - 0.25;
        double minZ = entity.getZ() - 0.75;
        double maxX = entity.getX() + 0.75;
        double maxY = entity.getY() + entity.getTargetHeight() + 0.5;
        double maxZ = entity.getZ() + 0.75;
        for (LivyatanWaveVisualEntity.WaveLane lane : lanes) {
            minX = Math.min(minX, entity.getX() + lane.offsetX() - 0.75);
            minY = Math.min(minY, entity.getY() + lane.offsetY() - 0.25);
            minZ = Math.min(minZ, entity.getZ() + lane.offsetZ() - 0.75);
            maxX = Math.max(maxX, entity.getX() + lane.offsetX() + 0.75);
            maxY = Math.max(maxY, entity.getY() + lane.offsetY()
                    + lane.targetHeight() + 0.5);
            maxZ = Math.max(maxZ, entity.getZ() + lane.offsetZ() + 0.75);
        }
        return frustum.isVisible(new Box(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public void render(LivyatanWaveVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float heightScale = Math.max(0.0F, entity.getHeightScale());
        if (heightScale <= 0.01F) {
            return;
        }

        Sprite sprite = MinecraftClient.getInstance()
                .getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .getSprite(WATER_STILL_SPRITE);

        List<LivyatanWaveVisualEntity.WaveLane> lanes = entity.getBloodwakeLanes();
        if (entity.isBloodwakeStyle() && !lanes.isEmpty()) {
            for (int laneIndex = 0; laneIndex < lanes.size(); laneIndex++) {
                LivyatanWaveVisualEntity.WaveLane lane = lanes.get(laneIndex);
                renderLane(entity, matrices, vertexConsumers, sprite, light,
                        tickDelta, heightScale, lane.offsetX(), lane.offsetY(),
                        lane.offsetZ(), lane.targetHeight(), laneIndex * 0.33F);
            }
        } else {
            renderLane(entity, matrices, vertexConsumers, sprite, light,
                    tickDelta, heightScale, 0.0F, 0.0F, 0.0F,
                    entity.getTargetHeight(), 0.0F);
        }
    }

    private static void renderLane(
            LivyatanWaveVisualEntity entity, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, Sprite sprite, int light,
            float tickDelta, float heightScale,
            float offsetX, float offsetY, float offsetZ, float targetHeight,
            float foamPhaseOffset) {
        float height = Math.max(0.08F, targetHeight * heightScale);
        matrices.push();
        matrices.translate(offsetX, offsetY, offsetZ);
        if (entity.isBloodwakeStyle()) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                    .rotationDegrees(entity.getYaw()));
        }
        matrices.translate(-0.5, 0.0, -0.5);
        renderWaterCuboid(entity, matrices, vertexConsumers, sprite, light,
                height, tickDelta, foamPhaseOffset);
        matrices.pop();
    }

    private static void renderWaterCuboid(
            LivyatanWaveVisualEntity entity,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            Sprite sprite,
            int light,
            float height,
            float tickDelta,
            float foamPhaseOffset
    ) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        MatrixStack.Entry entry = matrices.peek();

        float x0 = BLOCK_INSET;
        float x1 = 1.0F - BLOCK_INSET;
        float z0 = BLOCK_INSET;
        float z1 = 1.0F - BLOCK_INSET;
        float y0 = BLOCK_INSET;
        float y1 = Math.max(y0 + 0.01F, height);

        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        int waterColor = entity.isBloodwakeStyle() ? 0x9E0719 : 0x3F76E4;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!entity.isBloodwakeStyle() && client.world != null) {
            waterColor = BiomeColors.getWaterColor(client.world, BlockPos.ofFloored(entitySafePos(client)));
        }
        float r = ((waterColor >> 16) & 0xFF) / 255.0F;
        float g = ((waterColor >> 8) & 0xFF) / 255.0F;
        float b = (waterColor & 0xFF) / 255.0F;
        float topR = Math.min(1.0F, r * 1.15F + 0.04F);
        float topG = Math.min(1.0F, g * 1.15F + 0.04F);
        float topB = Math.min(1.0F, b * 1.15F + 0.04F);
        float sideR = r * 0.95F;
        float sideG = g * 0.95F;
        float sideB = b * 0.95F;
        float topA = 0.90F;
        float sideA = 0.82F;

        quadDoubleSided(vc, entry, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, light, topR, topG, topB, topA, 0.0F, 1.0F, 0.0F);
        quadDoubleSided(vc, entry, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, u0, v0, u1, v1, light, sideR, sideG, sideB, sideA, 0.0F, -1.0F, 0.0F);
        quadDoubleSided(vc, entry, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1, light, sideR, sideG, sideB, sideA, 0.0F, 0.0F, -1.0F);
        quadDoubleSided(vc, entry, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, light, sideR, sideG, sideB, sideA, 0.0F, 0.0F, 1.0F);
        quadDoubleSided(vc, entry, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1, light, sideR, sideG, sideB, sideA, -1.0F, 0.0F, 0.0F);
        quadDoubleSided(vc, entry, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1, light, sideR, sideG, sideB, sideA, 1.0F, 0.0F, 0.0F);

        renderFoamCrest(entity, vc, entry, sprite, light, x0, x1, y1, z0, z1,
                tickDelta, foamPhaseOffset);
    }

    private static void renderFoamCrest(
            LivyatanWaveVisualEntity entity,
            VertexConsumer vc,
            MatrixStack.Entry entry,
            Sprite sprite,
            int light,
            float x0,
            float x1,
            float y1,
            float z0,
            float z1,
            float tickDelta,
            float foamPhaseOffset
    ) {
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        float phase = (entity.age + tickDelta + (entity.getId() % 11) * 0.33F
                + foamPhaseOffset) * 0.42F;
        float pulse = 0.55F + 0.45F * (float) Math.sin(phase);
        float foamAlpha = Math.min(0.94F, 0.38F + pulse * 0.28F);
        float sideFoamAlpha = foamAlpha * 0.72F;
        float foamR = entity.isBloodwakeStyle() ? 1.0F : 0.95F;
        float foamG = entity.isBloodwakeStyle() ? 0.12F : 0.98F;
        float foamB = entity.isBloodwakeStyle() ? 0.16F : 1.0F;

        float capY0 = Math.max(BLOCK_INSET, y1 - FOAM_CAP_THICKNESS);
        quadDoubleSided(vc, entry, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, light, foamR, foamG, foamB, foamAlpha, 0.0F, 1.0F, 0.0F);
        quadDoubleSided(vc, entry, x0, capY0, z1, x1, capY0, z1, x1, capY0, z0, x0, capY0, z0, u0, v0, u1, v1, light, foamR, foamG, foamB, sideFoamAlpha * 0.65F, 0.0F, -1.0F, 0.0F);

        float bandY0 = Math.max(BLOCK_INSET, y1 - FOAM_SIDE_BAND_HEIGHT);
        quadDoubleSided(vc, entry, x1, bandY0, z0, x0, bandY0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1, light, foamR, foamG, foamB, sideFoamAlpha, 0.0F, 0.0F, -1.0F);
        quadDoubleSided(vc, entry, x0, bandY0, z1, x1, bandY0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, light, foamR, foamG, foamB, sideFoamAlpha, 0.0F, 0.0F, 1.0F);
        quadDoubleSided(vc, entry, x0, bandY0, z0, x0, bandY0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1, light, foamR, foamG, foamB, sideFoamAlpha * 0.85F, -1.0F, 0.0F, 0.0F);
        quadDoubleSided(vc, entry, x1, bandY0, z1, x1, bandY0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1, light, foamR, foamG, foamB, sideFoamAlpha * 0.85F, 1.0F, 0.0F, 0.0F);
    }

    private static void quadDoubleSided(
            VertexConsumer vc,
            MatrixStack.Entry entry,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            int light,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        vertex(vc, entry, x0, y0, z0, u0, v1, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x1, y1, z1, u1, v1, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x2, y2, z2, u1, v0, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x3, y3, z3, u0, v0, light, r, g, b, a, nx, ny, nz);
        vertex(vc, entry, x3, y3, z3, u0, v0, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x2, y2, z2, u1, v0, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x1, y1, z1, u1, v1, light, r, g, b, a, -nx, -ny, -nz);
        vertex(vc, entry, x0, y0, z0, u0, v1, light, r, g, b, a, -nx, -ny, -nz);
    }

    private static void vertex(
            VertexConsumer vc,
            MatrixStack.Entry entry,
            float x, float y, float z,
            float u, float v,
            int light,
            float r, float g, float b, float a,
            float nx, float ny, float nz
    ) {
        vc.vertex(entry.getPositionMatrix(), x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz).next();
    }

    private static Vec3d entitySafePos(MinecraftClient client) {
        return client.player != null ? client.player.getPos() : Vec3d.ZERO;
    }
}
