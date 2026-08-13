package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.client.render.IonDeferredRenderController;
import net.sweenus.simplyswords.client.render.MinecraftLightningRenderer;
import net.sweenus.simplyswords.entity.IonboundStormscaleVisualEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class IonboundStormscaleVisualEntityRenderer extends EntityRenderer<IonboundStormscaleVisualEntity> {
    private static final Identifier WHITE = new Identifier("textures/misc/white.png");
    private static final int DARK_BLUE = 0x102C5A;
    private static final int ION_BLUE = 0x2F8CFF;
    private static final int PALE_BLUE = 0xBCEEFF;
    private static final int WHITE_BLUE = 0xF7FDFF;
    private static final int[][] CUBE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    public IonboundStormscaleVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override public Identifier getTexture(IonboundStormscaleVisualEntity entity) { return WHITE; }

    @Override
    public boolean shouldRender(IonboundStormscaleVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(IonboundStormscaleVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        Entity owner = entity.getOwner();
        boolean ownerAnchored = owner != null && (entity.getKind() == IonboundStormscaleVisualEntity.ORBIT
                || entity.getKind() == IonboundStormscaleVisualEntity.SHIELD
                || entity.getKind() == IonboundStormscaleVisualEntity.BEAM);
        if (ownerAnchored) {
            Vec3d offset = owner.getLerpedPos(tickDelta).subtract(entity.getLerpedPos(tickDelta));
            matrices.push();
            matrices.translate(offset.x, offset.y, offset.z);
        }
        switch (entity.getKind()) {
            case IonboundStormscaleVisualEntity.ORBIT -> renderOrbit(entity, age, matrices, consumers);
            case IonboundStormscaleVisualEntity.SHIELD -> renderShield(entity, age, matrices, consumers);
            case IonboundStormscaleVisualEntity.CORRIDOR -> renderCorridor(entity, age, matrices, consumers);
            case IonboundStormscaleVisualEntity.BEAM -> renderBeam(entity, owner, age, tickDelta, matrices, consumers);
            default -> { }
        }
        if (ownerAnchored) matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static void renderOrbit(IonboundStormscaleVisualEntity entity, float age,
                                    MatrixStack matrices, VertexConsumerProvider consumers) {
        int cubes = entity.getCubes();
        Entity owner = entity.getOwner();
        float ownerHeight = owner == null ? 1.8F : Math.max(0.8F, owner.getHeight());
        float ownerWidth = owner == null ? 0.6F : Math.max(0.4F, owner.getWidth());
        for (int i = 0; i < cubes; i++) {
            long cubeSeed = entity.getSeed() + i * 7919L;
            float phase = unitNoise(cubeSeed) * MathHelper.TAU + i * MathHelper.TAU / 3.0F;
            float angleSpeed = 0.023F + unitNoise(cubeSeed + 19L) * 0.013F;
            float angle = phase + age * angleSpeed;
            float radius = Math.max(1.15F, ownerWidth * 0.65F + 0.8F)
                    + unitNoise(cubeSeed + 41L) * 0.30F;
            float baseHeight = ownerHeight * (0.32F + unitNoise(cubeSeed + 73L) * 0.68F);
            float driftAmplitude = ownerHeight * (0.12F + unitNoise(cubeSeed + 101L) * 0.10F);
            float driftSpeed = 0.021F + unitNoise(cubeSeed + 137L) * 0.018F;
            float driftPhase = unitNoise(cubeSeed + 173L) * MathHelper.TAU;
            float y = MathHelper.clamp(baseHeight + MathHelper.sin(age * driftSpeed + driftPhase) * driftAmplitude,
                    ownerHeight * 0.22F, ownerHeight * 1.18F);
            matrices.push();
            matrices.translate(MathHelper.cos(angle) * radius, y, MathHelper.sin(angle) * radius);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age * (1.7F + i * 0.31F) + i * 47.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * (2.1F + i * 0.27F) + i * 83.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * (1.35F + i * 0.23F) + i * 29.0F));
            renderCube(matrices, consumers, 0.18F, 0.18F, age, cubeSeed, 0.88F, 34);
            matrices.pop();
        }
    }

    private static float unitNoise(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return (mixed & 0xFFFFFFL) / (float) 0x1000000;
    }

    private static void renderShield(IonboundStormscaleVisualEntity entity, float age,
                                     MatrixStack matrices, VertexConsumerProvider consumers) {
        float life = Math.max(1.0F, entity.getLifetime());
        float appear = MathHelper.clamp(age / 5.0F, 0.0F, 1.0F);
        float fade = MathHelper.clamp((life - age) / 7.0F, 0.0F, 1.0F);
        float scale = (0.45F + appear * 0.55F) * fade;
        matrices.push();
        matrices.translate(0.0, entity.getVisualHeight() * 0.5F - 0.15F, 0.0);
        matrices.scale(scale, scale, scale);
        renderCube(matrices, consumers, entity.getVisualWidth() * 0.58F,
                entity.getVisualHeight() * 0.5F, age, entity.getSeed(), 0.95F, 48);
        matrices.pop();
    }

    private static void renderCube(MatrixStack matrices, VertexConsumerProvider consumers,
                                   float half, float halfY, float age, long seed,
                                   float alpha, int faceAlpha) {
        Vec3d[] p = cubeCorners(half, halfY);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer faces = IonDeferredRenderController.getFieldBuffer();
        quad(faces, matrix, p[0], p[1], p[5], p[4], DARK_BLUE, faceAlpha);
        quad(faces, matrix, p[2], p[3], p[7], p[6], DARK_BLUE, faceAlpha);
        quad(faces, matrix, p[1], p[2], p[6], p[5], ION_BLUE, faceAlpha / 2);
        quad(faces, matrix, p[3], p[0], p[4], p[7], ION_BLUE, faceAlpha / 2);
        quad(faces, matrix, p[4], p[5], p[6], p[7], PALE_BLUE, faceAlpha / 2);

        VertexConsumer edges = IonDeferredRenderController.getLightningBuffer();
        for (int i = 0; i < CUBE_EDGES.length; i++) {
            int[] edge = CUBE_EDGES[i];
            float[] path = MinecraftLightningRenderer.generate(p[edge[0]], p[edge[1]],
                    seed * 131L + i * 8191L, age, 0.018F, 2, 3.0F);
            MinecraftLightningRenderer.draw(edges, matrix, path, 0.032F, 0.014F,
                    WHITE_BLUE, ION_BLUE, alpha, false);
        }
    }

    private static void renderCorridor(IonboundStormscaleVisualEntity entity, float age,
                                       MatrixStack matrices, VertexConsumerProvider consumers) {
        float materialize = Math.max(1, entity.getMaterializeTicks());
        float holdEnds = materialize + Math.max(1, entity.getHoldTicks());
        float closeTicks = Math.max(1, entity.getCloseTicks());
        float rise = ease(MathHelper.clamp(age / materialize, 0.0F, 1.0F));
        float close = ease(MathHelper.clamp((age - holdEnds) / closeTicks, 0.0F, 1.0F));
        float halfWidth = entity.getVisualWidth() * 0.5F * (1.0F - close);
        float height = entity.getVisualHeight() * rise;
        float sink = (1.0F - rise) * entity.getVisualHeight();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        matrices.translate(0.0, -sink, 0.0);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int segments = Math.max(4, MathHelper.ceil(entity.getLength() / 1.5F));
        float segmentLength = entity.getLength() / segments;
        VertexConsumer faces = IonDeferredRenderController.getFieldBuffer();
        for (int wall = -1; wall <= 1; wall += 2) {
            float x = halfWidth * wall;
            for (int i = 0; i < segments; i++) {
                float z0 = i * segmentLength + 0.025F;
                float z1 = (i + 1) * segmentLength - 0.025F;
                int color = (i + wall & 1) == 0 ? DARK_BLUE : 0x153B78;
                quad(faces, matrix, new Vec3d(x, 0.0, z0), new Vec3d(x, 0.0, z1),
                        new Vec3d(x, height, z1), new Vec3d(x, height, z0), color, 66);
            }
        }

        VertexConsumer lightning = IonDeferredRenderController.getLightningBuffer();
        for (int wall = -1; wall <= 1; wall += 2) {
            float x = halfWidth * wall;
            drawBolt(lightning, matrix, new Vec3d(x, 0.0, 0.0), new Vec3d(x, height, 0.0),
                    entity.getSeed() + wall * 31L, age, 0.038F, 0.93F);
            drawBolt(lightning, matrix, new Vec3d(x, 0.0, entity.getLength()),
                    new Vec3d(x, height, entity.getLength()), entity.getSeed() + wall * 67L, age, 0.038F, 0.93F);
            for (int i = 0; i <= segments; i++) {
                float z = i * segmentLength;
                drawBolt(lightning, matrix, new Vec3d(x, 0.0, z), new Vec3d(x, height, z),
                        entity.getSeed() + wall * 101L + i * 3571L, age, 0.024F, 0.62F);
            }
            for (int row = 0; row <= 2; row++) {
                float y = height * row / 2.0F;
                drawBolt(lightning, matrix, new Vec3d(x, y, 0.0), new Vec3d(x, y, entity.getLength()),
                        entity.getSeed() + wall * 151L + row * 7283L, age, 0.030F, 0.75F);
            }
        }
        matrices.pop();
    }

    private static void renderBeam(IonboundStormscaleVisualEntity entity, Entity owner, float age, float tickDelta,
                                   MatrixStack matrices, VertexConsumerProvider consumers) {
        float life = Math.max(1.0F, entity.getLifetime());
        float appear = ease(MathHelper.clamp(age / 3.0F, 0.0F, 1.0F));
        float fade = ease(MathHelper.clamp((life - age) / 7.0F, 0.0F, 1.0F));
        float half = entity.getVisualWidth() * 0.5F * appear;
        float ownerHeight = owner == null ? Math.max(0.8F, entity.getVisualHeight()) : Math.max(0.8F, owner.getHeight());
        float beamYaw = owner == null ? entity.getYaw(tickDelta) : owner.getYaw(tickDelta);
        float beamPitch = owner == null ? entity.getPitch(tickDelta) : owner.getPitch(tickDelta);
        matrices.push();
        if (isFirstPersonOwner(owner)) {
            MinecraftClient client = MinecraftClient.getInstance();
            Vec3d cameraOffset = client.gameRenderer.getCamera().getPos()
                    .subtract(owner.getLerpedPos(tickDelta));
            Vector3f cameraUp = client.gameRenderer.getCamera().getVerticalPlane();
            matrices.translate(cameraOffset.x - cameraUp.x * 0.9F,
                    cameraOffset.y - cameraUp.y * 0.9F,
                    cameraOffset.z - cameraUp.z * 0.9F);
        } else {
            matrices.translate(0.0, ownerHeight * 0.68F, 0.0);
        }
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-beamYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(beamPitch));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer core = IonDeferredRenderController.getFieldBuffer();
        quad(core, matrix, new Vec3d(-half, -half, 0.0), new Vec3d(half, -half, 0.0),
                new Vec3d(half, -half, entity.getLength()), new Vec3d(-half, -half, entity.getLength()),
                PALE_BLUE, Math.round(205 * fade));
        quad(core, matrix, new Vec3d(-half, half, 0.0), new Vec3d(-half, half, entity.getLength()),
                new Vec3d(half, half, entity.getLength()), new Vec3d(half, half, 0.0),
                WHITE_BLUE, Math.round(235 * fade));
        quad(core, matrix, new Vec3d(-half, -half, 0.0), new Vec3d(-half, -half, entity.getLength()),
                new Vec3d(-half, half, entity.getLength()), new Vec3d(-half, half, 0.0),
                ION_BLUE, Math.round(190 * fade));
        quad(core, matrix, new Vec3d(half, -half, 0.0), new Vec3d(half, half, 0.0),
                new Vec3d(half, half, entity.getLength()), new Vec3d(half, -half, entity.getLength()),
                ION_BLUE, Math.round(190 * fade));

        for (int i = 0; i < 3; i++) {
            float travel = ((age * 0.48F + i * entity.getLength() / 3.0F) % entity.getLength());
            renderBeamCharge(core, matrix, travel, Math.max(half * 1.5F, 0.22F),
                    Math.max(0.12F, entity.getVisualWidth() * 0.18F), Math.round(220 * fade));
        }

        VertexConsumer arcs = IonDeferredRenderController.getLightningBuffer();
        for (int i = 0; i < 5; i++) {
            float x = MathHelper.lerp(i / 4.0F, -half, half);
            float yy = (i & 1) == 0 ? -half * 0.55F : half * 0.55F;
            drawBolt(arcs, matrix, new Vec3d(x, yy, 0.0), new Vec3d(x, yy, entity.getLength()),
                    entity.getSeed() + i * 9151L, age * 1.8F, 0.042F, fade);
        }
        matrices.pop();
    }

    private static boolean isFirstPersonOwner(Entity owner) {
        MinecraftClient client = MinecraftClient.getInstance();
        return owner != null && client.player == owner && client.getCameraEntity() == owner && client.options != null
                && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    private static void renderBeamCharge(VertexConsumer vertices, Matrix4f matrix, float z,
                                         float radius, float depth, int alpha) {
        float z0 = Math.max(0.0F, z - depth);
        float z1 = z + depth;
        quad(vertices, matrix, new Vec3d(-radius, -radius, z0), new Vec3d(radius, -radius, z0),
                new Vec3d(radius, radius, z0), new Vec3d(-radius, radius, z0), WHITE_BLUE, alpha);
        quad(vertices, matrix, new Vec3d(-radius, -radius, z1), new Vec3d(-radius, radius, z1),
                new Vec3d(radius, radius, z1), new Vec3d(radius, -radius, z1), PALE_BLUE, alpha);
        quad(vertices, matrix, new Vec3d(-radius, -radius, z0), new Vec3d(-radius, -radius, z1),
                new Vec3d(-radius, radius, z1), new Vec3d(-radius, radius, z0), ION_BLUE, alpha / 2);
        quad(vertices, matrix, new Vec3d(radius, -radius, z0), new Vec3d(radius, radius, z0),
                new Vec3d(radius, radius, z1), new Vec3d(radius, -radius, z1), ION_BLUE, alpha / 2);
    }

    private static Vec3d[] cubeCorners(float half, float halfY) {
        return new Vec3d[]{
                new Vec3d(-half, -halfY, -half), new Vec3d(half, -halfY, -half),
                new Vec3d(half, -halfY, half), new Vec3d(-half, -halfY, half),
                new Vec3d(-half, halfY, -half), new Vec3d(half, halfY, -half),
                new Vec3d(half, halfY, half), new Vec3d(-half, halfY, half)
        };
    }

    private static void drawBolt(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                                 long seed, float age, float width, float alpha) {
        float[] path = MinecraftLightningRenderer.generate(start, end, seed, age, 0.035F, 4, 3.0F);
        MinecraftLightningRenderer.draw(vertices, matrix, path, width, width * 0.42F,
                WHITE_BLUE, ION_BLUE, alpha, false);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix, Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                             int color, int alpha) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        vertices.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(red, green, blue, alpha).next();
        vertices.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(red, green, blue, alpha).next();
        vertices.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(red, green, blue, alpha).next();
        vertices.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(red, green, blue, alpha).next();
    }

    private static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
