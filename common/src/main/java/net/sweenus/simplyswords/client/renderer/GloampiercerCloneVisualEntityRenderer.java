package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.sweenus.simplyswords.entity.GloampiercerCloneVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.joml.Matrix4f;

public final class GloampiercerCloneVisualEntityRenderer
        extends EntityRenderer<GloampiercerCloneVisualEntity> {
    private final ItemRenderer itemRenderer;

    public GloampiercerCloneVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(GloampiercerCloneVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(GloampiercerCloneVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(3.0));
    }

    @Override
    public void render(GloampiercerCloneVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        float age = entity.age + tickDelta;
        float fadeIn = MathHelper.clamp(age / 6.0F, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((entity.getLifetime() - age) / 7.0F, 0.0F, 1.0F);
        float opacity = fadeIn * fadeOut;
        if (opacity <= 0.01F) {
            return;
        }
        ThrowAnimation animation = throwAnimation(age, entity.getThrowTick(), entity.getThrowInterval());
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getVisualYaw()));
        matrices.translate(0.0, Math.sin(age * 0.2 + entity.getSeed()) * 0.045, 0.0);
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getDebugQuads());
        drawFigure(vertices, matrices, animation, opacity, entity.getSeed());
        renderSpear(entity, matrices, consumers, animation, opacity);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void renderSpear(GloampiercerCloneVisualEntity entity, MatrixStack matrices,
                             VertexConsumerProvider consumers, ThrowAnimation animation, float opacity) {
        boolean left = (entity.getSeed() & 1) != 0;
        matrices.push();
        matrices.translate(left ? -0.3 : 0.3, 1.36, 0.0);
        applyArmRotation(matrices, animation.armPitch, left ? -4.0F : 4.0F);
        matrices.translate(0.0, -0.57, -0.015);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-135.0F));
        float scale = 0.98F * Math.max(0.04F, opacity);
        matrices.scale(scale, scale, scale);
        ItemStack stack = ItemsRegistry.GLOAMPIERCER.get().getDefaultStack();
        BakedModel model = itemRenderer.getModels().getModel(stack);
        itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, false, matrices, consumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, model);
        matrices.pop();
    }

    private static void drawFigure(VertexConsumer vertices, MatrixStack matrices,
                                   ThrowAnimation animation, float opacity, int seed) {
        int innerAlpha = MathHelper.clamp(Math.round(205.0F * opacity), 0, 205);
        int rimAlpha = MathHelper.clamp(Math.round(82.0F * opacity), 0, 96);
        float sway = MathHelper.sin(seed * 0.13F) * 4.0F;
        drawPart(vertices, matrices, 0.0F, 1.09F, 0.0F,
                0.38F, 0.62F, 0.22F, 0.0F, 0.0F, sway, innerAlpha, rimAlpha);
        drawPart(vertices, matrices, 0.0F, 1.58F, 0.0F,
                0.34F, 0.34F, 0.34F, 0.0F, 0.0F, -sway * 0.45F, innerAlpha, rimAlpha);
        drawPart(vertices, matrices, -0.1F, 0.42F, 0.0F,
                0.16F, 0.72F, 0.18F, animation.intensity * 5.0F, 0.0F,
                -animation.intensity * 3.0F, innerAlpha, rimAlpha);
        drawPart(vertices, matrices, 0.1F, 0.42F, 0.0F,
                0.16F, 0.72F, 0.18F, -animation.intensity * 3.0F, 0.0F,
                animation.intensity * 3.0F, innerAlpha, rimAlpha);
        boolean left = (seed & 1) != 0;
        drawArm(vertices, matrices, left ? -0.3F : 0.3F, left,
                animation.armPitch, innerAlpha, rimAlpha);
        drawArm(vertices, matrices, left ? 0.3F : -0.3F, !left,
                -animation.intensity * 12.0F, innerAlpha, rimAlpha);
    }

    private static void drawArm(VertexConsumer vertices, MatrixStack matrices, float x,
                                boolean left, float pitch, int innerAlpha, int rimAlpha) {
        matrices.push();
        matrices.translate(x, 1.36, 0.0);
        applyArmRotation(matrices, pitch, left ? -4.0F : 4.0F);
        matrices.translate(0.0, -0.3, 0.0);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        drawBox(vertices, matrix, 0.215F, 0.645F, 0.225F,
                40, 224, 232, rimAlpha);
        drawBox(vertices, matrix, 0.17F, 0.6F, 0.18F,
                11, 4, 27, innerAlpha);
        matrices.pop();
    }

    private static void applyArmRotation(MatrixStack matrices, float pitch, float roll) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
    }

    private static void drawPart(VertexConsumer vertices, MatrixStack matrices,
                                 float x, float y, float z, float width, float height, float depth,
                                 float rotationX, float rotationY, float rotationZ,
                                 int innerAlpha, int rimAlpha) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZ));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        drawBox(vertices, matrix, width + 0.045F, height + 0.045F, depth + 0.045F,
                40, 224, 232, rimAlpha);
        drawBox(vertices, matrix, width, height, depth, 11, 4, 27, innerAlpha);
        matrices.pop();
    }

    private static void drawBox(VertexConsumer vertices, Matrix4f matrix,
                                float width, float height, float depth,
                                int red, int green, int blue, int alpha) {
        float x = width * 0.5F;
        float y = height * 0.5F;
        float z = depth * 0.5F;
        quad(vertices, matrix, -x, -y, z, x, -y, z, x, y, z, -x, y, z, red, green, blue, alpha);
        quad(vertices, matrix, x, -y, -z, -x, -y, -z, -x, y, -z, x, y, -z, red, green, blue, alpha);
        quad(vertices, matrix, -x, -y, -z, -x, -y, z, -x, y, z, -x, y, -z, red, green, blue, alpha);
        quad(vertices, matrix, x, -y, z, x, -y, -z, x, y, -z, x, y, z, red, green, blue, alpha);
        quad(vertices, matrix, -x, y, z, x, y, z, x, y, -z, -x, y, -z, red, green, blue, alpha);
        quad(vertices, matrix, -x, -y, -z, x, -y, -z, x, -y, z, -x, -y, z, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             int red, int green, int blue, int alpha) {
        vertex(vertices, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(vertices, matrix, x2, y2, z2, red, green, blue, alpha);
        vertex(vertices, matrix, x3, y3, z3, red, green, blue, alpha);
        vertex(vertices, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .next();
    }

    private static ThrowAnimation throwAnimation(float age, int firstThrowTick, int throwInterval) {
        float releaseTick = firstThrowTick;
        if (throwInterval > 0 && age > firstThrowTick) {
            int cycle = Math.max(0, Math.round((age - firstThrowTick) / throwInterval));
            releaseTick += cycle * throwInterval;
        }
        float lead = throwInterval > 0
                ? Math.min(5.0F, Math.max(1.0F, throwInterval * 0.55F))
                : 5.0F;
        float recovery = throwInterval > 0
                ? Math.min(4.0F, Math.max(0.75F, throwInterval * 0.4F))
                : 4.0F;
        float relative = age - releaseTick;
        if (relative < -lead || relative > recovery) {
            return new ThrowAnimation(0.0F, 0.0F);
        }
        float acceleration = Math.min(2.0F, lead * 0.45F);
        if (relative < -acceleration) {
            float progress = smooth((relative + lead) / Math.max(0.01F, lead - acceleration));
            return new ThrowAnimation(MathHelper.lerp(progress, 0.0F, -34.0F), progress * 0.45F);
        }
        if (relative < 0.0F) {
            float progress = smooth((relative + acceleration) / Math.max(0.01F, acceleration));
            return new ThrowAnimation(MathHelper.lerp(progress, -34.0F, 94.0F),
                    MathHelper.lerp(progress, 0.45F, 1.0F));
        }
        float progress = smooth(relative / Math.max(0.01F, recovery));
        return new ThrowAnimation(MathHelper.lerp(progress, 94.0F, 0.0F), 1.0F - progress);
    }

    private static float smooth(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private record ThrowAnimation(float armPitch, float intensity) {
    }
}
