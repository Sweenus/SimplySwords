package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
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
import net.sweenus.simplyswords.api.render.ParchmentGlyphFlightRenderData;
import net.sweenus.simplyswords.client.render.BoltPath;
import net.sweenus.simplyswords.client.util.ParchmentRendering;

public class ParchmentGlyphFlightVisualEntityRenderer<T extends Entity & ParchmentGlyphFlightRenderData>
        extends EntityRenderer<T> {

    private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);
    private static final float FLIGHT_SPAN = 0.55F;

    public ParchmentGlyphFlightVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return ParchmentRendering.GLYPHS_TEXTURE;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        Vec3d target = resolveTargetOffset(entity, 1.0F);
        double reach = Math.max(4.0, target.length() + 2.0);
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(reach));
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float progress = entity.getLifeProgress(tickDelta);
        int count = Math.max(1, entity.getGlyphCount());
        Vec3d target = resolveTargetOffset(entity, tickDelta);

        Vec3d direction = target.lengthSquared() < 1.0E-4 ? UP : target.normalize();
        Vec3d perp1 = direction.crossProduct(UP);
        if (perp1.lengthSquared() < 1.0E-4) {
            perp1 = direction.crossProduct(new Vec3d(1.0, 0.0, 0.0));
        }
        perp1 = perp1.normalize();
        Vec3d perp2 = direction.crossProduct(perp1).normalize();

        float age = entity.age + tickDelta;
        int seed = entity.getSeed();
        float stagger = count > 1 ? (1.0F - FLIGHT_SPAN) / (count - 1) : 0.0F;

        for (int i = 0; i < count; i++) {
            float t = MathHelper.clamp((progress - i * stagger) / FLIGHT_SPAN, 0.0F, 1.0F);
            if (t <= 0.0F || t >= 1.0F) {
                continue;
            }
            float alpha = MathHelper.clamp(Math.min(t / 0.12F, (1.0F - t) / 0.22F), 0.0F, 1.0F) * 0.95F;
            if (alpha <= 0.01F) {
                continue;
            }

            long glyphSeed = seed + i * 6871L;
            float phase = (float) BoltPath.unit(glyphSeed, 0L, 1L) * MathHelper.TAU;
            float swing = (0.35F + (float) BoltPath.unit(glyphSeed, 0L, 2L) * 0.35F)
                    * MathHelper.sin(t * MathHelper.PI);
            float ease = t * t * (3.0F - 2.0F * t);

            Vec3d lateral = perp1.multiply(MathHelper.cos(phase) * swing)
                    .add(perp2.multiply(MathHelper.sin(phase) * swing));
            Vec3d pos = target.multiply(ease).add(lateral).add(0.0, (1.0F - ease) * 0.35F, 0.0);
            int glyphIndex = (int) (BoltPath.unit(glyphSeed, 0L, 3L) * 64.0);

            matrices.push();
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(this.dispatcher.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.sin(age * 0.14F + i * 1.9F) * 14.0F));
            ParchmentRendering.renderGlyph(matrices, vertexConsumers, entity.getGlyphStyleId(), glyphIndex,
                    0.26F * (1.0F - ease * 0.35F), alpha);
            matrices.pop();
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private Vec3d resolveTargetOffset(T entity, float tickDelta) {
        int ownerId = entity.getOwnerEntityId();
        MinecraftClient client = MinecraftClient.getInstance();
        if (ownerId < 0 || client.world == null) {
            return UP;
        }
        Entity owner = client.world.getEntityById(ownerId);
        if (owner == null) {
            return UP;
        }
        Vec3d ownerPos = new Vec3d(
                MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ()));
        Vec3d selfPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        return ownerPos.subtract(selfPos).add(0.0, Math.max(0.6, owner.getHeight() * 0.65), 0.0);
    }
}
