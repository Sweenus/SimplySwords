package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.MagibladeWardenHeadVisualEntity;

public class MagibladeWardenHeadVisualEntityRenderer
        extends EntityRenderer<MagibladeWardenHeadVisualEntity> {

    private static final Identifier BASE_TEXTURE =
            Identifier.ofVanilla("textures/entity/warden/warden.png");
    private static final Identifier BIOLUMINESCENT_TEXTURE =
            Identifier.ofVanilla("textures/entity/warden/warden_bioluminescent_layer.png");
    private static final double ORBIT_BOB_HEIGHT = 0.1;
    private static final double ORBIT_BOB_SPEED = 0.16;

    private final ModelPart head;
    private final ModelPart rightTendril;
    private final ModelPart leftTendril;

    public MagibladeWardenHeadVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        ModelPart root = context.getPart(EntityModelLayers.WARDEN);
        this.head = root.getChild("bone").getChild("body").getChild("head");
        this.rightTendril = this.head.getChild("right_tendril");
        this.leftTendril = this.head.getChild("left_tendril");
        this.head.setPivot(0.0F, 0.0F, 0.0F);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(MagibladeWardenHeadVisualEntity entity) {
        return BASE_TEXTURE;
    }

    @Override
    public boolean shouldRender(MagibladeWardenHeadVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getBoundingBox().expand(2.5));
    }

    @Override
    public void render(MagibladeWardenHeadVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (shouldSkipFirstPersonOwner(entity)) {
            return;
        }

        float spawnFade = MathHelper.clamp((entity.age + tickDelta) / 8.0F, 0.0F, 1.0F);
        float dismissFade = entity.isDismissing()
                ? 1.0F - MathHelper.clamp(
                (entity.getDismissTicks() + tickDelta)
                        / MagibladeWardenHeadVisualEntity.DISMISS_DURATION_TICKS,
                0.0F,
                1.0F)
                : 1.0F;
        float fade = spawnFade * dismissFade;
        if (fade <= 0.01F) {
            return;
        }

        float renderYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getTargetYaw());
        float renderPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getTargetPitch());
        float pulseProgress = entity.getShotPulseTicks() <= 0
                ? 0.0F
                : 1.0F - (entity.getShotPulseTicks() - tickDelta) / 8.0F;
        float shotPulse = MathHelper.sin(MathHelper.clamp(pulseProgress, 0.0F, 1.0F) * MathHelper.PI);
        float appearScale = 0.72F + 0.28F * spawnFade;
        float dismissScale = 0.55F + 0.45F * dismissFade;
        float scale = entity.getScale() * appearScale * dismissScale * (1.0F + shotPulse * 0.1F);

        animateTendrils(entity, tickDelta, shotPulse);

        Vec3d entityPosition = interpolatedPosition(entity, tickDelta);
        Vec3d renderAnchor = getRenderAnchor(entity, tickDelta);
        Vec3d renderOffset = renderAnchor.subtract(entityPosition);

        matrices.push();
        matrices.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - renderYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-renderPitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.scale(scale, scale, scale);
        matrices.translate(0.0F, -0.5F, -shotPulse * 0.08F);

        int alpha = MathHelper.clamp((int) (255.0F * fade), 0, 255);
        int color = alpha << 24 | 0xFFFFFF;

        VertexConsumer baseVertices =
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BASE_TEXTURE));
        this.head.render(matrices, baseVertices, light, OverlayTexture.DEFAULT_UV, color);

        VertexConsumer glowVertices =
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(BIOLUMINESCENT_TEXTURE));
        this.head.render(
                matrices,
                glowVertices,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                color
        );
        matrices.pop();
    }

    private static Vec3d getRenderAnchor(MagibladeWardenHeadVisualEntity entity, float tickDelta) {
        if (entity.getOwner() instanceof LivingEntity owner) {
            float age = entity.age + tickDelta;
            double angle = entity.getOrbitPhase()
                    + age * Math.max(0.001, Config.uniqueEffects.magiblade.headOrbitSpeed);
            double radius = Math.max(0.0, Config.uniqueEffects.magiblade.headOrbitRadius);
            double bob = MathHelper.sin((float) (age * ORBIT_BOB_SPEED + entity.getOrbitPhase()))
                    * ORBIT_BOB_HEIGHT;
            return new Vec3d(
                    MathHelper.lerp(tickDelta, owner.prevX, owner.getX()) + Math.cos(angle) * radius,
                    MathHelper.lerp(tickDelta, owner.prevY, owner.getY())
                            + owner.getEyeHeight(owner.getPose())
                            + Config.uniqueEffects.magiblade.headVerticalOffset
                            + bob,
                    MathHelper.lerp(tickDelta, owner.prevZ, owner.getZ()) + Math.sin(angle) * radius
            );
        }
        return interpolatedPosition(entity, tickDelta);
    }

    private static Vec3d interpolatedPosition(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private void animateTendrils(MagibladeWardenHeadVisualEntity entity,
                                 float tickDelta, float shotPulse) {
        float time = entity.age + tickDelta;
        float activity = entity.isAiming() ? 1.0F : 0.45F;
        float speed = entity.isAiming() ? 0.55F : 0.24F;
        float spread = (0.16F + 0.24F * activity) * MathHelper.cos(time * speed);

        this.head.pitch = 0.0F;
        this.head.yaw = 0.0F;
        this.head.roll = 0.0F;
        this.rightTendril.pitch = 0.15F + spread + shotPulse * 0.25F;
        this.leftTendril.pitch = 0.15F - spread + shotPulse * 0.25F;
        this.rightTendril.yaw = -0.08F - activity * 0.08F;
        this.leftTendril.yaw = 0.08F + activity * 0.08F;
        this.rightTendril.roll = -shotPulse * 0.12F;
        this.leftTendril.roll = shotPulse * 0.12F;
    }

    private static boolean shouldSkipFirstPersonOwner(MagibladeWardenHeadVisualEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return false;
        }
        Entity owner = entity.getOwner();
        return owner == client.player && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }
}
