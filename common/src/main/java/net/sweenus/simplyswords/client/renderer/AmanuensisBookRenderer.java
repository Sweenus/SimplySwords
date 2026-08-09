package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.util.ParchmentRendering;
import net.sweenus.simplyswords.api.render.ParchmentBookRenderData;

import java.util.HashMap;
import java.util.Map;

public class AmanuensisBookRenderer<T extends Entity & ParchmentBookRenderData> extends EntityRenderer<T> {

    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/enchanting_table_book.png");
    private static final int SCRIBBLE_COUNT = 7;
    private static final float SMOOTHING_RATE = 0.35F;
    private static final float POSITION_SMOOTHING_RATE = 0.30F;
    private static final float YAW_SMOOTHING_RATE = 0.35F;
    private static final float CHANNEL_YAW_SMOOTHING_RATE = 0.75F;
    private static final float PAGE_TILT_DEGREES = 20.0F;

    private static final Map<Integer, PageState> PAGE_STATES = new HashMap<>();

    private final BookModel model;

    public AmanuensisBookRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new BookModel(context.getPart(EntityModelLayers.BOOK));
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(T entity) {
        return TEXTURE;
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float age = entity.age + tickDelta;
        float channel = entity.getChannelProgress();

        PageState pages = updatePageState(entity, tickDelta, channel);
        float speed = (float) pages.velocity.horizontalLength();
        float bodyYaw = pages.yaw;

        matrices.push();
        matrices.translate(pages.offset.x, pages.offset.y, pages.offset.z);
        matrices.translate(
                MathHelper.sin(age * 0.055F) * 0.05F,
                MathHelper.sin(age * 0.1F) * 0.045F + MathHelper.sin(age * 0.21F) * 0.02F,
                MathHelper.sin(age * 0.11F) * 0.05F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));

        float bank = MathHelper.clamp(speed * 160.0F, 0.0F, 18.0F)
                * MathHelper.sin(age * 0.09F + entity.getId());
        float climb = MathHelper.clamp((float) pages.velocity.y * 220.0F, -14.0F, 14.0F);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(bank * 0.4F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-climb));
        // BookModel's open face points along +X, unlike the entity frame's -Z.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(PAGE_TILT_DEGREES));
        matrices.scale(0.85F, -0.85F, -0.85F);

        float flip = MathHelper.sin(pages.flutterPhase) * (0.14F + pages.agitation * 0.3F)
                + pages.pulse * 0.4F;
        this.model.setPageAngles(pages.pageTurnPhase,
                MathHelper.clamp(flip, 0.0F, 1.0F),
                MathHelper.clamp(flip * 0.8F, 0.0F, 1.0F), pages.openAmount);

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(TEXTURE));
        this.model.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();

        if (channel > 0.0F) {
            renderScribbles(entity, age, channel, matrices, vertexConsumers, light);
        }
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static <T extends Entity & ParchmentBookRenderData> PageState updatePageState(
            T entity, float tickDelta, float channel) {
        float ageTicks = entity.age + tickDelta;
        Vec3d targetPos = new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        float targetYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        float targetPulse = entity.getRecitePulseProgress();

        PageState state = PAGE_STATES.computeIfAbsent(entity.getId(),
                ignored -> new PageState(targetPos, targetYaw, targetPulse, channel, ageTicks));

        float deltaTicks = ageTicks - state.lastAgeTicks;
        if (deltaTicks < 0.0F || deltaTicks > 5.0F) {
            state.position = targetPos;
            state.velocity = Vec3d.ZERO;
            state.yaw = targetYaw;
            state.pulse = targetPulse;
            state.channel = channel;
            state.agitation = channel * 0.8F;
            state.openAmount = MathHelper.clamp(0.32F + state.agitation * 0.35F + targetPulse * 0.5F, 0.0F, 1.0F);
        } else if (deltaTicks > 0.0F) {
            Vec3d eased = state.position.lerp(targetPos,
                    Math.min(1.0F, POSITION_SMOOTHING_RATE * deltaTicks));
            state.velocity = eased.subtract(state.position).multiply(1.0 / deltaTicks);
            state.position = eased;
            float yawRate = MathHelper.lerp(MathHelper.clamp(channel, 0.0F, 1.0F),
                    YAW_SMOOTHING_RATE, CHANNEL_YAW_SMOOTHING_RATE);
            state.yaw = MathHelper.lerpAngleDegrees(
                    Math.min(1.0F, yawRate * deltaTicks), state.yaw, targetYaw);

            float speed = (float) state.velocity.horizontalLength();
            float targetAgitation = MathHelper.clamp(speed * 6.0F, 0.0F, 0.5F) + channel * 0.8F;
            float targetOpen = MathHelper.clamp(
                    0.32F + targetAgitation * 0.35F + targetPulse * 0.5F, 0.0F, 1.0F);

            float ease = Math.min(1.0F, SMOOTHING_RATE * deltaTicks);
            state.agitation = MathHelper.lerp(ease, state.agitation, targetAgitation);
            state.openAmount = MathHelper.lerp(ease, state.openAmount, targetOpen);
            state.pulse = MathHelper.lerp(ease, state.pulse, targetPulse);
            state.channel = MathHelper.lerp(ease, state.channel, channel);

            state.flutterPhase += deltaTicks * (0.16F + state.agitation * 0.5F);
            state.pageTurnPhase += deltaTicks * (0.05F + state.channel * 0.2F);
        }
        state.lastAgeTicks = ageTicks;
        state.offset = state.position.subtract(targetPos);

        while (state.flutterPhase > MathHelper.TAU) {
            state.flutterPhase -= MathHelper.TAU;
        }
        while (state.pageTurnPhase > MathHelper.TAU) {
            state.pageTurnPhase -= MathHelper.TAU;
        }

        if (entity.age % 80 == 0) {
            PAGE_STATES.entrySet().removeIf(entry ->
                    entry.getKey() != entity.getId() && ageTicks - entry.getValue().lastAgeTicks > 100.0F);
        }
        return state;
    }

    private static final class PageState {
        private Vec3d position;
        private Vec3d velocity = Vec3d.ZERO;
        private Vec3d offset = Vec3d.ZERO;
        private float yaw;
        private float flutterPhase;
        private float pageTurnPhase;
        private float agitation;
        private float openAmount;
        private float pulse;
        private float channel;
        private float lastAgeTicks;

        private PageState(Vec3d position, float yaw, float pulse, float channel, float ageTicks) {
            this.position = position;
            this.yaw = yaw;
            this.pulse = pulse;
            this.channel = channel;
            this.agitation = channel * 0.8F;
            this.openAmount = MathHelper.clamp(0.32F + this.agitation * 0.35F + pulse * 0.5F, 0.0F, 1.0F);
            this.lastAgeTicks = ageTicks;
        }
    }

    private void renderScribbles(T entity, float age, float channel,
                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ParchmentRendering.renderGlyphArc(matrices, vertexConsumers, entity.getGlyphStyleId(),
                entity.getGlyphBase(), entity.getGlyphCount(), channel, age, 1.0F);
    }
}
