package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.CaelestisBreachVisualEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CaelestisBreachVisualEntityRenderer
        extends EntityRenderer<CaelestisBreachVisualEntity> {

    private static final int SEGMENTS = 96;
    private static final int TERRAIN_CIRCLE_SEGMENTS = 128;
    private static final int TERRAIN_REFRESH_TICKS = 20;
    private static final int TERRAIN_CACHE_GRACE_TICKS = 40;
    private static final float CORE_HALF_WIDTH = 0.11F;
    private static final float VEIL_HEIGHT = 2.8F;
    private static final float TERRAIN_FACE_OFFSET = 0.006F;
    private static final float TERRAIN_DETAIL_OFFSET = 0.001F;
    private static final float TERRAIN_WASH_OFFSET = 0.002F;
    private static final float TERRAIN_EDGE_OVERLAP = 0.008F;
    private static final float SURFACE_EPSILON = 0.001F;
    private static final int TERRAIN_RED = 232;
    private static final int TERRAIN_GREEN = 205;
    private static final int TERRAIN_BLUE = 255;
    private static final int TERRAIN_LIGHT_FLOOR = 8;
    private static final int WASH_RED = 110;
    private static final int WASH_GREEN = 35;
    private static final int WASH_BLUE = 160;
    private static final int WASH_ALPHA = 42;

    private static final Identifier SCULK_TEXTURE = Identifier.ofVanilla("block/sculk");
    private static final Identifier CATALYST_TOP_TEXTURE = Identifier.ofVanilla("block/sculk_catalyst_top");
    private static final Identifier CATALYST_SIDE_TEXTURE = Identifier.ofVanilla("block/sculk_catalyst_side");
    private static final Identifier SHRIEKER_TOP_TEXTURE = Identifier.ofVanilla("block/sculk_shrieker_top");
    private static final Identifier SHRIEKER_INNER_TOP_TEXTURE =
            Identifier.ofVanilla("block/sculk_shrieker_inner_top");
    private static final Identifier SHRIEKER_SIDE_TEXTURE = Identifier.ofVanilla("block/sculk_shrieker_side");
    private static final Identifier SENSOR_TOP_TEXTURE = Identifier.ofVanilla("block/sculk_sensor_top");
    private static final Identifier SENSOR_SIDE_TEXTURE = Identifier.ofVanilla("block/sculk_sensor_side");
    private static final Identifier WHITE_TEXTURE =
            Identifier.ofVanilla("textures/misc/white.png");

    private final Map<UUID, TerrainCache> terrainCaches = new HashMap<>();

    public CaelestisBreachVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CaelestisBreachVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(CaelestisBreachVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        float reach = Math.max(1.0F, entity.getMaxRadius() + 2.0F);
        float verticalReach = Math.max(2.0F, entity.getVerticalRange() + 2.0F);
        Box visualBounds = new Box(
                entity.getX() - reach,
                entity.getY() - verticalReach,
                entity.getZ() - reach,
                entity.getX() + reach,
                entity.getY() + verticalReach + VEIL_HEIGHT,
                entity.getZ() + reach
        );
        return frustum.isVisible(visualBounds);
    }

    @Override
    public void render(CaelestisBreachVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!Config.general.enableModernFieldEffects) {
            terrainCaches.remove(entity.getUuid());
            return;
        }
        if (entity.getRadius() <= 0.05F) {
            terrainCaches.remove(entity.getUuid());
            return;
        }

        float radius = entity.getRadius();
        renderSculkTerrain(entity, matrices.peek(), vertexConsumers, radius);

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        VertexConsumer veilVertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(WHITE_TEXTURE));
        float time = entity.age + tickDelta;
        float pulse = 0.5F + 0.5F * MathHelper.sin(time * 0.18F);
        boolean collapsing = entity.getPhase() == CaelestisBreachVisualEntity.PHASE_COLLAPSING;

        int coreRed = collapsing ? 218 : 126;
        int coreGreen = collapsing ? 20 : 26;
        int coreBlue = collapsing ? 70 : 182;
        int edgeRed = collapsing ? 255 : 176;
        int edgeGreen = collapsing ? 34 : 20;
        int edgeBlue = collapsing ? 56 : 116;

        drawTerrainBand(entity, vertices, matrices.peek(), radius, CORE_HALF_WIDTH,
                coreRed, coreGreen, coreBlue, 155 + (int) (pulse * 55.0F), false, 0);
        drawTerrainBand(entity, vertices, matrices.peek(), Math.max(0.1F, radius - 0.42F), 0.055F,
                edgeRed, edgeGreen, edgeBlue, 65 + (int) (pulse * 35.0F), true, 5);
        drawTerrainBand(entity, vertices, matrices.peek(), radius + 0.34F, 0.05F,
                74, 8, 104, 50 + (int) ((1.0F - pulse) * 30.0F), true, 9);

        float rippleProgress = Math.floorMod(entity.age, 34) / 34.0F;
        float rippleRadius = collapsing
                ? MathHelper.lerp(rippleProgress, radius + 1.1F, Math.max(0.1F, radius - 1.2F))
                : MathHelper.lerp(rippleProgress, Math.max(0.1F, radius - 1.2F), radius + 0.75F);
        drawTerrainBand(entity, vertices, matrices.peek(), rippleRadius, 0.04F,
                edgeRed, edgeGreen, edgeBlue,
                MathHelper.clamp((int) (88.0F * (1.0F - rippleProgress)), 0, 88), true, 13);

        drawBoundaryVeil(entity, veilVertices, matrices.peek(), radius, time,
                edgeRed, edgeGreen, edgeBlue);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void renderSculkTerrain(CaelestisBreachVisualEntity entity, MatrixStack.Entry matrices,
                                    VertexConsumerProvider vertexConsumers, float radius) {
        TerrainCache cache = getTerrainCache(entity);
        if (cache.faces.isEmpty()) {
            return;
        }

        SpriteSet sprites = SpriteSet.load();
        VertexConsumer texturedVertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        VertexConsumer washVertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucent(WHITE_TEXTURE));
        double minY = entity.getY() - entity.getVerticalRange();
        double maxY = entity.getY() + entity.getVerticalRange();

        for (TerrainFace face : cache.faces) {
            if (face.direction.getAxis() == Direction.Axis.Y) {
                drawHorizontalFace(entity, matrices, texturedVertices, washVertices,
                        cache, sprites, face, radius, minY, maxY);
            } else {
                drawVerticalFace(entity, matrices, texturedVertices, washVertices,
                        cache, sprites, face, radius, minY, maxY);
            }
        }
    }

    private TerrainCache getTerrainCache(CaelestisBreachVisualEntity entity) {
        World world = entity.getWorld();
        long now = world.getTime();
        terrainCaches.entrySet().removeIf(entry ->
                entry.getValue().world != world
                        || now - entry.getValue().lastSeenTick > TERRAIN_CACHE_GRACE_TICKS);

        UUID id = entity.getUuid();
        int centerX = MathHelper.floor(entity.getX());
        int centerY = MathHelper.floor(entity.getY());
        int centerZ = MathHelper.floor(entity.getZ());
        int maxRadius = MathHelper.ceil(entity.getMaxRadius()) + 2;
        float verticalRange = entity.getVerticalRange();
        TerrainCache cache = terrainCaches.get(id);
        boolean rebuild = cache == null
                || cache.world != world
                || cache.centerX != centerX
                || cache.centerY != centerY
                || cache.centerZ != centerZ
                || cache.maxRadius != maxRadius
                || Float.compare(cache.verticalRange, verticalRange) != 0
                || now - cache.lastRefreshTick >= TERRAIN_REFRESH_TICKS;
        if (rebuild) {
            cache = buildTerrainCache(
                    world, centerX, centerY, centerZ, maxRadius, verticalRange, now);
            terrainCaches.put(id, cache);
        }
        cache.lastSeenTick = now;
        return cache;
    }

    private static TerrainCache buildTerrainCache(World world, int centerX, int centerY,
                                                  int centerZ, int maxRadius,
                                                  float verticalRange, long now) {
        List<TerrainFace> faces = new ArrayList<>();
        Map<FaceKey, TerrainFace> facesByKey = new HashMap<>();
        int minBlockY = Math.max(world.getBottomY() + 1,
                MathHelper.floor(centerY - verticalRange) - 1);
        int maxBlockY = Math.min(world.getTopY() - 1,
                MathHelper.ceil(centerY + verticalRange) + 1);
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int x = centerX - maxRadius; x <= centerX + maxRadius; x++) {
            for (int z = centerZ - maxRadius; z <= centerZ + maxRadius; z++) {
                for (int y = minBlockY; y <= maxBlockY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = world.getBlockState(cursor);
                    if (!isTerrainStateEligible(world, cursor, state)) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(world, cursor);
                    if (shape.getBoundingBoxes().size() != 1) {
                        continue;
                    }
                    Box bounds = shape.getBoundingBox();
                    BlockPos blockPos = cursor.toImmutable();
                    int hash = coordinateHash(x, y, z);
                    SculkVariant variant = SculkVariant.fromHash(hash);
                    int uvTransform = Math.floorMod(hash >>> 8, 8);

                    for (Direction direction : Direction.values()) {
                        if (!Block.isFaceFullSquare(shape, direction)) {
                            continue;
                        }
                        BlockPos neighborPos = blockPos.offset(direction);
                        if (!Block.shouldDrawSide(
                                state, world, blockPos, direction, neighborPos)) {
                            continue;
                        }

                        double plane = facePlane(x, y, z, bounds, direction);
                        TerrainFace face = new TerrainFace(
                                x, y, z, plane, direction, variant, uvTransform,
                                state, blockPos);
                        faces.add(face);
                        facesByKey.put(new FaceKey(x, y, z, direction), face);
                    }
                }
            }
        }
        return new TerrainCache(world, centerX, centerY, centerZ, maxRadius,
                verticalRange, now, faces, facesByKey);
    }

    private static boolean isTerrainStateEligible(World world, BlockPos pos, BlockState state) {
        if (state.getRenderType() != BlockRenderType.MODEL
                || !state.getFluidState().isEmpty()
                || state.isIn(BlockTags.LEAVES)) {
            return false;
        }
        VoxelShape shape = state.getCollisionShape(world, pos);
        return !shape.isEmpty();
    }

    private static double facePlane(int x, int y, int z, Box bounds, Direction direction) {
        return switch (direction) {
            case UP -> y + bounds.maxY;
            case DOWN -> y + bounds.minY;
            case NORTH -> z + bounds.minZ;
            case SOUTH -> z + bounds.maxZ;
            case WEST -> x + bounds.minX;
            case EAST -> x + bounds.maxX;
        };
    }

    private static void drawHorizontalFace(CaelestisBreachVisualEntity entity,
                                           MatrixStack.Entry matrices,
                                           VertexConsumer texturedVertices,
                                           VertexConsumer washVertices,
                                           TerrainCache cache, SpriteSet sprites,
                                           TerrainFace face, float radius,
                                           double minY, double maxY) {
        if (face.plane < minY - SURFACE_EPSILON || face.plane > maxY + SURFACE_EPSILON) {
            return;
        }

        double x0 = face.x;
        double x1 = face.x + 1.0;
        double z0 = face.z;
        double z1 = face.z + 1.0;
        if (!hasCoplanarNeighbor(cache, face, Direction.WEST)) {
            x0 -= TERRAIN_EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.EAST)) {
            x1 += TERRAIN_EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.NORTH)) {
            z0 -= TERRAIN_EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.SOUTH)) {
            z1 += TERRAIN_EDGE_OVERLAP;
        }

        List<TerrainPoint> polygon = clipHorizontalRectangle(
                entity, x0, x1, z0, z1, radius);
        if (polygon.size() < 3) {
            return;
        }

        Sprite foundation = face.direction == Direction.UP
                ? sprites.topFoundation(face.variant)
                : sprites.sculk;
        Sprite detail = face.direction == Direction.UP
                ? sprites.topDetail(face.variant)
                : null;
        drawHorizontalTextureLayer(entity, matrices, texturedVertices, face,
                polygon, foundation, TERRAIN_FACE_OFFSET);
        if (detail != null) {
            drawHorizontalTextureLayer(entity, matrices, texturedVertices, face,
                    polygon, detail, TERRAIN_FACE_OFFSET + TERRAIN_DETAIL_OFFSET);
        }
        drawHorizontalWashLayer(entity, matrices, washVertices, face, polygon,
                TERRAIN_FACE_OFFSET + TERRAIN_WASH_OFFSET);
    }

    private static List<TerrainPoint> clipHorizontalRectangle(
            CaelestisBreachVisualEntity entity,
            double worldX0, double worldX1, double worldZ0, double worldZ1,
            float radius) {
        double minX = worldX0 - entity.getX();
        double maxX = worldX1 - entity.getX();
        double minZ = worldZ0 - entity.getZ();
        double maxZ = worldZ1 - entity.getZ();
        double radiusSquared = radius * radius;
        double closestX = MathHelper.clamp(0.0, minX, maxX);
        double closestZ = MathHelper.clamp(0.0, minZ, maxZ);
        if (closestX * closestX + closestZ * closestZ > radiusSquared) {
            return List.of();
        }

        List<TerrainPoint> polygon = new ArrayList<>(List.of(
                new TerrainPoint(minX, minZ),
                new TerrainPoint(maxX, minZ),
                new TerrainPoint(maxX, maxZ),
                new TerrainPoint(minX, maxZ)
        ));
        double farthestSquared = Math.max(minX * minX, maxX * maxX)
                + Math.max(minZ * minZ, maxZ * maxZ);
        if (farthestSquared <= radiusSquared) {
            return polygon;
        }

        for (int i = 0; i < TERRAIN_CIRCLE_SEGMENTS && !polygon.isEmpty(); i++) {
            double angle0 = Math.PI * 2.0 * i / TERRAIN_CIRCLE_SEGMENTS;
            double angle1 = Math.PI * 2.0 * (i + 1) / TERRAIN_CIRCLE_SEGMENTS;
            TerrainPoint edgeStart = new TerrainPoint(
                    Math.cos(angle0) * radius, Math.sin(angle0) * radius);
            TerrainPoint edgeEnd = new TerrainPoint(
                    Math.cos(angle1) * radius, Math.sin(angle1) * radius);
            polygon = clipPolygon(polygon, edgeStart, edgeEnd);
        }
        return polygon;
    }

    private static void drawHorizontalTextureLayer(
            CaelestisBreachVisualEntity entity, MatrixStack.Entry matrices,
            VertexConsumer vertices, TerrainFace face, List<TerrainPoint> polygon,
            Sprite sprite, float normalOffset) {
        TerrainPoint center = polygonCenter(polygon);
        float localY = (float) (face.plane - entity.getY()
                + face.direction.getOffsetY() * normalOffset);
        int light = terrainLight(entity, face);
        for (int i = 0; i < polygon.size(); i++) {
            TerrainPoint first = polygon.get(i);
            TerrainPoint second = polygon.get((i + 1) % polygon.size());
            putHorizontalTextureVertex(entity, matrices, vertices, face, sprite,
                    center, localY, light);
            putHorizontalTextureVertex(entity, matrices, vertices, face, sprite,
                    first, localY, light);
            putHorizontalTextureVertex(entity, matrices, vertices, face, sprite,
                    second, localY, light);
            putHorizontalTextureVertex(entity, matrices, vertices, face, sprite,
                    second, localY, light);
        }
    }

    private static void drawHorizontalWashLayer(
            CaelestisBreachVisualEntity entity, MatrixStack.Entry matrices,
            VertexConsumer vertices, TerrainFace face, List<TerrainPoint> polygon,
            float normalOffset) {
        TerrainPoint center = polygonCenter(polygon);
        float localY = (float) (face.plane - entity.getY()
                + face.direction.getOffsetY() * normalOffset);
        int light = terrainLight(entity, face);
        for (int i = 0; i < polygon.size(); i++) {
            TerrainPoint first = polygon.get(i);
            TerrainPoint second = polygon.get((i + 1) % polygon.size());
            putWashVertex(matrices, vertices, center.x, localY, center.z,
                    light, face.direction);
            putWashVertex(matrices, vertices, first.x, localY, first.z,
                    light, face.direction);
            putWashVertex(matrices, vertices, second.x, localY, second.z,
                    light, face.direction);
            putWashVertex(matrices, vertices, second.x, localY, second.z,
                    light, face.direction);
        }
    }

    private static TerrainPoint polygonCenter(List<TerrainPoint> polygon) {
        double x = 0.0;
        double z = 0.0;
        for (TerrainPoint point : polygon) {
            x += point.x;
            z += point.z;
        }
        return new TerrainPoint(x / polygon.size(), z / polygon.size());
    }

    private static void putHorizontalTextureVertex(
            CaelestisBreachVisualEntity entity, MatrixStack.Entry matrices,
            VertexConsumer vertices, TerrainFace face, Sprite sprite,
            TerrainPoint point, float localY, int light) {
        double worldX = point.x + entity.getX();
        double worldZ = point.z + entity.getZ();
        double textureU = MathHelper.clamp(worldX - face.x, 0.0, 1.0);
        double textureV = MathHelper.clamp(worldZ - face.z, 0.0, 1.0);
        TextureCoordinates uv = transformTopUv(textureU, textureV, face.uvTransform);
        putTextureVertex(matrices, vertices, sprite,
                point.x, localY, point.z, uv.u, uv.v, light, face.direction);
    }

    private static void drawVerticalFace(CaelestisBreachVisualEntity entity,
                                         MatrixStack.Entry matrices,
                                         VertexConsumer texturedVertices,
                                         VertexConsumer washVertices,
                                         TerrainCache cache, SpriteSet sprites,
                                         TerrainFace face, float radius,
                                         double minY, double maxY) {
        boolean xAxis = face.direction.getAxis() == Direction.Axis.Z;
        Direction lowDirection = xAxis ? Direction.WEST : Direction.NORTH;
        Direction highDirection = xAxis ? Direction.EAST : Direction.SOUTH;
        double variableStart = xAxis ? face.x : face.z;
        double variableEnd = variableStart + 1.0;
        if (!hasCoplanarNeighbor(cache, face, lowDirection)) {
            variableStart -= TERRAIN_EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, highDirection)) {
            variableEnd += TERRAIN_EDGE_OVERLAP;
        }

        double y0 = face.y;
        double y1 = face.y + 1.0;
        if (!hasCoplanarNeighbor(cache, face, Direction.DOWN)) {
            y0 -= TERRAIN_EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.UP)) {
            y1 += TERRAIN_EDGE_OVERLAP;
        }
        y0 = Math.max(y0, minY);
        y1 = Math.min(y1, maxY);
        if (y1 - y0 <= 1.0E-5) {
            return;
        }

        double fixedCenter = xAxis ? entity.getZ() : entity.getX();
        double fixedDistance = face.plane - fixedCenter;
        double radiusSquared = radius * radius;
        if (fixedDistance * fixedDistance > radiusSquared) {
            return;
        }
        double span = Math.sqrt(Math.max(0.0,
                radiusSquared - fixedDistance * fixedDistance));
        double variableCenter = xAxis ? entity.getX() : entity.getZ();
        double clippedStart = Math.max(variableStart, variableCenter - span);
        double clippedEnd = Math.min(variableEnd, variableCenter + span);
        if (clippedEnd - clippedStart <= 1.0E-5) {
            return;
        }

        Sprite foundation = sprites.sideFoundation(face.variant);
        Sprite detail = sprites.sideDetail(face.variant);
        drawVerticalTextureLayer(entity, matrices, texturedVertices, face, foundation,
                xAxis, clippedStart, clippedEnd, y0, y1, TERRAIN_FACE_OFFSET);
        if (detail != null) {
            drawVerticalTextureLayer(entity, matrices, texturedVertices, face, detail,
                    xAxis, clippedStart, clippedEnd, y0, y1,
                    TERRAIN_FACE_OFFSET + TERRAIN_DETAIL_OFFSET);
        }
        drawVerticalWashLayer(entity, matrices, washVertices, face,
                xAxis, clippedStart, clippedEnd, y0, y1,
                TERRAIN_FACE_OFFSET + TERRAIN_WASH_OFFSET);
    }

    private static void drawVerticalTextureLayer(
            CaelestisBreachVisualEntity entity, MatrixStack.Entry matrices,
            VertexConsumer vertices, TerrainFace face, Sprite sprite, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float normalOffset) {
        VerticalQuad quad = verticalQuad(
                entity, face, xAxis, variableStart, variableEnd, y0, y1, normalOffset);
        int light = terrainLight(entity, face);
        double u0 = MathHelper.clamp(
                variableStart - (xAxis ? face.x : face.z), 0.0, 1.0);
        double u1 = MathHelper.clamp(
                variableEnd - (xAxis ? face.x : face.z), 0.0, 1.0);
        if ((face.uvTransform & 4) != 0) {
            double flippedU0 = 1.0 - u1;
            u1 = 1.0 - u0;
            u0 = flippedU0;
        }
        double v0 = MathHelper.clamp(1.0 - (y0 - face.y), 0.0, 1.0);
        double v1 = MathHelper.clamp(1.0 - (y1 - face.y), 0.0, 1.0);

        putTextureVertex(matrices, vertices, sprite,
                quad.x0, quad.y0, quad.z0, u0, v0, light, face.direction);
        putTextureVertex(matrices, vertices, sprite,
                quad.x1, quad.y0, quad.z1, u1, v0, light, face.direction);
        putTextureVertex(matrices, vertices, sprite,
                quad.x1, quad.y1, quad.z1, u1, v1, light, face.direction);
        putTextureVertex(matrices, vertices, sprite,
                quad.x0, quad.y1, quad.z0, u0, v1, light, face.direction);
    }

    private static void drawVerticalWashLayer(
            CaelestisBreachVisualEntity entity, MatrixStack.Entry matrices,
            VertexConsumer vertices, TerrainFace face, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float normalOffset) {
        VerticalQuad quad = verticalQuad(
                entity, face, xAxis, variableStart, variableEnd, y0, y1, normalOffset);
        int light = terrainLight(entity, face);
        putWashVertex(matrices, vertices, quad.x0, quad.y0, quad.z0,
                light, face.direction);
        putWashVertex(matrices, vertices, quad.x1, quad.y0, quad.z1,
                light, face.direction);
        putWashVertex(matrices, vertices, quad.x1, quad.y1, quad.z1,
                light, face.direction);
        putWashVertex(matrices, vertices, quad.x0, quad.y1, quad.z0,
                light, face.direction);
    }

    private static VerticalQuad verticalQuad(
            CaelestisBreachVisualEntity entity, TerrainFace face, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float normalOffset) {
        double fixed = face.plane
                + (xAxis ? face.direction.getOffsetZ() : face.direction.getOffsetX())
                * normalOffset;
        double x0;
        double x1;
        double z0;
        double z1;
        if (xAxis) {
            x0 = variableStart - entity.getX();
            x1 = variableEnd - entity.getX();
            z0 = z1 = fixed - entity.getZ();
        } else {
            x0 = x1 = fixed - entity.getX();
            z0 = variableStart - entity.getZ();
            z1 = variableEnd - entity.getZ();
        }
        return new VerticalQuad(
                x0, (float) (y0 - entity.getY()), z0,
                x1, (float) (y1 - entity.getY()), z1);
    }

    private static boolean hasCoplanarNeighbor(
            TerrainCache cache, TerrainFace face, Direction offset) {
        TerrainFace neighbor = cache.facesByKey.get(new FaceKey(
                face.x + offset.getOffsetX(),
                face.y + offset.getOffsetY(),
                face.z + offset.getOffsetZ(),
                face.direction));
        return neighbor != null && Math.abs(neighbor.plane - face.plane) <= SURFACE_EPSILON;
    }

    private static List<TerrainPoint> clipPolygon(List<TerrainPoint> input,
                                                   TerrainPoint edgeStart, TerrainPoint edgeEnd) {
        if (input.isEmpty()) {
            return input;
        }
        int insideCount = 0;
        for (TerrainPoint point : input) {
            if (insideEdge(point, edgeStart, edgeEnd)) {
                insideCount++;
            }
        }
        if (insideCount == input.size()) {
            return input;
        }
        if (insideCount == 0) {
            return List.of();
        }

        List<TerrainPoint> output = new ArrayList<>(input.size() + 1);
        TerrainPoint previous = input.get(input.size() - 1);
        boolean previousInside = insideEdge(previous, edgeStart, edgeEnd);
        for (TerrainPoint current : input) {
            boolean currentInside = insideEdge(current, edgeStart, edgeEnd);
            if (currentInside != previousInside) {
                output.add(edgeIntersection(previous, current, edgeStart, edgeEnd));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean insideEdge(TerrainPoint point, TerrainPoint start, TerrainPoint end) {
        return cross(end.x - start.x, end.z - start.z,
                point.x - start.x, point.z - start.z) >= -1.0E-7;
    }

    private static TerrainPoint edgeIntersection(TerrainPoint lineStart, TerrainPoint lineEnd,
                                                  TerrainPoint clipStart, TerrainPoint clipEnd) {
        double lineX = lineEnd.x - lineStart.x;
        double lineZ = lineEnd.z - lineStart.z;
        double clipX = clipEnd.x - clipStart.x;
        double clipZ = clipEnd.z - clipStart.z;
        double denominator = cross(lineX, lineZ, clipX, clipZ);
        if (Math.abs(denominator) < 1.0E-9) {
            return lineEnd;
        }
        double t = cross(clipStart.x - lineStart.x, clipStart.z - lineStart.z,
                clipX, clipZ) / denominator;
        return new TerrainPoint(lineStart.x + lineX * t, lineStart.z + lineZ * t);
    }

    private static double cross(double ax, double az, double bx, double bz) {
        return ax * bz - az * bx;
    }

    private static TextureCoordinates transformTopUv(double u, double v, int transform) {
        if ((transform & 4) != 0) {
            u = 1.0 - u;
        }
        return switch (transform & 3) {
            case 1 -> new TextureCoordinates(v, 1.0 - u);
            case 2 -> new TextureCoordinates(1.0 - u, 1.0 - v);
            case 3 -> new TextureCoordinates(1.0 - v, u);
            default -> new TextureCoordinates(u, v);
        };
    }

    private static void putTextureVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices, Sprite sprite,
            double x, float y, double z, double u, double v, int light,
            Direction direction) {
        vertices.vertex(matrices, (float) x, y, (float) z)
                .color(TERRAIN_RED, TERRAIN_GREEN, TERRAIN_BLUE, 255)
                .texture(sprite.getFrameU((float) u), sprite.getFrameV((float) v))
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices, direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ());
    }

    private static void putWashVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices,
            double x, float y, double z, int light, Direction direction) {
        vertices.vertex(matrices, (float) x, y, (float) z)
                .color(WASH_RED, WASH_GREEN, WASH_BLUE, WASH_ALPHA)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices, direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ());
    }

    private static int terrainLight(
            CaelestisBreachVisualEntity entity, TerrainFace face) {
        int light = WorldRenderer.getLightmapCoordinates(
                entity.getWorld(), face.state, face.blockPos);
        int blockLight = Math.max(
                LightmapTextureManager.getBlockLightCoordinates(light),
                TERRAIN_LIGHT_FLOOR);
        int skyLight = Math.max(
                LightmapTextureManager.getSkyLightCoordinates(light),
                TERRAIN_LIGHT_FLOOR);
        return LightmapTextureManager.pack(blockLight, skyLight);
    }

    private static int coordinateHash(int x, int y, int z) {
        int hash = x * 0x1f1f1f1f ^ y * 0x6c8e9cf5 ^ z * 0x5f356495;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        return hash ^ hash >>> 16;
    }

    private static void drawTerrainBand(CaelestisBreachVisualEntity entity, VertexConsumer vertices,
                                        MatrixStack.Entry matrices, float radius, float halfWidth,
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

            vertices.vertex(matrices, ix, y0, iz).color(red, green, blue, alpha);
            vertices.vertex(matrices, ox, y0, oz).color(red, green, blue, alpha);
            vertices.vertex(matrices, nox, y1, noz).color(red, green, blue, alpha);
            vertices.vertex(matrices, nix, y1, niz).color(red, green, blue, alpha);
        }
    }

    private static void drawBoundaryVeil(CaelestisBreachVisualEntity entity, VertexConsumer vertices,
                                         MatrixStack.Entry matrices, float radius, float time,
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
            float normalX0 = (float) Math.cos(angle);
            float normalZ0 = (float) Math.sin(angle);
            float normalX1 = (float) Math.cos(next);
            float normalZ1 = (float) Math.sin(next);

            putVeilVertex(matrices, vertices, x0, y0, z0,
                    red, green, blue, alpha, normalX0, normalZ0);
            putVeilVertex(matrices, vertices, x1, y1, z1,
                    red, green, blue, alpha, normalX1, normalZ1);
            putVeilVertex(matrices, vertices, x1, y1 + h1, z1,
                    red, green, blue, 0, normalX1, normalZ1);
            putVeilVertex(matrices, vertices, x0, y0 + h0, z0,
                    red, green, blue, 0, normalX0, normalZ0);
        }
    }

    private static void putVeilVertex(MatrixStack.Entry matrices, VertexConsumer vertices,
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

    private static float groundOffset(CaelestisBreachVisualEntity entity, float localX, float localZ) {
        int x = MathHelper.floor(entity.getX() + localX);
        int z = MathHelper.floor(entity.getZ() + localZ);
        int verticalRange = MathHelper.ceil(entity.getVerticalRange());
        int start = Math.min(entity.getWorld().getTopY() - 1,
                MathHelper.floor(entity.getY()) + verticalRange);
        int end = Math.max(entity.getWorld().getBottomY() + 1,
                MathHelper.floor(entity.getY()) - verticalRange);
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

    private enum SculkVariant {
        SCULK,
        CATALYST,
        SHRIEKER,
        SENSOR;

        private static SculkVariant fromHash(int hash) {
            int roll = Math.floorMod(hash, 100);
            if (roll < 65) {
                return SCULK;
            }
            if (roll < 82) {
                return CATALYST;
            }
            if (roll < 92) {
                return SHRIEKER;
            }
            return SENSOR;
        }
    }

    private record SpriteSet(Sprite sculk, Sprite catalystTop, Sprite catalystSide,
                             Sprite shriekerTop, Sprite shriekerInnerTop, Sprite shriekerSide,
                             Sprite sensorTop, Sprite sensorSide) {
        private static SpriteSet load() {
            BakedModelManager models = MinecraftClient.getInstance().getBakedModelManager();
            SpriteAtlasTexture atlas = models.getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            return new SpriteSet(
                    atlas.getSprite(SCULK_TEXTURE),
                    atlas.getSprite(CATALYST_TOP_TEXTURE),
                    atlas.getSprite(CATALYST_SIDE_TEXTURE),
                    atlas.getSprite(SHRIEKER_TOP_TEXTURE),
                    atlas.getSprite(SHRIEKER_INNER_TOP_TEXTURE),
                    atlas.getSprite(SHRIEKER_SIDE_TEXTURE),
                    atlas.getSprite(SENSOR_TOP_TEXTURE),
                    atlas.getSprite(SENSOR_SIDE_TEXTURE)
            );
        }

        private Sprite topFoundation(SculkVariant variant) {
            return switch (variant) {
                case CATALYST -> catalystTop;
                case SHRIEKER -> shriekerInnerTop;
                case SENSOR -> sensorTop;
                default -> sculk;
            };
        }

        private Sprite topDetail(SculkVariant variant) {
            return variant == SculkVariant.SHRIEKER ? shriekerTop : null;
        }

        private Sprite sideFoundation(SculkVariant variant) {
            return switch (variant) {
                case CATALYST -> catalystSide;
                default -> sculk;
            };
        }

        private Sprite sideDetail(SculkVariant variant) {
            return switch (variant) {
                case SHRIEKER -> shriekerSide;
                case SENSOR -> sensorSide;
                default -> null;
            };
        }
    }

    private static final class TerrainFace {
        private final int x;
        private final int y;
        private final int z;
        private final double plane;
        private final Direction direction;
        private final SculkVariant variant;
        private final int uvTransform;
        private final BlockState state;
        private final BlockPos blockPos;

        private TerrainFace(int x, int y, int z, double plane, Direction direction,
                            SculkVariant variant, int uvTransform,
                            BlockState state, BlockPos blockPos) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.plane = plane;
            this.direction = direction;
            this.variant = variant;
            this.uvTransform = uvTransform;
            this.state = state;
            this.blockPos = blockPos;
        }
    }

    private static final class TerrainCache {
        private final World world;
        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final int maxRadius;
        private final float verticalRange;
        private final long lastRefreshTick;
        private final List<TerrainFace> faces;
        private final Map<FaceKey, TerrainFace> facesByKey;
        private long lastSeenTick;

        private TerrainCache(World world, int centerX, int centerY, int centerZ,
                             int maxRadius, float verticalRange, long now,
                             List<TerrainFace> faces,
                             Map<FaceKey, TerrainFace> facesByKey) {
            this.world = world;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.maxRadius = maxRadius;
            this.verticalRange = verticalRange;
            this.lastRefreshTick = now;
            this.lastSeenTick = now;
            this.faces = faces;
            this.facesByKey = facesByKey;
        }
    }

    private record FaceKey(int x, int y, int z, Direction direction) {
    }

    private record TerrainPoint(double x, double z) {
    }

    private record TextureCoordinates(double u, double v) {
    }

    private record VerticalQuad(double x0, float y0, double z0,
                                double x1, float y1, double z1) {
    }
}
