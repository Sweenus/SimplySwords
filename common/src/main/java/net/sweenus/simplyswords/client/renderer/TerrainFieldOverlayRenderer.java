package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a circular, visual-only block texture replacement over exposed terrain
 * faces. The world remains untouched; curved clipping also supports partial blocks.
 */
public final class TerrainFieldOverlayRenderer {

    private static final int CIRCLE_SEGMENTS = 128;
    private static final int CACHE_GRACE_TICKS = 40;
    private static final float FACE_OFFSET = 0.006F;
    private static final float DETAIL_OFFSET = 0.001F;
    private static final float WASH_OFFSET = 0.002F;
    private static final float EDGE_OVERLAP = 0.008F;
    private static final float FLUID_OFFSET = 0.012F;
    private static final float FLUID_EDGE_OVERLAP = 0.003F;
    private static final float SURFACE_EPSILON = 0.001F;
    private static final Identifier WHITE_TEXTURE =
            new Identifier("minecraft", "textures/misc/white.png");

    public static final Palette CAELESTIS = new Palette(
            List.of(
                    new Variant(65,
                            new Identifier("minecraft", "block/sculk"), null,
                            new Identifier("minecraft", "block/sculk"), null),
                    new Variant(17,
                            new Identifier("minecraft", "block/sculk_catalyst_top"), null,
                            new Identifier("minecraft", "block/sculk_catalyst_side"), null),
                    new Variant(10,
                            new Identifier("minecraft", "block/sculk_shrieker_inner_top"),
                            new Identifier("minecraft", "block/sculk_shrieker_top"),
                            new Identifier("minecraft", "block/sculk"),
                            new Identifier("minecraft", "block/sculk_shrieker_side")),
                    new Variant(8,
                            new Identifier("minecraft", "block/sculk_sensor_top"), null,
                            new Identifier("minecraft", "block/sculk"),
                            new Identifier("minecraft", "block/sculk_sensor_side"))
            ),
            255, 125, 255,
            108, 65, 152, 72,
            8,
            20,
            0
    );

    public static final Palette SOUL_PYRE = new Palette(
            List.of(
                    new Variant(43,
                            new Identifier("minecraft", "block/soul_soil"), null,
                            new Identifier("minecraft", "block/soul_soil"), null),
                    new Variant(22,
                            new Identifier("minecraft", "block/soul_sand"), null,
                            new Identifier("minecraft", "block/soul_sand"), null),
                    new Variant(14,
                            new Identifier("minecraft", "block/basalt_top"), null,
                            new Identifier("minecraft", "block/basalt_side"), null),
                    new Variant(11,
                            new Identifier("minecraft", "block/blackstone"), null,
                            new Identifier("minecraft", "block/blackstone"), null),
                    new Variant(5,
                            new Identifier("minecraft", "block/crying_obsidian"), null,
                            new Identifier("minecraft", "block/crying_obsidian"), null),
                    new Variant(5,
                            new Identifier("minecraft", "block/glowstone"), null,
                            new Identifier("minecraft", "block/glowstone"), null,
                            true)
            ),
            188, 210, 222,
            20, 88, 94, 30,
            7,
            10,
            3,
            new FluidReplacement(
                    new Identifier("minecraft", "block/lava_still"),
                    new Identifier("minecraft", "block/lava_flow")
            )
    );

    private final Map<UUID, TerrainCache> caches = new HashMap<>();

    public void clear(UUID id) {
        this.caches.remove(id);
    }

    public void render(World world, UUID id,
                       double centerX, double centerY, double centerZ,
                       float radius, float maxRadius, float verticalRange,
                       Palette palette, MatrixStack.Entry matrices,
                       VertexConsumerProvider vertexConsumers) {
        if (radius <= 0.05F) {
            clear(id);
            return;
        }
        TerrainCache cache = getCache(
                world, id, centerX, centerY, centerZ, maxRadius, verticalRange, palette);
        if (cache.faces.isEmpty() && cache.fluidSurfaces.isEmpty()) {
            return;
        }

        SpriteSet sprites = SpriteSet.load(palette);
        double minY = centerY - verticalRange;
        double maxY = centerY + verticalRange;
        List<PreparedFace> preparedFaces = new ArrayList<>(cache.faces.size());
        for (TerrainFace face : cache.faces) {
            PreparedFace prepared = face.direction.getAxis() == Direction.Axis.Y
                    ? prepareHorizontalFace(centerX, centerZ, cache, face, radius, minY, maxY)
                    : prepareVerticalFace(centerX, centerZ, cache, face, radius, minY, maxY);
            if (prepared != null) {
                preparedFaces.add(prepared);
            }
        }

        VertexConsumer texturedVertices = vertexConsumers.getBuffer(
                RenderLayer.getCutout());
        for (PreparedFace prepared : preparedFaces) {
            if (prepared instanceof PreparedHorizontalFace horizontal) {
                drawHorizontalTextureFace(
                        centerX, centerY, centerZ, matrices, texturedVertices,
                        sprites, palette, horizontal);
            } else if (prepared instanceof PreparedVerticalFace vertical) {
                drawVerticalTextureFace(
                        centerX, centerY, centerZ, matrices, texturedVertices,
                        sprites, palette, vertical);
            }
        }
        if (sprites.fluidStill != null && sprites.fluidFlow != null) {
            for (FluidSurface surface : cache.fluidSurfaces) {
                drawFluidSurface(
                        centerX, centerY, centerZ,
                        matrices, texturedVertices, sprites,
                        surface, radius, minY, maxY
                );
            }
        }

        VertexConsumer washVertices = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucent(WHITE_TEXTURE));
        for (PreparedFace prepared : preparedFaces) {
            if (prepared instanceof PreparedHorizontalFace horizontal) {
                drawHorizontalWashFace(centerY, matrices, washVertices, palette, horizontal);
            } else if (prepared instanceof PreparedVerticalFace vertical) {
                drawVerticalWashFace(
                        centerX, centerY, centerZ, matrices, washVertices,
                        palette, vertical);
            }
        }
    }

    private TerrainCache getCache(World world, UUID id,
                                  double centerX, double centerY, double centerZ,
                                  float maxRadius, float verticalRange, Palette palette) {
        long now = world.getTime();
        caches.entrySet().removeIf(entry ->
                entry.getValue().world != world
                        || now - entry.getValue().lastSeenTick > CACHE_GRACE_TICKS);

        int requestedX = MathHelper.floor(centerX);
        int requestedY = MathHelper.floor(centerY);
        int requestedZ = MathHelper.floor(centerZ);
        int scanRadius = MathHelper.ceil(maxRadius) + 2 + palette.movementMargin;
        TerrainCache cache = caches.get(id);
        boolean movedOutsideMargin = cache != null
                && (Math.abs(requestedX - cache.centerX) > palette.movementMargin
                || Math.abs(requestedZ - cache.centerZ) > palette.movementMargin
                || Math.abs(requestedY - cache.centerY) > 1);
        boolean rebuild = cache == null
                || cache.world != world
                || movedOutsideMargin
                || cache.scanRadius != scanRadius
                || Float.compare(cache.verticalRange, verticalRange) != 0
                || cache.palette != palette
                || now - cache.lastRefreshTick >= palette.refreshTicks;
        if (rebuild) {
            cache = buildCache(
                    world, requestedX, requestedY, requestedZ,
                    scanRadius, verticalRange, palette, now);
            caches.put(id, cache);
        }
        cache.lastSeenTick = now;
        return cache;
    }

    private static TerrainCache buildCache(World world, int centerX, int centerY,
                                           int centerZ, int scanRadius,
                                           float verticalRange, Palette palette, long now) {
        List<TerrainFace> faces = new ArrayList<>();
        Map<FaceKey, TerrainFace> facesByKey = new HashMap<>();
        List<FluidSurface> fluidSurfaces = new ArrayList<>();
        int minBlockY = Math.max(
                world.getBottomY() + 1,
                MathHelper.floor(centerY - verticalRange) - 1);
        int maxBlockY = Math.min(
                world.getTopY() - 1,
                MathHelper.ceil(centerY + verticalRange) + 1);
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int x = centerX - scanRadius; x <= centerX + scanRadius; x++) {
            for (int z = centerZ - scanRadius; z <= centerZ + scanRadius; z++) {
                for (int y = minBlockY; y <= maxBlockY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = world.getBlockState(cursor);
                    if (palette.fluidReplacement != null
                            && isReplaceableWater(world, cursor, state)) {
                        FluidSurface fluidSurface = buildFluidSurface(
                                world, cursor.toImmutable(), state);
                        if (fluidSurface != null) {
                            fluidSurfaces.add(fluidSurface);
                        }
                    }
                    if (!isEligible(world, cursor, state)) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(world, cursor);
                    if (shape.getBoundingBoxes().size() != 1) {
                        continue;
                    }
                    Box bounds = shape.getBoundingBox();
                    BlockPos blockPos = cursor.toImmutable();
                    int hash = coordinateHash(x, y, z);
                    int variant = palette.variantIndex(hash);
                    int uvTransform = Math.floorMod(hash >>> 8, 8);

                    for (Direction direction : Direction.values()) {
                        if (!Block.isFaceFullSquare(shape, direction)) {
                            continue;
                        }
                        BlockPos neighborPos = blockPos.offset(direction);
                        if (!Block.shouldDrawSide(state, world, blockPos, direction, neighborPos)) {
                            continue;
                        }
                        TerrainFace face = new TerrainFace(
                                world,
                                x, y, z,
                                facePlane(x, y, z, bounds, direction),
                                direction,
                                variant,
                                uvTransform,
                                state,
                                blockPos
                        );
                        faces.add(face);
                        facesByKey.put(new FaceKey(x, y, z, direction), face);
                    }
                }
            }
        }
        return new TerrainCache(
                world, centerX, centerY, centerZ, scanRadius,
                verticalRange, palette, now, faces, facesByKey, fluidSurfaces);
    }

    private static boolean isEligible(World world, BlockPos pos, BlockState state) {
        if (state.getRenderType() != BlockRenderType.MODEL
                || !state.getFluidState().isEmpty()
                || state.isIn(BlockTags.LEAVES)) {
            return false;
        }
        return !state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean isReplaceableWater(
            World world, BlockPos pos, BlockState state) {
        return state.getFluidState().isIn(FluidTags.WATER)
                && state.getCollisionShape(world, pos).isEmpty();
    }

    private static FluidSurface buildFluidSurface(
            World world, BlockPos pos, BlockState state) {
        FluidState fluid = state.getFluidState();
        boolean top = FluidRenderer.shouldRenderSide(
                world,
                pos,
                fluid,
                state,
                Direction.UP,
                world.getFluidState(pos.up())
        );
        int sideMask = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (FluidRenderer.shouldRenderSide(
                    world,
                    pos,
                    fluid,
                    state,
                    direction,
                    world.getFluidState(pos.offset(direction)))) {
                sideMask |= 1 << direction.getHorizontal();
            }
        }
        if (!top && sideMask == 0) {
            return null;
        }

        Vec3d velocity = fluid.getVelocity(world, pos);
        return new FluidSurface(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                fluidCornerHeight(world, pos, 0, 0),
                fluidCornerHeight(world, pos, 0, 1),
                fluidCornerHeight(world, pos, 1, 1),
                fluidCornerHeight(world, pos, 1, 0),
                velocity.x,
                velocity.z,
                top,
                sideMask
        );
    }

    private static float fluidCornerHeight(
            World world, BlockPos origin, int cornerX, int cornerZ) {
        float weightedHeight = 0.0F;
        float totalWeight = 0.0F;
        BlockPos.Mutable sample = new BlockPos.Mutable();
        for (int dx = cornerX - 1; dx <= cornerX; dx++) {
            for (int dz = cornerZ - 1; dz <= cornerZ; dz++) {
                sample.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                BlockState state = world.getBlockState(sample);
                if (!isReplaceableWater(world, sample, state)) {
                    continue;
                }
                if (isReplaceableWater(world, sample.up(),
                        world.getBlockState(sample.up()))) {
                    return 1.0F;
                }
                float height = state.getFluidState().getHeight(world, sample);
                float weight = height >= 0.8F ? 10.0F : 1.0F;
                weightedHeight += height * weight;
                totalWeight += weight;
            }
        }
        if (totalWeight <= 0.0F) {
            return 0.0F;
        }
        return weightedHeight / totalWeight;
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

    private static void drawFluidSurface(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            SpriteSet sprites, FluidSurface surface, float radius,
            double minY, double maxY) {
        if (surface.top) {
            drawFluidTop(
                    centerX, centerY, centerZ,
                    matrices, vertices, sprites, surface,
                    radius, minY, maxY
            );
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if ((surface.sideMask & 1 << direction.getHorizontal()) != 0) {
                drawFluidSide(
                        centerX, centerY, centerZ,
                        matrices, vertices, sprites.fluidFlow,
                        surface, direction, radius, minY, maxY
                );
            }
        }
    }

    private static void drawFluidTop(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            SpriteSet sprites, FluidSurface surface, float radius,
            double minY, double maxY) {
        double lowest = surface.y + Math.min(
                Math.min(surface.northWestHeight, surface.southWestHeight),
                Math.min(surface.southEastHeight, surface.northEastHeight));
        double highest = surface.y + Math.max(
                Math.max(surface.northWestHeight, surface.southWestHeight),
                Math.max(surface.southEastHeight, surface.northEastHeight));
        if (highest < minY - SURFACE_EPSILON
                || lowest > maxY + SURFACE_EPSILON) {
            return;
        }

        List<TerrainPoint> polygon = clipHorizontalRectangle(
                centerX,
                centerZ,
                surface.x - FLUID_EDGE_OVERLAP,
                surface.x + 1.0 + FLUID_EDGE_OVERLAP,
                surface.z - FLUID_EDGE_OVERLAP,
                surface.z + 1.0 + FLUID_EDGE_OVERLAP,
                radius
        );
        if (polygon.size() < 3) {
            return;
        }

        boolean flowing = surface.velocityX * surface.velocityX
                + surface.velocityZ * surface.velocityZ > 1.0E-6;
        Sprite sprite = flowing ? sprites.fluidFlow : sprites.fluidStill;
        TerrainPoint center = polygonCenter(polygon);
        for (int i = 0; i < polygon.size(); i++) {
            TerrainPoint first = polygon.get(i);
            TerrainPoint second = polygon.get((i + 1) % polygon.size());
            TerrainPoint clockwiseFirst = second;
            TerrainPoint clockwiseSecond = first;
            putFluidTopVertex(
                    centerX, centerY, centerZ,
                    matrices, vertices, sprite, surface, center, flowing);
            putFluidTopVertex(
                    centerX, centerY, centerZ,
                    matrices, vertices, sprite, surface, clockwiseFirst, flowing);
            putFluidTopVertex(
                    centerX, centerY, centerZ,
                    matrices, vertices, sprite, surface, clockwiseSecond, flowing);
            putFluidTopVertex(
                    centerX, centerY, centerZ,
                    matrices, vertices, sprite, surface, clockwiseSecond, flowing);
        }
    }

    private static void putFluidTopVertex(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            Sprite sprite, FluidSurface surface,
            TerrainPoint point, boolean flowing) {
        double worldX = point.x + centerX;
        double worldZ = point.z + centerZ;
        double u = MathHelper.clamp(worldX - surface.x, 0.0, 1.0);
        double v = MathHelper.clamp(worldZ - surface.z, 0.0, 1.0);
        float height = fluidHeightAt(surface, u, v);
        TextureCoordinates texture = fluidTextureCoordinates(
                u, v, surface.velocityX, surface.velocityZ, flowing);
        putFluidVertex(
                matrices,
                vertices,
                sprite,
                point.x,
                (float) (surface.y + height - centerY + FLUID_OFFSET),
                point.z,
                texture.u,
                texture.v,
                Direction.UP
        );
    }

    private static float fluidHeightAt(
            FluidSurface surface, double u, double v) {
        float north = MathHelper.lerp(
                (float) u,
                surface.northWestHeight,
                surface.northEastHeight
        );
        float south = MathHelper.lerp(
                (float) u,
                surface.southWestHeight,
                surface.southEastHeight
        );
        return MathHelper.lerp((float) v, north, south);
    }

    private static TextureCoordinates fluidTextureCoordinates(
            double u, double v, double velocityX, double velocityZ,
            boolean flowing) {
        if (!flowing) {
            return new TextureCoordinates(u, v);
        }
        double angle = Math.atan2(velocityZ, velocityX) - Math.PI * 0.5;
        double sine = Math.sin(angle) * 0.35;
        double cosine = Math.cos(angle) * 0.35;
        double centeredU = u - 0.5;
        double centeredV = v - 0.5;
        return new TextureCoordinates(
                0.5 + centeredU * cosine - centeredV * sine,
                0.5 + centeredU * sine + centeredV * cosine
        );
    }

    private static void drawFluidSide(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            Sprite sprite, FluidSurface surface, Direction direction,
            float radius, double minY, double maxY) {
        boolean xAxis = direction.getAxis() == Direction.Axis.Z;
        double variableStart = xAxis ? surface.x : surface.z;
        double variableEnd = variableStart + 1.0;
        double fixed = switch (direction) {
            case NORTH -> surface.z - FLUID_OFFSET;
            case SOUTH -> surface.z + 1.0 + FLUID_OFFSET;
            case WEST -> surface.x - FLUID_OFFSET;
            case EAST -> surface.x + 1.0 + FLUID_OFFSET;
            default -> throw new IllegalArgumentException("Vertical fluid side");
        };

        double fixedCenter = xAxis ? centerZ : centerX;
        double fixedDistance = fixed - fixedCenter;
        double radiusSquared = radius * radius;
        if (fixedDistance * fixedDistance > radiusSquared) {
            return;
        }
        double span = Math.sqrt(Math.max(
                0.0, radiusSquared - fixedDistance * fixedDistance));
        double variableCenter = xAxis ? centerX : centerZ;
        double clippedStart = Math.max(
                variableStart - FLUID_EDGE_OVERLAP,
                variableCenter - span);
        double clippedEnd = Math.min(
                variableEnd + FLUID_EDGE_OVERLAP,
                variableCenter + span);
        if (clippedEnd - clippedStart <= 1.0E-5) {
            return;
        }

        double startU = MathHelper.clamp(
                clippedStart - variableStart, 0.0, 1.0);
        double endU = MathHelper.clamp(
                clippedEnd - variableStart, 0.0, 1.0);
        float startHeight;
        float endHeight;
        if (direction == Direction.NORTH) {
            startHeight = MathHelper.lerp(
                    (float) startU,
                    surface.northWestHeight,
                    surface.northEastHeight);
            endHeight = MathHelper.lerp(
                    (float) endU,
                    surface.northWestHeight,
                    surface.northEastHeight);
        } else if (direction == Direction.SOUTH) {
            startHeight = MathHelper.lerp(
                    (float) startU,
                    surface.southWestHeight,
                    surface.southEastHeight);
            endHeight = MathHelper.lerp(
                    (float) endU,
                    surface.southWestHeight,
                    surface.southEastHeight);
        } else if (direction == Direction.WEST) {
            startHeight = MathHelper.lerp(
                    (float) startU,
                    surface.northWestHeight,
                    surface.southWestHeight);
            endHeight = MathHelper.lerp(
                    (float) endU,
                    surface.northWestHeight,
                    surface.southWestHeight);
        } else {
            startHeight = MathHelper.lerp(
                    (float) startU,
                    surface.northEastHeight,
                    surface.southEastHeight);
            endHeight = MathHelper.lerp(
                    (float) endU,
                    surface.northEastHeight,
                    surface.southEastHeight);
        }

        double bottomY = Math.max(surface.y, minY);
        double startTopY = Math.min(surface.y + startHeight + FLUID_OFFSET, maxY);
        double endTopY = Math.min(surface.y + endHeight + FLUID_OFFSET, maxY);
        if (startTopY <= bottomY && endTopY <= bottomY) {
            return;
        }

        double x0;
        double x1;
        double z0;
        double z1;
        if (xAxis) {
            x0 = clippedStart - centerX;
            x1 = clippedEnd - centerX;
            z0 = z1 = fixed - centerZ;
        } else {
            x0 = x1 = fixed - centerX;
            z0 = clippedStart - centerZ;
            z1 = clippedEnd - centerZ;
        }
        float localBottom = (float) (bottomY - centerY);
        float localStartTop = (float) (startTopY - centerY);
        float localEndTop = (float) (endTopY - centerY);
        if (direction == Direction.NORTH || direction == Direction.EAST) {
            putFluidVertex(
                    matrices, vertices, sprite,
                    x0, localBottom, z0,
                    startU, 1.0, direction);
            putFluidVertex(
                    matrices, vertices, sprite,
                    x0, localStartTop, z0,
                    startU, 1.0 - startHeight, direction);
            putFluidVertex(
                    matrices, vertices, sprite,
                    x1, localEndTop, z1,
                    endU, 1.0 - endHeight, direction);
            putFluidVertex(
                    matrices, vertices, sprite,
                    x1, localBottom, z1,
                    endU, 1.0, direction);
        } else {
            putFluidVertex(
                    matrices, vertices, sprite,
                    x0, localBottom, z0,
                    startU, 1.0, direction);
            putFluidVertex(
                    matrices, vertices, sprite,
                    x1, localBottom, z1,
                    endU, 1.0, direction);
            putFluidVertex(
                    matrices, vertices, sprite,
                    x1, localEndTop, z1,
                    endU, 1.0 - endHeight, direction);
            putFluidVertex(
                    matrices, vertices, sprite,
                    x0, localStartTop, z0,
                    startU, 1.0 - startHeight, direction);
        }
    }

    private static void putFluidVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices, Sprite sprite,
            double x, float y, double z, double u, double v,
            Direction direction) {
        vertices.vertex(matrices.getPositionMatrix(), (float) x, y, (float) z)
                .color(255, 255, 255, 255)
                .texture(normalizedSpriteU(sprite, u), normalizedSpriteV(sprite, v))
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(matrices.getNormalMatrix(), direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ()).next();
    }

    private static PreparedHorizontalFace prepareHorizontalFace(
            double centerX, double centerZ, TerrainCache cache,
            TerrainFace face, float radius, double minY, double maxY) {
        if (face.plane < minY - SURFACE_EPSILON || face.plane > maxY + SURFACE_EPSILON) {
            return null;
        }

        double x0 = face.x;
        double x1 = face.x + 1.0;
        double z0 = face.z;
        double z1 = face.z + 1.0;
        if (!hasCoplanarNeighbor(cache, face, Direction.WEST)) {
            x0 -= EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.EAST)) {
            x1 += EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.NORTH)) {
            z0 -= EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.SOUTH)) {
            z1 += EDGE_OVERLAP;
        }

        List<TerrainPoint> polygon = clipHorizontalRectangle(
                centerX, centerZ, x0, x1, z0, z1, radius);
        if (polygon.size() < 3) {
            return null;
        }
        return new PreparedHorizontalFace(face, polygon);
    }

    private static void drawHorizontalTextureFace(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer texturedVertices,
            SpriteSet sprites, Palette palette, PreparedHorizontalFace prepared) {
        TerrainFace face = prepared.face;
        List<TerrainPoint> polygon = prepared.polygon;
        Sprite foundation = sprites.topFoundation(
                face.direction == Direction.UP ? face.variant : 0);
        Sprite detail = face.direction == Direction.UP
                ? sprites.topDetail(face.variant)
                : null;
        drawHorizontalTextureLayer(
                centerX, centerY, centerZ, matrices, texturedVertices,
                face, polygon, foundation, palette, FACE_OFFSET);
        if (detail != null) {
            drawHorizontalTextureLayer(
                    centerX, centerY, centerZ, matrices, texturedVertices,
                    face, polygon, detail, palette, FACE_OFFSET + DETAIL_OFFSET);
        }
    }

    private static void drawHorizontalWashFace(
            double centerY, MatrixStack.Entry matrices, VertexConsumer washVertices,
            Palette palette, PreparedHorizontalFace prepared) {
        drawHorizontalWashLayer(
                centerY, matrices, washVertices, prepared.face, prepared.polygon, palette,
                FACE_OFFSET + WASH_OFFSET);
    }

    private static List<TerrainPoint> clipHorizontalRectangle(
            double centerX, double centerZ,
            double worldX0, double worldX1, double worldZ0, double worldZ1,
            float radius) {
        double minX = worldX0 - centerX;
        double maxX = worldX1 - centerX;
        double minZ = worldZ0 - centerZ;
        double maxZ = worldZ1 - centerZ;
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

        for (int i = 0; i < CIRCLE_SEGMENTS && !polygon.isEmpty(); i++) {
            double angle0 = MathHelper.TAU * i / CIRCLE_SEGMENTS;
            double angle1 = MathHelper.TAU * (i + 1) / CIRCLE_SEGMENTS;
            polygon = clipPolygon(
                    polygon,
                    new TerrainPoint(Math.cos(angle0) * radius, Math.sin(angle0) * radius),
                    new TerrainPoint(Math.cos(angle1) * radius, Math.sin(angle1) * radius)
            );
        }
        return polygon;
    }

    private static void drawHorizontalTextureLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, List<TerrainPoint> polygon, Sprite sprite,
            Palette palette, float normalOffset) {
        TerrainPoint center = polygonCenter(polygon);
        float localY = (float) (face.plane - centerY
                + face.direction.getOffsetY() * normalOffset);
        int light = terrainLight(face, palette);
        for (int i = 0; i < polygon.size(); i++) {
            TerrainPoint first = polygon.get(i);
            TerrainPoint second = polygon.get((i + 1) % polygon.size());
            if (face.direction == Direction.UP) {
                TerrainPoint swap = first;
                first = second;
                second = swap;
            }
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, center, localY, light, palette);
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, first, localY, light, palette);
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, second, localY, light, palette);
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, second, localY, light, palette);
        }
    }

    private static void drawHorizontalWashLayer(
            double centerY, MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, List<TerrainPoint> polygon, Palette palette,
            float normalOffset) {
        TerrainPoint center = polygonCenter(polygon);
        float localY = (float) (face.plane - centerY
                + face.direction.getOffsetY() * normalOffset);
        int light = terrainLight(face, palette);
        for (int i = 0; i < polygon.size(); i++) {
            TerrainPoint first = polygon.get(i);
            TerrainPoint second = polygon.get((i + 1) % polygon.size());
            if (face.direction == Direction.UP) {
                TerrainPoint swap = first;
                first = second;
                second = swap;
            }
            putWashVertex(matrices, vertices, center.x, localY, center.z,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, first.x, localY, first.z,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, second.x, localY, second.z,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, second.x, localY, second.z,
                    light, face.direction, palette);
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
            double centerX, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, Sprite sprite, TerrainPoint point,
            float localY, int light, Palette palette) {
        double worldX = point.x + centerX;
        double worldZ = point.z + centerZ;
        double textureU = MathHelper.clamp(worldX - face.x, 0.0, 1.0);
        double textureV = MathHelper.clamp(worldZ - face.z, 0.0, 1.0);
        TextureCoordinates uv = transformTopUv(textureU, textureV, face.uvTransform);
        putTextureVertex(matrices, vertices, sprite,
                point.x, localY, point.z, uv.u, uv.v,
                light, face.direction, palette);
    }

    private static PreparedVerticalFace prepareVerticalFace(
            double centerX, double centerZ, TerrainCache cache,
            TerrainFace face, float radius, double minY, double maxY) {
        boolean xAxis = face.direction.getAxis() == Direction.Axis.Z;
        Direction lowDirection = xAxis ? Direction.WEST : Direction.NORTH;
        Direction highDirection = xAxis ? Direction.EAST : Direction.SOUTH;
        double variableStart = xAxis ? face.x : face.z;
        double variableEnd = variableStart + 1.0;
        if (!hasCoplanarNeighbor(cache, face, lowDirection)) {
            variableStart -= EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, highDirection)) {
            variableEnd += EDGE_OVERLAP;
        }

        double y0 = face.y;
        double y1 = face.y + 1.0;
        if (!hasCoplanarNeighbor(cache, face, Direction.DOWN)) {
            y0 -= EDGE_OVERLAP;
        }
        if (!hasCoplanarNeighbor(cache, face, Direction.UP)) {
            y1 += EDGE_OVERLAP;
        }
        y0 = Math.max(y0, minY);
        y1 = Math.min(y1, maxY);
        if (y1 - y0 <= 1.0E-5) {
            return null;
        }

        double fixedCenter = xAxis ? centerZ : centerX;
        double fixedDistance = face.plane - fixedCenter;
        double radiusSquared = radius * radius;
        if (fixedDistance * fixedDistance > radiusSquared) {
            return null;
        }
        double span = Math.sqrt(Math.max(
                0.0, radiusSquared - fixedDistance * fixedDistance));
        double variableCenter = xAxis ? centerX : centerZ;
        double clippedStart = Math.max(variableStart, variableCenter - span);
        double clippedEnd = Math.min(variableEnd, variableCenter + span);
        if (clippedEnd - clippedStart <= 1.0E-5) {
            return null;
        }
        return new PreparedVerticalFace(
                face, xAxis, clippedStart, clippedEnd, y0, y1);
    }

    private static void drawVerticalTextureFace(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer texturedVertices,
            SpriteSet sprites, Palette palette, PreparedVerticalFace prepared) {
        TerrainFace face = prepared.face;
        Sprite foundation = sprites.sideFoundation(face.variant);
        Sprite detail = sprites.sideDetail(face.variant);
        drawVerticalTextureLayer(
                centerX, centerY, centerZ, matrices, texturedVertices,
                face, foundation, palette, prepared.xAxis,
                prepared.clippedStart, prepared.clippedEnd,
                prepared.y0, prepared.y1, FACE_OFFSET);
        if (detail != null) {
            drawVerticalTextureLayer(
                    centerX, centerY, centerZ, matrices, texturedVertices,
                    face, detail, palette, prepared.xAxis,
                    prepared.clippedStart, prepared.clippedEnd,
                    prepared.y0, prepared.y1, FACE_OFFSET + DETAIL_OFFSET);
        }
    }

    private static void drawVerticalWashFace(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer washVertices,
            Palette palette, PreparedVerticalFace prepared) {
        drawVerticalWashLayer(
                centerX, centerY, centerZ, matrices, washVertices,
                prepared.face, palette, prepared.xAxis,
                prepared.clippedStart, prepared.clippedEnd,
                prepared.y0, prepared.y1,
                FACE_OFFSET + WASH_OFFSET);
    }

    private static void drawVerticalTextureLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, Sprite sprite, Palette palette, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float normalOffset) {
        VerticalQuad quad = verticalQuad(
                centerX, centerY, centerZ, face, xAxis,
                variableStart, variableEnd, y0, y1, normalOffset);
        int light = terrainLight(face, palette);
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

        if (face.direction == Direction.NORTH || face.direction == Direction.EAST) {
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y0, quad.z0, u0, v0,
                    light, face.direction, palette);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y1, quad.z0, u0, v1,
                    light, face.direction, palette);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y1, quad.z1, u1, v1,
                    light, face.direction, palette);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y0, quad.z1, u1, v0,
                    light, face.direction, palette);
        } else {
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y0, quad.z0, u0, v0,
                    light, face.direction, palette);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y0, quad.z1, u1, v0,
                    light, face.direction, palette);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y1, quad.z1, u1, v1,
                    light, face.direction, palette);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y1, quad.z0, u0, v1,
                    light, face.direction, palette);
        }
    }

    private static void drawVerticalWashLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, Palette palette, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float normalOffset) {
        VerticalQuad quad = verticalQuad(
                centerX, centerY, centerZ, face, xAxis,
                variableStart, variableEnd, y0, y1, normalOffset);
        int light = terrainLight(face, palette);
        if (face.direction == Direction.NORTH || face.direction == Direction.EAST) {
            putWashVertex(matrices, vertices, quad.x0, quad.y0, quad.z0,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, quad.x0, quad.y1, quad.z0,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, quad.x1, quad.y1, quad.z1,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, quad.x1, quad.y0, quad.z1,
                    light, face.direction, palette);
        } else {
            putWashVertex(matrices, vertices, quad.x0, quad.y0, quad.z0,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, quad.x1, quad.y0, quad.z1,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, quad.x1, quad.y1, quad.z1,
                    light, face.direction, palette);
            putWashVertex(matrices, vertices, quad.x0, quad.y1, quad.z0,
                    light, face.direction, palette);
        }
    }

    private static VerticalQuad verticalQuad(
            double centerX, double centerY, double centerZ,
            TerrainFace face, boolean xAxis,
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
            x0 = variableStart - centerX;
            x1 = variableEnd - centerX;
            z0 = z1 = fixed - centerZ;
        } else {
            x0 = x1 = fixed - centerX;
            z0 = variableStart - centerZ;
            z1 = variableEnd - centerZ;
        }
        return new VerticalQuad(
                x0, (float) (y0 - centerY), z0,
                x1, (float) (y1 - centerY), z1);
    }

    private static boolean hasCoplanarNeighbor(
            TerrainCache cache, TerrainFace face, Direction offset) {
        TerrainFace neighbor = cache.facesByKey.get(new FaceKey(
                face.x + offset.getOffsetX(),
                face.y + offset.getOffsetY(),
                face.z + offset.getOffsetZ(),
                face.direction));
        return neighbor != null
                && Math.abs(neighbor.plane - face.plane) <= SURFACE_EPSILON;
    }

    private static List<TerrainPoint> clipPolygon(
            List<TerrainPoint> input, TerrainPoint edgeStart, TerrainPoint edgeEnd) {
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

    private static boolean insideEdge(
            TerrainPoint point, TerrainPoint start, TerrainPoint end) {
        return cross(end.x - start.x, end.z - start.z,
                point.x - start.x, point.z - start.z) >= -1.0E-7;
    }

    private static TerrainPoint edgeIntersection(
            TerrainPoint lineStart, TerrainPoint lineEnd,
            TerrainPoint clipStart, TerrainPoint clipEnd) {
        double lineX = lineEnd.x - lineStart.x;
        double lineZ = lineEnd.z - lineStart.z;
        double clipX = clipEnd.x - clipStart.x;
        double clipZ = clipEnd.z - clipStart.z;
        double denominator = cross(lineX, lineZ, clipX, clipZ);
        if (Math.abs(denominator) < 1.0E-9) {
            return lineEnd;
        }
        double t = cross(
                clipStart.x - lineStart.x,
                clipStart.z - lineStart.z,
                clipX,
                clipZ
        ) / denominator;
        return new TerrainPoint(
                lineStart.x + lineX * t,
                lineStart.z + lineZ * t);
    }

    private static double cross(double ax, double az, double bx, double bz) {
        return ax * bz - az * bx;
    }

    private static TextureCoordinates transformTopUv(
            double u, double v, int transform) {
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
            Direction direction, Palette palette) {
        vertices.vertex(matrices.getPositionMatrix(), (float) x, y, (float) z)
                .color(palette.terrainRed, palette.terrainGreen, palette.terrainBlue, 255)
                .texture(normalizedSpriteU(sprite, u), normalizedSpriteV(sprite, v))
                .light(light)
                .normal(matrices.getNormalMatrix(), direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ()).next();
    }

    static float normalizedSpriteU(Sprite sprite, double normalizedU) {
        return sprite.getFrameU(normalizedU * 16.0);
    }

    static float normalizedSpriteV(Sprite sprite, double normalizedV) {
        return sprite.getFrameV(normalizedV * 16.0);
    }

    private static void putWashVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices,
            double x, float y, double z, int light,
            Direction direction, Palette palette) {
        vertices.vertex(matrices.getPositionMatrix(), (float) x, y, (float) z)
                .color(palette.washRed, palette.washGreen,
                        palette.washBlue, palette.washAlpha)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices.getNormalMatrix(), direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ()).next();
    }

    private static int terrainLight(TerrainFace face, Palette palette) {
        if (palette.variants.get(face.variant).emissive) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
        int light = WorldRenderer.getLightmapCoordinates(
                face.world, face.state, face.blockPos);
        int blockLight = Math.max(
                LightmapTextureManager.getBlockLightCoordinates(light),
                palette.lightFloor);
        int skyLight = Math.max(
                LightmapTextureManager.getSkyLightCoordinates(light),
                palette.lightFloor);
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

    public static final class Palette {
        private final List<Variant> variants;
        private final int terrainRed;
        private final int terrainGreen;
        private final int terrainBlue;
        private final int washRed;
        private final int washGreen;
        private final int washBlue;
        private final int washAlpha;
        private final int lightFloor;
        private final int refreshTicks;
        private final int movementMargin;
        private final FluidReplacement fluidReplacement;
        private final int totalWeight;

        private Palette(List<Variant> variants,
                        int terrainRed, int terrainGreen, int terrainBlue,
                        int washRed, int washGreen, int washBlue, int washAlpha,
                        int lightFloor, int refreshTicks, int movementMargin) {
            this(
                    variants,
                    terrainRed, terrainGreen, terrainBlue,
                    washRed, washGreen, washBlue, washAlpha,
                    lightFloor, refreshTicks, movementMargin,
                    null
            );
        }

        private Palette(List<Variant> variants,
                        int terrainRed, int terrainGreen, int terrainBlue,
                        int washRed, int washGreen, int washBlue, int washAlpha,
                        int lightFloor, int refreshTicks, int movementMargin,
                        FluidReplacement fluidReplacement) {
            this.variants = variants;
            this.terrainRed = terrainRed;
            this.terrainGreen = terrainGreen;
            this.terrainBlue = terrainBlue;
            this.washRed = washRed;
            this.washGreen = washGreen;
            this.washBlue = washBlue;
            this.washAlpha = washAlpha;
            this.lightFloor = lightFloor;
            this.refreshTicks = Math.max(1, refreshTicks);
            this.movementMargin = Math.max(0, movementMargin);
            this.fluidReplacement = fluidReplacement;
            this.totalWeight = variants.stream().mapToInt(Variant::weight).sum();
        }

        private int variantIndex(int hash) {
            int roll = Math.floorMod(hash, Math.max(1, totalWeight));
            for (int i = 0; i < variants.size(); i++) {
                roll -= variants.get(i).weight;
                if (roll < 0) {
                    return i;
                }
            }
            return 0;
        }
    }

    private record Variant(int weight, Identifier topFoundation,
                           Identifier topDetail, Identifier sideFoundation,
                           Identifier sideDetail, boolean emissive) {
        private Variant(int weight, Identifier topFoundation,
                        Identifier topDetail, Identifier sideFoundation,
                        Identifier sideDetail) {
            this(
                    weight,
                    topFoundation,
                    topDetail,
                    sideFoundation,
                    sideDetail,
                    false
            );
        }
    }

    private record FluidReplacement(Identifier still, Identifier flow) {
    }

    private record SpriteVariant(Sprite topFoundation, Sprite topDetail,
                                 Sprite sideFoundation, Sprite sideDetail) {
    }

    private record SpriteSet(List<SpriteVariant> variants,
                             Sprite fluidStill, Sprite fluidFlow) {
        private static SpriteSet load(Palette palette) {
            BakedModelManager models = MinecraftClient.getInstance().getBakedModelManager();
            SpriteAtlasTexture atlas = models.getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            List<SpriteVariant> sprites = new ArrayList<>(palette.variants.size());
            for (Variant variant : palette.variants) {
                sprites.add(new SpriteVariant(
                        atlas.getSprite(variant.topFoundation),
                        variant.topDetail == null ? null : atlas.getSprite(variant.topDetail),
                        atlas.getSprite(variant.sideFoundation),
                        variant.sideDetail == null ? null : atlas.getSprite(variant.sideDetail)
                ));
            }
            Sprite fluidStill = palette.fluidReplacement == null
                    ? null
                    : atlas.getSprite(palette.fluidReplacement.still);
            Sprite fluidFlow = palette.fluidReplacement == null
                    ? null
                    : atlas.getSprite(palette.fluidReplacement.flow);
            return new SpriteSet(sprites, fluidStill, fluidFlow);
        }

        private Sprite topFoundation(int index) {
            return variants.get(index).topFoundation;
        }

        private Sprite topDetail(int index) {
            return variants.get(index).topDetail;
        }

        private Sprite sideFoundation(int index) {
            return variants.get(index).sideFoundation;
        }

        private Sprite sideDetail(int index) {
            return variants.get(index).sideDetail;
        }
    }

    private static final class TerrainFace {
        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private final double plane;
        private final Direction direction;
        private final int variant;
        private final int uvTransform;
        private final BlockState state;
        private final BlockPos blockPos;

        private TerrainFace(World world, int x, int y, int z, double plane,
                            Direction direction, int variant, int uvTransform,
                            BlockState state, BlockPos blockPos) {
            this.world = world;
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

    private static final class FluidSurface {
        private final int x;
        private final int y;
        private final int z;
        private final float northWestHeight;
        private final float southWestHeight;
        private final float southEastHeight;
        private final float northEastHeight;
        private final double velocityX;
        private final double velocityZ;
        private final boolean top;
        private final int sideMask;

        private FluidSurface(
                int x, int y, int z,
                float northWestHeight, float southWestHeight,
                float southEastHeight, float northEastHeight,
                double velocityX, double velocityZ,
                boolean top, int sideMask) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.northWestHeight = northWestHeight;
            this.southWestHeight = southWestHeight;
            this.southEastHeight = southEastHeight;
            this.northEastHeight = northEastHeight;
            this.velocityX = velocityX;
            this.velocityZ = velocityZ;
            this.top = top;
            this.sideMask = sideMask;
        }
    }

    private static final class TerrainCache {
        private final World world;
        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final int scanRadius;
        private final float verticalRange;
        private final Palette palette;
        private final long lastRefreshTick;
        private final List<TerrainFace> faces;
        private final Map<FaceKey, TerrainFace> facesByKey;
        private final List<FluidSurface> fluidSurfaces;
        private long lastSeenTick;

        private TerrainCache(World world, int centerX, int centerY, int centerZ,
                             int scanRadius, float verticalRange, Palette palette,
                             long now, List<TerrainFace> faces,
                             Map<FaceKey, TerrainFace> facesByKey,
                             List<FluidSurface> fluidSurfaces) {
            this.world = world;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.scanRadius = scanRadius;
            this.verticalRange = verticalRange;
            this.palette = palette;
            this.lastRefreshTick = now;
            this.lastSeenTick = now;
            this.faces = faces;
            this.facesByKey = facesByKey;
            this.fluidSurfaces = fluidSurfaces;
        }
    }

    private record FaceKey(int x, int y, int z, Direction direction) {
    }

    private interface PreparedFace {
    }

    private record PreparedHorizontalFace(
            TerrainFace face, List<TerrainPoint> polygon) implements PreparedFace {
    }

    private record PreparedVerticalFace(
            TerrainFace face, boolean xAxis,
            double clippedStart, double clippedEnd,
            double y0, double y1) implements PreparedFace {
    }

    private record TerrainPoint(double x, double z) {
    }

    private record TextureCoordinates(double u, double v) {
    }

    private record VerticalQuad(double x0, float y0, double z0,
                                double x1, float y1, double z1) {
    }
}
