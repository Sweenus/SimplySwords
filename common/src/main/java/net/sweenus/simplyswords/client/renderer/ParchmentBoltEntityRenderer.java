package net.sweenus.simplyswords.client.renderer;

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
import net.sweenus.simplyswords.api.render.ParchmentBoltRenderData;

import java.util.HashMap;
import java.util.Map;

public class ParchmentBoltEntityRenderer<T extends Entity & ParchmentBoltRenderData> extends EntityRenderer<T> {

    private static final float POSITION_SMOOTHING_RATE = 0.55F;

    private static final Map<Integer, RenderState> RENDER_STATES = new HashMap<>();

    public ParchmentBoltEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return ParchmentRendering.PAGE_TEXTURE;
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float seed = entity.getSpinSeed();

        matrices.push();

        Vec3d offset = resolveSmoothedOffset(entity, tickDelta);
        matrices.translate(offset.x, offset.y, offset.z);

        float flightYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getFlightYaw());
        float flightPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getFlightPitch());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-flightYaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flightPitch));

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(seed + age * 6.0F));

        ParchmentRendering.renderBentPage(matrices, vertexConsumers, light,
                age, seed * 0.1F, 0.5F, 0.4F, 0.55F, 1.0F);
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    // Plain entities apply sparse position packets without interpolation.
    private static <T extends Entity & ParchmentBoltRenderData> Vec3d resolveSmoothedOffset(T entity, float tickDelta) {
        float ageTicks = entity.age + tickDelta;
        Vec3d target = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));

        RenderState state = RENDER_STATES.computeIfAbsent(entity.getId(),
                ignored -> new RenderState(target, ageTicks));

        float deltaTicks = ageTicks - state.lastAgeTicks;
        if (deltaTicks < 0.0F || deltaTicks > 5.0F) {
            state.position = target;
        } else if (deltaTicks > 0.0F) {
            state.position = state.position.lerp(target,
                    Math.min(1.0F, POSITION_SMOOTHING_RATE * deltaTicks));
        }
        state.lastAgeTicks = ageTicks;

        if (entity.age % 80 == 0) {
            RENDER_STATES.entrySet().removeIf(entry ->
                    entry.getKey() != entity.getId() && ageTicks - entry.getValue().lastAgeTicks > 100.0F);
        }
        return state.position.subtract(target);
    }

    private static final class RenderState {
        private Vec3d position;
        private float lastAgeTicks;

        private RenderState(Vec3d position, float ageTicks) {
            this.position = position;
            this.lastAgeTicks = ageTicks;
        }
    }
}
