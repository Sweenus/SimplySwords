package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.util.ParchmentRendering;
import net.sweenus.simplyswords.api.render.ParchmentOrbitRenderData;

import java.util.ArrayList;
import java.util.List;

public class ParchmentOrbitVisualEntityRenderer<T extends Entity & ParchmentOrbitRenderData> extends EntityRenderer<T> {

    public ParchmentOrbitVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return ParchmentRendering.PAGE_TEXTURE;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        // Orbit art extends beyond the entity's culling box.
        float reach = Math.max(entity.getOrbitRadius(), entity.getOrbitHeight()) + 2.5F;
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(reach));
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        switch (entity.getMode()) {
            case ParchmentOrbitRenderData.MODE_BLESSING ->
                    renderBlessing(entity, age, tickDelta, matrices, vertexConsumers, light);
            case ParchmentOrbitRenderData.MODE_GROUND_RING ->
                    renderGroundRing(entity, age, matrices, vertexConsumers, light);
            case ParchmentOrbitRenderData.MODE_SEAL_STAMP ->
                    renderSealStamp(entity, age, matrices, vertexConsumers, light);
            default -> {
            }
        }
    }

    private void renderBlessing(T entity, float age, float tickDelta,
                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Vec3d anchor = resolveAnchorOffset(entity, tickDelta);
        float sealHeight = entity.getOrbitHeight();
        float bob = MathHelper.sin(age * 0.08F) * 0.05F;

        matrices.push();
        matrices.translate(anchor.x, anchor.y + sealHeight + bob, anchor.z);
        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        float pulse = 1.0F + MathHelper.sin(age * 0.11F) * 0.05F;
        ParchmentRendering.renderSeal(matrices, vertexConsumers, light, 0.34F * pulse, 1.0F);
        matrices.pop();

        if (!shouldHideRibbons(entity)) {
            for (int r = 0; r < 2; r++) {
                float side = r == 0 ? 1.0F : -1.0F;
                float sway = MathHelper.sin(age * 0.07F + r * 2.4F) * 0.16F;
                float swayZ = MathHelper.cos(age * 0.055F + r * 1.7F) * 0.12F;
                List<Vec3d> path = new ArrayList<>();
                int samples = 10;
                float length = sealHeight * 0.72F;
                for (int i = 0; i <= samples; i++) {
                    float t = i / (float) samples;
                    path.add(new Vec3d(
                            side * (0.08 + t * t * 0.12) + sway * t,
                            -0.12 - t * length,
                            swayZ * t + MathHelper.sin(age * 0.13F + t * 5.0F + r) * 0.045F * t));
                }
                ParchmentRendering.renderRibbon(matrices, vertexConsumers, light, path, 0.085F,
                        new Vec3d(MathHelper.cos(age * 0.03F + r), 0.0, MathHelper.sin(age * 0.03F + r)).normalize(),
                        age, 0.95F);
            }
        }
        matrices.pop();

        int pages = Math.min(entity.getPageCount(), 3);
        float radius = entity.getOrbitRadius();
        for (int i = 0; i < pages; i++) {
            float phase = i * (MathHelper.TAU / Math.max(1, pages));
            float angle = phase + age * 0.03F;
            double x = anchor.x + MathHelper.cos(angle) * radius;
            double z = anchor.z + MathHelper.sin(angle) * (radius * 0.8F);
            double y = anchor.y + sealHeight * 0.45F + MathHelper.sin(age * 0.09F + phase) * 0.14F;
            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-angle * MathHelper.DEGREES_PER_RADIAN + 90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(age * 0.06F + i * 2.0F) * 18.0F));
            ParchmentRendering.renderBentPage(matrices, vertexConsumers, light,
                    age, i * 3.1F, 0.26F, 0.3F, 0.5F, 0.9F);
            matrices.pop();
        }
    }

    private void renderGroundRing(T entity, float age,
                                  MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        int pages = entity.getPageCount();
        if (pages <= 0) {
            return;
        }
        float radius = entity.getOrbitRadius();
        float height = entity.getOrbitHeight();
        float pageScale = MathHelper.clamp(radius / 2.2F, 1.0F, 2.4F);

        for (int i = 0; i < pages; i++) {
            float phase = i * (MathHelper.TAU / pages);
            float angle = phase + age * 0.008F;
            double x = MathHelper.cos(angle) * radius;
            double z = MathHelper.sin(angle) * radius;
            double y = height + MathHelper.sin(age * 0.07F + phase * 2.0F) * 0.06F;

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-angle * MathHelper.DEGREES_PER_RADIAN + 90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.sin(age * 0.05F + i * 1.3F) * 9.0F));
            ParchmentRendering.renderBentPage(matrices, vertexConsumers, light,
                    age, i * 2.7F, 0.3F * pageScale, 0.44F * pageScale, 0.4F, 0.95F);
            matrices.pop();
        }
    }

    private void renderSealStamp(T entity, float age,
                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = entity.getOrbitRadius();
        float grow = Math.min(1.0F, age / 5.0F);
        float overshoot = 1.0F + 0.35F * MathHelper.sin(Math.min(grow, 1.0F) * MathHelper.PI);
        float flash = MathHelper.clamp(1.4F - age * 0.03F, 0.6F, 1.0F);

        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(age * 0.05F) * 4.0F));
        ParchmentRendering.renderSeal(matrices, vertexConsumers, light, scale * grow * overshoot, flash);
        matrices.pop();
    }

    private static <T extends Entity & ParchmentOrbitRenderData> boolean shouldHideRibbons(T entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return false;
        }
        return entity.getOwnerEntityId() == client.player.getId()
                && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    private Vec3d resolveAnchorOffset(T entity, float tickDelta) {
        int ownerId = entity.getOwnerEntityId();
        if (ownerId < 0 || MinecraftClient.getInstance().world == null) {
            return Vec3d.ZERO;
        }
        Entity owner = MinecraftClient.getInstance().world.getEntityById(ownerId);
        if (owner == null) {
            return Vec3d.ZERO;
        }
        Vec3d ownerPos = new Vec3d(
                MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ()));
        Vec3d selfPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        return ownerPos.subtract(selfPos);
    }
}
