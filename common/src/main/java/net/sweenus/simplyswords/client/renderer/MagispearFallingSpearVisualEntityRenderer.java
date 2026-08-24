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
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.MagispearFallingSpearVisualEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.joml.Matrix4f;

public class MagispearFallingSpearVisualEntityRenderer
        extends EntityRenderer<MagispearFallingSpearVisualEntity> {

    private final ItemRenderer itemRenderer;

    public MagispearFallingSpearVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(MagispearFallingSpearVisualEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(MagispearFallingSpearVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        double length = new Vec3d(entity.getEndOffsetX(), entity.getEndOffsetY(), entity.getEndOffsetZ()).length();
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(Math.max(4.0, length)));
    }

    @Override
    public void render(MagispearFallingSpearVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float progress = MathHelper.clamp(
                (entity.age + tickDelta) / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float eased = entity.getMode() == MagispearFallingSpearVisualEntity.MODE_LAUNCH
                ? 1.0F - (float) Math.pow(1.0F - progress, 2.0)
                : progress * progress;
        Vec3d path = new Vec3d(entity.getEndOffsetX(), entity.getEndOffsetY(), entity.getEndOffsetZ());
        Vec3d current = path.multiply(eased);
        float fade = MathHelper.clamp((1.0F - progress) * 1.8F, 0.2F, 1.0F);
        int packedLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        drawTrail(matrices, vertexConsumers, path, current, progress, fade, entity.getMode(), packedLight);
        renderSpear(matrices, vertexConsumers, path, current, entity.getScale(), entity.getMode(), packedLight);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void renderSpear(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                             Vec3d path, Vec3d current, float scale, int mode, int light) {
        Vec3d direction = path.lengthSquared() < 0.001 ? new Vec3d(0.0, -1.0, 0.0) : path.normalize();
        float horizontal = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float visualYaw = (float) -Math.toDegrees(Math.atan2(direction.x, direction.z));
        float visualPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
        float modeScale = mode == MagispearFallingSpearVisualEntity.MODE_FINAL ? 2.0F
                : mode == MagispearFallingSpearVisualEntity.MODE_LAUNCH ? 1.30F : 1.50F;

        ItemStack stack = ItemsRegistry.MAGISPEAR.get().getDefaultStack();
        BakedModel model = this.itemRenderer.getModels().getModel(stack);
        matrices.push();
        matrices.translate(current.x, current.y, current.z);
        matrices.scale(scale * modeScale, scale * modeScale, scale * modeScale);
        if (mode == MagispearFallingSpearVisualEntity.MODE_LAUNCH) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(visualYaw));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(visualPitch - 45.0F));
        } else {
            // Generated item models are nearly flat. Facing the camera keeps each falling spear
            // readable from every angle, while the roll points the Magispear tip downward.
            matrices.multiply(this.dispatcher.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-135.0F));
        }
        this.itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, false, matrices,
                vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);
        matrices.pop();
    }

    private static void drawTrail(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                  Vec3d path, Vec3d current, float progress, float fade,
                                  int mode, int light) {
        if (path.lengthSquared() < 0.001) {
            return;
        }
        Vec3d direction = path.normalize();
        double trailLength = mode == MagispearFallingSpearVisualEntity.MODE_FINAL ? 4.5
                : mode == MagispearFallingSpearVisualEntity.MODE_LAUNCH ? 3.0 : 2.4;
        double modelClearance = mode == MagispearFallingSpearVisualEntity.MODE_FINAL ? 0.95
                : mode == MagispearFallingSpearVisualEntity.MODE_LAUNCH ? 0.55 : 0.70;
        double travelled = path.length() * progress;
        double visibleTrailLength = Math.min(trailLength, Math.max(0.0, travelled - modelClearance));
        if (visibleTrailLength <= 0.01) {
            return;
        }
        Vec3d trailEnd = current.subtract(direction.multiply(modelClearance));
        Vec3d trailStart = trailEnd.subtract(direction.multiply(visibleTrailLength));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        drawRibbon(vertices, matrix, trailStart, trailEnd, 0.20F, 105, 36, 220,
                MathHelper.clamp(Math.round(110.0F * fade), 0, 150), light);
        drawRibbon(vertices, matrix, trailStart, trailEnd, 0.07F, 220, 238, 255,
                MathHelper.clamp(Math.round(225.0F * fade), 0, 245), light);
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                                   float halfWidth, int red, int green, int blue, int alpha, int light) {
        Vec3d line = end.subtract(start);
        if (line.lengthSquared() < 0.001 || alpha <= 0) {
            return;
        }
        Vec3d direction = line.normalize();
        Vec3d side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 0.001) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3d up = direction.crossProduct(side).normalize();
        drawQuad(vertices, matrix, start, end, side.multiply(halfWidth), red, green, blue, alpha, light);
        drawQuad(vertices, matrix, start, end, up.multiply(halfWidth), red, green, blue, alpha, light);
    }

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                                 Vec3d offset, int red, int green, int blue, int alpha, int light) {
        vertex(vertices, matrix, start.add(offset), red, green, blue, alpha, light);
        vertex(vertices, matrix, start.subtract(offset), red, green, blue, alpha, light);
        vertex(vertices, matrix, end.subtract(offset), red, green, blue, alpha, light);
        vertex(vertices, matrix, end.add(offset), red, green, blue, alpha, light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3d pos,
                               int red, int green, int blue, int alpha, int light) {
        vertices.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha).light(light);
    }
}
