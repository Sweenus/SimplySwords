package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().stretch(
                entity.getEnd().subtract(entity.getStart())).expand(3.0));
    }

    @Override
    public void render(LightningPhenomenonVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (!Config.general.enableModernFieldEffects) return;
        float age = entity.age + tickDelta - entity.getDelay();
        if (age < 0.0F || age > entity.getLifetime()) return;

        Vec3d entityPos = entity.getLerpedPos(tickDelta);
        Vec3d start = entity.getStart().subtract(entityPos);
        Vec3d end = entity.getEnd().subtract(entityPos);
        double length = start.distanceTo(end);
        if (length < 0.01) return;

        int detail = MathHelper.clamp(Config.general.stormEffectDetail, 0, 2);
        int segments = MathHelper.clamp(MathHelper.ceil((float) length * 0.72F) + detail - 1, 4, 14);
        float[] trunk = MinecraftLightningRenderer.generate(start, end, entity.getSeed(), age,
                Math.max(0.025F, entity.getSpread() * 0.32F), segments, 2.0F);
        VertexConsumer vertices = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float width = entity.getCoreWidth();

        switch (entity.getShape()) {
            case DIRECT_FLASH -> drawDirect(entity, vertices, matrix, trunk, age, width);
            case LEADER_RETURN -> drawLeaderReturn(entity, vertices, matrix, trunk, age, width);
            case SKY_STRIKE -> drawSkyStrike(entity, vertices, matrix, trunk, age, width);
            case CONDUCTIVE_LINK -> drawConductiveLink(entity, vertices, matrix, trunk, age, width);
        }
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
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

    private static float ease(float value) {
        return 1.0F - (1.0F - value) * (1.0F - value) * (1.0F - value);
    }
}
