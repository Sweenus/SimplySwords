package net.sweenus.simplyswords.client.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.render.BoltPath;
import net.sweenus.simplyswords.client.render.LightningRenderLayers;
import net.sweenus.simplyswords.client.render.MinecraftLightningRenderer;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.AtmosphericVisualEntity;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AtmosphericVisualEntityRenderer extends EntityRenderer<AtmosphericVisualEntity> {

    private static final Identifier CLOUD = id("cloud_soft.png");

    private static final Identifier CRYSTAL_SPRITE = new Identifier("minecraft", "block/smooth_basalt");
    private static final Vec3d CRYSTAL_LIGHT = new Vec3d(0.3, 1.0, 0.2).normalize();
    private static final Vec3d[] CRYSTAL_POINTS = {
            new Vec3d(0.5, 0.0, 0.0), new Vec3d(-0.5, 0.0, 0.0),
            new Vec3d(0.0, 0.625, 0.0), new Vec3d(0.0, -0.625, 0.0),
            new Vec3d(0.0, 0.0, 0.5), new Vec3d(0.0, 0.0, -0.5)
    };
    private static final int[][] CRYSTAL_FACETS = {
            {0, 4, 2}, {4, 1, 2}, {1, 5, 2}, {5, 0, 2},
            {0, 4, 3}, {4, 1, 3}, {1, 5, 3}, {5, 0, 3}
    };
    private static final int[][] CRYSTAL_EDGES = {
            {0, 4}, {4, 1}, {1, 5}, {5, 0},
            {0, 2}, {4, 2}, {1, 2}, {5, 2},
            {0, 3}, {4, 3}, {1, 3}, {5, 3}
    };
    private static final Vec3d[] CRYSTAL_OFFSETS = {
            Vec3d.ZERO, new Vec3d(0.42, -0.12, 0.18), new Vec3d(-0.34, 0.22, -0.26)
    };
    private static final float[] CRYSTAL_SCALE = {1.0F, 0.72F, 0.38F};
    private static final float[] CRYSTAL_YAW = {0.0F, 35.0F, -50.0F};
    private static final float[] CRYSTAL_ROLL = {8.0F, 20.0F, -14.0F};

    private final Map<Integer, SurfaceCache> surfacePaths = new HashMap<>();

    public AtmosphericVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(AtmosphericVisualEntity entity) {
        return CLOUD;
    }

    @Override
    public boolean shouldRender(AtmosphericVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        float extent = Math.max(3.0F, Math.max(entity.getRadius(), entity.getVisualHeight()) + 3.0F);
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(extent));
    }

    @Override
    public void render(AtmosphericVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (!Config.general.enableModernFieldEffects) return;
        float age = entity.age + tickDelta;
        float life = Math.max(1.0F, entity.getLifetime());
        float fade = lifetimeFade(age, life);
        if (fade <= 0.0F) return;
        switch (entity.getKind()) {
            case AtmosphericVisualEntity.STORM_VOLUME -> renderVolume(entity, matrices, consumers, light, age, fade);
            case AtmosphericVisualEntity.SURFACE_DISCHARGE -> renderSurface(entity, tickDelta, matrices, consumers, age, fade);
            default -> { }
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void renderVolume(AtmosphericVisualEntity entity, MatrixStack matrices,
                              VertexConsumerProvider consumers, int light, float age, float fade) {
        switch (entity.getStormShape()) {
            case SHELF_CLOUD -> renderShelf(entity, matrices, consumers, light, age, fade);
            case LIGHTNING_CUBE -> renderLightningCube(entity, matrices, consumers, age, fade);
            case LODESTONE_CORE -> renderLodestoneCore(entity, matrices, consumers, light, age, fade);
            case ROD_STAKE -> renderRodStake(entity, matrices, consumers, light);
        }
    }

    private void renderLodestoneCore(AtmosphericVisualEntity entity, MatrixStack matrices,
                                     VertexConsumerProvider consumers, int light, float age, float fade) {
        float scale = Math.max(0.1F, entity.getRadius());
        float bob = MathHelper.sin(age * 0.08F) * 0.09F;
        Sprite sprite = MinecraftClient.getInstance().getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getSprite(CRYSTAL_SPRITE);
        VertexConsumer facets = consumers.getBuffer(
                RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        VertexConsumer edges = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        int facetLight = Math.max(light, 0x00C000C0);
        int crystals = detailCount(CRYSTAL_OFFSETS.length);

        matrices.push();
        matrices.translate(0.0, bob, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * entity.getParamA() * 3.0F));
        matrices.scale(scale, scale, scale);
        for (int i = 0; i < crystals; i++) {
            matrices.push();
            Vec3d offset = CRYSTAL_OFFSETS[i];
            matrices.translate(offset.x, offset.y, offset.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(CRYSTAL_YAW[i]));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(CRYSTAL_ROLL[i]));
            matrices.scale(CRYSTAL_SCALE[i], CRYSTAL_SCALE[i], CRYSTAL_SCALE[i]);
            drawCrystal(facets, matrices.peek(), sprite, facetLight, entity.getSeed() + i * 5471L, fade);
            if (i < 2) drawCrystalEdges(edges, matrices.peek().getPositionMatrix(),
                    entity.getSeed() + i * 977L, age, fade);
            matrices.pop();
        }
        matrices.pop();
    }

    private static void drawCrystal(VertexConsumer vertices, MatrixStack.Entry entry, Sprite sprite,
                                    int light, long seed, float fade) {
        for (int facet = 0; facet < CRYSTAL_FACETS.length; facet++) {
            int[] corners = CRYSTAL_FACETS[facet];
            Vec3d a = CRYSTAL_POINTS[corners[0]];
            Vec3d b = CRYSTAL_POINTS[corners[1]];
            Vec3d c = CRYSTAL_POINTS[corners[2]];
            Vec3d centroid = a.add(b).add(c).multiply(1.0 / 3.0);
            Vec3d normal = b.subtract(a).crossProduct(c.subtract(a)).normalize();
            if (normal.dotProduct(centroid) < 0.0) normal = normal.multiply(-1.0);
            float lit = (float) Math.max(0.0, normal.dotProduct(CRYSTAL_LIGHT));
            float shade = MathHelper.clamp(0.28F + 1.05F * lit
                    + (BoltPath.unit(seed, facet, 23L) - 0.5F) * 0.24F, 0.12F, 0.94F);
            boolean flip = BoltPath.unit(seed, facet, 47L) > 0.5F;
            float u0 = flip ? sprite.getMaxU() : sprite.getMinU();
            float u1 = flip ? sprite.getMinU() : sprite.getMaxU();
            crystalFacet(vertices, entry, a, b, c, normal, u0, sprite.getMinV(), u1, sprite.getMaxV(),
                    light, shade, fade);
        }
    }

    private static void crystalFacet(VertexConsumer vertices, MatrixStack.Entry entry,
                                     Vec3d a, Vec3d b, Vec3d c, Vec3d normal,
                                     float u0, float v0, float u1, float v1,
                                     int light, float shade, float alpha) {
        float midU = (u0 + u1) * 0.5F;
        crystalVertex(vertices, entry, a, u0, v1, light, shade, alpha, normal, false);
        crystalVertex(vertices, entry, b, u1, v1, light, shade, alpha, normal, false);
        crystalVertex(vertices, entry, c, midU, v0, light, shade, alpha, normal, false);
        crystalVertex(vertices, entry, c, midU, v0, light, shade, alpha, normal, false);
        crystalVertex(vertices, entry, c, midU, v0, light, shade, alpha, normal, true);
        crystalVertex(vertices, entry, c, midU, v0, light, shade, alpha, normal, true);
        crystalVertex(vertices, entry, b, u1, v1, light, shade, alpha, normal, true);
        crystalVertex(vertices, entry, a, u0, v1, light, shade, alpha, normal, true);
    }

    private static void crystalVertex(VertexConsumer vertices, MatrixStack.Entry entry, Vec3d point,
                                      float u, float v, int light, float shade, float alpha,
                                      Vec3d normal, boolean back) {
        float sign = back ? -1.0F : 1.0F;
        vertices.vertex(entry.getPositionMatrix(), (float) point.x, (float) point.y, (float) point.z)
                .color(shade, shade, shade * 1.06F, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry.getNormalMatrix(), (float) normal.x * sign, (float) normal.y * sign, (float) normal.z * sign)
                .next();
    }

    private static void drawCrystalEdges(VertexConsumer vertices, Matrix4f matrix, long seed,
                                         float age, float fade) {
        for (int i = 0; i < CRYSTAL_EDGES.length; i++) {
            int[] edge = CRYSTAL_EDGES[i];
            float[] path = MinecraftLightningRenderer.generate(CRYSTAL_POINTS[edge[0]], CRYSTAL_POINTS[edge[1]],
                    seed * 131L + i * 3821L, age, 0.01F, 2, 3.0F);
            MinecraftLightningRenderer.draw(vertices, matrix, path, 0.030F, 0.012F,
                    0xF7FDFF, 0xC8DCE6, 0.5F * fade, false);
        }
    }

    private void renderRodStake(AtmosphericVisualEntity entity, MatrixStack matrices,
                                VertexConsumerProvider consumers, int light) {
        float scale = Math.max(0.1F, entity.getRadius());
        matrices.push();
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, 0.0, -0.5);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.LIGHTNING_ROD.getDefaultState(), matrices, consumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    private void renderLightningCube(AtmosphericVisualEntity entity, MatrixStack matrices,
                                     VertexConsumerProvider consumers, float age, float fade) {
        float half = entity.getRadius();
        float halfY = entity.getVisualHeight() * 0.5F;
        Vec3d[] corners = {
                new Vec3d(-half, -halfY, -half), new Vec3d(half, -halfY, -half),
                new Vec3d(half, -halfY, half), new Vec3d(-half, -halfY, half),
                new Vec3d(-half, halfY, -half), new Vec3d(half, halfY, -half),
                new Vec3d(half, halfY, half), new Vec3d(-half, halfY, half)
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        VertexConsumer vertices = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float pulse = (0.68F + BoltPath.unit(entity.getSeed(), MathHelper.floor(age / 2.0F), 71L) * 0.32F) * fade;
        for (int i = 0; i < edges.length; i++) {
            long seed = entity.getSeed() * 127L + i * 8191L;
            float[] path = MinecraftLightningRenderer.generate(corners[edges[i][0]], corners[edges[i][1]],
                    seed, age, 0.07F, 5, 3.0F);
            MinecraftLightningRenderer.draw(vertices, matrix, path, 0.038F, 0.025F,
                    entity.getSecondaryColor(), entity.getPrimaryColor(), pulse, false);
        }
    }

    private void renderShelf(AtmosphericVisualEntity entity, MatrixStack matrices,
                             VertexConsumerProvider consumers, int light, float age, float fade) {
        int count = detailCount(entity.getDensity());
        double yaw = Math.toRadians(entity.getYaw());
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        for (int i = 0; i < count; i++) {
            long seed = entity.getSeed() * 53L + i * 4513L;
            double x = (unit(seed, 1) - 0.5) * entity.getRadius() * 2.0;
            double y = entity.getVisualHeight() * (0.38 + unit(seed, 2) * 0.48);
            double z = (unit(seed, 3) - 0.5) * Math.max(1.2F, entity.getRadius() * 0.34F);
            Vec3d offset = new Vec3d(x * cosYaw - z * sinYaw, y, x * sinYaw + z * cosYaw);
            float size = 1.15F + unit(seed, 4) * 0.85F;
            boolean flash = internalFlash(seed, age, 15);
            crossedSprite(entity, matrices, consumers, CLOUD, offset,
                    size * (i % 5 == 0 ? 1.45F : 1.12F), size,
                    entity.getYaw() + MathHelper.floor(unit(seed, 5) * 4.0F) * 45.0F,
                    flash ? entity.getSecondaryColor() : entity.getPrimaryColor(),
                    fade * (flash ? 0.72F : 0.58F), flash ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light, flash);
        }
    }

    private void renderSurface(AtmosphericVisualEntity entity, float tickDelta, MatrixStack matrices,
                               VertexConsumerProvider consumers, float age, float fade) {
        Vec3d originWorld = entity.getLerpedPos(tickDelta);
        Vec3d endWorld = entity.getEnd();
        Vec3d direction = endWorld.subtract(originWorld);
        double length = direction.horizontalLength();
        if (length < 0.1) return;
        long epoch = MathHelper.floor(age / Math.max(1.0F, entity.getIntB()));
        SurfaceCache cache = surfacePaths.get(entity.getId());
        if (cache == null || cache.epoch != epoch || !cache.origin.isInRange(originWorld, 0.1)
                || !cache.end.isInRange(endWorld, 0.1)) {
            cache = buildSurfacePaths(entity, originWorld, endWorld, length, epoch);
            if (surfacePaths.size() > 128) surfacePaths.clear();
            surfacePaths.put(entity.getId(), cache);
        }

        float grow = ease(MathHelper.clamp(age / Math.max(1.0F, entity.getIntA()), 0.0F, 1.0F));
        VertexConsumer vertices = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float pulseAge = positiveModulo(age, Math.max(1.0F, entity.getIntB()));
        float pulse = (pulseAge < 2.0F ? 1.0F : 0.52F) * fade;
        for (float[] fullPath : cache.paths) {
            float[] path = prefix(fullPath, grow);
            MinecraftLightningRenderer.draw(vertices, matrix, path,
                    entity.getRadius(), entity.getRadius() * 0.45F,
                    entity.getSecondaryColor(), entity.getPrimaryColor(), pulse, true);
        }
    }

    private SurfaceCache buildSurfacePaths(AtmosphericVisualEntity entity, Vec3d originWorld,
                                           Vec3d endWorld, double length, long epoch) {
        Vec3d direction = endWorld.subtract(originWorld);
        Vec3d forward = new Vec3d(direction.x / length, 0.0, direction.z / length);
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);
        int count = detailCount(entity.getDensity());
        float[][] paths = new float[count][];
        int segments = MathHelper.clamp(MathHelper.ceil((float) length * 0.9F), 5, 14);
        for (int i = 0; i < count; i++) {
            long seed = entity.getSeed() * 113L + i * 9341L;
            float ratio = count == 1 ? 0.0F : i / (float) (count - 1) - 0.5F;
            double reach = length * (0.65 + unit(seed, 1) * 0.35);
            Vec3d end = forward.multiply(reach).add(side.multiply(ratio * entity.getVisualHeight() * length));
            float[] path = MinecraftLightningRenderer.generate(new Vec3d(0.0, 0.06, 0.0), end,
                    seed, epoch * Math.max(1.0F, entity.getIntB()),
                    Math.max(0.025F, entity.getVisualHeight() * 0.16F), segments, Math.max(1.0F, entity.getIntB()));
            projectToTerrain(entity, originWorld, path);
            paths[i] = path;
        }
        return new SurfaceCache(epoch, originWorld, endWorld, paths);
    }

    private static void projectToTerrain(AtmosphericVisualEntity entity, Vec3d originWorld, float[] path) {
        for (int i = 0; i < path.length; i += 3) {
            double x = originWorld.x + path[i];
            double z = originWorld.z + path[i + 2];
            Vec3d from = new Vec3d(x, originWorld.y + 2.5, z);
            Vec3d to = new Vec3d(x, originWorld.y - 4.5, z);
            BlockHitResult hit = entity.getWorld().raycast(new RaycastContext(from, to,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) {
                path[i + 1] = (float) (hit.getPos().y - originWorld.y + 0.055);
            }
        }
    }

    private void crossedSprite(AtmosphericVisualEntity entity, MatrixStack matrices,
                               VertexConsumerProvider consumers, Identifier texture, Vec3d offset,
                               float width, float height, float yawDegrees,
                               int color, float alpha, int light, boolean emissive) {
        float visibleAlpha = alpha * cameraFade(entity, offset);
        if (visibleAlpha <= 0.01F) return;
        matrices.push();
        matrices.translate(offset.x, offset.y, offset.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDegrees));
        renderPlane(matrices, consumers, texture, width, height, color, visibleAlpha, light, emissive);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        renderPlane(matrices, consumers, texture, width, height, color, visibleAlpha * 0.88F, light, emissive);
        matrices.pop();
    }

    private static void renderPlane(MatrixStack matrices, VertexConsumerProvider consumers,
                                    Identifier texture, float width, float height, int color,
                                    float alpha, int light, boolean emissive) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = consumers.getBuffer(emissive
                ? RenderLayer.getEntityTranslucentEmissive(texture)
                : RenderLayer.getEntityTranslucent(texture));
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        int a = MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255);
        int usedLight = emissive ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
        vertex(vertices, matrix, -width * 0.5F, height * 0.5F, 0.0F, 0.0F, 0.0F, r, g, b, a, usedLight);
        vertex(vertices, matrix, width * 0.5F, height * 0.5F, 0.0F, 1.0F, 0.0F, r, g, b, a, usedLight);
        vertex(vertices, matrix, width * 0.5F, -height * 0.5F, 0.0F, 1.0F, 1.0F, r, g, b, a, usedLight);
        vertex(vertices, matrix, -width * 0.5F, -height * 0.5F, 0.0F, 0.0F, 1.0F, r, g, b, a, usedLight);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
                               float u, float v, int r, int g, int b, int a, int light) {
        vertices.vertex(matrix, x, y, z).color(r, g, b, a).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0.0F, 1.0F, 0.0F).next();
    }

    private float cameraFade(AtmosphericVisualEntity entity, Vec3d offset) {
        Vec3d worldPoint = entity.getPos().add(offset);
        double distance = this.dispatcher.camera.getPos().distanceTo(worldPoint);
        return MathHelper.clamp((float) ((distance - 1.15) / 1.5), 0.0F, 1.0F);
    }

    private static boolean internalFlash(long seed, float age, int period) {
        int tick = MathHelper.floor(age);
        int phase = Math.floorMod(tick + (int) (seed & 7L), period);
        return phase <= 1 && unit(seed, tick / period + 19L) > 0.55F;
    }

    private static int detailCount(int requested) {
        int detail = MathHelper.clamp(Config.general.stormEffectDetail, 0, 2);
        float scale = detail == 0 ? 0.34F : detail == 1 ? 0.64F : 1.0F;
        return Math.max(1, Math.round(requested * scale));
    }

    private static float lifetimeFade(float age, float life) {
        float attack = MathHelper.clamp(age / 5.0F, 0.0F, 1.0F);
        float release = MathHelper.clamp((life - age) / Math.min(20.0F, life * 0.22F), 0.0F, 1.0F);
        return attack * release;
    }

    private static float[] prefix(float[] path, float progress) {
        int points = path.length / 3;
        int keep = Math.max(2, Math.min(points, 1 + Math.round((points - 1) * progress)));
        return Arrays.copyOf(path, keep * 3);
    }

    private static float ease(float value) { return 1.0F - (1.0F - value) * (1.0F - value) * (1.0F - value); }
    private static float positiveModulo(float value, float divisor) { float result = value % divisor; return result < 0.0F ? result + divisor : result; }
    private static float unit(long seed, long salt) { return BoltPath.unit(seed, salt, 0L); }
    private static Identifier id(String name) { return new Identifier(SimplySwords.MOD_ID, "textures/effect/storm/" + name); }

    private record SurfaceCache(long epoch, Vec3d origin, Vec3d end, float[][] paths) {
    }
}
