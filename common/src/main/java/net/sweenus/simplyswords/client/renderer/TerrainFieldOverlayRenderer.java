package net.sweenus.simplyswords.client.renderer;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
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
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.BloodStainVisualEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Renders visual-only texture treatments over exposed terrain faces. Palettes can
 * either replace the underlying texture or tint each face's baked block texture.
 * The world remains untouched; footprint clipping also supports partial blocks.
 */
public final class TerrainFieldOverlayRenderer {

    private static final int CIRCLE_SEGMENTS = 48;
    private static final int BLOOD_POOL_SEGMENTS = 24;
    private static final int BLOOD_FINE_GRID_SCALE = 4;
    private static final int BLOOD_COARSE_GRID_SCALE = 2;
    private static final int BLOOD_COARSE_ENTER_CELLS = 768;
    private static final int BLOOD_COARSE_EXIT_CELLS = 512;
    private static final int BLOOD_CACHE_GROWTH_MARGIN = 4;
    private static final int CACHE_GRACE_TICKS = 40;
    private static final float FACE_OFFSET = 0.006F;
    private static final float DETAIL_OFFSET = 0.001F;
    private static final float BLOOD_BORDER_OFFSET = 0.004F;
    private static final double BLOOD_BORDER_WIDTH = 0.10;
    private static final float WASH_OFFSET = 0.002F;
    private static final float EDGE_OVERLAP = 0.008F;
    private static final float FLUID_OFFSET = 0.012F;
    private static final float FLUID_EDGE_OVERLAP = 0.003F;
    private static final float SURFACE_EPSILON = 0.001F;
    private static final Identifier WHITE_TEXTURE =
            Identifier.ofVanilla("textures/misc/white.png");
    private static final Set<FaceKey> BLOOD_FACES_THIS_FRAME = new HashSet<>();
    private static final Set<UUID> BLOOD_COMPONENTS_THIS_FRAME = new HashSet<>();
    private static long bloodFrame;
    private static boolean skipBloodRenderThisFrame;

    public static final Palette CAELESTIS = new Palette(
            List.of(
                    new Variant(65,
                            Identifier.ofVanilla("block/sculk"), null,
                            Identifier.ofVanilla("block/sculk"), null),
                    new Variant(17,
                            Identifier.ofVanilla("block/sculk_catalyst_top"), null,
                            Identifier.ofVanilla("block/sculk_catalyst_side"), null),
                    new Variant(10,
                            Identifier.ofVanilla("block/sculk_shrieker_inner_top"),
                            Identifier.ofVanilla("block/sculk_shrieker_top"),
                            Identifier.ofVanilla("block/sculk"),
                            Identifier.ofVanilla("block/sculk_shrieker_side")),
                    new Variant(8,
                            Identifier.ofVanilla("block/sculk_sensor_top"), null,
                            Identifier.ofVanilla("block/sculk"),
                            Identifier.ofVanilla("block/sculk_sensor_side"))
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
                            Identifier.ofVanilla("block/soul_soil"), null,
                            Identifier.ofVanilla("block/soul_soil"), null),
                    new Variant(22,
                            Identifier.ofVanilla("block/soul_sand"), null,
                            Identifier.ofVanilla("block/soul_sand"), null),
                    new Variant(14,
                            Identifier.ofVanilla("block/basalt_top"), null,
                            Identifier.ofVanilla("block/basalt_side"), null),
                    new Variant(11,
                            Identifier.ofVanilla("block/blackstone"), null,
                            Identifier.ofVanilla("block/blackstone"), null),
                    new Variant(5,
                            Identifier.ofVanilla("block/crying_obsidian"), null,
                            Identifier.ofVanilla("block/crying_obsidian"), null),
                    new Variant(5,
                            Identifier.ofVanilla("block/glowstone"), null,
                            Identifier.ofVanilla("block/glowstone"), null,
                            true)
            ),
            188, 210, 222,
            20, 88, 94, 30,
            7,
            10,
            3,
            new FluidReplacement(
                    Identifier.ofVanilla("block/lava_still"),
                    Identifier.ofVanilla("block/lava_flow")
            )
    );

    public static final Palette BLOOD_STAIN = Palette.blockTextureTint(
            168, 42, 53,
            new BloodStyle(104, 17, 28, (float) BLOOD_BORDER_WIDTH),
            40,
            0
    );

    public static final Palette WHITE_MARBLE = new Palette(
            List.of(
                    new Variant(46,
                            Identifier.ofVanilla("block/calcite"), null,
                            Identifier.ofVanilla("block/calcite"), null),
                    new Variant(20,
                            Identifier.ofVanilla("block/quartz_block_top"), null,
                            Identifier.ofVanilla("block/quartz_block_side"), null),
                    new Variant(16,
                            Identifier.ofVanilla("block/polished_diorite"), null,
                            Identifier.ofVanilla("block/polished_diorite"), null),
                    new Variant(10,
                            Identifier.ofVanilla("block/quartz_block_bottom"), null,
                            Identifier.ofVanilla("block/quartz_block_bottom"), null),
                    new Variant(5,
                            Identifier.ofVanilla("block/chiseled_quartz_block_top"), null,
                            Identifier.ofVanilla("block/chiseled_quartz_block"), null),
                    new Variant(3,
                            Identifier.ofVanilla("block/quartz_pillar_top"), null,
                            Identifier.ofVanilla("block/quartz_pillar"), null)
            ),
            255, 255, 255,
            255, 255, 255, 0,
            10,
            20,
            0
    );

    private final Map<UUID, TerrainCache> caches = new HashMap<>();
    private final Map<UUID, ShapeCache> bloodShapes = new HashMap<>();
    private final Map<UUID, BloodMaskCache> bloodMasks = new HashMap<>();
    private long bloodSnapshotFrame = Long.MIN_VALUE;
    private BloodSnapshot bloodSnapshot = BloodSnapshot.EMPTY;
    private World bloodTopologyWorld;
    private long bloodTopologySignature = Long.MIN_VALUE;
    private int bloodGridScale = BLOOD_FINE_GRID_SCALE;
    private Map<UUID, BloodComponent> bloodComponentsByMember = Map.of();

    public static void beginWorldFrame() {
        BLOOD_FACES_THIS_FRAME.clear();
        BLOOD_COMPONENTS_THIS_FRAME.clear();
        bloodFrame++;
        skipBloodRenderThisFrame = IrisCompat.isRenderingShadowPass();
    }

    public static boolean shouldSkipBloodRender() {
        return skipBloodRenderThisFrame || IrisCompat.isRenderingShadowPass();
    }

    public void clear(UUID id) {
        this.caches.remove(id);
        this.bloodShapes.remove(id);
        this.bloodMasks.remove(id);
    }

    public void clearBlood() {
        this.bloodSnapshot = BloodSnapshot.EMPTY;
        this.bloodSnapshotFrame = Long.MIN_VALUE;
        this.bloodMasks.clear();
        this.bloodTopologyWorld = null;
        this.bloodTopologySignature = Long.MIN_VALUE;
        this.bloodComponentsByMember = Map.of();
        this.bloodGridScale = BLOOD_FINE_GRID_SCALE;
    }

    public void render(World world, UUID id,
                       double centerX, double centerY, double centerZ,
                       float radius, float maxRadius, float verticalRange,
                       Palette palette, MatrixStack.Entry matrices,
                       VertexConsumerProvider vertexConsumers) {
        render(world, id, centerX, centerY, centerZ, radius, maxRadius,
                verticalRange, palette, 1.0F, matrices, vertexConsumers);
    }

    public void render(World world, UUID id,
                       double centerX, double centerY, double centerZ,
                       float radius, float maxRadius, float verticalRange,
                       Palette palette, float opacity, MatrixStack.Entry matrices,
                       VertexConsumerProvider vertexConsumers) {
        if (radius <= 0.05F) {
            clear(id);
            return;
        }
        List<TerrainPoint> footprint = circleFootprint(radius);
        renderFootprint(world, id, centerX, centerY, centerZ,
                SplatterShape.single(footprint), shapeKey(0, radius, 0.0F, 0.0F, 0),
                Math.max(radius, maxRadius), radius,
                verticalRange, palette, opacity, matrices, vertexConsumers);
    }

    public void renderTrail(World world, UUID id,
                            double centerX, double centerY, double centerZ,
                            float halfLength, float radius, float yaw,
                            float verticalRange, Palette palette, float opacity,
                            MatrixStack.Entry matrices,
                            VertexConsumerProvider vertexConsumers) {
        if (radius <= 0.05F) {
            clear(id);
            return;
        }
        List<TerrainPoint> footprint = capsuleFootprint(halfLength, radius, yaw);
        renderFootprint(world, id, centerX, centerY, centerZ,
                SplatterShape.single(footprint), shapeKey(1, halfLength, radius, yaw, 0),
                halfLength + radius,
                0.0F, verticalRange, palette, opacity, matrices, vertexConsumers);
    }

    public void renderBloodCircle(World world, UUID id,
                                  double centerX, double centerY, double centerZ,
                                  float radius, float verticalRange, int seed, float opacity,
                                  MatrixStack.Entry matrices,
                                  VertexConsumerProvider vertexConsumers) {
        if (radius <= 0.05F || shouldSkipBloodRender()) {
            if (radius <= 0.05F) {
                clear(id);
            }
            return;
        }
        long shapeKey = shapeKey(2, radius, 0.0F, 0.0F, seed);
        SplatterShape shape = getBloodShape(
                id, shapeKey, () -> bloodPoolShape(radius, seed));
        renderFootprint(world, id, centerX, centerY, centerZ,
                shape, shapeKey, radius, 0.0F, verticalRange,
                BLOOD_STAIN, opacity, matrices, vertexConsumers);
    }

    public void renderBloodTrail(World world, UUID id,
                                 double centerX, double centerY, double centerZ,
                                 float halfLength, float radius, float yaw,
                                 float verticalRange, int seed, float opacity,
                                 MatrixStack.Entry matrices,
                                 VertexConsumerProvider vertexConsumers) {
        if (radius <= 0.05F || shouldSkipBloodRender()) {
            if (radius <= 0.05F) {
                clear(id);
            }
            return;
        }
        long shapeKey = shapeKey(3, halfLength, radius, yaw, seed);
        SplatterShape shape = getBloodShape(
                id, shapeKey, () -> bloodTrailShape(halfLength, radius, yaw, seed));
        renderFootprint(world, id, centerX, centerY, centerZ,
                shape, shapeKey, halfLength + radius, 0.0F, verticalRange,
                BLOOD_STAIN, opacity, matrices, vertexConsumers);
    }

    public void renderMergedBlood(BloodStainVisualEntity trigger, float tickDelta,
                                  MatrixStack.Entry matrices,
                                  VertexConsumerProvider vertexConsumers) {
        if (shouldSkipBloodRender()) {
            return;
        }
        World world = trigger.getWorld();
        if (this.bloodSnapshotFrame != bloodFrame
                || this.bloodSnapshot.world != world) {
            this.bloodSnapshot = buildBloodSnapshot(world, tickDelta);
            this.bloodSnapshotFrame = bloodFrame;
        }
        BloodComponent component = this.bloodSnapshot.byMember.get(trigger.getUuid());
        if (component == null || !BLOOD_COMPONENTS_THIS_FRAME.add(component.id)) {
            return;
        }

        double renderCenterX = MathHelper.lerp(
                tickDelta, trigger.prevX, trigger.getX());
        double renderCenterY = MathHelper.lerp(
                tickDelta, trigger.prevY, trigger.getY());
        double renderCenterZ = MathHelper.lerp(
                tickDelta, trigger.prevZ, trigger.getZ());
        double gridSize = 1.0 / component.gridScale;
        double minX = component.minGridX * gridSize;
        double maxX = (component.maxGridX + 1) * gridSize;
        double minZ = component.minGridZ * gridSize;
        double maxZ = (component.maxGridZ + 1) * gridSize;
        double cacheCenterX = (minX + maxX) * 0.5;
        double cacheCenterY = (component.minY + component.maxY) * 0.5;
        double cacheCenterZ = (minZ + maxZ) * 0.5;
        double extentX = (maxX - minX) * 0.5;
        double extentZ = (maxZ - minZ) * 0.5;
        float verticalRange = (float) Math.max(2.0,
                Math.max(cacheCenterY - component.minY,
                        component.maxY - cacheCenterY));
        TerrainCache terrain = getCache(
                world, component.id, cacheCenterX, cacheCenterY, cacheCenterZ,
                (float) Math.max(extentX, extentZ),
                verticalRange, BLOOD_STAIN);
        if (terrain.faces.isEmpty()) {
            return;
        }
        BloodPreparedGeometry geometry = prepareBloodGeometry(
                terrain, component, cacheCenterX, cacheCenterY, cacheCenterZ,
                component.minY, component.maxY);
        drawBloodGeometry(renderCenterX, renderCenterY, renderCenterZ,
                matrices, vertexConsumers,
                geometry, this.bloodSnapshot.sourceOpacities,
                this.bloodSnapshot.allOpaque);
    }

    private BloodSnapshot buildBloodSnapshot(World world, float tickDelta) {
        List<BloodEntityState> states = new ArrayList<>();
        Set<UUID> loaded = new HashSet<>();
        for (BloodStainVisualEntity entity : bloodEntities(world)) {
            loaded.add(entity.getUuid());
            float opacity = bloodOpacity(entity, tickDelta);
            if (opacity <= 0.01F || entity.getRadius() <= 0.05F) {
                continue;
            }
            double centerX = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
            double centerY = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
            double centerZ = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
            float yaw = MathHelper.lerpAngleDegrees(
                    tickDelta, entity.prevYaw, entity.getYaw())
                    * MathHelper.RADIANS_PER_DEGREE;
            long shapeKey;
            SplatterShape organicShape;
            if (entity.getShape() == BloodStainVisualEntity.SHAPE_TRAIL) {
                shapeKey = shapeKey(3, entity.getHalfLength(),
                        entity.getRadius(), yaw, entity.getSeed());
                organicShape = getBloodShape(entity.getUuid(), shapeKey,
                        () -> bloodTrailShape(entity.getHalfLength(),
                                entity.getRadius(), yaw, entity.getSeed()));
            } else {
                shapeKey = shapeKey(2, entity.getRadius(),
                        0.0F, 0.0F, entity.getSeed());
                organicShape = getBloodShape(entity.getUuid(), shapeKey,
                        () -> bloodPoolShape(entity.getRadius(), entity.getSeed()));
            }
            long maskKey = maskKey(shapeKey, centerX, centerZ);
            BloodMaskCache maskCache = this.bloodMasks.get(entity.getUuid());
            if (maskCache == null || maskCache.maskKey != maskKey) {
                maskCache = new BloodMaskCache(maskKey,
                        rasterizeBloodShape(
                                organicShape, centerX, centerZ, BLOOD_FINE_GRID_SCALE),
                        null);
                this.bloodMasks.put(entity.getUuid(), maskCache);
            }
            if (!maskCache.fineCells.isEmpty()) {
                states.add(new BloodEntityState(
                        entity.getUuid(), maskKey, maskCache,
                        centerY - entity.getVerticalRange(),
                        centerY + entity.getVerticalRange(),
                        opacity, entity.age));
            }
        }
        this.bloodMasks.keySet().removeIf(id -> !loaded.contains(id));
        this.bloodShapes.keySet().removeIf(id ->
                !loaded.contains(id) && !this.caches.containsKey(id));
        if (states.isEmpty()) {
            this.bloodComponentsByMember = Map.of();
            this.bloodTopologyWorld = world;
            this.bloodTopologySignature = 0L;
            return new BloodSnapshot(world, Map.of(), new float[0], true);
        }

        states.sort(Comparator.comparing(BloodEntityState::id));
        int estimatedFineCells = 0;
        for (BloodEntityState state : states) {
            estimatedFineCells += state.mask.fineCells.size();
        }
        if (this.bloodGridScale == BLOOD_FINE_GRID_SCALE
                && estimatedFineCells > BLOOD_COARSE_ENTER_CELLS) {
            this.bloodGridScale = BLOOD_COARSE_GRID_SCALE;
        } else if (this.bloodGridScale == BLOOD_COARSE_GRID_SCALE
                && estimatedFineCells < BLOOD_COARSE_EXIT_CELLS) {
            this.bloodGridScale = BLOOD_FINE_GRID_SCALE;
        }

        List<BloodSource> sources = new ArrayList<>(states.size());
        float[] opacities = new float[states.size()];
        boolean allOpaque = true;
        long topologySignature = mixSignature(
                0xcbf29ce484222325L, this.bloodGridScale);
        for (int i = 0; i < states.size(); i++) {
            BloodEntityState state = states.get(i);
            LongSet cells = this.bloodGridScale == BLOOD_FINE_GRID_SCALE
                    ? state.mask.fineCells
                    : coarseBloodMask(state.mask);
            sources.add(new BloodSource(
                    i, state.id, cells, state.minY, state.maxY, state.age));
            opacities[i] = state.opacity;
            allOpaque &= state.opacity >= 0.999F;
            topologySignature = mixSignature(topologySignature, state.id.hashCode());
            topologySignature = mixSignature(topologySignature, state.maskKey);
            topologySignature = mixSignature(
                    topologySignature, Double.doubleToLongBits(state.minY));
            topologySignature = mixSignature(
                    topologySignature, Double.doubleToLongBits(state.maxY));
        }

        if (this.bloodTopologyWorld != world
                || this.bloodTopologySignature != topologySignature) {
            this.bloodComponentsByMember = buildBloodTopology(
                    sources, this.bloodGridScale);
            this.bloodTopologyWorld = world;
            this.bloodTopologySignature = topologySignature;
        }
        return new BloodSnapshot(
                world, this.bloodComponentsByMember, opacities, allOpaque);
    }

    private static Iterable<BloodStainVisualEntity> bloodEntities(World world) {
        List<BloodStainVisualEntity> result = new ArrayList<>();
        if (!(world instanceof ClientWorld clientWorld)) {
            return result;
        }
        for (net.minecraft.entity.Entity entity : clientWorld.getEntities()) {
            if (entity instanceof BloodStainVisualEntity blood && blood.isAlive()) {
                result.add(blood);
            }
        }
        return result;
    }

    private static float bloodOpacity(BloodStainVisualEntity entity, float tickDelta) {
        float age = entity.age + tickDelta;
        float remaining = entity.getLifetime() - age;
        return remaining >= entity.getFadeDuration()
                ? 1.0F
                : MathHelper.clamp(remaining / entity.getFadeDuration(), 0.0F, 1.0F);
    }

    private void renderFootprint(World world, UUID id,
                                 double centerX, double centerY, double centerZ,
                                 SplatterShape shape, long shapeKey, float scanExtent,
                                 float fluidRadius, float verticalRange,
                                 Palette palette, float opacity,
                                 MatrixStack.Entry matrices,
                                 VertexConsumerProvider vertexConsumers) {
        opacity = MathHelper.clamp(opacity, 0.0F, 1.0F);
        if (opacity <= 0.01F || shape.parts.isEmpty()) {
            clear(id);
            return;
        }
        TerrainCache cache = getCache(
                world, id, centerX, centerY, centerZ, scanExtent, verticalRange, palette);
        if (cache.faces.isEmpty() && cache.fluidSurfaces.isEmpty()) {
            return;
        }

        SpriteSet sprites = palette.useBlockModelTextures
                ? SpriteSet.EMPTY
                : SpriteSet.load(palette);
        double minY = centerY - verticalRange;
        double maxY = centerY + verticalRange;
        PreparedGeometry preparedGeometry = prepareGeometry(
                cache, centerX, centerY, centerZ, shape, shapeKey, minY, maxY);
        List<PreparedFace> preparedFaces = preparedGeometry.faces;

        boolean translucent = opacity < 0.999F
                || shape.parts.stream().anyMatch(part -> part.opacity < 0.999F);
        VertexConsumer texturedVertices = vertexConsumers.getBuffer(
                translucent
                        ? RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                        : RenderLayer.getCutout());
        for (PreparedFace prepared : preparedFaces) {
            float faceOpacity = opacity * prepared.part().opacity;
            if (palette == BLOOD_STAIN && faceOpacity >= 0.999F
                    && prepared.fullyCovered()
                    && !BLOOD_FACES_THIS_FRAME.add(prepared.face().key())) {
                continue;
            }
            if (prepared instanceof PreparedHorizontalFace horizontal) {
                drawHorizontalTextureFace(
                        centerX, centerY, centerZ, matrices, texturedVertices,
                        sprites, palette, faceOpacity, horizontal);
            } else if (prepared instanceof PreparedVerticalFace vertical) {
                drawVerticalTextureFace(
                        centerX, centerY, centerZ, matrices, texturedVertices,
                        sprites, palette, faceOpacity, vertical);
            }
        }
        if (sprites.fluidStill != null && sprites.fluidFlow != null) {
            for (SplatterPart part : shape.parts) {
                for (FluidSurface surface : cache.fluidSurfaces) {
                    drawFluidSurface(
                            centerX, centerY, centerZ,
                            matrices, texturedVertices, sprites,
                            surface, fluidRadius, part.points, minY, maxY
                    );
                }
            }
        }

        if (palette.washAlpha > 0) {
            VertexConsumer washVertices = vertexConsumers.getBuffer(
                    RenderLayer.getEntityTranslucent(WHITE_TEXTURE));
            for (PreparedFace prepared : preparedFaces) {
                float faceOpacity = opacity * prepared.part().opacity;
                if (prepared instanceof PreparedHorizontalFace horizontal) {
                    drawHorizontalWashFace(centerY, matrices, washVertices, palette, faceOpacity, horizontal);
                } else if (prepared instanceof PreparedVerticalFace vertical) {
                    drawVerticalWashFace(
                            centerX, centerY, centerZ, matrices, washVertices,
                            palette, faceOpacity, vertical);
                }
            }
        }
    }

    private TerrainCache getCache(World world, UUID id,
                                  double centerX, double centerY, double centerZ,
                                  float scanExtent, float verticalRange, Palette palette) {
        long now = world.getTime();
        caches.entrySet().removeIf(entry ->
                entry.getValue().world != world
                        || now - entry.getValue().lastSeenTick > CACHE_GRACE_TICKS);
        bloodShapes.keySet().removeIf(shapeId ->
                !shapeId.equals(id)
                        && !caches.containsKey(shapeId)
                        && !bloodMasks.containsKey(shapeId));

        int requestedX = MathHelper.floor(centerX);
        int requestedY = MathHelper.floor(centerY);
        int requestedZ = MathHelper.floor(centerZ);
        int requiredScanRadius = MathHelper.ceil(scanExtent)
                + 2 + palette.movementMargin;
        boolean bloodPalette = palette == BLOOD_STAIN;
        int scanRadius = requiredScanRadius
                + (bloodPalette ? BLOOD_CACHE_GROWTH_MARGIN : 0);
        TerrainCache cache = caches.get(id);
        boolean movedOutsideMargin = cache != null && (bloodPalette
                ? requestedX - requiredScanRadius < cache.centerX - cache.scanRadius
                || requestedX + requiredScanRadius > cache.centerX + cache.scanRadius
                || requestedZ - requiredScanRadius < cache.centerZ - cache.scanRadius
                || requestedZ + requiredScanRadius > cache.centerZ + cache.scanRadius
                || Math.abs(requestedY - cache.centerY) > 1
                : Math.abs(requestedX - cache.centerX) > palette.movementMargin
                || Math.abs(requestedZ - cache.centerZ) > palette.movementMargin
                || Math.abs(requestedY - cache.centerY) > 1);
        boolean rebuild = cache == null
                || cache.world != world
                || movedOutsideMargin
                || (!bloodPalette && cache.scanRadius != scanRadius)
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

    private SplatterShape getBloodShape(
            UUID id, long shapeKey, Supplier<SplatterShape> factory) {
        ShapeCache cached = this.bloodShapes.get(id);
        if (cached != null && cached.shapeKey == shapeKey) {
            return cached.shape;
        }
        SplatterShape shape = factory.get();
        this.bloodShapes.put(id, new ShapeCache(shapeKey, shape));
        return shape;
    }

    private static LongSet rasterizeBloodShape(
            SplatterShape shape, double centerX, double centerZ, int gridScale) {
        LongOpenHashSet cells = new LongOpenHashSet();
        for (SplatterPart part : shape.parts) {
            if (part.points.isEmpty()) {
                continue;
            }
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            double meanX = 0.0;
            double meanZ = 0.0;
            for (TerrainPoint point : part.points) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minZ = Math.min(minZ, point.z);
                maxZ = Math.max(maxZ, point.z);
                meanX += point.x;
                meanZ += point.z;
            }
            int minGridX = MathHelper.floor((centerX + minX) * gridScale);
            int maxGridX = MathHelper.floor((centerX + maxX) * gridScale);
            int minGridZ = MathHelper.floor((centerZ + minZ) * gridScale);
            int maxGridZ = MathHelper.floor((centerZ + maxZ) * gridScale);
            int before = cells.size();
            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                double sampleZ = (gridZ + 0.5) / gridScale - centerZ;
                for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
                    double sampleX = (gridX + 0.5) / gridScale - centerX;
                    if (containsConvex(part.points, sampleX, sampleZ)) {
                        cells.add(packGridCell(gridX, gridZ));
                    }
                }
            }
            if (cells.size() == before) {
                meanX /= part.points.size();
                meanZ /= part.points.size();
                cells.add(packGridCell(
                        MathHelper.floor((centerX + meanX) * gridScale),
                        MathHelper.floor((centerZ + meanZ) * gridScale)));
            }
        }
        return cells;
    }

    private static long maskKey(long shapeKey, double centerX, double centerZ) {
        long key = shapeKey * 31L
                + MathHelper.floor(centerX * BLOOD_FINE_GRID_SCALE);
        return key * 31L
                + MathHelper.floor(centerZ * BLOOD_FINE_GRID_SCALE);
    }

    private static LongSet coarseBloodMask(BloodMaskCache mask) {
        if (mask.coarseCells != null) {
            return mask.coarseCells;
        }
        LongOpenHashSet coarse = new LongOpenHashSet();
        for (LongIterator iterator = mask.fineCells.iterator(); iterator.hasNext(); ) {
            long cell = iterator.nextLong();
            coarse.add(packGridCell(
                    Math.floorDiv(gridCellX(cell), 2),
                    Math.floorDiv(gridCellZ(cell), 2)));
        }
        mask.coarseCells = coarse;
        return coarse;
    }

    private static Map<UUID, BloodComponent> buildBloodTopology(
            List<BloodSource> sources, int gridScale) {
        int[] parents = new int[sources.size()];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = i;
        }
        Long2IntOpenHashMap cellOwner = new Long2IntOpenHashMap();
        cellOwner.defaultReturnValue(-1);
        for (int sourceIndex = 0; sourceIndex < sources.size(); sourceIndex++) {
            BloodSource source = sources.get(sourceIndex);
            for (LongIterator iterator = source.cells.iterator(); iterator.hasNext(); ) {
                long cell = iterator.nextLong();
                int x = gridCellX(cell);
                int z = gridCellZ(cell);
                connectBloodOwner(parents, sources, sourceIndex, cellOwner.get(cell));
                connectBloodOwner(parents, sources, sourceIndex,
                        cellOwner.get(packGridCell(x - 1, z)));
                connectBloodOwner(parents, sources, sourceIndex,
                        cellOwner.get(packGridCell(x + 1, z)));
                connectBloodOwner(parents, sources, sourceIndex,
                        cellOwner.get(packGridCell(x, z - 1)));
                connectBloodOwner(parents, sources, sourceIndex,
                        cellOwner.get(packGridCell(x, z + 1)));
                cellOwner.putIfAbsent(cell, sourceIndex);
            }
        }

        Map<Integer, List<BloodSource>> grouped = new HashMap<>();
        for (int i = 0; i < sources.size(); i++) {
            grouped.computeIfAbsent(find(parents, i), ignored -> new ArrayList<>())
                    .add(sources.get(i));
        }
        Map<UUID, BloodComponent> byMember = new HashMap<>();
        for (List<BloodSource> group : grouped.values()) {
            BloodComponent component = mergeBloodSources(group, gridScale);
            for (BloodSource source : group) {
                byMember.put(source.id, component);
            }
        }
        return Map.copyOf(byMember);
    }

    private static void connectBloodOwner(
            int[] parents, List<BloodSource> sources,
            int sourceIndex, int ownerIndex) {
        if (ownerIndex < 0) {
            return;
        }
        BloodSource source = sources.get(sourceIndex);
        BloodSource owner = sources.get(ownerIndex);
        if (source.maxY >= owner.minY && owner.maxY >= source.minY) {
            union(parents, sourceIndex, ownerIndex);
        }
    }

    private static int find(int[] parents, int index) {
        int root = index;
        while (parents[root] != root) {
            root = parents[root];
        }
        while (parents[index] != index) {
            int next = parents[index];
            parents[index] = root;
            index = next;
        }
        return root;
    }

    private static void union(int[] parents, int first, int second) {
        int firstRoot = find(parents, first);
        int secondRoot = find(parents, second);
        if (firstRoot != secondRoot) {
            parents[secondRoot] = firstRoot;
        }
    }

    private static BloodComponent mergeBloodSources(
            List<BloodSource> sources, int gridScale) {
        Long2ObjectOpenHashMap<IntArrayList> contributorLists =
                new Long2ObjectOpenHashMap<>();
        UUID id = sources.stream()
                .max(Comparator.comparingInt(BloodSource::age)
                        .thenComparing(BloodSource::id,
                                Comparator.reverseOrder()))
                .map(BloodSource::id)
                .orElseThrow();
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (BloodSource source : sources) {
            minY = Math.min(minY, source.minY);
            maxY = Math.max(maxY, source.maxY);
            for (LongIterator iterator = source.cells.iterator(); iterator.hasNext(); ) {
                long cell = iterator.nextLong();
                IntArrayList contributors = contributorLists.get(cell);
                if (contributors == null) {
                    contributors = new IntArrayList(2);
                    contributorLists.put(cell, contributors);
                }
                contributors.add(source.index);
            }
        }
        Long2ObjectOpenHashMap<int[]> contributorsByCell =
                new Long2ObjectOpenHashMap<>(contributorLists.size());
        int minGridX = Integer.MAX_VALUE;
        int maxGridX = Integer.MIN_VALUE;
        int minGridZ = Integer.MAX_VALUE;
        int maxGridZ = Integer.MIN_VALUE;
        LongSet blockColumns = new LongOpenHashSet();
        long signature = mixSignature(0xcbf29ce484222325L, gridScale);
        for (var entry : contributorLists.long2ObjectEntrySet()) {
            long cell = entry.getLongKey();
            int[] contributors = entry.getValue().toIntArray();
            contributorsByCell.put(cell, contributors);
            int x = gridCellX(cell);
            int z = gridCellZ(cell);
            minGridX = Math.min(minGridX, x);
            maxGridX = Math.max(maxGridX, x);
            minGridZ = Math.min(minGridZ, z);
            maxGridZ = Math.max(maxGridZ, z);
            blockColumns.add(packGridCell(
                    Math.floorDiv(x, gridScale),
                    Math.floorDiv(z, gridScale)));
            signature ^= mixSignature(cell, Arrays.hashCode(contributors));
        }
        return new BloodComponent(id, contributorsByCell, blockColumns,
                minGridX, maxGridX, minGridZ, maxGridZ,
                minY, maxY, signature, gridScale);
    }

    private static long mixSignature(long signature, long value) {
        signature ^= value + 0x9e3779b97f4a7c15L
                + (signature << 6) + (signature >>> 2);
        return signature;
    }

    private static long packGridCell(int x, int z) {
        return (long) x << 32 | z & 0xffffffffL;
    }

    private static int gridCellX(long cell) {
        return (int) (cell >> 32);
    }

    private static int gridCellZ(long cell) {
        return (int) cell;
    }

    private static BloodPreparedGeometry prepareBloodGeometry(
            TerrainCache cache, BloodComponent component,
            double centerX, double centerY, double centerZ,
            double minY, double maxY) {
        BloodPreparedSignature signature = new BloodPreparedSignature(
                centerX, centerY, centerZ, component.signature,
                component.gridScale, minY, maxY);
        if (signature.equals(cache.bloodPreparedSignature)
                && cache.bloodPreparedGeometry != null) {
            return cache.bloodPreparedGeometry;
        }

        List<BloodPreparedPatch> fillPatches = new ArrayList<>();
        List<BloodPreparedPatch> borderPatches = new ArrayList<>();
        double phase = (component.id.hashCode() & 0x7fffffff)
                / (double) Integer.MAX_VALUE * MathHelper.TAU;
        for (LongIterator iterator = component.blockColumns.iterator();
             iterator.hasNext(); ) {
            List<TerrainFace> columnFaces = cache.facesByColumn.get(iterator.nextLong());
            if (columnFaces == null) {
                continue;
            }
            for (TerrainFace face : columnFaces) {
                if (face.plane < minY - SURFACE_EPSILON
                        || face.plane > maxY + SURFACE_EPSILON) {
                    continue;
                }
                if (face.direction == Direction.UP) {
                    prepareBloodHorizontalPatches(
                            fillPatches, face, component, phase);
                    prepareBloodHorizontalBorderPatches(
                            borderPatches, face, component, phase);
                } else if (face.direction.getAxis().isHorizontal()) {
                    prepareBloodVerticalPatches(
                            fillPatches, borderPatches, cache, face,
                            component, phase, minY, maxY);
                }
            }
        }
        cache.bloodPreparedSignature = signature;
        cache.bloodPreparedGeometry = new BloodPreparedGeometry(
                List.copyOf(fillPatches), List.copyOf(borderPatches));
        return cache.bloodPreparedGeometry;
    }

    private static void prepareBloodHorizontalPatches(
            List<BloodPreparedPatch> patches, TerrainFace face,
            BloodComponent component, double phase) {
        int scale = component.gridScale;
        double gridSize = 1.0 / scale;
        int baseX = face.x * scale;
        int baseZ = face.z * scale;
        int[] fullBlockContributors = fullBloodBlockContributors(
                component, baseX, baseZ, scale);
        if (fullBlockContributors != null) {
            patches.add(horizontalBloodPatch(
                    face, fullBlockContributors, phase,
                    face.x, face.x + 1.0, face.z, face.z + 1.0));
            return;
        }
        for (int localZ = 0; localZ < scale; localZ++) {
            int localX = 0;
            while (localX < scale) {
                int gridX = baseX + localX;
                int gridZ = baseZ + localZ;
                int[] contributors = component.contributorsByCell.get(
                        packGridCell(gridX, gridZ));
                if (contributors == null) {
                    localX++;
                    continue;
                }
                int endX = localX + 1;
                while (endX < scale) {
                    int nextGridX = baseX + endX;
                    int[] next = component.contributorsByCell.get(
                            packGridCell(nextGridX, gridZ));
                    if (!Arrays.equals(contributors, next)) {
                        break;
                    }
                    endX++;
                }
                double x0 = face.x + localX * gridSize;
                double x1 = face.x + endX * gridSize;
                double z0 = face.z + localZ * gridSize;
                double z1 = z0 + gridSize;
                patches.add(horizontalBloodPatch(
                        face, contributors, phase,
                        x0, x1, z0, z1));
                localX = endX;
            }
        }
    }

    private static void prepareBloodHorizontalBorderPatches(
            List<BloodPreparedPatch> patches, TerrainFace face,
            BloodComponent component, double phase) {
        int scale = component.gridScale;
        double gridSize = 1.0 / scale;
        int baseX = face.x * scale;
        int baseZ = face.z * scale;
        prepareBloodBorderRuns(
                patches, face, component, phase, Direction.NORTH);
        prepareBloodBorderRuns(
                patches, face, component, phase, Direction.SOUTH);
        prepareBloodBorderRuns(
                patches, face, component, phase, Direction.WEST);
        prepareBloodBorderRuns(
                patches, face, component, phase, Direction.EAST);
        for (int localZ = 0; localZ < scale; localZ++) {
            for (int localX = 0; localX < scale; localX++) {
                int gridX = baseX + localX;
                int gridZ = baseZ + localZ;
                int[] contributors = component.contributorsByCell.get(
                        packGridCell(gridX, gridZ));
                if (contributors == null) {
                    continue;
                }
                boolean north = !hasBloodCell(component, gridX, gridZ - 1);
                boolean south = !hasBloodCell(component, gridX, gridZ + 1);
                boolean west = !hasBloodCell(component, gridX - 1, gridZ);
                boolean east = !hasBloodCell(component, gridX + 1, gridZ);
                double x0 = face.x + localX * gridSize;
                double x1 = x0 + gridSize;
                double z0 = face.z + localZ * gridSize;
                double z1 = z0 + gridSize;
                double width = Math.min(BLOOD_BORDER_WIDTH, gridSize * 0.4);

                if (north && west) {
                    addHorizontalBloodBorderPatch(
                            patches, face, contributors, phase,
                            x0, x0 + width, z0, z0 + width,
                            true, true, false, true);
                }
                if (north && east) {
                    addHorizontalBloodBorderPatch(
                            patches, face, contributors, phase,
                            x1 - width, x1, z0, z0 + width,
                            true, false, true, true);
                }
                if (south && east) {
                    addHorizontalBloodBorderPatch(
                            patches, face, contributors, phase,
                            x1 - width, x1, z1 - width, z1,
                            false, true, true, true);
                }
                if (south && west) {
                    addHorizontalBloodBorderPatch(
                            patches, face, contributors, phase,
                            x0, x0 + width, z1 - width, z1,
                            true, true, true, false);
                }
            }
        }
    }

    private static void prepareBloodBorderRuns(
            List<BloodPreparedPatch> patches, TerrainFace face,
            BloodComponent component, double phase, Direction edge) {
        int scale = component.gridScale;
        double gridSize = 1.0 / scale;
        double width = Math.min(BLOOD_BORDER_WIDTH, gridSize * 0.4);
        int baseX = face.x * scale;
        int baseZ = face.z * scale;
        boolean variableX = edge.getAxis() == Direction.Axis.Z;
        for (int fixed = 0; fixed < scale; fixed++) {
            int variable = 0;
            while (variable < scale) {
                int localX = variableX ? variable : fixed;
                int localZ = variableX ? fixed : variable;
                int gridX = baseX + localX;
                int gridZ = baseZ + localZ;
                int[] contributors = component.contributorsByCell.get(
                        packGridCell(gridX, gridZ));
                if (contributors == null
                        || !bloodEdgeExposed(component, gridX, gridZ, edge)) {
                    variable++;
                    continue;
                }
                int end = variable + 1;
                while (end < scale) {
                    int nextLocalX = variableX ? end : fixed;
                    int nextLocalZ = variableX ? fixed : end;
                    int nextGridX = baseX + nextLocalX;
                    int nextGridZ = baseZ + nextLocalZ;
                    if (!bloodEdgeExposed(
                            component, nextGridX, nextGridZ, edge)
                            || !Arrays.equals(contributors,
                            component.contributorsByCell.get(
                                    packGridCell(nextGridX, nextGridZ)))) {
                        break;
                    }
                    end++;
                }

                if (variableX) {
                    double x0 = face.x + variable * gridSize;
                    double x1 = face.x + end * gridSize;
                    double z0 = face.z + fixed * gridSize;
                    double z1 = z0 + gridSize;
                    if (bloodEdgeExposed(component,
                            baseX + variable, baseZ + fixed, Direction.WEST)) {
                        x0 += width;
                    }
                    if (bloodEdgeExposed(component,
                            baseX + end - 1, baseZ + fixed, Direction.EAST)) {
                        x1 -= width;
                    }
                    if (edge == Direction.NORTH) {
                        addHorizontalBloodBorderPatch(
                                patches, face, contributors, phase,
                                x0, x1, z0, z0 + width,
                                true, false, false, true);
                    } else {
                        addHorizontalBloodBorderPatch(
                                patches, face, contributors, phase,
                                x0, x1, z1 - width, z1,
                                false, true, true, false);
                    }
                } else {
                    double x0 = face.x + fixed * gridSize;
                    double x1 = x0 + gridSize;
                    double z0 = face.z + variable * gridSize;
                    double z1 = face.z + end * gridSize;
                    if (bloodEdgeExposed(component,
                            baseX + fixed, baseZ + variable, Direction.NORTH)) {
                        z0 += width;
                    }
                    if (bloodEdgeExposed(component,
                            baseX + fixed, baseZ + end - 1, Direction.SOUTH)) {
                        z1 -= width;
                    }
                    if (edge == Direction.WEST) {
                        addHorizontalBloodBorderPatch(
                                patches, face, contributors, phase,
                                x0, x0 + width, z0, z1,
                                true, true, false, false);
                    } else {
                        addHorizontalBloodBorderPatch(
                                patches, face, contributors, phase,
                                x1 - width, x1, z0, z1,
                                false, false, true, true);
                    }
                }
                variable = end;
            }
        }
    }

    private static boolean bloodEdgeExposed(
            BloodComponent component, int gridX, int gridZ, Direction edge) {
        return component.contributorsByCell.containsKey(packGridCell(gridX, gridZ))
                && !hasBloodCell(component,
                gridX + edge.getOffsetX(), gridZ + edge.getOffsetZ());
    }

    private static boolean hasBloodCell(
            BloodComponent component, int gridX, int gridZ) {
        return component.contributorsByCell.containsKey(
                packGridCell(gridX, gridZ));
    }

    private static void addHorizontalBloodBorderPatch(
            List<BloodPreparedPatch> patches, TerrainFace face,
            int[] contributors, double phase,
            double x0, double x1, double z0, double z1,
            boolean rim00, boolean rim01, boolean rim11, boolean rim10) {
        if (x1 - x0 <= 1.0E-5 || z1 - z0 <= 1.0E-5) {
            return;
        }
        double y = face.plane;
        patches.add(new BloodPreparedPatch(
                face, contributors,
                bloodBorderVertex(phase, x0, y, z0, rim00),
                bloodBorderVertex(phase, x0, y, z1, rim01),
                bloodBorderVertex(phase, x1, y, z1, rim11),
                bloodBorderVertex(phase, x1, y, z0, rim10),
                terrainLight(face, BLOOD_STAIN), BLOOD_BORDER_OFFSET));
    }

    private static int[] fullBloodBlockContributors(
            BloodComponent component, int baseX, int baseZ, int scale) {
        int[] contributors = component.contributorsByCell.get(
                packGridCell(baseX, baseZ));
        if (contributors == null) {
            return null;
        }
        for (int localZ = 0; localZ < scale; localZ++) {
            for (int localX = 0; localX < scale; localX++) {
                if (!Arrays.equals(contributors,
                        component.contributorsByCell.get(packGridCell(
                                baseX + localX, baseZ + localZ)))) {
                    return null;
                }
            }
        }
        return contributors;
    }

    private static BloodPreparedPatch horizontalBloodPatch(
            TerrainFace face, int[] contributors,
            double phase, double x0, double x1, double z0, double z1) {
        double y = face.plane;
        BloodPatchVertex v00 = bloodPatchVertex(
                phase, x0, y, z0);
        BloodPatchVertex v01 = bloodPatchVertex(
                phase, x0, y, z1);
        BloodPatchVertex v11 = bloodPatchVertex(
                phase, x1, y, z1);
        BloodPatchVertex v10 = bloodPatchVertex(
                phase, x1, y, z0);
        return new BloodPreparedPatch(
                face, contributors, v00, v01, v11, v10,
                terrainLight(face, BLOOD_STAIN), 0.0F);
    }

    private static void prepareBloodVerticalPatches(
            List<BloodPreparedPatch> fillPatches,
            List<BloodPreparedPatch> borderPatches, TerrainCache cache,
            TerrainFace face, BloodComponent component, double phase,
            double minY, double maxY) {
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
            return;
        }

        int scale = component.gridScale;
        double gridSize = 1.0 / scale;
        int segment = 0;
        while (segment < scale) {
            int[] contributors = verticalBloodContributors(
                    face, component, segment);
            if (contributors == null) {
                segment++;
                continue;
            }
            int end = segment + 1;
            while (end < scale && Arrays.equals(
                    contributors, verticalBloodContributors(face, component, end))) {
                end++;
            }
            boolean variableX = face.direction.getAxis() == Direction.Axis.Z;
            double variable0 = (variableX ? face.x : face.z) + segment * gridSize;
            double variable1 = (variableX ? face.x : face.z) + end * gridSize;
            fillPatches.add(verticalBloodPatch(
                    face, contributors, phase,
                    variableX, variable0, variable1, y0, y1));
            segment = end;
        }

        segment = 0;
        while (segment < scale) {
            int[] contributors = verticalBloodContributors(
                    face, component, segment);
            if (contributors == null
                    || !isVerticalBloodBoundary(face, component, segment)) {
                segment++;
                continue;
            }
            int end = segment + 1;
            while (end < scale
                    && isVerticalBloodBoundary(face, component, end)
                    && Arrays.equals(contributors,
                    verticalBloodContributors(face, component, end))) {
                end++;
            }
            boolean variableX = face.direction.getAxis() == Direction.Axis.Z;
            double variable0 = (variableX ? face.x : face.z) + segment * gridSize;
            double variable1 = (variableX ? face.x : face.z) + end * gridSize;
            borderPatches.add(verticalBloodBorderPatch(
                    face, contributors, phase, variableX,
                    variable0, variable1,
                    Math.max(y0, y1 - BLOOD_BORDER_WIDTH), y1));
            segment = end;
        }
    }

    private static int[] verticalBloodContributors(
            TerrainFace face, BloodComponent component, int segment) {
        int scale = component.gridScale;
        int baseX = face.x * scale;
        int baseZ = face.z * scale;
        int gridX;
        int gridZ;
        switch (face.direction) {
            case NORTH -> {
                gridX = baseX + segment;
                gridZ = baseZ;
            }
            case SOUTH -> {
                gridX = baseX + segment;
                gridZ = baseZ + scale - 1;
            }
            case WEST -> {
                gridX = baseX;
                gridZ = baseZ + segment;
            }
            case EAST -> {
                gridX = baseX + scale - 1;
                gridZ = baseZ + segment;
            }
            default -> {
                return null;
            }
        }
        return component.contributorsByCell.get(packGridCell(gridX, gridZ));
    }

    private static boolean isVerticalBloodBoundary(
            TerrainFace face, BloodComponent component, int segment) {
        int scale = component.gridScale;
        int baseX = face.x * scale;
        int baseZ = face.z * scale;
        return switch (face.direction) {
            case NORTH -> !hasBloodCell(
                    component, baseX + segment, baseZ - 1);
            case SOUTH -> !hasBloodCell(
                    component, baseX + segment, baseZ + scale);
            case WEST -> !hasBloodCell(
                    component, baseX - 1, baseZ + segment);
            case EAST -> !hasBloodCell(
                    component, baseX + scale, baseZ + segment);
            default -> false;
        };
    }

    private static BloodPreparedPatch verticalBloodPatch(
            TerrainFace face, int[] contributors,
            double phase, boolean variableX,
            double variable0, double variable1, double y0, double y1) {
        double x0;
        double x1;
        double z0;
        double z1;
        if (variableX) {
            x0 = variable0;
            x1 = variable1;
            z0 = z1 = face.plane;
        } else {
            x0 = x1 = face.plane;
            z0 = variable0;
            z1 = variable1;
        }
        BloodPatchVertex low0 = bloodPatchVertex(
                phase, x0, y0, z0);
        BloodPatchVertex high0 = bloodPatchVertex(
                phase, x0, y1, z0);
        BloodPatchVertex high1 = bloodPatchVertex(
                phase, x1, y1, z1);
        BloodPatchVertex low1 = bloodPatchVertex(
                phase, x1, y0, z1);
        if (face.direction == Direction.NORTH || face.direction == Direction.EAST) {
            return new BloodPreparedPatch(
                    face, contributors, low0, high0, high1, low1,
                    terrainLight(face, BLOOD_STAIN), 0.0F);
        }
        return new BloodPreparedPatch(
                face, contributors, low0, low1, high1, high0,
                terrainLight(face, BLOOD_STAIN), 0.0F);
    }

    private static BloodPreparedPatch verticalBloodBorderPatch(
            TerrainFace face, int[] contributors, double phase,
            boolean variableX, double variable0, double variable1,
            double y0, double y1) {
        double x0;
        double x1;
        double z0;
        double z1;
        if (variableX) {
            x0 = variable0;
            x1 = variable1;
            z0 = z1 = face.plane;
        } else {
            x0 = x1 = face.plane;
            z0 = variable0;
            z1 = variable1;
        }
        BloodPatchVertex low0 = bloodBorderVertex(
                phase, x0, y0, z0, false);
        BloodPatchVertex high0 = bloodBorderVertex(
                phase, x0, y1, z0, true);
        BloodPatchVertex high1 = bloodBorderVertex(
                phase, x1, y1, z1, true);
        BloodPatchVertex low1 = bloodBorderVertex(
                phase, x1, y0, z1, false);
        if (face.direction == Direction.NORTH || face.direction == Direction.EAST) {
            return new BloodPreparedPatch(
                    face, contributors, low0, high0, high1, low1,
                    terrainLight(face, BLOOD_STAIN), BLOOD_BORDER_OFFSET);
        }
        return new BloodPreparedPatch(
                face, contributors, low0, low1, high1, high0,
                terrainLight(face, BLOOD_STAIN), BLOOD_BORDER_OFFSET);
    }

    private static BloodPatchVertex bloodPatchVertex(
            double phase, double x, double y, double z) {
        return new BloodPatchVertex(
                x, y, z, bloodBaseTint(phase, x, z));
    }

    private static BloodPatchVertex bloodBorderVertex(
            double phase, double x, double y, double z, boolean rim) {
        return new BloodPatchVertex(
                x, y, z, rim
                ? bloodRimTint(phase, x, z)
                : bloodBaseTint(phase, x, z));
    }

    private static TintColor bloodBaseTint(
            double phase, double worldX, double worldZ) {
        return bloodSolidTint(
                BLOOD_STAIN.terrainRed,
                BLOOD_STAIN.terrainGreen,
                BLOOD_STAIN.terrainBlue,
                bloodTintVariation(phase, worldX, worldZ));
    }

    private static TintColor bloodRimTint(
            double phase, double worldX, double worldZ) {
        return bloodSolidTint(
                BLOOD_STAIN.bloodStyle.rimRed,
                BLOOD_STAIN.bloodStyle.rimGreen,
                BLOOD_STAIN.bloodStyle.rimBlue,
                bloodTintVariation(phase, worldX, worldZ));
    }

    private static double bloodTintVariation(
            double phase, double worldX, double worldZ) {
        return 1.0
                + 0.035 * Math.sin(worldX * 1.47 + worldZ * 0.73 + phase)
                + 0.025 * Math.sin(
                worldX * 0.51 - worldZ * 1.19 + phase * 1.7);
    }

    private static TintColor bloodSolidTint(
            int red, int green, int blue, double variation) {
        return new TintColor(
                MathHelper.clamp((int) Math.round(red * variation), 0, 255),
                MathHelper.clamp((int) Math.round(green * variation), 0, 255),
                MathHelper.clamp((int) Math.round(blue * variation), 0, 255));
    }

    private static void drawBloodGeometry(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices,
            VertexConsumerProvider vertexConsumers,
            BloodPreparedGeometry geometry, float[] sourceOpacities,
            boolean allOpaque) {
        VertexConsumer opaque = vertexConsumers.getBuffer(RenderLayer.getCutout());
        if (allOpaque) {
            for (BloodPreparedPatch patch : geometry.fillPatches) {
                drawBloodPatch(centerX, centerY, centerZ,
                        matrices, opaque, patch, 1.0F);
            }
            for (BloodPreparedPatch patch : geometry.borderPatches) {
                drawBloodPatch(centerX, centerY, centerZ,
                        matrices, opaque, patch, 1.0F);
            }
            return;
        }
        for (BloodPreparedPatch patch : geometry.fillPatches) {
            float opacity = bloodPatchOpacity(patch.contributors, sourceOpacities);
            if (opacity >= 0.999F) {
                drawBloodPatch(centerX, centerY, centerZ,
                        matrices, opaque, patch, 1.0F);
            }
        }
        for (BloodPreparedPatch patch : geometry.borderPatches) {
            float opacity = bloodPatchOpacity(patch.contributors, sourceOpacities);
            if (opacity >= 0.999F) {
                drawBloodPatch(centerX, centerY, centerZ,
                        matrices, opaque, patch, 1.0F);
            }
        }
        VertexConsumer translucent = null;
        for (BloodPreparedPatch patch : geometry.fillPatches) {
            float opacity = bloodPatchOpacity(patch.contributors, sourceOpacities);
            if (opacity > 0.01F && opacity < 0.999F) {
                if (translucent == null) {
                    translucent = vertexConsumers.getBuffer(
                            RenderLayer.getEntityTranslucent(
                                    SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                }
                drawBloodPatch(centerX, centerY, centerZ,
                        matrices, translucent, patch, opacity);
            }
        }
        for (BloodPreparedPatch patch : geometry.borderPatches) {
            float opacity = bloodPatchOpacity(patch.contributors, sourceOpacities);
            if (opacity > 0.01F && opacity < 0.999F) {
                if (translucent == null) {
                    translucent = vertexConsumers.getBuffer(
                            RenderLayer.getEntityTranslucent(
                                    SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                }
                drawBloodPatch(centerX, centerY, centerZ,
                        matrices, translucent, patch, opacity);
            }
        }
    }

    private static float bloodPatchOpacity(
            int[] contributors, float[] sourceOpacities) {
        float opacity = 0.0F;
        for (int contributor : contributors) {
            if (contributor >= 0 && contributor < sourceOpacities.length) {
                opacity = Math.max(opacity, sourceOpacities[contributor]);
            }
        }
        return opacity;
    }

    private static void drawBloodPatch(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            BloodPreparedPatch patch, float opacity) {
        TerrainFace face = patch.face;
        for (int layer = 0; layer < face.modelTextures.size(); layer++) {
            ModelTexture texture = face.modelTextures.get(layer);
            float normalOffset = FACE_OFFSET + patch.surfaceOffset
                    + layer * DETAIL_OFFSET;
            putBloodPatchVertex(centerX, centerY, centerZ,
                    matrices, vertices, face, texture,
                    patch.first, patch.light, opacity, normalOffset);
            putBloodPatchVertex(centerX, centerY, centerZ,
                    matrices, vertices, face, texture,
                    patch.second, patch.light, opacity, normalOffset);
            putBloodPatchVertex(centerX, centerY, centerZ,
                    matrices, vertices, face, texture,
                    patch.third, patch.light, opacity, normalOffset);
            putBloodPatchVertex(centerX, centerY, centerZ,
                    matrices, vertices, face, texture,
                    patch.fourth, patch.light, opacity, normalOffset);
        }
    }

    private static void putBloodPatchVertex(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, ModelTexture texture,
            BloodPatchVertex point, int light,
            float opacity, float normalOffset) {
        double first;
        double second;
        if (face.direction.getAxis() == Direction.Axis.Y) {
            first = point.x - face.x;
            second = point.z - face.z;
        } else if (face.direction.getAxis() == Direction.Axis.Z) {
            first = point.x - face.x;
            second = point.y - face.y;
        } else {
            first = point.z - face.z;
            second = point.y - face.y;
        }
        TextureCoordinates uv = texture.mapping.coordinates(first, second);
        Direction direction = face.direction;
        double localX = point.x - centerX + direction.getOffsetX() * normalOffset;
        double localY = point.y - centerY + direction.getOffsetY() * normalOffset;
        double localZ = point.z - centerZ + direction.getOffsetZ() * normalOffset;
        var vertex = vertices.vertex(
                        matrices, (float) localX, (float) localY, (float) localZ)
                .color(point.tint.red, point.tint.green, point.tint.blue,
                        MathHelper.clamp(Math.round(opacity * 255.0F), 0, 255))
                .texture(texture.sprite.getFrameU((float) uv.u),
                        texture.sprite.getFrameV((float) uv.v));
        if (opacity < 0.999F) {
            vertex.overlay(OverlayTexture.DEFAULT_UV);
        }
        vertex.light(light).normal(
                matrices, direction.getOffsetX(),
                direction.getOffsetY(), direction.getOffsetZ());
    }

    private static PreparedGeometry prepareGeometry(
            TerrainCache cache,
            double centerX, double centerY, double centerZ,
            SplatterShape shape, long shapeKey,
            double minY, double maxY) {
        PreparedSignature signature = new PreparedSignature(
                centerX, centerY, centerZ, shapeKey, minY, maxY);
        if (signature.equals(cache.preparedSignature)
                && cache.preparedGeometry != null) {
            return cache.preparedGeometry;
        }

        List<PreparedFace> faces = new ArrayList<>();
        for (SplatterPart part : shape.parts) {
            for (TerrainFace face : cache.faces) {
                PreparedFace prepared = face.direction.getAxis() == Direction.Axis.Y
                        ? prepareHorizontalFace(
                        centerX, centerZ, cache, face, part, minY, maxY)
                        : prepareVerticalFace(
                        centerX, centerZ, cache, face, part, minY, maxY);
                if (prepared != null) {
                    faces.add(prepared);
                }
            }
        }
        cache.preparedSignature = signature;
        cache.preparedGeometry = new PreparedGeometry(List.copyOf(faces));
        return cache.preparedGeometry;
    }

    private static TerrainCache buildCache(World world, int centerX, int centerY,
                                           int centerZ, int scanRadius,
                                           float verticalRange, Palette palette, long now) {
        List<TerrainFace> faces = new ArrayList<>();
        Map<FaceKey, TerrainFace> facesByKey = new HashMap<>();
        Map<Long, List<TerrainFace>> facesByColumn = new HashMap<>();
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
                        if (palette == BLOOD_STAIN && direction == Direction.DOWN) {
                            continue;
                        }
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
                                blockPos,
                                palette.useBlockModelTextures
                                        ? resolveModelTextures(state, blockPos, direction, bounds)
                                        : List.of()
                        );
                        faces.add(face);
                        facesByKey.put(new FaceKey(x, y, z, direction), face);
                        facesByColumn.computeIfAbsent(
                                packGridCell(x, z), ignored -> new ArrayList<>())
                                .add(face);
                    }
                }
            }
        }
        return new TerrainCache(
                world, centerX, centerY, centerZ, scanRadius,
                verticalRange, palette, now, faces, facesByKey,
                Map.copyOf(facesByColumn), fluidSurfaces);
    }

    private static List<ModelTexture> resolveModelTextures(
            BlockState state, BlockPos blockPos, Direction direction, Box bounds) {
        BakedModel model = MinecraftClient.getInstance()
                .getBlockRenderManager().getModel(state);
        long seed = state.getRenderingSeed(blockPos);
        List<ModelTexture> textures = new ArrayList<>();
        addModelTextures(textures, model.getQuads(
                state, direction, Random.create(seed)), direction, bounds);
        addModelTextures(textures, model.getQuads(
                state, null, Random.create(seed)), direction, bounds);
        if (textures.isEmpty()) {
            textures.add(new ModelTexture(
                    model.getParticleSprite(), FaceTextureMapping.fallback(direction, bounds)));
        }
        return List.copyOf(textures);
    }

    private static void addModelTextures(
            List<ModelTexture> textures, List<BakedQuad> quads,
            Direction direction, Box bounds) {
        for (BakedQuad quad : quads) {
            if (quad.getFace() != direction) {
                continue;
            }
            FaceTextureMapping mapping = FaceTextureMapping.from(quad, direction, bounds);
            if (mapping != null) {
                textures.add(new ModelTexture(quad.getSprite(), mapping));
            }
        }
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
            List<TerrainPoint> footprint, double minY, double maxY) {
        if (surface.top) {
            drawFluidTop(
                    centerX, centerY, centerZ,
                    matrices, vertices, sprites, surface,
                    footprint, minY, maxY
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
            SpriteSet sprites, FluidSurface surface, List<TerrainPoint> footprint,
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
                footprint
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
        vertices.vertex(matrices, (float) x, y, (float) z)
                .color(255, 255, 255, 255)
                .texture(sprite.getFrameU((float) u), sprite.getFrameV((float) v))
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(matrices, direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ());
    }

    private static PreparedHorizontalFace prepareHorizontalFace(
            double centerX, double centerZ, TerrainCache cache,
            TerrainFace face, SplatterPart part,
            double minY, double maxY) {
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
                centerX, centerZ, x0, x1, z0, z1, part.points);
        if (polygon.size() < 3) {
            return null;
        }
        boolean fullyCovered = containsConvex(
                part.points, face.x - centerX, face.z - centerZ)
                && containsConvex(part.points, face.x + 1.0 - centerX, face.z - centerZ)
                && containsConvex(part.points, face.x + 1.0 - centerX, face.z + 1.0 - centerZ)
                && containsConvex(part.points, face.x - centerX, face.z + 1.0 - centerZ);
        return new PreparedHorizontalFace(face, polygon, part, fullyCovered);
    }

    private static void drawHorizontalTextureFace(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer texturedVertices,
            SpriteSet sprites, Palette palette, float opacity,
            PreparedHorizontalFace prepared) {
        TerrainFace face = prepared.face;
        List<TerrainPoint> polygon = prepared.polygon;
        if (palette.useBlockModelTextures) {
            for (int i = 0; i < face.modelTextures.size(); i++) {
                ModelTexture texture = face.modelTextures.get(i);
                drawHorizontalModelTextureLayer(
                        centerX, centerY, centerZ, matrices, texturedVertices,
                        face, polygon, prepared.part, texture, palette, opacity,
                        FACE_OFFSET + i * DETAIL_OFFSET);
            }
            return;
        }
        Sprite foundation = sprites.topFoundation(
                face.direction == Direction.UP ? face.variant : 0);
        Sprite detail = face.direction == Direction.UP
                ? sprites.topDetail(face.variant)
                : null;
        drawHorizontalTextureLayer(
                centerX, centerY, centerZ, matrices, texturedVertices,
                face, polygon, foundation, palette, opacity, FACE_OFFSET);
        if (detail != null) {
            drawHorizontalTextureLayer(
                    centerX, centerY, centerZ, matrices, texturedVertices,
                    face, polygon, detail, palette, opacity, FACE_OFFSET + DETAIL_OFFSET);
        }
    }

    private static void drawHorizontalWashFace(
            double centerY, MatrixStack.Entry matrices, VertexConsumer washVertices,
            Palette palette, float opacity, PreparedHorizontalFace prepared) {
        drawHorizontalWashLayer(
                centerY, matrices, washVertices, prepared.face, prepared.polygon,
                palette, opacity,
                FACE_OFFSET + WASH_OFFSET);
    }

    private static List<TerrainPoint> clipHorizontalRectangle(
            double centerX, double centerZ,
            double worldX0, double worldX1, double worldZ0, double worldZ1,
            List<TerrainPoint> footprint) {
        double minX = worldX0 - centerX;
        double maxX = worldX1 - centerX;
        double minZ = worldZ0 - centerZ;
        double maxZ = worldZ1 - centerZ;
        List<TerrainPoint> polygon = new ArrayList<>(List.of(
                new TerrainPoint(minX, minZ),
                new TerrainPoint(maxX, minZ),
                new TerrainPoint(maxX, maxZ),
                new TerrainPoint(minX, maxZ)
        ));
        for (int i = 0; i < footprint.size() && !polygon.isEmpty(); i++) {
            polygon = clipPolygon(
                    polygon,
                    footprint.get(i), footprint.get((i + 1) % footprint.size())
            );
        }
        return polygon;
    }

    private static void drawHorizontalTextureLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, List<TerrainPoint> polygon, Sprite sprite,
            Palette palette, float opacity, float normalOffset) {
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
                    face, sprite, center, localY, light, palette, opacity);
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, first, localY, light, palette, opacity);
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, second, localY, light, palette, opacity);
            putHorizontalTextureVertex(centerX, centerZ, matrices, vertices,
                    face, sprite, second, localY, light, palette, opacity);
        }
    }

    private static void drawHorizontalModelTextureLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, List<TerrainPoint> polygon, SplatterPart part,
            ModelTexture texture,
            Palette palette, float opacity, float normalOffset) {
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
            putHorizontalModelTextureVertex(
                    centerX, centerZ, matrices, vertices, face, texture,
                    part, center, localY, light, palette, opacity);
            putHorizontalModelTextureVertex(
                    centerX, centerZ, matrices, vertices, face, texture,
                    part, first, localY, light, palette, opacity);
            putHorizontalModelTextureVertex(
                    centerX, centerZ, matrices, vertices, face, texture,
                    part, second, localY, light, palette, opacity);
            putHorizontalModelTextureVertex(
                    centerX, centerZ, matrices, vertices, face, texture,
                    part, second, localY, light, palette, opacity);
        }
    }

    private static void drawHorizontalWashLayer(
            double centerY, MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, List<TerrainPoint> polygon, Palette palette,
            float opacity, float normalOffset) {
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
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, first.x, localY, first.z,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, second.x, localY, second.z,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, second.x, localY, second.z,
                    light, face.direction, palette, opacity);
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
            float localY, int light, Palette palette, float opacity) {
        double worldX = point.x + centerX;
        double worldZ = point.z + centerZ;
        double textureU = MathHelper.clamp(worldX - face.x, 0.0, 1.0);
        double textureV = MathHelper.clamp(worldZ - face.z, 0.0, 1.0);
        TextureCoordinates uv = transformTopUv(textureU, textureV, face.uvTransform);
        putTextureVertex(matrices, vertices, sprite,
                point.x, localY, point.z, uv.u, uv.v,
                light, face.direction, palette, opacity);
    }

    private static void putHorizontalModelTextureVertex(
            double centerX, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, ModelTexture texture, SplatterPart part,
            TerrainPoint point,
            float localY, int light, Palette palette, float opacity) {
        double localX = point.x + centerX - face.x;
        double localZ = point.z + centerZ - face.z;
        TextureCoordinates uv = texture.mapping.coordinates(localX, localZ);
        putModelTextureVertex(matrices, vertices, texture.sprite,
                point.x, localY, point.z, uv.u, uv.v,
                light, face.direction, palette, opacity,
                point.x, point.z, centerX, centerZ, part);
    }

    private static PreparedVerticalFace prepareVerticalFace(
            double centerX, double centerZ, TerrainCache cache,
            TerrainFace face, SplatterPart part,
            double minY, double maxY) {
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

        double fixedDistance = face.plane - (xAxis ? centerZ : centerX);
        Span footprintSpan = footprintSpan(part.points, xAxis, fixedDistance);
        if (footprintSpan == null) {
            return null;
        }
        double variableCenter = xAxis ? centerX : centerZ;
        double clippedStart = Math.max(variableStart, variableCenter + footprintSpan.min);
        double clippedEnd = Math.min(variableEnd, variableCenter + footprintSpan.max);
        if (clippedEnd - clippedStart <= 1.0E-5) {
            return null;
        }
        boolean fullyCovered = clippedStart <= variableStart + SURFACE_EPSILON
                && clippedEnd >= variableEnd - SURFACE_EPSILON
                && y0 <= face.y + SURFACE_EPSILON
                && y1 >= face.y + 1.0 - SURFACE_EPSILON;
        return new PreparedVerticalFace(
                face, xAxis, clippedStart, clippedEnd, y0, y1,
                part, fullyCovered);
    }

    private static void drawVerticalTextureFace(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer texturedVertices,
            SpriteSet sprites, Palette palette, float opacity,
            PreparedVerticalFace prepared) {
        TerrainFace face = prepared.face;
        if (palette.useBlockModelTextures) {
            for (int i = 0; i < face.modelTextures.size(); i++) {
                ModelTexture texture = face.modelTextures.get(i);
                drawVerticalModelTextureLayer(
                        centerX, centerY, centerZ, matrices, texturedVertices,
                        face, texture, prepared.part, palette, prepared.xAxis,
                        prepared.clippedStart, prepared.clippedEnd,
                        prepared.y0, prepared.y1, opacity,
                        FACE_OFFSET + i * DETAIL_OFFSET);
            }
            return;
        }
        Sprite foundation = sprites.sideFoundation(face.variant);
        Sprite detail = sprites.sideDetail(face.variant);
        drawVerticalTextureLayer(
                centerX, centerY, centerZ, matrices, texturedVertices,
                face, foundation, palette, prepared.xAxis,
                prepared.clippedStart, prepared.clippedEnd,
                prepared.y0, prepared.y1, opacity, FACE_OFFSET);
        if (detail != null) {
            drawVerticalTextureLayer(
                    centerX, centerY, centerZ, matrices, texturedVertices,
                    face, detail, palette, prepared.xAxis,
                    prepared.clippedStart, prepared.clippedEnd,
                    prepared.y0, prepared.y1, opacity, FACE_OFFSET + DETAIL_OFFSET);
        }
    }

    private static void drawVerticalWashFace(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer washVertices,
            Palette palette, float opacity, PreparedVerticalFace prepared) {
        drawVerticalWashLayer(
                centerX, centerY, centerZ, matrices, washVertices,
                prepared.face, palette, prepared.xAxis,
                prepared.clippedStart, prepared.clippedEnd,
                prepared.y0, prepared.y1,
                opacity,
                FACE_OFFSET + WASH_OFFSET);
    }

    private static void drawVerticalTextureLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, Sprite sprite, Palette palette, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float opacity, float normalOffset) {
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
                    light, face.direction, palette, opacity);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y1, quad.z0, u0, v1,
                    light, face.direction, palette, opacity);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y1, quad.z1, u1, v1,
                    light, face.direction, palette, opacity);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y0, quad.z1, u1, v0,
                    light, face.direction, palette, opacity);
        } else {
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y0, quad.z0, u0, v0,
                    light, face.direction, palette, opacity);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y0, quad.z1, u1, v0,
                    light, face.direction, palette, opacity);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x1, quad.y1, quad.z1, u1, v1,
                    light, face.direction, palette, opacity);
            putTextureVertex(matrices, vertices, sprite,
                    quad.x0, quad.y1, quad.z0, u0, v1,
                    light, face.direction, palette, opacity);
        }
    }

    private static void drawVerticalModelTextureLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, ModelTexture texture, SplatterPart part,
            Palette palette, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float opacity, float normalOffset) {
        VerticalQuad quad = verticalQuad(
                centerX, centerY, centerZ, face, xAxis,
                variableStart, variableEnd, y0, y1, normalOffset);
        int light = terrainLight(face, palette);
        double first0 = variableStart - (xAxis ? face.x : face.z);
        double first1 = variableEnd - (xAxis ? face.x : face.z);
        double second0 = y0 - face.y;
        double second1 = y1 - face.y;
        TextureCoordinates uv00 = texture.mapping.coordinates(first0, second0);
        TextureCoordinates uv01 = texture.mapping.coordinates(first0, second1);
        TextureCoordinates uv11 = texture.mapping.coordinates(first1, second1);
        TextureCoordinates uv10 = texture.mapping.coordinates(first1, second0);

        if (face.direction == Direction.NORTH || face.direction == Direction.EAST) {
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x0, quad.y0, quad.z0, uv00.u, uv00.v,
                    light, face.direction, palette, opacity,
                    quad.x0, quad.z0, centerX, centerZ, part);
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x0, quad.y1, quad.z0, uv01.u, uv01.v,
                    light, face.direction, palette, opacity,
                    quad.x0, quad.z0, centerX, centerZ, part);
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x1, quad.y1, quad.z1, uv11.u, uv11.v,
                    light, face.direction, palette, opacity,
                    quad.x1, quad.z1, centerX, centerZ, part);
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x1, quad.y0, quad.z1, uv10.u, uv10.v,
                    light, face.direction, palette, opacity,
                    quad.x1, quad.z1, centerX, centerZ, part);
        } else {
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x0, quad.y0, quad.z0, uv00.u, uv00.v,
                    light, face.direction, palette, opacity,
                    quad.x0, quad.z0, centerX, centerZ, part);
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x1, quad.y0, quad.z1, uv10.u, uv10.v,
                    light, face.direction, palette, opacity,
                    quad.x1, quad.z1, centerX, centerZ, part);
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x1, quad.y1, quad.z1, uv11.u, uv11.v,
                    light, face.direction, palette, opacity,
                    quad.x1, quad.z1, centerX, centerZ, part);
            putModelTextureVertex(matrices, vertices, texture.sprite,
                    quad.x0, quad.y1, quad.z0, uv01.u, uv01.v,
                    light, face.direction, palette, opacity,
                    quad.x0, quad.z0, centerX, centerZ, part);
        }
    }

    private static void drawVerticalWashLayer(
            double centerX, double centerY, double centerZ,
            MatrixStack.Entry matrices, VertexConsumer vertices,
            TerrainFace face, Palette palette, boolean xAxis,
            double variableStart, double variableEnd, double y0, double y1,
            float opacity, float normalOffset) {
        VerticalQuad quad = verticalQuad(
                centerX, centerY, centerZ, face, xAxis,
                variableStart, variableEnd, y0, y1, normalOffset);
        int light = terrainLight(face, palette);
        if (face.direction == Direction.NORTH || face.direction == Direction.EAST) {
            putWashVertex(matrices, vertices, quad.x0, quad.y0, quad.z0,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, quad.x0, quad.y1, quad.z0,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, quad.x1, quad.y1, quad.z1,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, quad.x1, quad.y0, quad.z1,
                    light, face.direction, palette, opacity);
        } else {
            putWashVertex(matrices, vertices, quad.x0, quad.y0, quad.z0,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, quad.x1, quad.y0, quad.z1,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, quad.x1, quad.y1, quad.z1,
                    light, face.direction, palette, opacity);
            putWashVertex(matrices, vertices, quad.x0, quad.y1, quad.z0,
                    light, face.direction, palette, opacity);
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
            Direction direction, Palette palette, float opacity) {
        var vertex = vertices.vertex(matrices, (float) x, y, (float) z)
                .color(palette.terrainRed, palette.terrainGreen, palette.terrainBlue,
                        MathHelper.clamp(Math.round(255.0F * opacity), 0, 255))
                .texture(sprite.getFrameU((float) u), sprite.getFrameV((float) v));
        if (opacity < 0.999F) {
            vertex.overlay(OverlayTexture.DEFAULT_UV);
        }
        vertex.light(light)
                .normal(matrices, direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ());
    }

    private static void putModelTextureVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices, Sprite sprite,
            double x, float y, double z, double u, double v, int light,
            Direction direction, Palette palette, float opacity,
            double localX, double localZ, double centerX, double centerZ,
            SplatterPart part) {
        TintColor tint = bloodTint(
                palette, localX, localZ, centerX, centerZ, part);
        var vertex = vertices.vertex(matrices, (float) x, y, (float) z)
                .color(tint.red, tint.green, tint.blue,
                        MathHelper.clamp(Math.round(255.0F * opacity), 0, 255))
                .texture(sprite.getFrameU((float) u), sprite.getFrameV((float) v));
        if (opacity < 0.999F) {
            vertex.overlay(OverlayTexture.DEFAULT_UV);
        }
        vertex.light(light)
                .normal(matrices, direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ());
    }

    private static TintColor bloodTint(
            Palette palette, double localX, double localZ,
            double centerX, double centerZ, SplatterPart part) {
        if (palette.bloodStyle == null) {
            return new TintColor(
                    palette.terrainRed, palette.terrainGreen, palette.terrainBlue);
        }
        double worldX = centerX + localX;
        double worldZ = centerZ + localZ;
        double distance = distanceToBoundary(localX, localZ, part.points);
        double blend = MathHelper.clamp(
                distance / palette.bloodStyle.rimWidth, 0.0, 1.0);
        blend = blend * blend * (3.0 - 2.0 * blend);
        double variation = 1.0 + 0.035 * Math.sin(worldX * 1.47 + worldZ * 0.73 + part.phase)
                + 0.025 * Math.sin(worldX * 0.51 - worldZ * 1.19 + part.phase * 1.7);
        return new TintColor(
                tintChannel(palette.bloodStyle.rimRed, palette.terrainRed, blend, variation),
                tintChannel(palette.bloodStyle.rimGreen, palette.terrainGreen, blend, variation),
                tintChannel(palette.bloodStyle.rimBlue, palette.terrainBlue, blend, variation));
    }

    private static int tintChannel(
            int rim, int fill, double blend, double variation) {
        return MathHelper.clamp(
                (int) Math.round(MathHelper.lerp(blend, rim, fill) * variation),
                0, 255);
    }

    private static double distanceToBoundary(
            double x, double z, List<TerrainPoint> polygon) {
        double nearestSquared = Double.POSITIVE_INFINITY;
        for (int i = 0; i < polygon.size(); i++) {
            TerrainPoint first = polygon.get(i);
            TerrainPoint second = polygon.get((i + 1) % polygon.size());
            double dx = second.x - first.x;
            double dz = second.z - first.z;
            double lengthSquared = dx * dx + dz * dz;
            double t = lengthSquared <= 1.0E-9
                    ? 0.0
                    : MathHelper.clamp(
                    ((x - first.x) * dx + (z - first.z) * dz) / lengthSquared,
                    0.0, 1.0);
            double offsetX = x - (first.x + dx * t);
            double offsetZ = z - (first.z + dz * t);
            nearestSquared = Math.min(
                    nearestSquared, offsetX * offsetX + offsetZ * offsetZ);
        }
        return Math.sqrt(nearestSquared);
    }

    private static void putWashVertex(
            MatrixStack.Entry matrices, VertexConsumer vertices,
            double x, float y, double z, int light,
            Direction direction, Palette palette, float opacity) {
        vertices.vertex(matrices, (float) x, y, (float) z)
                .color(palette.washRed, palette.washGreen,
                        palette.washBlue, MathHelper.clamp(
                                Math.round(palette.washAlpha * opacity), 0, 255))
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices, direction.getOffsetX(),
                        direction.getOffsetY(), direction.getOffsetZ());
    }

    private static int terrainLight(TerrainFace face, Palette palette) {
        if (!palette.useBlockModelTextures
                && palette.variants.get(face.variant).emissive) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
        int light = palette == BLOOD_STAIN
                ? WorldRenderer.getLightmapCoordinates(
                face.world, face.blockPos.offset(face.direction))
                : WorldRenderer.getLightmapCoordinates(
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

    private static List<TerrainPoint> circleFootprint(float radius) {
        List<TerrainPoint> points = new ArrayList<>(CIRCLE_SEGMENTS);
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle = MathHelper.TAU * i / CIRCLE_SEGMENTS;
            points.add(new TerrainPoint(Math.cos(angle) * radius, Math.sin(angle) * radius));
        }
        return points;
    }

    private static SplatterShape bloodPoolShape(float radius, int seed) {
        List<TerrainPoint> candidates = new ArrayList<>(BLOOD_POOL_SEGMENTS);
        double phaseA = randomUnit(seed, 11) * MathHelper.TAU;
        double phaseB = randomUnit(seed, 17) * MathHelper.TAU;
        for (int i = 0; i < BLOOD_POOL_SEGMENTS; i++) {
            double angle = MathHelper.TAU * i / BLOOD_POOL_SEGMENTS;
            double lobe = Math.sin(angle * 3.0 + phaseA) * 0.065
                    + Math.sin(angle * 5.0 + phaseB) * 0.035;
            double jitter = (randomUnit(seed, 100 + i) - 0.5) * 0.045;
            double pointRadius = radius * MathHelper.clamp(
                    0.80 + lobe + jitter, 0.70, 0.91);
            candidates.add(new TerrainPoint(
                    Math.cos(angle) * pointRadius,
                    Math.sin(angle) * pointRadius));
        }

        List<SplatterPart> parts = new ArrayList<>();
        parts.add(new SplatterPart(
                convexHull(candidates), randomUnit(seed, 900) * MathHelper.TAU,
                1.0F));
        int droplets = 5 + Math.floorMod(seed, 4);
        for (int i = 0; i < droplets; i++) {
            double angle = MathHelper.TAU * randomUnit(seed, 200 + i);
            double dropletRadius = radius * (0.035 + randomUnit(seed, 300 + i) * 0.035);
            double distance = Math.min(
                    radius * (0.89 + randomUnit(seed, 400 + i) * 0.055),
                    radius * 0.985 - dropletRadius);
            parts.add(new SplatterPart(
                    dropletFootprint(
                            Math.cos(angle) * distance,
                            Math.sin(angle) * distance,
                            dropletRadius, seed, i),
                    randomUnit(seed, 500 + i) * MathHelper.TAU,
                    1.0F));
        }
        return new SplatterShape(List.copyOf(parts));
    }

    private static SplatterShape bloodTrailShape(
            float halfLength, float radius, float yaw, int seed) {
        int arcSegments = 8;
        List<TerrainPoint> candidates = new ArrayList<>(arcSegments * 2 + 8);
        for (int i = 0; i <= arcSegments; i++) {
            double angle = -Math.PI * 0.5 + Math.PI * i / arcSegments;
            double scale = 0.78 + randomUnit(seed, 600 + i) * 0.13;
            candidates.add(rotatePoint(
                    halfLength + Math.cos(angle) * radius * scale,
                    Math.sin(angle) * radius * scale,
                    Math.cos(yaw), Math.sin(yaw)));
        }
        for (int i = 0; i <= arcSegments; i++) {
            double angle = Math.PI * 0.5 + Math.PI * i / arcSegments;
            double scale = 0.75 + randomUnit(seed, 700 + i) * 0.14;
            candidates.add(rotatePoint(
                    -halfLength + Math.cos(angle) * radius * scale,
                    Math.sin(angle) * radius * scale,
                    Math.cos(yaw), Math.sin(yaw)));
        }
        for (int i = 1; i <= 3; i++) {
            double along = MathHelper.lerp(i / 4.0, -halfLength, halfLength);
            double upper = radius * (0.76 + randomUnit(seed, 800 + i) * 0.14);
            double lower = -radius * (0.76 + randomUnit(seed, 810 + i) * 0.14);
            candidates.add(rotatePoint(along, upper, Math.cos(yaw), Math.sin(yaw)));
            candidates.add(rotatePoint(along, lower, Math.cos(yaw), Math.sin(yaw)));
        }

        List<SplatterPart> parts = new ArrayList<>();
        parts.add(new SplatterPart(
                convexHull(candidates), randomUnit(seed, 990) * MathHelper.TAU,
                1.0F));
        int droplets = 3 + Math.floorMod(seed, 4);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        for (int i = 0; i < droplets; i++) {
            double along = MathHelper.lerp(
                    randomUnit(seed, 1000 + i), -halfLength, halfLength);
            double sideSign = (i & 1) == 0 ? 1.0 : -1.0;
            double dropletRadius = radius * (0.04 + randomUnit(seed, 1010 + i) * 0.035);
            double side = sideSign * Math.min(
                    radius * (0.89 + randomUnit(seed, 1020 + i) * 0.045),
                    radius * 0.985 - dropletRadius);
            TerrainPoint center = rotatePoint(along, side, cos, sin);
            parts.add(new SplatterPart(
                    dropletFootprint(
                            center.x, center.z, dropletRadius, seed, 20 + i),
                    randomUnit(seed, 1030 + i) * MathHelper.TAU,
                    1.0F));
        }
        return new SplatterShape(List.copyOf(parts));
    }

    private static List<TerrainPoint> dropletFootprint(
            double centerX, double centerZ, double radius, int seed, int index) {
        int segments = 8;
        List<TerrainPoint> points = new ArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            double angle = MathHelper.TAU * i / segments;
            double scale = 0.88 + randomUnit(seed, index * 37 + i) * 0.20;
            points.add(new TerrainPoint(
                    centerX + Math.cos(angle) * radius * scale,
                    centerZ + Math.sin(angle) * radius * scale));
        }
        return convexHull(points);
    }

    private static List<TerrainPoint> convexHull(List<TerrainPoint> source) {
        if (source.size() <= 3) {
            return List.copyOf(source);
        }
        List<TerrainPoint> points = new ArrayList<>(source);
        points.sort((first, second) -> {
            int x = Double.compare(first.x, second.x);
            return x != 0 ? x : Double.compare(first.z, second.z);
        });
        List<TerrainPoint> hull = new ArrayList<>(points.size() * 2);
        for (TerrainPoint point : points) {
            while (hull.size() >= 2 && cross(
                    hull.get(hull.size() - 1).x - hull.get(hull.size() - 2).x,
                    hull.get(hull.size() - 1).z - hull.get(hull.size() - 2).z,
                    point.x - hull.get(hull.size() - 1).x,
                    point.z - hull.get(hull.size() - 1).z) <= 0.0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }
        int lowerSize = hull.size();
        for (int i = points.size() - 2; i >= 0; i--) {
            TerrainPoint point = points.get(i);
            while (hull.size() > lowerSize && cross(
                    hull.get(hull.size() - 1).x - hull.get(hull.size() - 2).x,
                    hull.get(hull.size() - 1).z - hull.get(hull.size() - 2).z,
                    point.x - hull.get(hull.size() - 1).x,
                    point.z - hull.get(hull.size() - 1).z) <= 0.0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }
        hull.remove(hull.size() - 1);
        return List.copyOf(hull);
    }

    private static boolean containsConvex(
            List<TerrainPoint> polygon, double x, double z) {
        TerrainPoint point = new TerrainPoint(x, z);
        for (int i = 0; i < polygon.size(); i++) {
            if (!insideEdge(
                    point, polygon.get(i), polygon.get((i + 1) % polygon.size()))) {
                return false;
            }
        }
        return true;
    }

    private static double randomUnit(int seed, int salt) {
        int hash = coordinateHash(seed ^ salt * 0x632be5ab, salt, seed + salt * 31);
        return (hash & 0x7fffffff) / (double) Integer.MAX_VALUE;
    }

    private static long shapeKey(
            int type, float first, float second, float yaw, int seed) {
        long key = ((long) type << 60) ^ Integer.toUnsignedLong(seed);
        key = key * 31L + Float.floatToIntBits(first);
        key = key * 31L + Float.floatToIntBits(second);
        return key * 31L + Float.floatToIntBits(yaw);
    }

    private static List<TerrainPoint> capsuleFootprint(float halfLength, float radius, float yaw) {
        int arcSegments = CIRCLE_SEGMENTS / 2;
        List<TerrainPoint> points = new ArrayList<>(CIRCLE_SEGMENTS + 2);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        for (int i = 0; i <= arcSegments; i++) {
            double angle = -Math.PI * 0.5 + Math.PI * i / arcSegments;
            points.add(rotatePoint(halfLength + Math.cos(angle) * radius,
                    Math.sin(angle) * radius, cos, sin));
        }
        for (int i = 0; i <= arcSegments; i++) {
            double angle = Math.PI * 0.5 + Math.PI * i / arcSegments;
            points.add(rotatePoint(-halfLength + Math.cos(angle) * radius,
                    Math.sin(angle) * radius, cos, sin));
        }
        return points;
    }

    private static TerrainPoint rotatePoint(double x, double z, double cos, double sin) {
        return new TerrainPoint(x * cos - z * sin, x * sin + z * cos);
    }

    private static Span footprintSpan(List<TerrainPoint> footprint, boolean variableX,
                                      double fixedValue) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < footprint.size(); i++) {
            TerrainPoint first = footprint.get(i);
            TerrainPoint second = footprint.get((i + 1) % footprint.size());
            double firstFixed = variableX ? first.z : first.x;
            double secondFixed = variableX ? second.z : second.x;
            double firstVariable = variableX ? first.x : first.z;
            double secondVariable = variableX ? second.x : second.z;
            if (Math.abs(firstFixed - fixedValue) <= 1.0E-7) {
                min = Math.min(min, firstVariable);
                max = Math.max(max, firstVariable);
            }
            if ((firstFixed <= fixedValue && secondFixed >= fixedValue)
                    || (secondFixed <= fixedValue && firstFixed >= fixedValue)) {
                double denominator = secondFixed - firstFixed;
                double variable = Math.abs(denominator) <= 1.0E-9
                        ? firstVariable
                        : MathHelper.lerp((fixedValue - firstFixed) / denominator,
                        firstVariable, secondVariable);
                min = Math.min(min, variable);
                max = Math.max(max, variable);
            }
        }
        return Double.isFinite(min) && Double.isFinite(max) ? new Span(min, max) : null;
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
        private final boolean useBlockModelTextures;
        private final BloodStyle bloodStyle;
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
                    null,
                    false,
                    null
            );
        }

        private Palette(List<Variant> variants,
                        int terrainRed, int terrainGreen, int terrainBlue,
                        int washRed, int washGreen, int washBlue, int washAlpha,
                        int lightFloor, int refreshTicks, int movementMargin,
                        FluidReplacement fluidReplacement) {
            this(
                    variants,
                    terrainRed, terrainGreen, terrainBlue,
                    washRed, washGreen, washBlue, washAlpha,
                    lightFloor, refreshTicks, movementMargin,
                    fluidReplacement,
                    false,
                    null
            );
        }

        private Palette(List<Variant> variants,
                        int terrainRed, int terrainGreen, int terrainBlue,
                        int washRed, int washGreen, int washBlue, int washAlpha,
                        int lightFloor, int refreshTicks, int movementMargin,
                        FluidReplacement fluidReplacement,
                        boolean useBlockModelTextures,
                        BloodStyle bloodStyle) {
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
            this.useBlockModelTextures = useBlockModelTextures;
            this.bloodStyle = bloodStyle;
            this.totalWeight = variants.stream().mapToInt(Variant::weight).sum();
        }

        private static Palette blockTextureTint(
                int red, int green, int blue,
                BloodStyle bloodStyle,
                int refreshTicks, int movementMargin) {
            return new Palette(
                    List.of(),
                    red, green, blue,
                    0, 0, 0, 0,
                    0, refreshTicks, movementMargin,
                    null,
                    true,
                    bloodStyle
            );
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

    private record BloodStyle(
            int rimRed, int rimGreen, int rimBlue, float rimWidth) {
    }

    private record SpriteVariant(Sprite topFoundation, Sprite topDetail,
                                 Sprite sideFoundation, Sprite sideDetail) {
    }

    private record SpriteSet(List<SpriteVariant> variants,
                             Sprite fluidStill, Sprite fluidFlow) {
        private static final SpriteSet EMPTY =
                new SpriteSet(List.of(), null, null);

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
        private final List<ModelTexture> modelTextures;

        private TerrainFace(World world, int x, int y, int z, double plane,
                            Direction direction, int variant, int uvTransform,
                            BlockState state, BlockPos blockPos,
                            List<ModelTexture> modelTextures) {
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
            this.modelTextures = modelTextures;
        }

        private FaceKey key() {
            return new FaceKey(this.x, this.y, this.z, this.direction);
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
        private final Map<Long, List<TerrainFace>> facesByColumn;
        private final List<FluidSurface> fluidSurfaces;
        private long lastSeenTick;
        private PreparedSignature preparedSignature;
        private PreparedGeometry preparedGeometry;
        private BloodPreparedSignature bloodPreparedSignature;
        private BloodPreparedGeometry bloodPreparedGeometry;

        private TerrainCache(World world, int centerX, int centerY, int centerZ,
                             int scanRadius, float verticalRange, Palette palette,
                             long now, List<TerrainFace> faces,
                             Map<FaceKey, TerrainFace> facesByKey,
                             Map<Long, List<TerrainFace>> facesByColumn,
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
            this.facesByColumn = facesByColumn;
            this.fluidSurfaces = fluidSurfaces;
        }
    }

    private record FaceKey(int x, int y, int z, Direction direction) {
    }

    private interface PreparedFace {
        TerrainFace face();

        SplatterPart part();

        boolean fullyCovered();
    }

    private record PreparedHorizontalFace(
            TerrainFace face, List<TerrainPoint> polygon,
            SplatterPart part, boolean fullyCovered) implements PreparedFace {
    }

    private record PreparedVerticalFace(
            TerrainFace face, boolean xAxis,
            double clippedStart, double clippedEnd,
            double y0, double y1,
            SplatterPart part, boolean fullyCovered) implements PreparedFace {
    }

    private record PreparedGeometry(List<PreparedFace> faces) {
    }

    private record PreparedSignature(
            double centerX, double centerY, double centerZ,
            long shapeKey, double minY, double maxY) {
    }

    private record ShapeCache(long shapeKey, SplatterShape shape) {
    }

    private static final class BloodMaskCache {
        private final long maskKey;
        private final LongSet fineCells;
        private LongSet coarseCells;

        private BloodMaskCache(
                long maskKey, LongSet fineCells, LongSet coarseCells) {
            this.maskKey = maskKey;
            this.fineCells = fineCells;
            this.coarseCells = coarseCells;
        }
    }

    private record BloodSource(
            int index, UUID id, LongSet cells,
            double minY, double maxY, int age) {
    }

    private record BloodEntityState(
            UUID id, long maskKey, BloodMaskCache mask,
            double minY, double maxY, float opacity, int age) {
    }

    private record BloodComponent(
            UUID id, Long2ObjectOpenHashMap<int[]> contributorsByCell,
            LongSet blockColumns,
            int minGridX, int maxGridX, int minGridZ, int maxGridZ,
            double minY, double maxY, long signature, int gridScale) {
    }

    private record BloodSnapshot(
            World world, Map<UUID, BloodComponent> byMember,
            float[] sourceOpacities, boolean allOpaque) {
        private static final BloodSnapshot EMPTY =
                new BloodSnapshot(null, Map.of(), new float[0], true);
    }

    private record BloodPreparedSignature(
            double centerX, double centerY, double centerZ,
            long componentSignature, int gridScale,
            double minY, double maxY) {
    }

    private record BloodPreparedGeometry(
            List<BloodPreparedPatch> fillPatches,
            List<BloodPreparedPatch> borderPatches) {
    }

    private record BloodPreparedPatch(
            TerrainFace face, int[] contributors,
            BloodPatchVertex first, BloodPatchVertex second,
            BloodPatchVertex third, BloodPatchVertex fourth,
            int light, float surfaceOffset) {
    }

    private record BloodPatchVertex(
            double x, double y, double z, TintColor tint) {
    }

    private record SplatterShape(List<SplatterPart> parts) {
        private static SplatterShape single(List<TerrainPoint> points) {
            return new SplatterShape(List.of(
                    new SplatterPart(points, 0.0, 1.0F)));
        }
    }

    private record SplatterPart(
            List<TerrainPoint> points, double phase,
            float opacity) {
    }

    private record TerrainPoint(double x, double z) {
    }

    private record Span(double min, double max) {
    }

    private record TextureCoordinates(double u, double v) {
    }

    private record TintColor(int red, int green, int blue) {
    }

    private record ModelTexture(Sprite sprite, FaceTextureMapping mapping) {
    }

    private record FaceTextureMapping(
            double uConstant, double uFirst, double uSecond,
            double vConstant, double vFirst, double vSecond,
            double minFirst, double maxFirst,
            double minSecond, double maxSecond) {
        private static final double COVERAGE_EPSILON = 1.0E-4;

        private static FaceTextureMapping from(
                BakedQuad quad, Direction direction, Box bounds) {
            int[] data = quad.getVertexData();
            if (data.length < 24 || data.length % 4 != 0) {
                return null;
            }
            int stride = data.length / 4;
            if (stride < 6) {
                return null;
            }

            FaceBounds required = FaceBounds.of(direction, bounds);
            ModelVertex[] vertices = new ModelVertex[4];
            double actualMinFirst = Double.POSITIVE_INFINITY;
            double actualMaxFirst = Double.NEGATIVE_INFINITY;
            double actualMinSecond = Double.POSITIVE_INFINITY;
            double actualMaxSecond = Double.NEGATIVE_INFINITY;
            Sprite sprite = quad.getSprite();
            for (int i = 0; i < vertices.length; i++) {
                int offset = i * stride;
                double x = Float.intBitsToFloat(data[offset]);
                double y = Float.intBitsToFloat(data[offset + 1]);
                double z = Float.intBitsToFloat(data[offset + 2]);
                FaceCoordinates coordinates = FaceCoordinates.of(direction, x, y, z);
                double u = sprite.getFrameFromU(Float.intBitsToFloat(data[offset + 4]));
                double v = sprite.getFrameFromV(Float.intBitsToFloat(data[offset + 5]));
                vertices[i] = new ModelVertex(coordinates.first, coordinates.second, u, v);
                actualMinFirst = Math.min(actualMinFirst, coordinates.first);
                actualMaxFirst = Math.max(actualMaxFirst, coordinates.first);
                actualMinSecond = Math.min(actualMinSecond, coordinates.second);
                actualMaxSecond = Math.max(actualMaxSecond, coordinates.second);
            }
            if (actualMinFirst > required.minFirst + COVERAGE_EPSILON
                    || actualMaxFirst < required.maxFirst - COVERAGE_EPSILON
                    || actualMinSecond > required.minSecond + COVERAGE_EPSILON
                    || actualMaxSecond < required.maxSecond - COVERAGE_EPSILON) {
                return null;
            }

            ModelVertex origin = vertices[0];
            for (int first = 1; first < vertices.length; first++) {
                double firstDeltaA = vertices[first].first - origin.first;
                double firstDeltaB = vertices[first].second - origin.second;
                for (int second = first + 1; second < vertices.length; second++) {
                    double secondDeltaA = vertices[second].first - origin.first;
                    double secondDeltaB = vertices[second].second - origin.second;
                    double determinant = firstDeltaA * secondDeltaB
                            - secondDeltaA * firstDeltaB;
                    if (Math.abs(determinant) <= 1.0E-9) {
                        continue;
                    }
                    double uFirst = ((vertices[first].u - origin.u) * secondDeltaB
                            - (vertices[second].u - origin.u) * firstDeltaB) / determinant;
                    double uSecond = (firstDeltaA * (vertices[second].u - origin.u)
                            - secondDeltaA * (vertices[first].u - origin.u)) / determinant;
                    double vFirst = ((vertices[first].v - origin.v) * secondDeltaB
                            - (vertices[second].v - origin.v) * firstDeltaB) / determinant;
                    double vSecond = (firstDeltaA * (vertices[second].v - origin.v)
                            - secondDeltaA * (vertices[first].v - origin.v)) / determinant;
                    return new FaceTextureMapping(
                            origin.u - uFirst * origin.first - uSecond * origin.second,
                            uFirst, uSecond,
                            origin.v - vFirst * origin.first - vSecond * origin.second,
                            vFirst, vSecond,
                            required.minFirst, required.maxFirst,
                            required.minSecond, required.maxSecond);
                }
            }
            return null;
        }

        private static FaceTextureMapping fallback(Direction direction, Box bounds) {
            FaceBounds face = FaceBounds.of(direction, bounds);
            double firstRange = Math.max(1.0E-6, face.maxFirst - face.minFirst);
            double secondRange = Math.max(1.0E-6, face.maxSecond - face.minSecond);
            double uFirst = 1.0 / firstRange;
            double vSecond = direction.getAxis() == Direction.Axis.Y
                    ? 1.0 / secondRange
                    : -1.0 / secondRange;
            return new FaceTextureMapping(
                    -face.minFirst * uFirst, uFirst, 0.0,
                    direction.getAxis() == Direction.Axis.Y
                            ? -face.minSecond * vSecond
                            : 1.0 - face.minSecond * vSecond,
                    0.0, vSecond,
                    face.minFirst, face.maxFirst,
                    face.minSecond, face.maxSecond);
        }

        private TextureCoordinates coordinates(double first, double second) {
            first = MathHelper.clamp(first, minFirst, maxFirst);
            second = MathHelper.clamp(second, minSecond, maxSecond);
            return new TextureCoordinates(
                    MathHelper.clamp(uConstant + uFirst * first + uSecond * second, 0.0, 1.0),
                    MathHelper.clamp(vConstant + vFirst * first + vSecond * second, 0.0, 1.0));
        }
    }

    private record ModelVertex(double first, double second, double u, double v) {
    }

    private record FaceCoordinates(double first, double second) {
        private static FaceCoordinates of(
                Direction direction, double x, double y, double z) {
            return switch (direction.getAxis()) {
                case X -> new FaceCoordinates(z, y);
                case Y -> new FaceCoordinates(x, z);
                case Z -> new FaceCoordinates(x, y);
            };
        }
    }

    private record FaceBounds(
            double minFirst, double maxFirst,
            double minSecond, double maxSecond) {
        private static FaceBounds of(Direction direction, Box bounds) {
            return switch (direction.getAxis()) {
                case X -> new FaceBounds(
                        bounds.minZ, bounds.maxZ, bounds.minY, bounds.maxY);
                case Y -> new FaceBounds(
                        bounds.minX, bounds.maxX, bounds.minZ, bounds.maxZ);
                case Z -> new FaceBounds(
                        bounds.minX, bounds.maxX, bounds.minY, bounds.maxY);
            };
        }
    }

    private record VerticalQuad(double x0, float y0, double z0,
                                double x1, float y1, double z1) {
    }

}
