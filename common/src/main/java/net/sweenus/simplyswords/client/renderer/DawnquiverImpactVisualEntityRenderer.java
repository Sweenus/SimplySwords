package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.DawnquiverImpactVisualEntity;
import org.joml.Matrix4f;

public final class DawnquiverImpactVisualEntityRenderer extends EntityRenderer<DawnquiverImpactVisualEntity> {

    private static final Identifier WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png");

    public DawnquiverImpactVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(DawnquiverImpactVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(DawnquiverImpactVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(10.0));
    }

    @Override
    public void render(DawnquiverImpactVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        float age = entity.age + tickDelta;
        float progress = MathHelper.clamp(age / entity.getLifetime(), 0.0F, 1.0F);
        float eased = DawnquiverRenderGeometry.easeOutCubic(progress);
        float fade = MathHelper.clamp((1.0F - progress) * 1.4F, 0.0F, 1.0F);
        float scale = entity.getScale();
        drawPass(DawnquiverRenderPass.BODY, DawnquiverRenderPass.BODY.getBuffer(consumers), entity,
                matrices, age, progress, eased, fade, scale);
        drawPass(DawnquiverRenderPass.GLOW, DawnquiverRenderPass.GLOW.getBuffer(consumers), entity,
                matrices, age, progress, eased, fade, scale);

        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void drawPass(DawnquiverRenderPass pass, VertexConsumer vertices,
                          DawnquiverImpactVisualEntity entity, MatrixStack matrices,
                          float age, float progress, float eased, float fade, float scale) {
        VertexConsumer body = pass.isBody() ? vertices : null;
        VertexConsumer glow = pass.isGlow() ? vertices : null;

        matrices.push();
        matrices.scale(scale, scale, scale);
        Matrix4f worldMatrix = matrices.peek().getPositionMatrix();
        double ringRadius = 0.35 + eased * 2.7;
        DawnquiverRenderGeometry.ring(glow, worldMatrix, 48,
                new Vec3d(0.0, 0.05 + eased * 0.12, 0.0),
                ringRadius, 0.25 * (1.0F - progress * 0.55F),
                255, 187, 52, Math.round(190 * fade));
        DawnquiverRenderGeometry.ring(body, worldMatrix, 48,
                new Vec3d(0.0, 0.055 + eased * 0.12, 0.0),
                ringRadius * 0.82, 0.065,
                255, 243, 185, Math.round(205 * fade));
        matrices.pop();

        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(scale, scale, scale);
        Matrix4f billboard = matrices.peek().getPositionMatrix();
        float flash = (0.48F + eased * 2.35F) * (1.0F - progress * 0.48F);
        DawnquiverRenderGeometry.billboardLens(glow, billboard, Vec3d.ZERO,
                flash, flash, 255, 175, 38, Math.round(205 * fade));
        DawnquiverRenderGeometry.billboardLens(body, billboard, new Vec3d(0.0, 0.0, 0.003),
                flash * 0.27F, flash * 0.27F,
                255, 255, 235, Math.round(245 * fade));
        DawnquiverRenderGeometry.flatRing(glow, billboard, 48, Vec3d.ZERO,
                0.24 + eased * 2.15, 0.18 * (1.0F - progress * 0.58F),
                255, 229, 136, Math.round(190 * fade));
        DawnquiverRenderGeometry.rays(glow, billboard, Vec3d.ZERO, 12,
                0.18 + eased * 0.22, 0.85 + eased * 2.75, 0.095,
                entity.getSeed() * 0.01F + age * 0.035F,
                255, 203, 76, Math.round(210 * fade), 0);
        matrices.pop();
    }
}
