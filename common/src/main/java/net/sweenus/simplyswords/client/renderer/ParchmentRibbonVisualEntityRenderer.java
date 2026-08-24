package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.util.ParchmentRendering;
import net.sweenus.simplyswords.api.render.ParchmentRibbonRenderData;

import java.util.ArrayList;
import java.util.List;

public class ParchmentRibbonVisualEntityRenderer<T extends Entity & ParchmentRibbonRenderData> extends EntityRenderer<T> {

    public ParchmentRibbonVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return ParchmentRendering.RIBBON_TEXTURE;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        Vec3d end = entity.getEndOffset();
        double reach = Math.max(4.0, end.length() + 2.0);
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(reach));
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float progress = entity.getLifeProgress(tickDelta);
        float alpha = Math.min(progress / 0.15F, (1.0F - progress) / 0.25F);
        alpha = MathHelper.clamp(alpha, 0.0F, 1.0F) * 0.92F;
        if (alpha <= 0.01F) {
            return;
        }

        int seed = entity.getSeed();
        float halfWidth = entity.getHalfWidth();

        switch (entity.getStyle()) {
            case ParchmentRibbonRenderData.STYLE_LINK -> {
                List<Vec3d> path = ParchmentRendering.buildWindingPath(
                        Vec3d.ZERO, entity.getEndOffset(), seed, age, 0.45F, 20);
                ParchmentRendering.renderRibbon(matrices, vertexConsumers, light, path, halfWidth, null, age, alpha);
            }
            case ParchmentRibbonRenderData.STYLE_SKY_SPIRAL ->
                    renderSkySpiral(entity, age, alpha, matrices, vertexConsumers, light);
            case ParchmentRibbonRenderData.STYLE_NOVA_RING ->
                    renderNovaRing(entity, age, progress, alpha, matrices, vertexConsumers, light);
            case ParchmentRibbonRenderData.STYLE_ARC ->
                    renderArc(entity, age, alpha, matrices, vertexConsumers, light);
            default -> {
            }
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void renderSkySpiral(T entity, float age, float alpha,
                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        double height = entity.getEndOffset().y;
        float phase = (entity.getSeed() % 628) / 100.0F;
        int samples = 26;
        List<Vec3d> path = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            float t = i / (float) samples; // 0 = top, 1 = impact
            float angle = t * MathHelper.TAU * 2.0F + phase + age * 0.06F;
            float radius = 1.05F * (1.0F - t * 0.8F) + 0.12F;
            path.add(new Vec3d(
                    MathHelper.cos(angle) * radius,
                    height * (1.0F - t),
                    MathHelper.sin(angle) * radius));
        }
        ParchmentRendering.renderRibbon(matrices, vertexConsumers, light, path,
                entity.getHalfWidth(), null, age, alpha);
    }

    private void renderNovaRing(T entity, float age, float progress, float alpha,
                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float maxRadius = (float) entity.getEndOffset().x;
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float radius = 0.5F + (maxRadius - 0.5F) * eased;
        float phase = (entity.getSeed() % 628) / 100.0F;

        int samples = Math.max(32, (int) (radius * 10.0F));
        List<Vec3d> path = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            float t = i / (float) samples;
            float angle = t * MathHelper.TAU;
            float waveY = 0.35F + MathHelper.sin(angle * 3.0F + phase + age * 0.14F) * 0.22F;
            path.add(new Vec3d(
                    MathHelper.cos(angle) * radius,
                    waveY,
                    MathHelper.sin(angle) * radius));
        }
        ParchmentRendering.renderRibbon(matrices, vertexConsumers, light, path,
                entity.getHalfWidth(), new Vec3d(0.0, 1.0, 0.0), age, alpha);
    }

    private void renderArc(T entity, float age, float alpha,
                           MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float radius = (float) entity.getEndOffset().y;
        float yawRad = entity.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(yawRad);
        float sway = MathHelper.sin(age * 0.09F + entity.getSeed()) * 0.08F;

        int samples = 22;
        List<Vec3d> path = new ArrayList<>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            float t = i / (float) samples;
            float angle = MathHelper.PI * t;
            float along = MathHelper.cos(angle) * radius;
            float up = MathHelper.sin(angle) * radius;
            path.add(new Vec3d(along * cos + sway * sin, up + 0.1F, along * sin - sway * cos));
        }
        ParchmentRendering.renderRibbon(matrices, vertexConsumers, light, path,
                entity.getHalfWidth(), null, age, alpha);
    }
}
