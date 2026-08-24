package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.api.render.LightningPhenomenonShape;
import net.sweenus.simplyswords.client.render.BoltIntensity;
import net.sweenus.simplyswords.client.render.BoltPath;
import net.sweenus.simplyswords.client.render.LightningRenderLayers;
import net.sweenus.simplyswords.client.render.MinecraftLightningRenderer;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.LightningPhenomenonVisualEntity;
import org.joml.Matrix4f;

import java.util.Arrays;

public class LightningPhenomenonVisualEntityRenderer
        extends EntityRenderer<LightningPhenomenonVisualEntity> {

    public LightningPhenomenonVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(LightningPhenomenonVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(LightningPhenomenonVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) {
            return true;
        }
        LightningPhenomenonShape shape = entity.getShape();
        Vec3d start = usesInterpolatedAnchors(shape)
                ? resolveAnchor(entity, entity.getStartId(), entity.getStartOffset(), entity.getStart(), 1.0F)
                : entity.getStart();
        Vec3d end = usesInterpolatedAnchors(shape)
                ? resolveAnchor(entity, entity.getEndId(), entity.getEndOffset(), entity.getEnd(), 1.0F)
                : entity.getEnd();
        return frustum.isVisible(new Box(start, end).expand(3.0));
    }

    @Override
    public void render(LightningPhenomenonVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (!Config.general.enableModernFieldEffects) return;
        float age = entity.age + tickDelta - entity.getDelay();
        if (age < 0.0F || age > entity.getLifetime()) return;

        LightningPhenomenonShape shape = entity.getShape();
        Vec3d startWorld = usesInterpolatedAnchors(shape)
                ? resolveAnchor(entity, entity.getStartId(), entity.getStartOffset(), entity.getStart(), tickDelta)
                : entity.getStart();
        Vec3d endWorld = usesInterpolatedAnchors(shape)
                ? resolveAnchor(entity, entity.getEndId(), entity.getEndOffset(), entity.getEnd(), tickDelta)
                : entity.getEnd();
        Vec3d entityPos = entity.getLerpedPos(tickDelta);
        Vec3d start = startWorld.subtract(entityPos);
        Vec3d end = endWorld.subtract(entityPos);
        double length = start.distanceTo(end);
        if (length < 0.01) return;

        int detail = MathHelper.clamp(Config.general.stormEffectDetail, 0, 2);
        int segments = MathHelper.clamp(MathHelper.ceil((float) length * 0.72F) + detail - 1, 4, 14);
        float[] trunk = shape == LightningPhenomenonShape.ENERGY_LINK
                || shape == LightningPhenomenonShape.TRAVELLING_PULSE
                ? linearPath(start, end, segments)
                : MinecraftLightningRenderer.generate(start, end, entity.getSeed(), age,
                Math.max(0.025F, entity.getSpread() * 0.32F), segments, 2.0F);
        VertexConsumer vertices = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float width = entity.getCoreWidth();

        switch (entity.getShape()) {
            case DIRECT_FLASH -> drawDirect(entity, vertices, matrix, trunk, age, width);
            case LEADER_RETURN -> drawLeaderReturn(entity, vertices, matrix, trunk, age, width);
            case SKY_STRIKE -> drawSkyStrike(entity, vertices, matrix, trunk, age, width);
            case CONDUCTIVE_LINK -> drawConductiveLink(entity, vertices, matrix, trunk, age, width);
            case ENERGY_LINK -> drawEnergyLink(entity, vertices, matrix, trunk, age, width);
            case TRAVELLING_PULSE -> drawTravellingPulse(entity, vertices, matrix, trunk, age, width);
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static boolean usesInterpolatedAnchors(LightningPhenomenonShape shape) {
        return shape == LightningPhenomenonShape.ENERGY_LINK
                || shape == LightningPhenomenonShape.TRAVELLING_PULSE;
    }

    private static Vec3d resolveAnchor(LightningPhenomenonVisualEntity visual, int entityId,
                                       float yOffset, Vec3d fallback, float tickDelta) {
        if (entityId < 0) {
            return fallback;
        }
        Entity anchor = visual.getWorld().getEntityById(entityId);
        return anchor == null || anchor.isRemoved()
                ? fallback
                : anchor.getLerpedPos(tickDelta).add(0.0, yOffset, 0.0);
    }

    private static void drawConductiveLink(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                           Matrix4f matrix, float[] trunk, float age, float width) {
        float pulse = 0.58F + BoltIntensity.of(entity.getSeed(), age, 7.0F) * 0.34F;
        MinecraftLightningRenderer.draw(vertices, matrix, trunk, width, width * 0.72F,
                entity.getCoreColor(), entity.getPrimaryColor(), pulse, false);
        if (MathHelper.floor(age) % 5 < 3) {
            MinecraftLightningRenderer.drawBranches(vertices, matrix, trunk, entity.getSeed(), age,
                    Math.min(3, entity.getBranches()), Math.max(0.35F, entity.getSpread()), width * 0.42F,
                    entity.getCoreColor(), entity.getPrimaryColor(), pulse * 0.7F);
        }
    }

    private static void drawEnergyLink(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                       Matrix4f matrix, float[] trunk, float age, float width) {
        float shimmer = 0.76F + BoltIntensity.shimmer(entity.getSeed(), age) * 0.18F;
        MinecraftLightningRenderer.draw(vertices, matrix, trunk,
                width * 1.35F, width * 1.05F,
                entity.getCoreColor(), entity.getPrimaryColor(), shimmer, false);
        MinecraftLightningRenderer.draw(vertices, matrix, trunk,
                width * 0.52F, width * 0.42F,
                0xFFFFFF, entity.getCoreColor(), Math.min(1.0F, shimmer + 0.12F), false);
    }

    private static void drawTravellingPulse(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                            Matrix4f matrix, float[] trunk, float age, float width) {
        float progress = MathHelper.clamp(age / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float tailProgress = Math.max(0.0F, progress - 0.12F);
        float headProgress = Math.min(1.0F, progress + 0.035F);
        Vec3d tail = sample(trunk, tailProgress);
        Vec3d center = sample(trunk, progress);
        Vec3d head = sample(trunk, headProgress);
        float[] wake = path(tail, center);
        float[] crest = path(center, head);
        float shimmer = 0.86F + BoltIntensity.shimmer(entity.getSeed(), age) * 0.14F;

        MinecraftLightningRenderer.draw(vertices, matrix, wake,
                width * 0.35F, width * 2.8F,
                entity.getCoreColor(), entity.getPrimaryColor(), shimmer * 0.82F, false);
        MinecraftLightningRenderer.draw(vertices, matrix, crest,
                width * 2.8F, width * 0.55F,
                0xFFFFFF, entity.getPrimaryColor(), shimmer, false);
    }

    private static void drawDirect(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                   Matrix4f matrix, float[] trunk, float age, float width) {
        float intensity = flash(age, entity.getLifetime(), entity.getSeed());
        drawTrunk(entity, vertices, matrix, trunk, width, intensity, 1);
    }

    private static void drawLeaderReturn(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                         Matrix4f matrix, float[] trunk, float age, float width) {
        int leaderTicks = Math.max(1, entity.getLeaderTicks());
        if (age < leaderTicks) {
            float progress = ease(MathHelper.clamp(age / leaderTicks, 0.0F, 1.0F));
            float[] leader = prefix(trunk, progress);
            MinecraftLightningRenderer.draw(vertices, matrix, leader, width * 0.28F, width * 0.16F,
                    entity.getCoreColor(), entity.getPrimaryColor(),
                    0.28F + 0.18F * BoltIntensity.shimmer(entity.getSeed(), age), true);
            return;
        }
        float returnAge = age - leaderTicks;
        float intensity = flash(returnAge, Math.max(4.0F, entity.getLifetime() - leaderTicks), entity.getSeed()) * 1.35F;
        drawTrunk(entity, vertices, matrix, trunk, width * 1.45F, intensity, 2);
    }

    private static void drawSkyStrike(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                      Matrix4f matrix, float[] trunk, float age, float width) {
        float intensity = flash(age, entity.getLifetime(), entity.getSeed()) * 1.2F;
        drawTrunk(entity, vertices, matrix, trunk, width * 1.25F, intensity, 2);
    }

    private static void drawTrunk(LightningPhenomenonVisualEntity entity, VertexConsumer vertices,
                                  Matrix4f matrix, float[] trunk, float width,
                                  float intensity, int branchScale) {
        MinecraftLightningRenderer.draw(vertices, matrix, trunk, width, width * 0.42F,
                entity.getCoreColor(), entity.getPrimaryColor(), intensity, false);
        int branches = Math.min(6, entity.getBranches() / Math.max(1, 3 - branchScale));
        if (branches > 0) {
            int last = trunk.length - 3;
            double trunkLength = new Vec3d(trunk[0], trunk[1], trunk[2])
                    .distanceTo(new Vec3d(trunk[last], trunk[last + 1], trunk[last + 2]));
            float reach = (float) Math.max(0.45, trunkLength * MathHelper.clamp(entity.getSpread(), 0.08F, 0.5F) * 0.65);
            MinecraftLightningRenderer.drawBranches(vertices, matrix, trunk, entity.getSeed(), entity.age,
                    branches, reach, width * 0.55F,
                    entity.getCoreColor(), entity.getPrimaryColor(), intensity);
        }
    }

    private static float flash(float age, float lifetime, long seed) {
        float first = age < 2.2F ? 1.0F : 0.0F;
        float restrike = age >= 3.0F && age < Math.min(lifetime, 5.2F) ? 0.58F : 0.0F;
        float flicker = 0.82F + BoltPath.unit(seed, MathHelper.floor(age), 43L) * 0.18F;
        return (first + restrike) * flicker;
    }

    private static float[] prefix(float[] path, float progress) {
        int points = path.length / 3;
        int keep = Math.max(2, Math.min(points, 1 + Math.round((points - 1) * progress)));
        return Arrays.copyOf(path, keep * 3);
    }

    private static Vec3d sample(float[] path, float progress) {
        int points = path.length / 3;
        float scaled = MathHelper.clamp(progress, 0.0F, 1.0F) * (points - 1);
        int first = Math.min(points - 2, MathHelper.floor(scaled));
        int second = first + 1;
        float blend = scaled - first;
        int a = first * 3;
        int b = second * 3;
        return new Vec3d(
                MathHelper.lerp(blend, path[a], path[b]),
                MathHelper.lerp(blend, path[a + 1], path[b + 1]),
                MathHelper.lerp(blend, path[a + 2], path[b + 2])
        );
    }

    private static float[] path(Vec3d start, Vec3d end) {
        return new float[]{
                (float) start.x, (float) start.y, (float) start.z,
                (float) end.x, (float) end.y, (float) end.z
        };
    }

    private static float[] linearPath(Vec3d start, Vec3d end, int segments) {
        float[] path = new float[(segments + 1) * 3];
        for (int index = 0; index <= segments; index++) {
            double progress = index / (double) segments;
            Vec3d point = start.lerp(end, progress);
            int offset = index * 3;
            path[offset] = (float) point.x;
            path[offset + 1] = (float) point.y;
            path[offset + 2] = (float) point.z;
        }
        return path;
    }

    private static float ease(float value) {
        return 1.0F - (1.0F - value) * (1.0F - value) * (1.0F - value);
    }
}
