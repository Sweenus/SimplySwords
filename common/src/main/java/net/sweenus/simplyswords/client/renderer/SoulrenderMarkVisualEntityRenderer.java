package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VexEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.entity.SoulrenderMarkVisualEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SoulrenderMarkVisualEntityRenderer extends EntityRenderer<SoulrenderMarkVisualEntity> {

    private static final Identifier VEX_TEXTURE = new Identifier("minecraft", "textures/entity/illager/vex.png");
    private static final double RENDER_POSITION_SMOOTHING = 0.4;
    private static final float RENDER_YAW_SMOOTHING = 0.4F;
    private static final double MIN_MOTION_FOR_HEADING_SQ = 1.0E-5;

    private final VexEntityRenderer vexRenderer;
    private VexEntity renderVex;
    private final Map<Integer, Vec3d> smoothedPositions = new HashMap<>();
    private final Map<Integer, Float> smoothedYaws = new HashMap<>();

    public SoulrenderMarkVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.vexRenderer = new VexEntityRenderer(context);
    }

    @Override
    public Identifier getTexture(SoulrenderMarkVisualEntity entity) {
        return VEX_TEXTURE;
    }

    @Override
    public void render(SoulrenderMarkVisualEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float scale = MathHelper.clamp(entity.getScale(), 0.0F, 2.0F);
        if (scale <= 0.01F) {
            return;
        }

        VexEntity vex = getOrCreateVex();
        if (vex == null) {
            return;
        }

        if (entity.age % 40 == 0) {
            pruneDeadEntries();
        }

        int entityId = entity.getId();
        Vec3d targetPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
        Vec3d previousSmoothedPos = this.smoothedPositions.get(entityId);
        Vec3d smoothedPos = previousSmoothedPos;
        if (smoothedPos == null) {
            smoothedPos = targetPos;
        } else {
            smoothedPos = smoothedPos.lerp(targetPos, RENDER_POSITION_SMOOTHING);
        }
        this.smoothedPositions.put(entityId, smoothedPos);

        float targetYaw = entity.getYaw();
        if (previousSmoothedPos != null) {
            Vec3d movement = smoothedPos.subtract(previousSmoothedPos);
            if (movement.lengthSquared() > MIN_MOTION_FOR_HEADING_SQ) {
                targetYaw = (float) (Math.atan2(movement.x, movement.z) * (180.0F / Math.PI));
            }
        }
        float previousSmoothedYaw = this.smoothedYaws.getOrDefault(entityId, targetYaw);
        float renderYaw = MathHelper.lerpAngleDegrees(RENDER_YAW_SMOOTHING, previousSmoothedYaw, targetYaw);
        this.smoothedYaws.put(entityId, renderYaw);

        vex.refreshPositionAndAngles(smoothedPos.x, smoothedPos.y, smoothedPos.z, 0.0F, 0.0F);
        vex.prevYaw = 0.0F;
        vex.setYaw(0.0F);
        vex.prevPitch = 0.0F;
        vex.setPitch(0.0F);
        vex.prevBodyYaw = 0.0F;
        vex.bodyYaw = 0.0F;
        vex.prevHeadYaw = 0.0F;
        vex.setHeadYaw(0.0F);
        vex.setNoGravity(true);
        vex.age = entity.age;

        matrices.push();
        matrices.translate(0.0, -0.12, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));
        matrices.scale(0.35F * scale, 0.35F * scale, 0.35F * scale);
        this.vexRenderer.render(vex, 0.0F, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }

    private void pruneDeadEntries() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            this.smoothedPositions.clear();
            this.smoothedYaws.clear();
            return;
        }

        Iterator<Integer> posIt = this.smoothedPositions.keySet().iterator();
        while (posIt.hasNext()) {
            int id = posIt.next();
            if (client.world.getEntityById(id) == null) {
                posIt.remove();
                this.smoothedYaws.remove(id);
            }
        }
    }

    private VexEntity getOrCreateVex() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }
        if (this.renderVex == null || this.renderVex.getWorld() != client.world) {
            this.renderVex = EntityType.VEX.create(client.world);
        }
        return this.renderVex;
    }
}
