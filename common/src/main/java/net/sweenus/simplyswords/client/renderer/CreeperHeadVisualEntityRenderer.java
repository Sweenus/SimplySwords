package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.SimplySwordsCreeperHeadEntity;

public class CreeperHeadVisualEntityRenderer extends EntityRenderer<SimplySwordsCreeperHeadEntity> {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/creeper/creeper.png");

    private final SkullEntityModel model;

    public CreeperHeadVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new SkullEntityModel(context.getPart(EntityModelLayers.CREEPER_HEAD));
    }

    @Override
    public Identifier getTexture(SimplySwordsCreeperHeadEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(SimplySwordsCreeperHeadEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        float headYaw;
        float pitch;

        if (!entity.isHoming()) {
            Vec3d offset = getOrbitOffset(entity, tickDelta);
            matrices.translate(offset.x, offset.y, offset.z);

            double angle = entity.getOrbitBaseAngle() + (entity.age + tickDelta) * entity.getOrbitAngularSpeed();
            double tangentX = -Math.sin(angle);
            double tangentZ = Math.cos(angle);
            headYaw = (float) Math.toDegrees(Math.atan2(-tangentX, tangentZ)) + 180.0F;
            pitch = 0.0F;
        } else {
            headYaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) + 180.0F;
            pitch = entity.getPitch(tickDelta);
        }

        matrices.scale(-1.0F, -1.0F, 1.0F);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(getTexture(entity)));
        this.model.setHeadRotation(0.0F, headYaw, pitch);
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV,
                1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private Vec3d getOrbitOffset(SimplySwordsCreeperHeadEntity entity, float tickDelta) {
        Vec3d entityPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );

        Vec3d center = entityPos;
        if (entity.getWorld().getEntityById(entity.getOwnerEntityId()) instanceof LivingEntity owner) {
            center = new Vec3d(
                    MathHelper.lerp(tickDelta, owner.prevX, owner.getX()),
                    MathHelper.lerp(tickDelta, owner.prevY, owner.getY()),
                    MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ())
            );
        }

        double angle = entity.getOrbitBaseAngle() + (entity.age + tickDelta) * entity.getOrbitAngularSpeed();
        double bob = Math.sin(angle * 2.0) * 0.08;
        double radius = entity.getOrbitRadius();
        double height = entity.getOrbitHeight();

        Vec3d headPos = center.add(Math.cos(angle) * radius, height + bob, Math.sin(angle) * radius);
        return headPos.subtract(entityPos);
    }
}
